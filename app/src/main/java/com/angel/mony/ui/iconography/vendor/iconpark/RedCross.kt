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
 * IconPark `red-cross` (48x48) as a Compose [ImageVector].
 *
 * [ink] replaces the collection's #000 outline so the artwork follows light and dark themes
 * while the #2F88FF, #43CCF8 and #fff fills stay untouched. A fresh vector is built on every
 * call, so callers should cache it for the composition (for example with `remember`).
 */
public fun IconPark.redCross(ink: Color): ImageVector = ImageVector.Builder(
    name = "IconPark.red-cross",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    addPath(
        pathData = PathParser().parsePathString("M4,24A20,20 0 1 0 44,24A20,20 0 1 0 4,24Z").toNodes(),
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
        pathData = PathParser().parsePathString("M27 12H21V21L12 21V27H21V36H27V27L36 27V21H27V12Z").toNodes(),
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
}.build()
