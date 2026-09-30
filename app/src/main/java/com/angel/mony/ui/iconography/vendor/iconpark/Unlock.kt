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
 * IconPark `unlock` (48x48) as a Compose [ImageVector].
 *
 * The geometry is shared by every IconPark theme; [palette] selects the theme by supplying the
 * four colour slots IconPark remaps at runtime. A fresh vector is built on every call, so callers
 * should cache it for the composition (for example with `remember`).
 */
public fun IconPark.unlock(palette: IconParkPalette): ImageVector = ImageVector.Builder(
    name = "IconPark.unlock",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    addPath(
        pathData = PathParser().parsePathString("M9,22.048H39A2,2 0 0 1 41,24.048V42.048A2,2 0 0 1 39,44.048H9A2,2 0 0 1 7,42.048V24.048A2,2 0 0 1 9,22.048Z").toNodes(),
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
        pathData = PathParser().parsePathString("M14 22V14.0047C13.9948 8.87022 17.9227 4.56718 23.0859 4.05117C28.249 3.53516 32.9673 6.97408 34 12.0059").toNodes(),
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
        pathData = PathParser().parsePathString("M24 30V36").toNodes(),
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
