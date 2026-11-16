package can.losfer.lsbox.ui

import androidx.activity.compose.setContent
import can.losfer.lsbox.R
import can.losfer.lsbox.ui.base.BaseActivity
import can.losfer.lsbox.ui.components.SheetHostScaffold
import can.losfer.lsbox.ui.theme.AVBoxTheme
import can.losfer.lsbox.ui.theme.enableTransparentEdgeToEdge

class SearchActivity : BaseActivity() {

    override fun shouldRefreshAutoSize(): Boolean = true

    override fun hideSysBar() {
    }

    override fun init() {
        enableTransparentEdgeToEdge()
        findViewById<androidx.compose.ui.platform.ComposeView>(R.id.compose_view).setContent {
            AVBoxTheme {
                SheetHostScaffold {
                    SearchScreen()
                }
            }
        }
    }
}
