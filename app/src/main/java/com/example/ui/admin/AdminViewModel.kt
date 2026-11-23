package com.example.ui.admin

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.UiState
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminAlertItem
import com.example.data.model.AdminAnalyticsEngine
import com.example.data.model.AdminAnalyticsReport
import com.example.data.model.AdminNotificationEngine
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AiAnalyticsCalculator
import com.example.data.model.AiAnalyticsSummary
import com.example.data.model.AiAssistantConfig
import com.example.data.model.AiChatMessage
import com.example.data.model.AiConversation
import com.example.data.model.AiFaqEntry
import com.example.data.model.AiStoreSnapshot
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
import com.example.data.model.ProductDraftAiParser
import com.example.data.model.Review
import com.example.data.model.SizeGuide
import com.example.data.model.StoreCollection
import com.example.data.model.StoreCurrency
import com.example.data.model.StoreSettings
import com.example.data.model.SupportEnquiry
import com.example.data.model.SupportInternalNote
import com.example.data.model.SupportReply
import com.example.data.model.SupportStatus
import com.example.data.model.Testimonial
import com.example.data.model.UserPrivateInfo
import com.example.data.repository.AdminOperationsRepository
import com.example.data.repository.AdminRepository
import com.example.data.repository.AdminSeedHelper
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.ProductPhotoAnalysisService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)

