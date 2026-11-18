package com.safelivebridge.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SamsungBlueDark,
    onPrimary = LightTextPrimary,
    primaryContainer = DarkCard,
    onPrimaryContainer = DarkTextPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    outline = DarkCardBorder,
    surfaceVariant = DarkCard
)

private val LightColorScheme = lightColorScheme(
    primary = SamsungBlue,
    onPrimary = LightSurface,
    primaryContainer = LightCard,
    onPrimaryContainer = LightTextPrimary,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    outline = LightCardBorder,
    surfaceVariant = LightCard
)

val OneUIShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SafeLiveBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = OneUIShapes,
        content = content
    )
}
