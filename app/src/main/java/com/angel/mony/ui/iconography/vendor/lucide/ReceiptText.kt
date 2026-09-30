package com.composables.icons.lucide

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Lucide.ReceiptText: ImageVector
    get() {
        if (_ReceiptText != null) {
            return _ReceiptText!!
        }
        _ReceiptText = ImageVector.Builder(
            name = "receipt-text",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(4f, 2f)
                verticalLineToRelative(20f)
                lineToRelative(2f, -1f)
                lineToRelative(2f, 1f)
                lineToRelative(2f, -1f)
                lineToRelative(2f, 1f)
                lineToRelative(2f, -1f)
                lineToRelative(2f, 1f)
                lineToRelative(2f, -1f)
                lineToRelative(2f, 1f)
                verticalLineTo(2f)
                lineToRelative(-2f, 1f)
                lineToRelative(-2f, -1f)
                lineToRelative(-2f, 1f)
                lineToRelative(-2f, -1f)
                lineToRelative(-2f, 1f)
                lineToRelative(-2f, -1f)
                lineToRelative(-2f, 1f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(14f, 8f)
                horizontalLineTo(8f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(16f, 12f)
                horizontalLineTo(8f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(13f, 16f)
                horizontalLineTo(8f)
            }
        }.build()
        return _ReceiptText!!
    }

private var _ReceiptText: ImageVector? = null
