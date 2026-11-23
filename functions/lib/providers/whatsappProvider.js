"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.resolveWhatsAppProvider = resolveWhatsAppProvider;
const smsProvider_1 = require("./smsProvider");
class MetaWhatsAppProvider {
    name = "Meta WhatsApp Cloud API";
    accessToken = (process.env.WHATSAPP_ACCESS_TOKEN || "").trim();
    phoneNumberId = (process.env.WHATSAPP_PHONE_NUMBER_ID || "").trim();
    isConfigured() {
        return this.accessToken.length > 0 && this.phoneNumberId.length > 0;
    }
    async send(payload) {
        if (!this.isConfigured()) {
            return {
                status: "PROVIDER_NOT_CONFIGURED",
                providerName: this.name,
                failureReason: "WHATSAPP_ACCESS_TOKEN or WHATSAPP_PHONE_NUMBER_ID secret is not configured.",
            };
        }
        const recipientDigits = (0, smsProvider_1.normalizeNigerianPhone)(payload.toPhone).replace(/^\+/, "");
        try {
            const response = await fetch(`https://graph.facebook.com/v21.0/${this.phoneNumberId}/messages`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${this.accessToken}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    messaging_product: "whatsapp",
                    to: recipientDigits,
                    type: "text",
                    text: {
                        preview_url: true,
                        body: payload.bodyText,
                    },
                }),
            });
            if (!response.ok) {
                const text = await response.text();
                return {
                    status: "FAILED",
                    providerName: this.name,
                    failureReason: `WhatsApp HTTP ${response.status}: ${text.slice(0, 260)}`,
                };
            }
            const json = (await response.json());
            return {
                status: "SENT",
                providerName: this.name,
                externalMessageId: json.messages?.[0]?.id,
            };
        }
        catch (err) {
            return {
                status: "FAILED",
                providerName: this.name,
                failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown WhatsApp API error",
            };
        }
    }
}
function resolveWhatsAppProvider() {
    return new MetaWhatsAppProvider();
}
//# sourceMappingURL=whatsappProvider.js.map