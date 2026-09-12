package com.example.ui.screens.home.inlineeditor.dialogs

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
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
        delay(150)
        try {
            titleFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val currentProject = remember(selectedProjectId, projects) {
        projects.firstOrNull { it.id == selectedProjectId }
    }

    val destinationName = remember(currentProject, section) {
        if (currentProject != null) {
            currentProject.name
        } else {
            when (section) {
                TaskSection.TODAY -> if (isTonight) "This Evening" else "Today"
                TaskSection.UPCOMING -> "Upcoming"
                TaskSection.ANYTIME -> "Anytime"
                TaskSection.SOMEDAY -> "Someday"
                else -> "Inbox"
            }
        }
    }

    val density = LocalDensity.current
    val startOffsetPx = remember(density) { with(density) { 380.dp.toPx() } }
    val dismissNudgePx = remember(density) { with(density) { (-12).dp.toPx() } }
    val dismissDropPx = remember(density) { with(density) { 220.dp.toPx() } }

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
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
        }
        launch {
            scrimAlpha.animateTo(0.4f, animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
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
                    offsetY.animateTo(dismissNudgePx, tween(durationMillis = 90, easing = FastOutSlowInEasing))
                    offsetY.animateTo(dismissDropPx, tween(durationMillis = 260, easing = FastOutSlowInEasing))
                }
                // 2. Плавное растворение карточки и затемнения фона за 300мс
                launch {
                    delay(30)
                    alpha.animateTo(0f, tween(durationMillis = 270, easing = FastOutLinearInEasing))
                }
                launch {
                    scrimAlpha.animateTo(0f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
                }
                delay(300)
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
                    offsetY.animateTo(dismissNudgePx, tween(durationMillis = 90, easing = FastOutSlowInEasing))
                    offsetY.animateTo(dismissDropPx, tween(durationMillis = 260, easing = FastOutSlowInEasing))
                }
                // 2. Плавное растворение карточки и затемнения фона за 300мс
                launch {
                    delay(30)
                    alpha.animateTo(0f, tween(durationMillis = 270, easing = FastOutLinearInEasing))
                }
                launch {
                    scrimAlpha.animateTo(0f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
                }
                delay(300)
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
                    checklist,
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
            .background(Color.Black.copy(alpha = scrimAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                handleDismiss()
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .graphicsLayer {
                    translationY = offsetY.value
                    this.alpha = alpha.value
                }
                .statusBarsPadding()
                .padding(top = 4.dp)
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
                            .padding(start = 28.dp, end = 20.dp, top = 26.dp, bottom = 18.dp)
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
                                uncheckedColor = Color(0xFFC7C7CC),
                                modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 40.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (title.isEmpty()) {
                                        Text(
                                            text = "New To-Do",
                                            style = TextStyle(
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                                fontWeight = FontWeight.Normal,
                                                color = ThingsTextNotesLight.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    // Пока диалог закрывается, курсор и его маркер не рисуем: фокус остаётся
                                    // до полного скрытия клавиатуры (см. hideSoftKeyboardNow)
                                    HideTextSelectionHandles(hidden = isClosing) {
                                        BasicTextField(
                                            value = title,
                                            onValueChange = { title = it },
                                            textStyle = TextStyle(
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF1C1C1E)
                                            ),
                                            singleLine = false,
                                            maxLines = 4,
                                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                            cursorBrush = SolidColor(if (isClosing) Color.Unspecified else ThingsBlue),
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
                                            text = "Notes",
                                            style = TextStyle(
                                                fontSize = MaterialTheme.typography.taskEditorNotes.fontSize,
                                                fontWeight = FontWeight.Normal,
                                                color = ThingsTextNotesLight.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    HideTextSelectionHandles(hidden = isClosing) {
                                        BasicTextField(
                                            value = notes,
                                            onValueChange = { notes = it },
                                            minLines = 4,
                                            textStyle = TextStyle(
                                                fontSize = MaterialTheme.typography.taskEditorNotes.fontSize,
                                                fontWeight = FontWeight.Normal,
                                                color = ThingsTextNotesLight
                                            ),
                                            cursorBrush = SolidColor(if (isClosing) Color.Unspecified else ThingsBlue),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Чеклист (InlineChecklistPanel)
                        Box(modifier = Modifier.fillMaxWidth().padding(end = 40.dp)) {
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

                        Spacer(modifier = Modifier.height(32.dp))
                        // 4. Панель индикаторов (активные теги, дата, дедлайн) + Панель инструментов (иконки действий)
                    val textPrimaryColor = Color(0xFF1C1C1E)
                    val iconInactiveColor = Color(0xFFC7C7CC)
                    val bodyFontSize = MaterialTheme.typography.taskEditorDate.fontSize

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
                                if (isTonight) Color.Unspecified else ThingsTodayStar
                            }
                            startDate == null && section == TaskSection.TODAY -> {
                                if (isTonight) Color.Unspecified else ThingsTodayStar
                            }
                            section == TaskSection.SOMEDAY -> ThingsSomedayGrey
                            else -> Color.Unspecified
                        }
                    } else Color.Unspecified

                    val startRelativePadding = 24.dp

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
                                        .padding(top = 4.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    activeTags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFD1EAE2)) // light-teal background
                                                .clickable { showTagDialog = true }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = TextStyle(
                                                    color = Color(0xFF2C7D64), // dark-teal text
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
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
                                        .padding(vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = activeDateIcon,
                                        contentDescription = "Change date",
                                        tint = activeDateColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeDateLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = TextStyle(
                                            fontSize = bodyFontSize,
                                            color = textPrimaryColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            if (hasActiveDate && dueDate != null) {
                                Spacer(modifier = Modifier.height(12.dp))
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

                                val locale = remember(lang) {
                                    when (lang) {
                                        "uk" -> Locale("uk")
                                        "ru" -> Locale("ru")
                                        else -> Locale.US
                                    }
                                }
                                val sdf = remember(locale) { SimpleDateFormat("EEE, d MMMM", locale) }
                                val dateText = remember(dueDate) {
                                    dueDate?.let { sdf.format(Date(it)).lowercase() } ?: ""
                                }

                                val isOverdueOrToday = delta <= 0
                                val primaryColor = if (isOverdueOrToday) ThingsUpcomingRed else Color(0xFF1C1C1E)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showDatePicker = true }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Deadline,
                                        contentDescription = "Deadline Flag",
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dateText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = TextStyle(
                                            fontSize = bodyFontSize,
                                            color = primaryColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = relativeText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = TextStyle(
                                            fontSize = bodyFontSize,
                                            color = Color(0xFF8E8E93),
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Справа: ряд иконок тулбара (Когда, Теги, Чеклист, Дедлайн)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 10.dp, bottom = 4.dp)
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
                                        contentDescription = "Schedule",
                                        tint = iconInactiveColor,
                                        modifier = Modifier
                                            .size(22.dp)
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
                                        imageVector = Icons.Outlined.LocalOffer,
                                        contentDescription = "Tags",
                                        tint = iconInactiveColor,
                                        modifier = Modifier
                                            .size(22.dp)
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
                                        contentDescription = "Checklists",
                                        tint = iconInactiveColor,
                                        modifier = Modifier
                                            .size(22.dp)
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
                                        contentDescription = "Set Deadline",
                                        tint = iconInactiveColor,
                                        modifier = Modifier
                                            .size(22.dp)
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
                            .background(Color(0xFFF2F2F7))
                            .padding(start = 22.dp, end = 18.dp, top = 8.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопка выбора назначения
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMoveDialog = true }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Inbox,
                                contentDescription = "Destination",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = destinationName,
                                style = TextStyle(
                                    color = Color(0xFF8E8E93),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        // Кнопка "Save" (синяя капсула)
                        Button(
                            onClick = { handleSave() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThingsBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(percent = 50),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Save",
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // Круглая кнопка закрытия ✕ поверх карточки в правом верхнем углу
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E5EA))
                        .clickable { handleDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
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
