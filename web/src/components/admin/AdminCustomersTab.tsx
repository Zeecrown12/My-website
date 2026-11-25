import React, { useMemo, useState } from 'react';
import {
  collection,
  doc,
  getDoc,
  getDocs,
  serverTimestamp,
  updateDoc,
} from 'firebase/firestore';
import { Search } from 'lucide-react';
import { db } from '../../lib/firebase';
import { buildWhatsAppLink, formatMoney, formatOrderStatus } from '../../lib/formatters';
import {
  CurrencyCode,
  CustomerPrivateInfo,
  CustomerSummary,
  DeliveryAddress,
  StoreOrder,
  StoreSettings,
} from '../../types/store';
import { AdminActor, recordAdminAudit } from './adminHelpers';

interface AdminCustomersTabProps {
  actor: AdminActor;
  customers: CustomerSummary[];
  orders: StoreOrder[];
  currency: CurrencyCode;
  storeSettings: StoreSettings;
  onReload: () => Promise<void>;
  onStatus: (msg: string) => void;
}

export function AdminCustomersTab({
  actor,
  customers,
  orders,
  currency,
  storeSettings,
  onReload,
  onStatus,
}: AdminCustomersTabProps) {
  const [queryText, setQueryText] = useState('');
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerSummary | null>(null);
  const [privateInfo, setPrivateInfo] = useState<CustomerPrivateInfo | null>(null);
  const [savedAddresses, setSavedAddresses] = useState<DeliveryAddress[]>([]);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [notesDraft, setNotesDraft] = useState('');
  const [savingNotes, setSavingNotes] = useState(false);

  const ordersByUid = useMemo(() => {
    const map: Record<string, StoreOrder[]> = {};
    orders.forEach((o) => {
      if (!o.userId) return;
      if (!map[o.userId]) map[o.userId] = [];
      map[o.userId].push(o);
    });
    return map;
  }, [orders]);

  const filteredCustomers = useMemo(() => {
    const q = queryText.trim().toLowerCase();
    if (!q) return customers;
    return customers.filter((c) => {
      const custOrders = ordersByUid[c.userId || c.id] || [];
      const orderText = custOrders
        .map((o) => `${o.customerEmail || ''} ${o.recipientPhone || ''} ${o.shippingCity}`)
        .join(' ')
        .toLowerCase();
      return (
        (c.displayName || '').toLowerCase().includes(q) ||
        (c.userId || c.id).toLowerCase().includes(q) ||
        orderText.includes(q)
      );
    });
  }, [customers, ordersByUid, queryText]);

  const openCustomerDetail = async (c: CustomerSummary) => {
    const uid = c.userId || c.id;
    setSelectedCustomer(c);
    setNotesDraft(c.supportNotes || '');
    setPrivateInfo(null);
    setSavedAddresses([]);
    setLoadingDetail(true);
    try {
      const [privSnap, addrSnap] = await Promise.all([
        getDoc(doc(db, 'users', uid, 'private', 'info')).catch(() => null),
        getDocs(collection(db, 'users', uid, 'addresses')).catch(() => null),
      ]);
      if (privSnap?.exists()) {
        setPrivateInfo(privSnap.data() as CustomerPrivateInfo);
      }
      if (addrSnap) {
        setSavedAddresses(
          addrSnap.docs.map((d) => ({
            id: d.id,
            ...(d.data() as Omit<DeliveryAddress, 'id'>),
          }))
        );
      }
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleSaveSupportNotes = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCustomer) return;
    setSavingNotes(true);
    try {
      const uid = selectedCustomer.id;
      await updateDoc(doc(db, 'users', uid), {
        supportNotes: notesDraft.trim().slice(0, 1000),
        updatedAt: serverTimestamp(),
      });
      await recordAdminAudit(
        actor,
        'UPDATE_CUSTOMER_NOTES',
        'user',
        uid,
        `Updated CRM notes for ${selectedCustomer.displayName || uid}`
      );
      onStatus(`Saved CRM notes for ${selectedCustomer.displayName || uid}.`);
      await onReload();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to save customer notes.');
    } finally {
      setSavingNotes(false);
    }
  };

  const selectedOrders = selectedCustomer ? ordersByUid[selectedCustomer.userId || selectedCustomer.id] || [] : [];
  const selectedPhone = privateInfo?.phone || selectedOrders[0]?.recipientPhone || '';
  const selectedEmail = privateInfo?.email || selectedOrders[0]?.customerEmail || '';

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: selectedCustomer ? 'repeat(auto-fit, minmax(320px, 1fr))' : '1fr',
        gap: 20,
        alignItems: 'start',
      }}
    >
      <div className="card" style={{ padding: 20 }}>
        <h3>Registered Customers &amp; Delivery Profiles ({filteredCustomers.length})</h3>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            border: '1px solid #D9D9E0',
            borderRadius: '999px',
            padding: '5px 12px',
            marginBottom: 14,
            maxWidth: 360,
          }}
        >
          <Search size={15} color="#5C4B6E" />
          <input
            value={queryText}
            onChange={(e) => setQueryText(e.target.value)}
            placeholder="Search name, UID, email, phone, city..."
            style={{ border: 'none', outline: 'none', background: 'transparent', width: '100%' }}
          />
        </div>

        {filteredCustomers.length === 0 ? (
          <p style={{ color: '#5C4B6E', margin: 0 }}>No matching customer profiles loaded yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="zc-table">
              <thead>
                <tr>
                  <th>Customer</th>
                  <th>Orders / Spend</th>
                  <th>Currency</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {filteredCustomers.map((c) => {
                  const uid = c.userId || c.id;
                  const custOrders = ordersByUid[uid] || [];
                  const totalSpent = custOrders
                    .filter((o) => o.paymentStatus === 'PAID')
                    .reduce((s, o) => s + Number(o.totalNgn || 0), 0);
                  return (
                    <tr key={c.id}>
                      <td>
                        <strong>{c.displayName || 'ZEECROWN Client'}</strong>
                        <div style={{ fontSize: '0.78rem', color: '#5C4B6E' }}>
                          {c.role || 'customer'} &middot; {c.emailVerified ? 'Verified' : 'Unverified'}
                        </div>
                      </td>
                      <td>
                        <strong>{custOrders.length}</strong> order(s)
                        <div style={{ fontSize: '0.78rem', color: '#5C4B6E' }}>
                          Paid: {formatMoney(totalSpent, currency, undefined, storeSettings.usdRateNgn)}
                        </div>
                      </td>
                      <td>{c.preferredCurrency || 'NGN'}</td>
                      <td>
                        <button
                          type="button"
                          className="zc-pill-btn primary"
                          onClick={() => void openCustomerDetail(c)}
                        >
                          View Profile &amp; Addresses
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedCustomer && (
        <div className="card" style={{ padding: 20, border: '2px solid #542080' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
            <h3 style={{ margin: 0 }}>{selectedCustomer.displayName || 'Customer Profile'}</h3>
            <button type="button" className="zc-pill-btn" onClick={() => setSelectedCustomer(null)}>
              Close
            </button>
          </div>

          {loadingDetail ? (
            <p style={{ color: '#5C4B6E' }}>Loading contact details &amp; delivery addresses...</p>
          ) : (
            <>
              <div className="alert alert-info" style={{ fontSize: '0.86rem' }}>
                <div><strong>UID:</strong> <code>{selectedCustomer.userId || selectedCustomer.id}</code></div>
                <div><strong>Email:</strong> {selectedEmail || 'Not recorded'}</div>
                <div><strong>Phone:</strong> {selectedPhone || 'Not recorded'}</div>
                {selectedPhone && (
                  <div style={{ marginTop: 8 }}>
                    <a
                      href={buildWhatsAppLink(
                        selectedPhone,
                        `Hello ${selectedCustomer.displayName || 'Valued Client'}, reaching out from ZEECROWN STORE Customer Care.`
                      )}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="zc-pill-btn whatsapp"
                    >
                      Message on WhatsApp
                    </a>
                  </div>
                )}
              </div>

              <h4 style={{ marginBottom: 8 }}>Saved Delivery Addresses ({savedAddresses.length})</h4>
              {savedAddresses.length === 0 ? (
                <p style={{ color: '#5C4B6E', fontSize: '0.86rem' }}>
                  No saved delivery addresses yet.
                </p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 14 }}>
                  {savedAddresses.map((addr) => (
                    <div
                      key={addr.id}
                      style={{
                        padding: 10,
                        borderRadius: 10,
                        border: '1px solid #D9D9E0',
                        background: '#FFFCF5',
                        fontSize: '0.84rem',
                      }}
                    >
                      <strong>{addr.label}</strong> {addr.defaultAddress ? '· Default' : ''}
                      <div>{addr.recipientName} &middot; {addr.phone}</div>
                      <div style={{ color: '#5C4B6E' }}>
                        {[addr.streetAddress, addr.lga, addr.city, addr.state, addr.country]
                          .filter(Boolean)
                          .join(', ')}
                      </div>
                      {addr.landmark && <div style={{ color: '#5C4B6E' }}>Landmark: {addr.landmark}</div>}
                      {addr.deliveryInstructions && (
                        <div style={{ color: '#5C4B6E' }}>Instructions: {addr.deliveryInstructions}</div>
                      )}
                    </div>
                  ))}
                </div>
              )}

              <h4 style={{ marginBottom: 8 }}>Order History ({selectedOrders.length})</h4>
              {selectedOrders.length === 0 ? (
                <p style={{ color: '#5C4B6E', fontSize: '0.86rem' }}>No orders placed yet.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 14 }}>
                  {selectedOrders.slice(0, 8).map((o) => (
                    <div
                      key={o.id}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: '0.82rem',
                        borderBottom: '1px solid #D9D9E0',
                        paddingBottom: 6,
                      }}
                    >
                      <span>
                        <strong>{o.orderNumber}</strong> &middot; {formatOrderStatus(o.status)}
                      </span>
                      <strong>
                        {formatMoney(o.totalNgn, currency, undefined, storeSettings.usdRateNgn)}
                      </strong>
                    </div>
                  ))}
                </div>
              )}

              <form onSubmit={(e) => void handleSaveSupportNotes(e)}>
                <div className="field">
                  <label>Admin Support / Delivery CRM Notes</label>
                  <textarea
                    rows={3}
                    value={notesDraft}
                    onChange={(e) => setNotesDraft(e.target.value)}
                    placeholder="Preferred courier, VIP sizing notes, delivery preferences..."
                  />
                </div>
                <button type="submit" className="btn" disabled={savingNotes}>
                  {savingNotes ? 'Saving Notes...' : 'Save Customer Notes'}
                </button>
              </form>
            </>
          )}
        </div>
      )}
    </div>
  );
}
