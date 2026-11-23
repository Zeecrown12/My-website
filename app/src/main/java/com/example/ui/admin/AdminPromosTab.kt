package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.Coupon
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

private val DISCOUNT_TYPES = listOf(
    "PERCENTAGE" to "Percentage (%)",
    "FIXED_NGN" to "Fixed (NGN)",
    "FREE_DELIVERY" to "Free Delivery",
)

@Composable
fun AdminPromosTab(
    couponsState: UiState<List<Coupon>>,
    isBusy: Boolean,
    onSaveCoupon: (couponId: String?, coupon: Coupon) -> Unit,
    onDeleteCoupon: (Coupon) -> Unit,
) {
    val coupons = (couponsState as? UiState.Success)?.data.orEmpty()

    var editingId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf("PERCENTAGE") }
    var discountValueText by remember { mutableStateOf("10") }
    var minOrderText by remember { mutableStateOf("0") }
    var maxDiscountText by remember { mutableStateOf("") }
    var usageLimitText by remember { mutableStateOf("") }
    var perCustomerLimitText by remember { mutableStateOf("") }
    var expiresDaysText by remember { mutableStateOf("30") }
    var restrictedCategories by remember { mutableStateOf("") }
    var restrictedProducts by remember { mutableStateOf("") }
    var restrictedEmails by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    fun loadForEdit(c: Coupon) {
        editingId = c.id
        code = c.code
        description = c.description
        discountType = c.discountType
        discountValueText = c.discountValue.toLong().toString()
        minOrderText = c.minOrderNgn.toLong().toString()
        maxDiscountText = c.maxDiscountNgn?.toLong()?.toString().orEmpty()
        usageLimitText = c.usageLimit?.toString().orEmpty()
        perCustomerLimitText = c.perCustomerLimit?.toString().orEmpty()
        expiresDaysText = c.expiresAtMillis?.let { exp ->
            ((exp - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(1L).toString()
        }.orEmpty()
        restrictedCategories = c.applicableCategoryIds.joinToString(", ")
        restrictedProducts = c.applicableProductIds.joinToString(", ")
        restrictedEmails = c.restrictedEmails.joinToString(", ")
        active = c.active
    }

    fun resetForm() {
        editingId = null
        code = ""
        description = ""
        discountType = "PERCENTAGE"
        discountValueText = "10"
        minOrderText = "0"
        maxDiscountText = ""
        usageLimitText = ""
        perCustomerLimitText = ""
        expiresDaysText = "30"
        restrictedCategories = ""
        restrictedProducts = ""
        restrictedEmails = ""
        active = true
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
                    Text(if (editingId == null) "Create Promo Code" else "Edit Promo Code", style = MaterialTheme.typography.titleLarge)
                    if (editingId != null) {
                        TextButton(onClick = ::resetForm) { Text("New Code") }
                    }
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Promo Code * (e.g. ZEECROWN15)") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_coupon_code_input"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Offer Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text("Discount Type", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DISCOUNT_TYPES) { (key, label) ->
                        FilterChip(
                            selected = discountType == key,
                            onClick = { discountType = key },
                            label = { Text(label) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = discountValueText,
                        onValueChange = { discountValueText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (discountType == "PERCENTAGE") "% Value" else "Amount NGN") },
                        enabled = discountType != "FREE_DELIVERY",
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = minOrderText,
                        onValueChange = { minOrderText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Min Order NGN") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = maxDiscountText,
                        onValueChange = { maxDiscountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Max Cap NGN") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = usageLimitText,
                        onValueChange = { usageLimitText = it.filter { c -> c.isDigit() } },
                        label = { Text("Total Limit") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = perCustomerLimitText,
                        onValueChange = { perCustomerLimitText = it.filter { c -> c.isDigit() } },
                        label = { Text("Per-User") },
                        modifier = Modifier.weight(0.9f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = expiresDaysText,
                        onValueChange = { expiresDaysText = it.filter { c -> c.isDigit() } },
                        label = { Text("Days") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = restrictedCategories,
                    onValueChange = { restrictedCategories = it },
                    label = { Text("Applicable Category IDs (optional comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = restrictedProducts,
                    onValueChange = { restrictedProducts = it },
                    label = { Text("Applicable Product IDs (optional comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = restrictedEmails,
                    onValueChange = { restrictedEmails = it },
                    label = { Text("Customer Email Allowlist (optional comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Promo Code Active")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val expDays = expiresDaysText.toLongOrNull()
                        val coupon = Coupon(
                            code = code.trim().uppercase(),
                            description = description.trim(),
                            discountType = discountType,
                            discountValue = if (discountType == "FREE_DELIVERY") 0.0 else (discountValueText.toDoubleOrNull() ?: 0.0),
                            minOrderNgn = minOrderText.toDoubleOrNull() ?: 0.0,
                            maxDiscountNgn = maxDiscountText.toDoubleOrNull(),
                            usageLimit = usageLimitText.toLongOrNull(),
                            perCustomerLimit = perCustomerLimitText.toLongOrNull(),
                            usageCount = coupons.firstOrNull { it.id == editingId }?.usageCount ?: 0L,
                            startsAtMillis = now,
                            expiresAtMillis = expDays?.takeIf { it > 0 }?.let { now + it * 86_400_000L },
                            applicableCategoryIds = restrictedCategories.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            applicableProductIds = restrictedProducts.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            restrictedEmails = restrictedEmails.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() },
                            active = active,
                        )
                        onSaveCoupon(editingId, coupon)
                        resetForm()
                    },
                    enabled = !isBusy && code.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("admin_save_coupon_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) {
                    Text(if (editingId == null) "Publish Promo Code" else "Save Promo Updates")
                }
            }
        }

        item { Text("Promo Codes (${coupons.size})", style = MaterialTheme.typography.titleLarge) }

        when (couponsState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading promo codes...") }
            is UiState.Error -> item { BoutiqueErrorBanner(couponsState.message) }
            is UiState.Success -> {
                if (coupons.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Promo Codes Configured",
                            subtitle = "Create percentage discounts, fixed Naira offers, or free delivery codes above.",
                        )
                    }
                } else {
                    items(coupons, key = { it.id }) { coupon ->
                        val validNow = coupon.isCurrentlyValid()
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_coupon_row_${coupon.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(coupon.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    val valueStr = when (coupon.discountType) {
                                        "PERCENTAGE" -> "${coupon.discountValue.toInt()}% Off"
                                        "FREE_DELIVERY" -> "Free Delivery"
                                        else -> "${formatNgnAmount(coupon.discountValue)} Off"
                                    }
                                    Text(
                                        "$valueStr · Min ${formatNgnAmount(coupon.minOrderNgn)} · Used ${coupon.usageCount}${coupon.usageLimit?.let { "/$it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        "Expires: ${formatAdminDateShort(coupon.expiresAtMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AdminStatusPill(
                                        label = if (validNow) "ACTIVE" else if (!coupon.active) "DISABLED" else "EXPIRED/CAPPED",
                                        tone = if (validNow) PillTone.SUCCESS else PillTone.ROSE,
                                    )
                                }
                                Row {
                                    IconButton(onClick = { loadForEdit(coupon) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit ${coupon.code}", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeleteCoupon(coupon) }, enabled = !isBusy, modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${coupon.code}", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
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
