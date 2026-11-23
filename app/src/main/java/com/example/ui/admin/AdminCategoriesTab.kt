package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.firebase.UiState
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.SeoLengthStatus
import com.example.data.model.SeoUtils
import com.example.data.model.SizeGuide
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender

@Composable
fun AdminCategoriesTab(
    allCategoriesState: UiState<List<Category>>,
    allProducts: List<Product>,
    sizeGuides: List<SizeGuide> = emptyList(),
    isBusy: Boolean,
    onSaveCategory: (
        categoryId: String?,
        name: String,
        description: String,
        parentCategoryId: String,
        imageKeyOrUri: String,
        seoTitle: String,
        seoDescription: String,
        sortOrder: Long,
        active: Boolean,
        slug: String,
        socialImageUrl: String,
        sizeGuideId: String,
    ) -> Unit,
    onMoveCategory: (categoryId: String, moveUp: Boolean, categories: List<Category>) -> Unit,
    onToggleCategoryActive: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
) {
    val categories = (allCategoriesState as? UiState.Success)?.data.orEmpty().sortedBy { it.sortOrder }
    val topLevelOptions = remember(categories) { categories.filter { it.parentCategoryId.isBlank() } }

    var searchQuery by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var parentCategoryId by remember { mutableStateOf("") }
    var imageKeyOrUri by remember { mutableStateOf("img_cat_adire_abaya") }
    var slug by remember { mutableStateOf("") }
    var seoTitle by remember { mutableStateOf("") }
    var seoDescription by remember { mutableStateOf("") }
    var socialImageUrl by remember { mutableStateOf("") }
    var sizeGuideId by remember { mutableStateOf("") }
    var sortOrderText by remember { mutableStateOf("9") }
    var active by remember { mutableStateOf(true) }
    var categoryPendingDelete by remember { mutableStateOf<Category?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageKeyOrUri = uri.toString()
        }
    }

    fun loadForEdit(cat: Category) {
        editingId = cat.id
        name = cat.name
        description = cat.description
        parentCategoryId = cat.parentCategoryId
        imageKeyOrUri = cat.imageKey.ifBlank { "img_cat_adire_abaya" }
        slug = cat.slug
        seoTitle = cat.seoTitle
        seoDescription = cat.seoDescription
        socialImageUrl = cat.socialImageUrl
        sizeGuideId = cat.sizeGuideId
        sortOrderText = cat.sortOrder.toString()
        active = cat.active
    }

    fun resetForm() {
        editingId = null
        name = ""
        description = ""
        parentCategoryId = ""
        imageKeyOrUri = "img_cat_adire_abaya"
        slug = ""
        seoTitle = ""
        seoDescription = ""
        socialImageUrl = ""
        sizeGuideId = ""
        sortOrderText = ((categories.maxOfOrNull { it.sortOrder } ?: 0L) + 1L).toString()
        active = true
    }

    val filteredCategories = remember(categories, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) categories
        else categories.filter { it.name.lowercase().contains(q) || it.description.lowercase().contains(q) }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (editingId == null) "Create Category / Subcategory" else "Edit Category",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (editingId != null) {
                        TextButton(onClick = ::resetForm, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Text("New Category")
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_category_name_input"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )

                Text("Parent Department", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = parentCategoryId.isBlank(),
                            onClick = { parentCategoryId = "" },
                            label = { Text("Top-Level") },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                    items(topLevelOptions.filter { it.id != editingId }, key = { it.id }) { parent ->
                        FilterChip(
                            selected = parentCategoryId == parent.id,
                            onClick = { parentCategoryId = parent.id },
                            label = { Text(parent.name) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }

                if (sizeGuides.isNotEmpty()) {
                    Text("Assigned Size Guide (Optional)", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = sizeGuideId.isBlank(),
                                onClick = { sizeGuideId = "" },
                                label = { Text("Auto / None") },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                        }
                        items(sizeGuides, key = { it.id }) { guide ->
                            FilterChip(
                                selected = sizeGuideId == guide.id,
                                onClick = { sizeGuideId = guide.id },
                                label = { Text(guide.name) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (imageKeyOrUri.startsWith("content://") || imageKeyOrUri.startsWith("http")) {
                        AsyncImage(
                            model = imageKeyOrUri,
                            contentDescription = "Selected category image",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    } else {
                        ZeecrownProductImage(
                            imageUrl = "drawable://$imageKeyOrUri",
                            contentDescription = "Category image",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Photo")
                    }
                    OutlinedTextField(
                        value = sortOrderText,
                        onValueChange = { sortOrderText = it.filter { c -> c.isDigit() } },
                        label = { Text("Order") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }

                AdminSectionCard(containerColor = ZeecrownLightLilac) {
                    val titleFeedback = SeoUtils.evaluateTitleLength(seoTitle)
                    val descFeedback = SeoUtils.evaluateDescriptionLength(seoDescription)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Category SEO & Social Sharing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = {
                                val generated = SeoUtils.generateCategorySeoDefaults(name, description, slug)
                                if (slug.isBlank()) slug = generated.slug
                                seoTitle = generated.seoTitle
                                seoDescription = generated.seoDescription
                            },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_category_generate_seo_btn"),
                        ) { Text("Generate SEO Defaults") }
                    }
                    OutlinedTextField(
                        value = slug,
                        onValueChange = { slug = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '-' } },
                        label = { Text("Category URL Slug") },
                        supportingText = { Text("Public URL: /category/${SeoUtils.sanitizeSlug(slug.ifBlank { name }, "category")}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = seoTitle,
                        onValueChange = { seoTitle = it.take(120) },
                        label = { Text("SEO Title (Optional)") },
                        supportingText = {
                            Text(
                                text = titleFeedback.guidance,
                                color = if (titleFeedback.status == SeoLengthStatus.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = seoDescription,
                        onValueChange = { seoDescription = it.take(300) },
                        label = { Text("SEO Description (Optional)") },
                        supportingText = {
                            Text(
                                text = descFeedback.guidance,
                                color = if (descFeedback.status == SeoLengthStatus.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = socialImageUrl,
                        onValueChange = { socialImageUrl = it.take(600) },
                        label = { Text("Social / Open Graph Image URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Category Active & Visible", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                Button(
                    onClick = {
                        onSaveCategory(
                            editingId,
                            name,
                            description,
                            parentCategoryId,
                            imageKeyOrUri,
                            seoTitle,
                            seoDescription,
                            sortOrderText.toLongOrNull() ?: 1L,
                            active,
                            slug,
                            socialImageUrl,
                            sizeGuideId,
                        )
                        resetForm()
                    },
                    enabled = !isBusy && name.trim().length >= 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("admin_save_category_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) {
                    Text(if (editingId == null) "Publish Category" else "Save Category Changes")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Categories (${categories.size})") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        when (allCategoriesState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading categories...") }
            is UiState.Error -> item { BoutiqueErrorBanner(allCategoriesState.message) }
            is UiState.Success -> {
                if (filteredCategories.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Categories Match",
                            subtitle = "Create a category above or clear your search filter.",
                        )
                    }
                } else {
                    val parentNames = categories.associate { it.id to it.name }
                    items(filteredCategories, key = { it.id }) { cat ->
                        val index = categories.indexOfFirst { it.id == cat.id }
                        val productCount = allProducts.count { it.categoryId == cat.id }
                        val childCount = categories.count { it.parentCategoryId == cat.id }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_category_row_${cat.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (cat.active) ZeecrownLightLilac else ZeecrownSoftLavender,
                                            ) {
                                                Text(
                                                    text = if (cat.active) "ACTIVE" else "HIDDEN",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ZeecrownDeepPurple,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                        if (cat.parentCategoryId.isNotBlank()) {
                                            Text(
                                                text = "Subcategory of ${parentNames[cat.parentCategoryId] ?: cat.parentCategoryId}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ZeecrownGoldInk,
                                            )
                                        }
                                        Text(
                                            text = "$productCount product(s) · $childCount subcategory(ies) · Order #${cat.sortOrder}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { onMoveCategory(cat.id, true, categories) },
                                            enabled = !isBusy && index > 0,
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move ${cat.name} up", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { onMoveCategory(cat.id, false, categories) },
                                            enabled = !isBusy && index < categories.lastIndex,
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move ${cat.name} down", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { loadForEdit(cat) },
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit ${cat.name}", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { categoryPendingDelete = cat },
                                            enabled = !isBusy,
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${cat.name}", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onToggleCategoryActive(cat) },
                                    enabled = !isBusy,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(if (cat.active) "Hide Category" else "Activate Category")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    categoryPendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryPendingDelete = null },
            title = { Text("Delete '${cat.name}'?") },
            text = {
                Text("Deleting removes this category from Firestore. Categories with linked products or subcategories are automatically protected from accidental deletion.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { categoryPendingDelete = null }) { Text("Cancel") } },
        )
    }
}
