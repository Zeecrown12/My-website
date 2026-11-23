package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.AuthManager
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AiAssistantConfig
import com.example.data.model.AiConversation
import com.example.data.model.AiFaqEntry
import com.example.data.model.AiUnansweredQuestion
import com.example.data.model.Category
import com.example.data.model.CommunicationLog
import com.example.data.model.Coupon
import com.example.data.model.CustomerAdminSummary
import com.example.data.model.DeliveryAddress
import com.example.data.model.DeliveryFeedback
import com.example.data.model.DeliverySettings
import com.example.data.model.DeliveryZone
import com.example.data.model.HomepageContent
import com.example.data.model.InventoryRecord
import com.example.data.model.InventoryTransaction
import com.example.data.model.Order
import com.example.data.model.PaymentRecord
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.SizeGuide
import com.example.data.model.StoreCollection
import com.example.data.model.SupportEnquiry
import com.example.data.model.SupportInternalNote
import com.example.data.model.SupportReply
import com.example.data.model.Testimonial
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeCurrentAdminRole(user: FirebaseUser?): Flow<AdminRole> = flow {
        if (user == null) {
            emit(AdminRole.NONE)
            return@flow
        }
        if (AuthManager.isBootstrappedAdmin(user)) {
            emit(AdminRole.SUPER_ADMIN)
            return@flow
        }
        val docRef = db.collection("adminUsers").document(user.uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    val admin = snap.toObject(AdminUser::class.java)
                    if (admin != null && admin.active) AdminRole.fromRaw(admin.role) else AdminRole.NONE
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(AdminRole.NONE)
                }
        )
    }

    private inline fun <reified T> observeAdminCollection(
        collectionPath: String,
        crossinline sortBlock: (List<T>) -> List<T>,
    ): Flow<List<T>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(collectionPath)
                .snapshots()
                .map { snapshot ->
                    val items = snapshot.toObjects(
                        T::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    sortBlock(items)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, collectionPath)
                    }
                    throw error
                }
        )
    }

    fun observeAllOrdersAdmin(): Flow<List<Order>> =
        observeAdminCollection<Order>("orders") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllProductsAdmin(): Flow<List<Product>> =
        observeAdminCollection<Product>("products") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllCategoriesAdmin(): Flow<List<Category>> =
        observeAdminCollection<Category>("categories") { list ->
            list.sortedBy { it.sortOrder }
        }

    fun observeAllCollectionsAdmin(): Flow<List<StoreCollection>> =
        observeAdminCollection<StoreCollection>("collections") { list ->
            list.sortedBy { it.sortOrder }
        }

    fun observeAllSizeGuidesAdmin(): Flow<List<SizeGuide>> =
        observeAdminCollection<SizeGuide>("sizeGuides") { list ->
            list.sortedBy { it.sortOrder }
        }

    fun observeAllInventoryAdmin(): Flow<Map<String, InventoryRecord>> =
        observeAdminCollection<InventoryRecord>("inventory") { it }
            .map { list -> list.associateBy { it.productId.ifBlank { it.id } } }

    fun observeInventoryTransactionsAdmin(): Flow<List<InventoryTransaction>> =
        observeAdminCollection<InventoryTransaction>("inventoryTransactions") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllPaymentsAdmin(): Flow<List<PaymentRecord>> =
        observeAdminCollection<PaymentRecord>("payments") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllCouponsAdmin(): Flow<List<Coupon>> =
        observeAdminCollection<Coupon>("coupons") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllDeliveryZonesAdmin(): Flow<List<DeliveryZone>> =
        observeAdminCollection<DeliveryZone>("deliveryZones") { list ->
            list.sortedBy { it.feeNgn }
        }

    fun observeDeliverySettingsAdmin(): Flow<DeliverySettings> = flow {
        val docRef = db.collection("deliverySettings").document("public")
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(
                        DeliverySettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: DeliverySettings()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    fun observeAllHomepageContentAdmin(): Flow<List<HomepageContent>> =
        observeAdminCollection<HomepageContent>("homepageContent") { list ->
            list.sortedBy { it.sortOrder }
        }

    fun observeAllTestimonialsAdmin(): Flow<List<Testimonial>> =
        observeAdminCollection<Testimonial>("testimonials") { list ->
            list.sortedBy { it.sortOrder }
        }

    fun observeAllReviewsAdmin(): Flow<List<Review>> =
        observeAdminCollection<Review>("reviews") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAdminUsers(): Flow<List<AdminUser>> =
        observeAdminCollection<AdminUser>("adminUsers") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeActivityLogs(): Flow<List<AdminActivityLog>> =
        observeAdminCollection<AdminActivityLog>("adminActivityLogs") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }.take(200)
        }

    fun observeCommunicationLogsAdmin(): Flow<List<CommunicationLog>> =
        observeAdminCollection<CommunicationLog>("communicationLogs") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }.take(300)
        }

    fun observeSupportEnquiriesAdmin(): Flow<List<SupportEnquiry>> =
        observeAdminCollection<SupportEnquiry>("supportEnquiries") { list ->
            list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeDeliveryFeedbackAdmin(): Flow<List<DeliveryFeedback>> =
        observeAdminCollection<DeliveryFeedback>("deliveryFeedback") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAiConfigAdmin(): Flow<AiAssistantConfig> = flow {
        val docRef = db.collection("aiConfig").document("public")
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(
                        AiAssistantConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: AiAssistantConfig()
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    emit(AiAssistantConfig())
                }
        )
    }

    fun observeAllAiFaqsAdmin(): Flow<List<AiFaqEntry>> =
        observeAdminCollection<AiFaqEntry>("aiKnowledgeFaqs") { list ->
            list.sortedBy { it.sortOrder }
        }

    fun observeAiUnansweredQuestionsAdmin(): Flow<List<AiUnansweredQuestion>> =
        observeAdminCollection<AiUnansweredQuestion>("aiUnansweredQuestions") { list ->
            list.sortedByDescending { it.occurrenceCount }
        }

    fun observeAiConversationsAdmin(): Flow<List<AiConversation>> =
        observeAdminCollection<AiConversation>("aiConversations") { list ->
            list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }.take(200)
        }

    fun observeSupportRepliesAdmin(enquiryId: String): Flow<List<SupportReply>> = flow {
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

    fun observeSupportInternalNotesAdmin(enquiryId: String): Flow<List<SupportInternalNote>> = flow {
        if (enquiryId.isBlank() || auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("supportEnquiries").document(enquiryId).collection("internalNotes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupportInternalNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    throw error
                }
        )
    }

    fun observeCustomerDirectoryAdmin(): Flow<List<CustomerAdminSummary>> {
        val usersFlow = observeAdminCollection<UserProfile>("users") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        val ordersFlow = observeAllOrdersAdmin()
        return combine(usersFlow, ordersFlow) { profiles, orders ->
            val ordersByUser = orders.groupBy { it.userId }
            profiles.map { profile ->
                val uid = profile.userId.ifBlank { profile.id }
                val userOrders = ordersByUser[uid].orEmpty()
                val paidOrders = userOrders.filter {
                    val ps = com.example.data.model.PaymentStatus.fromRaw(it.paymentStatus)
                    val os = com.example.data.model.OrderStatus.fromRaw(it.status)
                    (ps == com.example.data.model.PaymentStatus.PAID || ps == com.example.data.model.PaymentStatus.PARTIALLY_REFUNDED) &&
                        os != com.example.data.model.OrderStatus.CANCELLED &&
                        os != com.example.data.model.OrderStatus.REFUNDED
                }
                CustomerAdminSummary(
                    profile = profile,
                    privateInfo = null,
                    addresses = emptyList(),
                    orders = userOrders,
                    totalOrderCount = userOrders.size,
                    paidOrderCount = paidOrders.size,
                    lifetimeSpendPaidNgn = paidOrders.sumOf { (it.totalNgn - it.refundedNgn).coerceAtLeast(0.0) },
                )
            }
        }
    }

    suspend fun fetchCustomerDetails(userId: String): Result<Pair<UserPrivateInfo?, List<DeliveryAddress>>> {
        return try {
            val privSnap = db.collection("users").document(userId).collection("private").document("info").get().await()
            val priv = privSnap.toObject(UserPrivateInfo::class.java)
            val addrSnap = db.collection("users").document(userId).collection("addresses").get().await()
            val addrs = addrSnap.toObjects(DeliveryAddress::class.java)
            Result.success(priv to addrs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId/private")
            Result.failure(e)
        }
    }
}
