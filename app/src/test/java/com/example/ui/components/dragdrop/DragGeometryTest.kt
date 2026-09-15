package com.example.ui.components.dragdrop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Закрепляет правила пересечения строк и компенсации прыжка в движке перетаскивания.
 *
 * Все строки в тестах высотой 100, поэтому пороги считаются в уме: при доле 0.7 обмен вниз
 * происходит, когда нижний край карточки зашёл за 70 пикселей от верха цели, а вверх —
 * когда верхний край поднялся выше 30 пикселей от её верха.
 */
class DragGeometryTest {

    private val far = 100_000

    // ─── Выбор цели обмена ────────────────────────────────────────────────────

    @Test
    fun `вниз — обмен только когда нижний край карточки перекрыл цель на порог`() {
        val slots = slots("a", "b", "c")

        assertNull(target(slots, dragged = "a", dragTop = 60f, deltaY = 1f))
        assertEquals("b", target(slots, dragged = "a", dragTop = 80f, deltaY = 1f)?.key)
    }

    @Test
    fun `вверх — обмен считается по верхнему краю карточки`() {
        val slots = slots("a", "b", "c")

        assertNull(target(slots, dragged = "c", dragTop = 140f, deltaY = -1f))
        assertEquals("b", target(slots, dragged = "c", dragTop = 120f, deltaY = -1f)?.key)
    }

    @Test
    fun `без движения решения нет`() {
        val slots = slots("a", "b", "c")

        assertNull(target(slots, dragged = "a", dragTop = 500f, deltaY = 0f))
    }

    @Test
    fun `недоступная строка не становится целью, порог считается по следующей`() {
        val slots = slots("a", "b", "c")

        // b пропускаем: целью становится c, а её порог на 100 пикселей ниже
        assertNull(target(slots, dragged = "a", dragTop = 150f, deltaY = 1f, canDropOver = { it != "b" }))
        assertEquals(
            "c",
            target(slots, dragged = "a", dragTop = 180f, deltaY = 1f, canDropOver = { it != "b" })?.key
        )
    }

    @Test
    fun `вниз порог считается по нижней строке составного блока`() {
        val slots = slots("a", "b", "c", "d")
        val block = { key: Any -> key == "c" }

        // Порог одиночной цели b был бы пройден, но блок b+c тянется до 300
        assertNull(target(slots, dragged = "a", dragTop = 100f, deltaY = 1f, isBlockContinuation = block))
        // Целью остаётся начало блока, а не его продолжение
        assertEquals(
            "b",
            target(slots, dragged = "a", dragTop = 180f, deltaY = 1f, isBlockContinuation = block)?.key
        )
    }

    @Test
    fun `цель, не помещающаяся в видимую область, не принимается`() {
        val slots = slots("a", "b", "c")
        val block = { key: Any -> key == "c" }

        // Хвост блока заканчивается на 300, а видно только до 250
        assertNull(
            target(
                slots, dragged = "a", dragTop = 180f, deltaY = 1f,
                viewportEnd = 250, totalItemsCount = 3, isBlockContinuation = block
            )
        )
    }

    @Test
    fun `блок, оборванный нижним краем экрана, не принимается`() {
        val slots = slots("a", "b", "c")
        val block = { key: Any -> key == "c" }

        // c — последняя видимая строка, но в списке есть ещё элементы: настоящая высота блока неизвестна
        assertNull(
            target(
                slots, dragged = "a", dragTop = 180f, deltaY = 1f,
                totalItemsCount = 10, isBlockContinuation = block
            )
        )
        // Тот же кадр, но в списке больше ничего нет — блок виден целиком
        assertEquals(
            "b",
            target(
                slots, dragged = "a", dragTop = 180f, deltaY = 1f,
                totalItemsCount = 3, isBlockContinuation = block
            )?.key
        )
    }

    @Test
    fun `цель выше верхней границы экрана не принимается`() {
        val slots = slots("a", "b", "c")

        assertNull(target(slots, dragged = "c", dragTop = 120f, deltaY = -1f, viewportStart = 150))
    }

    @Test
    fun `порог ограничен рабочим диапазоном`() {
        val slots = slots("a", "b", "c")

        // 5f ужимается до 0.9: обмен вниз произойдёт только у самого низа цели
        assertNull(target(slots, dragged = "a", dragTop = 80f, deltaY = 1f, threshold = 5f))
        assertEquals("b", target(slots, dragged = "a", dragTop = 95f, deltaY = 1f, threshold = 5f)?.key)
    }

    @Test
    fun `перетаскиваемой строки нет среди видимых — решения нет`() {
        val slots = slots("a", "b", "c")

        assertNull(target(slots, dragged = "нет такой", dragTop = 0f, deltaY = 1f))
    }

    // ─── Компенсация прыжка ───────────────────────────────────────────────────

    @Test
    fun `вниз компенсация равна шагу за нижний край цели`() {
        val slots = slots("a", "b", "c")

        assertEquals(-100f, jumpCompensation(slots, "a", "b")!!, 0f)
    }

    @Test
    fun `вниз компенсация учитывает продолжение блока`() {
        val slots = slots("a", "b", "c")

        assertEquals(-200f, jumpCompensation(slots, "a", "b") { it == "c" }!!, 0f)
    }

    @Test
    fun `вверх компенсация равна расстоянию до отступа цели`() {
        val slots = slots("a", "b", "c")

        assertEquals(200f, jumpCompensation(slots, "c", "a")!!, 0f)
    }

    @Test
    fun `компенсации нет, если строки или цели нет среди видимых`() {
        val slots = slots("a", "b", "c")

        assertNull(jumpCompensation(slots, "нет такой", "b"))
        assertNull(jumpCompensation(slots, "a", "нет такой"))
    }

    // ─── Фикстуры ─────────────────────────────────────────────────────────────

    private fun slots(vararg keys: String, size: Int = 100): List<DragSlot> =
        keys.mapIndexed { index, key -> DragSlot(key = key, index = index, offset = index * size, size = size) }

    private fun target(
        slots: List<DragSlot>,
        dragged: String,
        dragTop: Float,
        deltaY: Float,
        threshold: Float = 0.7f,
        viewportStart: Int = 0,
        viewportEnd: Int = far,
        totalItemsCount: Int = slots.size,
        canDropOver: (Any) -> Boolean = { true },
        isBlockContinuation: (Any) -> Boolean = { false },
    ): DragSlot? = findSwapTarget(
        slots = slots,
        draggedKey = dragged,
        dragTop = dragTop,
        deltaY = deltaY,
        thresholdFraction = threshold,
        viewportStart = viewportStart,
        viewportEnd = viewportEnd,
        totalItemsCount = totalItemsCount,
        canDropOver = canDropOver,
        isBlockContinuation = isBlockContinuation,
    )
}
