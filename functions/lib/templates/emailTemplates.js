"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildZeecrownEmail = buildZeecrownEmail;
function escapeHtml(raw) {
    return raw
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}
function buildZeecrownEmail(ctx) {
    const safeStore = escapeHtml(ctx.storeName || "ZEECROWN STORE");
    const safeCustomer = escapeHtml(ctx.customerName || "Valued Client");
    const safeHeadline = escapeHtml(ctx.headline);
    const safeMessage = escapeHtml(ctx.message);
    const safeOrder = escapeHtml(ctx.orderNumber);
    const logoBlock = ctx.logoUrl && ctx.logoUrl.startsWith("http")
        ? `<img src="${escapeHtml(ctx.logoUrl)}" alt="${safeStore}" style="height:44px;margin-bottom:8px;border-radius:6px;" />`
        : `<div style="font-family:Georgia,serif;font-size:22px;letter-spacing:3px;color:#FFFFFF;font-weight:bold;">${safeStore}</div>`;
    const detailRows = [];
    if (ctx.totalFormatted)
        detailRows.push(`<tr><td style="color:#5C4B6E;padding:4px 0;">Order Total</td><td style="color:#1F122B;font-weight:600;text-align:right;">${escapeHtml(ctx.totalFormatted)}</td></tr>`);
    if (ctx.destinationMasked)
        detailRows.push(`<tr><td style="color:#5C4B6E;padding:4px 0;">Destination</td><td style="color:#1F122B;text-align:right;">${escapeHtml(ctx.destinationMasked)}</td></tr>`);
    if (ctx.courierName)
        detailRows.push(`<tr><td style="color:#5C4B6E;padding:4px 0;">Courier</td><td style="color:#1F122B;text-align:right;">${escapeHtml(ctx.courierName)}</td></tr>`);
    if (ctx.trackingReference)
        detailRows.push(`<tr><td style="color:#5C4B6E;padding:4px 0;">Tracking Ref</td><td style="color:#351052;font-weight:600;text-align:right;">${escapeHtml(ctx.trackingReference)}</td></tr>`);
    if (ctx.estimatedDeliveryWindow)
        detailRows.push(`<tr><td style="color:#5C4B6E;padding:4px 0;">Est. Window</td><td style="color:#1F122B;text-align:right;">${escapeHtml(ctx.estimatedDeliveryWindow)}</td></tr>`);
    const detailsTable = detailRows.length > 0
        ? `<table style="width:100%;border-collapse:collapse;margin:18px 0;padding:12px;background:#F4EFF9;border:1px solid #D9D9E0;border-radius:10px;"><tbody>${detailRows.join("")}</tbody></table>`
        : "";
    const ctaBlock = ctx.trackingLink
        ? `<div style="margin:24px 0;text-align:center;"><a href="${escapeHtml(ctx.trackingLink)}" style="background:#542080;border:1px solid #C8A64D;color:#FFFFFF;text-decoration:none;padding:12px 26px;border-radius:24px;font-weight:600;display:inline-block;">Track Your Order</a></div>`
        : "";
    const footerSupport = ctx.supportEmail
        ? `Need assistance? Reply to ${escapeHtml(ctx.supportEmail)} or chat on WhatsApp (+234 816 989 8763).`
        : `Thank you for shopping with ${safeStore}. WhatsApp Support: +234 816 989 8763.`;
    const html = `<!DOCTYPE html>
<html>
<body style="margin:0;padding:24px;background:#FFFCF5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;color:#1F122B;">
  <div style="max-width:560px;margin:0 auto;background:#FFFFFF;border:1px solid #D9D9E0;border-radius:16px;overflow:hidden;">
    <div style="text-align:center;background:#542080;border-bottom:3px solid #C8A64D;padding:20px 24px;">
      ${logoBlock}
      <div style="font-size:12px;color:#E5D2A0;letter-spacing:1.5px;margin-top:6px;font-weight:600;">ORDER ${safeOrder}</div>
    </div>
    <div style="padding:28px;">
      <h2 style="font-family:Georgia,serif;color:#351052;margin:0 0 12px 0;">${safeHeadline}</h2>
      <p style="font-size:15px;line-height:1.6;color:#1F122B;">Hello ${safeCustomer},</p>
      <p style="font-size:15px;line-height:1.6;color:#1F122B;">${safeMessage}</p>
      ${detailsTable}
      ${ctaBlock}
      <p style="font-size:12px;color:#5C4B6E;border-top:1px solid #D9D9E0;padding-top:16px;margin-top:24px;">${footerSupport}</p>
    </div>
  </div>
</body>
</html>`;
    const textLines = [
        `${ctx.storeName || "ZEECROWN STORE"} — ${ctx.headline}`,
        `Order: ${ctx.orderNumber}`,
        "",
        `Hello ${ctx.customerName || "Valued Client"},`,
        ctx.message,
    ];
    if (ctx.totalFormatted)
        textLines.push(`Total: ${ctx.totalFormatted}`);
    if (ctx.courierName)
        textLines.push(`Courier: ${ctx.courierName}`);
    if (ctx.trackingReference)
        textLines.push(`Tracking Ref: ${ctx.trackingReference}`);
    if (ctx.estimatedDeliveryWindow)
        textLines.push(`Estimated Delivery Window: ${ctx.estimatedDeliveryWindow}`);
    if (ctx.trackingLink)
        textLines.push(`Track Order: ${ctx.trackingLink}`);
    if (ctx.supportEmail)
        textLines.push(`Support: ${ctx.supportEmail}`);
    textLines.push("WhatsApp Support: +234 816 989 8763 (https://wa.me/2348169898763)");
    return { html, text: textLines.join("\n") };
}
//# sourceMappingURL=emailTemplates.js.map