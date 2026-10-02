package com.example.ui.screens.home.subcomponents

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.ui.components.HideTextSelectionHandles
import com.example.ui.components.hideSoftKeyboardThen
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsDeepBlue
import com.example.ui.theme.ThingsUpcomingRed

private val HeadingMenuBackground = Color(0xFF22242C)
// Тот же размер, что у остальных подзаголовков экранов («Вечер», «Планы», «Когда-нибудь»)
private val HeadingTitleStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = ThingsDeepBlue)

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
                    Text(text = heading.title, style = HeadingTitleStyle, maxLines = 1)
                }
            }

            Box {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Heading Options",
                    tint = ThingsDeepBlue,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showMenu = true }
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(HeadingMenuBackground)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = Color.White, fontSize = 17.sp) },
                        onClick = { showMenu = false; onStartEditing() }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive", color = Color.White, fontSize = 17.sp) },
                        onClick = { showMenu = false; onArchive() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = ThingsUpcomingRed, fontSize = 17.sp) },
                        onClick = { showMenu = false; onDelete() }
                    )
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
            textStyle = HeadingTitleStyle,
            cursorBrush = SolidColor(if (isFinishing) Color.Unspecified else ThingsBlue),
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
                        Text("New Heading", style = HeadingTitleStyle.copy(color = ThingsDeepBlue.copy(alpha = 0.4f)))
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
