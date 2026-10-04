package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Единая точка входа визуальных значений: компоненты берут цвета, стили текста и формы только отсюда,
 * по роли (`ThingsTheme.colors.overlaySurface`), а не числом. Одна роль — одно значение: так у похожих
 * элементов не расходятся размеры и оттенки. Тема (светлая / тёмная) выбирается здесь один раз.
 *
 * Константы из Color.kt — палитра, из которой собраны роли; в компонентах вместо них — роли.
 * Спецификация ролей: docs/design-system.
 */

/** Цвета по ролям для одной темы. */
@Immutable
data class ThingsColors(
    val isDark: Boolean,
    /** Фон экрана и списка */
    val background: Color,
    /** Поверхности: Material surface, карточки */
    val surface: Color,
    /** Разделители строк */
    val divider: Color,
    /** Основной текст на `background` */
    val textPrimary: Color,
    /** Вторичный текст: подписи, даты, пояснения на `background` */
    val textSecondary: Color,
    /** Главный акцент: «+», отмеченный чекбокс, курсор, галочка выбранного */
    val accent: Color,
    /** Опасное действие: удаление */
    val danger: Color,
    /** Тёмная подложка в обеих темах: диалоги, меню, плавающая панель действий, кружки у «+» */
    val overlaySurface: Color,
    /** Основной текст и иконки на `overlaySurface` */
    val overlayContent: Color,
    /** Вторичный текст и иконки на `overlaySurface` */
    val overlayContentSecondary: Color,
    /** Кнопки, поля и кнопка закрытия внутри тёмных диалогов */
    val overlayControl: Color,
    /** Выбранная строка в тёмном диалоге */
    val overlayRowSelected: Color,
    /** Разделители на `overlaySurface` */
    val overlayDivider: Color,
    /** Текст заметок в редакторе задачи */
    val textNotes: Color,
    /** Текст задачи и чек-листа в редакторе */
    val editorText: Color,
    /** Рамка неотмеченного чекбокса */
    val checkboxBorder: Color,
    /** Фон чипа тега */
    val tagChipBackground: Color,
    /** Текст чипа тега */
    val tagChipText: Color,
    /** Кружок чек-листа */
    val checklistCircle: Color,
    /** Галочка чек-листа */
    val checklistCheck: Color,
    /** Ручка перетаскивания пункта чек-листа */
    val checklistHandle: Color,
    /** Удаление пункта свайпом */
    val checklistDelete: Color,
    /** Разделитель пунктов чек-листа */
    val checklistDivider: Color,
    /** Текст выполненного пункта чек-листа */
    val checklistCompleted: Color,
    /** Фон выделенной строки в режиме мультивыбора */
    val accentSelection: Color,
    /** Фон списка при открытом редакторе */
    val listDim: Color,
    /** Плашка места вставки при перетаскивании */
    val dropPlaceholder: Color,
    /** Тонкая обводка карточек стопки и разделителей */
    val hairline: Color,
    /** Нижние карточки стопки при перетаскивании */
    val stackCard: Color,
    /** Фон поля поиска (Quick Find и полноэкранный поиск) */
    val searchField: Color,
    /** Фон карточки Quick Find */
    val searchCard: Color,
    /** Подсветка совпадений в результатах поиска */
    val searchMatchHighlight: Color,
    /** Кнопка закрытия ✕ в Quick Find */
    val searchCloseButton: Color,
    /** Фон круга индикатора оттяжки поиска */
    val pullIndicatorBackground: Color,
    /** Стрелка индикатора оттяжки поиска */
    val pullArrow: Color,
    /** Стрелка индикатора оттяжки после порога */
    val pullArrowSelected: Color,
)

private val LightThingsColors = ThingsColors(
    isDark = false,
    background = ThingsBackgroundLight,
    surface = ThingsSurfaceLight,
    divider = ThingsDividerLight,
    textPrimary = ThingsTextPrimaryLight,
    textSecondary = ThingsTextSecondaryLight,
    accent = ThingsBlue,
    danger = ThingsUpcomingRed,
    overlaySurface = ThingsDialogBackground,
    overlayContent = Color.White,
    overlayContentSecondary = ThingsMetaGrey,
    overlayControl = ThingsDialogButton,
    overlayRowSelected = ThingsDialogRowSelected,
    overlayDivider = ThingsDividerDark,
    textNotes = ThingsTextNotesLight,
    editorText = ThingsInk,
    checkboxBorder = ThingsCheckboxBorder,
    tagChipBackground = ThingsTagChipBackground,
    tagChipText = ThingsTagChipText,
    checklistCircle = ThingsChecklistCircle,
    checklistCheck = ThingsChecklistCheck,
    checklistHandle = ThingsChecklistHandle,
    checklistDelete = ThingsChecklistDelete,
    checklistDivider = ThingsChecklistDivider,
    checklistCompleted = ThingsChecklistCompletedText,
    accentSelection = ThingsBlue.copy(alpha = 0.22f),
    listDim = ThingsListDimLight,
    dropPlaceholder = ThingsDropPlaceholderLight,
    hairline = ThingsHairlineLight,
    stackCard = ThingsBackgroundLight,
    searchField = ThingsFieldLight,
    searchCard = Color.White,
    searchMatchHighlight = SearchMatchHighlightLight,
    searchCloseButton = ThingsHairlineLight,
    pullIndicatorBackground = ThingsPullIndicatorLight,
    pullArrow = ThingsPullArrowLight,
    pullArrowSelected = ThingsBlue,
)

