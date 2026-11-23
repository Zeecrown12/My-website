package com.example.data.model

import com.google.firebase.Timestamp

data class CatalogFacetOptions(
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val minAvailablePriceNgn: Double = 0.0,
    val maxAvailablePriceNgn: Double = 0.0,
)

enum class SearchSuggestionType {
    CATEGORY,
    COLLECTION,
    PRODUCT,
    SKU,
}

data class SearchSuggestion(
    val label: String,
    val queryText: String,
    val type: SearchSuggestionType,
    val categoryId: String? = null,
    val productId: String? = null,
)

object CatalogQueryEngine {

    private val tokenSplitRegex = Regex("[^a-z0-9\\-]+")

    fun buildSearchTokens(
        title: String,
        description: String,
        categoryName: String = "",
        collectionName: String = "",
        brand: String = "",
        sku: String = "",
        sizes: List<String> = emptyList(),
        colors: List<String> = emptyList(),
    ): List<String> {
        val rawText = listOf(title, description, categoryName, collectionName, brand, sku)
            .plus(sizes)
            .plus(colors)
            .joinToString(" ")
            .lowercase()

        val tokens = linkedSetOf<String>()
        rawText.split(tokenSplitRegex)
            .map { it.trim('-') }
            .filter { it.length >= 2 }
            .forEach { word ->
                tokens.add(word)
                // Add short prefixes (>= 3 chars) to support efficient Firestore token lookup
                if (word.length > 4) {
                    tokens.add(word.take(3))
                    tokens.add(word.take(4))
                }
            }
        if (sku.isNotBlank()) {
            tokens.add(sku.trim().lowercase())
        }
        return tokens.take(75)
    }

    fun extractFacetOptions(products: List<Product>): CatalogFacetOptions {
        if (products.isEmpty()) return CatalogFacetOptions()
        val sizes = products.flatMap { it.sizes }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val colors = products.flatMap { it.colors }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val brands = products.map { it.brand.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val prices = products.map { it.priceNgn }
        return CatalogFacetOptions(
            sizes = sizes,
            colors = colors,
            brands = brands,
            minAvailablePriceNgn = prices.minOrNull() ?: 0.0,
            maxAvailablePriceNgn = prices.maxOrNull() ?: 0.0,
        )
    }

    fun buildSearchSuggestions(
        query: String,
        products: List<Product>,
        categories: List<Category>,
        collections: List<StoreCollection>,
        limit: Int = 6,
    ): List<SearchSuggestion> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()

        val suggestions = mutableListOf<SearchSuggestion>()

        categories.filter { it.active && it.name.lowercase().contains(q) }
            .take(2)
            .forEach { cat ->
                suggestions += SearchSuggestion(
                    label = "Category: ${cat.name}",
                    queryText = cat.name,
                    type = SearchSuggestionType.CATEGORY,
                    categoryId = cat.id,
                )
            }

        collections.filter { it.active && it.name.lowercase().contains(q) }
            .take(1)
            .forEach { col ->
                suggestions += SearchSuggestion(
                    label = "Collection: ${col.name}",
                    queryText = col.name,
                    type = SearchSuggestionType.COLLECTION,
                )
            }

        products.filter { prod ->
            prod.title.lowercase().contains(q) || prod.brand.lowercase().contains(q)
        }.take(limit).forEach { prod ->
            if (suggestions.none { it.productId == prod.id }) {
                suggestions += SearchSuggestion(
                    label = prod.title,
                    queryText = prod.title,
                    type = SearchSuggestionType.PRODUCT,
                    productId = prod.id,
                )
            }
        }

        products.filter { prod ->
            prod.sku.isNotBlank() && prod.sku.lowercase().contains(q)
        }.take(2).forEach { prod ->
            if (suggestions.none { it.productId == prod.id }) {
                suggestions += SearchSuggestion(
                    label = "SKU ${prod.sku}: ${prod.title}",
                    queryText = prod.sku,
                    type = SearchSuggestionType.SKU,
                    productId = prod.id,
                )
            }
        }

        return suggestions.take(limit)
    }

