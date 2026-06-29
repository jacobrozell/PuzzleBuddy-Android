package com.jacobrozell.puzzlebuddy

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.jacobrozell.puzzlebuddy.bootstrap.AppBootstrapper
import com.jacobrozell.puzzlebuddy.data.prefs.AppPreferencesStore
import com.jacobrozell.puzzlebuddy.data.prefs.OnboardingStore
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import com.jacobrozell.puzzlebuddy.ui.navigation.PuzzleBuddyNavHost
import com.jacobrozell.puzzlebuddy.ui.onboarding.OnboardingFlow
import com.jacobrozell.puzzlebuddy.ui.splash.SplashScreen
import com.jacobrozell.puzzlebuddy.ui.theme.PuzzleBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var onboardingStore: OnboardingStore
    @Inject lateinit var appPreferencesStore: AppPreferencesStore
    @Inject lateinit var appBootstrapper: AppBootstrapper
    @Inject lateinit var logger: AppLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { appBootstrapper.onLaunch() }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            var showOnboarding by remember { mutableStateOf(onboardingStore.shouldPresentOnLaunch) }
            val appearanceMode by appPreferencesStore.appearanceMode.collectAsStateWithLifecycle(initialValue = "system")
            val darkTheme = when (appearanceMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            DisposableEffect(onboardingStore) {
                val listener = { showOnboarding = true }
                onboardingStore.addReplayListener(listener)
                onDispose { onboardingStore.removeReplayListener(listener) }
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                delay(900)
                showSplash = false
            }

            PuzzleBuddyTheme(darkTheme = darkTheme) {
                when {
                    showSplash -> SplashScreen()
                    else -> {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            PuzzleBuddyNavHost(
                                onReplayOnboarding = { showOnboarding = true },
                            )
                        }
                        if (showOnboarding) {
                            OnboardingFlow(
                                onSkipped = {
                                    lifecycleScope.launch {
                                        logger.info(
                                            LogCategory.APP,
                                            eventName = "onboarding_skipped",
                                            message = "Onboarding skipped.",
                                            metadata = mapOf("page_index" to "0"),
                                        )
                                        onboardingStore.markComplete()
                                        showOnboarding = false
                                    }
                                },
                                onFinished = {
                                    lifecycleScope.launch {
                                        onboardingStore.markComplete()
                                        showOnboarding = false
                                        logger.info(
                                            LogCategory.APP,
                                            eventName = "onboarding_completed",
                                            message = "Onboarding finished.",
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
