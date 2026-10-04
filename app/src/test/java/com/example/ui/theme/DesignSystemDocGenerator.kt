package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import java.io.File
import kotlin.math.roundToInt
import kotlin.reflect.full.memberProperties

/**
 * Single Source of Truth генератор документации дизайн-системы Things 2.
 *
 * Строит `docs/design-system/tokens.json` и `docs/design-system/Tokens.md`
 * напрямую из актуальных классов Kotlin:
 * - [ThingsColors] ([LightThingsColors], [DarkThingsColors])
 * - [ThingsTypography] ([thingsTypography])
 * - [ThingsShapes]
 *
 * Гарантирует через Kotlin Reflection, что 100% свойств дизайн-системы
 * задокументированы и синхронизированы с кодовой базой.
 */
object DesignSystemDocGenerator {

    data class ColorToken(
        val tokenName: String,
        val propertyName: String,
        val lightGetter: (ThingsColors) -> Color,
        val darkGetter: (ThingsColors) -> Color,
        val usage: String
    )

    data class StaticColorToken(
        val tokenName: String,
        val color: Color,
        val kotlinRef: String,
        val usage: String
    )

    data class TypeToken(
        val propertyName: String,
        val getter: (ThingsTypography) -> TextStyle,
        val usage: String,
        val sample: String = ""
    )

    data class ShapeToken(
        val tokenName: String,
        val propertyName: String,
        val getter: (ThingsShapes) -> Dp,
        val shapeProperty: String,
        val usage: String
    )

