package com.example.ui.screens.home.inlineeditor.dialogs

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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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

// Цвета диалога — роли темы (ThingsTheme)
private val DialogBackgroundColor @Composable get() = ThingsTheme.colors.overlaySurface
private val ItemMutedColor @Composable get() = ThingsTheme.colors.overlayContentSecondary
private val DarkButtonBgColor @Composable get() = ThingsTheme.colors.overlayControl
private val DeleteButtonBgColor @Composable get() = ThingsTheme.colors.danger

// Private Dimensions
private val DialogWidth = 320.dp
private val DialogHeight = 480.dp
private val InnerContentPadding = 16.dp
private val RowPaddingStartNormal = 4.dp
private val RowPaddingStartChild = 28.dp
private val RowPaddingEnd = 4.dp
private val RowPaddingTop = 8.dp
private val RowPaddingBottom = 8.dp
private val ButtonIconSize = 16.dp
private val EditButtonSize = 28.dp
private val DeleteButtonSize = 28.dp

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
    // Перетаскивание — общим движком списков (как задачи, проекты и области), правила перестановки — в TagReorder
    val dragDropState = rememberGenericDragDropState(lazyListState)
    var localManageTags by remember { mutableStateOf<List<TagRow>>(flatTagList) }
    // Группы тегов, перенесённых перетаскиванием, пока база их не сохранила: до этого список
    // не перечитывается из базы, иначе строка на мгновение вернулась бы в прежнюю группу
    var awaitingParents by remember { mutableStateOf<Map<String, String?>?>(null) }

    LaunchedEffect(awaitingParents) {
        if (awaitingParents != null) {
            delay(2000L)
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
            delay(100L) // Allow slide animation to prepare
            focusRequester.requestFocus()
        }
    }

    if (tagToDeleteWithChildren != null) {
        AlertDialog(
            onDismissRequest = { tagToDeleteWithChildren = null },
            title = { Text(stringResource(id = R.string.delete_group_title)) },
            text = { Text(stringResource(id = R.string.delete_group_message)) },
            confirmButton = {
                TextButton(onClick = {
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
                }) {
                    Text(
                        text = stringResource(id = R.string.tag_dialog_delete),
                        color = ThingsTheme.colors.danger,
                        style = ThingsTheme.type.dialogButton
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDeleteWithChildren = null }) {
                    Text(
                        text = stringResource(id = R.string.tag_dialog_cancel),
                        style = ThingsTheme.type.dialogButton
                    )
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
            closeCreateScreen()
        }

        fun closeEditScreen() {
            dialogView.hideSoftKeyboardNow()
            currentScreen = DialogScreen.MANAGE
            editingTag = null
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Окно диалога исчезает сразу — сначала дожидаемся скрытия клавиатуры
                    detectTapGestures(onTap = { dialogView.hideSoftKeyboardThen(onDismissRequest) })
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = ThingsTheme.shapes.dialogShape,
                colors = CardDefaults.cardColors(
                    containerColor = DialogBackgroundColor
                ),
                modifier = Modifier
                    .width(DialogWidth)
                    .wrapContentHeight()
                    .pointerInput(Unit) { detectTapGestures() }
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DialogHeight)
            ) {
                // ----------------------------------------------------
                // SCREEN 1: Tags List Screen
                // ----------------------------------------------------
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentScreen == DialogScreen.LIST,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        val hasChanges = selectedTags != activeTags.toSet()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(36.dp))

                            Text(
                                text = stringResource(id = R.string.tag_dialog_title),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (hasChanges) ThingsTheme.colors.accent else DarkButtonBgColor)
                                    .clickable {
                                        if (hasChanges) {
                                            onTagsSelected(selectedTags.toList())
                                        }
                                        onDismissRequest()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasChanges) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = if (hasChanges) "Save" else "Cancel",
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Tags List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                            start = if (isChild) 28.dp else 4.dp,
                                            end = 4.dp,
                                            top = 10.dp,
                                            bottom = 10.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) AppIcons.TagFilled else AppIcons.Tag,
                                        contentDescription = null,
                                        tint = if (isSelected) ThingsTheme.colors.accent else ItemMutedColor,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = tag.title,
                                        style = ThingsTheme.type.dialogRow.copy(
                                            color = ThingsTheme.colors.overlayContent,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = ThingsTheme.colors.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { currentScreen = DialogScreen.MANAGE },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkButtonBgColor,
                                    contentColor = ThingsTheme.colors.overlayContent
                                ),
                                shape = ThingsTheme.shapes.rowShape,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
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
                                contentPadding = PaddingValues(vertical = 10.dp)
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
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(150)) +
                        // Поле держится, пока клавиатура не скрыта полностью: в окне диалога она уходит штатной
                        // анимацией, дольше, чем в окне приложения (см. SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS)
                        holdForSoftKeyboardHide(SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DialogBackgroundColor)
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(DarkButtonBgColor)
                                    .clickable { closeCreateScreen() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_new_tag),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (isSaveEnabled) ThingsTheme.colors.accent else DarkButtonBgColor)
                                    .clickable(enabled = isSaveEnabled) { saveNewTag() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = if (isSaveEnabled) ThingsTheme.colors.overlayContent else ItemMutedColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Frameless Tag Input Field with Dark Container.
                        // Пока экран уезжает, фокус остаётся на поле — снять его до скрытия клавиатуры нельзя
                        // (см. hideSoftKeyboardNow), поэтому курсор и маркеры прячем, как в редакторе задачи
                        val leaving = currentScreen != DialogScreen.CREATE
                        HideTextSelectionHandles(hidden = leaving) {
                            OutlinedTextField(
                                value = newTagName,
                                onValueChange = { newTagName = it },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { saveNewTag() }),
                                placeholder = { Text(stringResource(id = R.string.tag_dialog_placeholder), color = ItemMutedColor) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkButtonBgColor,
                                    unfocusedContainerColor = DarkButtonBgColor,
                                    disabledContainerColor = DarkButtonBgColor,
                                    focusedTextColor = ThingsTheme.colors.overlayContent,
                                    unfocusedTextColor = ThingsTheme.colors.overlayContent,
                                    focusedBorderColor = ThingsTheme.colors.accent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = if (leaving) Color.Transparent else ThingsTheme.colors.accent
                                ),
                                shape = ThingsTheme.shapes.rowShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

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
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.tag_dialog_group),
                                style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent, fontWeight = FontWeight.Normal)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedGroup?.title ?: stringResource(id = R.string.tag_dialog_no_tag),
                                    style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.accent, fontWeight = FontWeight.Medium)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ThingsTheme.colors.accent,
                                    modifier = Modifier.size(18.dp)
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
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(150))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DialogBackgroundColor)
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(DarkButtonBgColor)
                                    .clickable {
                                        currentScreen = groupSelectionTargetScreen
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Back",
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_select_group),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(modifier = Modifier.size(36.dp))
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
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (option.id == "No Tag") stringResource(id = R.string.tag_dialog_no_tag) else option.title,
                                        style = ThingsTheme.type.dialogRow.copy(
                                            color = ThingsTheme.colors.overlayContent,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = ThingsTheme.colors.accent,
                                            modifier = Modifier.size(18.dp)
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
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(150))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DialogBackgroundColor)
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(36.dp))

                            Text(
                                text = stringResource(id = R.string.tag_dialog_manage_tags),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(DarkButtonBgColor)
                                    .clickable {
                                        currentScreen = DialogScreen.LIST
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Done",
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(18.dp)
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
                                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                                shadowElevation = 8.dp.toPx() * lift
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
                                                contentDescription = "Delete",
                                                tint = ThingsTheme.colors.overlayContent,
                                                modifier = Modifier.size(ButtonIconSize)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Tag Title
                                        Text(
                                            text = tag.title,
                                            style = ThingsTheme.type.dialogRow.copy(
                                                color = ThingsTheme.colors.overlayContent,
                                                fontWeight = FontWeight.Normal
                                            ),
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
                                                contentDescription = "Edit",
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
                            contentPadding = PaddingValues(vertical = 10.dp)
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
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(150)) +
                        // Поле держится, пока клавиатура не скрыта полностью: в окне диалога она уходит штатной
                        // анимацией, дольше, чем в окне приложения (см. SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS)
                        holdForSoftKeyboardHide(SOFT_KEYBOARD_SYSTEM_HIDE_SETTLE_MS)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DialogBackgroundColor)
                            .padding(InnerContentPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(DarkButtonBgColor)
                                    .clickable { closeEditScreen() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = ThingsTheme.colors.overlayContent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_edit_tag),
                                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (isSaveEnabled) ThingsTheme.colors.accent else DarkButtonBgColor)
                                    .clickable(enabled = isSaveEnabled) { saveEditedTag() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = if (isSaveEnabled) ThingsTheme.colors.overlayContent else ItemMutedColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Frameless Tag Input Field with Dark Container.
                        // Пока экран уезжает, фокус остаётся на поле — снять его до скрытия клавиатуры нельзя
                        // (см. hideSoftKeyboardNow), поэтому курсор и маркеры прячем, как в редакторе задачи
                        val leaving = currentScreen != DialogScreen.EDIT
                        HideTextSelectionHandles(hidden = leaving) {
                            OutlinedTextField(
                                value = editTagName,
                                onValueChange = { editTagName = it },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { saveEditedTag() }),
                                placeholder = { Text(stringResource(id = R.string.tag_dialog_placeholder), color = ItemMutedColor) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkButtonBgColor,
                                    unfocusedContainerColor = DarkButtonBgColor,
                                    disabledContainerColor = DarkButtonBgColor,
                                    focusedTextColor = ThingsTheme.colors.overlayContent,
                                    unfocusedTextColor = ThingsTheme.colors.overlayContent,
                                    focusedBorderColor = ThingsTheme.colors.accent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = if (leaving) Color.Transparent else ThingsTheme.colors.accent
                                ),
                                trailingIcon = {
                                    if (editTagName.isNotEmpty()) {
                                        IconButton(onClick = { editTagName = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = ItemMutedColor
                                            )
                                        }
                                    }
                                },
                                shape = ThingsTheme.shapes.rowShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

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
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.tag_dialog_group),
                                style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent, fontWeight = FontWeight.Normal)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedGroup?.title ?: stringResource(id = R.string.tag_dialog_no_tag),
                                    style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.accent, fontWeight = FontWeight.Medium)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ThingsTheme.colors.accent,
                                    modifier = Modifier.size(18.dp)
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