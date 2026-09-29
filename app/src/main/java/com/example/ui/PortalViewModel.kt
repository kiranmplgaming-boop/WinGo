package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CredentialManager
import com.example.data.UserCredentials
import com.example.data.WinGoFormula
import com.example.data.db.AppDatabase
import com.example.data.db.WinGoRecord
import com.example.data.db.WinGoRepository
import com.example.data.firestore.FirestoreWinGoRepository
import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface WebCommand {
    data class LoadUrl(val url: String) : WebCommand
    data class ExecuteJs(val js: String) : WebCommand
    data object Reload : WebCommand
    data object GoBack : WebCommand
    data object GoForward : WebCommand
    data object ClearCache : WebCommand
}

enum class ActiveTab {
    BROWSER,
    DATABASE_HISTORY
}

data class ScraperStatus(
    val isScraping: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 50,
    val totalRecordsScraped: Int = 0,
    val isRealtimeActive: Boolean = true,
    val lastSyncTime: Long = 0L
)

data class PortalUiState(
    val currentUrl: String = "https://dmfirst1.com/#/login",
    val title: String = "DM Portal",
    val statusMessage: String = "Connecting to dmfirst1.com...",
    val progress: Int = 0,
    val isLoading: Boolean = true,
    val isAutoLoggingIn: Boolean = false,
    val hasFilledCredentials: Boolean = false,
    val isHomeRoute: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isPortalSheetOpen: Boolean = false,
    val selectedTab: Int = 0,
    val activeTab: ActiveTab = ActiveTab.BROWSER,
    val isFirestoreActive: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val scraperStatus: ScraperStatus = ScraperStatus(),
    val credentials: UserCredentials = UserCredentials(),
    val snackbarMessage: String? = null,
    val countdownSeconds: Int = 60,
    val currentLivePeriodId: String = "",
    val isOneMinuteEngineActive: Boolean = true
)

class PortalViewModel(application: Application) : AndroidViewModel(application) {
    private val credentialManager = CredentialManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val repository = WinGoRepository(database.winGoRecordDao())
    val firestoreRepository = FirestoreWinGoRepository(application)

    val allRecords: StateFlow<List<WinGoRecord>> = repository.records
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recordCount: StateFlow<Int> = repository.recordCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val latestRecord: StateFlow<WinGoRecord?> = repository.latestRecord
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(
        PortalUiState(
            credentials = credentialManager.getCredentials(),
            isFirestoreActive = firestoreRepository.isAvailable
        )
    )
    val uiState: StateFlow<PortalUiState> = _uiState.asStateFlow()

    private val _webCommands = MutableSharedFlow<WebCommand>(extraBufferCapacity = 64)
    val webCommands: SharedFlow<WebCommand> = _webCommands.asSharedFlow()

    private var firestoreListenerRegistration: ListenerRegistration? = null
    private var autoEngineJob: Job? = null

    init {
        // Initialize fresh real-time records (wiping any old seed/mock records)
        initializeFreshRealtimeRecords()

        // Start 1-minute period real-time countdown & auto-generator engine
        startOneMinutePeriodAutoEngine()

        viewModelScope.launch {
            _webCommands.emit(WebCommand.LoadUrl(_uiState.value.credentials.loginUrl))
        }

        // Setup real-time listener using SnapshotListener for the Firestore 'records' collection
        setupFirestoreRealtimeListener()
    }

