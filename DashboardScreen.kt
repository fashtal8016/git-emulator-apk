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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.network.GitHubConnectionState
import com.example.core.network.GitHubUser
import com.example.core.network.RateLimitInfo
import com.example.core.storage.BuildHistoryEntity
import com.example.core.storage.ProjectEntity
import com.example.ui.Screen
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgeIndigo
import com.example.ui.theme.ForgeRose
import com.example.ui.theme.ForgeViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    activeProject: ProjectEntity?,
    projects: List<ProjectEntity>,
    connectionState: GitHubConnectionState,
    currentUser: GitHubUser?,
    rateLimit: RateLimitInfo?,
    buildHistory: List<BuildHistoryEntity>,
    onNavigate: (Screen) -> Unit,
    onSelectProject: (ProjectEntity) -> Unit,
    onQuickBuild: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "KotlinForge AI IDE",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ForgeCyan
                            )
                            Text(
                                text = "Mobile Development & Cloud Build Station",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ForgeCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = ForgeCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.BUILD_APK) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_dashboard_build")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Build APK", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onNavigate(Screen.EDITOR) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_dashboard_editor")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Code Editor")
                        }
                    }
                }
            }
        }

        // 2. GitHub Connection Hub Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Screen.GITHUB) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentUser != null && currentUser.avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = currentUser.avatarUrl,
                            contentDescription = "GitHub Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = ForgeCyan)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentUser != null) currentUser.login else "GitHub Not Connected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val isConnected = connectionState is GitHubConnectionState.Connected
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isConnected) ForgeEmerald.copy(alpha = 0.2f) else ForgeRose.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isConnected) "CONNECTED" else "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isConnected) ForgeEmerald else ForgeRose,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (currentUser != null) {
                                "Public: ${currentUser.publicRepos} | Rate Limit: ${rateLimit?.remaining ?: "-"}/60"
                            } else {
                                "Tap to connect OAuth or Personal Access Token"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open GitHub",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // 3. Active Workspace Card
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
                        Text(
                            text = "Active Project",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Switch",
                            style = MaterialTheme.typography.labelMedium,
                            color = ForgeCyan,
                            modifier = Modifier.clickable { onNavigate(Screen.PROJECTS) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeProject != null) {
                        Text(
                            text = activeProject.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeProject.path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onNavigate(Screen.FILE_MANAGER) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Files", color = MaterialTheme.colorScheme.onSurface)
                            }

                            Button(
                                onClick = { onNavigate(Screen.GIT) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SyncAlt, contentDescription = null, tint = ForgeIndigo, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Git Sync", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    } else {
                        Text(
                            text = "No project selected. Open or import a project to start coding.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onNavigate(Screen.PROJECTS) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                        ) {
                            Text("Open Projects", color = Color.Black)
                        }
                    }
                }
            }
        }

        // 4. Quick Actions Grid
        item {
            Text(
                text = "Rapid Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickActionCard(
                    title = "Connect GitHub",
                    icon = Icons.Default.Link,
                    color = ForgeCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.GITHUB) }
                )
                QuickActionCard(
                    title = "Sync Git",
                    icon = Icons.Default.SyncAlt,
                    color = ForgeIndigo,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SYNC) }
                )
                QuickActionCard(
                    title = "Build APK",
                    icon = Icons.Default.Build,
                    color = ForgeEmerald,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.BUILD_APK) }
                )
                QuickActionCard(
                    title = "Downloads",
                    icon = Icons.Default.CloudDownload,
                    color = ForgeViolet,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.DOWNLOADS) }
                )
            }
        }

        // 5. Recent Builds
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Recent Build Runs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    color = ForgeCyan,
                    modifier = Modifier.clickable { onNavigate(Screen.BUILD_APK) }
                )
            }
        }

        if (buildHistory.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No builds triggered yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "KotlinForge uses GitHub Actions to build APKs in the cloud",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(buildHistory.take(3), key = { it.id }) { build ->
                BuildItemCard(build = build, onNavigate = onNavigate)
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BuildItemCard(
    build: BuildHistoryEntity,
    onNavigate: (Screen) -> Unit
) {
    val isSuccess = build.conclusion == "success"
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(build.createdAt))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(Screen.BUILD_APK) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) ForgeEmerald.copy(alpha = 0.2f) else ForgeRose.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Build,
                    contentDescription = null,
                    tint = if (isSuccess) ForgeEmerald else ForgeRose,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${build.projectName} #${build.runNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${build.repoOwner}/${build.repoName} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSuccess) ForgeEmerald.copy(alpha = 0.15f) else ForgeRose.copy(alpha = 0.15f)
            ) {
                Text(
                    text = build.conclusion?.uppercase() ?: build.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSuccess) ForgeEmerald else ForgeRose,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
