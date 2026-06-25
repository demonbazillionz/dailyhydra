package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RanksScreen(
    viewModel: HydrationViewModel,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allXpTransactions by viewModel.allXpTransactions.collectAsStateWithLifecycle(emptyList())
    val allRankHistory by viewModel.allRankHistory.collectAsStateWithLifecycle(emptyList())
    val allUnlockedAchievements by viewModel.allUnlockedAchievements.collectAsStateWithLifecycle(emptyList())
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val isWater = currentTheme == "Water" || currentTheme == "Light Water" || currentTheme == "Dark Water"

    // Stats calculations
    val totalWaterLogged = uiState.allEntries.sumOf { it.amountMl }
    val totalGoalsCompleted = uiState.streak.totalHydratedDays
    val currentStreakDays = uiState.streak.currentStreak
    val bestStreakDays = uiState.streak.bestStreak

    val activeDays = remember(uiState.allEntries) {
        val cal = Calendar.getInstance()
        uiState.allEntries.map { 
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
        }.distinct().size
    }

    // Basic calculation
    val totalXp = allXpTransactions.sumOf { it.amount }
    val currentRank = getRankForStats(totalXp, activeDays, bestStreakDays)
    val currentRankIndex = RANKS_LIST.indexOf(currentRank)

    val nextRank = if (currentRankIndex != -1 && currentRankIndex < RANKS_LIST.size - 1) {
        RANKS_LIST[currentRankIndex + 1]
    } else {
        null
    }

    val progressToNextRank = if (nextRank != null) {
        val tierMaxXp = nextRank.minXp - currentRank.minXp
        val tierCurrentXp = totalXp - currentRank.minXp
        if (tierMaxXp > 0) (tierCurrentXp.toFloat() / tierMaxXp.toFloat()).coerceIn(0f, 1f) else 1.0f
    } else {
        1.0f
    }

    val xpNeededForNextRank = if (nextRank != null) {
        (nextRank.minXp - totalXp).coerceAtLeast(0)
    } else {
        0
    }

    val daysNeededForNextRank = if (nextRank != null) {
        (nextRank.minActiveDays - activeDays).coerceAtLeast(0)
    } else {
        0
    }

    val streakNeededForNextRank = if (nextRank != null) {
        (nextRank.minStreak - bestStreakDays).coerceAtLeast(0)
    } else {
        0
    }

    // Hydra levels
    val currentLevel = (totalXp / 500) + 1
    val xpInCurrentLevel = totalXp % 500
    val progressToNextLevel = (xpInCurrentLevel.toFloat() / 500f).coerceIn(0f, 1f)
    val xpNeededForNextLevel = 500 - xpInCurrentLevel

    // Haptic feedback preference Hook
    val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val triggerHaptic = {
        if (hapticEnabled) {
            try {
                val view = android.view.View(context)
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            } catch (e: Exception) {
                // Safe ignore
            }
        }
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val showBottomBar = windowSizeClass == WindowSizeClass.COMPACT || windowSizeClass == WindowSizeClass.MEDIUM

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                GlassmorphicNavBar(
                    currentTab = "ranks",
                    onTabSelected = onTabSelected,
                    currentTheme = currentTheme,
                    onFabClick = { viewModel.triggerAddWaterSheet() }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = if (showBottomBar) 0.dp else innerPadding.calculateBottomPadding()
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            // Screen Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dailyhydra_logo),
                    contentDescription = "dailyhydra logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column {
                    Text(
                        text = "Ranks & Badges",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track your offline hydration progress tier",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // 1. Current Rank Card (PREMIUM ACCENT GRADIENT)
            val colors = getRankGradientColors(currentRank.name)
            val brush = if (isWater) {
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color(colors[0]), Color(colors[1]))
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rank_progress_card"),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush)
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                 Text(
                                     text = "Current Rank",
                                     style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                     color = if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
                                 )
                                 Text(
                                     text = currentRank.name,
                                     style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                     color = if (isWater) MaterialTheme.colorScheme.onBackground else Color.White
                                 )
                            }
                            // Rank Emoji Badge representation with crystal-water reflections
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .let {
                                        if (isWater) {
                                            it.background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.7f),
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                    )
                                                )
                                            )
                                        } else {
                                            it.background(Color.White.copy(alpha = 0.2f))
                                        }
                                    }
                                    .border(
                                        1.5.dp,
                                        if (isWater) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                                    .let {
                                        if (isWater) {
                                            it.drawBehind {
                                                // Reflective glare arc on the glass droplet
                                                drawArc(
                                                    color = Color.White.copy(alpha = 0.82f),
                                                    startAngle = 180f,
                                                    sweepAngle = 90f,
                                                    useCenter = false,
                                                    topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                                                    size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
                                                    style = Stroke(width = 2.5.dp.toPx())
                                                )
                                                // Subtle water drop glare spot
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = 2.5.dp.toPx(),
                                                    center = Offset(size.width * 0.3f, size.height * 0.3f)
                                                )
                                            }
                                        } else {
                                            it
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                RankBadge(
                                    rankName = currentRank.iconEmoji,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = currentRank.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)
                        )

                        // Progress to next rank
                        if (nextRank != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Requirements for ${nextRank.name}:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isWater) MaterialTheme.colorScheme.onBackground else Color.White
                                )

                                // 1. XP Requirement Indicator
                                val xpReqProgress = if (nextRank.minXp > 0) (totalXp.toFloat() / nextRank.minXp.toFloat()).coerceIn(0f, 1f) else 1f
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Experience (XP)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "$totalXp / ${nextRank.minXp} XP",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isWater) MaterialTheme.colorScheme.onBackground else Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(xpReqProgress)
                                                .fillMaxHeight()
                                                .background(if (isWater) MaterialTheme.colorScheme.primary else Color.White)
                                        )
                                    }
                                }

                                // 2. Active Days Requirement Indicator
                                val daysReqProgress = if (nextRank.minActiveDays > 0) (activeDays.toFloat() / nextRank.minActiveDays.toFloat()).coerceIn(0f, 1f) else 1f
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Active Tracking Days",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "$activeDays / ${nextRank.minActiveDays} Days",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isWater) MaterialTheme.colorScheme.onBackground else Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(daysReqProgress)
                                                .fillMaxHeight()
                                                .background(if (isWater) MaterialTheme.colorScheme.primary else Color.White)
                                        )
                                    }
                                }

                                // 3. Streak Requirement Indicator
                                val streakReqProgress = if (nextRank.minStreak > 0) (bestStreakDays.toFloat() / nextRank.minStreak.toFloat()).coerceIn(0f, 1f) else 1f
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Best Streak Goal",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "$bestStreakDays / ${nextRank.minStreak}-Day streak",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isWater) MaterialTheme.colorScheme.onBackground else Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(streakReqProgress)
                                                .fillMaxHeight()
                                                .background(if (isWater) MaterialTheme.colorScheme.primary else Color.White)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "🌌 Ultimate Grandmaster tier unlocked!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isWater) MaterialTheme.colorScheme.onBackground else Color.White
                            )
                        }

                        if (nextRank != null) {
                            val reqsList = mutableListOf<String>()
                            if (xpNeededForNextRank > 0) reqsList.add("$xpNeededForNextRank XP")
                            if (daysNeededForNextRank > 0) reqsList.add("$daysNeededForNextRank Active Days")
                            if (streakNeededForNextRank > 0) reqsList.add("$streakNeededForNextRank Streak Days")
                            
                            val reqsStr = if (reqsList.isEmpty()) {
                                "All conditions met for next tier!"
                            } else {
                                "Needed: " + reqsList.joinToString(", ")
                            }

                            Text(
                                text = "$reqsStr until ${nextRank.name}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.95f)
                            )
                        } else {
                            Text(
                                text = "Ultimate Grandmaster reached!",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // 1.5 Grandmaster Pursuit Card
            val gmXpProgress = (totalXp.toFloat() / 40000f).coerceIn(0f, 1f)
            val gmDaysProgress = (activeDays.toFloat() / 180f).coerceIn(0f, 1f)
            val gmStreakProgress = (bestStreakDays.toFloat() / 30f).coerceIn(0f, 1f)
            val overallGmProgress = (gmXpProgress + gmDaysProgress + gmStreakProgress) / 3f

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grandmaster_pursuit_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🌌", style = TextStyle(fontSize = 20.sp))
                            Text(
                                text = "Grandmaster Quest",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${(overallGmProgress * 100).toInt()}% Done",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Grandmaster status requires a minimum of 180 consistent active days, a 30-day streak, and 40,000 XP. Only genuine, daily dedication can unlock this ultimate tier.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    LinearProgressIndicator(
                        progress = { overallGmProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "XP Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${totalXp} / 40K",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Active Days",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${activeDays} / 180",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Best Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${bestStreakDays} / 30",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Hydra Level Section (Linear Card)
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Hydra Level Core",
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hydra Level $currentLevel",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$xpInCurrentLevel / 500 XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressToNextLevel },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )

                        Text(
                            text = "$xpNeededForNextLevel XP to Level ${currentLevel + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 3. Compact Metrics/Stats Panel
            Text(
                text = "My Progress Indicators",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBox(
                            title = "Water Consumed",
                            value = if (totalWaterLogged >= 1000) String.format("%.2f L", totalWaterLogged / 1000f) else "${totalWaterLogged}ml",
                            icon = Icons.Default.WaterDrop,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatBox(
                            title = "Streak Record",
                            value = "$currentStreakDays Days",
                            subValue = "Record: $bestStreakDays d",
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFFF7043)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBox(
                            title = "Goals Completed",
                            value = "$totalGoalsCompleted times",
                            icon = Icons.Default.TaskAlt,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF66BB6A)
                        )
                        StatBox(
                            title = "Total XP Earned",
                            value = "$totalXp XP",
                            icon = Icons.Default.Star,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFFFCA28)
                        )
                    }
                }
            }

            // 5. Progression Roadmap Timeline
            Text(
                text = "Rank Progression Track",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Render current, past and near-term milestones to prevent rendering 27 rows (which lags/bloats)
                    // We render completed ranks, current rank, and up to 3 upcoming ranks for premium minimal UI
                    val ranksToRender = mutableListOf<Rank>()
                    
                    // Always show Bronze III, Bronze II, Bronze I
                    // And then shows current Rank, and up to 3 next ranks
                    val completedRanks = RANKS_LIST.filterIndexed { index, _ -> index < currentRankIndex }
                    val upcomingRanks = RANKS_LIST.filterIndexed { index, _ -> index > currentRankIndex }
                    
                    // Pick the last 2 completed, the current rank, and next 3 upcoming
                    val renderList = mutableListOf<Pair<Rank, String>>() // Rank, Status
                    
                    RANKS_LIST.forEachIndexed { index, rank ->
                        val status = when {
                            index < currentRankIndex -> "completed"
                            index == currentRankIndex -> "current"
                            else -> "locked"
                        }
                        // To make the roadmap clean, short and modern, we can prioritize:
                        // - Bronze III (always)
                        // - silver / gold thresholds or just filter to show completed, current, and locked
                        // But wait! Users can appreciate a full, beautifully scrollable timeline, OR we can show a representative compact selection
                        // The prompt says: "Vertical list showing completed, current, and locked ranks."
                        // Let's render a beautifully organized selection where Bronze III, Silver III, Gold V, Platinum V, Diamond V, Master V, Grandmaster are shown as milestones, plus current rank! This is super intuitive and avoids 27 items cluttering the UI.
                        // Wait! A scrollable roadmap is very easy to render. Let's render the entire list so it perfectly matches the literal requirement. Android has nested recycling/scrolling, and here it is inside verticalScroll, so it works flawlessly! Let's display the list.
                    }

                    RANKS_LIST.forEachIndexed { index, rank ->
                        val isCompleted = index < currentRankIndex
                        val isCurrent = index == currentRankIndex
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Timeline dot representation
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.12f)
                                                isCurrent -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isCompleted -> Icons.Default.Check
                                            isCurrent -> Icons.Default.PlayArrow
                                            else -> Icons.Default.Lock
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isCompleted -> Color(0xFF4CAF50)
                                            isCurrent -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Content details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RankBadge(
                                        rankName = rank.iconEmoji,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = rank.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold
                                        ),
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.primary
                                        } else if (isCompleted) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        }
                                    )
                                }
                                Text(
                                    text = "Requires: ${rank.minXp} XP · ${rank.minActiveDays} Days · ${rank.minStreak}d Streak",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    }
                                )
                                Text(
                                    text = rank.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Requirement status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.08f)
                                            isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isCompleted -> "Achieved"
                                        isCurrent -> "Current"
                                        else -> "Locked"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        isCompleted -> Color(0xFF4CAF50)
                                        isCurrent -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Upcoming Locked Liters Milestones (Unlocked Milestones are filtered out of Ranks tab)
            val lockedMilestones = LITERS_MILESTONES.filter { (totalWaterLogged / 1000f) < it.targetLiters }
            if (lockedMilestones.isNotEmpty()) {
                Text(
                    text = "Upcoming Liters Milestones (Locked)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        lockedMilestones.forEach { milestone ->
                            val currentLiters = totalWaterLogged / 1000f
                            val progressFrac = (currentLiters / milestone.targetLiters).coerceIn(0f, 1f)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked milestone",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = milestone.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "+${milestone.rewardXp} XP",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    
                                    Text(
                                        text = milestone.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    LinearProgressIndicator(
                                        progress = { progressFrac },
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f L / %d L", currentLiters, milestone.targetLiters),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Rank Achievements History Log
            if (allRankHistory.isNotEmpty()) {
                Text(
                    text = "Unlocked Milestone Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        allRankHistory.forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Milestone reached",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Reached ${log.rankName}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Acquired rank status",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                                Text(
                                    text = df.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Unlocked Milestone Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No rank promotion achievements unlocked yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Earn your first rank milestone by tracking hydration and collecting XP transactions!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val iconString: String,
    val description: String
)

@Composable
fun AchievementRow(ach: AchievementDefinition, isUnlocked: Boolean, isWaterTheme: Boolean = false) {
    val brush = if (isUnlocked && isWaterTheme) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondary, // Crystal Water Blue
                MaterialTheme.colorScheme.primary   // Soft Baby Aqua
            )
        )
    } else {
        null
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (isUnlocked && isWaterTheme) {
                    it.shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                } else {
                    it
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                if (isWaterTheme) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
            }
        ),
        border = if (isWaterTheme) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (isUnlocked) 0.4f else 0.15f))
        } else if (isUnlocked) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (brush != null) {
                        it.background(brush)
                    } else {
                        it
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AchievementBadge(
                    achievementId = ach.id,
                    isUnlocked = isUnlocked,
                    modifier = Modifier.size(44.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ach.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isUnlocked) {
                                if (isWaterTheme) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            }
                        )
                        if (isUnlocked) {
                            Text(
                                text = "UNLOCKED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isWaterTheme) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = ach.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnlocked) {
                            if (isWaterTheme) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    subValue: String? = null,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subValue != null) {
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class LitersMilestone(
    val id: String,
    val targetLiters: Int,
    val title: String,
    val rewardXp: Int,
    val description: String
)

val LITERS_MILESTONES = listOf(
    LitersMilestone("lite_10", 10, "Bronze Well", 500, "Consume a total of 10 Liters (10,000 ml) of water."),
    LitersMilestone("lite_50", 50, "Silver Spring", 1500, "Consume a total of 50 Liters (50,000 ml) of water."),
    LitersMilestone("lite_100", 100, "Gold River", 3000, "Consume a total of 100 Liters (100,000 ml) of water."),
    LitersMilestone("lite_500", 500, "Grand Ocean", 10000, "Consume a total of 500 Liters (500,000 ml) of water.")
)

