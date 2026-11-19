package can.losfer.lsbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import can.losfer.lsbox.bean.AbsXml
import can.losfer.lsbox.bean.Movie
import can.losfer.lsbox.bean.MovieSort
import can.losfer.lsbox.bean.TmdbPosterCache
import can.losfer.lsbox.sourcedata.SourceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 详情页「推荐影片」的独立加载器。
 *
 * 为什么不复用 [can.losfer.lsbox.ui.page.PartitionListViewModel]：
 * 它的底层 SourceViewModel.listResult 是 replay=1 的共享流，而分类页是常驻 Tab，
 * 两个消费者会互相抢走对方的结果。这里用私有 SourceViewModel + 全程 collect
 * 的独立协程，互不干扰。
 */
class RecommendViewModel : ViewModel(), TmdbPosterCache {

    override fun tmdbCachedPoster(name: String?, year: Int, altName: String?): String? =
        can.losfer.lsbox.api.TmdbPoster.cachedPoster(name, year, altName)


    data class UiState(
        val loading: Boolean = true,
        val videos: List<Movie.Video> = emptyList(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private var sourceKey: String = ""
    private var tid: String = ""
    private var page = 1
    private var maxPage = 0

    fun load(sourceKey: String, tid: String) {
        if (this.sourceKey == sourceKey && this.tid == tid && _ui.value.videos.isNotEmpty()) return
        this.sourceKey = sourceKey
        this.tid = tid
        page = 1
        maxPage = 0
        _ui.value = UiState(loading = true)
        request(1)
    }

    fun loadMore() {
        val state = _ui.value
        if (state.loading || state.videos.isEmpty()) return
        if (maxPage > 0 && page > maxPage) return
        request(page)
    }

    private fun request(target: Int) {
        val key = sourceKey.ifBlank { return }
        val category = tid.ifBlank { return }
        viewModelScope.launch(Dispatchers.IO) {
            // 每次请求用独立的 SourceViewModel，结束后即丢弃，避免共享流互相串数据
            val svm = SourceViewModel()
            val sort = MovieSort.SortData(category, null).apply { filterSelect = HashMap() }
            val result: AbsXml? = withContext(Dispatchers.Main) {
                var received: AbsXml? = null
                val job = launch {
                    svm.listResult.flow.collect {
                        if (received == null) received = it
                    }
                }
                svm.getList(key, sort, target)
                // 给数据源一点时间回调；超时则按空结果处理
                var waited = 0
                while (received == null && waited < 8000) {
                    kotlinx.coroutines.delay(100)
                    waited += 100
                }
                job.cancel()
                received
            }
            val videos = result?.movie?.videoList.orEmpty()
            val count = result?.movie?.pagecount ?: 0
            maxPage = count
            page = target + 1
            val merged = if (target == 1) videos else _ui.value.videos + videos
            _ui.value = UiState(loading = false, videos = merged)
        }
    }
}
