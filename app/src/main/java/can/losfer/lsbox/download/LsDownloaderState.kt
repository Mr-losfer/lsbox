package can.losfer.lsbox.download

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import can.losfer.lsbox.util.KV
import can.losfer.lsbox.util.KVKeys

/**
 * LS 下载器（Media Super 远程下载服务）配置。
 * 默认关闭，用户需在设置页填写接口地址与 API Key 后才可用。
 */
data class LsDownloaderConfig(
    val enabled: Boolean,
    val baseUrl: String,
    val apiKey: String,
    val saveDir: String,
)

object LsDownloaderState {

    private var current by mutableStateOf(load())

    val config: LsDownloaderConfig get() = current

    /** 配置完整且已开启时才认为可用。 */
    val ready: Boolean
        get() = current.enabled && current.baseUrl.isNotBlank() && current.apiKey.isNotBlank()

    private fun load(): LsDownloaderConfig = LsDownloaderConfig(
        enabled = KV.get(KVKeys.LS_DL_ENABLE, false),
        baseUrl = KV.get(KVKeys.LS_DL_BASE_URL, "").ifBlank { KVKeys.LS_DL_BASE_URL_DEFAULT },
        apiKey = KV.get(KVKeys.LS_DL_API_KEY, ""),
        saveDir = KV.get(KVKeys.LS_DL_SAVE_DIR, ""),
    )

    fun reload() {
        current = load()
    }

    var enabled: Boolean
        get() = current.enabled
        set(value) {
            KV.put(KVKeys.LS_DL_ENABLE, value)
            current = current.copy(enabled = value)
        }

    var baseUrl: String
        get() = current.baseUrl
        set(value) {
            val v = value.trim().trimEnd('/')
            KV.put(KVKeys.LS_DL_BASE_URL, v)
            current = current.copy(baseUrl = v)
        }

    var apiKey: String
        get() = current.apiKey
        set(value) {
            val v = value.trim()
            KV.put(KVKeys.LS_DL_API_KEY, v)
            current = current.copy(apiKey = v)
        }

    var saveDir: String
        get() = current.saveDir
        set(value) {
            val v = value.trim()
            KV.put(KVKeys.LS_DL_SAVE_DIR, v)
            current = current.copy(saveDir = v)
        }
}