    val colorTokens: List<ColorToken> = listOf(
        ColorToken("bg", "background", { it.background }, { it.background }, "Фон экранов и списков. На нём textPrimary и textSecondary."),
        ColorToken("surface", "surface", { it.surface }, { it.surface }, "Второй уровень поверхностей: Material surface, фон перетаскиваемой карточки."),
        ColorToken("divider", "divider", { it.divider }, { it.divider }, "Разделители строк и линия под подзаголовками (0.6–1 dp)."),
        ColorToken("text-primary", "textPrimary", { it.textPrimary }, { it.textPrimary }, "Названия задач, проектов, заголовки экранов на bg и surface. Контраст 21:1 / 17:1."),
        ColorToken("text-secondary", "textSecondary", { it.textSecondary }, { it.textSecondary }, "Подписи под задачей (название проекта), даты, пояснения на bg. 4.6:1 в светлой теме, 6.2:1 в тёмной."),
        ColorToken("accent", "accent", { it.accent }, { it.accent }, "Главный акцент: кнопка «+», отмеченный чекбокс, курсор, выделение, фон свайпа влево (ThingsBlue)."),
        ColorToken("danger", "danger", { it.danger }, { it.danger }, "Опасные действия: «Delete» в меню подзаголовка и диалоге удаления (ThingsUpcomingRed)."),
        ColorToken("dialog-bg", "overlaySurface", { it.overlaySurface }, { it.overlaySurface }, "Тёмные диалоги, плавающий тулбар и меню в обеих темах: Move, Delete, меню подзаголовка, меню «•••»."),
        ColorToken("overlay-content", "overlayContent", { it.overlayContent }, { it.overlayContent }, "Основной текст и иконки на тёмных диалогах и тулбаре (белый 15:1)."),
        ColorToken("meta-grey", "overlayContentSecondary", { it.overlayContentSecondary }, { it.overlayContentSecondary }, "Плейсхолдеры («New To-Do»), вторичный текст Quick Find и диалогов (ThingsMetaGrey)."),
        ColorToken("dialog-button", "overlayControl", { it.overlayControl }, { it.overlayControl }, "Кнопки и поля внутри тёмных диалогов (Move, теги) на overlaySurface."),
        ColorToken("dialog-row-selected", "overlayRowSelected", { it.overlayRowSelected }, { it.overlayRowSelected }, "Подсветка выбранной строки в диалоге Move."),
        ColorToken("overlay-divider", "overlayDivider", { it.overlayDivider }, { it.overlayDivider }, "Разделители внутри тёмных диалогов и меню."),
        ColorToken("text-notes", "textNotes", { it.textNotes }, { it.textNotes }, "Текст заметок в раскрытом редакторе. Редактор всегда светлый (белая карточка)."),
        ColorToken("editor-text", "editorText", { it.editorText }, { it.editorText }, "Название задачи и пункты чек-листа в раскрытом редакторе (на белой карточке); карточка Quick Find в светлой теме (ThingsInk)."),
        ColorToken("checkbox-border", "checkboxBorder", { it.checkboxBorder }, { it.checkboxBorder }, "Рамка неотмеченного чекбокса, неактивные иконки панели редактора."),
        ColorToken("tag-chip-bg", "tagChipBackground", { it.tagChipBackground }, { it.tagChipBackground }, "Фон чипа тега в редакторе и Quick Add."),
        ColorToken("tag-chip-text", "tagChipText", { it.tagChipText }, { it.tagChipText }, "Текст чипа тега на tagChipBackground."),
        ColorToken("checklist-circle", "checklistCircle", { it.checklistCircle }, { it.checklistCircle }, "Кружок отметки пункта чек-листа."),
        ColorToken("checklist-check", "checklistCheck", { it.checklistCheck }, { it.checklistCheck }, "Галочка пункта чек-листа."),
        ColorToken("checklist-handle", "checklistHandle", { it.checklistHandle }, { it.checklistHandle }, "Ручка ≡ перетаскивания пункта."),
        ColorToken("checklist-delete", "checklistDelete", { it.checklistDelete }, { it.checklistDelete }, "Удаление пункта свайпом."),
        ColorToken("checklist-divider", "checklistDivider", { it.checklistDivider }, { it.checklistDivider }, "Разделители пунктов чек-листа."),
        ColorToken("checklist-completed", "checklistCompleted", { it.checklistCompleted }, { it.checklistCompleted }, "Текст выполненного пункта."),
        ColorToken("checklist-highlight", "checklistHighlight", { it.checklistHighlight }, { it.checklistHighlight }, "Вспышка строки чек-листа при отметке пункта."),
        ColorToken("checklist-focus", "checklistFocus", { it.checklistFocus }, { it.checklistFocus }, "Фон пункта чек-листа, текст которого в фокусе."),
        ColorToken("checklist-drag", "checklistDrag", { it.checklistDrag }, { it.checklistDrag }, "Фон пункта чек-листа, который тащат за ручку ≡."),
        ColorToken("accent-selection", "accentSelection", { it.accentSelection }, { it.accentSelection }, "Фон выбранной строки в режиме мультивыбора: accent с прозрачностью 22 %."),
        ColorToken("list-dim", "listDim", { it.listDim }, { it.listDim }, "Фон списка, пока раскрыта задача (плавно, вместе с затемнением строк)."),
        ColorToken("drop-placeholder", "dropPlaceholder", { it.dropPlaceholder }, { it.dropPlaceholder }, "Серая плашка места вставки при перетаскивании задачи, подзаголовка и «+»; рисуется с прозрачностью 50 %."),
        ColorToken("hairline", "hairline", { it.hairline }, { it.hairline }, "Обводка нижних карточек «стопки» при перетаскивании (0.5 dp), фон кнопки ✕ в Quick Find, разделитель Quick Add."),
        ColorToken("stack-card", "stackCard", { it.stackCard }, { it.stackCard }, "Нижние карточки стопки при групповом перетаскивании."),
        ColorToken("field-bg", "searchField", { it.searchField }, { it.searchField }, "Фон полей поиска (Quick Find, Search), капсулы на главном."),
        ColorToken("search-card", "searchCard", { it.searchCard }, { it.searchCard }, "Карточка быстрого поиска Quick Find."),
        ColorToken("search-highlight", "searchMatchHighlight", { it.searchMatchHighlight }, { it.searchMatchHighlight }, "Подсветка совпадения в Quick Find под textPrimary."),
        ColorToken("search-close-button", "searchCloseButton", { it.searchCloseButton }, { it.searchCloseButton }, "Фон кнопки закрытия в Quick Find."),
        ColorToken("pull-indicator", "pullIndicatorBackground", { it.pullIndicatorBackground }, { it.pullIndicatorBackground }, "Круг индикатора оттяжки поиска."),
        ColorToken("pull-arrow", "pullArrow", { it.pullArrow }, { it.pullArrow }, "Стрелка индикатора оттяжки до порога."),
        ColorToken("pull-arrow-selected", "pullArrowSelected", { it.pullArrowSelected }, { it.pullArrowSelected }, "Стрелка после порога срабатывания."),
        ColorToken("calendar-card", "calendarCard", { it.calendarCard }, { it.calendarCard }, "Карточка событий календаря."),
        ColorToken("muted-grey", "calendarMuted", { it.calendarMuted }, { it.calendarMuted }, "Приглушённый текст и полоски виджета календаря."),
        ColorToken("badge-bg", "badgeBackground", { it.badgeBackground }, { it.badgeBackground }, "Фон бейджа в строке задачи."),
        ColorToken("badge-text", "badgeText", { it.badgeText }, { it.badgeText }, "Текст бейджа в строке задачи; иконка календаря в диалоге When."),
        ColorToken("editor-icon-inactive", "editorIconInactive", { it.editorIconInactive }, { it.editorIconInactive }, "Неактивные иконки нижней строки раскрытого редактора."),
        ColorToken("clear-action", "clearAction", { it.clearAction }, { it.clearAction }, "Кнопка «Clear» в диалоге When (ThingsWhenClear).")
    )

