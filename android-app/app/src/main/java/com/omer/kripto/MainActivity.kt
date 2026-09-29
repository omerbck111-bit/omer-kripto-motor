package com.omer.kripto

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.content.Intent
import android.app.DownloadManager
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var connectivity: ConnectivityManager
    private var lastResumeRefresh = 0L
    private lateinit var localAi: LocalAiEngine
    private lateinit var networkResilience: NetworkResilienceEngine
    private val pickModelRequest = 4417

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { pushNetworkState(true); probeNetworkSoon("available") }
        override fun onLost(network: Network) { pushNetworkState(false) }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            pushNetworkState(
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            )
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) probeNetworkSoon("validated")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localAi = LocalAiEngine(this)
        networkResilience = NetworkResilienceEngine(this)
        webView = WebView(this)
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                val trusted = u.scheme == "https" && u.host == "appassets.androidplatform.net"
                if (trusted) return false
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, u)) }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (!url.startsWith("https://appassets.androidplatform.net/assets/")) {
                    view.loadUrl("https://appassets.androidplatform.net/assets/OMER_KRIPTO_MOTORU.html")
                    return
                }
                pushNetworkState(currentNetworkValidated())
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.setBackgroundColor(0xFF070D16)
        webView.isVerticalScrollBarEnabled = false
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.isHorizontalScrollBarEnabled = false
        webView.isNestedScrollingEnabled = true
        ViewCompat.setOnApplyWindowInsetsListener(webView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            userAgentString = userAgentString + " OmerKriptoAndroid/1.0"
        }

        webView.addJavascriptInterface(AndroidNetworkBridge(this), "AndroidNetwork")
        webView.addJavascriptInterface(LocalAiBridge(this), "AndroidLocalAI")

        webView.loadUrl("https://appassets.androidplatform.net/assets/OMER_KRIPTO_MOTORU.html")

        connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivity.registerDefaultNetworkCallback(networkCallback)
        pushNetworkState(currentNetworkValidated())
        scheduleSilentHealthWork()
        scheduleSystemValidationWork()
        scheduleNetworkResilienceWork()
        probeNetworkSoon("startup")
    }

    private fun scheduleSystemValidationWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SystemValidationWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "okm_system_validation", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    override fun onResume() {
        super.onResume()
        val now = System.currentTimeMillis()
        if (now - lastResumeRefresh > 15000) {
            lastResumeRefresh = now
            webView.postDelayed({
                webView.evaluateJavascript(
                    "window.__okmRefreshSoon && window.__okmRefreshSoon('resume'); window.OKM_RUN_QUALITY_LAB && window.OKM_RUN_QUALITY_LAB();", null
                )
            }, 250)
        }
    }

    private fun scheduleSilentHealthWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<ConnectivityHealthWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "okm_connectivity_health",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleNetworkResilienceWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<NetworkResilienceWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "okm_network_resilience", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    private fun probeNetworkSoon(reason: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val report = runCatching { networkResilience.probeAll() }.getOrNull() ?: return@launch
            val diagnosis = networkResilience.diagnosis()
            runOnUiThread {
                val jsReport = JSONObject.quote(report.toString())
                val jsDiagnosis = JSONObject.quote(diagnosis.toString())
                webView.evaluateJavascript(
                    "window.__androidNetworkReport && window.__androidNetworkReport(JSON.parse($jsReport)); window.__androidNetworkDiagnosis && window.__androidNetworkDiagnosis(JSON.parse($jsDiagnosis));", null
                )
            }
        }
    }

    private fun currentNetworkValidated(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun pushNetworkState(online: Boolean) {
        runOnUiThread {
            webView.evaluateJavascript(
                "window.__androidNetworkState && window.__androidNetworkState(${online});", null
            )
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickModelRequest || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        Thread {
            try {
                val sourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "selected-model.gguf"
                val safeName = if (sourceName.lowercase().endsWith(".gguf")) sourceName.replace(Regex("[^A-Za-z0-9._-]"), "_") else "selected-model.gguf"
                val target = File(localAi.modelDir(), safeName)
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output, 1024 * 1024) }
                }
                val verified = runCatching { localAi.verifyRecommendedModel(target) }.getOrDefault(false)
                runOnUiThread {
                    val js = "window.__localAIModelChanged && window.__localAIModelChanged(" + verified + ");"
                    webView.evaluateJavascript(js, null)
                }
            } catch (t: Throwable) {
                val msg = JSONObject.quote(t.message ?: "Model kopyalanamadı")
                runOnUiThread { webView.evaluateJavascript("window.__localAIModelError && window.__localAIModelError($msg);", null) }
            }
        }.start()
    }

    private fun openModelPicker() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        startActivityForResult(i, pickModelRequest)
    }

    override fun onDestroy() {
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        runCatching { webView.removeJavascriptInterface("AndroidNetwork") }
        runCatching { webView.removeJavascriptInterface("AndroidLocalAI") }
        runCatching { webView.stopLoading() }
        webView.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }


    inner class LocalAiBridge(private val context: Context) {
        @JavascriptInterface
        fun status(): String {
            val s = localAi.status()
            return JSONObject().apply {
                put("runtime", s.runtime)
                put("model", s.model)
                put("verified", s.verified)
                put("modelPath", s.modelPath ?: JSONObject.NULL)
                put("modelBytes", s.modelBytes)
                put("backend", s.backend)
                put("abi", s.abi)
                put("error", s.error ?: JSONObject.NULL)
            }.toString()
        }

        @JavascriptInterface
        fun pickModel() {
            runOnUiThread { openModelPicker() }
        }

        @JavascriptInterface
        fun downloadRecommendedModel(): String {
            return try {
                val url = "https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q4_K_M.gguf?download=true"
                val dir = localAi.modelDir()
                dir.mkdirs()
                val file = File(dir, LocalAiEngine.RECOMMENDED_MODEL)
                if (file.exists()) file.delete()
                getExternalFilesDir("models")?.let { File(it, LocalAiEngine.RECOMMENDED_MODEL).delete() }
                val req = DownloadManager.Request(Uri.parse(url))
                    .setTitle("Ömer AI — Qwen3 4B Q4_K_M")
                    .setDescription("Yerel AI modeli indiriliyor")
                    .setMimeType("application/octet-stream")
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationInExternalFilesDir(this@MainActivity, "models", LocalAiEngine.RECOMMENDED_MODEL)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val id = dm.enqueue(req)
                getSharedPreferences("okm_ai", MODE_PRIVATE).edit().putLong("model_download_id", id).apply()
                "STARTED:$id"
            } catch (t: Throwable) {
                "DOWNLOAD_ERROR:" + (t.message ?: "bilinmeyen hata")
            }
        }

        @JavascriptInterface
        fun generate(prompt: String, maxTokens: Int): String = localAi.generate(prompt, maxTokens)

        @JavascriptInterface
        fun generateStructured(prompt: String, maxTokens: Int): String = localAi.generateStructured(prompt, maxTokens)

        @JavascriptInterface
        fun verifyRecommendedModel(): Boolean {
            val m = localAi.findModel() ?: return false
            return runCatching { localAi.verifyRecommendedModel(m) }.getOrDefault(false)
        }
    }

    inner class AndroidNetworkBridge(private val context: Context) {
        @JavascriptInterface
        fun backgroundHealth(): String {
            val p = context.getSharedPreferences("okm_background", Context.MODE_PRIVATE)
            return "{\"checkedAt\":${p.getLong("last_check",0L)},\"ok\":${p.getBoolean("last_ok",false)},\"code\":${p.getInt("last_code",-1)}}"
        }

        @JavascriptInterface
        fun resilienceReport(): String {
            return networkResilience.lastReport()?.toString() ?: "{}"
        }

        @JavascriptInterface
        fun resilienceDiagnosis(): String {
            return networkResilience.diagnosis().toString()
        }

        @JavascriptInterface
        fun probeNow(): String {
            Thread { probeNetworkSoon("js") }.start()
            return "STARTED"
        }

        @JavascriptInterface
        fun httpGet(url: String, timeoutMs: Int): String {
            return runCatching {
                val u = URL(url)
                val host = u.host.lowercase()
                val allowed = host == "api.binance.com" || host == "api1.binance.com" || host == "api2.binance.com" || host == "api3.binance.com" || host == "api.coinbase.com" || host == "api.kraken.com"
                if (u.protocol != "https" || !allowed) return "{\"ok\":false,\"status\":0,\"body\":\"\",\"error\":\"HOST_NOT_ALLOWED\"}"
                val c = (u.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = timeoutMs.coerceIn(1500, 8000)
                    readTimeout = timeoutMs.coerceIn(2000, 9000)
                    useCaches = false
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("User-Agent", "OmerKriptoAndroid/47")
                }
                val code = c.responseCode
                val stream = if (code in 200..399) c.inputStream else c.errorStream
                val body = stream?.bufferedReader()?.use { it.readText().take(1_500_000) } ?: ""
                c.disconnect()
                JSONObject().apply { put("ok", code in 200..299); put("status", code); put("body", body) }.toString()
            }.getOrElse { JSONObject().apply { put("ok", false); put("status", 0); put("body", ""); put("error", it.message ?: "network_error") }.toString() }
        }

        @JavascriptInterface
        fun webSearch(query: String): String {
            return runCatching {
                val q = java.net.URLEncoder.encode(query.take(300), "UTF-8")
                val url = "https://html.duckduckgo.com/html/?q=$q&kl=tr-tr"
                val c = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 5000; readTimeout = 8000; useCaches = false
                    setRequestProperty("User-Agent", "Mozilla/5.0 OmerKriptoAndroid/47")
                }
                val code = c.responseCode
                val body = (if (code in 200..399) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText().take(500_000) } ?: ""
                c.disconnect()
                JSONObject().apply { put("ok", code in 200..299); put("status", code); put("body", body); put("query", query) }.toString()
            }.getOrElse { "ERROR:" + (it.message ?: "web_search_error") }
        }

        @JavascriptInterface
        fun webRead(url: String): String {
            return runCatching {
                val u = URL(url)
                val host = u.host.lowercase()
                val allowed = host == "api.binance.com" || host == "api1.binance.com" || host == "api2.binance.com" || host == "api3.binance.com" || host == "api.coinbase.com" || host == "api.kraken.com" || host == "1.1.1.1" || host.endsWith(".wikipedia.org") || host == "www.google.com" || host == "html.duckduckgo.com"
                if (u.protocol != "https" || !allowed) return "ERROR:HOST_NOT_ALLOWED"
                val c = (u.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 5000; readTimeout = 7000; useCaches = false
                    setRequestProperty("User-Agent", "OmerKriptoAI/47")
                }
                val code = c.responseCode
                val stream = if (code in 200..399) c.inputStream else c.errorStream
                val text = stream?.bufferedReader()?.use { it.readText().take(120_000) } ?: ""
                c.disconnect()
                JSONObject().apply { put("ok", code in 200..299); put("status", code); put("url", url); put("body", text) }.toString()
            }.getOrElse { "ERROR:" + (it.message ?: "network_error") }
        }

        @JavascriptInterface
        fun isOnline(): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return false
            val c = cm.getNetworkCapabilities(n) ?: return false
            return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
}
