package com.nof1.core.export

import java.math.BigDecimal

/** CSV formatting as in RFC 4180: comma-separated fields, CRLF line endings. */
internal object Csv {
    private const val LINE_END = "\r\n"

    // Spreadsheet apps can run a cell that starts with one of these as a formula.
    private const val FORMULA_STARTS = "=+-@\t\r\n"

    fun line(fields: List<String>): String =
        fields.joinToString(",", transform = ::escape) + LINE_END

    /** Quotes a field that contains a comma, quote or line break, doubling any quotes. */
    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /**
     * Text the user typed, made safe to open in a spreadsheet: if it could be read as a formula, a
     * leading apostrophe makes it plain text (the OWASP recommendation against CSV injection).
     */
    fun userText(text: String): String {
        val first = text.firstOrNull() ?: return text
        return if (first in FORMULA_STARTS) "'$text" else text
    }

    /** A number without an exponent or trailing zeros, such as "7.5", "8" or "0.0001". */
    fun number(value: Double): String =
        if (value == 0.0) "0" else BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}
