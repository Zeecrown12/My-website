package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AdminRole(val rawKey: String, val label: String) {
    SUPER_ADMIN("super_admin", "Super Admin"),
    STORE_MANAGER("store_manager", "Store Manager"),
    ORDER_MANAGER("order_manager", "Order Manager"),
    CONTENT_MANAGER("content_manager", "Content Manager"),
    SUPPORT_STAFF("support_staff", "Support Staff"),
    NONE("none", "Customer");

    val isAdmin: Boolean
        get() = this != NONE

    val canManageProducts: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER)

    val canManageCategories: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, CONTENT_MANAGER)

    val canManageInventory: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER)

    val canManageOrders: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, ORDER_MANAGER)

    val canVerifyPayments: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, ORDER_MANAGER)

    val canViewCustomers: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, ORDER_MANAGER, SUPPORT_STAFF)

    val canEditCustomerNotes: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, ORDER_MANAGER, SUPPORT_STAFF)

    val canManagePromos: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, CONTENT_MANAGER)

    val canManageDelivery: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER)

    val canManageContent: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, CONTENT_MANAGER)

    val canModerateReviews: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, CONTENT_MANAGER, SUPPORT_STAFF)

    val canManageSupport: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, ORDER_MANAGER, SUPPORT_STAFF)

    val canManageCommunications: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, ORDER_MANAGER, SUPPORT_STAFF)

    val canViewReports: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, ORDER_MANAGER)

    val canManageAi: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER, CONTENT_MANAGER, SUPPORT_STAFF)

    val canManageSettings: Boolean
        get() = this in setOf(SUPER_ADMIN, STORE_MANAGER)

    val canManageTeam: Boolean
        get() = this == SUPER_ADMIN

    val canViewAuditLogs: Boolean
        get() = this != NONE

    fun canAccessSection(section: AdminSection): Boolean = when (section) {
        AdminSection.OVERVIEW -> isAdmin
        AdminSection.PRODUCTS -> canManageProducts
        AdminSection.CATEGORIES -> canManageCategories
        AdminSection.SIZE_GUIDES -> canManageProducts || canManageCategories
        AdminSection.INVENTORY -> canManageInventory
        AdminSection.ORDERS -> canManageOrders
        AdminSection.PAYMENTS -> canVerifyPayments
        AdminSection.CUSTOMERS -> canViewCustomers
        AdminSection.SUPPORT_INBOX -> canManageSupport
        AdminSection.COMMUNICATIONS -> canManageCommunications
        AdminSection.AI_ASSISTANT -> canManageAi
        AdminSection.PROMOS -> canManagePromos
        AdminSection.DELIVERY -> canManageDelivery
        AdminSection.CONTENT -> canManageContent
        AdminSection.REVIEWS -> canModerateReviews
        AdminSection.REPORTS -> canViewReports
        AdminSection.SETTINGS -> canManageSettings
        AdminSection.TEAM_AUDIT -> canViewAuditLogs
    }

    companion object {
        fun fromRaw(raw: String?): AdminRole = when (raw?.trim()?.lowercase()) {
            "super_admin" -> SUPER_ADMIN
            "store_manager", "manager" -> STORE_MANAGER
            "order_manager" -> ORDER_MANAGER
            "content_manager" -> CONTENT_MANAGER
            "support_staff" -> SUPPORT_STAFF
            else -> NONE
        }

        val assignableRoles: List<AdminRole> = listOf(
            SUPER_ADMIN,
            STORE_MANAGER,
            ORDER_MANAGER,
            CONTENT_MANAGER,
            SUPPORT_STAFF,
        )
    }
}

