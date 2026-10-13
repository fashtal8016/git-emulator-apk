package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.AiAssistant
import com.example.core.ai.AiPromptType
import com.example.core.build.ApkDownloadManager
import com.example.core.build.ApkResult
import com.example.core.git.GitEngine
import com.example.core.git.GitFileStatus
import com.example.core.network.ArtifactItem
import com.example.core.network.GitHubBranch
import com.example.core.network.GitHubConnectionState
import com.example.core.network.GitHubRepo
import com.example.core.network.GitHubRepository
import com.example.core.network.WorkflowRun
import com.example.core.storage.ActionCommandEntity
import com.example.core.storage.AppDatabase
import com.example.core.storage.AppPreferences
import com.example.core.storage.BuildHistoryEntity
import com.example.core.storage.FileItem
import com.example.core.storage.ProjectEntity
import com.example.core.storage.ProjectStorageManager
import com.example.core.storage.ProjectValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class EditorTab(
    val file: File,
    val title: String,
    val content: String,
    val originalContent: String,
    val isDirty: Boolean = false
)

enum class Screen {
    DASHBOARD,
    PROJECTS,
    FILE_MANAGER,
    EDITOR,
    GITHUB,
    GIT,
    SYNC,
    BUILD_APK,
    DOWNLOADS,
    ACTIONS,
    SETTINGS,
    ABOUT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val preferences = AppPreferences(application)
    val storageManager = ProjectStorageManager(application)
    val gitHubRepository = GitHubRepository(application, preferences)
    val apkDownloadManager = ApkDownloadManager(application, gitHubRepository, database)
    val aiAssistant = AiAssistant(preferences)

    // Navigation
    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Status & Feedback
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Projects
    val projects: StateFlow<List<ProjectEntity>> = database.projectDao().getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    private val _activeProjectDir = MutableStateFlow<File?>(null)
    val activeProjectDir: StateFlow<File?> = _activeProjectDir.asStateFlow()

    private val _fileTree = MutableStateFlow<List<FileItem>>(emptyList())
    val fileTree: StateFlow<List<FileItem>> = _fileTree.asStateFlow()

    // Editor Tabs
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // Git Status
    private val _gitStatuses = MutableStateFlow<List<GitFileStatus>>(emptyList())
    val gitStatuses: StateFlow<List<GitFileStatus>> = _gitStatuses.asStateFlow()

    // GitHub
    val connectionState = gitHubRepository.connectionState
    val currentUser = gitHubRepository.currentUser
    val rateLimit = gitHubRepository.rateLimit

    private val _reposList = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val reposList: StateFlow<List<GitHubRepo>> = _reposList.asStateFlow()

    private val _branchesList = MutableStateFlow<List<GitHubBranch>>(emptyList())
    val branchesList: StateFlow<List<GitHubBranch>> = _branchesList.asStateFlow()

    private val _selectedRepo = MutableStateFlow<GitHubRepo?>(null)
    val selectedRepo: StateFlow<GitHubRepo?> = _selectedRepo.asStateFlow()

    // Build & Workflow
    val buildHistory: StateFlow<List<BuildHistoryEntity>> = database.buildHistoryDao().getAllBuilds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _latestWorkflowRuns = MutableStateFlow<List<WorkflowRun>>(emptyList())
    val latestWorkflowRuns: StateFlow<List<WorkflowRun>> = _latestWorkflowRuns.asStateFlow()

    private val _activeRunArtifacts = MutableStateFlow<List<ArtifactItem>>(emptyList())
    val activeRunArtifacts: StateFlow<List<ArtifactItem>> = _activeRunArtifacts.asStateFlow()

    private val _activeWorkflowRun = MutableStateFlow<WorkflowRun?>(null)
    val activeWorkflowRun: StateFlow<WorkflowRun?> = _activeWorkflowRun.asStateFlow()

