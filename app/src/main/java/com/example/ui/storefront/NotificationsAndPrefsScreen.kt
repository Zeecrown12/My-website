package com.example.ui.storefront

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.firebase.UiState
import com.example.data.model.AppNotification
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationPreferences
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationsCenterScreen(
    isSignedIn: Boolean,
    notificationsState: UiState<List<AppNotification>>,
    onBack: () -> Unit,
    onOpenPreferences: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onOpenSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var categoryFilter by remember { mutableStateOf<NotificationCategory?>(null) }
    var inspectingNotification by remember { mutableStateOf<AppNotification?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = 880.dp).fillMaxSize()) {
            Surface(color = ZeecrownWarmIvory, border = BorderStroke(1.dp, ZeecrownElegantSilver)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                        Text("Notification Center", style = MaterialTheme.typography.titleLarge, color = ZeecrownDeepPurple)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSignedIn) {
                            IconButton(
                                onClick = onMarkAllRead,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("mark_all_notifications_read"),
                            ) {
                                Icon(Icons.Outlined.DoneAll, contentDescription = "Mark all notifications read")
                            }
                            IconButton(
                                onClick = onOpenPreferences,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("open_notif_prefs_button"),
                            ) {
                                Icon(Icons.Outlined.Tune, contentDescription = "Notification Preferences")
                            }
                        }
                    }
                }
            }

            if (!isSignedIn) {
                BoutiqueEmptyView(
                    title = "Sign In to View Your Alerts",
                    subtitle = "Order confirmations, payment verifications, dispatch updates, and support replies sync here for signed-in accounts.",
                    actionLabel = "Sign In",
                    onAction = onOpenSignIn,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = categoryFilter == null,
                            onClick = { categoryFilter = null },
                            label = { Text("All") },
                        )
                    }
                    items(
                        listOf(
                            NotificationCategory.ORDER,
                            NotificationCategory.PAYMENT,
                            NotificationCategory.DELIVERY,
                            NotificationCategory.SUPPORT,
                            NotificationCategory.PROMOTION,
                            NotificationCategory.SECURITY,
                        )
                    ) { cat ->
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                            label = { Text(cat.label) },
                        )
                    }
                }

                when (notificationsState) {
                    is UiState.Loading -> BoutiqueLoadingView("Loading boutique notifications...", modifier = Modifier.padding(20.dp))
                    is UiState.Error -> BoutiqueErrorBanner(notificationsState.message, modifier = Modifier.padding(16.dp))
                    is UiState.Success -> {
                        val filtered = remember(notificationsState.data, categoryFilter) {
                            val list = notificationsState.data
                            if (categoryFilter == null) list
                            else list.filter { NotificationCategory.fromRaw(it.category) == categoryFilter }
                        }
                        if (filtered.isEmpty()) {
                            BoutiqueEmptyView(
                                title = "You're All Caught Up",
                                subtitle = "No notifications match this filter yet.",
                                modifier = Modifier.padding(24.dp),
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(filtered, key = { it.id }) { notif ->
                                    val dateStr = remember(notif.createdAt) {
                                        notif.createdAt?.toDate()?.let {
                                            SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(it)
                                        } ?: "Just now"
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!notif.readFlag) onMarkRead(notif.id)
                                                inspectingNotification = notif
                                            }
                                            .testTag("notif_card_${notif.id}"),
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (!notif.readFlag) ZeecrownSoftLavender.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.surfaceContainerLowest,
                                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = ZeecrownLightLilac) {
                                                    Text(
                                                        NotificationCategory.fromRaw(notif.category).label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    )
                                                }
                                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                notif.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (!notif.readFlag) FontWeight.Bold else FontWeight.Medium,
                                            )
                                            Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (notif.orderNumber.isNotBlank() || !notif.readFlag) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    if (notif.orderNumber.isNotBlank()) {
                                                        Text("Order ${notif.orderNumber}", style = MaterialTheme.typography.labelSmall, color = ZeecrownGoldInk)
                                                    } else Spacer(Modifier.width(1.dp))
                                                    if (!notif.readFlag) {
                                                        Text("Unread", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ZeecrownDeepPurple)
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
        }
    }

    inspectingNotification?.let { notif ->
        AlertDialog(
            onDismissRequest = { inspectingNotification = null },
            title = { Text(notif.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(notif.body, style = MaterialTheme.typography.bodyMedium)
                    if (notif.details.isNotBlank()) {
                        HorizontalDivider(color = ZeecrownElegantSilver)
                        Text(notif.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                if (notif.targetRoute.isNotBlank()) {
                    Button(
                        onClick = {
                            val route = notif.targetRoute
                            inspectingNotification = null
                            onNavigateRoute(route)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) { Text("View Details") }
                } else {
                    TextButton(onClick = { inspectingNotification = null }) { Text("Done") }
                }
            },
            dismissButton = {
                if (notif.targetRoute.isNotBlank()) {
                    TextButton(onClick = { inspectingNotification = null }) { Text("Close") }
                }
            },
        )
    }
}

@Composable
fun NotificationPreferencesScreen(
    preferencesState: UiState<NotificationPreferences>,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSavePreferences: (NotificationPreferences) -> Unit,
    onRequestPushPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val current = (preferencesState as? UiState.Success)?.data ?: NotificationPreferences()

    var orderUpdatesEmail by remember { mutableStateOf(true) }
    var promotionalEmails by remember { mutableStateOf(false) }
    var promotionalPush by remember { mutableStateOf(true) }
    var smsOrderAlerts by remember { mutableStateOf(false) }
    var whatsappOrderAlerts by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        orderUpdatesEmail = current.orderUpdatesEmail
        promotionalEmails = current.promotionalEmails
        promotionalPush = current.promotionalPush
        smsOrderAlerts = current.smsOrderAlerts
        whatsappOrderAlerts = current.whatsappOrderAlerts
    }

    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = 880.dp).fillMaxSize()) {
            Surface(color = ZeecrownWarmIvory, border = BorderStroke(1.dp, ZeecrownElegantSilver)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Text("Notification Preferences", style = MaterialTheme.typography.titleLarge, color = ZeecrownDeepPurple)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = if (hasNotificationPermission) ZeecrownSageSuccess.copy(alpha = 0.12f) else ZeecrownLightLilac,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    if (hasNotificationPermission) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                )
                                Column {
                                    Text(
                                        if (hasNotificationPermission) "Push Notifications Enabled" else "Push Permission Needed",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        if (hasNotificationPermission) "Order & delivery alerts will appear in your Android status bar."
                                        else "Allow notifications so courier & verification alerts reach your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            if (!hasNotificationPermission) {
                                OutlinedButton(onClick = onRequestPushPermission, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Text("Enable")
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = ZeecrownLightLilac,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = ZeecrownDeepPurple)
                                Column {
                                    Text("Essential Account & Security Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Locked ON — protects payment confirmations, password resets & account verification.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Switch(checked = true, onCheckedChange = null, enabled = false)
                        }
                    }
                }

                item {
                    PreferenceToggleRow("Order & Delivery Emails", "Order confirmation, courier tracking & delivery receipts via email.", orderUpdatesEmail) {
                        orderUpdatesEmail = it
                    }
                }
                item {
                    PreferenceToggleRow("Promotional Push Alerts", "Optional alerts for curated new arrivals & seasonal boutique drops.", promotionalPush) {
                        promotionalPush = it
                    }
                }
                item {
                    PreferenceToggleRow("Promotional Style Emails", "Exclusive editorial lookbooks and member offers.", promotionalEmails) {
                        promotionalEmails = it
                    }
                }
                item {
                    PreferenceToggleRow("SMS Order Alerts", "Important order & courier transit SMS when enabled by store.", smsOrderAlerts) {
                        smsOrderAlerts = it
                    }
                }
                item {
                    PreferenceToggleRow("WhatsApp Order Concierge", "Receive approved order & dispatch notifications on WhatsApp.", whatsappOrderAlerts) {
                        whatsappOrderAlerts = it
                    }
                }

                item {
                    val consentText = remember(current.consentUpdatedAt) {
                        current.consentUpdatedAt?.toDate()?.let {
                            "Last consent audit: " + SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(it)
                        } ?: "Consent audit timestamp saves automatically on update."
                    }
                    Text(consentText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            onSavePreferences(
                                current.copy(
                                    orderUpdatesEmail = orderUpdatesEmail,
                                    promotionalEmails = promotionalEmails,
                                    promotionalPush = promotionalPush,
                                    smsOrderAlerts = smsOrderAlerts,
                                    whatsappOrderAlerts = whatsappOrderAlerts,
                                    essentialSecurityLocked = true,
                                )
                            )
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("save_notif_prefs_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) { Text(if (isSubmitting) "Saving..." else "Save Notification Preferences") }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
