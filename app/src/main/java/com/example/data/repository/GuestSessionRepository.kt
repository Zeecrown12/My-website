package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreCurrency
import com.example.data.model.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.guestSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "zeecrown_guest_session"
)

class GuestSessionRepository(context: Context) {

    private val dataStore = context.applicationContext.guestSessionDataStore

    val guestCartFlow: Flow<List<CartItem>> = dataStore.data.map { prefs ->
        decodeCartItems(prefs[KEY_GUEST_CART].orEmpty())
    }

    val guestWishlistFlow: Flow<List<WishlistItem>> = dataStore.data.map { prefs ->
        decodeWishlistItems(prefs[KEY_GUEST_WISHLIST].orEmpty())
    }

    val guestCurrencyFlow: Flow<StoreCurrency> = dataStore.data.map { prefs ->
        StoreCurrency.fromRaw(prefs[KEY_GUEST_CURRENCY])
    }

    suspend fun setGuestCurrency(currency: StoreCurrency) {
        dataStore.edit { prefs ->
            prefs[KEY_GUEST_CURRENCY] = currency.code
        }
    }

    suspend fun addToGuestCart(
        product: Product,
        variantLabel: String,
        quantity: Long = 1L,
        availableStock: Long = 10L,
    ): Result<Unit> {
        if (!product.active || product.id.isBlank()) {
            return Result.failure(IllegalStateException("This piece is currently unavailable."))
        }
        val cleanVariant = variantLabel.trim().ifBlank { "Standard" }.take(80)
        if (product.unavailableVariants.any { it.equals(cleanVariant, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Selected variation ($cleanVariant) is out of stock."))
        }
        if (availableStock <= 0L) {
            return Result.failure(IllegalStateException("${product.title} is currently out of stock."))
        }
        val safeQty = quantity.coerceIn(1L, 50L)
        val itemId = buildGuestCartItemId(product.id, cleanVariant)

        var limitError: String? = null
        dataStore.edit { prefs ->
            val current = decodeCartItems(prefs[KEY_GUEST_CART].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                val nextQty = current[index].quantity + safeQty
                if (nextQty > availableStock) {
                    limitError = "Only $availableStock piece(s) of ${product.title} available in stock."
                    return@edit
                }
                current[index] = current[index].copy(
                    quantity = nextQty.coerceAtMost(50L),
                    unitPriceNgn = product.priceNgn,
                    productTitle = product.title.take(120),
                )
            } else {
                if (safeQty > availableStock) {
                    limitError = "Only $availableStock piece(s) of ${product.title} available in stock."
                    return@edit
                }
                current.add(
                    0,
                    CartItem(
                        id = itemId,
                        userId = GUEST_USER_ID,
                        productId = product.id,
                        productTitle = product.title.take(120),
                        productImageUrl = product.mainImageUrl.ifBlank { product.imageUrls.firstOrNull().orEmpty() }.take(600),
                        variantLabel = cleanVariant,
                        unitPriceNgn = product.priceNgn,
                        quantity = safeQty,
                    )
                )
            }
            prefs[KEY_GUEST_CART] = encodeCartItems(current)
        }
        return if (limitError != null) {
            Result.failure(IllegalStateException(limitError))
        } else {
            Result.success(Unit)
        }
    }

    suspend fun updateGuestCartQuantity(
        itemId: String,
        newQuantity: Long,
        availableStock: Long = 50L,
    ): Result<Unit> {
        var limitError: String? = null
        dataStore.edit { prefs ->
            val current = decodeCartItems(prefs[KEY_GUEST_CART].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                if (newQuantity <= 0L) {
                    current.removeAt(index)
                } else if (newQuantity > availableStock) {
                    limitError = "Only $availableStock piece(s) available in stock."
                    return@edit
                } else {
                    current[index] = current[index].copy(quantity = newQuantity.coerceAtMost(50L))
                }
                prefs[KEY_GUEST_CART] = encodeCartItems(current)
            }
        }
        return if (limitError != null) {
            Result.failure(IllegalStateException(limitError))
        } else {
            Result.success(Unit)
        }
    }

    suspend fun removeGuestCartItem(itemId: String) {
        dataStore.edit { prefs ->
            val current = decodeCartItems(prefs[KEY_GUEST_CART].orEmpty())
                .filterNot { it.id == itemId }
            prefs[KEY_GUEST_CART] = encodeCartItems(current)
        }
    }

    suspend fun toggleGuestWishlist(product: Product): Boolean {
        var added = false
        dataStore.edit { prefs ->
            val current = decodeWishlistItems(prefs[KEY_GUEST_WISHLIST].orEmpty()).toMutableList()
            val existingIdx = current.indexOfFirst { it.productId == product.id }
            if (existingIdx >= 0) {
                current.removeAt(existingIdx)
                added = false
            } else {
                current.add(
                    0,
                    WishlistItem(
                        id = product.id,
                        userId = GUEST_USER_ID,
                        productId = product.id,
                        productTitle = product.title.take(120),
                        productImageUrl = product.mainImageUrl.ifBlank { product.imageUrls.firstOrNull().orEmpty() }.take(600),
                        priceNgn = product.priceNgn,
                    )
                )
                added = true
            }
            prefs[KEY_GUEST_WISHLIST] = encodeWishlistItems(current)
        }
        return added
    }

    suspend fun peekGuestCartAndWishlist(): Pair<List<CartItem>, List<WishlistItem>> {
        val prefs = dataStore.data.first()
        val cart = decodeCartItems(prefs[KEY_GUEST_CART].orEmpty())
        val wishlist = decodeWishlistItems(prefs[KEY_GUEST_WISHLIST].orEmpty())
        return cart to wishlist
    }

    suspend fun clearGuestCartAndWishlist() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_GUEST_CART)
            prefs.remove(KEY_GUEST_WISHLIST)
        }
    }

    private companion object {
        const val GUEST_USER_ID = "guest"
        val KEY_GUEST_CART = stringPreferencesKey("guest_cart_json")
        val KEY_GUEST_WISHLIST = stringPreferencesKey("guest_wishlist_json")
        val KEY_GUEST_CURRENCY = stringPreferencesKey("guest_currency")

        fun buildGuestCartItemId(productId: String, variantLabel: String): String =
            "${productId}_${variantLabel.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}".take(120)

        fun encodeCartItems(items: List<CartItem>): String {
            val array = JSONArray()
            items.forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("productId", item.productId)
                        put("productTitle", item.productTitle)
                        put("productImageUrl", item.productImageUrl)
                        put("variantLabel", item.variantLabel)
                        put("unitPriceNgn", item.unitPriceNgn)
                        put("quantity", item.quantity)
                    }
                )
            }
            return array.toString()
        }

        fun decodeCartItems(raw: String): List<CartItem> {
            if (raw.isBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val pid = obj.optString("productId")
                        if (pid.isBlank()) continue
                        val variant = obj.optString("variantLabel", "Standard").ifBlank { "Standard" }
                        add(
                            CartItem(
                                id = obj.optString("id").ifBlank { buildGuestCartItemId(pid, variant) },
                                userId = GUEST_USER_ID,
                                productId = pid,
                                productTitle = obj.optString("productTitle"),
                                productImageUrl = obj.optString("productImageUrl"),
                                variantLabel = variant,
                                unitPriceNgn = obj.optDouble("unitPriceNgn", 0.0),
                                quantity = obj.optLong("quantity", 1L).coerceIn(1L, 50L),
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        fun encodeWishlistItems(items: List<WishlistItem>): String {
            val array = JSONArray()
            items.forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("productId", item.productId)
                        put("productTitle", item.productTitle)
                        put("productImageUrl", item.productImageUrl)
                        put("priceNgn", item.priceNgn)
                    }
                )
            }
            return array.toString()
        }

        fun decodeWishlistItems(raw: String): List<WishlistItem> {
            if (raw.isBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val pid = obj.optString("productId")
                        if (pid.isBlank()) continue
                        add(
                            WishlistItem(
                                id = obj.optString("id").ifBlank { pid },
                                userId = GUEST_USER_ID,
                                productId = pid,
                                productTitle = obj.optString("productTitle"),
                                productImageUrl = obj.optString("productImageUrl"),
                                priceNgn = obj.optDouble("priceNgn", 0.0),
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
