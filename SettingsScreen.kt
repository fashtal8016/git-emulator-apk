package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.storage.AppPreferences
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeRose

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onSaveGitAuthor: (name: String, email: String) -> Unit,
    onClearCredentials: () -> Unit
) {
    var gitName by remember { mutableStateOf(preferences.gitAuthorName.value) }
    var gitEmail by remember { mutableStateOf(preferences.gitAuthorEmail.value) }
    var fontSize by remember { mutableStateOf(preferences.editorFontSize.value.toFloat()) }
    var wordWrap by remember { mutableStateOf(preferences.editorWordWrap.value) }
    var lineNumbers by remember { mutableStateOf(preferences.editorLineNumbers.value) }
    var autoSave by remember { mutableStateOf(preferences.autoSave.value) }
    var themeMode by remember { mutableStateOf(preferences.themeMode.value) }
    var language by remember { mutableStateOf(preferences.language.value) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_settings"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Git Configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Git Author Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = gitName,
                        onValueChange = { gitName = it },
                        label = { Text("Author Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = gitEmail,
                        onValueChange = { gitEmail = it },
                        label = { Text("Author Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onSaveGitAuthor(gitName.trim(), gitEmail.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                    ) {
                        Text("Save Author Details", color = Color.Black)
                    }
                }
            }
        }

        // Editor Preferences
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Editor Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size: ${fontSize.toInt()}sp")
                        Slider(
                            value = fontSize,
                            onValueChange = {
                                fontSize = it
                                preferences.setEditorSettings(it.toInt(), wordWrap, lineNumbers, autoSave)
                            },
                            valueRange = 10f..20f,
                            steps = 9,
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Word Wrap")
                        Switch(
                            checked = wordWrap,
                            onCheckedChange = {
                                wordWrap = it
                                preferences.setEditorSettings(fontSize.toInt(), it, lineNumbers, autoSave)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show Line Numbers")
                        Switch(
                            checked = lineNumbers,
                            onCheckedChange = {
                                lineNumbers = it
                                preferences.setEditorSettings(fontSize.toInt(), wordWrap, it, autoSave)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Auto Save")
                        Switch(
                            checked = autoSave,
                            onCheckedChange = {
                                autoSave = it
                                preferences.setEditorSettings(fontSize.toInt(), wordWrap, lineNumbers, it)
                            }
                        )
                    }
                }
            }
        }

        // Language & Localization
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Language / زبان",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                language = "en"
                                preferences.setLanguage("en")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (language == "en") ForgeCyan else MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("English", color = if (language == "en") Color.Black else MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = {
                                language = "fa"
                                preferences.setLanguage("fa")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (language == "fa") ForgeCyan else MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("فارسی (Persian)", color = if (language == "fa") Color.Black else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Security & Keystore
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Security & Keystore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Tokens and secrets are encrypted with AES-256-GCM via the Android hardware Keystore. No plain credentials are ever written to disk or logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onClearCredentials,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeRose)
                    ) {
                        Text("Purge Stored Tokens & Credentials")
                    }
                }
            }
        }
    }
}
