import React, { useEffect, useMemo, useState } from 'react';
import {
  collection,
  deleteField,
  doc,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from 'firebase/firestore';
import {
  ArrowLeft,
  ArrowRight,
  Copy,
  Eye,
  EyeOff,
  Search,
  Sparkles,
  Star,
  Trash2,
  Upload,
} from 'lucide-react';
import { db } from '../../lib/firebase';
import { normalizeSlug } from '../../lib/formatters';
import {
  buildVariantKey,
  findVariantStockValue,
  parseVariantMatrix,
  serializeVariantMatrix,
} from '../../lib/variants';
import {
  Category,
  InventoryItem,
  Product,
  SizeGuide,
  StoreCollection,
  StoreSettings,
} from '../../types/store';
import { AdminActor, recordAdminAudit, uploadProductPhotoFile } from './adminHelpers';

interface VariantDraftRow {
  key: string;
  size: string;
  color: string;
  sku: string;
  priceNgn: string;
  priceUsd: string;
  stockQty: string;
  available: boolean;
}

interface AdminProductsTabProps {
  actor: AdminActor;
  allProducts: Product[];
  categories: Category[];
  collections: StoreCollection[];
  sizeGuides: SizeGuide[];
  inventoryByProduct: Record<string, InventoryItem>;
  storeSettings: StoreSettings;
  onRefreshCatalog: () => Promise<void>;
  onStatus: (msg: string) => void;
}

export function AdminProductsTab({
  actor,
  allProducts,
  categories,
  collections,
  sizeGuides,
  inventoryByProduct,
  storeSettings,
  onRefreshCatalog,
  onStatus,
}: AdminProductsTabProps) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [pTitle, setPTitle] = useState('');
  const [pSummary, setPSummary] = useState('');
  const [pDesc, setPDesc] = useState('');
  const [pCatId, setPCatId] = useState('');
  const [pCollectionId, setPCollectionId] = useState('');
  const [pSizeGuideId, setPSizeGuideId] = useState('');
  const [pBrand, setPBrand] = useState('ZEECROWN Atelier');
  const [pSku, setPSku] = useState('');
  const [pPriceNgn, setPPriceNgn] = useState('');
  const [pCompareAtNgn, setPCompareAtNgn] = useState('');
  const [pPriceUsd, setPPriceUsd] = useState('');
  const [pStock, setPStock] = useState('10');
  const [pLowStockThreshold, setPLowStockThreshold] = useState('3');
  const [pSizes, setPSizes] = useState('S, M, L, XL');
  const [pColors, setPColors] = useState('Royal Purple, Gold');
  const [pFeatures, setPFeatures] = useState('');
  const [pFashionStyle, setPFashionStyle] = useState('');
  const [pTags, setPTags] = useState('');
  const [pImageUrls, setPImageUrls] = useState<string[]>([]);
  const [photoUrlInput, setPhotoUrlInput] = useState('');
  const [pFeatured, setPFeatured] = useState(true);
  const [pNewArrival, setPNewArrival] = useState(true);
  const [pActive, setPActive] = useState(true);
  const [variantRows, setVariantRows] = useState<VariantDraftRow[]>([]);
  const [saving, setSaving] = useState(false);

  // List filters
  const [listQuery, setListQuery] = useState('');
  const [listFilter, setListFilter] = useState<'ALL' | 'ACTIVE' | 'ARCHIVED'>('ALL');

  useEffect(() => {
    if (categories.length > 0 && !pCatId) {
      setPCatId(categories[0].id);
    }
  }, [categories, pCatId]);

  const resetForm = () => {
    setEditingId(null);
    setPTitle('');
    setPSummary('');
    setPDesc('');
    setPCatId(categories[0]?.id || '');
    setPCollectionId('');
    setPSizeGuideId('');
    setPBrand('ZEECROWN Atelier');
    setPSku('');
    setPPriceNgn('');
    setPCompareAtNgn('');
    setPPriceUsd('');
    setPStock('10');
    setPLowStockThreshold('3');
    setPSizes('S, M, L, XL');
    setPColors('Royal Purple, Gold');
    setPFeatures('');
    setPFashionStyle('');
    setPTags('');
    setPImageUrls([]);
    setPhotoUrlInput('');
    setPFeatured(true);
    setPNewArrival(true);
    setPActive(true);
    setVariantRows([]);
  };

  const syncVariantRowsFromInputs = (
    rawSizes = pSizes,
    rawColors = pColors,
    existingProduct?: Product,
    existingInv?: InventoryItem
  ) => {
    const sizes = rawSizes
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
      .slice(0, 15);
    const colors = rawColors
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
      .slice(0, 15);

    const sizeList = sizes.length > 0 ? sizes : ['Standard'];
    const colorList = colors.length > 0 ? colors : [''];
    const matrix = existingProduct ? parseVariantMatrix(existingProduct) : {};
    const unavailableSet = new Set(
      (existingProduct?.unavailableVariants || []).map((v) => v.trim().toLowerCase())
    );

    const currentByKey = new Map(variantRows.map((r) => [r.key, r]));
    const nextRows: VariantDraftRow[] = [];

    for (const s of sizeList) {
      for (const c of colorList) {
        if (nextRows.length >= 40) break;
        const key = buildVariantKey(s, c);
        const existingRow = currentByKey.get(key);
        const storedEntry = matrix[key];
        const storedStock = findVariantStockValue(existingInv?.variantStock, key);
        const blocked =
          unavailableSet.has(key.toLowerCase()) ||
          unavailableSet.has(s.toLowerCase()) ||
          (c ? unavailableSet.has(c.toLowerCase()) : false);

        nextRows.push({
          key,
          size: s,
          color: c,
          sku: existingRow?.sku ?? storedEntry?.sku ?? '',
          priceNgn:
            existingRow?.priceNgn ??
            (typeof storedEntry?.priceNgn === 'number' ? String(storedEntry.priceNgn) : ''),
          priceUsd:
            existingRow?.priceUsd ??
            (typeof storedEntry?.priceUsd === 'number' ? String(storedEntry.priceUsd) : ''),
          stockQty:
            existingRow?.stockQty ??
            (storedStock !== null ? String(storedStock) : ''),
          available: existingRow ? existingRow.available : !blocked,
        });
      }
    }
    setVariantRows(nextRows);
  };

  const handleLoadProductForEdit = (p: Product) => {
    const inv = inventoryByProduct[p.id];
    setEditingId(p.id);
    setPTitle(p.title);
    setPSummary(p.summary || '');
    setPDesc(p.description);
    setPCatId(p.categoryId || categories[0]?.id || '');
    setPCollectionId(p.collectionId || '');
    setPSizeGuideId(p.sizeGuideId || '');
    setPBrand(p.brand || 'ZEECROWN Atelier');
    setPSku(inv?.sku || p.sku || '');
    setPPriceNgn(String(p.priceNgn));
    setPCompareAtNgn(p.compareAtPriceNgn ? String(p.compareAtPriceNgn) : '');
    setPPriceUsd(String(p.priceUsd));
    setPStock(String(inv?.stockQuantity ?? 0));
    setPLowStockThreshold(String(inv?.lowStockThreshold ?? p.lowStockThreshold ?? 3));
    const sizesStr = p.sizes.join(', ');
    const colorsStr = p.colors.join(', ');
    setPSizes(sizesStr);
    setPColors(colorsStr);
    setPFeatures((p.features || []).join('\n'));
    setPFashionStyle(p.fashionStyle || '');
    setPTags((p.tags || []).join(', '));
    setPImageUrls(p.imageUrls.length > 0 ? p.imageUrls : p.mainImageUrl ? [p.mainImageUrl] : []);
    setPFeatured(p.featured);
    setPNewArrival(p.newArrival !== false);
    setPActive(p.active);
    syncVariantRowsFromInputs(sizesStr, colorsStr, p, inv);
    onStatus(`Loaded "${p.title}" for editing.`);
  };

  const handleUploadMultipleImages = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;
    const remainingSlots = Math.max(0, 10 - pImageUrls.length);
    if (remainingSlots === 0) {
      onStatus('Maximum 10 product photos reached. Remove an existing photo before adding more.');
      return;
    }

    const batch = files.slice(0, remainingSlots);
    onStatus(`Uploading ${batch.length} product photo(s)...`);
    const targetProdId = editingId || `draft_${Date.now()}`;
    const uploadedUrls: string[] = [];
    let lastError = '';

    for (const file of batch) {
      try {
        const res = await uploadProductPhotoFile(file, targetProdId);
        uploadedUrls.push(res.url);
      } catch (err) {
        lastError = err instanceof Error ? err.message : 'Upload unavailable';
      }
    }

    if (uploadedUrls.length > 0) {
      setPImageUrls((prev) => Array.from(new Set([...prev, ...uploadedUrls])).slice(0, 10));
      onStatus(`Uploaded ${uploadedUrls.length} photo(s) cleanly.`);
    } else {
      onStatus(
        `Direct Storage upload unavailable on Spark (${lastError}). Paste HTTPS image URLs or /assets/... paths below (you can add multiple separated by commas or new lines).`
      );
    }
    e.target.value = '';
  };

  const handleAddPhotoUrls = () => {
    const entries = photoUrlInput
      .split(/[\n,]+/)
      .map((s) => s.trim())
      .filter(Boolean);
    if (entries.length === 0) return;
    const valid = entries.filter((u) => u.startsWith('https://') || u.startsWith('/assets/'));
    if (valid.length === 0) {
      onStatus('Enter valid https:// image URL(s) or /assets/... paths.');
      return;
    }
    setPImageUrls((prev) => Array.from(new Set([...prev, ...valid])).slice(0, 10));
    setPhotoUrlInput('');
    onStatus(`Added ${valid.length} photo URL(s) to product gallery.`);
  };

  const moveImage = (index: number, delta: number) => {
    const target = index + delta;
    if (target < 0 || target >= pImageUrls.length) return;
    const copy = [...pImageUrls];
    const [picked] = copy.splice(index, 1);
    copy.splice(target, 0, picked);
    setPImageUrls(copy);
  };

  const makeMainImage = (index: number) => {
    if (index <= 0 || index >= pImageUrls.length) return;
    const copy = [...pImageUrls];
    const [picked] = copy.splice(index, 1);
    copy.unshift(picked);
    setPImageUrls(copy);
  };

  const handleGenerateSmartDraft = () => {
    const catNameSelected = categories.find((c) => c.id === pCatId)?.name || 'Luxury Couture';
    const ngn = Number(pPriceNgn);
    if (!pPriceUsd.trim() && !Number.isNaN(ngn) && ngn > 0) {
      const computedUsd = Math.round((ngn / Math.max(storeSettings.usdRateNgn || 1600, 1)) * 100) / 100;
      setPPriceUsd(String(computedUsd));
    }
    if (!pDesc.trim()) {
      const titleText = pTitle.trim() || `${catNameSelected} Piece`;
      setPDesc(
        `${titleText} from ZEECROWN STORE — handcrafted in our ${catNameSelected} edit with refined tailoring, lasting comfort, and timeless Nigerian modest elegance.`
      );
    }
    if (!pFeatures.trim()) {
      setPFeatures(
        `Hand-finished ${catNameSelected} craftsmanship\nBreathable luxury drape and tailored silhouette\nAvailable for nationwide & international dispatch`
      );
    }
    onStatus('Smart draft populated locally (free Spark helper).');
  };

  const handleAutoSkuMatrix = () => {
    const baseSku = (pSku.trim() || `ZC-${normalizeSlug(pTitle || 'ITEM').slice(0, 6).toUpperCase() || 'NEW'}`).toUpperCase();
    if (!pSku.trim()) setPSku(baseSku);
    if (variantRows.length === 0) {
      syncVariantRowsFromInputs();
      return;
    }
    setVariantRows((prev) =>
      prev.map((row) => {
        if (row.sku.trim()) return row;
        const sizeCode = row.size.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 4);
        const colorCode = row.color.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 4);
        const suffix = [sizeCode, colorCode].filter(Boolean).join('-');
        return { ...row, sku: `${baseSku}${suffix ? `-${suffix}` : ''}`.slice(0, 48) };
      })
    );
    onStatus('Generated default variant SKUs from product SKU.');
  };

  const handleSaveProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    const ngn = Number(pPriceNgn);
    if (!pTitle.trim() || !pDesc.trim() || !pCatId || Number.isNaN(ngn) || ngn < 0) {
      onStatus('Title, description, category, and valid NGN price are required.');
      return;
    }
    setSaving(true);
    try {
      const rate = Math.max(storeSettings.usdRateNgn || 1600, 1);
      const usd = Number(pPriceUsd) > 0 ? Number(pPriceUsd) : Math.round((ngn / rate) * 100) / 100;
      const compareNgn = Number(pCompareAtNgn);
      const prodId = editingId || `prod_${Date.now()}`;
      const slug = normalizeSlug(pTitle) || prodId;
      const sizes = pSizes
        .split(',')
        .map((s) => s.trim().slice(0, 40))
        .filter(Boolean)
        .slice(0, 15);
      const colors = pColors
        .split(',')
        .map((s) => s.trim().slice(0, 40))
        .filter(Boolean)
        .slice(0, 15);
      const features = pFeatures
        .split('\n')
        .map((s) => s.trim().slice(0, 140))
        .filter(Boolean)
        .slice(0, 15);
      const tags = pTags
        .split(',')
        .map((s) => s.trim().slice(0, 40))
        .filter(Boolean)
        .slice(0, 20);
      const resolvedSku = (pSku.trim() || `ZC-${prodId.slice(-6).toUpperCase()}`).slice(0, 60);
      const lowThreshold = Math.max(0, Math.min(10000, Math.round(Number(pLowStockThreshold) || 3)));

      // Per-variant matrix (pricing + SKUs) and per-variant stock + unavailableVariants
      const variantStockMap: Record<string, number> = {};
      const unavailableVariants: string[] = [];
      let variantQtyCount = 0;
      let variantQtySum = 0;

      const matrixEntries = variantRows.map((row) => {
        if (!row.available) {
          unavailableVariants.push(row.key);
        }
        if (row.stockQty.trim() !== '') {
          const vq = Math.max(0, Math.min(100000, Math.round(Number(row.stockQty) || 0)));
          variantStockMap[row.key] = vq;
          variantQtyCount++;
          variantQtySum += vq;
        }
        const vPriceNgn =
          row.priceNgn.trim() !== '' && !Number.isNaN(Number(row.priceNgn))
            ? Number(row.priceNgn)
            : undefined;
        const vPriceUsd =
          row.priceUsd.trim() !== '' && !Number.isNaN(Number(row.priceUsd))
            ? Number(row.priceUsd)
            : typeof vPriceNgn === 'number'
            ? Math.round((vPriceNgn / rate) * 100) / 100
            : undefined;
        return {
          key: row.key,
          size: row.size,
          color: row.color,
          sku: row.sku.trim() || undefined,
          priceNgn: vPriceNgn,
          priceUsd: vPriceUsd,
        };
      });

      const matrixJson = serializeVariantMatrix(matrixEntries);
      const existingAttributes = { ...(allProducts.find((x) => x.id === prodId)?.attributes || {}) };
      if (matrixJson) {
        existingAttributes.variantMatrixJson = matrixJson;
      } else {
        delete existingAttributes.variantMatrixJson;
      }

      const catName = categories.find((c) => c.id === pCatId)?.name || '';
      const searchTokens = Array.from(
        new Set(
          [
            ...pTitle.toLowerCase().split(/\s+/),
            ...catName.toLowerCase().split(/\s+/),
            ...sizes.map((s) => s.toLowerCase()),
            ...colors.map((c) => c.toLowerCase()),
            ...tags.map((t) => t.toLowerCase()),
            resolvedSku.toLowerCase(),
          ].filter((t) => t.length >= 2)
        )
      ).slice(0, 75);

      const qty =
        variantQtyCount > 0
          ? variantQtySum
          : Math.max(0, Math.min(100000, Math.round(Number(pStock) || 0)));

      onStatus('Saving product & variant inventory...');
      const productPayload: Record<string, unknown> = {
        title: pTitle.trim().slice(0, 120),
        slug: slug.slice(0, 120),
        description: pDesc.trim().slice(0, 2000),
        summary: pSummary.trim().slice(0, 300),
        categoryId: pCatId,
        collectionId: pCollectionId.trim().slice(0, 128),
        sizeGuideId: pSizeGuideId.trim().slice(0, 128),
        brand: (pBrand.trim() || 'ZEECROWN Atelier').slice(0, 80),
        sku: resolvedSku,
        priceNgn: ngn,
        priceUsd: usd,
        mainImageUrl: (pImageUrls[0] || '').slice(0, 600),
        imageUrls: pImageUrls.slice(0, 10),
        sizes,
        colors,
        unavailableVariants: unavailableVariants.slice(0, 40),
        attributes: existingAttributes,
        features,
        fashionStyle: pFashionStyle.trim().slice(0, 80),
        tags,
        searchTokens,
        lowStockThreshold: lowThreshold,
        featured: pFeatured,
        newArrival: pNewArrival,
        active: pActive,
        updatedAt: serverTimestamp(),
      };

      if (!Number.isNaN(compareNgn) && compareNgn > ngn) {
        productPayload.compareAtPriceNgn = compareNgn;
      } else if (editingId) {
        productPayload.compareAtPriceNgn = deleteField();
      }

      if (editingId) {
        await updateDoc(doc(db, 'products', prodId), productPayload);
      } else {
        productPayload.soldCount = 0;
        productPayload.createdAt = serverTimestamp();
        await setDoc(doc(db, 'products', prodId), productPayload);
      }

      const prevStock = inventoryByProduct[prodId]?.stockQuantity ?? 0;
      const inventoryPayload: Record<string, unknown> = {
        productId: prodId,
        sku: resolvedSku.length >= 2 ? resolvedSku : `ZC-${prodId.slice(-6).toUpperCase()}`,
        stockQuantity: qty,
        lowStockThreshold: lowThreshold,
        reservedQuantity: inventoryByProduct[prodId]?.reservedQuantity ?? 0,
        variantStock: variantStockMap,
        inStock: qty > 0,
        updatedAt: serverTimestamp(),
      };
      await setDoc(doc(db, 'inventory', prodId), inventoryPayload, { merge: true });

      if (!editingId || prevStock !== qty) {
        await setDoc(doc(collection(db, 'inventoryTransactions')), {
          productId: prodId,
          productTitle: pTitle.trim().slice(0, 120),
          sku: String(inventoryPayload.sku),
          previousQty: prevStock,
          deltaQty: qty - prevStock,
          newQty: qty,
          reason: editingId ? 'Product editor stock update' : 'Initial product stock',
          adminUid: actor.uid,
          adminEmail: actor.email || actor.uid,
          createdAt: serverTimestamp(),
        }).catch(() => undefined);
      }

      await recordAdminAudit(
        actor,
        editingId ? 'UPDATE_PRODUCT' : 'CREATE_PRODUCT',
        'product',
        prodId,
        `${editingId ? 'Updated' : 'Created'} product '${pTitle.trim()}' (stock ${qty})`
      );

      resetForm();
      onStatus('Product, variants & inventory saved cleanly!');
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to save product.');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleActive = async (p: Product) => {
    try {
      const next = !p.active;
      await updateDoc(doc(db, 'products', p.id), {
        active: next,
        updatedAt: serverTimestamp(),
      });
      await recordAdminAudit(
        actor,
        next ? 'PUBLISH_PRODUCT' : 'ARCHIVE_PRODUCT',
        'product',
        p.id,
        `${next ? 'Published' : 'Archived'} '${p.title}'`
      );
      onStatus(`"${p.title}" ${next ? 'published to shop' : 'archived (hidden from shop)'}.`);
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to update product status.');
    }
  };

  const handleDuplicateProduct = async (p: Product) => {
    try {
      const newId = `prod_${Date.now()}`;
      const inv = inventoryByProduct[p.id];
      const newSku = `ZC-${newId.slice(-6).toUpperCase()}`;
      const newTitle = `${p.title} (Copy)`.slice(0, 120);
      await setDoc(doc(db, 'products', newId), {
        title: newTitle,
        slug: `${normalizeSlug(p.slug || p.title)}-${newId.slice(-4)}`.slice(0, 120),
        description: p.description || newTitle,
        summary: p.summary || '',
        categoryId: p.categoryId || categories[0]?.id || 'cat_general',
        collectionId: p.collectionId || '',
        sizeGuideId: p.sizeGuideId || '',
        brand: p.brand || 'ZEECROWN Atelier',
        sku: newSku,
        priceNgn: p.priceNgn,
        ...(p.compareAtPriceNgn && p.compareAtPriceNgn > p.priceNgn
          ? { compareAtPriceNgn: p.compareAtPriceNgn }
          : {}),
        priceUsd: p.priceUsd,
        mainImageUrl: p.mainImageUrl || p.imageUrls[0] || '',
        imageUrls: p.imageUrls || [],
        sizes: p.sizes || [],
        colors: p.colors || [],
        unavailableVariants: p.unavailableVariants || [],
        attributes: p.attributes || {},
        features: p.features || [],
        fashionStyle: p.fashionStyle || '',
        tags: p.tags || [],
        lowStockThreshold: inv?.lowStockThreshold ?? p.lowStockThreshold ?? 3,
        featured: false,
        newArrival: false,
        soldCount: 0,
        active: false,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      await setDoc(doc(db, 'inventory', newId), {
        productId: newId,
        sku: newSku,
        stockQuantity: inv?.stockQuantity ?? 0,
        lowStockThreshold: inv?.lowStockThreshold ?? 3,
        reservedQuantity: 0,
        variantStock: inv?.variantStock || {},
        inStock: (inv?.stockQuantity ?? 0) > 0,
        updatedAt: serverTimestamp(),
      });
      await recordAdminAudit(actor, 'DUPLICATE_PRODUCT', 'product', newId, `Duplicated '${p.title}' as draft`);
      onStatus(`Duplicated "${p.title}" as an archived draft.`);
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to duplicate product.');
    }
  };

  const handleDeleteProduct = async (p: Product) => {
    if (!window.confirm(`Permanently delete "${p.title}" and its inventory record? This cannot be undone.`)) {
      return;
    }
    try {
      const batch = writeBatch(db);
      batch.delete(doc(db, 'products', p.id));
      batch.delete(doc(db, 'inventory', p.id));
      await batch.commit();
      await recordAdminAudit(actor, 'DELETE_PRODUCT', 'product', p.id, `Deleted product '${p.title}'`);
      if (editingId === p.id) resetForm();
      onStatus(`Deleted "${p.title}".`);
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to delete product.');
    }
  };

  const filteredProducts = useMemo(() => {
    const q = listQuery.trim().toLowerCase();
    return allProducts.filter((p) => {
      if (listFilter === 'ACTIVE' && !p.active) return false;
      if (listFilter === 'ARCHIVED' && p.active) return false;
      if (!q) return true;
      const sku = inventoryByProduct[p.id]?.sku || p.sku || '';
      return p.title.toLowerCase().includes(q) || sku.toLowerCase().includes(q);
    });
  }, [allProducts, inventoryByProduct, listQuery, listFilter]);

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))',
        gap: 24,
        alignItems: 'start',
      }}
    >
      <form onSubmit={(e) => void handleSaveProduct(e)} className="card" style={{ padding: 22 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3 style={{ margin: 0 }}>{editingId ? 'Edit Product & Variants' : 'Add New Product'}</h3>
          {editingId && (
            <button type="button" className="zc-pill-btn" onClick={resetForm}>
              + New Product
            </button>
          )}
        </div>

        <div style={{ marginBottom: 12, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <label className="zc-pill-btn" style={{ cursor: 'pointer' }}>
            <Upload size={15} /> Upload Photos (Multi)
            <input
              type="file"
              accept="image/*"
              multiple
              hidden
              onChange={(e) => void handleUploadMultipleImages(e)}
            />
          </label>
          <button type="button" className="zc-pill-btn primary" onClick={handleGenerateSmartDraft}>
            <Sparkles size={15} /> Auto-Fill Draft &amp; USD
          </button>
        </div>

        <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
          <input
            value={photoUrlInput}
            onChange={(e) => setPhotoUrlInput(e.target.value)}
            placeholder="Paste HTTPS photo URL(s) or /assets/... (comma-separated)"
            style={{ flex: 1, padding: '8px 12px', borderRadius: 10, border: '1px solid #D9D9E0' }}
          />
          <button type="button" className="zc-pill-btn" onClick={handleAddPhotoUrls}>
            Add URL
          </button>
        </div>

        {pImageUrls.length > 0 && (
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 14 }}>
            {pImageUrls.map((url, idx) => (
              <div
                key={`${url}-${idx}`}
                style={{
                  border: idx === 0 ? '2px solid #542080' : '1px solid #D9D9E0',
                  borderRadius: 10,
                  padding: 4,
                  background: '#FFFCF5',
                }}
              >
                <img
                  src={url}
                  alt=""
                  style={{ width: 66, height: 78, objectFit: 'cover', borderRadius: 6, display: 'block' }}
                />
                <div style={{ display: 'flex', gap: 2, justifyContent: 'center', marginTop: 4 }}>
                  {idx > 0 && (
                    <button
                      type="button"
                      title="Set as main image"
                      aria-label="Set as main image"
                      onClick={() => makeMainImage(idx)}
                      style={{ border: 'none', background: '#F4EFF9', borderRadius: 4, padding: 3, cursor: 'pointer' }}
                    >
                      <Star size={11} color="#542080" />
                    </button>
                  )}
                  {idx > 0 && (
                    <button
                      type="button"
                      title="Move left"
                      aria-label="Move left"
                      onClick={() => moveImage(idx, -1)}
                      style={{ border: 'none', background: '#F4EFF9', borderRadius: 4, padding: 3, cursor: 'pointer' }}
                    >
                      <ArrowLeft size={11} />
                    </button>
                  )}
                  {idx < pImageUrls.length - 1 && (
                    <button
                      type="button"
                      title="Move right"
                      aria-label="Move right"
                      onClick={() => moveImage(idx, 1)}
                      style={{ border: 'none', background: '#F4EFF9', borderRadius: 4, padding: 3, cursor: 'pointer' }}
                    >
                      <ArrowRight size={11} />
                    </button>
                  )}
                  <button
                    type="button"
                    title="Remove photo"
                    aria-label="Remove photo"
                    onClick={() => setPImageUrls((prev) => prev.filter((_, i) => i !== idx))}
                    style={{ border: 'none', background: '#9E2A3B', color: '#FFF', borderRadius: 4, padding: 3, cursor: 'pointer' }}
                  >
                    <Trash2 size={11} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="field">
          <label>Product Title *</label>
          <input required value={pTitle} onChange={(e) => setPTitle(e.target.value)} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          <div className="field">
            <label>Category *</label>
            <select value={pCatId} onChange={(e) => setPCatId(e.target.value)}>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} {!c.active ? '(Archived)' : ''}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Collection (Optional)</label>
            <select value={pCollectionId} onChange={(e) => setPCollectionId(e.target.value)}>
              <option value="">None</option>
              {collections.map((col) => (
                <option key={col.id} value={col.id}>
                  {col.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
          <div className="field">
            <label>Base SKU</label>
            <input value={pSku} onChange={(e) => setPSku(e.target.value)} placeholder="ZC-ABY-001" />
          </div>
          <div className="field">
            <label>Brand</label>
            <input value={pBrand} onChange={(e) => setPBrand(e.target.value)} />
          </div>
          <div className="field">
            <label>Size Guide</label>
            <select value={pSizeGuideId} onChange={(e) => setPSizeGuideId(e.target.value)}>
              <option value="">Category Default</option>
              {sizeGuides.map((sg) => (
                <option key={sg.id} value={sg.id}>
                  {sg.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
          <div className="field">
            <label>Base Price (₦ NGN) *</label>
            <input
              required
              type="number"
              min={0}
              value={pPriceNgn}
              onChange={(e) => setPPriceNgn(e.target.value)}
            />
          </div>
          <div className="field">
            <label>Compare-At (₦)</label>
            <input
              type="number"
              min={0}
              value={pCompareAtNgn}
              onChange={(e) => setPCompareAtNgn(e.target.value)}
              placeholder="Optional"
            />
          </div>
          <div className="field">
            <label>Price ($ USD)</label>
            <input
              type="number"
              min={0}
              step="0.01"
              value={pPriceUsd}
              onChange={(e) => setPPriceUsd(e.target.value)}
            />
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          <div className="field">
            <label>Total Stock Qty</label>
            <input type="number" min={0} value={pStock} onChange={(e) => setPStock(e.target.value)} />
          </div>
          <div className="field">
            <label>Low-Stock Alert Threshold</label>
            <input
              type="number"
              min={0}
              value={pLowStockThreshold}
              onChange={(e) => setPLowStockThreshold(e.target.value)}
            />
          </div>
        </div>

        <div className="field">
          <label>Short Summary (Optional)</label>
          <input value={pSummary} onChange={(e) => setPSummary(e.target.value)} placeholder="One-line highlight" />
        </div>

        <div className="field">
          <label>Description *</label>
          <textarea required rows={3} value={pDesc} onChange={(e) => setPDesc(e.target.value)} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          <div className="field">
            <label>Sizes (comma-separated)</label>
            <input value={pSizes} onChange={(e) => setPSizes(e.target.value)} />
          </div>
          <div className="field">
            <label>Colours (comma-separated)</label>
            <input value={pColors} onChange={(e) => setPColors(e.target.value)} />
          </div>
        </div>

        <div
          style={{
            border: '1px solid #D9D9E0',
            borderRadius: 12,
            padding: 12,
            marginBottom: 14,
            background: '#F4EFF9',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <strong style={{ fontSize: '0.88rem' }}>
              Individual Variant Pricing, Stock &amp; SKUs ({variantRows.length})
            </strong>
            <div style={{ display: 'flex', gap: 6 }}>
              <button
                type="button"
                className="zc-pill-btn"
                onClick={() => syncVariantRowsFromInputs()}
              >
                Sync Combinations
              </button>
              <button type="button" className="zc-pill-btn" onClick={handleAutoSkuMatrix}>
                Auto SKUs
              </button>
            </div>
          </div>
          <p style={{ fontSize: '0.78rem', color: '#5C4B6E', margin: '6px 0 8px' }}>
            Leave variant price blank to inherit base price. Entering variant stock sums total stock automatically.
          </p>

          {variantRows.length > 0 && (
            <div className="table-wrap">
              <table className="zc-table" style={{ fontSize: '0.8rem' }}>
                <thead>
                  <tr>
                    <th>Variant</th>
                    <th>Variant SKU</th>
                    <th>Price ₦</th>
                    <th>Stock</th>
                    <th>Avail.</th>
                  </tr>
                </thead>
                <tbody>
                  {variantRows.map((row, idx) => (
                    <tr key={row.key}>
                      <td><strong>{row.key}</strong></td>
                      <td>
                        <input
                          value={row.sku}
                          onChange={(e) =>
                            setVariantRows((prev) =>
                              prev.map((r, i) => (i === idx ? { ...r, sku: e.target.value } : r))
                            )
                          }
                          placeholder="Auto/SKU"
                          style={{ width: 96, padding: '4px 6px', borderRadius: 6, border: '1px solid #D9D9E0' }}
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          min={0}
                          value={row.priceNgn}
                          onChange={(e) =>
                            setVariantRows((prev) =>
                              prev.map((r, i) => (i === idx ? { ...r, priceNgn: e.target.value } : r))
                            )
                          }
                          placeholder="Base"
                          style={{ width: 78, padding: '4px 6px', borderRadius: 6, border: '1px solid #D9D9E0' }}
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          min={0}
                          value={row.stockQty}
                          onChange={(e) =>
                            setVariantRows((prev) =>
                              prev.map((r, i) => (i === idx ? { ...r, stockQty: e.target.value } : r))
                            )
                          }
                          placeholder="-"
                          style={{ width: 60, padding: '4px 6px', borderRadius: 6, border: '1px solid #D9D9E0' }}
                        />
                      </td>
                      <td>
                        <input
                          type="checkbox"
                          checked={row.available}
                          aria-label={`Available ${row.key}`}
                          onChange={(e) =>
                            setVariantRows((prev) =>
                              prev.map((r, i) => (i === idx ? { ...r, available: e.target.checked } : r))
                            )
                          }
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="field">
          <label>Features (one per line)</label>
          <textarea rows={2} value={pFeatures} onChange={(e) => setPFeatures(e.target.value)} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          <div className="field">
            <label>Style (Optional)</label>
            <input value={pFashionStyle} onChange={(e) => setPFashionStyle(e.target.value)} placeholder="Modest Luxury" />
          </div>
          <div className="field">
            <label>Tags (comma-separated)</label>
            <input value={pTags} onChange={(e) => setPTags(e.target.value)} placeholder="silk, abaya, eid" />
          </div>
        </div>

        <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 14 }}>
          <label>
            <input type="checkbox" checked={pFeatured} onChange={(e) => setPFeatured(e.target.checked)} /> Featured
          </label>
          <label>
            <input type="checkbox" checked={pNewArrival} onChange={(e) => setPNewArrival(e.target.checked)} /> New Arrival
          </label>
          <label>
            <input type="checkbox" checked={pActive} onChange={(e) => setPActive(e.target.checked)} /> Active in Shop
          </label>
        </div>

        <div style={{ display: 'flex', gap: 10 }}>
          <button type="submit" className="btn" disabled={saving}>
            {saving ? 'Saving...' : editingId ? 'Save Changes' : 'Publish Product'}
          </button>
          {editingId && (
            <button type="button" className="btn btn-outline" onClick={resetForm}>
              Cancel Edit
            </button>
          )}
        </div>
      </form>

      <div className="card" style={{ padding: 20 }}>
        <h3>Catalogue Products ({filteredProducts.length})</h3>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              border: '1px solid #D9D9E0',
              borderRadius: '999px',
              padding: '4px 10px',
              flex: '1 1 180px',
            }}
          >
            <Search size={14} color="#5C4B6E" />
            <input
              value={listQuery}
              onChange={(e) => setListQuery(e.target.value)}
              placeholder="Search title or SKU..."
              style={{ border: 'none', outline: 'none', background: 'transparent', width: '100%' }}
            />
          </div>
          {(['ALL', 'ACTIVE', 'ARCHIVED'] as const).map((f) => (
            <button
              key={f}
              type="button"
              className={`chip ${listFilter === f ? 'active' : ''}`}
              onClick={() => setListFilter(f)}
            >
              {f === 'ALL' ? 'All' : f === 'ACTIVE' ? 'Active' : 'Archived'}
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {filteredProducts.map((p) => {
            const inv = inventoryByProduct[p.id];
            const stock = inv?.stockQuantity ?? 0;
            return (
              <div
                key={p.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: 10,
                  flexWrap: 'wrap',
                  paddingBottom: 10,
                  borderBottom: '1px solid #D9D9E0',
                }}
              >
                <div style={{ display: 'flex', gap: 10, alignItems: 'center', flex: '1 1 180px' }}>
                  <img
                    src={p.mainImageUrl || p.imageUrls[0] || '/assets/zeecrown-logo.jpg'}
                    alt=""
                    style={{ width: 44, height: 54, objectFit: 'cover', borderRadius: 8, border: '1px solid #D9D9E0' }}
                  />
                  <div>
                    <strong>{p.title}</strong>
                    <div style={{ fontSize: '0.82rem', color: '#5C4B6E' }}>
                      ₦{Math.round(p.priceNgn).toLocaleString()} &middot; SKU: {inv?.sku || p.sku || 'N/A'} &middot;{' '}
                      Stock: {stock} &middot;{' '}
                      <strong style={{ color: p.active ? '#1D6B45' : '#9E2A3B' }}>
                        {p.active ? 'Active' : 'Archived'}
                      </strong>
                    </div>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  <button type="button" className="zc-pill-btn" onClick={() => handleLoadProductForEdit(p)}>
                    Edit
                  </button>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    title={p.active ? 'Archive product' : 'Publish product'}
                    onClick={() => void handleToggleActive(p)}
                  >
                    {p.active ? <EyeOff size={14} /> : <Eye size={14} />}
                    {p.active ? 'Archive' : 'Publish'}
                  </button>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    title="Duplicate as draft"
                    onClick={() => void handleDuplicateProduct(p)}
                  >
                    <Copy size={14} />
                  </button>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    title="Delete product"
                    style={{ color: '#9E2A3B' }}
                    onClick={() => void handleDeleteProduct(p)}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
