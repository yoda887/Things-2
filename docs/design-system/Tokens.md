# Токены

Спецификация токенов дизайн-системы Things 2.
Все визуальные параметры в UI-компонентах используются исключительно через семантические роли `ThingsTheme.colors`, `ThingsTheme.type`, `ThingsTheme.shapes`.
Значения цветов приведены в формате «светлая / тёмная тема»; одно значение — одинаково в обеих темах.

## Цвета (`ThingsColors`)

| Токен | Значение | Роль в Kotlin | Где используется |
| --- | --- | --- | --- |
| `bg` | `#ffffff` / `#000000` | `ThingsTheme.colors.background` | Фон экранов и списков. На нём `textPrimary` и `textSecondary`. |
| `surface` | `#f4f5f7` / `#18191b` | `ThingsTheme.colors.surface` | Второй уровень поверхностей: Material surface, фон перетаскиваемой карточки. |
| `text-primary` | `#000000` / `#ececed` | `ThingsTheme.colors.textPrimary` | Названия задач, проектов, заголовки экранов на `bg` и `surface`. Контраст 21:1 / 17:1. |
| `text-secondary` | `#747679` / `#8b8c8e` | `ThingsTheme.colors.textSecondary` | Подписи под задачей (название проекта), даты, пояснения на `bg`. 4.6:1 в светлой теме, 6.2:1 в тёмной. |
| `text-notes` | `#424242` | `ThingsTheme.colors.textNotes` | Текст заметок в раскрытом редакторе. Редактор всегда светлый (белая карточка), поэтому тёмного значения нет. |
| `divider` | `#e4e4e6` / `#28292b` | `ThingsTheme.colors.divider` | Разделители строк и линия под подзаголовками (0.6–1 dp). |
| `accent` | `#5b9aff` | `ThingsTheme.colors.accent` | Главный акцент: кнопка «+», отмеченный чекбокс, курсор, выделение, фон свайпа влево (`ThingsBlue`). |
| `accent-selection` | `rgba(91, 154, 255, 0.22)` | `ThingsTheme.colors.accentSelection` | Фон выбранной строки в режиме мультивыбора: `accent` с прозрачностью 22 %. |
| `deep-blue` | `#2a62d9` | `ThingsDeepBlue` | Подзаголовки проекта и их «•••», иконки-прогресс проектов. 5.5:1 на белом; на чёрном фоне тёмной темы 3.8:1 — допустимо для крупного полужирного текста (19 sp SemiBold). |
| `inbox-blue` | `#1b80fa` | `ThingsInboxBlue` | Раздел «Входящие». |
| `today-star` | `#e9ac10` | `ThingsTodayStar` | Раздел «Сегодня»: звезда-маркер в строке задачи и редакторе. Иконка раздела в боковом списке рисуется своим цветом `#fecb2f`. |
| `upcoming-red` | `#f35f50` | `ThingsTheme.colors.danger` | Раздел «Предстоящие» (`ThingsUpcomingRed`) и опасные действия: «Delete» в меню подзаголовка и в диалоге удаления. |
| `anytime-teal` | `#2eb7cd` | `ThingsAnytimeTeal` | Раздел «В любое время»; иконка тега в поле тегов редактора. |
| `someday-grey` | `#8f93a3` | `ThingsSomedayGrey` | Раздел «Когда-нибудь», иконки тегов в списках и поиске, Material secondary. |
| `area-green` | `#269c6e` / `#7ad0a7` | `ThingsAreaGreen` | Иконка области (Area) и её название в поиске; в тёмной теме светлее, чтобы читаться на чёрном. |
| `when-yellow` | `#ffd401` | `ThingsSwipeWhenYellow` | Фон свайпа вправо (действие «Когда»). |
| `search-highlight` | `#ffe692` / `#6e5d01` | `ThingsTheme.colors.searchMatchHighlight` | Подсветка совпадения в Quick Find под `textPrimary`. |
| `checkbox-border` | `#c7c7cc` | `ThingsTheme.colors.checkboxBorder` | Рамка неотмеченного чекбокса, неактивные иконки панели редактора. |
| `editor-text` | `#1c1c1e` | `ThingsTheme.colors.editorText` | Название задачи и пункты чек-листа в раскрытом редакторе (на белой карточке); карточка Quick Find в светлой теме (`ThingsInk`). |
| `meta-grey` | `#8e8e93` | `ThingsTheme.colors.overlayContentSecondary` | Плейсхолдеры («New To-Do»), вторичный текст Quick Find и диалогов (`ThingsMetaGrey`). |
| `field-bg` | `#f2f2f7` / `#2c2c2e` | `ThingsTheme.colors.searchField` | Фон полей поиска (Quick Find, Search), капсулы на главном. |
| `drop-placeholder` | `#e5e6eb` / `#2c2d32` | `ThingsTheme.colors.dropPlaceholder` | Серая плашка места вставки при перетаскивании задачи, подзаголовка и «+»; рисуется с прозрачностью 50 %. |
| `hairline` | `#e5e5ea` / `#38393d` | `ThingsTheme.colors.hairline` | Обводка нижних карточек «стопки» при перетаскивании (0.5 dp), фон кнопки ✕ в Quick Find, разделитель Quick Add. |
| `dialog-bg` | `#22242c` | `ThingsTheme.colors.overlaySurface` | Тёмные диалоги, плавающий тулбар и меню в обеих темах: Move, Delete, меню подзаголовка, меню «•••» (`ThingsDialogBackground`). Текст на нём белый (15:1). |
| `tag-chip-bg` | `#d1eae2` | `ThingsTheme.colors.tagChipBackground` | Фон чипа тега в редакторе и Quick Add. |
| `tag-chip-text` | `#2c7d64` | `ThingsTheme.colors.tagChipText` | Текст чипа тега на `tagChipBackground`. |
| `editor-icon-inactive` | `#a9a9a9` | `ThingsTheme.colors.editorIconInactive` | Неактивные иконки нижней строки раскрытого редактора. |
| `muted-grey` | `#d1d1d6` / `#48484a` | `ThingsTheme.colors.calendarMuted` | Приглушённый текст и полоски виджета календаря. |
| `list-dim` | `#f4f4f6` / `#151618` | `ThingsTheme.colors.listDim` | Фон списка, пока раскрыта задача (плавно, вместе с затемнением строк). |
| `stack-card` | `#ffffff` / `#252629` | `ThingsTheme.colors.stackCard` | Нижние карточки стопки при групповом перетаскивании. |
| `home-drop-placeholder` | `#e9eaee` | `ThingsHomeDropPlaceholder` | Плашка места вставки нового проекта на главном экране. |
| `new-heading-line` | `#c9cbd1` | `ThingsNewHeadingLine` | Пунктир места нового подзаголовка при перетаскивании «+». |
| `new-heading-text` | `#b4b6bc` | `ThingsNewHeadingText` | Надпись «NEW HEADING» на месте нового подзаголовка. |
| `dialog-button` | `#2c2e38` | `ThingsTheme.colors.overlayControl` | Кнопки и поля внутри тёмных диалогов (Move, теги) на `overlaySurface`. |
| `dialog-row-selected` | `#1e2027` | `ThingsTheme.colors.overlayRowSelected` | Подсветка выбранной строки в диалоге Move. |
| `when-clear` | `#e22d5a` | `ThingsWhenClear` | Кнопка «Clear» диалога When, белый текст. |
| `evening-indicator` | `#2196f3` | `ThingsEveningIndicator` | Значок «вечер» в строке задачи. |
| `badge-text` | `#5f6368` / `#e0e0e0` | `ThingsTheme.colors.badgeText` | Текст бейджа в строке задачи; иконка календаря в диалоге When. |
| `badge-bg` | `#ececec` / `#2c2c2e` | `ThingsTheme.colors.badgeBackground` | Фон бейджа в строке задачи. |
| `area-icon-stroke` | `#9c9c9c` | `ThingsAreaIconStroke` | Контур анимированной иконки области. |
| `calendar-default` | `#63c655` | `ThingsCalendarDefaultGreen` | Цвет события, если у календаря нет своего. |
| `pull-indicator` | `#8e929c` / `#2c2c2e` | `ThingsTheme.colors.pullIndicatorBackground` | Круг индикатора оттяжки поиска. |
| `pull-arrow` | `rgba(0, 11, 27, 0.31)` / `rgba(231, 241, 255, 0.29)` | `ThingsTheme.colors.pullArrow` | Стрелка индикатора оттяжки до порога. |
| `pull-arrow-selected` | `#5b9aff` / `#3f85f4` | `ThingsTheme.colors.pullArrowSelected` | Стрелка после порога срабатывания. |
| `checklist-circle` | `#4a7df2` | `ThingsTheme.colors.checklistCircle` | Кружок отметки пункта чек-листа. |
| `checklist-check` | `#888888` | `ThingsTheme.colors.checklistCheck` | Галочка пункта чек-листа. |
| `checklist-handle` | `#b5b5b5` | `ThingsTheme.colors.checklistHandle` | Ручка ≡ перетаскивания пункта. |
| `checklist-delete` | `#ee004e` | `ThingsTheme.colors.checklistDelete` | Удаление пункта свайпом. |
| `checklist-divider` | `#ebebeb` | `ThingsTheme.colors.checklistDivider` | Разделители пунктов чек-листа. |
| `checklist-completed` | `#777778` | `ThingsTheme.colors.checklistCompleted` | Текст выполненного пункта. |
| `calendar-green` | `#34a853` | `CalendarGreen` | Пресет цвета календаря (Google Зеленый). |
| `calendar-blue` | `#4285f4` | `CalendarBlue` | Пресет цвета календаря (Google Синий). |
| `calendar-yellow` | `#fbbc05` | `CalendarYellow` | Пресет цвета календаря (Google Желтый). |
| `calendar-red` | `#ea4335` | `CalendarRed` | Пресет цвета календаря (Google Красный). |
| `calendar-purple` | `#8e24aa` | `CalendarPurple` | Пресет цвета календаря (Фиолетовый). |
| `calendar-pink` | `#f06292` | `CalendarPink` | Пресет цвета календаря (Розовый). |
| `calendar-teal` | `#00acc1` | `CalendarTeal` | Пресет цвета календаря (Бирюзовый). |

