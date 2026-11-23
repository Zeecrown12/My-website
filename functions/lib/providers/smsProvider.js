"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.normalizeNigerianPhone = normalizeNigerianPhone;
exports.resolveSmsProvider = resolveSmsProvider;
function normalizeNigerianPhone(phone) {
    const digits = phone.replace(/[^\d+]/g, "");
    if (digits.startsWith("+"))
        return digits;
    if (digits.startsWith("234") && digits.length >= 13)
        return `+${digits}`;
    if (digits.startsWith("0") && digits.length === 11)
        return `+234${digits.slice(1)}`;
    return digits;
}
class TermiiSmsProvider {
    name = "Termii";
    apiKey = (process.env.TERMII_API_KEY || "").trim();
    senderId = (process.env.TERMII_SENDER_ID || "ZEECROWN").trim();
    isConfigured() {
        return this.apiKey.length > 0;
    }
    async send(payload) {
        if (!this.isConfigured()) {
            return {
                status: "PROVIDER_NOT_CONFIGURED",
                providerName: this.name,
                failureReason: "TERMII_API_KEY secret is not configured in Cloud Functions.",
            };
        }
        const formattedPhone = normalizeNigerianPhone(payload.toPhone).replace(/^\+/, "");
        try {
            const response = await fetch("https://api.ng.termii.com/api/sms/send", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    to: formattedPhone,
                    from: this.senderId,
                    sms: payload.message,
                    type: "plain",
                    channel: "generic",
                    api_key: this.apiKey,
                }),
            });
            if (!response.ok) {
                const text = await response.text();
                return {
                    status: "FAILED",
                    providerName: this.name,
                    failureReason: `Termii HTTP ${response.status}: ${text.slice(0, 260)}`,
                };
            }
            const data = (await response.json());
            return {
                status: "SENT",
                providerName: this.name,
                externalMessageId: data.message_id,
            };
        }
        catch (err) {
            return {
                status: "FAILED",
                providerName: this.name,
                failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown Termii error",
            };
        }
    }
}
class TwilioSmsProvider {
    name = "Twilio";
    sid = (process.env.TWILIO_ACCOUNT_SID || "").trim();
    token = (process.env.TWILIO_AUTH_TOKEN || "").trim();
    fromNumber = (process.env.TWILIO_FROM_NUMBER || "").trim();
    isConfigured() {
        return this.sid.length > 0 && this.token.length > 0 && this.fromNumber.length > 0;
    }
    async send(payload) {
        if (!this.isConfigured()) {
            return {
                status: "PROVIDER_NOT_CONFIGURED",
                providerName: this.name,
                failureReason: "TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, or TWILIO_FROM_NUMBER secret is missing.",
            };
        }
        try {
            const auth = Buffer.from(`${this.sid}:${this.token}`).toString("base64");
            const form = new URLSearchParams({
                To: normalizeNigerianPhone(payload.toPhone),
                From: this.fromNumber,
                Body: payload.message,
            });
            const response = await fetch(`https://api.twilio.com/2010-04-01/Accounts/${this.sid}/Messages.json`, {
                method: "POST",
                headers: {
                    Authorization: `Basic ${auth}`,
                    "Content-Type": "application/x-www-form-urlencoded",
                },
                body: form.toString(),
            });
            if (!response.ok) {
                const text = await response.text();
                return {
                    status: "FAILED",
                    providerName: this.name,
                    failureReason: `Twilio HTTP ${response.status}: ${text.slice(0, 260)}`,
                };
            }
            const data = (await response.json());
            return {
                status: "SENT",
                providerName: this.name,
                externalMessageId: data.sid,
            };
        }
        catch (err) {
            return {
                status: "FAILED",
                providerName: this.name,
                failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown Twilio error",
            };
        }
    }
}
function resolveSmsProvider() {
    const mode = (process.env.SMS_PROVIDER || "TERMII").trim().toUpperCase();
    if (mode === "TWILIO")
        return new TwilioSmsProvider();
    return new TermiiSmsProvider();
}
//# sourceMappingURL=smsProvider.js.map