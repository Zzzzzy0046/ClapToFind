package com.claptofind.phone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claptofind.phone.ClapToFindApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit
) {
    val app = ClapToFindApp.instance
    val scope = rememberCoroutineScope()

    val savedDraft by app.prefsManager.feedbackDraft.collectAsState(initial = "")
    var feedbackText by remember(savedDraft) { mutableStateOf(savedDraft) }
    var showThanksToast by remember { mutableStateOf(false) }

    val isValid = feedbackText.trim().length >= 5

    // Save draft on change
    LaunchedEffect(feedbackText) {
        if (feedbackText.isNotEmpty()) {
            scope.launch { app.prefsManager.setFeedbackDraft(feedbackText) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback") },
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
                .padding(16.dp)
        ) {
            Text(
                "Describe any issue or idea to help us improve.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = feedbackText,
                onValueChange = { if (it.length <= 2000) feedbackText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Please enter your feedback here (at least 5 characters)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                maxLines = 15,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "${feedbackText.length}/2000",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        // In production: send feedback to server
                        app.prefsManager.setFeedbackDraft("")
                        feedbackText = ""
                        showThanksToast = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid
            ) {
                Text("Submit")
            }

            if (showThanksToast) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.inverseSurface
                ) {
                    Text(
                        "Thanks for your feedback!",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                LaunchedEffect(Unit) {
                    delay(3000)
                    showThanksToast = false
                }
            }
        }
    }
}
