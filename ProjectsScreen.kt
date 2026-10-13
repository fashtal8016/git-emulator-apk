package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.storage.ProjectEntity
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeIndigo
import com.example.ui.theme.ForgeRose
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    activeProject: ProjectEntity?,
    onSelectProject: (ProjectEntity) -> Unit,
    onCreateProject: (name: String, pkg: String) -> Unit,
    onImportZip: (Uri, String) -> Unit,
    onExportZip: (ProjectEntity, (File) -> Unit) -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportZipDialog by remember { mutableStateOf(false) }
    var importProjectName by remember { mutableStateOf("ImportedProject") }
    var selectedZipUri by remember { mutableStateOf<Uri?>(null) }

    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedZipUri = uri
            showImportZipDialog = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_projects"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_new_project")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Project", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { zipPicker.launch("application/zip") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_import_zip")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = ForgeIndigo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import ZIP")
                }
            }
        }

        item {
            Text(
                text = "Projects Workspace (${projects.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (projects.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No projects yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Create a new Kotlin starter or import an AI-generated ZIP project.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { project ->
                val isActive = activeProject?.id == project.id
                ProjectCardItem(
                    project = project,
                    isActive = isActive,
                    onSelect = { onSelectProject(project) },
                    onExport = { onExportZip(project) {} },
                    onDelete = { onDeleteProject(project) }
                )
            }
        }
    }

    // New Project Dialog
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var pkg by remember { mutableStateOf("com.example.myapp") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project Name") },
                        placeholder = { Text("e.g. MyCoolApp") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_project_name")
                    )

                    OutlinedTextField(
                        value = pkg,
                        onValueChange = { pkg = it },
                        label = { Text("Package Name") },
                        placeholder = { Text("com.example.myapp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateProject(name.trim(), pkg.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Text("Create", color = MaterialTheme.colorScheme.surface)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import ZIP Confirmation Dialog
    if (showImportZipDialog && selectedZipUri != null) {
        AlertDialog(
            onDismissRequest = { showImportZipDialog = false },
            title = { Text("Import ZIP Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Specify a directory name for the imported project:")
                    OutlinedTextField(
                        value = importProjectName,
                        onValueChange = { importProjectName = it },
                        label = { Text("Project Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "KotlinForge extracts the ZIP safely with Zip-Slip path traversal protection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportZip(selectedZipUri!!, importProjectName.trim())
                        showImportZipDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Text("Extract & Import", color = MaterialTheme.colorScheme.surface)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportZipDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProjectCardItem(
    project: ProjectEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(project.lastModified))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("project_item_${project.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (isActive) ForgeCyan else ForgeIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ForgeCyan.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = ForgeCyan)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = project.description.ifEmpty { project.path },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Modified: $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.Archive, contentDescription = "Export ZIP", tint = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = ForgeRose)
                    }
                }
            }
        }
    }
}
