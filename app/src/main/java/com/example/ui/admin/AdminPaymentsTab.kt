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
import com.example.data.model.Order
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRecordStatus
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownSageSuccess

@Composable
fun AdminPaymentsTab(
    paymentsState: UiState<List<PaymentRecord>>,
    orders: List<Order>,
    isBusy: Boolean,
    onReviewPayment: (payment: PaymentRecord, order: Order?, approve: Boolean, note: String) -> Unit,
) {
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val ordersById = remember(orders) { orders.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<PaymentRecordStatus?>(null) }
    var providerFilter by remember { mutableStateOf<PaymentMethod?>(null) }
    var paymentToReview by remember { mutableStateOf<PaymentRecord?>(null) }

    val filteredPayments = remember(payments, searchQuery, statusFilter, providerFilter) {
        val q = searchQuery.trim().lowercase()
        payments.filter { p ->
            val linkedOrder = ordersById[p.orderId]
            val ps = PaymentRecordStatus.fromRaw(p.status)
            val pm = PaymentMethod.fromRaw(p.provider)
            val matchesQuery = q.isBlank() ||
                p.reference.lowercase().contains(q) ||
                p.orderNumber.lowercase().contains(q) ||
                linkedOrder?.orderNumber?.lowercase()?.contains(q) == true ||
                p.transferNote.lowercase().contains(q) ||
                p.customerName.lowercase().contains(q)
            val matchesStatus = statusFilter == null || ps == statusFilter
            val matchesProvider = providerFilter == null || pm == providerFilter
            matchesQuery && matchesStatus && matchesProvider
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Payment Verification Center (${payments.size})", style = MaterialTheme.typography.titleLarge)
                val awaiting = payments.count { PaymentRecordStatus.fromRaw(it.status) == PaymentRecordStatus.SUBMITTED }
                if (awaiting > 0) {
                    AdminSectionCard(containerColor = ZeecrownLightLilac) {
                        Text(
                            "$awaiting Bank Transfer proof(s) awaiting manual verification",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Approving verifies the payment record and atomically updates the linked order payment status to PAID.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Reference, Order # or Transfer Note") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All States") }) }
                    items(PaymentRecordStatus.entries) { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text(st.label) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = providerFilter == null, onClick = { providerFilter = null }, label = { Text("All Methods") }) }
                    items(PaymentMethod.entries) { pm ->
                        FilterChip(
                            selected = providerFilter == pm,
                            onClick = { providerFilter = if (providerFilter == pm) null else pm },
                            label = { Text(pm.label) },
                        )
                    }
                }
            }
        }

        when (paymentsState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading payment transactions...") }
            is UiState.Error -> item { BoutiqueErrorBanner(paymentsState.message) }
            is UiState.Success -> {
                if (filteredPayments.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Payment Transactions Match",
                            subtitle = "Payment attempts across Paystack, Flutterwave, Bank Transfer, and COD will list here.",
                        )
                    }
                } else {
                    items(filteredPayments, key = { it.id }) { payment ->
                        val linkedOrder = ordersById[payment.orderId]
                        val ps = PaymentRecordStatus.fromRaw(payment.status)
                        val pm = PaymentMethod.fromRaw(payment.provider)
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_payment_row_${payment.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${payment.reference} · ${linkedOrder?.orderNumber ?: payment.orderNumber.ifBlank { payment.orderId }}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "${pm.label} · ${formatAdminTimestamp(payment.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(formatNgnAmount(payment.amountNgn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                if (payment.transferNote.isNotBlank()) {
                                    Text("Customer Note: ${payment.transferNote}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (payment.verificationNote.isNotBlank()) {
                                    Text("Admin Review: ${payment.verificationNote} (${payment.reviewedByEmail})", style = MaterialTheme.typography.labelSmall, color = ZeecrownDeepPurple)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    AdminStatusPill(
                                        label = ps.label,
                                        tone = when (ps) {
                                            PaymentRecordStatus.VERIFIED -> PillTone.SUCCESS
                                            PaymentRecordStatus.SUBMITTED -> PillTone.ROSE
                                            PaymentRecordStatus.FAILED, PaymentRecordStatus.REFUNDED -> PillTone.ERROR
                                            PaymentRecordStatus.INITIATED -> PillTone.GOLD
                                        },
                                    )
                                    if (ps != PaymentRecordStatus.VERIFIED) {
                                        OutlinedButton(
                                            onClick = { paymentToReview = payment },
                                            enabled = !isBusy,
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Text("Verify / Review")
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

    paymentToReview?.let { payment ->
        val linkedOrder = ordersById[payment.orderId]
        var note by remember(payment.id) { mutableStateOf(payment.verificationNote) }
        AlertDialog(
            onDismissRequest = { paymentToReview = null },
            title = { Text("Verify Payment · ${payment.reference}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order: ${linkedOrder?.orderNumber ?: payment.orderId}", style = MaterialTheme.typography.titleSmall)
                    Text("Amount: ${formatNgnAmount(payment.amountNgn)} via ${PaymentMethod.fromRaw(payment.provider).label}", style = MaterialTheme.typography.bodyMedium)
                    if (payment.transferNote.isNotBlank()) {
                        Text("Transfer Reference / Sender: ${payment.transferNote}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Verification Audit Note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewPayment(payment, linkedOrder, true, note)
                        paymentToReview = null
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownSageSuccess, contentColor = ZeecrownPureWhite),
                ) { Text("Approve & Mark PAID") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            onReviewPayment(payment, linkedOrder, false, note)
                            paymentToReview = null
                        },
                        enabled = !isBusy,
                    ) { Text("Reject", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { paymentToReview = null }) { Text("Cancel") }
                }
            },
        )
    }
}
