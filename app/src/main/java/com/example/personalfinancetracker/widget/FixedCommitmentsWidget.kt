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
import java.time.ZoneId

/**
 * "Gastos fijos activos": commitment summary of active fixed entries and the
 * next automatic posting.
 */
class FixedCommitmentsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(MEDIUM_SIZE, LARGE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadFixedSnapshot(context)
        provideContent { FixedCommitmentsContent(context, snapshot) }
    }

    @Composable
    private fun FixedCommitmentsContent(context: Context, snapshot: FixedWidgetSnapshot) {
        val theme = WidgetTheme.of(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .clickable(actionStartActivity(openDestinationIntent(context, "fixed")))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (snapshot.isEmpty) {
                WidgetEmptyState(
                    message = context.getString(R.string.widget_fixed_empty),
                    hint = context.getString(R.string.widget_fixed_empty_hint),
                    secondaryColor = theme.secondaryText,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            } else if (LocalSize.current == MEDIUM_SIZE) {
                MediumContent(context, snapshot, theme)
            } else {
                LargeContent(context, snapshot, theme)
            }
        }
    }

    @Composable
    private fun MediumContent(context: Context, snapshot: FixedWidgetSnapshot, theme: WidgetTheme) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    WidgetIcon(iconRes = R.drawable.ic_w_repeat, tint = theme.secondaryText)
                    Spacer(GlanceModifier.width(5.dp))
                    Text(
                        text = context.getString(R.string.widget_fixed_title),
                        style = TextStyle(color = theme.secondaryText, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = MoneyFormatter.format(snapshot.expenseTotalInCents),
                    style = TextStyle(color = theme.accent, fontSize = 21.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    text = context.getString(
                        R.string.widget_fixed_entries_count,
                        snapshot.activeCount,
                    ),
                    style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${MoneyFormatter.format(snapshot.incomeTotalInCents)}",
                    style = TextStyle(color = theme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = nextRunLabel(context, snapshot),
                    style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    private fun LargeContent(context: Context, snapshot: FixedWidgetSnapshot, theme: WidgetTheme) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            WidgetHeader(
                text = context.getString(R.string.widget_fixed_title),
                iconRes = R.drawable.ic_w_repeat,
                tint = theme.secondaryText,
                modifier = GlanceModifier.defaultWeight(),
            )
            WidgetChip(
                text = nextRunLabel(context, snapshot),
                containerColor = theme.chipSurface,
                contentColor = theme.secondaryText,
            )
        }
        Spacer(GlanceModifier.height(7.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            WidgetMetric(
                label = context.getString(R.string.widget_expense),
                value = "−${MoneyFormatter.format(snapshot.expenseTotalInCents)}",
                valueColor = theme.accent,
                labelColor = theme.secondaryText,
                modifier = GlanceModifier.defaultWeight(),
            )
            WidgetMetric(
                label = context.getString(R.string.widget_income),
                value = "+${MoneyFormatter.format(snapshot.incomeTotalInCents)}",
                valueColor = theme.primary,
                labelColor = theme.secondaryText,
                modifier = GlanceModifier.defaultWeight(),
            )
            val netImpact = snapshot.incomeTotalInCents - snapshot.expenseTotalInCents
            WidgetMetric(
                label = context.getString(R.string.widget_fixed_net_impact),
                value = signedAmountLabel(isIncome = netImpact >= 0L, amountInCents = kotlin.math.abs(netImpact)),
                valueColor = if (netImpact >= 0L) theme.primary else theme.accent,
                labelColor = theme.secondaryText,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        Spacer(GlanceModifier.height(10.dp))
        WidgetListRowSeparator(theme.track)
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_fixed_top_list),
                style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = context.getString(R.string.widget_fixed_entries_count, snapshot.activeCount),
                style = TextStyle(color = theme.secondaryText, fontSize = 9.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(5.dp))
        snapshot.topEntries.forEach { entry ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                WidgetDot(color = if (entry.isIncome) theme.primary else theme.accent)
                Spacer(GlanceModifier.width(7.dp))
                Text(
                    text = entry.description,
                    style = TextStyle(color = theme.primaryText, fontSize = 11.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = signedAmountLabel(entry.isIncome, entry.amountInCents),
                    style = TextStyle(
                        color = if (entry.isIncome) theme.primary else theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }

    private fun nextRunLabel(context: Context, snapshot: FixedWidgetSnapshot): String =
        snapshot.nextRunAt?.let { next ->
            formatShortDate(next.atZone(ZoneId.systemDefault()).toLocalDate())
        } ?: context.getString(R.string.widget_fixed_no_next_run)

    companion object {
        private val MEDIUM_SIZE = DpSize(280.dp, 130.dp)
        private val LARGE_SIZE = DpSize(320.dp, 220.dp)
    }
}

class FixedCommitmentsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FixedCommitmentsWidget()
}
