package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.Category
import com.example.data.model.DefaultSizeGuides
import com.example.data.model.Product
import com.example.data.model.SizeGuide
import com.example.data.model.SizeGuideCategoryType
import com.example.data.model.SizeGuideRow
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSizeGuidesTab(
    sizeGuidesState: UiState<List<SizeGuide>>,
    categories: List<Category>,
    products: List<Product>,
    isBusy: Boolean,
    onSaveSizeGuide: (guideId: String?, guide: SizeGuide, onDone: () -> Unit) -> Unit,
    onDeleteSizeGuide: (SizeGuide) -> Unit,
) {
    val guides = (sizeGuidesState as? UiState.Success)?.data.orEmpty()
    val categoryNameMap = remember(categories) { categories.associate { it.id to it.name } }
    val productNameMap = remember(products) { products.associate { it.id to it.title } }

    var showEditor by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var categoryType by remember { mutableStateOf(SizeGuideCategoryType.ABAYAS_BUBU) }
    var description by remember { mutableStateOf("") }
    var unitDefault by remember { mutableStateOf("CM") }
    var sortOrderText by remember { mutableStateOf("1") }
    var active by remember { mutableStateOf(true) }
    var stepsText by remember { mutableStateOf("") }
    var fitNotes by remember { mutableStateOf("") }
    var productSearch by remember { mutableStateOf("") }

    val columns = remember { mutableStateListOf("Size", "Bust (cm)", "Waist (cm)", "Length (cm)") }
    val rows = remember {
        mutableStateListOf(
            SizeGuideRow("S", listOf("90", "74", "136")),
            SizeGuideRow("M", listOf("98", "82", "138")),
            SizeGuideRow("L", listOf("106", "90", "140")),
        )
    }
    var assignedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var assignedProducts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var guidePendingDelete by remember { mutableStateOf<SizeGuide?>(null) }

    fun loadTemplate(template: SizeGuide, keepAsNew: Boolean = true) {
        if (keepAsNew) editingId = null
        name = if (keepAsNew && template.name.isNotBlank()) template.name else name
        categoryType = template.resolvedType
        description = template.description
        unitDefault = if (template.measurementUnitDefault.equals("IN", ignoreCase = true)) "IN" else "CM"
        sortOrderText = template.sortOrder.coerceAtLeast(1L).toString()
        active = template.active
        columns.clear()
        columns.addAll(template.columns.ifEmpty { listOf("Size", "Measurement (cm)") })
        rows.clear()
        rows.addAll(template.rows.ifEmpty { listOf(SizeGuideRow("Standard", listOf("—"))) })
        stepsText = template.howToMeasureSteps.joinToString("\n")
        fitNotes = template.fitNotes
        assignedCategories = template.assignedCategoryIds.toSet()
        assignedProducts = template.assignedProductIds.toSet()
        showEditor = true
    }

    fun openNewBlank() {
        val defaultTemplate = DefaultSizeGuides.starterById("sg_abayas_bubu") ?: SizeGuide()
        editingId = null
        name = ""
        loadTemplate(defaultTemplate, keepAsNew = true)
    }

    fun openEdit(guide: SizeGuide) {
        editingId = guide.id
        name = guide.name
        loadTemplate(guide, keepAsNew = false)
    }

    val filteredProducts = remember(products, productSearch) {
        val q = productSearch.trim().lowercase()
        val list = if (q.isBlank()) products else products.filter {
            it.title.lowercase().contains(q) || it.sku.lowercase().contains(q)
        }
        list.take(24)
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
                    Text("Fashion Size Guides (${guides.size})", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Configure footwear, Abaya, Bubu & clothing measurement charts assigned to categories or items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { if (showEditor) showEditor = false else openNewBlank() },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_toggle_size_guide_form"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) {
                    Icon(if (showEditor) Icons.Outlined.Close else Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showEditor) "Close" else "New Chart")
                }
            }
        }

        if (showEditor) {
            item {
                AdminSectionCard {
                    Text(
                        text = if (editingId == null) "Create Size Guide Chart" else "Edit Size Guide",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text("Load Quick Starter Template", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DefaultSizeGuides.starterGuides(), key = { it.first }) { (_, template) ->
                            OutlinedButton(
                                onClick = {
                                    name = template.name
                                    loadTemplate(template, keepAsNew = editingId == null)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Text(template.resolvedType.label)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(100) },
                        label = { Text("Size Guide Title *") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_size_guide_name_input"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(400) },
                        label = { Text("Chart Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )

                    Text("Garment / Footwear Category Type", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SizeGuideCategoryType.entries) { type ->
                            FilterChip(
                                selected = categoryType == type,
                                onClick = { categoryType = type },
                                label = { Text(type.label) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Unit", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = unitDefault == "CM",
                                    onClick = { unitDefault = "CM" },
                                    label = { Text("CM") },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                )
                                FilterChip(
                                    selected = unitDefault == "IN",
                                    onClick = { unitDefault = "IN" },
                                    label = { Text("Inches") },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = sortOrderText,
                            onValueChange = { sortOrderText = it.filter { c -> c.isDigit() } },
                            label = { Text("Sort Order") },
                            modifier = Modifier.width(120.dp),
                            singleLine = true,
                        )
                    }

                    HorizontalDivider(color = ZeecrownElegantSilver)

                    // Columns Editor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Measurement Columns (${columns.size}/8)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = {
                                if (columns.size < 8) {
                                    columns.add("Column ${columns.size + 1}")
                                    rows.indices.forEach { idx ->
                                        val r = rows[idx]
                                        val expanded = r.values.toMutableList().apply { add("—") }
                                        rows[idx] = r.copy(values = expanded)
                                    }
                                }
                            },
                            enabled = columns.size < 8,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) { Text("+ Column") }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        columns.forEachIndexed { colIdx, colLabel ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = colLabel,
                                    onValueChange = { columns[colIdx] = it.take(40) },
                                    label = { Text(if (colIdx == 0) "Size Header" else "Col ${colIdx + 1}") },
                                    modifier = Modifier.width(150.dp),
                                    singleLine = true,
                                )
                                if (colIdx >= 2 && columns.size > 2) {
                                    IconButton(
                                        onClick = {
                                            val valueIndex = colIdx - 1
                                            columns.removeAt(colIdx)
                                            rows.indices.forEach { rIdx ->
                                                val vals = rows[rIdx].values.toMutableList()
                                                if (valueIndex in vals.indices) vals.removeAt(valueIndex)
                                                rows[rIdx] = rows[rIdx].copy(values = vals)
                                            }
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Remove column")
                                    }
                                }
                            }
                        }
                    }

                    // Rows Editor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Size Chart Rows (${rows.size}/30)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = {
                                if (rows.size < 30) {
                                    val blankValues = List((columns.size - 1).coerceAtLeast(1)) { "—" }
                                    rows.add(SizeGuideRow("Size ${rows.size + 1}", blankValues))
                                }
                            },
                            enabled = rows.size < 30,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_add_size_row_btn"),
                        ) { Text("+ Add Size Row") }
                    }

                    val valueColumnCount = (columns.size - 1).coerceAtLeast(1)
                    rows.forEachIndexed { rowIdx, row ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = ZeecrownLightLilac,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = row.sizeLabel,
                                    onValueChange = { rows[rowIdx] = row.copy(sizeLabel = it.take(40)) },
                                    label = { Text(columns.firstOrNull().orEmpty().ifBlank { "Size" }) },
                                    modifier = Modifier.width(120.dp),
                                    singleLine = true,
                                )
                                for (vIdx in 0 until valueColumnCount) {
                                    val header = columns.getOrElse(vIdx + 1) { "Val ${vIdx + 1}" }
                                    val currentVal = row.values.getOrElse(vIdx) { "" }
                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = { newVal ->
                                            val updated = MutableList(valueColumnCount) { i ->
                                                row.values.getOrElse(i) { "" }
                                            }
                                            updated[vIdx] = newVal.take(40)
                                            rows[rowIdx] = row.copy(values = updated)
                                        },
                                        label = { Text(header) },
                                        modifier = Modifier.width(120.dp),
                                        singleLine = true,
                                    )
                                }
                                if (rows.size > 1) {
                                    IconButton(
                                        onClick = { rows.removeAt(rowIdx) },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove row ${row.sizeLabel}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = stepsText,
                        onValueChange = { stepsText = it.take(2400) },
                        label = { Text("How to Measure Instructions (one step per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    OutlinedTextField(
                        value = fitNotes,
                        onValueChange = { fitNotes = it.take(500) },
                        label = { Text("Atelier Fit Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )

                    HorizontalDivider(color = ZeecrownElegantSilver)

                    Text("Assign to Categories", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.forEach { cat ->
                            val selected = cat.id in assignedCategories
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    assignedCategories = if (selected) assignedCategories - cat.id else assignedCategories + cat.id
                                },
                                label = { Text(cat.name) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                        }
                    }

                    Text("Assign to Specific Products (Optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = productSearch,
                        onValueChange = { productSearch = it },
                        label = { Text("Filter products to assign") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredProducts, key = { it.id }) { prod ->
                            val selected = prod.id in assignedProducts
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    assignedProducts = if (selected) assignedProducts - prod.id else assignedProducts + prod.id
                                },
                                label = { Text(prod.title.take(28)) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Active & Visible on Storefront")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }

                    Button(
                        onClick = {
                            val cleanCols = columns.map { it.trim() }.filter { it.isNotBlank() }
                            val cleanRows = rows.map {
                                val normalizedVals = List((cleanCols.size - 1).coerceAtLeast(1)) { idx ->
                                    it.values.getOrElse(idx) { "" }.trim()
                                }
                                SizeGuideRow(it.sizeLabel.trim(), normalizedVals)
                            }.filter { it.sizeLabel.isNotBlank() }

                            val guide = SizeGuide(
                                id = editingId.orEmpty(),
                                name = name.trim(),
                                categoryType = categoryType.name,
                                description = description.trim(),
                                measurementUnitDefault = unitDefault,
                                columns = cleanCols,
                                rows = cleanRows,
                                howToMeasureSteps = stepsText.lines().map { it.trim() }.filter { it.isNotBlank() },
                                fitNotes = fitNotes.trim(),
                                assignedCategoryIds = assignedCategories.toList(),
                                assignedProductIds = assignedProducts.toList(),
                                sortOrder = sortOrderText.toLongOrNull() ?: 1L,
                                active = active,
                            )
                            onSaveSizeGuide(editingId, guide) {
                                showEditor = false
                            }
                        },
                        enabled = !isBusy && name.trim().length >= 2 && columns.count { it.isNotBlank() } >= 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("admin_save_size_guide_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                    ) {
                        Text(if (editingId == null) "Publish Size Guide" else "Save Size Guide Changes")
                    }
                }
            }
        }

        when (sizeGuidesState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading size guides...") }
            is UiState.Error -> item { BoutiqueErrorBanner(sizeGuidesState.message) }
            is UiState.Success -> {
                if (guides.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Size Guides Configured",
                            subtitle = "Create a size chart above or tap Seed in the admin top bar to load footwear, Abaya & Bubu charts.",
                        )
                    }
                } else {
                    items(guides, key = { it.id }) { guide ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_size_guide_row_${guide.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(shape = RoundedCornerShape(8.dp), color = ZeecrownLightLilac) {
                                            Icon(Icons.Outlined.Straighten, contentDescription = null, modifier = Modifier.padding(6.dp).size(18.dp), tint = ZeecrownDeepPurple)
                                        }
                                        Column {
                                            Text(guide.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = "${guide.resolvedType.label} · ${guide.rows.size} sizes · ${guide.measurementUnitDefault}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    AdminStatusPill(
                                        label = if (guide.active) "ACTIVE" else "HIDDEN",
                                        tone = if (guide.active) PillTone.SUCCESS else PillTone.ROSE,
                                    )
                                }

                                val assignedCatLabels = guide.assignedCategoryIds.mapNotNull { categoryNameMap[it] }
                                val assignedProdLabels = guide.assignedProductIds.mapNotNull { productNameMap[it] }
                                if (assignedCatLabels.isNotEmpty() || assignedProdLabels.isNotEmpty()) {
                                    Text(
                                        text = buildString {
                                            if (assignedCatLabels.isNotEmpty()) {
                                                append("Categories: ").append(assignedCatLabels.joinToString(", "))
                                            }
                                            if (assignedProdLabels.isNotEmpty()) {
                                                if (isNotEmpty()) append(" · ")
                                                append("Products: ").append(assignedProdLabels.take(3).joinToString(", "))
                                                if (assignedProdLabels.size > 3) append(" +${assignedProdLabels.size - 3} more")
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ZeecrownDeepPurple,
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = { openEdit(guide) },
                                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit Chart")
                                    }
                                    IconButton(
                                        onClick = {
                                            editingId = null
                                            name = "${guide.name} (Copy)"
                                            loadTemplate(guide, keepAsNew = true)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate ${guide.name}")
                                    }
                                    IconButton(
                                        onClick = { guidePendingDelete = guide },
                                        enabled = !isBusy,
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${guide.name}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    guidePendingDelete?.let { guide ->
        AlertDialog(
            onDismissRequest = { guidePendingDelete = null },
            title = { Text("Delete '${guide.name}'?") },
            text = { Text("Removing this chart will fall back to automatic category/default sizing until a new guide is assigned.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSizeGuide(guide)
                        guidePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { guidePendingDelete = null }) { Text("Cancel") } },
        )
    }
}
