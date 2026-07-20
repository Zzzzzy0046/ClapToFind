package com.claptofind.phone.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RatingScreen(
    onDismiss: () -> Unit,
    fromSettings: Boolean = false
) {
    var rating by remember { mutableIntStateOf(0) }
    var showStarAnimation by remember { mutableStateOf(!fromSettings) }
    val starScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (showStarAnimation) {
            starScale.animateTo(1.2f, animationSpec = tween(300))
            starScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f))
            showStarAnimation = false
        }
    }

    val emoji: String
    val titleText: String
    val subtitleText: String
    when {
        rating == 0 -> {
            emoji = "🤔"; titleText = "Rate your experience"; subtitleText = "Your feedback helps us improve"
        }
        rating in 1..3 -> {
            emoji = "😔"; titleText = "We apologize for the inconvenience."; subtitleText = "Share your feedback to help us improve"
        }
        rating == 4 -> {
            emoji = "😊"; titleText = "The pleasure is ours!"; subtitleText = "Thank you for your support"
        }
        rating == 5 -> {
            emoji = "🥳"; titleText = "Could you please share your rating on Google Play?"; subtitleText = ""
        }
        else -> {
            emoji = "🤔"; titleText = "Rate your experience"; subtitleText = ""
        }
    }

    val context = LocalContext.current
    val app = ClapToFindApp.instance
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(emoji, fontSize = 48.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                if (titleText.isNotEmpty()) {
                    Text(titleText, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    for (i in 1..5) {
                        val filled = i <= rating
                        IconButton(
                            onClick = { rating = i },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                if (filled) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = "$i stars",
                                tint = if (filled) PremiumGold else Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                if (subtitleText.isNotEmpty() && rating < 5) {
                    Text(subtitleText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            if (rating == 5) {
                                app.prefsManager.setRatingGiven5Stars(true)
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                                onDismiss()
                            } else if (rating in 1..4) {
                                onDismiss()
                            }
                        }
                    },
                    enabled = rating > 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (rating == 5) "Rate on Google Play" else "Rate")
                }
                if (rating in 1..4) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        scope.launch {
                            app.prefsManager.setRatingAskShown(true)
                        }
                        onDismiss()
                    }) { Text("Send Feedback") }
                }
            }
        },
        dismissButton = {
            if (rating <= 0) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