enum class AdminSection(val title: String, val group: String) {
    OVERVIEW("Overview", "Dashboard"),
    ORDERS("Orders", "Commerce"),
    PAYMENTS("Payments", "Commerce"),
    PRODUCTS("Products", "Catalogue"),
    CATEGORIES("Categories", "Catalogue"),
    SIZE_GUIDES("Size Guides", "Catalogue"),
    INVENTORY("Inventory", "Catalogue"),
    CUSTOMERS("Customers", "Audience"),
    SUPPORT_INBOX("Support Inbox", "Audience"),
    COMMUNICATIONS("Comms & Logs", "Audience"),
    AI_ASSISTANT("AI Assistant", "Intelligence"),
    PROMOS("Promo Codes", "Growth"),
    DELIVERY("Delivery & Shipping", "Logistics"),
    CONTENT("Homepage & Media", "Brand"),
    REVIEWS("Reviews", "Moderation"),
    REPORTS("Reports & Export", "Intelligence"),
    SETTINGS("Store Settings", "Configuration"),
    TEAM_AUDIT("Team & Audit Log", "Governance"),
}

@Keep
@IgnoreExtraProperties
data class DeliverySettings(
    @DocumentId val id: String = "public",
    val freeDeliveryThresholdNgn: Double = 0.0,
    val nationwideFlatFeeNgn: Double? = null,
    val pickupEnabled: Boolean = false,
    val pickupAddress: String = "",
    val pickupHours: String = "",
    val specialPromoNote: String = "",
    val visibility: String = "public",
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "freeDeliveryThresholdNgn" to freeDeliveryThresholdNgn.coerceAtLeast(0.0),
        "nationwideFlatFeeNgn" to nationwideFlatFeeNgn?.takeIf { it >= 0.0 },
        "pickupEnabled" to pickupEnabled,
        "pickupAddress" to pickupAddress.trim().takeIf { it.isNotBlank() }?.take(240),
        "pickupHours" to pickupHours.trim().takeIf { it.isNotBlank() }?.take(120),
        "specialPromoNote" to specialPromoNote.trim().takeIf { it.isNotBlank() }?.take(200),
        "visibility" to visibility,
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Testimonial(
    @DocumentId val id: String = "",
    val customerName: String = "",
    val location: String = "",
    val rating: Long = 5L,
    val quote: String = "",
    val productTitle: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "customerName" to customerName.trim().take(80),
        "location" to location.trim().takeIf { it.isNotBlank() }?.take(80),
        "rating" to rating.coerceIn(1L, 5L),
        "quote" to quote.trim().take(500),
        "productTitle" to productTitle.trim().takeIf { it.isNotBlank() }?.take(120),
        "sortOrder" to sortOrder.coerceIn(0L, 1000L),
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class InventoryTransaction(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val sku: String = "",
    val variation: String = "",
    val previousQty: Long = 0L,
    val deltaQty: Long = 0L,
    val newQty: Long = 0L,
    val reason: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "productTitle" to productTitle.trim().take(120),
        "sku" to sku.trim().take(60),
        "variation" to variation.trim().takeIf { it.isNotBlank() }?.take(80),
        "previousQty" to previousQty,
        "deltaQty" to deltaQty,
        "newQty" to newQty,
        "reason" to reason.trim().take(200),
        "adminUid" to adminUid,
        "adminEmail" to adminEmail.trim().take(200),
        "createdAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class AdminActivityLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val adminRole: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val summary: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "adminUid" to adminUid,
        "adminEmail" to adminEmail.trim().take(200),
        "adminRole" to adminRole.trim().take(40),
        "action" to action.trim().take(80),
        "targetType" to targetType.trim().take(60),
        "targetId" to targetId.trim().take(128),
        "summary" to summary.trim().take(320),
        "previousValue" to previousValue.trim().takeIf { it.isNotBlank() }?.take(300),
        "newValue" to newValue.trim().takeIf { it.isNotBlank() }?.take(300),
        "createdAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

data class CustomerAdminSummary(
    val profile: UserProfile,
    val privateInfo: UserPrivateInfo?,
    val addresses: List<DeliveryAddress>,
    val orders: List<Order>,
    val totalOrderCount: Int,
    val paidOrderCount: Int,
    val lifetimeSpendPaidNgn: Double,
) {
    val email: String get() = privateInfo?.email.orEmpty()
    val phone: String get() = privateInfo?.phone.orEmpty()
}

enum class AnalyticsDateRange(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    ALL_TIME("All Time"),
    CUSTOM("Custom"),
}

data class CategorySalesMetric(
    val categoryId: String,
    val categoryName: String,
    val productCount: Int,
    val unitsSold: Long,
    val attributedRevenueNgn: Double,
)

data class PaymentMethodMetric(
    val method: PaymentMethod,
    val orderCount: Int,
    val paidAmountNgn: Double,
)

data class AdminAnalyticsReport(
    val dateRangeLabel: String,
    val grossOrderValueNgn: Double,
    val verifiedPaidRevenueNgn: Double,
    val unpaidOrdersValueNgn: Double,
    val refundedTotalNgn: Double,
    val netPaidRevenueNgn: Double,
    val totalOrders: Int,
    val paidOrdersCount: Int,
    val pendingPaymentOrders: Int,
    val pendingVerificationPayments: Int,
    val processingOrders: Int,
    val completedDeliveredOrders: Int,
    val cancelledOrders: Int,
    val refundedOrders: Int,
    val averageOrderValueNgn: Double,
    val totalCustomers: Int,
    val newCustomersInRange: Int,
    val returningCustomersInRange: Int,
    val publishedProductsCount: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val bestSellingProducts: List<Product>,
    val categoryBreakdown: List<CategorySalesMetric>,
    val paymentMethodBreakdown: List<PaymentMethodMetric>,
    val dailyRevenueBuckets: List<Pair<String, Double>>,
    val dailyOrderCounts: List<Pair<String, Int>>,
)

data class AdminAlertItem(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val targetSection: AdminSection,
    val severityTone: String,
    val timestampMillis: Long,
)

object AdminNotificationEngine {
    fun buildLiveAlerts(
        orders: List<Order>,
        payments: List<PaymentRecord>,
        products: List<Product>,
        inventoryMap: Map<String, InventoryRecord>,
        customers: List<CustomerAdminSummary>,
        reviews: List<Review>,
        escalatedEnquiries: List<SupportEnquiry> = emptyList(),
    ): List<AdminAlertItem> {
        val alerts = mutableListOf<AdminAlertItem>()

        escalatedEnquiries
            .filter {
                it.sourceChannel.equals("AI_ASSISTANT", ignoreCase = true) &&
                    SupportStatus.fromRaw(it.status) in setOf(
                        SupportStatus.NEW,
                        SupportStatus.OPEN,
                        SupportStatus.WAITING_FOR_STAFF,
                    )
            }
            .take(8)
            .forEach { enq ->
                alerts += AdminAlertItem(
                    id = "ai_esc_${enq.id}",
                    title = "AI Handover: ${enq.subject.take(48)}",
                    body = "${enq.customerName.ifBlank { "Customer" }} · ${enq.escalationReason.ifBlank { "Requested staff support" }}",
                    category = "AI_HANDOVER",
                    targetSection = AdminSection.SUPPORT_INBOX,
                    severityTone = if (enq.priority == "URGENT" || enq.priority == "HIGH") "ERROR" else "ROSE",
                    timestampMillis = enq.updatedAt?.toDate()?.time ?: enq.createdAt?.toDate()?.time ?: 0L,
                )
            }

        // Pending Bank Transfer Proofs
        payments.filter { PaymentRecordStatus.fromRaw(it.status) == PaymentRecordStatus.SUBMITTED }
            .forEach { pay ->
                alerts += AdminAlertItem(
                    id = "pay_sub_${pay.id}",
                    title = "Bank Transfer Proof Submitted",
                    body = "Ref ${pay.reference} (${pay.orderNumber.ifBlank { pay.orderId }}) awaiting verification.",
                    category = "PAYMENT",
                    targetSection = AdminSection.PAYMENTS,
                    severityTone = "ROSE",
                    timestampMillis = pay.createdAt?.toDate()?.time ?: 0L,
                )
            }

        // Failed payment events
        payments.filter { PaymentRecordStatus.fromRaw(it.status) == PaymentRecordStatus.FAILED }
            .take(5)
            .forEach { pay ->
                alerts += AdminAlertItem(
                    id = "pay_fail_${pay.id}",
                    title = "Failed Payment Event",
                    body = "${pay.provider} Ref ${pay.reference} for ${pay.orderNumber.ifBlank { pay.orderId }}.",
                    category = "PAYMENT_FAILED",
                    targetSection = AdminSection.PAYMENTS,
                    severityTone = "ERROR",
                    timestampMillis = pay.createdAt?.toDate()?.time ?: 0L,
                )
            }

        // Recent orders & cancellation/refund requests
        orders.filter { it.cancellationStatus == "REQUESTED" }
            .forEach { ord ->
                alerts += AdminAlertItem(
                    id = "ord_cancel_${ord.id}",
                    title = "Cancellation / Refund Request",
                    body = "${ord.orderNumber}: ${ord.cancellationRequestReason.ifBlank { "Requested by customer" }}",
                    category = "REFUND",
                    targetSection = AdminSection.ORDERS,
                    severityTone = "ERROR",
                    timestampMillis = ord.updatedAt?.toDate()?.time ?: ord.createdAt?.toDate()?.time ?: 0L,
                )
            }

        orders.filter {
            OrderStatus.fromRaw(it.status) in setOf(
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAYMENT_SUBMITTED,
                OrderStatus.CONFIRMED,
            )
        }.take(6).forEach { ord ->
            alerts += AdminAlertItem(
                id = "ord_new_${ord.id}",
                title = "Order ${ord.orderNumber} (${OrderStatus.fromRaw(ord.status).label})",
                body = "${ord.recipientName.ifBlank { "Customer" }} · ${ord.itemsSummary}",
                category = "ORDER",
                targetSection = AdminSection.ORDERS,
                severityTone = "GOLD",
                timestampMillis = ord.createdAt?.toDate()?.time ?: 0L,
            )
        }

        // Out-of-stock & low-stock alerts
        products.forEach { prod ->
            val rec = inventoryMap[prod.id]
            if (rec != null) {
                val threshold = rec.lowStockThreshold.coerceAtLeast(prod.lowStockThreshold)
                if (rec.stockQuantity <= 0L) {
                    alerts += AdminAlertItem(
                        id = "inv_oos_${prod.id}",
                        title = "Out of Stock: ${prod.title}",
                        body = "SKU ${prod.sku} has 0 units available.",
                        category = "INVENTORY",
                        targetSection = AdminSection.INVENTORY,
                        severityTone = "ERROR",
                        timestampMillis = rec.updatedAt?.toDate()?.time ?: 0L,
                    )
                } else if (rec.stockQuantity <= threshold) {
                    alerts += AdminAlertItem(
                        id = "inv_low_${prod.id}",
                        title = "Low Stock: ${prod.title}",
                        body = "Only ${rec.stockQuantity} left (threshold $threshold).",
                        category = "INVENTORY",
                        targetSection = AdminSection.INVENTORY,
                        severityTone = "ROSE",
                        timestampMillis = rec.updatedAt?.toDate()?.time ?: 0L,
                    )
                }
            }
        }

        // New customer registrations
        customers.sortedByDescending { it.profile.createdAt?.toDate()?.time ?: 0L }
            .take(4)
            .forEach { cust ->
                alerts += AdminAlertItem(
                    id = "cust_reg_${cust.profile.id}",
                    title = "Customer Registered",
                    body = cust.profile.displayName.ifBlank { cust.profile.id },
                    category = "CUSTOMER",
                    targetSection = AdminSection.CUSTOMERS,
                    severityTone = "SUCCESS",
                    timestampMillis = cust.profile.createdAt?.toDate()?.time ?: 0L,
                )
            }

        // New or flagged reviews
        reviews.filter { it.moderationStatus.equals("PENDING", true) || it.moderationStatus.equals("FLAGGED", true) }
            .take(5)
            .forEach { rev ->
                alerts += AdminAlertItem(
                    id = "rev_mod_${rev.id}",
                    title = "Review Needs Moderation (${rev.rating}★)",
                    body = "${rev.authorName}: ${rev.comment.take(80)}",
                    category = "REVIEW",
                    targetSection = AdminSection.REVIEWS,
                    severityTone = "GOLD",
                    timestampMillis = rev.createdAt?.toDate()?.time ?: 0L,
                )
            }

        return alerts.sortedByDescending { it.timestampMillis }
    }
}

object AdminAnalyticsEngine {

    fun computeDateBounds(
        range: AnalyticsDateRange,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): Pair<Long?, Long?> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return when (range) {
            AnalyticsDateRange.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to nowMillis
            }
            AnalyticsDateRange.YESTERDAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayStart = cal.timeInMillis
                val yesterdayStart = todayStart - 86_400_000L
                yesterdayStart to (todayStart - 1L)
            }
            AnalyticsDateRange.LAST_7_DAYS -> (nowMillis - 7L * 86_400_000L) to nowMillis
            AnalyticsDateRange.LAST_30_DAYS -> (nowMillis - 30L * 86_400_000L) to nowMillis
            AnalyticsDateRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to nowMillis
            }
            AnalyticsDateRange.LAST_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val thisMonthStart = cal.timeInMillis
                cal.add(Calendar.MONTH, -1)
                val lastMonthStart = cal.timeInMillis
                lastMonthStart to (thisMonthStart - 1L)
            }
            AnalyticsDateRange.ALL_TIME -> null to null
            AnalyticsDateRange.CUSTOM -> customStartMillis to (customEndMillis ?: nowMillis)
        }
    }

    fun isWithinBounds(timestamp: Timestamp?, startMillis: Long?, endMillis: Long?): Boolean {
        if (startMillis == null && endMillis == null) return true
        val ms = timestamp?.toDate()?.time ?: return false
        if (startMillis != null && ms < startMillis) return false
        if (endMillis != null && ms > endMillis) return false
        return true
    }

    fun isOrderPaidAndCountedAsRevenue(order: Order): Boolean {
        val payStatus = PaymentStatus.fromRaw(order.paymentStatus)
        val status = OrderStatus.fromRaw(order.status)
        return (payStatus == PaymentStatus.PAID || payStatus == PaymentStatus.PARTIALLY_REFUNDED) &&
            status != OrderStatus.CANCELLED &&
            status != OrderStatus.REFUNDED
    }

    fun generateReport(
        range: AnalyticsDateRange,
        orders: List<Order>,
        payments: List<PaymentRecord>,
        products: List<Product>,
        categories: List<Category>,
        inventoryMap: Map<String, InventoryRecord>,
        customers: List<CustomerAdminSummary>,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): AdminAnalyticsReport {
        val (startMs, endMs) = computeDateBounds(range, customStartMillis, customEndMillis, nowMillis)
        val filteredOrders = orders.filter { isWithinBounds(it.createdAt, startMs, endMs) }

        val grossOrderValue = filteredOrders
            .filter { OrderStatus.fromRaw(it.status) != OrderStatus.CANCELLED }
            .sumOf { it.totalNgn }
        val paidOrders = filteredOrders.filter { isOrderPaidAndCountedAsRevenue(it) }
        val verifiedPaidRevenue = paidOrders.sumOf { it.totalNgn }
        val unpaidOrdersValue = filteredOrders
            .filter {
                OrderStatus.fromRaw(it.status) !in setOf(OrderStatus.CANCELLED, OrderStatus.REFUNDED) &&
                    !isOrderPaidAndCountedAsRevenue(it)
            }
            .sumOf { it.totalNgn }
        val refundedOrdersList = filteredOrders.filter {
            OrderStatus.fromRaw(it.status) == OrderStatus.REFUNDED ||
                PaymentStatus.fromRaw(it.paymentStatus) == PaymentStatus.REFUNDED ||
                it.refundedNgn > 0.0
        }
        val refundedTotal = refundedOrdersList.sumOf { order ->
            when {
                order.refundedNgn > 0.0 -> order.refundedNgn
                OrderStatus.fromRaw(order.status) == OrderStatus.REFUNDED -> order.totalNgn
                else -> 0.0
            }
        }
        val netPaidRevenue = (verifiedPaidRevenue - refundedTotal).coerceAtLeast(0.0)
        val aov = if (paidOrders.isNotEmpty()) verifiedPaidRevenue / paidOrders.size else 0.0

        val pendingPayOrders = filteredOrders.count {
            OrderStatus.fromRaw(it.status) in setOf(OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_SUBMITTED)
        }
        val processingOrders = filteredOrders.count {
            OrderStatus.fromRaw(it.status) in setOf(
                OrderStatus.CONFIRMED,
                OrderStatus.PROCESSING,
                OrderStatus.READY_FOR_DISPATCH,
                OrderStatus.SHIPPED,
                OrderStatus.DISPATCHED,
                OrderStatus.OUT_FOR_DELIVERY,
            )
        }
        val deliveredOrders = filteredOrders.count { OrderStatus.fromRaw(it.status) == OrderStatus.DELIVERED }
        val cancelledOrders = filteredOrders.count { OrderStatus.fromRaw(it.status) == OrderStatus.CANCELLED }
        val refundedCount = filteredOrders.count {
            OrderStatus.fromRaw(it.status) == OrderStatus.REFUNDED ||
                PaymentStatus.fromRaw(it.paymentStatus) == PaymentStatus.REFUNDED
        }

        val pendingVerificationPayments = payments.count {
            PaymentRecordStatus.fromRaw(it.status) == PaymentRecordStatus.SUBMITTED
        } + filteredOrders.count {
            PaymentStatus.fromRaw(it.paymentStatus) == PaymentStatus.PENDING_VERIFICATION
        }.let { orderPending ->
            val subRefs = payments.count { PaymentRecordStatus.fromRaw(it.status) == PaymentRecordStatus.SUBMITTED }
            (orderPending - subRefs).coerceAtLeast(0)
        }

        val newCustomers = customers.count { isWithinBounds(it.profile.createdAt, startMs, endMs) }
        val returningCustomerUids = filteredOrders.map { it.userId }.distinct().count { uid ->
            val priorOrders = orders.count { o -> o.userId == uid }
            priorOrders > 1
        }

        val publishedCount = products.count { it.active }
        var lowStock = 0
        var outOfStock = 0
        products.forEach { product ->
            val rec = inventoryMap[product.id]
            if (rec != null) {
                val threshold = rec.lowStockThreshold.coerceAtLeast(product.lowStockThreshold)
                if (rec.stockQuantity <= 0L) outOfStock++
                else if (rec.stockQuantity <= threshold) lowStock++
            }
        }

        val bestSellers = products
            .sortedWith(compareByDescending<Product> { it.soldCount }.thenByDescending { it.featured })
            .take(8)

        val categoryBreakdown = categories.map { cat ->
            val catProducts = products.filter { it.categoryId == cat.id }
            val units = catProducts.sumOf { it.soldCount }
            val revenue = catProducts.sumOf { it.soldCount * it.priceNgn }
            CategorySalesMetric(
                categoryId = cat.id,
                categoryName = cat.name,
                productCount = catProducts.size,
                unitsSold = units,
                attributedRevenueNgn = revenue,
            )
        }.sortedByDescending { it.unitsSold }

        val methodBreakdown = PaymentMethod.entries.map { method ->
            val methodOrders = filteredOrders.filter { PaymentMethod.fromRaw(it.paymentMethod) == method }
            val paidSum = methodOrders.filter { isOrderPaidAndCountedAsRevenue(it) }.sumOf { it.totalNgn }
            PaymentMethodMetric(
                method = method,
                orderCount = methodOrders.size,
                paidAmountNgn = paidSum,
            )
        }

        val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dailyBuckets = paidOrders
            .groupBy { order ->
                val d = order.createdAt?.toDate() ?: Date(nowMillis)
                dayFormat.format(d)
            }
            .map { (day, list) -> day to list.sumOf { it.totalNgn } }
            .takeLast(14)

        val dailyOrderCountBuckets = filteredOrders
            .groupBy { order ->
                val d = order.createdAt?.toDate() ?: Date(nowMillis)
                dayFormat.format(d)
            }
            .map { (day, list) -> day to list.size }
            .takeLast(14)

        val label = if (range == AnalyticsDateRange.CUSTOM && startMs != null) {
            val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
            "${fmt.format(Date(startMs))} – ${fmt.format(Date(endMs ?: nowMillis))}"
        } else {
            range.label
        }

        return AdminAnalyticsReport(
            dateRangeLabel = label,
            grossOrderValueNgn = grossOrderValue,
            verifiedPaidRevenueNgn = verifiedPaidRevenue,
            unpaidOrdersValueNgn = unpaidOrdersValue,
            refundedTotalNgn = refundedTotal,
            netPaidRevenueNgn = netPaidRevenue,
            totalOrders = filteredOrders.size,
            paidOrdersCount = paidOrders.size,
            pendingPaymentOrders = pendingPayOrders,
            pendingVerificationPayments = pendingVerificationPayments,
            processingOrders = processingOrders,
            completedDeliveredOrders = deliveredOrders,
            cancelledOrders = cancelledOrders,
            refundedOrders = refundedCount,
            averageOrderValueNgn = aov,
            totalCustomers = customers.size,
            newCustomersInRange = newCustomers,
            returningCustomersInRange = returningCustomerUids,
            publishedProductsCount = publishedCount,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock,
            bestSellingProducts = bestSellers,
            categoryBreakdown = categoryBreakdown,
            paymentMethodBreakdown = methodBreakdown,
            dailyRevenueBuckets = dailyBuckets,
            dailyOrderCounts = dailyOrderCountBuckets,
        )
    }

    private fun csvEscape(raw: String): String {
        val sanitized = raw.replace("\r", " ").replace("\n", " ")
        return if (sanitized.contains(",") || sanitized.contains("\"")) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else {
            sanitized
        }
    }

    fun buildOrdersCsv(orders: List<Order>): String {
        val sb = StringBuilder()
        sb.appendLine("Order Number,Customer,City,State,Country,Payment Method,Payment Status,Order Status,Items Summary,Item Count,Subtotal NGN,Delivery Fee NGN,Discount NGN,Total NGN,Refunded NGN,Created At")
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        orders.forEach { o ->
            val created = o.createdAt?.toDate()?.let { dateFmt.format(it) }.orEmpty()
            sb.appendLine(
                listOf(
                    csvEscape(o.orderNumber),
                    csvEscape(o.recipientName.ifBlank { o.userId }),
                    csvEscape(o.shippingCity),
                    csvEscape(o.shippingState),
                    csvEscape(o.shippingCountry),
                    csvEscape(o.paymentMethod),
                    csvEscape(o.paymentStatus),
                    csvEscape(o.status),
                    csvEscape(o.itemsSummary),
                    o.itemCount.toString(),
                    o.subtotalNgn.toString(),
                    o.deliveryFeeNgn.toString(),
                    o.discountNgn.toString(),
                    o.totalNgn.toString(),
                    o.refundedNgn.toString(),
                    csvEscape(created),
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun buildSalesReportCsv(report: AdminAnalyticsReport): String {
        val sb = StringBuilder()
        sb.appendLine("Metric,Value,Formula / Rule")
        sb.appendLine("Period,${csvEscape(report.dateRangeLabel)},Selected dashboard range")
        sb.appendLine("Verified Paid Revenue (NGN),${report.verifiedPaidRevenueNgn},Sum(totalNgn) for PAID orders excluding CANCELLED/REFUNDED")
        sb.appendLine("Refunded Amount (NGN),${report.refundedTotalNgn},Sum(refundedNgn or totalNgn) on refunded orders")
        sb.appendLine("Net Paid Revenue (NGN),${report.netPaidRevenueNgn},Verified Paid Revenue - Refunded Amount")
        sb.appendLine("Total Orders,${report.totalOrders},All orders created in range")
        sb.appendLine("Paid Orders,${report.paidOrdersCount},Orders with verified PAID status")
        sb.appendLine("Average Order Value (NGN),${report.averageOrderValueNgn},Verified Paid Revenue / Paid Orders")
        sb.appendLine("Cancelled Orders,${report.cancelledOrders},Orders in CANCELLED status")
        sb.appendLine("Total Customers,${report.totalCustomers},Registered customer accounts")
        sb.appendLine("New Customers In Range,${report.newCustomersInRange},Customers registered in selected range")
        sb.appendLine()
        sb.appendLine("Category,Products,Units Sold,Attributed Revenue (NGN)")
        report.categoryBreakdown.forEach { cat ->
            sb.appendLine("${csvEscape(cat.categoryName)},${cat.productCount},${cat.unitsSold},${cat.attributedRevenueNgn}")
        }
        return sb.toString()
    }
}
