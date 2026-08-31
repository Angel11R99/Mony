package com.angel.mony.widget

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
import com.angel.mony.R
import com.angel.mony.core.MoneyFormatter

/**
 * "Ingresos vs gastos": tiny comparison of the current cycle.
 * Square variant keeps the essentials; wide variant adds the balance line.
 */
class IncomeExpenseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SQUARE_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadCoreSnapshot(context)
        provideContent { IncomeExpenseContent(context, snapshot) }
    }

    @Composable
    private fun IncomeExpenseContent(context: Context, snapshot: WidgetCoreSnapshot) {
        val theme = WidgetTheme.of(context)
        val total = snapshot.incomeInCents + snapshot.expenseInCents
        val incomeFraction = if (total <= 0) 0.5f else snapshot.incomeInCents.toFloat() / total
        val incomePercent = (incomeFraction * 100).toInt()
        val square = LocalSize.current == SQUARE_SIZE

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .clickable(actionStartActivity(openDestinationIntent(context, "statistics")))
                .padding(horizontal = 13.dp, vertical = 12.dp),
        ) {
            WidgetHeader(
                text = context.getString(R.string.widget_income_expense_title),
                iconRes = R.drawable.ic_w_bars,
                tint = theme.secondaryText,
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Text(
                text = context.getString(R.string.widget_current_cycle),
                style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())
            if (!square) {
                Row(GlanceModifier.fillMaxWidth()) {
                    WidgetValueLabel(
                        value = MoneyFormatter.format(snapshot.balanceInCents),
                        label = context.getString(R.string.widget_balance),
                        valueColor = if (snapshot.balanceInCents < 0) theme.accent else theme.primaryText,
                        labelColor = theme.secondaryText,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = MoneyFormatter.format(snapshot.incomeInCents),
                            style = TextStyle(
                                color = theme.primary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = "−${MoneyFormatter.format(snapshot.expenseInCents)}",
                            style = TextStyle(
                                color = theme.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
            }
            WidgetProgressBar(
                fraction = incomeFraction,
                totalWidth = LocalSize.current.width - 26.dp,
                fillColor = theme.primary,
                trackColor = theme.accent,
                barHeight = 9,
            )
            Spacer(GlanceModifier.height(8.dp))
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
        }
    }

    companion object {
        private val SQUARE_SIZE = DpSize(110.dp, 110.dp)
        private val WIDE_SIZE = DpSize(220.dp, 110.dp)
    }
}

class IncomeExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IncomeExpenseWidget()
}
