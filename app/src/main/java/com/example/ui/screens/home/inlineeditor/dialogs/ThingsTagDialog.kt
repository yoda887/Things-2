package com.example.ui.screens.home.inlineeditor.dialogs

import com.example.ui.theme.ThingsElevation
import com.example.ui.theme.dimens
import kotlinx.coroutines.launch
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.activity.compose.BackHandler
import com.example.ui.theme.ThingsAlpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsMotion
import androidx.compose.ui.res.pluralStringResource
import com.example.ui.components.ThingsConfirmDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.example.ui.theme.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.ClearFocusOnImeHidden
import com.example.ui.components.IME_FOCUS_CLEAR_DIALOG_SETTLE_MS
import com.example.ui.components.hideSoftKeyboardNow
import com.example.ui.components.hideSoftKeyboardThen
import com.example.ui.components.holdForSoftKeyboardHide
import com.example.ui.components.SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS
import com.example.ui.components.HideTextSelectionHandles
import com.example.ui.components.dragdrop.rememberGenericDragDropState
import com.example.domain.tag.TagTitles

import com.example.R
import com.example.data.model.Tag
import com.example.ui.theme.ThingsTheme
import kotlinx.coroutines.delay

// Сколько ждать, пока новая группа тега сохранится в базе
private const val PARENT_SYNC_WAIT_MS = 2000L

// Цвета диалога — роли темы (ThingsTheme)
private val DialogBackgroundColor @Composable get() = ThingsTheme.colors.overlaySurface
private val ItemMutedColor @Composable get() = ThingsTheme.colors.overlayContentSecondary
private val DarkButtonBgColor @Composable get() = ThingsTheme.colors.overlayControl
private val DeleteButtonBgColor @Composable get() = ThingsTheme.colors.danger

// Private Dimensions
// Размеры — шкалы и AppDimens темы; своё у окна только смещение вложенного тега
private val InnerContentPadding = ThingsSpacing.L
private val RowPaddingStartNormal = ThingsSpacing.XS
/** Вложенный тег сдвинут под название родителя: отступ строки + значок + промежуток */
private val RowPaddingStartChild = 28.dp
private val RowPaddingEnd = ThingsSpacing.XS
private val RowPaddingTop = ThingsSpacing.S
private val RowPaddingBottom = ThingsSpacing.S
private val ButtonIconSize = ThingsIconSize.XS
private val EditButtonSize @Composable get() = MaterialTheme.dimens.dialogRowButtonSize
private val DeleteButtonSize @Composable get() = MaterialTheme.dimens.dialogRowButtonSize
/** Круглые кнопки шапки (закрыть, сохранить, назад) */
private val HeaderButtonSize @Composable get() = MaterialTheme.dimens.dialogHeaderButtonSize
/** Строка тега в списке — сверху и снизу */
private val TagRowVerticalPadding = ThingsSpacing.S_PLUS
/** Кнопки «Управлять тегами» / «Новый тег» внизу: промежуток и вертикальный отступ */
private val TagDialogButtonsGap = ThingsSpacing.S_PLUS
private val TagDialogButtonVerticalPadding = ThingsSpacing.S_PLUS

/**
 * Screen states for the ThingsTagDialog options.
 */
enum class DialogScreen {
    LIST,
    CREATE,
    SELECT_GROUP,
    MANAGE,
    EDIT
}

