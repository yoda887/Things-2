package com.example.ui.screens.home.components

import com.example.domain.lists.PlaceGroups

/** Строки списка для групп «по месту»: заголовок проекта или области, строки проектов и задачи. */
object PlaceGroupRows {

    /** [entries] — строки группы под заголовком; по умолчанию строки проектов, затем задачи */
    fun rows(
        groups: List<PlaceGroups.Group>,
        entries: (PlaceGroups.Group) -> List<Any> = { it.projectRows + it.tasks }
    ): List<Any> = buildList {
        groups.forEach { group ->
            val project = group.project
            val area = group.area
            when {
                project != null -> add(
                    SearchSectionHeaderItem("place_hdr_project_${project.id}", project.title, SearchSectionKind.PROJECT, project = project)
                )
                area != null -> add(
                    SearchSectionHeaderItem("place_hdr_area_${area.id}", area.title, SearchSectionKind.AREA, area = area)
                )
            }
            addAll(entries(group))
        }
    }
}
