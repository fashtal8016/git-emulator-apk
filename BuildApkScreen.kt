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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.network.ArtifactItem
import com.example.core.network.GitHubRepo
import com.example.core.network.WorkflowRun
import com.example.core.storage.BuildHistoryEntity
import com.example.core.storage.ProjectEntity
import com.example.ui.theme.ForgeAmber
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeIndigo
import com.example.ui.theme.ForgeRose
import java.io.File
import java.text.DecimalFormat

@Composable
fun BuildApkScreen(
    activeProject: ProjectEntity?,
    selectedRepo: GitHubRepo?,
    workflowRuns: List<WorkflowRun>,
    activeRun: WorkflowRun?,
    artifacts: List<ArtifactItem>,
    buildHistory: List<BuildHistoryEntity>,
    isBusy: Boolean,
    onTriggerBuild: (owner: String, repo: String, type: String) -> Unit,
    onRefreshRuns: (owner: String, repo: String) -> Unit,
    onDownloadArtifact: (owner: String, repo: String, run: WorkflowRun, artifact: ArtifactItem) -> Unit,
    onGenerateWorkflow: () -> Unit,
    onInstallApk: (File) -> Unit,
    onShareApk: (File) -> Unit,
    onDownloadFromUrl: ((String) -> Unit)? = null
) {
    var directUrlInput by remember { mutableStateOf("https://github.com/fashtal8016/Kotlin-forge-ai-ide.apk") }
    val context = LocalContext.current
    val owner = selectedRepo?.fullName?.substringBefore('/') ?: ""
    val repoName = selectedRepo?.name ?: ""

    val completedBuildsWithApk = remember(buildHistory) {
        buildHistory.filter { it.apkPath.isNotEmpty() && File(it.apkPath).exists() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_build_apk"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Build Engine Header Card
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
                                text = "GitHub Actions Cloud Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedRepo != null) "Repo: ${selectedRepo.fullName}" else "Select a GitHub repository to build",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (selectedRepo != null) {
                            IconButton(onClick = { onRefreshRuns(owner, repoName) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ForgeCyan)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedRepo == null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ForgeAmber.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ForgeAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connect and select a GitHub repository in the GitHub tab first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ForgeAmber
                                )
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onTriggerBuild(owner, repoName, "debug") },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_build_debug")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Build Debug APK", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onTriggerBuild(owner, repoName, "release") },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_build_release")
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = ForgeIndigo, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Build Release")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onGenerateWorkflow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Inject / Repair .github/workflows/android-build.yml")
                        }
                    }
                }
            }
        }

        // Direct Download from GitHub Link Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = ForgeCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Direct GitHub APK Link Download",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Paste any GitHub raw, release or repository APK link to download and install",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = directUrlInput,
                        onValueChange = { directUrlInput = it },
                        label = { Text("GitHub APK Link") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_direct_apk_url")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (directUrlInput.isNotBlank() && onDownloadFromUrl != null) {
                                onDownloadFromUrl(directUrlInput.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_direct_apk_download")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download & Install APK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Workflow Run Status
        if (activeRun != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (statusColor, statusIcon) = when (activeRun.conclusion) {
                                    "success" -> Pair(ForgeEmerald, Icons.Default.CheckCircle)
                                    "failure" -> Pair(ForgeRose, Icons.Default.Error)
                                    else -> when (activeRun.status) {
                                        "in_progress" -> Pair(ForgeCyan, Icons.Default.HourglassEmpty)
                                        else -> Pair(ForgeAmber, Icons.Default.HourglassEmpty)
                                    }
                                }

                                Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Workflow Run #${activeRun.runNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (activeRun.conclusion == "success") ForgeEmerald.copy(alpha = 0.2f) else ForgeCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = (activeRun.conclusion ?: activeRun.status).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeRun.conclusion == "success") ForgeEmerald else ForgeCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Branch: ${activeRun.headBranch} • Commit: ${activeRun.headSha.take(7)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (activeRun.htmlUrl.isNotEmpty()) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeRun.htmlUrl)))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View on GitHub")
                            }

                            Button(
                                onClick = { onRefreshRuns(owner, repoName) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeIndigo),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Poll Status")
                            }
                        }
                    }
                }
            }

            // Artifacts List
            if (artifacts.isNotEmpty()) {
                item {
                    Text(
                        text = "Build Output Artifacts (${artifacts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(artifacts, key = { it.id }) { artifact ->
                    ArtifactCardItem(
                        artifact = artifact,
                        onDownload = { onDownloadArtifact(owner, repoName, activeRun, artifact) }
                    )
                }
            }
        }

        // Saved Local APKs from Build History
        if (completedBuildsWithApk.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Local APKs (${completedBuildsWithApk.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(completedBuildsWithApk, key = { it.id }) { item ->
                val apkFile = File(item.apkPath)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, tint = ForgeEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = apkFile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Size: ${formatBytes(item.artifactSize)} | SHA-256: ${item.sha256.take(12)}...",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onInstallApk(apkFile) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeEmerald),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_install_apk")
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Install APK", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onShareApk(apkFile) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtifactCardItem(
    artifact: ArtifactItem,
    onDownload: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artifact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatBytes(artifact.sizeInBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                modifier = Modifier.testTag("btn_download_artifact")
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download APK", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val num = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return "${DecimalFormat("#,##0.#").format(num)} ${units[digitGroups]}"
}
