package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class StoreCurrency(val symbol: String, val code: String) {
    NGN("₦", "NGN"),
    USD("$", "USD");

    companion object {
        fun fromRaw(raw: String?): StoreCurrency =
            entries.firstOrNull { it.name == raw } ?: NGN
    }
}

enum class CatalogFilterMode(val label: String) {
    ALL("All Pieces"),
    NEW_ARRIVALS("New Arrivals"),
    BEST_SELLERS("Best Sellers"),
}

enum class CatalogSortOption(val label: String) {
    FEATURED("Featured"),
    NEWEST("Newest Arrivals"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    BEST_SELLING("Best Selling"),
    HIGHEST_RATED("Highest Rated"),
}

data class CatalogFilterState(
    val selectedCategoryId: String? = null,
    val selectedSubcategoryId: String? = null,
    val selectedCollectionId: String? = null,
    val selectedBrand: String? = null,
    val searchQuery: String = "",
    val minPriceNgn: Double? = null,
    val maxPriceNgn: Double? = null,
    val inStockOnly: Boolean = false,
    val discountedOnly: Boolean = false,
    val newArrivalsOnly: Boolean = false,
    val selectedSizes: Set<String> = emptySet(),
    val selectedColors: Set<String> = emptySet(),
    val sortOption: CatalogSortOption = CatalogSortOption.FEATURED,
) {
    fun activeFilterCount(): Int {
        var count = 0
        if (selectedCategoryId != null) count++
        if (selectedSubcategoryId != null) count++
        if (selectedCollectionId != null) count++
        if (selectedBrand != null) count++
        if (minPriceNgn != null || maxPriceNgn != null) count++
        if (inStockOnly) count++
        if (discountedOnly) count++
        if (newArrivalsOnly) count++
        count += selectedSizes.size
        count += selectedColors.size
        return count
    }
}

enum class OrderStatus(val label: String) {
    PENDING_PAYMENT("Awaiting Payment"),
    PAYMENT_SUBMITTED("Payment Verification"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    READY_FOR_DISPATCH("Ready for Dispatch"),
    SHIPPED("Shipped"),
    DISPATCHED("Dispatched"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING_PAYMENT
    }
}

enum class PaymentMethod(val label: String, val subtitle: String) {
    PAYSTACK("Paystack", "Cards, Bank, USSD & Transfer"),
    FLUTTERWAVE("Flutterwave", "Nigerian & International Cards"),
    BANK_TRANSFER("Direct Bank Transfer", "Instant Naira Transfer & Verification"),
    COD("Cash on Delivery", "Pay cash upon verified delivery");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: PAYSTACK
    }
}

enum class PaymentStatus(val label: String) {
    UNPAID("Unpaid"),
    PENDING_VERIFICATION("Pending Verification"),
    PAID("Paid"),
    FAILED("Failed"),
    REFUNDED("Refunded"),
    PARTIALLY_REFUNDED("Partially Refunded");

    companion object {
        fun fromRaw(raw: String?): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: UNPAID
    }
}

enum class PaymentRecordStatus(val label: String) {
    INITIATED("Initiated"),
    SUBMITTED("Submitted"),
    VERIFIED("Verified"),
    FAILED("Failed"),
    REFUNDED("Refunded");

