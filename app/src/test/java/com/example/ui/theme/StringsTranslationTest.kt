package com.example.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Интерфейс — на языке системы: у каждой строки и plurals из res/values есть перевод
 * в values-uk и values-ru (кроме помеченных translatable="false").
 */
class StringsTranslationTest {

    private fun resDirectory(): File = listOf(
        File("src/main/res"), File("app/src/main/res"), File("../app/src/main/res")
    ).firstOrNull { it.isDirectory } ?: throw IllegalStateException("Не найдена папка res")

    private fun keys(file: File, onlyTranslatable: Boolean): Set<String> {
        val text = file.readText(Charsets.UTF_8)
        val strings = Regex("""<string name="([^"]+)"([^>]*)>""").findAll(text)
            .filter { !onlyTranslatable || !it.groupValues[2].contains("translatable=\"false\"") }
            .map { it.groupValues[1] }
        val plurals = Regex("""<plurals name="([^"]+)"""").findAll(text).map { it.groupValues[1] }
        return (strings + plurals).toSet()
    }

    @Test
    fun everyStringIsTranslated() {
        val res = resDirectory()
        val base = keys(File(res, "values/strings.xml"), onlyTranslatable = true)
        val missing = listOf("values-uk", "values-ru").flatMap { dir ->
            val translated = keys(File(res, "$dir/strings.xml"), onlyTranslatable = false)
            (base - translated).map { "$dir: $it" }
        }
        assertTrue(
            "Нет перевода строк (добавьте в values-uk и values-ru):\n" + missing.sorted().joinToString("\n"),
            missing.isEmpty()
        )
    }
}
