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
import com.example.personalfinancetracker.domain.model.TransactionType

/**
 * "Balance y registro rápido": available money plus quick add actions.
 * Adapts its layout to square (~2x2), medium (~4x2) and large (~4x3) sizes.
 */
class FinanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadCoreSnapshot(context)
        provideContent { FinanceWidgetContent(context, snapshot) }
    }

    @Composable
    private fun FinanceWidgetContent(context: Context, snapshot: WidgetCoreSnapshot) {
        val theme = WidgetTheme.of(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(theme.background)
                .clickable(actionStartActivity(openFinanceApp(context)))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            when (LocalSize.current) {
                SMALL_SIZE -> CompactContent(context, snapshot, theme)
                MEDIUM_SIZE -> MediumContent(context, snapshot, theme)
                else -> LargeContent(context, snapshot, theme)
            }
        }
    }

    @Composable
    private fun ColumnScope.CompactContent(
        context: Context,
        snapshot: WidgetCoreSnapshot,
        theme: WidgetTheme,
    ) {
        Text(
            text = context.getString(R.string.widget_available),
            style = TextStyle(color = theme.secondaryText, fontSize = 10.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = MoneyFormatter.format(snapshot.availableInCents),
            style = TextStyle(color = theme.primaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(GlanceModifier.defaultWeight())
        WidgetPillButton(
            text = context.getString(R.string.widget_add_expense),
            action = actionStartActivity(addTransactionIntent(context, TransactionType.EXPENSE)),
            containerColor = theme.chipSurface,
            contentColor = theme.accent,
            modifier = GlanceModifier.fillMaxWidth(),
            buttonHeight = 30,
        )
        Spacer(GlanceModifier.height(6.dp))
        WidgetPillButton(
            text = context.getString(R.string.widget_add_income),
            action = actionStartActivity(addTransactionIntent(context, TransactionType.INCOME)),
            containerColor = theme.primary,
            contentColor = theme.onPrimary(),
            modifier = GlanceModifier.fillMaxWidth(),
            buttonHeight = 30,
        )
    }

    @Composable
    private fun ColumnScope.MediumContent(
        context: Context,
        snapshot: WidgetCoreSnapshot,
        theme: WidgetTheme,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_available),
                    style = TextStyle(color = theme.secondaryText, fontSize = 11.sp),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = MoneyFormatter.format(snapshot.availableInCents),
                    style = TextStyle(color = theme.primaryText, fontSize = 21.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_latest_expense),
                    style = TextStyle(color = theme.secondaryText, fontSize = 11.sp),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = snapshot.latestExpense?.let { "−${MoneyFormatter.format(it.amountInCents)}" } ?: "—",
                    style = TextStyle(color = theme.accent, fontSize = 21.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth()) {
            WidgetPillButton(
                text = context.getString(R.string.widget_add_expense),
                action = actionStartActivity(addTransactionIntent(context, TransactionType.EXPENSE)),
                containerColor = theme.chipSurface,
                contentColor = theme.accent,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            WidgetPillButton(
                text = context.getString(R.string.widget_add_income),
                action = actionStartActivity(addTransactionIntent(context, TransactionType.INCOME)),
                containerColor = theme.primary,
                contentColor = theme.onPrimary(),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }

    @Composable
    private fun ColumnScope.LargeContent(
        context: Context,
        snapshot: WidgetCoreSnapshot,
        theme: WidgetTheme,
    ) {
        Text(
            text = context.getString(R.string.widget_available),
            style = TextStyle(color = theme.secondaryText, fontSize = 11.sp),
            maxLines = 1,
        )
        Text(
            text = MoneyFormatter.format(snapshot.availableInCents),
            style = TextStyle(color = theme.primaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(12.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            WidgetMetric(
                label = context.getString(R.string.widget_period_income),
                value = MoneyFormatter.format(snapshot.incomeInCents),
                valueColor = theme.primary,
                labelColor = theme.secondaryText,
                modifier = GlanceModifier.defaultWeight(),
            )
            WidgetMetric(
                label = context.getString(R.string.widget_period_expense),
                value = MoneyFormatter.format(snapshot.expenseInCents),
                valueColor = theme.accent,
                labelColor = theme.secondaryText,
                modifier = GlanceModifier.defaultWeight(),
            )
            WidgetMetric(
                label = context.getString(R.string.widget_latest_expense),
                value = snapshot.latestExpense?.let { "−${MoneyFormatter.format(it.amountInCents)}" } ?: "—",
                valueColor = theme.primaryText,
                labelColor = theme.secondaryText,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth()) {
            WidgetPillButton(
                text = context.getString(R.string.widget_add_expense),
                action = actionStartActivity(addTransactionIntent(context, TransactionType.EXPENSE)),
                containerColor = theme.chipSurface,
                contentColor = theme.accent,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            WidgetPillButton(
                text = context.getString(R.string.widget_add_income),
                action = actionStartActivity(addTransactionIntent(context, TransactionType.INCOME)),
                containerColor = theme.primary,
                contentColor = theme.onPrimary(),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }

    companion object {
        private val SMALL_SIZE = DpSize(150.dp, 140.dp)
        private val MEDIUM_SIZE = DpSize(280.dp, 130.dp)
        private val LARGE_SIZE = DpSize(320.dp, 220.dp)
    }
}

class FinanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinanceWidget()
}
