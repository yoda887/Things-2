package com.example.data.sync

import com.example.data.local.AppDatabase
import com.example.data.sync.SyncMerge.Key
import com.example.data.sync.SyncMerge.State
import com.example.data.sync.SyncMerge.SyncDeletionMark
import com.example.data.sync.SyncMerge.SyncRow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Общее состояние синхронизации в файле на Drive: JSON, сжатый gzip. Закреплено тестами SyncCodecTest.
 *
 * `{"v":1, "schema":10, "rows":{"items":[{поля}], …}, "deletions":[{"t":"items","id":"…","at":…}]}` — поля записи
 * как в базе: текст — строкой, целые и вещественные — числом, NULL — null.
 *
 * `schema` — версия базы ([AppDatabase.VERSION]) приложения, записавшего файл. Файл из более новой версии
 * (в записях могут быть столбцы, которых здесь нет) это приложение не применяет и не перезаписывает: просит
 * обновиться. Файл без `schema` — из версии [FIRST_SCHEMA], с которой синхронизация появилась.
 * Код одинаковый в Things 2 и Things Desktop.
 */
object SyncCodec {

    private const val VERSION = 1

    /** Версия базы этого приложения */
    const val SCHEMA = AppDatabase.VERSION

    /** Версия базы, с которой появилась синхронизация: у файлов без `schema` */
    private const val FIRST_SCHEMA = 10L

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(state: State, schema: Int = SCHEMA): ByteArray {
        val root = buildJsonObject {
            put("v", VERSION)
            put("schema", schema)
            put("rows", buildJsonObject {
                state.rows.values.groupBy { it.key.table }.forEach { (table, rows) ->
                    put(table, buildJsonArray { rows.forEach { add(rowToJson(it.values)) } })
                }
            })
            put("deletions", buildJsonArray {
                state.deletions.values.forEach { mark ->
                    add(buildJsonObject {
                        put("t", mark.key.table)
                        put("id", mark.key.id)
                        put("at", mark.deletedAt)
                    })
                }
            })
        }
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    /** Разобрать файл; не тот формат, новее известного или из более новой версии базы, чем [schema], — null (ничего не трогать) */
    fun decode(bytes: ByteArray, schema: Int = SCHEMA): State? = runCatching {
        val root = root(bytes)
        val version = root["v"]?.jsonPrimitive?.long ?: return null
        if (version > VERSION) return null
        if (schemaOf(root) > schema) return null
        val rows = HashMap<Key, SyncRow>()
        (root["rows"] as? JsonObject)?.forEach { (table, array) ->
            (array as? JsonArray)?.forEach { element ->
                val values = (element as? JsonObject)?.mapValues { (_, v) -> fromJson(v) } ?: return@forEach
                val id = SyncTables.keyOf(table, values) ?: return@forEach
                val key = Key(table, id)
                rows[key] = SyncRow(key, (values["updatedAt"] as? Long) ?: 0L, values)
            }
        }
        val deletions = HashMap<Key, SyncDeletionMark>()
        (root["deletions"] as? JsonArray)?.forEach { element ->
            val mark = element as? JsonObject ?: return@forEach
            val key = Key(mark["t"]?.jsonPrimitive?.content ?: return@forEach, mark["id"]?.jsonPrimitive?.content ?: return@forEach)
            deletions[key] = SyncDeletionMark(key, mark["at"]?.jsonPrimitive?.long ?: return@forEach)
        }
        State(rows, deletions)
    }.getOrNull()

    /** Файл записан более новой версией приложения (формат или база новее) — обновить приложение на этом устройстве */
    fun isFromNewerApp(bytes: ByteArray, schema: Int = SCHEMA): Boolean = runCatching {
        val root = root(bytes)
        (root["v"]?.jsonPrimitive?.long ?: 0L) > VERSION || schemaOf(root) > schema
    }.getOrDefault(false)

    private fun root(bytes: ByteArray): JsonObject {
        val text = GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes().toString(Charsets.UTF_8) }
        return json.parseToJsonElement(text) as JsonObject
    }

    private fun schemaOf(root: JsonObject): Long = root["schema"]?.jsonPrimitive?.long ?: FIRST_SCHEMA

    private fun rowToJson(values: Map<String, Any?>): JsonObject = JsonObject(
        values.mapValues { (_, value) ->
            when (value) {
                null -> JsonNull
                is String -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value.toLong())
                is Double -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value.toDouble())
                is Boolean -> JsonPrimitive(if (value) 1L else 0L)
                else -> JsonPrimitive(value.toString())
            }
        }
    )

    private fun fromJson(element: kotlinx.serialization.json.JsonElement): Any? {
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: return element.toString()
        if (primitive.isString) return primitive.content
        val content = primitive.content
        return if (content.any { it == '.' || it == 'e' || it == 'E' }) content.toDouble() else content.toLong()
    }
}

/** Таблицы синхронизации и ключи их записей */
object SyncTables {

    /** В этом порядке записи вставляются (сначала те, на кого ссылаются); удаляются — в обратном */
    val ORDER = listOf("areas", "tags", "items", "item_tags", "checklist_items", "recurrence_rules")

    /** Id записи: у связей задача–тег — «itemId|tagId», у остальных — поле id */
    fun keyOf(table: String, values: Map<String, Any?>): String? =
        if (table == "item_tags") {
            val item = values["itemId"] as? String
            val tag = values["tagId"] as? String
            if (item != null && tag != null) "$item|$tag" else null
        } else values["id"] as? String
}
