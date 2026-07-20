package com.claptofind.phone.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.data.SoundDuration
import com.claptofind.phone.data.SoundEffect
import com.claptofind.phone.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsScreen(
    onBack: () -> Unit
) {
    val app = ClapToFindApp.instance
    val scope = rememberCoroutineScope()

    val volume by app.prefsManager.soundVolume.collectAsState(initial = 80)
    val duration by app.prefsManager.soundDuration.collectAsState(initial = 15)
    val selectedSound by app.prefsManager.selectedSound.collectAsState(initial = "Air Horn")
    val isMuted by app.prefsManager.isMuted.collectAsState(initial = false)
    val isPro by app.prefsManager.isProUser.collectAsState(initial = false)

    var showFreeSounds by remember { mutableStateOf(true) }
    var showPremiumSounds by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showVolumeLockDialog by remember { mutableStateOf(false) }

    // Local editing state
    var localVolume by remember(volume) { mutableIntStateOf(volume) }
    var localMuted by remember(isMuted) { mutableStateOf(isMuted) }
    var localDuration by remember(duration) { mutableIntStateOf(duration) }
    var localSound by remember(selectedSound) { mutableStateOf(selectedSound) }

    fun markChanged() { hasUnsavedChanges = true }

    // Preview playback
    LaunchedEffect(localSound) {
        if (isPlaying) {
            app.soundEngine.playPreview(localSound, localVolume)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sound") },
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
                            // Check premium sounds
                            val sound = SoundEffect.fromDisplayName(localSound)
                            if (sound.isPremium && !isPro) {
                                // In production: navigate to subscription
                                return@launch
                            }
                            app.prefsManager.setSoundVolume(localVolume)
                            app.prefsManager.setIsMuted(localMuted)
                            app.prefsManager.setSoundDuration(localDuration)
                            app.prefsManager.setSelectedSound(localSound)
                            hasUnsavedChanges = false
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Apply", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Volume Section ---
            Text(
                "Volume",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = {
                            localMuted = !localMuted
                            markChanged()
                        }) {
                            Icon(
                                if (localMuted || localVolume == 0) Icons.Filled.VolumeOff
                                else Icons.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = localVolume.toFloat(),
                            onValueChange = {
                                if (it > 100f && !isPro) {
                                    showVolumeLockDialog = true
                                    return@Slider
                                }
                                localVolume = it.toInt()
                                markChanged()
                            },
                            valueRange = 0f..if (isPro) 200f else 100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Text(
                            "${localVolume}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    // Min/Max labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            if (isPro) "200" else "100",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // Volume Boost indicator for pro
                    if (isPro && localVolume > 100) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Volume Boost Active — Up to 200%",
                            style = MaterialTheme.typography.bodySmall,
                            color = PremiumDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Sound Duration ---
            Text(
                "Sound Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 60, 180).forEach { secs ->
                    val label = when (secs) {
                        15 -> "15s"
                        30 -> "30s"
                        60 -> "1min"
                        180 -> "3mins"
                        else -> "${secs}s"
                    }
                    FilterChip(
                        selected = localDuration == secs,
                        onClick = {
                            localDuration = secs
                            markChanged()
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Free Sounds ---
            Text(
                "Free Sound",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val visibleFreeSounds = if (showFreeSounds)
                SoundEffect.freeSounds.take(3) else SoundEffect.freeSounds

            visibleFreeSounds.forEach { sound ->
                SoundItem(
                    sound = sound,
                    isSelected = localSound == sound.displayName,
                    isPlaying = isPlaying && localSound == sound.displayName,
                    onClick = {
                        localSound = sound.displayName
                        markChanged()
                    },
                    onPlayPause = {
                        isPlaying = !isPlaying
                        if (isPlaying) {
                            app.soundEngine.playPreview(sound.displayName, localVolume)
                        } else {
                            app.soundEngine.stopAlert()
                        }
                    }
                )
            }

            if (SoundEffect.freeSounds.size > 3) {
                TextButton(
                    onClick = { showFreeSounds = !showFreeSounds },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(if (showFreeSounds) "Show More" else "Collapse")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Premium Sounds ---
            Text(
                "Premium Sound",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val visiblePremiumSounds = if (showPremiumSounds)
                SoundEffect.premiumSounds.take(3) else SoundEffect.premiumSounds

            visiblePremiumSounds.forEach { sound ->
                SoundItem(
                    sound = sound,
                    isSelected = localSound == sound.displayName,
                    isPlaying = isPlaying && localSound == sound.displayName,
                    isLocked = !isPro,
                    onClick = {
                        if (!isPro) {
                            // Navigate to subscription
                            return@SoundItem
                        }
                        localSound = sound.displayName
                        markChanged()
                    },
                    onPlayPause = {
                        if (!isPro) return@SoundItem
                        isPlaying = !isPlaying
                        if (isPlaying) app.soundEngine.playPreview(sound.displayName, localVolume)
                        else app.soundEngine.stopAlert()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { showPremiumSounds = !showPremiumSounds },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(if (showPremiumSounds) "Show More" else "Collapse")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Exit unsaved dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Apply change before leaving?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        app.prefsManager.setSoundVolume(localVolume)
                        app.prefsManager.setIsMuted(localMuted)
                        app.prefsManager.setSoundDuration(localDuration)
                        app.prefsManager.setSelectedSound(localSound)
                        showExitDialog = false
                        onBack()
                    }
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) { Text("Discard") }
            }
        )
    }

    // Volume lock dialog
    if (showVolumeLockDialog) {
        AlertDialog(
            onDismissRequest = { showVolumeLockDialog = false },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            title = { Text("Unlock Volume Boost") },
            text = {
                Column {
                    Text("Boost volume up to 200%")
                    Text("Make your phone louder than ever.")
                    Text("Perfect for finding your phone quickly")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showVolumeLockDialog = false
                    // In production: navigate to subscription
                }) { Text("Subscribe Now") }
            },
            dismissButton = {
                TextButton(onClick = { showVolumeLockDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SoundItem(
    sound: SoundEffect,
    isSelected: Boolean,
    isPlaying: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying && isSelected) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }

            Text(
                sound.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurface
            )

            if (isLocked) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Premium",
                    tint = PremiumGold,
                    modifier = Modifier.size(20.dp)
                )
            } else if (sound.isPremium) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = "Premium",
                    tint = PremiumGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
