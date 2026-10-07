package com.simpleinvoice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.simpleinvoice.app.ui.nav.SimpleInvoiceNavHost
import com.simpleinvoice.app.ui.theme.SimpleInvoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleInvoiceTheme {
                SimpleInvoiceNavHost()
            }
        }
    }
}
