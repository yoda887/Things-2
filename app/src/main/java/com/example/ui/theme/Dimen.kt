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

    // Общие размеры
    /** Строка списка: задача, проект, раздел, строка диалога */
    val rowHeight: Dp = 46.dp,
    /** Минимальная зона нажатия кнопки-иконки */
    val minTouchTarget: Dp = 48.dp,
    /** Круглые кнопки в шапке диалога и окна (закрыть, назад) */
    val dialogHeaderButtonSize: Dp = 36.dp,
    /** Подзаголовок проекта — высота строки */
    val headingRowHeight: Dp = 40.dp,

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
    /**
     * Иконка «Входящих» на главном экране: она залита плотнее остальных (сплошной синий лоток) и при том же
     * размере кажется крупнее — рисуется чуть меньше внутри той же рамки ThingsIconSize.L
     */
    val homeInboxIconSize: Dp = 19.dp,
    /** Промежуток между «Входящими» и «Сегодня» — шире, чем между остальными умными списками */
    val homeInboxGap: Dp = 20.dp,
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
    val homeSyncInnerGap: Dp = 10.dp,

    // Quick Find
    /** Карточка от краёв экрана и поле ввода внутри неё */
    val quickFindCardMargin: Dp = 14.dp,
    /** Отступ карточки сверху и снизу и шапки до поля */
    val quickFindCardPadding: Dp = 20.dp,
    val quickFindCardMaxWidth: Dp = 480.dp,
    /** Поле ввода, кнопка ✕ и строка результата */
    val quickFindRowHeight: Dp = 44.dp,
    val quickFindResultsMaxHeight: Dp = 350.dp,
    /** Промежуток между заголовком раздела и разделителем в результатах */
    val quickFindSectionGap: Dp = 6.dp,
    /** Подсказка пустого окна — отступ сверху */
    val quickFindHintTop: Dp = 18.dp,

    // Окна-диалоги
    /** Окно тегов по эталону Things: доля ширины и высоты экрана, по центру */
    val dialogWidthFraction: Float = 0.89f,
    val dialogHeightFraction: Float = 0.67f,
    /** Однострочное поле ввода в окне (название тега) — залитая плашка без рамки */
    val dialogFieldHeight: Dp = 40.dp,
    /** Маленькие круглые кнопки в строке окна (изменить, удалить) */
    val dialogRowButtonSize: Dp = 28.dp,
    /** Круглая кнопка очистки поля ввода и крестик в ней — в Quick Find и в окне тегов */
    val clearBadgeSize: Dp = 18.dp,
    val clearBadgeIconSize: Dp = 11.dp
)

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }

val MaterialTheme.dimens: AppDimens
    @Composable
    @ReadOnlyComposable
    get() = LocalAppDimens.current
