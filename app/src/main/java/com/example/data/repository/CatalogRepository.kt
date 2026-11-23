package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Category
import com.example.data.model.DeliverySettings
import com.example.data.model.DeliveryZone
import com.example.data.model.HomepageContent
import com.example.data.model.InventoryRecord
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.SizeGuide
import com.example.data.model.StoreCollection
import com.example.data.model.StoreSettings
import com.example.data.model.Testimonial
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class CatalogRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private inline fun <reified T> observeActiveCollection(
        collectionPath: String,
        crossinline sortBlock: (List<T>) -> List<T>,
    ): Flow<List<T>> = flow {
        emitAll(
            db.collection(collectionPath)
                .whereEqualTo("active", true)
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
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, collectionPath)
                    }
                    throw error
                }
        )
    }

    fun observeProducts(): Flow<List<Product>> =
        observeActiveCollection<Product>("products") { items ->
            items.sortedWith(
                compareByDescending<Product> { it.featured }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
        }

    fun observePagedProducts(limitCount: Long): Flow<List<Product>> = flow {
        val safeLimit = limitCount.coerceIn(1L, 120L)
        emitAll(
            db.collection("products")
                .whereEqualTo("active", true)
                .limit(safeLimit)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<Product> { it.featured }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "products")
                    }
                    throw error
                }
        )
    }

    fun observeProductById(productId: String): Flow<Product?> = flow {
        if (productId.isBlank()) {
            emit(null)
            return@flow
        }
        val docRef = db.collection("products").document(productId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )?.takeIf { it.active }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeProductByIdOrSlug(idOrSlug: String): Flow<Product?> {
        val clean = idOrSlug.trim()
        if (clean.isBlank()) return flowOf(null)
        val byIdFlow = observeProductById(clean).catch { emit(null) }
        val bySlugFlow = db.collection("products")
            .whereEqualTo("active", true)
            .whereEqualTo("slug", clean)
            .limit(1)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Product::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).firstOrNull()
            }
            .catch { emit(null) }
        return combine(byIdFlow, bySlugFlow) { byId, bySlug -> byId ?: bySlug }
    }

    fun searchProductsByToken(token: String, limitCount: Long = 24L): Flow<List<Product>> = flow {
        val cleanToken = token.trim().lowercase()
        if (cleanToken.length < 2) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("products")
                .whereEqualTo("active", true)
                .whereArrayContains("searchTokens", cleanToken)
                .limit(limitCount.coerceIn(1L, 60L))
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "products")
                    }
                    throw error
                }
        )
    }

    fun observeCategories(): Flow<List<Category>> =
        observeActiveCollection<Category>("categories") { items ->
            items.sortedBy { it.sortOrder }
        }

    fun observeCollections(): Flow<List<StoreCollection>> =
        observeActiveCollection<StoreCollection>("collections") { items ->
            items.sortedBy { it.sortOrder }
        }

    fun observeSizeGuides(): Flow<List<SizeGuide>> =
        observeActiveCollection<SizeGuide>("sizeGuides") { items ->
            items.sortedBy { it.sortOrder }
        }

    fun observeDeliveryZones(): Flow<List<DeliveryZone>> =
        observeActiveCollection<DeliveryZone>("deliveryZones") { items ->
            items.sortedBy { it.feeNgn }
        }

    fun observeHomepageContent(): Flow<List<HomepageContent>> =
        observeActiveCollection<HomepageContent>("homepageContent") { items ->
            items.sortedBy { it.sortOrder }
        }

    fun observeInStockInventory(): Flow<Map<String, InventoryRecord>> = flow {
        emitAll(
            db.collection("inventory")
                .whereEqualTo("inStock", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InventoryRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).associateBy { it.productId.ifBlank { it.id } }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "inventory")
                    }
                    throw error
                }
        )
    }

    fun observeInventoryForProduct(productId: String): Flow<InventoryRecord?> = flow {
        if (productId.isBlank()) {
            emit(null)
            return@flow
        }
        val docRef = db.collection("inventory").document(productId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        InventoryRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observePublicStoreSettings(): Flow<StoreSettings> = flow {
        val docRef = db.collection("storeSettings").document("public")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        StoreSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: StoreSettings()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeReviewsForProduct(productId: String): Flow<List<Review>> = flow {
        if (productId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("reviews")
                .whereEqualTo("visible", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Review::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { it.productId == productId }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "reviews")
                    }
                    throw error
                }
        )
    }

    fun observeAllVisibleReviews(): Flow<List<Review>> = flow {
        emitAll(
            db.collection("reviews")
                .whereEqualTo("visible", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Review::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "reviews")
                    }
                    throw error
                }
        )
    }

    fun observeDeliverySettings(): Flow<DeliverySettings> = flow {
        val docRef = db.collection("deliverySettings").document("public")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
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
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeTestimonials(): Flow<List<Testimonial>> =
        observeActiveCollection<Testimonial>("testimonials") { items ->
            items.sortedBy { it.sortOrder }
        }
}

