package can.losfer.lsbox.bean

interface TmdbPosterCache {

    fun tmdbCachedPoster(name: String?, year: Int, altName: String?): String?
}
