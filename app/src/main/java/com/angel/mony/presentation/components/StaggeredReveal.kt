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
 * A composable that reveals its content items with a subtle staggered animation.
 * Each child added via [add] will fade in and slide up with a small delay between items.
 *
 * Usage:
 * ```kotlin
 * StaggeredReveal {
 *     add { Header() }
 *     add { SummaryCard() }
 *     add { ContentList() }
 *     add { Actions() }
 * }
 * ```
 *
 * The animation respects the system's "reduce motion" setting.
 */
@Composable
fun StaggeredReveal(
    modifier: Modifier = Modifier,
    content: StaggeredRevealScope.() -> Unit,
) {
    val scope = remember { StaggeredRevealScope() }
    content(scope)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        scope.items.forEachIndexed { index, item ->
            StaggeredRevealItem(index = index) {
                item()
            }
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
 * A single item in a staggered reveal sequence. Handles the fade-in and slide-up animation.
 * The animation only runs once when the item first appears, and respects reduced motion settings.
 */
@Composable
fun StaggeredRevealItem(
    index: Int,
    modifier: Modifier = Modifier,
    delayMillis: Int = 40,
    durationMillis: Int = 180,
    content: @Composable () -> Unit,
) {
    val (visible, setVisible) = remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val slideOffset = with(LocalDensity.current) { 12.dp.roundToPx() }
    val slideOffsetProvider = { height: Int -> slideOffset }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            val delay = index * delayMillis
            if (delay > 0) {
                delay(delay.toLong())
            }
        }
        setVisible(true)
    }

    if (visible || reducedMotion) {
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
            modifier = modifier,
        ) {
            content()
        }
    } else {
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