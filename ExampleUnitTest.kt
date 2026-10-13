package com.example

import com.example.core.git.GitDiffLine
import com.example.core.git.GitEngine
import com.example.core.storage.SensitiveFileDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSensitiveFileDetection() {
        assertTrue(SensitiveFileDetector.isSensitive(".env"))
        assertTrue(SensitiveFileDetector.isSensitive(".env.local"))
        assertTrue(SensitiveFileDetector.isSensitive("release.jks"))
        assertTrue(SensitiveFileDetector.isSensitive("upload.keystore"))
        assertTrue(SensitiveFileDetector.isSensitive("google-services.json"))
        assertTrue(SensitiveFileDetector.isSensitive("id_rsa"))

        assertFalse(SensitiveFileDetector.isSensitive("MainActivity.kt"))
        assertFalse(SensitiveFileDetector.isSensitive("build.gradle.kts"))
        assertFalse(SensitiveFileDetector.isSensitive("README.md"))
    }

    @Test
    fun testGitBlobSha1Calculation() {
        // Standard Git SHA-1 for "hello world\n"
        val text = "hello world\n"
        val sha = GitEngine.computeGitBlobSha1(text.toByteArray(Charsets.UTF_8))
        // Git: printf "hello world\n" | git hash-object --stdin -> 3b18e512dba79e4c8300dd08aeb37f8e728b8dad
        assertEquals("3b18e512dba79e4c8300dd08aeb37f8e728b8dad", sha)
    }

    @Test
    fun testUnifiedDiffGeneration() {
        val oldText = "fun main() {\n    println(\"old\")\n}"
        val newText = "fun main() {\n    println(\"new\")\n}"
        val diff = GitEngine.generateUnifiedDiff(oldText, newText)

        assertTrue(diff.any { it.type == GitDiffLine.LineType.REMOVED && it.content.contains("old") })
        assertTrue(diff.any { it.type == GitDiffLine.LineType.ADDED && it.content.contains("new") })
        assertTrue(diff.any { it.type == GitDiffLine.LineType.UNCHANGED && it.content.contains("main") })
    }
}
