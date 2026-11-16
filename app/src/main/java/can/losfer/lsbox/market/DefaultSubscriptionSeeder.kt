package can.losfer.lsbox.market

import can.losfer.lsbox.api.AppBootstrap
import can.losfer.lsbox.ui.page.SUBSCRIBE_SPLIT
import can.losfer.lsbox.ui.page.parseSubscribe
import can.losfer.lsbox.util.AppLog
import can.losfer.lsbox.util.KV
import can.losfer.lsbox.util.KVKeys

/**
 * 首次启动时把市场列表里"第一个最上方的站点"写入订阅并设为当前订阅。
 *
 * 只在没有任何点播订阅时执行一次，避免覆盖用户已有配置。
 */
object DefaultSubscriptionSeeder {

    private const val KEY_SEEDED = "market_default_seeded"

    suspend fun seedIfNeeded() {
        if (KV.get(KEY_SEEDED, false)) return
        val existing = KV.get(KVKeys.SUBSCRIBE_LIST, ArrayList<String>())
        if (existing.isNotEmpty()) {
            KV.put(KEY_SEEDED, true)
            return
        }
        val sites = MarketRepository.loadSites().getOrNull() ?: run {
            AppLog.d("Market", "seed skipped: market unavailable")
            return
        }
        // 第一个最上方的站点（优先点播）
        val first = sites.firstOrNull { it.type == MarketSiteType.Vod } ?: sites.firstOrNull() ?: return
        val value = first.name + SUBSCRIBE_SPLIT + first.url
        KV.put(KVKeys.SUBSCRIBE_LIST, arrayListOf(value))
        if (first.type == MarketSiteType.Vod) {
            AppBootstrap.switchVodSubscription(first.url)
        }
        KV.put(KVKeys.API_URL, first.url)
        KV.put(KEY_SEEDED, true)
        AppLog.d("Market", "default subscription seeded: ${first.name}")

        // 若有直播站点，也加进直播订阅但不抢占当前
        val live = sites.firstOrNull { it.type == MarketSiteType.Live }
        if (live != null) {
            val liveList = KV.get(KVKeys.LIVE_SUBSCRIBE_LIST, ArrayList<String>())
            if (liveList.isEmpty()) {
                KV.put(KVKeys.LIVE_SUBSCRIBE_LIST, arrayListOf(live.name + SUBSCRIBE_SPLIT + live.url))
            }
        }
    }
}
