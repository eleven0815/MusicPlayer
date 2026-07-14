package com.yinqi.player.player

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import java.io.File
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.WorkManager
import com.yinqi.player.data.LocalSong
import com.yinqi.player.data.MediaRepository
import com.yinqi.player.YinqiApplication
import com.yinqi.player.data.EnrichmentEntity
import com.yinqi.player.data.EnrichmentScheduler
import com.yinqi.player.data.EnrichmentStatus
import com.yinqi.player.data.LibraryRepository
import com.yinqi.player.data.LyricsDocument
import com.yinqi.player.data.MusicDatabase
import com.yinqi.player.data.parseLrc
import com.yinqi.player.data.toLocalSong
import com.yinqi.player.data.toTrackEntity
import com.yinqi.player.data.TextNormalizer
import com.yinqi.player.settingsDataStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class LibrarySnapshot(
    val library: Pair<List<LocalSong>, List<EnrichmentEntity>>,
    val favoriteKeys: List<String>,
    val playlists: List<com.yinqi.player.data.PlaylistEntity>,
    val playlistTracks: List<com.yinqi.player.data.PlaylistTrackEntity>,
)

@OptIn(FlowPreview::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    private val database: MusicDatabase = (application as YinqiApplication).database
    private val libraryRepository = LibraryRepository(database)
    private var player: MediaController? = null
    private val controllerFuture = MediaController.Builder(
        application,
        SessionToken(application, ComponentName(application, PlaybackService::class.java)),
    ).buildAsync()
    private val _state = MutableStateFlow(MusicUiState())
    val state: StateFlow<MusicUiState> = _state.asStateFlow()
    private var selectedTreeUri: Uri? = null
    private var selectedLyricsTreeUri: Uri? = null
    private val queryFlow = MutableStateFlow("")
    private var positionJob: Job? = null
    private var playerListener: Player.Listener? = null
    private var queueRestored = false
    private var persistedQueueKeys: List<String> = emptyList()
    private var persistedCurrentKey: String? = null
    private var persistedPositionMs: Long = 0L
    private var lastPersistedPositionMs = -1L

    init {
        controllerFuture.addListener({
            player = runCatching { controllerFuture.get() }.getOrNull()?.also(::observePlayer)
        }, ContextCompat.getMainExecutor(application))
        viewModelScope.launch {
            combine(
                combine(libraryRepository.observeTracks(), libraryRepository.observeEnrichments()) { tracks, enrichments ->
                    val enrichmentByKey = enrichments.associateBy { it.trackKey }
                    tracks.map { track ->
                        track.toLocalSong(enrichmentByKey[track.trackKey])
                    } to enrichments
                },
                database.collectionDao().observeFavoriteKeys(),
                database.collectionDao().observePlaylists(),
                database.collectionDao().observePlaylistTracks(),
            ) { library, favoriteKeys, playlists, playlistTracks ->
                LibrarySnapshot(library, favoriteKeys, playlists, playlistTracks)
            }.collect { snapshot ->
                val (songs, enrichments) = snapshot.library
                val favoriteKeys = snapshot.favoriteKeys
                val playlists = snapshot.playlists
                val playlistTracks = snapshot.playlistTracks
                val lyrics = enrichments.mapNotNull { item ->
                    item.lyricsRaw?.let { raw -> item.trackKey to parseLrc(raw) }
                }.toMap()
                _state.update { current ->
                    val songByKey = songs.associateBy { it.trackKey }
                    val queue = current.queue.mapNotNull { songByKey[it.trackKey] }
                    val tracksByPlaylist = playlistTracks.groupBy { it.playlistId }
                    current.copy(
                        songs = songs,
                        queue = queue,
                        currentSong = current.currentSong?.trackKey?.let(songByKey::get),
                        lyricsByTrackKey = lyrics,
                        enrichmentByTrackKey = enrichments.associateBy { it.trackKey },
                        favoriteTrackKeys = favoriteKeys.toSet(),
                        showEnrichmentReview = current.showEnrichmentReview || (current.onlineEnabled && enrichments.any { item ->
                            item.artworkStatus == EnrichmentStatus.ReviewRequired.name || item.lyricsStatus == EnrichmentStatus.ReviewRequired.name
                        }),
                        playlists = playlists.map { playlist ->
                            UserPlaylist(
                                playlist.name,
                                tracksByPlaylist[playlist.id].orEmpty().sortedBy { it.position }.map { it.trackKey }.toSet(),
                            )
                        },
                        debouncedQuery = current.debouncedQuery,
                    )
                }
                player?.let { activePlayer ->
                    val index = activePlayer.currentMediaItemIndex
                    val updated = songs.getOrNull(index)
                    if (updated != null && activePlayer.currentMediaItem?.mediaId == updated.trackKey) {
                        activePlayer.replaceMediaItem(index, updated.toMediaItem(getApplication()))
                    }
                }
                maybeRestoreQueue()
            }
        }
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.data
                .catch { emit(emptyPreferences()) }
                .collect { preferences ->
                    val asked = preferences[ONLINE_CONSENT_ASKED] ?: false
                    val enabled = preferences[ONLINE_ENABLED] ?: false
                    val mode = PlaybackMode.entries.getOrNull(preferences[PLAYBACK_MODE] ?: 0) ?: PlaybackMode.Sequential
                    persistedQueueKeys = preferences[QUEUE_KEYS]
                        ?.split(QUEUE_SEPARATOR)
                        ?.filter(String::isNotBlank)
                        .orEmpty()
                    persistedCurrentKey = preferences[QUEUE_CURRENT_KEY]
                    persistedPositionMs = preferences[QUEUE_POSITION_MS] ?: 0L
                    selectedTreeUri = preferences[TREE_URI]?.let(Uri::parse)
                    selectedLyricsTreeUri = preferences[LYRICS_FOLDER_URI]?.let(Uri::parse)
                    _state.update {
                        it.copy(
                            onlineConsentAsked = asked,
                            onlineEnabled = enabled,
                            wifiOnly = preferences[WIFI_ONLY] ?: true,
                            dynamicBackdrop = preferences[DYNAMIC_BACKDROP] ?: true,
                            restoreQueue = preferences[RESTORE_QUEUE] ?: true,
                            includeShortAudio = preferences[INCLUDE_SHORT_AUDIO] ?: false,
                            lastScannedAtMillis = preferences[LAST_SCANNED_AT],
                            playbackMode = mode,
                            showOnlineConsent = !asked && it.songs.isNotEmpty(),
                            showEnrichmentReview = it.showEnrichmentReview || (enabled && it.enrichmentByTrackKey.values.any { item ->
                                item.artworkStatus == EnrichmentStatus.ReviewRequired.name || item.lyricsStatus == EnrichmentStatus.ReviewRequired.name
                            }),
                        )
                    }
                    player?.let { applyPlaybackMode(it, mode) }
                }
        }
        viewModelScope.launch {
            queryFlow.debounce(200L).collect { query ->
                _state.update { it.copy(debouncedQuery = query) }
            }
        }
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkFlow(EnrichmentScheduler.WORK_NAME).collect { infos ->
                _state.update { state -> state.copy(enrichmentWorkRunning = infos.any { !it.state.isFinished }) }
            }
        }
    }

    private fun observePlayer(activePlayer: Player) {
        applyPlaybackMode(activePlayer, _state.value.playbackMode)
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionUpdates(activePlayer) else stopPositionUpdates()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val song = _state.value.queue.getOrNull(activePlayer.currentMediaItemIndex)
                _state.update { it.copy(currentSong = song, positionMs = 0L) }
                persistQueue()
            }
        }
        playerListener = listener
        activePlayer.addListener(listener)
        if (activePlayer.isPlaying) startPositionUpdates(activePlayer)
        maybeRestoreQueue()
    }

    private fun startPositionUpdates(activePlayer: Player) {
        if (positionJob?.isActive == true) return
        positionJob = viewModelScope.launch {
            while (isActive) {
                val position = activePlayer.currentPosition.coerceAtLeast(0L)
                _state.update { it.copy(positionMs = position) }
                if (position - lastPersistedPositionMs >= 2_000L) {
                    persistQueue()
                    lastPersistedPositionMs = position
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    fun initialize(permissionGranted: Boolean) {
        viewModelScope.launch {
            val hasCachedLibrary = withContext(Dispatchers.IO) { database.trackDao().getAll().isNotEmpty() }
            _state.update {
                it.copy(
                    permissionGranted = permissionGranted,
                    destination = when {
                        !permissionGranted -> Destination.PermissionIntro
                        hasCachedLibrary -> Destination.Library
                        else -> Destination.Scanning
                    },
                )
            }
            if (permissionGranted) scan(showScanningUi = !hasCachedLibrary)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update {
            it.copy(
                permissionGranted = granted,
                permissionRejected = !granted,
                destination = if (granted) Destination.Scanning else Destination.PermissionDenied,
            )
        }
        if (granted) scan()
    }

    fun useDocumentTree(uri: Uri) {
        selectedTreeUri = uri
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { preferences -> preferences[TREE_URI] = uri.toString() }
        }
        _state.update { it.copy(permissionRejected = false, destination = Destination.Scanning) }
        scan()
    }

    fun useLyricsFolder(uri: Uri) {
        selectedLyricsTreeUri = uri
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { preferences -> preferences[LYRICS_FOLDER_URI] = uri.toString() }
        }
        scan()
    }

    fun scan(showScanningUi: Boolean = _state.value.songs.isEmpty()) {
        viewModelScope.launch {
            val previousState = _state.value
            val isRefresh = !showScanningUi
            val previousKeys = previousState.songs.mapTo(mutableSetOf()) { it.trackKey }
            _state.update {
                it.copy(
                    isScanning = true,
                    scanError = null,
                    destination = if (showScanningUi) Destination.Scanning else it.destination,
                    showQueue = false,
                    moreTarget = null,
                    playlistTargetSong = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val scanned = selectedTreeUri?.let { repository.scanDocumentTree(it, _state.value.includeShortAudio) }
                        ?: repository.scanMediaStore(_state.value.includeShortAudio)
                    val externalLyrics = selectedLyricsTreeUri?.let(repository::scanLyricsTree).orEmpty()
                    scanned.map { song ->
                        val key = listOf(song.displayName.substringBeforeLast('.'), song.title)
                            .asSequence()
                            .map(::normalizeLyricsKey)
                            .mapNotNull(externalLyrics::get)
                            .firstOrNull()
                        song.copy(localLyricsUri = song.localLyricsUri ?: key)
                    }
                }
            }.onSuccess { songs ->
                withContext(Dispatchers.IO) { libraryRepository.replaceScan(songs) }
                viewModelScope.launch(Dispatchers.IO) {
                    val embeddedArtwork = repository.extractEmbeddedArtwork(songs)
                    if (embeddedArtwork.isNotEmpty()) {
                        database.trackDao().upsertAll(
                            songs.mapNotNull { song ->
                                embeddedArtwork[song.trackKey]?.let { artworkUri ->
                                    song.copy(localArtworkUri = artworkUri).toTrackEntity()
                                }
                            },
                        )
                    }
                }
                withContext(Dispatchers.IO) {
                    songs.forEach { song ->
                        val localLyrics = song.localLyricsUri?.let(repository::readLyrics) ?: return@forEach
                        database.enrichmentDao().upsert(
                            EnrichmentEntity(
                                trackKey = song.trackKey,
                                lyricsRaw = localLyrics,
                                lyricsSource = "local",
                                lyricsStatus = EnrichmentStatus.Ready.name,
                                audioVersion = song.audioVersion,
                            ).let { seeded ->
                                database.enrichmentDao().get(song.trackKey)?.let { existing ->
                                    if (existing.manualLyrics) existing else seeded.copy(
                                        artworkUri = existing.artworkUri,
                                        artworkSource = existing.artworkSource,
                                        artworkStatus = existing.artworkStatus,
                                        manualArtwork = existing.manualArtwork,
                                        candidateArtworkUri = existing.candidateArtworkUri,
                                        candidateLyricsRaw = null,
                                        candidateSource = existing.candidateSource,
                                        confidence = existing.confidence,
                                    )
                                } ?: seeded
                            },
                        )
                    }
                }
                val scannedKeys = songs.mapTo(mutableSetOf()) { it.trackKey }
                val addedCount = (scannedKeys - previousKeys).size
                val removedCount = (previousKeys - scannedKeys).size
                _state.update {
                    it.copy(
                        isScanning = false,
                        songs = songs,
                        destination = if (showScanningUi) Destination.Library else previousState.destination,
                        selectedTab = if (showScanningUi) LibraryTab.Songs else it.selectedTab,
                        snackbar = if (isRefresh) {
                            when {
                                addedCount == 0 && removedCount == 0 -> "扫描完成，曲库已是最新"
                                else -> "扫描完成：新增 $addedCount 首，移除 $removedCount 首"
                            }
                        } else {
                            null
                        },
                        showOnlineConsent = !it.onlineConsentAsked && songs.isNotEmpty(),
                    )
                }
                getApplication<Application>().settingsDataStore.edit { it[LAST_SCANNED_AT] = System.currentTimeMillis() }
                if (_state.value.onlineEnabled) EnrichmentScheduler.schedule(getApplication(), _state.value.wifiOnly.not())
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isScanning = false,
                        scanError = error.message ?: "扫描未完成",
                        destination = if (showScanningUi) Destination.PermissionDenied else previousState.destination,
                        snackbar = if (isRefresh) "重新扫描失败：${error.message ?: "未知错误"}" else null,
                    )
                }
            }
        }
    }

    fun navigate(destination: Destination) {
        _state.update { it.copy(destination = destination, showQueue = false, moreTarget = null, playlistTargetSong = null) }
    }

    fun goBack() {
        _state.update {
            it.copy(
                destination = when (it.destination) {
                    Destination.Player, Destination.Lyrics -> Destination.Library
                    is Destination.AlbumDetail, is Destination.ArtistDetail, is Destination.FolderDetail -> Destination.Library
                    is Destination.PlaylistDetail -> Destination.Playlists
                    Destination.Search -> Destination.Library
                    else -> Destination.Library
                },
                showQueue = false,
                moreTarget = null,
                playlistTargetSong = null,
            )
        }
    }

    fun selectTab(tab: LibraryTab) {
        _state.update { it.copy(selectedTab = tab, destination = Destination.Library) }
    }

    fun setQuery(query: String) {
        queryFlow.value = query
        _state.update { it.copy(query = query) }
    }

    fun toggleAlbumLayout() {
        _state.update { it.copy(albumLayout = it.albumLayout.next()) }
    }

    fun toggleArtistLayout() {
        _state.update { it.copy(artistLayout = it.artistLayout.next()) }
    }

    fun play(song: LocalSong, source: List<LocalSong> = _state.value.songs) {
        val activePlayer = player ?: run {
            _state.update { it.copy(snackbar = "播放器正在准备，请稍后再试") }
            return
        }
        val queue = source.ifEmpty { listOf(song) }
        val index = queue.indexOfFirst { it.trackKey == song.trackKey }.coerceAtLeast(0)
        activePlayer.setMediaItems(queue.map { it.toMediaItem(getApplication()) }, index, 0L)
        activePlayer.prepare()
        activePlayer.play()
        _state.update { it.copy(queue = queue, currentSong = queue[index], positionMs = 0L, destination = Destination.Player) }
        persistQueue()
    }

    fun togglePlayback() {
        val activePlayer = player ?: run {
            _state.update { it.copy(snackbar = "播放器正在准备，请稍后再试") }
            return
        }
        if (activePlayer.currentMediaItem == null) {
            _state.value.songs.firstOrNull()?.let(::play)
            return
        }
        if (activePlayer.isPlaying) activePlayer.pause() else activePlayer.play()
    }

    fun skipNext() {
        player?.let { activePlayer ->
            if (activePlayer.hasNextMediaItem()) activePlayer.seekToNext() else activePlayer.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        player?.let { activePlayer ->
            if (activePlayer.currentPosition > 3_000L) activePlayer.seekTo(0L) else if (activePlayer.hasPreviousMediaItem()) activePlayer.seekToPrevious()
        }
    }

    fun seekTo(positionMs: Long) {
        val position = positionMs.coerceAtLeast(0L)
        player?.seekTo(position)
        _state.update { it.copy(positionMs = position) }
        persistQueue()
    }

    fun cyclePlaybackMode() {
        val mode = _state.value.playbackMode.next()
        player?.let { applyPlaybackMode(it, mode) }
        _state.update { it.copy(playbackMode = mode, snackbar = mode.label) }
        viewModelScope.launch { getApplication<Application>().settingsDataStore.edit { it[PLAYBACK_MODE] = mode.ordinal } }
    }

    fun toggleFavorite(song: LocalSong) {
        viewModelScope.launch {
            val isFavorite = _state.value.favoriteTrackKeys.contains(song.trackKey)
            if (isFavorite) database.collectionDao().removeFavorite(song.trackKey)
            else database.collectionDao().addFavorite(com.yinqi.player.data.FavoriteEntity(song.trackKey))
            _state.update { it.copy(snackbar = if (isFavorite) "已移出喜欢的音乐" else "已加入喜欢的音乐") }
        }
    }

    fun showQueue() {
        _state.update { it.copy(showQueue = true, moreTarget = null) }
    }

    fun showMore() {
        _state.update { it.copy(moreTarget = MoreTarget.Screen(it.destination), showQueue = false) }
    }

    fun showSongMore(song: LocalSong) {
        _state.update { it.copy(moreTarget = MoreTarget.Song(song), showQueue = false) }
    }

    fun showLyricsFolderMore() {
        _state.update { it.copy(moreTarget = MoreTarget.LyricsFolder, showQueue = false) }
    }

    fun showSupportedFormatsMore() {
        _state.update { it.copy(moreTarget = MoreTarget.SupportedFormats, showQueue = false) }
    }

    fun dismissOverlay() {
        _state.update { it.copy(showQueue = false, moreTarget = null, playlistTargetSong = null) }
    }

    fun createPlaylist(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) {
            _state.update { it.copy(snackbar = "请输入歌单名称") }
            return
        }
        _state.update {
            if (it.playlists.any { playlist -> playlist.name.equals(normalizedName, ignoreCase = true) } || normalizedName == "喜欢的音乐") {
                it.copy(snackbar = "歌单名称已存在")
            } else {
                it.copy(
                    snackbar = "已创建歌单「$normalizedName」",
                )
            }
        }
        viewModelScope.launch {
            runCatching { database.collectionDao().insertPlaylist(com.yinqi.player.data.PlaylistEntity(name = normalizedName)) }
                .onFailure { _state.update { it.copy(snackbar = "歌单名称已存在") } }
        }
    }

    fun showPlaylistPicker(song: LocalSong) {
        _state.update { it.copy(playlistTargetSong = song, moreTarget = null, showQueue = false) }
    }

    fun addSongToPlaylist(name: String) {
        val song = _state.value.playlistTargetSong ?: return
        if (name == "喜欢的音乐") {
            if (song.trackKey !in _state.value.favoriteTrackKeys) toggleFavorite(song)
            _state.update { it.copy(playlistTargetSong = null) }
            return
        }
        _state.update {
            val playlist = it.playlists.firstOrNull { item -> item.name == name }
            when {
                playlist == null -> it.copy(playlistTargetSong = null, snackbar = "歌单不存在")
                song.trackKey in playlist.songKeys -> it.copy(playlistTargetSong = null, snackbar = "歌曲已在「$name」中")
                else -> it.copy(playlistTargetSong = null, snackbar = "已加入「$name」")
            }
        }
        viewModelScope.launch {
            database.collectionDao().getPlaylist(name)?.let { playlist ->
                val current = _state.value.playlists.firstOrNull { it.name == name }
                val position = current?.songKeys?.size ?: 0
                database.collectionDao().upsertPlaylistTracks(listOf(com.yinqi.player.data.PlaylistTrackEntity(playlist.id, song.trackKey, position)))
            }
        }
    }

    fun playAll(songs: List<LocalSong>, shuffled: Boolean = false) {
        val first = songs.firstOrNull() ?: return
        val mode = if (shuffled) PlaybackMode.Shuffle else PlaybackMode.Sequential
        player?.let { applyPlaybackMode(it, mode) }
        _state.update { it.copy(playbackMode = mode) }
        viewModelScope.launch { getApplication<Application>().settingsDataStore.edit { it[PLAYBACK_MODE] = mode.ordinal } }
        play(first, songs)
    }

    fun addToQueue(song: LocalSong) {
        val activePlayer = player ?: return
        if (_state.value.queue.any { it.trackKey == song.trackKey }) {
            _state.update { it.copy(snackbar = "歌曲已在播放队列中") }
            return
        }
        activePlayer.addMediaItem(song.toMediaItem(getApplication()))
        _state.update { it.copy(queue = it.queue + song, snackbar = "已添加到播放队列") }
        persistQueue()
    }

    fun showMessage(message: String) {
        _state.update { it.copy(snackbar = message, moreTarget = null) }
    }

    fun removeFromQueue(song: LocalSong) {
        val queue = _state.value.queue
        val index = queue.indexOfFirst { it.trackKey == song.trackKey }
        if (index >= 0) player?.removeMediaItem(index)
        _state.update { it.copy(queue = it.queue.filterNot { queued -> queued.trackKey == song.trackKey }) }
        persistQueue()
    }

    fun toggleDynamicBackdrop() {
        val value = !_state.value.dynamicBackdrop
        _state.update { it.copy(dynamicBackdrop = value) }
        viewModelScope.launch { getApplication<Application>().settingsDataStore.edit { it[DYNAMIC_BACKDROP] = value } }
    }

    fun toggleRestoreQueue() {
        val value = !_state.value.restoreQueue
        _state.update { it.copy(restoreQueue = value) }
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit {
                it[RESTORE_QUEUE] = value
                if (!value) {
                    it.remove(QUEUE_KEYS)
                    it.remove(QUEUE_CURRENT_KEY)
                    it.remove(QUEUE_POSITION_MS)
                }
            }
        }
    }

    fun toggleIncludeShortAudio() {
        val value = !_state.value.includeShortAudio
        _state.update { it.copy(includeShortAudio = value) }
        viewModelScope.launch { getApplication<Application>().settingsDataStore.edit { it[INCLUDE_SHORT_AUDIO] = value } }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun acceptOnlineConsent() {
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { preferences ->
                preferences[ONLINE_CONSENT_ASKED] = true
                preferences[ONLINE_ENABLED] = true
                preferences[WIFI_ONLY] = true
            }
            _state.update { it.copy(showOnlineConsent = false, onlineConsentAsked = true, onlineEnabled = true, wifiOnly = true) }
            EnrichmentScheduler.schedule(getApplication())
        }
    }

    fun declineOnlineConsent() {
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { preferences ->
                preferences[ONLINE_CONSENT_ASKED] = true
                preferences[ONLINE_ENABLED] = false
            }
            _state.update { it.copy(showOnlineConsent = false, onlineConsentAsked = true, onlineEnabled = false) }
        }
    }

    fun toggleOnlineEnabled() {
        val enabled = !_state.value.onlineEnabled
        if (enabled && !_state.value.onlineConsentAsked) {
            _state.update { it.copy(showOnlineConsent = true) }
            return
        }
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { preferences ->
                preferences[ONLINE_CONSENT_ASKED] = true
                preferences[ONLINE_ENABLED] = enabled
            }
            _state.update { it.copy(onlineConsentAsked = true, onlineEnabled = enabled) }
            if (enabled) {
                EnrichmentScheduler.schedule(getApplication(), _state.value.wifiOnly.not())
            } else {
                WorkManager.getInstance(getApplication()).cancelUniqueWork(EnrichmentScheduler.WORK_NAME)
            }
        }
    }

    fun toggleWifiOnly() {
        val wifiOnly = !_state.value.wifiOnly
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { preferences -> preferences[WIFI_ONLY] = wifiOnly }
            _state.update { it.copy(wifiOnly = wifiOnly) }
        }
    }

    fun retryEnrichment(allowMetered: Boolean = false) {
        if (!_state.value.onlineConsentAsked || !_state.value.onlineEnabled) {
            _state.update { it.copy(showOnlineConsent = true) }
            return
        }
        viewModelScope.launch {
            database.enrichmentDao().resetPendingBulk(EnrichmentStatus.Missing.name)
            WorkManager.getInstance(getApplication()).cancelUniqueWork(EnrichmentScheduler.WORK_NAME)
            EnrichmentScheduler.schedule(getApplication(), allowMetered, replace = true)
        }
    }

    fun clearOnlineContent() {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).cancelUniqueWork(EnrichmentScheduler.WORK_NAME)
            database.enrichmentDao().clearOnlineContentBulk(EnrichmentStatus.Missing.name)
            val protectedFiles = database.enrichmentDao().getAll()
                .filter { it.manualArtwork }
                .mapNotNull { it.artworkUri?.let(Uri::parse)?.path }
                .toSet()
            File(getApplication<Application>().filesDir, "artwork/remote").listFiles().orEmpty()
                .filterNot { it.absolutePath in protectedFiles }
                .forEach { it.deleteRecursively() }
        }
    }

    fun lyricsFor(song: LocalSong?): LyricsDocument? = song?.let { _state.value.lyricsByTrackKey[it.trackKey] }

    fun showEnrichmentReview() {
        _state.update { it.copy(showEnrichmentReview = true) }
    }

    fun dismissEnrichmentReview() {
        _state.update { it.copy(showEnrichmentReview = false) }
    }

    fun acceptArtworkCandidate(trackKey: String) {
        viewModelScope.launch {
            database.enrichmentDao().get(trackKey)?.let { item ->
                item.candidateArtworkUri?.let { candidate ->
                    database.enrichmentDao().upsert(
                        item.copy(
                            artworkUri = candidate,
                            artworkSource = item.candidateSource,
                            artworkStatus = EnrichmentStatus.Ready.name,
                            manualArtwork = true,
                            candidateArtworkUri = null,
                            candidateSource = null,
                        ),
                    )
                }
            }
        }
    }

    fun acceptLyricsCandidate(trackKey: String) {
        viewModelScope.launch {
            database.enrichmentDao().get(trackKey)?.let { item ->
                item.candidateLyricsRaw?.let { candidate ->
                    database.enrichmentDao().upsert(
                        item.copy(
                            lyricsRaw = candidate,
                            lyricsStatus = EnrichmentStatus.Ready.name,
                            manualLyrics = true,
                            candidateLyricsRaw = null,
                            candidateSource = null,
                        ),
                    )
                }
            }
        }
    }

    override fun onCleared() {
        persistQueue()
        stopPositionUpdates()
        player?.let { activePlayer -> playerListener?.let(activePlayer::removeListener) }
        controllerFuture.cancel(true)
        player?.release()
        super.onCleared()
    }

    private fun maybeRestoreQueue() {
        val activePlayer = player ?: return
        val current = _state.value
        if (queueRestored || !current.restoreQueue || persistedQueueKeys.isEmpty() || current.songs.isEmpty()) return
        val songsByKey = current.songs.associateBy { it.trackKey }
        val restoredQueue = persistedQueueKeys.mapNotNull(songsByKey::get)
        if (restoredQueue.isEmpty()) {
            queueRestored = true
            return
        }
        val startIndex = restoredQueue.indexOfFirst { it.trackKey == persistedCurrentKey }.coerceAtLeast(0)
        activePlayer.setMediaItems(restoredQueue.map { it.toMediaItem(getApplication()) }, startIndex, persistedPositionMs)
        activePlayer.prepare()
        _state.update {
            it.copy(
                queue = restoredQueue,
                currentSong = restoredQueue[startIndex],
                positionMs = persistedPositionMs,
            )
        }
        queueRestored = true
    }

    private fun persistQueue() {
        val current = _state.value
        if (!current.restoreQueue) return
        val position = player?.currentPosition?.coerceAtLeast(0L) ?: current.positionMs
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { preferences ->
                preferences[QUEUE_KEYS] = current.queue.joinToString(QUEUE_SEPARATOR) { it.trackKey }
                current.currentSong?.let { preferences[QUEUE_CURRENT_KEY] = it.trackKey }
                preferences[QUEUE_POSITION_MS] = position
            }
        }
    }

    private fun applyPlaybackMode(activePlayer: Player, mode: PlaybackMode) {
        activePlayer.repeatMode = mode.repeatMode
        activePlayer.shuffleModeEnabled = mode.shuffleEnabled
    }

    companion object {
        private val ONLINE_CONSENT_ASKED = booleanPreferencesKey("online_consent_asked")
        private val ONLINE_ENABLED = booleanPreferencesKey("online_enabled")
        private val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val DYNAMIC_BACKDROP = booleanPreferencesKey("dynamic_backdrop")
        private val RESTORE_QUEUE = booleanPreferencesKey("restore_queue")
        private val INCLUDE_SHORT_AUDIO = booleanPreferencesKey("include_short_audio")
        private val TREE_URI = stringPreferencesKey("tree_uri")
        private val LYRICS_FOLDER_URI = stringPreferencesKey("lyrics_folder_uri")
        private val PLAYBACK_MODE = intPreferencesKey("playback_mode")
        private val QUEUE_KEYS = stringPreferencesKey("queue_track_keys")
        private val QUEUE_CURRENT_KEY = stringPreferencesKey("queue_current_key")
        private val QUEUE_POSITION_MS = androidx.datastore.preferences.core.longPreferencesKey("queue_position_ms")
        private val LAST_SCANNED_AT = androidx.datastore.preferences.core.longPreferencesKey("last_scanned_at")
        private const val QUEUE_SEPARATOR = "\u0001"

        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MusicViewModel(application) as T
            }
    }
}

private fun normalizeLyricsKey(value: String): String = TextNormalizer.normalizeLyricsKey(value)

private fun LocalSong.toMediaItem(context: Application): MediaItem =
    MediaItem.Builder()
        .setMediaId(trackKey)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(effectiveArtworkUri?.toMediaUri(context))
                .build(),
        )
        .build()

private fun Uri.toMediaUri(context: Application): Uri = if (scheme == "file") {
    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(requireNotNull(path)))
} else {
    this
}
