package can.losfer.lsbox.player.host

interface TextureRenderHost {

    fun setOnSurfaceReadyListener(listener: Runnable?)

    fun setOutputSize(width: Int, height: Int)
}
