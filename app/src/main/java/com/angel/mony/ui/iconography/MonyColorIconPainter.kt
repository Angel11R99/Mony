package com.angel.mony.ui.iconography

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.min

internal object MonyColorPalette {
    val Coral = Color(0xFFFF7F86)
    val Pink = Color(0xFFFFA4C2)
    val Yellow = Color(0xFFFFD166)
    val Mint = Color(0xFF7DDDB5)
    val Blue = Color(0xFF72B7FF)
    val Cyan = Color(0xFF67D7E5)
    val Violet = Color(0xFFB49AF7)
    val Orange = Color(0xFFFFA45B)
    val Cream = Color(0xFFFFF0CD)
}

/** Scalable, original vector illustrations used exclusively by the Mony Color pack. */
internal class MonyColorIconPainter(
    private val icon: MonyIcon,
    private val outline: Color,
) : Painter() {
    override val intrinsicSize: Size = Size(24f, 24f)

    override fun DrawScope.onDraw() {
        val scale = min(size.width, size.height) / 24f
        val left = (size.width - 24f * scale) / 2f
        val top = (size.height - 24f * scale) / 2f
        withTransform({
            translate(left, top)
            scale(scale, scale, Offset.Zero)
        }) {
            drawMonyColorGlyph(icon, outline)
        }
    }

    companion object {
        private val supported = setOf(
            MonyIcon.Home, MonyIcon.Fixed, MonyIcon.Pending, MonyIcon.Savings,
            MonyIcon.Shopping, MonyIcon.Statistics, MonyIcon.Fortnight, MonyIcon.History,
            MonyIcon.Settings, MonyIcon.Appearance, MonyIcon.Navigation, MonyIcon.Finance,
            MonyIcon.Add, MonyIcon.Edit, MonyIcon.Delete, MonyIcon.Back, MonyIcon.Search,
            MonyIcon.More, MonyIcon.Check, MonyIcon.Close, MonyIcon.Warning, MonyIcon.Info,
            MonyIcon.Category, MonyIcon.Completed, MonyIcon.Expense, MonyIcon.Income,
            MonyIcon.Food, MonyIcon.Debt, MonyIcon.Education, MonyIcon.Emergency,
            MonyIcon.Entertainment, MonyIcon.Family, MonyIcon.Internet, MonyIcon.Health,
            MonyIcon.Services, MonyIcon.Subscription, MonyIcon.Phone, MonyIcon.Transport,
            MonyIcon.Housing, MonyIcon.Other, MonyIcon.Calendar,
        )

        fun supports(icon: MonyIcon): Boolean = icon in supported

        fun colorCount(icon: MonyIcon): Int = if (supports(icon)) 3 else 0

        fun semanticAccent(icon: MonyIcon): Color? = when (icon) {
            MonyIcon.Delete, MonyIcon.Close -> MonyColorPalette.Coral
            MonyIcon.Warning -> MonyColorPalette.Yellow
            MonyIcon.Check, MonyIcon.Completed -> MonyColorPalette.Mint
            else -> null
        }
    }
}

