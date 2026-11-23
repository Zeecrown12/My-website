package com.example.ui.storefront

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.firebase.UiState
import com.example.data.model.AppNotification
import com.example.data.model.DeliveryAddress
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StoreCurrency
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.ui.auth.AuthMode
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownObsidianInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender

enum class AccountDashboardSection(val label: String) {
    OVERVIEW("Overview"),
    PROFILE("Profile"),
    ORDERS("Orders"),
    ADDRESSES("Addresses"),
    WISHLIST("Wishlist"),
    SECURITY("Security"),
}

@Composable
fun AccountAndOrdersScreen(
    isSignedIn: Boolean,
    userProfileState: UiState<UserProfile?>,
    privateInfoState: UiState<UserPrivateInfo?>,
    deliveryAddressesState: UiState<List<DeliveryAddress>>,
    ordersState: UiState<List<Order>>,
    wishlistState: UiState<List<WishlistItem>>,
    notificationsState: UiState<List<AppNotification>>,
    products: List<Product>,
    userEmail: String,
    isEmailVerified: Boolean,
    hasPasswordProvider: Boolean,
    resendCooldownSeconds: Int,
    isAdmin: Boolean,
    isSubmitting: Boolean,
    unreadNotificationCount: Int = 0,
    onOpenAuth: (AuthMode) -> Unit,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onUploadPhoto: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onSavePersonalProfile: (firstName: String, lastName: String, phone: String) -> Unit,
    onToggleCurrency: (StoreCurrency) -> Unit,
    onSaveAddress: (addressId: String?, draft: DeliveryAddress, onDone: () -> Unit) -> Unit,
    onSetDefaultAddress: (String) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onSubmitPaymentNotice: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onTrackOrder: (String) -> Unit = {},
    onOpenNotificationsCenter: () -> Unit = {},
    onOpenNotificationPrefs: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenProduct: (String) -> Unit,
    onRemoveWishlist: (Product) -> Unit,
    onBrowseStore: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String, onSuccess: () -> Unit) -> Unit,
    onRequestEmailChange: (currentPassword: String, newEmail: String, onSuccess: () -> Unit) -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isSignedIn) {
        GuestAccountLanding(
            wishlistCount = (wishlistState as? UiState.Success)?.data?.size ?: 0,
            onSignIn = { onOpenAuth(AuthMode.SIGN_IN) },
            onRegister = { onOpenAuth(AuthMode.REGISTER) },
            onTrackGuestOrder = { onTrackOrder("") },
            onBrowseStore = onBrowseStore,
            modifier = modifier,
        )
        return
    }

    val profile = (userProfileState as? UiState.Success)?.data
    val privateInfo = (privateInfoState as? UiState.Success)?.data
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val wishlist = (wishlistState as? UiState.Success)?.data.orEmpty()
    val addresses = (deliveryAddressesState as? UiState.Success)?.data.orEmpty()

    var activeSection by rememberSaveable { mutableStateOf(AccountDashboardSection.OVERVIEW) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        val avatarUrl = profile?.photoUrl.orEmpty()
                        if (avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ZeecrownSoftLavender),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = ZeecrownDeepPurple)
                            }
                        }
                        Column {
                            Text(
                                text = profile?.displayName?.ifBlank { "My Zeecrown Account" } ?: "My Zeecrown Account",
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = userEmail.ifBlank { "Signed-in Customer" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_button"),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }

            if (isAdmin) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = ZeecrownDeepPurple,
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Store Administrator Access",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ZeecrownPureWhite,
                                )
                                Text(
                                    text = "Manage products, categories, inventory, orders, and storefront banners.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ZeecrownPureWhite.copy(alpha = 0.85f),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = onOpenAdminDashboard,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_admin_dashboard_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ZeecrownMetallicGold,
                                    contentColor = ZeecrownObsidianInk,
                                ),
                            ) {
                                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Open")
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onOpenNotificationsCenter,
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("account_quick_notifs"),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Outlined.NotificationsNone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (unreadNotificationCount > 0) "Alerts ($unreadNotificationCount)" else "Alerts")
                    }
                    OutlinedButton(
                        onClick = { onTrackOrder("") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("account_quick_track"),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Track")
                    }
                    OutlinedButton(
                        onClick = onOpenSupport,
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("account_quick_support"),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Outlined.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Support")
                    }
                    OutlinedButton(
                        onClick = onOpenNotificationPrefs,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("account_quick_prefs"),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Notification Preferences", modifier = Modifier.size(16.dp))
                    }
                }
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = activeSection.ordinal,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    AccountDashboardSection.entries.forEach { section ->
                        Tab(
                            selected = activeSection == section,
                            onClick = { activeSection = section },
                            text = { Text(section.label) },
                            modifier = Modifier.testTag("account_tab_${section.name.lowercase()}"),
                        )
                    }
                }
            }

            item {
                when (activeSection) {
                    AccountDashboardSection.OVERVIEW -> AccountOverviewTab(
                        profile = profile,
                        userEmail = userEmail,
                        isEmailVerified = isEmailVerified,
                        resendCooldownSeconds = resendCooldownSeconds,
                        orderCount = orders.size,
                        wishlistCount = wishlist.size,
                        addressCount = addresses.size,
                        recentOrder = orders.firstOrNull(),
                        isSubmitting = isSubmitting,
                        onResendVerification = onResendVerification,
                        onRefreshVerification = onRefreshVerification,
                        onNavigateSection = { activeSection = it },
                    )

                    AccountDashboardSection.PROFILE -> AccountProfileTab(
                        profile = profile,
                        privateInfo = privateInfo,
                        userEmail = userEmail,
                        isSubmitting = isSubmitting,
                        onUploadPhoto = onUploadPhoto,
                        onRemovePhoto = onRemovePhoto,
                        onSavePersonalInfo = onSavePersonalProfile,
                        onToggleCurrency = onToggleCurrency,
                    )

                    AccountDashboardSection.ORDERS -> AccountOrdersTab(
                        ordersState = ordersState,
                        onSubmitPaymentNotice = onSubmitPaymentNotice,
                        onTrackOrder = onTrackOrder,
                        onBrowseStore = onBrowseStore,
                    )

                    AccountDashboardSection.ADDRESSES -> AccountAddressesTab(
                        addressesState = deliveryAddressesState,
                        defaultRecipientName = profile?.displayName.orEmpty(),
                        defaultPhone = privateInfo?.phone.orEmpty(),
                        isSubmitting = isSubmitting,
                        onSaveAddress = onSaveAddress,
                        onSetDefaultAddress = onSetDefaultAddress,
                        onDeleteAddress = onDeleteAddress,
                    )

                    AccountDashboardSection.WISHLIST -> AccountWishlistTab(
                        wishlistState = wishlistState,
                        products = products,
                        onOpenProduct = onOpenProduct,
                        onRemoveWishlist = onRemoveWishlist,
                        onBrowseStore = onBrowseStore,
                    )

                    AccountDashboardSection.SECURITY -> AccountSecurityAndUpdatesTab(
                        userEmail = userEmail,
                        isEmailVerified = isEmailVerified,
                        hasPasswordProvider = hasPasswordProvider,
                        resendCooldownSeconds = resendCooldownSeconds,
                        notificationsState = notificationsState,
                        isSubmitting = isSubmitting,
                        onResendVerification = onResendVerification,
                        onRefreshVerification = onRefreshVerification,
                        onChangePassword = onChangePassword,
                        onRequestEmailChange = onRequestEmailChange,
                        onMarkNotificationRead = onMarkNotificationRead,
                        onOpenNotificationsCenter = onOpenNotificationsCenter,
                        onOpenNotificationPrefs = onOpenNotificationPrefs,
                        onOpenSupport = onOpenSupport,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestAccountLanding(
    wishlistCount: Int,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onTrackGuestOrder: () -> Unit,
    onBrowseStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ZeecrownSoftLavender),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = ZeecrownDeepPurple, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = "Welcome to Your Zeecrown Account",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Sign in or create an account to save Nigerian & international delivery addresses, track orders, sync your bag and wishlist ($wishlistCount saved), and manage security preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("guest_account_signin_button"),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZeecrownRoyalPurple,
                        contentColor = ZeecrownPureWhite,
                    ),
                ) {
                    Text("Sign In / Google Sign-In")
                }
                OutlinedButton(
                    onClick = onRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("guest_account_register_button"),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, ZeecrownRoyalPurple),
                ) {
                    Text("Create New Customer Account", color = ZeecrownRoyalPurple)
                }
                OutlinedButton(
                    onClick = onTrackGuestOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("guest_track_order_button"),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Track Order Without Signing In")
                }
                OutlinedButton(
                    onClick = onBrowseStore,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Continue Browsing Store")
                }
            }
        }
    }
}
