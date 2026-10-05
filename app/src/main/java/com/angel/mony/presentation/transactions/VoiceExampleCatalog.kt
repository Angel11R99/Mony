package com.angel.mony.presentation.transactions

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType
import kotlin.random.Random

data class VoiceExample(
    val key: String,
    val text: String,
    val type: TransactionType,
    val categoryId: Long?,
)

data class VoiceExampleRotationState(
    val remainingKeys: List<String> = emptyList(),
    val lastKey: String? = null,
)

data class VoiceExampleSession(
    val openingId: String,
    val exampleKey: String,
)

data class VoiceExampleSelection(
    val example: VoiceExample,
    val rotationState: VoiceExampleRotationState,
    val session: VoiceExampleSession,
    val advanced: Boolean,
)

object VoiceExampleCatalog {
    fun build(categories: List<Category>): List<VoiceExample> = buildList {
        TransactionType.entries.forEach { type ->
            val active = categories
                .filter { it.isActive && it.type == type }
                .sortedWith(compareBy(Category::name, Category::id))
            if (active.isEmpty()) {
                addTemplates(type, null)
            } else {
                active.forEach { addTemplates(type, it) }
            }
        }
    }

    private fun MutableList<VoiceExample>.addTemplates(type: TransactionType, category: Category?) {
        val categoryText = category?.let { " en ${it.name}" }.orEmpty()
        val templates = when (type) {
            TransactionType.EXPENSE -> listOf(
                "Registra un gasto de quinientos pesos$categoryText hoy, con nota compra del día",
                "Agrega un gasto de doscientos pesos$categoryText el viernes",
                "Anota un gasto de trescientos pesos$categoryText hoy y guárdalo",
                "Gasté ciento cincuenta pesos$categoryText ayer, con nota pago realizado",
            )
            TransactionType.INCOME -> listOf(
                "Registra un ingreso de mil pesos$categoryText hoy, con nota pago recibido",
                "Agrega un ingreso de dos mil pesos$categoryText el domingo",
                "Anota un ingreso de quinientos pesos$categoryText hoy y guárdalo",
                "Recibí mil quinientos pesos$categoryText ayer, con nota depósito",
            )
        }
        templates.forEachIndexed { index, text ->
            add(
                VoiceExample(
                    key = "${type.name}:${category?.id ?: "none"}:$index",
                    text = text,
                    type = type,
                    categoryId = category?.id,
                ),
            )
        }
    }
}

fun selectVoiceExample(
    catalog: List<VoiceExample>,
    rotationState: VoiceExampleRotationState,
    currentSession: VoiceExampleSession?,
    openingId: String,
    random: Random = Random.Default,
): VoiceExampleSelection {
    require(catalog.isNotEmpty()) { "El catálogo de ejemplos no puede estar vacío" }
    val byKey = catalog.associateBy(VoiceExample::key)
    if (currentSession?.openingId == openingId) {
        byKey[currentSession.exampleKey]?.let { current ->
            return VoiceExampleSelection(current, rotationState, currentSession, advanced = false)
        }
    }

    var remaining = rotationState.remainingKeys.filter(byKey::containsKey)
    if (remaining.isEmpty()) {
        remaining = catalog.map(VoiceExample::key).shuffled(random)
        if (remaining.size > 1 && remaining.first() == rotationState.lastKey) {
            val replacement = remaining.indexOfFirst { it != rotationState.lastKey }
            remaining = remaining.toMutableList().also { keys ->
                val first = keys[0]
                keys[0] = keys[replacement]
                keys[replacement] = first
            }
        }
    }
    val selectedKey = remaining.first()
    val selected = byKey.getValue(selectedKey)
    return VoiceExampleSelection(
        example = selected,
        rotationState = VoiceExampleRotationState(
            remainingKeys = remaining.drop(1),
            lastKey = selectedKey,
        ),
        session = VoiceExampleSession(openingId, selectedKey),
        advanced = true,
    )
}
