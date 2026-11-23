package com.example.ui.storefront

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.BoutiqueContactHelper
import com.example.data.model.CatalogFilterMode
import com.example.data.model.DeliveryZone
import com.example.data.model.HomepageContent
import com.example.data.model.Review
import com.example.data.model.StoreCurrency
import com.example.data.model.StoreSettings
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.CurrencyToggleChip
import com.example.ui.components.ZeecrownBrandLogo
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.components.formatNgnAmount
import com.example.ui.components.launchWhatsAppChat
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownObsidianInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory
import com.example.ui.theme.ZeecrownWhatsAppGreen
import kotlinx.coroutines.delay

enum class BoutiqueInfoTopic(val title: String) {
    ABOUT("About ZEECROWN STORE"),
    CONTACT("Contact Customer Concierge"),
    FAQ("Frequently Asked Questions"),
    DELIVERY("Delivery Information"),
    RETURNS("Order Policy"),
    PRIVACY("Privacy Policy"),
    TERMS("Terms & Conditions"),
    REFUND("Refund & All-Sales Policy"),
}

@Composable
fun AnnouncementBar(
    messages: List<String>,
    modifier: Modifier = Modifier,
) {
    val cleanMessages = messages.ifEmpty {
        listOf(
            "Welcome to Zeecrown Store.",
            "Discover our latest fashion collections.",
            "Nationwide delivery available.",
            "Special offers on selected products.",
        )
    }
    var index by remember(cleanMessages) { mutableIntStateOf(0) }

    LaunchedEffect(cleanMessages) {
        while (cleanMessages.size > 1) {
            delay(4500L)
            index = (index + 1) % cleanMessages.size
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("announcement_bar"),
        color = ZeecrownSoftLavender,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = {
                    index = if (index - 1 < 0) cleanMessages.lastIndex else index - 1
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("announcement_prev"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Previous announcement",
                    tint = ZeecrownDeepPurple,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = cleanMessages.getOrElse(index) { cleanMessages.first() },
                style = MaterialTheme.typography.labelMedium,
                color = ZeecrownDeepPurple,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            )
            IconButton(
                onClick = { index = (index + 1) % cleanMessages.size },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("announcement_next"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Next announcement",
                    tint = ZeecrownDeepPurple,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun BoutiqueHeaderBar(
    isWideScreen: Boolean,
    currency: StoreCurrency,
    wishlistCount: Int,
    bagCount: Int,
    unreadNotificationCount: Int = 0,
    isAdmin: Boolean,
    logoUrl: String = "",
    whatsappNumber: String = BoutiqueContactHelper.OFFICIAL_WHATSAPP_NUMBER_DISPLAY,
    onToggleCurrency: () -> Unit,
    onNavigateShop: (CatalogFilterMode) -> Unit,
    onScrollToCategories: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenOrderTracking: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenWishlist: () -> Unit,
    onOpenBag: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenInfoTopic: (BoutiqueInfoTopic) -> Unit,
    onOpenMobileMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("boutique_header"),
        color = ZeecrownPureWhite,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        if (isWideScreen) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ZeecrownBrandLogo(compact = false, logoUrl = logoUrl)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Button(
                            onClick = { launchWhatsAppChat(context, whatsappNumber) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("header_whatsapp_button"),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZeecrownWhatsAppGreen,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Chat with Us on WhatsApp", style = MaterialTheme.typography.labelMedium)
                        }
                        CurrencyToggleChip(currency = currency, onToggle = onToggleCurrency)
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("header_search_button"),
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search Catalog")
                        }
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("header_notifications_button"),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge { Text(unreadNotificationCount.coerceAtMost(99).toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications")
                            }
                        }
                        IconButton(
                            onClick = onOpenAccount,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("header_account_button"),
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = "Customer Account")
                        }
                        IconButton(
                            onClick = onOpenWishlist,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("header_wishlist_button"),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (wishlistCount > 0) {
                                        Badge { Text(wishlistCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist")
                            }
                        }
                        IconButton(
                            onClick = onOpenBag,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("header_bag_button"),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (bagCount > 0) {
                                        Badge { Text(bagCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.ShoppingBag, contentDescription = "Shopping Bag")
                            }
                        }
                        if (isAdmin) {
                            OutlinedButton(
                                onClick = onOpenAdmin,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_admin_shortcut"),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Admin")
                            }
                        }
                    }
                }
                HorizontalDivider(color = ZeecrownElegantSilver)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderNavLink("Home", "nav_link_home") {}
                    HeaderNavLink("Shop", "nav_link_shop") { onNavigateShop(CatalogFilterMode.ALL) }
                    HeaderNavLink("Categories", "nav_link_categories") { onScrollToCategories() }
                    HeaderNavLink("New Arrivals", "nav_link_new_arrivals") { onNavigateShop(CatalogFilterMode.NEW_ARRIVALS) }
                    HeaderNavLink("Best Sellers", "nav_link_best_sellers") { onNavigateShop(CatalogFilterMode.BEST_SELLERS) }
                    HeaderNavLink("Track Order", "nav_link_track_order") { onOpenOrderTracking() }
                    HeaderNavLink("Support", "nav_link_support") { onOpenSupport() }
                    HeaderNavLink("About Us", "nav_link_about") { onOpenInfoTopic(BoutiqueInfoTopic.ABOUT) }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    IconButton(
                        onClick = onOpenMobileMenu,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mobile_menu_button"),
                    ) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Open navigation menu", tint = ZeecrownDeepPurple)
                    }
                    ZeecrownBrandLogo(compact = true, logoUrl = logoUrl)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Surface(
                        onClick = { launchWhatsAppChat(context, whatsappNumber) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("header_whatsapp_mobile_button"),
                        shape = RoundedCornerShape(18.dp),
                        color = ZeecrownWhatsAppGreen,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Chat,
                                contentDescription = "Chat with Us on WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "WhatsApp",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("header_search_button"),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search products")
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("header_notifications_button"),
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text(unreadNotificationCount.coerceAtMost(99).toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications")
                        }
                    }
                    IconButton(
                        onClick = onOpenWishlist,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("header_wishlist_button"),
                    ) {
                        BadgedBox(
                            badge = {
                                if (wishlistCount > 0) {
                                    Badge { Text(wishlistCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist")
                        }
                    }
                    IconButton(
                        onClick = onOpenBag,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("header_bag_button"),
                    ) {
                        BadgedBox(
                            badge = {
                                if (bagCount > 0) {
                                    Badge { Text(bagCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.ShoppingBag, contentDescription = "Shopping Bag")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderNavLink(
    label: String,
    tag: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(tag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ZeecrownDeepPurple,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorialHeroSection(
    hero: HomepageContent?,
    isWideLayout: Boolean,
    onShopNow: () -> Unit,
    onExploreCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headline = hero?.title?.ifBlank { null } ?: "STYLE THAT SPEAKS. ELEGANCE THAT LASTS."
    val subtitle = hero?.subtitle?.ifBlank { null }
        ?: "Discover beautiful fashion essentials and timeless pieces selected to elevate your everyday style."
    val primaryLabel = hero?.ctaLabel?.ifBlank { null } ?: "SHOP NOW"
    val secondaryLabel = hero?.secondaryCtaLabel?.ifBlank { null } ?: "EXPLORE COLLECTION"
    val imageKey = hero?.imageKey?.ifBlank { null } ?: "img_hero_editorial"

    if (isWideLayout) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("hero_section"),
            shape = MaterialTheme.shapes.large,
            color = ZeecrownLightLilac,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ZeecrownSoftLavender,
                    ) {
                        Text(
                            text = "ZEECROWN SIGNATURE ATELIER",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZeecrownDeepPurple,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.displaySmall,
                        color = ZeecrownDeepPurple,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onShopNow,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("hero_shop_now_button"),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZeecrownRoyalPurple,
                                contentColor = ZeecrownPureWhite,
                            ),
                        ) {
                            Text(primaryLabel)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        OutlinedButton(
                            onClick = onExploreCollection,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("hero_explore_button"),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, ZeecrownRoyalPurple),
                        ) {
                            Text(secondaryLabel, color = ZeecrownRoyalPurple)
                        }
                    }
                }
                ZeecrownProductImage(
                    imageUrl = "drawable://$imageKey",
                    contentDescription = headline,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)),
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(MaterialTheme.shapes.large)
                .testTag("hero_section"),
        ) {
            ZeecrownProductImage(
                imageUrl = "drawable://$imageKey",
                contentDescription = headline,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.08f),
                                ZeecrownDeepPurple.copy(alpha = 0.84f),
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ZeecrownSoftLavender.copy(alpha = 0.94f),
                ) {
                    Text(
                        text = "ZEECROWN SIGNATURE ATELIER",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZeecrownDeepPurple,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineLarge,
                    color = ZeecrownPureWhite,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZeecrownPureWhite.copy(alpha = 0.92f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onShopNow,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hero_shop_now_button"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeecrownMetallicGold,
                            contentColor = ZeecrownObsidianInk,
                        ),
                    ) {
                        Text(primaryLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onExploreCollection,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hero_explore_button"),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, ZeecrownPureWhite),
                    ) {
                        Text(secondaryLabel, color = ZeecrownPureWhite, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun TheZeecrownEditSection(
    content: HomepageContent?,
    isWideLayout: Boolean,
    onShopCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = content?.title?.ifBlank { null } ?: "THE ZEECROWN EDIT"
    val subtitle = content?.subtitle?.ifBlank { null }
        ?: "Luminous silk Abayas, artisanal Abeokuta Adire, and sculpted metallic gold finishing pieces curated for effortless poise."
    val ctaLabel = content?.ctaLabel?.ifBlank { null } ?: "SHOP THE COLLECTION"
    val imageKey = content?.imageKey?.ifBlank { null } ?: "img_zeecrown_edit_campaign"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("zeecrown_edit_section"),
        shape = MaterialTheme.shapes.large,
        color = ZeecrownLightLilac,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        if (isWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZeecrownProductImage(
                    imageUrl = "drawable://$imageKey",
                    contentDescription = title,
                    modifier = Modifier
                        .weight(1f)
                        .height(280.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "FEATURED COLLECTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZeecrownGoldInk,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = ZeecrownDeepPurple,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onShopCollection,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("zeecrown_edit_cta"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeecrownRoyalPurple,
                            contentColor = ZeecrownPureWhite,
                        ),
                    ) {
                        Text(ctaLabel)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                ZeecrownProductImage(
                    imageUrl = "drawable://$imageKey",
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                )
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "FEATURED COLLECTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZeecrownGoldInk,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ZeecrownDeepPurple,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onShopCollection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("zeecrown_edit_cta"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeecrownRoyalPurple,
                            contentColor = ZeecrownPureWhite,
                        ),
                    ) {
                        Text(ctaLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionalBannersSection(
    promos: List<HomepageContent>,
    isAdmin: Boolean,
    onOpenPromo: (HomepageContent) -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("promotional_banners_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "SEASONAL HIGHLIGHTS",
            style = MaterialTheme.typography.headlineMedium,
            color = ZeecrownDeepPurple,
        )
        if (promos.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = ZeecrownLightLilac,
                border = BorderStroke(1.dp, ZeecrownElegantSilver),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Promotional campaigns configured by the store team will showcase here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (isAdmin) {
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = onOpenAdmin,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Text("Configure")
                        }
                    }
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(promos, key = { it.id }) { promo ->
                    Surface(
                        onClick = { onOpenPromo(promo) },
                        modifier = Modifier
                            .width(292.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("promo_banner_${promo.id}"),
                        shape = MaterialTheme.shapes.medium,
                        color = ZeecrownPureWhite,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Column {
                            ZeecrownProductImage(
                                imageUrl = "drawable://${promo.imageKey.ifBlank { "img_cat_adire_abaya" }}",
                                contentDescription = promo.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            )
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = promo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ZeecrownDeepPurple,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = promo.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = promo.ctaLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhyShopWithZeecrownSection(modifier: Modifier = Modifier) {
    val pillars = listOf(
        Triple(Icons.Outlined.AutoAwesome, "Curated Fashion Pieces", "Handpicked Adire, Abayas, leather footwear & elevated accessories."),
        Triple(Icons.Outlined.ShoppingBag, "Convenient Shopping", "Dual NGN/USD pricing with live bag and wishlist sync."),
        Triple(Icons.Outlined.VerifiedUser, "Secure Payments", "Paystack, Flutterwave & verified direct bank transfers."),
        Triple(Icons.Outlined.LocalShipping, "Reliable Delivery", "Express dispatch across Lagos, Nigeria & global hubs."),
        Triple(Icons.Outlined.SupportAgent, "Dedicated Support", "Responsive boutique concierge from styling to doorstep delivery."),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("why_zeecrown_section"),
        shape = MaterialTheme.shapes.large,
        color = ZeecrownPureWhite,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "WHY SHOP WITH ZEECROWN",
                style = MaterialTheme.typography.headlineMedium,
                color = ZeecrownDeepPurple,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pillars.forEach { (icon, title, desc) ->
                    Surface(
                        modifier = Modifier.width(260.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = ZeecrownLightLilac,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ZeecrownSoftLavender),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(title, style = MaterialTheme.typography.titleSmall, color = ZeecrownDeepPurple)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerTestimonialsSection(
    reviewsState: UiState<List<Review>>,
    onOpenProduct: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("testimonials_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "CLIENT TESTIMONIALS",
            style = MaterialTheme.typography.headlineMedium,
            color = ZeecrownDeepPurple,
        )
        when (reviewsState) {
            is UiState.Loading -> Text(
                text = "Loading verified client reviews...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is UiState.Error -> BoutiqueErrorBanner(reviewsState.message)
            is UiState.Success -> {
                val reviews = reviewsState.data.take(8)
                if (reviews.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = ZeecrownLightLilac,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Approved reviews submitted by verified Zeecrown clients will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(reviews, key = { it.id }) { review ->
                            Surface(
                                onClick = { onOpenProduct(review.productId) },
                                modifier = Modifier
                                    .width(270.dp)
                                    .minimumInteractiveComponentSize(),
                                shape = MaterialTheme.shapes.medium,
                                color = ZeecrownPureWhite,
                                border = BorderStroke(1.dp, ZeecrownElegantSilver),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        repeat(review.rating.toInt().coerceIn(1, 5)) {
                                            Icon(Icons.Filled.Star, contentDescription = null, tint = ZeecrownMetallicGold, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = "\"${review.comment}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ZeecrownObsidianInk,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "— ${review.authorName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsletterSignupSection(
    defaultEmail: String,
    isSubmitting: Boolean,
    feedback: Pair<Boolean, String>?,
    onSubscribe: (String) -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var emailInput by remember(defaultEmail) { mutableStateOf(defaultEmail) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("newsletter_section"),
        shape = MaterialTheme.shapes.large,
        color = ZeecrownSoftLavender.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, ZeecrownMetallicGold.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "STAY IN STYLE WITH ZEECROWN",
                style = MaterialTheme.typography.headlineMedium,
                color = ZeecrownDeepPurple,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Be the first to discover new arrivals, exclusive offers and beautiful fashion inspiration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        if (feedback != null) onClearFeedback()
                    },
                    label = { Text("Email address") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("newsletter_email_input"),
                )
                Button(
                    onClick = { onSubscribe(emailInput) },
                    enabled = !isSubmitting && emailInput.isNotBlank(),
                    modifier = Modifier
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("newsletter_subscribe_button"),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZeecrownRoyalPurple,
                        contentColor = ZeecrownPureWhite,
                    ),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ZeecrownPureWhite, strokeWidth = 2.dp)
                    } else {
                        Text("Subscribe")
                    }
                }
            }
            if (feedback != null) {
                val (success, msg) = feedback
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (success) ZeecrownPureWhite else MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (success) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ZeecrownSageSuccess, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (success) ZeecrownObsidianInk else MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialDiscoverySection(
    storeSettings: StoreSettings,
    isAdmin: Boolean,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val hasSocialLinks = storeSettings.instagramUrl.isNotBlank() ||
        storeSettings.instagramHandle.isNotBlank() ||
        storeSettings.tiktokUrl.isNotBlank() ||
        storeSettings.tiktokHandle.isNotBlank() ||
        storeSettings.facebookUrl.startsWith("http") ||
        storeSettings.pinterestUrl.startsWith("http") ||
        storeSettings.youtubeUrl.startsWith("http") ||
        storeSettings.twitterHandle.isNotBlank()
    val gallery = storeSettings.socialGalleryUrls

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("social_media_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "ZEECROWN ON SOCIAL",
            style = MaterialTheme.typography.headlineMedium,
            color = ZeecrownDeepPurple,
        )
        if (!hasSocialLinks && gallery.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = ZeecrownLightLilac,
                border = BorderStroke(1.dp, ZeecrownElegantSilver),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Official Instagram, TikTok, Facebook & Pinterest social profiles can be configured in Store Admin Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (isAdmin) {
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Text("Set Links")
                        }
                    }
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (storeSettings.instagramUrl.isNotBlank() || storeSettings.instagramHandle.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            if (storeSettings.instagramUrl.startsWith("http")) {
                                uriHandler.openUri(storeSettings.instagramUrl)
                            }
                        },
                        enabled = storeSettings.instagramUrl.startsWith("http"),
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("Instagram ${storeSettings.instagramHandle.ifBlank { "" }}".trim())
                    }
                }
                if (storeSettings.tiktokUrl.isNotBlank() || storeSettings.tiktokHandle.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            if (storeSettings.tiktokUrl.startsWith("http")) {
                                uriHandler.openUri(storeSettings.tiktokUrl)
                            }
                        },
                        enabled = storeSettings.tiktokUrl.startsWith("http"),
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("TikTok ${storeSettings.tiktokHandle.ifBlank { "" }}".trim())
                    }
                }
                if (storeSettings.facebookUrl.startsWith("http")) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(storeSettings.facebookUrl) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("Facebook")
                    }
                }
                if (storeSettings.pinterestUrl.startsWith("http")) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(storeSettings.pinterestUrl) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("Pinterest")
                    }
                }
                if (storeSettings.youtubeUrl.startsWith("http")) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(storeSettings.youtubeUrl) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("YouTube")
                    }
                }
                if (storeSettings.twitterHandle.isNotBlank()) {
                    val cleanHandle = storeSettings.twitterHandle.trim().removePrefix("@")
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://x.com/$cleanHandle") },
                        enabled = cleanHandle.isNotBlank(),
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("X @$cleanHandle")
                    }
                }
            }
            if (gallery.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(gallery) { imgUrl ->
                        ZeecrownProductImage(
                            imageUrl = imgUrl,
                            contentDescription = "Zeecrown social feature",
                            modifier = Modifier
                                .size(136.dp)
                                .clip(MaterialTheme.shapes.medium),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoutiqueFooterSection(
    storeSettings: StoreSettings,
    onNavigateShop: (CatalogFilterMode) -> Unit,
    onScrollToCategories: () -> Unit,
    onTrackOrders: () -> Unit,
    onOpenSupport: () -> Unit = {},
    onOpenInfoTopic: (BoutiqueInfoTopic) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("boutique_footer"),
        color = ZeecrownLightLilac,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ZeecrownBrandLogo(compact = true, logoUrl = storeSettings.logoUrl)
                Text(
                    text = storeSettings.aboutText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = ZeecrownElegantSilver)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FooterColumn(title = "SHOP") {
                    FooterLink("All Products") { onNavigateShop(CatalogFilterMode.ALL) }
                    FooterLink("New Arrivals") { onNavigateShop(CatalogFilterMode.NEW_ARRIVALS) }
                    FooterLink("Best Sellers") { onNavigateShop(CatalogFilterMode.BEST_SELLERS) }
                    FooterLink("Categories") { onScrollToCategories() }
                }

                FooterColumn(title = "CUSTOMER SERVICE") {
                    FooterLink("Concierge Support Center") { onOpenSupport() }
                    FooterLink("Contact Us") { onOpenInfoTopic(BoutiqueInfoTopic.CONTACT) }
                    FooterLink("Frequently Asked Questions") { onOpenInfoTopic(BoutiqueInfoTopic.FAQ) }
                    FooterLink("Delivery Information") { onOpenInfoTopic(BoutiqueInfoTopic.DELIVERY) }
                    FooterLink("Order Policy") { onOpenInfoTopic(BoutiqueInfoTopic.RETURNS) }
                    FooterLink("Order Tracking") { onTrackOrders() }
                }

                FooterColumn(title = "POLICIES") {
                    FooterLink("Privacy Policy") { onOpenInfoTopic(BoutiqueInfoTopic.PRIVACY) }
                    FooterLink("Terms & Conditions") { onOpenInfoTopic(BoutiqueInfoTopic.TERMS) }
                    FooterLink("Refund & All-Sales Policy") { onOpenInfoTopic(BoutiqueInfoTopic.REFUND) }
                }

                FooterColumn(title = "CONTACT US") {
                    val footerContext = LocalContext.current
                    val resolvedWaDisplay = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(storeSettings.whatsappNumber)
                    if (storeSettings.supportEmail.isNotBlank()) {
                        Text("Email: ${storeSettings.supportEmail}", style = MaterialTheme.typography.bodySmall, color = ZeecrownObsidianInk)
                    }
                    if (storeSettings.supportPhone.isNotBlank()) {
                        Text("Phone: ${storeSettings.supportPhone}", style = MaterialTheme.typography.bodySmall, color = ZeecrownObsidianInk)
                    }
                    Surface(
                        onClick = { launchWhatsAppChat(footerContext, storeSettings.whatsappNumber) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .padding(vertical = 2.dp)
                            .testTag("footer_whatsapp_link"),
                        shape = RoundedCornerShape(10.dp),
                        color = ZeecrownWhatsAppGreen,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text(
                                text = "WhatsApp: $resolvedWaDisplay",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (storeSettings.instagramHandle.isNotBlank()) {
                        Text("Instagram: ${storeSettings.instagramHandle}", style = MaterialTheme.typography.bodySmall, color = ZeecrownObsidianInk)
                    }
                }
            }

            HorizontalDivider(color = ZeecrownElegantSilver)

            Text(
                text = "© 2026 ${storeSettings.storeName}. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FooterColumn(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.width(190.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = ZeecrownDeepPurple,
        )
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun FooterLink(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutiqueMobileMenuSheet(
    currency: StoreCurrency,
    unreadNotificationCount: Int = 0,
    isAdmin: Boolean,
    whatsappNumber: String = BoutiqueContactHelper.OFFICIAL_WHATSAPP_NUMBER_DISPLAY,
    onDismiss: () -> Unit,
    onToggleCurrency: () -> Unit,
    onNavigateShop: (CatalogFilterMode) -> Unit,
    onScrollToCategories: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenOrderTracking: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenAdmin: () -> Unit,
    onOpenInfoTopic: (BoutiqueInfoTopic) -> Unit,
) {
    val menuContext = LocalContext.current
    val resolvedWaDisplay = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(whatsappNumber)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ZeecrownWarmIvory,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZeecrownBrandLogo(compact = true)
                CurrencyToggleChip(currency = currency, onToggle = onToggleCurrency)
            }
            Button(
                onClick = {
                    onDismiss()
                    launchWhatsAppChat(menuContext, whatsappNumber)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .padding(vertical = 4.dp)
                    .testTag("mobile_menu_whatsapp_item"),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownWhatsAppGreen, contentColor = Color.White),
            ) {
                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chat with Us on WhatsApp · $resolvedWaDisplay")
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = ZeecrownElegantSilver)
            MenuSheetItem("Home") { onDismiss() }
            MenuSheetItem("Shop All Pieces") { onDismiss(); onNavigateShop(CatalogFilterMode.ALL) }
            MenuSheetItem("Shop by Category") { onDismiss(); onScrollToCategories() }
            MenuSheetItem("New Arrivals") { onDismiss(); onNavigateShop(CatalogFilterMode.NEW_ARRIVALS) }
            MenuSheetItem("Best Sellers") { onDismiss(); onNavigateShop(CatalogFilterMode.BEST_SELLERS) }
            MenuSheetItem("Track an Order") { onDismiss(); onOpenOrderTracking() }
            MenuSheetItem(
                if (unreadNotificationCount > 0) "Notification Center ($unreadNotificationCount unread)" else "Notification Center"
            ) { onDismiss(); onOpenNotifications() }
            MenuSheetItem("Concierge Support") { onDismiss(); onOpenSupport() }
            MenuSheetItem("Customer Account & Orders") { onDismiss(); onOpenAccount() }
            MenuSheetItem("About Us") { onDismiss(); onOpenInfoTopic(BoutiqueInfoTopic.ABOUT) }
            MenuSheetItem("Contact Concierge") { onDismiss(); onOpenInfoTopic(BoutiqueInfoTopic.CONTACT) }
            MenuSheetItem("Delivery Information") { onDismiss(); onOpenInfoTopic(BoutiqueInfoTopic.DELIVERY) }
            if (isAdmin) {
                MenuSheetItem("Store Admin Dashboard") { onDismiss(); onOpenAdmin() }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MenuSheetItem(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ZeecrownDeepPurple)
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = ZeecrownRoyalPurple)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutiqueInfoModalSheet(
    topic: BoutiqueInfoTopic,
    storeSettings: StoreSettings,
    deliveryZones: List<DeliveryZone>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ZeecrownWarmIvory,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(topic.title, style = MaterialTheme.typography.headlineMedium, color = ZeecrownDeepPurple)
            HorizontalDivider(color = ZeecrownElegantSilver)
            when (topic) {
                BoutiqueInfoTopic.ABOUT -> {
                    Text(storeSettings.aboutText, style = MaterialTheme.typography.bodyLarge, color = ZeecrownObsidianInk)
                    Text("Serving discerning style lovers across Lagos, Nigeria, and worldwide.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BoutiqueInfoTopic.CONTACT -> {
                    val modalContext = LocalContext.current
                    val resolvedWaDisplay = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(storeSettings.whatsappNumber)
                    if (storeSettings.supportEmail.isNotBlank()) Text("Email: ${storeSettings.supportEmail}", style = MaterialTheme.typography.bodyLarge)
                    if (storeSettings.supportPhone.isNotBlank()) Text("Phone: ${storeSettings.supportPhone}", style = MaterialTheme.typography.bodyLarge)
                    Text("Official WhatsApp: $resolvedWaDisplay", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ZeecrownDeepPurple)
                    if (storeSettings.businessAddress.isNotBlank()) Text("Address: ${storeSettings.businessAddress}", style = MaterialTheme.typography.bodyMedium)
                    if (storeSettings.supportHours.isNotBlank()) Text("Hours: ${storeSettings.supportHours}", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { launchWhatsAppChat(modalContext, storeSettings.whatsappNumber) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("contact_modal_whatsapp_btn"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownWhatsAppGreen, contentColor = Color.White),
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Chat with Us on WhatsApp")
                    }
                }
                BoutiqueInfoTopic.DELIVERY -> {
                    if (storeSettings.shippingPolicy.isNotBlank()) {
                        Text(storeSettings.shippingPolicy, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (deliveryZones.isEmpty()) {
                        Text("Delivery zones across Lagos, regional Nigerian states, and international express hubs are configured in Firestore.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        deliveryZones.forEach { zone ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = ZeecrownLightLilac,
                                border = BorderStroke(1.dp, ZeecrownElegantSilver),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(zone.name, style = MaterialTheme.typography.titleSmall)
                                        Text("${zone.estimatedDays} • ${zone.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(formatNgnAmount(zone.feeNgn), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                BoutiqueInfoTopic.FAQ -> {
                    Text("• Sizing & Fit: Every garment and footwear listing displays exact size notes, Size Guides, and color variants prior to bag addition.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Payments: Choose Paystack, Flutterwave, or Direct Nigerian Bank Transfer at checkout.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Order Updates: Track real-time confirmation, dispatch, and delivery statuses inside Account & Orders.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Returns & Refunds: ZEECROWN STORE currently does not accept customer returns or refunds. Please check our Size Guide or WhatsApp concierge before ordering.", style = MaterialTheme.typography.bodyMedium)
                }
                BoutiqueInfoTopic.RETURNS, BoutiqueInfoTopic.REFUND -> {
                    Text(
                        storeSettings.returnPolicy.ifBlank {
                            "ZEECROWN STORE currently does not accept customer returns or refunds. Please check sizing, measurements, and product details using our Size Guide or WhatsApp concierge before placing your order. If you received a wrong or damaged item or have a fulfillment concern, please submit a Support Ticket or message us on WhatsApp."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = ZeecrownObsidianInk,
                    )
                }
                BoutiqueInfoTopic.PRIVACY -> {
                    Text(
                        storeSettings.privacyPolicy.ifBlank {
                            "ZEECROWN STORE isolates customer email, phone, and street addresses inside private Firestore records accessible solely to the account holder and authorized store fulfillment."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                BoutiqueInfoTopic.TERMS -> {
                    Text(
                        storeSettings.termsPolicy.ifBlank {
                            "Orders placed on ZEECROWN STORE are fulfilled subject to live inventory confirmation and verified payment settlement."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
