package com.example.data.repository

import com.example.data.remote.GoogleTasksApi
import javax.inject.Inject
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
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.domain.tag.TagTitles

/**
 * Реализация репозитория управления задачами, проектами и областями.
 * Реализует интерфейс [ITaskRepository], отделяя источник данных Room, внешний API Google Tasks
 * и локальный провайдер событий календаря устройства.
 *
 * @property localDataSource Локальный источник данных (БД Room)
 * @property remoteDataSource Удаленный источник данных (Google Tasks API)
 * @property calendarDataSource Источник данных календаря устройства
 * @property database База Room — для транзакций, объединяющих несколько записей
 */
class TaskRepositoryImpl @Inject constructor(
    private val localDataSource: LocalTaskDataSource,
    private val remoteDataSource: RemoteTaskDataSource,
    private val calendarDataSource: DeviceCalendarDataSource,
    private val database: AppDatabase
) : ITaskRepository {

    // Внутри транзакции можно звать только suspend-методы DAO: Room сам переносит их на поток
    // транзакции, даже из-под withContext(Dispatchers.IO). Блокирующий вызов DAO с другого потока
    // ждал бы окончания транзакции, а она — его.
    override suspend fun <R> inTransaction(block: suspend () -> R): R = database.withTransaction { block() }

    override fun observeAllTasks(): Flow<List<ItemWithChecklist>> = combine(
        localDataSource.getAllItems(),
        localDataSource.getAllChecklistItemsFlow()
    ) { items, checklistItems ->
        val checklistMap = checklistItems.groupBy { it.itemId }
        // Заголовки проектов идут отдельным потоком (observeHeadings)
        items.filter { !it.isHeading }.map { item ->
            ItemWithChecklist(
                item = item,
                checklist = checklistMap[item.id] ?: emptyList()
            )
        }
    }

    override fun observeAllProjects(): Flow<List<Item>> = localDataSource.getAllItems().map { items ->
        items.filter { it.type == Item.TYPE_PROJECT }
    }

    override fun observeHeadings(): Flow<List<Item>> = localDataSource.getAllItems().map { items ->
        items.filter { it.isHeading }
    }

    override suspend fun archiveHeading(heading: Item): Unit = withContext(Dispatchers.IO) {
        inTransaction {
            val now = System.currentTimeMillis()
            val openTasks = localDataSource.getAllItemsSync()
                .filter { it.headingId == heading.id && !it.isHeading && it.status == Item.STATUS_OPEN }
                .map { it.copy(status = Item.STATUS_COMPLETED, stopDate = now, modificationDate = now) }
            val archived = heading.copy(status = Item.STATUS_COMPLETED, stopDate = now, modificationDate = now)
            localDataSource.insertItems(openTasks + archived)
        }
    }

    override suspend fun reorderHeadings(headings: List<Item>): Unit = withContext(Dispatchers.IO) {
        val changed = headings.mapIndexedNotNull { index, heading ->
            if (heading.sortOrder != index) heading.copy(sortOrder = index) else null
        }
        if (changed.isNotEmpty()) localDataSource.insertItems(changed)
    }

    override fun observeAllAreas(): Flow<List<Area>> = localDataSource.getAllAreasFlow()

    override suspend fun refreshCachedTags(itemId: String): Unit = withContext(Dispatchers.IO) {
        val tags = localDataSource.getTagsByItemId(itemId)
        val cachedString = TagTitles.join(tags.map { it.title })
        val item = localDataSource.getItemById(itemId)
        if (item != null && item.cachedTags != cachedString) {
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

    // Хирургическое исправление: обновляем чек-лист только если он передан явно (не равен null)
    override suspend fun insertTask(item: Item, checklist: List<ChecklistItem>?): Unit = withContext(Dispatchers.IO) {
        localDataSource.insertItem(detachForeignHeadings(listOf(item)).single())
        if (checklist != null) {
            updateChecklistItems(item.id, checklist)
        }
    }

    override suspend fun insertTasks(items: List<Item>): Unit = withContext(Dispatchers.IO) {
        localDataSource.insertItems(detachForeignHeadings(items))
    }

    /**
     * Задача, перенесённая в другой проект (или из проекта), теряет заголовок прежнего проекта.
     * Проверка здесь, а не на каждом пути переноса: редактор, «Переместить», пакетный перенос.
     */
    private suspend fun detachForeignHeadings(items: List<Item>): List<Item> {
        if (items.none { it.headingId != null && !it.isHeading }) return items
        val headingProjects = localDataSource.getAllItemsSync()
            .filter { it.isHeading }
            .associate { it.id to it.projectId }
        return items.map { item ->
            val headingId = item.headingId
            if (headingId != null && !item.isHeading && headingProjects[headingId] != item.projectId) {
                item.copy(headingId = null)
            } else {
                item
            }
        }
    }

    override suspend fun deleteTask(item: Item): Unit = withContext(Dispatchers.IO) {
        localDataSource.deleteItem(item)
    }

    override suspend fun deleteTasks(items: List<Item>): Unit = withContext(Dispatchers.IO) {
        localDataSource.deleteItems(items)
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
        inTransaction { saveTag(tag) }
    }

    /**
     * Создание, переименование и перенос тега. Названия уникальны без учёта регистра:
     * новый тег с уже занятым названием не создаётся, а переименование в занятое название
     * сливает тег с существующим — иначе задачи расходились бы между двумя одинаковыми тегами.
     * Новый тег встаёт в конец списка, а с [atTop] — в начало: так его сразу видно в окне тегов.
     */
    private suspend fun saveTag(tag: Tag, atTop: Boolean = false): Tag? {
        val title = tag.title.trim()
        if (title.isEmpty()) return null
        val allTags = localDataSource.getAllTags()
        val old = allTags.firstOrNull { it.id == tag.id }
        val clash = TagTitles.find(allTags, title, exceptId = tag.id)

        if (old == null) {
            if (clash != null) return clash
            val created = tag.copy(
                title = title,
                sortOrder = if (atTop) {
                    (allTags.minOfOrNull { it.sortOrder } ?: 1) - 1
                } else {
                    (allTags.maxOfOrNull { it.sortOrder } ?: -1) + 1
                }
            )
            localDataSource.insertTag(created)
            return created
        }

        if (clash != null) {
            mergeTag(from = old, into = clash)
            return clash
        }

        val updated = tag.copy(title = title)
        localDataSource.insertTag(updated)
        if (old.title != title) {
            updateItemsTags { titles -> TagTitles.rename(titles, old.title, title) }
        }
        return updated
    }

    /** Переносит задачи и дочерние теги [from] на [into] и удаляет [from]. */
    private suspend fun mergeTag(from: Tag, into: Tag) {
        localDataSource.getAllItemTags()
            .filter { it.tagId == from.id }
            .forEach { localDataSource.insertItemTag(ItemTag(it.itemId, into.id)) }
        localDataSource.reparentTags(from.id, into.id)
        localDataSource.deleteTag(from)
        updateItemsTags { titles -> TagTitles.rename(titles, from.title, into.title) }
    }

    /**
     * Меняет строки тегов у всех задач по [transform] и сохраняет одним пакетом —
     * только изменившиеся задачи.
     */
    private suspend fun updateItemsTags(transform: (List<String>) -> List<String>) {
        val changed = localDataSource.getAllItemsSync().mapNotNull { item ->
            val newCached = TagTitles.join(transform(TagTitles.parse(item.cachedTags)))
            if (newCached != item.cachedTags) item.copy(cachedTags = newCached) else null
        }
        if (changed.isNotEmpty()) localDataSource.insertItems(changed)
    }

    override suspend fun createGroup(groupName: String): Tag = withContext(Dispatchers.IO) {
        inTransaction { saveTag(Tag(title = groupName, parentId = null)) } ?: Tag(title = groupName)
    }

    override suspend fun createTagInGroup(tagName: String, parentId: String?): Tag = withContext(Dispatchers.IO) {
        inTransaction { saveTag(Tag(title = tagName, parentId = parentId), atTop = true) } ?: Tag(title = tagName, parentId = parentId)
    }

    override suspend fun moveTagToGroup(tagId: String, newGroupId: String?): Unit = withContext(Dispatchers.IO) {
        val tag = localDataSource.getTagById(tagId)
        if (tag != null && tag.id != newGroupId) {
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
        inTransaction {
            val allTags = localDataSource.getAllTags()
            // Удаляется тег вместе со всеми вложенными
            val tagsToDelete = mutableSetOf<Tag>()
            fun addTagAndChildren(t: Tag) {
                if (tagsToDelete.add(t)) {
                    allTags.filter { it.parentId == t.id }.forEach { addTagAndChildren(it) }
                }
            }
            addTagAndChildren(allTags.firstOrNull { it.id == tag.id } ?: tag)
            tagsToDelete.forEach { localDataSource.deleteTag(it) }
            val titles = tagsToDelete.map { it.title }
            updateItemsTags { current -> TagTitles.remove(current, titles) }
        }
    }

    override suspend fun updateTagsOrder(tags: List<Tag>): Unit = withContext(Dispatchers.IO) {
        inTransaction {
            // Только теги из справочника: в списке диалога бывают временные записи для названий,
            // которые есть у задачи, но ещё не заведены в справочник
            val existingIds = localDataSource.getAllTags().map { it.id }.toSet()
            val toSave = tags.filter { it.id in existingIds && it.parentId != it.id }
            if (toSave.isNotEmpty()) localDataSource.insertTags(toSave)
        }
    }

    override suspend fun updateItemTags(itemId: String, tags: List<Tag>): Unit = withContext(Dispatchers.IO) {
        localDataSource.deleteItemTagsByItemId(itemId)
        for (tag in tags) {
            localDataSource.insertItemTag(ItemTag(itemId, tag.id))
        }
        refreshCachedTags(itemId)
    }

    override suspend fun setItemTagsByTitles(itemId: String, titles: List<String>): Unit = withContext(Dispatchers.IO) {
        linkItemTags(itemId, titles, localDataSource.getAllTags().toMutableList())
    }

    /**
     * Привязывает к задаче теги по названиям: находит существующие без учёта регистра, недостающие
     * заводит в справочник, пересобирает связи и нормализует строку тегов задачи.
     * [knownTags] — справочник; дополняется созданными тегами, чтобы пакетные вызовы не читали его заново.
     */
    private suspend fun linkItemTags(itemId: String, titles: List<String>, knownTags: MutableList<Tag>) {
        val resolution = TagTitles.resolve(titles, knownTags)
        if (resolution.created.isNotEmpty()) {
            localDataSource.insertTags(resolution.created)
            knownTags += resolution.created
        }
        val currentIds = localDataSource.getTagsByItemId(itemId).map { it.id }.toSet()
        val newIds = resolution.tags.map { it.id }.toSet()
        if (currentIds != newIds) {
            localDataSource.deleteItemTagsByItemId(itemId)
            resolution.tags.forEach { localDataSource.insertItemTag(ItemTag(itemId, it.id)) }
        }
        val item = localDataSource.getItemById(itemId) ?: return
        val newCached = TagTitles.join(resolution.tags.map { it.title })
        if (item.cachedTags != newCached) {
            localDataSource.insertItem(item.copy(cachedTags = newCached))
        }
    }

    override suspend fun repairTags(): Unit = withContext(Dispatchers.IO) {
        inTransaction {
            // 1. Сливаем дубли справочника, оставшиеся от прежних версий
            val tags = localDataSource.getAllTags().toMutableList()
            TagTitles.duplicates(tags).forEach { (duplicate, keep) ->
                localDataSource.getAllItemTags()
                    .filter { it.tagId == duplicate.id }
                    .forEach { localDataSource.insertItemTag(ItemTag(it.itemId, keep.id)) }
                localDataSource.reparentTags(duplicate.id, keep.id)
                localDataSource.deleteTag(duplicate)
                tags.remove(duplicate)
            }
            // 2. Восстанавливаем связи задач со справочником по их строке тегов — ей верит интерфейс.
            // Связи могли пропасть: раньше каждое переименование и перестановка тега удаляли их каскадом
            val linkedItemIds = localDataSource.getAllItemTags().map { it.itemId }.toSet()
            localDataSource.getAllItemsSync()
                .filter { it.cachedTags.isNotBlank() || it.id in linkedItemIds }
                .forEach { item -> linkItemTags(item.id, TagTitles.parse(item.cachedTags), tags) }
        }
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
            val authHeader = GoogleTasksApi.authHeader(accessToken)
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
                if (remoteList.id == GoogleTasksApi.DEFAULT_LIST_ID) continue
                val existingProject = localDataSource.getItemById(remoteList.id)
                if (existingProject == null) {
                    val newProject = Item(
                        id = remoteList.id,
                        type = Item.TYPE_PROJECT,
                        title = remoteList.title,
                        googleTaskListId = remoteList.id
                    )
                    localDataSource.insertItem(newProject)
                }
            }

            // 3. Синхронизируем список задач "по умолчанию" (Inbox)
            syncListTasks(authHeader, GoogleTasksApi.DEFAULT_LIST_ID, null)

            // 4. Синхронизируем задачи для каждого пользовательского проекта
            for (remoteList in remoteLists) {
                if (remoteList.id == GoogleTasksApi.DEFAULT_LIST_ID) continue
                syncListTasks(authHeader, remoteList.id, remoteList.id)
            }

            // 5. Отправляем на сервер Google все локально созданные несохраненные задачи
            val unsyncedTasks = localDataSource.getUnsyncedTasks()
            for (task in unsyncedTasks) {
                try {
                    val listId = if (task.projectId != null) {
                        localDataSource.getItemById(task.projectId)?.googleTaskListId ?: GoogleTasksApi.DEFAULT_LIST_ID
                    } else {
                        GoogleTasksApi.DEFAULT_LIST_ID
                    }

                    val gTask = GoogleTask(
                        title = task.title,
                        notes = task.notes,
                        status = if (task.isCompleted) GoogleTasksApi.STATUS_COMPLETED else GoogleTasksApi.STATUS_NEEDS_ACTION
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
                val isCompletedRemote = remoteTask.status == GoogleTasksApi.STATUS_COMPLETED
                val notesRemote = remoteTask.notes ?: ""
                val titleRemote = remoteTask.title

                if (localTask == null) {
                    val defaultStart = if (projectId == null) 0 else 2 // 0 = Inbox, 2 = Anytime
                    val newTask = Item(
                        type = Item.TYPE_TASK,
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
