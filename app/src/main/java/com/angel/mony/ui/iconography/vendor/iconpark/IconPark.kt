package com.angel.mony.ui.iconography.vendor.iconpark

// GENERATED FILE - do not edit by hand.
// Source: @iconify-json/icon-park@1.2.4 (IconPark by ByteDance, Apache-2.0).
// Regenerate with: node tools/iconpark/generate.mjs --source <extracted>/package/icons.json

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * The four colour slots IconPark remaps to render its themes.
 *
 * IconPark (IconPark by ByteDance, Apache-2.0) authors each glyph once and derives every theme by
 * swapping these slots, so the vendored vectors stay theme-parameterised instead of duplicated per
 * style. [outerFill] and [innerFill] are nullable because the outline theme paints no fill.
 *
 * @see MonyColorStyle for the theme presets the UI offers.
 */
public class IconParkPalette(
    public val outerStroke: Color,
    public val outerFill: Color?,
    public val innerStroke: Color,
    public val innerFill: Color?,
)

/** Namespace for the selected IconPark vectors bundled by Mony for the Mony Color pack. */
public object IconPark

/** Resolves a nullable palette slot to a brush, or `null` when the theme paints no fill. */
internal fun Color?.brush(): Brush? = this?.let { SolidColor(it) }