class AdminViewModel(
    private val db: FirebaseFirestore,
    private val adminRepository: AdminRepository,
    private val opsRepository: AdminOperationsRepository,
    catalogRepository: CatalogRepository,
    private val aiRepository: AiAssistantRepository = AiAssistantRepository(db),
    private val productPhotoAnalysisService: ProductPhotoAnalysisService = ProductPhotoAnalysisService(),
    initialRole: AdminRole,
) : ViewModel() {

    private val _adminRole = MutableStateFlow(initialRole)
    val adminRole: StateFlow<AdminRole> = _adminRole.asStateFlow()

    fun updateCurrentRole(role: AdminRole) {
        _adminRole.value = role
    }

    private val _selectedPhotoEntries = MutableStateFlow<List<String>>(emptyList())
    val selectedPhotoEntries: StateFlow<List<String>> = _selectedPhotoEntries.asStateFlow()

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _aiProductAnalysisState = MutableStateFlow<ProductAiAnalysisState>(ProductAiAnalysisState.Idle)
    val aiProductAnalysisState: StateFlow<ProductAiAnalysisState> = _aiProductAnalysisState.asStateFlow()

    private val _draftProductDocId = MutableStateFlow<String?>(null)
    private var productAiAnalysisJob: Job? = null
    private var isSavingProduct = false

    private val _readAlertIds = MutableStateFlow<Set<String>>(emptySet())
    val readAlertIds: StateFlow<Set<String>> = _readAlertIds.asStateFlow()

    private val _selectedDateRange = MutableStateFlow(AnalyticsDateRange.ALL_TIME)
    val selectedDateRange: StateFlow<AnalyticsDateRange> = _selectedDateRange.asStateFlow()

    private val _customStartMillis = MutableStateFlow<Long?>(null)
    val customStartMillis: StateFlow<Long?> = _customStartMillis.asStateFlow()

    private val _customEndMillis = MutableStateFlow<Long?>(null)
    val customEndMillis: StateFlow<Long?> = _customEndMillis.asStateFlow()

    private val _selectedCustomerDetails = MutableStateFlow<Pair<UserPrivateInfo?, List<DeliveryAddress>>?>(null)
    val selectedCustomerDetails: StateFlow<Pair<UserPrivateInfo?, List<DeliveryAddress>>?> =
        _selectedCustomerDetails.asStateFlow()

    private val _isLoadingCustomerDetails = MutableStateFlow(false)
    val isLoadingCustomerDetails: StateFlow<Boolean> = _isLoadingCustomerDetails.asStateFlow()

    private fun <T> Flow<T>.toUiState(tag: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Admin stream error for $tag", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $tag"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val allProductsState: StateFlow<UiState<List<Product>>> =
        adminRepository.observeAllProductsAdmin().toUiState("admin_products")

    val allOrdersState: StateFlow<UiState<List<Order>>> =
        adminRepository.observeAllOrdersAdmin().toUiState("admin_orders")

    val allPaymentsState: StateFlow<UiState<List<PaymentRecord>>> =
        adminRepository.observeAllPaymentsAdmin().toUiState("admin_payments")

    val allCategoriesAdminState: StateFlow<UiState<List<Category>>> =
        adminRepository.observeAllCategoriesAdmin().toUiState("admin_categories")

    val collectionsState: StateFlow<UiState<List<StoreCollection>>> =
        adminRepository.observeAllCollectionsAdmin().toUiState("admin_collections")

    val sizeGuidesState: StateFlow<UiState<List<SizeGuide>>> =
        adminRepository.observeAllSizeGuidesAdmin().toUiState("admin_size_guides")

    val inventoryState: StateFlow<UiState<Map<String, InventoryRecord>>> =
        adminRepository.observeAllInventoryAdmin().toUiState("admin_inventory")

    val inventoryTransactionsState: StateFlow<UiState<List<InventoryTransaction>>> =
        adminRepository.observeInventoryTransactionsAdmin().toUiState("admin_inv_tx")

    val couponsState: StateFlow<UiState<List<Coupon>>> =
        adminRepository.observeAllCouponsAdmin().toUiState("admin_coupons")

    val deliveryZonesState: StateFlow<UiState<List<DeliveryZone>>> =
        adminRepository.observeAllDeliveryZonesAdmin().toUiState("admin_zones")

    val deliverySettingsState: StateFlow<UiState<DeliverySettings>> =
        adminRepository.observeDeliverySettingsAdmin().toUiState("admin_delivery_settings")

    val storeSettingsState: StateFlow<UiState<StoreSettings>> =
        catalogRepository.observePublicStoreSettings().toUiState("admin_settings")

    val homepageContentState: StateFlow<UiState<List<HomepageContent>>> =
        adminRepository.observeAllHomepageContentAdmin().toUiState("admin_homepage")

    val testimonialsState: StateFlow<UiState<List<Testimonial>>> =
        adminRepository.observeAllTestimonialsAdmin().toUiState("admin_testimonials")

    val allReviewsState: StateFlow<UiState<List<Review>>> =
        adminRepository.observeAllReviewsAdmin().toUiState("admin_reviews")

    val customersState: StateFlow<UiState<List<CustomerAdminSummary>>> =
        adminRepository.observeCustomerDirectoryAdmin().toUiState("admin_customers")

    val adminTeamState: StateFlow<UiState<List<AdminUser>>> =
        adminRepository.observeAdminUsers().toUiState("admin_team")

    val activityLogsState: StateFlow<UiState<List<AdminActivityLog>>> =
        adminRepository.observeActivityLogs().toUiState("admin_audit_logs")

    val communicationLogsState: StateFlow<UiState<List<CommunicationLog>>> =
        adminRepository.observeCommunicationLogsAdmin().toUiState("admin_comms_logs")

    val supportEnquiriesState: StateFlow<UiState<List<SupportEnquiry>>> =
        adminRepository.observeSupportEnquiriesAdmin().toUiState("admin_support_inbox")

    val deliveryFeedbackState: StateFlow<UiState<List<DeliveryFeedback>>> =
        adminRepository.observeDeliveryFeedbackAdmin().toUiState("admin_delivery_feedback")

    val aiConfigState: StateFlow<UiState<AiAssistantConfig>> =
        adminRepository.observeAiConfigAdmin().toUiState("admin_ai_config")

    val aiFaqsState: StateFlow<UiState<List<AiFaqEntry>>> =
        adminRepository.observeAllAiFaqsAdmin().toUiState("admin_ai_faqs")

    val aiUnansweredQuestionsState: StateFlow<UiState<List<AiUnansweredQuestion>>> =
        adminRepository.observeAiUnansweredQuestionsAdmin().toUiState("admin_ai_unanswered")

    val aiConversationsState: StateFlow<UiState<List<AiConversation>>> =
        adminRepository.observeAiConversationsAdmin().toUiState("admin_ai_conversations")

    val aiAnalyticsSummary: StateFlow<AiAnalyticsSummary> = aiConversationsState
        .map { state ->
            AiAnalyticsCalculator.summarize((state as? UiState.Success)?.data.orEmpty())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AiAnalyticsCalculator.summarize(emptyList()),
        )

    private val _aiTestReply = MutableStateFlow<AiChatMessage?>(null)
    val aiTestReply: StateFlow<AiChatMessage?> = _aiTestReply.asStateFlow()

    private val _aiTestRunning = MutableStateFlow(false)
    val aiTestRunning: StateFlow<Boolean> = _aiTestRunning.asStateFlow()

    private val _selectedEnquiryId = MutableStateFlow<String?>(null)

    val selectedEnquiryReplies: StateFlow<List<SupportReply>> = _selectedEnquiryId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else adminRepository.observeSupportRepliesAdmin(id)
        }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    val selectedEnquiryInternalNotes: StateFlow<List<SupportInternalNote>> = _selectedEnquiryId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else adminRepository.observeSupportInternalNotesAdmin(id)
        }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    fun selectSupportEnquiry(enquiryId: String?) {
        _selectedEnquiryId.value = enquiryId
    }

    @Suppress("UNCHECKED_CAST")
    val analyticsReport: StateFlow<AdminAnalyticsReport> = combine(
        _selectedDateRange,
        _customStartMillis,
        _customEndMillis,
        allOrdersState,
        allPaymentsState,
        allProductsState,
        allCategoriesAdminState,
        inventoryState,
        customersState,
    ) { values ->
        val range = values[0] as AnalyticsDateRange
        val customStart = values[1] as Long?
        val customEnd = values[2] as Long?
        val orders = (values[3] as? UiState.Success<List<Order>>)?.data.orEmpty()
        val payments = (values[4] as? UiState.Success<List<PaymentRecord>>)?.data.orEmpty()
        val products = (values[5] as? UiState.Success<List<Product>>)?.data.orEmpty()
        val categories = (values[6] as? UiState.Success<List<Category>>)?.data.orEmpty()
        val invMap = (values[7] as? UiState.Success<Map<String, InventoryRecord>>)?.data.orEmpty()
        val customers = (values[8] as? UiState.Success<List<CustomerAdminSummary>>)?.data.orEmpty()

        AdminAnalyticsEngine.generateReport(
            range = range,
            orders = orders,
            payments = payments,
            products = products,
            categories = categories,
            inventoryMap = invMap,
            customers = customers,
            customStartMillis = customStart,
            customEndMillis = customEnd,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AdminAnalyticsEngine.generateReport(
            range = AnalyticsDateRange.ALL_TIME,
            orders = emptyList(),
            payments = emptyList(),
            products = emptyList(),
            categories = emptyList(),
            inventoryMap = emptyMap(),
            customers = emptyList(),
        ),
    )

    @Suppress("UNCHECKED_CAST")
    val liveAlerts: StateFlow<List<AdminAlertItem>> = combine(
        allOrdersState,
        allPaymentsState,
        allProductsState,
        inventoryState,
        customersState,
        allReviewsState,
        supportEnquiriesState,
    ) { values ->
        val orders = (values[0] as? UiState.Success<List<Order>>)?.data.orEmpty()
        val payments = (values[1] as? UiState.Success<List<PaymentRecord>>)?.data.orEmpty()
        val products = (values[2] as? UiState.Success<List<Product>>)?.data.orEmpty()
        val invMap = (values[3] as? UiState.Success<Map<String, InventoryRecord>>)?.data.orEmpty()
        val customers = (values[4] as? UiState.Success<List<CustomerAdminSummary>>)?.data.orEmpty()
        val reviews = (values[5] as? UiState.Success<List<Review>>)?.data.orEmpty()
        val enquiries = (values[6] as? UiState.Success<List<SupportEnquiry>>)?.data.orEmpty()
        AdminNotificationEngine.buildLiveAlerts(
            orders = orders,
            payments = payments,
            products = products,
            inventoryMap = invMap,
            customers = customers,
            reviews = reviews,
            escalatedEnquiries = enquiries,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    fun markAlertRead(alertId: String) {
        _readAlertIds.value = _readAlertIds.value + alertId
    }

    fun markAllAlertsRead() {
        _readAlertIds.value = _readAlertIds.value + liveAlerts.value.map { it.id }
    }

    fun setDateRange(range: AnalyticsDateRange, customStart: Long? = null, customEnd: Long? = null) {
        _selectedDateRange.value = range
        if (range == AnalyticsDateRange.CUSTOM) {
            _customStartMillis.value = customStart
            _customEndMillis.value = customEnd
        }
    }

    fun clearMessage() {
        _adminMessage.value = null
    }

    fun postMessage(msg: String) {
        _adminMessage.value = msg
    }

    fun startNewProductDraftSession() {
        productAiAnalysisJob?.cancel()
        _aiProductAnalysisState.value = ProductAiAnalysisState.Idle
        _selectedPhotoEntries.value = emptyList()
        _draftProductDocId.value = db.collection("products").document().id
        isSavingProduct = false
    }

    fun prepareEditProductSession() {
        productAiAnalysisJob?.cancel()
        _aiProductAnalysisState.value = ProductAiAnalysisState.Idle
        _draftProductDocId.value = null
        isSavingProduct = false
    }

    fun clearProductAiDraft() {
        productAiAnalysisJob?.cancel()
        _aiProductAnalysisState.value = ProductAiAnalysisState.Idle
    }

    fun loadPhotoEntries(entries: List<String>) {
        _selectedPhotoEntries.value = entries.distinct().take(10)
    }

    fun addSelectedPhotos(
        uris: List<Uri>,
        autoAnalyzeContext: Context? = null,
        titleHint: String = "",
    ) {
        if (uris.isEmpty()) return
        val combined = (_selectedPhotoEntries.value + uris.map { it.toString() }).distinct().take(10)
        _selectedPhotoEntries.value = combined
        if (autoAnalyzeContext != null) {
            analyzeSelectedProductPhotos(autoAnalyzeContext, titleHint)
        }
    }

    fun analyzeSelectedProductPhotos(
        context: Context,
        titleHint: String = "",
    ) {
        val entries = _selectedPhotoEntries.value
        if (entries.isEmpty()) {
            _aiProductAnalysisState.value = ProductAiAnalysisState.Error(
                message = "Upload or capture at least one product photo before generating AI details.",
                analyzedSignature = "",
            )
            return
        }
        val signature = ProductDraftAiParser.computePhotoSignature(entries)
        val categories = (allCategoriesAdminState.value as? UiState.Success)?.data.orEmpty()
        productAiAnalysisJob?.cancel()
        productAiAnalysisJob = viewModelScope.launch {
            _aiProductAnalysisState.value = ProductAiAnalysisState.Analyzing(
                photoCount = entries.size.coerceAtMost(5),
                signature = signature,
            )
            val result = productPhotoAnalysisService.analyzeProductPhotos(
                context = context,
                photoEntries = entries,
                categories = categories,
                existingTitleHint = titleHint,
            )
            _aiProductAnalysisState.value = result.fold(
                onSuccess = { suggestion ->
                    ProductAiAnalysisState.Success(
                        suggestion = suggestion,
                        analyzedSignature = signature,
                        appliedToFields = false,
                    )
                },
                onFailure = { err ->
                    ProductAiAnalysisState.Error(
                        message = err.localizedMessage ?: "AI photo analysis failed.",
                        analyzedSignature = signature,
                    )
                },
            )
        }
    }

    fun markAiDraftApplied() {
        val curr = _aiProductAnalysisState.value
        if (curr is ProductAiAnalysisState.Success && !curr.appliedToFields) {
            _aiProductAnalysisState.value = curr.copy(appliedToFields = true)
        }
    }

    fun setMainPhotoAt(index: Int) {
        val current = _selectedPhotoEntries.value.toMutableList()
        if (index in current.indices && index != 0) {
            val chosen = current.removeAt(index)
            current.add(0, chosen)
            _selectedPhotoEntries.value = current
        }
    }

    fun movePhoto(fromIndex: Int, toIndex: Int) {
        val current = _selectedPhotoEntries.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _selectedPhotoEntries.value = current
        }
    }

    fun removePhotoAt(index: Int) {
        val current = _selectedPhotoEntries.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedPhotoEntries.value = current
        }
    }

    fun clearSelectedPhotos() {
        _selectedPhotoEntries.value = emptyList()
    }

    fun seedStarterStore() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = AdminSeedHelper.seedFoundationCatalog(db, Firebase.auth, _adminRole.value)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "ZEECROWN foundation catalog, delivery rules, coupon, and banners initialized." },
                onFailure = { it.localizedMessage ?: "Failed to initialize foundation catalog." }
            )
        }
    }

    fun saveProduct(
        context: Context,
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
        slug: String = "",
        socialImageUrl: String = "",
        sizeGuideId: String = "",
        summary: String = "",
        featuresText: String = "",
        fashionStyle: String = "",
        suitableOccasionsText: String = "",
        tagsText: String = "",
        aiAssistedDraft: Boolean = false,
        onDone: () -> Unit = {},
    ) {
        if (isSavingProduct) return
        viewModelScope.launch {
            isSavingProduct = true
            _isBusy.value = true
            try {
                val sizes = sizesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("One Size") }
                val colors = colorsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Royal Purple") }
                val featuresList = featuresText.lines()
                    .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                    .filter { it.isNotEmpty() }
                val occasionsList = suitableOccasionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val tagsList = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val parsedAttributes = attributesText.lines()
                    .mapNotNull { line ->
                        val parts = line.split(":", limit = 2)
                        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                            parts[0].trim() to parts[1].trim()
                        } else null
                    }
                    .toMap()
                val parsedUnavailable = unavailableVariantsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val categoryName = (allCategoriesAdminState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == categoryId }?.name.orEmpty()
                val collectionName = (collectionsState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == collectionId }?.name.orEmpty()
                val photoEntries = _selectedPhotoEntries.value.ifEmpty { listOf("drawable://$fallbackDrawableKey") }

                val res = opsRepository.saveOrUpdateProduct(
                    context = context,
                    actorRole = _adminRole.value,
                    existingProductId = existingProductId,
                    title = title,
                    description = description,
                    categoryId = categoryId,
                    collectionId = collectionId,
                    brand = brand,
                    sku = sku,
                    priceNgn = priceNgn,
                    compareAtPriceNgn = compareAtPriceNgn,
                    priceUsd = priceUsd,
                    sizes = sizes,
                    colors = colors,
                    imageEntries = photoEntries,
                    initialOrUpdatedStock = stockQuantity,
                    lowStockThreshold = lowStockThreshold,
                    featured = featured,
                    newArrival = newArrival,
                    active = active,
                    attributes = parsedAttributes,
                    unavailableVariants = parsedUnavailable,
                    seoTitle = seoTitle,
                    seoDescription = seoDescription,
                    categoryName = categoryName,
                    collectionName = collectionName,
                    slug = slug,
                    socialImageUrl = socialImageUrl,
                    sizeGuideId = sizeGuideId,
                    summary = summary,
                    features = featuresList,
                    fashionStyle = fashionStyle,
                    suitableOccasions = occasionsList,
                    tags = tagsList,
                    aiAssistedDraft = aiAssistedDraft,
                    preallocatedNewProductId = _draftProductDocId.value,
                    onUploadProgress = { pct -> _uploadProgress.value = pct },
                )
                res.fold(
                    onSuccess = {
                        _selectedPhotoEntries.value = emptyList()
                        _aiProductAnalysisState.value = ProductAiAnalysisState.Idle
                        _draftProductDocId.value = null
                        _adminMessage.value = when {
                            existingProductId.isNullOrBlank() && !active -> "Product draft saved safely (not live yet)."
                            existingProductId.isNullOrBlank() -> "Product approved & published to storefront."
                            !active -> "Product saved as hidden draft."
                            else -> "Product updated live in catalog."
                        }
                        onDone()
                    },
                    onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save product." }
                )
            } finally {
                _uploadProgress.value = null
                _isBusy.value = false
                isSavingProduct = false
            }
        }
    }

    fun setProductActive(product: Product, active: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.setProductActive(_adminRole.value, product, active)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { if (active) "'${product.title}' published." else "'${product.title}' archived." },
                onFailure = { it.localizedMessage ?: "Failed to update product visibility." }
            )
        }
    }

    fun duplicateProduct(product: Product) {
        val currentStock = (inventoryState.value as? UiState.Success)?.data?.get(product.id)?.stockQuantity ?: 5L
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.duplicateProduct(_adminRole.value, product, currentStock)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Created draft duplicate of '${product.title}'." },
                onFailure = { it.localizedMessage ?: "Failed to duplicate product." }
            )
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.deleteProduct(_adminRole.value, product)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "'${product.title}' deleted permanently." },
                onFailure = { it.localizedMessage ?: "Failed to delete product." }
            )
        }
    }

    fun saveCategory(
        context: Context,
        categoryId: String?,
        name: String,
        description: String,
        parentCategoryId: String,
        imageKeyOrUri: String,
        seoTitle: String,
        seoDescription: String,
        sortOrder: Long,
        active: Boolean,
        slug: String = "",
        socialImageUrl: String = "",
        sizeGuideId: String = "",
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveCategory(
                context = context,
                actorRole = _adminRole.value,
                categoryId = categoryId,
                name = name,
                description = description,
                parentCategoryId = parentCategoryId,
                imageKeyOrUri = imageKeyOrUri,
                seoTitle = seoTitle,
                seoDescription = seoDescription,
                sortOrder = sortOrder,
                active = active,
                slug = slug,
                socialImageUrl = socialImageUrl,
                sizeGuideId = sizeGuideId,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Category '$name' saved live."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save category." }
            )
        }
    }

    fun deleteCategorySafely(category: Category) {
        val allProducts = (allProductsState.value as? UiState.Success)?.data.orEmpty()
        val allCategories = (allCategoriesAdminState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.deleteCategorySafely(_adminRole.value, category, allProducts, allCategories)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Category '${category.name}' deleted." },
                onFailure = { it.localizedMessage ?: "Cannot delete category." }
            )
        }
    }

    fun moveCategory(categoryId: String, moveUp: Boolean, categories: List<Category>) {
        val sorted = categories.sortedBy { it.sortOrder }
        val idx = sorted.indexOfFirst { it.id == categoryId }
        if (idx < 0) return
        val swapIdx = if (moveUp) idx - 1 else idx + 1
        if (swapIdx !in sorted.indices) return
        val current = sorted[idx]
        val neighbor = sorted[swapIdx]
        val currentOrder = current.sortOrder
        val neighborOrder = if (neighbor.sortOrder == currentOrder) {
            if (moveUp) (currentOrder - 1L).coerceAtLeast(0L) else currentOrder + 1L
        } else neighbor.sortOrder
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.reorderCategories(_adminRole.value, current.id, neighborOrder, neighbor.id, currentOrder)
            _isBusy.value = false
            res.onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to reorder categories." }
        }
    }

    fun toggleCategoryActive(category: Category) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.setCategoryActive(_adminRole.value, category, !category.active)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "${category.name} ${if (!category.active) "activated" else "hidden"}." },
                onFailure = { it.localizedMessage ?: "Failed to update category." }
            )
        }
    }

    fun adjustInventory(
        product: Product,
        currentRecord: InventoryRecord?,
        newTotalStock: Long,
        newThreshold: Long,
        variantKey: String,
        newVariantQty: Long?,
        reason: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.adjustInventoryStock(
                actorRole = _adminRole.value,
                product = product,
                currentRecord = currentRecord,
                newTotalStock = newTotalStock,
                newThreshold = newThreshold,
                variantKey = variantKey,
                newVariantQty = newVariantQty,
                reason = reason,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Updated '${product.title}' stock to $newTotalStock."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to update stock." }
            )
        }
    }

    fun updateOrderFulfillment(
        order: Order,
        newStatus: OrderStatus,
        internalNotes: String,
        cancellationDecision: String = order.cancellationStatus,
        courierName: String = order.courierName,
        trackingReference: String = order.trackingReference,
        trackingUrl: String = order.trackingUrl,
        estimatedDeliveryWindow: String = order.estimatedDeliveryWindow,
        deliveryStatus: String = newStatus.label,
        deliveryNotes: String = order.deliveryNotes,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.updateOrderFulfillment(
                actorRole = _adminRole.value,
                order = order,
                newStatus = newStatus,
                internalNotes = internalNotes,
                cancellationDecision = cancellationDecision,
                courierName = courierName,
                trackingReference = trackingReference,
                trackingUrl = trackingUrl,
                estimatedDeliveryWindow = estimatedDeliveryWindow,
                deliveryStatus = deliveryStatus,
                deliveryNotes = deliveryNotes,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Order ${order.orderNumber} updated to ${newStatus.label}."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to update order." }
            )
        }
    }

    fun reviewPayment(
        payment: PaymentRecord,
        order: Order?,
        approve: Boolean,
        verificationNote: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.reviewPaymentVerification(
                actorRole = _adminRole.value,
                payment = payment,
                order = order,
                approve = approve,
                verificationNote = verificationNote,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = if (approve) {
                        "Payment ${payment.reference} verified & order marked PAID."
                    } else {
                        "Payment ${payment.reference} rejected."
                    }
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Payment review failed." }
            )
        }
    }

    fun recordRefund(order: Order, amountNgn: Double, reason: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.recordOrderRefund(_adminRole.value, order, amountNgn, reason)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Refund recorded on ${order.orderNumber}."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Refund recording failed." }
            )
        }
    }

    fun loadCustomerDetails(userId: String) {
        viewModelScope.launch {
            _isLoadingCustomerDetails.value = true
            _selectedCustomerDetails.value = null
            val res = adminRepository.fetchCustomerDetails(userId)
            _isLoadingCustomerDetails.value = false
            res.fold(
                onSuccess = { _selectedCustomerDetails.value = it },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Could not load customer details." }
            )
        }
    }

    fun updateCustomerNotes(userId: String, notes: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.updateCustomerSupportNotes(_adminRole.value, userId, notes)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Customer support notes saved." },
                onFailure = { it.localizedMessage ?: "Failed to save customer notes." }
            )
        }
    }

    fun saveAdminTeamMember(
        uid: String,
        email: String,
        displayName: String,
        role: AdminRole,
        active: Boolean,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveAdminTeamMember(_adminRole.value, uid, email, displayName, role, active)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Admin role saved for $email."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save admin team member." }
            )
        }
    }

    fun saveCoupon(couponId: String?, coupon: Coupon, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveCoupon(_adminRole.value, couponId, coupon)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Promo code ${coupon.code.uppercase()} saved."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save promo code." }
            )
        }
    }

    fun deleteCoupon(coupon: Coupon) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.deleteCoupon(_adminRole.value, coupon)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Promo code ${coupon.code} deleted." },
                onFailure = { it.localizedMessage ?: "Failed to delete promo code." }
            )
        }
    }

    fun saveDeliveryZone(zoneId: String?, zone: DeliveryZone, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveDeliveryZone(_adminRole.value, zoneId, zone)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Delivery zone '${zone.name}' saved."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save zone." }
            )
        }
    }

    fun deleteDeliveryZone(zone: DeliveryZone) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.deleteDeliveryZone(_adminRole.value, zone)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Delivery zone '${zone.name}' removed." },
                onFailure = { it.localizedMessage ?: "Failed to remove zone." }
            )
        }
    }

    fun saveDeliverySettings(settings: DeliverySettings) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveDeliverySettings(_adminRole.value, settings)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Global shipping thresholds & pickup settings saved." },
                onFailure = { it.localizedMessage ?: "Failed to save delivery settings." }
            )
        }
    }

    fun saveHomepageSection(
        context: Context,
        sectionId: String,
        content: HomepageContent,
        localImageUri: Uri? = null,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveHomepageSection(
                context = context,
                actorRole = _adminRole.value,
                sectionId = sectionId,
                content = content,
                localImageUri = localImageUri,
                onUploadProgress = { pct -> _uploadProgress.value = pct },
            )
            _uploadProgress.value = null
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Homepage banner '${content.title}' published live." },
                onFailure = { it.localizedMessage ?: "Failed to save banner." }
            )
        }
    }

    fun saveCollection(collectionId: String?, col: StoreCollection, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveCollection(_adminRole.value, collectionId, col)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Collection '${col.name}' saved."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save collection." }
            )
        }
    }

    fun saveTestimonial(testimonialId: String?, testimonial: Testimonial, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveTestimonial(_adminRole.value, testimonialId, testimonial)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Testimonial saved."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save testimonial." }
            )
        }
    }

    fun deleteTestimonial(id: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.deleteTestimonial(_adminRole.value, id)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Testimonial deleted." },
                onFailure = { it.localizedMessage ?: "Failed to delete testimonial." }
            )
        }
    }

    fun moderateReview(reviewId: String, visible: Boolean, status: String, reason: String = "") {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.moderateReview(_adminRole.value, reviewId, visible, status, reason)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Review updated to $status." },
                onFailure = { it.localizedMessage ?: "Failed to moderate review." }
            )
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.deleteReview(_adminRole.value, reviewId)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Review permanently deleted." },
                onFailure = { it.localizedMessage ?: "Failed to delete review." }
            )
        }
    }

    fun saveStoreSettings(context: Context, settings: StoreSettings, newLogoUri: Uri? = null) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveStoreSettings(
                context = context,
                actorRole = _adminRole.value,
                settings = settings,
                newLogoUri = newLogoUri,
                onUploadProgress = { pct -> _uploadProgress.value = pct },
            )
            _uploadProgress.value = null
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Store profile, logo, payment gateways & policies saved." },
                onFailure = { it.localizedMessage ?: "Failed to save store settings." }
            )
        }
    }

    fun updateSupportEnquiryMeta(
        enquiry: SupportEnquiry,
        newStatus: SupportStatus,
        assignToMe: Boolean = false,
        assignedAdmin: AdminUser? = null,
        clearAssignment: Boolean = false,
        priority: String? = null,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.updateSupportEnquiryMeta(
                actorRole = _adminRole.value,
                enquiry = enquiry,
                newStatus = newStatus,
                assignToMe = assignToMe,
                assignedAdmin = assignedAdmin,
                clearAssignment = clearAssignment,
                priority = priority,
            )
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Enquiry updated (${newStatus.label})." },
                onFailure = { it.localizedMessage ?: "Failed to update enquiry." }
            )
        }
    }

    fun postAdminSupportReply(
        enquiry: SupportEnquiry,
        replyMessage: String,
        nextStatus: SupportStatus = SupportStatus.WAITING_CUSTOMER,
        onSent: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.postAdminSupportReply(
                actorRole = _adminRole.value,
                enquiry = enquiry,
                replyMessage = replyMessage,
                nextStatus = nextStatus,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Customer reply sent & notification triggered."
                    onSent()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to send support reply." }
            )
        }
    }

    fun addAdminSupportInternalNote(
        enquiry: SupportEnquiry,
        noteText: String,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.addAdminSupportInternalNote(
                actorRole = _adminRole.value,
                enquiry = enquiry,
                noteText = noteText,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Private internal staff note added."
                    onSaved()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save internal note." }
            )
        }
    }

    fun sendManualOrderUpdateNotification(
        order: Order,
        title: String,
        message: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.sendManualOrderUpdateNotification(
                actorRole = _adminRole.value,
                order = order,
                title = title,
                message = message,
            )
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Customer notification delivered for ${order.orderNumber}."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to send manual update." }
            )
        }
    }

    fun retryCommunicationLog(log: CommunicationLog) {
        val currentSettings = (storeSettingsState.value as? UiState.Success)?.data ?: StoreSettings()
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.retryCommunicationLogAdmin(_adminRole.value, log, currentSettings)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { it },
                onFailure = { it.localizedMessage ?: "Retry operation failed." }
            )
        }
    }

    fun saveAiConfig(config: AiAssistantConfig) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveAiAssistantConfig(_adminRole.value, config)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "ZEECROWN AI Assistant configuration saved live." },
                onFailure = { it.localizedMessage ?: "Failed to save AI configuration." }
            )
        }
    }

    fun saveAiFaq(faqId: String?, entry: AiFaqEntry, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveAiFaq(_adminRole.value, faqId, entry)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Verified AI FAQ saved live."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save AI FAQ." }
            )
        }
    }

    fun deleteAiFaq(faqId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.deleteAiFaq(_adminRole.value, faqId)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "AI FAQ entry removed." },
                onFailure = { it.localizedMessage ?: "Failed to remove AI FAQ." }
            )
        }
    }

    fun promoteUnansweredToFaq(
        item: AiUnansweredQuestion,
        verifiedAnswer: String,
        category: String,
        keywordsText: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.promoteUnansweredToFaq(_adminRole.value, item, verifiedAnswer, category, keywordsText)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Promoted verified answer into live AI Knowledge Base."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to promote FAQ." }
            )
        }
    }

    fun dismissUnansweredQuestion(questionId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.dismissUnansweredQuestion(_adminRole.value, questionId)
            _isBusy.value = false
            res.onFailure { _adminMessage.value = it.localizedMessage ?: "Could not dismiss item." }
        }
    }

    fun runAiSandboxTest(query: String, previewConfig: AiAssistantConfig) {
        val clean = query.trim()
        if (clean.isBlank() || _aiTestRunning.value) return
        viewModelScope.launch {
            _aiTestRunning.value = true
            val snapshot = AiStoreSnapshot(
                config = previewConfig,
                products = (allProductsState.value as? UiState.Success)?.data.orEmpty(),
                categories = (allCategoriesAdminState.value as? UiState.Success)?.data.orEmpty(),
                collections = (collectionsState.value as? UiState.Success)?.data.orEmpty(),
                inventoryMap = (inventoryState.value as? UiState.Success)?.data.orEmpty(),
                deliveryZones = (deliveryZonesState.value as? UiState.Success)?.data.orEmpty(),
                deliverySettings = (deliverySettingsState.value as? UiState.Success)?.data ?: DeliverySettings(),
                storeSettings = (storeSettingsState.value as? UiState.Success)?.data ?: StoreSettings(),
                coupons = (couponsState.value as? UiState.Success)?.data.orEmpty(),
                faqs = (aiFaqsState.value as? UiState.Success)?.data.orEmpty(),
                customerOrders = emptyList(),
                isSignedIn = false,
                currentUserId = "",
                customerFirstName = "Preview Guest",
            )
            _aiTestReply.value = aiRepository.generateAssistantReply(
                userQuery = clean,
                history = emptyList(),
                snapshot = snapshot,
                currency = StoreCurrency.NGN,
            )
            _aiTestRunning.value = false
        }
    }

    fun saveSizeGuide(guideId: String?, guide: SizeGuide, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.saveSizeGuide(_adminRole.value, guideId, guide)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _adminMessage.value = "Size guide '${guide.name}' saved live."
                    onDone()
                },
                onFailure = { _adminMessage.value = it.localizedMessage ?: "Failed to save size guide." }
            )
        }
    }

    fun deleteSizeGuide(guide: SizeGuide) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = opsRepository.deleteSizeGuide(_adminRole.value, guide)
            _isBusy.value = false
            _adminMessage.value = res.fold(
                onSuccess = { "Deleted size guide '${guide.name}'." },
                onFailure = { it.localizedMessage ?: "Failed to delete size guide." }
            )
        }
    }

    private companion object {
        const val TAG = "AdminVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
