package com.yinqi.player.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.yinqi.player.R
import com.yinqi.player.data.LocalSong
import com.yinqi.player.ui.theme.PlayerCanvas
import com.yinqi.player.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@Composable
internal fun AlbumArtwork(song: LocalSong?, size: Dp, modifier: Modifier = Modifier) {
    val palette = rememberArtworkColors(song)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        val uri = song?.effectiveArtworkUri
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = "${song.title}封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_music_notification),
            )
        } else {
            Icon(Icons.Filled.MusicNote, null, tint = Surface.copy(alpha = 0.76f), modifier = Modifier.size(size * 0.36f))
        }
    }
}

@Composable
internal fun rememberArtworkColors(song: LocalSong?): List<Color> {
    val context = LocalContext.current
    var extracted by remember(song?.effectiveArtworkUri) { mutableStateOf<List<Color>?>(null) }
    LaunchedEffect(song?.effectiveArtworkUri) {
        extracted = withContext(Dispatchers.IO) {
            song?.effectiveArtworkUri?.let { artworkPalette(context, it) }
        }
    }
    return extracted ?: artworkColors(song)
}

private val artworkPaletteCache = LruCache<String, List<Color>>(64)

private fun artworkPalette(context: android.content.Context, uri: Uri): List<Color>? {
    artworkPaletteCache.get(uri.toString())?.let { return it }
    val options = BitmapFactory.Options().apply {
        inSampleSize = 4
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    val bitmap = runCatching {
        if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path, options)
        } else {
            context.contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input, null, options) }
        }
    }.getOrNull() ?: return null
    val palette = runCatching { Palette.from(bitmap).generate() }.getOrNull()
    bitmap.recycle()
    val dominant = palette?.getDominantColor(0xFF214D34.toInt()) ?: return null
    val secondary = palette.getVibrantColor(palette.getMutedColor(dominant))
    val dark = clampPlayerColor(shadeColor(dominant, 0.36f))
    val mid = clampPlayerColor(shadeColor(secondary, 0.82f))
    return listOf(Color(dark).copy(alpha = 0.9f), Color(mid).copy(alpha = 0.72f), PlayerCanvas).also {
        artworkPaletteCache.put(uri.toString(), it)
    }
}

private fun clampPlayerColor(color: Int): Int {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color, hsv)
    hsv[2] = hsv[2].coerceAtMost(0.24f)
    return android.graphics.Color.HSVToColor(hsv)
}

private fun shadeColor(color: Int, factor: Float): Int = android.graphics.Color.rgb(
    (android.graphics.Color.red(color) * factor).toInt().coerceIn(0, 255),
    (android.graphics.Color.green(color) * factor).toInt().coerceIn(0, 255),
    (android.graphics.Color.blue(color) * factor).toInt().coerceIn(0, 255),
)

private fun artworkColors(song: LocalSong?): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF214D34), Color(0xFF81C98F), Color(0xFF0F2018)),
        listOf(Color(0xFF0D2020), Color(0xFF2C6A5F), Color(0xFF101613)),
        listOf(Color(0xFFE5FBF0), Color(0xFF8ED8C0), Color(0xFF245C4A)),
        listOf(Color(0xFFB8C9C0), Color(0xFF305247), Color(0xFF10251F)),
        listOf(Color(0xFF042B28), Color(0xFF4E9187), Color(0xFF07100E)),
        listOf(Color(0xFF102218), Color(0xFF1FC777), Color(0xFF0B120F)),
    )
    val index = ((song?.trackKey?.hashCode()?.toLong() ?: 0L).absoluteValue % palettes.size).toInt()
    return palettes[index]
}
