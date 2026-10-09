# Токены

Спецификация токенов дизайн-системы Things 2.
Все визуальные параметры в UI-компонентах используются исключительно через семантические роли `ThingsTheme.colors`, `ThingsTheme.type`, `ThingsTheme.shapes`.
Значения цветов приведены в формате «светлая / тёмная тема»; одно значение — одинаково в обеих темах.

## Цвета (`ThingsColors`)

| Токен | Значение | Роль в Kotlin | Где используется |
| --- | --- | --- | --- |
| `bg` | `#ffffff` / `#000000` | `ThingsTheme.colors.background` | Фон экранов и списков. На нём textPrimary и textSecondary. |
| `surface` | `#f4f5f7` / `#18191b` | `ThingsTheme.colors.surface` | Второй уровень поверхностей: Material surface, фон перетаскиваемой карточки. |
| `divider` | `#e4e4e6` / `#28292b` | `ThingsTheme.colors.divider` | Разделители строк и линия под подзаголовками (0.6–1 dp). |
| `text-primary` | `#000000` / `#ececed` | `ThingsTheme.colors.textPrimary` | Названия задач, проектов, заголовки экранов на bg и surface. Контраст 21:1 / 17:1. |
| `text-secondary` | `#747679` / `#8b8c8e` | `ThingsTheme.colors.textSecondary` | Подписи под задачей (название проекта), даты, пояснения на bg. 4.6:1 в светлой теме, 6.2:1 в тёмной. |
| `accent` | `#5b9aff` | `ThingsTheme.colors.accent` | Главный акцент: кнопка «+», отмеченный чекбокс, курсор, выделение, фон свайпа влево (ThingsBlue). |
| `danger` | `#f35f50` | `ThingsTheme.colors.danger` | Опасные действия: «Delete» в меню подзаголовка и диалоге удаления (ThingsUpcomingRed). |
| `dialog-bg` | `#22242c` | `ThingsTheme.colors.overlaySurface` | Тёмные диалоги, плавающий тулбар и меню в обеих темах: Move, Delete, меню подзаголовка, меню «•••». |
| `overlay-content` | `#ffffff` | `ThingsTheme.colors.overlayContent` | Основной текст и иконки на тёмных диалогах и тулбаре (белый 15:1). |
| `meta-grey` | `#8e8e93` | `ThingsTheme.colors.overlayContentSecondary` | Плейсхолдеры («New To-Do»), вторичный текст Quick Find и диалогов (ThingsMetaGrey). |
| `dialog-button` | `#2c2e38` | `ThingsTheme.colors.overlayControl` | Кнопки и поля внутри тёмных диалогов (Move, теги) на overlaySurface. |
| `dialog-row-selected` | `#1e2027` | `ThingsTheme.colors.overlayRowSelected` | Подсветка выбранной строки в диалоге Move. |
| `overlay-divider` | `#28292b` | `ThingsTheme.colors.overlayDivider` | Разделители внутри тёмных диалогов и меню. |
| `text-notes` | `#424242` / `#8b8c8e` | `ThingsTheme.colors.textNotes` | Текст заметок в раскрытом редакторе. Редактор всегда светлый (белая карточка). |
| `editor-text` | `#1c1c1e` / `#ececed` | `ThingsTheme.colors.editorText` | Название задачи и пункты чек-листа в раскрытом редакторе (на белой карточке); карточка Quick Find в светлой теме (ThingsInk). |
| `checkbox-border` | `#c7c7cc` | `ThingsTheme.colors.checkboxBorder` | Рамка неотмеченного чекбокса, неактивные иконки панели редактора. |
| `tag-chip-bg` | `#d1eae2` | `ThingsTheme.colors.tagChipBackground` | Фон чипа тега в редакторе и Quick Add. |
| `tag-chip-text` | `#2c7d64` | `ThingsTheme.colors.tagChipText` | Текст чипа тега на tagChipBackground. |
| `checklist-circle` | `#4a7df2` | `ThingsTheme.colors.checklistCircle` | Кружок отметки пункта чек-листа. |
| `checklist-check` | `#888888` | `ThingsTheme.colors.checklistCheck` | Галочка пункта чек-листа. |
| `checklist-handle` | `#b5b5b5` | `ThingsTheme.colors.checklistHandle` | Ручка ≡ перетаскивания пункта. |
| `checklist-delete` | `#ee004e` | `ThingsTheme.colors.checklistDelete` | Удаление пункта свайпом. |
| `checklist-divider` | `#ebebeb` / `#28292b` | `ThingsTheme.colors.checklistDivider` | Разделители пунктов чек-листа. |
| `checklist-completed` | `#777778` / `#8b8c8e` | `ThingsTheme.colors.checklistCompleted` | Текст выполненного пункта. |
| `checklist-highlight` | `#f0f1f3` / `#2c2c2e` | `ThingsTheme.colors.checklistHighlight` | Вспышка строки чек-листа при отметке пункта. |
| `checklist-focus` | `#f7f7f7` / `#252629` | `ThingsTheme.colors.checklistFocus` | Фон пункта чек-листа, текст которого в фокусе. |
| `checklist-drag` | `#d7e6fd` / `rgba(91, 154, 255, 0.25)` | `ThingsTheme.colors.checklistDrag` | Фон пункта чек-листа, который тащат за ручку ≡. |
| `accent-selection` | `rgba(91, 154, 255, 0.22)` | `ThingsTheme.colors.accentSelection` | Фон выбранной строки в режиме мультивыбора: accent с прозрачностью 22 %. |
| `list-dim` | `#f4f4f6` / `#151618` | `ThingsTheme.colors.listDim` | Фон списка, пока раскрыта задача (плавно, вместе с затемнением строк). |
| `drop-placeholder` | `#e5e6eb` / `#2c2d32` | `ThingsTheme.colors.dropPlaceholder` | Серая плашка места вставки при перетаскивании задачи, подзаголовка и «+»; рисуется с прозрачностью 50 %. |
| `hairline` | `#e5e5ea` / `#38393d` | `ThingsTheme.colors.hairline` | Обводка нижних карточек «стопки» при перетаскивании (0.5 dp), фон кнопки ✕ в Quick Find, разделитель Quick Add. |
| `stack-card` | `#ffffff` / `#252629` | `ThingsTheme.colors.stackCard` | Нижние карточки стопки при групповом перетаскивании. |
| `field-bg` | `#f2f2f7` / `#2c2c2e` | `ThingsTheme.colors.searchField` | Фон полей поиска (Quick Find, Search), капсулы на главном. |
| `search-card` | `#ffffff` / `#1c1c1e` | `ThingsTheme.colors.searchCard` | Карточка быстрого поиска Quick Find. |
| `search-highlight` | `#ffe692` / `#6e5d01` | `ThingsTheme.colors.searchMatchHighlight` | Подсветка совпадения в Quick Find под textPrimary. |
| `search-close-button` | `#e5e5ea` / `#2c2c2e` | `ThingsTheme.colors.searchCloseButton` | Фон кнопки закрытия в Quick Find. |
| `pull-indicator` | `#8e929c` / `#2c2c2e` | `ThingsTheme.colors.pullIndicatorBackground` | Круг индикатора оттяжки поиска. |
| `pull-arrow` | `rgba(0, 11, 27, 0.31)` / `rgba(231, 241, 255, 0.29)` | `ThingsTheme.colors.pullArrow` | Стрелка индикатора оттяжки до порога. |
| `pull-arrow-selected` | `#5b9aff` / `#3f85f4` | `ThingsTheme.colors.pullArrowSelected` | Стрелка после порога срабатывания. |
| `calendar-card` | `#f2f2f7` / `#18191b` | `ThingsTheme.colors.calendarCard` | Карточка событий календаря. |
| `muted-grey` | `#d1d1d6` / `#48484a` | `ThingsTheme.colors.calendarMuted` | Приглушённый текст и полоски виджета календаря. |
| `badge-bg` | `#ececec` / `#2c2c2e` | `ThingsTheme.colors.badgeBackground` | Фон бейджа в строке задачи. |
| `badge-text` | `#5f6368` / `#e0e0e0` | `ThingsTheme.colors.badgeText` | Текст бейджа в строке задачи; иконка календаря в диалоге When. |
| `editor-icon-inactive` | `#a9a9a9` / `#8b8c8e` | `ThingsTheme.colors.editorIconInactive` | Неактивные иконки нижней строки раскрытого редактора. |
| `clear-action` | `#e22d5a` | `ThingsTheme.colors.clearAction` | Кнопка «Clear» в диалоге When (ThingsWhenClear). |
| `on-accent` | `#ffffff` | `ThingsTheme.colors.onAccent` | Текст и иконки на цветной заливке: «+», отмеченный чекбокс, фон свайпа, выбранный тег, кнопки accent. |
| `scrim` | `#000000` | `ThingsTheme.colors.scrim` | Затемнение под модальными окнами и при переходах; прозрачность задаёт место использования. |
| `inbox` | `#1b80fa` | `ThingsTheme.colors.inbox` | Раздел «Входящие»: значок и маркер. |
| `today` | `#e9ac10` | `ThingsTheme.colors.today` | Раздел «Сегодня»: звезда-маркер в строке задачи и редакторе; приоритет «средний». |
| `upcoming` | `#f35f50` | `ThingsTheme.colors.upcoming` | Раздел «Предстоящие»; приоритет «высокий». |
| `anytime` | `#2eb7cd` | `ThingsTheme.colors.anytime` | Раздел «В любое время»; иконка тега в поле тегов редактора; приоритет «низкий». |
| `someday` | `#8f93a3` | `ThingsTheme.colors.someday` | Раздел «Когда-нибудь», иконки тегов в списках и поиске, неактивные уровни приоритета. |
| `area` | `#269c6e` / `#7ad0a7` | `ThingsTheme.colors.area` | Иконка области и её название; в тёмной теме светлее, чтобы читаться на чёрном. |
| `project` | `#2a62d9` | `ThingsTheme.colors.project` | Иконка-прогресс проекта, подзаголовки проекта и их «•••» (ThingsDeepBlue). |
| `swipe-when` | `#ffd401` | `ThingsTheme.colors.swipeWhen` | Фон свайпа вправо (действие «Когда»). |
| `evening-indicator` | `#2196f3` | `ThingsTheme.colors.eveningIndicator` | Значок «вечер» в строке задачи. |
| `area-icon-stroke` | `#9c9c9c` | `ThingsTheme.colors.areaIconStroke` | Контур анимированной иконки области на главном экране. |
| `new-heading-line` | `#c9cbd1` | `ThingsTheme.colors.newHeadingLine` | Пунктир места нового подзаголовка при перетаскивании «+». |
| `new-heading-text` | `#b4b6bc` | `ThingsTheme.colors.newHeadingText` | Надпись «NEW HEADING» на месте нового подзаголовка. |
| `home-drop-placeholder` | `#e9eaee` | `ThingsTheme.colors.homeDropPlaceholder` | Плашка места вставки нового проекта на главном экране (без прозрачности). |
| `calendar-default` | `#63c655` | `ThingsTheme.colors.calendarDefault` | Цвет события, если у календаря нет своего. |
| `calendar-green` | `#34a853` | `calendarPresets[0]` | Пресет цвета календаря (Google Зеленый). |
| `calendar-blue` | `#4285f4` | `calendarPresets[1]` | Пресет цвета календаря (Google Синий). |
| `calendar-yellow` | `#fbbc05` | `calendarPresets[2]` | Пресет цвета календаря (Google Желтый). |
| `calendar-red` | `#ea4335` | `calendarPresets[3]` | Пресет цвета календаря (Google Красный). |
| `calendar-purple` | `#8e24aa` | `calendarPresets[4]` | Пресет цвета календаря (Фиолетовый). |
| `calendar-pink` | `#f06292` | `calendarPresets[5]` | Пресет цвета календаря (Розовый). |
| `calendar-teal` | `#00acc1` | `calendarPresets[6]` | Пресет цвета календаря (Бирюзовый). |

