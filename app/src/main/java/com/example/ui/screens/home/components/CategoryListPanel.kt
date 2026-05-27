package com.example.ui.screens.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.Project
import com.example.data.model.Task
import com.example.data.model.TaskSection
import com.example.ui.components.ProjectProgressArc
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.subcomponents.TaskItemRow
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThingsViewModel

@Composable
fun ThingsCategoryListPanel(
    screen: ActiveScreen,
    project: Project?,
    tasks: List<Task>,
    allTags: Set<String>,
    selectedTag: String?,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onTagSelect: (String?) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    projects: List<Project>,
    viewModel: ThingsViewModel,
    inlineExpandedTaskId: String?,
    onInlineExpandedTaskIdChange: (String?) -> Unit
) {
    val listTasks = remember(tasks, screen, project) {
        tasks.filter { task ->
            when (screen) {
                ActiveScreen.INBOX -> task.section == TaskSection.INBOX && !task.isCompleted
                ActiveScreen.TODAY -> task.section == TaskSection.TODAY && !task.isCompleted
                ActiveScreen.UPCOMING -> task.section == TaskSection.UPCOMING && !task.isCompleted
                ActiveScreen.ANYTIME -> task.section == TaskSection.ANYTIME && !task.isCompleted
                ActiveScreen.SOMEDAY -> task.section == TaskSection.SOMEDAY && !task.isCompleted
                ActiveScreen.LOGBOOK -> task.isCompleted
                ActiveScreen.PROJECT_DETAIL -> task.projectId == project?.id && !task.isCompleted
                else -> false
            }
        }.sortedBy { it.creationDate }
    }

    val calendarEvents by viewModel.calendarEvents.collectAsState()

    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    val scaleFactor = if (isLargeScreen) 1.25f else 1.0f

    val headerEmojiFontSize = MaterialTheme.typography.displayMedium.fontSize
    val headerTitleFontSize = MaterialTheme.typography.displayLarge.fontSize
    val subHeaderFontSize = MaterialTheme.typography.headlineSmall.fontSize

    val filteredTasks = remember(listTasks, selectedTag) {
        if (selectedTag == null) listTasks else listTasks.filter { it.tags.contains(selectedTag) }
    }

    val lazyListState = rememberLazyListState()
    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    var dragAccumulatedOffset by remember { mutableStateOf(0f) }
    var localTasksList by remember { mutableStateOf(filteredTasks) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(filteredTasks) {
        localTasksList = filteredTasks
    }

    val makeDragModifier = { task: Task ->
        Modifier.pointerInput(task.id, task.creationDate) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    draggedTaskId = task.id
                    dragAccumulatedOffset = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragAccumulatedOffset += dragAmount.y

                    val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                    val draggedItemInfo = visibleItems.firstOrNull { it.key == task.id }
                    if (draggedItemInfo != null) {
                        val dragCenterY = draggedItemInfo.offset + draggedItemInfo.size / 2f + dragAccumulatedOffset
                        val hoveredItem = visibleItems.firstOrNull { item ->
                            val itemKey = item.key as? String
                            itemKey != null && itemKey != task.id &&
                            dragCenterY > item.offset &&
                            dragCenterY < item.offset + item.size
                        }
                        if (hoveredItem != null) {
                            val fromIndex = localTasksList.indexOfFirst { it.id == task.id }
                            
                            // [ВОЗВРАТ ИЗМЕНЕНИЙ]: Возвращена оригинальная логика перетаскивания.
                            // Теперь при протаскивании элемента через заголовки или другие элементы, 
                            // свойство isTonight и порядок задач обновляются моментально "на лету", предотвращая прыжки.
                            if (hoveredItem.key == "evening_header") {
                                if (fromIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    if (!movedItem.isTonight) {
                                        movedItem = movedItem.copy(isTonight = true)
                                        val firstEveningIndex = newList.indexOfFirst { it.isTonight }
                                        val toIndex = if (firstEveningIndex != -1) firstEveningIndex else newList.size
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        // [ИСПРАВЛЕНИЕ ПРЫЖКА]: При перемещении из дневной секции (сверху) в вечернюю (снизу) через заголовок "evening_header",
                                        // удаление элемента сверху сдвигает заголовок вверх на размер этого элемента.
                                        // Чтобы скомпенсировать это смещение и предотвратить "прыжок", вычитаем размер перетаскиваемого элемента из дистанции.
                                        val distance = (hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset - draggedItemInfo.size
                                        dragAccumulatedOffset -= distance
                                    } else {
                                        // [ИСПРАВЛЕНИЕ СМЕЩЕНИЯ ВВЕРХ]: Когда вечерняя задача перетаскивается вверх
                                        // через заголовок "evening_header", мы сразу делаем её дневной (isTonight = false).
                                        // Это обеспечивает моментальное смещение и размещение задачи в конце списка дневных задач
                                        // (прямо над заголовком "This Evening"), исключая задержку при перетаскивании.
                                        movedItem = movedItem.copy(isTonight = false)
                                        val firstEveningIndex = newList.indexOfFirst { it.isTonight }
                                        val toIndex = if (firstEveningIndex != -1) firstEveningIndex else newList.size
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        // Смещение вычисляется от текущего уровня заголовка "This Evening"
                                        val distance = hoveredItem.offset - draggedItemInfo.offset
                                        dragAccumulatedOffset -= distance
                                    }
                                }
                            } else if (hoveredItem.key == "main_header") {
                                if (fromIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    if (movedItem.isTonight) {
                                        movedItem = movedItem.copy(isTonight = false)
                                        val toIndex = 0
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        val distance = (hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset
                                        dragAccumulatedOffset -= distance
                                    }
                                }
                            } else {
                                val toIndex = localTasksList.indexOfFirst { it.id == hoveredItem.key }
                                if (fromIndex != -1 && toIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    
                                    val hoveredItemTask = localTasksList.firstOrNull { it.id == hoveredItem.key }
                                    if (hoveredItemTask != null && hoveredItemTask.isTonight != movedItem.isTonight) {
                                        movedItem = movedItem.copy(isTonight = hoveredItemTask.isTonight)
                                    }
                                    
                                    newList.add(toIndex, movedItem)
                                    localTasksList = newList

                                    val distance = hoveredItem.offset - draggedItemInfo.offset
                                    dragAccumulatedOffset -= distance
                                }
                            }
                        }
                    }
                },
                onDragEnd = {
                    val baseTime = System.currentTimeMillis() - localTasksList.size * 1000L
                    val updatedList = localTasksList.mapIndexed { index, t ->
                        val newTime = baseTime + index * 1000L
                        t.copy(creationDate = newTime)
                    }
                    val changedTasks = updatedList.filter { t ->
                        t != filteredTasks.firstOrNull { it.id == t.id }
                    }
                    localTasksList = updatedList
                    if (changedTasks.isNotEmpty()) {
                        viewModel.updateTasks(changedTasks)
                    }
                    draggedTaskId = null
                    dragAccumulatedOffset = 0f
                },
                onDragCancel = {
                    draggedTaskId = null
                    dragAccumulatedOffset = 0f
                }
            )
        }
    }

    val standardToday = remember(localTasksList) { localTasksList.filter { !it.isTonight } }
    val eveningToday = remember(localTasksList) { localTasksList.filter { it.isTonight } }

    val anyExpanded = inlineExpandedTaskId != null
    val globalDimAlpha by animateFloatAsState(targetValue = if (anyExpanded) 0.3f else 1f, label = "globalDim")

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    onInlineExpandedTaskIdChange(null)
                    focusManager.clearFocus()
                })
            }
            .padding(horizontal = 20.dp)
            .testTag("tasks_lazy_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "main_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 14.dp)
                    .graphicsLayer { alpha = globalDimAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (screen) {
                    ActiveScreen.TODAY -> {
                        Text(
                            text = "⭐",
                            fontSize = headerEmojiFontSize,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Today",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.INBOX -> {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF1B80FA),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Inbox",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.UPCOMING -> {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFFF35F50),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Upcoming",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.ANYTIME -> {
                        Icon(
                            imageVector = Icons.Outlined.Archive,
                            contentDescription = null,
                            tint = Color(0xFF2EB7CD),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Anytime",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.SOMEDAY -> {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = Color(0xFF8F93A3),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Someday",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.LOGBOOK -> {
                        Icon(
                            imageVector = Icons.Outlined.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = Color(0xFF2EC275),
                            modifier = Modifier.size((32 * scaleFactor).dp).padding(end = 10.dp)
                        )
                        Text(
                            text = "Logbook",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    ActiveScreen.PROJECT_DETAIL -> {
                        val completedCount = tasks.count { it.projectId == project?.id && it.isCompleted }
                        val totalCount = tasks.count { it.projectId == project?.id }
                        ProjectProgressArc(
                            completed = completedCount,
                            total = totalCount,
                            modifier = Modifier.size((26 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = project?.name ?: "Project",
                            style = TextStyle(
                                fontSize = headerTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        )
                    }
                    else -> {}
                }
            }
        }

        if (screen == ActiveScreen.TODAY && calendarEvents.isNotEmpty()) {
            item {
                CalendarEventsWidget(
                    events = calendarEvents,
                    textSecondaryColor = textSecondaryColor,
                    isDark = false
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        if (allTags.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        val isAllSelected = selectedTag == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isAllSelected) ThingsBlue else Color.Transparent)
                                .border(1.dp, if (isAllSelected) ThingsBlue else dividerColor, RoundedCornerShape(12.dp))
                                .clickable { onTagSelect(null) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("All", color = if (isAllSelected) Color.White else textSecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(allTags.toList()) { tag ->
                        val isSelected = selectedTag == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ThingsBlue else Color.Transparent)
                                .border(1.dp, if (isSelected) ThingsBlue else dividerColor, RoundedCornerShape(12.dp))
                                .clickable { onTagSelect(if (isSelected) null else tag) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(tag, color = if (isSelected) Color.White else textSecondaryColor, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        val displayTasks = localTasksList
        val hasTasks = if (screen == ActiveScreen.TODAY) standardToday.isNotEmpty() || eveningToday.isNotEmpty() || draggedTaskId != null else displayTasks.isNotEmpty()

        if (!hasTasks) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp)
                        .testTag("empty_state_view"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.AssignmentTurnedIn,
                            contentDescription = "Empty",
                            tint = textSecondaryColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "All clear here! Enjoy your day.",
                            color = textSecondaryColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            val flattened = buildList<Any> {
                if (screen == ActiveScreen.TODAY) {
                    addAll(standardToday)
                    if (eveningToday.isNotEmpty() || draggedTaskId != null) {
                        add("evening_header")
                        addAll(eveningToday)
                    }
                } else {
                    addAll(displayTasks)
                }
            }

            items(flattened, key = { if (it is Task) it.id else it.toString() }) { item ->
                if (item is Task) {
                    val task = item
                    val isDragTask = draggedTaskId == task.id
                    val isExpanded = inlineExpandedTaskId == task.id
                    val shouldDim = inlineExpandedTaskId != null && !isExpanded
                    
                    val dimAlpha by animateFloatAsState(
                        targetValue = if (shouldDim) 0.3f else 1f,
                        label = "dimAlpha_${task.id}"
                    )
                    val dragScale by animateFloatAsState(
                        targetValue = if (isDragTask) 1.04f else 1.0f,
                        label = "dragScale_${task.id}"
                    )
                    val dragElevation by animateDpAsState(
                        targetValue = if (isDragTask) 8.dp else (if (isExpanded) 8.dp else 0.dp),
                        label = "dragElev_${task.id}"
                    )
                    val zIndexValToUse = if (isDragTask) 100f else (if (isExpanded) 1f else 0f)
                    val translationYVal = if (isDragTask) dragAccumulatedOffset else 0f
                    
                    val containerBgColor = if (isExpanded || isDragTask) MaterialTheme.colorScheme.background else Color.Transparent
                    
                    Column(
                        modifier = (if (isDragTask) Modifier else Modifier.animateItem())
                            .zIndex(zIndexValToUse)
                            .graphicsLayer {
                                translationY = translationYVal
                                scaleX = dragScale
                                scaleY = dragScale
                                alpha = dimAlpha
                            }
                            .shadow(dragElevation, RoundedCornerShape(8.dp))
                            .background(containerBgColor, RoundedCornerShape(8.dp))
                            .animateContentSize(animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ))
                    ) {
                        if (isExpanded) {
                            ThingsTaskInlineEditor(
                                task = task,
                                projects = projects,
                                onSave = { title, notes, section, isTonight, dueDate, tags, projectId, checklist, priority ->
                                    val updatedTask = task.copy(
                                        title = title,
                                        notes = notes,
                                        section = section,
                                        isTonight = isTonight,
                                        dueDate = dueDate,
                                        tags = tags,
                                        projectId = projectId,
                                        checklist = checklist,
                                        priority = priority
                                    )
                                    viewModel.updateTask(updatedTask)
                                    onInlineExpandedTaskIdChange(null)
                                },
                                onDelete = {
                                    viewModel.deleteTask(task)
                                    onInlineExpandedTaskIdChange(null)
                                },
                                onDone = {
                                    onInlineExpandedTaskIdChange(null)
                                }
                            )
                        } else {
                            TaskItemRow(
                                modifier = Modifier,
                                task = task,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                dividerColor = dividerColor,
                                onToggle = { onTaskToggle(task) },
                                onClick = { 
                                    onInlineExpandedTaskIdChange(task.id)
                                },
                                projects = projects,
                                showTodayIndicator = screen == ActiveScreen.TODAY && !task.isTonight,
                                isDragging = false,
                                dragOffsetY = 0f,
                                dragModifier = makeDragModifier(task)
                            )
                        }
                    }
                } else if (item == "evening_header") {
                    val shouldDim = inlineExpandedTaskId != null
                    val dimAlpha by animateFloatAsState(
                        targetValue = if (shouldDim) 0.3f else 1f,
                        label = "dimAlpha_evening"
                    )

                    Column(modifier = Modifier
                        .animateItem()
                        .graphicsLayer { alpha = dimAlpha }
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌙",
                                fontSize = 18.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "This Evening",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimaryColor
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(dividerColor)
                                .padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}