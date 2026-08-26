package com.example.personalfinancetracker.presentation.components

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.State
import kotlinx.coroutines.delay

private const val SkeletonShimmerDurationMillis = 1600
private const val SkeletonMinVisibleMillis = 350L
private const val SkeletonContentFadeMillis = 220

private val LocalSkeletonProgress = compositionLocalOf<State<Float>?> { null }

enum class SkeletonTone { Base, Accent }

@Immutable
private data class SkeletonPalette(val base: Color, val accent: Color, val highlight: Color)

@Composable
private fun skeletonPalette(): SkeletonPalette {
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceVariant
    val darkTheme = base.luminance() < 0.5f
    return SkeletonPalette(
        base = base,
        accent = lerp(base, scheme.surface, if (darkTheme) 0.5f else 0.65f),
        highlight = lerp(base, Color.White, if (darkTheme) 0.22f else 0.40f),
    )
}

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val resolver = context.contentResolver
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
            Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
    }
}

/**
 * Wraps module content and provides the shared shimmer progress only while data is loading.
 * The infinite transition is created lazily (and disposed) so it never runs once the content
 * is ready, avoiding a continuous frame loop outside of composition.
 */
@Composable
fun SkeletonHost(
    isLoading: Boolean,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    if (isLoading && !reducedMotion) {
        ShimmerProvider(content = content)
    } else {
        CompositionLocalProvider(LocalSkeletonProgress provides null, content = content)
    }
}

@Composable
private fun ShimmerProvider(content: @Composable () -> Unit) {
    val progress = rememberInfiniteTransition(label = "skeletonShimmer")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = SkeletonShimmerDurationMillis, easing = LinearEasing),
            ),
            label = "skeletonShimmerProgress",
        )
    CompositionLocalProvider(LocalSkeletonProgress provides progress, content = content)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    tone: SkeletonTone = SkeletonTone.Base,
) {
    val palette = skeletonPalette()
    val baseColor = when (tone) {
        SkeletonTone.Base -> palette.base
        SkeletonTone.Accent -> palette.accent
    }
    val progress = LocalSkeletonProgress.current
    Box(
        modifier
            .clip(shape)
            .background(baseColor, shape)
            .then(
                if (progress == null) Modifier
                else Modifier.drawBehind {
                    val width = size.width
                    if (width <= 0f) return@drawBehind
                    val x = (progress.value * 2f - 1f) * width
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(baseColor, palette.highlight, baseColor),
                            start = Offset(x, 0f),
                            end = Offset(x + width, 0f),
                        ),
                    )
                },
            ),
    )
}

@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    tone: SkeletonTone = SkeletonTone.Accent,
) {
    SkeletonBox(modifier.height(height), RoundedCornerShape(50), tone)
}

@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
    tone: SkeletonTone = SkeletonTone.Base,
) {
    SkeletonBox(modifier.size(size), CircleShape, tone)
}

@Composable
fun SkeletonChip(
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
    tone: SkeletonTone = SkeletonTone.Base,
) {
    SkeletonBox(modifier.height(height), MaterialTheme.shapes.small, tone)
}

@Composable
fun SkeletonTextField(modifier: Modifier = Modifier) {
    SkeletonBox(modifier.fillMaxWidth().height(56.dp), MaterialTheme.shapes.small)
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(skeletonPalette().base)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
fun SkeletonTransactionRow(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonCircle(26.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonLine(Modifier.fillMaxWidth(0.55f), height = 15.dp)
            SkeletonLine(Modifier.fillMaxWidth(0.35f), height = 11.dp)
        }
        SkeletonLine(Modifier.width(76.dp), height = 14.dp)
    }
}

@Composable
fun SkeletonEntryCard(
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
) {
    SkeletonCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SkeletonLine(Modifier.fillMaxWidth(0.6f), height = 16.dp)
                SkeletonLine(Modifier.fillMaxWidth(0.35f), height = 11.dp)
            }
            SkeletonLine(Modifier.width(64.dp), height = 15.dp, tone = SkeletonTone.Accent)
            SkeletonBox(
                Modifier.size(width = 40.dp, height = 22.dp),
                RoundedCornerShape(11.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonLine(Modifier.weight(1f), height = 11.dp)
            repeat(4) { SkeletonCircle(20.dp) }
        }
        if (showProgress) {
            SkeletonLine(Modifier.fillMaxWidth(), height = 8.dp)
        }
    }
}

@Composable
fun SkeletonGoalCard(modifier: Modifier = Modifier) {
    SkeletonCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SkeletonLine(Modifier.fillMaxWidth(0.5f), height = 16.dp)
                SkeletonLine(Modifier.fillMaxWidth(0.3f), height = 11.dp)
            }
            SkeletonLine(Modifier.width(72.dp), height = 15.dp)
        }
        SkeletonLine(Modifier.fillMaxWidth(), height = 8.dp, tone = SkeletonTone.Accent)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SkeletonLine(Modifier.width(90.dp), height = 12.dp)
            SkeletonLine(Modifier.width(64.dp), height = 12.dp)
        }
    }
}

/**
 * Shows [skeleton] while [isLoading] is true and crossfades to [content] once data is ready.
 *
 * A minimum visible duration prevents ultra-short skeletons (fast Room loads) from flashing
 * abruptly. The shimmer itself is driven by [SkeletonHost], which only runs the infinite
 * animation while loading.
 */
@Composable
fun LoadingContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    minVisibleMillis: Long = SkeletonMinVisibleMillis,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var showSkeleton by remember { mutableStateOf(isLoading) }
    var loadingStartedAtMs by remember { mutableLongStateOf(Long.MAX_VALUE) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            loadingStartedAtMs = SystemClock.elapsedRealtime()
            showSkeleton = true
        } else {
            val startedAtMs = loadingStartedAtMs
            if (startedAtMs != Long.MAX_VALUE) {
                val remainingMillis = minVisibleMillis - (SystemClock.elapsedRealtime() - startedAtMs)
                if (remainingMillis > 0) delay(remainingMillis)
                loadingStartedAtMs = Long.MAX_VALUE
            }
            showSkeleton = false
        }
    }

    Crossfade(
        targetState = showSkeleton,
        modifier = modifier,
        animationSpec = tween(SkeletonContentFadeMillis),
        label = "loadingContent",
    ) { skeletonVisible ->
        if (skeletonVisible) skeleton() else content()
    }
}
