package can.losfer.lsbox.api

/**
 * TMDB 标题语言判定。
 *
 * 维吾尔语影片（标题为维吾尔文，且没有中文/英文标题）不进入 TMDB 查询：
 * 这类名称在 TMDB 上没有对应条目，请求必然是空命中，还会反复触发网络请求与异常。
 */
object TmdbTitleFilter {

    /** 阿拉伯字母区段：U+0600–U+06FF（含维吾尔文、阿拉伯文）。 */
    private fun codeInArabicBlock(code: Int): Boolean = code in 0x0600..0x06FF

    /** 拉丁扩展与基本拉丁。 */
    private fun codeInLatin(code: Int): Boolean =
        code in 'a'.code..'z'.code || code in 'A'.code..'Z'.code ||
            code in 0x00C0..0x024F

    /** CJK 统一表意文字。 */
    private fun codeInCjk(code: Int): Boolean = code in 0x4E00..0x9FFF

    /** 是否包含维吾尔文/阿拉伯字母。 */
    fun hasUyghurScript(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return text.any { codeInArabicBlock(it.code) }
    }

    /** 是否包含中文或拉丁字母（说明可能是其他语言的标题）。 */
    fun hasLatOrCjk(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return text.any { codeInLatin(it.code) || codeInCjk(it.code) }
    }

    /**
     * 是否为纯维吾尔语标题：含维文但除维文外没有中文/拉丁字母。
     * 例如「ياخشى كۈنلەر」返回 true；「三体 (ياخشى)」返回 false。
     */
    fun isUyghurOnlyTitle(text: String?): Boolean = hasUyghurScript(text) && !hasLatOrCjk(text)

    /**
     * 是否应跳过 TMDB 查询。
     *
     * 只要主标题或副标题是纯维吾尔语（且没有可用的中文/英文标题）就跳过；
     * 副标题若存在中文/英文标题，仍允许查询。
     */
    fun shouldSkipTmdb(title: String?, altTitle: String? = null): Boolean {
        if (title.isNullOrBlank()) return true
        val primaryUyghurOnly = isUyghurOnlyTitle(title)
        if (!primaryUyghurOnly) return false
        // 主标题是纯维语：只有当副标题提供了中文/英文标题时才继续
        val alt = altTitle?.trim().orEmpty()
        return alt.isEmpty() || isUyghurOnlyTitle(alt)
    }
}
