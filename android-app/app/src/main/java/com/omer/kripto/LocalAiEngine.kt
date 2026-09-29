package com.omer.kripto

import android.content.Context
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Real on-device GGUF inference boundary backed by a prebuilt llama.cpp Android AAR. */
class LocalAiEngine(private val context: Context) {
    data class Status(
        val runtime: Boolean,
        val model: Boolean,
        val verified: Boolean,
        val modelPath: String?,
        val modelBytes: Long,
        val backend: String,
        val abi: String,
        val error: String? = null
    )

    companion object {
        const val RECOMMENDED_MODEL = "Qwen3-4B-Q4_K_M.gguf"
        const val RECOMMENDED_SHA256 = "7485fe6f11af29433bc51cab58009521f205840f5b4ae3a32fa7f92e8534fdf5"
    }

    private val modelDir = File(context.filesDir, "models").apply { mkdirs() }
    private val inferenceLock = Any()
    private var runtimeError: String? = null

    fun modelDir(): File = modelDir

    fun findModel(): File? {
        val files = listOfNotNull(modelDir, context.getExternalFilesDir("models"))
            .flatMap { it.listFiles()?.toList() ?: emptyList() }
            .filter { it.isFile && it.length() > 0L && it.name.lowercase(Locale.US).endsWith(".gguf") }
        return files.firstOrNull { it.name.equals(RECOMMENDED_MODEL, ignoreCase = true) }
            ?: files.maxByOrNull { it.length() }
    }

    private fun runtimeReady(): Boolean {
        // The app bundles the published llama-android AAR. The public API documents
        // loadModel/complete/releaseModel, but not getSystemInfo; runtime readiness
        // is therefore established by the packaged API and the real inference call.
        return try {
            Class.forName("dev.ffmpegkit.llama.Llama")
            runtimeError = null
            true
        } catch (t: Throwable) {
            runtimeError = t.javaClass.simpleName + ": " + (t.message ?: "llama.cpp runtime yüklenemedi")
            false
        }
    }

    fun verifyRecommendedModel(file: File): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        if (!file.name.equals(RECOMMENDED_MODEL, ignoreCase = true)) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        val ok = actual.equals(RECOMMENDED_SHA256, ignoreCase = true)
        context.getSharedPreferences("okm_ai", Context.MODE_PRIVATE).edit()
            .putBoolean("recommended_verified", ok)
            .putString("recommended_sha256", actual)
            .apply()
        return ok
    }

    fun status(): Status {
        val m = findModel()
        val runtime = runtimeReady()
        val verified = m?.name.equals(RECOMMENDED_MODEL, ignoreCase = true) &&
            context.getSharedPreferences("okm_ai", Context.MODE_PRIVATE)
                .getBoolean("recommended_verified", false)
        return Status(
            runtime = runtime,
            model = m != null,
            verified = verified,
            modelPath = m?.absolutePath,
            modelBytes = m?.length() ?: 0L,
            backend = if (runtime) "llama.cpp AAR / CPU-NEON" else "yüklenemedi",
            abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            error = runtimeError
        )
    }

    private fun loadAndComplete(modelFile: File, prompt: String, systemPrompt: String, maxTokens: Int): String {
        val threads = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(2, 6)
        val config = LlamaConfig(contextSize = 4096, threads = threads, gpuLayers = 0)
        val model: LlamaModel = runBlocking { Llama.loadModel(modelFile.absolutePath, config) }
        return try {
            runBlocking {
                Llama.complete(model, prompt, systemPrompt, maxTokens.coerceIn(16, 4096)).text
            }
        } finally {
            Llama.releaseModel(model)
        }
    }

    fun generate(prompt: String, maxTokens: Int = 512): String {
        val m = findModel() ?: return "LOCAL_AI_MODEL_MISSING"
        if (!runtimeReady()) return "LOCAL_AI_RUNTIME_MISSING"
        return synchronized(inferenceLock) {
            try {
                loadAndComplete(
                    m,
                    prompt.take(120_000),
                    "Sen Ömer Kripto Motoru'nun cihaz içi yardımcı AI'sısın. Verilen piyasa/uygulama bağlamına sadık kal. Sayısal verileri uydurma. Finansal sonucu garanti etme.",
                    maxTokens
                )
            } catch (t: Throwable) {
                runtimeError = t.javaClass.simpleName + ": " + (t.message ?: "native inference hatası")
                "LOCAL_AI_INFERENCE_ERROR"
            }
        }
    }

    fun generateStructured(prompt: String, maxTokens: Int = 1200): String {
        val m = findModel() ?: return "LOCAL_AI_MODEL_MISSING"
        if (!runtimeReady()) return "LOCAL_AI_RUNTIME_MISSING"
        return synchronized(inferenceLock) {
            try {
                loadAndComplete(
                    m,
                    prompt.take(160_000),
                    "Yalnız geçerli JSON döndür. Markdown çiti, açıklama veya ek metin kullanma. İstenen araç/patch sözleşmesine uy. Kullanıcı kodunu çalıştırma.",
                    maxTokens
                )
            } catch (t: Throwable) {
                runtimeError = t.javaClass.simpleName + ": " + (t.message ?: "structured inference hatası")
                "LOCAL_AI_INFERENCE_ERROR"
            }
        }
    }
}