private val FineStroke = Stroke(width = 1.45f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val BoldStroke = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawMonyColorGlyph(icon: MonyIcon, outline: Color) {
    when (icon) {
        MonyIcon.Home, MonyIcon.Finance -> drawWallet(outline)
        MonyIcon.Savings -> drawPiggyBank(outline)
        MonyIcon.Shopping -> drawShopping(outline)
        MonyIcon.Statistics -> drawStatistics(outline)
        MonyIcon.Fortnight, MonyIcon.Calendar -> drawCalendar(outline)
        MonyIcon.Settings -> drawSettings(outline)
        MonyIcon.Expense -> drawTransaction(outline, income = false)
        MonyIcon.Income -> drawTransaction(outline, income = true)
        MonyIcon.Food -> drawFood(outline)
        MonyIcon.Debt -> drawDebt(outline)
        MonyIcon.Education -> drawEducation(outline)
        MonyIcon.Emergency -> drawEmergency(outline)
        MonyIcon.Entertainment -> drawEntertainment(outline)
        MonyIcon.Family -> drawFamily(outline)
        MonyIcon.Internet -> drawInternet(outline)
        MonyIcon.Health -> drawHealth(outline)
        MonyIcon.Services -> drawServices(outline)
        MonyIcon.Subscription, MonyIcon.Fixed -> drawSubscription(outline)
        MonyIcon.Phone -> drawPhone(outline)
        MonyIcon.Transport -> drawTransport(outline)
        MonyIcon.Housing -> drawHousing(outline)
        MonyIcon.Pending -> drawPending(outline)
        MonyIcon.History -> drawHistory(outline)
        MonyIcon.Appearance -> drawAppearance(outline)
        MonyIcon.Navigation -> drawNavigation(outline)
        MonyIcon.Category -> drawCategory(outline)
        MonyIcon.Add, MonyIcon.Edit, MonyIcon.Delete, MonyIcon.Back, MonyIcon.Search,
        MonyIcon.More, MonyIcon.Check, MonyIcon.Close, MonyIcon.Warning, MonyIcon.Info,
        MonyIcon.Completed -> drawAction(icon, outline)
        MonyIcon.Other -> drawOther(outline)
        else -> Unit
    }
}

private fun DrawScope.drawWallet(outline: Color) {
    bubble(MonyColorPalette.Yellow)
    roundBox(MonyColorPalette.Violet, outline, 3f, 6f, 18f, 14f, 3f)
    roundBox(MonyColorPalette.Mint, outline, 12f, 10f, 9f, 6f, 2f)
    drawCircle(MonyColorPalette.Coral, 1.25f, Offset(15.5f, 13f))
}

private fun DrawScope.drawPiggyBank(outline: Color) {
    drawCircle(MonyColorPalette.Yellow, 2.7f, Offset(15.8f, 4.1f), style = Fill)
    drawCircle(outline, 2.7f, Offset(15.8f, 4.1f), style = FineStroke)
    roundBox(MonyColorPalette.Pink, outline, 3f, 7f, 18f, 11f, 5f)
    triangle(MonyColorPalette.Pink, outline, Offset(6f, 8f), Offset(5f, 4.8f), Offset(9f, 7.2f))
    drawLine(outline, Offset(9f, 10f), Offset(14f, 10f), 1.5f, StrokeCap.Round)
    drawCircle(outline, 0.8f, Offset(17.2f, 11.5f))
    drawLine(outline, Offset(7f, 17f), Offset(7f, 20f), 1.7f, StrokeCap.Round)
    drawLine(outline, Offset(17f, 17f), Offset(17f, 20f), 1.7f, StrokeCap.Round)
}

private fun DrawScope.drawShopping(outline: Color) {
    bubble(MonyColorPalette.Cyan)
    roundBox(MonyColorPalette.Coral, outline, 5f, 7f, 14f, 13f, 2.5f)
    drawRoundRect(MonyColorPalette.Yellow, Offset(8f, 10f), Size(8f, 7f), CornerRadius(1.5f))
    drawArc(outline, 200f, 140f, false, Offset(8f, 3.5f), Size(8f, 9f), style = FineStroke)
}

private fun DrawScope.drawStatistics(outline: Color) {
    bubble(MonyColorPalette.Pink)
    roundBox(MonyColorPalette.Mint, outline, 3.5f, 12f, 4f, 8f, 1.4f)
    roundBox(MonyColorPalette.Blue, outline, 9.8f, 8f, 4f, 12f, 1.4f)
    roundBox(MonyColorPalette.Yellow, outline, 16.1f, 4f, 4f, 16f, 1.4f)
}

private fun DrawScope.drawCalendar(outline: Color) {
    roundBox(MonyColorPalette.Cream, outline, 3f, 5f, 18f, 16f, 3f)
    drawRoundRect(MonyColorPalette.Coral, Offset(3f, 5f), Size(18f, 5f), CornerRadius(3f, 3f))
    drawLine(outline, Offset(3f, 10f), Offset(21f, 10f), FineStroke.width)
    drawLine(outline, Offset(7f, 3f), Offset(7f, 7f), BoldStroke.width, StrokeCap.Round)
    drawLine(outline, Offset(17f, 3f), Offset(17f, 7f), BoldStroke.width, StrokeCap.Round)
    drawCircle(MonyColorPalette.Blue, 1.7f, Offset(8f, 14f))
    drawCircle(MonyColorPalette.Mint, 1.7f, Offset(15.5f, 16.5f))
}

private fun DrawScope.drawSettings(outline: Color) {
    bubble(MonyColorPalette.Pink)
    drawCircle(MonyColorPalette.Blue, 7f, Offset(12f, 12f))
    drawCircle(outline, 7f, Offset(12f, 12f), style = BoldStroke)
    repeat(8) { index ->
        val points = listOf(
            Offset(12f, 2.5f), Offset(18.7f, 5.3f), Offset(21.5f, 12f), Offset(18.7f, 18.7f),
            Offset(12f, 21.5f), Offset(5.3f, 18.7f), Offset(2.5f, 12f), Offset(5.3f, 5.3f),
        )
        drawCircle(if (index % 2 == 0) MonyColorPalette.Yellow else MonyColorPalette.Violet, 1.7f, points[index])
        drawCircle(outline, 1.7f, points[index], style = FineStroke)
    }
    drawCircle(MonyColorPalette.Cream, 2.5f, Offset(12f, 12f))
    drawCircle(outline, 2.5f, Offset(12f, 12f), style = FineStroke)
}

private fun DrawScope.drawTransaction(outline: Color, income: Boolean) {
    roundBox(MonyColorPalette.Cream, outline, 5f, 3f, 13f, 18f, 2.5f)
    drawLine(MonyColorPalette.Blue, Offset(8f, 7f), Offset(15f, 7f), 2f, StrokeCap.Round)
    drawLine(MonyColorPalette.Violet, Offset(8f, 11f), Offset(13f, 11f), 2f, StrokeCap.Round)
    val accent = if (income) MonyColorPalette.Mint else MonyColorPalette.Coral
    drawCircle(accent, 4f, Offset(17f, 17f))
    drawCircle(outline, 4f, Offset(17f, 17f), style = FineStroke)
    val direction = if (income) -1f else 1f
    drawLine(outline, Offset(17f, 15f - direction), Offset(17f, 19f + direction), 1.4f, StrokeCap.Round)
    drawLine(outline, Offset(17f, 19f + direction), Offset(15.4f, 17.5f + direction), 1.4f, StrokeCap.Round)
    drawLine(outline, Offset(17f, 19f + direction), Offset(18.6f, 17.5f + direction), 1.4f, StrokeCap.Round)
}

private fun DrawScope.drawFood(outline: Color) {
    drawCircle(MonyColorPalette.Cream, 7.5f, Offset(12f, 12f))
    drawCircle(outline, 7.5f, Offset(12f, 12f), style = FineStroke)
    drawCircle(MonyColorPalette.Mint, 3.5f, Offset(12f, 12f))
    drawCircle(MonyColorPalette.Coral, 1.4f, Offset(11f, 11f))
    drawCircle(MonyColorPalette.Yellow, 1.2f, Offset(14f, 13.5f))
    drawLine(outline, Offset(3f, 4f), Offset(3f, 20f), 1.5f, StrokeCap.Round)
    drawLine(outline, Offset(21f, 4f), Offset(21f, 20f), 1.5f, StrokeCap.Round)
}

private fun DrawScope.drawDebt(outline: Color) {
    bubble(MonyColorPalette.Coral)
    roundBox(MonyColorPalette.Blue, outline, 3f, 6f, 18f, 13f, 2.5f)
    drawRect(MonyColorPalette.Yellow, Offset(3f, 9f), Size(18f, 3.5f))
    drawLine(outline, Offset(6f, 16f), Offset(11f, 16f), 1.5f, StrokeCap.Round)
}

private fun DrawScope.drawEducation(outline: Color) {
    val left = Path().apply { moveTo(3f, 6f); quadraticTo(8f, 4f, 12f, 8f); lineTo(12f, 20f); quadraticTo(8f, 16f, 3f, 18f); close() }
    val right = Path().apply { moveTo(21f, 6f); quadraticTo(16f, 4f, 12f, 8f); lineTo(12f, 20f); quadraticTo(16f, 16f, 21f, 18f); close() }
    drawPath(left, MonyColorPalette.Blue)
    drawPath(right, MonyColorPalette.Yellow)
    drawPath(left, outline, style = FineStroke)
    drawPath(right, outline, style = FineStroke)
    drawCircle(MonyColorPalette.Pink, 2f, Offset(18.5f, 4f))
}

private fun DrawScope.drawEmergency(outline: Color) {
    bubble(MonyColorPalette.Yellow)
    roundBox(MonyColorPalette.Coral, outline, 3f, 6f, 18f, 14f, 3f)
    roundBox(MonyColorPalette.Cream, outline, 8.8f, 8f, 6.4f, 10f, 1.2f)
    drawRect(MonyColorPalette.Cream, Offset(7f, 9.8f), Size(10f, 6.4f))
    drawRect(outline, Offset(10.9f, 10f), Size(2.2f, 6f))
    drawRect(outline, Offset(9f, 11.9f), Size(6f, 2.2f))
}

private fun DrawScope.drawEntertainment(outline: Color) {
    bubble(MonyColorPalette.Violet)
    roundBox(MonyColorPalette.Blue, outline, 3f, 8f, 18f, 11f, 5f)
    drawLine(outline, Offset(7f, 13.5f), Offset(11f, 13.5f), 1.5f, StrokeCap.Round)
    drawLine(outline, Offset(9f, 11.5f), Offset(9f, 15.5f), 1.5f, StrokeCap.Round)
    drawCircle(MonyColorPalette.Yellow, 1.4f, Offset(16f, 12f))
    drawCircle(MonyColorPalette.Coral, 1.4f, Offset(18f, 15f))
}

private fun DrawScope.drawFamily(outline: Color) {
    drawCircle(MonyColorPalette.Yellow, 3f, Offset(8f, 7f))
    drawCircle(outline, 3f, Offset(8f, 7f), style = FineStroke)
    drawCircle(MonyColorPalette.Pink, 3f, Offset(16f, 7f))
    drawCircle(outline, 3f, Offset(16f, 7f), style = FineStroke)
    roundBox(MonyColorPalette.Blue, outline, 3f, 12f, 9f, 8f, 4f)
    roundBox(MonyColorPalette.Mint, outline, 12f, 12f, 9f, 8f, 4f)
}

private fun DrawScope.drawInternet(outline: Color) {
    drawCircle(MonyColorPalette.Cyan, 9f, Offset(12f, 12f))
    drawCircle(outline, 9f, Offset(12f, 12f), style = FineStroke)
    drawArc(outline, 210f, 120f, false, Offset(5f, 7f), Size(14f, 12f), style = BoldStroke)
    drawArc(outline, 210f, 120f, false, Offset(8f, 10f), Size(8f, 7f), style = BoldStroke)
    drawCircle(MonyColorPalette.Yellow, 1.8f, Offset(12f, 16.8f))
}

private fun DrawScope.drawHealth(outline: Color) {
    val heart = Path().apply {
        moveTo(12f, 20f); cubicTo(3f, 15f, 3f, 8f, 7.5f, 6f); cubicTo(10f, 5f, 12f, 7.5f, 12f, 7.5f)
        cubicTo(12f, 7.5f, 14f, 5f, 16.5f, 6f); cubicTo(21f, 8f, 21f, 15f, 12f, 20f); close()
    }
    drawPath(heart, MonyColorPalette.Coral)
    drawPath(heart, outline, style = FineStroke)
    drawRoundRect(MonyColorPalette.Mint, Offset(10.7f, 9f), Size(2.6f, 7f), CornerRadius(0.8f))
    drawRoundRect(MonyColorPalette.Mint, Offset(8.5f, 11.2f), Size(7f, 2.6f), CornerRadius(0.8f))
}

private fun DrawScope.drawServices(outline: Color) {
    bubble(MonyColorPalette.Blue)
    drawCircle(MonyColorPalette.Yellow, 6.5f, Offset(10f, 14f))
    drawCircle(outline, 6.5f, Offset(10f, 14f), style = FineStroke)
    drawLine(outline, Offset(14f, 10f), Offset(20f, 4f), 3.2f, StrokeCap.Round)
    drawCircle(MonyColorPalette.Coral, 2.2f, Offset(19f, 5f))
}

private fun DrawScope.drawSubscription(outline: Color) {
    drawCircle(MonyColorPalette.Violet, 8f, Offset(12f, 12f))
    drawArc(outline, 35f, 245f, false, Offset(5f, 5f), Size(14f, 14f), style = BoldStroke)
    triangle(MonyColorPalette.Yellow, outline, Offset(18f, 4f), Offset(21f, 8f), Offset(16f, 8f))
    drawCircle(MonyColorPalette.Mint, 2f, Offset(12f, 12f))
}

private fun DrawScope.drawPhone(outline: Color) {
    bubble(MonyColorPalette.Yellow)
    roundBox(MonyColorPalette.Cyan, outline, 6f, 2.5f, 12f, 19f, 3f)
    roundBox(MonyColorPalette.Cream, outline, 8f, 5.5f, 8f, 11f, 1.5f)
    drawCircle(MonyColorPalette.Pink, 1.2f, Offset(12f, 19f))
}

private fun DrawScope.drawTransport(outline: Color) {
    bubble(MonyColorPalette.Mint)
    val body = Path().apply { moveTo(3f, 12f); lineTo(6f, 7f); lineTo(17f, 7f); lineTo(21f, 12f); lineTo(21f, 17f); lineTo(3f, 17f); close() }
    drawPath(body, MonyColorPalette.Blue)
    drawPath(body, outline, style = FineStroke)
    drawRect(MonyColorPalette.Cream, Offset(7f, 8.5f), Size(9f, 3.5f))
    drawCircle(MonyColorPalette.Yellow, 2.2f, Offset(7f, 18f))
    drawCircle(MonyColorPalette.Coral, 2.2f, Offset(17f, 18f))
    drawCircle(outline, 2.2f, Offset(7f, 18f), style = FineStroke)
    drawCircle(outline, 2.2f, Offset(17f, 18f), style = FineStroke)
}

private fun DrawScope.drawHousing(outline: Color) {
    val roof = Path().apply { moveTo(2.5f, 11f); lineTo(12f, 3f); lineTo(21.5f, 11f); lineTo(19f, 13f); lineTo(12f, 7f); lineTo(5f, 13f); close() }
    drawPath(roof, MonyColorPalette.Coral)
    drawPath(roof, outline, style = FineStroke)
    roundBox(MonyColorPalette.Yellow, outline, 5f, 11f, 14f, 10f, 1.5f)
    drawRoundRect(MonyColorPalette.Blue, Offset(9.5f, 14f), Size(5f, 7f), CornerRadius(1f))
    drawCircle(MonyColorPalette.Mint, 1.2f, Offset(13f, 17.5f))
}

private fun DrawScope.drawPending(outline: Color) {
    bubble(MonyColorPalette.Coral)
    drawCircle(MonyColorPalette.Yellow, 7f, Offset(12f, 12f))
    drawCircle(outline, 7f, Offset(12f, 12f), style = FineStroke)
    drawLine(outline, Offset(12f, 8f), Offset(12f, 13f), 1.6f, StrokeCap.Round)
    drawCircle(MonyColorPalette.Blue, 1.2f, Offset(12f, 16f))
}

private fun DrawScope.drawHistory(outline: Color) {
    drawCircle(MonyColorPalette.Cream, 8f, Offset(12f, 12f))
    drawCircle(outline, 8f, Offset(12f, 12f), style = FineStroke)
    drawArc(MonyColorPalette.Violet, 140f, 250f, false, Offset(3f, 3f), Size(18f, 18f), style = Stroke(3f, cap = StrokeCap.Round))
    drawLine(outline, Offset(12f, 7f), Offset(12f, 12f), 1.5f, StrokeCap.Round)
    drawLine(outline, Offset(12f, 12f), Offset(16f, 14f), 1.5f, StrokeCap.Round)
    triangle(MonyColorPalette.Coral, outline, Offset(3f, 7f), Offset(7f, 6f), Offset(5f, 10f))
}

private fun DrawScope.drawAppearance(outline: Color) {
    drawCircle(MonyColorPalette.Cream, 8.5f, Offset(12f, 12f))
    drawCircle(outline, 8.5f, Offset(12f, 12f), style = FineStroke)
    drawCircle(MonyColorPalette.Coral, 2f, Offset(9f, 8f))
    drawCircle(MonyColorPalette.Yellow, 2f, Offset(15f, 8f))
    drawCircle(MonyColorPalette.Blue, 2f, Offset(8f, 14f))
    drawCircle(MonyColorPalette.Mint, 2f, Offset(14f, 16f))
}

private fun DrawScope.drawNavigation(outline: Color) {
    bubble(MonyColorPalette.Pink)
    listOf(7f, 12f, 17f).forEachIndexed { index, y ->
        drawLine(outline, Offset(4f, y), Offset(20f, y), 1.6f, StrokeCap.Round)
        val x = listOf(9f, 16f, 11f)[index]
        drawCircle(listOf(MonyColorPalette.Blue, MonyColorPalette.Yellow, MonyColorPalette.Mint)[index], 2.3f, Offset(x, y))
        drawCircle(outline, 2.3f, Offset(x, y), style = FineStroke)
    }
}

private fun DrawScope.drawCategory(outline: Color) {
    val tag = Path().apply { moveTo(3f, 5f); lineTo(13f, 5f); lineTo(21f, 13f); lineTo(13f, 21f); lineTo(3f, 11f); close() }
    drawPath(tag, MonyColorPalette.Violet)
    drawPath(tag, outline, style = FineStroke)
    drawCircle(MonyColorPalette.Yellow, 2f, Offset(8f, 10f))
    drawCircle(MonyColorPalette.Mint, 1.5f, Offset(14.5f, 14f))
}

private fun DrawScope.drawAction(icon: MonyIcon, outline: Color) {
    val fill = MonyColorIconPainter.semanticAccent(icon) ?: when (icon) {
        MonyIcon.Info, MonyIcon.Search, MonyIcon.Back -> MonyColorPalette.Blue
        else -> MonyColorPalette.Violet
    }
    drawCircle(fill, 8f, Offset(12f, 12f))
    drawCircle(outline, 8f, Offset(12f, 12f), style = FineStroke)
    drawCircle(MonyColorPalette.Cream, 2f, Offset(18f, 6f))
    drawCircle(outline, 2f, Offset(18f, 6f), style = FineStroke)
    when (icon) {
        MonyIcon.Add -> {
            drawLine(outline, Offset(8f, 12f), Offset(16f, 12f), 1.8f, StrokeCap.Round)
            drawLine(outline, Offset(12f, 8f), Offset(12f, 16f), 1.8f, StrokeCap.Round)
        }
        MonyIcon.Edit -> {
            drawLine(outline, Offset(8f, 16f), Offset(16f, 8f), 2.4f, StrokeCap.Round)
            triangle(MonyColorPalette.Yellow, outline, Offset(7f, 17f), Offset(8f, 13.5f), Offset(10.5f, 16f))
        }
        MonyIcon.Delete -> {
            roundBox(MonyColorPalette.Cream, outline, 8f, 9f, 8f, 8f, 1.5f)
            drawLine(outline, Offset(7f, 8f), Offset(17f, 8f), 1.6f, StrokeCap.Round)
        }
        MonyIcon.Back -> {
            drawLine(outline, Offset(16f, 12f), Offset(8f, 12f), 1.8f, StrokeCap.Round)
            drawLine(outline, Offset(8f, 12f), Offset(11f, 9f), 1.8f, StrokeCap.Round)
            drawLine(outline, Offset(8f, 12f), Offset(11f, 15f), 1.8f, StrokeCap.Round)
        }
        MonyIcon.Search -> {
            drawCircle(MonyColorPalette.Cream, 3.5f, Offset(10.5f, 10.5f))
            drawCircle(outline, 3.5f, Offset(10.5f, 10.5f), style = FineStroke)
            drawLine(outline, Offset(13f, 13f), Offset(17f, 17f), 1.8f, StrokeCap.Round)
        }
        MonyIcon.More -> listOf(8f, 12f, 16f).forEach { drawCircle(outline, 1.2f, Offset(it, 12f)) }
        MonyIcon.Check, MonyIcon.Completed -> {
            drawLine(outline, Offset(8f, 12f), Offset(11f, 15f), 1.8f, StrokeCap.Round)
            drawLine(outline, Offset(11f, 15f), Offset(16.5f, 9f), 1.8f, StrokeCap.Round)
        }
        MonyIcon.Close -> {
            drawLine(outline, Offset(9f, 9f), Offset(15f, 15f), 1.8f, StrokeCap.Round)
            drawLine(outline, Offset(15f, 9f), Offset(9f, 15f), 1.8f, StrokeCap.Round)
        }
        MonyIcon.Warning -> {
            drawLine(outline, Offset(12f, 8f), Offset(12f, 13f), 1.8f, StrokeCap.Round)
            drawCircle(MonyColorPalette.Coral, 1.2f, Offset(12f, 16f))
        }
        MonyIcon.Info -> {
            drawCircle(MonyColorPalette.Yellow, 1.2f, Offset(12f, 8f))
            drawLine(outline, Offset(12f, 11f), Offset(12f, 16f), 1.8f, StrokeCap.Round)
        }
        else -> Unit
    }
}

private fun DrawScope.drawOther(outline: Color) {
    drawCircle(MonyColorPalette.Cream, 8f, Offset(12f, 12f))
    drawCircle(outline, 8f, Offset(12f, 12f), style = FineStroke)
    listOf(MonyColorPalette.Coral, MonyColorPalette.Blue, MonyColorPalette.Mint).forEachIndexed { index, color ->
        drawCircle(color, 1.8f, Offset(8f + index * 4f, 12f))
        drawCircle(outline, 1.8f, Offset(8f + index * 4f, 12f), style = FineStroke)
    }
}

private fun DrawScope.bubble(color: Color) {
    drawCircle(color, 2.4f, Offset(19f, 5f))
}

private fun DrawScope.roundBox(
    fill: Color,
    outline: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    val topLeft = Offset(left, top)
    val size = Size(width, height)
    val corner = CornerRadius(radius)
    drawRoundRect(fill, topLeft, size, corner)
    drawRoundRect(outline, topLeft, size, corner, style = FineStroke)
}

private fun DrawScope.triangle(fill: Color, outline: Color, a: Offset, b: Offset, c: Offset) {
    val path = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }
    drawPath(path, fill)
    drawPath(path, outline, style = FineStroke)
}
