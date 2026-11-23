package com.example.ui.storefront

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.SupportAgent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.UiState
import com.example.data.model.BoutiqueContactHelper
import com.example.data.model.Order
import com.example.data.model.StoreSettings
import com.example.data.model.SupportCategory
import com.example.data.model.SupportEnquiry
import com.example.data.model.SupportReply
import com.example.data.model.SupportStatus
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.launchWhatsAppChat
import com.example.ui.theme.ZeecrownChampagneGold
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownObsidianInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWhatsAppGreen
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportCenterScreen(
    isSignedIn: Boolean,
    storeSettings: StoreSettings,
    orders: List<Order>,
    enquiriesState: UiState<List<SupportEnquiry>>,
    observeReplies: (String) -> Flow<List<SupportReply>>,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onOpenOrderTracking: () -> Unit,
    onOpenSignIn: () -> Unit,
    onSubmitEnquiry: (SupportCategory, String, String, String, String, () -> Unit) -> Unit,
    onPostReply: (SupportEnquiry, String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var selectedEnquiryId by remember { mutableStateOf<String?>(null) }

    var category by remember { mutableStateOf(SupportCategory.ORDER_ISSUE) }
    var linkedOrder by remember { mutableStateOf<Order?>(null) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var replyText by remember { mutableStateOf("") }

    val enquiries = (enquiriesState as? UiState.Success)?.data.orEmpty()
    val activeEnquiry = enquiries.firstOrNull { it.id == selectedEnquiryId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = 880.dp).fillMaxSize()) {
            Surface(color = ZeecrownPureWhite, border = BorderStroke(1.dp, ZeecrownElegantSilver)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = ZeecrownDeepPurple)
                        }
                        Column {
                            Text("ZEECROWN Customer Support", style = MaterialTheme.typography.titleLarge, color = ZeecrownDeepPurple)
                            Text("Order assistance, tracking & policy help", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = onOpenOrderTracking, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Track Order")
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = ZeecrownLightLilac,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val supportContext = LocalContext.current
                            val resolvedWaDisplay = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(storeSettings.whatsappNumber)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = ZeecrownRoyalPurple)
                                Text("Boutique Contact Channels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ZeecrownDeepPurple)
                            }
                            if (storeSettings.supportEmail.isNotBlank()) Text("Email: ${storeSettings.supportEmail}", style = MaterialTheme.typography.bodyMedium)
                            if (storeSettings.supportPhone.isNotBlank()) Text("Phone: ${storeSettings.supportPhone}", style = MaterialTheme.typography.bodyMedium)
                            Text("Official WhatsApp: $resolvedWaDisplay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ZeecrownDeepPurple)
                            if (storeSettings.supportHours.isNotBlank()) Text("Hours: ${storeSettings.supportHours}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (storeSettings.businessAddress.isNotBlank()) Text("Atelier: ${storeSettings.businessAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = { launchWhatsAppChat(supportContext, storeSettings.whatsappNumber) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("support_whatsapp_btn"),
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownWhatsAppGreen, contentColor = Color.White),
                            ) {
                                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Chat with Us on WhatsApp")
                            }
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("support_order_policy_card"),
                        shape = MaterialTheme.shapes.medium,
                        color = ZeecrownSoftLavender.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, ZeecrownChampagneGold),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Official Order Policy",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ZeecrownDeepPurple,
                            )
                            Text(
                                storeSettings.returnPolicy.ifBlank {
                                    "ZEECROWN STORE currently does not accept customer returns or refunds. Please verify sizing and details before purchasing. If your delivery arrived wrong or damaged, submit a support request below or message WhatsApp for priority resolution."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ZeecrownObsidianInk,
                            )
                        }
                    }
                }

                if (!isSignedIn) {
                    item {
                        BoutiqueEmptyView(
                            title = "Sign In to Open a Support Ticket",
                            subtitle = "Track responses from ZEECROWN staff linked directly to your account and orders.",
                            actionLabel = "Sign In",
                            onAction = onOpenSignIn,
                        )
                    }
                } else if (activeEnquiry != null) {
                    item {
                        val repliesFlow = remember(activeEnquiry.id) { observeReplies(activeEnquiry.id) }
                        val replies by repliesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { selectedEnquiryId = null }) { Text("← All Enquiries") }
                                    Surface(shape = RoundedCornerShape(6.dp), color = ZeecrownSoftLavender) {
                                        Text(
                                            SupportStatus.fromRaw(activeEnquiry.status).label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ZeecrownDeepPurple,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                                Text(activeEnquiry.subject, style = MaterialTheme.typography.titleLarge)
                                if (activeEnquiry.orderNumber.isNotBlank()) {
                                    Text("Linked Order: ${activeEnquiry.orderNumber}", style = MaterialTheme.typography.labelMedium, color = ZeecrownGoldInk)
                                }
                                Surface(shape = MaterialTheme.shapes.small, color = ZeecrownLightLilac) {
                                    Text(activeEnquiry.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                                }
                                HorizontalDivider(color = ZeecrownElegantSilver)

                                replies.forEach { rep ->
                                    val isAdmin = rep.senderRole == "ADMIN"
                                    val ts = rep.createdAt?.toDate()?.let {
                                        SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(it)
                                    } ?: "Just now"
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (isAdmin) ZeecrownSoftLavender.copy(alpha = 0.6f) else ZeecrownPureWhite,
                                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(rep.senderName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                Text(ts, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(rep.message, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    label = { Text("Write a reply...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                )
                                Button(
                                    onClick = {
                                        onPostReply(activeEnquiry, replyText) { replyText = "" }
                                    },
                                    enabled = !isSubmitting && replyText.trim().isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Send Reply")
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Submit a Support Request", style = MaterialTheme.typography.titleLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SupportCategory.entries.forEach { cat ->
                                        FilterChip(
                                            selected = category == cat,
                                            onClick = { category = cat },
                                            label = { Text(cat.label) },
                                        )
                                    }
                                }
                                if (orders.isNotEmpty()) {
                                    Text("Link to Order (Optional):", style = MaterialTheme.typography.labelMedium)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = linkedOrder == null,
                                            onClick = { linkedOrder = null },
                                            label = { Text("None") },
                                        )
                                        orders.take(5).forEach { ord ->
                                            FilterChip(
                                                selected = linkedOrder?.id == ord.id,
                                                onClick = { linkedOrder = ord },
                                                label = { Text(ord.orderNumber) },
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = { subject = it },
                                    label = { Text("Subject *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("support_subject_input"),
                                )
                                OutlinedTextField(
                                    value = message,
                                    onValueChange = { message = it },
                                    label = { Text("How can we assist you? *") },
                                    modifier = Modifier.fillMaxWidth().testTag("support_message_input"),
                                    minLines = 3,
                                )
                                Button(
                                    onClick = {
                                        onSubmitEnquiry(
                                            category,
                                            subject,
                                            message,
                                            linkedOrder?.id.orEmpty(),
                                            linkedOrder?.orderNumber.orEmpty(),
                                        ) {
                                            subject = ""
                                            message = ""
                                        }
                                    },
                                    enabled = !isSubmitting && subject.trim().length >= 2 && message.trim().length >= 5,
                                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("support_submit_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                                ) { Text(if (isSubmitting) "Submitting..." else "Submit Support Request") }
                            }
                        }
                    }

                    item { Text("Your Enquiries (${enquiries.size})", style = MaterialTheme.typography.titleLarge) }
                    when (enquiriesState) {
                        is UiState.Loading -> item { BoutiqueLoadingView("Loading enquiries...") }
                        is UiState.Error -> item { BoutiqueErrorBanner(enquiriesState.message) }
                        is UiState.Success -> {
                            if (enquiries.isEmpty()) {
                                item {
                                    OutlinedButton(onClick = onOpenOrderTracking, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()) {
                                        Text("No tickets yet — Track an existing order")
                                    }
                                }
                            } else {
                                items(enquiries, key = { it.id }) { enq ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedEnquiryId = enq.id },
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(enq.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                Surface(shape = RoundedCornerShape(6.dp), color = ZeecrownSoftLavender) {
                                                    Text(SupportStatus.fromRaw(enq.status).label, style = MaterialTheme.typography.labelSmall, color = ZeecrownDeepPurple, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                                }
                                            }
                                            Text(
                                                "${SupportCategory.fromRaw(enq.category).label}${if (enq.orderNumber.isNotBlank()) " · ${enq.orderNumber}" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ZeecrownGoldInk,
                                            )
                                            Text(enq.lastReplyPreview.ifBlank { enq.message }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
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
}
