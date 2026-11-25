import React, { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  collection,
  doc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  where,
} from 'firebase/firestore';
import { Heart, MessageCircle, Ruler, ShoppingBag, Star } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { db } from '../lib/firebase';
import { ProductReview } from '../types/store';
import {
  buildWhatsAppLink,
  formatMoney,
  resolveEffectiveReturnPolicy,
} from '../lib/formatters';
import { SizeGuideModal } from '../components/SizeGuideModal';
import { resolveVariantDetails } from '../lib/variants';

export function ProductDetailPage() {
  const { slugOrId } = useParams<{ slugOrId: string }>();
  const {
    user,
    products,
    categories,
    inventoryByProduct,
    sizeGuides,
    currency,
    storeSettings,
    addToCart,
    toggleWishlist,
    isWishlisted,
  } = useStore();

  const product = useMemo(
    () =>
      products.find(
        (p) => p.slug === slugOrId || p.id === slugOrId
      ) || null,
    [products, slugOrId]
  );

  const [activeImage, setActiveImage] = useState<string>('');
  const [selectedSize, setSelectedSize] = useState<string>('');
  const [selectedColor, setSelectedColor] = useState<string>('');
  const [qty, setQty] = useState(1);
  const [addedBanner, setAddedBanner] = useState(false);
  const [showSizeGuide, setShowSizeGuide] = useState(false);

  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewTitle, setReviewTitle] = useState('');
  const [reviewComment, setReviewComment] = useState('');
  const [reviewStatus, setReviewStatus] = useState<string | null>(null);

  useEffect(() => {
    if (!product) return;
    setActiveImage(product.mainImageUrl || product.imageUrls[0] || '/assets/zeecrown-logo.jpg');
    setSelectedSize(product.sizes[0] || 'One Size');
    setSelectedColor(product.colors[0] || 'Standard');

    void getDocs(query(collection(db, 'reviews'), where('visible', '==', true)))
      .then((snap) => {
        setReviews(
          snap.docs
            .map((d) => {
              const data = d.data();
              return {
                id: d.id,
                productId: String(data.productId || ''),
                userId: String(data.userId || ''),
                reviewerName: String(data.authorName || data.reviewerName || 'Verified Client'),
                rating: Number(data.rating || 5),
                title: typeof data.title === 'string' ? data.title : undefined,
                comment: String(data.comment || ''),
                approved: data.visible !== false && data.approved !== false,
                createdAt: data.createdAt,
              };
            })
            .filter((r) => r.productId === product.id && r.approved !== false)
        );
      })
      .catch(() => undefined);
  }, [product]);

  if (!product) {
    return (
      <div className="container section">
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <h2>Product Not Found</h2>
          <p style={{ color: '#5C4B6E' }}>
            This item may have moved or is currently unavailable.
          </p>
          <Link to="/shop" className="btn">
            Return to Shop
          </Link>
        </div>
      </div>
    );
  }

  const category = categories.find((c) => c.id === product.categoryId);
  const inv = inventoryByProduct[product.id];
  const resolvedVariant = resolveVariantDetails(
    product,
    inv,
    selectedSize,
    selectedColor,
    storeSettings.usdRateNgn
  );
  const variantLabel = resolvedVariant.key;
  const outOfStock = resolvedVariant.stockQuantity !== null ? resolvedVariant.stockQuantity <= 0 : false;
  const variantUnavailable = !resolvedVariant.available;

  const matchedSizeGuide =
    sizeGuides.find((g) => g.id === product.sizeGuideId) ||
    sizeGuides.find((g) => g.id === category?.sizeGuideId) ||
    sizeGuides.find((g) => g.assignedProductIds?.includes(product.id)) ||
    sizeGuides.find((g) => g.assignedCategoryIds?.includes(product.categoryId)) ||
    null;

  const images = Array.from(
    new Set([product.mainImageUrl, ...product.imageUrls].filter(Boolean) as string[])
  );
  if (images.length === 0) images.push('/assets/zeecrown-logo.jpg');

  const handleAdd = async () => {
    if (outOfStock || variantUnavailable) return;
    await addToCart(product, variantLabel, qty, resolvedVariant.priceNgn);
    setAddedBanner(true);
    setTimeout(() => setAddedBanner(false), 2500);
  };

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      setReviewStatus('Please sign in to post a review.');
      return;
    }
    if (!reviewComment.trim()) return;
    setReviewStatus('Submitting review...');
    try {
      const revId = `rev_${user.uid}_web_${product.id}_${Date.now()}`.slice(0, 120);
      const authorName = (user.displayName || user.email?.split('@')[0] || 'Client').slice(0, 80);
      const ratingInt = Math.min(5, Math.max(1, Math.round(reviewRating)));
      const cleanComment = reviewTitle.trim()
        ? `${reviewTitle.trim().slice(0, 120)} — ${reviewComment.trim()}`.slice(0, 1000)
        : reviewComment.trim().slice(0, 1000);
      await setDoc(doc(db, 'reviews', revId), {
        productId: product.id,
        userId: user.uid,
        authorName,
        rating: ratingInt,
        comment: cleanComment,
        visible: true,
        moderationStatus: 'APPROVED',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setReviews((prev) => [
        {
          id: revId,
          productId: product.id,
          userId: user.uid,
          reviewerName: authorName,
          rating: ratingInt,
          comment: cleanComment,
          approved: true,
        },
        ...prev,
      ]);
      setReviewTitle('');
      setReviewComment('');
      setReviewStatus('Thank you! Your review has been published.');
    } catch (err) {
      setReviewStatus(
        err instanceof Error ? err.message : 'Unable to save review right now.'
      );
    }
  };

  return (
    <div className="container section">
      <div style={{ fontSize: '0.86rem', color: '#5C4B6E', marginBottom: 18 }}>
        <Link to="/">Home</Link> / <Link to="/shop">Shop</Link>
        {category && (
          <>
            {' / '}
            <Link to={`/category/${encodeURIComponent(category.slug || category.id)}`}>
              {category.name}
            </Link>
          </>
        )}{' '}
        / <strong>{product.title}</strong>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: 32,
          alignItems: 'start',
        }}
      >
        <div>
          <div
            className="card"
            style={{ overflow: 'hidden', borderRadius: 20, marginBottom: 12 }}
          >
            <img
              src={activeImage || images[0]}
              alt={product.title}
              style={{ width: '100%', aspectRatio: '4 / 5', objectFit: 'cover', display: 'block' }}
            />
          </div>
          {images.length > 1 && (
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              {images.map((url) => (
                <button
                  key={url}
                  type="button"
                  onClick={() => setActiveImage(url)}
                  style={{
                    padding: 0,
                    border: activeImage === url ? '2px solid #542080' : '1px solid #D9D9E0',
                    borderRadius: 10,
                    overflow: 'hidden',
                    width: 72,
                    height: 86,
                    cursor: 'pointer',
                  }}
                >
                  <img
                    src={url}
                    alt=""
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        <div>
          {category && <span className="product-cat">{category.name}</span>}
          <h1 style={{ marginTop: 6, marginBottom: 10 }}>{product.title}</h1>

          <div className="price-row" style={{ marginBottom: 8 }}>
            <span className="price-now" style={{ fontSize: '1.5rem' }}>
              {formatMoney(
                resolvedVariant.priceNgn,
                currency,
                resolvedVariant.priceUsd,
                storeSettings.usdRateNgn
              )}
            </span>
            {product.compareAtPriceNgn && product.compareAtPriceNgn > resolvedVariant.priceNgn && (
              <span className="price-was">
                {formatMoney(product.compareAtPriceNgn, currency, undefined, storeSettings.usdRateNgn)}
              </span>
            )}
          </div>
          {resolvedVariant.sku && (
            <div style={{ fontSize: '0.8rem', color: '#5C4B6E', marginBottom: 14 }}>
              SKU: <strong>{resolvedVariant.sku}</strong>
            </div>
          )}

          <p style={{ color: '#1F122B', marginBottom: 16 }}>{product.description}</p>

          {product.features && product.features.length > 0 && (
            <ul style={{ paddingLeft: 20, color: '#5C4B6E', marginBottom: 18 }}>
              {product.features.map((f, idx) => (
                <li key={idx}>{f}</li>
              ))}
            </ul>
          )}

          {product.sizes.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: 8,
                }}
              >
                <strong style={{ fontSize: '0.88rem' }}>Size: {selectedSize}</strong>
                {matchedSizeGuide && (
                  <button
                    type="button"
                    className="zc-pill-btn"
                    onClick={() => setShowSizeGuide(true)}
                  >
                    <Ruler size={15} /> Size Guide
                  </button>
                )}
              </div>
              <div className="chip-row">
                {product.sizes.map((s) => (
                  <button
                    key={s}
                    type="button"
                    className={`chip ${selectedSize === s ? 'active' : ''}`}
                    onClick={() => setSelectedSize(s)}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {product.colors.length > 0 && (
            <div style={{ marginBottom: 18 }}>
              <strong style={{ display: 'block', fontSize: '0.88rem', marginBottom: 8 }}>
                Colour: {selectedColor}
              </strong>
              <div className="chip-row">
                {product.colors.map((c) => (
                  <button
                    key={c}
                    type="button"
                    className={`chip ${selectedColor === c ? 'active' : ''}`}
                    onClick={() => setSelectedColor(c)}
                  >
                    {c}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
            <label htmlFor="qty-input" style={{ fontWeight: 600, fontSize: '0.88rem' }}>
              Quantity:
            </label>
            <input
              id="qty-input"
              type="number"
              min={1}
              max={20}
              value={qty}
              onChange={(e) => setQty(Math.max(1, Math.min(20, Number(e.target.value) || 1)))}
              style={{
                width: 78,
                padding: '8px 10px',
                borderRadius: 10,
                border: '1px solid #D9D9E0',
              }}
            />
            {resolvedVariant.stockQuantity !== null && (
              <span style={{ fontSize: '0.84rem', color: outOfStock ? '#9E2A3B' : '#1D6B45', fontWeight: 600 }}>
                {outOfStock
                  ? 'Currently Out of Stock'
                  : `${resolvedVariant.stockQuantity} in stock${resolvedVariant.hasVariantStock ? ` (${variantLabel})` : ''}`}
              </span>
            )}
          </div>

          {variantUnavailable && (
            <div className="alert alert-warning">
              This specific size/colour combination is currently unavailable. Please choose another option.
            </div>
          )}

          {addedBanner && (
            <div className="alert alert-success">
              Added to your shopping bag! <Link to="/cart"><strong>View Bag →</strong></Link>
            </div>
          )}

          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 20 }}>
            <button
              type="button"
              className="btn"
              disabled={outOfStock || variantUnavailable}
              onClick={() => void handleAdd()}
              style={{ flex: '1 1 200px' }}
            >
              <ShoppingBag size={17} />
              {outOfStock ? 'Out of Stock' : 'Add to Bag'}
            </button>

            <button
              type="button"
              className="btn btn-outline"
              onClick={() => void toggleWishlist(product)}
            >
              <Heart
                size={17}
                fill={isWishlisted(product.id) ? '#542080' : 'none'}
              />
              {isWishlisted(product.id) ? 'Saved' : 'Save'}
            </button>

            <a
              href={buildWhatsAppLink(
                storeSettings.whatsappNumber,
                `Hello ZEECROWN STORE, I would like sizing/order details for: ${product.title}`
              )}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-wa"
            >
              <MessageCircle size={17} /> Ask on WhatsApp
            </a>
          </div>

          <div className="alert alert-info" style={{ marginBottom: 0 }}>
            <strong>Store Policy Reminder:</strong>{' '}
            {resolveEffectiveReturnPolicy(storeSettings.returnPolicy)}
          </div>
        </div>
      </div>

      <section style={{ marginTop: 48 }}>
        <h2>Customer Reviews ({reviews.length})</h2>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: 20,
          }}
        >
          <div>
            {reviews.length === 0 ? (
              <p style={{ color: '#5C4B6E' }}>No reviews yet for this piece.</p>
            ) : (
              reviews.map((rev) => (
                <div key={rev.id} className="card" style={{ padding: 16, marginBottom: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <strong>{rev.reviewerName}</strong>
                    <span style={{ color: '#8B6914', fontWeight: 700 }}>
                      {'★'.repeat(Math.max(1, Math.min(5, rev.rating)))}
                    </span>
                  </div>
                  {rev.title && <div style={{ fontWeight: 600, marginBottom: 4 }}>{rev.title}</div>}
                  <p style={{ margin: 0, color: '#5C4B6E', fontSize: '0.9rem' }}>{rev.comment}</p>
                </div>
              ))
            )}
          </div>

          <form onSubmit={(e) => void handleSubmitReview(e)} className="card" style={{ padding: 20 }}>
            <h4>Leave a Review</h4>
            {reviewStatus && <div className="alert alert-info">{reviewStatus}</div>}
            <div className="field">
              <label>Rating</label>
              <select
                value={reviewRating}
                onChange={(e) => setReviewRating(Number(e.target.value))}
              >
                {[5, 4, 3, 2, 1].map((n) => (
                  <option key={n} value={n}>
                    {n} <Star size={12} /> Stars
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Headline (Optional)</label>
              <input
                value={reviewTitle}
                onChange={(e) => setReviewTitle(e.target.value)}
                placeholder="Gorgeous drape and finishing"
              />
            </div>
            <div className="field">
              <label>Your Review</label>
              <textarea
                required
                rows={3}
                value={reviewComment}
                onChange={(e) => setReviewComment(e.target.value)}
                placeholder="Share your experience with quality and fit..."
              />
            </div>
            <button type="submit" className="btn">
              Publish Review
            </button>
          </form>
        </div>
      </section>

      {showSizeGuide && matchedSizeGuide && (
        <SizeGuideModal guide={matchedSizeGuide} onClose={() => setShowSizeGuide(false)} />
      )}
    </div>
  );
}
