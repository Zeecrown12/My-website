package com.example.ui.storefront

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.auth.AuthValidation
import com.example.data.firebase.UiState
import com.example.data.model.DeliveryAddress
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess

@Composable
fun AccountAddressesTab(
    addressesState: UiState<List<DeliveryAddress>>,
    defaultRecipientName: String,
    defaultPhone: String,
    isSubmitting: Boolean,
    onSaveAddress: (addressId: String?, address: DeliveryAddress, onDone: () -> Unit) -> Unit,
    onSetDefaultAddress: (addressId: String) -> Unit,
    onDeleteAddress: (addressId: String) -> Unit,
) {
    var editingAddress by remember { mutableStateOf<DeliveryAddress?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var deletingAddress by remember { mutableStateOf<DeliveryAddress?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Saved Delivery Addresses", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = {
                    editingAddress = DeliveryAddress(
                        label = "Home",
                        recipientName = defaultRecipientName,
                        phone = defaultPhone,
                        country = "Nigeria",
                        state = "Lagos",
                    )
                    showFormDialog = true
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_new_address_button"),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZeecrownRoyalPurple,
                    contentColor = ZeecrownPureWhite,
                ),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Address")
            }
        }

        when (addressesState) {
            is UiState.Loading -> BoutiqueLoadingView("Loading saved addresses...")
            is UiState.Error -> BoutiqueErrorBanner(addressesState.message)
            is UiState.Success -> {
                val list = addressesState.data
                if (list.isEmpty()) {
                    BoutiqueEmptyView(
                        title = "No Saved Delivery Addresses",
                        subtitle = "Add your Lagos, Abuja, nationwide Nigerian, or international delivery destination for one-tap checkout.",
                    )
                } else {
                    list.forEach { address ->
                        DeliveryAddressCard(
                            address = address,
                            isSubmitting = isSubmitting,
                            onSetDefault = { onSetDefaultAddress(address.id) },
                            onEdit = {
                                editingAddress = address
                                showFormDialog = true
                            },
                            onDelete = { deletingAddress = address },
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog && editingAddress != null) {
        DeliveryAddressFormDialog(
            initialAddress = editingAddress!!,
            isSubmitting = isSubmitting,
            onDismiss = { showFormDialog = false },
            onSave = { draft ->
                val targetId = editingAddress!!.id.takeIf { it.isNotBlank() }
                onSaveAddress(targetId, draft) {
                    showFormDialog = false
                }
            },
        )
    }

    deletingAddress?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingAddress = null },
            title = { Text("Remove Delivery Address?") },
            text = { Text("Delete '${target.label}' (${target.streetAddress}) from your saved addresses?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAddress(target.id)
                        deletingAddress = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAddress = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DeliveryAddressCard(
    address: DeliveryAddress,
    isSubmitting: Boolean,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = if (address.defaultAddress) 1.5.dp else 1.dp,
            color = if (address.defaultAddress) MaterialTheme.colorScheme.primary else ZeecrownElegantSilver,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = address.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    if (address.defaultAddress) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ZeecrownLightLilac,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = ZeecrownSageSuccess,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text("Default", style = MaterialTheme.typography.labelSmall, color = ZeecrownDeepPurple)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(
                        onClick = onEdit,
                        enabled = !isSubmitting,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    TextButton(
                        onClick = onDelete,
                        enabled = !isSubmitting,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete address",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Text("${address.recipientName} · ${address.phone}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = address.formattedSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (address.deliveryInstructions.isNotBlank()) {
                Text(
                    text = "Instructions: ${address.deliveryInstructions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!address.defaultAddress) {
                OutlinedButton(
                    onClick = onSetDefault,
                    enabled = !isSubmitting,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Set as Default")
                }
            }
        }
    }
}

@Composable
fun DeliveryAddressFormDialog(
    initialAddress: DeliveryAddress,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (DeliveryAddress) -> Unit,
) {
    var label by remember(initialAddress) { mutableStateOf(initialAddress.label.ifBlank { "Home" }) }
    var recipientName by remember(initialAddress) { mutableStateOf(initialAddress.recipientName) }
    var phone by remember(initialAddress) { mutableStateOf(initialAddress.phone) }
    var country by remember(initialAddress) { mutableStateOf(initialAddress.country.ifBlank { "Nigeria" }) }
    var state by remember(initialAddress) { mutableStateOf(initialAddress.state.ifBlank { "Lagos" }) }
    var city by remember(initialAddress) { mutableStateOf(initialAddress.city) }
    var lga by remember(initialAddress) { mutableStateOf(initialAddress.lga) }
    var streetAddress by remember(initialAddress) { mutableStateOf(initialAddress.streetAddress) }
    var landmark by remember(initialAddress) { mutableStateOf(initialAddress.landmark) }
    var instructions by remember(initialAddress) { mutableStateOf(initialAddress.deliveryInstructions) }
    var isDefault by remember(initialAddress) { mutableStateOf(initialAddress.defaultAddress) }
    var showCountryMenu by remember { mutableStateOf(false) }
    var showStateMenu by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialAddress.id.isBlank()) "Add Delivery Address" else "Edit Delivery Address")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it; errorMsg = null },
                    label = { Text("Label (e.g., Home, Office) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it; errorMsg = null },
                    label = { Text("Recipient Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    label = { Text("Recipient Phone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country *") },
                        trailingIcon = {
                            TextButton(onClick = { showCountryMenu = true }) {
                                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Choose country")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = showCountryMenu,
                        onDismissRequest = { showCountryMenu = false },
                    ) {
                        AuthValidation.SUPPORTED_COUNTRIES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    country = option
                                    if (option != "Nigeria" && state in AuthValidation.NIGERIAN_STATES) {
                                        state = ""
                                    } else if (option == "Nigeria" && state.isBlank()) {
                                        state = "Lagos"
                                    }
                                    showCountryMenu = false
                                },
                            )
                        }
                    }
                }

                if (country.equals("Nigeria", ignoreCase = true)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it; errorMsg = null },
                            label = { Text("Nigerian State *") },
                            trailingIcon = {
                                TextButton(onClick = { showStateMenu = true }) {
                                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Choose state")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DropdownMenu(
                            expanded = showStateMenu,
                            onDismissRequest = { showStateMenu = false },
                        ) {
                            AuthValidation.NIGERIAN_STATES.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st) },
                                    onClick = {
                                        state = st
                                        showStateMenu = false
                                    },
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it; errorMsg = null },
                        label = { Text("State / Province / Region *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it; errorMsg = null },
                        label = { Text("City / Area *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = lga,
                        onValueChange = { lga = it },
                        label = { Text("LGA (Optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = streetAddress,
                    onValueChange = { streetAddress = it; errorMsg = null },
                    label = { Text("Street Address *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Nearest Landmark (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Delivery Instructions (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                    )
                    Text("Set as default delivery address", style = MaterialTheme.typography.bodySmall)
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val err = AuthValidation.validateDeliveryAddress(
                        label = label,
                        recipientName = recipientName,
                        phone = phone,
                        country = country,
                        state = state,
                        city = city,
                        streetAddress = streetAddress,
                    )
                    if (err != null) {
                        errorMsg = err
                        return@Button
                    }
                    onSave(
                        initialAddress.copy(
                            label = label.trim(),
                            recipientName = recipientName.trim(),
                            phone = AuthValidation.normalizePhone(phone),
                            country = country.trim(),
                            state = state.trim(),
                            city = city.trim(),
                            lga = lga.trim(),
                            streetAddress = streetAddress.trim(),
                            landmark = landmark.trim(),
                            deliveryInstructions = instructions.trim(),
                            defaultAddress = isDefault,
                        )
                    )
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZeecrownRoyalPurple,
                    contentColor = ZeecrownPureWhite,
                ),
            ) {
                Text(if (isSubmitting) "Saving..." else "Save Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
