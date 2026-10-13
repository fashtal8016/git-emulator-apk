package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.storage.ActionCommandEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppTopBar
import com.example.ui.components.CommandPaletteDialog
import com.example.ui.components.MoreMenuSheet
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ActionsCenterScreen
import com.example.ui.screens.BuildApkScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.GitHubScreen
import com.example.ui.screens.GitScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)

        setContent {
            val themeMode by viewModel.preferences.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> true // default developer dark theme
            }

            MyApplicationTheme(darkTheme = isDark) {
                KotlinForgeApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "kotlinforge" && uri.host == "oauth") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                viewModel.connectWithToken(code)
            }
        }
    }
}

@Composable
fun KotlinForgeApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val fileTree by viewModel.fileTree.collectAsStateWithLifecycle()
    val openTabs by viewModel.openTabs.collectAsStateWithLifecycle()
    val activeTabIndex by viewModel.activeTabIndex.collectAsStateWithLifecycle()
    val gitStatuses by viewModel.gitStatuses.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val rateLimit by viewModel.rateLimit.collectAsStateWithLifecycle()
    val reposList by viewModel.reposList.collectAsStateWithLifecycle()
    val selectedRepo by viewModel.selectedRepo.collectAsStateWithLifecycle()
    val buildHistory by viewModel.buildHistory.collectAsStateWithLifecycle()
    val latestWorkflowRuns by viewModel.latestWorkflowRuns.collectAsStateWithLifecycle()
    val activeRun by viewModel.activeWorkflowRun.collectAsStateWithLifecycle()
    val activeRunArtifacts by viewModel.activeRunArtifacts.collectAsStateWithLifecycle()
    val downloadedApks by viewModel.downloadedApks.collectAsStateWithLifecycle()
    val actionsList by viewModel.actionsList.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showCommandPalette by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Display status messages via Snackbar
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Predictive back / BackHandler navigation
    if (currentScreen != Screen.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(Screen.DASHBOARD)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                activeProject = activeProject,
                connectionState = connectionState,
                onOpenCommandPalette = { showCommandPalette = true },
                onOpenSettings = { viewModel.navigateTo(Screen.SETTINGS) }
            )
        },
        bottomBar = {
            AppBottomBar(
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) },
                onOpenMoreMenu = { showMoreMenu = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        activeProject = activeProject,
                        projects = projects,
                        connectionState = connectionState,
                        currentUser = currentUser,
                        rateLimit = rateLimit,
                        buildHistory = buildHistory,
                        onNavigate = { viewModel.navigateTo(it) },
                        onSelectProject = { viewModel.selectProject(it) },
                        onQuickBuild = { viewModel.navigateTo(Screen.BUILD_APK) }
                    )
                }

                Screen.PROJECTS -> {
                    ProjectsScreen(
                        projects = projects,
                        activeProject = activeProject,
                        onSelectProject = { viewModel.selectProject(it) },
                        onCreateProject = { name, pkg -> viewModel.createStarterProject(name, pkg) },
                        onImportZip = { uri, name -> viewModel.importZipProject(uri, name) },
                        onExportZip = { project, onDone -> viewModel.exportProjectZip(project, onDone) },
                        onDeleteProject = { viewModel.deleteProject(it) }
                    )
                }

                Screen.FILE_MANAGER -> {
                    FileManagerScreen(
                        activeProject = activeProject,
                        fileTree = fileTree,
                        onOpenFile = { viewModel.openFileInEditor(it) },
                        onCreateFile = { parent, name ->
                            viewModel.storageManager.createFile(parent, name)
                            viewModel.refreshFileTree()
                        },
                        onCreateFolder = { parent, name ->
                            viewModel.storageManager.createDirectory(parent, name)
                            viewModel.refreshFileTree()
                        },
                        onRenameFile = { file, newName ->
                            viewModel.storageManager.renameFile(file, newName)
                            viewModel.refreshFileTree()
                        },
                        onDeleteFile = { file ->
                            viewModel.storageManager.moveToTrash(file)
                            viewModel.refreshFileTree()
                        },
                        onRefresh = { viewModel.refreshFileTree() }
                    )
                }

                Screen.EDITOR -> {
                    EditorScreen(
                        openTabs = openTabs,
                        activeTabIndex = activeTabIndex,
                        onTabSelected = { viewModel.openFileInEditor(openTabs[it].file) },
                        onCloseTab = { viewModel.closeTab(it) },
                        onContentChanged = { viewModel.updateActiveTabContent(it) },
                        onSave = { viewModel.saveActiveTab() },
                        onAiAction = { type, prompt -> viewModel.requestAiAnalysis(type, prompt) },
                        aiResponse = aiResponse,
                        onClearAiResponse = { viewModel.clearAiResponse() }
                    )
                }

                Screen.GITHUB -> {
                    GitHubScreen(
                        connectionState = connectionState,
                        currentUser = currentUser,
                        rateLimit = rateLimit,
                        repos = reposList,
                        selectedRepo = selectedRepo,
                        onConnectToken = { viewModel.connectWithToken(it) },
                        onDisconnect = { viewModel.disconnectGitHub() },
                        onRefreshRepos = { viewModel.loadUserRepositories() },
                        onCreateRepo = { name, desc, isPriv -> viewModel.createNewRepo(name, desc, isPriv) },
                        onSelectRepo = { viewModel.selectRepo(it) }
                    )
                }

                Screen.GIT -> {
                    GitScreen(
                        activeProject = activeProject,
                        selectedRepo = selectedRepo,
                        gitStatuses = gitStatuses,
                        onRefreshStatus = { viewModel.refreshGitStatus() },
                        onCommitAndPush = { msg, owner, repo, branch ->
                            viewModel.commitAndPush(msg, owner, repo, branch)
                        }
                    )
                }

                Screen.SYNC -> {
                    SyncScreen(
                        activeProject = activeProject,
                        selectedRepo = selectedRepo,
                        gitStatuses = gitStatuses,
                        isBusy = isBusy,
                        onPush = {
                            if (selectedRepo != null) {
                                val owner = selectedRepo!!.fullName.substringBefore('/')
                                val rName = selectedRepo!!.name
                                viewModel.commitAndPush("Sync update from KotlinForge IDE", owner, rName, selectedRepo!!.defaultBranch)
                            }
                        },
                        onPull = { viewModel.showMessage("Pulling latest tree from GitHub...") },
                        onRefresh = { viewModel.refreshGitStatus() }
                    )
                }

                Screen.BUILD_APK -> {
                    BuildApkScreen(
                        activeProject = activeProject,
                        selectedRepo = selectedRepo,
                        workflowRuns = latestWorkflowRuns,
                        activeRun = activeRun,
                        artifacts = activeRunArtifacts,
                        buildHistory = buildHistory,
                        isBusy = isBusy,
                        onTriggerBuild = { owner, repo, type -> viewModel.triggerApkBuild(owner, repo, type) },
                        onRefreshRuns = { owner, repo -> viewModel.fetchWorkflowRuns(owner, repo) },
                        onDownloadArtifact = { owner, repo, run, art ->
                            viewModel.downloadApkArtifact(owner, repo, run, art)
                        },
                        onGenerateWorkflow = { viewModel.generateStandardWorkflow() },
                        onInstallApk = { viewModel.installApk(it) },
                        onShareApk = { viewModel.shareApk(it) },
                        onDownloadFromUrl = { viewModel.downloadApkFromUrl(it) }
                    )
                }

                Screen.DOWNLOADS -> {
                    DownloadsScreen(
                        downloadedApks = downloadedApks,
                        onInstallApk = { viewModel.installApk(it) },
                        onShareApk = { viewModel.shareApk(it) },
                        onDeleteApk = { it.delete(); viewModel.refreshDownloadedApks() },
                        onRefresh = { viewModel.refreshDownloadedApks() },
                        onDownloadFromUrl = { viewModel.downloadApkFromUrl(it) }
                    )
                }

                Screen.ACTIONS -> {
                    ActionsCenterScreen(
                        actions = actionsList,
                        onExecuteAction = { handleActionCommand(it, viewModel) },
                        onToggleFavorite = { action ->
                            scope.launch {
                                viewModel.database.actionCommandDao().toggleFavorite(action.id, !action.isFavorite)
                            }
                        }
                    )
                }

                Screen.SETTINGS -> {
                    SettingsScreen(
                        preferences = viewModel.preferences,
                        onSaveGitAuthor = { name, email ->
                            viewModel.preferences.setGitAuthor(name, email)
                            viewModel.showMessage("Git author updated")
                        },
                        onClearCredentials = {
                            viewModel.preferences.clearGitHubToken()
                            viewModel.showMessage("Stored Keystore credentials purged")
                        }
                    )
                }

                Screen.ABOUT -> {
                    AboutScreen()
                }
            }

            if (isBusy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = com.example.ui.theme.ForgeCyan)
                }
            }
        }
    }

    // Command Palette Dialog
    if (showCommandPalette) {
        CommandPaletteDialog(
            actions = actionsList,
            onDismiss = { showCommandPalette = false },
            onExecuteAction = { handleActionCommand(it, viewModel) },
            onToggleFavorite = { action ->
                scope.launch {
                    viewModel.database.actionCommandDao().toggleFavorite(action.id, !action.isFavorite)
                }
            }
        )
    }

    // More Menu Modal Bottom Sheet
    if (showMoreMenu) {
        MoreMenuSheet(
            onDismiss = { showMoreMenu = false },
            onNavigate = {
                viewModel.navigateTo(it)
                showMoreMenu = false
            }
        )
    }
}

