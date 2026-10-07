package com.simpleinvoice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.simpleinvoice.app.plan.AdRules
import com.simpleinvoice.app.ui.nav.SimpleInvoiceNavHost
import com.simpleinvoice.app.ui.theme.SimpleInvoiceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleInvoiceTheme {
                SimpleInvoiceNavHost()
            }
        }
        // Only on a fresh launch, not when the activity is recreated (e.g. rotation).
        if (savedInstanceState == null) showAppOpenAdIfDue()
    }

    private fun showAppOpenAdIfDue() {
        val app = application as SimpleInvoiceApp
        lifecycleScope.launch {
            if (AdRules.popupOnAppOpen(app.planRepository.tier.value, app.planRepository.invoicesThisMonth())) {
                app.adManager.showInterstitialWhenReady(this@MainActivity)
            }
        }
    }
}