    val staticPaletteTokens: List<StaticColorToken> = listOf(
        StaticColorToken("deep-blue", ThingsDeepBlue, "ThingsDeepBlue", "Подзаголовки проекта и их «•••», иконки-прогресс проектов. 5.5:1 на белом."),
        StaticColorToken("inbox-blue", ThingsInboxBlue, "ThingsInboxBlue", "Раздел «Входящие»."),
        StaticColorToken("today-star", ThingsTodayStar, "ThingsTodayStar", "Раздел «Сегодня»: звезда-маркер в строке задачи и редакторе."),
        StaticColorToken("anytime-teal", ThingsAnytimeTeal, "ThingsAnytimeTeal", "Раздел «В любое время»; иконка тега в поле тегов редактора."),
        StaticColorToken("someday-grey", ThingsSomedayGrey, "ThingsSomedayGrey", "Раздел «Когда-нибудь», иконки тегов в списках и поиске, Material secondary."),
        StaticColorToken("area-green-light", ThingsAreaGreenLight, "ThingsAreaGreenLight", "Иконка области (Area) и её название в поиске (светлая тема)."),
        StaticColorToken("area-green-dark", ThingsAreaGreenDark, "ThingsAreaGreenDark", "Иконка области (Area) и её название в поиске (тёмная тема)."),
        StaticColorToken("when-yellow", ThingsSwipeWhenYellow, "ThingsSwipeWhenYellow", "Фон свайпа вправо (действие «Когда»)."),
        StaticColorToken("home-drop-placeholder", ThingsHomeDropPlaceholder, "ThingsHomeDropPlaceholder", "Плашка места вставки нового проекта на главном экране."),
        StaticColorToken("new-heading-line", ThingsNewHeadingLine, "ThingsNewHeadingLine", "Пунктир места нового подзаголовка при перетаскивании «+»."),
        StaticColorToken("new-heading-text", ThingsNewHeadingText, "ThingsNewHeadingText", "Надпись «NEW HEADING» на месте нового подзаголовка."),
        StaticColorToken("when-clear", ThingsWhenClear, "ThingsWhenClear", "Кнопка «Clear» диалога When, белый текст."),
        StaticColorToken("evening-indicator", ThingsEveningIndicator, "ThingsEveningIndicator", "Значок «вечер» в строке задачи."),
        StaticColorToken("area-icon-stroke", ThingsAreaIconStroke, "ThingsAreaIconStroke", "Контур анимированной иконки области."),
        StaticColorToken("calendar-default", ThingsCalendarDefaultGreen, "ThingsCalendarDefaultGreen", "Цвет события, если у календаря нет своего."),
        StaticColorToken("calendar-green", CalendarGreen, "CalendarGreen", "Пресет цвета календаря (Google Зеленый)."),
        StaticColorToken("calendar-blue", CalendarBlue, "CalendarBlue", "Пресет цвета календаря (Google Синий)."),
        StaticColorToken("calendar-yellow", CalendarYellow, "CalendarYellow", "Пресет цвета календаря (Google Желтый)."),
        StaticColorToken("calendar-red", CalendarRed, "CalendarRed", "Пресет цвета календаря (Google Красный)."),
        StaticColorToken("calendar-purple", CalendarPurple, "CalendarPurple", "Пресет цвета календаря (Фиолетовый)."),
        StaticColorToken("calendar-pink", CalendarPink, "CalendarPink", "Пресет цвета календаря (Розовый)."),
        StaticColorToken("calendar-teal", CalendarTeal, "CalendarTeal", "Пресет цвета календаря (Бирюзовый).")
    )

