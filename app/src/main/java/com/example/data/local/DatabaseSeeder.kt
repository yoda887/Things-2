package com.example.data.local

import com.example.data.model.Area
import com.example.data.model.Item
import java.util.Calendar

/**
 * Вспомогательный класс для заполнения базы данных демонстрационными данными.
 * Последовательно создает области, проекты и подробную сетку задач на несколько дней вперед.
 */
object DatabaseSeeder {

    /**
     * Заселяет базу данных стартовыми областями, проектами и обширным списком задач.
     * @param localDataSource Экземпляр источника данных для вставки сущностей.
     */
    suspend fun seedDatabase(localDataSource: LocalTaskDataSource) {
        // [ИЗМЕНЕНИЕ]: Создаем две базовые системные папки (Области / Areas)
        val workArea = Area(id = "work_area", title = "Рабочие дела", sortOrder = 1)
        val personalArea = Area(id = "personal_area", title = "Личная жизнь", sortOrder = 2)
        localDataSource.insertArea(workArea)
        localDataSource.insertArea(personalArea)

        // [ИЗМЕНЕНИЕ]: Создаем по 2 стартовых проекта внутри областей, и 2 проекта вне областей
        // Внутри "Рабочие дела"
        val workProj1 = Item(
            id = "work_proj_1",
            type = 1, // проект
            title = "Повышение квалификации",
            notes = "Курсы, книги, вебинары и профессиональное развитие",
            areaId = "work_area",
            creationDate = System.currentTimeMillis() - 100000
        )
        val workProj2 = Item(
            id = "work_proj_2",
            type = 1, // проект
            title = "Запуск нового продукта",
            notes = "Подготовка, запуск и сбор обратной связи по новому проекту",
            areaId = "work_area",
            creationDate = System.currentTimeMillis() - 90000
        )

        // Внутри "Личная жизнь"
        val personalProj1 = Item(
            id = "personal_proj_1",
            type = 1, // проект
            title = "Спорт и здоровье",
            notes = "Тренировки, активность, правильное питание и сон",
            areaId = "personal_area",
            creationDate = System.currentTimeMillis() - 80000
        )
        val personalProj2 = Item(
            id = "personal_proj_2",
            type = 1, // проект
            title = "Домашний уют",
            notes = "Уборка, обустройство квартиры и бытовые дела",
            areaId = "personal_area",
            creationDate = System.currentTimeMillis() - 70000
        )

        // Вне областей (areaId = null)
        val globalProj1 = Item(
            id = "global_proj_1",
            type = 1, // проект
            title = "Изучение английского",
            notes = "Лексика, грамматика, разговорная практика по Skype",
            areaId = null,
            creationDate = System.currentTimeMillis() - 60000
        )
        val globalProj2 = Item(
            id = "global_proj_2",
            type = 1, // проект
            title = "Планирование отпуска",
            notes = "Маршруты, билеты, жилье и бюджет на Алтай",
            areaId = null,
            creationDate = System.currentTimeMillis() - 50000
        )

        localDataSource.insertItem(workProj1)
        localDataSource.insertItem(workProj2)
        localDataSource.insertItem(personalProj1)
        localDataSource.insertItem(personalProj2)
        localDataSource.insertItem(globalProj1)
        localDataSource.insertItem(globalProj2)

        // [ИЗМЕНЕНИЕ]: Создаем структурированный список задач с привязкой ко времени
        val itemsToInsert = mutableListOf<Item>()

        // Получаем начало сегодняшнего дня (00:00:00)
        val todayStartCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMillis = todayStartCal.timeInMillis

        // База задач на каждый день (от 0 до 7 смещения в днях). Каждая группа содержит ровно 4 задачи.
        val tasksData = mapOf(
            0 to listOf( // Сегодня
                Quadruple("Проверить почту и чаты Jira", "Ответить на все критические сообщения", "work_proj_1", false),
                Quadruple("Заказать продукты", "Купить молоко, фрукты и овощи на выходные", "personal_proj_2", false),
                Quadruple("Вечерняя тренировка", "Пробежка 5 км в парке или растяжка", "personal_proj_1", true),
                Quadruple("Подведение итогов дня", "Записать результаты и составить список дел на завтра", "work_proj_2", true)
            ),
            1 to listOf( // Завтра
                Quadruple("Провести созвон с командой", "Обсудить текущий статус спринта и раздать задачи", "work_proj_2", false),
                Quadruple("Разобрать шкаф в спальне", "Убрать зимние вещи и помыть полки", "personal_proj_2", false),
                Quadruple("Прочитать главу книги по Scrum", "Книга: 'Scrum. Революционный метод управления проектами'", "work_proj_1", false),
                Quadruple("Выбрать отель в Сочи", "Посмотреть отзывы на Booking и сравнить цены", "global_proj_2", false)
            ),
            2 to listOf( // Послезавтра
                Quadruple("Написать черновик статьи на Habr", "Тема: Секреты Jetpack Compose для начинающих", "work_proj_1", false),
                Quadruple("Приготовить ужин для семьи", "Купить ингредиенты для пасты карбонара", "personal_proj_2", false),
                Quadruple("Выучить 20 новых глаголов", "Использовать приложение Anki для запоминания", "global_proj_1", false),
                Quadruple("Спланировать маршрут похода", "Найти трек на Wikiloc и скачать оффлайн карты", "global_proj_2", false)
            ),
            3 to listOf( // Через 3 дня
                Quadruple("Подготовить аналитический отчет", "Собрать метрики вовлеченности за прошедший месяц", "work_proj_2", false),
                Quadruple("Велопрогулка по набережной", "Проверить тормоза и давление в шинах перед выездом", "personal_proj_1", false),
                Quadruple("Купить путеводитель по Алтаю", "Издательство 'Оранжевый гид' или заказать онлайн", "global_proj_2", false),
                Quadruple("Посмотреть обучающий вебинар", "Тема: Оптимизация производительности Android-приложений", "work_proj_1", false)
            ),
            4 to listOf( // Через 4 дня
                Quadruple("Проверить код (Code Review)", "Критически оценить пул-реквесты коллег", "work_proj_2", false),
                Quadruple("Сдать одежду в химчистку", "Взять пальто и зимнюю куртку", "personal_proj_2", false),
                Quadruple("Начать новый модуль по английскому", "Пройти модуль 'Business Correspondence' на Coursera", "global_proj_1", false),
                Quadruple("Рассчитать бюджет поездки", "Учесть билеты, жилье, питание и сувениры", "global_proj_2", false)
            ),
            5 to listOf( // Через 5 дней
                Quadruple("Написать отчет по KPI", "Заполнить таблицу результатов работы за квартал", "work_proj_2", false),
                Quadruple("Проверить трекер привычек", "Отметить успехи за неделю в дневнике", "personal_proj_1", false),
                Quadruple("Прокачать аудирование", "Послушать эпизод подкаста Luke's English Podcast", "global_proj_1", false),
                Quadruple("Оформить страховку для отпуска", "Сравнить предложения АльфаСтрахование и Тинькофф", "global_proj_2", false)
            ),
            6 to listOf( // Через 6 дней
                Quadruple("Ознакомиться с новым ТЗ", "Задать вопросы аналитику в Slack до созвона", "work_proj_2", false),
                Quadruple("Полить комнатные растения", "Добавить удобрение в воду для полива", "personal_proj_2", false),
                Quadruple("Разговорный клуб по Skype", "Тема обсуждения: 'Artificial Intelligence and Future Of Work'", "global_proj_1", false),
                Quadruple("Забронировать авиабилеты", "Посмотреть цены на Авиасейлс ранним утром", "global_proj_2", false)
            ),
            7 to listOf( // Через 7 дней
                Quadruple("Провести демо-презентацию", "Показать рабочую версию фичи стейкхолдерам", "work_proj_2", false),
                Quadruple("Утренняя разминка и медитация", "15 минут на растяжку спины и дыхательную гимнастику", "personal_proj_1", false),
                Quadruple("Сдать тест на уровень языка", "Пройти бесплатный тест на EF SET", "global_proj_1", false),
                Quadruple("Составить список вещей в поездку", "Взять документы, зарядки, аптечку и удобную обувь", "global_proj_2", false)
            )
        )

        for ((dayOffset, list) in tasksData) {
            val taskCal = Calendar.getInstance().apply {
                timeInMillis = todayStartMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            // Каждая задача имеет время старта 12:00 соответствующего дня
            val taskTimeMillis = Calendar.getInstance().apply {
                timeInMillis = taskCal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            for ((idx, quad) in list.withIndex()) {
                val taskId = "seed_day_${dayOffset}_task_${idx}"
                
                val item = Item(
                    id = taskId,
                    type = 0, // задача
                    title = quad.title,
                    notes = quad.notes,
                    status = 0, // open
                    // Для Сегодня (dayOffset = 0) устанавливаем start = 1 (Сегодня)
                    // Для других дней устанавливаем start = 2 (Anytime/Upcoming)
                    start = if (dayOffset == 0) 1 else 2,
                    isTonight = quad.isTonight,
                    startDate = if (dayOffset == 0) null else taskTimeMillis,
                    dueDate = if (dayOffset == 0) null else taskTimeMillis,
                    projectId = quad.projectId,
                    creationDate = System.currentTimeMillis() - (8 - dayOffset) * 10000 - idx * 1000
                )
                itemsToInsert.add(item)
            }
        }

        localDataSource.insertItems(itemsToInsert)
    }

    private data class Quadruple(
        val title: String,
        val notes: String,
        val projectId: String,
        val isTonight: Boolean
    )
}

