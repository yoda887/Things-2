package com.example.ui.screens.home.subcomponents

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.lists.AreaList
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsTheme

/** Действия меню «…» у названия области, которым нужно окно или система: теги и «Поділитися». */
enum class AreaAction { TAGS, SHARE }

/**
 * Теги области под её названием, как в Things: плашки в строку; нажатие открывает окно тегов. Без тегов строки нет.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AreaTagsRow(area: Area, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tags = area.tags
    if (tags.isEmpty()) return
    val view = LocalView.current
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .padding(vertical = ThingsSpacing.S_PLUS),
        horizontalArrangement = Arrangement.spacedBy(ThingsSpacing.S),
        verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XS_PLUS)
    ) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(ThingsTheme.shapes.chipShape)
                    .background(ThingsTheme.colors.tagChipBackground)
                    .padding(horizontal = ThingsSpacing.S_PLUS, vertical = ThingsSpacing.XS)
            ) {
                Text(
                    text = tag,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = ThingsTheme.type.tagChip.copy(color = ThingsTheme.colors.tagChipText)
                )
            }
        }
    }
}

/**
 * Область текстом для «Поділитися»: название, теги, затем проекты и задачи, как на экране — текущие, потом
 * «Плани» и «Колись» (их заголовки — [upcomingTitle], [somedayTitle]).
 */
fun areaShareText(
    area: Area,
    tasks: List<ItemWithChecklist>,
    projects: List<Item>,
    upcomingTitle: String,
    somedayTitle: String
): String = buildString {
    append(area.title)
    if (area.tags.isNotEmpty()) append("\n").append(area.tags.joinToString(", "))
    // В общем списке записей есть и сами проекты — без отбора проект попал бы в текст дважды
    val content = AreaList.content(area.id, tasks.filter { it.item.type == Item.TYPE_TASK }, projects)
    fun section(projectRows: List<Item>, taskRows: List<ItemWithChecklist>) {
        projectRows.forEach { append("\n• ").append(it.title) }
        taskRows.forEach { append("\n• ").append(it.item.title) }
    }
    if (content.projects.isNotEmpty() || content.current.isNotEmpty()) {
        append("\n")
        section(content.projects, content.current)
    }
    if (content.upcomingProjects.isNotEmpty() || content.upcoming.isNotEmpty()) {
        append("\n\n").append(upcomingTitle)
        section(content.upcomingProjects, content.upcoming)
    }
    if (content.somedayProjects.isNotEmpty() || content.someday.isNotEmpty()) {
        append("\n\n").append(somedayTitle)
        section(content.somedayProjects, content.someday)
    }
}

/** Строки текста, вставленного в список, — по задаче на непустую строку (маркеры списка «•», «-», «*» убираются). */
fun pastedTaskTitles(text: String): List<String> =
    text.lines()
        .map { it.trim().removePrefix("•").removePrefix("- ").removePrefix("* ").trim() }
        .filter { it.isNotEmpty() }
