package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SocialShareLinkBuilder
import com.example.data.model.SocialSharePlatform
import com.example.data.model.SocialShareTarget
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SocialShareSheet(
    target: SocialShareTarget,
    onDismiss: () -> Unit,
    onSharePlatform: (SocialSharePlatform) -> Unit = {},
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember(target.canonicalUrl) { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ZeecrownWarmIvory,
        modifier = Modifier.testTag("social_share_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (target.imageUrl.isNotBlank()) {
                    ZeecrownProductImage(
                        imageUrl = target.imageUrl,
                        contentDescription = target.title,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Share ${target.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ZeecrownDeepPurple,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (target.priceFormatted.isNotBlank()) {
                        Text(
                            text = target.priceFormatted,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = target.canonicalUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = ZeecrownElegantSilver)

            Text(
                text = "Share directly to social channels",
                style = MaterialTheme.typography.labelLarge,
                color = ZeecrownDeepPurple,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onSharePlatform(SocialSharePlatform.WHATSAPP)
                        runCatching { uriHandler.openUri(SocialShareLinkBuilder.buildWhatsAppUrl(target)) }
                        onDismiss()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_whatsapp_button"),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp")
                }

                OutlinedButton(
                    onClick = {
                        onSharePlatform(SocialSharePlatform.FACEBOOK)
                        runCatching { uriHandler.openUri(SocialShareLinkBuilder.buildFacebookUrl(target)) }
                        onDismiss()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_facebook_button"),
                ) {
                    Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Facebook")
                }

                OutlinedButton(
                    onClick = {
                        onSharePlatform(SocialSharePlatform.PINTEREST)
                        runCatching { uriHandler.openUri(SocialShareLinkBuilder.buildPinterestUrl(target)) }
                        onDismiss()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_pinterest_button"),
                ) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pinterest")
                }

                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(target.buildShareCaption()))
                        copiedNotice = "Caption & link copied — paste in Instagram Story or bio."
                        onSharePlatform(SocialSharePlatform.INSTAGRAM)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, target.title)
                            putExtra(Intent.EXTRA_TEXT, target.buildShareCaption())
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(sendIntent, "Share caption to Instagram"))
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_instagram_button"),
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Instagram")
                }

                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(target.canonicalUrl))
                        copiedNotice = "Canonical store link copied to clipboard."
                        onSharePlatform(SocialSharePlatform.COPY_LINK)
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_copy_link_button"),
                ) {
                    Icon(
                        imageVector = if (copiedNotice != null) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (copiedNotice != null) "Link Copied" else "Copy Link")
                }
            }

            copiedNotice?.let { notice ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ZeecrownSoftLavender,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZeecrownDeepPurple,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Button(
                onClick = {
                    onSharePlatform(SocialSharePlatform.SYSTEM_SHARE)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "${target.title} — ZEECROWN STORE")
                        putExtra(Intent.EXTRA_TEXT, target.buildShareCaption())
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(sendIntent, "Share ${target.title}"))
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("share_system_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share via Device Apps")
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ZeecrownLightLilac,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Shared links open the ZEECROWN STORE product or department page cleanly across web and Android.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}
