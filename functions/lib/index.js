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
exports.onSupportEnquiryCreated = exports.analyzeProductPhotos = exports.aiAssistantEscalate = exports.aiAssistantChat = exports.robotsTxt = exports.sitemapXml = exports.seoServe = exports.retryCommunicationLog = exports.lookupGuestOrder = exports.courierWebhook = exports.onSupportReplyCreated = exports.onOrderWritten = void 0;
const crypto = __importStar(require("crypto"));
const app_1 = require("firebase-admin/app");
const firestore_1 = require("firebase-admin/firestore");
const messaging_1 = require("firebase-admin/messaging");
const firestore_2 = require("firebase-functions/v2/firestore");
const https_1 = require("firebase-functions/v2/https");
const courierProvider_1 = require("./providers/courierProvider");
const emailProvider_1 = require("./providers/emailProvider");
const smsProvider_1 = require("./providers/smsProvider");
const whatsappProvider_1 = require("./providers/whatsappProvider");
const aiEngine_1 = require("./ai/aiEngine");
const productAnalyzer_1 = require("./ai/productAnalyzer");
const seoRenderer_1 = require("./seo/seoRenderer");
const sitemapAndRobots_1 = require("./seo/sitemapAndRobots");
const emailTemplates_1 = require("./templates/emailTemplates");
(0, app_1.initializeApp)();
const DATABASE_ID = process.env.ZEECROWN_FIRESTORE_DB_ID ||
    "ai-studio-android-zeecrown-dd5ff843-a456-4fda-9689-4c093932b633";
