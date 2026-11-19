@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package can.losfer.lsbox.ui.page

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import can.losfer.lsbox.R
import can.losfer.lsbox.io.FileUtils
import can.losfer.lsbox.download.LsDownloaderState
import can.losfer.lsbox.ui.ConfigManageActivity
import can.losfer.lsbox.ui.DownloadListActivity
import can.losfer.lsbox.ui.LivePlayActivity
import can.losfer.lsbox.ui.LsDownloaderActivity
import can.losfer.lsbox.ui.SettingsDetailActivity
import can.losfer.lsbox.ui.components.AVBoxAlertDialog
import can.losfer.lsbox.ui.components.AVBoxBottomSheet
import can.losfer.lsbox.ui.components.AppTopBarScaffold
import can.losfer.lsbox.ui.components.LocalSheetDismissThen
import can.losfer.lsbox.ui.components.SettingsCard
import can.losfer.lsbox.ui.components.SettingsCardPosition
import can.losfer.lsbox.ui.components.SettingsGroup
import can.losfer.lsbox.ui.components.SettingsRow
import can.losfer.lsbox.ui.settings.UiSettingsState
import can.losfer.lsbox.util.DefaultConfig
import can.losfer.lsbox.util.SpiderStorageNotice
import can.losfer.lsbox.api.TmdbPoster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CACHE_SIZE_REFRESH_MIN_INTERVAL_MS = 60_000L

data class SettingsState(
    val cacheSizeText: String = "",
)

class SettingsViewModel : ViewModel() {
    private var cacheSizeText: String = ""
    private var cacheSizeRefreshedAt = 0L

    private val _state = mutableStateOf(loadState())

    init {
        refresh()
    }

    val state: androidx.compose.runtime.State<SettingsState> = _state

    fun refresh() {
        UiSettingsState.reload()
        _state.value = loadState()
        refreshCacheSize()
    }

    fun refreshState() {
        UiSettingsState.reload()
        _state.value = loadState()
    }

    fun refreshCacheSize() {
        cacheSizeRefreshedAt = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { cacheSizeText() }
            applyCacheSizeText(text)
        }
    }

    fun refreshCacheSizeIfStale() {
        if (SystemClock.elapsedRealtime() - cacheSizeRefreshedAt < CACHE_SIZE_REFRESH_MIN_INTERVAL_MS) return
        refreshCacheSize()
    }

    fun clearCache(onCleared: () -> Unit = {}) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                FileUtils.clearCache()
                TmdbPoster.clearCachedPosters()
                cacheSizeText()
            }
            applyCacheSizeText(text)
            onCleared()
        }
    }

    private fun cacheSizeText(): String =
        FileUtils.formatCacheSize(FileUtils.cacheSize())

    private fun applyCacheSizeText(text: String) {
        if (text != cacheSizeText) {
            cacheSizeText = text
            _state.value = _state.value.copy(cacheSizeText = text)
        }
    }

    private fun loadState(): SettingsState = SettingsState(
        cacheSizeText = cacheSizeText,
    )
}

