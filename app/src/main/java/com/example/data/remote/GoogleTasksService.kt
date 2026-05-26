package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class GoogleTaskList(
    val id: String,
    val title: String
)

data class GoogleTaskListResponse(
    val items: List<GoogleTaskList>? = null
)

data class GoogleTask(
    val id: String? = null,
    val title: String,
    val notes: String? = null,
    val status: String? = null, // "needsAction" or "completed"
    val due: String? = null,    // RFC3339 timestamp (e.g., yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
    val completed: String? = null
)

data class GoogleTasksResponse(
    val items: List<GoogleTask>? = null
)

interface GoogleTasksService {
    @GET("users/@me/lists")
    suspend fun getTaskLists(
        @Header("Authorization") authHeader: String
    ): GoogleTaskListResponse

    @POST("users/@me/lists")
    suspend fun createTaskList(
        @Header("Authorization") authHeader: String,
        @Body list: Map<String, String>
    ): GoogleTaskList

    @GET("lists/{tasklistId}/tasks")
    suspend fun getTasks(
        @Header("Authorization") authHeader: String,
        @Path("tasklistId") tasklistId: String
    ): GoogleTasksResponse

    @POST("lists/{tasklistId}/tasks")
    suspend fun createTask(
        @Header("Authorization") authHeader: String,
        @Path("tasklistId") tasklistId: String,
        @Body task: GoogleTask
    ): GoogleTask

    @PATCH("lists/{tasklistId}/tasks/{taskId}")
    suspend fun updateTask(
        @Header("Authorization") authHeader: String,
        @Path("tasklistId") tasklistId: String,
        @Path("taskId") taskId: String,
        @Body task: GoogleTask
    ): GoogleTask

    @DELETE("lists/{tasklistId}/tasks/{taskId}")
    suspend fun deleteTask(
        @Header("Authorization") authHeader: String,
        @Path("tasklistId") tasklistId: String,
        @Path("taskId") taskId: String
    )
}
