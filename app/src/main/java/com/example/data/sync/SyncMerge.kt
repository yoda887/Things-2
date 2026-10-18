package com.example.data.sync

/**
 * Слияние состояний двух устройств — без базы и сети, закреплено тестами SyncMergeTest.
 *
 * Запись таблицы — набор полей ([SyncRow.values]) с временем последнего изменения ([SyncRow.updatedAt]); удаление
 * насовсем — запись журнала ([SyncDeletionMark]). По каждому ключу «таблица + id» побеждает самое позднее событие:
 * правка или удаление. При равном времени правка этого устройства остаётся (результат один и тот же с обеих сторон,
 * когда они обменяются).
 */
object SyncMerge {

    /** Ключ записи: таблица и id (у связей задача–тег — «itemId|tagId») */
    data class Key(val table: String, val id: String)

    data class SyncRow(val key: Key, val updatedAt: Long, val values: Map<String, Any?>)
    data class SyncDeletionMark(val key: Key, val deletedAt: Long)

    /** Состояние устройства или общего файла: живые записи и журнал удалений */
    data class State(val rows: Map<Key, SyncRow>, val deletions: Map<Key, SyncDeletionMark>) {
        companion object {
            val EMPTY = State(emptyMap(), emptyMap())
        }
    }

    /**
     * Итог слияния: [merged] — общее состояние после обмена; [applyLocally] — записи с другого устройства, новее
     * своих (вставить или заменить у себя); [deleteLocally] — свои записи, удалённые там позже их правки;
     * [remoteChanged] — в общем файле есть что-то новое от этого устройства, его нужно записать.
     */
    data class Result(
        val merged: State,
        val applyLocally: List<SyncRow>,
        val deleteLocally: List<Key>,
        val remoteChanged: Boolean
    )

    /**
     * Время изменения образцов новой базы (DatabaseSeeder): самое раннее — любая правка или удаление их перебивает.
     * Новое устройство, подключаясь к уже общему файлу, своих образцов, которых в нём нет, не отправляет, а убирает
     * у себя: оно принимает общее состояние, а не возвращает давно удалённые образцы на все устройства.
     */
    const val SEED_TIME = 1L

    fun merge(local: State, remote: State): Result {
        val sharedExists = remote.rows.isNotEmpty() || remote.deletions.isNotEmpty()
        val seedsToDrop = if (!sharedExists) emptySet() else local.rows.values
            .filter { it.updatedAt <= SEED_TIME && it.key !in remote.rows && it.key !in remote.deletions }
            .mapTo(HashSet()) { it.key }
        if (seedsToDrop.isNotEmpty()) {
            val adopted = merge(local.copy(rows = local.rows - seedsToDrop), remote)
            return adopted.copy(deleteLocally = adopted.deleteLocally + seedsToDrop)
        }
        val keys = local.rows.keys + local.deletions.keys + remote.rows.keys + remote.deletions.keys
        val rows = HashMap<Key, SyncRow>()
        val deletions = HashMap<Key, SyncDeletionMark>()
        val apply = ArrayList<SyncRow>()
        val delete = ArrayList<Key>()
        var remoteChanged = false

        for (key in keys) {
            val winner = latest(
                local.rows[key]?.let { Event.Row(it, mine = true) },
                local.deletions[key]?.let { Event.Deletion(it, mine = true) },
                remote.rows[key]?.let { Event.Row(it, mine = false) },
                remote.deletions[key]?.let { Event.Deletion(it, mine = false) }
            ) ?: continue

            when (winner) {
                is Event.Row -> {
                    rows[key] = winner.row
                    if (!winner.mine) apply += winner.row
                    else if (remote.rows[key]?.updatedAt != winner.row.updatedAt || key in remote.deletions) remoteChanged = true
                }
                is Event.Deletion -> {
                    deletions[key] = winner.mark
                    if (!winner.mine && key in local.rows) delete += key
                    if (winner.mine && (remote.deletions[key]?.deletedAt != winner.mark.deletedAt || key in remote.rows)) {
                        remoteChanged = true
                    }
                }
            }
        }
        return Result(State(rows, deletions), apply, delete, remoteChanged)
    }

    /** Удаления старше этого срока больше не нужны: все устройства давно о них узнали */
    fun pruneDeletions(state: State, now: Long, keepMillis: Long = DELETION_KEEP_MS): State =
        state.copy(deletions = state.deletions.filterValues { now - it.deletedAt <= keepMillis })

    private sealed interface Event {
        val at: Long
        val mine: Boolean

        data class Row(val row: SyncRow, override val mine: Boolean) : Event {
            override val at get() = row.updatedAt
        }

        data class Deletion(val mark: SyncDeletionMark, override val mine: Boolean) : Event {
            override val at get() = mark.deletedAt
        }
    }

    /** Самое позднее событие; при равном времени — своё раньше чужого, правка раньше удаления */
    private fun latest(vararg events: Event?): Event? = events.filterNotNull().maxWithOrNull(
        compareBy<Event> { it.at }
            .thenBy { if (it.mine) 1 else 0 }
            .thenBy { if (it is Event.Row) 1 else 0 }
    )

    /** Журнал удалений хранится 90 дней */
    const val DELETION_KEEP_MS = 90L * 24 * 60 * 60 * 1000
}
