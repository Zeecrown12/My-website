const { test } = require("node:test");
const assert = require("node:assert/strict");
const {
  OFFICIAL_WHATSAPP_DISPLAY,
  OFFICIAL_WHATSAPP_URL,
  resolveWhatsAppContact,
} = require("../lib/seo/seoUtils");
const { buildAiWebWidgetHtml } = require("../lib/ai/webWidget");
const {
  STARTER_WEB_SIZE_GUIDES,
  renderSizeGuideHtml,
  resolveSizeGuideForWebProduct,
} = require("../lib/seo/webSizeGuides");

test("official WhatsApp contact defaults and normalizes Nigerian numbers safely", () => {
  assert.equal(OFFICIAL_WHATSAPP_DISPLAY, "+234 816 989 8763");
  assert.equal(OFFICIAL_WHATSAPP_URL, "https://wa.me/2348169898763");
  assert.deepEqual(resolveWhatsAppContact(""), {
    display: "+234 816 989 8763",
    url: "https://wa.me/2348169898763",
  });
  assert.deepEqual(resolveWhatsAppContact("08169898763"), {
    display: "08169898763",
    url: "https://wa.me/2348169898763",
  });
});

test("AI web widget renders Speak to a Human and Contact Store Owner WhatsApp actions", () => {
  const html = buildAiWebWidgetHtml({ whatsappNumber: "" });
  assert.ok(html.includes("https://wa.me/2348169898763"));
  assert.ok(html.includes("+234 816 989 8763"));
  assert.ok(html.includes("Contact Store Owner (WhatsApp)"));
  assert.ok(html.includes("Speak to a Human"));
});

test("Web size guide resolver matches footwear and abaya/clothing and skips non-apparel bags", () => {
  const slippersGuide = resolveSizeGuideForWebProduct({
    productId: "p1",
    productTitle: "Lagos Leather Slippers",
    productSlug: "lagos-leather-slippers",
    guides: STARTER_WEB_SIZE_GUIDES,
  });
  assert.equal(slippersGuide?.id, "sg_slippers_sandals");

  const bagGuide = resolveSizeGuideForWebProduct({
    productId: "p2",
    productTitle: "Zaria Handcrafted Leather Handbag",
    productSlug: "zaria-leather-handbag",
    categoryId: "cat_handbags",
    categoryName: "Handbags",
    guides: STARTER_WEB_SIZE_GUIDES,
  });
  assert.equal(bagGuide, null);

  const rendered = renderSizeGuideHtml(
    slippersGuide,
    OFFICIAL_WHATSAPP_URL,
    OFFICIAL_WHATSAPP_DISPLAY,
    "Lagos Leather Slippers"
  );
  assert.ok(rendered.includes('id="size-guide"'));
  assert.ok(rendered.includes("Foot Length (cm)"));
  assert.ok(rendered.includes("https://wa.me/2348169898763"));
});
