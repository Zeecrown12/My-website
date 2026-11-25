import React, { useMemo, useState } from 'react';
import { doc, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';
import { Search } from 'lucide-react';
import { db } from '../../lib/firebase';
import { buildWhatsAppLink, formatMoney, formatOrderStatus } from '../../lib/formatters';
import { CurrencyCode, PaymentRecord, StoreOrder, StoreSettings } from '../../types/store';
import { AdminActor, recordAdminAudit } from './adminHelpers';

interface AdminOrdersTabProps {
  actor: AdminActor;
  orders: StoreOrder[];
  paymentsByOrderId: Record<string, PaymentRecord>;
  currency: CurrencyCode;
  storeSettings: StoreSettings;
  onReload: () => Promise<void>;
  onStatus: (msg: string) => void;
}

const ORDER_STATUSES = [
  'PENDING_PAYMENT',
  'PAYMENT_SUBMITTED',
  'CONFIRMED',
  'PROCESSING',
  'READY_FOR_DISPATCH',
  'SHIPPED',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'CANCELLED',
  'REFUNDED',
] as const;

export function AdminOrdersTab({
  actor,
  orders,
  paymentsByOrderId,
  currency,
  storeSettings,
  onReload,
  onStatus,
}: AdminOrdersTabProps) {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [queryText, setQueryText] = useState('');
  const [expandedOrderId, setExpandedOrderId] = useState<string | null>(null);

  // Fulfillment / tracking draft for expanded order
  const [nextStatusDraft, setNextStatusDraft] = useState('');
  const [courierDraft, setCourierDraft] = useState('');
  const [trackingRefDraft, setTrackingRefDraft] = useState('');
  const [trackingUrlDraft, setTrackingUrlDraft] = useState('');
  const [etaDraft, setEtaDraft] = useState('');
  const [deliveryNotesDraft, setDeliveryNotesDraft] = useState('');
  const [internalNotesDraft, setInternalNotesDraft] = useState('');
  const [paymentNoteDraft, setPaymentNoteDraft] = useState('');
  const [busyOrderId, setBusyOrderId] = useState<string | null>(null);

  const openOrderEditor = (o: StoreOrder) => {
    if (expandedOrderId === o.id) {
      setExpandedOrderId(null);
      return;
    }
    setExpandedOrderId(o.id);
    setNextStatusDraft(o.status || 'CONFIRMED');
    setCourierDraft(o.courierName || '');
    setTrackingRefDraft(o.trackingReference || '');
    setTrackingUrlDraft(o.trackingUrl || '');
    setEtaDraft(o.estimatedDeliveryWindow || '');
    setDeliveryNotesDraft(o.deliveryNotes || '');
    setInternalNotesDraft(o.internalNotes || '');
    setPaymentNoteDraft(paymentsByOrderId[o.id]?.verificationNote || '');
  };

  const syncProjectionsAndNotify = async (
    order: StoreOrder,
    updates: Record<string, unknown>,
    notifTitle: string,
    notifBody: string,
    notifCategory: 'ORDER' | 'PAYMENT'
  ) => {
    const calls: Array<Promise<unknown>> = [];
    if (order.trackingToken) {
      calls.push(
        updateDoc(doc(db, 'orderTrackingTokens', order.trackingToken), updates).catch(() => undefined)
      );
    }
    if (order.guestLookupHashEmail) {
      calls.push(
        updateDoc(doc(db, 'orderTrackingLookups', order.guestLookupHashEmail), updates).catch(() => undefined)
      );
    }
    if (order.guestLookupHashPhone) {
      calls.push(
        updateDoc(doc(db, 'orderTrackingLookups', order.guestLookupHashPhone), updates).catch(() => undefined)
      );
    }
    if (order.userId) {
      calls.push(
        setDoc(
          doc(db, 'notifications', `notif_${notifCategory.toLowerCase()}_${order.id}_${Date.now()}`),
          {
            userId: order.userId,
            title: notifTitle.slice(0, 120),
            body: notifBody.slice(0, 400),
            category: notifCategory,
            readFlag: false,
            targetRoute: `track_order/${order.id}`,
            orderId: order.id,
            orderNumber: order.orderNumber,
            createdAt: serverTimestamp(),
          }
        ).catch(() => undefined)
      );
    }
    await Promise.all(calls);
  };

  const handlePaymentDecision = async (
    order: StoreOrder,
    decision: 'PAID' | 'FAILED',
    note = paymentNoteDraft
  ) => {
    setBusyOrderId(order.id);
    try {
      const nextOrderStatus = decision === 'PAID' ? 'CONFIRMED' : 'PENDING_PAYMENT';
      const nextHistory = [
        ...(order.statusHistory || []),
        {
          stage: nextOrderStatus,
          label: decision === 'PAID' ? 'Payment Verified' : 'Payment Marked Failed',
          note: note.trim() || `Payment updated to ${decision} by ${actor.email}`,
          actorRole: 'ADMIN',
          timestampMillis: Date.now(),
        },
      ].slice(-30);

      await updateDoc(doc(db, 'orders', order.id), {
        status: nextOrderStatus,
        paymentStatus: decision,
        statusHistory: nextHistory,
        updatedAt: serverTimestamp(),
      });

      const linkedPayment = paymentsByOrderId[order.id];
      if (linkedPayment) {
        await updateDoc(doc(db, 'payments', linkedPayment.id), {
          status: decision === 'PAID' ? 'VERIFIED' : 'FAILED',
          verificationNote: (note.trim() || `Reviewed by ${actor.email}`).slice(0, 400),
          reviewedByEmail: actor.email.slice(0, 200),
          reviewedAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        }).catch(() => undefined);
      }

      await syncProjectionsAndNotify(
        order,
        {
          status: nextOrderStatus,
          paymentStatus: decision,
          statusHistory: nextHistory,
          updatedAt: serverTimestamp(),
        },
        decision === 'PAID' ? 'Payment Verified & Order Confirmed' : 'Payment Verification Issue',
        decision === 'PAID'
          ? `Payment for order ${order.orderNumber} is verified. We are preparing your order.`
          : `We could not verify payment for order ${order.orderNumber}. Please contact support or retry.`,
        'PAYMENT'
      );

      await recordAdminAudit(
        actor,
        decision === 'PAID' ? 'VERIFY_PAYMENT' : 'FAIL_PAYMENT',
        'order',
        order.id,
        `Marked order ${order.orderNumber} payment as ${decision}`
      );
      onStatus(`Order ${order.orderNumber} payment marked ${decision}.`);
      await onReload();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to update payment status.');
    } finally {
      setBusyOrderId(null);
    }
  };

  const handleSaveFulfillment = async (order: StoreOrder, statusOverride?: string) => {
    setBusyOrderId(order.id);
    try {
      const targetStatus = statusOverride || nextStatusDraft || order.status;
      const nextHistory = [
        ...(order.statusHistory || []),
        {
          stage: targetStatus,
          label: formatOrderStatus(targetStatus),
          note:
            deliveryNotesDraft.trim() ||
            `Fulfillment updated to ${formatOrderStatus(targetStatus)} by ${actor.email}`,
          actorRole: 'ADMIN',
          timestampMillis: Date.now(),
        },
      ].slice(-30);

      const orderUpdates: Record<string, unknown> = {
        status: targetStatus,
        courierName: courierDraft.trim().slice(0, 80),
        trackingReference: trackingRefDraft.trim().slice(0, 100),
        trackingUrl: trackingUrlDraft.trim().slice(0, 400),
        estimatedDeliveryWindow: etaDraft.trim().slice(0, 120),
        deliveryStatus: formatOrderStatus(targetStatus).slice(0, 60),
        deliveryNotes: deliveryNotesDraft.trim().slice(0, 400),
        internalNotes: internalNotesDraft.trim().slice(0, 1000),
        statusHistory: nextHistory,
        updatedAt: serverTimestamp(),
      };
      if (targetStatus === 'SHIPPED' || targetStatus === 'DISPATCHED') {
        orderUpdates.dispatchedAt = serverTimestamp();
      }
      if (targetStatus === 'DELIVERED') {
        orderUpdates.deliveredAt = serverTimestamp();
      }

      await updateDoc(doc(db, 'orders', order.id), orderUpdates);

      const projectionUpdates: Record<string, unknown> = {
        status: targetStatus,
        courierName: courierDraft.trim().slice(0, 80),
        trackingReference: trackingRefDraft.trim().slice(0, 100),
        trackingUrl: trackingUrlDraft.trim().slice(0, 400),
        estimatedDeliveryWindow: etaDraft.trim().slice(0, 120),
        deliveryStatus: formatOrderStatus(targetStatus).slice(0, 60),
        deliveryNotes: deliveryNotesDraft.trim().slice(0, 400),
        statusHistory: nextHistory,
        updatedAt: serverTimestamp(),
      };
      if (targetStatus === 'SHIPPED' || targetStatus === 'DISPATCHED') {
        projectionUpdates.dispatchedAt = serverTimestamp();
      }
      if (targetStatus === 'DELIVERED') {
        projectionUpdates.deliveredAt = serverTimestamp();
      }

      await syncProjectionsAndNotify(
        order,
        projectionUpdates,
        `Order ${formatOrderStatus(targetStatus)}`,
        `Your order ${order.orderNumber} is now ${formatOrderStatus(targetStatus)}${
          trackingRefDraft.trim() ? ` (Ref: ${trackingRefDraft.trim()})` : ''
        }.`,
        'ORDER'
      );

      await recordAdminAudit(
        actor,
        'UPDATE_ORDER_FULFILLMENT',
        'order',
        order.id,
        `Updated order ${order.orderNumber} to ${targetStatus}`
      );
      onStatus(`Order ${order.orderNumber} updated to ${formatOrderStatus(targetStatus)}.`);
      await onReload();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to update order fulfillment.');
    } finally {
      setBusyOrderId(null);
    }
  };

  const handleCancellationDecision = async (order: StoreOrder, decision: 'APPROVED' | 'REJECTED') => {
    setBusyOrderId(order.id);
    try {
      const nextStatus = decision === 'APPROVED' ? 'CANCELLED' : order.status;
      const nextHistory = [
        ...(order.statusHistory || []),
        {
          stage: nextStatus,
          label: decision === 'APPROVED' ? 'Cancellation Approved' : 'Cancellation Declined',
          note: `Cancellation ${decision.toLowerCase()} by ${actor.email}`,
          actorRole: 'ADMIN',
          timestampMillis: Date.now(),
        },
      ].slice(-30);

      await updateDoc(doc(db, 'orders', order.id), {
        status: nextStatus,
        cancellationStatus: decision,
        statusHistory: nextHistory,
        updatedAt: serverTimestamp(),
      });
      await syncProjectionsAndNotify(
        order,
        { status: nextStatus, statusHistory: nextHistory, updatedAt: serverTimestamp() },
        decision === 'APPROVED' ? 'Order Cancelled' : 'Cancellation Request Reviewed',
        decision === 'APPROVED'
          ? `Your cancellation request for order ${order.orderNumber} was approved.`
          : `Your cancellation request for order ${order.orderNumber} could not be approved.`,
        'ORDER'
      );
      await onReload();
      onStatus(`Cancellation ${decision.toLowerCase()} for ${order.orderNumber}.`);
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to process cancellation request.');
    } finally {
      setBusyOrderId(null);
    }
  };

  const filteredOrders = useMemo(() => {
    const q = queryText.trim().toLowerCase();
    return orders.filter((o) => {
      if (statusFilter === 'PENDING_PAYMENTS') {
        if (o.paymentStatus !== 'PENDING_VERIFICATION' && o.paymentStatus !== 'UNPAID') return false;
      } else if (statusFilter !== 'ALL' && o.status !== statusFilter) {
        return false;
      }
      if (!q) return true;
      return (
        o.orderNumber.toLowerCase().includes(q) ||
        (o.recipientName || '').toLowerCase().includes(q) ||
        (o.customerEmail || '').toLowerCase().includes(q) ||
        (o.recipientPhone || '').toLowerCase().includes(q) ||
        (o.shippingCity || '').toLowerCase().includes(q)
      );
    });
  }, [orders, statusFilter, queryText]);

  return (
    <div className="card" style={{ padding: 20 }}>
      <h3>Orders, Delivery &amp; Payment Verification ({filteredOrders.length})</h3>

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
            placeholder="Search order #, customer, phone, city..."
            style={{ border: 'none', outline: 'none', background: 'transparent', width: '100%' }}
          />
        </div>
        {[
          { key: 'ALL', label: 'All' },
          { key: 'PENDING_PAYMENTS', label: 'Pending Payments' },
          { key: 'CONFIRMED', label: 'Confirmed' },
          { key: 'PROCESSING', label: 'Processing' },
          { key: 'SHIPPED', label: 'Shipped' },
          { key: 'DELIVERED', label: 'Delivered' },
        ].map((chip) => (
          <button
            key={chip.key}
            type="button"
            className={`chip ${statusFilter === chip.key ? 'active' : ''}`}
            onClick={() => setStatusFilter(chip.key)}
          >
            {chip.label}
          </button>
        ))}
      </div>

      <div className="table-wrap">
        <table className="zc-table">
          <thead>
            <tr>
              <th>Order &amp; Items</th>
              <th>Customer &amp; Delivery Address</th>
              <th>Total / Payment</th>
              <th>Fulfillment</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredOrders.map((o) => {
              const payment = paymentsByOrderId[o.id];
              const isExpanded = expandedOrderId === o.id;
              return (
                <React.Fragment key={o.id}>
                  <tr>
                    <td>
                      <strong>{o.orderNumber}</strong>
                      <div style={{ fontSize: '0.8rem', color: '#5C4B6E' }}>{o.itemsSummary}</div>
                      {o.cancellationStatus === 'REQUESTED' && (
                        <div style={{ fontSize: '0.78rem', color: '#9E2A3B', fontWeight: 700, marginTop: 4 }}>
                          Cancellation Requested: {o.cancellationRequestReason || 'Customer requested'}
                        </div>
                      )}
                    </td>
                    <td style={{ fontSize: '0.84rem' }}>
                      <strong>{o.recipientName || o.customerEmail || o.userId}</strong>
                      {o.recipientPhone && <div>{o.recipientPhone}</div>}
                      <div style={{ color: '#5C4B6E' }}>
                        {[o.shippingAddress, o.lga, o.shippingCity, o.shippingState, o.shippingCountry]
                          .filter(Boolean)
                          .join(', ')}
                      </div>
                      {o.landmark && <div style={{ color: '#5C4B6E' }}>Landmark: {o.landmark}</div>}
                      {o.deliveryInstructions && (
                        <div style={{ color: '#5C4B6E' }}>Note: {o.deliveryInstructions}</div>
                      )}
                    </td>
                    <td>
                      <strong>{formatMoney(o.totalNgn, currency, undefined, storeSettings.usdRateNgn)}</strong>
                      <div style={{ fontSize: '0.8rem', color: '#5C4B6E' }}>
                        {o.paymentMethod} &middot; {formatOrderStatus(o.paymentStatus)}
                      </div>
                      {payment?.transferNote && (
                        <div style={{ fontSize: '0.76rem', color: '#542080', marginTop: 2 }}>
                          Ref Note: {payment.transferNote}
                        </div>
                      )}
                    </td>
                    <td>
                      <div><strong>{formatOrderStatus(o.status)}</strong></div>
                      {o.courierName && (
                        <div style={{ fontSize: '0.78rem', color: '#5C4B6E' }}>
                          {o.courierName} {o.trackingReference ? `(${o.trackingReference})` : ''}
                        </div>
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        {o.paymentStatus !== 'PAID' && (
                          <button
                            type="button"
                            className="zc-pill-btn primary"
                            disabled={busyOrderId === o.id}
                            onClick={() => void handlePaymentDecision(o, 'PAID')}
                          >
                            Verify Payment
                          </button>
                        )}
                        <button
                          type="button"
                          className="zc-pill-btn"
                          onClick={() => openOrderEditor(o)}
                        >
                          {isExpanded ? 'Close' : 'Manage'}
                        </button>
                        {o.recipientPhone && (
                          <a
                            href={buildWhatsAppLink(
                              o.recipientPhone,
                              `Hello ${o.recipientName || 'Valued Client'}, update on your ZEECROWN STORE order ${o.orderNumber}: status is ${formatOrderStatus(o.status)} (Payment: ${formatOrderStatus(o.paymentStatus)}).`
                            )}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="zc-pill-btn whatsapp"
                          >
                            WhatsApp
                          </a>
                        )}
                      </div>
                    </td>
                  </tr>

                  {isExpanded && (
                    <tr>
                      <td colSpan={5} style={{ background: '#F4EFF9', padding: 16 }}>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 10 }}>
                          <div className="field">
                            <label>Order Status</label>
                            <select value={nextStatusDraft} onChange={(e) => setNextStatusDraft(e.target.value)}>
                              {ORDER_STATUSES.map((s) => (
                                <option key={s} value={s}>
                                  {formatOrderStatus(s)}
                                </option>
                              ))}
                            </select>
                          </div>
                          <div className="field">
                            <label>Courier / Dispatch Rider</label>
                            <input
                              value={courierDraft}
                              onChange={(e) => setCourierDraft(e.target.value)}
                              placeholder="GIG Logistics / DHL / Dispatch"
                            />
                          </div>
                          <div className="field">
                            <label>Tracking Reference</label>
                            <input
                              value={trackingRefDraft}
                              onChange={(e) => setTrackingRefDraft(e.target.value)}
                              placeholder="Waybill #"
                            />
                          </div>
                          <div className="field">
                            <label>Tracking URL (Optional)</label>
                            <input
                              value={trackingUrlDraft}
                              onChange={(e) => setTrackingUrlDraft(e.target.value)}
                              placeholder="https://..."
                            />
                          </div>
                          <div className="field">
                            <label>Estimated Delivery Window</label>
                            <input
                              value={etaDraft}
                              onChange={(e) => setEtaDraft(e.target.value)}
                              placeholder="2–4 business days"
                            />
                          </div>
                          <div className="field">
                            <label>Payment Verification Note</label>
                            <input
                              value={paymentNoteDraft}
                              onChange={(e) => setPaymentNoteDraft(e.target.value)}
                              placeholder="Bank alert confirmed..."
                            />
                          </div>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 10 }}>
                          <div className="field">
                            <label>Customer Delivery Note</label>
                            <input
                              value={deliveryNotesDraft}
                              onChange={(e) => setDeliveryNotesDraft(e.target.value)}
                              placeholder="Visible on order tracking"
                            />
                          </div>
                          <div className="field">
                            <label>Internal Staff Note</label>
                            <input
                              value={internalNotesDraft}
                              onChange={(e) => setInternalNotesDraft(e.target.value)}
                              placeholder="Admin-only note"
                            />
                          </div>
                        </div>

                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                          <button
                            type="button"
                            className="btn"
                            disabled={busyOrderId === o.id}
                            onClick={() => void handleSaveFulfillment(o)}
                          >
                            Save Fulfillment &amp; Notify
                          </button>
                          {o.paymentStatus !== 'PAID' && (
                            <button
                              type="button"
                              className="zc-pill-btn primary"
                              disabled={busyOrderId === o.id}
                              onClick={() => void handlePaymentDecision(o, 'PAID')}
                            >
                              Verify Payment (PAID)
                            </button>
                          )}
                          {o.paymentStatus !== 'FAILED' && o.paymentStatus !== 'PAID' && (
                            <button
                              type="button"
                              className="zc-pill-btn"
                              style={{ color: '#9E2A3B' }}
                              disabled={busyOrderId === o.id}
                              onClick={() => void handlePaymentDecision(o, 'FAILED')}
                            >
                              Mark Payment Failed
                            </button>
                          )}
                          {o.cancellationStatus === 'REQUESTED' && (
                            <>
                              <button
                                type="button"
                                className="zc-pill-btn"
                                style={{ color: '#9E2A3B' }}
                                disabled={busyOrderId === o.id}
                                onClick={() => void handleCancellationDecision(o, 'APPROVED')}
                              >
                                Approve Cancellation
                              </button>
                              <button
                                type="button"
                                className="zc-pill-btn"
                                disabled={busyOrderId === o.id}
                                onClick={() => void handleCancellationDecision(o, 'REJECTED')}
                              >
                                Reject Cancellation
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
