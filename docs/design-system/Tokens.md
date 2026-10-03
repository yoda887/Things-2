# Токены

Сгенерировано из `tokens.json`. Значения в формате «светлая / тёмная тема»; одно значение — одинаково в обеих.
Каждый цвет объявлен в `app/src/main/java/com/example/ui/theme/Color.kt` под именем из колонки «Kotlin».

## Цвета

| Токен | Значение | Kotlin | Где используется |
| --- | --- | --- | --- |
| `bg` | `#ffffff` / `#000000` | `ThingsBackgroundLight / ThingsBackgroundDark` | Фон экранов и списков. На нём `text-primary` и `text-secondary`. |
| `surface` | `#f4f5f7` / `#18191b` | `ThingsSurfaceLight / ThingsSurfaceDark` | Второй уровень поверхностей: Material surface, фон перетаскиваемой карточки. |
| `text-primary` | `#000000` / `#ececed` | `ThingsTextPrimaryLight / ThingsTextPrimaryDark` | Названия задач, проектов, заголовки экранов на `bg` и `surface`. Контраст 21:1 / 17:1. |
| `text-secondary` | `#747679` / `#8b8c8e` | `ThingsTextSecondaryLight / ThingsTextSecondaryDark` | Подписи под задачей (название проекта), даты, пояснения на `bg`. 4.6:1 в светлой теме, 6.2:1 в тёмной. |
| `text-notes` | `#424242` | `ThingsTextNotesLight` | Текст заметок в раскрытом редакторе. Редактор всегда светлый (белая карточка), поэтому тёмного значения нет. |
| `divider` | `#e4e4e6` / `#28292b` | `ThingsDividerLight / ThingsDividerDark` | Разделители строк и линия под подзаголовками (0.6–1 dp). |
| `accent` | `#5b9aff` | `ThingsBlue` | Главный акцент: кнопка «+», отмеченный чекбокс, курсор, выделение, фон свайпа влево. Как цвет текста на `bg` даёт 2.8:1 — для мелкого текста не годится, только для заливок, иконок и крупных элементов. |
| `accent-selection` | `rgba(91, 154, 255, 0.22)` | `ThingsBlue.copy(alpha = 0.22f)` | Фон выбранной строки в режиме мультивыбора: `accent` с прозрачностью 22 %. |
| `deep-blue` | `#2a62d9` | `ThingsDeepBlue` | Подзаголовки проекта и их «•••», иконки-прогресс проектов. 5.5:1 на белом; на чёрном фоне тёмной темы 3.8:1 — допустимо только для крупного полужирного текста (подзаголовок 19 sp SemiBold). |
| `inbox-blue` | `#1b80fa` | `ThingsInboxBlue` | Раздел «Входящие». |
| `today-star` | `#e9ac10` | `ThingsTodayStar` | Раздел «Сегодня»: звезда-маркер в строке задачи и редакторе. Иконка раздела в боковом списке рисуется своим цветом `#fecb2f` (см. ассеты). |
| `upcoming-red` | `#f35f50` | `ThingsUpcomingRed` | Раздел «Предстоящие» и опасные действия: «Delete» в меню подзаголовка и в диалоге удаления. На `bg` как текст — 3.2:1, только для крупного текста или на тёмном `dialog-bg`. |
| `anytime-teal` | `#2eb7cd` | `ThingsAnytimeTeal` | Раздел «В любое время»; иконка тега в поле тегов редактора. |
| `someday-grey` | `#8f93a3` | `ThingsSomedayGrey` | Раздел «Когда-нибудь», иконки тегов в списках и поиске, Material secondary. |
| `logbook-green` | `#2ec275` | `ThingsLogbookGreen` | Раздел «Журнал». |
| `area-green` | `#269c6e` / `#7ad0a7` | `ThingsAreaGreen (Light / Dark)` | Иконка области (Area) и её название в поиске; в тёмной теме светлее, чтобы читаться на чёрном. |
| `when-yellow` | `#ffd401` | `ThingsSwipeWhenYellow` | Фон свайпа вправо (действие «Когда»). |
| `search-highlight` | `#ffe692` / `#6e5d01` | `SearchMatchHighlight (Light / Dark)` | Подсветка совпадения в Quick Find под `text-primary`. |
| `checkbox-border` | `#c7c7cc` | `ThingsCheckboxBorder` | Рамка неотмеченного чекбокса, неактивные иконки панели редактора. Как граница на `bg` даёт 1.7:1 — ниже 3:1, оставлен как в источнике. |
| `editor-text` | `#1c1c1e` | `ThingsInk` | Название задачи и пункты чек-листа в раскрытом редакторе (на белой карточке); карточка Quick Find в светлой теме. |
| `meta-grey` | `#8e8e93` | `ThingsMetaGrey` | Плейсхолдеры («New To-Do»), вторичный текст Quick Find, приоритет 0. 3.3:1 на белом — только для подсказок, не для смыслового текста. |
| `field-bg` | `#f2f2f7` / `#2c2c2e` | `ThingsFieldLight / ThingsFieldDark` | Фон полей поиска (Quick Find, Search), капсулы на главном. |
| `drop-placeholder` | `#e5e6eb` / `#2c2d32` | `ThingsDropPlaceholderLight / ThingsDropPlaceholderDark` | Серая плашка места вставки при перетаскивании задачи, подзаголовка и «+»; рисуется с прозрачностью 50 %. |
| `hairline` | `#e5e5ea` / `#38393d` | `ThingsHairlineLight / ThingsHairlineDark` | Обводка нижних карточек «стопки» при перетаскивании (0.5 dp), фон кнопки ✕ в Quick Find, разделитель Quick Add. |
| `dialog-bg` | `#22242c` | `ThingsDialogBackground` | Тёмные диалоги и меню в обеих темах: Move, Delete, меню подзаголовка, меню «•••». Текст на нём белый (15:1). |
| `tag-chip-bg` | `#d1eae2` | `ThingsTagChipBackground` | Фон чипа тега в редакторе и Quick Add. |
| `tag-chip-text` | `#2c7d64` | `ThingsTagChipText` | Текст чипа тега на `tag-chip-bg` (3.9:1 — ниже 4.5:1 для мелкого текста, как в источнике). |
| `editor-icon-inactive` | `#a9a9a9` | `ThingsEditorIconInactive` | Неактивные иконки нижней строки раскрытого редактора. |
| `muted-grey` | `#d1d1d6` / `#48484a` | `ThingsMutedGreyLight / ThingsMutedGreyDark` | Приглушённый текст и полоски виджета календаря. |
| `list-dim` | `#f4f4f6` / `#151618` | `ThingsListDimLight / ThingsListDimDark` | Фон списка, пока раскрыта задача (плавно, вместе с затемнением строк). |
| `stack-card` | `#ffffff` / `#252629` | `ThingsBackgroundLight / ThingsStackCardDark` | Нижние карточки стопки при групповом перетаскивании. |
| `home-drop-placeholder` | `#e9eaee` | `ThingsHomeDropPlaceholder` | Плашка места вставки нового проекта на главном экране. |
| `new-heading-line` | `#c9cbd1` | `ThingsNewHeadingLine` | Пунктир места нового подзаголовка при перетаскивании «+». |
| `new-heading-text` | `#b4b6bc` | `ThingsNewHeadingText` | Надпись «NEW HEADING» на месте нового подзаголовка. |
| `dialog-button` | `#2c2e38` | `ThingsDialogButton` | Кнопки и поля внутри тёмных диалогов (Move, теги) на `dialog-bg`. |
| `dialog-row-selected` | `#1e2027` | `ThingsDialogRowSelected` | Подсветка выбранной строки в диалоге Move. |
| `dialog-close` | `#181a1d` | `ThingsDialogCloseButton` | Кнопка закрытия диалога When. |
| `menu-bg` | `#23252e` | `ThingsMenuBackground` | Выпадающее меню «+» на главном экране (задача / проект / область). |
| `toolbar-bg` | `#232329` | `ThingsToolbarBackground` | Плавающая панель действий для раскрытой задачи и для выбранных; белые иконки на ней — 15:1. |
| `fab-action` | `#3a3b40` | `ThingsFabActionButton` | Кружки «отмена» и «во Входящие» при перетаскивании «+». |
| `when-secondary` | `#6c6f7d` | `ThingsWhenSecondary` | Дни недели и вторичные иконки диалога When на `dialog-bg` (3.0:1). |
| `when-clear` | `#e22d5a` | `ThingsWhenClear` | Кнопка «Clear» диалога When, белый текст. |
| `evening-indicator` | `#2196f3` | `ThingsEveningIndicator` | Значок «вечер» в строке задачи. |
| `badge-text` | `#5f6368` / `#e0e0e0` | `ThingsBadgeTextLight / ThingsBadgeTextDark` | Текст бейджа в строке задачи; иконка календаря в диалоге When. |
| `badge-bg` | `#ececec` / `#2c2c2e` | `ThingsBadgeBackgroundLight / ThingsFieldDark` | Фон бейджа в строке задачи. |
| `area-icon-stroke` | `#9c9c9c` | `ThingsAreaIconStroke` | Контур анимированной иконки области. |
| `calendar-default` | `#63c655` | `ThingsCalendarDefaultGreen` | Цвет события, если у календаря нет своего. |
| `pull-indicator` | `#8e929c` / `#2c2c2e` | `ThingsPullIndicatorLight / ThingsFieldDark` | Круг индикатора оттяжки поиска. |
| `pull-arrow` | `rgba(0, 11, 27, 0.31)` / `rgba(231, 241, 255, 0.29)` | `ThingsPullArrowLight / ThingsPullArrowDark` | Стрелка индикатора оттяжки до порога. |
| `pull-arrow-selected` | `#5b9aff` / `#3f85f4` | `ThingsBlue / ThingsPullArrowSelectedDark` | Стрелка после порога срабатывания. |
| `checklist-circle` | `#4a7df2` | `ThingsChecklistCircle` | Кружок отметки пункта чек-листа. |
| `checklist-check` | `#888888` | `ThingsChecklistCheck` | Галочка пункта чек-листа. |
| `checklist-handle` | `#b5b5b5` | `ThingsChecklistHandle` | Ручка ≡ перетаскивания пункта. |
| `checklist-delete` | `#ee004e` | `ThingsChecklistDelete` | Удаление пункта свайпом. |
| `checklist-divider` | `#ebebeb` | `ThingsChecklistDivider` | Разделители пунктов чек-листа. |
| `checklist-highlight` | `#f0f1f3` | `ThingsChecklistHighlight` | Пункт при нажатии. |
| `checklist-completed` | `#777778` | `ThingsChecklistCompletedText` | Текст выполненного пункта. |
| `checklist-focus` | `#f7f7f7` | `ThingsChecklistFocus` | Пункт, который правят. |
| `checklist-drag` | `#d7e6fd` | `ThingsChecklistDrag` | Пункт, который тащат. |
| `calendar-green` | `#34a853` | `CalendarGreen` | Пресет цвета календаря (полоска события на «Сегодня»). |
| `calendar-blue` | `#4285f4` | `CalendarBlue` | Пресет цвета календаря. |
| `calendar-yellow` | `#fbbc05` | `CalendarYellow` | Пресет цвета календаря. |
| `calendar-red` | `#ea4335` | `CalendarRed` | Пресет цвета календаря. |
| `calendar-purple` | `#8e24aa` | `CalendarPurple` | Пресет цвета календаря. |
| `calendar-pink` | `#f06292` | `CalendarPink` | Пресет цвета календаря. |
| `calendar-teal` | `#00acc1` | `CalendarTeal` | Пресет цвета календаря. |

