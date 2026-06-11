package com.example.data.remote

/**
 * Удаленный источник данных для взаимодействия с Google Tasks API.
 * Обертывает сетевые вызовы к интерфейсу [GoogleTasksService], скрывая непосредственные детали работы с Retrofit.
 *
 * @property api Реализация интерфейса запросов Retrofit [GoogleTasksService]
 */
class RemoteTaskDataSource(private val api: GoogleTasksService) {

    /**
     * Запрашивает списки задач пользователя (Task Lists) из Google Tasks API.
     * @param authHeader Заголовок авторизации вида "Bearer <access_token>"
     */
    suspend fun getTaskLists(authHeader: String): GoogleTaskListResponse {
        return api.getTaskLists(authHeader)
    }

    /**
     * Создает новый список задач (Task List) в Google Tasks API.
     * @param authHeader Заголовок авторизации вида "Bearer <access_token>"
     * @param list Данные нового списка (например, mapOf("title" to name))
     */
    suspend fun createTaskList(authHeader: String, list: Map<String, String>): GoogleTaskList {
        return api.createTaskList(authHeader, list)
    }

    /**
     * Запрашивает список всех задач для конкретного списка задач.
     * @param authHeader Заголовок авторизации вида "Bearer <access_token>"
     * @param tasklistId Идентификатор списка задач
     */
    suspend fun getTasks(authHeader: String, tasklistId: String): GoogleTasksResponse {
        return api.getTasks(authHeader, tasklistId)
    }

    /**
     * Создает новую задачу в рамках указанного списка в Google Tasks API.
     * @param authHeader Заголовок авторизации вида "Bearer <access_token>"
     * @param tasklistId Идентификатор списка задач
     * @param task Объект задачи [GoogleTask]
     */
    suspend fun createTask(authHeader: String, tasklistId: String, task: GoogleTask): GoogleTask {
        return api.createTask(authHeader, tasklistId, task)
    }

    /**
     * Обновляет состояние существующей задачи в Google Tasks API.
     * @param authHeader Заголовок авторизации вида "Bearer <access_token>"
     * @param tasklistId Идентификатор списка задач
     * @param taskId Идентификатор обновляемой задачи
     * @param task Новые данные задачи [GoogleTask]
     */
    suspend fun updateTask(authHeader: String, tasklistId: String, taskId: String, task: GoogleTask): GoogleTask {
        return api.updateTask(authHeader, tasklistId, taskId, task)
    }

    /**
     * Удаляет указанную задачу из Google Tasks.
     * @param authHeader Заголовок авторизации вида "Bearer <access_token>"
     * @param tasklistId Идентификатор списка задач
     * @param taskId Идентификатор удаляемой задачи
     */
    suspend fun deleteTask(authHeader: String, tasklistId: String, taskId: String) {
        api.deleteTask(authHeader, tasklistId, taskId)
    }
}
