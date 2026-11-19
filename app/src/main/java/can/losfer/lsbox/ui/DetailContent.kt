package can.losfer.lsbox.ui

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import can.losfer.lsbox.download.LsDownloaderApi
import can.losfer.lsbox.download.LsDownloaderException
import can.losfer.lsbox.download.LsDownloaderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import can.losfer.lsbox.R
import can.losfer.lsbox.api.ApiConfig
import can.losfer.lsbox.data.FollowDays
import can.losfer.lsbox.ui.components.FollowReminderSheet

internal val HeroScrollFadeDistance = 300.dp

internal fun heroScrollProgress(firstVisibleIndex: Int, firstVisibleOffset: Int, referencePx: Int): Float = when {
    firstVisibleIndex > 0 -> 1f
    firstVisibleIndex < 0 -> 0f
    referencePx <= 0 -> 0f
    else -> (firstVisibleOffset.toFloat() / referencePx).coerceIn(0f, 1f)
}

@Composable
internal fun DetailContent(
    activity: DetailActivity,
    vm: DetailViewModel,
    listState: LazyListState,
    onPosterPic: (String) -> Unit,
    onSeed: (Int?) -> Unit,
) {
    val vodUi by vm.vodUi.collectAsState()
    val info = vodUi?.info ?: return

    val flags = info.seriesFlags.orEmpty()
    val currentFlag = info.playFlag
    val episodes = info.seriesMap?.get(currentFlag).orEmpty()
    val playIndex = info.playIndex
    val collected by vm.collected.collectAsState()
    val followRecord by vm.follow.collectAsState()
    val description = remember(info.des) { removeHtmlTag(info.des) }
    var followScheduleOpen by rememberSaveable { mutableStateOf(false) }
    var episodePickOpen by rememberSaveable { mutableStateOf(false) }
    var exportIndexes by remember { mutableStateOf<List<Int>?>(null) }

    val currentSource = ApiConfig.get().getSource(vm.firstsourceKey)
    val displaySourceName = currentSource?.name ?: vm.firstsourceKey
    val heroRollPaused by vm.fullScreen.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // LS 下载器开启且配置完整时才显示下载按钮
    val lsDownloader = LsDownloaderState.config
    val downloadEnabled = lsDownloader.enabled && lsDownloader.apiKey.isNotBlank()
    // 下载流程：点按钮 → 选集弹窗 → 确认后把各集原始链接逐条提交到 LS 下载器
    var downloadPickOpen by rememberSaveable { mutableStateOf(false) }
    var downloadBusy by remember { mutableStateOf(false) }

    /** 单集任务标题：影片名 + 集名。 */
    fun downloadTitleFor(index: Int): String? {
        val episode = episodes.getOrNull(index) ?: return null
        val rawName = episode.name?.trim().orEmpty()
        val movieName = info.name?.trim().orEmpty()
        return when {
            rawName.isBlank() -> movieName.ifBlank { null }
            movieName.isBlank() -> rawName
            else -> "$movieName $rawName"
        }
    }

    fun submitDownloads(indexes: List<Int>) {
        val urls = indexes.mapNotNull { index ->
            val url = episodes.getOrNull(index)?.url?.trim().orEmpty()
            if (url.isEmpty()) null else index to url
        }
        if (urls.isEmpty()) {
            downloadPickOpen = false
            Toast.makeText(
                context,
                context.getString(R.string.detail_no_playable_content),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        downloadBusy = true
        scope.launch {
            val (okCount, duplicateCount, failures) = withContext(Dispatchers.IO) {
                var ok = 0
                var duplicate = 0
                val errors = mutableListOf<String>()
                for ((index, url) in urls) {
                    runCatching { LsDownloaderApi.createTask(url, downloadTitleFor(index)) }
                        .onSuccess { ok++ }
                        .onFailure { e ->
                            if ((e as? LsDownloaderException)?.duplicate == true) {
                                duplicate++
                            } else {
                                errors.add(e.message.orEmpty())
                            }
                        }
                }
                Triple(ok, duplicate, errors)
            }
            downloadBusy = false
            downloadPickOpen = false
            val message = when {
                failures.isNotEmpty() -> context.getString(
                    R.string.ls_dl_batch_result_with_failure,
                    okCount,
                    duplicateCount,
                    failures.size,
                    failures.first().orEmpty(),
                )

                else -> context.getString(R.string.ls_dl_batch_result, okCount, duplicateCount)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "hero") {
            DetailHero(
                title = info.name ?: "TVBox",
                altTitle = info.en,
                picture = info.pic,
                year = info.year,
                area = info.area,
                type = info.type,
                note = info.note,
                description = description,
                collected = collected,
                followed = followRecord != null,
                onPosterPic = onPosterPic,
                onSeed = onSeed,
                onBack = { activity.onBackPressedDispatcher.onBackPressed() },
                onPlay = { vm.onPlayRequested(DetailPlaybackEntry.PlayCapsule) },
                onMusic = { activity.openMusicPlayer() },
                onCast = { activity.openCast() },
                onCollect = { vm.toggleCollect() },
                onFollow = { followScheduleOpen = true },
                onCopyEpisodes = { episodePickOpen = true },
                onDownload = if (downloadEnabled) ({ downloadPickOpen = true }) else null,
                vm = vm,
                rollPaused = heroRollPaused,
            )
        }

        if (episodes.isNotEmpty()) {
            item(key = "episodes") {
                EpisodeRow(vm, info, episodes, playIndex, currentFlag)
            }
        }

        if (flags.isNotEmpty()) {
            item(key = "flags") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                    ) {
                        SectionTitleIcon(painterResource(R.drawable.ic_detail_line))
                        Text(
                            text = stringResource(R.string.detail_line),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.detail_source, displaySourceName),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .widthIn(max = 180.dp),
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(flags, key = { i, f -> "${i}_${f.name}" }) { _, flag ->
                            LineCard(
                                name = flag.name ?: "",
                                count = info.seriesMap?.get(flag.name)?.size ?: 0,
                                selected = flag.name == currentFlag,
                                onClick = { vm.onFlagClick(flag.name ?: "") },
                            )
                        }
                    }
                }
            }
        }

        item(key = "sources") {
            SourceSection(vm, currentSourceName = displaySourceName)
        }

        item(key = "tmdb_info") {
            TmdbCastSection(vm, info.name, info.en, info.year)
        }

        item(key = "tmdb_genres") {
            TmdbGenreSection(vm, info.name, info.en, info.year)
        }

        item(key = "recommend") {
            // 用当前影片的一级分类推荐同类影片，放在页面最下方
            LaunchedEffect(info.tid, info.sourceKey, vm.vodId) { vm.loadRecommend() }
            DetailRecommendSection(vm)
        }
    }

    if (episodePickOpen) {
        EpisodePickSheet(
            episodes = episodes,
            onDismiss = { episodePickOpen = false },
            onConfirm = { picked ->
                episodePickOpen = false
                exportIndexes = picked
            },
        )
    }

    exportIndexes?.let { picked ->
        EpisodeLinkSheet(
            episodes = episodes,
            selectedIndexes = picked,
            onDismiss = { exportIndexes = null },
        )
    }

    if (downloadPickOpen) {
        DownloadPickSheet(
            episodes = episodes,
            initialIndex = playIndex,
            busy = downloadBusy,
            onDismiss = { downloadPickOpen = false },
            onConfirm = { submitDownloads(it) },
        )
    }

    if (followScheduleOpen) {
        FollowReminderSheet(
            initialDays = followRecord?.let { FollowDays.decode(it.updateDays) }?.takeIf { it.isNotEmpty() }
                ?: setOf(0),
            initialHour = followRecord?.updateHour ?: 21,
            onDismissRequest = { followScheduleOpen = false },
            onSave = { days, hour -> vm.saveFollow(days, hour) },
        )
    }
}

@Composable
private fun LineCard(
    name: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DetailItemCard(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.width(DetailItemWidth),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.detail_line_video_count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
