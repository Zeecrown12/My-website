import {
  doc,
  getDoc,
  serverTimestamp,
  writeBatch,
} from 'firebase/firestore';
import { db } from '../../lib/firebase';
import {
  NO_RETURNS_POLICY_DEFAULT,
  OFFICIAL_STORE_EMAIL,
  OFFICIAL_WHATSAPP_DISPLAY,
} from '../../lib/formatters';
import {
  buildVariantKey,
  serializeVariantMatrix,
  VariantMatrixEntry,
} from '../../lib/variants';
import { AdminActor, recordAdminAudit } from './adminHelpers';

interface SeedDocSpec {
  path: [string, string];
  data: Record<string, unknown>;
}

function buildSearchTokens(parts: string[]): string[] {
  const set = new Set<string>();
  parts
    .join(' ')
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .map((t) => t.trim())
    .filter((t) => t.length >= 2)
    .forEach((t) => set.add(t));
  return Array.from(set).slice(0, 80);
}

export async function activateFoundationStore(
  actor: AdminActor
): Promise<{ createdCount: number; skippedCount: number }> {
  const now = serverTimestamp();

  const starterCategories: SeedDocSpec[] = [
    {
      path: ['categories', 'cat_abayas'],
      data: {
        name: 'Abayas & Bubu Dresses',
        slug: 'abayas-bubu',
        description: 'Flowing silk-crepe Abayas, embellished Bubu gowns and luxury modestwear.',
        imageKey: '/assets/img_cat_adire_abaya.jpg',
        sizeGuideId: 'sg_abayas_bubu',
        sortOrder: 1,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['categories', 'cat_adire'],
      data: {
        name: 'Adire Fabrics & Fashion',
        slug: 'adire-fashion',
        description: 'Authentic Abeokuta silk Adire kaftans, indigo yardage and statement sets.',
        imageKey: '/assets/img_hero_editorial.jpg',
        sizeGuideId: 'sg_adire_kampala_clothing',
        sortOrder: 2,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['categories', 'cat_kampala'],
      data: {
        name: 'Kampala Fabrics & Clothing',
        slug: 'kampala-clothing',
        description: 'Hand-dyed artisanal Kampala boubous, two-pieces and heritage prints.',
        imageKey: '/assets/img_cat_adire_abaya.jpg',
        sizeGuideId: 'sg_adire_kampala_clothing',
        sortOrder: 3,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['categories', 'cat_slippers'],
      data: {
        name: 'Slippers & Footwear',
        slug: 'slippers-footwear',
        description: 'Handcrafted slides, mules and palm-soled luxury slippers.',
        imageKey: '/assets/img_cat_bags_footwear.jpg',
        sizeGuideId: 'sg_slippers_sandals',
        sortOrder: 4,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['categories', 'cat_shoes'],
      data: {
        name: 'Shoes & Sandals',
        slug: 'shoes-sandals',
        description: 'Strappy occasion heels, artisanal sandals and bridal footwear.',
        imageKey: '/assets/img_cat_bags_footwear.jpg',
        sizeGuideId: 'sg_shoes_heels',
        sortOrder: 5,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['categories', 'cat_bags'],
      data: {
        name: 'Bags & Handbags',
        slug: 'bags-handbags',
        description: 'Structured top-handle totes, beaded clutches and crossbody silhouettes.',
        imageKey: '/assets/img_cat_bags_footwear.jpg',
        sortOrder: 6,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['categories', 'cat_jewellery'],
      data: {
        name: 'Jewellery',
        slug: 'jewellery',
        description: '18k gold-plated cuffs, pearl drops and sculptural statement earrings.',
        imageKey: '/assets/img_cat_jewellery_beauty.jpg',
        sortOrder: 7,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['categories', 'cat_beauty'],
      data: {
        name: 'Beauty & Lifestyle',
        slug: 'beauty-lifestyle',
        description: 'Oud-infused body oils, whipped shea elixirs and signature home scents.',
        imageKey: '/assets/img_cat_jewellery_beauty.jpg',
        sortOrder: 8,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
  ];

  const starterSizeGuides: SeedDocSpec[] = [
    {
      path: ['sizeGuides', 'sg_abayas_bubu'],
      data: {
        name: 'Abayas & Bubu Dresses Guide',
        categoryType: 'ABAYAS_BUBU',
        description: 'Relaxed luxury drape measured from shoulder to hem.',
        measurementUnitDefault: 'CM',
        columns: ['Size', 'Height Range', 'Length (cm)', 'Bust Relaxed'],
        rows: [
          { Size: '52', 'Height Range': '5\'1" - 5\'3"', 'Length (cm)': '132', 'Bust Relaxed': '124 cm' },
          { Size: '54', 'Height Range': '5\'4" - 5\'6"', 'Length (cm)': '137', 'Bust Relaxed': '128 cm' },
          { Size: '56', 'Height Range': '5\'7" - 5\'8"', 'Length (cm)': '142', 'Bust Relaxed': '132 cm' },
          { Size: '58', 'Height Range': '5\'9" - 5\'11"', 'Length (cm)': '147', 'Bust Relaxed': '138 cm' },
        ],
        howToMeasureSteps: [
          'Stand upright barefoot and measure from the highest shoulder point straight to the ankle.',
          'Add 2 cm clearance if pairing with platform heels.',
        ],
        fitNotes: 'Cut for an effortless, breathable modest silhouette.',
        sortOrder: 1,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['sizeGuides', 'sg_adire_kampala_clothing'],
      data: {
        name: 'Adire & Kampala Kaftan Guide',
        categoryType: 'CLOTHING_KAFTAN',
        description: 'Tailored & fluid sizing for Abeokuta silk sets.',
        measurementUnitDefault: 'CM',
        columns: ['Size', 'UK Dress', 'Bust (cm)', 'Hip (cm)'],
        rows: [
          { Size: 'S/M', 'UK Dress': '8 - 12', 'Bust (cm)': '88 - 98', 'Hip (cm)': '96 - 106' },
          { Size: 'L/XL', 'UK Dress': '14 - 18', 'Bust (cm)': '102 - 116', 'Hip (cm)': '110 - 124' },
          { Size: 'One Size', 'UK Dress': '8 - 20', 'Bust (cm)': 'Fluid up to 132', 'Hip (cm)': 'Fluid up to 140' },
        ],
        howToMeasureSteps: ['Measure around the fullest part of bust and hip with tape level.'],
        fitNotes: 'Hand-dyed silks relax naturally for comfortable drape.',
        sortOrder: 2,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['sizeGuides', 'sg_slippers_sandals'],
      data: {
        name: 'Slippers & Mules Footwear Guide',
        categoryType: 'SLIPPERS_SANDALS',
        description: 'European sole sizing for handcrafted Nappa mules.',
        measurementUnitDefault: 'CM',
        columns: ['EU Size', 'UK Size', 'Foot Length (cm)'],
        rows: [
          { 'EU Size': 'EU 37', 'UK Size': '4', 'Foot Length (cm)': '23.5 cm' },
          { 'EU Size': 'EU 38', 'UK Size': '5', 'Foot Length (cm)': '24.2 cm' },
          { 'EU Size': 'EU 39', 'UK Size': '6', 'Foot Length (cm)': '24.9 cm' },
          { 'EU Size': 'EU 40', 'UK Size': '7', 'Foot Length (cm)': '25.5 cm' },
          { 'EU Size': 'EU 41', 'UK Size': '8', 'Foot Length (cm)': '26.2 cm' },
        ],
        howToMeasureSteps: ['Trace your foot flat on paper from heel to longest toe.'],
        fitNotes: 'For wider insteps, select one EU size up.',
        sortOrder: 3,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['sizeGuides', 'sg_shoes_heels'],
      data: {
        name: 'Shoes & Occasion Heels Guide',
        categoryType: 'SHOES_HEELS',
        description: 'Evening sandals & bridal footwear conversion.',
        measurementUnitDefault: 'CM',
        columns: ['EU Size', 'US Women', 'Foot Length (cm)'],
        rows: [
          { 'EU Size': 'EU 37', 'US Women': '6.5', 'Foot Length (cm)': '23.5 cm' },
          { 'EU Size': 'EU 38', 'US Women': '7.5', 'Foot Length (cm)': '24.2 cm' },
          { 'EU Size': 'EU 39', 'US Women': '8.5', 'Foot Length (cm)': '24.9 cm' },
          { 'EU Size': 'EU 40', 'US Women': '9.5', 'Foot Length (cm)': '25.5 cm' },
          { 'EU Size': 'EU 41', 'US Women': '10.5', 'Foot Length (cm)': '26.2 cm' },
        ],
        sortOrder: 4,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
  ];

  const starterCollections: SeedDocSpec[] = [
    {
      path: ['collections', 'col_zeecrown_edit'],
      data: {
        name: 'The Zeecrown Edit',
        slug: 'zeecrown-edit',
        tagline: 'Curated resort silhouettes & champagne-finish essentials.',
        bannerKey: '/assets/img_zeecrown_edit_campaign.jpg',
        sortOrder: 1,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['collections', 'col_heritage_silk'],
      data: {
        name: 'Abeokuta Heritage Silk',
        slug: 'heritage-silk',
        tagline: 'Small-batch resist-dyed Adire & Kampala capsule.',
        bannerKey: '/assets/img_cat_adire_abaya.jpg',
        sortOrder: 2,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
  ];

  const starterZones: SeedDocSpec[] = [
    {
      path: ['deliveryZones', 'zone_lagos'],
      data: {
        name: 'Lagos Express Dispatch',
        country: 'Nigeria',
        states: ['Lagos'],
        cities: ['Ikoyi', 'Victoria Island', 'Lekki', 'Ikeja', 'Surulere'],
        feeNgn: 3500,
        estimatedDays: '1 - 2 business days',
        promotionNote: 'Same-day dispatch on confirmed orders before 1 PM',
        active: true,
        updatedAt: now,
      },
    },
    {
      path: ['deliveryZones', 'zone_nigeria_interstate'],
      data: {
        name: 'Nationwide Nigeria (Abuja, PH, Ibadan)',
        country: 'Nigeria',
        states: ['FCT Abuja', 'Rivers', 'Oyo', 'Ogun', 'Kano', 'Enugu'],
        feeNgn: 6500,
        estimatedDays: '2 - 4 business days',
        active: true,
        updatedAt: now,
      },
    },
    {
      path: ['deliveryZones', 'zone_international'],
      data: {
        name: 'International DHL Priority',
        country: 'International (UK, US, EU, Canada)',
        states: ['Worldwide'],
        feeNgn: 42000,
        estimatedDays: '4 - 7 business days',
        active: true,
        updatedAt: now,
      },
    },
  ];

  const settingsSpecs: SeedDocSpec[] = [
    {
      path: ['deliverySettings', 'public'],
      data: {
        freeDeliveryThresholdNgn: 150000,
        pickupEnabled: true,
        pickupAddress: 'Lekki Phase 1 Showroom, Lagos, Nigeria',
        pickupHours: 'Mon - Sat · 10:00 AM - 6:00 PM',
        specialPromoNote: 'Complimentary Lagos Express delivery on orders over ₦150,000.',
        visibility: 'public',
        updatedAt: now,
      },
    },
    {
      path: ['storeSettings', 'public'],
      data: {
        storeName: 'ZEECROWN STORE',
        tagline: 'Style That Speaks. Elegance That Lasts.',
        logoPlaceholderText: 'ZEECROWN',
        bankName: 'Providus Bank (Configure in Settings)',
        accountName: 'ZEECROWN STORE',
        accountNumber: 'Configure in Admin Settings',
        paystackEnabled: true,
        flutterwaveEnabled: false,
        bankTransferEnabled: true,
        codEnabled: true,
        guestCheckoutEnabled: true,
        maintenanceMode: false,
        supportEmail: OFFICIAL_STORE_EMAIL,
        supportPhone: OFFICIAL_WHATSAPP_DISPLAY,
        whatsappNumber: OFFICIAL_WHATSAPP_DISPLAY,
        businessAddress: 'Lekki Phase 1, Lagos, Nigeria',
        supportHours: 'Mon - Sat · 9:00 AM - 6:00 PM WAT',
        defaultCurrency: 'NGN',
        aboutText:
          'ZEECROWN STORE curates Nigerian and global luxury modest fashion, handcrafted leather footwear, Adire silk garments, and heirloom accessories.',
        shippingPolicy:
          'Orders dispatch within 24 hours of verified settlement across Lagos, nationwide Nigeria, and worldwide.',
        returnPolicy: NO_RETURNS_POLICY_DEFAULT,
        privacyPolicy:
          'Customer details and order records are secured via Firebase Authentication and role-based Firestore security rules.',
        termsPolicy:
          'All orders are subject to item availability and payment confirmation before dispatch.',
        instagramHandle: '@zeecrownstore',
        tiktokHandle: '@zeecrownstore',
        visibility: 'public',
        updatedAt: now,
      },
    },
    {
      path: ['aiConfig', 'public'],
      data: {
        enabled: true,
        assistantName: 'ZEECROWN CUSTOMER CARE',
        toneStyle: 'LUXURY_WARM',
        humanSupportAvailable: true,
        supportWorkingDays: 'Mon – Sat',
        supportWorkingHours: '9:00 AM – 6:00 PM WAT',
        expectedResponseTimeMessage: 'Our specialists respond promptly during studio hours.',
        updatedAt: now,
      },
    },
    {
      path: ['homepageContent', 'hero_main'],
      data: {
        title: 'STYLE THAT SPEAKS. ELEGANCE THAT LASTS.',
        subtitle:
          'Hand-crafted Nigerian Adire silks, flowing Abayas, Italian leather mules & warm atelier finishes.',
        ctaLabel: 'SHOP COLLECTION',
        secondaryCtaLabel: 'NEW ARRIVALS',
        sectionType: 'HERO',
        targetCategorySlug: '',
        imageKey: '/assets/img_hero_editorial.jpg',
        sortOrder: 1,
        active: true,
        updatedAt: now,
      },
    },
    {
      path: ['coupons', 'WELCOME10'],
      data: {
        code: 'WELCOME10',
        description: '10% boutique welcome privilege on orders over ₦40,000',
        discountType: 'PERCENTAGE',
        discountValue: 10,
        minOrderNgn: 40000,
        maxDiscountNgn: 25000,
        usageLimit: 500,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
    {
      path: ['testimonials', 'test_lagos_1'],
      data: {
        customerName: 'Aisha Bello',
        location: 'Ikoyi, Lagos',
        rating: 5,
        quote: 'The silk crepe Abaya draped like couture and arrived same afternoon in Ikoyi.',
        productTitle: 'Zara Rose-Crepe Beaded Abaya',
        sortOrder: 1,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    },
  ];

  const starterProductsRaw = [
    {
      id: 'prod_royal_silk_abaya',
      title: 'Zara Rose-Crepe Beaded Abaya',
      slug: 'zara-rose-crepe-beaded-abaya',
      description:
        'Fluid Japanese silk-crepe Abaya with hand-beaded champagne cuff detailing and matching sheila scarf.',
      summary: 'Hand-beaded Japanese silk-crepe Abaya with matching sheila.',
      categoryId: 'cat_abayas',
      collectionId: 'col_zeecrown_edit',
      sizeGuideId: 'sg_abayas_bubu',
      priceNgn: 68000,
      compareAtPriceNgn: 78000,
      priceUsd: 48,
      mainImageUrl: '/assets/img_cat_adire_abaya.jpg',
      imageUrls: ['/assets/img_cat_adire_abaya.jpg', '/assets/img_zeecrown_edit_campaign.jpg'],
      sizes: ['52', '54', '56', '58'],
      colors: ['Dusty Rose', 'Warm Espresso', 'Ivory'],
      featured: true,
      newArrival: true,
      brand: 'Zeecrown Atelier',
      sku: 'ZC-ABY-001',
      attributes: { Fabric: 'Japanese Silk Crepe', Origin: 'Lagos Atelier' },
      lowStockThreshold: 3,
      defaultStockPerVariant: 2,
    },
    {
      id: 'prod_abeokuta_adire_bubu',
      title: 'Abeokuta Indigo Silk Adire Bubu',
      slug: 'abeokuta-indigo-silk-adire-bubu',
      description:
        'Artisanal resist-dyed mulberry silk-blend Bubu kaftan crafted in Abeokuta with draped kimono sleeves.',
      summary: 'Resist-dyed Abeokuta silk-blend Bubu with kimono drape.',
      categoryId: 'cat_adire',
      collectionId: 'col_heritage_silk',
      sizeGuideId: 'sg_adire_kampala_clothing',
      priceNgn: 54000,
      priceUsd: 38,
      mainImageUrl: '/assets/img_hero_editorial.jpg',
      imageUrls: ['/assets/img_hero_editorial.jpg', '/assets/img_cat_adire_abaya.jpg'],
      sizes: ['S/M', 'L/XL', 'One Size'],
      colors: ['Indigo Mist', 'Blush Terracotta'],
      featured: true,
      newArrival: true,
      brand: 'Zeecrown Heritage',
      sku: 'ZC-ADR-002',
      attributes: { Craft: 'Hand-dyed Adire', Care: 'Dry Clean / Cold Hand Wash' },
      lowStockThreshold: 4,
      defaultStockPerVariant: 3,
    },
    {
      id: 'prod_champagne_leather_mule',
      title: 'Amina Braided Leather Mule Slippers',
      slug: 'amina-braided-leather-mule-slippers',
      description:
        'Cushioned square-toe Nappa leather slides finished with brushed champagne-gold hardware.',
      summary: 'Cushioned Nappa leather slides with champagne-gold hardware.',
      categoryId: 'cat_slippers',
      collectionId: 'col_zeecrown_edit',
      sizeGuideId: 'sg_slippers_sandals',
      priceNgn: 38500,
      compareAtPriceNgn: 44000,
      priceUsd: 28,
      mainImageUrl: '/assets/img_cat_bags_footwear.jpg',
      imageUrls: ['/assets/img_cat_bags_footwear.jpg'],
      sizes: ['EU 37', 'EU 38', 'EU 39', 'EU 40', 'EU 41'],
      colors: ['Champagne', 'Espresso', 'Blush'],
      featured: true,
      newArrival: false,
      brand: 'Zeecrown Footwear',
      sku: 'ZC-SLP-003',
      attributes: { Upper: 'Supple Nappa Leather', Sole: 'Anti-Slip Cushioned' },
      lowStockThreshold: 4,
      defaultStockPerVariant: 2,
    },
    {
      id: 'prod_pearl_drop_cuff',
      title: 'Kemi 18k Gold-Plated Pearl Set',
      slug: 'kemi-18k-gold-plated-pearl-set',
      description:
        'Hypoallergenic 18k gold-vermeil sculptural cuff paired with baroque freshwater pearl drop earrings.',
      summary: '18k gold-vermeil cuff and freshwater pearl drop earrings.',
      categoryId: 'cat_jewellery',
      collectionId: 'col_zeecrown_edit',
      priceNgn: 29000,
      priceUsd: 22,
      mainImageUrl: '/assets/img_cat_jewellery_beauty.jpg',
      imageUrls: ['/assets/img_cat_jewellery_beauty.jpg'],
      sizes: ['One Size'],
      colors: ['Champagne Gold'],
      featured: false,
      newArrival: true,
      brand: 'Zeecrown Jewels',
      sku: 'ZC-JWL-004',
      attributes: { Plating: '18k Gold Vermeil', Stone: 'Freshwater Pearl' },
      lowStockThreshold: 3,
      defaultStockPerVariant: 9,
    },
  ];

  const productSpecs: SeedDocSpec[] = [];
  for (const p of starterProductsRaw) {
    const entries: VariantMatrixEntry[] = [];
    const variantStock: Record<string, number> = {};
    p.sizes.forEach((size, sIdx) => {
      p.colors.forEach((color, cIdx) => {
        const key = buildVariantKey(size, color);
        const skuCode = `${p.sku}-${String(sIdx + 1)}${String(cIdx + 1)}`;
        entries.push({
          key,
          size,
          color,
          sku: skuCode,
          priceNgn: p.priceNgn,
          priceUsd: p.priceUsd,
        });
        variantStock[key] = p.defaultStockPerVariant;
      });
    });
    const variantMatrixJson = serializeVariantMatrix(entries);
    const totalQty = Object.values(variantStock).reduce((sum, q) => sum + q, 0);

    productSpecs.push({
      path: ['products', p.id],
      data: {
        title: p.title,
        slug: p.slug,
        description: p.description,
        summary: p.summary,
        categoryId: p.categoryId,
        collectionId: p.collectionId,
        sizeGuideId: p.sizeGuideId || '',
        priceNgn: p.priceNgn,
        ...(p.compareAtPriceNgn ? { compareAtPriceNgn: p.compareAtPriceNgn } : {}),
        priceUsd: p.priceUsd,
        mainImageUrl: p.mainImageUrl,
        imageUrls: p.imageUrls,
        sizes: p.sizes,
        colors: p.colors,
        featured: p.featured,
        newArrival: p.newArrival,
        brand: p.brand,
        sku: p.sku,
        attributes: {
          ...p.attributes,
          ...(variantMatrixJson ? { variantMatrixJson } : {}),
        },
        searchTokens: buildSearchTokens([p.title, p.description, p.brand, p.sku, ...p.sizes, ...p.colors]),
        lowStockThreshold: p.lowStockThreshold,
        active: true,
        createdAt: now,
        updatedAt: now,
      },
    });

    productSpecs.push({
      path: ['inventory', p.id],
      data: {
        productId: p.id,
        sku: p.sku,
        stockQuantity: totalQty,
        lowStockThreshold: p.lowStockThreshold,
        reservedQuantity: 0,
        variantStock,
        inStock: totalQty > 0,
        updatedAt: now,
      },
    });
  }

  const allSpecs: SeedDocSpec[] = [
    ...starterCategories,
    ...starterSizeGuides,
    ...starterCollections,
    ...starterZones,
    ...settingsSpecs,
    ...productSpecs,
  ];

  // Check existence first so we never violate createdAt == existing().createdAt on update or overwrite custom edits
  const snapshots = await Promise.all(
    allSpecs.map(async (spec) => ({
      spec,
      exists: (await getDoc(doc(db, spec.path[0], spec.path[1]))).exists(),
    }))
  );

  const missing = snapshots.filter((item) => !item.exists);
  if (missing.length === 0) {
    return { createdCount: 0, skippedCount: allSpecs.length };
  }

  const batch = writeBatch(db);
  for (const { spec } of missing) {
    batch.set(doc(db, spec.path[0], spec.path[1]), spec.data);
  }
  await batch.commit();

  await recordAdminAudit(
    actor,
    'ACTIVATE_FOUNDATION_STORE',
    'store',
    'zeecrown',
    `Activated starter store database (${missing.length} documents created, ${allSpecs.length - missing.length} preserved)`
  );

  return {
    createdCount: missing.length,
    skippedCount: allSpecs.length - missing.length,
  };
}
