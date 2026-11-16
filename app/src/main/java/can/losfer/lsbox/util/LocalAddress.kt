package can.losfer.lsbox.util

object LocalAddress {
    @JvmStatic
    var provider: (() -> String?)? = null

    @JvmStatic
    fun get(): String = provider?.invoke() ?: ""
}