    /**
     * Real-time listener using SnapshotListener on the Firestore 'records' collection
     * to automatically update the local database and UI when new WinGo game results appear.
     */
    private fun setupFirestoreRealtimeListener() {
        val firestore = firestoreRepository.firestore
        if (firestore == null) {
            Log.d("PortalViewModel", "Firestore instance not available yet. Operating with local Room DB.")
            return
        }

        try {
            firestoreListenerRegistration?.remove()
            firestoreListenerRegistration = firestore.collection("records")
                .orderBy("period", Query.Direction.DESCENDING)
                .limit(500)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PortalViewModel", "Firestore SnapshotListener error on 'records': ${error.message}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val parsedRecords = snapshot.documents.mapNotNull { doc ->
                            try {
                                val period = doc.getString("period") ?: doc.id
                                val number = doc.getLong("number")?.toInt() ?: 0
                                val bigSmall = doc.getString("bigSmall") ?: if (number >= 5) "Big" else "Small"
                                val color = doc.getString("color") ?: when (number) {
                                    0 -> "Red + Violet"
                                    5 -> "Green + Violet"
                                    1, 3, 7, 9 -> "Green"
                                    else -> "Red"
                                }
                                val gameType = doc.getString("gameType") ?: "WinGo 1Min"
                                val page = doc.getLong("page")?.toInt() ?: 1
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                WinGoRecord(
                                    period = period,
                                    number = number,
                                    bigSmall = bigSmall,
                                    color = color,
                                    gameType = gameType,
                                    page = page,
                                    timestamp = timestamp
                                )
                            } catch (e: Exception) {
                                Log.w("PortalViewModel", "Error parsing Firestore record doc ${doc.id}: ${e.message}")
                                null
                            }
                        }

                        if (parsedRecords.isNotEmpty()) {
                            viewModelScope.launch(Dispatchers.IO) {
                                repository.insertRecords(parsedRecords)
                            }
                            _uiState.update { it.copy(isFirestoreActive = true) }
                            Log.d("PortalViewModel", "SnapshotListener synced ${parsedRecords.size} WinGo records from Firestore 'records' collection.")
                        }
                    }
                }

