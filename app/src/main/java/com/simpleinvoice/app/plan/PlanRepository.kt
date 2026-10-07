package com.simpleinvoice.app.plan

import android.content.Context
import com.simpleinvoice.app.BuildConfig
import com.simpleinvoice.app.repository.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The user's current [Tier]. Until Google Play Billing is added, the tier is a stored setting
 * that can only be changed from the developer options on the Plans screen (debug builds).
 */
class PlanRepository(context: Context, private val invoiceRepository: InvoiceRepository) {
    private val prefs = context.getSharedPreferences("plan", Context.MODE_PRIVATE)

    private val _tier = MutableStateFlow(readTier())
    val tier: StateFlow<Tier> = _tier.asStateFlow()

    private val _invoiceCountOverride = MutableStateFlow(readOverride())

    /** Debug builds only: pretend this many invoices were issued this month, to test ad levels. */
    val invoiceCountOverride: StateFlow<Int?> = _invoiceCountOverride.asStateFlow()

    fun setTier(tier: Tier) {
        prefs.edit().putString(KEY_TIER, tier.name).apply()
        _tier.value = tier
    }

    fun setInvoiceCountOverride(count: Int?) {
        prefs.edit().apply { if (count == null) remove(KEY_OVERRIDE) else putInt(KEY_OVERRIDE, count) }.apply()
        _invoiceCountOverride.value = readOverride()
    }

    suspend fun invoicesThisMonth(): Int =
        _invoiceCountOverride.value ?: invoiceRepository.countIssuedThisMonth()

    private fun readTier(): Tier =
        prefs.getString(KEY_TIER, null)?.let { runCatching { Tier.valueOf(it) }.getOrNull() } ?: Tier.FREE

    private fun readOverride(): Int? =
        if (BuildConfig.DEBUG && prefs.contains(KEY_OVERRIDE)) prefs.getInt(KEY_OVERRIDE, 0) else null

    private companion object {
        const val KEY_TIER = "tier"
        const val KEY_OVERRIDE = "debug_invoice_count"
    }
}
