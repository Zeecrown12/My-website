package com.example.ui.admin

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.AdminRole
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

@Composable
fun AdminOrdersTab(
    role: AdminRole,
    allOrdersState: UiState<List<Order>>,
    isBusy: Boolean,
    onUpdateFulfillment: (
        order: Order,
        newStatus: OrderStatus,
        internalNotes: String,
        cancellationDecision: String,
        courierName: String,
        trackingReference: String,
        trackingUrl: String,
        estimatedDeliveryWindow: String,
        deliveryStatus: String,
        deliveryNotes: String,
    ) -> Unit,
    onRecordRefund: (order: Order, amountNgn: Double, reason: String) -> Unit,
) {
    val context = LocalContext.current
    val orders = (allOrdersState as? UiState.Success)?.data.orEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var paymentFilter by remember { mutableStateOf<PaymentStatus?>(null) }
    var methodFilter by remember { mutableStateOf<PaymentMethod?>(null) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    val filteredOrders = remember(orders, searchQuery, statusFilter, paymentFilter, methodFilter) {
        val q = searchQuery.trim().lowercase()
        orders.filter { o ->
            val st = OrderStatus.fromRaw(o.status)
            val ps = PaymentStatus.fromRaw(o.paymentStatus)
            val pm = PaymentMethod.fromRaw(o.paymentMethod)
            val matchesQuery = q.isBlank() ||
                o.orderNumber.lowercase().contains(q) ||
                o.recipientName.lowercase().contains(q) ||
                o.recipientPhone.lowercase().contains(q) ||
                o.shippingCity.lowercase().contains(q) ||
                o.shippingState.lowercase().contains(q)
            val matchesStatus = statusFilter == null || st == statusFilter
            val matchesPayment = paymentFilter == null || ps == paymentFilter
            val matchesMethod = methodFilter == null || pm == methodFilter
            matchesQuery && matchesStatus && matchesPayment && matchesMethod
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Order Management (${orders.size})", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Order #, Customer, Phone, City or State") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All Statuses") })
                    }
                    items(OrderStatus.entries) { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text(st.label) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = paymentFilter == null, onClick = { paymentFilter = null }, label = { Text("All Payment States") })
                    }
                    items(PaymentStatus.entries) { ps ->
                        FilterChip(
                            selected = paymentFilter == ps,
                            onClick = { paymentFilter = if (paymentFilter == ps) null else ps },
                            label = { Text(ps.label) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = methodFilter == null, onClick = { methodFilter = null }, label = { Text("All Gateways") })
                    }
                    items(PaymentMethod.entries) { pm ->
                        FilterChip(
                            selected = methodFilter == pm,
                            onClick = { methodFilter = if (methodFilter == pm) null else pm },
                            label = { Text(pm.label) },
                        )
                    }
                }
            }
        }

        when (allOrdersState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading live customer orders...") }
            is UiState.Error -> item { BoutiqueErrorBanner(allOrdersState.message) }
            is UiState.Success -> {
                if (filteredOrders.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Orders Match Filters",
                            subtitle = "Adjust status/payment filters or wait for new customer checkouts.",
                        )
                    }
                } else {
                    items(filteredOrders, key = { it.id }) { order ->
                        val st = OrderStatus.fromRaw(order.status)
                        val ps = PaymentStatus.fromRaw(order.paymentStatus)
                        val pm = PaymentMethod.fromRaw(order.paymentMethod)
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_order_row_${order.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${order.orderNumber} · ${order.recipientName.ifBlank { order.userId.take(8) }}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "${formatAdminTimestamp(order.createdAt)} · ${pm.label} · ${order.shippingCity}, ${order.shippingState}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(formatNgnAmount(order.totalNgn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AdminStatusPill(
                                        label = st.label,
                                        tone = when (st) {
                                            OrderStatus.DELIVERED -> PillTone.SUCCESS
                                            OrderStatus.CANCELLED, OrderStatus.REFUNDED -> PillTone.ERROR
                                            OrderStatus.PAYMENT_SUBMITTED -> PillTone.ROSE
                                            else -> PillTone.GOLD
                                        },
                                    )
                                    AdminStatusPill(
                                        label = ps.label,
                                        tone = when (ps) {
                                            PaymentStatus.PAID -> PillTone.SUCCESS
                                            PaymentStatus.PENDING_VERIFICATION -> PillTone.ROSE
                                            PaymentStatus.FAILED, PaymentStatus.REFUNDED -> PillTone.ERROR
                                            else -> PillTone.NEUTRAL
                                        },
                                    )
                                    if (order.cancellationStatus == "REQUESTED") {
                                        AdminStatusPill(label = "Cancel Requested", tone = PillTone.ERROR)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { selectedOrder = order },
                                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                                    ) {
                                        Text("Manage / Inspect Order")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->
        AdminOrderDetailDialog(
            order = order,
            role = role,
            isBusy = isBusy,
            onDismiss = { selectedOrder = null },
            onSaveFulfillment = { newStatus, notes, cancelDecision, courier, trackRef, trackUrl, etaWindow, delivStatus, delivNotes ->
                onUpdateFulfillment(order, newStatus, notes, cancelDecision, courier, trackRef, trackUrl, etaWindow, delivStatus, delivNotes)
                selectedOrder = null
            },
            onRecordRefund = { amt, reason ->
                onRecordRefund(order, amt, reason)
                selectedOrder = null
            },
            onShareSummary = {
                val slip = buildString {
                    appendLine("ZEECROWN STORE — ORDER SLIP")
                    appendLine("Order: ${order.orderNumber}")
                    appendLine("Date: ${formatAdminTimestamp(order.createdAt)}")
                    appendLine("Customer: ${order.recipientName} (${order.recipientPhone})")
                    appendLine("Address: ${order.shippingAddress}, ${order.shippingCity}, ${order.shippingState}, ${order.shippingCountry}")
                    if (order.landmark.isNotBlank()) appendLine("Landmark: ${order.landmark}")
                    if (order.courierName.isNotBlank()) appendLine("Courier: ${order.courierName} (${order.trackingReference})")
                    appendLine("Items (${order.itemCount}): ${order.itemsSummary}")
                    appendLine("Subtotal: ${formatNgnAmount(order.subtotalNgn)}")
                    appendLine("Delivery: ${formatNgnAmount(order.deliveryFeeNgn)}")
                    appendLine("Discount: ${formatNgnAmount(order.discountNgn)}")
                    appendLine("Total: ${formatNgnAmount(order.totalNgn)}")
                    appendLine("Payment: ${PaymentMethod.fromRaw(order.paymentMethod).label} (${PaymentStatus.fromRaw(order.paymentStatus).label})")
                    appendLine("Fulfillment: ${OrderStatus.fromRaw(order.status).label}")
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Order Slip ${order.orderNumber}")
                    putExtra(Intent.EXTRA_TEXT, slip)
                }
                context.startActivity(Intent.createChooser(intent, "Share / Print Order Summary"))
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminOrderDetailDialog(
    order: Order,
    role: AdminRole,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSaveFulfillment: (OrderStatus, String, String, String, String, String, String, String, String) -> Unit,
    onRecordRefund: (Double, String) -> Unit,
    onShareSummary: () -> Unit,
) {
    var chosenStatus by remember(order.id) { mutableStateOf(OrderStatus.fromRaw(order.status)) }
    var internalNotes by remember(order.id) { mutableStateOf(order.internalNotes) }
    var cancelDecision by remember(order.id) { mutableStateOf(order.cancellationStatus) }
    var courierName by remember(order.id) { mutableStateOf(order.courierName) }
    var trackingReference by remember(order.id) { mutableStateOf(order.trackingReference) }
    var trackingUrl by remember(order.id) { mutableStateOf(order.trackingUrl) }
    var estimatedDeliveryWindow by remember(order.id) { mutableStateOf(order.estimatedDeliveryWindow) }
    var deliveryStatus by remember(order.id) { mutableStateOf(order.deliveryStatus) }
    var deliveryNotes by remember(order.id) { mutableStateOf(order.deliveryNotes) }
    var showRefundFields by remember(order.id) { mutableStateOf(false) }
    var refundAmountText by remember(order.id) { mutableStateOf(order.totalNgn.toLong().toString()) }
    var refundReason by remember(order.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Order ${order.orderNumber}")
                OutlinedButton(onClick = onShareSummary, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share summary", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Print/Share")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color = ZeecrownElegantSilver)
                Text("Customer: ${order.recipientName.ifBlank { "—" }} · ${order.recipientPhone.ifBlank { "No phone" }}", style = MaterialTheme.typography.bodySmall)
                Text("Address: ${order.shippingAddress}, ${order.shippingCity}, ${order.shippingState} (${order.shippingCountry})", style = MaterialTheme.typography.bodySmall)
                if (order.deliveryInstructions.isNotBlank()) {
                    Text("Note: ${order.deliveryInstructions}", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Total: ${formatNgnAmount(order.totalNgn)} · Payment: ${PaymentMethod.fromRaw(order.paymentMethod).label} (${PaymentStatus.fromRaw(order.paymentStatus).label})",
                    style = MaterialTheme.typography.titleSmall,
                )

                if (order.cancellationStatus == "REQUESTED") {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Customer Requested Cancellation", style = MaterialTheme.typography.titleSmall)
                            if (order.cancellationRequestReason.isNotBlank()) {
                                Text("Reason: ${order.cancellationRequestReason}", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    cancelDecision = "APPROVED"
                                    chosenStatus = OrderStatus.CANCELLED
                                }) { Text("Approve Cancel") }
                                OutlinedButton(onClick = { cancelDecision = "REJECTED" }) { Text("Decline") }
                            }
                        }
                    }
                }

                HorizontalDivider(color = ZeecrownElegantSilver)
                Text("Fulfillment Status (Triggers Automated Customer Updates)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrderStatus.entries.forEach { st ->
                        FilterChip(
                            selected = chosenStatus == st,
                            onClick = { chosenStatus = st },
                            label = { Text(st.label) },
                        )
                    }
                }

                HorizontalDivider(color = ZeecrownElegantSilver)
                Text("Courier & Live Tracking Details", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = courierName,
                    onValueChange = { courierName = it },
                    label = { Text("Courier Partner (e.g. GIG Logistics, DHL, Sendbox)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_order_courier_input"),
                )
                OutlinedTextField(
                    value = trackingReference,
                    onValueChange = { trackingReference = it },
                    label = { Text("Waybill / Tracking Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_order_tracking_ref_input"),
                )
                OutlinedTextField(
                    value = trackingUrl,
                    onValueChange = { trackingUrl = it },
                    label = { Text("Direct Tracking URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = estimatedDeliveryWindow,
                    onValueChange = { estimatedDeliveryWindow = it },
                    label = { Text("Estimated Delivery Window (e.g. 24-48 hours)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = deliveryStatus,
                    onValueChange = { deliveryStatus = it },
                    label = { Text("Courier Checkpoint / Delivery Status") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = deliveryNotes,
                    onValueChange = { deliveryNotes = it },
                    label = { Text("Customer-Visible Delivery Note") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = internalNotes,
                    onValueChange = { internalNotes = it },
                    label = { Text("Internal Admin Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )

                if (role.canVerifyPayments && PaymentStatus.fromRaw(order.paymentStatus) == PaymentStatus.PAID) {
                    TextButton(onClick = { showRefundFields = !showRefundFields }) {
                        Text(if (showRefundFields) "Hide Refund Form" else "Record Customer Refund")
                    }
                    if (showRefundFields) {
                        OutlinedTextField(
                            value = refundAmountText,
                            onValueChange = { refundAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Refund Amount (NGN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = refundReason,
                            onValueChange = { refundReason = it },
                            label = { Text("Refund Reason") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                val amt = refundAmountText.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) onRecordRefund(amt, refundReason)
                            },
                            enabled = !isBusy && (refundAmountText.toDoubleOrNull() ?: 0.0) > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("Confirm Refund Record") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveFulfillment(
                        chosenStatus,
                        internalNotes,
                        cancelDecision,
                        courierName,
                        trackingReference,
                        trackingUrl,
                        estimatedDeliveryWindow,
                        deliveryStatus,
                        deliveryNotes,
                    )
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
            ) { Text("Save Order Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