## Типографика (`ThingsTypography`)

Шрифт: системный шрифт (`FontFamily.Default`). На экранах шире 600 dp базовые размеры автоматически масштабируются на 1.25x внутри `ProvideThingsTheme`.
Все текстовые стили доступны через `ThingsTheme.type.*`.

| Роль в Kotlin | Размер | Насыщенность | Где используется |
| --- | --- | --- | --- |
| `largeTitle` | 32 sp | Bold (700) | Крупное название экрана над списком: «Сегодня», название проекта. |
| `dialogTitle` | 20 sp | Bold (700) | Заголовок диалога: Move, теги, When, удаление. |
| `topAppBarTitle` | 20 sp | SemiBold (600) | Название экрана в верхнем тулбаре после прокрутки. |
| `taskTitle` | 19 sp | Normal (400) | Название задачи в строке списка и в раскрытом редакторе. |
| `sectionHeader` | 19 sp | Medium (500) | Подзаголовки секций: «Сегодня вечером», «Планы», «Когда-нибудь», заголовок поиска. |
| `dialogRow` | 19 sp | Normal (400) | Строка выбора в диалоге: проект в Move, тег, «Сегодня» в When. |
| `dialogButton` | 18 sp | Normal (400) | Кнопка действия в диалоге: Cancel, Delete, Clear. |
| `menuItem` | 18 sp | Normal (400) | Пункт выпадающего меню: «New To-Do», «Delete Project», меню подзаголовка. |
| `editorNotes` | 18 sp | Normal (400) | Заметки в раскрытом редакторе. |
| `editorDate` | 18 sp | Normal (400) | Строка даты и дедлайна в редакторе. |
| `editorChecklist` | 17.2 sp | Normal (400) | Пункты чек-листа в раскрытом редакторе. |
| `button` | 17 sp | SemiBold (600) | Основные кнопки действий и подтверждения. |
| `bodyLarge` | 16 sp | Normal (400) | Поля ввода, основной текст экранов и листов. |
| `headline` | 16 sp | SemiBold (600) | Акцентный подзаголовок, выделенный пункт. |
| `taskSubtitle` | 15 sp | Normal (400) | Подпись под задачей (проект, дата), вторичные пояснения. |
| `dialogBody` | 15 sp | Normal (400) | Пояснительный текст модальных окон и диалогов. |
| `subhead` | 15 sp | Normal (400) | Вспомогательный подзаголовок. |
| `subheadMedium` | 15 sp | Medium (500) | Вспомогательный подзаголовок средней жирности. |
| `bodyMedium` | 14 sp | Normal (400) | Вспомогательный текст, описание проекта, теги. |
| `bodySmall` | 13 sp | Normal (400) | Мелкий пояснительный текст, вторичные счётчики. |
| `caption` | 12 sp | Medium (500) | Мелкие метки, бейджи, подписи. |
| `badge` | 11 sp | Medium (500) | Компактные бейджи даты и приоритета в строке задачи. |

