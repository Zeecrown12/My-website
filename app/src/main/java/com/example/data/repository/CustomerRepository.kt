package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.R
import com.example.data.auth.AuthValidation
import com.example.data.firebase.AuthManager
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddress
import com.example.data.model.GuestCartMergeResult
import com.example.data.model.NewsletterSubscription
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderTimelineEngine
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.StoreCurrency
import com.example.data.model.TrackingSecurityUtils
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID

class CustomerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in to access your account.")

    suspend fun ensureUserProfileInitialized(
        userId: String,
        displayName: String,
        email: String,
        firstName: String = "",
        lastName: String = "",
        phone: String = "",
        emailVerified: Boolean = false,
        preferredCurrency: StoreCurrency = StoreCurrency.NGN,
    ): Result<Unit> {
        val profileRef = db.collection("users").document(userId)
        val privateRef = profileRef.collection("private").document("info")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val resolvedFirst = firstName.trim().ifBlank {
                displayName.trim().substringBefore(" ", "")
            }.take(40)
            val resolvedLast = lastName.trim().ifBlank {
                val parts = displayName.trim().split(Regex("\\s+"))
                if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            }.take(40)
            val safeName = displayName.trim().ifBlank {
                listOf(resolvedFirst, resolvedLast).filter { it.isNotBlank() }.joinToString(" ")
            }.ifBlank { "Zeecrown Client" }.take(80)

            val existingProfile = profileRef.get().await()
            if (!existingProfile.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = safeName,
                    firstName = resolvedFirst,
                    lastName = resolvedLast,
                    photoUrl = auth.currentUser?.photoUrl?.toString().orEmpty().take(600),
                    preferredCurrency = preferredCurrency.code,
                    role = "customer",
                    emailVerified = emailVerified,
                )
                profileRef.set(profile.toCreateMap()).await()
            } else {
                val updates = mutableMapOf<String, Any>()
                if (existingProfile.getBoolean("emailVerified") != emailVerified) {
                    updates["emailVerified"] = emailVerified
                }
                if (resolvedFirst.isNotBlank() && existingProfile.getString("firstName").isNullOrBlank()) {
                    updates["firstName"] = resolvedFirst
                }
                if (resolvedLast.isNotBlank() && existingProfile.getString("lastName").isNullOrBlank()) {
                    updates["lastName"] = resolvedLast
                }
                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                    profileRef.update(updates).await()
                }
            }

            val existingPrivate = privateRef.get().await()
            if (!existingPrivate.exists() && email.isNotBlank()) {
                val privateInfo = UserPrivateInfo(
                    userId = uid,
                    email = email.trim().take(200),
                    phone = phone.trim().take(30),
                    country = "Nigeria",
                )
                privateRef.set(privateInfo.toWriteMap()).await()
            } else if (existingPrivate.exists()) {
                val privateUpdates = mutableMapOf<String, Any>()
                if (email.isNotBlank() && existingPrivate.getString("email") != email.trim()) {
                    privateUpdates["email"] = email.trim().take(200)
                }
                if (phone.isNotBlank() && existingPrivate.getString("phone").isNullOrBlank()) {
                    privateUpdates["phone"] = phone.trim().take(30)
                }
                if (privateUpdates.isNotEmpty()) {
                    privateUpdates["updatedAt"] = FieldValue.serverTimestamp()
                    privateRef.update(privateUpdates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePersonalDetails(
        userId: String,
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
    ): Result<Unit> {
        val profileRef = db.collection("users").document(userId)
        val privateRef = profileRef.collection("private").document("info")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            AuthValidation.validateName(firstName, "First name")?.let { throw IllegalArgumentException(it) }
            AuthValidation.validateName(lastName, "Last name")?.let { throw IllegalArgumentException(it) }
            val normalizedPhone = if (phone.isNotBlank()) {
                AuthValidation.validatePhone(phone)?.let { throw IllegalArgumentException(it) }
                AuthValidation.normalizePhone(phone)
            } else {
                ""
            }
            val cleanFirst = firstName.trim().take(40)
            val cleanLast = lastName.trim().take(40)
            val fullName = "$cleanFirst $cleanLast".trim().take(80)

            AuthManager.updateDisplayNameAndPhoto(displayName = fullName)

            profileRef.update(
                mapOf(
                    "displayName" to fullName,
                    "firstName" to cleanFirst,
                    "lastName" to cleanLast,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            val existingPrivate = privateRef.get().await()
            val safeEmail = email.trim().ifBlank {
                existingPrivate.getString("email").orEmpty().ifBlank { auth.currentUser?.email.orEmpty() }
            }.take(200)
            if (existingPrivate.exists()) {
                privateRef.update(
                    mapOf(
                        "email" to safeEmail,
                        "phone" to normalizedPhone.take(30),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else if (safeEmail.isNotBlank()) {
                val info = UserPrivateInfo(
                    userId = uid,
                    email = safeEmail,
                    phone = normalizedPhone.take(30),
                    country = "Nigeria",
                )
                privateRef.set(info.toWriteMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncEmailVerificationAndEmail(
        userId: String,
        email: String,
        emailVerified: Boolean,
    ): Result<Unit> {
        val profileRef = db.collection("users").document(userId)
        val privateRef = profileRef.collection("private").document("info")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            profileRef.update(
                mapOf(
                    "emailVerified" to emailVerified,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            if (email.isNotBlank() && privateRef.get().await().exists()) {
                privateRef.update(
                    mapOf(
                        "email" to email.trim().take(200),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(
        context: Context,
        userId: String,
        imageUri: Uri,
    ): Result<String> {
        val profileRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val jpegBytes = withContext(Dispatchers.IO) {
                decodeAndCompressAvatar(context, imageUri)
            }
            val storageRef = Firebase.storage.reference.child("users/$uid/profile/avatar.jpg")
            storageRef.putBytes(jpegBytes).await()
            val downloadUrl = storageRef.downloadUrl.await().toString().take(600)

            AuthManager.updateDisplayNameAndPhoto(photoUrl = downloadUrl)
            profileRef.update(
                mapOf(
                    "photoUrl" to downloadUrl,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            Result.failure(
                IllegalStateException(
                    "Firebase Storage is not enabled or storage rules are not deployed yet (${e.errorCode}). Enable Storage in Firebase Console to upload avatars."
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Could not upload profile photo."))
        }
    }

    suspend fun removeProfilePhoto(userId: String): Result<Unit> {
        val profileRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            runCatching {
                Firebase.storage.reference.child("users/$uid/profile/avatar.jpg").delete().await()
            }
            AuthManager.updateDisplayNameAndPhoto(photoUrl = "")
            profileRef.update(
                mapOf(
                    "photoUrl" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            Result.failure(e)
        }
    }

    private fun decodeAndCompressAvatar(context: Context, uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open selected image.")
        val original = input.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Unsupported image format. Please choose a JPEG or PNG image.")
        val maxDim = 640
        val scale = minOf(
            maxDim.toFloat() / original.width.coerceAtLeast(1),
            maxDim.toFloat() / original.height.coerceAtLeast(1),
            1f,
        )
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            original
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        if (scaled != original) scaled.recycle()
        original.recycle()
        val bytes = out.toByteArray()
        require(bytes.size <= 4 * 1024 * 1024) { "Compressed photo exceeds 4MB limit." }
        return bytes
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeUserPrivateInfo(userId: String): Flow<UserPrivateInfo?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val docRef = db.collection("users").document(userId).collection("private").document("info")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserPrivateInfo::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun updatePreferredCurrency(userId: String, currency: StoreCurrency): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            docRef.update(
                mapOf(
                    "preferredCurrency" to currency.code,
                    "updatedAt" to FieldValue.serverTimestamp(),
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

    suspend fun saveShippingInfo(
        userId: String,
        displayName: String,
        email: String,
        phone: String,
        shippingAddress: String,
        city: String,
        state: String,
        country: String,
    ): Result<Unit> {
        val profileRef = db.collection("users").document(userId)
        val privateRef = profileRef.collection("private").document("info")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanName = displayName.trim().take(80)
            require(cleanName.isNotEmpty()) { "Display name is required." }
            require(email.trim().length >= 3) { "Email is required." }

            profileRef.update(
                mapOf(
                    "displayName" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            val info = UserPrivateInfo(
                userId = uid,
                email = email.trim().take(200),
                phone = phone.trim().take(30),
                shippingAddress = shippingAddress.trim().take(250),
                city = city.trim().take(80),
                state = state.trim().take(80),
                country = country.trim().ifBlank { "Nigeria" }.take(80),
            )
            privateRef.set(info.toWriteMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privateRef.path)
            Result.failure(e)
        }
    }

    fun observeDeliveryAddresses(userId: String): Flow<List<DeliveryAddress>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/addresses"
        emitAll(
            db.collection("users").document(userId).collection("addresses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DeliveryAddress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<DeliveryAddress> { it.defaultAddress }
                            .thenByDescending { it.updatedAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveDeliveryAddress(
        userId: String,
        addressId: String?,
        draft: DeliveryAddress,
    ): Result<String> {
        val colRef = db.collection("users").document(userId).collection("addresses")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val normalizedPhone = AuthValidation.normalizePhone(draft.phone)
            AuthValidation.validateDeliveryAddress(
                label = draft.label,
                recipientName = draft.recipientName,
                phone = normalizedPhone,
                country = draft.country,
                state = draft.state,
                city = draft.city,
                streetAddress = draft.streetAddress,
            )?.let { throw IllegalArgumentException(it) }

            val existingDocs = colRef.get().await().documents
            val shouldBeDefault = draft.defaultAddress || existingDocs.isEmpty()
            val targetRef = if (addressId.isNullOrBlank()) colRef.document() else colRef.document(addressId)

            val batch = db.batch()
            if (shouldBeDefault) {
                existingDocs.forEach { doc ->
                    if (doc.id != targetRef.id && doc.getBoolean("defaultAddress") == true) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "defaultAddress" to false,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    }
                }
            }

            val cleanAddress = draft.copy(
                userId = uid,
                phone = normalizedPhone,
                defaultAddress = shouldBeDefault,
            )
            if (addressId.isNullOrBlank() || existingDocs.none { it.id == targetRef.id }) {
                batch.set(targetRef, cleanAddress.toCreateMap())
            } else {
                batch.update(targetRef, cleanAddress.toUpdateMap())
            }

            if (shouldBeDefault) {
                val privateRef = db.collection("users").document(uid).collection("private").document("info")
                val privateSnap = privateRef.get().await()
                if (privateSnap.exists()) {
                    batch.update(
                        privateRef,
                        mapOf(
                            "phone" to normalizedPhone.take(30),
                            "shippingAddress" to cleanAddress.streetAddress.trim().take(250),
                            "city" to cleanAddress.city.trim().take(80),
                            "state" to cleanAddress.state.trim().take(80),
                            "country" to cleanAddress.country.trim().ifBlank { "Nigeria" }.take(80),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(targetRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun setDefaultDeliveryAddress(userId: String, addressId: String): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("addresses")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docs = colRef.get().await().documents
            val targetDoc = docs.firstOrNull { it.id == addressId }
                ?: return Result.failure(IllegalArgumentException("Address not found."))

            val batch = db.batch()
            docs.forEach { doc ->
                val targetFlag = doc.id == addressId
                if (doc.getBoolean("defaultAddress") != targetFlag) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "defaultAddress" to targetFlag,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }
            val privateRef = db.collection("users").document(uid).collection("private").document("info")
            if (privateRef.get().await().exists()) {
                batch.update(
                    privateRef,
                    mapOf(
                        "phone" to (targetDoc.getString("phone") ?: "").take(30),
                        "shippingAddress" to (targetDoc.getString("streetAddress") ?: "").take(250),
                        "city" to (targetDoc.getString("city") ?: "").take(80),
                        "state" to (targetDoc.getString("state") ?: "").take(80),
                        "country" to (targetDoc.getString("country") ?: "Nigeria").take(80),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDeliveryAddress(userId: String, addressId: String): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("addresses")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val targetRef = colRef.document(addressId)
            val snap = targetRef.get().await()
            val wasDefault = snap.getBoolean("defaultAddress") == true
            targetRef.delete().await()
            if (wasDefault) {
                val nextDoc = colRef.get().await().documents.firstOrNull()
                if (nextDoc != null) {
                    nextDoc.reference.update(
                        mapOf(
                            "defaultAddress" to true,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun mergeGuestData(
        userId: String,
        guestCart: List<CartItem>,
        guestWishlist: List<WishlistItem>,
    ): Result<GuestCartMergeResult> {
        if (guestCart.isEmpty() && guestWishlist.isEmpty()) {
            return Result.success(GuestCartMergeResult())
        }
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            var addedCart = 0
            var mergedCart = 0
            var addedWish = 0
            val warnings = mutableListOf<String>()

            // Server-authoritative cart merge: verify live product price, active state & stock
            for (guestItem in guestCart) {
                val productSnap = db.collection("products").document(guestItem.productId).get().await()
                val liveProduct = productSnap.toObject(Product::class.java)
                if (liveProduct == null || !liveProduct.active) {
                    warnings += "${guestItem.productTitle.ifBlank { "Item" }} is no longer available and was skipped."
                    continue
                }
                val cleanVariant = guestItem.variantLabel.trim().ifBlank { "Standard" }.take(80)
                if (liveProduct.unavailableVariants.any { it.equals(cleanVariant, ignoreCase = true) }) {
                    warnings += "${liveProduct.title} ($cleanVariant) is currently unavailable."
                    continue
                }
                val invSnap = db.collection("inventory").document(liveProduct.id).get().await()
                val liveStock = if (invSnap.exists()) invSnap.getLong("stockQuantity") ?: 0L else 10L
                if (liveStock <= 0L) {
                    warnings += "${liveProduct.title} is currently out of stock."
                    continue
                }

                val docId = "${liveProduct.id}_${cleanVariant.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}".take(120)
                val cartDocRef = db.collection("carts").document(uid).collection("items").document(docId)
                val existingSnap = cartDocRef.get().await()
                if (existingSnap.exists()) {
                    val prevQty = existingSnap.getLong("quantity") ?: 1L
                    val cappedQty = (prevQty + guestItem.quantity).coerceIn(1L, minOf(50L, liveStock))
                    cartDocRef.update(
                        mapOf(
                            "variantLabel" to cleanVariant,
                            "quantity" to cappedQty,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                    mergedCart++
                } else {
                    val cappedQty = guestItem.quantity.coerceIn(1L, minOf(50L, liveStock))
                    val serverItem = CartItem(
                        userId = uid,
                        productId = liveProduct.id,
                        productTitle = liveProduct.title.take(120),
                        productImageUrl = liveProduct.mainImageUrl
                            .ifBlank { liveProduct.imageUrls.firstOrNull().orEmpty() }
                            .take(600),
                        variantLabel = cleanVariant,
                        unitPriceNgn = liveProduct.priceNgn,
                        quantity = cappedQty,
                    )
                    cartDocRef.set(serverItem.toCreateMap()).await()
                    addedCart++
                }
            }

            // Duplicate-free wishlist merge using live product metadata
            for (guestWish in guestWishlist) {
                val productSnap = db.collection("products").document(guestWish.productId).get().await()
                val liveProduct = productSnap.toObject(Product::class.java)
                if (liveProduct == null || !liveProduct.active) continue
                val wishRef = db.collection("wishlists").document(uid).collection("items").document(liveProduct.id)
                if (!wishRef.get().await().exists()) {
                    val serverWish = WishlistItem(
                        userId = uid,
                        productId = liveProduct.id,
                        productTitle = liveProduct.title.take(120),
                        productImageUrl = liveProduct.mainImageUrl
                            .ifBlank { liveProduct.imageUrls.firstOrNull().orEmpty() }
                            .take(600),
                        priceNgn = liveProduct.priceNgn,
                    )
                    wishRef.set(serverWish.toCreateMap()).await()
                    addedWish++
                }
            }

            Result.success(
                GuestCartMergeResult(
                    addedCartItems = addedCart,
                    mergedCartItems = mergedCart,
                    addedWishlistItems = addedWish,
                    warnings = warnings,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "carts/$userId/items")
            Result.failure(e)
        }
    }

    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "carts/$userId/items"
        emitAll(
            db.collection("carts").document(userId).collection("items")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CartItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addToCart(
        userId: String,
        product: Product,
        variantLabel: String,
        quantity: Long = 1L,
        availableStock: Long? = null,
    ): Result<Unit> {
        val cleanVariant = variantLabel.trim().ifBlank { "Standard" }.take(80)
        val docId = "${product.id}_${cleanVariant.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}".take(120)
        val docRef = db.collection("carts").document(userId).collection("items").document(docId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(product.id.isNotBlank()) { "Product ID required." }
            require(product.active) { "This product is currently unavailable." }
            require(quantity in 1L..50L) { "Quantity must be between 1 and 50." }

            if (product.unavailableVariants.any { it.equals(cleanVariant, ignoreCase = true) }) {
                return Result.failure(IllegalStateException("Selected variation ($cleanVariant) is out of stock."))
            }

            val invSnap = db.collection("inventory").document(product.id).get().await()
            val liveStock = if (invSnap.exists()) {
                invSnap.getLong("stockQuantity") ?: 0L
            } else {
                availableStock ?: 10L
            }
            if (liveStock <= 0L) {
                return Result.failure(IllegalStateException("${product.title} is currently out of stock."))
            }

            val existing = docRef.get().await()
            if (existing.exists()) {
                val prevQty = existing.getLong("quantity") ?: 1L
                val requestedTotal = prevQty + quantity
                if (requestedTotal > liveStock) {
                    return Result.failure(
                        IllegalStateException("Only $liveStock piece(s) of ${product.title} available in stock.")
                    )
                }
                val newQty = requestedTotal.coerceAtMost(50L)
                docRef.update(
                    mapOf(
                        "variantLabel" to cleanVariant,
                        "quantity" to newQty,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                if (quantity > liveStock) {
                    return Result.failure(
                        IllegalStateException("Only $liveStock piece(s) of ${product.title} available in stock.")
                    )
                }
                val item = CartItem(
                    userId = uid,
                    productId = product.id,
                    productTitle = product.title.take(120),
                    productImageUrl = product.mainImageUrl.ifBlank { product.imageUrls.firstOrNull().orEmpty() }.take(600),
                    variantLabel = cleanVariant,
                    unitPriceNgn = product.priceNgn,
                    quantity = quantity,
                )
                docRef.set(item.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(
        userId: String,
        itemId: String,
        quantity: Long,
        availableStock: Long? = null,
    ): Result<Unit> {
        val docRef = db.collection("carts").document(userId).collection("items").document(itemId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            if (quantity <= 0L) {
                docRef.delete().await()
            } else {
                val existingSnap = docRef.get().await()
                val productId = existingSnap.getString("productId").orEmpty()
                val liveStock = if (productId.isNotBlank()) {
                    val invSnap = db.collection("inventory").document(productId).get().await()
                    if (invSnap.exists()) invSnap.getLong("stockQuantity") ?: 0L else (availableStock ?: 50L)
                } else {
                    availableStock ?: 50L
                }
                if (quantity > liveStock) {
                    return Result.failure(IllegalStateException("Only $liveStock available in stock."))
                }
                docRef.update(
                    mapOf(
                        "quantity" to quantity.coerceAtMost(50L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeCartItem(userId: String, itemId: String): Result<Unit> {
        val docRef = db.collection("carts").document(userId).collection("items").document(itemId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "wishlists/$userId/items"
        emitAll(
            db.collection("wishlists").document(userId).collection("items")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WishlistItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun toggleWishlist(userId: String, product: Product): Result<Boolean> {
        val docRef = db.collection("wishlists").document(userId).collection("items").document(product.id)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val item = WishlistItem(
                    userId = uid,
                    productId = product.id,
                    productTitle = product.title.take(120),
                    productImageUrl = product.mainImageUrl.ifBlank { product.imageUrls.firstOrNull().orEmpty() }.take(600),
                    priceNgn = product.priceNgn,
                )
                docRef.set(item.toCreateMap()).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeOrders(userId: String): Flow<List<Order>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "orders")
                    }
                    throw error
                }
        )
    }

    suspend fun placeOrder(
        userId: String,
        cartItems: List<CartItem>,
        deliveryZoneId: String,
        deliveryFeeNgn: Double,
        shippingCity: String,
        shippingState: String,
        currency: StoreCurrency,
        paymentMethod: PaymentMethod,
        transferNote: String = "",
        selectedAddress: DeliveryAddress? = null,
    ): Result<String> {
        val orderRef = db.collection("orders").document()
        val paymentRef = db.collection("payments").document()
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(cartItems.isNotEmpty()) { "Your shopping bag is empty." }
            require(deliveryZoneId.isNotBlank()) { "Please select a delivery zone." }
            val resolvedCity = selectedAddress?.city?.takeIf { it.isNotBlank() } ?: shippingCity
            val resolvedState = selectedAddress?.state?.takeIf { it.isNotBlank() } ?: shippingState
            require(resolvedCity.isNotBlank() && resolvedState.isNotBlank()) { "Shipping city and state are required." }

            val subtotal = cartItems.sumOf { it.unitPriceNgn * it.quantity }
            val itemCount = cartItems.sumOf { it.quantity }.coerceIn(1L, 200L)
            val summary = cartItems.joinToString(", ") { "${it.quantity}x ${it.productTitle} (${it.variantLabel})" }.take(1000)
            val total = subtotal + deliveryFeeNgn
            val shortCode = UUID.randomUUID().toString().take(6).uppercase()
            val orderNumber = "ZC-$shortCode"

            val customerEmail = auth.currentUser?.email.orEmpty().trim().lowercase().take(200)
            val trackingToken = TrackingSecurityUtils.generateTrackingToken()
            val lookupEmailHash = TrackingSecurityUtils.buildLookupId(orderNumber, customerEmail)
            val lookupPhoneHash = TrackingSecurityUtils.buildLookupId(orderNumber, selectedAddress?.phone.orEmpty())
            val initialHistory = listOf(
                OrderTimelineEngine.historyEntry(
                    stage = OrderStatus.PENDING_PAYMENT.name,
                    label = "Order Placed",
                    note = "Order $orderNumber created (${paymentMethod.label}).",
                    actorRole = "CUSTOMER",
                )
            )

            val order = Order(
                userId = uid,
                orderNumber = orderNumber,
                itemsSummary = summary,
                itemCount = itemCount,
                subtotalNgn = subtotal,
                deliveryFeeNgn = deliveryFeeNgn,
                discountNgn = 0.0,
                totalNgn = total,
                currency = currency.code,
                deliveryZoneId = deliveryZoneId,
                recipientName = selectedAddress?.recipientName.orEmpty().trim().take(80),
                recipientPhone = selectedAddress?.phone.orEmpty().trim().take(30),
                shippingAddress = selectedAddress?.streetAddress.orEmpty().trim().take(250),
                shippingCity = resolvedCity.trim().take(80),
                shippingState = resolvedState.trim().take(80),
                shippingCountry = selectedAddress?.country.orEmpty().trim().take(80),
                lga = selectedAddress?.lga.orEmpty().trim().take(80),
                landmark = selectedAddress?.landmark.orEmpty().trim().take(150),
                deliveryInstructions = selectedAddress?.deliveryInstructions.orEmpty().trim().take(250),
                status = OrderStatus.PENDING_PAYMENT.name,
                paymentMethod = paymentMethod.name,
                paymentStatus = PaymentStatus.UNPAID.name,
                customerEmail = customerEmail,
                trackingToken = trackingToken,
                guestLookupHashEmail = lookupEmailHash,
                guestLookupHashPhone = lookupPhoneHash,
                deliveryStatus = "Order Placed",
                statusHistory = initialHistory,
            )
            orderRef.set(order.toCreateMap()).await()

            val payment = PaymentRecord(
                userId = uid,
                orderId = orderRef.id,
                orderNumber = orderNumber,
                customerName = order.recipientName,
                provider = paymentMethod.name,
                reference = "PAY-$shortCode-${System.currentTimeMillis() % 10000}",
                amountNgn = total,
                currency = currency.code,
                status = if (paymentMethod == PaymentMethod.BANK_TRANSFER && transferNote.isNotBlank()) {
                    PaymentRecordStatus.SUBMITTED.name
                } else {
                    PaymentRecordStatus.INITIATED.name
                },
                transferNote = transferNote.trim().take(300),
            )
            paymentRef.set(payment.toCreateMap()).await()

            var persistedOrder = order.copy(id = orderRef.id)
            if (paymentMethod == PaymentMethod.BANK_TRANSFER && transferNote.isNotBlank()) {
                val submittedHistory = initialHistory + OrderTimelineEngine.historyEntry(
                    stage = OrderStatus.PAYMENT_SUBMITTED.name,
                    label = "Payment Reference Submitted",
                    note = "Bank transfer notice submitted for verification.",
                    actorRole = "CUSTOMER",
                )
                orderRef.update(
                    mapOf(
                        "status" to OrderStatus.PAYMENT_SUBMITTED.name,
                        "paymentStatus" to PaymentStatus.PENDING_VERIFICATION.name,
                        "statusHistory" to submittedHistory,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                persistedOrder = persistedOrder.copy(
                    status = OrderStatus.PAYMENT_SUBMITTED.name,
                    paymentStatus = PaymentStatus.PENDING_VERIFICATION.name,
                    statusHistory = submittedHistory,
                )
            }

            // Clear ordered cart items
            val batch = db.batch()
            cartItems.forEach { item ->
                if (item.id.isNotBlank()) {
                    batch.delete(db.collection("carts").document(uid).collection("items").document(item.id))
                }
            }
            batch.commit().await()

            val commsRepo = CommunicationRepository(db, auth)
            commsRepo.syncOrderTrackingProjections(persistedOrder)
            commsRepo.createIdempotentCustomerNotification(
                notificationId = "notif_placed_${orderRef.id}",
                notification = AppNotification(
                    userId = uid,
                    title = "Order Placed",
                    body = "Your order $orderNumber has been placed and is awaiting payment confirmation.",
                    details = "Tracking Token: $trackingToken",
                    category = "ORDER",
                    readFlag = false,
                    targetRoute = "track_order/${orderRef.id}",
                    orderId = orderRef.id,
                    orderNumber = orderNumber,
                    dedupeKey = "order_placed_${orderRef.id}",
                )
            )

            Result.success(orderNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitPaymentNotice(userId: String, orderId: String): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val existingOrder = docRef.get().await().toObject(Order::class.java)
            val history = (existingOrder?.statusHistory ?: emptyList()) + OrderTimelineEngine.historyEntry(
                stage = OrderStatus.PAYMENT_SUBMITTED.name,
                label = "Transfer Notice Submitted",
                note = "Awaiting verification by store finance.",
                actorRole = "CUSTOMER",
            )
            docRef.update(
                mapOf(
                    "status" to OrderStatus.PAYMENT_SUBMITTED.name,
                    "paymentStatus" to PaymentStatus.PENDING_VERIFICATION.name,
                    "statusHistory" to history.takeLast(30),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            if (existingOrder != null) {
                val commsRepo = CommunicationRepository(db, auth)
                commsRepo.syncOrderTrackingProjections(
                    existingOrder.copy(
                        id = orderId,
                        status = OrderStatus.PAYMENT_SUBMITTED.name,
                        paymentStatus = PaymentStatus.PENDING_VERIFICATION.name,
                        statusHistory = history.takeLast(30),
                    )
                )
                commsRepo.createIdempotentCustomerNotification(
                    notificationId = "notif_paysub_${orderId}",
                    notification = AppNotification(
                        userId = uid,
                        title = "Payment Notice Received",
                        body = "Transfer notice for order ${existingOrder.orderNumber} is pending verification.",
                        category = "PAYMENT",
                        readFlag = false,
                        targetRoute = "track_order/$orderId",
                        orderId = orderId,
                        orderNumber = existingOrder.orderNumber,
                        dedupeKey = "pay_sub_$orderId",
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("notifications")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "notifications")
                    }
                    throw error
                }
        )
    }

    suspend fun submitReview(
        userId: String,
        productId: String,
        authorName: String,
        rating: Long,
        comment: String,
        orderId: String = "",
        requireApproval: Boolean = false,
    ): Result<Unit> {
        val cleanOrderId = orderId.trim()
        val docRef = if (cleanOrderId.isNotBlank()) {
            db.collection("reviews").document("rev_${userId}_${cleanOrderId}_${productId}".take(120))
        } else {
            db.collection("reviews").document()
        }
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(productId.isNotBlank()) { "Product ID required." }
            val cleanComment = comment.trim().take(1000)
            require(cleanComment.isNotEmpty()) { "Please enter your review comment." }
            val cleanAuthor = authorName.trim().ifBlank { "Zeecrown Client" }.take(80)
            val safeRating = rating.coerceIn(1L, 5L)

            if (cleanOrderId.isNotBlank() && docRef.get().await().exists()) {
                return Result.failure(
                    IllegalStateException("You have already submitted a review for this item from this order.")
                )
            }

            val payload = mutableMapOf<String, Any>(
                "productId" to productId,
                "userId" to uid,
                "authorName" to cleanAuthor,
                "rating" to safeRating,
                "comment" to cleanComment,
                "visible" to !requireApproval,
                "moderationStatus" to if (requireApproval) "PENDING" else "APPROVED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (cleanOrderId.isNotBlank()) {
                payload["orderId"] = cleanOrderId.take(128)
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val docRef = db.collection("notifications").document(notificationId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            docRef.update(mapOf("readFlag" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun subscribeNewsletter(userId: String, rawEmail: String): Result<Boolean> {
        val cleanEmail = rawEmail.trim().lowercase().take(200)
        val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        if (!emailRegex.matches(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(cleanEmail.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(28)
        val docId = "sub_$hash"
        val docRef = db.collection("newsletterSubscribers").document(docId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val existing = try {
                docRef.get().await().exists()
            } catch (fe: FirebaseFirestoreException) {
                if (fe.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    // Another signed-in account already subscribed this email address
                    return Result.success(false)
                }
                throw fe
            }
            if (existing) {
                return Result.success(false)
            }

            val payload = NewsletterSubscription(
                userId = uid,
                email = cleanEmail,
                active = true,
            ).toCreateMap()
            docRef.set(payload).await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.ALREADY_EXISTS ||
                e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                Result.success(false)
            } else {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                Result.failure(e)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