    fun applyFiltersAndSort(
        products: List<Product>,
        categories: List<Category>,
        collections: List<StoreCollection>,
        inventoryMap: Map<String, InventoryRecord>,
        ratingsByProduct: Map<String, Pair<Double, Int>>,
        filter: CatalogFilterState,
    ): List<Product> {
        val categoryMap = categories.associateBy { it.id }
        val collectionMap = collections.associateBy { it.id }
        val childCategoryIdsForSelectedParent = if (filter.selectedCategoryId != null) {
            categories.filter { it.parentCategoryId == filter.selectedCategoryId }
                .map { it.id }
                .toSet()
        } else {
            emptySet()
        }
        val queryTerms = filter.searchQuery.trim().lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val filtered = products.filter { prod ->
            if (!prod.active) return@filter false

            // Category + nested subcategory match
            val matchesCategory = when {
                filter.selectedSubcategoryId != null -> prod.categoryId == filter.selectedSubcategoryId
                filter.selectedCategoryId != null -> {
                    prod.categoryId == filter.selectedCategoryId ||
                        prod.categoryId in childCategoryIdsForSelectedParent
                }
                else -> true
            }
            if (!matchesCategory) return@filter false

            // Collection & Brand filters
            if (filter.selectedCollectionId != null && prod.collectionId != filter.selectedCollectionId) {
                return@filter false
            }
            if (filter.selectedBrand != null && !prod.brand.equals(filter.selectedBrand, ignoreCase = true)) {
                return@filter false
            }

            // Price bounds
            if (filter.minPriceNgn != null && prod.priceNgn < filter.minPriceNgn) return@filter false
            if (filter.maxPriceNgn != null && prod.priceNgn > filter.maxPriceNgn) return@filter false

            // Stock availability
            val stockQty = inventoryMap[prod.id]?.stockQuantity ?: 10L
            if (filter.inStockOnly && stockQty <= 0L) return@filter false

            // Discount & New Arrival flags
            if (filter.discountedOnly && prod.discountPercent() == null) return@filter false
            if (filter.newArrivalsOnly && !prod.newArrival) return@filter false

            // Sizes & Colors (any selected matches)
            if (filter.selectedSizes.isNotEmpty() &&
                prod.sizes.none { size -> filter.selectedSizes.any { it.equals(size, ignoreCase = true) } }
            ) {
                return@filter false
            }
            if (filter.selectedColors.isNotEmpty() &&
                prod.colors.none { color -> filter.selectedColors.any { it.equals(color, ignoreCase = true) } }
            ) {
                return@filter false
            }

            // Search query across name, description, category, collection, brand, SKU & indexed tokens
            if (queryTerms.isNotEmpty()) {
                val cat = categoryMap[prod.categoryId]
                val parentCat = cat?.parentCategoryId?.takeIf { it.isNotBlank() }?.let { categoryMap[it] }
                val col = collectionMap[prod.collectionId]
                val searchableText = buildString {
                    append(prod.title).append(' ')
                    append(prod.description).append(' ')
                    append(prod.brand).append(' ')
                    append(prod.sku).append(' ')
                    append(inventoryMap[prod.id]?.sku.orEmpty()).append(' ')
                    append(cat?.name.orEmpty()).append(' ')
                    append(parentCat?.name.orEmpty()).append(' ')
                    append(col?.name.orEmpty()).append(' ')
                    append(prod.searchTokens.joinToString(" "))
                }.lowercase()
                if (!queryTerms.all { term -> searchableText.contains(term) }) {
                    return@filter false
                }
            }

            true
        }

        val hasRealSales = products.any { it.soldCount > 0L }
        return when (filter.sortOption) {
            CatalogSortOption.FEATURED -> filtered.sortedWith(
                compareByDescending<Product> { it.featured }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            CatalogSortOption.NEWEST -> filtered.sortedWith(
                compareByDescending<Product> { it.newArrival }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            CatalogSortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.priceNgn }
            CatalogSortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.priceNgn }
            CatalogSortOption.BEST_SELLING -> {
                if (hasRealSales) {
                    filtered.sortedWith(
                        compareByDescending<Product> { it.soldCount }
                            .thenByDescending { it.featured }
                    )
                } else {
                    filtered.sortedWith(
                        compareByDescending<Product> { it.featured }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
            }
            CatalogSortOption.HIGHEST_RATED -> filtered.sortedWith(
                compareByDescending<Product> { ratingsByProduct[it.id]?.first ?: 0.0 }
                    .thenByDescending { ratingsByProduct[it.id]?.second ?: 0 }
                    .thenByDescending { it.featured }
            )
        }
    }

    fun findRelatedProducts(
        currentProduct: Product,
        allProducts: List<Product>,
        categories: List<Category>,
        maxCount: Int = 6,
    ): List<Product> {
        val catMap = categories.associateBy { it.id }
        val currentCat = catMap[currentProduct.categoryId]
        val currentParentId = currentCat?.parentCategoryId?.takeIf { it.isNotBlank() } ?: currentProduct.categoryId

        return allProducts
            .asSequence()
            .filter { it.active && it.id != currentProduct.id }
            .map { candidate ->
                var score = 0
                val candCat = catMap[candidate.categoryId]
                val candParentId = candCat?.parentCategoryId?.takeIf { it.isNotBlank() } ?: candidate.categoryId
                if (candidate.categoryId == currentProduct.categoryId) score += 5
                else if (candParentId == currentParentId) score += 3
                if (currentProduct.collectionId.isNotBlank() &&
                    candidate.collectionId == currentProduct.collectionId
                ) score += 2
                if (currentProduct.brand.isNotBlank() &&
                    candidate.brand.equals(currentProduct.brand, ignoreCase = true)
                ) score += 2
                if (candidate.colors.any { c -> currentProduct.colors.any { it.equals(c, ignoreCase = true) } }) {
                    score += 1
                }
                if (candidate.featured) score += 1
                candidate to score
            }
            .sortedByDescending { it.second }
            .take(maxCount)
            .map { it.first }
            .toList()
    }
}
