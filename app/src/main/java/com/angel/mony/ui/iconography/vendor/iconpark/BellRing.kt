package com.angel.mony.ui.iconography.vendor.iconpark

// GENERATED FILE - do not edit by hand.
// Source: @iconify-json/icon-park@1.2.4 (IconPark by ByteDance, Apache-2.0).
// Regenerate with: node tools/iconpark/generate.mjs --source <extracted>/package/icons.json

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * IconPark `bell-ring` (48x48) as a Compose [ImageVector].
 *
 * [ink] replaces the collection's #000 outline so the artwork follows light and dark themes
 * while the #2F88FF, #43CCF8 and #fff fills stay untouched. A fresh vector is built on every
 * call, so callers should cache it for the composition (for example with `remember`).
 */
public fun IconPark.bellRing(ink: Color): ImageVector = ImageVector.Builder(
    name = "IconPark.bell-ring",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    addPath(
        pathData = PathParser().parsePathString("M4,20A16,16 0 1 0 36,20A16,16 0 1 0 4,20Z").toNodes(),
        fill = SolidColor(Color(0xFF2F88FF)),
        fillAlpha = 1f,
        stroke = SolidColor(ink),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
    addPath(
        pathData = PathParser().parsePathString("M44 18V20H42V18H44Z").toNodes(),
        fill = SolidColor(Color(0xFF2F88FF)),
        fillAlpha = 1f,
        stroke = null,
        strokeAlpha = 1f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
    addPath(
        pathData = PathParser().parsePathString("M42 20H44V18H42V20ZM42 20C42 29.1371 36.4299 36.9732 28.5 40.2978").toNodes(),
        fill = null,
        fillAlpha = 1f,
        stroke = SolidColor(ink),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
    addPath(
        pathData = PathParser().parsePathString("M14 35L10 44H30L26 35").toNodes(),
        fill = null,
        fillAlpha = 1f,
        stroke = SolidColor(ink),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
    addPath(
        pathData = PathParser().parsePathString("M16,20A4,4 0 1 0 24,20A4,4 0 1 0 16,20Z").toNodes(),
        fill = SolidColor(Color(0xFF43CCF8)),
        fillAlpha = 1f,
        stroke = SolidColor(Color(0xFFFFFFFF)),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
    addPath(
        pathData = PathParser().parsePathString("M10 20C10 14.4772 14.4772 10 20 10").toNodes(),
        fill = null,
        fillAlpha = 1f,
        stroke = SolidColor(Color(0xFFFFFFFF)),
        strokeAlpha = 1f,
        strokeLineWidth = 4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
    )
}.build()
