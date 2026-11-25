import { useEffect, useMemo, useState } from 'react';
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
import { ShieldAlert } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { db } from '../lib/firebase';
import { formatMoney } from '../lib/formatters';
import {
  CustomerSummary,
  PaymentRecord,
  StoreOrder,
  SupportEnquiry,
} from '../types/store';
import { AdminActor } from '../components/admin/adminHelpers';
import { AdminProductsTab } from '../components/admin/AdminProductsTab';
import { AdminInventoryTab } from '../components/admin/AdminInventoryTab';
import { AdminOrdersTab } from '../components/admin/AdminOrdersTab';
import { AdminCustomersTab } from '../components/admin/AdminCustomersTab';
import { AdminCatalogZonesTab } from '../components/admin/AdminCatalogZonesTab';
import { AdminSettingsTab } from '../components/admin/AdminSettingsTab';
import { activateFoundationStore } from '../components/admin/webStoreSeeder';

type AdminTabKey =
  | 'overview'
  | 'products'
  | 'inventory'
  | 'orders'
  | 'customers'
  | 'support'
  | 'categories'
  | 'settings';

export function AdminPage() {
  const {
    user,
    authLoading,
    isAdmin,
    adminRole,
    allProducts,
    allCategories,
    collections,
    sizeGuides,
    inventoryByProduct,
    allDeliveryZones,
    deliverySettings,
    storeSettings,
    currency,
    refreshCatalog,
  } = useStore();

  const [tab, setTab] = useState<AdminTabKey>('overview');
  const [adminOrders, setAdminOrders] = useState<StoreOrder[]>([]);
  const [adminPayments, setAdminPayments] = useState<PaymentRecord[]>([]);
  const [adminEnquiries, setAdminEnquiries] = useState<SupportEnquiry[]>([]);
  const [adminCustomers, setAdminCustomers] = useState<CustomerSummary[]>([]);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const [seeding, setSeeding] = useState(false);
  const [replyTextById, setReplyTextById] = useState<Record<string, string>>({});

  const loadAdminRecords = async () => {
    if (!isAdmin) return;
    try {
      const [ordSnap, paySnap, supSnap, custSnap] = await Promise.all([
        getDocs(collection(db, 'orders')),
        getDocs(collection(db, 'payments')).catch(() => null),
        getDocs(collection(db, 'supportEnquiries')).catch(() => null),
        getDocs(collection(db, 'users')).catch(() => null),
      ]);
      setAdminOrders(
        ordSnap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<StoreOrder, 'id'>) })).reverse()
      );
      if (paySnap) {
        setAdminPayments(
          paySnap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<PaymentRecord, 'id'>) })).reverse()
        );
      }
      if (supSnap) {
        setAdminEnquiries(
          supSnap.docs
            .map((d) => ({ id: d.id, ...(d.data() as Omit<SupportEnquiry, 'id'>) }))
            .reverse()
        );
      }
      if (custSnap) {
        setAdminCustomers(
          custSnap.docs
            .map((d) => ({ id: d.id, ...(d.data() as Omit<CustomerSummary, 'id'>) }))
            .reverse()
        );
      }
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to load admin records.');
    }
  };

  useEffect(() => {
    void loadAdminRecords();
  }, [isAdmin]);

  const paymentsByOrderId = useMemo(() => {
    const map: Record<string, PaymentRecord> = {};
    adminPayments.forEach((p) => {
      if (p.orderId && !map[p.orderId]) {
        map[p.orderId] = p;
      }
    });
    return map;
  }, [adminPayments]);

  const lowStockCount = useMemo(
    () =>
      allProducts.filter((p) => {
        const rec = inventoryByProduct[p.id];
        const qty = rec?.stockQuantity ?? 0;
        const thresh = rec?.lowStockThreshold ?? p.lowStockThreshold ?? 3;
        return qty <= thresh;
      }).length,
    [allProducts, inventoryByProduct]
  );

  if (authLoading) {
    return (
      <div className="container section" style={{ maxWidth: 560 }}>
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <h2>Verifying Administrator Session...</h2>
          <p style={{ color: '#5C4B6E', marginBottom: 0 }}>
            Checking your Firebase Authentication and administrator permissions.
          </p>
        </div>
      </div>
    );
  }

  if (!user || !isAdmin) {
    return (
      <div className="container section" style={{ maxWidth: 560 }}>
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <ShieldAlert size={40} color="#9E2A3B" style={{ marginBottom: 10 }} />
          <h2>Administrator Access Required</h2>
          <p style={{ color: '#5C4B6E' }}>
            Sign in with an authorized ZEECROWN STORE administrator account to access inventory,
            products, variants, orders, payment verification, customers, and support inbox.
          </p>
          <Link to="/login" state={{ from: '/admin' }} className="btn">
            Sign In as Administrator
          </Link>
        </div>
      </div>
    );
  }

  const actor: AdminActor = {
    uid: user.uid,
    email: user.email || user.uid,
    role: adminRole,
  };

  const totalRevenueNgn = adminOrders
    .filter((o) => o.paymentStatus === 'PAID')
    .reduce((sum, o) => sum + Number(o.totalNgn || 0), 0);

  const pendingPaymentsCount = adminOrders.filter(
    (o) => o.paymentStatus === 'PENDING_VERIFICATION' || o.paymentStatus === 'UNPAID'
  ).length;

  const openTicketsCount = adminEnquiries.filter(
    (e) => e.status === 'NEW' || e.status === 'OPEN' || e.status === 'WAITING_FOR_STAFF'
  ).length;

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
      if (enquiry.userId && !enquiry.userId.startsWith('web_guest_')) {
        await setDoc(
          doc(db, 'notifications', `notif_support_${enquiry.id}_${Date.now()}`),
          {
            userId: enquiry.userId,
            title: `Support Reply: ${enquiry.subject}`.slice(0, 115),
            body: msg.slice(0, 380),
            category: 'SUPPORT',
            readFlag: false,
            targetRoute: 'support',
            createdAt: serverTimestamp(),
          }
        ).catch(() => undefined);
      }
      setReplyTextById((prev) => ({ ...prev, [enquiry.id]: '' }));
      setStatusMsg(`Reply sent for ticket "${enquiry.subject}".`);
      await loadAdminRecords();
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to send support reply.');
    }
  };

  const handleActivateStore = async () => {
    setSeeding(true);
    setStatusMsg(null);
    try {
      const res = await activateFoundationStore(actor);
      await Promise.all([refreshCatalog(true), loadAdminRecords()]);
      setStatusMsg(
        res.createdCount > 0
          ? `Store activated! Created ${res.createdCount} Firestore records (categories, size guides, collections, delivery zones, store settings, and starter products). Preserved ${res.skippedCount} existing records.`
          : `All ${res.skippedCount} starter store records are already active in Firestore.`
      );
    } catch (err) {
      setStatusMsg(err instanceof Error ? err.message : 'Unable to activate starter store.');
    } finally {
      setSeeding(false);
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
        <button
          type="button"
          className="zc-pill-btn"
          onClick={() => void Promise.all([refreshCatalog(true), loadAdminRecords()])}
        >
          Refresh Live Data
        </button>
      </div>

      {statusMsg && <div className="alert alert-info">{statusMsg}</div>}

      <div className="tabs">
        <button type="button" className={`chip ${tab === 'overview' ? 'active' : ''}`} onClick={() => setTab('overview')}>
          Overview
        </button>
        <button type="button" className={`chip ${tab === 'products' ? 'active' : ''}`} onClick={() => setTab('products')}>
          Products ({allProducts.length})
        </button>
        <button type="button" className={`chip ${tab === 'inventory' ? 'active' : ''}`} onClick={() => setTab('inventory')}>
          Inventory ({lowStockCount} Low)
        </button>
        <button type="button" className={`chip ${tab === 'orders' ? 'active' : ''}`} onClick={() => setTab('orders')}>
          Orders &amp; Payments ({adminOrders.length})
        </button>
        <button type="button" className={`chip ${tab === 'customers' ? 'active' : ''}`} onClick={() => setTab('customers')}>
          Customers ({adminCustomers.length})
        </button>
        <button type="button" className={`chip ${tab === 'support' ? 'active' : ''}`} onClick={() => setTab('support')}>
          Support Inbox ({openTicketsCount})
        </button>
        <button type="button" className={`chip ${tab === 'categories' ? 'active' : ''}`} onClick={() => setTab('categories')}>
          Categories &amp; Delivery
        </button>
        <button type="button" className={`chip ${tab === 'settings' ? 'active' : ''}`} onClick={() => setTab('settings')}>
          Settings &amp; Payments
        </button>
      </div>

      {tab === 'overview' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div className="card" style={{ padding: 20, border: '1px solid #D9C9EA', background: '#FAF7FD' }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: 12,
              }}
            >
              <div>
                <h3 style={{ margin: '0 0 4px', color: '#351052' }}>
                  Store Backend &amp; Starter Database Activation (Free Spark Plan)
                </h3>
                <p style={{ margin: 0, color: '#5C4B6E', fontSize: '0.88rem' }}>
                  Live Status: <strong>{allCategories.length}</strong> categories &middot;{' '}
                  <strong>{allProducts.length}</strong> products &middot;{' '}
                  <strong>{allDeliveryZones.length}</strong> delivery zones. Clicking below writes any
                  missing starter records without overwriting existing items.
                </p>
              </div>
              <button
                type="button"
                className="btn"
                disabled={seeding}
                onClick={() => void handleActivateStore()}
              >
                {seeding ? 'Activating Store...' : 'Activate Starter Store & Database'}
              </button>
            </div>
          </div>

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
              <span style={{ color: '#5C4B6E', fontSize: '0.84rem' }}>Low / Out-of-Stock Products</span>
              <h2 style={{ margin: '6px 0 0', color: lowStockCount > 0 ? '#9E2A3B' : '#1D6B45' }}>
                {lowStockCount}
              </h2>
            </div>
            <div className="card" style={{ padding: 20 }}>
              <span style={{ color: '#5C4B6E', fontSize: '0.84rem' }}>Open Support Tickets</span>
              <h2 style={{ margin: '6px 0 0' }}>{openTicketsCount}</h2>
            </div>
          </div>
        </div>
      )}

      {tab === 'products' && (
        <AdminProductsTab
          actor={actor}
          allProducts={allProducts}
          categories={allCategories}
          collections={collections}
          sizeGuides={sizeGuides}
          inventoryByProduct={inventoryByProduct}
          storeSettings={storeSettings}
          onRefreshCatalog={() => refreshCatalog(true)}
          onStatus={setStatusMsg}
        />
      )}

      {tab === 'inventory' && (
        <AdminInventoryTab
          actor={actor}
          allProducts={allProducts}
          inventoryByProduct={inventoryByProduct}
          onRefreshCatalog={() => refreshCatalog(true)}
          onStatus={setStatusMsg}
        />
      )}

      {tab === 'orders' && (
        <AdminOrdersTab
          actor={actor}
          orders={adminOrders}
          paymentsByOrderId={paymentsByOrderId}
          currency={currency}
          storeSettings={storeSettings}
          onReload={loadAdminRecords}
          onStatus={setStatusMsg}
        />
      )}

      {tab === 'customers' && (
        <AdminCustomersTab
          actor={actor}
          customers={adminCustomers}
          orders={adminOrders}
          currency={currency}
          storeSettings={storeSettings}
          onReload={loadAdminRecords}
          onStatus={setStatusMsg}
        />
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
        <AdminCatalogZonesTab
          actor={actor}
          categories={allCategories}
          products={allProducts}
          deliveryZones={allDeliveryZones}
          deliverySettings={deliverySettings}
          onRefreshCatalog={() => refreshCatalog(true)}
          onStatus={setStatusMsg}
        />
      )}

      {tab === 'settings' && (
        <AdminSettingsTab
          actor={actor}
          storeSettings={storeSettings}
          onRefreshCatalog={() => refreshCatalog(true)}
          onStatus={setStatusMsg}
        />
      )}
    </div>
  );
}
