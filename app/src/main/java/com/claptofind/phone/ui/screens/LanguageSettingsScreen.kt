package com.claptofind.phone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.data.SupportedLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit
) {
    val app = ClapToFindApp.instance
    val scope = rememberCoroutineScope()

    val currentLanguage by app.prefsManager.selectedLanguage.collectAsState(initial = "System")
    var selected by remember { mutableStateOf(SupportedLanguage.fromDisplayName(currentLanguage)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Languages") },
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
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        scope.launch {
                            app.prefsManager.setSelectedLanguage(selected.displayName)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text("Apply")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(SupportedLanguage.entries) { language ->
                val isSelected = selected == language
                ListItem(
                    headlineContent = { Text(language.displayName) },
                    trailingContent = {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.clickable { selected = language },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}
