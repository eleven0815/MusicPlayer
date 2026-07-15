package com.yinqi.player.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.yinqi.player.data.EnrichmentStatus
import com.yinqi.player.data.LyricsDocument
import com.yinqi.player.data.LocalSong
import com.yinqi.player.player.Destination
import com.yinqi.player.player.CollectionLayout
import com.yinqi.player.player.LibraryTab
import com.yinqi.player.player.MusicUiState
import com.yinqi.player.player.MusicViewModel
import com.yinqi.player.player.MoreTarget
import com.yinqi.player.player.PlaybackMode
import com.yinqi.player.ui.theme.Brand
import com.yinqi.player.ui.theme.BrandDeep
import com.yinqi.player.ui.theme.BrandSoft
import com.yinqi.player.ui.theme.Canvas
import com.yinqi.player.ui.theme.Ink
import com.yinqi.player.ui.theme.PlayerCanvas
import com.yinqi.player.ui.theme.PlayerSecondary
import com.yinqi.player.ui.theme.PlayerSurface
import com.yinqi.player.ui.theme.SecondaryInk
import com.yinqi.player.ui.theme.Surface
import com.yinqi.player.ui.theme.SurfaceSubtle
import com.yinqi.player.ui.theme.YinqiTheme
import com.yinqi.player.ui.components.AlbumArtwork
import com.yinqi.player.ui.components.BottomTabs
import com.yinqi.player.ui.components.MiniPlayer
import com.yinqi.player.ui.components.rememberArtworkColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@Composable
fun MusicApp(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onRequestPermission: () -> Unit,
    onPickFolder: () -> Unit,
    onPickLyricsFolder: () -> Unit,
) {
    val darkPlayer = state.destination == Destination.Player || state.destination == Destination.Lyrics
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    YinqiTheme(dark = darkPlayer) {
        BackHandler(enabled = state.destination !in setOf(Destination.Library, Destination.PermissionIntro)) {
            viewModel.goBack()
        }
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.destination,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                        androidx.compose.animation.fadeOut(tween(140))
                },
                label = "screen",
            ) { destination ->
                when (destination) {
                    Destination.PermissionIntro -> PermissionIntroScreen(onRequestPermission, onPickFolder)
                    Destination.PermissionDenied -> PermissionDeniedScreen(onRequestPermission, onPickFolder)
                    Destination.Scanning -> ScanningScreen(state, viewModel::scan)
                    Destination.Library -> LibraryScreen(state, viewModel)
                    Destination.Search -> SearchScreen(state, viewModel)
                    Destination.Albums -> AlbumsScreen(state, viewModel)
                    Destination.Artists -> ArtistsScreen(state, viewModel)
                    Destination.Folders -> FoldersScreen(state, viewModel)
                    Destination.Playlists -> PlaylistsScreen(state, viewModel)
                    Destination.Settings -> SettingsScreen(state, viewModel)
                    is Destination.AlbumDetail -> AlbumDetailScreen(state, destination.album, viewModel)
                    is Destination.ArtistDetail -> ArtistDetailScreen(state, destination.artist, viewModel)
                    is Destination.FolderDetail -> FolderDetailScreen(state, destination.folder, viewModel)
                    is Destination.PlaylistDetail -> PlaylistDetailScreen(state, destination.name, viewModel)
                    Destination.Player -> PlayerScreen(state, viewModel, lyrics = false)
                    Destination.Lyrics -> PlayerScreen(state, viewModel, lyrics = true)
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) { data ->
                Snackbar(
                    containerColor = PlayerSurface,
                    contentColor = Surface,
                    actionColor = Brand,
                    snackbarData = data,
                )
            }

            if (state.showQueue) {
                QueueSheet(state, viewModel)
            }
            if (state.moreTarget != null) {
                MoreSheet(state, viewModel, onPickLyricsFolder)
            }
            if (state.playlistTargetSong != null) {
                PlaylistPickerSheet(state, viewModel)
            }
            if (state.showOnlineConsent) {
                OnlineConsentDialog(viewModel)
            }
            if (state.showEnrichmentReview) {
                EnrichmentReviewSheet(state, viewModel)
            }
        }
    }
}

@Composable
private fun OnlineConsentDialog(viewModel: MusicViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::declineOnlineConsent,
        title = { Text("开启歌词与封面补全") },
        text = {
            Text("扫描完成后，音栖会在 Wi‑Fi 下查询歌曲名、歌手、专辑和时长，不上传音频文件或本地路径。音频指纹仍处于可选技术验证阶段。")
        },
        confirmButton = {
            TextButton(onClick = viewModel::acceptOnlineConsent) { Text("同意并开启", color = BrandDeep) }
        },
        dismissButton = {
            TextButton(onClick = viewModel::declineOnlineConsent) { Text("暂不开启", color = SecondaryInk) }
        },
    )
}

