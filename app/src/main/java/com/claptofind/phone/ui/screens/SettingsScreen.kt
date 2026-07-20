package com.claptofind.phone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.data.SoundSensitivity
import com.claptofind.phone.service.DetectionService
import com.claptofind.phone.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToHowToUse: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToRating: () -> Unit
) {
    val app = ClapToFindApp.instance
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val isPro by app.prefsManager.isProUser.collectAsState(initial = false)
    val pauseWhileUsing by app.prefsManager.pauseWhileUsingPhone.collectAsState(initial = false)
    val sensitivity by app.prefsManager.soundSensitivity.collectAsState(initial = "High")
    val scheduleEnabled by app.prefsManager.scheduleEnabled.collectAsState(initial = false)
    val scheduleStartH by app.prefsManager.scheduleStartHour.collectAsState(initial = 0)
    val scheduleStartM by app.prefsManager.scheduleStartMinute.collectAsState(initial = 0)
    val scheduleEndH by app.prefsManager.scheduleEndHour.collectAsState(initial = 6)
    val scheduleEndM by app.prefsManager.scheduleEndMinute.collectAsState(initial = 0)
    val whistleEnabled by app.prefsManager.whistleDetectionEnabled.collectAsState(initial = false)
    val isDetectionEnabled by app.prefsManager.isDetectionEnabled.collectAsState(initial = false)

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showWhistleConfirmDialog by remember { mutableStateOf(false) }

    // Mark visited for tip
    LaunchedEffect(Unit) {
        scope.launch {
            app.prefsManager.setHasVisitedSettings(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Subscription status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPro) PremiumGold.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* subscription - skipped */ }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = if (isPro) PremiumGold else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isPro) "You're on Pro" else "Upgrade to Premium",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isPro) "All features unlocked" else "Unlock all premium features",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }

            // --- Settings Items ---

            SettingsGroup("General") {
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = "Select language",
                    subtitle = null,
                    onClick = onNavigateToLanguage
                )
                SettingsItem(
                    icon = Icons.Filled.Star,
                    title = "Rate us",
                    subtitle = null,
                    onClick = onNavigateToRating
                )
                SettingsItem(
                    icon = Icons.Filled.Feedback,
                    title = "Feedback",
                    subtitle = null,
                    onClick = onNavigateToFeedback
                )
                SettingsItem(
                    icon = Icons.Filled.Share,
                    title = "Share",
                    subtitle = null,
                    onClick = {
                        val intent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out Clap to Find Phone! https://play.google.com/store/apps/details?id=${context.packageName}")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                    }
                )
                SettingsItem(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy policy",
                    subtitle = null,
                    onClick = { /* Open privacy policy URL */ }
                )
                SettingsItem(
                    icon = Icons.Filled.HelpOutline,
                    title = "How to use?",
                    subtitle = null,
                    onClick = onNavigateToHowToUse
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsGroup("Detection") {
                // Pause while using phone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pause detection while using phone", fontWeight = FontWeight.Medium)
                        Text(
                            "Detection pauses when screen is on and unlocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = pauseWhileUsing,
                        onCheckedChange = {
                            scope.launch { app.prefsManager.setPauseWhileUsingPhone(it) }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                // Sound sensitivity
                SettingsItem(
                    icon = Icons.Filled.Hearing,
                    title = "Sound Sensitivity",
                    subtitle = sensitivity,
                    onClick = {
                        // Cycle through sensitivities
                        val next = when (SoundSensitivity.fromDisplayName(sensitivity)) {
                            SoundSensitivity.VERY_HIGH -> SoundSensitivity.HIGH
                            SoundSensitivity.HIGH -> SoundSensitivity.MEDIUM
                            SoundSensitivity.MEDIUM -> SoundSensitivity.VERY_HIGH
                        }
                        scope.launch { app.prefsManager.setSoundSensitivity(next.displayName) }
                    }
                )
                Text(
                    "Higher sound sensitivity lets your phone detect sound from farther away.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                // Whistle detection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!whistleEnabled && !isDetectionEnabled) {
                                showWhistleConfirmDialog = true
                            } else {
                                scope.launch { app.prefsManager.setWhistleDetectionEnabled(!whistleEnabled) }
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Whistle Detection", fontWeight = FontWeight.Medium)
                        Text(
                            "Also respond to whistling sounds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = whistleEnabled,
                        onCheckedChange = {
                            if (it && !isDetectionEnabled) {
                                showWhistleConfirmDialog = true
                            } else {
                                scope.launch { app.prefsManager.setWhistleDetectionEnabled(it) }
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                // Schedule deactivate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Schedule deactivate period", fontWeight = FontWeight.Medium)
                        if (scheduleEnabled) {
                            Text(
                                "${String.format("%02d:%02d", scheduleStartH, scheduleStartM)} – ${String.format("%02d:%02d", scheduleEndH, scheduleEndM)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = scheduleEnabled,
                        onCheckedChange = {
                            scope.launch { app.prefsManager.setScheduleEnabled(it) }
                        }
                    )
                }

                if (scheduleEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start: ${String.format("%02d:%02d", scheduleStartH, scheduleStartM)}")
                        }
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("End: ${String.format("%02d:%02d", scheduleEndH, scheduleEndM)}")
                        }
                    }
                }
            }
        }
    }

    // Time pickers
    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = scheduleStartH,
            initialMinute = scheduleStartM,
            onConfirm = { h, m ->
                scope.launch {
                    app.prefsManager.setScheduleStartHour(h)
                    app.prefsManager.setScheduleStartMinute(m)
                }
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = scheduleEndH,
            initialMinute = scheduleEndM,
            onConfirm = { h, m ->
                scope.launch {
                    app.prefsManager.setScheduleEndHour(h)
                    app.prefsManager.setScheduleEndMinute(m)
                }
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }

    // Whistle confirm dialog
    if (showWhistleConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWhistleConfirmDialog = false },
            icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
            title = { Text("Enable Find My Phone Now?") },
            text = { Text("Turn on Find My Phone to enable whistle and clap detection.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        app.prefsManager.setDetectionEnabled(true)
                        app.prefsManager.setWhistleDetectionEnabled(true)
                        DetectionService.start(context)
                    }
                    showWhistleConfirmDialog = false
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showWhistleConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour picker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { hour = (hour + 1) % 24 }) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Increase hour")
                    }
                    Text(
                        String.format("%02d", hour),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Decrease hour")
                    }
                }

                Text(":", style = MaterialTheme.typography.headlineLarge)

                // Minute picker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { minute = (minute + 1) % 60 }) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Increase minute")
                    }
                    Text(
                        String.format("%02d", minute),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    IconButton(onClick = { minute = (minute - 1 + 60) % 60 }) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Decrease minute")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
