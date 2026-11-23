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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

@Composable
fun AdminTeamAndAuditTab(
    role: AdminRole,
    teamState: UiState<List<AdminUser>>,
    logsState: UiState<List<AdminActivityLog>>,
    isBusy: Boolean,
    onSaveTeamMember: (
        uid: String,
        email: String,
        displayName: String,
        role: AdminRole,
        active: Boolean,
    ) -> Unit,
) {
    val team = (teamState as? UiState.Success)?.data.orEmpty()
    val logs = (logsState as? UiState.Success)?.data.orEmpty()

    var targetUid by remember { mutableStateOf("") }
    var targetEmail by remember { mutableStateOf("") }
    var targetName by remember { mutableStateOf("") }
    var selectedStaffRole by remember { mutableStateOf(AdminRole.STORE_MANAGER) }
    var active by remember { mutableStateOf(true) }
    var logFilterQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, logFilterQuery) {
        val q = logFilterQuery.trim().lowercase()
        if (q.isBlank()) logs
        else logs.filter {
            it.action.lowercase().contains(q) ||
                it.summary.lowercase().contains(q) ||
                it.adminEmail.lowercase().contains(q) ||
                it.targetType.lowercase().contains(q)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (role.canManageTeam) {
            item {
                AdminSectionCard {
                    Text("Admin Team & RBAC Assignment", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Assign granular staff roles to signed-up Firebase Auth users by their User UID & email. Super Admin control is enforced in Firestore security rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = targetUid,
                        onValueChange = { targetUid = it },
                        label = { Text("Firebase User UID *") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_team_uid_input"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = targetEmail,
                        onValueChange = { targetEmail = it },
                        label = { Text("Staff Email *") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_team_email_input"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = targetName,
                        onValueChange = { targetName = it },
                        label = { Text("Staff Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text("Staff Role", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AdminRole.assignableRoles) { r ->
                            FilterChip(
                                selected = selectedStaffRole == r,
                                onClick = { selectedStaffRole = r },
                                label = { Text(r.label) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Account Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                    Button(
                        onClick = {
                            onSaveTeamMember(targetUid, targetEmail, targetName, selectedStaffRole, active)
                            targetUid = ""
                            targetEmail = ""
                            targetName = ""
                        },
                        enabled = !isBusy && targetUid.isNotBlank() && targetEmail.contains("@"),
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("admin_save_team_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) { Text("Assign / Update Admin Role") }
                }
            }
        }

        item {
            AdminSectionCard {
                Text("Role Permissions Matrix", style = MaterialTheme.typography.titleMedium)
                val matrix = listOf(
                    "Super Admin" to "Full store control, Team RBAC, Settings, Audit, Orders, Payments & Catalog",
                    "Store Manager" to "Catalog, Categories, Inventory, Orders, Payments, Customers, Delivery & Reports",
                    "Order Manager" to "Orders, Bank Transfer Payment Verification, Customers & Reports",
                    "Content Manager" to "Homepage Banners, Categories, Promo Codes, Collections & Review Moderation",
                    "Support Staff" to "Customer Directory, Support Notes, Review Moderation & Audit Logs",
                )
                matrix.forEach { (rTitle, scopeText) ->
                    Text(rTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(scopeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = ZeecrownElegantSilver)
                }
            }
        }

        if (role.canManageTeam) {
            item { Text("Staff Directory (${team.size})", style = MaterialTheme.typography.titleMedium) }
            when (teamState) {
                is UiState.Loading -> item { BoutiqueLoadingView("Loading admin team...") }
                is UiState.Error -> item { BoutiqueErrorBanner(teamState.message) }
                is UiState.Success -> {
                    items(team, key = { it.id }) { member ->
                        val memberRole = AdminRole.fromRaw(member.role)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(member.displayName.ifBlank { member.email }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${member.email} · UID: ${member.userId.ifBlank { member.id }}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AdminStatusPill(label = memberRole.label, tone = PillTone.GOLD)
                                        AdminStatusPill(label = if (member.active) "ACTIVE" else "DISABLED", tone = if (member.active) PillTone.SUCCESS else PillTone.ERROR)
                                    }
                                }
                                OutlinedButton(onClick = {
                                    targetUid = member.userId.ifBlank { member.id }
                                    targetEmail = member.email
                                    targetName = member.displayName
                                    selectedStaffRole = if (memberRole.isAdmin) memberRole else AdminRole.SUPPORT_STAFF
                                    active = member.active
                                }) { Text("Edit") }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Immutable Activity Audit Trail (${logs.size})", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = logFilterQuery,
                    onValueChange = { logFilterQuery = it },
                    label = { Text("Filter audit log (action, staff email, summary)") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        when (logsState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading activity logs...") }
            is UiState.Error -> item { BoutiqueErrorBanner(logsState.message) }
            is UiState.Success -> {
                if (filteredLogs.isEmpty()) {
                    item { BoutiqueEmptyView(title = "No Audit Entries Yet", subtitle = "Every admin change creates an immutable audit record here.") }
                } else {
                    items(filteredLogs.take(60), key = { it.id }) { log ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    AdminStatusPill(label = log.adminRole, tone = PillTone.GOLD)
                                }
                                Text(log.summary, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${log.adminEmail} · Target: ${log.targetType}/${log.targetId} · ${formatAdminTimestamp(log.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZeecrownGoldInk,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
