package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun DynamicGlassOfWater(
    amountMl: Int,
    modifier: Modifier = Modifier,
    glassSize: Dp = 24.dp,
    animated: Boolean = true
) {
    // 100ml = ~10%, 250ml = ~25%, 500ml = ~50%, 750ml = ~75%, 1000ml = 100% filled glass.
    // Anything above 1000ml is 100% filled. Clamped at 0.08f minimum so it doesn't look empty.
    val targetFillRatio = remember(amountMl) {
        (amountMl.toFloat() / 1000f).coerceIn(0.08f, 1.0f)
    }

    val animatedFillRatioState = if (animated) {
        animateFloatAsState(
            targetValue = targetFillRatio,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "glass_water_fill"
        )
    } else {
        remember(targetFillRatio) { mutableStateOf(targetFillRatio) }
    }

    val waveOffsetState = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "glass_wave")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "glass_wave_offset"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Capture theme colors inside the composable block
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Reuse Paths to eliminate memory allocation in draw scope loops
    val glassPath = remember { Path() }
    val waterPath = remember { Path() }
    val waveHighlightPath = remember { Path() }
    val glintLeftPath = remember { Path() }
    val glintRightPath = remember { Path() }

    Canvas(modifier = modifier.size(glassSize)) {
        val animatedFillRatio = animatedFillRatioState.value
        val waveOffset = waveOffsetState.value

        val w = size.width
        val h = size.height

        // Horizontal and vertical paddings
        val padX = w * 0.15f
        val padY = h * 0.12f

        // Bottom thickness of the solid glass physical base
        val glassBaseThickness = h * 0.12f

        // Let's define the points of the glass container profile (tapered tumbler cup geometry)
        val glassTopLeft = Offset(padX, padY)
        val glassTopRight = Offset(w - padX, padY)
        val glassBottomLeft = Offset(padX + w * 0.08f, h - padY)
        val glassBottomRight = Offset(w - padX - w * 0.08f, h - padY)

        // Reuse and compute glassPath
        glassPath.reset()
        glassPath.moveTo(glassTopLeft.x, glassTopLeft.y)
        glassPath.lineTo(glassBottomLeft.x, glassBottomLeft.y)
        glassPath.lineTo(glassBottomRight.x, glassBottomRight.y)
        glassPath.lineTo(glassTopRight.x, glassTopRight.y)

        // 1. Draw Glass interior frosted glass background
        drawPath(
            path = glassPath,
            color = onSurfaceColor.copy(alpha = 0.04f),
            style = Fill
        )

        // 2. Draw the liquid (water) inside the glass
        // Liquid boundaries:
        val liquidBottomY = glassBottomLeft.y - glassBaseThickness
        val liquidTopY = liquidBottomY - (liquidBottomY - glassTopLeft.y) * animatedFillRatio

        // For tapered sides, interpolate x-edges based on vertical position
        fun getLiquidXLeft(y: Float): Float {
            val progress = (y - glassTopLeft.y) / (glassBottomLeft.y - glassTopLeft.y)
            return glassTopLeft.x + (glassBottomLeft.x - glassTopLeft.x) * progress
        }

        fun getLiquidXRight(y: Float): Float {
            val progress = (y - glassTopRight.y) / (glassBottomRight.y - glassTopRight.y)
            return glassTopRight.x + (glassBottomRight.x - glassTopRight.x) * progress
        }

        val liquidBottomLeftX = getLiquidXLeft(liquidBottomY) + 1.2.dp.toPx()
        val liquidBottomRightX = getLiquidXRight(liquidBottomY) - 1.2.dp.toPx()
        val liquidTopLeftX = getLiquidXLeft(liquidTopY) + 1.2.dp.toPx()
        val liquidTopRightX = getLiquidXRight(liquidTopY) - 1.2.dp.toPx()

        val liquidWidth = liquidTopRightX - liquidTopLeftX

        // Reuse and compute waterPath
        waterPath.reset()
        waterPath.moveTo(liquidBottomLeftX, liquidBottomY)
        waterPath.lineTo(liquidBottomRightX, liquidBottomY)
        waterPath.lineTo(liquidTopRightX, liquidTopY)

        // Draw animated waves across the liquid surface
        val waveAmplitude = if (animated) {
            (1.5.dp.toPx() * (1f - (animatedFillRatio - 0.5f).let { kotlin.math.abs(it) })).coerceAtLeast(0.5.dp.toPx())
        } else {
            0f
        }
        val segments = 24
        for (i in segments downTo 0) {
            val f = i.toFloat() / segments
            val x = liquidTopLeftX + liquidWidth * f
            val angle = f * 2f * Math.PI.toFloat() + waveOffset
            val y = liquidTopY + sin(angle) * waveAmplitude
            waterPath.lineTo(x, y)
        }
        waterPath.lineTo(liquidTopLeftX, liquidTopY)
        waterPath.close()

        // Draw water filled body with gradient matching selected theme palette
        val waterGradient = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.85f),
                secondaryColor.copy(alpha = 0.55f)
            ),
            startY = liquidTopY,
            endY = liquidBottomY
        )
        drawPath(
            path = waterPath,
            brush = waterGradient,
            style = Fill
        )

        // Give liquid a glossy top highlight edge - Reuse waveHighlightPath
        waveHighlightPath.reset()
        waveHighlightPath.moveTo(liquidTopLeftX, liquidTopY)
        for (i in 0..segments) {
            val f = i.toFloat() / segments
            val x = liquidTopLeftX + liquidWidth * f
            val angle = f * 2f * Math.PI.toFloat() + waveOffset
            val y = liquidTopY + sin(angle) * waveAmplitude
            waveHighlightPath.lineTo(x, y)
        }
        drawPath(
            path = waveHighlightPath,
            color = Color.White.copy(alpha = 0.45f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Draw glass structure outline
        val glassStrokeWidth = 2.dp.toPx()
        drawPath(
            path = glassPath,
            color = onSurfaceColor.copy(alpha = 0.22f),
            style = Stroke(width = glassStrokeWidth, join = StrokeJoin.Round, cap = StrokeCap.Round)
        )

        // 4. Draw realistic reflections on the glass body (Glint/Gloss) - Reuse glintLeftPath
        glintLeftPath.reset()
        val startY = glassTopLeft.y + 2.dp.toPx()
        val endY = glassBottomLeft.y - glassBaseThickness - 2.dp.toPx()
        glintLeftPath.moveTo(getLiquidXLeft(startY) + 1.dp.toPx(), startY)
        glintLeftPath.lineTo(getLiquidXLeft(endY) + 1.dp.toPx(), endY)
        
        drawPath(
            path = glintLeftPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.05f)
                ),
                startY = glassTopLeft.y,
                endY = glassBottomLeft.y
            ),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Right subtle secondary accent reflection - Reuse glintRightPath
        glintRightPath.reset()
        val startYRight = glassTopRight.y + 4.dp.toPx()
        val endYRight = glassTopRight.y + h * 0.35f
        glintRightPath.moveTo(getLiquidXRight(startYRight) - 1.5.dp.toPx(), startYRight)
        glintRightPath.lineTo(getLiquidXRight(endYRight) - 1.5.dp.toPx(), endYRight)
        
        drawPath(
            path = glintRightPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
