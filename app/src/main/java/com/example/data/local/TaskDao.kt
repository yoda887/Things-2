package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Project
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY creationDate DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("SELECT * FROM projects ORDER BY creationDate ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): Project?

    @Query("SELECT * FROM tasks WHERE googleTaskId = :googleTaskId")
    suspend fun getTaskByGoogleTaskId(googleTaskId: String): Task?

    @Query("SELECT * FROM tasks WHERE googleTaskId IS NULL")
    suspend fun getUnsyncedTasks(): List<Task>

    @Query("SELECT * FROM projects WHERE googleTaskListId IS NULL")
    suspend fun getUnsyncedProjects(): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)
}
