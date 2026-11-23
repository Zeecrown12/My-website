import { getAuth } from "firebase-admin/auth";
import { Firestore } from "firebase-admin/firestore";
import { Request } from "firebase-functions/v2/https";

export interface AnalyzerHttpResponse {
  status(code: number): AnalyzerHttpResponse;
  set(field: string, value: string): AnalyzerHttpResponse;
  json(body: unknown): void;
}

export interface CategoryHint {
  id: string;
  name: string;
  slug?: string;
}

export interface ProductAiDraftResult {
  suggestedTitle: string;
  summary: string;
  description: string;
  visibleFeatures: string[];
  suggestedCategoryId: string;
  suggestedCategoryName: string;
  visibleColors: string[];
  fashionStyle: string;
  suitableOccasions: string[];
  seoTitle: string;
  seoDescription: string;
  searchKeywords: string[];
  tags: string[];
  uncertainDetails: string[];
  guardrailWarnings: string[];
}

const BOOTSTRAPPED_ADMIN_EMAIL = "olaademi.ZO@gmail.com";
const ALLOWED_AI_PRODUCT_ROLES = new Set([
  "SUPER_ADMIN",
  "STORE_MANAGER",
  "CONTENT_MANAGER",
]);

const FORBIDDEN_PATTERNS: Array<{ regex: RegExp; warning: string }> = [
  {
    regex: /\b\d{1,3}%\s*(?:cotton|silk|linen|polyester|wool|cashmere|nylon|viscose|rayon|spandex|elastane|leather)\b/gi,
    warning: "Exact fabric percentage removed — verify composition manually.",
  },
  {
    regex: /\bmade in\s+[a-z][a-z\s]{2,24}\b/gi,
    warning: "Country of origin claim removed — verify origin manually if known.",
  },
  {
    regex: /(?:₦|ngn|\$|usd|eur|£)\s*\d[\d,]*(?:\.\d+)?/gi,
    warning: "Invented price reference removed — enter selling price manually.",
  },
  {
    regex: /\b\d+(?:\.\d+)?\s*(?:cm|mm|inches|inch|grams|gram|kg|lbs)\b/gi,
    warning: "Unverified measurement or weight claim removed.",
  },
  {
    regex: /\b(?:iso\s*\d+|oeko-?tex|gots\s*certified|certified\s*organic|in\s*stock:\s*\d+|only\s+\d+\s+left)\b/gi,
    warning: "Unverified certification or stock count claim removed.",
  },
];

function isPrivateOrDisallowedHost(hostname: string): boolean {
  const host = hostname.trim().toLowerCase().replace(/^\[|\]$/g, "");
  if (!host) return true;
  if (
    host === "localhost" ||
    host === "localhost.localdomain" ||
    host.endsWith(".localhost") ||
    host.endsWith(".local") ||
    host.endsWith(".internal") ||
    host.endsWith(".arpa")
  ) {
    return true;
  }

  // IPv4 literal check
  const ipv4Match = host.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
  if (ipv4Match) {
    const octets = ipv4Match.slice(1).map((n) => Number(n));
    if (octets.some((o) => o < 0 || o > 255)) return true;
    const [a, b] = octets;
    if (
      a === 0 ||
      a === 10 ||
      a === 127 ||
      (a === 169 && b === 254) ||
      (a === 172 && b >= 16 && b <= 31) ||
      (a === 192 && b === 168) ||
      (a === 100 && b >= 64 && b <= 127) ||
      a >= 224
    ) {
      return true;
    }
  }

  // IPv6 loopback, ULA, or link-local checks
  if (host.includes(":")) {
    const normalized = host.split("%")[0].toLowerCase();
    if (
      normalized === "::" ||
      normalized === "::1" ||
      normalized.startsWith("fc") ||
      normalized.startsWith("fd") ||
      normalized.startsWith("fe80:") ||
      normalized.startsWith("::ffff:127.") ||
      normalized.startsWith("::ffff:10.") ||
      normalized.startsWith("::ffff:169.254.") ||
      normalized.startsWith("::ffff:192.168.")
    ) {
      return true;
    }
  }

  return false;
}

