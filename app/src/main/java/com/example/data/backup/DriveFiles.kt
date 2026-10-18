package com.example.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Файл резервной копии в скрытой папке приложения на Google Drive */
data class DriveFile(val id: String, val modifiedTime: String?, val size: Long?, val md5: String? = null)

/** Разбор ответов Drive REST v3 — без сети, закреплено тестами DriveBackupTest. */
object DriveFiles {

    /** Имя копии базы в скрытой папке приложения (appDataFolder) */
    const val BACKUP_NAME = "things_backup.db"

    /** Имя общего состояния синхронизации устройств там же */
    const val SYNC_NAME = "sync_state.json.gz"

    private val json = Json { ignoreUnknownKeys = true }

    /** Ответ `files.list`: файлы копии, свежие сверху */
    fun parseList(body: String): List<DriveFile> {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return emptyList()
        val files = root["files"] as? JsonArray ?: return emptyList()
        return files.mapNotNull { element ->
            val file = element as? JsonObject ?: return@mapNotNull null
            val id = file["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            DriveFile(
                id = id,
                modifiedTime = file["modifiedTime"]?.jsonPrimitive?.content,
                size = file["size"]?.jsonPrimitive?.longOrNull,
                md5 = file["md5Checksum"]?.jsonPrimitive?.content
            )
        }.sortedByDescending { it.modifiedTime }
    }

    /** Ответ `about.get?fields=user(emailAddress)`: адрес аккаунта Google */
    fun parseEmail(body: String): String? =
        ((json.parseToJsonElement(body) as? JsonObject)?.get("user") as? JsonObject)?.get("emailAddress")?.jsonPrimitive?.content

    /** Ответ `files.create` / `files.update`: id файла */
    fun parseId(body: String): String? =
        (json.parseToJsonElement(body) as? JsonObject)?.get("id")?.jsonPrimitive?.content

    /** Время Drive (RFC 3339, «2026-10-18T09:15:02.123Z») — в миллисекундах; не разобрать — null */
    fun parseTime(value: String?): Long? = value?.let {
        runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull()
    }
}
