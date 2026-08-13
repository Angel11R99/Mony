package com.example.personalfinancetracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
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
import androidx.glance.unit.ColorProvider
import com.example.personalfinancetracker.MainActivity
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.availableForBudget
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class FinanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val transactions = entryPoint.transactions().observeAll().first()
        val budget = entryPoint.budgets().observe().first()
        val available = availableForBudget(budget, transactions)
        val latestExpense = transactions.firstOrNull { it.type == TransactionType.EXPENSE }
        provideContent { WidgetContent(context, available, latestExpense?.amountInCents) }
    }
}

@Composable
private fun WidgetContent(context: Context, available: Long, latestExpense: Long?) {
    val primaryText = ColorProvider(Color(0xFFF7F4EF))
    val secondaryText = ColorProvider(Color(0xFFA7A7AD))
    val buttonColors = ButtonDefaults.buttonColors(
        backgroundColor = ColorProvider(Color(0xFF53689F)),
        contentColor = primaryText,
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_available),
                    style = TextStyle(color = secondaryText, fontSize = 11.sp),
                    maxLines = 1,
                )
                Text(
                    text = MoneyFormatter.format(available),
                    style = TextStyle(
                        color = primaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_latest_expense),
                    style = TextStyle(color = secondaryText, fontSize = 11.sp),
                    maxLines = 1,
                )
                Text(
                    text = latestExpense?.let { "−${MoneyFormatter.format(it)}" } ?: "—",
                    style = TextStyle(
                        color = if (latestExpense == null) secondaryText else ColorProvider(Color(0xFFFF5D73)),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.height(12.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Button(
                text = context.getString(R.string.widget_add_expense),
                onClick = actionStartActivity(addIntent(context, TransactionType.EXPENSE)),
                modifier = GlanceModifier.defaultWeight().height(40.dp),
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                colors = buttonColors,
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(8.dp))
            Button(
                text = context.getString(R.string.widget_add_income),
                onClick = actionStartActivity(addIntent(context, TransactionType.INCOME)),
                modifier = GlanceModifier.defaultWeight().height(40.dp),
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                colors = buttonColors,
                maxLines = 1,
            )
        }
    }
}

private fun addIntent(context: Context, type: TransactionType) =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_TRANSACTION_TYPE, type.name)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

class FinanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinanceWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun transactions(): TransactionRepository
    fun budgets(): BudgetRepository
}
