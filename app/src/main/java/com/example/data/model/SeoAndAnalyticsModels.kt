package com.example.data.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

enum class SeoLengthStatus(val label: String) {
    TOO_SHORT("Too short"),
    OPTIMAL("Optimal length"),
    TOO_LONG("Too long"),
    EMPTY("Using default"),
}

data class SeoLengthFeedback(
    val charCount: Int,
    val status: SeoLengthStatus,
    val guidance: String,
)

data class GeneratedSeoDefaults(
    val slug: String,
    val seoTitle: String,
    val seoDescription: String,
)

enum class SocialSharePlatform(val label: String) {
    WHATSAPP("WhatsApp"),
    FACEBOOK("Facebook"),
    PINTEREST("Pinterest"),
    INSTAGRAM("Instagram"),
    COPY_LINK("Copy Link"),
    SYSTEM_SHARE("More Apps"),
}

data class SocialShareTarget(
    val contentType: String, // "product" | "category" | "collection" | "storefront"
    val itemId: String,
    val title: String,
    val subtitle: String = "",
    val canonicalUrl: String,
    val imageUrl: String = "",
    val priceFormatted: String = "",
) {
    fun buildShareCaption(): String = buildString {
        append(title.trim())
        if (priceFormatted.isNotBlank()) {
            append(" — ")
            append(priceFormatted.trim())
        }
        if (subtitle.isNotBlank()) {
            append("\n")
            append(subtitle.trim().take(140))
        }
        append("\n")
        append(canonicalUrl)
    }
}

object SeoUtils {
    private val nonSlugRegex = Regex("[^a-z0-9\\s-]")
    private val whitespaceOrDashRegex = Regex("[\\s-]+")
    private val ga4Regex = Regex("^G-[A-Z0-9]{4,20}$")

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return "https://zeecrownstore.com"
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun isValidGa4MeasurementId(raw: String): Boolean {
        val trimmed = raw.trim().uppercase(Locale.US)
        return trimmed.isNotBlank() && ga4Regex.matches(trimmed)
    }

    fun sanitizeSlug(raw: String, fallbackPrefix: String = "zeecrown-piece"): String {
        val cleaned = raw.lowercase(Locale.US)
            .replace("&", " and ")
            .replace(nonSlugRegex, "")
            .trim()
            .replace(whitespaceOrDashRegex, "-")
            .trim('-')
            .take(100)
        return cleaned.ifBlank { fallbackPrefix }
    }

    fun evaluateTitleLength(title: String): SeoLengthFeedback {
        val len = title.trim().length
        return when {
            len == 0 -> SeoLengthFeedback(0, SeoLengthStatus.EMPTY, "Aim for 45–60 characters")
            len < 25 -> SeoLengthFeedback(len, SeoLengthStatus.TOO_SHORT, "$len chars (aim for 45–60)")
            len <= 65 -> SeoLengthFeedback(len, SeoLengthStatus.OPTIMAL, "$len / 60 chars — ideal for Google")
            else -> SeoLengthFeedback(len, SeoLengthStatus.TOO_LONG, "$len chars — may truncate past 65")
        }
    }

    fun evaluateDescriptionLength(description: String): SeoLengthFeedback {
        val len = description.trim().length
        return when {
            len == 0 -> SeoLengthFeedback(0, SeoLengthStatus.EMPTY, "Aim for 120–160 characters")
            len < 70 -> SeoLengthFeedback(len, SeoLengthStatus.TOO_SHORT, "$len chars (aim for 120–160)")
            len <= 165 -> SeoLengthFeedback(len, SeoLengthStatus.OPTIMAL, "$len / 160 chars — ideal snippet")
            else -> SeoLengthFeedback(len, SeoLengthStatus.TOO_LONG, "$len chars — may truncate past 165")
        }
    }

