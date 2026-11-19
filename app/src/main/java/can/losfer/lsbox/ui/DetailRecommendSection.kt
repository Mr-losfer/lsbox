@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package can.losfer.lsbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import can.losfer.lsbox.R
import can.losfer.lsbox.ui.components.ProvideTmdbPosterLookup
import can.losfer.lsbox.ui.components.VodCard
import can.losfer.lsbox.ui.components.VodCardStyle
import can.losfer.lsbox.ui.page.jumpToDetail

/**
 * 详情页底部的「推荐影片」区块。
 *
 * 取当前影片的一级分类（tid）作为筛选条件再拉一页列表，滤掉影片自身后网格展示。
 * 属于可选增强：拿不到分类或加载失败时整块不渲染，不影响详情页本身。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailRecommendSection(vm: DetailViewModel) {
    val category by vm.recommendCategory.collectAsState()
    val current = category ?: return

    val recVm: RecommendViewModel = viewModelForRecommend()
    val ui by recVm.ui.collectAsState()
    val context = LocalContext.current
    val excludeId = vm.vodId

    val sectionTitle = stringResource(R.string.detail_recommend)
    LaunchedEffect(current, excludeId) {
        val (tid, sourceKey) = current
        recVm.load(sourceKey, tid.toString())
    }

    val videos = remember(ui.videos, excludeId) {
        ui.videos.filter { it.id != excludeId }
    }
    if (!ui.loading && videos.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        ) {
            SectionTitleIcon(painterResource(R.drawable.ic_detail_recommend))
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.weight(1f))
        }

        when {
            ui.loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator(Modifier.size(44.dp))
            }

            else -> {
                val gridState = rememberLazyGridState()
                // 首页只展示 2 行，避免详情页被推荐内容冲淡主体信息
                val shown = videos.take(RecommendMaxItems)
                ProvideTmdbPosterLookup(recVm) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(RecommendColumns),
                        state = gridState,
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(
                            shown,
                            key = { i, v -> "rec_${i}_${v.sourceKey}_${v.id}" },
                        ) { _, video ->
                            VodCard(
                                video = video,
                                onClick = {
                                    context.jumpToDetail(
                                        video.id,
                                        video.sourceKey,
                                        video.name,
                                        video.pic,
                                    )
                                },
                                onLongClick = {},
                                style = VodCardStyle.Stacked,
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            // 一页通常够 9 条；不足时自动补下一页，够则不再请求
                            LaunchedEffect(shown.size) {
                                if (shown.size < RecommendMaxItems) recVm.loadMore()
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val RecommendColumns = 3
private const val RecommendMaxItems = 9

@Composable
private fun viewModelForRecommend(): RecommendViewModel {
    // 独立 key，避免与其他 ViewModel 共用实例
    return androidx.lifecycle.viewmodel.compose.viewModel(key = "detail_recommend")
}
