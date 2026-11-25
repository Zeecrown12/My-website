export type CurrencyCode = 'NGN' | 'USD';

export type AdminRole =
  | 'super_admin'
  | 'manager'
  | 'store_manager'
  | 'order_manager'
  | 'content_manager'
  | 'support_staff';

export interface VariantConfig {
  key: string;
  size: string;
  color: string;
  sku?: string;
  priceNgn?: number;
  priceUsd?: number;
}

export interface Product {
  id: string;
  title: string;
  slug: string;
  description: string;
  summary?: string;
  categoryId: string;
  collectionId?: string;
  sizeGuideId?: string;
  priceNgn: number;
  compareAtPriceNgn?: number;
  priceUsd: number;
  mainImageUrl?: string;
  imageUrls: string[];
  sizes: string[];
  colors: string[];
  unavailableVariants?: string[];
  attributes?: Record<string, string>;
  lowStockThreshold?: number;
  features?: string[];
  fashionStyle?: string;
  suitableOccasions?: string[];
  tags?: string[];
  brand?: string;
  sku?: string;
  featured: boolean;
  newArrival?: boolean;
  soldCount?: number;
  active: boolean;
  seoTitle?: string;
  seoDescription?: string;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  description: string;
  imageKey?: string;
  parentCategoryId?: string;
  sizeGuideId?: string;
  sortOrder: number;
  active: boolean;
}

export interface StoreCollection {
  id: string;
  name: string;
  slug: string;
  tagline: string;
  bannerKey?: string;
  sortOrder: number;
  active: boolean;
}

export interface InventoryItem {
  id: string;
  productId: string;
  sku: string;
  stockQuantity: number;
  lowStockThreshold: number;
  reservedQuantity: number;
  variantStock?: Record<string, number>;
  inStock: boolean;
}

export interface InventoryTransaction {
  id: string;
  productId: string;
  productTitle: string;
  sku: string;
  variation?: string;
  previousQty: number;
  deltaQty: number;
  newQty: number;
  reason: string;
  adminUid: string;
  adminEmail: string;
  createdAt?: unknown;
}

export interface DeliveryZone {
  id: string;
  name: string;
  region: string;
  country?: string;
  states?: string[];
  feeNgn: number;
  estimatedDays: string;
  promotionNote?: string;
  active: boolean;
  sortOrder: number;
}

export interface DeliverySettings {
  freeDeliveryThresholdNgn: number;
  nationwideFlatFeeNgn?: number;
  pickupEnabled: boolean;
  pickupAddress?: string;
  pickupHours?: string;
  specialPromoNote?: string;
  visibility?: string;
}

export interface SizeGuide {
  id: string;
  name: string;
  categoryType: 'ABAYAS_BUBU' | 'CLOTHING_KAFTAN' | 'SLIPPERS_SANDALS' | 'SHOES_HEELS' | 'CUSTOM';
  description?: string;
  measurementUnitDefault?: 'CM' | 'IN';
  columns: string[];
  rows: Array<Record<string, string>>;
  howToMeasureSteps?: string[];
  fitNotes?: string;
  assignedCategoryIds?: string[];
  assignedProductIds?: string[];
  sortOrder: number;
  active: boolean;
}

export interface StoreSettings {
  storeName: string;
  tagline: string;
  supportEmail: string;
  supportPhone: string;
  whatsappNumber: string;
  businessAddress?: string;
  supportHours?: string;
  aboutText: string;
  shippingPolicy: string;
  returnPolicy: string;
  privacyPolicy: string;
  termsPolicy: string;
  defaultCurrency: CurrencyCode;
  usdRateNgn: number;
  paystackEnabled: boolean;
  paystackPublicKey?: string;
  flutterwaveEnabled: boolean;
  flutterwavePublicKey?: string;
  bankTransferEnabled: boolean;
  codEnabled: boolean;
  guestCheckoutEnabled?: boolean;
  maintenanceMode?: boolean;
  maintenanceMessage?: string;
  bankName: string;
  accountName: string;
  accountNumber: string;
  instagramHandle?: string;
  instagramUrl?: string;
  tiktokHandle?: string;
  tiktokUrl?: string;
  freeDeliveryThresholdNgn?: number;
}

