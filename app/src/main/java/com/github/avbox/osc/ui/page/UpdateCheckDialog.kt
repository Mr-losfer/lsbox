package com.github.avbox.osc.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.avbox.osc.R
import com.github.avbox.osc.ui.components.AVBoxAlertDialog
import com.github.avbox.osc.update.ReleaseInfo
import com.github.avbox.osc.update.UpdateManager
import com.github.avbox.osc.update.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UpdateCheckDialog(
    autoCheck: Boolean = true,
    onDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableIntStateOf(if (autoCheck) 0 else 1) }
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(-1) }

    LaunchedEffect(autoCheck) {
        if (!autoCheck) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) { UpdateManager.check() }
        if (result is UpdateResult.Available && !UpdateManager.isSkipped(result.release.versionName)) {
            release = result.release
            phase = 1
        } else {
            phase = -1
        }
    }

    if (phase == -1) {
        onDismiss()
        return
    }
    if (phase == 0) return

    val info = release ?: return

    AVBoxAlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        dismissible = !downloading,
        title = { Text(stringResource(R.string.update_title, info.versionName)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_current, com.github.avbox.osc.BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = info.releaseNotes.ifBlank { stringResource(R.string.update_no_notes) },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { if (progress >= 0) progress / 100f else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(top = 8.dp),
                    )
                    if (progress >= 0) {
                        Text(
                            text = "$progress%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloading,
                onClick = {
                    downloading = true
                    UpdateManager.downloadAndInstall(
                        context = context,
                        release = info,
                        onProgress = { progress = it },
                        onDone = { downloading = false },
                    )
                },
            ) {
                Text(stringResource(R.string.update_confirm))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = !downloading,
                    onClick = {
                        UpdateManager.skipVersion(info.versionName)
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.update_skip))
                }
                TextButton(
                    enabled = !downloading,
                    onClick = onDismiss,
                ) {
                    Text(stringResource(R.string.update_later))
                }
            }
        },
    )
}
