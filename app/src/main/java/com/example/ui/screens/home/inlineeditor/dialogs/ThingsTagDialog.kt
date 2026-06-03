package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

import com.example.R
import com.example.data.model.Tag
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsUpcomingRed
import kotlinx.coroutines.delay

// Private Theme Color Constants inside the file to avoid hardcoding inline
private val DialogBackgroundColor = Color(0xFF22242C)
private val ItemMutedColor = Color(0xFF8E8E93)
private val DarkButtonBgColor = Color(0xFF2C2E38)
private val DeleteButtonBgColor = ThingsUpcomingRed

// Private Dimensions
private val DialogWidth = 320.dp
private val DialogHeight = 480.dp
private val RoundedCornerSize = 16.dp
private val InnerContentPadding = 16.dp
private val RowPaddingStartNormal = 4.dp
private val RowPaddingStartChild = 28.dp
private val RowPaddingEnd = 4.dp
private val RowPaddingTop = 8.dp
private val RowPaddingBottom = 8.dp
private val ButtonIconSize = 16.dp
private val EditButtonSize = 28.dp
private val DeleteButtonSize = 28.dp
private val TextButtonFontSize = 16.sp

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
    // Standard default tags list
    val defaultTags = remember {
        listOf("Errand", "Home", "Office", "Important", "Pending", "Phone", "Books", "Diane", "Marc")
    }

    // Map database tags by title
    val dbTagsByTitle = remember(allSavedTagObjects) {
        allSavedTagObjects.associateBy { it.title }
    }

    var deletedDefaultTags by remember { mutableStateOf(emptySet<String>()) }
    var deletedTags by remember { mutableStateOf(emptySet<String>()) }

    // Combined available tags list (transformed into Tag objects)
    val availableTagObjects = remember(allSavedTagObjects, activeTags, deletedDefaultTags, deletedTags) {
        val activeTagObjects = activeTags
            .filter { it !in deletedTags }
            .map { title ->
                dbTagsByTitle[title] ?: Tag(id = title, title = title, parentId = null)
            }
        val defaultTagObjects = defaultTags
            .filter { it !in deletedDefaultTags && it !in deletedTags }
            .map { title ->
                dbTagsByTitle[title] ?: Tag(id = title, title = title, parentId = null)
            }
        (defaultTagObjects + allSavedTagObjects + activeTagObjects)
            .filter { it.title !in deletedDefaultTags && it.title !in deletedTags }
            .distinctBy { it.title }
    }

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
    var draggedTagId by remember { mutableStateOf<String?>(null) }
    var dragAccumulatedOffset by remember { mutableStateOf(0f) }
    var localManageTags by remember(flatTagList) { mutableStateOf(flatTagList) }

    LaunchedEffect(flatTagList) {
        localManageTags = flatTagList
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
                    
                    onDeleteTag(parent)
                    var newlyDeleted = setOf(parent.title)
                    
                    children.forEach { child ->
                        onDeleteTag(child)
                        newlyDeleted = newlyDeleted + child.title
                    }
                    
                    deletedTags = deletedTags + newlyDeleted
                    deletedDefaultTags = deletedDefaultTags + newlyDeleted.filter { defaultTags.contains(it) }
                    selectedTags = selectedTags - newlyDeleted
                    
                    tagToDeleteWithChildren = null
                }) {
                    Text(stringResource(id = R.string.tag_dialog_delete), color = ThingsUpcomingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDeleteWithChildren = null }) {
                    Text(stringResource(id = R.string.tag_dialog_cancel))
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        Card(
            shape = RoundedCornerShape(RoundedCornerSize),
            colors = CardDefaults.cardColors(
                containerColor = DialogBackgroundColor
            ),
            modifier = Modifier
                .width(DialogWidth)
                .wrapContentHeight()
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
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (hasChanges) ThingsBlue else DarkButtonBgColor)
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
                                    tint = Color.White,
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
                                        .animateItemPlacement()
                                        .clip(RoundedCornerShape(8.dp))
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
                                        imageVector = if (isSelected) Icons.Filled.LocalOffer else Icons.Outlined.LocalOffer,
                                        contentDescription = null,
                                        tint = if (isSelected) ThingsBlue else ItemMutedColor,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = tag.title,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = ThingsBlue,
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
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tag_dialog_manage_tags),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                                )
                            }

                            Button(
                                onClick = {
                                    createScreenReturnTarget = DialogScreen.LIST
                                    currentScreen = DialogScreen.CREATE
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkButtonBgColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tag_dialog_new_tag),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
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
                                    .clickable {
                                        currentScreen = createScreenReturnTarget
                                        newTagName = ""
                                        selectedGroup = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_new_tag),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (isSaveEnabled) ThingsBlue else DarkButtonBgColor)
                                    .clickable(enabled = isSaveEnabled) {
                                        val name = newTagName.trim()
                                        if (name.isNotEmpty()) {
                                            onNewTagCreated(name, selectedGroup?.id)
                                            selectedTags = selectedTags + name
                                            deletedTags = deletedTags - name
                                            deletedDefaultTags = deletedDefaultTags - name
                                        }
                                        currentScreen = createScreenReturnTarget
                                        newTagName = ""
                                        selectedGroup = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = if (isSaveEnabled) Color.White else ItemMutedColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Frameless Tag Input Field with Dark Container
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            placeholder = { Text(stringResource(id = R.string.tag_dialog_placeholder), color = ItemMutedColor) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkButtonBgColor,
                                unfocusedContainerColor = DarkButtonBgColor,
                                disabledContainerColor = DarkButtonBgColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ThingsBlue,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = ThingsBlue
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )

                        // "Group" Action Picker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    groupSelectionTargetScreen = DialogScreen.CREATE
                                    currentScreen = DialogScreen.SELECT_GROUP
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.tag_dialog_group),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedGroup?.title ?: stringResource(id = R.string.tag_dialog_no_tag),
                                    style = TextStyle(
                                        color = ThingsBlue,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ThingsBlue,
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
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_select_group),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )

                            Box(modifier = Modifier.size(36.dp))
                        }

                        // Presaved groups list (all top-level tags + Sentinel)
                        val groupOptions = remember(allSavedTagObjects) {
                            listOf(Tag(id = "No Tag", title = "No Tag", parentId = null)) + allSavedTagObjects.filter { it.parentId == null }
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedGroup = if (option.id == "No Tag") null else option
                                            currentScreen = groupSelectionTargetScreen
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (option.id == "No Tag") stringResource(id = R.string.tag_dialog_no_tag) else option.title,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = ThingsBlue,
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
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
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
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Drag & Drop Reorderable list modifier
                        val makeDragModifier = { itemPair: Pair<Tag, Boolean> ->
                            val tag = itemPair.first
                            Modifier.pointerInput(tag.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedTagId = tag.id
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
                                            spacing
                                        }
                                        val draggedItemInfo = visibleItems.firstOrNull { it.key == tag.id }
                                        if (draggedItemInfo != null) {
                                            val dragCenterY = draggedItemInfo.offset + draggedItemInfo.size / 2f + dragAccumulatedOffset
                                            val hoveredItem = visibleItems.firstOrNull { item ->
                                                val itemKey = item.key as? String
                                                itemKey != null && itemKey != tag.id &&
                                                dragCenterY > item.offset &&
                                                dragCenterY < item.offset + item.size
                                            }
                                            if (hoveredItem != null) {
                                                val fromIndex = localManageTags.indexOfFirst { it.first.id == tag.id }
                                                val toIndex = localManageTags.indexOfFirst { it.first.id == hoveredItem.key }
                                                if (fromIndex != -1 && toIndex != -1) {
                                                    val newList = localManageTags.toMutableList()
                                                    val movedItem = newList.removeAt(fromIndex)
                                                    newList.add(toIndex, movedItem)
                                                    localManageTags = newList
                                                    
                                                    val distance = if (hoveredItem.offset > draggedItemInfo.offset) {
                                                        hoveredItem.size.toFloat() + detectedSpacing
                                                    } else {
                                                        -(hoveredItem.size.toFloat() + detectedSpacing)
                                                    }
                                                    dragAccumulatedOffset -= distance
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val updatedTags = localManageTags.map { it.first }
                                        onUpdateTagsOrder(updatedTags)
                                        draggedTagId = null
                                        dragAccumulatedOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggedTagId = null
                                        dragAccumulatedOffset = 0f
                                    }
                                )
                            }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(localManageTags, key = { it.first.id }) { item ->
                                val tag = item.first
                                val isChild = item.second
                                
                                val isDragged = draggedTagId == tag.id
                                val dragOffset = if (isDragged) dragAccumulatedOffset else 0f
                                val rowBg = if (isDragged) DarkButtonBgColor.copy(alpha = 0.8f) else Color.Transparent

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = dragOffset
                                        }
                                        .background(rowBg, RoundedCornerShape(8.dp))
                                        .then(makeDragModifier(item))
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
                                                    if (defaultTags.contains(tag.title)) {
                                                        deletedDefaultTags = deletedDefaultTags + tag.title
                                                    }
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
                                            tint = Color.White,
                                            modifier = Modifier.size(ButtonIconSize)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Tag Title
                                    Text(
                                        text = tag.title,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Blue edit pencil button on the right
                                    Box(
                                        modifier = Modifier
                                            .size(EditButtonSize)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(ThingsBlue)
                                            .clickable {
                                                editingTag = tag
                                                editTagName = tag.title
                                                selectedGroup = allSavedTagObjects.firstOrNull { it.id == tag.parentId }
                                                currentScreen = DialogScreen.EDIT
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.White,
                                            modifier = Modifier.size(ButtonIconSize)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Actions: New Tag
                        Button(
                            onClick = {
                                createScreenReturnTarget = DialogScreen.MANAGE
                                currentScreen = DialogScreen.CREATE
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkButtonBgColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.tag_dialog_new_tag),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
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
                                    .clickable {
                                        currentScreen = DialogScreen.MANAGE
                                        editingTag = null
                                        editTagName = ""
                                        selectedGroup = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = stringResource(id = R.string.tag_dialog_edit_tag),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (isSaveEnabled) ThingsBlue else DarkButtonBgColor)
                                    .clickable(enabled = isSaveEnabled) {
                                        val name = editTagName.trim()
                                        val tagToEdit = editingTag
                                        if (name.isNotEmpty() && tagToEdit != null) {
                                            // Update selectedTags if renamed
                                            if (selectedTags.contains(tagToEdit.title)) {
                                                selectedTags = (selectedTags - tagToEdit.title) + name
                                            }

                                            val isDefault = defaultTags.contains(tagToEdit.title) && !allSavedTagObjects.any { it.id == tagToEdit.id }
                                            if (isDefault) {
                                                // Hide the old default tag
                                                deletedDefaultTags = deletedDefaultTags + tagToEdit.title
                                                // Create a new custom tag
                                                onNewTagCreated(name, selectedGroup?.id)
                                                deletedTags = deletedTags - name
                                                deletedDefaultTags = deletedDefaultTags - name
                                            } else {
                                                // Update the existing custom tag
                                                onUpdateTag(tagToEdit.copy(title = name, parentId = selectedGroup?.id))
                                                deletedTags = (deletedTags + tagToEdit.title) - name
                                                deletedDefaultTags = deletedDefaultTags - name
                                            }
                                        }
                                        currentScreen = DialogScreen.MANAGE
                                        editingTag = null
                                        editTagName = ""
                                        selectedGroup = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = if (isSaveEnabled) Color.White else ItemMutedColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Frameless Tag Input Field with Dark Container
                        OutlinedTextField(
                            value = editTagName,
                            onValueChange = { editTagName = it },
                            placeholder = { Text(stringResource(id = R.string.tag_dialog_placeholder), color = ItemMutedColor) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkButtonBgColor,
                                unfocusedContainerColor = DarkButtonBgColor,
                                disabledContainerColor = DarkButtonBgColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ThingsBlue,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = ThingsBlue
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
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )

                        // "Group" Action Picker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    groupSelectionTargetScreen = DialogScreen.EDIT
                                    currentScreen = DialogScreen.SELECT_GROUP
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.tag_dialog_group),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedGroup?.title ?: stringResource(id = R.string.tag_dialog_no_tag),
                                    style = TextStyle(
                                        color = ThingsBlue,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ThingsBlue,
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
