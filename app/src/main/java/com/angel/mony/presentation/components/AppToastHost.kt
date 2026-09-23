package com.angel.mony.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.angel.mony.core.AppToastMessage
import com.angel.mony.core.AppToastNotifications
import com.angel.mony.core.AppToastTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val MAX_VISIBLE_TOASTS = 3
private const val DECK_SCALE = 0.93f
private const val DECK_ALPHA = 0.80f
private const val DECK_VISIBLE_FRACTION = 0.10f
private const val ENTER_DELAY_MILLIS = 90L
private const val EXIT_MILLIS = 200L
private val STACK_PEEK_OFFSET = 5.dp

private data class ToastNotice(
    val id: Long,
    val message: AppToastMessage,
    val durationMillis: Long,
)

private fun stackOffsetYFor(
    index: Int,
    toasts: List<ToastNotice>,
    heights: Map<Long, Int>,
    spacingPx: Float,
): Float {
    if (index == 0) return 0f
    var offset = 0f
    for (j in 1..index) {
        val height = heights[toasts[j].id] ?: return 0f
        offset -= (height * (1f - DECK_VISIBLE_FRACTION) + spacingPx)
    }
    return offset
}

@Composable
fun AppToastHost(modifier: Modifier = Modifier) {
    val accessibilityManager = LocalAccessibilityManager.current
    val density = LocalDensity.current
    val spacingPx = with(density) { 4.dp.toPx() }
    var toasts by remember { mutableStateOf(emptyList<ToastNotice>()) }
    var nextId by remember { mutableStateOf(0L) }
    val heightsById = remember { mutableStateMapOf<Long, Int>() }

    fun removeToast(id: Long) {
        toasts = toasts.filterNot { it.id == id }
        heightsById.remove(id)
    }

    LaunchedEffect(accessibilityManager) {
        AppToastNotifications.messages.collect { message ->
            val visibleDuration = accessibilityManager?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = message.durationMillis,
                containsIcons = true,
                containsText = true,
                containsControls = true,
            ) ?: message.durationMillis
            val notice = ToastNotice(
                id = nextId++,
                message = message,
                durationMillis = visibleDuration,
            )
            toasts = (listOf(notice) + toasts).take(MAX_VISIBLE_TOASTS)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 12.dp, end = 14.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(animationSpec = tween(240)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            toasts.forEachIndexed { index, notice ->
                key(notice.id) {
                    StackedToastCard(
                        notice = notice,
                        behind = index > 0,
                        stackOffsetY = stackOffsetYFor(
                            index = index,
                            toasts = toasts,
                            heights = heightsById,
                            spacingPx = spacingPx,
                        ),
                        onDismiss = { removeToast(notice.id) },
                        onSizeChanged = { heightsById[notice.id] = it.height },
                        modifier = Modifier
                            .zIndex((MAX_VISIBLE_TOASTS - index).toFloat()),
                    )
                }
            }
        }
    }
}

@Composable
private fun StackedToastCard(
    notice: ToastNotice,
    behind: Boolean,
    stackOffsetY: Float,
    onDismiss: () -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val peekPx = with(density) { STACK_PEEK_OFFSET.toPx() }

    val toastScale by animateFloatAsState(
        targetValue = if (behind) DECK_SCALE else 1f,
        animationSpec = tween(260),
        label = "toastScale",
    )
    val toastAlpha by animateFloatAsState(
        targetValue = if (behind) DECK_ALPHA else 1f,
        animationSpec = tween(260),
        label = "toastAlpha",
    )
    val toastOffsetY by animateFloatAsState(
        targetValue = if (behind) stackOffsetY else 0f,
        animationSpec = tween(260),
        label = "toastOffsetY",
    )
    val toastOffsetX by animateFloatAsState(
        targetValue = if (behind) peekPx else 0f,
        animationSpec = tween(260),
        label = "toastOffsetX",
    )

    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun dismiss() {
        if (!visible) return
        visible = false
        scope.launch {
            delay(EXIT_MILLIS)
            onDismiss()
        }
    }

    LaunchedEffect(notice) {
        delay(ENTER_DELAY_MILLIS)
        visible = true
        delay(notice.durationMillis)
        visible = false
        delay(EXIT_MILLIS)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.graphicsLayer {
            scaleX = toastScale
            scaleY = toastScale
            this.alpha = toastAlpha
            translationY = toastOffsetY
            translationX = toastOffsetX
        },
        enter = slideInHorizontally(
            animationSpec = tween(260),
            initialOffsetX = { it },
        ) + fadeIn(tween(180)) + scaleIn(
            animationSpec = tween(240),
            initialScale = 0.96f,
            transformOrigin = TransformOrigin(1f, 0f),
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(220),
            targetOffsetX = { it },
        ) + fadeOut(tween(160)) + scaleOut(
            animationSpec = tween(200),
            targetScale = 0.97f,
            transformOrigin = TransformOrigin(1f, 0f),
        ),
    ) {
        Box(
            modifier = Modifier.onSizeChanged(onSizeChanged),
        ) {
            AppToastCard(message = notice.message, onDismiss = { dismiss() })
        }
    }
}

@Composable
private fun AppToastCard(
    message: AppToastMessage,
    onDismiss: () -> Unit,
) {
    val accent = when (message.tone) {
        AppToastTone.SUCCESS -> MaterialTheme.colorScheme.primary
        AppToastTone.ERROR -> MaterialTheme.colorScheme.error
        AppToastTone.INFO -> MaterialTheme.colorScheme.secondary
    }
    val icon = when (message.tone) {
        AppToastTone.SUCCESS -> Icons.Outlined.CheckCircle
        AppToastTone.ERROR -> Icons.Outlined.ErrorOutline
        AppToastTone.INFO -> Icons.Outlined.Info
    }
    val title = when (message.tone) {
        AppToastTone.SUCCESS -> "Listo"
        AppToastTone.ERROR -> "Atención"
        AppToastTone.INFO -> "Aviso"
    }

    Surface(
        modifier = Modifier
            .widthIn(min = 248.dp, max = 340.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(accent),
            )
            Box(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar aviso",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}