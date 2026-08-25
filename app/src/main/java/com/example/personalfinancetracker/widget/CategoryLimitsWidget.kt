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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.core.MoneyFormatter

/**
 * "Límites por categoría": usage of the spending limits configured per
 * category during the current cycle. Categories without a limit fall back to
 * their share of total expenses. Medium (~4x2) and large (~4x3) variants.
 */
class CategoryLimitsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(MEDIUM_SIZE, LARGE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadCoreSnapshot(context)
        provideContent { CategoryLimitsContent(context, snapshot) }
    }

    @Composable
    private fun CategoryLimitsContent(context: Context, snapshot: WidgetCoreSnapshot) {
        val theme = WidgetTheme.of(context)
        val usages = buildCategoryLimitUsages(snapshot.topExpenseCategories)
        val maxRows = if (LocalSize.current == MEDIUM_SIZE) 3 else 5

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .clickable(actionStartActivity(openDestinationIntent(context, "settings")))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            WidgetHeader(
                text = context.getString(R.string.widget_limits_title),
                iconRes = R.drawable.ic_w_gauge,
                tint = theme.secondaryText,
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.height(7.dp))

            if (snapshot.topExpenseCategories.isEmpty()) {
                WidgetEmptyState(
                    message = context.getString(R.string.widget_limits_empty),
                    hint = context.getString(R.string.widget_limits_empty_hint),
                    secondaryColor = theme.secondaryText,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            } else {
                usages.take(maxRows).forEachIndexed { index, usage ->
                    val hasLimit = usage.limitInCents > 0L
                    WidgetUsageRow(
                        title = usage.slice.name,
                        endText = if (hasLimit) {
                            context.getString(
                                R.string.widget_limits_spent_of_limit,
                                MoneyFormatter.format(usage.slice.amountInCents),
                                MoneyFormatter.format(usage.limitInCents),
                            )
                        } else {
                            MoneyFormatter.format(usage.slice.amountInCents)
                        },
                        fraction = if (hasLimit) usage.fraction else usage.slice.fraction,
                        barWidth = LocalSize.current.width - 28.dp,
                        fillColor = if (usage.isOverLimit) theme.accent else theme.primary,
                        trackColor = theme.chipSurface,
                        titleColor = if (usage.isOverLimit) theme.accent else theme.primaryText,
                        endTextColor = theme.secondaryText,
                    )
                    if (index != minOf(maxRows, usages.size) - 1) {
                        Spacer(GlanceModifier.height(7.dp))
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                if (usages.none { it.limitInCents > 0L }) {
                    Text(
                        text = context.getString(R.string.widget_limits_no_limits_hint),
                        style = TextStyle(
                            color = theme.secondaryText,
                            fontSize = 9.sp,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }

    companion object {
        private val MEDIUM_SIZE = DpSize(280.dp, 130.dp)
        private val LARGE_SIZE = DpSize(320.dp, 220.dp)
    }
}

class CategoryLimitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CategoryLimitsWidget()
}
