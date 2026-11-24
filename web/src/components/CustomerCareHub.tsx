import React, { useState } from 'react';
import { addDoc, collection, serverTimestamp } from 'firebase/firestore';
import { MessageCircle, Send, Sparkles, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useStore } from '../context/StoreContext';
import { db, FUNCTIONS_BASE_URL } from '../lib/firebase';
import {
  buildWhatsAppLink,
  formatMoney,
  OFFICIAL_WHATSAPP_DISPLAY,
  resolveEffectiveReturnPolicy,
} from '../lib/formatters';

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
}

type CareMode = 'SHOPPING' | 'SUPPORT';

export function CustomerCareHub() {
  const {
    user,
    products,
    deliveryZones,
    storeSettings,
    aiConfig,
    currency,
  } = useStore();

  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState<CareMode>('SHOPPING');
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [showTicketForm, setShowTicketForm] = useState(false);
  const [ticketSubject, setTicketSubject] = useState('');
  const [ticketMessage, setTicketMessage] = useState('');
  const [ticketStatus, setTicketStatus] = useState<string | null>(null);

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      role: 'assistant',
      text: `Welcome to ${storeSettings.storeName}. Choose Shopping Assistance, Customer Support, or Chat with Us on WhatsApp (${storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}).`,
    },
  ]);

  const waUrl = buildWhatsAppLink(storeSettings.whatsappNumber);

  const buildLocalGroundedReply = (query: string): string => {
    const q = query.toLowerCase();

    if (/return|refund|exchange|policy/.test(q)) {
      return `${resolveEffectiveReturnPolicy(storeSettings.returnPolicy)}\n\nWhatsApp Customer Care: ${storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}`;
    }

    if (/deliver|shipping|fee|lagos|abuja|zone/.test(q)) {
      if (deliveryZones.length === 0) {
        return `${storeSettings.shippingPolicy}\n\nCheck delivery zones during checkout or message us on WhatsApp (${storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}).`;
      }
      const zoneLines = deliveryZones
        .slice(0, 6)
        .map(
          (z) =>
            `• ${z.name}: ${formatMoney(z.feeNgn, currency, undefined, storeSettings.usdRateNgn)} (${z.estimatedDays})`
        )
        .join('\n');
      return `Live ZEECROWN STORE delivery zones:\n${zoneLines}`;
    }

    if (/pay|transfer|paystack|flutterwave|bank|cod/.test(q)) {
      const enabled: string[] = [];
      if (storeSettings.paystackEnabled) enabled.push('Paystack');
      if (storeSettings.flutterwaveEnabled) enabled.push('Flutterwave');
      if (storeSettings.bankTransferEnabled) enabled.push('Direct Bank Transfer');
      if (storeSettings.codEnabled) enabled.push('Cash on Delivery');
      return `Accepted payment methods: ${enabled.join(', ') || 'Standard Checkout'}. Both NGN (₦) and USD ($) currency displays are supported in the header.`;
    }

    if (/order|track|waybill/.test(q)) {
      return 'To check live order status securely, open the Track Order page with your Order Number and checkout email/phone (or tracking token), or sign in to view your Account Order History.';
    }

    const matches = products
      .filter((p) => {
        const hay = `${p.title} ${p.description} ${(p.tags || []).join(' ')} ${p.colors.join(' ')}`.toLowerCase();
        return q
          .split(/\s+/)
          .filter((w) => w.length >= 3)
          .some((w) => hay.includes(w));
      })
      .slice(0, 3);

    if (matches.length > 0) {
      const lines = matches
        .map(
          (p) =>
            `• ${p.title} — ${formatMoney(p.priceNgn, currency, p.priceUsd, storeSettings.usdRateNgn)} (Sizes: ${p.sizes.join(', ') || 'Standard'})`
        )
        .join('\n');
      return `Here are matching pieces from our live catalogue:\n${lines}\n\nTap Shop to filter or open any piece for full details and Size Guide.`;
    }

    return `I can help with product recommendations, sizing guides, delivery zones, payment options, order tracking, or store policies. For direct human assistance, click "Chat with Us on WhatsApp" (${storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}).`;
  };

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || sending) return;

    setInput('');
    setMessages((prev) => [...prev, { id: `u_${Date.now()}`, role: 'user', text: trimmed }]);
    setSending(true);

    try {
      const endpoint = FUNCTIONS_BASE_URL
        ? `${FUNCTIONS_BASE_URL}/aiAssistantChat`
        : '/api/ai/chat';
      const token = user ? await user.getIdToken().catch(() => null) : null;
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ query: trimmed, mode }),
      });
      if (res.ok) {
        const data = (await res.json()) as { replyText?: string };
        if (data.replyText) {
          setMessages((prev) => [
            ...prev,
            { id: `a_${Date.now()}`, role: 'assistant', text: data.replyText! },
          ]);
          return;
        }
      }
      throw new Error('Fallback to live Firestore-grounded local assistant');
    } catch {
      const reply = buildLocalGroundedReply(trimmed);
      setMessages((prev) => [
        ...prev,
        { id: `a_${Date.now()}`, role: 'assistant', text: reply },
      ]);
    } finally {
      setSending(false);
    }
  };

  const handleCreateSupportTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      setTicketStatus('Please sign in first to submit an account support ticket, or use WhatsApp.');
      return;
    }
    if (!ticketSubject.trim() || !ticketMessage.trim()) return;
    setTicketStatus('Submitting...');
    try {
      await addDoc(collection(db, 'supportEnquiries'), {
        userId: user.uid,
        customerName: (user.displayName || user.email?.split('@')[0] || 'Client').slice(0, 80),
        customerEmail: (user.email || '').slice(0, 200),
        category: mode === 'SHOPPING' ? 'PRODUCT_ENQUIRY' : 'CUSTOMER_SUPPORT',
        subject: ticketSubject.trim().slice(0, 140),
        message: ticketMessage.trim().slice(0, 1800),
        status: 'NEW',
        priority: 'NORMAL',
        sourceChannel: 'WEB_CARE_HUB',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setTicketSubject('');
      setTicketMessage('');
      setTicketStatus('Support ticket submitted! Our team will respond in your Account & Inbox.');
      setShowTicketForm(false);
    } catch (err) {
      setTicketStatus(
        err instanceof Error ? err.message : 'Could not submit ticket right now. Please try WhatsApp.'
      );
    }
  };

  if (!aiConfig.enabled) return null;

  return (
    <>
      <div className="zc-floating-stack">
        <a
          href={waUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="zc-pill-btn whatsapp"
          style={{ boxShadow: '0 10px 26px rgba(53,16,82,0.22)' }}
        >
          <MessageCircle size={16} />
          WhatsApp: {storeSettings.whatsappNumber || OFFICIAL_WHATSAPP_DISPLAY}
        </a>

        <button
          type="button"
          className="btn"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          style={{ boxShadow: '0 12px 28px rgba(53,16,82,0.28)' }}
        >
          <Sparkles size={16} />
          Need Help?
        </button>
      </div>

      {open && (
        <section className="zc-care-drawer" aria-label="ZEECROWN CUSTOMER CARE">
          <div
            style={{
              background: 'linear-gradient(135deg, #351052 0%, #542080 100%)',
              color: '#FFFFFF',
              padding: '14px 16px',
              borderBottom: '2px solid #C8A64D',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <div>
              <strong style={{ display: 'block', letterSpacing: '0.05em' }}>
                ZEECROWN CUSTOMER CARE
              </strong>
              <span style={{ fontSize: '0.78rem', color: '#E5D2A0' }}>
                Your shopping and customer support assistant.
              </span>
            </div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              aria-label="Close Customer Care"
              style={{
                background: 'transparent',
                border: '1px solid rgba(255,255,255,0.4)',
                color: '#FFFFFF',
                borderRadius: '999px',
                width: 32,
                height: 32,
                cursor: 'pointer',
              }}
            >
              <X size={16} />
            </button>
          </div>

          <div
            style={{
              padding: '8px 12px',
              background: '#F4EFF9',
              borderBottom: '1px solid #D9D9E0',
              display: 'flex',
              gap: 6,
              overflowX: 'auto',
            }}
          >
            <button
              type="button"
              className={`chip ${mode === 'SHOPPING' ? 'active' : ''}`}
              style={{ padding: '6px 11px', fontSize: '0.76rem', whiteSpace: 'nowrap' }}
              onClick={() => setMode('SHOPPING')}
            >
              Shopping Assistance
            </button>
            <button
              type="button"
              className={`chip ${mode === 'SUPPORT' ? 'active' : ''}`}
              style={{ padding: '6px 11px', fontSize: '0.76rem', whiteSpace: 'nowrap' }}
              onClick={() => setMode('SUPPORT')}
            >
              Customer Support
            </button>
            <a
              href={waUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="zc-pill-btn whatsapp"
              style={{ padding: '5px 11px', fontSize: '0.76rem', minHeight: 30, whiteSpace: 'nowrap' }}
            >
              Chat with Us on WhatsApp
            </a>
          </div>

          <div
            style={{
              flex: 1,
              overflowY: 'auto',
              padding: 14,
              background: '#FFFCF5',
              display: 'flex',
              flexDirection: 'column',
              gap: 10,
            }}
          >
            {messages.map((m) => (
              <div
                key={m.id}
                style={{
                  alignSelf: m.role === 'user' ? 'flex-end' : 'flex-start',
                  background: m.role === 'user' ? '#542080' : '#FFFFFF',
                  color: m.role === 'user' ? '#FFFFFF' : '#1F122B',
                  border: m.role === 'user' ? 'none' : '1px solid #D9D9E0',
                  borderRadius: 14,
                  padding: '9px 13px',
                  maxWidth: '88%',
                  fontSize: '0.88rem',
                  whiteSpace: 'pre-line',
                }}
              >
                {m.text}
              </div>
            ))}

            {ticketStatus && (
              <div className="alert alert-info" style={{ fontSize: '0.82rem', marginBottom: 0 }}>
                {ticketStatus}
              </div>
            )}

            {showTicketForm && (
              <form
                onSubmit={(e) => void handleCreateSupportTicket(e)}
                className="card"
                style={{ padding: 12 }}
              >
                <div className="field" style={{ marginBottom: 8 }}>
                  <label>Subject</label>
                  <input
                    required
                    value={ticketSubject}
                    onChange={(e) => setTicketSubject(e.target.value)}
                    placeholder="Sizing help / Order inquiry"
                  />
                </div>
                <div className="field" style={{ marginBottom: 8 }}>
                  <label>Message</label>
                  <textarea
                    required
                    rows={3}
                    value={ticketMessage}
                    onChange={(e) => setTicketMessage(e.target.value)}
                    placeholder="Share your question or order number..."
                  />
                </div>
                <button type="submit" className="btn" style={{ width: '100%', padding: '8px 12px' }}>
                  Submit Support Ticket
                </button>
              </form>
            )}
          </div>

          <div
            style={{
              padding: '8px 12px',
              borderTop: '1px solid #D9D9E0',
              background: '#FFFFFF',
              display: 'flex',
              justifyContent: 'space-between',
              gap: 8,
              flexWrap: 'wrap',
              fontSize: '0.78rem',
            }}
          >
            <button
              type="button"
              onClick={() => setShowTicketForm((v) => !v)}
              style={{
                background: '#F4EFF9',
                color: '#542080',
                border: '1px solid #E9DDF5',
                borderRadius: 999,
                padding: '5px 11px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              {showTicketForm ? 'Hide Ticket Form' : 'Speak to a Human (Ticket)'}
            </button>
            <Link
              to="/track"
              onClick={() => setOpen(false)}
              style={{ color: '#542080', fontWeight: 600, alignSelf: 'center' }}
            >
              Track Order →
            </Link>
          </div>

          <form
            onSubmit={(e) => void handleSend(e)}
            style={{
              display: 'flex',
              gap: 8,
              padding: '10px 12px',
              borderTop: '1px solid #D9D9E0',
              background: '#FFFFFF',
            }}
          >
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={
                mode === 'SHOPPING'
                  ? 'Ask about styles, prices, or sizing...'
                  : 'Ask about order tracking, delivery, or policies...'
              }
              style={{
                flex: 1,
                border: '1px solid #D9D9E0',
                borderRadius: 999,
                padding: '9px 14px',
                fontSize: '0.88rem',
              }}
            />
            <button
              type="submit"
              className="btn"
              disabled={sending}
              aria-label="Send message"
              style={{ padding: '9px 15px', minHeight: 38 }}
            >
              <Send size={15} />
            </button>
          </form>
        </section>
      )}
    </>
  );
}