export function validatePublicImageUrl(rawUrl: string): string | null {
  const clean = String(rawUrl || "").trim();
  if (!clean || clean.length > 2000) return null;
  let parsed: URL;
  try {
    parsed = new URL(clean);
  } catch {
    return null;
  }
  if (parsed.protocol !== "https:") return null;
  if (parsed.username || parsed.password) return null;
  if (isPrivateOrDisallowedHost(parsed.hostname)) return null;
  return parsed.toString();
}

export function resolveCategoryHint(
  categories: CategoryHint[],
  idHint: string,
  nameHint: string
): CategoryHint | null {
  if (!categories.length) return null;
  const cleanId = idHint.trim().toLowerCase();
  if (cleanId) {
    const byId = categories.find((c) => c.id.toLowerCase() === cleanId);
    if (byId) return byId;
  }
  const cleanName = nameHint.trim().toLowerCase();
  if (!cleanName) return null;
  const exact = categories.find(
    (c) =>
      c.name.trim().toLowerCase() === cleanName ||
      (c.slug || "").trim().toLowerCase() === cleanName.replace(/\s+/g, "-")
  );
  if (exact) return exact;
  return (
    categories.find((c) => {
      const n = c.name.trim().toLowerCase();
      return n.includes(cleanName) || cleanName.includes(n);
    }) || null
  );
}

export function sanitizeProductAiDraft(
  raw: Record<string, unknown>,
  categories: CategoryHint[]
): ProductAiDraftResult {
  const warnings: string[] = [];

  const cleanText = (val: unknown, maxLen: number): string => {
    let text = String(val || "").replace(/\s+/g, " ").trim();
    for (const rule of FORBIDDEN_PATTERNS) {
      if (rule.regex.test(text)) {
        rule.regex.lastIndex = 0;
        text = text.replace(rule.regex, "").replace(/\s{2,}/g, " ").trim();
        if (!warnings.includes(rule.warning)) {
          warnings.push(rule.warning);
        }
      }
      rule.regex.lastIndex = 0;
    }
    return text.slice(0, maxLen);
  };

  const cleanList = (val: unknown, maxItems: number, itemMax: number): string[] => {
    if (!Array.isArray(val)) return [];
    const seen = new Set<string>();
    const out: string[] = [];
    for (const entry of val) {
      const cleaned = cleanText(entry, itemMax);
      const key = cleaned.toLowerCase();
      if (cleaned && !seen.has(key)) {
        seen.add(key);
        out.push(cleaned);
      }
      if (out.length >= maxItems) break;
    }
    return out;
  };

  const rawTitle = cleanText(raw.suggestedTitle, 120);
  const rawSummary = cleanText(raw.summary, 300);
  const rawDescription = cleanText(raw.description, 2000);
  const visibleFeatures = cleanList(raw.visibleFeatures, 10, 140);
  const visibleColors = cleanList(raw.visibleColors, 12, 40);
  const fashionStyle = cleanText(raw.fashionStyle, 80);
  const suitableOccasions = cleanList(raw.suitableOccasions, 8, 60);
  const rawSeoTitle = cleanText(raw.seoTitle, 120);
  const rawSeoDescription = cleanText(raw.seoDescription, 300);
  const searchKeywords = cleanList(raw.searchKeywords, 15, 40);
  const tagsRaw = cleanList(raw.tags, 15, 40);
  const uncertainDetails = cleanList(raw.uncertainDetails, 8, 140);

  const catHintName = String(raw.suggestedCategoryName || "").trim();
  const catHintId = String(raw.suggestedCategoryId || "").trim();
  const matchedCat = resolveCategoryHint(categories, catHintId, catHintName);
  if (!matchedCat && catHintName) {
    warnings.push(
      `Suggested category '${catHintName}' did not match an active category — select manually.`
    );
  }
  if (visibleColors.length === 0) {
    warnings.push("Colour could not be verified confidently from photos — confirm manually.");
  }

  const suggestedTitle = rawTitle || "New Fashion Piece Draft";
  const description =
    rawDescription || rawSummary || "Review and confirm product description before publishing.";
  const seoTitle = rawSeoTitle || `${suggestedTitle} | ZEECROWN STORE`.slice(0, 120);
  const seoDescription = rawSeoDescription || (rawSummary || description).slice(0, 160);
  const tags = Array.from(
    new Set([...tagsRaw, ...searchKeywords].map((t) => t.toLowerCase().trim()).filter(Boolean))
  ).slice(0, 15);

  return {
    suggestedTitle,
    summary: rawSummary,
    description,
    visibleFeatures,
    suggestedCategoryId: matchedCat?.id || "",
    suggestedCategoryName: matchedCat?.name || "",
    visibleColors,
    fashionStyle,
    suitableOccasions,
    seoTitle,
    seoDescription,
    searchKeywords,
    tags,
    uncertainDetails,
    guardrailWarnings: warnings,
  };
}

