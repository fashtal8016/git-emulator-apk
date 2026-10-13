package com.example.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long = 0,
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "html_url") val htmlUrl: String = "",
    val name: String? = null,
    val bio: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    @Json(name = "total_private_repos") val totalPrivateRepos: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val private: Boolean = false,
    @Json(name = "html_url") val htmlUrl: String = "",
    val description: String? = null,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "forks_count") val forksCount: Int = 0,
    @Json(name = "clone_url") val cloneUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String = "",
    val private: Boolean = true,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitHubBranch(
    val name: String,
    val commit: BranchCommit
)

@JsonClass(generateAdapter = true)
data class BranchCommit(
    val sha: String,
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubCommitItem(
    val sha: String,
    val commit: CommitDetails,
    @Json(name = "html_url") val htmlUrl: String = "",
    val author: CommitAuthor? = null
)

@JsonClass(generateAdapter = true)
data class CommitDetails(
    val message: String,
    val author: CommitPersonDetails? = null
)

@JsonClass(generateAdapter = true)
data class CommitPersonDetails(
    val name: String = "",
    val email: String = "",
    val date: String = ""
)

@JsonClass(generateAdapter = true)
data class CommitAuthor(
    val login: String = "",
    @Json(name = "avatar_url") val avatarUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubTreeResponse(
    val sha: String,
    val tree: List<GitHubTreeItem> = emptyList(),
    val truncated: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitHubTreeItem(
    val path: String,
    val mode: String = "100644",
    val type: String = "blob", // blob or tree
    val sha: String? = null,
    val size: Long? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateTreeRequest(
    @Json(name = "base_tree") val baseTree: String? = null,
    val tree: List<CreateTreeEntry>
)

@JsonClass(generateAdapter = true)
data class CreateTreeEntry(
    val path: String,
    val mode: String = "100644",
    val type: String = "blob",
    val content: String
)

@JsonClass(generateAdapter = true)
data class CreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>
)

@JsonClass(generateAdapter = true)
data class CommitResponse(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String = "active"
)

@JsonClass(generateAdapter = true)
data class WorkflowsResponse(
    @Json(name = "total_count") val totalCount: Int,
    val workflows: List<GitHubWorkflow> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WorkflowRun(
    val id: Long,
    val name: String = "",
    @Json(name = "head_branch") val headBranch: String = "main",
    @Json(name = "head_sha") val headSha: String = "",
    val event: String = "workflow_dispatch",
    val status: String = "queued", // queued, in_progress, completed
    val conclusion: String? = null, // success, failure, cancelled
    @Json(name = "html_url") val htmlUrl: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "run_number") val runNumber: Int = 0
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArtifactItem(
    val id: Long,
    val name: String,
    @Json(name = "size_in_bytes") val sizeInBytes: Long = 0,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String = "",
    val expired: Boolean = false,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponse(
    @Json(name = "total_count") val totalCount: Int,
    val artifacts: List<ArtifactItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RateLimitResponse(
    val resources: RateLimitResources
)

@JsonClass(generateAdapter = true)
data class RateLimitResources(
    val core: RateLimitInfo
)

@JsonClass(generateAdapter = true)
data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val reset: Long,
    val used: Int
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchRequest(
    val ref: String = "main",
    val inputs: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class DeviceCodeResponse(
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "user_code") val userCode: String,
    @Json(name = "verification_uri") val verificationUri: String,
    @Json(name = "expires_in") val expiresIn: Int = 900,
    val interval: Int = 5
)

@JsonClass(generateAdapter = true)
data class DeviceTokenResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)
