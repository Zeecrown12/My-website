const { test } = require("node:test");
const assert = require("node:assert/strict");
const { buildAiWebWidgetHtml } = require("../lib/ai/webWidget");
const { buildGroundedFallbackReply, parseIntentFromQuery } = require("../lib/ai/aiEngine");
const { buildZeecrownEmail } = require("../lib/templates/emailTemplates");
const { sanitizeProductAiDraft, validatePublicImageUrl } = require("../lib/ai/productAnalyzer");

test("webWidget renders Need Help? launcher, ZEECROWN CUSTOMER CARE header, Royal Purple & Gold branding, and 3 modes", () => {
  const html = buildAiWebWidgetHtml({ whatsappNumber: "+234 816 989 8763" });

  assert.ok(html.includes("Need Help?"));
  assert.ok(html.includes("ZEECROWN CUSTOMER CARE"));
  assert.ok(html.includes("Your shopping and customer support assistant."));
  assert.ok(html.includes("Shopping Assistance"));
  assert.ok(html.includes("Customer Support"));
  assert.ok(html.includes("Chat with Us on WhatsApp"));
  assert.ok(html.includes("#542080"));
  assert.ok(html.includes("#C8A64D"));
  assert.equal(html.includes("#5D2D46"), false);
});

test("aiEngine enforces official no-returns / no-refunds policy and strips legacy 7-day return claims", () => {
  const intent = parseIntentFromQuery("Can I return my dress for a refund?");
  const reply = buildGroundedFallbackReply({
    intent,
    matchedProducts: [],
    matchedFaqs: [],
    matchedZones: [],
    storeSettings: {
      storeName: "ZEECROWN STORE",
      returnPolicy: "Unworn items may be returned or exchanged within 7 days of delivery.",
    },
    deliverySettings: {},
    coupons: [],
    customerOrders: [],
    isSignedIn: false,
    config: { humanSupportAvailable: true },
    baseUrl: "https://zeecrownstore.com",
  });

  assert.ok(reply.replyText.toLowerCase().includes("does not accept customer returns or refunds"));
  assert.equal(/7[- ]?day|within 7 days/i.test(reply.replyText), false);
});

test("sanitizeProductAiDraft blocks hallucinated price, fabric %, origin, weight, certifications, stock, and unknown categories", () => {
  const categories = [
    { id: "cat_abayas", name: "Luxury Abayas", slug: "luxury-abayas" },
    { id: "cat_slippers", name: "Leather Slippers", slug: "leather-slippers" },
  ];

  const draft = sanitizeProductAiDraft(
    {
      suggestedTitle: "Royal Purple Silk Gown ₦65,000",
      summary: "100% silk gown Made in Dubai weighing 500 grams with GOTS certified dye.",
      description: "Only 2 left for $150. Length 140 cm.",
      visibleFeatures: ["Fluid draped sleeves", "In stock: 2", "70% silk linen blend"],
      suggestedCategoryId: "",
      suggestedCategoryName: "Luxury Abayas",
      visibleColors: ["Royal Purple", "Gold"],
      fashionStyle: "Modest Couture",
      suitableOccasions: ["Evening Gala"],
      seoTitle: "Royal Purple Silk Gown",
      seoDescription: "Modest purple couture gown at ZEECROWN STORE.",
      searchKeywords: ["purple gown", "abaya"],
      tags: ["abaya"],
      uncertainDetails: ["Exact fabric content requires garment label check"],
    },
    categories
  );

  assert.equal(draft.suggestedCategoryId, "cat_abayas");
  assert.equal(draft.suggestedCategoryName, "Luxury Abayas");
  assert.equal("priceNgn" in draft, false);
  assert.equal("stockQuantity" in draft, false);
  assert.equal("sizes" in draft, false);

  const combined = [draft.suggestedTitle, draft.summary, draft.description, ...draft.visibleFeatures].join(" ");
  assert.equal(combined.includes("65,000"), false);
  assert.equal(combined.includes("$150"), false);
  assert.equal(/100% silk|70% silk/i.test(combined), false);
  assert.equal(/Made in Dubai/i.test(combined), false);
  assert.equal(/500 grams|140 cm/i.test(combined), false);
  assert.equal(/GOTS certified|Only 2 left|In stock: 2/i.test(combined), false);
  assert.ok(draft.guardrailWarnings.length >= 4);
});

test("validatePublicImageUrl enforces HTTPS and rejects SSRF loopback/private/metadata targets", () => {
  assert.equal(
    validatePublicImageUrl("https://cdn.zeecrownstore.com/products/abaya.jpg"),
    "https://cdn.zeecrownstore.com/products/abaya.jpg"
  );
  assert.equal(validatePublicImageUrl("http://cdn.zeecrownstore.com/products/abaya.jpg"), null);
  assert.equal(validatePublicImageUrl("https://localhost/secret.png"), null);
  assert.equal(validatePublicImageUrl("https://127.0.0.1/admin"), null);
  assert.equal(validatePublicImageUrl("https://169.254.169.254/latest/meta-data/"), null);
  assert.equal(validatePublicImageUrl("https://10.0.0.5/internal.jpg"), null);
  assert.equal(validatePublicImageUrl("https://192.168.1.1/router.png"), null);
  assert.equal(validatePublicImageUrl("https://[::1]/loop.png"), null);
  assert.equal(validatePublicImageUrl("https://user:pass@cdn.zeecrownstore.com/x.png"), null);
});

test("emailTemplates use Official Royal Purple & Metallic Gold palette and official WhatsApp contact", () => {
  const rendered = buildZeecrownEmail({
    storeName: "ZEECROWN STORE",
    customerName: "Amaka",
    orderNumber: "ZC-8821",
    headline: "Order Confirmed",
    message: "Your couture piece is being prepared.",
    trackingLink: "https://zeecrownstore.com/track?token=abc",
  });

  assert.ok(rendered.html.includes("#542080"));
  assert.ok(rendered.html.includes("#C8A64D"));
  assert.ok(rendered.html.includes("+234 816 989 8763"));
  assert.equal(rendered.html.includes("#C98F9B"), false);
});
