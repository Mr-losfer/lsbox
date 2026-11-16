package can.losfer.lsbox.ui

import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import can.losfer.lsbox.R
import can.losfer.lsbox.ui.base.BaseActivity
import can.losfer.lsbox.player.PlaybackService
import can.losfer.lsbox.ui.page.MainScreen
import can.losfer.lsbox.ui.theme.AVBoxTheme
import can.losfer.lsbox.ui.theme.AppThemeState
import can.losfer.lsbox.ui.page.UpdateSheet
import can.losfer.lsbox.ui.page.WelcomeScreen
import can.losfer.lsbox.ui.page.checkUpdate
import can.losfer.lsbox.update.ReleaseInfo
import can.losfer.lsbox.util.LanguageManager
import can.losfer.lsbox.util.PermissionHelper
import can.losfer.lsbox.ui.theme.enableTransparentEdgeToEdge

private const val PREWARM_ON_RESUME_DELAY_MS = 2000L

class MainActivity : BaseActivity() {

    override fun shouldRefreshAutoSize(): Boolean = true

    override fun hideSysBar() {
    }

    private fun applyStatusBarAppearance() {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val light = !AppThemeState.isDark(night)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarAppearance()
        PlaybackService.prewarm(this, PREWARM_ON_RESUME_DELAY_MS)
    }

    override fun init() {
        enableTransparentEdgeToEdge()
        applyStatusBarAppearance()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !PermissionHelper.isStorageGranted(this)) {
            PermissionHelper.requestStorage(this) { _, _ -> }
        }
        PermissionHelper.requestNotificationIfNeeded(this)
        findViewById<ComposeView>(R.id.compose_view).setContent {
            AVBoxTheme {
                val alreadyPicked: Boolean = LanguageManager.isPicked()
                var showWelcome by remember { mutableStateOf(!alreadyPicked) }
                var updateRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
                val updateScope = rememberCoroutineScope()

                // 自动检测更新只在进入主界面后触发一次；结果驱动 state，绝不在组合期间修改父级状态
                LaunchedEffect(showWelcome) {
                    if (showWelcome) return@LaunchedEffect
                    checkUpdate(
                        scope = this,
                        onAvailable = { updateRelease = it },
                        onMessage = { /* 自动检测时静默，不打扰用户 */ },
                    )
                }

                if (showWelcome) {
                    WelcomeScreen(onFinished = { showWelcome = false })
                } else {
                    MainScreen()
                    updateRelease?.let { rel ->
                        UpdateSheet(release = rel, onDismiss = { updateRelease = null })
                    }
                }
            }
        }
    }
}
