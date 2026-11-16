package can.losfer.lsbox.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import can.losfer.lsbox.BuildConfig
import can.losfer.lsbox.util.AppLog
import can.losfer.lsbox.util.KV
import can.losfer.lsbox.util.KVKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ReleaseInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSize: Long,
    val publishedAt: String,
)

sealed class UpdateResult {
    data class Available(val release: ReleaseInfo) : UpdateResult()
    data object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateManager {

    const val OWNER = "Mr-losfer"
    const val REPO = "lsbox"

    private const val GH_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    private const val GH_PROXY = "https://gh-proxy.com/"

    private const val KEY_SKIP_VERSION = KVKeys.UPDATE_SKIP_VERSION
    private const val KEY_LAST_CHECK = KVKeys.UPDATE_LAST_CHECK
    private const val KEY_DOWNLOAD_ID = KVKeys.UPDATE_DOWNLOAD_ID

    private val versionRegex = Regex("""^\D*(\d+(?:\.\d+){0,3})""")

    fun extractVersion(tag: String): String? =
        versionRegex.find(tag.trim())?.groupValues?.get(1)

    fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split('.').mapNotNull { it.toIntOrNull() }
        val l = local.split('.').mapNotNull { it.toIntOrNull() }
        if (r.isEmpty()) return false
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    fun proxied(url: String): String = GH_PROXY + url

    fun directOf(url: String): String =
        if (url.startsWith(GH_PROXY)) url.removePrefix(GH_PROXY) else url

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val body = fetchWithFallback(GH_API)
            val release = parseRelease(body) ?: return@withContext UpdateResult.Error("parse_failed")
            val remote = extractVersion(release.versionName)
            val local = BuildConfig.VERSION_NAME
            KV.put(KVKeys.UPDATE_LAST_CHECK, System.currentTimeMillis())
            if (remote == null || !isNewer(remote, local)) {
                UpdateResult.UpToDate
            } else {
                UpdateResult.Available(release)
            }
        } catch (e: Exception) {
            AppLog.d("Update", "update check failed: ${e.message}")
            UpdateResult.Error(e.message ?: "unknown")
        }
    }

    fun isSkipped(version: String): Boolean = KV.get(KEY_SKIP_VERSION, "") == version

    fun skipVersion(version: String) {
        KV.put(KEY_SKIP_VERSION, version)
    }

    fun lastCheckMs(): Long = KV.get(KEY_LAST_CHECK, 0L)

    suspend fun fetchWithFallback(url: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val candidates = listOf(proxied(url), directOf(url))
        var lastError: Exception? = null
        for (candidate in candidates) {
            try {
                val req = Request.Builder().url(candidate)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "LSBox/${BuildConfig.VERSION_NAME}")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} @ $candidate")
                    return resp.body.string()
                }
            } catch (e: Exception) {
                AppLog.d("Update", "update fetch failed @ $candidate: ${e.message}")
                lastError = e
            }
        }
        throw lastError ?: IOException("all endpoints failed")
    }

    private fun parseRelease(json: String): ReleaseInfo? {
        return try {
            val obj = org.json.JSONObject(json)
            val tag = obj.optString("tag_name")
            if (tag.isEmpty()) return null
            val notes = obj.optString("body", "")
            val published = obj.optString("published_at", "")
            val assets = obj.optJSONArray("assets") ?: return null
            var apkUrl = ""
            var apkSize = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.optString("browser_download_url", "")
                    apkSize = a.optLong("size", 0L)
                    break
                }
            }
            if (apkUrl.isEmpty()) return null
            ReleaseInfo(tag, notes, apkUrl, apkSize, published)
        } catch (e: Exception) {
            AppLog.d("Update", "update parse failed: ${e.message}")
            null
        }
    }

    fun downloadAndInstall(context: Context, release: ReleaseInfo, onProgress: (Int) -> Unit = {}, onDone: (Boolean) -> Unit) {
        val target = File(context.getExternalFilesDir(null) ?: context.filesDir, "update")
        if (!target.exists()) target.mkdirs()
        val file = File(target, "LSBox_${release.versionName}.apk")
        if (file.exists()) file.delete()

        val directUrl = directOf(release.apkUrl)
        val proxyUrl = if (release.apkUrl.startsWith(GH_PROXY)) release.apkUrl else proxied(release.apkUrl)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var request = DownloadManager.Request(Uri.parse(proxyUrl))
            .setTitle("LSBox ${release.versionName}")
            .setDestinationUri(Uri.fromFile(file))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val id = try {
            dm.enqueue(request)
        } catch (e: Exception) {
            AppLog.d("Update", "update download via proxy failed: ${e.message}, fallback direct")
            request = DownloadManager.Request(Uri.parse(directUrl))
                .setTitle("LSBox ${release.versionName}")
                .setDestinationUri(Uri.fromFile(file))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            dm.enqueue(request)
        }
        KV.put(KEY_DOWNLOAD_ID, id)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val receivedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (receivedId != id) return
                runCatching { context.unregisterReceiver(this) }
                val query = DownloadManager.Query().setFilterById(id)
                dm.query(query).use { c ->
                    if (c.moveToFirst()) {
                        val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            installApk(context, file)
                            onDone(true)
                        } else {
                            onDone(false)
                        }
                    } else {
                        onDone(false)
                    }
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        Thread {
            var lastProgress = -1
            while (true) {
                Thread.sleep(800)
                val query = DownloadManager.Query().setFilterById(id)
                dm.query(query).use { c ->
                    if (!c.moveToFirst()) return@use
                    val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    if (total > 0) {
                        val p = (done * 100 / total).toInt()
                        if (p != lastProgress) {
                            lastProgress = p
                            onProgress(p)
                        }
                    }
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) return@Thread
                }
            }
        }.start()
    }

    private fun installApk(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }
}
