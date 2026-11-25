import React, { useEffect, useMemo, useState } from 'react';
import {
  collection,
  doc,
  getDocs,
  serverTimestamp,
  writeBatch,
} from 'firebase/firestore';
import { Search } from 'lucide-react';
import { db } from '../../lib/firebase';
import { buildVariantKey, findVariantStockValue } from '../../lib/variants';
import { InventoryItem, InventoryTransaction, Product } from '../../types/store';
import { AdminActor, recordAdminAudit } from './adminHelpers';

interface AdminInventoryTabProps {
  actor: AdminActor;
  allProducts: Product[];
  inventoryByProduct: Record<string, InventoryItem>;
  onRefreshCatalog: () => Promise<void>;
  onStatus: (msg: string) => void;
}

export function AdminInventoryTab({
  actor,
  allProducts,
  inventoryByProduct,
  onRefreshCatalog,
  onStatus,
}: AdminInventoryTabProps) {
  const [filter, setFilter] = useState<'ALL' | 'LOW_STOCK' | 'OUT_OF_STOCK'>('ALL');
  const [queryText, setQueryText] = useState('');
  const [selectedProdId, setSelectedProdId] = useState<string | null>(null);
  const [totalQtyInput, setTotalQtyInput] = useState('');
  const [thresholdInput, setThresholdInput] = useState('3');
  const [skuInput, setSkuInput] = useState('');
  const [reasonInput, setReasonInput] = useState('Restock delivery');
  const [variantStockDraft, setVariantStockDraft] = useState<Record<string, string>>({});
  const [transactions, setTransactions] = useState<InventoryTransaction[]>([]);
  const [saving, setSaving] = useState(false);

  const loadTransactions = async () => {
    try {
      const snap = await getDocs(collection(db, 'inventoryTransactions'));
      setTransactions(
        snap.docs
          .map((d) => ({ id: d.id, ...(d.data() as Omit<InventoryTransaction, 'id'>) }))
          .reverse()
          .slice(0, 30)
      );
    } catch {
      // ignore if empty or offline
    }
  };

  useEffect(() => {
    void loadTransactions();
  }, []);

  const filteredProducts = useMemo(() => {
    const q = queryText.trim().toLowerCase();
    return allProducts.filter((p) => {
      const rec = inventoryByProduct[p.id];
      const qty = rec?.stockQuantity ?? 0;
      const thresh = rec?.lowStockThreshold ?? p.lowStockThreshold ?? 3;
      if (filter === 'LOW_STOCK' && !(qty >= 1 && qty <= thresh)) return false;
      if (filter === 'OUT_OF_STOCK' && qty > 0) return false;
      if (!q) return true;
      const sku = (rec?.sku || p.sku || '').toLowerCase();
      return p.title.toLowerCase().includes(q) || sku.includes(q);
    });
  }, [allProducts, inventoryByProduct, filter, queryText]);

  const openAdjust = (p: Product) => {
    const rec = inventoryByProduct[p.id];
    setSelectedProdId(p.id);
    setTotalQtyInput(String(rec?.stockQuantity ?? 0));
    setThresholdInput(String(rec?.lowStockThreshold ?? p.lowStockThreshold ?? 3));
    setSkuInput(rec?.sku || p.sku || `ZC-${p.id.slice(-6).toUpperCase()}`);
    setReasonInput('Manual stock adjustment');

    const sizes = p.sizes.length > 0 ? p.sizes : ['Standard'];
    const colors = p.colors.length > 0 ? p.colors : [''];
    const nextDraft: Record<string, string> = {};
    for (const s of sizes) {
      for (const c of colors) {
        const key = buildVariantKey(s, c);
        const existingVal = findVariantStockValue(rec?.variantStock, key);
        nextDraft[key] = existingVal !== null ? String(existingVal) : '';
      }
    }
    if (rec?.variantStock) {
      Object.entries(rec.variantStock).forEach(([k, v]) => {
        if (!(k in nextDraft)) nextDraft[k] = String(v);
      });
    }
    setVariantStockDraft(nextDraft);
  };

  const selectedProduct = selectedProdId ? allProducts.find((p) => p.id === selectedProdId) || null : null;

  const handleSaveStock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProduct) return;
    setSaving(true);
    try {
      const rec = inventoryByProduct[selectedProduct.id];
      const prevStock = rec?.stockQuantity ?? 0;
      const variantStockMap: Record<string, number> = {};
      let hasVariantQuantities = false;
      let variantSum = 0;

      Object.entries(variantStockDraft).forEach(([k, rawVal]) => {
        if (rawVal.trim() !== '') {
          const parsed = Math.max(0, Math.min(100000, Math.round(Number(rawVal) || 0)));
          variantStockMap[k] = parsed;
          hasVariantQuantities = true;
          variantSum += parsed;
        }
      });

      const nextQty = hasVariantQuantities
        ? variantSum
        : Math.max(0, Math.min(100000, Math.round(Number(totalQtyInput) || 0)));
      const nextThreshold = Math.max(0, Math.min(10000, Math.round(Number(thresholdInput) || 3)));
      const resolvedSku = (skuInput.trim() || `ZC-${selectedProduct.id.slice(-6).toUpperCase()}`).slice(0, 60);
      const cleanReason = (reasonInput.trim() || 'Manual stock adjustment').slice(0, 200);

      const batch = writeBatch(db);
      const invRef = doc(db, 'inventory', selectedProduct.id);
      if (rec) {
        batch.update(invRef, {
          sku: resolvedSku,
          stockQuantity: nextQty,
          lowStockThreshold: nextThreshold,
          variantStock: variantStockMap,
          inStock: nextQty > 0,
          updatedAt: serverTimestamp(),
        });
      } else {
        batch.set(invRef, {
          productId: selectedProduct.id,
          sku: resolvedSku,
          stockQuantity: nextQty,
          lowStockThreshold: nextThreshold,
          reservedQuantity: 0,
          variantStock: variantStockMap,
          inStock: nextQty > 0,
          updatedAt: serverTimestamp(),
        });
      }

      const txRef = doc(collection(db, 'inventoryTransactions'));
      batch.set(txRef, {
        productId: selectedProduct.id,
        productTitle: selectedProduct.title.slice(0, 120),
        sku: resolvedSku,
        previousQty: prevStock,
        deltaQty: nextQty - prevStock,
        newQty: nextQty,
        reason: cleanReason,
        adminUid: actor.uid,
        adminEmail: actor.email || actor.uid,
        createdAt: serverTimestamp(),
      });

      await batch.commit();
      await recordAdminAudit(
        actor,
        'ADJUST_STOCK',
        'inventory',
        selectedProduct.id,
        `Adjusted '${selectedProduct.title}' stock ${prevStock} -> ${nextQty} (${cleanReason})`
      );
      setSelectedProdId(null);
      onStatus(`Updated stock for "${selectedProduct.title}" to ${nextQty}.`);
      await Promise.all([onRefreshCatalog(), loadTransactions()]);
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to save inventory adjustment.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="card" style={{ padding: 20 }}>
        <h3>Inventory &amp; Per-Variant Stock ({filteredProducts.length})</h3>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 14 }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              border: '1px solid #D9D9E0',
              borderRadius: '999px',
              padding: '4px 12px',
              flex: '1 1 220px',
            }}
          >
            <Search size={15} color="#5C4B6E" />
            <input
              value={queryText}
              onChange={(e) => setQueryText(e.target.value)}
              placeholder="Search SKU or product title..."
              style={{ border: 'none', outline: 'none', background: 'transparent', width: '100%' }}
            />
          </div>
          {(['ALL', 'LOW_STOCK', 'OUT_OF_STOCK'] as const).map((f) => (
            <button
              key={f}
              type="button"
              className={`chip ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
            >
              {f === 'ALL' ? 'All Stock' : f === 'LOW_STOCK' ? 'Low Stock' : 'Out of Stock'}
            </button>
          ))}
        </div>

        <div className="table-wrap">
          <table className="zc-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>SKU</th>
                <th>Total Stock</th>
                <th>Variant Stock Breakdown</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.map((p) => {
                const rec = inventoryByProduct[p.id];
                const qty = rec?.stockQuantity ?? 0;
                const thresh = rec?.lowStockThreshold ?? p.lowStockThreshold ?? 3;
                const variantEntries = Object.entries(rec?.variantStock || {});
                return (
                  <tr key={p.id}>
                    <td>
                      <strong>{p.title}</strong>
                      <div style={{ fontSize: '0.78rem', color: '#5C4B6E' }}>
                        {p.active ? 'Active' : 'Archived'} &middot; Alert ≤ {thresh}
                      </div>
                    </td>
                    <td>{rec?.sku || p.sku || 'N/A'}</td>
                    <td>
                      <strong style={{ color: qty <= 0 ? '#9E2A3B' : qty <= thresh ? '#8A5A00' : '#1D6B45' }}>
                        {qty <= 0 ? 'OUT OF STOCK (0)' : qty <= thresh ? `LOW (${qty})` : `${qty} in stock`}
                      </strong>
                    </td>
                    <td style={{ fontSize: '0.8rem', color: '#5C4B6E' }}>
                      {variantEntries.length === 0
                        ? 'Product-wide pool'
                        : variantEntries.map(([k, v]) => `${k}: ${v}`).join(' · ')}
                    </td>
                    <td>
                      <button type="button" className="zc-pill-btn primary" onClick={() => openAdjust(p)}>
                        Adjust Stock
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {selectedProduct && (
        <form onSubmit={(e) => void handleSaveStock(e)} className="card" style={{ padding: 20, border: '2px solid #542080' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3 style={{ margin: 0 }}>Adjust Stock &middot; {selectedProduct.title}</h3>
            <button type="button" className="zc-pill-btn" onClick={() => setSelectedProdId(null)}>
              Close
            </button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
            <div className="field">
              <label>Base SKU *</label>
              <input required value={skuInput} onChange={(e) => setSkuInput(e.target.value)} />
            </div>
            <div className="field">
              <label>Total Stock (if no variant stock)</label>
              <input type="number" min={0} value={totalQtyInput} onChange={(e) => setTotalQtyInput(e.target.value)} />
            </div>
            <div className="field">
              <label>Low-Stock Threshold</label>
              <input type="number" min={0} value={thresholdInput} onChange={(e) => setThresholdInput(e.target.value)} />
            </div>
            <div className="field">
              <label>Audit Reason *</label>
              <input required value={reasonInput} onChange={(e) => setReasonInput(e.target.value)} />
            </div>
          </div>

          {Object.keys(variantStockDraft).length > 0 && (
            <div style={{ marginBottom: 14 }}>
              <strong style={{ display: 'block', fontSize: '0.86rem', marginBottom: 8 }}>
                Per-Variant Stock Quantities (auto-sums Total Stock when filled)
              </strong>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 10 }}>
                {Object.entries(variantStockDraft).map(([key, val]) => (
                  <div key={key} className="field" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.78rem' }}>{key}</label>
                    <input
                      type="number"
                      min={0}
                      value={val}
                      placeholder="Shared pool"
                      onChange={(e) =>
                        setVariantStockDraft((prev) => ({ ...prev, [key]: e.target.value }))
                      }
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          <button type="submit" className="btn" disabled={saving}>
            {saving ? 'Saving Stock...' : 'Save Stock & Log Movement'}
          </button>
        </form>
      )}

      <div className="card" style={{ padding: 20 }}>
        <h3>Recent Stock Movement History ({transactions.length})</h3>
        {transactions.length === 0 ? (
          <p style={{ color: '#5C4B6E', margin: 0 }}>No stock adjustments logged yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="zc-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Change</th>
                  <th>Reason</th>
                  <th>Admin</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => (
                  <tr key={tx.id}>
                    <td><strong>{tx.productTitle}</strong></td>
                    <td>{tx.sku}</td>
                    <td>
                      {tx.previousQty} → {tx.newQty} ({tx.deltaQty >= 0 ? `+${tx.deltaQty}` : tx.deltaQty})
                    </td>
                    <td>{tx.reason}</td>
                    <td style={{ fontSize: '0.82rem', color: '#5C4B6E' }}>{tx.adminEmail}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
