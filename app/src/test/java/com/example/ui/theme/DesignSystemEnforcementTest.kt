package com.example.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Архитектурный тест дизайн-системы Things.
 *
 * Проверяет, что ни один UI-компонент за пределами `ui/theme/` не использует:
 * 1. Сырые шестнадцатеричные цвета: `Color(0x...)`
 * 2. Хардкодные размеры шрифтов: `fontSize = ...sp`
 * 3. Хардкодные скругления углов: `RoundedCornerShape(...dp)`
 * 4. Прямые вызовы проверки системной темы: `isSystemInDarkTheme()`
 * 5. Константы сырой палитры из `Color.kt`
 *
 * Все визуальные параметры должны получаться исключительно через роли `ThingsTheme`.
 */
class DesignSystemEnforcementTest {

    private val colorHexRegex = Regex("""Color\s*\(\s*0x[0-9a-fA-F]+""")
    private val fontSizeRegex = Regex("""fontSize\s*=\s*[0-9]+(?:\.[0-9]+)?\.sp""")
    private val roundedCornerShapeRegex = Regex("""RoundedCornerShape\s*\(\s*[0-9]+(?:\.[0-9]+)?\.dp\s*\)""")
    private val isSystemDarkRegex = Regex("""\bisSystemInDarkTheme\s*\(""")

    private val rawPaletteConstants = listOf(
        "ThingsBackgroundLight", "ThingsBackgroundDark",
        "ThingsSurfaceLight", "ThingsSurfaceDark",
        "ThingsTextPrimaryLight", "ThingsTextPrimaryDark",
        "ThingsTextSecondaryLight", "ThingsTextSecondaryDark",
        "ThingsTextNotesLight",
        "ThingsDividerLight", "ThingsDividerDark",
        "ThingsInk",
        "ThingsMetaGrey",
        "ThingsCheckboxBorder",
        "ThingsEditorIconInactive",
        "ThingsFieldLight", "ThingsFieldDark",
        "ThingsHairlineLight", "ThingsHairlineDark",
        "ThingsMutedGreyLight", "ThingsMutedGreyDark",
        "ThingsDropPlaceholderLight", "ThingsDropPlaceholderDark",
        "ThingsHomeDropPlaceholder",
        "ThingsStackCardDark",
        "ThingsDialogBackground", "ThingsDialogButton", "ThingsDialogRowSelected", "ThingsWhenClear",
        "ThingsTagChipBackground", "ThingsTagChipText",
        "ThingsBadgeTextLight", "ThingsBadgeTextDark",
        "ThingsBadgeBackgroundLight", "ThingsBadgeBackgroundDark",
        "ThingsPullArrowLight", "ThingsPullArrowDark",
        "ThingsPullArrowSelectedLight", "ThingsPullArrowSelectedDark",
        "ThingsListDimLight", "ThingsListDimDark",
        "SearchMatchHighlightLight", "SearchMatchHighlightDark"
    )

    private val rawPaletteRegex = Regex("""\b(${rawPaletteConstants.joinToString("|")})\b""")

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
