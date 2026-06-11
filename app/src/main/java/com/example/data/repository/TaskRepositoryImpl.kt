package com.example.data.repository

import android.util.Log
import com.example.data.local.LocalTaskDataSource
import com.example.data.local.DeviceCalendarDataSource
import com.example.data.remote.RemoteTaskDataSource
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.Tag
import com.example.data.model.ItemTag
import com.example.data.model.ChecklistItem
import com.example.data.remote.GoogleTask
import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Реализация репозитория управления задачами, проектами и областями.
 * Реализует интерфейс [ITaskRepository], отделяя источник данных Room, внешний API Google Tasks
 * и локальный провайдер событий календаря устройства.
 *
 * @property localDataSource Локальный источник данных (БД Room)
 * @property remoteDataSource Удаленный источник данных (Google Tasks API)
 * @property calendarDataSource Источник данных календаря устройства
 */
class TaskRepositoryImpl(
    private val localDataSource: LocalTaskDataSource,
    private val remoteDataSource: RemoteTaskDataSource,
    private val calendarDataSource: DeviceCalendarDataSource
) : ITaskRepository {

    override fun observeAllTasks(): Flow<List<ItemWithChecklist>> = combine(
        localDataSource.getAllItems(),
        localDataSource.getAllChecklistItemsFlow()
    ) { items, checklistItems ->
        val checklistMap = checklistItems.groupBy { it.itemId }
        items.map { item ->
            ItemWithChecklist(
                item = item,
                checklist = checklistMap[item.id] ?: emptyList()
            )
        }
    }

    override fun observeAllProjects(): Flow<List<Item>> = localDataSource.getAllItems().map { items ->
        items.filter { it.type == 1 }
    }

    override fun observeAllAreas(): Flow<List<Area>> = localDataSource.getAllAreasFlow()

    override suspend fun refreshCachedTags(itemId: String): Unit = withContext(Dispatchers.IO) {
        val tags = localDataSource.getTagsByItemId(itemId)
        val cachedString = tags.joinToString(", ") { it.title }
        val item = localDataSource.getItemById(itemId)
        if (item != null) {
            localDataSource.insertItem(item.copy(cachedTags = cachedString))
        }
    }

    override suspend fun refreshChecklistCounters(itemId: String): Unit = withContext(Dispatchers.IO) {
        val total = localDataSource.getTotalChecklistCount(itemId)
        val open = localDataSource.getOpenChecklistCount(itemId)
        localDataSource.updateChecklistCounters(itemId, total, open)
    }

    override suspend fun fetchLocalCalendarEvents(): Result<List<Item>> = withContext(Dispatchers.IO) {
        calendarDataSource.fetchLocalCalendarEvents()
    }

    override suspend fun insertTask(item: Item, checklist: List<ChecklistItem>): Unit = withContext(Dispatchers.IO) {
        localDataSource.insertItem(item)
        updateChecklistItems(item.id, checklist)
    }

    override suspend fun insertTasks(items: List<Item>): Unit = withContext(Dispatchers.IO) {
        localDataSource.insertItems(items)
    }

    override suspend fun deleteTask(item: Item): Unit = withContext(Dispatchers.IO) {
        localDataSource.deleteItem(item)
    }

    override suspend fun deleteTaskById(id: String): Unit = withContext(Dispatchers.IO) {
        localDataSource.deleteItemById(id)
    }

    override suspend fun insertProject(project: Item): Unit = withContext(Dispatchers.IO) {
        localDataSource.insertItem(project)
    }

    override suspend fun deleteProject(project: Item): Unit = withContext(Dispatchers.IO) {
        localDataSource.deleteItem(project)
    }

    override suspend fun insertArea(area: Area): Unit = withContext(Dispatchers.IO) {
        localDataSource.insertArea(area)
    }

    override suspend fun deleteArea(area: Area): Unit = withContext(Dispatchers.IO) {
        localDataSource.deleteArea(area)
    }

    override suspend fun insertTag(tag: Tag): Unit = withContext(Dispatchers.IO) {
        val oldTag = localDataSource.getTagById(tag.id)
        localDataSource.insertTag(tag)
        
        if (oldTag != null && oldTag.title != tag.title) {
            val allItems = localDataSource.getAllItemsSync()
            for (item in allItems) {
                val currentTags = item.tags
                if (currentTags.any { it.equals(oldTag.title, ignoreCase = true) }) {
                    val newTags = currentTags.map { 
                        if (it.equals(oldTag.title, ignoreCase = true)) tag.title else it 
                    }
                    val newCachedString = newTags.joinToString(", ")
                    localDataSource.insertItem(item.copy(cachedTags = newCachedString))
                }
            }
        }
    }

    override suspend fun createGroup(groupName: String): Tag = withContext(Dispatchers.IO) {
        val group = Tag(title = groupName, parentId = null)
        localDataSource.insertTag(group)
        group
    }

    override suspend fun createTagInGroup(tagName: String, parentId: String?): Tag = withContext(Dispatchers.IO) {
        val tag = Tag(title = tagName, parentId = parentId)
        localDataSource.insertTag(tag)
        tag
    }

    override suspend fun moveTagToGroup(tagId: String, newGroupId: String?): Unit = withContext(Dispatchers.IO) {
        val tag = localDataSource.getTagById(tagId)
        if (tag != null) {
            localDataSource.insertTag(tag.copy(parentId = newGroupId))
        }
    }

    override fun observeGroups(): Flow<List<Tag>> {
        return localDataSource.observeGroups()
    }

    override fun observeByParent(parentId: String): Flow<List<Tag>> {
        return localDataSource.observeByParent(parentId)
    }

    override suspend fun deleteTag(tag: Tag): Unit = withContext(Dispatchers.IO) {
        val allItems = localDataSource.getAllItemsSync()
        val allTags = localDataSource.getAllTags()
        
        val tagsToDelete = mutableSetOf<Tag>()
        fun addTagAndChildren(t: Tag) {
            if (tagsToDelete.add(t)) {
                val children = allTags.filter { it.parentId == t.id }
                children.forEach { addTagAndChildren(it) }
            }
        }
        addTagAndChildren(tag)
        
        val titlesToDelete = tagsToDelete.map { it.title.lowercase() }
        
        for (t in tagsToDelete) {
            localDataSource.deleteTag(t)
        }
        
        for (item in allItems) {
            val currentTags = item.tags
            if (currentTags.any { titlesToDelete.contains(it.lowercase()) }) {
                val newTags = currentTags.filter { !titlesToDelete.contains(it.lowercase()) }
                val newCachedString = newTags.joinToString(", ")
                localDataSource.insertItem(item.copy(cachedTags = newCachedString))
            }
        }
    }

    override suspend fun updateTagsOrder(tags: List<Tag>): Unit = withContext(Dispatchers.IO) {
        localDataSource.insertTags(tags)
    }

    override suspend fun updateItemTags(itemId: String, tags: List<Tag>): Unit = withContext(Dispatchers.IO) {
        localDataSource.deleteItemTagsByItemId(itemId)
        for (tag in tags) {
            localDataSource.insertItemTag(ItemTag(itemId, tag.id))
        }
        refreshCachedTags(itemId)
    }

    override suspend fun getTagsByItemId(itemId: String): List<Tag> = withContext(Dispatchers.IO) {
        localDataSource.getTagsByItemId(itemId)
    }

    override suspend fun getAllTags(): List<Tag> = withContext(Dispatchers.IO) {
        localDataSource.getAllTags()
    }

    override fun getAllTagsFlow(): Flow<List<Tag>> {
        return localDataSource.getAllTagsFlow()
    }

    override suspend fun updateChecklistItems(itemId: String, list: List<ChecklistItem>): Unit = withContext(Dispatchers.IO) {
        if (list.isEmpty()) {
            localDataSource.deleteChecklistItemsByItemId(itemId)
        } else {
            val entities = list.mapIndexed { index, item ->
                item.copy(itemId = itemId, sortOrder = index)
            }
            val keptIds = entities.map { it.id }
            
            localDataSource.deleteRemovedChecklistItems(itemId, keptIds)
            localDataSource.insertChecklistItems(entities)
        }
        refreshChecklistCounters(itemId)
    }

    override suspend fun syncWithGoogleTasks(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer $accessToken"
            Log.d("TaskRepository", "Starting Google Tasks sync")

            // 1. Извлекаем локальные несохраненные проекты и отправляем их на сервер Google
            val unsyncedProjects = localDataSource.getUnsyncedProjects()
            for (proj in unsyncedProjects) {
                try {
                    val body = mapOf("title" to proj.title)
                    val remoteList = remoteDataSource.createTaskList(authHeader, body)
                    val updatedProj = proj.copy(
                        id = remoteList.id,
                        googleTaskListId = remoteList.id
                    )
                    localDataSource.deleteItem(proj) // Заменяем временный ключ на серверный ID
                    localDataSource.insertItem(updatedProj)
                    Log.d("TaskRepository", "Pushed project ${proj.title} to list ${remoteList.id}")
                } catch (e: Exception) {
                    Log.e("TaskRepository", "Failed to push project ${proj.title}", e)
                }
            }

            // 2. Получаем все удаленные списки задач с сервера Google
            val listsResponse = remoteDataSource.getTaskLists(authHeader)
            val remoteLists = listsResponse.items ?: emptyList()

            // Мержим удаленные списки Google во внутренние проекты Project
            for (remoteList in remoteLists) {
                if (remoteList.id == "@default") continue
                val existingProject = localDataSource.getItemById(remoteList.id)
                if (existingProject == null) {
                    val newProject = Item(
                        id = remoteList.id,
                        type = 1,
                        title = remoteList.title,
                        googleTaskListId = remoteList.id
                    )
                    localDataSource.insertItem(newProject)
                }
            }

            // 3. Синхронизируем список задач "по умолчанию" (Inbox)
            syncListTasks(authHeader, "@default", null)

            // 4. Синхронизируем задачи для каждого пользовательского проекта
            for (remoteList in remoteLists) {
                if (remoteList.id == "@default") continue
                syncListTasks(authHeader, remoteList.id, remoteList.id)
            }

            // 5. Отправляем на сервер Google все локально созданные несохраненные задачи
            val unsyncedTasks = localDataSource.getUnsyncedTasks()
            for (task in unsyncedTasks) {
                try {
                    val listId = if (task.projectId != null) {
                        localDataSource.getItemById(task.projectId)?.googleTaskListId ?: "@default"
                    } else {
                        "@default"
                    }

                    val gTask = GoogleTask(
                        title = task.title,
                        notes = task.notes,
                        status = if (task.isCompleted) "completed" else "needsAction"
                    )

                    val remoteTask = remoteDataSource.createTask(authHeader, listId, gTask)
                    val updatedTask = task.copy(
                        googleTaskId = remoteTask.id
                    )
                    localDataSource.insertItem(updatedTask)
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

    /**
     * Синхронизирует задачи конкретного удаленного списка Google Tasks и обновляет локальную базу.
     */
    private suspend fun syncListTasks(authHeader: String, googleListId: String, projectId: String?) {
        try {
            val tasksResponse = remoteDataSource.getTasks(authHeader, googleListId)
            val remoteTasks = tasksResponse.items ?: emptyList()

            for (remoteTask in remoteTasks) {
                val remoteTaskId = remoteTask.id ?: continue
                val localTask = localDataSource.getItemByGoogleTaskId(remoteTaskId)
                val isCompletedRemote = remoteTask.status == "completed"
                val notesRemote = remoteTask.notes ?: ""
                val titleRemote = remoteTask.title

                if (localTask == null) {
                    val defaultStart = if (projectId == null) 0 else 2 // 0 = Inbox, 2 = Anytime
                    val newTask = Item(
                        type = 0,
                        title = titleRemote,
                        notes = notesRemote,
                        start = defaultStart,
                        status = if (isCompletedRemote) 3 else 0,
                        projectId = projectId,
                        googleTaskId = remoteTaskId
                    )
                    localDataSource.insertItem(newTask)
                    refreshChecklistCounters(newTask.id)
                    refreshCachedTags(newTask.id)
                } else {
                    val updatedTask = localTask.copy(
                        title = titleRemote,
                        notes = notesRemote,
                        status = if (isCompletedRemote) 3 else 0,
                        projectId = projectId
                    )
                    localDataSource.insertItem(updatedTask)
                    refreshChecklistCounters(updatedTask.id)
                    refreshCachedTags(updatedTask.id)
                }
            }
        } catch (e: Exception) {
            Log.e("TaskRepository", "Sync list $googleListId tasks failed", e)
        }
    }
}
