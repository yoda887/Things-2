package com.example.ui.screens.home.subcomponents

import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsAlpha
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens

private val CHEVRON_GAP = 3.dp
private val SEARCH_EMPTY_VERTICAL_PADDING = 56.dp

private val TAG_BADGE_ICON_SIZE = 11.dp
private val EMPTY_STATE_ICON_SIZE = 60.dp

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
    val fieldBackground = ThingsTheme.colors.searchField
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
            .clip(ThingsTheme.shapes.capsuleShape)
            .background(fieldBackground)
            .padding(horizontal = ThingsSpacing.M_PLUS)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = textSecondaryColor,
            modifier = Modifier.size(ThingsIconSize.M)
        )
        Spacer(modifier = Modifier.width(ThingsSpacing.S))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.cd_search),
                    color = textSecondaryColor.copy(alpha = ThingsAlpha.HINT),
                    style = ThingsTheme.type.bodyLarge
                )
            }
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != query) onQueryChange(newValue.text)
                },
                textStyle = ThingsTheme.type.bodyLarge.copy(color = textPrimaryColor),
                cursorBrush = SolidColor(ThingsTheme.colors.accent),
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
                    .size(ThingsIconSize.S)
                    .clip(CircleShape)
                    .background(textSecondaryColor.copy(alpha = ThingsAlpha.HALF))
                    .clickable {
                        textFieldValue = TextFieldValue("", selection = TextRange.Zero)
                        onQueryChange("")
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_clear),
                    tint = ThingsTheme.colors.background,
                    modifier = Modifier.size(TAG_BADGE_ICON_SIZE)
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
            .padding(top = SEARCH_SECTION_TOP_SPACING, bottom = ThingsSpacing.XS_PLUS)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ThingsTheme.shapes.rowShape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(
                    start = MaterialTheme.dimens.taskRowStartPadding,
                    end = MaterialTheme.dimens.taskRowEndPadding,
                    top = ThingsSpacing.XS,
                    bottom = ThingsSpacing.XS_PLUS
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
                style = ThingsTheme.type.sectionHeader.copy(color = textPrimaryColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (showChevron) {
                Spacer(modifier = Modifier.width(CHEVRON_GAP))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.MUTED),
                    modifier = Modifier.size(ThingsIconSize.XS)
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
                .height(ThingsStroke.DIVIDER)
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
            .height(MaterialTheme.dimens.rowHeight)
            .clip(ThingsTheme.shapes.rowShape)
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
            style = ThingsTheme.type.listTitle.copy(
                color = textPrimaryColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(CHEVRON_GAP))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.MUTED),
            modifier = Modifier.size(ThingsIconSize.XS)
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
            .padding(vertical = SEARCH_EMPTY_VERTICAL_PADDING),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textSecondaryColor.copy(alpha = ThingsAlpha.LOW),
                modifier = Modifier.size(EMPTY_STATE_ICON_SIZE)
            )
            Spacer(modifier = Modifier.height(ThingsSpacing.M))
            Text(
                text = text,
                color = textSecondaryColor,
                style = ThingsTheme.type.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
