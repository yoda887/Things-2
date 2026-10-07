package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AppDimens contains custom dimension values for the application layout.
 */
data class AppDimens(
    val mainCheckboxSize: Dp = 16.dp,
    val taskLeftColumnWidthDefault: Dp = 16.dp,
    val taskSpacingToTextDefault: Dp = 8.dp,
    val searchLeftColumnWidth: Dp = 28.dp,
    val searchSpacingToText: Dp = 8.dp,
    val taskItemEstimatedHeight: Dp = 56.dp,
    val mainHeaderPaddingTop: Dp = 24.dp,
    val dialogWidth: Dp = 320.dp,
    val dialogHeight: Dp = 480.dp,
    val dialogInnerContentPadding: Dp = 16.dp,
    val floatingToolbarHeight: Dp = 50.dp,
    val floatingToolbarBottomPadding: Dp = 24.dp,
    val eveningSectionSpacing: Dp = 32.dp,
    val mainHeaderPaddingBottom: Dp = 25.dp,
    val calendarBetweenSectionSpacing: Dp = 25.dp,
    val tagsBetweenSectionSpacing: Dp = 11.dp,
    val taskExpandedVerticalGap: Dp = 42.dp,
    val taskCollapsedTopPadding: Dp = 13.dp,
    val taskExpandedTopPadding: Dp = 20.dp,
    val taskCollapsedBottomPadding: Dp = 13.dp,
    val taskExpandedBottomPadding: Dp = 16.dp,
    val taskExpandedTitleNotesGap: Dp = 10.dp,
    val listBottomSpacerHeight: Dp = 72.dp,
    /** Горизонтальный отступ контента внутри карточки задачи (start padding Row в TaskItemRow) */
    val taskRowStartPadding: Dp = 10.dp,
    /** Горизонтальный отступ контента в полностью раскрытом inline-редакторе */
    val taskEditorExpandedStartPadding: Dp = 12.dp,
    /** Горизонтальный отступ начала нижних индикаторов в редакторе (теги, дата, дедлайн) */
    val taskEditorStartRelativePadding: Dp = 24.dp,
    /** Вертикальный отступ строки чипов тегов в редакторе */
    val taskEditorTagsVerticalPadding: Dp = 4.dp,
    /** Вертикальный отступ строки индикатора даты и дедлайна в редакторе */
    val taskEditorIndicatorVerticalPadding: Dp = 6.dp,
    /** Отступ между тегами и датой/дедлайном в редакторе (в сумме с паддингами дает зазор 16.dp) */
    val taskEditorTagsToDateSpacer: Dp = 6.dp,
    /** Отступ между датой и дедлайном в редакторе (в сумме с паддингами дает зазор 16.dp) */
    val taskEditorDateToDeadlineSpacer: Dp = 4.dp,
    /** Правый отступ контента внутри карточки задачи (end padding Row в TaskItemRow) */
    val taskRowEndPadding: Dp = 8.dp,
    /** Начальный правый отступ контента в редакторе (16.dp обеспечивает строго симметричное расширение контента из центра на 8dp влево и 8dp вправо) */
    val taskEditorCollapsedEndPadding: Dp = 16.dp,
    /** Правый отступ контента в полностью раскрытом inline-редакторе (18.dp по эталону Things 3; в сумме с ростом карточки вправо на 10dp сохраняет фиксированное положение правой границы контента 18dp от края экрана) */
    val taskEditorExpandedEndPadding: Dp = 18.dp,
    /** Расстояние между иконками действий в тулбаре раскрытой задачи и окне быстрого добавления */
    val taskEditorActionIconsSpacing: Dp = 16.dp,
    /** Отступ между чек-листом и тулбаром действий в раскрытом редакторе задачи (Things 3 эталон) */
    val taskEditorChecklistToToolbarSpacer: Dp = 30.dp,

    // Главный экран
    /** Поле списка главного экрана от краёв экрана */
    val homeListGutter: Dp = 14.dp,
    /** Строка раздела, проекта, области и плашка места вставки */
    val homeRowHeight: Dp = 46.dp,
    /** Внутренний отступ строки и разделителей главного экрана */
    val homeRowInset: Dp = 6.dp,
    /** Иконка строки (прогресс проекта, значок области) — до названия */
    val homeRowIconSize: Dp = 20.dp,
    val homeRowIconTextGap: Dp = 7.dp,
    /** Значок области — высота (ширина homeRowIconSize) */
    val homeAreaIconHeight: Dp = 28.dp,
    /** Промежуток между разделами Входящие / Сегодня / … */
    val homeSmartListSpacing: Dp = 10.dp,
    /** Отступ до и после разделителя перед областью */
    val homeAreaDividerGap: Dp = 13.dp,
    /** Кнопка сворачивания области */
    val homeAreaToggleSize: Dp = 44.dp,
    /** Поле Quick Find на главном экране и блок над списком под него */
    val homeSearchFieldHeight: Dp = 44.dp,
    val homeSearchBlockHeight: Dp = 68.dp,
    /** Карточка синхронизации Google Tasks */
    val homeSyncCardTopPadding: Dp = 18.dp,
    val homeSyncInnerGap: Dp = 10.dp
)

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }

val MaterialTheme.dimens: AppDimens
    @Composable
    @ReadOnlyComposable
    get() = LocalAppDimens.current
