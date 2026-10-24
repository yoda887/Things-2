package com.example.data.sync

import com.example.domain.tag.TagTitles

/**
 * Слияние состояний двух устройств — без базы и сети, закреплено тестами SyncMergeTest.
 *
 * Запись таблицы — набор полей ([SyncRow.values]) с временем последнего изменения ([SyncRow.updatedAt]); удаление
 * насовсем — запись журнала ([SyncDeletionMark]). По каждому ключу «таблица + id» побеждает самое позднее событие:
 * правка или удаление. При равном времени правка этого устройства остаётся (результат один и тот же с обеих сторон,
 * когда они обменяются).
 *
 * Запись, которую с прошлого обмена правили на обоих устройствах, сливается по полям относительно общего
 * состояния прошлого обмена ([merge] base): поле, изменённое только на одном устройстве, берётся оттуда; изменённое
 * на обоих по-разному — с той стороны, где запись правили позже. Так правки разных полей одной задачи (название на
 * телефоне, дата на компьютере) не перебивают друг друга. Строка тегов задачи и счётчики её чек-листа, изменённые на
 * обоих, не выбираются, а пересчитываются по слитым связям и пунктам — чтобы сходились с ними.
 * Без общего состояния прошлого обмена (первый обмен устройства) — целиком побеждает более поздняя запись.
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
     * Итог слияния: [merged] — общее состояние после обмена; [applyLocally] — записи с другого устройства или
     * слитые по полям, отличающиеся от своих (вставить или заменить у себя); [deleteLocally] — свои записи, удалённые
     * там позже их правки; [remoteChanged] — в общем файле есть что-то новое от этого устройства, его нужно записать.
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

    /** Поле времени изменения записи — не сливается, его ставит слияние */
    private const val UPDATED_AT = "updatedAt"

    /** Поля задачи, которые выводятся из других таблиц: при правке на обоих — пересчитываются */
    private const val CACHED_TAGS = "cachedTags"
    private const val CHECKLIST_TOTAL = "checklistItemsCount"
    private const val CHECKLIST_OPEN = "openChecklistItemsCount"
    private val DERIVED = setOf(CACHED_TAGS, CHECKLIST_TOTAL, CHECKLIST_OPEN)

    /** Откуда запись итога: своя, с другого устройства или новая — слитая по полям */
    private enum class Source { MINE, THEIRS, MERGED }

    /**
     * Слить своё состояние [local] с общим [remote]. [base] — общее состояние прошлого обмена этого устройства
     * (null — обмена ещё не было): по нему видно, какие поля записи с тех пор правили на каждой стороне.
     */
    fun merge(local: State, remote: State, base: State? = null): Result {
        val sharedExists = remote.rows.isNotEmpty() || remote.deletions.isNotEmpty()
        val seedsToDrop = if (!sharedExists) emptySet() else local.rows.values
            .filter { it.updatedAt <= SEED_TIME && it.key !in remote.rows && it.key !in remote.deletions }
            .mapTo(HashSet()) { it.key }
        if (seedsToDrop.isNotEmpty()) {
            val adopted = merge(local.copy(rows = local.rows - seedsToDrop), remote, base)
            return adopted.copy(deleteLocally = adopted.deleteLocally + seedsToDrop)
        }
        val keys = local.rows.keys + local.deletions.keys + remote.rows.keys + remote.deletions.keys
        val rows = HashMap<Key, SyncRow>()
        val sources = HashMap<Key, Source>()
        // Поля, которые правили на обоих устройствах по-разному (выводимые — правили на обоих) — у слитых по полям записей
        val conflicts = HashMap<Key, Set<String>>()
        val deletions = HashMap<Key, SyncDeletionMark>()
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
                    val mine = local.rows[key]
                    val theirs = remote.rows[key]
                    val was = base?.rows?.get(key)
                    val merged = if (mine != null && theirs != null && was != null) mergeFields(mine, theirs, was) else null
                    if (merged != null) {
                        rows[key] = merged.row
                        sources[key] = merged.source
                        if (merged.conflicts.isNotEmpty()) conflicts[key] = merged.conflicts
                    } else {
                        rows[key] = winner.row
                        sources[key] = if (winner.mine) Source.MINE else Source.THEIRS
                    }
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

        deriveItemFields(rows, sources, conflicts)

        val apply = ArrayList<SyncRow>()
        for ((key, row) in rows) {
            when (sources.getValue(key)) {
                Source.THEIRS -> apply += row
                Source.MERGED -> { apply += row; remoteChanged = true }
                Source.MINE -> if (remote.rows[key]?.updatedAt != row.updatedAt || key in remote.deletions) remoteChanged = true
            }
        }
        return Result(State(rows, deletions), apply, delete, remoteChanged)
    }

    private class FieldMerge(val row: SyncRow, val source: Source, val conflicts: Set<String>)

    /**
     * Слить по полям запись, которую правили на обоих устройствах после прошлого обмена ([was] — она тогда).
     * Если её правили только на одной стороне — null: решает время, как у остальных записей.
     */
    private fun mergeFields(mine: SyncRow, theirs: SyncRow, was: SyncRow): FieldMerge? {
        if (mine.updatedAt == was.updatedAt || theirs.updatedAt == was.updatedAt) return null
        if (mine.updatedAt == theirs.updatedAt && mine.values == theirs.values) return null
        val mineIsNewer = mine.updatedAt >= theirs.updatedAt
        val conflicts = HashSet<String>()
        val values = LinkedHashMap<String, Any?>()
        for (field in mine.values.keys + theirs.values.keys) {
            if (field == UPDATED_AT) continue
            val m = mine.values[field]
            val t = theirs.values[field]
            // Выводимое поле, изменённое на обоих (пусть и одинаково: по одному пункту чек-листа с каждой стороны), —
            // пересчитать по слитым записям
            if (field in DERIVED && field in was.values && was.values[field] != m && was.values[field] != t) conflicts += field
            values[field] = when {
                field !in theirs.values -> m
                field !in mine.values -> t
                m == t -> m
                field in was.values && was.values[field] == m -> t
                field in was.values && was.values[field] == t -> m
                else -> {
                    conflicts += field
                    if (mineIsNewer) m else t
                }
            }
        }
        return when {
            // Итог совпал с более поздней записью — она и остаётся
            values == mine.values - UPDATED_AT && mine.updatedAt > theirs.updatedAt -> FieldMerge(mine, Source.MINE, conflicts)
            values == theirs.values - UPDATED_AT && theirs.updatedAt > mine.updatedAt -> FieldMerge(theirs, Source.THEIRS, conflicts)
            // Новая запись — позже обеих: на другом устройстве она перебьёт его прежнюю и без слияния
            else -> {
                val at = maxOf(mine.updatedAt, theirs.updatedAt) + 1
                FieldMerge(SyncRow(mine.key, at, values + (UPDATED_AT to at)), Source.MERGED, conflicts)
            }
        }
    }

    /**
     * Строка тегов и счётчики чек-листа задачи, которые правили на обоих устройствах, — заново по слитым связям
     * задача–тег (названия в порядке тегов, как в базе) и пунктам чек-листа.
     */
    private fun deriveItemFields(rows: HashMap<Key, SyncRow>, sources: HashMap<Key, Source>, conflicts: Map<Key, Set<String>>) {
        val items = conflicts.filter { (key, fields) ->
            key.table == "items" && fields.any { it in DERIVED }
        }
        if (items.isEmpty()) return
        val tags = rows.values.filter { it.key.table == "tags" }.associateBy { it.values["id"] as? String }
        val links = rows.values.filter { it.key.table == "item_tags" }.groupBy { it.values["itemId"] as? String }
        val checklist = rows.values.filter { it.key.table == "checklist_items" }.groupBy { it.values["itemId"] as? String }
        for ((key, fields) in items) {
            val row = rows.getValue(key)
            val values = LinkedHashMap(row.values)
            if (CACHED_TAGS in fields) {
                val titles = links[key.id].orEmpty()
                    .mapNotNull { link -> tags[link.values["tagId"] as? String] }
                    .sortedBy { (it.values["sortOrder"] as? Number)?.toLong() ?: 0L }
                    .mapNotNull { it.values["title"] as? String }
                values[CACHED_TAGS] = TagTitles.join(titles)
            }
            if (CHECKLIST_TOTAL in fields || CHECKLIST_OPEN in fields) {
                val points = checklist[key.id].orEmpty()
                values[CHECKLIST_TOTAL] = points.size.toLong()
                values[CHECKLIST_OPEN] = points.count { (it.values["isCompleted"] as? Number)?.toLong() == 0L }.toLong()
            }
            if (values != row.values) {
                // Пересчитанная запись — новая: позже обеих
                val at = if (sources[key] == Source.MERGED) row.updatedAt else row.updatedAt + 1
                rows[key] = SyncRow(key, at, values + (UPDATED_AT to at))
                sources[key] = Source.MERGED
            }
        }
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
