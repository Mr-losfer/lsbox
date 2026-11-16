package can.losfer.lsbox.market

import can.losfer.lsbox.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 订阅站点类型 */
enum class MarketSiteType(val key: String) {
    Vod("vod"),
    Live("live");

    companion object {
        fun of(raw: String?): MarketSiteType? = entries.firstOrNull {
            it.key.equals(raw?.trim(), ignoreCase = true)
        }
    }
}

/** 市场中的一个订阅站点 */
data class MarketSite(
    val name: String,
    val type: MarketSiteType,
    val url: String,
    val description: String = "",
    val recommend: Boolean = false,
)

/**
 * 订阅市场数据源。
 *
 * 数据来自本仓库 market/sites.json；拉取时优先走 gh-proxy.com 代理，失败自动直连。
 */
object MarketRepository {

    const val OWNER = "Mr-losfer"
    const val REPO = "lsbox"
    const val BRANCH = "main"

    /** 仓库内市场清单路径 */
    private const val SITES_PATH = "market/sites.json"

    private const val RAW_BASE = "https://raw.githubusercontent.com/$OWNER/$REPO/$BRANCH"
    private const val GH_PROXY = "https://gh-proxy.com/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun sitesUrl(): String = "$RAW_BASE/$SITES_PATH"

    /** 转换为代理地址 */
    fun proxied(url: String): String =
        if (url.startsWith(GH_PROXY)) url else GH_PROXY + url

    /** 直接地址（去掉代理前缀） */
    fun direct(url: String): String =
        if (url.startsWith(GH_PROXY)) url.removePrefix(GH_PROXY) else url

    /**
     * 拉取市场站点列表。
     * @return 解析成功的站点列表（可能为空）
     */
    suspend fun loadSites(): Result<List<MarketSite>> = withContext(Dispatchers.IO) {
        try {
            val body = fetchWithFallback(sitesUrl())
            val sites = parseSites(body)
            if (sites.isEmpty()) {
                Result.failure(IOException("empty market list"))
            } else {
                Result.success(sites)
            }
        } catch (e: Exception) {
            AppLog.d("Market", "load sites failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** 代理优先、直连降级的文本拉取 */
    suspend fun fetchWithFallback(url: String): String = withContext(Dispatchers.IO) {
        val candidates = listOf(proxied(direct(url)), direct(url))
        var lastError: Exception? = null
        for (candidate in candidates) {
            try {
                val req = Request.Builder()
                    .url(candidate)
                    .header("User-Agent", "LSBox-Market")
                    .header("Accept", "application/json, */*")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} @ $candidate")
                    val text = resp.body.string()
                    if (text.isBlank()) throw IOException("blank body @ $candidate")
                    return@withContext text
                }
            } catch (e: Exception) {
                AppLog.d("Market", "fetch failed @ $candidate: ${e.message}")
                lastError = e
            }
        }
        throw lastError ?: IOException("all endpoints failed")
    }

    private fun parseSites(json: String): List<MarketSite> {
        val root = JSONObject(json)
        val array = root.optJSONArray("sites") ?: return emptyList()
        val result = ArrayList<MarketSite>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name").trim()
            val url = obj.optString("url").trim()
            val type = MarketSiteType.of(obj.optString("type")) ?: continue
            if (name.isEmpty() || url.isEmpty()) continue
            result.add(
                MarketSite(
                    name = name,
                    type = type,
                    url = url,
                    description = obj.optString("description", ""),
                    recommend = obj.optBoolean("recommend", false),
                ),
            )
        }
        return result
    }
}
