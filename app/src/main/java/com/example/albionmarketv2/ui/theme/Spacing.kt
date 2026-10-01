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
 * Provides uniform, clear, and spacious spacing tokens across the entire application.
 */
data class AlbionSpacing(
    val tiny: Dp = 4.dp,
    val extraSmall: Dp = 6.dp,
    val small: Dp = 8.dp,
    val compact: Dp = 10.dp,
    val medium: Dp = 12.dp,
    val regular: Dp = 16.dp,
    val large: Dp = 20.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
    val xxlarge: Dp = 48.dp,

    // Semantic Spacing
    val cardPadding: Dp = 16.dp,
    val cardContentGap: Dp = 12.dp,
    val screenPadding: Dp = 16.dp,
    val itemSpacing: Dp = 14.dp,
    val chipSpacing: Dp = 8.dp,
    val rowGap: Dp = 8.dp,
    val columnGap: Dp = 12.dp,
    val sectionGap: Dp = 20.dp,

    // Corner Radii
    val cardCornerRadius: Dp = 16.dp,
    val buttonCornerRadius: Dp = 12.dp,
    val chipCornerRadius: Dp = 20.dp,
    val inputCornerRadius: Dp = 12.dp
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
