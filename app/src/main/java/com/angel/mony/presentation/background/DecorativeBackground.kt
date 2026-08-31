package com.angel.mony.presentation.background

import androidx.compose.runtime.Composable
import com.angel.mony.ui.theme.BackgroundDecoration

private const val MAX_DECORATION_ALPHA = 0.30f

@Composable
fun DecorativeBackground(
    decoration: BackgroundDecoration,
    intensity: Float,
) {
    when (decoration) {
        BackgroundDecoration.NONE -> Unit
        BackgroundDecoration.MEDICAL -> MedicalBackground(
            alpha = decorationVisualAlpha(intensity),
        )
        BackgroundDecoration.CATS -> CatsBackground(
            alpha = decorationVisualAlpha(intensity),
        )
    }
}

internal fun decorationVisualAlpha(intensity: Float): Float =
    intensity.coerceIn(0f, 1f) * MAX_DECORATION_ALPHA
