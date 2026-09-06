package com.example.ui.screens.home.subcomponents

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.NightsStay
import com.example.ui.screens.home.ActiveScreen
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.data.model.Item
import com.example.ui.components.ThingsCheckbox
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TaskItemRow: Renders the complete, highly responsive checklist item with custom 
 * elevation feedback transitions, note flags, and tag chips..
 */

// [ИЗМЕНЕНИЕ]: Добавлен параметр isHighlighted для кратковременной подсветки задачи при клике из поиска
@Composable
fun TaskItemRow(
    modifier: Modifier = Modifier,
    task: Item,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    projects: List<Item>,
    showTodayIndicator: Boolean = false,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    dragModifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    isDimmed: Boolean = false,
    isBeingDeleted: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    leftColumnWidth: androidx.compose.ui.unit.Dp = androidx.compose.material3.MaterialTheme.dimens.mainCheckboxSize,
    spacingToText: androidx.compose.ui.unit.Dp = androidx.compose.material3.MaterialTheme.dimens.taskSpacingToTextDefault,
    screen: ActiveScreen = ActiveScreen.INBOX,
    areas: List<com.example.data.model.Area> = emptyList()
) {
    val isDark = false
    val titleFontSize = androidx.compose.material3.MaterialTheme.typography.titleMedium.fontSize
    val subFontSize = androidx.compose.material3.MaterialTheme.typography.bodySmall.fontSize

    val scope = rememberCoroutineScope()
    var localCompleted by remember(task.isCompleted) { mutableStateOf(task.isCompleted) }
    var completionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var localDeleted by remember { mutableStateOf(false) }
    LaunchedEffect(isBeingDeleted) {
        if (isBeingDeleted) {
            localDeleted = true
        }
    }

    val isMarkedDoneOrDeleted = localCompleted || localDeleted

    val completionFillProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isMarkedDoneOrDeleted) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 350,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "completionFill_${task.id}"
    )

    val animatedTitleColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isMarkedDoneOrDeleted) textSecondaryColor else textPrimaryColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        label = "titleColor_${task.id}"
    )

    var titleScaleTarget by remember { mutableStateOf(1.0f) }

    val animatedTitleScaleX by animateFloatAsState(
        targetValue = titleScaleTarget,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (titleScaleTarget < 1.0f) 70 else 120,
            easing = if (titleScaleTarget < 1.0f) androidx.compose.animation.core.FastOutSlowInEasing else androidx.compose.animation.core.FastOutLinearInEasing
        ),
        label = "titleScaleX_${task.id}"
    )

    var cardScaleTarget by remember { mutableStateOf(1.0f) }

    val cardScale by animateFloatAsState(
        targetValue = cardScaleTarget,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (cardScaleTarget < 1.0f) 50 else 100,
            easing = if (cardScaleTarget < 1.0f) androidx.compose.animation.core.FastOutSlowInEasing else androidx.compose.animation.core.FastOutLinearInEasing
        ),
        label = "cardScale_${task.id}"
    )

    val scale by androidx.compose.animation.core.animateFloatAsState(if (isDragging) 1.04f else 1.0f)
    val elevation by androidx.compose.animation.core.animateDpAsState(if (isDragging) 6.dp else 0.dp)

    val highlightColor = if (isHighlighted) {
        ThingsBlue.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    val animatedRowBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.surface
            isSelected -> ThingsBlue.copy(alpha = 0.22f)
            else -> highlightColor
        },
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "rowBgColor_${task.id}"
    )

    val fillBgColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF2C2D32) else Color(0xFFE5E6EB)

    val isCalendarTask = task.id.startsWith("cal_")

    val dateIndicator = remember(task.startDate, task.isTonight) {
        if (task.startDate != null) {
            getStartDateIndicator(task.startDate, task.isTonight)
        } else {
            null
        }
    }

    val rowInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = scale * cardScale
                scaleY = scale * cardScale
            }
            .shadow(elevation, RoundedCornerShape(8.dp))
            .background(animatedRowBgColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .drawBehind {
                if (localCompleted && completionFillProgress > 0f) {
                    val centerX = (10.dp + leftColumnWidth / 2f).toPx()
                    val centerY = size.height / 2f
                    val maxRadius = kotlin.math.hypot(size.width - centerX, centerY)
                    val currentRadius = maxRadius * completionFillProgress

                    drawCircle(
                        color = fillBgColor,
                        radius = currentRadius,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    )
                }
            }
            .then(dragModifier)
            .clickable(
                interactionSource = rowInteractionSource,
                indication = if (isDimmed || isSelectionMode) null else androidx.compose.foundation.LocalIndication.current,
                enabled = !isCalendarTask && !isSelectionMode
            ) { onClick() }
            .padding(start = androidx.compose.material3.MaterialTheme.dimens.taskRowStartPadding, end = androidx.compose.material3.MaterialTheme.dimens.taskRowEndPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
            Box(
                modifier = Modifier.size(leftColumnWidth),
                contentAlignment = Alignment.Center
            ) {
                if (isCalendarTask) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Calendar Event",
                        tint = ThingsBlue,
                        modifier = Modifier
                            .size(16.dp)
                    )
                } else {
                    ThingsCheckbox(
                        checked = localCompleted,
                        onCheckedChange = {
                            val newChecked = !localCompleted
                            localCompleted = newChecked
                            completionJob?.cancel()

                            // Импульсы сжатия текста и карточки запускаются СТРОГО при физическом клике по чекбоксу
                            scope.launch {
                                titleScaleTarget = 0.93f
                                delay(70)
                                titleScaleTarget = 1.0f
                            }
                            scope.launch {
                                cardScaleTarget = 0.97f
                                delay(50)
                                cardScaleTarget = 1.0f
                            }

                            if (task.isCompleted) {
                                scope.launch {
                                    delay(220)
                                    onToggle()
                                }
                            } else {
                                if (newChecked) {
                                    completionJob = scope.launch {
                                        delay(500)
                                        onToggle()
                                    }
                                }
                            }
                        },
                        size = 16.dp,
                        modifier = Modifier
                            .testTag("task_checkbox")
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacingToText))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (screen == ActiveScreen.PROJECT_DETAIL && dateIndicator != null) {
                            when (dateIndicator) {
                                is DateIndicatorResult.IconIndicator -> {
                                    Icon(
                                        imageVector = dateIndicator.icon,
                                        contentDescription = dateIndicator.contentDescription,
                                        tint = dateIndicator.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                is DateIndicatorResult.TextIndicator -> {
                                    val displayText = if (dateIndicator.text == "TOMORROW_PLACEHOLDER") {
                                        androidx.compose.ui.res.stringResource(com.example.R.string.tomorrow)
                                    } else {
                                        dateIndicator.text
                                    }
                                    
                                    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                                    val badgeTextColor = if (isSystemDark) Color(0xFFE0E0E0) else Color(0xFF5F6368)
                                    val badgeBgColor = if (isSystemDark) Color(0xFF2C2C2E) else Color(0xFFECECEC)

                                    Text(
                                        text = displayText,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = badgeTextColor
                                        ),
                                        modifier = Modifier
                                            .background(
                                                color = badgeBgColor,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = task.title,
                            style = TextStyle(
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Normal,
                                color = animatedTitleColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .graphicsLayer {
                                    scaleX = animatedTitleScaleX
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                }
                        )

                    // Inline note/subtask icons representation
                    if (task.notes.isNotBlank() || task.checklistItemsCount > 0 || task.cachedTags.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (task.notes.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = "Has notes",
                                    tint = textSecondaryColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            if (task.checklistItemsCount > 0) {
                                Icon(
                                    imageVector = Icons.Outlined.FormatListBulleted,
                                    contentDescription = "Has checklist",
                                    tint = textSecondaryColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            if (task.cachedTags.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalOffer,
                                    contentDescription = "Has tags",
                                    tint = textSecondaryColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                if (task.dueDate != null) {
                    val delta = remember(task.dueDate) {
                        val today = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        val due = java.util.Calendar.getInstance().apply {
                            timeInMillis = task.dueDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        val diffMillis = due.timeInMillis - today.timeInMillis
                        if (diffMillis >= 0) {
                            (diffMillis / (24 * 60 * 60 * 1000L)).toInt()
                        } else {
                            ((diffMillis - (24 * 60 * 60 * 1000L - 1)) / (24 * 60 * 60 * 1000L)).toInt()
                        }
                    }

                    val lang = remember { java.util.Locale.getDefault().language }
                    val relativeText = when (lang) {
                        "uk" -> when {
                            delta < 0 -> "протерміновано"
                            delta == 0 -> "сьогодні"
                            delta == 1 -> "завтра"
                            else -> "через $delta дн."
                        }
                        "ru" -> when {
                            delta < 0 -> "просрочено"
                            delta == 0 -> "сегодня"
                            delta == 1 -> "завтра"
                            else -> "через $delta дн."
                        }
                        else -> when {
                            delta < 0 -> "overdue"
                            delta == 0 -> "today"
                            delta == 1 -> "tomorrow"
                            else -> "in $delta d."
                        }
                    }

                    val isOverdueOrToday = delta <= 0
                    val color = if (isOverdueOrToday) ThingsUpcomingRed else textSecondaryColor.copy(alpha = 0.85f)

                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "Deadline Flag",
                            tint = color,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = relativeText,
                            style = TextStyle(
                                fontSize = subFontSize,
                                color = color,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Project link displayed underneath title, matching screenshot
            val project = remember(task.projectId, projects) {
                projects.firstOrNull { it.id == task.projectId }
            }
            val area = remember(task.areaId, areas) {
                areas.firstOrNull { it.id == task.areaId }
            }
            if (project != null && screen != ActiveScreen.PROJECT_DETAIL) {
                // Расстояние уменьшено до 0.dp по запросу пользователя.
                Spacer(modifier = Modifier.height(0.dp))
                Text(
                    text = project.name,
                    style = TextStyle(
                        fontSize = subFontSize,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.Normal
                    )
                )
            } else if (project == null && area != null && screen != ActiveScreen.AREA_DETAIL) {
                // Расстояние уменьшено до 0.dp по запросу пользователя.
                Spacer(modifier = Modifier.height(0.dp))
                Text(
                    text = area.title,
                    style = TextStyle(
                        fontSize = subFontSize,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isSelectionMode,
            enter = androidx.compose.animation.scaleIn(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) + androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
            exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
        ) {
            val animatedBorderColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) ThingsBlue else Color(0xFFC7C7CC),
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                label = "selectionBorderColor"
            )
            val animatedCheckScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                ),
                label = "selectionCheckScale"
            )

            val iconBounceScale = remember { androidx.compose.animation.core.Animatable(1f) }
            var isInitialComposition by remember { mutableStateOf(true) }

            LaunchedEffect(isSelected) {
                if (isInitialComposition) {
                    isInitialComposition = false
                } else {
                    iconBounceScale.animateTo(
                        targetValue = 1.18f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 80,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    )
                    iconBounceScale.animateTo(
                        targetValue = 1.0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                val view = androidx.compose.ui.platform.LocalView.current
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = iconBounceScale.value
                            scaleY = iconBounceScale.value
                        }
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            onToggleSelect()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.size(24.dp)
                    ) {
                        val strokeWidth = 2.dp.toPx()
                        val radius = size.minDimension / 2f
                        
                        // 1. Внешняя окружность с плавным переходом цвета
                        drawCircle(
                            color = animatedBorderColor,
                            radius = radius - strokeWidth / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )

                        // 2. Анимированное заполнение при выборе задачи (spring bounce)
                        if (animatedCheckScale > 0.001f) {
                            // Центральная синяя заливка с пружинным масштабированием
                            val targetInnerRadius = (radius - strokeWidth) - 2.dp.toPx()
                            val currentInnerRadius = targetInnerRadius * animatedCheckScale
                            if (currentInnerRadius > 0f) {
                                drawCircle(
                                    color = ThingsBlue,
                                    radius = currentInnerRadius
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class DateIndicatorResult {
    data class IconIndicator(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val color: androidx.compose.ui.graphics.Color,
        val contentDescription: String
    ) : DateIndicatorResult()
    
    data class TextIndicator(
        val text: String
    ) : DateIndicatorResult()
}

private fun getStartDateIndicator(startDate: Long, isTonight: Boolean): DateIndicatorResult {
    val taskCal = java.util.Calendar.getInstance().apply { timeInMillis = startDate }
    val todayCal = java.util.Calendar.getInstance()
    val currentYear = todayCal.get(java.util.Calendar.YEAR)
    
    val endOfToday = (todayCal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.HOUR_OF_DAY, 23)
        set(java.util.Calendar.MINUTE, 59)
        set(java.util.Calendar.SECOND, 59)
        set(java.util.Calendar.MILLISECOND, 999)
    }.timeInMillis

    val isTodayOrPast = startDate <= endOfToday
                  
    if (isTodayOrPast) {
        return if (isTonight) {
            // Иконка "Вечер" с использованием AppIcons.Evening
            DateIndicatorResult.IconIndicator(
                icon = AppIcons.Evening,
                color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                contentDescription = "Tonight"
            )
        } else {
            DateIndicatorResult.IconIndicator(
                icon = AppIcons.Today,
                color = androidx.compose.ui.graphics.Color.Unspecified,
                contentDescription = "Today"
            )
        }
    }
    
    val tomorrowCal = java.util.Calendar.getInstance().apply {
        timeInMillis = todayCal.timeInMillis
        add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    val isTomorrow = taskCal.get(java.util.Calendar.YEAR) == tomorrowCal.get(java.util.Calendar.YEAR) &&
                     taskCal.get(java.util.Calendar.DAY_OF_YEAR) == tomorrowCal.get(java.util.Calendar.DAY_OF_YEAR)
                     
    if (isTomorrow) {
        return DateIndicatorResult.TextIndicator("TOMORROW_PLACEHOLDER")
    }
    
    // Check if the task is in the same calendar week as today
    val inSameCalendarWeek = taskCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
                             taskCal.get(java.util.Calendar.WEEK_OF_YEAR) == todayCal.get(java.util.Calendar.WEEK_OF_YEAR)
                             
    if (inSameCalendarWeek) {
        val locale = java.util.Locale.getDefault()
        val dayOfWeekStr = java.text.SimpleDateFormat("EEE", locale).format(taskCal.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        return DateIndicatorResult.TextIndicator(dayOfWeekStr)
    }
    
    if (taskCal.get(java.util.Calendar.YEAR) > currentYear) {
        return DateIndicatorResult.TextIndicator(taskCal.get(java.util.Calendar.YEAR).toString())
    }
    
    val locale = java.util.Locale.getDefault()
    val monthAndDayStr = java.text.SimpleDateFormat("d MMM", locale).format(taskCal.time)
    return DateIndicatorResult.TextIndicator(monthAndDayStr)
}

