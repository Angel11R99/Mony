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
import androidx.glance.layout.Box
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

class BudgetProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent { BudgetProgressContent(context, snapshot) }
    }
}

@Composable
private fun BudgetProgressContent(context: Context, snapshot: WidgetSnapshot) {
    val colors = financeWidgetColors(context)
    val budget = snapshot.budget?.amountInCents ?: 0L
    val ratio = if (budget <= 0) 0f else snapshot.expenseInCents.toFloat() / budget
    val boundedRatio = ratio.coerceIn(0f, 1f)
    val barWidth = 198.dp

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
            .padding(horizontal = 15.dp, vertical = 10.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_available),
                    style = TextStyle(color = colors.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
                Text(
                    text = MoneyFormatter.format(snapshot.availableInCents),
                    style = TextStyle(
                        color = colors.primaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Text(
                text = if (budget > 0) context.getString(
                    R.string.widget_percentage_used,
                    (ratio * 100).toInt(),
                ) else context.getString(R.string.widget_no_budget),
                style = TextStyle(
                    color = if (ratio > 1f) colors.accent else colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(7.dp))
        Box(
            modifier = GlanceModifier
                .width(barWidth)
                .height(8.dp)
                .background(colors.track),
            contentAlignment = Alignment.CenterStart,
        ) {
            Spacer(
                GlanceModifier
                    .width(barWidth * boundedRatio)
                    .height(8.dp)
                    .background(if (ratio > 1f) colors.accent else colors.primary),
            )
        }
    }
}

class BudgetProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetProgressWidget()
}
