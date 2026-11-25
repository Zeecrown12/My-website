import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { doc, getDoc } from 'firebase/firestore';
import { Search } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { buildTrackingLookupId, db } from '../lib/firebase';
import { formatMoney, formatOrderStatus } from '../lib/formatters';
import { OrderStatusHistoryEntry } from '../types/store';

interface TrackedProjection {
  orderNumber: string;
  status: string;
  paymentStatus: string;
  currency: 'NGN' | 'USD';
  totalNgn: number;
  itemCount: number;
  itemsSummary: string;
  maskedDestination: string;
  courierName?: string;
  trackingReference?: string;
  trackingUrl?: string;
  estimatedDeliveryWindow?: string;
  deliveryStatus?: string;
  statusHistory?: OrderStatusHistoryEntry[];
}

const LOOKUP_TIMESTAMPS: number[] = [];

export function TrackOrderPage() {
  const [searchParams] = useSearchParams();
  const tokenParam = searchParams.get('token') || '';
  const { orders, currency, storeSettings } = useStore();

  const [trackingToken, setTrackingToken] = useState(tokenParam);
  const [orderNumber, setOrderNumber] = useState('');
  const [contact, setContact] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<TrackedProjection | null>(null);

  const performLookup = async (params: {
    token?: string;
    orderNum?: string;
    contactInfo?: string;
  }) => {
    const now = Date.now();
    while (LOOKUP_TIMESTAMPS.length > 0 && now - LOOKUP_TIMESTAMPS[0] > 60_000) {
      LOOKUP_TIMESTAMPS.shift();
    }
    if (LOOKUP_TIMESTAMPS.length >= 12) {
      setError('Too many tracking attempts. Please wait a minute and try again.');
      return;
    }
    LOOKUP_TIMESTAMPS.push(now);

    setLoading(true);
    setError(null);
    try {
      // Direct single-doc get allowed by firestore.rules (no list enumeration, Spark-compatible)
      if (params.token && params.token.trim().length >= 8) {
        const tokenSnap = await getDoc(doc(db, 'orderTrackingTokens', params.token.trim()));
        if (tokenSnap.exists()) {
          setResult(tokenSnap.data() as TrackedProjection);
          return;
        }
      }

      if (params.orderNum && params.contactInfo) {
        const lookupId = await buildTrackingLookupId(params.orderNum, params.contactInfo);
        if (lookupId) {
          const lookupSnap = await getDoc(doc(db, 'orderTrackingLookups', lookupId));
          if (lookupSnap.exists()) {
            setResult(lookupSnap.data() as TrackedProjection);
            return;
          }
        }
      }

      setResult(null);
      setError('No matching order found for the details entered.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to look up order right now.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tokenParam) {
      void performLookup({ token: tokenParam });
    }
  }, [tokenParam]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    void performLookup({
      token: trackingToken,
      orderNum: orderNumber,
      contactInfo: contact,
    });
  };

  return (
    <div className="container section">
      <h1 style={{ marginBottom: 8 }}>Track Your Order</h1>
      <p style={{ color: '#5C4B6E', marginTop: 0, marginBottom: 24 }}>
        Enter your private tracking token OR your Order Number together with the checkout email/phone number.
      </p>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: 24,
          alignItems: 'start',
        }}
      >
        <form onSubmit={handleSubmit} className="card" style={{ padding: 22 }}>
          <div className="field">
            <label>Option A: Private Tracking Token</label>
            <input
              value={trackingToken}
              onChange={(e) => setTrackingToken(e.target.value)}
              placeholder="Paste tracking token"
            />
          </div>

          <div
            style={{
              textAlign: 'center',
              fontSize: '0.82rem',
              color: '#5C4B6E',
              margin: '10px 0',
            }}
          >
            — OR —
          </div>

          <div className="field">
            <label>Option B: Order Number</label>
            <input
              value={orderNumber}
              onChange={(e) => setOrderNumber(e.target.value)}
              placeholder="e.g. ZC-A1B2C3"
            />
          </div>
          <div className="field">
            <label>Checkout Email or Phone</label>
            <input
              value={contact}
              onChange={(e) => setContact(e.target.value)}
              placeholder="Email or phone used on order"
            />
          </div>

          <button type="submit" className="btn" disabled={loading} style={{ width: '100%' }}>
            <Search size={16} />
            {loading ? 'Looking up order...' : 'Track Order'}
          </button>

          {orders.length > 0 && (
            <div style={{ marginTop: 18 }}>
              <strong style={{ display: 'block', fontSize: '0.86rem', marginBottom: 8 }}>
                Your Recent Orders:
              </strong>
              <div className="chip-row">
                {orders.slice(0, 5).map((o) => (
                  <button
                    key={o.id}
                    type="button"
                    className="chip"
                    onClick={() => {
                      if (o.trackingToken) {
                        setTrackingToken(o.trackingToken);
                        void performLookup({ token: o.trackingToken });
                      }
                    }}
                  >
                    {o.orderNumber}
                  </button>
                ))}
              </div>
            </div>
          )}
        </form>

        <div>
          {error && <div className="alert alert-error">{error}</div>}

          {result ? (
            <div className="card" style={{ padding: 22 }}>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: 8,
                  marginBottom: 12,
                }}
              >
                <h2 style={{ margin: 0 }}>Order {result.orderNumber}</h2>
                <span className="tag-badge">{formatOrderStatus(result.status)}</span>
              </div>

              <p style={{ color: '#5C4B6E', marginTop: 0 }}>{result.itemsSummary}</p>
              <div style={{ display: 'grid', gap: 6, fontSize: '0.92rem', marginBottom: 16 }}>
                <div>
                  <strong>Payment Status:</strong> {formatOrderStatus(result.paymentStatus)}
                </div>
                <div>
                  <strong>Destination:</strong> {result.maskedDestination}
                </div>
                <div>
                  <strong>Total:</strong>{' '}
                  {formatMoney(result.totalNgn, currency, undefined, storeSettings.usdRateNgn)}
                </div>
                {result.courierName && (
                  <div>
                    <strong>Courier:</strong> {result.courierName}{' '}
                    {result.trackingReference ? `(${result.trackingReference})` : ''}
                  </div>
                )}
                {result.estimatedDeliveryWindow && (
                  <div>
                    <strong>Estimated Delivery:</strong> {result.estimatedDeliveryWindow}
                  </div>
                )}
                {result.trackingUrl && (
                  <div>
                    <a
                      href={result.trackingUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{ color: '#542080', fontWeight: 700 }}
                    >
                      Open Courier Tracking Link →
                    </a>
                  </div>
                )}
              </div>

              {result.statusHistory && result.statusHistory.length > 0 && (
                <div>
                  <h4 style={{ marginBottom: 10 }}>Fulfillment Timeline</h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                    {result.statusHistory.map((step, idx) => (
                      <div
                        key={idx}
                        style={{
                          padding: '10px 12px',
                          background: '#F4EFF9',
                          borderLeft: '3px solid #542080',
                          borderRadius: 8,
                        }}
                      >
                        <strong>{step.label || formatOrderStatus(step.stage)}</strong>
                        {step.note && (
                          <div style={{ fontSize: '0.84rem', color: '#5C4B6E' }}>{step.note}</div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="card" style={{ padding: 26, color: '#5C4B6E' }}>
              Enter your order details on the left to inspect live fulfillment &amp; courier progress.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
