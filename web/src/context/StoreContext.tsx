import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import {
  User,
  onAuthStateChanged,
  signOut as firebaseSignOut,
} from 'firebase/auth';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';
import {
  AdminRole,
  AiAssistantConfig,
  CartItem,
  Category,
  CurrencyCode,
  DeliveryAddress,
  DeliverySettings,
  DeliveryZone,
  InventoryItem,
  Product,
  SizeGuide,
  StoreCollection,
  StoreOrder,
  StoreSettings,
  WishlistItem,
} from '../types/store';
import {
  auth,
  BOOTSTRAPPED_ADMIN_EMAIL,
  buildTrackingLookupId,
  db,
  generateTrackingToken,
} from '../lib/firebase';
import {
  NO_RETURNS_POLICY_DEFAULT,
  OFFICIAL_STORE_EMAIL,
  OFFICIAL_WHATSAPP_DISPLAY,
  resolveEffectiveReturnPolicy,
  resolveEffectiveSupportEmail,
  resolveProductImageUrl,
} from '../lib/formatters';

interface PlaceOrderInput {
  deliveryZoneId: string;
  deliveryFeeNgn: number;
  shippingCity: string;
  shippingState: string;
  shippingCountry: string;
  recipientName: string;
  recipientPhone: string;
  shippingAddress: string;
  lga?: string;
  landmark?: string;
  deliveryInstructions?: string;
  paymentMethod: 'PAYSTACK' | 'FLUTTERWAVE' | 'BANK_TRANSFER' | 'COD';
  transferNote?: string;
}

interface StoreContextValue {
  user: User | null;
  authLoading: boolean;
  isAdmin: boolean;
  adminRole: AdminRole | 'none';
  currency: CurrencyCode;
  setCurrency: (c: CurrencyCode) => void;
  products: Product[];
  allProducts: Product[];
  categories: Category[];
  allCategories: Category[];
  collections: StoreCollection[];
  inventoryByProduct: Record<string, InventoryItem>;
  deliveryZones: DeliveryZone[];
  allDeliveryZones: DeliveryZone[];
  deliverySettings: DeliverySettings;
  sizeGuides: SizeGuide[];
  storeSettings: StoreSettings;
  aiConfig: AiAssistantConfig;
  catalogLoading: boolean;
  catalogError: string | null;
  cart: CartItem[];
  wishlist: WishlistItem[];
  orders: StoreOrder[];
  addresses: DeliveryAddress[];
  addToCart: (
    product: Product,
    variantLabel: string,
    qty?: number,
    unitPriceOverrideNgn?: number
  ) => Promise<void>;
  updateCartQty: (itemId: string, qty: number) => Promise<void>;
  removeFromCart: (itemId: string) => Promise<void>;
  clearCart: () => Promise<void>;
  toggleWishlist: (product: Product) => Promise<void>;
  isWishlisted: (productId: string) => boolean;
  saveAddress: (address: Omit<DeliveryAddress, 'id' | 'userId'>) => Promise<void>;
  deleteAddress: (addressId: string) => Promise<void>;
  placeOrder: (
    input: PlaceOrderInput
  ) => Promise<{ orderNumber: string; orderId: string; paymentId: string; trackingToken: string }>;
  submitGatewayPaymentReference: (params: {
    orderId: string;
    paymentId: string;
    gatewayRef: string;
    provider: 'PAYSTACK' | 'FLUTTERWAVE';
    trackingToken?: string;
  }) => Promise<void>;
  signOut: () => Promise<void>;
  refreshCatalog: (includeInactiveForAdmin?: boolean) => Promise<void>;
}

const DEFAULT_SETTINGS: StoreSettings = {
  storeName: 'ZEECROWN STORE',
  tagline: 'Style That Speaks. Elegance That Lasts.',
  supportEmail: OFFICIAL_STORE_EMAIL,
  supportPhone: OFFICIAL_WHATSAPP_DISPLAY,
  whatsappNumber: OFFICIAL_WHATSAPP_DISPLAY,
  aboutText:
    'ZEECROWN STORE curates Nigerian and global luxury modest fashion, handcrafted leather footwear, Adire silk garments, and heirloom accessories.',
  shippingPolicy:
    'Orders are dispatched via trusted Nigerian & international couriers according to your selected delivery zone.',
  returnPolicy: NO_RETURNS_POLICY_DEFAULT,
  privacyPolicy:
    'Customer details and order records are secured via Firebase Authentication and role-based Firestore security rules.',
  termsPolicy:
    'All orders are subject to item availability and payment confirmation before dispatch.',
  defaultCurrency: 'NGN',
  usdRateNgn: 1600,
  paystackEnabled: true,
  paystackPublicKey: '',
  flutterwaveEnabled: false,
  flutterwavePublicKey: '',
  bankTransferEnabled: true,
  codEnabled: true,
  guestCheckoutEnabled: true,
  maintenanceMode: false,
  maintenanceMessage: '',
  businessAddress: 'Lekki Phase 1, Lagos, Nigeria',
  supportHours: 'Mon – Sat · 9:00 AM – 6:00 PM WAT',
  bankName: 'ZEECROWN STORE Boutique Account',
  accountName: 'ZEECROWN STORE',
  accountNumber: 'Confirm via Checkout / WhatsApp',
  instagramHandle: '@zeecrownstore',
  instagramUrl: '',
  tiktokHandle: '@zeecrownstore',
  tiktokUrl: '',
  freeDeliveryThresholdNgn: 150000,
};

