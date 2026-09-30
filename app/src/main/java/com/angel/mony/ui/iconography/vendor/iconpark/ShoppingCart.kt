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
 * IconPark `shopping-cart` (48x48) as a Compose [ImageVector].
 *
 * The geometry is shared by every IconPark theme; [palette] selects the theme by supplying the
 * four colour slots IconPark remaps at runtime. A fresh vector is built on every call, so callers
 * should cache it for the composition (for example with `remember`).
 */
public fun IconPark.shoppingCart(palette: IconParkPalette): ImageVector = ImageVector.Builder(
    name = "IconPark.shopping-cart",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    addPath(
        pathData = PathParser().parsePathString("M39 32H13L8 12H44L39 32Z").toNodes(),
        fill = palette.outerFill.brush(),
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
        pathData = PathParser().parsePathString("M3 6H6.5L8 12M8 12L13 32H39L44 12H8Z").toNodes(),
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
        pathData = PathParser().parsePathString("M10,39A3,3 0 1 0 16,39A3,3 0 1 0 10,39Z").toNodes(),
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
        pathData = PathParser().parsePathString("M36,39A3,3 0 1 0 42,39A3,3 0 1 0 36,39Z").toNodes(),
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
        pathData = PathParser().parsePathString("M22 22H30").toNodes(),
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
        pathData = PathParser().parsePathString("M26 26V18").toNodes(),
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
