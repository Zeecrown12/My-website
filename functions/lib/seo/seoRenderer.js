"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleSeoPageRequest = handleSeoPageRequest;
const webWidget_1 = require("../ai/webWidget");
const seoUtils_1 = require("./seoUtils");
const webSizeGuides_1 = require("./webSizeGuides");
function formatNgn(amount) {
    return `₦${Math.round(amount).toLocaleString("en-NG")}`;
}
async function loadPublicSettings(db) {
    const snap = await db.collection("storeSettings").doc("public").get();
    const d = snap.data() || {};
    return {
        storeName: String(d.storeName || "ZEECROWN STORE"),
        tagline: String(d.tagline || "Style That Speaks. Elegance That Lasts."),
        supportEmail: String(d.supportEmail || ""),
        supportPhone: String(d.supportPhone || ""),
        whatsappNumber: String(d.whatsappNumber || seoUtils_1.OFFICIAL_WHATSAPP_DISPLAY),
        aboutUs: String(d.aboutText || ""),
        shippingPolicy: String(d.shippingPolicy || ""),
        returnsPolicy: String(d.returnPolicy || ""),
        privacyPolicy: String(d.privacyPolicy || ""),
        termsOfService: String(d.termsPolicy || ""),
        logoUrl: String(d.logoUrl || ""),
        publicSiteBaseUrl: (0, seoUtils_1.normalizeBaseUrl)(typeof d.publicSiteBaseUrl === "string" && d.publicSiteBaseUrl
            ? d.publicSiteBaseUrl
            : process.env.APP_BASE_URL || "https://zeecrownstore.com"),
        defaultSeoTitle: String(d.defaultSeoTitle || "ZEECROWN STORE | Luxury Nigerian & Modest Fashion Boutique"),
        defaultSeoDescription: String(d.defaultSeoDescription ||
            "Shop luxury Abayas, hand-dyed Adire silk pieces, leather slippers, and artisanal handbags with worldwide delivery from ZEECROWN STORE."),
        defaultSocialImageUrl: String(d.defaultSocialImageUrl || d.logoUrl || ""),
        instagramUrl: String(d.instagramUrl || ""),
        facebookUrl: String(d.facebookUrl || ""),
        pinterestUrl: String(d.pinterestUrl || ""),
        youtubeUrl: String(d.youtubeUrl || ""),
        twitterHandle: String(d.twitterHandle || ""),
        ga4MeasurementId: String(d.ga4MeasurementId || ""),
        analyticsConsentRequired: d.analyticsConsentRequired !== false,
        maintenanceMode: d.maintenanceMode === true,
    };
}
function buildAnalyticsHeadHtml(settings) {
    if (!(0, seoUtils_1.isValidGa4Id)(settings.ga4MeasurementId))
        return "";
    const id = (0, seoUtils_1.escapeHtml)(settings.ga4MeasurementId.trim().toUpperCase());
    const defaultStorage = settings.analyticsConsentRequired ? "denied" : "granted";
    return `
<script async src="https://www.googletagmanager.com/gtag/js?id=${id}"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('consent', 'default', { 'analytics_storage': '${defaultStorage}' });
  gtag('js', new Date());
  gtag('config', '${id}', { 'anonymize_ip': true });
  function zcGrantConsent() {
    try { localStorage.setItem('zc_ga_consent', 'granted'); } catch(e) {}
    gtag('consent', 'update', { 'analytics_storage': 'granted' });
    var banner = document.getElementById('zc-consent-banner');
    if (banner) banner.style.display = 'none';
  }
  function zcDeclineConsent() {
    try { localStorage.setItem('zc_ga_consent', 'denied'); } catch(e) {}
    var banner = document.getElementById('zc-consent-banner');
    if (banner) banner.style.display = 'none';
  }
  document.addEventListener('DOMContentLoaded', function() {
    if (!${settings.analyticsConsentRequired ? "true" : "false"}) return;
    try {
      var saved = localStorage.getItem('zc_ga_consent');
      if (saved === 'granted') {
        gtag('consent', 'update', { 'analytics_storage': 'granted' });
      } else if (!saved) {
        var b = document.getElementById('zc-consent-banner');
        if (b) b.style.display = 'flex';
      }
    } catch(e) {}
  });
</script>`;
}
function renderHtmlDocument(meta, settings) {
    const baseUrl = settings.publicSiteBaseUrl;
    const siteName = (0, seoUtils_1.escapeHtml)(settings.storeName);
    const twitterSite = settings.twitterHandle
        ? `<meta name="twitter:site" content="${(0, seoUtils_1.escapeHtml)(settings.twitterHandle.startsWith("@") ? settings.twitterHandle : `@${settings.twitterHandle}`)}">`
        : "";
    const ldScripts = meta.jsonLdBlocks
        .map((block) => `<script type="application/ld+json">${(0, seoUtils_1.safeJsonLdStringify)(block)}</script>`)
        .join("\n");
    const analyticsHead = meta.robots.startsWith("noindex") ? "" : buildAnalyticsHeadHtml(settings);
    const wa = (0, seoUtils_1.resolveWhatsAppContact)(settings.whatsappNumber);
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${(0, seoUtils_1.escapeHtml)(meta.title)}</title>
  <meta name="description" content="${(0, seoUtils_1.escapeHtml)(meta.description)}">
  <meta name="robots" content="${(0, seoUtils_1.escapeHtml)(meta.robots)}">
  <link rel="canonical" href="${(0, seoUtils_1.escapeHtml)(meta.canonicalUrl)}">
  <meta property="og:site_name" content="${siteName}">
  <meta property="og:title" content="${(0, seoUtils_1.escapeHtml)(meta.title)}">
  <meta property="og:description" content="${(0, seoUtils_1.escapeHtml)(meta.description)}">
  <meta property="og:type" content="${meta.ogType}">
  <meta property="og:url" content="${(0, seoUtils_1.escapeHtml)(meta.canonicalUrl)}">
  <meta property="og:image" content="${(0, seoUtils_1.escapeHtml)(meta.ogImage)}">
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="${(0, seoUtils_1.escapeHtml)(meta.title)}">
  <meta name="twitter:description" content="${(0, seoUtils_1.escapeHtml)(meta.description)}">
  <meta name="twitter:image" content="${(0, seoUtils_1.escapeHtml)(meta.ogImage)}">
  ${twitterSite}
  <style>
    :root { --bg:#FFFFFF; --surface:#FFFFFF; --lilac:#F4EFF9; --lavender:#E9DDF5; --ink:#1F122B; --muted:#5C4B6E; --rose:#C8A64D; --gold:#C8A64D; --gold-ink:#8B6914; --plum:#542080; --deep-plum:#351052; --border:#D9D9E0; --wa:#1F8A4D; }
    * { box-sizing:border-box; }
    body { margin:0; font-family: Georgia, "Times New Roman", serif; background:var(--bg); color:var(--ink); line-height:1.6; }
    header { background:var(--surface); border-bottom:1px solid var(--border); padding:16px 24px; display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; }
    .brand { font-weight:700; letter-spacing:0.14em; color:var(--plum); text-decoration:none; font-size:1.1rem; }
    nav { display:flex; align-items:center; flex-wrap:wrap; gap:14px; }
    nav a { color:var(--ink); text-decoration:none; font-family:system-ui,-apple-system,sans-serif; font-size:0.92rem; }
    nav a:hover { color:var(--plum); }
    nav a.nav-wa { background:var(--wa); color:#fff; padding:8px 14px; border-radius:999px; font-weight:600; font-size:0.84rem; }
    nav a.nav-wa:hover { background:#18703E; color:#fff; }
    main { max-width:1100px; margin:0 auto; padding:32px 20px 80px; font-family:system-ui,-apple-system,sans-serif; }
    h1, h2, h3 { font-family:Georgia, serif; color:var(--deep-plum); margin-top:0; }
    .grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(230px, 1fr)); gap:20px; margin-top:20px; }
    .card { background:var(--surface); border:1px solid var(--border); border-radius:16px; overflow:hidden; display:flex; flex-direction:column; }
    .card img { width:100%; aspect-ratio:4/5; object-fit:cover; background:var(--lilac); display:block; }
    .card-body { padding:14px; display:flex; flex-direction:column; gap:6px; }
    .card-title { font-weight:600; color:var(--ink); text-decoration:none; }
    .price { color:var(--plum); font-weight:700; }
    .btn { display:inline-block; background:var(--plum); color:#fff; padding:12px 22px; border-radius:999px; text-decoration:none; font-weight:600; font-size:0.95rem; border:1px solid var(--gold); }
    .btn:hover { background:var(--deep-plum); }
    .btn-outline { display:inline-block; border:1px solid var(--plum); color:var(--plum); padding:11px 20px; border-radius:999px; text-decoration:none; font-weight:600; }
    .btn-wa { display:inline-flex; align-items:center; gap:8px; background:var(--wa); color:#fff; padding:12px 22px; border-radius:999px; text-decoration:none; font-weight:600; font-size:0.94rem; }
    .crumbs { font-size:0.85rem; color:var(--muted); margin-bottom:16px; }
    .crumbs a { color:var(--muted); text-decoration:none; }
    .detail { display:grid; grid-template-columns:repeat(auto-fit, minmax(290px, 1fr)); gap:32px; align-items:start; }
    .detail img { width:100%; border-radius:20px; border:1px solid var(--border); aspect-ratio:4/5; object-fit:cover; }
    .sg-box { margin-top:24px; background:var(--surface); border:1px solid var(--border); border-radius:16px; padding:16px 18px; }
    .sg-box summary { cursor:pointer; font-weight:700; color:var(--plum); font-size:1rem; }
    .sg-table { width:100%; border-collapse:collapse; font-size:0.86rem; }
    .sg-table th, .sg-table td { border:1px solid var(--border); padding:8px 10px; text-align:left; }
    .sg-table thead th { background:var(--lilac); color:var(--deep-plum); }
    .sg-steps { margin-top:14px; font-size:0.88rem; }
    .sg-steps ol { margin:6px 0 0 18px; padding:0; }
    .sg-note { margin-top:12px; padding:10px 12px; background:var(--lilac); border-left:3px solid var(--gold); border-radius:6px; font-size:0.86rem; }
    .contact-card { background:var(--lilac); border:1px solid var(--border); border-radius:18px; padding:24px; max-width:680px; margin-top:16px; }
    footer { border-top:1px solid var(--border); padding:28px 24px 90px; text-align:center; font-family:system-ui,sans-serif; font-size:0.86rem; color:var(--muted); background:var(--lilac); }
    footer a { color:var(--plum); margin:0 10px; text-decoration:none; }
    .zc-wa-float { position:fixed; right:20px; bottom:74px; z-index:97; background:var(--wa); color:#FFFFFF; border:1px solid #C8A64D; border-radius:999px; padding:10px 18px; font-family:system-ui,-apple-system,sans-serif; font-weight:600; font-size:0.88rem; text-decoration:none; box-shadow:0 10px 26px rgba(53,16,82,0.22); display:inline-flex; align-items:center; gap:8px; }
    .zc-wa-float:hover { background:#18703E; }
    #zc-consent-banner { display:none; position:fixed; bottom:16px; left:16px; right:16px; max-width:680px; margin:0 auto; background:var(--surface); border:1px solid var(--border); border-radius:16px; padding:14px 18px; box-shadow:0 10px 30px rgba(31,18,43,0.12); align-items:center; justify-content:space-between; gap:12px; font-family:system-ui,sans-serif; font-size:0.88rem; z-index:99; }
    #zc-consent-banner button { border-radius:999px; padding:8px 16px; cursor:pointer; font-weight:600; border:1px solid var(--plum); }
  </style>
  ${ldScripts}
  ${analyticsHead}
</head>
<body>
  <header>
    <a class="brand" href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/">${siteName}</a>
    <nav aria-label="Primary navigation">
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/">Home</a>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shop">Shop</a>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/about">About</a>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/contact">Contact</a>
      <a class="nav-wa" href="${(0, seoUtils_1.escapeHtml)(wa.url)}" target="_blank" rel="noopener noreferrer">Chat with Us on WhatsApp</a>
    </nav>
  </header>
  <main>
    ${meta.bodyHtml}
  </main>
  <footer>
    <p>${siteName} — ${(0, seoUtils_1.escapeHtml)(settings.tagline)}</p>
    <p>
      <strong>Customer Support &amp; Contact Us:</strong>
      <a href="${(0, seoUtils_1.escapeHtml)(wa.url)}" target="_blank" rel="noopener noreferrer">WhatsApp: ${(0, seoUtils_1.escapeHtml)(wa.display)}</a>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/contact">Contact Us</a>
    </p>
    <p>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shipping-policy">Shipping</a>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/returns-policy">Returns</a>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/privacy-policy">Privacy</a>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/terms">Terms</a>
      <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/sitemap.xml">Sitemap</a>
    </p>
  </footer>
  ${!settings.maintenanceMode
        ? `<a class="zc-wa-float" href="${(0, seoUtils_1.escapeHtml)(wa.url)}" target="_blank" rel="noopener noreferrer" aria-label="Chat with ZEECROWN STORE on WhatsApp (${(0, seoUtils_1.escapeHtml)(wa.display)})">WhatsApp: ${(0, seoUtils_1.escapeHtml)(wa.display)}</a>`
        : ""}
  <div id="zc-consent-banner" role="dialog" aria-label="Analytics consent">
    <span>We use privacy-safe analytics to improve our collections and shopping experience.</span>
    <div style="display:flex;gap:8px;flex-shrink:0;">
      <button type="button" onclick="zcDeclineConsent()" style="background:#fff;color:var(--plum);">Decline</button>
      <button type="button" onclick="zcGrantConsent()" style="background:var(--plum);color:#fff;">Accept</button>
    </div>
  </div>
  ${!settings.maintenanceMode && !meta.robots.startsWith("noindex")
        ? (0, webWidget_1.buildAiWebWidgetHtml)({
            supportEmail: settings.supportEmail,
            supportPhone: settings.supportPhone,
            whatsappNumber: wa.display,
        })
        : ""}
</body>
</html>`;
}
function mapProductDoc(id, d) {
    return {
        id,
        title: String(d.title || "ZEECROWN Fashion Piece"),
        slug: typeof d.slug === "string" ? d.slug : id,
        description: typeof d.description === "string" ? d.description : "",
        seoTitle: typeof d.seoTitle === "string" ? d.seoTitle : "",
        seoDescription: typeof d.seoDescription === "string" ? d.seoDescription : "",
        mainImageUrl: typeof d.mainImageUrl === "string" ? d.mainImageUrl : "",
        socialImageUrl: typeof d.socialImageUrl === "string" ? d.socialImageUrl : "",
        imageUrls: Array.isArray(d.imageUrls)
            ? d.imageUrls.filter((x) => typeof x === "string")
            : [],
        sku: typeof d.sku === "string" ? d.sku : "",
        brand: typeof d.brand === "string" ? d.brand : "ZEECROWN STORE",
        priceNgn: typeof d.priceNgn === "number" ? d.priceNgn : 0,
        priceUsd: typeof d.priceUsd === "number" ? d.priceUsd : 0,
        active: d.active === true,
    };
}
function renderProductCardsHtml(products, baseUrl) {
    if (products.length === 0) {
        return `<p>No active pieces currently listed in this section.</p>`;
    }
    return `<div class="grid">${products
        .map((p) => {
        const url = (0, seoUtils_1.buildCanonicalProductUrl)(baseUrl, p);
        const img = (0, seoUtils_1.resolvePublicImageUrl)(p.mainImageUrl || p.imageUrls?.[0], baseUrl);
        return `<article class="card">
        <a href="${(0, seoUtils_1.escapeHtml)(url)}">
          <img src="${(0, seoUtils_1.escapeHtml)(img)}" alt="${(0, seoUtils_1.escapeHtml)(p.title)}" loading="lazy">
        </a>
        <div class="card-body">
          <a class="card-title" href="${(0, seoUtils_1.escapeHtml)(url)}">${(0, seoUtils_1.escapeHtml)(p.title)}</a>
          <span class="price">${(0, seoUtils_1.escapeHtml)(formatNgn(p.priceNgn || 0))}</span>
        </div>
      </article>`;
    })
        .join("")}</div>`;
}
function buildNotFoundPage(path, settings) {
    const baseUrl = settings.publicSiteBaseUrl;
    return {
        statusCode: 404,
        title: `Piece Not Found | ${settings.storeName}`,
        description: "The requested ZEECROWN STORE page or piece could not be found.",
        canonicalUrl: `${baseUrl}${path}`,
        ogType: "website",
        ogImage: (0, seoUtils_1.resolvePublicImageUrl)(settings.defaultSocialImageUrl, baseUrl),
        robots: "noindex, nofollow",
        jsonLdBlocks: [],
        cacheControl: "no-store",
        bodyHtml: `
      <h1>Page Not Found</h1>
      <p>The piece or boutique page you requested is unavailable or no longer published.</p>
      <p><a class="btn" href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shop">Explore Active Collection</a></p>
    `,
    };
}
async function handleSeoPageRequest(db, req, res) {
    try {
        const path = (0, seoUtils_1.normalizePathname)(req.path || "/");
        const settings = await loadPublicSettings(db);
        const baseUrl = settings.publicSiteBaseUrl;
        if (settings.maintenanceMode) {
            const html = renderHtmlDocument({
                statusCode: 503,
                title: `Boutique Maintenance | ${settings.storeName}`,
                description: `${settings.storeName} is undergoing scheduled curation and will return shortly.`,
                canonicalUrl: `${baseUrl}/`,
                ogType: "website",
                ogImage: (0, seoUtils_1.resolvePublicImageUrl)(settings.defaultSocialImageUrl, baseUrl),
                robots: "noindex, nofollow",
                jsonLdBlocks: [],
                cacheControl: "no-store",
                bodyHtml: `
            <h1>Scheduled Boutique Update</h1>
            <p>${(0, seoUtils_1.escapeHtml)(settings.storeName)} is refreshing our catalog. Please check back shortly.</p>
          `,
            }, settings);
            res.status(503)
                .set("Retry-After", "3600")
                .set("X-Robots-Tag", "noindex, nofollow")
                .set("Cache-Control", "no-store")
                .type("text/html; charset=utf-8")
                .send(html);
            return;
        }
        // Legacy plural route 301 redirects
        if (path.startsWith("/products/")) {
            res.redirect(301, `${baseUrl}/product/${path.slice("/products/".length)}`);
            return;
        }
        if (path.startsWith("/categories/")) {
            res.redirect(301, `${baseUrl}/category/${path.slice("/categories/".length)}`);
            return;
        }
        if (path.startsWith("/collections/")) {
            res.redirect(301, `${baseUrl}/collection/${path.slice("/collections/".length)}`);
            return;
        }
        // Private or operational routes must never be indexed
        if ((0, seoUtils_1.isPrivateNoIndexPath)(path)) {
            const html = renderHtmlDocument({
                statusCode: 200,
                title: `Account & Private Portal | ${settings.storeName}`,
                description: "Sign in via the ZEECROWN STORE Android app or customer portal.",
                canonicalUrl: `${baseUrl}/`,
                ogType: "website",
                ogImage: (0, seoUtils_1.resolvePublicImageUrl)(settings.defaultSocialImageUrl, baseUrl),
                robots: "noindex, nofollow",
                jsonLdBlocks: [],
                cacheControl: "no-store",
                bodyHtml: `
            <h1>Customer & Store Portal</h1>
            <p>Account, cart, checkout, order tracking, and admin views are protected and available in the ZEECROWN STORE app.</p>
            <p><a class="btn" href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shop">Continue Shopping</a></p>
          `,
            }, settings);
            res.status(200)
                .set("X-Robots-Tag", "noindex, nofollow")
                .set("Cache-Control", "no-store")
                .type("text/html; charset=utf-8")
                .send(html);
            return;
        }
        const socialUrls = [
            settings.instagramUrl,
            settings.facebookUrl,
            settings.pinterestUrl,
            settings.youtubeUrl,
        ].filter(Boolean);
        // Home route
        if (path === "/" || path === "/index.html") {
            const [catSnap, prodSnap] = await Promise.all([
                db.collection("categories").where("active", "==", true).orderBy("sortOrder", "asc").limit(8).get(),
                db.collection("products").where("active", "==", true).limit(12).get(),
            ]);
            const categories = catSnap.docs.map((doc) => ({
                id: doc.id,
                name: String(doc.data().name || ""),
                slug: String(doc.data().slug || doc.id),
            }));
            const products = prodSnap.docs.map((doc) => mapProductDoc(doc.id, doc.data()));
            const meta = {
                statusCode: 200,
                title: settings.defaultSeoTitle,
                description: settings.defaultSeoDescription,
                canonicalUrl: `${baseUrl}/`,
                ogType: "website",
                ogImage: (0, seoUtils_1.resolvePublicImageUrl)(settings.defaultSocialImageUrl, baseUrl),
                robots: "index, follow, max-image-preview:large",
                cacheControl: "public, max-age=120, s-maxage=300",
                jsonLdBlocks: [
                    (0, seoUtils_1.buildOrganizationJsonLd)({
                        storeName: settings.storeName,
                        baseUrl,
                        logoUrl: settings.logoUrl,
                        supportEmail: settings.supportEmail,
                        socialUrls,
                    }),
                    {
                        "@context": "https://schema.org",
                        "@type": "WebSite",
                        name: settings.storeName,
                        url: `${baseUrl}/`,
                        potentialAction: {
                            "@type": "SearchAction",
                            target: `${baseUrl}/shop?q={search_term_string}`,
                            "query-input": "required name=search_term_string",
                        },
                    },
                ],
                bodyHtml: `
          <section>
            <h1>${(0, seoUtils_1.escapeHtml)(settings.storeName)} — ${(0, seoUtils_1.escapeHtml)(settings.tagline)}</h1>
            <p>${(0, seoUtils_1.escapeHtml)(settings.defaultSeoDescription)}</p>
            <p><a class="btn" href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shop">Shop Full Collection</a></p>
          </section>
          ${categories.length > 0
                    ? `<section style="margin-top:32px;"><h2>Departments</h2><p>${categories
                        .map((c) => `<a class="btn-outline" style="margin:4px 8px 4px 0;" href="${(0, seoUtils_1.escapeHtml)((0, seoUtils_1.buildCanonicalCategoryUrl)(baseUrl, c))}">${(0, seoUtils_1.escapeHtml)(c.name)}</a>`)
                        .join("")}</p></section>`
                    : ""}
          <section style="margin-top:36px;">
            <h2>Curated Pieces</h2>
            ${renderProductCardsHtml(products, baseUrl)}
          </section>
        `,
            };
            res.status(200)
                .set("Cache-Control", meta.cacheControl)
                .type("text/html; charset=utf-8")
                .send(renderHtmlDocument(meta, settings));
            return;
        }
        // Shop route
        if (path === "/shop") {
            const prodSnap = await db.collection("products").where("active", "==", true).limit(48).get();
            const products = prodSnap.docs.map((doc) => mapProductDoc(doc.id, doc.data()));
            const canonical = `${baseUrl}/shop`;
            const meta = {
                statusCode: 200,
                title: `Shop All Pieces | ${settings.storeName}`,
                description: `Browse luxury Abayas, hand-dyed Adire silk pieces, leather slippers, and handbags from ${settings.storeName}.`,
                canonicalUrl: canonical,
                ogType: "website",
                ogImage: (0, seoUtils_1.resolvePublicImageUrl)(settings.defaultSocialImageUrl, baseUrl),
                robots: "index, follow, max-image-preview:large",
                cacheControl: "public, max-age=120, s-maxage=300",
                jsonLdBlocks: [
                    (0, seoUtils_1.buildCollectionPageJsonLd)({
                        name: `Shop ${settings.storeName}`,
                        description: `Active fashion catalog for ${settings.storeName}`,
                        canonicalUrl: canonical,
                        itemUrls: products.map((p) => (0, seoUtils_1.buildCanonicalProductUrl)(baseUrl, p)),
                    }),
                    (0, seoUtils_1.buildBreadcrumbJsonLd)([
                        { name: "Home", url: `${baseUrl}/` },
                        { name: "Shop", url: canonical },
                    ]),
                ],
                bodyHtml: `
          <div class="crumbs"><a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/">Home</a> / Shop</div>
          <h1>Shop All Pieces</h1>
          ${renderProductCardsHtml(products, baseUrl)}
        `,
            };
            res.status(200)
                .set("Cache-Control", meta.cacheControl)
                .type("text/html; charset=utf-8")
                .send(renderHtmlDocument(meta, settings));
            return;
        }
        // Product detail route: /product/:slugOrId
        if (path.startsWith("/product/")) {
            const slugOrId = decodeURIComponent(path.slice("/product/".length)).trim();
            if (!slugOrId) {
                const nf = buildNotFoundPage(path, settings);
                res.status(404).set("X-Robots-Tag", "noindex, nofollow").set("Cache-Control", "no-store")
                    .type("text/html; charset=utf-8").send(renderHtmlDocument(nf, settings));
                return;
            }
            let productDoc = await db.collection("products").doc(slugOrId).get();
            if (!productDoc.exists || productDoc.data()?.active !== true) {
                const slugQuery = await db
                    .collection("products")
                    .where("active", "==", true)
                    .where("slug", "==", slugOrId)
                    .limit(1)
                    .get();
                if (!slugQuery.empty) {
                    productDoc = slugQuery.docs[0];
                }
            }
            const rawData = productDoc.data();
            if (!productDoc.exists || !rawData || rawData.active !== true) {
                const nf = buildNotFoundPage(path, settings);
                res.status(404).set("X-Robots-Tag", "noindex, nofollow").set("Cache-Control", "no-store")
                    .type("text/html; charset=utf-8").send(renderHtmlDocument(nf, settings));
                return;
            }
            const product = mapProductDoc(productDoc.id, rawData);
            const categoryId = typeof rawData.categoryId === "string" ? rawData.categoryId : "";
            const [invSnap, revSnap, catSnap, sgSnap] = await Promise.all([
                db.collection("inventory").doc(product.id).get(),
                db.collection("reviews").where("productId", "==", product.id).where("visible", "==", true).get(),
                categoryId ? db.collection("categories").doc(categoryId).get() : Promise.resolve(null),
                db.collection("sizeGuides").where("active", "==", true).get(),
            ]);
            const stockQty = invSnap.exists && typeof invSnap.data()?.stockQuantity === "number"
                ? Number(invSnap.data()?.stockQuantity)
                : 1;
            const inStock = stockQty > 0;
            const categoryName = catSnap?.exists ? String(catSnap.data()?.name || "") : "";
            const categorySlug = catSnap?.exists ? String(catSnap.data()?.slug || "") : "";
            const categorySizeGuideId = catSnap?.exists && typeof catSnap.data()?.sizeGuideId === "string"
                ? String(catSnap.data()?.sizeGuideId)
                : "";
            const productSizeGuideId = typeof rawData.sizeGuideId === "string" ? String(rawData.sizeGuideId) : "";
            const webGuides = sgSnap.docs.map((doc) => (0, webSizeGuides_1.mapSizeGuideDoc)(doc.id, doc.data()));
            const activeSizeGuide = (0, webSizeGuides_1.resolveSizeGuideForWebProduct)({
                productId: product.id,
                productTitle: product.title,
                productSlug: product.slug || product.id,
                productSizeGuideId,
                categoryId,
                categoryName,
                categorySlug,
                categorySizeGuideId,
                guides: webGuides,
            });
            const wa = (0, seoUtils_1.resolveWhatsAppContact)(settings.whatsappNumber);
            const visibleReviews = revSnap.docs.map((doc) => ({
                rating: Number(doc.data().rating || 0),
                visible: doc.data().visible === true,
            }));
            const canonical = (0, seoUtils_1.buildCanonicalProductUrl)(baseUrl, product);
            const seoTitle = (product.seoTitle || `${product.title} | ${settings.storeName}`).trim();
            const seoDesc = (product.seoDescription || product.description || settings.defaultSeoDescription)
                .trim()
                .slice(0, 165);
            const ogImage = (0, seoUtils_1.resolvePublicImageUrl)(product.socialImageUrl || product.mainImageUrl || product.imageUrls?.[0], baseUrl);
            const jsonLdBlocks = [];
            const productLd = (0, seoUtils_1.buildProductJsonLd)({
                product,
                baseUrl,
                categoryName,
                inStock,
                visibleReviews,
            });
            if (productLd)
                jsonLdBlocks.push(productLd);
            jsonLdBlocks.push((0, seoUtils_1.buildBreadcrumbJsonLd)([
                { name: "Home", url: `${baseUrl}/` },
                { name: "Shop", url: `${baseUrl}/shop` },
                { name: product.title, url: canonical },
            ]));
            const meta = {
                statusCode: 200,
                title: seoTitle,
                description: seoDesc,
                canonicalUrl: canonical,
                ogType: "product",
                ogImage,
                robots: "index, follow, max-image-preview:large",
                cacheControl: "public, max-age=120, s-maxage=300",
                jsonLdBlocks,
                bodyHtml: `
          <div class="crumbs"><a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/">Home</a> / <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shop">Shop</a> / ${(0, seoUtils_1.escapeHtml)(product.title)}</div>
          <article class="detail">
            <img src="${(0, seoUtils_1.escapeHtml)(ogImage)}" alt="${(0, seoUtils_1.escapeHtml)(product.title)}">
            <div>
              <h1>${(0, seoUtils_1.escapeHtml)(product.title)}</h1>
              <p class="price" style="font-size:1.4rem;">${(0, seoUtils_1.escapeHtml)(formatNgn(product.priceNgn || 0))}</p>
              <p><strong>Availability:</strong> ${inStock ? "In Stock" : "Out of Stock"}</p>
              ${categoryName ? `<p><strong>Category:</strong> ${(0, seoUtils_1.escapeHtml)(categoryName)}</p>` : ""}
              <p>${(0, seoUtils_1.escapeHtml)(product.description || "")}</p>
              <div style="margin-top:20px;display:flex;flex-wrap:wrap;gap:10px;">
                <a class="btn" href="zeecrown://product/${(0, seoUtils_1.escapeHtml)(product.slug || product.id)}">Open in ZEECROWN App</a>
                ${activeSizeGuide ? `<a class="btn-outline" href="#size-guide">Size Guide</a>` : ""}
                <a class="btn-wa" href="${(0, seoUtils_1.escapeHtml)(wa.url)}" target="_blank" rel="noopener noreferrer">Order via WhatsApp</a>
              </div>
              ${activeSizeGuide ? (0, webSizeGuides_1.renderSizeGuideHtml)(activeSizeGuide, wa.url, wa.display, product.title) : ""}
            </div>
          </article>
        `,
            };
            res.status(200)
                .set("Cache-Control", meta.cacheControl)
                .type("text/html; charset=utf-8")
                .send(renderHtmlDocument(meta, settings));
            return;
        }
        // Category route: /category/:slugOrId
        if (path.startsWith("/category/")) {
            const slugOrId = decodeURIComponent(path.slice("/category/".length)).trim();
            let catDoc = slugOrId ? await db.collection("categories").doc(slugOrId).get() : null;
            if (!catDoc || !catDoc.exists || catDoc.data()?.active !== true) {
                const slugQuery = await db
                    .collection("categories")
                    .where("active", "==", true)
                    .where("slug", "==", slugOrId)
                    .limit(1)
                    .get();
                if (!slugQuery.empty) {
                    catDoc = slugQuery.docs[0];
                }
            }
            if (!catDoc || !catDoc.exists || catDoc.data()?.active !== true) {
                const nf = buildNotFoundPage(path, settings);
                res.status(404).set("X-Robots-Tag", "noindex, nofollow").set("Cache-Control", "no-store")
                    .type("text/html; charset=utf-8").send(renderHtmlDocument(nf, settings));
                return;
            }
            const d = catDoc.data() || {};
            const cat = {
                id: catDoc.id,
                name: String(d.name || "Collection"),
                slug: typeof d.slug === "string" ? d.slug : catDoc.id,
                description: typeof d.description === "string" ? d.description : "",
                seoTitle: typeof d.seoTitle === "string" ? d.seoTitle : "",
                seoDescription: typeof d.seoDescription === "string" ? d.seoDescription : "",
                socialImageUrl: typeof d.socialImageUrl === "string" ? d.socialImageUrl : "",
                imageKey: typeof d.imageKey === "string" ? d.imageKey : "",
                active: true,
            };
            const prodSnap = await db
                .collection("products")
                .where("active", "==", true)
                .where("categoryId", "==", cat.id)
                .limit(48)
                .get();
            const products = prodSnap.docs.map((doc) => mapProductDoc(doc.id, doc.data()));
            const canonical = (0, seoUtils_1.buildCanonicalCategoryUrl)(baseUrl, cat);
            const seoTitle = (cat.seoTitle || `${cat.name} | ${settings.storeName}`).trim();
            const seoDesc = (cat.seoDescription || cat.description || settings.defaultSeoDescription)
                .trim()
                .slice(0, 165);
            const meta = {
                statusCode: 200,
                title: seoTitle,
                description: seoDesc,
                canonicalUrl: canonical,
                ogType: "website",
                ogImage: (0, seoUtils_1.resolvePublicImageUrl)(cat.socialImageUrl || settings.defaultSocialImageUrl, baseUrl),
                robots: "index, follow, max-image-preview:large",
                cacheControl: "public, max-age=120, s-maxage=300",
                jsonLdBlocks: [
                    (0, seoUtils_1.buildCollectionPageJsonLd)({
                        name: cat.name,
                        description: seoDesc,
                        canonicalUrl: canonical,
                        itemUrls: products.map((p) => (0, seoUtils_1.buildCanonicalProductUrl)(baseUrl, p)),
                    }),
                    (0, seoUtils_1.buildBreadcrumbJsonLd)([
                        { name: "Home", url: `${baseUrl}/` },
                        { name: "Shop", url: `${baseUrl}/shop` },
                        { name: cat.name, url: canonical },
                    ]),
                ],
                bodyHtml: `
          <div class="crumbs"><a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/">Home</a> / <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shop">Shop</a> / ${(0, seoUtils_1.escapeHtml)(cat.name)}</div>
          <h1>${(0, seoUtils_1.escapeHtml)(cat.name)}</h1>
          <p>${(0, seoUtils_1.escapeHtml)(cat.description || "")}</p>
          ${renderProductCardsHtml(products, baseUrl)}
        `,
            };
            res.status(200)
                .set("Cache-Control", meta.cacheControl)
                .type("text/html; charset=utf-8")
                .send(renderHtmlDocument(meta, settings));
            return;
        }
        // Collection route: /collection/:slugOrId
        if (path.startsWith("/collection/")) {
            const slugOrId = decodeURIComponent(path.slice("/collection/".length)).trim();
            let colDoc = slugOrId ? await db.collection("collections").doc(slugOrId).get() : null;
            if (!colDoc || !colDoc.exists || colDoc.data()?.active !== true) {
                const slugQuery = await db
                    .collection("collections")
                    .where("active", "==", true)
                    .where("slug", "==", slugOrId)
                    .limit(1)
                    .get();
                if (!slugQuery.empty) {
                    colDoc = slugQuery.docs[0];
                }
            }
            if (!colDoc || !colDoc.exists || colDoc.data()?.active !== true) {
                const nf = buildNotFoundPage(path, settings);
                res.status(404).set("X-Robots-Tag", "noindex, nofollow").set("Cache-Control", "no-store")
                    .type("text/html; charset=utf-8").send(renderHtmlDocument(nf, settings));
                return;
            }
            const d = colDoc.data() || {};
            const col = {
                id: colDoc.id,
                name: String(d.name || "Editorial Capsule"),
                slug: typeof d.slug === "string" ? d.slug : colDoc.id,
                tagline: typeof d.tagline === "string" ? d.tagline : "",
                seoTitle: typeof d.seoTitle === "string" ? d.seoTitle : "",
                seoDescription: typeof d.seoDescription === "string" ? d.seoDescription : "",
                socialImageUrl: typeof d.socialImageUrl === "string" ? d.socialImageUrl : "",
                active: true,
            };
            const prodSnap = await db
                .collection("products")
                .where("active", "==", true)
                .where("collectionId", "==", col.id)
                .limit(48)
                .get();
            const products = prodSnap.docs.map((doc) => mapProductDoc(doc.id, doc.data()));
            const canonical = (0, seoUtils_1.buildCanonicalCollectionUrl)(baseUrl, col);
            const seoTitle = (col.seoTitle || `${col.name} | ${settings.storeName}`).trim();
            const seoDesc = (col.seoDescription || col.tagline || settings.defaultSeoDescription)
                .trim()
                .slice(0, 165);
            const meta = {
                statusCode: 200,
                title: seoTitle,
                description: seoDesc,
                canonicalUrl: canonical,
                ogType: "website",
                ogImage: (0, seoUtils_1.resolvePublicImageUrl)(col.socialImageUrl || settings.defaultSocialImageUrl, baseUrl),
                robots: "index, follow, max-image-preview:large",
                cacheControl: "public, max-age=120, s-maxage=300",
                jsonLdBlocks: [
                    (0, seoUtils_1.buildCollectionPageJsonLd)({
                        name: col.name,
                        description: seoDesc,
                        canonicalUrl: canonical,
                        itemUrls: products.map((p) => (0, seoUtils_1.buildCanonicalProductUrl)(baseUrl, p)),
                    }),
                    (0, seoUtils_1.buildBreadcrumbJsonLd)([
                        { name: "Home", url: `${baseUrl}/` },
                        { name: "Shop", url: `${baseUrl}/shop` },
                        { name: col.name, url: canonical },
                    ]),
                ],
                bodyHtml: `
          <div class="crumbs"><a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/">Home</a> / <a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shop">Shop</a> / ${(0, seoUtils_1.escapeHtml)(col.name)}</div>
          <h1>${(0, seoUtils_1.escapeHtml)(col.name)}</h1>
          <p>${(0, seoUtils_1.escapeHtml)(col.tagline || "")}</p>
          ${renderProductCardsHtml(products, baseUrl)}
        `,
            };
            res.status(200)
                .set("Cache-Control", meta.cacheControl)
                .type("text/html; charset=utf-8")
                .send(renderHtmlDocument(meta, settings));
            return;
        }
        // Dedicated Contact Us page with clickable official WhatsApp contact
        if (path === "/contact") {
            const wa = (0, seoUtils_1.resolveWhatsAppContact)(settings.whatsappNumber);
            const canonical = `${baseUrl}/contact`;
            const desc = `Contact ${settings.storeName} customer support via WhatsApp (${wa.display}) for sizing, styling, orders, and boutique concierge care.`;
            const meta = {
                statusCode: 200,
                title: `Contact Us | ${settings.storeName}`,
                description: desc,
                canonicalUrl: canonical,
                ogType: "article",
                ogImage: (0, seoUtils_1.resolvePublicImageUrl)(settings.defaultSocialImageUrl, baseUrl),
                robots: "index, follow",
                cacheControl: "public, max-age=300, s-maxage=600",
                jsonLdBlocks: [
                    (0, seoUtils_1.buildBreadcrumbJsonLd)([
                        { name: "Home", url: `${baseUrl}/` },
                        { name: "Contact Us", url: canonical },
                    ]),
                ],
                bodyHtml: `
          <div class="crumbs"><a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/">Home</a> / Contact Us</div>
          <article class="contact-card">
            <h1>Contact Us — ${(0, seoUtils_1.escapeHtml)(settings.storeName)}</h1>
            <p>Speak directly with our boutique concierge on WhatsApp for order updates, delivery quotes, styling recommendations, and size guidance.</p>
            <p><strong>Official WhatsApp:</strong> <a href="${(0, seoUtils_1.escapeHtml)(wa.url)}" target="_blank" rel="noopener noreferrer" style="color:var(--wa);font-weight:700;text-decoration:none;">${(0, seoUtils_1.escapeHtml)(wa.display)}</a></p>
            ${settings.supportEmail ? `<p><strong>Email:</strong> ${(0, seoUtils_1.escapeHtml)(settings.supportEmail)}</p>` : ""}
            ${settings.supportPhone ? `<p><strong>Phone:</strong> ${(0, seoUtils_1.escapeHtml)(settings.supportPhone)}</p>` : ""}
            <div style="margin-top:18px;display:flex;flex-wrap:wrap;gap:12px;">
              <a class="btn-wa" href="${(0, seoUtils_1.escapeHtml)(wa.url)}" target="_blank" rel="noopener noreferrer">Chat with Us on WhatsApp (${(0, seoUtils_1.escapeHtml)(wa.display)})</a>
              <a class="btn-outline" href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/shop">Explore Collection</a>
            </div>
          </article>
        `,
            };
            res.status(200)
                .set("Cache-Control", meta.cacheControl)
                .type("text/html; charset=utf-8")
                .send(renderHtmlDocument(meta, settings));
            return;
        }
        // Static informational boutique routes backed by storeSettings/public
        const infoPages = {
            "/about": {
                title: `About Us | ${settings.storeName}`,
                heading: `About ${settings.storeName}`,
                content: settings.aboutUs || settings.defaultSeoDescription,
            },
            "/shipping-policy": {
                title: `Shipping & Delivery Policy | ${settings.storeName}`,
                heading: "Shipping & Delivery Policy",
                content: settings.shippingPolicy ||
                    "Nationwide Nigeria delivery and international tracked courier dispatch.",
            },
            "/returns-policy": {
                title: `Store Policy — No Returns or Refunds | ${settings.storeName}`,
                heading: "Return & Refund Policy (All Sales Final)",
                content: settings.returnsPolicy &&
                    !/7[- ]?day|within 7 days|may be returned|may be exchanged/i.test(settings.returnsPolicy)
                    ? settings.returnsPolicy
                    : "ZEECROWN STORE currently does not accept customer returns or refunds. All sales are final once your order is confirmed.\n\nBefore placing an order, please review our Size Guide carefully or message our concierge on WhatsApp (+234 816 989 8763) for sizing, fabric, and styling guidance.\n\nIf an item arrives damaged or differs from what you ordered, please reach out to Customer Support promptly with your order number so our team can assist.",
            },
            "/privacy-policy": {
                title: `Privacy Policy | ${settings.storeName}`,
                heading: "Privacy Policy",
                content: settings.privacyPolicy ||
                    "We protect customer privacy and collect analytics only in accordance with your consent settings.",
            },
            "/terms": {
                title: `Terms of Service | ${settings.storeName}`,
                heading: "Terms of Service",
                content: settings.termsOfService ||
                    `Terms governing purchases and storefront use at ${settings.storeName}.`,
            },
        };
        const infoPage = infoPages[path];
        if (infoPage) {
            const canonical = `${baseUrl}${path}`;
            const meta = {
                statusCode: 200,
                title: infoPage.title,
                description: infoPage.content.replace(/\s+/g, " ").trim().slice(0, 160),
                canonicalUrl: canonical,
                ogType: "article",
                ogImage: (0, seoUtils_1.resolvePublicImageUrl)(settings.defaultSocialImageUrl, baseUrl),
                robots: "index, follow",
                cacheControl: "public, max-age=300, s-maxage=600",
                jsonLdBlocks: [
                    (0, seoUtils_1.buildBreadcrumbJsonLd)([
                        { name: "Home", url: `${baseUrl}/` },
                        { name: infoPage.heading, url: canonical },
                    ]),
                ],
                bodyHtml: `
          <div class="crumbs"><a href="${(0, seoUtils_1.escapeHtml)(baseUrl)}/">Home</a> / ${(0, seoUtils_1.escapeHtml)(infoPage.heading)}</div>
          <article>
            <h1>${(0, seoUtils_1.escapeHtml)(infoPage.heading)}</h1>
            <p style="white-space:pre-line;">${(0, seoUtils_1.escapeHtml)(infoPage.content)}</p>
          </article>
        `,
            };
            res.status(200)
                .set("Cache-Control", meta.cacheControl)
                .type("text/html; charset=utf-8")
                .send(renderHtmlDocument(meta, settings));
            return;
        }
        // 404 for unknown routes
        const nf = buildNotFoundPage(path, settings);
        res.status(404)
            .set("X-Robots-Tag", "noindex, nofollow")
            .set("Cache-Control", "no-store")
            .type("text/html; charset=utf-8")
            .send(renderHtmlDocument(nf, settings));
    }
    catch (err) {
        console.error("SEO render error:", err);
        res.status(500)
            .set("X-Robots-Tag", "noindex, nofollow")
            .set("Cache-Control", "no-store")
            .type("text/plain; charset=utf-8")
            .send("Temporary error rendering page.");
    }
}
//# sourceMappingURL=seoRenderer.js.map