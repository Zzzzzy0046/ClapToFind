package com.claptofind.phone.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claptofind.phone.ClapToFindApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to use?") },
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
                .padding(16.dp)
        ) {
            Text(
                "Clap to find phone",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            GuideStep(
                number = 1,
                title = "Turn on detection and allow microphone access.",
                description = "Tap the main button on the home screen to enable detection. Grant microphone permission when prompted."
            )

            GuideStep(
                number = 2,
                title = "When you can't find your phone, just clap or whistle clearly nearby.",
                description = "Make sure you're within detection range (up to ~10m) and clap clearly. Two quick claps trigger the alert."
            )

            GuideStep(
                number = 3,
                title = "The app detects your clap/whistle and rings your phone.",
                description = "You can customize the sound, flash, or vibration in the settings. Choose from 24 sounds, 6 flash patterns, and 6 vibration modes."
            )

            GuideStep(
                number = 4,
                title = "To stop the alert, tap \"Stop\" on the screen or press the volume button.",
                description = "Once you find your phone, tap the stop button or press any volume key or the power button to silence the alert."
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "💡 Tips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• First-time use runs at maximum sensitivity for best results.")
                    Text("• You can schedule quiet hours in Settings to avoid nighttime triggers.")
                    Text("• Add a home screen widget for quick access.")
                    Text("• Enable whistle detection for an alternative trigger method.")
                }
            }
        }
    }
}

@Composable
private fun GuideStep(number: Int, title: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "$number",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
