package com.example.ui.storefront

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.auth.AuthValidation
import com.example.data.firebase.UiState
import com.example.data.model.AppNotification
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.WishlistItem
import com.example.ui.auth.PasswordStrengthMeter
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AccountOrdersTab(
    ordersState: UiState<List<Order>>,
    onSubmitPaymentNotice: (String) -> Unit,
    onTrackOrder: (String) -> Unit = {},
    onBrowseStore: () -> Unit,
) {
    var inspectingOrder by remember { mutableStateOf<Order?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Order History & Tracking", style = MaterialTheme.typography.titleLarge)
        when (ordersState) {
            is UiState.Loading -> BoutiqueLoadingView("Loading your orders...")
            is UiState.Error -> BoutiqueErrorBanner(ordersState.message)
            is UiState.Success -> {
                val orders = ordersState.data
                if (orders.isEmpty()) {
                    BoutiqueEmptyView(
                        title = "No Orders Yet",
                        subtitle = "Your completed Paystack, Flutterwave, and Bank Transfer orders will appear here with real-time delivery tracking.",
                        actionLabel = "Explore Boutique",
                        onAction = onBrowseStore,
                    )
                } else {
                    orders.forEach { order ->
                        AccountOrderCard(
                            order = order,
                            onInspectOrder = { inspectingOrder = order },
                            onTrackOrder = { onTrackOrder(order.id) },
                            onSubmitTransferNotice = { onSubmitPaymentNotice(order.id) },
                        )
                    }
                }
            }
        }
    }

    inspectingOrder?.let { selectedOrder ->
        OrderDetailModalDialog(
            order = selectedOrder,
            onDismiss = { inspectingOrder = null },
            onSubmitTransferNotice = {
                onSubmitPaymentNotice(selectedOrder.id)
                inspectingOrder = null
            },
        )
    }
}

