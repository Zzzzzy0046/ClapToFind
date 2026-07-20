package com.claptofind.phone.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.service.DetectionService
import com.claptofind.phone.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToSoundSettings: () -> Unit,
    onNavigateToFlashlightSettings: () -> Unit,
    onNavigateToVibrateSettings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHowToUse: () -> Unit,
    onEnableSuccess: () -> Unit,
    onDisableSuccess: () -> Unit
) {
    val context = LocalContext.current
    val app = ClapToFindApp.instance
    val scope = rememberCoroutineScope()

    val isDetectionEnabled by app.prefsManager.isDetectionEnabled.collectAsState(initial = false)
    val scheduleEnabled by app.prefsManager.scheduleEnabled.collectAsState(initial = false)
    val scheduleEndHour by app.prefsManager.scheduleEndHour.collectAsState(initial = 6)
    val scheduleEndMinute by app.prefsManager.scheduleEndMinute.collectAsState(initial = 0)
    val whistleEnabled by app.prefsManager.whistleDetectionEnabled.collectAsState(initial = false)
    val hasVisitedSettings by app.prefsManager.hasVisitedSettings.collectAsState(initial = false)
    val settingsTipShown by app.prefsManager.settingsTipShown.collectAsState(initial = false)

    // Permission dialogs
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    var showFloatingPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showWidgetTutorial by remember { mutableStateOf(false) }

    // State for schedule pause
    val isInSchedulePeriod = remember(scheduleEnabled) {
        if (!scheduleEnabled) false
        else {
            val cal = java.util.Calendar.getInstance()
            val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val endMin = scheduleEndHour * 60 + scheduleEndMinute
            nowMin < endMin // simplified; real logic in DetectionService
        }
    }

    // Show settings tip bubble on 2nd visit
    var showSettingsTip by remember { mutableStateOf(false) }
    LaunchedEffect(hasVisitedSettings, settingsTipShown) {
        if (!hasVisitedSettings && !settingsTipShown) {
            showSettingsTip = true
        }
    }

    // Mic permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                app.prefsManager.setDetectionEnabled(true)
                DetectionService.start(context)
                onEnableSuccess()
            }
        }
    }

    // Check mic
    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    // Check overlay
    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    // Check notification
    fun hasNotificationPermission(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true

    // Toggle function
    fun toggleDetection(
        app: ClapToFindApp,
        targetEnabled: Boolean,
        onSuccess: () -> Unit,
        onDisable: () -> Unit
    ) {
        scope.launch {
            if (targetEnabled) {
                // Need mic first
                if (!hasMicPermission()) {
                    showMicPermissionDialog = true
                    return@launch
                }
                app.prefsManager.setDetectionEnabled(true)
                DetectionService.start(context)
                onSuccess()
            } else {
                app.prefsManager.setDetectionEnabled(false)
                DetectionService.stop(context)
                onDisable()
            }
        }
    }

    // Determine status text
    val statusText = when {
        !isDetectionEnabled -> "Click here to activate"
        isInSchedulePeriod -> "Enabled – Temporarily paused until ${String.format("%02d:%02d", scheduleEndHour, scheduleEndMinute)} (Scheduled)"
        else -> "Detecting Now"
    }

    val statusColor by animateColorAsState(
        targetValue = if (isDetectionEnabled && !isInSchedulePeriod) ActiveGreen else InactiveRed,
        label = "statusColor"
    )

    // Pulse animation for detecting state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Find My Phone",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        // Subscription icon (placeholder - no real subscription)
                        IconButton(onClick = { /* subscription - skipped */ }) {
                            Icon(
                                Icons.Outlined.WorkspacePremium,
                                contentDescription = "Premium",
                                tint = PremiumGold
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // --- Main Toggle ---
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .then(
                            if (isDetectionEnabled && !isInSchedulePeriod) {
                                Modifier.scale(pulseScale)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer ring
                    Surface(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!isDetectionEnabled) {
                                if (!hasMicPermission()) {
                                    showMicPermissionDialog = true
                                } else {
                                    scope.launch {
                                        app.prefsManager.setDetectionEnabled(true)
                                        DetectionService.start(context)
                                        onEnableSuccess()
                                    }
                                }
                            } else {
                                scope.launch {
                                    app.prefsManager.setDetectionEnabled(false)
                                    DetectionService.stop(context)
                                    onDisableSuccess()
                                }
                            }
                            },
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Inner circle
                            Surface(
                                modifier = Modifier.size(120.dp),
                                shape = CircleShape,
                                color = statusColor
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            if (isDetectionEnabled) Icons.Filled.Mic
                                            else Icons.Filled.MicOff,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                            if (isDetectionEnabled) "ON" else "OFF",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pulse ring when active
                    if (isDetectionEnabled && !isInSchedulePeriod) {
                        Surface(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape),
                            shape = CircleShape,
                            color = statusColor.copy(alpha = pulseAlpha)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status text
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                // Whistle indicator
                if (whistleEnabled && isDetectionEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Whistle detection active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- Quick Settings Cards ---
                Text(
                    "Adjust detecting settings for smarter response",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sound Settings Card
                SettingsCard(
                    icon = Icons.Filled.VolumeUp,
                    title = "Ringtone",
                    subtitle = "Sound, volume & duration",
                    onClick = onNavigateToSoundSettings
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Flashlight Settings Card
                SettingsCard(
                    icon = Icons.Filled.FlashlightOn,
                    title = "FlashLight",
                    subtitle = "Blink patterns & effects",
                    onClick = onNavigateToFlashlightSettings
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Vibrate Settings Card
                SettingsCard(
                    icon = Icons.Filled.Vibration,
                    title = "Vibrate",
                    subtitle = "Vibration patterns",
                    onClick = onNavigateToVibrateSettings
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Widget & How to Use row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Try to trigger system widget picker or show tutorial
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                showWidgetTutorial = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+ Add widget to Home Screen", style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = onNavigateToHowToUse,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("How to use", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Settings tip bubble
            if (showSettingsTip) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                        .clickable {
                            showSettingsTip = false
                            scope.launch {
                                app.prefsManager.setSettingsTipShown(true)
                            }
                        }
                ) {
                    Text(
                        "Tap ⚙️ to adjust sensitivity & schedule",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    // --- Permission Dialogs ---

    if (showMicPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMicPermissionDialog = false },
            icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
            title = { Text("Microphone Permission Required") },
            text = {
                Text(
                    "To use \"Clap to Find Phone,\" we need access to your microphone.\n\n" +
                            "This allows the app to detect claps in the background and help you find your phone instantly."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showMicPermissionDialog = false
                        // We can't check rationale here easily; just launch mic permission
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFloatingPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showFloatingPermissionDialog = false },
            icon = { Icon(Icons.Filled.OpenInFull, contentDescription = null) },
            title = { Text("Allow Floating Window") },
            text = { Text("Required to show the ringing screen when your phone is found.") },
            confirmButton = {
                TextButton(onClick = {
                    showFloatingPermissionDialog = false
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFloatingPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
            title = { Text("Notification Permission Required") },
            text = { Text("Turn on notifications to control the feature in notification bar.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Widget tutorial
    if (showWidgetTutorial) {
        WidgetTutorialDialog(onDismiss = { showWidgetTutorial = false })
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun WidgetTutorialDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Please follow the steps below to add widget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TutorialStep("1", "Long press on the home screen, enter home screen edit mode")
                TutorialStep("2", "Tap \"Add Widgets\" or \"Widgets\" and find this app in the widget list")
                TutorialStep("3", "Drag and place the widget on the home screen")
                TutorialStep("4", "Tap the widget to activate/deactivate features")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
fun TutorialStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun shouldShowRationale(permission: String): Boolean {
    // Simplified — in real app, use ActivityCompat.shouldShowRequestPermissionRationale
    return true
}
