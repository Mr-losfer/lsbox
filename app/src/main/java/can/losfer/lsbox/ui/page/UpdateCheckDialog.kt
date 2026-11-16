package can.losfer.lsbox.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import can.losfer.lsbox.BuildConfig
import can.losfer.lsbox.R
import can.losfer.lsbox.update.ReleaseInfo
import can.losfer.lsbox.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 更新弹窗 —— 使用 Material3 官方 ModalBottomSheet + LazyColumn。
 *
 * 说明：项目自定义的 AVBoxBottomSheet 内部用 weight + verticalScroll 承载内容，
 * verticalScroll 会给子内容无限高度约束，若内容里再出现滚动容器就会抛出
 * "Vertically scrollable component was measured with an infinity maximum height" 崩溃。
 * 这里改用官方组件，LazyColumn 自身即滚动容器、由官方正确处理高度约束，彻底规避该问题。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    release: ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(-1) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!downloading) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.update_title, release.versionName),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.update_current, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    text = release.releaseNotes.ifBlank { stringResource(R.string.update_no_notes) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (downloading) {
                item {
                    LinearProgressIndicator(
                        progress = { if (progress >= 0) progress / 100f else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (progress >= 0) {
                    item {
                        Text(
                            text = "$progress%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                FilledTonalButton(
                    onClick = {
                        if (downloading) return@FilledTonalButton
                        downloading = true
                        UpdateManager.downloadAndInstall(
                            context = context,
                            release = release,
                            onProgress = { progress = it },
                            onDone = { downloading = false },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !downloading,
                ) {
                    Text(stringResource(R.string.update_confirm))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            UpdateManager.skipVersion(release.versionName)
                            onDismiss()
                        },
                        enabled = !downloading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.update_skip))
                    }
                    TextButton(
                        onClick = onDismiss,
                        enabled = !downloading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.update_later))
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/**
 * 执行一次更新检测，把结果安全地分发给调用方。
 * 所有网络与解析都在 IO 线程，通过 runCatching 兜底，任何异常都不会抛到 UI 线程导致闪退。
 */
fun checkUpdate(
    scope: CoroutineScope,
    onAvailable: (ReleaseInfo) -> Unit,
    onMessage: (message: String) -> Unit,
    skipIfDismissed: Boolean = true,
) {
    scope.launch {
        val result = runCatching { withContext(Dispatchers.IO) { UpdateManager.check() } }
            .getOrElse { can.losfer.lsbox.update.UpdateResult.Error(it.message ?: "unknown") }
        when (result) {
            is can.losfer.lsbox.update.UpdateResult.Available -> {
                if (!skipIfDismissed || !UpdateManager.isSkipped(result.release.versionName)) {
                    onAvailable(result.release)
                }
            }
            is can.losfer.lsbox.update.UpdateResult.UpToDate -> onMessage("uptodate")
            is can.losfer.lsbox.update.UpdateResult.Error -> onMessage("error")
        }
    }
}
