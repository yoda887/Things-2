package com.example.reminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.data.local.TaskDao
import com.example.domain.reminders.Reminders
import com.example.domain.usecase.TaskUseCases
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThingsAlpha
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * «Позже…» из уведомления-напоминания: окно поверх всего с выбором, на сколько отложить, как в Things —
 * через 15 минут, час, 3 часа или до завтра. Кнопок у уведомления не больше трёх, поэтому полный выбор —
 * здесь. Нажатие мимо карточки закрывает окно без изменений.
 */
@AndroidEntryPoint
class ReminderSnoozeActivity : ComponentActivity() {

    @Inject lateinit var dao: TaskDao
    @Inject lateinit var taskUseCases: TaskUseCases

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return finish()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        setContent {
            MyApplicationTheme {
                SnoozeChooser(
                    title = title.ifBlank { stringResource(R.string.untitled_task) },
                    onChoose = { choice -> snooze(taskId, choice) },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun snooze(taskId: String, choice: SnoozeChoice) {
        NotificationManagerCompat.from(this).cancel(taskId.hashCode())
        lifecycleScope.launch {
            val item = dao.getItemById(taskId)
            if (item != null && !item.isClosed && !item.trashed) {
                val snoozed = when (choice) {
                    is SnoozeChoice.Minutes -> Reminders.snoozed(item, choice.minutes)
                    SnoozeChoice.Tomorrow -> Reminders.snoozedUntilTomorrow(item)
                }
                taskUseCases.updateTask(snoozed)
            }
            finish()
        }
    }

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_TITLE = "title"

        fun intent(context: Context, taskId: String, title: String): Intent =
            Intent(context, ReminderSnoozeActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_TITLE, title)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
}

/** На сколько отложить напоминание */
private sealed interface SnoozeChoice {
    data class Minutes(val minutes: Int) : SnoozeChoice
    data object Tomorrow : SnoozeChoice
}

/** Варианты «Позже»: 15 минут, час, 3 часа — и до завтра */
private val SNOOZE_CHOICES = listOf(
    R.string.reminder_snooze_15m to SnoozeChoice.Minutes(15),
    R.string.reminder_snooze_1h to SnoozeChoice.Minutes(60),
    R.string.reminder_snooze_3h to SnoozeChoice.Minutes(180),
    R.string.reminder_snooze_tomorrow to SnoozeChoice.Tomorrow
)

@Composable
private fun SnoozeChooser(title: String, onChoose: (SnoozeChoice) -> Unit, onDismiss: () -> Unit) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThingsTheme.colors.scrim.copy(alpha = ThingsAlpha.MUTED))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(MaterialTheme.dimens.dialogWidthFraction)
                .clip(ThingsTheme.shapes.dialogShape)
                .background(ThingsTheme.colors.overlaySurface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(vertical = ThingsSpacing.L),
            verticalArrangement = Arrangement.spacedBy(ThingsSpacing.XXS)
        ) {
            Text(
                text = title,
                style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ThingsSpacing.L)
            )
            Text(
                text = stringResource(R.string.reminder_snooze_title),
                style = ThingsTheme.type.caption.copy(color = ThingsTheme.colors.overlayContentSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = ThingsSpacing.S)
            )
            SNOOZE_CHOICES.forEachIndexed { index, (label, choice) ->
                if (index == SNOOZE_CHOICES.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = ThingsSpacing.L, vertical = ThingsSpacing.XS),
                        color = ThingsTheme.colors.overlayDivider,
                        thickness = ThingsStroke.THIN
                    )
                }
                Text(
                    text = stringResource(label),
                    style = ThingsTheme.type.dialogRow.copy(color = ThingsTheme.colors.overlayContent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.dimens.rowHeight)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onChoose(choice)
                        }
                        .padding(horizontal = ThingsSpacing.L_PLUS, vertical = ThingsSpacing.S)
                )
            }
        }
    }
}
