package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.WinGoRecord
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseHistoryScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val recordCount by viewModel.recordCount.collectAsStateWithLifecycle()
    val latestRecord by viewModel.latestRecord.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scraperStatus = uiState.scraperStatus

    var selectedViewTab by remember { mutableIntStateOf(0) } // 0: Recharts Visualizer, 1: Records Table
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredRecords = remember(records, searchQuery, selectedFilter) {
        records.filter { rec ->
            val matchesQuery = searchQuery.isBlank() || rec.period.contains(searchQuery.trim())
            val matchesFilter = when (selectedFilter) {
                "Big" -> rec.bigSmall.equals("Big", ignoreCase = true)
                "Small" -> rec.bigSmall.equals("Small", ignoreCase = true)
                "Green" -> rec.color.contains("Green", ignoreCase = true)
                "Red" -> rec.color.contains("Red", ignoreCase = true)
                "Violet" -> rec.color.contains("Violet", ignoreCase = true)
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    // Stats calculations
    val bigCount = remember(records) { records.count { it.bigSmall.equals("Big", ignoreCase = true) } }
    val smallCount = remember(records) { records.count { it.bigSmall.equals("Small", ignoreCase = true) } }
    val greenCount = remember(records) { records.count { it.color.contains("Green", ignoreCase = true) } }
    val redCount = remember(records) { records.count { it.color.contains("Red", ignoreCase = true) } }
    val violetCount = remember(records) { records.count { it.color.contains("Violet", ignoreCase = true) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scraper progress banner if scraping
        AnimatedVisibility(visible = scraperStatus.isScraping) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Syncing 50 Pages: Page ${scraperStatus.currentPage}/${scraperStatus.totalPages}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${scraperStatus.totalRecordsScraped} records saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { scraperStatus.currentPage.toFloat() / scraperStatus.totalPages.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Action & Sync Control Bar (Common to both views)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WinGo 1Min Database ($recordCount records)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Big vs Small Distribution & Live Pattern Analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Cloud sync badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Firestore Cloud",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Live watcher badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (scraperStatus.isRealtimeActive) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = if (scraperStatus.isRealtimeActive) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(8.dp)
                                )
                                Text(
                                    text = if (scraperStatus.isRealtimeActive) "Live Active" else "Live Idle",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (scraperStatus.isRealtimeActive) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.start50PagesScrape() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("sync_50_pages_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync 50 Pages", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { viewModel.toggleRealtimeWatcher() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("toggle_realtime_button")
                    ) {
                        Icon(
                            if (scraperStatus.isRealtimeActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (scraperStatus.isRealtimeActive) RoseError else EmeraldSuccess
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (scraperStatus.isRealtimeActive) "Pause Live" else "Start Live",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearDatabaseRecords() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("clear_db_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Tab Row (Recharts Visualizer vs Records List)
        TabRow(
            selectedTabIndex = selectedViewTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedViewTab == 0,
                onClick = { selectedViewTab = 0 },
                text = { Text("Recharts Visualizer") },
                icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_recharts_visualizer")
            )
            Tab(
                selected = selectedViewTab == 1,
                onClick = { selectedViewTab = 1 },
                text = { Text("Records Table") },
                icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_records_table")
            )
        }

        // Content Area based on Selected Tab
        when (selectedViewTab) {
            0 -> {
                // Recharts Interactive Visualizer View
                Box(modifier = Modifier.fillMaxSize()) {
                    RechartsVisualizerView(
                        records = records,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            1 -> {
                // Full Historical Records Table with Search and Filters
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Breakdown Row
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Quick Distribution Summary",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    DistributionPill(label = "Big", count = bigCount, total = recordCount, color = Color(0xFFF59E0B))
                                    DistributionPill(label = "Small", count = smallCount, total = recordCount, color = Color(0xFF3B82F6))
                                    DistributionPill(label = "Green", count = greenCount, total = recordCount, color = EmeraldSuccess)
                                    DistributionPill(label = "Red", count = redCount, total = recordCount, color = RoseError)
                                    DistributionPill(label = "Violet", count = violetCount, total = recordCount, color = Color(0xFF8B5CF6))
                                }
                            }
                        }
                    }

                    // Search & Filter Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by Period number...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search icon")
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_period_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("All", "Big", "Small", "Green", "Red", "Violet").forEach { filter ->
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = { selectedFilter = filter },
                                        label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Column Headers
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Period",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.4f)
                            )
                            Text(
                                text = "Number",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Big/Small",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Color",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }

                    // Records List Rows
                    if (filteredRecords.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "No matching records found" else "No Records Stored Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Open Portal Browser, navigate to Win Go 1 min, and tap 'Sync 50 Pages'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredRecords, key = { it.period }) { record ->
                            RecordRowItem(record = record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordRowItem(record: WinGoRecord) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_item_${record.period}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Period
            Column(modifier = Modifier.weight(1.4f)) {
                Text(
                    text = record.period,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Page ${record.page}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Number Ball
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NumberBall(number = record.number, colorName = record.color, size = 28.dp)
            }

            // Big / Small Chip
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val isBig = record.bigSmall.equals("Big", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isBig) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = record.bigSmall,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBig) Color(0xFFD97706) else Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Color Indicator
            Row(
                modifier = Modifier.weight(1.2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                ColorBadge(colorName = record.color)
            }
        }
    }
}

@Composable
fun NumberBall(number: Int, colorName: String, size: androidx.compose.ui.unit.Dp) {
    val isZero = number == 0
    val isFive = number == 5
    val isGreen = listOf(1, 3, 7, 9).contains(number)

    val ballBackgroundModifier = when {
        isZero -> Modifier.background(
            Brush.horizontalGradient(
                listOf(Color(0xFFEF4444), Color(0xFF8B5CF6))
            )
        )
        isFive -> Modifier.background(
            Brush.horizontalGradient(
                listOf(Color(0xFF10B981), Color(0xFF8B5CF6))
            )
        )
        isGreen -> Modifier.background(EmeraldSuccess)
        else -> Modifier.background(RoseError)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(ballBackgroundModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            color = Color.White,
            style = if (size > 30.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ColorBadge(colorName: String) {
    val lower = colorName.lowercase()
    val isRedViolet = lower.contains("red") && lower.contains("violet")
    val isGreenViolet = lower.contains("green") && lower.contains("violet")
    val isViolet = lower.contains("violet") || lower.contains("purple")
    val isGreen = lower.contains("green")

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = when {
            isRedViolet -> Color(0xFFEF4444).copy(alpha = 0.15f)
            isGreenViolet -> Color(0xFF10B981).copy(alpha = 0.15f)
            isViolet -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
            isGreen -> EmeraldSuccess.copy(alpha = 0.15f)
            else -> RoseError.copy(alpha = 0.15f)
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isRedViolet) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6))
                )
                Text(
                    text = "Red + Violet",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            } else if (isGreenViolet) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6))
                )
                Text(
                    text = "Green + Violet",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669)
                )
            } else if (isGreen) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess)
                )
                Text(
                    text = "Green",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
            } else if (isViolet) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6))
                )
                Text(
                    text = "Violet",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5CF6)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(RoseError)
                )
                Text(
                    text = "Red",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = RoseError
                )
            }
        }
    }
}

@Composable
fun DistributionPill(label: String, count: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    val percentage = if (total > 0) (count * 100 / total) else 0
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "$label ($percentage%)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
