package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A custom curved cutout shape for the bottom navigation bar to embed a center-docked FAB (BHIM style).
 * It uses Bezier curves to smoothly transition from the horizontal top edge down into the circular cradle.
 */
class CurvedCutoutNavbarShape(
    private val cradleRadius: Dp,
    private val cornerRadius: Dp,
    private val shoulderWidth: Dp = 12.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        with(density) {
            val width = size.width
            val height = size.height
            val r = cornerRadius.toPx()
            val cr = cradleRadius.toPx()
            val s = shoulderWidth.toPx()
            val center = width / 2f

            // Start at top-left corner
            path.moveTo(0f, r)
            path.arcTo(
                rect = Rect(0f, 0f, 2 * r, 2 * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Line to left shoulder start
            val cx1 = center - cr - s
            path.lineTo(cx1, 0f)

            // Smooth left shoulder dip
            path.cubicTo(
                x1 = center - cr - s * 0.5f, y1 = 0f,
                x2 = center - cr, y2 = 0f,
                x3 = center - cr, y3 = cr * 0.2f
            )

            // Left side of bottom cradle
            path.cubicTo(
                x1 = center - cr, y1 = cr * 0.7f,
                x2 = center - cr * 0.5f, y2 = cr,
                x3 = center, y3 = cr
            )

            // Right side of bottom cradle returning up
            path.cubicTo(
                x1 = center + cr * 0.5f, y1 = cr,
                x2 = center + cr, y2 = cr * 0.7f,
                x3 = center + cr, y3 = cr * 0.2f
            )

            // Smooth right shoulder rise back to top-edge
            path.cubicTo(
                x1 = center + cr, y1 = 0f,
                x2 = center + cr + s * 0.5f, y2 = 0f,
                x3 = center + cr + s, y3 = 0f
            )

            // Line to top-right corner
            path.lineTo(width - r, 0f)

            // Top-right corner arc
            path.arcTo(
                rect = Rect(width - 2 * r, 0f, width, 2 * r),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Down to bottom-right corner
            path.lineTo(width, height - r)

            // Bottom-right corner arc
            path.arcTo(
                rect = Rect(width - 2 * r, height - 2 * r, width, height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Line to bottom-left corner
            path.lineTo(r, height)

            // Bottom-left corner arc
            path.arcTo(
                rect = Rect(0f, height - 2 * r, 2 * r, height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            path.close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun GlassmorphicNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    currentTheme: String,
    modifier: Modifier = Modifier,
    onFabClick: () -> Unit = {}
) {
    val isSystemDark = isSystemInDarkTheme()
    val isLightWater = currentTheme == "Water" || currentTheme == "Light Water"
    val isDarkWater = currentTheme == "Dark Water"
    val isDarkSelected = when (currentTheme) {
        "Dark" -> true
        "Dark Water" -> true
        "Light" -> false
        "Light Water" -> false
        else -> isSystemDark
    }

    // Determine colors and background styles based on themes
    val barBgColor = when {
        isDarkWater -> Color(0xFF040A10).copy(alpha = 0.08f)
        isLightWater -> Color(0xFFE1F5FE).copy(alpha = 0.04f)
        isDarkSelected -> Color(0xFF0C0C0C).copy(alpha = 0.05f)
        else -> Color.White.copy(alpha = 0.04f)
    }

    // Modern multi-layered gradient border to simulate frosted glass lighting reflections
    val borderBrush = when {
        isDarkWater -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00D4FF).copy(alpha = 0.25f),
                Color(0xFF0094C6).copy(alpha = 0.04f)
            )
        )
        isLightWater -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00B0FF).copy(alpha = 0.18f),
                Color(0xFF0288D1).copy(alpha = 0.03f)
            )
        )
        isDarkSelected -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.01f)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.40f),
                Color.Black.copy(alpha = 0.02f)
            )
        )
    }

    val shadowColor = when {
        isDarkWater -> Color(0xFF00D4FF)
        isLightWater -> Color(0xFF0288D1)
        isDarkSelected -> Color.Black
        else -> Color(0xFF202020)
    }

    // Centered FAB cradle dimension specifications
    val fabDiameter = 56.dp
    val cradleRadius = 38.dp // Provides precise uniform comfort clearance all around FAB
    val curveRadius = 32.dp

    // Outer Container holding navbar and overlapping FAB
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding() // extending/clipping correctly over gesture navigation bar
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Navigation Bar sitting directly under the floating center FAB overlapping area
        Box(
            modifier = Modifier
                .padding(top = 28.dp) // creates perfect 50% vertical overlap space for 56dp FAB
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // Sleek atmospheric outer custom shadow layout matching our cutout shape
                    // We render custom backdrop glowing reflections specifically for Dark Water theme
                    if (isDarkWater) {
                        drawRoundRect(
                            color = Color(0xFF00D4FF).copy(alpha = 0.06f),
                            topLeft = Offset(-10.dp.toPx(), -6.dp.toPx() + 28.dp.toPx()),
                            size = Size(width + 20.dp.toPx(), height + 20.dp.toPx()),
                            cornerRadius = CornerRadius(curveRadius.toPx() + 10.dp.toPx())
                        )
                    }
                }
                .shadow(
                    elevation = 8.dp,
                    shape = CurvedCutoutNavbarShape(cradleRadius = cradleRadius, cornerRadius = curveRadius),
                    clip = false,
                    spotColor = shadowColor.copy(alpha = 0.25f),
                    ambientColor = shadowColor.copy(alpha = 0.1f)
                )
                .background(
                    barBgColor,
                    CurvedCutoutNavbarShape(cradleRadius = cradleRadius, cornerRadius = curveRadius)
                )
                .border(
                    BorderStroke(1.dp, borderBrush),
                    CurvedCutoutNavbarShape(cradleRadius = cradleRadius, cornerRadius = curveRadius)
                )
                .padding(vertical = 4.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE TABS: Home, Stats
                NavBarItem(
                    tabId = "home",
                    label = "Home",
                    activeIcon = Icons.Filled.WaterDrop,
                    inactiveIcon = Icons.Outlined.WaterDrop,
                    isSelected = currentTab == "home",
                    isDarkSelected = isDarkSelected,
                    isDarkWater = isDarkWater,
                    isLightWater = isLightWater,
                    onSelect = { onTabSelected("home") }
                )

                NavBarItem(
                    tabId = "ranks",
                    label = "Stats",
                    activeIcon = Icons.Filled.EmojiEvents,
                    inactiveIcon = Icons.Outlined.EmojiEvents,
                    isSelected = currentTab == "ranks",
                    isDarkSelected = isDarkSelected,
                    isDarkWater = isDarkWater,
                    isLightWater = isLightWater,
                    onSelect = { onTabSelected("ranks") }
                )

                // CENTRAL CUTOUT GAP FOR DOCKED FAB
                Spacer(modifier = Modifier.width(72.dp))

                // RIGHT SIDE TABS: History, Settings
                NavBarItem(
                    tabId = "history",
                    label = "History",
                    activeIcon = Icons.Filled.Analytics,
                    inactiveIcon = Icons.Outlined.Analytics,
                    isSelected = currentTab == "history",
                    isDarkSelected = isDarkSelected,
                    isDarkWater = isDarkWater,
                    isLightWater = isLightWater,
                    onSelect = { onTabSelected("history") }
                )

                NavBarItem(
                    tabId = "settings",
                    label = "Settings",
                    activeIcon = Icons.Filled.Settings,
                    inactiveIcon = Icons.Outlined.Settings,
                    isSelected = currentTab == "settings",
                    isDarkSelected = isDarkSelected,
                    isDarkWater = isDarkWater,
                    isLightWater = isLightWater,
                    onSelect = { onTabSelected("settings") }
                )
            }
        }

        // 2. CENTRAL FLOATING Center-Docked FAB with premium animations
        var isPressed by remember { mutableStateOf(false) }
        val fabScale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "fabScale"
        )

        val fabColor = when {
            isDarkWater -> Color(0xFF00D4FF)
            isLightWater -> Color(0xFF0288D1)
            else -> MaterialTheme.colorScheme.primary
        }

        FloatingActionButton(
            onClick = {
                onFabClick()
            },
            containerColor = fabColor,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 2.dp,
                hoveredElevation = 10.dp,
                focusedElevation = 10.dp
            ),
            modifier = Modifier
                .size(fabDiameter)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = fabScale
                    scaleY = fabScale
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            onFabClick()
                        }
                    )
                }
                .testTag("fab_add_water_center")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Manual Water Intake",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun RowScope.NavBarItem(
    tabId: String,
    label: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    isSelected: Boolean,
    isDarkSelected: Boolean,
    isDarkWater: Boolean,
    isLightWater: Boolean,
    onSelect: () -> Unit
) {
    // Elegant organic spring transition spec
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val activeColor = when {
        isDarkWater -> Color(0xFF00D4FF)
        isLightWater -> Color(0xFF0288D1)
        isDarkSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    val inactiveColor = when {
        isDarkWater -> Color(0xFF5BE7FF).copy(alpha = 0.4f)
        isDarkSelected -> Color.White.copy(alpha = 0.4f)
        else -> Color.Black.copy(alpha = 0.4f)
    }

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "color"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .testTag("nav_item_${tabId}")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Subtle water-inspired active glow using RadialGradient
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        activeColor.copy(alpha = 0.28f),
                                        activeColor.copy(alpha = 0.06f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.width / 1.5f
                                )
                            )
                        }
                )
            }

            Icon(
                imageVector = if (isSelected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Tiny elegant active label dot dynamic scale
        val dotScale by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "dotScale"
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                    alpha = dotScale
                }
                .background(activeColor, CircleShape)
        )
    }
}
