package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nof1.utils.SecureStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        apiKey = secureStorage.getOpenAIApiKey() ?: ""
        baseUrl = secureStorage.getApiBaseUrl()
    }
    
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (apiKey.isNotBlank()) {
                                secureStorage.saveOpenAIApiKey(apiKey.trim())
                            } else {
                                secureStorage.removeOpenAIApiKey()
                            }
                            
                            if (baseUrl.isNotBlank()) {
                                secureStorage.saveApiBaseUrl(baseUrl.trim())
                            }
                            
                            showSuccessMessage = true
                        }
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (showSuccessMessage) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Settings saved successfully!",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "OpenAI Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "To enable AI-powered hypothesis generation, you'll need to provide your OpenAI API key. Your key is stored securely on your device and never shared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("OpenAI API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                )
                            }
                        }
                    )
                    
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("API Base URL (Optional)") },
                        placeholder = { Text("https://api.openai.com/") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Leave blank to use OpenAI's default endpoint") }
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How to get an OpenAI API key:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "1. Visit platform.openai.com",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Sign up or log in to your account",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. Go to API Keys section",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "4. Create a new secret key",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "5. Copy and paste it above",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            if (secureStorage.hasOpenAIApiKey()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            secureStorage.removeOpenAIApiKey()
                            apiKey = ""
                            showSuccessMessage = true
                        }
                    ) {
                        Text("Remove API Key")
                    }
                }
            }
        }
    }
}