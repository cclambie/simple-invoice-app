package com.simpleinvoice.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The Activity behind a Compose [Context], needed to show full-screen ads. */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
