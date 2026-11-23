package com.example.ui.storefront

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.CatalogFilterState
import com.example.data.model.CatalogQueryEngine
import com.example.data.model.CatalogSortOption
import com.example.data.model.Category
import com.example.data.model.InventoryRecord
import com.example.data.model.Product
import com.example.data.model.SearchSuggestionType
import com.example.data.model.SeoUtils
import com.example.data.model.SocialSharePlatform
import com.example.data.model.SocialShareTarget
import com.example.data.model.StoreCollection
import com.example.data.model.StoreCurrency
import com.example.data.model.WishlistItem
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.CurrencyToggleChip
import com.example.ui.components.EditorialProductCard
import com.example.ui.components.EditorialSkeletonCard
import com.example.ui.components.SocialShareSheet
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownSoftLavender

@Composable
fun CatalogScreen(
    pagedProductsState: UiState<List<Product>>,
    allProductsForFacets: List<Product>,
    categoriesState: UiState<List<Category>>,
    collectionsState: UiState<List<StoreCollection>>,
    inventoryState: UiState<Map<String, InventoryRecord>>,
    wishlistItems: List<WishlistItem>,
    filterState: CatalogFilterState,
    pageLimit: Long,
    focusSearchNonce: Int,
    ratingsByProduct: Map<String, Pair<Double, Int>>,
    currency: StoreCurrency,
    onNavigateHome: () -> Unit,
    onUpdateFilter: ((CatalogFilterState) -> CatalogFilterState) -> Unit,
    onClearAllFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleCurrency: () -> Unit,
    onProductClick: (String) -> Unit,
    onQuickView: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    publicSiteBaseUrl: String = "https://zeecrownstore.com",
    onShareCatalogTarget: (String, String, SocialSharePlatform) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val wishlistIds = remember(wishlistItems) { wishlistItems.map { it.productId }.toSet() }
    val inventoryMap = (inventoryState as? UiState.Success)?.data ?: emptyMap()
    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
    val collections = (collectionsState as? UiState.Success)?.data.orEmpty()

    val topLevelCategories = remember(categories) {
        categories.filter { it.parentCategoryId.isBlank() }
    }
    val selectedCategory = remember(categories, filterState.selectedCategoryId) {
        categories.firstOrNull { it.id == filterState.selectedCategoryId }
    }
    val selectedSubcategory = remember(categories, filterState.selectedSubcategoryId) {
        categories.firstOrNull { it.id == filterState.selectedSubcategoryId }
    }
    val childSubcategories = remember(categories, filterState.selectedCategoryId) {
        if (filterState.selectedCategoryId == null) emptyList()
        else categories.filter { it.parentCategoryId == filterState.selectedCategoryId }
    }

    val facetSourceProducts = remember(pagedProductsState, allProductsForFacets) {
        allProductsForFacets.ifEmpty { (pagedProductsState as? UiState.Success)?.data.orEmpty() }
    }
    val facets = remember(facetSourceProducts) {
        CatalogQueryEngine.extractFacetOptions(facetSourceProducts)
    }
    val searchSuggestions = remember(filterState.searchQuery, facetSourceProducts, categories, collections) {
        CatalogQueryEngine.buildSearchSuggestions(
            query = filterState.searchQuery,
            products = facetSourceProducts,
            categories = categories,
            collections = collections,
        )
    }

    val selectedCollection = remember(collections, filterState.selectedCollectionId) {
        collections.firstOrNull { it.id == filterState.selectedCollectionId }
    }
    val searchFocusRequester = remember { FocusRequester() }
    var showMobileFilterSheet by remember { mutableStateOf(false) }
    var activeShareTarget by remember { mutableStateOf<SocialShareTarget?>(null) }

    LaunchedEffect(focusSearchNonce) {
        if (focusSearchNonce > 0) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        val showDesktopSidebar = maxWidth >= 840.dp
        val columns = when {
            maxWidth >= 1060.dp -> 3
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        Column(
            modifier = Modifier
                .widthIn(max = 1220.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            ShopBreadcrumbBar(
                selectedCategory = selectedCategory,
                selectedSubcategory = selectedSubcategory,
                onNavigateHome = onNavigateHome,
                onResetToShop = {
                    onUpdateFilter {
                        it.copy(selectedCategoryId = null, selectedSubcategoryId = null)
                    }
                },
                onSelectCategory = { catId ->
                    onUpdateFilter {
                        it.copy(selectedCategoryId = catId, selectedSubcategoryId = null)
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedSubcategory?.name
                            ?: selectedCategory?.name
                            ?: if (filterState.newArrivalsOnly) "New Arrivals"
                            else if (filterState.sortOption == CatalogSortOption.BEST_SELLING) "Best Sellers"
                            else "The Zeecrown Boutique",
                        style = MaterialTheme.typography.headlineLarge,
                        color = ZeecrownDeepPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selectedSubcategory?.description?.ifBlank { null }
                            ?: selectedCategory?.description?.ifBlank { null }
                            ?: "Curated Nigerian craftsmanship, luxury Abayas, Adire silk, leather footwear & accessories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                val shareableCat = selectedSubcategory ?: selectedCategory
                if (shareableCat != null || selectedCollection != null) {
                    IconButton(
                        onClick = {
                            activeShareTarget = when {
                                shareableCat != null -> SocialShareTarget(
                                    contentType = "category",
                                    itemId = shareableCat.id,
                                    title = shareableCat.seoTitle.ifBlank { "${shareableCat.name} | ZEECROWN STORE" },
                                    subtitle = shareableCat.seoDescription.ifBlank { shareableCat.description },
                                    canonicalUrl = SeoUtils.buildCanonicalCategoryUrl(publicSiteBaseUrl, shareableCat),
                                    imageUrl = shareableCat.socialImageUrl.ifBlank { shareableCat.imageKey },
                                )
                                selectedCollection != null -> SocialShareTarget(
                                    contentType = "collection",
                                    itemId = selectedCollection.id,
                                    title = selectedCollection.seoTitle.ifBlank { "${selectedCollection.name} | ZEECROWN STORE" },
                                    subtitle = selectedCollection.seoDescription.ifBlank { selectedCollection.tagline },
                                    canonicalUrl = SeoUtils.buildCanonicalCollectionUrl(publicSiteBaseUrl, selectedCollection),
                                    imageUrl = selectedCollection.socialImageUrl.ifBlank { selectedCollection.bannerKey },
                                )
                                else -> null
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("catalog_share_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share collection or category",
                            tint = ZeecrownDeepPurple,
                        )
                    }
                }
                CurrencyToggleChip(currency = currency, onToggle = onToggleCurrency)
            }

            CatalogSearchBarWithSuggestions(
                query = filterState.searchQuery,
                suggestions = searchSuggestions,
                focusRequester = searchFocusRequester,
                onQueryChange = { q -> onUpdateFilter { it.copy(searchQuery = q) } },
                onSuggestionClick = { suggestion ->
                    when (suggestion.type) {
                        SearchSuggestionType.CATEGORY -> {
                            onUpdateFilter {
                                it.copy(
                                    selectedCategoryId = suggestion.categoryId,
                                    selectedSubcategoryId = null,
                                    searchQuery = "",
                                )
                            }
                        }
                        SearchSuggestionType.PRODUCT, SearchSuggestionType.SKU -> {
                            val prodId = suggestion.productId
                            if (prodId != null) onProductClick(prodId)
                            else onUpdateFilter { it.copy(searchQuery = suggestion.queryText) }
                        }
                        SearchSuggestionType.COLLECTION -> {
                            onUpdateFilter { it.copy(searchQuery = suggestion.queryText) }
                        }
                    }
                },
            )

            // Category Navigation Strip with Visual Previews
            if (topLevelCategories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "cat_all") {
                        CategoryStripCard(
                            name = "All Pieces",
                            imageKey = "img_hero_editorial",
                            selected = filterState.selectedCategoryId == null,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(selectedCategoryId = null, selectedSubcategoryId = null)
                                }
                            },
                            testTag = "filter_all",
                        )
                    }
                    items(topLevelCategories, key = { it.id }) { cat ->
                        CategoryStripCard(
                            name = cat.name,
                            imageKey = cat.imageKey,
                            selected = filterState.selectedCategoryId == cat.id,
                            onClick = {
                                onUpdateFilter {
                                    val next = if (it.selectedCategoryId == cat.id) null else cat.id
                                    it.copy(selectedCategoryId = next, selectedSubcategoryId = null)
                                }
                            },
                            testTag = "filter_${cat.id}",
                        )
                    }
                }
            }

            // Nested Subcategory Chips when parent category has children
            if (childSubcategories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "sub_all") {
                        FilterChip(
                            selected = filterState.selectedSubcategoryId == null,
                            onClick = { onUpdateFilter { it.copy(selectedSubcategoryId = null) } },
                            label = { Text("All ${selectedCategory?.name ?: ""}") },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZeecrownSoftLavender),
                        )
                    }
                    items(childSubcategories, key = { it.id }) { sub ->
                        FilterChip(
                            selected = filterState.selectedSubcategoryId == sub.id,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(selectedSubcategoryId = if (it.selectedSubcategoryId == sub.id) null else sub.id)
                                }
                            },
                            label = { Text(sub.name) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("subcategory_chip_${sub.id}"),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZeecrownSoftLavender),
                        )
                    }
                }
            }

            ActiveFilterPillsRow(
                filter = filterState,
                categories = categories,
                collections = collections,
                onUpdateFilter = onUpdateFilter,
                onClearAll = onClearAllFilters,
            )

            // Main Catalog Body (Sidebar + Grid on wide screens, Full Grid + Filter Button on compact)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (showDesktopSidebar) {
                    DesktopFilterSidebar(
                        filter = filterState,
                        facets = facets,
                        collections = collections,
                        onUpdateFilter = onUpdateFilter,
                        onClearAll = onClearAllFilters,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val activeFilterCount = filterState.activeFilterCount()

                    // Product Count + Filter Trigger (Mobile) + Sort Control
                    val filteredProducts = remember(
                        pagedProductsState,
                        allProductsForFacets,
                        categories,
                        collections,
                        inventoryMap,
                        ratingsByProduct,
                        filterState,
                    ) {
                        val rawList = when {
                            // When searching or deep filtering, evaluate against full active catalog cache as well as paged cache
                            filterState.searchQuery.isNotBlank() || activeFilterCount > 0 -> {
                                allProductsForFacets.ifEmpty { (pagedProductsState as? UiState.Success)?.data.orEmpty() }
                            }
                            else -> (pagedProductsState as? UiState.Success)?.data.orEmpty()
                        }
                        CatalogQueryEngine.applyFiltersAndSort(
                            products = rawList,
                            categories = categories,
                            collections = collections,
                            inventoryMap = inventoryMap,
                            ratingsByProduct = ratingsByProduct,
                            filter = filterState,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (pagedProductsState is UiState.Loading) {
                                "Curating showroom..."
                            } else {
                                "${filteredProducts.size} ${if (filteredProducts.size == 1) "Piece" else "Pieces"}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("catalog_product_count"),
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!showDesktopSidebar) {
                                OutlinedButton(
                                    onClick = { showMobileFilterSheet = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("open_filters_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                ) {
                                    Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Filters")
                                    if (activeFilterCount > 0) {
                                        Spacer(Modifier.width(6.dp))
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(activeFilterCount.toString())
                                        }
                                    }
                                }
                            }
                            SortDropdownChip(
                                selectedSort = filterState.sortOption,
                                onSelectSort = { opt -> onUpdateFilter { it.copy(sortOption = opt) } },
                            )
                        }
                    }

                    when (pagedProductsState) {
                        is UiState.Loading -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                            ) {
                                items(2) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        repeat(columns) { EditorialSkeletonCard(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }

                        is UiState.Error -> BoutiqueErrorBanner(pagedProductsState.message)

                        is UiState.Success -> {
                            if (filteredProducts.isEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BoutiqueEmptyView(
                                        title = "No Matching Pieces",
                                        subtitle = "Adjust your search terms, price range, or department filters to explore more Zeecrown styles.",
                                        actionLabel = "Reset All Filters",
                                        onAction = onClearAllFilters,
                                    )
                                    if (topLevelCategories.isNotEmpty()) {
                                        Text(
                                            text = "Explore popular departments",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ZeecrownDeepPurple,
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(topLevelCategories.take(6), key = { "empty_cat_${it.id}" }) { cat ->
                                                FilterChip(
                                                    selected = false,
                                                    onClick = {
                                                        onClearAllFilters()
                                                        onUpdateFilter { it.copy(selectedCategoryId = cat.id, selectedSubcategoryId = null) }
                                                    },
                                                    label = { Text(cat.name) },
                                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val rawPagedCount = pagedProductsState.data.size
                                val hasMoreServerPages =
                                    filterState.searchQuery.isBlank() &&
                                        activeFilterCount == 0 &&
                                        rawPagedCount >= pageLimit &&
                                        pageLimit < 120L

                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    val rows = filteredProducts.chunked(columns)
                                    items(rows) { rowProducts ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            rowProducts.forEach { product ->
                                                val stock = inventoryMap[product.id]?.stockQuantity ?: 10L
                                                val ratingPair = ratingsByProduct[product.id]
                                                EditorialProductCard(
                                                    product = product,
                                                    currency = currency,
                                                    wishlisted = product.id in wishlistIds,
                                                    inStock = stock > 0L,
                                                    onProductClick = { onProductClick(product.id) },
                                                    onToggleWishlist = { onToggleWishlist(product) },
                                                    onQuickAdd = { onQuickAdd(product) },
                                                    verifiedRating = ratingPair?.first,
                                                    reviewCount = ratingPair?.second ?: 0,
                                                    onQuickView = { onQuickView(product) },
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                            repeat(columns - rowProducts.size) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }

                                    item(key = "catalog_pagination_footer") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = "Showing ${filteredProducts.size} curated ${if (filteredProducts.size == 1) "piece" else "pieces"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (hasMoreServerPages) {
                                                OutlinedButton(
                                                    onClick = onLoadMore,
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("load_more_products_button"),
                                                    shape = MaterialTheme.shapes.small,
                                                ) {
                                                    Text("Load More Pieces")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMobileFilterSheet) {
        MobileFilterBottomSheet(
            filter = filterState,
            facets = facets,
            collections = collections,
            onApply = { updated -> onUpdateFilter { updated } },
            onClearAll = onClearAllFilters,
            onDismiss = { showMobileFilterSheet = false },
        )
    }

    activeShareTarget?.let { shareTarget ->
        SocialShareSheet(
            target = shareTarget,
            onDismiss = { activeShareTarget = null },
            onSharePlatform = { platform ->
                onShareCatalogTarget(shareTarget.contentType, shareTarget.itemId, platform)
            },
        )
    }
}

@Composable
private fun CategoryStripCard(
    name: String,
    imageKey: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ZeecrownSoftLavender else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) ZeecrownMetallicGold else ZeecrownElegantSilver,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZeecrownProductImage(
                imageUrl = "drawable://${imageKey.ifBlank { "img_cat_adire_abaya" }}",
                contentDescription = name,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = ZeecrownDeepPurple,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}
