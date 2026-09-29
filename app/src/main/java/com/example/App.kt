package com.example

import android.app.Application
import android.util.Log
import java.io.File

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Pre-create Chromium WebView code cache subdirectories to prevent disk index reconstruction errors
            val codeCacheDir = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            File(codeCacheDir, "js").mkdirs()
            File(codeCacheDir, "wasm").mkdirs()
            File(cacheDir, "WebView/Default").mkdirs()
        } catch (e: Exception) {
            Log.w("App", "WebView cache dir preparation note: ${e.message}")
        }
    }
}
