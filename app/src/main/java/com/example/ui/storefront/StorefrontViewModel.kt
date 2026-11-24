package com.example.ui.storefront

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.analytics.AnalyticsTracker
import com.example.data.firebase.AuthManager
import com.example.data.firebase.UiState
import com.example.data.model.AiAssistantConfig
import com.example.data.model.AiChatMessage
import com.example.data.model.AiConversation
import com.example.data.model.AiFaqEntry
import com.example.data.model.AiResponseSource
import com.example.data.model.AiStoreSnapshot
import com.example.data.model.AppNotification
import com.example.data.model.CartItem
import com.example.data.model.CatalogFilterMode
import com.example.data.model.CatalogFilterState
import com.example.data.model.CatalogSortOption
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.DeliveryAddress
import com.example.data.model.DeliveryFeedback
import com.example.data.model.DeliverySettings
import com.example.data.model.DeliveryZone
import com.example.data.model.HomepageContent
import com.example.data.model.InventoryRecord
import com.example.data.model.NotificationPreferences
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderTrackingProjection
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.SizeGuide
import com.example.data.model.StoreCollection
import com.example.data.model.StoreCurrency
import com.example.data.model.StoreSettings
import com.example.data.model.SupportCategory
import com.example.data.model.SupportEnquiry
import com.example.data.model.SupportReply
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CommunicationRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.GuestSessionRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StorefrontViewModel(
    private val catalogRepository: CatalogRepository,
    private val customerRepository: CustomerRepository,
    private val guestSessionRepository: GuestSessionRepository,
    private val communicationRepository: CommunicationRepository,
    private val aiRepository: AiAssistantRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val isEmailVerified: Boolean,
    private val initialFirstName: String = "",
    private val initialLastName: String = "",
    private val initialPhone: String = "",
) : ViewModel() {

    val isSignedIn: Boolean = currentUserId.isNotBlank()

    private val _filterState = MutableStateFlow(CatalogFilterState())
    val filterState: StateFlow<CatalogFilterState> = _filterState.asStateFlow()

    private val _productPageLimit = MutableStateFlow(DEFAULT_PAGE_SIZE)
    val productPageLimit: StateFlow<Long> = _productPageLimit.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _newsletterSubmitting = MutableStateFlow(false)
    val newsletterSubmitting: StateFlow<Boolean> = _newsletterSubmitting.asStateFlow()

    private val _newsletterFeedback = MutableStateFlow<Pair<Boolean, String>?>(null)
    val newsletterFeedback: StateFlow<Pair<Boolean, String>?> = _newsletterFeedback.asStateFlow()

    private val _verificationEmailVerified = MutableStateFlow(isEmailVerified)
    val verificationEmailVerified: StateFlow<Boolean> = _verificationEmailVerified.asStateFlow()

    private val _verificationResendCooldown = MutableStateFlow(0)
    val verificationResendCooldown: StateFlow<Int> = _verificationResendCooldown.asStateFlow()

    private val _isMergingGuestData = MutableStateFlow(false)
    val isMergingGuestData: StateFlow<Boolean> = _isMergingGuestData.asStateFlow()

    init {
        if (isSignedIn) {
            viewModelScope.launch {
                val initResult = customerRepository.ensureUserProfileInitialized(
                    userId = currentUserId,
                    displayName = userDisplayName,
                    email = userEmail,
                    firstName = initialFirstName,
                    lastName = initialLastName,
                    phone = initialPhone,
                    emailVerified = isEmailVerified,
                )
                if (initResult.isSuccess) {
                    syncGuestBagAndWishlistOnSignIn()
                }
            }
        }
    }

    private suspend fun syncGuestBagAndWishlistOnSignIn() {
        val (guestCart, guestWishlist) = guestSessionRepository.peekGuestCartAndWishlist()
        if (guestCart.isEmpty() && guestWishlist.isEmpty()) return

        _isMergingGuestData.value = true
        try {
            val mergeRes = customerRepository.mergeGuestData(
                userId = currentUserId,
                guestCart = guestCart,
                guestWishlist = guestWishlist,
            )
            mergeRes.onSuccess { summary ->
                guestSessionRepository.clearGuestCartAndWishlist()
                if (summary.totalSyncedCount > 0 || summary.warnings.isNotEmpty()) {
                    val baseMsg = buildString {
                        if (summary.addedCartItems + summary.mergedCartItems > 0) {
                            append("Synced ${summary.addedCartItems + summary.mergedCartItems} bag item(s)")
                        }
                        if (summary.addedWishlistItems > 0) {
                            if (isNotEmpty()) append(" and ")
                            append("${summary.addedWishlistItems} saved wishlist item(s)")
                        }
                        if (isNotEmpty()) append(" to your account.")
                        if (summary.warnings.isNotEmpty()) {
                            if (isNotEmpty()) append(" ")
                            append(summary.warnings.first())
                        }
                    }
                    if (baseMsg.isNotBlank()) _snackbarMessage.value = baseMsg
                }
            }
        } finally {
            _isMergingGuestData.value = false
        }
    }

    private fun <T> Flow<T>.toUiState(tag: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tag", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $tag"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val productsState: StateFlow<UiState<List<Product>>> =
        catalogRepository.observeProducts().toUiState("products")

    val pagedProductsState: StateFlow<UiState<List<Product>>> =
        _productPageLimit
            .flatMapLatest { limit -> catalogRepository.observePagedProducts(limit) }
            .toUiState("pagedProducts")

    val categoriesState: StateFlow<UiState<List<Category>>> =
        catalogRepository.observeCategories().toUiState("categories")

    val collectionsState: StateFlow<UiState<List<StoreCollection>>> =
        catalogRepository.observeCollections().toUiState("collections")

    val sizeGuidesState: StateFlow<UiState<List<SizeGuide>>> =
        catalogRepository.observeSizeGuides().toUiState("sizeGuides")

    val homepageContentState: StateFlow<UiState<List<HomepageContent>>> =
        catalogRepository.observeHomepageContent().toUiState("homepage")

    val deliveryZonesState: StateFlow<UiState<List<DeliveryZone>>> =
        catalogRepository.observeDeliveryZones().toUiState("deliveryZones")

    val inventoryState: StateFlow<UiState<Map<String, InventoryRecord>>> =
        catalogRepository.observeInStockInventory().toUiState("inventory")

    val storeSettingsState: StateFlow<UiState<StoreSettings>> =
        catalogRepository.observePublicStoreSettings().toUiState("storeSettings")

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        if (isSignedIn) customerRepository.observeUserProfile(currentUserId).toUiState("profile")
        else flowOf<UserProfile?>(null).toUiState("profile")

    val userPrivateInfoState: StateFlow<UiState<UserPrivateInfo?>> =
        if (isSignedIn) customerRepository.observeUserPrivateInfo(currentUserId).toUiState("privateInfo")
        else flowOf<UserPrivateInfo?>(null).toUiState("privateInfo")

    val deliveryAddressesState: StateFlow<UiState<List<DeliveryAddress>>> =
        if (isSignedIn) customerRepository.observeDeliveryAddresses(currentUserId).toUiState("addresses")
        else flowOf(emptyList<DeliveryAddress>()).toUiState("addresses")

    val cartState: StateFlow<UiState<List<CartItem>>> =
        if (isSignedIn) customerRepository.observeCart(currentUserId).toUiState("cart")
        else guestSessionRepository.guestCartFlow.toUiState("guestCart")

    val wishlistState: StateFlow<UiState<List<WishlistItem>>> =
        if (isSignedIn) customerRepository.observeWishlist(currentUserId).toUiState("wishlist")
        else guestSessionRepository.guestWishlistFlow.toUiState("guestWishlist")

    val guestCurrencyState: StateFlow<StoreCurrency> =
        guestSessionRepository.guestCurrencyFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StoreCurrency.NGN,
        )

    val ordersState: StateFlow<UiState<List<Order>>> =
        if (isSignedIn) customerRepository.observeOrders(currentUserId).toUiState("orders")
        else flowOf(emptyList<Order>()).toUiState("orders")

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        if (isSignedIn) customerRepository.observeNotifications(currentUserId).toUiState("notifications")
        else flowOf(emptyList<AppNotification>()).toUiState("notifications")

    val unreadNotificationsCount: StateFlow<Int> = notificationsState
        .map { state -> (state as? UiState.Success)?.data?.count { !it.readFlag } ?: 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0,
        )

    val notificationPreferencesState: StateFlow<UiState<NotificationPreferences>> =
        if (isSignedIn) communicationRepository.observeNotificationPreferences(currentUserId).toUiState("notifPrefs")
        else flowOf(NotificationPreferences()).toUiState("notifPrefs")

    val customerEnquiriesState: StateFlow<UiState<List<SupportEnquiry>>> =
        if (isSignedIn) communicationRepository.observeCustomerEnquiries(currentUserId).toUiState("enquiries")
        else flowOf(emptyList<SupportEnquiry>()).toUiState("enquiries")

    val aiConfigState: StateFlow<AiAssistantConfig> = aiRepository.observePublicAiConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AiAssistantConfig())

    val aiFaqsState: StateFlow<List<AiFaqEntry>> = aiRepository.observeActiveFaqs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val aiDeliverySettingsState: StateFlow<DeliverySettings> = aiRepository.observeDeliverySettingsPublic()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DeliverySettings())

    val aiCouponsState: StateFlow<List<Coupon>> =
        if (isSignedIn) aiRepository.observeActiveCoupons()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
        else flowOf(emptyList<Coupon>())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val aiRecentConversationsState: StateFlow<List<AiConversation>> =
        if (isSignedIn) aiRepository.observeCustomerRecentConversations(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
        else flowOf(emptyList<AiConversation>())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _aiConversationId = MutableStateFlow<String?>(null)
    val aiConversationId: StateFlow<String?> = _aiConversationId.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _aiIsSending = MutableStateFlow(false)
    val aiIsSending: StateFlow<Boolean> = _aiIsSending.asStateFlow()

    private val _aiEscalatedTicketId = MutableStateFlow<String?>(null)
    val aiEscalatedTicketId: StateFlow<String?> = _aiEscalatedTicketId.asStateFlow()

    private val _aiHandoverError = MutableStateFlow<String?>(null)
    val aiHandoverError: StateFlow<String?> = _aiHandoverError.asStateFlow()

    private val _aiMarkedHelpful = MutableStateFlow(false)
    val aiMarkedHelpful: StateFlow<Boolean> = _aiMarkedHelpful.asStateFlow()

    private var lastAiSendMillis: Long = 0L
    private var activeConvMeta = AiConversation()

    val deliveryFeedbackState: StateFlow<UiState<List<DeliveryFeedback>>> =
        if (isSignedIn) communicationRepository.observeCustomerDeliveryFeedback(currentUserId).toUiState("deliveryFeedback")
        else flowOf(emptyList<DeliveryFeedback>()).toUiState("deliveryFeedback")

    private val _guestTrackingResultState = MutableStateFlow<UiState<OrderTrackingProjection>?>(null)
    val guestTrackingResultState: StateFlow<UiState<OrderTrackingProjection>?> = _guestTrackingResultState.asStateFlow()

    val allVisibleReviewsState: StateFlow<UiState<List<Review>>> =
        catalogRepository.observeAllVisibleReviews().toUiState("allReviews")

    fun observeEnquiryReplies(enquiryId: String): Flow<List<SupportReply>> =
        communicationRepository.observeSupportReplies(enquiryId)

    fun observeProductReviews(productId: String): Flow<List<Review>> =
        catalogRepository.observeReviewsForProduct(productId)

    fun observeProductById(productId: String): Flow<Product?> =
        catalogRepository.observeProductById(productId)

    fun observeProductByIdOrSlug(idOrSlug: String): Flow<Product?> =
        catalogRepository.observeProductByIdOrSlug(idOrSlug)

    fun observeInventoryForProduct(productId: String): Flow<InventoryRecord?> =
        catalogRepository.observeInventoryForProduct(productId)

    fun loadMoreProducts() {
        _productPageLimit.update { (it + DEFAULT_PAGE_SIZE).coerceAtMost(120L) }
    }

    fun setSelectedCategory(categoryId: String?) {
        _filterState.update {
            it.copy(
                selectedCategoryId = categoryId,
                selectedSubcategoryId = null,
            )
        }
    }

    fun setSelectedSubcategory(subcategoryId: String?) {
        _filterState.update { it.copy(selectedSubcategoryId = subcategoryId) }
    }

    fun setCatalogFilterMode(mode: CatalogFilterMode) {
        _filterState.update { current ->
            when (mode) {
                CatalogFilterMode.ALL -> current.copy(
                    newArrivalsOnly = false,
                    sortOption = CatalogSortOption.FEATURED,
                )
                CatalogFilterMode.NEW_ARRIVALS -> current.copy(
                    newArrivalsOnly = true,
                    sortOption = CatalogSortOption.NEWEST,
                )
                CatalogFilterMode.BEST_SELLERS -> current.copy(
                    newArrivalsOnly = false,
                    sortOption = CatalogSortOption.BEST_SELLING,
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun updateFilterState(transform: (CatalogFilterState) -> CatalogFilterState) {
        _filterState.update(transform)
    }

    fun clearAllFilters() {
        _filterState.value = CatalogFilterState()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun toggleCurrency(current: StoreCurrency) {
        val next = if (current == StoreCurrency.NGN) StoreCurrency.USD else StoreCurrency.NGN
        viewModelScope.launch {
            if (isSignedIn) {
                val res = customerRepository.updatePreferredCurrency(currentUserId, next)
                res.onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not switch currency."
                }
            } else {
                guestSessionRepository.setGuestCurrency(next)
            }
        }
    }

    fun addToCart(
        product: Product,
        variantLabel: String,
        quantity: Long = 1L,
        onAdded: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val knownStock = (inventoryState.value as? UiState.Success)?.data?.get(product.id)?.stockQuantity
            val res = if (isSignedIn) {
                customerRepository.addToCart(
                    userId = currentUserId,
                    product = product,
                    variantLabel = variantLabel,
                    quantity = quantity,
                    availableStock = knownStock,
                )
            } else {
                guestSessionRepository.addToGuestCart(
                    product = product,
                    variantLabel = variantLabel,
                    quantity = quantity,
                    availableStock = knownStock ?: 10L,
                )
            }
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    AnalyticsTracker.trackAddToCart(
                        productId = product.id,
                        title = product.title,
                        priceNgn = product.priceNgn,
                        quantity = quantity.toInt(),
                    )
                    _snackbarMessage.value = "Added ${product.title} to your bag."
                    onAdded()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not add item to bag."
                }
            )
        }
    }

    fun updateCartQuantity(itemId: String, newQuantity: Long) {
        viewModelScope.launch {
            val cartItems = (cartState.value as? UiState.Success)?.data.orEmpty()
            val productId = cartItems.firstOrNull { it.id == itemId }?.productId
            val knownStock = productId?.let { (inventoryState.value as? UiState.Success)?.data?.get(it)?.stockQuantity }
            val res = if (isSignedIn) {
                customerRepository.updateCartQuantity(currentUserId, itemId, newQuantity, knownStock)
            } else {
                guestSessionRepository.updateGuestCartQuantity(itemId, newQuantity, knownStock ?: 50L)
            }
            res.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Could not update quantity."
            }
        }
    }

    fun removeCartItem(itemId: String) {
        viewModelScope.launch {
            val cartItems = (cartState.value as? UiState.Success)?.data.orEmpty()
            val removedItem = cartItems.firstOrNull { it.id == itemId }
            if (isSignedIn) {
                val res = customerRepository.removeCartItem(currentUserId, itemId)
                res.onSuccess {
                    removedItem?.let {
                        AnalyticsTracker.trackRemoveFromCart(it.productId, it.productTitle, it.unitPriceNgn, it.quantity.toInt())
                    }
                }.onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not remove item."
                }
            } else {
                guestSessionRepository.removeGuestCartItem(itemId)
                removedItem?.let {
                    AnalyticsTracker.trackRemoveFromCart(it.productId, it.productTitle, it.unitPriceNgn, it.quantity.toInt())
                }
            }
        }
    }

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            if (isSignedIn) {
                val res = customerRepository.toggleWishlist(currentUserId, product)
                _snackbarMessage.value = res.fold(
                    onSuccess = { added ->
                        if (added) {
                            AnalyticsTracker.trackAddToWishlist(product)
                            "Saved ${product.title} to your wishlist."
                        } else {
                            "Removed ${product.title} from wishlist."
                        }
                    },
                    onFailure = { it.localizedMessage ?: "Could not update wishlist." }
                )
            } else {
                val added = guestSessionRepository.toggleGuestWishlist(product)
                if (added) AnalyticsTracker.trackAddToWishlist(product)
                _snackbarMessage.value = if (added) {
                    "Saved ${product.title} to your wishlist (Sign in to sync across devices)."
                } else {
                    "Removed ${product.title} from wishlist."
                }
            }
        }
    }

    fun updatePersonalProfile(firstName: String, lastName: String, phone: String) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = customerRepository.updatePersonalDetails(
                userId = currentUserId,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                email = userEmail,
            )
            _isSubmitting.value = false
            _snackbarMessage.value = res.fold(
                onSuccess = { "Personal information updated." },
                onFailure = { it.localizedMessage ?: "Could not update profile." }
            )
        }
    }

    fun uploadProfilePhoto(context: Context, imageUri: Uri) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = customerRepository.uploadProfilePhoto(context, currentUserId, imageUri)
            _isSubmitting.value = false
            _snackbarMessage.value = res.fold(
                onSuccess = { "Profile photo updated." },
                onFailure = { it.localizedMessage ?: "Could not upload profile photo." }
            )
        }
    }

    fun removeProfilePhoto() {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = customerRepository.removeProfilePhoto(currentUserId)
            _isSubmitting.value = false
            _snackbarMessage.value = res.fold(
                onSuccess = { "Profile photo removed." },
                onFailure = { it.localizedMessage ?: "Could not remove photo." }
            )
        }
    }

    fun saveDeliveryAddress(
        addressId: String?,
        draft: DeliveryAddress,
        onSaved: (String) -> Unit = {},
    ) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = customerRepository.saveDeliveryAddress(currentUserId, addressId, draft)
            _isSubmitting.value = false
            res.fold(
                onSuccess = { savedId ->
                    _snackbarMessage.value = "Delivery address saved."
                    onSaved(savedId)
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not save address."
                }
            )
        }
    }

    fun setDefaultAddress(addressId: String) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = customerRepository.setDefaultDeliveryAddress(currentUserId, addressId)
            _isSubmitting.value = false
            _snackbarMessage.value = res.fold(
                onSuccess = { "Default delivery address updated." },
                onFailure = { it.localizedMessage ?: "Could not update default address." }
            )
        }
    }

    fun deleteDeliveryAddress(addressId: String) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = customerRepository.deleteDeliveryAddress(currentUserId, addressId)
            _isSubmitting.value = false
            _snackbarMessage.value = res.fold(
                onSuccess = { "Address removed." },
                onFailure = { it.localizedMessage ?: "Could not remove address." }
            )
        }
    }

    fun resendVerificationEmail() {
        if (!isSignedIn || _verificationResendCooldown.value > 0) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = AuthManager.resendVerificationEmail()
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _snackbarMessage.value = "Verification link sent to ${userEmail.ifBlank { "your email" }}."
                    startResendCooldown()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not resend verification email."
                }
            )
        }
    }

    private fun startResendCooldown() {
        viewModelScope.launch {
            _verificationResendCooldown.value = 60
            while (_verificationResendCooldown.value > 0) {
                delay(1000L)
                _verificationResendCooldown.update { (it - 1).coerceAtLeast(0) }
            }
        }
    }

    fun refreshEmailVerificationStatus() {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = AuthManager.reloadCurrentUser()
            res.fold(
                onSuccess = { user ->
                    val verified = user?.isEmailVerified == true
                    val freshEmail = user?.email.orEmpty().ifBlank { userEmail }
                    _verificationEmailVerified.value = verified
                    customerRepository.syncEmailVerificationAndEmail(currentUserId, freshEmail, verified)
                    _snackbarMessage.value = if (verified) {
                        "Your email address is verified."
                    } else {
                        "Email is still awaiting verification. Open the link in your inbox, then refresh again."
                    }
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not refresh verification status."
                }
            )
            _isSubmitting.value = false
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
    ) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = AuthManager.changePassword(currentPassword, newPassword)
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _snackbarMessage.value = "Password updated successfully."
                    onSuccess()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not update password."
                }
            )
        }
    }

    fun requestEmailChange(
        currentPassword: String,
        newEmail: String,
        onSuccess: () -> Unit,
    ) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = AuthManager.requestEmailChange(currentPassword, newEmail)
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _snackbarMessage.value = "Verification link sent to $newEmail. Confirm it to complete your email change."
                    onSuccess()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not request email update."
                }
            )
        }
    }

    fun saveShippingProfile(
        displayName: String,
        email: String,
        phone: String,
        shippingAddress: String,
        city: String,
        state: String,
        country: String,
    ) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = customerRepository.saveShippingInfo(
                userId = currentUserId,
                displayName = displayName,
                email = email,
                phone = phone,
                shippingAddress = shippingAddress,
                city = city,
                state = state,
                country = country,
            )
            _isSubmitting.value = false
            _snackbarMessage.value = res.fold(
                onSuccess = { "Shipping details saved." },
                onFailure = { it.localizedMessage ?: "Could not save shipping details." }
            )
        }
    }

    fun placeOrder(
        cartItems: List<CartItem>,
        zone: DeliveryZone,
        shippingCity: String,
        shippingState: String,
        currency: StoreCurrency,
        paymentMethod: PaymentMethod,
        transferNote: String,
        selectedAddress: DeliveryAddress? = null,
        onOrderPlaced: (String) -> Unit,
    ) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val estimatedTotal = cartItems.sumOf { it.unitPriceNgn * it.quantity } + zone.feeNgn
            val res = customerRepository.placeOrder(
                userId = currentUserId,
                cartItems = cartItems,
                deliveryZoneId = zone.id,
                deliveryFeeNgn = zone.feeNgn,
                shippingCity = shippingCity,
                shippingState = shippingState,
                currency = currency,
                paymentMethod = paymentMethod,
                transferNote = transferNote,
                selectedAddress = selectedAddress,
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = { orderNumber ->
                    AnalyticsTracker.trackAddPaymentInfo(paymentMethod.name, estimatedTotal)
                    _snackbarMessage.value = "Order $orderNumber placed successfully."
                    onOrderPlaced(orderNumber)
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not place order."
                }
            )
        }
    }

    fun submitPaymentNotice(orderId: String) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = customerRepository.submitPaymentNotice(currentUserId, orderId)
            _isSubmitting.value = false
            _snackbarMessage.value = res.fold(
                onSuccess = { "Payment confirmation submitted for verification." },
                onFailure = { it.localizedMessage ?: "Could not submit payment notice." }
            )
        }
    }

    fun submitProductReview(
        productId: String,
        rating: Long,
        comment: String,
        orderId: String = "",
        onSubmitted: () -> Unit = {},
    ) {
        if (!isSignedIn) {
            _snackbarMessage.value = "Please sign in to publish a product review."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
            val authorName = profileName?.ifBlank { null }
                ?: userDisplayName.ifBlank { "Zeecrown Client" }
            val requireApproval = (storeSettingsState.value as? UiState.Success)?.data?.requireReviewApproval == true
            val res = customerRepository.submitReview(
                userId = currentUserId,
                productId = productId,
                authorName = authorName,
                rating = rating,
                comment = comment,
                orderId = orderId,
                requireApproval = requireApproval,
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _snackbarMessage.value = if (requireApproval) {
                        "Thank you — your review is submitted and awaiting store moderation."
                    } else {
                        "Thank you — your review is live."
                    }
                    onSubmitted()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not publish review."
                }
            )
        }
    }

    fun markNotificationRead(notificationId: String) {
        if (!isSignedIn) return
        viewModelScope.launch {
            communicationRepository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun markAllNotificationsRead() {
        if (!isSignedIn) return
        val current = (notificationsState.value as? UiState.Success)?.data.orEmpty()
        if (current.none { !it.readFlag }) return
        viewModelScope.launch {
            val res = communicationRepository.markAllNotificationsRead(currentUserId, current)
            res.onSuccess { count ->
                if (count > 0) _snackbarMessage.value = "Marked $count notification(s) as read."
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Could not update notifications."
            }
        }
    }

    fun saveNotificationPreferences(prefs: NotificationPreferences) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = communicationRepository.saveNotificationPreferences(currentUserId, prefs)
            _isSubmitting.value = false
            _snackbarMessage.value = res.fold(
                onSuccess = { "Notification preferences updated." },
                onFailure = { it.localizedMessage ?: "Could not save notification preferences." }
            )
        }
    }

    fun registerFcmToken(token: String) {
        if (!isSignedIn || token.isBlank()) return
        viewModelScope.launch {
            communicationRepository.registerDeviceToken(currentUserId, token)
        }
    }

    fun lookupGuestOrder(
        orderNumber: String,
        contact: String,
        trackingToken: String = "",
    ) {
        viewModelScope.launch {
            _guestTrackingResultState.value = UiState.Loading
            val res = communicationRepository.lookupGuestOrder(
                orderNumberInput = orderNumber,
                contactInput = contact,
                trackingTokenInput = trackingToken,
            )
            _guestTrackingResultState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Order could not be found.") }
            )
        }
    }

    fun clearGuestTrackingResult() {
        _guestTrackingResultState.value = null
    }

    fun retryFailedPayment(
        order: Order,
        paymentMethod: PaymentMethod,
        transferNote: String = "",
        onDone: () -> Unit = {},
    ) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = communicationRepository.retryFailedOrderPayment(
                userId = currentUserId,
                order = order,
                paymentMethod = paymentMethod,
                transferNote = transferNote,
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = { refCode ->
                    _snackbarMessage.value = "Payment retry ($refCode) recorded for ${order.orderNumber}."
                    onDone()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not retry payment."
                }
            )
        }
    }

    fun submitSupportEnquiry(
        category: SupportCategory,
        subject: String,
        message: String,
        orderId: String = "",
        orderNumber: String = "",
        onSubmitted: (String) -> Unit = {},
    ) {
        if (!isSignedIn) {
            _snackbarMessage.value = "Please sign in to open a concierge support request."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?.ifBlank { userDisplayName } ?: userDisplayName
            val res = communicationRepository.submitSupportEnquiry(
                userId = currentUserId,
                customerName = profileName,
                customerEmail = userEmail,
                category = category,
                subject = subject,
                message = message,
                orderId = orderId,
                orderNumber = orderNumber,
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = { enquiryId ->
                    _snackbarMessage.value = "Support request submitted. Our concierge will respond shortly."
                    onSubmitted(enquiryId)
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not submit support request."
                }
            )
        }
    }

    fun postCustomerSupportReply(
        enquiry: SupportEnquiry,
        message: String,
        onSent: () -> Unit = {},
    ) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?.ifBlank { userDisplayName } ?: userDisplayName
            val res = communicationRepository.postCustomerSupportReply(
                userId = currentUserId,
                enquiry = enquiry,
                senderName = profileName,
                message = message,
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _snackbarMessage.value = "Reply sent."
                    onSent()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not send reply."
                }
            )
        }
    }

    fun submitDeliveryFeedback(
        order: Order,
        overallRating: Long,
        deliveryRating: Long,
        packagingRating: Long,
        comment: String,
        onDone: () -> Unit = {},
    ) {
        if (!isSignedIn) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?.ifBlank { userDisplayName } ?: userDisplayName
            val res = communicationRepository.submitDeliveryFeedback(
                userId = currentUserId,
                order = order,
                customerName = profileName,
                overallRating = overallRating,
                deliveryRating = deliveryRating,
                packagingRating = packagingRating,
                comment = comment,
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _snackbarMessage.value = "Thank you for rating your ZEECROWN delivery!"
                    onDone()
                },
                onFailure = {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not save delivery feedback."
                }
            )
        }
    }

    fun clearNewsletterFeedback() {
        _newsletterFeedback.value = null
    }

    fun subscribeNewsletter(email: String) {
        if (!isSignedIn) {
            _newsletterFeedback.value = false to "Please sign in or create an account to subscribe to the Zeecrown Style List."
            return
        }
        viewModelScope.launch {
            _newsletterSubmitting.value = true
            _newsletterFeedback.value = null
            val res = customerRepository.subscribeNewsletter(currentUserId, email)
            _newsletterSubmitting.value = false
            res.fold(
                onSuccess = { newlySubscribed ->
                    val msg = if (newlySubscribed) {
                        "Welcome to the Zeecrown Style List — your subscription is confirmed."
                    } else {
                        "You are already subscribed to Zeecrown updates."
                    }
                    _newsletterFeedback.value = true to msg
                    _snackbarMessage.value = msg
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: "Please enter a valid email address."
                    _newsletterFeedback.value = false to msg
                }
            )
        }
    }

    // --- ZEECROWN AI Assistant ---

    private fun buildAiStoreSnapshot(): AiStoreSnapshot {
        val profile = (userProfileState.value as? UiState.Success)?.data
        return AiStoreSnapshot(
            config = aiConfigState.value,
            products = (productsState.value as? UiState.Success)?.data.orEmpty(),
            categories = (categoriesState.value as? UiState.Success)?.data.orEmpty(),
            collections = (collectionsState.value as? UiState.Success)?.data.orEmpty(),
            inventoryMap = (inventoryState.value as? UiState.Success)?.data.orEmpty(),
            deliveryZones = (deliveryZonesState.value as? UiState.Success)?.data.orEmpty(),
            deliverySettings = aiDeliverySettingsState.value,
            storeSettings = (storeSettingsState.value as? UiState.Success)?.data ?: StoreSettings(),
            coupons = aiCouponsState.value,
            faqs = aiFaqsState.value,
            customerOrders = if (isSignedIn) (ordersState.value as? UiState.Success)?.data.orEmpty() else emptyList(),
            isSignedIn = isSignedIn,
            currentUserId = currentUserId,
            customerFirstName = profile?.firstName?.ifBlank { userDisplayName.substringBefore(" ") } ?: initialFirstName,
        )
    }

    fun sendAiMessage(query: String, currency: StoreCurrency) {
        val clean = query.trim()
        if (clean.isBlank() || _aiIsSending.value) return
        val now = System.currentTimeMillis()
        if (now - lastAiSendMillis < 1200L) {
            _snackbarMessage.value = "Please wait a moment before sending the next message."
            return
        }
        lastAiSendMillis = now

        val cfg = aiConfigState.value
        if (!cfg.enabled) {
            _snackbarMessage.value = "ZEECROWN AI Assistant is currently paused by store administrators."
            return
        }

        val userMsg = AiChatMessage(
            id = "u_${now}",
            role = "USER",
            text = clean.take(800),
            timestampMillis = now,
        )
        val priorHistory = _aiMessages.value
        _aiMessages.value = priorHistory + userMsg
        _aiIsSending.value = true
        _aiHandoverError.value = null

        viewModelScope.launch {
            val snapshot = buildAiStoreSnapshot()
            val reply = aiRepository.generateAssistantReply(
                userQuery = clean,
                history = priorHistory,
                snapshot = snapshot,
                currency = currency,
            )
            _aiMessages.value = _aiMessages.value + reply
            _aiIsSending.value = false

            if (isSignedIn) {
                val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
                    ?.ifBlank { userDisplayName } ?: userDisplayName
                val persistedId = aiRepository.recordConversationTurn(
                    conversationId = _aiConversationId.value,
                    userId = currentUserId,
                    customerName = profileName,
                    userMessage = userMsg,
                    assistantMessage = reply,
                    existingRecommendedIds = activeConvMeta.recommendedProductIds,
                    previousMessageCount = activeConvMeta.messageCount,
                    previousAvgLatency = activeConvMeta.avgLatencyMs,
                    previousFailures = activeConvMeta.failureCount,
                    previousPromptTokens = activeConvMeta.estimatedPromptTokens,
                    previousCompletionTokens = activeConvMeta.estimatedCompletionTokens,
                    previousCostUsd = activeConvMeta.estimatedCostUsd,
                )
                if (!persistedId.isNullOrBlank()) {
                    _aiConversationId.value = persistedId
                    activeConvMeta = activeConvMeta.copy(
                        id = persistedId,
                        recommendedProductIds = (activeConvMeta.recommendedProductIds + reply.recommendedProducts.map { it.productId }).distinct().take(20),
                        messageCount = activeConvMeta.messageCount + 2L,
                        avgLatencyMs = if (reply.latencyMs > 0L) reply.latencyMs else activeConvMeta.avgLatencyMs,
                        failureCount = activeConvMeta.failureCount + (if (reply.failedAiCall) 1L else 0L),
                        estimatedPromptTokens = activeConvMeta.estimatedPromptTokens + reply.estimatedPromptTokens,
                        estimatedCompletionTokens = activeConvMeta.estimatedCompletionTokens + reply.estimatedCompletionTokens,
                        estimatedCostUsd = activeConvMeta.estimatedCostUsd + reply.estimatedCostUsd,
                    )
                }
            }
        }
    }

    fun startNewAiConversation() {
        _aiConversationId.value = null
        _aiMessages.value = emptyList()
        _aiEscalatedTicketId.value = null
        _aiHandoverError.value = null
        _aiMarkedHelpful.value = false
        activeConvMeta = AiConversation()
    }

    fun openSavedAiConversation(conv: AiConversation) {
        _aiConversationId.value = conv.id
        activeConvMeta = conv
        _aiEscalatedTicketId.value = conv.supportEnquiryId.takeIf { it.isNotBlank() }
        _aiMarkedHelpful.value = conv.resolvedHelpful == true
        _aiHandoverError.value = null
        viewModelScope.launch {
            _aiIsSending.value = true
            val res = aiRepository.loadConversationMessages(conv.id)
            _aiIsSending.value = false
            res.onSuccess { loaded ->
                _aiMessages.value = loaded
            }.onFailure {
                _snackbarMessage.value = "Could not load past messages."
            }
        }
    }

    fun markAiConversationHelpful() {
        _aiMarkedHelpful.value = true
        val convId = _aiConversationId.value ?: return
        viewModelScope.launch {
            aiRepository.markConversationResolvedHelpful(convId)
        }
    }

    fun trackAiRecommendationClick(productId: String) {
        val convId = _aiConversationId.value ?: return
        viewModelScope.launch {
            aiRepository.recordProductRecommendationClick(convId, productId)
        }
    }

    fun clearAiHandoverError() {
        _aiHandoverError.value = null
    }

    fun escalateAiToHuman(
        reason: String,
        category: SupportCategory,
        orderNumberInput: String,
        extraNote: String,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _aiHandoverError.value = null
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?.ifBlank { userDisplayName } ?: userDisplayName
            val matchedOrder = if (isSignedIn && orderNumberInput.isNotBlank()) {
                (ordersState.value as? UiState.Success)?.data?.firstOrNull {
                    it.orderNumber.equals(orderNumberInput.trim(), ignoreCase = true)
                }
            } else null

            val res = aiRepository.escalateConversationToHuman(
                conversationId = _aiConversationId.value,
                userId = currentUserId,
                customerName = profileName,
                customerEmail = userEmail,
                reason = reason,
                category = category,
                orderId = matchedOrder?.id.orEmpty(),
                orderNumber = matchedOrder?.orderNumber ?: orderNumberInput.trim(),
                messages = _aiMessages.value,
                customerExtraNote = extraNote,
            )
            _isSubmitting.value = false

            res.fold(
                onSuccess = { ticketId ->
                    _aiEscalatedTicketId.value = ticketId
                    val cfg = aiConfigState.value
                    val confirmText = buildString {
                        append(cfg.humanHandoverMessage.ifBlank { "Your conversation has been forwarded to ZEECROWN STORE human support." })
                        append("\n\nTicket Reference: #${ticketId.take(8).uppercase()}")
                        append("\nStatus: Waiting for Staff")
                        if (!cfg.humanSupportAvailable) {
                            append("\nNote: Our team is currently outside working hours (${cfg.supportWorkingDays}, ${cfg.supportWorkingHours}). ${cfg.expectedResponseTimeMessage}")
                        } else if (cfg.expectedResponseTimeMessage.isNotBlank()) {
                            append("\n${cfg.expectedResponseTimeMessage}")
                        }
                    }
                    _aiMessages.value = _aiMessages.value + AiChatMessage(
                        id = "sys_esc_${System.currentTimeMillis()}",
                        role = "SYSTEM",
                        text = confirmText,
                        escalatedTicketId = ticketId,
                        detectedCategory = "Human Handover",
                        responseSource = AiResponseSource.HUMAN_HANDOVER,
                    )
                    _snackbarMessage.value = "Escalated to ZEECROWN Concierge (#${ticketId.take(8).uppercase()})."
                },
                onFailure = { err ->
                    val settings = (storeSettingsState.value as? UiState.Success)?.data ?: StoreSettings()
                    val fallbackChannels = listOfNotNull(
                        settings.supportEmail.takeIf { it.isNotBlank() }?.let { "Email: $it" },
                        settings.whatsappNumber.takeIf { it.isNotBlank() }?.let { "WhatsApp: $it" },
                        settings.supportPhone.takeIf { it.isNotBlank() }?.let { "Phone: $it" },
                    ).joinToString(" · ")
                    val errMsg = buildString {
                        append(err.localizedMessage ?: "Unable to submit handover ticket right now.")
                        if (fallbackChannels.isNotBlank()) {
                            append(" Please reach us directly via: $fallbackChannels.")
                        }
                    }
                    _aiHandoverError.value = errMsg
                }
            )
        }
    }

    fun lookupGuestOrderFromAiChat(
        orderNumber: String,
        contact: String,
        trackingToken: String = "",
    ) {
        viewModelScope.launch {
            _aiIsSending.value = true
            val res = aiRepository.lookupVerifiedGuestOrderInChat(orderNumber, contact, trackingToken)
            _aiIsSending.value = false
            val reply = res.fold(
                onSuccess = { proj ->
                    val stLabel = OrderStatus.fromRaw(proj.status).label
                    val payLabel = PaymentStatus.fromRaw(proj.paymentStatus).label
                    val courier = if (proj.courierName.isNotBlank() || proj.trackingReference.isNotBlank()) {
                        "\nCourier: ${proj.courierName} ${proj.trackingReference}".trim()
                    } else ""
                    val eta = if (proj.estimatedDeliveryWindow.isNotBlank()) "\nETA: ${proj.estimatedDeliveryWindow}" else ""
                    AiChatMessage(
                        id = "ai_trk_${System.currentTimeMillis()}",
                        role = "ASSISTANT",
                        text = "Verified Order ${proj.orderNumber}:\nStatus: $stLabel (Payment: $payLabel)\nDestination: ${proj.maskedDestination}$courier$eta",
                        detectedCategory = "Order Tracking",
                        responseSource = AiResponseSource.AUTHENTICATED_ORDER_LOOKUP,
                    )
                },
                onFailure = { err ->
                    AiChatMessage(
                        id = "ai_trk_err_${System.currentTimeMillis()}",
                        role = "ASSISTANT",
                        text = err.localizedMessage ?: "We couldn't verify an order with those exact details. Double-check your Order Number and email/phone, or escalate to human support.",
                        suggestHumanHandover = true,
                        escalationReason = "Guest order verification failed",
                        detectedCategory = "Order Tracking",
                        responseSource = AiResponseSource.AUTHENTICATED_ORDER_LOOKUP,
                    )
                }
            )
            _aiMessages.value = _aiMessages.value + reply
        }
    }

    private companion object {
        const val TAG = "StorefrontVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val DEFAULT_PAGE_SIZE = 12L
    }
}
