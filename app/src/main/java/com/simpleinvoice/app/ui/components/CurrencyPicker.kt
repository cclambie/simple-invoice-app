package com.simpleinvoice.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.simpleinvoice.app.util.commonCurrencyCodes
import com.simpleinvoice.app.util.currencyLabel

/**
 * Dropdown for choosing a currency. When [noneLabel] is given, an extra first option selects
 * `null` (e.g. "Use business default").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPicker(
    label: String,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val codes = (listOfNotNull(selected) + commonCurrencyCodes).distinct()
        .sortedBy { commonCurrencyCodes.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let(::currencyLabel) ?: noneLabel.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (noneLabel != null) {
                DropdownMenuItem(text = { Text(noneLabel) }, onClick = { onSelect(null); expanded = false })
            }
            codes.forEach { code ->
                DropdownMenuItem(text = { Text(currencyLabel(code)) }, onClick = { onSelect(code); expanded = false })
            }
        }
    }
}
