package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scheduler.DailyHydraScheduler
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: HydrationViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) } // 1 to 7 screens

    // Saved inputs in dynamic states
    var selectedGoalMl by remember { mutableStateOf(3000) }
    var selectedPresets by remember { mutableStateOf(setOf(250, 500, 750)) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "reminders activated! 🔔", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Reminders can be enabled later in settings.", Toast.LENGTH_LONG).show()
        }
    }

    fun playHaptic() {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            // Unchanging top progress indicators (7 steps)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (step in 1..7) {
                    val isActive = step == currentStep
                    val isCompleted = step < currentStep
                    val barColor = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else if (isCompleted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    }
                    val weight = if (isActive) 1.8f else 1.0f

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated step-by-step content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width / 2 } + fadeIn(animationSpec = tween(300)) togetherWith
                                    slideOutHorizontally { width -> -width / 2 } + fadeOut(animationSpec = tween(300))
                        } else {
                            slideInHorizontally { width -> -width / 2 } + fadeIn(animationSpec = tween(300)) togetherWith
                                    slideOutHorizontally { width -> width / 2 } + fadeOut(animationSpec = tween(300))
                        }
                    },
                    label = "onboarding_screen_transitions"
                ) { step ->
                    when (step) {
                        1 -> ScreenWelcome(onPlayHaptic = { playHaptic() })
                        2 -> ScreenGoalSetup(
                            selectedGoal = selectedGoalMl,
                            onGoalChanged = {
                                playHaptic()
                                selectedGoalMl = it
                            }
                        )
                        3 -> ScreenShortcuts(
                            selectedPresets = selectedPresets,
                            onTogglePreset = { preset ->
                                playHaptic()
                                selectedPresets = if (selectedPresets.contains(preset)) {
                                    if (selectedPresets.size > 1) selectedPresets - preset else selectedPresets
                                } else {
                                    selectedPresets + preset
                                }
                            }
                        )
                        4 -> ScreenNotifications(
                            onEnableNotification = {
                                playHaptic()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationPermissionGranted = true
                                    Toast.makeText(context, "reminders activated! 🔔", Toast.LENGTH_SHORT).show()
                                }
                            },
                            notificationGranted = notificationPermissionGranted
                        )
                        5 -> ScreenThemeSelection(
                            selectedTheme = currentTheme,
                            onThemeChanged = { theme ->
                                playHaptic()
                                viewModel.setAppTheme(theme)
                            }
                        )
                        6 -> ScreenPrivacy()
                        7 -> ScreenReady(onPlayHaptic = { playHaptic() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unchanging beautiful bottom action rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (hidden on first screen)
                if (currentStep > 1) {
                    TextButton(
                        onClick = {
                            playHaptic()
                            currentStep--
                        },
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Skip Option for optional layouts where allowed
                val allowSkip = currentStep in 3..5
                if (allowSkip) {
                    TextButton(
                        onClick = {
                            playHaptic()
                            currentStep++
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Large Premium Primary CTA Button
                Button(
                    onClick = {
                        playHaptic()
                        if (currentStep < 7) {
                            // On transitions, persist choices contextually to preserve responsive performance updates
                            if (currentStep == 2) {
                                viewModel.setDailyGoal(selectedGoalMl)
                                viewModel.setGoalUnit("ml")
                            } else if (currentStep == 3) {
                                val sorted = selectedPresets.toList().sorted()
                                viewModel.setQuickLoggingPresets(sorted)
                                viewModel.setDefaultQuickAddMls(sorted.firstOrNull() ?: 250)
                            } else if (currentStep == 4) {
                                viewModel.setRemindersEnabled(notificationPermissionGranted, context)
                                if (notificationPermissionGranted) {
                                    viewModel.setQuietHoursEnabled(false) // disable quiet hours by default in clean config
                                    viewModel.setSmartReminderInterval(120, context) // default 2 hours reminder frequency
                                }
                            }
                            currentStep++
                        } else {
                            // Final Screen 7 transition: lock onboarding state & save completion
                            viewModel.completeOnboarding()
                            DailyHydraScheduler.scheduleNextReminder(context)
                            onFinished()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (currentStep == 1) "Get Started" else if (currentStep == 7) "Start Hydrating" else "Next",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1 — WELCOME
// ==========================================
@Composable
fun ScreenWelcome(onPlayHaptic: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_breath")
    val scaleState = infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subtle_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = scaleState.value
                    scaleY = scaleState.value
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
            )
            Image(
                painter = painterResource(id = R.drawable.dailyhydra_logo),
                contentDescription = "dailyhydra logo",
                modifier = Modifier.size(110.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "dailyhydra",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Track Water. Build Habits. Stay Hydrated.",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )


    }
}

// ==========================================
// SCREEN 2 — DAILY GOAL SETUP
// ==========================================
@Composable
fun ScreenGoalSetup(
    selectedGoal: Int,
    onGoalChanged: (Int) -> Unit
) {
    val presets = listOf(2000, 2500, 3000, 3500)
    
    // Dynamic transition mapping a sleek filling bottle
    val progressFraction = (selectedGoal.toFloat() / 4000f).coerceIn(0.1f, 1.0f)
    val fillingState = animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bottle_fill_state"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose your water intake target",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose a preset or input your custom daily volume goal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.61f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Large high-performance water bottle simulator
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Wave fluid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleY = fillingState.value
                    }
                    .fillMaxHeight()
                    .graphicsLayer {
                        // Pivots scale directly from the bottom to mimic filling!
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.0f)
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF2979FF)
                            )
                        )
                    )
            )

            // Dynamic volume reading over glass
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$selectedGoal",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (fillingState.value > 0.45f) Color.White else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ml",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fillingState.value > 0.45f) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Preset Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { cap ->
                val isSelected = selectedGoal == cap
                val borderStroke = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }

                Surface(
                    onClick = { onGoalChanged(cap) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = borderStroke,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "${cap}ml",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom volume control
        Text(
            text = "CUSTOM GOAL",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            IconButton(
                onClick = { onGoalChanged((selectedGoal - 100).coerceAtLeast(1000)) },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease goal", tint = MaterialTheme.colorScheme.primary)
            }

            Text(
                text = "$selectedGoal ml",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { onGoalChanged((selectedGoal + 100).coerceAtMost(8000)) },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase goal", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ==========================================
// SCREEN 3 — QUICK ADD SHORTCUTS
// ==========================================
@Composable
fun ScreenShortcuts(
    selectedPresets: Set<Int>,
    onTogglePreset: (Int) -> Unit
) {
    val options = listOf(100, 250, 500, 750, 1000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quick Add Shortcuts",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap to choose your preferred shortcut cups to display on your dashboard.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        options.forEach { vol ->
            val isEnabled = selectedPresets.contains(vol)
            val borderStroke = if (isEnabled) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }

            Card(
                onClick = { onTogglePreset(vol) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                vol <= 150 -> Icons.Default.LocalCafe
                                vol <= 300 -> Icons.Default.LocalBar
                                vol <= 600 -> Icons.Default.WaterDrop
                                else -> Icons.Default.Opacity
                            },
                            contentDescription = null,
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "$vol ml",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (vol == 250) "Standard cup" else if (vol == 500) "Sport bottle" else "Custom cup volume",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEnabled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4 — NOTIFICATIONS
// ==========================================
@Composable
fun ScreenNotifications(
    onEnableNotification: () -> Unit,
    notificationGranted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hydration Reminders",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Stay on track and build smart water habits.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feature checklist items
        listOf(
            Triple(Icons.Default.Timer, "Hydration Reminders", "Get gentle reminders to drink throughout your active day."),
            Triple(Icons.Default.Celebration, "Goal Completion Alerts", "Celebrate when you successfully reach your daily target!"),
            Triple(Icons.Default.MilitaryTech, "Achievement Notifications", "Unlock beautiful milestones as you strengthen your streak.")
        ).forEach { (icon, title, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (notificationGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Notifications are active and configured!",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Button(
                onClick = onEnableNotification,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enable Reminders")
            }
        }
    }
}

// ==========================================
// SCREEN 5 — THEME SELECTION
// ==========================================
@Composable
fun ScreenThemeSelection(
    selectedTheme: String,
    onThemeChanged: (String) -> Unit
) {
    val themes = listOf(
        Triple("System", Icons.Default.SettingsSuggest, "Adapts to your operating system styling"),
        Triple("Light", Icons.Default.WbSunny, "Crisp light dynamic layout"),
        Triple("Dark", Icons.Default.NightsStay, "Midnight deep high-contrast interface"),
        Triple("Light Water", Icons.Default.Opacity, "Dynamic pure water design theme"),
        Triple("Dark Water", Icons.Default.WaterDrop, "Beautiful night navigation layout")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Application Theme",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Switches are applied live instantly. Explore what works best for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        themes.forEach { (themeName, icon, desc) ->
            val isCurrent = selectedTheme == themeName
            val borderSpec = if (isCurrent) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }

            Card(
                onClick = { onThemeChanged(themeName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .graphicsLayer {
                        // Dynamic interactive hover feedback scale
                        scaleX = if (isCurrent) 1.01f else 1.0f
                        scaleY = if (isCurrent) 1.01f else 1.0f
                    },
                shape = RoundedCornerShape(16.dp),
                border = borderSpec,
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (themeName == "Light Water") "Water" else themeName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active theme",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 6 — PRIVACY ADVOCACY
// ==========================================
@Composable
fun ScreenPrivacy() {
    val privacyPoints = listOf(
        Pair(Icons.Default.SignalWifiOff, "Offline First") to "No unnecessary internet connectivity profiles. Works entirely without server integrations.",
        Pair(Icons.Default.AccountCircle, "No Account Required") to "No logging credentials, emails, passwords, or profile requirements.",
        Pair(Icons.Default.Security, "No Tracking") to "100% analytics-free. No telemetry trackers or third-party SDKs.",
        Pair(Icons.Default.Storage, "Local Storage Only") to "All logs, daily accomplishments, and transactions are stored inside your device's secure SQL container.",
        Pair(Icons.Default.Devices, "Your Data Stays On Device") to "Pure uncompromised privacy and safety. You are fully in control."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Privacy Mandate",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "dailyhydra has been crafted around state-of-the-art privacy principles.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        privacyPoints.forEach { (header, bodyText) ->
            val (icon, title) = header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = bodyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 7 — READY CELEBRATION
// ==========================================
@Composable
fun ScreenReady(onPlayHaptic: () -> Unit) {
    val wavesTransition = rememberInfiniteTransition(label = "fluid_waves")
    val waveHeightState = wavesTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ready_wave_elevation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                .padding(24.dp)
                .graphicsLayer {
                    translationY = waveHeightState.value
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(68.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "You're Ready",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Let's start your custom hydration journey. Tap button below to reach the dashboard.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
