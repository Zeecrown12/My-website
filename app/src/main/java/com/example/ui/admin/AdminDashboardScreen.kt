package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminAlertItem
import com.example.data.model.AdminAnalyticsReport
import com.example.data.model.AdminRole
import com.example.data.model.AdminSection
import com.example.data.model.AdminUser
import com.example.data.model.AiAnalyticsCalculator
import com.example.data.model.AiAnalyticsSummary
import com.example.data.model.AiAssistantConfig
import com.example.data.model.AiChatMessage
import com.example.data.model.AiFaqEntry
import com.example.data.model.AiUnansweredQuestion
import com.example.data.model.AnalyticsDateRange
import com.example.data.model.Category
import com.example.data.model.CommunicationLog
import com.example.data.model.Coupon
import com.example.data.model.CustomerAdminSummary
import com.example.data.model.DeliveryAddress
import com.example.data.model.DeliveryFeedback
import com.example.data.model.DeliverySettings
import com.example.data.model.DeliveryZone
import com.example.data.model.HomepageContent
import com.example.data.model.InventoryRecord
import com.example.data.model.InventoryTransaction
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentRecord
import com.example.data.model.Product
import com.example.data.model.ProductAiAnalysisState
import com.example.data.model.Review
import com.example.data.model.SizeGuide
import com.example.data.model.StoreCollection
import com.example.data.model.StoreSettings
import com.example.data.model.SupportEnquiry
import com.example.data.model.SupportInternalNote
import com.example.data.model.SupportReply
import com.example.data.model.SupportStatus
import com.example.data.model.Testimonial
import com.example.data.model.UserPrivateInfo
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    role: AdminRole,
    adminEmail: String = "",
    analyticsReport: AdminAnalyticsReport,
    selectedDateRange: AnalyticsDateRange,
    allProductsState: UiState<List<Product>>,
    allOrdersState: UiState<List<Order>>,
    allPaymentsState: UiState<List<PaymentRecord>>,
    allCategoriesAdminState: UiState<List<Category>>,
    collectionsState: UiState<List<StoreCollection>>,
    allSizeGuidesState: UiState<List<SizeGuide>> = UiState.Success(emptyList()),
    inventoryState: UiState<Map<String, InventoryRecord>>,
    inventoryTransactionsState: UiState<List<InventoryTransaction>>,
    couponsState: UiState<List<Coupon>>,
    deliveryZonesState: UiState<List<DeliveryZone>>,
    deliverySettingsState: UiState<DeliverySettings>,
    storeSettingsState: UiState<StoreSettings>,
    homepageContentState: UiState<List<HomepageContent>>,
    testimonialsState: UiState<List<Testimonial>>,
    allReviewsState: UiState<List<Review>>,
    customersState: UiState<List<CustomerAdminSummary>>,
    adminTeamState: UiState<List<AdminUser>>,
    activityLogsState: UiState<List<AdminActivityLog>>,
    communicationLogsState: UiState<List<CommunicationLog>>,
    supportEnquiriesState: UiState<List<SupportEnquiry>>,
    deliveryFeedbackState: UiState<List<DeliveryFeedback>>,
    selectedEnquiryReplies: List<SupportReply>,
    selectedEnquiryInternalNotes: List<SupportInternalNote>,
    aiConfigState: UiState<AiAssistantConfig> = UiState.Loading,
    aiFaqsState: UiState<List<AiFaqEntry>> = UiState.Loading,
    aiUnansweredState: UiState<List<AiUnansweredQuestion>> = UiState.Loading,
    aiAnalyticsSummary: AiAnalyticsSummary = AiAnalyticsCalculator.summarize(emptyList()),
    aiSandboxTestReply: AiChatMessage? = null,
    aiSandboxTestRunning: Boolean = false,
    liveAlerts: List<AdminAlertItem> = emptyList(),
    readAlertIds: Set<String> = emptySet(),
    selectedCustomerDetails: Pair<UserPrivateInfo?, List<DeliveryAddress>>?,
    isLoadingCustomerDetails: Boolean,
    selectedPhotoEntries: List<String>,
    aiProductAnalysisState: ProductAiAnalysisState = ProductAiAnalysisState.Idle,
    isBusy: Boolean,
    uploadProgress: Float? = null,
    onMarkAlertRead: (String) -> Unit = {},
    onMarkAllAlertsRead: () -> Unit = {},
    onSelectDateRange: (AnalyticsDateRange, Long?, Long?) -> Unit,
    onStartNewProductDraft: () -> Unit = {},
    onPrepareEditProduct: () -> Unit = {},
    onLoadPhotoEntries: (List<String>) -> Unit,
    onAddPhotos: (List<Uri>, String) -> Unit,
    onAnalyzeProductPhotos: (String) -> Unit = {},
    onClearProductAiDraft: () -> Unit = {},
    onMarkAiDraftApplied: () -> Unit = {},
    onSetMainPhotoAt: (Int) -> Unit,
    onMovePhoto: (Int, Int) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onClearPhotos: () -> Unit,
    onSeedFoundationStore: () -> Unit,
    onSaveProduct: (
        existingProductId: String?,
        title: String,
        description: String,
        categoryId: String,
        collectionId: String,
        brand: String,
        sku: String,
        priceNgn: Double,
        compareAtPriceNgn: Double?,
        priceUsd: Double,
        sizesText: String,
        colorsText: String,
        stockQuantity: Long,
        lowStockThreshold: Long,
        featured: Boolean,
        newArrival: Boolean,
        active: Boolean,
        attributesText: String,
        unavailableVariantsText: String,
        seoTitle: String,
        seoDescription: String,
        fallbackDrawableKey: String,
        slug: String,
        socialImageUrl: String,
        sizeGuideId: String,
        summary: String,
        featuresText: String,
        fashionStyle: String,
        suitableOccasionsText: String,
        tagsText: String,
        aiAssistedDraft: Boolean,
        onDone: () -> Unit,
    ) -> Unit,
    onToggleProductActive: (Product, Boolean) -> Unit,
    onDuplicateProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onSaveCategory: (String?, String, String, String, String, String, String, Long, Boolean, String, String, String) -> Unit,
    onMoveCategory: (String, Boolean, List<Category>) -> Unit,
    onToggleCategoryActive: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onSaveSizeGuide: (String?, SizeGuide, () -> Unit) -> Unit = { _, _, _ -> },
    onDeleteSizeGuide: (SizeGuide) -> Unit = {},
    onAdjustInventory: (Product, InventoryRecord?, Long, Long, String, Long?, String) -> Unit,
    onUpdateOrderFulfillment: (Order, OrderStatus, String, String, String, String, String, String, String, String) -> Unit,
    onReviewPayment: (PaymentRecord, Order?, Boolean, String) -> Unit,
    onRecordRefund: (Order, Double, String) -> Unit,
    onLoadCustomerDetails: (String) -> Unit,
    onUpdateCustomerNotes: (String, String) -> Unit,
    onSelectSupportEnquiry: (String?) -> Unit,
    onUpdateSupportMeta: (SupportEnquiry, SupportStatus, Boolean, AdminUser?, Boolean, String?) -> Unit,
    onPostAdminSupportReply: (SupportEnquiry, String, SupportStatus, () -> Unit) -> Unit,
    onAddSupportInternalNote: (SupportEnquiry, String, () -> Unit) -> Unit,
    onSendManualOrderUpdate: (Order, String, String, () -> Unit) -> Unit,
    onRetryCommunicationLog: (CommunicationLog) -> Unit,
    onSaveTeamMember: (String, String, String, AdminRole, Boolean) -> Unit,
    onSaveCoupon: (String?, Coupon) -> Unit,
    onDeleteCoupon: (Coupon) -> Unit,
    onSaveZone: (String?, DeliveryZone) -> Unit,
    onDeleteZone: (DeliveryZone) -> Unit,
    onSaveDeliverySettings: (DeliverySettings) -> Unit,
    onSaveHomepageContent: (String, HomepageContent, Uri?) -> Unit,
    onSaveCollection: (String?, StoreCollection) -> Unit,
    onSaveTestimonial: (String?, Testimonial) -> Unit,
    onDeleteTestimonial: (String) -> Unit,
    onModerateReview: (String, Boolean, String, String) -> Unit,
    onDeleteReview: (String) -> Unit,
    onSaveStoreSettings: (StoreSettings, Uri?) -> Unit,
    onSaveAiConfig: (AiAssistantConfig) -> Unit = {},
    onSaveAiFaq: (String?, AiFaqEntry, () -> Unit) -> Unit = { _, _, _ -> },
    onDeleteAiFaq: (String) -> Unit = {},
    onPromoteAiUnanswered: (AiUnansweredQuestion, String, String, String, () -> Unit) -> Unit = { _, _, _, _, _ -> },
    onDismissAiUnanswered: (String) -> Unit = {},
    onRunAiSandboxTest: (String, AiAssistantConfig) -> Unit = { _, _ -> },
    onPostMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    if (!role.isAdmin) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            BoutiqueEmptyView(
                title = "Administrator Access Required",
                subtitle = "This protected command center is restricted to verified ZEECROWN STORE staff roles.",
                actionLabel = "Return to Boutique",
                onAction = onBack,
            )
        }
        return
    }

    val accessibleSections = remember(role) { AdminSection.entries.filter { role.canAccessSection(it) } }
    var activeSection by remember { mutableStateOf(AdminSection.OVERVIEW) }
    LaunchedEffect(role) {
        if (!role.canAccessSection(activeSection)) {
            activeSection = accessibleSections.firstOrNull() ?: AdminSection.OVERVIEW
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showGlobalSearchDialog by remember { mutableStateOf(false) }

    val productsList = (allProductsState as? UiState.Success)?.data.orEmpty()
    val ordersList = (allOrdersState as? UiState.Success)?.data.orEmpty()
    val categoriesList = (allCategoriesAdminState as? UiState.Success)?.data.orEmpty()
    val collectionsList = (collectionsState as? UiState.Success)?.data.orEmpty()
    val sizeGuidesList = (allSizeGuidesState as? UiState.Success)?.data.orEmpty()
    val inventoryMap = (inventoryState as? UiState.Success)?.data.orEmpty()
    val customersList = (customersState as? UiState.Success)?.data.orEmpty()
    val unreadAlertCount = remember(liveAlerts, readAlertIds, role) {
        liveAlerts.count { role.canAccessSection(it.targetSection) && it.id !in readAlertIds }
    }

    @Composable
    fun DrawerSidebarContent(onPick: (AdminSection) -> Unit) {
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 12.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ZEECROWN Admin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ZeecrownDeepPurple)
                if (adminEmail.isNotBlank()) {
                    Text(adminEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AdminStatusPill(label = role.label, tone = PillTone.GOLD)
            }
            HorizontalDivider(color = ZeecrownElegantSilver)
            LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(accessibleSections, key = { it.name }) { section ->
                    NavigationDrawerItem(
                        selected = activeSection == section,
                        onClick = { onPick(section) },
                        icon = { Icon(sectionIcon(section), contentDescription = section.title) },
                        label = { Text(section.title) },
                        modifier = Modifier.testTag("admin_nav_${section.name.lowercase()}"),
                    )
                }
            }
        }
    }

    @Composable
    fun MainAdminBody(showMenuButton: Boolean) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Surface(color = ZeecrownPureWhite, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (showMenuButton) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Outlined.Menu, contentDescription = "Open Admin Navigation")
                            }
                        }
                        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Store")
                        }
                        Column {
                            Text(
                                text = "ZEECROWN / ${activeSection.group} / ${activeSection.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(activeSection.title, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = { showGlobalSearchDialog = true }, modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_global_search_btn")) {
                            Icon(Icons.Outlined.Search, contentDescription = "Global Search")
                        }
                        IconButton(onClick = { showNotificationsDialog = true }, modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_notifications_btn")) {
                            if (unreadAlertCount > 0) {
                                BadgedBox(badge = { Badge { Text(unreadAlertCount.coerceAtMost(99).toString()) } }) {
                                    Icon(Icons.Outlined.Notifications, contentDescription = "Admin Alerts")
                                }
                            } else {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Admin Alerts")
                            }
                        }
                        if (role.canManageSettings) {
                            IconButton(onClick = { activeSection = AdminSection.SETTINGS }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Store Settings Shortcut")
                            }
                        }
                        if (role.canManageProducts) {
                            OutlinedButton(
                                onClick = onSeedFoundationStore,
                                enabled = !isBusy,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_seed_catalog_button"),
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("Seed")
                            }
                        }
                    }
                }
            }
            if (uploadProgress != null) {
                LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth().height(3.dp))
            } else if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp))
            }
            HorizontalDivider(color = ZeecrownElegantSilver)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeSection) {
                    AdminSection.OVERVIEW -> AdminOverviewTab(
                        role = role,
                        report = analyticsReport,
                        selectedRange = selectedDateRange,
                        allOrdersState = allOrdersState,
                        allProductsState = allProductsState,
                        inventoryState = inventoryState,
                        customersState = customersState,
                        isBusy = isBusy,
                        onSelectDateRange = onSelectDateRange,
                        onSeedFoundationStore = onSeedFoundationStore,
                        onOpenSection = { activeSection = it },
                    )
                    AdminSection.PRODUCTS -> AdminProductsTab(
                        allProductsState = allProductsState,
                        categories = categoriesList,
                        collections = collectionsList,
                        sizeGuides = sizeGuidesList,
                        inventoryMap = inventoryMap,
                        selectedPhotoEntries = selectedPhotoEntries,
                        aiAnalysisState = aiProductAnalysisState,
                        isBusy = isBusy,
                        uploadProgress = uploadProgress,
                        onStartNewProductDraft = onStartNewProductDraft,
                        onPrepareEditProduct = onPrepareEditProduct,
                        onLoadPhotoEntries = onLoadPhotoEntries,
                        onAddPhotos = onAddPhotos,
                        onAnalyzeProductPhotos = onAnalyzeProductPhotos,
                        onClearAiDraft = onClearProductAiDraft,
                        onMarkAiDraftApplied = onMarkAiDraftApplied,
                        onSetMainPhotoAt = onSetMainPhotoAt,
                        onMovePhoto = onMovePhoto,
                        onRemovePhoto = onRemovePhoto,
                        onClearPhotos = onClearPhotos,
                        onSaveProduct = onSaveProduct,
                        onToggleActive = onToggleProductActive,
                        onDuplicateProduct = onDuplicateProduct,
                        onDeleteProduct = onDeleteProduct,
                    )
                    AdminSection.CATEGORIES -> AdminCategoriesTab(
                        allCategoriesState = allCategoriesAdminState,
                        allProducts = productsList,
                        sizeGuides = sizeGuidesList,
                        isBusy = isBusy,
                        onSaveCategory = onSaveCategory,
                        onMoveCategory = onMoveCategory,
                        onToggleCategoryActive = onToggleCategoryActive,
                        onDeleteCategory = onDeleteCategory,
                    )
                    AdminSection.SIZE_GUIDES -> AdminSizeGuidesTab(
                        sizeGuidesState = allSizeGuidesState,
                        categories = categoriesList,
                        products = productsList,
                        isBusy = isBusy,
                        onSaveSizeGuide = onSaveSizeGuide,
                        onDeleteSizeGuide = onDeleteSizeGuide,
                    )
                    AdminSection.INVENTORY -> AdminInventoryTab(
                        productsState = allProductsState,
                        inventoryState = inventoryState,
                        transactionsState = inventoryTransactionsState,
                        isBusy = isBusy,
                        onAdjustInventory = onAdjustInventory,
                    )
                    AdminSection.ORDERS -> AdminOrdersTab(
                        role = role,
                        allOrdersState = allOrdersState,
                        isBusy = isBusy,
                        onUpdateFulfillment = onUpdateOrderFulfillment,
                        onRecordRefund = onRecordRefund,
                    )
                    AdminSection.PAYMENTS -> AdminPaymentsTab(
                        paymentsState = allPaymentsState,
                        orders = ordersList,
                        isBusy = isBusy,
                        onReviewPayment = onReviewPayment,
                    )
                    AdminSection.CUSTOMERS -> AdminCustomersTab(
                        role = role,
                        customersState = customersState,
                        selectedCustomerDetails = selectedCustomerDetails,
                        isLoadingDetails = isLoadingCustomerDetails,
                        isBusy = isBusy,
                        onLoadCustomerDetails = onLoadCustomerDetails,
                        onUpdateCustomerNotes = onUpdateCustomerNotes,
                    )
                    AdminSection.SUPPORT_INBOX -> AdminSupportInboxTab(
                        supportEnquiriesState = supportEnquiriesState,
                        deliveryFeedbackState = deliveryFeedbackState,
                        adminTeamState = adminTeamState,
                        selectedReplies = selectedEnquiryReplies,
                        selectedInternalNotes = selectedEnquiryInternalNotes,
                        isBusy = isBusy,
                        onSelectEnquiry = onSelectSupportEnquiry,
                        onUpdateMeta = onUpdateSupportMeta,
                        onPostReply = onPostAdminSupportReply,
                        onAddInternalNote = onAddSupportInternalNote,
                    )
                    AdminSection.COMMUNICATIONS -> AdminCommunicationCenterTab(
                        communicationLogsState = communicationLogsState,
                        orders = ordersList,
                        isBusy = isBusy,
                        onSendManualOrderUpdate = onSendManualOrderUpdate,
                        onRetryLog = onRetryCommunicationLog,
                    )
                    AdminSection.PROMOS -> AdminPromosTab(
                        couponsState = couponsState,
                        isBusy = isBusy,
                        onSaveCoupon = onSaveCoupon,
                        onDeleteCoupon = onDeleteCoupon,
                    )
                    AdminSection.DELIVERY -> AdminDeliveryTab(
                        zonesState = deliveryZonesState,
                        deliverySettingsState = deliverySettingsState,
                        isBusy = isBusy,
                        onSaveDeliverySettings = onSaveDeliverySettings,
                        onSaveZone = onSaveZone,
                        onDeleteZone = onDeleteZone,
                    )
                    AdminSection.CONTENT -> AdminContentTab(
                        homepageContentState = homepageContentState,
                        collectionsState = collectionsState,
                        testimonialsState = testimonialsState,
                        storeSettingsState = storeSettingsState,
                        isBusy = isBusy,
                        onSaveHomepageContent = onSaveHomepageContent,
                        onSaveAnnouncements = { updated -> onSaveStoreSettings(updated, null) },
                        onSaveCollection = onSaveCollection,
                        onSaveTestimonial = onSaveTestimonial,
                        onDeleteTestimonial = onDeleteTestimonial,
                    )
                    AdminSection.REVIEWS -> AdminReviewsTab(
                        reviewsState = allReviewsState,
                        products = productsList,
                        isBusy = isBusy,
                        onModerateReview = onModerateReview,
                        onDeleteReview = onDeleteReview,
                    )
                    AdminSection.REPORTS -> AdminReportsTab(
                        report = analyticsReport,
                        selectedRange = selectedDateRange,
                        orders = ordersList,
                        onSelectDateRange = onSelectDateRange,
                        onExportFeedback = onPostMessage,
                    )
                    AdminSection.SETTINGS -> AdminSettingsTab(
                        storeSettingsState = storeSettingsState,
                        isBusy = isBusy,
                        onSaveSettings = onSaveStoreSettings,
                    )
                    AdminSection.TEAM_AUDIT -> AdminTeamAndAuditTab(
                        role = role,
                        teamState = adminTeamState,
                        logsState = activityLogsState,
                        isBusy = isBusy,
                        onSaveTeamMember = onSaveTeamMember,
                    )
                    AdminSection.AI_ASSISTANT -> AdminAiAssistantTab(
                        aiConfigState = aiConfigState,
                        aiFaqsState = aiFaqsState,
                        aiUnansweredState = aiUnansweredState,
                        aiAnalyticsSummary = aiAnalyticsSummary,
                        sandboxTestReply = aiSandboxTestReply,
                        sandboxTestRunning = aiSandboxTestRunning,
                        liveProductsCount = productsList.size,
                        liveCategoriesCount = categoriesList.size,
                        liveZonesCount = (deliveryZonesState as? UiState.Success)?.data?.size ?: 0,
                        isBusy = isBusy,
                        onSaveConfig = onSaveAiConfig,
                        onSaveFaq = onSaveAiFaq,
                        onDeleteFaq = onDeleteAiFaq,
                        onPromoteUnanswered = onPromoteAiUnanswered,
                        onDismissUnanswered = onDismissAiUnanswered,
                        onRunSandboxTest = onRunAiSandboxTest,
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(256.dp), drawerContainerColor = ZeecrownLightLilac) {
                        DrawerSidebarContent { activeSection = it }
                    }
                },
            ) {
                MainAdminBody(showMenuButton = false)
            }
        } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(drawerContainerColor = ZeecrownLightLilac) {
                                DrawerSidebarContent { section ->
                                    activeSection = section
                                    scope.launch { drawerState.close() }
                                }
                            }
                        },
                    ) {
                        MainAdminBody(showMenuButton = true)
                    }
                }
            }

            if (showNotificationsDialog) {
                AdminNotificationsDialog(
                    alerts = liveAlerts,
                    readAlertIds = readAlertIds,
                    role = role,
                    onMarkRead = onMarkAlertRead,
                    onMarkAllRead = onMarkAllAlertsRead,
                    onNavigateSection = { activeSection = it },
                    onDismiss = { showNotificationsDialog = false },
                )
            }

            if (showGlobalSearchDialog) {
                AdminGlobalSearchDialog(
                    role = role,
                    products = productsList,
                    orders = ordersList,
                    customers = customersList,
                    onNavigateSection = { activeSection = it },
                    onDismiss = { showGlobalSearchDialog = false },
                )
            }
        }

