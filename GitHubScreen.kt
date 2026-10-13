package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.network.GitHubConnectionState
import com.example.core.network.GitHubRepo
import com.example.core.network.GitHubUser
import com.example.core.network.RateLimitInfo
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeIndigo
import com.example.ui.theme.ForgeRose

@Composable
fun GitHubScreen(
    connectionState: GitHubConnectionState,
    currentUser: GitHubUser?,
    rateLimit: RateLimitInfo?,
    repos: List<GitHubRepo>,
    selectedRepo: GitHubRepo?,
    onConnectToken: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRefreshRepos: () -> Unit,
    onCreateRepo: (name: String, desc: String, isPrivate: Boolean) -> Unit,
    onSelectRepo: (GitHubRepo) -> Unit
) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf("") }
    var showTokenDialog by remember { mutableStateOf(false) }
    var showCreateRepoDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRepos = remember(searchQuery, repos) {
        if (searchQuery.isBlank()) repos
        else repos.filter { it.name.contains(searchQuery, ignoreCase = true) || it.fullName.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_github"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentUser != null && currentUser.avatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = currentUser.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(28.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (currentUser != null) currentUser.login else "GitHub Offline",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentUser != null) (currentUser.name ?: "Connected") else "Not authenticated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isConnected = connectionState is GitHubConnectionState.Connected
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isConnected) ForgeEmerald.copy(alpha = 0.2f) else ForgeRose.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConnected) ForgeEmerald else ForgeRose,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentUser != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Rate Limit Remaining: ${rateLimit?.remaining ?: "-"}/60",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Total Repos: ${currentUser.publicRepos + currentUser.totalPrivateRepos}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onRefreshRepos,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refresh")
                            }

                            Button(
                                onClick = onDisconnect,
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeRose),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disconnect")
                            }
                        }
                    } else {
                        Text(
                            text = "Connect securely to GitHub using official OAuth or Personal Access Token (stored encrypted in Android Keystore).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showTokenDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_connect_github")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect GitHub", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val tokenUrl = "https://github.com/settings/tokens/new?scopes=repo,workflow,write:packages&description=KotlinForge%20AI%20IDE"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tokenUrl)))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Get Token")
                            }
                        }
                    }
                }
            }
        }

        // Repositories Section
        if (currentUser != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Repositories (${repos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showCreateRepoDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeIndigo),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Repo", fontSize = 12.sp)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search repositories...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(filteredRepos, key = { it.id }) { repo ->
                val isSelected = selectedRepo?.id == repo.id
                RepoItemCard(
                    repo = repo,
                    isSelected = isSelected,
                    onSelect = { onSelectRepo(repo) }
                )
            }
        }
    }

    // Connect Token Dialog
    if (showTokenDialog) {
        var tokenText by remember { mutableStateOf(tokenInput) }
        var showPassword by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("Connect to GitHub") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter or paste your GitHub Personal Access Token (classic or fine-grained) with 'repo' and 'workflow' scopes.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = tokenText,
                        onValueChange = { tokenText = it },
                        label = { Text("GitHub Token (ghp_...)") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_github_token")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Token", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = showPassword, onCheckedChange = { showPassword = it })
                    }

                    Text(
                        text = "Secured: Token is encrypted via AES-256-GCM using hardware-backed Android Keystore.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgeEmerald
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tokenText.isNotBlank()) {
                            onConnectToken(tokenText.trim())
                            showTokenDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                    modifier = Modifier.testTag("btn_confirm_token")
                ) {
                    Text("Connect", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTokenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Create Repo Dialog
    if (showCreateRepoDialog) {
        var repoName by remember { mutableStateOf("") }
        var repoDesc by remember { mutableStateOf("") }
        var isPrivate by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreateRepoDialog = false },
            title = { Text("Create GitHub Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = repoName,
                        onValueChange = { repoName = it },
                        label = { Text("Repository Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = repoDesc,
                        onValueChange = { repoDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Private Repository", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (repoName.isNotBlank()) {
                            onCreateRepo(repoName.trim(), repoDesc.trim(), isPrivate)
                            showCreateRepoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateRepoDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RepoItemCard(
    repo: GitHubRepo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (repo.private) Icons.Default.Lock else Icons.Default.Link,
                        contentDescription = null,
                        tint = if (repo.private) ForgeIndigo else ForgeCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = repo.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ForgeCyan.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("SELECTED", style = MaterialTheme.typography.labelSmall, color = ForgeCyan)
                        }
                    }
                }
            }

            if (!repo.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Branch: ${repo.defaultBranch}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${repo.stargazersCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
