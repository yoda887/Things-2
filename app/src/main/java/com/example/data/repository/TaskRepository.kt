package com.example.data.repository

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.data.local.TaskDao
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.Tag
import com.example.data.model.ItemTag
import com.example.data.model.ChecklistItem
import com.example.data.model.TaskSection
import com.example.data.remote.GoogleTask
import com.example.data.remote.GoogleTasksService
import com.example.data.model.ItemWithChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Calendar

class TaskRepository(private val taskDao: TaskDao, private val context: Context) {

    val allTasks: Flow<List<ItemWithChecklist>> = combine(
        taskDao.getAllItems(),
        taskDao.getAllChecklistItemsFlow()
    ) { items, checklistItems ->
        val checklistMap = checklistItems.groupBy { it.itemId }
        items.map { item ->
            ItemWithChecklist(
                item = item,
                checklist = checklistMap[item.id] ?: emptyList()
            )
        }
    }

    val allProjects: Flow<List<Item>> = taskDao.getAllItems().map { items ->
        items.filter { it.type == 1 }
    }

    val allAreas: Flow<List<Area>> = taskDao.getAllAreasFlow()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://tasks.googleapis.com/v1/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val api = retrofit.create(GoogleTasksService::class.java)

    suspend fun refreshCachedTags(itemId: String) = withContext(Dispatchers.IO) {
        val tags = taskDao.getTagsByItemId(itemId)
        val cachedString = tags.joinToString(", ") { it.title }
        val item = taskDao.getItemById(itemId)
        if (item != null) {
            taskDao.insertItem(item.copy(cachedTags = cachedString))
        }
    }

    suspend fun refreshChecklistCounters(itemId: String) = withContext(Dispatchers.IO) {
        val total = taskDao.getTotalChecklistCount(itemId)
        val open = taskDao.getOpenChecklistCount(itemId)
        taskDao.updateChecklistCounters(itemId, total, open)
    }

    suspend fun fetchLocalCalendarEvents(): Result<List<Item>> = withContext(Dispatchers.IO) {
        try {
            val events = mutableListOf<Item>()
            
            // Get today's start and end times in milliseconds
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startDay = cal.timeInMillis
            
            // Extract events up to 14 days ahead
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

                    Log.d("TaskRepository", "Found calendar event: ID=$id, Title='$title', Start=$start")

                    events.add(
                        Item(
                            id = "cal_$id",
                            type = 0,
                            title = title,
                            notes = "Local Calendar Event",
                            start = 1, // today
                            status = 0,
                            cachedTags = "Calendar",
                            calendarColor = calendarColor,
                            calendarDisplayName = calendarDisplayName,
                            eventStartMillis = start,
                            isAllDay = allDay
                        )
                    )
                }
            } ?: Log.e("TaskRepository", "ContentResolver.query returned null for Calendar Instances")

            // If empty, add mock calendar events for demonstration
            if (events.isEmpty()) {
                val tom = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                
                val lydiaStart = Calendar.getInstance().apply {
                    timeInMillis = tom.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 10)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                events.add(
                    Item(
                        id = "cal_mock_lydia",
                        type = 0,
                        title = "Interview with Lydia",
                        notes = "Local Calendar Event",
                        start = 1, // mapped to today's sections
                        status = 0,
                        cachedTags = "Calendar",
                        calendarColor = android.graphics.Color.parseColor("#4CD964"),
                        calendarDisplayName = "Work",
                        eventStartMillis = lydiaStart,
                        isAllDay = false
                    )
                )

                val benefitsStart = Calendar.getInstance().apply {
                    timeInMillis = tom.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 13)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                events.add(
                    Item(
                        id = "cal_mock_benefits",
                        type = 0,
                        title = "Benefits presentation",
                        notes = "Local Calendar Event",
                        start = 1,
                        status = 0,
                        cachedTags = "Calendar",
                        calendarColor = android.graphics.Color.parseColor("#4CD964"),
                        calendarDisplayName = "Work",
                        eventStartMillis = benefitsStart,
                        isAllDay = false
                    )
                )

                val thur = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }

