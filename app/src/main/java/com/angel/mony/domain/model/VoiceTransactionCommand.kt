package com.angel.mony.domain.model

import com.angel.mony.core.MoneyFormatter
import java.text.Normalizer
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

data class VoiceTransactionDraft(
    val type: TransactionType? = null,
    val amountInCents: Long? = null,
    val categoryId: Long? = null,
    val date: LocalDate? = null,
    val note: String? = null,
    val fundingSource: String? = null,
    val saveRequested: Boolean = false,
)

sealed interface VoiceTransactionCommand {
    data class Apply(
        val draft: VoiceTransactionDraft,
        val mentioned: Set<VoiceDraftField>,
        val categoryOptions: List<Category> = emptyList(),
        val messages: List<String> = emptyList(),
    ) : VoiceTransactionCommand

    data object Save : VoiceTransactionCommand
    data object Review : VoiceTransactionCommand
    data object MissingFields : VoiceTransactionCommand
    data object ShowCategories : VoiceTransactionCommand
    data object Undo : VoiceTransactionCommand
    data object Cancel : VoiceTransactionCommand
    data object Clear : VoiceTransactionCommand
    data object Exit : VoiceTransactionCommand
    data object ConfirmFunding : VoiceTransactionCommand
    data class SelectCategoryOption(val index: Int) : VoiceTransactionCommand
    data class Invalid(val message: String) : VoiceTransactionCommand
}

enum class VoiceDraftField { TYPE, AMOUNT, CATEGORY, DATE, NOTE, FUNDING_SOURCE }

/**
 * Gramática de voz de una sola captura. Una orden de guardado solo se reconoce al final de la
 * frase y fuera de una cláusula de nota. De este modo, "nota recordar guardar el recibo" es texto,
 * mientras que "nota taxi al trabajo y guárdalo" autoriza el guardado del borrador actual.
 */
class VoiceTransactionInterpreter(private val clock: Clock) {
    fun interpret(
        transcript: String,
        categories: List<Category>,
        current: VoiceTransactionDraft = VoiceTransactionDraft(),
    ): VoiceTransactionCommand {
        val original = transcript.trim().replace(Regex("\\s+"), " ")
        if (original.isBlank()) return VoiceTransactionCommand.Invalid("No se reconoció ningún comando.")
        val normalized = original.voiceNormalized()

        simpleCommand(normalized)?.let { return it }
        categoryOrdinal(normalized)?.let { return VoiceTransactionCommand.SelectCategoryOption(it) }

        val saveRequested = SAVE_SUFFIX.find(normalized) != null
        val commandText = SAVE_SUFFIX.replace(normalized, "").trim().trimEnd(',', '.')
        val commandCore = commandText
            .substringBefore(" con nota ")
            .substringBefore(" nota ")
            .substringBefore(" con eso ")
        val mentioned = linkedSetOf<VoiceDraftField>()
        val messages = mutableListOf<String>()
        var result = current.copy(saveRequested = current.saveRequested || saveRequested)

        val detectedTypes = buildSet {
            if (EXPENSE_WORDS.any { commandCore.containsWord(it) }) add(TransactionType.EXPENSE)
            if (INCOME_WORDS.any { commandCore.containsWord(it) }) add(TransactionType.INCOME)
        }
        if (detectedTypes.size > 1) {
            return VoiceTransactionCommand.Invalid("La frase menciona gasto e ingreso. ¿Cuál deseas registrar?")
        }
        detectedTypes.singleOrNull()?.let {
            result = result.copy(type = it)
            mentioned += VoiceDraftField.TYPE
        }

        parseFundingSource(original)?.let {
            result = result.copy(fundingSource = it)
            mentioned += VoiceDraftField.FUNDING_SOURCE
        }

        when {
            normalized == "borra la nota" || normalized == "borra nota" -> {
                result = result.copy(note = null)
                mentioned += VoiceDraftField.NOTE
            }
            normalized.startsWith("agrega a la nota ") -> {
                val addition = Regex("(?i)\\bnota\\s+(.+)$").find(original)?.groupValues?.get(1)?.trim().orEmpty()
                result = result.copy(note = listOfNotNull(current.note?.takeIf(String::isNotBlank), addition)
                    .joinToString(" "))
                mentioned += VoiceDraftField.NOTE
            }
            NOTE_PATTERN.find(original)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank) != null -> {
                val note = NOTE_PATTERN.find(original)!!.groupValues[1].trim()
                    .replace(SAVE_SUFFIX_ORIGINAL, "").trim().trimEnd(',', '.')
                result = result.copy(note = note)
                mentioned += VoiceDraftField.NOTE
            }
        }

