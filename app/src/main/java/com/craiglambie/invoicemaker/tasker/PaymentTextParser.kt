package com.craiglambie.invoicemaker.tasker

/** Pulls payment details out of a bank notification's text. */
object PaymentTextParser {
    data class Parsed(val amount: Double, val clientName: String, val reference: String)

    private class BankPattern(val name: String, val regex: Regex)

    // Add a bank by appending a pattern with the named groups client, amount and reference.
    private val patterns = listOf(
        // Up: Third Street Pty sent you $453.75 "CraigL0010" Available: $1,500.97
        BankPattern("Up", Regex("""(?:Money received )?(?<client>.+?) sent you \$(?<amount>[\d,]+(?:\.\d{1,2})?) "(?<reference>[^"]*)"""")),
    )

    private const val MAX_TEXT = 2000
    private const val MAX_PATTERN = 1000

    /**
     * Reads the payment from [text]. [customPattern] is tried first: a regex with the named groups
     * client, amount and reference, as built by the Tasker recipe page on the website.
     */
    fun parse(text: String, customPattern: String? = null): Parsed? {
        val flat = text.take(MAX_TEXT).replace(Regex("""\s+"""), " ")
        val custom = customPattern?.takeIf { it.length <= MAX_PATTERN }?.let {
            try { BankPattern("custom", Regex(it, RegexOption.IGNORE_CASE)) } catch (e: IllegalArgumentException) { null }
        }
        for (p in listOfNotNull(custom) + patterns) {
            val m = p.regex.find(flat) ?: continue
            val amount = m.groups["amount"]?.value?.replace(",", "")?.toDoubleOrNull() ?: continue
            return Parsed(
                amount = amount,
                clientName = m.groups["client"]?.value?.trim().orEmpty(),
                reference = m.groups["reference"]?.value?.trim().orEmpty(),
            )
        }
        return null
    }
}
