package com.example

import com.example.data.model.AiAssistantConfig
import com.example.data.model.AiKnowledgeRetrievalEngine
import com.example.data.model.AiStoreSnapshot
import com.example.data.model.Category
import com.example.data.model.ProductDraftAiParser
import com.example.data.model.StoreSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase13BrandRedesignAndAiProductTest {

    private val sampleCategories = listOf(
        Category(id = "cat_abayas", name = "Luxury Abayas", slug = "luxury-abayas", active = true),
        Category(id = "cat_slippers", name = "Leather Slippers", slug = "leather-slippers", active = true),
        Category(id = "cat_adire", name = "Adire Silk", slug = "adire-silk", active = true),
    )

    @Test
    fun `01 product ai draft parser populates visual fields and resolves existing categories accurately`() {
        val rawJson = """
            {
              "suggestedTitle": "Imperial Purple Embroidered Kaftan Abaya",
              "summary": "Flowing modest silhouette accented with gold embroidery along the cuffs.",
              "description": "Crafted for grand evening occasions, featuring billowing sleeves and ornate metallic-gold trim.",
              "visibleFeatures": ["Gold cuff embroidery", "Draped maxi silhouette", "Round modest neckline"],
              "suggestedCategoryId": "",
              "suggestedCategoryName": "Luxury Abayas",
              "visibleColors": ["Royal Purple", "Gold"],
              "fashionStyle": "Modest Luxury",
              "suitableOccasions": ["Eid Celebrations", "Evening Receptions"],
              "seoTitle": "Imperial Purple Embroidered Abaya | ZEECROWN STORE",
              "seoDescription": "Shop the draped royal purple modest abaya with gold cuff embroidery at ZEECROWN STORE.",
              "searchKeywords": ["purple abaya", "gold embroidered gown"],
              "tags": ["abaya", "modest"],
              "uncertainDetails": ["Exact fabric composition requires physical tag confirmation"]
            }
        """.trimIndent()

        val result = ProductDraftAiParser.parseAndSanitize(rawJson, sampleCategories)
        assertTrue(result.isSuccess)
        val draft = result.getOrThrow()

        assertEquals("Imperial Purple Embroidered Kaftan Abaya", draft.suggestedTitle)
        assertEquals("cat_abayas", draft.suggestedCategoryId)
        assertEquals("Luxury Abayas", draft.suggestedCategoryName)
        assertEquals(listOf("Royal Purple", "Gold"), draft.suggestedColors)
        assertEquals("Modest Luxury", draft.fashionStyle)
        assertEquals(listOf("Exact fabric composition requires physical tag confirmation"), draft.uncertainNotes)
    }

    @Test
    fun `02 product ai guardrails strip invented prices fabric percentages origins measurements and stock`() {
        val hallucinatedJson = """
            {
              "suggestedTitle": "Artisan Slippers ₦45,000",
              "summary": "100% silk lined pair Made in Italy with 12 cm straps and OEKO-TEX certification.",
              "description": "Priced at $120 with Only 3 left in stock: 3. Weighs 450 grams.",
              "visibleFeatures": ["Hand-stitched vamp", "In stock: 3", "95% cotton lining"],
              "suggestedCategoryId": "non_existent",
              "suggestedCategoryName": "Winter Ski Gear",
              "visibleColors": [],
              "fashionStyle": "Boutique Footwear",
              "suitableOccasions": ["Everyday Luxury"],
              "seoTitle": "Artisan Slippers ₦45,000",
              "seoDescription": "100% silk Made in Italy slippers.",
              "searchKeywords": ["slippers"],
              "tags": ["footwear"],
              "uncertainDetails": ["Sole material", "Exact sizing measurements"]
            }
        """.trimIndent()

        val draft = ProductDraftAiParser.parseAndSanitize(hallucinatedJson, sampleCategories).getOrThrow()

        val combinedText = listOf(draft.suggestedTitle, draft.summary, draft.description) + draft.visibleFeatures
        combinedText.forEach { text ->
            assertFalse("Price slipped through: $text", text.contains("45,000"))
            assertFalse("USD slipped through: $text", text.contains("$120"))
            assertFalse("Fabric % slipped through: $text", text.contains("100% silk", ignoreCase = true))
            assertFalse("Fabric % slipped through: $text", text.contains("95% cotton", ignoreCase = true))
            assertFalse("Origin slipped through: $text", text.contains("Made in Italy", ignoreCase = true))
            assertFalse("Measurement slipped through: $text", text.contains("12 cm", ignoreCase = true))
            assertFalse("Weight slipped through: $text", text.contains("450 grams", ignoreCase = true))
            assertFalse("Cert slipped through: $text", text.contains("OEKO-TEX", ignoreCase = true))
            assertFalse("Stock slipped through: $text", text.contains("Only 3 left", ignoreCase = true))
        }
        assertEquals("", draft.suggestedCategoryId)
        assertTrue(draft.guardrailWarnings.size >= 5)
    }

    @Test
    fun `03 ai assistant overrides stale 7-day policy and enforces official no-returns no-refunds rule`() {
        val legacySnapshot = AiStoreSnapshot(
            config = AiAssistantConfig(enabled = true),
            storeSettings = StoreSettings(
                storeName = "ZEECROWN STORE",
                returnPolicy = "Items may be returned within 7 days of delivery.",
                whatsappNumber = "+234 816 989 8763",
            ),
        )

        val intent = AiKnowledgeRetrievalEngine.analyzeAndRetrieve("Can I get a refund or return my order?", legacySnapshot)
        val reply = AiKnowledgeRetrievalEngine.buildDeterministicGroundedReply("Can I get a refund or return my order?", intent, legacySnapshot)

        assertTrue(intent.asksAboutReturns)
        assertTrue(reply.text.contains("does not accept customer returns or refunds", ignoreCase = true))
        assertTrue(reply.text.contains("+234 816 989 8763"))
        assertFalse(reply.text.contains("7 days", ignoreCase = true))
        assertFalse(reply.text.contains("7-day", ignoreCase = true))
    }

    @Test
    fun `04 customer care prompt context preserves no-returns truth and customer care framing`() {
        val snapshot = AiStoreSnapshot(
            config = AiAssistantConfig(enabled = true, assistantName = "ZEECROWN CUSTOMER CARE"),
            storeSettings = StoreSettings(
                storeName = "ZEECROWN STORE",
                returnPolicy = "ZEECROWN STORE currently does not accept customer returns or refunds.",
            ),
        )
        val intent = AiKnowledgeRetrievalEngine.analyzeAndRetrieve("What is your refund policy?", snapshot)
        val prompt = AiKnowledgeRetrievalEngine.buildGroundedModelPrompt("What is your refund policy?", intent, snapshot)

        assertTrue(prompt.contains("does not accept customer returns or refunds", ignoreCase = true))
        assertFalse(prompt.contains("7-day", ignoreCase = true))
    }
}
