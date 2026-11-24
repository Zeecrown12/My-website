import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, Lock } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { formatMoney, resolveEffectiveReturnPolicy } from '../lib/formatters';

export function CheckoutPage() {
  const {
    user,
    cart,
    addresses,
    deliveryZones,
    storeSettings,
    currency,
    saveAddress,
    placeOrder,
  } = useStore();

  const [selectedAddressId, setSelectedAddressId] = useState<string>('NEW');
  const [recipientName, setRecipientName] = useState('');
  const [recipientPhone, setRecipientPhone] = useState('');
  const [shippingCountry, setShippingCountry] = useState('Nigeria');
  const [shippingState, setShippingState] = useState('Lagos');
  const [shippingCity, setShippingCity] = useState('Lagos');
  const [lga, setLga] = useState('');
  const [shippingAddress, setShippingAddress] = useState('');
  const [landmark, setLandmark] = useState('');
  const [deliveryInstructions, setDeliveryInstructions] = useState('');
  const [saveForFuture, setSaveForFuture] = useState(true);

  const [deliveryZoneId, setDeliveryZoneId] = useState<string>('');
  const [paymentMethod, setPaymentMethod] = useState<
    'PAYSTACK' | 'FLUTTERWAVE' | 'BANK_TRANSFER' | 'COD'
  >('BANK_TRANSFER');
  const [transferNote, setTransferNote] = useState('');
  const [ackNoReturns, setAckNoReturns] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmed, setConfirmed] = useState<{
    orderNumber: string;
    trackingToken: string;
  } | null>(null);

  useEffect(() => {
    if (deliveryZones.length > 0 && !deliveryZoneId) {
      setDeliveryZoneId(deliveryZones[0].id);
    }
  }, [deliveryZones, deliveryZoneId]);

  useEffect(() => {
    if (addresses.length > 0 && selectedAddressId === 'NEW') {
      const def = addresses.find((a) => a.defaultAddress) || addresses[0];
      setSelectedAddressId(def.id);
    }
  }, [addresses]);

  const activeSavedAddress =
    selectedAddressId !== 'NEW'
      ? addresses.find((a) => a.id === selectedAddressId) || null
      : null;

  const selectedZone = deliveryZones.find((z) => z.id === deliveryZoneId) || null;
  const subtotalNgn = cart.reduce((sum, item) => sum + item.unitPriceNgn * item.quantity, 0);
  const freeThreshold = storeSettings.freeDeliveryThresholdNgn || 0;
  const qualifiesFree = freeThreshold > 0 && subtotalNgn >= freeThreshold;
  const deliveryFeeNgn = qualifiesFree ? 0 : selectedZone?.feeNgn || 0;
  const totalNgn = subtotalNgn + deliveryFeeNgn;

  if (confirmed) {
    return (
      <div className="container section" style={{ maxWidth: 640 }}>
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <CheckCircle2 size={46} color="#1D6B45" style={{ marginBottom: 12 }} />
          <h1>Order Placed Successfully</h1>
          <p style={{ color: '#5C4B6E' }}>
            Thank you for shopping with {storeSettings.storeName}. Keep your Order Number and
            Tracking Token for live status updates.
          </p>
          <div className="alert alert-info" style={{ textAlign: 'left' }}>
            <div>
              <strong>Order Number:</strong> {confirmed.orderNumber}
            </div>
            <div>
              <strong>Private Tracking Token:</strong> <code>{confirmed.trackingToken}</code>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link
              to={`/track?token=${encodeURIComponent(confirmed.trackingToken)}`}
              className="btn"
            >
              Track Order Live
            </Link>
            <Link to="/account" className="btn btn-outline">
              View My Orders
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="container section" style={{ maxWidth: 560 }}>
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <Lock size={34} color="#542080" style={{ marginBottom: 10 }} />
          <h2>Sign In to Complete Checkout</h2>
          <p style={{ color: '#5C4B6E' }}>
            Orders and payment verification records are securely tied to your verified ZEECROWN
            STORE customer account. Your shopping bag items are saved and will sync automatically.
          </p>
          <Link to="/login" state={{ from: '/checkout' }} className="btn">
            Sign In or Create Account
          </Link>
        </div>
      </div>
    );
  }

  if (cart.length === 0) {
    return (
      <div className="container section" style={{ maxWidth: 560 }}>
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <h2>Your Bag Is Empty</h2>
          <p style={{ color: '#5C4B6E' }}>Add items to your bag before checking out.</p>
          <Link to="/shop" className="btn">
            Return to Shop
          </Link>
        </div>
      </div>
    );
  }

  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!ackNoReturns) {
      setError('Please acknowledge the ZEECROWN STORE No-Returns / No-Refunds policy to proceed.');
      return;
    }
    if (!deliveryZoneId) {
      setError('Please select a delivery zone.');
      return;
    }

    const rName = activeSavedAddress?.recipientName || recipientName;
    const rPhone = activeSavedAddress?.phone || recipientPhone;
    const rStreet = activeSavedAddress?.streetAddress || shippingAddress;
    const rCity = activeSavedAddress?.city || shippingCity;
    const rState = activeSavedAddress?.state || shippingState;
    const rCountry = activeSavedAddress?.country || shippingCountry;

    if (!rName.trim() || !rPhone.trim() || !rStreet.trim() || !rCity.trim() || !rState.trim()) {
      setError('Please complete all required delivery address fields.');
      return;
    }

    setSubmitting(true);
    try {
      if (!activeSavedAddress && saveForFuture) {
        await saveAddress({
          label: 'Primary Address',
          recipientName: rName.trim(),
          phone: rPhone.trim(),
          country: rCountry.trim(),
          state: rState.trim(),
          city: rCity.trim(),
          lga: lga.trim(),
          streetAddress: rStreet.trim(),
          landmark: landmark.trim(),
          deliveryInstructions: deliveryInstructions.trim(),
          defaultAddress: addresses.length === 0,
        }).catch(() => undefined);
      }

      const result = await placeOrder({
        deliveryZoneId,
        deliveryFeeNgn,
        shippingCity: rCity,
        shippingState: rState,
        shippingCountry: rCountry,
        recipientName: rName,
        recipientPhone: rPhone,
        shippingAddress: rStreet,
        lga: activeSavedAddress?.lga || lga,
        landmark: activeSavedAddress?.landmark || landmark,
        deliveryInstructions: activeSavedAddress?.deliveryInstructions || deliveryInstructions,
        paymentMethod,
        transferNote,
      });
      setConfirmed({ orderNumber: result.orderNumber, trackingToken: result.trackingToken });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to place order.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container section">
      <h1 style={{ marginBottom: 20 }}>Secure Checkout</h1>

      {error && <div className="alert alert-error">{error}</div>}

      <form
        onSubmit={(e) => void handlePlaceOrder(e)}
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))',
          gap: 24,
          alignItems: 'start',
        }}
      >
        <div>
          <div className="card" style={{ padding: 22, marginBottom: 20 }}>
            <h3>1. Delivery Address</h3>

            {addresses.length > 0 && (
              <div style={{ marginBottom: 14 }}>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.86rem', marginBottom: 8 }}>
                  Choose Saved Address or Enter New
                </label>
                <div className="chip-row">
                  {addresses.map((addr) => (
                    <button
                      key={addr.id}
                      type="button"
                      className={`chip ${selectedAddressId === addr.id ? 'active' : ''}`}
                      onClick={() => setSelectedAddressId(addr.id)}
                    >
                      {addr.label}: {addr.city}, {addr.state}
                    </button>
                  ))}
                  <button
                    type="button"
                    className={`chip ${selectedAddressId === 'NEW' ? 'active' : ''}`}
                    onClick={() => setSelectedAddressId('NEW')}
                  >
                    + New Address
                  </button>
                </div>
              </div>
            )}

            {activeSavedAddress ? (
              <div className="alert alert-info" style={{ marginBottom: 0 }}>
                <strong>{activeSavedAddress.recipientName}</strong> &middot; {activeSavedAddress.phone}
                <br />
                {activeSavedAddress.streetAddress}, {activeSavedAddress.city},{' '}
                {activeSavedAddress.state}, {activeSavedAddress.country}
              </div>
            ) : (
              <>
                <div className="field">
                  <label>Recipient Full Name *</label>
                  <input
                    required
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    placeholder="Full name"
                  />
                </div>
                <div className="field">
                  <label>Phone Number (WhatsApp / Courier Contact) *</label>
                  <input
                    required
                    value={recipientPhone}
                    onChange={(e) => setRecipientPhone(e.target.value)}
                    placeholder="+234 800 000 0000"
                  />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div className="field">
                    <label>Country *</label>
                    <input
                      required
                      value={shippingCountry}
                      onChange={(e) => setShippingCountry(e.target.value)}
                    />
                  </div>
                  <div className="field">
                    <label>State / Province *</label>
                    <input
                      required
                      value={shippingState}
                      onChange={(e) => setShippingState(e.target.value)}
                    />
                  </div>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div className="field">
                    <label>City *</label>
                    <input
                      required
                      value={shippingCity}
                      onChange={(e) => setShippingCity(e.target.value)}
                    />
                  </div>
                  <div className="field">
                    <label>LGA / District</label>
                    <input value={lga} onChange={(e) => setLga(e.target.value)} />
                  </div>
                </div>
                <div className="field">
                  <label>Street Address *</label>
                  <input
                    required
                    value={shippingAddress}
                    onChange={(e) => setShippingAddress(e.target.value)}
                    placeholder="House number, street, estate"
                  />
                </div>
                <div className="field">
                  <label>Nearest Landmark</label>
                  <input value={landmark} onChange={(e) => setLandmark(e.target.value)} />
                </div>
                <div className="field">
                  <label>Delivery Instructions</label>
                  <input
                    value={deliveryInstructions}
                    onChange={(e) => setDeliveryInstructions(e.target.value)}
                  />
                </div>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.88rem' }}>
                  <input
                    type="checkbox"
                    checked={saveForFuture}
                    onChange={(e) => setSaveForFuture(e.target.checked)}
                  />
                  Save this address to my account
                </label>
              </>
            )}
          </div>

          <div className="card" style={{ padding: 22, marginBottom: 20 }}>
            <h3>2. Delivery Zone</h3>
            {deliveryZones.length === 0 ? (
              <div className="alert alert-warning">
                No active delivery zones found in Firestore yet. Please contact store support on
                WhatsApp before placing an order.
              </div>
            ) : (
              <div className="field" style={{ marginBottom: 0 }}>
                <label>Select Delivery Zone *</label>
                <select
                  value={deliveryZoneId}
                  onChange={(e) => setDeliveryZoneId(e.target.value)}
                >
                  {deliveryZones.map((z) => (
                    <option key={z.id} value={z.id}>
                      {z.name} ({z.estimatedDays}) —{' '}
                      {qualifiesFree
                        ? 'FREE'
                        : formatMoney(z.feeNgn, currency, undefined, storeSettings.usdRateNgn)}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          <div className="card" style={{ padding: 22 }}>
            <h3>3. Payment Method</h3>
            <div className="chip-row" style={{ marginBottom: 14 }}>
              {storeSettings.bankTransferEnabled && (
                <button
                  type="button"
                  className={`chip ${paymentMethod === 'BANK_TRANSFER' ? 'active' : ''}`}
                  onClick={() => setPaymentMethod('BANK_TRANSFER')}
                >
                  Bank Transfer
                </button>
              )}
              {storeSettings.paystackEnabled && (
                <button
                  type="button"
                  className={`chip ${paymentMethod === 'PAYSTACK' ? 'active' : ''}`}
                  onClick={() => setPaymentMethod('PAYSTACK')}
                >
                  Paystack
                </button>
              )}
              {storeSettings.flutterwaveEnabled && (
                <button
                  type="button"
                  className={`chip ${paymentMethod === 'FLUTTERWAVE' ? 'active' : ''}`}
                  onClick={() => setPaymentMethod('FLUTTERWAVE')}
                >
                  Flutterwave
                </button>
              )}
              {storeSettings.codEnabled && (
                <button
                  type="button"
                  className={`chip ${paymentMethod === 'COD' ? 'active' : ''}`}
                  onClick={() => setPaymentMethod('COD')}
                >
                  Cash on Delivery
                </button>
              )}
            </div>

            {paymentMethod === 'BANK_TRANSFER' && (
              <div className="alert alert-info">
                <div>
                  <strong>Bank:</strong> {storeSettings.bankName}
                </div>
                <div>
                  <strong>Account Name:</strong> {storeSettings.accountName}
                </div>
                <div>
                  <strong>Account Number:</strong> {storeSettings.accountNumber}
                </div>
                <div className="field" style={{ marginTop: 10, marginBottom: 0 }}>
                  <label>Transfer Sender Name / Reference (if already paid)</label>
                  <input
                    value={transferNote}
                    onChange={(e) => setTransferNote(e.target.value)}
                    placeholder="e.g. GTBank transfer from Adaeze Ref #1234"
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="card" style={{ padding: 22 }}>
          <h3>Order Summary</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 14 }}>
            {cart.map((item) => (
              <div
                key={item.id}
                style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}
              >
                <span>
                  {item.quantity}x {item.productTitle} ({item.variantLabel})
                </span>
                <strong>
                  {formatMoney(
                    item.unitPriceNgn * item.quantity,
                    currency,
                    undefined,
                    storeSettings.usdRateNgn
                  )}
                </strong>
              </div>
            ))}
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid #D9D9E0', margin: '12px 0' }} />

          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
            <span>Subtotal</span>
            <strong>{formatMoney(subtotalNgn, currency, undefined, storeSettings.usdRateNgn)}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
            <span>Delivery ({selectedZone?.name || 'Zone'})</span>
            <strong>
              {qualifiesFree
                ? 'FREE'
                : formatMoney(deliveryFeeNgn, currency, undefined, storeSettings.usdRateNgn)}
            </strong>
          </div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              color: '#542080',
              fontWeight: 800,
              marginBottom: 16,
            }}
          >
            <span>Total</span>
            <span>{formatMoney(totalNgn, currency, undefined, storeSettings.usdRateNgn)}</span>
          </div>

          <div className="alert alert-warning" style={{ fontSize: '0.84rem' }}>
            {resolveEffectiveReturnPolicy(storeSettings.returnPolicy)}
          </div>

          <label
            style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: 8,
              fontSize: '0.86rem',
              marginBottom: 16,
            }}
          >
            <input
              type="checkbox"
              checked={ackNoReturns}
              onChange={(e) => setAckNoReturns(e.target.checked)}
            />
            <span>
              I have reviewed my sizing/item choices and understand ZEECROWN STORE&rsquo;s No-Returns
              &amp; No-Refunds policy.
            </span>
          </label>

          <button
            type="submit"
            className="btn"
            disabled={submitting || deliveryZones.length === 0}
            style={{ width: '100%' }}
          >
            {submitting ? 'Placing Order...' : 'Place Order'}
          </button>
        </div>
      </form>
    </div>
  );
}
