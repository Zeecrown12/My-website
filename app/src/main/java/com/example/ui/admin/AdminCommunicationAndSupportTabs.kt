package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.AdminUser
import com.example.data.model.CommunicationChannel
import com.example.data.model.CommunicationLog
import com.example.data.model.CommunicationStatus
import com.example.data.model.DeliveryFeedback
import com.example.data.model.Order
import com.example.data.model.SupportCategory
import com.example.data.model.SupportEnquiry
import com.example.data.model.SupportInternalNote
import com.example.data.model.SupportReply
import com.example.data.model.SupportStatus
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCommunicationCenterTab(
    communicationLogsState: UiState<List<CommunicationLog>>,
    orders: List<Order>,
    isBusy: Boolean,
    onSendManualOrderUpdate: (order: Order, title: String, message: String, onDone: () -> Unit) -> Unit,
    onRetryLog: (CommunicationLog) -> Unit,
) {
    val logs = (communicationLogsState as? UiState.Success)?.data.orEmpty()
    var searchQuery by remember { mutableStateOf("") }
    var channelFilter by remember { mutableStateOf<CommunicationChannel?>(null) }
    var statusFilter by remember { mutableStateOf<CommunicationStatus?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }

    val sentCount = remember(logs) { logs.count { CommunicationStatus.fromRaw(it.status) == CommunicationStatus.SENT } }
    val failedCount = remember(logs) { logs.count { CommunicationStatus.fromRaw(it.status) == CommunicationStatus.FAILED } }
    val notConfiguredCount = remember(logs) { logs.count { CommunicationStatus.fromRaw(it.status) == CommunicationStatus.PROVIDER_NOT_CONFIGURED } }
    val queuedOrSkippedCount = remember(logs) {
        logs.count {
            val st = CommunicationStatus.fromRaw(it.status)
            st == CommunicationStatus.QUEUED || st == CommunicationStatus.SKIPPED_NO_CONSENT
        }
    }

    val filteredLogs = remember(logs, searchQuery, channelFilter, statusFilter) {
        val q = searchQuery.trim().lowercase()
        logs.filter { log ->
            val ch = CommunicationChannel.fromRaw(log.channel)
            val st = CommunicationStatus.fromRaw(log.status)
            val matchesQ = q.isBlank() ||
                log.orderNumber.lowercase().contains(q) ||
                log.recipientMasked.lowercase().contains(q) ||
                log.subject.lowercase().contains(q) ||
                log.eventType.lowercase().contains(q) ||
                log.providerName.lowercase().contains(q)
            val matchesChannel = channelFilter == null || ch == channelFilter
            val matchesStatus = statusFilter == null || st == statusFilter
            matchesQ && matchesChannel && matchesStatus
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Customer Communication Center (${logs.size})", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Live delivery telemetry, email/SMS/WhatsApp logs, consent skips & manual dispatch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { showManualDialog = true },
                        enabled = !isBusy && orders.isNotEmpty(),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_send_manual_notif_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send Order Update")
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AdminMetricCard("Delivered / Sent", sentCount.toString(), accentColor = ZeecrownSageSuccess, modifier = Modifier.width(150.dp))
                    AdminMetricCard("Failed", failedCount.toString(), accentColor = MaterialTheme.colorScheme.error, modifier = Modifier.width(140.dp))
                    AdminMetricCard("Provider Missing", notConfiguredCount.toString(), subtitle = "Honest provider check", modifier = Modifier.width(160.dp))
                    AdminMetricCard("Queued / Skipped", queuedOrSkippedCount.toString(), modifier = Modifier.width(155.dp))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Order #, Recipient, Event Type, Subject or Provider") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = channelFilter == null, onClick = { channelFilter = null }, label = { Text("All Channels") })
                    }
                    items(CommunicationChannel.entries) { ch ->
                        FilterChip(
                            selected = channelFilter == ch,
                            onClick = { channelFilter = if (channelFilter == ch) null else ch },
                            label = { Text(ch.label) },
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All States") })
                    }
                    items(CommunicationStatus.entries) { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text(st.label) },
                        )
                    }
                }
            }
        }

        when (communicationLogsState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading communication logs...") }
            is UiState.Error -> item { BoutiqueErrorBanner(communicationLogsState.message) }
            is UiState.Success -> {
                if (filteredLogs.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Communication Logs Match",
                            subtitle = "Adjust channel/status filters or dispatch an order notification.",
                        )
                    }
                } else {
                    items(filteredLogs, key = { it.id }) { log ->
                        val channel = CommunicationChannel.fromRaw(log.channel)
                        val status = CommunicationStatus.fromRaw(log.status)
                        val canRetry = status == CommunicationStatus.FAILED ||
                            status == CommunicationStatus.QUEUED ||
                            status == CommunicationStatus.PROVIDER_NOT_CONFIGURED

                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_comm_log_${log.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AdminStatusPill(label = channel.label, tone = PillTone.GOLD)
                                        AdminStatusPill(
                                            label = status.label,
                                            tone = when (status) {
                                                CommunicationStatus.SENT -> PillTone.SUCCESS
                                                CommunicationStatus.FAILED -> PillTone.ERROR
                                                CommunicationStatus.PROVIDER_NOT_CONFIGURED -> PillTone.ROSE
                                                else -> PillTone.NEUTRAL
                                            },
                                        )
                                    }
                                    Text(formatAdminTimestamp(log.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Text(log.subject.ifBlank { log.eventType }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (log.bodyPreview.isNotBlank()) {
                                    Text(log.bodyPreview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = buildString {
                                        append("Recipient: ${log.recipientMasked.ifBlank { "Customer" }}")
                                        if (log.orderNumber.isNotBlank()) append(" · Order ${log.orderNumber}")
                                        if (log.providerName.isNotBlank()) append(" · Provider: ${log.providerName}")
                                        if (log.retryCount > 0L) append(" · Retries: ${log.retryCount}")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (log.failureReason.isNotBlank()) {
                                    Text("Reason: ${log.failureReason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                                if (canRetry) {
                                    OutlinedButton(
                                        onClick = { onRetryLog(log) },
                                        enabled = !isBusy,
                                        modifier = Modifier.minimumInteractiveComponentSize().testTag("retry_comm_log_${log.id}"),
                                    ) {
                                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Retry Dispatch")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualOrderNotificationDialog(
            orders = orders,
            isBusy = isBusy,
            onDismiss = { showManualDialog = false },
            onSend = { order, title, message ->
                onSendManualOrderUpdate(order, title, message) {
                    showManualDialog = false
                }
            },
        )
    }
}

@Composable
private fun ManualOrderNotificationDialog(
    orders: List<Order>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSend: (Order, String, String) -> Unit,
) {
    var selectedOrder by remember(orders) { mutableStateOf(orders.firstOrNull()) }
    var title by remember { mutableStateOf("Boutique Order Update") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Manual Order Notification") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Select Order", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(orders.take(20), key = { it.id }) { order ->
                        FilterChip(
                            selected = selectedOrder?.id == order.id,
                            onClick = { selectedOrder = order },
                            label = { Text(order.orderNumber) },
                        )
                    }
                }
                selectedOrder?.let { ord ->
                    Text(
                        "To: ${ord.recipientName.ifBlank { "Customer" }} · ${ord.shippingCity}, ${ord.shippingState}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notification Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_notif_title"),
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message to Customer *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("manual_notif_message"),
                )
                Text(
                    "Creates an in-app notification + queued email dispatch log (if consent & email provider enabled).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedOrder?.let { onSend(it, title, message) } },
                enabled = !isBusy && selectedOrder != null && title.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                modifier = Modifier.testTag("manual_notif_confirm"),
            ) { Text("Send Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSupportInboxTab(
    supportEnquiriesState: UiState<List<SupportEnquiry>>,
    deliveryFeedbackState: UiState<List<DeliveryFeedback>>,
    adminTeamState: UiState<List<AdminUser>> = UiState.Success(emptyList()),
    selectedReplies: List<SupportReply>,
    selectedInternalNotes: List<SupportInternalNote>,
    isBusy: Boolean,
    onSelectEnquiry: (String?) -> Unit,
    onUpdateMeta: (SupportEnquiry, SupportStatus, Boolean, AdminUser?, Boolean, String?) -> Unit,
    onPostReply: (SupportEnquiry, String, SupportStatus, () -> Unit) -> Unit,
    onAddInternalNote: (SupportEnquiry, String, () -> Unit) -> Unit,
) {
    val enquiries = (supportEnquiriesState as? UiState.Success)?.data.orEmpty()
    val feedbacks = (deliveryFeedbackState as? UiState.Success)?.data.orEmpty()
    val activeStaff = remember(adminTeamState) {
        (adminTeamState as? UiState.Success)?.data.orEmpty().filter { it.active }
    }

    var selectedEnquiryId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<SupportStatus?>(null) }
    var categoryFilter by remember { mutableStateOf<SupportCategory?>(null) }

    val activeEnquiry = enquiries.firstOrNull { it.id == selectedEnquiryId }

    if (activeEnquiry != null) {
        var replyText by remember(activeEnquiry.id) { mutableStateOf("") }
        var nextStatus by remember(activeEnquiry.id) { mutableStateOf(SupportStatus.WAITING_CUSTOMER) }
        var internalNoteText by remember(activeEnquiry.id) { mutableStateOf("") }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                selectedEnquiryId = null
                                onSelectEnquiry(null)
                            },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Inbox")
                        }
                        Column {
                            Text(activeEnquiry.subject, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${activeEnquiry.customerName.ifBlank { "Customer" }} · ${activeEnquiry.customerEmail.ifBlank { activeEnquiry.userId }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    AdminStatusPill(
                        label = SupportStatus.fromRaw(activeEnquiry.status).label,
                        tone = when (SupportStatus.fromRaw(activeEnquiry.status)) {
                            SupportStatus.RESOLVED, SupportStatus.CLOSED -> PillTone.SUCCESS
                            SupportStatus.OPEN, SupportStatus.NEW, SupportStatus.WAITING_FOR_STAFF -> PillTone.ROSE
                            else -> PillTone.GOLD
                        },
                    )
                }
            }

            if (activeEnquiry.sourceChannel.contains("AI", ignoreCase = true) ||
                activeEnquiry.aiSummary.isNotBlank() ||
                activeEnquiry.transcriptPreview.isNotBlank()
            ) {
                item {
                    var showTranscript by remember(activeEnquiry.id) { mutableStateOf(false) }
                    AdminSectionCard(containerColor = ZeecrownWarmIvory) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AdminStatusPill(
                                label = if (activeEnquiry.sourceChannel == "WEB_AI_ASSISTANT") "Web AI Handover" else "AI Handover",
                                tone = PillTone.GOLD,
                            )
                            if (activeEnquiry.aiConversationId.isNotBlank()) {
                                Text(
                                    "Session #${activeEnquiry.aiConversationId.take(8).uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (activeEnquiry.escalationReason.isNotBlank()) {
                            Text(
                                "Escalation Trigger: ${activeEnquiry.escalationReason}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ZeecrownDeepPurple,
                            )
                        }
                        if (activeEnquiry.aiSummary.isNotBlank()) {
                            Text(
                                "AI Summary: ${activeEnquiry.aiSummary}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (activeEnquiry.lastAssistantReply.isNotBlank()) {
                            Text(
                                "Last AI Reply: ${activeEnquiry.lastAssistantReply}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (activeEnquiry.transcriptPreview.isNotBlank()) {
                            TextButton(
                                onClick = { showTranscript = !showTranscript },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Text(if (showTranscript) "Hide AI Conversation Transcript ▲" else "View AI Conversation Transcript ▼")
                            }
                            if (showTranscript) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.small,
                                    color = ZeecrownPureWhite,
                                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                ) {
                                    Text(
                                        activeEnquiry.transcriptPreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                AdminSectionCard {
                    Text(
                        "Category: ${SupportCategory.fromRaw(activeEnquiry.category).label}" +
                            if (activeEnquiry.orderNumber.isNotBlank()) " · Order ${activeEnquiry.orderNumber}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(activeEnquiry.message, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(color = ZeecrownElegantSilver)
                    Text(
                        text = if (activeEnquiry.assignedAdminEmail.isNotBlank()) {
                            "Assigned to: ${activeEnquiry.assignedAdminEmail}"
                        } else {
                            "Unassigned ticket"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onUpdateMeta(activeEnquiry, SupportStatus.ASSIGNED, true, null, false, null) },
                            enabled = !isBusy,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) { Text("Assign to Me") }
                        activeStaff.take(6).forEach { staff ->
                            val isSelectedStaff = activeEnquiry.assignedAdminUid == staff.userId.ifBlank { staff.id }
                            FilterChip(
                                selected = isSelectedStaff,
                                onClick = {
                                    val nextSt = if (SupportStatus.fromRaw(activeEnquiry.status) in setOf(SupportStatus.NEW, SupportStatus.OPEN, SupportStatus.WAITING_FOR_STAFF)) {
                                        SupportStatus.ASSIGNED
                                    } else SupportStatus.fromRaw(activeEnquiry.status)
                                    onUpdateMeta(activeEnquiry, nextSt, false, staff, false, null)
                                },
                                label = { Text(staff.displayName.ifBlank { staff.email.substringBefore("@") }) },
                            )
                        }
                        if (activeEnquiry.assignedAdminUid.isNotBlank()) {
                            TextButton(
                                onClick = { onUpdateMeta(activeEnquiry, SupportStatus.WAITING_FOR_STAFF, false, null, true, null) },
                                enabled = !isBusy,
                            ) { Text("Unassign") }
                        }
                    }
                    Text("Ticket Priority", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LOW", "NORMAL", "HIGH", "URGENT").forEach { prio ->
                            FilterChip(
                                selected = activeEnquiry.priority.uppercase() == prio,
                                onClick = {
                                    onUpdateMeta(activeEnquiry, SupportStatus.fromRaw(activeEnquiry.status), false, null, false, prio)
                                },
                                label = { Text(prio) },
                            )
                        }
                    }
                    Text("Ticket Status", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SupportStatus.entries.forEach { st ->
                            FilterChip(
                                selected = SupportStatus.fromRaw(activeEnquiry.status) == st,
                                onClick = { onUpdateMeta(activeEnquiry, st, false, null, false, null) },
                                label = { Text(st.label) },
                            )
                        }
                    }
                }
            }

            item {
                Text("Customer Conversation Thread (${selectedReplies.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (selectedReplies.isEmpty()) {
                item {
                    Text(
                        "No replies yet. Send a helpful concierge response below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(selectedReplies, key = { it.id }) { reply ->
                    val isStaff = reply.senderRole != "CUSTOMER"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isStaff) ZeecrownLightLilac else MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isStaff) "${reply.senderName} (${reply.senderRole})" else reply.senderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(formatAdminTimestamp(reply.createdAt), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(reply.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                AdminSectionCard {
                    Text("Send Public Reply to Customer", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            SupportStatus.WAITING_CUSTOMER,
                            SupportStatus.IN_PROGRESS,
                            SupportStatus.RESOLVED,
                            SupportStatus.CLOSED,
                        ).forEach { st ->
                            FilterChip(
                                selected = nextStatus == st,
                                onClick = { nextStatus = st },
                                label = { Text("Set: ${st.label}") },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Customer-Visible Reply *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("admin_support_reply_input"),
                    )
                    Button(
                        onClick = {
                            onPostReply(activeEnquiry, replyText, nextStatus) {
                                replyText = ""
                            }
                        },
                        enabled = !isBusy && replyText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("admin_support_reply_send"),
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send Customer Reply")
                    }
                }
            }

            item {
                AdminSectionCard(containerColor = ZeecrownLightLilac) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Lock, contentDescription = "Staff Only", modifier = Modifier.size(18.dp), tint = ZeecrownDeepPurple)
                        Text("Staff-Only Private Internal Notes (${selectedInternalNotes.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Stored in an isolated admin-only subcollection. Customers can never read or list these notes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    selectedInternalNotes.forEach { note ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = ZeecrownPureWhite,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(note.adminEmail.ifBlank { "Staff" }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(formatAdminTimestamp(note.createdAt), style = MaterialTheme.typography.labelSmall)
                                }
                                Text(note.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = internalNoteText,
                        onValueChange = { internalNoteText = it },
                        label = { Text("Add Internal Staff Note") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("admin_internal_note_input"),
                    )
                    OutlinedButton(
                        onClick = {
                            onAddInternalNote(activeEnquiry, internalNoteText) {
                                internalNoteText = ""
                            }
                        },
                        enabled = !isBusy && internalNoteText.isNotBlank(),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_internal_note_save"),
                    ) { Text("Save Private Note") }
                }
            }
        }
        return
    }

    val filteredEnquiries = remember(enquiries, searchQuery, statusFilter, categoryFilter) {
        val q = searchQuery.trim().lowercase()
        enquiries.filter { e ->
            val st = SupportStatus.fromRaw(e.status)
            val cat = SupportCategory.fromRaw(e.category)
            val matchesQ = q.isBlank() ||
                e.subject.lowercase().contains(q) ||
                e.customerName.lowercase().contains(q) ||
                e.customerEmail.lowercase().contains(q) ||
                e.orderNumber.lowercase().contains(q)
            val matchesSt = statusFilter == null || st == statusFilter
            val matchesCat = categoryFilter == null || cat == categoryFilter
            matchesQ && matchesSt && matchesCat
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = ZeecrownRoyalPurple)
                    Text("Concierge Support Inbox (${enquiries.size})", style = MaterialTheme.typography.titleLarge, color = ZeecrownDeepPurple)
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Subject, Customer Name/Email or Order #") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All Statuses") })
                    }
                    items(SupportStatus.entries) { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text(st.label) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("All Categories") })
                    }
                    items(SupportCategory.entries) { cat ->
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                            label = { Text(cat.label) },
                        )
                    }
                }
            }
        }

        when (supportEnquiriesState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading customer support enquiries...") }
            is UiState.Error -> item { BoutiqueErrorBanner(supportEnquiriesState.message) }
            is UiState.Success -> {
                if (filteredEnquiries.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "Support Inbox Clear",
                            subtitle = "No customer enquiries match your current filters.",
                        )
                    }
                } else {
                    items(filteredEnquiries, key = { it.id }) { enq ->
                        val st = SupportStatus.fromRaw(enq.status)
                        val cat = SupportCategory.fromRaw(enq.category)
                        Surface(
                            onClick = {
                                selectedEnquiryId = enq.id
                                onSelectEnquiry(enq.id)
                            },
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("admin_enquiry_${enq.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AdminStatusPill(
                                            label = st.label,
                                            tone = when (st) {
                                                SupportStatus.RESOLVED, SupportStatus.CLOSED -> PillTone.SUCCESS
                                                SupportStatus.OPEN, SupportStatus.NEW, SupportStatus.WAITING_FOR_STAFF -> PillTone.ROSE
                                                else -> PillTone.GOLD
                                            },
                                        )
                                        AdminStatusPill(label = cat.label, tone = PillTone.NEUTRAL)
                                        if (enq.priority.uppercase() in setOf("HIGH", "URGENT")) {
                                            AdminStatusPill(label = enq.priority.uppercase(), tone = PillTone.ERROR)
                                        }
                                        if (enq.sourceChannel.contains("AI", ignoreCase = true)) {
                                            AdminStatusPill(label = "AI Handover", tone = PillTone.GOLD)
                                        }
                                    }
                                    Text(formatAdminTimestamp(enq.updatedAt ?: enq.createdAt), style = MaterialTheme.typography.labelSmall)
                                }
                                Text(enq.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${enq.customerName.ifBlank { "Customer" }} · ${enq.customerEmail.ifBlank { "No email" }}" +
                                        (if (enq.orderNumber.isNotBlank()) " · Order ${enq.orderNumber}" else "") +
                                        (if (enq.assignedAdminEmail.isNotBlank()) " · Assigned: ${enq.assignedAdminEmail}" else ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = enq.lastReplyPreview.ifBlank { enq.message },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            AdminSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = ZeecrownRoyalPurple, modifier = Modifier.size(18.dp))
                    Text("Post-Delivery Customer Feedback (${feedbacks.size})", style = MaterialTheme.typography.titleMedium)
                }
                if (feedbacks.isEmpty()) {
                    Text("No post-delivery ratings submitted yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    feedbacks.take(15).forEach { fb ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = ZeecrownSoftLavender.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Order ${fb.orderNumber} · ${fb.customerName.ifBlank { "Customer" }}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(formatAdminTimestamp(fb.createdAt), style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    "Overall ${fb.overallRating}/5 · Courier Speed ${fb.deliveryRating}/5 · Packaging ${fb.packagingRating}/5",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                if (fb.comment.isNotBlank()) {
                                    Text(fb.comment, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
