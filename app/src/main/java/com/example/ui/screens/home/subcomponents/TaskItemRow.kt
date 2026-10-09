package com.example.ui.screens.home.subcomponents

import com.example.ui.viewmodel.ActiveScreen
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.theme.ThingsTheme
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Flag
import com.example.ui.theme.AppIcons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
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

// Колонка чекбокса от края строки; кружок выбора и зазор кольца отметки
private val ROW_CHECKBOX_COLUMN_START = 10.dp
private val SELECTION_CIRCLE_SIZE = 28.dp
private val SELECTION_RING_GAP = 2.dp

private const val COMPLETION_FILL_MS = 350
// Подсветка найденной задачи — по кадрам эталона Things 3
private const val FOUND_GLOW_IN_MS = 160
private const val FOUND_GLOW_HOLD_MS = 80L
private const val FOUND_GLOW_OUT_MS = 720
private const val FOUND_SCALE_DELAY_MS = 40L
private const val FOUND_SCALE_UP_MS = 60
private const val FOUND_SCALE_DOWN_MS = 650
// Импульс нажатия чекбокса
private const val TITLE_PRESS_MS = 70L
private const val CARD_PRESS_MS = 50L
private const val COMPLETE_TOGGLE_DELAY_MS = 220L
private const val SELECT_BOUNCE_MS = 80
private val ROW_META_ICON_SIZE = 13.dp
private val DUE_ICON_SIZE = 15.dp
private const val DUE_TEXT_ALPHA = 0.85f

