package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.Canvas
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.AdminAnalyticsReport
import com.example.data.model.AdminRole
import com.example.data.model.AdminSection
import com.example.data.model.AnalyticsDateRange
import com.example.data.model.CustomerAdminSummary
import com.example.data.model.InventoryRecord
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender

@Composable
fun AdminOverviewTab(
    role: AdminRole,
    report: AdminAnalyticsReport,
    selectedRange: AnalyticsDateRange,
    allOrdersState: UiState<List<Order>>,
    allProductsState: UiState<List<Product>>,
    inventoryState: UiState<Map<String, InventoryRecord>>,
    customersState: UiState<List<CustomerAdminSummary>>,
    isBusy: Boolean,
    onSelectDateRange: (AnalyticsDateRange, Long?, Long?) -> Unit,
    onSeedFoundationStore: () -> Unit,
    onOpenSection: (AdminSection) -> Unit,
) {
    val orders = (allOrdersState as? UiState.Success)?.data.orEmpty()
    val products = (allProductsState as? UiState.Success)?.data.orEmpty()
    val inventory = (inventoryState as? UiState.Success)?.data.orEmpty()
    val customers = (customersState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AdminDateRangeSelectorRow(
                selectedRange = selectedRange,
                customLabel = report.dateRangeLabel,
                onSelectRange = onSelectDateRange,
            )
        }

        // Empty store starter CTA if catalog has not been populated yet
        if (allProductsState is UiState.Success && products.isEmpty()) {
            item {
                AdminSectionCard(containerColor = ZeecrownSoftLavender.copy(alpha = 0.45f)) {
                    Text("Initialize ZEECROWN Catalog", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Your live Firestore catalog is empty. Seed the 8 fashion categories, starter products, collections, delivery zones, promo code, and editorial banners with one click.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onSeedFoundationStore,
                        enabled = !isBusy,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) {
                        Text("Seed ZEECROWN Foundation Store")
                    }
                }
            }
        }

        // Primary KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdminMetricCard(
                        title = "Net Verified Revenue",
                        value = formatNgnAmount(report.netPaidRevenueNgn),
                        subtitle = "Verified ${formatNgnAmount(report.verifiedPaidRevenueNgn)} · Refunded ${formatNgnAmount(report.refundedTotalNgn)}",
                        accentColor = ZeecrownSageSuccess,
                        containerColor = ZeecrownLightLilac,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_net_revenue"),
                    )
                    AdminMetricCard(
                        title = "Gross Order Value",
                        value = formatNgnAmount(report.grossOrderValueNgn),
                        subtitle = "Unpaid: ${formatNgnAmount(report.unpaidOrdersValueNgn)} · AOV ${formatNgnAmount(report.averageOrderValueNgn)}",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_gross_value"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdminMetricCard(
                        title = "Total Orders (${report.dateRangeLabel})",
                        value = report.totalOrders.toString(),
                        subtitle = "${report.processingOrders} active · ${report.completedDeliveredOrders} delivered · ${report.cancelledOrders} cancelled",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_orders"),
                    )
                    AdminMetricCard(
                        title = "Pending Payments",
                        value = "${report.pendingPaymentOrders}",
                        subtitle = "${report.pendingVerificationPayments} transfer proof(s) awaiting review",
                        accentColor = if (report.pendingVerificationPayments > 0) MaterialTheme.colorScheme.error else ZeecrownDeepPurple,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdminMetricCard(
                        title = "Stock Alerts",
                        value = "${report.lowStockCount + report.outOfStockCount}",
                        subtitle = "${report.outOfStockCount} out of stock · ${report.lowStockCount} low",
                        accentColor = if (report.outOfStockCount > 0) MaterialTheme.colorScheme.error else ZeecrownDeepPurple,
                        modifier = Modifier.weight(1f),
                    )
                    AdminMetricCard(
                        title = "Catalog & Customers",
                        value = "${report.publishedProductsCount} / ${report.totalCustomers}",
                        subtitle = "${products.size} products · +${report.newCustomersInRange} new shoppers",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Sales & Orders Over Time Chart
        item {
            var showOrdersChart by remember { mutableStateOf(false) }
            AdminSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (showOrdersChart) "Orders Over Time" else "Verified Revenue Trend",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = !showOrdersChart, onClick = { showOrdersChart = false }, label = { Text("Revenue") })
                        FilterChip(selected = showOrdersChart, onClick = { showOrdersChart = true }, label = { Text("Orders") })
                    }
                }
                val series: List<Pair<String, Double>> = if (showOrdersChart) {
                    report.dailyOrderCounts.map { it.first to it.second.toDouble() }
                } else {
                    report.dailyRevenueBuckets
                }
                if (series.isEmpty()) {
                    Text(
                        "When customer orders arrive in '${report.dateRangeLabel}', daily revenue and order volume bars will chart here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val maxVal = series.maxOf { it.second }.coerceAtLeast(1.0)
                    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        val slot = size.width / series.size.coerceAtLeast(1)
                        val barW = (slot * 0.58f).coerceAtLeast(10f)
                        series.forEachIndexed { idx, (_, v) ->
                            val ratio = (v / maxVal).toFloat().coerceIn(0.05f, 1f)
                            val h = size.height * ratio
                            drawRoundRect(
                                color = if (showOrdersChart) ZeecrownMetallicGold else ZeecrownRoyalPurple,
                                topLeft = Offset(idx * slot + (slot - barW) / 2f, size.height - h),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(6f, 6f),
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        series.takeLast(6).forEach { (day, v) ->
                            Text(
                                if (showOrdersChart) "$day (${v.toInt()})" else "$day · ${formatNgnAmount(v)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZeecrownDeepPurple,
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Strip filtered by role permissions
        item {
            AdminSectionCard {
                Text("Quick Store Actions", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (role.canVerifyPayments) {
                        Button(
                            onClick = { onOpenSection(AdminSection.PAYMENTS) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                        ) {
                            Text("Verify Transfers (${report.pendingVerificationPayments})", maxLines = 1)
                        }
                    }
                    if (role.canManageProducts) {
                        OutlinedButton(
                            onClick = { onOpenSection(AdminSection.PRODUCTS) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize(),
                        ) {
                            Text("Add / Edit Product", maxLines = 1)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (role.canManageInventory) {
                        OutlinedButton(
                            onClick = { onOpenSection(AdminSection.INVENTORY) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize(),
                        ) {
                            Text("Low Stock (${report.lowStockCount + report.outOfStockCount})", maxLines = 1)
                        }
                    }
                    if (role.canViewReports) {
                        OutlinedButton(
                            onClick = { onOpenSection(AdminSection.REPORTS) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize(),
                        ) {
                            Text("Export CSV Report", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Recent Orders
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent Orders", style = MaterialTheme.typography.titleLarge)
                if (role.canManageOrders) {
                    TextButton(onClick = { onOpenSection(AdminSection.ORDERS) }) { Text("View All") }
                }
            }
        }

        when (allOrdersState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading live orders...") }
            is UiState.Error -> item { BoutiqueErrorBanner(allOrdersState.message) }
            is UiState.Success -> {
                val recentOrders = orders.take(5)
                if (recentOrders.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Orders Recorded Yet",
                            subtitle = "Real orders placed by ZEECROWN customers will appear here immediately.",
                        )
                    }
                } else {
                    items(recentOrders, key = { it.id }) { order ->
                        val st = OrderStatus.fromRaw(order.status)
                        val ps = PaymentStatus.fromRaw(order.paymentStatus)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "${order.orderNumber} · ${order.recipientName.ifBlank { "Customer" }}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "${order.shippingCity}, ${order.shippingState} · ${formatAdminTimestamp(order.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(formatNgnAmount(order.totalNgn), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    AdminStatusPill(
                                        label = "${st.label} · ${ps.label}",
                                        tone = when (ps) {
                                            PaymentStatus.PAID -> PillTone.SUCCESS
                                            PaymentStatus.PENDING_VERIFICATION -> PillTone.ROSE
                                            PaymentStatus.FAILED, PaymentStatus.REFUNDED -> PillTone.ERROR
                                            else -> PillTone.GOLD
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Low-stock items alert card
        val alertItems = products.filter { p ->
            val rec = inventory[p.id]
            rec != null && rec.stockQuantity <= rec.lowStockThreshold.coerceAtLeast(p.lowStockThreshold)
        }.take(6)

        if (alertItems.isNotEmpty()) {
            item {
                AdminSectionCard {
                    Text("Low & Out-of-Stock Watchlist", style = MaterialTheme.typography.titleMedium)
                    alertItems.forEach { prod ->
                        val qty = inventory[prod.id]?.stockQuantity ?: 0L
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(prod.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            AdminStatusPill(
                                label = if (qty <= 0L) "OUT OF STOCK" else "$qty left",
                                tone = if (qty <= 0L) PillTone.ERROR else PillTone.ROSE,
                            )
                        }
                        HorizontalDivider(color = ZeecrownElegantSilver)
                    }
                }
            }
        }

        // Best sellers & Recent registrations summary
        item {
            AdminSectionCard {
                Text("Best-Selling Items & Recent Customers", style = MaterialTheme.typography.titleMedium)
                if (report.bestSellingProducts.isEmpty()) {
                    Text("No product data yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    report.bestSellingProducts.take(4).forEach { prod ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(prod.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                "${prod.soldCount} sold · ${formatNgnAmount(prod.priceNgn)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ZeecrownGoldInk,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = ZeecrownElegantSilver)
                Text("Latest Registered Customers (${customers.size})", style = MaterialTheme.typography.labelLarge)
                if (customers.isEmpty()) {
                    Text("No customer profiles yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    customers.take(4).forEach { cust ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(cust.profile.displayName.ifBlank { cust.profile.id }, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${cust.paidOrderCount} paid · ${formatNgnAmount(cust.lifetimeSpendPaidNgn)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZeecrownDeepPurple,
                            )
                        }
                    }
                }
            }
        }
    }
}
