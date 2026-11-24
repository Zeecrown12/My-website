import { CurrencyCode } from '../types/store';

export const OFFICIAL_WHATSAPP_DISPLAY = '+234 816 989 8763';
export const OFFICIAL_WHATSAPP_URL = 'https://wa.me/2348169898763';
export const NO_RETURNS_POLICY_DEFAULT =
  'ZEECROWN STORE currently does not accept customer returns or refunds. Please carefully review product descriptions, photos, colour notes, and the Size Guide before placing an order, or message Customer Care on WhatsApp (+234 816 989 8763) prior to checkout. If an item arrives damaged or incorrect, contact Customer Care with your order number for direct review.';

export function resolveEffectiveReturnPolicy(configured?: string): string {
  const clean = (configured || '').trim();
  if (
    !clean ||
    /7[\s-]?day/i.test(clean) ||
    /within\s+7\s+days/i.test(clean) ||
    /may\s+be\s+returned/i.test(clean)
  ) {
    return NO_RETURNS_POLICY_DEFAULT;
  }
  return clean;
}

export function buildWhatsAppLink(number?: string, prefill?: string): string {
  const digits = (number || OFFICIAL_WHATSAPP_DISPLAY).replace(/\D/g, '');
  const normalized = digits.startsWith('0') && digits.length === 11 ? `234${digits.slice(1)}` : digits || '2348169898763';
  const base = `https://wa.me/${normalized}`;
  if (!prefill) return base;
  return `${base}?text=${encodeURIComponent(prefill)}`;
}

export function formatMoney(amountNgn: number, currency: CurrencyCode, usdPriceOverride?: number, usdRateNgn = 1600): string {
  if (currency === 'USD') {
    const usd = typeof usdPriceOverride === 'number' && usdPriceOverride > 0
      ? usdPriceOverride
      : amountNgn / Math.max(usdRateNgn || 1600, 1);
    return `$${usd.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
  return `₦${Math.round(amountNgn).toLocaleString('en-NG')}`;
}

export function normalizeSlug(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 110);
}

export function formatOrderStatus(status: string): string {
  return (status || 'PENDING_PAYMENT')
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}
