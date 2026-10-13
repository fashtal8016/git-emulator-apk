package com.example.core.git

import java.io.File
import java.security.MessageDigest

data class GitFileStatus(
    val relativePath: String,
    val file: File?,
    val status: StatusType,
    val localContent: String? = null,
    val remoteContent: String? = null,
    val isSensitive: Boolean = false
) {
    enum class StatusType {
        ADDED,
        MODIFIED,
        DELETED,
        UNMODIFIED,
        CONFLICT
    }
}

data class GitDiffLine(
    val type: LineType,
    val content: String,
    val oldLineNumber: Int? = null,
    val newLineNumber: Int? = null
) {
    enum class LineType {
        ADDED,
        REMOVED,
        UNCHANGED,
        HEADER
    }
}

object GitEngine {

    /**
     * Compute Git blob SHA-1 according to official Git object storage standard:
     * sha1("blob " + size + "\0" + content)
     */
    fun computeGitBlobSha1(bytes: ByteArray): String {
        val header = "blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-1")
        md.update(header)
        md.update(bytes)
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compare local project directory against remote file map to find added, modified and deleted files.
     */
    fun computeWorkingTreeChanges(
        projectDir: File,
        remoteFiles: Map<String, String> = emptyMap() // relative path -> remote SHA or remote content
    ): List<GitFileStatus> {
        val statuses = mutableListOf<GitFileStatus>()
        val localFilesMap = mutableMapOf<String, File>()

        projectDir.walkTopDown()
            .filter { it.isFile && !shouldIgnore(it, projectDir) }
            .forEach { file ->
                val relPath = file.relativeTo(projectDir).path.replace('\\', '/')
                localFilesMap[relPath] = file
            }

        // Check local files
        localFilesMap.forEach { (relPath, file) ->
            val remoteContent = remoteFiles[relPath]
            if (remoteContent == null) {
                statuses.add(
                    GitFileStatus(
                        relativePath = relPath,
                        file = file,
                        status = GitFileStatus.StatusType.ADDED,
                        localContent = file.readText(Charsets.UTF_8),
                        isSensitive = com.example.core.storage.SensitiveFileDetector.isSensitive(file.name)
                    )
                )
            } else {
                val localBytes = file.readBytes()
                val localSha = computeGitBlobSha1(localBytes)
                if (localSha != remoteContent && file.readText(Charsets.UTF_8) != remoteContent) {
                    statuses.add(
                        GitFileStatus(
                            relativePath = relPath,
                            file = file,
                            status = GitFileStatus.StatusType.MODIFIED,
                            localContent = file.readText(Charsets.UTF_8),
                            remoteContent = remoteContent,
                            isSensitive = com.example.core.storage.SensitiveFileDetector.isSensitive(file.name)
                        )
                    )
                } else {
                    statuses.add(
                        GitFileStatus(
                            relativePath = relPath,
                            file = file,
                            status = GitFileStatus.StatusType.UNMODIFIED,
                            localContent = file.readText(Charsets.UTF_8),
                            remoteContent = remoteContent
                        )
                    )
                }
            }
        }

        // Check remote files that no longer exist locally
        remoteFiles.keys.forEach { relPath ->
            if (!localFilesMap.containsKey(relPath)) {
                statuses.add(
                    GitFileStatus(
                        relativePath = relPath,
                        file = null,
                        status = GitFileStatus.StatusType.DELETED,
                        remoteContent = remoteFiles[relPath]
                    )
                )
            }
        }

        return statuses.sortedBy { it.relativePath }
    }

    /**
     * Generate standard unified text diff lines between old and new text.
     */
    fun generateUnifiedDiff(oldText: String, newText: String): List<GitDiffLine> {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isEmpty()) emptyList() else newText.lines()

        val diffLines = mutableListOf<GitDiffLine>()
        var oIdx = 0
        var nIdx = 0

        while (oIdx < oldLines.size || nIdx < newLines.size) {
            when {
                oIdx < oldLines.size && nIdx < newLines.size && oldLines[oIdx] == newLines[nIdx] -> {
                    diffLines.add(
                        GitDiffLine(
                            type = GitDiffLine.LineType.UNCHANGED,
                            content = " " + oldLines[oIdx],
                            oldLineNumber = oIdx + 1,
                            newLineNumber = nIdx + 1
                        )
                    )
                    oIdx++
                    nIdx++
                }
                oIdx < oldLines.size && (nIdx >= newLines.size || !newLines.contains(oldLines[oIdx])) -> {
                    diffLines.add(
                        GitDiffLine(
                            type = GitDiffLine.LineType.REMOVED,
                            content = "-" + oldLines[oIdx],
                            oldLineNumber = oIdx + 1
                        )
                    )
                    oIdx++
                }
                nIdx < newLines.size -> {
                    diffLines.add(
                        GitDiffLine(
                            type = GitDiffLine.LineType.ADDED,
                            content = "+" + newLines[nIdx],
                            newLineNumber = nIdx + 1
                        )
                    )
                    nIdx++
                }
            }
        }
        return diffLines
    }

    private fun shouldIgnore(file: File, projectDir: File): Boolean {
        val rel = file.relativeTo(projectDir).path.replace('\\', '/')
        if (rel.startsWith(".git/")) return true
        if (rel.startsWith(".gradle/")) return true
        if (rel.startsWith("build/") || rel.contains("/build/")) return true
        if (rel.startsWith(".idea/")) return true
        if (rel.endsWith(".iml")) return true
        return false
    }
}
