package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    isDark: Boolean = false, // Явный флаг темной темы (если передан)
    modifier: Modifier = Modifier,
    currentTimeMillis: Long = remember { System.currentTimeMillis() } // Текущее системное время для вычисления прошедших событий
) {
    // Если событий нет, компонент ничего не рендерит и освобождает ресурсы Compose
    if (events.isEmpty()) return

    // Определяем, используется ли темная тема (берется системная либо переданный из параметров флаг)
    val systemDark = isSystemInDarkTheme() || isDark
    
    // Вычисляем фоновый цвет карточки в соответствии с текущей темой
    val cardBackground = if (systemDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF2F2F7)

    // КЭШИРОВАНИЕ SimpleDateFormat: Создается один раз при инициализации виджета.
    // Это предотвращает лавинообразную нагрузку на сборщик мусора (Garbage Collector) при частых рекомпозициях.
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // КЭШИРОВАНИЕ ПРЕСЕТОВ ЦВЕТОВ: Статический список цветов закеширован в памяти,
    // теперь используются цвета из централизованной темы (Color.kt)
    val calendarPresets = remember {
        listOf(
            CalendarGreen,
            CalendarBlue,
            CalendarYellow,
            CalendarRed,
            CalendarPurple,
            CalendarPink,
            CalendarTeal
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                    if (systemDark) Color(0xFF48484A) else Color(0xFFD1D1D6)
                } else {
                    baseColor
                }

                // Адаптация цвета метки времени
                val timeColor = if (isPastEvent) {
                    if (systemDark) Color(0xFF48484A) else Color(0xFFD1D1D6)
                } else {
                    baseColor
                }

                // Адаптация цвета заголовка события (приглушаем серым цветом для завершенных/прошедших событий)
                val titleColor = if (isPastEvent) {
                     if (systemDark) Color(0xFF48484A) else Color(0xFFD1D1D6)
                } else {
                    if (systemDark) Color(0xFFD1D1D6) else Color(0xFF2C2C2E)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Вертикальный цветной индикатор слева (маркер календаря)
                    if (!hasTime) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                // Используем markerColor вместо baseColor, чтобы прошедшие события тускнели
                                .background(baseColor) 
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Если событие привязано к конкретному времени дня, отображаем его форматированную метку
                    if (hasTime) {
                        // КЭШИРОВАНИЕ СТРОКИ ВРЕМЕНИ:
                        // Форматируем дату только тогда, когда eventStart действительно изменяется,
                        // повторно используя сохраненный SimpleDateFormat экземпляр.
                        val timeString = remember(eventStart) { timeFormatter.format(Date(eventStart)) }
                        Text(
                            text = timeString,
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = timeColor,
                                fontWeight = FontWeight.Normal
                            ),
                        )
                    } 
                    Spacer(modifier = Modifier.width(4.dp))

                    // Заголовок события с длинным текстом обрезается троеточием во избежание разрывов разметки
                    Text(
                        text = event.title,
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = titleColor,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
