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
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.PendingType
import java.time.LocalDate

/**
 * "Próximos pagos y cobros": upcoming pending reminders with totals.
 * Overdue and today rows are emphasized; the footer summarizes both flows.
 */
class PendingRemindersWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(MEDIUM_SIZE, LARGE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadPendingSnapshot(context)
        provideContent { PendingRemindersContent(context, snapshot) }
    }

    @Composable
    private fun PendingRemindersContent(context: Context, snapshot: PendingWidgetSnapshot) {
        val theme = WidgetTheme.of(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .clickable(actionStartActivity(openDestinationIntent(context, "pending")))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            WidgetHeader(
                text = context.getString(R.string.widget_pending_title),
                iconRes = R.drawable.ic_w_bell,
                tint = theme.secondaryText,
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.height(7.dp))

            if (snapshot.isEmpty) {
                WidgetEmptyState(
                    message = context.getString(R.string.widget_pending_empty),
                    hint = context.getString(R.string.widget_pending_empty_hint),
                    secondaryColor = theme.secondaryText,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            } else {
                val items =
                    if (LocalSize.current == MEDIUM_SIZE) snapshot.items.take(3) else snapshot.items.take(5)
                items.forEachIndexed { index, item ->
                    PendingRow(context, item, theme)
                    if (index != items.lastIndex) {
                        Spacer(GlanceModifier.height(5.dp))
                        WidgetListRowSeparator(theme.track)
                        Spacer(GlanceModifier.height(5.dp))
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                TotalsFooter(context, snapshot, theme)
            }
        }
    }

    @Composable
    private fun PendingRow(context: Context, item: PendingLine, theme: WidgetTheme) {
        val dayLabel = pendingDayLabel(item.date, LocalDate.now())
        val dateText = when (dayLabel.kind) {
            PendingDayKind.TODAY -> context.getString(R.string.widget_pending_today)
            PendingDayKind.TOMORROW -> context.getString(R.string.widget_pending_tomorrow)
            PendingDayKind.OVERDUE -> context.getString(R.string.widget_pending_overdue, dayLabel.dateText)
            PendingDayKind.ON_DATE -> dayLabel.dateText
        }
        val isPayment = item.type == PendingType.PAYMENT
        val urgent = dayLabel.kind == PendingDayKind.OVERDUE || dayLabel.kind == PendingDayKind.TODAY
        val dateColor = if (dayLabel.kind == PendingDayKind.OVERDUE) theme.accent else theme.secondaryText

        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            WidgetDot(color = if (isPayment) theme.accent else theme.primary)
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = item.description,
                    style = TextStyle(
                        color = theme.primaryText,
                        fontSize = 12.sp,
                        fontWeight = if (urgent) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = if (isPayment) context.getString(R.string.widget_payment)
                        else context.getString(R.string.widget_collection),
                        style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(text = "·", style = TextStyle(color = theme.secondaryText, fontSize = 9.sp), maxLines = 1)
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = dateText,
                        style = TextStyle(color = dateColor, fontSize = 9.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                }
            }
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = MoneyFormatter.format(item.amountInCents),
                style = TextStyle(
                    color = if (isPayment) theme.accent else theme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun TotalsFooter(context: Context, snapshot: PendingWidgetSnapshot, theme: WidgetTheme) {
        Spacer(GlanceModifier.height(6.dp))
        WidgetListRowSeparator(theme.track)
        Spacer(GlanceModifier.height(6.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Text(
                text = context.getString(R.string.widget_pending_to_pay, MoneyFormatter.format(snapshot.toPayInCents)),
                style = TextStyle(color = theme.accent, fontSize = 10.sp),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = context.getString(
                    R.string.widget_pending_to_collect,
                    MoneyFormatter.format(snapshot.toCollectInCents),
                ),
                style = TextStyle(color = theme.primary, fontSize = 10.sp),
                maxLines = 1,
                modifier = GlanceModifier.padding(start = 8.dp),
            )
        }
    }

    companion object {
        private val MEDIUM_SIZE = DpSize(280.dp, 130.dp)
        private val LARGE_SIZE = DpSize(320.dp, 220.dp)
    }
}

class PendingRemindersWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PendingRemindersWidget()
}
