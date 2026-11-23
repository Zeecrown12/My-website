package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.DeliverySettings
import com.example.data.model.DeliveryZone
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

@Composable
fun AdminDeliveryTab(
    zonesState: UiState<List<DeliveryZone>>,
    deliverySettingsState: UiState<DeliverySettings>,
    isBusy: Boolean,
    onSaveDeliverySettings: (DeliverySettings) -> Unit,
    onSaveZone: (zoneId: String?, zone: DeliveryZone) -> Unit,
    onDeleteZone: (DeliveryZone) -> Unit,
) {
    val zones = (zonesState as? UiState.Success)?.data.orEmpty()
    val currentSettings = (deliverySettingsState as? UiState.Success)?.data ?: DeliverySettings()

    var freeThresholdText by remember { mutableStateOf("") }
    var flatFeeText by remember { mutableStateOf("") }
    var pickupEnabled by remember { mutableStateOf(false) }
    var pickupAddress by remember { mutableStateOf("") }
    var pickupHours by remember { mutableStateOf("") }
    var globalPromoNote by remember { mutableStateOf("") }

    LaunchedEffect(currentSettings) {
        freeThresholdText = currentSettings.freeDeliveryThresholdNgn.toLong().toString()
        flatFeeText = currentSettings.nationwideFlatFeeNgn?.toLong()?.toString().orEmpty()
        pickupEnabled = currentSettings.pickupEnabled
        pickupAddress = currentSettings.pickupAddress
        pickupHours = currentSettings.pickupHours
        globalPromoNote = currentSettings.specialPromoNote
    }

    var editingZoneId by remember { mutableStateOf<String?>(null) }
    var zoneName by remember { mutableStateOf("") }
    var zoneCountry by remember { mutableStateOf("Nigeria") }
    var zoneStates by remember { mutableStateOf("Lagos") }
    var zoneCities by remember { mutableStateOf("Ikoyi, Victoria Island, Lekki") }
    var zoneLgas by remember { mutableStateOf("") }
    var zoneFeeText by remember { mutableStateOf("3500") }
    var zoneEta by remember { mutableStateOf("1 - 3 business days") }
    var zonePromoNote by remember { mutableStateOf("") }
    var zoneActive by remember { mutableStateOf(true) }

    fun loadZone(z: DeliveryZone) {
        editingZoneId = z.id
        zoneName = z.name
        zoneCountry = z.country
        zoneStates = z.states.joinToString(", ")
        zoneCities = z.cities.joinToString(", ")
        zoneLgas = z.lgas.joinToString(", ")
        zoneFeeText = z.feeNgn.toLong().toString()
        zoneEta = z.estimatedDays
        zonePromoNote = z.promotionNote
        zoneActive = z.active
    }

    fun resetZone() {
        editingZoneId = null
        zoneName = ""
        zoneCountry = "Nigeria"
        zoneStates = ""
        zoneCities = ""
        zoneLgas = ""
        zoneFeeText = "3500"
        zoneEta = "1 - 3 business days"
        zonePromoNote = ""
        zoneActive = true
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminSectionCard {
                Text("Global Shipping & Store Pickup Rules", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = freeThresholdText,
                        onValueChange = { freeThresholdText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Free Delivery Threshold (NGN)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = flatFeeText,
                        onValueChange = { flatFeeText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Flat Fee Override (Optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable In-Store Pickup")
                    Switch(checked = pickupEnabled, onCheckedChange = { pickupEnabled = it })
                }
                if (pickupEnabled) {
                    OutlinedTextField(
                        value = pickupAddress,
                        onValueChange = { pickupAddress = it },
                        label = { Text("Pickup Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = pickupHours,
                        onValueChange = { pickupHours = it },
                        label = { Text("Pickup Opening Hours") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = globalPromoNote,
                    onValueChange = { globalPromoNote = it },
                    label = { Text("Special Delivery Announcement") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        onSaveDeliverySettings(
                            DeliverySettings(
                                freeDeliveryThresholdNgn = freeThresholdText.toDoubleOrNull() ?: 0.0,
                                nationwideFlatFeeNgn = flatFeeText.toDoubleOrNull(),
                                pickupEnabled = pickupEnabled,
                                pickupAddress = pickupAddress,
                                pickupHours = pickupHours,
                                specialPromoNote = globalPromoNote,
                            )
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Save Global Delivery Rules") }
            }
        }

        item {
            AdminSectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (editingZoneId == null) "Add Delivery Zone" else "Edit Delivery Zone", style = MaterialTheme.typography.titleLarge)
                    if (editingZoneId != null) {
                        TextButton(onClick = ::resetZone) { Text("New Zone") }
                    }
                }
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text("Zone Name * (e.g. Lagos Island Express)") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_zone_name_input"),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = zoneCountry, onValueChange = { zoneCountry = it }, label = { Text("Country") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = zoneFeeText, onValueChange = { zoneFeeText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Fee NGN *") }, modifier = Modifier.weight(0.8f), singleLine = true)
                }
                OutlinedTextField(value = zoneStates, onValueChange = { zoneStates = it }, label = { Text("States (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = zoneCities, onValueChange = { zoneCities = it }, label = { Text("Cities (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = zoneLgas, onValueChange = { zoneLgas = it }, label = { Text("LGAs (comma-separated, optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = zoneEta, onValueChange = { zoneEta = it }, label = { Text("Estimated Delivery Window") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = zonePromoNote, onValueChange = { zonePromoNote = it }, label = { Text("Zone Promo / Note (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Zone Active")
                    Switch(checked = zoneActive, onCheckedChange = { zoneActive = it })
                }
                Button(
                    onClick = {
                        onSaveZone(
                            editingZoneId,
                            DeliveryZone(
                                name = zoneName.trim(),
                                country = zoneCountry.trim().ifBlank { "Nigeria" },
                                states = zoneStates.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                cities = zoneCities.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                lgas = zoneLgas.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                feeNgn = zoneFeeText.toDoubleOrNull() ?: 0.0,
                                estimatedDays = zoneEta.trim(),
                                promotionNote = zonePromoNote.trim(),
                                active = zoneActive,
                            )
                        )
                        resetZone()
                    },
                    enabled = !isBusy && zoneName.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text(if (editingZoneId == null) "Save Delivery Zone" else "Update Zone") }
            }
        }

        item { Text("Configured Delivery Zones (${zones.size})", style = MaterialTheme.typography.titleLarge) }

        when (zonesState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading delivery zones...") }
            is UiState.Error -> item { BoutiqueErrorBanner(zonesState.message) }
            is UiState.Success -> {
                if (zones.isEmpty()) {
                    item { BoutiqueEmptyView(title = "No Delivery Zones", subtitle = "Add Lagos, Nigerian states, or international DHL shipping zones above.") }
                } else {
                    items(zones, key = { it.id }) { zone ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_zone_row_${zone.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${zone.name} · ${formatNgnAmount(zone.feeNgn)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${zone.country} · ETA: ${zone.estimatedDays}", style = MaterialTheme.typography.bodySmall)
                                    if (zone.states.isNotEmpty() || zone.cities.isNotEmpty()) {
                                        Text(
                                            (zone.states + zone.cities).joinToString(", "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ZeecrownGoldInk,
                                        )
                                    }
                                    AdminStatusPill(label = if (zone.active) "ACTIVE" else "HIDDEN", tone = if (zone.active) PillTone.SUCCESS else PillTone.ROSE)
                                }
                                Row {
                                    IconButton(onClick = { loadZone(zone) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit ${zone.name}", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeleteZone(zone) }, enabled = !isBusy, modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${zone.name}", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
