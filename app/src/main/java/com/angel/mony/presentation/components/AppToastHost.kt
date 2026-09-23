package com.angel.mony.presentation.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.angel.mony.core.AppToastMessage
import com.angel.mony.core.AppToastNotifications
import com.angel.mony.core.AppToastTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppToastHost(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<AppToastMessage?>(null) }
    val accessibilityManager = LocalAccessibilityManager.current

    LaunchedEffect(accessibilityManager) {
        AppToastNotifications.messages.collectLatest { message ->
            current = message
            val visibleDuration = accessibilityManager?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = message.durationMillis,
                containsIcons = true,
                containsText = true,
                containsControls = true,
            ) ?: message.durationMillis
            delay(visibleDuration)
            current = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 12.dp, end = 14.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        AnimatedVisibility(
            visible = current != null,
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
            current?.let { message ->
                AppToastCard(message = message, onDismiss = { current = null })
            }
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