@Composable
private fun PermissionIntroScreen(
    onRequestPermission: () -> Unit,
    onPickFolder: () -> Unit,
) {
    Scaffold(containerColor = Canvas) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(84.dp))
            BrandMark(112.dp)
            Spacer(Modifier.height(26.dp))
            Text("只听见你的本地收藏", color = BrandDeep, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Text("欢迎来到音栖", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text(
                "扫描设备里的音乐，离线播放，不上传、不打扰。",
                color = SecondaryInk,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.weight(1f))
            BrandButton("允许访问本地音乐", Modifier.fillMaxWidth(), onRequestPermission)
            TextButton(onClick = onPickFolder) {
                Text("暂不授权，选择文件或文件夹", color = BrandDeep, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            Text("音频始终保留在设备上", color = SecondaryInk, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PermissionDeniedScreen(
    onRequestPermission: () -> Unit,
    onPickFolder: () -> Unit,
) {
    EmptyStateScreen(
        icon = Icons.Filled.Folder,
        title = "无法访问设备音乐",
        body = "你仍可以通过系统文件选择器添加文件或文件夹。",
        action = "选择文件夹",
        onAction = onPickFolder,
        secondaryAction = "再次请求系统权限",
        onSecondaryAction = onRequestPermission,
    )
}

@Composable
private fun ScanningScreen(state: MusicUiState, onRetry: () -> Unit) {
    Scaffold(containerColor = Canvas) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("正在扫描设备音乐", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (state.songs.isEmpty()) "正在建立本地曲库" else "已发现 ${state.songs.size} 首",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandDeep,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("/Music / Download / Recordings", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SecondaryInk)
                    Spacer(Modifier.height(26.dp))
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(BrandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Brand, strokeWidth = 3.dp)
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("正在读取标签与封面，约需几秒钟", color = SecondaryInk, fontSize = 13.sp)
                    if (state.songs.isEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceSubtle),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = BrandSoft, contentColor = BrandDeep),
                shape = RoundedCornerShape(99.dp),
            ) {
                Text("重新扫描")
            }
        }
    }
}

@Composable
private fun EmptyStateScreen(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Scaffold(containerColor = Canvas) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = BrandDeep, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text(body, color = SecondaryInk, textAlign = TextAlign.Center, lineHeight = 21.sp)
            Spacer(Modifier.height(20.dp))
            BrandButton(action, Modifier.widthIn(min = 152.dp), onAction)
            if (secondaryAction != null && onSecondaryAction != null) {
                TextButton(onClick = onSecondaryAction) {
                    Text(secondaryAction, color = BrandDeep)
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(state: MusicUiState, viewModel: MusicViewModel) {
    if (state.songs.isEmpty() && !state.isScanning) {
        EmptyStateScreen(
            icon = Icons.Filled.Album,
            title = "这里还没有音乐",
            body = "扫描设备上的音频文件，或手动选择一个音乐文件夹。",
            action = "开始扫描",
            onAction = viewModel::scan,
        )
        return
    }

    LibraryScaffold(
        state = state,
        viewModel = viewModel,
        title = when (state.selectedTab) {
            LibraryTab.Songs -> "本地音乐"
            LibraryTab.Albums -> "专辑"
            LibraryTab.Artists -> "歌手"
            LibraryTab.Folders -> "文件夹"
        },
        onSearch = { viewModel.navigate(Destination.Search) },
    ) { contentPadding ->
        when (state.selectedTab) {
            LibraryTab.Songs -> SongsHome(state, viewModel, contentPadding)
            LibraryTab.Albums -> AlbumGrid(state, viewModel, contentPadding)
            LibraryTab.Artists -> ArtistGrid(state, viewModel, contentPadding)
            LibraryTab.Folders -> FolderList(state, viewModel, contentPadding)
        }
    }
}

@Composable
private fun LibraryScaffold(
    state: MusicUiState,
    viewModel: MusicViewModel,
    title: String,
    onSearch: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Canvas,
        topBar = {
            LibraryTopBar(title, state.isScanning, onSearch, viewModel::showMore)
        },
        bottomBar = {
            Column {
                MiniPlayer(state, viewModel)
                BottomTabs(state.destination, viewModel)
            }
        },
    ) { padding -> content(padding) }
}

@Composable
private fun LibraryTopBar(title: String, isScanning: Boolean, onSearch: () -> Unit, onMore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Canvas)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Brand,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "搜索", tint = Ink) }
        IconButton(onClick = onMore) { Icon(Icons.Filled.MoreVert, "更多", tint = Ink) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsHome(
    state: MusicUiState,
    viewModel: MusicViewModel,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    PullToRefreshBox(
        isRefreshing = state.isScanning,
        onRefresh = viewModel::scan,
        state = rememberPullToRefreshState(),
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                EnrichmentSummaryCard(state, viewModel)
                if (state.enrichmentByTrackKey.isNotEmpty()) Spacer(Modifier.height(12.dp))
                LibrarySummary(state.songs, state.lastScannedAtMillis)
                Spacer(Modifier.height(16.dp))
                CategoryTabs(state.selectedTab, viewModel::selectTab)
                Spacer(Modifier.height(18.dp))
                Text("最近添加", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            items(state.songs, key = { it.trackKey }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.trackKey == state.currentSong?.trackKey,
                    isFavorite = song.trackKey in state.favoriteTrackKeys,
                    onClick = { viewModel.play(song, state.songs) },
                    onMore = { viewModel.showSongMore(song) },
                )
            }
        }
    }
}

@Composable
private fun EnrichmentSummaryCard(state: MusicUiState, viewModel: MusicViewModel) {
    if (!state.onlineEnabled || state.enrichmentByTrackKey.isEmpty()) return
    val ready = state.enrichmentByTrackKey.values.count {
        it.artworkStatus == EnrichmentStatus.Ready.name && it.lyricsStatus == EnrichmentStatus.Ready.name
    }
    val total = state.enrichmentByTrackKey.size
    val pending = state.enrichmentByTrackKey.values.count {
        it.artworkStatus in setOf(EnrichmentStatus.Missing.name, EnrichmentStatus.Resolving.name, EnrichmentStatus.Failed.name) ||
            it.lyricsStatus in setOf(EnrichmentStatus.Missing.name, EnrichmentStatus.Resolving.name, EnrichmentStatus.Failed.name)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Refresh, null, tint = BrandDeep, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        pending > 0 && state.enrichmentWorkRunning -> "正在补全 $pending 首歌曲"
                        pending > 0 -> "等待补全 $pending 首歌曲"
                        else -> "歌词与封面已更新"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                if (pending > 0) {
                    Text("$ready/$total", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SecondaryInk)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "本地音乐不受联网状态影响",
                fontSize = 12.sp,
                color = SecondaryInk,
            )
            if (pending > 0) {
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { viewModel.retryEnrichment() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text("重试缺失内容", color = BrandDeep)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnrichmentReviewSheet(state: MusicUiState, viewModel: MusicViewModel) {
    val entries = state.enrichmentByTrackKey.values.filter {
        it.artworkStatus == EnrichmentStatus.ReviewRequired.name || it.lyricsStatus == EnrichmentStatus.ReviewRequired.name
    }
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissEnrichmentReview,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("待确认 · ${entries.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (entries.isNotEmpty()) {
                TextButton(
                    onClick = viewModel::acceptAllEnrichmentCandidates,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text("接受全部候选", color = BrandDeep)
                }
                Spacer(Modifier.height(4.dp))
            }
            entries.forEach { item ->
                val song = state.songs.firstOrNull { it.trackKey == item.trackKey } ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val candidateSong = item.candidateArtworkUri?.let { song.copy(remoteArtworkUri = Uri.parse(it)) } ?: song
                    AlbumArtwork(candidateSong, 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, fontSize = 12.sp, color = SecondaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                item.candidateArtworkUri != null && item.candidateLyricsRaw != null -> "封面与歌词候选"
                                item.candidateArtworkUri != null -> "封面候选"
                                else -> "歌词候选"
                            },
                            fontSize = 11.sp,
                            color = BrandDeep,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (item.candidateArtworkUri != null) {
                            TextButton(onClick = { viewModel.acceptArtworkCandidate(item.trackKey) }) { Text("用封面", color = BrandDeep) }
                        }
                        if (item.candidateLyricsRaw != null) {
                            TextButton(onClick = { viewModel.acceptLyricsCandidate(item.trackKey) }) { Text("用歌词", color = BrandDeep) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySummary(songs: List<LocalSong>, lastScannedAtMillis: Long?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandSoft),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 116.dp)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("设备曲库", fontSize = 13.sp, color = BrandDeep, fontWeight = FontWeight.Medium)
                Text(songs.size.toString(), fontFamily = FontFamily.Monospace, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    "${formatDuration(songs.sumOf { it.durationMs })} · ${formatRelativeTime(lastScannedAtMillis)}",
                    fontSize = 12.sp,
                    color = SecondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                songs.take(3).forEachIndexed { index, song ->
                    AlbumArtwork(song, 72.dp, Modifier.rotate((-index * 7).toFloat()))
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(selected: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LibraryTab.entries.forEach { tab ->
            Column(
                modifier = Modifier
                    .width(if (tab == LibraryTab.Folders) 64.dp else 48.dp)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    when (tab) {
                        LibraryTab.Songs -> "歌曲"
                        LibraryTab.Albums -> "专辑"
                        LibraryTab.Artists -> "歌手"
                        LibraryTab.Folders -> "文件夹"
                    },
                    color = if (tab == selected) BrandDeep else SecondaryInk,
                    fontSize = 14.sp,
                    fontWeight = if (tab == selected) FontWeight.Medium else FontWeight.Normal,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .width(18.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (tab == selected) Brand else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun SongRow(
    song: LocalSong,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val background = if (isCurrent) BrandSoft else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(song, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isCurrent) BrandDeep else Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("${song.artist} · ${song.album}", fontSize = 12.sp, color = SecondaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatTrackDuration(song.durationMs), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (isCurrent) BrandDeep else SecondaryInk)
        IconButton(onClick = onMore) {
            Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.MoreVert, "更多", tint = if (isFavorite) BrandDeep else SecondaryInk)
        }
    }
}

@Composable
private fun AlbumGrid(
    state: MusicUiState,
    viewModel: MusicViewModel,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    val albums = remember(state.songs) { state.songs.groupBy { it.album }.toList() }
    val contentPadding = androidx.compose.foundation.layout.PaddingValues(
        start = 20.dp,
        end = 20.dp,
        top = padding.calculateTopPadding() + 16.dp,
        bottom = padding.calculateBottomPadding() + 16.dp,
    )
    if (state.albumLayout == CollectionLayout.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                CollectionLayoutHeader(state.selectedTab, state.albumLayout, viewModel::selectTab, viewModel::toggleAlbumLayout)
            }
            items(albums, key = { it.first }) { (album, songs) ->
                Column(
                    modifier = Modifier.clickable { viewModel.navigate(Destination.AlbumDetail(album)) },
                ) {
                    AlbumArtwork(songs.firstOrNull(), 176.dp, Modifier.fillMaxWidth().aspectRatio(1f))
                    Spacer(Modifier.height(8.dp))
                    Text(album, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${songs.firstOrNull()?.artist.orEmpty()} · ${songs.size} 首", fontSize = 12.sp, color = SecondaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { CollectionLayoutHeader(state.selectedTab, state.albumLayout, viewModel::selectTab, viewModel::toggleAlbumLayout) }
            items(albums, key = { it.first }) { (album, songs) ->
                CollectionListRow(
                    song = songs.firstOrNull(),
                    title = album,
                    subtitle = "${songs.firstOrNull()?.artist.orEmpty()} · ${songs.size} 首",
                    circularArtwork = false,
                ) { viewModel.navigate(Destination.AlbumDetail(album)) }
            }
        }
    }
}

@Composable
private fun ArtistGrid(
    state: MusicUiState,
    viewModel: MusicViewModel,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    val artists = remember(state.songs) { state.songs.groupBy { it.artist }.toList() }
    val contentPadding = androidx.compose.foundation.layout.PaddingValues(
        start = if (state.artistLayout == CollectionLayout.Grid) 28.dp else 20.dp,
        end = if (state.artistLayout == CollectionLayout.Grid) 28.dp else 20.dp,
        top = padding.calculateTopPadding() + 16.dp,
        bottom = padding.calculateBottomPadding() + 16.dp,
    )
    if (state.artistLayout == CollectionLayout.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                CollectionLayoutHeader(state.selectedTab, state.artistLayout, viewModel::selectTab, viewModel::toggleArtistLayout)
            }
            items(artists, key = { it.first }) { (artist, songs) ->
                Column(
                    modifier = Modifier.clickable { viewModel.navigate(Destination.ArtistDetail(artist)) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AlbumArtwork(songs.firstOrNull(), 148.dp, Modifier.clip(CircleShape))
                    Spacer(Modifier.height(10.dp))
                    Text(artist, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink, maxLines = 1)
                    Text("${songs.size} 首歌曲", fontSize = 12.sp, color = SecondaryInk)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { CollectionLayoutHeader(state.selectedTab, state.artistLayout, viewModel::selectTab, viewModel::toggleArtistLayout) }
            items(artists, key = { it.first }) { (artist, songs) ->
                CollectionListRow(
                    song = songs.firstOrNull(),
                    title = artist,
                    subtitle = "${songs.size} 首歌曲",
                    circularArtwork = true,
                ) { viewModel.navigate(Destination.ArtistDetail(artist)) }
            }
        }
    }
}

@Composable
private fun CollectionLayoutHeader(
    selectedTab: LibraryTab,
    layout: CollectionLayout,
    onSelectTab: (LibraryTab) -> Unit,
    onToggleLayout: () -> Unit,
) {
    Column {
        CategoryTabs(selectedTab, onSelectTab)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (layout == CollectionLayout.Grid) "网格展示" else "列表展示", color = SecondaryInk, fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleLayout) {
                Icon(
                    imageVector = if (layout == CollectionLayout.Grid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (layout == CollectionLayout.Grid) "切换为列表" else "切换为网格",
                    tint = BrandDeep,
                )
            }
        }
    }
}

@Composable
private fun CollectionListRow(
    song: LocalSong?,
    title: String,
    subtitle: String,
    circularArtwork: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(song, 56.dp, if (circularArtwork) Modifier.clip(CircleShape) else Modifier)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = SecondaryInk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SecondaryInk)
    }
}

@Composable
private fun FolderList(
    state: MusicUiState,
    viewModel: MusicViewModel,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    val folders = remember(state.songs) { state.songs.groupBy { it.folder }.toList() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { CategoryTabs(state.selectedTab, viewModel::selectTab) }
        items(folders, key = { it.first }) { (folder, songs) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .clickable { viewModel.navigate(Destination.FolderDetail(folder)) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Folder, null, tint = BrandDeep) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("/$folder", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Ink)
                    Text("${songs.size} 首 · ${formatDuration(songs.sumOf { it.durationMs })}", fontSize = 12.sp, color = SecondaryInk)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SecondaryInk)
            }
        }
    }
}

@Composable
private fun AlbumsScreen(state: MusicUiState, viewModel: MusicViewModel) {
    LibraryScaffold(state, viewModel, "专辑", { viewModel.navigate(Destination.Search) }) { padding ->
        AlbumGrid(state.copy(selectedTab = LibraryTab.Albums), viewModel, padding)
    }
}

@Composable
private fun ArtistsScreen(state: MusicUiState, viewModel: MusicViewModel) {
    LibraryScaffold(state, viewModel, "歌手", { viewModel.navigate(Destination.Search) }) { padding ->
        ArtistGrid(state.copy(selectedTab = LibraryTab.Artists), viewModel, padding)
    }
}

@Composable
private fun FoldersScreen(state: MusicUiState, viewModel: MusicViewModel) {
    LibraryScaffold(state, viewModel, "文件夹", { viewModel.navigate(Destination.Search) }) { padding ->
        FolderList(state.copy(selectedTab = LibraryTab.Folders), viewModel, padding)
    }
}

@Composable
private fun SearchScreen(state: MusicUiState, viewModel: MusicViewModel) {
    val results = remember(state.debouncedQuery, state.songs) {
        state.songs.filter {
            listOf(it.title, it.artist, it.album).any { value -> value.contains(state.debouncedQuery, ignoreCase = true) }
        }
    }
    Scaffold(
        containerColor = Canvas,
        topBar = {
            Row(
                modifier = Modifier
                    .background(Canvas)
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Ink) }
                Text("搜索", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
        },
        bottomBar = { MiniPlayer(state, viewModel) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索歌曲、专辑或歌手") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = if (state.query.isNotEmpty()) ({
                        IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Filled.Close, "清空") }
                    }) else null,
                    singleLine = true,
                    shape = RoundedCornerShape(99.dp),
                )
                Spacer(Modifier.height(8.dp))
                if (state.debouncedQuery.isNotBlank()) {
                    Text("找到 ${results.size} 首歌曲", color = SecondaryInk, fontSize = 13.sp)
                }
            }
            if (state.debouncedQuery.isBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("输入关键词开始搜索", color = SecondaryInk, modifier = Modifier.padding(24.dp))
                    }
                }
            } else {
                items(results, key = { it.trackKey }) { song ->
                    SongRow(song, song.trackKey == state.currentSong?.trackKey, song.trackKey in state.favoriteTrackKeys, { viewModel.play(song, results) }, { viewModel.showSongMore(song) })
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailScreen(state: MusicUiState, album: String, viewModel: MusicViewModel) {
    val songs = remember(album, state.songs) { state.songs.filter { it.album == album } }
    DetailScaffold(state, viewModel, album) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                DetailHero(songs.firstOrNull(), album, songs.firstOrNull()?.artist.orEmpty(), "${songs.size} 首 · ${formatDuration(songs.sumOf { it.durationMs })}") {
                    songs.firstOrNull()?.let { viewModel.play(it, songs) }
                }
                Spacer(Modifier.height(16.dp))
            }
            items(songs, key = { it.trackKey }) { song ->
                SongRow(song, song.trackKey == state.currentSong?.trackKey, song.trackKey in state.favoriteTrackKeys, { viewModel.play(song, songs) }, { viewModel.showSongMore(song) })
            }
        }
    }
}

@Composable
private fun ArtistDetailScreen(state: MusicUiState, artist: String, viewModel: MusicViewModel) {
    val songs = remember(artist, state.songs) { state.songs.filter { it.artist == artist } }
    DetailScaffold(state, viewModel, artist) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = BrandSoft), shape = RoundedCornerShape(24.dp)) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArtwork(songs.firstOrNull(), 124.dp, Modifier.clip(CircleShape))
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(artist, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("${songs.size} 首歌曲", fontSize = 13.sp, color = SecondaryInk)
                            Spacer(Modifier.height(12.dp))
                            BrandButton("播放全部", Modifier.width(152.dp)) { songs.firstOrNull()?.let { viewModel.play(it, songs) } }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("热门歌曲", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
            }
            items(songs, key = { it.trackKey }) { song -> SongRow(song, song.trackKey == state.currentSong?.trackKey, song.trackKey in state.favoriteTrackKeys, { viewModel.play(song, songs) }, { viewModel.showSongMore(song) }) }
        }
    }
}

@Composable
private fun FolderDetailScreen(state: MusicUiState, folder: String, viewModel: MusicViewModel) {
    val songs = remember(folder, state.songs) { state.songs.filter { it.folder == folder } }
    DetailScaffold(state, viewModel, "/$folder") { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.FolderOpen, null, tint = BrandDeep)
                    Spacer(Modifier.width(10.dp))
                    Text("/$folder", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Ink)
                }
                Spacer(Modifier.height(12.dp))
                Text("${songs.size} 首 · ${formatDuration(songs.sumOf { it.durationMs })} · 按文件名排序", color = SecondaryInk, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(songs, key = { it.trackKey }) { song -> SongRow(song, song.trackKey == state.currentSong?.trackKey, song.trackKey in state.favoriteTrackKeys, { viewModel.play(song, songs) }, { viewModel.showSongMore(song) }) }
        }
    }
}

