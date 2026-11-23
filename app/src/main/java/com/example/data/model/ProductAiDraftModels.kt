package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class ProductAiSuggestion(
    val suggestedTitle: String = "",
    val summary: String = "",
    val description: String = "",
    val visibleFeatures: List<String> = emptyList(),
    val suggestedCategoryId: String = "",
    val suggestedCategoryName: String = "",
    val suggestedColors: List<String> = emptyList(),
    val fashionStyle: String = "",
    val suitableOccasions: List<String> = emptyList(),
    val seoTitle: String = "",
    val seoDescription: String = "",
    val searchKeywords: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val uncertainNotes: List<String> = emptyList(),
    val guardrailWarnings: List<String> = emptyList(),
)

sealed interface ProductAiAnalysisState {
    data object Idle : ProductAiAnalysisState
    data class Analyzing(val photoCount: Int, val signature: String) : ProductAiAnalysisState
    data class Success(
        val suggestion: ProductAiSuggestion,
        val analyzedSignature: String,
        val appliedToFields: Boolean = true,
    ) : ProductAiAnalysisState
    data class Error(
        val message: String,
        val analyzedSignature: String,
    ) : ProductAiAnalysisState
}

object ProductDraftAiParser {

    private val FORBIDDEN_CLAIM_PATTERNS = listOf(
        Regex("\\b\\d{1,3}%\\s*(cotton|silk|linen|polyester|wool|cashmere|nylon|viscose|rayon|spandex|leather)\\b", RegexOption.IGNORE_CASE) to
            "Exact fabric percentage removed — verify material composition before listing.",
        Regex("\\bmade in\\s+[a-z][a-z\\s]{2,24}\\b", RegexOption.IGNORE_CASE) to
            "Country of origin claim removed — verify origin manually if known.",
        Regex("(?:₦|ngn|\\$|usd)\\s*\\d+[\\d,]*(?:\\.\\d+)?", RegexOption.IGNORE_CASE) to
            "Price reference removed — admin must enter selling price manually.",
        Regex("\\b\\d+(?:\\.\\d+)?\\s*(?:cm|mm|inches|inch|grams|kg|lbs)\\b", RegexOption.IGNORE_CASE) to
            "Exact measurement/weight claim removed — add verified measurements in Size Guide or Attributes.",
        Regex("\\b(iso\\s*\\d+|oeko-?tex|certified organic|gots certified|in stock:\\s*\\d+|only\\s+\\d+\\s+left)\\b", RegexOption.IGNORE_CASE) to
            "Unverified certification or stock count claim removed.",
    )

    fun computePhotoSignature(entries: List<String>): String =
        entries.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("|")

