package com.example.ui.screens.home.inlineeditor.dialogs

import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.theme.ThingsTheme
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.example.ui.theme.AppIcons
import com.example.ui.theme.dimens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.HideTextSelectionHandles
import com.example.ui.components.hideSoftKeyboardNow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Area
import com.example.data.model.ChecklistItem
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.data.model.TaskSection
import com.example.ui.components.ThingsCheckbox
import com.example.ui.screens.home.inlineeditor.DeadlineDatePickerDialog
import com.example.ui.screens.home.inlineeditor.components.InlineChecklistPanel
import com.example.ui.screens.home.inlineeditor.utils.formatStartDateLabel
import com.example.ui.screens.home.inlineeditor.utils.isTodayDateOrPast
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Окно быстрой задачи: путь появления и ухода, поля карточки и нижней панели
private val QUICK_ADD_START_OFFSET = 380.dp
private val QUICK_ADD_DISMISS_DROP = 220.dp
private val QUICK_ADD_CARD_TOP = 26.dp
private val QUICK_ADD_CARD_BOTTOM = 18.dp
private val QUICK_ADD_TITLE_END = 40.dp
private val QUICK_ADD_INDICATORS_START = 24.dp
private val QUICK_ADD_FOOTER_START = 22.dp
private val QUICK_ADD_FOOTER_END = 18.dp
private val QUICK_ADD_FOOTER_BUTTON_HEIGHT = 36.dp

// Закрытие окна: подскок, уход вниз и растворение
private const val DISMISS_NUDGE_MS = 90
private const val DISMISS_DROP_MS = 260
private const val DISMISS_FADE_DELAY_MS = 30L
private const val DISMISS_FADE_MS = 270

