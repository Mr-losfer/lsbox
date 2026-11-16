package can.losfer.lsbox.ui

import android.content.res.Configuration
import android.os.Build
import can.losfer.lsbox.ui.theme.enableTransparentEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import can.losfer.lsbox.R
import can.losfer.lsbox.ui.base.BaseActivity
import can.losfer.lsbox.player.PlaybackService
import can.losfer.lsbox.ui.page.MainScreen
import can.losfer.lsbox.ui.theme.AVBoxTheme
import can.losfer.lsbox.ui.theme.AppThemeState
import can.losfer.lsbox.ui.page.UpdateCheckDialog
import can.losfer.lsbox.ui.page.WelcomeScreen
import can.losfer.lsbox.util.LanguageManager
import can.losfer.lsbox.util.PermissionHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
                var showUpdate by remember { mutableStateOf(alreadyPicked) }
                if (showWelcome) {
                    WelcomeScreen(onFinished = { showWelcome = false; showUpdate = true })
                } else {
                    MainScreen()
                    if (showUpdate) {
                        UpdateCheckDialog(autoCheck = true, onDismiss = { showUpdate = false })
                    }
                }
            }
        }
    }
}