async function verifyAdminCaller(
  db: Firestore,
  req: Request
): Promise<{ uid: string; email: string } | { errorCode: number; errorMsg: string }> {
  const authHeader = String(req.header("authorization") || "");
  if (!authHeader.startsWith("Bearer ")) {
    return { errorCode: 401, errorMsg: "Missing Firebase ID token." };
  }
  const token = authHeader.slice("Bearer ".length).trim();
  if (!token) {
    return { errorCode: 401, errorMsg: "Empty Firebase ID token." };
  }

  let decoded;
  try {
    decoded = await getAuth().verifyIdToken(token);
  } catch {
    return { errorCode: 401, errorMsg: "Invalid or expired authentication token." };
  }

  const email = String(decoded.email || "");
  if (decoded.email_verified === true && email === BOOTSTRAPPED_ADMIN_EMAIL) {
    return { uid: decoded.uid, email };
  }

  const adminSnap = await db.collection("adminUsers").doc(decoded.uid).get();
  const adminData = adminSnap.data();
  const role = String(adminData?.role || "").toUpperCase();
  if (!adminSnap.exists || adminData?.active !== true || !ALLOWED_AI_PRODUCT_ROLES.has(role)) {
    return {
      errorCode: 403,
      errorMsg: "Administrator permission (Super Admin, Store Manager, or Content Manager) is required.",
    };
  }
  return { uid: decoded.uid, email };
}