@Composable
private fun AccountOrderCard(
    order: Order,
    onInspectOrder: () -> Unit,
    onTrackOrder: () -> Unit,
    onSubmitTransferNotice: () -> Unit,
) {
    val status = OrderStatus.fromRaw(order.status)
    val paymentStatus = PaymentStatus.fromRaw(order.paymentStatus)
    val method = PaymentMethod.fromRaw(order.paymentMethod)
    val dateText = remember(order.createdAt) {
        order.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(it)
        } ?: "Pending timestamp"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspectOrder),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ZeecrownLightLilac,
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = ZeecrownDeepPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${method.label} · ${paymentStatus.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatNgnAmount(order.totalNgn),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (order.courierName.isNotBlank() || order.trackingReference.isNotBlank()) {
                Text(
                    text = "Courier: ${order.courierName.ifBlank { "Assigned" }} · Ref: ${order.trackingReference.ifBlank { "Pending" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onTrackOrder,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("account_track_order_${order.id}"),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(if (paymentStatus == PaymentStatus.FAILED) "Retry Payment & Track" else "Live Timeline & Track")
                }
                if (status == OrderStatus.PENDING_PAYMENT && paymentStatus != PaymentStatus.FAILED) {
                    Button(
                        onClick = onSubmitTransferNotice,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeecrownRoyalPurple,
                            contentColor = ZeecrownPureWhite,
                        ),
                    ) {
                        Text("I Have Sent Payment")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailModalDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSubmitTransferNotice: () -> Unit,
) {
    val status = OrderStatus.fromRaw(order.status)
    val paymentStatus = PaymentStatus.fromRaw(order.paymentStatus)
    val stages = listOf(
        OrderStatus.PENDING_PAYMENT to "Order Placed",
        OrderStatus.CONFIRMED to "Payment Verified & Confirmed",
        OrderStatus.DISPATCHED to "Dispatched for Delivery",
        OrderStatus.DELIVERED to "Delivered",
    )
    val activeIdx = when (status) {
        OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_SUBMITTED -> 0
        OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.READY_FOR_DISPATCH -> 1
        OrderStatus.SHIPPED, OrderStatus.DISPATCHED, OrderStatus.OUT_FOR_DELIVERY -> 2
        OrderStatus.DELIVERED -> 3
        OrderStatus.CANCELLED, OrderStatus.REFUNDED -> -1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order ${order.orderNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color = ZeecrownElegantSilver)
                Text("Payment: ${PaymentMethod.fromRaw(order.paymentMethod).label} (${paymentStatus.label})", style = MaterialTheme.typography.bodySmall)
                Text("Subtotal: ${formatNgnAmount(order.subtotalNgn)}", style = MaterialTheme.typography.bodySmall)
                Text("Delivery: ${formatNgnAmount(order.deliveryFeeNgn)}", style = MaterialTheme.typography.bodySmall)
                Text("Total: ${formatNgnAmount(order.totalNgn)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = ZeecrownElegantSilver)
                val destLine = listOfNotNull(
                    order.recipientName.takeIf { it.isNotBlank() },
                    order.shippingAddress.takeIf { it.isNotBlank() },
                    "${order.shippingCity}, ${order.shippingState}",
                    order.shippingCountry.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                Text("Delivery To: $destLine", style = MaterialTheme.typography.bodySmall)
                if (status == OrderStatus.CANCELLED) {
                    Text("Status: Cancelled", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
                } else {
                    Text("Fulfilment Progress", style = MaterialTheme.typography.titleSmall)
                    stages.forEachIndexed { idx, (_, label) ->
                        val reached = idx <= activeIdx
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (reached) ZeecrownSageSuccess else ZeecrownElegantSilver,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (reached) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (status == OrderStatus.PENDING_PAYMENT) {
                Button(onClick = onSubmitTransferNotice) {
                    Text("I Have Sent Payment")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (status == OrderStatus.PENDING_PAYMENT) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
fun AccountWishlistTab(
    wishlistState: UiState<List<WishlistItem>>,
    products: List<Product>,
    onOpenProduct: (String) -> Unit,
    onRemoveWishlist: (Product) -> Unit,
    onBrowseStore: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Wishlist", style = MaterialTheme.typography.titleLarge)
        when (wishlistState) {
            is UiState.Loading -> BoutiqueLoadingView("Loading saved favorites...")
            is UiState.Error -> BoutiqueErrorBanner(wishlistState.message)
            is UiState.Success -> {
                val wishes = wishlistState.data
                if (wishes.isEmpty()) {
                    BoutiqueEmptyView(
                        title = "Your Wishlist Is Empty",
                        subtitle = "Save Adire pieces, Abayas, leather slippers, and handbags to revisit anytime.",
                        actionLabel = "Discover Pieces",
                        onAction = onBrowseStore,
                    )
                } else {
                    wishes.forEach { wish ->
                        val productMatch = products.firstOrNull { it.id == wish.productId }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProduct(wish.productId) },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ZeecrownProductImage(
                                    imageUrl = wish.productImageUrl,
                                    contentDescription = wish.productTitle,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(MaterialTheme.shapes.small),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(wish.productTitle, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(formatNgnAmount(wish.priceNgn), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                if (productMatch != null) {
                                    IconButton(
                                        onClick = { onRemoveWishlist(productMatch) },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove from wishlist")
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

@Composable
fun AccountSecurityAndUpdatesTab(
    userEmail: String,
    isEmailVerified: Boolean,
    hasPasswordProvider: Boolean,
    resendCooldownSeconds: Int,
    notificationsState: UiState<List<AppNotification>>,
    isSubmitting: Boolean,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String, onSuccess: () -> Unit) -> Unit,
    onRequestEmailChange: (currentPassword: String, newEmail: String, onSuccess: () -> Unit) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onOpenNotificationsCenter: () -> Unit = {},
    onOpenNotificationPrefs: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
) {
    var currentPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var pwdError by remember { mutableStateOf<String?>(null) }

    var emailPwd by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    val strength = remember(newPwd) { AuthValidation.evaluatePasswordStrength(newPwd) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EmailVerificationStatusCard(
            email = userEmail,
            isVerified = isEmailVerified,
            resendCooldownSeconds = resendCooldownSeconds,
            isSubmitting = isSubmitting,
            onResend = onResendVerification,
            onRefresh = onRefreshVerification,
        )

        // Change Password Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Change Password", style = MaterialTheme.typography.titleLarge)
                if (!hasPasswordProvider) {
                    Text(
                        text = "You signed in with Google. Password management is handled through your Google Account, or you can use Forgot Password on the Sign-In screen to set an email password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = currentPwd,
                        onValueChange = { currentPwd = it; pwdError = null },
                        label = { Text("Current Password") },
                        visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPwd = !showPwd }) {
                                Icon(
                                    if (showPwd) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle password visibility",
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("change_current_password"),
                    )
                    OutlinedTextField(
                        value = newPwd,
                        onValueChange = { newPwd = it; pwdError = null },
                        label = { Text("New Password") },
                        visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("change_new_password"),
                    )
                    if (newPwd.isNotEmpty()) {
                        PasswordStrengthMeter(strength = strength)
                    }
                    OutlinedTextField(
                        value = confirmPwd,
                        onValueChange = { confirmPwd = it; pwdError = null },
                        label = { Text("Confirm New Password") },
                        visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("change_confirm_password"),
                    )
                    if (pwdError != null) {
                        Text(pwdError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            val err = (if (currentPwd.isBlank()) "Enter your current password." else null)
                                ?: (if (!strength.meetsMinimum) "Use at least 8 characters with uppercase/lowercase or symbol." else null)
                                ?: AuthValidation.validatePasswordConfirmation(newPwd, confirmPwd)
                            if (err != null) {
                                pwdError = err
                                return@Button
                            }
                            onChangePassword(currentPwd, newPwd) {
                                currentPwd = ""
                                newPwd = ""
                                confirmPwd = ""
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("update_password_button"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) {
                        Text("Update Password")
                    }
                }
            }
        }

        // Update Email Address Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Update Account Email", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "A confirmation link will be sent to your new email before the change takes effect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it; emailError = null },
                    label = { Text("New Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasPasswordProvider) {
                    OutlinedTextField(
                        value = emailPwd,
                        onValueChange = { emailPwd = it; emailError = null },
                        label = { Text("Confirm Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (emailError != null) {
                    Text(emailError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(
                    onClick = {
                        val err = AuthValidation.validateEmail(newEmail)
                            ?: (if (hasPasswordProvider && emailPwd.isBlank()) "Enter your current password to authorize this change." else null)
                        if (err != null) {
                            emailError = err
                            return@OutlinedButton
                        }
                        onRequestEmailChange(emailPwd, newEmail.trim()) {
                            emailPwd = ""
                            newEmail = ""
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Send Email Change Link")
                }
            }
        }

        // Boutique Updates & Communication Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Boutique Updates", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = onOpenNotificationsCenter) { Text("Open Full Center") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onOpenNotificationPrefs,
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("account_notif_prefs_btn"),
                shape = MaterialTheme.shapes.small,
            ) { Text("Channel Preferences") }
            OutlinedButton(
                onClick = onOpenSupport,
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("account_support_btn"),
                shape = MaterialTheme.shapes.small,
            ) { Text("Concierge Support") }
        }
        when (notificationsState) {
            is UiState.Loading -> BoutiqueLoadingView("Checking updates...")
            is UiState.Error -> BoutiqueErrorBanner(notificationsState.message)
            is UiState.Success -> {
                val alerts = notificationsState.data
                if (alerts.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Text(
                            text = "No boutique alerts yet. Order confirmations and dispatch notices sync here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                } else {
                    alerts.forEach { alert ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (!alert.readFlag) ZeecrownSoftLavender.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(alert.title, style = MaterialTheme.typography.titleSmall)
                                    Text(alert.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!alert.readFlag) {
                                    TextButton(onClick = { onMarkNotificationRead(alert.id) }) { Text("Mark Read") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
