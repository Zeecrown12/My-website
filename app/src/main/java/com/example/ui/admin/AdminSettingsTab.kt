package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.firebase.UiState
import com.example.data.model.SeoLengthStatus
import com.example.data.model.SeoUtils
import com.example.data.model.StoreSettings
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple

@Composable
fun AdminSettingsTab(
    storeSettingsState: UiState<StoreSettings>,
    isBusy: Boolean,
    onSaveSettings: (StoreSettings, Uri?) -> Unit,
) {
    val current = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()

    var storeName by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var newLogoUri by remember { mutableStateOf<Uri?>(null) }

    var supportEmail by remember { mutableStateOf("") }
    var supportPhone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var supportHours by remember { mutableStateOf("") }
    var defaultCurrency by remember { mutableStateOf("NGN") }
    var timeZone by remember { mutableStateOf("Africa/Lagos") }
    var orderNumberPrefix by remember { mutableStateOf("ZC") }
    var taxEnabled by remember { mutableStateOf(false) }
    var taxRateText by remember { mutableStateOf("0") }

    var bankName by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }

    var paystackEnabled by remember { mutableStateOf(true) }
    var flutterwaveEnabled by remember { mutableStateOf(true) }
    var bankTransferEnabled by remember { mutableStateOf(true) }
    var codEnabled by remember { mutableStateOf(false) }
    var guestCheckoutEnabled by remember { mutableStateOf(false) }

    var paystackPublicKey by remember { mutableStateOf("") }
    var flutterwavePublicKey by remember { mutableStateOf("") }

    var aboutText by remember { mutableStateOf("") }
    var shippingPolicy by remember { mutableStateOf("") }
    var returnPolicy by remember { mutableStateOf("") }
    var privacyPolicy by remember { mutableStateOf("") }
    var termsPolicy by remember { mutableStateOf("") }

    var instagramHandle by remember { mutableStateOf("") }
    var instagramUrl by remember { mutableStateOf("") }
    var tiktokHandle by remember { mutableStateOf("") }
    var tiktokUrl by remember { mutableStateOf("") }

    var maintenanceMode by remember { mutableStateOf(false) }
    var maintenanceMessage by remember { mutableStateOf("") }

    var emailNotificationsEnabled by remember { mutableStateOf(true) }
    var smsNotificationsEnabled by remember { mutableStateOf(false) }
    var whatsappNotificationsEnabled by remember { mutableStateOf(false) }
    var requireReviewApproval by remember { mutableStateOf(false) }
    var emailSenderName by remember { mutableStateOf("ZEECROWN STORE") }
    var emailReplyTo by remember { mutableStateOf("") }

    var publicSiteBaseUrl by remember { mutableStateOf("https://zeecrownstore.com") }
    var defaultSeoTitle by remember { mutableStateOf("") }
    var defaultSeoDescription by remember { mutableStateOf("") }
    var defaultSocialImageUrl by remember { mutableStateOf("") }
    var facebookUrl by remember { mutableStateOf("") }
    var pinterestUrl by remember { mutableStateOf("") }
    var youtubeUrl by remember { mutableStateOf("") }
    var twitterHandle by remember { mutableStateOf("") }
    var ga4MeasurementId by remember { mutableStateOf("") }
    var analyticsConsentRequired by remember { mutableStateOf(true) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) newLogoUri = uri }

    LaunchedEffect(current) {
        storeName = current.storeName
        tagline = current.tagline
        logoUrl = current.logoUrl
        supportEmail = current.supportEmail
        supportPhone = current.supportPhone
        whatsapp = current.whatsappNumber
        businessAddress = current.businessAddress
        supportHours = current.supportHours
        defaultCurrency = current.defaultCurrency
        timeZone = current.timeZone
        orderNumberPrefix = current.orderNumberPrefix
        taxEnabled = current.taxEnabled
        taxRateText = if (current.taxRatePercent > 0.0) current.taxRatePercent.toString() else "0"
        bankName = current.bankName
        accountName = current.accountName
        accountNumber = current.accountNumber
        paystackEnabled = current.paystackEnabled
        flutterwaveEnabled = current.flutterwaveEnabled
        bankTransferEnabled = current.bankTransferEnabled
        codEnabled = current.codEnabled
        guestCheckoutEnabled = current.guestCheckoutEnabled
        paystackPublicKey = current.paystackPublicKey
        flutterwavePublicKey = current.flutterwavePublicKey
        aboutText = current.aboutText
        shippingPolicy = current.shippingPolicy
        returnPolicy = current.returnPolicy
        privacyPolicy = current.privacyPolicy
        termsPolicy = current.termsPolicy
        instagramHandle = current.instagramHandle
        instagramUrl = current.instagramUrl
        tiktokHandle = current.tiktokHandle
        tiktokUrl = current.tiktokUrl
        maintenanceMode = current.maintenanceMode
        maintenanceMessage = current.maintenanceMessage
        emailNotificationsEnabled = current.emailNotificationsEnabled
        smsNotificationsEnabled = current.smsNotificationsEnabled
        whatsappNotificationsEnabled = current.whatsappNotificationsEnabled
        requireReviewApproval = current.requireReviewApproval
        emailSenderName = current.emailSenderName
        emailReplyTo = current.emailReplyTo
        publicSiteBaseUrl = current.publicSiteBaseUrl
        defaultSeoTitle = current.defaultSeoTitle
        defaultSeoDescription = current.defaultSeoDescription
        defaultSocialImageUrl = current.defaultSocialImageUrl
        facebookUrl = current.facebookUrl
        pinterestUrl = current.pinterestUrl
        youtubeUrl = current.youtubeUrl
        twitterHandle = current.twitterHandle
        ga4MeasurementId = current.ga4MeasurementId
        analyticsConsentRequired = current.analyticsConsentRequired
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Brand Profile & Logo
        item {
            AdminSectionCard {
                Text("Brand Identity & Official Logo", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val previewModel: Any? = newLogoUri ?: logoUrl.takeIf { it.isNotBlank() }
                    if (previewModel != null) {
                        AsyncImage(
                            model = previewModel,
                            contentDescription = "ZEECROWN Store Logo",
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.zeecrown_logo),
                            contentDescription = "ZEECROWN Default Crest Logo",
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Replace Official Logo")
                        }
                        Text("Renders with preserved aspect ratio.", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Store Name *") }, modifier = Modifier.fillMaxWidth().testTag("admin_settings_store_name"), singleLine = true)
                OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Brand Tagline") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Default Currency:", style = MaterialTheme.typography.titleSmall)
                    FilterChip(selected = defaultCurrency == "NGN", onClick = { defaultCurrency = "NGN" }, label = { Text("NGN (₦)") })
                    FilterChip(selected = defaultCurrency == "USD", onClick = { defaultCurrency = "USD" }, label = { Text("USD ($)") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = timeZone, onValueChange = { timeZone = it }, label = { Text("Time Zone") }, modifier = Modifier.weight(1.3f), singleLine = true)
                    OutlinedTextField(value = orderNumberPrefix, onValueChange = { orderNumberPrefix = it.uppercase().take(12) }, label = { Text("Order # Prefix") }, modifier = Modifier.weight(0.9f), singleLine = true)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Checkout Tax / VAT")
                    Switch(checked = taxEnabled, onCheckedChange = { taxEnabled = it })
                }
                if (taxEnabled) {
                    OutlinedTextField(
                        value = taxRateText,
                        onValueChange = { taxRateText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tax Rate (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }

        // Payment Gateways & Bank Transfer
        item {
            AdminSectionCard {
                Text("Payment Gateways & Checkout Rules", style = MaterialTheme.typography.titleLarge)
                AdminSectionCard(containerColor = ZeecrownLightLilac) {
                    Text(
                        "Security Notice: Store ONLY public checkout keys here. Never place Paystack or Flutterwave secret keys inside Firestore public storeSettings.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Paystack"); Switch(checked = paystackEnabled, onCheckedChange = { paystackEnabled = it })
                }
                OutlinedTextField(value = paystackPublicKey, onValueChange = { paystackPublicKey = it }, label = { Text("Paystack Public Key (pk_...)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Flutterwave"); Switch(checked = flutterwaveEnabled, onCheckedChange = { flutterwaveEnabled = it })
                }
                OutlinedTextField(value = flutterwavePublicKey, onValueChange = { flutterwavePublicKey = it }, label = { Text("Flutterwave Public Key (FLWPUBK_...)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Direct Bank Transfer"); Switch(checked = bankTransferEnabled, onCheckedChange = { bankTransferEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Cash on Delivery (COD)"); Switch(checked = codEnabled, onCheckedChange = { codEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Allow Guest Checkout Flag"); Switch(checked = guestCheckoutEnabled, onCheckedChange = { guestCheckoutEnabled = it })
                }
                OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        }

        // Contact, Social & Legal Policies
        item {
            AdminSectionCard {
                Text("Support, Social Links & Store Policies", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Support Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = supportPhone, onValueChange = { supportPhone = it }, label = { Text("Support Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp Business Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = businessAddress, onValueChange = { businessAddress = it }, label = { Text("Business / Showroom Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = supportHours, onValueChange = { supportHours = it }, label = { Text("Support Hours") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = instagramHandle, onValueChange = { instagramHandle = it }, label = { Text("Instagram @") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = instagramUrl, onValueChange = { instagramUrl = it }, label = { Text("Instagram URL") }, modifier = Modifier.weight(1.2f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = tiktokHandle, onValueChange = { tiktokHandle = it }, label = { Text("TikTok @") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = tiktokUrl, onValueChange = { tiktokUrl = it }, label = { Text("TikTok URL") }, modifier = Modifier.weight(1.2f), singleLine = true)
                }
                OutlinedTextField(value = aboutText, onValueChange = { aboutText = it }, label = { Text("About ZEECROWN STORE") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = shippingPolicy, onValueChange = { shippingPolicy = it }, label = { Text("Shipping Policy") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = returnPolicy, onValueChange = { returnPolicy = it }, label = { Text("Return & Refund Policy (All Sales Final)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = privacyPolicy, onValueChange = { privacyPolicy = it }, label = { Text("Privacy Policy Summary") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = termsPolicy, onValueChange = { termsPolicy = it }, label = { Text("Terms & Conditions Summary") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        }

        // Notification Channels & Review Moderation Policy
        item {
            AdminSectionCard {
                Text("Customer Notifications & Review Moderation", style = MaterialTheme.typography.titleLarge)
                AdminSectionCard(containerColor = ZeecrownLightLilac) {
                    Text(
                        "Provider Secrets Notice: Email (Resend/SendGrid/SMTP), Termii/Twilio SMS, and WhatsApp Cloud API tokens must be configured in Cloud Functions .env / Secrets — never stored in Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Email Order Notifications")
                    Switch(checked = emailNotificationsEnabled, onCheckedChange = { emailNotificationsEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable SMS Delivery Alerts")
                    Switch(checked = smsNotificationsEnabled, onCheckedChange = { smsNotificationsEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable WhatsApp Order Updates")
                    Switch(checked = whatsappNotificationsEnabled, onCheckedChange = { whatsappNotificationsEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Require Admin Approval Before Reviews Publish")
                    Switch(checked = requireReviewApproval, onCheckedChange = { requireReviewApproval = it })
                }
                OutlinedTextField(
                    value = emailSenderName,
                    onValueChange = { emailSenderName = it },
                    label = { Text("Email Sender Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = emailReplyTo,
                    onValueChange = { emailReplyTo = it },
                    label = { Text("Customer Reply-To Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // SEO, Social Media & Google Analytics (GA4)
        item {
            AdminSectionCard {
                val titleFb = SeoUtils.evaluateTitleLength(defaultSeoTitle)
                val descFb = SeoUtils.evaluateDescriptionLength(defaultSeoDescription)
                val gaValid = ga4MeasurementId.isBlank() || SeoUtils.isValidGa4MeasurementId(ga4MeasurementId)
                Text("SEO, Social Media & Google Analytics (GA4)", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = publicSiteBaseUrl,
                    onValueChange = { publicSiteBaseUrl = it.take(240) },
                    label = { Text("Canonical Public Storefront URL") },
                    supportingText = { Text("Used for canonical URLs, XML sitemaps, and social sharing links (e.g. https://zeecrownstore.com).") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_settings_site_url"),
                )
                OutlinedTextField(
                    value = defaultSeoTitle,
                    onValueChange = { defaultSeoTitle = it.take(120) },
                    label = { Text("Homepage Default SEO Title") },
                    supportingText = {
                        Text(
                            text = titleFb.guidance,
                            color = if (titleFb.status == SeoLengthStatus.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = defaultSeoDescription,
                    onValueChange = { defaultSeoDescription = it.take(300) },
                    label = { Text("Homepage Default Meta Description") },
                    supportingText = {
                        Text(
                            text = descFb.guidance,
                            color = if (descFb.status == SeoLengthStatus.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = defaultSocialImageUrl,
                    onValueChange = { defaultSocialImageUrl = it.take(600) },
                    label = { Text("Default Open Graph / Social Share Image URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = facebookUrl, onValueChange = { facebookUrl = it.take(240) }, label = { Text("Facebook Page URL") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = pinterestUrl, onValueChange = { pinterestUrl = it.take(240) }, label = { Text("Pinterest Profile URL") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = youtubeUrl, onValueChange = { youtubeUrl = it.take(240) }, label = { Text("YouTube Channel URL") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = twitterHandle, onValueChange = { twitterHandle = it.take(80) }, label = { Text("X / Twitter @handle") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(
                    value = ga4MeasurementId,
                    onValueChange = { ga4MeasurementId = it.uppercase().take(30) },
                    label = { Text("Google Analytics 4 Measurement ID") },
                    supportingText = {
                        Text(
                            text = if (gaValid) "Format: G-XXXXXXXXXX. Leave blank to keep GA4 disabled." else "Invalid GA4 format — expected G- followed by alphanumeric characters.",
                            color = if (gaValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        )
                    },
                    isError = !gaValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_settings_ga4_id"),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Require Customer Analytics Consent Banner")
                    Switch(checked = analyticsConsentRequired, onCheckedChange = { analyticsConsentRequired = it })
                }
            }
        }

        // Store Maintenance Toggle + Save CTA
        item {
            AdminSectionCard {
                Text("Maintenance Mode", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Boutique Maintenance Banner")
                    Switch(checked = maintenanceMode, onCheckedChange = { maintenanceMode = it })
                }
                if (maintenanceMode) {
                    OutlinedTextField(value = maintenanceMessage, onValueChange = { maintenanceMessage = it }, label = { Text("Maintenance Banner Message") }, modifier = Modifier.fillMaxWidth())
                }

                Button(
                    onClick = {
                        onSaveSettings(
                            current.copy(
                                storeName = storeName.trim().ifBlank { "ZEECROWN STORE" },
                                tagline = tagline.trim(),
                                logoUrl = logoUrl.trim(),
                                supportEmail = supportEmail.trim(),
                                supportPhone = supportPhone.trim(),
                                whatsappNumber = whatsapp.trim(),
                                businessAddress = businessAddress.trim(),
                                supportHours = supportHours.trim(),
                                defaultCurrency = defaultCurrency,
                                timeZone = timeZone.trim().ifBlank { "Africa/Lagos" },
                                orderNumberPrefix = orderNumberPrefix.trim().ifBlank { "ZC" },
                                taxEnabled = taxEnabled,
                                taxRatePercent = taxRateText.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 0.0,
                                bankName = bankName.trim(),
                                accountName = accountName.trim(),
                                accountNumber = accountNumber.trim(),
                                paystackEnabled = paystackEnabled,
                                flutterwaveEnabled = flutterwaveEnabled,
                                bankTransferEnabled = bankTransferEnabled,
                                codEnabled = codEnabled,
                                guestCheckoutEnabled = guestCheckoutEnabled,
                                paystackPublicKey = paystackPublicKey.trim(),
                                flutterwavePublicKey = flutterwavePublicKey.trim(),
                                aboutText = aboutText.trim(),
                                shippingPolicy = shippingPolicy.trim(),
                                returnPolicy = returnPolicy.trim(),
                                privacyPolicy = privacyPolicy.trim(),
                                termsPolicy = termsPolicy.trim(),
                                instagramHandle = instagramHandle.trim(),
                                instagramUrl = instagramUrl.trim(),
                                tiktokHandle = tiktokHandle.trim(),
                                tiktokUrl = tiktokUrl.trim(),
                                maintenanceMode = maintenanceMode,
                                maintenanceMessage = maintenanceMessage.trim(),
                                emailNotificationsEnabled = emailNotificationsEnabled,
                                smsNotificationsEnabled = smsNotificationsEnabled,
                                whatsappNotificationsEnabled = whatsappNotificationsEnabled,
                                requireReviewApproval = requireReviewApproval,
                                emailSenderName = emailSenderName.trim().ifBlank { "ZEECROWN STORE" },
                                emailReplyTo = emailReplyTo.trim(),
                                publicSiteBaseUrl = SeoUtils.normalizeBaseUrl(publicSiteBaseUrl),
                                facebookUrl = facebookUrl.trim(),
                                pinterestUrl = pinterestUrl.trim(),
                                youtubeUrl = youtubeUrl.trim(),
                                twitterHandle = twitterHandle.trim(),
                                defaultSeoTitle = defaultSeoTitle.trim(),
                                defaultSeoDescription = defaultSeoDescription.trim(),
                                defaultSocialImageUrl = defaultSocialImageUrl.trim(),
                                ga4MeasurementId = ga4MeasurementId.trim().uppercase(),
                                analyticsConsentRequired = analyticsConsentRequired,
                            ),
                            newLogoUri,
                        )
                        newLogoUri = null
                    },
                    enabled = !isBusy && storeName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp).minimumInteractiveComponentSize().testTag("admin_save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) { Text("Save All Store Settings") }
            }
        }
    }
}
