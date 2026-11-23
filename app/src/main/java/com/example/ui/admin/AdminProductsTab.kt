package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.firebase.UiState
import com.example.data.model.Category
import com.example.data.model.InventoryRecord
import com.example.data.model.Product
import com.example.data.model.ProductAiAnalysisState
import com.example.data.model.ProductDraftAiParser
import com.example.data.model.SeoLengthStatus
import com.example.data.model.SeoUtils
import com.example.data.model.SizeGuide
import com.example.data.model.StoreCollection
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownAmberWarning
import com.example.ui.theme.ZeecrownChampagneGold
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory

private enum class ProductStatusFilter(val label: String) {
    ALL("All"),
    ACTIVE("Published"),
    ARCHIVED("Hidden"),
    LOW_STOCK("Low/Out Stock"),
}

@Composable
fun AdminProductsTab(
    allProductsState: UiState<List<Product>>,
    categories: List<Category>,
    collections: List<StoreCollection>,
    sizeGuides: List<SizeGuide> = emptyList(),
    inventoryMap: Map<String, InventoryRecord>,
    selectedPhotoEntries: List<String>,
    aiAnalysisState: ProductAiAnalysisState = ProductAiAnalysisState.Idle,
    isBusy: Boolean,
    uploadProgress: Float? = null,
    onStartNewProductDraft: () -> Unit = {},
    onPrepareEditProduct: () -> Unit = {},
    onLoadPhotoEntries: (List<String>) -> Unit,
    onAddPhotos: (List<Uri>, String) -> Unit,
    onAnalyzeProductPhotos: (String) -> Unit = {},
    onClearAiDraft: () -> Unit = {},
    onMarkAiDraftApplied: () -> Unit = {},
    onSetMainPhotoAt: (Int) -> Unit,
    onMovePhoto: (Int, Int) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onClearPhotos: () -> Unit,
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
    onToggleActive: (Product, Boolean) -> Unit,
    onDuplicateProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
) {
    val products = (allProductsState as? UiState.Success)?.data.orEmpty()
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }

    var showEditorForm by remember { mutableStateOf(false) }
    var editingProductId by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(ProductStatusFilter.ALL) }
    var categoryFilterId by remember { mutableStateOf<String?>(null) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }

    // Form fields
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var featuresText by remember { mutableStateOf("") }
    var fashionStyle by remember { mutableStateOf("") }
    var occasionsText by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var aiAssistedDraft by remember { mutableStateOf(false) }
    var selectedCatId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id.orEmpty()) }
    var selectedColId by remember { mutableStateOf("") }
    var selectedSizeGuideId by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("Zeecrown Atelier") }
    var sku by remember { mutableStateOf("") }
    var priceNgnText by remember { mutableStateOf("") }
    var compareAtNgnText by remember { mutableStateOf("") }
    var priceUsdText by remember { mutableStateOf("") }
    var sizesText by remember { mutableStateOf("") }
    var colorsText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("10") }
    var thresholdText by remember { mutableStateOf("3") }
    var attributesText by remember { mutableStateOf("") }
    var unavailableVariantsText by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var seoTitle by remember { mutableStateOf("") }
    var seoDescription by remember { mutableStateOf("") }
    var socialImageUrl by remember { mutableStateOf("") }
    var featured by remember { mutableStateOf(true) }
    var newArrival by remember { mutableStateOf(true) }
    var active by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(aiAnalysisState) {
        if (aiAnalysisState is ProductAiAnalysisState.Success && !aiAnalysisState.appliedToFields) {
            val s = aiAnalysisState.suggestion
            if (s.suggestedTitle.isNotBlank()) title = s.suggestedTitle
            if (s.summary.isNotBlank()) summary = s.summary
            if (s.description.isNotBlank()) description = s.description
            if (s.visibleFeatures.isNotEmpty()) featuresText = s.visibleFeatures.joinToString("\n")
            if (s.suggestedCategoryId.isNotBlank()) selectedCatId = s.suggestedCategoryId
            if (s.suggestedColors.isNotEmpty()) colorsText = s.suggestedColors.joinToString(", ")
            if (s.fashionStyle.isNotBlank()) fashionStyle = s.fashionStyle
            if (s.suitableOccasions.isNotEmpty()) occasionsText = s.suitableOccasions.joinToString(", ")
            if (s.tags.isNotEmpty() || s.searchKeywords.isNotEmpty()) {
                tagsText = (s.tags + s.searchKeywords).distinct().joinToString(", ")
            }
            if (s.seoTitle.isNotBlank()) seoTitle = s.seoTitle
            if (s.seoDescription.isNotBlank()) seoDescription = s.seoDescription
            if (slug.isBlank() && s.suggestedTitle.isNotBlank()) {
                slug = SeoUtils.sanitizeSlug(s.suggestedTitle, "zeecrown-piece")
            }
            aiAssistedDraft = true
            onMarkAiDraftApplied()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddPhotos(uris, title)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        val uri = pendingCameraUri
        if (captured && uri != null) {
            onAddPhotos(listOf(uri), title)
        }
        pendingCameraUri = null
    }

    fun openCreateForm() {
        editingProductId = null
        title = ""
        summary = ""
        description = ""
        featuresText = ""
        fashionStyle = ""
        occasionsText = ""
        tagsText = ""
        aiAssistedDraft = false
        selectedCatId = categories.firstOrNull()?.id.orEmpty()
        selectedColId = ""
        selectedSizeGuideId = ""
        brand = "Zeecrown Atelier"
        sku = ""
        priceNgnText = ""
        compareAtNgnText = ""
        priceUsdText = ""
        sizesText = ""
        colorsText = ""
        stockText = "10"
        thresholdText = "3"
        attributesText = ""
        unavailableVariantsText = ""
        slug = ""
        seoTitle = ""
        seoDescription = ""
        socialImageUrl = ""
        featured = true
        newArrival = true
        active = false
        onStartNewProductDraft()
        showEditorForm = true
    }

    fun openEditForm(prod: Product) {
        editingProductId = prod.id
        title = prod.title
        summary = prod.summary
        description = prod.description
        featuresText = prod.features.joinToString("\n")
        fashionStyle = prod.fashionStyle
        occasionsText = prod.suitableOccasions.joinToString(", ")
        tagsText = prod.tags.joinToString(", ")
        aiAssistedDraft = prod.aiAssistedDraft
        selectedCatId = prod.categoryId.ifBlank { categories.firstOrNull()?.id.orEmpty() }
        selectedColId = prod.collectionId
        selectedSizeGuideId = prod.sizeGuideId
        brand = prod.brand.ifBlank { "Zeecrown Atelier" }
        sku = prod.sku
        priceNgnText = prod.priceNgn.toLong().toString()
        compareAtNgnText = prod.compareAtPriceNgn?.toLong()?.toString().orEmpty()
        priceUsdText = prod.priceUsd.toLong().toString()
        sizesText = prod.sizes.joinToString(", ")
        colorsText = prod.colors.joinToString(", ")
        stockText = (inventoryMap[prod.id]?.stockQuantity ?: 10L).toString()
        thresholdText = (inventoryMap[prod.id]?.lowStockThreshold ?: prod.lowStockThreshold).toString()
        attributesText = prod.attributes.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        unavailableVariantsText = prod.unavailableVariants.joinToString(", ")
        slug = prod.slug
        seoTitle = prod.seoTitle
        seoDescription = prod.seoDescription
        socialImageUrl = prod.socialImageUrl
        featured = prod.featured
        newArrival = prod.newArrival
        active = prod.active
        onPrepareEditProduct()
        onLoadPhotoEntries(prod.imageUrls.ifEmpty { listOfNotNull(prod.mainImageUrl.takeIf { it.isNotBlank() }) })
        showEditorForm = true
    }

    val filteredProducts = remember(products, inventoryMap, searchQuery, statusFilter, categoryFilterId) {
        val q = searchQuery.trim().lowercase()
        products.filter { prod ->
            val rec = inventoryMap[prod.id]
            val qty = rec?.stockQuantity ?: 0L
            val threshold = rec?.lowStockThreshold ?: prod.lowStockThreshold
            val matchesQuery = q.isBlank() ||
                prod.title.lowercase().contains(q) ||
                prod.sku.lowercase().contains(q) ||
                prod.brand.lowercase().contains(q)
            val matchesCat = categoryFilterId == null || prod.categoryId == categoryFilterId
            val matchesStatus = when (statusFilter) {
                ProductStatusFilter.ALL -> true
                ProductStatusFilter.ACTIVE -> prod.active
                ProductStatusFilter.ARCHIVED -> !prod.active
                ProductStatusFilter.LOW_STOCK -> qty <= threshold
            }
            matchesQuery && matchesCat && matchesStatus
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Products Catalog (${products.size})", style = MaterialTheme.typography.titleLarge)
                    Text("Create, edit, duplicate, upload photos, or archive items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { if (showEditorForm) showEditorForm = false else openCreateForm() },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_toggle_product_form"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) {
                    Icon(if (showEditorForm) Icons.Outlined.Close else Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showEditorForm) "Close Form" else "New Product")
                }
            }
        }

        if (showEditorForm) {
            item {
                val currentPhotoSig = remember(selectedPhotoEntries) { ProductDraftAiParser.computePhotoSignature(selectedPhotoEntries) }
                AdminSectionCard {
                    Text(
                        text = if (editingProductId == null) "Create New Product" else "Edit Product Details",
                        style = MaterialTheme.typography.titleLarge,
                        color = ZeecrownDeepPurple,
                    )

                    Text("1. Product Photos (${selectedPhotoEntries.size}/10) — Upload First for AI Listing Assistance", style = MaterialTheme.typography.titleSmall, color = ZeecrownDeepPurple)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("admin_pick_gallery_photos"),
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gallery")
                        }
                        OutlinedButton(
                            onClick = {
                                val tempFile = File(context.cacheDir, "zeecrown_cam_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("admin_capture_camera_photo"),
                        ) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Camera")
                        }
                    }

                    // AI Product Description Generator Card
                    AdminSectionCard(containerColor = ZeecrownLightLilac) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = ZeecrownRoyalPurple, modifier = Modifier.size(18.dp))
                                Text("AI Product Description Generator", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ZeecrownDeepPurple)
                            }
                            if (aiAssistedDraft) {
                                AdminStatusPill(label = "AI-Suggested — Review Before Publishing", tone = PillTone.GOLD)
                            }
                        }
                        when (aiAnalysisState) {
                            is ProductAiAnalysisState.Idle -> {
                                Text(
                                    text = if (selectedPhotoEntries.isEmpty()) {
                                        "Upload one or more product photos and AI will automatically inspect them together to draft title, summary, description, visible features, category, colors, occasions, SEO, and search tags — never guessing price, stock, or sizes."
                                    } else {
                                        "Ready to inspect ${selectedPhotoEntries.size} photo(s) as one ZEECROWN product."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            is ProductAiAnalysisState.Analyzing -> {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ZeecrownRoyalPurple)
                                    Text(
                                        "Analyzing ${aiAnalysisState.photoCount} product photo(s) together...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ZeecrownDeepPurple,
                                    )
                                }
                            }
                            is ProductAiAnalysisState.Success -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ZeecrownAmberWarning.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, ZeecrownMetallicGold),
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "AI-Suggested Draft Populated — Verify Title, Category, Price & Stock Before Publishing",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ZeecrownDeepPurple,
                                        )
                                        if (aiAnalysisState.suggestion.uncertainNotes.isNotEmpty()) {
                                            Text(
                                                "Needs admin check: ${aiAnalysisState.suggestion.uncertainNotes.joinToString("; ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (currentPhotoSig != aiAnalysisState.analyzedSignature && selectedPhotoEntries.isNotEmpty()) {
                                            Text(
                                                "Photos changed since the last AI analysis — tap Regenerate from Updated Photos.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ZeecrownGoldInk,
                                            )
                                        }
                                    }
                                }
                            }
                            is ProductAiAnalysisState.Error -> {
                                BoutiqueErrorBanner(message = aiAnalysisState.message)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            val needsRegenerate = aiAnalysisState is ProductAiAnalysisState.Success || aiAnalysisState is ProductAiAnalysisState.Error
                            Button(
                                onClick = { onAnalyzeProductPhotos(title) },
                                enabled = !isBusy && selectedPhotoEntries.isNotEmpty() && aiAnalysisState !is ProductAiAnalysisState.Analyzing,
                                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("admin_ai_analyze_photos_btn"),
                            ) {
                                Icon(if (needsRegenerate) Icons.Outlined.Refresh else Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (needsRegenerate) "Regenerate from Updated Photos" else "Generate from Photos", style = MaterialTheme.typography.labelMedium)
                            }
                            if (aiAssistedDraft || aiAnalysisState !is ProductAiAnalysisState.Idle) {
                                OutlinedButton(
                                    onClick = {
                                        onClearAiDraft()
                                        aiAssistedDraft = false
                                    },
                                    enabled = aiAnalysisState !is ProductAiAnalysisState.Analyzing,
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_ai_clear_draft_btn"),
                                ) {
                                    Text("Clear AI Draft", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Product Title *") }, modifier = Modifier.fillMaxWidth().testTag("admin_product_title_input"), singleLine = true)
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Short Luxury Summary") }, modifier = Modifier.fillMaxWidth().testTag("admin_product_summary_input"), minLines = 2)
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Full Editorial Description") }, modifier = Modifier.fillMaxWidth().testTag("admin_product_description_input"), minLines = 4)
                    OutlinedTextField(value = featuresText, onValueChange = { featuresText = it }, label = { Text("Visible Features (one per line)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = fashionStyle, onValueChange = { fashionStyle = it }, label = { Text("Fashion Style") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = occasionsText, onValueChange = { occasionsText = it }, label = { Text("Suitable Occasions (comma-separated)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Tags & Search Keywords (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = priceNgnText, onValueChange = { priceNgnText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Price NGN *") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = compareAtNgnText, onValueChange = { compareAtNgnText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Compare At NGN") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = priceUsdText, onValueChange = { priceUsdText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Price USD") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = stockText, onValueChange = { stockText = it.filter { c -> c.isDigit() } }, label = { Text("Initial Stock") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = thresholdText, onValueChange = { thresholdText = it.filter { c -> c.isDigit() } }, label = { Text("Low-Stock Alert") }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Text("Category *", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(cat.name) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                        }
                    }

                    if (collections.isNotEmpty()) {
                        Text("Collection (Optional)", style = MaterialTheme.typography.titleSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(selected = selectedColId.isBlank(), onClick = { selectedColId = "" }, label = { Text("None") }, modifier = Modifier.minimumInteractiveComponentSize())
                            }
                            items(collections, key = { it.id }) { col ->
                                FilterChip(selected = selectedColId == col.id, onClick = { selectedColId = col.id }, label = { Text(col.name) }, modifier = Modifier.minimumInteractiveComponentSize())
                            }
                        }
                    }

                    if (sizeGuides.isNotEmpty()) {
                        Text("Size Guide Override (Optional)", style = MaterialTheme.typography.titleSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedSizeGuideId.isBlank(),
                                    onClick = { selectedSizeGuideId = "" },
                                    label = { Text("Auto / Category Default") },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                )
                            }
                            items(sizeGuides, key = { it.id }) { guide ->
                                FilterChip(
                                    selected = selectedSizeGuideId == guide.id,
                                    onClick = { selectedSizeGuideId = guide.id },
                                    label = { Text(guide.name) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = sizesText, onValueChange = { sizesText = it }, label = { Text("Sizes (Admin-Entered, comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = colorsText, onValueChange = { colorsText = it }, label = { Text("Colors (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = unavailableVariantsText, onValueChange = { unavailableVariantsText = it }, label = { Text("Unavailable Variants (e.g. M • Purple)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attributesText, onValueChange = { attributesText = it }, label = { Text("Verified Specifications (Key: Value per line)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                    AdminSectionCard(containerColor = ZeecrownWarmIvory) {
                        val titleFeedback = SeoUtils.evaluateTitleLength(seoTitle)
                        val descFeedback = SeoUtils.evaluateDescriptionLength(seoDescription)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Search Engine & Social SEO", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            OutlinedButton(
                                onClick = {
                                    val generated = SeoUtils.generateProductSeoDefaults(
                                        title = title,
                                        description = description,
                                        categoryName = categoryNames[selectedCatId].orEmpty(),
                                        brand = brand,
                                        explicitSlug = slug,
                                    )
                                    if (slug.isBlank()) slug = generated.slug
                                    seoTitle = generated.seoTitle
                                    seoDescription = generated.seoDescription
                                },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_product_generate_seo_btn"),
                            ) {
                                Text("Generate SEO Defaults")
                            }
                        }
                        OutlinedTextField(
                            value = slug,
                            onValueChange = { slug = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '-' } },
                            label = { Text("Clean URL Slug (e.g. silk-adire-boubou)") },
                            supportingText = {
                                val effective = SeoUtils.sanitizeSlug(slug.ifBlank { title }, "zeecrown-piece")
                                Text("Public URL: /product/$effective")
                            },
                            modifier = Modifier.fillMaxWidth().testTag("admin_product_slug_input"),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = seoTitle,
                            onValueChange = { seoTitle = it.take(120) },
                            label = { Text("SEO Meta Title") },
                            supportingText = {
                                Text(
                                    text = titleFeedback.guidance,
                                    color = if (titleFeedback.status == SeoLengthStatus.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("admin_product_seo_title_input"),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = seoDescription,
                            onValueChange = { seoDescription = it.take(300) },
                            label = { Text("SEO Meta Description") },
                            supportingText = {
                                Text(
                                    text = descFeedback.guidance,
                                    color = if (descFeedback.status == SeoLengthStatus.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("admin_product_seo_desc_input"),
                            minLines = 2,
                        )
                        OutlinedTextField(
                            value = socialImageUrl,
                            onValueChange = { socialImageUrl = it.take(600) },
                            label = { Text("Social / Open Graph Image URL (Optional)") },
                            supportingText = { Text("Leave blank to use main product photo when shared.") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }

                    Text("Selected Photo Gallery (${selectedPhotoEntries.size}/10)", style = MaterialTheme.typography.titleSmall)
                    if (uploadProgress != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Uploading optimized photos to Firebase Storage (${(uploadProgress * 100).toInt()}%)...", style = MaterialTheme.typography.labelSmall)
                            LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    if (selectedPhotoEntries.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(selectedPhotoEntries) { idx, entry ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp))) {
                                        if (entry.startsWith("drawable://")) {
                                            ZeecrownProductImage(imageUrl = entry, contentDescription = "Product photo", modifier = Modifier.fillMaxSize())
                                        } else {
                                            AsyncImage(model = entry, contentDescription = "Product photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                        IconButton(
                                            onClick = { onRemovePhoto(idx) },
                                            modifier = Modifier.align(Alignment.TopEnd).size(26.dp).background(ZeecrownDeepPurple.copy(alpha = 0.78f), CircleShape),
                                        ) {
                                            Icon(Icons.Outlined.Close, contentDescription = "Remove photo", tint = ZeecrownPureWhite, modifier = Modifier.size(14.dp))
                                        }
                                        Surface(
                                            onClick = { onSetMainPhotoAt(idx) },
                                            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (idx == 0) ZeecrownMetallicGold else ZeecrownSoftLavender,
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Star, contentDescription = "Set main", tint = ZeecrownDeepPurple, modifier = Modifier.size(11.dp))
                                                Text(if (idx == 0) " Main" else " #${idx + 1}", style = MaterialTheme.typography.labelSmall, color = ZeecrownDeepPurple)
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { onMovePhoto(idx, idx - 1) }, enabled = idx > 0, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Move left", modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(onClick = { onMovePhoto(idx, idx + 1) }, enabled = idx < selectedPhotoEntries.lastIndex, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Move right", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Published (Visible to Customers)")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Featured on Home Showroom")
                        Switch(checked = featured, onCheckedChange = { featured = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Mark as New Arrival")
                        Switch(checked = newArrival, onCheckedChange = { newArrival = it })
                    }

                    val parsedNgn = priceNgnText.toDoubleOrNull() ?: 0.0
                    fun submitWithVisibility(publishLive: Boolean) {
                        onSaveProduct(
                            editingProductId,
                            title,
                            description,
                            selectedCatId,
                            selectedColId,
                            brand,
                            sku,
                            parsedNgn,
                            compareAtNgnText.toDoubleOrNull(),
                            priceUsdText.toDoubleOrNull() ?: (parsedNgn / 1500.0).coerceAtLeast(1.0),
                            sizesText,
                            colorsText,
                            stockText.toLongOrNull() ?: 10L,
                            thresholdText.toLongOrNull() ?: 3L,
                            featured,
                            newArrival,
                            publishLive,
                            attributesText,
                            unavailableVariantsText,
                            seoTitle,
                            seoDescription,
                            "img_cat_adire_abaya",
                            slug,
                            socialImageUrl,
                            selectedSizeGuideId,
                            summary,
                            featuresText,
                            fashionStyle,
                            occasionsText,
                            tagsText,
                            aiAssistedDraft,
                            { showEditorForm = false },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { submitWithVisibility(false) },
                            enabled = !isBusy && title.trim().length >= 2 && selectedCatId.isNotBlank() && parsedNgn > 0.0,
                            modifier = Modifier.weight(1f).height(48.dp).minimumInteractiveComponentSize().testTag("admin_save_product_draft_button"),
                        ) {
                            Text("Save Draft")
                        }
                        Button(
                            onClick = { submitWithVisibility(true) },
                            enabled = !isBusy && title.trim().length >= 2 && selectedCatId.isNotBlank() && parsedNgn > 0.0,
                            modifier = Modifier.weight(1.35f).height(48.dp).minimumInteractiveComponentSize().testTag("admin_submit_product_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                        ) {
                            Text(if (editingProductId == null) "Approve & Publish" else "Save & Publish Live")
                        }
                    }
                }
            }
        }

        // Search & Filter Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search title, SKU or brand") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProductStatusFilter.entries) { filter ->
                        FilterChip(
                            selected = statusFilter == filter,
                            onClick = { statusFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }
                if (categories.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(selected = categoryFilterId == null, onClick = { categoryFilterId = null }, label = { Text("All Departments") })
                        }
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(selected = categoryFilterId == cat.id, onClick = { categoryFilterId = cat.id }, label = { Text(cat.name) })
                        }
                    }
                }
            }
        }

        when (allProductsState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading products...") }
            is UiState.Error -> item { BoutiqueErrorBanner(allProductsState.message) }
            is UiState.Success -> {
                if (filteredProducts.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Matching Products",
                            subtitle = "Add a product or clear search/filter selections.",
                        )
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { prod ->
                        val inv = inventoryMap[prod.id]
                        val qty = inv?.stockQuantity ?: 0L
                        val threshold = inv?.lowStockThreshold ?: prod.lowStockThreshold
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_product_row_${prod.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ZeecrownProductImage(
                                        imageUrl = prod.mainImageUrl.ifBlank { prod.imageUrls.firstOrNull().orEmpty() },
                                        contentDescription = prod.title,
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                                    )
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(prod.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${categoryNames[prod.categoryId] ?: "Uncategorized"} · SKU ${prod.sku} · ${formatNgnAmount(prod.priceNgn)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AdminStatusPill(
                                                label = if (prod.active) "PUBLISHED" else "HIDDEN",
                                                tone = if (prod.active) PillTone.SUCCESS else PillTone.ROSE,
                                            )
                                            AdminStatusPill(
                                                label = if (qty <= 0L) "OUT OF STOCK" else "Stock: $qty",
                                                tone = when {
                                                    qty <= 0L -> PillTone.ERROR
                                                    qty <= threshold -> PillTone.ROSE
                                                    else -> PillTone.GOLD
                                                },
                                            )
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { openEditForm(prod) }, modifier = Modifier.weight(1f).minimumInteractiveComponentSize()) {
                                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                    OutlinedButton(onClick = { onToggleActive(prod, !prod.active) }, enabled = !isBusy, modifier = Modifier.weight(1f).minimumInteractiveComponentSize()) {
                                        Text(if (prod.active) "Archive" else "Publish")
                                    }
                                    IconButton(onClick = { onDuplicateProduct(prod) }, enabled = !isBusy, modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate ${prod.title}")
                                    }
                                    IconButton(onClick = { productPendingDelete = prod }, enabled = !isBusy, modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${prod.title}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    productPendingDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Delete '${prod.title}'?") },
            text = { Text("This permanently deletes the product and its inventory record. Consider archiving if it appears in historical orders.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(prod)
                        productPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { productPendingDelete = null }) { Text("Cancel") } },
        )
    }
}
