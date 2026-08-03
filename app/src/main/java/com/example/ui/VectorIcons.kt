package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RankBadge(rankName: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val activeRank = rankName.lowercase()

        when {
            activeRank.contains("bronze") -> {
                // Bronze Badge: A classic 3D-shield with central star
                val shieldPath = Path().apply {
                    moveTo(width * 0.15f, height * 0.15f)
                    lineTo(width * 0.85f, height * 0.15f)
                    lineTo(width * 0.85f, height * 0.55f)
                    quadraticTo(width * 0.85f, height * 0.85f, width * 0.5f, height * 0.95f)
                    quadraticTo(width * 0.15f, height * 0.85f, width * 0.15f, height * 0.55f)
                    close()
                }
                val bronzeGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFFCD7F32), Color(0xFF8B4513))
                )
                drawPath(path = shieldPath, brush = bronzeGradient, style = Fill)
                drawPath(path = shieldPath, color = Color.White.copy(alpha = 0.3f), style = Stroke(width = 3f))

                // Inner shield line
                val innerPath = Path().apply {
                    moveTo(width * 0.25f, height * 0.23f)
                    lineTo(width * 0.75f, height * 0.23f)
                    lineTo(width * 0.75f, height * 0.52f)
                    quadraticTo(width * 0.75f, height * 0.78f, width * 0.5f, height * 0.86f)
                    quadraticTo(width * 0.25f, height * 0.78f, width * 0.25f, height * 0.52f)
                    close()
                }
                drawPath(path = innerPath, color = Color.White.copy(alpha = 0.2f), style = Stroke(width = 2f))

                // Bronze Star
                drawStar(center = Offset(width * 0.5f, height * 0.5f), points = 5, innerRadius = width * 0.08f, outerRadius = width * 0.18f, color = Color(0xFFFFD700))
            }
            activeRank.contains("silver") -> {
                // Silver Badge: Sleek hexagonal shield
                val hexPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.08f)
                    lineTo(width * 0.9f, height * 0.3f)
                    lineTo(width * 0.9f, height * 0.7f)
                    lineTo(width * 0.5f, height * 0.92f)
                    lineTo(width * 0.1f, height * 0.7f)
                    lineTo(width * 0.1f, height * 0.3f)
                    close()
                }
                val silverGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE0E0E0), Color(0xFF757575))
                )
                drawPath(path = hexPath, brush = silverGradient, style = Fill)
                drawPath(path = hexPath, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = 3.5f))

                // Inner circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = width * 0.25f,
                    center = Offset(width * 0.5f, height * 0.51f),
                    style = Stroke(width = 2f)
                )

                // Silver Wings/V Shape
                val vPath = Path().apply {
                    moveTo(width * 0.3f, height * 0.42f)
                    lineTo(width * 0.5f, height * 0.62f)
                    lineTo(width * 0.7f, height * 0.42f)
                    lineTo(width * 0.5f, height * 0.72f)
                    close()
                }
                drawPath(path = vPath, brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFCECECE))), style = Fill)
            }
            activeRank.contains("gold") -> {
                // Gold Badge: Rich hexagonal shield
                val goldPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.05f)
                    lineTo(width * 0.92f, height * 0.25f)
                    lineTo(width * 0.92f, height * 0.75f)
                    lineTo(width * 0.5f, height * 0.95f)
                    lineTo(width * 0.08f, height * 0.75f)
                    lineTo(width * 0.08f, height * 0.25f)
                    close()
                }
                val goldGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF176), Color(0xFFF57F17))
                )
                drawPath(path = goldPath, brush = goldGradient, style = Fill)
                drawPath(path = goldPath, color = Color(0xFFFFD54F), style = Stroke(width = 4f))

                // Inner hexagonal border
                val goldInnerPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.12f)
                    lineTo(width * 0.85f, height * 0.3f)
                    lineTo(width * 0.85f, height * 0.7f)
                    lineTo(width * 0.5f, height * 0.88f)
                    lineTo(width * 0.15f, height * 0.7f)
                    lineTo(width * 0.15f, height * 0.3f)
                    close()
                }
                drawPath(path = goldInnerPath, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = 1.5f))

                // Gold Trophy/Crown in middle
                val trophyPath = Path().apply {
                    moveTo(width * 0.35f, height * 0.35f)
                    lineTo(width * 0.65f, height * 0.35f)
                    lineTo(width * 0.6f, height * 0.55f)
                    quadraticTo(width * 0.5f, height * 0.65f, width * 0.4f, height * 0.55f)
                    close()
                }
                drawPath(path = trophyPath, color = Color.White, style = Fill)

                // Trophy stem and base
                drawRect(
                    color = Color.White,
                    topLeft = Offset(width * 0.46f, height * 0.58f),
                    size = Size(width * 0.08f, height * 0.12f)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(width * 0.38f, height * 0.7f),
                    size = Size(width * 0.24f, height * 0.04f)
                )
            }
            activeRank.contains("platinum") -> {
                // Platinum Badge: Sleek Diamond Shape
                val platPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.06f)
                    lineTo(width * 0.94f, height * 0.5f)
                    lineTo(width * 0.5f, height * 0.94f)
                    lineTo(width * 0.06f, height * 0.5f)
                    close()
                }
                val platGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE0F7FA), Color(0xFF00ACC1))
                )
                drawPath(path = platPath, brush = platGradient, style = Fill)
                drawPath(path = platPath, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 3f))

                // Sleek inner line
                val platInnerPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.16f)
                    lineTo(width * 0.84f, height * 0.5f)
                    lineTo(width * 0.5f, height * 0.84f)
                    lineTo(width * 0.16f, height * 0.5f)
                    close()
                }
                drawPath(path = platInnerPath, color = Color(0xFFE0F7FA).copy(alpha = 0.5f), style = Stroke(width = 1.5f))

                // Diamond core spark
                drawStar(
                    center = Offset(width * 0.5f, height * 0.5f),
                    points = 4,
                    innerRadius = width * 0.06f,
                    outerRadius = width * 0.22f,
                    color = Color.White
                )
            }
            activeRank.contains("diamond") -> {
                // Diamond Badge: Crisp physical gemstone design
                val gemOuterPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.05f)
                    lineTo(width * 0.9f, height * 0.35f)
                    lineTo(width * 0.5f, height * 0.95f)
                    lineTo(width * 0.1f, height * 0.35f)
                    close()
                }
                val gemGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE3F2FD), Color(0xFF1E88E5))
                )
                drawPath(path = gemOuterPath, brush = gemGradient, style = Fill)
                drawPath(path = gemOuterPath, color = Color.White.copy(alpha = 0.7f), style = Stroke(width = 3.5f))

                // Crystal Facets
                val horizontalLine = Path().apply {
                    moveTo(width * 0.1f, height * 0.35f)
                    lineTo(width * 0.9f, height * 0.35f)
                }
                drawPath(path = horizontalLine, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 2f))

                val centerFacet = Path().apply {
                    moveTo(width * 0.5f, height * 0.05f)
                    lineTo(width * 0.5f, height * 0.95f)
                }
                drawPath(path = centerFacet, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = 2f))

                val leftSlope = Path().apply {
                    moveTo(width * 0.5f, height * 0.35f)
                    lineTo(width * 0.1f, height * 0.35f)
                    lineTo(width * 0.5f, height * 0.05f)
                    close()
                }
                drawPath(path = leftSlope, color = Color.White.copy(alpha = 0.15f), style = Fill)

                val rightSlope = Path().apply {
                    moveTo(width * 0.5f, height * 0.35f)
                    lineTo(width * 0.9f, height * 0.35f)
                    lineTo(width * 0.5f, height * 0.05f)
                    close()
                }
                drawPath(path = rightSlope, color = Color.White.copy(alpha = 0.25f), style = Fill)
            }
            activeRank.contains("grandmaster") -> {
                // Grandmaster Badge: Celestial Orbits & Cosmic Flare
                val outerCircle = Path().apply {
                    addOval(Rect(0f, 0f, width, height))
                }
                val cosmicGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFF3F51B5), Color(0xFFD500F9), Color(0xFFFF1744))
                )
                drawPath(path = outerCircle, brush = cosmicGradient, style = Fill)
                drawPath(path = outerCircle, color = Color(0xFFFFD700).copy(alpha = 0.8f), style = Stroke(width = 3.5f))

                // Inner celestial glowing ring
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = 0.4f),
                    radius = width * 0.42f,
                    center = Offset(width * 0.5f, height * 0.5f),
                    style = Stroke(width = 2f)
                )

                // Orbits
                drawOval(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(width * 0.1f, height * 0.32f),
                    size = Size(width * 0.8f, height * 0.36f),
                    style = Stroke(width = 4f)
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = width * 0.35f,
                    center = Offset(width * 0.5f, height * 0.5f),
                    style = Stroke(width = 2f)
                )

                // Radiant core Star
                drawStar(
                    center = Offset(width * 0.5f, height * 0.5f),
                    points = 8,
                    innerRadius = width * 0.08f,
                    outerRadius = width * 0.28f,
                    color = Color(0xFFFFD700)
                )

                // Diamond flares at the star corners
                drawCircle(color = Color.White, radius = width * 0.06f, center = Offset(width * 0.5f, height * 0.5f))
            }
            activeRank.contains("master") -> {
                // Master Badge: Majestic Crown
                val backPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.08f)
                    lineTo(width * 0.92f, height * 0.3f)
                    lineTo(width * 0.92f, height * 0.8f)
                    quadraticTo(width * 0.5f, height * 0.98f, width * 0.08f, height * 0.8f)
                    lineTo(width * 0.08f, height * 0.3f)
                    close()
                }
                val masterGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE1BEE7), Color(0xFF6A1B9A))
                )
                drawPath(path = backPath, brush = masterGradient, style = Fill)
                drawPath(path = backPath, color = Color(0xFFD500F9).copy(alpha = 0.7f), style = Stroke(width = 3f))

                // Majestic crown
                val crownPath = Path().apply {
                    moveTo(width * 0.22f, height * 0.65f)
                    lineTo(width * 0.2f, height * 0.38f)
                    lineTo(width * 0.38f, height * 0.52f)
                    lineTo(width * 0.5f, height * 0.28f)
                    lineTo(width * 0.62f, height * 0.52f)
                    lineTo(width * 0.8f, height * 0.38f)
                    lineTo(width * 0.78f, height * 0.65f)
                    close()
                }
                drawPath(path = crownPath, color = Color.White, style = Fill)
                drawPath(path = crownPath, color = Color(0xFFFFD700).copy(alpha = 0.7f), style = Stroke(width = 1.5f))

                // Crown gems
                drawCircle(color = Color(0xFFFFD700), radius = width * 0.035f, center = Offset(width * 0.2f, height * 0.38f))
                drawCircle(color = Color(0xFFFFD700), radius = width * 0.045f, center = Offset(width * 0.5f, height * 0.28f))
                drawCircle(color = Color(0xFFFFD700), radius = width * 0.035f, center = Offset(width * 0.8f, height * 0.38f))
            }
            else -> {
                // Fallback Grey Shield
                val fallbackPath = Path().apply {
                    moveTo(width * 0.15f, height * 0.15f)
                    lineTo(width * 0.85f, height * 0.15f)
                    lineTo(width * 0.85f, height * 0.55f)
                    quadraticTo(width * 0.85f, height * 0.85f, width * 0.5f, height * 0.95f)
                    quadraticTo(width * 0.15f, height * 0.85f, width * 0.15f, height * 0.55f)
                    close()
                }
                drawPath(path = fallbackPath, color = Color.Gray, style = Fill)
            }
        }
    }
}

