package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.security.SecureRandom

enum class OrderTimelineStage(
    val stepIndex: Int,
    val title: String,
    val subtitle: String,
) {
    ORDER_PLACED(0, "Order Placed", "Received in your ZEECROWN account"),
    ORDER_CONFIRMED(1, "Order Confirmed", "Order & payment details confirmed"),
    PROCESSING(2, "Processing", "Tailored & prepared by our boutique team"),
    READY_FOR_DISPATCH(3, "Ready for Dispatch", "Packed & sealed for courier pickup"),
    SHIPPED(4, "Shipped", "Handed over to courier partner"),
    OUT_FOR_DELIVERY(5, "Out for Delivery", "Courier is en route to destination"),
    DELIVERED(6, "Delivered", "Successfully delivered to recipient"),
}

data class TimelineStepState(
    val stage: OrderTimelineStage,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val timestampMillis: Long? = null,
    val note: String? = null,
)

data class OrderTimelineSummary(
    val steps: List<TimelineStepState>,
    val isCancelled: Boolean,
    val isRefunded: Boolean,
    val terminalNote: String? = null,
)

object OrderTimelineEngine {
    private val orderedStages = listOf(
        OrderTimelineStage.ORDER_PLACED,
        OrderTimelineStage.ORDER_CONFIRMED,
        OrderTimelineStage.PROCESSING,
        OrderTimelineStage.READY_FOR_DISPATCH,
        OrderTimelineStage.SHIPPED,
        OrderTimelineStage.OUT_FOR_DELIVERY,
        OrderTimelineStage.DELIVERED,
    )

    fun stageForOrderStatus(statusRaw: String): OrderTimelineStage? = when (statusRaw.trim().uppercase()) {
        "PENDING_PAYMENT", "PAYMENT_SUBMITTED" -> OrderTimelineStage.ORDER_PLACED
        "CONFIRMED" -> OrderTimelineStage.ORDER_CONFIRMED
        "PROCESSING" -> OrderTimelineStage.PROCESSING
        "READY_FOR_DISPATCH" -> OrderTimelineStage.READY_FOR_DISPATCH
        "SHIPPED", "DISPATCHED" -> OrderTimelineStage.SHIPPED
        "OUT_FOR_DELIVERY" -> OrderTimelineStage.OUT_FOR_DELIVERY
        "DELIVERED" -> OrderTimelineStage.DELIVERED
        else -> null
    }

    fun buildTimeline(
        statusRaw: String,
        createdAtMillis: Long?,
        dispatchedAtMillis: Long?,
        deliveredAtMillis: Long?,
        history: List<Map<String, Any>>,
    ): OrderTimelineSummary {
        val normalized = statusRaw.trim().uppercase()
        val isCancelled = normalized == "CANCELLED"
        val isRefunded = normalized == "REFUNDED"
        val timestampByStage = mutableMapOf<OrderTimelineStage, Pair<Long?, String?>>()

        if (createdAtMillis != null && createdAtMillis > 0L) {
            timestampByStage[OrderTimelineStage.ORDER_PLACED] = createdAtMillis to null
        }

        history.forEach { entry ->
            val rawStage = entry["stage"]?.toString().orEmpty()
            val mapped = stageForOrderStatus(rawStage)
            val ts = (entry["timestampMillis"] as? Number)?.toLong()
            val note = entry["note"]?.toString()?.takeIf { it.isNotBlank() }
            if (mapped != null) {
                timestampByStage[mapped] = (ts ?: timestampByStage[mapped]?.first) to (note ?: timestampByStage[mapped]?.second)
            }
        }

        if (dispatchedAtMillis != null && dispatchedAtMillis > 0L && timestampByStage[OrderTimelineStage.SHIPPED]?.first == null) {
            timestampByStage[OrderTimelineStage.SHIPPED] = dispatchedAtMillis to timestampByStage[OrderTimelineStage.SHIPPED]?.second
        }
        if (deliveredAtMillis != null && deliveredAtMillis > 0L && timestampByStage[OrderTimelineStage.DELIVERED]?.first == null) {
            timestampByStage[OrderTimelineStage.DELIVERED] = deliveredAtMillis to timestampByStage[OrderTimelineStage.DELIVERED]?.second
        }

        var highestIndex = stageForOrderStatus(normalized)?.stepIndex ?: -1
        if (highestIndex < 0 && timestampByStage.isNotEmpty()) {
            highestIndex = timestampByStage.keys.maxOf { it.stepIndex }
        }
        if (highestIndex < 0) highestIndex = 0

        val steps = orderedStages.map { stage ->
            val info = timestampByStage[stage]
            val completed = if (isCancelled || isRefunded) {
                info != null
            } else {
                stage.stepIndex <= highestIndex
            }
            val current = !isCancelled && !isRefunded && stage.stepIndex == highestIndex
            TimelineStepState(
                stage = stage,
                isCompleted = completed,
                isCurrent = current,
                timestampMillis = info?.first,
                note = info?.second,
            )
        }

        val terminalNote = history.lastOrNull {
            val st = it["stage"]?.toString()?.uppercase()
            st == "CANCELLED" || st == "REFUNDED"
        }?.get("note")?.toString()

        return OrderTimelineSummary(
            steps = steps,
            isCancelled = isCancelled,
            isRefunded = isRefunded,
            terminalNote = terminalNote,
        )
    }

