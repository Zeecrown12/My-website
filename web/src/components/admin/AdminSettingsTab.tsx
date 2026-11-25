import React, { useEffect, useState } from 'react';
import { doc, serverTimestamp, setDoc } from 'firebase/firestore';
import { db } from '../../lib/firebase';
import { OFFICIAL_STORE_EMAIL, OFFICIAL_WHATSAPP_DISPLAY } from '../../lib/formatters';
import { CurrencyCode, StoreSettings } from '../../types/store';
import { AdminActor, recordAdminAudit } from './adminHelpers';

interface AdminSettingsTabProps {
  actor: AdminActor;
  storeSettings: StoreSettings;
  onRefreshCatalog: () => Promise<void>;
  onStatus: (msg: string) => void;
}

export function AdminSettingsTab({
  actor,
  storeSettings,
  onRefreshCatalog,
  onStatus,
}: AdminSettingsTabProps) {
  const [storeName, setStoreName] = useState(storeSettings.storeName);
  const [tagline, setTagline] = useState(storeSettings.tagline);
  const [supportEmail, setSupportEmail] = useState(storeSettings.supportEmail || OFFICIAL_STORE_EMAIL);
  const [supportPhone, setSupportPhone] = useState(
    storeSettings.supportPhone || OFFICIAL_WHATSAPP_DISPLAY
  );
  const [whatsappNumber, setWhatsappNumber] = useState(
    storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY
  );
  const [businessAddress, setBusinessAddress] = useState(
    storeSettings.businessAddress || 'Lekki Phase 1, Lagos, Nigeria'
  );
  const [supportHours, setSupportHours] = useState(
    storeSettings.supportHours || 'Mon – Sat · 9:00 AM – 6:00 PM WAT'
  );
  const [defaultCurrency, setDefaultCurrency] = useState<CurrencyCode>(
    storeSettings.defaultCurrency
  );

  const [bankTransferEnabled, setBankTransferEnabled] = useState(
    storeSettings.bankTransferEnabled
  );
  const [bankName, setBankName] = useState(storeSettings.bankName);
  const [accountName, setAccountName] = useState(storeSettings.accountName);
  const [accountNumber, setAccountNumber] = useState(storeSettings.accountNumber);

  const [paystackEnabled, setPaystackEnabled] = useState(storeSettings.paystackEnabled);
  const [paystackPublicKey, setPaystackPublicKey] = useState(
    storeSettings.paystackPublicKey || ''
  );
  const [flutterwaveEnabled, setFlutterwaveEnabled] = useState(
    storeSettings.flutterwaveEnabled
  );
  const [flutterwavePublicKey, setFlutterwavePublicKey] = useState(
    storeSettings.flutterwavePublicKey || ''
  );
  const [codEnabled, setCodEnabled] = useState(storeSettings.codEnabled);
  const [guestCheckoutEnabled, setGuestCheckoutEnabled] = useState(
    storeSettings.guestCheckoutEnabled !== false
  );

  const [maintenanceMode, setMaintenanceMode] = useState(
    storeSettings.maintenanceMode === true
  );
  const [maintenanceMessage, setMaintenanceMessage] = useState(
    storeSettings.maintenanceMessage || ''
  );

  const [instagramHandle, setInstagramHandle] = useState(
    storeSettings.instagramHandle || '@zeecrownstore'
  );
  const [instagramUrl, setInstagramUrl] = useState(storeSettings.instagramUrl || '');
  const [tiktokHandle, setTiktokHandle] = useState(
    storeSettings.tiktokHandle || '@zeecrownstore'
  );
  const [tiktokUrl, setTiktokUrl] = useState(storeSettings.tiktokUrl || '');

  const [aboutText, setAboutText] = useState(storeSettings.aboutText);
  const [shippingPolicy, setShippingPolicy] = useState(storeSettings.shippingPolicy);
  const [returnPolicy, setReturnPolicy] = useState(storeSettings.returnPolicy);
  const [privacyPolicy, setPrivacyPolicy] = useState(storeSettings.privacyPolicy);
  const [termsPolicy, setTermsPolicy] = useState(storeSettings.termsPolicy);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setStoreName(storeSettings.storeName);
    setTagline(storeSettings.tagline);
    setSupportEmail(storeSettings.supportEmail || OFFICIAL_STORE_EMAIL);
    setSupportPhone(storeSettings.supportPhone || OFFICIAL_WHATSAPP_DISPLAY);
    setWhatsappNumber(storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY);
    setBusinessAddress(storeSettings.businessAddress || 'Lekki Phase 1, Lagos, Nigeria');
    setSupportHours(storeSettings.supportHours || 'Mon – Sat · 9:00 AM – 6:00 PM WAT');
    setDefaultCurrency(storeSettings.defaultCurrency);
    setBankTransferEnabled(storeSettings.bankTransferEnabled);
    setBankName(storeSettings.bankName);
    setAccountName(storeSettings.accountName);
    setAccountNumber(storeSettings.accountNumber);
    setPaystackEnabled(storeSettings.paystackEnabled);
    setPaystackPublicKey(storeSettings.paystackPublicKey || '');
    setFlutterwaveEnabled(storeSettings.flutterwaveEnabled);
    setFlutterwavePublicKey(storeSettings.flutterwavePublicKey || '');
    setCodEnabled(storeSettings.codEnabled);
    setGuestCheckoutEnabled(storeSettings.guestCheckoutEnabled !== false);
    setMaintenanceMode(storeSettings.maintenanceMode === true);
    setMaintenanceMessage(storeSettings.maintenanceMessage || '');
    setInstagramHandle(storeSettings.instagramHandle || '@zeecrownstore');
    setInstagramUrl(storeSettings.instagramUrl || '');
    setTiktokHandle(storeSettings.tiktokHandle || '@zeecrownstore');
    setTiktokUrl(storeSettings.tiktokUrl || '');
    setAboutText(storeSettings.aboutText);
    setShippingPolicy(storeSettings.shippingPolicy);
    setReturnPolicy(storeSettings.returnPolicy);
    setPrivacyPolicy(storeSettings.privacyPolicy);
    setTermsPolicy(storeSettings.termsPolicy);
  }, [storeSettings]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (storeName.trim().length < 2) {
      onStatus('Store name must be at least 2 characters.');
      return;
    }
    setSaving(true);
    try {
      await setDoc(
        doc(db, 'storeSettings', 'public'),
        {
          storeName: storeName.trim().slice(0, 80),
          tagline: tagline.trim().slice(0, 160),
          logoPlaceholderText: 'ZEECROWN',
          bankName: bankName.trim().slice(0, 80),
          accountName: accountName.trim().slice(0, 100),
          accountNumber: accountNumber.trim().slice(0, 30),
          paystackEnabled,
          paystackPublicKey: paystackPublicKey.trim().slice(0, 120),
          flutterwaveEnabled,
          flutterwavePublicKey: flutterwavePublicKey.trim().slice(0, 120),
          bankTransferEnabled,
          codEnabled,
          guestCheckoutEnabled,
          maintenanceMode,
          maintenanceMessage: maintenanceMessage.trim().slice(0, 300),
          supportEmail: (supportEmail.trim() || OFFICIAL_STORE_EMAIL).slice(0, 160),
          supportPhone: (supportPhone.trim() || OFFICIAL_WHATSAPP_DISPLAY).slice(0, 40),
          whatsappNumber: (whatsappNumber.trim() || OFFICIAL_WHATSAPP_DISPLAY).slice(0, 40),
          businessAddress: businessAddress.trim().slice(0, 240),
          supportHours: supportHours.trim().slice(0, 120),
          defaultCurrency,
          aboutText: aboutText.trim().slice(0, 1000),
          shippingPolicy: shippingPolicy.trim().slice(0, 2000),
          returnPolicy: returnPolicy.trim().slice(0, 2000),
          privacyPolicy: privacyPolicy.trim().slice(0, 2000),
          termsPolicy: termsPolicy.trim().slice(0, 2000),
          instagramHandle: instagramHandle.trim().slice(0, 80),
          instagramUrl: instagramUrl.trim().slice(0, 240),
          tiktokHandle: tiktokHandle.trim().slice(0, 80),
          tiktokUrl: tiktokUrl.trim().slice(0, 240),
          visibility: 'public',
          updatedAt: serverTimestamp(),
        },
        { merge: true }
      );
      await recordAdminAudit(
        actor,
        'UPDATE_STORE_SETTINGS',
        'storeSettings',
        'public',
        'Updated store profile, payment gateways, and store policies'
      );
      onStatus('Store settings and payment configuration saved to Firebase.');
      await onRefreshCatalog();
    } catch (err) {
      onStatus(err instanceof Error ? err.message : 'Unable to save store settings.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={(e) => void handleSave(e)} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="card" style={{ padding: 20 }}>
        <h3>Store Identity &amp; Official Contact</h3>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
            gap: 14,
          }}
        >
          <div className="field">
            <label>Store Name</label>
            <input required value={storeName} onChange={(e) => setStoreName(e.target.value)} />
          </div>
          <div className="field">
            <label>Brand Tagline</label>
            <input required value={tagline} onChange={(e) => setTagline(e.target.value)} />
          </div>
          <div className="field">
            <label>Official Support Email</label>
            <input
              type="email"
              value={supportEmail}
              onChange={(e) => setSupportEmail(e.target.value)}
            />
          </div>
          <div className="field">
            <label>Customer Care Phone</label>
            <input value={supportPhone} onChange={(e) => setSupportPhone(e.target.value)} />
          </div>
          <div className="field">
            <label>WhatsApp Number</label>
            <input value={whatsappNumber} onChange={(e) => setWhatsappNumber(e.target.value)} />
          </div>
          <div className="field">
            <label>Default Currency</label>
            <select
              value={defaultCurrency}
              onChange={(e) => setDefaultCurrency(e.target.value === 'USD' ? 'USD' : 'NGN')}
            >
              <option value="NGN">NGN (₦)</option>
              <option value="USD">USD ($)</option>
            </select>
          </div>
          <div className="field">
            <label>Studio / Business Address</label>
            <input value={businessAddress} onChange={(e) => setBusinessAddress(e.target.value)} />
          </div>
          <div className="field">
            <label>Support Hours</label>
            <input value={supportHours} onChange={(e) => setSupportHours(e.target.value)} />
          </div>
        </div>
      </div>

      <div className="card" style={{ padding: 20 }}>
        <h3>Payment Methods &amp; Gateways (Spark-Compatible)</h3>
        <p style={{ color: '#5C4B6E', fontSize: '0.88rem', marginTop: 0 }}>
          Enter only <strong>public</strong> gateway keys here (never paste secret keys).
          Paystack and Flutterwave launch inline client checkout when a public key is saved;
          third-party gateway transaction charges apply per provider.
        </p>

        <div className="chip-row" style={{ marginBottom: 16 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}>
            <input
              type="checkbox"
              checked={bankTransferEnabled}
              onChange={(e) => setBankTransferEnabled(e.target.checked)}
            />
            Bank Transfer
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}>
            <input
              type="checkbox"
              checked={paystackEnabled}
              onChange={(e) => setPaystackEnabled(e.target.checked)}
            />
            Paystack
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}>
            <input
              type="checkbox"
              checked={flutterwaveEnabled}
              onChange={(e) => setFlutterwaveEnabled(e.target.checked)}
            />
            Flutterwave
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}>
            <input
              type="checkbox"
              checked={codEnabled}
              onChange={(e) => setCodEnabled(e.target.checked)}
            />
            Cash on Delivery
          </label>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
            gap: 14,
          }}
        >
          <div className="field">
            <label>Bank Name</label>
            <input value={bankName} onChange={(e) => setBankName(e.target.value)} />
          </div>
          <div className="field">
            <label>Bank Account Name</label>
            <input value={accountName} onChange={(e) => setAccountName(e.target.value)} />
          </div>
          <div className="field">
            <label>Bank Account Number</label>
            <input value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} />
          </div>
          <div className="field">
            <label>Paystack Public Key (pk_live_... or pk_test_...)</label>
            <input
              value={paystackPublicKey}
              onChange={(e) => setPaystackPublicKey(e.target.value)}
              placeholder="pk_live_..."
            />
          </div>
          <div className="field">
            <label>Flutterwave Public Key (FLWPUBK_...)</label>
            <input
              value={flutterwavePublicKey}
              onChange={(e) => setFlutterwavePublicKey(e.target.value)}
              placeholder="FLWPUBK-..."
            />
          </div>
        </div>
      </div>

      <div className="card" style={{ padding: 20 }}>
        <h3>Social Links &amp; Policies</h3>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
            gap: 14,
            marginBottom: 14,
          }}
        >
          <div className="field">
            <label>Instagram Handle</label>
            <input value={instagramHandle} onChange={(e) => setInstagramHandle(e.target.value)} />
          </div>
          <div className="field">
            <label>Instagram URL</label>
            <input
              value={instagramUrl}
              onChange={(e) => setInstagramUrl(e.target.value)}
              placeholder="https://instagram.com/..."
            />
          </div>
          <div className="field">
            <label>TikTok Handle</label>
            <input value={tiktokHandle} onChange={(e) => setTiktokHandle(e.target.value)} />
          </div>
          <div className="field">
            <label>TikTok URL</label>
            <input
              value={tiktokUrl}
              onChange={(e) => setTiktokUrl(e.target.value)}
              placeholder="https://tiktok.com/@..."
            />
          </div>
        </div>

        <div className="field">
          <label>About ZEECROWN STORE</label>
          <textarea rows={2} value={aboutText} onChange={(e) => setAboutText(e.target.value)} />
        </div>
        <div className="field">
          <label>Shipping Policy</label>
          <textarea
            rows={2}
            value={shippingPolicy}
            onChange={(e) => setShippingPolicy(e.target.value)}
          />
        </div>
        <div className="field">
          <label>No-Returns / Refunds Policy</label>
          <textarea
            rows={2}
            value={returnPolicy}
            onChange={(e) => setReturnPolicy(e.target.value)}
          />
        </div>
        <div className="field">
          <label>Privacy Policy</label>
          <textarea
            rows={2}
            value={privacyPolicy}
            onChange={(e) => setPrivacyPolicy(e.target.value)}
          />
        </div>
        <div className="field">
          <label>Terms of Service</label>
          <textarea rows={2} value={termsPolicy} onChange={(e) => setTermsPolicy(e.target.value)} />
        </div>

        <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
          <input
            type="checkbox"
            checked={maintenanceMode}
            onChange={(e) => setMaintenanceMode(e.target.checked)}
          />
          Enable Maintenance Notice Banner
        </label>
        {maintenanceMode && (
          <div className="field">
            <label>Maintenance Message</label>
            <input
              value={maintenanceMessage}
              onChange={(e) => setMaintenanceMessage(e.target.value)}
              placeholder="We are refreshing our boutique..."
            />
          </div>
        )}

        <button type="submit" className="btn" disabled={saving}>
          {saving ? 'Saving Settings...' : 'Save Store & Payment Settings'}
        </button>
      </div>
    </form>
  );
}