private fun sectionIcon(section: AdminSection): ImageVector = when (section) {
    AdminSection.OVERVIEW -> Icons.Outlined.Dashboard
    AdminSection.ORDERS -> Icons.Outlined.ReceiptLong
    AdminSection.PAYMENTS -> Icons.Outlined.Payments
    AdminSection.PRODUCTS -> Icons.Outlined.ShoppingBag
    AdminSection.CATEGORIES -> Icons.Outlined.Category
    AdminSection.SIZE_GUIDES -> Icons.Outlined.Straighten
    AdminSection.INVENTORY -> Icons.Outlined.Inventory2
    AdminSection.CUSTOMERS -> Icons.Outlined.PeopleOutline
    AdminSection.SUPPORT_INBOX -> Icons.Outlined.SupportAgent
    AdminSection.COMMUNICATIONS -> Icons.Outlined.MarkEmailRead
    AdminSection.PROMOS -> Icons.Outlined.LocalOffer
    AdminSection.DELIVERY -> Icons.Outlined.LocalShipping
    AdminSection.CONTENT -> Icons.Outlined.Web
    AdminSection.REVIEWS -> Icons.Outlined.RateReview
    AdminSection.REPORTS -> Icons.Outlined.BarChart
    AdminSection.SETTINGS -> Icons.Outlined.Settings
    AdminSection.TEAM_AUDIT -> Icons.Outlined.Security
    AdminSection.AI_ASSISTANT -> Icons.Outlined.AutoAwesome
}