        val moneyText = amountFragment(commandCore)
        if (moneyText != null) {
            when (val parsed = SpanishMoneyParser.parse(moneyText)) {
                is SpokenMoney.Valid -> {
                    result = result.copy(amountInCents = parsed.cents)
                    mentioned += VoiceDraftField.AMOUNT
                }
                is SpokenMoney.Invalid -> messages += parsed.message
            }
        }

        parseDate(commandCore)?.let { parsed ->
            if (parsed.date != null) {
                result = result.copy(date = parsed.date)
                mentioned += VoiceDraftField.DATE
            } else messages += parsed.error.orEmpty()
        }

        val resolvedType = result.type
        if (resolvedType != null) {
            val active = categories.filter { it.type == resolvedType && it.isActive }
            val matches = categoryMatches(commandCore, active)
            when {
                matches.size == 1 -> {
                    result = result.copy(categoryId = matches.single().id)
                    mentioned += VoiceDraftField.CATEGORY
                }
                matches.size > 1 -> messages += "Hay varias categorías con ese nombre. Elige una opción."
                categoryWasRequested(commandCore) -> messages += "No encontré una categoría activa válida para ese movimiento."
            }
            if (matches.size > 1) {
                return VoiceTransactionCommand.Apply(result, mentioned, matches, messages)
            }
        }

