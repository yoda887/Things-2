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
import com.example.ui.components.HideTextSelectionHandles
import androidx.compose.ui.graphics.Brush

/**
 * @param showCursor при `false` курсор и его маркер не рисуются, но поле остаётся в фокусе.
 * Нужен на время сворачивания редактора: снять там фокус или выключить поле (`enabled = false`)
 * нельзя — Compose сразу вызовет restartInput, и ещё видимая клавиатура перерисуется
 * (см. hideSoftKeyboardNow).
 */
@Composable
fun InlineMainInputRow(
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isCompleted: Boolean,
    onCheckboxClick: () -> Unit,
    expansionProgress: Float = 1f,
    subtitleText: String? = null,
    showCursor: Boolean = true
) {
    HideTextSelectionHandles(hidden = !showCursor) {
        InlineMainInputRowContent(
            title = title,
            onTitleChange = onTitleChange,
            notes = notes,
            onNotesChange = onNotesChange,
            isCompleted = isCompleted,
            onCheckboxClick = onCheckboxClick,
            expansionProgress = expansionProgress,
            subtitleText = subtitleText,
            cursorBrush = SolidColor(if (showCursor) ThingsBlue else Color.Unspecified)
        )
    }
}

@Composable
private fun InlineMainInputRowContent(
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isCompleted: Boolean,
    onCheckboxClick: () -> Unit,
    expansionProgress: Float,
    subtitleText: String?,
    cursorBrush: Brush
) {
    val textPrimaryColor = Color(0xFF1C1C1E) // blackish font
    val textSecondaryColor = com.example.ui.theme.ThingsTextNotesLight

    // Размер шрифта в заголовке статически равен MaterialTheme.typography.titleMedium.fontSize
    val titleFontSize = MaterialTheme.typography.titleMedium.fontSize
    val notesFontSize = MaterialTheme.typography.taskEditorNotes.fontSize
    val subFontSize = MaterialTheme.typography.bodySmall.fontSize

    val titleSpacing = 8.dp

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
            TitleSubtitleLayout(
                hasSubtitle = !subtitleText.isNullOrBlank(),
                expansionProgress = expansionProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Normal,
                        color = textPrimaryColor
                    ),
                    cursorBrush = cursorBrush,
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

                if (!subtitleText.isNullOrBlank()) {
                    Text(
                        text = subtitleText,
                        style = TextStyle(
                            fontSize = subFontSize,
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dummy text to measure exactly one line height for the title
                Text(
                    text = "A",
                    style = TextStyle(
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Normal,
                        color = textPrimaryColor
                    ),
                    maxLines = 1
                )
            }

            // Smoothly fade out the notes section during closing animation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = expansionProgress
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
                    cursorBrush = cursorBrush,
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

@Composable
fun TitleSubtitleLayout(
    hasSubtitle: Boolean,
    expansionProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val titlePlaceable = measurables[0].measure(constraints)
        val subtitlePlaceable = if (hasSubtitle && measurables.size > 1) {
            measurables[1].measure(constraints)
        } else null
        
        val dummyIndex = if (hasSubtitle) 2 else 1
        val dummyPlaceable = if (measurables.size > dummyIndex) {
            measurables[dummyIndex].measure(constraints)
        } else null

        val titleHeight = titlePlaceable.height
        val subtitleHeight = subtitlePlaceable?.height ?: 0
        
        val titleSingleHeight = dummyPlaceable?.height ?: titleHeight

        val simulatedFullHeight = titleSingleHeight + subtitleHeight
        
        // --- Calculate Y offsets ---
        // At progress = 0, we want the content (simulatedFullHeight) to be vertically centered in 46.dp.
        // The Layout itself is placed at topPadding (13.dp) inside the Card.
        val cardHeightPx = 46.dp.toPx()
        val topPaddingPx = 13.dp.toPx()
        
        // Absolute Y position in the Card where the content should start:
        val targetAbsoluteY = (cardHeightPx - simulatedFullHeight.toFloat()) / 2f
        
        // Since Layout is at topPaddingPx, the offset relative to Layout is:
        val startOffset = targetAbsoluteY - topPaddingPx
        
        // Current offset interpolates to 0 at progress = 1 (subpixel Float precision)
        val currentOffset = startOffset * (1f - expansionProgress)

        val width = maxOf(titlePlaceable.width, subtitlePlaceable?.width ?: 0)

        // Reported height is simply titleHeight, since the card container smoothly animates its overall height
        layout(width, titleHeight) {
            titlePlaceable.placeWithLayer(0, 0) {
                translationY = currentOffset
            }
            
            // Subtitle starts below title's first line, and as progress goes to 1, it moves UP (overlaps title)
            val subtitleStartY = startOffset + titleSingleHeight.toFloat()
            val subtitleCurrentY = subtitleStartY * (1f - expansionProgress)
            
            subtitlePlaceable?.placeWithLayer(0, 0) {
                translationY = subtitleCurrentY
                alpha = (1f - expansionProgress).coerceIn(0f, 1f)
            }
        }
    }
}
