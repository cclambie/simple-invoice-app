package com.simpleinvoice.app.plan

/** Subscription level. Everything a tier unlocks is decided here so the rules live in one place. */
enum class Tier(val label: String, val priceLabel: String) {
    FREE("Free", "Ad supported"),
    PREMIUM("Premium", "$1 / month"),
    PREMIUM_PLUS("Premium Plus", "$2 / month");

    /** The small footer banner. Only Premium Plus removes it. */
    val showsBanner: Boolean get() = this != PREMIUM_PLUS

    /** Pop-up (interstitial) ads, subject to [AdRules]. Any paid tier removes them. */
    val showsPopupAds: Boolean get() = this == FREE

    val canExportCsv: Boolean get() = this != FREE

    /** Premium exports at most one month per CSV; Premium Plus any range. */
    val csvExportUnlimited: Boolean get() = this == PREMIUM_PLUS

    val canAutoBackup: Boolean get() = this == PREMIUM_PLUS
}

/** When free users see pop-up ads, by the number of invoices issued this calendar month. */
object AdRules {
    /** Up to this many invoices a month: footer banner only. */
    const val NO_POPUPS_UP_TO = 3

    /** Above this many invoices a month: a pop-up on app open as well. */
    const val APP_OPEN_POPUP_ABOVE = 7

    /** Pop-up when sending or saving an invoice PDF. */
    fun popupOnInvoiceExport(tier: Tier, invoicesThisMonth: Int): Boolean =
        tier.showsPopupAds && invoicesThisMonth > NO_POPUPS_UP_TO

    fun popupOnAppOpen(tier: Tier, invoicesThisMonth: Int): Boolean =
        tier.showsPopupAds && invoicesThisMonth > APP_OPEN_POPUP_ABOVE

    /** Free users watch a pop-up before every manual backup or restore. */
    fun popupOnBackup(tier: Tier): Boolean = tier.showsPopupAds
}
