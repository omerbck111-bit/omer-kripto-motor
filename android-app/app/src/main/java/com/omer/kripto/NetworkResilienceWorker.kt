package com.omer.kripto

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NetworkResilienceWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val engine = NetworkResilienceEngine(applicationContext)
        val report = engine.probeAll()
        val prefs = applicationContext.getSharedPreferences("okm_background", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("network_resilience_at", report.optLong("time", System.currentTimeMillis()))
            .putString("network_resilience_report", report.toString())
            .putString("network_resilience_diagnosis", engine.diagnosis().toString())
            .apply()
        return if (report.optBoolean("ok")) Result.success() else Result.retry()
    }
}
