package com.example.personalfinancetracker.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.unit.ColorProvider

/**
 * Small uppercase section label with the "◆" marker used by the app screens.
 */
@Composable
fun WidgetSectionLabel(text: String, color: ColorProvider, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium),
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun WidgetMetric(
    label: String,
    value: String,
    valueColor: ColorProvider,
    labelColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier) {
        Text(
            text = label,
            style = TextStyle(color = labelColor, fontSize = 9.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = value,
            style = TextStyle(color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

/**
 * Progress bar with an explicit [totalWidth] because Glance does not support
 * fractional widths. Callers derive the width from LocalSize so it adapts on
 * resize.
 */
@Composable
fun WidgetProgressBar(
    fraction: Float,
    totalWidth: Dp,
    fillColor: ColorProvider,
    trackColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    barHeight: Int = 8,
) {
    val bounded = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .width(totalWidth)
            .height(barHeight.dp)
            .background(trackColor),
        contentAlignment = Alignment.CenterStart,
    ) {
        Spacer(
            GlanceModifier
                .width(totalWidth * bounded)
                .height(barHeight.dp)
                .background(fillColor),
        )
    }
}

/**
 * Pill action button matching the app's rounded button language.
 */
@Composable
fun WidgetPillButton(
    text: String,
    action: Action,
    containerColor: ColorProvider,
    contentColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    buttonHeight: Int = 36,
) {
    Row(
        modifier = modifier
            .height(buttonHeight.dp)
            .background(containerColor)
            .clickable(action),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = text,
            style = TextStyle(color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.padding(horizontal = 12.dp),
        )
    }
}

/**
 * Small colored marker used instead of per-category icons in lists.
 */
@Composable
fun WidgetDot(color: ColorProvider, modifier: GlanceModifier = GlanceModifier) {
    Spacer(
        modifier
            .width(8.dp)
            .height(8.dp)
            .cornerRadius(4.dp)
            .background(color),
    )
}

@Composable
fun WidgetEmptyState(
    message: String,
    hint: String?,
    secondaryColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = TextStyle(color = secondaryColor, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 2,
        )
        if (hint != null) {
            Spacer(GlanceModifier.height(3.dp))
            Text(
                text = hint,
                style = TextStyle(color = secondaryColor, fontSize = 10.sp),
                maxLines = 2,
            )
        }
    }
}

@Composable
fun WidgetListRowSeparator(color: ColorProvider) {
    Spacer(
        GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}
