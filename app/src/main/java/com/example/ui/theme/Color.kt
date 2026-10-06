package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Things iOS dynamic palette
val ThingsBlue = Color(0xFF5B9AFF)
// Глубокий синий: подзаголовки проектов и их меню, иконки проектов
val ThingsDeepBlue = Color(0xFF2A62D9)
val ThingsInboxBlue = Color(0xFF1B80FA)
val ThingsTodayStar = Color(0xFFE9AC10)
val ThingsUpcomingRed = Color(0xFFF35F50)
val ThingsAnytimeTeal = Color(0xFF2EB7CD)
val ThingsSomedayGrey = Color(0xFF8F93A3)
val ThingsAreaGreenLight = Color(0xFF269C6E)
val ThingsAreaGreenDark = Color(0xFF7AD0A7)
val ThingsSwipeWhenYellow = Color(0xFFFFD401)

// Quick Find search match highlight (themes-ios.json)
val SearchMatchHighlightLight = Color(0xFFFFE692)
val SearchMatchHighlightDark = Color(0xFF6E5D01)

// Light Mode Theme
val ThingsBackgroundLight = Color(0xFFFFFFFF)
val ThingsSurfaceLight = Color(0xFFF4F5F7)
val ThingsTextPrimaryLight = Color(0xFF000000)
val ThingsTextSecondaryLight = Color(0xFF747679)
val ThingsTextNotesLight = Color(0xFF424242)
val ThingsDividerLight = Color(0xFFE4E4E6)

// Dark Mode Theme (Classic Slate & Black)
val ThingsBackgroundDark = Color(0xFF000000)
val ThingsSurfaceDark = Color(0xFF18191B)
val ThingsTextPrimaryDark = Color(0xFFECECED)
val ThingsTextSecondaryDark = Color(0xFF8B8C8E)
val ThingsDividerDark = Color(0xFF28292B)

// Цвета компонентов. Раньше были прописаны прямо в компонентах; значения не менялись.

// ── Нейтральные серые компонентов (значения системной палитры iOS, как в Things 3) ──
/** Текст раскрытого редактора и Quick Add; карточка Quick Find (фон в тёмной теме, текст в светлой) */
val ThingsInk = Color(0xFF1C1C1E)
/** Плейсхолдеры («New To-Do», «Notes»), вторичный текст Quick Find и диалогов */
val ThingsMetaGrey = Color(0xFF8E8E93)
/** Рамка неотмеченного чекбокса, неактивные иконки панели Quick Add и редактора */
val ThingsCheckboxBorder = Color(0xFFC7C7CC)
/** Неактивные иконки нижней строки раскрытого редактора */
val ThingsEditorIconInactive = Color(0xFFA9A9A9)
/** Фон полей поиска и ввода, светлая тема */
val ThingsFieldLight = Color(0xFFF2F2F7)
/** Фон полей поиска и ввода, тёмная тема */
val ThingsFieldDark = Color(0xFF2C2C2E)
/** Обводка карточек стопки, фон кнопки ✕ в Quick Find, разделитель Quick Add — светлая тема */
val ThingsHairlineLight = Color(0xFFE5E5EA)
/** Обводка карточек стопки — тёмная тема */
val ThingsHairlineDark = Color(0xFF38393D)
/** Приглушённый текст и полоски виджета календаря, светлая тема */
val ThingsMutedGreyLight = Color(0xFFD1D1D6)
/** Приглушённый текст и полоски виджета календаря, тёмная тема */
val ThingsMutedGreyDark = Color(0xFF48484A)

// ── Перетаскивание ──
/** Плашка места вставки (рисуется с прозрачностью 50 %), светлая тема */
val ThingsDropPlaceholderLight = Color(0xFFE5E6EB)
/** Плашка места вставки, тёмная тема */
val ThingsDropPlaceholderDark = Color(0xFF2C2D32)
/** Плашка места вставки нового проекта на главном экране */
val ThingsHomeDropPlaceholder = Color(0xFFE9EAEE)
/** Нижние карточки стопки при групповом перетаскивании, тёмная тема (в светлой — белые) */
val ThingsStackCardDark = Color(0xFF252629)
/** Пунктир места нового подзаголовка при перетаскивании «+» */
val ThingsNewHeadingLine = Color(0xFFC9CBD1)
/** Надпись «NEW HEADING» на месте нового подзаголовка */
val ThingsNewHeadingText = Color(0xFFB4B6BC)

