package com.example.personalfinancetracker.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )
    background(
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateX, 0f),
            end = Offset(translateX + 450f, 0f),
        ),
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier
            .clip(shape)
            .shimmerEffect(),
    )
}

@Composable
fun ShimmerBar(
    modifier: Modifier = Modifier,
    height: Int = 14,
    widthFraction: Float = 1f,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    ShimmerBox(
        modifier
            .fillMaxWidth(widthFraction)
            .height(height.dp),
        shape,
    )
}

@Composable
fun ShimmerRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
    ) {
        content()
    }
}

@Composable
fun CardSkeleton(
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    showProgress: Boolean = true,
) {
    FinanceCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerRow {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBar(height = 16, widthFraction = 0.6f)
                    ShimmerBar(height = 12, widthFraction = 0.35f)
                }
                if (showActions) {
                    ShimmerBox(Modifier.size(36.dp), CircleShape)
                    Spacer(Modifier.width(6.dp))
                    ShimmerBox(Modifier.size(36.dp), CircleShape)
                }
            }
            ShimmerRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerBar(height = 24, widthFraction = 0.45f)
                    ShimmerBar(height = 12, widthFraction = 0.3f)
                }
                if (showActions) {
                    ShimmerBox(
                        Modifier
                            .width(80.dp)
                            .height(36.dp),
                        MaterialTheme.shapes.small,
                    )
                }
            }
            if (showProgress) {
                ShimmerBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    shape = RoundedCornerShape(4.dp),
                )
            }
        }
    }
}

@Composable
fun TransactionSkeleton(
    modifier: Modifier = Modifier,
) {
    FinanceCard(modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerBox(
                Modifier
                    .size(42.dp),
                CircleShape,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBar(height = 14, widthFraction = 0.55f)
                ShimmerBar(height = 11, widthFraction = 0.3f)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShimmerBar(height = 14, widthFraction = 0.25f)
                ShimmerBar(height = 10, widthFraction = 0.18f)
            }
        }
    }
}

@Composable
fun ModuleListSkeleton(
    modifier: Modifier = Modifier,
    cardCount: Int = 4,
    cardContent: @Composable () -> Unit = { CardSkeleton() },
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cardCount) {
            cardContent()
        }
    }
}
