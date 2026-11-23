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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.model.InventoryRecord
import com.example.data.model.InventoryTransaction
import com.example.data.model.Product
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

private enum class InventoryFilter(val label: String) {
    ALL("All Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock"),
}

@Composable
fun AdminInventoryTab(
    productsState: UiState<List<Product>>,
    inventoryState: UiState<Map<String, InventoryRecord>>,
    transactionsState: UiState<List<InventoryTransaction>>,
    isBusy: Boolean,
    onAdjustInventory: (
        product: Product,
        currentRecord: InventoryRecord?,
        newTotalStock: Long,
        newThreshold: Long,
        variantKey: String,
        newVariantQty: Long?,
        reason: String,
    ) -> Unit,
) {
    val products = (productsState as? UiState.Success)?.data.orEmpty()
    val inventoryMap = (inventoryState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(InventoryFilter.ALL) }
    var productToAdjust by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(products, inventoryMap, searchQuery, filter) {
        val q = searchQuery.trim().lowercase()
        products.filter { p ->
            val rec = inventoryMap[p.id]
            val qty = rec?.stockQuantity ?: 0L
            val threshold = rec?.lowStockThreshold ?: p.lowStockThreshold
            val matchesQuery = q.isBlank() || p.title.lowercase().contains(q) || p.sku.lowercase().contains(q)
            val matchesFilter = when (filter) {
                InventoryFilter.ALL -> true
                InventoryFilter.LOW_STOCK -> qty in 1L..threshold
                InventoryFilter.OUT_OF_STOCK -> qty <= 0L
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Inventory & Stock Ledger", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search SKU or Product Title") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(InventoryFilter.entries) { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }
            }
        }

        when (inventoryState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading live inventory...") }
            is UiState.Error -> item { BoutiqueErrorBanner(inventoryState.message) }
            is UiState.Success -> {
                if (filteredProducts.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Inventory Records Match",
                            subtitle = "Adjust filters or create products first.",
                        )
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { prod ->
                        val rec = inventoryMap[prod.id]
                        val qty = rec?.stockQuantity ?: 0L
                        val threshold = rec?.lowStockThreshold ?: prod.lowStockThreshold
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_inventory_row_${prod.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(prod.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "SKU: ${rec?.sku?.ifBlank { prod.sku } ?: prod.sku} · Alert Threshold: $threshold",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (rec != null && rec.variantStock.isNotEmpty()) {
                                        Text(
                                            text = "Variants: ${rec.variantStock.entries.joinToString { "${it.key}=${it.value}" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    AdminStatusPill(
                                        label = when {
                                            qty <= 0L -> "OUT OF STOCK (0)"
                                            qty <= threshold -> "LOW STOCK ($qty)"
                                            else -> "IN STOCK ($qty)"
                                        },
                                        tone = when {
                                            qty <= 0L -> PillTone.ERROR
                                            qty <= threshold -> PillTone.ROSE
                                            else -> PillTone.SUCCESS
                                        },
                                    )
                                }
                                OutlinedButton(
                                    onClick = { productToAdjust = prod },
                                    enabled = !isBusy,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Text("Adjust")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Recent Stock Movement History (${transactions.size})", style = MaterialTheme.typography.titleMedium)
        }

        when (transactionsState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading inventory history...") }
            is UiState.Error -> item { BoutiqueErrorBanner(transactionsState.message) }
            is UiState.Success -> {
                if (transactions.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Stock Adjustments Yet",
                            subtitle = "Every manual restock or variant update records an immutable entry here.",
                        )
                    }
                } else {
                    items(transactions.take(25), key = { it.id }) { tx ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tx.productTitle, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                    val deltaStr = if (tx.deltaQty >= 0) "+${tx.deltaQty}" else tx.deltaQty.toString()
                                    AdminStatusPill(
                                        label = "${tx.previousQty} -> ${tx.newQty} ($deltaStr)",
                                        tone = if (tx.deltaQty >= 0) PillTone.SUCCESS else PillTone.ROSE,
                                    )
                                }
                                Text(
                                    text = "${tx.reason}${if (tx.variation.isNotBlank()) " · Variant: ${tx.variation}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "By ${tx.adminEmail.ifBlank { tx.adminUid }} · ${formatAdminTimestamp(tx.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    productToAdjust?.let { prod ->
        val rec = inventoryMap[prod.id]
        var newQtyText by remember(prod.id) { mutableStateOf((rec?.stockQuantity ?: 0L).toString()) }
        var newThreshText by remember(prod.id) { mutableStateOf((rec?.lowStockThreshold ?: prod.lowStockThreshold).toString()) }
        var variantKey by remember(prod.id) { mutableStateOf("") }
        var variantQtyText by remember(prod.id) { mutableStateOf("") }
        var reason by remember(prod.id) { mutableStateOf("Restock delivery") }

        AlertDialog(
            onDismissRequest = { productToAdjust = null },
            title = { Text("Adjust Stock · ${prod.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newQtyText,
                        onValueChange = { newQtyText = it.filter { c -> c.isDigit() } },
                        label = { Text("Total Stock Quantity *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newThreshText,
                        onValueChange = { newThreshText = it.filter { c -> c.isDigit() } },
                        label = { Text("Low-Stock Threshold") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HorizontalDivider(color = ZeecrownElegantSilver)
                    OutlinedTextField(
                        value = variantKey,
                        onValueChange = { variantKey = it },
                        label = { Text("Specific Variant (Optional, e.g. 54 • Rose)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = variantQtyText,
                        onValueChange = { variantQtyText = it.filter { c -> c.isDigit() } },
                        label = { Text("Variant Qty (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Audit Reason *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdjustInventory(
                            prod,
                            rec,
                            newQtyText.toLongOrNull() ?: 0L,
                            newThreshText.toLongOrNull() ?: 3L,
                            variantKey.trim(),
                            variantQtyText.toLongOrNull(),
                            reason,
                        )
                        productToAdjust = null
                    },
                    enabled = reason.isNotBlank() && newQtyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Save Stock") }
            },
            dismissButton = { TextButton(onClick = { productToAdjust = null }) { Text("Cancel") } },
        )
    }
}
