import * as nodemailer from "nodemailer";

export interface EmailPayload {
  to: string;
  subject: string;
  html: string;
  text: string;
  senderName: string;
  replyTo?: string;
}

export interface ProviderSendResult {
  status: "SENT" | "FAILED" | "PROVIDER_NOT_CONFIGURED";
  providerName: string;
  externalMessageId?: string;
  failureReason?: string;
}

export interface EmailProvider {
  readonly name: string;
  isConfigured(): boolean;
  send(payload: EmailPayload): Promise<ProviderSendResult>;
}

class ResendEmailProvider implements EmailProvider {
  readonly name = "Resend";
  private readonly apiKey = (process.env.RESEND_API_KEY || "").trim();
  private readonly fromAddress = (process.env.EMAIL_FROM_ADDRESS || "").trim();

  isConfigured(): boolean {
    return this.apiKey.length > 0 && this.fromAddress.length > 0;
  }

  async send(payload: EmailPayload): Promise<ProviderSendResult> {
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
      const json = (await response.json()) as { id?: string };
      return {
        status: "SENT",
        providerName: this.name,
        externalMessageId: json.id,
      };
    } catch (err) {
      return {
        status: "FAILED",
        providerName: this.name,
        failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown Resend transport error",
      };
    }
  }
}

class SendGridEmailProvider implements EmailProvider {
  readonly name = "SendGrid";
  private readonly apiKey = (process.env.SENDGRID_API_KEY || "").trim();
  private readonly fromAddress = (process.env.EMAIL_FROM_ADDRESS || "").trim();

  isConfigured(): boolean {
    return this.apiKey.length > 0 && this.fromAddress.length > 0;
  }

  async send(payload: EmailPayload): Promise<ProviderSendResult> {
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
    } catch (err) {
      return {
        status: "FAILED",
        providerName: this.name,
        failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown SendGrid transport error",
      };
    }
  }
}

class SmtpEmailProvider implements EmailProvider {
  readonly name = "SMTP";
  private readonly host = (process.env.SMTP_HOST || "").trim();
  private readonly port = Number(process.env.SMTP_PORT || "587");
  private readonly user = (process.env.SMTP_USER || "").trim();
  private readonly pass = (process.env.SMTP_PASS || "").trim();
  private readonly fromAddress = (process.env.EMAIL_FROM_ADDRESS || "").trim();

  isConfigured(): boolean {
    return this.host.length > 0 && this.user.length > 0 && this.pass.length > 0 && this.fromAddress.length > 0;
  }

  async send(payload: EmailPayload): Promise<ProviderSendResult> {
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
    } catch (err) {
      return {
        status: "FAILED",
        providerName: this.name,
        failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown SMTP error",
      };
    }
  }
}

export function resolveEmailProvider(): EmailProvider {
  const mode = (process.env.EMAIL_PROVIDER || "RESEND").trim().toUpperCase();
  if (mode === "SENDGRID") return new SendGridEmailProvider();
  if (mode === "SMTP") return new SmtpEmailProvider();
  return new ResendEmailProvider();
}
