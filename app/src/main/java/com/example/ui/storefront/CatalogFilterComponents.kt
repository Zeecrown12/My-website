package com.example.ui.storefront

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogFacetOptions
import com.example.data.model.CatalogFilterState
import com.example.data.model.CatalogSortOption
import com.example.data.model.Category
import com.example.data.model.SearchSuggestion
import com.example.data.model.SearchSuggestionType
import com.example.data.model.StoreCollection
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory

@Composable
fun ShopBreadcrumbBar(
    selectedCategory: Category?,
    selectedSubcategory: Category?,
    onNavigateHome: () -> Unit,
    onResetToShop: () -> Unit,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shop_breadcrumb_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TextButton(
            onClick = onNavigateHome,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Text("Home", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ZeecrownGoldInk,
            modifier = Modifier.size(16.dp),
        )
        TextButton(
            onClick = onResetToShop,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Text(
                text = "Shop",
                style = MaterialTheme.typography.labelLarge,
                color = if (selectedCategory == null) ZeecrownDeepPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selectedCategory == null) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        if (selectedCategory != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = ZeecrownGoldInk,
                modifier = Modifier.size(16.dp),
            )
            TextButton(
                onClick = { onSelectCategory(selectedCategory.id) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(
                    text = selectedCategory.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedSubcategory == null) ZeecrownDeepPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedSubcategory == null) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
        if (selectedSubcategory != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = ZeecrownGoldInk,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = selectedSubcategory.name,
                style = MaterialTheme.typography.labelLarge,
                color = ZeecrownDeepPurple,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

@Composable
fun CatalogSearchBarWithSuggestions(
    query: String,
    suggestions: List<SearchSuggestion>,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasFocus by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { hasFocus = it.isFocused }
                .testTag("catalog_search_input"),
            placeholder = { Text("Search product, SKU, Adire, Abayas, Slippers, brand...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("catalog_search_clear"),
                    ) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        if (hasFocus && query.trim().length >= 2 && suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("search_suggestions_panel"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, ZeecrownElegantSilver),
                shadowElevation = 4.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    suggestions.forEachIndexed { idx, suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionClick(suggestion) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = ZeecrownGoldInk,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = suggestion.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ZeecrownDeepPurple,
                                )
                            }
                            Text(
                                text = when (suggestion.type) {
                                    SearchSuggestionType.CATEGORY -> "Department"
                                    SearchSuggestionType.COLLECTION -> "Collection"
                                    SearchSuggestionType.SKU -> "SKU"
                                    SearchSuggestionType.PRODUCT -> "Product"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ZeecrownGoldInk,
                            )
                        }
                        if (idx < suggestions.lastIndex) {
                            HorizontalDivider(color = ZeecrownElegantSilver)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortDropdownChip(
    selectedSort: CatalogSortOption,
    onSelectSort: (CatalogSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("sort_dropdown_button"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Icon(Icons.Outlined.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Sort: ${selectedSort.label}",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CatalogSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (option == selectedSort) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    trailingIcon = {
                        if (option == selectedSort) {
                            Icon(Icons.Outlined.Check, contentDescription = "Selected")
                        }
                    },
                    onClick = {
                        onSelectSort(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("sort_option_${option.name}"),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFilterPillsRow(
    filter: CatalogFilterState,
    categories: List<Category>,
    collections: List<StoreCollection>,
    onUpdateFilter: ((CatalogFilterState) -> CatalogFilterState) -> Unit,
    onClearAll: () -> Unit,
) {
    val categoryMap = categories.associateBy { it.id }
    val collectionMap = collections.associateBy { it.id }
    val activeCount = filter.activeFilterCount()
    if (activeCount == 0 && filter.searchQuery.isBlank()) return

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_filter_pills"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filter.selectedCategoryId?.let { catId ->
            RemovableFilterPill("Category: ${categoryMap[catId]?.name ?: catId}") {
                onUpdateFilter { it.copy(selectedCategoryId = null, selectedSubcategoryId = null) }
            }
        }
        filter.selectedSubcategoryId?.let { subId ->
            RemovableFilterPill("Subcategory: ${categoryMap[subId]?.name ?: subId}") {
                onUpdateFilter { it.copy(selectedSubcategoryId = null) }
            }
        }
        filter.selectedCollectionId?.let { colId ->
            RemovableFilterPill("Collection: ${collectionMap[colId]?.name ?: colId}") {
                onUpdateFilter { it.copy(selectedCollectionId = null) }
            }
        }
        filter.selectedBrand?.let { brand ->
            RemovableFilterPill("Brand: $brand") {
                onUpdateFilter { it.copy(selectedBrand = null) }
            }
        }
        if (filter.minPriceNgn != null || filter.maxPriceNgn != null) {
            val minLabel = filter.minPriceNgn?.let { formatNgnAmount(it) } ?: "₦0"
            val maxLabel = filter.maxPriceNgn?.let { formatNgnAmount(it) } ?: "Any"
            RemovableFilterPill("Price: $minLabel – $maxLabel") {
                onUpdateFilter { it.copy(minPriceNgn = null, maxPriceNgn = null) }
            }
        }
        if (filter.inStockOnly) {
            RemovableFilterPill("In Stock") {
                onUpdateFilter { it.copy(inStockOnly = false) }
            }
        }
        if (filter.discountedOnly) {
            RemovableFilterPill("On Sale") {
                onUpdateFilter { it.copy(discountedOnly = false) }
            }
        }
        if (filter.newArrivalsOnly) {
            RemovableFilterPill("New Arrivals") {
                onUpdateFilter { it.copy(newArrivalsOnly = false) }
            }
        }
        filter.selectedSizes.forEach { size ->
            RemovableFilterPill("Size: $size") {
                onUpdateFilter { it.copy(selectedSizes = it.selectedSizes - size) }
            }
        }
        filter.selectedColors.forEach { color ->
            RemovableFilterPill("Shade: $color") {
                onUpdateFilter { it.copy(selectedColors = it.selectedColors - color) }
            }
        }
        if (filter.searchQuery.isNotBlank()) {
            RemovableFilterPill("Search: \"${filter.searchQuery.trim()}\"") {
                onUpdateFilter { it.copy(searchQuery = "") }
            }
        }
        TextButton(
            onClick = onClearAll,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("clear_all_filters_button"),
        ) {
            Text("Clear All", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RemovableFilterPill(
    label: String,
    onRemove: () -> Unit,
) {
    Surface(
        onClick = onRemove,
        modifier = Modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(20.dp),
        color = ZeecrownSoftLavender,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = ZeecrownDeepPurple)
            Icon(Icons.Outlined.Close, contentDescription = "Remove $label", modifier = Modifier.size(14.dp), tint = ZeecrownDeepPurple)
        }
    }
}

@Composable
fun DesktopFilterSidebar(
    filter: CatalogFilterState,
    facets: CatalogFacetOptions,
    collections: List<StoreCollection>,
    onUpdateFilter: ((CatalogFilterState) -> CatalogFilterState) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(272.dp)
            .testTag("desktop_filter_sidebar"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Refine Selection", style = MaterialTheme.typography.titleMedium)
                }
                if (filter.activeFilterCount() > 0) {
                    TextButton(onClick = onClearAll, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Text("Reset")
                    }
                }
            }
            FacetFilterControls(
                filter = filter,
                facets = facets,
                collections = collections,
                onChangeFilter = { newState -> onUpdateFilter { newState } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileFilterBottomSheet(
    filter: CatalogFilterState,
    facets: CatalogFacetOptions,
    collections: List<StoreCollection>,
    onApply: (CatalogFilterState) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(filter) { mutableStateOf(filter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ZeecrownWarmIvory,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Filter Boutique Pieces", style = MaterialTheme.typography.headlineSmall)
                TextButton(
                    onClick = {
                        onClearAll()
                        onDismiss()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("Clear All")
                }
            }

            FacetFilterControls(
                filter = draft,
                facets = facets,
                collections = collections,
                onChangeFilter = { draft = it },
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize(),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onApply(draft)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("apply_filters_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZeecrownRoyalPurple,
                        contentColor = ZeecrownPureWhite,
                    ),
                ) {
                    Text("Apply Filters")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacetFilterControls(
    filter: CatalogFilterState,
    facets: CatalogFacetOptions,
    collections: List<StoreCollection>,
    onChangeFilter: (CatalogFilterState) -> Unit,
) {
    var minPriceText by remember(filter.minPriceNgn) {
        mutableStateOf(filter.minPriceNgn?.toInt()?.toString().orEmpty())
    }
    var maxPriceText by remember(filter.maxPriceNgn) {
        mutableStateOf(filter.maxPriceNgn?.toInt()?.toString().orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Price Range (NGN ₦)", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minPriceText,
                onValueChange = { raw ->
                    minPriceText = raw.filter { it.isDigit() }
                    onChangeFilter(filter.copy(minPriceNgn = minPriceText.toDoubleOrNull()))
                },
                label = { Text("Min ₦") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = maxPriceText,
                onValueChange = { raw ->
                    maxPriceText = raw.filter { it.isDigit() }
                    onChangeFilter(filter.copy(maxPriceNgn = maxPriceText.toDoubleOrNull()))
                },
                label = { Text("Max ₦") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = filter.minPriceNgn == null && filter.maxPriceNgn == 40000.0,
                onClick = {
                    val active = filter.minPriceNgn == null && filter.maxPriceNgn == 40000.0
                    onChangeFilter(
                        if (active) filter.copy(minPriceNgn = null, maxPriceNgn = null)
                        else filter.copy(minPriceNgn = null, maxPriceNgn = 40000.0)
                    )
                },
                label = { Text("Under ₦40k") },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
            FilterChip(
                selected = filter.minPriceNgn == 40000.0 && filter.maxPriceNgn == 80000.0,
                onClick = {
                    val active = filter.minPriceNgn == 40000.0 && filter.maxPriceNgn == 80000.0
                    onChangeFilter(
                        if (active) filter.copy(minPriceNgn = null, maxPriceNgn = null)
                        else filter.copy(minPriceNgn = 40000.0, maxPriceNgn = 80000.0)
                    )
                },
                label = { Text("₦40k – ₦80k") },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
            FilterChip(
                selected = filter.minPriceNgn == 80000.0 && filter.maxPriceNgn == null,
                onClick = {
                    val active = filter.minPriceNgn == 80000.0 && filter.maxPriceNgn == null
                    onChangeFilter(
                        if (active) filter.copy(minPriceNgn = null, maxPriceNgn = null)
                        else filter.copy(minPriceNgn = 80000.0, maxPriceNgn = null)
                    )
                },
                label = { Text("Over ₦80k") },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        }

        HorizontalDivider(color = ZeecrownElegantSilver)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("In Stock Only", style = MaterialTheme.typography.titleSmall)
            Switch(
                checked = filter.inStockOnly,
                onCheckedChange = { onChangeFilter(filter.copy(inStockOnly = it)) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Discounted Pieces", style = MaterialTheme.typography.titleSmall)
            Switch(
                checked = filter.discountedOnly,
                onCheckedChange = { onChangeFilter(filter.copy(discountedOnly = it)) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("New Arrivals Only", style = MaterialTheme.typography.titleSmall)
            Switch(
                checked = filter.newArrivalsOnly,
                onCheckedChange = { onChangeFilter(filter.copy(newArrivalsOnly = it)) },
            )
        }

        if (collections.isNotEmpty()) {
            HorizontalDivider(color = ZeecrownElegantSilver)
            Text("Collection", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                collections.forEach { col ->
                    FilterChip(
                        selected = filter.selectedCollectionId == col.id,
                        onClick = {
                            onChangeFilter(
                                filter.copy(
                                    selectedCollectionId = if (filter.selectedCollectionId == col.id) null else col.id
                                )
                            )
                        },
                        label = { Text(col.name) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZeecrownSoftLavender),
                    )
                }
            }
        }

        if (facets.brands.isNotEmpty()) {
            HorizontalDivider(color = ZeecrownElegantSilver)
            Text("Brand / Atelier Line", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                facets.brands.forEach { brand ->
                    FilterChip(
                        selected = filter.selectedBrand == brand,
                        onClick = {
                            onChangeFilter(
                                filter.copy(selectedBrand = if (filter.selectedBrand == brand) null else brand)
                            )
                        },
                        label = { Text(brand) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZeecrownSoftLavender),
                    )
                }
            }
        }

        if (facets.sizes.isNotEmpty()) {
            HorizontalDivider(color = ZeecrownElegantSilver)
            Text("Size", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                facets.sizes.forEach { size ->
                    val selected = size in filter.selectedSizes
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onChangeFilter(
                                filter.copy(
                                    selectedSizes = if (selected) filter.selectedSizes - size else filter.selectedSizes + size
                                )
                            )
                        },
                        label = { Text(size) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZeecrownSoftLavender),
                    )
                }
            }
        }

        if (facets.colors.isNotEmpty()) {
            HorizontalDivider(color = ZeecrownElegantSilver)
            Text("Colour / Shade", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                facets.colors.forEach { color ->
                    val selected = color in filter.selectedColors
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onChangeFilter(
                                filter.copy(
                                    selectedColors = if (selected) filter.selectedColors - color else filter.selectedColors + color
                                )
                            )
                        },
                        label = { Text(color) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZeecrownSoftLavender),
                    )
                }
            }
        }
    }
}
