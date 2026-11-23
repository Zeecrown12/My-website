package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Product
import com.example.data.model.StoreCurrency
import com.example.ui.theme.ZeecrownChampagneGold
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import java.text.NumberFormat
import java.util.Locale

fun formatBoutiquePrice(
    priceNgn: Double,
    priceUsd: Double,
    currency: StoreCurrency,
): String {
    return if (currency == StoreCurrency.USD) {
        val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
        "$${fmt.format(priceUsd)}"
    } else {
        val fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-NG")).apply { maximumFractionDigits = 0 }
        "₦${fmt.format(priceNgn)}"
    }
}

fun formatNgnAmount(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-NG")).apply { maximumFractionDigits = 0 }
    return "₦${fmt.format(amount)}"
}

@Composable
fun ZeecrownBrandLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    logoUrl: String = "",
) {
    Surface(
        modifier = modifier.testTag("zeecrown_brand_logo"),
        shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        color = ZeecrownPureWhite,
        border = BorderStroke(1.dp, ZeecrownMetallicGold.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 16.dp,
                vertical = if (compact) 6.dp else 10.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            if (logoUrl.isNotBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = "ZEECROWN Official Logo",
                    modifier = Modifier
                        .size(if (compact) 28.dp else 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.zeecrown_logo),
                    contentDescription = "ZEECROWN Official Logo",
                    modifier = Modifier
                        .size(if (compact) 28.dp else 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentScale = ContentScale.Fit,
                )
            }
            Column {
                Text(
                    text = "ZEECROWN STORE",
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!compact) {
                    Text(
                        text = "LUXURY FASHION & LIFESTYLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZeecrownGoldInk,
                    )
                }
            }
        }
    }
}

@Composable
fun ZeecrownProductImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val resolvedAlt = contentDescription.ifBlank { "ZEECROWN Fashion Piece" }
    val cleanUrl = imageUrl.removePrefix("drawable://")
    val drawableRes = when (cleanUrl) {
        "img_hero_editorial" -> R.drawable.img_hero_editorial
        "img_cat_adire_abaya" -> R.drawable.img_cat_adire_abaya
        "img_cat_bags_footwear" -> R.drawable.img_cat_bags_footwear
        "img_cat_jewellery_beauty" -> R.drawable.img_cat_jewellery_beauty
        "img_zeecrown_edit_campaign" -> R.drawable.img_zeecrown_edit_campaign
        else -> null
    }

    if (drawableRes != null) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = resolvedAlt,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else if (imageUrl.isNotBlank()) {
        val request = ImageRequest.Builder(context)
            .data(cleanUrl)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        AsyncImage(
            model = request,
            contentDescription = resolvedAlt,
            modifier = modifier.background(ZeecrownLightLilac),
            contentScale = contentScale,
            placeholder = painterResource(id = R.drawable.img_cat_adire_abaya),
            error = painterResource(id = R.drawable.img_cat_adire_abaya),
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_cat_adire_abaya),
            contentDescription = resolvedAlt,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

@Composable
fun CurrencyToggleChip(
    currency: StoreCurrency,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onToggle,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("currency_toggle_chip"),
        shape = RoundedCornerShape(20.dp),
        color = ZeecrownLightLilac,
        border = BorderStroke(1.dp, ZeecrownMetallicGold.copy(alpha = 0.7f)),
    ) {
        Text(
            text = "${currency.code} ${currency.symbol}",
            style = MaterialTheme.typography.labelMedium,
            color = ZeecrownDeepPurple,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
fun EditorialProductCard(
    product: Product,
    currency: StoreCurrency,
    wishlisted: Boolean,
    inStock: Boolean,
    onProductClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    verifiedRating: Double? = null,
    reviewCount: Int = 0,
    onQuickView: (() -> Unit)? = null,
) {
    val discountPct = product.discountPercent()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clickable(onClick = onProductClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.86f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(ZeecrownLightLilac),
            ) {
                ZeecrownProductImage(
                    imageUrl = product.mainImageUrl.ifBlank { product.imageUrls.firstOrNull().orEmpty() },
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    IconButton(
                        onClick = onToggleWishlist,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .background(Color.White.copy(alpha = 0.90f), CircleShape)
                            .size(38.dp)
                            .testTag("wishlist_toggle_${product.id}"),
                    ) {
                        Icon(
                            imageVector = if (wishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (wishlisted) "Remove from wishlist" else "Save to wishlist",
                            tint = if (wishlisted) MaterialTheme.colorScheme.primary else ZeecrownDeepPurple,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    if (onQuickView != null) {
                        IconButton(
                            onClick = onQuickView,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(Color.White.copy(alpha = 0.90f), CircleShape)
                                .size(38.dp)
                                .testTag("quick_view_${product.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = "Quick view ${product.title}",
                                tint = ZeecrownDeepPurple,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (discountPct != null && currency == StoreCurrency.NGN) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ZeecrownDeepPurple,
                        ) {
                            Text(
                                text = "-$discountPct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            )
                        }
                    } else if (product.newArrival) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ZeecrownSoftLavender.copy(alpha = 0.95f),
                        ) {
                            Text(
                                text = "NEW IN",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZeecrownDeepPurple,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            )
                        }
                    } else if (product.featured) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ZeecrownChampagneGold.copy(alpha = 0.92f),
                        ) {
                            Text(
                                text = "ATELIER",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZeecrownDeepPurple,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = product.brand.ifBlank { "ZEECROWN" }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ZeecrownGoldInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (inStock) "In Stock" else "Sold Out",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (inStock) ZeecrownRoyalPurple else MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (verifiedRating != null && reviewCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = ZeecrownMetallicGold,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f (%d)", verifiedRating, reviewCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = formatBoutiquePrice(product.priceNgn, product.priceUsd, currency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (currency == StoreCurrency.NGN && product.compareAtPriceNgn != null && product.compareAtPriceNgn > product.priceNgn) {
                        Text(
                            text = formatNgnAmount(product.compareAtPriceNgn),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                OutlinedButton(
                    onClick = onQuickAdd,
                    enabled = inStock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("quick_add_${product.id}"),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                    border = BorderStroke(1.dp, ZeecrownRoyalPurple.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ZeecrownRoyalPurple,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (inStock) "Add to Bag" else "Sold Out",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun EditorialSkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.86f)
                    .background(ZeecrownLightLilac)
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ZeecrownElegantSilver)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ZeecrownSoftLavender)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZeecrownLightLilac)
                )
            }
        }
    }
}

@Composable
fun BoutiqueLoadingView(
    message: String = "Curating the Zeecrown showroom...",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.5.dp,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun BoutiqueEmptyView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ZeecrownSoftLavender),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun BoutiqueErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
