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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        isDarkWater -> Color(0xFF040A10).copy(alpha = 0.85f)
        isLightWater -> Color(0xFFE1F5FE).copy(alpha = 0.88f)
        isDarkSelected -> Color(0xFF141414).copy(alpha = 0.88f)
        else -> Color.White.copy(alpha = 0.90f)
    }

    // Modern multi-layered gradient border to simulate frosted glass lighting reflections
    val borderBrush = when {
        isDarkWater -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00D4FF).copy(alpha = 0.35f),
                Color(0xFF0094C6).copy(alpha = 0.10f)
            )
        )
        isLightWater -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00B0FF).copy(alpha = 0.30f),
                Color(0xFF0288D1).copy(alpha = 0.08f)
            )
        )
        isDarkSelected -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.04f)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.80f),
                Color.Black.copy(alpha = 0.06f)
            )
        )
    }

    val shadowColor = when {
        isDarkWater -> Color(0xFF00D4FF)
        isLightWater -> Color(0xFF0288D1)
        isDarkSelected -> Color.Black
        else -> Color(0xFF202020)
    }

    val navBarShape = RoundedCornerShape(32.dp)
    val haptic = LocalHapticFeedback.current

    val fabBrush = when {
        isDarkWater -> Brush.linearGradient(
            listOf(Color(0xFF00E5FF), Color(0xFF0083B0))
        )
        isLightWater -> Brush.linearGradient(
            listOf(Color(0xFF29B6F6), Color(0xFF0288D1))
        )
        isDarkSelected -> Brush.linearGradient(
            listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
        )
        else -> Brush.linearGradient(
            listOf(Color(0xFF29B6F6), Color(0xFF1976D2))
        )
    }

    val fabBorderColor = when {
        isDarkWater -> Color(0xFF040A10)
        isLightWater -> Color(0xFFE1F5FE)
        isDarkSelected -> Color(0xFF141414)
        else -> Color.White
    }

    // Outer Container centering the floating navbar
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Pill Navigation Bar centered with Home, Analytics Dashboard, and Settings
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = navBarShape,
                    spotColor = shadowColor.copy(alpha = if (isDarkWater) 0.25f else 0.15f),
                    ambientColor = shadowColor.copy(alpha = 0.08f)
                )
                .border(
                    BorderStroke(1.dp, borderBrush),
                    navBarShape
                ),
            shape = navBarShape,
            color = barBgColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. HOME TAB
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

                // 2. ANALYTICS DASHBOARD TAB
                NavBarItem(
                    tabId = "history",
                    label = "Analytics",
                    activeIcon = Icons.Filled.Analytics,
                    inactiveIcon = Icons.Outlined.Analytics,
                    isSelected = currentTab == "history",
                    isDarkSelected = isDarkSelected,
                    isDarkWater = isDarkWater,
                    isLightWater = isLightWater,
                    onSelect = { onTabSelected("history") }
                )

                // 3. SETTINGS TAB
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
    val haptic = LocalHapticFeedback.current

    val activeColor = when {
        isDarkWater -> Color(0xFF00D4FF)
        isLightWater -> Color(0xFF0288D1)
        isDarkSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    val inactiveColor = when {
        isDarkWater -> Color(0xFF5BE7FF).copy(alpha = 0.5f)
        isDarkSelected -> Color.White.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "color"
    )

    val itemBgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = if (isDarkSelected) 0.16f else 0.12f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "itemBgColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(itemBgColor)
            .testTag("nav_item_${tabId}")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Icon(
                imageVector = if (isSelected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(iconColor)
                )
            }
        }
    }
}
