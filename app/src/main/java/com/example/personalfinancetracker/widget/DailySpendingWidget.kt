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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.TransactionType

/**
 * "Gasto de hoy": how much was spent today against the remaining daily pace.
 * Square variant shows the essentials; wide variant adds cycle context and a
 * quick add-expense action. Without a budget it falls back to today's total.
 */
class DailySpendingWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SQUARE_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadCoreSnapshot(context)
        provideContent { DailySpendingContent(context, snapshot) }
    }

    @Composable
    private fun DailySpendingContent(context: Context, snapshot: WidgetCoreSnapshot) {
        val theme = WidgetTheme.of(context)
        val allowance = snapshot.dailyAllowanceInCents
        val paceFraction = dailyPaceFraction(snapshot.todayExpenseInCents, allowance)
        val overPace = allowance != null && snapshot.todayExpenseInCents > allowance

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .clickable(actionStartActivity(addTransactionIntent(context, TransactionType.EXPENSE)))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                WidgetHeader(
                    text = context.getString(R.string.widget_today_title),
                    iconRes = R.drawable.ic_w_calendar,
                    tint = theme.secondaryText,
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (overPace) {
                    WidgetChip(
                        text = context.getString(R.string.widget_today_over_pace),
                        containerColor = theme.chipSurface,
                        contentColor = theme.accent,
                    )
                }
            }

            if (allowance == null) {
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = MoneyFormatter.format(snapshot.todayExpenseInCents),
                    style = TextStyle(color = theme.primaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    text = context.getString(R.string.widget_today_spent_no_budget),
                    style = TextStyle(color = theme.secondaryText, fontSize = 10.sp),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.defaultWeight())
            } else {
                Spacer(GlanceModifier.height(4.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(
                            text = MoneyFormatter.format(snapshot.todayExpenseInCents),
                            style = TextStyle(
                                color = if (overPace) theme.accent else theme.primaryText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = context.getString(R.string.widget_today_spent),
                            style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                            maxLines = 1,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = MoneyFormatter.format(allowance),
                            style = TextStyle(
                                color = if (overPace) theme.accent else theme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = context.getString(R.string.widget_daily_allowance),
                            style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                            maxLines = 1,
                        )
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                WidgetProgressBar(
                    fraction = paceFraction,
                    totalWidth = LocalSize.current.width - 28.dp,
                    fillColor = if (overPace) theme.accent else theme.primary,
                    trackColor = theme.track,
                    barHeight = 8,
                )
                if (LocalSize.current != SQUARE_SIZE) {
                    Spacer(GlanceModifier.height(6.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = context.getString(
                                R.string.widget_today_cycle_spent,
                                MoneyFormatter.format(snapshot.expenseInCents),
                            ),
                            style = TextStyle(color = theme.secondaryText, fontSize = 10.sp),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        DaysLeftChip(context, snapshot, theme)
                    }
                }
            }
        }
    }

    companion object {
        private val SQUARE_SIZE = DpSize(140.dp, 130.dp)
        private val WIDE_SIZE = DpSize(280.dp, 110.dp)
    }
}

class DailySpendingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailySpendingWidget()
}