    fun generateProductSeoDefaults(
        title: String,
        description: String,
        categoryName: String = "",
        brand: String = "ZEECROWN STORE",
        explicitSlug: String = "",
    ): GeneratedSeoDefaults {
        val slug = sanitizeSlug(explicitSlug.ifBlank { title }, "zeecrown-piece")
        val cleanTitle = title.trim().ifBlank { "Boutique Piece" }
        val suffix = if (categoryName.isNotBlank()) " | ${categoryName.trim()} | ZEECROWN" else " | ZEECROWN STORE"
        val seoTitle = if ((cleanTitle + suffix).length <= 65) {
            cleanTitle + suffix
        } else {
            "$cleanTitle | ZEECROWN".take(65)
        }

        val cleanDesc = description.trim().replace(Regex("\\s+"), " ")
        val baseSnippet = if (cleanDesc.isNotBlank()) {
            cleanDesc
        } else {
            val catPart = if (categoryName.isNotBlank()) " in ${categoryName.trim()}" else ""
            "Discover $cleanTitle$catPart crafted with timeless elegance at ${brand.ifBlank { "ZEECROWN STORE" }}."
        }
        val ctaTail = " Shop online with fast nationwide & international delivery."
        val combinedDesc = if (baseSnippet.length >= 110) {
            baseSnippet.take(157).let { if (baseSnippet.length > 157) "${it.trimEnd()}..." else it }
        } else {
            (baseSnippet.trimEnd('.') + "." + ctaTail).take(160)
        }
        return GeneratedSeoDefaults(
            slug = slug,
            seoTitle = seoTitle.take(120),
            seoDescription = combinedDesc.take(300),
        )
    }

    fun generateCategorySeoDefaults(
        name: String,
        description: String,
        explicitSlug: String = "",
    ): GeneratedSeoDefaults {
        val slug = sanitizeSlug(explicitSlug.ifBlank { name }, "collection")
        val cleanName = name.trim().ifBlank { "Boutique Category" }
        val seoTitle = "$cleanName Collection | ZEECROWN STORE".take(65)
        val cleanDesc = description.trim().replace(Regex("\\s+"), " ")
        val seoDescription = if (cleanDesc.length >= 70) {
            cleanDesc.take(160)
        } else {
            "Explore curated $cleanName pieces at ZEECROWN STORE. Elevated craftsmanship and signature Nigerian luxury shipped worldwide."
                .take(160)
        }
        return GeneratedSeoDefaults(
            slug = slug,
            seoTitle = seoTitle,
            seoDescription = seoDescription,
        )
    }

    fun generateCollectionSeoDefaults(
        name: String,
        tagline: String,
        explicitSlug: String = "",
    ): GeneratedSeoDefaults {
        val slug = sanitizeSlug(explicitSlug.ifBlank { name }, "edit")
        val cleanName = name.trim().ifBlank { "Signature Edit" }
        val seoTitle = "$cleanName Edit | ZEECROWN STORE".take(65)
        val cleanTagline = tagline.trim().replace(Regex("\\s+"), " ")
        val seoDescription = if (cleanTagline.length >= 70) {
            cleanTagline.take(160)
        } else {
            "Shop the $cleanName editorial capsule at ZEECROWN STORE. Limited luxury fashion pieces with global shipping."
                .take(160)
        }
        return GeneratedSeoDefaults(
            slug = slug,
            seoTitle = seoTitle,
            seoDescription = seoDescription,
        )
    }

    fun buildCanonicalProductUrl(baseUrl: String, product: Product): String {
        val normalized = normalizeBaseUrl(baseUrl)
        val identifier = sanitizeSlug(product.slug.ifBlank { product.id }, product.id.ifBlank { "piece" })
        return "$normalized/product/$identifier"
    }

    fun buildCanonicalCategoryUrl(baseUrl: String, category: Category): String {
        val normalized = normalizeBaseUrl(baseUrl)
        val identifier = sanitizeSlug(category.slug.ifBlank { category.id }, category.id.ifBlank { "shop" })
        return "$normalized/category/$identifier"
    }

    fun buildCanonicalCollectionUrl(baseUrl: String, collection: StoreCollection): String {
        val normalized = normalizeBaseUrl(baseUrl)
        val identifier = sanitizeSlug(collection.slug.ifBlank { collection.id }, collection.id.ifBlank { "edit" })
        return "$normalized/collection/$identifier"
    }

