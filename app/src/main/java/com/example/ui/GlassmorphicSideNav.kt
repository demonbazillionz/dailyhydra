package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassmorphicSideNav(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    currentTheme: String,
    sizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
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

    // Determine side navigation configuration
    val navWidth = when (sizeClass) {
        WindowSizeClass.EXPANDED -> 80.dp
        WindowSizeClass.LARGE -> 100.dp
        WindowSizeClass.EXTRA_LARGE -> 260.dp
        else -> 80.dp
    }

    val isCollapsed = sizeClass == WindowSizeClass.EXPANDED || sizeClass == WindowSizeClass.LARGE
    val showLabels = sizeClass == WindowSizeClass.LARGE || sizeClass == WindowSizeClass.EXTRA_LARGE

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .width(navWidth)
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, top = 24.dp, bottom = 24.dp, end = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val cornerRadiusPx = 28.dp.toPx()
                    val width = size.width
                    val height = size.height

                    // Soft atmospheric outer shadow (iOS mimic)
                    drawRoundRect(
                        color = shadowColor.copy(alpha = if (isDarkSelected) 0.08f else 0.02f),
                        topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                        size = Size(width + 4.dp.toPx(), height + 4.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                    
                    // Controlled reflection glow (Exclusive for Dark Water theme)
                    if (isDarkWater) {
                        drawRoundRect(
                            color = Color(0xFF00D4FF).copy(alpha = 0.05f),
                            topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()),
                            size = Size(width + 12.dp.toPx(), height + 12.dp.toPx()),
                            cornerRadius = CornerRadius(cornerRadiusPx + 6.dp.toPx())
                        )
                    }
                }
                .background(barBgColor, RoundedCornerShape(28.dp))
                .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(28.dp))
                .padding(vertical = 24.dp, horizontal = if (isCollapsed) 8.dp else 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    if (isCollapsed) {
                        // Collapsed Logo Icon
                        Image(
                            painter = painterResource(id = R.drawable.dailyhydra_logo),
                            contentDescription = "dailyhydra logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        // Expanded brand header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.dailyhydra_logo),
                                contentDescription = "dailyhydra logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Text(
                                text = "dailyhydra",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isDarkWater) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Main Menu list
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
                    ) {
                        val items = listOf(
                            Triple("home", "Home", Pair(Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop)),
                            Triple("history", "Analytics", Pair(Icons.Filled.Analytics, Icons.Outlined.Analytics)),
                            Triple("ranks", "Ranks", Pair(Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents)),
                            Triple("settings", "Settings", Pair(Icons.Filled.Settings, Icons.Outlined.Settings))
                        )
                        
                        items.forEach { (tabId, label, icons) ->
                            SideMenuItem(
                                tabId = tabId,
                                label = label,
                                activeIcon = icons.first,
                                inactiveIcon = icons.second,
                                isSelected = currentTab == tabId,
                                isDarkSelected = isDarkSelected,
                                isDarkWater = isDarkWater,
                                isLightWater = isLightWater,
                                showLabel = showLabels,
                                isCollapsed = isCollapsed,
                                onSelect = { onTabSelected(tabId) }
                            )
                        }
                    }
                }
                
                // Bottom section: Info pill or diagnostic info
                if (!isCollapsed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Every Drop Counts 💧",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SideMenuItem(
    tabId: String,
    label: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    isSelected: Boolean,
    isDarkSelected: Boolean,
    isDarkWater: Boolean,
    isLightWater: Boolean,
    showLabel: Boolean,
    isCollapsed: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val activeColor = when {
        isDarkWater -> Color(0xFF00D4FF)
        isLightWater -> Color(0xFF0288D1)
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

    if (isCollapsed) {
        // Navigation Rail item (Vertical column layout)
        Column(
            modifier = Modifier
                .width(64.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                )
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
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
                                            activeColor.copy(alpha = 0.05f),
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
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
            
            if (showLabel) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = iconColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // Permanent side panel menu item (Row layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Transparent
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                )
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isSelected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
            
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = iconColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
