package com.example.ui.screens.home.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Множественный выбор задач на экране списка. Режим включается свайпом влево по задаче или пунктом «Вибрати» в меню
 * тулбара (тогда и без выбранных) и держится, пока его не закроют: крестиком, «Назад», пакетным действием, сменой
 * экрана или началом перетаскивания.
 */
@Stable
class TaskSelectionState {
    var isActive by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf(emptySet<String>())
        private set

    /** Войти в режим выбора; [initialId] — задача, выбранная сразу. */
    fun enter(initialId: String? = null) {
        isActive = true
        selectedIds = setOfNotNull(initialId)
    }

    fun exit() {
        isActive = false
        selectedIds = emptySet()
    }

    /** Свайп влево: вне режима — войти с этой задачей; в режиме — отметить или снять, снятие последней закрывает режим. */
    fun swipe(id: String) {
        if (!isActive) {
            enter(id)
        } else if (id in selectedIds) {
            val next = selectedIds - id
            if (next.isEmpty()) exit() else selectedIds = next
        } else {
            selectedIds = selectedIds + id
        }
    }

    /** Нажатие в режиме выбора — отметить или снять, режим остаётся. */
    fun toggle(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    /** Выбор протяжкой: поставить задаче состояние выбора. */
    fun set(id: String, selected: Boolean) {
        selectedIds = if (selected) selectedIds + id else selectedIds - id
    }

    fun selectAll(ids: Collection<String>) {
        selectedIds = ids.toSet()
    }

    fun deselectAll() {
        selectedIds = emptySet()
    }

    /** Выбранные задачи из [all]. */
    fun selected(all: List<ItemWithChecklist>): List<ItemWithChecklist> = all.filter { it.item.id in selectedIds }
}

/**
 * Множественный выбор проектов (главный экран, «Пізніші проєкти») и «Когда» одного проекта по свайпу вправо.
 *
 * @param keepsModeWhenEmpty режим выбора держится и без выбранных (его включают из меню тулбара и закрывают
 *   крестиком — «Пізніші проєкти»); иначе режим — это просто «есть выбранные» (главный экран)
 */
@Stable
class ProjectSelectionState(private val keepsModeWhenEmpty: Boolean = false) {
    var selectedIds by mutableStateOf(emptySet<String>())
        private set
    private var active by mutableStateOf(false)

    /** Проект, для которого свайпом вправо открыт диалог «Когда», и точка, из которой диалог вырастает. */
    var whenProject by mutableStateOf<Item?>(null)
        private set
    var whenOrigin by mutableStateOf<Offset?>(null)
        private set

    val isSelectionMode: Boolean get() = (keepsModeWhenEmpty && active) || selectedIds.isNotEmpty()

    /** Войти в режим выбора без выбранных (пункт «Вибрати» в меню тулбара). */
    fun enter() {
        active = true
    }

    fun exit() {
        selectedIds = emptySet()
        active = false
    }

    /** Нажатие в режиме выбора — отметить или снять; режим остаётся, если он держится без выбранных. */
    fun toggle(project: Item) {
        active = true
        selectedIds = if (project.id in selectedIds) selectedIds - project.id else selectedIds + project.id
    }

    /** Свайп влево — как у задач: отметить или снять, снятие последнего выбранного закрывает режим. */
    fun swipeSelect(project: Item) {
        val next = if (project.id in selectedIds) selectedIds - project.id else selectedIds + project.id
        if (next.isEmpty()) exit() else {
            selectedIds = next
            active = true
        }
    }

    /** Выбор протяжкой: поставить строке состояние выбора. */
    fun set(id: String, selected: Boolean) {
        selectedIds = if (selected) selectedIds + id else selectedIds - id
    }

    fun selectAll(ids: Collection<String>) {
        active = true
        selectedIds = ids.toSet()
    }

    fun deselectAll() {
        selectedIds = emptySet()
    }

    /** Выбранные проекты из [all]. */
    fun selected(all: List<Item>): List<Item> = all.filter { it.id in selectedIds }

    fun openWhen(project: Item, origin: Offset?) {
        whenProject = project
        whenOrigin = origin
    }

    fun closeWhen() {
        whenProject = null
        whenOrigin = null
    }
}
