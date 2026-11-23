export interface CourierStatusEvent {
  orderNumber: string;
  courierName: string;
  trackingReference: string;
  trackingUrl?: string;
  deliveryStatus: string;
  mappedOrderStatus?: string;
  estimatedDeliveryWindow?: string;
  note?: string;
}

export interface CourierAdapter {
  readonly providerName: string;
  buildTrackingUrl(trackingReference: string): string | undefined;
  mapDeliveryStatusToOrderStatus(deliveryStatus: string): string | undefined;
}

export class DefaultCourierAdapter implements CourierAdapter {
  constructor(readonly providerName: string) {}

  buildTrackingUrl(trackingReference: string): string | undefined {
    const cleanRef = trackingReference.trim();
    if (!cleanRef) return undefined;
    const normalized = this.providerName.trim().toLowerCase();
    if (normalized.includes("dhl")) {
      return `https://www.dhl.com/ng-en/home/tracking.html?tracking-id=${encodeURIComponent(cleanRef)}`;
    }
    if (normalized.includes("gigl") || normalized.includes("gig")) {
      return `https://giglogistics.com/track?waybill=${encodeURIComponent(cleanRef)}`;
    }
    if (normalized.includes("sendbox")) {
      return `https://sendbox.co/track/${encodeURIComponent(cleanRef)}`;
    }
    return undefined;
  }

  mapDeliveryStatusToOrderStatus(deliveryStatus: string): string | undefined {
    const status = deliveryStatus.trim().toUpperCase();
    if (status.includes("DELIVERED")) return "DELIVERED";
    if (status.includes("OUT_FOR_DELIVERY") || status.includes("OUT FOR DELIVERY")) return "OUT_FOR_DELIVERY";
    if (status.includes("IN_TRANSIT") || status.includes("SHIPPED") || status.includes("DISPATCHED")) return "SHIPPED";
    if (status.includes("PICKED_UP") || status.includes("READY")) return "READY_FOR_DISPATCH";
    return undefined;
  }
}
