"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleSitemapRequest = handleSitemapRequest;
exports.handleRobotsTxtRequest = handleRobotsTxtRequest;
const firestore_1 = require("firebase-admin/firestore");
const seoUtils_1 = require("./seoUtils");
function formatLastMod(raw) {
    if (raw instanceof firestore_1.Timestamp) {
        return raw.toDate().toISOString().split("T")[0];
    }
    if (raw && typeof raw.toDate === "function") {
        return raw.toDate().toISOString().split("T")[0];
    }
    return undefined;
}
async function resolveSiteBaseUrl(db) {
    const settingsSnap = await db.collection("storeSettings").doc("public").get();
    const raw = settingsSnap.data()?.publicSiteBaseUrl;
    return (0, seoUtils_1.normalizeBaseUrl)(typeof raw === "string" && raw ? raw : process.env.APP_BASE_URL || "https://zeecrownstore.com");
}
async function handleSitemapRequest(db, _req, res) {
    try {
        const [baseUrl, categoriesSnap, collectionsSnap, productsSnap] = await Promise.all([
            resolveSiteBaseUrl(db),
            db.collection("categories").where("active", "==", true).get(),
            db.collection("collections").where("active", "==", true).get(),
            db.collection("products").where("active", "==", true).get(),
        ]);
        const entries = [
            { loc: `${baseUrl}/`, changefreq: "daily", priority: "1.0" },
            { loc: `${baseUrl}/shop`, changefreq: "daily", priority: "0.9" },
            { loc: `${baseUrl}/about`, changefreq: "monthly", priority: "0.5" },
            { loc: `${baseUrl}/contact`, changefreq: "monthly", priority: "0.5" },
            { loc: `${baseUrl}/shipping-policy`, changefreq: "monthly", priority: "0.4" },
            { loc: `${baseUrl}/returns-policy`, changefreq: "monthly", priority: "0.4" },
            { loc: `${baseUrl}/privacy-policy`, changefreq: "monthly", priority: "0.3" },
            { loc: `${baseUrl}/terms`, changefreq: "monthly", priority: "0.3" },
        ];
        categoriesSnap.docs.forEach((doc) => {
            const d = doc.data();
            entries.push({
                loc: (0, seoUtils_1.buildCanonicalCategoryUrl)(baseUrl, {
                    id: doc.id,
                    name: String(d.name || ""),
                    slug: typeof d.slug === "string" ? d.slug : doc.id,
                    active: true,
                }),
                lastmod: formatLastMod(d.updatedAt),
                changefreq: "weekly",
                priority: "0.8",
            });
        });
        collectionsSnap.docs.forEach((doc) => {
            const d = doc.data();
            entries.push({
                loc: (0, seoUtils_1.buildCanonicalCollectionUrl)(baseUrl, {
                    id: doc.id,
                    name: String(d.name || ""),
                    slug: typeof d.slug === "string" ? d.slug : doc.id,
                    active: true,
                }),
                lastmod: formatLastMod(d.updatedAt),
                changefreq: "weekly",
                priority: "0.8",
            });
        });
        productsSnap.docs.forEach((doc) => {
            const d = doc.data();
            entries.push({
                loc: (0, seoUtils_1.buildCanonicalProductUrl)(baseUrl, {
                    id: doc.id,
                    title: String(d.title || ""),
                    slug: typeof d.slug === "string" ? d.slug : doc.id,
                    active: true,
                }),
                lastmod: formatLastMod(d.updatedAt),
                changefreq: "daily",
                priority: "0.85",
            });
        });
        const xml = (0, seoUtils_1.buildSitemapXml)(entries);
        res
            .status(200)
            .set("Cache-Control", "public, max-age=300, s-maxage=600")
            .type("application/xml; charset=utf-8")
            .send(xml);
    }
    catch (err) {
        console.error("Sitemap generation error:", err);
        res.status(500).type("text/plain; charset=utf-8").send("Failed to generate sitemap.");
    }
}
async function handleRobotsTxtRequest(db, _req, res) {
    try {
        const baseUrl = await resolveSiteBaseUrl(db);
        const body = [
            "User-agent: *",
            "Allow: /",
            "Allow: /shop",
            "Allow: /product/",
            "Allow: /category/",
            "Allow: /collection/",
            "Allow: /about",
            "Allow: /contact",
            "Allow: /shipping-policy",
            "Allow: /returns-policy",
            "Disallow: /admin",
            "Disallow: /account",
            "Disallow: /bag",
            "Disallow: /cart",
            "Disallow: /checkout",
            "Disallow: /auth",
            "Disallow: /login",
            "Disallow: /register",
            "Disallow: /notifications",
            "Disallow: /track",
            "Disallow: /order/",
            "Disallow: /support",
            "Disallow: /search",
            "",
            `Sitemap: ${baseUrl}/sitemap.xml`,
        ].join("\n");
        res
            .status(200)
            .set("Cache-Control", "public, max-age=300, s-maxage=600")
            .type("text/plain; charset=utf-8")
            .send(body);
    }
    catch (err) {
        console.error("Robots generation error:", err);
        res.status(500).type("text/plain; charset=utf-8").send("User-agent: *\nAllow: /\n");
    }
}
//# sourceMappingURL=sitemapAndRobots.js.map