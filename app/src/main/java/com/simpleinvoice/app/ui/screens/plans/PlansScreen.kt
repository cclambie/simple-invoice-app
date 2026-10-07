package com.simpleinvoice.app.ui.screens.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simpleinvoice.app.BuildConfig
import com.simpleinvoice.app.SimpleInvoiceApp
import com.simpleinvoice.app.plan.AdRules
import com.simpleinvoice.app.plan.Tier

private val tierFeatures = mapOf(
    Tier.FREE to listOf(
        "Unlimited invoices",
        "Small footer banner",
        "Pop-up ads after ${AdRules.NO_POPUPS_UP_TO} invoices a month",
        "Manual backup & restore (after an ad)"
    ),
    Tier.PREMIUM to listOf(
        "No pop-up ads",
        "Export reports to CSV, 1 month at a time",
        "Manual backup & restore, no ads"
    ),
    Tier.PREMIUM_PLUS to listOf(
        "No ads at all",
        "Export reports for any period",
        "Automatic backups"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as SimpleInvoiceApp
    val plans = app.planRepository
    val tier by plans.tier.collectAsStateWithLifecycle()
    val override by plans.invoiceCountOverride.collectAsStateWithLifecycle()
    var invoicesThisMonth by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(override) { invoicesThisMonth = plans.invoicesThisMonth() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Unlimited invoices on every plan. Pay only if you'd rather not see ads.")
            invoicesThisMonth?.let {
                Text("Invoices this month: $it", style = MaterialTheme.typography.bodyMedium)
            }

            Tier.entries.forEach { option ->
                TierCard(option, isCurrent = option == tier)
            }

            if (BuildConfig.DEBUG) {
                Divider()
                Text("Developer options (debug builds only)", style = MaterialTheme.typography.titleMedium)
                Text("Plan")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tier.entries.forEach { option ->
                        FilterChip(selected = tier == option, onClick = { plans.setTier(option) }, label = { Text(option.label) })
                    }
                }
                Text("Pretend invoices this month")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null, 2, 5, 9).forEach { count ->
                        FilterChip(
                            selected = override == count,
                            onClick = { plans.setInvoiceCountOverride(count) },
                            label = { Text(count?.toString() ?: "Real") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierCard(tier: Tier, isCurrent: Boolean) {
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tier.label, style = MaterialTheme.typography.titleLarge)
                Text(tier.priceLabel, style = MaterialTheme.typography.titleMedium)
            }
            tierFeatures[tier].orEmpty().forEach { Text("• $it") }
            if (isCurrent) {
                Text("Your current plan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else if (tier != Tier.FREE) {
                // Google Play Billing isn't wired up yet.
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Subscribe (coming soon)") }
            }
        }
    }
    if (isCurrent) {
        OutlinedCard(modifier = Modifier.fillMaxWidth(), border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) { content() }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) { content() }
    }
}
