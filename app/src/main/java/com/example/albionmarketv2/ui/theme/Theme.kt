package com.example.albionmarketv2.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AlbionShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

private val AlbionLogoColorScheme = darkColorScheme(
    primary = LogoGoldPrimary,
    onPrimary = OnPrimaryBlack,                  // Schwarz auf Gold für perfekten Kontrast (Text auf Knöpfen)
    primaryContainer = PrimaryContainerDark,      // Dunkler Container
    onPrimaryContainer = OnPrimaryContainerGold,  // Goldener Text
    secondary = LogoCyanPrimary,                  // Logo Cyan Accent (#06B6D4)
    onSecondary = OnPrimaryBlack,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerCyan,
    tertiary = EmeraldGreen,                     // Logo Emerald Green (#10B981)
    onTertiary = OnPrimaryBlack,
    tertiaryContainer = Color(0xFF063726),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = CosmicDarkBg,
    onBackground = TextMain,
    surface = GlassSurface,
    onSurface = TextMain,
    surfaceVariant = GlassSurfaceHigh,
    onSurfaceVariant = TextMuted,
    surfaceContainer = GlassSurface,
    surfaceContainerLow = Color(0xFF0E141E),
    surfaceContainerHigh = GlassSurfaceHigh,
    surfaceContainerHighest = GlassSurfaceElevated,
    outline = GlassBorder,
    outlineVariant = GlassBorderLight,
    error = CrimsonRed,
    onError = Color.White
)

@Composable
fun AlbionMarketV2Theme(
    content: @Composable () -> Unit
) {
    ProvideSpacing {
        MaterialTheme(
            colorScheme = AlbionLogoColorScheme,
            typography = Typography,
            shapes = AlbionShapes,
            content = content
        )
    }
}
