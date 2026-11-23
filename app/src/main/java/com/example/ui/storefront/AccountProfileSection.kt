package com.example.ui.storefront

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.auth.AuthValidation
import com.example.data.model.Order
import com.example.data.model.StoreCurrency
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountOverviewTab(
    profile: UserProfile?,
    userEmail: String,
    isEmailVerified: Boolean,
    resendCooldownSeconds: Int,
    orderCount: Int,
    wishlistCount: Int,
    addressCount: Int,
    recentOrder: Order?,
    isSubmitting: Boolean,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onNavigateSection: (AccountDashboardSection) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EmailVerificationStatusCard(
            email = userEmail,
            isVerified = isEmailVerified,
            resendCooldownSeconds = resendCooldownSeconds,
            isSubmitting = isSubmitting,
            onResend = onResendVerification,
            onRefresh = onRefreshVerification,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OverviewStatTile(
                title = "Orders",
                value = orderCount.toString(),
                icon = Icons.Outlined.ShoppingBag,
                onClick = { onNavigateSection(AccountDashboardSection.ORDERS) },
                modifier = Modifier.width(155.dp),
            )
            OverviewStatTile(
                title = "Wishlist",
                value = wishlistCount.toString(),
                icon = Icons.Outlined.FavoriteBorder,
                onClick = { onNavigateSection(AccountDashboardSection.WISHLIST) },
                modifier = Modifier.width(155.dp),
            )
            OverviewStatTile(
                title = "Saved Addresses",
                value = addressCount.toString(),
                icon = Icons.Outlined.LocationOn,
                onClick = { onNavigateSection(AccountDashboardSection.ADDRESSES) },
                modifier = Modifier.width(155.dp),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = ZeecrownLightLilac,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Personal Boutique Summary", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Name: ${profile?.displayName?.ifBlank { "Zeecrown Client" } ?: "Zeecrown Client"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Preferred Display Currency: ${profile?.preferredCurrency ?: StoreCurrency.NGN.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (recentOrder != null) {
                    Text(
                        text = "Latest Order: ${recentOrder.orderNumber} (${recentOrder.status.replace('_', ' ')})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onNavigateSection(AccountDashboardSection.PROFILE) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("Edit Profile")
                    }
                    OutlinedButton(
                        onClick = { onNavigateSection(AccountDashboardSection.ADDRESSES) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("Manage Addresses")
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStatTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmailVerificationStatusCard(
    email: String,
    isVerified: Boolean,
    resendCooldownSeconds: Int,
    isSubmitting: Boolean,
    onResend: () -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isVerified) ZeecrownSageSuccess.copy(alpha = 0.12f) else ZeecrownLightLilac,
        border = BorderStroke(
            width = 1.dp,
            color = if (isVerified) ZeecrownSageSuccess.copy(alpha = 0.45f) else ZeecrownRoyalPurple.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isVerified) Icons.Outlined.CheckCircle else Icons.Outlined.MarkEmailUnread,
                    contentDescription = null,
                    tint = if (isVerified) ZeecrownSageSuccess else ZeecrownDeepPurple,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isVerified) "Email Verified" else "Verify Your Email Address",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isVerified) {
                            "$email is verified for order receipts and security actions."
                        } else {
                            "Check $email for a verification link so order updates and security changes stay protected."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!isVerified) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onResend,
                        enabled = !isSubmitting && resendCooldownSeconds == 0,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("resend_verification_button"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeecrownRoyalPurple,
                            contentColor = ZeecrownPureWhite,
                        ),
                    ) {
                        Text(
                            if (resendCooldownSeconds > 0) "Resend in ${resendCooldownSeconds}s"
                            else "Resend Verification"
                        )
                    }
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_verification_button"),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("I've Verified")
                    }
                }
            }
        }
    }
}

@Composable
fun AccountProfileTab(
    profile: UserProfile?,
    privateInfo: UserPrivateInfo?,
    userEmail: String,
    isSubmitting: Boolean,
    onUploadPhoto: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onSavePersonalInfo: (firstName: String, lastName: String, phone: String) -> Unit,
    onToggleCurrency: (StoreCurrency) -> Unit,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUploadPhoto(uri)
    }

    var firstName by remember(profile) { mutableStateOf(profile?.resolvedFirstName().orEmpty()) }
    var lastName by remember(profile) { mutableStateOf(profile?.resolvedLastName().orEmpty()) }
    var phone by remember(privateInfo) { mutableStateOf(privateInfo?.phone.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val activeCurrency = StoreCurrency.fromRaw(profile?.preferredCurrency)
    val photoUrl = profile?.photoUrl.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Profile Photo Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Customer Profile Photo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    val initials = listOf(firstName, lastName)
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                        .ifBlank { "ZC" }
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(ZeecrownSoftLavender),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initials, style = MaterialTheme.typography.titleLarge, color = ZeecrownDeepPurple)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Profile Photo", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("upload_profile_photo_button"),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (photoUrl.isBlank()) "Upload Photo" else "Change")
                        }
                        if (photoUrl.isNotBlank()) {
                            TextButton(
                                onClick = onRemovePhoto,
                                enabled = !isSubmitting,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }

        // Personal Details Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, ZeecrownElegantSilver),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Personal Information", style = MaterialTheme.typography.titleLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it
                            validationError = null
                        },
                        label = { Text("First Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_first_name_input"),
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it
                            validationError = null
                        },
                        label = { Text("Last Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_last_name_input"),
                    )
                }

                OutlinedTextField(
                    value = userEmail,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Primary Email (Manage in Security tab)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        validationError = null
                    },
                    label = { Text("Phone / WhatsApp (+234... or international)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Display Currency", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = activeCurrency == StoreCurrency.NGN,
                            onClick = { if (activeCurrency != StoreCurrency.NGN) onToggleCurrency(activeCurrency) },
                            label = { Text("NGN (₦)") },
                        )
                        FilterChip(
                            selected = activeCurrency == StoreCurrency.USD,
                            onClick = { if (activeCurrency != StoreCurrency.USD) onToggleCurrency(activeCurrency) },
                            label = { Text("USD ($)") },
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = {
                        val err = AuthValidation.validateName(firstName, "First name")
                            ?: AuthValidation.validateName(lastName, "Last name")
                            ?: (if (phone.isNotBlank()) AuthValidation.validatePhone(phone) else null)
                        if (err != null) {
                            validationError = err
                            return@Button
                        }
                        onSavePersonalInfo(firstName.trim(), lastName.trim(), phone.trim())
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("save_personal_profile_button"),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZeecrownRoyalPurple,
                        contentColor = ZeecrownPureWhite,
                    ),
                ) {
                    Text(if (isSubmitting) "Saving..." else "Save Profile Changes")
                }
            }
        }
    }
}
