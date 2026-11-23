"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.STARTER_WEB_SIZE_GUIDES = void 0;
exports.mapSizeGuideDoc = mapSizeGuideDoc;
exports.resolveSizeGuideForWebProduct = resolveSizeGuideForWebProduct;
exports.renderSizeGuideHtml = renderSizeGuideHtml;
const seoUtils_1 = require("./seoUtils");
const FOOTWEAR_KEYWORDS = ["shoe", "slipper", "sandal", "heel", "mule", "footwear", "slide"];
const HEEL_KEYWORDS = ["shoe", "heel", "court", "stiletto", "pump"];
const ABAYA_KEYWORDS = ["abaya", "hijab", "modest", "gown", "kimono", "sheila"];
const CLOTHING_KEYWORDS = [
    "bubu",
    "boubou",
    "adire",
    "kampala",
    "kaftan",
    "dress",
    "set",
    "skirt",
    "top",
    "shirt",
    "outfit",
    "wear",
    "clothing",
];
exports.STARTER_WEB_SIZE_GUIDES = [
    {
        id: "sg_slippers_sandals",
        name: "Slippers & Sandals Size Guide",
        categoryType: "SLIPPERS_SANDALS",
        description: "Exact EU/UK/US conversion and foot-length chart for ZEECROWN artisanal leather slippers and sandals.",
        measurementUnitDefault: "CM",
        columns: ["EU Size", "UK Size", "US Women", "Foot Length (cm)", "Fit Note"],
        rows: [
            { sizeLabel: "36", values: ["3", "5.5", "22.8", "True to size"] },
            { sizeLabel: "37", values: ["4", "6.5", "23.5", "True to size"] },
            { sizeLabel: "38", values: ["5", "7.5", "24.1", "True to size"] },
            { sizeLabel: "39", values: ["6", "8.5", "24.8", "True to size"] },
            { sizeLabel: "40", values: ["7", "9.5", "25.5", "Size up if wide fit"] },
            { sizeLabel: "41", values: ["7.5", "10", "26.2", "Size up if wide fit"] },
            { sizeLabel: "42", values: ["8.5", "11", "26.8", "Comfort room toe"] },
            { sizeLabel: "43", values: ["9.5", "12", "27.5", "Extended sole"] },
        ],
        howToMeasureSteps: [
            "Place a blank sheet of A4 paper flat against a straight wall on a hard floor.",
            "Stand evenly on the paper with your heel lightly touching the wall.",
            "Mark the tip of your longest toe with a pencil, then measure heel-to-mark with a ruler.",
            "Compare the longest foot measurement in cm to the Foot Length column above.",
        ],
        fitNotes: "For broad feet or high insteps in backless mules and palm slippers, we recommend selecting one EU size up.",
        assignedCategoryIds: ["cat_slippers"],
        assignedProductIds: [],
        sortOrder: 1,
        active: true,
    },
    {
        id: "sg_shoes_heels",
        name: "Shoes & Strappy Heels Size Guide",
        categoryType: "SHOES_HEELS",
        description: "Structured shoe sizing guide covering heels, occasion courts, and luxury sandals.",
        measurementUnitDefault: "CM",
        columns: ["EU Size", "UK Size", "US Women", "Foot Length (cm)", "Width Guide"],
        rows: [
            { sizeLabel: "36", values: ["3", "5.5", "22.6", "Standard"] },
            { sizeLabel: "37", values: ["4", "6.5", "23.3", "Standard"] },
            { sizeLabel: "38", values: ["5", "7.5", "24.0", "Standard"] },
            { sizeLabel: "39", values: ["6", "8.5", "24.7", "Standard"] },
            { sizeLabel: "40", values: ["7", "9.5", "25.4", "Balanced vamp"] },
            { sizeLabel: "41", values: ["7.5", "10", "26.0", "Balanced vamp"] },
            { sizeLabel: "42", values: ["8.5", "11", "26.7", "Generous toe box"] },
            { sizeLabel: "43", values: ["9.5", "12", "27.4", "Generous toe box"] },
        ],
        howToMeasureSteps: [
            "Measure your feet in the late afternoon when your feet are naturally slightly wider.",
            "Stand barefoot on paper with your heel anchored gently to a wall.",
            "Measure from the back edge of the heel to the tip of your longest toe.",
            "If between two sizes for pointed-toe shoes or heels, pick the larger EU size.",
        ],
        fitNotes: "Cushioned leather footbeds soften after 1-2 wears while ankle straps adjust up to 1.5 cm.",
        assignedCategoryIds: ["cat_shoes"],
        assignedProductIds: [],
        sortOrder: 2,
        active: true,
    },
    {
        id: "sg_abayas_bubu",
        name: "Abayas & Modest Gown Size Guide",
        categoryType: "ABAYAS_BUBU",
        description: "Garment body & length guide for floor-length Abayas (52-60) with relaxed modest drape.",
        measurementUnitDefault: "CM",
        columns: ["Abaya Size", "Bust (cm)", "Waist (cm)", "Hip (cm)", "Shoulder (cm)", "Length (cm)"],
        rows: [
            { sizeLabel: "52 (S)", values: ["102", "96", "112", "39", "132"] },
            { sizeLabel: "54 (M)", values: ["108", "102", "118", "41", "137"] },
            { sizeLabel: "56 (L)", values: ["116", "110", "124", "43", "142"] },
            { sizeLabel: "58 (XL)", values: ["124", "118", "132", "45", "147"] },
            { sizeLabel: "60 (XXL)", values: ["132", "126", "140", "47", "152"] },
        ],
        howToMeasureSteps: [
            "Bust: Wrap a soft measuring tape horizontally around the fullest part of your chest.",
            "Waist: Measure around the natural narrowest waistline without pulling the tape tight.",
            "Hip: Measure around the fullest part of the hips with feet together.",
            "Shoulder & Length: Measure across the top shoulder seam and straight down from high shoulder to your ankle with preferred heels on.",
        ],
        fitNotes: "Abayas follow standard Emirati length numbering (52-60). Customers 5'3\"-5'5\" typically wear 54; 5'6\"-5'8\" wear 56 or 58.",
        assignedCategoryIds: ["cat_abayas"],
        assignedProductIds: [],
        sortOrder: 3,
        active: true,
    },
    {
        id: "sg_adire_kampala_clothing",
        name: "Bubu, Adire & Kampala Size Guide",
        categoryType: "CLOTHING_KAFTAN",
        description: "Measurements for artisanal Bubu kaftans, Adire silk sets, and Kampala tailored clothing.",
        measurementUnitDefault: "CM",
        columns: ["Size", "Bust (cm)", "Waist (cm)", "Hip (cm)", "Shoulder (cm)", "Length (cm)"],
        rows: [
            { sizeLabel: "S / UK 8-10", values: ["88 - 94", "72 - 78", "96 - 102", "38", "136"] },
            { sizeLabel: "M / UK 12", values: ["95 - 101", "79 - 85", "103 - 109", "40", "138"] },
            { sizeLabel: "L / UK 14", values: ["102 - 110", "86 - 94", "110 - 118", "42", "140"] },
            { sizeLabel: "XL / UK 16-18", values: ["111 - 122", "95 - 106", "119 - 130", "44", "142"] },
            { sizeLabel: "S/M (Draped)", values: ["Up to 112", "Free", "Up to 120", "Dropped", "140"] },
            { sizeLabel: "L/XL (Draped)", values: ["Up to 130", "Free", "Up to 138", "Dropped", "144"] },
            { sizeLabel: "One Size", values: ["Up to 138", "Free", "Up to 146", "Dropped", "145"] },
        ],
        howToMeasureSteps: [
            "Bust: Keep arms relaxed at your sides and measure across the fullest chest circumference.",
            "Waist & Hip: Stand comfortably; Bubu and kaftan silhouettes provide generous hip ease.",
            "Shoulder: Measure between left and right shoulder bone tips (dropped shoulder for kimono cuts).",
            "Length: Measure vertically from collarbone/shoulder top to desired hemline.",
        ],
        fitNotes: "Our silk Bubu dresses are tailored with signature fluidity—size down if you prefer a closer drape.",
        assignedCategoryIds: ["cat_adire", "cat_kampala"],
        assignedProductIds: [],
        sortOrder: 4,
        active: true,
    },
];
function mapSizeGuideDoc(id, d) {
    const columns = Array.isArray(d.columns)
        ? d.columns.filter((x) => typeof x === "string")
        : [];
    const rows = Array.isArray(d.rows)
        ? d.rows
            .filter((r) => Boolean(r) && typeof r === "object")
            .map((r) => ({
            sizeLabel: String(r.sizeLabel || ""),
            values: Array.isArray(r.values)
                ? r.values.map((v) => String(v ?? ""))
                : [],
        }))
        : [];
    const howToMeasureSteps = Array.isArray(d.howToMeasureSteps)
        ? d.howToMeasureSteps.filter((x) => typeof x === "string")
        : [];
    const assignedCategoryIds = Array.isArray(d.assignedCategoryIds)
        ? d.assignedCategoryIds.filter((x) => typeof x === "string")
        : [];
    const assignedProductIds = Array.isArray(d.assignedProductIds)
        ? d.assignedProductIds.filter((x) => typeof x === "string")
        : [];
    return {
        id,
        name: String(d.name || "Boutique Size Guide"),
        categoryType: String(d.categoryType || "ABAYAS_BUBU"),
        description: String(d.description || ""),
        measurementUnitDefault: String(d.measurementUnitDefault || "CM"),
        columns,
        rows,
        howToMeasureSteps,
        fitNotes: String(d.fitNotes || ""),
        assignedCategoryIds,
        assignedProductIds,
        sortOrder: typeof d.sortOrder === "number" ? d.sortOrder : 0,
        active: d.active !== false,
    };
}
function resolveSizeGuideForWebProduct(params) {
    const activeGuides = params.guides
        .filter((g) => g.active)
        .sort((a, b) => a.sortOrder - b.sortOrder);
    const byId = new Map(activeGuides.map((g) => [g.id, g]));
    const starterById = (id) => exports.STARTER_WEB_SIZE_GUIDES.find((g) => g.id === id) || null;
    if (params.productSizeGuideId) {
        const direct = byId.get(params.productSizeGuideId) || starterById(params.productSizeGuideId);
        if (direct)
            return direct;
    }
    const byAssignedProd = activeGuides.find((g) => g.assignedProductIds.includes(params.productId));
    if (byAssignedProd)
        return byAssignedProd;
    if (params.categorySizeGuideId) {
        const catDirect = byId.get(params.categorySizeGuideId) || starterById(params.categorySizeGuideId);
        if (catDirect)
            return catDirect;
    }
    if (params.categoryId) {
        const byAssignedCat = activeGuides.find((g) => g.assignedCategoryIds.includes(params.categoryId || ""));
        if (byAssignedCat)
            return byAssignedCat;
    }
    const haystack = [
        params.productTitle,
        params.productSlug,
        params.categoryId || "",
        params.categoryName || "",
        params.categorySlug || "",
    ]
        .join(" ")
        .toLowerCase();
    if (FOOTWEAR_KEYWORDS.some((kw) => haystack.includes(kw))) {
        const preferredType = HEEL_KEYWORDS.some((kw) => haystack.includes(kw)) && !haystack.includes("slipper")
            ? "SHOES_HEELS"
            : "SLIPPERS_SANDALS";
        return (activeGuides.find((g) => g.categoryType === preferredType) ||
            activeGuides.find((g) => g.categoryType === "SLIPPERS_SANDALS" || g.categoryType === "SHOES_HEELS") ||
            exports.STARTER_WEB_SIZE_GUIDES.find((g) => g.categoryType === preferredType) ||
            null);
    }
    if (ABAYA_KEYWORDS.some((kw) => haystack.includes(kw))) {
        return (activeGuides.find((g) => g.categoryType === "ABAYAS_BUBU") ||
            starterById("sg_abayas_bubu"));
    }
    if (CLOTHING_KEYWORDS.some((kw) => haystack.includes(kw))) {
        return (activeGuides.find((g) => g.categoryType === "CLOTHING_KAFTAN") ||
            activeGuides.find((g) => g.categoryType === "ABAYAS_BUBU") ||
            starterById("sg_adire_kampala_clothing"));
    }
    return null;
}
function renderSizeGuideHtml(guide, waUrl, waDisplay, productTitle) {
    const cols = guide.columns.length > 0 ? guide.columns : ["Size", "Measurement"];
    const headerCells = cols.map((col) => `<th>${(0, seoUtils_1.escapeHtml)(col)}</th>`).join("");
    const bodyRows = guide.rows
        .map((row) => {
        const valueCells = cols
            .slice(1)
            .map((_, idx) => `<td>${(0, seoUtils_1.escapeHtml)(row.values[idx] || "-")}</td>`)
            .join("");
        return `<tr><th>${(0, seoUtils_1.escapeHtml)(row.sizeLabel)}</th>${valueCells}</tr>`;
    })
        .join("");
    const stepsHtml = guide.howToMeasureSteps.length > 0
        ? `<div class="sg-steps"><strong>How to Measure</strong><ol>${guide.howToMeasureSteps
            .map((step) => `<li>${(0, seoUtils_1.escapeHtml)(step)}</li>`)
            .join("")}</ol></div>`
        : "";
    const fitNoteHtml = guide.fitNotes
        ? `<p class="sg-note"><strong>Boutique Fit Guidance:</strong> ${(0, seoUtils_1.escapeHtml)(guide.fitNotes)}</p>`
        : "";
    const waSep = waUrl.includes("?") ? "&" : "?";
    const adviceHref = `${waUrl}${waSep}text=${encodeURIComponent(`Hello ZEECROWN STORE, I need sizing advice for ${productTitle} (${guide.name}).`)}`;
    return `<details id="size-guide" class="sg-box">
    <summary>Size Guide &amp; Measurement Chart — ${(0, seoUtils_1.escapeHtml)(guide.name)}</summary>
    ${guide.description ? `<p style="margin:10px 0;color:var(--muted);">${(0, seoUtils_1.escapeHtml)(guide.description)}</p>` : ""}
    <div style="overflow-x:auto;margin-top:12px;">
      <table class="sg-table">
        <thead><tr>${headerCells}</tr></thead>
        <tbody>${bodyRows}</tbody>
      </table>
    </div>
    ${stepsHtml}
    ${fitNoteHtml}
    <p style="margin-top:14px;">
      <a class="btn-wa" href="${(0, seoUtils_1.escapeHtml)(adviceHref)}" target="_blank" rel="noopener noreferrer">Ask Sizing Advice on WhatsApp (${(0, seoUtils_1.escapeHtml)(waDisplay)})</a>
    </p>
  </details>`;
}
//# sourceMappingURL=webSizeGuides.js.map