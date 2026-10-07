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
    /** Вспышка строки чек-листа при отметке пункта */
    val checklistHighlight: Color,
    /** Фон пункта чек-листа, текст которого в фокусе */
    val checklistFocus: Color,
    /** Фон пункта чек-листа, который тащат за ручку */
    val checklistDrag: Color,
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
    /** Карточка виджета календаря */
    val calendarCard: Color,
    /** Приглушённые прошедшие события и разделители календаря */
    val calendarMuted: Color,
    /** Фон бейджа даты */
    val badgeBackground: Color,
    /** Текст бейджа даты */
    val badgeText: Color,
    /** Неактивные иконки в строке инструментов редактора */
    val editorIconInactive: Color,
    /** Кнопка «Clear» в диалоге When */
    val clearAction: Color,
    /** Текст и иконки на цветной заливке: «+», отмеченный чекбокс, фон свайпа, выбранный тег, кнопки `accent` */
    val onAccent: Color,
    /** Затемнение под модальными окнами и при переходах (прозрачность задаёт место использования) */
    val scrim: Color,
    /** Разделы: значок, маркер и подпись раздела */
    val inbox: Color,
    val today: Color,
    val upcoming: Color,
    val anytime: Color,
    val someday: Color,
    /** Область: значок и название */
    val area: Color,
    /** Проект: иконка-прогресс, подзаголовки проекта и их меню */
    val project: Color,
    /** Фон свайпа вправо (действие «Когда») */
    val swipeWhen: Color,
    /** Значок «вечер» в строке задачи */
    val eveningIndicator: Color,
    /** Контур анимированной иконки области на главном экране */
    val areaIconStroke: Color,
    /** Место нового подзаголовка при перетаскивании «+»: пунктир и надпись */
    val newHeadingLine: Color,
    val newHeadingText: Color,
    /** Плашка места вставки нового проекта на главном экране */
    val homeDropPlaceholder: Color,
    /** Цвет события, если у календаря нет своего */
    val calendarDefault: Color,
    /** Пресеты цветов календарей */
    val calendarPresets: List<Color>,
)

internal val LightThingsColors = ThingsColors(
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
    checklistHighlight = ThingsChecklistHighlight,
    checklistFocus = ThingsChecklistFocus,
    checklistDrag = ThingsChecklistDrag,
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
    calendarCard = ThingsFieldLight,
    calendarMuted = ThingsMutedGreyLight,
    badgeBackground = ThingsBadgeBackgroundLight,
    badgeText = ThingsBadgeTextLight,
    editorIconInactive = ThingsEditorIconInactive,
    clearAction = ThingsWhenClear,
    onAccent = Color.White,
    scrim = Color.Black,
    inbox = ThingsInboxBlue,
    today = ThingsTodayStar,
    upcoming = ThingsUpcomingRed,
    anytime = ThingsAnytimeTeal,
    someday = ThingsSomedayGrey,
    area = ThingsAreaGreenLight,
    project = ThingsDeepBlue,
    swipeWhen = ThingsSwipeWhenYellow,
    eveningIndicator = ThingsEveningIndicator,
    areaIconStroke = ThingsAreaIconStroke,
    newHeadingLine = ThingsNewHeadingLine,
    newHeadingText = ThingsNewHeadingText,
    homeDropPlaceholder = ThingsHomeDropPlaceholder,
    calendarDefault = ThingsCalendarDefaultGreen,
    calendarPresets = listOf(CalendarGreen, CalendarBlue, CalendarYellow, CalendarRed, CalendarPurple, CalendarPink, CalendarTeal),
)

internal val DarkThingsColors = LightThingsColors.copy(
    isDark = true,
    background = ThingsBackgroundDark,
    surface = ThingsSurfaceDark,
    divider = ThingsDividerDark,
    textPrimary = ThingsTextPrimaryDark,
    textSecondary = ThingsTextSecondaryDark,
    // Редактор задачи лежит на фоне списка, поэтому в тёмной теме его текст и чек-лист тоже светлые
    editorText = ThingsTextPrimaryDark,
    textNotes = ThingsTextSecondaryDark,
    editorIconInactive = ThingsTextSecondaryDark,
    checklistDivider = ThingsDividerDark,
    checklistCompleted = ThingsTextSecondaryDark,
    checklistHighlight = ThingsFieldDark,
    checklistFocus = ThingsStackCardDark,
    checklistDrag = ThingsBlue.copy(alpha = 0.25f),
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
    calendarCard = ThingsSurfaceDark,
    calendarMuted = ThingsMutedGreyDark,
    badgeBackground = ThingsFieldDark,
    badgeText = ThingsBadgeTextDark,
    area = ThingsAreaGreenDark,
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
    /** Заголовок верхней панели навигации (TopAppBar) */
    val topAppBarTitle: TextStyle,
    /** Мелкие метки и бейджи */
    val caption: TextStyle,
    /** Чип тега в редакторе задачи и окне быстрого добавления (14 sp Medium) */
    val tagChip: TextStyle,
    /** Текст бейджа даты */
    val badge: TextStyle,
    /** Мелкий пояснительный текст (13 sp) */
    val bodySmall: TextStyle,
    /** Стандартный вспомогательный текст (14 sp) */
    val bodyMedium: TextStyle,
    /** Основной текст (16 sp) */
    val bodyLarge: TextStyle,
    /** Подзаголовок средней жирности (16 sp SemiBold) */
    val headline: TextStyle,
    /** Вспомогательный подзаголовок (15 sp) */
    val subhead: TextStyle,
    /** Вспомогательный подзаголовок средней жирности (15 sp Medium) */
    val subheadMedium: TextStyle,
    /** Крупный заголовок экрана (32 sp) */
    val largeTitle: TextStyle,
    /** Кнопка подтверждения / действия (17 sp SemiBold) */
    val button: TextStyle,
    /** Название проекта в строке главного экрана (19 sp Regular) */
    val listItem: TextStyle,
    /** Заголовок карточки настроек на главном экране (14 sp SemiBold) */
    val settingsTitle: TextStyle,
    /** Текст кнопки действия в карточке настроек (14 sp Bold) */
    val settingsAction: TextStyle,
    /** Название в строке главного экрана: раздел, проект, область (19 sp Medium) */
    val listTitle: TextStyle,
    /** Эмодзи перед крупным названием проекта или области (30 sp) */
    val heroEmoji: TextStyle,
    /** Надпись капителью над группой: «PROJECTS», «TASKS», «NEW HEADING» (11 sp Bold, разрядка 1 sp) */
    val overline: TextStyle,
)