@Composable
private fun DetailScaffold(
    state: MusicUiState,
    viewModel: MusicViewModel,
    title: String,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Canvas,
        topBar = {
            Row(
                modifier = Modifier
                    .background(Canvas)
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Ink) }
                Text(title, modifier = Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (state.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Brand, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = viewModel::showMore) { Icon(Icons.Filled.MoreVert, "更多", tint = Ink) }
            }
        },
        bottomBar = { Column { MiniPlayer(state, viewModel); BottomTabs(state.destination, viewModel) } },
        content = content,
    )
}

@Composable
private fun DetailHero(song: LocalSong?, title: String, subtitle: String, meta: String, onPlay: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AlbumArtwork(song, 156.dp)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = BrandDeep, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(meta, color = SecondaryInk, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            BrandButton("播放全部", Modifier.fillMaxWidth(), onPlay)
        }
    }
}

@Composable
private fun PlaylistsScreen(state: MusicUiState, viewModel: MusicViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    val favoriteSongs = state.songs.filter { it.trackKey in state.favoriteTrackKeys }
    val playlists = buildList {
        add(Playlist("喜欢的音乐", "${favoriteSongs.size} 首", favoriteSongs.firstOrNull()))
        state.playlists.forEach { playlist ->
            val songs = state.songs.filter { it.trackKey in playlist.songKeys }
            add(Playlist(playlist.name, "${songs.size} 首", songs.firstOrNull()))
        }
    }
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建歌单") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("歌单名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName)
                            playlistName = ""
                            showCreateDialog = false
                        }
                    },
                ) { Text("创建", color = BrandDeep) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
            },
        )
    }
    LibraryScaffold(state, viewModel, "歌单", { viewModel.navigate(Destination.Search) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 18.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("我的歌单", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, null, tint = BrandDeep)
                        Spacer(Modifier.width(4.dp))
                        Text("新建歌单", color = BrandDeep)
                    }
                }
            }
            items(playlists, key = { it.name }) { playlist ->
                Column(Modifier.clickable { viewModel.navigate(Destination.PlaylistDetail(playlist.name)) }) {
                    Box {
                        AlbumArtwork(playlist.artwork, 176.dp, Modifier.fillMaxWidth().aspectRatio(1f))
                        Text(
                            playlist.meta.substringBefore(' '),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            color = Surface,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(playlist.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Text(playlist.meta, fontSize = 12.sp, color = SecondaryInk)
                }
            }
        }
    }
}

