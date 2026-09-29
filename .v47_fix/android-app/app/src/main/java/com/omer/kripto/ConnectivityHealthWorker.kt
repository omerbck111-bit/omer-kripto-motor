package com.omer.kripto

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Legacy health entry point kept for compatibility; V45 delegates to the shared resilience engine. */
class ConnectivityHealthWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val engine = NetworkResilienceEngine(applicationContext)
        val report = runCatching { engine.probeAll() }.getOrElse { return Result.retry() }
        val prefs = applicationContext.getSharedPreferences("okm_background", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_check", report.optLong("time", System.currentTimeMillis()))
            .putBoolean("last_ok", report.optBoolean("ok"))
            .putInt("last_code", report.optJSONArray("endpoints")?.optJSONObject(0)?.optInt("code", -1) ?: -1)
            .putString("last_network_resilience", report.toString())
            .apply()
        return if (report.optBoolean("ok")) Result.success() else Result.retry()
    }
}
