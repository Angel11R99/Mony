package com.example.personalfinancetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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

class StatisticsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent { StatisticsContent(context, snapshot) }
    }
}

@Composable
private fun StatisticsContent(context: Context, snapshot: WidgetSnapshot) {
    val colors = financeWidgetColors(context)
    val total = snapshot.incomeInCents + snapshot.expenseInCents
    val incomeFraction = if (total <= 0) 0.5f else snapshot.incomeInCents.toFloat() / total
    val incomePercent = if (total <= 0) 0 else (incomeFraction * 100).toInt()
    val barWidth = 214.dp
    val incomeWidth = barWidth * incomeFraction.coerceIn(0f, 1f)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(
                ImageProvider(
                    if (colors.dark) R.drawable.widget_background_dark
                    else R.drawable.widget_background_light,
                ),
            )
            .clickable(actionStartActivity(openFinanceApp(context)))
            .padding(horizontal = 15.dp, vertical = 12.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = context.getString(R.string.widget_statistics_title),
                style = TextStyle(
                    color = colors.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = context.resources.getQuantityString(
                    R.plurals.widget_movements,
                    snapshot.transactionCount,
                    snapshot.transactionCount,
                ),
                style = TextStyle(color = colors.secondaryText, fontSize = 9.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = MoneyFormatter.format(snapshot.balanceInCents),
            style = TextStyle(
                color = if (snapshot.balanceInCents < 0) colors.accent else colors.primaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(
            text = context.getString(R.string.widget_balance),
            style = TextStyle(color = colors.secondaryText, fontSize = 9.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.width(barWidth).height(9.dp)) {
            Spacer(GlanceModifier.width(incomeWidth).height(9.dp).background(colors.primary))
            Spacer(
                GlanceModifier
                    .width(barWidth - incomeWidth)
                    .height(9.dp)
                    .background(colors.accent),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            StatisticValue(
                label = "${context.getString(R.string.widget_income)} · $incomePercent%",
                amount = MoneyFormatter.format(snapshot.incomeInCents),
                color = colors.primary,
                modifier = GlanceModifier.defaultWeight(),
            )
            StatisticValue(
                label = "${context.getString(R.string.widget_expense)} · ${100 - incomePercent}%",
                amount = "−${MoneyFormatter.format(snapshot.expenseInCents)}",
                color = colors.accent,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun StatisticValue(
    label: String,
    amount: String,
    color: androidx.glance.unit.ColorProvider,
    modifier: GlanceModifier,
) {
    Column(modifier) {
        Text(
            text = label,
            style = TextStyle(color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        Text(
            text = amount,
            style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

class StatisticsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatisticsWidget()
}
