package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.analytics.AnalyticsTracker
import com.example.data.firebase.AuthManager
import com.example.data.firebase.UiState
import com.example.data.model.AdminRole
import com.example.data.model.AiActionDestination
import com.example.data.model.BoutiqueContactHelper
import com.example.data.model.CatalogFilterMode
import com.example.data.model.Product
import com.example.data.model.SeoUtils
import com.example.data.model.StoreCurrency
import com.example.data.model.StoreSettings
import com.example.data.repository.AdminOperationsRepository
import com.example.data.repository.AdminRepository
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CommunicationRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.GuestSessionRepository
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.auth.AuthMode
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.RegistrationBootstrapData
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.ProductQuickViewDialog
import com.example.ui.components.launchWhatsAppChat
import com.example.ui.storefront.AccountAndOrdersScreen
import com.example.ui.storefront.AiAssistantScreen
import com.example.ui.storefront.BagAndWishlistScreen
import com.example.ui.storefront.CatalogScreen
import com.example.ui.storefront.CheckoutScreen
import com.example.ui.storefront.HomeScreen
import com.example.ui.storefront.NotificationPreferencesScreen
import com.example.ui.storefront.NotificationsCenterScreen
import com.example.ui.storefront.OrderTrackingScreen
import com.example.ui.storefront.ProductDetailScreen
import com.example.ui.storefront.StorefrontViewModel
import com.example.ui.storefront.SupportCenterScreen
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownMutedPlum
import com.example.ui.theme.ZeecrownObsidianInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory
import com.example.ui.theme.ZeecrownWhatsAppGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import java.net.URLEncoder

data class PendingDeepLink(
    val type: String,
    val id: String = "",
    val token: String = "",
)

object StoreRoutes {
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val BAG = "bag"
    const val ACCOUNT = "account"
    const val AUTH = "auth"
    const val CHECKOUT = "checkout"
    const val ADMIN = "admin"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val TRACK_ORDER = "track_order?orderId={orderId}&token={token}"
    const val NOTIFICATIONS = "notifications"
    const val NOTIFICATION_PREFS = "notification_prefs"
    const val SUPPORT = "support"
    const val AI_ASSISTANT = "ai_assistant"

    fun productDetail(productId: String): String = "product_detail/$productId"

    fun trackOrder(orderId: String = "", token: String = ""): String {
        val encOrder = URLEncoder.encode(orderId, "UTF-8")
        val encToken = URLEncoder.encode(token, "UTF-8")
        return "track_order?orderId=$encOrder&token=$encToken"
    }
}

private data class TopNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun ZeecrownAppRoot(
    pendingDeepLink: PendingDeepLink? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var pendingBootstrapData by remember { mutableStateOf(RegistrationBootstrapData()) }
    var pendingPostAuthRoute by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    // Silent Google credential restore if user previously signed in; never blocks guest browsing
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope,
            )
        }
    }

    val user = currentUser
    val sessionKey = user?.uid ?: "guest"
    val appContext = context.applicationContext
    val dbId = remember(appContext) { appContext.getString(R.string.firestore_database_id) }
    val roleFlow = remember(user?.uid, user?.email, user?.isEmailVerified, dbId) {
        if (user == null) {
            flowOf(AdminRole.NONE)
        } else {
            AdminRepository(FirebaseFirestore.getInstance(dbId)).observeCurrentAdminRole(user)
        }
    }
    val adminRole by roleFlow.collectAsStateWithLifecycle(
        initialValue = if (AuthManager.isBootstrappedAdmin(user)) AdminRole.SUPER_ADMIN else AdminRole.NONE
    )

    key(sessionKey) {
        ZeecrownStoreShell(
            uid = user?.uid.orEmpty(),
            displayName = user?.displayName.orEmpty(),
            email = user?.email.orEmpty(),
            isEmailVerified = user?.isEmailVerified == true,
            hasPasswordProvider = AuthManager.hasPasswordProvider(user),
            adminRole = adminRole,
            initialBootstrapData = pendingBootstrapData,
            pendingPostAuthRoute = pendingPostAuthRoute,
            onConsumeBootstrapData = { pendingBootstrapData = RegistrationBootstrapData() },
            onSetPendingPostAuthRoute = { pendingPostAuthRoute = it },
            onClearPendingPostAuthRoute = { pendingPostAuthRoute = null },
            onAuthCompleted = { bootstrap ->
                pendingBootstrapData = bootstrap
                currentUser = Firebase.auth.currentUser
            },
            pendingDeepLink = pendingDeepLink,
            onDeepLinkHandled = onDeepLinkHandled,
        )
    }
}

