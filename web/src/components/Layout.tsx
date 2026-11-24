import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import {
  MessageCircle,
  Search,
  ShieldCheck,
  ShoppingBag,
  User as UserIcon,
} from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { buildWhatsAppLink, OFFICIAL_WHATSAPP_DISPLAY } from '../lib/formatters';
import { CustomerCareHub } from './CustomerCareHub';

export function Layout({ children }: { children: React.ReactNode }) {
  const {
    user,
    isAdmin,
    currency,
    setCurrency,
    cart,
    storeSettings,
    catalogError,
  } = useStore();
  const [searchTerm, setSearchTerm] = useState('');
  const navigate = useNavigate();

  const cartCount = cart.reduce((sum, item) => sum + item.quantity, 0);
  const waUrl = buildWhatsAppLink(storeSettings.whatsappNumber);

  const onSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const q = searchTerm.trim();
    navigate(q ? `/shop?q=${encodeURIComponent(q)}` : '/shop');
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div className="zc-announcement">
        {storeSettings.storeName} &middot; {storeSettings.tagline} &middot; Official WhatsApp:{' '}
        <a
          href={waUrl}
          target="_blank"
          rel="noopener noreferrer"
          style={{ color: '#FFFFFF', textDecoration: 'underline', fontWeight: 700 }}
        >
          {storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}
        </a>
      </div>

      <header className="zc-header">
        <div className="zc-header-inner">
          <Link to="/" className="zc-brand">
            <img src="/assets/zeecrown-logo.jpg" alt="ZEECROWN STORE Crest" />
            <span>{storeSettings.storeName}</span>
          </Link>

          <nav className="zc-nav" aria-label="Main navigation">
            <NavLink to="/" end>
              Home
            </NavLink>
            <NavLink to="/shop">Shop</NavLink>
            <NavLink to="/track">Track Order</NavLink>
            <NavLink to="/about">About</NavLink>
            <NavLink to="/contact">Contact</NavLink>
            {isAdmin && (
              <NavLink to="/admin" style={{ color: '#542080' }}>
                <ShieldCheck size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                Admin
              </NavLink>
            )}
          </nav>

          <form
            onSubmit={onSearchSubmit}
            style={{
              display: 'flex',
              alignItems: 'center',
              border: '1px solid #D9D9E0',
              borderRadius: '999px',
              padding: '4px 10px',
              background: '#F4EFF9',
              minWidth: 200,
            }}
          >
            <Search size={15} color="#5C4B6E" />
            <input
              type="search"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search abayas, slippers..."
              aria-label="Search products"
              style={{
                border: 'none',
                background: 'transparent',
                outline: 'none',
                padding: '4px 8px',
                fontSize: '0.86rem',
                width: '100%',
              }}
            />
          </form>

          <div className="zc-actions">
            <button
              type="button"
              className="zc-pill-btn"
              onClick={() => setCurrency(currency === 'NGN' ? 'USD' : 'NGN')}
              title="Switch currency"
            >
              {currency === 'NGN' ? '₦ NGN' : '$ USD'}
            </button>

            <a
              href={waUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="zc-pill-btn whatsapp"
            >
              <MessageCircle size={15} />
              WhatsApp
            </a>

            <Link to="/cart" className="zc-pill-btn">
              <ShoppingBag size={15} />
              Bag
              {cartCount > 0 && <span className="zc-badge">{cartCount}</span>}
            </Link>

            <Link to={user ? '/account' : '/login'} className="zc-pill-btn primary">
              <UserIcon size={15} />
              {user ? 'Account' : 'Sign In'}
            </Link>
          </div>
        </div>
      </header>

      {catalogError && (
        <div className="container" style={{ paddingTop: 16 }}>
          <div className="alert alert-warning">
            <strong>Live Firebase Notice:</strong> {catalogError}
          </div>
        </div>
      )}

      <main style={{ flex: 1 }}>{children}</main>

      <footer className="zc-footer">
        <div className="zc-footer-grid">
          <div>
            <h4>{storeSettings.storeName}</h4>
            <p style={{ fontSize: '0.88rem', lineHeight: 1.6, margin: '0 0 10px' }}>
              {storeSettings.tagline}. Handcrafted modest couture, Adire silk garments, and artisan
              leatherwear delivered nationwide &amp; globally.
            </p>
          </div>

          <div>
            <h4>Shop &amp; Orders</h4>
            <Link to="/shop">All Collections</Link>
            <Link to="/cart">Shopping Bag</Link>
            <Link to="/track">Track Order</Link>
            <Link to="/account">My Account</Link>
            {isAdmin && <Link to="/admin">Admin Dashboard</Link>}
          </div>

          <div>
            <h4>Store Policies</h4>
            <Link to="/about">About ZEECROWN STORE</Link>
            <Link to="/shipping-policy">Delivery &amp; Shipping</Link>
            <Link to="/returns-policy">No-Returns &amp; Store Policy</Link>
            <Link to="/contact">Contact Us</Link>
          </div>

          <div>
            <h4>Customer Care</h4>
            <a href={waUrl} target="_blank" rel="noopener noreferrer">
              WhatsApp: {storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}
            </a>
            {storeSettings.supportEmail && (
              <a href={`mailto:${storeSettings.supportEmail}`}>{storeSettings.supportEmail}</a>
            )}
            <p style={{ fontSize: '0.8rem', color: '#E5D2A0', marginTop: 10 }}>
              Note: ZEECROWN STORE currently does not accept customer returns or refunds. Please
              confirm sizing via our Size Guides or Customer Care before ordering.
            </p>
          </div>
        </div>
      </footer>

      <CustomerCareHub />
    </div>
  );
}
