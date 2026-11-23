package com.example.ui.storefront

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.auth.AuthValidation
import com.example.data.firebase.UiState
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddress
import com.example.data.model.DeliveryZone
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreCurrency
import com.example.data.model.StoreSettings
import com.example.data.model.UserPrivateInfo
import com.example.data.model.WishlistItem
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender

@Composable
fun BagAndWishlistScreen(
    isSignedIn: Boolean,
    cartState: UiState<List<CartItem>>,
    wishlistState: UiState<List<WishlistItem>>,
    products: List<Product>,
    initialTab: Int = 0,
    tabNavigationNonce: Int = 0,
    onSignInToSync: () -> Unit = {},
    onUpdateCartQuantity: (String, Long) -> Unit,
    onRemoveCartItem: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onRemoveWishlist: (Product) -> Unit,
    onBrowseStore: () -> Unit,
    onProceedToCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember(initialTab, tabNavigationNonce) {
        mutableIntStateOf(initialTab.coerceIn(0, 1))
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
                .fillMaxSize(),
        ) {
            Text(
                text = "Your Boutique Edit",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )

            if (!isSignedIn) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = ZeecrownDeepPurple,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shopping as Guest",
                                style = MaterialTheme.typography.labelLarge,
                                color = ZeecrownDeepPurple,
                            )
                            Text(
                                text = "Your bag and wishlist are saved on this device. Sign in to sync across devices and complete checkout.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = onSignInToSync,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("bag_guest_signin_button"),
                        ) {
                            Text("Sign In")
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                val bagCount = (cartState as? UiState.Success)?.data?.sumOf { it.quantity } ?: 0L
                val wishCount = (wishlistState as? UiState.Success)?.data?.size ?: 0
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Shopping Bag ($bagCount)") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tab_shopping_bag"),
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Wishlist ($wishCount)") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tab_wishlist"),
                )
            }

            if (selectedTab == 0) {
                when (cartState) {
                    is UiState.Loading -> BoutiqueLoadingView("Loading shopping bag...")
                    is UiState.Error -> Box(Modifier.padding(16.dp)) {
                        BoutiqueErrorBanner(cartState.message)
                    }
                    is UiState.Success -> {
                        val items = cartState.data
                        if (items.isEmpty()) {
                            BoutiqueEmptyView(
                                title = "Your Bag Is Empty",
                                subtitle = "Explore our latest Adire, Abayas, leather slippers, and handbags to begin curating your order.",
                                actionLabel = "Explore Boutique",
                                onAction = onBrowseStore,
                            )
                        } else {
                            val subtotal = items.sumOf { it.unitPriceNgn * it.quantity }
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(items, key = { it.id }) { item ->
                                        BagLineCard(
                                            item = item,
                                            onIncrement = { onUpdateCartQuantity(item.id, item.quantity + 1) },
                                            onDecrement = { onUpdateCartQuantity(item.id, item.quantity - 1) },
                                            onRemove = { onRemoveCartItem(item.id) },
                                            onClick = { onOpenProduct(item.productId) },
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text("Subtotal", style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = formatNgnAmount(subtotal),
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        Text(
                                            text = "Delivery fees for Lagos, Nigeria & international destinations are calculated at checkout.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Button(
                                            onClick = onProceedToCheckout,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .minimumInteractiveComponentSize()
                                                .testTag("proceed_to_checkout_button"),
                                            shape = MaterialTheme.shapes.small,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ZeecrownRoyalPurple,
                                                contentColor = ZeecrownPureWhite,
                                            ),
                                        ) {
                                            Icon(Icons.Outlined.ShoppingBag, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (isSignedIn) "Proceed to Checkout" else "Sign In to Checkout",
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when (wishlistState) {
                    is UiState.Loading -> BoutiqueLoadingView("Loading wishlist...")
                    is UiState.Error -> Box(Modifier.padding(16.dp)) {
                        BoutiqueErrorBanner(wishlistState.message)
                    }
                    is UiState.Success -> {
                        val wishes = wishlistState.data
                        if (wishes.isEmpty()) {
                            BoutiqueEmptyView(
                                title = "No Saved Favorites Yet",
                                subtitle = "Tap the heart icon on any piece to keep your personal wardrobe wishlist ready.",
                                actionLabel = "Discover Pieces",
                                onAction = onBrowseStore,
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(wishes, key = { it.id }) { wish ->
                                    val productMatch = products.firstOrNull { it.id == wish.productId }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenProduct(wish.productId) },
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            ZeecrownProductImage(
                                                imageUrl = wish.productImageUrl,
                                                contentDescription = wish.productTitle,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(MaterialTheme.shapes.small),
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Text(
                                                    text = wish.productTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = formatNgnAmount(wish.priceNgn),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            if (productMatch != null) {
                                                IconButton(
                                                    onClick = { onRemoveWishlist(productMatch) },
                                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.DeleteOutline,
                                                        contentDescription = "Remove from wishlist",
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
            }
        }
    }
}

@Composable
private fun BagLineCard(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ZeecrownProductImage(
                imageUrl = item.productImageUrl,
                contentDescription = item.productTitle,
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.productTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.variantLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatNgnAmount(item.unitPriceNgn * item.quantity),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity")
                    }
                    Text(
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Increase quantity")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove item",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    deliveryZones: List<DeliveryZone>,
    savedAddresses: List<DeliveryAddress>,
    privateInfo: UserPrivateInfo?,
    defaultRecipientName: String,
    storeSettings: StoreSettings,
    currency: StoreCurrency,
    isCartLoading: Boolean,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSaveNewAddress: (DeliveryAddress, onSaved: (String) -> Unit) -> Unit,
    onPlaceOrder: (
        zone: DeliveryZone,
        city: String,
        state: String,
        paymentMethod: PaymentMethod,
        transferNote: String,
        selectedAddress: DeliveryAddress?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var selectedAddressId by remember(savedAddresses) {
        mutableStateOf(
            savedAddresses.firstOrNull { it.defaultAddress }?.id
                ?: savedAddresses.firstOrNull()?.id.orEmpty()
        )
    }
    var useCustomAddress by remember(savedAddresses) {
        mutableStateOf(savedAddresses.isEmpty())
    }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    var recipientName by remember(privateInfo, defaultRecipientName) {
        mutableStateOf(defaultRecipientName)
    }
    var recipientPhone by remember(privateInfo) {
        mutableStateOf(privateInfo?.phone.orEmpty())
    }
    var streetAddress by remember(privateInfo) {
        mutableStateOf(privateInfo?.shippingAddress.orEmpty())
    }
    var shippingCity by remember(privateInfo) {
        mutableStateOf(privateInfo?.city.orEmpty().ifBlank { "Lekki" })
    }
    var shippingState by remember(privateInfo) {
        mutableStateOf(privateInfo?.state.orEmpty().ifBlank { "Lagos" })
    }

    val chosenSavedAddress = if (!useCustomAddress) {
        savedAddresses.firstOrNull { it.id == selectedAddressId }
            ?: savedAddresses.firstOrNull { it.defaultAddress }
            ?: savedAddresses.firstOrNull()
    } else {
        null
    }

    val activeState = chosenSavedAddress?.state?.ifBlank { shippingState } ?: shippingState
    val activeCity = chosenSavedAddress?.city?.ifBlank { shippingCity } ?: shippingCity

    var selectedZoneId by remember(deliveryZones) {
        mutableStateOf(deliveryZones.firstOrNull()?.id.orEmpty())
    }
    LaunchedEffect(activeState, deliveryZones) {
        val stateMatch = deliveryZones.firstOrNull { zone ->
            zone.states.any { it.equals(activeState.trim(), ignoreCase = true) }
        }
        if (stateMatch != null) {
            selectedZoneId = stateMatch.id
        }
    }

    var selectedPayment by remember { mutableStateOf(PaymentMethod.PAYSTACK) }
    var transferNote by remember { mutableStateOf("") }
    var addressValidationError by remember { mutableStateOf<String?>(null) }

    val selectedZone = deliveryZones.firstOrNull { it.id == selectedZoneId } ?: deliveryZones.firstOrNull()
    val subtotal = cartItems.sumOf { it.unitPriceNgn * it.quantity }
    val deliveryFee = selectedZone?.feeNgn ?: 0.0
    val total = subtotal + deliveryFee

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Bag")
                }
                Text(
                    text = "Secure Checkout",
                    style = MaterialTheme.typography.headlineLarge,
                )
            }

            if (isCartLoading && cartItems.isEmpty()) {
                BoutiqueLoadingView("Syncing your shopping bag...")
                return@Column
            }

            if (cartItems.isEmpty() || selectedZone == null) {
                BoutiqueEmptyView(
                    title = "Checkout Unavailable",
                    subtitle = if (cartItems.isEmpty()) {
                        "Your shopping bag is currently empty."
                    } else {
                        "No active delivery zones are configured yet. Ask an administrator to initialize delivery zones."
                    },
                    actionLabel = "Back",
                    onAction = onBack,
                )
                return@Column
            }

            // Delivery Destination / Saved Addresses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Delivery Destination", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(
                    onClick = { showAddAddressDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("checkout_add_address_button"),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Address")
                }
            }

            if (savedAddresses.isNotEmpty()) {
                savedAddresses.forEach { addr ->
                    val isSelected = !useCustomAddress && chosenSavedAddress?.id == addr.id
                    Surface(
                        onClick = {
                            useCustomAddress = false
                            selectedAddressId = addr.id
                            addressValidationError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_address_${addr.id}"),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) ZeecrownSoftLavender.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else ZeecrownElegantSilver,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    useCustomAddress = false
                                    selectedAddressId = addr.id
                                    addressValidationError = null
                                },
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = addr.label,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                    if (addr.defaultAddress) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ZeecrownLightLilac,
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            ) {
                                                Icon(
                                                    Icons.Outlined.CheckCircle,
                                                    contentDescription = null,
                                                    tint = ZeecrownSageSuccess,
                                                    modifier = Modifier.size(12.dp),
                                                )
                                                Text("Default", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "${addr.recipientName} · ${addr.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = addr.formattedSummary(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = { useCustomAddress = true },
                    shape = MaterialTheme.shapes.medium,
                    color = if (useCustomAddress) ZeecrownSoftLavender.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(
                        width = if (useCustomAddress) 1.5.dp else 1.dp,
                        color = if (useCustomAddress) MaterialTheme.colorScheme.primary else ZeecrownElegantSilver,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = useCustomAddress,
                            onClick = { useCustomAddress = true },
                        )
                        Column {
                            Text("Use One-Time Delivery Details", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Enter a different recipient or destination without modifying your address book.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (useCustomAddress || savedAddresses.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = recipientName,
                            onValueChange = { recipientName = it; addressValidationError = null },
                            label = { Text("Recipient Name *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = recipientPhone,
                            onValueChange = { recipientPhone = it; addressValidationError = null },
                            label = { Text("Phone *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = streetAddress,
                        onValueChange = { streetAddress = it; addressValidationError = null },
                        label = { Text("Street Address & Landmark *") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = shippingCity,
                            onValueChange = { shippingCity = it; addressValidationError = null },
                            label = { Text("City / Area *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = shippingState,
                            onValueChange = { shippingState = it; addressValidationError = null },
                            label = { Text("State / Country *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
            }

            if (addressValidationError != null) {
                BoutiqueErrorBanner(message = addressValidationError!!)
            }

            Text("Select Delivery Zone", style = MaterialTheme.typography.titleMedium)
            deliveryZones.forEach { zone ->
                Surface(
                    onClick = { selectedZoneId = zone.id },
                    shape = MaterialTheme.shapes.medium,
                    color = if (zone.id == selectedZone.id) ZeecrownSoftLavender.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(
                        width = if (zone.id == selectedZone.id) 1.5.dp else 1.dp,
                        color = if (zone.id == selectedZone.id) MaterialTheme.colorScheme.primary else ZeecrownElegantSilver,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = zone.id == selectedZone.id,
                            onClick = { selectedZoneId = zone.id },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(zone.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${zone.estimatedDays} • ${zone.country}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = formatNgnAmount(zone.feeNgn),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }

            HorizontalDivider(color = ZeecrownElegantSilver)

            // Payment Method Selection
            Text("Payment Method", style = MaterialTheme.typography.titleLarge)
            val enabledMethods = remember(storeSettings) {
                PaymentMethod.entries.filter { method ->
                    when (method) {
                        PaymentMethod.PAYSTACK -> storeSettings.paystackEnabled
                        PaymentMethod.FLUTTERWAVE -> storeSettings.flutterwaveEnabled
                        PaymentMethod.BANK_TRANSFER -> storeSettings.bankTransferEnabled
                        PaymentMethod.COD -> storeSettings.codEnabled
                    }
                }.ifEmpty { listOf(PaymentMethod.BANK_TRANSFER) }
            }
            LaunchedEffect(enabledMethods) {
                if (selectedPayment !in enabledMethods) {
                    selectedPayment = enabledMethods.first()
                }
            }
            enabledMethods.forEach { method ->
                Surface(
                    onClick = { selectedPayment = method },
                    modifier = Modifier.testTag("payment_method_${method.name}"),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selectedPayment == method) ZeecrownSoftLavender.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(
                        width = if (selectedPayment == method) 1.5.dp else 1.dp,
                        color = if (selectedPayment == method) MaterialTheme.colorScheme.primary else ZeecrownElegantSilver,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedPayment == method,
                            onClick = { selectedPayment = method },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(method.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = method.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (selectedPayment == PaymentMethod.BANK_TRANSFER) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Bank Transfer Instructions", style = MaterialTheme.typography.titleSmall)
                        Text("Bank: ${storeSettings.bankName.ifBlank { "Configured in Admin Store Settings" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Account Name: ${storeSettings.accountName.ifBlank { "ZEECROWN STORE" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Account No: ${storeSettings.accountNumber.ifBlank { "Pending setup" }}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = transferNote,
                            onValueChange = { transferNote = it },
                            label = { Text("Sender Name / Transfer Reference (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            } else if (selectedPayment == PaymentMethod.COD) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Cash on Delivery", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Your order will be verified by concierge dispatch prior to cash collection at delivery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                val configuredKey = if (selectedPayment == PaymentMethod.PAYSTACK) {
                    storeSettings.paystackPublicKey
                } else {
                    storeSettings.flutterwavePublicKey
                }
                val isPlaceholderKey = configuredKey.isBlank() || configuredKey.contains("configure", ignoreCase = true)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${selectedPayment.label} Gateway Status",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = if (isPlaceholderKey) {
                                "Placing this order records an unpaid ${selectedPayment.label} transaction in Firestore. Configure your live ${selectedPayment.label} public key in Admin Settings and connect a server-side verification webhook to complete live card charges."
                            } else {
                                "Public key configured (${configuredKey.take(14)}...). Placing this order registers a pending ${selectedPayment.label} transaction reference for server-side verification."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Order Summary
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, ZeecrownElegantSilver),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryRow("Subtotal (${cartItems.sumOf { it.quantity }} items)", formatNgnAmount(subtotal))
                    SummaryRow("Delivery (${selectedZone.name})", formatNgnAmount(deliveryFee))
                    HorizontalDivider(color = ZeecrownElegantSilver)
                    SummaryRow("Total (${currency.code})", formatNgnAmount(total), bold = true)
                }
            }

            Button(
                onClick = {
                    addressValidationError = null
                    val addressSnapshot = if (chosenSavedAddress != null) {
                        chosenSavedAddress
                    } else {
                        val err = AuthValidation.validateDeliveryAddress(
                            label = "Checkout",
                            recipientName = recipientName,
                            phone = recipientPhone,
                            country = "Nigeria",
                            state = shippingState,
                            city = shippingCity,
                            streetAddress = streetAddress,
                        )
                        if (err != null) {
                            addressValidationError = err
                            return@Button
                        }
                        DeliveryAddress(
                            label = "Checkout",
                            recipientName = recipientName.trim(),
                            phone = AuthValidation.normalizePhone(recipientPhone),
                            country = "Nigeria",
                            state = shippingState.trim(),
                            city = shippingCity.trim(),
                            streetAddress = streetAddress.trim(),
                        )
                    }
                    onPlaceOrder(
                        selectedZone,
                        activeCity,
                        activeState,
                        selectedPayment,
                        transferNote,
                        addressSnapshot,
                    )
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("place_order_button"),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZeecrownRoyalPurple,
                    contentColor = ZeecrownPureWhite,
                ),
            ) {
                Text(
                    text = if (isSubmitting) "Creating Order..." else "Confirm & Place Order",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    if (showAddAddressDialog) {
        DeliveryAddressFormDialog(
            initialAddress = DeliveryAddress(
                label = if (savedAddresses.isEmpty()) "Home" else "Office",
                recipientName = recipientName,
                phone = recipientPhone,
                country = "Nigeria",
                state = shippingState.ifBlank { "Lagos" },
                city = shippingCity,
                streetAddress = streetAddress,
                defaultAddress = savedAddresses.isEmpty(),
            ),
            isSubmitting = isSubmitting,
            onDismiss = { showAddAddressDialog = false },
            onSave = { draft ->
                onSaveNewAddress(draft) { savedId ->
                    selectedAddressId = savedId
                    useCustomAddress = false
                    addressValidationError = null
                    showAddAddressDialog = false
                }
            },
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