        if (mentioned.isEmpty() && messages.isEmpty() && !saveRequested) {
            return VoiceTransactionCommand.Invalid("No entendí el comando. Puedes registrar o corregir un movimiento.")
        }
        return VoiceTransactionCommand.Apply(result, mentioned, messages = messages)
    }

    private fun simpleCommand(text: String): VoiceTransactionCommand? = when (text) {
        "guardalo", "guardar", "guarda el gasto", "guarda el ingreso", "guarda el movimiento" -> VoiceTransactionCommand.Save
        "revisa el movimiento", "revisar el movimiento" -> VoiceTransactionCommand.Review
        "que falta para guardar" -> VoiceTransactionCommand.MissingFields
        "muestrame las categorias", "mostrar categorias" -> VoiceTransactionCommand.ShowCategories
        "deshaz el ultimo cambio", "deshacer el ultimo cambio" -> VoiceTransactionCommand.Undo
        "cancelar dictado", "cancela el dictado" -> VoiceTransactionCommand.Cancel
        "limpia el formulario", "limpiar el formulario" -> VoiceTransactionCommand.Clear
        "volver", "salir", "salir sin guardar", "volver sin guardar" -> VoiceTransactionCommand.Exit
        "confirma la fuente", "confirmar la fuente" -> VoiceTransactionCommand.ConfirmFunding
        else -> null
    }

    private fun categoryOrdinal(text: String): Int? = when (text) {
        "la primera", "primera" -> 0
        "la segunda", "segunda" -> 1
        "la tercera", "tercera" -> 2
        "la cuarta", "cuarta" -> 3
        else -> null
    }

    private fun parseFundingSource(text: String): String? =
        Regex("(?i)\\bla fuente es\\s+(.+?)(?=\\s+y\\s+(?:confirma|gu[aá]rd)|$)")
            .find(text)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)

    private fun amountFragment(text: String): String? {
        val correction = Regex("(?:cambia|pon) el monto (?:a|en) (.+?)(?=\\s+(?:en|para|hoy|ayer|anteayer|hace|con nota|nota|el dia|la fecha)\\b|$)")
            .find(text)?.groupValues?.get(1)
        if (correction != null) return correction.removeSuffix(" pesos").trim()
        val creation = Regex("(?:gasto|ingreso|gaste|recibi)\\s+(?:de\\s+)?(.+?)(?=\\s+(?:pesos?|rd\\$?)?\\s*(?:en|para|hoy|ayer|anteayer|hace|con nota|nota|el dia|la fecha|el (?:lunes|martes|miercoles|jueves|viernes|sabado|domingo)|proximo (?:lunes|martes|miercoles|jueves|viernes|sabado|domingo))\\b|$)")
            .find(text)?.groupValues?.get(1)
        if (creation != null) return creation.removeSuffix(" pesos").trim()
        return GENERIC_CREATION_AMOUNT.find(text)?.groupValues?.get(1)?.trim()
    }

    private data class ParsedDate(val date: LocalDate?, val error: String? = null)
    private data class WeekdayMention(val dayOfWeek: DayOfWeek, val direction: WeekdayDirection)
    private enum class WeekdayDirection { RECENT, PAST, FUTURE }

    private fun parseDate(text: String): ParsedDate? {
        val today = LocalDate.now(clock)
        val explicit = parseExplicitDate(text, today)
        val weekday = parseWeekday(text)
        if (explicit != null) {
            if (
                explicit.date != null &&
                weekday != null &&
                explicit.date.dayOfWeek != weekday.dayOfWeek
            ) {
                return ParsedDate(
                    null,
                    "La fecha explícita no coincide con el día de la semana. Aclara cuál fecha deseas usar.",
                )
            }
            return explicit
        }
        return weekday?.let { ParsedDate(resolveWeekday(today, it)) }
    }

    private fun parseExplicitDate(text: String, today: LocalDate): ParsedDate? = when {
        ISO_DATE.find(text) != null -> ParsedDate(
            runCatching { LocalDate.parse(ISO_DATE.find(text)!!.value) }.getOrNull(),
            "La fecha no es válida.",
        )
        SLASH_DATE.find(text) != null -> {
            val date = runCatching { LocalDate.parse(SLASH_DATE.find(text)!!.value, SLASH_FORMATTER) }.getOrNull()
            ParsedDate(date, if (date == null) "La fecha no es válida." else null)
        }
        FULL_DATE.find(text) != null -> {
            val match = FULL_DATE.find(text)!!
            val month = MONTHS[match.groupValues[2]]
            val date = runCatching { LocalDate.of(match.groupValues[3].toInt(), month!!, match.groupValues[1].toInt()) }.getOrNull()
            ParsedDate(date, if (date == null) "La fecha no es válida." else null)
        }
        INCOMPLETE_DATE.containsMatchIn(text) -> ParsedDate(null, "Indica también el año para evitar una fecha ambigua.")
        else -> when {
            text.containsWord("anteayer") -> ParsedDate(today.minusDays(2))
            text.containsWord("ayer") -> ParsedDate(today.minusDays(1))
            text.containsWord("hoy") -> ParsedDate(today)
            DAYS_AGO.find(text) != null -> {
                val rawDays = DAYS_AGO.find(text)!!.groupValues[1]
                val days = rawDays.toLongOrNull() ?: when (val spoken = SpanishMoneyParser.parse(rawDays)) {
                    is SpokenMoney.Valid -> spoken.cents.takeIf { it % 100L == 0L }?.div(100L)
                    is SpokenMoney.Invalid -> null
                }
                if (days == null) ParsedDate(null, "No pude interpretar la fecha.") else ParsedDate(today.minusDays(days))
            }
            else -> null
        }
    }

    private fun parseWeekday(text: String): WeekdayMention? {
        val futurePrefix = FUTURE_WEEKDAY_PREFIX.find(text)
        if (futurePrefix != null) return WeekdayMention(WEEKDAYS.getValue(futurePrefix.groupValues[1]), WeekdayDirection.FUTURE)
        val futureSuffix = FUTURE_WEEKDAY_SUFFIX.find(text)
        if (futureSuffix != null) return WeekdayMention(WEEKDAYS.getValue(futureSuffix.groupValues[1]), WeekdayDirection.FUTURE)
        val past = PAST_WEEKDAY.find(text)
        if (past != null) return WeekdayMention(WEEKDAYS.getValue(past.groupValues[1]), WeekdayDirection.PAST)
        val recent = PLAIN_WEEKDAY.find(text) ?: return null
        return WeekdayMention(WEEKDAYS.getValue(recent.groupValues[1]), WeekdayDirection.RECENT)
    }

    private fun resolveWeekday(today: LocalDate, mention: WeekdayMention): LocalDate {
        val current = today.dayOfWeek.value
        val target = mention.dayOfWeek.value
        return when (mention.direction) {
            WeekdayDirection.RECENT -> today.minusDays(((current - target + 7) % 7).toLong())
            WeekdayDirection.PAST -> {
                val days = (current - target + 7) % 7
                today.minusDays((if (days == 0) 7 else days).toLong())
            }
            WeekdayDirection.FUTURE -> {
                val days = (target - current + 7) % 7
                today.plusDays((if (days == 0) 7 else days).toLong())
            }
        }
    }

    private fun categoryMatches(text: String, categories: List<Category>): List<Category> {
        val direct = categories.filter { text.containsWord(it.name.voiceNormalized()) }
        if (direct.isNotEmpty()) return direct
        // Speech recognizers sometimes join the preposition to the following word ("entrasporte").
        // This remains an exact category-name match; it is not fuzzy matching.
        val joinedPreposition = categories.filter { category ->
            val name = category.name.voiceNormalized()
            Regex("(?:^|\\s)en${Regex.escape(name)}(?:$|[\\s,.;:])").containsMatchIn(text)
        }
        if (joinedPreposition.isNotEmpty()) return joinedPreposition
        val targetNames = CATEGORY_ALIASES.filter { (alias, _) ->
            text.containsWord(alias) || Regex("(?:^|\\s)en${Regex.escape(alias)}(?:$|[\\s,.;:])").containsMatchIn(text)
        }.values.flatten().toSet()
        return categories.filter { it.name.voiceNormalized() in targetNames }
    }

    private fun categoryWasRequested(text: String): Boolean =
        Regex("\\b(?:categoria|en|para)\\s+[a-z]").containsMatchIn(text)

    companion object {
        private val EXPENSE_WORDS = setOf("gasto", "gaste", "egreso")
        private val INCOME_WORDS = setOf("ingreso", "recibi", "cobro")
        private val CATEGORY_ALIASES = mapOf(
            "comida" to setOf("alimentacion", "comida"),
            "carro" to setOf("transporte"),
            "trasporte" to setOf("transporte"),
            "casa" to setOf("vivienda", "hogar"),
            "medico" to setOf("salud"),
            "sueldo" to setOf("salario"),
        )
        private val SAVE_SUFFIX = Regex("(?:,?\\s+y\\s+)(?:guardalo|guarda(?:r)?(?: el (?:gasto|ingreso|movimiento))?)$")
        private val SAVE_SUFFIX_ORIGINAL = Regex("(?i)(?:,?\\s+y\\s+)(?:gu[aá]rdalo|guarda(?:r)?(?: el (?:gasto|ingreso|movimiento))?)$")
        private val NOTE_PATTERN = Regex("(?i)\\b(?:cambia la nota a|pon la nota|con nota|nota|con eso)\\s+(.+)$")
        private val GENERIC_CREATION_AMOUNT = Regex(
            "^(?:(?:registra|agrega|anota|suma)(?:\\s+un)?(?:\\s+(?:gasto|ingreso))?(?:\\s+de)?\\s+)?(.+?)\\s+pesos?(?=\\s|$)",
        )
        private val ISO_DATE = Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b")
        private val SLASH_DATE = Regex("\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b")
        private val SLASH_FORMATTER = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)
        private val DAYS_AGO = Regex("\\bhace (\\d+|[a-z]+(?:\\s+y\\s+[a-z]+)?) dias?\\b")
        private val FULL_DATE = Regex("\\b(\\d{1,2}) de (enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre) de (\\d{4})\\b")
        private val INCOMPLETE_DATE = Regex("\\b\\d{1,2} de (?:enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre)\\b")
        private const val WEEKDAY_NAMES = "lunes|martes|miercoles|jueves|viernes|sabado|domingo"
        private val FUTURE_WEEKDAY_PREFIX = Regex("\\b(?:el\\s+)?proximo\\s+($WEEKDAY_NAMES)\\b")
        private val FUTURE_WEEKDAY_SUFFIX = Regex("\\b(?:el\\s+)?($WEEKDAY_NAMES)\\s+que viene\\b")
        private val PAST_WEEKDAY = Regex("\\b(?:el\\s+)?($WEEKDAY_NAMES)\\s+pasado\\b")
        private val PLAIN_WEEKDAY = Regex("\\b(?:el\\s+)?($WEEKDAY_NAMES)\\b")
        private val WEEKDAYS = mapOf(
            "lunes" to DayOfWeek.MONDAY,
            "martes" to DayOfWeek.TUESDAY,
            "miercoles" to DayOfWeek.WEDNESDAY,
            "jueves" to DayOfWeek.THURSDAY,
            "viernes" to DayOfWeek.FRIDAY,
            "sabado" to DayOfWeek.SATURDAY,
            "domingo" to DayOfWeek.SUNDAY,
        )
        private val MONTHS = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
            .mapIndexed { index, name -> name to index + 1 }.toMap()
    }
}

