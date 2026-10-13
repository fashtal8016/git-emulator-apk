package com.example.core.network

import android.content.Context
import com.example.core.storage.AppPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GitHubRepository(
    private val context: Context,
    private val appPreferences: AppPreferences
) {
    private val _connectionState = MutableStateFlow<GitHubConnectionState>(GitHubConnectionState.Disconnected)
    val connectionState: StateFlow<GitHubConnectionState> = _connectionState.asStateFlow()

    private val _currentUser = MutableStateFlow<GitHubUser?>(null)
    val currentUser: StateFlow<GitHubUser?> = _currentUser.asStateFlow()

    private val _rateLimit = MutableStateFlow<RateLimitInfo?>(null)
    val rateLimit: StateFlow<RateLimitInfo?> = _rateLimit.asStateFlow()

    private var apiService: GitHubApiService? = null

    init {
        val savedToken = appPreferences.getGitHubToken()
        if (savedToken.isNotEmpty()) {
            setupApiClient(savedToken)
        }
    }

    private fun setupApiClient(token: String) {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "KotlinForge-AI-IDE-Android")

            if (token.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        apiService = retrofit.create(GitHubApiService::class.java)
    }

    suspend fun testAndSaveToken(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        setupApiClient(token)
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("API Service not initialized"))

        _connectionState.value = GitHubConnectionState.Connecting
        try {
            val response = service.getAuthenticatedUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                appPreferences.saveGitHubToken(token)
                appPreferences.saveGitHubUserInfo(user.login, user.avatarUrl)
                _currentUser.value = user
                _connectionState.value = GitHubConnectionState.Connected(user)
                fetchRateLimit()
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Authentication failed (${response.code()})"
                _connectionState.value = GitHubConnectionState.Error(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Network error during GitHub authentication"
            _connectionState.value = GitHubConnectionState.Error(errorMsg)
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        appPreferences.clearGitHubToken()
        _currentUser.value = null
        _connectionState.value = GitHubConnectionState.Disconnected
        apiService = null
    }

    suspend fun fetchCurrentUser(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.getAuthenticatedUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                _currentUser.value = user
                _connectionState.value = GitHubConnectionState.Connected(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRateLimit(): Result<RateLimitInfo> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.getRateLimit()
            if (response.isSuccessful && response.body() != null) {
                val info = response.body()!!.resources.core
                _rateLimit.value = info
                Result.success(info)
            } else {
                Result.failure(Exception("Failed to fetch rate limit"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listRepositories(): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.listUserRepos()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error fetching repositories: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRepository(name: String, description: String, isPrivate: Boolean): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val request = CreateRepoRequest(name = name, description = description, private = isPrivate, autoInit = true)
            val response = service.createRepo(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: "Failed to create repo (${response.code()})"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBranches(owner: String, repo: String): Result<List<GitHubBranch>> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.listBranches(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch branches: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listCommits(owner: String, repo: String, branch: String? = null): Result<List<GitHubCommitItem>> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.listCommits(owner, repo, branch)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to list commits: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically commit multiple files to GitHub using the Git Data API (Tree + Commit + Ref Update).
     * Prevents cluttering repository history with multi-commit spam.
     */
    suspend fun commitFilesAtomic(
        owner: String,
        repo: String,
        branch: String,
        commitMessage: String,
        files: Map<String, String> // relative path -> file text content
    ): Result<String> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            // 1. Get current branch head commit SHA
            val branchesRes = service.listBranches(owner, repo)
            if (!branchesRes.isSuccessful || branchesRes.body() == null) {
                return@withContext Result.failure(Exception("Could not read branch reference: ${branchesRes.code()}"))
            }
            val targetBranch = branchesRes.body()!!.find { it.name == branch }
                ?: return@withContext Result.failure(Exception("Branch '$branch' not found in repository"))
            val parentCommitSha = targetBranch.commit.sha

            // 2. Create Git tree entries
            val treeEntries = files.map { (path, content) ->
                CreateTreeEntry(
                    path = path,
                    mode = "100644",
                    type = "blob",
                    content = content
                )
            }

            val treeRes = service.createTree(
                owner,
                repo,
                CreateTreeRequest(baseTree = parentCommitSha, tree = treeEntries)
            )
            if (!treeRes.isSuccessful || treeRes.body() == null) {
                return@withContext Result.failure(Exception("Failed to create Git tree: ${treeRes.errorBody()?.string()}"))
            }
            val newTreeSha = treeRes.body()!!.sha

            // 3. Create commit
            val commitRes = service.createCommit(
                owner,
                repo,
                CreateCommitRequest(
                    message = commitMessage,
                    tree = newTreeSha,
                    parents = listOf(parentCommitSha)
                )
            )
            if (!commitRes.isSuccessful || commitRes.body() == null) {
                return@withContext Result.failure(Exception("Failed to create commit: ${commitRes.errorBody()?.string()}"))
            }
            val newCommitSha = commitRes.body()!!.sha

            // 4. Update branch ref
            val refRes = service.updateRef(
                owner,
                repo,
                branch,
                UpdateRefRequest(sha = newCommitSha, force = false)
            )
            if (!refRes.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to update branch ref: ${refRes.errorBody()?.string()}"))
            }

            Result.success(newCommitSha)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dispatch an Android build workflow on GitHub Actions.
     */
    suspend fun triggerWorkflow(
        owner: String,
        repo: String,
        workflowFileName: String = "android-build.yml",
        ref: String = "main",
        buildType: String = "debug"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.dispatchWorkflow(
                owner = owner,
                repo = repo,
                workflowId = workflowFileName,
                request = WorkflowDispatchRequest(
                    ref = ref,
                    inputs = mapOf("build_type" to buildType)
                )
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Workflow dispatch failed: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listWorkflowRuns(owner: String, repo: String): Result<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.listWorkflowRuns(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.workflowRuns)
            } else {
                Result.failure(Exception("Failed to list workflow runs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRun(owner: String, repo: String, runId: Long): Result<WorkflowRun> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.getWorkflowRun(owner, repo, runId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get workflow run: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listRunArtifacts(owner: String, repo: String, runId: Long): Result<List<ArtifactItem>> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response = service.listRunArtifacts(owner, repo, runId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.artifacts)
            } else {
                Result.failure(Exception("Failed to list artifacts: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download artifact ZIP file directly to the given destination file with byte progress callbacks.
     */
    suspend fun downloadArtifactZip(
        owner: String,
        repo: String,
        artifactId: Long,
        destinationFile: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val response: Response<ResponseBody> = service.downloadArtifact(owner, repo, artifactId)
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(Exception("Failed to download artifact: HTTP ${response.code()}"))
            }

            destinationFile.parentFile?.mkdirs()
            val body = response.body()!!
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Long = 0
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        onProgress?.invoke(bytesRead, totalBytes)
                    }
                }
            }
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

sealed class GitHubConnectionState {
    object Disconnected : GitHubConnectionState()
    object Connecting : GitHubConnectionState()
    data class Connected(val user: GitHubUser) : GitHubConnectionState()
    data class Error(val message: String) : GitHubConnectionState()
}
