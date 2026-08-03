package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CustomCup
import com.example.data.IntakeEntry
import com.example.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HydrationViewModel,
    onTabSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
    val presets by viewModel.quickLoggingPresets.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
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
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun triggerHaptic() {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val lastAddedEntry by viewModel.lastAddedEntry.collectAsStateWithLifecycle()

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

    // Dialog & Bottom Sheet visibility states
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var showHistoryBottomSheet by remember { mutableStateOf(false) }
    var editGoalDialogVisible by remember { mutableStateOf(false) }
    var editEntryDialogVisible by remember { mutableStateOf(false) }
    var activeEntryToEdit by remember { mutableStateOf<IntakeEntry?>(null) }
    
    var editCupDialogVisible by remember { mutableStateOf(false) }
    var activeCupToEdit by remember { mutableStateOf<CustomCup?>(null) }
    
    val windowSizeClass = LocalWindowSizeClass.current
    val showBottomBar = windowSizeClass == WindowSizeClass.COMPACT || windowSizeClass == WindowSizeClass.MEDIUM
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isSplit = isLandscape || windowSizeClass != WindowSizeClass.COMPACT && windowSizeClass != WindowSizeClass.MEDIUM
    
    var showTimePickerDialog by remember { mutableStateOf(false) }
    
    // Floating dynamic motivational greeting based on system hour
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour in 0..11 -> "Good Morning"
        hour in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val dateString = remember {
        SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date())
    }

    // Interactive celebrate state
    var triggerGoalCelebrationAlert by remember { mutableStateOf(false) }
    var lastObservedProgress by remember { mutableStateOf(0) }

    // Monitor for Goal Complete Celebration moment
    LaunchedEffect(uiState.progressPercent) {
        if (uiState.progressPercent >= 100 && lastObservedProgress < 100 && lastObservedProgress != 0) {
            triggerGoalCelebrationAlert = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastObservedProgress = uiState.progressPercent
    }

    LaunchedEffect(viewModel) {
        viewModel.navigateToHomeAndShowAdd.collect {
            showAddBottomSheet = true
        }
    }

    val leftPanel = @Composable {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
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
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Today's History Log shortcut badge
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHistoryBottomSheet = true
                    },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("btn_history_shortcut_left")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Show water intake logging history",
                            tint = if (isDarkWater) Color(0xFF00D4FF) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Logs",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDarkWater) Color(0xFF00D4FF) else MaterialTheme.colorScheme.primary
                        )
                    }
                }


            }
        }

        // Quick motivational quote
        val motivationMsg = when {
            uiState.progressPercent >= 100 -> "Spectacular effort! Goal fulfilled today."
            uiState.progressPercent >= 75 -> "Almost finished! Take another refreshing draft."
            uiState.progressPercent >= 40 -> "Sustaining momentum! Keep flowing."
            uiState.progressPercent > 0 -> "Excellent start! Hydration engine is active."
            else -> "A fresh, healthy day. Refresh with water!"
        }
        Text(
            text = motivationMsg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Smart Status Indicator Box
        SmartStatusCard(
            statusText = uiState.smartStatus,
            statusType = uiState.smartStatusType
        )

        // Main Hydration Hero Card
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isLightWater -> Color.Transparent
                    isDarkWater -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            ),
            border = BorderStroke(
                if (isWater) 1.5.dp else 1.dp,
                when {
                    isDarkWater -> Color(0xFF00D4FF).copy(alpha = 0.35f)
                    isLightWater -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .let {
                    when {
                        isLightWater -> {
                            it.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                ),
                                RoundedCornerShape(28.dp)
                            )
                        }
                        isDarkWater -> {
                            it.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF101820).copy(alpha = 0.2f),
                                        Color(0xFF000000).copy(alpha = 0.5f)
                                    )
                                ),
                                RoundedCornerShape(28.dp)
                            )
                        }
                        else -> it
                    }
                }
                .testTag("hero_card")
        ) {
            val breathingTransition = rememberInfiniteTransition(label = "breathing")
            val breathingScale by breathingTransition.animateFloat(
                initialValue = 0.97f,
                targetValue = 1.03f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breathingScale"
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Fluid Animated representation with History button overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = breathingScale
                                scaleY = breathingScale
                            }
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDarkWater -> {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF05070A),
                                                Color(0xFF00D4FF).copy(alpha = 0.15f)
                                            )
                                        )
                                    }
                                    isLightWater -> {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.White,
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                            )
                                        )
                                    }
                                    else -> {
                                        SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    }
                                }
                            )
                            .border(
                                width = if (isWater) 1.5.dp else 3.dp,
                                color = when {
                                    isDarkWater -> Color(0xFF00D4FF).copy(alpha = 0.3f)
                                    isLightWater -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                },
                                shape = CircleShape
                            )
                            .let {
                                if (isWater) {
                                    it.padding(4.dp).border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(
                                            colors = if (isDarkWater) {
                                                listOf(
                                                    Color(0xFF00D4FF),
                                                    Color(0xFF0094C6),
                                                    Color(0xFF5BE7FF),
                                                    Color(0xFF00D4FF)
                                                )
                                            } else {
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary,
                                                    MaterialTheme.colorScheme.tertiary,
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        ),
                                        shape = CircleShape
                                    )
                                } else {
                                    it
                                }
                            }
                            .padding(4.dp)
                            .let {
                                if (isWater) {
                                    it.drawBehind {
                                        if (isDarkWater) {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(Color(0xFF00D4FF).copy(alpha = 0.25f), Color.Transparent),
                                                    center = center,
                                                    radius = size.width * 0.55f
                                                ),
                                                radius = size.width * 0.55f
                                            )
                                        }
                                        // Draw a gorgeous glossy premium reflect highlight arc at the top-left
                                        drawArc(
                                            color = if (isDarkWater) Color(0xFF5BE7FF).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                                            startAngle = 180f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                                            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                        // Draw a small bright glowing drop glare light spot
                                        drawCircle(
                                            color = if (isDarkWater) Color(0xFF5BE7FF) else Color.White,
                                            radius = 4.dp.toPx(),
                                            center = Offset(size.width * 0.28f, size.height * 0.28f)
                                        )
                                    }
                                } else {
                                    it
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedWaterWave(
                            progressPercent = uiState.progressPercent,
                            goalStatus = uiState.goalStatus,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            isWaterTheme = isWater
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.progressPercent}%",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    shadow = if (isLightWater) {
                                        androidx.compose.ui.graphics.Shadow(
                                            color = Color.White.copy(alpha = 0.8f),
                                            offset = Offset(1f, 1f),
                                            blurRadius = 4f
                                        )
                                    } else if (isDarkWater) {
                                        androidx.compose.ui.graphics.Shadow(
                                            color = Color(0xFF00D4FF).copy(alpha = 0.8f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 12f
                                        )
                                    } else null
                                ),
                                color = if (isDarkWater) {
                                    Color.White
                                } else if (uiState.progressPercent > 45) {
                                    if (isLightWater) MaterialTheme.colorScheme.onBackground else Color.White
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Text(
                                text = "COMPLETE",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = if (isLightWater) {
                                        androidx.compose.ui.graphics.Shadow(
                                            color = Color.White.copy(alpha = 0.8f),
                                            offset = Offset(1f, 1f),
                                            blurRadius = 4f
                                        )
                                    } else if (isDarkWater) {
                                        androidx.compose.ui.graphics.Shadow(
                                            color = Color(0xFF00D4FF).copy(alpha = 0.6f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 6f
                                        )
                                    } else null
                                ),
                                color = if (isDarkWater) {
                                    Color(0xFF8CA4B3)
                                } else if (uiState.progressPercent > 45) {
                                    if (isLightWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                }
                            )
                        }
                    }


                }

                Spacer(modifier = Modifier.width(20.dp))

                // Right Column: Progress metrics
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TODAY'S INTAKE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${uiState.totalIntakeToday} ml",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TARGET",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${uiState.dailyGoalMls} ml",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    editGoalDialogVisible = true
                                }
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "REMAINING",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${uiState.remainingMls} ml",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (uiState.remainingMls > 0) MaterialTheme.colorScheme.primary else ColorExcellent
                            )
                        }
                    }

                    // Tap target to update Daily Goal
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            editGoalDialogVisible = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 14.dp)
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Goal", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    val rightPanel = @Composable {
        // Quick ml Add customizable panel
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_ml_add_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Quick ml Add",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Small "Customizable" indicator pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Customizable",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Tap any cup below to instantly log hydration into your database profile. Press on any item to customize its ml capacity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Presets list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presets.forEach { amt ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isWater) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    triggerHaptic()
                                    viewModel.logWater(amt)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DynamicGlassOfWater(
                                    amountMl = amt,
                                    glassSize = 40.dp,
                                    animated = false,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = "${amt}ml",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hydration Intelligence Coach Dashboard Panel
        HydrationCoachCard(
            uiState = uiState,
            isWater = isWater
        )
    }

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
                    currentTab = "home",
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
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = if (showBottomBar) 0.dp else innerPadding.calculateBottomPadding()
                )
                .let { mod ->
                    if (isDarkWater) {
                        mod.drawBehind {
                            val w = size.width
                            val h = size.height
                            
                            // Abyssal deep ocean textured flows
                            val path1 = Path()
                            path1.moveTo(0f, h * 0.25f)
                            path1.quadraticTo(w * 0.35f, h * 0.15f, w * 0.65f, h * 0.35f)
                            path1.quadraticTo(w * 0.85f, h * 0.45f, w, h * 0.3f)
                            drawPath(
                                path = path1,
                                color = Color(0xFF00D4FF).copy(alpha = 0.04f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            
                            val path2 = Path()
                            path2.moveTo(0f, h * 0.75f)
                            path2.quadraticTo(w * 0.45f, h * 0.85f, w * 0.7f, h * 0.62f)
                            path2.quadraticTo(w * 0.9f, h * 0.52f, w, h * 0.68f)
                            drawPath(
                                path = path2,
                                color = Color(0xFF0094C6).copy(alpha = 0.04f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            
                            // Glowing cyber core atmospheric glow top right
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF00D4FF).copy(alpha = 0.06f), Color.Transparent),
                                    center = Offset(w * 0.85f, h * 0.15f),
                                    radius = w * 0.65f
                                )
                            )
                            
                            // Mid bottom core secondary glow and subtle depth spots
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF0094C6).copy(alpha = 0.04f), Color.Transparent),
                                    center = Offset(w * 0.2f, h * 0.8f),
                                    radius = w * 0.55f
                                )
                            )
                        }
                    } else if (isLightWater) {
                        mod.drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFF4FDFF),
                                        Color(0xFFE6F9FD)
                                    )
                                )
                            )
                        }
                    } else {
                        mod
                    }
                }
        ) {
            if (isSplit) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        leftPanel()
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        rightPanel()
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Today's History Log shortcut badge
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showHistoryBottomSheet = true
                        },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("btn_history_shortcut_compact")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Show water intake logging history",
                                tint = if (isDarkWater) Color(0xFF00D4FF) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Logs",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isDarkWater) Color(0xFF00D4FF) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }


                }
            }

            // Quick motivational quote
            val motivationMsg = when {
                uiState.progressPercent >= 100 -> "Spectacular effort! Goal fulfilled today."
                uiState.progressPercent >= 75 -> "Almost finished! Take another refreshing draft."
                uiState.progressPercent >= 40 -> "Sustaining momentum! Keep flowing."
                uiState.progressPercent > 0 -> "Excellent start! Hydration engine is active."
                else -> "A fresh, healthy day. Refresh with water!"
            }
            Text(
                text = motivationMsg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Smart Status Indicator Box
            SmartStatusCard(
                statusText = uiState.smartStatus,
                statusType = uiState.smartStatusType
            )

            // Main Hydration Hero Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isLightWater -> Color.Transparent
                        isDarkWater -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                ),
                border = BorderStroke(
                    if (isWater) 1.5.dp else 1.dp,
                    when {
                        isDarkWater -> Color(0xFF00D4FF).copy(alpha = 0.35f)
                        isLightWater -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .let {
                        when {
                            isLightWater -> {
                                it.background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        )
                                    ),
                                    RoundedCornerShape(28.dp)
                                )
                            }
                            isDarkWater -> {
                                it.background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF101820).copy(alpha = 0.2f),
                                            Color(0xFF000000).copy(alpha = 0.5f)
                                        )
                                    ),
                                    RoundedCornerShape(28.dp)
                                )
                            }
                            else -> it
                        }
                    }
                    .testTag("hero_card")
            ) {
                val breathingTransition = rememberInfiniteTransition(label = "breathing")
                val breathingScale by breathingTransition.animateFloat(
                    initialValue = 0.97f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "breathingScale"
                )

                val isGoalAchieved = uiState.progressPercent >= 100
                val breakProgress by animateFloatAsState(
                    targetValue = if (isGoalAchieved) 1f else 0f,
                    animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                    label = "ring_break_progress"
                )

                val themePrimaryColor = MaterialTheme.colorScheme.primary
                val themeSecondaryColor = MaterialTheme.colorScheme.secondary
                val themeTertiaryColor = MaterialTheme.colorScheme.tertiary

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Fluid Animated representation with History button overlay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = breathingScale
                                    scaleY = breathingScale
                                }
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDarkWater -> {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF05070A),
                                                    Color(0xFF00D4FF).copy(alpha = 0.15f)
                                                )
                                            )
                                        }
                                        isLightWater -> {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White,
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                        else -> {
                                            SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        }
                                    }
                                )
                                .let { baseModifier ->
                                    if (breakProgress > 0f) {
                                        baseModifier.drawBehind {
                                            val strokeWidthVal = 3.dp.toPx()
                                            val translation = breakProgress * 30.dp.toPx()
                                            val alphaVal = (1f - breakProgress).coerceIn(0f, 1f)
                                            
                                            val colorsList = if (isDarkWater) {
                                                listOf(Color(0xFF00D4FF), Color(0xFF0094C6), Color(0xFF5BE7FF), Color(0xFF00D4FF))
                                            } else {
                                                listOf(
                                                    themePrimaryColor,
                                                    themeSecondaryColor,
                                                    themeTertiaryColor,
                                                    themePrimaryColor
                                                )
                                            }
                                            
                                            val segmentAngle = 80f
                                            val gapAngle = 10f
                                            for (k in 0..3) {
                                                val startAngle = k * (segmentAngle + gapAngle)
                                                val centerAngleRad = java.lang.Math.toRadians((startAngle + segmentAngle / 2f).toDouble())
                                                
                                                val dx = (java.lang.Math.cos(centerAngleRad) * translation).toFloat()
                                                val dy = (java.lang.Math.sin(centerAngleRad) * translation).toFloat()
                                                
                                                val fragmentColor = colorsList[k % colorsList.size].copy(alpha = alphaVal)
                                                
                                                drawArc(
                                                    color = fragmentColor,
                                                    startAngle = startAngle,
                                                    sweepAngle = segmentAngle,
                                                    useCenter = false,
                                                    topLeft = Offset(4.dp.toPx() + dx, 4.dp.toPx() + dy),
                                                    size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                                                    style = Stroke(width = strokeWidthVal)
                                                )
                                            }
                                        }
                                    } else {
                                        val m1 = baseModifier.border(
                                            width = if (isWater) 1.5.dp else 3.dp,
                                            color = when {
                                                isDarkWater -> Color(0xFF00D4FF).copy(alpha = 0.3f)
                                                isLightWater -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            },
                                            shape = CircleShape
                                        )
                                        if (isWater) {
                                            m1.padding(4.dp).border(
                                                width = 3.dp,
                                                brush = Brush.sweepGradient(
                                                    colors = if (isDarkWater) {
                                                        listOf(
                                                            Color(0xFF00D4FF),
                                                            Color(0xFF0094C6),
                                                            Color(0xFF5BE7FF),
                                                            Color(0xFF00D4FF)
                                                        )
                                                    } else {
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary,
                                                            MaterialTheme.colorScheme.tertiary,
                                                            MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                ),
                                                shape = CircleShape
                                            )
                                        } else {
                                            m1
                                        }
                                    }
                                }
                                .padding(4.dp)
                                .let {
                                    if (isWater) {
                                        it.drawBehind {
                                            if (isDarkWater) {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(Color(0xFF00D4FF).copy(alpha = 0.25f), Color.Transparent),
                                                        center = center,
                                                        radius = size.width * 0.55f
                                                    ),
                                                    radius = size.width * 0.55f
                                                )
                                            }
                                            // Draw a gorgeous glossy premium reflect highlight arc at the top-left
                                            drawArc(
                                                color = if (isDarkWater) Color(0xFF5BE7FF).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                                                startAngle = 180f,
                                                sweepAngle = 90f,
                                                useCenter = false,
                                                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                                                size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                                                style = Stroke(width = 3.dp.toPx())
                                            )
                                            // Draw a small bright glowing drop glare light spot
                                            drawCircle(
                                                color = if (isDarkWater) Color(0xFF5BE7FF) else Color.White,
                                                radius = 4.dp.toPx(),
                                                center = Offset(size.width * 0.28f, size.height * 0.28f)
                                            )
                                        }
                                    } else {
                                        it
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedWaterWave(
                                progressPercent = uiState.progressPercent,
                                goalStatus = uiState.goalStatus,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                isWaterTheme = isWater
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.progressPercent}%",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        shadow = if (isLightWater) {
                                            androidx.compose.ui.graphics.Shadow(
                                                color = Color.White.copy(alpha = 0.8f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 4f
                                            )
                                        } else if (isDarkWater) {
                                            androidx.compose.ui.graphics.Shadow(
                                                color = Color(0xFF00D4FF).copy(alpha = 0.8f),
                                                offset = Offset(0f, 0f),
                                                blurRadius = 12f
                                            )
                                        } else null
                                    ),
                                    color = if (isDarkWater) {
                                        Color.White
                                    } else if (uiState.progressPercent > 45) {
                                        if (isLightWater) MaterialTheme.colorScheme.onBackground else Color.White
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                Text(
                                    text = "COMPLETE",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        shadow = if (isLightWater) {
                                            androidx.compose.ui.graphics.Shadow(
                                                color = Color.White.copy(alpha = 0.8f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 4f
                                            )
                                        } else if (isDarkWater) {
                                            androidx.compose.ui.graphics.Shadow(
                                                color = Color(0xFF00D4FF).copy(alpha = 0.6f),
                                                offset = Offset(0f, 0f),
                                                blurRadius = 6f
                                            )
                                        } else null
                                    ),
                                    color = if (isDarkWater) {
                                        Color(0xFF8CA4B3)
                                    } else if (uiState.progressPercent > 45) {
                                        if (isLightWater) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    }
                                )
                            }
                        }


                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Right Column: Progress metrics
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "TODAY'S INTAKE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.totalIntakeToday} ml",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "TARGET",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${uiState.dailyGoalMls} ml",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        editGoalDialogVisible = true
                                    }
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "REMAINING",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${uiState.remainingMls} ml",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (uiState.remainingMls > 0) MaterialTheme.colorScheme.primary else ColorExcellent
                                )
                            }
                        }

                        // Tap target to update Daily Goal
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editGoalDialogVisible = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 14.dp)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Goal", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Quick ml Add customizable panel
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_ml_add_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Quick ml Add",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Small "Customizable" indicator pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Customizable",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "Instantly record preset hydration. Edit items anytime in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Row of quick preset options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (presets.isEmpty()) {
                            Text(
                                text = "No presets configured. Go to Settings to add some!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            presets.forEach { amt ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isWater) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            triggerHaptic()
                                            viewModel.logWater(amt)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        DynamicGlassOfWater(
                                            amountMl = amt,
                                            glassSize = 40.dp,
                                            animated = false,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                        Text(
                                            text = "${amt}ml",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }



            // Hydration Intelligence Coach Dashboard Panel
            HydrationCoachCard(
                uiState = uiState,
                isWater = isWater
            )
            
            Spacer(modifier = Modifier.height(100.dp))
            }
            }
        }
    }

    // Modal Bottom Sheet: Manual Entry & Create Custom Cup combined
    if (showAddBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("add_water_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Text(
                    text = "Add Hydration Record",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Tab 1: Manual Water Entry
                var manualMlText by remember { mutableStateOf("") }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "MANUAL WATER LOGGING",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = manualMlText,
                        onValueChange = { manualMlText = it.filter { c -> c.isDigit() } },
                        label = { Text("Volume (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_manual_ml"),
                        singleLine = true,
                        trailingIcon = { Text("ml", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 10.dp)) }
                    )

                    // Quick Select Manual presets
                    val bottomPresets = listOf(150, 250, 330, 500)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bottomPresets.forEach { pre ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    manualMlText = pre.toString()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                label = { Text("${pre}ml") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val mlVal = manualMlText.toIntOrNull()
                            if (mlVal != null && mlVal > 0) {
                                viewModel.logWater(mlVal)
                                showAddBottomSheet = false
                            }
                        },
                        enabled = manualMlText.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_log_manual_ml"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Log Water Intake", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Bottom Sheet: History list with edit / delete capabilities
    if (showHistoryBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistoryBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("history_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                if (uiState.todayEntries.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "No records",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No hydration logged today yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = uiState.todayEntries,
                                key = { it.id }
                            ) { entry ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeEntryToEdit = entry
                                            editEntryDialogVisible = true
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left circular blue hydration container
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    color = if (isDarkWater) Color(0xFF00D4FF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WaterDrop,
                                                contentDescription = null,
                                                tint = if (isDarkWater) Color(0xFF00D4FF) else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Volume/time text detail
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "${entry.amountMl} ml",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "at ${timeFormat.format(Date(entry.timestamp))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        // Edit action button
                                        IconButton(
                                            onClick = {
                                                activeEntryToEdit = entry
                                                editEntryDialogVisible = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit manual water log",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Trash delete action button
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.deleteIntakeEntry(entry)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete entry log",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialog: Edit Goal Setting
    if (editGoalDialogVisible) {
        var goalText by remember { mutableStateOf(uiState.dailyGoalMls.toString()) }
        Dialog(onDismissRequest = { editGoalDialogVisible = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("edit_goal_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Customize Hydration Goal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Advisable standard health guidelines advise logging at least 2000-3000 ml water every day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter { c -> c.isDigit() } },
                        label = { Text("Daily target value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = { Text("ml") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_goal")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { editGoalDialogVisible = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val target = goalText.toIntOrNull()
                                if (target != null && target > 0) {
                                    viewModel.setDailyGoal(target)
                                    editGoalDialogVisible = false
                                }
                            }
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }

    // Dialog: Edit Log Entry ml
    if (editEntryDialogVisible && activeEntryToEdit != null) {
        var editEntryAmountText by remember { mutableStateOf(activeEntryToEdit!!.amountMl.toString()) }
        Dialog(onDismissRequest = { editEntryDialogVisible = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Logged Intake",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = editEntryAmountText,
                        onValueChange = { editEntryAmountText = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantity (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = { Text("ml") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editEntryDialogVisible = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val sizeVal = editEntryAmountText.toIntOrNull()
                                if (sizeVal != null && sizeVal > 0) {
                                    viewModel.editIntakeEntry(activeEntryToEdit!!, sizeVal)
                                    editEntryDialogVisible = false
                                    activeEntryToEdit = null
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }



    // Dialog: Edit Reminder alert time
    if (showTimePickerDialog) {
        val reminderChoiceTimes = listOf("08:00 AM", "09:00 AM", "10:30 AM", "12:00 PM", "02:00 PM", "03:30 PM", "05:00 PM", "06:30 PM", "08:00 PM")
        Dialog(onDismissRequest = { showTimePickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configure Reminder Alarm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose preferred day hour to receive reminder triggers:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reminderChoiceTimes.forEach { rawTime ->
                            val isSelected = uiState.nextReminderTime == rawTime
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setNextReminderTime(rawTime)
                                        showTimePickerDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = rawTime, style = MaterialTheme.typography.bodyLarge)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePickerDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }

    // Celebration Alert overlay when goal reached
    if (triggerGoalCelebrationAlert) {
        Dialog(onDismissRequest = { triggerGoalCelebrationAlert = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("celebration_alert")
            ) {
                Column(
                    modifier = Modifier.padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = "Celebration",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Text(
                        text = "100% Hydrated!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Amazing achievement! You hit your daily water volume requirement. Your health feels vibrant!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { triggerGoalCelebrationAlert = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Wonderful!")
                    }
                }
            }
        }
    }
}

// Sub-component: Smart Status Banner
@Composable
fun SmartStatusCard(
    statusText: String,
    statusType: String
) {
    val (bgColor, textColor, imageVector) = when (statusType) {
        "FULLY" -> Triple(ColorExcellent.copy(alpha = 0.15f), ColorExcellent, Icons.Default.Celebration)
        "EXCELLENT" -> Triple(ColorExcellent.copy(alpha = 0.15f), ColorExcellent, Icons.Default.WorkspacePremium)
        "GOOD" -> Triple(ColorOnTrack.copy(alpha = 0.15f), ColorOnTrack, Icons.Default.WaterDrop)
        "LOW" -> Triple(ColorNeedsWater.copy(alpha = 0.15f), ColorNeedsWater, Icons.Default.LocalDrink)
        else -> Triple(ColorBehind.copy(alpha = 0.15f), ColorBehind, Icons.Default.Warning)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_status_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = statusType,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Smart Hydration Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor.copy(alpha = 0.8f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
            }
        }
    }
}

// Sub-component: Hydration Coach Intelligence Panel
@Composable
fun HydrationCoachCard(
    uiState: HomeUiState,
    isWater: Boolean
) {
    val statusColor = when (uiState.smartStatusType) {
        "FULLY", "EXCELLENT" -> ColorExcellent
        "GOOD" -> ColorOnTrack
        "LOW" -> ColorNeedsWater
        else -> ColorBehind
    }

    val statusIcon = when (uiState.smartStatusType) {
        "FULLY" -> Icons.Default.Celebration
        "EXCELLENT" -> Icons.Default.WorkspacePremium
        "GOOD" -> Icons.Default.WaterDrop
        "LOW" -> Icons.Default.LocalDrink
        else -> Icons.Default.Warning
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hydration_coach_panel")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Coach Avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Hydration Intelligence Coach",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "Message icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = uiState.dynamicCoachMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "Score", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text("Hydro Score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Text(
                            text = "${uiState.hydrationScore} / 100",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(statusIcon, contentDescription = "Status", tint = statusColor, modifier = Modifier.size(16.dp))
                            Text("Coach Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Text(
                            text = uiState.hydrationStatus,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Rank Progress", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("Next Rank", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Text(
                        text = uiState.nextRankName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LinearProgressIndicator(
                        progress = { uiState.nextRankProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 2.dp)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Goal", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Today's Goal Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Text(
                            text = "${uiState.progressPercent}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    val volumeText = if (uiState.totalIntakeToday >= 1000 || uiState.dailyGoalMls >= 1000) {
                        String.format(Locale.getDefault(), "%.1fL / %.1fL", uiState.totalIntakeToday / 1000f, uiState.dailyGoalMls / 1000f)
                    } else {
                        "${uiState.totalIntakeToday}ml / ${uiState.dailyGoalMls}ml"
                    }
                    
                    Text(
                        text = volumeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    LinearProgressIndicator(
                        progress = { (uiState.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }
            }
        }
    }
}

// Sub-component: Animated waves drawer canvas
@Composable
fun AnimatedWaterWave(
    progressPercent: Int,
    goalStatus: String,
    modifier: Modifier = Modifier,
    isWaterTheme: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffsetState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    // Smooth liquid transition animation: rise naturally like real liquid!
    val animatedProgressFractionState = animateFloatAsState(
        targetValue = (progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "waterFillRise"
    )

    // Continuous floating bubbles timer
    val bubbleProgressionState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleProgression"
    )

    // Twinkling sparklers timer for completion
    val sparkleAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )

    // Dynamic color shifts: always refreshing water/oceanic blue
    val waveBaseColor = Color(0xFF00B2FE) // Rich oceanic blue
    
    val waveAccentColor = Color(0xFF00E5FF) // Light neon cyan

    Canvas(modifier = modifier) {
        val waveOffset = waveOffsetState.value
        val animatedProgressFraction = animatedProgressFractionState.value
        val bubbleProgression = bubbleProgressionState.value
        val sparkleAlpha = sparkleAlphaState.value

        val width = size.width
        val height = size.height

        // Inverse height calculation because canvas coordinates start top-left
        val waterLevelY = height - (height * animatedProgressFraction)

        val path1 = Path()
        val path2 = Path()

        path1.moveTo(0f, height)
        path2.moveTo(0f, height)

        val waveAmplitude = 10.dp.toPx()
        val waveFrequency = 0.012f

        val step = 6
        for (x in 0..width.toInt() step step) {
            val xF = x.toFloat()
            val y1 = waterLevelY + waveAmplitude * kotlin.math.sin(waveFrequency * xF + waveOffset)
            path1.lineTo(xF, y1)

            val y2 = waterLevelY + (waveAmplitude * 0.7f) * kotlin.math.cos(waveFrequency * xF - waveOffset)
            path2.lineTo(xF, y2)
        }

        // Force connection to end coordinates for a clean finish on the right edge
        val xFMax = width
        val y1End = waterLevelY + waveAmplitude * kotlin.math.sin(waveFrequency * xFMax + waveOffset)
        path1.lineTo(xFMax, y1End)
        val y2End = waterLevelY + (waveAmplitude * 0.7f) * kotlin.math.cos(waveFrequency * xFMax - waveOffset)
        path2.lineTo(xFMax, y2End)

        path1.lineTo(width, height)
        path2.lineTo(width, height)

        path1.close()
        path2.close()

        // Draw background wave (Path 2)
        drawPath(
            path = path2,
            brush = Brush.verticalGradient(
                colors = listOf(
                    waveAccentColor.copy(alpha = 0.45f),
                    waveBaseColor.copy(alpha = 0.25f)
                )
            )
        )

        // Draw foreground wave (Path 1)
        drawPath(
            path = path1,
            brush = Brush.verticalGradient(
                colors = listOf(
                    waveAccentColor.copy(alpha = 0.95f),
                    waveBaseColor.copy(alpha = 0.8f),
                    waveBaseColor.copy(alpha = 0.95f)
                )
            )
        )

        // Floating procedural bubble particles inside the water
        val bubbleCount = 14
        for (i in 0 until bubbleCount) {
            // Deterministic position seeding
            val seedX = (0.12f + 0.76f * (i * 0.93f % 1f)) * width
            val seedRadius = (3.dp.toPx() + 3.5.dp.toPx() * (i * 0.41f % 1f))
            val speedFactor = 0.7f + 0.6f * (i * 0.57f % 1f)

            // Ascending floating coordinates
            val bubbleYProgression = (bubbleProgression * speedFactor + (i * 0.15f)) % 1f
            val bubbleY = height - (height * bubbleYProgression)

            // Draw only if completely submerged inside wave limit and within bounded circle height
            if (bubbleY > waterLevelY + seedRadius && bubbleY < height - seedRadius) {
                // Lateral wave sway animation
                val swayX = seedX + kotlin.math.sin(bubbleProgression * 2 * Math.PI.toFloat() + i) * 5.dp.toPx()
                
                // Outer ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = seedRadius,
                    center = Offset(swayX, bubbleY),
                    style = Stroke(width = 1.dp.toPx())
                )
                // Glare highlight spot inside bubble
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = seedRadius * 0.3f,
                    center = Offset(swayX - seedRadius * 0.35f, bubbleY - seedRadius * 0.35f)
                )
            }
        }

        // Draw golden/yellow twinkling diamond sparkles when goal is fully completed
        if (goalStatus == "Completed") {
            val starCount = 4
            for (i in 0 until starCount) {
                val starX = (0.22f + 0.56f * (i * 0.73f % 1f)) * width
                val starY = (0.18f + 0.45f * (i * 0.39f % 1f)) * height
                val starSize = 7.dp.toPx() + 4.dp.toPx() * (i * 0.5f % 1f)

                val starPath = Path().apply {
                    moveTo(starX, starY - starSize)
                    quadraticTo(starX, starY, starX + starSize, starY)
                    quadraticTo(starX, starY, starX, starY + starSize)
                    quadraticTo(starX, starY, starX - starSize, starY)
                    quadraticTo(starX, starY, starX, starY - starSize)
                    close()
                }
                drawPath(
                    path = starPath,
                    color = Color(0xFFFFF59D).copy(alpha = sparkleAlpha * (0.6f + 0.4f * (i % 2))),
                )
            }
        }
    }
}

// Sub-component: Horizontal Streak column metrics
@Composable
fun StreakStatColumn(
    emoji: String,
    label: String,
    value: String
) {
    val (vector, tint) = when (emoji) {
        "fire" -> Pair(Icons.Default.Whatshot, Color(0xFFFF5722))
        "crown" -> Pair(Icons.Default.WorkspacePremium, Color(0xFFFFD700))
        else -> Pair(Icons.Default.WaterDrop, MaterialTheme.colorScheme.primary)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = vector,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// Sub-component: Metric insight widget box
@Composable
fun InsightMetricBox(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun DailyHydrationTipCard(
    viewModel: HydrationViewModel,
    isWater: Boolean
) {
    val tipText by viewModel.hydrationTipText.collectAsStateWithLifecycle()
    val isLoading by viewModel.hydrationTipLoading.collectAsStateWithLifecycle()
    val errorMsg by viewModel.hydrationTipError.collectAsStateWithLifecycle()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_hydration_tip_panel")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Tip of the Day Logo",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Daily Hydration Tip",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { viewModel.loadDailyHydrationTip(force = true) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("refresh_tip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Tip",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Consulting Hydration Expert...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tipText.ifBlank { "Analyzing water intake to generate today's advice..." },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (errorMsg != null) {
                            Text(
                                text = errorMsg ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { viewModel.loadDailyHydrationTip(force = true) }
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "100% Offline & Open Source",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getCupIcon(iconName: String): ImageVector {
    return when (iconName) {
        "glass" -> Icons.Default.LocalDrink
        "bottle" -> Icons.Default.WaterDrop
        "steel" -> Icons.Default.Kitchen // representation for steel bottle
        "tumbler" -> Icons.Default.Coffee
        "mug" -> Icons.Default.FreeBreakfast
        else -> Icons.Default.LocalDrink
    }
}