## Типографика

Шрифт: Roboto, system-ui, -apple-system, sans-serif (`FontFamily.Default`). На экранах шире 600 dp размеры, кроме `settings-header`, умножаются на 1.25 (`Type.kt`).

### Экраны

| Стиль | Размер | Насыщенность | Где используется |
| --- | --- | --- | --- |
| `hero-title` | 32 sp | 700 | Крупное название экрана над списком: «Сегодня», название проекта (displayLarge). |
| `hero-emoji` | 30 sp | 400 | Эмодзи перед названием проекта или области (displayMedium). |
| `top-bar-title` | 20 sp | 600 | Маленькое название в верхнем тулбаре после прокрутки (topAppBarTitle). |
| `settings-header` | 24 sp | 700 | Заголовок настроек и групп (headlineLarge). |

### Подзаголовки

| Стиль | Размер | Насыщенность | Где используется |
| --- | --- | --- | --- |
| `section-header` | 19 sp | 500 | Подзаголовки секций: «Сегодня вечером», «Планы», «Когда-нибудь» на экране области (displaySmall). |
| `project-heading` | 19 sp | 600 | Подзаголовок внутри проекта, цвет `deep-blue`. |
| `upcoming-day-number` | 32 sp | 700 | Число дня в «Предстоящих» (32 sp Bold). |
| `dialog-title` | 22 sp | 700 | Заголовок нижнего листа и диалога (headlineMedium). |
| `dialog-subheader` | 18 sp | 600 | Подзаголовок диалога, «When?» (headlineSmall). |
| `title-large` | 16 sp | 500 | Заголовок раздела внутри экрана (titleLarge). |

