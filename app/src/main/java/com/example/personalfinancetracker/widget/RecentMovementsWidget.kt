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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.personalfinancetracker.R

/**
 * "Últimos movimientos": list of the most recent transactions. Each row opens
 * the movement in the editor; the widget itself opens the history module.
 */
class RecentMovementsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(MEDIUM_SIZE, LARGE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadCoreSnapshot(context)
        provideContent { RecentMovementsContent(context, snapshot) }
    }

    @Composable
    private fun RecentMovementsContent(context: Context, snapshot: WidgetCoreSnapshot) {
        val theme = WidgetTheme.of(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .clickable(actionStartActivity(openDestinationIntent(context, "history")))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                WidgetHeader(
                    text = context.getString(R.string.widget_recent_title),
                    iconRes = R.drawable.ic_w_receipt,
                    tint = theme.secondaryText,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = context.getString(R.string.widget_open_history),
                    style = TextStyle(color = theme.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(7.dp))

            if (snapshot.recent.isEmpty()) {
                WidgetEmptyState(
                    message = context.getString(R.string.widget_no_movements),
                    hint = context.getString(R.string.widget_no_movements_hint),
                    secondaryColor = theme.secondaryText,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            } else {
                val items = if (LocalSize.current == MEDIUM_SIZE) {
                    snapshot.recent.take(3)
                } else {
                    snapshot.recent.take(6)
                }
                items.forEachIndexed { index, movement ->
                    MovementRow(context, movement, theme)
                    if (index != items.lastIndex) {
                        Spacer(GlanceModifier.height(6.dp))
                        WidgetListRowSeparator(theme.track)
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun MovementRow(context: Context, movement: MovementLine, theme: WidgetTheme) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(
                    actionStartActivity(
                        editTransactionIntent(context, movement.id, movement.isIncome),
                    ),
                ),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            WidgetDot(color = if (movement.isIncome) theme.primary else theme.accent)
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = movement.categoryName
                        ?: context.getString(R.string.widget_no_category),
                    style = TextStyle(color = theme.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(
                        movement.description,
                        formatShortDate(movement.date),
                    ).joinToString(" · "),
                    style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = signedAmountLabel(movement.isIncome, movement.amountInCents),
                style = TextStyle(
                    color = if (movement.isIncome) theme.primary else theme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }

    companion object {
        private val MEDIUM_SIZE = DpSize(280.dp, 130.dp)
        private val LARGE_SIZE = DpSize(320.dp, 220.dp)
    }
}

class RecentMovementsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentMovementsWidget()
}