sealed interface SpokenMoney {
    data class Valid(val cents: Long) : SpokenMoney
    data class Invalid(val message: String) : SpokenMoney
}

object SpanishMoneyParser {
    fun parse(raw: String): SpokenMoney {
        val text = raw.voiceNormalized().removePrefix("de ").trim()
        if (text.isBlank()) return SpokenMoney.Invalid("Indica un monto.")
        if (text.startsWith("menos ") || text.startsWith("-")) return SpokenMoney.Invalid("El monto debe ser mayor que cero.")
        if (text.any(Char::isDigit)) return parseDigits(text)

        val parts = when {
            " con " in text -> text.split(" con ", limit = 2)
            Regex("\\s+y\\s+.+\\s+centavos?$").containsMatchIn(text) -> text.split(Regex("\\s+y\\s+"), limit = 2)
            else -> listOf(text)
        }
        val wholeWords = parts[0].removeSuffix(" pesos").removeSuffix(" peso").trim()
        val whole = parseIntegerWords(wholeWords)
            ?: return SpokenMoney.Invalid("No pude interpretar el monto.")
        val cents = if (parts.size == 2) {
            val centsText = parts[1].removeSuffix(" centavos").removeSuffix(" centavo").trim()
            parseIntegerWords(centsText)?.takeIf { it in 0..99 }
                ?: return SpokenMoney.Invalid("Los centavos no son válidos.")
        } else 0L
        if (whole > (Long.MAX_VALUE - cents) / 100L) return SpokenMoney.Invalid("El monto está fuera de rango.")
        val total = whole * 100L + cents
        return if (total > 0) SpokenMoney.Valid(total) else SpokenMoney.Invalid("El monto debe ser mayor que cero.")
    }