private data class Playlist(val name: String, val meta: String, val artwork: LocalSong?)

@Composable
private fun PlaylistDetailScreen(state: MusicUiState, name: String, viewModel: MusicViewModel) {
    val songs = when (name) {
        "喜欢的音乐" -> state.songs.filter { it.trackKey in state.favoriteTrackKeys }
        else -> state.playlists.firstOrNull { it.name == name }?.let { playlist ->
            state.songs.filter { it.trackKey in playlist.songKeys }
        }.orEmpty()
    }
    DetailScaffold(state, viewModel, name) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                DetailHero(songs.firstOrNull(), name, "本地歌单", "${songs.size} 首 · ${formatDuration(songs.sumOf { it.durationMs })}") {
                    songs.firstOrNull()?.let { viewModel.play(it, songs) }
                }
                Spacer(Modifier.height(16.dp))
            }
            items(songs, key = { it.trackKey }) { song -> SongRow(song, song.trackKey == state.currentSong?.trackKey, song.trackKey in state.favoriteTrackKeys, { viewModel.play(song, songs) }, { viewModel.showSongMore(song) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistPickerSheet(state: MusicUiState, viewModel: MusicViewModel) {
    val song = state.playlistTargetSong ?: return
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissOverlay,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("将「${song.title}」加入歌单", fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            MoreAction(Icons.Filled.Favorite, "喜欢的音乐") { viewModel.addSongToPlaylist("喜欢的音乐") }
            state.playlists.forEach { playlist ->
                MoreAction(Icons.AutoMirrored.Outlined.PlaylistPlay, "${playlist.name} · ${playlist.songKeys.size} 首") {
                    viewModel.addSongToPlaylist(playlist.name)
                }
            }
            if (state.playlists.isEmpty()) {
                Text("还没有自建歌单，可前往“歌单”页面创建。", color = SecondaryInk, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                MoreAction(Icons.Filled.Add, "前往新建歌单") { viewModel.navigate(Destination.Playlists) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: MusicUiState, viewModel: MusicViewModel) {
    LibraryScaffold(state, viewModel, "设置", { viewModel.navigate(Destination.Search) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 14.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { Text("播放", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(vertical = 10.dp)) }
            item {
                SettingsToggleRow(Icons.Filled.Settings, "动态播放器背景", "根据封面生成柔和背景", state.dynamicBackdrop, viewModel::toggleDynamicBackdrop)
            }
            item {
                SettingsToggleRow(Icons.Filled.Refresh, "恢复上次播放队列", "下次启动时继续播放", state.restoreQueue, viewModel::toggleRestoreQueue)
            }
            item {
                SettingsToggleRow(Icons.Filled.Folder, "包含短音频", "扫描小于 30 秒的文件", state.includeShortAudio, viewModel::toggleIncludeShortAudio)
            }
            item { Text("在线补全", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)) }
            item {
                SettingsToggleRow(Icons.Filled.Refresh, "歌词与封面补全", "扫描后自动补齐缺失内容", state.onlineEnabled, viewModel::toggleOnlineEnabled)
            }
            item {
                SettingsToggleRow(Icons.Filled.Settings, "仅使用 Wi‑Fi", "移动网络需手动继续", state.wifiOnly, viewModel::toggleWifiOnly)
            }
            item {
                SettingsNavigationRow(Icons.Filled.Refresh, "重试缺失内容", "重新查询未找到或失败的项目", viewModel::retryEnrichment)
            }
            item {
                SettingsNavigationRow(Icons.Filled.Close, "清除在线内容", "保留本地封面与歌词，删除下载结果", viewModel::clearOnlineContent)
            }
            item { Text("曲库", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)) }
            item { SettingsNavigationRow(Icons.Filled.Refresh, "重新扫描", "更新新增、移动或删除的文件", viewModel::scan) }
            item { SettingsNavigationRow(Icons.Filled.MusicNote, "歌词文件夹", "选择手动 .lrc 文件位置", viewModel::showLyricsFolderMore) }
            item { SettingsNavigationRow(Icons.Filled.Folder, "支持的格式", "MP3、AAC、FLAC、WAV、OGG/Opus", viewModel::showSupportedFormatsMore) }
        }
    }
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = SecondaryInk)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SettingsNavigationRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = SecondaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SecondaryInk)
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BrandSoft),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = BrandDeep, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun PlayerScreen(state: MusicUiState, viewModel: MusicViewModel, lyrics: Boolean) {
    val song = state.currentSong
    val document = viewModel.lyricsFor(song)
    val pagerState = rememberPagerState(
        initialPage = if (lyrics) 1 else 0,
        pageCount = { 2 },
    )
    var selectedPage by remember(lyrics) { mutableStateOf(if (lyrics) 1 else 0) }
    LaunchedEffect(pagerState.currentPage) { selectedPage = pagerState.currentPage }
    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) pagerState.animateScrollToPage(selectedPage)
    }
    val palette = rememberArtworkColors(song)
    val progress by animateFloatAsState(
        targetValue = if (song?.durationMs ?: 0L > 0L) {
            (state.positionMs.toFloat() / song!!.durationMs).coerceIn(0f, 1f)
        } else {
            0f
        },
        label = "progress",
    )
    Scaffold(containerColor = PlayerCanvas) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        if (state.dynamicBackdrop) palette else listOf(BrandDeep.copy(alpha = 0.18f), PlayerCanvas, PlayerCanvas),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Surface) }
                    PlayerPageTabs(selectedPage, onSelect = { selectedPage = it })
                    IconButton(onClick = viewModel::showMore) { Icon(Icons.Filled.MoreVert, "更多", tint = Surface) }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    pageSpacing = 16.dp,
                    beyondViewportPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapAnimationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
                ) { page ->
                    if (page == 0) {
                        Column(Modifier.fillMaxSize()) {
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                val artworkSize = minOf(maxWidth, maxHeight - 24.dp)
                                AlbumArtwork(
                                    song,
                                    artworkSize,
                                    Modifier.shadow(28.dp, RoundedCornerShape(32.dp), clip = false),
                                )
                            }
                            NowPlayingInfo(song, state, viewModel)
                        }
                    } else {
                        LyricsContent(document, state.positionMs, viewModel::seekTo)
                    }
                }
                PlayerSeek(progress, song?.durationMs ?: 0L, viewModel::seekTo)
                Spacer(Modifier.height(16.dp))
                PlayerControls(state, viewModel)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun NowPlayingInfo(song: LocalSong?, state: MusicUiState, viewModel: MusicViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(song?.title ?: "还没有正在播放的音乐", color = Surface, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song?.let { "${it.artist} · ${it.album}" }.orEmpty(), color = PlayerSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { song?.let(viewModel::toggleFavorite) }) {
            Icon(if (song?.trackKey in state.favoriteTrackKeys) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "喜欢", tint = Surface)
        }
    }
}

