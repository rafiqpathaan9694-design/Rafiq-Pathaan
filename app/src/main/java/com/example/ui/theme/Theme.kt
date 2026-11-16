package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = PureWhite,
    primaryContainer = IceBlueContainer,
    onPrimaryContainer = DeepNavy,
    secondary = BrightBlue,
    onSecondary = PureWhite,
    secondaryContainer = SoftBlueSurface,
    onSecondaryContainer = RoyalBlue,
    tertiary = PrimaryBlue,
    onTertiary = PureWhite,
    background = OffWhiteBackground,
    onBackground = SlateInk,
    surface = PureWhite,
    onSurface = SlateInk,
    surfaceVariant = SoftBlueSurface,
    onSurfaceVariant = SlateMuted,
    outline = OutlineGray,
    outlineVariant = CardBorderBlue,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorRedBg,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryBlue,
    onPrimary = DeepNavy,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = IceBlueContainer,
    secondary = Color(0xFF38BDF8),
    onSecondary = DeepNavy,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color(0xFFBAE6FD),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E3A8A),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MobileSolutionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