    fun parseAndSanitize(
        rawJson: String,
        categories: List<Category>,
    ): Result<ProductAiSuggestion> {
        return try {
            val cleanJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(cleanJson)

            val warnings = mutableListOf<String>()

            fun sanitizeText(input: String, maxLen: Int): String {
                var value = input.replace(Regex("\\s+"), " ").trim()
                FORBIDDEN_CLAIM_PATTERNS.forEach { (pattern, warningMsg) ->
                    if (pattern.containsMatchIn(value)) {
                        value = pattern.replace(value, "").replace(Regex("\\s{2,}"), " ").trim()
                        if (warningMsg !in warnings) warnings += warningMsg
                    }
                }
                return value.take(maxLen)
            }

            fun readStringArray(key: String, maxItems: Int, itemMaxLen: Int): List<String> {
                val arr: JSONArray = obj.optJSONArray(key) ?: return emptyList()
                val out = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val item = sanitizeText(arr.optString(i).orEmpty(), itemMaxLen)
                    if (item.isNotBlank() && out.none { it.equals(item, ignoreCase = true) }) {
                        out += item
                    }
                    if (out.size >= maxItems) break
                }
                return out
            }

            val rawTitle = sanitizeText(obj.optString("suggestedTitle"), 120)
            val rawSummary = sanitizeText(obj.optString("summary"), 300)
            val rawDescription = sanitizeText(obj.optString("description"), 2000)
            val visibleFeatures = readStringArray("visibleFeatures", maxItems = 10, itemMaxLen = 140)
            val rawColors = readStringArray("visibleColors", maxItems = 12, itemMaxLen = 40)
            val fashionStyle = sanitizeText(obj.optString("fashionStyle"), 80)
            val occasions = readStringArray("suitableOccasions", maxItems = 8, itemMaxLen = 60)
            val rawSeoTitle = sanitizeText(obj.optString("seoTitle"), 120)
            val rawSeoDesc = sanitizeText(obj.optString("seoDescription"), 300)
            val searchKeywords = readStringArray("searchKeywords", maxItems = 15, itemMaxLen = 40)
            val tags = readStringArray("tags", maxItems = 15, itemMaxLen = 40)
            val uncertainNotes = readStringArray("uncertainDetails", maxItems = 8, itemMaxLen = 140)

            val rawCatHint = obj.optString("suggestedCategoryName").trim()
            val rawCatIdHint = obj.optString("suggestedCategoryId").trim()
            val matchedCategory = resolveMatchingCategory(
                categories = categories,
                idHint = rawCatIdHint,
                nameHint = rawCatHint,
            )
            if (matchedCategory == null && rawCatHint.isNotBlank()) {
                warnings += "Suggested category '$rawCatHint' did not match an existing category — please select one manually."
            }
            if (rawColors.isEmpty()) {
                warnings += "Colour was uncertain from photos — verify visible shades manually."
            }

            val resolvedTitle = rawTitle.ifBlank { "New Fashion Piece Draft" }
            val resolvedDesc = rawDescription.ifBlank {
                rawSummary.ifBlank { "Review and refine product description before publishing." }
            }
            val resolvedSeoTitle = rawSeoTitle.ifBlank { "$resolvedTitle | ZEECROWN STORE".take(120) }
            val resolvedSeoDesc = rawSeoDesc.ifBlank {
                (rawSummary.ifBlank { resolvedDesc }).take(160)
            }

            Result.success(
                ProductAiSuggestion(
                    suggestedTitle = resolvedTitle,
                    summary = rawSummary,
                    description = resolvedDesc,
                    visibleFeatures = visibleFeatures,
                    suggestedCategoryId = matchedCategory?.id.orEmpty(),
                    suggestedCategoryName = matchedCategory?.name.orEmpty(),
                    suggestedColors = rawColors,
                    fashionStyle = fashionStyle,
                    suitableOccasions = occasions,
                    seoTitle = resolvedSeoTitle,
                    seoDescription = resolvedSeoDesc,
                    searchKeywords = searchKeywords,
                    tags = (tags + searchKeywords)
                        .map { it.lowercase(Locale.ROOT).trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(15),
                    uncertainNotes = uncertainNotes,
                    guardrailWarnings = warnings,
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Unable to parse AI product draft output.", e))
        }
    }

    fun resolveMatchingCategory(
        categories: List<Category>,
        idHint: String,
        nameHint: String,
    ): Category? {
        if (categories.isEmpty()) return null
        if (idHint.isNotBlank()) {
            categories.firstOrNull { it.id.equals(idHint.trim(), ignoreCase = true) }?.let { return it }
        }
        val cleanName = nameHint.trim().lowercase(Locale.ROOT)
        if (cleanName.isBlank()) return null
        categories.firstOrNull {
            it.name.trim().equals(cleanName, ignoreCase = true) ||
                it.slug.trim().equals(cleanName.replace(" ", "-"), ignoreCase = true)
        }?.let { return it }
        return categories.firstOrNull { cat ->
            val catLower = cat.name.trim().lowercase(Locale.ROOT)
            catLower.contains(cleanName) || cleanName.contains(catLower)
        }
    }
}