    companion object {
        fun fromRaw(raw: String?): PaymentRecordStatus =
            entries.firstOrNull { it.name == raw } ?: INITIATED
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val photoUrl: String = "",
    val preferredCurrency: String = StoreCurrency.NGN.code,
    val role: String = "customer",
    val emailVerified: Boolean = false,
    val supportNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun resolvedFirstName(): String =
        firstName.ifBlank { displayName.trim().substringBefore(" ", "") }

    fun resolvedLastName(): String =
        lastName.ifBlank {
            val parts = displayName.trim().split(Regex("\\s+"))
            if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
        }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "firstName" to firstName.takeIf { it.isNotBlank() },
        "lastName" to lastName.takeIf { it.isNotBlank() },
        "photoUrl" to photoUrl.takeIf { it.isNotBlank() },
        "preferredCurrency" to preferredCurrency,
        "role" to role,
        "emailVerified" to emailVerified,
        "supportNotes" to supportNotes.takeIf { it.isNotBlank() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class DeliveryAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val recipientName: String = "",
    val phone: String = "",
    val country: String = "Nigeria",
    val state: String = "Lagos",
    val city: String = "",
    val lga: String = "",
    val streetAddress: String = "",
    val landmark: String = "",
    val deliveryInstructions: String = "",
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun formattedSummary(): String = listOfNotNull(
        streetAddress.takeIf { it.isNotBlank() },
        landmark.takeIf { it.isNotBlank() }?.let { "Near $it" },
        listOf(city, lga, state).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() },
        country.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "label" to label.trim().ifBlank { "Home" }.take(40),
        "recipientName" to recipientName.trim().take(80),
        "phone" to phone.trim().take(30),
        "country" to country.trim().ifBlank { "Nigeria" }.take(80),
        "state" to state.trim().take(80),
        "city" to city.trim().take(80),
        "lga" to lga.trim().takeIf { it.isNotBlank() }?.take(80),
        "streetAddress" to streetAddress.trim().take(250),
        "landmark" to landmark.trim().takeIf { it.isNotBlank() }?.take(150),
        "deliveryInstructions" to deliveryInstructions.trim().takeIf { it.isNotBlank() }?.take(250),
        "defaultAddress" to defaultAddress,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "label" to label.trim().ifBlank { "Home" }.take(40),
        "recipientName" to recipientName.trim().take(80),
        "phone" to phone.trim().take(30),
        "country" to country.trim().ifBlank { "Nigeria" }.take(80),
        "state" to state.trim().take(80),
        "city" to city.trim().take(80),
        "lga" to lga.trim().take(80),
        "streetAddress" to streetAddress.trim().take(250),
        "landmark" to landmark.trim().take(150),
        "deliveryInstructions" to deliveryInstructions.trim().take(250),
        "defaultAddress" to defaultAddress,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

data class GuestCartMergeResult(
    val addedCartItems: Int = 0,
    val mergedCartItems: Int = 0,
    val addedWishlistItems: Int = 0,
    val warnings: List<String> = emptyList(),
) {
    val totalSyncedCount: Int
        get() = addedCartItems + mergedCartItems + addedWishlistItems
}

@Keep
@IgnoreExtraProperties
data class UserPrivateInfo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phone: String = "",
    val shippingAddress: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "Nigeria",
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email,
        "phone" to phone.takeIf { it.isNotBlank() },
        "shippingAddress" to shippingAddress.takeIf { it.isNotBlank() },
        "city" to city.takeIf { it.isNotBlank() },
        "state" to state.takeIf { it.isNotBlank() },
        "country" to country.takeIf { it.isNotBlank() },
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "super_admin",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email,
        "displayName" to displayName.takeIf { it.isNotBlank() },
        "role" to role,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val title: String = "",
    val slug: String = "",
    val description: String = "",
    val categoryId: String = "",
    val collectionId: String = "",
    val sizeGuideId: String = "",
    val priceNgn: Double = 0.0,
    val compareAtPriceNgn: Double? = null,
    val priceUsd: Double = 0.0,
    val mainImageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val featured: Boolean = false,
    val newArrival: Boolean = false,
    val soldCount: Long = 0L,
    val brand: String = "",
    val sku: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val unavailableVariants: List<String> = emptyList(),
    val searchTokens: List<String> = emptyList(),
    val seoTitle: String = "",
    val seoDescription: String = "",
    val socialImageUrl: String = "",
    val summary: String = "",
    val features: List<String> = emptyList(),
    val fashionStyle: String = "",
    val suitableOccasions: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val aiAssistedDraft: Boolean = false,
    val lowStockThreshold: Long = 3L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun displayPrice(currency: StoreCurrency): Double =
        if (currency == StoreCurrency.USD) priceUsd else priceNgn

    fun discountPercent(): Int? {
        val compare = compareAtPriceNgn ?: return null
        if (compare <= priceNgn || compare <= 0.0) return null
        return (((compare - priceNgn) / compare) * 100.0).toInt().coerceIn(1, 95)
    }

    fun isVariantAvailable(size: String, color: String): Boolean {
        if (unavailableVariants.isEmpty()) return true
        val combo = "$size • $color".trim()
        val comboAlt = "$size - $color".trim()
        return unavailableVariants.none { blocked ->
            blocked.equals(combo, ignoreCase = true) ||
                blocked.equals(comboAlt, ignoreCase = true) ||
                blocked.equals(size, ignoreCase = true) ||
                blocked.equals(color, ignoreCase = true)
        }
    }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "title" to title,
        "slug" to slug,
        "description" to description,
        "categoryId" to categoryId,
        "collectionId" to collectionId.takeIf { it.isNotBlank() },
        "sizeGuideId" to sizeGuideId.takeIf { it.isNotBlank() },
        "priceNgn" to priceNgn,
        "compareAtPriceNgn" to compareAtPriceNgn,
        "priceUsd" to priceUsd,
        "mainImageUrl" to mainImageUrl.takeIf { it.isNotBlank() },
        "imageUrls" to imageUrls,
        "sizes" to sizes,
        "colors" to colors,
        "featured" to featured,
        "newArrival" to newArrival,
        "soldCount" to soldCount,
        "brand" to brand.takeIf { it.isNotBlank() },
        "sku" to sku.takeIf { it.isNotBlank() },
        "attributes" to attributes.takeIf { it.isNotEmpty() },
        "unavailableVariants" to unavailableVariants.takeIf { it.isNotEmpty() },
        "searchTokens" to searchTokens.takeIf { it.isNotEmpty() },
        "seoTitle" to seoTitle.takeIf { it.isNotBlank() },
        "seoDescription" to seoDescription.takeIf { it.isNotBlank() },
        "socialImageUrl" to socialImageUrl.takeIf { it.isNotBlank() },
        "summary" to summary.trim().takeIf { it.isNotBlank() }?.take(300),
        "features" to features.map { it.trim().take(140) }.filter { it.isNotBlank() }.take(10).takeIf { it.isNotEmpty() },
        "fashionStyle" to fashionStyle.trim().takeIf { it.isNotBlank() }?.take(80),
        "suitableOccasions" to suitableOccasions.map { it.trim().take(60) }.filter { it.isNotBlank() }.take(8).takeIf { it.isNotEmpty() },
        "tags" to tags.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(15).takeIf { it.isNotEmpty() },
        "aiAssistedDraft" to aiAssistedDraft,
        "lowStockThreshold" to lowStockThreshold,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val imageKey: String = "",
    val parentCategoryId: String = "",
    val sizeGuideId: String = "",
    val seoTitle: String = "",
    val seoDescription: String = "",
    val socialImageUrl: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name,
        "slug" to slug,
        "description" to description,
        "imageKey" to imageKey.takeIf { it.isNotBlank() },
        "parentCategoryId" to parentCategoryId.takeIf { it.isNotBlank() },
        "sizeGuideId" to sizeGuideId.takeIf { it.isNotBlank() },
        "seoTitle" to seoTitle.takeIf { it.isNotBlank() },
        "seoDescription" to seoDescription.takeIf { it.isNotBlank() },
        "socialImageUrl" to socialImageUrl.takeIf { it.isNotBlank() },
        "sortOrder" to sortOrder,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class StoreCollection(
    @DocumentId val id: String = "",
    val name: String = "",
    val slug: String = "",
    val tagline: String = "",
    val bannerKey: String = "",
    val seoTitle: String = "",
    val seoDescription: String = "",
    val socialImageUrl: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name,
        "slug" to slug,
        "tagline" to tagline,
        "bannerKey" to bannerKey.takeIf { it.isNotBlank() },
        "seoTitle" to seoTitle.takeIf { it.isNotBlank() },
        "seoDescription" to seoDescription.takeIf { it.isNotBlank() },
        "socialImageUrl" to socialImageUrl.takeIf { it.isNotBlank() },
        "sortOrder" to sortOrder,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val productImageUrl: String = "",
    val variantLabel: String = "",
    val unitPriceNgn: Double = 0.0,
    val quantity: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "productId" to productId,
        "productTitle" to productTitle,
        "productImageUrl" to productImageUrl.takeIf { it.isNotBlank() },
        "variantLabel" to variantLabel,
        "unitPriceNgn" to unitPriceNgn,
        "quantity" to quantity,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val productImageUrl: String = "",
    val priceNgn: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "productId" to productId,
        "productTitle" to productTitle,
        "productImageUrl" to productImageUrl.takeIf { it.isNotBlank() },
        "priceNgn" to priceNgn,
        "createdAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val itemsSummary: String = "",
    val itemCount: Long = 1L,
    val subtotalNgn: Double = 0.0,
    val deliveryFeeNgn: Double = 0.0,
    val discountNgn: Double = 0.0,
    val totalNgn: Double = 0.0,
    val currency: String = StoreCurrency.NGN.code,
    val deliveryZoneId: String = "",
    val recipientName: String = "",
    val recipientPhone: String = "",
    val shippingAddress: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingCountry: String = "",
    val lga: String = "",
    val landmark: String = "",
    val deliveryInstructions: String = "",
    val status: String = OrderStatus.PENDING_PAYMENT.name,
    val paymentMethod: String = PaymentMethod.PAYSTACK.name,
    val paymentStatus: String = PaymentStatus.UNPAID.name,
    val internalNotes: String = "",
    val cancellationRequestReason: String = "",
    val cancellationStatus: String = "",
    val refundedNgn: Double = 0.0,
    val customerEmail: String = "",
    val trackingToken: String = "",
    val guestLookupHashEmail: String = "",
    val guestLookupHashPhone: String = "",
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
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "orderNumber" to orderNumber,
        "itemsSummary" to itemsSummary,
        "itemCount" to itemCount,
        "subtotalNgn" to subtotalNgn,
        "deliveryFeeNgn" to deliveryFeeNgn,
        "discountNgn" to discountNgn,
        "totalNgn" to totalNgn,
        "currency" to currency,
        "deliveryZoneId" to deliveryZoneId,
        "recipientName" to recipientName.takeIf { it.isNotBlank() },
        "recipientPhone" to recipientPhone.takeIf { it.isNotBlank() },
        "shippingAddress" to shippingAddress.takeIf { it.isNotBlank() },
        "shippingCity" to shippingCity,
        "shippingState" to shippingState,
        "shippingCountry" to shippingCountry.takeIf { it.isNotBlank() },
        "lga" to lga.takeIf { it.isNotBlank() },
        "landmark" to landmark.takeIf { it.isNotBlank() },
        "deliveryInstructions" to deliveryInstructions.takeIf { it.isNotBlank() },
        "status" to status,
        "paymentMethod" to paymentMethod,
        "paymentStatus" to paymentStatus,
        "customerEmail" to customerEmail.takeIf { it.isNotBlank() },
        "trackingToken" to trackingToken.takeIf { it.isNotBlank() },
        "guestLookupHashEmail" to guestLookupHashEmail.takeIf { it.isNotBlank() },
        "guestLookupHashPhone" to guestLookupHashPhone.takeIf { it.isNotBlank() },
        "estimatedDeliveryWindow" to estimatedDeliveryWindow.takeIf { it.isNotBlank() },
        "deliveryStatus" to deliveryStatus.takeIf { it.isNotBlank() },
        "statusHistory" to statusHistory.takeIf { it.isNotEmpty() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val provider: String = PaymentMethod.PAYSTACK.name,
    val reference: String = "",
    val amountNgn: Double = 0.0,
    val currency: String = StoreCurrency.NGN.code,
    val status: String = PaymentRecordStatus.INITIATED.name,
    val transferNote: String = "",
    val verificationNote: String = "",
    val reviewedByEmail: String = "",
    val reviewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "orderId" to orderId,
        "orderNumber" to orderNumber.takeIf { it.isNotBlank() },
        "customerName" to customerName.takeIf { it.isNotBlank() },
        "provider" to provider,
        "reference" to reference,
        "amountNgn" to amountNgn,
        "currency" to currency,
        "status" to status,
        "transferNote" to transferNote.takeIf { it.isNotBlank() },
        "verificationNote" to verificationNote.takeIf { it.isNotBlank() },
        "reviewedByEmail" to reviewedByEmail.takeIf { it.isNotBlank() },
        "reviewedAt" to reviewedAt,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class InventoryRecord(
    @DocumentId val id: String = "",
    val productId: String = "",
    val sku: String = "",
    val stockQuantity: Long = 0L,
    val lowStockThreshold: Long = 3L,
    val reservedQuantity: Long = 0L,
    val variantStock: Map<String, Long> = emptyMap(),
    val inStock: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "sku" to sku,
        "stockQuantity" to stockQuantity,
        "lowStockThreshold" to lowStockThreshold,
        "reservedQuantity" to reservedQuantity,
        "variantStock" to variantStock.takeIf { it.isNotEmpty() },
        "inStock" to (stockQuantity > 0L),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val description: String = "",
    val discountType: String = "PERCENTAGE",
    val discountValue: Double = 0.0,
    val minOrderNgn: Double = 0.0,
    val maxDiscountNgn: Double? = null,
    val usageLimit: Long? = null,
    val perCustomerLimit: Long? = null,
    val usageCount: Long = 0L,
    val startsAtMillis: Long? = null,
    val expiresAtMillis: Long? = null,
    val applicableCategoryIds: List<String> = emptyList(),
    val applicableProductIds: List<String> = emptyList(),
    val restrictedEmails: List<String> = emptyList(),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun isCurrentlyValid(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!active) return false
        if (startsAtMillis != null && nowMillis < startsAtMillis) return false
        if (expiresAtMillis != null && nowMillis > expiresAtMillis) return false
        if (usageLimit != null && usageLimit > 0 && usageCount >= usageLimit) return false
        return true
    }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "code" to code.trim().uppercase(),
        "description" to description.trim().takeIf { it.isNotBlank() },
        "discountType" to discountType,
        "discountValue" to discountValue,
        "minOrderNgn" to minOrderNgn,
        "maxDiscountNgn" to maxDiscountNgn,
        "usageLimit" to usageLimit,
        "perCustomerLimit" to perCustomerLimit,
        "usageCount" to usageCount,
        "startsAtMillis" to startsAtMillis,
        "expiresAtMillis" to expiresAtMillis,
        "applicableCategoryIds" to applicableCategoryIds.takeIf { it.isNotEmpty() },
        "applicableProductIds" to applicableProductIds.takeIf { it.isNotEmpty() },
        "restrictedEmails" to restrictedEmails.takeIf { it.isNotEmpty() },
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val productId: String = "",
    val orderId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val visible: Boolean = true,
    val moderationStatus: String = "APPROVED",
    val flaggedReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DeliveryZone(
    @DocumentId val id: String = "",
    val name: String = "",
    val country: String = "Nigeria",
    val states: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val lgas: List<String> = emptyList(),
    val feeNgn: Double = 0.0,
    val estimatedDays: String = "",
    val promotionNote: String = "",
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name,
        "country" to country,
        "states" to states,
        "cities" to cities.takeIf { it.isNotEmpty() },
        "lgas" to lgas.takeIf { it.isNotEmpty() },
        "feeNgn" to feeNgn,
        "estimatedDays" to estimatedDays,
        "promotionNote" to promotionNote.takeIf { it.isNotBlank() },
        "active" to active,
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class StoreSettings(
    @DocumentId val id: String = "",
    val storeName: String = "ZEECROWN STORE",
    val tagline: String = "Style That Speaks. Elegance That Lasts.",
    val logoPlaceholderText: String = "ZEECROWN Official Crest",
    val logoUrl: String = "",
    val bankName: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val paystackPublicKey: String = "",
    val flutterwavePublicKey: String = "",
    val paystackEnabled: Boolean = true,
    val flutterwaveEnabled: Boolean = true,
    val bankTransferEnabled: Boolean = true,
    val codEnabled: Boolean = false,
    val guestCheckoutEnabled: Boolean = false,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "We are refreshing our seasonal boutique showcase. Check back shortly.",
    val supportEmail: String = "",
    val supportPhone: String = "",
    val whatsappNumber: String = BoutiqueContactHelper.OFFICIAL_WHATSAPP_NUMBER_DISPLAY,
    val businessAddress: String = "",
    val supportHours: String = "Mon - Sat, 9:00 AM - 6:00 PM WAT",
    val defaultCurrency: String = "NGN",
    val timeZone: String = "Africa/Lagos",
    val orderNumberPrefix: String = "ZC",
    val taxEnabled: Boolean = false,
    val taxRatePercent: Double = 0.0,
    val aboutText: String = "ZEECROWN STORE is a Nigerian and international luxury fashion boutique celebrating artisanal Adire & Kampala silk, handcrafted footwear, modest Abayas, handbags, jewellery, and curated beauty rituals.",
    val shippingPolicy: String = "Orders dispatch within 24 hours of verified settlement.",
    val returnPolicy: String = "ZEECROWN STORE currently does not accept customer returns or refunds. Please check our Size Guide and reach out on WhatsApp (+234 816 989 8763) before ordering if you need sizing or styling confirmation. If an order arrives damaged or incorrect, contact Customer Support immediately with your order number.",
    val privacyPolicy: String = "",
    val termsPolicy: String = "",
    val instagramHandle: String = "",
    val instagramUrl: String = "",
    val tiktokHandle: String = "",
    val tiktokUrl: String = "",
    val announcementMessages: List<String> = listOf(
        "Welcome to Zeecrown Store.",
        "Discover our latest fashion collections.",
        "Nationwide delivery available.",
        "Special offers on selected products.",
    ),
    val socialGalleryUrls: List<String> = emptyList(),
    val emailNotificationsEnabled: Boolean = true,
    val smsNotificationsEnabled: Boolean = false,
    val whatsappNotificationsEnabled: Boolean = false,
    val requireReviewApproval: Boolean = false,
    val emailSenderName: String = "ZEECROWN STORE",
    val emailReplyTo: String = "",
    val publicSiteBaseUrl: String = "https://zeecrownstore.com",
    val facebookUrl: String = "",
    val pinterestUrl: String = "",
    val youtubeUrl: String = "",
    val twitterHandle: String = "",
    val defaultSeoTitle: String = "ZEECROWN STORE — Luxury Nigerian & Modern Fashion Boutique",
    val defaultSeoDescription: String = "Shop handcrafted slippers, heels, handbags, artisanal Adire & Kampala fabrics, luxury Abayas, jewellery & beauty rituals with worldwide delivery.",
    val defaultSocialImageUrl: String = "",
    val ga4MeasurementId: String = "",
    val analyticsConsentRequired: Boolean = true,
    val visibility: String = "public",
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "storeName" to storeName,
        "tagline" to tagline,
        "logoPlaceholderText" to logoPlaceholderText,
        "logoUrl" to logoUrl.takeIf { it.isNotBlank() },
        "bankName" to bankName.takeIf { it.isNotBlank() },
        "accountName" to accountName.takeIf { it.isNotBlank() },
        "accountNumber" to accountNumber.takeIf { it.isNotBlank() },
        "paystackPublicKey" to paystackPublicKey.takeIf { it.isNotBlank() },
        "flutterwavePublicKey" to flutterwavePublicKey.takeIf { it.isNotBlank() },
        "paystackEnabled" to paystackEnabled,
        "flutterwaveEnabled" to flutterwaveEnabled,
        "bankTransferEnabled" to bankTransferEnabled,
        "codEnabled" to codEnabled,
        "guestCheckoutEnabled" to guestCheckoutEnabled,
        "maintenanceMode" to maintenanceMode,
        "maintenanceMessage" to maintenanceMessage.takeIf { it.isNotBlank() },
        "supportEmail" to supportEmail.takeIf { it.isNotBlank() },
        "supportPhone" to supportPhone.takeIf { it.isNotBlank() },
        "whatsappNumber" to whatsappNumber.takeIf { it.isNotBlank() },
        "businessAddress" to businessAddress.takeIf { it.isNotBlank() },
        "supportHours" to supportHours.takeIf { it.isNotBlank() },
        "defaultCurrency" to defaultCurrency,
        "timeZone" to timeZone.takeIf { it.isNotBlank() },
        "orderNumberPrefix" to orderNumberPrefix.takeIf { it.isNotBlank() },
        "taxEnabled" to taxEnabled,
        "taxRatePercent" to taxRatePercent.coerceIn(0.0, 50.0),
        "aboutText" to aboutText.takeIf { it.isNotBlank() },
        "shippingPolicy" to shippingPolicy.takeIf { it.isNotBlank() },
        "returnPolicy" to returnPolicy.takeIf { it.isNotBlank() },
        "privacyPolicy" to privacyPolicy.takeIf { it.isNotBlank() },
        "termsPolicy" to termsPolicy.takeIf { it.isNotBlank() },
        "instagramHandle" to instagramHandle.takeIf { it.isNotBlank() },
        "instagramUrl" to instagramUrl.takeIf { it.isNotBlank() },
        "tiktokHandle" to tiktokHandle.takeIf { it.isNotBlank() },
        "tiktokUrl" to tiktokUrl.takeIf { it.isNotBlank() },
        "announcementMessages" to announcementMessages.filter { it.isNotBlank() }.take(8),
        "socialGalleryUrls" to socialGalleryUrls.filter { it.isNotBlank() }.take(8),
        "emailNotificationsEnabled" to emailNotificationsEnabled,
        "smsNotificationsEnabled" to smsNotificationsEnabled,
        "whatsappNotificationsEnabled" to whatsappNotificationsEnabled,
        "requireReviewApproval" to requireReviewApproval,
        "emailSenderName" to emailSenderName.trim().takeIf { it.isNotBlank() }?.take(80),
        "emailReplyTo" to emailReplyTo.trim().takeIf { it.isNotBlank() }?.take(160),
        "publicSiteBaseUrl" to publicSiteBaseUrl.trim().takeIf { it.isNotBlank() }?.take(240),
        "facebookUrl" to facebookUrl.trim().takeIf { it.isNotBlank() }?.take(240),
        "pinterestUrl" to pinterestUrl.trim().takeIf { it.isNotBlank() }?.take(240),
        "youtubeUrl" to youtubeUrl.trim().takeIf { it.isNotBlank() }?.take(240),
        "twitterHandle" to twitterHandle.trim().takeIf { it.isNotBlank() }?.take(80),
        "defaultSeoTitle" to defaultSeoTitle.trim().takeIf { it.isNotBlank() }?.take(120),
        "defaultSeoDescription" to defaultSeoDescription.trim().takeIf { it.isNotBlank() }?.take(300),
        "defaultSocialImageUrl" to defaultSocialImageUrl.trim().takeIf { it.isNotBlank() }?.take(600),
        "ga4MeasurementId" to ga4MeasurementId.trim().uppercase().takeIf { it.isNotBlank() }?.take(30),
        "analyticsConsentRequired" to analyticsConsentRequired,
        "visibility" to visibility,
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class HomepageContent(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val ctaLabel: String = "SHOP NOW",
    val secondaryCtaLabel: String = "",
    val sectionType: String = "HERO",
    val targetCategorySlug: String = "",
    val imageKey: String = "img_hero_editorial",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "title" to title,
        "subtitle" to subtitle,
        "ctaLabel" to ctaLabel,
        "secondaryCtaLabel" to secondaryCtaLabel.takeIf { it.isNotBlank() },
        "sectionType" to sectionType.takeIf { it in listOf("HERO", "FEATURED_EDIT", "PROMO") },
        "targetCategorySlug" to targetCategorySlug.takeIf { it.isNotBlank() },
        "imageKey" to imageKey.takeIf { it.isNotBlank() },
        "sortOrder" to sortOrder,
        "active" to active,
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class NewsletterSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val active: Boolean = true,
    @ServerTimestamp val subscribedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email,
        "active" to active,
        "subscribedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val details: String = "",
    val category: String = "ORDER",
    val readFlag: Boolean = false,
    val readAt: Timestamp? = null,
    val targetRoute: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val dedupeKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.trim().take(120),
        "body" to body.trim().take(400),
        "details" to details.trim().takeIf { it.isNotBlank() }?.take(600),
        "category" to category,
        "readFlag" to readFlag,
        "targetRoute" to targetRoute.trim().takeIf { it.isNotBlank() }?.take(120),
        "orderId" to orderId.trim().takeIf { it.isNotBlank() }?.take(128),
        "orderNumber" to orderNumber.trim().takeIf { it.isNotBlank() }?.take(40),
        "dedupeKey" to dedupeKey.trim().takeIf { it.isNotBlank() }?.take(120),
        "createdAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }.mapValues { it.value!! }
}
