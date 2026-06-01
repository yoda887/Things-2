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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.dimens

@Composable
fun InlineMainInputRow(
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isCompleted: Boolean,
    onCheckboxClick: () -> Unit
) {
    val textPrimaryColor = Color(0xFF1C1C1E) // blackish font
    val textSecondaryColor = Color(0xFF747679) // dark grey font

    val titleFontSize = MaterialTheme.typography.headlineSmall.fontSize
    val notesFontSize = MaterialTheme.typography.bodyMedium.fontSize

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
            modifier = Modifier.padding(end = 12.dp, top = 2.dp)
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

            Spacer(modifier = Modifier.height(6.dp))

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