internal fun thingsTypography(scale: Float = 1f) = ThingsTypography(
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
    topAppBarTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = (20 * scale).sp),
    caption = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp),
    tagChip = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp),
    badge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = (11 * scale).sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (13 * scale).sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (16 * scale).sp),
    headline = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = (16 * scale).sp),
    subhead = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (15 * scale).sp),
    subheadMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = (15 * scale).sp),
    largeTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = (32 * scale).sp),
    button = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = (17 * scale).sp),
    listTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = (19 * scale).sp),
    listItem = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (19 * scale).sp),
    settingsTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp),
    settingsAction = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp),
    heroEmoji = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = (30 * scale).sp),
    overline = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = (11 * scale).sp, letterSpacing = 1.sp),
)

/** Скругления по ролям. */
@Immutable
data class ThingsShapes(
    /** Полоска-индикатор (1.5 dp) */
    val indicator: Dp = 1.5.dp,
    /** Ручка перетаскивания шторки (2 dp) */
    val tiny: Dp = 2.dp,
    /** Бейдж даты, метка (4 dp) */
    val badge: Dp = 4.dp,
    /** Мелкий чип, иконка действия (6 dp) */
    val small: Dp = 6.dp,
    /** Строка списка, строка в диалоге, раскрытый редактор (8 dp) */
    val row: Dp = 8.dp,
    /** Чип, плашка, мелкая карточка (10 dp) */
    val chip: Dp = 10.dp,
    /** Фильтр тегов, карточка деталей (12 dp) */
    val tagFilter: Dp = 12.dp,
    /** Карточка настроек (14 dp) */
    val card: Dp = 14.dp,
    /** Всплывающее меню-карточка: меню «+» на главном экране (16 dp) */
    val menu: Dp = 16.dp,
    /** Кнопка действия (18 dp) */
    val button: Dp = 18.dp,
    /** Капсула поиска, пилюля (22 dp) */
    val capsule: Dp = 22.dp,
    /** Плавающая панель действий внизу экрана — капсула высотой 50 dp (25 dp) */
    val toolbar: Dp = 25.dp,
    /** Плавающая карточка, вырастающая из своего источника: Quick Find, Quick Add (30 dp) */
    val floatingCard: Dp = 30.dp,
    /** Модальный диалог: Move, теги, When, удаление (32 dp) */
    val dialog: Dp = 32.dp,
) {
    val indicatorShape get() = RoundedCornerShape(indicator)
    val tinyShape get() = RoundedCornerShape(tiny)
    val badgeShape get() = RoundedCornerShape(badge)
    val smallShape get() = RoundedCornerShape(small)
    val rowShape get() = RoundedCornerShape(row)
    val chipShape get() = RoundedCornerShape(chip)
    val tagFilterShape get() = RoundedCornerShape(tagFilter)
    val cardShape get() = RoundedCornerShape(card)
    val menuShape get() = RoundedCornerShape(menu)
    val buttonShape get() = RoundedCornerShape(button)
    val capsuleShape get() = RoundedCornerShape(capsule)
    val toolbarShape get() = RoundedCornerShape(toolbar)
    val floatingCardShape get() = RoundedCornerShape(floatingCard)
    val dialogShape get() = RoundedCornerShape(dialog)
}

private val LocalThingsColors = staticCompositionLocalOf { LightThingsColors }
private val LocalThingsTypography = staticCompositionLocalOf { thingsTypography(1f) }
private val LocalThingsShapes = staticCompositionLocalOf { ThingsShapes() }

/**
 * Подаёт роли всем компонентам под ним. Вызывается из MyApplicationTheme, который передаёт
 * тот же флаг [darkTheme], что и в MaterialTheme, — так обе темы всегда совпадают.
 */
@Composable
fun ProvideThingsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scale = if (LocalConfiguration.current.screenWidthDp >= 600) 1.25f else 1f
    CompositionLocalProvider(
        LocalThingsColors provides if (darkTheme) DarkThingsColors else LightThingsColors,
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
