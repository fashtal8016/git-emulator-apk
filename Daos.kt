package com.example.core.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface BuildHistoryDao {
    @Query("SELECT * FROM build_history ORDER BY createdAt DESC")
    fun getAllBuilds(): Flow<List<BuildHistoryEntity>>

    @Query("SELECT * FROM build_history WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getBuildsForProject(projectId: Long): Flow<List<BuildHistoryEntity>>

    @Query("SELECT * FROM build_history WHERE runId = :runId LIMIT 1")
    suspend fun getBuildByRunId(runId: Long): BuildHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: BuildHistoryEntity): Long

    @Update
    suspend fun updateBuild(build: BuildHistoryEntity)

    @Query("DELETE FROM build_history WHERE id = :id")
    suspend fun deleteBuildById(id: Long)

    @Query("DELETE FROM build_history")
    suspend fun clearAll()
}

@Dao
interface ActionCommandDao {
    @Query("SELECT * FROM action_commands ORDER BY isFavorite DESC, orderIndex ASC")
    fun getAllActions(): Flow<List<ActionCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionCommandEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(actions: List<ActionCommandEntity>)

    @Update
    suspend fun updateAction(action: ActionCommandEntity)

    @Query("UPDATE action_commands SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFav: Boolean)

    @Delete
    suspend fun deleteAction(action: ActionCommandEntity)

    @Query("SELECT COUNT(*) FROM action_commands")
    suspend fun getCount(): Int
}
