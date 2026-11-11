package com.example.ui.screens.home.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.RecurrenceRule
import com.example.domain.lists.AllProjectsList
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.universalDragAndDrop
import com.example.ui.screens.home.subcomponents.ProjectRowSwipe
import com.example.ui.screens.home.subcomponents.TopAppBarSelectionState
import com.example.ui.viewmodel.ThingsCategoryListState

/** Данные, из которых складывается список «Пізніших проєктів»: всегда свежие, читаются в момент жеста. */
class LaterProjectsSource(
    val projects: List<Item>,
    val areas: List<Area>,
    val templates: List<Item>,
    val rules: Map<String, RecurrenceRule>
)

/**
 * Экран «Пізніші проєкти»: выбор проектов ([selection]), «Когда» по свайпу и порядок во время перетаскивания — как
 * у задач. Режим выбора включается свайпом влево или пунктом «Вибрати» в меню тулбара (тогда и без выбранных),
 * выключается крестиком в тулбаре, «Назад», действием нижней панели или началом перетаскивания.
 *
 * @param enabled экран — «Пізніші проєкти»; на остальных экранах контроллер ничего не делает
 * @param source свежие данные списка
 */
@Stable
class LaterProjectsController(
    val enabled: Boolean,
    private val source: () -> LaterProjectsSource
) {
    /** Выбор проектов и «Когда» по свайпу вправо; режим выбора держится и без выбранных — его закрывают крестиком. */
    val selection = ProjectSelectionState(keepsModeWhenEmpty = true)

    /** Проекты сейчас выбирают протяжкой по правому краю. */
    var isDragSelecting by mutableStateOf(false)

    /** Проекты в новом порядке, пока их тянут и пока база не вернула сохранённый порядок; null — порядок из базы. */
    var orderOverride by mutableStateOf<List<Item>?>(null)
        private set

    /** Более поздние проекты без области — как на экране сейчас (во время перетаскивания — в новом порядке). */
    fun now(): List<Item> {
        val s = source()
        return AllProjectsList.content(orderOverride ?: s.projects, s.areas, templates = s.templates, rules = s.rules)
            .groups.firstOrNull { it.area == null }?.later.orEmpty()
    }

    /** Сохранённый порядок пришёл из базы — показываем его. */
    fun onProjectsStored() {
        orderOverride = null
    }

    /** Свайпы строки проекта: влево — выбор, вправо — «Когда»; пока что-то тянут, свайпы выключены. */
    fun rowSwipe(project: Item, dragDropState: GenericDragDropState) = ProjectRowSwipe(
        onSwipeLeft = { selection.swipeSelect(project) },
        onSwipeRight = { origin -> selection.openWhen(project, origin) },
        enabled = dragDropState.draggedItemKey == null
    )

    /** Верхний бар в режиме выбора проектов: крестик отмены, «Вибрати все» / «Зняти вибір», как у задач. */
    fun topBarSelection(): TopAppBarSelectionState {
        val selectable = now().filter { it.type == Item.TYPE_PROJECT }.map { it.id }.toSet()
        return TopAppBarSelectionState(
            isSelectionMode = selection.isSelectionMode,
            selectedCount = selection.selectedIds.size,
            isAllSelected = selectable.isNotEmpty() && selection.selectedIds.containsAll(selectable),
            onCancelSelection = selection::exit,
            onSelectAllClick = { selection.selectAll(selectable) },
            onDeselectAllClick = selection::deselectAll,
            onEnterSelectionMode = selection::enter
        )
    }

    // ── Перетаскивание ──

    /** Проект [dragged] встаёт на место [target] (в своём ряду, AllProjectsList.reorderLater); false — сюда нельзя. */
    internal fun move(dragged: String, target: String): Boolean {
        val moved = AllProjectsList.reorderLater(now(), dragged, target) ?: return false
        applyOrder(moved)
        return true
    }

    /** Начало перетаскивания: выбранные сворачиваются стопкой под ведущим, режим выбора закрывается — как у задач. */
    internal fun onDragStarted(dragDropState: GenericDragDropState, projectId: String) {
        val selected = selection.selectedIds
        dragDropState.stackedDragKeys =
            if (projectId in selected && selected.size > 1) listOf(projectId) + selected.filter { it != projectId }
            else emptyList()
        if (selection.isSelectionMode) selection.exit()
    }

    /** Конец перетаскивания: стопка встаёт за ведущим; возвращает проекты, которые надо сохранить. */
    internal fun onDragEnd(dragDropState: GenericDragDropState): List<Item> {
        val stack = dragDropState.stackedDragKeys.mapNotNull { it as? String }
        if (stack.size > 1) {
            AllProjectsList.placeFollowers(now(), stack.first(), stack.drop(1))?.let(::applyOrder)
        }
        val stored = source().projects.associateBy { it.id }
        val changed = orderOverride.orEmpty().filter { stored[it.id] != null && stored[it.id] != it }
        if (changed.isEmpty()) orderOverride = null
        return changed
    }

    private fun applyOrder(later: List<Item>) {
        val byId = later.associateBy { it.id }
        orderOverride = (orderOverride ?: source().projects).map { byId[it.id] ?: it }
    }
}

