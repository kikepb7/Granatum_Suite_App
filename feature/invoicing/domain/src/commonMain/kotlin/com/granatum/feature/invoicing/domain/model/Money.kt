package com.granatum.feature.invoicing.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue

/**
 * An amount in cents. The server sends and takes decimals as text (`"1234.56"`, up to ten whole
 * digits and two decimals); a Long of cents holds them exactly, where floating point would not
 * (specs/008-facturacion, research D2). VAT rates use it too, as a percentage with two decimals.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun unaryMinus() = Money(-cents)
    override fun compareTo(other: Money) = cents.compareTo(other.cents)

    val isZero: Boolean get() = cents == 0L
    val isNegative: Boolean get() = cents < 0L

    /** The server's form: dot as decimal separator, always two decimals, no grouping. */
    fun toApi(): String {
        val sign = if (cents < 0) "-" else ""
        val abs = cents.absoluteValue
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    /** The Spanish form for people: `1.234,56`. */
    fun format(): String {
        val sign = if (cents < 0) "-" else ""
        val abs = cents.absoluteValue
        val whole = (abs / 100).toString().reversed().chunked(3).joinToString(".").reversed()
        return "$sign$whole,${(abs % 100).toString().padStart(2, '0')}"
    }

    /** What goes in an input: `1234,56`, without grouping so it can be edited back. */
    fun toInput(): String = toApi().replace('.', ',')

    companion object {
        val ZERO = Money(0)
        private const val MAX_WHOLE_DIGITS = 10
        private val PATTERN = Regex("""^(-)?(\d{1,$MAX_WHOLE_DIGITS})(?:[.,](\d{1,2}))?$""")

        /**
         * Reads what the server sends or what a person types: a dot or a comma as decimal
         * separator, up to two decimals, no grouping. Null when it is not a valid amount.
         */
        fun parse(text: String?): Money? {
            val match = PATTERN.matchEntire(text?.trim()?.replace(" ", "") ?: return null) ?: return null
            val (sign, whole, decimals) = match.destructured
            val cents = whole.toLong() * 100 + decimals.padEnd(2, '0').ifEmpty { "00" }.toLong()
            return Money(if (sign == "-") -cents else cents)
        }

        /** The server sends some totals as JSON numbers; read their text, never via Double. */
        fun parseNumber(text: String?): Money? {
            val raw = text?.trim() ?: return null
            if (raw.contains('e', ignoreCase = true)) return null
            val dot = raw.indexOf('.')
            val normalised = if (dot >= 0 && raw.length - dot - 1 > 2) raw.substring(0, dot + 3) else raw
            return parse(normalised)
        }
    }
}
