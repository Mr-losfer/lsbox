package can.losfer.lsbox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import can.losfer.lsbox.R
import can.losfer.lsbox.bean.VodInfo

/**
 * LS 下载器的选集弹窗：勾选要下载的集数，确认后把这些集的原始播放链接提交到下载器。
 *
 * 与 [EpisodePickSheet]（只用于导出链接文本）分开，避免互相牵连。
 *
 * @param initialIndex 打开时默认勾选的集数（通常是当前正在播放的那一集）
 * @param busy 提交中，按钮置灰防重复点击
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPickSheet(
    episodes: List<VodInfo.VodSeries>,
    initialIndex: Int,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = remember {
        mutableStateListOf<Int>().apply {
            val start = initialIndex.coerceIn(0, (episodes.size - 1).coerceAtLeast(0))
            if (episodes.isNotEmpty()) add(start)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ls_dl_pick_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.ls_dl_pick_count,
                            selected.size,
                            episodes.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = {
                        selected.clear()
                        selected.addAll(episodes.indices)
                    },
                ) {
                    Text(stringResource(R.string.episode_pick_select_all))
                }
                TextButton(onClick = { selected.clear() }) {
                    Text(stringResource(R.string.episode_pick_clear_all))
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                itemsIndexed(episodes) { index, episode ->
                    val checked = selected.contains(index)
                    fun toggle() {
                        if (checked) selected.remove(index) else selected.add(index)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { toggle() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { toggle() })
                        Text(
                            text = episode.name.orEmpty().ifBlank { "${index + 1}" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.episode_pick_cancel))
                }
                Button(
                    onClick = { onConfirm(selected.sorted()) },
                    enabled = selected.isNotEmpty() && !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (busy) {
                            stringResource(R.string.ls_dl_pick_submitting)
                        } else {
                            stringResource(R.string.ls_dl_pick_confirm)
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