## Типографика (`ThingsTypography`)

Шрифт: системный шрифт (`FontFamily.Default`). На экранах шире 600 dp базовые размеры автоматически масштабируются на 1.25x внутри `ProvideThingsTheme`.
Все текстовые стили доступны через `ThingsTheme.type.*`.

| Роль в Kotlin | Размер | Насыщенность | Где используется |
| --- | --- | --- | --- |
| `largeTitle` | 32.0 sp | Medium (500) | Крупное название экрана над списком: «Сегодня», название проекта. |
| `dialogTitle` | 20.0 sp | Bold (700) | Заголовок диалога: Move, теги, When, удаление. |
| `topAppBarTitle` | 20.0 sp | SemiBold (600) | Название экрана в верхнем тулбаре после прокрутки. |
| `taskTitle` | 19.0 sp | Normal (400) | Название задачи в строке списка и в раскрытом редакторе. |
| `sectionHeader` | 19.0 sp | Medium (500) | Подзаголовки секций: «Сегодня вечером», «Планы», «Когда-нибудь», заголовок поиска. |
| `dialogRow` | 19.0 sp | Normal (400) | Строка выбора в диалоге: проект в Move, тег, «Сегодня» в When. |
| `dialogRowSelected` | 19.0 sp | SemiBold (600) | Выбранная строка диалога: тег, группа тегов. |
| `dialogRowAction` | 19.0 sp | Medium (500) | Строка-действие диалога цветом accent: «Новий тег», «Керування тегами». |
| `dialogButton` | 18.0 sp | Normal (400) | Кнопка действия в диалоге: Cancel, Delete, Clear. |
| `dialogButtonStrong` | 18.0 sp | Bold (700) | Подпись «Перемістити» на плавающей панели действий. |
| `menuItem` | 18.0 sp | Normal (400) | Пункт выпадающего меню: «New To-Do», «Delete Project», меню подзаголовка. |
| `editorNotes` | 18.0 sp | Normal (400) | Заметки в раскрытом редакторе. |
| `editorDate` | 18.0 sp | Normal (400) | Строка даты и дедлайна в редакторе. |
| `editorDateStrong` | 18.0 sp | Medium (500) | Выделенная дата в редакторе и окне быстрого добавления: «Сьогодні», срок. |
| `editorChecklist` | 17.2 sp | Normal (400) | Пункты чек-листа в раскрытом редакторе. |
| `editorChecklistStrong` | 17.2 sp | Medium (500) | Назначение в нижней панели окна быстрого добавления. |
| `button` | 17.0 sp | SemiBold (600) | Основные кнопки действий и подтверждения. |
| `bodyLarge` | 16.0 sp | Normal (400) | Поля ввода, основной текст экранов и листов. |
| `headline` | 16.0 sp | SemiBold (600) | Акцентный подзаголовок, выделенный пункт. |
| `headlineStrong` | 16.0 sp | Bold (700) | Название дня и месяца в «Планах». |
| `taskSubtitle` | 15.0 sp | Normal (400) | Подпись под задачей (проект, дата), вторичные пояснения. |
| `taskSubtitleStrong` | 15.0 sp | SemiBold (600) | Выделенная подпись под задачей: срок. |
| `dialogBody` | 15.0 sp | Normal (400) | Пояснительный текст модальных окон и диалогов. |
| `subhead` | 15.0 sp | Normal (400) | Вспомогательный подзаголовок. |
| `subheadMedium` | 15.0 sp | Medium (500) | Вспомогательный подзаголовок средней жирности. |
| `bodyMedium` | 14.0 sp | Normal (400) | Вспомогательный текст, описание проекта, теги. |
| `bodySmall` | 13.0 sp | Normal (400) | Мелкий пояснительный текст, вторичные счётчики. |
| `caption` | 12.0 sp | Medium (500) | Мелкие метки, бейджи, подписи. |
| `captionStrong` | 12.0 sp | Bold (700) | Чип «Все» и чипы тегов в строке фильтра по тегам. |
| `tagChip` | 14.0 sp | Medium (500) | Название тега в чипе редактора задачи и окна быстрого добавления. |
| `badge` | 11.0 sp | Medium (500) | Компактные бейджи даты и приоритета в строке задачи. |
| `badgeStrong` | 11.0 sp | Bold (700) | Метка уровня приоритета в панели приоритета. |
| `listTitle` | 19.0 sp | Medium (500) | Название в строке главного экрана: раздел, проект, область. |
| `listTitleStrong` | 19.0 sp | SemiBold (600) | Выделенная строка: «Продовжити пошук», подзаголовок проекта. |
| `listItem` | 19.0 sp | Normal (400) | Название проекта в строке главного экрана — обычное начертание. |
| `settingsTitle` | 14.0 sp | SemiBold (600) | Заголовок карточки настроек на главном экране (синхронизация Google Tasks). |
| `settingsAction` | 14.0 sp | Bold (700) | Текст кнопки действия в карточке настроек. |
| `heroEmoji` | 30.0 sp | Normal (400) | Эмодзи перед крупным названием проекта или области. |
| `overline` | 11.0 sp | Bold (700) | Надпись капителью над группой: «PROJECTS», «TASKS», «NEW HEADING». |