### Задача

| Стиль | Размер | Насыщенность | Где используется |
| --- | --- | --- | --- |
| `task-title` | 19 sp | 400 | Название задачи в строке списка и в редакторе (titleMedium). |
| `task-subtitle` | 15 sp | 400 | Подпись под названием: проект, дата (bodySmall), цвет `text-secondary`. |
| `editor-notes` | 18 sp | 400 | Заметки в раскрытом редакторе (taskEditorNotes), цвет `text-notes`. |
| `editor-checklist` | 17.2 sp | 400 | Пункты чек-листа в редакторе: заглавная ≈ 0,90 заглавной названия (taskEditorChecklist). |
| `editor-date` | 18 sp | 400 | Строка даты и дедлайна в нижней части редактора (taskEditorDate). |

### Текст и подписи

| Стиль | Размер | Насыщенность | Где используется |
| --- | --- | --- | --- |
| `body-large` | 18 sp | 400 | Поля ввода, основной текст деталей (bodyLarge). |
| `body-medium` | 16 sp | 400 | Вспомогательный текст, описание проекта, теги на главном (bodyMedium). |
| `title-small` | 15 sp | 400 | Заголовок листа, альтернативный подзаголовок (titleSmall). |
| `label-large` | 18 sp | 400 | Текст кнопок действий диалогов: Cancel, Delete, Clear, Move (labelLarge, ThingsTheme.type.dialogButton). |
| `label-medium` | 12 sp | 500 | Мелкие метки, бейджи (labelMedium). |
| `label-small` | 11 sp | 700 | Самые мелкие бейджи, приоритет (labelSmall). |

