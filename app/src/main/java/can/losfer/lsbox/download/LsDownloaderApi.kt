package can.losfer.lsbox.download

import can.losfer.lsbox.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 下载任务状态 */
enum class LsTaskStatus {
    QUEUED, RUNNING, MERGING, COMPLETED, FAILED, CANCELED, UNKNOWN;

    companion object {
        fun from(raw: String?): LsTaskStatus = when (raw?.lowercase()) {
            "queued" -> QUEUED
            "running" -> RUNNING
            "merging" -> MERGING
            "completed" -> COMPLETED
            "failed" -> FAILED
            "canceled", "cancelled" -> CANCELED
            else -> UNKNOWN
        }
    }
}

data class LsTask(
    val id: String,
    val url: String,
    val title: String,
    val status: LsTaskStatus,
    val progress: Float,
    val kind: String,
    val format: String,
    val saveDir: String,
    val createdAt: String,
    val error: String?,
) {
    val finished: Boolean
        get() = status == LsTaskStatus.COMPLETED || status == LsTaskStatus.FAILED ||
                status == LsTaskStatus.CANCELED
}

data class LsStats(
    val total: Int,
    val active: Int,
    val diskFreeText: String,
)

class LsDownloaderException(message: String, val duplicate: Boolean = false) : Exception(message)

/**
 * LS 下载器 REST API 客户端。
 *
 * 文档约定：所有接口 application/json; charset=utf-8，鉴权使用
 * `Authorization: Bearer <API Key>`，写接口以返回的 ok / 任务对象为准。
 */
object LsDownloaderApi {

    private const val TAG = "LsDownloaderApi"
    private const val TIMEOUT_SECONDS = 20L

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private fun baseUrl(): String {
        val raw = LsDownloaderState.config.baseUrl.trim().trimEnd('/')
        return raw.ifBlank { "https://office.app.5ddd.com:34949" }
    }

    private fun apiKey(): String = LsDownloaderState.config.apiKey.trim()

    /** 拼出带 query token 的地址（播放/下载/SSE 用）。 */
    fun tokenUrl(path: String): String {
        val sep = if (path.contains('?')) '&' else '?'
        return baseUrl() + path + sep + "token=" + apiKey()
    }

    fun taskStreamUrl(id: String): String = tokenUrl("/api/tasks/$id/stream")

    fun taskDownloadUrl(id: String): String = tokenUrl("/api/tasks/$id/download")

