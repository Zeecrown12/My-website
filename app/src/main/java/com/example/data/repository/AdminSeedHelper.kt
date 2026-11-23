package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminRole
import com.example.data.model.BoutiqueContactHelper
import com.example.data.model.CatalogQueryEngine
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.DefaultSizeGuides
import com.example.data.model.DeliverySettings
import com.example.data.model.DeliveryZone
import com.example.data.model.HomepageContent
import com.example.data.model.InventoryRecord
import com.example.data.model.Product
import com.example.data.model.StoreCollection
import com.example.data.model.StoreSettings
import com.example.data.model.Testimonial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object AdminSeedHelper {

    suspend fun seedFoundationCatalog(
        db: FirebaseFirestore,
        auth: FirebaseAuth,
        actorRole: AdminRole,
    ): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("Administrator must be signed in.")
            check(actorRole.isAdmin) { "Administrator permission required." }

            val batch = db.batch()

            val categories = listOf(
                "cat_slippers" to Category(
                    name = "Slippers & Footwear",
                    slug = "slippers-footwear",
                    description = "Handcrafted slides, mules and palm-soled luxury slippers.",
                    imageKey = "img_cat_bags_footwear",
                    sizeGuideId = "sg_slippers_sandals",
                    sortOrder = 1L,
                    active = true,
                ),
                "cat_shoes" to Category(
                    name = "Shoes & Sandals",
                    slug = "shoes-sandals",
                    description = "Strappy occasion heels, artisanal sandals and bridal footwear.",
                    imageKey = "img_cat_bags_footwear",
                    sizeGuideId = "sg_shoes_heels",
                    sortOrder = 2L,
                    active = true,
                ),
                "cat_bags" to Category(
                    name = "Bags & Handbags",
                    slug = "bags-handbags",
                    description = "Structured top-handle totes, beaded clutches and crossbody silhouettes.",
                    imageKey = "img_cat_bags_footwear",
                    sortOrder = 3L,
                    active = true,
                ),
                "cat_adire" to Category(
                    name = "Adire Fabrics & Fashion",
                    slug = "adire-fashion",
                    description = "Authentic Abeokuta silk Adire kaftans, indigo yardage and statement sets.",
                    imageKey = "img_cat_adire_abaya",
                    sizeGuideId = "sg_adire_kampala_clothing",
                    sortOrder = 4L,
                    active = true,
                ),
                "cat_kampala" to Category(
                    name = "Kampala Fabrics & Clothing",
                    slug = "kampala-clothing",
                    description = "Hand-dyed artisanal Kampala boubous, two-pieces and heritage prints.",
                    imageKey = "img_cat_adire_abaya",
                    sizeGuideId = "sg_adire_kampala_clothing",
                    sortOrder = 5L,
                    active = true,
                ),
                "cat_abayas" to Category(
                    name = "Abayas & Bubu Dresses",
                    slug = "abayas-bubu",
                    description = "Flowing silk-crepe Abayas, embellished Bubu gowns and luxury modestwear.",
                    imageKey = "img_cat_adire_abaya",
                    sizeGuideId = "sg_abayas_bubu",
                    sortOrder = 6L,
                    active = true,
                ),
                "cat_jewellery" to Category(
                    name = "Jewellery",
                    slug = "jewellery",
                    description = "18k gold-plated cuffs, pearl drops and sculptural statement earrings.",
                    imageKey = "img_cat_jewellery_beauty",
                    sortOrder = 7L,
                    active = true,
                ),
                "cat_beauty" to Category(
                    name = "Beauty & Lifestyle",
                    slug = "beauty-lifestyle",
                    description = "Oud-infused body oils, whipped shea elixirs and signature home scents.",
                    imageKey = "img_cat_jewellery_beauty",
                    sortOrder = 8L,
                    active = true,
                ),
            )
            categories.forEach { (id, cat) ->
                batch.set(db.collection("categories").document(id), cat.toCreateMap())
            }

            DefaultSizeGuides.starterGuides().forEach { (id, guide) ->
                batch.set(db.collection("sizeGuides").document(id), guide.toCreateMap(isCreate = true))
            }

            val collections = listOf(
                "col_zeecrown_edit" to StoreCollection(
                    name = "The Zeecrown Edit",
                    slug = "zeecrown-edit",
                    tagline = "Curated resort silhouettes & champagne-finish essentials.",
                    bannerKey = "img_zeecrown_edit_campaign",
                    sortOrder = 1L,
                    active = true,
                ),
                "col_heritage_silk" to StoreCollection(
                    name = "Abeokuta Heritage Silk",
                    slug = "heritage-silk",
                    tagline = "Small-batch resist-dyed Adire & Kampala capsule.",
                    bannerKey = "img_cat_adire_abaya",
                    sortOrder = 2L,
                    active = true,
                ),
            )
            collections.forEach { (id, col) ->
                batch.set(db.collection("collections").document(id), col.toCreateMap())
            }

            val zones = listOf(
                "zone_lagos" to DeliveryZone(
                    name = "Lagos Express Dispatch",
                    country = "Nigeria",
                    states = listOf("Lagos"),
                    cities = listOf("Ikoyi", "Victoria Island", "Lekki", "Ikeja", "Surulere"),
                    feeNgn = 3500.0,
                    estimatedDays = "1 - 2 business days",
                    promotionNote = "Same-day dispatch on confirmed orders before 1 PM",
                    active = true,
                ),
                "zone_nigeria_interstate" to DeliveryZone(
                    name = "Nationwide Nigeria (Abuja, PH, Ibadan)",
                    country = "Nigeria",
                    states = listOf("FCT Abuja", "Rivers", "Oyo", "Ogun", "Kano", "Enugu"),
                    feeNgn = 6500.0,
                    estimatedDays = "2 - 4 business days",
                    active = true,
                ),
                "zone_international" to DeliveryZone(
                    name = "International DHL Priority",
                    country = "International (UK, US, EU, Canada)",
                    states = listOf("Worldwide"),
                    feeNgn = 42000.0,
                    estimatedDays = "4 - 7 business days",
                    active = true,
                ),
            )
            zones.forEach { (id, zone) ->
                batch.set(db.collection("deliveryZones").document(id), zone.toCreateMap())
            }

            batch.set(
                db.collection("deliverySettings").document("public"),
                DeliverySettings(
                    freeDeliveryThresholdNgn = 150000.0,
                    pickupEnabled = true,
                    pickupAddress = "Lekki Phase 1 Showroom, Lagos, Nigeria",
                    pickupHours = "Mon - Sat · 10:00 AM - 6:00 PM",
                    specialPromoNote = "Complimentary Lagos Express delivery on orders over ₦150,000.",
                ).toWriteMap()
            )

            val homepageBanners = listOf(
                "hero_main" to HomepageContent(
                    title = "STYLE THAT SPEAKS. ELEGANCE THAT LASTS.",
                    subtitle = "Hand-crafted Nigerian Adire silks, flowing Abayas, Italian leather mules & warm atelier finishes.",
                    ctaLabel = "SHOP COLLECTION",
                    secondaryCtaLabel = "NEW ARRIVALS",
                    sectionType = "HERO",
                    targetCategorySlug = "",
                    imageKey = "img_hero_editorial",
                    sortOrder = 1L,
                    active = true,
                ),
                "featured_edit" to HomepageContent(
                    title = "THE ZEECROWN EDIT",
                    subtitle = "Luminous silk Abayas, artisanal Abeokuta Adire & sculpted champagne gold accessories.",
                    ctaLabel = "SHOP THE EDIT",
                    sectionType = "FEATURED_EDIT",
                    targetCategorySlug = "abayas-bubu",
                    imageKey = "img_zeecrown_edit_campaign",
                    sortOrder = 2L,
                    active = true,
                ),
                "promo_adire" to HomepageContent(
                    title = "ABEOKUTA SILK ADIRE",
                    subtitle = "Small-batch indigo & rosewood dyed silhouettes.",
                    ctaLabel = "EXPLORE ADIRE",
                    sectionType = "PROMO",
                    targetCategorySlug = "adire-fashion",
                    imageKey = "img_cat_adire_abaya",
                    sortOrder = 3L,
                    active = true,
                ),
            )
            homepageBanners.forEach { (id, banner) ->
                batch.set(db.collection("homepageContent").document(id), banner.toCreateMap())
            }

            batch.set(
                db.collection("storeSettings").document("public"),
                StoreSettings(
                    storeName = "ZEECROWN STORE",
                    tagline = "Style That Speaks. Elegance That Lasts.",
                    bankName = "Providus Bank (Configure in Settings)",
                    accountName = "ZEECROWN STORE",
                    accountNumber = "Configure in Admin Settings",
                    paystackEnabled = true,
                    flutterwaveEnabled = true,
                    bankTransferEnabled = true,
                    codEnabled = false,
                    supportEmail = "concierge@zeecrownstore.com",
                    supportPhone = BoutiqueContactHelper.OFFICIAL_WHATSAPP_NUMBER_DISPLAY,
                    whatsappNumber = BoutiqueContactHelper.OFFICIAL_WHATSAPP_NUMBER_DISPLAY,
                    businessAddress = "Lekki Phase 1, Lagos, Nigeria",
                    shippingPolicy = "Orders dispatch within 24 hours of verified settlement.",
                    returnPolicy = "ZEECROWN STORE currently does not accept customer returns or refunds. Please review our Size Guide or message us on WhatsApp before purchasing if you need fit guidance.",
                ).toWriteMap()
            )

            batch.set(
                db.collection("coupons").document("WELCOME10"),
                Coupon(
                    code = "WELCOME10",
                    description = "10% boutique welcome privilege on orders over ₦40,000",
                    discountType = "PERCENTAGE",
                    discountValue = 10.0,
                    minOrderNgn = 40000.0,
                    maxDiscountNgn = 25000.0,
                    usageLimit = 500L,
                    active = true,
                ).toCreateMap()
            )

            val starterProducts = listOf(
                "prod_royal_silk_abaya" to Triple(
                    Product(
                        title = "Zara Rose-Crepe Beaded Abaya",
                        slug = "zara-rose-crepe-beaded-abaya",
                        description = "Fluid Japanese silk-crepe Abaya with hand-beaded champagne cuff detailing and matching sheila scarf.",
                        categoryId = "cat_abayas",
                        collectionId = "col_zeecrown_edit",
                        priceNgn = 68000.0,
                        compareAtPriceNgn = 78000.0,
                        priceUsd = 48.0,
                        mainImageUrl = "drawable://img_cat_adire_abaya",
                        imageUrls = listOf("drawable://img_cat_adire_abaya", "drawable://img_zeecrown_edit_campaign"),
                        sizes = listOf("52", "54", "56", "58"),
                        colors = listOf("Dusty Rose", "Warm Espresso", "Ivory"),
                        featured = true,
                        newArrival = true,
                        brand = "Zeecrown Atelier",
                        sku = "ZC-ABY-001",
                        attributes = mapOf("Fabric" to "Japanese Silk Crepe", "Origin" to "Lagos Atelier"),
                        lowStockThreshold = 3L,
                        active = true,
                    ),
                    14L,
                    "Abayas & Bubu Dresses",
                ),
                "prod_abeokuta_adire_bubu" to Triple(
                    Product(
                        title = "Abeokuta Indigo Silk Adire Bubu",
                        slug = "abeokuta-indigo-silk-adire-bubu",
                        description = "Artisanal resist-dyed mulberry silk-blend Bubu kaftan crafted in Abeokuta with draped kimono sleeves.",
                        categoryId = "cat_adire",
                        collectionId = "col_heritage_silk",
                        priceNgn = 54000.0,
                        priceUsd = 38.0,
                        mainImageUrl = "drawable://img_hero_editorial",
                        imageUrls = listOf("drawable://img_hero_editorial", "drawable://img_cat_adire_abaya"),
                        sizes = listOf("S/M", "L/XL", "One Size"),
                        colors = listOf("Indigo Mist", "Blush Terracotta"),
                        featured = true,
                        newArrival = true,
                        brand = "Zeecrown Heritage",
                        sku = "ZC-ADR-002",
                        attributes = mapOf("Craft" to "Hand-dyed Adire", "Care" to "Dry Clean / Cold Hand Wash"),
                        lowStockThreshold = 4L,
                        active = true,
                    ),
                    11L,
                    "Adire Fabrics & Fashion",
                ),
                "prod_champagne_leather_mule" to Triple(
                    Product(
                        title = "Amina Braided Leather Mule Slippers",
                        slug = "amina-braided-leather-mule-slippers",
                        description = "Cushioned square-toe Nappa leather slides finished with brushed champagne-gold hardware.",
                        categoryId = "cat_slippers",
                        collectionId = "col_zeecrown_edit",
                        priceNgn = 38500.0,
                        compareAtPriceNgn = 44000.0,
                        priceUsd = 28.0,
                        mainImageUrl = "drawable://img_cat_bags_footwear",
                        imageUrls = listOf("drawable://img_cat_bags_footwear"),
                        sizes = listOf("EU 37", "EU 38", "EU 39", "EU 40", "EU 41"),
                        colors = listOf("Champagne", "Espresso", "Blush"),
                        featured = true,
                        newArrival = false,
                        brand = "Zeecrown Footwear",
                        sku = "ZC-SLP-003",
                        attributes = mapOf("Upper" to "Supple Nappa Leather", "Sole" to "Anti-Slip Cushioned"),
                        lowStockThreshold = 4L,
                        active = true,
                    ),
                    18L,
                    "Slippers & Footwear",
                ),
                "prod_pearl_drop_cuff" to Triple(
                    Product(
                        title = "Kemi 18k Gold-Plated Pearl Set",
                        slug = "kemi-18k-gold-plated-pearl-set",
                        description = "Hypoallergenic 18k gold-vermeil sculptural cuff paired with baroque freshwater pearl drop earrings.",
                        categoryId = "cat_jewellery",
                        collectionId = "col_zeecrown_edit",
                        priceNgn = 29000.0,
                        priceUsd = 22.0,
                        mainImageUrl = "drawable://img_cat_jewellery_beauty",
                        imageUrls = listOf("drawable://img_cat_jewellery_beauty"),
                        sizes = listOf("One Size"),
                        colors = listOf("Champagne Gold"),
                        featured = false,
                        newArrival = true,
                        brand = "Zeecrown Jewels",
                        sku = "ZC-JWL-004",
                        attributes = mapOf("Plating" to "18k Gold Vermeil", "Stone" to "Freshwater Pearl"),
                        lowStockThreshold = 3L,
                        active = true,
                    ),
                    9L,
                    "Jewellery",
                ),
            )

            starterProducts.forEach { (id, triple) ->
                val (prod, qty, catName) = triple
                val tokens = CatalogQueryEngine.buildSearchTokens(
                    title = prod.title,
                    description = prod.description,
                    categoryName = catName,
                    collectionName = "The Zeecrown Edit",
                    brand = prod.brand,
                    sku = prod.sku,
                    sizes = prod.sizes,
                    colors = prod.colors,
                )
                batch.set(db.collection("products").document(id), prod.copy(searchTokens = tokens).toCreateMap())
                batch.set(
                    db.collection("inventory").document(id),
                    InventoryRecord(
                        productId = id,
                        sku = prod.sku,
                        stockQuantity = qty,
                        lowStockThreshold = prod.lowStockThreshold,
                        inStock = qty > 0L,
                    ).toCreateMap()
                )
            }

            batch.set(
                db.collection("testimonials").document("test_lagos_1"),
                Testimonial(
                    customerName = "Aisha Bello",
                    location = "Ikoyi, Lagos",
                    rating = 5L,
                    quote = "The silk crepe Abaya draped like couture and arrived same afternoon in Ikoyi.",
                    productTitle = "Zara Rose-Crepe Beaded Abaya",
                    sortOrder = 1L,
                    active = true,
                ).toCreateMap()
            )

            batch.set(
                db.collection("adminActivityLogs").document(),
                AdminActivityLog(
                    adminUid = user.uid,
                    adminEmail = user.email.orEmpty(),
                    adminRole = actorRole.rawKey,
                    action = "SEED_FOUNDATION_STORE",
                    targetType = "store",
                    targetId = "zeecrown",
                    summary = "Initialized foundation categories, collections, products, zones, and public settings",
                ).toCreateMap()
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seedFoundationCatalog")
            Result.failure(e)
        }
    }
}
