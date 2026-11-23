import { Firestore, Timestamp } from "firebase-admin/firestore";
import { Request } from "firebase-functions/v2/https";
import type { SeoHttpResponse } from "./seoRenderer";
import {
  buildCanonicalCategoryUrl,
  buildCanonicalCollectionUrl,
  buildCanonicalProductUrl,
  buildSitemapXml,
  normalizeBaseUrl,
  SitemapEntry,
} from "./seoUtils";

function formatLastMod(raw: unknown): string | undefined {
  if (raw instanceof Timestamp) {
    return raw.toDate().toISOString().split("T")[0];
  }
  if (raw && typeof (raw as { toDate?: () => Date }).toDate === "function") {
    return (raw as { toDate: () => Date }).toDate().toISOString().split("T")[0];
  }
  return undefined;
}

async function resolveSiteBaseUrl(db: Firestore): Promise<string> {
  const settingsSnap = await db.collection("storeSettings").doc("public").get();
  const raw = settingsSnap.data()?.publicSiteBaseUrl;
  return normalizeBaseUrl(
    typeof raw === "string" && raw ? raw : process.env.APP_BASE_URL || "https://zeecrownstore.com"
  );
}

export async function handleSitemapRequest(
  db: Firestore,
  _req: Request,
  res: SeoHttpResponse
): Promise<void> {
  try {
    const [baseUrl, categoriesSnap, collectionsSnap, productsSnap] = await Promise.all([
      resolveSiteBaseUrl(db),
      db.collection("categories").where("active", "==", true).get(),
      db.collection("collections").where("active", "==", true).get(),
      db.collection("products").where("active", "==", true).get(),
    ]);

    const entries: SitemapEntry[] = [
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
        loc: buildCanonicalCategoryUrl(baseUrl, {
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
        loc: buildCanonicalCollectionUrl(baseUrl, {
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
        loc: buildCanonicalProductUrl(baseUrl, {
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

    const xml = buildSitemapXml(entries);
    res
      .status(200)
      .set("Cache-Control", "public, max-age=300, s-maxage=600")
      .type("application/xml; charset=utf-8")
      .send(xml);
  } catch (err) {
    console.error("Sitemap generation error:", err);
    res.status(500).type("text/plain; charset=utf-8").send("Failed to generate sitemap.");
  }
}

export async function handleRobotsTxtRequest(
  db: Firestore,
  _req: Request,
  res: SeoHttpResponse
): Promise<void> {
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
  } catch (err) {
    console.error("Robots generation error:", err);
    res.status(500).type("text/plain; charset=utf-8").send("User-agent: *\nAllow: /\n");
  }
}