export interface AiAssistantConfig {
  enabled: boolean;
  assistantName: string;
  toneStyle: string;
  humanSupportAvailable: boolean;
  supportWorkingDays: string;
  supportWorkingHours: string;
  expectedResponseTimeMessage: string;
}

export interface CartItem {
  id: string;
  userId: string;
  productId: string;
  productTitle: string;
  productImageUrl?: string;
  variantLabel: string;
  unitPriceNgn: number;
  quantity: number;
}

export interface WishlistItem {
  id: string;
  userId: string;
  productId: string;
  productTitle: string;
  productImageUrl?: string;
  priceNgn: number;
}

export interface DeliveryAddress {
  id: string;
  userId: string;
  label: string;
  recipientName: string;
  phone: string;
  country: string;
  state: string;
  city: string;
  lga?: string;
  streetAddress: string;
  landmark?: string;
  deliveryInstructions?: string;
  defaultAddress: boolean;
}

export interface OrderStatusHistoryEntry {
  stage: string;
  label: string;
  note?: string;
  actorRole?: string;
  timestampMillis?: number;
}

export interface StoreOrder {
  id: string;
  userId: string;
  orderNumber: string;
  itemsSummary: string;
  itemCount: number;
  subtotalNgn: number;
  deliveryFeeNgn: number;
  discountNgn: number;
  totalNgn: number;
  currency: CurrencyCode;
  deliveryZoneId: string;
  recipientName?: string;
  recipientPhone?: string;
  shippingAddress?: string;
  shippingCity: string;
  shippingState: string;
  shippingCountry?: string;
  lga?: string;
  landmark?: string;
  deliveryInstructions?: string;
  status: string;
  paymentMethod: 'PAYSTACK' | 'FLUTTERWAVE' | 'BANK_TRANSFER' | 'COD';
  paymentStatus: string;
  internalNotes?: string;
  cancellationRequestReason?: string;
  cancellationStatus?: string;
  refundedNgn?: number;
  customerEmail?: string;
  trackingToken?: string;
  guestLookupHashEmail?: string;
  guestLookupHashPhone?: string;
  courierName?: string;
  trackingReference?: string;
  trackingUrl?: string;
  estimatedDeliveryWindow?: string;
  deliveryStatus?: string;
  deliveryNotes?: string;
  statusHistory?: OrderStatusHistoryEntry[];
  createdAt?: unknown;
}

export interface PaymentRecord {
  id: string;
  userId: string;
  orderId: string;
  orderNumber?: string;
  customerName?: string;
  provider: string;
  reference: string;
  amountNgn: number;
  currency: CurrencyCode;
  status: string;
  transferNote?: string;
  verificationNote?: string;
  reviewedByEmail?: string;
  reviewedAt?: unknown;
  createdAt?: unknown;
}

export interface StoreNotification {
  id: string;
  userId: string;
  title: string;
  body: string;
  category: string;
  readFlag: boolean;
  targetRoute?: string;
  orderId?: string;
  orderNumber?: string;
  createdAt?: unknown;
}

export interface CustomerSummary {
  id: string;
  userId: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  preferredCurrency?: string;
  role?: string;
  emailVerified?: boolean;
  supportNotes?: string;
  createdAt?: unknown;
}

export interface CustomerPrivateInfo {
  userId: string;
  email: string;
  phone?: string;
  shippingAddress?: string;
  city?: string;
  state?: string;
  country?: string;
}

export interface ProductReview {
  id: string;
  productId: string;
  userId: string;
  reviewerName: string;
  rating: number;
  title?: string;
  comment: string;
  verifiedPurchase?: boolean;
  approved?: boolean;
  createdAt?: unknown;
}

export interface SupportEnquiry {
  id: string;
  userId: string;
  customerName: string;
  customerEmail?: string;
  category: string;
  subject: string;
  message: string;
  orderId?: string;
  orderNumber?: string;
  status: string;
  priority: string;
  sourceChannel?: string;
  lastReplyPreview?: string;
  createdAt?: unknown;
}

export interface SupportReply {
  id: string;
  enquiryId: string;
  ownerUid: string;
  senderUid: string;
  senderName: string;
  senderRole: 'CUSTOMER' | 'ADMIN';
  message: string;
  createdAt?: unknown;
}
