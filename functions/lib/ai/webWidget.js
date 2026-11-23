"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildAiWebWidgetHtml = buildAiWebWidgetHtml;
const seoUtils_1 = require("../seo/seoUtils");
/**
 * Generates an accessible, responsive, XSS-safe ZEECROWN CUSTOMER CARE concierge floating
 * hub for server-rendered storefront HTML pages. Uses DOM textContent binding exclusively
 * for all user/server strings.
 */
function buildAiWebWidgetHtml(options) {
    const assistantName = (0, seoUtils_1.escapeHtml)(options.assistantName || "ZEECROWN CUSTOMER CARE");
    const welcomeMessage = (0, seoUtils_1.escapeHtml)(options.welcomeMessage ||
        "Welcome to ZEECROWN STORE. Choose Shopping Assistance, Customer Support, or Chat with Us on WhatsApp (+234 816 989 8763) — we are here to help.");
    const apiBase = (0, seoUtils_1.escapeHtml)((options.aiApiBase || "/api/ai").replace(/\/$/, ""));
    const wa = (0, seoUtils_1.resolveWhatsAppContact)(options.whatsappNumber);
    const directContacts = [
        options.supportEmail ? `Email: ${options.supportEmail}` : "",
        `WhatsApp: ${wa.display}`,
        options.supportPhone ? `Phone: ${options.supportPhone}` : "",
    ]
        .filter(Boolean)
        .join(" · ");
    return `
<style>
  #zc-ai-launcher { position:fixed; right:20px; bottom:20px; z-index:98; background:#542080; color:#FFFFFF; border:1px solid #C8A64D; border-radius:999px; padding:12px 22px; font-family:system-ui,-apple-system,sans-serif; font-weight:700; font-size:0.92rem; cursor:pointer; box-shadow:0 12px 28px rgba(53,16,82,0.28); display:flex; align-items:center; gap:8px; }
  #zc-ai-launcher:hover { background:#351052; }
  #zc-ai-drawer { display:none; position:fixed; right:20px; bottom:78px; width:min(400px, calc(100vw - 24px)); max-height:min(640px, calc(100vh - 108px)); background:#FFFFFF; border:1px solid #D9D9E0; border-radius:20px; box-shadow:0 20px 50px rgba(31,18,43,0.24); z-index:99; flex-direction:column; overflow:hidden; font-family:system-ui,-apple-system,sans-serif; color:#1F122B; }
  #zc-ai-drawer.open { display:flex; }
  .zc-ai-head { background:linear-gradient(135deg, #351052 0%, #542080 100%); color:#FFFFFF; padding:14px 16px; border-bottom:2px solid #C8A64D; display:flex; align-items:center; justify-content:space-between; }
  .zc-ai-head strong { font-size:0.95rem; letter-spacing:0.05em; display:block; color:#FFFFFF; }
  .zc-ai-head span { font-size:0.76rem; color:#E5D2A0; }
  .zc-ai-close { background:transparent; border:1px solid rgba(255,255,255,0.45); color:#FFFFFF; border-radius:999px; width:32px; height:32px; cursor:pointer; font-size:1rem; }
  .zc-ai-modes { display:flex; gap:6px; padding:8px 12px; background:#F4EFF9; border-bottom:1px solid #D9D9E0; overflow-x:auto; }
  .zc-mode-btn { background:#FFFFFF; color:#542080; border:1px solid #D9D9E0; border-radius:999px; padding:6px 11px; font-size:0.75rem; font-weight:700; cursor:pointer; white-space:nowrap; text-decoration:none; display:inline-flex; align-items:center; gap:5px; }
  .zc-mode-btn.active { background:#542080; color:#FFFFFF; border-color:#542080; }
  .zc-mode-btn.wa { background:#1F8A4D; color:#FFFFFF; border-color:#1F8A4D; }
  .zc-ai-msgs { flex:1; overflow-y:auto; padding:14px; display:flex; flex-direction:column; gap:10px; background:#FFFCF5; }
  .zc-msg { max-width:88%; padding:10px 13px; border-radius:14px; font-size:0.88rem; line-height:1.45; white-space:pre-line; }
  .zc-msg.user { align-self:flex-end; background:#542080; color:#FFFFFF; border-bottom-right-radius:4px; }
  .zc-msg.ai { align-self:flex-start; background:#FFFFFF; border:1px solid #D9D9E0; color:#1F122B; border-bottom-left-radius:4px; }
  .zc-msg.sys { align-self:center; background:#E9DDF5; border:1px solid #C8A64D; color:#1F122B; font-size:0.83rem; width:100%; }
  .zc-ai-chips { padding:6px 12px; display:flex; gap:6px; overflow-x:auto; border-top:1px solid #D9D9E0; background:#FFFFFF; }
  .zc-chip { background:#F4EFF9; color:#542080; border:1px solid #D9D9E0; border-radius:999px; padding:6px 11px; font-size:0.78rem; cursor:pointer; white-space:nowrap; font-weight:600; text-decoration:none; display:inline-flex; align-items:center; }
  .zc-prod-card { display:flex; gap:10px; background:#FFFFFF; border:1px solid #D9D9E0; border-radius:12px; padding:8px; margin-top:6px; text-decoration:none; color:#1F122B; align-items:center; }
  .zc-prod-card img { width:52px; height:64px; object-fit:cover; border-radius:8px; background:#F4EFF9; }
  .zc-ai-form { display:flex; gap:8px; padding:10px 12px; border-top:1px solid #D9D9E0; background:#FFFFFF; }
  .zc-ai-form input { flex:1; border:1px solid #D9D9E0; border-radius:999px; padding:9px 14px; font-size:0.88rem; outline:none; color:#1F122B; }
  .zc-ai-form input:focus { border-color:#542080; }
  .zc-ai-form button { background:#542080; color:#FFFFFF; border:none; border-radius:999px; padding:9px 16px; font-weight:600; cursor:pointer; }
  .zc-ai-form button:hover { background:#351052; }
  .zc-box { border:1px solid #D9D9E0; background:#FFFFFF; border-radius:12px; padding:10px; margin-top:6px; display:flex; flex-direction:column; gap:6px; }
  .zc-box input, .zc-box textarea { width:100%; border:1px solid #D9D9E0; border-radius:8px; padding:7px 9px; font-size:0.82rem; font-family:inherit; color:#1F122B; }
  .zc-box button { background:#542080; color:#FFFFFF; border:none; border-radius:999px; padding:8px 12px; font-size:0.82rem; font-weight:600; cursor:pointer; }
</style>
<button id="zc-ai-launcher" type="button" aria-label="Open ZEECROWN CUSTOMER CARE">&#10024; Need Help?</button>
<section id="zc-ai-drawer" role="dialog" aria-label="${assistantName}">
  <div class="zc-ai-head">
    <div>
      <strong>${assistantName}</strong>
      <span>Your shopping and customer support assistant.</span>
    </div>
    <button id="zc-ai-close" class="zc-ai-close" type="button" aria-label="Close Customer Care">&times;</button>
  </div>
  <div class="zc-ai-modes" role="tablist" aria-label="Customer Care Modes">
    <button type="button" class="zc-mode-btn active" id="zc-mode-shopping" data-mode="shopping">Shopping Assistance</button>
    <button type="button" class="zc-mode-btn" id="zc-mode-support" data-mode="support">Customer Support</button>
    <a class="zc-mode-btn wa" href="${(0, seoUtils_1.escapeHtml)(wa.url)}" target="_blank" rel="noopener noreferrer">Chat with Us on WhatsApp</a>
  </div>
  <div id="zc-ai-msgs" class="zc-ai-msgs">
    <div class="zc-msg ai">${welcomeMessage}</div>
  </div>
  <div id="zc-ai-chips" class="zc-ai-chips">
    <a class="zc-chip" style="background:#1F8A4D;color:#FFFFFF;border-color:#1F8A4D;" href="${(0, seoUtils_1.escapeHtml)(wa.url)}" target="_blank" rel="noopener noreferrer">Contact Store Owner (WhatsApp)</a>
    <button type="button" class="zc-chip" id="zc-chip-human">Speak to a Human</button>
    <button type="button" class="zc-chip" data-mode-tag="shopping" data-q="Show outfits under 25000 naira">Under &#8358;25,000</button>
    <button type="button" class="zc-chip" data-mode-tag="shopping" data-q="Which sizes and styles are available?">Sizing &amp; Fit</button>
    <button type="button" class="zc-chip" data-mode-tag="support" data-q="What are Lagos and Abuja delivery fees?">Delivery Fees</button>
    <button type="button" class="zc-chip" data-mode-tag="support" data-q="How do I pay and order?">Payments</button>
    <button type="button" class="zc-chip" data-mode-tag="support" data-q="What is your return and refund policy?">Return Policy</button>
    <button type="button" class="zc-chip" data-mode-tag="support" data-q="Track my order">Track Order</button>
  </div>
  <form id="zc-ai-form" class="zc-ai-form">
    <input id="zc-ai-input" type="text" maxlength="800" placeholder="Ask about styles, sizing, delivery, or your order..." required>
    <button type="submit">Send</button>
  </form>
</section>
<script>
(function(){
  var apiBase = (window.__ZC_AI_BASE__ || "${apiBase}").replace(/\\/$/, "");
  var waUrl = ${JSON.stringify(wa.url)};
  var waDisplay = ${JSON.stringify(wa.display)};
  var fallbackContacts = ${JSON.stringify(directContacts)};
  var launcher = document.getElementById("zc-ai-launcher");
  var drawer = document.getElementById("zc-ai-drawer");
  var closeBtn = document.getElementById("zc-ai-close");
  var msgsEl = document.getElementById("zc-ai-msgs");
  var form = document.getElementById("zc-ai-form");
  var input = document.getElementById("zc-ai-input");
  var shoppingModeBtn = document.getElementById("zc-mode-shopping");
  var supportModeBtn = document.getElementById("zc-mode-support");
  var activeMode = "shopping";
  var history = [];
  var sending = false;

  if (!launcher || !drawer) return;
  launcher.addEventListener("click", function(){ drawer.classList.toggle("open"); });
  closeBtn.addEventListener("click", function(){ drawer.classList.remove("open"); });

  function setMode(mode) {
    activeMode = mode;
    if (shoppingModeBtn) shoppingModeBtn.classList.toggle("active", mode === "shopping");
    if (supportModeBtn) supportModeBtn.classList.toggle("active", mode === "support");
    if (input) {
      input.placeholder = mode === "support"
        ? "Ask about order tracking, delivery, payments, or request support..."
        : "Ask about Abayas, Adire silk sets, prices, sizes, or styling...";
    }
    if (mode === "support") {
      appendMsg("SYSTEM", "Customer Support Mode: Check an order, view delivery & store policies (all sales final), or tap 'Speak to a Human' / WhatsApp (" + waDisplay + ").", { suggestHumanHandover: true, escalationReason: "Customer Support Enquiry" });
    }
  }
  if (shoppingModeBtn) shoppingModeBtn.addEventListener("click", function(){ setMode("shopping"); });
  if (supportModeBtn) supportModeBtn.addEventListener("click", function(){ setMode("support"); });

  function appendMsg(role, text, opts) {
    var box = document.createElement("div");
    box.className = "zc-msg " + (role === "USER" ? "user" : (role === "SYSTEM" ? "sys" : "ai"));
    box.textContent = text;

    if (opts && Array.isArray(opts.recommendedProducts) && opts.recommendedProducts.length) {
      opts.recommendedProducts.forEach(function(p){
        var a = document.createElement("a");
        a.className = "zc-prod-card";
        a.href = p.url || ("/product/" + encodeURIComponent(p.slug || p.productId));
        if (p.imageUrl) {
          var img = document.createElement("img");
          img.src = p.imageUrl;
          img.alt = p.title || "ZEECROWN piece";
          a.appendChild(img);
        }
        var meta = document.createElement("div");
        var title = document.createElement("div");
        title.style.fontWeight = "600";
        title.textContent = p.title || "";
        var price = document.createElement("div");
        price.style.color = "#542080";
        price.style.fontWeight = "700";
        price.textContent = "NGN " + Number(p.priceNgn || 0).toLocaleString("en-NG");
        meta.appendChild(title);
        meta.appendChild(price);
        a.appendChild(meta);
        box.appendChild(a);
      });
    }

    if (opts && opts.autoOfferGuestTracker) {
      box.appendChild(buildGuestTrackerBox());
    }
    if (opts && opts.suggestHumanHandover) {
      var escRow = document.createElement("div");
      escRow.style.display = "flex";
      escRow.style.flexWrap = "wrap";
      escRow.style.gap = "6px";
      escRow.style.marginTop = "8px";
      var escBtn = document.createElement("button");
      escBtn.type = "button";
      escBtn.className = "zc-chip";
      escBtn.textContent = "Speak to a Human";
      escBtn.addEventListener("click", function(){ openHandoverForm(opts.escalationReason || "Customer Care Support"); });
      var waLink = document.createElement("a");
      waLink.className = "zc-chip";
      waLink.style.background = "#1F8A4D";
      waLink.style.color = "#FFFFFF";
      waLink.style.borderColor = "#1F8A4D";
      waLink.href = waUrl;
      waLink.target = "_blank";
      waLink.rel = "noopener noreferrer";
      waLink.textContent = "WhatsApp Store Owner";
      escRow.appendChild(escBtn);
      escRow.appendChild(waLink);
      box.appendChild(escRow);
    }
    msgsEl.appendChild(box);
    msgsEl.scrollTop = msgsEl.scrollHeight;
  }

  function buildGuestTrackerBox() {
    var wrap = document.createElement("div");
    wrap.className = "zc-box";
    var h = document.createElement("strong");
    h.textContent = "Verify Your Order Status";
    var ordIn = document.createElement("input");
    ordIn.placeholder = "Order Number (e.g. ZC-1001)";
    var conIn = document.createElement("input");
    conIn.placeholder = "Checkout Email or Phone";
    var btn = document.createElement("button");
    btn.type = "button";
    btn.textContent = "Check Order Status";
    var resP = document.createElement("div");
    resP.style.fontSize = "0.8rem";
    btn.addEventListener("click", async function(){
      resP.textContent = "Verifying...";
      try {
        var r = await fetch(apiBase + "/guest-track", {
          method: "POST",
          headers: {"Content-Type":"application/json"},
          body: JSON.stringify({ orderNumber: ordIn.value, contact: conIn.value })
        });
        var d = await r.json();
        if (!r.ok || !d.projection) {
          resP.textContent = d.error || "Order details could not be verified.";
        } else {
          var pr = d.projection;
          resP.textContent = "Order " + pr.orderNumber + ": " + pr.status + " (Payment: " + pr.paymentStatus + ")" + (pr.courierName ? " · Courier: " + pr.courierName : "");
        }
      } catch(e) {
        resP.textContent = "Verification service unavailable right now. Please open Customer Support below.";
      }
    });
    wrap.appendChild(h); wrap.appendChild(ordIn); wrap.appendChild(conIn); wrap.appendChild(btn); wrap.appendChild(resP);
    return wrap;
  }

  function openHandoverForm(defaultReason) {
    var wrap = document.createElement("div");
    wrap.className = "zc-msg ai";
    var box = document.createElement("div");
    box.className = "zc-box";
    var h = document.createElement("strong");
    h.textContent = "Speak to a Human / Contact Store Owner";
    var waDirect = document.createElement("a");
    waDirect.href = waUrl;
    waDirect.target = "_blank";
    waDirect.rel = "noopener noreferrer";
    waDirect.style.display = "inline-block";
    waDirect.style.background = "#1F8A4D";
    waDirect.style.color = "#FFFFFF";
    waDirect.style.textDecoration = "none";
    waDirect.style.borderRadius = "999px";
    waDirect.style.padding = "8px 12px";
    waDirect.style.fontSize = "0.82rem";
    waDirect.style.fontWeight = "600";
    waDirect.style.textAlign = "center";
    waDirect.textContent = "Chat with Us on WhatsApp (" + waDisplay + ")";
    var nameIn = document.createElement("input"); nameIn.placeholder = "Your Full Name *";
    var emailIn = document.createElement("input"); emailIn.placeholder = "Email Address *";
    var ordIn = document.createElement("input"); ordIn.placeholder = "Order Number (Optional)";
    var noteIn = document.createElement("textarea"); noteIn.rows = 2; noteIn.placeholder = "How can our team assist you? *";
    var subBtn = document.createElement("button"); subBtn.type = "button"; subBtn.textContent = "Submit Support Ticket";
    var statusEl = document.createElement("div"); statusEl.style.fontSize = "0.8rem";
    subBtn.addEventListener("click", async function(){
      statusEl.textContent = "Submitting to Customer Care desk...";
      try {
        var r = await fetch(apiBase + "/escalate", {
          method: "POST",
          headers: {"Content-Type":"application/json"},
          body: JSON.stringify({
            customerName: nameIn.value,
            customerEmail: emailIn.value,
            orderNumber: ordIn.value,
            reason: defaultReason,
            note: noteIn.value,
            transcript: history.slice(-8)
          })
        });
        var d = await r.json();
        if (r.ok && d.ok) {
          appendMsg("SYSTEM", d.message || ("Ticket confirmed: " + d.ticketId));
          subBtn.disabled = true;
        } else {
          statusEl.textContent = (d.error || "Unable to submit ticket.") + (fallbackContacts ? " Contact directly: " + fallbackContacts : "");
        }
      } catch(e) {
        statusEl.textContent = "Network error submitting ticket." + (fallbackContacts ? " Contact directly: " + fallbackContacts : "");
      }
    });
    box.appendChild(h); box.appendChild(waDirect); box.appendChild(nameIn); box.appendChild(emailIn); box.appendChild(ordIn); box.appendChild(noteIn); box.appendChild(subBtn); box.appendChild(statusEl);
    wrap.appendChild(box);
    msgsEl.appendChild(wrap);
    msgsEl.scrollTop = msgsEl.scrollHeight;
  }

  async function sendQuery(q) {
    if (!q || sending) return;
    sending = true;
    appendMsg("USER", q);
    history.push({ role: "USER", text: q });
    var typing = document.createElement("div");
    typing.className = "zc-msg ai";
    typing.textContent = "Checking live store records...";
    msgsEl.appendChild(typing);
    msgsEl.scrollTop = msgsEl.scrollHeight;
    try {
      var res = await fetch(apiBase + "/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query: q, mode: activeMode, history: history.slice(-8) })
      });
      var data = await res.json();
      typing.remove();
      if (!res.ok) {
        appendMsg("SYSTEM", data.error || "Customer Care assistant temporarily unavailable." + (fallbackContacts ? " Contact us: " + fallbackContacts : ""));
      } else {
        history.push({ role: "ASSISTANT", text: data.replyText || "" });
        appendMsg("ASSISTANT", data.replyText || "", data);
      }
    } catch(e) {
      typing.remove();
      appendMsg("SYSTEM", "Could not reach ZEECROWN Customer Care right now." + (fallbackContacts ? " Reach us directly: " + fallbackContacts : ""));
    } finally {
      sending = false;
    }
  }

  form.addEventListener("submit", function(ev){
    ev.preventDefault();
    var q = (input.value || "").trim();
    if (!q) return;
    input.value = "";
    sendQuery(q);
  });

  document.querySelectorAll(".zc-chip[data-q]").forEach(function(btn){
    btn.addEventListener("click", function(){
      drawer.classList.add("open");
      sendQuery(btn.getAttribute("data-q"));
    });
  });
  var humanChip = document.getElementById("zc-chip-human");
  if (humanChip) humanChip.addEventListener("click", function(){ openHandoverForm("Customer requested human specialist"); });
})();
</script>`;
}
//# sourceMappingURL=webWidget.js.map