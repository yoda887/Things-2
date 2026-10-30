package com.example.ui.screens.home.subcomponents

import com.example.ui.components.foundHighlight
import androidx.compose.material.icons.filled.Notifications
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
import com.example.ui.components.selectionTint
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

private const val COMPLETION_FILL_MS = 350

// Импульс нажатия чекбокса
private const val TITLE_PRESS_MS = 70L
private const val CARD_PRESS_MS = 50L
private const val COMPLETE_TOGGLE_DELAY_MS = 220L
// Служебные иконки строки (напоминание, заметки, чек-лист, теги): чуть крупнее и темнее подписей
private val ROW_META_ICON_SIZE = 14.dp
private const val ROW_META_ICON_ALPHA = ThingsAlpha.HALF
/** Значок повтора перед названием повторяющейся задачи и её копий */
private val ROW_REPEAT_ICON_SIZE = 15.dp
private val DUE_ICON_SIZE = 15.dp
// Новое в «Сегодня» — жёлтая точка слева от названия
private val TODAY_NEW_DOT_SIZE = 8.dp
private const val DUE_TEXT_ALPHA = 0.85f

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
    dateBadge: String? = null,
    // Значки напоминания, заметок, чек-листа, тегов и дедлайн; в быстром поиске их нет — только название
    showMetaIcons: Boolean = true,
    // Дата старта (плашка, звезда, луна, день повтора шаблона); в быстром поиске её нет
    showStartDate: Boolean = true
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
    // Выполненные и отменённые задачи в «Журнале» и в результатах поиска — как в Logbook эталона: без
    // серой подложки, с заполненным тёмно-синим флажком и датой завершения перед названием
    val isSearchLogbookStyle = (screen == ActiveScreen.SEARCH || screen == ActiveScreen.LOGBOOK) &&
        (task.isCompleted || isCancelledTask)

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
    val found = com.example.ui.components.rememberFoundHighlight(isFound)
    val highlightColor = when {
        // Выделенная строка результатов Quick Find — 30 % акцента
        isHighlighted -> ThingsTheme.colors.accent.copy(alpha = ThingsAlpha.LOW)
        else -> Color.Transparent
    }

    val animatedRowBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isDragging) ThingsTheme.colors.surface else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = ThingsMotion.BASE),
        label = "rowBgColor_${task.id}"
    )
    // Выделение — поверх всей строки (foreground), чтобы ложилось и на плашки, чипы и чекбокс
    val animatedRowTint by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isDragging -> Color.Transparent
            isSelected -> ThingsTheme.colors.accentSelection
            else -> highlightColor
        },
        animationSpec = androidx.compose.animation.core.tween(durationMillis = ThingsMotion.BASE),
        label = "rowTint_${task.id}"
    )

    val fillBgColor = ThingsTheme.colors.dropPlaceholder

    val isCalendarTask = task.id.startsWith("cal_")

    val eveningColor = ThingsTheme.colors.eveningIndicator
    val tonightDescription = stringResource(R.string.cd_tonight)
    val todayDescription = stringResource(R.string.category_today)
    // Число и месяц — словом или цифрами, по настройке «Месяц словом в датах»
    val monthName = com.example.ui.components.LocalDatesWithMonthName.current
    val dateIndicator = remember(task.startDate, task.isTonight, task.start, task.dueDate, eveningColor, tonightDescription, todayDescription, monthName) {
        if (task.startDate != null) {
            getStartDateIndicator(task.startDate, task.isTonight, eveningColor, tonightDescription, todayDescription, monthName)
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
                scaleX = scale * cardScale * found.scale
                scaleY = scale * cardScale * found.scale
            }
            .shadow(elevation, ThingsTheme.shapes.rowShape)
            .background(animatedRowBgColor, ThingsTheme.shapes.rowShape)
            .selectionTint(animatedRowTint, ThingsTheme.shapes.rowShape)
            // Жёлтый найденной задачи — поверх всей строки (foreground), как выделение
            .foundHighlight(found, ThingsTheme.shapes.rowShape)
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
                        onCheckedChange = onCheckedChange@{
                            // Задачу шаблона повторяющегося проекта не отмечают — отмечают её копии
                            if (task.isTemplatePart) return@onCheckedChange
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
                                        // Повторяющаяся задача отмечена заранее: выполнена копия, сама она остаётся —
                                        // уже со следующим повтором, и чекбокс снова пуст
                                        if (task.isRepeatTemplate) {
                                            delay(ThingsMotion.LONG.toLong())
                                            localCompleted = false
                                        }
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
            // Где задача и так под заголовком своего проекта или области — без подписи (taskSubtitle, общая с редактором)
            val subtitle = taskSubtitle(task, project, area, screen, com.example.ui.components.LocalTodayGrouped.current)
            // Когда под заголовком есть вторая строка, плашка с датой встаёт слева от обеих строк
            // и центрируется по вертикали, а заголовок с подписью идут после неё
            // У шаблона повтора вместо даты — день следующего повтора или «Пауза», на любом экране: свои дата
            // и «Сегодня» у шаблона ничего не значат (в раскрытом шаблоне — тоже день следующего повтора)
            // В «Планах» шаблон стоит на днях своих повторов, как обычная задача: в дневных разделах без плашки,
            // в месячных — плашка дня этого повтора (число и месяц), её даёт экран через dateBadge
            val repeatRule = if (task.isRepeatTemplate && screen != ActiveScreen.UPCOMING) {
                com.example.ui.components.LocalRepeatRules.current[task.id]
            } else null
            val repeatBadge = com.example.ui.components.repeatBadge(repeatRule)
            val dateBadge = when {
                !showStartDate -> null
                repeatRule != null -> repeatBadge
                // Копия, созданная заранее, в «Повторяющихся» — под шаблоном, со своим днём
                // Копия в «Повторяющихся» — как задача на экране проекта: звезда или луна сегодня, иначе день
                // (это показывает индикатор даты ниже); у отложенной — «Когда-нибудь»
                screen == ActiveScreen.REPEATING && task.isRepeatCopy ->
                    if (task.start == Item.START_SOMEDAY) stringResource(R.string.category_someday) else null
                else -> dateBadge
            }
            val badgeBesideBothLines = dateBadge != null && subtitle != null
            // Копия в «Повторяющихся»: звезда, луна или день — как у задачи на экране проекта; с подписью под
            // названием — слева от обеих строк, по центру, как плашка. Шаблоны — только своей плашкой
            val repeatingCopyIndicator = screen == ActiveScreen.REPEATING && task.isRepeatCopy && dateBadge == null && dateIndicator != null
            val indicatorBesideBothLines = repeatingCopyIndicator && subtitle != null

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (badgeBesideBothLines) {
                    DateBadge(text = dateBadge!!)
                    Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                } else if (indicatorBesideBothLines) {
                    DateIndicatorMark(dateIndicator!!)
                    Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                }

                // Дата завершения — отдельной колонкой слева от названия и подзаголовка,
                // по центру строки по вертикали
                if (isSearchLogbookStyle) {
                    ClosedDateLabel(closedAt = task.stopDate ?: task.modificationDate, inLogbook = screen == ActiveScreen.LOGBOOK)
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
                            } else if (showStartDate && dateBadge == null && !indicatorBesideBothLines && dateIndicator != null && !task.isRepeatTemplate && (
                                    screen == ActiveScreen.PROJECT_DETAIL || screen == ActiveScreen.AREA_DETAIL ||
                                        repeatingCopyIndicator ||
                                        // В «В любое время» звезда или луна отмечают задачи «Сегодня»
                                        screen == ActiveScreen.ANYTIME ||
                                        // На экране тега — звезда, луна или день старта, как на экране проекта
                                        screen == ActiveScreen.TAG_DETAIL ||
                                        // Во «Входящих» дата бывает только у ушедших оттуда задач под плашкой
                                        screen == ActiveScreen.INBOX ||
                                        // В поиске открытые задачи стоят под проектом или областью — срок виден в строке
                                        (screen == ActiveScreen.SEARCH && !isSearchLogbookStyle)
                                )
                            ) {
                                DateIndicatorMark(dateIndicator)
                                Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            }

                            // Повторяющаяся задача — синий значок повтора перед названием, как в Things
                            // (у копий — серая стрелка в ряду служебных иконок справа)
                            if (task.isRepeatTemplate) {
                                com.example.ui.components.RepeatTemplateIcon(modifier = Modifier.size(ROW_REPEAT_ICON_SIZE))
                                Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            }
                            // Новое в «Сегодня» (пришло само: настал день, копия повтора) — жёлтая точка, до «OK» на плашке
                            if (screen == ActiveScreen.TODAY && task.id in com.example.ui.components.LocalTodayNew.current.ids) {
                                Box(
                                    modifier = Modifier
                                        .size(TODAY_NEW_DOT_SIZE)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(ThingsTheme.colors.todayNew)
                                )
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
                        // Напоминание открытой задачи — колокольчик в ряду с заметками, чек-листом и тегами, без времени
                        val hasReminder = showMetaIcons && task.reminderTime != null && !task.isClosed
                        val hasNotes = showMetaIcons && task.notes.isNotBlank()
                        val hasChecklist = showMetaIcons && task.checklistItemsCount > 0
                        val hasTags = showMetaIcons && task.cachedTags.isNotBlank()
                        if (task.isRepeatCopy || hasReminder || hasNotes || hasChecklist || hasTags) {
                            Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.XS),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Копия повторяющейся задачи — серая круговая стрелка, первой в ряду
                                if (task.isRepeatCopy) {
                                    Icon(
                                        imageVector = AppIcons.RepeatCopy,
                                        contentDescription = stringResource(R.string.cd_repeat_copy),
                                        tint = textSecondaryColor.copy(alpha = ROW_META_ICON_ALPHA),
                                        modifier = Modifier.size(ROW_META_ICON_SIZE)
                                    )
                                }
                                if (hasReminder) {
                                    Icon(
                                        imageVector = AppIcons.Bell,
                                        contentDescription = stringResource(R.string.reminder_title),
                                        tint = textSecondaryColor.copy(alpha = ROW_META_ICON_ALPHA),
                                        modifier = Modifier.size(ROW_META_ICON_SIZE)
                                    )
                                }
                                if (hasNotes) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = stringResource(R.string.cd_has_notes),
                                        tint = textSecondaryColor.copy(alpha = ROW_META_ICON_ALPHA),
                                        modifier = Modifier.size(ROW_META_ICON_SIZE)
                                    )
                                }
                                if (hasChecklist) {
                                    Icon(
                                        imageVector = AppIcons.BulletList,
                                        contentDescription = stringResource(R.string.cd_has_checklist),
                                        tint = textSecondaryColor.copy(alpha = ROW_META_ICON_ALPHA),
                                        modifier = Modifier.size(ROW_META_ICON_SIZE)
                                    )
                                }
                                if (hasTags) {
                                    Icon(
                                        imageVector = AppIcons.Tag,
                                        contentDescription = stringResource(R.string.cd_has_tags),
                                        tint = textSecondaryColor.copy(alpha = ROW_META_ICON_ALPHA),
                                        modifier = Modifier.size(ROW_META_ICON_SIZE)
                                    )
                                }
                            }
                        }
                    }

                    if (showMetaIcons && task.dueDate != null) {
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
            SelectionCircle(isSelected = isSelected, isDragSelecting = isDragSelecting, onToggle = onToggleSelect)
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
internal fun DateBadge(text: String) {
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

/** Звезда или луна (сегодня, вечер) либо плашка с днём — индикатор дня старта задачи в строке */
@Composable
private fun DateIndicatorMark(indicator: DateIndicatorResult) {
    when (indicator) {
        is DateIndicatorResult.IconIndicator -> Icon(
            imageVector = indicator.icon,
            contentDescription = indicator.contentDescription,
            tint = indicator.color,
            modifier = Modifier.size(ThingsIconSize.XS)
        )
        is DateIndicatorResult.TextIndicator -> DateBadge(
            text = if (indicator.text == "TOMORROW_PLACEHOLDER") stringResource(R.string.tomorrow) else indicator.text
        )
    }
}

/**
 * Текст плашки дня старта — в том же формате, что у задач на экране проекта: «Сегодня», «Завтра», день
 * недели на этой неделе («Пт»), год у следующих лет («2027»), иначе число и месяц («22 жовт.»). Им пишут
 * плашки и в списках, где дата — не «Когда» задачи: «Повторяющиеся», «Все проекты», «Планы» области.
 */
@Composable
internal fun startDateBadgeText(startDate: Long, isTonight: Boolean = false): String =
    when (val indicator = getStartDateIndicator(
        startDate, false, Color.Unspecified, "", "", com.example.ui.components.LocalDatesWithMonthName.current
    )) {
        is DateIndicatorResult.IconIndicator ->
            stringResource(if (isTonight) R.string.category_this_evening else R.string.category_today)
        is DateIndicatorResult.TextIndicator ->
            if (indicator.text == "TOMORROW_PLACEHOLDER") stringResource(R.string.tomorrow) else indicator.text
    }

private fun getStartDateIndicator(
    startDate: Long,
    isTonight: Boolean,
    eveningColor: Color,
    tonightDescription: String,
    todayDescription: String,
    monthName: Boolean = true
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
    // «22 жовт.» или цифрами «22.10.», как в «Журнале» (настройка)
    val monthAndDayStr = java.text.SimpleDateFormat(if (monthName) "d MMM" else "d.M.", locale).format(taskCal.time)
    return DateIndicatorResult.TextIndicator(monthAndDayStr)
}

/** Заливка флажка выполненной/отменённой задачи в результатах поиска — фирменный голубой. */
@Composable
private fun searchLogbookCheckColor(): Color = ThingsTheme.colors.accent
