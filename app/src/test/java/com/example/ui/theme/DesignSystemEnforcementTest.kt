package com.example.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Архитектурный тест дизайн-системы Things.
 *
 * Проверяет, что ни один UI-компонент за пределами `ui/theme/` не использует:
 * 1. Сырые шестнадцатеричные цвета: `Color(0x...)`
 * 2. Числа в sp: `fontSize = 17.sp`, `letterSpacing = 1.sp` и любое `N.sp`
 * 3. Скругления не из ролей: `RoundedCornerShape(8.dp)`, `RoundedCornerShape(SOME_CONST)`, `RoundedCornerShape(MaterialTheme.dimens…)`
 * 4. Прямые вызовы проверки системной темы: `isSystemInDarkTheme()`
 * 5. Любые константы палитры из `Color.kt` (список читается из самого файла)
 * 6. Именованные цвета Compose: `Color.White`, `Color.Black`, `Color.Gray` и т. п. (`Transparent` и `Unspecified` разрешены)
 * 7. Стили `MaterialTheme.typography` в обход `ThingsTheme.type`
 *
 * Все визуальные параметры должны получаться исключительно через роли `ThingsTheme`.
 */
class DesignSystemEnforcementTest {

    private val colorHexRegex = Regex("""Color\s*\(\s*0x[0-9a-fA-F]+""")
    private val fontSizeRegex = Regex("""(?<![\w.)])[0-9]+(?:\.[0-9]+)?f?\.sp\b""")
    private val roundedCornerShapeRegex = Regex("""RoundedCornerShape\s*\(\s*(?:[0-9]|[A-Z][A-Z0-9_]+\b|MaterialTheme\b)""")
    private val namedColorRegex = Regex("""\bColor\.(White|Black|Gray|LightGray|DarkGray|Red|Green|Blue|Yellow|Cyan|Magenta)\b""")
    private val materialTypographyRegex = Regex("""\bMaterialTheme\.typography\b""")
    private val materialAlertDialogRegex = Regex("""\bAlertDialog\s*\(""")
    // Длительность — из ThingsMotion или именованной константы файла, не числом на месте
    private val durationLiteralRegex = Regex("""(durationMillis\s*=\s*|\btween\(\s*|\bdelay\(\s*|delayMillis\s*=\s*)\d""")
    // Прозрачность — из ThingsAlpha или именованной константы файла
    private val alphaLiteralRegex = Regex("""copy\(\s*alpha\s*=\s*\d+(\.\d+)?f?\s*\)""")
    private val iconSizeLiteralRegex = Regex("""\.size\(\s*\d+(\.\d+)?\.dp\s*\)""")
    // Разрешено только MaterialTheme.colorScheme.copy(...) — подмена схемы под компонент темы (ThingsMenu)
    private val materialColorSchemeRegex = Regex("""\bMaterialTheme\.colorScheme\b(?!\.copy\()""")
    // Видимый текст и подписи для экранного диктора — только из strings.xml. Одиночная буква
    // (невидимая строка для замера высоты) не считается надписью
    private val hardcodedUiTextRegex = Regex(
        """(Text\(\s*(text\s*=\s*)?|(?<![\w.])text\s*=\s*|contentDescription\s*=\s*|ThingsMenuItem\(\s*)"[^"]*\p{L}{2,}"""
    )
    private val isSystemDarkRegex = Regex("""\bisSystemInDarkTheme\s*\(""")

    /** Все константы палитры Color.kt: компоненты берут цвета только через роли ThingsTheme.colors */
    private val rawPaletteConstants: List<String> by lazy {
        val colorKt = File(getUiSourceDirectory(), "theme/Color.kt")
        Regex("""^val (\w+)""", RegexOption.MULTILINE).findAll(colorKt.readText()).map { it.groupValues[1] }.toList()
    }

    private val rawPaletteRegex by lazy { Regex("""\b(${rawPaletteConstants.joinToString("|")})\b""") }

    private fun getUiSourceDirectory(): File {
        val candidatePaths = listOf(
            File("src/main/java/com/example/ui"),
            File("app/src/main/java/com/example/ui"),
            File("../app/src/main/java/com/example/ui")
        )
        return candidatePaths.firstOrNull { it.exists() && it.isDirectory }
            ?: throw IllegalStateException("Не удалось найти директорию com/example/ui ни по одному из путей")
    }

    private fun getUiFilesToValidate(): List<File> {
        val uiDir = getUiSourceDirectory()
        return uiDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { file ->
                val normalizedPath = file.path.replace('\\', '/')
                normalizedPath.contains("/ui/theme/")
            }
            .toList()
    }

