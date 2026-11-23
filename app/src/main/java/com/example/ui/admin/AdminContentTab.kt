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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.firebase.UiState
import com.example.data.model.HomepageContent
import com.example.data.model.SeoLengthStatus
import com.example.data.model.SeoUtils
import com.example.data.model.StoreCollection
import com.example.data.model.StoreSettings
import com.example.data.model.Testimonial
import com.example.ui.components.ZeecrownProductImage
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

private val BANNER_SECTION_TYPES = listOf("HERO", "FEATURED_EDIT", "PROMO")

@Composable
fun AdminContentTab(
    homepageContentState: UiState<List<HomepageContent>>,
    collectionsState: UiState<List<StoreCollection>>,
    testimonialsState: UiState<List<Testimonial>>,
    storeSettingsState: UiState<StoreSettings>,
    isBusy: Boolean,
    onSaveHomepageContent: (sectionId: String, content: HomepageContent, localUri: Uri?) -> Unit,
    onSaveAnnouncements: (StoreSettings) -> Unit,
    onSaveCollection: (collectionId: String?, col: StoreCollection) -> Unit,
    onSaveTestimonial: (testimonialId: String?, item: Testimonial) -> Unit,
    onDeleteTestimonial: (id: String) -> Unit,
) {
    val banners = (homepageContentState as? UiState.Success)?.data.orEmpty()
    val collections = (collectionsState as? UiState.Success)?.data.orEmpty()
    val testimonials = (testimonialsState as? UiState.Success)?.data.orEmpty()
    val settings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()

    // Announcement bar
    var announcementsText by remember { mutableStateOf("") }
    LaunchedEffect(settings) {
        announcementsText = settings.announcementMessages.joinToString("\n")
    }

    // Banner Editor
    var bannerId by remember { mutableStateOf("hero_main") }
    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }
    var bannerCta by remember { mutableStateOf("SHOP COLLECTION") }
    var bannerSecondaryCta by remember { mutableStateOf("NEW ARRIVALS") }
    var bannerType by remember { mutableStateOf("HERO") }
    var bannerSlug by remember { mutableStateOf("") }
    var bannerImageKey by remember { mutableStateOf("img_hero_editorial") }
    var bannerSortOrder by remember { mutableStateOf("1") }
    var bannerActive by remember { mutableStateOf(true) }
    var pickedBannerUri by remember { mutableStateOf<Uri?>(null) }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pickedBannerUri = uri }

    LaunchedEffect(banners) {
        if (bannerTitle.isBlank()) {
            banners.firstOrNull { it.id == bannerId || it.sectionType == "HERO" }?.let { h ->
                bannerId = h.id.ifBlank { "hero_main" }
                bannerTitle = h.title
                bannerSubtitle = h.subtitle
                bannerCta = h.ctaLabel
                bannerSecondaryCta = h.secondaryCtaLabel
                bannerType = h.sectionType
                bannerSlug = h.targetCategorySlug
                bannerImageKey = h.imageKey
                bannerSortOrder = h.sortOrder.toString()
                bannerActive = h.active
            }
        }
    }

    // Collection Editor
    var editingCollectionId by remember { mutableStateOf<String?>(null) }
    var colName by remember { mutableStateOf("") }
    var colTagline by remember { mutableStateOf("") }
    var colSlug by remember { mutableStateOf("") }
    var colSeoTitle by remember { mutableStateOf("") }
    var colSeoDescription by remember { mutableStateOf("") }
    var colSocialImageUrl by remember { mutableStateOf("") }
    var colOrderText by remember { mutableStateOf("1") }
    var colActive by remember { mutableStateOf(true) }

    // Testimonial Editor
    var editingTestimonialId by remember { mutableStateOf<String?>(null) }
    var testName by remember { mutableStateOf("") }
    var testLocation by remember { mutableStateOf("") }
    var testRatingText by remember { mutableStateOf("5") }
    var testQuote by remember { mutableStateOf("") }
    var testProduct by remember { mutableStateOf("") }
    var testActive by remember { mutableStateOf(true) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Announcement Bar Manager
        item {
            AdminSectionCard {
                Text("Top Announcement Bar Ticker", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = announcementsText,
                    onValueChange = { announcementsText = it },
                    label = { Text("Rotating Messages (one per line, max 8)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Button(
                    onClick = {
                        val lines = announcementsText.lines().map { it.trim() }.filter { it.isNotBlank() }.take(8)
                        onSaveAnnouncements(settings.copy(announcementMessages = lines))
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Update Announcement Ticker") }
            }
        }

        // Homepage Hero / Edit / Promo Banners
        item {
            AdminSectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Homepage Banners & Campaigns", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        bannerId = "promo_${System.currentTimeMillis() % 10000}"
                        bannerTitle = ""
                        bannerSubtitle = ""
                        bannerCta = "EXPLORE NOW"
                        bannerType = "PROMO"
                        pickedBannerUri = null
                    }) { Text("+ New Banner") }
                }

                if (banners.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(banners, key = { it.id }) { b ->
                            FilterChip(
                                selected = bannerId == b.id,
                                onClick = {
                                    bannerId = b.id
                                    bannerTitle = b.title
                                    bannerSubtitle = b.subtitle
                                    bannerCta = b.ctaLabel
                                    bannerSecondaryCta = b.secondaryCtaLabel
                                    bannerType = b.sectionType
                                    bannerSlug = b.targetCategorySlug
                                    bannerImageKey = b.imageKey
                                    bannerSortOrder = b.sortOrder.toString()
                                    bannerActive = b.active
                                    pickedBannerUri = null
                                },
                                label = { Text("${b.sectionType}: ${b.title.take(18)}") },
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BANNER_SECTION_TYPES) { t ->
                        FilterChip(selected = bannerType == t, onClick = { bannerType = t }, label = { Text(t) })
                    }
                }

                OutlinedTextField(value = bannerId, onValueChange = { bannerId = it }, label = { Text("Section Document ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Headline *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = bannerSubtitle, onValueChange = { bannerSubtitle = it }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = bannerCta, onValueChange = { bannerCta = it }, label = { Text("Primary CTA") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = bannerSecondaryCta, onValueChange = { bannerSecondaryCta = it }, label = { Text("Secondary CTA") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = bannerSlug, onValueChange = { bannerSlug = it }, label = { Text("Target Category Slug") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = bannerSortOrder, onValueChange = { bannerSortOrder = it.filter { c -> c.isDigit() } }, label = { Text("Order") }, modifier = Modifier.weight(0.6f), singleLine = true)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pickedBannerUri != null) {
                        AsyncImage(model = pickedBannerUri, contentDescription = "Banner preview", modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    } else {
                        ZeecrownProductImage(imageUrl = if (bannerImageKey.startsWith("http")) bannerImageKey else "drawable://$bannerImageKey", contentDescription = "Banner", modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)))
                    }
                    OutlinedButton(
                        onClick = { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Upload Banner Photo")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Banner Visible")
                    Switch(checked = bannerActive, onCheckedChange = { bannerActive = it })
                }
                Button(
                    onClick = {
                        onSaveHomepageContent(
                            bannerId,
                            HomepageContent(
                                title = bannerTitle.trim(),
                                subtitle = bannerSubtitle.trim(),
                                ctaLabel = bannerCta.trim(),
                                secondaryCtaLabel = bannerSecondaryCta.trim(),
                                sectionType = bannerType,
                                targetCategorySlug = bannerSlug.trim(),
                                imageKey = bannerImageKey.trim(),
                                sortOrder = bannerSortOrder.toLongOrNull() ?: 1L,
                                active = bannerActive,
                            ),
                            pickedBannerUri,
                        )
                        pickedBannerUri = null
                    },
                    enabled = !isBusy && bannerTitle.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Publish Homepage Section") }
            }
        }

        // Collections Manager
        item {
            AdminSectionCard {
                Text("Curated Collections (${collections.size})", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = colName, onValueChange = { colName = it }, label = { Text("Collection Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = colTagline, onValueChange = { colTagline = it }, label = { Text("Editorial Tagline") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                AdminSectionCard(containerColor = ZeecrownLightLilac) {
                    val colTitleFb = SeoUtils.evaluateTitleLength(colSeoTitle)
                    val colDescFb = SeoUtils.evaluateDescriptionLength(colSeoDescription)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Collection SEO", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = {
                                val generated = SeoUtils.generateCollectionSeoDefaults(colName, colTagline, colSlug)
                                if (colSlug.isBlank()) colSlug = generated.slug
                                colSeoTitle = generated.seoTitle
                                colSeoDescription = generated.seoDescription
                            },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) { Text("Generate SEO Defaults") }
                    }
                    OutlinedTextField(
                        value = colSlug,
                        onValueChange = { colSlug = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '-' } },
                        label = { Text("Collection URL Slug") },
                        supportingText = { Text("Public URL: /collection/${SeoUtils.sanitizeSlug(colSlug.ifBlank { colName }, "collection")}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = colSeoTitle,
                        onValueChange = { colSeoTitle = it.take(120) },
                        label = { Text("SEO Title") },
                        supportingText = {
                            Text(
                                text = colTitleFb.guidance,
                                color = if (colTitleFb.status == SeoLengthStatus.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = colSeoDescription,
                        onValueChange = { colSeoDescription = it.take(300) },
                        label = { Text("SEO Meta Description") },
                        supportingText = {
                            Text(
                                text = colDescFb.guidance,
                                color = if (colDescFb.status == SeoLengthStatus.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = colSocialImageUrl,
                        onValueChange = { colSocialImageUrl = it.take(600) },
                        label = { Text("Social / Open Graph Image URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = colOrderText, onValueChange = { colOrderText = it.filter { c -> c.isDigit() } }, label = { Text("Order") }, modifier = Modifier.weight(1f), singleLine = true)
                    Switch(checked = colActive, onCheckedChange = { colActive = it })
                }
                Button(
                    onClick = {
                        onSaveCollection(
                            editingCollectionId,
                            StoreCollection(
                                name = colName,
                                slug = colSlug,
                                tagline = colTagline,
                                bannerKey = "img_zeecrown_edit_campaign",
                                seoTitle = colSeoTitle,
                                seoDescription = colSeoDescription,
                                socialImageUrl = colSocialImageUrl,
                                sortOrder = colOrderText.toLongOrNull() ?: 1L,
                                active = colActive,
                            ),
                        )
                        editingCollectionId = null; colName = ""; colTagline = ""; colSlug = ""; colSeoTitle = ""; colSeoDescription = ""; colSocialImageUrl = ""
                    },
                    enabled = !isBusy && colName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text(if (editingCollectionId == null) "Save Collection" else "Update Collection") }

                collections.forEach { c ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(c.tagline, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            editingCollectionId = c.id
                            colName = c.name
                            colTagline = c.tagline
                            colSlug = c.slug
                            colSeoTitle = c.seoTitle
                            colSeoDescription = c.seoDescription
                            colSocialImageUrl = c.socialImageUrl
                            colOrderText = c.sortOrder.toString()
                            colActive = c.active
                        }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit ${c.name}", modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }

        // Testimonials Manager
        item {
            AdminSectionCard {
                Text("Featured Client Testimonials (${testimonials.size})", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = testName, onValueChange = { testName = it }, label = { Text("Client Name *") }, modifier = Modifier.weight(1.2f), singleLine = true)
                    OutlinedTextField(value = testLocation, onValueChange = { testLocation = it }, label = { Text("City") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = testQuote, onValueChange = { testQuote = it }, label = { Text("Testimonial Quote *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = testProduct, onValueChange = { testProduct = it }, label = { Text("Product Mentioned") }, modifier = Modifier.weight(1.3f), singleLine = true)
                    OutlinedTextField(value = testRatingText, onValueChange = { testRatingText = it.filter { c -> c in '1'..'5' }.take(1) }, label = { Text("Stars") }, modifier = Modifier.weight(0.5f), singleLine = true)
                    Switch(checked = testActive, onCheckedChange = { testActive = it })
                }
                Button(
                    onClick = {
                        onSaveTestimonial(
                            editingTestimonialId,
                            Testimonial(customerName = testName, location = testLocation, rating = testRatingText.toLongOrNull() ?: 5L, quote = testQuote, productTitle = testProduct, active = testActive),
                        )
                        editingTestimonialId = null; testName = ""; testLocation = ""; testQuote = ""; testProduct = ""
                    },
                    enabled = !isBusy && testName.isNotBlank() && testQuote.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text(if (editingTestimonialId == null) "Publish Testimonial" else "Update Testimonial") }

                testimonials.forEach { t ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, ZeecrownElegantSilver)) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${t.customerName} (${t.location}) · ${t.rating}★", style = MaterialTheme.typography.titleSmall)
                                Text("\"${t.quote}\"", style = MaterialTheme.typography.bodySmall)
                            }
                            Row {
                                IconButton(onClick = {
                                    editingTestimonialId = t.id; testName = t.customerName; testLocation = t.location; testQuote = t.quote; testProduct = t.productTitle; testRatingText = t.rating.toString(); testActive = t.active
                                }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit testimonial", modifier = Modifier.size(18.dp)) }
                                IconButton(onClick = { onDeleteTestimonial(t.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete testimonial", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
