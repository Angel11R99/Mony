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
 * IconPark `notes` (48x48) as a Compose [ImageVector].
 *
 * The geometry is shared by every IconPark theme; [palette] selects the theme by supplying the
 * four colour slots IconPark remaps at runtime. A fresh vector is built on every call, so callers
 * should cache it for the composition (for example with `remember`).
 */
public fun IconPark.notes(palette: IconParkPalette): ImageVector = ImageVector.Builder(
    name = "IconPark.notes",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    addPath(
        pathData = PathParser().parsePathString("M8 6C8 4.89543 8.89543 4 10 4H30L40 14V42C40 43.1046 39.1046 44 38 44H10C8.89543 44 8 43.1046 8 42V6Z").toNodes(),
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
        pathData = PathParser().parsePathString("M16 20H32").toNodes(),
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
        pathData = PathParser().parsePathString("M16 28H32").toNodes(),
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
}.build()