## Радиусы и формы (`ThingsShapes`)

Все скругления углов доступны через `ThingsTheme.shapes.*Shape` (объекты `RoundedCornerShape`) и `ThingsTheme.shapes.*` (значения в `Dp`).

| Токен | Dp | Роль Shape в Kotlin | Где используется |
| --- | --- | --- | --- |
| `radius-indicator` | `1.5 dp` | `ThingsTheme.shapes.indicatorShape` | Тонкая полоска-индикатор статуса/секции. |
| `radius-tiny` | `2.0 dp` | `ThingsTheme.shapes.tinyShape` | Ручка перетаскивания модальной шторки (drag handle). |
| `radius-badge` | `4.0 dp` | `ThingsTheme.shapes.badgeShape` | Бейдж даты, метки дедлайна. |
| `radius-small` | `6.0 dp` | `ThingsTheme.shapes.smallShape` | Мелкий чип, иконка действия. |
| `radius-task` | `8.0 dp` | `ThingsTheme.shapes.rowShape` | Строка списка, строка в диалоге, раскрытый редактор задачи. |
| `radius-chip` | `10.0 dp` | `ThingsTheme.shapes.chipShape` | Чип, плашка места вставки при drag-and-drop, подложка подзаголовка. |
| `radius-tag-filter` | `12.0 dp` | `ThingsTheme.shapes.tagFilterShape` | Фильтр тегов вверху экрана, карточка деталей. |
| `radius-card` | `14.0 dp` | `ThingsTheme.shapes.cardShape` | Карточка настроек, карточка деталей проекта. |
| `radius-menu` | `16.0 dp` | `ThingsTheme.shapes.menuShape` | Всплывающее меню-карточка: меню «+» на главном экране. |
| `radius-button` | `18.0 dp` | `ThingsTheme.shapes.buttonShape` | Кнопки действий (Save в шторке задачи). |
| `radius-capsule` | `22.0 dp` | `ThingsTheme.shapes.capsuleShape` | Капсула поиска (Quick Find), пилюля ввода, плавающее поле. |
| `radius-toolbar` | `25.0 dp` | `ThingsTheme.shapes.toolbarShape` | Плавающая панель действий внизу экрана — капсула высотой 50 dp. |
| `radius-floating-card` | `30.0 dp` | `ThingsTheme.shapes.floatingCardShape` | Плавающие карточки: Quick Add, карточка быстрого поиска. |
| `radius-dialog` | `32.0 dp` | `ThingsTheme.shapes.dialogShape` | Модальные диалоги: When, Move, диалог тегов, подтверждение удаления. |
| `radius-checkbox` | `23 %` | `RoundedCornerShape(size * 0.23f)` | Скругление чекбокса — 23 % его стороны (3.7 dp у 16 dp чекбокса). |
| `radius-full` | `50 %` | `CircleShape` | Кнопка «+», кружки отмены, иконка-прогресс проекта. |

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
