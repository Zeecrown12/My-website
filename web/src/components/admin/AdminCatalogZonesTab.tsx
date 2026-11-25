import React, { useEffect, useState } from 'react';
import {
  deleteDoc,
  doc,
  serverTimestamp,
  setDoc,
  updateDoc,
} from 'firebase/firestore';
import { Trash2 } from 'lucide-react';
import { db } from '../../lib/firebase';
import { normalizeSlug } from '../../lib/formatters';
import { Category, DeliverySettings, DeliveryZone, Product } from '../../types/store';
import { AdminActor, recordAdminAudit } from './adminHelpers';

interface AdminCatalogZonesTabProps {
  actor: AdminActor;
  categories: Category[];
  products: Product[];
  deliveryZones: DeliveryZone[];
  deliverySettings: DeliverySettings;
  onRefreshCatalog: () => Promise<void>;
  onStatus: (msg: string) => void;
}

export function AdminCatalogZonesTab({
  actor,
  categories,
  products,
  deliveryZones,
  deliverySettings,
  onRefreshCatalog,
  onStatus,
}: AdminCatalogZonesTabProps) {
  // Category editor
  const [editingCatId, setEditingCatId] = useState<string | null>(null);
  const [catName, setCatName] = useState('');
  const [catDesc, setCatDesc] = useState('');
  const [catSortOrder, setCatSortOrder] = useState('1');
  const [catActive, setCatActive] = useState(true);

  // Zone editor
  const [editingZoneId, setEditingZoneId] = useState<string | null>(null);
  const [zoneName, setZoneName] = useState('');
  const [zoneCountry, setZoneCountry] = useState('Nigeria');
  const [zoneStates, setZoneStates] = useState('Lagos');
  const [zoneFeeNgn, setZoneFeeNgn] = useState('5000');
  const [zoneDays, setZoneDays] = useState('1–3 business days');
  const [zonePromoNote, setZonePromoNote] = useState('');
  const [zoneActive, setZoneActive] = useState(true);

  // Global delivery settings
  const [freeThresholdNgn, setFreeThresholdNgn] = useState(
    String(deliverySettings.freeDeliveryThresholdNgn ?? 150000)
  );
  const [pickupEnabled, setPickupEnabled] = useState(deliverySettings.pickupEnabled);
  const [pickupAddress, setPickupAddress] = useState(deliverySettings.pickupAddress || '');
  const [pickupHours, setPickupHours] = useState(deliverySettings.pickupHours || '');
  const [specialPromoNote, setSpecialPromoNote] = useState(deliverySettings.specialPromoNote || '');

  useEffect(() => {
    setFreeThresholdNgn(String(deliverySettings.freeDeliveryThresholdNgn ?? 150000));
    setPickupEnabled(deliverySettings.pickupEnabled);
    setPickupAddress(deliverySettings.pickupAddress || '');
    setPickupHours(deliverySettings.pickupHours || '');
    setSpecialPromoNote(deliverySettings.specialPromoNote || '');
  }, [deliverySettings]);

  const resetCatForm = () => {
    setEditingCatId(null);
    setCatName('');
    setCatDesc('');
    setCatSortOrder(String(categories.length + 1));
    setCatActive(true);
  };

  const handleSaveCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (catName.trim().length < 2) {
      onStatus('Category name must be at least 2 characters.');
      return;
    }
    const cleanName = catName.trim().slice(0, 80);
    const cleanSlug = normalizeSlug(cleanName).slice(0, 80) || `cat-${Date.now()}`;
    const catId = editingCatId || `cat_${cleanSlug.replace(/-/g, '_')}`.slice(0, 60);
    const sortOrder = Math.max(0, Math.min(1000, Math.round(Number(catSortOrder) || 0)));

    try {
      if (editingCatId) {
        await updateDoc(doc(db, 'categories', catId), {
          name: cleanName,
          slug: cleanSlug,
          description: catDesc.trim().slice(0, 400),
          sortOrder,
          active: catActive,
          updatedAt: serverTimestamp(),
        });
      } else {
        await setDoc(doc(db, 'categories', catId), {
          name: cleanName,
          slug: cleanSlug,
          description: catDesc.trim().slice(0, 400),
          sortOrder,
          active: catActive,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
      await recordAdminAudit(
        actor,
        editingCatId ? 'UPDATE_CATEGORY' : 'CREATE_CATEGORY',
        'category',
        catId,
        `${editingCatId ? 'Updated' : 'Created'} category '${cleanName}'`
      );
      resetCatForm();
      onStatus(`Category "${cleanName}" saved.`);
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to save category.');
    }
  };

  const handleDeleteCategory = async (cat: Category) => {
    const linked = products.filter((p) => p.categoryId === cat.id).length;
    if (linked > 0) {
      onStatus(
        `Cannot delete "${cat.name}": ${linked} product(s) still belong to this category. Archive it or reassign products first.`
      );
      return;
    }
    if (!window.confirm(`Delete category "${cat.name}"?`)) return;
    try {
      await deleteDoc(doc(db, 'categories', cat.id));
      await recordAdminAudit(actor, 'DELETE_CATEGORY', 'category', cat.id, `Deleted category '${cat.name}'`);
      if (editingCatId === cat.id) resetCatForm();
      onStatus(`Deleted category "${cat.name}".`);
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to delete category.');
    }
  };

  const resetZoneForm = () => {
    setEditingZoneId(null);
    setZoneName('');
    setZoneCountry('Nigeria');
    setZoneStates('');
    setZoneFeeNgn('5000');
    setZoneDays('2–5 business days');
    setZonePromoNote('');
    setZoneActive(true);
  };

  const handleSaveZone = async (e: React.FormEvent) => {
    e.preventDefault();
    if (zoneName.trim().length < 2) {
      onStatus('Delivery zone name must be at least 2 characters.');
      return;
    }
    const zoneId = editingZoneId || `zone_${normalizeSlug(zoneName).replace(/-/g, '_') || Date.now()}`.slice(0, 60);
    const statesList = zoneStates
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
      .slice(0, 40);
    const fee = Math.max(0, Number(zoneFeeNgn) || 0);

    try {
      const payload = {
        name: zoneName.trim().slice(0, 80),
        country: (zoneCountry.trim() || 'Nigeria').slice(0, 80),
        states: statesList,
        feeNgn: fee,
        estimatedDays: (zoneDays.trim() || '2–5 business days').slice(0, 40),
        ...(zonePromoNote.trim() ? { promotionNote: zonePromoNote.trim().slice(0, 200) } : {}),
        active: zoneActive,
        updatedAt: serverTimestamp(),
      };
      await setDoc(doc(db, 'deliveryZones', zoneId), payload, { merge: Boolean(editingZoneId) });
      await recordAdminAudit(
        actor,
        editingZoneId ? 'UPDATE_DELIVERY_ZONE' : 'CREATE_DELIVERY_ZONE',
        'deliveryZone',
        zoneId,
        `${editingZoneId ? 'Updated' : 'Created'} delivery zone '${zoneName.trim()}' (₦${fee})`
      );
      resetZoneForm();
      onStatus(`Delivery zone "${zoneName.trim()}" saved.`);
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to save delivery zone.');
    }
  };

  const handleDeleteZone = async (z: DeliveryZone) => {
    if (!window.confirm(`Delete delivery zone "${z.name}"?`)) return;
    try {
      await deleteDoc(doc(db, 'deliveryZones', z.id));
      await recordAdminAudit(actor, 'DELETE_DELIVERY_ZONE', 'deliveryZone', z.id, `Deleted zone '${z.name}'`);
      if (editingZoneId === z.id) resetZoneForm();
      onStatus(`Deleted delivery zone "${z.name}".`);
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to delete delivery zone.');
    }
  };

  const handleSaveDeliverySettings = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await setDoc(
        doc(db, 'deliverySettings', 'public'),
        {
          freeDeliveryThresholdNgn: Math.max(0, Number(freeThresholdNgn) || 0),
          pickupEnabled,
          pickupAddress: pickupAddress.trim().slice(0, 240),
          pickupHours: pickupHours.trim().slice(0, 120),
          specialPromoNote: specialPromoNote.trim().slice(0, 200),
          visibility: 'public',
          updatedAt: serverTimestamp(),
        },
        { merge: true }
      );
      await recordAdminAudit(
        actor,
        'UPDATE_DELIVERY_SETTINGS',
        'deliverySettings',
        'public',
        'Updated global delivery threshold and pickup settings'
      );
      onStatus('Global delivery settings updated.');
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to save delivery settings.');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: 20,
          alignItems: 'start',
        }}
      >
        <form onSubmit={(e) => void handleSaveCategory(e)} className="card" style={{ padding: 20 }}>
          <h3>{editingCatId ? 'Edit Category' : 'Create Category'}</h3>
          <div className="field">
            <label>Category Name *</label>
            <input
              required
              value={catName}
              onChange={(e) => setCatName(e.target.value)}
              placeholder="e.g. Luxury Abayas"
            />
          </div>
          <div className="field">
            <label>Description</label>
            <textarea rows={2} value={catDesc} onChange={(e) => setCatDesc(e.target.value)} />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div className="field">
              <label>Sort Order</label>
              <input
                type="number"
                min={0}
                value={catSortOrder}
                onChange={(e) => setCatSortOrder(e.target.value)}
              />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', paddingTop: 18 }}>
              <label>
                <input type="checkbox" checked={catActive} onChange={(e) => setCatActive(e.target.checked)} />{' '}
                Active in Shop
              </label>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" className="btn">
              {editingCatId ? 'Save Category' : 'Add Category'}
            </button>
            {editingCatId && (
              <button type="button" className="btn btn-outline" onClick={resetCatForm}>
                Cancel
              </button>
            )}
          </div>
        </form>

        <div className="card" style={{ padding: 20 }}>
          <h3>Categories ({categories.length})</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {categories.map((cat) => (
              <div
                key={cat.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: 8,
                  paddingBottom: 8,
                  borderBottom: '1px solid #D9D9E0',
                }}
              >
                <div>
                  <strong>{cat.name}</strong>
                  <div style={{ fontSize: '0.8rem', color: '#5C4B6E' }}>
                    {cat.active ? 'Active' : 'Archived'} &middot;{' '}
                    {products.filter((p) => p.categoryId === cat.id).length} product(s)
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => {
                      setEditingCatId(cat.id);
                      setCatName(cat.name);
                      setCatDesc(cat.description);
                      setCatSortOrder(String(cat.sortOrder));
                      setCatActive(cat.active);
                    }}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    style={{ color: '#9E2A3B' }}
                    onClick={() => void handleDeleteCategory(cat)}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: 20,
          alignItems: 'start',
        }}
      >
        <form onSubmit={(e) => void handleSaveZone(e)} className="card" style={{ padding: 20 }}>
          <h3>{editingZoneId ? 'Edit Delivery Zone' : 'Add Delivery Zone'}</h3>
          <div className="field">
            <label>Zone Name *</label>
            <input
              required
              value={zoneName}
              onChange={(e) => setZoneName(e.target.value)}
              placeholder="e.g. Lagos Island Express"
            />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div className="field">
              <label>Country *</label>
              <input required value={zoneCountry} onChange={(e) => setZoneCountry(e.target.value)} />
            </div>
            <div className="field">
              <label>Delivery Fee (₦ NGN) *</label>
              <input
                required
                type="number"
                min={0}
                value={zoneFeeNgn}
                onChange={(e) => setZoneFeeNgn(e.target.value)}
              />
            </div>
          </div>
          <div className="field">
            <label>Covered States / Regions (comma-separated)</label>
            <input
              value={zoneStates}
              onChange={(e) => setZoneStates(e.target.value)}
              placeholder="Lagos, Ogun"
            />
          </div>
          <div className="field">
            <label>Estimated Delivery Days *</label>
            <input required value={zoneDays} onChange={(e) => setZoneDays(e.target.value)} />
          </div>
          <div className="field">
            <label>Promotion / Courier Note (Optional)</label>
            <input value={zonePromoNote} onChange={(e) => setZonePromoNote(e.target.value)} />
          </div>
          <label style={{ display: 'block', marginBottom: 14 }}>
            <input type="checkbox" checked={zoneActive} onChange={(e) => setZoneActive(e.target.checked)} />{' '}
            Active at Checkout
          </label>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" className="btn">
              {editingZoneId ? 'Save Zone' : 'Add Zone'}
            </button>
            {editingZoneId && (
              <button type="button" className="btn btn-outline" onClick={resetZoneForm}>
                Cancel
              </button>
            )}
          </div>
        </form>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div className="card" style={{ padding: 20 }}>
            <h3>Delivery Zones ({deliveryZones.length})</h3>
            {deliveryZones.map((z) => (
              <div
                key={z.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: 8,
                  padding: '8px 0',
                  borderBottom: '1px solid #D9D9E0',
                }}
              >
                <div>
                  <strong>{z.name}</strong> ({z.estimatedDays})
                  <div style={{ fontSize: '0.8rem', color: '#5C4B6E' }}>
                    ₦{Math.round(z.feeNgn).toLocaleString()} &middot; {z.active ? 'Active' : 'Inactive'}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => {
                      setEditingZoneId(z.id);
                      setZoneName(z.name);
                      setZoneCountry(z.country || 'Nigeria');
                      setZoneStates((z.states || []).join(', ') || z.region);
                      setZoneFeeNgn(String(z.feeNgn));
                      setZoneDays(z.estimatedDays);
                      setZonePromoNote(z.promotionNote || '');
                      setZoneActive(z.active);
                    }}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    style={{ color: '#9E2A3B' }}
                    onClick={() => void handleDeleteZone(z)}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <form onSubmit={(e) => void handleSaveDeliverySettings(e)} className="card" style={{ padding: 20 }}>
            <h3>Global Delivery &amp; Pickup Settings</h3>
            <div className="field">
              <label>Free Delivery Threshold (₦ NGN, 0 disables)</label>
              <input
                type="number"
                min={0}
                value={freeThresholdNgn}
                onChange={(e) => setFreeThresholdNgn(e.target.value)}
              />
            </div>
            <label style={{ display: 'block', marginBottom: 10 }}>
              <input
                type="checkbox"
                checked={pickupEnabled}
                onChange={(e) => setPickupEnabled(e.target.checked)}
              />{' '}
              Store Pickup Enabled
            </label>
            <div className="field">
              <label>Pickup Address</label>
              <input value={pickupAddress} onChange={(e) => setPickupAddress(e.target.value)} />
            </div>
            <div className="field">
              <label>Pickup Hours</label>
              <input value={pickupHours} onChange={(e) => setPickupHours(e.target.value)} />
            </div>
            <div className="field">
              <label>Delivery Promo Banner Note</label>
              <input value={specialPromoNote} onChange={(e) => setSpecialPromoNote(e.target.value)} />
            </div>
            <button type="submit" className="btn">
              Save Delivery Settings
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
