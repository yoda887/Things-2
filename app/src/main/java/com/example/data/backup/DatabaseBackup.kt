package com.example.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Файл базы для резервной копии: целостный снимок и замена базы копией. Room пишет в режиме WAL, поэтому перед
 * копированием журнал сливается в основной файл (`wal_checkpoint(TRUNCATE)`), иначе в копию не попали бы
 * последние изменения.
 */
@Singleton
class DatabaseBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    /** Снимок базы во временный файл — его и отправляют на Drive */
    suspend fun snapshot(): File = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val target = File(context.cacheDir, DriveFiles.BACKUP_NAME)
        context.getDatabasePath(DATABASE_NAME).copyTo(target, overwrite = true)
        target
    }

    /**
     * Годится ли [file] для восстановления: открывается как база SQLite, в ней таблицы приложения, а её версия
     * не новее нынешней (старые Room доведёт миграциями при запуске).
     */
    fun isRestorable(file: File): Boolean = runCatching {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val tables = HashSet<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }
            BackupRules.isRestorable(db.version, CURRENT_VERSION, tables)
        }
    }.getOrDefault(false)

    /**
     * Заменить базу копией [file]. Нынешняя база сначала сохраняется рядом (`backups/before_restore.db`) — если
     * копия окажется не той, данные не потеряны. После замены приложение нужно перезапустить: открытая база закрыта.
     */
    suspend fun restore(file: File) = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val safety = File(File(context.filesDir, LOCAL_BACKUPS), BEFORE_RESTORE).apply { parentFile?.mkdirs() }
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        dbFile.copyTo(safety, overwrite = true)
        database.close()
        file.copyTo(dbFile, overwrite = true)
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }

    companion object {
        /** Имя базы Room (DatabaseModule) */
        const val DATABASE_NAME = "things_database"

        /** Версия схемы Room */
        const val CURRENT_VERSION = AppDatabase.VERSION

        private const val LOCAL_BACKUPS = "backups"
        private const val BEFORE_RESTORE = "before_restore.db"
    }
}

/** Правила восстановления — без Android, закреплено тестами DriveBackupTest */
object BackupRules {

    /** Таблицы, без которых копия — не база приложения */
    val REQUIRED_TABLES = setOf("items", "areas", "tags")

    fun isRestorable(version: Int, currentVersion: Int, tables: Set<String>): Boolean =
        version in 1..currentVersion && tables.containsAll(REQUIRED_TABLES)
}
