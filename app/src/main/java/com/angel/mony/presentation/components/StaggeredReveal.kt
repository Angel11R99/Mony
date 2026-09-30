package com.angel.mony.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
    val scope = remember { StaggeredRevealScope() }
    content(scope)

    val hasAnimated = remember(screenKey) { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        scope.items.forEachIndexed { index, item ->
            StaggeredRevealItem(
                index = index,
                hasAnimated = hasAnimated,
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
    private val _items = mutableStateOf(emptyList<@Composable () -> Unit>())
    val items: List<@Composable () -> Unit> get() = _items.value

    fun add(item: @Composable () -> Unit) {
        _items.value = _items.value + item
    }
}

/**
 * A single group in a staggered reveal sequence. Handles the fade-in and slide-up animation.
 * The animation only runs once per screen lifecycle, and respects reduced motion settings.
 */
@Composable
private fun StaggeredRevealItem(
    index: Int,
    hasAnimated: androidx.compose.runtime.MutableState<Boolean>,
    reducedMotion: Boolean,
    staggerMillis: Int,
    durationMillis: Int,
    slideOffsetDp: Dp,
    content: @Composable () -> Unit,
) {
    val (visible, setVisible) = remember { mutableStateOf(false) }
    val slideOffset = with(LocalDensity.current) { slideOffsetDp.roundToPx() }
    val slideOffsetProvider = { height: Int -> slideOffset }

    LaunchedEffect(hasAnimated.value) {
        if (!hasAnimated.value && !reducedMotion) {
            val delay = index * staggerMillis
            if (delay > 0) {
                delay(delay.toLong())
            }
            setVisible(true)
        } else if (hasAnimated.value || reducedMotion) {
            setVisible(true)
        }
    }

    // Mark as animated after the first item starts (or immediately if reduced motion)
    LaunchedEffect(Unit) {
        if (!hasAnimated.value && !reducedMotion) {
            delay((staggerMillis * (index + 1)).toLong())
            hasAnimated.value = true
        } else if (!hasAnimated.value && reducedMotion) {
            hasAnimated.value = true
        }
    }

    if (visible || reducedMotion || hasAnimated.value) {
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
            ) + androidx.compose.animation.slideInVertically(
                initialOffsetY = slideOffsetProvider,
                animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
            ),
        ) {
            content()
        }
    } else {
        // Should not reach here, but render content without animation as fallback
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