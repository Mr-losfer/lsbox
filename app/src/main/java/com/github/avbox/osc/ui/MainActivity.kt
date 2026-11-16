package com.github.avbox.osc.ui

import android.content.res.Configuration
import android.os.Build
import com.github.avbox.osc.ui.theme.enableTransparentEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import com.github.avbox.osc.R
import com.github.avbox.osc.ui.base.BaseActivity
import com.github.avbox.osc.player.PlaybackService
import com.github.avbox.osc.ui.page.MainScreen
import com.github.avbox.osc.ui.theme.AVBoxTheme
import com.github.avbox.osc.ui.theme.AppThemeState
import com.github.avbox.osc.ui.page.UpdateCheckDialog
import com.github.avbox.osc.ui.page.WelcomeScreen
import com.github.avbox.osc.util.LanguageManager
import com.github.avbox.osc.util.PermissionHelper
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