## Отступы (`Dimen.kt`, dp)

| Токен | Значение | Где используется |
| --- | --- | --- |
| `space-row-start` | `10 dp` | Отступ слева внутри строки задачи (taskRowStartPadding); список целиком отстоит от края экрана ещё на 10 dp. |
| `space-row-end` | `8 dp` | Отступ справа внутри строки задачи (taskRowEndPadding). |
| `space-checkbox-to-text` | `8 dp` | От чекбокса до названия задачи (taskSpacingToTextDefault). |
| `space-list-gutter` | `10 dp` | Боковые поля списка задач от края экрана. |
| `space-task-top` | `13 dp` | Верхний отступ свёрнутой строки задачи (taskCollapsedTopPadding). |
| `space-task-bottom` | `13 dp` | Нижний отступ свёрнутой строки задачи (taskCollapsedBottomPadding). |
| `space-editor-top` | `20 dp` | Верхний отступ раскрытого редактора (taskExpandedTopPadding). |
| `space-editor-bottom` | `16 dp` | Нижний отступ раскрытого редактора (taskExpandedBottomPadding). |
| `space-editor-gap` | `42 dp` | Зазор над и под раскрытой задачей — соседи раздвигаются на эту величину (taskExpandedVerticalGap). |
| `space-editor-title-notes` | `10 dp` | Между названием и заметками в редакторе (taskExpandedTitleNotesGap). |
| `space-editor-start` | `12 dp` | Левый отступ контента раскрытого редактора (taskEditorExpandedStartPadding). |
| `space-editor-end` | `18 dp` | Правый отступ контента раскрытого редактора (taskEditorExpandedEndPadding), по эталону Things 3. |
| `space-editor-indicators-start` | `24 dp` | Начало строки тегов, даты и дедлайна внизу редактора. |
| `space-editor-icons` | `16 dp` | Между иконками действий в панели редактора и Quick Add. |
| `space-editor-checklist-toolbar` | `30 dp` | От чек-листа до панели действий в редакторе. |
| `space-header-top` | `24 dp` | Над крупным названием экрана (mainHeaderPaddingTop). |
| `space-header-bottom` | `25 dp` | Под крупным названием экрана (mainHeaderPaddingBottom). |
| `space-section` | `32 dp` | Перед секцией «Сегодня вечером» и разделами области (eveningSectionSpacing). |
| `space-heading-top` | `22 dp` | Над подзаголовком проекта. |
| `space-heading-bottom` | `4 dp` | Под подзаголовком проекта. |
| `space-calendar-section` | `25 dp` | Между блоком событий календаря и задачами. |
| `space-tags-section` | `11 dp` | Между фильтром тегов и задачами. |
| `space-fab-edge` | `16 dp` | От кнопки «+» до правого и нижнего края (слот Scaffold, Material 3). |
| `space-toolbar-bottom` | `24 dp` | От плавающей панели действий до низа экрана. |
| `space-dialog-inner` | `16 dp` | Внутренние поля диалогов. |
| `space-list-bottom` | `72 dp` | Распорка в конце списка, чтобы последняя задача не пряталась под «+». |

