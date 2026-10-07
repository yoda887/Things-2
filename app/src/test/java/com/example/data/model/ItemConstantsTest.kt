package com.example.data.model

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Статус, раздел, тип и приоритет задачи — только именованными константами Item
 * (STATUS_*, START_*, TYPE_*, PRIORITY_*). Число на месте (`status == 2`, `start = 3`)
 * легко перепутать, и ошибка не видна ни компилятору, ни при чтении.
 */
class ItemConstantsTest {

    private val magicNumberRegex = Regex("""\b(status|start|type|priority)\s*(==|!=|=)\s*\d+\b(?!\.)""")

    private fun sourceDirectory(): File = listOf(
        File("src/main/java"), File("app/src/main/java"), File("../app/src/main/java")
    ).firstOrNull { it.isDirectory } ?: throw IllegalStateException("Не найдена папка src/main/java")

    @Test
    fun noMagicNumbersForItemFields() {
        val violations = mutableListOf<String>()
        sourceDirectory().walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val code = line.substringBefore("//")
                if (code.trim().startsWith("*")) return@forEachIndexed
                if (magicNumberRegex.containsMatchIn(code)) violations.add("${file.name}:${index + 1}: ${line.trim()}")
            }
        }
        assertTrue(
            "Статус / раздел / тип / приоритет задачи числом. Используйте Item.STATUS_*, START_*, TYPE_*, PRIORITY_*:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
