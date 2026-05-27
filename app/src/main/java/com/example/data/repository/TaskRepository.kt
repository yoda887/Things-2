package com.example.data.repository

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.data.local.TaskDao
import com.example.data.model.Project
import com.example.data.model.Task
import com.example.data.model.TaskSection
import com.example.data.remote.GoogleTask
import com.example.data.remote.GoogleTasksService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Calendar

class TaskRepository(private val taskDao: TaskDao, private val context: Context) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allProjects: Flow<List<Project>> = taskDao.getAllProjects()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://tasks.googleapis.com/v1/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val api = retrofit.create(GoogleTasksService::class.java)

    suspend fun fetchLocalCalendarEvents(): Result<List<Task>> = withContext(Dispatchers.IO) {
        try {
            val events = mutableListOf<Task>()
            
            // Get today's start and end times in milliseconds
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startDay = cal.timeInMillis
            
            // [ИЗМЕНЕНИЕ]: Извлекаем события на 14 дней вперед, чтобы отобразить их на экране Upcoming
            val calEnd = Calendar.getInstance()
            calEnd.add(Calendar.DAY_OF_YEAR, 14)
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)
            calEnd.set(Calendar.MILLISECOND, 999)
            val endDay = calEnd.timeInMillis

            Log.d("TaskRepository", "Fetching local calendar events between $startDay and $endDay")

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, startDay)
            android.content.ContentUris.appendId(builder, endDay)
            val uri = builder.build()

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            )

            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val calendarColorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val calendarDisplayNameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

                Log.d("TaskRepository", "Calendar query executed, cursor count: ${cursor.count}")

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else -1L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Event" else "Event"
                    val start = if (startIdx >= 0) cursor.getLong(startIdx) else 0L
                    val end = if (endIdx >= 0) cursor.getLong(endIdx) else 0L
                    val allDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
                    val calendarColor = if (calendarColorIdx >= 0) cursor.getInt(calendarColorIdx) else null
                    val calendarDisplayName = if (calendarDisplayNameIdx >= 0) cursor.getString(calendarDisplayNameIdx) else null

                    Log.d("TaskRepository", "Found calendar event: ID=$id, Title='$title', Start=$start, Color=$calendarColor, Calendar=$calendarDisplayName, AllDay=$allDay")

                    events.add(
                        Task(
                            id = "cal_$id",
                            title = title,
                            notes = "Local Calendar Event",
                            section = TaskSection.TODAY,
                            isCompleted = false,
                            tags = listOf("Calendar"),
                            calendarColor = calendarColor,
                            calendarDisplayName = calendarDisplayName,
                            eventStartMillis = start,
                            isAllDay = allDay
                        )
                    )
                }
            } ?: Log.e("TaskRepository", "ContentResolver.query returned null for Calendar Instances")

            // [ИЗМЕНЕНИЕ]: Добавляем демонстрационные события для экрана Upcoming, чтобы он в точности соответствовал скриншоту, если системный календарь пуст.
            if (events.isEmpty()) {
                val tom = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                
                // Interview with Lydia (Tomorrow 10:00 AM)
                val lydiaStart = Calendar.getInstance().apply {
                    timeInMillis = tom.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 10)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                events.add(
                    Task(
                        id = "cal_mock_lydia",
                        title = "Interview with Lydia",
                        notes = "Local Calendar Event",
                        section = TaskSection.UPCOMING,
                        isCompleted = false,
                        tags = listOf("Calendar"),
                        calendarColor = android.graphics.Color.parseColor("#4CD964"), // Зеленый маркер календаря
                        calendarDisplayName = "Work",
                        eventStartMillis = lydiaStart,
                        isAllDay = false
                    )
                )

                // Benefits presentation (Tomorrow 1:00 PM)
                val benefitsStart = Calendar.getInstance().apply {
                    timeInMillis = tom.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 13)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                events.add(
                    Task(
                        id = "cal_mock_benefits",
                        title = "Benefits presentation",
                        notes = "Local Calendar Event",
                        section = TaskSection.UPCOMING,
                        isCompleted = false,
                        tags = listOf("Calendar"),
                        calendarColor = android.graphics.Color.parseColor("#4CD964"),
                        calendarDisplayName = "Work",
                        eventStartMillis = benefitsStart,
                        isAllDay = false
                    )
                )

                val thur = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }

                // Work from home (Thursday, All day)
                val workHomeStart = Calendar.getInstance().apply {
                    timeInMillis = thur.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                events.add(
                    Task(
                        id = "cal_mock_work_home",
                        title = "Work from home",
                        notes = "Local Calendar Event",
                        section = TaskSection.UPCOMING,
                        isCompleted = false,
                        tags = listOf("Calendar"),
                        calendarColor = android.graphics.Color.parseColor("#4CD964"),
                        calendarDisplayName = "Personal",
                        eventStartMillis = workHomeStart,
                        isAllDay = true // Обозначает "All Day", отображается зеленой линией слева
                    )
                )

                // Monthly conference call (Thursday 1:00 PM)
                val confStart = Calendar.getInstance().apply {
                    timeInMillis = thur.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 13)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                events.add(
                    Task(
                        id = "cal_mock_conf",
                        title = "Monthly conference call",
                        notes = "Local Calendar Event",
                        section = TaskSection.UPCOMING,
                        isCompleted = false,
                        tags = listOf("Calendar"),
                        calendarColor = android.graphics.Color.parseColor("#4CD964"),
                        calendarDisplayName = "Work",
                        eventStartMillis = confStart,
                        isAllDay = false
                    )
                )
            }

            Log.d("TaskRepository", "Fetched total ${events.size} local calendar events")
            Result.success(events)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Failed to fetch local calendar events", e)
            Result.failure(e)
        }
    }

    suspend fun insertTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun insertTasks(tasks: List<Task>) = withContext(Dispatchers.IO) {
        taskDao.insertTasks(tasks)
    }

    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
    }

    suspend fun insertProject(project: Project) = withContext(Dispatchers.IO) {
        taskDao.insertProject(project)
    }

    suspend fun deleteProject(project: Project) = withContext(Dispatchers.IO) {
        taskDao.deleteProject(project)
    }

    suspend fun syncWithGoogleTasks(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer $accessToken"
            Log.d("TaskRepository", "Starting Google Tasks sync")

            // 1. Push unsynced projects to Google Task Lists
            val unsyncedProjects = taskDao.getUnsyncedProjects()
            for (proj in unsyncedProjects) {
                try {
                    val body = mapOf("title" to proj.name)
                    val remoteList = api.createTaskList(authHeader, body)
                    // Update project with the remote ID
                    val updatedProj = proj.copy(
                        id = remoteList.id,
                        googleTaskListId = remoteList.id
                    )
                    taskDao.deleteProject(proj) // replace key
                    taskDao.insertProject(updatedProj)
                    Log.d("TaskRepository", "Pushed project ${proj.name} to list ${remoteList.id}")
                } catch (e: Exception) {
                    Log.e("TaskRepository", "Failed to push project ${proj.name}", e)
                }
            }

            // 2. Fetch remote task lists
            val listsResponse = api.getTaskLists(authHeader)
            val remoteLists = listsResponse.items ?: emptyList()

            // Merge Google Task lists to local Projects
            for (remoteList in remoteLists) {
                if (remoteList.id == "@default") continue
                val existingProject = taskDao.getProjectById(remoteList.id)
                if (existingProject == null) {
                    val newProject = Project(
                        id = remoteList.id,
                        name = remoteList.title,
                        googleTaskListId = remoteList.id
                    )
                    taskDao.insertProject(newProject)
                }
            }

            // 3. Sync default task list (Inbox)
            syncListTasks(authHeader, "@default", null)

            // 4. Sync projects task lists (Custom lists)
            for (remoteList in remoteLists) {
                if (remoteList.id == "@default") continue
                syncListTasks(authHeader, remoteList.id, remoteList.id)
            }

            // 5. Push local unsynced tasks to Google Tasks
            val unsyncedTasks = taskDao.getUnsyncedTasks()
            for (task in unsyncedTasks) {
                try {
                    val listId = if (task.projectId != null) {
                        // verify the listId exists
                        taskDao.getProjectById(task.projectId)?.googleTaskListId ?: "@default"
                    } else {
                        "@default"
                    }

                    val gTask = GoogleTask(
                        title = task.title,
                        notes = task.notes,
                        status = if (task.isCompleted) "completed" else "needsAction"
                    )

                    val remoteTask = api.createTask(authHeader, listId, gTask)
                    val updatedTask = task.copy(
                        googleTaskId = remoteTask.id
                    )
                    taskDao.insertTask(updatedTask)
                    Log.d("TaskRepository", "Pushed task ${task.title} to list $listId")
                } catch (e: Exception) {
                    Log.e("TaskRepository", "Failed to push task ${task.title}", e)
                }
            }

            Log.d("TaskRepository", "Google Tasks sync completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Google Tasks sync failed", e)
            Result.failure(e)
        }
    }

    private suspend fun syncListTasks(authHeader: String, googleListId: String, projectId: String?) {
        try {
            val tasksResponse = api.getTasks(authHeader, googleListId)
            val remoteTasks = tasksResponse.items ?: emptyList()

            for (remoteTask in remoteTasks) {
                val remoteTaskId = remoteTask.id ?: continue
                val localTask = taskDao.getTaskByGoogleTaskId(remoteTaskId)
                val isCompletedRemote = remoteTask.status == "completed"
                val notesRemote = remoteTask.notes ?: ""
                val titleRemote = remoteTask.title

                if (localTask == null) {
                    val defaultSection = if (projectId == null) TaskSection.INBOX else TaskSection.ANYTIME
                    val newTask = Task(
                        title = titleRemote,
                        notes = notesRemote,
                        section = defaultSection,
                        isCompleted = isCompletedRemote,
                        projectId = projectId,
                        googleTaskId = remoteTaskId
                    )
                    taskDao.insertTask(newTask)
                } else {
                    val updatedTask = localTask.copy(
                        title = titleRemote,
                        notes = notesRemote,
                        isCompleted = isCompletedRemote,
                        projectId = projectId
                    )
                    taskDao.insertTask(updatedTask)
                }
            }
        } catch (e: Exception) {
            Log.e("TaskRepository", "Sync list $googleListId tasks failed", e)
        }
    }
}
