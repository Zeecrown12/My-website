package com.example

import com.example.data.model.AdminAnalyticsEngine
import com.example.data.model.AdminNotificationEngine
import com.example.data.model.AdminRole
import com.example.data.model.AdminSection
import com.example.data.model.AnalyticsDateRange
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.CustomerAdminSummary
import com.example.data.model.InventoryRecord
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.StoreSettings
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase07AdminDashboardTests {

    @Test
    fun `admin role rbac enforces granular section and action permissions`() {
        assertTrue(AdminRole.SUPER_ADMIN.isAdmin)
        assertTrue(AdminRole.SUPER_ADMIN.canManageTeam)
        AdminSection.entries.forEach { section ->
            assertTrue("Super admin should access $section", AdminRole.SUPER_ADMIN.canAccessSection(section))
        }

        // Store manager has broad store control except team assignment
        assertFalse(AdminRole.STORE_MANAGER.canManageTeam)
        assertTrue(AdminRole.STORE_MANAGER.canManageProducts)
        assertTrue(AdminRole.STORE_MANAGER.canManageSettings)
        assertTrue(AdminRole.STORE_MANAGER.canViewAuditLogs)

        // Order manager focuses on orders, payments, customers, reports
        assertTrue(AdminRole.ORDER_MANAGER.canManageOrders)
        assertTrue(AdminRole.ORDER_MANAGER.canVerifyPayments)
        assertTrue(AdminRole.ORDER_MANAGER.canViewCustomers)
        assertTrue(AdminRole.ORDER_MANAGER.canViewReports)
        assertFalse(AdminRole.ORDER_MANAGER.canManageProducts)
        assertFalse(AdminRole.ORDER_MANAGER.canManageSettings)
        assertFalse(AdminRole.ORDER_MANAGER.canManageTeam)

        // Content manager focuses on categories, promos, homepage content, reviews
        assertTrue(AdminRole.CONTENT_MANAGER.canManageCategories)
        assertTrue(AdminRole.CONTENT_MANAGER.canManagePromos)
        assertTrue(AdminRole.CONTENT_MANAGER.canManageContent)
        assertTrue(AdminRole.CONTENT_MANAGER.canModerateReviews)
        assertFalse(AdminRole.CONTENT_MANAGER.canManageOrders)
        assertFalse(AdminRole.CONTENT_MANAGER.canVerifyPayments)

        // Support staff can view customers, edit support notes, moderate reviews, view audit log
        assertTrue(AdminRole.SUPPORT_STAFF.canViewCustomers)
        assertTrue(AdminRole.SUPPORT_STAFF.canEditCustomerNotes)
        assertTrue(AdminRole.SUPPORT_STAFF.canModerateReviews)
        assertFalse(AdminRole.SUPPORT_STAFF.canManageProducts)
        assertFalse(AdminRole.SUPPORT_STAFF.canManageDelivery)

        // Ordinary customer has no admin access
        assertFalse(AdminRole.NONE.isAdmin)
        AdminSection.entries.forEach { section ->
            assertFalse("Customer should not access $section", AdminRole.NONE.canAccessSection(section))
        }
        assertEquals(AdminRole.STORE_MANAGER, AdminRole.fromRaw("manager"))
        assertEquals(AdminRole.NONE, AdminRole.fromRaw("customer"))
    }

    @Test
    fun `analytics engine counts only verified paid non-cancelled revenue and subtracts refunds`() {
        val nowMs = 1_750_000_000_000L
        val withinRangeTs = Timestamp(Date(nowMs - 3_600_000L))
        val oldTs = Timestamp(Date(nowMs - 45L * 86_400_000L))

        val orders = listOf(
            // Paid & confirmed -> counted (80,000)
            Order(
                id = "o1",
                userId = "u1",
                orderNumber = "ZC-1001",
                totalNgn = 80_000.0,
                paymentMethod = PaymentMethod.PAYSTACK.name,
                paymentStatus = PaymentStatus.PAID.name,
                status = OrderStatus.CONFIRMED.name,
                createdAt = withinRangeTs,
            ),
            // Partially refunded but paid & delivered (100,000 paid, 20,000 refunded -> net 80,000)
            Order(
                id = "o2",
                userId = "u1",
                orderNumber = "ZC-1002",
                totalNgn = 100_000.0,
                refundedNgn = 20_000.0,
                paymentMethod = PaymentMethod.BANK_TRANSFER.name,
                paymentStatus = PaymentStatus.PARTIALLY_REFUNDED.name,
                status = OrderStatus.DELIVERED.name,
                createdAt = withinRangeTs,
            ),
            // Unpaid pending payment -> excluded from verified revenue
            Order(
                id = "o3",
                userId = "u2",
                orderNumber = "ZC-1003",
                totalNgn = 50_000.0,
                paymentMethod = PaymentMethod.BANK_TRANSFER.name,
                paymentStatus = PaymentStatus.PENDING_VERIFICATION.name,
                status = OrderStatus.PAYMENT_SUBMITTED.name,
                createdAt = withinRangeTs,
            ),
            // Cancelled order even if tagged paid -> excluded from verified revenue
            Order(
                id = "o4",
                userId = "u3",
                orderNumber = "ZC-1004",
                totalNgn = 45_000.0,
                paymentMethod = PaymentMethod.FLUTTERWAVE.name,
                paymentStatus = PaymentStatus.PAID.name,
                status = OrderStatus.CANCELLED.name,
                createdAt = withinRangeTs,
            ),
            // Fully refunded -> excluded from verified revenue and contributes 30,000 to refundedTotalNgn
            Order(
                id = "o5",
                userId = "u4",
                orderNumber = "ZC-1005",
                totalNgn = 30_000.0,
                refundedNgn = 30_000.0,
                paymentMethod = PaymentMethod.PAYSTACK.name,
                paymentStatus = PaymentStatus.REFUNDED.name,
                status = OrderStatus.REFUNDED.name,
                createdAt = withinRangeTs,
            ),
            // Outside 7-day range -> excluded in LAST_7_DAYS
            Order(
                id = "o6",
                userId = "u5",
                orderNumber = "ZC-0999",
                totalNgn = 120_000.0,
                paymentMethod = PaymentMethod.PAYSTACK.name,
                paymentStatus = PaymentStatus.PAID.name,
                status = OrderStatus.DELIVERED.name,
                createdAt = oldTs,
            ),
        )

        val payments = listOf(
            PaymentRecord(
                id = "pay1",
                orderId = "o3",
                status = PaymentRecordStatus.SUBMITTED.name,
                amountNgn = 50_000.0,
            ),
            PaymentRecord(
                id = "pay2",
                orderId = "o1",
                status = PaymentRecordStatus.VERIFIED.name,
                amountNgn = 80_000.0,
            ),
        )

        val categories = listOf(
            Category(id = "cat_bags", name = "Bags"),
            Category(id = "cat_shoes", name = "Shoes"),
        )
        val products = listOf(
            Product(id = "p1", title = "Crown Tote", categoryId = "cat_bags", priceNgn = 45_000.0, soldCount = 5L, active = true, lowStockThreshold = 3L),
            Product(id = "p2", title = "Lagos Heels", categoryId = "cat_shoes", priceNgn = 35_000.0, soldCount = 2L, active = true, lowStockThreshold = 3L),
            Product(id = "p3", title = "Draft Bag", categoryId = "cat_bags", priceNgn = 25_000.0, soldCount = 0L, active = false, lowStockThreshold = 3L),
        )
        val inventory = mapOf(
            "p1" to InventoryRecord(productId = "p1", stockQuantity = 2L, lowStockThreshold = 3L), // low stock
            "p2" to InventoryRecord(productId = "p2", stockQuantity = 0L, lowStockThreshold = 3L), // out of stock
            "p3" to InventoryRecord(productId = "p3", stockQuantity = 10L, lowStockThreshold = 3L),
        )
        val customers = listOf(
            CustomerAdminSummary(
                profile = UserProfile(id = "u1", userId = "u1", createdAt = withinRangeTs),
                privateInfo = null,
                addresses = emptyList(),
                orders = emptyList(),
                totalOrderCount = 2,
                paidOrderCount = 2,
                lifetimeSpendPaidNgn = 180_000.0,
            ),
            CustomerAdminSummary(
                profile = UserProfile(id = "u5", userId = "u5", createdAt = oldTs),
                privateInfo = null,
                addresses = emptyList(),
                orders = emptyList(),
                totalOrderCount = 1,
                paidOrderCount = 1,
                lifetimeSpendPaidNgn = 120_000.0,
            ),
        )

        val report = AdminAnalyticsEngine.generateReport(
            range = AnalyticsDateRange.LAST_7_DAYS,
            orders = orders,
            payments = payments,
            products = products,
            categories = categories,
            inventoryMap = inventory,
            customers = customers,
            nowMillis = nowMs,
        )

        assertEquals(5, report.totalOrders)
        assertEquals(2, report.paidOrdersCount)
        assertEquals(260_000.0, report.grossOrderValueNgn, 0.01) // 80k + 100k + 50k + 30k (excl 45k cancelled)
        assertEquals(180_000.0, report.verifiedPaidRevenueNgn, 0.01)
        assertEquals(50_000.0, report.unpaidOrdersValueNgn, 0.01)
        assertEquals(50_000.0, report.refundedTotalNgn, 0.01)
        assertEquals(130_000.0, report.netPaidRevenueNgn, 0.01)
        assertEquals(90_000.0, report.averageOrderValueNgn, 0.01)
        assertEquals(1, report.pendingPaymentOrders)
        assertEquals(1, report.pendingVerificationPayments)
        assertEquals(1, report.cancelledOrders)
        assertEquals(1, report.refundedOrders)
        assertEquals(1, report.completedDeliveredOrders)
        assertEquals(1, report.processingOrders)
        assertEquals(2, report.publishedProductsCount)
        assertEquals(1, report.lowStockCount)
        assertEquals(1, report.outOfStockCount)
        assertEquals(2, report.totalCustomers)
        assertEquals(1, report.newCustomersInRange)
        assertEquals(1, report.returningCustomersInRange)
        assertEquals("cat_bags", report.categoryBreakdown.first().categoryId)
        assertEquals(5L, report.categoryBreakdown.first().unitsSold)
        assertTrue(report.dailyOrderCounts.isNotEmpty())
        assertEquals(5, report.dailyOrderCounts.sumOf { it.second })

        val paystackMetric = report.paymentMethodBreakdown.first { it.method == PaymentMethod.PAYSTACK }
        assertEquals(2, paystackMetric.orderCount)
        assertEquals(80_000.0, paystackMetric.paidAmountNgn, 0.01)
    }

    @Test
    fun `csv export builders escape commas quotes and newlines cleanly`() {
        val order = Order(
            id = "o1",
            userId = "u1",
            orderNumber = "ZC-5001",
            recipientName = "Adaeze, \"Zizi\" Okafor",
            shippingCity = "Ikoyi",
            shippingState = "Lagos",
            shippingCountry = "Nigeria",
            paymentMethod = PaymentMethod.PAYSTACK.name,
            paymentStatus = PaymentStatus.PAID.name,
            status = OrderStatus.CONFIRMED.name,
            itemsSummary = "1x Silk Bubu\n1x Tote",
            itemCount = 2L,
            subtotalNgn = 90_000.0,
            deliveryFeeNgn = 3_500.0,
            discountNgn = 5_000.0,
            totalNgn = 88_500.0,
            refundedNgn = 0.0,
        )

        val ordersCsv = AdminAnalyticsEngine.buildOrdersCsv(listOf(order))
        assertTrue(ordersCsv.startsWith("Order Number,Customer,City,State"))
        assertTrue(ordersCsv.contains("\"Adaeze, \"\"Zizi\"\" Okafor\""))
        assertTrue(ordersCsv.contains("1x Silk Bubu 1x Tote"))
        assertTrue(ordersCsv.contains("ZC-5001"))

        val report = AdminAnalyticsEngine.generateReport(
            range = AnalyticsDateRange.ALL_TIME,
            orders = listOf(order),
            payments = emptyList(),
            products = emptyList(),
            categories = listOf(Category(id = "c1", name = "Adire, Silk")),
            inventoryMap = emptyMap(),
            customers = emptyList(),
        )
        val salesCsv = AdminAnalyticsEngine.buildSalesReportCsv(report)
        assertTrue(salesCsv.contains("Verified Paid Revenue (NGN),88500.0"))
        assertTrue(salesCsv.contains("\"Adire, Silk\",0,0,0.0"))
    }

    @Test
    fun `coupon validity checks active flag date bounds and usage limits`() {
        val now = 1_750_000_000_000L
        val valid = Coupon(
            code = "WELCOME10",
            active = true,
            startsAtMillis = now - 10_000L,
            expiresAtMillis = now + 10_000L,
            usageLimit = 50L,
            usageCount = 12L,
        )
        assertTrue(valid.isCurrentlyValid(now))

        assertFalse(valid.copy(active = false).isCurrentlyValid(now))
        assertFalse(valid.copy(startsAtMillis = now + 5_000L).isCurrentlyValid(now))
        assertFalse(valid.copy(expiresAtMillis = now - 1_000L).isCurrentlyValid(now))
        assertFalse(valid.copy(usageCount = 50L).isCurrentlyValid(now))
        assertTrue(valid.copy(usageLimit = null, startsAtMillis = null, expiresAtMillis = null).isCurrentlyValid(now))

        val mapWithPerUser = valid.copy(perCustomerLimit = 2L).toCreateMap()
        assertEquals(2L, mapWithPerUser["perCustomerLimit"])
    }

    @Test
    fun `analytics date bounds support yesterday and last month ranges`() {
        val nowMs = 1_750_000_000_000L
        val (yStart, yEnd) = AdminAnalyticsEngine.computeDateBounds(AnalyticsDateRange.YESTERDAY, nowMillis = nowMs)
        assertNotNull(yStart)
        assertNotNull(yEnd)
        assertEquals(86_400_000L - 1L, yEnd!! - yStart!!)

        val (lmStart, lmEnd) = AdminAnalyticsEngine.computeDateBounds(AnalyticsDateRange.LAST_MONTH, nowMillis = nowMs)
        assertNotNull(lmStart)
        assertNotNull(lmEnd)
        assertTrue(lmEnd!! > lmStart!!)
        assertTrue(lmEnd < nowMs)
    }

    @Test
    fun `live notification engine surfaces transfer proofs failed payments stock alerts and flagged reviews`() {
        val ts = Timestamp(Date(1_750_000_000_000L))
        val alerts = AdminNotificationEngine.buildLiveAlerts(
            orders = listOf(
                Order(
                    id = "o1",
                    orderNumber = "ZC-9001",
                    recipientName = "Tope",
                    itemsSummary = "1x Ivory Abaya",
                    status = OrderStatus.PAYMENT_SUBMITTED.name,
                    cancellationStatus = "REQUESTED",
                    cancellationRequestReason = "Need larger size",
                    createdAt = ts,
                    updatedAt = ts,
                )
            ),
            payments = listOf(
                PaymentRecord(id = "p_sub", orderId = "o1", orderNumber = "ZC-9001", reference = "BT-1", status = PaymentRecordStatus.SUBMITTED.name, createdAt = ts),
                PaymentRecord(id = "p_fail", orderId = "o2", orderNumber = "ZC-9002", reference = "PS-2", provider = "PAYSTACK", status = PaymentRecordStatus.FAILED.name, createdAt = ts),
            ),
            products = listOf(
                Product(id = "pr1", title = "Silk Tote", sku = "ZC-T1", lowStockThreshold = 3L),
                Product(id = "pr2", title = "Gold Mules", sku = "ZC-M2", lowStockThreshold = 4L),
            ),
            inventoryMap = mapOf(
                "pr1" to InventoryRecord(productId = "pr1", stockQuantity = 0L, lowStockThreshold = 3L),
                "pr2" to InventoryRecord(productId = "pr2", stockQuantity = 2L, lowStockThreshold = 4L),
            ),
            customers = listOf(
                CustomerAdminSummary(
                    profile = UserProfile(id = "u1", userId = "u1", displayName = "Zainab Musa", createdAt = ts),
                    privateInfo = null,
                    addresses = emptyList(),
                    orders = emptyList(),
                    totalOrderCount = 1,
                    paidOrderCount = 0,
                    lifetimeSpendPaidNgn = 0.0,
                )
            ),
            reviews = listOf(
                Review(id = "rev1", authorName = "Sade", rating = 4L, comment = "Lovely embroidery", moderationStatus = "PENDING", createdAt = ts)
            ),
        )

        val categories = alerts.map { it.category }.toSet()
        assertTrue("PAYMENT" in categories)
        assertTrue("PAYMENT_FAILED" in categories)
        assertTrue("REFUND" in categories)
        assertTrue("ORDER" in categories)
        assertTrue("INVENTORY" in categories)
        assertTrue("CUSTOMER" in categories)
        assertTrue("REVIEW" in categories)
        assertTrue(alerts.any { it.id == "inv_oos_pr1" })
        assertTrue(alerts.any { it.id == "inv_low_pr2" })
    }

    @Test
    fun `store settings write map includes timezone order prefix and tax controls`() {
        val settings = StoreSettings(
            storeName = "ZEECROWN STORE",
            timeZone = "Africa/Lagos",
            orderNumberPrefix = "ZCW",
            taxEnabled = true,
            taxRatePercent = 7.5,
        )
        val map = settings.toWriteMap()
        assertEquals("Africa/Lagos", map["timeZone"])
        assertEquals("ZCW", map["orderNumberPrefix"])
        assertEquals(true, map["taxEnabled"])
        assertEquals(7.5, map["taxRatePercent"])
    }
}
