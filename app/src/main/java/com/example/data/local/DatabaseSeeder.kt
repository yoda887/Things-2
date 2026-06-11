package com.example.data.local

import com.example.data.model.Area
import com.example.data.model.Item
import java.util.Calendar

/**
 * Utility helper responsible for initial database seeding.
 * Encapsulates setup logic for default areas, projects, and tasks to keep [ThingsViewModel] clean and focused.
 */
object DatabaseSeeder {

    /**
     * Seeds the database with default tasks, areas, and projects if they do not exist.
     * @param localDataSource local data source instance used for database insertions
     */
    suspend fun seedDatabase(localDataSource: LocalTaskDataSource) {
        // [ИЗМЕНЕНИЕ]: Инициализируем пустую базу данных начальными областями (Areas) через локальный источник данных
        val workArea = Area(id = "work_area", title = "Рабочие дела", sortOrder = 1)
        val personalArea = Area(id = "personal_area", title = "Личная жизнь", sortOrder = 2)
        localDataSource.insertArea(workArea)
        localDataSource.insertArea(personalArea)

        // [ИЗМЕНЕНИЕ]: Добавляем начальные проекты (Projects, Item с типом = 1)
        val workProj = Item(
            id = "work_proj",
            type = 1,
            title = "Onboard James",
            notes = "Onboarding new hire",
            areaId = "work_area",
            creationDate = System.currentTimeMillis() - 50000
        )
        val partyProj = Item(
            id = "party_proj",
            type = 1,
            title = "Throw Party for Eve",
            notes = "Planning Eve's birthday party",
            areaId = "personal_area",
            creationDate = System.currentTimeMillis() - 40000
        )
        
        localDataSource.insertItem(workProj)
        localDataSource.insertItem(partyProj)
        
        val tom = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomStart = Calendar.getInstance().apply {
            timeInMillis = tom.timeInMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val thur = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
        val thurStart = Calendar.getInstance().apply {
            timeInMillis = thur.timeInMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // [ИЗМЕНЕНИЕ]: Добавляем начальные задачи (Tasks, Item с типом = 0) с привязкой к проектам
        val prepareQuestions = Item(
            id = "seed_prep_questions",
            type = 0,
            title = "Prepare interview questions",
            notes = "Review Candidate resume and portfolio",
            start = 1, // today's smart lists
            status = 0,
            dueDate = tomStart,
            projectId = "work_proj"
        )
        
        val reserveDinner = Item(
            id = "seed_reserve_dinner",
            type = 0,
            title = "Make reservation for dinner",
            notes = "Italian place by the corner",
            start = 1,
            status = 0,
            dueDate = tomStart,
            projectId = "party_proj"
        )

        val movieTickets = Item(
            id = "seed_movie_tickets",
            type = 0,
            title = "Buy movie tickets for Friday",
            notes = "IMAX 3D preferred",
            start = 1,
            status = 0,
            dueDate = thurStart,
            priority = 1
        )

        val signedContract = Item(
            id = "seed_signed_contract",
            type = 0,
            title = "Get copy of signed contract",
            notes = "Check compliance system",
            start = 1,
            status = 0,
            dueDate = thurStart,
            projectId = "work_proj"
        )

        localDataSource.insertItems(listOf(prepareQuestions, reserveDinner, movieTickets, signedContract))
    }
}
