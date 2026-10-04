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

    @Test
    fun generateDocs() {
        // Удобный тестовый метод для запуска генерации напрямую:
        // .\gradlew testDebugUnitTest --tests com.example.ui.theme.DesignSystemDocSyncTest.generateDocs
        val docsDir = getDocsDirectory()
        DesignSystemDocGenerator.syncDocs(docsDir)
        println("Документация успешно сгенерирована в: ${docsDir.absolutePath}")
    }
}
