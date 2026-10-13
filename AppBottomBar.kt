package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.Screen

@Composable
fun AppBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onOpenMoreMenu: () -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("main_bottom_nav")
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { onNavigate(Screen.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            modifier = Modifier.testTag("nav_dashboard")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.PROJECTS,
            onClick = { onNavigate(Screen.PROJECTS) },
            icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
            label = { Text("Projects") },
            modifier = Modifier.testTag("nav_projects")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.EDITOR,
            onClick = { onNavigate(Screen.EDITOR) },
            icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
            label = { Text("Editor") },
            modifier = Modifier.testTag("nav_editor")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.GIT || currentScreen == Screen.SYNC,
            onClick = { onNavigate(Screen.GIT) },
            icon = { Icon(Icons.Default.SyncAlt, contentDescription = "Git") },
            label = { Text("Git/Sync") },
            modifier = Modifier.testTag("nav_git")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.BUILD_APK,
            onClick = { onNavigate(Screen.BUILD_APK) },
            icon = { Icon(Icons.Default.Build, contentDescription = "Build APK") },
            label = { Text("Build") },
            modifier = Modifier.testTag("nav_build")
        )

        NavigationBarItem(
            selected = currentScreen in setOf(Screen.GITHUB, Screen.DOWNLOADS, Screen.ACTIONS, Screen.SETTINGS, Screen.ABOUT, Screen.FILE_MANAGER),
            onClick = onOpenMoreMenu,
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Menu") },
            label = { Text("More") },
            modifier = Modifier.testTag("nav_more")
        )
    }
}
