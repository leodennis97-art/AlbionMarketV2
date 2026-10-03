package com.example.albionmarketv2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Albion Market V2 Modern Spacing System
 * Provides uniform, compact spacing tokens (0.5mm - 2mm range / ~2dp - 8dp) across the entire application.
 */
data class AlbionSpacing(
    val tiny: Dp = 2.dp,
    val extraSmall: Dp = 3.dp,
    val small: Dp = 4.dp,
    val compact: Dp = 6.dp,
    val medium: Dp = 8.dp,
    val regular: Dp = 10.dp,
    val large: Dp = 12.dp,
    val extraLarge: Dp = 14.dp,
    val huge: Dp = 16.dp,
    val xxlarge: Dp = 20.dp,

    // Semantic Spacing
    val cardPadding: Dp = 8.dp,
    val cardContentGap: Dp = 4.dp,
    val screenPadding: Dp = 8.dp,
    val itemSpacing: Dp = 6.dp,
    val chipSpacing: Dp = 4.dp,
    val rowGap: Dp = 4.dp,
    val columnGap: Dp = 6.dp,
    val sectionGap: Dp = 8.dp,

    // Corner Radii
    val cardCornerRadius: Dp = 10.dp,
    val buttonCornerRadius: Dp = 8.dp,
    val chipCornerRadius: Dp = 12.dp,
    val inputCornerRadius: Dp = 8.dp
)

val LocalSpacing = staticCompositionLocalOf { AlbionSpacing() }

val MaterialTheme.spacing: AlbionSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

@Composable
fun ProvideSpacing(
    spacing: AlbionSpacing = AlbionSpacing(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalSpacing provides spacing) {
        content()
    }
}