    val typeTokens: List<TypeToken> = listOf(
        TypeToken("largeTitle", { it.largeTitle }, "Крупное название экрана над списком: «Сегодня», название проекта.", "Сьогодні"),
        TypeToken("dialogTitle", { it.dialogTitle }, "Заголовок диалога: Move, теги, When, удаление.", "When?"),
        TypeToken("topAppBarTitle", { it.topAppBarTitle }, "Название экрана в верхнем тулбаре после прокрутки.", "Сьогодні"),
        TypeToken("taskTitle", { it.taskTitle }, "Название задачи в строке списка и в раскрытом редакторе.", "Купити молоко"),
        TypeToken("sectionHeader", { it.sectionHeader }, "Подзаголовки секций: «Сегодня вечером», «Планы», «Когда-нибудь», заголовок поиска.", "ЦЬОГО ВЕЧОРА"),
        TypeToken("dialogRow", { it.dialogRow }, "Строка выбора в диалоге: проект в Move, тег, «Сегодня» в When.", "Inbox"),
        TypeToken("dialogButton", { it.dialogButton }, "Кнопка действия в диалоге: Cancel, Delete, Clear.", "Cancel"),
        TypeToken("menuItem", { it.menuItem }, "Пункт выпадающего меню: «New To-Do», «Delete Project», меню подзаголовка.", "New To-Do"),
        TypeToken("editorNotes", { it.editorNotes }, "Заметки в раскрытом редакторе.", "Додаткові примітки..."),
        TypeToken("editorDate", { it.editorDate }, "Строка даты и дедлайна в редакторе.", "Today"),
        TypeToken("editorChecklist", { it.editorChecklist }, "Пункты чек-листа в раскрытом редакторе.", "Пункт 1"),
        TypeToken("button", { it.button }, "Основные кнопки действий и подтверждения.", "Done"),
        TypeToken("bodyLarge", { it.bodyLarge }, "Поля ввода, основной текст экранов и листов.", "Quick Add"),
        TypeToken("headline", { it.headline }, "Акцентный подзаголовок, выделенный пункт.", "Headline"),
        TypeToken("taskSubtitle", { it.taskSubtitle }, "Подпись под задачей (проект, дата), вторичные пояснения.", "Things 2"),
        TypeToken("dialogBody", { it.dialogBody }, "Пояснительный текст модальных окон и диалогов.", "Are you sure?"),
        TypeToken("subhead", { it.subhead }, "Вспомогательный подзаголовок.", "Subhead"),
        TypeToken("subheadMedium", { it.subheadMedium }, "Вспомогательный подзаголовок средней жирности.", "Subhead Medium"),
        TypeToken("bodyMedium", { it.bodyMedium }, "Вспомогательный текст, описание проекта, теги.", "Description text"),
        TypeToken("bodySmall", { it.bodySmall }, "Мелкий пояснительный текст, вторичные счётчики.", "Secondary small text"),
        TypeToken("caption", { it.caption }, "Мелкие метки, бейджи, подписи.", "14:00"),
        TypeToken("badge", { it.badge }, "Компактные бейджи даты и приоритета в строке задачи.", "TODAY")
    )

    val shapeTokens: List<ShapeToken> = listOf(
        ShapeToken("radius-indicator", "indicator", { it.indicator }, "indicatorShape", "Тонкая полоска-индикатор статуса/секции."),
        ShapeToken("radius-tiny", "tiny", { it.tiny }, "tinyShape", "Ручка перетаскивания модальной шторки (drag handle)."),
        ShapeToken("radius-badge", "badge", { it.badge }, "badgeShape", "Бейдж даты, метки дедлайна."),
        ShapeToken("radius-small", "small", { it.small }, "smallShape", "Мелкий чип, иконка действия."),
        ShapeToken("radius-task", "row", { it.row }, "rowShape", "Строка списка, строка в диалоге, раскрытый редактор задачи."),
        ShapeToken("radius-chip", "chip", { it.chip }, "chipShape", "Чип, плашка места вставки при drag-and-drop, подложка подзаголовка."),
        ShapeToken("radius-tag-filter", "tagFilter", { it.tagFilter }, "tagFilterShape", "Фильтр тегов вверху экрана, карточка деталей."),
        ShapeToken("radius-card", "card", { it.card }, "cardShape", "Карточка настроек, карточка деталей проекта."),
        ShapeToken("radius-menu", "menu", { it.menu }, "menuShape", "Всплывающее меню-карточка: меню «+» на главном экране."),
        ShapeToken("radius-button", "button", { it.button }, "buttonShape", "Кнопки действий (Save в шторке задачи)."),
        ShapeToken("radius-capsule", "capsule", { it.capsule }, "capsuleShape", "Капсула поиска (Quick Find), пилюля ввода, плавающее поле."),
        ShapeToken("radius-floating-card", "floatingCard", { it.floatingCard }, "floatingCardShape", "Плавающие карточки: Quick Add, карточка быстрого поиска."),
        ShapeToken("radius-dialog", "dialog", { it.dialog }, "dialogShape", "Модальные диалоги: When, Move, диалог тегов, подтверждение удаления.")
    )

