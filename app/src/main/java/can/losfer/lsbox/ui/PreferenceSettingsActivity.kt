package can.losfer.lsbox.ui

import android.content.Context
import android.content.Intent
import can.losfer.lsbox.ui.theme.enableTransparentEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import can.losfer.lsbox.R
import can.losfer.lsbox.ui.base.BaseActivity
import can.losfer.lsbox.ui.components.SheetHostScaffold
import can.losfer.lsbox.ui.page.PreferenceSettingsScreen
import can.losfer.lsbox.ui.theme.AVBoxTheme

class PreferenceSettingsActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, PreferenceSettingsActivity::class.java))
        }
    }

    override fun shouldRefreshAutoSize(): Boolean = true

    override fun hideSysBar() {
    }

    override fun init() {
        enableTransparentEdgeToEdge()
        findViewById<ComposeView>(R.id.compose_view).setContent {
            AVBoxTheme {
                SheetHostScaffold {
                    PreferenceSettingsScreen(onNavigateBack = { finish() })
                }
            }
        }
    }
}
