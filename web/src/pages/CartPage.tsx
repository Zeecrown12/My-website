import { Link } from 'react-router-dom';
import { ArrowRight, Minus, Plus, Trash2 } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { formatMoney, resolveProductImageUrl } from '../lib/formatters';

export function CartPage() {
  const {
    cart,
    currency,
    storeSettings,
    updateCartQty,
    removeFromCart,
    clearCart,
  } = useStore();

  const subtotalNgn = cart.reduce((sum, item) => sum + item.unitPriceNgn * item.quantity, 0);

  return (
    <div className="container section">
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 12,
          marginBottom: 24,
        }}
      >
        <h1 style={{ margin: 0 }}>Your Shopping Bag</h1>
        {cart.length > 0 && (
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => void clearCart()}
          >
            Clear Bag
          </button>
        )}
      </div>

      {cart.length === 0 ? (
        <div className="card" style={{ padding: 36, textAlign: 'center' }}>
          <h3>Your Shopping Bag Is Empty</h3>
          <p style={{ color: '#5C4B6E' }}>
            Explore our latest Abayas, Adire silk garments, handcrafted slippers, and handbags.
          </p>
          <Link to="/shop" className="btn">
            Explore Store Catalogue
          </Link>
        </div>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: 24,
            alignItems: 'start',
          }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {cart.map((item) => (
              <div
                key={item.id}
                className="card"
                style={{
                  padding: 14,
                  display: 'flex',
                  gap: 14,
                  alignItems: 'center',
                  flexWrap: 'wrap',
                }}
              >
                <img
                  src={resolveProductImageUrl(item.productImageUrl)}
                  alt={item.productTitle}
                  style={{
                    width: 80,
                    height: 96,
                    objectFit: 'cover',
                    borderRadius: 10,
                    border: '1px solid #D9D9E0',
                  }}
                />
                <div style={{ flex: '1 1 180px' }}>
                  <strong style={{ display: 'block', fontSize: '1rem' }}>{item.productTitle}</strong>
                  <span style={{ display: 'block', fontSize: '0.84rem', color: '#5C4B6E' }}>
                    Variant: {item.variantLabel}
                  </span>
                  <span style={{ display: 'block', fontWeight: 700, color: '#542080', marginTop: 4 }}>
                    {formatMoney(item.unitPriceNgn, currency, undefined, storeSettings.usdRateNgn)}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => void updateCartQty(item.id, item.quantity - 1)}
                    aria-label="Decrease quantity"
                  >
                    <Minus size={14} />
                  </button>
                  <strong style={{ minWidth: 24, textAlign: 'center' }}>{item.quantity}</strong>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => void updateCartQty(item.id, item.quantity + 1)}
                    aria-label="Increase quantity"
                  >
                    <Plus size={14} />
                  </button>
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => void removeFromCart(item.id)}
                    aria-label="Remove item"
                    style={{ color: '#9E2A3B' }}
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="card" style={{ padding: 22 }}>
            <h3>Order Summary</h3>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                marginBottom: 10,
                fontSize: '0.95rem',
              }}
            >
              <span>Subtotal</span>
              <strong>{formatMoney(subtotalNgn, currency, undefined, storeSettings.usdRateNgn)}</strong>
            </div>
            <p style={{ fontSize: '0.84rem', color: '#5C4B6E', marginBottom: 18 }}>
              Delivery fee is calculated on the Checkout step based on your chosen delivery zone.
            </p>
            <Link to="/checkout" className="btn" style={{ width: '100%' }}>
              Proceed to Checkout <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
