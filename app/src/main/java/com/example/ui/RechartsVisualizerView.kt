package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

private val GoldBigColor = Color(0xFFF59E0B)
private val GoldBigDark = Color(0xFFB45309)
private val CyanSmallColor = Color(0xFF06B6D4)
private val CyanSmallDark = Color(0xFF0E7490)
private val PurpleViolet = Color(0xFF8B5CF6)

@Composable
fun RechartsVisualizerView(
    records: List<WinGoRecord>,
    modifier: Modifier = Modifier
) {
    val totalCount = records.size
    val bigRecords = remember(records) { records.filter { it.bigSmall.equals("Big", ignoreCase = true) } }
    val smallRecords = remember(records) { records.filter { it.bigSmall.equals("Small", ignoreCase = true) } }
    val bigCount = bigRecords.size
    val smallCount = smallRecords.size
    val bigPct = if (totalCount > 0) (bigCount * 100f / totalCount) else 50f
    val smallPct = if (totalCount > 0) (smallCount * 100f / totalCount) else 50f

    val numberCounts = remember(records) {
        val arr = IntArray(10) { 0 }
        records.forEach { if (it.number in 0..9) arr[it.number]++ }
        arr
    }
    val maxNumberFreq = remember(numberCounts) {
        (numberCounts.maxOrNull() ?: 1).coerceAtLeast(1)
    }

    val greenCount = remember(records) { records.count { it.color.contains("Green", ignoreCase = true) } }
    val redCount = remember(records) { records.count { it.color.contains("Red", ignoreCase = true) } }
    val violetCount = remember(records) { records.count { it.color.contains("Violet", ignoreCase = true) } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("native_visualizer_container"),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Big vs Small Visual Gauge Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Big vs Small Distribution ($totalCount rounds)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Dual Color Proportional Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(bigPct.coerceAtLeast(1f))
                                .background(Brush.horizontalGradient(listOf(GoldBigDark, GoldBigColor)))
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BIG ${"%.1f".format(bigPct)}% ($bigCount)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(smallPct.coerceAtLeast(1f))
                                .background(Brush.horizontalGradient(listOf(CyanSmallColor, CyanSmallDark)))
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SMALL ${"%.1f".format(smallPct)}% ($smallCount)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Bead Road Pattern Matrix (Native 6xN Grid)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "WinGo 1Min Bead Road Matrix",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Chronological bead road (Recent 36 periods). Gold: Big (5-9), Cyan: Small (0-4)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val recentRecords = remember(records) { records.take(36) }
                    // 6 rows x 6 columns grid display
                    val columns = (recentRecords.size + 5) / 6
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until columns.coerceAtLeast(1)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (row in 0 until 6) {
                                    val idx = col * 6 + row
                                    if (idx < recentRecords.size) {
                                        val rec = recentRecords[idx]
                                        val isBig = rec.bigSmall.equals("Big", ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isBig) GoldBigColor else CyanSmallColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isBig) "B" else "S",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Number Frequency Bar Chart (0 to 9)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Digit Frequency Analysis (0 - 9)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 10 Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        for (digit in 0..9) {
                            val count = numberCounts[digit]
                            val barHeightFraction = (count.toFloat() / maxNumberFreq).coerceIn(0.08f, 1f)
                            val barColor = when (digit) {
                                0 -> RoseError
                                5 -> EmeraldSuccess
                                1, 3, 7, 9 -> EmeraldSuccess
                                else -> RoseError
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height((100 * barHeightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$digit",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Color Distribution Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Color Outcome Distribution",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorMetricChip(
                            title = "Green",
                            count = greenCount,
                            total = totalCount,
                            color = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        ColorMetricChip(
                            title = "Red",
                            count = redCount,
                            total = totalCount,
                            color = RoseError,
                            modifier = Modifier.weight(1f)
                        )
                        ColorMetricChip(
                            title = "Violet",
                            count = violetCount,
                            total = totalCount,
                            color = PurpleViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorMetricChip(
    title: String,
    count: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pct = if (total > 0) (count * 100f / total) else 0f
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = "${"%.1f".format(pct)}%",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}
