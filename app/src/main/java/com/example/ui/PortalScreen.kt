package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.WinGoRecord
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PortalScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val recordCount by viewModel.recordCount.collectAsStateWithLifecycle()
    val latestRecord by viewModel.latestRecord.collectAsStateWithLifecycle()
    val scraperStatus = uiState.scraperStatus

    val snackbarHostState = remember { SnackbarHostState() }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewRecreateKey by remember { mutableIntStateOf(0) }

    val portalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentDomain = remember(uiState.currentUrl) {
        val url = uiState.currentUrl
        if (url.contains("dmfirst2")) "dmfirst2.com" else "dmfirst1.com"
    }

    // Intercept back button if modal sheet or webview is open
    BackHandler(enabled = uiState.isPortalSheetOpen) {
        viewModel.setPortalSheetOpen(false)
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    // Handle WebView commands from ViewModel
    LaunchedEffect(Unit) {
        viewModel.webCommands.collectLatest { command ->
            val wv = webViewInstance ?: return@collectLatest
            when (command) {
                is WebCommand.LoadUrl -> {
                    wv.loadUrl(command.url)
                }
                is WebCommand.ExecuteJs -> {
                    wv.evaluateJavascript(command.js, null)
                }
                WebCommand.Reload -> {
                    wv.reload()
                }
                WebCommand.GoBack -> {
                    if (wv.canGoBack()) wv.goBack()
                }
                WebCommand.GoForward -> {
                    if (wv.canGoForward()) wv.goForward()
                }
                WebCommand.ClearCache -> {
                    wv.clearCache(true)
                    wv.clearHistory()
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                    wv.loadUrl(uiState.credentials.loginUrl)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = "WinGo 1Min Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "WinGo 1Min",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                // Live sync badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (scraperStatus.isRealtimeActive) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (scraperStatus.isRealtimeActive) EmeraldSuccess else MaterialTheme.colorScheme.onSecondaryContainer)
                                        )
                                        Text(
                                            text = if (scraperStatus.isRealtimeActive) "Live" else "DB Ready",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (scraperStatus.isRealtimeActive) EmeraldSuccess else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Big / Small Analysis • $recordCount stored",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Settings
                    IconButton(
                        onClick = { viewModel.setSettingsOpen(true) },
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Portal Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Tab Row: Analysis vs Records Table vs Forecast (3 clean tabs)
            TabRow(
                selectedTabIndex = uiState.selectedTab.coerceIn(0, 2),
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("1Min Analysis") },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_analysis")
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Records DB")
                            if (recordCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = "$recordCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_records")
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = { Text("Forecast %") },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_forecast")
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.selectedTab) {
                    0 -> {
                        WinGoAnalysisDashboard(
                            records = records,
                            latestRecord = latestRecord,
                            countdownSeconds = uiState.countdownSeconds,
                            currentLivePeriodId = uiState.currentLivePeriodId,
                            onViewRecordsClick = { viewModel.setSelectedTab(1) },
                            onViewForecastClick = { viewModel.setSelectedTab(2) },
                            onAddCustomRecord = { period, num -> viewModel.addCustomRecord(period, num) }
                        )
                    }
                    1 -> {
                        val context = LocalContext.current
                        HomeRecordsTableView(
                            records = records,
                            recordCount = recordCount,
                            scraperStatus = scraperStatus,
                            onClearClick = { viewModel.clearDatabaseRecords() },
                            onExportClick = { viewModel.exportRecordsToClipboard(context) },
                            onAddRecord = { period, num -> viewModel.addCustomRecord(period, num) }
                        )
                    }
                    2 -> {
                        WinGoPredictionForecastScreen(
                            records = records,
                            currentLivePeriodId = uiState.currentLivePeriodId,
                            countdownSeconds = uiState.countdownSeconds,
                            onViewRecordsClick = { viewModel.setSelectedTab(1) }
                        )
                    }
                }
            }

            // Off-screen Headless Background WebView completely disabled
            val isHeadlessWebViewNeeded = false
            if (isHeadlessWebViewNeeded) {
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .alpha(0.001f)
                ) {
                key(webViewRecreateKey) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(1, 1)

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    displayZoomControls = false
                                    builtInZoomControls = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    cacheMode = WebSettings.LOAD_DEFAULT

                                    val defaultUa = userAgentString
                                    userAgentString = defaultUa.replace("; wv", "")
                                }

                                addJavascriptInterface(
                                    WebAppInterface(
                                        onStatus = { msg ->
                                            post { viewModel.onStatusUpdateFromJs(msg) }
                                        },
                                        onAutoFilled = {
                                            post { viewModel.onAutoFillSuccessFromJs() }
                                        },
                                        onLoginClicked = {
                                            post { viewModel.onLoginButtonClickedFromJs() }
                                        },
                                        onRedirectHome = { url ->
                                            post { viewModel.onLoginRedirectFromJs(url) }
                                        },
                                        onBatchRecordsReceived = { json, page, totalPages ->
                                            post { viewModel.onBatchRecordsReceived(json, page, totalPages) }
                                        },
                                        onNewRecordReceived = { json ->
                                            post { viewModel.onNewRecordReceived(json) }
                                        },
                                        onScrapeProgressUpdate = { page, totalPages, count ->
                                            post { viewModel.onScrapeProgressUpdate(page, totalPages, count) }
                                        },
                                        onScrapeCompleted = { count ->
                                            post { viewModel.onScrapeFinished(count) }
                                        }
                                    ),
                                    "AndroidBridge"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                            return true
                                        }
                                        viewModel.onUrlChanged(url)
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        url?.let { viewModel.onPageStarted(it) }
                                        viewModel.updateNavigationState(canGoBack(), canGoForward())
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        url?.let { viewModel.onPageFinished(it) }
                                        viewModel.updateNavigationState(canGoBack(), canGoForward())
                                    }

                                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                        super.doUpdateVisitedHistory(view, url, isReload)
                                        url?.let { viewModel.onUrlChanged(it) }
                                        viewModel.updateNavigationState(canGoBack(), canGoForward())
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: android.webkit.SslErrorHandler?,
                                        error: android.net.http.SslError?
                                    ) {
                                        handler?.proceed()
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: android.webkit.RenderProcessGoneDetail?
                                    ): Boolean {
                                        try {
                                            view?.let {
                                                (it.parent as? ViewGroup)?.removeView(it)
                                                it.destroy()
                                            }
                                            webViewInstance = null
                                            webViewRecreateKey++
                                        } catch (_: Exception) {}
                                        return true
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            val errDesc = error?.description?.toString() ?: "Network error"
                                            viewModel.onPageError(errDesc)
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        viewModel.onProgressChanged(newProgress)
                                        viewModel.updateNavigationState(canGoBack(), canGoForward())
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        title?.let { viewModel.onTitleChanged(it) }
                                    }
                                }

                                webViewInstance = this
                                loadUrl(uiState.credentials.loginUrl)
                            }
                        },
                        update = {
                            webViewInstance = it
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        }
    }

    // Modal Bottom Sheet: Full Web Portal Inspector (Disabled)
    if (false && uiState.isPortalSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setPortalSheetOpen(false) },
            sheetState = portalSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp)
            ) {
                // Header & Controls
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.goBack() },
                                enabled = uiState.canGoBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            IconButton(
                                onClick = { viewModel.goForward() },
                                enabled = uiState.canGoForward,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                            }
                            IconButton(
                                onClick = { viewModel.reload() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.triggerAutoLogin() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto Login", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = { viewModel.navigateToWinGo1Min() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open WinGo 1Min", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Interactive Portal WebView Inside Sheet
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            val cm = CookieManager.getInstance()
                            cm.setAcceptCookie(true)
                            cm.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                val defaultUa = userAgentString
                                userAgentString = defaultUa.replace("; wv", "")
                            }
                            loadUrl(uiState.currentUrl)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }

    // Portal Settings Modal Sheet
    if (uiState.isSettingsOpen) {
        PortalSettingsSheet(
            credentials = uiState.credentials,
            onDismiss = { viewModel.setSettingsOpen(false) },
            onSave = { phone, pass, autoLogin, portalUrl ->
                viewModel.saveCredentials(phone, pass, autoLogin, portalUrl)
            },
            onResetDefaults = {
                viewModel.resetToDefaults()
            },
            onClearCache = {
                viewModel.clearCacheAndCookies()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}

@Composable
fun HomeRecordsTableView(
    records: List<WinGoRecord>,
    recordCount: Int,
    scraperStatus: ScraperStatus,
    onClearClick: () -> Unit,
    onExportClick: () -> Unit = {},
    onAddRecord: (period: String, number: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<WinGoRecord?>(null) }

    if (showAddDialog) {
        QuickAddRecordDialog(
            suggestedPeriod = records.firstOrNull()?.let {
                val seq = WinGoFormula.extractSequence(it.period) ?: 10000
                it.period.dropLast(5) + String.format(java.util.Locale.US, "%05d", seq + 1)
            } ?: "",
            onDismiss = { showAddDialog = false },
            onConfirm = { period, num ->
                onAddRecord(period, num)
                showAddDialog = false
            }
        )
    }

    if (editingRecord != null) {
        QuickAddRecordDialog(
            suggestedPeriod = editingRecord!!.period,
            onDismiss = { editingRecord = null },
            onConfirm = { period, num ->
                onAddRecord(period, num)
                editingRecord = null
            }
        )
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = {
                Text("Clear Permanent Database?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete all $recordCount records from local Room SQLite and Firestore? This will remove historical records used for analysis.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearClick()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All Records")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Action & Sync Control Bar
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WinGo 1Min Database ($recordCount records)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Permanent Room SQLite Storage • Stored for analysis",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess)
                                )
                                Text(
                                    text = "Live Active (<5s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Drawn Result", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onExportClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showConfirmClearDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(0.9f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ Tap on any record row below to edit or correct its drawn ball number.",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Search & Filter
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Period ID (e.g. 20260928...)") },
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
                    text = "Period ID",
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
                    text = "Big / Small",
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
                            text = if (searchQuery.isNotEmpty()) "No matching records found" else "No WinGo 1Min Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Drawn Result' above to log a new round.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.period }) { record ->
                RecordRowItem(
                    record = record,
                    onClick = { editingRecord = record }
                )
            }
        }
    }
}

@Composable
fun DmFirstLivePortalView(
    viewModel: PortalViewModel,
    uiState: PortalUiState,
    scraperStatus: ScraperStatus,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top Web Control Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { viewModel.goBack() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { viewModel.goForward() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { viewModel.reload() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.triggerAutoLogin() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto Login", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = { viewModel.navigateToWinGo1Min() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WinGo 1Min", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Scraper Status and Actions Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = "⚡ Real-Time Auto Sync Active (<5s): Results update immediately upon draw",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Live Interactive WebView
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        val defaultUa = userAgentString
                        userAgentString = defaultUa.replace("; wv", "")
                    }

                    addJavascriptInterface(
                        WebAppInterface(
                            onStatus = { msg ->
                                post { viewModel.onStatusUpdateFromJs(msg) }
                            },
                            onAutoFilled = {
                                post { viewModel.onAutoFillSuccessFromJs() }
                            },
                            onLoginClicked = {
                                post { viewModel.onLoginButtonClickedFromJs() }
                            },
                            onRedirectHome = { url ->
                                post { viewModel.onLoginRedirectFromJs(url) }
                            },
                            onBatchRecordsReceived = { json, page, total ->
                                post { viewModel.onBatchRecordsReceived(json, page, total) }
                            },
                            onNewRecordReceived = { json ->
                                post { viewModel.onNewRecordReceived(json) }
                            },
                            onScrapeProgressUpdate = { cur, tot, stored ->
                                post { viewModel.onScrapeProgressUpdate(cur, tot, stored) }
                            },
                            onScrapeCompleted = { total ->
                                post { viewModel.onScrapeFinished(total) }
                            }
                        ),
                        "AndroidBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (url != null) viewModel.onUrlChanged(url)
                            if (uiState.credentials.isAutoLoginEnabled && url?.contains("login", ignoreCase = true) == true) {
                                viewModel.triggerAutoLogin()
                            } else {
                                view?.evaluateJavascript(PortalJsHelper.generateRealtimeWatcherScript(), null)
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            try {
                                view?.let {
                                    (it.parent as? ViewGroup)?.removeView(it)
                                    it.destroy()
                                }
                            } catch (_: Exception) {}
                            return true
                        }
                    }

                    loadUrl(uiState.currentUrl)
                }
            },
            onRelease = { webView ->
                try {
                    webView.stopLoading()
                    webView.removeJavascriptInterface("AndroidBridge")
                    webView.destroy()
                } catch (_: Exception) {}
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

