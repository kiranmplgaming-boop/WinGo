package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.WinGoRecord
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GoldBigColor = Color(0xFFF59E0B)
private val GoldBigDark = Color(0xFFB45309)
private val CyanSmallColor = Color(0xFF06B6D4)
private val CyanSmallDark = Color(0xFF0E7490)
private val PurpleVioletColor = Color(0xFF8B5CF6)

@Composable
fun WinGoAnalysisDashboard(
    records: List<WinGoRecord>,
    latestRecord: WinGoRecord?,
    isScraping: Boolean = false,
    countdownSeconds: Int = 60,
    currentLivePeriodId: String = "",
    onSyncClick: () -> Unit = {},
    onViewRecordsClick: () -> Unit = {},
    onViewForecastClick: () -> Unit = {},
    onAddCustomRecord: (period: String, number: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val totalCount = records.size
    val bigRecords = remember(records) { records.filter { it.bigSmall.equals("Big", ignoreCase = true) } }
    val smallRecords = remember(records) { records.filter { it.bigSmall.equals("Small", ignoreCase = true) } }

    val bigCount = bigRecords.size
    val smallCount = smallRecords.size

    val bigPct = if (totalCount > 0) (bigCount * 100f / totalCount) else 50f
    val smallPct = if (totalCount > 0) (smallCount * 100f / totalCount) else 50f

    val streakAnalysis = remember(records) {
        calculateStreaks(records)
    }

    val numberFrequencies = remember(records) {
        val counts = IntArray(10) { 0 }
        records.forEach {
            if (it.number in 0..9) counts[it.number]++
        }
        counts
    }

    val mostFrequentNumber = remember(numberFrequencies) {
        numberFrequencies.indices.maxByOrNull { numberFrequencies[it] } ?: 0
    }
    val leastFrequentNumber = remember(numberFrequencies) {
        numberFrequencies.indices.minByOrNull { numberFrequencies[it] } ?: 0
    }

    val greenCount = remember(records) { records.count { it.color.contains("Green", ignoreCase = true) } }
    val redCount = remember(records) { records.count { it.color.contains("Red", ignoreCase = true) } }
    val violetCount = remember(records) { records.count { it.color.contains("Violet", ignoreCase = true) } }

    var showQuickAddDialog by remember { mutableStateOf(false) }

    if (showQuickAddDialog) {
        QuickAddRecordDialog(
            suggestedPeriod = currentLivePeriodId.ifBlank { latestRecord?.period ?: "" },
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { period, num ->
                onAddCustomRecord(period, num)
                showQuickAddDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section: Real-time 1-Minute Live Engine Countdown Card
        item {
            LiveCountdownCard(
                countdownSeconds = countdownSeconds,
                currentPeriodId = currentLivePeriodId,
                latestPeriodId = latestRecord?.period ?: "",
                isScraping = isScraping,
                onSyncClick = onSyncClick,
                onViewForecastClick = onViewForecastClick,
                onQuickAddClick = { showQuickAddDialog = true }
            )
        }

        // Hero Card: Latest WinGo 1 Min Period Result
        item {
            HeroLatestResultCard(
                latest = latestRecord,
                totalRecords = totalCount,
                onSyncClick = onSyncClick
            )
        }

        // Section: Big vs Small Ratio & Percentage Card
        item {
            BigSmallRatioCard(
                bigCount = bigCount,
                smallCount = smallCount,
                bigPct = bigPct,
                smallPct = smallPct,
                total = totalCount
            )
        }

        // Section: Streak & Pattern Momentum
        item {
            StreakMomentumCard(streakAnalysis = streakAnalysis)
        }

        // Section: Big/Small Bead Roadmap (Recent 40 rounds)
        item {
            BeadRoadmapCard(records = records.take(40))
        }

        // Section: Number Frequency Heatmap (0 - 9)
        item {
            NumberFrequencyHeatmapCard(
                frequencies = numberFrequencies,
                total = totalCount,
                hotNumber = mostFrequentNumber,
                coldNumber = leastFrequentNumber
            )
        }

        // Section: Color Distribution
        item {
            ColorDistributionCard(
                greenCount = greenCount,
                redCount = redCount,
                violetCount = violetCount,
                total = totalCount
            )
        }

        // Section: Next Period Pattern Indicator
        item {
            PatternPredictionCard(
                streakAnalysis = streakAnalysis,
                bigPct = bigPct,
                smallPct = smallPct
            )
        }

        // Section: WinGo 1Min Stored Records in Database
        item {
            StoredRecordsSummaryCard(
                records = records.take(15),
                totalRecords = totalCount,
                onViewAllClick = onViewRecordsClick
            )
        }
    }
}

@Composable
fun StoredRecordsSummaryCard(
    records: List<WinGoRecord>,
    totalRecords: Int,
    onViewAllClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stored_records_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stored WinGo 1Min Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ID & Big/Small stored in Room Database ($totalRecords total)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onViewAllClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View All", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No records stored yet. Sync or add records to begin analysis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    records.forEach { rec ->
                        val isBig = rec.bigSmall.equals("Big", ignoreCase = true)
                        val numColor = when (rec.number) {
                            0 -> RoseError
                            5 -> EmeraldSuccess
                            1, 3, 7, 9 -> EmeraldSuccess
                            else -> RoseError
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = rec.period,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formatTimestamp(rec.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(numColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${rec.number}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isBig) GoldBigColor else CyanSmallColor,
                                        modifier = Modifier.width(64.dp)
                                    ) {
                                        Text(
                                            text = if (isBig) "BIG" else "SMALL",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveCountdownCard(
    countdownSeconds: Int,
    currentPeriodId: String,
    latestPeriodId: String,
    isScraping: Boolean,
    onSyncClick: () -> Unit,
    onViewForecastClick: () -> Unit = {},
    onQuickAddClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_countdown_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = "LIVE WINGO 1MIN ENGINE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Auto-updates every 1m",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Next Period Draw",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (currentPeriodId.isNotBlank()) currentPeriodId else "Syncing...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Countdown badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (countdownSeconds <= 10) RoseError else EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "00:${String.format(Locale.US, "%02d", countdownSeconds)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (countdownSeconds <= 10) RoseError else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickAddClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Result", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onViewForecastClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Forecast %", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldSuccess.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = "⚡ Real-Time Auto Sync Active (<5s): Results update immediately upon draw",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddRecordDialog(
    suggestedPeriod: String,
    onDismiss: () -> Unit,
    onConfirm: (period: String, number: Int) -> Unit
) {
    var periodInput by remember { mutableStateOf(suggestedPeriod) }
    var selectedNum by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add / Update WinGo Result", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Log exact result from DmFirst into Room & Firestore database:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = periodInput,
                    onValueChange = { periodInput = it },
                    label = { Text("Period ID (17 digits)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Winning Ball Number (0 - 9):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (0..4).forEach { n ->
                        NumberBallPicker(
                            num = n,
                            isSelected = selectedNum == n,
                            onClick = { selectedNum = n }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (5..9).forEach { n ->
                        NumberBallPicker(
                            num = n,
                            isSelected = selectedNum == n,
                            onClick = { selectedNum = n }
                        )
                    }
                }

                val isBig = selectedNum >= 5
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBig) GoldBigColor.copy(alpha = 0.2f) else CyanSmallColor.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Selected Result: Number $selectedNum • ${if (isBig) "BIG (5-9)" else "SMALL (0-4)"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBig) GoldBigDark else CyanSmallDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (periodInput.isNotBlank()) {
                        onConfirm(periodInput.trim(), selectedNum)
                    }
                }
            ) {
                Text("Save to DB")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NumberBallPicker(
    num: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isZero = num == 0
    val isFive = num == 5
    val isGreen = listOf(1, 3, 7, 9).contains(num)

    val backgroundModifier = when {
        isZero -> Modifier.background(
            Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFF8B5CF6)))
        )
        isFive -> Modifier.background(
            Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF8B5CF6)))
        )
        isGreen -> Modifier.background(EmeraldSuccess)
        else -> Modifier.background(RoseError)
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) backgroundModifier
                else when {
                    isZero -> Modifier.background(
                        Brush.horizontalGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.3f), Color(0xFF8B5CF6).copy(alpha = 0.3f)))
                    )
                    isFive -> Modifier.background(
                        Brush.horizontalGradient(listOf(Color(0xFF10B981).copy(alpha = 0.3f), Color(0xFF8B5CF6).copy(alpha = 0.3f)))
                    )
                    isGreen -> Modifier.background(EmeraldSuccess.copy(alpha = 0.3f))
                    else -> Modifier.background(RoseError.copy(alpha = 0.3f))
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$num",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HeroLatestResultCard(
    latest: WinGoRecord?,
    totalRecords: Int,
    onSyncClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_latest_period_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = "LATEST WINGO 1MIN RESULT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (totalRecords > 0) "$totalRecords in DB" else "No Data",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (latest != null) {
                val isBig = latest.bigSmall.equals("Big", ignoreCase = true)
                val badgeBrush = if (isBig) {
                    Brush.horizontalGradient(listOf(GoldBigDark, GoldBigColor))
                } else {
                    Brush.horizontalGradient(listOf(CyanSmallDark, CyanSmallColor))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Period ID
                    Column {
                        Text(
                            text = "Period ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = latest.period,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatTimestamp(latest.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Large Big/Small Badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(badgeBrush)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.background(Color.Transparent)
                        ) {
                            Text(
                                text = if (isBig) "BIG" else "SMALL",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isBig) "(5 - 9)" else "(0 - 4)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Number ball and Color details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Drawn Number:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        NumberBall(
                            number = latest.number,
                            colorName = latest.color,
                            size = 32.dp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Color:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        ColorBadge(colorName = latest.color)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = "Real-Time Engine Active",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Monitoring live 1-minute period. Results update automatically within 3-5 seconds of round completion.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BigSmallRatioCard(
    bigCount: Int,
    smallCount: Int,
    bigPct: Float,
    smallPct: Float,
    total: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Big vs Small Distribution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total $total Rounds",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldBigColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldBigColor.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BIG (5-9)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldBigDark
                        )
                        Text(
                            text = "$bigCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldBigDark
                        )
                        Text(
                            text = "%.1f%%".format(Locale.US, bigPct),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldBigDark
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyanSmallColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanSmallColor.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SMALL (0-4)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanSmallDark
                        )
                        Text(
                            text = "$smallCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanSmallDark
                        )
                        Text(
                            text = "%.1f%%".format(Locale.US, smallPct),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanSmallDark
                        )
                    }
                }
            }

            // Visual Ratio Split Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    val bigWeight = if (total > 0) bigPct.coerceAtLeast(1f) else 50f
                    val smallWeight = if (total > 0) smallPct.coerceAtLeast(1f) else 50f

                    Box(
                        modifier = Modifier
                            .weight(bigWeight)
                            .fillMaxSize()
                            .background(GoldBigColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(smallWeight)
                            .fillMaxSize()
                            .background(CyanSmallColor)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Big: %.1f%%".format(Locale.US, bigPct),
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldBigDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (bigCount >= smallCount) "Big leads by +${bigCount - smallCount}" else "Small leads by +${smallCount - bigCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Small: %.1f%%".format(Locale.US, smallPct),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanSmallDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StreakMomentumCard(streakAnalysis: StreakResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Streak & Momentum Analysis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (streakAnalysis.currentType.equals("Big", ignoreCase = true)) GoldBigColor.copy(alpha = 0.2f) else CyanSmallColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Current: ${streakAnalysis.currentType} x${streakAnalysis.currentCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (streakAnalysis.currentType.equals("Big", ignoreCase = true)) GoldBigDark else CyanSmallDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreakPill(
                    title = "Max Big Streak",
                    value = "${streakAnalysis.maxBigStreak} in row",
                    color = GoldBigDark,
                    bgColor = GoldBigColor.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                )

                StreakPill(
                    title = "Max Small Streak",
                    value = "${streakAnalysis.maxSmallStreak} in row",
                    color = CyanSmallDark,
                    bgColor = CyanSmallColor.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                )

                StreakPill(
                    title = "Alternation Rate",
                    value = "%.0f%%".format(Locale.US, streakAnalysis.alternationRate),
                    color = MaterialTheme.colorScheme.primary,
                    bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StreakPill(
    title: String,
    value: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun BeadRoadmapCard(records: List<WinGoRecord>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Big / Small Visual Roadmap",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Last ${records.size} rounds",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (records.isEmpty()) {
                Text(
                    text = "No records to display roadmap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    items(records, key = { it.period }) { rec ->
                        val isBig = rec.bigSmall.equals("Big", ignoreCase = true)
                        val beadColor = if (isBig) GoldBigColor else CyanSmallColor

                        Surface(
                            shape = CircleShape,
                            color = beadColor,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isBig) "B" else "S",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "${rec.number}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = Color.Black.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumberFrequencyHeatmapCard(
    frequencies: IntArray,
    total: Int,
    hotNumber: Int,
    coldNumber: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Number Frequency Heatmap (0-9)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hot: #$hotNumber",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                    Text(
                        text = "Cold: #$coldNumber",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                }
            }

            val maxFreq = frequencies.maxOrNull()?.coerceAtLeast(1) ?: 1

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                frequencies.forEachIndexed { num, count ->
                    val isBig = num >= 5
                    val pct = if (total > 0) (count * 100f / total) else 0f
                    val barWidthRatio = count.toFloat() / maxFreq

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isBig) GoldBigColor.copy(alpha = 0.2f) else CyanSmallColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$num",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isBig) GoldBigDark else CyanSmallDark
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barWidthRatio.coerceIn(0.02f, 1f))
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (num == hotNumber) Color(0xFFEF4444) else if (isBig) GoldBigColor else CyanSmallColor)
                            )
                        }

                        Text(
                            text = "$count (%.1f%%)".format(Locale.US, pct),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(65.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorDistributionCard(
    greenCount: Int,
    redCount: Int,
    violetCount: Int,
    total: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Color Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistributionPill(label = "Green", count = greenCount, total = total, color = EmeraldSuccess, modifier = Modifier.weight(1f))
                DistributionPill(label = "Red", count = redCount, total = total, color = RoseError, modifier = Modifier.weight(1f))
                DistributionPill(label = "Violet", count = violetCount, total = total, color = PurpleVioletColor, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PatternPredictionCard(
    streakAnalysis: StreakResult,
    bigPct: Float,
    smallPct: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Pattern Momentum Insight",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                val insightText = if (streakAnalysis.currentCount >= 4) {
                    "Current ${streakAnalysis.currentType} streak is at ${streakAnalysis.currentCount}. High mean-reversion probability suggests preparing for an opposite flip."
                } else if (bigPct > 55f) {
                    "Big is currently dominating at %.1f%%. Trend continuation favored on medium streaks.".format(Locale.US, bigPct)
                } else if (smallPct > 55f) {
                    "Small is currently dominating at %.1f%%. Watch for streak consolidation.".format(Locale.US, smallPct)
                } else {
                    "Even 50/50 balance between Big and Small. Alternation pattern probability is high (%.0f%%).".format(Locale.US, streakAnalysis.alternationRate)
                }

                Text(
                    text = insightText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class StreakResult(
    val currentType: String,
    val currentCount: Int,
    val maxBigStreak: Int,
    val maxSmallStreak: Int,
    val alternationRate: Float
)

private fun calculateStreaks(records: List<WinGoRecord>): StreakResult {
    if (records.isEmpty()) {
        return StreakResult("None", 0, 0, 0, 50f)
    }

    var currentType = ""
    var currentCount = 0
    var maxBig = 0
    var maxSmall = 0
    var currentStreak = 0
    var lastType = ""
    var alternations = 0

    records.forEachIndexed { index, record ->
        val type = if (record.bigSmall.equals("Big", ignoreCase = true)) "Big" else "Small"

        if (index == 0) {
            currentType = type
            currentCount = 1
            currentStreak = 1
            lastType = type
        } else {
            if (index < 10 && type == currentType && currentCount == index) {
                currentCount++
            }
            if (type == lastType) {
                currentStreak++
            } else {
                alternations++
                currentStreak = 1
            }
        }

        if (type == "Big" && currentStreak > maxBig) maxBig = currentStreak
        if (type == "Small" && currentStreak > maxSmall) maxSmall = currentStreak

        lastType = type
    }

    val rate = if (records.size > 1) (alternations.toFloat() / (records.size - 1)) * 100f else 50f

    return StreakResult(
        currentType = currentType,
        currentCount = currentCount,
        maxBigStreak = maxBig,
        maxSmallStreak = maxSmall,
        alternationRate = rate
    )
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}
