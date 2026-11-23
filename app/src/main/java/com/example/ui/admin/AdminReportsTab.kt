package com.example.ui.admin

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminAnalyticsEngine
import com.example.data.model.AdminAnalyticsReport
import com.example.data.model.AnalyticsDateRange
import com.example.data.model.Order
import com.example.ui.components.formatNgnAmount
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess

@Composable
fun AdminReportsTab(
    report: AdminAnalyticsReport,
    selectedRange: AnalyticsDateRange,
    orders: List<Order>,
    onSelectDateRange: (AnalyticsDateRange, Long?, Long?) -> Unit,
    onExportFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    var pendingCsvContent by remember { mutableStateOf("") }

    val csvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingCsvContent.isNotBlank()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pendingCsvContent.toByteArray(Charsets.UTF_8))
                }
            }.fold(
                onSuccess = { onExportFeedback("CSV report saved successfully.") },
                onFailure = { onExportFeedback(it.localizedMessage ?: "Failed to write CSV file.") }
            )
            pendingCsvContent = ""
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AdminDateRangeSelectorRow(
                selectedRange = selectedRange,
                customLabel = report.dateRangeLabel,
                onSelectRange = onSelectDateRange,
            )
        }

        // Core Analytics & Formula Documentation
        item {
            AdminSectionCard {
                Text("Analytics & Revenue Summary (${report.dateRangeLabel})", style = MaterialTheme.typography.titleLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = "Verified Paid Revenue",
                        value = formatNgnAmount(report.verifiedPaidRevenueNgn),
                        accentColor = ZeecrownSageSuccess,
                        modifier = Modifier.weight(1f),
                    )
                    AdminMetricCard(
                        title = "Net After Refunds",
                        value = formatNgnAmount(report.netPaidRevenueNgn),
                        subtitle = "Refunded: ${formatNgnAmount(report.refundedTotalNgn)}",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetricCard(
                        title = "Average Order Value",
                        value = formatNgnAmount(report.averageOrderValueNgn),
                        subtitle = "${report.paidOrdersCount} paid order(s)",
                        modifier = Modifier.weight(1f),
                    )
                    AdminMetricCard(
                        title = "New vs Returning",
                        value = "${report.newCustomersInRange} / ${report.returningCustomersInRange}",
                        subtitle = "Total accounts: ${report.totalCustomers}",
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = "Metric Formula: Verified Paid Revenue sums only orders with paymentStatus = PAID or PARTIALLY_REFUNDED and excludes CANCELLED/REFUNDED orders. Net Revenue subtracts recorded refunds. Unpaid or pending transfers never inflate revenue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Daily Paid Revenue Chart (Compose Canvas)
        item {
            AdminSectionCard {
                Text("Daily Verified Paid Revenue Trend", style = MaterialTheme.typography.titleMedium)
                if (report.dailyRevenueBuckets.isEmpty()) {
                    Text(
                        "No paid orders recorded in '${report.dateRangeLabel}' yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val buckets = report.dailyRevenueBuckets
                    val maxVal = buckets.maxOf { it.second }.coerceAtLeast(1.0)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val barCount = buckets.size.coerceAtLeast(1)
                        val slotWidth = size.width / barCount
                        val barWidth = (slotWidth * 0.62f).coerceAtLeast(12f)
                        buckets.forEachIndexed { idx, (_, amount) ->
                            val ratio = (amount / maxVal).toFloat().coerceIn(0.04f, 1f)
                            val barHeight = size.height * ratio
                            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                            val top = size.height - barHeight
                            drawRoundRect(
                                color = ZeecrownRoyalPurple,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8f, 8f),
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        buckets.forEach { (day, amt) ->
                            Column {
                                Text(day, style = MaterialTheme.typography.labelSmall)
                                Text(formatNgnAmount(amt), style = MaterialTheme.typography.labelSmall, color = ZeecrownGoldInk)
                            }
                        }
                    }
                }
            }
        }

        // Payment Method & Category Breakdown
        item {
            AdminSectionCard {
                Text("Payment Method Breakdown", style = MaterialTheme.typography.titleMedium)
                report.paymentMethodBreakdown.forEach { pm ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${pm.method.label} (${pm.orderCount} orders)", style = MaterialTheme.typography.bodyMedium)
                        Text(formatNgnAmount(pm.paidAmountNgn), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = ZeecrownElegantSilver)
                }

                Spacer(Modifier.height(6.dp))
                Text("Category Performance Breakdown", style = MaterialTheme.typography.titleMedium)
                if (report.categoryBreakdown.isEmpty()) {
                    Text("No category data available.", style = MaterialTheme.typography.bodySmall)
                } else {
                    report.categoryBreakdown.forEach { cat ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${cat.categoryName} (${cat.productCount} SKUs)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("${cat.unitsSold} sold · ${formatNgnAmount(cat.attributedRevenueNgn)}", style = MaterialTheme.typography.bodySmall, color = ZeecrownDeepPurple)
                        }
                    }
                }
            }
        }

        // CSV Export Controls
        item {
            AdminSectionCard(containerColor = ZeecrownLightLilac) {
                Text("Export Business Reports (CSV)", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Save clean RFC-4180 UTF-8 CSV spreadsheets for accounting, or share directly with finance.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = {
                        pendingCsvContent = AdminAnalyticsEngine.buildSalesReportCsv(report)
                        csvDocumentLauncher.launch("zeecrown_sales_report_${System.currentTimeMillis() / 1000}.csv")
                    },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("admin_export_sales_csv"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZeecrownRoyalPurple, contentColor = ZeecrownPureWhite),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Sales Summary CSV")
                }
                OutlinedButton(
                    onClick = {
                        pendingCsvContent = AdminAnalyticsEngine.buildOrdersCsv(orders)
                        csvDocumentLauncher.launch("zeecrown_orders_${System.currentTimeMillis() / 1000}.csv")
                    },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("admin_export_orders_csv"),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Orders Ledger CSV (${orders.size} rows)")
                }
                OutlinedButton(
                    onClick = {
                        val csvText = AdminAnalyticsEngine.buildSalesReportCsv(report)
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "ZEECROWN Sales Report (${report.dateRangeLabel})")
                            putExtra(Intent.EXTRA_TEXT, csvText)
                        }
                        context.startActivity(Intent.createChooser(share, "Share CSV Report"))
                    },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share Sales Report")
                }
            }
        }
    }
}