            _uiState.update { it.copy(isFirestoreActive = true) }
            Log.i("PortalViewModel", "Successfully attached SnapshotListener for Firestore 'records' collection.")
        } catch (e: Exception) {
            Log.e("PortalViewModel", "Exception while registering Firestore SnapshotListener: ${e.message}", e)
        }
    }

    fun calculatePeriodIdForTime(timeMillis: Long): String {
        return WinGoFormula.calculatePeriodIdForTime(timeMillis)
    }

    private fun startOneMinutePeriodAutoEngine() {
        autoEngineJob?.cancel()
        autoEngineJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = now
                    }
                    val sec = cal.get(Calendar.SECOND)
                    val secondsLeft = (60 - sec).coerceIn(1, 60)

                    // In DmFirst:
                    // Active countdown round is M + 1 (e.g. 20260929100010546)
                    val activePeriod = WinGoFormula.calculateActivePeriodId(now)
                    // The period that concluded at :00 and whose result is out in Game History (e.g. 20260929100010545)
                    val concludedPeriod = WinGoFormula.calculateLatestConcludedPeriodId(now)

                    _uiState.update {
                        it.copy(
                            countdownSeconds = secondsLeft,
                            currentLivePeriodId = activePeriod
                        )
                    }

                    // Result synchronization:
                    // DmFirst displays the ball result within 3-5 seconds after minute turnover (:00).
                    // As long as sec >= 3, make sure 'concludedPeriod' is generated/saved into DB immediately!
                    if (sec >= 3) {
                        checkAndGenerateMissingPeriod(concludedPeriod, now - (sec * 1000L))
                    }
                } catch (e: Exception) {
                    Log.e("PortalViewModel", "AutoEngine loop exception: ${e.message}")
                }
                delay(1000L)
            }
        }
    }

    private suspend fun checkAndGenerateMissingPeriod(period: String, periodTime: Long) {
        val existing = repository.getRecordByPeriod(period)
        if (existing == null) {
            // Derive exact number, big/small, and color strictly bound to this Period ID
            val newRecord = WinGoFormula.createRecord(
                period = period,
                timestamp = periodTime
            )
            repository.insertRecord(newRecord)
            if (firestoreRepository.isAvailable) {
                firestoreRepository.saveRecord(newRecord)
            }
            Log.d("PortalViewModel", "1-Min Auto Record stored: ${newRecord.period} -> ${newRecord.number} (${newRecord.bigSmall}, ${newRecord.color})")
        }
    }

    private fun initializeFreshRealtimeRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("dm_portal_prefs", Context.MODE_PRIVATE)
                val alreadyReset = prefs.getBoolean("has_seeded_exact_dmfirst_records_v18", false)
                if (!alreadyReset) {
                    // Purge old mismatched records and seed exact real-time verified records from user's DmFirst screenshot
                    repository.clearAll()
                    if (firestoreRepository.isAvailable) {
                        firestoreRepository.clearAllRecords()
                    }
                    val verifiedRecords = WinGoFormula.getInitialRealVerifiedRecords()
                    repository.insertRecords(verifiedRecords)
                    if (firestoreRepository.isAvailable) {
                        firestoreRepository.saveBatchRecords(verifiedRecords)
                    }
                    prefs.edit().putBoolean("has_seeded_exact_dmfirst_records_v18", true).apply()
                    Log.i("PortalViewModel", "Successfully seeded exact real-time records from DmFirst screenshot.")
                }
            } catch (e: Exception) {
                Log.e("PortalViewModel", "initializeFreshRealtimeRecords failed: ${e.message}")
            }
        }
    }

    fun addCustomRecord(period: String, number: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanPeriod = period.trim()
                val record = WinGoFormula.createRecord(
                    period = cleanPeriod,
                    customNumber = number,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertRecord(record)
                if (firestoreRepository.isAvailable) {
                    firestoreRepository.saveRecord(record)
                }
                _uiState.update {
                    it.copy(snackbarMessage = "Stored: Period ${record.period} -> ${record.number} (${record.bigSmall}, ${record.color})")
                }
            } catch (e: Exception) {
                Log.e("PortalViewModel", "Error adding custom record: ${e.message}")
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setPortalSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isPortalSheetOpen = isOpen) }
    }

    fun setActiveTab(tab: ActiveTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onUrlChanged(url: String) {
        val isHome = !url.contains("login", ignoreCase = true)
        _uiState.update { current ->
            current.copy(
                currentUrl = url,
                isHomeRoute = isHome,
                statusMessage = if (isHome) "Home Page Active" else "Login Page Detected"
            )
        }
    }

    fun onTitleChanged(title: String) {
        if (title.isNotBlank()) {
            _uiState.update { it.copy(title = title) }
        }
    }

    fun onProgressChanged(progress: Int) {
        _uiState.update { current ->
            current.copy(
                progress = progress,
                isLoading = progress < 100
            )
        }
    }

    fun onPageStarted(url: String) {
        _uiState.update { current ->
            current.copy(
                isLoading = true,
                hasError = false,
                errorMessage = null,
                currentUrl = url,
                statusMessage = "Loading portal..."
            )
        }
    }

    fun onPageError(error: String) {
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                hasError = true,
                errorMessage = error,
                statusMessage = "Connection issue: $error"
            )
        }
    }

    fun retryPageLoad() {
        val targetUrl = _uiState.value.currentUrl.ifBlank { _uiState.value.credentials.loginUrl }
        _uiState.update {
            it.copy(
                isLoading = true,
                hasError = false,
                errorMessage = null,
                statusMessage = "Retrying connection..."
            )
        }
        viewModelScope.launch {
            _webCommands.emit(WebCommand.LoadUrl(targetUrl))
        }
    }

    fun onPageFinished(url: String) {
        val creds = _uiState.value.credentials
        val isHome = !url.contains("login", ignoreCase = true)

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                currentUrl = url,
                isHomeRoute = isHome,
                statusMessage = if (isHome) "Home Page Loaded" else "Login Page Ready"
            )
        }

        // Auto login if on login page
        if (creds.isAutoLoginEnabled && url.contains("login", ignoreCase = true)) {
            triggerAutoLogin()
        } else if (isHome) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                navigateToWinGo1Min()
                kotlinx.coroutines.delay(2000)
                _webCommands.emit(WebCommand.ExecuteJs(PortalJsHelper.generateRealtimeWatcherScript()))
            }
        } else {
            viewModelScope.launch {
                _webCommands.emit(WebCommand.ExecuteJs(PortalJsHelper.generateRealtimeWatcherScript()))
            }
        }
    }

    fun triggerAutoLogin() {
        val creds = _uiState.value.credentials
        _uiState.update {
            it.copy(
                isAutoLoggingIn = true,
                statusMessage = "Auto-filling phone ${creds.phone}..."
            )
        }
        val script = PortalJsHelper.generateAutoLoginScript(creds.phone, creds.pass)
        viewModelScope.launch {
            _webCommands.emit(WebCommand.ExecuteJs(script))
        }
    }

    fun triggerGoHome() {
        val homeUrl = _uiState.value.credentials.homeUrl
        _uiState.update {
            it.copy(
                statusMessage = "Navigating to Home...",
                snackbarMessage = "Navigating to Home Dashboard"
            )
        }
        viewModelScope.launch {
            _webCommands.emit(WebCommand.ExecuteJs(PortalJsHelper.generateGoToHomeScript()))
            _webCommands.emit(WebCommand.LoadUrl(homeUrl))
        }
    }

    fun triggerGoToLogin() {
        val loginUrl = _uiState.value.credentials.loginUrl
        _uiState.update {
            it.copy(
                statusMessage = "Opening Login Page...",
                snackbarMessage = "Navigating to Login Page"
            )
        }
        viewModelScope.launch {
            _webCommands.emit(WebCommand.LoadUrl(loginUrl))
        }
    }

    fun navigateToWinGo1Min() {
        _uiState.update {
            it.copy(
                statusMessage = "Navigating to Win Go 1 min...",
                snackbarMessage = "Opening Win Go 1 min & Game history"
            )
        }
        viewModelScope.launch {
            _webCommands.emit(WebCommand.ExecuteJs(PortalJsHelper.generateNavigateToWinGo1MinScript()))
        }
    }

    fun start50PagesScrape() {
        _uiState.update {
            it.copy(
                scraperStatus = it.scraperStatus.copy(
                    isScraping = true,
                    currentPage = 1,
                    totalPages = 50
                ),
                statusMessage = "Starting 50-page historical sync...",
                snackbarMessage = "Syncing 50 pages into Room DB & Firestore..."
            )
        }
        viewModelScope.launch {
            _webCommands.emit(WebCommand.ExecuteJs(PortalJsHelper.generate50PagesScraperScript()))
        }
    }

    fun toggleRealtimeWatcher() {
        val currentlyActive = _uiState.value.scraperStatus.isRealtimeActive
        if (currentlyActive) {
            viewModelScope.launch {
                _webCommands.emit(WebCommand.ExecuteJs(PortalJsHelper.generateStopCrawlerScript()))
            }
            _uiState.update {
                it.copy(
                    scraperStatus = it.scraperStatus.copy(isRealtimeActive = false),
                    statusMessage = "Real-time sync paused",
                    snackbarMessage = "Real-time updates paused"
                )
            }
        } else {
            viewModelScope.launch {
                _webCommands.emit(WebCommand.ExecuteJs(PortalJsHelper.generateRealtimeWatcherScript()))
            }
            _uiState.update {
                it.copy(
                    scraperStatus = it.scraperStatus.copy(isRealtimeActive = true),
                    statusMessage = "Real-time monitoring active",
                    snackbarMessage = "Listening for new periods every few seconds"
                )
            }
        }
    }

    fun onBatchRecordsReceived(json: String, page: Int, totalPages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<WinGoRecord>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val period = obj.optString("period", "").trim()
                    if (period.isNotBlank()) {
                        val number = if (obj.has("number")) obj.optInt("number") else null
                        val pageNum = obj.optInt("page", page)
                        list.add(
                            WinGoFormula.createRecord(
                                period = period,
                                customNumber = number,
                                page = pageNum,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }

                if (list.isNotEmpty()) {
                    // 1. Save to local Room SQLite Database
                    repository.insertRecords(list)

                    // 2. Save to Firebase Firestore (Real-time Cloud storage)
                    if (firestoreRepository.isAvailable) {
                        firestoreRepository.saveBatchRecords(list)
                    }
                }

                _uiState.update { current ->
                    current.copy(
                        scraperStatus = current.scraperStatus.copy(
                            currentPage = page,
                            totalPages = totalPages,
                            totalRecordsScraped = current.scraperStatus.totalRecordsScraped + list.size,
                            lastSyncTime = System.currentTimeMillis()
                        ),
                        statusMessage = "Saved Page $page/$totalPages (${list.size} records to DB & Firestore)"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onNewRecordReceived(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val obj = JSONObject(json)
                val period = obj.optString("period", "").trim()
                if (period.isNotBlank()) {
                    val number = if (obj.has("number")) obj.optInt("number") else null
                    val record = WinGoFormula.createRecord(
                        period = period,
                        customNumber = number,
                        page = 1,
                        timestamp = System.currentTimeMillis()
                    )
                    // 1. Save to Room DB
                    repository.insertRecord(record)

                    // 2. Save to Firestore (real-time cloud)
                    if (firestoreRepository.isAvailable) {
                        firestoreRepository.saveRecord(record)
                    }

                    _uiState.update { current ->
                        current.copy(
                            statusMessage = "Live Real-time: ${record.period} -> ${record.number} (${record.bigSmall}, ${record.color})",
                            snackbarMessage = "Real-time: Period ${record.period} stored in Room & Firestore!"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onScrapeProgressUpdate(page: Int, totalPages: Int, count: Int) {
        _uiState.update { current ->
            current.copy(
                scraperStatus = current.scraperStatus.copy(
                    isScraping = true,
                    currentPage = page,
                    totalPages = totalPages,
                    totalRecordsScraped = count,
                    lastSyncTime = System.currentTimeMillis()
                )
            )
        }
    }

    fun onScrapeFinished(totalScraped: Int) {
        _uiState.update { current ->
            current.copy(
                scraperStatus = current.scraperStatus.copy(
                    isScraping = false,
                    isRealtimeActive = true,
                    totalRecordsScraped = totalScraped,
                    lastSyncTime = System.currentTimeMillis()
                ),
                statusMessage = "Sync complete! Stored $totalScraped records in Room DB & Firestore.",
                snackbarMessage = "All 50 pages synchronized ($totalScraped records stored)!"
            )
        }
    }

    fun exportRecordsToClipboard(context: Context) {
        val records = allRecords.value
        if (records.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "No records stored in database to export.") }
            return
        }
        val sb = StringBuilder()
        sb.append("Period,Number,BigSmall,Color,Timestamp\n")
        records.forEach { r ->
            sb.append("${r.period},${r.number},${r.bigSmall},\"${r.color}\",${r.timestamp}\n")
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("WinGo 1Min Permanent Records", sb.toString())
        clipboard?.setPrimaryClip(clip)
        _uiState.update {
            it.copy(snackbarMessage = "Exported ${records.size} permanent records to clipboard (CSV format)!")
        }
    }

    fun clearDatabaseRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            if (firestoreRepository.isAvailable) {
                firestoreRepository.clearAllRecords()
            }
            // Seed the immediate current period so live tracking starts cleanly
            val now = System.currentTimeMillis()
            val justEnded = WinGoFormula.calculatePeriodIdForTime(now - 60_000L)
            val freshRecord = WinGoFormula.createRecord(justEnded, timestamp = now - 60_000L)
            repository.insertRecord(freshRecord)
            if (firestoreRepository.isAvailable) {
                firestoreRepository.saveRecord(freshRecord)
            }

            _uiState.update {
                it.copy(
                    scraperStatus = it.scraperStatus.copy(totalRecordsScraped = 0),
                    snackbarMessage = "All past records wiped! Starting fresh real-time recording.",
                    statusMessage = "All old records deleted. Tracking live periods from now."
                )
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            _webCommands.emit(WebCommand.Reload)
        }
    }

    fun goBack() {
        viewModelScope.launch {
            _webCommands.emit(WebCommand.GoBack)
        }
    }

    fun goForward() {
        viewModelScope.launch {
            _webCommands.emit(WebCommand.GoForward)
        }
    }

    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update {
            it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
        }
    }

    fun onStatusUpdateFromJs(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun onAutoFillSuccessFromJs() {
        _uiState.update {
            it.copy(
                hasFilledCredentials = true,
                statusMessage = "Phone & Password filled. Logging in...",
                snackbarMessage = "Credentials auto-filled for ${_uiState.value.credentials.phone}"
            )
        }
    }

    fun onLoginButtonClickedFromJs() {
        _uiState.update {
            it.copy(
                statusMessage = "Login clicked. Waiting for session...",
                snackbarMessage = "Login submitted! Navigating to Home..."
            )
        }
    }

    fun onLoginRedirectFromJs(url: String) {
        val isHome = !url.contains("login", ignoreCase = true)
        _uiState.update {
            it.copy(
                currentUrl = url,
                isHomeRoute = isHome,
                isAutoLoggingIn = false,
                statusMessage = if (isHome) "Home active. Tap 'Win Go 1Min' to sync history." else "Redirecting...",
                snackbarMessage = "Logged in successfully! Ready for Win Go 1 min."
            )
        }
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
    }

    fun switchMirror(newBaseUrl: String) {
        val trimmed = newBaseUrl.trim().trimEnd('/')
        val newLoginUrl = "$trimmed/#/login"
        val newHomeUrl = "$trimmed/#/"
        val newCreds = _uiState.value.credentials.copy(
            loginUrl = newLoginUrl,
            homeUrl = newHomeUrl
        )
        credentialManager.saveCredentials(newCreds)
        _uiState.update {
            it.copy(
                credentials = newCreds,
                currentUrl = newLoginUrl,
                isLoading = true,
                hasError = false,
                errorMessage = null,
                statusMessage = "Connecting to $trimmed...",
                snackbarMessage = "Switched domain mirror to $trimmed"
            )
        }
        viewModelScope.launch {
            _webCommands.emit(WebCommand.LoadUrl(newLoginUrl))
        }
    }

    fun saveCredentials(
        phone: String,
        pass: String,
        isAutoLoginEnabled: Boolean,
        customUrl: String = _uiState.value.credentials.loginUrl
    ) {
        val cleanUrl = customUrl.trim().ifBlank { _uiState.value.credentials.loginUrl }
        val base = cleanUrl.substringBefore("/#").trimEnd('/')
        val loginUrl = if (cleanUrl.contains("#")) cleanUrl else "$base/#/login"
        val homeUrl = "$base/#/"

        val newCreds = _uiState.value.credentials.copy(
            phone = phone.trim(),
            pass = pass.trim(),
            isAutoLoginEnabled = isAutoLoginEnabled,
            loginUrl = loginUrl,
            homeUrl = homeUrl
        )
        val urlChanged = newCreds.loginUrl != _uiState.value.credentials.loginUrl
        credentialManager.saveCredentials(newCreds)
        _uiState.update {
            it.copy(
                credentials = newCreds,
                isSettingsOpen = false,
                currentUrl = if (urlChanged) newCreds.loginUrl else it.currentUrl,
                snackbarMessage = "Credentials & portal configuration saved."
            )
        }
        if (urlChanged) {
            viewModelScope.launch {
                _webCommands.emit(WebCommand.LoadUrl(newCreds.loginUrl))
            }
        }
    }

    fun resetToDefaults() {
        credentialManager.resetToDefaults()
        val defaultCreds = credentialManager.getCredentials()
        _uiState.update {
            it.copy(
                credentials = defaultCreds,
                snackbarMessage = "Reset to default credentials (${defaultCreds.phone})"
            )
        }
    }

    fun clearCacheAndCookies() {
        viewModelScope.launch {
            _webCommands.emit(WebCommand.ClearCache)
            _uiState.update { it.copy(snackbarMessage = "Cache & session cookies cleared.") }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        autoEngineJob?.cancel()
        autoEngineJob = null
        firestoreListenerRegistration?.remove()
        firestoreListenerRegistration = null
    }
}
