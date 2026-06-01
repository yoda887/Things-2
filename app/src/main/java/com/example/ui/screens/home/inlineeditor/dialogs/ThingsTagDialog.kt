package com.example.ui.screens.home.inlineeditor.dialogs

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ThingsBlue
import kotlinx.coroutines.delay

/**
 * A dialog that allows selecting and adding tags to the current task.
 * Matches the dark theme and layout from the user's design screenshot,
 * now featuring an elegant inline tag creation view with smooth animations.
 *
 * @param activeTags Currently active tags for the task.
 * @param onTagsSelected Callback triggered when user clicks 'Done', passing the updated list of tags.
 * @param onDismissRequest Callback triggered when the dialog is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThingsTagDialog(
    activeTags: List<String>,
    allSavedTags: List<String> = emptyList(),
    onNewTagCreated: (String) -> Unit = {},
    onTagsSelected: (List<String>) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Standard default tags list as shown in the screenshot
    val defaultTags = remember {
        listOf("Errand", "Home", "Office", "Important", "Pending", "Phone", "Books", "Diane", "Marc")
    }

    // Available tags list combines default tags, active tags for the current task, and any other saved tags.
    val availableTags = remember(allSavedTags, activeTags) {
        (defaultTags + allSavedTags + activeTags).distinct()
    }

    // Mutable state of currently selected tags
    var selectedTags by remember(activeTags) {
        mutableStateOf(activeTags.toSet())
    }

    var isCreatingTag by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val titleFontSize = MaterialTheme.typography.headlineSmall.fontSize
    val notesFontSize = MaterialTheme.typography.titleSmall.fontSize

    // Focus immediately when switching to creation mode
    LaunchedEffect(isCreatingTag) {
        if (isCreatingTag) {
            delay(100L) // Wait for the transition to finish so focus request works flawlessly
            focusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF22242C) // Same dark background as WhenDialog
            ),
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row (Equal-weight columns ensuring absolute centering)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isCreatingTag) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Tags",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Done",
                            style = TextStyle(
                                color = ThingsBlue,
                                fontSize = notesFontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onTagsSelected(selectedTags.toList())
                                    onDismissRequest()
                                },
                            textAlign = TextAlign.End
                        )
                    } else {
                        Text(
                            text = "Cancel",
                            style = TextStyle(
                                color = Color(0xFF8E8E93),
                                fontSize = notesFontSize,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isCreatingTag = false
                                    newTagName = ""
                                },
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "New Tag",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        val isSaveEnabled = newTagName.trim().isNotEmpty()
                        Text(
                            text = "Save",
                            style = TextStyle(
                                color = if (isSaveEnabled) ThingsBlue else Color(0xFF4C4D54),
                                fontSize = notesFontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = isSaveEnabled) {
                                    val name = newTagName.trim()
                                    if (name.isNotEmpty()) {
                                        onNewTagCreated(name)
                                        selectedTags = selectedTags + name
                                    }
                                    isCreatingTag = false
                                    newTagName = ""
                                },
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Stable Dialog Container to prevent height shifting
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    Crossfade(
                        targetState = isCreatingTag,
                        animationSpec = tween(durationMillis = 150),
                        label = "DialogContentCrossfade"
                    ) { tagCreateMode ->
                        if (!tagCreateMode) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Scrollable Tags List (Strictly matches original scroll constraints)
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(availableTags, key = { it }) { tag ->
                                        val isSelected = selectedTags.contains(tag)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItemPlacement()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedTags = if (isSelected) {
                                                        selectedTags - tag
                                                    } else {
                                                        selectedTags + tag
                                                    }
                                                }
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Tag Icon (Filled when selected, Outlined when unselected)
                                            Icon(
                                                imageVector = if (isSelected) Icons.Filled.LocalOffer else Icons.Outlined.LocalOffer,
                                                contentDescription = null,
                                                tint = if (isSelected) ThingsBlue else Color(0xFF8E8E93),
                                                modifier = Modifier.size(18.dp)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = tag,
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

                                // Bottom Buttons row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Manage Tags button
                                    Button(
                                        onClick = { /* In standard Things, opens tag manager */ },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2C2E38),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("Manage Tags", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }

                                    // New Tag button
                                    Button(
                                        onClick = { isCreatingTag = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2C2E38),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("New Tag", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Sleek, modern frameless input field sitting flat at the top of the content area
                                OutlinedTextField(
                                    value = newTagName,
                                    onValueChange = { newTagName = it },
                                    placeholder = { Text("Tag Title", color = Color(0xFF8E8E93)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF2C2E38),
                                        unfocusedContainerColor = Color(0xFF2C2E38),
                                        disabledContainerColor = Color(0xFF2C2E38),
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
                                // Preserve visual space so dialogue card size remains perfectly stable
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
