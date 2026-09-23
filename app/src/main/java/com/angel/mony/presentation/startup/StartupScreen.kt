package com.angel.mony.presentation.startup

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.angel.mony.R
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

private data class IconSlot(val x: Dp, val y: Dp)

private val financeIcons = listOf(
    Icons.Rounded.Wallet,
    Icons.Rounded.Savings,
    Icons.Rounded.Payments,
    Icons.AutoMirrored.Rounded.ReceiptLong,
    Icons.Rounded.CreditCard,
    Icons.Rounded.AccountBalance,
    Icons.Rounded.AttachMoney,
    Icons.AutoMirrored.Rounded.TrendingUp,
    Icons.Rounded.BarChart,
    Icons.Rounded.PieChart,
    Icons.Rounded.CurrencyExchange,
    Icons.Rounded.ShoppingCart,
)

@Composable
fun StartupScreen(
    state: AppStartupState,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedFinanceBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.personal_finance_tracker_icon),
                contentDescription = null,
                modifier = Modifier.size(178.dp),
                colorFilter = null,
            )
            Spacer(Modifier.height(34.dp))
            Text(
                text = "MONY",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(18.dp))

            when (state) {
                AppStartupState.Loading -> LoadingMessage()
                AppStartupState.Ready -> Unit
                is AppStartupState.Error -> ErrorMessage(state.message, onRetry)
            }
        }
    }
}

@Composable
private fun LoadingMessage() {
    CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        strokeWidth = 3.dp,
        color = MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = "Preparando tus finanzas…",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
    )
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRetry) {
        Text("Reintentar")
    }
}

@Composable
private fun AnimatedFinanceBackground() {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    if (reduceMotion) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val slots = remember(maxWidth, maxHeight) {
            buildIconSlots(maxWidth, maxHeight).shuffled(Random(maxWidth.value.toBits() xor maxHeight.value.toBits()))
        }
        val iconCount = remember(maxWidth, maxHeight, slots.size) {
            calculateIconCount(maxWidth.value, maxHeight.value, slots.size)
        }
        val assignments = remember(slots, iconCount) {
            mutableStateListOf<Int>().apply { addAll((0 until iconCount).toList()) }
        }

        repeat(iconCount) { index ->
            AnimatedFinanceIcon(
                index = index,
                icon = financeIcons[index % financeIcons.size],
                slots = slots,
                assignments = assignments,
            )
        }
    }
}

@Composable
private fun AnimatedFinanceIcon(
    index: Int,
    icon: ImageVector,
    slots: List<IconSlot>,
    assignments: MutableList<Int>,
) {
    val random = remember(index, slots.size) { Random(index * 7_919 + slots.size) }
    var targetAlpha by remember { mutableStateOf(random.nextFloat() * 0.29f + 0.17f) }
    val animatedAlpha = remember { Animatable(if (index < 12) targetAlpha else 0f) }
    var iconSize by remember { mutableStateOf(random.nextInt(16, 49).dp) }

    LaunchedEffect(index, slots) {
        delay(random.nextLong(0L, 5_201L))
        while (true) {
            animatedAlpha.animateTo(targetAlpha, tween(random.nextInt(480, 901)))
            delay(random.nextLong(2_600L, 5_201L))
            animatedAlpha.animateTo(0f, tween(random.nextInt(420, 801)))
            delay(1_500L)

            val occupied = assignments.filterIndexed { otherIndex, _ -> otherIndex != index }.toSet()
            val available = slots.indices.filterNot(occupied::contains)
            if (available.isNotEmpty()) assignments[index] = available.random(random)
            iconSize = random.nextInt(16, 49).dp
            targetAlpha = random.nextFloat() * 0.29f + 0.17f
        }
    }

    val slot = slots.getOrNull(assignments.getOrElse(index) { index }) ?: return
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .offset(x = slot.x - iconSize / 2, y = slot.y - iconSize / 2)
            .size(iconSize)
            .alpha(animatedAlpha.value),
    )
}

private fun buildIconSlots(width: Dp, height: Dp): List<IconSlot> {
    val cellSize = 60.dp
    val columns = floor(width / cellSize).toInt().coerceAtLeast(1)
    val rows = floor(height / cellSize).toInt().coerceAtLeast(1)
    val cellWidth = width / columns
    val cellHeight = height / rows
    return buildList(columns * rows) {
        repeat(rows) { row ->
            repeat(columns) { column ->
                val xFraction = (column + 0.5f) / columns
                val yFraction = (row + 0.5f) / rows
                val overlapsContent = xFraction in 0.18f..0.82f && yFraction in 0.24f..0.72f
                val overlapsSystemBars = yFraction !in 0.08f..0.92f
                if (!overlapsContent && !overlapsSystemBars) {
                    add(
                        IconSlot(
                            x = cellWidth * (column + 0.5f),
                            y = cellHeight * (row + 0.5f),
                        ),
                    )
                }
            }
        }
    }
}

internal fun calculateIconCount(widthDp: Float, heightDp: Float, slotCount: Int): Int {
    if (slotCount <= 0) return 0
    val areaCount = (widthDp * heightDp / 10_000f).roundToInt().coerceIn(12, 60)
    val availableCount = if (slotCount <= 16) slotCount else slotCount - 4
    return minOf(areaCount, availableCount)
}
