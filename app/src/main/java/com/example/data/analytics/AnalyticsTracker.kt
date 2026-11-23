package com.example.data.analytics

import android.util.Log
import com.example.data.model.Order
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.SeoUtils
import com.example.data.model.SocialSharePlatform
import com.example.data.model.StoreCurrency
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class AnalyticsEvent(
    val name: String,
    val measurementId: String,
    val params: Map<String, String>,
)

fun interface AnalyticsDispatcher {
    fun dispatch(event: AnalyticsEvent)
}

/**
 * Privacy-conscious e-commerce event tracker for ZEECROWN STORE.
 * - Never captures emails, phone numbers, street addresses, tokens, or payment secrets.
 * - Respects consent flag (`consentRequired` + `consentGranted`) and disabled Measurement IDs.
 * - Records `purchase` strictly after verified `PaymentStatus.PAID` and deduplicates per `orderId`.
 */
object AnalyticsTracker {
    private const val TAG = "ZeecrownAnalytics"

    private val measurementIdRef = AtomicReference("")
    private val consentRequired = AtomicBoolean(true)
    private val consentGranted = AtomicBoolean(false)
    private val recordedPurchaseIds: MutableSet<String> =
        Collections.synchronizedSet(linkedSetOf())
    private val recordedEventsForTesting: MutableList<AnalyticsEvent> =
        Collections.synchronizedList(mutableListOf())

    @Volatile
    private var dispatcher: AnalyticsDispatcher = AnalyticsDispatcher { event ->
        Log.d(TAG, "GA4[${event.measurementId}] ${event.name}: ${event.params}")
    }

    fun configure(
        measurementId: String,
        requireConsent: Boolean = true,
    ) {
        val cleaned = measurementId.trim().uppercase()
        measurementIdRef.set(if (SeoUtils.isValidGa4MeasurementId(cleaned)) cleaned else "")
        consentRequired.set(requireConsent)
    }

    fun setConsentGranted(granted: Boolean) {
        consentGranted.set(granted)
    }

    fun isConsentGranted(): Boolean = consentGranted.get()

    fun isConsentRequired(): Boolean = consentRequired.get()

    fun configuredMeasurementId(): String = measurementIdRef.get()

    fun canTrack(): Boolean {
        val id = measurementIdRef.get()
        if (id.isBlank()) return false
        return !consentRequired.get() || consentGranted.get()
    }

    fun setDispatcherForTesting(custom: AnalyticsDispatcher?) {
        dispatcher = custom ?: AnalyticsDispatcher { event ->
            Log.d(TAG, "GA4[${event.measurementId}] ${event.name}: ${event.params}")
        }
    }

    fun resetForTesting() {
        measurementIdRef.set("")
        consentRequired.set(true)
        consentGranted.set(false)
        recordedPurchaseIds.clear()
        recordedEventsForTesting.clear()
    }

    fun recordedEventsSnapshot(): List<AnalyticsEvent> =
        recordedEventsForTesting.toList()

    fun trackScreenView(screenName: String) {
        emit(
            name = "page_view",
            params = mapOf(
                "screen_name" to sanitizeValue(screenName, 64),
            ),
        )
    }

    fun trackViewItem(product: Product, currency: StoreCurrency = StoreCurrency.NGN) {
        emit(
            name = "view_item",
            params = mapOf(
                "item_id" to sanitizeValue(product.id, 64),
                "item_slug" to sanitizeValue(product.slug, 80),
                "item_name" to sanitizeValue(product.title, 80),
                "currency" to currency.code,
                "value" to String.format(java.util.Locale.US, "%.2f", product.displayPrice(currency)),
            ),
        )
    }

    fun trackViewItemList(listName: String, itemCount: Int) {
        emit(
            name = "view_item_list",
            params = mapOf(
                "item_list_name" to sanitizeValue(listName, 80),
                "item_count" to itemCount.coerceAtLeast(0).toString(),
            ),
        )
    }

