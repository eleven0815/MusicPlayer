package com.yinqi.player.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Brand = Color(0xFF1FC777)
val BrandDeep = Color(0xFF10804A)
val BrandSoft = Color(0xFFD4F7E5)
val Canvas = Color(0xFFF6F8F6)
val Surface = Color(0xFFFFFFFF)
val SurfaceSubtle = Color(0xFFEFF3F0)
val Ink = Color(0xFF121714)
val SecondaryInk = Color(0xFF66706A)
val PlayerCanvas = Color(0xFF101613)
val PlayerSurface = Color(0xFF1B2C24)
val PlayerSecondary = Color(0xFFA8B5AD)
val Error = Color(0xFFE14F4F)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Surface,
    secondary = BrandDeep,
    background = Canvas,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = SecondaryInk,
    error = Error,
)

private val DarkColors = darkColorScheme(
    primary = Brand,
    onPrimary = Ink,
    secondary = BrandSoft,
    background = PlayerCanvas,
    onBackground = Surface,
    surface = PlayerSurface,
    onSurface = Surface,
    surfaceVariant = Color(0xFF284238),
    onSurfaceVariant = PlayerSecondary,
    error = Error,
)

private val YinqiTypography = Typography(
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontSize = 12.sp),
)

@Composable
fun YinqiTheme(
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val colors = if (dark) DarkColors else LightColors

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }

    MaterialTheme(
        colorScheme = colors,
        typography = YinqiTypography,
        content = content,
    )
}
