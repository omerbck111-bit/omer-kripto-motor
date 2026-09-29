package com.omer.kripto

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

/**
 * V45 multi-endpoint, read-only network resilience engine.
 * It deliberately does not change device Wi-Fi/mobile settings.
 * It measures Android validation + independent public endpoints and keeps a
 * small circuit-breaker/backoff state so one provider cannot declare the
 * whole internet unavailable.
 */
class NetworkResilienceEngine(private val context: Context) {
    data class Endpoint(val id: String, val url: String, val kind: String)

    companion object {
        val ENDPOINTS = listOf(
            Endpoint("binance", "https://api.binance.com/api/v3/ping", "exchange"),
            Endpoint("coinbase", "https://api.coinbase.com/v2/time", "exchange"),
            Endpoint("kraken", "https://api.kraken.com/0/public/SystemStatus", "exchange"),
            Endpoint("cloudflare", "https://1.1.1.1/cdn-cgi/trace", "internet")
        )
        private const val PREFS = "okm_network_resilience"
        private const val MAX_BODY = 64 * 1024
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun androidState(): JSONObject {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork
        val caps = n?.let { cm.getNetworkCapabilities(it) }
        return JSONObject().apply {
            put("connected", caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            put("validated", caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            put("wifi", caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)
            put("cellular", caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true)
            put("vpn", caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true)
            put("metered", caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true)
            put("at", System.currentTimeMillis())
        }
    }

    suspend fun probeAll(): JSONObject = withContext(Dispatchers.IO) {
        val results = JSONArray()
        var successes = 0
        ENDPOINTS.forEach { ep ->
            val r = probe(ep)
            results.put(r)
            if (r.optBoolean("ok")) successes++
        }
        val state = androidState()
        val ok = state.optBoolean("validated") && successes >= 1 || successes >= 2
        val out = JSONObject().apply {
            put("version", "46.0")
            put("time", System.currentTimeMillis())
            put("android", state)
            put("ok", ok)
            put("successes", successes)
            put("total", ENDPOINTS.size)
            put("quorum", "1-of-4-with-validated-or-2-of-4")
            put("endpoints", results)
        }
        prefs.edit().putString("last_report", out.toString()).putLong("last_at", System.currentTimeMillis()).apply()
        out
    }

    private fun probe(ep: Endpoint): JSONObject {
        val now = System.currentTimeMillis()
        val t0 = System.nanoTime()
        var code = -1
        var bytes = 0
        var error = ""
        var ok = false
        try {
            val c = (URL(ep.url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 4500
                useCaches = false
                instanceFollowRedirects = true
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "OmerKriptoNetwork/46")
            }
            code = c.responseCode
            val input = if (code in 200..399) c.inputStream else c.errorStream
            if (input != null) {
                val buf = ByteArray(8192)
                var total = 0
                while (total < MAX_BODY) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                }
                bytes = total
                input.close()
            }
            ok = code in 200..299
            c.disconnect()
        } catch (t: Throwable) {
            error = t.javaClass.simpleName + ":" + (t.message ?: "")
        }
        val latency = ((System.nanoTime() - t0) / 1_000_000L).toInt()
        val failures = if (ok) 0 else prefs.getInt("fail_${ep.id}", 0) + 1
        prefs.edit().putInt("fail_${ep.id}", if (ok) 0 else min(failures, 20)).apply()
        return JSONObject().apply {
            put("id", ep.id); put("kind", ep.kind); put("url", ep.url)
            put("ok", ok); put("code", code); put("latencyMs", latency)
            put("bytes", bytes); put("consecutiveFailures", if (ok) 0 else min(failures, 20))
            put("error", error); put("at", now)
        }
    }

    fun lastReport(): JSONObject? = runCatching {
        prefs.getString("last_report", null)?.let { JSONObject(it) }
    }.getOrNull()

    fun diagnosis(): JSONObject {
        val r = lastReport() ?: return JSONObject().apply {
            put("status", "unknown"); put("action", "probe")
        }
        val android = r.optJSONObject("android")
        val endpoints = r.optJSONArray("endpoints")
        var okCount = 0
        var slowCount = 0
        var failCount = 0
        if (endpoints != null) for (i in 0 until endpoints.length()) {
            val e = endpoints.optJSONObject(i) ?: continue
            if (e.optBoolean("ok")) { okCount++; if (e.optInt("latencyMs", 0) > 2000) slowCount++ } else failCount++
        }
        val status = when {
            okCount == 0 -> "offline_or_blocked"
            failCount >= 3 && android?.optBoolean("validated") == true -> "provider_degradation"
            slowCount >= 2 -> "degraded_latency"
            else -> "healthy"
        }
        return JSONObject().apply {
            put("status", status)
            put("validated", android?.optBoolean("validated") ?: false)
            put("okProviders", okCount); put("failedProviders", failCount); put("slowProviders", slowCount)
            put("recommendation", when(status) {
                "offline_or_blocked" -> "Android ağ durumunu ve farklı taşıyıcıları yeniden doğrula; önbellekten devam et."
                "provider_degradation" -> "Tek sağlayıcıya bağlı kalma; çalışan endpoint havuzuna geç."
                "degraded_latency" -> "İstekleri tekilleştir, timeout/backoff uygula ve cache kullan."
                else -> "Normal çalışma."
            })
        }
    }
}
