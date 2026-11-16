package can.losfer.lsbox.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * 在从右向左（RTL，如维吾尔语、哈萨克语）布局下水平镜像当前内容。
 *
 * 用于返回箭头等有方向性的图标：RTL 下布局会自动翻转，但图标本身
 * 仍指向左侧会造成方向矛盾，因此需要同步镜像。
 */
fun Modifier.mirrorInRtl(): Modifier = composed {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    if (rtl) this.graphicsLayer { scaleX = -1f } else this
}

/** 当前是否处于 RTL 布局 */
@androidx.compose.runtime.Composable
fun isRtlLayout(): Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl
