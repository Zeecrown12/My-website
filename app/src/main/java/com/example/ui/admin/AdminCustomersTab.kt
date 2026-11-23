package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.AdminAnalyticsEngine
import com.example.data.model.AdminRole
import com.example.data.model.AnalyticsDateRange
import com.example.data.model.CustomerAdminSummary
import com.example.data.model.DeliveryAddress
import com.example.data.model.OrderStatus
import com.example.data.model.UserPrivateInfo
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

@Composable
fun AdminCustomersTab(
    role: AdminRole,
    customersState: UiState<List<CustomerAdminSummary>>,
    selectedCustomerDetails: Pair<UserPrivateInfo?, List<DeliveryAddress>>?,
    isLoadingDetails: Boolean,
    isBusy: Boolean,
    onLoadCustomerDetails: (String) -> Unit,
    onUpdateCustomerNotes: (userId: String, notes: String) -> Unit,
) {
    val customers = (customersState as? UiState.Success)?.data.orEmpty()
    var searchQuery by remember { mutableStateOf("") }
    var regRange by remember { mutableStateOf(AnalyticsDateRange.ALL_TIME) }
    var inspectedCustomer by remember { mutableStateOf<CustomerAdminSummary?>(null) }

    val filtered = remember(customers, searchQuery, regRange) {
        val q = searchQuery.trim().lowercase()
        val (startMs, endMs) = AdminAnalyticsEngine.computeDateBounds(regRange)
        customers.filter { c ->
            val matchesQuery = q.isBlank() ||
                c.profile.displayName.lowercase().contains(q) ||
                c.profile.id.lowercase().contains(q) ||
                c.profile.firstName.lowercase().contains(q) ||
                c.profile.lastName.lowercase().contains(q)
            val matchesDate = AdminAnalyticsEngine.isWithinBounds(c.profile.createdAt, startMs, endMs)
            matchesQuery && matchesDate
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Customer Directory (${customers.size})", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Customer Name or UID") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(AnalyticsDateRange.ALL_TIME, AnalyticsDateRange.TODAY, AnalyticsDateRange.LAST_7_DAYS, AnalyticsDateRange.LAST_30_DAYS)) { range ->
                        FilterChip(
                            selected = regRange == range,
                            onClick = { regRange = range },
                            label = { Text("Joined: ${range.label}") },
                        )
                    }
                }
            }
        }

        when (customersState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading customer directory...") }
            is UiState.Error -> item { BoutiqueErrorBanner(customersState.message) }
            is UiState.Success -> {
                if (filtered.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Customers Found",
                            subtitle = "Registered shoppers will appear here with lifetime spend and order history.",
                        )
                    }
                } else {
                    items(filtered, key = { it.profile.id }) { cust ->
                        val uid = cust.profile.userId.ifBlank { cust.profile.id }
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_customer_row_$uid"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(cust.profile.displayName.ifBlank { "Customer ${uid.take(6)}" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "Joined: ${formatAdminTimestamp(cust.profile.createdAt)} · Currency: ${cust.profile.preferredCurrency}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AdminStatusPill(
                                            label = "${cust.paidOrderCount}/${cust.totalOrderCount} Paid Orders",
                                            tone = if (cust.paidOrderCount > 0) PillTone.SUCCESS else PillTone.NEUTRAL,
                                        )
                                        AdminStatusPill(
                                            label = "Spend: ${formatNgnAmount(cust.lifetimeSpendPaidNgn)}",
                                            tone = PillTone.GOLD,
                                        )
                                        if (cust.profile.emailVerified) {
                                            AdminStatusPill(label = "Verified", tone = PillTone.SUCCESS)
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        inspectedCustomer = cust
                                        onLoadCustomerDetails(uid)
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) { Text("Profile") }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedCustomer?.let { cust ->
        val uid = cust.profile.userId.ifBlank { cust.profile.id }
        var notes by remember(uid) { mutableStateOf(cust.profile.supportNotes) }
        AlertDialog(
            onDismissRequest = { inspectedCustomer = null },
            title = { Text(cust.profile.displayName.ifBlank { "Customer Profile" }) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("User ID: $uid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Paid Spend: ${formatNgnAmount(cust.lifetimeSpendPaidNgn)} across ${cust.paidOrderCount} paid order(s)", style = MaterialTheme.typography.titleSmall)
                    HorizontalDivider(color = ZeecrownElegantSilver)

                    if (isLoadingDetails) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text("Loading private contact info & addresses...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (selectedCustomerDetails != null) {
                        val (priv, addrs) = selectedCustomerDetails
                        Text("Email: ${priv?.email?.ifBlank { "Not recorded" } ?: "Not recorded"}", style = MaterialTheme.typography.bodySmall)
                        Text("Phone: ${priv?.phone?.ifBlank { "Not recorded" } ?: "Not recorded"}", style = MaterialTheme.typography.bodySmall)
                        if (addrs.isNotEmpty()) {
                            HorizontalDivider(color = ZeecrownElegantSilver)
                            Text("Saved Addresses (${addrs.size})", style = MaterialTheme.typography.titleSmall)
                            addrs.forEach { a ->
                                Text("• ${a.label}: ${a.formattedSummary()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    HorizontalDivider(color = ZeecrownElegantSilver)
                    Text("Order History (${cust.orders.size})", style = MaterialTheme.typography.titleSmall)
                    if (cust.orders.isEmpty()) {
                        Text("No orders placed yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        cust.orders.take(10).forEach { o ->
                            Text(
                                "${o.orderNumber} · ${formatNgnAmount(o.totalNgn)} · ${OrderStatus.fromRaw(o.status).label}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    if (role.canEditCustomerNotes) {
                        HorizontalDivider(color = ZeecrownElegantSilver)
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Internal Support Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                    }
                }
            },
            confirmButton = {
                if (role.canEditCustomerNotes) {
                    Button(
                        onClick = {
                            onUpdateCustomerNotes(uid, notes)
                            inspectedCustomer = null
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) { Text("Save Support Notes") }
                }
            },
            dismissButton = { TextButton(onClick = { inspectedCustomer = null }) { Text("Close") } },
        )
    }
}
