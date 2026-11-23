package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminAlertItem
import com.example.data.model.AdminRole
import com.example.data.model.AdminSection
import com.example.data.model.CustomerAdminSummary
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownSoftLavender

@Composable
fun AdminNotificationsDialog(
    alerts: List<AdminAlertItem>,
    readAlertIds: Set<String>,
    role: AdminRole,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onNavigateSection: (AdminSection) -> Unit,
    onDismiss: () -> Unit,
) {
    val visibleAlerts = remember(alerts, role) {
        alerts.filter { role.canAccessSection(it.targetSection) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null)
                    Text("Admin Alerts (${visibleAlerts.size})")
                }
                if (visibleAlerts.isNotEmpty()) {
                    TextButton(onClick = onMarkAllRead) {
                        Icon(Icons.Outlined.DoneAll, contentDescription = null)
                        Text("Mark all")
                    }
                }
            }
        },
        text = {
            if (visibleAlerts.isEmpty()) {
                BoutiqueEmptyView(
                    title = "All Clear",
                    subtitle = "Real alerts for new orders, bank transfer proofs, failed payments, low stock, registrations, and reviews will appear here.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleAlerts, key = { it.id }) { alert ->
                        val isRead = alert.id in readAlertIds
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMarkRead(alert.id)
                                    onNavigateSection(alert.targetSection)
                                    onDismiss()
                                },
                            shape = MaterialTheme.shapes.small,
                            color = if (isRead) MaterialTheme.colorScheme.surfaceContainerLowest else ZeecrownSoftLavender.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        alert.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    AdminStatusPill(
                                        label = alert.category,
                                        tone = when (alert.severityTone) {
                                            "ERROR" -> PillTone.ERROR
                                            "ROSE" -> PillTone.ROSE
                                            "SUCCESS" -> PillTone.SUCCESS
                                            else -> PillTone.GOLD
                                        },
                                    )
                                }
                                Text(alert.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun AdminGlobalSearchDialog(
    role: AdminRole,
    products: List<Product>,
    orders: List<Order>,
    customers: List<CustomerAdminSummary>,
    onNavigateSection: (AdminSection) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    val matchingProducts = remember(q, products, role) {
        if (q.isBlank() || !role.canManageProducts) emptyList()
        else products.filter { it.title.lowercase().contains(q) || it.sku.lowercase().contains(q) }.take(5)
    }
    val matchingOrders = remember(q, orders, role) {
        if (q.isBlank() || !role.canManageOrders) emptyList()
        else orders.filter {
            it.orderNumber.lowercase().contains(q) ||
                it.recipientName.lowercase().contains(q) ||
                it.recipientPhone.lowercase().contains(q)
        }.take(5)
    }
    val matchingCustomers = remember(q, customers, role) {
        if (q.isBlank() || !role.canViewCustomers) emptyList()
        else customers.filter {
            it.profile.displayName.lowercase().contains(q) || it.profile.id.lowercase().contains(q)
        }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Admin Dashboard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search order #, phone, SKU, product, customer") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (q.isNotBlank()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (matchingOrders.isNotEmpty()) {
                            item { Text("Orders", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                            items(matchingOrders, key = { "so_${it.id}" }) { o ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        onNavigateSection(AdminSection.ORDERS)
                                        onDismiss()
                                    },
                                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        "${o.orderNumber} · ${o.recipientName} · ${formatNgnAmount(o.totalNgn)}",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            item { HorizontalDivider(color = ZeecrownElegantSilver) }
                        }
                        if (matchingProducts.isNotEmpty()) {
                            item { Text("Products", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                            items(matchingProducts, key = { "sp_${it.id}" }) { p ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        onNavigateSection(AdminSection.PRODUCTS)
                                        onDismiss()
                                    },
                                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        "${p.title} · SKU ${p.sku} · ${formatNgnAmount(p.priceNgn)}",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            item { HorizontalDivider(color = ZeecrownElegantSilver) }
                        }
                        if (matchingCustomers.isNotEmpty()) {
                            item { Text("Customers", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                            items(matchingCustomers, key = { "sc_${it.profile.id}" }) { c ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        onNavigateSection(AdminSection.CUSTOMERS)
                                        onDismiss()
                                    },
                                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        "${c.profile.displayName} (${c.totalOrderCount} orders)",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                        if (matchingOrders.isEmpty() && matchingProducts.isEmpty() && matchingCustomers.isEmpty()) {
                            item { Text("No matching admin records found for '$query'.", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
