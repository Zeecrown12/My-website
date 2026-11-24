const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_customer_1";
const BOB_UID = "bob_customer_2";
const ADMIN_UID = "zeecrown_admin_1";
const ADMIN_EMAIL = "olaademi.ZO@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated guests can browse active catalog only; PII, drafts, and all writes are denied", async () => {
  const adminDb = adminContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertSucceeds(
    adminDb.collection("products").doc("prod_public").set({
      title: "Adire Silk Scarf",
      slug: "adire-silk-scarf",
      description: "Artisanal scarf.",
      categoryId: "cat_1",
      priceNgn: 15000,
      priceUsd: 10,
      imageUrls: [],
      sizes: [],
      colors: [],
      featured: false,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("products").doc("prod_private_draft").set({
      title: "Draft Gown",
      slug: "draft-gown",
      description: "Unpublished.",
      categoryId: "cat_1",
      priceNgn: 50000,
      priceUsd: 35,
      imageUrls: [],
      sizes: [],
      colors: [],
      featured: false,
      active: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  const unauth = testEnv.unauthenticatedContext().firestore();
  // Active storefront browsing works for guests
  await assertSucceeds(unauth.collection("products").where("active", "==", true).get());
  await assertSucceeds(unauth.collection("categories").where("active", "==", true).get());
  // Drafts, unfiltered queries, and all PII/orders/addresses/carts remain strictly denied
  await assertFails(unauth.collection("products").get());
  await assertFails(unauth.collection("products").doc("prod_private_draft").get());
  await assertFails(unauth.collection("orders").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertFails(unauth.collection("carts").doc(ALICE_UID).collection("items").get());
  await assertFails(
    unauth.collection("products").doc("prod_1").set({
      title: "Hack",
      slug: "hack",
      description: "Hack",
      categoryId: "cat_1",
      priceNgn: 1000,
      priceUsd: 1,
      imageUrls: [],
      sizes: [],
      colors: [],
      featured: false,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Email spoofing with unverified admin email is rejected", async () => {
  const spoofedAdmin = testEnv.authenticatedContext("attacker_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const now = new Date(Date.now() - 1000);
  await assertFails(
    spoofedAdmin.collection("categories").doc("cat_slippers").set({
      name: "Slippers",
      slug: "slippers",
      description: "Luxury leather slippers",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User profile & PII isolation: owner can create/update profile, cannot escalate role or access other users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Amaka Okafor",
      preferredCurrency: "NGN",
      role: "customer",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cannot self-assign admin role on create or update
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      preferredCurrency: "NGN",
      role: "admin",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredCurrency: "USD",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Private PII split collection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("info").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phone: "+2348012345678",
      shippingAddress: "14 Admiralty Way, Lekki Phase 1",
      city: "Lagos",
      state: "Lagos",
      country: "Nigeria",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot read Alice's profile or private PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());
});

test("Delivery addresses subcollection enforces owner-only CRUD, validation bounds, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const created = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_home").set({
      userId: ALICE_UID,
      label: "Home",
      recipientName: "Amaka Okafor",
      phone: "+2348012345678",
      country: "Nigeria",
      state: "Lagos",
      city: "Lekki Phase 1",
      lga: "Eti-Osa",
      streetAddress: "14 Admiralty Way",
      landmark: "Opposite Zenith Bank",
      deliveryInstructions: "Call concierge on arrival",
      defaultAddress: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice can list and update her address without mutating createdAt or userId
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_home").update({
      landmark: "Beside Palms Mall",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field or spoofed userId
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_bad").set({
      userId: BOB_UID,
      label: "Office",
      recipientName: "Amaka",
      phone: "+2348012345678",
      country: "Nigeria",
      state: "Lagos",
      city: "VI",
      streetAddress: "10 Adeola Odeku",
      defaultAddress: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot read, list, update, or delete Alice's saved addresses
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_home").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_home").update({
      streetAddress: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_home").delete());
});

test("Bootstrapped Admin manages catalog; Customers must use filtered active queries", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("categories").doc("cat_adire").set({
      name: "Adire Fabrics & Fashion",
      slug: "adire",
      description: "Hand-dyed artisanal Adire silk and cotton pieces",
      imageKey: "img_cat_adire_abaya",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("categories").doc("cat_silk_adire").set({
      name: "Silk Adire Sets",
      slug: "silk-adire",
      description: "Luminous silk Adire sub-department",
      imageKey: "img_cat_adire_abaya",
      parentCategoryId: "cat_adire",
      sortOrder: 2,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("categories").doc("cat_hidden").set({
      name: "Archived Dept",
      slug: "archived",
      description: "Deactivated department",
      sortOrder: 99,
      active: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("products").doc("prod_adire_1").set({
      title: "Zaria Blush Silk Adire Boubou",
      slug: "zaria-blush-silk-adire-boubou",
      description: "Artisanal hand-dyed silk Adire boubou in blush and champagne tones.",
      categoryId: "cat_silk_adire",
      collectionId: "col_harmattan",
      priceNgn: 68500,
      compareAtPriceNgn: 78000,
      priceUsd: 46,
      mainImageUrl: "drawable://img_cat_adire_abaya",
      imageUrls: ["drawable://img_cat_adire_abaya"],
      sizes: ["S", "M", "L", "XL"],
      colors: ["Blush Pink", "Champagne"],
      featured: true,
      newArrival: true,
      brand: "Zeecrown Atelier",
      sku: "ZC-ZARIA-01",
      attributes: { Fabric: "100% Crepe Silk", Craft: "Hand-dyed Abeokuta Adire" },
      unavailableVariants: ["XL • Champagne"],
      searchTokens: ["zaria", "blush", "silk", "adire", "boubou", "zc-zaria-01"],
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("products").doc("prod_draft").set({
      title: "Unpublished Draft Gown",
      slug: "unpublished-draft",
      description: "Internal draft piece.",
      categoryId: "cat_adire",
      priceNgn: 50000,
      priceUsd: 33,
      imageUrls: [],
      sizes: ["M"],
      colors: ["Ivory"],
      featured: false,
      active: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot write products or run unfiltered queries or read inactive docs directly
  await assertFails(
    aliceDb.collection("products").doc("prod_hack").set({
      title: "Fake Item",
      slug: "fake",
      description: "Fake",
      categoryId: "cat_adire",
      priceNgn: 1,
      priceUsd: 1,
      imageUrls: [],
      sizes: [],
      colors: [],
      featured: false,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(aliceDb.collection("products").get());
  await assertFails(aliceDb.collection("products").doc("prod_draft").get());
  await assertFails(aliceDb.collection("categories").doc("cat_hidden").get());

  // Filtered storefront query & searchToken array-contains query succeed
  await assertSucceeds(aliceDb.collection("products").where("active", "==", true).get());
  await assertSucceeds(aliceDb.collection("products").where("active", "==", true).where("searchTokens", "array-contains", "adire").get());
  await assertSucceeds(aliceDb.collection("categories").where("active", "==", true).get());
});

test("Carts, Wishlists, Orders & Payments enforce ownership, @DocumentId exclusion, and payment integrity", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  // Seed product & delivery zone as admin
  await assertSucceeds(
    adminDb.collection("products").doc("prod_slipper_1").set({
      title: "Eko Champagne Leather Slippers",
      slug: "eko-champagne-slippers",
      description: "Handcrafted Nigerian leather slide slippers.",
      categoryId: "cat_slippers",
      priceNgn: 32000,
      priceUsd: 22,
      imageUrls: ["drawable://img_cat_bags_footwear"],
      sizes: ["38", "39", "40", "41"],
      colors: ["Champagne Gold"],
      featured: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("deliveryZones").doc("zone_lagos").set({
      name: "Lagos Mainland & Island",
      country: "Nigeria",
      states: ["Lagos"],
      feeNgn: 3500,
      estimatedDays: "1-2 business days",
      active: true,
      updatedAt: now,
    })
  );

  // Alice adds to cart (no "id" key, valid product reference)
  await assertSucceeds(
    aliceDb.collection("carts").doc(ALICE_UID).collection("items").doc("item_1").set({
      userId: ALICE_UID,
      productId: "prod_slipper_1",
      productTitle: "Eko Champagne Leather Slippers",
      productImageUrl: "drawable://img_cat_bags_footwear",
      variantLabel: "Size 39 • Champagne Gold",
      unitPriceNgn: 32000,
      quantity: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject @DocumentId `id` inside body
  await assertFails(
    aliceDb.collection("carts").doc(ALICE_UID).collection("items").doc("item_2").set({
      id: "item_2",
      userId: ALICE_UID,
      productId: "prod_slipper_1",
      productTitle: "Eko Champagne Leather Slippers",
      variantLabel: "Size 40",
      unitPriceNgn: 32000,
      quantity: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot list Alice's cart
  await assertFails(bobDb.collection("carts").doc(ALICE_UID).collection("items").get());

  // Alice creates order in PENDING_PAYMENT + UNPAID
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").set({
      userId: ALICE_UID,
      orderNumber: "ZC-2026-0001",
      itemsSummary: "1x Eko Champagne Leather Slippers (Size 39)",
      itemCount: 1,
      subtotalNgn: 32000,
      deliveryFeeNgn: 3500,
      discountNgn: 0,
      totalNgn: 35500,
      currency: "NGN",
      deliveryZoneId: "zone_lagos",
      shippingCity: "Lekki",
      shippingState: "Lagos",
      status: "PENDING_PAYMENT",
      paymentMethod: "BANK_TRANSFER",
      paymentStatus: "UNPAID",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice CANNOT forge paymentStatus = PAID directly
  await assertFails(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "CONFIRMED",
      paymentStatus: "PAID",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice CAN submit bank transfer notification (PENDING_VERIFICATION)
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "PAYMENT_SUBMITTED",
      paymentStatus: "PENDING_VERIFICATION",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin CAN verify payment and confirm order
  await assertSucceeds(
    adminDb.collection("orders").doc("ord_1").update({
      status: "CONFIRMED",
      paymentStatus: "PAID",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice's scoped order query succeeds; unfiltered fails
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());
});

test("Reviews and Notifications enforce product integrity, authorship, and read-flag scoping", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("products").doc("prod_bubu_1").set({
      title: "Zaria Blush Silk Adire Bubu",
      slug: "zaria-blush-silk-adire-bubu",
      description: "Hand-dyed silk Bubu.",
      categoryId: "cat_abayas",
      priceNgn: 74500,
      priceUsd: 49,
      imageUrls: ["drawable://img_cat_adire_abaya"],
      sizes: ["Free Size"],
      colors: ["Blush Rose"],
      featured: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice can submit a visible review for an active product
  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_1").set({
      productId: "prod_bubu_1",
      userId: ALICE_UID,
      authorName: "Amaka O.",
      rating: 5,
      comment: "Rich silk drape and glowing blush finish.",
      visible: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customers must query visible reviews
  await assertSucceeds(bobDb.collection("reviews").where("visible", "==", true).get());
  await assertFails(bobDb.collection("reviews").get());

  // Admin creates notification for Alice; Alice can list hers and mark readFlag = true; Bob cannot
  await assertSucceeds(
    adminDb.collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "Order Confirmed",
      body: "Your Zeecrown order ZC-101 is confirmed.",
      category: "ORDER",
      readFlag: false,
      targetRoute: "account",
      createdAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("notifications").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("notifications").doc("notif_1").update({ readFlag: true }));
  await assertFails(bobDb.collection("notifications").doc("notif_1").update({ readFlag: true }));
});

test("Newsletter subscriptions enforce email validation, duplicate denial, and admin-only listing", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Admin writes extended storeSettings with announcements, social handles, timezone, order prefix, and tax
  await assertSucceeds(
    adminDb.collection("storeSettings").doc("public").set({
      storeName: "ZEECROWN STORE",
      tagline: "Style That Speaks. Elegance That Lasts.",
      logoPlaceholderText: "ZEECROWN Official Crest",
      announcementMessages: ["Welcome to Zeecrown Store.", "Nationwide delivery available."],
      instagramHandle: "@zeecrownstore",
      instagramUrl: "https://instagram.com/zeecrownstore",
      timeZone: "Africa/Lagos",
      orderNumberPrefix: "ZC",
      taxEnabled: true,
      taxRatePercent: 7.5,
      visibility: "public",
      updatedAt: now,
    })
  );

  await assertFails(
    adminDb.collection("storeSettings").doc("public").set({
      storeName: "ZEECROWN STORE",
      tagline: "Style That Speaks. Elegance That Lasts.",
      logoPlaceholderText: "ZEECROWN Official Crest",
      taxEnabled: true,
      taxRatePercent: 75,
      visibility: "public",
      updatedAt: now,
    })
  );

  // Alice subscribes with deterministic ID
  await assertSucceeds(
    aliceDb.collection("newsletterSubscribers").doc("sub_alice_hash").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      active: true,
      subscribedAt: now,
    })
  );

  // Invalid email format rejected
  await assertFails(
    bobDb.collection("newsletterSubscribers").doc("sub_bad").set({
      userId: BOB_UID,
      email: "not-an-email",
      active: true,
      subscribedAt: now,
    })
  );

  // Bob cannot list subscribers or hijack/overwrite Alice's subscription doc
  await assertFails(bobDb.collection("newsletterSubscribers").get());
  await assertFails(bobDb.collection("newsletterSubscribers").doc("sub_alice_hash").get());
  await assertFails(
    bobDb.collection("newsletterSubscribers").doc("sub_alice_hash").set({
      userId: BOB_UID,
      email: "alice@example.com",
      active: true,
      subscribedAt: now,
    })
  );

  // Admin can list subscribers
  await assertSucceeds(adminDb.collection("newsletterSubscribers").get());
});

test("RBAC admin roles enforce least privilege and immutable audit/inventory logs", async () => {
  const superDb = adminContext().firestore();
  const orderMgrUid = "order_mgr_1";
  const contentMgrUid = "content_mgr_1";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderMgrDb = testEnv.authenticatedContext(orderMgrUid, {
    email: "orders@zeecrown.ng",
    email_verified: true,
  }).firestore();
  const contentMgrDb = testEnv.authenticatedContext(contentMgrUid, {
    email: "content@zeecrown.ng",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Super admin creates adminUsers for order_manager & content_manager
  await assertSucceeds(
    superDb.collection("adminUsers").doc(orderMgrUid).set({
      userId: orderMgrUid,
      email: "orders@zeecrown.ng",
      displayName: "Zainab Orders",
      role: "order_manager",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("adminUsers").doc(contentMgrUid).set({
      userId: contentMgrUid,
      email: "content@zeecrown.ng",
      displayName: "Tolu Content",
      role: "content_manager",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-super admin cannot assign team roles
  await assertFails(
    orderMgrDb.collection("adminUsers").doc("attacker").set({
      userId: "attacker",
      email: "a@b.com",
      role: "super_admin",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seed zone, product & order
  await assertSucceeds(
    superDb.collection("deliveryZones").doc("zone_lagos").set({
      name: "Lagos",
      country: "Nigeria",
      states: ["Lagos"],
      feeNgn: 4000,
      estimatedDays: "1-2 days",
      active: true,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("products").doc("prod_1").set({
      title: "Adire Silk Gown",
      slug: "adire-silk-gown",
      description: "Silk gown",
      categoryId: "cat_1",
      priceNgn: 45000,
      priceUsd: 30,
      imageUrls: [],
      sizes: ["M"],
      colors: ["Rose"],
      featured: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Content manager can manage testimonials & coupons (including perCustomerLimit), CANNOT delete products or verify payments
  await assertSucceeds(
    contentMgrDb.collection("testimonials").doc("test_1").set({
      customerName: "Chidinma",
      rating: 5,
      quote: "The silk drape and finishing exceeded expectations.",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    contentMgrDb.collection("coupons").doc("cp_welcome10").set({
      code: "WELCOME10",
      discountType: "PERCENTAGE",
      discountValue: 10,
      minOrderNgn: 20000,
      usageLimit: 100,
      perCustomerLimit: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    contentMgrDb.collection("coupons").doc("cp_invalid").set({
      code: "BAD",
      discountType: "PERCENTAGE",
      discountValue: 10,
      minOrderNgn: 0,
      perCustomerLimit: -1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(contentMgrDb.collection("products").doc("prod_1").delete());

  // Order manager can list orders & approve payment verification, CANNOT modify storeSettings
  await assertSucceeds(orderMgrDb.collection("orders").get());
  await assertFails(
    orderMgrDb.collection("storeSettings").doc("public").set({
      storeName: "HIJACKED",
      tagline: "X",
      logoPlaceholderText: "X",
      visibility: "public",
      updatedAt: now,
    })
  );

  // Delivery settings & Inventory transactions
  await assertSucceeds(
    superDb.collection("deliverySettings").doc("public").set({
      freeDeliveryThresholdNgn: 150000,
      pickupEnabled: true,
      pickupAddress: "Lekki Phase 1 Studio",
      visibility: "public",
      updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("inventoryTransactions").doc("tx_1").set({
      productId: "prod_1",
      productTitle: "Adire Silk Gown",
      sku: "ZC-1",
      previousQty: 10,
      deltaQty: 5,
      newQty: 15,
      reason: "Restock arrival",
      adminUid: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      createdAt: now,
    })
  );
  // Immutable audit & stock logs: cannot update or delete even as super admin
  await assertFails(superDb.collection("inventoryTransactions").doc("tx_1").update({ newQty: 99 }));
  await assertFails(superDb.collection("inventoryTransactions").doc("tx_1").delete());

  await assertSucceeds(
    orderMgrDb.collection("adminActivityLogs").doc("log_1").set({
      adminUid: orderMgrUid,
      adminEmail: "orders@zeecrown.ng",
      adminRole: "order_manager",
      action: "UPDATE_ORDER_STATUS",
      targetType: "order",
      targetId: "ord_1",
      summary: "Confirmed order after transfer verification",
      createdAt: now,
    })
  );
  await assertFails(superDb.collection("adminActivityLogs").doc("log_1").update({ summary: "tampered" }));
  await assertFails(superDb.collection("adminActivityLogs").doc("log_1").delete());
  await assertFails(aliceDb.collection("adminActivityLogs").get());
});

test("Phase 08 notification preferences, device tokens, and support enquiries enforce owner isolation", async () => {
  const superDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  // Notification preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("preferences").doc("notifications").set({
      userId: ALICE_UID,
      orderUpdatesEmail: true,
      promotionalEmails: false,
      promotionalPush: true,
      smsOrderAlerts: true,
      whatsappOrderAlerts: false,
      essentialSecurityLocked: true,
      consentUpdatedAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("preferences").doc("notifications").get()
  );

  // Device token registration
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("deviceTokens").doc("tok_1").set({
      userId: ALICE_UID,
      token: "fcm_valid_sample_token_123456",
      platform: "ANDROID",
      appVersion: "1.0",
      deviceLabel: "Pixel 9",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("deviceTokens").get()
  );

  // Support enquiry creation & isolation
  await assertSucceeds(
    aliceDb.collection("supportEnquiries").doc("enq_1").set({
      userId: ALICE_UID,
      customerName: "Amaka Okafor",
      customerEmail: "alice@example.com",
      category: "DELIVERY",
      subject: "Tracking update for ZC-1001",
      message: "Could you confirm when my courier rider arrives in Lekki?",
      status: "OPEN",
      priority: "NORMAL",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("supportEnquiries").doc("enq_1").get());
  await assertSucceeds(superDb.collection("supportEnquiries").doc("enq_1").get());

  // Customer reply succeeds for owner; internal admin note forbidden for customers
  await assertSucceeds(
    aliceDb.collection("supportEnquiries").doc("enq_1").collection("replies").doc("rep_1").set({
      enquiryId: "enq_1",
      ownerUid: ALICE_UID,
      senderUid: ALICE_UID,
      senderName: "Amaka Okafor",
      senderRole: "CUSTOMER",
      message: "Thank you!",
      createdAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("supportEnquiries").doc("enq_1").collection("internalNotes").doc("note_1").set({
      enquiryId: "enq_1",
      adminUid: ALICE_UID,
      adminEmail: "alice@example.com",
      note: "Attempted internal note",
      createdAt: now,
    })
  );
});

test("Phase 10 SEO metadata fields and GA4 settings pass schema rules cleanly", async () => {
  const superDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    superDb.collection("categories").doc("cat_seo").set({
      name: "Silk Abayas",
      slug: "silk-abayas",
      description: "Artisanal Nigerian modest silk couture.",
      seoTitle: "Silk Abayas Collection | ZEECROWN STORE",
      seoDescription: "Discover flowing Nigerian silk Abayas hand-finished for timeless modesty.",
      socialImageUrl: "https://cdn.zeecrownstore.com/cat-abayas-og.jpg",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    superDb.collection("collections").doc("col_seo").set({
      name: "Harmattan Edit",
      slug: "harmattan-edit",
      tagline: "Warm terracotta & champagne silk silhouettes.",
      seoTitle: "Harmattan Edit Capsule | ZEECROWN STORE",
      seoDescription: "Shop the limited Harmattan Edit luxury fashion capsule from ZEECROWN STORE.",
      socialImageUrl: "https://cdn.zeecrownstore.com/harmattan-og.jpg",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    superDb.collection("products").doc("prod_seo").set({
      title: "Zaria Sculpted Silk Abaya",
      slug: "zaria-sculpted-silk-abaya",
      description: "Fluid silk crepe abaya crafted in Lagos.",
      categoryId: "cat_seo",
      collectionId: "col_seo",
      priceNgn: 89000,
      priceUsd: 59,
      mainImageUrl: "https://cdn.zeecrownstore.com/zaria-main.jpg",
      socialImageUrl: "https://cdn.zeecrownstore.com/zaria-og.jpg",
      seoTitle: "Zaria Sculpted Silk Abaya | ZEECROWN STORE",
      seoDescription: "Shop the Zaria Sculpted Silk Abaya with fast Lagos & international delivery.",
      imageUrls: ["https://cdn.zeecrownstore.com/zaria-main.jpg"],
      sizes: ["S", "M", "L"],
      colors: ["Plum"],
      featured: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    superDb.collection("storeSettings").doc("public").set({
      storeName: "ZEECROWN STORE",
      tagline: "Style That Speaks. Elegance That Lasts.",
      logoPlaceholderText: "ZEECROWN Official Crest",
      publicSiteBaseUrl: "https://zeecrownstore.com",
      defaultSeoTitle: "ZEECROWN STORE | Luxury Nigerian Boutique",
      defaultSeoDescription: "Luxury Abayas, Adire silk sets, leather slippers & handbags.",
      defaultSocialImageUrl: "https://cdn.zeecrownstore.com/brand-og.jpg",
      facebookUrl: "https://facebook.com/zeecrownstore",
      pinterestUrl: "https://pinterest.com/zeecrownstore",
      youtubeUrl: "https://youtube.com/@zeecrownstore",
      twitterHandle: "@zeecrownstore",
      ga4MeasurementId: "G-ZEECROWN1",
      analyticsConsentRequired: true,
      visibility: "public",
      updatedAt: now,
    })
  );

  // Customer cannot mutate storeSettings or write unknown ghost keys
  await assertFails(
    aliceDb.collection("storeSettings").doc("public").update({
      ga4MeasurementId: "G-ATTACKER1",
      updatedAt: now,
    })
  );
});

test("Phase 11 AI config, FAQs, owner-scoped conversations, unanswered queue, and AI handover enquiries enforce zero-trust isolation", async () => {
  const superDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  // 1. aiConfig/public readable by public/guests, writable only by authorized admin
  await assertSucceeds(
    superDb.collection("aiConfig").doc("public").set({
      enabled: true,
      assistantName: "ZEECROWN AI ASSISTANT",
      humanSupportAvailable: true,
      updatedAt: now,
    })
  );
  await assertSucceeds(unauthDb.collection("aiConfig").doc("public").get());
  await assertFails(
    aliceDb.collection("aiConfig").doc("public").update({
      enabled: false,
      updatedAt: now,
    })
  );

  // 2. aiKnowledgeFaqs: active FAQ readable publicly, inactive FAQ denied to non-admin
  await assertSucceeds(
    superDb.collection("aiKnowledgeFaqs").doc("faq_active").set({
      question: "Do you accept pay on delivery?",
      answer: "Orders are prepaid securely via Paystack or verified Bank Transfer.",
      category: "Payments",
      keywords: ["paystack", "delivery"],
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("aiKnowledgeFaqs").doc("faq_draft").set({
      question: "Internal draft FAQ",
      answer: "Not published yet.",
      category: "General",
      keywords: [],
      active: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(unauthDb.collection("aiKnowledgeFaqs").doc("faq_active").get());
  await assertFails(unauthDb.collection("aiKnowledgeFaqs").doc("faq_draft").get());
  await assertFails(
    aliceDb.collection("aiKnowledgeFaqs").doc("faq_hack").set({
      question: "Fake discount",
      answer: "Everything is free",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 3. aiConversations & messages: strict owner isolation (Alice vs Bob)
  await assertSucceeds(
    aliceDb.collection("aiConversations").doc("conv_alice_1").set({
      userId: ALICE_UID,
      customerName: "Amaka",
      channel: "ANDROID",
      status: "ACTIVE_AI",
      messageCount: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("aiConversations").doc("conv_alice_1").collection("messages").doc("m1").set({
      role: "USER",
      text: "Show Abayas under 40000 naira",
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("aiConversations").doc("conv_alice_1").get());
  await assertFails(bobDb.collection("aiConversations").doc("conv_alice_1").collection("messages").doc("m1").get());
  await assertFails(
    bobDb.collection("aiConversations").doc("conv_alice_1").update({
      status: "RESOLVED_AI",
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("aiConversations").doc("conv_alice_1").update({
      status: "RESOLVED_AI",
      resolvedHelpful: true,
      updatedAt: now,
    })
  );

  // 4. aiUnansweredQuestions: signed-in create + increment +1 only; list denied to customers
  await assertSucceeds(
    aliceDb.collection("aiUnansweredQuestions").doc("uq_1").set({
      question: "Do you offer bridal bespoke measurements in Abuja?",
      normalizedKey: "bridal bespoke measurements abuja",
      reason: "MISSING_STORE_INFO",
      category: "Sizing",
      occurrenceCount: 1,
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("aiUnansweredQuestions").doc("uq_1").update({
      occurrenceCount: 2,
      updatedAt: now,
    })
  );
  await assertFails(aliceDb.collection("aiUnansweredQuestions").get());
  await assertSucceeds(superDb.collection("aiUnansweredQuestions").get());

  // 5. aiRateLimits strictly locked from clients
  await assertFails(aliceDb.collection("aiRateLimits").doc("rl_1").get());

  // 6. AI-escalated support enquiry creation & owner isolation
  await assertSucceeds(
    aliceDb.collection("supportEnquiries").doc("enq_ai_1").set({
      userId: ALICE_UID,
      customerName: "Amaka Okafor",
      customerEmail: "alice@example.com",
      category: "PAYMENT",
      subject: "AI Handover: Double debit check",
      message: "Escalated from ZEECROWN AI Assistant.",
      status: "WAITING_FOR_STAFF",
      priority: "HIGH",
      sourceChannel: "AI_ASSISTANT",
      aiConversationId: "conv_alice_1",
      escalationReason: "Payment dispute",
      aiSummary: "Customer reported transfer pending.",
      transcriptPreview: "Customer: I was debited twice",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("supportEnquiries").doc("enq_ai_1").get());
  await assertSucceeds(superDb.collection("supportEnquiries").doc("enq_ai_1").get());
});

test("Size guides: public reads active charts, drafts & writes restricted to admins, optional sizeGuideId on products/categories", async () => {
  const superDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  // Customer/unauthenticated writes to sizeGuides are denied
  await assertFails(
    aliceDb.collection("sizeGuides").doc("sg_test").set({
      name: "Unauthorized Chart",
      categoryType: "ABAYAS_BUBU",
      columns: ["Size", "Bust (cm)"],
      rows: [{ sizeLabel: "54", values: ["108"] }],
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin creates active and inactive size guides
  await assertSucceeds(
    superDb.collection("sizeGuides").doc("sg_footwear_1").set({
      name: "Slippers & Sandals Guide",
      categoryType: "SLIPPERS_SANDALS",
      description: "EU & Foot Length chart",
      measurementUnitDefault: "CM",
      columns: ["EU Size", "Foot Length (cm)"],
      rows: [{ sizeLabel: "39", values: ["24.8"] }],
      howToMeasureSteps: ["Stand on paper against wall"],
      fitNotes: "True to size",
      assignedCategoryIds: ["cat_slippers"],
      assignedProductIds: [],
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("sizeGuides").doc("sg_draft_1").set({
      name: "Draft Chart",
      categoryType: "CUSTOM",
      columns: ["Size", "Bust (cm)"],
      rows: [],
      sortOrder: 2,
      active: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Guests can read active size guide queries/docs, but not drafts
  await assertSucceeds(unauthDb.collection("sizeGuides").where("active", "==", true).get());
  await assertSucceeds(unauthDb.collection("sizeGuides").doc("sg_footwear_1").get());
  await assertFails(unauthDb.collection("sizeGuides").doc("sg_draft_1").get());
  await assertFails(unauthDb.collection("sizeGuides").get());

  // Admin can update and delete size guides while preserving createdAt
  await assertSucceeds(
    superDb.collection("sizeGuides").doc("sg_footwear_1").update({
      fitNotes: "Size up for wide feet",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    superDb.collection("categories").doc("cat_slippers").set({
      name: "Slippers",
      slug: "slippers",
      description: "Artisanal slippers",
      sizeGuideId: "sg_footwear_1",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("products").doc("prod_slipper_1").set({
      title: "Palm Slide",
      slug: "palm-slide",
      description: "Leather slipper",
      categoryId: "cat_slippers",
      sizeGuideId: "sg_footwear_1",
      priceNgn: 28000,
      priceUsd: 20,
      imageUrls: [],
      sizes: ["38", "39", "40"],
      colors: ["Espresso"],
      featured: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(superDb.collection("sizeGuides").doc("sg_draft_1").delete());
});

test("Public singleton settings (storeSettings/public & deliverySettings/public) allow unseeded get while protecting admin-only docs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const superDb = adminContext().firestore();
  const now = new Date(Date.now() - 2000);

  // Unseeded singleton public docs must return empty snapshot without PERMISSION_DENIED
  await assertSucceeds(unauthDb.collection("storeSettings").doc("public").get());
  await assertSucceeds(unauthDb.collection("deliverySettings").doc("public").get());
  await assertSucceeds(aliceDb.collection("storeSettings").doc("public").get());

  // Non-public doc IDs remain strictly denied to non-admins even when unseeded
  await assertFails(unauthDb.collection("storeSettings").doc("private").get());
  await assertFails(unauthDb.collection("deliverySettings").doc("internal").get());

  // Once seeded with visibility == 'public', guests and customers can read storeSettings/public
  await assertSucceeds(
    superDb.collection("storeSettings").doc("public").set({
      storeName: "ZEECROWN STORE",
      tagline: "Style That Speaks. Elegance That Lasts.",
      logoPlaceholderText: "ZEECROWN",
      visibility: "public",
      updatedAt: now,
    })
  );
  await assertSucceeds(unauthDb.collection("storeSettings").doc("public").get());

  // Admin-only visibility document remains denied to guests/customers
  await assertSucceeds(
    superDb.collection("storeSettings").doc("admin_only").set({
      storeName: "ZEECROWN INTERNAL",
      tagline: "Internal",
      logoPlaceholderText: "ZC",
      visibility: "admin",
      updatedAt: now,
    })
  );
  await assertFails(unauthDb.collection("storeSettings").doc("admin_only").get());
  await assertFails(aliceDb.collection("storeSettings").doc("admin_only").get());
  await assertSucceeds(superDb.collection("storeSettings").doc("admin_only").get());
});




