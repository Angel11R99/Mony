package com.angel.mony.widget

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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

/**
 * Section header with a small tinted icon followed by the uppercase label,
 * mirroring the in-app screen headers.
 */
@Composable
fun WidgetHeader(
    text: String,
    @DrawableRes iconRes: Int,
    tint: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
        WidgetIcon(iconRes = iconRes, tint = tint)
        Spacer(GlanceModifier.width(5.dp))
        Text(
            text = text,
            style = TextStyle(color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
fun WidgetIcon(
    @DrawableRes iconRes: Int,
    tint: ColorProvider,
    size: Dp = 13.dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = null,
        colorFilter = androidx.glance.ColorFilter.tint(tint),
        modifier = modifier.width(size).height(size),
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
            .cornerRadius((barHeight / 2).dp)
            .background(trackColor),
        contentAlignment = Alignment.CenterStart,
    ) {
        Spacer(
            GlanceModifier
                .width(totalWidth * bounded)
                .height(barHeight.dp)
                .cornerRadius((barHeight / 2).dp)
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
            .cornerRadius((buttonHeight / 2).dp)
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

/**
 * Rounded chip for short contextual labels (trend, days left, states).
 */
@Composable
fun WidgetChip(
    text: String,
    containerColor: ColorProvider,
    contentColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier
            .cornerRadius(10.dp)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = text,
            style = TextStyle(color = contentColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

/**
 * Large tappable tile used by action-grid widgets: icon on top, label below.
 * With [iconOnly] the label is omitted for very narrow layouts.
 */
@Composable
fun WidgetActionTile(
    label: String,
    @DrawableRes iconRes: Int,
    iconTint: ColorProvider,
    labelColor: ColorProvider,
    surfaceColor: ColorProvider,
    action: Action,
    modifier: GlanceModifier = GlanceModifier,
    iconOnly: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(surfaceColor)
            .clickable(action)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetIcon(iconRes = iconRes, tint = iconTint, size = 20.dp)
        if (!iconOnly) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = label,
                style = TextStyle(color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
    }
}

/**
 * Metric block with the value above a small uppercase label.
 */
@Composable
fun WidgetValueLabel(
    value: String,
    label: String,
    valueColor: ColorProvider,
    labelColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    valueSize: Int = 15,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = alignment) {
        Text(
            text = value,
            style = TextStyle(color = valueColor, fontSize = valueSize.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = label,
            style = TextStyle(color = labelColor, fontSize = 9.sp),
            maxLines = 1,
        )
    }
}

/**
 * List row usage bar: name + amount on top, thin progress line below.
 * [barWidth] must be derived from LocalSize by the caller so it adapts on
 * resize (Glance has no fractional widths).
 */
@Composable
fun WidgetUsageRow(
    title: String,
    endText: String,
    fraction: Float,
    barWidth: Dp,
    fillColor: ColorProvider,
    trackColor: ColorProvider,
    titleColor: ColorProvider,
    endTextColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = title,
                style = TextStyle(color = titleColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = endText,
                style = TextStyle(color = endTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        WidgetProgressBar(
            fraction = fraction,
            totalWidth = barWidth,
            fillColor = fillColor,
            trackColor = trackColor,
            barHeight = 5,
        )
    }
}
