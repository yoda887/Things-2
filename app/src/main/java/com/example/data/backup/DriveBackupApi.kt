package com.example.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive REST v3, только скрытая папка приложения (appDataFolder, доступ `drive.appdata`): там одна копия
 * базы — [DriveFiles.BACKUP_NAME]. Свой клиент OkHttp, без журнала запросов — содержимое базы в журнал не попадает.
 */
@Singleton
class DriveBackupApi @Inject constructor() {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Копия базы на Drive (самая свежая, если их почему-то несколько); нет — null */
    suspend fun find(token: String): DriveFile? = withContext(Dispatchers.IO) {
        val url = "$FILES_URL?spaces=appDataFolder" +
            "&q=" + java.net.URLEncoder.encode("name='${DriveFiles.BACKUP_NAME}' and trashed=false", "UTF-8") +
            "&fields=" + java.net.URLEncoder.encode("files(id,modifiedTime,size)", "UTF-8")
        val body = call(Request.Builder().url(url).header(AUTH, bearer(token)).get().build())
        DriveFiles.parseList(body).firstOrNull()
    }

    /** Записать [file] копией базы: новый файл в скрытой папке или новое содержимое [existingId] */
    suspend fun upload(token: String, file: File, existingId: String?): String = withContext(Dispatchers.IO) {
        val media = file.asRequestBody(DB_TYPE)
        val request = if (existingId != null) {
            Request.Builder()
                .url("$UPLOAD_URL/$existingId?uploadType=media&fields=id")
                .header(AUTH, bearer(token))
                .patch(media)
                .build()
        } else {
            val metadata = """{"name":"${DriveFiles.BACKUP_NAME}","parents":["appDataFolder"]}"""
                .toRequestBody(JSON_TYPE)
            val multipart = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(metadata)
                .addPart(media)
                .build()
            Request.Builder()
                .url("$UPLOAD_URL?uploadType=multipart&fields=id")
                .header(AUTH, bearer(token))
                .post(multipart)
                .build()
        }
        DriveFiles.parseId(call(request)) ?: existingId ?: throw IOException("Drive did not return file id")
    }

    /** Скачать копию [id] в [to] */
    suspend fun download(token: String, id: String, to: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$FILES_URL/$id?alt=media").header(AUTH, bearer(token)).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw DriveHttpException(response.code)
            val source = response.body ?: throw IOException("Empty backup")
            to.outputStream().use { out -> source.byteStream().copyTo(out) }
        }
    }

    private fun call(request: Request): String =
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw DriveHttpException(response.code)
            text
        }

    private fun bearer(token: String) = "Bearer $token"

    private companion object {
        const val AUTH = "Authorization"
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        val DB_TYPE = "application/x-sqlite3".toMediaType()
        val JSON_TYPE = "application/json; charset=UTF-8".toMediaType()
    }
}

/** Drive ответил ошибкой: 401 — доступ истёк или отозван, 403 — нет разрешения или превышен лимит */
class DriveHttpException(val code: Int) : IOException("Google Drive HTTP $code")
