package can.losfer.lsbox.data

import can.losfer.lsbox.util.KVKeys
import can.losfer.lsbox.util.HistoryHelper
import can.losfer.lsbox.util.KV

internal object CurrentSubscription {

    fun cid(): String {
        val apiUrl = KV.get(KVKeys.API_URL, "")
        val lineSource = KV.get(KVKeys.API_LINE_SOURCE, "")
        if (lineSource != null && lineSource.isNotEmpty() && HistoryHelper.isApiLineUrl(apiUrl)) {
            return lineSource
        }
        return apiUrl
    }
}
