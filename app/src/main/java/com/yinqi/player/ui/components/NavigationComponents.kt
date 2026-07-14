package com.yinqi.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yinqi.player.player.Destination
import com.yinqi.player.player.MusicUiState
import com.yinqi.player.player.MusicViewModel
import com.yinqi.player.ui.theme.Brand
import com.yinqi.player.ui.theme.BrandDeep
import com.yinqi.player.ui.theme.BrandSoft
import com.yinqi.player.ui.theme.Ink
import com.yinqi.player.ui.theme.SecondaryInk
import com.yinqi.player.ui.theme.Surface
import com.yinqi.player.ui.theme.SurfaceSubtle

@Composable
internal fun MiniPlayer(state: MusicUiState, viewModel: MusicViewModel) {
    val song = state.currentSong ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .clickable { viewModel.navigate(Destination.Player) },
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(SurfaceSubtle)) {
            val progress = if (song.durationMs > 0L) {
                (state.positionMs.toFloat() / song.durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            Box(Modifier.fillMaxWidth(progress).height(2.dp).background(Brand))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(song, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                androidx.compose.material3.Text(song.title, color = Ink, fontSize = 14.sp, maxLines = 1)
                androidx.compose.material3.Text(song.artist, color = SecondaryInk, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = viewModel::togglePlayback) {
                Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "播放暂停", tint = Ink)
            }
            IconButton(onClick = viewModel::showQueue) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "队列", tint = Ink) }
        }
    }
}

@Composable
internal fun BottomTabs(destination: Destination, viewModel: MusicViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .navigationBarsPadding()
            .height(64.dp)
            .border(1.dp, SurfaceSubtle),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomTab("本地", Icons.Outlined.LibraryMusic, destination in setOf(Destination.Library, Destination.Albums, Destination.Artists, Destination.Folders), onClick = { viewModel.navigate(Destination.Library) })
        BottomTab("歌单", Icons.AutoMirrored.Outlined.PlaylistPlay, destination == Destination.Playlists, onClick = { viewModel.navigate(Destination.Playlists) })
        BottomTab("设置", Icons.Filled.Settings, destination == Destination.Settings, onClick = { viewModel.navigate(Destination.Settings) })
    }
}

@Composable
private fun BottomTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .background(if (selected) BrandSoft else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (selected) BrandDeep else SecondaryInk, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.Text(label, fontSize = 12.sp, color = if (selected) BrandDeep else SecondaryInk)
    }
}
