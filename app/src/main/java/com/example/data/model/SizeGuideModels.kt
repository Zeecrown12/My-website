package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale
import kotlin.math.roundToInt

enum class SizeGuideCategoryType(
    val label: String,
    val isFootwear: Boolean,
) {
    ABAYAS_BUBU("Abayas & Bubu", false),
    CLOTHING_KAFTAN("Adire, Kampala & Clothing", false),
    SLIPPERS_SANDALS("Slippers & Sandals", true),
    SHOES_HEELS("Shoes & Heels", true),
    CUSTOM("Custom Boutique Chart", false);

    companion object {
        fun fromRaw(raw: String?): SizeGuideCategoryType =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: ABAYAS_BUBU
    }
}

@Keep
@IgnoreExtraProperties
data class SizeGuideRow(
    val sizeLabel: String = "",
    val values: List<String> = emptyList(),
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sizeLabel" to sizeLabel.trim().take(40),
        "values" to values.map { it.trim().take(40) }.take(7),
    )
}

@Keep
@IgnoreExtraProperties
data class SizeGuide(
    @DocumentId val id: String = "",
    val name: String = "",
    val categoryType: String = SizeGuideCategoryType.ABAYAS_BUBU.name,
    val description: String = "",
    val measurementUnitDefault: String = "CM",
    val columns: List<String> = emptyList(),
    val rows: List<SizeGuideRow> = emptyList(),
    val howToMeasureSteps: List<String> = emptyList(),
    val fitNotes: String = "",
    val assignedCategoryIds: List<String> = emptyList(),
    val assignedProductIds: List<String> = emptyList(),
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val resolvedType: SizeGuideCategoryType
        get() = SizeGuideCategoryType.fromRaw(categoryType)

    fun toCreateMap(isCreate: Boolean = true): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "name" to name.trim().take(100),
            "categoryType" to SizeGuideCategoryType.fromRaw(categoryType).name,
            "description" to description.trim().take(400),
            "measurementUnitDefault" to if (measurementUnitDefault.equals("IN", ignoreCase = true)) "IN" else "CM",
            "columns" to columns.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(8),
            "rows" to rows.take(30).map { it.toMap() },
            "howToMeasureSteps" to howToMeasureSteps.map { it.trim().take(240) }.filter { it.isNotBlank() }.take(12),
            "fitNotes" to fitNotes.trim().take(500),
            "assignedCategoryIds" to assignedCategoryIds.map { it.trim().take(128) }.filter { it.isNotBlank() }.distinct().take(30),
            "assignedProductIds" to assignedProductIds.map { it.trim().take(128) }.filter { it.isNotBlank() }.distinct().take(100),
            "sortOrder" to sortOrder.coerceIn(0L, 1000L),
            "active" to active,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }
}

object SizeGuideUnitConverter {
    private val numberRegex = Regex("\\d+(?:\\.\\d+)?")

    fun convertHeader(header: String, targetUnit: String): String {
        val wantsInches = targetUnit.equals("IN", ignoreCase = true)
        return if (wantsInches) {
            header.replace("(cm)", "(in)", ignoreCase = true)
        } else {
            header.replace("(in)", "(cm)", ignoreCase = true)
        }
    }

    fun convertCellValue(
        rawValue: String,
        columnHeader: String,
        baseUnit: String,
        targetUnit: String,
    ): String {
        val cleanBase = if (baseUnit.equals("IN", ignoreCase = true)) "IN" else "CM"
        val cleanTarget = if (targetUnit.equals("IN", ignoreCase = true)) "IN" else "CM"
        if (cleanBase == cleanTarget) return rawValue

        val lowerHeader = columnHeader.lowercase(Locale.US)
        val isLengthMeasurement = lowerHeader.contains("cm") ||
            lowerHeader.contains("in") ||
            lowerHeader.contains("bust") ||
            lowerHeader.contains("waist") ||
            lowerHeader.contains("hip") ||
            lowerHeader.contains("shoulder") ||
            lowerHeader.contains("length") ||
            lowerHeader.contains("foot") ||
            lowerHeader.contains("sleeve")
        if (!isLengthMeasurement) return rawValue

        return numberRegex.replace(rawValue) { match ->
            val num = match.value.toDoubleOrNull() ?: return@replace match.value
            val converted = if (cleanBase == "CM" && cleanTarget == "IN") {
                num / 2.54
            } else {
                num * 2.54
            }
            val rounded = (converted * 10.0).roundToInt() / 10.0
            if (rounded % 1.0 == 0.0) {
                rounded.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", rounded)
            }
        }
    }
}