## Радиусы

| Токен | Значение | Где используется |
| --- | --- | --- |
| `radius-task` | `8px` | Строка задачи при перетаскивании, раскрытый редактор, фон свайпа, плашка места вставки. |
| `radius-checkbox` | `23%` | Скругление чекбокса — 23 % его стороны (3.7 px у основного 16 dp). |
| `radius-heading` | `10px` | Поднятый подзаголовок при перетаскивании и его стопка. |
| `radius-dialog` | `32px` | Диалоги: When, Move, теги, Delete (dialogRoundedCornerSize, ThingsTheme.shapes.dialogShape). |
| `radius-floating-card` | `30px` | Плавающие карточки: Quick Add, Quick Find (ThingsTheme.shapes.floatingCardShape). |
| `radius-toolbar` | `25px` | Плавающая панель «Move / Delete / •••» — капсула высотой 50 dp. |
| `radius-field` | `22px` | Поле поиска — капсула высотой 44 dp. |
| `radius-full` | `50%` | Кнопка «+», кружки отмены и «во Входящие», иконка-прогресс проекта. |

## Размеры элементов (dp)

| Токен | Значение | Где используется |
| --- | --- | --- |
| `size-checkbox` | `16 dp` | Основной чекбокс задачи (mainCheckboxSize). |
| `size-fab` | `56 dp` | Кнопка «+». |
| `size-fab-action` | `44 dp` | Кружки «отмена» и «во Входящие» при перетаскивании «+». |
| `size-row-min` | `46 dp` | Высота свёрнутой строки задачи (EDITOR_COLLAPSED_HEIGHT); оценка с подписью — 56 dp. |
| `size-drop-gap` | `44 dp` | Высота плашки места вставки при перетаскивании «+». |
| `size-heading-row` | `40 dp` | Высота строки подзаголовка проекта (без отступов). |
| `size-toolbar` | `50 dp` | Высота плавающей панели действий. |
| `size-search-field` | `44 dp` | Высота поля поиска. |
| `size-project-arc` | `20 dp` | Иконка-прогресс проекта в строке; в шапке экрана проекта — 26 dp. |
| `size-swipe-icon` | `22 dp` | Иконка на фоне свайпа. |
| `size-dialog-width` | `320 dp` | Ширина диалога. |

## Тени (shadowElevation)

| Токен | Значение | Где используется |
| --- | --- | --- |
| `elevation-fab` | `6dp` | Кнопка «+» в покое; при захвате растёт до `elevation-fab-lifted`. |
| `elevation-fab-lifted` | `18dp` | Кнопка «+», пока её тянут (6 + 12 × подъём). |
| `elevation-card` | `8dp` | Раскрытый редактор задачи и перетаскиваемая карточка. |
| `elevation-stack-2` | `6dp` | Второй слой стопки при групповом перетаскивании. |
| `elevation-stack-3` | `4dp` | Третий слой стопки. |
