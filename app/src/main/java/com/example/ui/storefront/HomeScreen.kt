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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.CatalogFilterMode
import com.example.data.model.Category
import com.example.data.model.DeliveryZone
import com.example.data.model.HomepageContent
import com.example.data.model.InventoryRecord
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.StoreCurrency
import com.example.data.model.StoreSettings
import com.example.data.model.WishlistItem
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.EditorialProductCard
import com.example.ui.components.EditorialSkeletonCard
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    productsState: UiState<List<Product>>,
    categoriesState: UiState<List<Category>>,
    homepageState: UiState<List<HomepageContent>>,
    inventoryState: UiState<Map<String, InventoryRecord>>,
    allReviewsState: UiState<List<Review>>,
    deliveryZones: List<DeliveryZone>,
    storeSettings: StoreSettings,
    wishlistItems: List<WishlistItem>,
    bagCount: Int,
    unreadNotificationCount: Int = 0,
    userEmail: String,
    currency: StoreCurrency,
    isAdmin: Boolean,
    isNewsletterSubmitting: Boolean,
    newsletterFeedback: Pair<Boolean, String>?,
    onToggleCurrency: () -> Unit,
    onOpenCatalog: (categoryId: String?, filterMode: CatalogFilterMode, focusSearch: Boolean) -> Unit,
    onProductClick: (String) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenBag: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenOrderTracking: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenAdmin: () -> Unit,
    onSubscribeNewsletter: (String) -> Unit,
    onClearNewsletterFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    onQuickView: ((Product) -> Unit)? = null,
) {
    val wishlistIds = remember(wishlistItems) { wishlistItems.map { it.productId }.toSet() }
    val inventoryMap = (inventoryState as? UiState.Success)?.data ?: emptyMap()
    val visibleReviews = (allReviewsState as? UiState.Success)?.data.orEmpty()
    val ratingsByProduct = remember(visibleReviews) {
        visibleReviews.groupBy { it.productId }.mapValues { (_, list) ->
            list.map { it.rating }.average() to list.size
        }
    }

    val homepageItems = (homepageState as? UiState.Success)?.data.orEmpty()
    val heroBanner = homepageItems.firstOrNull { it.sectionType == "HERO" || it.id == "hero_main" }
        ?: homepageItems.firstOrNull()
    val featuredEdit = homepageItems.firstOrNull { it.sectionType == "FEATURED_EDIT" || it.id == "featured_edit" }
    val promoBanners = homepageItems.filter { it.sectionType == "PROMO" }

    val categoriesList = (categoriesState as? UiState.Success)?.data.orEmpty()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showMobileMenu by remember { mutableStateOf(false) }
    var activeInfoTopic by remember { mutableStateOf<BoutiqueInfoTopic?>(null) }

    fun openBySlugOrAll(slug: String, fallbackMode: CatalogFilterMode = CatalogFilterMode.ALL) {
        val catId = categoriesList.firstOrNull { it.slug.equals(slug, ignoreCase = true) }?.id
        onOpenCatalog(catId, fallbackMode, false)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        val productColumns = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        val categoryColumns = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        val isWideScreen = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            AnnouncementBar(messages = storeSettings.announcementMessages)
            if (storeSettings.maintenanceMode && storeSettings.maintenanceMessage.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = storeSettings.maintenanceMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }

            BoutiqueHeaderBar(
                isWideScreen = isWideScreen,
                currency = currency,
                wishlistCount = wishlistItems.size,
                bagCount = bagCount,
                unreadNotificationCount = unreadNotificationCount,
                isAdmin = isAdmin,
                logoUrl = storeSettings.logoUrl,
                whatsappNumber = storeSettings.whatsappNumber,
                onToggleCurrency = onToggleCurrency,
                onNavigateShop = { mode -> onOpenCatalog(null, mode, false) },
                onScrollToCategories = {
                    scope.launch { listState.animateScrollToItem(1) }
                },
                onOpenSearch = { onOpenCatalog(null, CatalogFilterMode.ALL, true) },
                onOpenNotifications = onOpenNotifications,
                onOpenOrderTracking = onOpenOrderTracking,
                onOpenSupport = onOpenSupport,
                onOpenWishlist = onOpenWishlist,
                onOpenBag = onOpenBag,
                onOpenAccount = onOpenAccount,
                onOpenAdmin = onOpenAdmin,
                onOpenInfoTopic = { activeInfoTopic = it },
                onOpenMobileMenu = { showMobileMenu = true },
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 1140.dp)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                // Index 0: Hero Editorial Section
                item(key = "section_hero") {
                    EditorialHeroSection(
                        hero = heroBanner,
                        isWideLayout = isWideScreen,
                        onShopNow = { onOpenCatalog(null, CatalogFilterMode.ALL, false) },
                        onExploreCollection = {
                            scope.launch {
                                val targetIndex = (2 + (categoriesList.chunked(categoryColumns).size.coerceAtLeast(1)) + 1)
                                    .coerceAtMost(listState.layoutInfo.totalItemsCount - 1)
                                    .coerceAtLeast(0)
                                listState.animateScrollToItem(targetIndex)
                            }
                        },
                    )
                }

                // Index 1: Shop by Category Header
                item(key = "section_categories_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_by_category_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "SHOP BY CATEGORY",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ZeecrownDeepPurple,
                            )
                            Text(
                                text = "Explore handcrafted footwear, bags, Adire, Kampala, Abayas & beauty.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = { onOpenCatalog(null, CatalogFilterMode.ALL, false) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Text("VIEW ALL")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                when (categoriesState) {
                    is UiState.Loading -> {
                        item(key = "categories_loading") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                repeat(categoryColumns) { EditorialSkeletonCard(Modifier.weight(1f)) }
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "categories_error") { BoutiqueErrorBanner(categoriesState.message) }
                    }
                    is UiState.Success -> {
                        if (categoriesState.data.isEmpty()) {
                            item(key = "categories_empty") {
                                BoutiqueEmptyView(
                                    title = "Categories Pending Publication",
                                    subtitle = if (isAdmin) {
                                        "Initialize the 8 ZEECROWN STORE categories from the Store Admin Dashboard."
                                    } else {
                                        "Our fashion departments are being curated right now."
                                    },
                                    actionLabel = if (isAdmin) "Open Admin Dashboard" else null,
                                    onAction = if (isAdmin) onOpenAdmin else null,
                                )
                            }
                        } else {
                            val topCategories = categoriesState.data
                                .filter { it.parentCategoryId.isBlank() }
                                .ifEmpty { categoriesState.data }
                            val catRows = topCategories.chunked(categoryColumns)
                            items(catRows) { rowCats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    rowCats.forEach { cat ->
                                        BoutiqueCategoryTile(
                                            category = cat,
                                            onClick = { onOpenCatalog(cat.id, CatalogFilterMode.ALL, false) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat(categoryColumns - rowCats.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // New Arrivals Section
                item(key = "section_new_arrivals_header") {
                    SectionTitleRow(
                        title = "NEW ARRIVALS",
                        subtitle = "Freshly added boutique pieces synced live.",
                        ctaLabel = "VIEW ALL",
                        onCtaClick = { onOpenCatalog(null, CatalogFilterMode.NEW_ARRIVALS, false) },
                    )
                }

                when (productsState) {
                    is UiState.Loading -> {
                        item(key = "new_arrivals_loading") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                repeat(productColumns) { EditorialSkeletonCard(Modifier.weight(1f)) }
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "new_arrivals_error") { BoutiqueErrorBanner(productsState.message) }
                    }
                    is UiState.Success -> {
                        val arrivals = productsState.data.filter { it.newArrival }.take(productColumns * 2)
                        if (arrivals.isEmpty()) {
                            item(key = "new_arrivals_empty") {
                                BoutiqueEmptyView(
                                    title = "No New Arrivals Yet",
                                    subtitle = if (isAdmin) {
                                        "Mark items as New Arrivals or tap Seed Store in Admin Dashboard."
                                    } else {
                                        "Fresh drops are arriving shortly — check back soon."
                                    },
                                    actionLabel = if (isAdmin) "Open Admin Setup" else "View All Pieces",
                                    onAction = if (isAdmin) onOpenAdmin else {
                                        { onOpenCatalog(null, CatalogFilterMode.ALL, false) }
                                    },
                                )
                            }
                        } else {
                            val arrivalRows = arrivals.chunked(productColumns)
                            items(arrivalRows) { rowProds ->
                                ProductGridRow(
                                    products = rowProds,
                                    columns = productColumns,
                                    currency = currency,
                                    wishlistIds = wishlistIds,
                                    inventoryMap = inventoryMap,
                                    ratingsByProduct = ratingsByProduct,
                                    onProductClick = onProductClick,
                                    onToggleWishlist = onToggleWishlist,
                                    onQuickAdd = onQuickAdd,
                                    onQuickView = onQuickView,
                                )
                            }
                        }
                    }
                }

                // Featured Collection: THE ZEECROWN EDIT
                item(key = "section_zeecrown_edit") {
                    TheZeecrownEditSection(
                        content = featuredEdit,
                        isWideLayout = isWideScreen,
                        onShopCollection = {
                            val targetSlug = featuredEdit?.targetCategorySlug.orEmpty()
                            if (targetSlug.isNotBlank()) openBySlugOrAll(targetSlug)
                            else onOpenCatalog(null, CatalogFilterMode.ALL, false)
                        },
                    )
                }

                // Best Sellers Section
                if (productsState is UiState.Success) {
                    val hasRealSales = productsState.data.any { it.soldCount > 0L }
                    val bestSellers = if (hasRealSales) {
                        productsState.data.filter { it.soldCount > 0L }
                            .sortedByDescending { it.soldCount }
                            .take(productColumns * 2)
                    } else {
                        productsState.data.filter { it.featured }.take(productColumns * 2)
                    }

                    item(key = "section_best_sellers_header") {
                        SectionTitleRow(
                            title = "OUR BEST SELLERS",
                            subtitle = if (hasRealSales) {
                                "Ranked by verified customer orders."
                            } else {
                                "Featured selections curated by the Zeecrown Atelier."
                            },
                            ctaLabel = "VIEW ALL",
                            onCtaClick = { onOpenCatalog(null, CatalogFilterMode.BEST_SELLERS, false) },
                        )
                    }

                    if (bestSellers.isEmpty()) {
                        item(key = "best_sellers_empty") {
                            BoutiqueEmptyView(
                                title = "Curated Highlights Pending",
                                subtitle = "Featured pieces and verified top-selling styles will display here once published.",
                            )
                        }
                    } else {
                        val bestRows = bestSellers.chunked(productColumns)
                        items(bestRows) { rowProds ->
                            ProductGridRow(
                                products = rowProds,
                                columns = productColumns,
                                currency = currency,
                                wishlistIds = wishlistIds,
                                inventoryMap = inventoryMap,
                                ratingsByProduct = ratingsByProduct,
                                onProductClick = onProductClick,
                                onToggleWishlist = onToggleWishlist,
                                onQuickAdd = onQuickAdd,
                                onQuickView = onQuickView,
                            )
                        }
                    }
                }

                // Promotional Banners
                item(key = "section_promos") {
                    PromotionalBannersSection(
                        promos = promoBanners,
                        isAdmin = isAdmin,
                        onOpenPromo = { promo ->
                            if (promo.targetCategorySlug.isNotBlank()) {
                                openBySlugOrAll(promo.targetCategorySlug)
                            } else {
                                onOpenCatalog(null, CatalogFilterMode.ALL, false)
                            }
                        },
                        onOpenAdmin = onOpenAdmin,
                    )
                }

                // Why Shop With Zeecrown
                item(key = "section_trust") {
                    WhyShopWithZeecrownSection()
                }

                // Customer Testimonials (Genuine Visible Reviews Only)
                item(key = "section_testimonials") {
                    CustomerTestimonialsSection(
                        reviewsState = allReviewsState,
                        onOpenProduct = onProductClick,
                    )
                }

                // Newsletter Section
                item(key = "section_newsletter") {
                    NewsletterSignupSection(
                        defaultEmail = userEmail,
                        isSubmitting = isNewsletterSubmitting,
                        feedback = newsletterFeedback,
                        onSubscribe = onSubscribeNewsletter,
                        onClearFeedback = onClearNewsletterFeedback,
                    )
                }

                // Social Media Section
                item(key = "section_social") {
                    SocialDiscoverySection(
                        storeSettings = storeSettings,
                        isAdmin = isAdmin,
                        onOpenAdmin = onOpenAdmin,
                    )
                }

                // Professional Footer
                item(key = "section_footer") {
                    BoutiqueFooterSection(
                        storeSettings = storeSettings,
                        onNavigateShop = { mode -> onOpenCatalog(null, mode, false) },
                        onScrollToCategories = { scope.launch { listState.animateScrollToItem(1) } },
                        onTrackOrders = onOpenOrderTracking,
                        onOpenSupport = onOpenSupport,
                        onOpenInfoTopic = { activeInfoTopic = it },
                    )
                }
            }
        }
    }

    if (showMobileMenu) {
        BoutiqueMobileMenuSheet(
            currency = currency,
            unreadNotificationCount = unreadNotificationCount,
            isAdmin = isAdmin,
            whatsappNumber = storeSettings.whatsappNumber,
            onDismiss = { showMobileMenu = false },
            onToggleCurrency = onToggleCurrency,
            onNavigateShop = { mode -> onOpenCatalog(null, mode, false) },
            onScrollToCategories = { scope.launch { listState.animateScrollToItem(1) } },
            onOpenAccount = onOpenAccount,
            onOpenOrderTracking = onOpenOrderTracking,
            onOpenNotifications = onOpenNotifications,
            onOpenSupport = onOpenSupport,
            onOpenAdmin = onOpenAdmin,
            onOpenInfoTopic = { activeInfoTopic = it },
        )
    }

    activeInfoTopic?.let { topic ->
        BoutiqueInfoModalSheet(
            topic = topic,
            storeSettings = storeSettings,
            deliveryZones = deliveryZones,
            onDismiss = { activeInfoTopic = null },
        )
    }
}

@Composable
private fun SectionTitleRow(
    title: String,
    subtitle: String,
    ctaLabel: String,
    onCtaClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = ZeecrownDeepPurple,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ZeecrownGoldInk,
            )
        }
        TextButton(
            onClick = onCtaClick,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Text(ctaLabel)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ProductGridRow(
    products: List<Product>,
    columns: Int,
    currency: StoreCurrency,
    wishlistIds: Set<String>,
    inventoryMap: Map<String, InventoryRecord>,
    ratingsByProduct: Map<String, Pair<Double, Int>>,
    onProductClick: (String) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onQuickView: ((Product) -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        products.forEach { product ->
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
                onQuickView = if (onQuickView != null) { { onQuickView(product) } } else null,
                modifier = Modifier.weight(1f),
            )
        }
        repeat(columns - products.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun BoutiqueCategoryTile(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("category_card_${category.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column {
            ZeecrownProductImage(
                imageUrl = "drawable://${category.imageKey.ifBlank { "img_cat_adire_abaya" }}",
                contentDescription = category.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = ZeecrownDeepPurple,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (category.description.isNotBlank()) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
