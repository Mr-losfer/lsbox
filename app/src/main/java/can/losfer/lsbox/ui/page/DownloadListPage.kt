package can.losfer.lsbox.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import can.losfer.lsbox.R
import can.losfer.lsbox.download.LsDownloaderApi
import can.losfer.lsbox.download.LsTask
import can.losfer.lsbox.download.LsTaskStatus
import can.losfer.lsbox.ui.components.AppTopBarScaffold
import can.losfer.lsbox.ui.components.SettingsCard
import can.losfer.lsbox.ui.components.SettingsCardPosition
import can.losfer.lsbox.ui.components.SettingsGroup
import can.losfer.lsbox.ui.components.TopBarActionBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RefreshIntervalMs = 2000L

private sealed interface DownloadListState {
    data object Loading : DownloadListState

    data class Ready(val tasks: List<LsTask>) : DownloadListState

    data class Error(val message: String) : DownloadListState
}

@Composable
fun DownloadListScreen(onNavigateBack: () -> Unit) {
    var state by remember { mutableStateOf<DownloadListState>(DownloadListState.Loading) }
    var refreshTick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        val result = withContext(Dispatchers.IO) {
            runCatching { LsDownloaderApi.listTasks(limit = 200) }
        }
        state = result.fold(
            onSuccess = { DownloadListState.Ready(it) },
            onFailure = { DownloadListState.Error(it.message ?: "error") },
        )
    }

    LaunchedEffect(refreshTick) {
        while (true) {
            refresh()
            delay(RefreshIntervalMs)
        }
    }

    fun act(block: suspend () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { block() } }
            refresh()
        }
    }

    AppTopBarScaffold(
        titleContent = {
            Text(
                text = stringResource(R.string.ls_dl_list_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            TopBarActionBox(
                R.drawable.ic_arrow_left,
                stringResource(R.string.common_back),
                onClick = onNavigateBack,
                mirrorInRtl = true,
            )
        },
        actions = {
            TopBarActionBox(
                R.drawable.ic_ls_refresh,
                stringResource(R.string.common_retry),
                onClick = { refreshTick++ },
            )
        },
    ) { topPad, _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val current = state) {
                DownloadListState.Loading -> LoadingBlock(topPad)
                is DownloadListState.Error -> ErrorBlock(topPad, current.message) { refreshTick++ }
                is DownloadListState.Ready -> {
                    if (current.tasks.isEmpty()) {
                        EmptyBlock(topPad)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = topPad + 8.dp,
                                bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(current.tasks, key = { it.id }) { task ->
                                val index = current.tasks.indexOf(task)
                                SettingsCard(position = cardPosition(index, current.tasks.size)) {
                                    DownloadTaskRow(
                                        task = task,
                                        onCancel = { act { LsDownloaderApi.cancelTask(task.id) } },
                                        onRetry = { act { LsDownloaderApi.retryTask(task.id) } },
                                        onDelete = {
                                            act { LsDownloaderApi.deleteTask(task.id, deleteFile = true) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun cardPosition(index: Int, size: Int): SettingsCardPosition = when {
    size <= 1 -> SettingsCardPosition.SINGLE
    index == 0 -> SettingsCardPosition.FIRST
    index == size - 1 -> SettingsCardPosition.LAST
    else -> SettingsCardPosition.MIDDLE
}

@Composable
private fun DownloadTaskRow(
    task: LsTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.ifBlank { task.url },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusLabel(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!task.finished) {
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            when (task.status) {
                LsTaskStatus.QUEUED, LsTaskStatus.RUNNING, LsTaskStatus.MERGING -> {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.ls_dl_cancel))
                    }
                }

                LsTaskStatus.FAILED, LsTaskStatus.CANCELED -> {
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.ls_dl_retry))
                    }
                }

                else -> Unit
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.ls_dl_delete))
            }
        }
    }
}

@Composable
private fun statusLabel(task: LsTask): String {
    val statusText = when (task.status) {
        LsTaskStatus.QUEUED -> stringResource(R.string.ls_dl_status_queued)
        LsTaskStatus.RUNNING -> stringResource(R.string.ls_dl_status_running)
        LsTaskStatus.MERGING -> stringResource(R.string.ls_dl_status_merging)
        LsTaskStatus.COMPLETED -> stringResource(R.string.ls_dl_status_completed)
        LsTaskStatus.FAILED -> stringResource(R.string.ls_dl_status_failed)
        LsTaskStatus.CANCELED -> stringResource(R.string.ls_dl_status_canceled)
        LsTaskStatus.UNKNOWN -> stringResource(R.string.ls_dl_status_unknown)
    }
    val percent = (task.progress * 100).toInt()
    return if (task.finished) statusText else "$statusText · $percent%"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingBlock(topPad: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPad),
        contentAlignment = Alignment.Center,
    ) {
        ContainedLoadingIndicator(Modifier.size(40.dp))
    }
}

@Composable
private fun EmptyBlock(topPad: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPad, start = 32.dp, end = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.ls_dl_list_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorBlock(topPad: Dp, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPad, start = 32.dp, end = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.ls_dl_list_error),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.common_retry))
        }
    }
}
