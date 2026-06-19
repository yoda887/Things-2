package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Группируем все кастомные иконки в один объект для удобного автокомплита
object AppIcons {

    // Кастомная иконка для Inbox (Ящик)
    val Inbox: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Inbox",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF1B80FA)),
                stroke = SolidColor(Color(0xFF1B80FA)),
                strokeLineWidth = 0.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 3f)
                lineTo(5f, 3f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                lineTo(3f, 19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                lineTo(19f, 21f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(21f, 5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 15f)
                lineTo(15.5f, 15f)
                curveToRelative(-0.5f, 0f, -1f, 0.4f, -1f, 0.9f)
                curveToRelative(-0.2f, 1.1f, -1.2f, 2.1f, -2.5f, 2.1f)
                curveToRelative(-1.3f, 0f, -2.3f, -1f, -2.5f, -2.1f)
                curveToRelative(0f, -0.5f, -0.5f, -0.9f, -1f, -0.9f)
                lineTo(5f, 15f)
                lineTo(5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 15f)
                close()
            }
        }.build()
    }

    // Кастомная иконка для Today (Звезда)
    val Today: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Today",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFECB2F)),
                stroke = SolidColor(Color(0xFFFECB2F)),
                strokeLineWidth = 0.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                lineTo(15.09f, 8.26f)
                lineTo(22f, 9.27f)
                lineTo(17f, 14.14f)
                lineTo(18.18f, 21.02f)
                lineTo(12f, 17.77f)
                lineTo(5.82f, 21.02f)
                lineTo(7f, 14.14f)
                lineTo(2f, 9.27f)
                lineTo(8.91f, 8.26f)
                close()
            }
        }.build()
    }

    // Кастомная иконка для Upcoming (Календарь-сетка)
    val Upcoming: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Upcoming",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFF35F50)),
                stroke = SolidColor(Color(0xFFF35F50)),
                strokeLineWidth = 0.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 4f)
                lineTo(18f, 4f)
                lineTo(18f, 2f)
                lineTo(16f, 2f)
                lineTo(16f, 4f)
                lineTo(8f, 4f)
                lineTo(8f, 2f)
                lineTo(6f, 2f)
                lineTo(6f, 4f)
                lineTo(5f, 4f)
                curveToRelative(-1.11f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(3f, 20f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                lineTo(19f, 22f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(21f, 6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 20f)
                lineTo(5f, 20f)
                lineTo(5f, 9f)
                lineTo(19f, 9f)
                lineTo(19f, 20f)
                close()
                moveTo(7f, 11f)
                lineTo(9f, 11f)
                lineTo(9f, 13f)
                lineTo(7f, 13f)
                close()
                moveTo(11f, 11f)
                lineTo(13f, 11f)
                lineTo(13f, 13f)
                lineTo(11f, 13f)
                close()
                moveTo(15f, 11f)
                lineTo(17f, 11f)
                lineTo(17f, 13f)
                lineTo(15f, 13f)
                close()
                moveTo(7f, 15f)
                lineTo(9f, 15f)
                lineTo(9f, 17f)
                lineTo(7f, 17f)
                close()
                moveTo(11f, 15f)
                lineTo(13f, 15f)
                lineTo(13f, 17f)
                lineTo(11f, 17f)
                close()
                moveTo(15f, 15f)
                lineTo(17f, 15f)
                lineTo(17f, 17f)
                lineTo(15f, 17f)
                close()
            }
        }.build()
    }

    // Кастомная иконка для Anytime (Секционный ящик/сейф)
    val Anytime: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Anytime",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF2EB7CD)),
                stroke = SolidColor(Color(0xFF2EB7CD)),
                strokeLineWidth = 0.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.54f, 5.23f)
                lineTo(19.15f, 3.55f)
                curveToRelative(-0.26f, -0.32f, -0.65f, -0.55f, -1.09f, -0.55f)
                lineTo(5.94f, 3f)
                curveToRelative(-0.44f, 0f, -0.83f, 0.23f, -1.09f, 0.55f)
                lineTo(3.46f, 5.23f)
                curveToRelative(-0.29f, 0.35f, -0.46f, 0.8f, -0.46f, 1.27f)
                lineTo(3f, 19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                lineTo(19f, 21f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(21f, 6.5f)
                curveToRelative(0f, -0.47f, -0.17f, -0.92f, -0.46f, -1.27f)
                close()
                moveTo(5.12f, 5f)
                lineTo(6.12f, 5f)
                lineTo(17.88f, 5f)
                lineTo(18.88f, 5f)
                lineTo(19.7f, 6f)
                lineTo(4.3f, 6f)
                lineTo(5.12f, 5f)
                close()
                moveTo(19f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 8f)
                lineTo(19f, 8f)
                lineTo(19f, 19f)
                close()
                moveTo(15f, 11f)
                lineTo(9f, 11f)
                curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
                lineTo(8f, 12f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                lineTo(15f, 13f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                lineTo(16f, 12f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                close()
            }
        }.build()
    }

    // Кастомная иконка для Someday (Папка)
    val Someday: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Someday",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF8F93A3)),
                stroke = SolidColor(Color(0xFF8F93A3)),
                strokeLineWidth = 0.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 4f)
                lineTo(4f, 4f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(2f, 18f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                lineTo(20f, 20f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(22f, 8f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                lineTo(12f, 6f)
                lineTo(10f, 4f)
                close()
                moveTo(20f, 18f)
                lineTo(4f, 18f)
                lineTo(4f, 8f)
                lineTo(20f, 8f)
                lineTo(20f, 18f)
                close()
            }
        }.build()
    }

    // Кастомная иконка для Logbook (Блокнот с галочкой)
    val Logbook: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Logbook",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF2EC275)),
                stroke = SolidColor(Color(0xFF2EC275)),
                strokeLineWidth = 0.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 3f)
                lineTo(14.82f, 3f)
                curveToRelative(-0.4f, -1.17f, -1.5f, -2f, -2.82f, -2f)
                curveToRelative(-1.32f, 0f, -2.42f, 0.83f, -2.82f, 2f)
                lineTo(5f, 3f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                lineTo(3f, 19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                lineTo(19f, 21f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(21f, 5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(12f, 3f)
                curveToRelative(0.55f, 0f, 1f, 0.45f, 1f, 1f)
                curveToRelative(0f, 0.55f, -0.45f, 1f, -1f, 1f)
                curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
                curveToRelative(0f, -0.55f, 0.45f, -1f, 1f, -1f)
                close()
                moveTo(19f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 19f)
                close()
                moveTo(10f, 14.17f)
                lineTo(7.41f, 11.59f)
                lineTo(6f, 13f)
                lineTo(10f, 17f)
                lineTo(18f, 9f)
                lineTo(16.59f, 7.58f)
                lineTo(10f, 14.17f)
                close()
            }
        }.build()
    }

    // Используем by lazy для чистой ленивой инициализации
    val Evening: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "EveningIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                // Цвета оставлены оригинальными. 
                // Замените на Color.Black, если хотите, чтобы иконка красилась под тему приложения.
                fill = SolidColor(Color(0xFF91ADD9)),
                stroke = SolidColor(Color(0xFF89A5D1)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.4f, 1.5f)
                curveToRelative(-4.96f, 0.8515f, -8.5857f, 5.1589f, -8.5857f, 10.1995f)
                curveToRelative(0f, 5.7153f, 4.6255f, 10.3484f, 10.3312f, 10.3483f)
                curveToRelative(4.6317f, -0.0002f, 8.6972f, -3.0882f, 9.9473f, -7.5554f)
                curveToRelative(-1.5454f, 1.2192f, -3.4565f, 1.8822f, -5.4248f, 1.8821f)
                curveToRelative(-4.8369f, -0.0001f, -8.7579f, -3.9211f, -8.758f, -8.758f)
                curveToRelative(0f, -2.2856f, 0.8937f, -4.4808f, 2.4899f, -6.1166f)
                close()
            }
        }.build()
    }
}
