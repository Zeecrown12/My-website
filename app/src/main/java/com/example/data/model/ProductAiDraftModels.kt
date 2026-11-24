package com.example.data.model

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
            val obj = MiniJsonParser(cleanJson).parseObject()

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

            fun optString(key: String): String = obj[key] as? String ?: ""

            fun readStringArray(key: String, maxItems: Int, itemMaxLen: Int): List<String> {
                val arr = obj[key] as? List<*> ?: return emptyList()
                val out = mutableListOf<String>()
                for (raw in arr) {
                    val item = sanitizeText(raw?.toString().orEmpty(), itemMaxLen)
                    if (item.isNotBlank() && out.none { it.equals(item, ignoreCase = true) }) {
                        out += item
                    }
                    if (out.size >= maxItems) break
                }
                return out
            }

            val rawTitle = sanitizeText(optString("suggestedTitle"), 120)
            val rawSummary = sanitizeText(optString("summary"), 300)
            val rawDescription = sanitizeText(optString("description"), 2000)
            val visibleFeatures = readStringArray("visibleFeatures", maxItems = 10, itemMaxLen = 140)
            val rawColors = readStringArray("visibleColors", maxItems = 12, itemMaxLen = 40)
            val fashionStyle = sanitizeText(optString("fashionStyle"), 80)
            val occasions = readStringArray("suitableOccasions", maxItems = 8, itemMaxLen = 60)
            val rawSeoTitle = sanitizeText(optString("seoTitle"), 120)
            val rawSeoDesc = sanitizeText(optString("seoDescription"), 300)
            val searchKeywords = readStringArray("searchKeywords", maxItems = 15, itemMaxLen = 40)
            val tags = readStringArray("tags", maxItems = 15, itemMaxLen = 40)
            val uncertainNotes = readStringArray("uncertainDetails", maxItems = 8, itemMaxLen = 140)

            val rawCatHint = optString("suggestedCategoryName").trim()
            val rawCatIdHint = optString("suggestedCategoryId").trim()
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

private class MiniJsonParser(raw: String) {
    private val src: String = run {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start >= 0 && end >= start) raw.substring(start, end + 1) else raw
    }
    private var idx = 0

    fun parseObject(): Map<String, Any?> {
        skipWhitespace()
        expect('{')
        val map = linkedMapOf<String, Any?>()
        skipWhitespace()
        if (peek() == '}') {
            idx++
            return map
        }
        while (idx < src.length) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            when (peek()) {
                ',' -> idx++
                '}' -> {
                    idx++
                    return map
                }
                else -> error("Unexpected character '${peek()}' in JSON object at $idx")
            }
        }
        error("Unterminated JSON object")
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        val list = mutableListOf<Any?>()
        skipWhitespace()
        if (peek() == ']') {
            idx++
            return list
        }
        while (idx < src.length) {
            list += parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> idx++
                ']' -> {
                    idx++
                    return list
                }
                else -> error("Unexpected character '${peek()}' in JSON array at $idx")
            }
        }
        error("Unterminated JSON array")
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        return when (val ch = peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> consumeLiteral("true", true)
            'f' -> consumeLiteral("false", false)
            'n' -> consumeLiteral("null", null)
            '-', in '0'..'9' -> parseNumber()
            else -> error("Unexpected character '$ch' in JSON at $idx")
        }
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (idx < src.length) {
            val ch = src[idx++]
            if (ch == '"') return sb.toString()
            if (ch == '\\') {
                if (idx >= src.length) error("Unterminated escape in JSON string")
                when (val esc = src[idx++]) {
                    '"', '\\', '/' -> sb.append(esc)
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        val hex = src.substring(idx, (idx + 4).coerceAtMost(src.length))
                        sb.append(hex.toInt(16).toChar())
                        idx += 4
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(ch)
            }
        }
        error("Unterminated JSON string")
    }

    private fun parseNumber(): Number {
        val start = idx
        while (idx < src.length && src[idx] in "0123456789+-.eE") idx++
        val token = src.substring(start, idx)
        return token.toLongOrNull() ?: token.toDouble()
    }

    private fun <T> consumeLiteral(lit: String, result: T): T {
        if (!src.startsWith(lit, idx)) error("Expected '$lit' at $idx")
        idx += lit.length
        return result
    }

    private fun skipWhitespace() {
        while (idx < src.length && src[idx].isWhitespace()) idx++
    }

    private fun peek(): Char = if (idx < src.length) src[idx] else '\u0000'

    private fun expect(expected: Char) {
        if (idx >= src.length || src[idx] != expected) {
            error("Expected '$expected' at $idx")
        }
        idx++
    }
}