    fun trackSearch(query: String, resultCount: Int) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        // Strip email-like or phone-like accidental input before recording search telemetry
        if (trimmed.contains("@") || trimmed.count { it.isDigit() } >= 7) return
        emit(
            name = "search",
            params = mapOf(
                "search_term" to sanitizeValue(trimmed, 80),
                "result_count" to resultCount.coerceAtLeast(0).toString(),
            ),
        )
    }

    fun trackAddToCart(productId: String, title: String, priceNgn: Double, quantity: Int) {
        emit(
            name = "add_to_cart",
            params = mapOf(
                "item_id" to sanitizeValue(productId, 64),
                "item_name" to sanitizeValue(title, 80),
                "currency" to StoreCurrency.NGN.code,
                "value" to String.format(java.util.Locale.US, "%.2f", priceNgn * quantity.coerceAtLeast(1)),
                "quantity" to quantity.coerceAtLeast(1).toString(),
            ),
        )
    }

    fun trackRemoveFromCart(productId: String, title: String, priceNgn: Double, quantity: Int) {
        emit(
            name = "remove_from_cart",
            params = mapOf(
                "item_id" to sanitizeValue(productId, 64),
                "item_name" to sanitizeValue(title, 80),
                "currency" to StoreCurrency.NGN.code,
                "value" to String.format(java.util.Locale.US, "%.2f", priceNgn * quantity.coerceAtLeast(1)),
                "quantity" to quantity.coerceAtLeast(1).toString(),
            ),
        )
    }

    fun trackAddToWishlist(product: Product) {
        emit(
            name = "add_to_wishlist",
            params = mapOf(
                "item_id" to sanitizeValue(product.id, 64),
                "item_name" to sanitizeValue(product.title, 80),
                "currency" to StoreCurrency.NGN.code,
                "value" to String.format(java.util.Locale.US, "%.2f", product.priceNgn),
            ),
        )
    }

    fun trackBeginCheckout(itemCount: Int, totalNgn: Double, couponApplied: Boolean) {
        emit(
            name = "begin_checkout",
            params = mapOf(
                "currency" to StoreCurrency.NGN.code,
                "value" to String.format(java.util.Locale.US, "%.2f", totalNgn),
                "item_count" to itemCount.coerceAtLeast(0).toString(),
                "coupon_applied" to couponApplied.toString(),
            ),
        )
    }

    fun trackAddPaymentInfo(paymentMethod: String, totalNgn: Double) {
        emit(
            name = "add_payment_info",
            params = mapOf(
                "payment_type" to sanitizeValue(paymentMethod, 40),
                "currency" to StoreCurrency.NGN.code,
                "value" to String.format(java.util.Locale.US, "%.2f", totalNgn),
            ),
        )
    }

    fun trackShare(contentType: String, itemId: String, platform: SocialSharePlatform) {
        emit(
            name = "share",
            params = mapOf(
                "content_type" to sanitizeValue(contentType, 40),
                "item_id" to sanitizeValue(itemId, 64),
                "method" to platform.name.lowercase(),
            ),
        )
    }

    /**
     * Records a `purchase` conversion ONLY when the order's payment is genuinely verified (`PAID`),
     * deduplicated strictly by `order.id`. Returns true if newly recorded, false otherwise.
     */
    fun trackVerifiedPurchase(order: Order): Boolean {
        if (order.id.isBlank()) return false
        if (PaymentStatus.fromRaw(order.paymentStatus) != PaymentStatus.PAID) return false
        if (!canTrack()) return false
        val added = recordedPurchaseIds.add(order.id)
        if (!added) return false

        emit(
            name = "purchase",
            params = mapOf(
                "transaction_id" to sanitizeValue(order.orderNumber.ifBlank { order.id }, 40),
                "currency" to sanitizeValue(order.currency.ifBlank { StoreCurrency.NGN.code }, 8),
                "value" to String.format(java.util.Locale.US, "%.2f", order.totalNgn),
                "shipping" to String.format(java.util.Locale.US, "%.2f", order.deliveryFeeNgn),
                "item_count" to order.itemCount.coerceAtLeast(1L).toString(),
            ),
        )
        return true
    }

    private fun emit(name: String, params: Map<String, String>) {
        if (!canTrack()) return
        val id = measurementIdRef.get()
        val event = AnalyticsEvent(
            name = name,
            measurementId = id,
            params = params.filterValues { it.isNotBlank() },
        )
        recordedEventsForTesting.add(event)
        dispatcher.dispatch(event)
    }

    private fun sanitizeValue(raw: String, maxLen: Int): String =
        raw.trim().replace(Regex("\\s+"), " ").take(maxLen)
}
