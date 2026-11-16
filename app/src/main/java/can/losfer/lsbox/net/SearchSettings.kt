package can.losfer.lsbox.net

import can.losfer.lsbox.util.StringUtils

object SearchSettings {

    fun isExactMatch(title: String?, keyword: String?): Boolean {
        val normalized = normalize(title)
        return normalized.isNotEmpty() && normalized == normalize(keyword)
    }

    internal fun normalize(text: String?): String = StringUtils.normalizeTitle(text)
}
