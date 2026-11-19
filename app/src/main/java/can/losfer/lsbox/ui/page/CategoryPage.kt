@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package can.losfer.lsbox.ui.page

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import can.losfer.lsbox.R
import can.losfer.lsbox.bean.Movie
import can.losfer.lsbox.bean.MovieSort
import can.losfer.lsbox.ui.WindowSize
import can.losfer.lsbox.ui.components.ProvideTmdbPosterLookup
import can.losfer.lsbox.ui.components.VodCard
import can.losfer.lsbox.ui.components.VodCardMenu
import can.losfer.lsbox.ui.components.VodCardStyle
import can.losfer.lsbox.ui.components.glassTopBarSurface
import can.losfer.lsbox.ui.components.rememberVodCardMenuState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val CategoryHeaderHeight = 52.dp

/** 「全部」在筛选 tabs 里的内部取值（空串表示不限制）。 */
private const val FilterAllValue = ""

/**
 * 分类页：顶部为一级分类（武侠 / 动作 / 喜剧…），
 * 每个筛选维度（分类 / 地区 / 年份…）各占一行可左右滚动的 tabs，首项固定为「全部」，
 * 下方按筛选结果显示影片网格。
 *
 * 顶栏右侧提供「追剧」快捷入口（navbar 之外的第二入口）。
 */
@Composable
fun CategoryPage(
    homeViewModel: HomeViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** 顶栏「追剧」快捷入口：由 MainScreen 直接切换 Tab，避免重启 Activity */
    onOpenFollowing: () -> Unit = {},
) {
    val navStart = contentPadding.calculateStartPadding(LocalLayoutDirection.current)
    val navBottom = contentPadding.calculateBottomPadding()
    // 顶栏是自绘的，需自行让出状态栏，否则标题会顶到通知栏下面
    val statusTop = with(LocalDensity.current) {
        WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp()
    }
    val context = LocalContext.current

    val sorts by homeViewModel.allSorts.collectAsState()
    val vm: PartitionListViewModel = viewModelForCategory()
    val ui by vm.ui.collectAsState()
    val vodMenu = rememberVodCardMenuState(
        lookupCollected = vm::isVideoCollected,
        onCollectToggle = vm::toggleVideoCollect,
    )

    var selectedSortId by rememberSaveable { mutableStateOf<String?>(null) }
    // 当前选中的一级分类（含其筛选条件）
    val currentSort = remember(sorts, selectedSortId) {
        sorts.firstOrNull { it.id == selectedSortId }
    }

    // 首次拿到分类列表时自动选中第一项
    LaunchedEffect(sorts) {
        if (sorts.isEmpty()) return@LaunchedEffect
        val currentId = selectedSortId
        if (currentId == null || sorts.none { it.id == currentId }) {
            val first = sorts.first()
            selectedSortId = first.id
            vm.open(first, null, first.name ?: "")
        }
    }

    LaunchedEffect(vm) {
        vm.actionMessages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏：标题 + 追剧快捷入口
        CategoryTopBar(
            topPad = statusTop,
            navStart = navStart,
            onOpenFollowing = onOpenFollowing,
        )

        if (sorts.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(
                    start = 16.dp + navStart,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sorts, key = { it.id ?: it.name.orEmpty() }) { sort ->
                    CategoryPrimaryChip(
                        label = sort.name.orEmpty(),
                        selected = sort.id == selectedSortId,
                        onClick = {
                            if (sort.id != selectedSortId) {
                                selectedSortId = sort.id
                                vm.open(sort, null, sort.name ?: "")
                            }
                        },
                    )
                }
            }
        }

        // 筛选 tabs：每个维度一行（分类 / 地区 / 年份…），首项固定「全部」，可左右滚动
        val filters = currentSort?.filters.orEmpty()
        val selection = currentSort?.filterSelect.orEmpty()
        if (filters.isNotEmpty()) {
            FilterTabRows(
                filters = filters,
                selection = selection,
                navStart = navStart,
                onSelect = { key, value ->
                    val next = HashMap(selection)
                    if (value.isBlank()) next.remove(key) else next[key] = value
                    vm.applyFilter(next)
                },
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (ui.state) {
                PartitionListViewModel.State.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator(Modifier.size(56.dp))
                }

                PartitionListViewModel.State.Empty -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.common_empty_content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                PartitionListViewModel.State.Ready -> ProvideTmdbPosterLookup(vm) {
                    CategoryGrid(
                        videos = ui.videos,
                        onsCardClick = { video ->
                            context.dispatchVodCardClick(video, onAction = { vm.runAction(it) })
                        },
                        onCardLongClick = { video -> vodMenu.show(video) },
                        onLoadMore = { vm.loadMore() },
                        navStart = navStart,
                        navBottom = navBottom,
                        resetKey = selectedSortId,
                    )
                }
            }
        }
    }

    VodCardMenu(vodMenu) { context.jumpToSearch(it) }
}

