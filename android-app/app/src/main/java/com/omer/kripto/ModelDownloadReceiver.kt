package com.omer.kripto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

class ModelDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val prefs = context.getSharedPreferences("okm_ai", Context.MODE_PRIVATE)
        if (id <= 0L || prefs.getLong("model_download_id", -2L) != id) return
        val engine = LocalAiEngine(context)
        val model = context.getExternalFilesDir("models")?.let { File(it, LocalAiEngine.RECOMMENDED_MODEL) }
        val verified = model?.let { runCatching { engine.verifyRecommendedModel(it) }.getOrDefault(false) } ?: false
        prefs.edit()
            .putBoolean("model_download_complete", true)
            .putBoolean("recommended_verified", verified)
            .putString("model_download_error", if (verified) "" else "İndirilen GGUF SHA-256 doğrulaması başarısız.")
            .apply()
    }
}
