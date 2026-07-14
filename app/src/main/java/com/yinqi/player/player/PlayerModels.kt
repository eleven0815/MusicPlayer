package com.yinqi.player.player

import androidx.media3.common.Player
import com.yinqi.player.data.EnrichmentEntity
import com.yinqi.player.data.LocalSong
import com.yinqi.player.data.LyricsDocument

enum class LibraryTab { Songs, Albums, Artists, Folders }

enum class CollectionLayout {
    Grid,
    List,
    ;

    fun next(): CollectionLayout = entries[(ordinal + 1) % entries.size]
}

data class UserPlaylist(
    val name: String,
    val songKeys: Set<String> = emptySet(),
)

enum class PlaybackMode(
    val label: String,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
) {
    Sequential("顺序播放", Player.REPEAT_MODE_OFF, false),
    RepeatOne("单曲循环", Player.REPEAT_MODE_ONE, false),
    Shuffle("随机播放", Player.REPEAT_MODE_OFF, true),
    ;

    fun next(): PlaybackMode = entries[(ordinal + 1) % entries.size]
}

sealed interface Destination {
    data object PermissionIntro : Destination
    data object PermissionDenied : Destination
    data object Scanning : Destination
    data object Library : Destination
    data object Search : Destination
    data object Albums : Destination
    data object Artists : Destination
    data object Folders : Destination
    data object Playlists : Destination
    data object Settings : Destination
    data class AlbumDetail(val album: String) : Destination
    data class ArtistDetail(val artist: String) : Destination
    data class FolderDetail(val folder: String) : Destination
    data class PlaylistDetail(val name: String) : Destination
    data object Player : Destination
    data object Lyrics : Destination
}

sealed interface MoreTarget {
    data class Song(val song: LocalSong) : MoreTarget
    data class Screen(val destination: Destination) : MoreTarget
    data object LyricsFolder : MoreTarget
    data object SupportedFormats : MoreTarget
}

data class MusicUiState(
    val permissionGranted: Boolean = false,
    val permissionRejected: Boolean = false,
    val isScanning: Boolean = false,
    val scanError: String? = null,
    val songs: List<LocalSong> = emptyList(),
    val selectedTab: LibraryTab = LibraryTab.Songs,
    val albumLayout: CollectionLayout = CollectionLayout.Grid,
    val artistLayout: CollectionLayout = CollectionLayout.Grid,
    val destination: Destination = Destination.PermissionIntro,
    val query: String = "",
    val debouncedQuery: String = "",
    val queue: List<LocalSong> = emptyList(),
    val currentSong: LocalSong? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.Sequential,
    val favoriteTrackKeys: Set<String> = emptySet(),
    val playlists: List<UserPlaylist> = emptyList(),
    val playlistTargetSong: LocalSong? = null,
    val showQueue: Boolean = false,
    val moreTarget: MoreTarget? = null,
    val dynamicBackdrop: Boolean = true,
    val restoreQueue: Boolean = true,
    val includeShortAudio: Boolean = false,
    val lastScannedAtMillis: Long? = null,
    val lyricsByTrackKey: Map<String, LyricsDocument> = emptyMap(),
    val enrichmentByTrackKey: Map<String, EnrichmentEntity> = emptyMap(),
    val onlineConsentAsked: Boolean = false,
    val onlineEnabled: Boolean = false,
    val wifiOnly: Boolean = true,
    val enrichmentWorkRunning: Boolean = false,
    val showOnlineConsent: Boolean = false,
    val showEnrichmentReview: Boolean = false,
    val snackbar: String? = null,
)
