package com.example.ui.storefront

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AiActionDestination
import com.example.data.model.AiActionLink
import com.example.data.model.AiAssistantConfig
import com.example.data.model.AiChatMessage
import com.example.data.model.AiConversation
import com.example.data.model.AiProductCard
import com.example.data.model.BoutiqueContactHelper
import com.example.data.model.StoreCurrency
import com.example.data.model.StoreSettings
import com.example.data.model.SupportCategory
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.components.formatBoutiquePrice
import com.example.ui.components.formatNgnAmount
import com.example.ui.components.launchWhatsAppChat
import com.example.ui.theme.ZeecrownChampagneGold
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownErrorContainer
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownObsidianInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory
import com.example.ui.theme.ZeecrownWhatsAppGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    config: AiAssistantConfig,
    storeSettings: StoreSettings,
    currency: StoreCurrency,
    isSignedIn: Boolean,
    messages: List<AiChatMessage>,
    recentConversations: List<AiConversation>,
    isSending: Boolean,
    isSubmittingHandover: Boolean,
    escalatedTicketId: String?,
    handoverError: String?,
    markedHelpful: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onNewConversation: () -> Unit,
    onOpenConversationHistory: (AiConversation) -> Unit,
    onMarkHelpful: () -> Unit,
    onEscalateToHuman: (reason: String, category: SupportCategory, orderNumber: String, note: String) -> Unit,
    onClearHandoverError: () -> Unit,
    onLookupGuestOrder: (orderNumber: String, contact: String, token: String) -> Unit,
    onProductClick: (String) -> Unit,
    onNavigateAction: (AiActionLink) -> Unit,
    onOpenSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var draftInput by remember { mutableStateOf("") }
    var showEscalateDialog by remember { mutableStateOf(false) }
    var prefillEscalateReason by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = remember {
        listOf(
            "Show black leather slippers",
            "Abayas under ₦40,000",
            "Delivery fee to Abuja",
            "What payments do you accept?",
            "Track my order",
            "How do I place an order?",
            "Return & refund policy",
            "Speak with a human agent",
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ZeecrownWarmIvory),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
        ) {
            // Concierge Top Bar
            Surface(
                color = ZeecrownPureWhite,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ai_back_btn"),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = ZeecrownDeepPurple)
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ZeecrownSoftLavender),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = ZeecrownRoyalPurple,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = config.assistantName.ifBlank { "ZEECROWN CUSTOMER CARE" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ZeecrownDeepPurple,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (config.humanSupportAvailable) {
                                    "Shopping Assistance · Human Support Online (${config.supportWorkingHours})"
                                } else {
                                    "Shopping Assistance · Staff hours ${config.supportWorkingHours}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (config.humanSupportAvailable) ZeecrownSageSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSignedIn && recentConversations.isNotEmpty()) {
                            IconButton(
                                onClick = { showHistoryDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("ai_history_btn"),
                            ) {
                                Icon(Icons.Outlined.History, contentDescription = "Past Conversations", tint = ZeecrownDeepPurple)
                            }
                        }
                        IconButton(
                            onClick = onNewConversation,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ai_new_chat_btn"),
                        ) {
                            Icon(Icons.Outlined.AddComment, contentDescription = "New Conversation", tint = ZeecrownDeepPurple)
                        }
                        IconButton(
                            onClick = {
                                prefillEscalateReason = "Customer requested human concierge assistance"
                                showEscalateDialog = true
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ai_escalate_top_btn"),
                        ) {
                            Icon(Icons.Outlined.SupportAgent, contentDescription = "Speak with Human Support", tint = ZeecrownRoyalPurple)
                        }
                    }
                }
            }
            HorizontalDivider(color = ZeecrownElegantSilver)

            // Prominent Speak to a Human / Contact Store Owner WhatsApp strip
            val assistantContext = LocalContext.current
            val resolvedWaDisplay = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(storeSettings.whatsappNumber)
            Surface(color = ZeecrownLightLilac) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            launchWhatsAppChat(
                                context = assistantContext,
                                configuredNumber = storeSettings.whatsappNumber,
                                prefilledMessage = "Hello ZEECROWN STORE, I would like to speak with the store owner / a human specialist.",
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("ai_whatsapp_owner_btn"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeecrownWhatsAppGreen,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Speak to a Human · $resolvedWaDisplay",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            prefillEscalateReason = "Customer requested human concierge ticket"
                            showEscalateDialog = true
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("ai_open_ticket_dialog_btn"),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text("Ticket", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            HorizontalDivider(color = ZeecrownElegantSilver)

            if (!config.enabled) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = ZeecrownPureWhite,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = ZeecrownRoyalPurple, modifier = Modifier.size(36.dp))
                            Text("AI Concierge Temporarily Paused", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Our boutique team has temporarily paused automated chat. Chat directly with us on WhatsApp or visit the Support Center.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { launchWhatsAppChat(assistantContext, storeSettings.whatsappNumber) },
                                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownWhatsAppGreen, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Contact Store Owner on WhatsApp")
                            }
                            OutlinedButton(
                                onClick = {
                                    onNavigateAction(AiActionLink("Concierge Inbox", AiActionDestination.SUPPORT))
                                },
                                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                            ) {
                                Text("Open Human Concierge")
                            }
                        }
                    }
                }
                return@Column
            }

            // Active Handover / Error Banners
            AnimatedVisibility(visible = !escalatedTicketId.isNullOrBlank()) {
                Surface(
                    color = ZeecrownSoftLavender,
                    border = BorderStroke(1.dp, ZeecrownChampagneGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ZeecrownSageSuccess)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Forwarded to ZEECROWN Staff · #${escalatedTicketId.orEmpty().take(8).uppercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ZeecrownDeepPurple,
                            )
                            Text(
                                config.expectedResponseTimeMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { onNavigateAction(AiActionLink("Inbox", AiActionDestination.SUPPORT)) }) {
                            Text("Open Inbox")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !handoverError.isNullOrBlank()) {
                Surface(
                    color = ZeecrownErrorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Handover Could Not Be Completed",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(handoverError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ZeecrownObsidianInk)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isSignedIn) {
                                TextButton(onClick = onOpenSignIn) { Text("Sign In to Submit Ticket") }
                            }
                            TextButton(onClick = onClearHandoverError) { Text("Dismiss") }
                        }
                    }
                }
            }

            // Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = ZeecrownSoftLavender.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = ZeecrownRoyalPurple, modifier = Modifier.size(18.dp))
                                Text(
                                    config.assistantName.ifBlank { "ZEECROWN CUSTOMER CARE" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ZeecrownDeepPurple,
                                )
                            }
                            Text(
                                config.welcomeMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ZeecrownObsidianInk,
                            )
                            Text(
                                "Try asking:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(quickPrompts) { prompt ->
                                    AssistChip(
                                        onClick = { onSendMessage(prompt) },
                                        label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                                        modifier = Modifier.testTag("ai_quick_prompt"),
                                    )
                                }
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    when (msg.role) {
                        "USER" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                                    color = ZeecrownRoyalPurple,
                                    contentColor = ZeecrownPureWhite,
                                    modifier = Modifier.widthIn(max = 520.dp),
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                        "SYSTEM" -> {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = ZeecrownSoftLavender,
                                border = BorderStroke(1.dp, ZeecrownChampagneGold),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        "Concierge Handover Recorded",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ZeecrownDeepPurple,
                                    )
                                    Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = ZeecrownObsidianInk)
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                                    color = ZeecrownPureWhite,
                                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                    modifier = Modifier.widthIn(max = 580.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = config.assistantName.ifBlank { "ZEECROWN CUSTOMER CARE" },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = ZeecrownRoyalPurple,
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = ZeecrownLightLilac,
                                            ) {
                                                Text(
                                                    text = msg.detectedCategory,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                )
                                            }
                                        }

                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ZeecrownObsidianInk,
                                        )

                                        if (msg.autoOfferGuestTracker) {
                                            InlineGuestOrderLookupCard(
                                                isBusy = isSending,
                                                onSubmitLookup = onLookupGuestOrder,
                                            )
                                        }

                                        if (msg.actionLinks.isNotEmpty() || msg.suggestHumanHandover) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                msg.actionLinks.forEach { link ->
                                                    OutlinedButton(
                                                        onClick = { onNavigateAction(link) },
                                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                                        border = BorderStroke(1.dp, ZeecrownRoyalPurple),
                                                    ) {
                                                        Text(link.label, style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                                if (msg.suggestHumanHandover && escalatedTicketId.isNullOrBlank()) {
                                                    Button(
                                                        onClick = {
                                                            prefillEscalateReason = msg.escalationReason.ifBlank { "Customer needs human assistance" }
                                                            showEscalateDialog = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = ZeecrownRoyalPurple,
                                                            contentColor = ZeecrownPureWhite,
                                                        ),
                                                        modifier = Modifier
                                                            .minimumInteractiveComponentSize()
                                                            .testTag("ai_escalate_cta"),
                                                    ) {
                                                        Icon(Icons.Outlined.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Escalate to Human Support")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (msg.recommendedProducts.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        items(msg.recommendedProducts, key = { "${msg.id}_${it.productId}" }) { card ->
                                            AiRecommendedProductCard(
                                                card = card,
                                                currency = currency,
                                                onClick = { onProductClick(card.productId) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isSending) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ZeecrownPureWhite,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ZeecrownRoyalPurple,
                                )
                                Text(
                                    "Checking live ZEECROWN catalogue & store policies...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Helpful confirmation strip once conversation is active
            if (messages.any { it.role == "ASSISTANT" } && escalatedTicketId.isNullOrBlank()) {
                Surface(
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (markedHelpful) "Marked as resolved & helpful. Thank you!" else "Did this answer your question?",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZeecrownDeepPurple,
                            modifier = Modifier.weight(1f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!markedHelpful) {
                                TextButton(
                                    onClick = onMarkHelpful,
                                    modifier = Modifier.testTag("ai_mark_helpful_btn"),
                                ) {
                                    Icon(Icons.Outlined.ThumbUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Yes, Helpful")
                                }
                            }
                            TextButton(
                                onClick = {
                                    prefillEscalateReason = "Customer requested human concierge follow-up"
                                    showEscalateDialog = true
                                },
                                modifier = Modifier.testTag("ai_need_human_btn"),
                            ) {
                                Text("Talk to Staff")
                            }
                        }
                    }
                }
            }

            // Message Composer
            Surface(
                color = ZeecrownPureWhite,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, ZeecrownElegantSilver),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draftInput,
                        onValueChange = { draftInput = it },
                        placeholder = { Text("Ask about Abayas, Adire, prices, sizes, delivery, or orders...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_chat_input"),
                        maxLines = 3,
                    )
                    Button(
                        onClick = {
                            val text = draftInput.trim()
                            if (text.isNotBlank()) {
                                draftInput = ""
                                onSendMessage(text)
                            }
                        },
                        enabled = draftInput.isNotBlank() && !isSending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeecrownRoyalPurple,
                            contentColor = ZeecrownPureWhite,
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("ai_send_btn"),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send Message")
                    }
                }
            }
        }
    }

    if (showEscalateDialog) {
        AiHumanHandoverDialog(
            isSignedIn = isSignedIn,
            isSubmitting = isSubmittingHandover,
            defaultReason = prefillEscalateReason,
            humanAvailable = config.humanSupportAvailable,
            workingHoursNote = "${config.supportWorkingDays} · ${config.supportWorkingHours}",
            storeSettings = storeSettings,
            onDismiss = { showEscalateDialog = false },
            onSignIn = {
                showEscalateDialog = false
                onOpenSignIn()
            },
            onConfirmEscalate = { reason, cat, orderNum, note ->
                onEscalateToHuman(reason, cat, orderNum, note)
                showEscalateDialog = false
            },
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Previous AI Concierge Chats") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentConversations.take(8).forEach { conv ->
                        Surface(
                            onClick = {
                                showHistoryDialog = false
                                onOpenConversationHistory(conv)
                            },
                            shape = MaterialTheme.shapes.small,
                            color = ZeecrownLightLilac,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(conv.primaryCategory, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    conv.lastUserMessage.ifBlank { "Saved concierge conversation" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun AiRecommendedProductCard(
    card: AiProductCard,
    currency: StoreCurrency,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(172.dp)
            .minimumInteractiveComponentSize()
            .testTag("ai_product_card_${card.productId}"),
        shape = MaterialTheme.shapes.medium,
        color = ZeecrownPureWhite,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
        shadowElevation = 2.dp,
    ) {
        Column {
            ZeecrownProductImage(
                imageUrl = card.imageUrl,
                contentDescription = card.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            )
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (card.categoryName.isNotBlank()) {
                    Text(
                        text = card.categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ZeecrownRoyalPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ZeecrownObsidianInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = formatBoutiquePrice(card.priceNgn, card.priceUsd, currency),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ZeecrownDeepPurple,
                    )
                    val compare = card.compareAtPriceNgn
                    if (currency == StoreCurrency.NGN && compare != null && compare > card.priceNgn) {
                        Text(
                            text = formatNgnAmount(compare),
                            style = MaterialTheme.typography.labelSmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = if (card.stockQuantity > 0) "${card.stockQuantity} available" else "Out of stock",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (card.stockQuantity > 0) ZeecrownSageSuccess else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun InlineGuestOrderLookupCard(
    isBusy: Boolean,
    onSubmitLookup: (orderNumber: String, contact: String, token: String) -> Unit,
) {
    var orderNum by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = ZeecrownLightLilac,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp), tint = ZeecrownDeepPurple)
                Text("Verify Your Order Privately", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(
                value = orderNum,
                onValueChange = { orderNum = it },
                label = { Text("Order Number (e.g. ZC-1024)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("ai_guest_track_order_input"),
            )
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Checkout Email or Phone Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("ai_guest_track_contact_input"),
            )
            Button(
                onClick = { onSubmitLookup(orderNum, contact, "") },
                enabled = !isBusy && orderNum.trim().length >= 3 && contact.trim().length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("ai_guest_track_submit"),
            ) {
                Text("Verify & Check Status")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiHumanHandoverDialog(
    isSignedIn: Boolean,
    isSubmitting: Boolean,
    defaultReason: String,
    humanAvailable: Boolean,
    workingHoursNote: String,
    storeSettings: StoreSettings,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onConfirmEscalate: (reason: String, category: SupportCategory, orderNumber: String, note: String) -> Unit,
) {
    var category by remember { mutableStateOf(SupportCategory.GENERAL) }
    var orderNumber by remember { mutableStateOf("") }
    var extraNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escalate to ZEECROWN Staff") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!humanAvailable) {
                    Surface(
                        color = ZeecrownSoftLavender,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, ZeecrownChampagneGold),
                    ) {
                        Text(
                            text = "Human staff are currently offline ($workingHoursNote). Submitting creates a priority inbox ticket with your chat history so staff reply as soon as they open.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                } else {
                    Text(
                        "Your conversation summary will be transferred to our human concierge inbox ($workingHoursNote).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (!isSignedIn) {
                    Surface(
                        color = ZeecrownSoftLavender.copy(alpha = 0.65f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Sign in to link this handover to your account inbox for live staff replies, or message the store owner directly on WhatsApp.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            val resolvedWaDisplay = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(storeSettings.whatsappNumber)
                            val direct = listOfNotNull(
                                storeSettings.supportEmail.takeIf { it.isNotBlank() }?.let { "Email: $it" },
                                "WhatsApp: $resolvedWaDisplay",
                            ).joinToString(" · ")
                            Text("Direct contact: $direct", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Text("Topic", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SupportCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = orderNumber,
                        onValueChange = { orderNumber = it },
                        label = { Text("Order Number (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ai_escalate_order_input"),
                    )
                    OutlinedTextField(
                        value = extraNote,
                        onValueChange = { extraNote = it },
                        label = { Text("Additional Details for Staff") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("ai_escalate_note_input"),
                    )
                }

                val dialogContext = LocalContext.current
                val resolvedWaDisplay = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(storeSettings.whatsappNumber)
                Button(
                    onClick = {
                        launchWhatsAppChat(
                            context = dialogContext,
                            configuredNumber = storeSettings.whatsappNumber,
                            prefilledMessage = "Hello ZEECROWN STORE, I would like to speak to the store owner regarding: $defaultReason",
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("ai_dialog_whatsapp_btn"),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownWhatsAppGreen, contentColor = Color.White),
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Contact Store Owner on WhatsApp · $resolvedWaDisplay")
                }
            }
        },
        confirmButton = {
            if (isSignedIn) {
                Button(
                    onClick = { onConfirmEscalate(defaultReason, category, orderNumber, extraNote) },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    modifier = Modifier.testTag("ai_escalate_confirm_btn"),
                ) {
                    Text(if (isSubmitting) "Sending..." else "Confirm Handover")
                }
            } else {
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Sign In & Continue") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
