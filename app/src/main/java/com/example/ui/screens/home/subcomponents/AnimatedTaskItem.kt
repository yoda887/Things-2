package com.example.ui.screens.home.subcomponents

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.TaskSection
import com.example.data.model.ChecklistItem
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.components.ThingsCategoryListEvent
import com.example.ui.components.dragdrop.GenericDragDropState
import com.example.ui.components.dragdrop.draggedTaskId
import com.example.ui.screens.home.components.taskDragAndDrop
import com.example.ui.screens.home.components.UpcomingDay
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import com.example.ui.theme.dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.AppIcons
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProjectProgressArc
import com.example.ui.screens.home.components.ProjectProgress
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted
import com.example.ui.components.swipe.SwipeableTaskContainer

private const val DELETE_ANIMATION_DELAY_MS = 300L

/**
 * Анимированный и интерактивный контейнер задачи.
 * Объединяет логику анимаций перетаскивания (Drag-and-Drop), затемнения (Dimming),
 * отображения встроенного редактора задачи (ThingsTaskInlineEditor) и стандартной строки.
 */
@Composable
fun AnimatedTaskItem(
    taskWrapper: ItemWithChecklist,
    dragDropState: GenericDragDropState,
    inlineExpandedTaskId: String?,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    highlightedTaskId: String?,
    projects: List<Item>,
    areas: List<com.example.data.model.Area> = emptyList(),
    allSavedTags: List<String>,
    allSavedTagObjects: List<Tag>,
    deletedTaskIds: List<String>,
    onDeletedTaskIdAdd: (String) -> Unit,
    onEvent: (ThingsCategoryListEvent) -> Unit,
    coroutineScope: CoroutineScope,
    screen: ActiveScreen,
    upcomingDays: List<UpcomingDay>,
    localTasksList: List<ItemWithChecklist>,
    displayTasks: List<ItemWithChecklist>,
    allTasks: List<ItemWithChecklist> = emptyList(),
    projectProgressMap: Map<String, ProjectProgress> = emptyMap(),
    onLocalTasksListChange: (List<ItemWithChecklist>) -> Unit,
    lazyListState: LazyListState,
    onWhenDialogVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val task = taskWrapper.item
    val currentProject = remember(task.projectId, projects) {
        projects.firstOrNull { it.id == task.projectId }
    }
    val currentArea = remember(task.areaId, areas) {
        areas.firstOrNull { it.id == task.areaId }
    }
    val isDragTask = dragDropState.draggedTaskId == task.id
    val isExpanded = inlineExpandedTaskId == task.id
    val shouldDim = inlineExpandedTaskId != null && !isExpanded

    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS.toInt(),
            easing = FastOutSlowInEasing
        ),
        label = "expansionProgress_${task.id}"
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val verticalGapLimit = MaterialTheme.dimens.taskExpandedVerticalGap
    val verticalGapLimitPx = with(density) { verticalGapLimit.toPx() }
    val extraTopPaddingPx = with(density) {
        (MaterialTheme.dimens.taskExpandedTopPadding - MaterialTheme.dimens.taskCollapsedTopPadding).toPx()
    }
    val totalCompensationPx = verticalGapLimitPx + extraTopPaddingPx
    var prevPaddingPx by remember(task.id) { mutableStateOf(0f) }

    LaunchedEffect(expansionProgress) {
        val currentPaddingPx = verticalGapLimitPx * expansionProgress
        val delta = currentPaddingPx - prevPaddingPx
        if (delta != 0f) {
            lazyListState.dispatchRawDelta(delta)
        }
        prevPaddingPx = currentPaddingPx
    }

    val isBeingDeleted by remember(task.id) {
        derivedStateOf { deletedTaskIds.contains(task.id) }
    }

    val dimAlpha by animateFloatAsState(
        targetValue = if (shouldDim) 0.3f else 1f,
        label = "dimAlpha_${task.id}"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragTask) 1.04f else 1.0f,
        label = "dragScale_${task.id}"
    )
    val dragElevation by animateDpAsState(
        targetValue = if (isDragTask) 8.dp else (if (isExpanded || expansionProgress > 0f) 8.dp else 0.dp),
        animationSpec = tween(
            durationMillis = com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS.toInt(),
            easing = FastOutSlowInEasing
        ),
        label = "dragElev_${task.id}"
    )
    val zIndexValToUse = if (isDragTask) 100f else (if (isExpanded || expansionProgress > 0f) 1f else 0f)
    // Извлечение значения из Animatable
    val translationYVal = if (isDragTask) dragDropState.dragAccumulatedOffset.value else 0f
    val translationXVal = if (isDragTask) dragDropState.dragAccumulatedOffsetHorizontal.value else 0f

    val containerBgColor = if (isExpanded || expansionProgress > 0f || isDragTask || dragElevation > 0.dp) MaterialTheme.colorScheme.background else Color.Transparent

    val collapsedRadius = MaterialTheme.dimens.taskCollapsedCornerRadius
    val expandedRadius = MaterialTheme.dimens.taskExpandedCornerRadius
    val cornerRadiusValue = (collapsedRadius.value + (expandedRadius.value - collapsedRadius.value) * expansionProgress).dp
    val currCornerShape = RoundedCornerShape(cornerRadiusValue)

    val extraPaddingDp = 10.dp * expansionProgress

    val verticalGapPadding = MaterialTheme.dimens.taskExpandedVerticalGap * expansionProgress

    // Добавляем светло-серую подложку (плейсхолдер) на физическое место задачи во время перетаскивания (landing slot)
    Box(
        modifier = modifier
            .zIndex(zIndexValToUse)
    ) {
        if (isDragTask) {
            val isDark = isSystemInDarkTheme()
            val placeholderBgColor = if (isDark) Color(0xFF2C2D32) else Color(0xFFE5E6EB)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.5f) // Полупрозрачный
                    .zIndex(-1f) // Уровнем ниже всех задач в списке (в рамках контекста элемента)
                    .background(placeholderBgColor, currCornerShape)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = translationXVal
                    translationY = translationYVal
                    scaleX = dragScale
                    scaleY = dragScale
                    alpha = dimAlpha
                }
                .padding(top = verticalGapPadding, bottom = verticalGapPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        // Мгновенная компенсация ВСЕГО вертикального смещения при раскрытии:
                        // totalCompensationPx * progress = полное смещение (внешний зазор + внутренний top-padding)
                        // prevPaddingPx = сколько уже скомпенсировано скроллом (dispatchRawDelta)
                        // Разница компенсируется graphicsLayer мгновенно, в том же кадре отрисовки.
                        // По мере того как dispatchRawDelta догоняет, graphicsLayer плавно уменьшает компенсацию.
                        translationY = -(totalCompensationPx * expansionProgress - prevPaddingPx)
                    }
                    .layout { measurable, constraints ->
                        val extraPaddingPx = extraPaddingDp.roundToPx()
                        val extendedConstraints = constraints.copy(
                            minWidth = (constraints.minWidth + extraPaddingPx * 2).coerceAtMost(constraints.maxWidth + extraPaddingPx * 2),
                            maxWidth = (constraints.maxWidth + extraPaddingPx * 2)
                        )
                        val placeable = measurable.measure(extendedConstraints)
                        layout(placeable.width - extraPaddingPx * 2, placeable.height) {
                            placeable.place(-extraPaddingPx, 0)
                        }
                    }
                    .shadow(dragElevation, currCornerShape)
                    .background(containerBgColor, currCornerShape)
            ) {
                if (isExpanded || expansionProgress > 0f) {
                    ThingsTaskInlineEditor(
                        task = taskWrapper,
                        projects = projects,
                        allSavedTags = allSavedTags,
                        allSavedTagObjects = allSavedTagObjects,
                        onNewTagCreated = { title, parentId -> onEvent(ThingsCategoryListEvent.CreateTag(title, parentId)) },
                        onDeleteTag = { tag -> onEvent(ThingsCategoryListEvent.DeleteTag(tag)) },
                        onUpdateTag = { tag -> onEvent(ThingsCategoryListEvent.UpdateTag(tag)) },
                        onUpdateTagsOrder = { tags -> onEvent(ThingsCategoryListEvent.UpdateTagsOrder(tags)) },
                        isDeletedExternally = { deletedTaskIds.contains(task.id) },
                        isExpanded = isExpanded,
                        expansionProgress = expansionProgress,
                        screen = screen,
                        onSave = { title, notes, section, isTonight, startDate, dueDate, tags, projectId, checklist, priority ->
                            val hasPositionChange = (projectId != task.projectId) ||
                                    (isTonight != task.isTonight) ||
                                    (startDate != task.startDate) ||
                                    (dueDate != task.dueDate) ||
                                    (section != task.section)

                            if (hasPositionChange) {
                                // 1. Начинаем плавное сворачивание редактора (300 мс).
                                // Редактор в процессе сворачивания продолжает отображать НОВЫЙ заголовок и НОВУЮ дату.
                                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                                // 2. Применяем новые свойства во ViewModel ровно через 300 мс (после полного сворачивания редактора)
                                coroutineScope.launch {
                                    delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                                    onEvent(
                                        ThingsCategoryListEvent.SaveTask(
                                            taskWrapper = taskWrapper,
                                            title = title,
                                            notes = notes,
                                            section = section,
                                            isTonight = isTonight,
                                            startDate = startDate,
                                            dueDate = dueDate,
                                            tags = tags,
                                            projectId = projectId,
                                            priority = priority,
                                            checklist = checklist
                                        )
                                    )
                                }
                            } else {
                                // Если свойства позиции не менялись — обновляем всё мгновенно
                                onEvent(
                                    ThingsCategoryListEvent.SaveTask(
                                        taskWrapper = taskWrapper,
                                        title = title,
                                        notes = notes,
                                        section = section,
                                        isTonight = isTonight,
                                        startDate = startDate,
                                        dueDate = dueDate,
                                        tags = tags,
                                        projectId = projectId,
                                        priority = priority,
                                        checklist = checklist
                                    )
                                )
                                onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            }
                        },
                        onDelete = {
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            coroutineScope.launch {
                                // 1. Сначала сворачиваем открытый редактор в обычную белую строку (300 мс)
                                delay(com.example.ui.theme.AnimationConstants.TASK_EXPANSION_DURATION_MS)
                                // 2. Включаем серое закрашивание серым кругом от чекбокса и серость текста (как при чекбоксе)
                                onDeletedTaskIdAdd(task.id)
                                // 3. Ждём 500 мс (стандартный таймер выполнения чекбокса)
                                delay(500L)
                                // 4. Удаляем задачу из ViewModel -> animateItem растворяет серую карточку и подтягивает задачи
                                onEvent(ThingsCategoryListEvent.DeleteTask(taskWrapper))
                            }
                        },
                        onDone = {
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                        },
                        onWhenDialogVisibilityChange = onWhenDialogVisibilityChange,
                        areas = areas,
                        onNavigateToProject = { project ->
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            onEvent(ThingsCategoryListEvent.ClickProject(project))
                        },
                        onNavigateToArea = { area ->
                            onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                            onEvent(ThingsCategoryListEvent.ClickArea(area))
                        }
                    )
                } else {
                    SwipeableTaskContainer(
                        modifier = Modifier.taskDragAndDrop(
                            state = dragDropState,
                            taskWrapper = taskWrapper,
                            screen = screen,
                            upcomingDays = upcomingDays,
                            localTasksList = localTasksList,
                            filteredTasks = displayTasks,
                            onLocalTasksListChange = onLocalTasksListChange,
                            onTasksReordered = { items ->
                                onEvent(ThingsCategoryListEvent.ReorderTasks(items))
                            }
                        ),
                        onSwipeLeft = { onEvent(ThingsCategoryListEvent.SwipeTaskLeft(taskWrapper)) },
                        onSwipeRight = { onEvent(ThingsCategoryListEvent.SwipeTaskRight(taskWrapper)) },
                        enabled = inlineExpandedTaskId == null
                                && dragDropState.draggedTaskId == null
                                && !task.id.startsWith("cal_")
                    ) {
                        TaskItemRow(
                            modifier = Modifier,
                            task = taskWrapper.item,
                            textPrimaryColor = textPrimaryColor,
                            textSecondaryColor = textSecondaryColor,
                            dividerColor = dividerColor,
                            onToggle = { onEvent(ThingsCategoryListEvent.ToggleTask(taskWrapper)) },
                            onClick = {
                                if (inlineExpandedTaskId != null) {
                                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                                } else {
                                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(task.id))
                                }
                            },
                            projects = projects,
                            areas = areas,
                            showTodayIndicator = screen == ActiveScreen.TODAY && !task.isTonight,
                            isDragging = false,
                            dragOffsetY = 0f,
                            dragModifier = Modifier,
                            isHighlighted = task.id == highlightedTaskId,
                            isDimmed = shouldDim,
                            isBeingDeleted = isBeingDeleted,
                            screen = screen
                        )
                    }
                }
            }

        // Project/Area indicator row (drawn under/outside the Card)
        if (isExpanded || expansionProgress > 0f) {
            if (currentProject != null || currentArea != null) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isExpanded,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    val progress = if (currentProject != null) projectProgressMap[currentProject.id] else null
                    val completedCount = progress?.completed ?: 0
                    val totalCount = progress?.total ?: 0

                    val pillContentColor = Color(0xFF8E8E93)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val extraPaddingPx = extraPaddingDp.roundToPx()
                                val extendedConstraints = constraints.copy(
                                    minWidth = (constraints.minWidth + extraPaddingPx * 2).coerceAtMost(constraints.maxWidth + extraPaddingPx * 2),
                                    maxWidth = (constraints.maxWidth + extraPaddingPx * 2)
                                )
                                val placeable = measurable.measure(extendedConstraints)
                                layout(placeable.width - extraPaddingPx * 2, placeable.height) {
                                    placeable.place(-extraPaddingPx, 0)
                                }
                            }
                            .padding(top = 10.dp, bottom = 4.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .clickable {
                                    // Save task automatically and navigate
                                    onEvent(ThingsCategoryListEvent.ChangeInlineExpandedTaskId(null))
                                    if (currentProject != null) {
                                        onEvent(ThingsCategoryListEvent.ClickProject(currentProject))
                                    } else if (currentArea != null) {
                                        onEvent(ThingsCategoryListEvent.ClickArea(currentArea))
                                    }
                                }
                                .padding(start = 8.dp, top = 4.dp, end = 0.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentProject != null) {
                                ProjectProgressArc(
                                    completed = completedCount,
                                    total = totalCount,
                                    color = pillContentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = AppIcons.Area,
                                    contentDescription = null,
                                    tint = pillContentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentProject?.title ?: currentArea?.title ?: "",
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    color = pillContentColor,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = pillContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
