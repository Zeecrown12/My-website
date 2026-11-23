"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OFFICIAL_WHATSAPP_URL = exports.OFFICIAL_WHATSAPP_DIGITS = exports.OFFICIAL_WHATSAPP_DISPLAY = void 0;
exports.normalizeBaseUrl = normalizeBaseUrl;
exports.escapeHtml = escapeHtml;
exports.escapeXml = escapeXml;
exports.safeJsonLdStringify = safeJsonLdStringify;
exports.sanitizeSlug = sanitizeSlug;
exports.isValidGa4Id = isValidGa4Id;
exports.resolveWhatsAppContact = resolveWhatsAppContact;
exports.normalizePathname = normalizePathname;
exports.isPrivateNoIndexPath = isPrivateNoIndexPath;
exports.resolvePublicImageUrl = resolvePublicImageUrl;
exports.buildCanonicalProductUrl = buildCanonicalProductUrl;
exports.buildCanonicalCategoryUrl = buildCanonicalCategoryUrl;
exports.buildCanonicalCollectionUrl = buildCanonicalCollectionUrl;
exports.buildProductJsonLd = buildProductJsonLd;
exports.buildOrganizationJsonLd = buildOrganizationJsonLd;
exports.buildBreadcrumbJsonLd = buildBreadcrumbJsonLd;
exports.buildCollectionPageJsonLd = buildCollectionPageJsonLd;
exports.buildSitemapXml = buildSitemapXml;
const PRIVATE_PREFIXES = [
    "/admin",
    "/account",
    "/bag",
    "/cart",
    "/checkout",
    "/auth",
    "/login",
    "/register",
    "/notifications",
    "/track",
    "/order",
    "/support",
    "/search",
];
function normalizeBaseUrl(raw) {
    const trimmed = String(raw || "").trim().replace(/\/+$/, "");
    if (!trimmed)
        return "https://zeecrownstore.com";
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed;
    }
    return `https://${trimmed}`;
}
function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}
function escapeXml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&apos;");
}
function safeJsonLdStringify(payload) {
    return JSON.stringify(payload)
        .replace(/</g, "\\u003c")
        .replace(/>/g, "\\u003e")
        .replace(/&/g, "\\u0026");
}
function sanitizeSlug(raw, fallbackPrefix = "zeecrown-piece") {
    const cleaned = String(raw || "")
        .toLowerCase()
        .replace(/&/g, " and ")
        .replace(/[^a-z0-9\s-]/g, "")
        .trim()
        .replace(/[\s-]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 100);
    return cleaned || fallbackPrefix;
}
function isValidGa4Id(raw) {
    const cleaned = String(raw || "").trim().toUpperCase();
    return /^G-[A-Z0-9]{4,20}$/.test(cleaned);
}
exports.OFFICIAL_WHATSAPP_DISPLAY = "+234 816 989 8763";
exports.OFFICIAL_WHATSAPP_DIGITS = "2348169898763";
exports.OFFICIAL_WHATSAPP_URL = "https://wa.me/2348169898763";
function resolveWhatsAppContact(raw) {
    const trimmed = String(raw || "").trim();
    const display = trimmed || exports.OFFICIAL_WHATSAPP_DISPLAY;
    const digitsOnly = trimmed.replace(/\D+/g, "");
    if (digitsOnly.length < 7) {
        return { display, url: exports.OFFICIAL_WHATSAPP_URL };
    }
    const normalized = digitsOnly.startsWith("0") && digitsOnly.length === 11
        ? `234${digitsOnly.slice(1)}`
        : digitsOnly;
    return {
        display,
        url: `https://wa.me/${normalized}`,
    };
}
function normalizePathname(rawPath) {
    const raw = String(rawPath || "/").split("?")[0].split("#")[0].trim();
    if (!raw || raw === "/")
        return "/";
    const withLeading = raw.startsWith("/") ? raw : `/${raw}`;
    return withLeading.length > 1 ? withLeading.replace(/\/+$/, "") : withLeading;
}
function isPrivateNoIndexPath(rawPath) {
    const path = normalizePathname(rawPath).toLowerCase();
    return PRIVATE_PREFIXES.some((prefix) => path === prefix || path.startsWith(`${prefix}/`));
}
function resolvePublicImageUrl(rawUrl, baseUrl) {
    const clean = String(rawUrl || "").trim();
    if (!clean || clean.startsWith("drawable://")) {
        return `${normalizeBaseUrl(baseUrl)}/assets/aistudio/zeecrown-logo_4fe4a7c4.jpg`;
    }
    if (clean.startsWith("http://") || clean.startsWith("https://")) {
        return clean;
    }
    if (clean.startsWith("/")) {
        return `${normalizeBaseUrl(baseUrl)}${clean}`;
    }
    return `${normalizeBaseUrl(baseUrl)}/assets/aistudio/zeecrown-logo_4fe4a7c4.jpg`;
}
function buildCanonicalProductUrl(baseUrl, product) {
    const normalized = normalizeBaseUrl(baseUrl);
    const identifier = sanitizeSlug(product.slug || product.id, product.id || "piece");
    return `${normalized}/product/${identifier}`;
}
function buildCanonicalCategoryUrl(baseUrl, category) {
    const normalized = normalizeBaseUrl(baseUrl);
    const identifier = sanitizeSlug(category.slug || category.id, category.id || "shop");
    return `${normalized}/category/${identifier}`;
}
function buildCanonicalCollectionUrl(baseUrl, collection) {
    const normalized = normalizeBaseUrl(baseUrl);
    const identifier = sanitizeSlug(collection.slug || collection.id, collection.id || "edit");
    return `${normalized}/collection/${identifier}`;
}
function buildProductJsonLd(params) {
    const { product, baseUrl, categoryName, inStock, visibleReviews } = params;
    if (product.active !== true) {
        return null;
    }
    const canonical = buildCanonicalProductUrl(baseUrl, product);
    const rawImages = [
        product.socialImageUrl,
        product.mainImageUrl,
        ...(product.imageUrls || []),
    ].filter((img) => typeof img === "string" && img.trim().length > 0);
    const images = Array.from(new Set(rawImages.map((img) => resolvePublicImageUrl(img, baseUrl))));
    if (images.length === 0) {
        images.push(resolvePublicImageUrl(undefined, baseUrl));
    }
    const approvedReviews = (visibleReviews || []).filter((rev) => rev.visible !== false && typeof rev.rating === "number" && rev.rating >= 1 && rev.rating <= 5);
    const priceNgn = typeof product.priceNgn === "number" && Number.isFinite(product.priceNgn)
        ? product.priceNgn
        : 0;
    const jsonLd = {
        "@context": "https://schema.org",
        "@type": "Product",
        name: product.title,
        description: (product.seoDescription || product.description || "").trim(),
        url: canonical,
        image: images,
        brand: {
            "@type": "Brand",
            name: (product.brand || "ZEECROWN STORE").trim(),
        },
        offers: {
            "@type": "Offer",
            url: canonical,
            priceCurrency: "NGN",
            price: priceNgn.toFixed(2),
            availability: inStock ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
            itemCondition: "https://schema.org/NewCondition",
        },
    };
    if (product.sku && product.sku.trim()) {
        jsonLd.sku = product.sku.trim();
    }
    if (categoryName && categoryName.trim()) {
        jsonLd.category = categoryName.trim();
    }
    if (approvedReviews.length > 0) {
        const avg = approvedReviews.reduce((acc, item) => acc + item.rating, 0) / approvedReviews.length;
        jsonLd.aggregateRating = {
            "@type": "AggregateRating",
            ratingValue: avg.toFixed(1),
            reviewCount: approvedReviews.length,
        };
    }
    return jsonLd;
}
function buildOrganizationJsonLd(params) {
    const normalized = normalizeBaseUrl(params.baseUrl);
    const sameAs = (params.socialUrls || [])
        .map((u) => u.trim())
        .filter((u) => u.startsWith("https://") || u.startsWith("http://"));
    const org = {
        "@context": "https://schema.org",
        "@type": "Organization",
        name: params.storeName || "ZEECROWN STORE",
        url: normalized,
        logo: resolvePublicImageUrl(params.logoUrl, normalized),
    };
    if (params.supportEmail && params.supportEmail.trim()) {
        org.email = params.supportEmail.trim();
    }
    if (sameAs.length > 0) {
        org.sameAs = sameAs;
    }
    return org;
}
function buildBreadcrumbJsonLd(items) {
    return {
        "@context": "https://schema.org",
        "@type": "BreadcrumbList",
        itemListElement: items.map((item, idx) => ({
            "@type": "ListItem",
            position: idx + 1,
            name: item.name,
            item: item.url,
        })),
    };
}
function buildCollectionPageJsonLd(params) {
    return {
        "@context": "https://schema.org",
        "@type": "CollectionPage",
        name: params.name,
        description: params.description,
        url: params.canonicalUrl,
        mainEntity: {
            "@type": "ItemList",
            numberOfItems: params.itemUrls.length,
            itemListElement: params.itemUrls.slice(0, 50).map((url, idx) => ({
                "@type": "ListItem",
                position: idx + 1,
                url,
            })),
        },
    };
}
function buildSitemapXml(entries) {
    const rows = entries
        .map((entry) => {
        const lines = [`  <url>`, `    <loc>${escapeXml(entry.loc)}</loc>`];
        if (entry.lastmod) {
            lines.push(`    <lastmod>${escapeXml(entry.lastmod)}</lastmod>`);
        }
        if (entry.changefreq) {
            lines.push(`    <changefreq>${escapeXml(entry.changefreq)}</changefreq>`);
        }
        if (entry.priority) {
            lines.push(`    <priority>${escapeXml(entry.priority)}</priority>`);
        }
        lines.push(`  </url>`);
        return lines.join("\n");
    })
        .join("\n");
    return [
        `<?xml version="1.0" encoding="UTF-8"?>`,
        `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">`,
        rows,
        `</urlset>`,
    ].join("\n");
}
//# sourceMappingURL=seoUtils.js.map