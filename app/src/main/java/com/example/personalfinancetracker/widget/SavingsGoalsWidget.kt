package com.example.personalfinancetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress

/**
 * "Metas de ahorro": progress of savings goals. Square variant highlights the
 * most advanced goal; wide variants list up to three goals plus totals.
 */
class SavingsGoalsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SQUARE_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSavingsSnapshot(context)
        provideContent { SavingsGoalsContent(context, snapshot) }
    }

    @Composable
    private fun SavingsGoalsContent(context: Context, snapshot: SavingsWidgetSnapshot) {
        val theme = WidgetTheme.of(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .clickable(actionStartActivity(openDestinationIntent(context, "savings")))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (snapshot.isEmpty) {
                WidgetEmptyState(
                    message = context.getString(R.string.widget_savings_empty),
                    hint = context.getString(R.string.widget_savings_empty_hint),
                    secondaryColor = theme.secondaryText,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            } else if (LocalSize.current == SQUARE_SIZE) {
                GoalHighlight(context, snapshot.goals.first(), theme, snapshot)
            } else {
                GoalsList(context, snapshot, theme)
            }
        }
    }

    @Composable
    private fun ColumnScope.GoalHighlight(
        context: Context,
        goal: SavingsGoalProgress,
        theme: WidgetTheme,
        snapshot: SavingsWidgetSnapshot,
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            WidgetIcon(iconRes = R.drawable.ic_w_target, tint = theme.secondaryText)
            Spacer(GlanceModifier.width(5.dp))
            Text(
                text = context.getString(R.string.widget_savings_title),
                style = TextStyle(color = theme.secondaryText, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = goal.goal.name,
            style = TextStyle(color = theme.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "${goal.percent}%",
                style = TextStyle(
                    color = if (goal.isCompleted) theme.primary else theme.primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = MoneyFormatter.format(goal.savedInCents),
                style = TextStyle(color = theme.secondaryText, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        GoalBar(goal, theme)
        Spacer(GlanceModifier.height(5.dp))
        Text(
            text = if (goal.isCompleted) {
                context.getString(R.string.widget_savings_goal_completed)
            } else {
                context.getString(
                    R.string.widget_savings_saved_of,
                    MoneyFormatter.format(goal.savedInCents),
                    MoneyFormatter.format(goal.goal.targetAmountInCents),
                )
            },
            style = TextStyle(
                color = if (goal.isCompleted) theme.primary else theme.secondaryText,
                fontSize = 9.sp,
                fontWeight = if (goal.isCompleted) FontWeight.Medium else FontWeight.Normal,
            ),
            maxLines = 1,
        )
        val remaining = snapshot.goals.size - 1
        if (remaining > 0) {
            Spacer(GlanceModifier.height(3.dp))
            Text(
                text = context.getString(R.string.widget_savings_more_goals, remaining),
                style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun ColumnScope.GoalsList(context: Context, snapshot: SavingsWidgetSnapshot, theme: WidgetTheme) {
        WidgetHeader(
            text = context.getString(R.string.widget_savings_title),
            iconRes = R.drawable.ic_w_target,
            tint = theme.secondaryText,
        )
        Spacer(GlanceModifier.height(7.dp))
        snapshot.goals.take(3).forEachIndexed { index, goal ->
            GoalRow(goal, theme)
            if (index != minOf(2, snapshot.goals.lastIndex)) Spacer(GlanceModifier.height(8.dp))
        }
        Spacer(GlanceModifier.defaultWeight())
        if (snapshot.totalTargetInCents > 0L) {
            WidgetListRowSeparator(theme.track)
            Spacer(GlanceModifier.height(5.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                Text(
                    text = context.getString(
                        R.string.widget_savings_total_saved,
                        MoneyFormatter.format(snapshot.totalSavedInCents),
                    ),
                    style = TextStyle(color = theme.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = context.getString(
                        R.string.widget_savings_total_target,
                        MoneyFormatter.format(snapshot.totalTargetInCents),
                    ),
                    style = TextStyle(color = theme.secondaryText, fontSize = 10.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(start = 8.dp),
                )
            }
        }
    }

    /**
     * Progress bar that stretches with the available width; the percentage sits
     * beside it so resizes never clip the label.
     */
    @Composable
    private fun GoalBar(goal: SavingsGoalProgress, theme: WidgetTheme) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            WidgetProgressBar(
                fraction = goal.percent / 100f,
                totalWidth = LocalSize.current.width - 28.dp - PERCENT_RESERVE.dp,
                fillColor = theme.primary,
                trackColor = theme.track,
                barHeight = 7,
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "${goal.percent}%",
                style = TextStyle(
                    color = if (goal.isCompleted) theme.primary else theme.secondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun GoalRow(goal: SavingsGoalProgress, theme: WidgetTheme) {
        Text(
            text = goal.goal.name,
            style = TextStyle(
                color = theme.primaryText,
                fontSize = 12.sp,
                fontWeight = if (goal.isCompleted) FontWeight.Bold else FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(3.dp))
        GoalBar(goal, theme)
    }

    companion object {
        private val SQUARE_SIZE = DpSize(150.dp, 140.dp)
        private val WIDE_SIZE = DpSize(280.dp, 130.dp)

        /** Space kept to the right of each bar for the "NN%" label. */
        private const val PERCENT_RESERVE = 42
    }
}

class SavingsGoalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SavingsGoalsWidget()
}
