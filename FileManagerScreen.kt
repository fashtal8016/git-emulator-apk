package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.FileItem
import com.example.core.storage.ProjectEntity
import com.example.ui.theme.ForgeAmber
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeIndigo
import com.example.ui.theme.ForgeRose
import java.io.File

@Composable
fun FileManagerScreen(
    activeProject: ProjectEntity?,
    fileTree: List<FileItem>,
    onOpenFile: (File) -> Unit,
    onCreateFile: (parentDir: File, name: String) -> Unit,
    onCreateFolder: (parentDir: File, name: String) -> Unit,
    onRenameFile: (file: File, newName: String) -> Unit,
    onDeleteFile: (file: File) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_file_manager")
    ) {
        // Toolbar
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = activeProject?.name ?: "No Project",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "File Explorer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = { showCreateFileDialog = true },
                        modifier = Modifier.testTag("btn_create_file")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New File", tint = ForgeCyan)
                    }

                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.testTag("btn_create_folder")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = ForgeIndigo)
                    }
                }
            }
        }

        if (activeProject == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select an active project first to explore files.")
            }
        } else if (fileTree.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Project directory is empty.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(fileTree, key = { it.path }) { item ->
                    FileTreeItemRow(
                        item = item,
                        level = 0,
                        expandedMap = expandedFolders,
                        onOpenFile = onOpenFile,
                        onRename = { fileToRename = File(it.path) },
                        onDelete = { onDeleteFile(File(it.path)) }
                    )
                }
            }
        }
    }

    // Create File Dialog
    if (showCreateFileDialog && activeProject != null) {
        var newFileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name (e.g. MyClass.kt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            onCreateFile(File(activeProject.path), newFileName.trim())
                            showCreateFileDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog && activeProject != null) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(File(activeProject.path), newFolderName.trim())
                            showCreateFolderDialog = false
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Dialog
    if (fileToRename != null) {
        var renameText by remember { mutableStateOf(fileToRename!!.name) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameFile(fileToRename!!, renameText.trim())
                            fileToRename = null
                            onRefresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Text("Rename", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fileToRename = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FileTreeItemRow(
    item: FileItem,
    level: Int,
    expandedMap: MutableMap<String, Boolean>,
    onOpenFile: (File) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit
) {
    val isExpanded = expandedMap[item.path] ?: false

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (item.isDirectory) {
                    expandedMap[item.path] = !isExpanded
                } else {
                    onOpenFile(File(item.path))
                }
            }
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        if (item.isDirectory) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = ForgeIndigo,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(22.dp))
            val (icon, tint) = when (item.extension) {
                "kt", "kts" -> Pair(Icons.Default.Code, ForgeCyan)
                "xml" -> Pair(Icons.Default.Code, ForgeAmber)
                "json", "yaml", "yml" -> Pair(Icons.Default.Description, ForgeIndigo)
                else -> Pair(Icons.Default.Description, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (item.isSensitive) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = ForgeAmber.copy(alpha = 0.2f),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("SECRET", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = ForgeAmber)
                }
            }
        }

        IconButton(
            onClick = { onRename(item) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
        }

        IconButton(
            onClick = { onDelete(item) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ForgeRose, modifier = Modifier.size(14.dp))
        }
    }

    if (item.isDirectory && isExpanded) {
        item.children.forEach { child ->
            FileTreeItemRow(
                item = child,
                level = level + 1,
                expandedMap = expandedMap,
                onOpenFile = onOpenFile,
                onRename = onRename,
                onDelete = onDelete
            )
        }
    }
}
