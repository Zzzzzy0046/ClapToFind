package com.claptofind.phone.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object LanguageSelect : Screen("language_select")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object SoundSettings : Screen("sound_settings")
    data object FlashlightSettings : Screen("flashlight_settings")
    data object VibrateSettings : Screen("vibrate_settings")
    data object Settings : Screen("settings")
    data object HowToUse : Screen("how_to_use")
    data object Feedback : Screen("feedback")
    data object LanguageSettings : Screen("language_settings")
    data object RatingDialog : Screen("rating_dialog")
    data object EnableSuccess : Screen("enable_success")
    data object DisableSuccess : Screen("disable_success")
}
