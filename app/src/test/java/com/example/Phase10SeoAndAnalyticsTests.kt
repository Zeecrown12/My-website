package com.example

import com.example.data.analytics.AnalyticsTracker
import com.example.data.model.Category
import com.example.data.model.Order
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.SeoLengthStatus
import com.example.data.model.SeoUtils
import com.example.data.model.SocialShareLinkBuilder
import com.example.data.model.SocialSharePlatform
import com.example.data.model.SocialShareTarget
import com.example.data.model.StoreCollection
import com.example.data.model.StoreCurrency
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase10SeoAndAnalyticsTests {

    @Before
    fun setup() {
        AnalyticsTracker.resetForTesting()
        AnalyticsTracker.setDispatcherForTesting { }
    }

    @After
    fun tearDown() {
        AnalyticsTracker.resetForTesting()
        AnalyticsTracker.setDispatcherForTesting(null)
    }

    @Test
    fun `seo utils sanitize slugs generate accurate defaults and evaluate lengths`() {
        assertEquals(
            "zaria-blush-and-silk-adire-bubu",
            SeoUtils.sanitizeSlug("  Zaria Blush & Silk!! Adire Bubu "),
        )
        assertEquals("zeecrown-piece", SeoUtils.sanitizeSlug("!!!   "))

        assertEquals("https://zeecrownstore.com", SeoUtils.normalizeBaseUrl("zeecrownstore.com/"))
        assertEquals("https://shop.zeecrown.ng", SeoUtils.normalizeBaseUrl("https://shop.zeecrown.ng///"))

        assertTrue(SeoUtils.isValidGa4MeasurementId("G-ABC123XYZ"))
        assertFalse(SeoUtils.isValidGa4MeasurementId("UA-12345-1"))
        assertFalse(SeoUtils.isValidGa4MeasurementId(""))

        assertEquals(SeoLengthStatus.OPTIMAL, SeoUtils.evaluateTitleLength("Zaria Blush Silk Adire Bubu | ZEECROWN STORE").status)
        assertEquals(SeoLengthStatus.TOO_LONG, SeoUtils.evaluateTitleLength("A".repeat(75)).status)
        assertEquals(
            SeoLengthStatus.OPTIMAL,
            SeoUtils.evaluateDescriptionLength(
                "Hand-dyed artisanal Nigerian silk Adire bubu in luminous blush pink. Tailored drape with nationwide & international courier delivery."
            ).status,
        )

        val prodDefaults = SeoUtils.generateProductSeoDefaults(
            title = "Zaria Silk Adire Bubu",
            description = "Artisanal hand-dyed silk dress.",
            categoryName = "Adire Silk",
        )
        assertEquals("zaria-silk-adire-bubu", prodDefaults.slug)
        assertTrue(prodDefaults.seoTitle.contains("Zaria Silk Adire Bubu"))
        assertTrue(prodDefaults.seoDescription.length in 70..165)

        val catDefaults = SeoUtils.generateCategorySeoDefaults("Luxury Abayas", "")
        assertEquals("luxury-abayas", catDefaults.slug)
        assertTrue(catDefaults.seoTitle.contains("Luxury Abayas"))

        val colDefaults = SeoUtils.generateCollectionSeoDefaults("Harmattan Gold Edit", "")
        assertEquals("harmattan-gold-edit", colDefaults.slug)
        assertTrue(colDefaults.seoTitle.contains("Harmattan Gold Edit"))

        assertEquals(
            "https://zeecrownstore.com/category/abayas",
            SeoUtils.buildCanonicalCategoryUrl("zeecrownstore.com", Category(id = "c1", slug = "abayas")),
        )
        assertEquals(
            "https://zeecrownstore.com/collection/harmattan-edit",
            SeoUtils.buildCanonicalCollectionUrl("https://zeecrownstore.com/", StoreCollection(id = "col1", slug = "harmattan-edit")),
        )
    }

    @Test
    fun `product json-ld returns null for inactive items and omits fake aggregateRating when reviews are empty`() {
        val draftProduct = Product(
            id = "p_draft",
            title = "Draft Abaya",
            slug = "draft-abaya",
            priceNgn = 80_000.0,
            active = false,
        )
        assertNull(
            SeoUtils.buildProductJsonLd(
                product = draftProduct,
                baseUrl = "https://zeecrownstore.com",
                inStock = true,
                visibleReviews = listOf(Review(id = "r1", rating = 5, visible = true)),
            )
        )

        val activeProduct = Product(
            id = "p_live",
            title = "Royal Plum Abaya",
            slug = "royal-plum-abaya",
            description = "Fluid silk crepe abaya.",
            priceNgn = 95_000.0,
            sku = "ZC-AB-09",
            mainImageUrl = "https://cdn.zeecrownstore.com/abaya.jpg",
            active = true,
        )

        val noReviewsJson = SeoUtils.buildProductJsonLd(
            product = activeProduct,
            baseUrl = "https://zeecrownstore.com",
            categoryName = "Abayas",
            inStock = false,
            visibleReviews = emptyList(),
            currency = StoreCurrency.NGN,
        )
        assertNotNull(noReviewsJson)
        assertTrue(noReviewsJson!!.contains("https://schema.org/OutOfStock"))
        assertFalse(noReviewsJson.contains("aggregateRating"))

        val withVisibleReviewsJson = SeoUtils.buildProductJsonLd(
            product = activeProduct,
            baseUrl = "https://zeecrownstore.com",
            inStock = true,
            visibleReviews = listOf(
                Review(id = "r1", rating = 5, visible = true),
                Review(id = "r2", rating = 4, visible = true),
                Review(id = "r3", rating = 1, visible = false), // hidden moderation excluded
            ),
        )
        assertNotNull(withVisibleReviewsJson)
        assertTrue(withVisibleReviewsJson!!.contains("\"ratingValue\":\"4.5\""))
        assertTrue(withVisibleReviewsJson.contains("\"reviewCount\":2"))
    }

    @Test
    fun `social share link builder constructs encoded platform share urls`() {
        val target = SocialShareTarget(
            contentType = "product",
            itemId = "p1",
            title = "Zaria Silk Bubu",
            subtitle = "Hand-dyed in Lagos",
            canonicalUrl = "https://zeecrownstore.com/product/zaria-silk-bubu",
            imageUrl = "https://cdn.zeecrownstore.com/zaria.jpg",
            priceFormatted = "₦68,500",
        )
        val wa = SocialShareLinkBuilder.buildWhatsAppUrl(target)
        assertTrue(wa.startsWith("https://wa.me/?text="))
        assertTrue(wa.contains("zaria-silk-bubu"))

        val fb = SocialShareLinkBuilder.buildFacebookUrl(target)
        assertEquals(
            "https://www.facebook.com/sharer/sharer.php?u=https%3A%2F%2Fzeecrownstore.com%2Fproduct%2Fzaria-silk-bubu",
            fb,
        )

        val pin = SocialShareLinkBuilder.buildPinterestUrl(target)
        assertTrue(pin.startsWith("https://pinterest.com/pin/create/button/?url="))
        assertTrue(pin.contains("media=https%3A%2F%2Fcdn.zeecrownstore.com%2Fzaria.jpg"))
    }

    @Test
    fun `analytics tracker gates on consent drops pii search queries and deduplicates verified purchases`() {
        // Disabled with blank or invalid ID
        AnalyticsTracker.configure("", requireConsent = false)
        AnalyticsTracker.trackScreenView("home")
        assertTrue(AnalyticsTracker.recordedEventsSnapshot().isEmpty())

        // Valid ID with consent required -> blocked until granted
        AnalyticsTracker.configure("G-ZC998877", requireConsent = true)
        AnalyticsTracker.trackScreenView("home")
        assertTrue(AnalyticsTracker.recordedEventsSnapshot().isEmpty())

        // Grant consent -> events flow
        AnalyticsTracker.setConsentGranted(true)
        AnalyticsTracker.trackScreenView("home")
        assertEquals(1, AnalyticsTracker.recordedEventsSnapshot().size)

        // Search queries containing emails or 7+ digit phone numbers are dropped (no PII)
        AnalyticsTracker.trackSearch("amaka@gmail.com", 0)
        AnalyticsTracker.trackSearch("08012345678", 0)
        AnalyticsTracker.trackSearch("silk adire", 4)
        assertEquals(
            listOf("page_view", "search"),
            AnalyticsTracker.recordedEventsSnapshot().map { it.name },
        )

        // Purchase tracking strictly requires PaymentStatus.PAID and deduplicates by order.id
        val pendingOrder = Order(
            id = "ord_100",
            orderNumber = "ZC-100",
            paymentStatus = PaymentStatus.PENDING_VERIFICATION.name,
            totalNgn = 55_000.0,
        )
        assertFalse(AnalyticsTracker.trackVerifiedPurchase(pendingOrder))

        val paidOrder = pendingOrder.copy(paymentStatus = PaymentStatus.PAID.name)
        assertTrue(AnalyticsTracker.trackVerifiedPurchase(paidOrder))
        // Second call with same order ID must not double-count revenue
        assertFalse(AnalyticsTracker.trackVerifiedPurchase(paidOrder))

        AnalyticsTracker.trackShare("product", "p1", SocialSharePlatform.WHATSAPP)
        val purchaseEvent = AnalyticsTracker.recordedEventsSnapshot().first { it.name == "purchase" }
        assertEquals("ZC-100", purchaseEvent.params["transaction_id"])
        assertEquals("55000.00", purchaseEvent.params["value"])
    }
}
