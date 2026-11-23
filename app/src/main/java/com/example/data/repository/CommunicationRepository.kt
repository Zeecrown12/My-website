package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.CommunicationChannel
import com.example.data.model.CommunicationLog
import com.example.data.model.CommunicationStatus
import com.example.data.model.DefaultCourierTrackingAdapter
import com.example.data.model.DeliveryFeedback
import com.example.data.model.DeviceTokenRecord
import com.example.data.model.GuestTrackingRateLimiter
import com.example.data.model.NotificationPreferences
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderTimelineEngine
import com.example.data.model.OrderTrackingProjection
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.SupportCategory
import com.example.data.model.SupportEnquiry
import com.example.data.model.SupportInternalNote
import com.example.data.model.SupportReply
import com.example.data.model.TrackingSecurityUtils
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class CommunicationRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val guestRateLimiter = GuestTrackingRateLimiter()

    private fun requireUserId(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in to access this feature.")

    fun observeNotificationPreferences(userId: String): Flow<NotificationPreferences> = flow {
        if (userId.isBlank()) {
            emit(NotificationPreferences())
            return@flow
        }
        val docRef = db.collection("users").document(userId).collection("preferences").document("notifications")
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(
                        NotificationPreferences::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: NotificationPreferences(userId = userId)
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    emit(NotificationPreferences(userId = userId))
                }
        )
    }

    suspend fun saveNotificationPreferences(
        userId: String,
        prefs: NotificationPreferences,
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("preferences").document("notifications")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            docRef.set(prefs.copy(userId = uid, essentialSecurityLocked = true).toWriteMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun registerDeviceToken(userId: String, fcmToken: String): Result<Unit> {
        val cleanToken = fcmToken.trim()
        if (userId.isBlank() || cleanToken.length < 10) return Result.success(Unit)
        val tokenId = "fcm_" + MessageDigest.getInstance("SHA-256")
            .digest(cleanToken.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(32)
        val docRef = db.collection("users").document(userId).collection("deviceTokens").document(tokenId)
        return try {
            val uid = auth.currentUser?.uid
            if (uid != userId) return Result.success(Unit)
            val existing = docRef.get().await()
            val label = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(100)
            val record = DeviceTokenRecord(
                userId = uid,
                token = cleanToken,
                platform = "ANDROID",
                appVersion = "1.0",
                deviceLabel = label,
                active = true,
                createdAt = existing.getTimestamp("createdAt"),
            )
            docRef.set(record.toWriteMap(isCreate = !existing.exists())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val docRef = db.collection("notifications").document(notificationId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            docRef.update(
                mapOf(
                    "readFlag" to true,
                    "readAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(userId: String, notifications: List<AppNotification>): Result<Int> {
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val unread = notifications.filter { !it.readFlag && it.id.isNotBlank() }
            if (unread.isEmpty()) return Result.success(0)
            unread.chunked(200).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { n ->
                    batch.update(
                        db.collection("notifications").document(n.id),
                        mapOf(
                            "readFlag" to true,
                            "readAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(unread.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
            Result.failure(e)
        }
    }

    suspend fun lookupGuestOrder(
        orderNumberInput: String,
        contactInput: String,
        trackingTokenInput: String = "",
    ): Result<OrderTrackingProjection> {
        if (!guestRateLimiter.tryAcquire()) {
            val wait = guestRateLimiter.remainingWaitSeconds()
            return Result.failure(
                IllegalStateException("Too many tracking checks. Please wait ${wait}s before trying again.")
            )
        }

        val cleanToken = trackingTokenInput.trim()
        if (cleanToken.isNotBlank()) {
            val ref = db.collection("orderTrackingTokens").document(cleanToken)
            return try {
                val snap = ref.get().await()
                val projection = snap.toObject(
                    OrderTrackingProjection::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (projection == null) {
                    Result.failure(IllegalArgumentException("No order matched that tracking code."))
                } else {
                    Result.success(projection)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, ref.path)
                Result.failure(IllegalStateException("Unable to verify tracking code right now."))
            }
        }

        val cleanOrder = orderNumberInput.trim().uppercase()
        val cleanContact = contactInput.trim()
        if (cleanOrder.length < 3 || cleanContact.length < 4) {
            return Result.failure(
                IllegalArgumentException("Enter your Order Number (e.g. ZC-1024) and matching email or phone number.")
            )
        }

        val lookupId = TrackingSecurityUtils.buildLookupId(cleanOrder, cleanContact)
        if (lookupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter valid verification details."))
        }

        val ref = db.collection("orderTrackingLookups").document(lookupId)
        return try {
            val snap = ref.get().await()
            val projection = snap.toObject(
                OrderTrackingProjection::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (projection == null) {
                Result.failure(
                    IllegalArgumentException("No order matched that Order Number and email/phone combination.")
                )
            } else {
                Result.success(projection)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, ref.path)
            Result.failure(IllegalStateException("Unable to retrieve order status right now."))
        }
    }

    suspend fun retryFailedOrderPayment(
        userId: String,
        order: Order,
        paymentMethod: PaymentMethod,
        transferNote: String = "",
    ): Result<String> {
        val orderRef = db.collection("orders").document(order.id)
        val paymentRef = db.collection("payments").document()
        return try {
            val uid = requireUserId()
            check(uid == userId && order.userId == uid) { "Signed-in user mismatch." }
            check(PaymentStatus.fromRaw(order.paymentStatus) == PaymentStatus.FAILED) {
                "Retry is only available for orders with a failed payment status."
            }
            check(order.status !in setOf(OrderStatus.DELIVERED.name, OrderStatus.CANCELLED.name, OrderStatus.REFUNDED.name)) {
                "This order can no longer be retried."
            }

            val isBankNotice = paymentMethod == PaymentMethod.BANK_TRANSFER && transferNote.trim().isNotBlank()
            val nextOrderStatus = if (isBankNotice) OrderStatus.PAYMENT_SUBMITTED.name else OrderStatus.PENDING_PAYMENT.name
            val nextPaymentStatus = if (isBankNotice) PaymentStatus.PENDING_VERIFICATION.name else PaymentStatus.UNPAID.name

            val updatedHistory = order.statusHistory.toMutableList().apply {
                add(
                    OrderTimelineEngine.historyEntry(
                        stage = nextOrderStatus,
                        label = "Payment Retried",
                        note = if (isBankNotice) "Submitted new ${paymentMethod.label} transfer reference."
                        else "Initiated ${paymentMethod.label} payment retry.",
                        actorRole = "CUSTOMER",
                    )
                )
            }.takeLast(30)

            val payRefCode = "RETRY-${order.orderNumber}-${System.currentTimeMillis() % 10000}"
            val paymentRecord = PaymentRecord(
                userId = uid,
                orderId = order.id,
                orderNumber = order.orderNumber,
                customerName = order.recipientName,
                provider = paymentMethod.name,
                reference = payRefCode,
                amountNgn = order.totalNgn,
                currency = order.currency,
                status = if (isBankNotice) PaymentRecordStatus.SUBMITTED.name else PaymentRecordStatus.INITIATED.name,
                transferNote = transferNote.trim().take(300),
            )
            paymentRef.set(paymentRecord.toCreateMap()).await()

            orderRef.update(
                mapOf(
                    "status" to nextOrderStatus,
                    "paymentStatus" to nextPaymentStatus,
                    "paymentMethod" to paymentMethod.name,
                    "statusHistory" to updatedHistory,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            syncOrderTrackingProjections(
                order = order.copy(
                    status = nextOrderStatus,
                    paymentStatus = nextPaymentStatus,
                    paymentMethod = paymentMethod.name,
                    statusHistory = updatedHistory,
                ),
            )

            createIdempotentCustomerNotification(
                notificationId = "notif_retry_${order.id}_${System.currentTimeMillis() / 1000L}",
                notification = AppNotification(
                    userId = uid,
                    title = "Payment Retry Submitted",
                    body = "New payment attempt ($payRefCode) recorded for order ${order.orderNumber}.",
                    details = "Your order will move to Confirmed once verification completes.",
                    category = "PAYMENT",
                    readFlag = false,
                    targetRoute = "track_order/${order.id}",
                    orderId = order.id,
                    orderNumber = order.orderNumber,
                    dedupeKey = "retry_${order.id}_${System.currentTimeMillis() / 1000L}",
                )
            )
            Result.success(payRefCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncOrderTrackingProjections(order: Order) {
        if (order.id.isBlank() || order.orderNumber.isBlank() || order.userId.isBlank()) return
        val resolvedUrl = DefaultCourierTrackingAdapter.resolveTrackingUrl(
            courierName = order.courierName,
            trackingReference = order.trackingReference,
            explicitUrl = order.trackingUrl,
        )
        val maskedDest = TrackingSecurityUtils.maskDestination(
            city = order.shippingCity,
            state = order.shippingState,
            country = order.shippingCountry,
        )
        val payload = mapOf(
            "orderId" to order.id,
            "userId" to order.userId,
            "orderNumber" to order.orderNumber,
            "status" to order.status,
            "paymentStatus" to order.paymentStatus,
            "currency" to order.currency,
            "totalNgn" to order.totalNgn,
            "itemCount" to order.itemCount,
            "itemsSummary" to order.itemsSummary.take(1000),
            "maskedDestination" to maskedDest.take(160),
            "courierName" to order.courierName.trim().takeIf { it.isNotBlank() }?.take(80),
            "trackingReference" to order.trackingReference.trim().takeIf { it.isNotBlank() }?.take(100),
            "trackingUrl" to resolvedUrl.takeIf { it.isNotBlank() }?.take(400),
            "estimatedDeliveryWindow" to order.estimatedDeliveryWindow.trim().takeIf { it.isNotBlank() }?.take(120),
            "deliveryStatus" to order.deliveryStatus.trim().takeIf { it.isNotBlank() }?.take(60),
            "deliveryNotes" to order.deliveryNotes.trim().takeIf { it.isNotBlank() }?.take(400),
            "dispatchedAt" to order.dispatchedAt,
            "deliveredAt" to order.deliveredAt,
            "statusHistory" to order.statusHistory.takeLast(30).takeIf { it.isNotEmpty() },
            "createdAt" to (order.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }.mapValues { it.value!! }

        runCatching {
            if (order.trackingToken.isNotBlank()) {
                db.collection("orderTrackingTokens").document(order.trackingToken).set(payload).await()
            }
            if (order.guestLookupHashEmail.isNotBlank()) {
                db.collection("orderTrackingLookups").document(order.guestLookupHashEmail).set(payload).await()
            }
            if (order.guestLookupHashPhone.isNotBlank()) {
                db.collection("orderTrackingLookups").document(order.guestLookupHashPhone).set(payload).await()
            }
        }
    }

    suspend fun createIdempotentCustomerNotification(
        notificationId: String,
        notification: AppNotification,
        alsoLogInApp: Boolean = true,
    ) {
        val notifRef = db.collection("notifications").document(notificationId)
        try {
            val snap = notifRef.get().await()
            if (!snap.exists()) {
                notifRef.set(notification.toCreateMap()).await()
            }
            if (alsoLogInApp) {
                val logRef = db.collection("communicationLogs").document("log_inapp_$notificationId")
                if (!logRef.get().await().exists()) {
                    val log = CommunicationLog(
                        userId = notification.userId,
                        recipientMasked = "In-App Bell",
                        orderId = notification.orderId,
                        orderNumber = notification.orderNumber,
                        channel = CommunicationChannel.IN_APP.name,
                        eventType = notification.category,
                        subject = notification.title,
                        bodyPreview = notification.body,
                        status = CommunicationStatus.SENT.name,
                        providerName = "Firestore In-App",
                        dedupeKey = notification.dedupeKey.ifBlank { notificationId },
                        retryCount = 0L,
                        lastAttemptAt = Timestamp.now(),
                    )
                    logRef.set(log.toCreateMap()).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-blocking notification failure shouldn't roll back primary order action
        }
    }

    // --- Support Center (Customer) ---

    fun observeCustomerEnquiries(userId: String): Flow<List<SupportEnquiry>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("supportEnquiries")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupportEnquiry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    val ex = fe ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, "supportEnquiries")
                    }
                    throw error
                }
        )
    }

    fun observeSupportReplies(enquiryId: String): Flow<List<SupportReply>> = flow {
        if (enquiryId.isBlank() || auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("supportEnquiries").document(enquiryId).collection("replies")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupportReply::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    throw error
                }
        )
    }

    suspend fun submitSupportEnquiry(
        userId: String,
        customerName: String,
        customerEmail: String,
        category: SupportCategory,
        subject: String,
        message: String,
        orderId: String = "",
        orderNumber: String = "",
    ): Result<String> {
        val docRef = db.collection("supportEnquiries").document()
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(subject.trim().length >= 2) { "Please enter a clear subject." }
            require(message.trim().length >= 5) { "Please describe how we can help (at least 5 characters)." }

            val enquiry = SupportEnquiry(
                userId = uid,
                customerName = customerName.trim().ifBlank { "Zeecrown Client" }.take(100),
                customerEmail = customerEmail.trim().take(200),
                category = category.name,
                subject = subject.trim().take(140),
                message = message.trim().take(2000),
                orderId = orderId.trim().take(128),
                orderNumber = orderNumber.trim().take(40),
                status = "OPEN",
                priority = if (category == SupportCategory.PAYMENT || category == SupportCategory.ORDER_ISSUE) "HIGH" else "NORMAL",
            )
            docRef.set(enquiry.toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun postCustomerSupportReply(
        userId: String,
        enquiry: SupportEnquiry,
        senderName: String,
        message: String,
    ): Result<Unit> {
        val replyRef = db.collection("supportEnquiries").document(enquiry.id).collection("replies").document()
        val enquiryRef = db.collection("supportEnquiries").document(enquiry.id)
        return try {
            val uid = requireUserId()
            check(uid == userId && enquiry.userId == uid) { "Signed-in user mismatch." }
            val cleanMsg = message.trim().take(2000)
            require(cleanMsg.isNotEmpty()) { "Message cannot be empty." }

            val reply = SupportReply(
                enquiryId = enquiry.id,
                ownerUid = enquiry.userId,
                senderUid = uid,
                senderName = senderName.trim().ifBlank { enquiry.customerName }.take(100),
                senderRole = "CUSTOMER",
                message = cleanMsg,
            )
            replyRef.set(reply.toCreateMap()).await()
            enquiryRef.update(
                mapOf(
                    "status" to "OPEN",
                    "lastReplyAt" to FieldValue.serverTimestamp(),
                    "lastReplyPreview" to cleanMsg.take(280),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, replyRef.path)
            Result.failure(e)
        }
    }

    // --- Delivery Feedback (Post-Purchase) ---

    fun observeCustomerDeliveryFeedback(userId: String): Flow<List<DeliveryFeedback>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("deliveryFeedback")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DeliveryFeedback::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    throw error
                }
        )
    }

    suspend fun submitDeliveryFeedback(
        userId: String,
        order: Order,
        customerName: String,
        overallRating: Long,
        deliveryRating: Long,
        packagingRating: Long,
        comment: String,
    ): Result<Unit> {
        val docId = "fb_${userId}_${order.id}"
        val docRef = db.collection("deliveryFeedback").document(docId)
        return try {
            val uid = requireUserId()
            check(uid == userId && order.userId == uid) { "Signed-in user mismatch." }
            check(order.status == OrderStatus.DELIVERED.name) {
                "Delivery feedback unlocks once the order is marked Delivered."
            }
            if (docRef.get().await().exists()) {
                return Result.failure(IllegalStateException("You have already submitted feedback for order ${order.orderNumber}."))
            }
            val feedback = DeliveryFeedback(
                userId = uid,
                orderId = order.id,
                orderNumber = order.orderNumber,
                customerName = customerName.trim().ifBlank { order.recipientName }.take(80),
                overallRating = overallRating.coerceIn(1L, 5L),
                deliveryRating = deliveryRating.coerceIn(1L, 5L),
                packagingRating = packagingRating.coerceIn(1L, 5L),
                comment = comment.trim().take(1000),
            )
            docRef.set(feedback.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
