package can.losfer.lsbox.ui.page

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import can.losfer.lsbox.R
import can.losfer.lsbox.net.HttpClients
import can.losfer.lsbox.ui.PlaySettingsActivity
import can.losfer.lsbox.ui.PreferenceSettingsActivity
import can.losfer.lsbox.ui.ThemeSettingsActivity
import can.losfer.lsbox.ui.components.AVBoxAlertDialog
import can.losfer.lsbox.ui.components.AVBoxBottomSheet
import can.losfer.lsbox.ui.components.AppTopBarScaffold
import can.losfer.lsbox.ui.components.LocalSheetDismissThen
import can.losfer.lsbox.ui.components.SettingsCard
import can.losfer.lsbox.ui.components.SettingsCardPosition
import can.losfer.lsbox.ui.components.SettingsGroup
import can.losfer.lsbox.ui.components.SettingsOptionMenuRow
import can.losfer.lsbox.ui.components.SettingsRow
import can.losfer.lsbox.ui.components.TopBarActionBox
import can.losfer.lsbox.ui.settings.UiSettingsState
import can.losfer.lsbox.util.HistoryHelper
import can.losfer.lsbox.util.SearchSettingsState
import can.losfer.lsbox.util.UyghurFont
import can.losfer.lsbox.util.UyghurFontManager
import can.losfer.lsbox.update.ReleaseInfo
import kotlinx.coroutines.launch

/**
 * 全量设置页。原「我的」页里的主题/播放/偏好/字体/默认页/历史/缓存/DOH/更新
 * 全部迁移到这里，顶部入口卡片与「我的」页保持一致的设置卡片风格。
 */
