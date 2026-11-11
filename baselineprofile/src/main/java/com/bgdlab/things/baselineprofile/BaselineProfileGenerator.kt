package com.bgdlab.things.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Сценарий, по которому снимается профиль запуска: всё, что приложение при нём выполняет, Android потом
 * компилирует заранее, при установке. Сюда входит то, что человек делает сразу после запуска: главный
 * экран, прокрутка, открытие списка и возврат.
 *
 * Запуск: `./gradlew :app:generateBaselineProfile` с подключённым телефоном — см. build.gradle.kts модуля.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val packageName = QA_PACKAGE

        rule.collect(packageName = packageName, includeInStartupProfile = true) {
            // Холодный старт до первого кадра главного экрана
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.pkg(packageName).depth(0)), TIMEOUT_MS)
            device.waitForIdle()

            scrollHome()
            openFirstListAndReturn(packageName)
        }
    }

    /** Прокрутка главного экрана вниз и обратно — списки, строки и их анимации */
    private fun MacrobenchmarkScope.scrollHome() {
        val width = device.displayWidth
        val height = device.displayHeight
        // Жест — в середине экрана: не задевает системные жесты у краёв и оттяжку поиска у верха списка
        device.swipe(width / 2, height * 6 / 10, width / 2, height * 3 / 10, SWIPE_STEPS)
        device.waitForIdle()
        device.swipe(width / 2, height * 4 / 10, width / 2, height * 6 / 10, SWIPE_STEPS)
        device.waitForIdle()
    }

    /** Открыть первый список главного экрана («Входящие»), прокрутить его и вернуться */
    private fun MacrobenchmarkScope.openFirstListAndReturn(packageName: String) {
        val row = device.findObjects(By.pkg(packageName).clickable(true)).firstOrNull { it.visibleBounds.height() > 0 } ?: return
        row.click()
        device.waitForIdle()
        // Жест по координатам, а не через найденный элемент: экран перестраивается, и элемент «устаревает»
        device.swipe(device.displayWidth / 2, device.displayHeight * 6 / 10, device.displayWidth / 2, device.displayHeight * 4 / 10, SWIPE_STEPS)
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    private companion object {
        /** Тестовая сборка «Things QA» со своими данными — не рабочее приложение */
        const val QA_PACKAGE = "com.bgdlab.things.qa"
        const val TIMEOUT_MS = 5_000L
        const val SWIPE_STEPS = 20
    }
}
