package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.Category
import com.example.data.model.ProductAiSuggestion
import com.example.data.model.ProductDraftAiParser
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.URL

class ProductPhotoAnalysisService {

    private val productDraftSchema = Schema.obj(
        properties = mapOf(
            "suggestedTitle" to Schema.string(description = "Refined, luxury e-commerce product title matching what is clearly visible."),
            "summary" to Schema.string(description = "Concise 1-2 sentence storefront summary based only on visual details."),
            "description" to Schema.string(description = "Polished, scannable luxury fashion product description detailing visible design, silhouette, finish, and styling appeal."),
            "visibleFeatures" to Schema.array(
                items = Schema.string(),
                description = "Up to 6 short factual bullet points describing visible craft, cut, neckline, heel/sole, pattern, clasp, or trim.",
            ),
            "suggestedCategoryId" to Schema.string(description = "Exact category ID from the provided catalog categories list, or empty string if unsure."),
            "suggestedCategoryName" to Schema.string(description = "Matching category name from the provided catalog list, or empty string if unsure."),
            "visibleColors" to Schema.array(
                items = Schema.string(),
                description = "Visible colours/shades clearly seen across the photos (e.g., Royal Purple, Gold, Black). Empty if unclear.",
            ),
            "fashionStyle" to Schema.string(description = "Short fashion style classification (e.g., Modest Luxury, Contemporary Couture, Classic Footwear)."),
            "suitableOccasions" to Schema.array(
                items = Schema.string(),
                description = "Up to 5 suitable occasions (e.g., Evening Reception, Festive Celebrations, Smart Daytime).",
            ),
            "seoTitle" to Schema.string(description = "Concise SEO page title under 65 characters."),
            "seoDescription" to Schema.string(description = "Concise SEO meta description under 155 characters."),
            "searchKeywords" to Schema.array(
                items = Schema.string(),
                description = "Up to 8 natural search keywords buyers might type.",
            ),
            "tags" to Schema.array(
                items = Schema.string(),
                description = "Up to 8 short product tags in lowercase.",
            ),
            "uncertainDetails" to Schema.array(
                items = Schema.string(),
                description = "Details that cannot be verified from photos alone (such as fabric composition, sizing, weight, or origin) so admin knows to confirm.",
            ),
        ),
        optionalProperties = listOf(
            "suggestedCategoryId",
            "suggestedCategoryName",
            "visibleColors",
            "suitableOccasions",
            "searchKeywords",
            "tags",
            "uncertainDetails",
        ),
    )

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.25f
                responseMimeType = "application/json"
                responseSchema = productDraftSchema
            },
        )
    }

    suspend fun analyzeProductPhotos(
        context: Context,
        photoEntries: List<String>,
        categories: List<Category>,
        existingTitleHint: String = "",
    ): Result<ProductAiSuggestion> {
        val cleanEntries = photoEntries.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_PHOTOS_PER_REQUEST)
        if (cleanEntries.isEmpty()) {
            return Result.failure(IllegalArgumentException("Add at least one product photo to generate an AI draft."))
        }

        val bitmaps = mutableListOf<Bitmap>()
        return try {
            withContext(Dispatchers.IO) {
                for (entry in cleanEntries) {
                    val bmp = decodeEntryToBitmap(context, entry)
                    if (bmp != null) {
                        bitmaps += bmp
                    }
                }
            }

            if (bitmaps.isEmpty()) {
                return Result.failure(IllegalStateException("Could not read product image data for AI analysis."))
            }

            val categoryGuide = if (categories.isEmpty()) {
                "No predefined categories loaded; leave suggestedCategoryId empty."
            } else {
                categories.joinToString("\n") { "- id=\"${it.id}\", name=\"${it.name}\"" }
            }

            val promptText = buildString {
                appendLine("You are the merchandiser and luxury copywriter for ZEECROWN STORE (Nigerian & international fashion atelier).")
                appendLine("Analyze ALL ${bitmaps.size} photo(s) together as angles/details of ONE single product.")
                if (existingTitleHint.isNotBlank()) {
                    appendLine("Admin working title note (optional hint): \"${existingTitleHint.trim().take(100)}\"")
                }
                appendLine("\nAvailable Store Categories (pick exact id & name ONLY if clearly matching, otherwise return empty strings):")
                appendLine(categoryGuide)
                appendLine("\nSTRICT ACCURACY & NON-HALLUCINATION RULES:")
                appendLine("1. Describe ONLY what is visually observable in the uploaded photos (silhouette, embroidery, print, hardware, sole, colour, finish).")
                appendLine("2. NEVER invent or guess selling price, currency amounts, stock counts, available sizes, brand name, exact fabric/material percentages, dimensions, weight, country of origin, or quality certifications.")
                appendLine("3. If a detail like exact material blend, measurements, or underside craft is unclear, list it in uncertainDetails rather than guessing.")
                appendLine("4. Keep tone refined, inviting, warm, and clear for fashion shoppers.")
            }

            val response = withTimeout(60_000L) {
                visionModel.generateContent(
                    content {
                        bitmaps.forEach { bmp -> image(bmp) }
                        text(promptText)
                    }
                )
            }

            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) {
                return Result.failure(IllegalStateException("AI model returned an empty product draft."))
            }

            ProductDraftAiParser.parseAndSanitize(rawJson = rawJson, categories = categories)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Product photo AI analysis timed out", e)
            Result.failure(IllegalStateException("Product photo analysis timed out. Please check connection and retry.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Product photo AI analysis failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Unable to analyze product photos right now.", e))
        } finally {
            bitmaps.forEach { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
    }

    private fun decodeEntryToBitmap(context: Context, entry: String): Bitmap? {
        return try {
            val rawBytes: ByteArray? = when {
                entry.startsWith("content://") || entry.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(entry))?.use { it.readBytes() }
                }
                entry.startsWith("drawable://") -> {
                    val resName = entry.removePrefix("drawable://").trim()
                    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                    if (resId != 0) {
                        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                        return BitmapFactory.decodeResource(context.resources, resId, options)
                    } else null
                }
                entry.startsWith("https://") -> {
                    val conn = (URL(entry).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 10000
                        instanceFollowRedirects = true
                    }
                    try {
                        if (conn.responseCode in 200..299) {
                            conn.inputStream.use { it.readBytes() }
                        } else null
                    } finally {
                        conn.disconnect()
                    }
                }
                else -> null
            }

            if (rawBytes == null || rawBytes.isEmpty()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAX_AI_IMAGE_PX || bounds.outHeight / sampleSize > MAX_AI_IMAGE_PX) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
        } catch (e: Exception) {
            Log.w(TAG, "Skipping unreadable photo entry for AI draft: $entry", e)
            null
        }
    }

    private companion object {
        const val TAG = "ProductPhotoAiSvc"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val MAX_PHOTOS_PER_REQUEST = 5
        const val MAX_AI_IMAGE_PX = 1024
    }
}
