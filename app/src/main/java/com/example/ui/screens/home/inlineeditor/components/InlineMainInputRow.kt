package com.example.ui.screens.home.inlineeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.ui.components.ThingsCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.dimens
import com.example.ui.theme.taskEditorNotes

@Composable
fun InlineMainInputRow(
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isCompleted: Boolean,
    onCheckboxClick: () -> Unit,
    expansionProgress: Float = 1f
) {
    val textPrimaryColor = Color(0xFF1C1C1E) // blackish font
    val textSecondaryColor = com.example.ui.theme.ThingsTextNotesLight

    // Размер шрифта в заголовке статически равен MaterialTheme.typography.titleMedium.fontSize
    val titleFontSize = MaterialTheme.typography.titleMedium.fontSize
    val notesFontSize = MaterialTheme.typography.taskEditorNotes.fontSize

    val titleSpacing = (8 + 4 * expansionProgress).dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Custom ThingsCheckbox
        ThingsCheckbox(
            checked = isCompleted,
            onCheckedChange = { onCheckboxClick() },
            size = MaterialTheme.dimens.mainCheckboxSize,
            uncheckedColor = Color(0xFFC7C7CC),
            modifier = Modifier.padding(end = titleSpacing, top = 2.dp)
        )

        // Title and Notes Fields (Always black text on white background)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                textStyle = TextStyle(
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Normal,
                    color = textPrimaryColor
                ),
                cursorBrush = SolidColor(ThingsBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            "New To-Do",
                            style = TextStyle(
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Normal,
                                color = textSecondaryColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            )

            // Smoothly collapse height and fade out the notes section during closing animation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = expansionProgress
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val calculatedHeight = (placeable.height * expansionProgress).toInt()
                        layout(placeable.width, calculatedHeight) {
                            placeable.place(0, 0)
                        }
                    }
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.taskExpandedTitleNotesGap))

                BasicTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    minLines = 2,
                    textStyle = TextStyle(
                        fontSize = notesFontSize,
                        fontWeight = FontWeight.Normal,
                        color = textSecondaryColor
                    ),
                    cursorBrush = SolidColor(ThingsBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("task_notes_input"),
                    decorationBox = { innerTextField ->
                        if (notes.isEmpty()) {
                            Text(
                                "Notes",
                                style = TextStyle(
                                    fontSize = notesFontSize,
                                    fontWeight = FontWeight.Normal,
                                    color = textSecondaryColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}
