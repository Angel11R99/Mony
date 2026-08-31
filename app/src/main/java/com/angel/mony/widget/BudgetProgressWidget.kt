package com.angel.mony.widget

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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.angel.mony.R
import com.angel.mony.core.MoneyFormatter

/**
 * "Uso del presupuesto": usage tracking bar with days remaining.
 * Renders a thin strip (~4x1) or an expanded card (~4x2) depending on size.
 */
class BudgetProgressWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(THIN_SIZE, TALL_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadCoreSnapshot(context)
        provideContent { BudgetProgressContent(context, snapshot) }
    }

    @Composable
    private fun BudgetProgressContent(context: Context, snapshot: WidgetCoreSnapshot) {
        val theme = WidgetTheme.of(context)
        val budget = snapshot.budget?.amountInCents ?: 0L
        val ratio = if (budget <= 0) 0f else snapshot.expenseInCents.toFloat() / budget
        val overspent = ratio > 1f

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .clickable(actionStartActivity(openFinanceApp(context)))
                .padding(horizontal = 14.dp),
        ) {
            if (snapshot.budget == null || budget <= 0L) {
                WidgetEmptyState(
                    message = context.getString(R.string.widget_no_budget),
                    hint = context.getString(R.string.widget_configure_budget),
                    secondaryColor = theme.secondaryText,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            } else if (LocalSize.current == THIN_SIZE) {
                Spacer(GlanceModifier.height(8.dp))
                ThinContent(context, snapshot, theme, ratio, overspent)
                Spacer(GlanceModifier.height(8.dp))
            } else {
                Spacer(GlanceModifier.height(10.dp))
                TallContent(context, snapshot, theme, ratio, overspent)
                Spacer(GlanceModifier.height(10.dp))
            }
        }
    }

    @Composable
    private fun ThinContent(
        context: Context,
        snapshot: WidgetCoreSnapshot,
        theme: WidgetTheme,
        ratio: Float,
        overspent: Boolean,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_available),
                    style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
                Text(
                    text = MoneyFormatter.format(snapshot.availableInCents),
                    style = TextStyle(color = theme.primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(10.dp))
            WidgetProgressBar(
                fraction = ratio,
                totalWidth = LocalSize.current.width * THIN_BAR_FRACTION,
                fillColor = if (overspent) theme.accent else theme.primary,
                trackColor = theme.track,
                barHeight = 7,
            )
            Spacer(GlanceModifier.width(10.dp))
            Text(
                text = context.getString(R.string.widget_percentage_used, (ratio * 100).toInt()),
                style = TextStyle(
                    color = if (overspent) theme.accent else theme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun TallContent(
        context: Context,
        snapshot: WidgetCoreSnapshot,
        theme: WidgetTheme,
        ratio: Float,
        overspent: Boolean,
    ) {
        Row(GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    WidgetIcon(iconRes = R.drawable.ic_w_gauge, tint = theme.secondaryText, size = 11.dp)
                    Spacer(GlanceModifier.width(5.dp))
                    Text(
                        text = context.getString(R.string.widget_available),
                        style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                        maxLines = 1,
                    )
                }
                Text(
                    text = MoneyFormatter.format(snapshot.availableInCents),
                    style = TextStyle(color = theme.primaryText, fontSize = 19.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                if (overspent) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = context.getString(R.string.widget_budget_overspent),
                        style = TextStyle(color = theme.accent, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = context.getString(R.string.widget_percentage_used, (ratio * 100).toInt()),
                    style = TextStyle(
                        color = if (overspent) theme.accent else theme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(2.dp))
                DaysLeftText(context, snapshot, theme)
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        WidgetProgressBar(
            fraction = ratio,
            totalWidth = LocalSize.current.width - 28.dp,
            fillColor = if (overspent) theme.accent else theme.primary,
            trackColor = theme.track,
            barHeight = 9,
        )
        Spacer(GlanceModifier.height(6.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Text(
                text = context.getString(
                    R.string.widget_spent_of,
                    MoneyFormatter.format(snapshot.expenseInCents),
                    MoneyFormatter.format(snapshot.budget?.amountInCents ?: 0L),
                ),
                style = TextStyle(color = theme.secondaryText, fontSize = 10.sp),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            DailyPaceText(context, snapshot, theme)
        }
    }

    @Composable
    private fun DailyPaceText(context: Context, snapshot: WidgetCoreSnapshot, theme: WidgetTheme) {
        val allowance = snapshot.dailyAllowanceInCents ?: return
        val label = context.getString(R.string.widget_daily_pace, MoneyFormatter.format(allowance))
        Text(
            text = label,
            style = TextStyle(
                color = if (allowance < 0) theme.accent else theme.secondaryText,
                fontSize = 10.sp,
            ),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 8.dp),
        )
    }

    @Composable
    private fun DaysLeftText(context: Context, snapshot: WidgetCoreSnapshot, theme: WidgetTheme) {
        val daysLeft = cycleDaysLeft(snapshot.period, snapshot.today)
        val label = if (daysLeft == 0) {
            context.getString(R.string.widget_cycle_ends_today)
        } else {
            context.resources.getQuantityString(R.plurals.widget_days_left, daysLeft, daysLeft)
        }
        Text(text = label, style = TextStyle(color = theme.secondaryText, fontSize = 9.sp), maxLines = 1)
    }

    companion object {
        private val THIN_SIZE = DpSize(280.dp, 48.dp)
        private val TALL_SIZE = DpSize(280.dp, 110.dp)

        /** Thin bar takes this share of the widget width so it scales on resize. */
        private const val THIN_BAR_FRACTION = 0.32f
    }
}

class BudgetProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetProgressWidget()
}
