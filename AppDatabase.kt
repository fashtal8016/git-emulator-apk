package com.example.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProjectEntity::class,
        BuildHistoryEntity::class,
        ActionCommandEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun buildHistoryDao(): BuildHistoryDao
    abstract fun actionCommandDao(): ActionCommandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kotlinforge_ide.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDefaultActions(getInstance(context).actionCommandDao())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDefaultActions(dao: ActionCommandDao) {
            val defaults = listOf(
                ActionCommandEntity(
                    title = "Connect GitHub",
                    description = "Authenticate account via OAuth or Device Flow",
                    category = "GitHub",
                    commandCode = "ACTION_GITHUB_CONNECT",
                    isFavorite = true,
                    orderIndex = 1
                ),
                ActionCommandEntity(
                    title = "Build Debug APK",
                    description = "Trigger cloud workflow to assemble Debug APK",
                    category = "Build",
                    commandCode = "ACTION_BUILD_DEBUG",
                    isFavorite = true,
                    orderIndex = 2
                ),
                ActionCommandEntity(
                    title = "Build Release APK",
                    description = "Assemble signing-ready production APK on GitHub Actions",
                    category = "Build",
                    commandCode = "ACTION_BUILD_RELEASE",
                    isFavorite = true,
                    orderIndex = 3
                ),
                ActionCommandEntity(
                    title = "Sync Project to GitHub",
                    description = "Stage changes, create atomic Git commit and push",
                    category = "Sync",
                    commandCode = "ACTION_SYNC_PUSH",
                    isFavorite = true,
                    orderIndex = 4
                ),
                ActionCommandEntity(
                    title = "Pull Changes from GitHub",
                    description = "Fetch latest remote tree and merge locally",
                    category = "Sync",
                    commandCode = "ACTION_SYNC_PULL",
                    isFavorite = false,
                    orderIndex = 5
                ),
                ActionCommandEntity(
                    title = "Import Project ZIP",
                    description = "Safely unpack and register an external Android project",
                    category = "File",
                    commandCode = "ACTION_IMPORT_ZIP",
                    isFavorite = true,
                    orderIndex = 6
                ),
                ActionCommandEntity(
                    title = "Export Project ZIP",
                    description = "Archive project directory to portable ZIP",
                    category = "File",
                    commandCode = "ACTION_EXPORT_ZIP",
                    isFavorite = false,
                    orderIndex = 7
                ),
                ActionCommandEntity(
                    title = "Generate CI/CD Workflow",
                    description = "Inject standard .github/workflows/android-build.yml",
                    category = "Build",
                    commandCode = "ACTION_GENERATE_WORKFLOW",
                    isFavorite = false,
                    orderIndex = 8
                ),
                ActionCommandEntity(
                    title = "AI Code Explanation",
                    description = "Analyze syntax, structure and suggest improvements",
                    category = "AI",
                    commandCode = "ACTION_AI_EXPLAIN",
                    isFavorite = false,
                    orderIndex = 9
                ),
                ActionCommandEntity(
                    title = "Scan for Sensitive Files",
                    description = "Audit directory for keys, passwords and tokens",
                    category = "System",
                    commandCode = "ACTION_SECURITY_AUDIT",
                    isFavorite = false,
                    orderIndex = 10
                )
            )
            dao.insertAll(defaults)
        }
    }
}
