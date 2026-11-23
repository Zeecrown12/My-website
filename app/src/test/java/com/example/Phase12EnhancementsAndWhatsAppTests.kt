package com.example

import com.example.data.model.AdminRole
import com.example.data.model.BoutiqueContactHelper
import com.example.data.model.Category
import com.example.data.model.DefaultSizeGuides
import com.example.data.model.Product
import com.example.data.model.SizeGuide
import com.example.data.model.SizeGuideCategoryType
import com.example.data.model.SizeGuideResolver
import com.example.data.model.SizeGuideRow
import com.example.data.model.SizeGuideUnitConverter
import com.example.data.model.StoreSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12EnhancementsAndWhatsAppTests {

    @Test
    fun `01 boutique contact helper exposes exact official whatsapp number and link`() {
        assertEquals("+234 816 989 8763", BoutiqueContactHelper.OFFICIAL_WHATSAPP_NUMBER_DISPLAY)
        assertEquals("2348169898763", BoutiqueContactHelper.OFFICIAL_WHATSAPP_DIGITS)
        assertEquals("https://wa.me/2348169898763", BoutiqueContactHelper.OFFICIAL_WHATSAPP_URL)
        assertEquals("+234 816 989 8763", StoreSettings().whatsappNumber)
    }

    @Test
    fun `02 whatsapp number normalization converts local format and preserves official fallback`() {
        assertEquals("2348169898763", BoutiqueContactHelper.normalizeWhatsAppDigits("08169898763"))
        assertEquals("2348169898763", BoutiqueContactHelper.normalizeWhatsAppDigits("+234 816 989 8763"))
        assertEquals("2348169898763", BoutiqueContactHelper.normalizeWhatsAppDigits("123"))
        assertEquals("+234 816 989 8763", BoutiqueContactHelper.resolveWhatsAppDisplayNumber("   "))
        assertEquals(
            "https://wa.me/2348169898763",
            BoutiqueContactHelper.resolveWhatsAppUrl("")
        )
        assertEquals(
            "https://wa.me/2348169898763?text=Hello%20Zeecrown",
            BoutiqueContactHelper.resolveWhatsAppUrl("08169898763", "Hello Zeecrown")
        )
    }

    @Test
    fun `03 starter size guides cover slippers, shoes, abayas, and bubu adire kampala`() {
        val starters = DefaultSizeGuides.starterGuides().map { it.second }
        assertEquals(4, starters.size)
        assertTrue(starters.any { it.id == "sg_slippers_sandals" && it.resolvedType.isFootwear })
        assertTrue(starters.any { it.id == "sg_shoes_heels" && it.resolvedType.isFootwear })
        assertTrue(starters.any { it.id == "sg_abayas_bubu" && !it.resolvedType.isFootwear })
        assertTrue(starters.any { it.id == "sg_adire_kampala_clothing" && !it.resolvedType.isFootwear })
    }

    @Test
    fun `04 size guide resolver obeys product priority over category and keyword fallbacks`() {
        val guides = DefaultSizeGuides.starterGuides().map { it.second } + SizeGuide(
            id = "sg_custom_kaftan",
            name = "Custom Kaftan Fit",
            categoryType = SizeGuideCategoryType.CUSTOM.name,
            columns = listOf("Size", "Bust (cm)"),
            rows = listOf(SizeGuideRow("Free", listOf("128"))),
            assignedProductIds = listOf("prod_vip"),
            active = true,
        )
        val categories = listOf(
            Category(id = "cat_abayas", name = "Abayas", slug = "abayas", sizeGuideId = "sg_abayas_bubu"),
            Category(id = "cat_bags", name = "Leather Handbags", slug = "handbags"),
        )

        val directOverride = Product(
            id = "prod_abaya_heel_combo",
            title = "Modest Abaya Gown",
            categoryId = "cat_abayas",
            sizeGuideId = "sg_adire_kampala_clothing",
        )
        assertEquals(
            "sg_adire_kampala_clothing",
            SizeGuideResolver.resolveForProduct(directOverride, categories, guides)?.id
        )

        val assignedProd = Product(
            id = "prod_vip",
            title = "Royal Silk Boubou",
            categoryId = "cat_abayas",
        )
        assertEquals(
            "sg_custom_kaftan",
            SizeGuideResolver.resolveForProduct(assignedProd, categories, guides)?.id
        )

        val catAssigned = Product(
            id = "prod_plain",
            title = "Obsidian Silk Edition",
            categoryId = "cat_abayas",
        )
        assertEquals(
            "sg_abayas_bubu",
            SizeGuideResolver.resolveForProduct(catAssigned, categories, guides)?.id
        )

        val handbag = Product(
            id = "prod_bag_1",
            title = "Zaria Quilted Leather Tote",
            categoryId = "cat_bags",
        )
        assertNull(SizeGuideResolver.resolveForProduct(handbag, categories, guides))
    }

    @Test
    fun `05 semantic fallback matches slippers, heels, abayas, and adire without category id`() {
        val guides = DefaultSizeGuides.starterGuides().map { it.second }

        val slipper = Product(id = "p1", title = "Lagos Artisan Palm Slippers")
        val heel = Product(id = "p2", title = "Amina Block Heel Sandals")
        val adire = Product(id = "p3", title = "Kampala Dyed Bubu Kaftan")

        assertEquals("sg_slippers_sandals", SizeGuideResolver.resolveForProduct(slipper, emptyList(), guides)?.id)
        assertEquals("sg_shoes_heels", SizeGuideResolver.resolveForProduct(heel, emptyList(), guides)?.id)
        assertEquals("sg_adire_kampala_clothing", SizeGuideResolver.resolveForProduct(adire, emptyList(), guides)?.id)
    }

    @Test
    fun `06 size guide unit converter translates cm ranges to inches and skips eu shoe sizes`() {
        assertEquals("Bust (in)", SizeGuideUnitConverter.convertHeader("Bust (cm)", "IN"))
        assertEquals("Foot Length (cm)", SizeGuideUnitConverter.convertHeader("Foot Length (in)", "CM"))

        val convertedRange = SizeGuideUnitConverter.convertCellValue(
            rawValue = "88 - 94",
            columnHeader = "Bust (cm)",
            baseUnit = "CM",
            targetUnit = "IN",
        )
        assertEquals("34.6 - 37", convertedRange)

        val untouchedEu = SizeGuideUnitConverter.convertCellValue(
            rawValue = "39",
            columnHeader = "EU Size",
            baseUnit = "CM",
            targetUnit = "IN",
        )
        assertEquals("39", untouchedEu)

        val untouchedText = SizeGuideUnitConverter.convertCellValue(
            rawValue = "Free",
            columnHeader = "Waist (cm)",
            baseUnit = "CM",
            targetUnit = "IN",
        )
        assertEquals("Free", untouchedText)
    }

    @Test
    fun `07 firestore serialization maps preserve sizeGuideId and enforce size guide bounds`() {
        val guide = SizeGuide(
            id = "sg_test",
            name = "  Kampala Studio Chart  ",
            categoryType = SizeGuideCategoryType.CLOTHING_KAFTAN.name,
            measurementUnitDefault = "in",
            columns = List(12) { "Col $it" },
            rows = listOf(SizeGuideRow("M", listOf("98", "78", "104"))),
            assignedCategoryIds = listOf("cat_kampala", "cat_kampala", " "),
            sortOrder = 5000L,
        )
        val guideMap = guide.toCreateMap(isCreate = true)
        assertEquals("Kampala Studio Chart", guideMap["name"])
        assertEquals("IN", guideMap["measurementUnitDefault"])
        assertEquals(8, (guideMap["columns"] as List<*>).size)
        assertEquals(listOf("cat_kampala"), guideMap["assignedCategoryIds"])
        assertEquals(1000L, guideMap["sortOrder"])
        assertNotNull(guideMap["createdAt"])

        val prodMap = Product(
            id = "p1",
            title = "Abaya",
            sizeGuideId = "sg_abayas_bubu",
        ).toCreateMap()
        assertEquals("sg_abayas_bubu", prodMap["sizeGuideId"])

        val catMap = Category(
            id = "c1",
            name = "Abayas",
            sizeGuideId = "sg_abayas_bubu",
        ).toCreateMap()
        assertEquals("sg_abayas_bubu", catMap["sizeGuideId"])
    }

    @Test
    fun `08 size guide admin management respects existing rbac roles`() {
        assertTrue(AdminRole.SUPER_ADMIN.canAccessSection(com.example.data.model.AdminSection.SIZE_GUIDES))
        assertTrue(AdminRole.STORE_MANAGER.canAccessSection(com.example.data.model.AdminSection.SIZE_GUIDES))
        assertTrue(AdminRole.CONTENT_MANAGER.canAccessSection(com.example.data.model.AdminSection.SIZE_GUIDES))
        assertFalse(AdminRole.ORDER_MANAGER.canAccessSection(com.example.data.model.AdminSection.SIZE_GUIDES))
        assertFalse(AdminRole.SUPPORT_STAFF.canAccessSection(com.example.data.model.AdminSection.SIZE_GUIDES))
        assertFalse(AdminRole.NONE.canAccessSection(com.example.data.model.AdminSection.SIZE_GUIDES))
    }
}
