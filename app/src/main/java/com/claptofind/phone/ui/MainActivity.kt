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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ClapToFindApp

        // Determine start destination
        val hasCompletedOnboarding = runBlocking {
            app.prefsManager.hasCompletedOnboarding.first()
        }
        val hasSelectedLanguage = runBlocking {
            app.prefsManager.hasSelectedLanguage.first()
        }

        setContent {
            ClapToFindTheme {
                val navController = rememberNavController()

                val startDestination = when {
                    !hasCompletedOnboarding -> "splash"
                    !hasSelectedLanguage -> "language_select"
                    else -> "home"
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
        // Handle widget toggle or notification click
        when (intent.action) {
            "TOGGLE_DETECTION" -> {
                // Handled by DetectionService
            }
        }
    }
}