@Composable
private fun viewModelForCategory(): PartitionListViewModel {
    val owner = LocalViewModelStoreOwner.current
    return remember(owner) {
        ViewModelProvider(owner!!)[PartitionListViewModel::class.java]
    }
}

@Composable
private fun CategoryTopBar(
    topPad: Dp,
    navStart: Dp,
    onOpenFollowing: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp + navStart, end = 12.dp, top = topPad + 8.dp)
            .height(CategoryHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tab_category),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .glassTopBarSurface(CircleShape, MaterialTheme.colorScheme.surfaceBright)
                .clickable(onClick = onOpenFollowing),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tab_following),
                contentDescription = stringResource(R.string.tab_following),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun CategoryPrimaryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

/**
 * 筛选 tabs：每个筛选维度独占一行，行内横向滚动，首项固定「全部」。
 */
@Composable
private fun FilterTabRows(
    filters: List<MovieSort.SortFilter>,
    selection: Map<String, String>,
    navStart: Dp,
    onSelect: (key: String, value: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        filters.forEach { filter ->
            val key = filter.key.orEmpty()
            val values = filter.values.orEmpty()
            if (values.isEmpty()) return@forEach
            val selected = selection[key].orEmpty()
            FilterScrollRow(
                entries = buildList {
                    add(FilterAllValue to stringResource(R.string.common_all))
                    values.forEach { (value, label) ->
                        // 数据源有时会把「全部」自己加进 values，去掉避免重复
                        if (value.isNotBlank()) add(value to label)
                    }
                },
                selectedValue = selected,
                navStart = navStart,
                onSelect = { onSelect(key, it) },
            )
        }
    }
}

@Composable
private fun FilterScrollRow(
    entries: List<Pair<String, String>>,
    selectedValue: String,
    navStart: Dp,
    onSelect: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    // 让当前选中项尽量留在可视区（切换分类后取值会重置）
    LaunchedEffect(entries, selectedValue) {
        val index = entries.indexOfFirst { it.first == selectedValue }
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp + navStart, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        items(entries, key = { it.first.ifEmpty { "__all__" } }) { (value, label) ->
            FilterTab(
                label = label,
                selected = value == selectedValue,
                onClick = { if (value != selectedValue) onSelect(value) },
            )
        }
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

@Composable

private fun CategoryGrid(
    videos: List<Movie.Video>,
    onsCardClick: (Movie.Video) -> Unit,
    onCardLongClick: (Movie.Video) -> Unit,
    onLoadMore: () -> Unit,
    navStart: Dp,
    navBottom: Dp,
    resetKey: Any?,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = WindowSize.gridColumns(
            availableWidthDp = (maxWidth - 32.dp - navStart).value.toInt(),
            minColumns = 3,
        )
        val gridState = rememberLazyGridState()
        LaunchedEffect(resetKey) { gridState.scrollToItem(0) }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp + navStart,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp + navBottom,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(videos, key = { i, v -> "${i}_${v.sourceKey}_${v.id}" }) { _, video ->
                VodCard(
                    video = video,
                    onClick = { onsCardClick(video) },
                    onLongClick = { onCardLongClick(video) },
                    style = VodCardStyle.Stacked,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LaunchedEffect(videos.size) { onLoadMore() }
            }
        }

        LaunchedEffect(gridState, videos.size) {
            snapshotFlow { gridState.layoutInfo }
                .first { info ->
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@first false
                    shouldPrefetchNextPage(last, info.totalItemsCount, columns)
                }
            onLoadMore()
        }
    }
}