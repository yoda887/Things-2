package com.example.domain.repository

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.Tag
import com.example.data.model.ChecklistItem
import com.example.data.model.ItemWithChecklist
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория управления задачами, проектами, областями и тегами.
 * Контракт слоя Domain, определяющий поведение репозитория независимо от способа хранения данных.
 */
interface ITaskRepository {

    /**
     * Поток всех задач с их чек-листами.
     */
    fun observeAllTasks(): Flow<List<ItemWithChecklist>>

    /**
     * Поток всех проектов (тип = 1).
     */
    fun observeAllProjects(): Flow<List<Item>>

    /**
     * Поток всех областей (Areas).
     */
    fun observeAllAreas(): Flow<List<Area>>

    /**
     * Пересчитывает и кэширует строковое представление тегов для задачи.
     * @param itemId Идентификатор задачи
     */
    suspend fun refreshCachedTags(itemId: String)

    /**
     * Извлекает общее и незавершенное количество пунктов чек-листа и обновляет связанные счетчики в задаче.
     * @param itemId Идентификатор задачи
     */
    suspend fun refreshChecklistCounters(itemId: String)

    /**
     * Извлекает локальные события календаря из системного контент-провайдера.
     * @return [Result] со списком событий календаря
     */
    suspend fun fetchLocalCalendarEvents(): Result<List<Item>>

    /**
     * Создает или обновляет задачу локально с возможностью передать связанный чек-лист.
     * @param item Объект задачи
     * @param checklist Список пунктов чек-листа (null, если обновлять чек-лист не требуется)
     */
    // Хирургическое исправление: делаем checklist nullable для предотвращения стирания при обновлении задачи
    suspend fun insertTask(item: Item, checklist: List<ChecklistItem>? = null)

    /**
     * Вставляет список задач в пакетном режиме.
     * @param items Список задач для вставки
     */
    suspend fun insertTasks(items: List<Item>)

    /**
     * Удаляет задачу из локальной базы данных.
     * @param item Объект удаляемой задачи
     */
    suspend fun deleteTask(item: Item)

    /**
     * Удаляет задачу по ее идентификатору.
     * @param id Уникальный идентификатор задачи
     */
    suspend fun deleteTaskById(id: String)

    /**
     * Вставляет или обновляет проект.
     * @param project Проект для записи
     */
    suspend fun insertProject(project: Item)

    /**
     * Удаляет проект из базы данных.
     * @param project Удаляемый проект
     */
    suspend fun deleteProject(project: Item)

    /**
     * Создает или обновляет область дел (Area).
     * @param area Сохраняемая область дел
     */
    suspend fun insertArea(area: Area)

    /**
     * Удаляет область дел.
     * @param area Удаляемая область дел
     */
    suspend fun deleteArea(area: Area)

    /**
     * Создает или обновляет тег с каскадной синхронизацией имени в связанных строках кэша.
     * @param tag Новый или обновленный тег
     */
    suspend fun insertTag(tag: Tag)

    /**
     * Создает корневой тег/группу.
     * @param groupName Имя группы тегов
     */
    suspend fun createGroup(groupName: String): Tag

    /**
     * Создает дочерний тег в рамках определенной группы.
     * @param tagName Имя тега
     * @param parentId Идентификатор родительской группы
     */
    suspend fun createTagInGroup(tagName: String, parentId: String?): Tag

    /**
     * Перемещает тег в другую группу или на верхний уровень.
     * @param tagId Идентификатор перемещаемого тега
     * @param newGroupId Идентификатор целевой группы (или null, если на верхний уровень)
     */
    suspend fun moveTagToGroup(tagId: String, newGroupId: String?)

    /**
     * Получает поток корневых тегов/групп.
     */
    fun observeGroups(): Flow<List<Tag>>

    /**
     * Получает поток дочерних тегов по родителю.
     * @param parentId Идентификатор родительской группы
     */
    fun observeByParent(parentId: String): Flow<List<Tag>>

    /**
     * Удаляет выбранный тег каскадно (включая дочерние теги) и обновляет кэш связанных задач.
     * @param tag Удаляемый тег
     */
    suspend fun deleteTag(tag: Tag)

    /**
     * Пакетно обновляет порядок следования тегов.
     * @param tags Список упорядоченных тегов
     */
    suspend fun updateTagsOrder(tags: List<Tag>)

    /**
     * Обновляет привязаные теги для выбранной задачи.
     * @param itemId Идентификатор задачи
     * @param tags Новый список привязываемых тегов
     */
    suspend fun updateItemTags(itemId: String, tags: List<Tag>)

    /**
     * Возвращает список тегов, привязанных к конкретной задаче.
     * @param itemId Идентификатор задачи
     */
    suspend fun getTagsByItemId(itemId: String): List<Tag>

    /**
     * Возвращает полный список существующих тегов.
     */
    suspend fun getAllTags(): List<Tag>

    /**
     * Возвращает поток (Flow) со всеми тегами в системе.
     */
    fun getAllTagsFlow(): Flow<List<Tag>>

    /**
     * Обновляет список подпунктов чек-листа для конкретной задачи с сохранением сортировки.
     * @param itemId Идентификатор задачи
     * @param list Список подпунктов чек-листа
     */
    suspend fun updateChecklistItems(itemId: String, list: List<ChecklistItem>)

    /**
     * Проводит двустороннюю синхронизацию списков и задач между локальной базой данных и сервером Google Tasks.
     * @param accessToken Маркер OAuth доступа к Google API
     */
    suspend fun syncWithGoogleTasks(accessToken: String): Result<Unit>
}