const APP_BASE_URL = (process.env.APP_BASE_URL || "https://zeecrownstore.com").replace(/\/$/, "");
const BOOTSTRAPPED_ADMIN_EMAIL = "olaademi.ZO@gmail.com";
const db = (0, firestore_1.getFirestore)(DATABASE_ID);
const messaging = (0, messaging_1.getMessaging)();
function maskEmail(email) {
    const clean = email.trim();
    const at = clean.indexOf("@");
    if (at <= 1)
        return "***";
    return `${clean.slice(0, 2)}***${clean.slice(at)}`;
}
function maskPhone(phone) {
    const digits = phone.replace(/\s+/g, "");
    if (digits.length <= 4)
        return "****";
    return `***${digits.slice(-4)}`;
}
function sha256Prefix(input, len = 32) {
    return crypto.createHash("sha256").update(input).digest("hex").slice(0, len);
}
async function loadStoreConfig() {
    const snap = await db.collection("storeSettings").doc("public").get();
    const d = snap.data() || {};
    return {
        storeName: String(d.storeName || "ZEECROWN STORE"),
        logoUrl: typeof d.logoUrl === "string" ? d.logoUrl : undefined,
        supportEmail: typeof d.supportEmail === "string" && d.supportEmail ? d.supportEmail : undefined,
        emailNotificationsEnabled: d.emailNotificationsEnabled !== false,
        smsNotificationsEnabled: d.smsNotificationsEnabled === true,
        whatsappNotificationsEnabled: d.whatsappNotificationsEnabled === true,
        emailSenderName: String(d.emailSenderName || d.storeName || "ZEECROWN STORE"),
        emailReplyTo: typeof d.emailReplyTo === "string" && d.emailReplyTo
            ? d.emailReplyTo
            : typeof d.supportEmail === "string"
                ? d.supportEmail
                : undefined,
    };
}
async function loadUserPreferences(userId) {
    const snap = await db
        .collection("users")
        .doc(userId)
        .collection("preferences")
        .doc("notifications")
        .get();
    const d = snap.data() || {};
    return {
        orderUpdatesEmail: d.orderUpdatesEmail !== false,
        promotionalEmails: d.promotionalEmails === true,
        promotionalPush: d.promotionalPush !== false,
        smsOrderAlerts: d.smsOrderAlerts === true,
        whatsappOrderAlerts: d.whatsappOrderAlerts === true,
    };
}
async function loadUserContact(userId) {
    const [profileSnap, privateSnap] = await Promise.all([
        db.collection("users").doc(userId).get(),
        db.collection("users").doc(userId).collection("private").doc("info").get(),
    ]);
    const prof = profileSnap.data() || {};
    const priv = privateSnap.data() || {};
    return {
        name: typeof prof.displayName === "string" ? prof.displayName : undefined,
        email: typeof priv.email === "string" ? priv.email : undefined,
        phone: typeof priv.phone === "string" ? priv.phone : undefined,
    };
}
async function claimDedupeLog(logId, payload) {
    const ref = db.collection("communicationLogs").doc(logId);
    return db.runTransaction(async (tx) => {
        const existing = await tx.get(ref);
        if (existing.exists) {
            return false;
        }
        tx.set(ref, {
            ...payload,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return true;
    });
}
async function sendFcmPushForUser(params) {
    const logId = `log_push_${params.dedupeKey}`;
    const tokensSnap = await db
        .collection("users")
        .doc(params.userId)
        .collection("deviceTokens")
        .where("active", "==", true)
        .get();
    const tokens = [];
    tokensSnap.forEach((doc) => {
        const token = doc.data()?.token;
        if (typeof token === "string" && token.length >= 10) {
            tokens.push({ docId: doc.id, token });
        }
    });
    if (tokens.length === 0)
        return;
    const claimed = await claimDedupeLog(logId, {
        userId: params.userId,
        recipientMasked: `${tokens.length} device(s)`,
        orderId: params.orderId || null,
        channel: "PUSH",
        eventType: params.eventType,
        subject: params.title.slice(0, 150),
        bodyPreview: params.body.slice(0, 480),
        status: "QUEUED",
        providerName: "FCM",
        dedupeKey: params.dedupeKey,
        retryCount: 0,
    });
    if (!claimed)
        return;
    const response = await messaging.sendEachForMulticast({
        tokens: tokens.map((t) => t.token),
        notification: { title: params.title, body: params.body },
        data: {
            targetRoute: params.targetRoute,
            orderId: params.orderId || "",
            eventType: params.eventType,
        },
        android: {
            priority: "high",
            notification: {
                channelId: "zeecrown_orders",
            },
        },
    });
    const invalidCleanup = [];
    let firstError;
    response.responses.forEach((res, idx) => {
        if (!res.success && res.error) {
            if (!firstError)
                firstError = `${res.error.code}: ${res.error.message}`;
            if (res.error.code === "messaging/registration-token-not-registered" ||
                res.error.code === "messaging/invalid-registration-token") {
                const tokenDocId = tokens[idx].docId;
                invalidCleanup.push(db
                    .collection("users")
                    .doc(params.userId)
                    .collection("deviceTokens")
                    .doc(tokenDocId)
                    .delete());
            }
        }
    });
    await Promise.all([
        ...invalidCleanup,
        db.collection("communicationLogs").doc(logId).update({
            status: response.successCount > 0 ? "SENT" : "FAILED",
            failureReason: response.successCount > 0 ? firestore_1.FieldValue.delete() : (firstError || "Push delivery failed").slice(0, 380),
            lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        }),
    ]);
}
async function dispatchExternalChannels(params) {
    const [storeConfig, prefs] = await Promise.all([
        loadStoreConfig(),
        loadUserPreferences(params.userId),
    ]);
    const trackingLink = params.trackingToken
        ? `${APP_BASE_URL}/track?token=${encodeURIComponent(params.trackingToken)}`
        : `${APP_BASE_URL}/track`;
    // 1. Email Channel
    if (params.email && storeConfig.emailNotificationsEnabled) {
        const emailLogId = `log_email_${params.dedupeKey}`;
        if (!prefs.orderUpdatesEmail) {
            await claimDedupeLog(emailLogId, {
                userId: params.userId,
                recipientMasked: maskEmail(params.email),
                orderId: params.orderId || null,
                orderNumber: params.orderNumber,
                channel: "EMAIL",
                eventType: params.eventType,
                subject: `${params.headline} — ${params.orderNumber}`.slice(0, 150),
                bodyPreview: params.message.slice(0, 480),
                status: "SKIPPED_NO_CONSENT",
                providerName: (0, emailProvider_1.resolveEmailProvider)().name,
                failureReason: "Customer opted out of order update emails.",
                dedupeKey: params.dedupeKey,
                retryCount: 0,
            });
        }
        else {
            const provider = (0, emailProvider_1.resolveEmailProvider)();
            const claimed = await claimDedupeLog(emailLogId, {
                userId: params.userId,
                recipientMasked: maskEmail(params.email),
                orderId: params.orderId || null,
                orderNumber: params.orderNumber,
                channel: "EMAIL",
                eventType: params.eventType,
                subject: `${params.headline} — ${params.orderNumber}`.slice(0, 150),
                bodyPreview: params.message.slice(0, 480),
                status: "QUEUED",
                providerName: provider.name,
                dedupeKey: params.dedupeKey,
                retryCount: 0,
            });
            if (claimed) {
                const rendered = (0, emailTemplates_1.buildZeecrownEmail)({
                    storeName: storeConfig.storeName,
                    logoUrl: storeConfig.logoUrl,
                    customerName: params.customerName,
                    orderNumber: params.orderNumber,
                    headline: params.headline,
                    message: params.message,
                    totalFormatted: params.totalFormatted,
                    destinationMasked: params.destinationMasked,
                    courierName: params.courierName,
                    trackingReference: params.trackingReference,
                    estimatedDeliveryWindow: params.estimatedDeliveryWindow,
                    trackingLink,
                    supportEmail: storeConfig.supportEmail,
                });
                const result = await provider.send({
                    to: params.email,
                    subject: `${storeConfig.storeName}: ${params.headline} (${params.orderNumber})`,
                    html: rendered.html,
                    text: rendered.text,
                    senderName: storeConfig.emailSenderName,
                    replyTo: storeConfig.emailReplyTo,
                });
                await db.collection("communicationLogs").doc(emailLogId).update({
                    status: result.status,
                    providerName: result.providerName,
                    failureReason: result.failureReason || firestore_1.FieldValue.delete(),
                    lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
                    updatedAt: firestore_1.FieldValue.serverTimestamp(),
                });
            }
        }
    }
    // 2. SMS Channel (only if enabled by store + opted in by customer)
    if (params.phone && storeConfig.smsNotificationsEnabled && prefs.smsOrderAlerts) {
        const smsLogId = `log_sms_${params.dedupeKey}`;
        const smsProvider = (0, smsProvider_1.resolveSmsProvider)();
        const smsText = `${storeConfig.storeName}: ${params.headline} (${params.orderNumber}). ${params.message}`.slice(0, 300);
        const claimed = await claimDedupeLog(smsLogId, {
            userId: params.userId,
            recipientMasked: maskPhone(params.phone),
            orderId: params.orderId || null,
            orderNumber: params.orderNumber,
            channel: "SMS",
            eventType: params.eventType,
            subject: params.headline.slice(0, 150),
            bodyPreview: smsText,
            status: "QUEUED",
            providerName: smsProvider.name,
            dedupeKey: params.dedupeKey,
            retryCount: 0,
        });
        if (claimed) {
            const result = await smsProvider.send({ toPhone: params.phone, message: smsText });
            await db.collection("communicationLogs").doc(smsLogId).update({
                status: result.status,
                providerName: result.providerName,
                failureReason: result.failureReason || firestore_1.FieldValue.delete(),
                lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
                updatedAt: firestore_1.FieldValue.serverTimestamp(),
            });
        }
    }
    // 3. WhatsApp Business Cloud API Channel (only if enabled + opted in)
    if (params.phone && storeConfig.whatsappNotificationsEnabled && prefs.whatsappOrderAlerts) {
        const waLogId = `log_wa_${params.dedupeKey}`;
        const waProvider = (0, whatsappProvider_1.resolveWhatsAppProvider)();
        const waText = `*${storeConfig.storeName}* (${params.orderNumber})\n${params.headline}\n${params.message}\nTrack: ${trackingLink}`;
        const claimed = await claimDedupeLog(waLogId, {
            userId: params.userId,
            recipientMasked: maskPhone(params.phone),
            orderId: params.orderId || null,
            orderNumber: params.orderNumber,
            channel: "WHATSAPP",
            eventType: params.eventType,
            subject: params.headline.slice(0, 150),
            bodyPreview: waText.slice(0, 480),
            status: "QUEUED",
            providerName: waProvider.name,
            dedupeKey: params.dedupeKey,
            retryCount: 0,
        });
        if (claimed) {
            const result = await waProvider.send({
                toPhone: params.phone,
                templateName: "order_status_update",
                bodyText: waText,
            });
            await db.collection("communicationLogs").doc(waLogId).update({
                status: result.status,
                providerName: result.providerName,
                failureReason: result.failureReason || firestore_1.FieldValue.delete(),
                lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
                updatedAt: firestore_1.FieldValue.serverTimestamp(),
            });
        }
    }
}
exports.onOrderWritten = (0, firestore_2.onDocumentWritten)({ document: "orders/{orderId}", database: DATABASE_ID }, async (event) => {
    const after = event.data?.after?.data();
    const before = event.data?.before?.data();
    if (!after)
        return;
    const orderId = event.params.orderId;
    const userId = String(after.userId || "");
    const orderNumber = String(after.orderNumber || orderId);
    if (!userId)
        return;
    const contact = await loadUserContact(userId);
    const email = (typeof after.customerEmail === "string" && after.customerEmail) || contact.email;
    const phone = (typeof after.recipientPhone === "string" && after.recipientPhone) || contact.phone;
    const customerName = String(after.recipientName || contact.name || "Valued Client");
    const maskedDestination = [after.shippingCity, after.shippingState, after.shippingCountry]
        .filter((x) => typeof x === "string" && x.trim().length > 0)
        .join(", ");
    const totalFormatted = `NGN ${Number(after.totalNgn || 0).toLocaleString("en-NG")}`;
    const eventsToEmit = [];
    if (!before) {
        eventsToEmit.push({
            eventType: "ORDER_PLACED",
            category: "ORDER",
            headline: "Order Placed",
            message: `Your order ${orderNumber} (${totalFormatted}) has been placed. Complete or verify payment to begin tailored fulfillment.`,
            dedupeKey: `order_placed_${orderId}`,
        });
    }
    else {
        if (before.paymentStatus !== after.paymentStatus) {
            if (after.paymentStatus === "PAID") {
                eventsToEmit.push({
                    eventType: "PAYMENT_VERIFIED",
                    category: "PAYMENT",
                    headline: "Payment Verified",
                    message: `Payment for order ${orderNumber} has been verified. Thank you for shopping with ZEECROWN STORE.`,
                    dedupeKey: `pay_verified_${orderId}`,
                });
            }
            else if (after.paymentStatus === "FAILED") {
                eventsToEmit.push({
                    eventType: "PAYMENT_FAILED",
                    category: "PAYMENT",
                    headline: "Payment Attention Needed",
                    message: `Payment for order ${orderNumber} could not be confirmed yet. Open Track Order to retry payment safely or contact support.`,
                    dedupeKey: `pay_failed_${orderId}_${Date.now()}`,
                });
            }
        }
        if (before.status !== after.status) {
            const st = String(after.status || "");
            const copyByStatus = {
                CONFIRMED: {
                    headline: "Order Confirmed",
                    category: "ORDER",
                    msg: `Your order ${orderNumber} is confirmed and queued at the ZEECROWN atelier.`,
                },
                PROCESSING: {
                    headline: "Order Processing",
                    category: "ORDER",
                    msg: `Your pieces for ${orderNumber} are currently being prepared and quality checked.`,
                },
                READY_FOR_DISPATCH: {
                    headline: "Ready for Dispatch",
                    category: "DELIVERY",
                    msg: `Your parcel for ${orderNumber} is packed and awaiting courier pickup.`,
                },
                SHIPPED: {
                    headline: "Order Shipped",
                    category: "DELIVERY",
                    msg: `Order ${orderNumber} has been dispatched${after.courierName ? ` via ${after.courierName}` : ""}${after.trackingReference ? ` (Ref: ${after.trackingReference})` : ""}.`,
                },
                DISPATCHED: {
                    headline: "Order Dispatched",
                    category: "DELIVERY",
                    msg: `Order ${orderNumber} has left our studio${after.courierName ? ` via ${after.courierName}` : ""}.`,
                },
                OUT_FOR_DELIVERY: {
                    headline: "Out for Delivery",
                    category: "DELIVERY",
                    msg: `Your ZEECROWN parcel (${orderNumber}) is out for delivery today.`,
                },
                DELIVERED: {
                    headline: "Order Delivered",
                    category: "DELIVERY",
                    msg: `Order ${orderNumber} has been delivered. We hope you love wearing ZEECROWN!`,
                },
                CANCELLED: {
                    headline: "Order Cancelled",
                    category: "ORDER",
                    msg: `Order ${orderNumber} has been cancelled. Check order details or reach out to concierge support for assistance.`,
                },
                REFUNDED: {
                    headline: "Order Refunded",
                    category: "PAYMENT",
                    msg: `A refund update has been recorded on order ${orderNumber}.`,
                },
            };
            const info = copyByStatus[st];
            if (info) {
                eventsToEmit.push({
                    eventType: `STATUS_${st}`,
                    category: info.category,
                    headline: info.headline,
                    message: info.msg,
                    dedupeKey: `order_status_${st}_${orderId}`,
                });
            }
        }
    }
    for (const evt of eventsToEmit) {
        const notifRef = db.collection("notifications").doc(`notif_${evt.dedupeKey}`);
        await db.runTransaction(async (tx) => {
            const snap = await tx.get(notifRef);
            if (!snap.exists) {
                tx.set(notifRef, {
                    userId,
                    title: evt.headline,
                    body: evt.message,
                    category: evt.category,
                    readFlag: false,
                    targetRoute: `track_order/${orderId}`,
                    orderId,
                    orderNumber,
                    dedupeKey: evt.dedupeKey,
                    createdAt: firestore_1.FieldValue.serverTimestamp(),
                });
            }
        });
        await sendFcmPushForUser({
            userId,
            title: evt.headline,
            body: evt.message,
            targetRoute: `track_order/${orderId}`,
            orderId,
            eventType: evt.eventType,
            dedupeKey: evt.dedupeKey,
        });
        await dispatchExternalChannels({
            userId,
            customerName,
            email,
            phone,
            orderId,
            orderNumber,
            trackingToken: typeof after.trackingToken === "string" ? after.trackingToken : undefined,
            totalFormatted,
            destinationMasked: maskedDestination || undefined,
            courierName: typeof after.courierName === "string" ? after.courierName : undefined,
            trackingReference: typeof after.trackingReference === "string" ? after.trackingReference : undefined,
            estimatedDeliveryWindow: typeof after.estimatedDeliveryWindow === "string" ? after.estimatedDeliveryWindow : undefined,
            eventType: evt.eventType,
            headline: evt.headline,
            message: evt.message,
            dedupeKey: evt.dedupeKey,
        });
    }
});
exports.onSupportReplyCreated = (0, firestore_2.onDocumentCreated)({ document: "supportEnquiries/{enquiryId}/replies/{replyId}", database: DATABASE_ID }, async (event) => {
    const reply = event.data?.data();
    if (!reply || reply.senderRole !== "ADMIN")
        return;
    const enquiryId = event.params.enquiryId;
    const replyId = event.params.replyId;
    const ownerUid = String(reply.ownerUid || "");
    if (!ownerUid)
        return;
    const enquirySnap = await db.collection("supportEnquiries").doc(enquiryId).get();
    const enquiry = enquirySnap.data();
    if (!enquiry)
        return;
    const dedupeKey = `support_reply_${enquiryId}_${replyId}`;
    const title = `Support Reply: ${String(enquiry.subject || "Enquiry")}`.slice(0, 115);
    const body = String(reply.message || "We replied to your enquiry.").slice(0, 380);
    await db.collection("notifications").doc(`notif_${dedupeKey}`).set({
        userId: ownerUid,
        title,
        body,
        category: "SUPPORT",
        readFlag: false,
        targetRoute: "support",
        orderId: typeof enquiry.orderId === "string" ? enquiry.orderId : null,
        orderNumber: typeof enquiry.orderNumber === "string" ? enquiry.orderNumber : null,
        dedupeKey,
        createdAt: firestore_1.FieldValue.serverTimestamp(),
    }, { merge: false }).catch(() => undefined);
    await sendFcmPushForUser({
        userId: ownerUid,
        title,
        body,
        targetRoute: "support",
        eventType: "SUPPORT_REPLY",
        dedupeKey,
    });
});
exports.courierWebhook = (0, https_1.onRequest)(async (req, res) => {
    if (req.method !== "POST") {
        res.status(405).json({ error: "Method not allowed" });
        return;
    }
    const configuredSecret = (process.env.COURIER_WEBHOOK_SECRET || "").trim();
    const providedSecret = String(req.header("x-zeecrown-courier-secret") || "");
    if (!configuredSecret) {
        res.status(503).json({ error: "COURIER_WEBHOOK_SECRET is not configured." });
        return;
    }
    const expectedBuf = Buffer.from(configuredSecret);
    const actualBuf = Buffer.from(providedSecret);
    if (expectedBuf.length !== actualBuf.length || !crypto.timingSafeEqual(expectedBuf, actualBuf)) {
        res.status(401).json({ error: "Unauthorized webhook signature." });
        return;
    }
    const payload = req.body;
    if (!payload?.orderNumber || !payload?.deliveryStatus) {
        res.status(400).json({ error: "Missing orderNumber or deliveryStatus." });
        return;
    }
    const ordersQuery = await db
        .collection("orders")
        .where("orderNumber", "==", payload.orderNumber.trim())
        .limit(1)
        .get();
    if (ordersQuery.empty) {
        res.status(404).json({ error: "Order not found." });
        return;
    }
    const orderDoc = ordersQuery.docs[0];
    const existing = orderDoc.data();
    const courierName = (payload.courierName || existing.courierName || "Courier").trim();
    const adapter = new courierProvider_1.DefaultCourierAdapter(courierName);
    const trackingReference = (payload.trackingReference || existing.trackingReference || "").trim();
    const resolvedUrl = payload.trackingUrl || adapter.buildTrackingUrl(trackingReference) || existing.trackingUrl;
    const mappedStatus = adapter.mapDeliveryStatusToOrderStatus(payload.deliveryStatus) || existing.status;
    const history = Array.isArray(existing.statusHistory) ? [...existing.statusHistory] : [];
    history.push({
        stage: mappedStatus,
        label: payload.deliveryStatus,
        note: payload.note || `Courier update via ${courierName}`,
        actorRole: "COURIER_WEBHOOK",
        timestampMillis: Date.now(),
    });
    const updates = {
        courierName,
        deliveryStatus: payload.deliveryStatus,
        status: mappedStatus,
        statusHistory: history.slice(-30),
        updatedAt: firestore_1.FieldValue.serverTimestamp(),
    };
    if (trackingReference)
        updates.trackingReference = trackingReference;
    if (resolvedUrl)
        updates.trackingUrl = resolvedUrl;
    if (payload.estimatedDeliveryWindow)
        updates.estimatedDeliveryWindow = payload.estimatedDeliveryWindow;
    if (payload.note)
        updates.deliveryNotes = payload.note;
    if (mappedStatus === "SHIPPED" && !existing.dispatchedAt)
        updates.dispatchedAt = firestore_1.FieldValue.serverTimestamp();
    if (mappedStatus === "DELIVERED" && !existing.deliveredAt)
        updates.deliveredAt = firestore_1.FieldValue.serverTimestamp();
    await orderDoc.ref.update(updates);
    const projectionUpdates = {
        status: mappedStatus,
        deliveryStatus: payload.deliveryStatus,
        courierName,
        statusHistory: history.slice(-30),
        updatedAt: firestore_1.FieldValue.serverTimestamp(),
    };
    if (trackingReference)
        projectionUpdates.trackingReference = trackingReference;
    if (resolvedUrl)
        projectionUpdates.trackingUrl = resolvedUrl;
    if (payload.estimatedDeliveryWindow)
        projectionUpdates.estimatedDeliveryWindow = payload.estimatedDeliveryWindow;
    if (payload.note)
        projectionUpdates.deliveryNotes = payload.note;
    const syncTargets = [];
    if (typeof existing.trackingToken === "string" && existing.trackingToken) {
        syncTargets.push(db.collection("orderTrackingTokens").doc(existing.trackingToken).update(projectionUpdates));
    }
    if (typeof existing.guestLookupHashEmail === "string" && existing.guestLookupHashEmail) {
        syncTargets.push(db.collection("orderTrackingLookups").doc(existing.guestLookupHashEmail).update(projectionUpdates));
    }
    if (typeof existing.guestLookupHashPhone === "string" && existing.guestLookupHashPhone) {
        syncTargets.push(db.collection("orderTrackingLookups").doc(existing.guestLookupHashPhone).update(projectionUpdates));
    }
    await Promise.all(syncTargets);
    res.status(200).json({ ok: true, orderId: orderDoc.id, status: mappedStatus });
});
exports.lookupGuestOrder = (0, https_1.onRequest)({ cors: true }, async (req, res) => {
    if (req.method !== "POST") {
        res.status(405).json({ error: "Method not allowed" });
        return;
    }
    const clientKey = sha256Prefix(String(req.ip || "unknown"), 24);
    const rateRef = db.collection("guestTrackingRateLimits").doc(clientKey);
    const nowMs = Date.now();
    const allowed = await db.runTransaction(async (tx) => {
        const snap = await tx.get(rateRef);
        const data = snap.data() || {};
        const windowStart = typeof data.windowStartMs === "number" ? data.windowStartMs : 0;
        const count = typeof data.count === "number" ? data.count : 0;
        if (nowMs - windowStart > 60_000) {
            tx.set(rateRef, { windowStartMs: nowMs, count: 1, updatedAt: firestore_1.Timestamp.now() });
            return true;
        }
        if (count >= 12)
            return false;
        tx.set(rateRef, { windowStartMs: windowStart, count: count + 1, updatedAt: firestore_1.Timestamp.now() });
        return true;
    });
    if (!allowed) {
        res.status(429).json({ error: "Too many tracking attempts. Please wait a minute and try again." });
        return;
    }
    const body = req.body;
    if (body?.trackingToken && body.trackingToken.trim().length >= 8) {
        const tokenSnap = await db.collection("orderTrackingTokens").doc(body.trackingToken.trim()).get();
        if (!tokenSnap.exists) {
            res.status(404).json({ error: "No matching order found for that tracking token." });
            return;
        }
        res.status(200).json({ projection: tokenSnap.data() });
        return;
    }
    const orderNum = String(body?.orderNumber || "").trim().toUpperCase();
    const contact = String(body?.contact || "").trim().toLowerCase();
    if (!orderNum || !contact) {
        res.status(400).json({ error: "Provide either a trackingToken or both orderNumber and email/phone." });
        return;
    }
    const lookupId = `trk_${sha256Prefix(`${orderNum}:${contact}`, 32)}`;
    const snap = await db.collection("orderTrackingLookups").doc(lookupId).get();
    if (!snap.exists) {
        res.status(404).json({ error: "No matching order found for the details entered." });
        return;
    }
    res.status(200).json({ projection: snap.data() });
});
exports.retryCommunicationLog = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Authentication required.");
    }
    const callerEmail = request.auth.token?.email;
    const isBootstrapped = request.auth.token?.email_verified === true && callerEmail === BOOTSTRAPPED_ADMIN_EMAIL;
    if (!isBootstrapped) {
        const adminSnap = await db.collection("adminUsers").doc(request.auth.uid).get();
        if (!adminSnap.exists || adminSnap.data()?.active !== true) {
            throw new https_1.HttpsError("permission-denied", "Active admin role required.");
        }
    }
    const logId = String(request.data?.logId || "").trim();
    if (!logId) {
        throw new https_1.HttpsError("invalid-argument", "logId is required.");
    }
    const logRef = db.collection("communicationLogs").doc(logId);
    const logSnap = await logRef.get();
    if (!logSnap.exists) {
        throw new https_1.HttpsError("not-found", "Communication log not found.");
    }
    const log = logSnap.data();
    const currentRetries = Number(log.retryCount || 0);
    if (currentRetries >= 20) {
        throw new https_1.HttpsError("failed-precondition", "Maximum retry attempts reached.");
    }
    const channel = String(log.channel || "");
    const userId = String(log.userId || "");
    const [contact, storeConfig] = await Promise.all([
        loadUserContact(userId),
        loadStoreConfig(),
    ]);
    if (channel === "EMAIL") {
        if (!contact.email) {
            await logRef.update({
                status: "FAILED",
                failureReason: "Customer email missing.",
                retryCount: currentRetries + 1,
                lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
                updatedAt: firestore_1.FieldValue.serverTimestamp(),
            });
            return { status: "FAILED", reason: "Customer email missing." };
        }
        const provider = (0, emailProvider_1.resolveEmailProvider)();
        const rendered = (0, emailTemplates_1.buildZeecrownEmail)({
            storeName: storeConfig.storeName,
            logoUrl: storeConfig.logoUrl,
            customerName: contact.name || "Valued Client",
            orderNumber: String(log.orderNumber || log.orderId || "ORDER"),
            headline: String(log.subject || "Order Update"),
            message: String(log.bodyPreview || "Please sign in to view your latest order update."),
            supportEmail: storeConfig.supportEmail,
        });
        const result = await provider.send({
            to: contact.email,
            subject: String(log.subject || `${storeConfig.storeName} Update`),
            html: rendered.html,
            text: rendered.text,
            senderName: storeConfig.emailSenderName,
            replyTo: storeConfig.emailReplyTo,
        });
        await logRef.update({
            status: result.status,
            providerName: result.providerName,
            failureReason: result.failureReason || firestore_1.FieldValue.delete(),
            retryCount: currentRetries + 1,
            lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return { status: result.status, providerName: result.providerName, failureReason: result.failureReason || null };
    }
    if (channel === "SMS") {
        if (!contact.phone) {
            await logRef.update({
                status: "FAILED",
                failureReason: "Customer phone number missing.",
                retryCount: currentRetries + 1,
                lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
                updatedAt: firestore_1.FieldValue.serverTimestamp(),
            });
            return { status: "FAILED", reason: "Customer phone missing." };
        }
        const smsProvider = (0, smsProvider_1.resolveSmsProvider)();
        const result = await smsProvider.send({
            toPhone: contact.phone,
            message: String(log.bodyPreview || log.subject || "ZEECROWN STORE order update"),
        });
        await logRef.update({
            status: result.status,
            providerName: result.providerName,
            failureReason: result.failureReason || firestore_1.FieldValue.delete(),
            retryCount: currentRetries + 1,
            lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return { status: result.status, providerName: result.providerName, failureReason: result.failureReason || null };
    }
    if (channel === "WHATSAPP") {
        if (!contact.phone) {
            await logRef.update({
                status: "FAILED",
                failureReason: "Customer WhatsApp phone number missing.",
                retryCount: currentRetries + 1,
                lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
                updatedAt: firestore_1.FieldValue.serverTimestamp(),
            });
            return { status: "FAILED", reason: "Customer phone missing." };
        }
        const waProvider = (0, whatsappProvider_1.resolveWhatsAppProvider)();
        const result = await waProvider.send({
            toPhone: contact.phone,
            templateName: "order_status_update",
            bodyText: String(log.bodyPreview || log.subject || "ZEECROWN STORE update"),
        });
        await logRef.update({
            status: result.status,
            providerName: result.providerName,
            failureReason: result.failureReason || firestore_1.FieldValue.delete(),
            retryCount: currentRetries + 1,
            lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return { status: result.status, providerName: result.providerName, failureReason: result.failureReason || null };
    }
    await logRef.update({
        retryCount: currentRetries + 1,
        lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
        updatedAt: firestore_1.FieldValue.serverTimestamp(),
    });
    return { status: String(log.status || "QUEUED") };
});
// ============================================================================
// PROMPT 10: PUBLIC WEB SSR SEO PAGES, SITEMAP.XML & ROBOTS.TXT ENDPOINTS
// ============================================================================
exports.seoServe = (0, https_1.onRequest)({ cors: true }, async (req, res) => {
    await (0, seoRenderer_1.handleSeoPageRequest)(db, req, res);
});
exports.sitemapXml = (0, https_1.onRequest)({ cors: true }, async (req, res) => {
    await (0, sitemapAndRobots_1.handleSitemapRequest)(db, req, res);
});
exports.robotsTxt = (0, https_1.onRequest)({ cors: true }, async (req, res) => {
    await (0, sitemapAndRobots_1.handleRobotsTxtRequest)(db, req, res);
});
// ============================================================================
// PROMPT 11: SERVER-SIDE ZEECROWN AI ASSISTANT ENDPOINTS (PUBLIC WEB + API)
// ============================================================================
exports.aiAssistantChat = (0, https_1.onRequest)({ cors: true }, async (req, res) => {
    await (0, aiEngine_1.handleAiChatRequest)(db, req, res);
});
exports.aiAssistantEscalate = (0, https_1.onRequest)({ cors: true }, async (req, res) => {
    await (0, aiEngine_1.handleAiEscalateRequest)(db, req, res);
});
exports.analyzeProductPhotos = (0, https_1.onRequest)({ cors: true }, async (req, res) => {
    await (0, productAnalyzer_1.handleAnalyzeProductRequest)(db, req, res);
});
exports.onSupportEnquiryCreated = (0, firestore_2.onDocumentCreated)({ document: "supportEnquiries/{enquiryId}", database: DATABASE_ID }, async (event) => {
    const enquiry = event.data?.data();
    if (!enquiry)
        return;
    const enquiryId = event.params.enquiryId;
    const ownerUid = String(enquiry.userId || "guest");
    const customerName = String(enquiry.customerName || "Customer").slice(0, 80);
    const subject = String(enquiry.subject || "Customer Support Ticket").slice(0, 140);
    const priority = String(enquiry.priority || "NORMAL").toUpperCase();
    const sourceChannel = String(enquiry.sourceChannel || "IN_APP").toUpperCase();
    const dedupeKey = `support_ticket_created_${enquiryId}`;
    const logId = `log_admin_support_${enquiryId}`;
    const storeConfig = await loadStoreConfig();
    const supportEmail = storeConfig.supportEmail;
    const claimed = await claimDedupeLog(logId, {
        userId: ownerUid,
        recipientMasked: supportEmail ? maskEmail(supportEmail) : "Admin Inbox",
        channel: supportEmail && storeConfig.emailNotificationsEnabled ? "EMAIL" : "IN_APP",
        eventType: "SUPPORT_TICKET_CREATED",
        subject: `[${priority}] ${subject}`.slice(0, 150),
        bodyPreview: `${customerName} (${sourceChannel}): ${String(enquiry.message || "").slice(0, 320)}`,
        status: "QUEUED",
        providerName: supportEmail ? (0, emailProvider_1.resolveEmailProvider)().name : "FirestoreSupportInbox",
        dedupeKey,
        retryCount: 0,
    });
    if (!claimed)
        return;
    if (supportEmail && storeConfig.emailNotificationsEnabled) {
        const provider = (0, emailProvider_1.resolveEmailProvider)();
        const rendered = (0, emailTemplates_1.buildZeecrownEmail)({
            storeName: storeConfig.storeName,
            logoUrl: storeConfig.logoUrl,
            customerName: "ZEECROWN Support Team",
            orderNumber: String(enquiry.orderNumber || `TKT-${enquiryId.slice(0, 8).toUpperCase()}`),
            headline: `New ${priority} Support Ticket (${sourceChannel})`,
            message: `From: ${customerName}\nSubject: ${subject}\n\n${String(enquiry.message || "").slice(0, 1000)}`,
            supportEmail,
        });
        const result = await provider.send({
            to: supportEmail,
            subject: `[${storeConfig.storeName}] ${priority} Support Ticket: ${subject}`.slice(0, 150),
            html: rendered.html,
            text: rendered.text,
            senderName: storeConfig.emailSenderName,
            replyTo: typeof enquiry.customerEmail === "string" && enquiry.customerEmail ? enquiry.customerEmail : storeConfig.emailReplyTo,
        });
        await db.collection("communicationLogs").doc(logId).update({
            status: result.status,
            providerName: result.providerName,
            failureReason: result.failureReason || firestore_1.FieldValue.delete(),
            lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
    }
    else {
        await db.collection("communicationLogs").doc(logId).update({
            status: "SENT",
            lastAttemptAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
    }
});
//# sourceMappingURL=index.js.map