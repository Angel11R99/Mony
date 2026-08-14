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

class IncomeExpenseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent { IncomeExpenseContent(context, snapshot) }
    }
}

@Composable
private fun IncomeExpenseContent(context: Context, snapshot: WidgetSnapshot) {
    val colors = financeWidgetColors(context)
    val total = snapshot.incomeInCents + snapshot.expenseInCents
    val incomeFraction = if (total <= 0) 0.5f else snapshot.incomeInCents.toFloat() / total
    val barWidth = 126.dp
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
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Text(
            text = context.getString(R.string.widget_income_expense_title),
            style = TextStyle(
                color = colors.secondaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(9.dp))
        Row(GlanceModifier.width(barWidth).height(9.dp)) {
            Spacer(GlanceModifier.width(incomeWidth).height(9.dp).background(colors.primary))
            Spacer(
                GlanceModifier
                    .width(barWidth - incomeWidth)
                    .height(9.dp)
                    .background(colors.accent),
            )
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_income),
                    style = TextStyle(color = colors.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
                Text(
                    text = MoneyFormatter.format(snapshot.incomeInCents),
                    style = TextStyle(
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_expense),
                    style = TextStyle(color = colors.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
                Text(
                    text = "−${MoneyFormatter.format(snapshot.expenseInCents)}",
                    style = TextStyle(
                        color = colors.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

class IncomeExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IncomeExpenseWidget()
}
