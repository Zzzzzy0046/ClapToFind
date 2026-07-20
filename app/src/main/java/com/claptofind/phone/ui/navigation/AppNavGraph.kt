package com.claptofind.phone.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.claptofind.phone.ui.screens.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLanguage = {
                    navController.navigate(Screen.LanguageSelect.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LanguageSelect.route) {
            LanguageSelectScreen(
                onLanguageSelected = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.LanguageSelect.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            HowToUseScreen(
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSoundSettings = {
                    navController.navigate(Screen.SoundSettings.route)
                },
                onNavigateToFlashlightSettings = {
                    navController.navigate(Screen.FlashlightSettings.route)
                },
                onNavigateToVibrateSettings = {
                    navController.navigate(Screen.VibrateSettings.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToHowToUse = {
                    navController.navigate(Screen.HowToUse.route)
                }
            )
        }

        composable(Screen.SoundSettings.route) {
            SoundSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FlashlightSettings.route) {
            FlashlightSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VibrateSettings.route) {
            VibrateSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLanguage = {
                    navController.navigate(Screen.LanguageSettings.route)
                },
                onNavigateToHowToUse = {
                    navController.navigate(Screen.HowToUse.route)
                },
                onNavigateToFeedback = {
                    navController.navigate(Screen.Feedback.route)
                },
                onNavigateToRating = {
                    navController.navigate(Screen.RatingDialog.route)
                }
            )
        }

        composable(Screen.HowToUse.route) {
            HowToUseScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Feedback.route) {
            FeedbackScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LanguageSettings.route) {
            LanguageSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RatingDialog.route) {
            RatingScreen(
                onDismiss = { navController.popBackStack() },
                fromSettings = true
            )
        }

        composable(Screen.EnableSuccess.route) {
            EnableSuccessScreen(
                onBackToHome = {
                    navController.popBackStack(Screen.Home.route, false)
                }
            )
        }

        composable(Screen.DisableSuccess.route) {
            DisableSuccessScreen(
                onBackToHome = {
                    navController.popBackStack(Screen.Home.route, false)
                }
            )
        }
    }
}
