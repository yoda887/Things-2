package com.example.data.local

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ChecklistItem
import com.example.data.model.Tag
import com.example.data.model.ItemTag
import kotlinx.coroutines.flow.Flow

/**
 * Локальный источник данных для взаимодействия с базой данных Room.
 * Изолирует прямые запросы к [TaskDao] от репозитория и бизнес-логики.
 *
 * @property taskDao Объект доступа к данным Room [TaskDao]
 */
class LocalTaskDataSource(private val taskDao: TaskDao) {

    /**
     * Возвращает поток (Flow) со всеми объектами базы данных.
     */
    fun getAllItems(): Flow<List<Item>> {
        return taskDao.getAllItems()
    }

    /**
     * Возвращает поток (Flow) всех подпунктов чек-листов.
     */
    fun getAllChecklistItemsFlow(): Flow<List<ChecklistItem>> {
        return taskDao.getAllChecklistItemsFlow()
    }

    /**
     * Возвращает поток (Flow) всех областей (Areas).
     */
    fun getAllAreasFlow(): Flow<List<Area>> {
        return taskDao.getAllAreasFlow()
    }

    /**
     * Получает список тегов, привязанных к конкретной задаче.
     * @param itemId Идентификатор задачи
     */
    suspend fun getTagsByItemId(itemId: String): List<Tag> {
        return taskDao.getTagsByItemId(itemId)
    }

    /**
     * Находит задачу или проект по его уникальному идентификатору.
     * @param itemId Идентификатор искомого объекта
     */
    suspend fun getItemById(itemId: String): Item? {
        return taskDao.getItemById(itemId)
    }

    /**
     * Добавляет или обновляет задачу/проект в базе данных.
     * @param item Объект для вставки/обновления
     */
    suspend fun insertItem(item: Item) {
        taskDao.insertItem(item)
    }

    /**
     * Пакетная вставка нескольких задач или проектов.
     * @param items Список объектов для вставки
     */
    suspend fun insertItems(items: List<Item>) {
        taskDao.insertItems(items)
    }

    /**
     * Удаляет задачу или проект из локальной базы данных.
     * @param item Объект для удаления
     */
    suspend fun deleteItem(item: Item) {
        taskDao.deleteItem(item)
    }

    /**
     * Удаляет объект по его уникальному идентификатору.
     * @param id Идентификатор удаляемого объекта
     */
    suspend fun deleteItemById(id: String) {
        taskDao.deleteItemById(id)
    }

    /**
     * Вставляет новую область или обновляет существующую.
     * @param area Область для вставки
     */
    suspend fun insertArea(area: Area) {
        taskDao.insertArea(area)
    }

    /**
     * Удаляет указанную область из базы данных.
     * @param area Область для удаления
     */
    suspend fun deleteArea(area: Area) {
        taskDao.deleteArea(area)
    }

    /**
     * Находит тег по его уникальному идентификатору.
     * @param tagId Идентификатор тега
     */
    suspend fun getTagById(tagId: String): Tag? {
        return taskDao.getTagById(tagId)
    }

    /**
     * Вставляет новый тег или обновляет существующий.
     * @param tag Тег для добавления
     */
    suspend fun insertTag(tag: Tag) {
        taskDao.insertTag(tag)
    }

    /**
     * Синхронно (в блокирующем или приостановленном состоянии) получает все задачи/проекты.
     */
    suspend fun getAllItemsSync(): List<Item> {
        return taskDao.getAllItemsSync()
    }

    /**
     * Возвращает поток корневых групп тегов (у которых parentId == null).
     */
    fun observeGroups(): Flow<List<Tag>> {
        return taskDao.observeGroups()
    }

    /**
     * Возвращает поток тегов, принадлежащих конкретной группе.
     * @param parentId Идентификатор родительской группы
     */
    fun observeByParent(parentId: String): Flow<List<Tag>> {
        return taskDao.observeByParent(parentId)
    }

    /**
     * Находит все существующие теги в базе данных.
     */
    suspend fun getAllTags(): List<Tag> {
        return taskDao.getAllTags()
    }

    /**
     * Возвращает поток всех тегов.
     */
    fun getAllTagsFlow(): Flow<List<Tag>> {
        return taskDao.getAllTagsFlow()
    }

    /**
     * Удаляет указанный тег.
     * @param tag Тег для удаления
     */
    suspend fun deleteTag(tag: Tag) {
        taskDao.deleteTag(tag)
    }

    /**
     * Пакетная вставка списка тегов.
     * @param tags Список тегов
     */
    suspend fun insertTags(tags: List<Tag>) {
        taskDao.insertTags(tags)
    }

    /**
     * Удаляет связи тегов с конкретной задачей.
     * @param itemId Идентификатор задачи
     */
    suspend fun deleteItemTagsByItemId(itemId: String) {
        taskDao.deleteItemTagsByItemId(itemId)
    }

    /**
     * Добавляет связь между тегом и задачей.
     * @param itemTag Объект связи (itemId, tagId)
     */
    suspend fun insertItemTag(itemTag: ItemTag) {
        taskDao.insertItemTag(itemTag)
    }

    /**
     * Удаляет все подпункты чек-листа для указанной задачи.
     * @param itemId Идентификатор задачи
     */
    suspend fun deleteChecklistItemsByItemId(itemId: String) {
        taskDao.deleteChecklistItemsByItemId(itemId)
    }

    /**
     * Удаляет подпункты чек-листа, не входящие в список сохраняемых идентификаторов.
     * @param itemId Идентификатор задачи
     * @param keptIds Список сохраняемых идентификаторов подпунктов
     */
    suspend fun deleteRemovedChecklistItems(itemId: String, keptIds: List<String>) {
        taskDao.deleteRemovedChecklistItems(itemId, keptIds)
    }

    /**
     * Пакетно вставляет или обновляет подпункты чек-листов.
     * @param list Список подпунктов чек-листа
     */
    suspend fun insertChecklistItems(list: List<ChecklistItem>) {
        taskDao.insertChecklistItems(list)
    }

    /**
     * Возвращает общее количество подпунктов чек-листа для конкретной задачи.
     * @param itemId Идентификатор задачи
     */
    suspend fun getTotalChecklistCount(itemId: String): Int {
        return taskDao.getTotalChecklistCount(itemId)
    }

    /**
     * Возвращает количество незавершенных подпунктов чек-листа для конкретной задачи.
     * @param itemId Идентификатор задачи
     */
    suspend fun getOpenChecklistCount(itemId: String): Int {
        return taskDao.getOpenChecklistCount(itemId)
    }

    /**
     * Обновляет счетчики подпунктов в родительской задаче.
     * @param itemId Идентификатор задачи
     * @param total Общее число пунктов
     * @param open Число незавершенных пунктов
     */
    suspend fun updateChecklistCounters(itemId: String, total: Int, open: Int) {
        taskDao.updateChecklistCounters(itemId, total, open)
    }

    /**
     * Находит локальные проекты, которые еще не были синхронизированы с Google Tasks.
     */
    suspend fun getUnsyncedProjects(): List<Item> {
        return taskDao.getUnsyncedProjects()
    }

    /**
     * Находит локальные задачи, которые еще не были синхронизированы с Google Tasks.
     */
    suspend fun getUnsyncedTasks(): List<Item> {
        return taskDao.getUnsyncedTasks()
    }

    /**
     * Ищет задачу по ее внешнему идентификатору Google Tasks.
     * @param googleTaskId Идентификатор задачи на серверах Google
     */
    suspend fun getItemByGoogleTaskId(googleTaskId: String): Item? {
        return taskDao.getItemByGoogleTaskId(googleTaskId)
    }
}
