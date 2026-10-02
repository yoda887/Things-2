package com.example.ui.screens.home.subcomponents

import com.example.ui.theme.ThingsFieldDark
import com.example.ui.theme.ThingsFieldLight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThingsBackgroundDark
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.dimens

// Строка поиска — как поле Quick Find на стартовом экране
private val SEARCH_FIELD_HEIGHT = 44.dp
// Заголовки секций — как разделы экрана области
private val SEARCH_SECTION_TOP_SPACING = 32.dp

/** Размер иконки в колонке чекбоксов у заголовков и строк результатов поиска. */
val SEARCH_ROW_ICON_SIZE = 20.dp

/**
 * Строка запроса экрана поиска. При первом показе забирает фокус и поднимает клавиатуру.
 *
 * @param autoFocus забрать фокус при появлении. Поле — строка списка: прокрученное за край, оно
 *   уходит из композиции и при возврате создаётся заново, поэтому решение «фокус уже ставили» хранит
 *   экран (см. [onAutoFocused]), иначе клавиатура выезжала бы при каждой прокрутке к началу списка.
 */
@Composable
fun SearchQueryField(
    query: String,
    onQueryChange: (String) -> Unit,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    onAutoFocused: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val fieldBackground = if (isDark) ThingsFieldDark else ThingsFieldLight
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }
    // Запрос может смениться снаружи (очистка, возврат на экран) — курсор ставится в конец
    LaunchedEffect(query) {
        if (textFieldValue.text != query) {
            textFieldValue = textFieldValue.copy(text = query, selection = TextRange(query.length))
        }
    }
    LaunchedEffect(Unit) {
        if (autoFocus) {
            focusRequester.requestFocus()
            onAutoFocused()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(SEARCH_FIELD_HEIGHT)
            .clip(RoundedCornerShape(SEARCH_FIELD_HEIGHT / 2))
            .background(fieldBackground)
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = textSecondaryColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search",
                    color = textSecondaryColor.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != query) onQueryChange(newValue.text)
                },
                textStyle = TextStyle(color = textPrimaryColor, fontSize = 16.sp),
                cursorBrush = SolidColor(ThingsBlue),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("fullscreen_search_input")
            )
        }
        if (query.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(textSecondaryColor.copy(alpha = 0.5f))
                    .clickable {
                        textFieldValue = TextFieldValue("", selection = TextRange.Zero)
                        onQueryChange("")
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = if (isDark) ThingsBackgroundDark else Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * Заголовок секции результатов — как заголовки разделов на экране области: иконка в колонке
 * чекбоксов, название и тонкий разделитель.
 */
@Composable
fun SearchSectionHeader(
    title: String,
    icon: @Composable () -> Unit,
    dividerColor: Color,
    textPrimaryColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SEARCH_SECTION_TOP_SPACING, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(
                    start = MaterialTheme.dimens.taskRowStartPadding,
                    end = MaterialTheme.dimens.taskRowEndPadding,
                    top = 4.dp,
                    bottom = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(MaterialTheme.dimens.taskLeftColumnWidthDefault),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.taskSpacingToTextDefault))
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimaryColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (showChevron) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.dimens.taskRowStartPadding,
                    end = MaterialTheme.dimens.taskRowEndPadding
                )
                .height(0.6.dp)
                .background(dividerColor)
        )
    }
}

/**
 * Строка найденной области или тега — в той же раскладке, что строка проекта в списках.
 */
@Composable
fun SearchEntityRow(
    title: String,
    icon: @Composable () -> Unit,
    textPrimaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(
                start = MaterialTheme.dimens.taskRowStartPadding,
                end = MaterialTheme.dimens.taskRowEndPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(MaterialTheme.dimens.taskLeftColumnWidthDefault),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.taskSpacingToTextDefault))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                color = textPrimaryColor,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Пустое состояние экрана поиска — в стиле пустого состояния списков.
 */
@Composable
fun SearchEmptyState(
    icon: ImageVector,
    text: String,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textSecondaryColor.copy(alpha = 0.3f),
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                color = textSecondaryColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
