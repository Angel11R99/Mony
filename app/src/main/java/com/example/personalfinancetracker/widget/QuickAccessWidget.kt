package com.example.personalfinancetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.domain.model.TransactionType

/**
 * "Acceso rápido": pure action grid. Square (~2x2) shows a 2x2 tile grid;
 * wide (~4x1) shows labeled tiles in a row; compact keeps icon-only tiles so
 * nothing gets truncated. No data is loaded, so it renders instantly.
 */
class QuickAccessWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode =
        SizeMode.Responsive(setOf(COMPACT_SIZE, SQUARE_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { QuickAccessContent(context) }
    }

    @Composable
    private fun QuickAccessContent(context: Context) {
        val theme = WidgetTheme.of(context)
        val tiles = listOf(
            QuickTile(
                label = context.getString(R.string.widget_quick_expense),
                iconRes = R.drawable.ic_w_minus,
                iconTint = theme.accent,
                action = actionStartActivity(addTransactionIntent(context, TransactionType.EXPENSE)),
            ),
            QuickTile(
                label = context.getString(R.string.widget_quick_income),
                iconRes = R.drawable.ic_w_plus,
                iconTint = theme.primary,
                action = actionStartActivity(addTransactionIntent(context, TransactionType.INCOME)),
            ),
            QuickTile(
                label = context.getString(R.string.widget_quick_history),
                iconRes = R.drawable.ic_w_history,
                iconTint = theme.primaryText,
                action = actionStartActivity(openDestinationIntent(context, "history")),
            ),
            QuickTile(
                label = context.getString(R.string.widget_quick_pending),
                iconRes = R.drawable.ic_w_bell,
                iconTint = theme.primaryText,
                action = actionStartActivity(openDestinationIntent(context, "pending")),
            ),
        )
        val size = LocalSize.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .padding(8.dp),
        ) {
            when {
                size == SQUARE_SIZE -> {
                    Row(GlanceModifier.defaultWeight()) {
                        Tile(tiles[0], theme, false, GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(6.dp))
                        Tile(tiles[1], theme, false, GlanceModifier.defaultWeight())
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    Row(GlanceModifier.defaultWeight()) {
                        Tile(tiles[2], theme, false, GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(6.dp))
                        Tile(tiles[3], theme, false, GlanceModifier.defaultWeight())
                    }
                }
                size == COMPACT_SIZE -> {
                    Row(GlanceModifier.fillMaxSize()) {
                        tiles.forEachIndexed { index, tile ->
                            if (index > 0) Spacer(GlanceModifier.width(6.dp))
                            Tile(tile, theme, iconOnly = true, modifier = GlanceModifier.defaultWeight())
                        }
                    }
                }
                else -> {
                    Row(GlanceModifier.fillMaxSize()) {
                        tiles.forEachIndexed { index, tile ->
                            if (index > 0) Spacer(GlanceModifier.width(6.dp))
                            Tile(tile, theme, iconOnly = false, modifier = GlanceModifier.defaultWeight())
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Tile(tile: QuickTile, theme: WidgetTheme, iconOnly: Boolean, modifier: GlanceModifier) {
        WidgetActionTile(
            label = tile.label,
            iconRes = tile.iconRes,
            iconTint = tile.iconTint,
            labelColor = theme.primaryText,
            surfaceColor = theme.chipSurface,
            action = tile.action,
            iconOnly = iconOnly,
            modifier = modifier,
        )
    }

    private data class QuickTile(
        val label: String,
        val iconRes: Int,
        val iconTint: androidx.glance.unit.ColorProvider,
        val action: androidx.glance.action.Action,
    )

    companion object {
        private val COMPACT_SIZE = DpSize(110.dp, 52.dp)
        private val SQUARE_SIZE = DpSize(140.dp, 140.dp)
        private val WIDE_SIZE = DpSize(300.dp, 52.dp)
    }
}

class QuickAccessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAccessWidget()
}
