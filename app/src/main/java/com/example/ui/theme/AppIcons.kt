package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted

// Группируем все кастомные иконки в один объект для удобного автокомплита
object AppIcons {

    // Кастомная иконка для Inbox (Ящик) из ic_inbox_black_24dp.xml
    val Inbox: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Inbox",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF23A9EB)),
                stroke = SolidColor(Color(0xFF21A1DF)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 2f)
                horizontalLineToRelative(-16f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(15f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineToRelative(-15f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(20f, 12.5f)
                horizontalLineToRelative(-3f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, 1f)
                verticalLineToRelative(5f)
                horizontalLineToRelative(-8f)
                verticalLineToRelative(-5f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, -1f)
                horizontalLineToRelative(-3f)
                verticalLineToRelative(-7f)
                horizontalLineToRelative(16f)
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

    // Кастомная иконка для Upcoming (Календарь-сетка) из ic_upcoming_black_24dp.xml
    val Upcoming: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Upcoming",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFD30062)),
                stroke = SolidColor(Color(0xFFC5015C)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 4.3f)
                horizontalLineToRelative(2f)
                arcToRelative(0.3f, 0.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.3f, 0.3f)
                verticalLineToRelative(1.5f)
                arcToRelative(0.3f, 0.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.3f, 0.3f)
                horizontalLineToRelative(-2f)
                arcToRelative(0.3f, 0.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.3f, -0.3f)
                verticalLineToRelative(-1.5f)
                arcToRelative(0.3f, 0.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.3f, -0.3f)
                close()
                moveToRelative(-11f, 0f)
                horizontalLineToRelative(2f)
                arcToRelative(0.3f, 0.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.3f, 0.3f)
                verticalLineToRelative(1.5f)
                arcToRelative(0.3f, 0.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.3f, 0.3f)
                horizontalLineToRelative(-2f)
                arcToRelative(0.3f, 0.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.3f, -0.3f)
                verticalLineToRelative(-1.5f)
                arcToRelative(0.3f, 0.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.3f, -0.3f)
                close()
                moveTo(18f, 3f)
                horizontalLineToRelative(-12f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, -4f, 4f)
                lineToRelative(0f, 10f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4f, 4f)
                horizontalLineToRelative(12f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4f, -4f)
                lineToRelative(0f, -10f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, -4f, -4f)
                close()
                moveToRelative(3.5f, 14f)
                arcToRelative(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3.5f, 3.5f)
                horizontalLineToRelative(-12f)
                arcToRelative(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3.5f, -3.5f)
                lineToRelative(0f, -9.5f)
                horizontalLineToRelative(19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFFD25961)),
                strokeLineWidth = 1.2f
            ) {
                moveTo(5.5f, 10.5f)
                horizontalLineToRelative(3f)
                moveToRelative(2f, 0f)
                horizontalLineToRelative(3f)
                moveToRelative(2f, 0f)
                horizontalLineToRelative(3f)
                moveTo(5.5f, 13.5f)
                horizontalLineToRelative(3f)
                moveToRelative(2f, 0f)
                horizontalLineToRelative(3f)
                moveToRelative(2f, 0f)
                horizontalLineToRelative(3f)
                moveTo(5.5f, 16.5f)
                horizontalLineToRelative(3f)
                moveToRelative(2f, 0f)
                horizontalLineToRelative(3f)
                moveToRelative(2f, 0f)
                horizontalLineToRelative(3f)
            }
        }.build()
    }

    // Кастомная иконка для Anytime из ic_anytime_black_24dp.xml
    val Anytime: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Anytime",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF3BA39A))
            ) {
                moveToRelative(2f, 8.5f)
                lineToRelative(10f, 6.5f)
                lineToRelative(10f, -6.5f)
                lineToRelative(-10f, -6.5f)
                close()
                moveToRelative(0.5f, 3.5f)
                lineToRelative(9.5f, 6.5f)
                lineToRelative(9.5f, -6.5f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -2f)
                lineToRelative(-9.5f, 6.5f)
                lineToRelative(-9.5f, -6.5f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 2f)
                close()
                moveToRelative(0f, 3.5f)
                lineToRelative(9.5f, 6.5f)
                lineToRelative(9.5f, -6.5f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -2f)
                lineToRelative(-9.5f, 6.5f)
                lineToRelative(-9.5f, -6.5f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 2f)
                close()
            }
        }.build()
    }

    // Кастомная иконка для Someday из ic_someday_black_24dp.xml
    val Someday: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Someday",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFCEC493)),
                stroke = SolidColor(Color(0xFFC6BD8F)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveToRelative(2f, 2f)
                lineToRelative(0f, 4.5f)
                arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.5f, 0.5f)
                lineToRelative(19f, 0f)
                arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.5f, -0.5f)
                lineToRelative(0f, -4.5f)
                close()
                moveToRelative(0.75f, 7f)
                lineToRelative(0f, 11f)
                arcToRelative(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.8f, 0.8f)
                lineToRelative(17f, 0f)
                arcToRelative(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.8f, -0.8f)
                lineToRelative(0f, -11f)
                close()
                moveToRelative(6.25f, 1.85f)
                lineToRelative(6.2f, 0f)
                arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.25f, 0.25f)
                lineToRelative(0f, 1.6f)
                arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.25f, 0.25f)
                lineToRelative(-6.2f, 0f)
                arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.25f, -0.25f)
                lineToRelative(0f, -1.6f)
                arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.25f, -0.25f)
                close()
            }
        }.build()
    }

    // Кастомная иконка для Logbook из ic_logbook_black_24dp.xml
    val Logbook: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "Logbook",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF51BD63)),
                stroke = SolidColor(Color(0xFF50B661)),
                strokeLineWidth = 1f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.7f, 17f)
                lineToRelative(-4f, -4f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(2.59f, 2.58f)
                lineToRelative(5.59f, -5.59f)
                lineToRelative(1.41f, 1.42f)
                lineToRelative(-7f, 7f)
                close()
                moveToRelative(-6f, -13f)
                verticalLineToRelative(15f)
                curveToRelative(0f, 1f, 1f, 2f, 2f, 2f)
                lineToRelative(11f, 1f)
                curveToRelative(1f, 0f, 2f, -1f, 2f, -2f)
                verticalLineToRelative(-13f)
                curveToRelative(0f, -1f, -1f, -2f, -2f, -2f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF50B661)),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.5f, 19f)
                verticalLineToRelative(-15f)
                curveToRelative(0f, -1f, -1f, -2f, -2f, -2f)
                horizontalLineToRelative(-13.5f)
                curveToRelative(-1f, 0f, -2f, 1f, -2f, 2f)
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

    val Area: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = "CustomArea",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 15.5f)
                quadToRelative(0f, 2f, 1.8f, 2.9f)
                lineToRelative(6.4f, 3.2f)
                quadToRelative(1.8f, 0.9f, 3.6f, 0f)
                lineToRelative(6.4f, -3.2f)
                quadToRelative(1.8f, -0.9f, 1.8f, -2.9f)
                lineToRelative(0f, -7f)
                quadToRelative(0f, -2f, -1.8f, -2.9f)
                lineToRelative(-6.4f, -3.2f)
                quadToRelative(-1.8f, -0.9f, -3.6f, 0f)
                lineToRelative(-6.4f, 3.2f)
                quadToRelative(-1.8f, 0.9f, -1.8f, 2.9f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 6.5f)
                curveToRelative(-1f, 1.2f, -0.2f, 1.9f, 0.8f, 2.4f)
                lineTo(10.2f, 12.1f)
                quadTo(12f, 13f, 13.8f, 12.1f)
                lineTo(20.2f, 8.9f)
                curveToRelative(1f, -0.5f, 1.8f, -1.2f, 0.8f, -2.4f)
            }
        }.build()
    }

    // Иконка буллет-списка (чеклист / мультивыбор задач)
    val BulletList: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        androidx.compose.material.icons.Icons.Outlined.FormatListBulleted
    }
}