@Composable
private fun LyricsContent(document: LyricsDocument?, positionMs: Long, onSeek: (Long) -> Unit) {
    if (document == null || document.plainText.isBlank()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = PlayerSecondary, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(20.dp))
            Text("暂无歌词", color = Surface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("扫描后会自动补全本地歌词", color = PlayerSecondary, fontSize = 14.sp)
        }
        return
    }
    if (!document.isSynced) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(document.plainText, color = Surface, fontSize = 18.sp, lineHeight = 32.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(20.dp))
            Text("未同步歌词", color = PlayerSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        return
    }
    val currentIndex = document.currentLineIndex(positionMs)
    val listState = rememberLazyListState()
    var userScrolled by remember(document) { mutableStateOf(false) }
    LaunchedEffect(currentIndex, userScrolled) {
        if (!userScrolled && currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex, scrollOffset = -120)
        }
    }
    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) userScrolled = true
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 72.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(document.lines) { index, line ->
                Text(
                    text = line.text.ifBlank { "♪" },
                    color = if (index == currentIndex) Surface else PlayerSecondary,
                    fontSize = if (index == currentIndex) 22.sp else 17.sp,
                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = if (index == currentIndex) 30.sp else 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSeek(line.timestampMs) }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
        if (userScrolled) {
            TextButton(
                onClick = { userScrolled = false },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = PlayerSurface, contentColor = BrandSoft),
            ) { Text("回到当前歌词") }
        }
    }
}

