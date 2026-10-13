package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AiPromptType
import com.example.core.git.GitEngine
import com.example.ui.EditorTab
import com.example.ui.editor.SyntaxHighlighter
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeIndigo
import com.example.ui.theme.ForgeRose
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900

@Composable
fun EditorScreen(
    openTabs: List<EditorTab>,
    activeTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onContentChanged: (String) -> Unit,
    onSave: () -> Unit,
    onAiAction: (AiPromptType, String) -> Unit,
    aiResponse: String?,
    onClearAiResponse: () -> Unit
) {
    var showFindReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showDiffView by remember { mutableStateOf(false) }
    var showAiModal by remember { mutableStateOf(false) }
    var wordWrap by remember { mutableStateOf(true) }
    var editorFontSize by remember { mutableStateOf(13) }

    val currentTab = openTabs.getOrNull(activeTabIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_editor")
    ) {
        // Tab Strip
        if (openTabs.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                itemsIndexed(openTabs) { idx, tab ->
                    val isSelected = idx == activeTabIndex
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .clickable { onTabSelected(idx) }
                            .padding(end = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${tab.title}${if (tab.isDirty) " *" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (tab.isDirty) ForgeCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onCloseTab(idx) },
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        // Toolbar
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSave,
                        enabled = currentTab?.isDirty == true,
                        modifier = Modifier.testTag("btn_editor_save")
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            tint = if (currentTab?.isDirty == true) ForgeCyan else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(onClick = { showFindReplace = !showFindReplace }) {
                        Icon(Icons.Default.FindReplace, contentDescription = "Find/Replace", tint = if (showFindReplace) ForgeCyan else MaterialTheme.colorScheme.outline)
                    }

                    IconButton(onClick = { showDiffView = !showDiffView }) {
                        Icon(Icons.Default.Difference, contentDescription = "Diff", tint = if (showDiffView) ForgeEmerald else MaterialTheme.colorScheme.outline)
                    }

                    IconButton(onClick = { wordWrap = !wordWrap }) {
                        Icon(Icons.AutoMirrored.Filled.WrapText, contentDescription = "Word wrap", tint = if (wordWrap) ForgeCyan else MaterialTheme.colorScheme.outline)
                    }

                    IconButton(onClick = { editorFontSize = if (editorFontSize >= 16) 11 else editorFontSize + 2 }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "Font size", tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Button(
                    onClick = { showAiModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeIndigo),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("btn_editor_ai")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Assistant", fontSize = 12.sp)
                }
            }
        }

        // Find & Replace Strip
        if (showFindReplace && currentTab != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = { findQuery = it },
                        placeholder = { Text("Find...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        placeholder = { Text("Replace...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (findQuery.isNotEmpty()) {
                                val replaced = currentTab.content.replace(findQuery, replaceQuery)
                                onContentChanged(replaced)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                    ) {
                        Text("Replace All", fontSize = 11.sp, color = Color.Black)
                    }
                }
            }
        }

        if (currentTab == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No open files",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Open a file from the File Manager to start editing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else if (showDiffView) {
            // Visual Diff View
            val diffLines = remember(currentTab.originalContent, currentTab.content) {
                GitEngine.generateUnifiedDiff(currentTab.originalContent, currentTab.content)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate900)
                    .padding(8.dp)
            ) {
                items(diffLines.size) { idx ->
                    val line = diffLines[idx]
                    val bgColor = when (line.type) {
                        com.example.core.git.GitDiffLine.LineType.ADDED -> ForgeEmerald.copy(alpha = 0.2f)
                        com.example.core.git.GitDiffLine.LineType.REMOVED -> ForgeRose.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }
                    val textColor = when (line.type) {
                        com.example.core.git.GitDiffLine.LineType.ADDED -> ForgeEmerald
                        com.example.core.git.GitDiffLine.LineType.REMOVED -> ForgeRose
                        else -> Color.White
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor)
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = "${line.newLineNumber ?: line.oldLineNumber ?: ""}".padStart(4),
                            style = CodeTextStyle.copy(color = Color.Gray, fontSize = editorFontSize.sp),
                            modifier = Modifier.width(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = line.content,
                            style = CodeTextStyle.copy(color = textColor, fontSize = editorFontSize.sp)
                        )
                    }
                }
            }
        } else {
            // Code Editor with Line Numbers
            val lineCount = remember(currentTab.content) { currentTab.content.lines().size }
            val vScrollState = rememberScrollState()
            val hScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate900)
            ) {
                // Line Numbers Gutter
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                        .background(Slate850)
                        .verticalScroll(vScrollState)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = "$i",
                            style = CodeTextStyle.copy(color = Color.Gray, fontSize = editorFontSize.sp),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                // Code Input Area
                val ext = remember(currentTab.file.name) { currentTab.file.extension }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(vScrollState)
                        .then(if (!wordWrap) Modifier.horizontalScroll(hScrollState) else Modifier)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = currentTab.content,
                        onValueChange = onContentChanged,
                        textStyle = CodeTextStyle.copy(fontSize = editorFontSize.sp, color = MaterialTheme.colorScheme.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_code_editor")
                    )
                }
            }
        }
    }

    // AI Assistant Dialog
    if (showAiModal && currentTab != null) {
        var customPrompt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAiModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ForgeIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KotlinForge AI Assistant")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "File: ${currentTab.title}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                onAiAction(AiPromptType.EXPLAIN, "")
                                showAiModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Explain", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = {
                                onAiAction(AiPromptType.FIX_ERRORS, "")
                                showAiModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fix Errors", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                onAiAction(AiPromptType.OPTIMIZE, "")
                                showAiModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Optimize", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = {
                                onAiAction(AiPromptType.GENERATE_TESTS, "")
                                showAiModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Gen Tests", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        placeholder = { Text("Custom prompt or question...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPrompt.isNotBlank()) {
                            onAiAction(AiPromptType.CUSTOM, customPrompt)
                            showAiModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Text("Submit", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAiModal = false }) { Text("Close") }
            }
        )
    }

    // AI Response Dialog
    if (aiResponse != null) {
        AlertDialog(
            onDismissRequest = onClearAiResponse,
            title = { Text("AI Analysis Result") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    item {
                        Text(
                            text = aiResponse,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onClearAiResponse,
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Text("Dismiss", color = Color.Black)
                }
            }
        )
    }
}