                val workHomeStart = Calendar.getInstance().apply {
                    timeInMillis = thur.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                events.add(
                    Item(
                        id = "cal_mock_work_home",
                        type = 0,
                        title = "Work from home",
                        notes = "Local Calendar Event",
                        start = 1,
                        status = 0,
                        cachedTags = "Calendar",
                        calendarColor = android.graphics.Color.parseColor("#4CD964"),
                        calendarDisplayName = "Personal",
                        eventStartMillis = workHomeStart,
                        isAllDay = true
                    )
                )

                val confStart = Calendar.getInstance().apply {
                    timeInMillis = thur.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 13)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                events.add(
                    Item(
                        id = "cal_mock_conf",
                        type = 0,
                        title = "Monthly conference call",
                        notes = "Local Calendar Event",
                        start = 1,
                        status = 0,
                        cachedTags = "Calendar",
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

    suspend fun insertTask(item: Item, checklist: List<ChecklistItem> = emptyList()) = withContext(Dispatchers.IO) {
        taskDao.insertItem(item)
        updateChecklistItems(item.id, checklist)
    }

    suspend fun insertTasks(items: List<Item>) = withContext(Dispatchers.IO) {
        taskDao.insertItems(items)
    }

    suspend fun deleteTask(item: Item) = withContext(Dispatchers.IO) {
        taskDao.deleteItem(item)
    }

    suspend fun deleteTaskById(id: String) = withContext(Dispatchers.IO) {
        taskDao.deleteItemById(id)
    }

    suspend fun insertProject(project: Item) = withContext(Dispatchers.IO) {
        taskDao.insertItem(project)
    }

    suspend fun deleteProject(project: Item) = withContext(Dispatchers.IO) {
        taskDao.deleteItem(project)
    }

    // Areas management
    suspend fun insertArea(area: Area) = withContext(Dispatchers.IO) {
        taskDao.insertArea(area)
    }

    suspend fun deleteArea(area: Area) = withContext(Dispatchers.IO) {
        taskDao.deleteArea(area)
    }

    // Tag management
    suspend fun insertTag(tag: Tag) = withContext(Dispatchers.IO) {
        taskDao.insertTag(tag)
    }

    suspend fun deleteTag(tag: Tag) = withContext(Dispatchers.IO) {
        taskDao.deleteTag(tag)
    }

    suspend fun updateItemTags(itemId: String, tags: List<Tag>) = withContext(Dispatchers.IO) {
        taskDao.deleteItemTagsByItemId(itemId)
        for (tag in tags) {
            taskDao.insertItemTag(ItemTag(itemId, tag.id))
        }
        refreshCachedTags(itemId)
    }

    suspend fun getTagsByItemId(itemId: String): List<Tag> = withContext(Dispatchers.IO) {
        taskDao.getTagsByItemId(itemId)
    }

    suspend fun getAllTags(): List<Tag> = withContext(Dispatchers.IO) {
        taskDao.getAllTags()
    }

    fun getAllTagsFlow(): Flow<List<Tag>> {
        return taskDao.getAllTagsFlow()
    }

    // Checklist update helper
    suspend fun updateChecklistItems(itemId: String, list: List<ChecklistItem>) = withContext(Dispatchers.IO) {
        if (list.isEmpty()) {
            taskDao.deleteChecklistItemsByItemId(itemId)
        } else {
            val entities = list.mapIndexed { index, item ->
                item.copy(itemId = itemId, sortOrder = index)
            }
            val keptIds = entities.map { it.id }
            
            taskDao.deleteRemovedChecklistItems(itemId, keptIds)
            taskDao.insertChecklistItems(entities)
        }
        refreshChecklistCounters(itemId)
    }

    suspend fun syncWithGoogleTasks(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer $accessToken"
            Log.d("TaskRepository", "Starting Google Tasks sync")

            // 1. Push unsynced projects to Google Task Lists
            val unsyncedProjects = taskDao.getUnsyncedProjects()
            for (proj in unsyncedProjects) {
                try {
                    val body = mapOf("title" to proj.title)
                    val remoteList = api.createTaskList(authHeader, body)
                    val updatedProj = proj.copy(
                        id = remoteList.id,
                        googleTaskListId = remoteList.id
                    )
                    taskDao.deleteItem(proj) // replace key
                    taskDao.insertItem(updatedProj)
                    Log.d("TaskRepository", "Pushed project ${proj.title} to list ${remoteList.id}")
                } catch (e: Exception) {
                    Log.e("TaskRepository", "Failed to push project ${proj.title}", e)
                }
            }

            // 2. Fetch remote task lists
            val listsResponse = api.getTaskLists(authHeader)
            val remoteLists = listsResponse.items ?: emptyList()

            // Merge Google Task lists to local Projects
            for (remoteList in remoteLists) {
                if (remoteList.id == "@default") continue
                val existingProject = taskDao.getItemById(remoteList.id)
                if (existingProject == null) {
                    val newProject = Item(
                        id = remoteList.id,
                        type = 1,
                        title = remoteList.title,
                        googleTaskListId = remoteList.id
                    )
                    taskDao.insertItem(newProject)
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
                        taskDao.getItemById(task.projectId)?.googleTaskListId ?: "@default"
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
                    taskDao.insertItem(updatedTask)
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
                val localTask = taskDao.getItemByGoogleTaskId(remoteTaskId)
                val isCompletedRemote = remoteTask.status == "completed"
                val notesRemote = remoteTask.notes ?: ""
                val titleRemote = remoteTask.title

                if (localTask == null) {
                    val defaultStart = if (projectId == null) 0 else 2 // 0 = inbox, 2 = anytime
                    val newTask = Item(
                        type = 0,
                        title = titleRemote,
                        notes = notesRemote,
                        start = defaultStart,
                        status = if (isCompletedRemote) 3 else 0,
                        projectId = projectId,
                        googleTaskId = remoteTaskId
                    )
                    taskDao.insertItem(newTask)
                    refreshChecklistCounters(newTask.id)
                    refreshCachedTags(newTask.id)
                } else {
                    val updatedTask = localTask.copy(
                        title = titleRemote,
                        notes = notesRemote,
                        status = if (isCompletedRemote) 3 else 0,
                        projectId = projectId
                    )
                    taskDao.insertItem(updatedTask)
                    refreshChecklistCounters(updatedTask.id)
                    refreshCachedTags(updatedTask.id)
                }
            }
        } catch (e: Exception) {
            Log.e("TaskRepository", "Sync list $googleListId tasks failed", e)
        }
    }
}