export async function handleAnalyzeProductRequest(
  db: Firestore,
  req: Request,
  res: AnalyzerHttpResponse
): Promise<void> {
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  const authCheck = await verifyAdminCaller(db, req);
  if ("errorCode" in authCheck) {
    res.status(authCheck.errorCode).json({ error: authCheck.errorMsg });
    return;
  }

  const body = (req.body || {}) as {
    imageUrls?: unknown;
    titleHint?: unknown;
  };
  const rawUrls = Array.isArray(body.imageUrls) ? body.imageUrls.map(String) : [];
  const validUrls = rawUrls.map(validatePublicImageUrl).filter((u): u is string => Boolean(u)).slice(0, 4);

  if (validUrls.length === 0) {
    res.status(400).json({
      error: "Provide at least one valid public HTTPS product image URL.",
    });
    return;
  }

  const geminiKey = String(process.env.GEMINI_API_KEY || "").trim();
  if (!geminiKey) {
    res.status(503).json({
      error:
        "Server-side GEMINI_API_KEY is not configured. Use the Android app's Firebase AI Logic analyzer or set GEMINI_API_KEY in Cloud Functions Secrets.",
    });
    return;
  }

  const catSnap = await db.collection("categories").where("active", "==", true).limit(40).get();
  const categories: CategoryHint[] = catSnap.docs.map((doc) => ({
    id: doc.id,
    name: String(doc.data().name || ""),
    slug: typeof doc.data().slug === "string" ? doc.data().slug : doc.id,
  }));

  const imageParts: Array<{ inlineData: { mimeType: string; data: string } }> = [];
  for (const url of validUrls) {
    try {
      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 10_000);
      const resp = await fetch(url, { signal: controller.signal, redirect: "error" });
      clearTimeout(t);
      if (!resp.ok) continue;
      const ct = (resp.headers.get("content-type") || "image/jpeg").split(";")[0].trim().toLowerCase();
      if (!["image/jpeg", "image/jpg", "image/png", "image/webp"].includes(ct)) continue;
      const buf = Buffer.from(await resp.arrayBuffer());
      if (buf.length === 0 || buf.length > 4 * 1024 * 1024) continue;
      imageParts.push({
        inlineData: {
          mimeType: ct === "image/jpg" ? "image/jpeg" : ct,
          data: buf.toString("base64"),
        },
      });
    } catch {
      // Skip unreadable URL and inspect remaining ones
    }
  }

  if (imageParts.length === 0) {
    res.status(400).json({
      error: "Could not download valid image bytes from the provided HTTPS URLs.",
    });
    return;
  }

  const titleHint = String(body.titleHint || "").trim().slice(0, 100);
  const categoryListText = categories.length
    ? categories.map((c) => `- id="${c.id}", name="${c.name}"`).join("\n")
    : "No active categories loaded; leave suggestedCategoryId empty.";

  const promptText =
    `Analyze all ${imageParts.length} image(s) as one ZEECROWN STORE fashion product.\n` +
    (titleHint ? `Optional admin working note: "${titleHint}"\n` : "") +
    `Active Categories:\n${categoryListText}\n\n` +
    "STRICT NON-HALLUCINATION RULES:\n" +
    "1. Describe ONLY clearly visible details (design, silhouette, finish, sole, hardware, pattern, colour).\n" +
    "2. NEVER invent price, currency, stock quantity, available sizes, unverified brand name, exact fabric percentages (e.g. 100% silk), weight, dimensions, origin, or certifications.\n" +
    "3. Put anything unclear (exact material blend, fit measurements, backing) in uncertainDetails.\n" +
    "Return strict JSON matching keys: suggestedTitle, summary, description, visibleFeatures, suggestedCategoryId, suggestedCategoryName, visibleColors, fashionStyle, suitableOccasions, seoTitle, seoDescription, searchKeywords, tags, uncertainDetails.";

  const modelName = String(process.env.GEMINI_MODEL || "gemini-3.8-flash").trim();
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 35_000);
    const geminiResp = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(modelName)}:generateContent?key=${encodeURIComponent(geminiKey)}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        signal: controller.signal,
        body: JSON.stringify({
          contents: [
            {
              role: "user",
              parts: [...imageParts, { text: promptText }],
            },
          ],
          generationConfig: {
            responseMimeType: "application/json",
            temperature: 0.25,
            maxOutputTokens: 1200,
          },
        }),
      }
    );
    clearTimeout(timeoutId);

    if (!geminiResp.ok) {
      res.status(502).json({ error: `Vision API returned status ${geminiResp.status}.` });
      return;
    }

    const payload = (await geminiResp.json()) as {
      candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
    };
    const rawText = payload.candidates?.[0]?.content?.parts?.[0]?.text || "";
    const parsed = JSON.parse(rawText) as Record<string, unknown>;
    const draft = sanitizeProductAiDraft(parsed, categories);

    res.status(200).json({ ok: true, model: modelName, draft });
  } catch (err) {
    res.status(502).json({
      error: err instanceof Error ? err.message : "Product image analysis failed.",
    });
  }
}
