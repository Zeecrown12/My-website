package com.example.ui.storefront

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.BoutiqueContactHelper
import com.example.data.model.CatalogQueryEngine
import com.example.data.model.Category
import com.example.data.model.DeliveryZone
import com.example.data.model.InventoryRecord
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.SeoUtils
import com.example.data.model.SizeGuide
import com.example.data.model.SizeGuideResolver
import com.example.data.model.SocialSharePlatform
import com.example.data.model.SocialShareTarget
import com.example.data.model.StoreCurrency
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.SizeGuideModalSheet
import com.example.ui.components.SocialShareSheet
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.components.formatBoutiquePrice
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownObsidianInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    product: Product?,
    allProducts: List<Product>,
    categories: List<Category>,
    inventoryMap: Map<String, InventoryRecord>,
    wishlistIds: Set<String>,
    ratingsByProduct: Map<String, Pair<Double, Int>>,
    currency: StoreCurrency,
    wishlisted: Boolean,
    stockQuantity: Long,
    resolvedSku: String,
    deliveryZones: List<DeliveryZone>,
    reviews: List<Review>,
    isSubmittingReview: Boolean,
    sizeGuides: List<SizeGuide> = emptyList(),
    whatsappNumber: String = BoutiqueContactHelper.OFFICIAL_WHATSAPP_NUMBER_DISPLAY,
    publicSiteBaseUrl: String = "https://zeecrownstore.com",
    onShareTracked: (SocialSharePlatform) -> Unit = {},
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onQuickView: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onAddToBag: (Product, String, Long) -> Unit,
    onBuyNow: (Product, String, Long) -> Unit,
    onSubmitReview: (rating: Long, comment: String, onSuccess: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    if (product == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            BoutiqueEmptyView(
                title = "Piece Unavailable",
                subtitle = "This item is no longer active in the showroom.",
                actionLabel = "Back to Collection",
                onAction = onBack,
            )
        }
        return
    }

    val context = LocalContext.current
    val images = remember(product.id, product.imageUrls, product.mainImageUrl) {
        product.imageUrls.ifEmpty {
            listOf(product.mainImageUrl.ifBlank { "drawable://img_cat_adire_abaya" })
        }
    }
    var selectedImageIdx by remember(product.id) { mutableIntStateOf(0) }
    var showFullscreenGallery by remember(product.id) { mutableStateOf(false) }
    var showShareSheet by remember(product.id) { mutableStateOf(false) }
    var showSizeGuideSheet by remember(product.id) { mutableStateOf(false) }

    var selectedSize by remember(product.id) {
        mutableStateOf(product.sizes.firstOrNull() ?: "One Size")
    }
    var selectedColor by remember(product.id) {
        mutableStateOf(product.colors.firstOrNull() ?: "Standard")
    }
    var quantity by remember(product.id) { mutableLongStateOf(1L) }

    val variantAvailable = product.isVariantAvailable(selectedSize, selectedColor)
    val maxQty = stockQuantity.coerceAtLeast(1L).coerceAtMost(50L)
    val canPurchase = stockQuantity > 0L && variantAvailable

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val currentCategory = categoryMap[product.categoryId]
    val parentCategory = currentCategory?.parentCategoryId
        ?.takeIf { it.isNotBlank() }
        ?.let { categoryMap[it] }
    val displaySku = resolvedSku.ifBlank { product.sku.ifBlank { "ZC-${product.id.take(6).uppercase()}" } }
    val discountPct = product.discountPercent()
    val activeSizeGuide = remember(product.id, product.sizeGuideId, product.categoryId, categories, sizeGuides) {
        SizeGuideResolver.resolveForProduct(product = product, categories = categories, guides = sizeGuides)
    }

    val relatedProducts = remember(product.id, allProducts, categories) {
        CatalogQueryEngine.findRelatedProducts(
            currentProduct = product,
            allProducts = allProducts,
            categories = categories,
            maxCount = 6,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            // Section A: Multi-image Gallery + Zoom Viewer Trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(ZeecrownLightLilac)
                    .clickable { showFullscreenGallery = true },
            ) {
                ZeecrownProductImage(
                    imageUrl = images.getOrElse(selectedImageIdx) { images.first() },
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = ZeecrownPureWhite.copy(alpha = 0.94f),
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("product_detail_back"),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = ZeecrownDeepPurple)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = ZeecrownPureWhite.copy(alpha = 0.94f),
                        ) {
                            IconButton(
                                onClick = { showShareSheet = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("product_detail_share"),
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share product", tint = ZeecrownDeepPurple)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = ZeecrownPureWhite.copy(alpha = 0.94f),
                        ) {
                            IconButton(
                                onClick = { onToggleWishlist(product) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("product_detail_wishlist"),
                            ) {
                                Icon(
                                    imageVector = if (wishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (wishlisted) "Remove from wishlist" else "Save to wishlist",
                                    tint = if (wishlisted) MaterialTheme.colorScheme.primary else ZeecrownDeepPurple,
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = { showFullscreenGallery = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("product_detail_zoom_button"),
                    shape = RoundedCornerShape(20.dp),
                    color = ZeecrownPureWhite.copy(alpha = 0.94f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom gallery", tint = ZeecrownDeepPurple, modifier = Modifier.size(16.dp))
                        Text("Zoom (${selectedImageIdx + 1}/${images.size})", style = MaterialTheme.typography.labelSmall, color = ZeecrownDeepPurple)
                    }
                }
            }

            if (images.size > 1) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(images) { idx, img ->
                        Surface(
                            onClick = { selectedImageIdx = idx },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(
                                width = if (idx == selectedImageIdx) 2.dp else 1.dp,
                                color = if (idx == selectedImageIdx) MaterialTheme.colorScheme.primary else ZeecrownElegantSilver,
                            ),
                        ) {
                            ZeecrownProductImage(
                                imageUrl = img,
                                contentDescription = "${product.title} photo ${idx + 1}",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.small),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShopBreadcrumbBar(
                    selectedCategory = parentCategory ?: currentCategory,
                    selectedSubcategory = if (parentCategory != null) currentCategory else null,
                    onNavigateHome = onNavigateHome,
                    onResetToShop = onBack,
                    onSelectCategory = onOpenCategory,
                )

                // Section B: Product Title, Price, Discount, SKU, Category & Stock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = product.brand.ifBlank { "ZEECROWN ATELIER" }.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = ZeecrownGoldInk,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (stockQuantity > 0L) ZeecrownSoftLavender else MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = if (stockQuantity > 0L) "IN STOCK ($stockQuantity LEFT)" else "SOLD OUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stockQuantity > 0L) ZeecrownDeepPurple else MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("product_detail_stock_badge"),
                        )
                    }
                }

                Text(
                    text = product.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = formatBoutiquePrice(product.priceNgn, product.priceUsd, currency),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (currency == StoreCurrency.NGN &&
                        product.compareAtPriceNgn != null &&
                        product.compareAtPriceNgn > product.priceNgn
                    ) {
                        Text(
                            text = formatNgnAmount(product.compareAtPriceNgn),
                            style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (discountPct != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ZeecrownRoyalPurple,
                            ) {
                                Text(
                                    text = "SAVE $discountPct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZeecrownPureWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ZeecrownSoftLavender,
                    ) {
                        Text(
                            text = "SKU: $displaySku",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZeecrownDeepPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    if (currentCategory != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Text(
                                text = currentCategory.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = ZeecrownDeepPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                if (product.summary.isNotBlank()) {
                    Text(
                        text = product.summary,
                        style = MaterialTheme.typography.titleSmall,
                        color = ZeecrownDeepPurple,
                    )
                }

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider(color = ZeecrownElegantSilver)

                // Section C: Variation Selection (strictly existing sizes/colors + unavailableVariants check)
                if (product.sizes.isNotEmpty() || activeSizeGuide != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (product.sizes.isNotEmpty()) "Select Size / Length" else "Fit & Sizing",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (activeSizeGuide != null) {
                            OutlinedButton(
                                onClick = { showSizeGuideSheet = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("product_detail_size_guide_btn"),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Straighten,
                                    contentDescription = "Open Size Guide",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Size Guide")
                            }
                        }
                    }
                    if (product.sizes.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            product.sizes.forEach { size ->
                                val sizeOk = product.isVariantAvailable(size, selectedColor)
                                FilterChip(
                                    selected = selectedSize == size,
                                    onClick = { selectedSize = size },
                                    enabled = sizeOk || selectedSize == size,
                                    label = { Text(if (sizeOk) size else "$size (Unavailable)") },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("size_chip_$size"),
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZeecrownSoftLavender),
                                )
                            }
                        }
                    }
                }

                if (product.colors.isNotEmpty()) {
                    Text("Select Shade", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        product.colors.forEach { color ->
                            val colorOk = product.isVariantAvailable(selectedSize, color)
                            FilterChip(
                                selected = selectedColor == color,
                                onClick = { selectedColor = color },
                                enabled = colorOk || selectedColor == color,
                                label = { Text(if (colorOk) color else "$color (Unavailable)") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("color_chip_$color"),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZeecrownSoftLavender),
                            )
                        }
                    }
                }

                if (!variantAvailable) {
                    Text(
                        text = "The combination $selectedSize • $selectedColor is currently unavailable. Please choose another option.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                // Section D: Quantity Selector validated against stock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Quantity", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (stockQuantity > 0L) "Max $maxQty available" else "Out of stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1L) quantity-- },
                            enabled = quantity > 1L,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(ZeecrownSoftLavender, CircleShape)
                                .testTag("detail_qty_minus"),
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity", tint = ZeecrownDeepPurple)
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .testTag("detail_qty_value"),
                        )
                        IconButton(
                            onClick = { if (quantity < maxQty) quantity++ },
                            enabled = stockQuantity > 0L && quantity < maxQty,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(ZeecrownSoftLavender, CircleShape)
                                .testTag("detail_qty_plus"),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Increase quantity", tint = ZeecrownDeepPurple)
                        }
                    }
                }

                // Section E: Purchase Actions (Add to Bag, Buy Now, Wishlist, Share)
                val selectedVariantLabel = listOf(selectedSize, selectedColor)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                    .ifBlank { "Standard" }

                Button(
                    onClick = { onAddToBag(product, selectedVariantLabel, quantity) },
                    enabled = canPurchase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("detail_add_to_bag_button"),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZeecrownRoyalPurple,
                        contentColor = ZeecrownPureWhite,
                    ),
                ) {
                    Icon(Icons.Outlined.ShoppingBag, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = when {
                            stockQuantity <= 0L -> "Currently Out of Stock"
                            !variantAvailable -> "Selected Variation Unavailable"
                            else -> "Add $quantity to Shopping Bag"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onBuyNow(product, selectedVariantLabel, quantity) },
                        enabled = canPurchase,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("detail_buy_now_button"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeecrownMetallicGold,
                            contentColor = ZeecrownObsidianInk,
                        ),
                    ) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buy Now", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { onToggleWishlist(product) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .minimumInteractiveComponentSize(),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = if (wishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (wishlisted) "Saved" else "Wishlist")
                    }
                }

                // Section F: Additional Information Tabs, Reviews & Related Products
                ProductAdditionalInfoSection(
                    product = product,
                    deliveryZones = deliveryZones,
                )

                HorizontalDivider(color = ZeecrownElegantSilver)

                ProductReviewsSection(
                    reviews = reviews,
                    isSubmittingReview = isSubmittingReview,
                    onSubmitReview = onSubmitReview,
                )

                HorizontalDivider(color = ZeecrownElegantSilver)

                RelatedProductsRow(
                    relatedProducts = relatedProducts,
                    currency = currency,
                    wishlistIds = wishlistIds,
                    inventoryMap = inventoryMap,
                    ratingsByProduct = ratingsByProduct,
                    onProductClick = onProductClick,
                    onQuickView = onQuickView,
                    onToggleWishlist = onToggleWishlist,
                    onQuickAdd = { prod ->
                        val v = listOfNotNull(prod.sizes.firstOrNull(), prod.colors.firstOrNull())
                            .joinToString(" • ")
                            .ifBlank { "Standard" }
                        onAddToBag(prod, v, 1L)
                    },
                )
            }
        }
    }

    if (showFullscreenGallery) {
        ProductFullscreenGalleryDialog(
            images = images,
            initialIndex = selectedImageIdx,
            productTitle = product.title,
            onDismiss = { showFullscreenGallery = false },
        )
    }

    if (showShareSheet) {
        SocialShareSheet(
            target = SocialShareTarget(
                contentType = "product",
                itemId = product.id,
                title = product.seoTitle.ifBlank { product.title },
                subtitle = product.seoDescription.ifBlank { product.description },
                canonicalUrl = SeoUtils.buildCanonicalProductUrl(publicSiteBaseUrl, product),
                imageUrl = product.socialImageUrl.ifBlank { images.firstOrNull().orEmpty() },
                priceFormatted = formatBoutiquePrice(product.priceNgn, product.priceUsd, currency),
            ),
            onDismiss = { showShareSheet = false },
            onSharePlatform = onShareTracked,
        )
    }

    if (showSizeGuideSheet && activeSizeGuide != null) {
        SizeGuideModalSheet(
            guide = activeSizeGuide,
            whatsappNumber = whatsappNumber,
            onDismiss = { showSizeGuideSheet = false },
        )
    }
}

@Composable
private fun ProductReviewsSection(
    reviews: List<Review>,
    isSubmittingReview: Boolean,
    onSubmitReview: (rating: Long, comment: String, onSuccess: () -> Unit) -> Unit,
) {
    var ratingInput by remember { mutableIntStateOf(5) }
    var commentInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Client Reviews", style = MaterialTheme.typography.headlineSmall)
            if (reviews.isNotEmpty()) {
                val avg = reviews.map { it.rating }.average()
                Surface(shape = RoundedCornerShape(8.dp), color = ZeecrownSoftLavender) {
                    Text(
                        text = String.format(Locale.US, "%.1f ★ (%d)", avg, reviews.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = ZeecrownDeepPurple,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (reviews.isEmpty()) {
            Text(
                text = "No client reviews yet. Share your styling notes below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            reviews.forEach { review ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(review.authorName, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                val stars = review.rating.toInt().coerceIn(1, 5)
                                repeat(stars) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = ZeecrownMetallicGold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = review.comment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Write a Review", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { ratingInput = star },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("review_star_$star"),
                        ) {
                            Icon(
                                imageVector = if (star <= ratingInput) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$star out of 5 stars",
                                tint = ZeecrownMetallicGold,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    label = { Text("Share fit, fabric & craftsmanship notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_comment_input"),
                    minLines = 2,
                )
                Button(
                    onClick = {
                        onSubmitReview(ratingInput.toLong(), commentInput) {
                            commentInput = ""
                        }
                    },
                    enabled = !isSubmittingReview && commentInput.trim().isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("submit_review_button"),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(if (isSubmittingReview) "Publishing..." else "Publish Review")
                }
            }
        }
    }
}
