package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.data.model.StoreCurrency
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductQuickViewDialog(
    product: Product,
    currency: StoreCurrency,
    wishlisted: Boolean,
    stockQuantity: Long,
    onDismiss: () -> Unit,
    onToggleWishlist: () -> Unit,
    onAddToBag: (variantLabel: String, quantity: Long) -> Unit,
    onViewFullDetails: () -> Unit,
) {
    val defaultSize = product.sizes.firstOrNull() ?: "One Size"
    val defaultColor = product.colors.firstOrNull() ?: "Standard"
    var selectedSize by remember(product.id) { mutableStateOf(defaultSize) }
    var selectedColor by remember(product.id) { mutableStateOf(defaultColor) }
    var quantity by remember(product.id) { mutableLongStateOf(1L) }

    val variantAvailable = product.isVariantAvailable(selectedSize, selectedColor)
    val maxQty = stockQuantity.coerceAtLeast(1L).coerceAtMost(50L)
    val canPurchase = stockQuantity > 0L && variantAvailable

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
                .testTag("quick_view_dialog_${product.id}"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                    Row {
                        IconButton(
                            onClick = onToggleWishlist,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("quick_view_wishlist_${product.id}"),
                        ) {
                            Icon(
                                imageVector = if (wishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (wishlisted) "Remove from wishlist" else "Save to wishlist",
                                tint = if (wishlisted) MaterialTheme.colorScheme.primary else ZeecrownDeepPurple,
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("quick_view_close"),
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close quick view")
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(ZeecrownLightLilac),
                ) {
                    ZeecrownProductImage(
                        imageUrl = product.mainImageUrl.ifBlank { product.imageUrls.firstOrNull().orEmpty() },
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ZeecrownSoftLavender.copy(alpha = 0.94f),
                    ) {
                        Text(
                            text = if (stockQuantity > 0L) "IN STOCK ($stockQuantity LEFT)" else "SOLD OUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZeecrownDeepPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                Text(
                    text = product.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = formatBoutiquePrice(product.priceNgn, product.priceUsd, currency),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (currency == StoreCurrency.NGN &&
                        product.compareAtPriceNgn != null &&
                        product.compareAtPriceNgn > product.priceNgn
                    ) {
                        Text(
                            text = formatNgnAmount(product.compareAtPriceNgn),
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                if (product.sizes.isNotEmpty()) {
                    Text("Size / Length", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        product.sizes.forEach { size ->
                            val sizeOk = product.isVariantAvailable(size, selectedColor)
                            FilterChip(
                                selected = selectedSize == size,
                                onClick = { selectedSize = size },
                                enabled = sizeOk || selectedSize == size,
                                label = { Text(size) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ZeecrownSoftLavender,
                                ),
                            )
                        }
                    }
                }

                if (product.colors.isNotEmpty()) {
                    Text("Shade", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        product.colors.forEach { color ->
                            val colorOk = product.isVariantAvailable(selectedSize, color)
                            FilterChip(
                                selected = selectedColor == color,
                                onClick = { selectedColor = color },
                                enabled = colorOk || selectedColor == color,
                                label = { Text(color) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ZeecrownSoftLavender,
                                ),
                            )
                        }
                    }
                }

                if (!variantAvailable) {
                    Text(
                        text = "Selected combination ($selectedSize • $selectedColor) is currently unavailable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Quantity", style = MaterialTheme.typography.titleSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1L) quantity-- },
                            enabled = quantity > 1L,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(ZeecrownSoftLavender, CircleShape),
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity", tint = ZeecrownDeepPurple)
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        IconButton(
                            onClick = { if (quantity < maxQty) quantity++ },
                            enabled = stockQuantity > 0L && quantity < maxQty,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(ZeecrownSoftLavender, CircleShape),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Increase quantity", tint = ZeecrownDeepPurple)
                        }
                    }
                }

                Button(
                    onClick = {
                        val variant = listOf(selectedSize, selectedColor)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                            .ifBlank { "Standard" }
                        onAddToBag(variant, quantity)
                        onDismiss()
                    },
                    enabled = canPurchase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("quick_view_add_to_bag"),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZeecrownRoyalPurple,
                        contentColor = ZeecrownPureWhite,
                    ),
                ) {
                    Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            stockQuantity <= 0L -> "Sold Out"
                            !variantAvailable -> "Variation Unavailable"
                            else -> "Add $quantity to Bag"
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onViewFullDetails()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("quick_view_full_details"),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("View Full Product Details")
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .minimumInteractiveComponentSize(),
                ) {
                    Text("Continue Browsing", color = Color.Gray)
                }
            }
        }
    }
}
