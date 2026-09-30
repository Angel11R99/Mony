package com.angel.mony.ui.iconography.vendor.iconpark

// GENERATED FILE - do not edit by hand.
// Source: @iconify-json/icon-park@1.2.4 (IconPark by ByteDance, Apache-2.0).
// Regenerate with: node tools/iconpark/generate.mjs --source <extracted>/package/icons.json

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * IconPark `alarm-clock` (48x48) as a Compose [ImageVector].
 *
 * The geometry is shared by every IconPark theme; [palette] selects the theme by supplying the
 * four colour slots IconPark remaps at runtime. A fresh vector is built on every call, so callers
 * should cache it for the composition (for example with `remember`).
 */
public fun IconPark.alarmClock(palette: IconParkPalette): ImageVector = ImageVector.Builder(
    name = "IconPark.alarm-clock",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    addPath(
        pathData = PathParser().parsePathString("M23.9998 44.3332C34.1251 44.3332 42.3332 36.1251 42.3332 25.9999C42.3332 15.8747 34.1251 7.66656 23.9998 7.66656C13.8746 7.66656 5.6665 15.8747 5.6665 25.9999C5.6665 36.1251 13.8746 44.3332 23.9998 44.3332Z").toNodes(),
        fill = palette.outerFill.brush(),
        fillAlpha = 1f,
        stroke = SolidColor(palette.outerStroke),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
    addPath(
        pathData = PathParser().parsePathString("M23.7594 15.3536L23.7582 26.3624L31.5305 34.1347").toNodes(),
        fill = null,
        fillAlpha = 1f,
        stroke = SolidColor(palette.innerStroke),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
    addPath(
        pathData = PathParser().parsePathString("M4 9.00001L11 4.00001").toNodes(),
        fill = null,
        fillAlpha = 1f,
        stroke = SolidColor(palette.outerStroke),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
    addPath(
        pathData = PathParser().parsePathString("M44 9.00001L37 4.00001").toNodes(),
        fill = null,
        fillAlpha = 1f,
        stroke = SolidColor(palette.outerStroke),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
}.build()