@Composable
fun SettingsDetailScreen(
    onNavigateBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.state
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.refreshState()
        vm.refreshCacheSizeIfStale()
    }
    val context = LocalContext.current
    var fontSheet by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var updateRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberScrollState()

    AppTopBarScaffold(
        titleContent = {
            Text(
                text = stringResource(R.string.settings_page_title),
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
    ) { topPad, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(listState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(topPad + 8.dp))

            SettingsGroup(title = null) {
                SettingsCard(SettingsCardPosition.FIRST) {
                    SettingsRow(
                        title = stringResource(R.string.settings_theme),
                        subtitle = stringResource(R.string.settings_theme_subtitle),
                        iconRes = R.drawable.ic_settings_theme,
                        onClick = { ThemeSettingsActivity.start(context) },
                    )
                }
                SettingsCard(SettingsCardPosition.MIDDLE) {
                    SettingsRow(
                        title = stringResource(R.string.settings_play),
                        subtitle = stringResource(R.string.settings_play_subtitle),
                        iconRes = R.drawable.ic_settings_play,
                        onClick = { PlaySettingsActivity.start(context) },
                    )
                }
                SettingsCard(SettingsCardPosition.MIDDLE) {
                    SettingsRow(
                        title = stringResource(R.string.settings_preference_title),
                        subtitle = stringResource(R.string.settings_preference_subtitle),
                        iconRes = R.drawable.ic_settings_preference,
                        onClick = { PreferenceSettingsActivity.start(context) },
                    )
                }
                SettingsCard(SettingsCardPosition.LAST) {
                    SettingsRow(
                        title = stringResource(R.string.font_settings),
                        subtitle = stringResource(R.string.font_settings_subtitle),
                        iconRes = R.drawable.ic_settings_theme,
                        valueText = UyghurFontManager.current().displayName,
                        onClick = { fontSheet = true },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            SettingsGroup(title = null) {
                SettingsCard(SettingsCardPosition.FIRST) {
                    val uiConfig = UiSettingsState.config
                    SettingsOptionMenuRow(
                        title = stringResource(R.string.settings_default_page),
                        subtitle = stringResource(R.string.settings_default_page_subtitle),
                        iconRes = R.drawable.ic_settings_start,
                        valueText = stringResource(if (uiConfig.defaultLoadLive) R.string.common_live else R.string.common_vod),
                        options = listOf(
                            stringResource(R.string.common_vod),
                            stringResource(R.string.common_live),
                        ),
                        selectedIndex = if (uiConfig.defaultLoadLive) 1 else 0,
                        onSelect = { idx -> UiSettingsState.defaultLoadLive = idx == 1 },
                    )
                }
                SettingsCard(SettingsCardPosition.MIDDLE) {
                    SettingsOptionMenuRow(
                        title = stringResource(R.string.settings_history_limit),
                        subtitle = stringResource(R.string.settings_history_limit_subtitle),
                        iconRes = R.drawable.ic_settings_history,
                        valueText = stringResource(
                            R.string.settings_history_limit_value,
                            HistoryHelper.getHisNum(SearchSettingsState.config.historyNumIndex),
                        ),
                        options = listOf(0, 1, 2).map {
                            stringResource(R.string.settings_history_limit_value, HistoryHelper.getHisNum(it))
                        },
                        selectedIndex = SearchSettingsState.config.historyNumIndex,
                        onSelect = { idx -> SearchSettingsState.historyNumIndex = idx },
                    )
                }
                SettingsCard(SettingsCardPosition.MIDDLE) {
                    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
                    SettingsRow(
                        title = stringResource(R.string.settings_clear_cache),
                        subtitle = stringResource(R.string.settings_clear_cache_subtitle),
                        iconRes = R.drawable.ic_delete,
                        valueText = state.cacheSizeText,
                        onClick = { showClearCacheDialog = true },
                    )
                    if (showClearCacheDialog) {
                        val dismissThen = LocalSheetDismissThen.current
                        AVBoxAlertDialog(
                            onDismissRequest = { showClearCacheDialog = false },
                            title = { Text(stringResource(R.string.settings_clear_cache)) },
                            text = { Text(stringResource(R.string.settings_clear_cache_confirm_text)) },
                            dismissButton = {
                                TextButton(onClick = { showClearCacheDialog = false }) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showClearCacheDialog = false
                                    dismissThen {
                                        vm.clearCache {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_cache_cleared),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                }) {
                                    Text(stringResource(R.string.common_confirm))
                                }
                            },
                        )
                    }
                }
                SettingsCard(SettingsCardPosition.LAST) {
                    val uiConfig = UiSettingsState.config
                    SettingsOptionMenuRow(
                        title = "DOH",
                        subtitle = stringResource(R.string.settings_doh_subtitle),
                        iconRes = R.drawable.ic_settings_doh,
                        valueText = if (uiConfig.dohIndex == 0) stringResource(R.string.common_off)
                        else HttpClients.dnsHttpsList.getOrNull(uiConfig.dohIndex)
                            ?: stringResource(R.string.common_off),
                        options = HttpClients.dnsHttpsList.mapIndexed { index, name ->
                            if (index == 0) context.getString(R.string.common_off) else name
                        },
                        selectedIndex = uiConfig.dohIndex,
                        onSelect = { idx -> UiSettingsState.dohIndex = idx },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            SettingsGroup(title = null) {
                SettingsCard(SettingsCardPosition.FIRST) {
                    SettingsRow(
                        title = stringResource(R.string.ls_dl_title),
                        subtitle = stringResource(R.string.ls_dl_subtitle),
                        iconRes = R.drawable.ic_ls_download,
                        valueText = stringResource(
                            if (can.losfer.lsbox.download.LsDownloaderState.config.enabled) {
                                R.string.common_on
                            } else {
                                R.string.common_off
                            },
                        ),
                        onClick = { LsDownloaderActivityStart.open(context) },
                    )
                }
                SettingsCard(SettingsCardPosition.LAST) {
                    SettingsRow(
                        title = stringResource(R.string.update_check),
                        subtitle = stringResource(R.string.update_checking),
                        iconRes = R.drawable.ic_settings_api,
                        onClick = {
                            checkUpdate(
                                scope = scope,
                                onAvailable = { updateRelease = it },
                                onMessage = { updateMessage = it },
                                skipIfDismissed = false,
                            )
                        },
                    )
                }
            }
        }
    }

    if (fontSheet) {
        FontPickerSheet(
            onDismiss = { fontSheet = false },
            onSelected = { UyghurFontManager.set(it); fontSheet = false },
        )
    }

    val updateToast = updateMessage?.let { msg ->
        when (msg) {
            "uptodate" -> stringResource(R.string.update_no_new)
            "error" -> stringResource(R.string.update_failed)
            else -> null
        }
    }
    if (updateToast != null) {
        LaunchedEffect(updateToast) {
            Toast.makeText(context, updateToast, Toast.LENGTH_LONG).show()
            updateMessage = null
        }
    }

    updateRelease?.let { rel ->
        UpdateSheet(release = rel, onDismiss = { updateRelease = null })
    }
}

/** 字体选择底部弹窗（从原「我的」页迁出，保持原有交互）。 */
@Composable
internal fun FontPickerSheet(
    onDismiss: () -> Unit,
    onSelected: (UyghurFont) -> Unit,
) {
    var current by remember { mutableStateOf(UyghurFontManager.current()) }
    AVBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.font_page_title),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.font_page_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            UyghurFontManager.available().forEach { font ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { current = font; onSelected(font) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = font == current, onClick = { current = font; onSelected(font) })
                    Text(
                        text = font.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/** 打开 LS 下载器设置页（Activity 延迟引用，避免循环依赖）。 */
private object LsDownloaderActivityStart {
    fun open(context: android.content.Context) {
        can.losfer.lsbox.ui.LsDownloaderActivity.start(context)
    }
}
