package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AnalyticsDateRange
import com.example.ui.theme.ZeecrownChampagneGold
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess
import com.example.ui.theme.ZeecrownSoftLavender
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatAdminTimestamp(ts: Timestamp?, fallback: String = "Just now"): String {
    val date = ts?.toDate() ?: return fallback
    return SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(date)
}

fun formatAdminDateShort(millis: Long?): String {
    if (millis == null) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}

@Composable
fun AdminSectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    subtitle: String = "",
    accentColor: Color = ZeecrownDeepPurple,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = ZeecrownGoldInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun AdminStatusPill(
    label: String,
    tone: PillTone = PillTone.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    val bg = when (tone) {
        PillTone.SUCCESS -> ZeecrownSageSuccess.copy(alpha = 0.16f)
        PillTone.GOLD -> ZeecrownChampagneGold.copy(alpha = 0.28f)
        PillTone.ROSE -> ZeecrownSoftLavender
        PillTone.ERROR -> MaterialTheme.colorScheme.errorContainer
        PillTone.NEUTRAL -> ZeecrownLightLilac
    }
    val fg = when (tone) {
        PillTone.SUCCESS -> ZeecrownSageSuccess
        PillTone.ROSE -> ZeecrownRoyalPurple
        PillTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        else -> ZeecrownDeepPurple
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

enum class PillTone {
    SUCCESS,
    GOLD,
    ROSE,
    ERROR,
    NEUTRAL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDateRangeSelectorRow(
    selectedRange: AnalyticsDateRange,
    customLabel: String,
    onSelectRange: (AnalyticsDateRange, Long?, Long?) -> Unit,
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnalyticsDateRange.entries.forEach { range ->
            val label = if (range == AnalyticsDateRange.CUSTOM && selectedRange == AnalyticsDateRange.CUSTOM) {
                customLabel
            } else {
                range.label
            }
            FilterChip(
                selected = selectedRange == range,
                onClick = {
                    if (range == AnalyticsDateRange.CUSTOM) {
                        showCustomPicker = true
                    } else {
                        onSelectRange(range, null, null)
                    }
                },
                label = { Text(label) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        }
    }

    if (showCustomPicker) {
        val pickerState = rememberDateRangePickerState()
        Dialog(
            onDismissRequest = { showCustomPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Custom Date Range", style = MaterialTheme.typography.titleLarge)
                    DateRangePicker(
                        state = pickerState,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showCustomPicker = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                val start = pickerState.selectedStartDateMillis
                                val end = pickerState.selectedEndDateMillis?.plus(86_399_999L)
                                if (start != null) {
                                    onSelectRange(AnalyticsDateRange.CUSTOM, start, end)
                                }
                                showCustomPicker = false
                            },
                            enabled = pickerState.selectedStartDateMillis != null,
                        ) { Text("Apply Range") }
                    }
                }
            }
        }
    }
}
