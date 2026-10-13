package com.example.core.storage

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectStorageManager(private val context: Context) {
    private val projectsDir: File = File(context.filesDir, "projects").apply {
        if (!exists()) mkdirs()
    }

    private val trashDir: File = File(context.filesDir, "trash").apply {
        if (!exists()) mkdirs()
    }

    private val downloadsDir: File = File(context.filesDir, "downloads").apply {
        if (!exists()) mkdirs()
    }

    fun getProjectsRoot(): File = projectsDir
    fun getDownloadsDir(): File = downloadsDir

    fun createProjectFolder(projectName: String): File {
        val sanitized = sanitizeFileName(projectName)
        val dir = File(projectsDir, sanitized)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getProjectDirectory(projectName: String): File {
        return File(projectsDir, sanitizeFileName(projectName))
    }

    /**
     * Safely extract ZIP stream with Zip Slip protection.
     */
    fun extractZip(inputStream: InputStream, destinationDir: File, onProgress: ((Int, String) -> Unit)? = null): List<File> {
        val extractedFiles = mutableListOf<File>()
        val canonicalDest = destinationDir.canonicalPath

        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            var count = 0
            while (entry != null) {
                count++
                val newFile = File(destinationDir, entry.name)

                // Critical security check: prevent Zip Slip path traversal
                if (!newFile.canonicalPath.startsWith(canonicalDest)) {
                    throw SecurityException("ZIP entry attempts path traversal outside destination: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(newFile)).use { bos ->
                        val buffer = ByteArray(8192)
                        var readLen: Int
                        while (zis.read(buffer).also { readLen = it } != -1) {
                            bos.write(buffer, 0, readLen)
                        }
                    }
                    extractedFiles.add(newFile)
                }
                onProgress?.invoke(count, entry.name)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return extractedFiles
    }

    /**
     * Export directory to a ZIP archive.
     */
    fun exportToZip(sourceDir: File, outputZipFile: File): File {
        outputZipFile.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(sourceDir).path
                    val zipEntry = ZipEntry(relativePath)
                    zos.putNextEntry(zipEntry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
        return outputZipFile
    }

    /**
     * Build hierarchical tree of files.
     */
    fun getFileTree(dir: File): List<FileItem> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val files = dir.listFiles() ?: return emptyList()

        return files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { file ->
                val isDir = file.isDirectory
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = isDir,
                    size = if (isDir) 0 else file.length(),
                    lastModified = file.lastModified(),
                    extension = if (isDir) "" else file.extension.lowercase(),
                    isSensitive = SensitiveFileDetector.isSensitive(file.name),
                    children = if (isDir) getFileTree(file) else emptyList()
                )
            }
    }

    /**
     * List immediate children of a directory.
     */
    fun listFiles(dir: File): List<FileItem> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val files = dir.listFiles() ?: return emptyList()

        return files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { file ->
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) 0 else file.length(),
                    lastModified = file.lastModified(),
                    extension = if (file.isDirectory) "" else file.extension.lowercase(),
                    isSensitive = SensitiveFileDetector.isSensitive(file.name)
                )
            }
    }

    fun readFileContent(file: File): String {
        return if (file.exists() && file.isFile) {
            file.readText(Charsets.UTF_8)
        } else ""
    }

    fun writeFileContent(file: File, content: String) {
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    fun createFile(parentDir: File, fileName: String, initialContent: String = ""): File {
        val file = File(parentDir, fileName)
        file.parentFile?.mkdirs()
        file.writeText(initialContent, Charsets.UTF_8)
        return file
    }

    fun createDirectory(parentDir: File, dirName: String): File {
        val dir = File(parentDir, dirName)
        dir.mkdirs()
        return dir
    }

    fun renameFile(source: File, newName: String): File {
        val target = File(source.parentFile, newName)
        if (source.renameTo(target)) return target
        return source
    }

    /**
     * Move file to internal trash for safe recovery.
     */
    fun moveToTrash(file: File): Boolean {
        if (!file.exists()) return false
        val trashTarget = File(trashDir, "${System.currentTimeMillis()}_${file.name}")
        return file.renameTo(trashTarget)
    }

    /**
     * Validate whether a folder contains a valid Android project structure.
     */
    fun validateAndroidProject(projectDir: File): ProjectValidationResult {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return ProjectValidationResult(
                isValid = false,
                errors = listOf("Selected path does not exist or is not a directory.")
            )
        }

        val hasBuildGradle = File(projectDir, "build.gradle").exists() || File(projectDir, "build.gradle.kts").exists()
        val hasSettingsGradle = File(projectDir, "settings.gradle").exists() || File(projectDir, "settings.gradle.kts").exists()
        val hasAppModule = File(projectDir, "app").exists() && File(projectDir, "app").isDirectory
        val hasAppBuildGradle = File(projectDir, "app/build.gradle").exists() || File(projectDir, "app/build.gradle.kts").exists()
        val hasManifest = File(projectDir, "app/src/main/AndroidManifest.xml").exists()
        val hasGradlew = File(projectDir, "gradlew").exists()
        val hasWorkflow = File(projectDir, ".github/workflows/android-build.yml").exists()

        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        if (!hasBuildGradle && !hasSettingsGradle) {
            errors.add("Missing Gradle build configuration (build.gradle / settings.gradle)")
        }
        if (!hasAppModule || !hasAppBuildGradle) {
            warnings.add("Missing standard 'app' module directory with build.gradle")
        }
        if (!hasManifest) {
            warnings.add("Missing app/src/main/AndroidManifest.xml")
        }
        if (!hasGradlew) {
            warnings.add("Missing gradlew executable wrapper (required by GitHub Actions)")
        }
        if (!hasWorkflow) {
            warnings.add("Missing .github/workflows/android-build.yml (KotlinForge can generate this automatically)")
        }

        return ProjectValidationResult(
            isValid = errors.isEmpty(),
            hasGradlew = hasGradlew,
            hasWorkflow = hasWorkflow,
            hasManifest = hasManifest,
            warnings = warnings,
            errors = errors
        )
    }

    /**
     * Scaffold a complete, build-ready Android Kotlin Compose starter project.
     */
    fun createStarterProject(projectName: String, packageName: String = "com.example.app"): File {
        val root = createProjectFolder(projectName)

        // 1. settings.gradle.kts
        File(root, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "$projectName"
            include(":app")
            """.trimIndent()
        )

        // 2. build.gradle.kts (root)
        File(root, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.android.application") version "8.5.2" apply false
                id("org.jetbrains.kotlin.android") version "2.0.0" apply false
                id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
            }
            """.trimIndent()
        )

        // 3. gradle.properties
        File(root, "gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
            android.useAndroidX=true
            android.nonTransitiveRClass=true
            kotlin.code.style=official
            """.trimIndent()
        )

        // 4. .gitignore
        File(root, ".gitignore").writeText(
            """
            *.iml
            .gradle
            /local.properties
            /.idea/caches
            /.idea/libraries
            /.idea/modules.xml
            /.idea/workspace.xml
            .DS_Store
            /build
            /captures
            .externalNativeBuild
            .cxx
            local.properties
            *.jks
            *.keystore
            .env
            """.trimIndent()
        )

        // 5. gradlew placeholder
        val gradlew = File(root, "gradlew")
        gradlew.writeText("#!/usr/bin/env sh\nexec gradle \"$@\"\n")
        try { gradlew.setExecutable(true) } catch (_: Exception) {}

        // 6. app module
        val appDir = File(root, "app").apply { mkdirs() }
        File(appDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
                id("org.jetbrains.kotlin.plugin.compose")
            }

            android {
                namespace = "$packageName"
                compileSdk = 34

                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = 24
                    targetSdk = 34
                    versionCode = 1
                    versionName = "1.0"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                kotlinOptions {
                    jvmTarget = "17"
                }
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                implementation(platform("androidx.compose:compose-bom:2024.06.00"))
                implementation("androidx.activity:activity-compose:1.9.0")
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-graphics")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.core:core-ktx:1.13.1")
            }
            """.trimIndent()
        )

        // 7. AndroidManifest.xml
        val mainDir = File(appDir, "src/main").apply { mkdirs() }
        File(mainDir, "AndroidManifest.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="true"
                    android:label="$projectName"
                    android:supportsRtl="true"
                    android:theme="@android:style/Theme.Material.Light.NoActionBar">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()
        )

        // 8. Kotlin source
        val pkgPath = packageName.replace('.', '/')
        val javaDir = File(mainDir, "java/$pkgPath").apply { mkdirs() }
        File(javaDir, "MainActivity.kt").writeText(
            """
            package $packageName

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        MaterialTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "$projectName",
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                        Text(
                                            text = "Built with KotlinForge AI IDE",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent()
        )

        // 9. Workflow generator
        val workflowDir = File(root, ".github/workflows").apply { mkdirs() }
        File(workflowDir, "android-build.yml").writeText(getStandardWorkflowContent())

        return root
    }

    fun getStandardWorkflowContent(): String {
        return """
        name: Android Cloud Build

        on:
          workflow_dispatch:
            inputs:
              build_type:
                description: 'Build Variant'
                required: true
                default: 'debug'
                type: choice
                options:
                  - debug
                  - release
          push:
            branches: [ main, master ]
          pull_request:
            branches: [ main, master ]

        permissions:
          contents: read
          actions: read

        jobs:
          build:
            runs-on: ubuntu-latest
            steps:
              - name: Checkout Repository
                uses: actions/checkout@v4

              - name: Set up JDK 17
                uses: actions/setup-java@v4
                with:
                  java-version: '17'
                  distribution: 'temurin'
                  cache: gradle

              - name: Grant Execute Permission for Gradle Wrapper
                run: chmod +x gradlew || chmod +x ./gradlew || true

              - name: Build with Gradle
                run: |
                  if [ -f "./gradlew" ]; then
                    ./gradlew assembleDebug --stacktrace
                  else
                    gradle assembleDebug --stacktrace
                  fi

              - name: Locate Generated APK
                id: locate_apk
                run: |
                  APK_PATH=$(find app/build/outputs/apk/ -name "*.apk" | head -n 1)
                  echo "Found APK at: ${'$'}APK_PATH"
                  echo "apk_path=${'$'}APK_PATH" >> ${'$'}GITHUB_OUTPUT

              - name: Upload APK Artifact
                uses: actions/upload-artifact@v4
                with:
                  name: app-debug-apk
                  path: app/build/outputs/apk/debug/*.apk
                  retention-days: 14
        """.trimIndent()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}

data class ProjectValidationResult(
    val isValid: Boolean,
    val hasGradlew: Boolean = false,
    val hasWorkflow: Boolean = false,
    val hasManifest: Boolean = false,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)
