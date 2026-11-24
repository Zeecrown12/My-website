import { useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';
import { useStore } from '../context/StoreContext';
import { ProductCard } from '../components/ProductCard';

type SortOption = 'featured' | 'price_asc' | 'price_desc' | 'newest';

export function ShopPage() {
  const { categorySlug, collectionSlug } = useParams<{
    categorySlug?: string;
    collectionSlug?: string;
  }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') || '';

  const { products, categories, collections, catalogLoading } = useStore();
  const [queryText, setQueryText] = useState(initialQuery);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>(() => {
    if (!categorySlug) return 'ALL';
    const match = categories.find(
      (c) => c.slug === categorySlug || c.id === categorySlug
    );
    return match?.id || categorySlug;
  });
  const [sortBy, setSortBy] = useState<SortOption>('featured');

  const activeCategoryFromRoute = useMemo(() => {
    if (!categorySlug) return null;
    return categories.find((c) => c.slug === categorySlug || c.id === categorySlug) || null;
  }, [categorySlug, categories]);

  const activeCollectionFromRoute = useMemo(() => {
    if (!collectionSlug) return null;
    return collections.find((c) => c.slug === collectionSlug || c.id === collectionSlug) || null;
  }, [collectionSlug, collections]);

  const effectiveCategoryId = activeCategoryFromRoute?.id || (categorySlug ? categorySlug : selectedCategoryId);

  const filteredProducts = useMemo(() => {
    const q = queryText.trim().toLowerCase();
    const list = products.filter((p) => {
      if (effectiveCategoryId !== 'ALL' && p.categoryId !== effectiveCategoryId) {
        return false;
      }
      if (activeCollectionFromRoute && p.collectionId !== activeCollectionFromRoute.id) {
        return false;
      }
      if (q) {
        const hay = [
          p.title,
          p.description,
          p.summary || '',
          p.fashionStyle || '',
          p.brand || '',
          ...p.colors,
          ...p.sizes,
          ...(p.tags || []),
        ]
          .join(' ')
          .toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });

    const sorted = [...list];
    if (sortBy === 'price_asc') {
      sorted.sort((a, b) => a.priceNgn - b.priceNgn);
    } else if (sortBy === 'price_desc') {
      sorted.sort((a, b) => b.priceNgn - a.priceNgn);
    } else if (sortBy === 'newest') {
      sorted.sort((a, b) => Number(Boolean(b.newArrival)) - Number(Boolean(a.newArrival)));
    } else {
      sorted.sort((a, b) => Number(b.featured) - Number(a.featured));
    }
    return sorted;
  }, [products, effectiveCategoryId, activeCollectionFromRoute, queryText, sortBy]);

  return (
    <div className="container section">
      <div style={{ marginBottom: 24 }}>
        <h1>
          {activeCategoryFromRoute
            ? activeCategoryFromRoute.name
            : activeCollectionFromRoute
            ? activeCollectionFromRoute.name
            : 'ZEECROWN STORE Catalogue'}
        </h1>
        <p style={{ color: '#5C4B6E', margin: 0 }}>
          {activeCategoryFromRoute?.description ||
            activeCollectionFromRoute?.tagline ||
            'Browse handcrafted Nigerian luxury fashion, Abayas, Adire silk sets, slippers, and boutique bags.'}
        </p>
      </div>

      <div
        className="card"
        style={{
          padding: 16,
          marginBottom: 24,
          display: 'flex',
          flexDirection: 'column',
          gap: 14,
        }}
      >
        <div
          style={{
            display: 'flex',
            gap: 12,
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              border: '1px solid #D9D9E0',
              borderRadius: '999px',
              padding: '6px 14px',
              flex: '1 1 260px',
              background: '#FFFCF5',
            }}
          >
            <Search size={16} color="#5C4B6E" />
            <input
              type="search"
              value={queryText}
              onChange={(e) => {
                const v = e.target.value;
                setQueryText(v);
                setSearchParams(v ? { q: v } : {});
              }}
              placeholder="Search by product name, colour, size, or occasion..."
              style={{ border: 'none', background: 'transparent', outline: 'none', width: '100%' }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <label htmlFor="sort-select" style={{ fontSize: '0.86rem', fontWeight: 600 }}>
              Sort:
            </label>
            <select
              id="sort-select"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as SortOption)}
              style={{
                padding: '8px 12px',
                borderRadius: 10,
                border: '1px solid #D9D9E0',
                background: '#FFFFFF',
              }}
            >
              <option value="featured">Featured First</option>
              <option value="newest">New Arrivals</option>
              <option value="price_asc">Price: Low to High</option>
              <option value="price_desc">Price: High to Low</option>
            </select>
          </div>
        </div>

        {!categorySlug && categories.length > 0 && (
          <div className="chip-row">
            <button
              type="button"
              className={`chip ${selectedCategoryId === 'ALL' ? 'active' : ''}`}
              onClick={() => setSelectedCategoryId('ALL')}
            >
              All ({products.length})
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                type="button"
                className={`chip ${selectedCategoryId === cat.id ? 'active' : ''}`}
                onClick={() => setSelectedCategoryId(cat.id)}
              >
                {cat.name}
              </button>
            ))}
          </div>
        )}
      </div>

      {catalogLoading ? (
        <div className="alert alert-info">Loading catalogue...</div>
      ) : filteredProducts.length === 0 ? (
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <h3>No Matching Pieces Found</h3>
          <p style={{ color: '#5C4B6E' }}>
            Try clearing your search filter or selecting a different category.
          </p>
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => {
              setQueryText('');
              setSelectedCategoryId('ALL');
              setSearchParams({});
            }}
          >
            Reset Filters
          </button>
        </div>
      ) : (
        <div className="product-grid">
          {filteredProducts.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