    private fun parseDigits(text: String): SpokenMoney {
        val token = Regex("\\d[\\d.,]*").find(text)?.value
            ?: return SpokenMoney.Invalid("No pude interpretar el monto.")
        if (token.contains(',') && token.contains('.')) {
            return SpokenMoney.Invalid("El monto tiene separadores ambiguos. Dilo nuevamente con pesos y centavos.")
        }
        val separator = token.firstOrNull { it == ',' || it == '.' }
        if (separator != null && token.count { it == separator } > 1) {
            return SpokenMoney.Invalid("El monto tiene separadores ambiguos. Dilo nuevamente.")
        }
        if (separator != null && token.substringAfter(separator).length == 3) {
            return SpokenMoney.Invalid("No sé si el separador indica miles o decimales. Dilo nuevamente.")
        }
        val cents = MoneyFormatter.parseToCents(token)
            ?: return SpokenMoney.Invalid("El monto no es válido.")
        return if (cents > 0) SpokenMoney.Valid(cents) else SpokenMoney.Invalid("El monto debe ser mayor que cero.")
    }

    private fun parseIntegerWords(text: String): Long? {
        if (text == "cero") return 0
        var total = 0L
        var group = 0L
        for (word in text.replace(" y ", " ").split(' ').filter(String::isNotBlank)) {
            when (word) {
                "mil" -> { total += (if (group == 0L) 1L else group) * 1_000L; group = 0L }
                "millon", "millones" -> { total = (total + if (group == 0L) 1L else group) * 1_000_000L; group = 0L }
                else -> group += NUMBERS[word] ?: return null
            }
        }
        return runCatching { Math.addExact(total, group) }.getOrNull()
    }

    private val NUMBERS = mapOf(
        "un" to 1L, "uno" to 1L, "una" to 1L, "dos" to 2L, "tres" to 3L, "cuatro" to 4L,
        "cinco" to 5L, "seis" to 6L, "siete" to 7L, "ocho" to 8L, "nueve" to 9L,
        "diez" to 10L, "once" to 11L, "doce" to 12L, "trece" to 13L, "catorce" to 14L,
        "quince" to 15L, "dieciseis" to 16L, "diecisiete" to 17L, "dieciocho" to 18L,
        "diecinueve" to 19L, "veinte" to 20L, "veintiuno" to 21L, "veintidos" to 22L,
        "veintitres" to 23L, "veinticuatro" to 24L, "veinticinco" to 25L, "veintiseis" to 26L,
        "veintisiete" to 27L, "veintiocho" to 28L, "veintinueve" to 29L,
        "treinta" to 30L, "cuarenta" to 40L, "cincuenta" to 50L, "sesenta" to 60L,
        "setenta" to 70L, "ochenta" to 80L, "noventa" to 90L, "cien" to 100L,
        "ciento" to 100L, "doscientos" to 200L, "trescientos" to 300L, "cuatrocientos" to 400L,
        "quinientos" to 500L, "seiscientos" to 600L, "setecientos" to 700L,
        "ochocientos" to 800L, "novecientos" to 900L,
    )
}

internal fun String.voiceNormalized(): String = Normalizer.normalize(lowercase(Locale.forLanguageTag("es-DO")), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .replace('¿', ' ')
    .replace('?', ' ')
    .replace(Regex("\\s+"), " ")
    .trim()

private fun String.containsWord(value: String): Boolean =
    Regex("(?:^|\\s)${Regex.escape(value)}(?:$|[\\s,.;:])").containsMatchIn(this)