    // Downloads
    private val _downloadedApks = MutableStateFlow<List<File>>(emptyList())
    val downloadedApks: StateFlow<List<File>> = _downloadedApks.asStateFlow()

    // Actions
    val actionsList: StateFlow<List<ActionCommandEntity>> = database.actionCommandDao().getAllActions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI result
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    init {
        refreshDownloadedApks()
        // If there are no projects, create an initial sample project
        viewModelScope.launch {
            delay(300)
            val currentProjects = database.projectDao().getProjectById(1L)
            if (currentProjects == null) {
                createStarterProject("MyFirstApp", "com.example.myfirstapp")
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    // --- Projects Management ---

    fun selectProject(project: ProjectEntity) {
        _activeProject.value = project
        val dir = File(project.path)
        _activeProjectDir.value = dir
        preferences.setActiveProjectId(project.id)
        refreshFileTree()
        refreshGitStatus()
    }

    fun createStarterProject(name: String, pkg: String = "com.example.app") {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            try {
                val dir = storageManager.createStarterProject(name, pkg)
                val entity = ProjectEntity(
                    name = name,
                    path = dir.absolutePath,
                    description = "Android Jetpack Compose starter project with CI/CD",
                    isGitInitialized = true
                )
                val id = database.projectDao().insertProject(entity)
                val created = entity.copy(id = id)
                withContext(Dispatchers.Main) {
                    selectProject(created)
                    showMessage("Project '$name' created successfully!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("Error creating project: ${e.message}")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun importZipProject(uri: Uri, projectName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            try {
                val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    showMessage("Failed to open selected ZIP file")
                    return@launch
                }
                val destDir = storageManager.createProjectFolder(projectName)
                storageManager.extractZip(inputStream, destDir)
                val entity = ProjectEntity(
                    name = projectName,
                    path = destDir.absolutePath,
                    description = "Imported from ZIP archive"
                )
                val id = database.projectDao().insertProject(entity)
                withContext(Dispatchers.Main) {
                    selectProject(entity.copy(id = id))
                    showMessage("ZIP Project '$projectName' imported successfully!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("ZIP Import failed: ${e.message}")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun exportProjectZip(project: ProjectEntity, onReady: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            try {
                val src = File(project.path)
                val outZip = File(storageManager.getDownloadsDir(), "${project.name}_export.zip")
                storageManager.exportToZip(src, outZip)
                withContext(Dispatchers.Main) {
                    showMessage("Project exported to ${outZip.name}")
                    onReady(outZip)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("Export failed: ${e.message}")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.projectDao().deleteProject(project)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
                _activeProjectDir.value = null
                _openTabs.value = emptyList()
            }
            withContext(Dispatchers.Main) {
                showMessage("Project removed.")
            }
        }
    }

    fun refreshFileTree() {
        val dir = _activeProjectDir.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val tree = storageManager.getFileTree(dir)
            _fileTree.value = tree
        }
    }

    // --- Editor Tabs ---

    fun openFileInEditor(file: File) {
        val current = _openTabs.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex >= 0) {
            _activeTabIndex.value = existingIndex
        } else {
            val content = storageManager.readFileContent(file)
            current.add(
                EditorTab(
                    file = file,
                    title = file.name,
                    content = content,
                    originalContent = content,
                    isDirty = false
                )
            )
            _openTabs.value = current
            _activeTabIndex.value = current.size - 1
        }
        navigateTo(Screen.EDITOR)
    }

    fun updateActiveTabContent(newContent: String) {
        val idx = _activeTabIndex.value
        val current = _openTabs.value.toMutableList()
        if (idx in current.indices) {
            val tab = current[idx]
            current[idx] = tab.copy(
                content = newContent,
                isDirty = newContent != tab.originalContent
            )
            _openTabs.value = current
        }
    }

    fun saveActiveTab() {
        val idx = _activeTabIndex.value
        val current = _openTabs.value.toMutableList()
        if (idx in current.indices) {
            val tab = current[idx]
            storageManager.writeFileContent(tab.file, tab.content)
            current[idx] = tab.copy(
                originalContent = tab.content,
                isDirty = false
            )
            _openTabs.value = current
            showMessage("Saved ${tab.title}")
            refreshGitStatus()
        }
    }

    fun closeTab(index: Int) {
        val current = _openTabs.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _openTabs.value = current
            if (_activeTabIndex.value >= current.size) {
                _activeTabIndex.value = maxOf(0, current.size - 1)
            }
        }
    }

    // --- Git & Sync ---

    fun refreshGitStatus() {
        val dir = _activeProjectDir.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val statuses = GitEngine.computeWorkingTreeChanges(dir)
            _gitStatuses.value = statuses
        }
    }

    fun commitAndPush(commitMessage: String, owner: String, repo: String, branch: String) {
        val dir = _activeProjectDir.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            try {
                // Collect modified and added files
                val filesToCommit = mutableMapOf<String, String>()
                dir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relPath = file.relativeTo(dir).path.replace('\\', '/')
                    if (!relPath.startsWith(".git") && !relPath.startsWith("build") && !relPath.startsWith(".gradle")) {
                        filesToCommit[relPath] = file.readText(Charsets.UTF_8)
                    }
                }

                val result = gitHubRepository.commitFilesAtomic(
                    owner = owner,
                    repo = repo,
                    branch = branch,
                    commitMessage = commitMessage,
                    files = filesToCommit
                )

                if (result.isSuccess) {
                    withContext(Dispatchers.Main) {
                        showMessage("Successfully pushed commit ${result.getOrNull()?.take(7)} to $owner/$repo ($branch)")
                        refreshGitStatus()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showMessage("Push failed: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("Error during sync: ${e.message}")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- GitHub Connection & Repos ---

    fun connectWithToken(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            val res = gitHubRepository.testAndSaveToken(token.trim())
            withContext(Dispatchers.Main) {
                _isBusy.value = false
                if (res.isSuccess) {
                    showMessage("Connected as ${res.getOrNull()?.login}!")
                    loadUserRepositories()
                } else {
                    showMessage("Connection error: ${res.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun disconnectGitHub() {
        viewModelScope.launch(Dispatchers.IO) {
            gitHubRepository.disconnect()
            _reposList.value = emptyList()
            _selectedRepo.value = null
            withContext(Dispatchers.Main) {
                showMessage("Disconnected from GitHub")
            }
        }
    }

    fun loadUserRepositories() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = gitHubRepository.listRepositories()
            if (res.isSuccess) {
                _reposList.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    fun createNewRepo(name: String, desc: String, isPrivate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            val res = gitHubRepository.createRepository(name, desc, isPrivate)
            withContext(Dispatchers.Main) {
                _isBusy.value = false
                if (res.isSuccess) {
                    val repo = res.getOrNull()!!
                    showMessage("Created repository ${repo.fullName}")
                    loadUserRepositories()
                    _selectedRepo.value = repo
                } else {
                    showMessage("Failed to create repo: ${res.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun selectRepo(repo: GitHubRepo) {
        _selectedRepo.value = repo
        viewModelScope.launch(Dispatchers.IO) {
            val owner = repo.fullName.substringBefore('/')
            val repoName = repo.name
            val branchesRes = gitHubRepository.listBranches(owner, repoName)
            if (branchesRes.isSuccess) {
                _branchesList.value = branchesRes.getOrNull() ?: emptyList()
            }
            fetchWorkflowRuns(owner, repoName)
        }
    }

    // --- Build APK via GitHub Actions ---

    fun triggerApkBuild(owner: String, repo: String, buildType: String = "debug") {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            showMessage("Triggering GitHub Actions $buildType build...")
            try {
                val res = gitHubRepository.triggerWorkflow(
                    owner = owner,
                    repo = repo,
                    workflowFileName = "android-build.yml",
                    buildType = buildType
                )
                if (res.isSuccess) {
                    withContext(Dispatchers.Main) {
                        showMessage("Build workflow queued! Polling status...")
                    }
                    delay(3000)
                    fetchWorkflowRuns(owner, repo)
                } else {
                    withContext(Dispatchers.Main) {
                        showMessage("Dispatch error: ${res.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("Build error: ${e.message}")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun fetchWorkflowRuns(owner: String, repo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val runsRes = gitHubRepository.listWorkflowRuns(owner, repo)
            if (runsRes.isSuccess) {
                val runs = runsRes.getOrNull() ?: emptyList()
                _latestWorkflowRuns.value = runs
                if (runs.isNotEmpty()) {
                    val latest = runs.first()
                    _activeWorkflowRun.value = latest
                    fetchRunArtifacts(owner, repo, latest.id)
                }
            }
        }
    }

    fun fetchRunArtifacts(owner: String, repo: String, runId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val artRes = gitHubRepository.listRunArtifacts(owner, repo, runId)
            if (artRes.isSuccess) {
                _activeRunArtifacts.value = artRes.getOrNull() ?: emptyList()
            }
        }
    }

    fun downloadApkArtifact(owner: String, repo: String, run: WorkflowRun, artifact: ArtifactItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            showMessage("Downloading APK artifact (${apkDownloadManager.formatFileSize(artifact.sizeInBytes)})...")
            val pName = _activeProject.value?.name ?: "App"
            val res = apkDownloadManager.downloadAndProcessApk(
                owner = owner,
                repo = repo,
                runId = run.id,
                runNumber = run.runNumber,
                artifact = artifact,
                projectName = pName
            )
            withContext(Dispatchers.Main) {
                _isBusy.value = false
                if (res.isSuccess) {
                    val apk = res.getOrNull()!!
                    showMessage("APK downloaded successfully! SHA-256: ${apk.sha256.take(8)}...")
                    refreshDownloadedApks()
                } else {
                    showMessage("Download failed: ${res.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun downloadApkFromUrl(url: String, customName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            showMessage("Downloading APK from $url...")
            val res = apkDownloadManager.downloadApkFromDirectUrl(url, customName)
            withContext(Dispatchers.Main) {
                _isBusy.value = false
                if (res.isSuccess) {
                    val apk = res.getOrNull()!!
                    showMessage("Downloaded: ${apk.fileName} (SHA-256: ${apk.sha256.take(8)}...)")
                    refreshDownloadedApks()
                } else {
                    showMessage("Download error: ${res.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun refreshDownloadedApks() {
        _downloadedApks.value = apkDownloadManager.getDownloadedApks()
    }

    fun installApk(file: File) {
        apkDownloadManager.installApk(file)
    }

    fun shareApk(file: File) {
        apkDownloadManager.shareApk(file)
    }

    // --- AI Assistance ---

    fun requestAiAnalysis(type: AiPromptType, customPrompt: String = "") {
        val idx = _activeTabIndex.value
        val tab = _openTabs.value.getOrNull(idx) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            showMessage("KotlinForge AI analyzing ${tab.title}...")
            val result = aiAssistant.analyzeCode(
                promptType = type,
                code = tab.content,
                filePath = tab.file.name,
                customInstruction = customPrompt
            )
            withContext(Dispatchers.Main) {
                _isBusy.value = false
                _aiResponse.value = result.getOrNull()
            }
        }
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }

    fun generateStandardWorkflow() {
        val dir = _activeProjectDir.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val wfDir = File(dir, ".github/workflows").apply { mkdirs() }
            val wfFile = File(wfDir, "android-build.yml")
            wfFile.writeText(storageManager.getStandardWorkflowContent())
            refreshFileTree()
            withContext(Dispatchers.Main) {
                showMessage("Generated .github/workflows/android-build.yml")
            }
        }
    }
}
