package com.claptofind.phone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clap_to_find_prefs")

class PreferencesManager(private val context: Context) {

    // --- Detection State ---
    val isDetectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DETECTION_ENABLED] ?: false
    }

    suspend fun setDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DETECTION_ENABLED] = enabled }
    }

    // --- Language ---
    val selectedLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_LANGUAGE] ?: "System"
    }

    suspend fun setSelectedLanguage(language: String) {
        context.dataStore.edit { prefs -> prefs[SELECTED_LANGUAGE] = language }
    }

    val hasSelectedLanguage: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_SELECTED_LANGUAGE] ?: false
    }

    suspend fun setHasSelectedLanguage(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[HAS_SELECTED_LANGUAGE] = value }
    }

    // --- Onboarding ---
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_COMPLETED_ONBOARDING] ?: false
    }

    suspend fun setHasCompletedOnboarding(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[HAS_COMPLETED_ONBOARDING] = value }
    }

    // --- Sound Settings ---
    val soundVolume: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SOUND_VOLUME] ?: 80
    }

    suspend fun setSoundVolume(volume: Int) {
        context.dataStore.edit { prefs -> prefs[SOUND_VOLUME] = volume.coerceIn(0, 200) }
    }

    val soundDuration: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SOUND_DURATION] ?: 15
    }

    suspend fun setSoundDuration(seconds: Int) {
        context.dataStore.edit { prefs -> prefs[SOUND_DURATION] = seconds }
    }

    val selectedSound: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_SOUND] ?: "Air Horn"
    }

    suspend fun setSelectedSound(sound: String) {
        context.dataStore.edit { prefs -> prefs[SELECTED_SOUND] = sound }
    }

    val isMuted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_MUTED] ?: false
    }

    suspend fun setIsMuted(muted: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_MUTED] = muted }
    }

    // --- Flashlight Settings ---
    val flashlightEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FLASHLIGHT_ENABLED] ?: true
    }

    suspend fun setFlashlightEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[FLASHLIGHT_ENABLED] = enabled }
    }

    val flashlightMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[FLASHLIGHT_MODE] ?: "Quick Blink"
    }

    suspend fun setFlashlightMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[FLASHLIGHT_MODE] = mode }
    }

    // --- Vibrate Settings ---
    val vibrateEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[VIBRATE_ENABLED] ?: true
    }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[VIBRATE_ENABLED] = enabled }
    }

    val vibrateMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VIBRATE_MODE] ?: "High Frequency"
    }

    suspend fun setVibrateMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[VIBRATE_MODE] = mode }
    }

    // --- Sound Sensitivity ---
    val soundSensitivity: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SOUND_SENSITIVITY] ?: "High"
    }

    suspend fun setSoundSensitivity(sensitivity: String) {
        context.dataStore.edit { prefs -> prefs[SOUND_SENSITIVITY] = sensitivity }
    }

    // --- Pause Detection While Using Phone ---
    val pauseWhileUsingPhone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PAUSE_WHILE_USING] ?: false
    }

    suspend fun setPauseWhileUsingPhone(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[PAUSE_WHILE_USING] = value }
    }

    // --- Schedule Deactivate ---
    val scheduleEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SCHEDULE_ENABLED] ?: false
    }

    suspend fun setScheduleEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[SCHEDULE_ENABLED] = enabled }
    }

    val scheduleStartHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SCHEDULE_START_HOUR] ?: 0
    }

    suspend fun setScheduleStartHour(hour: Int) {
        context.dataStore.edit { prefs -> prefs[SCHEDULE_START_HOUR] = hour }
    }

    val scheduleStartMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SCHEDULE_START_MINUTE] ?: 0
    }

    suspend fun setScheduleStartMinute(minute: Int) {
        context.dataStore.edit { prefs -> prefs[SCHEDULE_START_MINUTE] = minute }
    }

    val scheduleEndHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SCHEDULE_END_HOUR] ?: 6
    }

    suspend fun setScheduleEndHour(hour: Int) {
        context.dataStore.edit { prefs -> prefs[SCHEDULE_END_HOUR] = hour }
    }

    val scheduleEndMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SCHEDULE_END_MINUTE] ?: 0
    }

    suspend fun setScheduleEndMinute(minute: Int) {
        context.dataStore.edit { prefs -> prefs[SCHEDULE_END_MINUTE] = minute }
    }

    // --- Whistle Detection ---
    val whistleDetectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WHISTLE_DETECTION] ?: false
    }

    suspend fun setWhistleDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[WHISTLE_DETECTION] = enabled }
    }

    // --- First Time Use (for max sensitivity on first detection) ---
    val hasEverDetected: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_EVER_DETECTED] ?: false
    }

    suspend fun setHasEverDetected(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[HAS_EVER_DETECTED] = value }
    }

    // --- Rating Shown ---
    val ratingAskShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[RATING_ASK_SHOWN] ?: false
    }

    suspend fun setRatingAskShown(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[RATING_ASK_SHOWN] = value }
    }

    val ratingGiven5Stars: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[RATING_GIVEN_5] ?: false
    }

    suspend fun setRatingGiven5Stars(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[RATING_GIVEN_5] = value }
    }

    // --- App Launch Count ---
    val appLaunchCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[APP_LAUNCH_COUNT] ?: 0
    }

    suspend fun incrementAppLaunchCount() {
        context.dataStore.edit { prefs ->
            prefs[APP_LAUNCH_COUNT] = (prefs[APP_LAUNCH_COUNT] ?: 0) + 1
        }
    }

    // --- Last Rating Date ---
    val lastRatingPromptDate: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_RATING_DATE] ?: ""
    }

    suspend fun setLastRatingPromptDate(date: String) {
        context.dataStore.edit { prefs -> prefs[LAST_RATING_DATE] = date }
    }

    // --- Settings Tip Shown ---
    val settingsTipShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SETTINGS_TIP_SHOWN] ?: false
    }

    suspend fun setSettingsTipShown(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SETTINGS_TIP_SHOWN] = value }
    }

    val hasVisitedSettings: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_VISITED_SETTINGS] ?: false
    }

    suspend fun setHasVisitedSettings(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[HAS_VISITED_SETTINGS] = value }
    }

    // --- Feedback Draft ---
    val feedbackDraft: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[FEEDBACK_DRAFT] ?: ""
    }

    suspend fun setFeedbackDraft(text: String) {
        context.dataStore.edit { prefs -> prefs[FEEDBACK_DRAFT] = text }
    }

    // --- Subscription Status (for feature gating, not actual payments) ---
    val isProUser: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PRO_USER] ?: false
    }

    suspend fun setIsProUser(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_PRO_USER] = value }
    }

    companion object {
        // Detection
        private val DETECTION_ENABLED = booleanPreferencesKey("detection_enabled")
        // Language
        private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        private val HAS_SELECTED_LANGUAGE = booleanPreferencesKey("has_selected_language")
        // Onboarding
        private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        // Sound
        private val SOUND_VOLUME = intPreferencesKey("sound_volume")
        private val SOUND_DURATION = intPreferencesKey("sound_duration")
        private val SELECTED_SOUND = stringPreferencesKey("selected_sound")
        private val IS_MUTED = booleanPreferencesKey("is_muted")
        // Flashlight
        private val FLASHLIGHT_ENABLED = booleanPreferencesKey("flashlight_enabled")
        private val FLASHLIGHT_MODE = stringPreferencesKey("flashlight_mode")
        // Vibrate
        private val VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
        private val VIBRATE_MODE = stringPreferencesKey("vibrate_mode")
        // Sensitivity
        private val SOUND_SENSITIVITY = stringPreferencesKey("sound_sensitivity")
        // Pause
        private val PAUSE_WHILE_USING = booleanPreferencesKey("pause_while_using")
        // Schedule
        private val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        private val SCHEDULE_START_HOUR = intPreferencesKey("schedule_start_hour")
        private val SCHEDULE_START_MINUTE = intPreferencesKey("schedule_start_minute")
        private val SCHEDULE_END_HOUR = intPreferencesKey("schedule_end_hour")
        private val SCHEDULE_END_MINUTE = intPreferencesKey("schedule_end_minute")
        // Whistle
        private val WHISTLE_DETECTION = booleanPreferencesKey("whistle_detection")
        // Detection tracking
        private val HAS_EVER_DETECTED = booleanPreferencesKey("has_ever_detected")
        // Rating
        private val RATING_ASK_SHOWN = booleanPreferencesKey("rating_ask_shown")
        private val RATING_GIVEN_5 = booleanPreferencesKey("rating_given_5")
        private val APP_LAUNCH_COUNT = intPreferencesKey("app_launch_count")
        private val LAST_RATING_DATE = stringPreferencesKey("last_rating_date")
        // Settings tip
        private val SETTINGS_TIP_SHOWN = booleanPreferencesKey("settings_tip_shown")
        private val HAS_VISITED_SETTINGS = booleanPreferencesKey("has_visited_settings")
        // Feedback
        private val FEEDBACK_DRAFT = stringPreferencesKey("feedback_draft")
        // Pro
        private val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
    }
}
