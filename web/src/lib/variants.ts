import { InventoryItem, Product } from '../types/store';

export interface VariantMatrixEntry {
  key: string;
  size: string;
  color: string;
  sku?: string;
  priceNgn?: number;
  priceUsd?: number;
}

export interface ResolvedVariantState {
  key: string;
  priceNgn: number;
  priceUsd: number;
  sku: string;
  stockQuantity: number | null;
  hasVariantStock: boolean;
  available: boolean;
}

export function buildVariantKey(size?: string, color?: string): string {
  const cleanSize = (size || '').trim();
  const cleanColor = (color || '').trim();
  if (cleanSize && cleanColor) return `${cleanSize} · ${cleanColor}`;
  return cleanSize || cleanColor || 'Standard';
}

function normalizeVariantCompare(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/\s*[•·\-|/]\s*/g, ' · ')
    .replace(/\s+/g, ' ');
}

export function parseVariantMatrix(product: Pick<Product, 'attributes'>): Record<string, VariantMatrixEntry> {
  const raw = product.attributes?.variantMatrixJson;
  if (!raw || typeof raw !== 'string') return {};
  try {
    const parsed = JSON.parse(raw) as Record<string, Partial<VariantMatrixEntry>>;
    if (!parsed || typeof parsed !== 'object') return {};
    const out: Record<string, VariantMatrixEntry> = {};
    Object.entries(parsed).forEach(([k, val]) => {
      if (!val || typeof val !== 'object') return;
      const key = (val.key || k).trim();
      if (!key) return;
      out[key] = {
        key,
        size: typeof val.size === 'string' ? val.size : '',
        color: typeof val.color === 'string' ? val.color : '',
        sku: typeof val.sku === 'string' && val.sku.trim() ? val.sku.trim() : undefined,
        priceNgn: typeof val.priceNgn === 'number' && val.priceNgn >= 0 ? val.priceNgn : undefined,
        priceUsd: typeof val.priceUsd === 'number' && val.priceUsd >= 0 ? val.priceUsd : undefined,
      };
    });
    return out;
  } catch {
    return {};
  }
}

export function serializeVariantMatrix(entries: VariantMatrixEntry[]): string {
  const compact: Record<string, VariantMatrixEntry> = {};
  entries.forEach((entry) => {
    const hasCustomPrice = typeof entry.priceNgn === 'number' && entry.priceNgn >= 0;
    const hasCustomUsd = typeof entry.priceUsd === 'number' && entry.priceUsd >= 0;
    const hasCustomSku = Boolean(entry.sku?.trim());
    if (!hasCustomPrice && !hasCustomUsd && !hasCustomSku) return;
    compact[entry.key] = {
      key: entry.key,
      size: entry.size,
      color: entry.color,
      ...(hasCustomSku ? { sku: entry.sku?.trim().slice(0, 48) } : {}),
      ...(hasCustomPrice ? { priceNgn: Number(entry.priceNgn) } : {}),
      ...(hasCustomUsd ? { priceUsd: Number(entry.priceUsd) } : {}),
    };
  });
  return Object.keys(compact).length > 0 ? JSON.stringify(compact) : '';
}

export function findVariantStockValue(
  variantStock: Record<string, number> | undefined,
  variantKey: string
): number | null {
  if (!variantStock) return null;
  if (typeof variantStock[variantKey] === 'number') return variantStock[variantKey];
  const target = normalizeVariantCompare(variantKey);
  for (const [k, qty] of Object.entries(variantStock)) {
    if (normalizeVariantCompare(k) === target && typeof qty === 'number') {
      return qty;
    }
  }
  return null;
}

export function resolveVariantDetails(
  product: Product,
  inventory: InventoryItem | undefined,
  size: string,
  color: string,
  usdRateNgn = 1600
): ResolvedVariantState {
  const key = buildVariantKey(size, color);
  const targetNorm = normalizeVariantCompare(key);
  const sizeNorm = size.trim().toLowerCase();
  const colorNorm = color.trim().toLowerCase();

  const matrix = parseVariantMatrix(product);
  let matchedEntry: VariantMatrixEntry | undefined = matrix[key];
  if (!matchedEntry) {
    matchedEntry = Object.values(matrix).find(
      (entry) => normalizeVariantCompare(entry.key) === targetNorm
    );
  }

  const priceNgn =
    typeof matchedEntry?.priceNgn === 'number' && matchedEntry.priceNgn > 0
      ? matchedEntry.priceNgn
      : product.priceNgn;
  const priceUsd =
    typeof matchedEntry?.priceUsd === 'number' && matchedEntry.priceUsd > 0
      ? matchedEntry.priceUsd
      : typeof matchedEntry?.priceNgn === 'number' && matchedEntry.priceNgn > 0
      ? Math.round((matchedEntry.priceNgn / Math.max(usdRateNgn || 1600, 1)) * 100) / 100
      : product.priceUsd;

  const baseSku = (inventory?.sku || product.sku || `ZC-${product.id.slice(-6).toUpperCase()}`).trim();
  const sku = matchedEntry?.sku?.trim() || baseSku;

  const variantQty = findVariantStockValue(inventory?.variantStock, key);
  const hasVariantStock = variantQty !== null;
  const effectiveStock = hasVariantStock
    ? variantQty
    : inventory
    ? inventory.stockQuantity
    : null;

  const isMarkedUnavailable = (product.unavailableVariants || []).some((blocked) => {
    const bNorm = normalizeVariantCompare(blocked);
    return bNorm === targetNorm || bNorm === sizeNorm || (colorNorm && bNorm === colorNorm);
  });

  const globalOutOfStock = inventory ? !inventory.inStock || inventory.stockQuantity <= 0 : false;
  const variantOutOfStock = hasVariantStock ? (variantQty ?? 0) <= 0 : false;

  return {
    key,
    priceNgn,
    priceUsd,
    sku,
    stockQuantity: effectiveStock,
    hasVariantStock,
    available: !isMarkedUnavailable && !globalOutOfStock && !variantOutOfStock,
  };
}
