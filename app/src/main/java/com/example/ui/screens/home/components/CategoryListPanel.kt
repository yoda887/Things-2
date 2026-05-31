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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.layout
import com.example.data.model.Item
import com.example.data.model.TaskSection
import com.example.data.model.Area
import com.example.ui.components.ProjectProgressArc
import com.example.ui.screens.home.ActiveScreen
import com.example.ui.screens.home.subcomponents.TaskItemRow
import com.example.ui.screens.home.inlineeditor.ThingsTaskInlineEditor
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThingsViewModel
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

// [ИЗМЕНЕНИЕ]: Вспомогательный класс для представления задач и календарных событий, сгруппированных по дням
data class UpcomingDay(
    val dateMillis: Long,
    val dayOfMonth: String,
    val dayOfWeekLabel: String,
    val calendarEvents: List<Item>,
    val tasks: List<Item>
)

data class UpcomingHeaderItem(
    val dateMillis: Long,
    val dayOfMonth: String,
    val dayOfWeekLabel: String
)

data class UpcomingEventItem(
    val event: Item,
    val dateMillis: Long
)

// [ИЗМЕНЕНИЕ]: Компонент для отображения одного календарного события на экране Upcoming
@Composable
fun UpcomingCalendarEventRow(
    event: Item,
    textSecondaryColor: Color,
    textPrimaryColor: Color
) {
    val eventStart = event.eventStartMillis ?: 0L
    val hasTime = !event.isAllDay && eventStart > 0
    
    val rawColor = event.calendarColor
    val baseColor = remember(rawColor, event.calendarDisplayName, event.id) {
        if (rawColor != null) {
            Color(rawColor)
        } else {
            Color(0xFF63C655) // Приятный зеленый цвет, соответствующий iOS стилю
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasTime) {
            val timeString = remember(eventStart) { 
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(eventStart)) 
            }
            Text(
                text = timeString,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = baseColor
                ),
                modifier = Modifier.width(72.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = event.title,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = textPrimaryColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            // All-day событие отображается зеленой вертикальной линией и текстом без времени
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(baseColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = event.title,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = textPrimaryColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ThingsCategoryListPanel(
    screen: ActiveScreen,
    project: Item?,
    tasks: List<Item>,
    allTags: Set<String>,
    selectedTag: String?,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onTagSelect: (String?) -> Unit,
    onTaskToggle: (Item) -> Unit,
    onTaskClick: (Item) -> Unit,
    projects: List<Item>,
    viewModel: ThingsViewModel,
    inlineExpandedTaskId: String?,
    onInlineExpandedTaskIdChange: (String?) -> Unit,
    area: Area? = null,
    onProjectClick: (Item) -> Unit = {}
) {
    val listTasks = remember(tasks, screen, project, area) {
        tasks.filter { task ->
            when (screen) {
                ActiveScreen.INBOX -> task.isInbox && !task.isCompleted
                ActiveScreen.TODAY -> task.isToday
                ActiveScreen.UPCOMING -> task.isUpcoming
                ActiveScreen.ANYTIME -> task.isAnytime
                ActiveScreen.SOMEDAY -> task.isSomeday
                ActiveScreen.LOGBOOK -> task.isCompleted
                ActiveScreen.PROJECT_DETAIL -> task.projectId == project?.id && !task.isCompleted
                // [ИЗМЕНЕНИЕ]: Фильтруем строго по типу задачи (task.type == 0), чтобы проекты не дублировались и не вызывали ошибку duplicate key
                ActiveScreen.AREA_DETAIL -> task.areaId == area?.id && task.type == 0 && !task.isCompleted
                else -> false
            }
        }.sortedBy { it.sortOrder }
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

    // [ИЗМЕНЕНИЕ]: Фильтруем календарные события на "Сегодня"
    val todayCalendarEvents = remember(calendarEvents) {
        calendarEvents.filter { event ->
            val start = event.eventStartMillis
            start != null && com.example.ui.screens.home.inlineeditor.utils.isTodayDate(start)
        }
    }

    // [ИЗМЕНЕНИЕ]: Вычисляем сгруппированные данные для экрана Upcoming
    val upcomingDays = remember(localTasksList, calendarEvents) {
        val daysList = mutableListOf<UpcomingDay>()
        
        // Начало завтрашнего дня в миллисекундах
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val tomorrowStart = cal.timeInMillis
        
        for (offset in 0 until 14) {
            val c = Calendar.getInstance()
            c.timeInMillis = tomorrowStart
            c.add(Calendar.DAY_OF_YEAR, offset)
            val dayStart = c.timeInMillis
            
            val dayEnd = Calendar.getInstance().apply {
                timeInMillis = dayStart
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            // Находим все календарные события на этот день
            val dayEvents = calendarEvents.filter { event ->
                event.eventStartMillis != null && event.eventStartMillis in dayStart..dayEnd
            }
            
            // Находим все задачи на этот день
            val dayTasks = localTasksList.filter { task ->
                task.startDate != null && task.startDate in dayStart..dayEnd
            }
            
            if (dayEvents.isNotEmpty() || dayTasks.isNotEmpty()) {
                val dayOfMonthLabel = Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_MONTH).toString()
                val dayOfWeekLabel = if (offset == 0) {
                    "Tomorrow"
                } else if (offset < 6) {
                    SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date(dayStart))
                } else {
                    SimpleDateFormat("MMMM", Locale.ENGLISH).format(Date(dayStart))
                }
                
                daysList.add(
                    UpcomingDay(
                        dateMillis = dayStart,
                        dayOfMonth = dayOfMonthLabel,
                        dayOfWeekLabel = dayOfWeekLabel,
                        calendarEvents = dayEvents,
                        tasks = dayTasks
                    )
                )
            }
        }
        daysList
    }

    LaunchedEffect(filteredTasks) {
        localTasksList = filteredTasks
    }

    val makeDragModifier = { task: Item ->
        Modifier.pointerInput(task.id, task.sortOrder) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    draggedTaskId = task.id
                    dragAccumulatedOffset = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragAccumulatedOffset += dragAmount.y

                    val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                    val detectedSpacing = run {
                        var spacing = 0f
                        try {
                            spacing = lazyListState.layoutInfo.mainAxisItemSpacing.toFloat()
                        } catch (e: Exception) {
                            // ignore
                        }
                        if (spacing <= 0f && visibleItems.size >= 2) {
                            for (i in 0 until visibleItems.lastIndex) {
                                val current = visibleItems[i]
                                val next = visibleItems[i + 1]
                                if (next.index == current.index + 1) {
                                    val gap = next.offset - (current.offset + current.size)
                                    if (gap >= 0) {
                                        spacing = gap.toFloat()
                                        break
                                    }
                                }
                            }
                        }
                        spacing
                    }
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
                            val cleanKey = (hoveredItem.key as? String) ?: ""
                            val isHdr = cleanKey.startsWith("hdr_")
                            val isEv = cleanKey.startsWith("ev_")

                            if (hoveredItem.key == "evening_header") {
                                if (fromIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    if (!movedItem.isTonight) {
                                        movedItem = movedItem.copyTask(isTonight = true)
                                        val firstEveningIndex = newList.indexOfFirst { it.isTonight }
                                        val toIndex = if (firstEveningIndex != -1) firstEveningIndex else newList.size
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        val distance = (hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset - draggedItemInfo.size
                                        dragAccumulatedOffset -= distance
                                    } else {
                                        movedItem = movedItem.copyTask(isTonight = false)
                                        val firstEveningIndex = newList.indexOfFirst { it.isTonight }
                                        val toIndex = if (firstEveningIndex != -1) firstEveningIndex else newList.size
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        val distance = if (hoveredItem.offset > draggedItemInfo.offset) {
                                            hoveredItem.size.toFloat()
                                        } else {
                                            -hoveredItem.size.toFloat()
                                        }
                                        dragAccumulatedOffset -= distance
                                    }
                                }
                            } else if (hoveredItem.key == "main_header") {
                                if (fromIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    if (movedItem.isTonight) {
                                        movedItem = movedItem.copyTask(isTonight = false)
                                        val toIndex = 0
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        val distance = (hoveredItem.offset + hoveredItem.size) - draggedItemInfo.offset
                                        dragAccumulatedOffset -= distance
                                    }
                                }
                            } else if (screen == ActiveScreen.UPCOMING && (isHdr || isEv)) {
                                val timestampStr = if (isHdr) cleanKey.substringAfter("hdr_") else cleanKey.substringAfterLast("_")
                                val timestamp = timestampStr.toLongOrNull()
                                if (timestamp != null && fromIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    
                                    val tomorrowStart = upcomingDays.firstOrNull()?.dateMillis ?: 0L
                                    
                                    val isCurrentlyInHoveredDay = if (movedItem.startDate == null) {
                                        timestamp == tomorrowStart
                                    } else {
                                        movedItem.startDate!! in timestamp..(timestamp + 24 * 3600 * 1000 - 1)
                                    }
                                    
                                    var targetTimestamp = if (isCurrentlyInHoveredDay && draggedItemInfo.offset > hoveredItem.offset) {
                                        timestamp - 24 * 3600 * 1000L
                                    } else {
                                        timestamp
                                    }
                                    
                                    if (targetTimestamp < tomorrowStart) {
                                        targetTimestamp = tomorrowStart
                                    }
                                    
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = targetTimestamp
                                        set(Calendar.HOUR_OF_DAY, 12)
                                        set(Calendar.MINUTE, 0)
                                    }
                                    val newStartDate = cal.timeInMillis
                                    
                                    val oldStartDateDayStart = if (movedItem.startDate == null) {
                                        tomorrowStart
                                    } else {
                                        Calendar.getInstance().apply {
                                            timeInMillis = movedItem.startDate!!
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    }
                                    
                                    val targetDayStart = Calendar.getInstance().apply {
                                        timeInMillis = targetTimestamp
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    
                                    if (oldStartDateDayStart != targetDayStart) {
                                        movedItem = movedItem.copyTask(startDate = newStartDate)
                                        
                                        var toIndex = newList.indexOfFirst { it.startDate != null && it.startDate >= targetDayStart }
                                        if (toIndex == -1) {
                                            toIndex = newList.size
                                        }
                                        newList.add(toIndex, movedItem)
                                        localTasksList = newList
                                        
                                        val distance = if (hoveredItem.offset > draggedItemInfo.offset) {
                                            hoveredItem.size.toFloat()
                                        } else {
                                            -hoveredItem.size.toFloat()
                                        }
                                        dragAccumulatedOffset -= distance
                                    }
                                }
                            } else {
                                val toIndex = localTasksList.indexOfFirst { it.id == hoveredItem.key }
                                if (fromIndex != -1 && toIndex != -1) {
                                    val newList = localTasksList.toMutableList()
                                    var movedItem = newList.removeAt(fromIndex)
                                    
                                    val hoveredItemTask = localTasksList.firstOrNull { it.id == hoveredItem.key }
                                    if (hoveredItemTask != null) {
                                        if (hoveredItemTask.isTonight != movedItem.isTonight) {
                                            movedItem = movedItem.copyTask(isTonight = hoveredItemTask.isTonight)
                                        }
                                        if (screen == ActiveScreen.UPCOMING && movedItem.startDate != hoveredItemTask.startDate) {
                                            movedItem = movedItem.copyTask(startDate = hoveredItemTask.startDate)
                                        }
                                    }
                                    
                                    newList.add(toIndex, movedItem)
                                    localTasksList = newList
                                    
                                    val distance = if (hoveredItem.offset > draggedItemInfo.offset) {
                                        hoveredItem.size.toFloat() + detectedSpacing
                                    } else {
                                        -(hoveredItem.size.toFloat() + detectedSpacing)
                                    }
                                    dragAccumulatedOffset -= distance
                                }
                            }
                        }
                    }
                },
                onDragEnd = {
                    val updatedList = localTasksList.mapIndexed { index, t ->
                        val original = filteredTasks.firstOrNull { it.id == t.id }
                        if (original != null && (original.sortOrder != index || original.isTonight != t.isTonight || original.startDate != t.startDate)) {
                            t.copyTask(sortOrder = index, modificationDate = System.currentTimeMillis())
                        } else {
                            t.copyTask(sortOrder = index)
                        }
                    }
                    val changedTasks = updatedList.filter { t ->
                        val original = filteredTasks.firstOrNull { it.id == t.id }
                        original == null || original.sortOrder != t.sortOrder || original.isTonight != t.isTonight || original.startDate != t.startDate
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
            .testTag("tasks_lazy_list")
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
                    ActiveScreen.AREA_DETAIL -> {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = Color(0xFF1B80FA),
                            modifier = Modifier.size((26 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = area?.title ?: "Responsibility Area",
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

        // [ИЗМЕНЕНИЕ]: Фильтруем календарные события на экране Today, чтобы показывались только сегодняшние события.
        if (screen == ActiveScreen.TODAY && todayCalendarEvents.isNotEmpty()) {
            item {
                CalendarEventsWidget(
                    events = todayCalendarEvents,
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
        // [ИЗМЕНЕНИЕ]: Меняем логику проверки наличия элементов, включая экран Upcoming и Area Detail
        val hasTasks = when (screen) {
            ActiveScreen.TODAY -> standardToday.isNotEmpty() || eveningToday.isNotEmpty() || draggedTaskId != null
            ActiveScreen.UPCOMING -> upcomingDays.isNotEmpty()
            ActiveScreen.AREA_DETAIL -> {
                val areaProjCount = projects.count { it.areaId == area?.id }
                val areaTasksCount = displayTasks.count { it.areaId == area?.id && (it.projectId == null || it.projectId == "") }
                areaProjCount > 0 || areaTasksCount > 0
            }
            else -> displayTasks.isNotEmpty()
        }

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
                } else if (screen == ActiveScreen.UPCOMING) {
                    upcomingDays.forEach { day ->
                        add(UpcomingHeaderItem(day.dateMillis, day.dayOfMonth, day.dayOfWeekLabel))
                        day.calendarEvents.forEach { event ->
                            add(UpcomingEventItem(event, day.dateMillis))
                        }
                        day.tasks.forEach { task ->
                            add(task)
                        }
                    }
                } else if (screen == ActiveScreen.AREA_DETAIL) {
                    val areaProjects = projects.filter { it.areaId == area?.id }
                    if (areaProjects.isNotEmpty()) {
                        add("projects_heading")
                        addAll(areaProjects)
                    }
                    val areaDirectTasks = displayTasks.filter { it.areaId == area?.id && (it.projectId == null || it.projectId == "") }
                    if (areaDirectTasks.isNotEmpty()) {
                        add("tasks_heading")
                        addAll(areaDirectTasks)
                    }
                } else {
                    addAll(displayTasks)
                }
            }

            items(flattened, key = { item ->
                when (item) {
                    is Item -> item.id
                    is UpcomingHeaderItem -> "hdr_${item.dateMillis}"
                    is UpcomingEventItem -> "ev_${item.event.id}_${item.dateMillis}"
                    else -> item.toString()
                }
            }) { item ->
                when (item) {
                    is Item -> {
                        val task = item
                        if (task.type == 1) {
                            // [ИЗМЕНЕНИЕ]: Отрисовка строки проекта в списке проектов области
                            // Использует скругление углов 10.dp для достижения полной идентичности с главным экраном!
                            val projectTasks = tasks.filter { it.projectId == task.id }
                            val completedCount = projectTasks.count { it.isCompleted }
                            val totalCount = projectTasks.size
                            val shouldDim = inlineExpandedTaskId != null
                            val dimAlpha by animateFloatAsState(
                                targetValue = if (shouldDim) 0.3f else 1f,
                                label = "dimAlpha_proj_${task.id}"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onProjectClick(task) }
                                    .graphicsLayer { alpha = dimAlpha }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProjectProgressArc(
                                    completed = completedCount,
                                    total = totalCount,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        color = textPrimaryColor,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
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
                                    .layout { measurable, constraints ->
                                        val paddingPx = 4.dp.roundToPx()
                                        val extendedConstraints = constraints.copy(
                                            minWidth = (constraints.minWidth + paddingPx * 2).coerceAtMost(constraints.maxWidth + paddingPx * 2),
                                            maxWidth = (constraints.maxWidth + paddingPx * 2)
                                        )
                                        val placeable = measurable.measure(extendedConstraints)
                                        layout(placeable.width - paddingPx * 2, placeable.height) {
                                            placeable.place(-paddingPx, 0)
                                        }
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
                                        onSave = { title, notes, section, isTonight, startDate, dueDate, tags, projectId, checklist, priority ->
                                            val startVal = when (section) {
                                                TaskSection.INBOX -> 0
                                                TaskSection.TODAY -> 1
                                                TaskSection.ANYTIME -> 2
                                                TaskSection.SOMEDAY -> 3
                                                TaskSection.UPCOMING -> 2
                                            }
                                            val updatedTask = task.copyTask(
                                                title = title,
                                                notes = notes,
                                                start = startVal,
                                                isTonight = isTonight,
                                                startDate = startDate,
                                                dueDate = dueDate,
                                                cachedTags = tags.joinToString(", "),
                                                projectId = projectId,
                                                priority = priority,
                                                checklist = checklist
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
                        }
                    }
                    is UpcomingHeaderItem -> {
                        val header = item
                        val shouldDim = inlineExpandedTaskId != null
                        val dimAlpha by animateFloatAsState(
                            targetValue = if (shouldDim) 0.3f else 1f,
                            label = "dimAlpha_hdr_${header.dateMillis}"
                        )
                        Row(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .graphicsLayer { alpha = dimAlpha }
                                .padding(top = 22.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = header.dayOfMonth,
                                style = TextStyle(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimaryColor
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = header.dayOfWeekLabel,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondaryColor.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(0.6.dp)
                                    .background(dividerColor)
                                    .padding(bottom = 4.dp)
                            )
                        }
                    }
                    is UpcomingEventItem -> {
                        val event = item.event
                        val shouldDim = inlineExpandedTaskId != null
                        val dimAlpha by animateFloatAsState(
                            targetValue = if (shouldDim) 0.3f else 1f,
                            label = "dimAlpha_ev_${event.id}"
                        )
                        Column(
                            modifier = Modifier
                                .animateItem()
                                .graphicsLayer { alpha = dimAlpha }
                        ) {
                            UpcomingCalendarEventRow(
                                event = event,
                                textSecondaryColor = textSecondaryColor,
                                textPrimaryColor = textPrimaryColor
                            )
                        }
                    }
                    else -> { // like "evening_header", "projects_heading", "tasks_heading"
                        val headerText = item as String
                        val shouldDim = inlineExpandedTaskId != null
                        val dimAlpha by animateFloatAsState(
                            targetValue = if (shouldDim) 0.3f else 1f,
                            label = "dimAlpha_$headerText"
                        )

                        if (headerText == "projects_heading") {
                            Column(
                                modifier = Modifier
                                    .animateItem()
                                    .graphicsLayer { alpha = dimAlpha }
                                    .padding(top = 22.dp, bottom = 10.dp)
                            ) {
                                Text(
                                    text = "PROJECTS",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondaryColor.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(dividerColor)
                                )
                            }
                        } else if (headerText == "tasks_heading") {
                            Column(
                                modifier = Modifier
                                    .animateItem()
                                    .graphicsLayer { alpha = dimAlpha }
                                    .padding(top = 22.dp, bottom = 10.dp)
                            ) {
                                Text(
                                    text = "TASKS",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondaryColor.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(dividerColor)
                                )
                            }
                        } else {
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
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}