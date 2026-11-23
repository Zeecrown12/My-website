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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.AiAnalyticsSummary
import com.example.data.model.AiAssistantConfig
import com.example.data.model.AiChatMessage
import com.example.data.model.AiFaqEntry
import com.example.data.model.AiToneStyle
import com.example.data.model.AiUnansweredQuestion
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownWarmIvory
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAiAssistantTab(
    aiConfigState: UiState<AiAssistantConfig>,
    aiFaqsState: UiState<List<AiFaqEntry>>,
    aiUnansweredState: UiState<List<AiUnansweredQuestion>>,
    aiAnalyticsSummary: AiAnalyticsSummary,
    sandboxTestReply: AiChatMessage?,
    sandboxTestRunning: Boolean,
    liveProductsCount: Int,
    liveCategoriesCount: Int,
    liveZonesCount: Int,
    isBusy: Boolean,
    onSaveConfig: (AiAssistantConfig) -> Unit,
    onSaveFaq: (String?, AiFaqEntry, () -> Unit) -> Unit,
    onDeleteFaq: (String) -> Unit,
    onPromoteUnanswered: (AiUnansweredQuestion, String, String, String, () -> Unit) -> Unit,
    onDismissUnanswered: (String) -> Unit,
    onRunSandboxTest: (String, AiAssistantConfig) -> Unit,
) {
    val loadedConfig = (aiConfigState as? UiState.Success)?.data ?: AiAssistantConfig()
    val faqs = (aiFaqsState as? UiState.Success)?.data.orEmpty()
    val unanswered = (aiUnansweredState as? UiState.Success)?.data.orEmpty()
    val openUnanswered = remember(unanswered) { unanswered.filter { it.status == "OPEN" } }

    var activeSubTab by remember { mutableIntStateOf(0) }
    var draftConfig by remember(loadedConfig.updatedAt, loadedConfig.assistantName, loadedConfig.enabled) {
        mutableStateOf(loadedConfig)
    }

    var editingFaq by remember { mutableStateOf<AiFaqEntry?>(null) }
    var showNewFaqDialog by remember { mutableStateOf(false) }
    var promotingItem by remember { mutableStateOf<AiUnansweredQuestion?>(null) }
    var sandboxPrompt by remember { mutableStateOf("Do you have black leather slippers under ₦25,000?") }

    val subTabs = listOf(
        "Controls & Hours",
        "Knowledge & FAQs (${faqs.size})",
        "Unanswered (${openUnanswered.size})",
        "Test Sandbox",
        "Analytics",
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = ZeecrownDeepPurple)
                    Column {
                        Text("ZEECROWN AI Assistant Management", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Owner controls for storefront AI chat, human handover hours, verified FAQs & analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Clear separation badge bar
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("3-Source Truth Architecture", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "1. Live Store Data (Synced Automatically): $liveProductsCount products, $liveCategoriesCount categories, $liveZonesCount delivery zones, payments & policies.\n" +
                                "2. Owner Knowledge & FAQs (Admin-Managed): ${faqs.count { it.active }} active verified FAQ answers.\n" +
                                "3. AI-Generated Responses: Composed by Gemini 3.8 Flash (App Check) or grounded fallback — never invented pricing or fake handovers.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    subTabs.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = activeSubTab == idx,
                            onClick = { activeSubTab = idx },
                            label = { Text(label) },
                            modifier = Modifier.testTag("admin_ai_subtab_$idx"),
                        )
                    }
                }
            }
        }

        when (activeSubTab) {
            0 -> {
                item {
                    AdminSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable AI Shopping Assistant", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Turn storefront & Android AI Concierge on or off immediately.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = draftConfig.enabled,
                                onCheckedChange = { draftConfig = draftConfig.copy(enabled = it) },
                                modifier = Modifier.testTag("admin_ai_enable_switch"),
                            )
                        }

                        HorizontalDivider(color = ZeecrownElegantSilver)

                        OutlinedTextField(
                            value = draftConfig.assistantName,
                            onValueChange = { draftConfig = draftConfig.copy(assistantName = it) },
                            label = { Text("Assistant Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_ai_name_input"),
                        )
                        OutlinedTextField(
                            value = draftConfig.welcomeMessage,
                            onValueChange = { draftConfig = draftConfig.copy(welcomeMessage = it) },
                            label = { Text("Customer Welcome Message") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text("Response Tone & Style", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AiToneStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = AiToneStyle.fromRaw(draftConfig.toneStyle) == style,
                                    onClick = { draftConfig = draftConfig.copy(toneStyle = style.name) },
                                    label = { Text(style.label) },
                                )
                            }
                        }

                        OutlinedTextField(
                            value = draftConfig.customInstructions,
                            onValueChange = { draftConfig = draftConfig.copy(customInstructions = it) },
                            label = { Text("Custom Store Guidance & Brand Instructions") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draftConfig.policyGuidance,
                            onValueChange = { draftConfig = draftConfig.copy(policyGuidance = it) },
                            label = { Text("Additional Approved Policy Notes") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draftConfig.escalationRulesText,
                            onValueChange = { draftConfig = draftConfig.copy(escalationRulesText = it) },
                            label = { Text("Human Escalation Rules") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draftConfig.humanHandoverMessage,
                            onValueChange = { draftConfig = draftConfig.copy(humanHandoverMessage = it) },
                            label = { Text("Handover Confirmation Message") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    AdminSectionCard {
                        Text("Human Availability & Support Hours", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Human Support Online Now", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "When off, AI informs customers staff are away and records priority inbox handovers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = draftConfig.humanSupportAvailable,
                                onCheckedChange = { draftConfig = draftConfig.copy(humanSupportAvailable = it) },
                                modifier = Modifier.testTag("admin_ai_human_switch"),
                            )
                        }
                        OutlinedTextField(
                            value = draftConfig.supportWorkingDays,
                            onValueChange = { draftConfig = draftConfig.copy(supportWorkingDays = it) },
                            label = { Text("Working Days") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draftConfig.supportWorkingHours,
                            onValueChange = { draftConfig = draftConfig.copy(supportWorkingHours = it) },
                            label = { Text("Working Hours") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draftConfig.preferredContactChannels,
                            onValueChange = { draftConfig = draftConfig.copy(preferredContactChannels = it) },
                            label = { Text("Preferred Contact Channels") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draftConfig.expectedResponseTimeMessage,
                            onValueChange = { draftConfig = draftConfig.copy(expectedResponseTimeMessage = it) },
                            label = { Text("Expected Human Response Time Notice") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Notify admins on AI escalation", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = draftConfig.escalationNotificationsEnabled,
                                onCheckedChange = { draftConfig = draftConfig.copy(escalationNotificationsEnabled = it) },
                            )
                        }
                        Button(
                            onClick = { onSaveConfig(draftConfig) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("admin_ai_save_config_btn"),
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save AI Assistant Settings")
                        }
                    }
                }
            }

            1 -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Verified Owner Knowledge & FAQs", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = { showNewFaqDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_ai_add_faq_btn"),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add FAQ")
                        }
                    }
                }
                when (aiFaqsState) {
                    is UiState.Loading -> item { BoutiqueLoadingView("Loading AI FAQs...") }
                    is UiState.Error -> item { BoutiqueErrorBanner(aiFaqsState.message) }
                    is UiState.Success -> {
                        if (faqs.isEmpty()) {
                            item {
                                BoutiqueEmptyView(
                                    title = "No Custom FAQs Added Yet",
                                    subtitle = "Add verified answers for custom boutique sizing, gift wrapping, event orders, or regional care instructions.",
                                )
                            }
                        } else {
                            items(faqs, key = { it.id }) { faq ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                    label = if (faq.active) "Active FAQ" else "Draft",
                                                    tone = if (faq.active) PillTone.SUCCESS else PillTone.NEUTRAL,
                                                )
                                                AdminStatusPill(label = faq.category, tone = PillTone.GOLD)
                                            }
                                            Row {
                                                IconButton(onClick = { editingFaq = faq }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit FAQ")
                                                }
                                                IconButton(onClick = { onDeleteFaq(faq.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete FAQ")
                                                }
                                            }
                                        }
                                        Text(faq.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(faq.answer, style = MaterialTheme.typography.bodySmall)
                                        if (faq.keywords.isNotEmpty()) {
                                            Text(
                                                "Keywords: ${faq.keywords.joinToString()}",
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
            }

            2 -> {
                item {
                    Text(
                        "Unanswered & Frequently Escalated Customer Questions",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                when (aiUnansweredState) {
                    is UiState.Loading -> item { BoutiqueLoadingView("Loading customer question queue...") }
                    is UiState.Error -> item { BoutiqueErrorBanner(aiUnansweredState.message) }
                    is UiState.Success -> {
                        if (openUnanswered.isEmpty()) {
                            item {
                                BoutiqueEmptyView(
                                    title = "No Unanswered Questions Pending",
                                    subtitle = "Questions that lack exact store information or trigger escalation appear here so you can promote verified FAQ answers.",
                                )
                            }
                        } else {
                            items(openUnanswered, key = { it.id }) { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = ZeecrownLightLilac,
                                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            AdminStatusPill(label = "${item.occurrenceCount}x asked", tone = PillTone.ROSE)
                                            AdminStatusPill(label = item.reason, tone = PillTone.NEUTRAL)
                                        }
                                        Text(item.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { promotingItem = item },
                                                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                                                modifier = Modifier.minimumInteractiveComponentSize(),
                                            ) { Text("Verify & Save to FAQ") }
                                            OutlinedButton(
                                                onClick = { onDismissUnanswered(item.id) },
                                                enabled = !isBusy,
                                                modifier = Modifier.minimumInteractiveComponentSize(),
                                            ) { Text("Dismiss") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                item {
                    AdminSectionCard {
                        Text("Test Assistant Console (Live Store Preview)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Preview how your current draft settings, live catalog, and FAQs answer any customer prompt before saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = sandboxPrompt,
                            onValueChange = { sandboxPrompt = it },
                            label = { Text("Sample Customer Prompt") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_ai_sandbox_input"),
                            minLines = 2,
                        )
                        Button(
                            onClick = { onRunSandboxTest(sandboxPrompt, draftConfig) },
                            enabled = !sandboxTestRunning && sandboxPrompt.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_ai_sandbox_run_btn"),
                        ) {
                            if (sandboxTestRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ZeecrownPureWhite)
                                Spacer(Modifier.width(8.dp))
                                Text("Testing...")
                            } else {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Run Live Test")
                            }
                        }
                        sandboxTestReply?.let { reply ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = ZeecrownWarmIvory,
                                border = BorderStroke(1.dp, ZeecrownElegantSilver),
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AdminStatusPill(label = reply.responseSource.name, tone = PillTone.GOLD)
                                        AdminStatusPill(label = reply.detectedCategory, tone = PillTone.NEUTRAL)
                                    }
                                    Text(reply.text, style = MaterialTheme.typography.bodyMedium)
                                    if (reply.recommendedProducts.isNotEmpty()) {
                                        Text(
                                            "Matched Products: ${reply.recommendedProducts.joinToString { "${it.title} (₦${it.priceNgn.toLong()})" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ZeecrownSageSuccess,
                                        )
                                    }
                                    if (reply.suggestHumanHandover) {
                                        Text(
                                            "Auto-Escalation Triggered: ${reply.escalationReason}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                item {
                    AdminSectionCard {
                        Text("Measured AI Support Metrics", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Note: 'Resolved by AI' counts only conversations explicitly confirmed helpful — never abandoned chats.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AdminMetricCard("Conversations", aiAnalyticsSummary.totalConversations.toString(), modifier = Modifier.width(145.dp))
                            AdminMetricCard("Confirmed AI Resolved", aiAnalyticsSummary.aiResolvedHelpfulCount.toString(), accentColor = ZeecrownSageSuccess, modifier = Modifier.width(160.dp))
                            AdminMetricCard("Escalated to Staff", aiAnalyticsSummary.escalatedCount.toString(), accentColor = MaterialTheme.colorScheme.error, modifier = Modifier.width(150.dp))
                            AdminMetricCard("Resolved by Staff", aiAnalyticsSummary.resolvedByStaffCount.toString(), modifier = Modifier.width(150.dp))
                            AdminMetricCard("Unresolved / Open", aiAnalyticsSummary.activeOrUnresolvedCount.toString(), modifier = Modifier.width(150.dp))
                            AdminMetricCard("Rec. Product Clicks", aiAnalyticsSummary.recommendationClickCount.toString(), modifier = Modifier.width(155.dp))
                            AdminMetricCard("Avg Latency", "${aiAnalyticsSummary.measuredAvgLatencyMs} ms", modifier = Modifier.width(145.dp))
                            AdminMetricCard("AI Call Fallbacks", aiAnalyticsSummary.failedAiCallsCount.toString(), modifier = Modifier.width(145.dp))
                        }

                        if (aiAnalyticsSummary.topQuestionCategories.isNotEmpty()) {
                            HorizontalDivider(color = ZeecrownElegantSilver)
                            Text("Top Question Categories", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            aiAnalyticsSummary.topQuestionCategories.forEach { (cat, count) ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(cat, style = MaterialTheme.typography.bodySmall)
                                    Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    AdminSectionCard(containerColor = ZeecrownLightLilac) {
                        Text("Estimated Usage & Cost Metrics (Heuristic Estimate)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Token counts (~4 chars/token) and estimated USD spend below are approximations for budget monitoring, not official billing invoices.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AdminMetricCard("Est. Prompt Tokens", aiAnalyticsSummary.estimatedTotalPromptTokens.toString(), modifier = Modifier.width(160.dp))
                            AdminMetricCard("Est. Reply Tokens", aiAnalyticsSummary.estimatedTotalCompletionTokens.toString(), modifier = Modifier.width(160.dp))
                            AdminMetricCard(
                                "Est. Cost (USD)",
                                "$${String.format(Locale.US, "%.4f", aiAnalyticsSummary.estimatedTotalCostUsd)}",
                                subtitle = "Heuristic estimate",
                                modifier = Modifier.width(160.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFaqDialog || editingFaq != null) {
        val target = editingFaq
        var q by remember(target?.id) { mutableStateOf(target?.question.orEmpty()) }
        var a by remember(target?.id) { mutableStateOf(target?.answer.orEmpty()) }
        var cat by remember(target?.id) { mutableStateOf(target?.category.orEmpty().ifBlank { "General" }) }
        var kw by remember(target?.id) { mutableStateOf(target?.keywords?.joinToString(", ").orEmpty()) }
        var active by remember(target?.id) { mutableStateOf(target?.active ?: true) }

        AlertDialog(
            onDismissRequest = {
                showNewFaqDialog = false
                editingFaq = null
            },
            title = { Text(if (target == null) "Add Verified AI FAQ" else "Edit AI FAQ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text("Customer Question *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("Verified Store Answer *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = kw, onValueChange = { kw = it }, label = { Text("Match Keywords (comma-separated)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Active in Assistant")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveFaq(
                            target?.id,
                            AiFaqEntry(
                                id = target?.id.orEmpty(),
                                question = q,
                                answer = a,
                                category = cat,
                                keywords = kw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() },
                                active = active,
                            ),
                        ) {
                            showNewFaqDialog = false
                            editingFaq = null
                        }
                    },
                    enabled = !isBusy && q.trim().length >= 3 && a.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Save FAQ") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewFaqDialog = false
                    editingFaq = null
                }) { Text("Cancel") }
            },
        )
    }

    promotingItem?.let { item ->
        var answer by remember(item.id) { mutableStateOf("") }
        var category by remember(item.id) { mutableStateOf(item.category.ifBlank { "General" }) }
        var kw by remember(item.id) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { promotingItem = null },
            title = { Text("Promote Question to Verified FAQ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Verified Official Answer *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = kw, onValueChange = { kw = it }, label = { Text("Keywords (optional, comma-separated)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPromoteUnanswered(item, answer, category, kw) {
                            promotingItem = null
                        }
                    },
                    enabled = !isBusy && answer.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Publish to FAQ") }
            },
            dismissButton = {
                TextButton(onClick = { promotingItem = null }) { Text("Cancel") }
            },
        )
    }
}
