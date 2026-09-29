package com.example.ui

import android.webkit.JavascriptInterface

class WebAppInterface(
    private val onStatus: (String) -> Unit,
    private val onAutoFilled: () -> Unit,
    private val onLoginClicked: () -> Unit,
    private val onRedirectHome: (String) -> Unit,
    private val onBatchRecordsReceived: (json: String, page: Int, totalPages: Int) -> Unit,
    private val onNewRecordReceived: (json: String) -> Unit,
    private val onScrapeProgressUpdate: (page: Int, totalPages: Int, count: Int) -> Unit,
    private val onScrapeCompleted: (totalScraped: Int) -> Unit
) {
    @JavascriptInterface
    fun onStatusUpdate(status: String) {
        onStatus(status)
    }

    @JavascriptInterface
    fun onAutoFillSuccess() {
        onAutoFilled()
    }

    @JavascriptInterface
    fun onLoginButtonClicked() {
        onLoginClicked()
    }

    @JavascriptInterface
    fun onLoginRedirect(url: String) {
        onRedirectHome(url)
    }

    @JavascriptInterface
    fun onBatchRecords(json: String, page: Int, totalPages: Int) {
        onBatchRecordsReceived(json, page, totalPages)
    }

    @JavascriptInterface
    fun onNewGameRecord(json: String) {
        onNewRecordReceived(json)
    }

    @JavascriptInterface
    fun onScrapeProgress(page: Int, totalPages: Int, count: Int) {
        onScrapeProgressUpdate(page, totalPages, count)
    }

    @JavascriptInterface
    fun onScrapeFinished(totalScraped: Int) {
        onScrapeCompleted(totalScraped)
    }
}
