package com.example.ui.screens.home.inlineeditor.dialogs

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ThingsBlue

/**
 * A dialog that allows selecting and adding tags to the current task.
 * Matches the dark theme and layout from the user's design screenshot.
 *
 * @param activeTags Currently active tags for the task.
 * @param onTagsSelected Callback triggered when user clicks 'Done', passing the updated list of tags.
 * @param onDismissRequest Callback triggered when the dialog is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    var showNewTagInput by remember { mutableStateOf(false) }

    val titleFontSize = MaterialTheme.typography.headlineSmall.fontSize
    val notesFontSize = MaterialTheme.typography.titleSmall.fontSize

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
                // Top header: "Tags" and "Done"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tags",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = "Done",
                        style = TextStyle(
                            color = ThingsBlue,
                            fontSize = notesFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable {
                                onTagsSelected(selectedTags.toList())
                                onDismissRequest()
                            }
                    )
                }

                // Scrollable Tags List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableTags) { tag ->
                        val isSelected = selectedTags.contains(tag)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
                        onClick = { showNewTagInput = true },
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
        }
    }

    // Modal dialog to input a new tag name
    if (showNewTagInput) {
        var newTagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewTagInput = false },
            title = { Text("New Tag", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThingsBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ThingsBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newTagName.trim()
                        if (name.isNotEmpty()) {
                            onNewTagCreated(name)
                            selectedTags = selectedTags + name
                        }
                        showNewTagInput = false
                        newTagName = ""
                    }
                ) {
                    Text("Create", color = ThingsBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewTagInput = false }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF22242C),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
