package com.jacobrozell.puzzlebuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.catalog.MediumDateLabel
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Grouped form section with a header and optional footer, like an iOS `Form` `Section`. */
@Composable
fun FormSection(
    header: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val brand = BrandTokens.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        header?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = brand.textSecondary,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .semantics { heading() },
            )
        }
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = brand.card),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
        footer?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = brand.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Read-only dropdown field. Uses the exposed dropdown so TalkBack announces it as a menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PickerField(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = labelFor(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelFor(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/**
 * Date row that opens the Material date picker. Keeps the stored time of day and changes only
 * the calendar date, as the iOS date pickers do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: Instant,
    onValueChange: (Instant) -> Unit,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = MediumDateLabel.format(value, zoneId),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Choose $label date")
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
    if (open) {
        val local = value.atZone(zoneId)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(withDate(value, day, zoneId))
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun withDate(value: Instant, day: LocalDate, zoneId: ZoneId): Instant =
    value.atZone(zoneId).with(day).toInstant()
