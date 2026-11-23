package com.example

import com.example.data.model.DefaultCourierTrackingAdapter
import com.example.data.model.GuestTrackingRateLimiter
import com.example.data.model.OrderTimelineEngine
import com.example.data.model.OrderTimelineStage
import com.example.data.model.TrackingSecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase08CommunicationAndTrackingTests {

    @Test
    fun `order timeline engine progresses stages chronologically and marks terminal cancellation safely`() {
        val now = 1_750_000_000_000L
        val summary = OrderTimelineEngine.buildTimeline(
            statusRaw = "SHIPPED",
            createdAtMillis = now - 7200_000L,
            dispatchedAtMillis = now - 1800_000L,
            deliveredAtMillis = null,
            history = listOf(
                OrderTimelineEngine.historyEntry(
                    stage = "CONFIRMED",
                    label = "Confirmed",
                    note = "Transfer verified by finance team",
                    actorRole = "order_manager",
                    timestampMillis = now - 5400_000L,
                )
            ),
        )

        assertFalse(summary.isCancelled)
        assertFalse(summary.isRefunded)
        assertEquals(7, summary.steps.size)

        val confirmed = summary.steps.first { it.stage == OrderTimelineStage.ORDER_CONFIRMED }
        assertTrue(confirmed.isCompleted)
        assertFalse(confirmed.isCurrent)
        assertEquals("Transfer verified by finance team", confirmed.note)

        val shipped = summary.steps.first { it.stage == OrderTimelineStage.SHIPPED }
        assertTrue(shipped.isCompleted)
        assertTrue(shipped.isCurrent)
        assertEquals(now - 1800_000L, shipped.timestampMillis)

        val delivered = summary.steps.first { it.stage == OrderTimelineStage.DELIVERED }
        assertFalse(delivered.isCompleted)
        assertFalse(delivered.isCurrent)

        val cancelledSummary = OrderTimelineEngine.buildTimeline(
            statusRaw = "CANCELLED",
            createdAtMillis = now - 5000L,
            dispatchedAtMillis = null,
            deliveredAtMillis = null,
            history = listOf(
                OrderTimelineEngine.historyEntry(
                    stage = "CANCELLED",
                    label = "Cancelled",
                    note = "Customer requested size exchange refund",
                    actorRole = "order_manager",
                    timestampMillis = now,
                )
            ),
        )
        assertTrue(cancelledSummary.isCancelled)
        assertEquals("Customer requested size exchange refund", cancelledSummary.terminalNote)
    }

    @Test
    fun `tracking security utils normalize contacts build deterministic lookup ids and mask pii`() {
        val idA = TrackingSecurityUtils.buildLookupId("  zc-2026-0091 ", "+234 801 234 5678")
        val idB = TrackingSecurityUtils.buildLookupId("ZC-2026-0091", "08012345678")
        val idEmail = TrackingSecurityUtils.buildLookupId("ZC-2026-0091", " Amaka@Example.com ")
        assertTrue(idA.startsWith("trk_"))
        assertEquals(idA, idB)
        assertNotEquals(idA, idEmail)
        assertEquals("", TrackingSecurityUtils.buildLookupId("", "amaka@example.com"))

        val token1 = TrackingSecurityUtils.generateTrackingToken()
        val token2 = TrackingSecurityUtils.generateTrackingToken()
        assertTrue(token1.startsWith("tok_"))
        assertNotEquals(token1, token2)

        assertEquals("Lekki, Lagos, Nigeria", TrackingSecurityUtils.maskDestination("Lekki ", " Lagos", "Nigeria"))
        assertEquals("Destination confirmed on order", TrackingSecurityUtils.maskDestination("", " ", ""))
        assertEquals("am***@zeecrown.ng", TrackingSecurityUtils.maskEmail("amaka@zeecrown.ng"))
    }

    @Test
    fun `guest tracking rate limiter enforces window limit and unlocks after window elapses`() {
        var fakeNow = 10_000L
        val limiter = GuestTrackingRateLimiter(
            maxAttemptsPerWindow = 3,
            windowMillis = 30_000L,
            timeProvider = { fakeNow },
        )

        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
        assertTrue(limiter.remainingWaitSeconds() in 29..30)

        fakeNow += 31_000L
        assertEquals(0, limiter.remainingWaitSeconds())
        assertTrue(limiter.tryAcquire())
    }

    @Test
    fun `courier tracking adapter prefers explicit url and resolves known courier carriers`() {
        assertEquals(
            "https://custom.track/ABC123",
            DefaultCourierTrackingAdapter.resolveTrackingUrl("DHL", "ABC123", "https://custom.track/ABC123"),
        )
        assertEquals(
            "https://www.dhl.com/ng-en/home/tracking.html?tracking-id=DHL99",
            DefaultCourierTrackingAdapter.resolveTrackingUrl("DHL Express", "DHL99", ""),
        )
        assertEquals(
            "https://giglogistics.com/track?waybill=GIG44",
            DefaultCourierTrackingAdapter.resolveTrackingUrl("GIG Logistics", "GIG44", ""),
        )
        assertEquals("", DefaultCourierTrackingAdapter.resolveTrackingUrl("Local Rider", "", ""))
    }
}