const DEFAULT_AI_CONFIG: AiAssistantConfig = {
  enabled: true,
  assistantName: 'ZEECROWN CUSTOMER CARE',
  toneStyle: 'LUXURY_WARM',
  humanSupportAvailable: true,
  supportWorkingDays: 'Mon – Sat',
  supportWorkingHours: '9:00 AM – 6:00 PM WAT',
  expectedResponseTimeMessage: 'Our specialists respond promptly during studio hours.',
};

const GUEST_CART_KEY = 'zc_guest_cart_v1';
const GUEST_WISH_KEY = 'zc_guest_wishlist_v1';
const CURRENCY_KEY = 'zc_currency_v1';

const StoreContext = createContext<StoreContextValue | null>(null);

export function StoreProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [adminRole, setAdminRole] = useState<AdminRole | 'none'>('none');

  const [currency, setCurrencyState] = useState<CurrencyCode>(() => {
    try {
      return localStorage.getItem(CURRENCY_KEY) === 'USD' ? 'USD' : 'NGN';
    } catch {
      return 'NGN';
    }
  });

  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [collections, setCollections] = useState<StoreCollection[]>([]);
  const [inventoryByProduct, setInventoryByProduct] = useState<Record<string, InventoryItem>>({});
  const [deliveryZones, setDeliveryZones] = useState<DeliveryZone[]>([]);
  const [deliverySettings, setDeliverySettings] = useState<DeliverySettings>({
    freeDeliveryThresholdNgn: 150000,
    pickupEnabled: false,
    pickupAddress: '',
    pickupHours: '',
    specialPromoNote: '',
    visibility: 'public',
  });
  const [sizeGuides, setSizeGuides] = useState<SizeGuide[]>([]);
  const [storeSettings, setStoreSettings] = useState<StoreSettings>(DEFAULT_SETTINGS);
  const [aiConfig, setAiConfig] = useState<AiAssistantConfig>(DEFAULT_AI_CONFIG);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState<string | null>(null);

  const [cart, setCart] = useState<CartItem[]>(() => {
    try {
      return JSON.parse(localStorage.getItem(GUEST_CART_KEY) || '[]');
    } catch {
      return [];
    }
  });
  const [wishlist, setWishlist] = useState<WishlistItem[]>(() => {
    try {
      return JSON.parse(localStorage.getItem(GUEST_WISH_KEY) || '[]');
    } catch {
      return [];
    }
  });
  const [orders, setOrders] = useState<StoreOrder[]>([]);
  const [addresses, setAddresses] = useState<DeliveryAddress[]>([]);

  const setCurrency = (c: CurrencyCode) => {
    setCurrencyState(c);
    try {
      localStorage.setItem(CURRENCY_KEY, c);
    } catch {
      // ignore storage quota errors
    }
  };

  const loadCatalogSnapshot = async (includeInactiveForAdmin = adminRole !== 'none') => {
    setCatalogLoading(true);
    setCatalogError(null);
    try {
      const productsQuery = includeInactiveForAdmin
        ? collection(db, 'products')
        : query(collection(db, 'products'), where('active', '==', true));
      const categoriesQuery = includeInactiveForAdmin
        ? collection(db, 'categories')
        : query(collection(db, 'categories'), where('active', '==', true));
      const inventoryQuery = includeInactiveForAdmin
        ? collection(db, 'inventory')
        : query(collection(db, 'inventory'), where('inStock', '==', true));
      const zonesQuery = includeInactiveForAdmin
        ? collection(db, 'deliveryZones')
        : query(collection(db, 'deliveryZones'), where('active', '==', true));
      const [
        prodSnap,
        catSnap,
        colSnap,
        invSnap,
        zoneSnap,
        sgSnap,
        settingsSnap,
        aiSnap,
        delivSetSnap,
      ] = await Promise.all([
        getDocs(productsQuery),
        getDocs(categoriesQuery).catch(() => null),
        getDocs(query(collection(db, 'collections'), where('active', '==', true))).catch(() => null),
        getDocs(inventoryQuery).catch(() => null),
        getDocs(zonesQuery).catch(() => null),
        getDocs(query(collection(db, 'sizeGuides'), where('active', '==', true))).catch(() => null),
        getDoc(doc(db, 'storeSettings', 'public')).catch(() => null),
        getDoc(doc(db, 'aiConfig', 'public')).catch(() => null),
        getDoc(doc(db, 'deliverySettings', 'public')).catch(() => null),
      ]);

      const prods: Product[] = prodSnap.docs.map((d) => {
        const data = d.data();
        const rawAttrs =
          data.attributes && typeof data.attributes === 'object'
            ? Object.fromEntries(
                Object.entries(data.attributes as Record<string, unknown>).map(([k, v]) => [k, String(v ?? '')])
              )
            : undefined;
        return {
          id: d.id,
          title: String(data.title || ''),
          slug: String(data.slug || d.id),
          description: String(data.description || ''),
          summary: typeof data.summary === 'string' ? data.summary : undefined,
          categoryId: String(data.categoryId || ''),
          collectionId: typeof data.collectionId === 'string' ? data.collectionId : undefined,
          sizeGuideId: typeof data.sizeGuideId === 'string' ? data.sizeGuideId : undefined,
          priceNgn: Number(data.priceNgn || 0),
          compareAtPriceNgn: typeof data.compareAtPriceNgn === 'number' ? data.compareAtPriceNgn : undefined,
          priceUsd: Number(data.priceUsd || 0),
          mainImageUrl:
            typeof data.mainImageUrl === 'string'
              ? resolveProductImageUrl(data.mainImageUrl)
              : undefined,
          imageUrls: Array.isArray(data.imageUrls)
            ? data.imageUrls.map((u) => resolveProductImageUrl(String(u)))
            : [],
          sizes: Array.isArray(data.sizes) ? data.sizes.map(String) : [],
          colors: Array.isArray(data.colors) ? data.colors.map(String) : [],
          unavailableVariants: Array.isArray(data.unavailableVariants) ? data.unavailableVariants.map(String) : [],
          attributes: rawAttrs,
          lowStockThreshold: typeof data.lowStockThreshold === 'number' ? data.lowStockThreshold : 3,
          features: Array.isArray(data.features) ? data.features.map(String) : [],
          fashionStyle: typeof data.fashionStyle === 'string' ? data.fashionStyle : undefined,
          suitableOccasions: Array.isArray(data.suitableOccasions) ? data.suitableOccasions.map(String) : [],
          tags: Array.isArray(data.tags) ? data.tags.map(String) : [],
          brand: typeof data.brand === 'string' ? data.brand : undefined,
          sku: typeof data.sku === 'string' ? data.sku : undefined,
          featured: data.featured === true,
          newArrival: data.newArrival === true,
          soldCount: Number(data.soldCount || 0),
          active: data.active !== false,
          seoTitle: typeof data.seoTitle === 'string' ? data.seoTitle : undefined,
          seoDescription: typeof data.seoDescription === 'string' ? data.seoDescription : undefined,
        };
      });
      setAllProducts(prods);

      const cats: Category[] = (catSnap?.docs || [])
        .map((d) => {
          const data = d.data();
          return {
            id: d.id,
            name: String(data.name || ''),
            slug: String(data.slug || d.id),
            description: String(data.description || ''),
            imageKey: typeof data.imageKey === 'string' ? data.imageKey : undefined,
            parentCategoryId: typeof data.parentCategoryId === 'string' ? data.parentCategoryId : undefined,
            sizeGuideId: typeof data.sizeGuideId === 'string' ? data.sizeGuideId : undefined,
            sortOrder: Number(data.sortOrder || 0),
            active: data.active !== false,
          };
        })
        .sort((a, b) => a.sortOrder - b.sortOrder);
      setCategories(cats);

      if (colSnap) {
        setCollections(
          colSnap.docs
            .map((d) => {
              const data = d.data();
              return {
                id: d.id,
                name: String(data.name || ''),
                slug: String(data.slug || d.id),
                tagline: String(data.tagline || ''),
                bannerKey: typeof data.bannerKey === 'string' ? data.bannerKey : undefined,
                sortOrder: Number(data.sortOrder || 0),
                active: data.active !== false,
              };
            })
            .sort((a, b) => a.sortOrder - b.sortOrder)
        );
      }

      if (invSnap) {
        const map: Record<string, InventoryItem> = {};
        invSnap.docs.forEach((d) => {
          const data = d.data();
          const pid = String(data.productId || d.id);
          const rawVariantStock =
            data.variantStock && typeof data.variantStock === 'object'
              ? Object.fromEntries(
                  Object.entries(data.variantStock as Record<string, unknown>)
                    .filter(([, v]) => typeof v === 'number')
                    .map(([k, v]) => [k, Number(v)])
                )
              : undefined;
          map[pid] = {
            id: d.id,
            productId: pid,
            sku: String(data.sku || ''),
            stockQuantity: Number(data.stockQuantity || 0),
            lowStockThreshold: Number(data.lowStockThreshold || 3),
            reservedQuantity: Number(data.reservedQuantity || 0),
            variantStock: rawVariantStock,
            inStock: data.inStock === true,
          };
        });
        setInventoryByProduct(map);
      }

      if (zoneSnap) {
        setDeliveryZones(
          zoneSnap.docs
            .map((d) => {
              const data = d.data();
              const statesArr = Array.isArray(data.states) ? data.states.map(String).filter(Boolean) : [];
              const statesText = statesArr.join(', ');
              return {
                id: d.id,
                name: String(data.name || ''),
                region: String(data.region || statesText || data.country || 'Nigeria'),
                country: typeof data.country === 'string' ? data.country : 'Nigeria',
                states: statesArr,
                feeNgn: Number(data.feeNgn || 0),
                estimatedDays: String(data.estimatedDays || '2–5 business days'),
                promotionNote: typeof data.promotionNote === 'string' ? data.promotionNote : undefined,
                active: data.active !== false,
                sortOrder: Number(data.sortOrder || 0),
              };
            })
            .sort((a, b) => a.sortOrder - b.sortOrder || a.feeNgn - b.feeNgn)
        );
      }

      if (delivSetSnap && delivSetSnap.exists()) {
        const ds = delivSetSnap.data();
        setDeliverySettings({
          freeDeliveryThresholdNgn: Number(ds.freeDeliveryThresholdNgn || 0),
          nationwideFlatFeeNgn:
            typeof ds.nationwideFlatFeeNgn === 'number' ? ds.nationwideFlatFeeNgn : undefined,
          pickupEnabled: ds.pickupEnabled === true,
          pickupAddress: typeof ds.pickupAddress === 'string' ? ds.pickupAddress : '',
          pickupHours: typeof ds.pickupHours === 'string' ? ds.pickupHours : '',
          specialPromoNote: typeof ds.specialPromoNote === 'string' ? ds.specialPromoNote : '',
          visibility: typeof ds.visibility === 'string' ? ds.visibility : 'public',
        });
      }

      if (sgSnap) {
        setSizeGuides(
          sgSnap.docs
            .map((d) => {
              const data = d.data();
              return {
                id: d.id,
                name: String(data.name || ''),
                categoryType: (data.categoryType || 'CUSTOM') as SizeGuide['categoryType'],
                description: typeof data.description === 'string' ? data.description : undefined,
                measurementUnitDefault: (data.measurementUnitDefault === 'IN' ? 'IN' : 'CM') as 'CM' | 'IN',
                columns: Array.isArray(data.columns) ? data.columns.map(String) : [],
                rows: Array.isArray(data.rows) ? data.rows : [],
                howToMeasureSteps: Array.isArray(data.howToMeasureSteps)
                  ? data.howToMeasureSteps.map(String)
                  : [],
                fitNotes: typeof data.fitNotes === 'string' ? data.fitNotes : undefined,
                assignedCategoryIds: Array.isArray(data.assignedCategoryIds)
                  ? data.assignedCategoryIds.map(String)
                  : [],
                assignedProductIds: Array.isArray(data.assignedProductIds)
                  ? data.assignedProductIds.map(String)
                  : [],
                sortOrder: Number(data.sortOrder || 0),
                active: data.active !== false,
              };
            })
            .sort((a, b) => a.sortOrder - b.sortOrder)
        );
      }

      if (settingsSnap && settingsSnap.exists()) {
        const s = settingsSnap.data();
        setStoreSettings({
          ...DEFAULT_SETTINGS,
          storeName: String(s.storeName || DEFAULT_SETTINGS.storeName),
          tagline: String(s.tagline || DEFAULT_SETTINGS.tagline),
          supportEmail: resolveEffectiveSupportEmail(
            typeof s.supportEmail === 'string' ? s.supportEmail : ''
          ),
          supportPhone: String(s.supportPhone || OFFICIAL_WHATSAPP_DISPLAY),
          whatsappNumber: String(s.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY),
          businessAddress: String(s.businessAddress || DEFAULT_SETTINGS.businessAddress || ''),
          supportHours: String(s.supportHours || DEFAULT_SETTINGS.supportHours || ''),
          aboutText: String(s.aboutText || DEFAULT_SETTINGS.aboutText),
          shippingPolicy: String(s.shippingPolicy || DEFAULT_SETTINGS.shippingPolicy),
          returnPolicy: resolveEffectiveReturnPolicy(typeof s.returnPolicy === 'string' ? s.returnPolicy : ''),
          privacyPolicy: String(s.privacyPolicy || DEFAULT_SETTINGS.privacyPolicy),
          termsPolicy: String(s.termsPolicy || DEFAULT_SETTINGS.termsPolicy),
          defaultCurrency: s.defaultCurrency === 'USD' ? 'USD' : 'NGN',
          usdRateNgn: Number(s.usdRateNgn || 1600),
          paystackEnabled: s.paystackEnabled !== false,
          paystackPublicKey: typeof s.paystackPublicKey === 'string' ? s.paystackPublicKey : '',
          flutterwaveEnabled: s.flutterwaveEnabled === true,
          flutterwavePublicKey: typeof s.flutterwavePublicKey === 'string' ? s.flutterwavePublicKey : '',
          bankTransferEnabled: s.bankTransferEnabled !== false,
          codEnabled: s.codEnabled !== false,
          guestCheckoutEnabled: s.guestCheckoutEnabled !== false,
          maintenanceMode: s.maintenanceMode === true,
          maintenanceMessage: typeof s.maintenanceMessage === 'string' ? s.maintenanceMessage : '',
          bankName: String(s.bankName || DEFAULT_SETTINGS.bankName),
          accountName: String(s.accountName || DEFAULT_SETTINGS.accountName),
          accountNumber: String(s.accountNumber || DEFAULT_SETTINGS.accountNumber),
          instagramHandle: typeof s.instagramHandle === 'string' ? s.instagramHandle : DEFAULT_SETTINGS.instagramHandle,
          instagramUrl: typeof s.instagramUrl === 'string' ? s.instagramUrl : '',
          tiktokHandle: typeof s.tiktokHandle === 'string' ? s.tiktokHandle : DEFAULT_SETTINGS.tiktokHandle,
          tiktokUrl: typeof s.tiktokUrl === 'string' ? s.tiktokUrl : '',
          freeDeliveryThresholdNgn:
            typeof s.freeDeliveryThresholdNgn === 'number'
              ? s.freeDeliveryThresholdNgn
              : DEFAULT_SETTINGS.freeDeliveryThresholdNgn,
        });
      }

      if (aiSnap && aiSnap.exists()) {
        const a = aiSnap.data();
        setAiConfig({
          enabled: a.enabled !== false,
          assistantName: String(a.assistantName || 'ZEECROWN CUSTOMER CARE'),
          toneStyle: String(a.toneStyle || 'LUXURY_WARM'),
          humanSupportAvailable: a.humanSupportAvailable !== false,
          supportWorkingDays: String(a.supportWorkingDays || 'Mon – Sat'),
          supportWorkingHours: String(a.supportWorkingHours || '9:00 AM – 6:00 PM WAT'),
          expectedResponseTimeMessage: String(
            a.expectedResponseTimeMessage || DEFAULT_AI_CONFIG.expectedResponseTimeMessage
          ),
        });
      }
    } catch (err) {
      setCatalogError(
        err instanceof Error
          ? err.message
          : 'Unable to load store catalogue from Firebase right now.'
      );
    } finally {
      setCatalogLoading(false);
    }
  };

  useEffect(() => {
    void loadCatalogSnapshot();
  }, []);

  // Auth listener + User profile & Admin RBAC sync
  useEffect(() => {
    let unsubCart: (() => void) | null = null;
    let unsubWish: (() => void) | null = null;
    let unsubOrders: (() => void) | null = null;
    let unsubAddrs: (() => void) | null = null;

    const unsubAuth = onAuthStateChanged(auth, async (nextUser) => {
      setUser(nextUser);
      unsubCart?.();
      unsubWish?.();
      unsubOrders?.();
      unsubAddrs?.();

      if (!nextUser) {
        setAdminRole('none');
        setOrders([]);
        setAddresses([]);
        try {
          setCart(JSON.parse(localStorage.getItem(GUEST_CART_KEY) || '[]'));
          setWishlist(JSON.parse(localStorage.getItem(GUEST_WISH_KEY) || '[]'));
        } catch {
          setCart([]);
          setWishlist([]);
        }
        setAuthLoading(false);
        return;
      }

      try {
        // Resolve admin role matching firestore.rules
        let resolvedRole: AdminRole | 'none' = 'none';
        if (
          nextUser.emailVerified &&
          nextUser.email?.toLowerCase() === BOOTSTRAPPED_ADMIN_EMAIL.toLowerCase()
        ) {
          resolvedRole = 'super_admin';
        } else {
          const adminSnap = await getDoc(doc(db, 'adminUsers', nextUser.uid)).catch(() => null);
          if (adminSnap?.exists() && adminSnap.data()?.active === true) {
            resolvedRole = (adminSnap.data()?.role || 'none') as AdminRole;
          }
        }
        setAdminRole(resolvedRole);
        if (resolvedRole !== 'none') {
          void loadCatalogSnapshot(true);
        }

        // Ensure users/{uid} profile + private info exist cleanly
        const profileRef = doc(db, 'users', nextUser.uid);
        const profileSnap = await getDoc(profileRef).catch(() => null);
        if (!profileSnap?.exists()) {
          await setDoc(profileRef, {
            userId: nextUser.uid,
            displayName: (nextUser.displayName || nextUser.email?.split('@')[0] || 'Client').slice(0, 80),
            photoUrl: (nextUser.photoURL || '').slice(0, 600),
            preferredCurrency: currency,
            role: 'customer',
            emailVerified: nextUser.emailVerified,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          }).catch(() => undefined);
        }
        if (nextUser.email) {
          const privRef = doc(db, 'users', nextUser.uid, 'private', 'info');
          const privSnap = await getDoc(privRef).catch(() => null);
          if (!privSnap?.exists()) {
            await setDoc(privRef, {
              userId: nextUser.uid,
              email: nextUser.email.slice(0, 200),
              updatedAt: serverTimestamp(),
            }).catch(() => undefined);
          }
        }

        // Merge any guest cart items into Firestore cart cleanly
        try {
          const guestCart: CartItem[] = JSON.parse(localStorage.getItem(GUEST_CART_KEY) || '[]');
          if (guestCart.length > 0) {
            for (const g of guestCart) {
              const safeDocId = `${g.productId}_${g.variantLabel.replace(/[^a-zA-Z0-9_-]/g, '_')}`.slice(0, 120);
              await setDoc(doc(db, 'carts', nextUser.uid, 'items', safeDocId), {
                userId: nextUser.uid,
                productId: g.productId,
                productTitle: g.productTitle.slice(0, 120),
                productImageUrl: (g.productImageUrl || '').slice(0, 600),
                variantLabel: g.variantLabel.slice(0, 80),
                unitPriceNgn: Number(g.unitPriceNgn || 0),
                quantity: Math.max(1, Math.min(50, Math.round(g.quantity || 1))),
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              }).catch(() => undefined);
            }
            localStorage.removeItem(GUEST_CART_KEY);
          }
        } catch {
          // ignore
        }

        unsubCart = onSnapshot(collection(db, 'carts', nextUser.uid, 'items'), (snap) => {
          setCart(
            snap.docs.map((d) => ({
              id: d.id,
              ...(d.data() as Omit<CartItem, 'id'>),
            }))
          );
        });

        unsubWish = onSnapshot(collection(db, 'wishlists', nextUser.uid, 'items'), (snap) => {
          setWishlist(
            snap.docs.map((d) => ({
              id: d.id,
              ...(d.data() as Omit<WishlistItem, 'id'>),
            }))
          );
        });

        unsubOrders = onSnapshot(
          query(collection(db, 'orders'), where('userId', '==', nextUser.uid)),
          (snap) => {
            const list: StoreOrder[] = snap.docs.map((d) => ({
              id: d.id,
              ...(d.data() as Omit<StoreOrder, 'id'>),
            }));
            setOrders(list.reverse());
          }
        );

        unsubAddrs = onSnapshot(
          collection(db, 'users', nextUser.uid, 'addresses'),
          (snap) => {
            setAddresses(
              snap.docs.map((d) => ({
                id: d.id,
                ...(d.data() as Omit<DeliveryAddress, 'id'>),
              }))
            );
          }
        );
      } finally {
        setAuthLoading(false);
      }
    });

    return () => {
      unsubAuth();
      unsubCart?.();
      unsubWish?.();
      unsubOrders?.();
      unsubAddrs?.();
    };
  }, []);

  const persistGuestCart = (next: CartItem[]) => {
    setCart(next);
    try {
      localStorage.setItem(GUEST_CART_KEY, JSON.stringify(next));
    } catch {
      // ignore
    }
  };

  const persistGuestWishlist = (next: WishlistItem[]) => {
    setWishlist(next);
    try {
      localStorage.setItem(GUEST_WISH_KEY, JSON.stringify(next));
    } catch {
      // ignore
    }
  };

  const addToCart = async (
    product: Product,
    variantLabel: string,
    qty = 1,
    unitPriceOverrideNgn?: number
  ) => {
    const cleanVariant = (variantLabel || 'Standard').slice(0, 80);
    const safeDocId = `${product.id}_${cleanVariant.replace(/[^a-zA-Z0-9_-]/g, '_')}`.slice(0, 120);
    const imageUrl = (product.mainImageUrl || product.imageUrls[0] || '').slice(0, 600);
    const effectiveUnitPrice =
      typeof unitPriceOverrideNgn === 'number' && unitPriceOverrideNgn >= 0
        ? unitPriceOverrideNgn
        : product.priceNgn;

    if (!user) {
      const existing = cart.find((c) => c.id === safeDocId);
      const next = existing
        ? cart.map((c) =>
            c.id === safeDocId
              ? { ...c, unitPriceNgn: effectiveUnitPrice, quantity: Math.min(50, c.quantity + qty) }
              : c
          )
        : [
            ...cart,
            {
              id: safeDocId,
              userId: 'guest',
              productId: product.id,
              productTitle: product.title.slice(0, 120),
              productImageUrl: imageUrl,
              variantLabel: cleanVariant,
              unitPriceNgn: effectiveUnitPrice,
              quantity: Math.max(1, Math.min(50, qty)),
            },
          ];
      persistGuestCart(next);
      return;
    }

    const itemRef = doc(db, 'carts', user.uid, 'items', safeDocId);
    const existingSnap = await getDoc(itemRef);
    if (existingSnap.exists()) {
      const prevQty = Number(existingSnap.data().quantity || 1);
      await updateDoc(itemRef, {
        quantity: Math.min(50, prevQty + qty),
        updatedAt: serverTimestamp(),
      });
    } else {
      await setDoc(itemRef, {
        userId: user.uid,
        productId: product.id,
        productTitle: product.title.slice(0, 120),
        productImageUrl: imageUrl,
        variantLabel: cleanVariant,
        unitPriceNgn: effectiveUnitPrice,
        quantity: Math.max(1, Math.min(50, qty)),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  };

  const updateCartQty = async (itemId: string, qty: number) => {
    if (qty <= 0) {
      await removeFromCart(itemId);
      return;
    }
    const clamped = Math.max(1, Math.min(50, Math.round(qty)));
    if (!user) {
      persistGuestCart(cart.map((c) => (c.id === itemId ? { ...c, quantity: clamped } : c)));
      return;
    }
    await updateDoc(doc(db, 'carts', user.uid, 'items', itemId), {
      quantity: clamped,
      updatedAt: serverTimestamp(),
    });
  };

  const removeFromCart = async (itemId: string) => {
    if (!user) {
      persistGuestCart(cart.filter((c) => c.id !== itemId));
      return;
    }
    await deleteDoc(doc(db, 'carts', user.uid, 'items', itemId));
  };

  const clearCart = async () => {
    if (!user) {
      persistGuestCart([]);
      return;
    }
    const batch = writeBatch(db);
    cart.forEach((item) => {
      batch.delete(doc(db, 'carts', user.uid, 'items', item.id));
    });
    await batch.commit();
  };

  const isWishlisted = (productId: string) => wishlist.some((w) => w.productId === productId);

  const toggleWishlist = async (product: Product) => {
    const exists = isWishlisted(product.id);
    if (!user) {
      if (exists) {
        persistGuestWishlist(wishlist.filter((w) => w.productId !== product.id));
      } else {
        persistGuestWishlist([
          ...wishlist,
          {
            id: product.id,
            userId: 'guest',
            productId: product.id,
            productTitle: product.title.slice(0, 120),
            productImageUrl: (product.mainImageUrl || product.imageUrls[0] || '').slice(0, 600),
            priceNgn: product.priceNgn,
          },
        ]);
      }
      return;
    }

    const ref = doc(db, 'wishlists', user.uid, 'items', product.id);
    if (exists) {
      await deleteDoc(ref);
    } else {
      await setDoc(ref, {
        userId: user.uid,
        productId: product.id,
        productTitle: product.title.slice(0, 120),
        productImageUrl: (product.mainImageUrl || product.imageUrls[0] || '').slice(0, 600),
        priceNgn: product.priceNgn,
        createdAt: serverTimestamp(),
      });
    }
  };

  const saveAddress = async (address: Omit<DeliveryAddress, 'id' | 'userId'>) => {
    if (!user) throw new Error('Please sign in to save delivery addresses.');
    const ref = doc(collection(db, 'users', user.uid, 'addresses'));
    await setDoc(ref, {
      ...address,
      userId: user.uid,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  };

  const deleteAddress = async (addressId: string) => {
    if (!user) return;
    await deleteDoc(doc(db, 'users', user.uid, 'addresses', addressId));
  };

  const placeOrder = async (input: PlaceOrderInput) => {
    if (!user) throw new Error('Please sign in to place an order.');
    if (cart.length === 0) throw new Error('Your shopping bag is empty.');

    const orderRef = doc(collection(db, 'orders'));
    const paymentRef = doc(collection(db, 'payments'));

    const subtotalNgn = cart.reduce((sum, item) => sum + item.unitPriceNgn * item.quantity, 0);
    const itemCount = Math.min(
      200,
      Math.max(1, cart.reduce((sum, item) => sum + item.quantity, 0))
    );
    const totalNgn = subtotalNgn + input.deliveryFeeNgn;
    const shortCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    const orderNumber = `ZC-${shortCode}`;
    const itemsSummary = cart
      .map((i) => `${i.quantity}x ${i.productTitle} (${i.variantLabel})`)
      .join(', ')
      .slice(0, 1000);
    const trackingToken = generateTrackingToken();
    const customerEmail = (user.email || '').trim().toLowerCase().slice(0, 200);
    const lookupEmailHash = customerEmail
      ? await buildTrackingLookupId(orderNumber, customerEmail)
      : '';
    const lookupPhoneHash = input.recipientPhone
      ? await buildTrackingLookupId(orderNumber, input.recipientPhone)
      : '';

    const initialHistory = [
      {
        stage: 'PENDING_PAYMENT',
        label: 'Order Placed',
        note: `Order ${orderNumber} placed via website (${input.paymentMethod}).`,
        actorRole: 'CUSTOMER',
        timestampMillis: Date.now(),
      },
    ];

    await setDoc(orderRef, {
      userId: user.uid,
      orderNumber,
      itemsSummary,
      itemCount,
      subtotalNgn,
      deliveryFeeNgn: input.deliveryFeeNgn,
      discountNgn: 0,
      totalNgn,
      currency,
      deliveryZoneId: input.deliveryZoneId,
      recipientName: input.recipientName.trim().slice(0, 80),
      recipientPhone: input.recipientPhone.trim().slice(0, 30),
      shippingAddress: input.shippingAddress.trim().slice(0, 250),
      shippingCity: input.shippingCity.trim().slice(0, 80),
      shippingState: input.shippingState.trim().slice(0, 80),
      shippingCountry: input.shippingCountry.trim().slice(0, 80),
      lga: (input.lga || '').trim().slice(0, 80),
      landmark: (input.landmark || '').trim().slice(0, 150),
      deliveryInstructions: (input.deliveryInstructions || '').trim().slice(0, 250),
      status: 'PENDING_PAYMENT',
      paymentMethod: input.paymentMethod,
      paymentStatus: 'UNPAID',
      customerEmail,
      trackingToken,
      guestLookupHashEmail: lookupEmailHash,
      guestLookupHashPhone: lookupPhoneHash,
      deliveryStatus: 'Order Placed',
      statusHistory: initialHistory,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });

    const isBankSubmitted =
      input.paymentMethod === 'BANK_TRANSFER' && Boolean(input.transferNote?.trim());

    await setDoc(paymentRef, {
      userId: user.uid,
      orderId: orderRef.id,
      orderNumber,
      customerName: input.recipientName.trim().slice(0, 80),
      provider: input.paymentMethod,
      reference: `PAY-${shortCode}-${Date.now() % 10000}`,
      amountNgn: totalNgn,
      currency,
      status: isBankSubmitted ? 'SUBMITTED' : 'INITIATED',
      transferNote: (input.transferNote || '').trim().slice(0, 300),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });

    let activeStatus = 'PENDING_PAYMENT';
    let activePayStatus = 'UNPAID';
    let activeHistory = initialHistory;

    if (isBankSubmitted) {
      activeStatus = 'PAYMENT_SUBMITTED';
      activePayStatus = 'PENDING_VERIFICATION';
      activeHistory = [
        ...initialHistory,
        {
          stage: 'PAYMENT_SUBMITTED',
          label: 'Payment Reference Submitted',
          note: 'Bank transfer notice submitted for verification.',
          actorRole: 'CUSTOMER',
          timestampMillis: Date.now(),
        },
      ];
      await updateDoc(orderRef, {
        status: activeStatus,
        paymentStatus: activePayStatus,
        statusHistory: activeHistory,
        updatedAt: serverTimestamp(),
      });
    }

    // Create PII-redacted tracking projections for guest lookup
    const maskedDestination = [input.shippingCity, input.shippingState, input.shippingCountry]
      .filter(Boolean)
      .join(', ')
      .slice(0, 160) || 'Nigeria';

    const projectionPayload = {
      orderId: orderRef.id,
      userId: user.uid,
      orderNumber,
      status: activeStatus,
      paymentStatus: activePayStatus,
      currency,
      totalNgn,
      itemCount,
      itemsSummary,
      maskedDestination,
      deliveryStatus: 'Order Placed',
      statusHistory: activeHistory,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };

    const projectionWrites: Array<Promise<unknown>> = [
      setDoc(doc(db, 'orderTrackingTokens', trackingToken), projectionPayload).catch(() => undefined),
    ];
    if (lookupEmailHash) {
      projectionWrites.push(
        setDoc(doc(db, 'orderTrackingLookups', lookupEmailHash), projectionPayload).catch(() => undefined)
      );
    }
    if (lookupPhoneHash) {
      projectionWrites.push(
        setDoc(doc(db, 'orderTrackingLookups', lookupPhoneHash), projectionPayload).catch(() => undefined)
      );
    }
    await Promise.all([
      ...projectionWrites,
      setDoc(doc(db, 'notifications', `notif_order_placed_${orderRef.id}`), {
        userId: user.uid,
        title: 'Order Placed',
        body: `Your order ${orderNumber} (₦${Math.round(totalNgn).toLocaleString('en-NG')}) has been placed.`,
        category: 'ORDER',
        readFlag: false,
        targetRoute: `track_order/${orderRef.id}`,
        orderId: orderRef.id,
        orderNumber,
        dedupeKey: `order_placed_${orderRef.id}`,
        createdAt: serverTimestamp(),
      }).catch(() => undefined),
    ]);

    await clearCart();
    return { orderNumber, orderId: orderRef.id, paymentId: paymentRef.id, trackingToken };
  };

  const submitGatewayPaymentReference = async ({
    orderId,
    paymentId,
    gatewayRef,
    provider,
    trackingToken,
  }: {
    orderId: string;
    paymentId: string;
    gatewayRef: string;
    provider: 'PAYSTACK' | 'FLUTTERWAVE';
    trackingToken?: string;
  }) => {
    if (!user) return;
    const cleanRef = gatewayRef.trim().slice(0, 260);
    await updateDoc(doc(db, 'payments', paymentId), {
      status: 'SUBMITTED',
      transferNote: `${provider} Checkout Ref: ${cleanRef}`.slice(0, 300),
      updatedAt: serverTimestamp(),
    });
    const orderRef = doc(db, 'orders', orderId);
    const orderSnap = await getDoc(orderRef);
    const existingHistory = Array.isArray(orderSnap.data()?.statusHistory)
      ? (orderSnap.data()?.statusHistory as Array<Record<string, unknown>>)
      : [];
    const nextHistory = [
      ...existingHistory.slice(-28),
      {
        stage: 'PAYMENT_SUBMITTED',
        label: `${provider === 'PAYSTACK' ? 'Paystack' : 'Flutterwave'} Payment Completed`,
        note: `Gateway reference ${cleanRef} submitted for admin confirmation.`,
        actorRole: 'CUSTOMER',
        timestampMillis: Date.now(),
      },
    ];
    await updateDoc(orderRef, {
      status: 'PAYMENT_SUBMITTED',
      paymentStatus: 'PENDING_VERIFICATION',
      statusHistory: nextHistory,
      updatedAt: serverTimestamp(),
    });
    if (trackingToken) {
      await updateDoc(doc(db, 'orderTrackingTokens', trackingToken), {
        status: 'PAYMENT_SUBMITTED',
        paymentStatus: 'PENDING_VERIFICATION',
        statusHistory: nextHistory,
        updatedAt: serverTimestamp(),
      }).catch(() => undefined);
    }
  };

  const signOut = async () => {
    await firebaseSignOut(auth);
  };

  const activeProducts = useMemo(() => allProducts.filter((p) => p.active), [allProducts]);
  const activeCategories = useMemo(() => categories.filter((c) => c.active), [categories]);
  const activeCollections = useMemo(() => collections.filter((c) => c.active), [collections]);
  const activeZones = useMemo(() => deliveryZones.filter((z) => z.active), [deliveryZones]);
  const activeSizeGuides = useMemo(() => sizeGuides.filter((s) => s.active), [sizeGuides]);

  const value: StoreContextValue = {
    user,
    authLoading,
    isAdmin: adminRole !== 'none',
    adminRole,
    currency,
    setCurrency,
    products: activeProducts,
    allProducts,
    categories: activeCategories,
    allCategories: categories,
    collections: activeCollections,
    inventoryByProduct,
    deliveryZones: activeZones,
    allDeliveryZones: deliveryZones,
    deliverySettings,
    sizeGuides: activeSizeGuides,
    storeSettings,
    aiConfig,
    catalogLoading,
    catalogError,
    cart,
    wishlist,
    orders,
    addresses,
    addToCart,
    updateCartQty,
    removeFromCart,
    clearCart,
    toggleWishlist,
    isWishlisted,
    saveAddress,
    deleteAddress,
    placeOrder,
    submitGatewayPaymentReference,
    signOut,
    refreshCatalog: loadCatalogSnapshot,
  };

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useStore(): StoreContextValue {
  const ctx = useContext(StoreContext);
  if (!ctx) throw new Error('useStore must be used inside StoreProvider');
  return ctx;
}
