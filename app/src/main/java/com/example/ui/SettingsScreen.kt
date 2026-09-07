package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.CustomCup
import com.example.data.IntakeEntry
import com.example.ui.theme.ColorExcellent
import com.example.ui.theme.ColorNeedsWater
import com.example.ui.theme.ColorOnTrack
import org.json.JSONArray
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.scheduler.DailyHydraScheduler
import com.example.scheduler.NotificationHelper
import com.example.scheduler.NotificationLogger
import androidx.compose.ui.res.painterResource
import com.example.R
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HydrationViewModel,
    onTabSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Observe App State
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Settings state flows from DB
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val currentGoalUnit by viewModel.goalUnit.collectAsStateWithLifecycle()
    val isHapticEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
    val isAnimationsEnabled by viewModel.animationsEnabled.collectAsStateWithLifecycle()
    val presets by viewModel.quickLoggingPresets.collectAsStateWithLifecycle()
    val isAutoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val receiveNotifsAfterGoal by viewModel.notifsAfterGoalCompletion.collectAsStateWithLifecycle()

    // Dialog & UI flows
    var showAddCupDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showResetHistoryConfirmationDialog by remember { mutableStateOf(false) }
    var showBackupManagementDialog by remember { mutableStateOf(false) }
    var cupToEdit by remember { mutableStateOf<CustomCup?>(null) }
    
    var showAboutDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showQuickLogPresetsDialog by remember { mutableStateOf(false) }
    var showSmartStartDialog by remember { mutableStateOf(false) }
    var showSmartEndDialog by remember { mutableStateOf(false) }
    var showWipeDataDialog by remember { mutableStateOf(false) }
    

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(com.example.scheduler.NotificationHelper.isIgnoringBatteryOptimizations(context))
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(com.example.scheduler.NotificationHelper.canScheduleExactAlarms(context))
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = com.example.scheduler.NotificationHelper.isIgnoringBatteryOptimizations(context)
                canScheduleExactAlarms = com.example.scheduler.NotificationHelper.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    var showThemeMenu by remember { mutableStateOf(false) }
    var showUnitMenu by remember { mutableStateOf(false) }
    var showNotificationDetailPanel by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission allowed! 🔔", Toast.LENGTH_SHORT).show()
            DailyHydraScheduler.scheduleNextReminder(context)
        } else {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonString = exportBackupToJson(viewModel)
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    Toast.makeText(context, "Data successfully saved of aqora! 📂", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (jsonString != null) {
                    val count = importBackupFromJson(jsonString, viewModel)
                    Toast.makeText(context, "Successfully imported $count aqora entries! 🎉", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "File is empty", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Reactive handler: automatically reschedule hydration alarms whenever settings are modified
    LaunchedEffect(
        uiState.remindersEnabled,
        uiState.smartReminderIntervalMins,
        uiState.smartReminderDayStart,
        uiState.smartReminderDayEnd
    ) {
        DailyHydraScheduler.scheduleNextReminder(context)
    }

    fun triggerHapticFeedback(type: HapticFeedbackType = HapticFeedbackType.LongPress) {
        if (isHapticEnabled) {
            haptic.performHapticFeedback(type)
        }
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val showBottomBar = windowSizeClass == WindowSizeClass.COMPACT || windowSizeClass == WindowSizeClass.MEDIUM

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                GlassmorphicNavBar(
                    currentTab = "settings",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = if (showBottomBar) 0.dp else innerPadding.calculateBottomPadding()
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("settings_scrollable"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            // Header Screen title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            triggerHapticFeedback()
                            onTabSelected("home")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        style = Modifier.let { MaterialTheme.typography.titleLarge }.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Brand section: Centered "aqora"
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aqora_logo),
                        contentDescription = "aqora logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Text(
                        text = "aqora",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "About aqora",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                triggerHapticFeedback()
                                showAboutDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // CARD 1: General Preferences (Theme, Unit, Goal, Presets)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                ) {
                    Column {
                        // 1. Theme Dropdown row
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showThemeMenu = true }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Theme",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentTheme,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                val themes = listOf("System", "Light", "Dark", "Light Water", "Dark Water")
                                themes.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            triggerHapticFeedback()
                                            viewModel.setAppTheme(t)
                                            showThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // 2. Animations Toggle row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Animation,
                                    contentDescription = "Animations",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Animations",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAnimationsEnabled) "Enabled (Smooth transitions & effects)" else "Disabled (0 animation mode)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = isAnimationsEnabled,
                                onCheckedChange = { checked ->
                                    triggerHapticFeedback()
                                    viewModel.setAnimationsEnabled(checked)
                                },
                                modifier = Modifier.testTag("settings_animations_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // 3. Unit system dropdown row
                        Box {
                            val unitLabel = if (currentGoalUnit == "ml") "Metric (ml, kg)" else "Imperial (oz, lbs)"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showUnitMenu = true }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Straighten,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Unit System",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = unitLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showUnitMenu,
                                onDismissRequest = { showUnitMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Metric (ml, kg)", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        triggerHapticFeedback()
                                        viewModel.setGoalUnit("ml")
                                        showUnitMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Imperial (oz, lbs)", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        triggerHapticFeedback()
                                        viewModel.setGoalUnit("Liter")
                                        showUnitMenu = false
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // 3. Goal edit row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback()
                                    showGoalDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Water Goal",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentGoalUnit == "ml") "${uiState.dailyGoalMls} ml" else String.format("%.2f L", uiState.dailyGoalMls / 1000f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // 4. Quick add presets customization presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback()
                                    showQuickLogPresetsDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Quick-Add Cup Sizes",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = presets.joinToString(", ") { "${it} ml" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // CARD 2: Reminders & Intelligent Notifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback()
                                    showNotificationDetailPanel = !showNotificationDetailPanel
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notifications",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.remindersEnabled) "Active • Every ${uiState.smartReminderIntervalMins}m" else "Adjust it to your routine",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = if (showNotificationDetailPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(visible = showNotificationDetailPanel) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                SettingsSwitchRow(
                                    title = "Enable Water Suggester",
                                    subtitle = "Receive periodic reminders to keep your body optimized",
                                    checked = uiState.remindersEnabled,
                                    onCheckedChange = { isEnabled ->
                                        triggerHapticFeedback()
                                        viewModel.setRemindersEnabled(isEnabled, context)
                                        if (isEnabled) {
                                            val isPermGranted = NotificationHelper.isNotificationPermissionGranted(context)
                                            if (!isPermGranted) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                        }
                                    }
                                )

                                AnimatedVisibility(visible = uiState.remindersEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        SettingsSwitchRow(
                                            title = "Receive Notifications After Goal Completion",
                                            subtitle = "Continue sending periodic drink reminders even after meeting today's goal",
                                            checked = receiveNotifsAfterGoal,
                                            onCheckedChange = { isEnabled ->
                                                triggerHapticFeedback()
                                                viewModel.setNotifsAfterGoalCompletion(isEnabled)
                                            }
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        SettingsClickableRow(
                                            title = "Day start limit",
                                            value = uiState.smartReminderDayStart,
                                            icon = Icons.Outlined.Alarm,
                                            onClick = {
                                                triggerHapticFeedback()
                                                showSmartStartDialog = true
                                            }
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        SettingsClickableRow(
                                            title = "Day end limit",
                                            value = uiState.smartReminderDayEnd,
                                            icon = Icons.Outlined.Bedtime,
                                            onClick = {
                                                triggerHapticFeedback()
                                                showSmartEndDialog = true
                                            }
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        // Alert filtering tips
                                        // Alert Reliability Checker
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Alert Reliability Checker",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            // Row 1: Battery Optimization Whitelist status
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isIgnoringBatteryOptimizations) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                                                border = BorderStroke(1.dp, if (isIgnoringBatteryOptimizations) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isIgnoringBatteryOptimizations) "✅" else "⚠️",
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Battery Optimization Whitelist",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                                color = if (isIgnoringBatteryOptimizations) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                                            )
                                                            Text(
                                                                text = if (isIgnoringBatteryOptimizations) 
                                                                    "Optimal: Battery restrictions ignored. Hydration alerts will arrive on time." 
                                                                    else "Restricted: Android system might put the app to sleep. Alerts could be heavily delayed.",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                    if (!isIgnoringBatteryOptimizations) {
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Button(
                                                            onClick = {
                                                                triggerHapticFeedback()
                                                                try {
                                                                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                                        data = android.net.Uri.parse("package:${context.packageName}")
                                                                    }
                                                                    context.startActivity(intent)
                                                                 } catch (e: Exception) {
                                                                    try {
                                                                        val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                                        context.startActivity(intent)
                                                                    } catch (ex: Exception) {
                                                                        Toast.makeText(context, "Please configure in System Settings", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text("Whitelist App", style = MaterialTheme.typography.labelMedium)
                                                        }
                                                     }
                                                 }
                                             }

                                             // Row 2: Exact Alarms permission status
                                             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                 Surface(
                                                     shape = RoundedCornerShape(12.dp),
                                                     color = if (canScheduleExactAlarms) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                                                     border = BorderStroke(1.dp, if (canScheduleExactAlarms) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                                     modifier = Modifier.fillMaxWidth()
                                                 ) {
                                                     Column(modifier = Modifier.padding(12.dp)) {
                                                         Row(
                                                             verticalAlignment = Alignment.CenterVertically,
                                                             horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                         ) {
                                                             Text(
                                                                 text = if (canScheduleExactAlarms) "✅" else "⚠️",
                                                                 style = MaterialTheme.typography.bodyLarge
                                                             )
                                                             Column(modifier = Modifier.weight(1f)) {
                                                                 Text(
                                                                     text = "Precise Alert Timing Permission",
                                                                     style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                                     color = if (canScheduleExactAlarms) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                                                 )
                                                                 Text(
                                                                     text = if (canScheduleExactAlarms) 
                                                                         "Optimal: Precise alarm clock level scheduling allowed." 
                                                                         else "Restricted: Device will bunch alarms together, delaying notifications.",
                                                                     style = MaterialTheme.typography.bodySmall,
                                                                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                                 )
                                                             }
                                                         }
                                                         if (!canScheduleExactAlarms) {
                                                             Spacer(modifier = Modifier.height(10.dp))
                                                             Button(
                                                                 onClick = {
                                                                     triggerHapticFeedback()
                                                                     try {
                                                                         val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                                             data = android.net.Uri.parse("package:${context.packageName}")
                                                                         }
                                                                         context.startActivity(intent)
                                                                     } catch (e: Exception) {
                                                                         Toast.makeText(context, "Exact Alarm setting is only supported on Android 12+", Toast.LENGTH_SHORT).show()
                                                                     }
                                                                 },
                                                                 shape = RoundedCornerShape(8.dp),
                                                                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                                 modifier = Modifier.fillMaxWidth()
                                                             ) {
                                                                 Text("Grant Exact Alarms", style = MaterialTheme.typography.labelMedium)
                                                             }
                                                         }
                                                     }
                                                 }
                                             }
                                         }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("🎯", style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    text = "Notifications will only surface between ${uiState.smartReminderDayStart} and ${uiState.smartReminderDayEnd}.",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Reminder Frequency Interval",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        val intervals = listOf(Pair(15, "15 Min"), Pair(30, "30 Min"), Pair(60, "1 Hour"), Pair(120, "2 Hours"), Pair(180, "3 Hours"), Pair(240, "4 Hours"))
                                        val ignoredIntervals = listOf(
                                            Pair(30, "30 Min"),
                                            Pair(45, "45 Min"),
                                            Pair(60, "1 Hour"),
                                            Pair(90, "1.5 Hrs"),
                                            Pair(120, "2 Hours")
                                        )
                                        val isCustomActive = !intervals.map { it.first }.contains(uiState.smartReminderIntervalMins)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(Pair(15, "15m"), Pair(30, "30m"), Pair(60, "1h"), Pair(120, "2h"), Pair(180, "3h"), Pair(240, "4h")).forEach { (mins, name) ->
                                                val isSelected = !isCustomActive && uiState.smartReminderIntervalMins == mins
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                        .clickable {
                                                            triggerHapticFeedback()
                                                            viewModel.setSmartReminderInterval(mins, context)
                                                        }
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = name,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1.1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isCustomActive) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                    .clickable {
                                                        triggerHapticFeedback()
                                                        if (!isCustomActive) {
                                                            viewModel.setSmartReminderInterval(150, context)
                                                        }
                                                    }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Custom",
                                                    color = if (isCustomActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }

                                        if (isCustomActive) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            var tempCustomMins by remember(uiState.smartReminderIntervalMins) {
                                                mutableStateOf(uiState.smartReminderIntervalMins.toString())
                                            }
                                            OutlinedTextField(
                                                value = tempCustomMins,
                                                onValueChange = { input ->
                                                    val filtered = input.filter { it.isDigit() }
                                                    tempCustomMins = filtered
                                                    val vol = filtered.toIntOrNull() ?: 0
                                                    if (vol > 0) {
                                                        viewModel.setSmartReminderInterval(vol, context)
                                                    }
                                                },
                                                label = { Text("Custom Interval (minutes)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CARD 3: Data backup, Import and Restore options
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                ) {
                    Column {
                        // Export logs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback()
                                    exportLauncher.launch("aqora_backup.json")
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export data",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Backup your days, history, and cups",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Import logs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback()
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Import data",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Restore your days, history, and cups",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }



                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Reset hydration history (New Row)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback()
                                    showResetHistoryConfirmationDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset hydration history",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Clear daily logs only. Retain settings and custom cups",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Wipe all data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback()
                                    showWipeDataDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Wipe all data",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Permanently delete streaks, XP and history",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // CARD 4: Privacy & Security
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                ) {
                    Column {
                        // Privacy & Security (New Row!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback()
                                    showPrivacyDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Privacy Statement",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your data stays on your device. Always.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

             // Wipe Data System Dialog
    if (showWipeDataDialog) {
        var confirmText by remember { mutableStateOf("") }
        var countdownSeconds by remember { mutableStateOf(3) }
        var isDeleting by remember { mutableStateOf(false) }
        var isSuccessShown by remember { mutableStateOf(false) }

        // Live timer countdown for anti-accidental protection (unlocked after 3 seconds)
        LaunchedEffect(showWipeDataDialog) {
            countdownSeconds = 3
            confirmText = ""
            isDeleting = false
            isSuccessShown = false
            triggerHapticFeedback(HapticFeedbackType.LongPress)
            while (countdownSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                countdownSeconds--
            }
        }

        AlertDialog(
            onDismissRequest = { 
                if (!isDeleting) {
                    showWipeDataDialog = false 
                }
            },
            icon = {
                val iconColor = if (isSuccessShown) ColorExcellent else MaterialTheme.colorScheme.error
                val iconVal = if (isSuccessShown) Icons.Default.CheckCircle else Icons.Default.DeleteForever
                Icon(
                    imageVector = iconVal,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                val titleText = when {
                    isSuccessShown -> "Erasure Complete"
                    isDeleting -> "Securing Erasure..."
                    else -> "Wipe All App Data?"
                }
                val titleColor = if (isSuccessShown) ColorExcellent else MaterialTheme.colorScheme.error
                Text(
                    text = titleText,
                    color = titleColor,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                AnimatedContent(
                    targetState = when {
                        isSuccessShown -> 2
                        isDeleting -> 1
                        else -> 0
                    },
                    label = "wipe_flow_transition"
                ) { state ->
                    when (state) {
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "All local aqora data has been permanently erased.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "The application has been successfully returned to its initial clean-install state.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.error,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Permanently clearing sqlite records, cache, diagnostic files, and local alarm schedules...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "This action permanently deletes all hydration history, streaks, goals, settings, shortcuts, achievements, reminders, and local data. This cannot be undone.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Justify
                                )
                                
                                Text(
                                    text = "To proceed, please type DELETE below:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = confirmText,
                                    onValueChange = { confirmText = it },
                                    placeholder = { Text("Type DELETE here") },
                                    modifier = Modifier.fillMaxWidth().testTag("wipe_confirm_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.error,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                        cursorColor = MaterialTheme.colorScheme.error
                                    )
                                )

                                if (countdownSeconds > 0) {
                                    Text(
                                        text = "Anti-accidental lock active for ${countdownSeconds}s...",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                } else {
                                    Text(
                                        text = "Safe Lock Released",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ColorExcellent,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val isUnlocked = countdownSeconds == 0 && confirmText == "DELETE"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isDeleting && !isSuccessShown) {
                        TextButton(
                            onClick = { showWipeDataDialog = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("wipe_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    when {
                        isSuccessShown -> {
                            Button(
                                onClick = {
                                    triggerHapticFeedback()
                                    showWipeDataDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("wipe_complete_ok")
                            ) {
                                Text("OK")
                            }
                        }
                        isDeleting -> {
                            // While wiping, show no active action button to ensure uninterrupted database operations
                        }
                        else -> {
                            Button(
                                onClick = {
                                    if (isUnlocked) {
                                        triggerHapticFeedback(HapticFeedbackType.LongPress)
                                        isDeleting = true
                                        viewModel.secureWipeAllData(context) {
                                            isDeleting = false
                                            isSuccessShown = true
                                        }
                                    } else {
                                        triggerHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                enabled = isUnlocked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.testTag("wipe_confirm_button")
                            ) {
                                Text("DELETE")
                            }
                        }
                    }
                }
            }
        )
    }

    // Goal Configuration Dialog
    if (showGoalDialog) {
        var goalText by remember { mutableStateOf(uiState.dailyGoalMls.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text(text = "Set Daily Water Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customize your targeted hydration goal in milliliters (ml):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter { char -> char.isDigit() } },
                        label = { Text("Water Goal (ml)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showGoalDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val amount = goalText.toIntOrNull() ?: 2500
                            viewModel.setDailyGoal(amount)
                            showGoalDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        )
    }

    // Custom Quick-Add Presets Modification Dialog
    if (showQuickLogPresetsDialog) {
        var tempPresets by remember { mutableStateOf(presets) }
        var newAmountText by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showQuickLogPresetsDialog = false },
            title = { Text(text = "Customize Quick Logging") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Customize the quick-add buttons on your Home screen. Tap a cup size to delete. (Maximum 5 items)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // List existing items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        if (tempPresets.isEmpty()) {
                            Text(
                                text = "No quick-log options. Add below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp).align(Alignment.CenterHorizontally)
                            )
                        } else {
                            tempPresets.forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        DynamicGlassOfWater(
                                            amountMl = preset,
                                            glassSize = 28.dp,
                                            animated = false
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (currentGoalUnit == "ml") "${preset} ml" else String.format("%.2f L", preset / 1000f),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            tempPresets = tempPresets.filter { it != preset }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove preset",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newAmountText,
                            onValueChange = { input -> newAmountText = input.filter { it.isDigit() } },
                            label = { Text("Volume (ml)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val amt = newAmountText.toIntOrNull()
                                if (amt != null && amt > 0) {
                                    if (tempPresets.contains(amt)) {
                                        Toast.makeText(context, "${amt}ml is already in presets", Toast.LENGTH_SHORT).show()
                                    } else if (tempPresets.size >= 5) {
                                        Toast.makeText(context, "Max 5 quick log options allowed", Toast.LENGTH_SHORT).show()
                                    } else {
                                        tempPresets = (tempPresets + amt).sorted()
                                        newAmountText = ""
                                    }
                                }
                            },
                            enabled = newAmountText.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add preset", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            tempPresets = listOf(100, 250, 500, 750, 1000)
                        },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Reset to Defaults")
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showQuickLogPresetsDialog = false }
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            if (tempPresets.isEmpty()) {
                                Toast.makeText(context, "Please add at least one preset", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.setQuickLoggingPresets(tempPresets)
                                showQuickLogPresetsDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        )
    }

    // Smart Reminder start and end selection dialogs
    if (showSmartStartDialog) {
        TimeSelectionDialog(
            title = "Set Day Start Time",
            initialTime = uiState.smartReminderDayStart,
            onDismiss = { showSmartStartDialog = false },
            onSave = { selectedTime ->
                viewModel.setSmartReminderDayStart(selectedTime, context)
            }
        )
    }

    if (showSmartEndDialog) {
        TimeSelectionDialog(
            title = "Set Day End Time",
            initialTime = uiState.smartReminderDayEnd,
            onDismiss = { showSmartEndDialog = false },
            onSave = { selectedTime ->
                viewModel.setSmartReminderDayEnd(selectedTime, context)
            }
        )
    }



    // Add / Edit Custom Cup Dialog
    if (showAddCupDialog) {
        val editing = cupToEdit
        var cupName by remember { mutableStateOf(editing?.name ?: "") }
        var cupAmount by remember { mutableStateOf(editing?.amountMl?.toString() ?: "250") }
        var selectedIconName by remember { mutableStateOf(editing?.iconName ?: "glass") }

        AlertDialog(
            onDismissRequest = { showAddCupDialog = false },
            title = { Text(text = if (editing != null) "Edit Cup Template" else "Add Custom Cup Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = cupName,
                        onValueChange = { cupName = it },
                        label = { Text("Cup Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cupAmount,
                        onValueChange = { cupAmount = it.filter { char -> char.isDigit() } },
                        label = { Text("Cup Capacity (ml)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Choose Cup Aesthetic Icon:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    val iconsList = listOf(
                        "glass" to Icons.Default.LocalDrink,
                        "bottle" to Icons.Default.WaterDrop,
                        "steel" to Icons.Default.SportsBar,
                        "tumbler" to Icons.Default.WineBar,
                        "mug" to Icons.Default.Coffee,
                        "drop" to Icons.Default.Opacity,
                        "tea" to Icons.Default.FreeBreakfast
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        iconsList.forEach { (name, icon) ->
                            val isChosen = selectedIconName == name
                            IconButton(
                                onClick = { selectedIconName = name },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isChosen) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    )
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAddCupDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val finalName = cupName.ifEmpty { "My Cup" }
                            val amt = cupAmount.toIntOrNull() ?: 250
                            if (editing != null) {
                                viewModel.editCustomCup(editing, finalName, amt, selectedIconName)
                            } else {
                                viewModel.addCustomCup(finalName, amt, selectedIconName)
                            }
                            showAddCupDialog = false
                        }
                    ) {
                        Text("Save Template")
                    }
                }
            }
        )
    }

    // Upgraded License / About info Dialog
    if (showAboutDialog) {
        val sampleContributors = listOf("demonbazillionz")
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aqora_logo),
                        contentDescription = "aqora logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "aqora",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "GitHub Repository Link:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "https://github.com/demonbazillionz/aqora",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Open Source License (GPLv3):",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "aqora is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n\n" +
                                    "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Privacy Statement Summary:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "aqora stores your database file entirely under secure SQLite local app storage. Absolutely no networks and no trackers exist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Core Contributors Group:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        sampleContributors.forEach {
                            Text(
                                text = "• $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "  Artificial Intelligent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { showAboutDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Privacy Statement Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Privacy & Security", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "aqora is designed with a strict stance of physical-media privacy.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val bulletPoints = listOf(
                        "Local-Only SQLite Sandbox" to "All logged hydration ounces/milliliters, customizable cups, streak counts, ranks, and metadata are persisted exclusively inside your device's local internal SQLite storage.",
                        "No Cloud Sync / Servers" to "No data is transmitted, synchronized, or sent to a cloud or secondary remote back-end server.",
                        "Zero Trackers or Telemetry" to "The app contains no analytics packages, no performance metric logs, and no crash reports that send data to outer networks.",
                        "No Advertising SDKs" to "No third-party advertisements or ad servers are integrated within aqora.",
                        "No Account Required" to "No signup, login, email address, password, or proprietary profile creation is required to use the app in any capacity."
                    )
                    
                    bulletPoints.forEach { (heading, explanation) ->
                        Column {
                            Text(
                                text = "✔ $heading",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.73f),
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { showPrivacyDialog = false }) {
                        Text("Understand & Agree")
                    }
                }
            }
        )
    }

    // Reset History Confirmation Dialog
    if (showResetHistoryConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetHistoryConfirmationDialog = false },
            title = { Text("Reset Hydration History", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to clear your daily hydration logs only?\n\nThis will keep all custom cup designs, preset buttons, achievements, and app-wide configurations intact, but it will clear your logged daily water data. Since history is cleared, active day streaks will reset.")
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showResetHistoryConfirmationDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            viewModel.clearIntakeHistoryOnly(context)
                            Toast.makeText(context, "Hydration logging history has been reset! 💧", Toast.LENGTH_LONG).show()
                            showResetHistoryConfirmationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear Logs")
                    }
                }
            }
        )
    }

    // Backup Management Dialog
    if (showBackupManagementDialog) {
        val backupsDir = java.io.File(context.filesDir, "backups")
        val backupsList = remember(showBackupManagementDialog) {
            val list = mutableListOf<java.io.File>()
            for (i in 1..3) {
                val f = java.io.File(backupsDir, "dailyhydra_auto_backup_$i.json")
                if (f.exists()) {
                    list.add(f)
                }
            }
            list
        }
        var fileToRestore by remember { mutableStateOf<java.io.File?>(null) }

        AlertDialog(
            onDismissRequest = { showBackupManagementDialog = false },
            title = { Text("Local Backups Directory", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "aqora keeps your last 3 auto backups locally. You can restore any of them below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (backupsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No auto backups found yet.\n(Backups generate automatically when you log intake if enabled)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        backupsList.forEachIndexed { idx, file ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val dateStr = sdf.format(Date(file.lastModified()))
                            val kbSize = "%.2f KB".format(file.length() / 1024.0)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto Backup #${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "$dateStr • $kbSize",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            fileToRestore = file
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Restore", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showBackupManagementDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )

        if (fileToRestore != null) {
            AlertDialog(
                onDismissRequest = { fileToRestore = null },
                title = { Text("Restore Confirmation") },
                text = {
                    Text("Are you sure you want to restore this local backup? Doing so will replace your current app history cleanly. This action cannot be undone.")
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { fileToRestore = null }) {
                            Text("Go Back")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                try {
                                    val jsonStr = fileToRestore?.readText() ?: ""
                                    val count = importBackupFromJson(jsonStr, viewModel)
                                    Toast.makeText(context, "Successfully restored $count drink logs! 🎉", Toast.LENGTH_LONG).show()
                                    showBackupManagementDialog = false
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    fileToRestore = null
                                }
                            }
                        ) {
                            Text("Yes, Restore")
                        }
                    }
                }
            )
        }
    }
}

// Helper composable for grouping setting blocks cleanly
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.widthIn(max = 160.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Cup icon mapping helper
fun getCupIconRepresentation(iconName: String): ImageVector {
    return when (iconName) {
        "glass" -> Icons.Default.LocalDrink
        "bottle" -> Icons.Default.WaterDrop
        "steel" -> Icons.Default.SportsBar
        "tumbler" -> Icons.Default.WineBar
        "mug" -> Icons.Default.Coffee
        "drop" -> Icons.Default.Opacity
        "tea" -> Icons.Default.FreeBreakfast
        else -> Icons.Default.LocalDrink
    }
}

// JSON Backup Exporter
suspend fun exportBackupToJson(viewModel: HydrationViewModel): String {
    val entries = viewModel.uiState.value.allEntries
    val cups = viewModel.uiState.value.customCups

    val root = JSONObject()
    root.put("version", 4)
    root.put("type", "dailyhydra_full_backup")

    // Entries
    val entriesArr = JSONArray()
    entries.forEach { entry ->
        val obj = JSONObject()
        obj.put("id", entry.id)
        obj.put("amountMl", entry.amountMl)
        obj.put("timestamp", entry.timestamp)
        entriesArr.put(obj)
    }
    root.put("entries", entriesArr)

    // Cups
    val cupsArr = JSONArray()
    cups.forEach { cup ->
        val obj = JSONObject()
        obj.put("id", cup.id)
        obj.put("name", cup.name)
        obj.put("amountMl", cup.amountMl)
        obj.put("iconName", cup.iconName)
        cupsArr.put(obj)
    }
    root.put("cups", cupsArr)

    return root.toString(4)
}

// JSON Backup Importer
fun importBackupFromJson(jsonString: String, viewModel: HydrationViewModel): Int {
    val trimmed = jsonString.trim()
    if (trimmed.isEmpty()) {
        throw IllegalArgumentException("The backup file is completely empty.")
    }
    
    // Attempt standard parsing to check for JSON syntax errors
    val rootObj: JSONObject?
    val rootArr: JSONArray?
    try {
        if (trimmed.startsWith("[")) {
            rootArr = JSONArray(trimmed)
            rootObj = null
        } else if (trimmed.startsWith("{")) {
            rootObj = JSONObject(trimmed)
            rootArr = null
        } else {
            throw IllegalArgumentException("Invalid file format. Backups must be valid JSON objects or arrays.")
        }
    } catch (e: Exception) {
        throw IllegalArgumentException("Malformed JSON syntax: ${e.localizedMessage}. Cannot restore corrupted files.")
    }

    if (rootArr != null) {
        // Validate array items
        for (i in 0 until rootArr.length()) {
            val obj = try { rootArr.getJSONObject(i) } catch (e: Exception) {
                throw IllegalArgumentException("Item at index $i is not a valid JSON Object.")
            }
            if (!obj.has("amountMl") || !obj.has("timestamp")) {
                throw IllegalArgumentException("Entry at index $i is missing required fields (amountMl or timestamp).")
            }
            val amount = try { obj.getInt("amountMl") } catch(e: Exception) {
                throw IllegalArgumentException("Invalid 'amountMl' at index $i. Must be a valid integer.")
            }
            val timestamp = try { obj.getLong("timestamp") } catch(e: Exception) {
                throw IllegalArgumentException("Invalid 'timestamp' at index $i. Must be a valid integer.")
            }
            if (amount < 0 || amount > 10000) {
                throw IllegalArgumentException("Intake volume $amount ml is out of valid bounds (0 to 10L).")
            }
            if (timestamp <= 0 || timestamp > 4102444800000L) { // Year 2100 threshold
                throw IllegalArgumentException("Timestamp $timestamp is corrupt or out of future bounds.")
            }
        }
        
        // Validation succeeded, let's restore
        for (i in 0 until rootArr.length()) {
            val obj = rootArr.getJSONObject(i)
            val amount = obj.getInt("amountMl")
            val timestamp = obj.getLong("timestamp")
            viewModel.logWater(amount, isQuickAdd = false, timestamp = timestamp)
        }
        return rootArr.length()
    } else if (rootObj != null) {
        // Validate object features
        if (rootObj.has("type") && rootObj.getString("type") != "dailyhydra_full_backup") {
            throw IllegalArgumentException("Unsupported backup type label: '${rootObj.optString("type")}'.")
        }

        // Parse custom cups with full validation
        val cupsList = mutableListOf<CustomCup>()
        if (rootObj.has("cups")) {
            val cupsArr = try { rootObj.getJSONArray("cups") } catch(e: Exception) {
                throw IllegalArgumentException("Cups field must be a valid list.")
            }
            for (i in 0 until cupsArr.length()) {
                val obj = try { cupsArr.getJSONObject(i) } catch(e: Exception) {
                    throw IllegalArgumentException("Cup at index $i is not a valid JSON Object.")
                }
                val name = try { obj.getString("name") } catch(e: Exception) {
                    throw IllegalArgumentException("Cup at index $i is missing required text field 'name'.")
                }
                val amountMl = try { obj.getInt("amountMl") } catch(e: Exception) {
                    throw IllegalArgumentException("Cup at index $i is missing or has invalid 'amountMl'.")
                }
                val iconName = obj.optString("iconName", "glass")
                if (name.trim().isEmpty() || name.length > 50) {
                    throw IllegalArgumentException("Cup name must be between 1 and 50 characters.")
                }
                if (amountMl < 1 || amountMl > 5000) {
                    throw IllegalArgumentException("Cup capacity $amountMl ml is out of bounds (1ml to 5L).")
                }
                cupsList.add(
                    CustomCup(
                        id = obj.optInt("id", 0),
                        name = name,
                        amountMl = amountMl,
                        iconName = iconName
                    )
                )
            }
        }

        // Parse entries with full validation
        val entriesList = mutableListOf<IntakeEntry>()
        if (rootObj.has("entries")) {
            val entriesArr = try { rootObj.getJSONArray("entries") } catch(e: Exception) {
                throw IllegalArgumentException("Entries field must be a valid list.")
            }
            for (i in 0 until entriesArr.length()) {
                val obj = try { entriesArr.getJSONObject(i) } catch(e: Exception) {
                    throw IllegalArgumentException("Entry at index $i is not a valid JSON Object.")
                }
                val amountMl = try { obj.getInt("amountMl") } catch(e: Exception) {
                    throw IllegalArgumentException("Entry at index $i is missing or has invalid 'amountMl'.")
                }
                val timestamp = try { obj.getLong("timestamp") } catch(e: Exception) {
                    throw IllegalArgumentException("Entry at index $i is missing or has invalid 'timestamp'.")
                }
                if (amountMl < 0 || amountMl > 10000) {
                    throw IllegalArgumentException("Logged fluid amount $amountMl ml exceeds safe bounds.")
                }
                if (timestamp <= 0 || timestamp > 4102444800000L) {
                    throw IllegalArgumentException("Future or corrupt timestamp ($timestamp) detected in entries.")
                }
                entriesList.add(
                    IntakeEntry(
                        id = obj.optInt("id", 0),
                        amountMl = amountMl,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Restore everything cleanly into database
        viewModel.restoreBackupRaw(
            entries = entriesList,
            cups = cupsList
        )
        return entriesList.size
    } else {
        throw IllegalArgumentException("Corrupt backup. No valid JSON Root detected.")
    }
}

// CSV Backup Exporter
fun exportBackupToCsv(entries: List<IntakeEntry>): String {
    val sb = java.lang.StringBuilder()
    sb.append("ID,Amount (ml),Timestamp,Readable Local Date\n")
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    entries.forEach { entry ->
        sb.append("${entry.id},${entry.amountMl},${entry.timestamp},${sdf.format(Date(entry.timestamp))}\n")
    }
    return sb.toString()
}

// Placeholder for Custom Icons used in settings options
private val Icons.Outlined.Target: ImageVector
    get() = Icons.Default.LocalDrink // Fallback or standard visual

@Composable
fun TimeSelectionDialog(
    title: String,
    initialTime: String, // format e.g., "08:00 AM" or "10:00 PM"
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // Let's parse initialTime.
    var isPm = initialTime.uppercase().contains("PM")
    val rawTimeStr = initialTime.replace("AM", "", ignoreCase = true).replace("PM", "", ignoreCase = true).trim()
    val parts = rawTimeStr.split(":")
    var hour by remember {
        mutableStateOf(
            parts.firstOrNull()?.toIntOrNull()?.coerceIn(1, 12) ?: 8
        )
    }
    var minute by remember {
        mutableStateOf(
            parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        )
    }
    var isAmPeriod by remember {
        mutableStateOf(!isPm)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { hour = if (hour == 12) 1 else hour + 1 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropUp, contentDescription = "Increase Hour", modifier = Modifier.size(28.dp))
                        }
                        Text(
                            text = String.format("%02d", hour),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { hour = if (hour == 1) 12 else hour - 1 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Decrease Hour", modifier = Modifier.size(28.dp))
                        }
                    }

                    // Colon
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Minute Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { minute = (minute + 5) % 60 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropUp, contentDescription = "Increase Minute", modifier = Modifier.size(28.dp))
                        }
                        Text(
                            text = String.format("%02d", minute),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { minute = (minute - 5 + 60) % 60 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Decrease Minute", modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // AM/PM Column Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { isAmPeriod = !isAmPeriod }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAmPeriod) "AM" else "PM",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = "Quick Select Active Hours:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Start)
                )

                // Common hours grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val activePresets = listOf(
                        Triple(8, 0, true),   // 8:00 AM
                        Triple(9, 0, true),   // 9:00 AM
                        Triple(10, 0, true),  // 10:00 AM
                        Triple(9, 0, false),  // 9:00 PM
                        Triple(10, 0, false)  // 10:00 PM
                    )
                    activePresets.forEach { (h, m, am) ->
                        val isCurrentMatch = hour == h && minute == m && isAmPeriod == am
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrentMatch) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    hour = h
                                    minute = m
                                    isAmPeriod = am
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%d %s", h, if (am) "AM" else "PM"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isCurrentMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        val formatted = String.format("%02d:%02d %s", hour, minute, if (isAmPeriod) "AM" else "PM")
                        onSave(formatted)
                        onDismiss()
                    }
                ) {
                    Text("Select Time")
                }
            }
        }
    )
}
