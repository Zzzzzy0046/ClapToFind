package com.claptofind.phone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.data.VibrateMode
import com.claptofind.phone.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrateSettingsScreen(
    onBack: () -> Unit
) {
    val app = ClapToFindApp.instance
    val scope = rememberCoroutineScope()

    val enabled by app.prefsManager.vibrateEnabled.collectAsState(initial = true)
    val selectedMode by app.prefsManager.vibrateMode.collectAsState(initial = "High Frequency")
    val isPro by app.prefsManager.isProUser.collectAsState(initial = false)

    var localEnabled by remember(enabled) { mutableStateOf(enabled) }
    var localMode by remember(selectedMode) { mutableStateOf(selectedMode) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }

    fun markChanged() { hasUnsavedChanges = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vibrate") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showExitDialog = true
                        else onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        scope.launch {
                            val mode = VibrateMode.fromDisplayName(localMode)
                            if (mode.isPremium && !isPro) return@launch
                            app.prefsManager.setVibrateEnabled(localEnabled)
                            app.prefsManager.setVibrateMode(localMode)
                            hasUnsavedChanges = false
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text("Apply Change") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Enable switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Vibrate Effect", fontWeight = FontWeight.Medium)
                        Text(
                            "Vibrates when phone is found",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = localEnabled,
                        onCheckedChange = { localEnabled = it; markChanged() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Vibrate modes
            VibrateMode.entries.forEach { mode ->
                val isLocked = mode.isPremium && !isPro
                val isSelected = localMode == mode.displayName

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (!localEnabled) {
                                showToastMessage = "Vibrate off. Enable to choose effects."
                                return@clickable
                            }
                            if (isLocked) return@clickable
                            localMode = mode.displayName
                            markChanged()
                            app.soundEngine.playVibratePreview(mode)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!localEnabled) Color.Gray.copy(alpha = 0.1f)
                        else if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                mode.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (!localEnabled) Color.Gray else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                getVibrateDescription(mode),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!localEnabled) Color.Gray.copy(alpha = 0.5f) else Color.Gray
                            )
                        }
                        if (isLocked) {
                            Icon(Icons.Filled.Lock, "Premium", tint = PremiumGold)
                        } else if (isSelected) {
                            Icon(Icons.Filled.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Toast
            showToastMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2000)
                    showToastMessage = null
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Apply change before leaving?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        app.prefsManager.setVibrateEnabled(localEnabled)
                        app.prefsManager.setVibrateMode(localMode)
                        showExitDialog = false; onBack()
                    }
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) { Text("Discard") }
            }
        )
    }
}

private fun getVibrateDescription(mode: VibrateMode): String = when (mode) {
    VibrateMode.HIGH_FREQUENCY -> "0.1s vibrate + 0.1s stop, repeating"
    VibrateMode.MEDIUM_FREQUENCY -> "0.3s vibrate + 0.3s stop, repeating"
    VibrateMode.SLOW_FREQUENCY -> "0.6s vibrate + 0.6s stop, repeating"
    VibrateMode.SOS_PATTERN -> "Short ×3 → Long ×3 → Short ×3"
    VibrateMode.RHYTHMIC_PATTERN -> "0.2s×2 + 0.5s pause, loop"
    VibrateMode.RANDOM_PATTERN -> "Random 0.1s~0.7s combinations"
}
