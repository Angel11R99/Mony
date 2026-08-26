package com.example.personalfinancetracker.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    background(
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFE0E0E0).copy(alpha = 0.5f),
                Color(0xFFF5F5F5).copy(alpha = 0.8f),
                Color(0xFFE0E0E0).copy(alpha = 0.5f),
            ),
            start = Offset(translateX, 0f),
            end = Offset(translateX + 400f, 0f),
        ),
    )
}

@Composable
fun ShimmerScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var showShimmer by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        showShimmer = false
    }
    Box(modifier.fillMaxSize()) {
        if (showShimmer) {
            Box(
                Modifier
                    .fillMaxSize()
                    .shimmerEffect(),
            )
        }
        content()
    }
}
