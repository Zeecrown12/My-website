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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.DefaultCourierTrackingAdapter
import com.example.data.model.DeliveryFeedback
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderTimelineEngine
import com.example.data.model.OrderTimelineSummary
import com.example.data.model.OrderTrackingProjection
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.StoreSettings
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.formatNgnAmount
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
import java.util.Date
import java.util.Locale

@Composable
fun OrderTrackingScreen(
    isSignedIn: Boolean,
    ordersState: UiState<List<Order>>,
    deliveryFeedbackList: List<DeliveryFeedback>,
    guestTrackingResultState: UiState<OrderTrackingProjection>?,
    storeSettings: StoreSettings,
    initialOrderId: String = "",
    initialTrackingToken: String = "",
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onLookupGuestOrder: (orderNumber: String, contact: String, trackingToken: String) -> Unit,
    onClearGuestTrackingResult: () -> Unit,
    onRetryFailedPayment: (Order, PaymentMethod, String, () -> Unit) -> Unit,
    onSubmitDeliveryFeedback: (Order, Long, Long, Long, String, () -> Unit) -> Unit,
    onOpenSupport: () -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember(isSignedIn, initialTrackingToken) {
        mutableIntStateOf(if (!isSignedIn || initialTrackingToken.isNotBlank()) 1 else 0)
    }
    var selectedOrderId by remember(initialOrderId) { mutableStateOf(initialOrderId) }

    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(orders, initialOrderId) {
        if (selectedOrderId.isBlank() && orders.isNotEmpty()) {
            selectedOrderId = orders.first().id
        }
    }

    LaunchedEffect(initialTrackingToken) {
        if (initialTrackingToken.isNotBlank()) {
            onLookupGuestOrder("", "", initialTrackingToken)
        }
    }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text("Order Tracking", style = MaterialTheme.typography.titleLarge, color = ZeecrownDeepPurple)
                            Text("Live 7-stage fulfillment & courier updates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = onOpenSupport, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Outlined.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Help")
                    }
                }
            }

            if (isSignedIn) {
                TabRow(selectedTabIndex = selectedTab, containerColor = ZeecrownWarmIvory) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Orders (${orders.size})") },
                        modifier = Modifier.testTag("tracking_tab_my_orders"),
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Lookup by Order #") },
                        modifier = Modifier.testTag("tracking_tab_guest_lookup"),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (isSignedIn && selectedTab == 0) {
                    when (ordersState) {
                        is UiState.Loading -> item { BoutiqueLoadingView("Loading your order timeline...") }
                        is UiState.Error -> item { BoutiqueErrorBanner(ordersState.message) }
                        is UiState.Success -> {
                            if (orders.isEmpty()) {
                                item {
                                    BoutiqueEmptyView(
                                        title = "No Orders Placed Yet",
                                        subtitle = "Once you place an order, every step from tailoring to doorstep delivery will update here in real time.",
                                        actionLabel = "Explore Store",
                                        onAction = onOpenCatalog,
                                    )
                                }
                            } else {
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(orders, key = { it.id }) { ord ->
                                            FilterChip(
                                                selected = ord.id == selectedOrderId,
                                                onClick = { selectedOrderId = ord.id },
                                                label = { Text("${ord.orderNumber} · ${OrderStatus.fromRaw(ord.status).label}") },
                                            )
                                        }
                                    }
                                }
                                val activeOrder = orders.firstOrNull { it.id == selectedOrderId } ?: orders.first()
                                item(key = "order_track_${activeOrder.id}") {
                                    SignedInOrderTrackingCard(
                                        order = activeOrder,
                                        existingFeedback = deliveryFeedbackList.firstOrNull { it.orderId == activeOrder.id },
                                        storeSettings = storeSettings,
                                        isSubmitting = isSubmitting,
                                        onRetryPayment = { method, note, done ->
                                            onRetryFailedPayment(activeOrder, method, note, done)
                                        },
                                        onSubmitFeedback = { overall, delivery, packaging, comment, done ->
                                            onSubmitDeliveryFeedback(activeOrder, overall, delivery, packaging, comment, done)
                                        },
                                        onOpenSupport = onOpenSupport,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        GuestTrackingLookupSection(
                            initialTrackingToken = initialTrackingToken,
                            resultState = guestTrackingResultState,
                            onLookup = onLookupGuestOrder,
                            onClear = onClearGuestTrackingResult,
                            onOpenSupport = onOpenSupport,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignedInOrderTrackingCard(
    order: Order,
    existingFeedback: DeliveryFeedback?,
    storeSettings: StoreSettings,
    isSubmitting: Boolean,
    onRetryPayment: (PaymentMethod, String, () -> Unit) -> Unit,
    onSubmitFeedback: (Long, Long, Long, String, () -> Unit) -> Unit,
    onOpenSupport: () -> Unit,
) {
    val paymentStatus = PaymentStatus.fromRaw(order.paymentStatus)
    val orderStatus = OrderStatus.fromRaw(order.status)
    val timeline = remember(order) {
        OrderTimelineEngine.buildTimeline(
            statusRaw = order.status,
            createdAtMillis = order.createdAt?.toDate()?.time,
            dispatchedAtMillis = order.dispatchedAt?.toDate()?.time,
            deliveredAtMillis = order.deliveredAt?.toDate()?.time,
            history = order.statusHistory,
        )
    }
    var showRetryDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (paymentStatus == PaymentStatus.FAILED) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("failed_payment_banner"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Payment Verification Failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Payment for ${order.orderNumber} could not be confirmed. You can safely retry payment or submit a fresh bank transfer notice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Button(
                        onClick = { showRetryDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("retry_payment_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry Payment")
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(order.orderNumber, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (order.trackingToken.isNotBlank()) {
                            Text("Guest Tracking Code: ${order.trackingToken}", style = MaterialTheme.typography.labelSmall, color = ZeecrownGoldInk)
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = ZeecrownSoftLavender) {
                        Text(orderStatus.label, style = MaterialTheme.typography.labelMedium, color = ZeecrownDeepPurple, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
                Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color = ZeecrownElegantSilver)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Payment: ${PaymentMethod.fromRaw(order.paymentMethod).label} (${paymentStatus.label})", style = MaterialTheme.typography.bodySmall)
                    Text(formatNgnAmount(order.totalNgn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Deliver To: ${order.recipientName.ifBlank { "Customer" }} · ${order.shippingAddress}, ${order.shippingCity}, ${order.shippingState}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        CourierDispatchCard(
            courierName = order.courierName,
            trackingReference = order.trackingReference,
            trackingUrl = DefaultCourierTrackingAdapter.resolveTrackingUrl(order.courierName, order.trackingReference, order.trackingUrl),
            estimatedWindow = order.estimatedDeliveryWindow,
            deliveryStatus = order.deliveryStatus,
            deliveryNotes = order.deliveryNotes,
        )

        OrderTimelineCard(summary = timeline)

        if (orderStatus == OrderStatus.DELIVERED) {
            DeliveryFeedbackCard(
                order = order,
                existingFeedback = existingFeedback,
                isSubmitting = isSubmitting,
                onSubmit = onSubmitFeedback,
            )
        }

        OutlinedButton(
            onClick = onOpenSupport,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        ) {
            Icon(Icons.Outlined.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Need Help With ${order.orderNumber}? Open Concierge Support")
        }
    }

    if (showRetryDialog) {
        RetryPaymentDialog(
            order = order,
            storeSettings = storeSettings,
            isSubmitting = isSubmitting,
            onDismiss = { showRetryDialog = false },
            onConfirm = { method, note ->
                onRetryPayment(method, note) { showRetryDialog = false }
            },
        )
    }
}

@Composable
private fun GuestTrackingLookupSection(
    initialTrackingToken: String,
    resultState: UiState<OrderTrackingProjection>?,
    onLookup: (String, String, String) -> Unit,
    onClear: () -> Unit,
    onOpenSupport: () -> Unit,
) {
    var orderNumber by remember { mutableStateOf("") }
    var contactInput by remember { mutableStateOf("") }
    var tokenInput by remember(initialTrackingToken) { mutableStateOf(initialTrackingToken) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = ZeecrownLightLilac,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Verify Your Order", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Enter your Order Number (e.g. ZC-1A2B3C) paired with the email or phone number used at checkout — or paste your secure Tracking Code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = orderNumber,
                    onValueChange = { orderNumber = it.uppercase() },
                    label = { Text("Order Number (e.g. ZC-1A2B3C)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("guest_track_order_number"),
                )
                OutlinedTextField(
                    value = contactInput,
                    onValueChange = { contactInput = it },
                    label = { Text("Checkout Email or Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("guest_track_contact"),
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Or Secure Tracking Code (tok_...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("guest_track_token"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onLookup(orderNumber, contactInput, tokenInput) },
                        enabled = tokenInput.isNotBlank() || (orderNumber.isNotBlank() && contactInput.isNotBlank()),
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("guest_track_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Check Order Status")
                    }
                    if (resultState != null) {
                        OutlinedButton(onClick = onClear, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Text("Reset")
                        }
                    }
                }
            }
        }

        when (resultState) {
            null -> Unit
            is UiState.Loading -> BoutiqueLoadingView("Verifying order reference...")
            is UiState.Error -> BoutiqueErrorBanner(resultState.message)
            is UiState.Success -> {
                val proj = resultState.data
                val timeline = remember(proj) {
                    OrderTimelineEngine.buildTimeline(
                        statusRaw = proj.status,
                        createdAtMillis = proj.createdAt?.toDate()?.time,
                        dispatchedAtMillis = proj.dispatchedAt?.toDate()?.time,
                        deliveredAtMillis = proj.deliveredAt?.toDate()?.time,
                        history = proj.statusHistory,
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("guest_tracking_result_card"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(proj.orderNumber, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(8.dp), color = ZeecrownSoftLavender) {
                                Text(OrderStatus.fromRaw(proj.status).label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Text(proj.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Payment: ${PaymentStatus.fromRaw(proj.paymentStatus).label} · Destination: ${proj.maskedDestination}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CourierDispatchCard(
                    courierName = proj.courierName,
                    trackingReference = proj.trackingReference,
                    trackingUrl = DefaultCourierTrackingAdapter.resolveTrackingUrl(proj.courierName, proj.trackingReference, proj.trackingUrl),
                    estimatedWindow = proj.estimatedDeliveryWindow,
                    deliveryStatus = proj.deliveryStatus,
                    deliveryNotes = proj.deliveryNotes,
                )
                OrderTimelineCard(summary = timeline)
                OutlinedButton(onClick = onOpenSupport, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()) {
                    Text("Contact Concierge About This Order")
                }
            }
        }
    }
}

@Composable
private fun CourierDispatchCard(
    courierName: String,
    trackingReference: String,
    trackingUrl: String,
    estimatedWindow: String,
    deliveryStatus: String,
    deliveryNotes: String,
) {
    if (courierName.isBlank() && trackingReference.isBlank() && estimatedWindow.isBlank() && deliveryNotes.isBlank()) return
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ZeecrownLightLilac,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = ZeecrownDeepPurple)
                Text("Courier & Dispatch Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (courierName.isNotBlank()) Text("Courier Partner: $courierName", style = MaterialTheme.typography.bodyMedium)
            if (trackingReference.isNotBlank()) Text("Tracking Reference: $trackingReference", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (deliveryStatus.isNotBlank()) Text("Transit Status: $deliveryStatus", style = MaterialTheme.typography.bodySmall)
            if (estimatedWindow.isNotBlank()) Text("Estimated Delivery: $estimatedWindow", style = MaterialTheme.typography.bodySmall, color = ZeecrownGoldInk)
            if (deliveryNotes.isNotBlank()) Text("Note: $deliveryNotes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (trackingUrl.startsWith("http")) {
                OutlinedButton(
                    onClick = { uriHandler.openUri(trackingUrl) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("Track on Courier Site")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderTimelineCard(summary: OrderTimelineSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("order_timeline_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Fulfillment Journey", style = MaterialTheme.typography.titleLarge)
            if (summary.isCancelled || summary.isRefunded) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Column {
                            Text(
                                if (summary.isRefunded) "Order Refunded" else "Order Cancelled",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            if (!summary.terminalNote.isNullOrBlank()) {
                                Text(summary.terminalNote, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            summary.steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        step.isCurrent -> ZeecrownDeepPurple
                                        step.isCompleted -> ZeecrownSageSuccess
                                        else -> ZeecrownSoftLavender
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (step.isCompleted) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = ZeecrownPureWhite, modifier = Modifier.size(16.dp))
                            } else {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = ZeecrownDeepPurple)
                            }
                        }
                        if (index < summary.steps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(26.dp)
                                    .background(if (step.isCompleted) ZeecrownSageSuccess.copy(alpha = 0.5f) else ZeecrownElegantSilver)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = step.stage.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (step.isCurrent || step.isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = if (step.isCompleted || step.isCurrent) ZeecrownDeepPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = step.note?.takeIf { it.isNotBlank() } ?: step.stage.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        step.timestampMillis?.let { ms ->
                            Text(
                                text = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(ms)),
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

@Composable
private fun DeliveryFeedbackCard(
    order: Order,
    existingFeedback: DeliveryFeedback?,
    isSubmitting: Boolean,
    onSubmit: (Long, Long, Long, String, () -> Unit) -> Unit,
) {
    var overall by remember { mutableLongStateOf(5L) }
    var deliverySpeed by remember { mutableLongStateOf(5L) }
    var packaging by remember { mutableLongStateOf(5L) }
    var comment by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("delivery_feedback_card"),
        shape = MaterialTheme.shapes.large,
        color = ZeecrownLightLilac,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Rate Your ZEECROWN Delivery", style = MaterialTheme.typography.titleLarge)
            if (existingFeedback != null) {
                Text(
                    "Thank you! You rated this delivery ${existingFeedback.overallRating}/5 stars (Speed ${existingFeedback.deliveryRating}/5 · Packaging ${existingFeedback.packagingRating}/5).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZeecrownSageSuccess,
                )
                if (existingFeedback.comment.isNotBlank()) {
                    Text("\"${existingFeedback.comment}\"", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                StarRatingRow("Overall Experience", overall) { overall = it }
                StarRatingRow("Delivery Speed", deliverySpeed) { deliverySpeed = it }
                StarRatingRow("Boutique Packaging", packaging) { packaging = it }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Optional delivery notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Button(
                    onClick = { onSubmit(overall, deliverySpeed, packaging, comment) {} },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("submit_delivery_feedback_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Submit Delivery Feedback") }
            }
        }
    }
}

@Composable
private fun StarRatingRow(label: String, rating: Long, onSelect: (Long) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row {
            (1L..5L).forEach { star ->
                IconButton(onClick = { onSelect(star) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$star stars for $label",
                        tint = ZeecrownGoldInk,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RetryPaymentDialog(
    order: Order,
    storeSettings: StoreSettings,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod, String) -> Unit,
) {
    val methods = remember(storeSettings) {
        buildList {
            if (storeSettings.paystackEnabled) add(PaymentMethod.PAYSTACK)
            if (storeSettings.flutterwaveEnabled) add(PaymentMethod.FLUTTERWAVE)
            if (storeSettings.bankTransferEnabled) add(PaymentMethod.BANK_TRANSFER)
        }.ifEmpty { listOf(PaymentMethod.PAYSTACK, PaymentMethod.BANK_TRANSFER) }
    }
    var chosenMethod by remember { mutableStateOf(methods.first()) }
    var transferNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retry Payment · ${order.orderNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Amount Due: ${formatNgnAmount(order.totalNgn)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { method ->
                        FilterChip(
                            selected = chosenMethod == method,
                            onClick = { chosenMethod = method },
                            label = { Text(method.label) },
                        )
                    }
                }
                if (chosenMethod == PaymentMethod.BANK_TRANSFER) {
                    if (storeSettings.bankName.isNotBlank()) {
                        Text(
                            "${storeSettings.bankName} · ${storeSettings.accountNumber} (${storeSettings.accountName})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedTextField(
                        value = transferNote,
                        onValueChange = { transferNote = it },
                        label = { Text("New Bank Transfer Reference / Sender Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(chosenMethod, transferNote) },
                enabled = !isSubmitting && (chosenMethod != PaymentMethod.BANK_TRANSFER || transferNote.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
            ) { Text("Confirm Payment Retry") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
