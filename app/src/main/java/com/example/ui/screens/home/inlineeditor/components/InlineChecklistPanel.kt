package com.example.ui.screens.home.inlineeditor.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.ui.components.ThingsCheckbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.ChecklistItem
import com.example.ui.theme.ThingsBlue

@Composable
fun InlineChecklistPanel(
    itemId: String = "",
    checklist: List<ChecklistItem>,
    onChecklistChange: (List<ChecklistItem>) -> Unit,
    showChecklistHelper: Boolean,
    onShowChecklistHelperChange: (Boolean) -> Unit,
    expansionProgress: Float = 1f
) {
    val showPanel = showChecklistHelper || checklist.isNotEmpty()

    val textPrimaryColor = Color(0xFF1C1C1E)
    val textSecondaryColor = Color(0xFF747679)
    val bodyFontSize = MaterialTheme.typography.bodyMedium.fontSize

    var newChecklistItemTitle by remember { mutableStateOf("") }

    val startPadding = 24.dp

    AnimatedVisibility(
        visible = showPanel,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding, top = 8.dp)
        ) {
            if (checklist.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
            }
            checklist.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThingsCheckbox(
                        checked = item.isCompleted,
                        onCheckedChange = {
                            val updated = checklist.mapIndexed { idx, checklistItem ->
                                if (idx == index) checklistItem.copy(isCompleted = !checklistItem.isCompleted)
                                else checklistItem
                            }
                            onChecklistChange(updated)
                        },
                        size = 15.dp,
                        uncheckedColor = Color(0xFFD1D1D6)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Editable checklist item title inline
                    BasicTextField(
                        value = item.title,
                        onValueChange = { updatedTitle ->
                            val updated = checklist.mapIndexed { idx, checklistItem ->
                                if (idx == index) checklistItem.copy(title = updatedTitle)
                                else checklistItem
                            }
                            onChecklistChange(updated)
                        },
                        textStyle = TextStyle(
                            fontSize = bodyFontSize,
                            color = if (item.isCompleted) textSecondaryColor else textPrimaryColor,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        cursorBrush = SolidColor(ThingsBlue),
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        //imageVector = Icons.Default.Close,
                        //contentDescription = "Remove Item",
                        //tint = textSecondaryColor.copy(alpha = 0.4f),
                        imageVector = Icons.Default.Reorder,
                        contentDescription = "Reorder Item",
                        tint = textSecondaryColor.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(16.dp)
                            //.clickable {
                              //  val updated = checklist.filterIndexed { idx, _ -> idx != index }
                                //onChecklistChange(updated)
                            //}
                    )
                }
                HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
           
            }

            // Add inline checklist item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Checklist",
                    tint = ThingsBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = newChecklistItemTitle,
                    onValueChange = { newChecklistItemTitle = it },
                    textStyle = TextStyle(fontSize = bodyFontSize, color = textPrimaryColor),
                    cursorBrush = SolidColor(ThingsBlue),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newChecklistItemTitle.isNotBlank()) {
                            val updated = checklist + ChecklistItem(itemId = itemId, title = newChecklistItemTitle.trim())
                            onChecklistChange(updated)
                            newChecklistItemTitle = ""
                        }
                    }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (newChecklistItemTitle.isEmpty()) {
                            Text(
                                "Add Checklist Item...",
                                style = TextStyle(fontSize = bodyFontSize, color = textSecondaryColor.copy(alpha = 0.4f))
                            )
                        }
                        innerTextField()
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Checklist Helper",
                    tint = textSecondaryColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable {
                            onShowChecklistHelperChange(false)
                        }
                )
            }
        }
    }
}
