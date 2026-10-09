package com.example.ui.screens.home.components

import com.example.domain.lists.PlaceGroups

/** Строки списка для групп «по месту»: заголовок проекта или области, строки проектов и задачи. */
object PlaceGroupRows {

    fun rows(groups: List<PlaceGroups.Group>): List<Any> = buildList {
        groups.forEach { group ->
            when {
                group.project != null -> add(
                    SearchSectionHeaderItem("place_hdr_project_${group.project.id}", group.project.title, SearchSectionKind.PROJECT, project = group.project)
                )
                group.area != null -> add(
                    SearchSectionHeaderItem("place_hdr_area_${group.area.id}", group.area.title, SearchSectionKind.AREA, area = group.area)
                )
            }
            addAll(group.projectRows)
            addAll(group.tasks)
        }
    }
}
