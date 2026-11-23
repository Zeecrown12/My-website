package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AiAssistantConfig
import com.example.data.model.AiFaqEntry
import com.example.data.model.AiUnansweredQuestion
import com.example.data.model.AppNotification
import com.example.data.model.CatalogQueryEngine
import com.example.data.model.Category
import com.example.data.model.CommunicationChannel
import com.example.data.model.CommunicationLog
import com.example.data.model.CommunicationStatus
import com.example.data.model.Coupon
import com.example.data.model.DefaultCourierTrackingAdapter
import com.example.data.model.DeliverySettings
import com.example.data.model.DeliveryZone
import com.example.data.model.HomepageContent
import com.example.data.model.InventoryRecord
import com.example.data.model.InventoryTransaction
import com.example.data.model.NotificationPreferences
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderTimelineEngine
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.SeoUtils
import com.example.data.model.SizeGuide
import com.example.data.model.StoreCollection
import com.example.data.model.StoreSettings
import com.example.data.model.SupportEnquiry
import com.example.data.model.SupportInternalNote
import com.example.data.model.SupportReply
import com.example.data.model.SupportStatus
import com.example.data.model.Testimonial
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AdminOperationsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    private fun requireActor(role: AdminRole): Pair<String, String> {
        val user = auth.currentUser
            ?: throw IllegalStateException("Administrator must be signed in.")
        check(role.isAdmin) { "Access denied: active administrator role required." }
        return user.uid to (user.email.orEmpty().ifBlank { user.uid })
    }

    private suspend fun recordAudit(
        actorRole: AdminRole,
        action: String,
        targetType: String,
        targetId: String,
        summary: String,
        previousValue: String = "",
        newValue: String = "",
    ) {
        val user = auth.currentUser ?: return
        val log = AdminActivityLog(
            adminUid = user.uid,
            adminEmail = user.email.orEmpty(),
            adminRole = actorRole.rawKey,
            action = action,
            targetType = targetType,
            targetId = targetId.ifBlank { "global" },
            summary = summary,
            previousValue = previousValue,
            newValue = newValue,
        )
        runCatching {
            db.collection("adminActivityLogs").document().set(log.toCreateMap()).await()
        }
    }

    suspend fun saveOrUpdateProduct(
        context: Context,
        actorRole: AdminRole,
        existingProductId: String?,
        title: String,
        description: String,
        categoryId: String,
        collectionId: String,
        brand: String,
        sku: String,
        priceNgn: Double,
        compareAtPriceNgn: Double?,
        priceUsd: Double,
        sizes: List<String>,
        colors: List<String>,
        imageEntries: List<String>,
        initialOrUpdatedStock: Long,
        lowStockThreshold: Long,
        featured: Boolean,
        newArrival: Boolean,
        active: Boolean,
        attributes: Map<String, String>,
        unavailableVariants: List<String>,
        seoTitle: String,
        seoDescription: String,
        categoryName: String,
        collectionName: String,
        slug: String = "",
        socialImageUrl: String = "",
        sizeGuideId: String = "",
        summary: String = "",
        features: List<String> = emptyList(),
        fashionStyle: String = "",
        suitableOccasions: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        aiAssistedDraft: Boolean = false,
        preallocatedNewProductId: String? = null,
        onUploadProgress: ((Float) -> Unit)? = null,
    ): Result<String> {
        val resolvedNewDocRef = preallocatedNewProductId?.takeIf { it.isNotBlank() }?.let {
            db.collection("products").document(it)
        } ?: db.collection("products").document()
        val docRef = if (existingProductId.isNullOrBlank()) {
            resolvedNewDocRef
        } else {
            db.collection("products").document(existingProductId)
        }
        return try {
            val (uid, email) = requireActor(actorRole)
            check(actorRole.canManageProducts) { "Your role lacks product management permission." }
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.length >= 2) { "Product title must be at least 2 characters." }
            val cleanSlug = SeoUtils.sanitizeSlug(slug.ifBlank { cleanTitle }, "zeecrown-item").take(120)
            val resolvedSku = sku.trim().ifBlank { "ZC-${docRef.id.take(6).uppercase()}" }.take(60)
            val cleanDesc = description.trim().ifBlank { cleanTitle }.take(2000)
            val cleanBrand = brand.trim().ifBlank { "Zeecrown Atelier" }.take(80)
            val cleanSizes = sizes.take(15)
            val cleanColors = colors.take(15)
            val cleanSummary = summary.trim().take(300)
            val cleanFeatures = features.map { it.trim().take(140) }.filter { it.isNotBlank() }.take(10)
            val cleanStyle = fashionStyle.trim().take(80)
            val cleanOccasions = suitableOccasions.map { it.trim().take(60) }.filter { it.isNotBlank() }.take(8)
            val cleanTags = tags.map { it.trim().take(40) }.filter { it.isNotBlank() }.distinct().take(15)

            // Upload any local content:// image URIs cleanly to Firebase Storage
            val resolvedImages = mutableListOf<String>()
            val uploadTotal = imageEntries.take(10).count { it.startsWith("content://") || it.startsWith("file://") }.coerceAtLeast(1)
            var uploadedIdx = 0
            for ((idx, entry) in imageEntries.take(10).withIndex()) {
                if (entry.startsWith("content://") || entry.startsWith("file://")) {
                    val baseOffset = uploadedIdx.toFloat() / uploadTotal.toFloat()
                    val slice = 1f / uploadTotal.toFloat()
                    val uploadRes = AdminStorageHelper.uploadImageUri(
                        context = context,
                        uri = Uri.parse(entry),
                        folderPath = "products/${docRef.id}",
                        fixedFileName = "img_${System.currentTimeMillis()}_$idx.jpg",
                        onProgress = { pct -> onUploadProgress?.invoke((baseOffset + pct * slice).coerceIn(0f, 1f)) },
                    )
                    resolvedImages += uploadRes.getOrThrow()
                    uploadedIdx++
                } else if (entry.isNotBlank()) {
                    resolvedImages += entry
                }
            }
            if (resolvedImages.isEmpty()) {
                resolvedImages += "drawable://img_cat_adire_abaya"
            }

            val baseTokens = CatalogQueryEngine.buildSearchTokens(
                title = cleanTitle,
                description = "$cleanDesc $cleanSummary $cleanStyle",
                categoryName = categoryName,
                collectionName = collectionName,
                brand = cleanBrand,
                sku = resolvedSku,
                sizes = cleanSizes,
                colors = cleanColors,
            )
            val tokens = (baseTokens + cleanTags.map { it.lowercase() } + cleanOccasions.map { it.lowercase() })
                .distinct()
                .take(75)

            val existingSnap = docRef.get().await()
            val safeStock = initialOrUpdatedStock.coerceIn(0L, 100000L)
            val safeThreshold = lowStockThreshold.coerceIn(0L, 10000L)

            if (existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "slug" to cleanSlug,
                    "description" to cleanDesc,
                    "categoryId" to categoryId,
                    "collectionId" to collectionId.trim().take(128),
                    "priceNgn" to priceNgn.coerceAtLeast(0.0),
                    "priceUsd" to priceUsd.coerceAtLeast(0.0),
                    "mainImageUrl" to resolvedImages.first(),
                    "imageUrls" to resolvedImages,
                    "sizes" to cleanSizes,
                    "colors" to cleanColors,
                    "featured" to featured,
                    "newArrival" to newArrival,
                    "brand" to cleanBrand,
                    "sku" to resolvedSku,
                    "attributes" to attributes.entries.take(20).associate { it.key.take(60) to it.value.take(160) },
                    "unavailableVariants" to unavailableVariants.take(40),
                    "searchTokens" to tokens,
                    "seoTitle" to seoTitle.trim().take(120),
                    "seoDescription" to seoDescription.trim().take(300),
                    "socialImageUrl" to socialImageUrl.trim().take(600),
                    "sizeGuideId" to sizeGuideId.trim().take(128),
                    "summary" to cleanSummary,
                    "features" to cleanFeatures,
                    "fashionStyle" to cleanStyle,
                    "suitableOccasions" to cleanOccasions,
                    "tags" to cleanTags,
                    "aiAssistedDraft" to aiAssistedDraft,
                    "lowStockThreshold" to safeThreshold,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                val validCompare = compareAtPriceNgn?.takeIf { it > priceNgn }
                if (validCompare != null) {
                    updates["compareAtPriceNgn"] = validCompare
                }
                docRef.update(updates).await()
                recordAudit(actorRole, "UPDATE_PRODUCT", "product", docRef.id, "Updated product '$cleanTitle'")
            } else {
                val product = Product(
                    title = cleanTitle,
                    slug = cleanSlug,
                    description = cleanDesc,
                    categoryId = categoryId,
                    collectionId = collectionId.trim().take(128),
                    priceNgn = priceNgn.coerceAtLeast(0.0),
                    compareAtPriceNgn = compareAtPriceNgn?.takeIf { it > priceNgn },
                    priceUsd = priceUsd.coerceAtLeast(0.0),
                    mainImageUrl = resolvedImages.first(),
                    imageUrls = resolvedImages,
                    sizes = cleanSizes,
                    colors = cleanColors,
                    featured = featured,
                    newArrival = newArrival,
                    soldCount = 0L,
                    brand = cleanBrand,
                    sku = resolvedSku,
                    attributes = attributes.entries.take(20).associate { it.key.take(60) to it.value.take(160) },
                    unavailableVariants = unavailableVariants.take(40),
                    searchTokens = tokens,
                    seoTitle = seoTitle.trim().take(120),
                    seoDescription = seoDescription.trim().take(300),
                    socialImageUrl = socialImageUrl.trim().take(600),
                    sizeGuideId = sizeGuideId.trim().take(128),
                    summary = cleanSummary,
                    features = cleanFeatures,
                    fashionStyle = cleanStyle,
                    suitableOccasions = cleanOccasions,
                    tags = cleanTags,
                    aiAssistedDraft = aiAssistedDraft,
                    lowStockThreshold = safeThreshold,
                    active = active,
                )
                docRef.set(product.toCreateMap()).await()
                val inv = InventoryRecord(
                    productId = docRef.id,
                    sku = resolvedSku,
                    stockQuantity = safeStock,
                    lowStockThreshold = safeThreshold,
                    reservedQuantity = 0L,
                    inStock = safeStock > 0L,
                )
                db.collection("inventory").document(docRef.id).set(inv.toCreateMap()).await()
                val tx = InventoryTransaction(
                    productId = docRef.id,
                    productTitle = cleanTitle,
                    sku = resolvedSku,
                    previousQty = 0L,
                    deltaQty = safeStock,
                    newQty = safeStock,
                    reason = "Initial product stock",
                    adminUid = uid,
                    adminEmail = email,
                )
                db.collection("inventoryTransactions").document().set(tx.toCreateMap()).await()
                recordAudit(actorRole, "CREATE_PRODUCT", "product", docRef.id, "Created product '$cleanTitle' with stock $safeStock")
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setProductActive(actorRole: AdminRole, product: Product, active: Boolean): Result<Unit> {
        val docRef = db.collection("products").document(product.id)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageProducts) { "Your role lacks product management permission." }
            docRef.update(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp())).await()
            recordAudit(actorRole, if (active) "PUBLISH_PRODUCT" else "UNPUBLISH_PRODUCT", "product", product.id, "${if (active) "Published" else "Unpublished"} '${product.title}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun duplicateProduct(actorRole: AdminRole, source: Product, sourceStock: Long): Result<String> {
        val docRef = db.collection("products").document()
        return try {
            val (uid, email) = requireActor(actorRole)
            check(actorRole.canManageProducts) { "Your role lacks product management permission." }
            val newTitle = "${source.title} (Copy)".take(120)
            val newSku = "ZC-${docRef.id.take(6).uppercase()}"
            val copy = source.copy(
                id = "",
                title = newTitle,
                slug = "${source.slug}-copy-${docRef.id.take(4)}".take(120),
                sku = newSku,
                soldCount = 0L,
                active = false,
            )
            docRef.set(copy.toCreateMap()).await()
            val inv = InventoryRecord(
                productId = docRef.id,
                sku = newSku,
                stockQuantity = sourceStock.coerceIn(0L, 100000L),
                lowStockThreshold = source.lowStockThreshold,
                reservedQuantity = 0L,
                inStock = sourceStock > 0L,
            )
            db.collection("inventory").document(docRef.id).set(inv.toCreateMap()).await()
            val tx = InventoryTransaction(
                productId = docRef.id,
                productTitle = newTitle,
                sku = newSku,
                previousQty = 0L,
                deltaQty = sourceStock,
                newQty = sourceStock,
                reason = "Duplicated from ${source.sku}",
                adminUid = uid,
                adminEmail = email,
            )
            db.collection("inventoryTransactions").document().set(tx.toCreateMap()).await()
            recordAudit(actorRole, "DUPLICATE_PRODUCT", "product", docRef.id, "Duplicated '${source.title}' as draft '$newTitle'")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(actorRole: AdminRole, product: Product): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManageProducts) { "Your role lacks product deletion permission." }
            val batch = db.batch()
            batch.delete(db.collection("products").document(product.id))
            batch.delete(db.collection("inventory").document(product.id))
            batch.commit().await()
            recordAudit(actorRole, "DELETE_PRODUCT", "product", product.id, "Deleted product '${product.title}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "products/${product.id}")
            Result.failure(e)
        }
    }

    suspend fun saveCategory(
        context: Context,
        actorRole: AdminRole,
        categoryId: String?,
        name: String,
        description: String,
        parentCategoryId: String,
        imageKeyOrUri: String,
        seoTitle: String,
        seoDescription: String,
        sortOrder: Long,
        active: Boolean,
        slug: String = "",
        socialImageUrl: String = "",
        sizeGuideId: String = "",
    ): Result<String> {
        val cleanName = name.trim().take(80)
        val cleanSlug = SeoUtils.sanitizeSlug(slug.ifBlank { cleanName }, "category").take(80)
        val targetRef = if (categoryId.isNullOrBlank()) {
            db.collection("categories").document("cat_${cleanSlug.replace("-", "_")}".take(60))
        } else {
            db.collection("categories").document(categoryId)
        }
        return try {
            requireActor(actorRole)
            check(actorRole.canManageCategories) { "Your role lacks category management permission." }
            require(cleanName.length >= 2) { "Category name must be at least 2 characters." }

            val finalImageKey = if (imageKeyOrUri.startsWith("content://") || imageKeyOrUri.startsWith("file://")) {
                AdminStorageHelper.uploadImageUri(
                    context = context,
                    uri = Uri.parse(imageKeyOrUri),
                    folderPath = "categories/${targetRef.id}",
                    fixedFileName = "cover_${System.currentTimeMillis()}.jpg",
                ).getOrThrow()
            } else {
                imageKeyOrUri.trim().ifBlank { "img_cat_adire_abaya" }.take(600)
            }

            val existing = targetRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "slug" to cleanSlug,
                    "description" to description.trim().take(400),
                    "imageKey" to finalImageKey,
                    "parentCategoryId" to parentCategoryId.trim().take(128),
                    "seoTitle" to seoTitle.trim().take(120),
                    "seoDescription" to seoDescription.trim().take(300),
                    "socialImageUrl" to socialImageUrl.trim().take(600),
                    "sizeGuideId" to sizeGuideId.trim().take(128),
                    "sortOrder" to sortOrder.coerceIn(0L, 1000L),
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                targetRef.update(updates).await()
                recordAudit(actorRole, "UPDATE_CATEGORY", "category", targetRef.id, "Updated category '$cleanName'")
            } else {
                val category = Category(
                    name = cleanName,
                    slug = cleanSlug,
                    description = description.trim().take(400),
                    imageKey = finalImageKey,
                    parentCategoryId = parentCategoryId.trim().take(128),
                    seoTitle = seoTitle.trim().take(120),
                    seoDescription = seoDescription.trim().take(300),
                    socialImageUrl = socialImageUrl.trim().take(600),
                    sizeGuideId = sizeGuideId.trim().take(128),
                    sortOrder = sortOrder.coerceIn(0L, 1000L),
                    active = active,
                )
                targetRef.set(category.toCreateMap()).await()
                recordAudit(actorRole, "CREATE_CATEGORY", "category", targetRef.id, "Created category '$cleanName'")
            }
            Result.success(targetRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategorySafely(
        actorRole: AdminRole,
        category: Category,
        allProducts: List<Product>,
        allCategories: List<Category>,
    ): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManageCategories) { "Your role lacks category management permission." }
            val linkedProducts = allProducts.count { it.categoryId == category.id }
            val linkedChildren = allCategories.count { it.parentCategoryId == category.id }
            if (linkedProducts > 0 || linkedChildren > 0) {
                return Result.failure(
                    IllegalStateException("Cannot delete '${category.name}': $linkedProducts product(s) and $linkedChildren subcategory(ies) still reference it. Archive/deactivate it or reassign items first.")
                )
            }
            db.collection("categories").document(category.id).delete().await()
            recordAudit(actorRole, "DELETE_CATEGORY", "category", category.id, "Deleted unused category '${category.name}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "categories/${category.id}")
            Result.failure(e)
        }
    }

    suspend fun reorderCategories(actorRole: AdminRole, firstId: String, firstNewOrder: Long, secondId: String, secondNewOrder: Long): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManageCategories) { "Your role lacks category management permission." }
            val batch = db.batch()
            batch.update(db.collection("categories").document(firstId), mapOf("sortOrder" to firstNewOrder.coerceIn(0L, 1000L), "updatedAt" to FieldValue.serverTimestamp()))
            batch.update(db.collection("categories").document(secondId), mapOf("sortOrder" to secondNewOrder.coerceIn(0L, 1000L), "updatedAt" to FieldValue.serverTimestamp()))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "categories")
            Result.failure(e)
        }
    }

    suspend fun setCategoryActive(actorRole: AdminRole, category: Category, active: Boolean): Result<Unit> {
        val docRef = db.collection("categories").document(category.id)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageCategories) { "Your role lacks category permission." }
            docRef.update(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp())).await()
            recordAudit(actorRole, if (active) "ACTIVATE_CATEGORY" else "ARCHIVE_CATEGORY", "category", category.id, "${if (active) "Activated" else "Archived"} category '${category.name}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adjustInventoryStock(
        actorRole: AdminRole,
        product: Product,
        currentRecord: InventoryRecord?,
        newTotalStock: Long,
        newThreshold: Long,
        variantKey: String,
        newVariantQty: Long?,
        reason: String,
    ): Result<Unit> {
        val invRef = db.collection("inventory").document(product.id)
        val txRef = db.collection("inventoryTransactions").document()
        return try {
            val (uid, email) = requireActor(actorRole)
            check(actorRole.canManageInventory) { "Your role lacks inventory management permission." }
            val cleanReason = reason.trim().ifBlank { "Manual stock adjustment" }.take(200)
            val prevStock = currentRecord?.stockQuantity ?: 0L
            val safeNewStock = newTotalStock.coerceIn(0L, 100000L)
            val safeThreshold = newThreshold.coerceIn(0L, 10000L)
            val updatedVariantStock = (currentRecord?.variantStock ?: emptyMap()).toMutableMap()
            if (variantKey.isNotBlank() && newVariantQty != null) {
                updatedVariantStock[variantKey] = newVariantQty.coerceIn(0L, 100000L)
            }

            val batch = db.batch()
            val resolvedSku = (currentRecord?.sku?.ifBlank { product.sku } ?: product.sku).ifBlank { "ZC-${product.id.take(6).uppercase()}" }
            if (currentRecord == null) {
                val rec = InventoryRecord(
                    productId = product.id,
                    sku = resolvedSku,
                    stockQuantity = safeNewStock,
                    lowStockThreshold = safeThreshold,
                    reservedQuantity = 0L,
                    variantStock = updatedVariantStock,
                    inStock = safeNewStock > 0L,
                )
                batch.set(invRef, rec.toCreateMap())
            } else {
                val updates = mutableMapOf<String, Any>(
                    "stockQuantity" to safeNewStock,
                    "lowStockThreshold" to safeThreshold,
                    "inStock" to (safeNewStock > 0L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (updatedVariantStock.isNotEmpty()) {
                    updates["variantStock"] = updatedVariantStock
                }
                batch.update(invRef, updates)
            }
            val tx = InventoryTransaction(
                productId = product.id,
                productTitle = product.title,
                sku = resolvedSku,
                variation = variantKey,
                previousQty = prevStock,
                deltaQty = safeNewStock - prevStock,
                newQty = safeNewStock,
                reason = cleanReason,
                adminUid = uid,
                adminEmail = email,
            )
            batch.set(txRef, tx.toCreateMap())
            batch.commit().await()
            recordAudit(
                actorRole,
                "ADJUST_STOCK",
                "inventory",
                product.id,
                "Adjusted '${product.title}' stock $prevStock -> $safeNewStock ($cleanReason)",
                previousValue = prevStock.toString(),
                newValue = safeNewStock.toString(),
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, invRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderFulfillment(
        actorRole: AdminRole,
        order: Order,
        newStatus: OrderStatus,
        internalNotes: String,
        cancellationDecision: String = order.cancellationStatus,
        courierName: String = order.courierName,
        trackingReference: String = order.trackingReference,
        trackingUrl: String = order.trackingUrl,
        estimatedDeliveryWindow: String = order.estimatedDeliveryWindow,
        deliveryStatus: String = order.deliveryStatus,
        deliveryNotes: String = order.deliveryNotes,
    ): Result<Unit> {
        val docRef = db.collection("orders").document(order.id)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageOrders) { "Your role lacks order management permission." }
            // Never permit updating to PAID via fulfillment tab; enforce payment verification workflow!
            if (newStatus == OrderStatus.DELIVERED && PaymentStatus.fromRaw(order.paymentStatus) == PaymentStatus.UNPAID &&
                order.paymentMethod != com.example.data.model.PaymentMethod.COD.name
            ) {
                return Result.failure(
                    IllegalStateException("Verify payment in Payments Center before marking prepaid order as Delivered.")
                )
            }

            val cleanCourier = courierName.trim().take(80)
            val cleanRef = trackingReference.trim().take(100)
            val resolvedTrackingUrl = DefaultCourierTrackingAdapter.resolveTrackingUrl(
                courierName = cleanCourier,
                trackingReference = cleanRef,
                explicitUrl = trackingUrl.trim(),
            ).take(400)
            val cleanWindow = estimatedDeliveryWindow.trim().take(120)
            val cleanDelivStatus = deliveryStatus.trim().ifBlank { newStatus.label }.take(60)
            val cleanDelivNotes = deliveryNotes.trim().take(400)

            val history = order.statusHistory.toMutableList()
            if (order.status != newStatus.name || cleanDelivNotes != order.deliveryNotes) {
                history.add(
                    OrderTimelineEngine.historyEntry(
                        stage = newStatus.name,
                        label = newStatus.label,
                        note = cleanDelivNotes.ifBlank {
                            if (cleanCourier.isNotBlank() && cleanRef.isNotBlank()) {
                                "$cleanCourier ($cleanRef)"
                            } else {
                                "Updated by ZEECROWN store operations"
                            }
                        },
                        actorRole = actorRole.rawKey,
                    )
                )
            }
            val boundedHistory = history.takeLast(30)

            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "internalNotes" to internalNotes.trim().take(1000),
                "cancellationStatus" to cancellationDecision,
                "courierName" to cleanCourier,
                "trackingReference" to cleanRef,
                "trackingUrl" to resolvedTrackingUrl,
                "estimatedDeliveryWindow" to cleanWindow,
                "deliveryStatus" to cleanDelivStatus,
                "deliveryNotes" to cleanDelivNotes,
                "statusHistory" to boundedHistory,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            val isShippedStage = newStatus in setOf(OrderStatus.SHIPPED, OrderStatus.DISPATCHED, OrderStatus.OUT_FOR_DELIVERY)
            if (isShippedStage && order.dispatchedAt == null) {
                updates["dispatchedAt"] = FieldValue.serverTimestamp()
            }
            if (newStatus == OrderStatus.DELIVERED && order.deliveredAt == null) {
                updates["deliveredAt"] = FieldValue.serverTimestamp()
            }

            docRef.update(updates).await()

            val updatedOrder = order.copy(
                status = newStatus.name,
                internalNotes = internalNotes.trim().take(1000),
                cancellationStatus = cancellationDecision,
                courierName = cleanCourier,
                trackingReference = cleanRef,
                trackingUrl = resolvedTrackingUrl,
                estimatedDeliveryWindow = cleanWindow,
                deliveryStatus = cleanDelivStatus,
                deliveryNotes = cleanDelivNotes,
                statusHistory = boundedHistory,
            )
            val commsRepo = CommunicationRepository(db, auth)
            commsRepo.syncOrderTrackingProjections(updatedOrder)

            if (order.status != newStatus.name) {
                val isDeliveryStage = newStatus in setOf(
                    OrderStatus.READY_FOR_DISPATCH,
                    OrderStatus.SHIPPED,
                    OrderStatus.DISPATCHED,
                    OrderStatus.OUT_FOR_DELIVERY,
                    OrderStatus.DELIVERED,
                )
                val body = buildString {
                    append("Order ${order.orderNumber} is now ${newStatus.label}.")
                    if (cleanCourier.isNotBlank() && cleanRef.isNotBlank()) {
                        append(" Courier: $cleanCourier ($cleanRef).")
                    }
                    if (cleanWindow.isNotBlank()) {
                        append(" Window: $cleanWindow.")
                    }
                }.take(400)
                commsRepo.createIdempotentCustomerNotification(
                    notificationId = "notif_status_${newStatus.name}_${order.id}",
                    notification = AppNotification(
                        userId = order.userId,
                        title = newStatus.label,
                        body = body,
                        details = cleanDelivNotes,
                        category = if (isDeliveryStage) "DELIVERY" else "ORDER",
                        readFlag = false,
                        targetRoute = "track_order/${order.id}",
                        orderId = order.id,
                        orderNumber = order.orderNumber,
                        dedupeKey = "order_status_${newStatus.name}_${order.id}",
                    )
                )
            }

            recordAudit(
                actorRole,
                "UPDATE_ORDER_STATUS",
                "order",
                order.id,
                "Order ${order.orderNumber} -> ${newStatus.label}" +
                    if (cleanRef.isNotBlank()) " [$cleanCourier $cleanRef]" else "",
                previousValue = order.status,
                newValue = newStatus.name,
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun reviewPaymentVerification(
        actorRole: AdminRole,
        payment: PaymentRecord,
        order: Order?,
        approve: Boolean,
        verificationNote: String,
    ): Result<Unit> {
        val payRef = db.collection("payments").document(payment.id)
        return try {
            val (_, email) = requireActor(actorRole)
            check(actorRole.canVerifyPayments) { "Your role lacks payment verification permission." }
            val cleanNote = verificationNote.trim().ifBlank {
                if (approve) "Verified by finance admin ($email)" else "Rejected verification ($email)"
            }.take(400)
            val batch = db.batch()
            batch.update(
                payRef,
                mapOf(
                    "status" to if (approve) PaymentRecordStatus.VERIFIED.name else PaymentRecordStatus.FAILED.name,
                    "verificationNote" to cleanNote,
                    "reviewedByEmail" to email.take(200),
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            var updatedHistory: List<Map<String, Any>> = order?.statusHistory ?: emptyList()
            var targetOrderStatus = order?.status ?: OrderStatus.PENDING_PAYMENT.name
            val nextPaymentStatus = if (approve) PaymentStatus.PAID.name else PaymentStatus.FAILED.name
            if (payment.orderId.isNotBlank() && order != null) {
                val orderRef = db.collection("orders").document(payment.orderId)
                targetOrderStatus = if (approve) {
                    if (order.status in setOf(OrderStatus.PENDING_PAYMENT.name, OrderStatus.PAYMENT_SUBMITTED.name)) {
                        OrderStatus.CONFIRMED.name
                    } else {
                        order.status
                    }
                } else {
                    OrderStatus.PENDING_PAYMENT.name
                }
                updatedHistory = (order.statusHistory + OrderTimelineEngine.historyEntry(
                    stage = targetOrderStatus,
                    label = if (approve) "Payment Verified" else "Payment Failed",
                    note = cleanNote,
                    actorRole = actorRole.rawKey,
                )).takeLast(30)

                batch.update(
                    orderRef,
                    mapOf(
                        "paymentStatus" to nextPaymentStatus,
                        "status" to targetOrderStatus,
                        "statusHistory" to updatedHistory,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()

            if (order != null) {
                val commsRepo = CommunicationRepository(db, auth)
                commsRepo.syncOrderTrackingProjections(
                    order.copy(
                        status = targetOrderStatus,
                        paymentStatus = nextPaymentStatus,
                        statusHistory = updatedHistory,
                    )
                )
                val notifId = if (approve) {
                    "notif_payver_${order.id}"
                } else {
                    "notif_payfail_${order.id}_${System.currentTimeMillis() / 1000L}"
                }
                commsRepo.createIdempotentCustomerNotification(
                    notificationId = notifId,
                    notification = AppNotification(
                        userId = order.userId,
                        title = if (approve) "Payment Verified" else "Payment Could Not Be Verified",
                        body = if (approve) {
                            "Payment for order ${order.orderNumber} is confirmed! Tailored fulfillment begins shortly."
                        } else {
                            "Payment for order ${order.orderNumber} failed verification. Open Track Order to safely retry payment."
                        },
                        details = cleanNote,
                        category = "PAYMENT",
                        readFlag = false,
                        targetRoute = "track_order/${order.id}",
                        orderId = order.id,
                        orderNumber = order.orderNumber,
                        dedupeKey = notifId,
                    )
                )
            }

            recordAudit(
                actorRole,
                if (approve) "APPROVE_PAYMENT" else "REJECT_PAYMENT",
                "payment",
                payment.id,
                "${if (approve) "Verified" else "Rejected"} payment ${payment.reference} (${payment.provider}): $cleanNote",
                previousValue = payment.status,
                newValue = if (approve) PaymentRecordStatus.VERIFIED.name else PaymentRecordStatus.FAILED.name,
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, payRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordOrderRefund(
        actorRole: AdminRole,
        order: Order,
        refundAmountNgn: Double,
        reasonNote: String,
    ): Result<Unit> {
        val docRef = db.collection("orders").document(order.id)
        return try {
            requireActor(actorRole)
            check(actorRole.canVerifyPayments) { "Your role lacks refund authorization." }
            val safeRefund = refundAmountNgn.coerceIn(0.0, order.totalNgn)
            val fullRefund = safeRefund >= order.totalNgn - 0.01
            val nextPayStatus = if (fullRefund) PaymentStatus.REFUNDED.name else PaymentStatus.PARTIALLY_REFUNDED.name
            val nextOrderStatus = if (fullRefund) OrderStatus.REFUNDED.name else order.status
            val updatedHistory = (order.statusHistory + OrderTimelineEngine.historyEntry(
                stage = nextOrderStatus,
                label = if (fullRefund) "Order Refunded" else "Partial Refund Recorded",
                note = "₦${safeRefund.toLong()} — ${reasonNote.trim()}",
                actorRole = actorRole.rawKey,
            )).takeLast(30)

            docRef.update(
                mapOf(
                    "refundedNgn" to safeRefund,
                    "paymentStatus" to nextPayStatus,
                    "status" to nextOrderStatus,
                    "internalNotes" to "${order.internalNotes}\n[Refund ₦${safeRefund.toLong()}]: ${reasonNote.trim()}".trim().take(1000),
                    "statusHistory" to updatedHistory,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            val commsRepo = CommunicationRepository(db, auth)
            commsRepo.syncOrderTrackingProjections(
                order.copy(
                    status = nextOrderStatus,
                    paymentStatus = nextPayStatus,
                    statusHistory = updatedHistory,
                )
            )
            commsRepo.createIdempotentCustomerNotification(
                notificationId = "notif_refund_${order.id}",
                notification = AppNotification(
                    userId = order.userId,
                    title = "Refund Update",
                    body = "A refund of ₦${safeRefund.toLong()} was recorded on order ${order.orderNumber}.",
                    details = reasonNote.trim(),
                    category = "PAYMENT",
                    readFlag = false,
                    targetRoute = "track_order/${order.id}",
                    orderId = order.id,
                    orderNumber = order.orderNumber,
                    dedupeKey = "refund_${order.id}",
                )
            )

            recordAudit(actorRole, "REFUND_ORDER", "order", order.id, "Recorded ₦${safeRefund.toLong()} refund on ${order.orderNumber}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomerSupportNotes(actorRole: AdminRole, userId: String, notes: String): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            requireActor(actorRole)
            check(actorRole.canEditCustomerNotes) { "Your role cannot edit customer support notes." }
            docRef.update(
                mapOf(
                    "supportNotes" to notes.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            recordAudit(actorRole, "UPDATE_CUSTOMER_NOTES", "user", userId, "Updated internal support notes for customer $userId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveAdminTeamMember(
        actorRole: AdminRole,
        targetUid: String,
        email: String,
        displayName: String,
        role: AdminRole,
        active: Boolean,
    ): Result<Unit> {
        val cleanUid = targetUid.trim()
        val docRef = db.collection("adminUsers").document(cleanUid)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageTeam) { "Only Super Admins can manage administrator team roles." }
            require(cleanUid.isNotBlank() && email.contains("@") && role.isAdmin) {
                "Valid User UID, email, and staff role are required."
            }
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "email" to email.trim().take(200),
                        "displayName" to displayName.trim().take(80),
                        "role" to role.rawKey,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                val entry = AdminUser(
                    userId = cleanUid,
                    email = email.trim().take(200),
                    displayName = displayName.trim().take(80),
                    role = role.rawKey,
                    active = active,
                )
                docRef.set(entry.toCreateMap()).await()
            }
            recordAudit(actorRole, "SAVE_ADMIN_ROLE", "adminUser", cleanUid, "Set admin ${email.trim()} to ${role.label} (active=$active)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveCoupon(actorRole: AdminRole, couponId: String?, coupon: Coupon): Result<String> {
        val docRef = if (couponId.isNullOrBlank()) db.collection("coupons").document() else db.collection("coupons").document(couponId)
        return try {
            requireActor(actorRole)
            check(actorRole.canManagePromos) { "Your role lacks promo code management permission." }
            val cleanCode = coupon.code.trim().uppercase().take(30)
            require(cleanCode.length >= 2) { "Promo code must be at least 2 characters." }
            val exists = docRef.get().await().exists()
            if (exists) {
                val updates = coupon.copy(code = cleanCode).toCreateMap().toMutableMap()
                updates.remove("createdAt")
                docRef.update(updates).await()
                recordAudit(actorRole, "UPDATE_COUPON", "coupon", docRef.id, "Updated promo code $cleanCode")
            } else {
                docRef.set(coupon.copy(code = cleanCode).toCreateMap()).await()
                recordAudit(actorRole, "CREATE_COUPON", "coupon", docRef.id, "Created promo code $cleanCode (${coupon.discountType})")
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCoupon(actorRole: AdminRole, coupon: Coupon): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManagePromos) { "Your role lacks promo code permission." }
            db.collection("coupons").document(coupon.id).delete().await()
            recordAudit(actorRole, "DELETE_COUPON", "coupon", coupon.id, "Deleted promo code ${coupon.code}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "coupons/${coupon.id}")
            Result.failure(e)
        }
    }

    suspend fun saveDeliveryZone(actorRole: AdminRole, zoneId: String?, zone: DeliveryZone): Result<String> {
        val docRef = if (zoneId.isNullOrBlank()) db.collection("deliveryZones").document() else db.collection("deliveryZones").document(zoneId)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageDelivery) { "Your role lacks delivery management permission." }
            require(zone.name.trim().length >= 2) { "Zone name required." }
            docRef.set(zone.toCreateMap()).await()
            recordAudit(actorRole, "SAVE_DELIVERY_ZONE", "deliveryZone", docRef.id, "Saved delivery zone '${zone.name}' (₦${zone.feeNgn.toLong()})")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDeliveryZone(actorRole: AdminRole, zone: DeliveryZone): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManageDelivery) { "Your role lacks delivery management permission." }
            db.collection("deliveryZones").document(zone.id).delete().await()
            recordAudit(actorRole, "DELETE_DELIVERY_ZONE", "deliveryZone", zone.id, "Removed delivery zone '${zone.name}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "deliveryZones/${zone.id}")
            Result.failure(e)
        }
    }

    suspend fun saveDeliverySettings(actorRole: AdminRole, settings: DeliverySettings): Result<Unit> {
        val docRef = db.collection("deliverySettings").document("public")
        return try {
            requireActor(actorRole)
            check(actorRole.canManageDelivery) { "Your role lacks delivery management permission." }
            docRef.set(settings.copy(visibility = "public").toWriteMap()).await()
            recordAudit(actorRole, "UPDATE_DELIVERY_SETTINGS", "deliverySettings", "public", "Updated global delivery/pickup rules")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveHomepageSection(
        context: Context,
        actorRole: AdminRole,
        sectionId: String,
        content: HomepageContent,
        localImageUri: Uri? = null,
        onUploadProgress: ((Float) -> Unit)? = null,
    ): Result<Unit> {
        val safeId = sectionId.trim().ifBlank { "hero_main" }
        val docRef = db.collection("homepageContent").document(safeId)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageContent) { "Your role lacks homepage content permission." }
            val finalImage = if (localImageUri != null) {
                AdminStorageHelper.uploadImageUri(
                    context = context,
                    uri = localImageUri,
                    folderPath = "banners/$safeId",
                    fixedFileName = "banner_${System.currentTimeMillis()}.jpg",
                    onProgress = onUploadProgress,
                ).getOrThrow()
            } else {
                content.imageKey
            }
            docRef.set(content.copy(imageKey = finalImage).toCreateMap()).await()
            recordAudit(actorRole, "UPDATE_BANNER", "homepageContent", safeId, "Updated banner '${content.title}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveCollection(actorRole: AdminRole, collectionId: String?, col: StoreCollection): Result<String> {
        val cleanSlug = SeoUtils.sanitizeSlug(col.slug.ifBlank { col.name }, "collection").take(80)
        val docRef = if (collectionId.isNullOrBlank()) db.collection("collections").document("col_${cleanSlug.replace("-", "_")}".take(60)) else db.collection("collections").document(collectionId)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageContent) { "Your role lacks collection management permission." }
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "name" to col.name.trim().take(80),
                        "slug" to cleanSlug,
                        "tagline" to col.tagline.trim().take(200),
                        "bannerKey" to col.bannerKey.trim().take(600),
                        "seoTitle" to col.seoTitle.trim().take(120),
                        "seoDescription" to col.seoDescription.trim().take(300),
                        "socialImageUrl" to col.socialImageUrl.trim().take(600),
                        "sortOrder" to col.sortOrder.coerceIn(0L, 1000L),
                        "active" to col.active,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(col.copy(slug = cleanSlug).toCreateMap()).await()
            }
            recordAudit(actorRole, "SAVE_COLLECTION", "collection", docRef.id, "Saved collection '${col.name}'")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveTestimonial(actorRole: AdminRole, testimonialId: String?, item: Testimonial): Result<String> {
        val docRef = if (testimonialId.isNullOrBlank()) db.collection("testimonials").document() else db.collection("testimonials").document(testimonialId)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageContent) { "Your role lacks testimonial management permission." }
            val exists = docRef.get().await().exists()
            if (exists) {
                val updates = item.toCreateMap().toMutableMap().apply { remove("createdAt") }
                docRef.update(updates).await()
            } else {
                docRef.set(item.toCreateMap()).await()
            }
            recordAudit(actorRole, "SAVE_TESTIMONIAL", "testimonial", docRef.id, "Saved testimonial by '${item.customerName}'")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTestimonial(actorRole: AdminRole, id: String): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManageContent) { "Your role lacks testimonial management permission." }
            db.collection("testimonials").document(id).delete().await()
            recordAudit(actorRole, "DELETE_TESTIMONIAL", "testimonial", id, "Removed testimonial $id")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "testimonials/$id")
            Result.failure(e)
        }
    }

    suspend fun moderateReview(
        actorRole: AdminRole,
        reviewId: String,
        visible: Boolean,
        status: String,
        flaggedReason: String = "",
    ): Result<Unit> {
        val docRef = db.collection("reviews").document(reviewId)
        return try {
            requireActor(actorRole)
            check(actorRole.canModerateReviews) { "Your role lacks review moderation permission." }
            docRef.update(
                mapOf(
                    "visible" to visible,
                    "moderationStatus" to status,
                    "flaggedReason" to flaggedReason.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            recordAudit(actorRole, "MODERATE_REVIEW", "review", reviewId, "Set review $reviewId -> $status (visible=$visible)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(actorRole: AdminRole, reviewId: String): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canModerateReviews) { "Your role lacks review moderation permission." }
            db.collection("reviews").document(reviewId).delete().await()
            recordAudit(actorRole, "DELETE_REVIEW", "review", reviewId, "Deleted customer review $reviewId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "reviews/$reviewId")
            Result.failure(e)
        }
    }

    suspend fun saveStoreSettings(
        context: Context,
        actorRole: AdminRole,
        settings: StoreSettings,
        newLogoUri: Uri? = null,
        onUploadProgress: ((Float) -> Unit)? = null,
    ): Result<Unit> {
        val docRef = db.collection("storeSettings").document("public")
        return try {
            requireActor(actorRole)
            check(actorRole.canManageSettings) { "Your role lacks store settings permission." }
            val resolvedLogoUrl = if (newLogoUri != null) {
                AdminStorageHelper.uploadImageUri(
                    context = context,
                    uri = newLogoUri,
                    folderPath = "store",
                    fixedFileName = "official_logo_${System.currentTimeMillis()}.jpg",
                    maxDimensionPx = 1024,
                    onProgress = onUploadProgress,
                ).getOrThrow()
            } else {
                settings.logoUrl
            }
            docRef.set(settings.copy(logoUrl = resolvedLogoUrl, visibility = "public").toWriteMap()).await()
            recordAudit(actorRole, "UPDATE_STORE_SETTINGS", "storeSettings", "public", "Updated store configuration & policies")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Admin Support Inbox Operations ---

    suspend fun updateSupportEnquiryMeta(
        actorRole: AdminRole,
        enquiry: SupportEnquiry,
        newStatus: SupportStatus,
        assignToMe: Boolean = false,
        assignedAdmin: AdminUser? = null,
        clearAssignment: Boolean = false,
        priority: String? = null,
    ): Result<Unit> {
        val docRef = db.collection("supportEnquiries").document(enquiry.id)
        return try {
            val (uid, email) = requireActor(actorRole)
            check(actorRole.canManageSupport) { "Your role lacks support inbox permission." }
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (priority != null && priority in listOf("LOW", "NORMAL", "HIGH", "URGENT")) {
                updates["priority"] = priority
            }
            if (assignedAdmin != null) {
                updates["assignedAdminUid"] = assignedAdmin.userId.ifBlank { assignedAdmin.id }
                updates["assignedAdminEmail"] = assignedAdmin.email.take(200)
            } else if (assignToMe) {
                updates["assignedAdminUid"] = uid
                updates["assignedAdminEmail"] = email.take(200)
            } else if (clearAssignment) {
                updates["assignedAdminUid"] = ""
                updates["assignedAdminEmail"] = ""
            }
            docRef.update(updates).await()
            recordAudit(actorRole, "UPDATE_SUPPORT_TICKET", "supportEnquiry", enquiry.id, "Updated enquiry '${enquiry.subject}' -> ${newStatus.label}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun postAdminSupportReply(
        actorRole: AdminRole,
        enquiry: SupportEnquiry,
        replyMessage: String,
        nextStatus: SupportStatus = SupportStatus.WAITING_CUSTOMER,
    ): Result<Unit> {
        val enquiryRef = db.collection("supportEnquiries").document(enquiry.id)
        val replyRef = enquiryRef.collection("replies").document()
        return try {
            val (uid, email) = requireActor(actorRole)
            check(actorRole.canManageSupport) { "Your role lacks support reply permission." }
            val cleanMsg = replyMessage.trim().take(2000)
            require(cleanMsg.isNotEmpty()) { "Reply message cannot be empty." }

            val reply = SupportReply(
                enquiryId = enquiry.id,
                ownerUid = enquiry.userId,
                senderUid = uid,
                senderName = "ZEECROWN Support",
                senderRole = "ADMIN",
                message = cleanMsg,
            )
            replyRef.set(reply.toCreateMap()).await()
            enquiryRef.update(
                mapOf(
                    "status" to nextStatus.name,
                    "assignedAdminUid" to enquiry.assignedAdminUid.ifBlank { uid },
                    "assignedAdminEmail" to enquiry.assignedAdminEmail.ifBlank { email.take(200) },
                    "lastReplyAt" to FieldValue.serverTimestamp(),
                    "lastReplyPreview" to cleanMsg.take(280),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            val commsRepo = CommunicationRepository(db, auth)
            commsRepo.createIdempotentCustomerNotification(
                notificationId = "notif_supreply_${enquiry.id}_${replyRef.id}",
                notification = AppNotification(
                    userId = enquiry.userId,
                    title = "Support Reply: ${enquiry.subject.take(60)}",
                    body = cleanMsg.take(350),
                    category = "SUPPORT",
                    readFlag = false,
                    targetRoute = "support",
                    orderId = enquiry.orderId,
                    orderNumber = enquiry.orderNumber,
                    dedupeKey = "sup_reply_${enquiry.id}_${replyRef.id}",
                )
            )
            recordAudit(actorRole, "REPLY_SUPPORT_TICKET", "supportEnquiry", enquiry.id, "Replied to customer enquiry '${enquiry.subject}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, replyRef.path)
            Result.failure(e)
        }
    }

    suspend fun addAdminSupportInternalNote(
        actorRole: AdminRole,
        enquiry: SupportEnquiry,
        noteText: String,
    ): Result<Unit> {
        val noteRef = db.collection("supportEnquiries").document(enquiry.id).collection("internalNotes").document()
        return try {
            val (uid, email) = requireActor(actorRole)
            check(actorRole.canManageSupport) { "Your role lacks internal note permission." }
            val cleanNote = noteText.trim().take(2000)
            require(cleanNote.isNotEmpty()) { "Internal note cannot be empty." }
            val note = SupportInternalNote(
                enquiryId = enquiry.id,
                adminUid = uid,
                adminEmail = email.take(200),
                note = cleanNote,
            )
            noteRef.set(note.toCreateMap()).await()
            recordAudit(actorRole, "ADD_SUPPORT_INTERNAL_NOTE", "supportEnquiry", enquiry.id, "Added private staff note on enquiry ${enquiry.id}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, noteRef.path)
            Result.failure(e)
        }
    }

    // --- Admin Communication Center Operations ---

    suspend fun sendManualOrderUpdateNotification(
        actorRole: AdminRole,
        order: Order,
        title: String,
        message: String,
    ): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManageCommunications) { "Your role lacks communication management permission." }
            val cleanTitle = title.trim().take(120)
            val cleanBody = message.trim().take(400)
            require(cleanTitle.length >= 2 && cleanBody.length >= 5) {
                "Enter a notification title and message."
            }

            val tsKey = System.currentTimeMillis() / 1000L
            val commsRepo = CommunicationRepository(db, auth)
            val notifId = "notif_manual_${order.id}_$tsKey"
            commsRepo.createIdempotentCustomerNotification(
                notificationId = notifId,
                notification = AppNotification(
                    userId = order.userId,
                    title = cleanTitle,
                    body = cleanBody,
                    category = "ORDER",
                    readFlag = false,
                    targetRoute = "track_order/${order.id}",
                    orderId = order.id,
                    orderNumber = order.orderNumber,
                    dedupeKey = "manual_${order.id}_$tsKey",
                ),
                alsoLogInApp = true,
            )
            recordAudit(actorRole, "SEND_MANUAL_ORDER_UPDATE", "order", order.id, "Sent update notification on ${order.orderNumber}: $cleanTitle")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notifications")
            Result.failure(e)
        }
    }

    suspend fun retryCommunicationLogAdmin(
        actorRole: AdminRole,
        log: CommunicationLog,
        storeSettings: StoreSettings,
    ): Result<String> {
        val docRef = db.collection("communicationLogs").document(log.id)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageCommunications) { "Your role lacks communication retry permission." }
            val nextCount = (log.retryCount + 1L).coerceAtMost(20L)

            if (log.channel == CommunicationChannel.IN_APP.name) {
                val notifId = "notif_retrylog_${log.id}"
                db.collection("notifications").document(notifId).set(
                    AppNotification(
                        userId = log.userId,
                        title = log.subject.ifBlank { "Order Update" },
                        body = log.bodyPreview.ifBlank { "Open Track Order for the latest update." },
                        category = "ORDER",
                        readFlag = false,
                        targetRoute = if (log.orderId.isNotBlank()) "track_order/${log.orderId}" else "notifications",
                        orderId = log.orderId,
                        orderNumber = log.orderNumber,
                        dedupeKey = "retry_${log.id}",
                    ).toCreateMap()
                ).await()
                docRef.update(
                    mapOf(
                        "status" to CommunicationStatus.SENT.name,
                        "failureReason" to "",
                        "retryCount" to nextCount,
                        "lastAttemptAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                recordAudit(actorRole, "RETRY_COMMUNICATION", "communicationLog", log.id, "Retried IN_APP log ${log.id} -> SENT")
                return Result.success("In-App notification re-delivered successfully.")
            }

            // Respect customer consent & store channel toggles before external retry
            val prefSnap = db.collection("users").document(log.userId)
                .collection("preferences").document("notifications").get().await()
            val prefs = prefSnap.toObject(NotificationPreferences::class.java)
                ?: NotificationPreferences(userId = log.userId)

            val consentBlocked = when (log.channel) {
                CommunicationChannel.EMAIL.name -> !storeSettings.emailNotificationsEnabled || !prefs.orderUpdatesEmail
                CommunicationChannel.SMS.name -> !storeSettings.smsNotificationsEnabled || !prefs.smsOrderAlerts
                CommunicationChannel.WHATSAPP.name -> !storeSettings.whatsappNotificationsEnabled || !prefs.whatsappOrderAlerts
                else -> false
            }

            if (consentBlocked) {
                docRef.update(
                    mapOf(
                        "status" to CommunicationStatus.SKIPPED_NO_CONSENT.name,
                        "failureReason" to "Blocked by store channel toggle or customer opt-out preference.",
                        "retryCount" to nextCount,
                        "lastAttemptAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                return Result.success("Retry skipped: channel disabled or customer opted out.")
            }

            // External channels (EMAIL/SMS/WHATSAPP/PUSH) execute server-side in Cloud Functions; never fake SENT client-side.
            val reason = "Marked for server-side Cloud Functions retry (${log.channel}). Deploy /functions with provider secrets to complete live dispatch."
            docRef.update(
                mapOf(
                    "status" to CommunicationStatus.PROVIDER_NOT_CONFIGURED.name,
                    "failureReason" to reason,
                    "retryCount" to nextCount,
                    "lastAttemptAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            recordAudit(actorRole, "RETRY_COMMUNICATION", "communicationLog", log.id, "Queued server-side retry check for ${log.channel} log ${log.id}")
            Result.success(reason)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Admin AI Assistant & Knowledge Base Operations ---

    suspend fun saveAiAssistantConfig(
        actorRole: AdminRole,
        config: AiAssistantConfig,
    ): Result<Unit> {
        val docRef = db.collection("aiConfig").document("public")
        return try {
            requireActor(actorRole)
            check(actorRole.canManageAi) { "Your role lacks AI Assistant configuration permission." }
            docRef.set(config.toWriteMap()).await()
            recordAudit(
                actorRole,
                "SAVE_AI_CONFIG",
                "aiConfig",
                "public",
                "Updated AI Assistant settings (enabled=${config.enabled}, humanOnline=${config.humanSupportAvailable})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveAiFaq(
        actorRole: AdminRole,
        faqId: String?,
        entry: AiFaqEntry,
    ): Result<String> {
        val docRef = if (faqId.isNullOrBlank()) {
            db.collection("aiKnowledgeFaqs").document()
        } else {
            db.collection("aiKnowledgeFaqs").document(faqId)
        }
        return try {
            requireActor(actorRole)
            check(actorRole.canManageAi) { "Your role lacks AI FAQ knowledge permission." }
            require(entry.question.trim().length >= 3 && entry.answer.trim().length >= 3) {
                "Enter both a clear question and verified answer."
            }
            val exists = !faqId.isNullOrBlank() && docRef.get().await().exists()
            if (exists) {
                val updates = entry.toCreateMap(isCreate = false).toMutableMap()
                updates.remove("createdAt")
                docRef.update(updates).await()
            } else {
                docRef.set(entry.toCreateMap(isCreate = true)).await()
            }
            recordAudit(actorRole, "SAVE_AI_FAQ", "aiKnowledgeFaq", docRef.id, "Saved FAQ '${entry.question.take(80)}'")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAiFaq(actorRole: AdminRole, faqId: String): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManageAi) { "Your role lacks AI FAQ deletion permission." }
            db.collection("aiKnowledgeFaqs").document(faqId).delete().await()
            recordAudit(actorRole, "DELETE_AI_FAQ", "aiKnowledgeFaq", faqId, "Deleted AI FAQ $faqId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "aiKnowledgeFaqs/$faqId")
            Result.failure(e)
        }
    }

    suspend fun promoteUnansweredToFaq(
        actorRole: AdminRole,
        item: AiUnansweredQuestion,
        verifiedAnswer: String,
        category: String,
        keywordsText: String,
    ): Result<String> {
        val faqRef = db.collection("aiKnowledgeFaqs").document()
        val uqRef = db.collection("aiUnansweredQuestions").document(item.id)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageAi) { "Your role lacks permission to promote AI answers." }
            require(verifiedAnswer.trim().length >= 3) { "Enter a verified store answer." }
            val kwList = keywordsText.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                .ifEmpty {
                    AiFaqEntry(question = item.question).question
                        .lowercase().split(Regex("\\s+")).filter { it.length >= 4 }.take(8)
                }
            val faq = AiFaqEntry(
                question = item.question,
                answer = verifiedAnswer.trim(),
                category = category.ifBlank { item.category.ifBlank { "General" } },
                keywords = kwList,
                active = true,
                verifiedByAdmin = true,
                sourceQuestionId = item.id,
            )
            faqRef.set(faq.toCreateMap(isCreate = true)).await()
            uqRef.update(
                mapOf(
                    "status" to "RESOLVED_TO_FAQ",
                    "linkedFaqId" to faqRef.id,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            recordAudit(actorRole, "PROMOTE_AI_FAQ", "aiKnowledgeFaq", faqRef.id, "Promoted unanswered query to FAQ")
            Result.success(faqRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, faqRef.path)
            Result.failure(e)
        }
    }

    suspend fun dismissUnansweredQuestion(actorRole: AdminRole, questionId: String): Result<Unit> {
        val ref = db.collection("aiUnansweredQuestions").document(questionId)
        return try {
            requireActor(actorRole)
            check(actorRole.canManageAi) { "Your role lacks AI queue moderation permission." }
            ref.update(
                mapOf(
                    "status" to "DISMISSED",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun saveSizeGuide(
        actorRole: AdminRole,
        guideId: String?,
        guide: SizeGuide,
    ): Result<String> {
        val cleanName = guide.name.trim().take(80)
        val slugBase = SeoUtils.sanitizeSlug(cleanName, "size-guide").replace("-", "_").take(50)
        val docRef = if (guideId.isNullOrBlank()) {
            db.collection("sizeGuides").document("sg_${slugBase}_${System.currentTimeMillis().toString().takeLast(4)}".take(64))
        } else {
            db.collection("sizeGuides").document(guideId.trim())
        }
        return try {
            requireActor(actorRole)
            check(actorRole.canManageProducts || actorRole.canManageCategories) {
                "Your role lacks permission to manage size guides."
            }
            require(cleanName.length >= 2) { "Size guide name must be at least 2 characters." }
            require(guide.columns.count { it.isNotBlank() } >= 2) {
                "Enter at least 2 measurement columns (for example Size and Foot Length (cm))."
            }
            require(guide.rows.any { it.sizeLabel.isNotBlank() }) {
                "Add at least 1 size row to this measurement chart."
            }
            val exists = !guideId.isNullOrBlank() && docRef.get().await().exists()
            if (exists) {
                val updates = guide.toCreateMap(isCreate = false).toMutableMap()
                updates.remove("createdAt")
                docRef.update(updates).await()
                recordAudit(actorRole, "SAVE_SIZE_GUIDE", "sizeGuide", docRef.id, "Updated size guide '$cleanName'")
            } else {
                docRef.set(guide.toCreateMap(isCreate = true)).await()
                recordAudit(actorRole, "SAVE_SIZE_GUIDE", "sizeGuide", docRef.id, "Created size guide '$cleanName'")
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSizeGuide(actorRole: AdminRole, guide: SizeGuide): Result<Unit> {
        return try {
            requireActor(actorRole)
            check(actorRole.canManageProducts || actorRole.canManageCategories) {
                "Your role lacks permission to delete size guides."
            }
            db.collection("sizeGuides").document(guide.id).delete().await()
            recordAudit(actorRole, "DELETE_SIZE_GUIDE", "sizeGuide", guide.id, "Deleted size guide '${guide.name}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "sizeGuides/${guide.id}")
            Result.failure(e)
        }
    }
}