## Радиусы и формы (`ThingsShapes`)

Все скругления углов доступны через `ThingsTheme.shapes.*Shape` (объекты `RoundedCornerShape`) и `ThingsTheme.shapes.*` (значения в `Dp`).

| Токен | Dp | Роль Shape в Kotlin | Где используется |
| --- | --- | --- | --- |
| `radius-indicator` | `1.5 dp` | `ThingsTheme.shapes.indicatorShape` | Тонкая полоска-индикатор статуса/секции. |
| `radius-tiny` | `2 dp` | `ThingsTheme.shapes.tinyShape` | Ручка перетаскивания модальной шторки (drag handle). |
| `radius-badge` | `4 dp` | `ThingsTheme.shapes.badgeShape` | Бейдж даты, метки дедлайна. |
| `radius-small` | `6 dp` | `ThingsTheme.shapes.smallShape` | Мелкий чип, иконка действия. |
| `radius-task` | `8 dp` | `ThingsTheme.shapes.rowShape` | Строка списка, строка в диалоге, раскрытый редактор задачи. |
| `radius-chip` | `10 dp` | `ThingsTheme.shapes.chipShape` | Чип, плашка места вставки при drag-and-drop, подложка подзаголовка. |
| `radius-tag-filter` | `12 dp` | `ThingsTheme.shapes.tagFilterShape` | Фильтр тегов вверху экрана, карточка деталей. |
| `radius-card` | `14 dp` | `ThingsTheme.shapes.cardShape` | Карточка настроек, карточка деталей проекта. |
| `radius-menu` | `16 dp` | `ThingsTheme.shapes.menuShape` | Всплывающее меню-карточка: меню «+» на главном экране. |
| `radius-capsule` | `22 dp` | `ThingsTheme.shapes.capsuleShape` | Капсула поиска (Quick Find), пилюля ввода, плавающее поле. |
| `radius-floating-card` | `30 dp` | `ThingsTheme.shapes.floatingCardShape` | Плавающие карточки: Quick Add, карточка быстрого поиска. |
| `radius-dialog` | `32 dp` | `ThingsTheme.shapes.dialogShape` | Модальные диалоги: When, Move, диалог тегов, подтверждение удаления. |
| `radius-checkbox` | `23 %` | `RoundedCornerShape(size * 0.23f)` | Скругление чекбокса — 23 % его стороны (3.7 dp у 16 dp чекбокса). |
| `radius-full` | `50 %` | `CircleShape` | Кнопка «+», кружки отмены, иконка-прогресс проекта. |

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
