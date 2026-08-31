package com.angel.mony.presentation.background

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.angel.mony.R

@Immutable
private data class CatDecorationItem(
    @param:DrawableRes val resource: Int,
    val xFraction: Float,
    val yFraction: Float,
    val sizeFraction: Float,
    val rotation: Float,
    val depthAlpha: Float,
)

private val catDecorationItems = listOf(
    CatDecorationItem(R.drawable.bg_cats_full_body, 0.18f, 0.10f, 0.18f, -5f, 0.75f),
    CatDecorationItem(R.drawable.bg_cats_face_filled, 0.72f, 0.08f, 0.12f, 8f, 0.95f),
    CatDecorationItem(R.drawable.bg_cats_curled, 0.92f, 0.23f, 0.16f, 12f, 0.80f),
    CatDecorationItem(R.drawable.bg_cats_face_outline_soft, 0.31f, 0.29f, 0.11f, -9f, 0.90f),
    CatDecorationItem(R.drawable.bg_cats_scared, 0.67f, 0.42f, 0.17f, 4f, 0.75f),
    CatDecorationItem(R.drawable.bg_cats_sitting_outline, 0.08f, 0.50f, 0.14f, -6f, 0.85f),
    CatDecorationItem(R.drawable.bg_cats_face_solid, 0.38f, 0.60f, 0.12f, 7f, 0.95f),
    CatDecorationItem(R.drawable.bg_cats_stretching, 0.84f, 0.67f, 0.18f, -8f, 0.80f),
    CatDecorationItem(R.drawable.bg_cats_face_outline, 0.18f, 0.82f, 0.11f, 10f, 0.90f),
    CatDecorationItem(R.drawable.bg_cats_sitting, 0.65f, 0.88f, 0.17f, 5f, 0.80f),
)

@Composable
internal fun CatsBackground(alpha: Float) {
    if (alpha <= 0f) return

    BoxWithConstraints(Modifier.fillMaxSize().alpha(alpha)) {
        val shortSide = minOf(maxWidth, maxHeight)
        val tint = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        catDecorationItems.forEach { item ->
            val itemSize = (shortSide * item.sizeFraction).coerceIn(42.dp, 108.dp)
            Image(
                painter = painterResource(item.resource),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = tint,
                modifier = Modifier
                    .offset(
                        x = maxWidth * item.xFraction - itemSize / 2,
                        y = maxHeight * item.yFraction - itemSize / 2,
                    )
                    .size(itemSize)
                    .rotate(item.rotation)
                    .alpha(item.depthAlpha),
            )
        }
    }
}
