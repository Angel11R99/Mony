package com.angel.mony.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.angel.mony.R

class VoiceRegistrationWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content(context) }
    }

    @Composable
    private fun Content(context: Context) {
        val theme = WidgetTheme.of(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(theme.background)
                .padding(12.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            WidgetActionTile(
                label = context.getString(R.string.widget_voice_action),
                iconRes = R.drawable.ic_w_mic,
                iconTint = theme.primary,
                labelColor = theme.primaryText,
                surfaceColor = theme.chipSurface,
                action = actionStartActivity(voiceTransactionIntent(context)),
                iconOnly = false,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
    }
}

class VoiceRegistrationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VoiceRegistrationWidget()
}
