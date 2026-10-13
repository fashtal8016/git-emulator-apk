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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.git.GitFileStatus
import com.example.core.network.GitHubRepo
import com.example.core.storage.ProjectEntity
import com.example.ui.theme.ForgeAmber
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeIndigo
import com.example.ui.theme.ForgeRose

@Composable
fun GitScreen(
    activeProject: ProjectEntity?,
    selectedRepo: GitHubRepo?,
    gitStatuses: List<GitFileStatus>,
    onRefreshStatus: () -> Unit,
    onCommitAndPush: (message: String, owner: String, repo: String, branch: String) -> Unit
) {
    var commitMessage by remember { mutableStateOf("") }
    val changedFiles = remember(gitStatuses) {
        gitStatuses.filter { it.status != GitFileStatus.StatusType.UNMODIFIED }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_git"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target Repository & Branch Info
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Git Version Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedRepo != null) "Remote: ${selectedRepo.fullName}" else "No remote repository linked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onRefreshStatus) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ForgeCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ForgeIndigo.copy(alpha = 0.2f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.SyncAlt, contentDescription = null, tint = ForgeIndigo, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Branch: ${selectedRepo?.defaultBranch ?: "main"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForgeIndigo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "${changedFiles.size} changed files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Commit Form
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Commit Changes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        placeholder = { Text("Enter commit message (e.g. Update UI and fix build)...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_commit_message")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val canCommit = commitMessage.isNotBlank() && selectedRepo != null && changedFiles.isNotEmpty()

                    Button(
                        onClick = {
                            if (canCommit) {
                                val owner = selectedRepo!!.fullName.substringBefore('/')
                                val repoName = selectedRepo.name
                                val branch = selectedRepo.defaultBranch
                                onCommitAndPush(commitMessage.trim(), owner, repoName, branch)
                                commitMessage = ""
                            }
                        },
                        enabled = canCommit,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_commit_push")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Commit & Push to GitHub", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (selectedRepo == null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Link a GitHub repository in the GitHub tab to push commits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForgeRose
                        )
                    }
                }
            }
        }

        // Staged & Working Tree Changes
        item {
            Text(
                text = "Working Tree Changes (${changedFiles.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (changedFiles.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForgeEmerald, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Working tree is clean", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "No uncommitted modifications detected in project.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(changedFiles) { statusItem ->
                GitStatusRowItem(status = statusItem)
            }
        }
    }
}

@Composable
private fun GitStatusRowItem(status: GitFileStatus) {
    val (statusText, statusColor, statusIcon) = when (status.status) {
        GitFileStatus.StatusType.ADDED -> Triple("ADDED", ForgeEmerald, Icons.Default.Add)
        GitFileStatus.StatusType.MODIFIED -> Triple("MODIFIED", ForgeCyan, Icons.Default.Edit)
        GitFileStatus.StatusType.DELETED -> Triple("DELETED", ForgeRose, Icons.Default.Delete)
        GitFileStatus.StatusType.CONFLICT -> Triple("CONFLICT", ForgeAmber, Icons.Default.Warning)
        else -> Triple("SYNCED", Color.Gray, Icons.Default.CheckCircle)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.relativePath,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (status.isSensitive) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ForgeAmber.copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "SENSITIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ForgeAmber,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
