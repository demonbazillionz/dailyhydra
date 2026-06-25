package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    COMPACT,      // Small phones (< 360dp)
    MEDIUM,       // Standard phones (360dp to 599dp)
    EXPANDED,     // Large phones / small tablets (600dp to 839dp)
    LARGE,        // Tablets and foldables (840dp to 1199dp)
    EXTRA_LARGE   // Desktop/Desktop environments (>= 1200dp)
}

val LocalWindowSizeClass = compositionLocalOf { WindowSizeClass.MEDIUM }

@Composable
fun rememberWindowSizeClass(width: Dp): WindowSizeClass {
    return when {
        width < 360.dp -> WindowSizeClass.COMPACT
        width < 600.dp -> WindowSizeClass.MEDIUM
        width < 840.dp -> WindowSizeClass.EXPANDED
        width < 1200.dp -> WindowSizeClass.LARGE
        else -> WindowSizeClass.EXTRA_LARGE
    }
}

/**
 * A responsive dual-pane helper composable that places two contents side-by-side on wide/landscape screens
 * and stacked vertically on narrow/portrait screens to avoid awkward visual stretching and respect folding lines.
 */
@Composable
fun ResponsiveSplitLayout(
    modifier: Modifier = Modifier,
    spacing: Dp = 18.dp,
    useSplit: Boolean,
    column1Weight: Float = 1f,
    column2Weight: Float = 1f,
    pane1: @Composable () -> Unit,
    pane2: @Composable () -> Unit
) {
    if (useSplit) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Box(
                modifier = Modifier
                    .weight(column1Weight)
                    .fillMaxHeight()
            ) {
                pane1()
            }
            Box(
                modifier = Modifier
                    .weight(column2Weight)
                    .fillMaxHeight()
            ) {
                pane2()
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            pane1()
            pane2()
        }
    }
}
