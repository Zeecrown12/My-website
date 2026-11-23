const { test } = require("node:test");
const assert = require("node:assert/strict");
const {
  buildBreadcrumbJsonLd,
  buildCanonicalCategoryUrl,
  buildCanonicalCollectionUrl,
  buildCanonicalProductUrl,
  buildProductJsonLd,
  buildSitemapXml,
  escapeHtml,
  isPrivateNoIndexPath,
  isValidGa4Id,
  normalizeBaseUrl,
  safeJsonLdStringify,
  sanitizeSlug,
} = require("../lib/seo/seoUtils");

test("normalizeBaseUrl and sanitizeSlug produce clean canonical URLs", () => {
  assert.equal(normalizeBaseUrl(""), "https://zeecrownstore.com");
  assert.equal(normalizeBaseUrl("zeecrownstore.com/"), "https://zeecrownstore.com");
  assert.equal(sanitizeSlug("Zaria & Silk Boubou!"), "zaria-and-silk-boubou");
  assert.equal(
    buildCanonicalProductUrl("zeecrownstore.com", { id: "p1", title: "Silk", slug: "zaria-silk" }),
    "https://zeecrownstore.com/product/zaria-silk"
  );
  assert.equal(
    buildCanonicalCategoryUrl("https://zeecrownstore.com", { id: "c1", name: "Abayas", slug: "abayas" }),
    "https://zeecrownstore.com/category/abayas"
  );
  assert.equal(
    buildCanonicalCollectionUrl("https://zeecrownstore.com", { id: "col1", name: "Edit", slug: "edit" }),
    "https://zeecrownstore.com/collection/edit"
  );
});

test("isPrivateNoIndexPath detects account, checkout, search, tracking, and admin routes", () => {
  assert.equal(isPrivateNoIndexPath("/admin"), true);
  assert.equal(isPrivateNoIndexPath("/admin/orders"), true);
  assert.equal(isPrivateNoIndexPath("/checkout"), true);
  assert.equal(isPrivateNoIndexPath("/account/orders"), true);
  assert.equal(isPrivateNoIndexPath("/track/ZC-101"), true);
  assert.equal(isPrivateNoIndexPath("/search?q=abaya"), true);
  assert.equal(isPrivateNoIndexPath("/"), false);
  assert.equal(isPrivateNoIndexPath("/shop"), false);
  assert.equal(isPrivateNoIndexPath("/product/zaria-silk"), false);
});

test("buildProductJsonLd rejects inactive products and never fabricates ratings", () => {
  const inactive = buildProductJsonLd({
    product: { id: "p0", title: "Draft", active: false, priceNgn: 1000 },
    baseUrl: "https://zeecrownstore.com",
    inStock: true,
    visibleReviews: [{ rating: 5, visible: true }],
  });
  assert.equal(inactive, null);

  const noReviews = buildProductJsonLd({
    product: {
      id: "p1",
      title: "Adire Kimono",
      slug: "adire-kimono",
      description: "Lagos luxury.",
      priceNgn: 54000,
      sku: "ZC-90",
      active: true,
    },
    baseUrl: "https://zeecrownstore.com",
    categoryName: "Adire Silk",
    inStock: false,
    visibleReviews: [],
  });
  assert.ok(noReviews);
  assert.equal(noReviews.offers.availability, "https://schema.org/OutOfStock");
  assert.equal(noReviews.aggregateRating, undefined);

  const withReviews = buildProductJsonLd({
    product: { id: "p1", title: "Adire Kimono", slug: "adire-kimono", priceNgn: 54000, active: true },
    baseUrl: "https://zeecrownstore.com",
    inStock: true,
    visibleReviews: [
      { rating: 5, visible: true },
      { rating: 4, visible: true },
      { rating: 1, visible: false },
    ],
  });
  assert.deepEqual(withReviews.aggregateRating, {
    "@type": "AggregateRating",
    ratingValue: "4.5",
    reviewCount: 2,
  });
});

test("escaping, GA4 ID check, breadcrumb LD, and XML sitemap generation work safely", () => {
  assert.equal(isValidGa4Id("G-ZEECROWN1"), true);
  assert.equal(isValidGa4Id("UA-1234"), false);
  assert.equal(escapeHtml("<script>alert('x')</script>"), "&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");

  const ld = safeJsonLdStringify(buildBreadcrumbJsonLd([{ name: "Home</script>", url: "https://zeecrownstore.com/" }]));
  assert.equal(ld.includes("</script>"), false);

  const xml = buildSitemapXml([
    { loc: "https://zeecrownstore.com/", changefreq: "daily", priority: "1.0" },
    { loc: "https://zeecrownstore.com/product/a&b", lastmod: "2026-11-22", priority: "0.85" },
  ]);
  assert.ok(xml.startsWith('<?xml version="1.0" encoding="UTF-8"?>'));
  assert.ok(xml.includes("<loc>https://zeecrownstore.com/product/a&amp;b</loc>"));
  assert.ok(xml.includes("<lastmod>2026-11-22</lastmod>"));
});
