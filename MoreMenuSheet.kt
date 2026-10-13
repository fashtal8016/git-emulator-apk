package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.Screen
import com.example.ui.theme.ForgeCyan

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuSheet(
    onDismiss: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    val menuItems = listOf(
        MenuItem("Dashboard", Icons.Default.Dashboard, Screen.DASHBOARD),
        MenuItem("Projects", Icons.Default.Folder, Screen.PROJECTS),
        MenuItem("File Manager", Icons.Default.Folder, Screen.FILE_MANAGER),
        MenuItem("Code Editor", Icons.Default.Code, Screen.EDITOR),
        MenuItem("GitHub Hub", Icons.Default.Link, Screen.GITHUB),
        MenuItem("Git & Commits", Icons.Default.SyncAlt, Screen.GIT),
        MenuItem("Sync Engine", Icons.Default.Sync, Screen.SYNC),
        MenuItem("AI Assistant", Icons.Default.AutoAwesome, Screen.EDITOR),
        MenuItem("Build APK", Icons.Default.Build, Screen.BUILD_APK),
        MenuItem("Actions Center", Icons.Default.PlayArrow, Screen.ACTIONS),
        MenuItem("Build History", Icons.Default.History, Screen.BUILD_APK),
        MenuItem("Downloads", Icons.Default.CloudDownload, Screen.DOWNLOADS),
        MenuItem("Templates", Icons.AutoMirrored.Filled.MenuBook, Screen.PROJECTS),
        MenuItem("Backup & Export", Icons.Default.Archive, Screen.PROJECTS),
        MenuItem("Activity Logs", Icons.AutoMirrored.Filled.ReceiptLong, Screen.DASHBOARD),
        MenuItem("Security Hub", Icons.Default.Lock, Screen.SETTINGS),
        MenuItem("Settings", Icons.Default.Settings, Screen.SETTINGS),
        MenuItem("About IDE", Icons.Default.Info, Screen.ABOUT)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("sheet_more_menu")
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = "KotlinForge Navigation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select a development workspace or tool",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(380.dp)
            ) {
                items(menuItems) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                onNavigate(item.screen)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = ForgeCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
