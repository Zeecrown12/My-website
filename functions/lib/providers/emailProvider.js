"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.resolveEmailProvider = resolveEmailProvider;
const nodemailer = __importStar(require("nodemailer"));
class ResendEmailProvider {
    name = "Resend";
    apiKey = (process.env.RESEND_API_KEY || "").trim();
    fromAddress = (process.env.EMAIL_FROM_ADDRESS || "").trim();
    isConfigured() {
        return this.apiKey.length > 0 && this.fromAddress.length > 0;
    }
    async send(payload) {
        if (!this.isConfigured()) {
            return {
                status: "PROVIDER_NOT_CONFIGURED",
                providerName: this.name,
                failureReason: "RESEND_API_KEY or EMAIL_FROM_ADDRESS secret is not configured in Cloud Functions.",
            };
        }
        try {
            const response = await fetch("https://api.resend.com/emails", {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${this.apiKey}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    from: `${payload.senderName} <${this.fromAddress}>`,
                    to: [payload.to],
                    reply_to: payload.replyTo || undefined,
                    subject: payload.subject,
                    html: payload.html,
                    text: payload.text,
                }),
            });
            if (!response.ok) {
                const errBody = await response.text();
                return {
                    status: "FAILED",
                    providerName: this.name,
                    failureReason: `Resend HTTP ${response.status}: ${errBody.slice(0, 260)}`,
                };
            }
            const json = (await response.json());
            return {
                status: "SENT",
                providerName: this.name,
                externalMessageId: json.id,
            };
        }
        catch (err) {
            return {
                status: "FAILED",
                providerName: this.name,
                failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown Resend transport error",
            };
        }
    }
}
class SendGridEmailProvider {
    name = "SendGrid";
    apiKey = (process.env.SENDGRID_API_KEY || "").trim();
    fromAddress = (process.env.EMAIL_FROM_ADDRESS || "").trim();
    isConfigured() {
        return this.apiKey.length > 0 && this.fromAddress.length > 0;
    }
    async send(payload) {
        if (!this.isConfigured()) {
            return {
                status: "PROVIDER_NOT_CONFIGURED",
                providerName: this.name,
                failureReason: "SENDGRID_API_KEY or EMAIL_FROM_ADDRESS secret is not configured in Cloud Functions.",
            };
        }
        try {
            const response = await fetch("https://api.sendgrid.com/v3/mail/send", {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${this.apiKey}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    personalizations: [{ to: [{ email: payload.to }] }],
                    from: { email: this.fromAddress, name: payload.senderName },
                    reply_to: payload.replyTo ? { email: payload.replyTo } : undefined,
                    subject: payload.subject,
                    content: [
                        { type: "text/plain", value: payload.text },
                        { type: "text/html", value: payload.html },
                    ],
                }),
            });
            if (!response.ok) {
                const errBody = await response.text();
                return {
                    status: "FAILED",
                    providerName: this.name,
                    failureReason: `SendGrid HTTP ${response.status}: ${errBody.slice(0, 260)}`,
                };
            }
            return {
                status: "SENT",
                providerName: this.name,
                externalMessageId: response.headers.get("x-message-id") || undefined,
            };
        }
        catch (err) {
            return {
                status: "FAILED",
                providerName: this.name,
                failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown SendGrid transport error",
            };
        }
    }
}
class SmtpEmailProvider {
    name = "SMTP";
    host = (process.env.SMTP_HOST || "").trim();
    port = Number(process.env.SMTP_PORT || "587");
    user = (process.env.SMTP_USER || "").trim();
    pass = (process.env.SMTP_PASS || "").trim();
    fromAddress = (process.env.EMAIL_FROM_ADDRESS || "").trim();
    isConfigured() {
        return this.host.length > 0 && this.user.length > 0 && this.pass.length > 0 && this.fromAddress.length > 0;
    }
    async send(payload) {
        if (!this.isConfigured()) {
            return {
                status: "PROVIDER_NOT_CONFIGURED",
                providerName: this.name,
                failureReason: "SMTP_HOST, SMTP_USER, SMTP_PASS, or EMAIL_FROM_ADDRESS secret is missing.",
            };
        }
        try {
            const transporter = nodemailer.createTransport({
                host: this.host,
                port: this.port,
                secure: this.port === 465,
                auth: { user: this.user, pass: this.pass },
            });
            const info = await transporter.sendMail({
                from: `"${payload.senderName}" <${this.fromAddress}>`,
                to: payload.to,
                replyTo: payload.replyTo,
                subject: payload.subject,
                text: payload.text,
                html: payload.html,
            });
            return {
                status: "SENT",
                providerName: this.name,
                externalMessageId: info.messageId,
            };
        }
        catch (err) {
            return {
                status: "FAILED",
                providerName: this.name,
                failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown SMTP error",
            };
        }
    }
}
function resolveEmailProvider() {
    const mode = (process.env.EMAIL_PROVIDER || "RESEND").trim().toUpperCase();
    if (mode === "SENDGRID")
        return new SendGridEmailProvider();
    if (mode === "SMTP")
        return new SmtpEmailProvider();
    return new ResendEmailProvider();
}
//# sourceMappingURL=emailProvider.js.map