package com.example

import com.example.data.model.CatalogFilterMode
import com.example.data.model.HomepageContent
import com.example.data.model.NewsletterSubscription
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreCurrency
import com.example.data.model.StoreSettings
import com.example.data.model.UserPrivateInfo
import com.example.ui.components.formatBoutiquePrice
import com.example.ui.components.formatNgnAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreModelsTest {

    @Test
    fun `currency formatting outputs NGN and USD symbols correctly`() {
        assertEquals("₦74,500", formatNgnAmount(74500.0))
        assertEquals("₦74,500", formatBoutiquePrice(74500.0, 49.0, StoreCurrency.NGN))
        assertEquals("$49", formatBoutiquePrice(74500.0, 49.0, StoreCurrency.USD))
    }

    @Test
    fun `write maps exclude DocumentId and blank optional fields`() {
        val product = Product(
            id = "doc_123",
            title = "Zaria Adire Silk Bubu",
            slug = "zaria-adire-silk-bubu",
            description = "Hand-dyed silk Bubu",
            categoryId = "cat_abayas",
            collectionId = "",
            priceNgn = 74500.0,
            compareAtPriceNgn = null,
            priceUsd = 49.0,
            newArrival = true,
            soldCount = 4L,
        ).toCreateMap()

        assertFalse(product.containsKey("id"))
        assertFalse(product.containsKey("collectionId"))
        assertFalse(product.containsKey("compareAtPriceNgn"))
        assertEquals(true, product["newArrival"])
        assertEquals(4L, product["soldCount"])
        assertTrue(product.containsKey("createdAt"))
        assertTrue(product.containsKey("updatedAt"))

        val privateInfo = UserPrivateInfo(
            id = "info",
            userId = "user_1",
            email = "client@zeecrown.com",
            phone = "",
            shippingAddress = "14 Admiralty Way",
        ).toWriteMap()

        assertFalse(privateInfo.containsKey("id"))
        assertFalse(privateInfo.containsKey("phone"))
        assertEquals("14 Admiralty Way", privateInfo["shippingAddress"])

        val settings = StoreSettings(
            id = "public",
            logoPlaceholderText = "ZEECROWN Official Crest",
            instagramHandle = "@zeecrownstore",
            tiktokHandle = "",
            announcementMessages = listOf("Welcome", "  ", "Nationwide delivery"),
        ).toWriteMap()

        assertFalse(settings.containsKey("id"))
        assertEquals("ZEECROWN Official Crest", settings["logoPlaceholderText"])
        assertEquals("@zeecrownstore", settings["instagramHandle"])
        assertFalse(settings.containsKey("tiktokHandle"))
        assertEquals(listOf("Welcome", "Nationwide delivery"), settings["announcementMessages"])
    }

    @Test
    fun `product discount percentage calculates cleanly only on genuine savings`() {
        val discounted = Product(priceNgn = 74500.0, compareAtPriceNgn = 86000.0)
        assertEquals(13, discounted.discountPercent())

        val noDiscount = Product(priceNgn = 74500.0, compareAtPriceNgn = 74500.0)
        assertNull(noDiscount.discountPercent())

        val nullCompare = Product(priceNgn = 74500.0, compareAtPriceNgn = null)
        assertNull(nullCompare.discountPercent())
    }

    @Test
    fun `homepage content and newsletter write maps validate section types and fields`() {
        val hero = HomepageContent(
            id = "hero_main",
            title = "STYLE THAT SPEAKS.",
            subtitle = "Curated pieces",
            ctaLabel = "SHOP NOW",
            secondaryCtaLabel = "EXPLORE",
            sectionType = "HERO",
            targetCategorySlug = "",
        ).toCreateMap()

        assertFalse(hero.containsKey("id"))
        assertEquals("HERO", hero["sectionType"])
        assertEquals("EXPLORE", hero["secondaryCtaLabel"])
        assertFalse(hero.containsKey("targetCategorySlug"))

        val sub = NewsletterSubscription(
            id = "sub_abc",
            userId = "user_99",
            email = "elegance@zeecrown.com",
        ).toCreateMap()

        assertFalse(sub.containsKey("id"))
        assertEquals("user_99", sub["userId"])
        assertEquals("elegance@zeecrown.com", sub["email"])
        assertEquals(true, sub["active"])
        assertTrue(sub.containsKey("subscribedAt"))
    }

    @Test
    fun `enums safely fall back on unknown raw values`() {
        assertEquals(StoreCurrency.NGN, StoreCurrency.fromRaw("UNKNOWN"))
        assertEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.fromRaw(null))
        assertEquals(PaymentMethod.PAYSTACK, PaymentMethod.fromRaw("INVALID"))
        assertEquals(3, CatalogFilterMode.entries.size)
    }
}
