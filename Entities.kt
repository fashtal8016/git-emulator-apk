package com.example.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String,
    val description: String = "",
    val gitRemoteUrl: String = "",
    val defaultBranch: String = "main",
    val selectedRepoFullName: String = "",
    val isGitInitialized: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "build_history")
data class BuildHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0,
    val projectName: String = "",
    val repoOwner: String = "",
    val repoName: String = "",
    val runId: Long = 0,
    val runNumber: Int = 0,
    val workflowName: String = "Android Build & Test",
    val branch: String = "main",
    val status: String = "queued", // queued, in_progress, completed
    val conclusion: String? = null, // success, failure, cancelled
    val htmlUrl: String = "",
    val apkPath: String = "",
    val artifactName: String = "",
    val artifactSize: Long = 0,
    val sha256: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "action_commands")
data class ActionCommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // GitHub, Git, Build, File, AI, System
    val commandCode: String,
    val isFavorite: Boolean = false,
    val orderIndex: Int = 0
)

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val extension: String = "",
    val isSensitive: Boolean = false,
    val children: List<FileItem> = emptyList()
)

object SensitiveFileDetector {
    private val SENSITIVE_EXTENSIONS = setOf("jks", "keystore", "p12", "pem", "key", "pk8", "env")
    private val SENSITIVE_FILENAMES = setOf(
        ".env",
        ".env.local",
        "google-services.json",
        "id_rsa",
        "id_ed25519",
        "my-upload-key.jks",
        "debug.keystore",
        "secrets.properties",
        "local.properties"
    )

    fun isSensitive(name: String): Boolean {
        val lower = name.lowercase()
        if (SENSITIVE_FILENAMES.contains(lower)) return true
        val ext = lower.substringAfterLast('.', "")
        return SENSITIVE_EXTENSIONS.contains(ext)
    }
}
