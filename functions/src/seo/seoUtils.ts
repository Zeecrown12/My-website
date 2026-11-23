export interface SeoProductInput {
  id: string;
  title: string;
  slug?: string;
  description?: string;
  seoTitle?: string;
  seoDescription?: string;
  mainImageUrl?: string;
  socialImageUrl?: string;
  imageUrls?: string[];
  sku?: string;
  brand?: string;
  priceNgn?: number;
  priceUsd?: number;
  active?: boolean;
}

export interface SeoCategoryInput {
  id: string;
  name: string;
  slug?: string;
  description?: string;
  seoTitle?: string;
  seoDescription?: string;
  socialImageUrl?: string;
  imageKey?: string;
  active?: boolean;
}

export interface SeoCollectionInput {
  id: string;
  name: string;
  slug?: string;
  tagline?: string;
  seoTitle?: string;
  seoDescription?: string;
  socialImageUrl?: string;
  bannerKey?: string;
  active?: boolean;
}

export interface VisibleReviewRating {
  rating: number;
  visible?: boolean;
}

export interface SitemapEntry {
  loc: string;
  lastmod?: string;
  changefreq?: "daily" | "weekly" | "monthly";
  priority?: string;
}

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

export function normalizeBaseUrl(raw?: string): string {
  const trimmed = String(raw || "").trim().replace(/\/+$/, "");
  if (!trimmed) return "https://zeecrownstore.com";
  if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    return trimmed;
  }
  return `https://${trimmed}`;
}

export function escapeHtml(value: string): string {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function escapeXml(value: string): string {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

export function safeJsonLdStringify(payload: unknown): string {
  return JSON.stringify(payload)
    .replace(/</g, "\\u003c")
    .replace(/>/g, "\\u003e")
    .replace(/&/g, "\\u0026");
}

export function sanitizeSlug(raw?: string, fallbackPrefix = "zeecrown-piece"): string {
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

export function isValidGa4Id(raw?: string): boolean {
  const cleaned = String(raw || "").trim().toUpperCase();
  return /^G-[A-Z0-9]{4,20}$/.test(cleaned);
}

export const OFFICIAL_WHATSAPP_DISPLAY = "+234 816 989 8763";
export const OFFICIAL_WHATSAPP_DIGITS = "2348169898763";
export const OFFICIAL_WHATSAPP_URL = "https://wa.me/2348169898763";

export function resolveWhatsAppContact(raw?: string): { display: string; url: string } {
  const trimmed = String(raw || "").trim();
  const display = trimmed || OFFICIAL_WHATSAPP_DISPLAY;
  const digitsOnly = trimmed.replace(/\D+/g, "");
  if (digitsOnly.length < 7) {
    return { display, url: OFFICIAL_WHATSAPP_URL };
  }
  const normalized =
    digitsOnly.startsWith("0") && digitsOnly.length === 11
      ? `234${digitsOnly.slice(1)}`
      : digitsOnly;
  return {
    display,
    url: `https://wa.me/${normalized}`,
  };
}

export function normalizePathname(rawPath?: string): string {
  const raw = String(rawPath || "/").split("?")[0].split("#")[0].trim();
  if (!raw || raw === "/") return "/";
  const withLeading = raw.startsWith("/") ? raw : `/${raw}`;
  return withLeading.length > 1 ? withLeading.replace(/\/+$/, "") : withLeading;
}

export function isPrivateNoIndexPath(rawPath?: string): boolean {
  const path = normalizePathname(rawPath).toLowerCase();
  return PRIVATE_PREFIXES.some(
    (prefix) => path === prefix || path.startsWith(`${prefix}/`)
  );
}

export function resolvePublicImageUrl(rawUrl: string | undefined, baseUrl: string): string {
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

export function buildCanonicalProductUrl(baseUrl: string, product: SeoProductInput): string {
  const normalized = normalizeBaseUrl(baseUrl);
  const identifier = sanitizeSlug(product.slug || product.id, product.id || "piece");
  return `${normalized}/product/${identifier}`;
}

export function buildCanonicalCategoryUrl(baseUrl: string, category: SeoCategoryInput): string {
  const normalized = normalizeBaseUrl(baseUrl);
  const identifier = sanitizeSlug(category.slug || category.id, category.id || "shop");
  return `${normalized}/category/${identifier}`;
}

export function buildCanonicalCollectionUrl(
  baseUrl: string,
  collection: SeoCollectionInput
): string {
  const normalized = normalizeBaseUrl(baseUrl);
  const identifier = sanitizeSlug(collection.slug || collection.id, collection.id || "edit");
  return `${normalized}/collection/${identifier}`;
}

export function buildProductJsonLd(params: {
  product: SeoProductInput;
  baseUrl: string;
  categoryName?: string;
  inStock: boolean;
  visibleReviews: VisibleReviewRating[];
}): Record<string, unknown> | null {
  const { product, baseUrl, categoryName, inStock, visibleReviews } = params;
  if (product.active !== true) {
    return null;
  }

  const canonical = buildCanonicalProductUrl(baseUrl, product);
  const rawImages = [
    product.socialImageUrl,
    product.mainImageUrl,
    ...(product.imageUrls || []),
  ].filter((img): img is string => typeof img === "string" && img.trim().length > 0);

  const images = Array.from(
    new Set(rawImages.map((img) => resolvePublicImageUrl(img, baseUrl)))
  );
  if (images.length === 0) {
    images.push(resolvePublicImageUrl(undefined, baseUrl));
  }

  const approvedReviews = (visibleReviews || []).filter(
    (rev) => rev.visible !== false && typeof rev.rating === "number" && rev.rating >= 1 && rev.rating <= 5
  );

  const priceNgn = typeof product.priceNgn === "number" && Number.isFinite(product.priceNgn)
    ? product.priceNgn
    : 0;

  const jsonLd: Record<string, unknown> = {
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
    const avg =
      approvedReviews.reduce((acc, item) => acc + item.rating, 0) / approvedReviews.length;
    jsonLd.aggregateRating = {
      "@type": "AggregateRating",
      ratingValue: avg.toFixed(1),
      reviewCount: approvedReviews.length,
    };
  }

  return jsonLd;
}

export function buildOrganizationJsonLd(params: {
  storeName: string;
  baseUrl: string;
  logoUrl?: string;
  supportEmail?: string;
  socialUrls?: string[];
}): Record<string, unknown> {
  const normalized = normalizeBaseUrl(params.baseUrl);
  const sameAs = (params.socialUrls || [])
    .map((u) => u.trim())
    .filter((u) => u.startsWith("https://") || u.startsWith("http://"));

  const org: Record<string, unknown> = {
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

export function buildBreadcrumbJsonLd(
  items: Array<{ name: string; url: string }>
): Record<string, unknown> {
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

export function buildCollectionPageJsonLd(params: {
  name: string;
  description: string;
  canonicalUrl: string;
  itemUrls: string[];
}): Record<string, unknown> {
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

export function buildSitemapXml(entries: SitemapEntry[]): string {
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
