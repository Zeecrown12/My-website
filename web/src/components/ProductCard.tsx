import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Heart, ShoppingBag } from 'lucide-react';
import { Product } from '../types/store';
import { useStore } from '../context/StoreContext';
import { formatMoney } from '../lib/formatters';

export function ProductCard({ product }: { product: Product }) {
  const {
    currency,
    storeSettings,
    categories,
    inventoryByProduct,
    addToCart,
    toggleWishlist,
    isWishlisted,
  } = useStore();
  const [addedFeedback, setAddedFeedback] = useState(false);

  const img = product.mainImageUrl || product.imageUrls[0] || '/assets/zeecrown-logo.jpg';
  const categoryName = categories.find((c) => c.id === product.categoryId)?.name || 'Boutique Piece';
  const inv = inventoryByProduct[product.id];
  const outOfStock = inv ? !inv.inStock || inv.stockQuantity <= 0 : false;
  const wished = isWishlisted(product.id);
  const discountPct =
    product.compareAtPriceNgn && product.compareAtPriceNgn > product.priceNgn
      ? Math.round(((product.compareAtPriceNgn - product.priceNgn) / product.compareAtPriceNgn) * 100)
      : 0;

  const defaultVariant = [product.sizes[0], product.colors[0]].filter(Boolean).join(' · ') || 'Standard';

  const handleQuickAdd = async () => {
    if (outOfStock) return;
    await addToCart(product, defaultVariant, 1);
    setAddedFeedback(true);
    setTimeout(() => setAddedFeedback(false), 1600);
  };

  return (
    <article className="product-card">
      <div className="product-img-wrap">
        <Link to={`/product/${encodeURIComponent(product.slug || product.id)}`}>
          <img src={img} alt={product.title} loading="lazy" />
        </Link>
        <div className="product-badges">
          {product.featured && <span className="tag-badge">Featured</span>}
          {product.newArrival && <span className="tag-badge">New</span>}
          {discountPct > 0 && <span className="tag-badge sale">-{discountPct}%</span>}
          {outOfStock && <span className="tag-badge">Sold Out</span>}
        </div>
        <button
          type="button"
          className="wish-icon-btn"
          onClick={() => void toggleWishlist(product)}
          aria-label={wished ? 'Remove from wishlist' : 'Save to wishlist'}
          title={wished ? 'Remove from wishlist' : 'Save to wishlist'}
        >
          <Heart
            size={18}
            fill={wished ? '#542080' : 'none'}
            color={wished ? '#542080' : '#5C4B6E'}
          />
        </button>
      </div>

      <div className="product-body">
        <span className="product-cat">{categoryName}</span>
        <Link
          to={`/product/${encodeURIComponent(product.slug || product.id)}`}
          className="product-title"
        >
          {product.title}
        </Link>
        <div className="price-row">
          <span className="price-now">
            {formatMoney(product.priceNgn, currency, product.priceUsd, storeSettings.usdRateNgn)}
          </span>
          {product.compareAtPriceNgn && product.compareAtPriceNgn > product.priceNgn && (
            <span className="price-was">
              {formatMoney(product.compareAtPriceNgn, currency, undefined, storeSettings.usdRateNgn)}
            </span>
          )}
        </div>

        <div style={{ marginTop: 'auto', paddingTop: 8, display: 'flex', gap: 8 }}>
          <button
            type="button"
            className="btn"
            style={{ width: '100%', padding: '9px 14px', fontSize: '0.84rem' }}
            disabled={outOfStock}
            onClick={() => void handleQuickAdd()}
          >
            <ShoppingBag size={15} />
            {outOfStock ? 'Out of Stock' : addedFeedback ? 'Added to Bag' : 'Add to Bag'}
          </button>
        </div>
      </div>
    </article>
  );
}
