import { MessageCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useStore } from '../context/StoreContext';
import {
  buildWhatsAppLink,
  formatMoney,
  OFFICIAL_WHATSAPP_DISPLAY,
  resolveEffectiveReturnPolicy,
} from '../lib/formatters';

export function ContactPage() {
  const { storeSettings } = useStore();
  const waUrl = buildWhatsAppLink(storeSettings.whatsappNumber);

  return (
    <div className="container section" style={{ maxWidth: 820 }}>
      <h1>Contact Us &amp; Customer Care</h1>
      <p style={{ color: '#5C4B6E' }}>
        Need help choosing a size, confirming stock, or following up on an order? Reach ZEECROWN
        STORE directly via WhatsApp or submit a support ticket in your account.
      </p>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
          gap: 18,
          marginTop: 20,
        }}
      >
        <div className="card" style={{ padding: 22 }}>
          <h3>Official WhatsApp Support</h3>
          <p style={{ color: '#5C4B6E', fontSize: '0.92rem' }}>
            Number: <strong>{storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}</strong>
          </p>
          <a href={waUrl} target="_blank" rel="noopener noreferrer" className="btn btn-wa">
            <MessageCircle size={17} /> Chat with Us on WhatsApp
          </a>
        </div>

        <div className="card" style={{ padding: 22 }}>
          <h3>Email &amp; Account Tickets</h3>
          {storeSettings.supportEmail && (
            <p style={{ fontSize: '0.92rem' }}>
              Email:{' '}
              <a href={`mailto:${storeSettings.supportEmail}`} style={{ color: '#542080', fontWeight: 700 }}>
                {storeSettings.supportEmail}
              </a>
            </p>
          )}
          <Link to="/account" className="btn btn-outline">
            Open Support Inbox in Account
          </Link>
        </div>
      </div>
    </div>
  );
}

export function AboutPage() {
  const { storeSettings } = useStore();
  return (
    <div className="container section" style={{ maxWidth: 820 }}>
      <h1>About {storeSettings.storeName}</h1>
      <div className="card" style={{ padding: 26 }}>
        <p style={{ fontSize: '1.02rem', marginTop: 0 }}>{storeSettings.aboutText}</p>
        <p style={{ color: '#5C4B6E', marginBottom: 0 }}>
          Every garment and leather piece is curated for enduring craftsmanship, modest sophistication,
          and tailored Nigerian &amp; global style.
        </p>
      </div>
    </div>
  );
}

export function ShippingPolicyPage() {
  const { deliveryZones, storeSettings, currency } = useStore();
  return (
    <div className="container section" style={{ maxWidth: 860 }}>
      <h1>Delivery &amp; Shipping Policy</h1>
      <div className="card" style={{ padding: 24, marginBottom: 22 }}>
        <p style={{ marginTop: 0 }}>{storeSettings.shippingPolicy}</p>
        {storeSettings.freeDeliveryThresholdNgn && storeSettings.freeDeliveryThresholdNgn > 0 && (
          <div className="alert alert-info" style={{ marginBottom: 0 }}>
            <strong>Free Delivery Offer:</strong> Orders from{' '}
            {formatMoney(
              storeSettings.freeDeliveryThresholdNgn,
              currency,
              undefined,
              storeSettings.usdRateNgn
            )}{' '}
            qualify for complimentary delivery.
          </div>
        )}
      </div>

      {deliveryZones.length > 0 && (
        <div className="card" style={{ padding: 20 }}>
          <h3>Active Delivery Zones</h3>
          <div className="table-wrap">
            <table className="zc-table">
              <thead>
                <tr>
                  <th>Zone</th>
                  <th>Region</th>
                  <th>Fee</th>
                  <th>Estimated Delivery</th>
                </tr>
              </thead>
              <tbody>
                {deliveryZones.map((z) => (
                  <tr key={z.id}>
                    <td>
                      <strong>{z.name}</strong>
                    </td>
                    <td>{z.region}</td>
                    <td>{formatMoney(z.feeNgn, currency, undefined, storeSettings.usdRateNgn)}</td>
                    <td>{z.estimatedDays}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

export function ReturnsPolicyPage() {
  const { storeSettings } = useStore();
  const waUrl = buildWhatsAppLink(storeSettings.whatsappNumber);
  return (
    <div className="container section" style={{ maxWidth: 820 }}>
      <h1>Store Return &amp; Refund Policy</h1>
      <div className="card" style={{ padding: 26 }}>
        <div className="alert alert-warning">
          <strong>Official Policy:</strong>{' '}
          {resolveEffectiveReturnPolicy(storeSettings.returnPolicy)}
        </div>
        <p style={{ color: '#5C4B6E' }}>
          We encourage every customer to review measurements in our Size Guides and ask our
          Customer Care specialists before completing checkout.
        </p>
        <a href={waUrl} target="_blank" rel="noopener noreferrer" className="btn btn-wa">
          <MessageCircle size={17} /> Confirm Sizing on WhatsApp (
          {storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY})
        </a>
      </div>
    </div>
  );
}

export function NotFoundPage() {
  return (
    <div className="container section">
      <div className="card" style={{ padding: 36, textAlign: 'center', maxWidth: 620, margin: '0 auto' }}>
        <p style={{ color: '#8A6A1C', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: 8 }}>
          404 &middot; Page Not Found
        </p>
        <h1 style={{ marginBottom: 12 }}>We couldn&apos;t find that boutique page</h1>
        <p style={{ color: '#5C4B6E', marginBottom: 22 }}>
          The collection or page link you followed may have moved. Browse our full collection or return home.
        </p>
        <div style={{ display: 'flex', justifyContent: 'center', gap: 12, flexWrap: 'wrap' }}>
          <Link to="/" className="btn btn-secondary">
            Return Home
          </Link>
          <Link to="/shop" className="btn">
            Browse All Pieces
          </Link>
        </div>
      </div>
    </div>
  );
}
