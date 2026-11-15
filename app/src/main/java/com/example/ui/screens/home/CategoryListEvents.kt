package com.example.ui.screens.home

import androidx.compose.ui.geometry.Rect
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.domain.edits.MoveTarget
import com.example.ui.screens.home.components.RepeatAction
import com.example.ui.screens.home.components.TaskSelectionState
import com.example.ui.screens.home.components.ThingsCategoryListEvent
import com.example.ui.viewmodel.ThingsCategoryListState
import com.example.ui.viewmodel.ThingsViewModel

/**
 * То, что по событиям экрана списка делает сам экран, а не модель: переходы между экранами и окно поиска.
 * Реализует экран ([ThingsHomeScreen]) — у него навигация и своё состояние окон.
 */
interface CategoryListScreenActions {
    fun openProject(project: Item)
    fun openArea(area: Area)
    fun openTag(tag: Tag)
    /** Открыть проект найденного подзаголовка и показать подзаголовок. */
    fun openHeading(heading: Item)
    fun openLoggedProjects()
    /** Открыть окно поиска; [sourceBounds] — откуда оно вырастает. */
    fun openSearch(sourceBounds: Rect?)
    fun back()
    /** Перейти к задаче: открыть её список и показать её. */
    fun showTask(task: ItemWithChecklist)
    /** Экран проекта закрывается (проект удалён или продублирован). */
    fun closeProjectScreen()
    /** Экран области закрывается (область удалена). */
    fun closeAreaScreen()
}

/**
 * Обработка событий экрана списка ([ThingsCategoryListEvent]): правки уходят в модель, выбор — в [taskSelection],
 * переходы и окна — экрану ([screen]). Все события перечислены явно (без else): новое событие компилятор заставит
 * обработать и здесь.
 *
 * @param state состояние экрана на момент события (выбранные и показанные задачи)
 * @param allTasks все задачи — по ним ищутся копии и шаблоны повторов
 * @param untitledTaskTitle название задачи, сохранённой без названия
 */
