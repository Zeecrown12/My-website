package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.firebase.configureAppCheck
import com.example.service.ZeecrownMessagingService
import com.example.ui.navigation.PendingDeepLink
import com.example.ui.navigation.ZeecrownAppRoot
import com.example.ui.theme.ZeecrownTheme

class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<PendingDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        ZeecrownMessagingService.ensureChannels(applicationContext)
        pendingDeepLink = extractDeepLink(intent)
        setContent {
            ZeecrownTheme {
                ZeecrownAppRoot(
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkHandled = { pendingDeepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLink(intent)?.let { parsed ->
            pendingDeepLink = parsed
        }
    }

    private fun extractDeepLink(intent: Intent?): PendingDeepLink? {
        if (intent == null) return null

        val extraRoute = intent.getStringExtra(ZeecrownMessagingService.EXTRA_TARGET_ROUTE).orEmpty().trim()
        val extraOrderId = intent.getStringExtra(ZeecrownMessagingService.EXTRA_ORDER_ID).orEmpty().trim()
        if (extraRoute.isNotBlank()) {
            return parseRouteString(extraRoute, extraOrderId)
        }
        if (extraOrderId.isNotBlank() && extraOrderId.matches(SAFE_ID_REGEX)) {
            return PendingDeepLink(type = "order", id = extraOrderId)
        }

        val data = intent.data ?: return null
        val host = data.host.orEmpty()
        val scheme = data.scheme.orEmpty()
        val path = data.path.orEmpty()

        return when {
            (scheme == "zeecrown" && host == "product") ||
                (scheme == "https" && path.startsWith("/product/")) -> {
                val id = data.lastPathSegment?.trim()
                    ?.takeIf { it.isNotBlank() && it != "product" && it.matches(SAFE_ID_REGEX) }
                id?.let { PendingDeepLink(type = "product", id = it) }
            }
            (scheme == "zeecrown" && host == "category") ||
                (scheme == "https" && path.startsWith("/category/")) -> {
                val id = data.lastPathSegment?.trim()
                    ?.takeIf { it.isNotBlank() && it != "category" && it.matches(SAFE_ID_REGEX) }
                id?.let { PendingDeepLink(type = "category", id = it) }
            }
            (scheme == "zeecrown" && host == "collection") ||
                (scheme == "https" && path.startsWith("/collection/")) -> {
                val id = data.lastPathSegment?.trim()
                    ?.takeIf { it.isNotBlank() && it != "collection" && it.matches(SAFE_ID_REGEX) }
                id?.let { PendingDeepLink(type = "collection", id = it) }
            }
            (scheme == "zeecrown" && host == "order") ||
                (scheme == "https" && path.startsWith("/order/")) -> {
                val id = data.lastPathSegment?.trim()
                    ?.takeIf { it.isNotBlank() && it != "order" && it.matches(SAFE_ID_REGEX) }
                PendingDeepLink(type = "order", id = id.orEmpty())
            }
            (scheme == "zeecrown" && host == "track") ||
                (scheme == "https" && path.startsWith("/track")) -> {
                val token = data.getQueryParameter("token")?.trim().orEmpty()
                val id = data.lastPathSegment?.trim()
                    ?.takeIf { it.isNotBlank() && it != "track" && it.matches(SAFE_ID_REGEX) }
                    .orEmpty()
                PendingDeepLink(type = "track", id = id, token = token)
            }
            scheme == "zeecrown" && host == "notifications" -> PendingDeepLink(type = "notifications")
            scheme == "zeecrown" && host == "support" -> PendingDeepLink(type = "support")
            else -> null
        }
    }

    private fun parseRouteString(route: String, fallbackOrderId: String): PendingDeepLink? {
        return when {
            route.startsWith("track_order/") -> {
                val id = route.removePrefix("track_order/").trim().takeIf { it.matches(SAFE_ID_REGEX) }.orEmpty()
                PendingDeepLink(type = "order", id = id.ifBlank { fallbackOrderId })
            }
            route == "track_order" -> PendingDeepLink(type = "track", id = fallbackOrderId)
            route == "notifications" -> PendingDeepLink(type = "notifications")
            route == "support" -> PendingDeepLink(type = "support")
            route.startsWith("product/") -> {
                val id = route.removePrefix("product/").trim().takeIf { it.matches(SAFE_ID_REGEX) }
                id?.let { PendingDeepLink(type = "product", id = it) }
            }
            fallbackOrderId.isNotBlank() -> PendingDeepLink(type = "order", id = fallbackOrderId)
            else -> null
        }
    }

    private companion object {
        val SAFE_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