private val DarkThingsColors = LightThingsColors.copy(
    isDark = true,
    background = ThingsBackgroundDark,
    surface = ThingsSurfaceDark,
    divider = ThingsDividerDark,
    textPrimary = ThingsTextPrimaryDark,
    textSecondary = ThingsTextSecondaryDark,
    listDim = ThingsListDimDark,
    dropPlaceholder = ThingsDropPlaceholderDark,
    hairline = ThingsHairlineDark,
    stackCard = ThingsStackCardDark,
    searchField = ThingsFieldDark,
    searchCard = ThingsInk,
    searchMatchHighlight = SearchMatchHighlightDark,
    searchCloseButton = ThingsFieldDark,
    pullIndicatorBackground = ThingsFieldDark,
    pullArrow = ThingsPullArrowDark,
    pullArrowSelected = ThingsPullArrowSelectedDark,
)

/** Стили текста по ролям. На экранах шире 600 dp размеры умножаются на 1.25 — здесь, один раз. */
@Immutable
data class ThingsTypography(
    /** Заголовок диалога: Move, теги, When, удаление */
    val dialogTitle: TextStyle,
    /** Строка выбора в диалоге: проект в Move, тег, «Сегодня» в When */
    val dialogRow: TextStyle,
    /** Пояснительный текст диалога */
    val dialogBody: TextStyle,
    /** Кнопка действия в диалоге: Cancel, Delete, Clear */
    val dialogButton: TextStyle,
    /** Пункт выпадающего меню: «New To-Do», «Delete Project», меню подзаголовка */
    val menuItem: TextStyle,
    /** Название задачи в строке списка и в редакторе */
    val taskTitle: TextStyle,
    /** Подпись под задачей (проект, дата) */
    val taskSubtitle: TextStyle,
    /** Заметки в раскрытом редакторе */
    val editorNotes: TextStyle,
    /** Пункты чек-листа в раскрытом редакторе */
    val editorChecklist: TextStyle,
    /** Строка даты и дедлайна в редакторе */
    val editorDate: TextStyle,
    /** Заголовок секции результатов поиска */
    val sectionHeader: TextStyle,
    /** Мелкие метки и бейджи */
    val caption: TextStyle,
)

private fun thingsTypography(scale: Float) = ThingsTypography(
    dialogTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = (20 * scale).sp),
    dialogRow = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (19 * scale).sp),
    dialogBody = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (15 * scale).sp),
    dialogButton = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (18 * scale).sp),
    menuItem = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (18 * scale).sp),
    taskTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (19 * scale).sp),
    taskSubtitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (15 * scale).sp),
    editorNotes = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (18 * scale).sp),
    editorChecklist = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (17.2f * scale).sp),
    editorDate = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (18 * scale).sp),
    sectionHeader = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = (19 * scale).sp),
    caption = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp),
)

/** Скругления по ролям. */
@Immutable
data class ThingsShapes(
    /** Строка списка, строка в диалоге, раскрытый редактор */
    val row: Dp = 8.dp,
    /** Чип, плашка, мелкая карточка */
    val chip: Dp = 10.dp,
    /** Модальный диалог: Move, теги, When, удаление */
    val dialog: Dp = 32.dp,
    /** Всплывающее меню-карточка: меню «+» на главном экране */
    val menu: Dp = 16.dp,
    /** Плавающая карточка, вырастающая из своего источника: Quick Find, Quick Add */
    val floatingCard: Dp = 30.dp,
) {
    val rowShape get() = RoundedCornerShape(row)
    val chipShape get() = RoundedCornerShape(chip)
    val dialogShape get() = RoundedCornerShape(dialog)
    val floatingCardShape get() = RoundedCornerShape(floatingCard)
    val menuShape get() = RoundedCornerShape(menu)
}

private val LocalThingsColors = staticCompositionLocalOf { LightThingsColors }
private val LocalThingsTypography = staticCompositionLocalOf { thingsTypography(1f) }
private val LocalThingsShapes = staticCompositionLocalOf { ThingsShapes() }

/** Подаёт роли всем компонентам под ним. Вызывается из MyApplicationTheme. */
@Composable
fun ProvideThingsTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scale = if (LocalConfiguration.current.screenWidthDp >= 600) 1.25f else 1f
    CompositionLocalProvider(
        LocalThingsColors provides if (dark) DarkThingsColors else LightThingsColors,
        LocalThingsTypography provides thingsTypography(scale),
        LocalThingsShapes provides ThingsShapes(),
        content = content
    )
}

object ThingsTheme {
    val colors: ThingsColors
        @Composable @ReadOnlyComposable get() = LocalThingsColors.current
    val type: ThingsTypography
        @Composable @ReadOnlyComposable get() = LocalThingsTypography.current
    val shapes: ThingsShapes
        @Composable @ReadOnlyComposable get() = LocalThingsShapes.current
}
