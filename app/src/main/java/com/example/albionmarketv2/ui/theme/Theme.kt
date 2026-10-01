package com.example.albionmarketv2.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AlbionShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val AlbionLogoColorScheme = darkColorScheme(
    primary = LogoGoldPrimary,
    onPrimary = OnPrimaryBlack,                  // Schwarz auf Gold für perfekten Kontrast (Text auf Knöpfen)
    primaryContainer = PrimaryContainerDark,      // Dunkler Container verhindert knallgelbe Flächen
    onPrimaryContainer = OnPrimaryContainerGold,  // Goldener Text auf dunklem Container
    secondary = LogoBronze,
    onSecondary = OnPrimaryBlack,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerGold,
    tertiary = LogoGoldLight,
    onTertiary = OnPrimaryBlack,
    tertiaryContainer = Color(0xFF332608),
    onTertiaryContainer = Color(0xFFFEF08A),
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