object DefaultSizeGuides {
    fun starterGuides(): List<Pair<String, SizeGuide>> = listOf(
        "sg_slippers_sandals" to SizeGuide(
            id = "sg_slippers_sandals",
            name = "Slippers & Sandals Size Guide",
            categoryType = SizeGuideCategoryType.SLIPPERS_SANDALS.name,
            description = "Exact EU/UK/US conversion and foot-length chart for ZEECROWN artisanal leather slippers and sandals.",
            measurementUnitDefault = "CM",
            columns = listOf("EU Size", "UK Size", "US Women", "Foot Length (cm)", "Fit Note"),
            rows = listOf(
                SizeGuideRow("36", listOf("3", "5.5", "22.8", "True to size")),
                SizeGuideRow("37", listOf("4", "6.5", "23.5", "True to size")),
                SizeGuideRow("38", listOf("5", "7.5", "24.1", "True to size")),
                SizeGuideRow("39", listOf("6", "8.5", "24.8", "True to size")),
                SizeGuideRow("40", listOf("7", "9.5", "25.5", "Size up if wide fit")),
                SizeGuideRow("41", listOf("7.5", "10", "26.2", "Size up if wide fit")),
                SizeGuideRow("42", listOf("8.5", "11", "26.8", "Comfort room toe")),
                SizeGuideRow("43", listOf("9.5", "12", "27.5", "Extended sole")),
            ),
            howToMeasureSteps = listOf(
                "Place a blank sheet of A4 paper flat against a straight wall on a hard floor.",
                "Stand evenly on the paper with your heel lightly touching the wall.",
                "Mark the tip of your longest toe with a pencil, then measure heel-to-mark with a ruler.",
                "Compare the longest foot measurement in cm/inches to the Foot Length column above.",
            ),
            fitNotes = "For broad feet or high insteps in backless mules and palm slippers, we recommend selecting one EU size up.",
            assignedCategoryIds = listOf("cat_slippers"),
            sortOrder = 1L,
            active = true,
        ),
        "sg_shoes_heels" to SizeGuide(
            id = "sg_shoes_heels",
            name = "Shoes & Strappy Heels Size Guide",
            categoryType = SizeGuideCategoryType.SHOES_HEELS.name,
            description = "Structured shoe sizing guide covering heels, occasion courts, and luxury sandals.",
            measurementUnitDefault = "CM",
            columns = listOf("EU Size", "UK Size", "US Women", "Foot Length (cm)", "Width Guide"),
            rows = listOf(
                SizeGuideRow("36", listOf("3", "5.5", "22.6", "Standard")),
                SizeGuideRow("37", listOf("4", "6.5", "23.3", "Standard")),
                SizeGuideRow("38", listOf("5", "7.5", "24.0", "Standard")),
                SizeGuideRow("39", listOf("6", "8.5", "24.7", "Standard")),
                SizeGuideRow("40", listOf("7", "9.5", "25.4", "Balanced vamp")),
                SizeGuideRow("41", listOf("7.5", "10", "26.0", "Balanced vamp")),
                SizeGuideRow("42", listOf("8.5", "11", "26.7", "Generous toe box")),
                SizeGuideRow("43", listOf("9.5", "12", "27.4", "Generous toe box")),
            ),
            howToMeasureSteps = listOf(
                "Measure your feet in the late afternoon when your feet are naturally slightly wider.",
                "Stand barefoot on paper with your heel anchored gently to a wall.",
                "Measure from the back edge of the heel to the tip of your longest toe.",
                "If between two sizes for pointed-toe shoes or heels, pick the larger EU size.",
            ),
            fitNotes = "Cushioned leather footbeds soften after 1-2 wears while ankle straps adjust up to 1.5 cm.",
            assignedCategoryIds = listOf("cat_shoes"),
            sortOrder = 2L,
            active = true,
        ),
        "sg_abayas_bubu" to SizeGuide(
            id = "sg_abayas_bubu",
            name = "Abayas & Modest Gown Size Guide",
            categoryType = SizeGuideCategoryType.ABAYAS_BUBU.name,
            description = "Garment body & length guide for floor-length Abayas (52-60) with relaxed modest drape.",
            measurementUnitDefault = "CM",
            columns = listOf("Abaya Size", "Bust (cm)", "Waist (cm)", "Hip (cm)", "Shoulder (cm)", "Length (cm)"),
            rows = listOf(
                SizeGuideRow("52 (S)", listOf("102", "96", "112", "39", "132")),
                SizeGuideRow("54 (M)", listOf("108", "102", "118", "41", "137")),
                SizeGuideRow("56 (L)", listOf("116", "110", "124", "43", "142")),
                SizeGuideRow("58 (XL)", listOf("124", "118", "132", "45", "147")),
                SizeGuideRow("60 (XXL)", listOf("132", "126", "140", "47", "152")),
            ),
            howToMeasureSteps = listOf(
                "Bust: Wrap a soft measuring tape horizontally around the fullest part of your chest.",
                "Waist: Measure around the natural narrowest waistline without pulling the tape tight.",
                "Hip: Measure around the fullest part of the hips with feet together.",
                "Shoulder & Length: Measure across the top shoulder seam and straight down from high shoulder to your ankle with preferred heels on.",
            ),
            fitNotes = "Abayas follow standard Emirati length numbering (52-60). Customers 5'3\"–5'5\" typically wear 54; 5'6\"–5'8\" wear 56 or 58.",
            assignedCategoryIds = listOf("cat_abayas"),
            sortOrder = 3L,
            active = true,
        ),
        "sg_adire_kampala_clothing" to SizeGuide(
            id = "sg_adire_kampala_clothing",
            name = "Bubu, Adire & Kampala Size Guide",
            categoryType = SizeGuideCategoryType.CLOTHING_KAFTAN.name,
            description = "Measurements for artisanal Bubu kaftans, Adire silk sets, and Kampala tailored clothing.",
            measurementUnitDefault = "CM",
            columns = listOf("Size", "Bust (cm)", "Waist (cm)", "Hip (cm)", "Shoulder (cm)", "Length (cm)"),
            rows = listOf(
                SizeGuideRow("S / UK 8-10", listOf("88 - 94", "72 - 78", "96 - 102", "38", "136")),
                SizeGuideRow("M / UK 12", listOf("95 - 101", "79 - 85", "103 - 109", "40", "138")),
                SizeGuideRow("L / UK 14", listOf("102 - 110", "86 - 94", "110 - 118", "42", "140")),
                SizeGuideRow("XL / UK 16-18", listOf("111 - 122", "95 - 106", "119 - 130", "44", "142")),
                SizeGuideRow("S/M (Draped)", listOf("Up to 112", "Free", "Up to 120", "Dropped", "140")),
                SizeGuideRow("L/XL (Draped)", listOf("Up to 130", "Free", "Up to 138", "Dropped", "144")),
                SizeGuideRow("One Size", listOf("Up to 138", "Free", "Up to 146", "Dropped", "145")),
            ),
            howToMeasureSteps = listOf(
                "Bust: Keep arms relaxed at your sides and measure across the fullest chest circumference.",
                "Waist & Hip: Stand comfortably; Bubu and kaftan silhouettes provide generous hip ease.",
                "Shoulder: Measure between left and right shoulder bone tips (dropped shoulder for kimono cuts).",
                "Length: Measure vertically from collarbone/shoulder top to desired hemline.",
            ),
            fitNotes = "Our silk Bubu dresses are tailored with signature fluidity—size down if you prefer a closer drape.",
            assignedCategoryIds = listOf("cat_adire", "cat_kampala"),
            sortOrder = 4L,
            active = true,
        ),
    )

