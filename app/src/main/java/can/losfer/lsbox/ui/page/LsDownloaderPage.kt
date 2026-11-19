package can.losfer.lsbox.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import can.losfer.lsbox.R
import can.losfer.lsbox.download.LsDownloaderApi
import can.losfer.lsbox.download.LsDownloaderState
import can.losfer.lsbox.ui.components.AppTopBarScaffold
import can.losfer.lsbox.ui.components.SettingsCard
import can.losfer.lsbox.ui.components.SettingsCardPosition
import can.losfer.lsbox.ui.components.SettingsGroup
import can.losfer.lsbox.ui.components.SettingsRow
import can.losfer.lsbox.ui.components.SettingsSwitchRow
import can.losfer.lsbox.ui.components.TopBarActionBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface ConnState {
    data object Idle : ConnState

    data object Testing : ConnState

    data object Ok : ConnState

    data class Fail(val message: String) : ConnState
}

/**
 * LS 下载器设置页：接口地址 / API Key / 保存目录 / 启用开关。
 * 默认关闭，只有开启并填写完整后，详情页才会出现下载按钮、我的页才会出现下载列表。
 */
@Composable
fun LsDownloaderScreen(onNavigateBack: () -> Unit) {
    val config = LsDownloaderState.config
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var saveDir by remember { mutableStateOf(config.saveDir) }
    var conn by remember { mutableStateOf<ConnState>(ConnState.Idle) }
    val scope = rememberCoroutineScope()
    val listState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun save() {
        LsDownloaderState.baseUrl = baseUrl
        LsDownloaderState.apiKey = apiKey
        LsDownloaderState.saveDir = saveDir
    }

    AppTopBarScaffold(
        titleContent = {
            Text(
                text = stringResource(R.string.ls_dl_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            TopBarActionBox(
                R.drawable.ic_arrow_left,
                stringResource(R.string.common_back),
                onClick = { save(); onNavigateBack() },
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
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(Modifier.height(topPad + 8.dp))

            Text(
                text = stringResource(R.string.ls_dl_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )

            SettingsGroup(title = null) {
                SettingsCard(SettingsCardPosition.SINGLE) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.ls_dl_enable),
                        subtitle = stringResource(R.string.ls_dl_enable_subtitle),
                        checked = config.enabled,
                        onCheckedChange = { LsDownloaderState.enabled = it },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            SettingsGroup(title = stringResource(R.string.ls_dl_group_conn)) {
                SettingsCard(SettingsCardPosition.SINGLE) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text(stringResource(R.string.ls_dl_base_url)) },
                            placeholder = { Text("https://office.app.5ddd.com:34949") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text(stringResource(R.string.ls_dl_api_key)) },
                            placeholder = { Text("ms_xxxxxxxx") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = saveDir,
                            onValueChange = { saveDir = it },
                            label = { Text(stringResource(R.string.ls_dl_save_dir)) },
                            supportingText = { Text(stringResource(R.string.ls_dl_save_dir_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            SettingsGroup(title = null) {
                SettingsCard(SettingsCardPosition.SINGLE) {
                    SettingsRow(
                        title = stringResource(R.string.ls_dl_save),
                        subtitle = stringResource(R.string.ls_dl_save_subtitle),
                        iconRes = R.drawable.ic_ls_save,
                        onClick = {
                            save()
                            Toast.makeText(
                                context,
                                context.getString(R.string.ls_dl_saved),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = {
                    save()
                    conn = ConnState.Testing
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching { LsDownloaderApi.health() }.getOrDefault(false)
                        }
                        conn = if (ok) ConnState.Ok else ConnState.Fail("health check failed")
                    }
                },
                enabled = conn !is ConnState.Testing,
            ) {
                Text(stringResource(R.string.ls_dl_test))
            }

            when (val c = conn) {
                ConnState.Ok -> Text(
                    text = stringResource(R.string.ls_dl_test_ok),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                )

                is ConnState.Fail -> Text(
                    text = stringResource(R.string.ls_dl_test_fail, c.message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                )

                ConnState.Testing -> Text(
                    text = stringResource(R.string.ls_dl_testing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                )

                ConnState.Idle -> Unit
            }
        }
    }
}
