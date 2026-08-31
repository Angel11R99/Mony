package com.angel.mony.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Conjunto centralizado de shapes por familia visual.
 * - extraSmall/small/medium/large/extraLarge → MaterialTheme.shapes
 * - buttonShape/chipShape/cardShape/dialogShape/textFieldShape → componentes específicos
 *
 * La familia garantiza coherencia sin forzar la MISMA silueta en todos los componentes
 * (ej: Chevron usa chevron en botón pero cut/rounded en card/dialog para no romper legibilidad).
 */
data class AppShapeSet(
    val extraSmall: CornerBasedShape,
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape,
    val extraLarge: CornerBasedShape,
    val buttonShape: Shape,
    val chipShape: Shape,
    val cardShape: Shape,
    val dialogShape: Shape,
    val textFieldShape: Shape,
) {
    fun toMaterialShapes(): Shapes = Shapes(
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
    )
}

// ---------------------------------------------------------------------------
// GenericShapes centrales (no duplicar Path en 20 archivos)
// ---------------------------------------------------------------------------

private val SquareShape: CornerBasedShape = RoundedCornerShape(0.dp)

private val OppositeCutShape: Shape = GenericShape { size, _ ->
    val cut = size.height * 0.22f // proporcional, ~12-14dp en botón 54dp
    moveTo(cut, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(0f, size.height)
    lineTo(0f, cut)
    close()
}

private val SingleCutShape: Shape = GenericShape { size, _ ->
    val cut = size.height * 0.30f
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(0f, size.height)
    close()
}

private val ChevronShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val inset = (h * 0.28f).coerceAtMost(w * 0.18f)
    moveTo(inset, 0f)
    lineTo(w - inset, 0f)
    lineTo(w, h / 2f)
    lineTo(w - inset, h)
    lineTo(inset, h)
    lineTo(0f, h / 2f)
    close()
}

private val BeveledShape: Shape = GenericShape { size, _ ->
    val b = (size.height * 0.18f).coerceAtMost(size.width * 0.08f)
    moveTo(b, 0f)
    lineTo(size.width - b, 0f)
    lineTo(size.width, b)
    lineTo(size.width, size.height - b)
    lineTo(size.width - b, size.height)
    lineTo(b, size.height)
    lineTo(0f, size.height - b)
    lineTo(0f, b)
    close()
}

private val NotchedShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val notchW = w * 0.10f
    val notchH = h * 0.18f
    val cx = w / 2f
    moveTo(0f, 0f)
    lineTo(cx - notchW / 2f, 0f)
    lineTo(cx - notchW / 2f, notchH)
    lineTo(cx + notchW / 2f, notchH)
    lineTo(cx + notchW / 2f, 0f)
    lineTo(w, 0f)
    lineTo(w, h)
    lineTo(0f, h)
    close()
}

private val TicketShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = (h * 0.14f).coerceAtMost(w * 0.06f)
    // semicírculos laterales pequeños (muesca tipo ticket) a mitad de altura
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h * 0.5f - r)
    // muesca derecha hacia adentro
    arcTo(Rect(w - r, h * 0.5f - r, w + r, h * 0.5f + r), 270f, -180f, false)
    lineTo(w, h)
    lineTo(0f, h)
    lineTo(0f, h * 0.5f + r)
    arcTo(Rect(-r, h * 0.5f - r, r, h * 0.5f + r), 90f, -180f, false)
    close()
}

// ---------------------------------------------------------------------------
// Factory
// ---------------------------------------------------------------------------