/**
 * Диалог быстрого добавления новой задачи (Quick Add Dialog).
 * Содержит все элементы и интерактивность раскрытой для редактирования задачи (TaskInlineEditor),
 * дополненный верхней кнопкой отмены (✕) и нижней панелью назначения (Inbox / Project / Area)
 * с кнопкой сохранения ("Save").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddDialog(
    projects: List<Item>,
    areas: List<Area> = emptyList(),
    allSavedTags: List<String> = emptyList(),
    allSavedTagObjects: List<Tag> = emptyList(),
    allTasksRaw: List<ItemWithChecklist> = emptyList(),
    onNewTagCreated: (String, String?) -> Unit = { _, _ -> },
    onDeleteTag: (Tag) -> Unit = {},
    onUpdateTag: (Tag) -> Unit = {},
    onUpdateTagsOrder: (List<Tag>) -> Unit = {},
    onSave: (
        title: String,
        notes: String,
        section: TaskSection,
        isTonight: Boolean,
        startDate: Long?,
        dueDate: Long?,
        tags: List<String>,
        projectId: String?,
        checklist: List<ChecklistItem>,
        priority: Int
    ) -> Unit,
    onDismissRequest: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var section by remember { mutableStateOf(TaskSection.INBOX) }
    var isTonight by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var tagInput by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var checklist by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var priority by remember { mutableStateOf(0) }

    // Visibility controllers
    var showWhenDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showChecklistHelper by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    val view = LocalView.current

    LaunchedEffect(Unit) {
        delay(ThingsMotion.QUICK.toLong())
        try {
            titleFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val currentProject = remember(selectedProjectId, projects) {
        projects.firstOrNull { it.id == selectedProjectId }
    }

    val destinationName = if (currentProject != null) {
        currentProject.name
    } else {
        when (section) {
            TaskSection.TODAY -> if (isTonight) androidx.compose.ui.res.stringResource(com.example.R.string.category_this_evening) else androidx.compose.ui.res.stringResource(com.example.R.string.category_today)
            TaskSection.UPCOMING -> androidx.compose.ui.res.stringResource(com.example.R.string.category_upcoming)
            TaskSection.ANYTIME -> androidx.compose.ui.res.stringResource(com.example.R.string.category_anytime)
            TaskSection.SOMEDAY -> androidx.compose.ui.res.stringResource(com.example.R.string.category_someday)
            else -> androidx.compose.ui.res.stringResource(com.example.R.string.category_inbox)
        }
    }

    val density = LocalDensity.current
    val startOffsetPx = remember(density) { with(density) { QUICK_ADD_START_OFFSET.toPx() } }
    val dismissNudgePx = remember(density) { with(density) { (-12).dp.toPx() } }
    val dismissDropPx = remember(density) { with(density) { QUICK_ADD_DISMISS_DROP.toPx() } }

    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(startOffsetPx) }
    val alpha = remember { Animatable(0f) }
    val scrimAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 240f
                )
            )
        }
        launch {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = ThingsMotion.BASE, easing = FastOutSlowInEasing))
        }
        launch {
            scrimAlpha.animateTo(0.4f, animationSpec = tween(durationMillis = ThingsMotion.MEDIUM, easing = FastOutSlowInEasing))
        }
    }

    var isClosing by remember { mutableStateOf(false) }

    val handleDismiss: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            // Клавиатуру прячем сразу, а фокус не снимаем: поле уйдёт вместе с диалогом через 300 мс,
            // когда клавиатура уже скрыта полностью (см. hideSoftKeyboardNow)
            view.hideSoftKeyboardNow()
            coroutineScope.launch {
                // 1. Мягкий подскок вверх (-12dp), затем плавный уход вниз
                launch {
                    offsetY.animateTo(dismissNudgePx, tween(durationMillis = DISMISS_NUDGE_MS, easing = FastOutSlowInEasing))
                    offsetY.animateTo(dismissDropPx, tween(durationMillis = DISMISS_DROP_MS, easing = FastOutSlowInEasing))
                }
                // 2. Плавное растворение карточки и затемнения фона за 300мс
                launch {
                    delay(DISMISS_FADE_DELAY_MS)
                    alpha.animateTo(0f, tween(durationMillis = DISMISS_FADE_MS, easing = FastOutLinearInEasing))
                }
                launch {
                    scrimAlpha.animateTo(0f, tween(durationMillis = ThingsMotion.STANDARD, easing = FastOutSlowInEasing))
                }
                delay(ThingsMotion.STANDARD.toLong())
                onDismissRequest()
            }
        }
    }

    val handleSave: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            // Клавиатуру прячем сразу, а фокус не снимаем: поле уйдёт вместе с диалогом через 300 мс,
            // когда клавиатура уже скрыта полностью (см. hideSoftKeyboardNow)
            view.hideSoftKeyboardNow()
            coroutineScope.launch {
                // 1. Мягкий подскок вверх (-12dp), затем плавный уход вниз
                launch {
                    offsetY.animateTo(dismissNudgePx, tween(durationMillis = DISMISS_NUDGE_MS, easing = FastOutSlowInEasing))
                    offsetY.animateTo(dismissDropPx, tween(durationMillis = DISMISS_DROP_MS, easing = FastOutSlowInEasing))
                }
                // 2. Плавное растворение карточки и затемнения фона за 300мс
                launch {
                    delay(DISMISS_FADE_DELAY_MS)
                    alpha.animateTo(0f, tween(durationMillis = DISMISS_FADE_MS, easing = FastOutLinearInEasing))
                }
                launch {
                    scrimAlpha.animateTo(0f, tween(durationMillis = ThingsMotion.STANDARD, easing = FastOutSlowInEasing))
                }
                delay(ThingsMotion.STANDARD.toLong())
                val tagList = tagInput.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                onSave(
                    title.trim(),
                    notes.trim(),
                    section,
                    isTonight,
                    startDate,
                    dueDate,
                    tagList,
                    selectedProjectId,
                    checklist.filter { it.title.isNotBlank() },
                    priority
                )
                onDismissRequest()
            }
        }
    }

    BackHandler(onBack = handleDismiss)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(ThingsTheme.colors.scrim.copy(alpha = scrimAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                handleDismiss()
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            shape = ThingsTheme.shapes.floatingCardShape,
            colors = CardDefaults.cardColors(containerColor = ThingsTheme.colors.background),
            elevation = CardDefaults.cardElevation(defaultElevation = ThingsElevation.CARD),
            modifier = Modifier
                .graphicsLayer {
                    translationY = offsetY.value
                    this.alpha = alpha.value
                }
                .statusBarsPadding()
                .padding(top = ThingsSpacing.XS)
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Предотвращаем закрытие при клике внутри */ }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = ThingsSpacing.L_PLUS, end = ThingsSpacing.L, top = QUICK_ADD_CARD_TOP, bottom = QUICK_ADD_CARD_BOTTOM)
                    ) {
                        // 1. Верхний ряд: Чекбокс + Заголовок (Title)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            ThingsCheckbox(
                                checked = false,
                                onCheckedChange = { /* В режиме создания нового чекбокс пассивный */ },
                                size = MaterialTheme.dimens.mainCheckboxSize,
                                uncheckedColor = ThingsTheme.colors.checkboxBorder,
                                modifier = Modifier.padding(end = ThingsSpacing.S_PLUS, top = ThingsSpacing.XXS)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = QUICK_ADD_TITLE_END)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (title.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.ui_new_to_do),
                                            style = ThingsTheme.type.taskTitle.copy(
                                                color = ThingsTheme.colors.textNotes.copy(alpha = ThingsAlpha.HALF)
                                            )
                                        )
                                    }
                                    // Пока диалог закрывается, курсор и его маркер не рисуем: фокус остаётся
                                    // до полного скрытия клавиатуры (см. hideSoftKeyboardNow)
                                    HideTextSelectionHandles(hidden = isClosing) {
                                        BasicTextField(
                                            value = title,
                                            onValueChange = { title = it },
                                            textStyle = ThingsTheme.type.taskTitle.copy(
                                                color = ThingsTheme.colors.editorText
                                            ),
                                            singleLine = false,
                                            maxLines = 4,
                                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                            cursorBrush = SolidColor(if (isClosing) Color.Unspecified else ThingsTheme.colors.accent),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(titleFocusRequester)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(MaterialTheme.dimens.taskExpandedTitleNotesGap))

                                // 2. Поле заметки (Notes)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (notes.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.ui_notes),
                                            style = ThingsTheme.type.editorNotes.copy(
                                                color = ThingsTheme.colors.textNotes.copy(alpha = ThingsAlpha.HALF)
                                            )
                                        )
                                    }
                                    HideTextSelectionHandles(hidden = isClosing) {
                                        BasicTextField(
                                            value = notes,
                                            onValueChange = { notes = it },
                                            minLines = 4,
                                            textStyle = ThingsTheme.type.editorNotes.copy(
                                                color = ThingsTheme.colors.textNotes
                                            ),
                                            cursorBrush = SolidColor(if (isClosing) Color.Unspecified else ThingsTheme.colors.accent),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(ThingsSpacing.L))

                        // 3. Чеклист (InlineChecklistPanel)
                        Box(modifier = Modifier.fillMaxWidth().padding(end = QUICK_ADD_TITLE_END)) {
                            HideTextSelectionHandles(hidden = isClosing) {
                                InlineChecklistPanel(
                                    itemId = "",
                                    checklist = checklist,
                                    onChecklistChange = { checklist = it },
                                    showChecklistHelper = showChecklistHelper,
                                    onShowChecklistHelperChange = { showChecklistHelper = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(ThingsSpacing.XXL))
                        // 4. Панель индикаторов (активные теги, дата, дедлайн) + Панель инструментов (иконки действий)
                    val textPrimaryColor = ThingsTheme.colors.editorText
                    val iconInactiveColor = ThingsTheme.colors.checkboxBorder

                    val hasActiveDate = startDate != null || section == TaskSection.TODAY || section == TaskSection.SOMEDAY

                    val activeDateLabel = if (hasActiveDate) {
                        formatStartDateLabel(startDate, section, isTonight)
                    } else ""

                    val activeDateIcon = if (hasActiveDate) {
                        when {
                            startDate != null && isTodayDateOrPast(startDate) -> {
                                if (isTonight) AppIcons.Evening else AppIcons.Today
                            }
                            startDate == null && section == TaskSection.TODAY -> {
                                if (isTonight) AppIcons.Evening else AppIcons.Today
                            }
                            section == TaskSection.SOMEDAY -> AppIcons.Someday
                            else -> AppIcons.Upcoming
                        }
                    } else AppIcons.Upcoming

                    val activeDateColor = if (hasActiveDate) {
                        when {
                            startDate != null && isTodayDateOrPast(startDate) -> {
                                if (isTonight) Color.Unspecified else ThingsTheme.colors.today
                            }
                            startDate == null && section == TaskSection.TODAY -> {
                                if (isTonight) Color.Unspecified else ThingsTheme.colors.today
                            }
                            section == TaskSection.SOMEDAY -> ThingsTheme.colors.someday
                            else -> Color.Unspecified
                        }
                    } else Color.Unspecified

                    val startRelativePadding = QUICK_ADD_INDICATORS_START

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Слева: теги, дата и дедлайн
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = startRelativePadding),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Активные чипы тегов
                            val activeTags = remember(tagInput) {
                                tagInput.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                            }
                            if (activeTags.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = ThingsSpacing.XS, bottom = ThingsSpacing.M),
                                    horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.S),
                                    verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XS_PLUS)
                                ) {
                                    activeTags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(ThingsTheme.shapes.chipShape)
                                                .background(ThingsTheme.colors.tagChipBackground) // light-teal background
                                                .clickable { showTagDialog = true }
                                                .padding(horizontal = ThingsSpacing.S_PLUS, vertical = ThingsSpacing.XS)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = ThingsTheme.type.tagChip.copy(
                                                    color = ThingsTheme.colors.tagChipText
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Индикатор установленной даты
                            if (hasActiveDate) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showWhenDialog = true }
                                        .padding(vertical = ThingsSpacing.XS_PLUS)
                                ) {
                                    Icon(
                                        imageVector = activeDateIcon,
                                        contentDescription = stringResource(R.string.cd_change_date),
                                        tint = activeDateColor,
                                        modifier = Modifier.size(ThingsIconSize.S)
                                    )
                                    Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                                    Text(
                                        text = activeDateLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = ThingsTheme.type.editorDateStrong.copy(
                                            color = textPrimaryColor
                                        )
                                    )
                                }
                            }

                            if (hasActiveDate && dueDate != null) {
                                Spacer(modifier = Modifier.height(ThingsSpacing.M))
                            }

                            // Индикатор установленного дедлайна (Due Date)
                            if (dueDate != null) {
                                val delta = remember(dueDate) {
                                    val today = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val due = Calendar.getInstance().apply {
                                        timeInMillis = dueDate!!
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val diffMillis = due.timeInMillis - today.timeInMillis
                                    if (diffMillis >= 0) {
                                        (diffMillis / (24 * 60 * 60 * 1000L)).toInt()
                                    } else {
                                        ((diffMillis - (24 * 60 * 60 * 1000L - 1)) / (24 * 60 * 60 * 1000L)).toInt()
                                    }
                                }

                                val lang = remember { Locale.getDefault().language }
                                val relativeText = com.example.ui.screens.home.inlineeditor.utils.relativeDueText(delta)

                                val locale = remember { java.util.Locale.getDefault() }
                                val sdf = remember(locale) { SimpleDateFormat("EEE, d MMMM", locale) }
                                val dateText = remember(dueDate) {
                                    dueDate?.let { sdf.format(Date(it)).lowercase() } ?: ""
                                }

                                val isOverdueOrToday = delta <= 0
                                val primaryColor = if (isOverdueOrToday) ThingsTheme.colors.danger else ThingsTheme.colors.editorText

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showDatePicker = true }
                                        .padding(vertical = ThingsSpacing.XS_PLUS)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Deadline,
                                        contentDescription = stringResource(R.string.cd_deadline_flag),
                                        tint = primaryColor,
                                        modifier = Modifier.size(ThingsIconSize.S)
                                    )
                                    Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                                    Text(
                                        text = dateText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = ThingsTheme.type.editorDateStrong.copy(
                                            color = primaryColor
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(ThingsSpacing.XS))
                                    Text(
                                        text = relativeText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = ThingsTheme.type.editorDate.copy(
                                            color = ThingsTheme.colors.textSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(ThingsSpacing.L))

                        // Справа: ряд иконок тулбара (Когда, Теги, Чеклист, Дедлайн)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = ThingsSpacing.S_PLUS, bottom = ThingsSpacing.XS)
                        ) {
                            // Иконка Когда (Календарь)
                            AnimatedVisibility(
                                visible = !hasActiveDate,
                                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                            ) {
                                Box(modifier = Modifier.padding(start = MaterialTheme.dimens.taskEditorActionIconsSpacing)) {
                                    Icon(
                                        imageVector = AppIcons.Upcoming,
                                        contentDescription = stringResource(R.string.cd_schedule),
                                        tint = iconInactiveColor,
                                        modifier = Modifier
                                            .size(ThingsIconSize.L)
                                            .clickable { showWhenDialog = true }
                                    )
                                }
                            }

                            // Иконка Тегов
                            AnimatedVisibility(
                                visible = tagInput.trim().isEmpty(),
                                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                            ) {
                                Box(modifier = Modifier.padding(start = MaterialTheme.dimens.taskEditorActionIconsSpacing)) {
                                    Icon(
                                        imageVector = AppIcons.Tag,
                                        contentDescription = stringResource(R.string.tag_dialog_title),
                                        tint = iconInactiveColor,
                                        modifier = Modifier
                                            .size(ThingsIconSize.M)
                                            .clickable { showTagDialog = true }
                                    )
                                }
                            }

                            // Иконка Чеклиста
                            AnimatedVisibility(
                                visible = !showChecklistHelper && checklist.isEmpty(),
                                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                            ) {
                                Box(modifier = Modifier.padding(start = MaterialTheme.dimens.taskEditorActionIconsSpacing)) {
                                    Icon(
                                        imageVector = AppIcons.BulletList,
                                        contentDescription = stringResource(R.string.cd_checklists),
                                        tint = iconInactiveColor,
                                        modifier = Modifier
                                            .size(ThingsIconSize.L)
                                            .clickable { showChecklistHelper = true }
                                    )
                                }
                            }

                            // Иконка Дедлайна (Флаг)
                            AnimatedVisibility(
                                visible = dueDate == null,
                                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                            ) {
                                Box(modifier = Modifier.padding(start = MaterialTheme.dimens.taskEditorActionIconsSpacing)) {
                                    Icon(
                                        imageVector = AppIcons.Deadline,
                                        contentDescription = stringResource(R.string.batch_action_set_deadline),
                                        tint = iconInactiveColor,
                                        modifier = Modifier
                                            .size(ThingsIconSize.L)
                                            .clickable { showDatePicker = true }
                                    )
                                }
                            }
                        }
                    }
                    }

                    // 5. Нижняя панель (Footer): серый фон как в iOS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThingsTheme.colors.surface)
                            .padding(start = QUICK_ADD_FOOTER_START, end = QUICK_ADD_FOOTER_END, top = ThingsSpacing.S, bottom = ThingsSpacing.S_PLUS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопка выбора назначения
                        Row(
                            modifier = Modifier
                                .clip(ThingsTheme.shapes.rowShape)
                                .clickable { showMoveDialog = true }
                                .padding(horizontal = ThingsSpacing.XS_PLUS, vertical = ThingsSpacing.XS_PLUS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Inbox,
                                contentDescription = stringResource(R.string.cd_destination),
                                tint = ThingsTheme.colors.textSecondary,
                                modifier = Modifier.size(ThingsIconSize.M)
                            )
                            Spacer(modifier = Modifier.width(ThingsSpacing.XS_PLUS))
                            Text(
                                text = destinationName,
                                style = ThingsTheme.type.editorChecklistStrong.copy(
                                    color = ThingsTheme.colors.textSecondary
                                )
                            )
                        }

                        // Кнопка "Save" (синяя капсула)
                        Button(
                            onClick = { handleSave() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThingsTheme.colors.accent,
                                contentColor = ThingsTheme.colors.onAccent
                            ),
                            shape = ThingsTheme.shapes.capsuleShape,
                            contentPadding = PaddingValues(horizontal = ThingsSpacing.XL, vertical = ThingsSpacing.XS_PLUS),
                            modifier = Modifier.height(QUICK_ADD_FOOTER_BUTTON_HEIGHT)
                        ) {
                            Text(
                                text = stringResource(R.string.tag_dialog_save),
                                style = ThingsTheme.type.button
                            )
                        }
                    }
                }

                // Круглая кнопка закрытия ✕ поверх карточки в правом верхнем углу
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = ThingsSpacing.L, end = ThingsSpacing.L)
                        .size(MaterialTheme.dimens.dialogHeaderButtonSize)
                        .clip(CircleShape)
                        .background(ThingsTheme.colors.divider)
                        .clickable { handleDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.tag_dialog_cancel),
                        tint = ThingsTheme.colors.textSecondary,
                        modifier = Modifier.size(ThingsIconSize.S)
                    )
                }
            }
        }
    }

    // ----------------------------------------------------
    // Вложенные диалоги: When, Tags, DatePicker, Move
    // ----------------------------------------------------
    if (showWhenDialog) {
        ThingsWhenDialog(
            startDate = startDate,
            section = section,
            isTonight = isTonight,
            onStartDateChange = { startDate = it },
            onSectionChange = { section = it },
            onIsTonightChange = { isTonight = it },
            onShowCalendarHelperChange = { /* no-op */ },
            onDismissRequest = { showWhenDialog = false }
        )
    }

    if (showTagDialog) {
        ThingsTagDialog(
            activeTags = remember(tagInput) {
                tagInput.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            },
            allSavedTags = allSavedTags,
            allSavedTagObjects = allSavedTagObjects,
            onNewTagCreated = onNewTagCreated,
            onDeleteTag = onDeleteTag,
            onUpdateTag = onUpdateTag,
            onUpdateTagsOrder = onUpdateTagsOrder,
            onTagsSelected = { selected ->
                tagInput = selected.joinToString(", ")
                showTagDialog = false
            },
            onDismissRequest = { showTagDialog = false }
        )
    }

    if (showDatePicker) {
        DeadlineDatePickerDialog(
            initialSelectedDateMillis = dueDate,
            onDateSelected = {
                dueDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showMoveDialog) {
        ThingsMoveDialog(
            currentProjectId = selectedProjectId,
            currentAreaId = null,
            currentIsInbox = selectedProjectId == null && section == TaskSection.INBOX,
            projects = projects,
            areas = areas,
            allTasks = allTasksRaw,
            onMove = { projId, _, moveToInbox ->
                selectedProjectId = projId
                if (moveToInbox) {
                    section = TaskSection.INBOX
                }
                showMoveDialog = false
            },
            onDismissRequest = { showMoveDialog = false }
        )
    }
}
