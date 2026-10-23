package com.example.ui.screens.home.inlineeditor.dialogs

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.RecurrenceRule
import com.example.domain.repeat.RepeatRule
import com.example.ui.screens.home.components.RepeatAction
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.dimens

/** Ширина и поля окна — как у «Коли?» */
private val TEMPLATE_DIALOG_WIDTH = 336.dp
private val TEMPLATE_DIALOG_VERTICAL_PADDING = 18.dp
private val TEMPLATE_DIALOG_GAP_AFTER_TITLE = 18.dp
private val TEMPLATE_DIALOG_BUTTON_HEIGHT = 40.dp
/** Сколько ближайших дат повтора в окне */
private const val TEMPLATE_DIALOG_DATES = 3

/**
 * Окно шаблона повтора, как «When?» в Things (нажатие на строку повтора в карточке шаблона): правило
 * предложением, ближайшие даты повтора полностью («субота, 24 жовтня 2026») или «Пауза» / «Чекає», и кнопки
 * «Змінити правило», «Наступна копія», «Попередня копія», «Пауза» («Продовжити»), «Зупинити». Стиль — окна «Коли?».
 */
@Composable
fun RepeatTemplateDialog(
    rule: RepeatRule,
    stored: RecurrenceRule?,
    onAction: (RepeatAction) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val paused = stored?.isActive == false
    val zone = java.time.ZoneId.systemDefault()
    val (dates, more) = if (stored == null || paused) emptyList<java.time.LocalDate>() to false else {
        com.example.domain.repeat.RepeatPlan.upcoming(
            com.example.domain.repeat.RepeatState(
                rule = rule,
                nextOccurrence = stored.nextGenerationDate.takeIf { it > 0 }
                    ?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() },
                generated = stored.generatedCount,
                active = stored.isActive
            ),
            TEMPLATE_DIALOG_DATES
        )
    }
    val fullDate = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", java.util.Locale.getDefault())
    val datesText = when {
        paused -> stringResource(R.string.repeat_paused)
        dates.isEmpty() -> stringResource(R.string.repeat_waiting)
        else -> dates.joinToString("\n") { fullDate.format(it).replaceFirstChar { c -> c.titlecase() } } +
            // «Після виконання» знает только ближайший повтор — одна дата, без «…»
            if (more && !rule.afterCompletion) "\n…" else ""
    }
    fun pick(action: RepeatAction) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onDismiss()
        onAction(action)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = ThingsTheme.shapes.dialogShape,
            colors = CardDefaults.cardColors(containerColor = ThingsTheme.colors.overlaySurface),
            modifier = Modifier.width(TEMPLATE_DIALOG_WIDTH)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ThingsSpacing.L, vertical = TEMPLATE_DIALOG_VERTICAL_PADDING)
            ) {
                // Заголовок «Коли?» и ✓ в круге справа — закрыть, как «When?» шаблона в Things
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.weight(5f), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.ui_when),
                            style = ThingsTheme.type.dialogTitle.copy(color = ThingsTheme.colors.overlayContent)
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DialogHeaderCircleButton(Icons.Default.Check, stringResource(R.string.cd_close), onDismiss)
                    }
                }
                Spacer(modifier = Modifier.height(TEMPLATE_DIALOG_GAP_AFTER_TITLE))
                // Правило — начертанием даты, под ним серым — ближайшие даты
                Text(
                    text = repeatRuleSentence(rule),
                    // Начертанием даты в карточке (Medium), не жирнее
                    style = ThingsTheme.type.dialogRowAction.copy(color = ThingsTheme.colors.overlayContent)
                )
                Spacer(modifier = Modifier.height(ThingsSpacing.M))
                Text(
                    text = datesText,
                    style = ThingsTheme.type.dialogBody.copy(color = ThingsTheme.colors.overlayContentSecondary)
                )
                Spacer(modifier = Modifier.height(ThingsSpacing.XL))
                // Кнопки полужирные, как в Things
                Column(verticalArrangement = Arrangement.spacedBy(ThingsSpacing.S)) {
                    TemplateDialogButton(stringResource(R.string.repeat_action_edit)) { pick(RepeatAction.EDIT) }
                    TemplateDialogButton(stringResource(R.string.repeat_action_next_copy)) { pick(RepeatAction.NEXT_COPY) }
                    TemplateDialogButton(stringResource(R.string.repeat_action_previous)) { pick(RepeatAction.PREVIOUS_COPY) }
                    if (paused) {
                        TemplateDialogButton(stringResource(R.string.repeat_action_resume)) { pick(RepeatAction.RESUME) }
                    } else {
                        TemplateDialogButton(stringResource(R.string.repeat_action_pause)) { pick(RepeatAction.PAUSE) }
                    }
                    Spacer(modifier = Modifier.height(ThingsSpacing.S))
                    // «Зупинити» — красной кнопкой, как «Очистити» в «Коли?»
                    TemplateDialogButton(stringResource(R.string.repeat_action_stop), destructive = true) { pick(RepeatAction.STOP) }
                }
            }
        }
    }
}

@Composable
private fun TemplateDialogButton(text: String, destructive: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) ThingsTheme.colors.clearAction else ThingsTheme.colors.overlayControl,
            contentColor = ThingsTheme.colors.overlayContent
        ),
        shape = ThingsTheme.shapes.capsuleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(TEMPLATE_DIALOG_BUTTON_HEIGHT)
    ) {
        Text(text = text, style = ThingsTheme.type.button)
    }
}
