package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.storage.AppDatabase
import com.example.core.storage.ProjectEntity
import com.example.core.storage.ProjectStorageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KotlinForge", appName)
    }

    @Test
    fun testProjectStarterScaffolding() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = ProjectStorageManager(context)

        val projectDir = storage.createStarterProject("TestForgeApp", "com.test.forge")
        assertTrue(projectDir.exists())
        assertTrue(File(projectDir, "settings.gradle.kts").exists())
        assertTrue(File(projectDir, "build.gradle.kts").exists())
        assertTrue(File(projectDir, "app/build.gradle.kts").exists())
        assertTrue(File(projectDir, "app/src/main/AndroidManifest.xml").exists())
        assertTrue(File(projectDir, ".github/workflows/android-build.yml").exists())

        val validation = storage.validateAndroidProject(projectDir)
        assertTrue(validation.isValid)
        assertTrue(validation.hasWorkflow)
        assertTrue(validation.hasManifest)
    }

    @Test
    fun testZipSlipProtection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = ProjectStorageManager(context)
        val destDir = File(context.filesDir, "test_dest").apply { mkdirs() }

        // Craft a malicious ZIP with path traversal ("../evil.txt")
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            zos.putNextEntry(ZipEntry("../evil.txt"))
            zos.write("payload".toByteArray())
            zos.closeEntry()
        }

        val maliciousZipBytes = bos.toByteArray()
        try {
            storage.extractZip(ByteArrayInputStream(maliciousZipBytes), destDir)
            fail("Expected SecurityException for Zip Slip attempt")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("path traversal", ignoreCase = true) == true)
        }
    }

    @Test
    fun testRoomDatabaseProjectPersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val entity = ProjectEntity(
            name = "RoboTestProject",
            path = "/mock/path/project",
            description = "Test description",
            isGitInitialized = true
        )
        val id = db.projectDao().insertProject(entity)
        assertTrue(id > 0)

        val retrieved = db.projectDao().getProjectById(id)
        assertNotNull(retrieved)
        assertEquals("RoboTestProject", retrieved?.name)
    }

    @Test
    fun testApkSha256Calculation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "test.apk").apply {
            writeText("dummy apk content for testing")
        }
        val sha256 = com.example.core.build.ApkDownloadManager.calculateSha256(testFile)
        assertNotNull(sha256)
        assertEquals(64, sha256.length)
        testFile.delete()
    }
}