    fun Color.toHexOrRgba(): String {
        val a = (alpha * 255f).roundToInt().coerceIn(0, 255)
        val r = (red * 255f).roundToInt().coerceIn(0, 255)
        val g = (green * 255f).roundToInt().coerceIn(0, 255)
        val b = (blue * 255f).roundToInt().coerceIn(0, 255)
        return if (a == 255) {
            String.format("#%02x%02x%02x", r, g, b)
        } else {
            val formattedAlpha = if (alpha == 0.22f) "0.22" else String.format(java.util.Locale.US, "%.2f", alpha)
            "rgba($r, $g, $b, $formattedAlpha)"
        }
    }

    /**
     * Проверяет, что 100% свойств из Kotlin-классов темы присутствуют в генераторе.
     */
    fun validateCompleteness() {
        // 1. Проверяем ThingsColors (все свойства типа Color)
        val colorPropertyNames = ThingsColors::class.memberProperties
            .filter { it.returnType.classifier == Color::class }
            .map { it.name }
            .toSet()
        val documentedColors = colorTokens.map { it.propertyName }.toSet()
        val missingColors = colorPropertyNames - documentedColors
        if (missingColors.isNotEmpty()) {
            throw IllegalStateException("В ThingsColors добавлены новые свойства, не учтённые в документации: $missingColors")
        }

        // 2. Проверяем ThingsTypography (все свойства типа TextStyle)
        val typePropertyNames = ThingsTypography::class.memberProperties
            .filter { it.returnType.classifier == TextStyle::class }
            .map { it.name }
            .toSet()
        val documentedTypes = typeTokens.map { it.propertyName }.toSet()
        val missingTypes = typePropertyNames - documentedTypes
        if (missingTypes.isNotEmpty()) {
            throw IllegalStateException("В ThingsTypography добавлены новые стили, не учтённые в документации: $missingTypes")
        }

        // 3. Проверяем ThingsShapes (все свойства типа Dp)
        val shapePropertyNames = ThingsShapes::class.memberProperties
            .filter { it.returnType.classifier == Dp::class }
            .map { it.name }
            .toSet()
        val documentedShapes = shapeTokens.map { it.propertyName }.toSet()
        val missingShapes = shapePropertyNames - documentedShapes
        if (missingShapes.isNotEmpty()) {
            throw IllegalStateException("В ThingsShapes добавлены новые формы, не учтённые в документации: $missingShapes")
        }
    }

    /**
     * Генерирует содержимое tokens.json
     */
    fun generateTokensJson(): String {
        validateCompleteness()

        val light = LightThingsColors
        val dark = DarkThingsColors
        val typo = thingsTypography(1f)
        val shapes = ThingsShapes()

        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"name\": \"Things 2\",")
        sb.appendLine("  \"version\": 1,")
        sb.appendLine("  \"meta\": {")
        sb.appendLine("    \"source\": \"github\",")
        sb.appendLine("    \"repo\": \"yoda887/Things-2\",")
        sb.appendLine("    \"package\": \"app\",")
        sb.appendLine("    \"sourceOfTruth\": \"app/src/main/java/com/example/ui/theme/ThingsTheme.kt\",")
        sb.appendLine("    \"synced\": \"auto-generated\"")
        sb.appendLine("  },")
        sb.appendLine("  \"color\": {")
        sb.appendLine("    \"themes\": [")
        sb.appendLine("      {")
        sb.appendLine("        \"id\": \"light\",")
        sb.appendLine("        \"name\": \"Light\"")
        sb.appendLine("      },")
        sb.appendLine("      {")
        sb.appendLine("        \"id\": \"dark\",")
        sb.appendLine("        \"name\": \"Dark\"")
        sb.appendLine("      }")
        sb.appendLine("    ],")
        sb.appendLine("    \"tokens\": [")

        val allColorJsonEntries = mutableListOf<String>()

        colorTokens.forEach { token ->
            val lVal = token.lightGetter(light).toHexOrRgba()
            val dVal = token.darkGetter(dark).toHexOrRgba()
            val entry = StringBuilder()
            entry.appendLine("      {")
            entry.appendLine("        \"name\": \"${token.tokenName}\",")
            if (lVal == dVal) {
                entry.appendLine("        \"value\": \"$lVal\",")
            } else {
                entry.appendLine("        \"value\": {")
                entry.appendLine("          \"light\": \"$lVal\",")
                entry.appendLine("          \"dark\": \"$dVal\"")
                entry.appendLine("        },")
            }
            entry.appendLine("        \"usage\": \"Kotlin: `ThingsTheme.colors.${token.propertyName}`. ${token.usage}\"")
            entry.append("      }")
            allColorJsonEntries.add(entry.toString())
        }