/**
 * A dialog that allows selecting, managing, and adding tags to the current task.
 * Matches the elegant dark theme, features full smooth slide transitions from the
 * bottom for screen transitions (New Tag, Save, Cancel, Select Group), and perfectly
 * mimics the provided visual screenshot design.
 *
 * @param activeTags Currently active tags for the task.
 * @param allSavedTags List of all currently existing tags.
 * @param onNewTagCreated Callback when a new tag has been created.
 * @param onTagsSelected Callback triggered with the newly selected list of tags.
 * @param onDismissRequest Dismiss handler.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThingsTagDialog(
    activeTags: List<String>,
    allSavedTags: List<String> = emptyList(),
    allSavedTagObjects: List<Tag> = emptyList(),
    onNewTagCreated: (String, String?) -> Unit = { _, _ -> },
    onDeleteTag: (Tag) -> Unit = {},
    onUpdateTag: (Tag) -> Unit = {},
    onUpdateTagsOrder: (List<Tag>) -> Unit = {},
    onTagsSelected: (List<String>) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Правки тегов, ещё не дошедшие до базы: запись откладывается до скрытия клавиатуры,
    // а список показывает новое название сразу
    var tagOverrides by remember { mutableStateOf(emptyMap<String, Tag>()) }
    val savedTags = remember(allSavedTagObjects, tagOverrides) {
        allSavedTagObjects.map { tagOverrides[it.id] ?: it }
    }
    val savedTagIds = remember(savedTags) { savedTags.map { it.id }.toSet() }

    // Теги справочника по названию — без учёта регистра, как их сопоставляет база
    val dbTagsByKey = remember(savedTags) {
        savedTags.associateBy { TagTitles.key(it.title) }
    }

    var deletedTags by remember { mutableStateOf(emptySet<String>()) }

    // Combined available tags list (transformed into Tag objects)
    val availableTagObjects = remember(savedTags, activeTags, deletedTags) {
        val activeTagObjects = activeTags
            .filter { it !in deletedTags }
            .map { title ->
                dbTagsByKey[TagTitles.key(title)] ?: Tag(id = title, title = title, parentId = null)
            }
        (savedTags + activeTagObjects)
            .filter { it.title !in deletedTags }
            .distinctBy { TagTitles.key(it.title) }
    }

    /** Тег справочника с таким названием (без учёта регистра), кроме [exceptId] и удалённых в диалоге. */
    fun existingTag(title: String, exceptId: String? = null): Tag? =
        TagTitles.find(savedTags.filter { it.title !in deletedTags }, title, exceptId)

    // Build the flat list of tuples (Tag, isChild: Boolean)
    val flatTagList = remember(availableTagObjects) {
        val result = mutableListOf<Pair<Tag, Boolean>>()
        val roots = availableTagObjects.filter { it.parentId == null }
        val childrenByParent = availableTagObjects.filter { it.parentId != null }.groupBy { it.parentId }
        
        for (root in roots) {
            result.add(Pair(root, false))
            val children = childrenByParent[root.id] ?: emptyList()
            for (child in children) {
                result.add(Pair(child, true))
            }
        }
        
        // Add any remaining children whose parent is missing from the list just in case
        val addedIds = result.map { it.first.id }.toSet()
        val remainingChildren = availableTagObjects.filter { it.parentId != null && !addedIds.contains(it.id) }
        for (child in remainingChildren) {
            result.add(Pair(child, true))
        }
        
        result
    }

    // Currently selected tags
    var selectedTags by remember(activeTags) {
        mutableStateOf(activeTags.toSet())
    }

    var tagToDeleteWithChildren by remember { mutableStateOf<Tag?>(null) }
    var childTagsToDelete by remember { mutableStateOf<List<Tag>>(emptyList()) }

    var currentScreen by remember { mutableStateOf(DialogScreen.LIST) }
    var selectedGroup: Tag? by remember { mutableStateOf<Tag?>(null) }
    var newTagName by remember { mutableStateOf("") }
    var editTagName by remember { mutableStateOf("") }
    var editingTag: Tag? by remember { mutableStateOf<Tag?>(null) }
    var createScreenReturnTarget by remember { mutableStateOf(DialogScreen.LIST) }
    var groupSelectionTargetScreen by remember { mutableStateOf(DialogScreen.CREATE) }
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()
    // Список выбора тегов и тег, к которому его прокрутить, когда тот появится, — только что созданный
    val tagListState = rememberLazyListState()
    var revealTagTitle by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(flatTagList, revealTagTitle) {
        val key = revealTagTitle?.let(TagTitles::key) ?: return@LaunchedEffect
        val index = flatTagList.indexOfFirst { TagTitles.key(it.first.title) == key }
        if (index >= 0) {
            tagListState.animateScrollToItem(index)
            revealTagTitle = null
        }
    }
    // Перетаскивание — общим движком списков (как задачи, проекты и области), правила перестановки — в TagReorder
    val dragDropState = rememberGenericDragDropState(lazyListState)
    var localManageTags by remember { mutableStateOf<List<TagRow>>(flatTagList) }
    // Группы тегов, перенесённых перетаскиванием, пока база их не сохранила: до этого список
    // не перечитывается из базы, иначе строка на мгновение вернулась бы в прежнюю группу
    var awaitingParents by remember { mutableStateOf<Map<String, String?>?>(null) }

    LaunchedEffect(awaitingParents) {
        if (awaitingParents != null) {
            delay(PARENT_SYNC_WAIT_MS)
            awaitingParents = null
        }
    }

    // Список из базы подхватывается, только когда ничего не перетаскивается и строка уже встала на место
    LaunchedEffect(flatTagList, awaitingParents, dragDropState.draggedItemKey) {
        if (dragDropState.draggedItemKey != null) return@LaunchedEffect
        val flatParents = flatTagList.associate { it.first.id to it.first.parentId }
        awaitingParents?.let { awaited ->
            if (awaited.any { (id, parentId) -> id in flatParents && flatParents[id] != parentId }) return@LaunchedEffect
            awaitingParents = null
        }
        val localIds = localManageTags.map { it.first.id }.toSet()
        val flatIds = flatParents.keys
        val parentIdsChanged = localManageTags.any { localPair ->
            val id = localPair.first.id
            id in flatParents && flatParents[id] != localPair.first.parentId
        }
        if (localIds != flatIds || parentIdsChanged) {
            localManageTags = flatTagList
        } else {
            localManageTags = localManageTags.map { localPair ->
                flatTagList.find { it.first.id == localPair.first.id } ?: localPair
            }
        }
    }

    // Пока группу несут под пальцем, её теги убраны из списка — как проекты свёрнутой области
    // на главном экране; после отпускания они снова встают под ней
    val draggedGroupId = (dragDropState.draggedItemKey as? String)
        ?.takeIf { key -> localManageTags.any { it.first.id == key && !it.second } }
    val displayedManageTags = remember(localManageTags, draggedGroupId) {
        TagReorder.withoutChildrenOf(localManageTags, draggedGroupId)
    }

    /** Отпускание: порядок и группы тегов сохраняются в базу. */
    fun saveManageOrder() {
        val initialParents = flatTagList.associate { it.first.id to it.first.parentId }
        val movedParents = localManageTags
            .filter { (tag, _) -> tag.id in initialParents && initialParents[tag.id] != tag.parentId }
            .associate { it.first.id to it.first.parentId }
        if (movedParents.isNotEmpty()) awaitingParents = movedParents
        onUpdateTagsOrder(localManageTags.map { it.first }.filter { it.id in savedTagIds })
    }

    // Focus immediately when switching to creation or edit mode
    LaunchedEffect(currentScreen) {
        if (currentScreen == DialogScreen.CREATE || currentScreen == DialogScreen.EDIT) {
            delay(ThingsMotion.FAST.toLong()) // Allow slide animation to prepare
            focusRequester.requestFocus()
        }
    }

    if (tagToDeleteWithChildren != null) {
        ThingsConfirmDialog(
            title = stringResource(id = R.string.delete_group_title),
            message = stringResource(id = R.string.delete_group_message),
            confirmText = stringResource(id = R.string.tag_dialog_delete),
            dismissText = stringResource(id = R.string.tag_dialog_cancel),
            onConfirm = {
                val parent = tagToDeleteWithChildren!!
                val children = childTagsToDelete

                // TaskRepository will handle deleting children from DB and items
                onDeleteTag(parent)

                var newlyDeleted = setOf(parent.title)
                children.forEach { child ->
                    newlyDeleted = newlyDeleted + child.title
                }

                deletedTags = deletedTags + newlyDeleted
                selectedTags = selectedTags - newlyDeleted

                tagToDeleteWithChildren = null
            },
            onDismiss = { tagToDeleteWithChildren = null }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        // Окно во весь экран, без системных отступов: карточка встаёт ровно по центру экрана,
        // а затемнение накрывает и полосу состояния. Нажатие мимо карточки ловит сам диалог
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        // Появление, закрытие и затемнение рисуем сами: системная анимация окна только гасит его,
        // а эталон растёт при открытии и сжимается при закрытии. Затемнение — той же силы, что у системы
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        val dimAmount = remember(dialogWindow) {
            val amount = dialogWindow?.attributes?.dimAmount ?: 0f
            dialogWindow?.setWindowAnimations(R.style.DialogWindowAnimationNone)
            dialogWindow?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // Окно — на весь экран. Без этого система ставит его между полосой состояния и навигации: карточка уезжает
            // ниже центра экрана, а полоса состояния остаётся незатемнённой
            dialogWindow?.let { window ->
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                // и под вырезом камеры в полосе состояния
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    window.attributes = window.attributes.apply {
                        fitInsetsTypes = 0
                        layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            amount
        }
        val appear = remember { Animatable(0f) }
        var closing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            appear.animateTo(1f, tween(ThingsMotion.QUICK, easing = FastOutSlowInEasing))
        }

        /** Закрывает окно: карточка сжимается и гаснет вместе с затемнением, затем окно убирается. */
        fun closeWindow() {
            if (closing) return
            closing = true
            scope.launch {
                appear.animateTo(0f, tween(ThingsMotion.FAST, easing = LinearEasing))
                onDismissRequest()
            }
        }

        // У диалога своё окно — со своими отступами клавиатуры и своим фокусом,
        // поэтому снятие курсора при сворачивании клавиатуры подключается здесь отдельно — с паузой
        // на хвост штатной анимации скрытия (управляемую окну диалога система не даёт)
        ClearFocusOnImeHidden(settleMillis = IME_FOCUS_CLEAR_DIALOG_SETTLE_MS)
        // У диалога своё окно, и клавиатура сейчас принадлежит ему — прячем её через его View
        val dialogView = LocalView.current
        // Запасной путь для ухода с экрана ввода не через кнопки ниже. Кнопки прячут клавиатуру сами,
        // прямо в обработчике нажатия: эффект сработал бы только на следующем кадре
        LaunchedEffect(currentScreen) {
            if (currentScreen != DialogScreen.CREATE && currentScreen != DialogScreen.EDIT) {
                dialogView.hideSoftKeyboardNow()
            }
        }

        // Уход с экранов ввода. Клавиатура прячется в том же кадре, что и нажатие, экран уезжает вместе
        // с ней, а запись в базу идёт после её скрытия: переименование переписывает задачи с этим тегом,
        // и пересборка списков под диалогом сбивала бы кадры анимации клавиатуры
        // Поля не очищаем: экран ещё виден, пока уезжает. Их сбрасывает открытие экрана
        fun closeCreateScreen() {
            dialogView.hideSoftKeyboardNow()
            currentScreen = createScreenReturnTarget
        }

        fun saveNewTag() {
            val name = newTagName.trim()
            if (name.isEmpty()) return
            // Такой тег уже есть — отмечаем его, а не заводим второй с тем же названием
            val existing = existingTag(name)
            val title = existing?.title ?: name
            if (existing == null) {
                val parentId = selectedGroup?.id
                dialogView.hideSoftKeyboardThen { onNewTagCreated(title, parentId) }
            }
            selectedTags = selectedTags + title
            deletedTags = deletedTags - title
            revealTagTitle = title
            closeCreateScreen()
        }

        fun closeEditScreen() {
            dialogView.hideSoftKeyboardNow()
            currentScreen = DialogScreen.MANAGE
            editingTag = null
        }

        // «Назад» — на уровень вверх, как крестик или стрелка экрана; окно закрывает только со списка
        BackHandler {
            when (currentScreen) {
                DialogScreen.CREATE -> closeCreateScreen()
                DialogScreen.EDIT -> closeEditScreen()
                DialogScreen.SELECT_GROUP -> currentScreen = groupSelectionTargetScreen
                DialogScreen.MANAGE -> currentScreen = DialogScreen.LIST
                DialogScreen.LIST -> closeWindow()
            }
        }

        fun saveEditedTag() {
            val name = editTagName.trim()
            val tagToEdit = editingTag
            if (name.isEmpty() || tagToEdit == null) return
            // Переименование в занятое название сливает тег с существующим (см. репозиторий)
            val clash = existingTag(name, exceptId = tagToEdit.id)
            val newTitle = clash?.title ?: name
            val updated = tagToEdit.copy(
                title = newTitle,
                parentId = selectedGroup?.id?.takeIf { it != tagToEdit.id }
            )
            val oldKey = TagTitles.key(tagToEdit.title)
            if (selectedTags.any { TagTitles.key(it) == oldKey }) {
                selectedTags = selectedTags.filterNot { TagTitles.key(it) == oldKey }.toSet() + newTitle
            }
            if (clash == null) tagOverrides = tagOverrides + (tagToEdit.id to updated)
            if (tagToEdit.title != newTitle) deletedTags = (deletedTags + tagToEdit.title) - newTitle
            dialogView.hideSoftKeyboardThen { onUpdateTag(updated) }
            closeEditScreen()
        }
        val scrimColor = ThingsTheme.colors.scrim
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(scrimColor, alpha = dimAmount * appear.value) }
                .pointerInput(Unit) {
                    // Окно диалога уходит вместе с клавиатурой — сначала дожидаемся её скрытия
                    detectTapGestures(onTap = { dialogView.hideSoftKeyboardThen { closeWindow() } })
                },
            contentAlignment = Alignment.Center
        ) {
            val dialogHeight = maxHeight * MaterialTheme.dimens.dialogHeightFraction
            Card(
                shape = ThingsTheme.shapes.dialogShape,
                colors = CardDefaults.cardColors(
                    containerColor = DialogBackgroundColor
                ),
                modifier = Modifier
                    .graphicsLayer {
                        val progress = appear.value
                        val fromScale = if (closing) ThingsMotion.DIALOG_EXIT_SCALE else ThingsMotion.DIALOG_ENTER_SCALE
                        val scale = fromScale + (1f - fromScale) * progress
                        scaleX = scale
                        scaleY = scale
                        alpha = progress
                    }
                    .width(maxWidth * MaterialTheme.dimens.dialogWidthFraction)
                    .wrapContentHeight()
                    .pointerInput(Unit) { detectTapGestures() }
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dialogHeight)
            ) {
                // ----------------------------------------------------
                // SCREEN 1: Tags List Screen
                // ----------------------------------------------------
                // Пока поверх выезжает «Новый тег», открытый из списка, или «Управление тегами»,
                // список остаётся на месте, как в эталоне
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentScreen == DialogScreen.LIST || currentScreen == DialogScreen.MANAGE ||
                        (currentScreen == DialogScreen.CREATE && createScreenReturnTarget == DialogScreen.LIST),
                    enter = fadeIn(animationSpec = tween(ThingsMotion.QUICK)),
                    exit = fadeOut(animationSpec = tween(ThingsMotion.QUICK))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.L)
                    ) {
                        // Header
                        val hasChanges = selectedTags != activeTags.toSet()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(HeaderButtonSize))

                            Text(
                                text = stringResource(id = R.string.tag_dialog_title),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(HeaderButtonSize)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (hasChanges) ThingsTheme.colors.accent else DarkButtonBgColor)
                                    .clickable {
                                        if (hasChanges) {
                                            onTagsSelected(selectedTags.toList())
                                        }
                                        closeWindow()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasChanges) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = stringResource(if (hasChanges) R.string.cd_save else R.string.cancel),
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }
                        }

                        // Tags List
                        LazyColumn(
                            state = tagListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XS)
                        ) {
                            items(flatTagList, key = { it.first.id }) { item ->
                                val tag = item.first
                                val isChild = item.second
                                val isSelected = selectedTags.contains(tag.title)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .clip(ThingsTheme.shapes.rowShape)
                                        .clickable {
                                            selectedTags = if (isSelected) {
                                                selectedTags - tag.title
                                            } else {
                                                selectedTags + tag.title
                                            }
                                        }
                                        .padding(
                                            start = if (isChild) RowPaddingStartChild else RowPaddingStartNormal,
                                            end = RowPaddingEnd,
                                            top = TagRowVerticalPadding,
                                            bottom = TagRowVerticalPadding
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) AppIcons.TagFilled else AppIcons.Tag,
                                        contentDescription = null,
                                        tint = if (isSelected) ThingsTheme.colors.accent else ItemMutedColor,
                                        modifier = Modifier.size(ThingsIconSize.S)
                                    )

                                    Spacer(modifier = Modifier.width(ThingsSpacing.M))

                                    Text(
                                        text = tag.title,
                                        style = (if (isSelected) ThingsTheme.type.dialogRowSelected else ThingsTheme.type.dialogRow).copy(color = ThingsTheme.colors.overlayContent),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.cd_selected),
                                            tint = ThingsTheme.colors.accent,
                                            modifier = Modifier.size(ThingsIconSize.S)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = ThingsSpacing.XS),
                            horizontalArrangement = Arrangement.spacedBy(TagDialogButtonsGap)
                        ) {
                            Button(
                                onClick = { currentScreen = DialogScreen.MANAGE },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkButtonBgColor,
                                    contentColor = ThingsTheme.colors.overlayContent
                                ),
                                shape = ThingsTheme.shapes.rowShape,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = TagDialogButtonVerticalPadding)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tag_dialog_manage_tags),
                                    style = ThingsTheme.type.dialogButton
                                )
                            }

                            Button(
                                onClick = {
                                    createScreenReturnTarget = DialogScreen.LIST
                                    newTagName = ""
                                    selectedGroup = null
                                    currentScreen = DialogScreen.CREATE
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkButtonBgColor,
                                    contentColor = ThingsTheme.colors.overlayContent
                                ),
                                shape = ThingsTheme.shapes.rowShape,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = TagDialogButtonVerticalPadding)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tag_dialog_new_tag),
                                    style = ThingsTheme.type.dialogButton
                                )
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // SCREEN 2: New Tag Screen (Matches target design)
                // ----------------------------------------------------
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentScreen == DialogScreen.CREATE,
                    // Непрозрачная панель выезжает снизу поверх списка и уезжает обратно, не угасая
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(ThingsMotion.MEDIUM)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ThingsMotion.MEDIUM)
                    ) +
                        // Поле держится, пока клавиатура не скрыта полностью: в окне диалога она уходит штатной
                        // анимацией, дольше, чем в окне приложения (см. SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS)
                        holdForSoftKeyboardHide(SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DialogBackgroundColor)
                            // Список под панелью не должен ловить нажатия
                            .pointerInput(Unit) { detectTapGestures() }
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.L)
                    ) {
                        // Header
                        val isSaveEnabled = newTagName.trim().isNotEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(HeaderButtonSize)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(DarkButtonBgColor)
                                    .clickable { closeCreateScreen() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.tag_dialog_cancel),
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_new_tag),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(HeaderButtonSize)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (isSaveEnabled) ThingsTheme.colors.accent else DarkButtonBgColor)
                                    .clickable(enabled = isSaveEnabled) { saveNewTag() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.tag_dialog_save),
                                    tint = if (isSaveEnabled) ThingsTheme.colors.overlayContent else ItemMutedColor,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }
                        }

                        TagNameField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            onDone = { saveNewTag() },
                            leaving = currentScreen != DialogScreen.CREATE,
                            focusRequester = focusRequester
                        )

                        // "Group" Action Picker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ThingsTheme.shapes.rowShape)
                                .clickable {
                                    dialogView.hideSoftKeyboardNow()
                                    groupSelectionTargetScreen = DialogScreen.CREATE
                                    currentScreen = DialogScreen.SELECT_GROUP
                                }
                                .padding(vertical = ThingsSpacing.M, horizontal = ThingsSpacing.XS),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.tag_dialog_group),
                                style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.XS)
                            ) {
                                Text(
                                    text = selectedGroup?.title ?: stringResource(id = R.string.tag_dialog_no_tag),
                                    style = ThingsTheme.type.dialogRowAction.copy(color = ThingsTheme.colors.accent)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ThingsTheme.colors.accent,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // ----------------------------------------------------
                // SCREEN 3: Select Group Screen
                // ----------------------------------------------------
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentScreen == DialogScreen.SELECT_GROUP,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(ThingsMotion.STANDARD)
                    ) + fadeIn(animationSpec = tween(ThingsMotion.QUICK)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ThingsMotion.STANDARD)
                    ) + fadeOut(animationSpec = tween(ThingsMotion.QUICK))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DialogBackgroundColor)
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.L)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(HeaderButtonSize)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(DarkButtonBgColor)
                                    .clickable {
                                        currentScreen = groupSelectionTargetScreen
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_select_group),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(modifier = Modifier.size(HeaderButtonSize))
                        }

                        // Presaved groups list (all top-level tags + Sentinel)
                        // Группой может быть корневой тег справочника, но не сам редактируемый тег
                        val groupOptions = remember(savedTags, deletedTags, editingTag, groupSelectionTargetScreen) {
                            val excludedId = editingTag?.id?.takeIf { groupSelectionTargetScreen == DialogScreen.EDIT }
                            listOf(Tag(id = "No Tag", title = "No Tag", parentId = null)) +
                                savedTags.filter { it.parentId == null && it.id != excludedId && it.title !in deletedTags }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XS)
                        ) {
                            items(groupOptions, key = { it.id }) { option ->
                                val isSelected = if (option.id == "No Tag") selectedGroup == null else selectedGroup?.id == option.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ThingsTheme.shapes.rowShape)
                                        .clickable {
                                            selectedGroup = if (option.id == "No Tag") null else option
                                            currentScreen = groupSelectionTargetScreen
                                        }
                                        .padding(vertical = ThingsSpacing.M, horizontal = ThingsSpacing.S),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (option.id == "No Tag") stringResource(id = R.string.tag_dialog_no_tag) else option.title,
                                        style = (if (isSelected) ThingsTheme.type.dialogRowSelected else ThingsTheme.type.dialogRow).copy(color = ThingsTheme.colors.overlayContent),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.cd_selected),
                                            tint = ThingsTheme.colors.accent,
                                            modifier = Modifier.size(ThingsIconSize.S)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // SCREEN 4: Manage Tags Screen
                // ----------------------------------------------------
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentScreen == DialogScreen.MANAGE,
                    // Как «Новый тег»: непрозрачная панель выезжает снизу поверх списка и съезжает обратно, не угасая
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(ThingsMotion.MEDIUM)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ThingsMotion.MEDIUM)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DialogBackgroundColor)
                            // Список под панелью не должен ловить нажатия
                            .pointerInput(Unit) { detectTapGestures() }
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.L)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(HeaderButtonSize))

                            Text(
                                text = stringResource(id = R.string.tag_dialog_manage_tags),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(HeaderButtonSize)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(DarkButtonBgColor)
                                    .clickable {
                                        currentScreen = DialogScreen.LIST
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.tag_dialog_done),
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }
                        }



                        CompositionLocalProvider(
                            LocalOverscrollConfiguration provides null
                        ) {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XS)
                            ) {
                                items(displayedManageTags, key = { it.first.id }) { item ->
                                    val tag = item.first
                                    val isChild = item.second

                                    val isDragged = dragDropState.draggedItemKey == tag.id
                                    // Поднятая строка чуть крупнее и с тенью, как проекты и области на главном экране;
                                    // опускается, как только палец отпущен, — вместе с возвратом на место
                                    val lift by animateFloatAsState(
                                        targetValue = if (isDragged && dragDropState.isInteracting) 1f else 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "tagLift"
                                    )
                                    val isLifted = isDragged || lift > 0f
                                    // Фон непрозрачный: поднятая строка проходит над соседними, и они не должны просвечивать
                                    val rowBg = if (isLifted) DarkButtonBgColor else Color.Transparent
                                    val liftShape = ThingsTheme.shapes.rowShape

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .zIndex(if (isLifted) 1f else 0f)
                                            .then(if (!isLifted) Modifier.animateItem() else Modifier)
                                            .graphicsLayer {
                                                translationY = if (isDragged) dragDropState.visualDragOffsetY(tag.id) else 0f
                                                val scale = 1f + 0.03f * lift
                                                scaleX = scale
                                                scaleY = scale
                                                shadowElevation = ThingsElevation.CARD.toPx() * lift
                                                shape = liftShape
                                                clip = false
                                            }
                                            .tagDragAndDrop(
                                                state = dragDropState,
                                                row = item,
                                                rows = localManageTags,
                                                canBeGroup = { it in savedTagIds },
                                                onRowsChange = { localManageTags = it },
                                                onDragEnd = { saveManageOrder() }
                                            )
                                            .background(rowBg, ThingsTheme.shapes.rowShape)
                                            .padding(
                                                start = if (isChild) RowPaddingStartChild else RowPaddingStartNormal,
                                                end = RowPaddingEnd,
                                                top = RowPaddingTop,
                                                bottom = RowPaddingBottom
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Delete button on the left (Red circle with white trash icon)
                                        Box(
                                            modifier = Modifier
                                                .size(DeleteButtonSize)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(DeleteButtonBgColor)
                                                .clickable {
                                                    val children = availableTagObjects.filter { it.parentId == tag.id }
                                                    if (children.isNotEmpty()) {
                                                        tagToDeleteWithChildren = tag
                                                        childTagsToDelete = children
                                                    } else {
                                                        onDeleteTag(tag)
                                                        deletedTags = deletedTags + tag.title
                                                        if (selectedTags.contains(tag.title)) {
                                                            selectedTags = selectedTags - tag.title
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.tag_dialog_delete),
                                                tint = ThingsTheme.colors.overlayContent,
                                                modifier = Modifier.size(ButtonIconSize)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(ThingsSpacing.M))

                                        // Tag Title
                                        Text(
                                            text = tag.title,
                                            style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent),
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Blue edit pencil button on the right
                                        Box(
                                            modifier = Modifier
                                                .size(EditButtonSize)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(ThingsTheme.colors.accent)
                                                .clickable {
                                                    editingTag = tag
                                                    editTagName = tag.title
                                                    selectedGroup = savedTags.firstOrNull { it.id == tag.parentId }
                                                    currentScreen = DialogScreen.EDIT
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.cd_edit),
                                                tint = ThingsTheme.colors.overlayContent,
                                                modifier = Modifier.size(ButtonIconSize)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions: New Tag
                        Button(
                            onClick = {
                                createScreenReturnTarget = DialogScreen.MANAGE
                                newTagName = ""
                                selectedGroup = null
                                currentScreen = DialogScreen.CREATE
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkButtonBgColor,
                                contentColor = ThingsTheme.colors.overlayContent
                            ),
                            shape = ThingsTheme.shapes.rowShape,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = TagDialogButtonVerticalPadding)
                        ) {
                            Text(
                                text = stringResource(id = R.string.tag_dialog_new_tag),
                                style = ThingsTheme.type.dialogButton
                            )
                        }
                    }
                }

                // ----------------------------------------------------
                // SCREEN 5: Edit Tag Screen
                // ----------------------------------------------------
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentScreen == DialogScreen.EDIT,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(ThingsMotion.STANDARD)
                    ) + fadeIn(animationSpec = tween(ThingsMotion.QUICK)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ThingsMotion.STANDARD)
                    ) + fadeOut(animationSpec = tween(ThingsMotion.QUICK)) +
                        // Поле держится, пока клавиатура не скрыта полностью: в окне диалога она уходит штатной
                        // анимацией, дольше, чем в окне приложения (см. SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS)
                        holdForSoftKeyboardHide(SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DialogBackgroundColor)
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.L)
                    ) {
                        // Header
                        val isSaveEnabled = editTagName.trim().isNotEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(HeaderButtonSize)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(DarkButtonBgColor)
                                    .clickable { closeEditScreen() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.tag_dialog_cancel),
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_edit_tag),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(HeaderButtonSize)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (isSaveEnabled) ThingsTheme.colors.accent else DarkButtonBgColor)
                                    .clickable(enabled = isSaveEnabled) { saveEditedTag() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.tag_dialog_save),
                                    tint = if (isSaveEnabled) ThingsTheme.colors.overlayContent else ItemMutedColor,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }
                        }

                        TagNameField(
                            value = editTagName,
                            onValueChange = { editTagName = it },
                            onDone = { saveEditedTag() },
                            leaving = currentScreen != DialogScreen.EDIT,
                            focusRequester = focusRequester
                        )

                        // "Group" Action Picker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ThingsTheme.shapes.rowShape)
                                .clickable {
                                    dialogView.hideSoftKeyboardNow()
                                    groupSelectionTargetScreen = DialogScreen.EDIT
                                    currentScreen = DialogScreen.SELECT_GROUP
                                }
                                .padding(vertical = ThingsSpacing.M, horizontal = ThingsSpacing.XS),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.tag_dialog_group),
                                style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.XS)
                            ) {
                                Text(
                                    text = selectedGroup?.title ?: stringResource(id = R.string.tag_dialog_no_tag),
                                    style = ThingsTheme.type.dialogRowAction.copy(color = ThingsTheme.colors.accent)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ThingsTheme.colors.accent,
                                    modifier = Modifier.size(ThingsIconSize.S)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        }
    }
}

