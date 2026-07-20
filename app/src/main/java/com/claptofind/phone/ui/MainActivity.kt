package com.claptofind.phone.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.ui.navigation.AppNavGraph
import com.claptofind.phone.ui.theme.ClapToFindTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ClapToFindApp

        // Render UI immediately; determine start destination reactively
        setContent {
            ClapToFindTheme {
                val navController = rememberNavController()

                var startDestination by remember { mutableStateOf("splash") }

                LaunchedEffect(Unit) {
                    val hasOnboarding = withContext(Dispatchers.IO) {
                        app.prefsManager.hasCompletedOnboarding.first()
                    }
                    val hasLanguage = withContext(Dispatchers.IO) {
                        app.prefsManager.hasSelectedLanguage.first()
                    }
                    startDestination = when {
                        !hasOnboarding -> "splash"
                        !hasLanguage -> "language_select"
                        else -> "home"
                    }
                }

                AppNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }

        // Handle incoming intents (widget clicks, notification actions)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            "TOGGLE_DETECTION" -> {
                // Handled by DetectionService
            }
        }
    }
}
