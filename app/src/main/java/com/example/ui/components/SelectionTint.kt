package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawWithContent

/**
 * Заливка выделенной строки — поверх содержимого (foreground), а не фоном: так она ложится на все
 * элементы карточки задачи или проекта, в том числе на непрозрачные плашки, чипы и чекбоксы.
 */
fun Modifier.selectionTint(color: Color, shape: Shape = RectangleShape): Modifier = drawWithContent {
    drawContent()
    if (color.alpha > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), color)
}