/**
 * Поле названия тега на экранах «Новый тег» и «Изменить тег»: залитая плашка без рамки,
 * подсказка, пока пусто, и круглая кнопка очистки, когда есть текст.
 * Пока экран уезжает ([leaving]), фокус остаётся на поле — снять его до скрытия клавиатуры нельзя
 * (см. hideSoftKeyboardNow), поэтому курсор и маркеры прячем, как в редакторе задачи.
 */
@Composable
private fun TagNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    leaving: Boolean,
    focusRequester: FocusRequester
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.dialogFieldHeight)
            .clip(ThingsTheme.shapes.rowShape)
            .background(DarkButtonBgColor)
            .padding(horizontal = ThingsSpacing.M)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.tag_dialog_placeholder),
                    style = ThingsTheme.type.dialogRow.copy(color = ItemMutedColor)
                )
            }
            HideTextSelectionHandles(hidden = leaving) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    cursorBrush = SolidColor(if (leaving) Color.Transparent else ThingsTheme.colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(ThingsSpacing.S))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(MaterialTheme.dimens.clearBadgeSize)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(ItemMutedColor.copy(alpha = ThingsAlpha.HALF))
                    .clickable { onValueChange("") }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_clear),
                    tint = DarkButtonBgColor,
                    modifier = Modifier.size(MaterialTheme.dimens.clearBadgeIconSize)
                )
            }
        }
    }
}