    fun historyEntry(
        stage: String,
        label: String,
        note: String,
        actorRole: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ): Map<String, Any> = mapOf(
        "stage" to stage.trim().uppercase(),
        "label" to label.trim().take(80),
        "note" to note.trim().take(240),
        "actorRole" to actorRole.trim().take(40),
        "timestampMillis" to timestampMillis,
    )
}

@Keep
@IgnoreExtraProperties
data class OrderTrackingProjection(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val status: String = OrderStatus.PENDING_PAYMENT.name,
    val paymentStatus: String = PaymentStatus.UNPAID.name,
    val currency: String = StoreCurrency.NGN.code,
    val totalNgn: Double = 0.0,
    val itemCount: Long = 1L,
    val itemsSummary: String = "",
    val maskedDestination: String = "",
    val courierName: String = "",
    val trackingReference: String = "",
    val trackingUrl: String = "",
    val estimatedDeliveryWindow: String = "",
    val deliveryStatus: String = "",
    val deliveryNotes: String = "",
    val dispatchedAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
    val statusHistory: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class NotificationCategory(val label: String) {
    ORDER("Orders"),
    PAYMENT("Payments"),
    DELIVERY("Delivery"),
    PROMOTION("Promotions"),
    SECURITY("Security"),
    ANNOUNCEMENT("Updates"),
    SUPPORT("Support"),
    SYSTEM("System");

    companion object {
        fun fromRaw(raw: String): NotificationCategory =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: SYSTEM
    }
}

@Keep
@IgnoreExtraProperties
data class NotificationPreferences(
    @DocumentId val id: String = "notifications",
    val userId: String = "",
    val orderUpdatesEmail: Boolean = true,
    val promotionalEmails: Boolean = false,
    val promotionalPush: Boolean = true,
    val smsOrderAlerts: Boolean = false,
    val whatsappOrderAlerts: Boolean = false,
    val essentialSecurityLocked: Boolean = true,
    val consentUpdatedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(preserveConsentTimestamp: Boolean = false): Map<String, Any> = mapOf(
        "userId" to userId,
        "orderUpdatesEmail" to orderUpdatesEmail,
        "promotionalEmails" to promotionalEmails,
        "promotionalPush" to promotionalPush,
        "smsOrderAlerts" to smsOrderAlerts,
        "whatsappOrderAlerts" to whatsappOrderAlerts,
        "essentialSecurityLocked" to true,
        "consentUpdatedAt" to if (preserveConsentTimestamp && consentUpdatedAt != null) consentUpdatedAt else FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class DeviceTokenRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val token: String = "",
    val platform: String = "ANDROID",
    val appVersion: String = "1.0",
    val deviceLabel: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = mapOf(
        "userId" to userId,
        "token" to token.trim(),
        "platform" to platform,
        "appVersion" to appVersion.trim().take(40),
        "deviceLabel" to deviceLabel.trim().takeIf { it.isNotBlank() }?.take(100),
        "active" to active,
        "createdAt" to if (isCreate) FieldValue.serverTimestamp() else createdAt,
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

enum class CommunicationChannel(val label: String) {
    IN_APP("In-App"),
    PUSH("Push"),
    EMAIL("Email"),
    SMS("SMS"),
    WHATSAPP("WhatsApp");

    companion object {
        fun fromRaw(raw: String): CommunicationChannel =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: IN_APP
    }
}

enum class CommunicationStatus(val label: String) {
    QUEUED("Queued"),
    SENT("Sent"),
    FAILED("Failed"),
    SKIPPED_NO_CONSENT("Skipped (No Consent)"),
    PROVIDER_NOT_CONFIGURED("Provider Not Configured");

    companion object {
        fun fromRaw(raw: String): CommunicationStatus =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: QUEUED
    }
}

@Keep
@IgnoreExtraProperties
data class CommunicationLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recipientMasked: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val channel: String = CommunicationChannel.IN_APP.name,
    val eventType: String = "",
    val subject: String = "",
    val bodyPreview: String = "",
    val status: String = CommunicationStatus.SENT.name,
    val providerName: String = "",
    val failureReason: String = "",
    val dedupeKey: String = "",
    val retryCount: Long = 0L,
    val lastAttemptAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "recipientMasked" to recipientMasked.trim().take(200),
        "orderId" to orderId.trim().takeIf { it.isNotBlank() }?.take(128),
        "orderNumber" to orderNumber.trim().takeIf { it.isNotBlank() }?.take(40),
        "channel" to channel,
        "eventType" to eventType.trim().take(80),
        "subject" to subject.trim().take(160),
        "bodyPreview" to bodyPreview.trim().takeIf { it.isNotBlank() }?.take(500),
        "status" to status,
        "providerName" to providerName.trim().takeIf { it.isNotBlank() }?.take(60),
        "failureReason" to failureReason.trim().takeIf { it.isNotBlank() }?.take(400),
        "dedupeKey" to dedupeKey.trim().takeIf { it.isNotBlank() }?.take(140),
        "retryCount" to retryCount.coerceIn(0L, 20L),
        "lastAttemptAt" to lastAttemptAt,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

enum class SupportCategory(val label: String) {
    ORDER_ISSUE("Order Issue"),
    PAYMENT("Payment Help"),
    DELIVERY("Delivery & Courier"),
    SIZING("Sizing & Fit"),
    RETURNS("Order Policy & Help"),
    GENERAL("General Concierge");

    companion object {
        fun fromRaw(raw: String): SupportCategory =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: GENERAL
    }
}

enum class SupportStatus(val label: String) {
    NEW("New"),
    OPEN("Open"),
    WAITING_FOR_STAFF("Waiting for Staff"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    WAITING_CUSTOMER("Waiting for Customer"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    companion object {
        fun fromRaw(raw: String): SupportStatus =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class SupportEnquiry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val category: String = SupportCategory.GENERAL.name,
    val subject: String = "",
    val message: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val status: String = SupportStatus.OPEN.name,
    val priority: String = "NORMAL",
    val assignedAdminUid: String = "",
    val assignedAdminEmail: String = "",
    val sourceChannel: String = "DIRECT",
    val aiConversationId: String = "",
    val escalationReason: String = "",
    val aiSummary: String = "",
    val lastAssistantReply: String = "",
    val transcriptPreview: String = "",
    val lastReplyAt: Timestamp? = null,
    val lastReplyPreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "customerName" to customerName.trim().take(100),
        "customerEmail" to customerEmail.trim().takeIf { it.isNotBlank() }?.take(200),
        "category" to category,
        "subject" to subject.trim().take(140),
        "message" to message.trim().take(2000),
        "orderId" to orderId.trim().takeIf { it.isNotBlank() }?.take(128),
        "orderNumber" to orderNumber.trim().takeIf { it.isNotBlank() }?.take(40),
        "status" to SupportStatus.fromRaw(status).let {
            if (it in setOf(SupportStatus.NEW, SupportStatus.OPEN, SupportStatus.WAITING_FOR_STAFF)) it.name
            else SupportStatus.OPEN.name
        },
        "priority" to priority,
        "sourceChannel" to sourceChannel.trim().takeIf { it.isNotBlank() }?.take(32),
        "aiConversationId" to aiConversationId.trim().takeIf { it.isNotBlank() }?.take(128),
        "escalationReason" to escalationReason.trim().takeIf { it.isNotBlank() }?.take(300),
        "aiSummary" to aiSummary.trim().takeIf { it.isNotBlank() }?.take(800),
        "lastAssistantReply" to lastAssistantReply.trim().takeIf { it.isNotBlank() }?.take(600),
        "transcriptPreview" to transcriptPreview.trim().takeIf { it.isNotBlank() }?.take(2000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class SupportReply(
    @DocumentId val id: String = "",
    val enquiryId: String = "",
    val ownerUid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "CUSTOMER",
    val message: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "enquiryId" to enquiryId,
        "ownerUid" to ownerUid,
        "senderUid" to senderUid,
        "senderName" to senderName.trim().take(100),
        "senderRole" to senderRole,
        "message" to message.trim().take(2000),
        "createdAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class SupportInternalNote(
    @DocumentId val id: String = "",
    val enquiryId: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "enquiryId" to enquiryId,
        "adminUid" to adminUid,
        "adminEmail" to adminEmail.trim().take(200),
        "note" to note.trim().take(2000),
        "createdAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class DeliveryFeedback(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val overallRating: Long = 5L,
    val deliveryRating: Long = 5L,
    val packagingRating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "orderId" to orderId,
        "orderNumber" to orderNumber.trim().take(40),
        "customerName" to customerName.trim().takeIf { it.isNotBlank() }?.take(80),
        "overallRating" to overallRating.coerceIn(1L, 5L),
        "deliveryRating" to deliveryRating.coerceIn(1L, 5L),
        "packagingRating" to packagingRating.coerceIn(1L, 5L),
        "comment" to comment.trim().takeIf { it.isNotBlank() }?.take(1000),
        "createdAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

object TrackingSecurityUtils {
    private val secureRandom = SecureRandom()

    fun normalizeContact(contact: String): String {
        val trimmed = contact.trim()
        return if (trimmed.contains("@")) {
            trimmed.lowercase()
        } else {
            val digits = trimmed.filter { it.isDigit() }
            if (digits.startsWith("234") && digits.length >= 13) "0${digits.drop(3)}" else digits
        }
    }

    fun buildLookupId(orderNumber: String, contact: String): String {
        val cleanOrder = orderNumber.trim().uppercase()
        val cleanContact = normalizeContact(contact)
        if (cleanOrder.isBlank() || cleanContact.isBlank()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$cleanOrder:$cleanContact".toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(32)
        return "trk_$hex"
    }

    fun generateTrackingToken(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return "tok_" + bytes.joinToString("") { "%02x".format(it) }
    }

    fun maskDestination(city: String, state: String, country: String): String {
        return listOf(city, state, country)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Destination confirmed on order" }
    }

    fun maskEmail(email: String): String {
        val clean = email.trim()
        val at = clean.indexOf('@')
        if (at <= 1) return "***"
        return "${clean.take(2)}***${clean.substring(at)}"
    }
}

class GuestTrackingRateLimiter(
    private val maxAttemptsPerWindow: Int = 6,
    private val windowMillis: Long = 60_000L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
) {
    private val timestamps = mutableListOf<Long>()

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = timeProvider()
        timestamps.removeAll { now - it > windowMillis }
        if (timestamps.size >= maxAttemptsPerWindow) {
            return false
        }
        timestamps.add(now)
        return true
    }

    @Synchronized
    fun remainingWaitSeconds(): Int {
        val now = timeProvider()
        timestamps.removeAll { now - it > windowMillis }
        if (timestamps.size < maxAttemptsPerWindow) return 0
        val oldest = timestamps.minOrNull() ?: return 0
        val remainingMs = (windowMillis - (now - oldest)).coerceAtLeast(1000L)
        return ((remainingMs + 999L) / 1000L).toInt()
    }
}

interface CourierTrackingAdapter {
    fun resolveTrackingUrl(courierName: String, trackingReference: String, explicitUrl: String): String
}

object DefaultCourierTrackingAdapter : CourierTrackingAdapter {
    override fun resolveTrackingUrl(courierName: String, trackingReference: String, explicitUrl: String): String {
        val cleanExplicit = explicitUrl.trim()
        if (cleanExplicit.startsWith("https://") || cleanExplicit.startsWith("http://")) {
            return cleanExplicit
        }
        val ref = trackingReference.trim()
        if (ref.isBlank()) return ""
        val courier = courierName.trim().lowercase()
        return when {
            courier.contains("dhl") -> "https://www.dhl.com/ng-en/home/tracking.html?tracking-id=$ref"
            courier.contains("gig") -> "https://giglogistics.com/track?waybill=$ref"
            courier.contains("sendbox") -> "https://sendbox.co/track/$ref"
            else -> ""
        }
    }
}
