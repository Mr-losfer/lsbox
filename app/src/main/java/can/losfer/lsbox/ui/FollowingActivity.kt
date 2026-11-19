package can.losfer.lsbox.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.ComposeView
import can.losfer.lsbox.R
import can.losfer.lsbox.ui.base.BaseActivity
import can.losfer.lsbox.ui.components.SheetHostScaffold
import can.losfer.lsbox.ui.page.FollowingPage
import can.losfer.lsbox.ui.theme.AVBoxTheme
import can.losfer.lsbox.ui.theme.enableTransparentEdgeToEdge

/**
 * 独立的「追剧」页面。
 *
 * v1.9.1 起「追剧」不再占用 TabBar，改为从「我的」页与分类页顶栏进入。
 */
class FollowingActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, FollowingActivity::class.java))
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
                    FollowingPage(onNavigateBack = { finish() })
                }
            }
        }
    }
}