        staticPaletteTokens.forEach { token ->
            val hex = token.color.toHexOrRgba()
            val entry = StringBuilder()
            entry.appendLine("      {")
            entry.appendLine("        \"name\": \"${token.tokenName}\",")
            entry.appendLine("        \"value\": \"$hex\",")
            entry.appendLine("        \"usage\": \"Kotlin: `${token.kotlinRef}`. ${token.usage}\"")
            entry.append("      }")
            allColorJsonEntries.add(entry.toString())
        }

        sb.appendLine(allColorJsonEntries.joinToString(",\n"))
        sb.appendLine("    ]")
        sb.appendLine("  },")

        // Типографика
        sb.appendLine("  \"type\": {")
        sb.appendLine("    \"fonts\": [],")
        sb.appendLine("    \"families\": {")
        sb.appendLine("      \"sans\": \"Roboto, system-ui, -apple-system, sans-serif\"")
        sb.appendLine("    },")
        sb.appendLine("    \"tokens\": [")

        val typeEntries = typeTokens.map { token ->
            val style = token.getter(typo)
            val size = "${style.fontSize.value.toInt()}px"
            val weight = style.fontWeight?.weight ?: 400
            val entry = StringBuilder()
            entry.appendLine("      {")
            entry.appendLine("        \"name\": \"${token.propertyName}\",")
            entry.appendLine("        \"fontSize\": \"$size\",")
            entry.appendLine("        \"fontWeight\": $weight,")
            entry.appendLine("        \"usage\": \"Kotlin: `ThingsTheme.type.${token.propertyName}`. ${token.usage}\",")
            entry.appendLine("        \"sample\": \"${token.sample}\"")
            entry.append("      }")
            entry.toString()
        }
        sb.appendLine(typeEntries.joinToString(",\n"))
        sb.appendLine("    ]")
        sb.appendLine("  },")

        // Радиусы
        sb.appendLine("  \"radius\": {")
        sb.appendLine("    \"tokens\": [")

        val shapeEntries = shapeTokens.map { token ->
            val dpVal = "${token.getter(shapes).value}px"
            val entry = StringBuilder()
            entry.appendLine("      {")
            entry.appendLine("        \"name\": \"${token.tokenName}\",")
            entry.appendLine("        \"value\": \"$dpVal\",")
            entry.appendLine("        \"usage\": \"Kotlin: `ThingsTheme.shapes.${token.shapeProperty}`. ${token.usage}\"")
            entry.append("      }")
            entry.toString()
        }

        val extraShapeEntries = listOf(
            "      {\n        \"name\": \"radius-checkbox\",\n        \"value\": \"23%\",\n        \"usage\": \"Скругление чекбокса — 23 % его стороны (3.7 px у основного 16 dp).\"\n      }",
            "      {\n        \"name\": \"radius-full\",\n        \"value\": \"50%\",\n        \"usage\": \"Кнопка «+», кружки отмены, иконка-прогресс проекта.\"\n      }"
        )

        sb.appendLine((shapeEntries + extraShapeEntries).joinToString(",\n"))
        sb.appendLine("    ]")
        sb.appendLine("  },")

        // Отступы и размеры
        sb.appendLine(staticDimensionsJsonSection())

