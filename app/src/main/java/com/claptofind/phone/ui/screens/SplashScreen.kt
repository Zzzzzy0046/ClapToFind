package com.claptofind.phone.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    onNavigateToLanguage: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(800),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        showContent = true
        delay(3000)

        val app = ClapToFindApp.instance
        val hasCompleted = app.prefsManager.run {
            kotlinx.coroutines.runBlocking {
                hasCompletedOnboarding.first()
            }
        }

        if (hasCompleted) {
            onNavigateToHome()
        } else {
            onNavigateToLanguage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("👏", fontSize = 48.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Clap To Find Phone",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Smart sound detection. Instant response.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