fun handleCategoryListEvent(
    event: ThingsCategoryListEvent,
    viewModel: ThingsViewModel,
    taskSelection: TaskSelectionState,
    state: ThingsCategoryListState,
    allTasks: List<ItemWithChecklist>,
    untitledTaskTitle: String,
    screen: CategoryListScreenActions
) {
    // Пакетное действие над выбранными задачами; после него режим выбора закрывается
    fun batch(action: (List<ItemWithChecklist>) -> Unit) {
        action(taskSelection.selected(state.allTasks))
        taskSelection.exit()
    }
    when (event) {
        // ── Переходы и окна ──
        is ThingsCategoryListEvent.ClickProject -> screen.openProject(event.project)
        is ThingsCategoryListEvent.ClickArea -> screen.openArea(event.area)
        is ThingsCategoryListEvent.ClickTag -> screen.openTag(event.tag)
        is ThingsCategoryListEvent.OpenHeading -> screen.openHeading(event.heading)
        is ThingsCategoryListEvent.OpenLoggedProjects -> screen.openLoggedProjects()
        is ThingsCategoryListEvent.ClickSearch -> screen.openSearch(event.sourceBounds)
        ThingsCategoryListEvent.ClickBack -> screen.back()
        is ThingsCategoryListEvent.ChangeSearchQuery -> viewModel.setSearchQuery(event.query)
        is ThingsCategoryListEvent.SetTagFilter -> viewModel.setTagFilter(event.filter)
        // Перенос из редактора панель уже превратила в MoveRepeat (после выбора)
        is ThingsCategoryListEvent.RequestRepeatMove -> Unit
        // Окно «Перемістити» открывает сама панель списка
        is ThingsCategoryListEvent.RequestMoveDialog -> Unit
        is ThingsCategoryListEvent.DismissInboxMoved -> viewModel.dismissInboxMoved()

        // ── Задача ──
        is ThingsCategoryListEvent.ClickTask -> viewModel.setInlineExpandedTaskId(event.task.item.id)
        is ThingsCategoryListEvent.ChangeInlineExpandedTaskId -> viewModel.setInlineExpandedTaskId(event.taskId)
        is ThingsCategoryListEvent.ToggleTask -> viewModel.toggleTaskCompletion(event.task)
        is ThingsCategoryListEvent.ToggleEditedTask ->
            viewModel.toggleEditedTask(event.task, event.fields, event.checklist, untitledTaskTitle)
        is ThingsCategoryListEvent.ToggleTaskCancel -> viewModel.toggleTaskCancel(event.taskId)
        is ThingsCategoryListEvent.SaveTask -> viewModel.updateTask(
            task = event.taskWrapper.item,
            checklist = event.checklist,
            section = event.section,
            title = event.title,
            notes = event.notes,
            isTonight = event.isTonight,
            startDate = event.startDate,
            dueDate = event.dueDate,
            tags = event.tags,
            projectId = event.projectId,
            priority = event.priority,
            reminderTime = event.reminderTime,
            untitledTitle = untitledTaskTitle
        )
        is ThingsCategoryListEvent.DeleteTask -> viewModel.deleteTask(event.taskWrapper)
        is ThingsCategoryListEvent.DuplicateTask -> viewModel.duplicateTask(event.taskWrapper)
        is ThingsCategoryListEvent.MoveTaskToNewProject ->
            viewModel.moveTasksToNewProject(listOf(event.taskWrapper), event.title)
        is ThingsCategoryListEvent.MoveTask -> viewModel.moveTask(
            event.taskWrapper,
            if (event.moveToInbox) MoveTarget.Inbox else MoveTarget.Place(event.projectId, event.areaId, event.headingId)
        )
        is ThingsCategoryListEvent.ReorderTasks -> viewModel.updateTasks(event.items)
        is ThingsCategoryListEvent.CreateTaskAt ->
            viewModel.createTaskAt(event.taskId, event.place, event.spot, event.insertIndex, event.listTasks)
        is ThingsCategoryListEvent.SwipeTaskRight -> {
            // Заглушка для When / Календаря
        }

        // ── Повторы ──
        is ThingsCategoryListEvent.MakeRepeating -> viewModel.makeRepeating(event.taskId, event.rule, event.reminderTime)
        is ThingsCategoryListEvent.UpdateRepeatRule -> viewModel.updateRepeatRule(event.templateId, event.rule, event.reminderTime)
        is ThingsCategoryListEvent.MoveRepeat -> viewModel.moveRepeat(event.templateId, event.appearOn, event.updateRule)
        is ThingsCategoryListEvent.BatchRepeatAction -> event.templateIds.forEach { id ->
            when (event.action) {
                RepeatAction.PAUSE -> viewModel.pauseRepeat(id)
                RepeatAction.RESUME -> viewModel.resumeRepeat(id)
                RepeatAction.STOP -> viewModel.stopRepeat(id)
                else -> Unit
            }
        }
        is ThingsCategoryListEvent.OpenRepeatTemplate -> {
            viewModel.setInlineExpandedTaskId(null)
            allTasks.firstOrNull { it.item.id == event.templateId }?.let { screen.showTask(it) }
        }
        is ThingsCategoryListEvent.RepeatTemplateAction -> when (event.action) {
            RepeatAction.NEXT_COPY -> viewModel.repeatNextCopy(event.templateId)
            RepeatAction.COMPLETE_EARLY -> viewModel.completeRepeatEarly(event.templateId)
            RepeatAction.PAUSE -> viewModel.pauseRepeat(event.templateId)
            RepeatAction.RESUME -> viewModel.resumeRepeat(event.templateId)
            RepeatAction.STOP -> viewModel.stopRepeat(event.templateId)
            // Предыдущая копия — самая поздняя из созданных
            RepeatAction.PREVIOUS_COPY -> {
                val previous = allTasks
                    .filter { it.item.repeatTemplateId == event.templateId && !it.item.isTemplatePart && !it.item.trashed }
                    .maxByOrNull { it.item.startDate ?: it.item.creationDate }
                if (previous != null) {
                    viewModel.setInlineExpandedTaskId(null)
                    screen.showTask(previous)
                }
            }
            RepeatAction.EDIT -> Unit
        }

        // ── Теги ──
        is ThingsCategoryListEvent.CreateTag -> viewModel.createTagInGroup(event.title, event.parentId)
        is ThingsCategoryListEvent.DeleteTag -> viewModel.deleteTag(event.tag)
        is ThingsCategoryListEvent.UpdateTag -> viewModel.updateTag(event.tag)
        is ThingsCategoryListEvent.UpdateTagsOrder -> viewModel.updateTagsOrder(event.tags)

        // ── Проекты и области ──
        is ThingsCategoryListEvent.EditProject -> viewModel.editProject(event.project, event.edit)
        is ThingsCategoryListEvent.CompleteProject -> viewModel.completeProject(event.project)
        is ThingsCategoryListEvent.DeleteProjects -> event.projects.forEach { viewModel.deleteProject(it) }
        is ThingsCategoryListEvent.DuplicateProjects -> event.projects.forEach { viewModel.duplicateProject(it) }
        is ThingsCategoryListEvent.DeleteProject -> {
            viewModel.deleteProject(event.project)
            screen.closeProjectScreen()
        }
        is ThingsCategoryListEvent.DuplicateProject -> {
            // Как в Things: копия встаёт сразу после проекта, экран проекта закрывается —
            // видно список, где появилась копия
            viewModel.duplicateProject(event.project)
            screen.closeProjectScreen()
        }
        is ThingsCategoryListEvent.RenameArea -> viewModel.updateArea(event.area.copy(title = event.title))
        // Назад — когда область уже удалена: главный экран появляется сразу без неё
        is ThingsCategoryListEvent.DeleteArea -> viewModel.deleteArea(event.area) { screen.closeAreaScreen() }
        is ThingsCategoryListEvent.SetAreaTags -> viewModel.setAreaTags(event.area, event.titles)
        is ThingsCategoryListEvent.NewProjectInArea -> viewModel.createProjectInArea(event.area.id) { screen.openProject(it) }
        is ThingsCategoryListEvent.PasteTasksToArea -> viewModel.addTasksToArea(event.titles, event.area.id)

        // ── Подзаголовки проекта ──
        is ThingsCategoryListEvent.SaveHeading -> viewModel.saveHeading(event.heading)
        is ThingsCategoryListEvent.DeleteHeading -> viewModel.deleteHeading(event.heading)
        is ThingsCategoryListEvent.DiscardHeading -> viewModel.discardHeading(event.heading, event.tasks, event.groupAboveId)
        is ThingsCategoryListEvent.ArchiveHeading -> viewModel.archiveHeading(event.heading)
        is ThingsCategoryListEvent.ReorderHeadings -> viewModel.reorderHeadings(event.headings)
        is ThingsCategoryListEvent.InsertHeadingAt ->
            viewModel.insertHeadingAt(event.headingId, event.projectId, event.headings, event.index, event.movedTasks)
        is ThingsCategoryListEvent.AddHeading -> viewModel.addHeading(event.headingId, event.projectId, event.headings)
        is ThingsCategoryListEvent.RenameHeading -> viewModel.renameHeading(event.heading, event.title)

        // ── Выбор задач ──
        is ThingsCategoryListEvent.SwipeTaskLeft -> {
            viewModel.setInlineExpandedTaskId(null)
            taskSelection.swipe(event.task.item.id)
        }
        is ThingsCategoryListEvent.EnterSelectionMode -> {
            viewModel.setInlineExpandedTaskId(null)
            taskSelection.enter(event.initialTaskId)
        }
        is ThingsCategoryListEvent.ToggleTaskSelection -> taskSelection.toggle(event.taskId)
        is ThingsCategoryListEvent.SetTaskSelected -> taskSelection.set(event.taskId, event.selected)
        ThingsCategoryListEvent.SelectAllTasks -> taskSelection.selectAll(state.displayTasks.map { it.item.id })
        ThingsCategoryListEvent.DeselectAllTasks -> taskSelection.deselectAll()
        ThingsCategoryListEvent.ExitSelectionMode -> taskSelection.exit()

        // ── Пакетные действия над выбранными задачами ──
        is ThingsCategoryListEvent.BatchCompleteTasks -> batch { viewModel.batchSetCompleted(it, event.completed) }
        ThingsCategoryListEvent.BatchDeleteTasks -> batch { selected -> viewModel.deleteTasks(selected.map { it.item }) }
        ThingsCategoryListEvent.BatchDuplicateTasks -> batch { viewModel.batchDuplicateTasks(it) }
        is ThingsCategoryListEvent.BatchScheduleTasks ->
            batch { viewModel.batchScheduleTasks(it, event.startDate, event.isTonight, event.section) }
        is ThingsCategoryListEvent.BatchMoveTasksToNewProject -> batch { viewModel.moveTasksToNewProject(it, event.title) }
        is ThingsCategoryListEvent.BatchMoveTasks ->
            batch { viewModel.batchMoveTasks(it, event.projectId, event.areaId, event.moveToInbox, event.headingId) }
        is ThingsCategoryListEvent.BatchSetTags -> batch { viewModel.batchAddTags(it, event.tags) }
        is ThingsCategoryListEvent.BatchSetDeadline -> batch { viewModel.batchSetDeadline(it, event.deadline) }
    }
}
