package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.data.model.ChecklistItem
import com.example.ui.components.ThingsCheckbox
import com.example.ui.theme.*

/**
 * Fullscreen Search screen conforming to ActiveScreen.SEARCH in high fidelity.
 * Performs deep search in titles, notes, checklists, and active/completed tasks.
 */
@Composable
fun ThingsSearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    allTasks: List<ItemWithChecklist>,
    projects: List<Item>,
    areas: List<Area>,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onTaskClick: (ItemWithChecklist) -> Unit,
    onTaskToggle: (ItemWithChecklist) -> Unit,
    onBack: () -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bkgColor = if (isDark) ThingsBackgroundDark else ThingsBackgroundLight
    val capsuleBkg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)

    // Deep search results filtering: titles, notes, checklists, and logbook completed/uncompleted tasks
    val deepResults = remember(searchQuery, allTasks, projects, areas) {
        if (searchQuery.isBlank()) {
            emptyList<DeepSearchResult>()
        } else {
            val list = mutableListOf<DeepSearchResult>()
            val query = searchQuery.trim()

            // 1. Filter tasks (both active and completed)
            allTasks.forEach { wrapper ->
                val matchesTitle = wrapper.item.type == 0 && wrapper.item.title.contains(query, ignoreCase = true)
                val matchesNotes = wrapper.item.type == 0 && wrapper.item.notes.contains(query, ignoreCase = true)
                
                // Matches checklist items
                val matchingChecklistItems = wrapper.checklist.filter { 
                    it.title.contains(query, ignoreCase = true) 
                }

                if (matchesTitle || matchesNotes || matchingChecklistItems.isNotEmpty()) {
                    list.add(
                        DeepSearchResult.TaskMatch(
                            taskWrapper = wrapper,
                            matchedChecklist = matchingChecklistItems
                        )
                    )
                }
            }

            // 2. Filter projects
            projects.forEach { proj ->
                if (proj.type == 1 && (proj.title.contains(query, ignoreCase = true) || proj.notes.contains(query, ignoreCase = true))) {
                    list.add(DeepSearchResult.ProjectMatch(proj))
                }
            }

            // 3. Filter areas
            areas.forEach { area ->
                if (area.title.contains(query, ignoreCase = true)) {
                    list.add(DeepSearchResult.AreaMatch(area))
                }
            }

            list
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bkgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: Back Arrow on left, "..." on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = ThingsBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = { /* Additional options / more context shortcuts */ },
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, textSecondaryColor.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = textSecondaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Large Title: Search
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = textPrimaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
            }

            // Search input capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(capsuleBkg)
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = textSecondaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search notes, checklists, logbook...",
                            color = textSecondaryColor.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        textStyle = TextStyle(
                            color = textPrimaryColor,
                            fontSize = 15.sp
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("fullscreen_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Clear",
                            tint = textSecondaryColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Results Listing
            if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Type to search your notes, checklists, and completed items.",
                        color = textSecondaryColor.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else if (deepResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = textSecondaryColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            color = textSecondaryColor,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                ) {
                    items(deepResults) { result ->
                        DeepSearchResultRow(
                            result = result,
                            textPrimaryColor = textPrimaryColor,
                            textSecondaryColor = textSecondaryColor,
                            dividerColor = dividerColor,
                            onTaskClick = onTaskClick,
                            onTaskToggle = onTaskToggle,
                            projects = projects,
                            areas = areas
                        )
                    }
                }
            }
        }

        // FAB + at bottom right to add a task with current query prefilled
        FloatingActionButton(
            onClick = onFabClick,
            containerColor = ThingsBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .testTag("search_add_task_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Task From Query",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Custom result model representing matching targets in deep search
 */
sealed class DeepSearchResult {
    data class TaskMatch(
        val taskWrapper: ItemWithChecklist,
        val matchedChecklist: List<ChecklistItem>
    ) : DeepSearchResult()

    data class ProjectMatch(val project: Item) : DeepSearchResult()
    data class AreaMatch(val area: Area) : DeepSearchResult()
}

@Composable
fun DeepSearchResultRow(
    result: DeepSearchResult,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    onTaskClick: (ItemWithChecklist) -> Unit,
    onTaskToggle: (ItemWithChecklist) -> Unit,
    projects: List<Item>,
    areas: List<com.example.data.model.Area> = emptyList()
) {
    when (result) {
        is DeepSearchResult.TaskMatch -> {
            val wrapper = result.taskWrapper
            val task = wrapper.item
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(textSecondaryColor.copy(alpha = 0.03f))
                    .border(1.dp, dividerColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { onTaskClick(wrapper) }
                    .padding(12.dp)
            ) {
                // Task Header Row: Custom styled checkbox + Title + Notes/Checklist icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThingsCheckbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onTaskToggle(wrapper) },
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (task.title.isBlank()) "Untitled To-Do" else task.title,
                        color = if (task.isCompleted) textSecondaryColor else textPrimaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Match indicators on right
                    if (task.notes.isNotBlank() || wrapper.checklist.isNotEmpty() || task.cachedTags.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (task.notes.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = "Has notes",
                                    tint = textSecondaryColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (wrapper.checklist.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Outlined.FormatListBulleted,
                                    contentDescription = "Has checklist",
                                    tint = textSecondaryColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (task.cachedTags.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalOffer,
                                    contentDescription = "Has tags",
                                    tint = textSecondaryColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Subtitle: Project/Area name and Section info if available
                val project = remember(task.projectId, projects) {
                    projects.firstOrNull { it.id == task.projectId }
                }
                val area = remember(task.areaId, areas) {
                    areas.firstOrNull { it.id == task.areaId }
                }
                val contextText = remember(task, project, area) {
                    buildString {
                        if (task.isCompleted) {
                            append("Logbook")
                        } else {
                            when {
                                task.isInbox -> append("Inbox")
                                task.isToday -> append("Today")
                                task.isUpcoming -> append("Upcoming")
                                task.isAnytime -> append("Anytime")
                                task.isSomeday -> append("Someday")
                            }
                        }
                        if (project != null) {
                            append(" • ")
                            append(project.title)
                        } else if (area != null) {
                            append(" • ")
                            append(area.title)
                        }
                    }
                }

                if (contextText.isNotBlank()) {
                    Text(
                        text = contextText,
                        color = textSecondaryColor.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 28.dp, top = 2.dp)
                    )
                }

                // If notes or checklist items matched, draw preview boxes under the title
                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        color = textSecondaryColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 28.dp, top = 6.dp)
                    )
                }

                // Render matched checklist items
                if (result.matchedChecklist.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        result.matchedChecklist.forEach { chItem ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (chItem.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (chItem.isCompleted) ThingsLogbookGreen else textSecondaryColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = chItem.title,
                                    color = if (chItem.isCompleted) textSecondaryColor else textPrimaryColor.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        is DeepSearchResult.ProjectMatch -> {
            val proj = result.project
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ThingsAnytimeTeal.copy(alpha = 0.05f))
                    .border(1.dp, ThingsAnytimeTeal.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = ThingsAnytimeTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proj.title,
                        color = textPrimaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Project",
                        color = textSecondaryColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
        is DeepSearchResult.AreaMatch -> {
            val area = result.area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(textSecondaryColor.copy(alpha = 0.03f))
                    .border(1.dp, dividerColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = ThingsSomedayGrey,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = area.title,
                        color = textPrimaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Area",
                        color = textSecondaryColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
