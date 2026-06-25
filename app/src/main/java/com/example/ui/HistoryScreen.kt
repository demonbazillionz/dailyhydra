package com.example.ui

import android.content.Context
import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.R
import com.example.data.IntakeEntry
import com.example.ui.theme.ColorExcellent
import com.example.ui.theme.ColorNeedsWater
import com.example.ui.theme.ColorOnTrack
import com.example.ui.theme.ColorBehind
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AnalyticsFilter(val label: String, val days: Int) {
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    LAST_90_DAYS("90 Days", 90),
    LAST_YEAR("Year", 365),
    ALL_TIME("All Time", -1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HydrationViewModel,
    onTabSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentGoalUnit by viewModel.goalUnit.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    fun triggerHaptic() {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val lastAddedEntry by viewModel.lastAddedEntry.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLightWater = currentTheme == "Water" || currentTheme == "Light Water"
    val isDarkWater = currentTheme == "Dark Water"
    val isWater = isLightWater || isDarkWater
    val isSystemDark = isSystemInDarkTheme()
    val isDarkSelected = when (currentTheme) {
        "Dark" -> true
        "Dark Water" -> true
        "Light" -> false
        "Light Water" -> false
        else -> isSystemDark
    }

    LaunchedEffect(lastAddedEntry) {
        lastAddedEntry?.let { entry ->
            val result = snackbarHostState.showSnackbar(
                message = "+${entry.amountMl}ml added",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                triggerHaptic()
                viewModel.undoLastAdd()
            }
        }
    }

    // Active analytics filter
    var selectedFilter by remember { mutableStateOf(AnalyticsFilter.LAST_30_DAYS) }

    // Active viewed month for heatmap calendar navigation
    var currentHeatmapMonth by remember { mutableStateOf(Calendar.getInstance()) }

    // Aggregate statistics
    val allLogs = uiState.allEntries
    val dailyGoal = uiState.dailyGoalMls

    // Dynamic Filtered Logs
    val filteredLogs = remember(allLogs, selectedFilter) {
        val cutoffTime = when (selectedFilter) {
            AnalyticsFilter.LAST_7_DAYS -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            AnalyticsFilter.LAST_30_DAYS -> System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
            AnalyticsFilter.LAST_90_DAYS -> System.currentTimeMillis() - 90 * 24 * 60 * 60 * 1000L
            AnalyticsFilter.LAST_YEAR -> System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L
            AnalyticsFilter.ALL_TIME -> 0L
        }
        allLogs.filter { it.timestamp >= cutoffTime }
    }

    // Goal units helper formatter
    val formatVolume = { ml: Int ->
        if (currentGoalUnit == "ml") {
            "$ml ml"
        } else {
            String.format(Locale.getDefault(), "%.2f L", ml / 1000f)
        }
    }

    // Today's Intake Calculations
    val todayIntakeMl = uiState.totalIntakeToday
    val todayPercent = if (dailyGoal > 0) (todayIntakeMl.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 2f) else 0f
    
    // Status text based on today's intake percentage
    val hydrationStatus = uiState.goalStatus
    
    val statusColor = when {
        todayPercent >= 1.0f -> ColorExcellent
        todayPercent >= 0.8f -> ColorOnTrack
        todayPercent >= 0.5f -> ColorBehind
        else -> ColorNeedsWater
    }

    // Offline Hydration Score calculation (0 - 100)
    val hydrationScore = uiState.hydrationScore

    val performanceRatingText = when {
        hydrationScore >= 90 -> "Excellent Hydration"
        hydrationScore >= 70 -> "Good Hydration"
        hydrationScore >= 50 -> "Fair Hydration"
        else -> "Needs Hydration Focus"
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val showBottomBar = windowSizeClass == WindowSizeClass.COMPACT || windowSizeClass == WindowSizeClass.MEDIUM

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("undo_snackbar_host")
            ) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = if (isDarkSelected) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.inverseSurface
                    },
                    contentColor = if (isDarkSelected) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    },
                    actionColor = if (isWater) {
                        Color(0xFF2196F3)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    actionContentColor = if (isWater) {
                        Color(0xFF2196F3)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .testTag("undo_snackbar")
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                GlassmorphicNavBar(
                    currentTab = "history",
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
            if (allLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            bottom = if (showBottomBar) 0.dp else innerPadding.calculateBottomPadding()
                        )
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.size(100.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Text(
                        text = "Initialize Your Analytics Studio",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Track offline logs, weekly averages, drinking time grids, streaks, weekday charts, and heatmaps without cloud telemetry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Button(
                        onClick = {
                            triggerHaptic()
                            viewModel.logWater(250)
                            Toast.makeText(context, "Logged default test intake entry (250ml)!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Initial 250ml")
                    }
                }
            }
        } else {
            LazyColumn(
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
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("analytics_scrollable"),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Screen Title Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = "Analytics Dashboard",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Offline-first hydration diagnostics module",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                    }
                }

                // Range Range Selector / Filter Horizontal Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnalyticsFilter.values().forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable {
                                        triggerHaptic()
                                        selectedFilter = filter
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 1. TOP SUMMARY CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("top_summary_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "TODAY'S ANALYSIS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${formatVolume(todayIntakeMl)} / ${formatVolume(dailyGoal)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Text(
                                        text = "$hydrationStatus • ${(todayPercent * 100).toInt()}% Done",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor
                                    )
                                }
                            }

                            // Large Circular Progress Indicator ring
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { todayPercent },
                                    modifier = Modifier.fillMaxSize(),
                                    color = statusColor,
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = "${(todayPercent * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                // 2. HYDRATION SCORE CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("hydration_score_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                            imageVector = Icons.Default.HealthAndSafety,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Hydration Diagnostics Score",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Algorithmic wellness coefficient",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier.size(54.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { hydrationScore.toFloat() / 100f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = ColorExcellent,
                                        strokeWidth = 5.dp,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                    Text(
                                        text = "$hydrationScore",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Rating banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current Rating Status:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = performanceRatingText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Score Weights Breakdown explanation
                            Text(
                                text = "Contributors: Goal completion (40%), Weekly average (30%), Streaks (15%), Frequency (15%). Always local.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }

                // 2B. STREAK MULTI-STATS (Moved from Home)
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("streaks_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Hydration Streak Engine",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StreakStatColumn(
                                    emoji = "fire",
                                    label = "Current Streak",
                                    value = "${uiState.streak.currentStreak} Days"
                                )
                                StreakStatColumn(
                                    emoji = "crown",
                                    label = "Best Streak",
                                    value = "${uiState.streak.bestStreak} Days"
                                )
                                StreakStatColumn(
                                    emoji = "drop",
                                    label = "Total Days",
                                    value = "${uiState.streak.totalHydratedDays} Days"
                                )
                            }
                        }
                    }
                }

                // 2C. DAILY INSIGHTS (Moved from Home)
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("insights_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Insights",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InsightMetricBox(
                                    title = "Estimated Goal Completion",
                                    value = uiState.estimatedCompletionTime,
                                    icon = Icons.Outlined.Analytics,
                                    modifier = Modifier.weight(1f)
                                )
                                InsightMetricBox(
                                    title = "Average / Log Session",
                                    value = "${uiState.averageIntakePerSession} ml",
                                    icon = Icons.Outlined.LocalDrink,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 3. WEEKLY TREND (Beziers Daily Chart + Goal Line + Average Line)
                item {
                    val scaleColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    val barColor = MaterialTheme.colorScheme.primary

                    // Get totals for past 7 days
                    val weeklyAnalysisData = remember(allLogs) {
                        val totals = FloatArray(7)
                        val labels = mutableListOf<String>()
                        val df = SimpleDateFormat("E", Locale.getDefault())
                        
                        val cal = Calendar.getInstance()
                        val sumsMap = allLogs.groupBy {
                            cal.timeInMillis = it.timestamp
                            cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
                        }.mapValues { it.value.sumOf { e -> e.amountMl } }

                        val tempCal = Calendar.getInstance()
                        for (i in 0..6) {
                            tempCal.timeInMillis = System.currentTimeMillis()
                            tempCal.add(Calendar.DAY_OF_YEAR, -6 + i)
                            labels.add(df.format(tempCal.time))
                            val keyInt = tempCal.get(Calendar.YEAR) * 10000 + (tempCal.get(Calendar.MONTH) + 1) * 100 + tempCal.get(Calendar.DAY_OF_MONTH)
                            val sum = sumsMap[keyInt] ?: 0
                            totals[i] = sum.toFloat()
                        }
                        Triple(totals, labels, totals.average().toFloat())
                    }

                    var selectedColIndex by remember { mutableStateOf<Int?>(null) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Weekly Hydro Line Trend",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "7-day intake flow with target thresholds",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("7d", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            // Canvas Rendering for Weekly Bezier Line + Reference lines
                            val fillScrollPath = remember { Path() }
                            val strokeScrollPath = remember { Path() }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(weeklyAnalysisData) {
                                            detectTapGestures { offset ->
                                                val chartLeft = 40.dp.toPx()
                                                val chartRight = size.width - 16.dp.toPx()
                                                val chartWidth = chartRight - chartLeft
                                                val colWidth = chartWidth / 7f
                                                val x = offset.x - chartLeft
                                                if (x in 0f..chartWidth) {
                                                    selectedColIndex = (x / colWidth).toInt().coerceIn(0, 6)
                                                }
                                            }
                                        }
                                ) {
                                    val width = size.width
                                    val height = size.height
                                    val paddingLeft = 40.dp.toPx()
                                    val paddingBottom = 30.dp.toPx()
                                    val chartWidth = width - paddingLeft - 16.dp.toPx()
                                    val chartHeight = height - paddingBottom - 24.dp.toPx()

                                    val totals = weeklyAnalysisData.first
                                    val labels = weeklyAnalysisData.second
                                    val averageIntake = weeklyAnalysisData.third

                                    val maxChartVal = maxOf(totals.maxOrNull() ?: 1f, dailyGoal.toFloat() + 500f)

                                    // 1. Draw Axis & divisions
                                    drawLine(scaleColor, Offset(paddingLeft, height - paddingBottom), Offset(width - 16.dp.toPx(), height - paddingBottom), 1.dp.toPx())
                                    drawLine(scaleColor, Offset(paddingLeft, 10.dp.toPx()), Offset(paddingLeft, height - paddingBottom), 1.dp.toPx())

                                    // 2. Goal Dotted Reference Line
                                    val yGoal = height - paddingBottom - (dailyGoal.toFloat() / maxChartVal * chartHeight)
                                    drawLine(
                                        color = ColorExcellent.copy(alpha = 0.5f),
                                        start = Offset(paddingLeft, yGoal),
                                        end = Offset(width - 16.dp.toPx(), yGoal),
                                        strokeWidth = 1.5.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                                    )

                                    // 3. Average line representation
                                    val yAvg = height - paddingBottom - (averageIntake / maxChartVal * chartHeight)
                                    drawLine(
                                        color = ColorOnTrack.copy(alpha = 0.4f),
                                        start = Offset(paddingLeft, yAvg),
                                        end = Offset(width - 16.dp.toPx(), yAvg),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                                    )

                                    val points = mutableListOf<Offset>()
                                    val colWidth = chartWidth / 6f

                                    for (i in 0..6) {
                                        val x = paddingLeft + (i * colWidth)
                                        val y = height - paddingBottom - (totals[i] / maxChartVal * chartHeight)
                                        points.add(Offset(x, y))
                                    }

                                    // Draw background cubic gradient paths
                                    if (points.isNotEmpty()) {
                                        fillScrollPath.reset(); val fillPath = fillScrollPath.apply {
                                            moveTo(points[0].x, height - paddingBottom)
                                            lineTo(points[0].x, points[0].y)
                                            for (i in 1..6) {
                                                val prev = points[i - 1]
                                                val curr = points[i]
                                                val cx1 = prev.x + (curr.x - prev.x) / 2
                                                val cy1 = prev.y
                                                val cx2 = prev.x + (curr.x - prev.x) / 2
                                                val cy2 = curr.y
                                                cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                                            }
                                            lineTo(points.last().x, height - paddingBottom)
                                            close()
                                        }

                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(barColor.copy(alpha = 0.25f), Color.Transparent)
                                            )
                                        )

                                        // Draw smooth Bezier curve line
                                        strokeScrollPath.reset(); val strokePath = strokeScrollPath.apply {
                                            moveTo(points[0].x, points[0].y)
                                            for (i in 1..6) {
                                                val prev = points[i - 1]
                                                val curr = points[i]
                                                val cx1 = prev.x + (curr.x - prev.x) / 2
                                                val cy1 = prev.y
                                                val cx2 = prev.x + (curr.x - prev.x) / 2
                                                val cy2 = curr.y
                                                cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                                            }
                                        }

                                        drawPath(
                                            path = strokePath,
                                            color = barColor,
                                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                        )

                                        // Draw points & Highlighted Column
                                        for (i in 0..6) {
                                            val p = points[i]
                                            val isAct = selectedColIndex == i
                                            drawCircle(
                                                color = if (isAct) ColorExcellent else barColor,
                                                radius = if (isAct) 6.dp.toPx() else 4.dp.toPx(),
                                                center = p
                                            )
                                            if (isAct) {
                                                drawCircle(
                                                    color = ColorExcellent.copy(alpha = 0.2f),
                                                    radius = 12.dp.toPx(),
                                                    center = p
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic Selector Overlay values helper info
                            val localIndex = selectedColIndex
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                if (localIndex != null) {
                                    val amt = weeklyAnalysisData.first[localIndex].toInt()
                                    val dateLabel = weeklyAnalysisData.second[localIndex]
                                    Text(
                                        text = "$dateLabel: logged ${formatVolume(amt)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "Tap dots on line chart to inspect volume",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Goal Target: ${formatVolume(dailyGoal)} (Avg: ${formatVolume(weeklyAnalysisData.third.toInt())})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. MONTHLY TREND (30 day scrollable chart + Achievement Goal markers)
                item {
                    // Aggregate 30-day historical data
                    val monthlyTrackingTotals = remember(allLogs) {
                        val totals = mutableListOf<Pair<String, Float>>()
                        val dfLabel = SimpleDateFormat("dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        
                        val sumsMap = allLogs.groupBy {
                            cal.timeInMillis = it.timestamp
                            cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
                        }.mapValues { it.value.sumOf { e -> e.amountMl } }

                        val temp = Calendar.getInstance()
                        for (i in 0..29) {
                            temp.timeInMillis = System.currentTimeMillis()
                            temp.add(Calendar.DAY_OF_YEAR, -29 + i)
                            val keyInt = temp.get(Calendar.YEAR) * 10000 + (temp.get(Calendar.MONTH) + 1) * 100 + temp.get(Calendar.DAY_OF_MONTH)
                            val sum = sumsMap[keyInt] ?: 0
                            totals.add(Pair(dfLabel.format(temp.time), sum.toFloat()))
                        }
                        totals
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Monthly 30-Day Activity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Scroll horizontally. Goal achievement marked with star indicators.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            // Horizontal scroll bar layout for 30 bars
                            val maxMonthVal = maxOf(monthlyTrackingTotals.maxOfOrNull { it.second } ?: 1f, dailyGoal.toFloat() + 400f)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                monthlyTrackingTotals.forEach { dayRecord ->
                                    val dayName = dayRecord.first
                                    val amount = dayRecord.second
                                    val metGoal = amount >= dailyGoal
                                    
                                    val brush = if (metGoal) {
                                        Brush.verticalGradient(listOf(ColorExcellent, ColorExcellent.copy(alpha = 0.5f)))
                                    } else {
                                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Goal marker star
                                        if (metGoal) {
                                            Text("⭐", fontSize = 9.sp)
                                        } else {
                                            Spacer(modifier = Modifier.height(11.dp))
                                        }

                                        // Bar
                                        val heightDp = ((amount / maxMonthVal) * 120f).coerceAtLeast(8f).dp
                                        Box(
                                            modifier = Modifier
                                                .width(15.dp)
                                                .height(heightDp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(brush)
                                        )

                                        // Label day
                                        Text(
                                            text = dayName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            // Monthly summary statistics
                            val monthlyAvg = monthlyTrackingTotals.map { it.second }.average().toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "30-Day Average Volume:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formatVolume(monthlyAvg),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 5. YEARLY OVERVIEW (GitHub-Style Heatmap calendar)
                item {
                    val currentYear = currentHeatmapMonth.get(Calendar.YEAR)
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Yearly Grid Overview",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Heatmap calendar of grid consistency",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            triggerHaptic()
                                            val prev = Calendar.getInstance().apply {
                                                time = currentHeatmapMonth.time
                                                add(Calendar.MONTH, -1)
                                            }
                                            currentHeatmapMonth = prev
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                                    }
                                    
                                    Text(
                                        text = monthFormat.format(currentHeatmapMonth.time),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.widthIn(max = 110.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    IconButton(
                                        onClick = {
                                            triggerHaptic()
                                            val next = Calendar.getInstance().apply {
                                                time = currentHeatmapMonth.time
                                                add(Calendar.MONTH, 1)
                                            }
                                            currentHeatmapMonth = next
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                                    }
                                }
                            }

                            // Grid Rendering: Days of viewed calendar month
                            val daysInMonth = currentHeatmapMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                            val originalMock = currentHeatmapMonth.clone() as Calendar
                            originalMock.set(Calendar.DAY_OF_MONTH, 1)
                            val firstDayIndex = originalMock.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
                            val adjustedFirstDayIndex = if (firstDayIndex < 0) firstDayIndex + 7 else firstDayIndex

                            val totalGridCells = adjustedFirstDayIndex + daysInMonth
                            val rowsNeeded = (totalGridCells / 7f).let { Math.ceil(it.toDouble()).toInt() }

                            val daySumsMap = remember(allLogs) {
                                val cal = Calendar.getInstance()
                                allLogs.groupBy {
                                    cal.timeInMillis = it.timestamp
                                    cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
                                }.mapValues { entry ->
                                    entry.value.sumOf { it.amountMl }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Weekday headers
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayL ->
                                        Text(
                                            text = dayL,
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }

                                val cellCal = originalMock.clone() as Calendar
                                // Rows of grid squares
                                for (row in 0 until rowsNeeded) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        for (col in 0..6) {
                                            val cellNumber = row * 7 + col
                                            val dayInMonthVal = cellNumber - adjustedFirstDayIndex + 1

                                            if (cellNumber in adjustedFirstDayIndex until totalGridCells) {
                                                cellCal.set(Calendar.DAY_OF_MONTH, dayInMonthVal)
                                                val keyInt = cellCal.get(Calendar.YEAR) * 10000 + (cellCal.get(Calendar.MONTH) + 1) * 100 + cellCal.get(Calendar.DAY_OF_MONTH)
                                                val totalOnDay = daySumsMap[keyInt] ?: 0

                                                val colorClass = when {
                                                    totalOnDay == 0 -> Color(0xFFE0E0E0).copy(alpha = 0.5f) // Gray
                                                    totalOnDay >= dailyGoal * 1.2 -> Color(0xFF0D47A1) // Dark Blue (Goal Exceeded)
                                                    totalOnDay >= dailyGoal -> Color(0xFF1E88E5) // Blue (Goal Achieved)
                                                    else -> Color(0xFF90CAF9) // Light Blue (Partial)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(colorClass)
                                                        .clickable {
                                                            triggerHaptic()
                                                            Toast.makeText(context, "${dayInMonthVal} ${monthFormat.format(cellCal.time)} logged ${formatVolume(totalOnDay)}", Toast.LENGTH_SHORT).show()
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$dayInMonthVal",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = if (totalOnDay >= dailyGoal) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            } else {
                                                Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // GitHub Heatmap Legend labels
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Legend:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                LegendSquare(Color(0xFF0D47A1), "Exceeded")
                                LegendSquare(Color(0xFF1E88E5), "Achieved")
                                LegendSquare(Color(0xFF90CAF9), "Partial")
                                LegendSquare(Color(0xFFE0E0E0).copy(alpha = 0.7f), "No Log")
                            }
                        }
                    }
                }

                // 6. DRINKING TIME ANALYSIS (Clock ratios visualized on donut charts)
                item {
                    val clockData = remember(allLogs) {
                        var morningCount = 0
                        var afternoonCount = 0
                        var eveningCount = 0
                        var nightCount = 0
                        val calendar = Calendar.getInstance()

                        allLogs.forEach { log ->
                            calendar.timeInMillis = log.timestamp
                            val hr = calendar.get(Calendar.HOUR_OF_DAY)
                            when (hr) {
                                in 6..11 -> morningCount += log.amountMl
                                in 12..16 -> afternoonCount += log.amountMl
                                in 17..20 -> eveningCount += log.amountMl
                                else -> nightCount += log.amountMl
                            }
                        }
                        val finalSum = (morningCount + afternoonCount + eveningCount + nightCount).toFloat()
                        val rM = if (finalSum > 0) (morningCount / finalSum) else 0f
                        val rA = if (finalSum > 0) (afternoonCount / finalSum) else 0f
                        val rEv = if (finalSum > 0) (eveningCount / finalSum) else 0f
                        val rN = if (finalSum > 0) (nightCount / finalSum) else 0f
                        listOf(rM, rA, rEv, rN)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text(
                                    text = "Drinking Clock Cycle",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Intake volume ratio spread dynamically across solar times",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Draw circular pie slice donut on Canvas
                                Box(
                                    modifier = Modifier.size(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeW = 14.dp.toPx()
                                        val totalRatio = clockData.sum()
                                        
                                        if (totalRatio == 0f) {
                                            drawArc(
                                                color = Color.LightGray.copy(alpha = 0.4f),
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = strokeW)
                                            )
                                        } else {
                                            var startAn = -90f
                                            // Morning: blue
                                            if (clockData[0] > 0) {
                                                drawArc(
                                                    color = Color(0xFF64B5F6),
                                                    startAngle = startAn,
                                                    sweepAngle = clockData[0] * 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                                )
                                                startAn += clockData[0] * 360f
                                            }
                                            // Afternoon: teal
                                            if (clockData[1] > 0) {
                                                drawArc(
                                                    color = Color(0xFF4DB6AC),
                                                    startAngle = startAn,
                                                    sweepAngle = clockData[1] * 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                                )
                                                startAn += clockData[1] * 360f
                                            }
                                            // Evening: amber
                                            if (clockData[2] > 0) {
                                                drawArc(
                                                    color = Color(0xFFFFB74D),
                                                    startAngle = startAn,
                                                    sweepAngle = clockData[2] * 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                                )
                                                startAn += clockData[2] * 360f
                                            }
                                            // Night: purple
                                            if (clockData[3] > 0) {
                                                drawArc(
                                                    color = Color(0xFF9575CD),
                                                    startAngle = startAn,
                                                    sweepAngle = clockData[3] * 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("⏰", fontSize = 20.sp)
                                        Text("Flow Ratio", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                                    }
                                }

                                // Legend descriptions
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TimeAnalysisLabel(Color(0xFF64B5F6), "Morning (6 AM - 12 PM)",clockData[0])
                                    TimeAnalysisLabel(Color(0xFF4DB6AC), "Afternoon (12 PM - 5 PM)", clockData[1])
                                    TimeAnalysisLabel(Color(0xFFFFB74D), "Evening (5 PM - 9 PM)", clockData[2])
                                    TimeAnalysisLabel(Color(0xFF9575CD), "Night (9 PM - 6 AM)", clockData[3])
                                }
                            }
                        }
                    }
                }

                // 7. WEEKDAY ANALYSIS (Averages and highlighting high/low)
                item {
                    val weekdayStats = remember(allLogs) {
                        val calendar = Calendar.getInstance()
                        val sums = FloatArray(8) // index 1..7 (Sunday is 1, Monday is 2... Saturday is 7)
                        val daysCount = FloatArray(8)

                        // Optimize grouping using Calendar math directly to avoid parsing SimpleDateFormat string keys
                        val daysGrouped = mutableMapOf<Int, Pair<Int, Int>>() // YYYYMMDD -> Pair(DayOfWeek, SumOfMl)
                        allLogs.forEach { log ->
                            calendar.timeInMillis = log.timestamp
                            val y = calendar.get(Calendar.YEAR)
                            val m = calendar.get(Calendar.MONTH) + 1
                            val d = calendar.get(Calendar.DAY_OF_MONTH)
                            val dayInt = y * 10000 + m * 100 + d
                            val dow = calendar.get(Calendar.DAY_OF_WEEK)
                            
                            val existing = daysGrouped[dayInt]
                            if (existing == null) {
                                daysGrouped[dayInt] = Pair(dow, log.amountMl)
                            } else {
                                daysGrouped[dayInt] = Pair(dow, existing.second + log.amountMl)
                            }
                        }

                        daysGrouped.values.forEach { (dow, daySum) ->
                            sums[dow] += daySum.toFloat()
                            daysCount[dow] += 1f
                        }
                        
                        val averages = FloatArray(8)
                        for (i in 1..7) {
                            averages[i] = if (daysCount[i] > 0) sums[i] / daysCount[i] else 0f
                        }

                        // Map to standard Mon-Sun sequence indices (Monday=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7, Sun=1)
                        val weekdaysOrder = listOf(2, 3, 4, 5, 6, 7, 1)

                        val highestIndex = weekdaysOrder.maxByOrNull { averages[it] } ?: 2
                        val lowestIndex = weekdaysOrder.filter { averages[it] > 0f }.minByOrNull { averages[it] } ?: weekdaysOrder.first { averages[it] == 0f }

                        Triple(averages, highestIndex, lowestIndex)
                    }

                    val dowLabelsShort = mapOf(2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat", 1 to "Sun")
                    val dowLabelsLong = mapOf(2 to "Monday", 3 to "Tuesday", 4 to "Wednesday", 5 to "Thursday", 6 to "Friday", 7 to "Saturday", 1 to "Sunday")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column {
                                Text(
                                    text = "Weekday Averages & Bounds",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Historical volume by day of week",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            // weekday list
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val weekdaysList = listOf(2, 3, 4, 5, 6, 7, 1)
                                weekdaysList.forEach { dow ->
                                    val avg = weekdayStats.first[dow]
                                    val isStrongest = dow == weekdayStats.second && avg > 0
                                    val isWeakest = dow == weekdayStats.third

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isStrongest -> ColorExcellent.copy(alpha = 0.12f)
                                                    isWeakest -> ColorNeedsWater.copy(alpha = 0.08f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                }
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = dowLabelsLong[dow] ?: "N/A",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            
                                            if (isStrongest) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = ColorExcellent),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "STRONGEST",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else if (isWeakest) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = ColorNeedsWater),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "WEAKEST",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (avg > 0) formatVolume(avg.toInt()) else "0 ml logged",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isStrongest) ColorExcellent else if (isWeakest) ColorNeedsWater else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. PERSONAL RECORDS
                item {
                    val bestRecords = remember(allLogs, uiState.streak) {
                        val dfKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        val friendlyStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val groupDays = allLogs.groupBy { dfKey.format(Date(it.timestamp)) }
                        
                        // Maximum Intake Day
                        val maxDayEntry = groupDays.maxByOrNull { it.value.sumOf { entry -> entry.amountMl } }
                        val maxVal = maxDayEntry?.value?.sumOf { it.amountMl } ?: 250
                        val maxDate = maxDayEntry?.key?.let {
                            val parsed = dfKey.parse(it)
                            if (parsed != null) friendlyStr.format(parsed) else "N/A"
                        } ?: "N/A"

                        // Fastest Goal Completion Time
                        var fastestCompleteTime = "N/A"
                        var fastestCompleteDate = "N/A"
                        var fastestMs = Long.MAX_VALUE

                        groupDays.forEach { (dateKey, entryList) ->
                            val sortedIntake = entryList.sortedBy { it.timestamp }
                            var runningSum = 0
                            val dayStartCal = Calendar.getInstance().apply {
                                val parsed = dfKey.parse(dateKey) ?: return@apply
                                time = parsed
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            
                            sortedIntake.forEach { entry ->
                                runningSum += entry.amountMl
                                if (runningSum >= dailyGoal) {
                                    val duration = entry.timestamp - dayStartCal.timeInMillis
                                    if (duration < fastestMs) {
                                        fastestMs = duration
                                        val hrs = duration / (1000 * 60 * 60)
                                        val mins = (duration % (1000 * 60 * 60)) / (1000 * 60)
                                        fastestCompleteTime = "${hrs}h ${mins}m"
                                        val dt = dfKey.parse(dateKey)
                                        if (dt != null) fastestCompleteDate = friendlyStr.format(dt)
                                    }
                                    return@forEach
                                }
                            }
                        }

                        // Most Water Consumed In 30-Day Window
                        var maxMonthSum = 0
                        var maxMonthDateStr = "N/A"
                        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        
                        val groupedMonth = allLogs.groupBy { SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date(it.timestamp)) }
                        val highestMonthGroup = groupedMonth.maxByOrNull { it.value.sumOf { e -> e.amountMl } }
                        if (highestMonthGroup != null) {
                            maxMonthSum = highestMonthGroup.value.sumOf { it.amountMl }
                            val dt = SimpleDateFormat("yyyyMM", Locale.getDefault()).parse(highestMonthGroup.key)
                            if (dt != null) maxMonthDateStr = monthYearFormat.format(dt)
                        }

                        // Most Water Consumed In One Week
                        var maxWeekSum = 0
                        var maxWeekDateStr = "N/A"
                        val groupedWeeks = allLogs.groupBy { SimpleDateFormat("yyyyww", Locale.getDefault()).format(Date(it.timestamp)) }
                        val highestWeekGroup = groupedWeeks.maxByOrNull { it.value.sumOf { e -> e.amountMl } }
                        if (highestWeekGroup != null) {
                            maxWeekSum = highestWeekGroup.value.sumOf { it.amountMl }
                            val startCal = Calendar.getInstance()
                            highestWeekGroup.value.minByOrNull { it.timestamp }?.let {
                                startCal.timeInMillis = it.timestamp
                                maxWeekDateStr = "Week of " + friendlyStr.format(startCal.time)
                            }
                        }

                        Triple(maxVal, maxDate, Triple(fastestCompleteTime, fastestCompleteDate, Pair(maxMonthSum, maxMonthDateStr) to Pair(maxWeekSum, maxWeekDateStr)))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Hydration Milestones & Records",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                RecordItemRow(
                                    icon = "trophy",
                                    title = "Highest Daily Intake",
                                    value = formatVolume(bestRecords.first),
                                    date = bestRecords.second
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                RecordItemRow(
                                    icon = "fire",
                                    title = "Longest Hydration Streak",
                                    value = "${uiState.streak.bestStreak} Days",
                                    date = "Forever authenticated offline"
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                RecordItemRow(
                                    icon = "bolt",
                                    title = "Fastest Goal Completion",
                                    value = bestRecords.third.first,
                                    date = bestRecords.third.second
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                RecordItemRow(
                                    icon = "waves",
                                    title = "Most Monthly Consumed",
                                    value = formatVolume(bestRecords.third.third.first.first),
                                    date = bestRecords.third.third.first.second
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                RecordItemRow(
                                    icon = "drop",
                                    title = "Most Weekly Consumed",
                                    value = formatVolume(bestRecords.third.third.second.first),
                                    date = bestRecords.third.third.second.second
                                )
                            }
                        }
                    }
                }

                // 9. LIFETIME STATISTICS
                item {
                    val totalLogsCount = allLogs.size
                    val totalV = allLogs.sumOf { it.amountMl }
                    val distinctDaysCount = remember(allLogs) {
                        val cal = Calendar.getInstance()
                        allLogs.map { 
                            cal.timeInMillis = it.timestamp
                            cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
                        }.distinct().size
                    }

                    // Level computation
                    val currentLevel = (totalV / 500) + 1
                    val currentXp = totalV / 2

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Grid3x3, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Lifetime Totals & Registry",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PairStatsRow("Total Liquids Consumed", formatVolume(totalV))
                                PairStatsRow("Total Liters Logged", String.format("%.2f Liters", totalV / 1000f))
                                PairStatsRow("Total Intake Log Events", "$totalLogsCount logs")
                                PairStatsRow("Active Logging Period", "$distinctDaysCount days")
                                PairStatsRow("Streak (Current / Best)", "${uiState.streak.currentStreak} d / ${uiState.streak.bestStreak} d")
                                PairStatsRow("Goals Succeeded", "${uiState.streak.totalHydratedDays} goals")
                                PairStatsRow("Hydra Level (XP)", "Level $currentLevel ($currentXp XP)")
                            }
                        }
                    }
                }

                // 10. GOAL COMPLETION ANALYTICS
                item {
                    val completionRatios = remember(allLogs) {
                        val cal = Calendar.getInstance()
                        val group = allLogs.groupBy {
                            cal.timeInMillis = it.timestamp
                            cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
                        }.mapValues { it.value.sumOf { e -> e.amountMl } }
                        
                        val calcRate = { daysLimit: Int ->
                            if (daysLimit == -1) {
                                // Lifetime Rate
                                val completed = group.count { it.value >= dailyGoal }
                                if (group.isNotEmpty()) (completed.toFloat() / group.size * 100f).toInt() else 0
                            } else {
                                val temp = Calendar.getInstance()
                                val windowKeys = (0 until daysLimit).map { d ->
                                    temp.timeInMillis = System.currentTimeMillis()
                                    temp.add(Calendar.DAY_OF_YEAR, -d)
                                    temp.get(Calendar.YEAR) * 10000 + (temp.get(Calendar.MONTH) + 1) * 100 + temp.get(Calendar.DAY_OF_MONTH)
                                }.toSet()

                                val matches = group.filter { it.key in windowKeys }
                                val completedInWindow = matches.count { it.value >= dailyGoal }
                                if (matches.isNotEmpty()) (completedInWindow.toFloat() / matches.size * 100f).toInt() else 0
                            }
                        }
                        listOf(calcRate(7), calcRate(30), calcRate(90), calcRate(-1))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Goal Achievement Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalRateRow("7 Days Tracker", completionRatios[0])
                                HorizontalRateRow("30 Days Tracker", completionRatios[1])
                                HorizontalRateRow("90 Days Tracker", completionRatios[2])
                                HorizontalRateRow("Lifetime Progress Rate", completionRatios[3])
                            }
                        }
                    }
                }

                // PERSONAL RECORDS SECTION
                item {
                    val personalRecords = remember(allLogs, dailyGoal) {
                        val dateFormatLabel = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        
                        // First group logs into days with no string conversion
                        val dailyLogs = mutableMapOf<Int, MutableList<IntakeEntry>>()
                        allLogs.forEach { log ->
                            cal.timeInMillis = log.timestamp
                            val y = cal.get(Calendar.YEAR)
                            val m = cal.get(Calendar.MONTH) + 1
                            val d = cal.get(Calendar.DAY_OF_MONTH)
                            val dayKey = y * 10000 + m * 100 + d
                            
                            var list = dailyLogs[dayKey]
                            if (list == null) {
                                list = mutableListOf()
                                dailyLogs[dayKey] = list
                            }
                            list.add(log)
                        }

                        // 1. Highest Intake Day
                        val highestDayEntry = dailyLogs.maxByOrNull { it.value.sumOf { e -> e.amountMl } }
                        val highestIntakeStr = if (highestDayEntry != null) {
                            val sum = highestDayEntry.value.sumOf { it.amountMl }
                            
                            // Re-use single cal to format the date label nicely without parsing
                            cal.clear()
                            val yearVal = highestDayEntry.key / 10000
                            val monthVal = (highestDayEntry.key % 10000) / 100 - 1
                            val dayVal = highestDayEntry.key % 100
                            cal.set(yearVal, monthVal, dayVal)
                            val formattedDate = dateFormatLabel.format(cal.time)
                            "$sum ml ($formattedDate)"
                        } else "No logs recorded"

                        // 2. Longest Streak
                        val longestStreakVal = uiState.streak.bestStreak
                        val longestStreakStr = "$longestStreakVal Days"

                        // 3. Fastest Goal Completion
                        var fastestCompletionMs = Long.MAX_VALUE
                        var fastestCompletionDateStr = ""
                        
                        dailyLogs.forEach { (dayKey, entries) ->
                            val yearVal = dayKey / 10000
                            val monthVal = (dayKey % 10000) / 100 - 1
                            val dayVal = dayKey % 100
                            
                            cal.clear()
                            cal.set(yearVal, monthVal, dayVal, 0, 0, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val dayStartMs = cal.timeInMillis
                            
                            val sortedEntries = entries.sortedBy { it.timestamp }
                            var cumSum = 0
                            for (entry in sortedEntries) {
                                cumSum += entry.amountMl
                                if (cumSum >= dailyGoal) {
                                    val durationMs = entry.timestamp - dayStartMs
                                    if (durationMs in 1..fastestCompletionMs) {
                                        fastestCompletionMs = durationMs
                                        fastestCompletionDateStr = dateFormatLabel.format(cal.time)
                                    }
                                    break
                                }
                            }
                        }

                        val fastestGoalStr = if (fastestCompletionMs != Long.MAX_VALUE) {
                            val totalSeconds = fastestCompletionMs / 1000
                            val hours = totalSeconds / 3600
                            val minutes = (totalSeconds % 3600) / 60
                            if (hours > 0) {
                                "$hours hrs $minutes mins ($fastestCompletionDateStr)"
                            } else {
                                "$minutes mins ($fastestCompletionDateStr)"
                            }
                        } else "No goals achieved yet"

                        // 4. Most Consistent Week
                        val calendarForWeek = Calendar.getInstance()
                        val goalsByWeek = allLogs.groupBy {
                            calendarForWeek.timeInMillis = it.timestamp
                            val year = calendarForWeek.get(Calendar.YEAR)
                            val week = calendarForWeek.get(Calendar.WEEK_OF_YEAR)
                            "$year-W$week"
                        }.mapValues { (_, entries) ->
                            val localDaysGrouped = mutableMapOf<Int, Int>() // YYYYMMDD -> sumMl
                            entries.forEach { log ->
                                calendarForWeek.timeInMillis = log.timestamp
                                val y = calendarForWeek.get(Calendar.YEAR)
                                val m = calendarForWeek.get(Calendar.WEEK_OF_YEAR)
                                val d = calendarForWeek.get(Calendar.DAY_OF_MONTH)
                                val dayInt = y * 10000 + m * 100 + d
                                localDaysGrouped[dayInt] = (localDaysGrouped[dayInt] ?: 0) + log.amountMl
                            }
                            localDaysGrouped.values.count { it >= dailyGoal }
                        }
                        
                        val bestWeekEntry = goalsByWeek.maxByOrNull { it.value }
                        val mostConsistentWeekStr = if (bestWeekEntry != null) {
                            "Week ${bestWeekEntry.key.substringAfter("-W")} (${bestWeekEntry.value} / 7 Days Met)"
                        } else "No records recorded"

                        listOf(
                            Triple("Highest Intake Day", highestIntakeStr, Icons.Default.WaterDrop),
                            Triple("Longest Streak", longestStreakStr, Icons.Default.Whatshot),
                            Triple("Fastest Goal Completion", fastestGoalStr, Icons.Default.Speed),
                            Triple("Most Consistent Week", mostConsistentWeekStr, Icons.Default.EmojiEvents)
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("personal_records_section"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Trophy icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Lifetime Personal Records",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                personalRecords.forEach { record ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = record.third,
                                                contentDescription = record.first,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = record.first,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = record.second,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 11. LOCAL OFFLINE INSIGHTS
                item {
                    IntelligenceCoachInsightsPanel(
                        uiState = uiState,
                        dailyGoal = dailyGoal
                    )
                }

                // 12. EXPORT SECTION (CSV, JSON, Local Printable vector HTML-to-PDF Report)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("export_section").navigationBarsPadding(),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Secure local offline exports",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "dailyhydra provides fully localized data safety. Generated formats are created 100% locally on your device without transmitting data over cloud APIs.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )

                            // Export buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        // Package beautiful formatted printable HTML representing dynamic charts and grids and call the android Print system to print or save native PDF local files
                                        val htmlReport = generateHTMLExport(allLogs, dailyGoal, hydrationScore, performanceRatingText)
                                        exportPDFFormattedReport(context, htmlReport)
                                    },
                                    modifier = Modifier.fillMaxWidth(0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorExcellent)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export PDF", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// HTMLExport Generator for local print system
fun generateHTMLExport(allLogs: List<IntakeEntry>, goal: Int, score: Int, rating: String): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val printedTime = SimpleDateFormat("MMM dd, yyyy @ hh:mm a", Locale.getDefault()).format(Date())
    
    val tableRows = StringBuilder()
    allLogs.sortedByDescending { it.timestamp }.forEachIndexed { idx, item ->
        tableRows.append("""
            <tr>
                <td>${idx + 1}</td>
                <td>${df.format(Date(item.timestamp))}</td>
                <td><b>${item.amountMl} ml</b></td>
            </tr>
        """.trimIndent())
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>dailyhydra Comprehensive Health Analytics</title>
            <style>
                body {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    color: #212121;
                    padding: 40px;
                    line-height: 1.5;
                }
                .header {
                    text-align: center;
                    border-bottom: 2px solid #1E88E5;
                    padding-bottom: 20px;
                    margin-bottom: 30px;
                }
                .logo {
                    font-size: 28px;
                    font-weight: bold;
                    color: #1E88E5;
                    margin-bottom: 5px;
                }
                .subheader {
                    font-size: 14px;
                    color: #757575;
                }
                .score-card {
                    background: #F5F5F5;
                    border-radius: 12px;
                    padding: 24px;
                    margin-bottom: 30px;
                    border-left: 6px solid #1E88E5;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                .score-val {
                    font-size: 48px;
                    font-weight: 900;
                    color: #1E88E5;
                }
                .score-title {
                    font-size: 18px;
                    font-weight: bold;
                    margin-bottom: 5px;
                }
                .table-title {
                    font-size: 18px;
                    font-weight: bold;
                    margin: 20px 0 10px 0;
                    color: #0D47A1;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 15px;
                    margin-bottom: 40px;
                }
                th, td {
                    border: 1px solid #E0E0E0;
                    padding: 12px;
                    text-align: left;
                }
                th {
                    background-color: #1E88E5;
                    color: white;
                    font-weight: bold;
                }
                tr:nth-child(even) {
                    background-color: #F9F9F9;
                }
                .footer {
                    margin-top: 60px;
                    text-align: center;
                    font-size: 11px;
                    color: #9E9E9E;
                    border-top: 1px solid #E0E0E0;
                    padding-top: 15px;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="logo">dailyhydra Analytics Summary Report</div>
                <div class="subheader">Completely local, encrypted, zero metadata collection</div>
                <div class="subheader" style="margin-top: 5px;">Created: $printedTime</div>
            </div>

            <div class="score-card">
                <div>
                    <div class="score-title">Hydration Diagnostics Rating: $rating</div>
                    <div class="subheader">Factorized over daily targets, logging consistency, and streak history.</div>
                </div>
                <div class="score-val">$score/100</div>
            </div>

            <div class="table-title">Offline Consolidated Hydration Event Logs</div>
            <table>
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Date & Timestamp</th>
                        <th>Fluid Registered</th>
                    </tr>
                </thead>
                <tbody>
                    $tableRows
                </tbody>
            </table>

            <div class="footer">
                dailyhydra Open Source Project. All data remains exclusively within the sandboxed local Room DB registry on-device.
            </div>
        </body>
        </html>
    """.trimIndent()
}

// Native PDF Print system trigger
fun exportPDFFormattedReport(context: Context, htmlContent: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "dailyhydra_analytics_report"
        
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to initialize document printer: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// CSV local share exporter
fun exportCSV(context: Context, logs: List<IntakeEntry>) {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvBuilder = StringBuilder()
        csvBuilder.append("ID,Date and Time,Intake Volume (ml)\n")
        logs.sortedBy { it.timestamp }.forEach { entry ->
            val dateStr = sdf.format(Date(entry.timestamp))
            csvBuilder.append("${entry.id},\"$dateStr\",${entry.amountMl}\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "dailyhydra_logs.csv")
            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Save dailyhydra CSV Logs"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// JSON local share exporter
fun exportJSON(context: Context, logs: List<IntakeEntry>) {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("[\n")
        logs.sortedBy { it.timestamp }.forEachIndexed { idx, entry ->
            val dateStr = sdf.format(Date(entry.timestamp))
            jsonBuilder.append("  {\n")
            jsonBuilder.append("    \"id\": ${entry.id},\n")
            jsonBuilder.append("    \"timestamp\": ${entry.timestamp},\n")
            jsonBuilder.append("    \"date_time\": \"$dateStr\",\n")
            jsonBuilder.append("    \"intake_ml\": ${entry.amountMl}\n")
            jsonBuilder.append("  }")
            if (idx != logs.lastIndex) jsonBuilder.append(",")
            jsonBuilder.append("\n")
        }
        jsonBuilder.append("]")

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "dailyhydra_logs.json")
            putExtra(Intent.EXTRA_TEXT, jsonBuilder.toString())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Save dailyhydra JSON Backup"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting JSON: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LegendSquare(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
fun TimeAnalysisLabel(color: Color, name: String, pct: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        Text(
            text = "${(pct * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RecordItemRow(icon: String, title: String, value: String, date: String) {
    val (vector, tint) = when (icon) {
        "trophy" -> Pair(Icons.Default.EmojiEvents, Color(0xFFFFD700))
        "fire" -> Pair(Icons.Default.Whatshot, Color(0xFFFF5722))
        "bolt" -> Pair(Icons.Default.Bolt, Color(0xFFFFEB3B))
        "waves" -> Pair(Icons.Default.Waves, MaterialTheme.colorScheme.secondary)
        "drop" -> Pair(Icons.Default.WaterDrop, MaterialTheme.colorScheme.primary)
        else -> Pair(Icons.Default.Star, MaterialTheme.colorScheme.primary)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vector,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun PairStatsRow(label: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = valStr,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HorizontalRateRow(label: String, pct: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { pct.toFloat() / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun IntelligenceCoachInsightsPanel(
    uiState: HomeUiState,
    dailyGoal: Int
) {
    var activeTab by remember { mutableStateOf("Daily") }
    val allLogs = uiState.allEntries

    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("yyyyMM", Locale.getDefault())
    val thisMonthStr = monthFormat.format(Date())

    val logsByDay = remember(allLogs) {
        allLogs.groupBy { dateFormat.format(Date(it.timestamp)) }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coach_insights_panel")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Coach Insights Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Smart Coach Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Localized, diagnostic tracking intelligence",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Daily", "Weekly", "Monthly").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "InsightsTabContent"
            ) { currentTab ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (currentTab) {
                        "Daily" -> {
                            val daysMetGoalCount = (0 until 7).count { offset ->
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, -offset)
                                val dayStr = dateFormat.format(cal.time)
                                val dayIntake = logsByDay[dayStr]?.sumOf { it.amountMl } ?: 0
                                dayIntake >= dailyGoal
                            }
                            val consistencyPct = (daysMetGoalCount / 7.0 * 100.0).toInt()

                            CoachInsightItem(
                                title = "Today's Total Intake",
                                value = "${uiState.totalIntakeToday} ml",
                                description = "Measured liquid logged from modern inputs",
                                icon = Icons.Default.LocalDrink,
                                color = MaterialTheme.colorScheme.primary
                            )
                            CoachInsightItem(
                                title = "Goal Completion",
                                value = "${uiState.progressPercent}%",
                                description = "Progress achieved towards today's target",
                                icon = Icons.Default.CheckCircle,
                                color = if (uiState.progressPercent >= 100) ColorExcellent else ColorOnTrack,
                                progress = (uiState.progressPercent / 100f).coerceIn(0f, 1f)
                            )
                            CoachInsightItem(
                                title = "Consistency Score",
                                value = "$consistencyPct%",
                                description = "Goal completion frequency over past 7 days",
                                icon = Icons.Default.CalendarToday,
                                color = if (consistencyPct >= 80) ColorExcellent else if (consistencyPct >= 50) ColorOnTrack else ColorNeedsWater,
                                progress = consistencyPct / 100f
                            )
                        }

                        "Weekly" -> {
                            val weekDaysVolumes = (0 until 7).map { offset ->
                                val tempCal = Calendar.getInstance()
                                tempCal.add(Calendar.DAY_OF_YEAR, -offset)
                                val dayStr = dateFormat.format(tempCal.time)
                                val dayIntake = logsByDay[dayStr]?.sumOf { it.amountMl } ?: 0
                                Pair(dayStr, dayIntake)
                            }
                            
                            val weekAvg = weekDaysVolumes.map { it.second }.average().toInt()
                            val bestDayPair = weekDaysVolumes.maxByOrNull { it.second } ?: Pair("N/A", 0)
                            val worstDayPair = weekDaysVolumes.minByOrNull { it.second } ?: Pair("N/A", 0)
                            
                            val weeksGoalsCompleted = weekDaysVolumes.count { it.second >= dailyGoal }
                            val completionRate = (weeksGoalsCompleted / 7f * 100f).toInt()

                            CoachInsightItem(
                                title = "Weekly Average Intake",
                                value = "$weekAvg ml / day",
                                description = "Baseline liquid consumed in past 7 days",
                                icon = Icons.Default.BarChart,
                                color = MaterialTheme.colorScheme.primary
                            )
                            CoachInsightItem(
                                title = "Best Hydration Day",
                                value = "${bestDayPair.second} ml",
                                description = "Your highest intake peak during the week",
                                icon = Icons.Default.WorkspacePremium,
                                color = ColorExcellent
                            )
                            CoachInsightItem(
                                title = "Worst Hydration Day",
                                value = "${worstDayPair.second} ml",
                                description = "Sip more on slow days to maintain balance",
                                icon = Icons.Default.Warning,
                                color = if (worstDayPair.second < dailyGoal / 2) ColorBehind else ColorNeedsWater
                            )
                            CoachInsightItem(
                                title = "Goal Completion Rate",
                                value = "$completionRate%",
                                description = "Ratio of daily goals completed successfully",
                                icon = Icons.Default.CalendarToday,
                                color = if (completionRate >= 80) ColorExcellent else ColorOnTrack,
                                progress = completionRate / 100f
                            )
                        }

                        "Monthly" -> {
                            val thisMonthLogs = allLogs.filter { monthFormat.format(Date(it.timestamp)) == thisMonthStr }
                            val totalMl = thisMonthLogs.sumOf { it.amountMl }
                            val totalLiters = totalMl / 1000f

                            val daysInMonthWithLogs = thisMonthLogs.groupBy { dateFormat.format(Date(it.timestamp)) }.size
                            val avgDaily = if (daysInMonthWithLogs > 0) totalMl / daysInMonthWithLogs else 0

                            CoachInsightItem(
                                title = "Total Copious Intake",
                                value = String.format(Locale.getDefault(), "%.1f L", totalLiters),
                                description = "Aggregate water logging inside the current month",
                                icon = Icons.Default.LocalDrink,
                                color = MaterialTheme.colorScheme.primary
                            )
                            CoachInsightItem(
                                title = "Average Daily Intake",
                                value = "$avgDaily ml / day",
                                description = "Calculated across days with logged logs",
                                icon = Icons.Default.TrendingUp,
                                color = ColorOnTrack
                            )
                            CoachInsightItem(
                                title = "Longest Streak",
                                value = "${uiState.streak.bestStreak} Days",
                                description = "Your historic all-time hydration consistency peak",
                                icon = Icons.Default.Whatshot,
                                color = ColorExcellent
                            )
                            CoachInsightItem(
                                title = "Rank Progress",
                                value = uiState.nextRankName,
                                description = "Tier XP development toward next progress level",
                                icon = Icons.Default.EmojiEvents,
                                color = MaterialTheme.colorScheme.secondary,
                                progress = uiState.nextRankProgress
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachInsightItem(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    color: Color,
    progress: Float? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = color
                )
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = color,
                    trackColor = color.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
        }
    }
}