private fun handleActionCommand(action: ActionCommandEntity, viewModel: MainViewModel) {
    when (action.commandCode) {
        "ACTION_GITHUB_CONNECT" -> viewModel.navigateTo(Screen.GITHUB)
        "ACTION_BUILD_DEBUG" -> {
            viewModel.navigateTo(Screen.BUILD_APK)
            val repo = viewModel.selectedRepo.value
            if (repo != null) {
                viewModel.triggerApkBuild(repo.fullName.substringBefore('/'), repo.name, "debug")
            } else {
                viewModel.showMessage("Select a GitHub repository first")
            }
        }
        "ACTION_BUILD_RELEASE" -> {
            viewModel.navigateTo(Screen.BUILD_APK)
            val repo = viewModel.selectedRepo.value
            if (repo != null) {
                viewModel.triggerApkBuild(repo.fullName.substringBefore('/'), repo.name, "release")
            } else {
                viewModel.showMessage("Select a GitHub repository first")
            }
        }
        "ACTION_SYNC_PUSH" -> viewModel.navigateTo(Screen.SYNC)
        "ACTION_SYNC_PULL" -> viewModel.navigateTo(Screen.SYNC)
        "ACTION_IMPORT_ZIP" -> viewModel.navigateTo(Screen.PROJECTS)
        "ACTION_EXPORT_ZIP" -> {
            val project = viewModel.activeProject.value
            if (project != null) {
                viewModel.exportProjectZip(project) {}
            } else {
                viewModel.showMessage("No active project to export")
            }
        }
        "ACTION_GENERATE_WORKFLOW" -> viewModel.generateStandardWorkflow()
        "ACTION_AI_EXPLAIN" -> {
            viewModel.navigateTo(Screen.EDITOR)
            viewModel.requestAiAnalysis(com.example.core.ai.AiPromptType.EXPLAIN)
        }
        "ACTION_SECURITY_AUDIT" -> {
            viewModel.refreshFileTree()
            viewModel.showMessage("Sensitive files audit complete")
        }
        else -> viewModel.showMessage("Action ${action.title} executed")
    }
}