@Composable
private fun RowScope.PlayerPageTabs(selectedPage: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
        listOf("封面", "歌词").forEachIndexed { index, label ->
            Text(
                label,
                color = if (selectedPage == index) Surface else PlayerSecondary,
                fontSize = 13.sp,
                fontWeight = if (selectedPage == index) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).clickable { onSelect(index) }.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun PlayerSeek(progress: Float, durationMs: Long, onSeek: (Long) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }
    var draggedValue by remember(progress) { mutableStateOf(progress) }
    LaunchedEffect(progress, isDragging) {
        if (!isDragging) draggedValue = progress
    }
    Column {
        Slider(
            value = draggedValue,
            onValueChange = {
                isDragging = true
                draggedValue = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((durationMs * draggedValue).toLong())
            },
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = BrandSoft,
                activeTrackColor = Brand,
                inactiveTrackColor = Surface.copy(alpha = 0.16f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTrackDuration((durationMs * progress).toLong()), color = PlayerSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text(formatTrackDuration(durationMs), color = PlayerSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlayerControls(state: MusicUiState, viewModel: MusicViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = viewModel::cyclePlaybackMode) {
            Icon(
                imageVector = when (state.playbackMode) {
                    PlaybackMode.Sequential -> Icons.Filled.Repeat
                    PlaybackMode.RepeatOne -> Icons.Filled.RepeatOne
                    PlaybackMode.Shuffle -> Icons.Filled.Shuffle
                },
                contentDescription = state.playbackMode.label,
                tint = if (state.playbackMode == PlaybackMode.Sequential) PlayerSecondary else BrandSoft,
            )
        }
        IconButton(onClick = viewModel::skipPrevious) { Icon(Icons.Filled.SkipPrevious, "上一首", tint = Surface, modifier = Modifier.size(30.dp)) }
        FilledIconButton(
            onClick = viewModel::togglePlayback,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Brand, contentColor = Ink),
        ) {
            Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "播放暂停", modifier = Modifier.size(30.dp))
        }
        IconButton(onClick = viewModel::skipNext) { Icon(Icons.Filled.SkipNext, "下一首", tint = Surface, modifier = Modifier.size(30.dp)) }
        IconButton(onClick = viewModel::showQueue) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "队列", tint = PlayerSecondary) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(state: MusicUiState, viewModel: MusicViewModel) {
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissOverlay,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("播放队列 · ${state.queue.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            state.queue.forEach { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (song.trackKey == state.currentSong?.trackKey) BrandSoft else Color.Transparent)
                        .clickable { viewModel.play(song, state.queue); viewModel.dismissOverlay() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = SecondaryInk)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (song.trackKey == state.currentSong?.trackKey) BrandDeep else Ink)
                        Text(song.artist, fontSize = 12.sp, color = SecondaryInk)
                    }
                    Text(formatTrackDuration(song.durationMs), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SecondaryInk)
                    IconButton(onClick = { viewModel.removeFromQueue(song) }) { Icon(Icons.Filled.Close, "移除", tint = SecondaryInk) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(state: MusicUiState, viewModel: MusicViewModel, onPickLyricsFolder: () -> Unit) {
    val target = state.moreTarget ?: return
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissOverlay,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(moreSheetTitle(target), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            when (target) {
                is MoreTarget.Song -> SongMoreActions(target.song, state, viewModel)
                is MoreTarget.Screen -> ScreenMoreActions(target.destination, state, viewModel)
                MoreTarget.LyricsFolder -> {
                    MoreAction(Icons.Outlined.FolderOpen, "选择歌词文件夹") {
                        onPickLyricsFolder()
                        viewModel.dismissOverlay()
                    }
                    MoreAction(Icons.Filled.MusicNote, "支持 .lrc 歌词文件") {
                        viewModel.showMessage("支持 [分:秒.毫秒] 时间轴和纯文本歌词")
                    }
                }
                MoreTarget.SupportedFormats -> {
                    MoreAction(Icons.Filled.MusicNote, "MP3、AAC / M4A、FLAC") {
                        viewModel.showMessage("支持常用有损与无损音频格式")
                    }
                    MoreAction(Icons.Filled.Folder, "WAV、OGG、Opus") {
                        viewModel.showMessage("支持 WAV、OGG 与 Opus 音频")
                    }
                    MoreAction(Icons.Filled.Refresh, "重新扫描曲库") { viewModel.scan() }
                }
            }
        }
    }
}

private fun moreSheetTitle(target: MoreTarget): String = when (target) {
    is MoreTarget.Song -> target.song.title
    is MoreTarget.Screen -> when (target.destination) {
        Destination.Player, Destination.Lyrics -> "当前歌曲"
        Destination.Settings -> "播放与曲库设置"
        Destination.Search -> "搜索结果"
        is Destination.AlbumDetail -> "专辑操作"
        is Destination.ArtistDetail -> "歌手操作"
        is Destination.FolderDetail -> "文件夹操作"
        is Destination.PlaylistDetail, Destination.Playlists -> "歌单操作"
        else -> "曲库操作"
    }
    MoreTarget.LyricsFolder -> "歌词文件夹"
    MoreTarget.SupportedFormats -> "支持的格式"
}

@Composable
private fun SongMoreActions(song: LocalSong, state: MusicUiState, viewModel: MusicViewModel) {
    MoreAction(Icons.Filled.PlayArrow, "立即播放") {
        viewModel.play(song)
        viewModel.dismissOverlay()
    }
    MoreAction(Icons.AutoMirrored.Filled.QueueMusic, "添加到播放队列") {
        viewModel.addToQueue(song)
        viewModel.dismissOverlay()
    }
    MoreAction(Icons.AutoMirrored.Outlined.PlaylistPlay, "加入歌单") {
        viewModel.showPlaylistPicker(song)
    }
    MoreAction(Icons.Outlined.FavoriteBorder, if (song.trackKey in state.favoriteTrackKeys) "移出喜欢" else "添加到喜欢") {
        viewModel.toggleFavorite(song)
        viewModel.dismissOverlay()
    }
    MoreAction(Icons.Outlined.FolderOpen, "查看所在文件夹") {
        viewModel.navigate(Destination.FolderDetail(song.folder))
    }
}

@Composable
private fun ScreenMoreActions(destination: Destination, state: MusicUiState, viewModel: MusicViewModel) {
    val songs = when (destination) {
        is Destination.AlbumDetail -> state.songs.filter { it.album == destination.album }
        is Destination.ArtistDetail -> state.songs.filter { it.artist == destination.artist }
        is Destination.FolderDetail -> state.songs.filter { it.folder == destination.folder }
        is Destination.PlaylistDetail -> if (destination.name == "喜欢的音乐") {
            state.songs.filter { it.trackKey in state.favoriteTrackKeys }
        } else {
            state.playlists.firstOrNull { it.name == destination.name }?.let { playlist ->
                state.songs.filter { it.trackKey in playlist.songKeys }
            }.orEmpty()
        }
        Destination.Search -> state.songs.filter {
            listOf(it.title, it.artist, it.album).any { value -> value.contains(state.query, ignoreCase = true) }
        }
        else -> state.songs
    }

    when (destination) {
        Destination.Player, Destination.Lyrics -> state.currentSong?.let { SongMoreActions(it, state, viewModel) }
        Destination.Settings -> {
            MoreAction(Icons.Filled.Settings, if (state.dynamicBackdrop) "关闭动态播放器背景" else "开启动态播放器背景") {
                viewModel.toggleDynamicBackdrop()
                viewModel.dismissOverlay()
            }
            MoreAction(Icons.Filled.Refresh, if (state.restoreQueue) "关闭恢复播放队列" else "开启恢复播放队列") {
                viewModel.toggleRestoreQueue()
                viewModel.dismissOverlay()
            }
            MoreAction(Icons.Filled.MusicNote, if (state.includeShortAudio) "忽略短音频" else "包含短音频") {
                viewModel.toggleIncludeShortAudio()
                viewModel.dismissOverlay()
            }
            MoreAction(Icons.Filled.Refresh, "重新扫描曲库") { viewModel.scan() }
        }
        Destination.Playlists -> {
            MoreAction(Icons.Filled.Favorite, "打开喜欢的音乐") { viewModel.navigate(Destination.PlaylistDetail("喜欢的音乐")) }
            MoreAction(Icons.Filled.Shuffle, "随机播放全部歌曲") { viewModel.playAll(state.songs, shuffled = true) }
            MoreAction(Icons.Filled.Settings, "歌单播放设置") { viewModel.navigate(Destination.Settings) }
        }
        else -> {
            MoreAction(Icons.Filled.PlayArrow, "播放全部") { viewModel.playAll(songs) }
            MoreAction(Icons.Filled.Shuffle, "随机播放") { viewModel.playAll(songs, shuffled = true) }
            when (destination) {
                is Destination.AlbumDetail -> songs.firstOrNull()?.let { song ->
                    MoreAction(Icons.Filled.Person, "查看歌手") { viewModel.navigate(Destination.ArtistDetail(song.artist)) }
                }
                is Destination.ArtistDetail -> MoreAction(Icons.Filled.Album, "查看全部专辑") { viewModel.navigate(Destination.Albums) }
                is Destination.FolderDetail -> MoreAction(Icons.Filled.Refresh, "重新扫描此文件夹") { viewModel.scan() }
                is Destination.PlaylistDetail -> MoreAction(Icons.Outlined.LibraryMusic, "返回本地曲库") { viewModel.navigate(Destination.Library) }
                Destination.Search -> MoreAction(Icons.Filled.Close, "清空搜索") {
                    viewModel.setQuery("")
                    viewModel.dismissOverlay()
                }
                else -> {
                    MoreAction(Icons.Filled.Refresh, "重新扫描曲库") { viewModel.scan() }
                    MoreAction(Icons.Filled.Settings, "曲库设置") { viewModel.navigate(Destination.Settings) }
                }
            }
        }
    }
}

@Composable
private fun MoreAction(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandSoft),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = BrandDeep) }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BrandMark(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(28.dp))
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.MusicNote, null, tint = Brand, modifier = Modifier.size(size * 0.56f))
        Box(
            modifier = Modifier
                .size(size * 0.76f)
                .border(size * 0.11f, Brand, CircleShape),
        )
    }
}

@Composable
private fun BrandButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(99.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Surface),
    ) { Text(label, fontWeight = FontWeight.Medium) }
}

private fun formatTrackDuration(durationMs: Long): String {
    val seconds = (durationMs / 1_000).coerceAtLeast(0)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    return when {
        minutes == 0L -> "$totalSeconds 秒"
        minutes >= 60L -> "${minutes / 60} 小时 ${minutes % 60} 分钟"
        else -> "$minutes 分钟"
    }
}

private fun formatRelativeTime(timestampMillis: Long?): String {
    if (timestampMillis == null) return "尚未扫描"
    val elapsedMinutes = ((System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L) / 60_000L)
    return when {
        elapsedMinutes == 0L -> "刚刚更新"
        elapsedMinutes < 60L -> "$elapsedMinutes 分钟前更新"
        elapsedMinutes < 1_440L -> "${elapsedMinutes / 60L} 小时前更新"
        else -> "${elapsedMinutes / 1_440L} 天前更新"
    }
}
