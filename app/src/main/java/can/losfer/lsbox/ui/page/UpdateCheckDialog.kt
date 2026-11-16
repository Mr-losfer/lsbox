package can.losfer.lsbox.ui.page

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import can.losfer.lsbox.ui.components.AVBoxBottomSheet
import can.losfer.lsbox.ui.components.LocalSheetDismiss
import can.losfer.lsbox.update.ReleaseInfo
import can.losfer.lsbox.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 更新弹窗 —— 从底部弹出的 Modal Bottom Sheet，自动检测与手动检测共用。
 *
 * 检测逻辑与展示分离：外部只在拿到 [UpdateResult.Available] 后把 release 传进来，
 * 本组件只负责展示与下载，绝不在组合期间写父级状态，避免重组崩溃。
 */
@Composable
fun UpdateSheet(
    release: ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dismissAnimated = LocalSheetDismiss.current
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(-1) }

    // 关闭时先播放下拉动画，再通知父级移除；幂等，避免漏关或重复关
    fun close() {
        dismissAnimated()
        onDismiss()
    }

    AVBoxBottomSheet(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = stringResource(R.string.update_title, release.versionName),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.update_current, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = release.releaseNotes.ifBlank { stringResource(R.string.update_no_notes) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (downloading) {
                LinearProgressIndicator(
                    progress = { if (progress >= 0) progress / 100f else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (progress >= 0) {
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
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
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        UpdateManager.skipVersion(release.versionName)
                        close()
                    },
                    enabled = !downloading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.update_skip))
                }
                TextButton(
                    onClick = { close() },
                    enabled = !downloading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.update_later))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * 执行一次更新检测，把结果安全地分发给调用方。
 * - Available：回调 [onAvailable]，由调用方决定何时/如何弹出 [UpdateSheet]
 * - UpToDate / Error：回调 [onMessage]，用于 toast 提示
 *
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
