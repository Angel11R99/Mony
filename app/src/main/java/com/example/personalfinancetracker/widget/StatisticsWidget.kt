package com.example.personalfinancetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.core.MoneyFormatter

/**
 * "Resumen estadístico": balance, income/expense proportions and, in the large
 * variant, top expense categories and trend against the previous cycle.
 */
class StatisticsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(MEDIUM_SIZE, LARGE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadCoreSnapshot(context)
        provideContent { StatisticsContent(context, snapshot) }
    }

    @Composable
    private fun StatisticsContent(context: Context, snapshot: WidgetCoreSnapshot) {
        val theme = WidgetTheme.of(context)
        val total = snapshot.incomeInCents + snapshot.expenseInCents
        val incomeFraction = if (total <= 0) 0.5f else snapshot.incomeInCents.toFloat() / total
        val incomePercent = (incomeFraction * 100).toInt()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(theme.background)
                .clickable(actionStartActivity(openDestinationIntent(context, "statistics")))
                .padding(horizontal = 15.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = context.getString(R.string.widget_statistics_title),
                    style = TextStyle(color = theme.secondaryText, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = context.resources.getQuantityString(
                        R.plurals.widget_movements,
                        snapshot.transactionCount,
                        snapshot.transactionCount,
                    ),
                    style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = MoneyFormatter.format(snapshot.balanceInCents),
                style = TextStyle(
                    color = if (snapshot.balanceInCents < 0) theme.accent else theme.primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = context.getString(R.string.widget_balance),
                style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(8.dp))
            WidgetProgressBar(
                fraction = incomeFraction,
                totalWidth = LocalSize.current.width - 30.dp,
                fillColor = theme.primary,
                trackColor = theme.accent,
                barHeight = 9,
            )
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "${context.getString(R.string.widget_income)} · $incomePercent%",
                    style = TextStyle(color = theme.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = "${context.getString(R.string.widget_expense)} · ${100 - incomePercent}%",
                    style = TextStyle(color = theme.accent, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(start = 8.dp),
                )
            }

            if (LocalSize.current != MEDIUM_SIZE) {
                Spacer(GlanceModifier.height(9.dp))
                WidgetListRowSeparator(theme.track)
                Spacer(GlanceModifier.height(7.dp))
                if (snapshot.topExpenseCategories.isEmpty() && snapshot.expenseInCents <= 0L) {
                    TrendLine(context, snapshot, theme)
                } else {
                    TopCategories(context, snapshot, theme)
                    Spacer(GlanceModifier.height(7.dp))
                    TrendLine(context, snapshot, theme)
                }
            } else {
                Spacer(GlanceModifier.height(2.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(
                            text = MoneyFormatter.format(snapshot.incomeInCents),
                            style = TextStyle(color = theme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                    Column(
                        GlanceModifier.defaultWeight().padding(start = 8.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "−${MoneyFormatter.format(snapshot.expenseInCents)}",
                            style = TextStyle(color = theme.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TopCategories(context: Context, snapshot: WidgetCoreSnapshot, theme: WidgetTheme) {
        Text(
            text = context.getString(R.string.widget_top_categories),
            style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(5.dp))
        snapshot.topExpenseCategories.take(3).forEach { slice ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                WidgetDot(color = theme.primary)
                Spacer(GlanceModifier.width(7.dp))
                Text(
                    text = slice.name,
                    style = TextStyle(color = theme.primaryText, fontSize = 11.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = MoneyFormatter.format(slice.amountInCents),
                    style = TextStyle(color = theme.primaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    private fun TrendLine(context: Context, snapshot: WidgetCoreSnapshot, theme: WidgetTheme) {
        val trend = expenseTrend(snapshot.expenseInCents, snapshot.previousCycleExpenseInCents)
        val label = when (trend?.direction) {
            ExpenseTrend.Direction.UP -> context.getString(R.string.widget_trend_up, trend.percent ?: 0)
            ExpenseTrend.Direction.DOWN -> context.getString(R.string.widget_trend_down, trend.percent ?: 0)
            ExpenseTrend.Direction.FLAT -> context.getString(R.string.widget_trend_flat)
            null ->
                if (snapshot.previousCycleExpenseInCents == null || snapshot.previousCycleExpenseInCents == 0L) {
                    context.getString(R.string.widget_no_previous_cycle)
                } else {
                    return
                }
        }
        val color = when (trend?.direction) {
            ExpenseTrend.Direction.UP -> theme.accent
            ExpenseTrend.Direction.DOWN -> theme.primary
            else -> theme.secondaryText
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = label,
                style = TextStyle(color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
    }

    companion object {
        private val MEDIUM_SIZE = DpSize(280.dp, 130.dp)
        private val LARGE_SIZE = DpSize(320.dp, 220.dp)
    }
}

class StatisticsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatisticsWidget()
}
