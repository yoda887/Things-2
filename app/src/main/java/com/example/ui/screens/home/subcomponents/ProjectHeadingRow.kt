package com.example.ui.screens.home.subcomponents

import com.example.ui.theme.ThingsAlpha
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import com.example.ui.components.ThingsDropdownMenu
import com.example.ui.components.ThingsMenuItem
import com.example.ui.theme.ThingsTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.data.model.Item
import com.example.ui.components.HideTextSelectionHandles
import com.example.ui.components.hideSoftKeyboardThen

private val HEADING_OPTIONS_ICON_SIZE = 26.dp

// Тот же размер, что у остальных подзаголовков экранов («Вечер», «Планы», «Когда-нибудь»)
private val headingTitleStyle: TextStyle
    @Composable get() = ThingsTheme.type.sectionHeader.copy(color = ThingsTheme.colors.project, fontWeight = FontWeight.SemiBold)

/**
 * Заголовок внутри проекта, как в Things: голубое название, «•••» с меню и линия под ним.
 *
 * Название правится прямо в строке. Клавиатура убирается по общим правилам (см. hideSoftKeyboardThen):
 * поле исчезает сразу, без анимации, поэтому сохранение — уже после полного скрытия клавиатуры.
 *
 * @param isEditing строка в режиме правки названия (в том числе только что созданный заголовок)
 * @param onTitleCommit правка закончена; пустое название у нового заголовка — его удаление
 */
@Composable
fun ProjectHeadingRow(
    heading: Item,
    isEditing: Boolean,
    dividerColor: Color,
    onStartEditing: () -> Unit,
    onTitleCommit: (String) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable(
                    enabled = !isEditing,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onStartEditing
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    HeadingTitleField(heading = heading, onCommit = onTitleCommit)
                } else {
                    Text(text = heading.title, style = headingTitleStyle, maxLines = 1)
                }
            }

            Box {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = stringResource(R.string.cd_heading_options),
                    tint = ThingsTheme.colors.project,
                    modifier = Modifier
                        .size(HEADING_OPTIONS_ICON_SIZE)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showMenu = true }
                )
                ThingsDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    ThingsMenuItem(stringResource(R.string.ui_rename), Icons.Default.Edit, onClick = { showMenu = false; onStartEditing() })
                    ThingsMenuItem(stringResource(R.string.ui_archive), Icons.Default.Archive, onClick = { showMenu = false; onArchive() })
                    ThingsMenuItem(stringResource(R.string.tag_dialog_delete), Icons.Default.Delete, destructive = true, onClick = { showMenu = false; onDelete() })
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(0.6.dp)
                .background(dividerColor)
        )
    }
}

/** Поле названия заголовка: фокус сразу, сохранение по «Готово» или при уходе фокуса. */
@Composable
private fun HeadingTitleField(heading: Item, onCommit: (String) -> Unit) {
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    var text by remember(heading.id) { mutableStateOf(heading.title) }
    var hasFocused by remember { mutableStateOf(false) }
    var isFinishing by remember { mutableStateOf(false) }

    HideTextSelectionHandles(hidden = isFinishing) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = headingTitleStyle,
            cursorBrush = SolidColor(if (isFinishing) Color.Unspecified else ThingsTheme.colors.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (hasFocused) {
                        hasFocused = false
                        isFinishing = true
                        // Поле пропадает сразу, без анимации, — убираем его только после полного
                        // скрытия клавиатуры, иначе Compose переподключит ещё видимую клавиатуру
                        view.hideSoftKeyboardThen { onCommit(text.trim()) }
                    }
                }
            ),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(stringResource(R.string.ui_new_heading_2), style = headingTitleStyle.copy(color = ThingsTheme.colors.project.copy(alpha = ThingsAlpha.MUTED)))
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        hasFocused = true
                    } else if (hasFocused) {
                        hasFocused = false
                        onCommit(text.trim())
                    }
                }
        )
    }

    LaunchedEffect(heading.id) {
        focusRequester.requestFocus()
    }
}
