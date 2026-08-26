package com.example.personalfinancetracker.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay

private const val MIN_SKELETON_DISPLAY_MS = 250L
private const val CROSSFADE_DURATION_MS = 200

@Composable
fun ModuleLoadingContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val startTime = remember { System.currentTimeMillis() }
    var hasMetMinDisplay by remember { mutableLongStateOf(if (!isLoading) startTime else 0L) }

    LaunchedEffect(isLoading) {
        if (!isLoading && hasMetMinDisplay == 0L) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < MIN_SKELETON_DISPLAY_MS) {
                delay(MIN_SKELETON_DISPLAY_MS - elapsed)
            }
            hasMetMinDisplay = System.currentTimeMillis()
        }
    }

    val showContent = !isLoading && hasMetMinDisplay > 0L
    val skeletonAlpha by animateFloatAsState(
        targetValue = if (showContent) 0f else 1f,
        animationSpec = tween(CROSSFADE_DURATION_MS),
        label = "skeletonAlpha",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(CROSSFADE_DURATION_MS),
        label = "contentAlpha",
    )

    Box(modifier.fillMaxSize()) {
        if (skeletonAlpha > 0f) {
            Box(Modifier.fillMaxSize().alpha(skeletonAlpha)) {
                val shimmerProgress = rememberShimmerProgress()
                CompositionLocalProvider(LocalShimmerProgress provides shimmerProgress) {
                    skeleton()
                }
            }
        }
        if (contentAlpha > 0f) {
            Box(Modifier.fillMaxSize().alpha(contentAlpha)) {
                content()
            }
        }
    }
}
