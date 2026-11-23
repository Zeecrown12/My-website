package com.example.ui.storefront

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import com.example.data.model.DeliveryZone
import com.example.data.model.InventoryRecord
import com.example.data.model.Product
import com.example.data.model.SeoUtils
import com.example.data.model.StoreCurrency
import com.example.ui.components.EditorialProductCard
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownChampagneGold
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownPureWhite

fun shareProductIntent(
    context: Context,
    product: Product,
    baseUrl: String = "https://zeecrownstore.com",
) {
    val slugOrId = product.slug.ifBlank { product.id }
    val shareUrl = SeoUtils.buildCanonicalProductUrl(baseUrl, product)
    val appLink = "zeecrown://product/$slugOrId"
    val message = buildString {
        append("Discover ${product.title} at ZEECROWN STORE\n")
        append("Price: ${formatNgnAmount(product.priceNgn)}\n")
        append("Shop here: $shareUrl\n")
        append("Open in Zeecrown App: $appLink")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${product.title} — ZEECROWN STORE")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    val chooser = Intent.createChooser(sendIntent, "Share ${product.title}")
    context.startActivity(chooser)
}

@Composable
fun ProductFullscreenGalleryDialog(
    images: List<String>,
    initialIndex: Int,
    productTitle: String,
    onDismiss: () -> Unit,
) {
    var activeIndex by remember(initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0)))
    }
    var scale by remember(activeIndex) { mutableFloatStateOf(1f) }
    var offset by remember(activeIndex) { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .testTag("fullscreen_gallery_dialog"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp)
                    .pointerInput(activeIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.2f
                                }
                            }
                        )
                    }
                    .pointerInput(activeIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 3.5f)
                            scale = newScale
                            offset = if (newScale <= 1.02f) {
                                Offset.Zero
                            } else {
                                Offset(
                                    x = (offset.x + pan.x).coerceIn(-450f, 450f),
                                    y = (offset.y + pan.y).coerceIn(-450f, 450f),
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                ZeecrownProductImage(
                    imageUrl = images.getOrElse(activeIndex) { images.first() },
                    contentDescription = "$productTitle zoom view",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = "${activeIndex + 1} / ${images.size} · Pinch or Double-Tap to Zoom",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("fullscreen_gallery_close"),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close zoom view", tint = Color.White)
                }
            }

            if (images.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(images) { idx, img ->
                        Surface(
                            onClick = { activeIndex = idx },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(
                                width = if (idx == activeIndex) 2.dp else 1.dp,
                                color = if (idx == activeIndex) ZeecrownMetallicGold else Color.White.copy(alpha = 0.4f),
                            ),
                        ) {
                            ZeecrownProductImage(
                                imageUrl = img,
                                contentDescription = "$productTitle thumbnail ${idx + 1}",
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(MaterialTheme.shapes.small),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductAdditionalInfoSection(
    product: Product,
    deliveryZones: List<DeliveryZone>,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_additional_info"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ZeecrownPureWhite,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Details & Specs") },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Delivery") },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Order Policy") },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (selectedTab) {
                    0 -> {
                        if (product.summary.isNotBlank()) {
                            Text(
                                text = product.summary,
                                style = MaterialTheme.typography.titleSmall,
                                color = ZeecrownDeepPurple,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (product.features.isNotEmpty()) {
                            HorizontalDivider(color = ZeecrownElegantSilver)
                            Text("Key Features", style = MaterialTheme.typography.labelLarge, color = ZeecrownDeepPurple)
                            product.features.forEach { feat ->
                                Text("• $feat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        if (product.fashionStyle.isNotBlank() || product.suitableOccasions.isNotEmpty()) {
                            HorizontalDivider(color = ZeecrownElegantSilver)
                            if (product.fashionStyle.isNotBlank()) {
                                Text("Style: ${product.fashionStyle}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            if (product.suitableOccasions.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    product.suitableOccasions.forEach { occ ->
                                        AssistChip(onClick = {}, label = { Text(occ, style = MaterialTheme.typography.labelSmall) })
                                    }
                                }
                            }
                        }
                        if (product.attributes.isNotEmpty()) {
                            HorizontalDivider(color = ZeecrownElegantSilver)
                            product.attributes.entries.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ZeecrownDeepPurple,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        if (deliveryZones.isEmpty()) {
                            Text(
                                text = "Nationwide delivery across Nigeria & international DHL express available at checkout.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            deliveryZones.forEachIndexed { idx, zone ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalShipping,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Column {
                                            Text(zone.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${zone.estimatedDays} · ${zone.states.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ZeecrownLightLilac,
                                    ) {
                                        Text(
                                            text = formatNgnAmount(zone.feeNgn),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ZeecrownDeepPurple,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                                if (idx < deliveryZones.lastIndex) {
                                    HorizontalDivider(color = ZeecrownElegantSilver)
                                }
                            }
                        }
                    }

                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = ZeecrownMetallicGold,
                            )
                            Text(
                                text = "Authenticity Inspection & All-Sales-Final Policy",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Text(
                            text = "Every ZEECROWN STORE piece is quality-checked before dispatch. Please note that ZEECROWN STORE currently does not accept customer returns or refunds once an order is fulfilled. Kindly verify your size using our Size Guide or message our WhatsApp concierge (+234 816 989 8763) before ordering. If you ever receive a wrong or damaged item, contact Customer Support immediately with your ZC- order number.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RelatedProductsRow(
    relatedProducts: List<Product>,
    currency: StoreCurrency,
    wishlistIds: Set<String>,
    inventoryMap: Map<String, InventoryRecord>,
    ratingsByProduct: Map<String, Pair<Double, Int>>,
    onProductClick: (String) -> Unit,
    onQuickView: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
) {
    if (relatedProducts.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("related_products_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "You May Also Adore",
            style = MaterialTheme.typography.headlineSmall,
            color = ZeecrownDeepPurple,
        )
        Text(
            text = "Complementary silhouettes & finishing touches from our collection.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(relatedProducts, key = { it.id }) { prod ->
                val stock = inventoryMap[prod.id]?.stockQuantity ?: 10L
                val ratingPair = ratingsByProduct[prod.id]
                EditorialProductCard(
                    product = prod,
                    currency = currency,
                    wishlisted = prod.id in wishlistIds,
                    inStock = stock > 0L,
                    onProductClick = { onProductClick(prod.id) },
                    onToggleWishlist = { onToggleWishlist(prod) },
                    onQuickAdd = { onQuickAdd(prod) },
                    verifiedRating = ratingPair?.first,
                    reviewCount = ratingPair?.second ?: 0,
                    onQuickView = { onQuickView(prod) },
                    modifier = Modifier.width(196.dp),
                )
            }
        }
    }
}
