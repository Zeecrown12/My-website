package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthValidation
import com.example.data.model.DeliveryAddress
import com.example.data.model.GuestCartMergeResult
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.StoreCurrency
import com.example.data.model.UserProfile
import com.example.data.repository.GuestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase04AuthAndAccountTests {

    @Test
    fun `nigerian and international phone numbers normalize and validate cleanly`() {
        assertNull(AuthValidation.validatePhone("08012345678"))
        assertNull(AuthValidation.validatePhone("0812 345 6789"))
        assertNull(AuthValidation.validatePhone("0703-456-7890"))
        assertNull(AuthValidation.validatePhone("09011223344"))
        assertNull(AuthValidation.validatePhone("+2348012345678"))
        assertNull(AuthValidation.validatePhone("+447911123456"))
        assertNull(AuthValidation.validatePhone("+12025550199"))

        assertEquals("+2348012345678", AuthValidation.normalizePhone("08012345678"))
        assertEquals("+2348123456789", AuthValidation.normalizePhone("0812 345 6789"))
        assertEquals("+2348031234567", AuthValidation.normalizePhone("2348031234567"))
        assertEquals("+447911123456", AuthValidation.normalizePhone("+44 7911 123456"))

        assertNotNull(AuthValidation.validatePhone(""))
        assertNotNull(AuthValidation.validatePhone("12345"))
        assertNotNull(AuthValidation.validatePhone("01012345678"))
    }

    @Test
    fun `email, names, and password strength rules enforce security standards`() {
        assertNull(AuthValidation.validateName("Amaka", "First name"))
        assertNotNull(AuthValidation.validateName("A", "First name"))
        assertNull(AuthValidation.validateEmail("amaka.obi@zeecrown.com"))
        assertNotNull(AuthValidation.validateEmail("not-an-email"))

        val weak = AuthValidation.evaluatePasswordStrength("abc")
        assertEquals("Weak", weak.label)
        assertFalse(weak.meetsMinimum)

        val fair = AuthValidation.evaluatePasswordStrength("abcdefgh")
        assertEquals("Fair", fair.label)
        assertFalse(fair.meetsMinimum)

        val good = AuthValidation.evaluatePasswordStrength("Abcdefgh")
        assertEquals("Good", good.label)
        assertTrue(good.meetsMinimum)

        val strong = AuthValidation.evaluatePasswordStrength("ZeeCrown#2025")
        assertEquals("Strong", strong.label)
        assertTrue(strong.meetsMinimum)

        assertNull(AuthValidation.validatePasswordConfirmation("ZeeCrown#2025", "ZeeCrown#2025"))
        assertNotNull(AuthValidation.validatePasswordConfirmation("ZeeCrown#2025", "Mismatch1!"))
    }

    @Test
    fun `user profile create map enforces customer role and never writes password fields`() {
        val profile = UserProfile(
            id = "uid_abc",
            userId = "uid_abc",
            displayName = "Teniola Adeyemi",
            firstName = "Teniola",
            lastName = "Adeyemi",
            preferredCurrency = StoreCurrency.NGN.code,
            role = "customer",
            emailVerified = false,
        )
        assertEquals("Teniola", profile.resolvedFirstName())
        assertEquals("Adeyemi", profile.resolvedLastName())

        val fallbackProfile = UserProfile(displayName = "Chidi Okafor Eze")
        assertEquals("Chidi", fallbackProfile.resolvedFirstName())
        assertEquals("Okafor Eze", fallbackProfile.resolvedLastName())

        val createMap = profile.toCreateMap()
        assertFalse(createMap.containsKey("id"))
        assertFalse(createMap.containsKey("password"))
        assertFalse(createMap.containsKey("passwordHash"))
        assertEquals("customer", createMap["role"])
        assertEquals("Teniola", createMap["firstName"])
        assertEquals("Adeyemi", createMap["lastName"])
        assertEquals(false, createMap["emailVerified"])
    }

    @Test
    fun `delivery address validation and firestore maps preserve defaultAddress key parity`() {
        assertTrue(AuthValidation.NIGERIAN_STATES.contains("Lagos"))
        assertTrue(AuthValidation.NIGERIAN_STATES.contains("Abuja (FCT)"))

        assertNull(
            AuthValidation.validateDeliveryAddress(
                label = "Home",
                recipientName = "Zainab Bello",
                phone = "08031234567",
                country = "Nigeria",
                state = "Lagos",
                city = "Lekki Phase 1",
                streetAddress = "14 Admiralty Way",
            )
        )
        assertNotNull(
            AuthValidation.validateDeliveryAddress(
                label = "Home",
                recipientName = "Zainab Bello",
                phone = "bad-phone",
                country = "Nigeria",
                state = "Lagos",
                city = "Lekki",
                streetAddress = "14 Admiralty Way",
            )
        )

        val address = DeliveryAddress(
            id = "addr_1",
            userId = "uid_1",
            label = "Home",
            recipientName = "Zainab Bello",
            phone = "+2348031234567",
            country = "Nigeria",
            state = "Lagos",
            city = "Lekki Phase 1",
            lga = "Eti-Osa",
            streetAddress = "14 Admiralty Way",
            landmark = "Near Zenith Bank",
            deliveryInstructions = "Call gate security on arrival",
            defaultAddress = true,
        )
        val createMap = address.toCreateMap()
        assertFalse(createMap.containsKey("id"))
        assertFalse(createMap.containsKey("isDefault"))
        assertEquals(true, createMap["defaultAddress"])
        assertEquals("uid_1", createMap["userId"])

        val updateMap = address.toUpdateMap()
        assertFalse(updateMap.containsKey("userId"))
        assertEquals(true, updateMap["defaultAddress"])
        assertEquals("14 Admiralty Way", updateMap["streetAddress"])
    }

    @Test
    fun `order create map persists address snapshot and guest merge summary counts accurately`() {
        val orderMap = Order(
            id = "ord_1",
            userId = "uid_1",
            orderNumber = "ZC-987654",
            itemsSummary = "1x Zaria Adire Bubu (M)",
            itemCount = 1L,
            subtotalNgn = 74500.0,
            deliveryFeeNgn = 3500.0,
            totalNgn = 78000.0,
            currency = StoreCurrency.NGN.code,
            deliveryZoneId = "zone_lagos",
            recipientName = "Zainab Bello",
            recipientPhone = "+2348031234567",
            shippingAddress = "14 Admiralty Way",
            shippingCity = "Lekki",
            shippingState = "Lagos",
            shippingCountry = "Nigeria",
            status = OrderStatus.PENDING_PAYMENT.name,
            paymentMethod = PaymentMethod.PAYSTACK.name,
            paymentStatus = PaymentStatus.UNPAID.name,
        ).toCreateMap()

        assertEquals("Zainab Bello", orderMap["recipientName"])
        assertEquals("+2348031234567", orderMap["recipientPhone"])
        assertEquals("14 Admiralty Way", orderMap["shippingAddress"])
        assertFalse(orderMap.containsKey("landmark"))

        val mergeSummary = GuestCartMergeResult(
            addedCartItems = 2,
            mergedCartItems = 1,
            addedWishlistItems = 3,
        )
        assertEquals(6, mergeSummary.totalSyncedCount)
    }

    @Test
    fun `guest session repository persists cart wishlist and currency locally via datastore`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GuestSessionRepository(context)
        repo.clearGuestCartAndWishlist()

        val sampleProduct = Product(
            id = "prod_adire_01",
            title = "Zaria Adire Silk Bubu",
            priceNgn = 74500.0,
            priceUsd = 49.0,
            active = true,
        )

        val addRes = repo.addToGuestCart(
            product = sampleProduct,
            variantLabel = "M • Rose Gold",
            quantity = 2L,
            availableStock = 5L,
        )
        assertTrue(addRes.isSuccess)

        val addedWish = repo.toggleGuestWishlist(sampleProduct)
        assertTrue(addedWish)

        repo.setGuestCurrency(StoreCurrency.USD)
        assertEquals(StoreCurrency.USD, repo.guestCurrencyFlow.first())

        val (cart, wish) = repo.peekGuestCartAndWishlist()
        assertEquals(1, cart.size)
        assertEquals(2L, cart.first().quantity)
        assertEquals("Zaria Adire Silk Bubu", cart.first().productTitle)
        assertEquals(1, wish.size)
        assertEquals("prod_adire_01", wish.first().productId)

        // Stock limit guard on guest cart
        val overStockRes = repo.addToGuestCart(
            product = sampleProduct,
            variantLabel = "M • Rose Gold",
            quantity = 10L,
            availableStock = 5L,
        )
        assertTrue(overStockRes.isFailure)

        repo.clearGuestCartAndWishlist()
        val (clearedCart, clearedWish) = repo.peekGuestCartAndWishlist()
        assertTrue(clearedCart.isEmpty())
        assertTrue(clearedWish.isEmpty())
    }

    @Test
    fun `auth manager resolves generated google web client id from android resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clientId = com.example.data.firebase.AuthManager.resolveWebClientId(context)
        assertNotNull(clientId)
        assertTrue(clientId!!.endsWith(".apps.googleusercontent.com"))
    }
}