@Composable
fun AchievementBadge(
    achievementId: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val bColor = if (isUnlocked) {
            when (achievementId) {
                "first_sip" -> Color(0xFF2196F3) // Water Blue
                "streak_7" -> Color(0xFFFF5722)  // Streak Orange
                "streak_30" -> Color(0xFFE040FB) // Purple Trophy
                "litres_100" -> Color(0xFF00BCD4) // Teal Ocean
                "goal_crusher" -> Color(0xFFFFEB3B) // Yellow Bolt
                "early_bird" -> Color(0xFFFF9800) // Horizon Amber
                "night_owl" -> Color(0xFF3F51B5)  // Indigo Sleep
                else -> Color(0xFF9E9E9E)
            }
        } else {
            Color(0xFFE0E0E0)
        }

        val strokeColor = if (isUnlocked) Color.White.copy(alpha = 0.6f) else Color(0xFFBDBDBD)

        if (isUnlocked) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(bColor.copy(alpha = 0.4f), bColor.copy(alpha = 0.05f))
                ),
                radius = width * 0.48f,
                center = Offset(width * 0.5f, height * 0.5f)
            )
        }

        drawCircle(
            color = if (isUnlocked) bColor else Color(0x1F000000),
            radius = width * 0.4f,
            center = Offset(width * 0.5f, height * 0.5f),
            style = Fill
        )

        drawCircle(
            color = strokeColor,
            radius = width * 0.4f,
            center = Offset(width * 0.5f, height * 0.5f),
            style = Stroke(width = 3f)
        )

        val glyphColor = if (isUnlocked) {
            if (achievementId == "goal_crusher") Color(0xFF5D4037) else Color.White
        } else {
            Color(0xFF9E9E9E)
        }

        when (achievementId) {
            "first_sip" -> {
                // Elegant Water Drop
                val dropPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.28f)
                    cubicTo(
                        width * 0.5f, height * 0.28f,
                        width * 0.72f, height * 0.48f,
                        width * 0.72f, height * 0.64f
                    )
                    quadraticTo(
                        width * 0.72f, height * 0.78f,
                        width * 0.5f, height * 0.78f
                    )
                    quadraticTo(
                        width * 0.28f, height * 0.78f,
                        width * 0.28f, height * 0.64f
                    )
                    cubicTo(
                        width * 0.28f, height * 0.48f,
                        width * 0.5f, height * 0.28f,
                        width * 0.5f, height * 0.28f
                    )
                    close()
                }
                drawPath(path = dropPath, color = glyphColor, style = Fill)
            }
            "streak_7" -> {
                // Streak Flame
                val flamePath = Path().apply {
                    moveTo(width * 0.5f, height * 0.24f)
                    cubicTo(width * 0.65f, height * 0.38f, width * 0.7f, height * 0.5f, width * 0.7f, height * 0.64f)
                    quadraticTo(width * 0.70f, height * 0.78f, width * 0.5f, height * 0.78f)
                    quadraticTo(width * 0.30f, height * 0.78f, width * 0.30f, height * 0.64f)
                    cubicTo(width * 0.30f, height * 0.56f, width * 0.36f, height * 0.46f, width * 0.42f, height * 0.38f)
                    quadraticTo(width * 0.5f, height * 0.52f, width * 0.54f, height * 0.48f)
                    close()
                }
                drawPath(path = flamePath, color = glyphColor, style = Fill)
            }
            "streak_30" -> {
                // Crown / Trophy
                val crownPath = Path().apply {
                    moveTo(width * 0.25f, height * 0.68f)
                    lineTo(width * 0.22f, height * 0.38f)
                    lineTo(width * 0.4f, height * 0.52f)
                    lineTo(width * 0.5f, height * 0.32f)
                    lineTo(width * 0.6f, height * 0.52f)
                    lineTo(width * 0.78f, height * 0.38f)
                    lineTo(width * 0.75f, height * 0.68f)
                    close()
                }
                drawPath(path = crownPath, color = glyphColor, style = Fill)
                drawCircle(color = glyphColor, radius = width * 0.03f, center = Offset(width * 0.22f, height * 0.38f))
                drawCircle(color = glyphColor, radius = width * 0.04f, center = Offset(width * 0.5f, height * 0.32f))
                drawCircle(color = glyphColor, radius = width * 0.03f, center = Offset(width * 0.78f, height * 0.38f))
            }
            "litres_100" -> {
                // Tidal wave
                val wavePath = Path().apply {
                    moveTo(width * 0.24f, height * 0.52f)
                    cubicTo(width * 0.34f, height * 0.38f, width * 0.46f, height * 0.38f, width * 0.56f, height * 0.52f)
                    cubicTo(width * 0.66f, height * 0.66f, width * 0.74f, height * 0.58f, width * 0.78f, height * 0.48f)
                    lineTo(width * 0.78f, height * 0.7f)
                    lineTo(width * 0.24f, height * 0.7f)
                    close()
                }
                drawPath(path = wavePath, color = glyphColor, style = Fill)

                val wavePath2 = Path().apply {
                    moveTo(width * 0.22f, height * 0.62f)
                    cubicTo(width * 0.34f, height * 0.52f, width * 0.44f, height * 0.52f, width * 0.54f, height * 0.62f)
                    cubicTo(width * 0.64f, height * 0.72f, width * 0.72f, height * 0.68f, width * 0.78f, height * 0.58f)
                    lineTo(width * 0.78f, height * 0.74f)
                    lineTo(width * 0.22f, height * 0.74f)
                    close()
                }
                drawPath(path = wavePath2, color = glyphColor, style = Fill)
            }
            "goal_crusher" -> {
                // Lightning Bolt / Energized flow
                val boltPath = Path().apply {
                    moveTo(width * 0.54f, height * 0.24f)
                    lineTo(width * 0.32f, height * 0.54f)
                    lineTo(width * 0.48f, height * 0.54f)
                    lineTo(width * 0.44f, height * 0.78f)
                    lineTo(width * 0.68f, height * 0.46f)
                    lineTo(width * 0.50f, height * 0.46f)
                    close()
                }
                drawPath(path = boltPath, color = glyphColor, style = Fill)
            }
            "early_bird" -> {
                // Sunrise horizon
                val sunCenter = Offset(width * 0.5f, height * 0.58f)
                drawArc(
                    color = glyphColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(sunCenter.x - width * 0.18f, sunCenter.y - height * 0.18f),
                    size = Size(width * 0.36f, height * 0.36f)
                )
                drawLine(
                    color = glyphColor,
                    start = Offset(width * 0.22f, height * 0.58f),
                    end = Offset(width * 0.78f, height * 0.58f),
                    strokeWidth = 3f
                )
                drawLine(color = glyphColor, start = Offset(width * 0.5f, height * 0.34f), end = Offset(width * 0.5f, height * 0.26f), strokeWidth = 2.5f)
                drawLine(color = glyphColor, start = Offset(width * 0.36f, height * 0.44f), end = Offset(width * 0.28f, height * 0.38f), strokeWidth = 2.5f)
                drawLine(color = glyphColor, start = Offset(width * 0.64f, height * 0.44f), end = Offset(width * 0.72f, height * 0.38f), strokeWidth = 2.5f)
            }
            "night_owl" -> {
                // Crescent Moon + Star
                val moonPath = Path().apply {
                    moveTo(width * 0.34f, height * 0.42f)
                    cubicTo(width * 0.34f, height * 0.62f, width * 0.54f, height * 0.72f, width * 0.68f, height * 0.62f)
                    cubicTo(width * 0.54f, height * 0.62f, width * 0.44f, height * 0.50f, width * 0.46f, height * 0.36f)
                    cubicTo(width * 0.40f, height * 0.34f, width * 0.36f, height * 0.38f, width * 0.34f, height * 0.42f)
                    close()
                }
                drawPath(path = moonPath, color = glyphColor, style = Fill)

                drawStar(
                    center = Offset(width * 0.64f, height * 0.38f),
                    points = 4,
                    innerRadius = width * 0.02f,
                    outerRadius = width * 0.05f,
                    color = glyphColor
                )
            }
            else -> {
                // Small cup silhouette fallback
                val cupPath = Path().apply {
                    moveTo(width * 0.36f, height * 0.34f)
                    lineTo(width * 0.64f, height * 0.34f)
                    lineTo(width * 0.58f, height * 0.68f)
                    quadraticTo(width * 0.5f, height * 0.72f, width * 0.42f, height * 0.68f)
                    close()
                }
                drawPath(path = cupPath, color = glyphColor, style = Fill)
            }
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    points: Int,
    innerRadius: Float,
    outerRadius: Float,
    color: Color
) {
    val path = Path()
    var angle = -Math.PI / 2
    val angleIncrement = Math.PI / points

    path.moveTo(
        (center.x + outerRadius * cos(angle)).toFloat(),
        (center.y + outerRadius * sin(angle)).toFloat()
    )

    for (i in 0 until points * 2) {
        angle += angleIncrement
        val radius = if (i % 2 == 0) innerRadius else outerRadius
        path.lineTo(
            (center.x + radius * cos(angle)).toFloat(),
            (center.y + radius * sin(angle)).toFloat()
        )
    }
    path.close()
    drawPath(path = path, color = color, style = Fill)
}
