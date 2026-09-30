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
 * IconPark `share-one` (48x48) as a Compose [ImageVector].
 *
 * The geometry is shared by every IconPark theme; [palette] selects the theme by supplying the
 * four colour slots IconPark remaps at runtime. A fresh vector is built on every call, so callers
 * should cache it for the composition (for example with `remember`).
 */
public fun IconPark.shareOne(palette: IconParkPalette): ImageVector = ImageVector.Builder(
    name = "IconPark.share-one",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    addPath(
        pathData = PathParser().parsePathString("M35 16C37.7614 16 40 13.7614 40 11C40 8.23858 37.7614 6 35 6C32.2386 6 30 8.23858 30 11C30 13.7614 32.2386 16 35 16Z").toNodes(),
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
        pathData = PathParser().parsePathString("M13 29C15.7614 29 18 26.7614 18 24C18 21.2386 15.7614 19 13 19C10.2386 19 8 21.2386 8 24C8 26.7614 10.2386 29 13 29Z").toNodes(),
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
        pathData = PathParser().parsePathString("M30.0004 13.5745L17.3393 21.2454").toNodes(),
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
        pathData = PathParser().parsePathString("M17.3385 26.5639L30.6789 34.4469").toNodes(),
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
        pathData = PathParser().parsePathString("M35 32C37.7614 32 40 34.2386 40 37C40 39.7614 37.7614 42 35 42C32.2386 42 30 39.7614 30 37C30 34.2386 32.2386 32 35 32Z").toNodes(),
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
}.build()