/** Пауза перед подсветкой найденной задачи: экран успевает открыться и докрутить до неё */
const val FOUND_HIGHLIGHT_DELAY_MS = 450L

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
    // Задача, к которой перешли из Quick Find (см. FOUND_HIGHLIGHT_DELAY_MS)
    isFound: Boolean = false,
    isDimmed: Boolean = false,
    isBeingDeleted: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isDragSelecting: Boolean = false,
    onToggleSelect: () -> Unit = {},
    leftColumnWidth: androidx.compose.ui.unit.Dp = androidx.compose.material3.MaterialTheme.dimens.mainCheckboxSize,
    spacingToText: androidx.compose.ui.unit.Dp = androidx.compose.material3.MaterialTheme.dimens.taskSpacingToTextDefault,
    screen: ActiveScreen = ActiveScreen.INBOX,
    areas: List<com.example.data.model.Area> = emptyList(),
    dateBadge: String? = null
) {
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
    val isCancelledTask = task.status == Item.STATUS_CANCELLED
    // Выполненные и отменённые задачи в результатах поиска — как в Logbook эталона: без серой
    // подложки, с заполненным тёмно-синим флажком и датой завершения перед названием
    val isSearchLogbookStyle = screen == ActiveScreen.SEARCH && (task.isCompleted || isCancelledTask)

    val completionFillProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isMarkedDoneOrDeleted) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = COMPLETION_FILL_MS,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "completionFill_${task.id}"
    )

    val animatedTitleColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isMarkedDoneOrDeleted || (isSearchLogbookStyle && isCancelledTask)) textSecondaryColor else textPrimaryColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = ThingsMotion.STANDARD),
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
    val elevation by androidx.compose.animation.core.animateDpAsState(if (isDragging) ThingsElevation.FAB else ThingsElevation.NONE)

    // Найденная в Quick Find задача, по кадрам эталона Things 3: после паузы строка за 160 мс
    // заливается светло-жёлтым и почти сразу (60 мс) вырастает до 1.05 вместе со всем содержимым,
    // затем за ~650 мс плавно возвращается к своему размеру, а жёлтый за ~720 мс растворяется
    val foundGlow = remember { androidx.compose.animation.core.Animatable(0f) }
    val foundScale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(isFound) {
        if (isFound) {
            delay(FOUND_HIGHLIGHT_DELAY_MS)
            launch {
                foundGlow.animateTo(1f, androidx.compose.animation.core.tween(FOUND_GLOW_IN_MS, easing = androidx.compose.animation.core.LinearEasing))
                delay(FOUND_GLOW_HOLD_MS)
                foundGlow.animateTo(0f, androidx.compose.animation.core.tween(FOUND_GLOW_OUT_MS, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
            }
            delay(FOUND_SCALE_DELAY_MS)
            foundScale.animateTo(1.05f, androidx.compose.animation.core.tween(FOUND_SCALE_UP_MS, easing = androidx.compose.animation.core.LinearEasing))
            foundScale.animateTo(1f, androidx.compose.animation.core.tween(FOUND_SCALE_DOWN_MS, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
        } else {
            foundGlow.snapTo(0f)
            foundScale.snapTo(1f)
        }
    }

    val foundGlowColor = ThingsTheme.colors.searchMatchHighlight
    val rowShapeForGlow = ThingsTheme.shapes.rowShape
    val highlightColor = when {
        // Выделенная строка результатов Quick Find — 30 % акцента
        isHighlighted -> ThingsTheme.colors.accent.copy(alpha = ThingsAlpha.LOW)
        else -> Color.Transparent
    }

    val animatedRowBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isDragging -> ThingsTheme.colors.surface
            isSelected -> ThingsTheme.colors.accentSelection
            else -> highlightColor
        },
        animationSpec = androidx.compose.animation.core.tween(durationMillis = ThingsMotion.BASE),
        label = "rowBgColor_${task.id}"
    )

    val fillBgColor = ThingsTheme.colors.dropPlaceholder

    val isCalendarTask = task.id.startsWith("cal_")

    val eveningColor = ThingsTheme.colors.eveningIndicator
    val tonightDescription = stringResource(R.string.cd_tonight)
    val todayDescription = stringResource(R.string.category_today)
    val dateIndicator = remember(task.startDate, task.isTonight, task.start, task.dueDate, eveningColor, tonightDescription, todayDescription) {
        if (task.startDate != null) {
            getStartDateIndicator(task.startDate, task.isTonight, eveningColor, tonightDescription, todayDescription)
        } else if (task.isToday) {
            if (task.isTonight) {
                DateIndicatorResult.IconIndicator(
                    icon = AppIcons.Evening,
                    color = eveningColor,
                    contentDescription = tonightDescription
                )
            } else {
                DateIndicatorResult.IconIndicator(
                    icon = AppIcons.Today,
                    color = androidx.compose.ui.graphics.Color.Unspecified,
                    contentDescription = todayDescription
                )
            }
        } else {
            null
        }
    }

    val rowInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.rowHeight)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = scale * cardScale * foundScale.value
                scaleY = scale * cardScale * foundScale.value
            }
            .shadow(elevation, ThingsTheme.shapes.rowShape)
            .background(animatedRowBgColor, ThingsTheme.shapes.rowShape)
            // Жёлтый найденной задачи — поверх, по кадрам своей анимации (без сглаживания фона)
            .drawBehind {
                if (foundGlow.value > 0f) drawOutline(rowShapeForGlow.createOutline(size, layoutDirection, this), foundGlowColor.copy(alpha = foundGlow.value))
            }
            .clip(ThingsTheme.shapes.rowShape)
            .drawBehind {
                if (localCompleted && completionFillProgress > 0f && !isSearchLogbookStyle) {
                    val centerX = (ROW_CHECKBOX_COLUMN_START + leftColumnWidth / 2f).toPx()
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
                        contentDescription = stringResource(R.string.cd_calendar_event),
                        tint = ThingsTheme.colors.accent,
                        modifier = Modifier
                            .size(ThingsIconSize.XS)
                    )
                } else {
                    val checkboxUncheckedColor = if (isHighlighted) {
                        androidx.compose.ui.graphics.lerp(ThingsTheme.colors.checkboxBorder, ThingsTheme.colors.accent, 0.45f)
                    } else {
                        ThingsTheme.colors.checkboxBorder
                    }
                    ThingsCheckbox(
                        checked = localCompleted || (isSearchLogbookStyle && isCancelledTask),
                        onCheckedChange = {
                            val newChecked = !localCompleted
                            localCompleted = newChecked
                            completionJob?.cancel()

                            // Импульсы сжатия текста и карточки запускаются СТРОГО при физическом клике по чекбоксу
                            scope.launch {
                                titleScaleTarget = 0.93f
                                delay(TITLE_PRESS_MS)
                                titleScaleTarget = 1.0f
                            }
                            scope.launch {
                                cardScaleTarget = 0.97f
                                delay(CARD_PRESS_MS)
                                cardScaleTarget = 1.0f
                            }

                            if (task.isCompleted) {
                                scope.launch {
                                    delay(COMPLETE_TOGGLE_DELAY_MS)
                                    onToggle()
                                }
                            } else {
                                if (newChecked) {
                                    completionJob = scope.launch {
                                        delay(ThingsMotion.LONG.toLong())
                                        onToggle()
                                    }
                                }
                            }
                        },
                        size = ThingsIconSize.XS,
                        checkedColor = if (isSearchLogbookStyle) searchLogbookCheckColor() else ThingsTheme.colors.accent,
                        uncheckedColor = checkboxUncheckedColor,
                        isDashed = task.start == Item.START_SOMEDAY && !localCompleted && !isSearchLogbookStyle,
                        isCancelled = isSearchLogbookStyle && isCancelledTask,
                        modifier = Modifier
                            .testTag("task_checkbox")
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacingToText))

            // Название проекта или области идёт второй строкой под заголовком
            val project = remember(task.projectId, projects) {
                projects.firstOrNull { it.id == task.projectId }
            }
            val area = remember(task.areaId, areas) {
                areas.firstOrNull { it.id == task.areaId }
            }
            val subtitle = when {
                // В поиске и «В любое время» задачи стоят под заголовком своего проекта или области — подпись повторяла бы его
                screen == ActiveScreen.SEARCH && !isSearchLogbookStyle -> null
                screen == ActiveScreen.ANYTIME || screen == ActiveScreen.SOMEDAY -> null
                project != null && screen != ActiveScreen.PROJECT_DETAIL -> project.name
                project == null && area != null && screen != ActiveScreen.AREA_DETAIL -> area.title
                else -> null
            }
            // Когда под заголовком есть вторая строка, плашка с датой встаёт слева от обеих строк
            // и центрируется по вертикали, а заголовок с подписью идут после неё
            val badgeBesideBothLines = dateBadge != null && subtitle != null

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (badgeBesideBothLines) {
                    DateBadge(text = dateBadge!!)
                    Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                }

                // Дата завершения — отдельной колонкой слева от названия и подзаголовка,
                // по центру строки по вертикали
                if (isSearchLogbookStyle) {
                    val completedAt = task.stopDate ?: task.modificationDate
                    val dateText = remember(completedAt) { formatSearchLogbookDate(completedAt) }
                    Text(
                        text = dateText,
                        style = ThingsTheme.type.taskSubtitleStrong.copy(
                            fontSize = ThingsTheme.type.taskTitle.fontSize * 0.9f,
                            color = searchLogbookDateColor()
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(ThingsSpacing.S))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (dateBadge != null && !badgeBesideBothLines) {
                                DateBadge(text = dateBadge)
                                Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            } else if (dateIndicator != null && (
                                    screen == ActiveScreen.PROJECT_DETAIL || screen == ActiveScreen.AREA_DETAIL ||
                                        // В поиске открытые задачи стоят под проектом или областью — срок виден в строке
                                        (screen == ActiveScreen.SEARCH && !isSearchLogbookStyle)
                                )
                            ) {
                                when (dateIndicator) {
                                    is DateIndicatorResult.IconIndicator -> {
                                        Icon(
                                            imageVector = dateIndicator.icon,
                                            contentDescription = dateIndicator.contentDescription,
                                            tint = dateIndicator.color,
                                            modifier = Modifier.size(ThingsIconSize.XS)
                                        )
                                    }
                                    is DateIndicatorResult.TextIndicator -> {
                                        val displayText = if (dateIndicator.text == "TOMORROW_PLACEHOLDER") {
                                            androidx.compose.ui.res.stringResource(com.example.R.string.tomorrow)
                                        } else {
                                            dateIndicator.text
                                        }
                                    
                                        DateBadge(text = displayText)
                                    }
                                }
                                Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            }

                            Text(
                                text = task.title,
                                style = ThingsTheme.type.taskTitle.copy(
                                    color = animatedTitleColor,
                                    textDecoration = if (isSearchLogbookStyle && isCancelledTask) {
                                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    } else {
                                        null
                                    }
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
                            Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.XS),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (task.notes.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = stringResource(R.string.cd_has_notes),
                                        tint = textSecondaryColor.copy(alpha = ThingsAlpha.MUTED),
                                        modifier = Modifier.size(ROW_META_ICON_SIZE)
                                    )
                                }
                                if (task.checklistItemsCount > 0) {
                                    Icon(
                                        imageVector = AppIcons.BulletList,
                                        contentDescription = stringResource(R.string.cd_has_checklist),
                                        tint = textSecondaryColor.copy(alpha = ThingsAlpha.MUTED),
                                        modifier = Modifier.size(ROW_META_ICON_SIZE)
                                    )
                                }
                                if (task.cachedTags.isNotBlank()) {
                                    Icon(
                                        imageVector = AppIcons.Tag,
                                        contentDescription = stringResource(R.string.cd_has_tags),
                                        tint = textSecondaryColor.copy(alpha = ThingsAlpha.MUTED),
                                        modifier = Modifier.size(ROW_META_ICON_SIZE)
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

                        val relativeText = com.example.ui.screens.home.inlineeditor.utils.relativeDueText(delta)

                        val isOverdueOrToday = delta <= 0
                        val color = if (isOverdueOrToday) ThingsTheme.colors.danger else textSecondaryColor.copy(alpha = DUE_TEXT_ALPHA)

                        Spacer(modifier = Modifier.width(ThingsSpacing.S))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = ThingsSpacing.XS)
                        ) {
                            Icon(
                                imageVector = AppIcons.Deadline,
                                contentDescription = stringResource(R.string.cd_deadline_flag),
                                tint = color,
                                modifier = Modifier.size(DUE_ICON_SIZE)
                            )
                            Spacer(modifier = Modifier.width(ThingsSpacing.XS))
                            Text(
                                text = relativeText,
                                style = ThingsTheme.type.taskSubtitle.copy(
                                    color = color
                                )
                            )
                        }
                    }
                }

                if (subtitle != null) {
                    val subtitleColor = if (isHighlighted) {
                        androidx.compose.ui.graphics.lerp(ThingsTheme.colors.textSecondary, ThingsTheme.colors.accent, 0.45f)
                    } else {
                        ThingsTheme.colors.textSecondary
                    }
                    Text(
                        text = subtitle,
                        style = ThingsTheme.type.taskSubtitle.copy(
                            color = subtitleColor
                        )
                    )
                }
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
            val targetBorderColor = when {
                isSelected -> ThingsTheme.colors.accent
                isDragSelecting -> ThingsTheme.colors.accent.copy(alpha = ThingsAlpha.HALF)
                else -> ThingsTheme.colors.checkboxBorder
            }
            val animatedBorderColor by androidx.compose.animation.animateColorAsState(
                targetValue = targetBorderColor,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = ThingsMotion.BASE),
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

            val animatedDragSelectScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isDragSelecting) 1.14f else 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = ThingsMotion.BASE),
                label = "dragSelectScale"
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
                            durationMillis = SELECT_BOUNCE_MS,
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
                Spacer(modifier = Modifier.width(ThingsSpacing.S))
                val view = androidx.compose.ui.platform.LocalView.current
                Box(
                    modifier = Modifier
                        .size(SELECTION_CIRCLE_SIZE)
                        .graphicsLayer {
                            scaleX = iconBounceScale.value * animatedDragSelectScale
                            scaleY = iconBounceScale.value * animatedDragSelectScale
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
                    val selectionFill = ThingsTheme.colors.accent
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.size(ThingsIconSize.XL)
                    ) {
                        val strokeWidth = ThingsStroke.BOLD.toPx()
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
                            val targetInnerRadius = (radius - strokeWidth) - SELECTION_RING_GAP.toPx()
                            val currentInnerRadius = targetInnerRadius * animatedCheckScale
                            if (currentInnerRadius > 0f) {
                                drawCircle(
                                    color = selectionFill,
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

/** Серая плашка с датой у строки задачи: число и месяц в месячных разделах, дата — на экранах проекта и сферы */
@Composable
private fun DateBadge(text: String) {
    Text(
        text = text,
        style = ThingsTheme.type.badge.copy(
            color = ThingsTheme.colors.badgeText
        ),
        modifier = Modifier
            .background(
                color = ThingsTheme.colors.badgeBackground,
                shape = ThingsTheme.shapes.badgeShape
            )
            .padding(horizontal = ThingsSpacing.XS_PLUS, vertical = ThingsSpacing.XXS)
    )
}

private fun getStartDateIndicator(
    startDate: Long,
    isTonight: Boolean,
    eveningColor: Color,
    tonightDescription: String,
    todayDescription: String
): DateIndicatorResult {
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
                color = eveningColor,
                contentDescription = tonightDescription
            )
        } else {
            DateIndicatorResult.IconIndicator(
                icon = AppIcons.Today,
                color = androidx.compose.ui.graphics.Color.Unspecified,
                contentDescription = todayDescription
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

/** Заливка флажка выполненной/отменённой задачи в результатах поиска — фирменный голубой. */
@Composable
private fun searchLogbookCheckColor(): Color = ThingsTheme.colors.accent

/** Цвет даты завершения перед названием задачи в результатах поиска — фирменный голубой. */
@Composable
private fun searchLogbookDateColor(): Color = ThingsTheme.colors.accent

/** Короткая числовая дата в формате системы: 04/28/24 для en-US, 28.04.24 для ru/uk. */
private fun formatSearchLogbookDate(millis: Long): String {
    val locale = java.util.Locale.getDefault()
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "ddMMyy")
    return java.text.SimpleDateFormat(pattern, locale).format(java.util.Date(millis))
}