@Composable
fun SettingsPage(
    vm: SettingsViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** 点击「追剧」时由 MainScreen 切换 Tab */
    onOpenFollowing: () -> Unit = {},
) {
    val navStart = contentPadding.calculateStartPadding(LocalLayoutDirection.current)
    val navBottom = contentPadding.calculateBottomPadding()
    val state by vm.state
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.refreshState()
        vm.refreshCacheSizeIfStale()
        LsDownloaderState.reload()
    }
    val context = LocalContext.current
    val versionName = remember { DefaultConfig.appVersionName(context) ?: "" }
    var aboutSheet by remember { mutableStateOf(false) }
    val lsDownloader = LsDownloaderState.config
    val listState = rememberScrollState()

    AppTopBarScaffold(
        topBarStartInset = navStart,
        titleContent = {
            Text(
                text = stringResource(R.string.tab_mine),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) { topPad, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(listState)
                .padding(horizontal = 16.dp)
                .padding(start = navStart, bottom = 8.dp + navBottom),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Spacer(Modifier.height(topPad - 20.dp))

            AppInfoHeaderCard(versionName = versionName)

            // 订阅管理 + 直播入口 + 下载相关
            val rows = buildList {
                add(MineRow.Kind.Subscribe)
                add(MineRow.Kind.Live)
                add(MineRow.Kind.Following)
                if (lsDownloader.enabled) {
                    add(MineRow.Kind.LsDownloader)
                    add(MineRow.Kind.DownloadList)
                }
                add(MineRow.Kind.Settings)
            }
            SettingsGroup(title = null) {
                rows.forEachIndexed { index, kind ->
                    SettingsCard(
                        position = when {
                            rows.size == 1 -> SettingsCardPosition.SINGLE
                            index == 0 -> SettingsCardPosition.FIRST
                            index == rows.lastIndex -> SettingsCardPosition.LAST
                            else -> SettingsCardPosition.MIDDLE
                        },
                    ) {
                        when (kind) {
                            MineRow.Kind.Subscribe -> SettingsRow(
                                title = stringResource(R.string.settings_config_manage),
                                subtitle = stringResource(R.string.settings_config_manage_subtitle),
                                iconRes = R.drawable.ic_settings_api,
                                onClick = { ConfigManageActivity.start(context) },
                            )

                            MineRow.Kind.Live -> SettingsRow(
                                title = stringResource(R.string.mine_live_entry),
                                subtitle = stringResource(R.string.mine_live_entry_subtitle),
                                iconRes = R.drawable.ic_live_fab,
                                onClick = {
                                    context.startActivity(
                                        Intent(context, LivePlayActivity::class.java),
                                    )
                                },
                            )

                            MineRow.Kind.Following -> SettingsRow(
                                title = stringResource(R.string.tab_following),
                                subtitle = stringResource(R.string.mine_following_subtitle),
                                iconRes = R.drawable.ic_tab_following,
                                onClick = onOpenFollowing,
                            )

                            MineRow.Kind.LsDownloader -> SettingsRow(
                                title = stringResource(R.string.ls_dl_title),
                                subtitle = stringResource(R.string.ls_dl_subtitle),
                                iconRes = R.drawable.ic_ls_download,
                                valueText = stringResource(
                                    if (lsDownloader.enabled) R.string.common_on else R.string.common_off,
                                ),
                                onClick = { LsDownloaderActivity.start(context) },
                            )

                            MineRow.Kind.DownloadList -> SettingsRow(
                                title = stringResource(R.string.ls_dl_list_title),
                                subtitle = stringResource(R.string.ls_dl_list_subtitle),
                                iconRes = R.drawable.ic_settings_history,
                                onClick = { DownloadListActivity.start(context) },
                            )

                            MineRow.Kind.Settings -> SettingsRow(
                                title = stringResource(R.string.settings_page_title),
                                subtitle = stringResource(R.string.settings_page_subtitle),
                                iconRes = R.drawable.ic_settings_preference,
                                onClick = { SettingsDetailActivity.start(context) },
                            )
                        }
                    }
                }
            }

            SettingsGroup(title = null) {
                SettingsCard(SettingsCardPosition.MIDDLE) {
                    SettingsRow(
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_about_subtitle),
                        iconRes = R.drawable.ic_settings_about,
                        onClick = { aboutSheet = true },
                    )
                }
                SettingsCard(SettingsCardPosition.LAST) {
                    SettingsRow(
                        title = stringResource(R.string.settings_github),
                        subtitle = stringResource(R.string.settings_github_subtitle),
                        iconRes = R.drawable.ic_settings_github,
                        onClick = { openExternalUrl(context, GITHUB_REPO_URL) },
                    )
                }
            }
        }
    }

    if (aboutSheet) {
        AboutSheet(versionName = versionName, onDismiss = { aboutSheet = false })
    }
}

/** 「我的」页的行类型，用于按开关状态动态拼接卡片。 */
private object MineRow {
    enum class Kind { Subscribe, Live, Following, LsDownloader, DownloadList, Settings }
}

private const val AboutSheetHeightFraction = 0.6f

private const val GITHUB_REPO_URL = "https://github.com/Mr-losfer/lsbox"

@Composable
private fun AboutSheet(versionName: String, onDismiss: () -> Unit) {
    AVBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_about),
        modifier = Modifier.fillMaxHeight(AboutSheetHeightFraction),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            if (versionName.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.settings_about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.settings_about_author),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.settings_about_upstream),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.settings_about_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        SpiderStorageNotice.suppress()
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.toast_no_app_for_link), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun TextEditDialog(
    title: String,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    AVBoxAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.dialog_api_url_hint)) },
            )
        },
        confirmButton = {
            val dismissThen = LocalSheetDismissThen.current
            TextButton(onClick = { dismissThen { onConfirm(text.trim()) } }) {
                Text(stringResource(R.string.common_confirm))
            }
        },
    )
}