fun createAppShapes(style: AppShapeStyle): AppShapeSet = when (style) {
    AppShapeStyle.SQUARE -> AppShapeSet(
        extraSmall = RoundedCornerShape(2.dp),
        small = SquareShape,
        medium = SquareShape,
        large = SquareShape,
        extraLarge = SquareShape,
        buttonShape = SquareShape,
        chipShape = SquareShape,
        cardShape = RoundedCornerShape(4.dp),
        dialogShape = RoundedCornerShape(4.dp),
        textFieldShape = RoundedCornerShape(4.dp),
    )
    AppShapeStyle.ROUNDED -> AppShapeSet(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
        buttonShape = RoundedCornerShape(10.dp),
        chipShape = RoundedCornerShape(8.dp),
        cardShape = RoundedCornerShape(12.dp),
        dialogShape = RoundedCornerShape(16.dp),
        textFieldShape = RoundedCornerShape(10.dp),
    )
    AppShapeStyle.SOFT -> AppShapeSet(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
        buttonShape = RoundedCornerShape(16.dp),
        chipShape = RoundedCornerShape(14.dp),
        cardShape = RoundedCornerShape(18.dp),
        dialogShape = RoundedCornerShape(22.dp),
        textFieldShape = RoundedCornerShape(14.dp),
    )
    AppShapeStyle.PILL -> AppShapeSet(
        extraSmall = RoundedCornerShape(percent = 50),
        small = RoundedCornerShape(percent = 50),
        medium = RoundedCornerShape(percent = 50),
        large = RoundedCornerShape(percent = 50),
        extraLarge = RoundedCornerShape(percent = 50),
        buttonShape = RoundedCornerShape(percent = 50),
        chipShape = RoundedCornerShape(percent = 50),
        cardShape = RoundedCornerShape(20.dp),
        dialogShape = RoundedCornerShape(20.dp),
        textFieldShape = RoundedCornerShape(percent = 50),
    )
    AppShapeStyle.CUT -> AppShapeSet(
        extraSmall = CutCornerShape(4.dp),
        small = CutCornerShape(6.dp),
        medium = CutCornerShape(10.dp),
        large = CutCornerShape(14.dp),
        extraLarge = CutCornerShape(18.dp),
        buttonShape = CutCornerShape(8.dp),
        chipShape = CutCornerShape(6.dp),
        cardShape = CutCornerShape(10.dp),
        dialogShape = CutCornerShape(12.dp),
        textFieldShape = CutCornerShape(8.dp),
    )
    AppShapeStyle.OPPOSITE_CUT -> AppShapeSet(
        extraSmall = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
        small = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        medium = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        large = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
        extraLarge = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp),
        buttonShape = OppositeCutShape,
        chipShape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
        cardShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        dialogShape = RoundedCornerShape(10.dp),
        textFieldShape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
    )
    AppShapeStyle.SINGLE_CUT -> AppShapeSet(
        extraSmall = CutCornerShape(bottomEnd = 6.dp),
        small = CutCornerShape(bottomEnd = 8.dp),
        medium = CutCornerShape(bottomEnd = 10.dp),
        large = CutCornerShape(bottomEnd = 14.dp),
        extraLarge = CutCornerShape(bottomEnd = 18.dp),
        buttonShape = SingleCutShape,
        chipShape = CutCornerShape(bottomEnd = 6.dp),
        cardShape = CutCornerShape(bottomEnd = 8.dp),
        dialogShape = RoundedCornerShape(12.dp),
        textFieldShape = CutCornerShape(bottomEnd = 8.dp),
    )
    AppShapeStyle.CHEVRON -> AppShapeSet(
        extraSmall = CutCornerShape(4.dp),
        small = CutCornerShape(6.dp),
        medium = CutCornerShape(8.dp),
        large = CutCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
        buttonShape = ChevronShape,
        chipShape = CutCornerShape(6.dp),
        cardShape = CutCornerShape(10.dp),
        dialogShape = RoundedCornerShape(14.dp),
        textFieldShape = CutCornerShape(6.dp),
    )
    AppShapeStyle.BEVELED -> AppShapeSet(
        extraSmall = CutCornerShape(4.dp),
        small = CutCornerShape(6.dp),
        medium = CutCornerShape(8.dp),
        large = CutCornerShape(10.dp),
        extraLarge = RoundedCornerShape(16.dp),
        buttonShape = BeveledShape,
        chipShape = CutCornerShape(6.dp),
        cardShape = CutCornerShape(8.dp),
        dialogShape = RoundedCornerShape(14.dp),
        textFieldShape = CutCornerShape(6.dp),
    )
    AppShapeStyle.NOTCHED -> AppShapeSet(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(18.dp),
        buttonShape = NotchedShape,
        chipShape = RoundedCornerShape(8.dp),
        cardShape = RoundedCornerShape(12.dp),
        dialogShape = RoundedCornerShape(14.dp),
        textFieldShape = RoundedCornerShape(8.dp),
    )
    AppShapeStyle.TICKET -> AppShapeSet(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(18.dp),
        buttonShape = TicketShape,
        chipShape = RoundedCornerShape(8.dp),
        cardShape = RoundedCornerShape(10.dp),
        dialogShape = RoundedCornerShape(14.dp),
        textFieldShape = RoundedCornerShape(10.dp),
    )
    AppShapeStyle.ASYMMETRIC -> AppShapeSet(
        extraSmall = RoundedCornerShape(topStart = 10.dp, topEnd = 4.dp, bottomEnd = 10.dp, bottomStart = 4.dp),
        small = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
        medium = RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
        large = RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
        extraLarge = RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp),
        buttonShape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
        chipShape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 4.dp),
        cardShape = RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 4.dp),
        dialogShape = RoundedCornerShape(14.dp),
        textFieldShape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 4.dp),
    )
    AppShapeStyle.TOP_ROUNDED -> AppShapeSet(
        extraSmall = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
        small = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        medium = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
        large = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
        extraLarge = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        buttonShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        chipShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        cardShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        dialogShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
        textFieldShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
    )
}
