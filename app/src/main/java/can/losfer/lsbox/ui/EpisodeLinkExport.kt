package can.losfer.lsbox.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import can.losfer.lsbox.R
import can.losfer.lsbox.bean.VodInfo
import can.losfer.lsbox.data.EpisodeMatcher

/**
 * 剧集链接导出：表达式模板。
 *
 * 支持的占位符：
 * - {n}     集数（优先从剧集名里提取的数字，取不到则用序号）
 * - {name}  剧集名称
 * - {url}   剧集播放地址
 * - {index} 选中列表中的顺序号（从 1 开始）
 * - \n      换行
 */
data class LinkTemplate(val label: String, val template: String)

val defaultLinkTemplates = listOf(
    LinkTemplate("{n},{url}", "{n},{url}"),
    LinkTemplate("{n}\${url}", "{n}\${url}"),
    LinkTemplate("{url}", "{url}"),
    LinkTemplate("第{n}集,{url}", "第{n}集,{url}"),
    LinkTemplate("{name},{url}", "{name},{url}"),
    LinkTemplate("{name}\${url}", "{name}\${url}"),
    LinkTemplate("M3U", "#EXTINF:-1,{name}\\n{url}"),
)

/** 按模板生成链接文本 */
fun buildEpisodeLinks(
    episodes: List<VodInfo.VodSeries>,
    selectedIndexes: Collection<Int>,
    template: String,
): String {
    if (template.isEmpty()) return ""
    val builder = StringBuilder()
    selectedIndexes.sorted().forEachIndexed { order, index ->
        val episode = episodes.getOrNull(index) ?: return@forEachIndexed
        val name = episode.name.orEmpty()
        val url = episode.url.orEmpty()
        val number = EpisodeMatcher.extractEpisodeNumber(name) ?: (order + 1)
        val line = template
            .replace("{n}", number.toString())
            .replace("{name}", name)
            .replace("{url}", url)
            .replace("{index}", (order + 1).toString())
            .replace("\\n", "\n")
        if (line.isNotBlank()) builder.append(line).append('\n')
    }
    return builder.toString().trimEnd()
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("LSBox", text))
}

/**
 * 剧集多选弹窗：全选 / 全取消 / 单选，底部左取消右确认。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePickSheet(
    episodes: List<VodInfo.VodSeries>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = remember { mutableStateListOf<Int>() }

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
                        text = stringResource(R.string.episode_pick_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.episode_pick_count,
                            selected.size,
                            episodes.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = {
                    selected.clear()
                    selected.addAll(episodes.indices)
                }) {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (checked) selected.remove(index) else selected.add(index)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                if (checked) selected.remove(index) else selected.add(index)
                            },
                        )
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
                    onClick = { onConfirm(selected.toList()) },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.episode_pick_confirm))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * 链接导出弹窗：表达式输入（textarea）+ 输出结果（textarea），可一键复制。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeLinkSheet(
    episodes: List<VodInfo.VodSeries>,
    selectedIndexes: List<Int>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var template by remember { mutableStateOf(defaultLinkTemplates.first().template) }
    val output = remember(template, selectedIndexes, episodes) {
        buildEpisodeLinks(episodes, selectedIndexes, template)
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
            Text(
                text = stringResource(R.string.episode_export_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.episode_export_count, selectedIndexes.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                defaultLinkTemplates.forEach { preset ->
                    FilterChip(
                        selected = template == preset.template,
                        onClick = { template = preset.template },
                        label = { Text(preset.label) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                label = { Text(stringResource(R.string.episode_export_template)) },
                supportingText = { Text(stringResource(R.string.episode_export_hint)) },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = output,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.episode_export_output)) },
                minLines = 5,
                maxLines = 9,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

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
                    Text(stringResource(R.string.episode_export_close))
                }
                Button(
                    onClick = {
                        if (output.isNotBlank()) {
                            copyToClipboard(context, output)
                            Toast.makeText(
                                context,
                                context.getString(R.string.episode_export_copied),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    enabled = output.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.episode_export_copy))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}