@Composable
private fun ZeecrownStoreShell(
    uid: String,
    displayName: String,
    email: String,
    isEmailVerified: Boolean,
    hasPasswordProvider: Boolean,
    adminRole: AdminRole,
    initialBootstrapData: RegistrationBootstrapData,
    pendingPostAuthRoute: String?,
    onConsumeBootstrapData: () -> Unit,
    onSetPendingPostAuthRoute: (String?) -> Unit,
    onClearPendingPostAuthRoute: () -> Unit,
    onAuthCompleted: (RegistrationBootstrapData) -> Unit,
    pendingDeepLink: PendingDeepLink?,
    onDeepLinkHandled: () -> Unit,
) {
    val isAdmin = adminRole.isAdmin
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val guestSessionRepository = remember(context) { GuestSessionRepository(context) }
    val isSignedIn = uid.isNotBlank()

    val storefrontVm: StorefrontViewModel = viewModel(
        key = "storefront_${uid.ifBlank { "guest" }}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                StorefrontViewModel(
                    catalogRepository = CatalogRepository(db),
                    customerRepository = CustomerRepository(db),
                    guestSessionRepository = guestSessionRepository,
                    communicationRepository = CommunicationRepository(db),
                    aiRepository = AiAssistantRepository(db),
                    currentUserId = uid,
                    userDisplayName = displayName,
                    userEmail = email,
                    isEmailVerified = isEmailVerified,
                    initialFirstName = initialBootstrapData.firstName,
                    initialLastName = initialBootstrapData.lastName,
                    initialPhone = initialBootstrapData.phone,
                )
            }
        }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && uid.isNotBlank()) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                storefrontVm.registerFcmToken(token)
            }
        }
    }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            onConsumeBootstrapData()
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                storefrontVm.registerFcmToken(token)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    val adminVm: AdminViewModel? = if (isAdmin) {
        val vm: AdminViewModel = viewModel(
            key = "admin_$uid",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    val dbId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(dbId)
                    AdminViewModel(
                        db = db,
                        adminRepository = AdminRepository(db),
                        opsRepository = AdminOperationsRepository(db),
                        catalogRepository = CatalogRepository(db),
                        aiRepository = AiAssistantRepository(db),
                        initialRole = adminRole,
                    )
                }
            }
        )
        LaunchedEffect(adminRole) {
            vm.updateCurrentRole(adminRole)
        }
        vm
    } else {
        null
    }

    val productsState by storefrontVm.productsState.collectAsStateWithLifecycle()
    val pagedProductsState by storefrontVm.pagedProductsState.collectAsStateWithLifecycle()
    val categoriesState by storefrontVm.categoriesState.collectAsStateWithLifecycle()
    val collectionsState by storefrontVm.collectionsState.collectAsStateWithLifecycle()
    val sizeGuidesState by storefrontVm.sizeGuidesState.collectAsStateWithLifecycle()
    val homepageState by storefrontVm.homepageContentState.collectAsStateWithLifecycle()
    val deliveryZonesState by storefrontVm.deliveryZonesState.collectAsStateWithLifecycle()
    val inventoryState by storefrontVm.inventoryState.collectAsStateWithLifecycle()
    val storeSettingsState by storefrontVm.storeSettingsState.collectAsStateWithLifecycle()
    val userProfileState by storefrontVm.userProfileState.collectAsStateWithLifecycle()
    val privateInfoState by storefrontVm.userPrivateInfoState.collectAsStateWithLifecycle()
    val deliveryAddressesState by storefrontVm.deliveryAddressesState.collectAsStateWithLifecycle()
    val cartState by storefrontVm.cartState.collectAsStateWithLifecycle()
    val wishlistState by storefrontVm.wishlistState.collectAsStateWithLifecycle()
    val ordersState by storefrontVm.ordersState.collectAsStateWithLifecycle()
    val notificationsState by storefrontVm.notificationsState.collectAsStateWithLifecycle()
    val unreadNotificationCount by storefrontVm.unreadNotificationsCount.collectAsStateWithLifecycle()
    val notificationPreferencesState by storefrontVm.notificationPreferencesState.collectAsStateWithLifecycle()
    val customerEnquiriesState by storefrontVm.customerEnquiriesState.collectAsStateWithLifecycle()
    val deliveryFeedbackState by storefrontVm.deliveryFeedbackState.collectAsStateWithLifecycle()
    val guestTrackingResultState by storefrontVm.guestTrackingResultState.collectAsStateWithLifecycle()
    val allVisibleReviewsState by storefrontVm.allVisibleReviewsState.collectAsStateWithLifecycle()
    val filterState by storefrontVm.filterState.collectAsStateWithLifecycle()
    val productPageLimit by storefrontVm.productPageLimit.collectAsStateWithLifecycle()
    val snackbarMessage by storefrontVm.snackbarMessage.collectAsStateWithLifecycle()
    val isSubmitting by storefrontVm.isSubmitting.collectAsStateWithLifecycle()
    val isMergingGuestData by storefrontVm.isMergingGuestData.collectAsStateWithLifecycle()
    val newsletterSubmitting by storefrontVm.newsletterSubmitting.collectAsStateWithLifecycle()
    val newsletterFeedback by storefrontVm.newsletterFeedback.collectAsStateWithLifecycle()
    val guestCurrency by storefrontVm.guestCurrencyState.collectAsStateWithLifecycle()
    val verifiedEmailStatus by storefrontVm.verificationEmailVerified.collectAsStateWithLifecycle()
    val verificationCooldown by storefrontVm.verificationResendCooldown.collectAsStateWithLifecycle()
    val aiConfig by storefrontVm.aiConfigState.collectAsStateWithLifecycle()
    val aiMessages by storefrontVm.aiMessages.collectAsStateWithLifecycle()
    val aiRecentConversations by storefrontVm.aiRecentConversationsState.collectAsStateWithLifecycle()
    val aiIsSending by storefrontVm.aiIsSending.collectAsStateWithLifecycle()
    val aiEscalatedTicketId by storefrontVm.aiEscalatedTicketId.collectAsStateWithLifecycle()
    val aiHandoverError by storefrontVm.aiHandoverError.collectAsStateWithLifecycle()
    val aiMarkedHelpful by storefrontVm.aiMarkedHelpful.collectAsStateWithLifecycle()

    val currency = if (isSignedIn) {
        StoreCurrency.fromRaw((userProfileState as? UiState.Success)?.data?.preferredCurrency)
    } else {
        guestCurrency
    }
    val userProfile = (userProfileState as? UiState.Success)?.data
    val products = (productsState as? UiState.Success)?.data.orEmpty()
    val categoriesList = (categoriesState as? UiState.Success)?.data.orEmpty()
    val collectionsList = (collectionsState as? UiState.Success)?.data.orEmpty()
    val sizeGuidesList = (sizeGuidesState as? UiState.Success)?.data.orEmpty()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()
    val publicSiteBaseUrl = remember(storeSettings.publicSiteBaseUrl) {
        SeoUtils.normalizeBaseUrl(storeSettings.publicSiteBaseUrl)
    }
    val wishlistItems = (wishlistState as? UiState.Success)?.data.orEmpty()
    val wishlistIds = remember(wishlistItems) { wishlistItems.map { it.productId }.toSet() }
    val cartItems = (cartState as? UiState.Success)?.data.orEmpty()
    val bagBadgeCount = cartItems.sumOf { it.quantity }.toInt()
    val deliveryZones = (deliveryZonesState as? UiState.Success)?.data.orEmpty()
    val savedAddresses = (deliveryAddressesState as? UiState.Success)?.data.orEmpty()
    val inventoryMap = (inventoryState as? UiState.Success)?.data.orEmpty()
    val visibleReviews = (allVisibleReviewsState as? UiState.Success)?.data.orEmpty()
    val ratingsByProduct = remember(visibleReviews) {
        visibleReviews.groupBy { it.productId }.mapValues { (_, list) ->
            list.map { it.rating }.average() to list.size
        }
    }

    var bagInitialTab by remember { mutableIntStateOf(0) }
    var bagTabNonce by remember { mutableIntStateOf(0) }
    var catalogSearchFocusNonce by remember { mutableIntStateOf(0) }
    var quickViewProduct by remember { mutableStateOf<Product?>(null) }
    var showCustomerCareHub by remember { mutableStateOf(false) }
    var authInitialMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var authGuestHint by remember { mutableStateOf<String?>(null) }
    var analyticsConsentAnswered by remember { mutableStateOf(false) }
    var analyticsConsentGranted by remember { mutableStateOf(false) }
    var unresolvedCategoryDeepLink by remember { mutableStateOf<String?>(null) }
    var unresolvedCollectionDeepLink by remember { mutableStateOf<String?>(null) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: StoreRoutes.HOME

    LaunchedEffect(storeSettings.ga4MeasurementId, storeSettings.analyticsConsentRequired, analyticsConsentGranted) {
        AnalyticsTracker.configure(
            measurementId = storeSettings.ga4MeasurementId,
            requireConsent = storeSettings.analyticsConsentRequired,
        )
        AnalyticsTracker.setConsentGranted(analyticsConsentGranted)
    }

    LaunchedEffect(currentRoute) {
        AnalyticsTracker.trackScreenView(currentRoute)
    }

    LaunchedEffect(ordersState) {
        val ordersList = (ordersState as? UiState.Success)?.data.orEmpty()
        ordersList.forEach { order ->
            AnalyticsTracker.trackVerifiedPurchase(order)
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openAuthFlow(mode: AuthMode, hint: String?, postAuthTarget: String?) {
        authInitialMode = mode
        authGuestHint = hint
        onSetPendingPostAuthRoute(postAuthTarget)
        navController.navigate(StoreRoutes.AUTH) {
            launchSingleTop = true
        }
    }

    LaunchedEffect(isSignedIn, pendingPostAuthRoute) {
        val target = pendingPostAuthRoute ?: return@LaunchedEffect
        if (isSignedIn) {
            onClearPendingPostAuthRoute()
            if (target == StoreRoutes.CHECKOUT) {
                navController.navigate(StoreRoutes.CHECKOUT) {
                    launchSingleTop = true
                }
            } else {
                navigateTopLevel(target)
            }
        }
    }

    fun navigateNotificationTargetRoute(rawRoute: String) {
        val clean = rawRoute.trim()
        when {
            clean.startsWith("track_order/") -> {
                val id = clean.removePrefix("track_order/").trim()
                navController.navigate(StoreRoutes.trackOrder(orderId = id)) { launchSingleTop = true }
            }
            clean == "track_order" -> {
                navController.navigate(StoreRoutes.trackOrder()) { launchSingleTop = true }
            }
            clean == "notifications" -> {
                navController.navigate(StoreRoutes.NOTIFICATIONS) { launchSingleTop = true }
            }
            clean == "notification_prefs" -> {
                navController.navigate(StoreRoutes.NOTIFICATION_PREFS) { launchSingleTop = true }
            }
            clean == "support" -> {
                navController.navigate(StoreRoutes.SUPPORT) { launchSingleTop = true }
            }
            clean == "ai" || clean == "ai_assistant" -> {
                navController.navigate(StoreRoutes.AI_ASSISTANT) { launchSingleTop = true }
            }
            clean.startsWith("product/") -> {
                val id = clean.removePrefix("product/").trim()
                if (id.isNotBlank()) {
                    navController.navigate(StoreRoutes.productDetail(id)) { launchSingleTop = true }
                }
            }
            clean == "account" -> navigateTopLevel(StoreRoutes.ACCOUNT)
            else -> navigateTopLevel(StoreRoutes.HOME)
        }
    }

    LaunchedEffect(pendingDeepLink) {
        val link = pendingDeepLink ?: return@LaunchedEffect
        when (link.type) {
            "product" -> if (link.id.isNotBlank()) {
                navController.navigate(StoreRoutes.productDetail(link.id)) { launchSingleTop = true }
            }
            "category" -> if (link.id.isNotBlank()) {
                unresolvedCategoryDeepLink = link.id
            }
            "collection" -> if (link.id.isNotBlank()) {
                unresolvedCollectionDeepLink = link.id
            }
            "order" -> {
                navController.navigate(StoreRoutes.trackOrder(orderId = link.id)) { launchSingleTop = true }
            }
            "track" -> {
                navController.navigate(StoreRoutes.trackOrder(orderId = link.id, token = link.token)) { launchSingleTop = true }
            }
            "notifications" -> {
                navController.navigate(StoreRoutes.NOTIFICATIONS) { launchSingleTop = true }
            }
            "support" -> {
                navController.navigate(StoreRoutes.SUPPORT) { launchSingleTop = true }
            }
            "ai", "ai_assistant" -> {
                navController.navigate(StoreRoutes.AI_ASSISTANT) { launchSingleTop = true }
            }
        }
        onDeepLinkHandled()
    }

    LaunchedEffect(unresolvedCategoryDeepLink, categoriesList) {
        val target = unresolvedCategoryDeepLink ?: return@LaunchedEffect
        if (categoriesList.isNotEmpty()) {
            val matched = categoriesList.firstOrNull {
                it.id.equals(target, ignoreCase = true) || it.slug.equals(target, ignoreCase = true)
            }
            if (matched != null) {
                storefrontVm.setSelectedCategory(matched.id)
                navigateTopLevel(StoreRoutes.CATALOG)
            } else {
                navigateTopLevel(StoreRoutes.CATALOG)
            }
            unresolvedCategoryDeepLink = null
        }
    }

    LaunchedEffect(unresolvedCollectionDeepLink, collectionsList) {
        val target = unresolvedCollectionDeepLink ?: return@LaunchedEffect
        if (collectionsList.isNotEmpty()) {
            val matched = collectionsList.firstOrNull {
                it.id.equals(target, ignoreCase = true) || it.slug.equals(target, ignoreCase = true)
            }
            if (matched != null) {
                storefrontVm.updateFilterState { it.copy(selectedCollectionId = matched.id, selectedCategoryId = null) }
            }
            navigateTopLevel(StoreRoutes.CATALOG)
            unresolvedCollectionDeepLink = null
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        storefrontVm.clearSnackbar()
    }

    val homeLabel = stringResource(R.string.nav_home)
    val navItems = remember(isAdmin, homeLabel) {
        buildList {
            add(TopNavItem(StoreRoutes.HOME, homeLabel, Icons.Filled.Storefront, Icons.Outlined.Storefront))
            add(TopNavItem(StoreRoutes.CATALOG, "Shop", Icons.Filled.GridView, Icons.Outlined.GridView))
            add(TopNavItem(StoreRoutes.BAG, "Bag", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag))
            add(TopNavItem(StoreRoutes.ACCOUNT, "Account", Icons.Filled.Person, Icons.Outlined.Person))
            if (isAdmin) {
                add(TopNavItem(StoreRoutes.ADMIN, "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings))
            }
        }
    }

    val showChromeNav = currentRoute in navItems.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavigationRail && showChromeNav) {
                    NavigationBar(
                        containerColor = ZeecrownPureWhite,
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (item.route == StoreRoutes.BAG) {
                                        bagInitialTab = 0
                                        bagTabNonce++
                                    }
                                    navigateTopLevel(item.route)
                                },
                                modifier = Modifier.testTag("nav_${item.route}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ZeecrownRoyalPurple,
                                    selectedTextColor = ZeecrownRoyalPurple,
                                    indicatorColor = ZeecrownSoftLavender,
                                    unselectedIconColor = ZeecrownMutedPlum,
                                    unselectedTextColor = ZeecrownMutedPlum,
                                ),
                                icon = {
                                    if (item.route == StoreRoutes.BAG && bagBadgeCount > 0) {
                                        BadgedBox(badge = { Badge(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite) { Text(bagBadgeCount.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (useNavigationRail && showChromeNav) {
                        NavigationRail(
                            containerColor = ZeecrownLightLilac,
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        if (item.route == StoreRoutes.BAG) {
                                            bagInitialTab = 0
                                            bagTabNonce++
                                        }
                                        navigateTopLevel(item.route)
                                    },
                                    modifier = Modifier.testTag("rail_${item.route}"),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ZeecrownRoyalPurple,
                                        selectedTextColor = ZeecrownRoyalPurple,
                                        indicatorColor = ZeecrownSoftLavender,
                                        unselectedIconColor = ZeecrownMutedPlum,
                                        unselectedTextColor = ZeecrownMutedPlum,
                                    ),
                                    icon = {
                                        if (item.route == StoreRoutes.BAG && bagBadgeCount > 0) {
                                            BadgedBox(badge = { Badge(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite) { Text(bagBadgeCount.toString()) } }) {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = item.label,
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                            )
                                        }
                                    },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = StoreRoutes.HOME,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                    composable(StoreRoutes.HOME) {
                        HomeScreen(
                            productsState = productsState,
                            categoriesState = categoriesState,
                            homepageState = homepageState,
                            inventoryState = inventoryState,
                            allReviewsState = allVisibleReviewsState,
                            deliveryZones = deliveryZones,
                            storeSettings = storeSettings,
                            wishlistItems = wishlistItems,
                            bagCount = bagBadgeCount,
                            unreadNotificationCount = unreadNotificationCount,
                            userEmail = email,
                            currency = currency,
                            isAdmin = isAdmin,
                            isNewsletterSubmitting = newsletterSubmitting,
                            newsletterFeedback = newsletterFeedback,
                            onToggleCurrency = { storefrontVm.toggleCurrency(currency) },
                            onOpenCatalog = { categoryId, filterMode, focusSearch ->
                                storefrontVm.setSelectedCategory(categoryId)
                                storefrontVm.setCatalogFilterMode(filterMode)
                                if (focusSearch) catalogSearchFocusNonce++
                                navigateTopLevel(StoreRoutes.CATALOG)
                            },
                            onProductClick = { productId ->
                                navController.navigate(StoreRoutes.productDetail(productId))
                            },
                            onToggleWishlist = { storefrontVm.toggleWishlist(it) },
                            onQuickAdd = { product ->
                                val defaultVariant = listOfNotNull(
                                    product.sizes.firstOrNull(),
                                    product.colors.firstOrNull()
                                ).joinToString(" • ").ifBlank { "Standard" }
                                storefrontVm.addToCart(product, defaultVariant)
                            },
                            onOpenWishlist = {
                                bagInitialTab = 1
                                bagTabNonce++
                                navigateTopLevel(StoreRoutes.BAG)
                            },
                            onOpenBag = {
                                bagInitialTab = 0
                                bagTabNonce++
                                navigateTopLevel(StoreRoutes.BAG)
                            },
                            onOpenAccount = { navigateTopLevel(StoreRoutes.ACCOUNT) },
                            onOpenNotifications = { navController.navigate(StoreRoutes.NOTIFICATIONS) },
                            onOpenOrderTracking = { navController.navigate(StoreRoutes.trackOrder()) },
                            onOpenSupport = { navController.navigate(StoreRoutes.SUPPORT) },
                            onOpenAdmin = {
                                if (isAdmin) navigateTopLevel(StoreRoutes.ADMIN)
                            },
                            onSubscribeNewsletter = storefrontVm::subscribeNewsletter,
                            onClearNewsletterFeedback = storefrontVm::clearNewsletterFeedback,
                            onQuickView = { quickViewProduct = it },
                        )
                    }

                    composable(StoreRoutes.CATALOG) {
                        LaunchedEffect(filterState.searchQuery) {
                            val q = filterState.searchQuery.trim()
                            if (q.length >= 3) {
                                delay(900L)
                                AnalyticsTracker.trackSearch(q, products.size)
                            }
                        }
                        CatalogScreen(
                            pagedProductsState = pagedProductsState,
                            allProductsForFacets = products,
                            categoriesState = categoriesState,
                            collectionsState = collectionsState,
                            inventoryState = inventoryState,
                            wishlistItems = wishlistItems,
                            filterState = filterState,
                            pageLimit = productPageLimit,
                            focusSearchNonce = catalogSearchFocusNonce,
                            ratingsByProduct = ratingsByProduct,
                            currency = currency,
                            onNavigateHome = { navigateTopLevel(StoreRoutes.HOME) },
                            onUpdateFilter = storefrontVm::updateFilterState,
                            onClearAllFilters = storefrontVm::clearAllFilters,
                            onLoadMore = storefrontVm::loadMoreProducts,
                            onToggleCurrency = { storefrontVm.toggleCurrency(currency) },
                            onProductClick = { productId ->
                                navController.navigate(StoreRoutes.productDetail(productId))
                            },
                            onQuickView = { quickViewProduct = it },
                            onToggleWishlist = { storefrontVm.toggleWishlist(it) },
                            onQuickAdd = { product ->
                                val defaultVariant = listOfNotNull(
                                    product.sizes.firstOrNull(),
                                    product.colors.firstOrNull()
                                ).joinToString(" • ").ifBlank { "Standard" }
                                storefrontVm.addToCart(product, defaultVariant)
                            },
                            publicSiteBaseUrl = publicSiteBaseUrl,
                            onShareCatalogTarget = { type, id, platform ->
                                AnalyticsTracker.trackShare(type, id, platform)
                            },
                        )
                    }

                    composable(
                        route = StoreRoutes.PRODUCT_DETAIL,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType }),
                    ) { entry ->
                        val productId = entry.arguments?.getString("productId").orEmpty()
                        val directProductFlow = remember(productId) {
                            storefrontVm.observeProductByIdOrSlug(productId)
                        }
                        val directProduct by directProductFlow.collectAsStateWithLifecycle(initialValue = null)
                        val product = directProduct ?: products.firstOrNull {
                            it.id == productId || it.slug.equals(productId, ignoreCase = true)
                        }
                        val resolvedProductId = product?.id ?: productId

                        LaunchedEffect(product?.id) {
                            product?.let { AnalyticsTracker.trackViewItem(it, currency) }
                        }

                        val directInventoryFlow = remember(resolvedProductId) {
                            storefrontVm.observeInventoryForProduct(resolvedProductId)
                        }
                        val directInventory by directInventoryFlow.collectAsStateWithLifecycle(initialValue = null)
                        val stock = directInventory?.stockQuantity
                            ?: inventoryMap[resolvedProductId]?.stockQuantity
                            ?: 10L
                        val resolvedSku = directInventory?.sku
                            ?: inventoryMap[resolvedProductId]?.sku
                            ?: product?.sku.orEmpty()

                        val reviewsFlow = remember(resolvedProductId) {
                            storefrontVm.observeProductReviews(resolvedProductId)
                        }
                        val reviews by reviewsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                        ProductDetailScreen(
                            product = product,
                            allProducts = products,
                            categories = categoriesList,
                            inventoryMap = inventoryMap,
                            wishlistIds = wishlistIds,
                            ratingsByProduct = ratingsByProduct,
                            currency = currency,
                            wishlisted = resolvedProductId in wishlistIds,
                            stockQuantity = stock,
                            resolvedSku = resolvedSku,
                            deliveryZones = deliveryZones,
                            reviews = reviews,
                            isSubmittingReview = isSubmitting,
                            sizeGuides = sizeGuidesList,
                            whatsappNumber = storeSettings.whatsappNumber,
                            publicSiteBaseUrl = publicSiteBaseUrl,
                            onShareTracked = { platform ->
                                AnalyticsTracker.trackShare("product", resolvedProductId, platform)
                            },
                            onBack = { navController.popBackStack() },
                            onNavigateHome = { navigateTopLevel(StoreRoutes.HOME) },
                            onOpenCategory = { catId ->
                                storefrontVm.setSelectedCategory(catId)
                                navigateTopLevel(StoreRoutes.CATALOG)
                            },
                            onProductClick = { targetId ->
                                navController.navigate(StoreRoutes.productDetail(targetId))
                            },
                            onQuickView = { quickViewProduct = it },
                            onToggleWishlist = { storefrontVm.toggleWishlist(it) },
                            onAddToBag = { prod, variant, qty ->
                                storefrontVm.addToCart(prod, variant, qty)
                            },
                            onBuyNow = { prod, variant, qty ->
                                storefrontVm.addToCart(prod, variant, qty) {
                                    if (isSignedIn) {
                                        navController.navigate(StoreRoutes.CHECKOUT)
                                    } else {
                                        openAuthFlow(
                                            mode = AuthMode.SIGN_IN,
                                            hint = "Sign in or create a free customer account to complete your order — your bag stays ready.",
                                            postAuthTarget = StoreRoutes.CHECKOUT,
                                        )
                                    }
                                }
                            },
                            onSubmitReview = { rating, comment, onSuccess ->
                                storefrontVm.submitProductReview(
                                    productId = resolvedProductId,
                                    rating = rating,
                                    comment = comment,
                                    onSubmitted = onSuccess,
                                )
                            },
                        )
                    }

                    composable(StoreRoutes.BAG) {
                        BagAndWishlistScreen(
                            isSignedIn = isSignedIn,
                            cartState = cartState,
                            wishlistState = wishlistState,
                            products = products,
                            initialTab = bagInitialTab,
                            tabNavigationNonce = bagTabNonce,
                            onSignInToSync = {
                                openAuthFlow(
                                    mode = AuthMode.SIGN_IN,
                                    hint = "Sign in or register to sync your shopping bag and wishlist across devices.",
                                    postAuthTarget = StoreRoutes.BAG,
                                )
                            },
                            onUpdateCartQuantity = storefrontVm::updateCartQuantity,
                            onRemoveCartItem = storefrontVm::removeCartItem,
                            onOpenProduct = { productId ->
                                navController.navigate(StoreRoutes.productDetail(productId))
                            },
                            onRemoveWishlist = storefrontVm::toggleWishlist,
                            onBrowseStore = { navigateTopLevel(StoreRoutes.CATALOG) },
                            onProceedToCheckout = {
                                if (isSignedIn) {
                                    navController.navigate(StoreRoutes.CHECKOUT)
                                } else {
                                    openAuthFlow(
                                        mode = AuthMode.SIGN_IN,
                                        hint = "Sign in or create an account to complete checkout — your guest bag items will merge automatically.",
                                        postAuthTarget = StoreRoutes.CHECKOUT,
                                    )
                                }
                            },
                        )
                    }

                    composable(StoreRoutes.CHECKOUT) {
                        LaunchedEffect(Unit) {
                            if (cartItems.isNotEmpty()) {
                                AnalyticsTracker.trackBeginCheckout(
                                    itemCount = cartItems.sumOf { it.quantity.toInt() },
                                    totalNgn = cartItems.sumOf { it.unitPriceNgn * it.quantity },
                                    couponApplied = false,
                                )
                            }
                        }
                        CheckoutScreen(
                            cartItems = cartItems,
                            deliveryZones = deliveryZones,
                            savedAddresses = savedAddresses,
                            privateInfo = (privateInfoState as? UiState.Success)?.data,
                            defaultRecipientName = userProfile?.displayName.orEmpty().ifBlank { displayName },
                            storeSettings = storeSettings,
                            currency = currency,
                            isCartLoading = (cartState is UiState.Loading) || isMergingGuestData,
                            isSubmitting = isSubmitting,
                            onBack = { navController.popBackStack() },
                            onSaveNewAddress = { draft, onSaved ->
                                storefrontVm.saveDeliveryAddress(null, draft, onSaved)
                            },
                            onPlaceOrder = { zone, city, state, method, note, selectedAddress ->
                                storefrontVm.placeOrder(
                                    cartItems = cartItems,
                                    zone = zone,
                                    shippingCity = city,
                                    shippingState = state,
                                    currency = currency,
                                    paymentMethod = method,
                                    transferNote = note,
                                    selectedAddress = selectedAddress,
                                    onOrderPlaced = {
                                        navController.popBackStack(StoreRoutes.HOME, inclusive = false)
                                        navigateTopLevel(StoreRoutes.ACCOUNT)
                                    },
                                )
                            },
                        )
                    }

                    composable(StoreRoutes.AUTH) {
                        AuthScreen(
                            initialMode = authInitialMode,
                            canBrowseAsGuest = true,
                            guestHintBanner = authGuestHint,
                            onContinueAsGuest = {
                                onClearPendingPostAuthRoute()
                                navController.popBackStack()
                            },
                            onAuthSuccess = { bootstrap ->
                                onAuthCompleted(bootstrap)
                            },
                        )
                    }

                    composable(StoreRoutes.ACCOUNT) {
                        AccountAndOrdersScreen(
                            isSignedIn = isSignedIn,
                            userProfileState = userProfileState,
                            privateInfoState = privateInfoState,
                            deliveryAddressesState = deliveryAddressesState,
                            ordersState = ordersState,
                            wishlistState = wishlistState,
                            notificationsState = notificationsState,
                            products = products,
                            userEmail = email,
                            isEmailVerified = verifiedEmailStatus,
                            hasPasswordProvider = hasPasswordProvider,
                            resendCooldownSeconds = verificationCooldown,
                            isAdmin = isAdmin,
                            isSubmitting = isSubmitting,
                            unreadNotificationCount = unreadNotificationCount,
                            onOpenAuth = { mode ->
                                openAuthFlow(
                                    mode = mode,
                                    hint = null,
                                    postAuthTarget = StoreRoutes.ACCOUNT,
                                )
                            },
                            onResendVerification = storefrontVm::resendVerificationEmail,
                            onRefreshVerification = storefrontVm::refreshEmailVerificationStatus,
                            onUploadPhoto = { uri -> storefrontVm.uploadProfilePhoto(context, uri) },
                            onRemovePhoto = storefrontVm::removeProfilePhoto,
                            onSavePersonalProfile = storefrontVm::updatePersonalProfile,
                            onToggleCurrency = { storefrontVm.toggleCurrency(currency) },
                            onSaveAddress = { addressId, draft, onDone ->
                                storefrontVm.saveDeliveryAddress(addressId, draft) { onDone() }
                            },
                            onSetDefaultAddress = storefrontVm::setDefaultAddress,
                            onDeleteAddress = storefrontVm::deleteDeliveryAddress,
                            onSubmitPaymentNotice = storefrontVm::submitPaymentNotice,
                            onMarkNotificationRead = storefrontVm::markNotificationRead,
                            onTrackOrder = { orderId ->
                                navController.navigate(StoreRoutes.trackOrder(orderId = orderId))
                            },
                            onOpenNotificationsCenter = { navController.navigate(StoreRoutes.NOTIFICATIONS) },
                            onOpenNotificationPrefs = { navController.navigate(StoreRoutes.NOTIFICATION_PREFS) },
                            onOpenSupport = { navController.navigate(StoreRoutes.SUPPORT) },
                            onOpenProduct = { productId ->
                                navController.navigate(StoreRoutes.productDetail(productId))
                            },
                            onRemoveWishlist = storefrontVm::toggleWishlist,
                            onBrowseStore = { navigateTopLevel(StoreRoutes.CATALOG) },
                            onChangePassword = storefrontVm::changePassword,
                            onRequestEmailChange = storefrontVm::requestEmailChange,
                            onOpenAdminDashboard = {
                                if (isAdmin) navigateTopLevel(StoreRoutes.ADMIN)
                            },
                            onSignOut = {
                                onClearPendingPostAuthRoute()
                                AuthManager.signOut(credentialManager, scope)
                            },
                        )
                    }

                    composable(
                        route = StoreRoutes.TRACK_ORDER,
                        arguments = listOf(
                            navArgument("orderId") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("token") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { entry ->
                        val initialOrderId = entry.arguments?.getString("orderId").orEmpty()
                        val initialToken = entry.arguments?.getString("token").orEmpty()
                        OrderTrackingScreen(
                            isSignedIn = isSignedIn,
                            ordersState = ordersState,
                            deliveryFeedbackList = (deliveryFeedbackState as? UiState.Success)?.data.orEmpty(),
                            guestTrackingResultState = guestTrackingResultState,
                            storeSettings = storeSettings,
                            initialOrderId = initialOrderId,
                            initialTrackingToken = initialToken,
                            isSubmitting = isSubmitting,
                            onBack = { navController.popBackStack() },
                            onLookupGuestOrder = storefrontVm::lookupGuestOrder,
                            onClearGuestTrackingResult = storefrontVm::clearGuestTrackingResult,
                            onRetryFailedPayment = storefrontVm::retryFailedPayment,
                            onSubmitDeliveryFeedback = storefrontVm::submitDeliveryFeedback,
                            onOpenSupport = { navController.navigate(StoreRoutes.SUPPORT) },
                            onOpenCatalog = { navigateTopLevel(StoreRoutes.CATALOG) },
                        )
                    }

                    composable(StoreRoutes.NOTIFICATIONS) {
                        NotificationsCenterScreen(
                            isSignedIn = isSignedIn,
                            notificationsState = notificationsState,
                            onBack = { navController.popBackStack() },
                            onOpenPreferences = { navController.navigate(StoreRoutes.NOTIFICATION_PREFS) },
                            onMarkRead = storefrontVm::markNotificationRead,
                            onMarkAllRead = storefrontVm::markAllNotificationsRead,
                            onNavigateRoute = { target -> navigateNotificationTargetRoute(target) },
                            onOpenSignIn = {
                                openAuthFlow(
                                    mode = AuthMode.SIGN_IN,
                                    hint = "Sign in to view your ZEECROWN notifications and alerts.",
                                    postAuthTarget = StoreRoutes.NOTIFICATIONS,
                                )
                            },
                        )
                    }

                    composable(StoreRoutes.NOTIFICATION_PREFS) {
                        NotificationPreferencesScreen(
                            preferencesState = notificationPreferencesState,
                            isSubmitting = isSubmitting,
                            onBack = { navController.popBackStack() },
                            onSavePreferences = storefrontVm::saveNotificationPreferences,
                            onRequestPushPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                                        storefrontVm.registerFcmToken(token)
                                    }
                                }
                            },
                        )
                    }

                    composable(StoreRoutes.SUPPORT) {
                        val ordersList = (ordersState as? UiState.Success)?.data.orEmpty()
                        SupportCenterScreen(
                            isSignedIn = isSignedIn,
                            storeSettings = storeSettings,
                            orders = ordersList,
                            enquiriesState = customerEnquiriesState,
                            observeReplies = storefrontVm::observeEnquiryReplies,
                            isSubmitting = isSubmitting,
                            onBack = { navController.popBackStack() },
                            onOpenOrderTracking = { navController.navigate(StoreRoutes.trackOrder()) },
                            onOpenSignIn = {
                                openAuthFlow(
                                    mode = AuthMode.SIGN_IN,
                                    hint = "Sign in to send a concierge request and view live support replies.",
                                    postAuthTarget = StoreRoutes.SUPPORT,
                                )
                            },
                            onSubmitEnquiry = { cat, subj, msg, orderId, orderNum, onDone ->
                                storefrontVm.submitSupportEnquiry(cat, subj, msg, orderId, orderNum) { onDone() }
                            },
                            onPostReply = storefrontVm::postCustomerSupportReply,
                        )
                    }

                    composable(StoreRoutes.ADMIN) {
                        if (adminVm != null && adminRole.isAdmin) {
                            val role by adminVm.adminRole.collectAsStateWithLifecycle()
                            val report by adminVm.analyticsReport.collectAsStateWithLifecycle()
                            val selectedDateRange by adminVm.selectedDateRange.collectAsStateWithLifecycle()
                            val allProductsState by adminVm.allProductsState.collectAsStateWithLifecycle()
                            val allOrdersState by adminVm.allOrdersState.collectAsStateWithLifecycle()
                            val allPaymentsState by adminVm.allPaymentsState.collectAsStateWithLifecycle()
                            val allCategoriesAdminState by adminVm.allCategoriesAdminState.collectAsStateWithLifecycle()
                            val adminCollectionsState by adminVm.collectionsState.collectAsStateWithLifecycle()
                            val adminSizeGuidesState by adminVm.sizeGuidesState.collectAsStateWithLifecycle()
                            val adminInventoryState by adminVm.inventoryState.collectAsStateWithLifecycle()
                            val invTransactionsState by adminVm.inventoryTransactionsState.collectAsStateWithLifecycle()
                            val couponsState by adminVm.couponsState.collectAsStateWithLifecycle()
                            val deliveryZonesState by adminVm.deliveryZonesState.collectAsStateWithLifecycle()
                            val deliverySettingsState by adminVm.deliverySettingsState.collectAsStateWithLifecycle()
                            val adminSettingsState by adminVm.storeSettingsState.collectAsStateWithLifecycle()
                            val adminHomepageState by adminVm.homepageContentState.collectAsStateWithLifecycle()
                            val testimonialsState by adminVm.testimonialsState.collectAsStateWithLifecycle()
                            val allReviewsState by adminVm.allReviewsState.collectAsStateWithLifecycle()
                            val customersState by adminVm.customersState.collectAsStateWithLifecycle()
                            val adminTeamState by adminVm.adminTeamState.collectAsStateWithLifecycle()
                            val activityLogsState by adminVm.activityLogsState.collectAsStateWithLifecycle()
                            val communicationLogsState by adminVm.communicationLogsState.collectAsStateWithLifecycle()
                            val supportEnquiriesState by adminVm.supportEnquiriesState.collectAsStateWithLifecycle()
                            val deliveryFeedbackState by adminVm.deliveryFeedbackState.collectAsStateWithLifecycle()
                            val selectedEnquiryReplies by adminVm.selectedEnquiryReplies.collectAsStateWithLifecycle()
                            val selectedEnquiryInternalNotes by adminVm.selectedEnquiryInternalNotes.collectAsStateWithLifecycle()
                            val selectedCustDetails by adminVm.selectedCustomerDetails.collectAsStateWithLifecycle()
                            val isLoadingCustDetails by adminVm.isLoadingCustomerDetails.collectAsStateWithLifecycle()
                            val selectedPhotoEntries by adminVm.selectedPhotoEntries.collectAsStateWithLifecycle()
                            val aiProductAnalysisState by adminVm.aiProductAnalysisState.collectAsStateWithLifecycle()
                            val adminBusy by adminVm.isBusy.collectAsStateWithLifecycle()
                            val adminUploadProgress by adminVm.uploadProgress.collectAsStateWithLifecycle()
                            val liveAlerts by adminVm.liveAlerts.collectAsStateWithLifecycle()
                            val readAlertIds by adminVm.readAlertIds.collectAsStateWithLifecycle()
                            val adminAiConfigState by adminVm.aiConfigState.collectAsStateWithLifecycle()
                            val adminAiFaqsState by adminVm.aiFaqsState.collectAsStateWithLifecycle()
                            val adminAiUnansweredState by adminVm.aiUnansweredQuestionsState.collectAsStateWithLifecycle()
                            val adminAiAnalyticsSummary by adminVm.aiAnalyticsSummary.collectAsStateWithLifecycle()
                            val adminAiTestReply by adminVm.aiTestReply.collectAsStateWithLifecycle()
                            val adminAiTestRunning by adminVm.aiTestRunning.collectAsStateWithLifecycle()
                            val adminMsg by adminVm.adminMessage.collectAsStateWithLifecycle()

                            LaunchedEffect(adminMsg) {
                                val msg = adminMsg ?: return@LaunchedEffect
                                snackbarHostState.showSnackbar(msg)
                                adminVm.clearMessage()
                            }

                            AdminDashboardScreen(
                                role = role,
                                adminEmail = email,
                                analyticsReport = report,
                                selectedDateRange = selectedDateRange,
                                allProductsState = allProductsState,
                                allOrdersState = allOrdersState,
                                allPaymentsState = allPaymentsState,
                                allCategoriesAdminState = allCategoriesAdminState,
                                collectionsState = adminCollectionsState,
                                allSizeGuidesState = adminSizeGuidesState,
                                inventoryState = adminInventoryState,
                                inventoryTransactionsState = invTransactionsState,
                                couponsState = couponsState,
                                deliveryZonesState = deliveryZonesState,
                                deliverySettingsState = deliverySettingsState,
                                storeSettingsState = adminSettingsState,
                                homepageContentState = adminHomepageState,
                                testimonialsState = testimonialsState,
                                allReviewsState = allReviewsState,
                                customersState = customersState,
                                adminTeamState = adminTeamState,
                                activityLogsState = activityLogsState,
                                communicationLogsState = communicationLogsState,
                                supportEnquiriesState = supportEnquiriesState,
                                deliveryFeedbackState = deliveryFeedbackState,
                                selectedEnquiryReplies = selectedEnquiryReplies,
                                selectedEnquiryInternalNotes = selectedEnquiryInternalNotes,
                                aiConfigState = adminAiConfigState,
                                aiFaqsState = adminAiFaqsState,
                                aiUnansweredState = adminAiUnansweredState,
                                aiAnalyticsSummary = adminAiAnalyticsSummary,
                                aiSandboxTestReply = adminAiTestReply,
                                aiSandboxTestRunning = adminAiTestRunning,
                                liveAlerts = liveAlerts,
                                readAlertIds = readAlertIds,
                                selectedCustomerDetails = selectedCustDetails,
                                isLoadingCustomerDetails = isLoadingCustDetails,
                                selectedPhotoEntries = selectedPhotoEntries,
                                aiProductAnalysisState = aiProductAnalysisState,
                                isBusy = adminBusy,
                                uploadProgress = adminUploadProgress,
                                onMarkAlertRead = adminVm::markAlertRead,
                                onMarkAllAlertsRead = adminVm::markAllAlertsRead,
                                onSelectDateRange = adminVm::setDateRange,
                                onStartNewProductDraft = adminVm::startNewProductDraftSession,
                                onPrepareEditProduct = adminVm::prepareEditProductSession,
                                onLoadPhotoEntries = adminVm::loadPhotoEntries,
                                onAddPhotos = { uris, titleHint ->
                                    adminVm.addSelectedPhotos(uris, autoAnalyzeContext = context, titleHint = titleHint)
                                },
                                onAnalyzeProductPhotos = { titleHint ->
                                    adminVm.analyzeSelectedProductPhotos(context = context, titleHint = titleHint)
                                },
                                onClearProductAiDraft = adminVm::clearProductAiDraft,
                                onMarkAiDraftApplied = adminVm::markAiDraftApplied,
                                onSetMainPhotoAt = adminVm::setMainPhotoAt,
                                onMovePhoto = adminVm::movePhoto,
                                onRemovePhoto = adminVm::removePhotoAt,
                                onClearPhotos = adminVm::clearSelectedPhotos,
                                onSeedFoundationStore = adminVm::seedStarterStore,
                                onSaveProduct = { prodId, title, desc, catId, colId, brand, sku, ngn, compNgn, usd, sizes, colors, stock, thresh, featured, newArr, active, attrs, blocked, seoT, seoD, fallback, prodSlug, prodSocialImg, prodSizeGuideId, prodSummary, prodFeaturesText, prodFashionStyle, prodOccasionsText, prodTagsText, aiDraftFlag, onDone ->
                                    adminVm.saveProduct(
                                        context = context,
                                        existingProductId = prodId,
                                        title = title,
                                        description = desc,
                                        categoryId = catId,
                                        collectionId = colId,
                                        brand = brand,
                                        sku = sku,
                                        priceNgn = ngn,
                                        compareAtPriceNgn = compNgn,
                                        priceUsd = usd,
                                        sizesText = sizes,
                                        colorsText = colors,
                                        stockQuantity = stock,
                                        lowStockThreshold = thresh,
                                        featured = featured,
                                        newArrival = newArr,
                                        active = active,
                                        attributesText = attrs,
                                        unavailableVariantsText = blocked,
                                        seoTitle = seoT,
                                        seoDescription = seoD,
                                        fallbackDrawableKey = fallback,
                                        slug = prodSlug,
                                        socialImageUrl = prodSocialImg,
                                        sizeGuideId = prodSizeGuideId,
                                        summary = prodSummary,
                                        featuresText = prodFeaturesText,
                                        fashionStyle = prodFashionStyle,
                                        suitableOccasionsText = prodOccasionsText,
                                        tagsText = prodTagsText,
                                        aiAssistedDraft = aiDraftFlag,
                                        onDone = onDone,
                                    )
                                },
                                onToggleProductActive = adminVm::setProductActive,
                                onDuplicateProduct = adminVm::duplicateProduct,
                                onDeleteProduct = adminVm::deleteProduct,
                                onSaveCategory = { catId, name, desc, parentId, img, seoT, seoD, order, active, catSlug, catSocialImg, catSizeGuideId ->
                                    adminVm.saveCategory(
                                        context = context,
                                        categoryId = catId,
                                        name = name,
                                        description = desc,
                                        parentCategoryId = parentId,
                                        imageKeyOrUri = img,
                                        seoTitle = seoT,
                                        seoDescription = seoD,
                                        sortOrder = order,
                                        active = active,
                                        slug = catSlug,
                                        socialImageUrl = catSocialImg,
                                        sizeGuideId = catSizeGuideId,
                                    )
                                },
                                onMoveCategory = adminVm::moveCategory,
                                onToggleCategoryActive = adminVm::toggleCategoryActive,
                                onDeleteCategory = adminVm::deleteCategorySafely,
                                onSaveSizeGuide = adminVm::saveSizeGuide,
                                onDeleteSizeGuide = adminVm::deleteSizeGuide,
                                onAdjustInventory = { p, r, q, t, vk, vq, reason ->
                                    adminVm.adjustInventory(p, r, q, t, vk, vq, reason)
                                },
                                onUpdateOrderFulfillment = { o, st, notes, cancelDec, courier, trackRef, trackUrl, etaWindow, delivStatus, delivNotes ->
                                    adminVm.updateOrderFulfillment(
                                        order = o,
                                        newStatus = st,
                                        internalNotes = notes,
                                        cancellationDecision = cancelDec,
                                        courierName = courier,
                                        trackingReference = trackRef,
                                        trackingUrl = trackUrl,
                                        estimatedDeliveryWindow = etaWindow,
                                        deliveryStatus = delivStatus,
                                        deliveryNotes = delivNotes,
                                    )
                                },
                                onReviewPayment = { pay, ord, approve, note ->
                                    adminVm.reviewPayment(pay, ord, approve, note)
                                },
                                onRecordRefund = { ord, amt, reason ->
                                    adminVm.recordRefund(ord, amt, reason)
                                },
                                onLoadCustomerDetails = adminVm::loadCustomerDetails,
                                onUpdateCustomerNotes = adminVm::updateCustomerNotes,
                                onSelectSupportEnquiry = adminVm::selectSupportEnquiry,
                                onUpdateSupportMeta = { enq, st, assignMe, assignedAdm, clearAssign, prio ->
                                    adminVm.updateSupportEnquiryMeta(
                                        enquiry = enq,
                                        newStatus = st,
                                        assignToMe = assignMe,
                                        assignedAdmin = assignedAdm,
                                        clearAssignment = clearAssign,
                                        priority = prio,
                                    )
                                },
                                onPostAdminSupportReply = adminVm::postAdminSupportReply,
                                onAddSupportInternalNote = adminVm::addAdminSupportInternalNote,
                                onSendManualOrderUpdate = adminVm::sendManualOrderUpdateNotification,
                                onRetryCommunicationLog = adminVm::retryCommunicationLog,
                                onSaveTeamMember = { targetUid, targetEmail, targetName, targetRole, active ->
                                    adminVm.saveAdminTeamMember(targetUid, targetEmail, targetName, targetRole, active)
                                },
                                onSaveCoupon = { cid, coupon -> adminVm.saveCoupon(cid, coupon) },
                                onDeleteCoupon = adminVm::deleteCoupon,
                                onSaveZone = { zid, zone -> adminVm.saveDeliveryZone(zid, zone) },
                                onDeleteZone = adminVm::deleteDeliveryZone,
                                onSaveDeliverySettings = adminVm::saveDeliverySettings,
                                onSaveHomepageContent = { secId, content, uri ->
                                    adminVm.saveHomepageSection(context, secId, content, uri)
                                },
                                onSaveCollection = { colId, col -> adminVm.saveCollection(colId, col) },
                                onSaveTestimonial = { tid, item -> adminVm.saveTestimonial(tid, item) },
                                onDeleteTestimonial = adminVm::deleteTestimonial,
                                onModerateReview = adminVm::moderateReview,
                                onDeleteReview = adminVm::deleteReview,
                                onSaveStoreSettings = { settings, logoUri ->
                                    adminVm.saveStoreSettings(context, settings, logoUri)
                                },
                                onSaveAiConfig = adminVm::saveAiConfig,
                                onSaveAiFaq = adminVm::saveAiFaq,
                                onDeleteAiFaq = adminVm::deleteAiFaq,
                                onPromoteAiUnanswered = adminVm::promoteUnansweredToFaq,
                                onDismissAiUnanswered = adminVm::dismissUnansweredQuestion,
                                onRunAiSandboxTest = adminVm::runAiSandboxTest,
                                onPostMessage = adminVm::postMessage,
                                onBack = { navigateTopLevel(StoreRoutes.HOME) },
                            )
                        } else {
                            BoutiqueEmptyView(
                                title = "Administrator Access Required",
                                subtitle = "This area is restricted to authorized ZEECROWN STORE staff accounts.",
                                actionLabel = "Back to Boutique",
                                onAction = { navigateTopLevel(StoreRoutes.HOME) },
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }

                    composable(StoreRoutes.AI_ASSISTANT) {
                        AiAssistantScreen(
                            config = aiConfig,
                            storeSettings = storeSettings,
                            currency = currency,
                            isSignedIn = isSignedIn,
                            messages = aiMessages,
                            recentConversations = aiRecentConversations,
                            isSending = aiIsSending,
                            isSubmittingHandover = isSubmitting,
                            escalatedTicketId = aiEscalatedTicketId,
                            handoverError = aiHandoverError,
                            markedHelpful = aiMarkedHelpful,
                            onBack = { navController.popBackStack() },
                            onSendMessage = { q -> storefrontVm.sendAiMessage(q, currency) },
                            onNewConversation = storefrontVm::startNewAiConversation,
                            onOpenConversationHistory = storefrontVm::openSavedAiConversation,
                            onMarkHelpful = storefrontVm::markAiConversationHelpful,
                            onEscalateToHuman = storefrontVm::escalateAiToHuman,
                            onClearHandoverError = storefrontVm::clearAiHandoverError,
                            onLookupGuestOrder = storefrontVm::lookupGuestOrderFromAiChat,
                            onProductClick = { productId ->
                                storefrontVm.trackAiRecommendationClick(productId)
                                navController.navigate(StoreRoutes.productDetail(productId))
                            },
                            onNavigateAction = { link ->
                                when (link.destination) {
                                    AiActionDestination.PRODUCT -> if (link.targetId.isNotBlank()) {
                                        navController.navigate(StoreRoutes.productDetail(link.targetId))
                                    } else {
                                        navigateTopLevel(StoreRoutes.CATALOG)
                                    }
                                    AiActionDestination.CATEGORY -> {
                                        if (link.targetId.isNotBlank()) {
                                            storefrontVm.setSelectedCategory(link.targetId)
                                        }
                                        navigateTopLevel(StoreRoutes.CATALOG)
                                    }
                                    AiActionDestination.CATALOG_SEARCH -> {
                                        if (link.targetId.isNotBlank()) {
                                            storefrontVm.updateFilterState { it.copy(searchQuery = link.targetId) }
                                        }
                                        navigateTopLevel(StoreRoutes.CATALOG)
                                    }
                                    AiActionDestination.CART -> {
                                        bagInitialTab = 0
                                        bagTabNonce++
                                        navigateTopLevel(StoreRoutes.BAG)
                                    }
                                    AiActionDestination.CHECKOUT -> {
                                        if (isSignedIn) {
                                            navController.navigate(StoreRoutes.CHECKOUT)
                                        } else {
                                            openAuthFlow(
                                                mode = AuthMode.SIGN_IN,
                                                hint = "Sign in or create an account to complete checkout — your guest bag items will merge automatically.",
                                                postAuthTarget = StoreRoutes.CHECKOUT,
                                            )
                                        }
                                    }
                                    AiActionDestination.TRACK_ORDER -> navController.navigate(StoreRoutes.trackOrder(orderId = link.targetId))
                                    AiActionDestination.ACCOUNT -> navigateTopLevel(StoreRoutes.ACCOUNT)
                                    AiActionDestination.SUPPORT -> navController.navigate(StoreRoutes.SUPPORT)
                                }
                            },
                            onOpenSignIn = {
                                openAuthFlow(
                                    mode = AuthMode.SIGN_IN,
                                    hint = "Sign in to access your order status and save AI concierge chats.",
                                    postAuthTarget = StoreRoutes.AI_ASSISTANT,
                                )
                            },
                        )
                    }
                }
                        val showCustomerCareFab = currentRoute !in setOf(
                            StoreRoutes.ADMIN,
                            StoreRoutes.CHECKOUT,
                            StoreRoutes.AUTH,
                            StoreRoutes.AI_ASSISTANT,
                        )
                        if (showCustomerCareFab) {
                            val fabBottomPadding = if (
                                currentRoute == StoreRoutes.PRODUCT_DETAIL || currentRoute == StoreRoutes.BAG
                            ) {
                                84.dp
                            } else {
                                16.dp
                            }
                            ExtendedFloatingActionButton(
                                onClick = { showCustomerCareHub = true },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.SupportAgent,
                                        contentDescription = "Open ZEECROWN Customer Care",
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Need Help?",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                containerColor = ZeecrownRoyalPurple,
                                contentColor = ZeecrownPureWhite,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = fabBottomPadding)
                                    .minimumInteractiveComponentSize()
                                    .testTag("customer_care_fab"),
                            )
                        }
                    }
                }

                if (SeoUtils.isValidGa4MeasurementId(storeSettings.ga4MeasurementId) &&
                    storeSettings.analyticsConsentRequired &&
                    !analyticsConsentAnswered &&
                    currentRoute != StoreRoutes.ADMIN
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analytics_consent_banner"),
                        color = ZeecrownLightLilac,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Allow anonymous shopping analytics to help personalize ZEECROWN STORE?",
                                style = MaterialTheme.typography.bodySmall,
                                color = ZeecrownObsidianInk,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = {
                                    analyticsConsentGranted = false
                                    analyticsConsentAnswered = true
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("analytics_consent_decline"),
                            ) {
                                Text("Decline")
                            }
                            Button(
                                onClick = {
                                    analyticsConsentGranted = true
                                    analyticsConsentAnswered = true
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("analytics_consent_allow"),
                                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                            ) {
                                Text("Allow")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomerCareHub) {
        ZeecrownCustomerCareSheet(
            aiEnabled = aiConfig.enabled,
            whatsappNumber = storeSettings.whatsappNumber,
            returnPolicy = storeSettings.returnPolicy,
            onDismiss = { showCustomerCareHub = false },
            onOpenShoppingAssistance = {
                showCustomerCareHub = false
                navController.navigate(StoreRoutes.AI_ASSISTANT) { launchSingleTop = true }
            },
            onOpenSupportCenter = {
                showCustomerCareHub = false
                navController.navigate(StoreRoutes.SUPPORT) { launchSingleTop = true }
            },
            onOpenOrderTracking = {
                showCustomerCareHub = false
                navController.navigate(StoreRoutes.trackOrder()) { launchSingleTop = true }
            },
            onOpenWhatsApp = {
                showCustomerCareHub = false
                launchWhatsAppChat(context, storeSettings.whatsappNumber)
            },
        )
    }

    quickViewProduct?.let { qvProduct ->
        val qvStock = inventoryMap[qvProduct.id]?.stockQuantity ?: 10L
        ProductQuickViewDialog(
            product = qvProduct,
            currency = currency,
            wishlisted = qvProduct.id in wishlistIds,
            stockQuantity = qvStock,
            onDismiss = { quickViewProduct = null },
            onToggleWishlist = { storefrontVm.toggleWishlist(qvProduct) },
            onAddToBag = { variant, qty ->
                storefrontVm.addToCart(qvProduct, variant, qty)
            },
            onViewFullDetails = {
                navController.navigate(StoreRoutes.productDetail(qvProduct.id))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZeecrownCustomerCareSheet(
    aiEnabled: Boolean,
    whatsappNumber: String,
    returnPolicy: String,
    onDismiss: () -> Unit,
    onOpenShoppingAssistance: () -> Unit,
    onOpenSupportCenter: () -> Unit,
    onOpenOrderTracking: () -> Unit,
    onOpenWhatsApp: () -> Unit,
) {
    var showPolicyNote by remember { mutableStateOf(false) }
    val resolvedWa = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(whatsappNumber)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ZeecrownPureWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("customer_care_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ZeecrownRoyalPurple,
                border = BorderStroke(1.dp, ZeecrownMetallicGold),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "ZEECROWN CUSTOMER CARE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZeecrownPureWhite,
                    )
                    Text(
                        text = "Your shopping and customer support assistant.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZeecrownSoftLavender,
                    )
                }
            }

            if (aiEnabled) {
                Surface(
                    onClick = onOpenShoppingAssistance,
                    shape = RoundedCornerShape(14.dp),
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownRoyalPurple.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("care_option_shopping"),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = ZeecrownRoyalPurple)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Shopping Assistance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ZeecrownDeepPurple,
                            )
                            Text(
                                "Find styles, Abayas, Adire, footwear, pricing, sizing & checkout guidance",
                                style = MaterialTheme.typography.bodySmall,
                                color = ZeecrownMutedPlum,
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = onOpenSupportCenter,
                shape = RoundedCornerShape(14.dp),
                color = ZeecrownWarmIvory,
                border = BorderStroke(1.dp, ZeecrownMetallicGold.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("care_option_support"),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = ZeecrownDeepPurple)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Customer Support",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ZeecrownDeepPurple,
                            )
                            Text(
                                "Order tracking, payment verification, delivery help & damaged/wrong-item claims",
                                style = MaterialTheme.typography.bodySmall,
                                color = ZeecrownMutedPlum,
                            )
                        }
                    }
                    HorizontalDivider(color = ZeecrownElegantSilver)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onOpenOrderTracking,
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Track Order", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(
                            onClick = { showPolicyNote = !showPolicyNote },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Outlined.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Order Policy", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (showPolicyNote) {
                        Text(
                            text = returnPolicy.ifBlank {
                                "ZEECROWN STORE currently does not accept customer returns or refunds. Please check our Size Guide or WhatsApp us before ordering. If you received a wrong or damaged item, submit a Support Ticket or message us on WhatsApp."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ZeecrownObsidianInk,
                        )
                    }
                }
            }

            Button(
                onClick = onOpenWhatsApp,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZeecrownWhatsAppGreen,
                    contentColor = ZeecrownPureWhite,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("care_option_whatsapp"),
            ) {
                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Chat with Us on WhatsApp",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        resolvedWa,
                        style = MaterialTheme.typography.labelSmall,
                        color = ZeecrownPureWhite.copy(alpha = 0.9f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}
