package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.ui.components.BoutiqueEmptyView
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.BoutiqueLoadingView
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

private val REVIEW_FILTERS = listOf("ALL", "APPROVED", "PENDING", "FLAGGED", "REJECTED")

@Composable
fun AdminReviewsTab(
    reviewsState: UiState<List<Review>>,
    products: List<Product>,
    isBusy: Boolean,
    onModerateReview: (reviewId: String, visible: Boolean, status: String, reason: String) -> Unit,
    onDeleteReview: (reviewId: String) -> Unit,
) {
    val reviews = (reviewsState as? UiState.Success)?.data.orEmpty()
    val productTitles = remember(products) { products.associate { it.id to it.title } }
    var statusFilter by remember { mutableStateOf("ALL") }
    var flaggingReview by remember { mutableStateOf<Review?>(null) }

    val filtered = remember(reviews, statusFilter) {
        if (statusFilter == "ALL") reviews
        else reviews.filter { it.moderationStatus.equals(statusFilter, ignoreCase = true) }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reviews Moderation (${reviews.size})", style = MaterialTheme.typography.titleLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(REVIEW_FILTERS) { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(st) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }
            }
        }

        when (reviewsState) {
            is UiState.Loading -> item { BoutiqueLoadingView("Loading customer reviews...") }
            is UiState.Error -> item { BoutiqueErrorBanner(reviewsState.message) }
            is UiState.Success -> {
                if (filtered.isEmpty()) {
                    item {
                        BoutiqueEmptyView(
                            title = "No Reviews in '$statusFilter'",
                            subtitle = "Verified customer product reviews will queue here for moderation.",
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { rev ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("admin_review_row_${rev.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, ZeecrownElegantSilver),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${rev.authorName} · ${rev.rating}/5 ★",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    AdminStatusPill(
                                        label = "${rev.moderationStatus} (${if (rev.visible) "LIVE" else "HIDDEN"})",
                                        tone = when (rev.moderationStatus) {
                                            "APPROVED" -> PillTone.SUCCESS
                                            "FLAGGED", "REJECTED" -> PillTone.ERROR
                                            else -> PillTone.GOLD
                                        },
                                    )
                                }
                                Text(
                                    text = "Product: ${productTitles[rev.productId] ?: rev.productId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZeecrownGoldInk,
                                )
                                Text("\"${rev.comment}\"", style = MaterialTheme.typography.bodyMedium)
                                if (rev.flaggedReason.isNotBlank()) {
                                    Text("Flag Note: ${rev.flaggedReason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onModerateReview(rev.id, true, "APPROVED", "") },
                                        enabled = !isBusy,
                                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                                    ) { Text("Approve") }
                                    OutlinedButton(
                                        onClick = { onModerateReview(rev.id, false, "REJECTED", "") },
                                        enabled = !isBusy,
                                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                                    ) { Text("Hide") }
                                    OutlinedButton(
                                        onClick = { flaggingReview = rev },
                                        enabled = !isBusy,
                                        modifier = Modifier.weight(0.9f).minimumInteractiveComponentSize(),
                                    ) { Text("Flag") }
                                    TextButton(
                                        onClick = { onDeleteReview(rev.id) },
                                        enabled = !isBusy,
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    flaggingReview?.let { rev ->
        var reason by remember(rev.id) { mutableStateOf(rev.flaggedReason.ifBlank { "Needs verification / spam check" }) }
        AlertDialog(
            onDismissRequest = { flaggingReview = null },
            title = { Text("Flag Review by ${rev.authorName}") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Moderation Reason") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onModerateReview(rev.id, false, "FLAGGED", reason)
                        flaggingReview = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Flag & Hide") }
            },
            dismissButton = { TextButton(onClick = { flaggingReview = null }) { Text("Cancel") } },
        )
    }
}
