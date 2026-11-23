package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BoutiqueContactHelper
import com.example.data.model.SizeGuide
import com.example.data.model.SizeGuideUnitConverter
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownMetallicGold
import com.example.ui.theme.ZeecrownObsidianInk
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory
import com.example.ui.theme.ZeecrownWhatsAppGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SizeGuideModalSheet(
    guide: SizeGuide,
    whatsappNumber: String = BoutiqueContactHelper.OFFICIAL_WHATSAPP_NUMBER_DISPLAY,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedUnit by remember(guide.id) {
        mutableStateOf(
            if (guide.measurementUnitDefault.equals("IN", ignoreCase = true)) "IN" else "CM"
        )
    }
    val displayNumber = BoutiqueContactHelper.resolveWhatsAppDisplayNumber(whatsappNumber)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ZeecrownWarmIvory,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ZeecrownSoftLavender,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Straighten,
                            contentDescription = null,
                            tint = ZeecrownDeepPurple,
                            modifier = Modifier.padding(8.dp).size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = guide.resolvedType.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ZeecrownGoldInk,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = guide.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = ZeecrownDeepPurple,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("size_guide_close_btn"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close size guide",
                        tint = ZeecrownDeepPurple,
                    )
                }
            }

            if (guide.description.isNotBlank()) {
                Text(
                    text = guide.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Unit selector (CM / IN)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Measurement Unit",
                    style = MaterialTheme.typography.titleSmall,
                    color = ZeecrownDeepPurple,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedUnit == "CM",
                        onClick = { selectedUnit = "CM" },
                        label = { Text("Centimetres (cm)") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("size_guide_unit_cm"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZeecrownRoyalPurple,
                            selectedLabelColor = ZeecrownPureWhite,
                        ),
                    )
                    FilterChip(
                        selected = selectedUnit == "IN",
                        onClick = { selectedUnit = "IN" },
                        label = { Text("Inches (in)") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("size_guide_unit_in"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZeecrownRoyalPurple,
                            selectedLabelColor = ZeecrownPureWhite,
                        ),
                    )
                }
            }

            // Measurement Illustration Card
            SizeGuideMeasurementDiagram(
                isFootwear = guide.resolvedType.isFootwear,
                unitLabel = if (selectedUnit == "IN") "in" else "cm",
            )

            // Responsive Horizontally Scrollable Measurement Table
            if (guide.columns.isNotEmpty() && guide.rows.isNotEmpty()) {
                val baseUnit = guide.measurementUnitDefault.ifBlank { "CM" }
                val convertedHeaders = remember(guide.columns, selectedUnit) {
                    guide.columns.map { SizeGuideUnitConverter.convertHeader(it, selectedUnit) }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    color = ZeecrownLightLilac,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        Column {
                            // Header Row
                            Surface(color = ZeecrownDeepPurple) {
                                Row(modifier = Modifier.padding(vertical = 10.dp)) {
                                    convertedHeaders.forEachIndexed { colIdx, header ->
                                        Box(
                                            modifier = Modifier
                                                .width(if (colIdx == 0) 112.dp else 104.dp)
                                                .padding(horizontal = 10.dp),
                                        ) {
                                            Text(
                                                text = header,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ZeecrownPureWhite,
                                            )
                                        }
                                    }
                                }
                            }
                            // Data Rows
                            guide.rows.forEachIndexed { rowIdx, row ->
                                val bg = if (rowIdx % 2 == 0) ZeecrownPureWhite else ZeecrownLightLilac
                                Surface(color = bg) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(112.dp)
                                                .padding(horizontal = 10.dp),
                                        ) {
                                            Text(
                                                text = row.sizeLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ZeecrownDeepPurple,
                                            )
                                        }
                                        val valueCols = convertedHeaders.drop(1)
                                        valueCols.forEachIndexed { valueIdx, colHeader ->
                                            val raw = row.values.getOrElse(valueIdx) { "-" }
                                            val displayVal = SizeGuideUnitConverter.convertCellValue(
                                                rawValue = raw,
                                                columnHeader = colHeader,
                                                baseUnit = baseUnit,
                                                targetUnit = selectedUnit,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(104.dp)
                                                    .padding(horizontal = 10.dp),
                                            ) {
                                                Text(
                                                    text = displayVal,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowIdx < guide.rows.lastIndex) {
                                    HorizontalDivider(color = ZeecrownElegantSilver)
                                }
                            }
                        }
                    }
                }
            }

            // Step-by-step Measuring Instructions
            if (guide.howToMeasureSteps.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = ZeecrownRoyalPurple,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "How to Measure Accurately",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ZeecrownDeepPurple,
                            )
                        }
                        guide.howToMeasureSteps.forEachIndexed { idx, step ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ZeecrownSoftLavender,
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ZeecrownDeepPurple,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // Fit Note Card
            if (guide.fitNotes.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = ZeecrownSoftLavender.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, ZeecrownMetallicGold.copy(alpha = 0.55f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = ZeecrownRoyalPurple,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Atelier Fit Note",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = ZeecrownDeepPurple,
                            )
                            Text(
                                text = guide.fitNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ZeecrownObsidianInk,
                            )
                        }
                    }
                }
            }

            // Direct WhatsApp sizing help CTA
            Button(
                onClick = {
                    launchWhatsAppChat(
                        context = context,
                        configuredNumber = whatsappNumber,
                        prefilledMessage = "Hello ZEECROWN STORE, I would like sizing assistance with ${guide.name}.",
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("size_guide_whatsapp_btn"),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZeecrownWhatsAppGreen,
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Need Sizing Advice? WhatsApp $displayNumber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SizeGuideMeasurementDiagram(
    isFootwear: Boolean,
    unitLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ZeecrownLightLilac,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isFootwear) {
                    "Foot Measurement Diagram ($unitLabel)"
                } else {
                    "Garment & Body Measurement Guide ($unitLabel)"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = ZeecrownDeepPurple,
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                val w = size.width
                val h = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

                if (isFootwear) {
                    val heelX = w * 0.14f
                    val toeX = w * 0.84f
                    val footMidY = h * 0.48f

                    drawLine(
                        color = ZeecrownDeepPurple,
                        start = Offset(heelX, h * 0.14f),
                        end = Offset(heelX, h * 0.86f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round,
                    )

                    val footPath = Path().apply {
                        moveTo(heelX + 6f, footMidY - h * 0.15f)
                        cubicTo(
                            w * 0.30f, footMidY - h * 0.18f,
                            w * 0.54f, footMidY - h * 0.25f,
                            toeX - w * 0.08f, footMidY - h * 0.22f,
                        )
                        cubicTo(
                            toeX + w * 0.02f, footMidY - h * 0.18f,
                            toeX + w * 0.02f, footMidY + h * 0.08f,
                            toeX - w * 0.06f, footMidY + h * 0.18f,
                        )
                        cubicTo(
                            w * 0.56f, footMidY + h * 0.22f,
                            w * 0.34f, footMidY + h * 0.14f,
                            heelX + 6f, footMidY + h * 0.15f,
                        )
                        close()
                    }
                    drawPath(
                        path = footPath,
                        color = ZeecrownSoftLavender.copy(alpha = 0.85f),
                    )
                    drawPath(
                        path = footPath,
                        color = ZeecrownDeepPurple,
                        style = Stroke(width = 3.5f, join = StrokeJoin.Round),
                    )

                    drawLine(
                        color = ZeecrownMetallicGold,
                        start = Offset(toeX, h * 0.16f),
                        end = Offset(toeX, h * 0.86f),
                        strokeWidth = 3f,
                        pathEffect = dashEffect,
                    )

                    val dimY = h * 0.80f
                    drawLine(
                        color = ZeecrownMetallicGold,
                        start = Offset(heelX, dimY),
                        end = Offset(toeX, dimY),
                        strokeWidth = 4f,
                    )
                    drawCircle(
                        color = ZeecrownDeepPurple,
                        radius = 12f,
                        center = Offset((heelX + toeX) * 0.5f, dimY),
                    )
                    val widthX = w * 0.63f
                    drawLine(
                        color = ZeecrownRoyalPurple.copy(alpha = 0.75f),
                        start = Offset(widthX, footMidY - h * 0.22f),
                        end = Offset(widthX, footMidY + h * 0.19f),
                        strokeWidth = 3f,
                        pathEffect = dashEffect,
                    )
                    drawCircle(
                        color = ZeecrownMetallicGold,
                        radius = 11f,
                        center = Offset(widthX, footMidY),
                    )
                } else {
                    val cx = w * 0.50f
                    val topY = h * 0.10f
                    val botY = h * 0.90f
                    val shoulderHalf = w * 0.19f
                    val bustHalf = w * 0.15f
                    val waistHalf = w * 0.135f
                    val hipHalf = w * 0.18f

                    val dressPath = Path().apply {
                        moveTo(cx - w * 0.055f, topY)
                        quadraticTo(cx, topY + h * 0.07f, cx + w * 0.055f, topY)
                        lineTo(cx + shoulderHalf, topY + h * 0.07f)
                        lineTo(cx + shoulderHalf + w * 0.05f, topY + h * 0.25f)
                        lineTo(cx + bustHalf + w * 0.02f, topY + h * 0.27f)
                        lineTo(cx + bustHalf, topY + h * 0.23f)
                        lineTo(cx + waistHalf, topY + h * 0.42f)
                        lineTo(cx + hipHalf, botY)
                        lineTo(cx - hipHalf, botY)
                        lineTo(cx - waistHalf, topY + h * 0.42f)
                        lineTo(cx - bustHalf, topY + h * 0.23f)
                        lineTo(cx - bustHalf - w * 0.02f, topY + h * 0.27f)
                        lineTo(cx - shoulderHalf - w * 0.05f, topY + h * 0.25f)
                        lineTo(cx - shoulderHalf, topY + h * 0.07f)
                        close()
                    }
                    drawPath(path = dressPath, color = ZeecrownSoftLavender.copy(alpha = 0.8f))
                    drawPath(
                        path = dressPath,
                        color = ZeecrownDeepPurple,
                        style = Stroke(width = 3.2f, join = StrokeJoin.Round),
                    )

                    val shY = topY + h * 0.08f
                    drawLine(
                        color = ZeecrownMetallicGold,
                        start = Offset(cx - shoulderHalf, shY),
                        end = Offset(cx + shoulderHalf, shY),
                        strokeWidth = 3f,
                        pathEffect = dashEffect,
                    )
                    val bustY = topY + h * 0.24f
                    drawLine(
                        color = ZeecrownRoyalPurple,
                        start = Offset(cx - bustHalf, bustY),
                        end = Offset(cx + bustHalf, bustY),
                        strokeWidth = 3f,
                        pathEffect = dashEffect,
                    )
                    val waistY = topY + h * 0.42f
                    drawLine(
                        color = ZeecrownMetallicGold,
                        start = Offset(cx - waistHalf, waistY),
                        end = Offset(cx + waistHalf, waistY),
                        strokeWidth = 3f,
                        pathEffect = dashEffect,
                    )
                    val hipY = topY + h * 0.62f
                    drawLine(
                        color = ZeecrownRoyalPurple,
                        start = Offset(cx - hipHalf * 0.88f, hipY),
                        end = Offset(cx + hipHalf * 0.88f, hipY),
                        strokeWidth = 3f,
                        pathEffect = dashEffect,
                    )
                    val lenX = cx + hipHalf + w * 0.09f
                    drawLine(
                        color = ZeecrownMetallicGold,
                        start = Offset(lenX, topY),
                        end = Offset(lenX, botY),
                        strokeWidth = 3.5f,
                    )
                    drawLine(
                        color = ZeecrownMetallicGold,
                        start = Offset(lenX - 8f, topY),
                        end = Offset(lenX + 8f, topY),
                        strokeWidth = 3.5f,
                    )
                    drawLine(
                        color = ZeecrownMetallicGold,
                        start = Offset(lenX - 8f, botY),
                        end = Offset(lenX + 8f, botY),
                        strokeWidth = 3.5f,
                    )
                }
            }

            val markers = if (isFootwear) {
                listOf("1. Heel-to-Longest-Toe ($unitLabel)", "2. Forefoot Width & Instep")
            } else {
                listOf("1. Shoulder", "2. Bust ($unitLabel)", "3. Waist ($unitLabel)", "4. Hip ($unitLabel)", "5. Garment Length ($unitLabel)")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                markers.forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ZeecrownPureWhite,
                        border = BorderStroke(1.dp, ZeecrownElegantSilver),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = ZeecrownDeepPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
