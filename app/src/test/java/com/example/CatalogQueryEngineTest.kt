package com.example

import com.example.data.model.CatalogFilterState
import com.example.data.model.CatalogQueryEngine
import com.example.data.model.CatalogSortOption
import com.example.data.model.Category
import com.example.data.model.InventoryRecord
import com.example.data.model.Product
import com.example.data.model.SearchSuggestionType
import com.example.data.model.StoreCollection
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogQueryEngineTest {

    private val categories = listOf(
        Category(id = "cat_abayas", name = "Abaya & Bubu", slug = "abayas", sortOrder = 1L),
        Category(
            id = "cat_bridal_abayas",
            name = "Bridal & Occasion Abayas",
            slug = "bridal-abayas",
            parentCategoryId = "cat_abayas",
            sortOrder = 2L,
        ),
        Category(id = "cat_slippers", name = "Slippers & Footwear", slug = "slippers", sortOrder = 3L),
    )

    private val collections = listOf(
        StoreCollection(id = "col_heritage_dyes", name = "Heritage Dyes", slug = "heritage-dyes"),
        StoreCollection(id = "col_lagos_atelier", name = "Lagos Atelier", slug = "lagos-atelier"),
    )

    private val products = listOf(
        Product(
            id = "p1",
            title = "Zaria Blush Silk Adire Bubu",
            description = "Hand-dyed crepe silk Bubu",
            categoryId = "cat_bridal_abayas",
            collectionId = "col_heritage_dyes",
            priceNgn = 74500.0,
            compareAtPriceNgn = 86000.0,
            sizes = listOf("Free Size", "Tall"),
            colors = listOf("Blush Rose", "Ivory Pearl"),
            brand = "Zeecrown Atelier",
            sku = "ZC-ZARIA-BUBU",
            unavailableVariants = listOf("Tall • Ivory Pearl"),
            featured = true,
            newArrival = true,
            soldCount = 12L,
            searchTokens = CatalogQueryEngine.buildSearchTokens(
                title = "Zaria Blush Silk Adire Bubu",
                description = "Hand-dyed crepe silk Bubu",
                categoryName = "Bridal & Occasion Abayas",
                collectionName = "Heritage Dyes",
                brand = "Zeecrown Atelier",
                sku = "ZC-ZARIA-BUBU",
            ),
            createdAt = Timestamp(2000L, 0),
        ),
        Product(
            id = "p2",
            title = "Eko Champagne Leather Slides",
            description = "Full-grain leather slide slippers",
            categoryId = "cat_slippers",
            collectionId = "col_lagos_atelier",
            priceNgn = 34000.0,
            sizes = listOf("38", "39", "40"),
            colors = listOf("Champagne Gold"),
            brand = "Zeecrown Footwear",
            sku = "ZC-EKO-SLIDE",
            featured = false,
            newArrival = true,
            soldCount = 25L,
            createdAt = Timestamp(3000L, 0),
        ),
        Product(
            id = "p3",
            title = "Nadia Evening Abaya",
            description = "Flowing silk evening Abaya",
            categoryId = "cat_abayas",
            collectionId = "col_lagos_atelier",
            priceNgn = 95000.0,
            compareAtPriceNgn = 110000.0,
            sizes = listOf("M", "L"),
            colors = listOf("Rosewater"),
            brand = "Zeecrown Atelier",
            sku = "ZC-NADIA-ABY",
            featured = true,
            newArrival = false,
            soldCount = 3L,
            createdAt = Timestamp(1000L, 0),
        ),
    )

    private val inventory = mapOf(
        "p1" to InventoryRecord(productId = "p1", sku = "ZC-ZARIA-BUBU", stockQuantity = 6L, inStock = true),
        "p2" to InventoryRecord(productId = "p2", sku = "ZC-EKO-SLIDE", stockQuantity = 14L, inStock = true),
        "p3" to InventoryRecord(productId = "p3", sku = "ZC-NADIA-ABY", stockQuantity = 0L, inStock = false),
    )

    private val ratings = mapOf(
        "p1" to (4.8 to 5),
        "p2" to (4.2 to 2),
        "p3" to (5.0 to 1),
    )

    @Test
    fun `parent category filter includes nested subcategory products while subcategory narrows down`() {
        val parentFiltered = CatalogQueryEngine.applyFiltersAndSort(
            products = products,
            categories = categories,
            collections = collections,
            inventoryMap = inventory,
            ratingsByProduct = ratings,
            filter = CatalogFilterState(selectedCategoryId = "cat_abayas"),
        )
        assertEquals(listOf("p1", "p3"), parentFiltered.map { it.id }.sorted())

        val subFiltered = CatalogQueryEngine.applyFiltersAndSort(
            products = products,
            categories = categories,
            collections = collections,
            inventoryMap = inventory,
            ratingsByProduct = ratings,
            filter = CatalogFilterState(
                selectedCategoryId = "cat_abayas",
                selectedSubcategoryId = "cat_bridal_abayas",
            ),
        )
        assertEquals(listOf("p1"), subFiltered.map { it.id })
    }

    @Test
    fun `combined multi-facet filters apply price, stock, discount, size and search query together`() {
        val result = CatalogQueryEngine.applyFiltersAndSort(
            products = products,
            categories = categories,
            collections = collections,
            inventoryMap = inventory,
            ratingsByProduct = ratings,
            filter = CatalogFilterState(
                minPriceNgn = 50000.0,
                maxPriceNgn = 90000.0,
                inStockOnly = true,
                discountedOnly = true,
                selectedSizes = setOf("Tall"),
                searchQuery = "ZC-ZARIA-BUBU",
            ),
        )
        assertEquals(listOf("p1"), result.map { it.id })
    }

    @Test
    fun `all six sort options order items accurately`() {
        val priceAsc = CatalogQueryEngine.applyFiltersAndSort(
            products, categories, collections, inventory, ratings,
            CatalogFilterState(sortOption = CatalogSortOption.PRICE_LOW_HIGH),
        )
        assertEquals(listOf("p2", "p1", "p3"), priceAsc.map { it.id })

        val priceDesc = CatalogQueryEngine.applyFiltersAndSort(
            products, categories, collections, inventory, ratings,
            CatalogFilterState(sortOption = CatalogSortOption.PRICE_HIGH_LOW),
        )
        assertEquals(listOf("p3", "p1", "p2"), priceDesc.map { it.id })

        val bestSelling = CatalogQueryEngine.applyFiltersAndSort(
            products, categories, collections, inventory, ratings,
            CatalogFilterState(sortOption = CatalogSortOption.BEST_SELLING),
        )
        assertEquals(listOf("p2", "p1", "p3"), bestSelling.map { it.id })

        val highestRated = CatalogQueryEngine.applyFiltersAndSort(
            products, categories, collections, inventory, ratings,
            CatalogFilterState(sortOption = CatalogSortOption.HIGHEST_RATED),
        )
        assertEquals(listOf("p3", "p1", "p2"), highestRated.map { it.id })

        val newest = CatalogQueryEngine.applyFiltersAndSort(
            products, categories, collections, inventory, ratings,
            CatalogFilterState(sortOption = CatalogSortOption.NEWEST),
        )
        assertEquals(listOf("p2", "p1", "p3"), newest.map { it.id })
    }

    @Test
    fun `unavailableVariants blocks only specified variation`() {
        val zaria = products.first { it.id == "p1" }
        assertFalse(zaria.isVariantAvailable("Tall", "Ivory Pearl"))
        assertTrue(zaria.isVariantAvailable("Tall", "Blush Rose"))
        assertTrue(zaria.isVariantAvailable("Free Size", "Ivory Pearl"))
    }

    @Test
    fun `search suggestions and related products operate accurately without self-inclusion`() {
        val suggestions = CatalogQueryEngine.buildSearchSuggestions(
            query = "Zaria",
            products = products,
            categories = categories,
            collections = collections,
        )
        assertTrue(suggestions.any { it.type == SearchSuggestionType.PRODUCT && it.productId == "p1" })

        val skuSuggestions = CatalogQueryEngine.buildSearchSuggestions(
            query = "ZC-EKO",
            products = products,
            categories = categories,
            collections = collections,
        )
        assertTrue(skuSuggestions.any { it.type == SearchSuggestionType.SKU && it.productId == "p2" })

        val related = CatalogQueryEngine.findRelatedProducts(
            currentProduct = products.first { it.id == "p1" },
            allProducts = products,
            categories = categories,
        )
        assertFalse(related.any { it.id == "p1" })
        assertEquals("p3", related.first().id)
    }
}
