import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { StoreProvider } from './context/StoreContext';
import { Layout } from './components/Layout';
import { HomePage } from './pages/HomePage';
import { ShopPage } from './pages/ShopPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { CartPage } from './pages/CartPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { AuthPage } from './pages/AuthPage';
import { AccountPage } from './pages/AccountPage';
import { TrackOrderPage } from './pages/TrackOrderPage';
import { AdminPage } from './pages/AdminPage';
import {
  AboutPage,
  ContactPage,
  NotFoundPage,
  ReturnsPolicyPage,
  ShippingPolicyPage,
} from './pages/PolicyPages';

export function App() {
  return (
    <BrowserRouter>
      <StoreProvider>
        <Layout>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/shop" element={<ShopPage />} />
            <Route path="/category/:categorySlug" element={<ShopPage />} />
            <Route path="/collection/:collectionSlug" element={<ShopPage />} />
            <Route path="/product/:slugOrId" element={<ProductDetailPage />} />
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/login" element={<AuthPage initialMode="login" />} />
            <Route path="/register" element={<AuthPage initialMode="register" />} />
            <Route path="/account" element={<AccountPage />} />
            <Route path="/track" element={<TrackOrderPage />} />
            <Route path="/admin" element={<AdminPage />} />
            <Route path="/contact" element={<ContactPage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/shipping-policy" element={<ShippingPolicyPage />} />
            <Route path="/returns-policy" element={<ReturnsPolicyPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Layout>
      </StoreProvider>
    </BrowserRouter>
  );
}
export default App;
