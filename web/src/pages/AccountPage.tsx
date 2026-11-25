import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  addDoc,
  collection,
  doc,
  getDocs,
  query,
  serverTimestamp,
  updateDoc,
  where,
} from 'firebase/firestore';
import { Bell, LogOut, Package, ShieldCheck, Trash2 } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { db } from '../lib/firebase';
import { StoreNotification, SupportEnquiry } from '../types/store';
import { formatMoney, formatOrderStatus, resolveProductImageUrl } from '../lib/formatters';

export function AccountPage() {
  const {
    user,
    isAdmin,
    adminRole,
    orders,
    wishlist,
    addresses,
    products,
    currency,
    storeSettings,
    deleteAddress,
    toggleWishlist,
    signOut,
  } = useStore();

  const [tab, setTab] = useState<'orders' | 'notifications' | 'wishlist' | 'addresses' | 'support'>('orders');
  const [enquiries, setEnquiries] = useState<SupportEnquiry[]>([]);
  const [notifications, setNotifications] = useState<StoreNotification[]>([]);
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [supportStatus, setSupportStatus] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    void Promise.all([
      getDocs(query(collection(db, 'supportEnquiries'), where('userId', '==', user.uid))),
      getDocs(query(collection(db, 'notifications'), where('userId', '==', user.uid))),
    ])
      .then(([enqSnap, notifSnap]) => {
        setEnquiries(
          enqSnap.docs.map((d) => ({
            id: d.id,
            ...(d.data() as Omit<SupportEnquiry, 'id'>),
          }))
        );
        setNotifications(
          notifSnap.docs
            .map((d) => ({
              id: d.id,
              ...(d.data() as Omit<StoreNotification, 'id'>),
            }))
            .reverse()
        );
      })
      .catch(() => undefined);
  }, [user]);

  const handleMarkNotificationRead = async (notifId: string) => {
    try {
      await updateDoc(doc(db, 'notifications', notifId), {
        readFlag: true,
        readAt: serverTimestamp(),
      });
      setNotifications((prev) =>
        prev.map((n) => (n.id === notifId ? { ...n, readFlag: true } : n))
      );
    } catch {
      // ignore if read-only/offline
    }
  };

  if (!user) {
    return (
      <div className="container section" style={{ maxWidth: 520 }}>
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <h2>My ZEECROWN Account</h2>
          <p style={{ color: '#5C4B6E' }}>
            Sign in to view your orders, saved delivery addresses, synced wishlist, and support tickets.
          </p>
          <Link to="/login" className="btn">
            Sign In / Register
          </Link>
        </div>
      </div>
    );
  }

  const handleSubmitTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (subject.trim().length < 2 || message.trim().length < 5) {
      setSupportStatus('Please enter a subject (at least 2 characters) and message (at least 5 characters).');
      return;
    }
    setSupportStatus('Sending enquiry...');
    try {
      const ref = await addDoc(collection(db, 'supportEnquiries'), {
        userId: user.uid,
        customerName: (user.displayName || user.email?.split('@')[0] || 'Client').slice(0, 80),
        customerEmail: (user.email || '').slice(0, 200),
        category: 'GENERAL',
        subject: subject.trim().slice(0, 140),
        message: message.trim().slice(0, 1800),
        status: 'NEW',
        priority: 'NORMAL',
        sourceChannel: 'WEB_ACCOUNT',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setEnquiries((prev) => [
        {
          id: ref.id,
          userId: user.uid,
          customerName: user.displayName || 'Client',
          category: 'GENERAL',
          subject: subject.trim(),
          message: message.trim(),
          status: 'NEW',
          priority: 'NORMAL',
        },
        ...prev,
      ]);
      setSubject('');
      setMessage('');
      setSupportStatus('Enquiry sent to ZEECROWN Customer Care!');
    } catch (err) {
      setSupportStatus(err instanceof Error ? err.message : 'Unable to send enquiry.');
    }
  };

  return (
    <div className="container section">
      <div
        className="card"
        style={{
          padding: 22,
          marginBottom: 24,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 14,
        }}
      >
        <div>
          <h1 style={{ marginBottom: 4 }}>{user.displayName || 'ZEECROWN Client'}</h1>
          <div style={{ color: '#5C4B6E', fontSize: '0.92rem' }}>
            {user.email} {isAdmin && <strong>&middot; Admin ({adminRole})</strong>}
          </div>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {isAdmin && (
            <Link to="/admin" className="btn">
              <ShieldCheck size={16} /> Open Admin Dashboard
            </Link>
          )}
          <button type="button" className="btn btn-outline" onClick={() => void signOut()}>
            <LogOut size={16} /> Sign Out
          </button>
        </div>
      </div>

      <div className="tabs">
        <button
          type="button"
          className={`chip ${tab === 'orders' ? 'active' : ''}`}
          onClick={() => setTab('orders')}
        >
          Orders ({orders.length})
        </button>
        <button
          type="button"
          className={`chip ${tab === 'notifications' ? 'active' : ''}`}
          onClick={() => setTab('notifications')}
        >
          Updates ({notifications.filter((n) => !n.readFlag).length})
        </button>
        <button
          type="button"
          className={`chip ${tab === 'wishlist' ? 'active' : ''}`}
          onClick={() => setTab('wishlist')}
        >
          Wishlist ({wishlist.length})
        </button>
        <button
          type="button"
          className={`chip ${tab === 'addresses' ? 'active' : ''}`}
          onClick={() => setTab('addresses')}
        >
          Addresses ({addresses.length})
        </button>
        <button
          type="button"
          className={`chip ${tab === 'support' ? 'active' : ''}`}
          onClick={() => setTab('support')}
        >
          Support ({enquiries.length})
        </button>
      </div>

      {tab === 'notifications' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {notifications.length === 0 ? (
            <div className="card" style={{ padding: 28, textAlign: 'center' }}>
              <Bell size={30} color="#542080" style={{ marginBottom: 8 }} />
              <h3>No Store Updates Yet</h3>
              <p style={{ color: '#5C4B6E', margin: 0 }}>
                Order confirmations, payment verifications, dispatch updates, and support replies appear here.
              </p>
            </div>
          ) : (
            notifications.map((n) => (
              <div
                key={n.id}
                className="card"
                style={{
                  padding: 16,
                  borderLeft: n.readFlag ? '3px solid #D9D9E0' : '4px solid #542080',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap' }}>
                  <strong>{n.title}</strong>
                  <span className="tag-badge">{n.category}</span>
                </div>
                <p style={{ margin: '6px 0 10px', color: '#5C4B6E', fontSize: '0.9rem' }}>{n.body}</p>
                {!n.readFlag && (
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => void handleMarkNotificationRead(n.id)}
                  >
                    Mark as Read
                  </button>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'orders' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          {orders.length === 0 ? (
            <div className="card" style={{ padding: 28, textAlign: 'center' }}>
              <Package size={32} color="#542080" style={{ marginBottom: 8 }} />
              <h3>No Orders Yet</h3>
              <Link to="/shop" className="btn">
                Start Shopping
              </Link>
            </div>
          ) : (
            orders.map((o) => (
              <div key={o.id} className="card" style={{ padding: 18 }}>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    gap: 10,
                    marginBottom: 8,
                  }}
                >
                  <strong>Order {o.orderNumber}</strong>
                  <span className="tag-badge">
                    {formatOrderStatus(o.status)} &middot; Payment: {formatOrderStatus(o.paymentStatus)}
                  </span>
                </div>
                <p style={{ margin: '0 0 8px', color: '#5C4B6E', fontSize: '0.9rem' }}>
                  {o.itemsSummary}
                </p>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    gap: 10,
                    alignItems: 'center',
                  }}
                >
                  <strong style={{ color: '#542080' }}>
                    {formatMoney(o.totalNgn, currency, undefined, storeSettings.usdRateNgn)}
                  </strong>
                  <Link
                    to={
                      o.trackingToken
                        ? `/track?token=${encodeURIComponent(o.trackingToken)}`
                        : '/track'
                    }
                    className="zc-pill-btn"
                  >
                    View Tracking Timeline →
                  </Link>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'wishlist' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 16 }}>
          {wishlist.length === 0 ? (
            <p style={{ color: '#5C4B6E' }}>No saved wishlist pieces yet.</p>
          ) : (
            wishlist.map((w) => {
              const prod = products.find((p) => p.id === w.productId);
              return (
                <div key={w.id} className="card" style={{ padding: 14 }}>
                  <img
                    src={resolveProductImageUrl(w.productImageUrl)}
                    alt={w.productTitle}
                    style={{ width: '100%', aspectRatio: '4 / 5', objectFit: 'cover', borderRadius: 10 }}
                  />
                  <strong style={{ display: 'block', marginTop: 10 }}>{w.productTitle}</strong>
                  <div style={{ color: '#542080', fontWeight: 700, marginBottom: 10 }}>
                    {formatMoney(w.priceNgn, currency, undefined, storeSettings.usdRateNgn)}
                  </div>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Link
                      to={`/product/${encodeURIComponent(prod?.slug || w.productId)}`}
                      className="btn"
                      style={{ flex: 1, padding: '8px 12px', fontSize: '0.82rem' }}
                    >
                      View
                    </Link>
                    {prod && (
                      <button
                        type="button"
                        className="zc-pill-btn"
                        onClick={() => void toggleWishlist(prod)}
                      >
                        Remove
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {tab === 'addresses' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 16 }}>
          {addresses.length === 0 ? (
            <p style={{ color: '#5C4B6E' }}>No saved delivery addresses yet. Add one during checkout.</p>
          ) : (
            addresses.map((a) => (
              <div key={a.id} className="card" style={{ padding: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <strong>{a.label}</strong>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => void deleteAddress(a.id)}
                    aria-label="Delete address"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
                <div style={{ fontSize: '0.9rem' }}>{a.recipientName} &middot; {a.phone}</div>
                <div style={{ fontSize: '0.86rem', color: '#5C4B6E', marginTop: 4 }}>
                  {a.streetAddress}, {a.city}, {a.state}, {a.country}
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'support' && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: 20,
          }}
        >
          <div>
            <h3>Your Support Enquiries</h3>
            {enquiries.length === 0 ? (
              <p style={{ color: '#5C4B6E' }}>No support tickets yet.</p>
            ) : (
              enquiries.map((q) => (
                <div key={q.id} className="card" style={{ padding: 16, marginBottom: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <strong>{q.subject}</strong>
                    <span className="tag-badge">{q.status}</span>
                  </div>
                  <p style={{ margin: '0 0 8px', color: '#5C4B6E', fontSize: '0.9rem' }}>
                    {q.message}
                  </p>
                  {q.lastReplyPreview && (
                    <div className="alert alert-info" style={{ marginBottom: 0, fontSize: '0.84rem' }}>
                      <strong>Latest Staff Reply:</strong> {q.lastReplyPreview}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>

          <form onSubmit={(e) => void handleSubmitTicket(e)} className="card" style={{ padding: 20 }}>
            <h3>Open a Support Ticket</h3>
            {supportStatus && <div className="alert alert-info">{supportStatus}</div>}
            <div className="field">
              <label>Subject</label>
              <input
                required
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="Order ZC-... inquiry or sizing question"
              />
            </div>
            <div className="field">
              <label>Message</label>
              <textarea
                required
                rows={4}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="How can our Customer Care team help?"
              />
            </div>
            <button type="submit" className="btn">
              Submit Support Ticket
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