    private suspend fun execute(
        method: String,
        path: String,
        body: JSONObject? = null,
    ): String = withContext(Dispatchers.IO) {
        val key = apiKey()
        if (key.isBlank()) throw LsDownloaderException("API Key 未配置")
        val builder = Request.Builder()
            .url(baseUrl() + path)
            .header("Authorization", "Bearer $key")
            .header("Accept", "application/json")
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post("{}".toRequestBody(jsonMedia))
            "DELETE" -> builder.delete()
            "PATCH" -> builder.patch("{}".toRequestBody(jsonMedia))
            else -> builder.get()
        }
        if (body != null) {
            val text = body.toString()
            when (method) {
                "POST" -> builder.post(text.toRequestBody(jsonMedia))
                "PATCH" -> builder.patch(text.toRequestBody(jsonMedia))
                "DELETE" -> builder.delete(text.toRequestBody(jsonMedia))
                else -> builder.get()
            }
        }
        val request = builder.build()
        val response = call(request)
        response.use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw parseError(resp.code, text)
            text
        }
    }

    private suspend fun call(request: Request): Response = suspendCancellableCoroutine { cont ->
        val c: Call = client.newCall(request)
        cont.invokeOnCancellation { runCatching { c.cancel() } }
        c.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response) else response.close()
            }
        })
    }

    private fun parseError(code: Int, body: String): LsDownloaderException {
        val message = try {
            val obj = JSONObject(body)
            obj.optString("error").ifBlank { "HTTP $code" }
        } catch (e: Exception) {
            "HTTP $code"
        }
        val duplicate = try {
            JSONObject(body).optBoolean("duplicate", false)
        } catch (e: Exception) {
            false
        }
        return LsDownloaderException(message, duplicate || code == 409)
    }

    private fun parseTask(obj: JSONObject): LsTask = LsTask(
        id = obj.optString("id"),
        url = obj.optString("url"),
        title = obj.optString("title").ifBlank { obj.optString("url") },
        status = LsTaskStatus.from(obj.optString("status")),
        progress = obj.optDouble("progress", 0.0).toFloat().let {
            if (it > 1f) (it / 100f).coerceIn(0f, 1f) else it.coerceIn(0f, 1f)
        },
        kind = obj.optString("kind"),
        format = obj.optString("format"),
        saveDir = obj.optString("save_dir"),
        createdAt = obj.optString("created_at"),
        error = obj.optString("error").ifBlank { null },
    )

    private fun parseTaskArray(text: String): List<LsTask> {
        if (text.isBlank()) return emptyList()
        val arr = if (text.trimStart().startsWith("[")) {
            JSONArray(text)
        } else {
            JSONObject(text).optJSONArray("tasks") ?: JSONArray()
        }
        val out = ArrayList<LsTask>(arr.length())
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out.add(parseTask(it)) }
        }
        return out
    }

    /** 提交下载任务。标题不要带扩展名，服务端按容器自动补。 */
    suspend fun createTask(
        url: String,
        title: String?,
        headers: Map<String, String>? = null,
        saveDir: String? = null,
    ): LsTask {
        val body = JSONObject().apply {
            put("url", url)
            if (!title.isNullOrBlank()) put("title", title)
            val dir = saveDir?.takeIf { it.isNotBlank() } ?: LsDownloaderState.config.saveDir
            if (!dir.isNullOrBlank()) put("save_dir", dir)
            if (!headers.isNullOrEmpty()) {
                put("headers", JSONObject().also { h -> headers.forEach { (k, v) -> h.put(k, v) } })
            }
        }
        val text = execute("POST", "/api/tasks", body)
        return parseTask(JSONObject(text))
    }

    /** 任务列表，status 可选 queued|running|merging|completed|failed|canceled|active */
    suspend fun listTasks(status: String? = null, limit: Int = 100): List<LsTask> {
        val query = buildString {
            append("?limit=").append(limit)
            if (!status.isNullOrBlank()) append("&status=").append(status)
        }
        return parseTaskArray(execute("GET", "/api/tasks$query"))
    }

    suspend fun getTask(id: String): LsTask? {
        val text = execute("GET", "/api/tasks/$id")
        if (text.isBlank()) return null
        return parseTask(JSONObject(text).let { it.optJSONObject("task") ?: it })
    }

    suspend fun renameTask(id: String, title: String) {
        execute("PATCH", "/api/tasks/$id", JSONObject().put("title", title))
    }

    suspend fun cancelTask(id: String) {
        execute("POST", "/api/tasks/$id/cancel")
    }

    suspend fun retryTask(id: String) {
        execute("POST", "/api/tasks/$id/retry")
    }

    suspend fun deleteTask(id: String, deleteFile: Boolean = true) {
        execute("DELETE", "/api/tasks/$id?file=$deleteFile")
    }

    /** 健康检查（免鉴权），用于“测试连接”。 */
    suspend fun health(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(baseUrl() + "/api/health").get().build()
        try {
            val resp = call(request)
            resp.use { it.isSuccessful }
        } catch (e: Exception) {
            AppLog.e(TAG, e)
            false
        }
    }

    /** 统计 + 磁盘剩余空间 */
    suspend fun stats(): LsStats? = withContext(Dispatchers.IO) {
        try {
            val text = execute("GET", "/api/stats")
            if (text.isBlank()) return@withContext null
            val obj = JSONObject(text)
            LsStats(
                total = obj.optInt("total", 0),
                active = obj.optInt("active", obj.optInt("running", 0)),
                diskFreeText = humanSize(obj.optLong("disk_free", obj.optLong("free", 0L))),
            )
        } catch (e: Exception) {
            AppLog.e(TAG, e)
            null
        }
    }

    private fun humanSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return String.format(java.util.Locale.US, "%.2f %s", value, units[index])
    }
}
