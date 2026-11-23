import { ProviderSendResult } from "./emailProvider";
import { normalizeNigerianPhone } from "./smsProvider";

export interface WhatsAppPayload {
  toPhone: string;
  templateName: string;
  bodyText: string;
}

export interface WhatsAppProvider {
  readonly name: string;
  isConfigured(): boolean;
  send(payload: WhatsAppPayload): Promise<ProviderSendResult>;
}

class MetaWhatsAppProvider implements WhatsAppProvider {
  readonly name = "Meta WhatsApp Cloud API";
  private readonly accessToken = (process.env.WHATSAPP_ACCESS_TOKEN || "").trim();
  private readonly phoneNumberId = (process.env.WHATSAPP_PHONE_NUMBER_ID || "").trim();

  isConfigured(): boolean {
    return this.accessToken.length > 0 && this.phoneNumberId.length > 0;
  }

  async send(payload: WhatsAppPayload): Promise<ProviderSendResult> {
    if (!this.isConfigured()) {
      return {
        status: "PROVIDER_NOT_CONFIGURED",
        providerName: this.name,
        failureReason: "WHATSAPP_ACCESS_TOKEN or WHATSAPP_PHONE_NUMBER_ID secret is not configured.",
      };
    }
    const recipientDigits = normalizeNigerianPhone(payload.toPhone).replace(/^\+/, "");
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
      const json = (await response.json()) as { messages?: Array<{ id?: string }> };
      return {
        status: "SENT",
        providerName: this.name,
        externalMessageId: json.messages?.[0]?.id,
      };
    } catch (err) {
      return {
        status: "FAILED",
        providerName: this.name,
        failureReason: err instanceof Error ? err.message.slice(0, 300) : "Unknown WhatsApp API error",
      };
    }
  }
}

export function resolveWhatsAppProvider(): WhatsAppProvider {
  return new MetaWhatsAppProvider();
}
