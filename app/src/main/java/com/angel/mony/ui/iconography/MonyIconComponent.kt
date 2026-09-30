package com.angel.mony.ui.iconography

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

data class IconographyConfig(
    val pack: IconPack = IconPack.MATERIAL,
    val colorMode: IconColorMode = IconColorMode.AUTOMATIC,
    val customColor: Color = Color.Unspecified,
)

val LocalIconography = compositionLocalOf { IconographyConfig() }

object MonyIconSize {
    val Small = 18.dp
    val Medium = 24.dp
    val Large = 32.dp
}

@Composable
fun MonyIcon(
    icon: com.angel.mony.ui.iconography.MonyIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    role: MonyIconRole = MonyIconRole.NORMAL,
    packOverride: IconPack? = null,
    size: Dp? = null,
) {
    val config = LocalIconography.current
    val pack = packOverride ?: config.pack
    val asset = MonyIconResolver.resolve(icon, pack)
    val iconModifier = if (size == null) modifier else modifier.size(size)
    val resolvedTint = resolveIconTint(
        config = config,
        role = role,
        requestedTint = tint,
        localContentColor = LocalContentColor.current,
        primaryColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.background,
    )
    when (asset) {
        is MonyIconAsset.Tintable -> Icon(
            imageVector = asset.imageVector,
            contentDescription = contentDescription,
            modifier = iconModifier,
            tint = if (asset.usesGlobalTint || tint != Color.Unspecified || role == MonyIconRole.STATE) {
                resolvedTint
            } else {
                LocalContentColor.current
            },
        )
        is MonyIconAsset.Multicolor -> {
            val outlineColor = MaterialTheme.colorScheme.onSurface
            val painter = remember(asset.icon, outlineColor) {
                MonyColorIconPainter(asset.icon, outlineColor)
            }
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = if (size == null) {
                    modifier.defaultMinSize(MonyIconSize.Medium, MonyIconSize.Medium)
                } else {
                    iconModifier
                },
            )
        }
    }
}

fun resolveIconTint(
    config: IconographyConfig,
    role: MonyIconRole,
    requestedTint: Color,
    localContentColor: Color,
    primaryColor: Color,
    backgroundColor: Color,
): Color = when {
    requestedTint != Color.Unspecified -> requestedTint
    role == MonyIconRole.STATE -> localContentColor
    config.colorMode == IconColorMode.PRIMARY -> primaryColor
    config.colorMode == IconColorMode.CUSTOM && hasIconContrast(config.customColor, backgroundColor) ->
        config.customColor
    else -> localContentColor
}

fun hasIconContrast(foreground: Color, background: Color, minimumRatio: Float = 3f): Boolean {
    if (foreground == Color.Unspecified || background == Color.Unspecified) return false
    val lighter = max(foreground.luminance(), background.luminance())
    val darker = min(foreground.luminance(), background.luminance())
    return (lighter + 0.05f) / (darker + 0.05f) >= minimumRatio
}
