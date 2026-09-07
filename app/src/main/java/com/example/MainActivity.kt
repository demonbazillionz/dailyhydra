package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.HydrationDatabase
import com.example.data.HydrationRepository
import com.example.ui.HomeScreen
import com.example.ui.HistoryScreen
import com.example.ui.SettingsScreen
import com.example.ui.OnboardingScreen
import com.example.ui.HydrationViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.LocalWindowSizeClass
import com.example.ui.WindowSizeClass
import com.example.ui.rememberWindowSizeClass
import com.example.ui.GlassmorphicSideNav
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.widget.HydrationWidgetProvider
import com.example.scheduler.NotificationHelper
import com.example.scheduler.DailyHydraScheduler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request highest refresh rate (120Hz+ displays) for super smooth animations
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.attributes.preferredRefreshRate = 120f
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                val modes = display?.supportedModes
                val maxRateMode = modes?.maxByOrNull { it.refreshRate }
                if (maxRateMode != null && maxRateMode.refreshRate >= 60f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxRateMode.modeId
                    window.attributes = params
                }
            }
        } catch (_: Exception) {}

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val database = remember { HydrationDatabase.getDatabase(applicationContext) }
            val repository = remember { HydrationRepository(database.hydrationDao()) }
            
            val hydrationViewModel: HydrationViewModel = viewModel(
                factory = HydrationViewModel.Factory(repository)
            )

            // Setup notification channels and schedule/verify reminders on app launch
            LaunchedEffect(Unit) {
                NotificationHelper.createNotificationChannel(applicationContext)
                DailyHydraScheduler.scheduleNextReminder(applicationContext)
            }

            // Dynamic Widget Update Hook
            LaunchedEffect(hydrationViewModel) {
                hydrationViewModel.uiState
                    .map { Pair(it.todayEntries, it.dailyGoalMls) }
                    .distinctUntilChanged()
                    .collect {
                        HydrationWidgetProvider.triggerWidgetUpdate(applicationContext)
                    }
            }

            val currentTheme by hydrationViewModel.appTheme.collectAsStateWithLifecycle()
            val isDynamicColor by hydrationViewModel.dynamicColorsEnabled.collectAsStateWithLifecycle()
            val amoledDarkMode by hydrationViewModel.amoledDarkMode.collectAsStateWithLifecycle()
            val animationsEnabled by hydrationViewModel.animationsEnabled.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = currentTheme,
                amoledDarkMode = amoledDarkMode,
                dynamicColor = isDynamicColor,
                animationsEnabled = animationsEnabled
            ) {
                val isOnboarded by hydrationViewModel.isOnboarded.collectAsStateWithLifecycle()
                var currentTab by remember { mutableStateOf("home") }

                LaunchedEffect(hydrationViewModel) {
                    hydrationViewModel.navigateToHomeAndShowAdd.collect {
                        currentTab = "home"
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isOnboarded == null) {
                        Surface(modifier = Modifier.fillMaxSize()) {}
                    } else if (isOnboarded == false) {
                        OnboardingScreen(
                            viewModel = hydrationViewModel,
                            onFinished = {
                                hydrationViewModel.completeOnboarding()
                            }
                        )
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val sizeClass = rememberWindowSizeClass(maxWidth)
                            
                            CompositionLocalProvider(LocalWindowSizeClass provides sizeClass) {
                                val showSideNav = sizeClass != WindowSizeClass.COMPACT && sizeClass != WindowSizeClass.MEDIUM
                                
                                if (showSideNav) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        GlassmorphicSideNav(
                                            currentTab = currentTab,
                                            onTabSelected = { currentTab = it },
                                            currentTheme = currentTheme,
                                            sizeClass = sizeClass
                                        )
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            when (currentTab) {
                                                "home" -> HomeScreen(
                                                    viewModel = hydrationViewModel,
                                                    onTabSelected = { currentTab = it }
                                                )
                                                "history" -> HistoryScreen(
                                                    viewModel = hydrationViewModel,
                                                    onTabSelected = { currentTab = it }
                                                )
                                                else -> SettingsScreen(
                                                    viewModel = hydrationViewModel,
                                                    onTabSelected = { currentTab = it }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    when (currentTab) {
                                        "home" -> HomeScreen(
                                            viewModel = hydrationViewModel,
                                            onTabSelected = { currentTab = it }
                                        )
                                        "history" -> HistoryScreen(
                                            viewModel = hydrationViewModel,
                                            onTabSelected = { currentTab = it }
                                        )
                                        else -> SettingsScreen(
                                            viewModel = hydrationViewModel,
                                            onTabSelected = { currentTab = it }
                                        )
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
