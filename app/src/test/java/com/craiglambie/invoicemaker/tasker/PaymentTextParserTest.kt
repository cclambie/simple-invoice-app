package com.craiglambie.invoicemaker.tasker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentTextParserTest {
    @Test
    fun parsesUpNotificationAndIgnoresBalance() {
        val p = PaymentTextParser.parse("Money received\nThird Street Pty sent you \$453.75 \"CraigL0010\" Available:\n\$1,500.97")!!
        assertEquals(453.75, p.amount, 0.0)
        assertEquals("Third Street Pty", p.clientName)
        assertEquals("CraigL0010", p.reference)
    }

    @Test
    fun parsesThousandsSeparator() {
        val p = PaymentTextParser.parse("John Smith sent you \$1,330.00 \"Inv001\"")!!
        assertEquals(1330.0, p.amount, 0.0)
    }

    @Test
    fun unknownTextReturnsNull() {
        assertNull(PaymentTextParser.parse("Your card was used at Woolworths"))
    }

    @Test
    fun customPatternHandlesOtherBanks() {
        val pattern = """You received \$(?<amount>[\d,]+(?:\.\d{1,2})?) from (?<client>.+?) ref (?<reference>\S+)"""
        val p = PaymentTextParser.parse("You received \$80.00 from Jane Doe ref INV7", pattern)!!
        assertEquals(80.0, p.amount, 0.0)
        assertEquals("Jane Doe", p.clientName)
        assertEquals("INV7", p.reference)
    }

    @Test
    fun badCustomPatternFallsBackToBuiltIn() {
        val p = PaymentTextParser.parse("John Smith sent you \$5.00 \"Inv1\"", "(unclosed")!!
        assertEquals("Inv1", p.reference)
    }
}
