package com.btsheng.erp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Front-end Spec §10.2 工业蓝色板 */
object ErpColors {
    val IndustrialBlue = Color(0xFF0969DA)
    val Blue50 = Color(0xFFDDF4FF)
    val Blue700 = Color(0xFF0550AE)
    val ManufacturingOrange = Color(0xFFFB8500)
    val SuccessGreen = Color(0xFF1A7F37)
    val WarningYellow = Color(0xFFBF8700)
    val ErrorRed = Color(0xFFCF222E)
    val TextPrimary = Color(0xFF1F2328)
    val TextSecondary = Color(0xFF59636E)
    val BorderGray = Color(0xFFD1D9E0)
    val BackgroundGray = Color(0xFFF6F8FA)
    val White = Color(0xFFFFFFFF)
}

private val LightScheme = lightColorScheme(
    primary = ErpColors.IndustrialBlue,
    onPrimary = ErpColors.White,
    primaryContainer = ErpColors.Blue50,
    onPrimaryContainer = ErpColors.Blue700,
    secondary = ErpColors.ManufacturingOrange,
    onSecondary = ErpColors.White,
    background = ErpColors.BackgroundGray,
    onBackground = ErpColors.TextPrimary,
    surface = ErpColors.White,
    onSurface = ErpColors.TextPrimary,
    error = ErpColors.ErrorRed,
    outline = ErpColors.BorderGray,
    tertiary = ErpColors.WarningYellow,
)

private val DarkScheme = darkColorScheme(
    primary = ErpColors.Blue50,
    onPrimary = ErpColors.Blue700,
    primaryContainer = ErpColors.Blue700,
    onPrimaryContainer = ErpColors.Blue50,
    secondary = ErpColors.ManufacturingOrange,
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
)

@Composable
fun ErpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