    private fun violationsOf(regex: Regex): List<String> {
        val violations = mutableListOf<String>()
        getUiFilesToValidate().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("//") && !trimmed.startsWith("*") && !trimmed.startsWith("import ")) {
                    if (regex.containsMatchIn(line)) violations.add("${file.name}:${index + 1}: $trimmed")
                }
            }
        }
        return violations
    }

    @Test
    fun noNamedComposeColorsInUiComponents() {
        val violations = violationsOf(namedColorRegex)
        assertTrue(
            "Обнаружены именованные цвета Color.White / Color.Black / Color.Gray… в UI компонентах. " +
                "Используйте роли: onAccent, overlayContent, scrim, textSecondary…:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noMaterialTypographyInUiComponents() {
        val violations = violationsOf(materialTypographyRegex)
        assertTrue(
            "Обнаружены стили MaterialTheme.typography в UI компонентах. Используйте роли ThingsTheme.type.*:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noMaterialAlertDialogInUiComponents() {
        val violations = violationsOf(materialAlertDialogRegex)
        assertTrue(
            "Обнаружен стандартный Material AlertDialog. Подтверждения — только общий тёмный ThingsConfirmDialog:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noMaterialColorSchemeInUiComponents() {
        val violations = violationsOf(materialColorSchemeRegex)
        assertTrue(
            "Обнаружены цвета MaterialTheme.colorScheme в UI компонентах. Используйте роли ThingsTheme.colors.*:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noHardcodedUiTextInUiComponents() {
        val violations = violationsOf(hardcodedUiTextRegex)
        assertTrue(
            "Обнаружены надписи прямо в коде. Вынесите их в res/values/strings.xml и используйте stringResource:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noDurationLiteralsInUiComponents() {
        val violations = violationsOf(durationLiteralRegex)
        assertTrue(
            "Длительность анимации числом на месте. Используйте ThingsMotion или именованную константу файла:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noAlphaLiteralsInUiComponents() {
        val violations = violationsOf(alphaLiteralRegex)
        assertTrue(
            "Прозрачность числом на месте. Используйте ThingsAlpha или именованную константу файла:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    /** Размер иконки (строка .size(N.dp) в пределах шести строк после Icon( ) — из ThingsIconSize или константы файла */
    @Test
    fun noIconSizeLiteralsInUiComponents() {
        val violations = mutableListOf<String>()
        getUiFilesToValidate().forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if (!iconSizeLiteralRegex.containsMatchIn(line)) return@forEachIndexed
                val context = lines.subList(maxOf(0, index - 6), index + 1).joinToString("\n")
                if (Regex("""\bIcon\(""").containsMatchIn(context)) {
                    violations.add("${file.name}:${index + 1}: ${line.trim()}")
                }
            }
        }
        assertTrue(
            "Размер иконки числом на месте. Используйте ThingsIconSize или именованную константу файла:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    /**
     * Экраны, переведённые на шкалы целиком: в них нет ни одного числа в dp и ни одного
     * начертания поверх стиля — только ThingsSpacing / ThingsElevation / ThingsIconSize,
     * MaterialTheme.dimens, роли ThingsTheme.type или именованные константы файла (`private val X = N.dp`).
     * Список растёт по мере перевода экранов.
     */
    private val fullyMigratedFiles = setOf("HomePanel.kt")

    @Test
    fun migratedScreensHaveNoDpLiteralsOrWeightOverrides() {
        val dpLiteral = Regex("""(?<![\w.])\d+(\.\d+)?\.dp\b""")
        val weightOverride = Regex("""fontWeight\s*=\s*FontWeight\.""")
        val violations = mutableListOf<String>()
        getUiFilesToValidate().filter { it.name in fullyMigratedFiles }.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val code = line.substringBefore("//").trim()
                if (code.startsWith("private val ") || code.startsWith("private const val ") || code.startsWith("*")) return@forEachIndexed
                if (dpLiteral.containsMatchIn(code) || weightOverride.containsMatchIn(code)) {
                    violations.add("${file.name}:${index + 1}: ${line.trim()}")
                }
            }
        }
        assertTrue(
            "В переведённом на шкалы экране число в dp или начертание поверх стиля. " +
                "Используйте ThingsSpacing / ThingsElevation / ThingsIconSize, MaterialTheme.dimens, роль ThingsTheme.type " +
                "или именованную константу файла:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun paletteParsedFromColorKt() {
        assertTrue("Не удалось прочитать константы палитры из Color.kt", rawPaletteConstants.size > 10)
    }

    @Test
    fun noHexColorLiteralsInUiComponents() {
        val violations = mutableListOf<String>()

        getUiFilesToValidate().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                    if (colorHexRegex.containsMatchIn(line)) {
                        violations.add("${file.name}:${index + 1}: $trimmed")
                    }
                }
            }
        }

        assertTrue(
            "Обнаружены сырые шестнадцатеричные цвета Color(0x...) в UI компонентах:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noFontSizeLiteralsInUiComponents() {
        val violations = mutableListOf<String>()

        getUiFilesToValidate().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                    if (fontSizeRegex.containsMatchIn(line)) {
                        violations.add("${file.name}:${index + 1}: $trimmed")
                    }
                }
            }
        }

        assertTrue(
            "Обнаружены хардкодные размеры шрифтов fontSize = ...sp в UI компонентах. " +
                "Используйте роли ThingsTheme.type.*:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noRoundedCornerShapeDpLiteralsInUiComponents() {
        val violations = mutableListOf<String>()

        getUiFilesToValidate().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                    if (roundedCornerShapeRegex.containsMatchIn(line)) {
                        violations.add("${file.name}:${index + 1}: $trimmed")
                    }
                }
            }
        }

        assertTrue(
            "Обнаружены хардкодные скругления RoundedCornerShape(N.dp) в UI компонентах. " +
                "Используйте роли ThingsTheme.shapes.*:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noDirectIsSystemInDarkThemeInUiComponents() {
        val violations = mutableListOf<String>()

        getUiFilesToValidate().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                    if (isSystemDarkRegex.containsMatchIn(line)) {
                        violations.add("${file.name}:${index + 1}: $trimmed")
                    }
                }
            }
        }

        assertTrue(
            "Обнаружены прямые вызовы isSystemInDarkTheme() в UI компонентах. " +
                "Выбор темы должен происходить централизованно в ThingsTheme, либо используйте ThingsTheme.colors.isDark:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun noRawPaletteConstantsInUiComponents() {
        val violations = mutableListOf<String>()

        getUiFilesToValidate().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                    if (rawPaletteRegex.containsMatchIn(line)) {
                        violations.add("${file.name}:${index + 1}: $trimmed")
                    }
                }
            }
        }

        assertTrue(
            "Обнаружены ссылки на константы сырой палитры из Color.kt в UI компонентах. " +
                "Используйте семантические роли ThingsTheme.colors.*:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
