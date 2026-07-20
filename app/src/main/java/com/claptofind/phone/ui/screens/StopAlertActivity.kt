package com.claptofind.phone.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claptofind.phone.service.DetectionService
import com.claptofind.phone.ui.theme.ClapToFindTheme

// Alert screen colors
private val AlertRed = Color(0xFFE53935)
private val AlertDarkRed = Color(0xFFB71C1C)

/**
 * Full-screen activity shown when clap is detected.
 * Displays the stop alert screen with a big stop button.
 */
class StopAlertActivity : ComponentActivity() {

    private val alertStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn screen on and show even when locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set brightness to max
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1.0f
        window.attributes = layoutParams

        // Register receiver for alert stopped
        registerReceiver(alertStoppedReceiver, IntentFilter(DetectionService.ACTION_ALERT_STOPPED), RECEIVER_NOT_EXPORTED)

        setContent {
            ClapToFindTheme {
                StopAlertContent(
                    onStop = {
                        DetectionService.stopAlert(this)
                        finish()
                    }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_POWER -> {
                DetectionService.stopAlert(this)
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(alertStoppedReceiver) } catch (_: Exception) {}
    }
}

@Composable
fun StopAlertContent(
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(AlertRed, AlertDarkRed)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "👏",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Tap here or press the volume\nbutton to stop the ringing.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Got it! Your phone is here.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Stop button
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Stop,
                                contentDescription = "Stop alert",
                                tint = AlertRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "STOP",
                                color = AlertRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                // Outer ring
                Surface(
                    modifier = Modifier.size(180.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to home button
            TextButton(
                onClick = onStop,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Back To Home", fontSize = 16.sp)
            }
        }
    }
}
