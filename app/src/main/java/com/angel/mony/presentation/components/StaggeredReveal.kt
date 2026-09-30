package com.angel.mony.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

/**
 * A composable that reveals its content groups with a subtle staggered animation.
 * Each group added via [add] will fade in and slide up with a small delay between groups.
 *
 * Usage:
 * ```kotlin
 * StaggeredReveal("home") {
 *     add { Header() }
 *     add { SummaryCard() }
 *     add { ContentList() }
 *     add { Actions() }
 * }
 * ```
 *
 * The animation respects the system's "reduce motion" setting and only runs on first appearance.
 * The [screenKey] should be unique per screen to prevent re-animation on recomposition.
 */
@Composable
fun StaggeredReveal(
    screenKey: String,
    modifier: Modifier = Modifier,
    staggerMillis: Int = 40,
    durationMillis: Int = 180,
    slideOffsetDp: Dp = 8.dp,
    content: StaggeredRevealScope.() -> Unit,
) {
    val scope = StaggeredRevealScope().apply(content)

    val reducedMotion = rememberReducedMotion()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        scope.items.forEachIndexed { index, item ->
            StaggeredRevealItem(
                screenKey = screenKey,
                index = index,
                reducedMotion = reducedMotion,
                staggerMillis = staggerMillis,
                durationMillis = durationMillis,
                slideOffsetDp = slideOffsetDp,
                content = item,
            )
        }
    }
}

class StaggeredRevealScope {
    private val _items = mutableListOf<@Composable () -> Unit>()
    val items: List<@Composable () -> Unit> get() = _items

    fun add(item: @Composable () -> Unit) {
        _items += item
    }
}

/**
 * A single group in a staggered reveal sequence. Handles the fade-in and slide-up animation.
 * The animation only runs once per screen lifecycle, and respects reduced motion settings.
 */
@Composable
private fun StaggeredRevealItem(
    screenKey: String,
    index: Int,
    reducedMotion: Boolean,
    staggerMillis: Int,
    durationMillis: Int,
    slideOffsetDp: Dp,
    content: @Composable () -> Unit,
) {
    var visible by remember(screenKey, index) { mutableStateOf(reducedMotion) }
    val slideOffset = with(LocalDensity.current) { slideOffsetDp.roundToPx() }
    val itemAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "staggeredRevealAlpha",
    )
    val itemTranslationY by animateFloatAsState(
        targetValue = if (visible) 0f else slideOffset.toFloat(),
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "staggeredRevealTranslation",
    )

    LaunchedEffect(screenKey, index, reducedMotion) {
        if (!reducedMotion) {
            delay((index * staggerMillis).toLong())
        }
        visible = true
    }

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = itemAlpha
            translationY = itemTranslationY
        },
    ) {
        content()
    }
}

/**
 * Checks if the user has reduced motion/animations enabled in system settings.
 */
@Composable
private fun rememberReducedMotion(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        val resolver = context.contentResolver
        val animatorScale = android.provider.Settings.Global.getFloat(
            resolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        val transitionScale = android.provider.Settings.Global.getFloat(
            resolver,
            android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f
        )
        animatorScale == 0f || transitionScale == 0f
    }
}