    fun starterById(guideId: String): SizeGuide? =
        starterGuides().firstOrNull { it.first == guideId }?.second
}

object SizeGuideResolver {
    private val footwearKeywords = listOf("shoe", "slipper", "sandal", "heel", "mule", "footwear", "slide")
    private val heelKeywords = listOf("shoe", "heel", "court", "stiletto", "pump")
    private val abayaKeywords = listOf("abaya", "hijab", "modest", "gown", "kimono", "sheila")
    private val clothingKeywords = listOf("bubu", "boubou", "adire", "kampala", "kaftan", "dress", "set", "skirt", "top", "shirt", "outfit", "wear", "clothing")

    fun resolveForProduct(
        product: Product,
        categories: List<Category>,
        guides: List<SizeGuide>,
    ): SizeGuide? {
        val activeGuides = guides.filter { it.active }
        val guideById = activeGuides.associateBy { it.id }

        // 1. Direct product assignment
        if (product.sizeGuideId.isNotBlank()) {
            guideById[product.sizeGuideId]?.let { return it }
            DefaultSizeGuides.starterById(product.sizeGuideId)?.let { return it }
        }
        activeGuides.firstOrNull { product.id in it.assignedProductIds }?.let { return it }

        // 2. Category or Parent Category assignment
        val categoryMap = categories.associateBy { it.id }
        val category = categoryMap[product.categoryId]
        val parentCategory = category?.parentCategoryId?.takeIf { it.isNotBlank() }?.let { categoryMap[it] }

        listOfNotNull(category, parentCategory).forEach { cat ->
            if (cat.sizeGuideId.isNotBlank()) {
                guideById[cat.sizeGuideId]?.let { return it }
                DefaultSizeGuides.starterById(cat.sizeGuideId)?.let { return it }
            }
            activeGuides.firstOrNull { cat.id in it.assignedCategoryIds }?.let { return it }
        }

        // 3. Semantic / category keyword fallback for fashion apparel & footwear
        val haystack = buildString {
            append(product.title).append(' ')
            append(product.slug).append(' ')
            append(product.categoryId).append(' ')
            append(category?.name.orEmpty()).append(' ')
            append(category?.slug.orEmpty()).append(' ')
            append(parentCategory?.name.orEmpty())
        }.lowercase(Locale.US)

        if (footwearKeywords.any { haystack.contains(it) }) {
            val preferredType = if (heelKeywords.any { haystack.contains(it) } && !haystack.contains("slipper")) {
                SizeGuideCategoryType.SHOES_HEELS
            } else {
                SizeGuideCategoryType.SLIPPERS_SANDALS
            }
            return activeGuides.firstOrNull { it.resolvedType == preferredType }
                ?: activeGuides.firstOrNull { it.resolvedType.isFootwear }
                ?: DefaultSizeGuides.starterGuides().first { it.second.resolvedType == preferredType }.second
        }

        if (abayaKeywords.any { haystack.contains(it) }) {
            return activeGuides.firstOrNull { it.resolvedType == SizeGuideCategoryType.ABAYAS_BUBU }
                ?: DefaultSizeGuides.starterById("sg_abayas_bubu")
        }

        if (clothingKeywords.any { haystack.contains(it) }) {
            return activeGuides.firstOrNull { it.resolvedType == SizeGuideCategoryType.CLOTHING_KAFTAN }
                ?: activeGuides.firstOrNull { it.resolvedType == SizeGuideCategoryType.ABAYAS_BUBU }
                ?: DefaultSizeGuides.starterById("sg_adire_kampala_clothing")
        }

        return null
    }
}
