import { Link } from 'react-router-dom';
import { ArrowRight, MessageCircle, ShieldCheck, Sparkles, Truck } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { ProductCard } from '../components/ProductCard';
import { buildWhatsAppLink, OFFICIAL_WHATSAPP_DISPLAY } from '../lib/formatters';

export function HomePage() {
  const {
    products,
    categories,
    collections,
    storeSettings,
    catalogLoading,
  } = useStore();

  const featuredProducts = products.filter((p) => p.featured).slice(0, 8);
  const newArrivals = products.filter((p) => p.newArrival).slice(0, 4);
  const fallbackGrid = featuredProducts.length > 0 ? featuredProducts : products.slice(0, 8);
  const waUrl = buildWhatsAppLink(storeSettings.whatsappNumber);

  return (
    <div>
      <section className="zc-hero">
        <div className="zc-hero-grid">
          <div>
            <span className="zc-hero-eyebrow">Nigerian &amp; Global Luxury Fashion</span>
            <h1>{storeSettings.tagline || 'Style That Speaks. Elegance That Lasts.'}</h1>
            <p>
              Discover curated modest Abayas, hand-dyed Adire silk pieces, handcrafted leather
              slippers, and signature boutique handbags — crafted for timeless presence and
              delivered worldwide.
            </p>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <Link to="/shop" className="btn btn-gold">
                Shop Collection <ArrowRight size={16} />
              </Link>
              <a
                href={waUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-wa"
              >
                <MessageCircle size={16} /> Chat with Us on WhatsApp
              </a>
              <Link
                to="/track"
                className="btn btn-outline"
                style={{ background: 'transparent', color: '#FFFFFF', borderColor: '#E5D2A0' }}
              >
                Track Your Order
              </Link>
            </div>
          </div>

          <div className="zc-hero-crest">
            <img src="/assets/zeecrown-logo.jpg" alt="ZEECROWN STORE Brand Crest" />
            <h3 style={{ color: '#E5D2A0', marginTop: 14, marginBottom: 4 }}>
              {storeSettings.storeName}
            </h3>
            <p style={{ margin: 0, fontSize: '0.88rem', color: '#E9DDF5' }}>
              Synced live with Firebase Catalogue, Customer Accounts &amp; Order Tracking.
            </p>
          </div>
        </div>
      </section>

      <section className="section section-lilac">
        <div className="container">
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
              gap: 16,
            }}
          >
            <div className="card" style={{ padding: 18 }}>
              <Truck size={22} color="#542080" />
              <h4 style={{ margin: '10px 0 6px' }}>Nationwide &amp; Global Delivery</h4>
              <p style={{ margin: 0, fontSize: '0.86rem', color: '#5C4B6E' }}>
                Live courier tracking across Lagos, Abuja, Port Harcourt, and international destinations.
              </p>
            </div>
            <div className="card" style={{ padding: 18 }}>
              <ShieldCheck size={22} color="#542080" />
              <h4 style={{ margin: '10px 0 6px' }}>Verified Boutique Quality</h4>
              <p style={{ margin: 0, fontSize: '0.86rem', color: '#5C4B6E' }}>
                Please review our interactive Size Guides before ordering. All sales are final under
                our No-Returns policy.
              </p>
            </div>
            <div className="card" style={{ padding: 18 }}>
              <Sparkles size={22} color="#542080" />
              <h4 style={{ margin: '10px 0 6px' }}>Personal Customer Care</h4>
              <p style={{ margin: 0, fontSize: '0.86rem', color: '#5C4B6E' }}>
                Need help picking a size or confirming an order? Tap &ldquo;Need Help?&rdquo; or
                WhatsApp {storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}.
              </p>
            </div>
          </div>
        </div>
      </section>

      {categories.length > 0 && (
        <section className="section">
          <div className="container">
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'baseline',
                marginBottom: 18,
              }}
            >
              <h2>Shop by Category</h2>
              <Link to="/shop" style={{ color: '#542080', fontWeight: 600, textDecoration: 'none' }}>
                View All →
              </Link>
            </div>
            <div className="chip-row">
              {categories.map((cat) => (
                <Link
                  key={cat.id}
                  to={`/category/${encodeURIComponent(cat.slug || cat.id)}`}
                  className="chip"
                  style={{ textDecoration: 'none' }}
                >
                  {cat.name}
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {collections.length > 0 && (
        <section className="section section-lilac">
          <div className="container">
            <h2 style={{ marginBottom: 18 }}>Signature Collections</h2>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                gap: 16,
              }}
            >
              {collections.map((col) => (
                <Link
                  key={col.id}
                  to={`/collection/${encodeURIComponent(col.slug || col.id)}`}
                  className="card"
                  style={{ padding: 20, textDecoration: 'none' }}
                >
                  <span className="tag-badge" style={{ marginBottom: 8, display: 'inline-block' }}>
                    Collection
                  </span>
                  <h3 style={{ marginBottom: 6 }}>{col.name}</h3>
                  <p style={{ margin: 0, color: '#5C4B6E', fontSize: '0.9rem' }}>{col.tagline}</p>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      <section className="section">
        <div className="container">
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'baseline',
              marginBottom: 18,
            }}
          >
            <h2>Featured Luxury Pieces</h2>
            <Link to="/shop" style={{ color: '#542080', fontWeight: 600, textDecoration: 'none' }}>
              Browse Catalogue →
            </Link>
          </div>

          {catalogLoading ? (
            <div className="alert alert-info">Loading live ZEECROWN STORE catalogue...</div>
          ) : fallbackGrid.length === 0 ? (
            <div className="card" style={{ padding: 28, textAlign: 'center' }}>
              <h3>Catalogue Coming Soon</h3>
              <p style={{ color: '#5C4B6E' }}>
                No active products have been published yet. Administrators can publish pieces via
                the Admin Dashboard or Android App.
              </p>
              <a href={waUrl} target="_blank" rel="noopener noreferrer" className="btn btn-wa">
                Enquire on WhatsApp ({storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY})
              </a>
            </div>
          ) : (
            <div className="product-grid">
              {fallbackGrid.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </div>
      </section>

      {newArrivals.length > 0 && (
        <section className="section section-lilac">
          <div className="container">
            <h2 style={{ marginBottom: 18 }}>Latest Arrivals</h2>
            <div className="product-grid">
              {newArrivals.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
