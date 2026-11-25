import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  addDoc,
  collection,
  doc,
  getDocs,
  serverTimestamp,
  setDoc,
  updateDoc,
} from 'firebase/firestore';
import { getDownloadURL, ref, uploadBytes } from 'firebase/storage';
import { ShieldAlert, Sparkles, Upload } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { db, FUNCTIONS_BASE_URL, storage } from '../lib/firebase';
import { formatMoney, formatOrderStatus, normalizeSlug } from '../lib/formatters';
import { Product, StoreOrder, SupportEnquiry } from '../types/store';

export function AdminPage() {
  const {
    user,
    isAdmin,
    adminRole,
    allProducts,
    categories,
    deliveryZones,
    storeSettings,
    currency,
    refreshCatalog,
  } = useStore();

  const [tab, setTab] = useState<'overview' | 'products' | 'orders' | 'support' | 'categories'>(
    'overview'
  );
  const [adminOrders, setAdminOrders] = useState<StoreOrder[]>([]);
  const [adminEnquiries, setAdminEnquiries] = useState<SupportEnquiry[]>([]);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  // Product editor state
  const [editingId, setEditingId] = useState<string | null>(null);
  const [pTitle, setPTitle] = useState('');
  const [pDesc, setPDesc] = useState('');
  const [pCatId, setPCatId] = useState('');
  const [pPriceNgn, setPPriceNgn] = useState('');
  const [pPriceUsd, setPPriceUsd] = useState('');
  const [pStock, setPStock] = useState('10');
  const [pSizes, setPSizes] = useState('S, M, L, XL');
  const [pColors, setPColors] = useState('Royal Purple, Gold');
  const [pFeatures, setPFeatures] = useState('');
  const [pImageUrls, setPImageUrls] = useState<string[]>([]);
  const [pFeatured, setPFeatured] = useState(true);
  const [pActive, setPActive] = useState(true);
  const [aiBusy, setAiBusy] = useState(false);

  // Category creator
  const [catName, setCatName] = useState('');
  const [catDesc, setCatDesc] = useState('');

  // Support reply draft
  const [replyTextById, setReplyTextById] = useState<Record<string, string>>({});

  const loadAdminRecords = async () => {
    if (!isAdmin) return;
    try {
      const [ordSnap, supSnap] = await Promise.all([
        getDocs(collection(db, 'orders')),
        getDocs(collection(db, 'supportEnquiries')),
      ]);
      setAdminOrders(
        ordSnap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<StoreOrder, 'id'>) })).reverse()
      );
      setAdminEnquiries(
        supSnap.docs
          .map((d) => ({ id: d.id, ...(d.data() as Omit<SupportEnquiry, 'id'>) }))
          .reverse()
      );
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to load admin records.');
    }
  };

  useEffect(() => {
    void loadAdminRecords();
  }, [isAdmin]);

  useEffect(() => {
    if (categories.length > 0 && !pCatId) {
      setPCatId(categories[0].id);
    }
  }, [categories, pCatId]);

  if (!user || !isAdmin) {
    return (
      <div className="container section" style={{ maxWidth: 560 }}>
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <ShieldAlert size={40} color="#9E2A3B" style={{ marginBottom: 10 }} />
          <h2>Administrator Access Required</h2>
          <p style={{ color: '#5C4B6E' }}>
            Sign in with an authorized ZEECROWN STORE administrator account to access inventory,
            orders, payment verification, support inbox, and AI drafting tools.
          </p>
          <Link to="/login" state={{ from: '/admin' }} className="btn">
            Sign In as Administrator
          </Link>
        </div>
      </div>
    );
  }

  const totalRevenueNgn = adminOrders
    .filter((o) => o.paymentStatus === 'PAID')
    .reduce((sum, o) => sum + Number(o.totalNgn || 0), 0);

  const pendingPaymentsCount = adminOrders.filter(
    (o) => o.paymentStatus === 'PENDING_VERIFICATION' || o.paymentStatus === 'UNPAID'
  ).length;

  const openTicketsCount = adminEnquiries.filter(
    (e) => e.status === 'NEW' || e.status === 'OPEN' || e.status === 'WAITING_FOR_STAFF'
  ).length;

  const handleUploadImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setStatusMsg('Uploading product photo to Firebase Storage...');
    try {
      const targetProdId = editingId || `draft_${Date.now()}`;
      const cleanName = `${Date.now()}_${file.name.replace(/[^a-zA-Z0-9._-]/g, '_')}`;
      const storageRef = ref(storage, `products/${targetProdId}/${cleanName}`);
      await uploadBytes(storageRef, file, { contentType: file.type || 'image/jpeg' });
      const url = await getDownloadURL(storageRef);
      setPImageUrls((prev) => [...prev, url]);
      setStatusMsg('Photo uploaded cleanly.');
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Photo upload failed.');
    }
  };

  const handleAnalyzePhotosWithAi = async () => {
    if (pImageUrls.length === 0) {
      setStatusMsg('Upload or add at least one product photo URL before generating AI description.');
      return;
    }
    setAiBusy(true);
    setStatusMsg('Analyzing uploaded product photos...');
    try {
      const endpoint = FUNCTIONS_BASE_URL
        ? `${FUNCTIONS_BASE_URL}/analyzeProductPhotos`
        : '/api/ai/analyze-product';
      const token = await user.getIdToken();
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ imageUrls: pImageUrls }),
      });
      const contentType = res.headers.get('content-type') || '';
      if (!contentType.includes('application/json')) {
        throw new Error('AI photo analyzer endpoint is not configured on this host (set VITE_FUNCTIONS_BASE_URL).');
      }
      const data = (await res.json()) as {
        error?: string;
        suggestion?: {
          suggestedTitle?: string;
          description?: string;
          visibleFeatures?: string[];
          visibleColors?: string[];
          suggestedCategoryId?: string;
        };
      };
      if (!res.ok || !data.suggestion) {
        throw new Error(data.error || 'AI photo analyzer endpoint not configured yet.');
      }
      if (data.suggestion.suggestedTitle) setPTitle(data.suggestion.suggestedTitle);
      if (data.suggestion.description) setPDesc(data.suggestion.description);
      if (data.suggestion.visibleColors?.length) {
        setPColors(data.suggestion.visibleColors.join(', '));
      }
      if (data.suggestion.visibleFeatures?.length) {
        setPFeatures(data.suggestion.visibleFeatures.join('\n'));
      }
      if (data.suggestion.suggestedCategoryId) {
        setPCatId(data.suggestion.suggestedCategoryId);
      }
      setStatusMsg('AI draft populated! Please review price, stock, and details before saving.');
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to run AI product analyzer.');
    } finally {
      setAiBusy(false);
    }
  };

  const handleSaveProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    const ngn = Number(pPriceNgn);
    if (!pTitle.trim() || !pDesc.trim() || !pCatId || Number.isNaN(ngn) || ngn < 0) {
      setStatusMsg('Title, description, category, and valid NGN price are required.');
      return;
    }
    const usd = Number(pPriceUsd) > 0 ? Number(pPriceUsd) : Math.round((ngn / Math.max(storeSettings.usdRateNgn || 1600, 1)) * 100) / 100;
    const prodId = editingId || `prod_${Date.now()}`;
    const slug = normalizeSlug(pTitle) || prodId;
    const sizes = pSizes
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    const colors = pColors
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    const features = pFeatures
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean);

    setStatusMsg('Saving product...');
    try {
      const productPayload: Record<string, unknown> = {
        title: pTitle.trim().slice(0, 120),
        slug: slug.slice(0, 120),
        description: pDesc.trim().slice(0, 2000),
        categoryId: pCatId,
        priceNgn: ngn,
        priceUsd: usd,
        mainImageUrl: (pImageUrls[0] || '').slice(0, 600),
        imageUrls: pImageUrls.slice(0, 10),
        sizes,
        colors,
        features,
        featured: pFeatured,
        active: pActive,
        updatedAt: serverTimestamp(),
      };
      if (!editingId) {
        productPayload.newArrival = true;
        productPayload.createdAt = serverTimestamp();
      }
      await setDoc(doc(db, 'products', prodId), productPayload, { merge: Boolean(editingId) });

      const qty = Math.max(0, Math.round(Number(pStock) || 0));
      await setDoc(
        doc(db, 'inventory', prodId),
        {
          productId: prodId,
          sku: `ZC-${prodId.slice(-6).toUpperCase()}`,
          stockQuantity: qty,
          lowStockThreshold: 3,
          reservedQuantity: 0,
          inStock: qty > 0,
          updatedAt: serverTimestamp(),
        },
        { merge: true }
      );

      setEditingId(null);
      setPTitle('');
      setPDesc('');
      setPPriceNgn('');
      setPPriceUsd('');
      setPFeatures('');
      setPImageUrls([]);
      setStatusMsg('Product & inventory saved to Firestore!');
      await refreshCatalog();
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to save product.');
    }
  };

  const handleLoadProductForEdit = (p: Product) => {
    setEditingId(p.id);
    setPTitle(p.title);
    setPDesc(p.description);
    setPCatId(p.categoryId);
    setPPriceNgn(String(p.priceNgn));
    setPPriceUsd(String(p.priceUsd));
    setPSizes(p.sizes.join(', '));
    setPColors(p.colors.join(', '));
    setPFeatures((p.features || []).join('\n'));
    setPImageUrls(p.imageUrls.length > 0 ? p.imageUrls : p.mainImageUrl ? [p.mainImageUrl] : []);
    setPFeatured(p.featured);
    setPActive(p.active);
    setTab('products');
  };

  const handleUpdateOrder = async (order: StoreOrder, nextStatus: string, nextPaymentStatus?: string) => {
    try {
      const nextHistory = [
        ...(order.statusHistory || []),
        {
          stage: nextStatus,
          label: formatOrderStatus(nextStatus),
          note: `Updated via Admin Web Dashboard by ${user.email || 'Admin'}`,
          actorRole: 'ADMIN',
          timestampMillis: Date.now(),
        },
      ].slice(-30);

      await updateDoc(doc(db, 'orders', order.id), {
        status: nextStatus,
        ...(nextPaymentStatus ? { paymentStatus: nextPaymentStatus } : {}),
        statusHistory: nextHistory,
        updatedAt: serverTimestamp(),
      });

      if (order.trackingToken) {
        await updateDoc(doc(db, 'orderTrackingTokens', order.trackingToken), {
          status: nextStatus,
          ...(nextPaymentStatus ? { paymentStatus: nextPaymentStatus } : {}),
          statusHistory: nextHistory,
          updatedAt: serverTimestamp(),
        }).catch(() => undefined);
      }

      setStatusMsg(`Order ${order.orderNumber} updated to ${nextStatus}.`);
      await loadAdminRecords();
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to update order.');
    }
  };

  const handleSendAdminReply = async (enquiry: SupportEnquiry) => {
    const msg = (replyTextById[enquiry.id] || '').trim();
    if (!msg) return;
    try {
      await addDoc(collection(db, 'supportEnquiries', enquiry.id, 'replies'), {
        enquiryId: enquiry.id,
        ownerUid: enquiry.userId,
        senderUid: user.uid,
        senderName: user.displayName || user.email || 'ZEECROWN Admin',
        senderRole: 'ADMIN',
        message: msg.slice(0, 1800),
        createdAt: serverTimestamp(),
      });
      await updateDoc(doc(db, 'supportEnquiries', enquiry.id), {
        status: 'RESOLVED',
        lastReplyAt: serverTimestamp(),
        lastReplyPreview: msg.slice(0, 200),
        updatedAt: serverTimestamp(),
      });
      setReplyTextById((prev) => ({ ...prev, [enquiry.id]: '' }));
      setStatusMsg(`Reply sent for ticket "${enquiry.subject}".`);
      await loadAdminRecords();
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to send support reply.');
    }
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!catName.trim()) return;
    const catId = `cat_${normalizeSlug(catName) || Date.now()}`;
    try {
      await setDoc(doc(db, 'categories', catId), {
        name: catName.trim().slice(0, 80),
        slug: normalizeSlug(catName).slice(0, 80),
        description: catDesc.trim().slice(0, 400),
        sortOrder: categories.length + 1,
        active: true,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setCatName('');
      setCatDesc('');
      setStatusMsg('Category created!');
      await refreshCatalog();
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to create category.');
    }
  };

  return (
    <div className="container section">
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 12,
          marginBottom: 20,
        }}
      >
        <div>
          <h1 style={{ marginBottom: 4 }}>ZEECROWN Store Admin Dashboard</h1>
          <span style={{ color: '#5C4B6E', fontSize: '0.9rem' }}>
            Signed in as {user.email} &middot; Role: <strong>{adminRole}</strong>
          </span>
        </div>
      </div>

      {statusMsg && <div className="alert alert-info">{statusMsg}</div>}

      <div className="tabs">
        <button type="button" className={`chip ${tab === 'overview' ? 'active' : ''}`} onClick={() => setTab('overview')}>
          Overview
        </button>
        <button type="button" className={`chip ${tab === 'products' ? 'active' : ''}`} onClick={() => setTab('products')}>
          Products ({allProducts.length})
        </button>
        <button type="button" className={`chip ${tab === 'orders' ? 'active' : ''}`} onClick={() => setTab('orders')}>
          Orders ({adminOrders.length})
        </button>
        <button type="button" className={`chip ${tab === 'support' ? 'active' : ''}`} onClick={() => setTab('support')}>
          Support Inbox ({openTicketsCount})
        </button>
        <button type="button" className={`chip ${tab === 'categories' ? 'active' : ''}`} onClick={() => setTab('categories')}>
          Categories &amp; Zones
        </button>
      </div>

      {tab === 'overview' && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
            gap: 16,
          }}
        >
          <div className="card" style={{ padding: 20 }}>
            <span style={{ color: '#5C4B6E', fontSize: '0.84rem' }}>Verified Revenue</span>
            <h2 style={{ margin: '6px 0 0', color: '#542080' }}>
              {formatMoney(totalRevenueNgn, currency, undefined, storeSettings.usdRateNgn)}
            </h2>
          </div>
          <div className="card" style={{ padding: 20 }}>
            <span style={{ color: '#5C4B6E', fontSize: '0.84rem' }}>Total Orders</span>
            <h2 style={{ margin: '6px 0 0' }}>{adminOrders.length}</h2>
          </div>
          <div className="card" style={{ padding: 20 }}>
            <span style={{ color: '#5C4B6E', fontSize: '0.84rem' }}>Pending Payment Checks</span>
            <h2 style={{ margin: '6px 0 0', color: '#8A5A00' }}>{pendingPaymentsCount}</h2>
          </div>
          <div className="card" style={{ padding: 20 }}>
            <span style={{ color: '#5C4B6E', fontSize: '0.84rem' }}>Open Support Tickets</span>
            <h2 style={{ margin: '6px 0 0' }}>{openTicketsCount}</h2>
          </div>
        </div>
      )}

      {tab === 'products' && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))',
            gap: 24,
            alignItems: 'start',
          }}
        >
          <form onSubmit={(e) => void handleSaveProduct(e)} className="card" style={{ padding: 22 }}>
            <h3>{editingId ? 'Edit Product' : 'Add New Product'}</h3>

            <div style={{ marginBottom: 14, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <label className="zc-pill-btn" style={{ cursor: 'pointer' }}>
                <Upload size={15} /> Upload Photo
                <input
                  type="file"
                  accept="image/*"
                  hidden
                  onChange={(e) => void handleUploadImage(e)}
                />
              </label>
              <button
                type="button"
                className="zc-pill-btn primary"
                disabled={aiBusy}
                onClick={() => void handleAnalyzePhotosWithAi()}
              >
                <Sparkles size={15} /> {aiBusy ? 'Analyzing...' : 'Generate AI Draft from Photos'}
              </button>
            </div>

            {pImageUrls.length > 0 && (
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
                {pImageUrls.map((url) => (
                  <img
                    key={url}
                    src={url}
                    alt=""
                    style={{ width: 58, height: 70, objectFit: 'cover', borderRadius: 8, border: '1px solid #D9D9E0' }}
                  />
                ))}
              </div>
            )}

            <div className="field">
              <label>Product Title *</label>
              <input required value={pTitle} onChange={(e) => setPTitle(e.target.value)} />
            </div>
            <div className="field">
              <label>Category *</label>
              <select value={pCatId} onChange={(e) => setPCatId(e.target.value)}>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
              <div className="field">
                <label>Price (₦ NGN) *</label>
                <input
                  required
                  type="number"
                  min={0}
                  value={pPriceNgn}
                  onChange={(e) => setPPriceNgn(e.target.value)}
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
              <div className="field">
                <label>Stock Qty</label>
                <input
                  type="number"
                  min={0}
                  value={pStock}
                  onChange={(e) => setPStock(e.target.value)}
                />
              </div>
            </div>
            <div className="field">
              <label>Description *</label>
              <textarea required rows={4} value={pDesc} onChange={(e) => setPDesc(e.target.value)} />
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
            <div className="field">
              <label>Features (one per line)</label>
              <textarea rows={2} value={pFeatures} onChange={(e) => setPFeatures(e.target.value)} />
            </div>
            <div style={{ display: 'flex', gap: 16, marginBottom: 14 }}>
              <label>
                <input type="checkbox" checked={pFeatured} onChange={(e) => setPFeatured(e.target.checked)} />{' '}
                Featured
              </label>
              <label>
                <input type="checkbox" checked={pActive} onChange={(e) => setPActive(e.target.checked)} />{' '}
                Active in Shop
              </label>
            </div>
            <div style={{ display: 'flex', gap: 10 }}>
              <button type="submit" className="btn">
                {editingId ? 'Save Changes' : 'Publish Product'}
              </button>
              {editingId && (
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => {
                    setEditingId(null);
                    setPTitle('');
                    setPDesc('');
                    setPImageUrls([]);
                  }}
                >
                  Cancel Edit
                </button>
              )}
            </div>
          </form>

          <div className="card" style={{ padding: 20 }}>
            <h3>Published &amp; Draft Products</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {allProducts.map((p) => (
                <div
                  key={p.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    gap: 10,
                    paddingBottom: 10,
                    borderBottom: '1px solid #D9D9E0',
                  }}
                >
                  <div>
                    <strong>{p.title}</strong>
                    <div style={{ fontSize: '0.84rem', color: '#5C4B6E' }}>
                      ₦{Math.round(p.priceNgn).toLocaleString()} &middot;{' '}
                      {p.active ? 'Active' : 'Hidden'}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => handleLoadProductForEdit(p)}
                  >
                    Edit
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {tab === 'orders' && (
        <div className="card" style={{ padding: 20 }}>
          <h3>Orders &amp; Payment Verification</h3>
          <div className="table-wrap">
            <table className="zc-table">
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Customer / Destination</th>
                  <th>Total</th>
                  <th>Status / Payment</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {adminOrders.map((o) => (
                  <tr key={o.id}>
                    <td>
                      <strong>{o.orderNumber}</strong>
                      <div style={{ fontSize: '0.8rem', color: '#5C4B6E' }}>{o.itemsSummary}</div>
                    </td>
                    <td>
                      {o.recipientName || o.customerEmail || o.userId}
                      <div style={{ fontSize: '0.8rem', color: '#5C4B6E' }}>
                        {o.shippingCity}, {o.shippingState}
                      </div>
                    </td>
                    <td>{formatMoney(o.totalNgn, currency, undefined, storeSettings.usdRateNgn)}</td>
                    <td>
                      <div>{formatOrderStatus(o.status)}</div>
                      <div style={{ fontSize: '0.8rem', color: '#5C4B6E' }}>
                        {o.paymentMethod} &middot; {formatOrderStatus(o.paymentStatus)}
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        {o.paymentStatus !== 'PAID' && (
                          <button
                            type="button"
                            className="zc-pill-btn primary"
                            onClick={() => void handleUpdateOrder(o, 'CONFIRMED', 'PAID')}
                          >
                            Verify Payment
                          </button>
                        )}
                        <button
                          type="button"
                          className="zc-pill-btn"
                          onClick={() => void handleUpdateOrder(o, 'SHIPPED')}
                        >
                          Mark Shipped
                        </button>
                        <button
                          type="button"
                          className="zc-pill-btn"
                          onClick={() => void handleUpdateOrder(o, 'DELIVERED')}
                        >
                          Delivered
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'support' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          {adminEnquiries.length === 0 ? (
            <div className="card" style={{ padding: 24 }}>No support tickets yet.</div>
          ) : (
            adminEnquiries.map((q) => (
              <div key={q.id} className="card" style={{ padding: 18 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <strong>
                    {q.subject} ({q.customerName})
                  </strong>
                  <span className="tag-badge">{q.status}</span>
                </div>
                <p style={{ margin: '0 0 12px', color: '#5C4B6E' }}>{q.message}</p>
                <div style={{ display: 'flex', gap: 8 }}>
                  <input
                    value={replyTextById[q.id] || ''}
                    onChange={(e) =>
                      setReplyTextById((prev) => ({ ...prev, [q.id]: e.target.value }))
                    }
                    placeholder="Write official support reply..."
                    style={{
                      flex: 1,
                      padding: '8px 12px',
                      borderRadius: 10,
                      border: '1px solid #D9D9E0',
                    }}
                  />
                  <button
                    type="button"
                    className="btn"
                    style={{ padding: '8px 16px', minHeight: 38 }}
                    onClick={() => void handleSendAdminReply(q)}
                  >
                    Reply &amp; Resolve
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'categories' && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: 20,
          }}
        >
          <form onSubmit={(e) => void handleCreateCategory(e)} className="card" style={{ padding: 20 }}>
            <h3>Create Category</h3>
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
              <textarea
                rows={3}
                value={catDesc}
                onChange={(e) => setCatDesc(e.target.value)}
              />
            </div>
            <button type="submit" className="btn">
              Add Category
            </button>
          </form>

          <div className="card" style={{ padding: 20 }}>
            <h3>Active Delivery Zones ({deliveryZones.length})</h3>
            {deliveryZones.map((z) => (
              <div
                key={z.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  padding: '8px 0',
                  borderBottom: '1px solid #D9D9E0',
                }}
              >
                <span>
                  <strong>{z.name}</strong> ({z.estimatedDays})
                </span>
                <strong>₦{Math.round(z.feeNgn).toLocaleString()}</strong>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
