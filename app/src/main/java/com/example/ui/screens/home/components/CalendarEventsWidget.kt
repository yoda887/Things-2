package com.example.ui.screens.home.components

import com.example.ui.theme.ThingsTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Полоска цвета календаря у события и колонка времени
private val EVENT_BAR_WIDTH = 3.dp
private val EVENT_BAR_HEIGHT = 11.dp
private val EVENT_BAR_GAP = 1.dp
private val EVENT_TIME_COLUMN = 72.dp

/**
 * CalendarEventsWidget: Отображает карточку со списком синхронизованных событий календаря.
 * 
 * Оптимизации производительности и архитектурные решения:
 * 1. Кеширование SimpleDateFormat через remember, чтобы избежать аллокации объектов в цикле рекомпозиции.
 * 2. Кеширование предустановленных пресетов цветов календаря в remember-блок.
 * 3. Расчет и кеширование цвета каждого календаря на основе хэш-кода имени, чтобы цвета оставались консистентными.
 * 4. Контролируемое время currentTimeMillis (инициализируется один раз при рекомпозиции или передается из ViewModel),
 *    что делает UI стейт детерминированным и тестируемым, а также избегает бесконтрольного системного опроса времени.
 */
@Composable
fun CalendarEventsWidget(
    events: List<Item>, // Список событий, полученных из календаря
    textSecondaryColor: Color = Color.Unspecified, // Запасной цвет для вторичного текста
    modifier: Modifier = Modifier,
    currentTimeMillis: Long = remember { System.currentTimeMillis() } // Текущее системное время для вычисления прошедших событий
) {
    // Если событий нет, компонент ничего не рендерит и освобождает ресурсы Compose
    if (events.isEmpty()) return

    // Фоновый цвет карточки в соответствии с текущей темой
    val cardBackground = ThingsTheme.colors.calendarCard

    // КЭШИРОВАНИЕ SimpleDateFormat: Создается один раз при инициализации виджета.
    // Это предотвращает лавинообразную нагрузку на сборщик мусора (Garbage Collector) при частых рекомпозициях.
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // КЭШИРОВАНИЕ ПРЕСЕТОВ ЦВЕТОВ: Статический список цветов закеширован в памяти,
    // теперь используются цвета из централизованной темы (Color.kt)
    val calendarPresets = ThingsTheme.colors.calendarPresets

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ThingsTheme.shapes.chipShape,
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = ThingsElevation.NONE)
    ) {
        Column(
            modifier = Modifier.padding(ThingsSpacing.S_PLUS),
            verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XXS)
        ) {
            // Ограничиваем список максимум пятью событиями во избежание чрезмерного растягивания карточки
            events.take(10).forEach { event ->
                val eventStart = event.eventStartMillis ?: 0L
                val hasTime = !event.isAllDay && eventStart > 0
                // Событие считается прошедшим, если его время начала меньше сохраненного текущего времени
                val isPastEvent = hasTime && eventStart < currentTimeMillis

                // ВЫЧИСЛЕНИЕБАЗОВОГОЦВЕТА КАЛЕНДАРЯ:
                // Используем remember с ключами (rawColor, displayName, id). Цвет пересчитывается
                // только при изменении самого события, а не на каждом цикле рекомпозиции интерфейса.
                val rawColor = event.calendarColor
                val baseColor = remember(rawColor, event.calendarDisplayName, event.id) {
                    if (rawColor != null) {
                        Color(rawColor)
                    } else {
                        // Если цвет календаря не задан, генерируем фиксированный пресет по хэш-коду его названия
                        val hash = (event.calendarDisplayName ?: event.id ?: "Default").hashCode()
                        calendarPresets[abs(hash) % calendarPresets.size]
                    }
                }

                // Адаптация цвета разделительного маркера: прошедшие события приглушаются
                val markerColor = if (isPastEvent) {
                    ThingsTheme.colors.calendarMuted
                } else {
                    baseColor
                }

                // Адаптация цвета метки времени
                val timeColor = if (isPastEvent) {
                    ThingsTheme.colors.calendarMuted
                } else {
                    baseColor
                }

                // Адаптация цвета заголовка события (приглушаем серым цветом для завершенных/прошедших событий)
                val titleColor = if (isPastEvent) {
                    ThingsTheme.colors.calendarMuted
                } else {
                    ThingsTheme.colors.textPrimary
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = ThingsSpacing.XXS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Вертикальный цветной индикатор слева (маркер календаря)
                    if (!hasTime) {
                        Box(
                            modifier = Modifier
                                .width(EVENT_BAR_WIDTH)
                                .height(EVENT_BAR_HEIGHT)
                                .clip(ThingsTheme.shapes.indicatorShape)
                                // Используем markerColor вместо baseColor, чтобы прошедшие события тускнели
                                .background(baseColor) 
                        )
                        Spacer(modifier = Modifier.width(EVENT_BAR_GAP))
                    }

                    // Если событие привязано к конкретному времени дня, отображаем его форматированную метку
                    if (hasTime) {
                        // КЭШИРОВАНИЕ СТРОКИ ВРЕМЕНИ:
                        // Форматируем дату только тогда, когда eventStart действительно изменяется,
                        // повторно используя сохраненный SimpleDateFormat экземпляр.
                        val timeString = remember(eventStart) { timeFormatter.format(Date(eventStart)) }
                        Text(
                            text = timeString,
                            style = ThingsTheme.type.subhead.copy(color = timeColor),
                        )
                    } 
                    Spacer(modifier = Modifier.width(ThingsSpacing.XS))

                    // Заголовок события с длинным текстом обрезается троеточием во избежание разрывов разметки
                    Text(
                        text = event.title,
                        style = ThingsTheme.type.subhead.copy(color = titleColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Модель данных для группировки события календаря с конкретной временной меткой в списке Upcoming.
 */
data class UpcomingEventItem(
    val event: Item,
    val dateMillis: Long,
    val dayOfMonthLabel: String? = null,
    /**
     * Последнее событие своего дня или месяца — под ним выводится отступ.
     *
     * Несмотря на название, флаг НЕ зависит от того, есть ли после события задачи: наличие задач
     * меняется во время перетаскивания, и высота строки менялась бы вместе с ним, из-за чего
     * карточка дёргалась бы при переходе через день.
     */
    val isLastBeforeTasks: Boolean = false
)

/**
 * Компонент для отображения одного календарного события на экране предстоящих событий (Upcoming).
 *
 * @param event объект события календаря типа [Item].
 * @param textSecondaryColor цвет для второстепенного текста.
 * @param textPrimaryColor цвет для основного текста.
 */
@Composable
fun UpcomingCalendarEventRow(
    event: Item,
    textSecondaryColor: Color,
    textPrimaryColor: Color,
    datePrefix: String? = null
) {
    val eventStart = event.eventStartMillis ?: 0L
    val hasTime = !event.isAllDay && eventStart > 0
    
    val rawColor = event.calendarColor
    val calendarDefault = ThingsTheme.colors.calendarDefault
    val baseColor = remember(rawColor, event.calendarDisplayName, event.id, calendarDefault) {
        if (rawColor != null) {
            Color(rawColor)
        } else {
            calendarDefault // Приятный зеленый цвет, соответствующий iOS стилю
        }
    }
    
    // В месячных разделах цвет календаря несёт только число, само событие — обычного цвета,
    // как и в разделах дней
    if (datePrefix != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ThingsSpacing.XXS, horizontal = ThingsSpacing.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = datePrefix,
                style = ThingsTheme.type.subhead.copy(color = baseColor),
                modifier = Modifier.padding(end = ThingsSpacing.S)
            )
            Text(
                text = event.title,
                style = ThingsTheme.type.subhead.copy(color = textPrimaryColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ThingsSpacing.XXS, horizontal = ThingsSpacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasTime) {
            val timeString = remember(eventStart) { 
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(eventStart)) 
            }
            Text(
                text = timeString,
                style = ThingsTheme.type.subhead.copy(color = baseColor),
                modifier = Modifier.width(EVENT_TIME_COLUMN)
            )
            Spacer(modifier = Modifier.width(ThingsSpacing.XS))
            Text(
                text = event.title,
                style = ThingsTheme.type.subhead.copy(color = textPrimaryColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            // All-day событие отображается зеленой вертикальной линией и текстом без времени
            Box(
                modifier = Modifier
                    .width(EVENT_BAR_WIDTH)
                    .height(EVENT_BAR_HEIGHT)
                    .clip(ThingsTheme.shapes.indicatorShape)
                    .background(baseColor)
            )
            Spacer(modifier = Modifier.width(ThingsSpacing.S))
            Text(
                text = event.title,
                style = ThingsTheme.type.subhead.copy(color = textPrimaryColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