        sb.appendLine("}")
        return sb.toString()
    }

    /**
     * Генерирует содержимое Tokens.md
     */
    fun generateTokensMarkdown(): String {
        validateCompleteness()

        val light = LightThingsColors
        val dark = DarkThingsColors
        val typo = thingsTypography(1f)
        val shapes = ThingsShapes()

        val sb = StringBuilder()
        sb.appendLine("# Токены")
        sb.appendLine()
        sb.appendLine("Спецификация токенов дизайн-системы Things 2.")
        sb.appendLine("Все визуальные параметры в UI-компонентах используются исключительно через семантические роли `ThingsTheme.colors`, `ThingsTheme.type`, `ThingsTheme.shapes`.")
        sb.appendLine("Значения цветов приведены в формате «светлая / тёмная тема»; одно значение — одинаково в обеих темах.")
        sb.appendLine()
        sb.appendLine("## Цвета (`ThingsColors`)")
        sb.appendLine()
        sb.appendLine("| Токен | Значение | Роль в Kotlin | Где используется |")
        sb.appendLine("| --- | --- | --- | --- |")

        colorTokens.forEach { token ->
            val lVal = token.lightGetter(light).toHexOrRgba()
            val dVal = token.darkGetter(dark).toHexOrRgba()
            val valStr = if (lVal == dVal) "`$lVal`" else "`$lVal` / `$dVal`"
            sb.appendLine("| `${token.tokenName}` | $valStr | `ThingsTheme.colors.${token.propertyName}` | ${token.usage} |")
        }

        staticPaletteTokens.forEach { token ->
            val hex = token.color.toHexOrRgba()
            sb.appendLine("| `${token.tokenName}` | `$hex` | `${token.kotlinRef}` | ${token.usage} |")
        }

        sb.appendLine()
        sb.appendLine("## Типографика (`ThingsTypography`)")
        sb.appendLine()
        sb.appendLine("Шрифт: системный шрифт (`FontFamily.Default`). На экранах шире 600 dp базовые размеры автоматически масштабируются на 1.25x внутри `ProvideThingsTheme`.")
        sb.appendLine("Все текстовые стили доступны через `ThingsTheme.type.*`.")
        sb.appendLine()
        sb.appendLine("| Роль в Kotlin | Размер | Насыщенность | Где используется |")
        sb.appendLine("| --- | --- | --- | --- |")

        typeTokens.forEach { token ->
            val style = token.getter(typo)
            val size = "${style.fontSize.value} sp"
            val weight = when (style.fontWeight?.weight) {
                700 -> "Bold (700)"
                600 -> "SemiBold (600)"
                500 -> "Medium (500)"
                else -> "Normal (400)"
            }
            sb.appendLine("| `${token.propertyName}` | $size | $weight | ${token.usage} |")
        }

        sb.appendLine()
        sb.appendLine("## Радиусы и формы (`ThingsShapes`)")
        sb.appendLine()
        sb.appendLine("Все скругления углов доступны через `ThingsTheme.shapes.*Shape` (объекты `RoundedCornerShape`) и `ThingsTheme.shapes.*` (значения в `Dp`).")
        sb.appendLine()
        sb.appendLine("| Токен | Dp | Роль Shape в Kotlin | Где используется |")
        sb.appendLine("| --- | --- | --- | --- |")

        shapeTokens.forEach { token ->
            val dpVal = "${token.getter(shapes).value} dp"
            sb.appendLine("| `${token.tokenName}` | `$dpVal` | `ThingsTheme.shapes.${token.shapeProperty}` | ${token.usage} |")
        }
        sb.appendLine("| `radius-checkbox` | `23 %` | `RoundedCornerShape(size * 0.23f)` | Скругление чекбокса — 23 % его стороны (3.7 dp у 16 dp чекбокса). |")
        sb.appendLine("| `radius-full` | `50 %` | `CircleShape` | Кнопка «+», кружки отмены, иконка-прогресс проекта. |")

        sb.appendLine()
        sb.appendLine(staticDimensionsMarkdownSection())

        return sb.toString()
    }

    fun syncDocs(docsDir: File) {
        val tokensJsonFile = File(docsDir, "tokens.json")
        val tokensMdFile = File(docsDir, "Tokens.md")

        tokensJsonFile.writeText(generateTokensJson(), Charsets.UTF_8)
        tokensMdFile.writeText(generateTokensMarkdown(), Charsets.UTF_8)
    }

    private fun staticDimensionsJsonSection(): String {
        return """
  "space": {
    "tokens": [
      { "name": "space-row-start", "value": "10px", "usage": "Отступ слева внутри строки задачи (taskRowStartPadding)." },
      { "name": "space-row-end", "value": "8px", "usage": "Отступ справа внутри строки задачи (taskRowEndPadding)." },
      { "name": "space-checkbox-to-text", "value": "8px", "usage": "От чекбокса до названия задачи (taskSpacingToTextDefault)." },
      { "name": "space-list-gutter", "value": "10px", "usage": "Боковые поля списка задач от края экрана." },
      { "name": "space-task-top", "value": "13px", "usage": "Верхний отступ свёрнутой строки задачи (taskCollapsedTopPadding)." },
      { "name": "space-task-bottom", "value": "13px", "usage": "Нижний отступ свёрнутой строки задачи (taskCollapsedBottomPadding)." },
      { "name": "space-editor-top", "value": "20px", "usage": "Верхний отступ раскрытого редактора (taskExpandedTopPadding)." },
      { "name": "space-editor-bottom", "value": "16px", "usage": "Нижний отступ раскрытого редактора (taskExpandedBottomPadding)." },
      { "name": "space-editor-gap", "value": "42px", "usage": "Зазор над и под раскрытой задачей (taskExpandedVerticalGap)." },
      { "name": "space-editor-title-notes", "value": "10px", "usage": "Между названием и заметками в редакторе (taskExpandedTitleNotesGap)." },
      { "name": "space-editor-start", "value": "12px", "usage": "Левый отступ контента раскрытого редактора (taskEditorExpandedStartPadding)." },
      { "name": "space-editor-end", "value": "18px", "usage": "Правый отступ контента раскрытого редактора (taskEditorExpandedEndPadding)." },
      { "name": "space-editor-indicators-start", "value": "24px", "usage": "Начало строки тегов, даты и дедлайна внизу редактора." },
      { "name": "space-editor-icons", "value": "16px", "usage": "Между иконками действий в панели редактора и Quick Add." },
      { "name": "space-editor-checklist-toolbar", "value": "30px", "usage": "От чек-листа до панели действий в редакторе." },
      { "name": "space-header-top", "value": "24px", "usage": "Над крупным названием экрана (mainHeaderPaddingTop)." },
      { "name": "space-header-bottom", "value": "25px", "usage": "Под крупным названием экрана (mainHeaderPaddingBottom)." },
      { "name": "space-section", "value": "32px", "usage": "Перед секцией «Сегодня вечером» и разделами области (eveningSectionSpacing)." },
      { "name": "space-heading-top", "value": "22px", "usage": "Над подзаголовком проекта." },
      { "name": "space-heading-bottom", "value": "4px", "usage": "Под подзаголовком проекта." },
      { "name": "space-calendar-section", "value": "25px", "usage": "Между блоком событий календаря и задачами." },
      { "name": "space-tags-section", "value": "11px", "usage": "Между фильтром тегов и задачами." },
      { "name": "space-fab-edge", "value": "16px", "usage": "От кнопки «+» до правого и нижнего края (слот Scaffold, Material 3)." },
      { "name": "space-toolbar-bottom", "value": "24px", "usage": "От плавающей панели действий до низа экрана." },
      { "name": "space-dialog-inner", "value": "16px", "usage": "Внутренние поля диалогов." },
      { "name": "space-list-bottom", "value": "72px", "usage": "Распорка в конце списка, чтобы последняя задача не пряталась под «+»." }
    ]
  },
  "size": {
    "note": "Размеры элементов в dp.",
    "tokens": [
      { "name": "size-checkbox", "value": "16px", "usage": "Основной чекбокс задачи (mainCheckboxSize)." },
      { "name": "size-fab", "value": "56px", "usage": "Кнопка «+»." },
      { "name": "size-fab-action", "value": "44px", "usage": "Кружки «отмена» и «во Входящие» при перетаскивании «+»." },
      { "name": "size-row-min", "value": "46px", "usage": "Высота свёрнутой строки задачи (EDITOR_COLLAPSED_HEIGHT)." },
      { "name": "size-drop-gap", "value": "44px", "usage": "Высота плашки места вставки при перетаскивании «+»." },
      { "name": "size-heading-row", "value": "40px", "usage": "Высота строки подзаголовка проекта (без отступов)." },
      { "name": "size-toolbar", "value": "50px", "usage": "Высота плавающей панели действий." },
      { "name": "size-search-field", "value": "44px", "usage": "Высота поля поиска." },
      { "name": "size-project-arc", "value": "20px", "usage": "Иконка-прогресс проекта в строке." },
      { "name": "size-swipe-icon", "value": "22px", "usage": "Иконка на фоне свайпа." },
      { "name": "size-dialog-width", "value": "320px", "usage": "Ширина диалога." }
    ]
  },
  "elevation": {
    "note": "Высота тени Compose (shadowElevation), в dp.",
    "tokens": [
      { "name": "elevation-fab", "value": "6dp", "usage": "Кнопка «+» в покое; при захвате растёт до elevation-fab-lifted." },
      { "name": "elevation-fab-lifted", "value": "18dp", "usage": "Кнопка «+», пока её тянут (6 + 12 × подъём)." },
      { "name": "elevation-card", "value": "8dp", "usage": "Раскрытый редактор задачи и перетаскиваемая карточка." },
      { "name": "elevation-stack-2", "value": "6dp", "usage": "Второй слой стопки при групповом перетаскивании." },
      { "name": "elevation-stack-3", "value": "4dp", "usage": "Третий слой стопки." }
    ]
  }
""".trimIndent()
    }

    private fun staticDimensionsMarkdownSection(): String {
        return """
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
""".trimIndent()
    }
}