// ── Тёмные диалоги, меню и панели (в обеих темах) ──
/** Фон диалогов Move, Delete, When, тегов, меню подзаголовка и «•••» */
val ThingsDialogBackground = Color(0xFF22242C)
/** Кнопки и поля внутри тёмных диалогов (Move, теги) */
val ThingsDialogButton = Color(0xFF2C2E38)
/** Подсветка выбранной строки в диалоге Move */
val ThingsDialogRowSelected = Color(0xFF1E2027)
/** Кнопка «Clear» диалога When */
val ThingsWhenClear = Color(0xFFE22D5A)

// ── Теги и маркеры задачи ──
/** Фон чипа тега в редакторе и Quick Add */
val ThingsTagChipBackground = Color(0xFFD1EAE2)
/** Текст чипа тега */
val ThingsTagChipText = Color(0xFF2C7D64)
/** Значок «вечер» в строке задачи */
val ThingsEveningIndicator = Color(0xFF2196F3)
/** Текст бейджа в строке задачи (светлая тема); иконка календаря в диалоге When */
val ThingsBadgeTextLight = Color(0xFF5F6368)
/** Текст бейджа в строке задачи, тёмная тема */
val ThingsBadgeTextDark = Color(0xFFE0E0E0)
/** Фон бейджа в строке задачи, светлая тема (в тёмной — ThingsFieldDark) */
val ThingsBadgeBackgroundLight = Color(0xFFECECEC)
/** Контур анимированной иконки области */
val ThingsAreaIconStroke = Color(0xFF9C9C9C)
/** Цвет события календаря, если у календаря нет своего */
val ThingsCalendarDefaultGreen = Color(0xFF63C655)

// ── Оттяжка поиска (pull-to-search) ──
/** Круг индикатора оттяжки, светлая тема (в тёмной — ThingsFieldDark) */
val ThingsPullIndicatorLight = Color(0xFF8E929C)
/** Стрелка индикатора оттяжки, светлая тема */
val ThingsPullArrowLight = Color(0x50000B1B)
/** Стрелка индикатора оттяжки, тёмная тема */
val ThingsPullArrowDark = Color(0x4BE7F1FF)
/** Стрелка после порога срабатывания, тёмная тема (в светлой — ThingsBlue) */
val ThingsPullArrowSelectedDark = Color(0xFF3F85F4)
/** Фон списка, пока раскрыта задача, светлая тема */
val ThingsListDimLight = Color(0xFFF4F4F6)
/** Фон списка, пока раскрыта задача, тёмная тема */
val ThingsListDimDark = Color(0xFF151618)

// ── Чек-лист в редакторе (сняты с видео Things 3) ──
/** Кружок отметки пункта чек-листа */
val ThingsChecklistCircle = Color(0xFF4A7DF2)
/** Галочка пункта */
val ThingsChecklistCheck = Color(0xFF888888)
/** Ручка ≡ перетаскивания пункта */
val ThingsChecklistHandle = Color(0xFFB5B5B5)
/** Удаление пункта свайпом */
val ThingsChecklistDelete = Color(0xFFEE004E)
/** Разделители пунктов */
val ThingsChecklistDivider = Color(0xFFEBEBEB)
/** Подсветка пункта при нажатии */
val ThingsChecklistHighlight = Color(0xFFF0F1F3)
/** Текст выполненного пункта */
val ThingsChecklistCompletedText = Color(0xFF777778)
/** Пункт, который правят */
val ThingsChecklistFocus = Color(0xFFF7F7F7)
/** Пункт, который тащат */
val ThingsChecklistDrag = Color(0xFFD7E6FD)

// Предустановленные цвета календарей (Calendar color presets)
val CalendarGreen = Color(0xFF34A853) // Google Зеленый
val CalendarBlue = Color(0xFF4285F4)  // Google Синий
val CalendarYellow = Color(0xFFFBBC05)// Google Желтый
val CalendarRed = Color(0xFFEA4335)   // Google Красный
val CalendarPurple = Color(0xFF8E24AA)// Фиолетовый
val CalendarPink = Color(0xFFF06292)  // Розовый
val CalendarTeal = Color(0xFF00ACC1)  // Голубой / Бирюзовый