/**
 * Перетаскивание проекта на «Пізніших проєктах»: между отложенными или между проектами того же дня. Как у задач:
 * тянуть можно и в режиме выбора — выбранные сворачиваются стопкой под ведущим, после сброса встают за ним.
 *
 * @param onSave проекты с новым порядком (и «Когда» у стопки), которые надо сохранить
 */
fun Modifier.laterProjectDrag(
    controller: LaterProjectsController,
    dragDropState: GenericDragDropState,
    projectId: String,
    onSave: (List<Item>) -> Unit
): Modifier = universalDragAndDrop(
    state = dragDropState,
    key = projectId,
    canDropOver = { key -> key is String && controller.now().any { it.id == key } },
    onMoveIfNecessary = { dragged, target -> controller.move(dragged as String, target as String) },
    onDragStarted = { controller.onDragStarted(dragDropState, projectId) },
    onDragEnd = {
        val changed = controller.onDragEnd(dragDropState)
        if (changed.isNotEmpty()) onSave(changed)
    }
)

/**
 * Слой «Пізніших проєктів» поверх списка: выбор протяжкой по правому краю, нижняя панель действий с выбранными
 * проектами и её диалоги, «Когда» по свайпу вправо — как на главном экране.
 */
@Composable
fun BoxScope.LaterProjectsLayer(
    controller: LaterProjectsController,
    state: ThingsCategoryListState,
    lazyListState: LazyListState,
    stripWidth: Dp,
    stripTopPadding: Dp,
    stripBottomPadding: Dp,
    listOffsetY: () -> Float,
    onEvent: (ThingsCategoryListEvent) -> Unit
) {
    if (!controller.enabled) return
    val selection = controller.selection
    if (selection.isSelectionMode) {
        DragSelectStrip(
            lazyListState = lazyListState,
            width = stripWidth,
            topPadding = stripTopPadding,
            bottomPadding = stripBottomPadding,
            listOffsetY = listOffsetY,
            isSelectable = { id -> controller.now().any { it.id == id } },
            isSelected = { it in selection.selectedIds },
            onSelect = selection::set,
            onDraggingChange = { controller.isDragSelecting = it }
        )
    }
    HomeProjectBatchLayer(
        isActive = selection.isSelectionMode,
        selected = selection.selected(state.projects),
        allTasks = state.allTasks,
        projects = state.projects,
        areas = state.areas,
        onEditProjects = { list, edit -> list.forEach { onEvent(ThingsCategoryListEvent.EditProject(it, edit)) } },
        onDeleteProjects = { onEvent(ThingsCategoryListEvent.DeleteProjects(it)) },
        onCompleteProjects = { list -> list.forEach { onEvent(ThingsCategoryListEvent.CompleteProject(it)) } },
        onDuplicateProjects = { onEvent(ThingsCategoryListEvent.DuplicateProjects(it)) },
        onDone = selection::exit
    )
    selection.whenProject?.let { project ->
        ProjectSwipeWhenDialog(
            project = project,
            growFromOffset = selection.whenOrigin,
            onEditProject = { p, edit -> onEvent(ThingsCategoryListEvent.EditProject(p, edit)) },
            onDismiss = selection::closeWhen
        )
    }
}
