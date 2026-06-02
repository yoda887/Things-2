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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay

// Private Theme Color Constants inside the file to avoid hardcoding inline
private val DialogBackgroundColor = Color(0xFF22242C)
private val ItemMutedColor = Color(0xFF8E8E93)
private val DarkButtonBgColor = Color(0xFF2C2E38)

// Private Dimensions
private val DialogWidth = 320.dp
private val DialogHeight = 380.dp
private val RoundedCornerSize = 16.dp
private val InnerContentPadding = 16.dp

/**
 * Screen states for the ThingsTagDialog options.
 */
enum class DialogScreen {
    LIST,
    CREATE,
    SELECT_GROUP
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

    // Combined available tags list (transformed into Tag objects)
    val availableTagObjects = remember(allSavedTagObjects, activeTags) {
        val activeTagObjects = activeTags.map { title ->
            dbTagsByTitle[title] ?: Tag(id = title, title = title, parentId = null)
        }
        val defaultTagObjects = defaultTags.map { title ->
            dbTagsByTitle[title] ?: Tag(id = title, title = title, parentId = null)
        }
        (defaultTagObjects + allSavedTagObjects + activeTagObjects).distinctBy { it.title }
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

    var currentScreen by remember { mutableStateOf(DialogScreen.LIST) }
    var selectedGroup: Tag? by remember { mutableStateOf<Tag?>(null) }
    var newTagName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Focus immediately when switching to creation mode
    LaunchedEffect(currentScreen) {
        if (currentScreen == DialogScreen.CREATE) {
            delay(100L) // Allow slide animation to prepare
            focusRequester.requestFocus()
        }
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
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 16.sp,
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
                                onClick = { /* Opens Tag Manager */ },
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
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = { currentScreen = DialogScreen.CREATE },
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
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
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
                                        currentScreen = DialogScreen.LIST
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
                                        }
                                        currentScreen = DialogScreen.LIST
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
                                        currentScreen = DialogScreen.CREATE
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
                                            currentScreen = DialogScreen.CREATE
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (option.id == "No Tag") stringResource(id = R.string.tag_dialog_no_tag) else option.title,
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 16.sp,
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
            }
        }
    }
}
