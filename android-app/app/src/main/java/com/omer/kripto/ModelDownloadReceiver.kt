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
        val pending = goAsync()
        Thread {
            try {
                val engine = LocalAiEngine(context)
                val model = context.getExternalFilesDir("models")?.let { File(it, LocalAiEngine.RECOMMENDED_MODEL) }
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                var successful = false
                var reason = ""
                dm.query(android.app.DownloadManager.Query().setFilterById(id)).use { q ->
                    if (q.moveToFirst()) {
                        val status = q.getInt(q.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                        successful = status == android.app.DownloadManager.STATUS_SUCCESSFUL
                        reason = q.getInt(q.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_REASON)).toString()
                    }
                }
                val verified = successful && model?.let { runCatching { engine.verifyRecommendedModel(it) }.getOrDefault(false) } == true
                prefs.edit()
                    .putBoolean("model_download_complete", verified)
                    .putBoolean("recommended_verified", verified)
                    .putString("model_download_error", if (verified) "" else "GGUF indirme/doğrulama başarısız. reason=$reason")
                    .apply()
                engine.close()
            } finally {
                pending.finish()
            }
        }.start()
    }
}