    /**
     * Generates accurate Schema.org Product JSON-LD using ONLY real catalog & visible review metrics.
     * Returns null when the product is inactive (`active == false`), and omits `aggregateRating`
     * whenever there are zero verified visible reviews.
     */
    fun buildProductJsonLd(
        product: Product,
        baseUrl: String,
        categoryName: String = "",
        inStock: Boolean = true,
        visibleReviews: List<Review> = emptyList(),
        currency: StoreCurrency = StoreCurrency.NGN,
    ): String? {
        if (!product.active) return null
        val canonical = buildCanonicalProductUrl(baseUrl, product)
        val approved = visibleReviews.filter { it.visible && it.rating in 1L..5L }
        val price = if (currency == StoreCurrency.USD && product.priceUsd > 0.0) {
            product.priceUsd
        } else {
            product.priceNgn
        }
        val priceCode = if (currency == StoreCurrency.USD && product.priceUsd > 0.0) "USD" else "NGN"
        val images = buildList {
            if (product.socialImageUrl.isNotBlank()) add(product.socialImageUrl)
            if (product.mainImageUrl.isNotBlank()) add(product.mainImageUrl)
            addAll(product.imageUrls.filter { it.isNotBlank() })
        }.distinct()
        val availability = if (inStock) "https://schema.org/InStock" else "https://schema.org/OutOfStock"

        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"@context\":\"https://schema.org\",")
        sb.append("\"@type\":\"Product\",")
        sb.append("\"name\":\"").append(escapeJson(product.title)).append("\",")
        sb.append("\"description\":\"").append(escapeJson(product.seoDescription.ifBlank { product.description })).append("\",")
        sb.append("\"url\":\"").append(escapeJson(canonical)).append("\",")
        if (product.sku.isNotBlank()) {
            sb.append("\"sku\":\"").append(escapeJson(product.sku)).append("\",")
        }
        if (categoryName.isNotBlank()) {
            sb.append("\"category\":\"").append(escapeJson(categoryName)).append("\",")
        }
        sb.append("\"brand\":{\"@type\":\"Brand\",\"name\":\"")
            .append(escapeJson(product.brand.ifBlank { "ZEECROWN STORE" }))
            .append("\"},")
        if (images.isNotEmpty()) {
            sb.append("\"image\":[")
            sb.append(images.joinToString(",") { "\"${escapeJson(it)}\"" })
            sb.append("],")
        }
        sb.append("\"offers\":{")
        sb.append("\"@type\":\"Offer\",")
        sb.append("\"url\":\"").append(escapeJson(canonical)).append("\",")
        sb.append("\"priceCurrency\":\"").append(priceCode).append("\",")
        sb.append("\"price\":\"").append(String.format(Locale.US, "%.2f", price)).append("\",")
        sb.append("\"availability\":\"").append(availability).append("\",")
        sb.append("\"itemCondition\":\"https://schema.org/NewCondition\"")
        sb.append("}")
        if (approved.isNotEmpty()) {
            val avg = approved.map { it.rating.toDouble() }.average()
            sb.append(",\"aggregateRating\":{")
            sb.append("\"@type\":\"AggregateRating\",")
            sb.append("\"ratingValue\":\"").append(String.format(Locale.US, "%.1f", avg)).append("\",")
            sb.append("\"reviewCount\":").append(approved.size)
            sb.append("}")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}

object SocialShareLinkBuilder {
    fun buildWhatsAppUrl(target: SocialShareTarget): String {
        val text = target.buildShareCaption()
        return "https://wa.me/?text=${urlEncode(text)}"
    }

    fun buildFacebookUrl(target: SocialShareTarget): String =
        "https://www.facebook.com/sharer/sharer.php?u=${urlEncode(target.canonicalUrl)}"

    fun buildPinterestUrl(target: SocialShareTarget): String {
        val description = buildString {
            append(target.title.trim())
            if (target.priceFormatted.isNotBlank()) append(" (${target.priceFormatted})")
        }
        val mediaParam = if (target.imageUrl.startsWith("http")) {
            "&media=${urlEncode(target.imageUrl)}"
        } else {
            ""
        }
        return "https://pinterest.com/pin/create/button/?url=${urlEncode(target.canonicalUrl)}$mediaParam&description=${urlEncode(description)}"
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
