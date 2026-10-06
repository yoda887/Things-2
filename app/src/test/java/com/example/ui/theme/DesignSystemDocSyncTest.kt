package com.example.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Архитектурный тест синхронизации документации дизайн-системы с кодовой базой.
 *
 * Проверяет, что:
 * 1. 100% свойств из [ThingsColors], [ThingsTypography] и [ThingsShapes] учтены в спецификации.
 * 2. Файлы `docs/design-system/tokens.json` и `docs/design-system/Tokens.md` в точности
 *    соответствуют текущему Kotlin-коду и не разошлись со спецификацией.
 *
 * Если файлы разошлись, тест падает и сообщает команду для автоматической генерации:
 * `.\gradlew testDebugUnitTest --tests com.example.ui.theme.DesignSystemDocSyncTest -DgenerateDesignDocs=true`
 */
class DesignSystemDocSyncTest {

    private fun getDocsDirectory(): File {
        val candidates = listOf(
            File("docs/design-system"),
            File("../docs/design-system"),
            File("../../docs/design-system")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: throw IllegalStateException("Не удалось найти директорию docs/design-system")
    }

    @Test
    fun allThemePropertiesAreDocumented() {
        // Проверяет через Kotlin Reflection полноту покрытия свойств ThingsColors, ThingsTypography, ThingsShapes
        DesignSystemDocGenerator.validateCompleteness()
    }

    @Test
    fun documentationIsSynchronizedWithCode() {
        val docsDir = getDocsDirectory()
        val shouldGenerate = System.getProperty("generateDesignDocs")?.toBoolean() ?: false

        if (shouldGenerate) {
            DesignSystemDocGenerator.syncDocs(docsDir)
            println("Документация дизайн-системы успешно сгенерирована из Kotlin-кода темы!")
            return
        }

        val tokensJsonFile = File(docsDir, "tokens.json")
        val tokensMdFile = File(docsDir, "Tokens.md")

        assertTrue("Файл docs/design-system/tokens.json должен существовать", tokensJsonFile.exists())
        assertTrue("Файл docs/design-system/Tokens.md должен существовать", tokensMdFile.exists())

        val expectedJson = DesignSystemDocGenerator.generateTokensJson()
        val actualJson = tokensJsonFile.readText(Charsets.UTF_8).replace("\r\n", "\n")

        val expectedMd = DesignSystemDocGenerator.generateTokensMarkdown()
        val actualMd = tokensMdFile.readText(Charsets.UTF_8).replace("\r\n", "\n")

        val mismatchHelpMessage = "\n\n" +
            "╔═════════════════════════════════════════════════════════════════════════════════╗\n" +
            "║ РАССИНХРОНИЗАЦИЯ ДОКУМЕНТАЦИИ И КОДА ДИЗАЙН-СИСТЕМЫ!                            ║\n" +
            "║ Файлы docs/design-system/ не соответствуют актуальным Kotlin-классам темы.      ║\n" +
            "║                                                                                 ║\n" +
            "║ Для автоматической перегенерации документации выполните команду:                ║\n" +
            "║ .\\gradlew testDebugUnitTest --tests *DesignSystemDocSyncTest                   ║\n" +
            "║          -DgenerateDesignDocs=true                                              ║\n" +
            "╚═════════════════════════════════════════════════════════════════════════════════╝\n"

        assertEquals(
            "docs/design-system/tokens.json не синхронизирован с ThingsTheme!$mismatchHelpMessage",
            expectedJson.replace("\r\n", "\n").trim(),
            actualJson.trim()
        )

        assertEquals(
            "docs/design-system/Tokens.md не синхронизирован с ThingsTheme!$mismatchHelpMessage",
            expectedMd.replace("\r\n", "\n").trim(),
            actualMd.trim()
        )
    }

    /**
     * Ручные документы (README.md, Motion.md и описания компонентов) не генерируются, поэтому проверяется,
     * что каждый токен, на который они ссылаются, существует: имя в обратных кавычках с дефисом
     * (`text-secondary`) — в tokens.json, полная роль (`ThingsTheme.colors.accent`) —
     * свойство ThingsColors / ThingsTypography / ThingsShapes. Имена параметров и API Compose не проверяются.
     */
    @Test
    fun handWrittenDocsReferenceExistingTokens() {
        val docsDir = getDocsDirectory()
        val tokenNames = Regex(""""name": "([^"]+)"""").findAll(File(docsDir, "tokens.json").readText(Charsets.UTF_8))
            .map { it.groupValues[1] }.toSet()
        val roleNames = (ThingsColors::class.members + ThingsTypography::class.members + ThingsShapes::class.members)
            .map { it.name }.toSet()
        // Не токены: пути, имена файлов, константы вибрации и т. п.
        val ignored = Regex("""[./]|^(kebab-case|light|dark)$""")
        val files = listOf(File(docsDir, "README.md"), File(docsDir, "Motion.md")) +
            (File(docsDir, "components").listFiles { f -> f.extension == "md" }?.toList() ?: emptyList())
        val missing = mutableListOf<String>()
        files.forEach { file ->
            Regex("""`([^`\s]+)`""").findAll(file.readText(Charsets.UTF_8)).forEach { m ->
                val ref = m.groupValues[1]
                when {
                    Regex("""^ThingsTheme\.(colors|type|shapes)\.(\w+)$""").matches(ref) -> {
                        val name = ref.substringAfterLast('.')
                        if (name !in roleNames) missing.add("${file.name}: $ref")
                    }
                    ignored.containsMatchIn(ref) -> Unit
                    Regex("""^[a-z][a-z0-9]*(-[a-z0-9]+)+$""").matches(ref) -> if (ref !in tokenNames) missing.add("${file.name}: $ref")
                }
            }
        }
        assertTrue("Ручные документы ссылаются на несуществующие токены:\n" + missing.joinToString("\n"), missing.isEmpty())
    }

    @Test
    fun generateDocs() {
        // Удобный тестовый метод для запуска генерации напрямую:
        // .\gradlew testDebugUnitTest --tests com.example.ui.theme.DesignSystemDocSyncTest.generateDocs
        val docsDir = getDocsDirectory()
        DesignSystemDocGenerator.syncDocs(docsDir)
        println("Документация успешно сгенерирована в: ${docsDir.absolutePath}")
    }
}
