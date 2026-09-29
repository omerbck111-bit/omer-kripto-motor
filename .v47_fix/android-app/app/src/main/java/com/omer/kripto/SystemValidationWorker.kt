package com.omer.kripto

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Lightweight, silent background quality gate.
 * It intentionally does not run WebView/JavaScript while the app is backgrounded.
 * Instead it validates the packaged asset, records connectivity and requests a
 * foreground-cycle full JS laboratory run on the next resume.
 */
class SystemValidationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        private const val EXPECTED_HTML_SHA256 = "3c92e52edc1f164c56d1c9bb6807339838b0abca9caceea2128203cc44f9386a"
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("okm_background", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val assetOk = runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            applicationContext.assets.open("OMER_KRIPTO_MOTORU.html").use { input ->
                BufferedInputStream(input).use { stream ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = stream.read(buf)
                        if (n < 0) break
                        digest.update(buf, 0, n)
                    }
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) } == EXPECTED_HTML_SHA256
        }.getOrDefault(false)

        var networkOk = false
        var code = -1
        runCatching {
            val conn = (URL("https://api.binance.com/api/v3/ping").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                useCaches = false
            }
            code = conn.responseCode
            networkOk = code in 200..299
            conn.disconnect()
        }

        prefs.edit()
            .putLong("validation_last_check", now)
            .putBoolean("validation_asset_ok", assetOk)
            .putBoolean("validation_network_ok", networkOk)
            .putInt("validation_network_code", code)
            .putBoolean("validation_run_on_resume", true)
            .apply()

        return if (assetOk) Result.success() else Result.failure()
    }
}
