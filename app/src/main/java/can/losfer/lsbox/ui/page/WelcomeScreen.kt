package can.losfer.lsbox.ui.page

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import can.losfer.lsbox.R
import can.losfer.lsbox.ui.components.SettingsCardPosition
import can.losfer.lsbox.util.AppLanguage
import can.losfer.lsbox.util.LanguageManager
import kotlinx.coroutines.delay

private data class Greeting(val text: String)

private val greetings = listOf(
    Greeting("ياخشىمۇسىز"),
    Greeting("Hello"),
    Greeting("你好"),
    Greeting("سالام"),
    Greeting("Salam"),
    Greeting("Bonjour"),
    Greeting("Hola"),
)

private data class LanguageOption(
    val language: AppLanguage,
    val nativeName: String,
    val subName: String,
    val icon: ImageVector,
)

private val languageOptions = listOf(
    LanguageOption(AppLanguage.Uyghur, "ئۇيغۇرچە", "Uyghur", Icons.Filled.Translate),
    LanguageOption(AppLanguage.Kazakh, "قازاقشا", "Kazakh (Xinjiang)", Icons.Filled.Language),
    LanguageOption(AppLanguage.SimplifiedChinese, "简体中文", "Chinese (Simplified)", Icons.Filled.Public),
    LanguageOption(AppLanguage.English, "English", "English", Icons.Filled.Language),
    LanguageOption(AppLanguage.System, "سېستېما تىلى", "System default", Icons.Filled.Settings),
)

/** 与 SettingsCard 一致的卡片圆角：首项上圆角、中间首尾直角、末项下圆角 */
private fun cardShape(position: SettingsCardPosition): RoundedCornerShape = when (position) {
    SettingsCardPosition.SINGLE -> RoundedCornerShape(32.dp)
    SettingsCardPosition.FIRST -> RoundedCornerShape(
        topStart = 32.dp, topEnd = 32.dp, bottomEnd = 4.dp, bottomStart = 4.dp,
    )
    SettingsCardPosition.MIDDLE -> RoundedCornerShape(4.dp)
    SettingsCardPosition.LAST -> RoundedCornerShape(
        topStart = 4.dp, topEnd = 4.dp, bottomEnd = 32.dp, bottomStart = 32.dp,
    )
}

private fun positionOf(index: Int, size: Int): SettingsCardPosition = when {
    size <= 1 -> SettingsCardPosition.SINGLE
    index == 0 -> SettingsCardPosition.FIRST
    index == size - 1 -> SettingsCardPosition.LAST
    else -> SettingsCardPosition.MIDDLE
}

/**
 * 首启语言引导页 —— 顶部 iPhone Hello 风格欢迎区 + 底部 Material 3 分组卡片语言列表。
 *
 * 视觉与主界面保持同一套主题：背景用纯 surface，LOGO 单色化跟随主题色，
 * 语言卡片底色使用主题容器色。
 */
@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableIntStateOf(-1) }

    var slideIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1100)
            slideIndex = (slideIndex + 1) % greetings.size
        }
    }

    val transition = rememberInfiniteTransition(label = "welcome-glow")
    val glow by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    val scheme = MaterialTheme.colorScheme

    Surface(modifier = Modifier.fillMaxSize(), color = scheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ===== 顶部 iPhone 风格欢迎区（与主题同底色，无彩色块）=====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.44f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(scheme.surfaceContainerHigh)
                            .graphicsLayer { scaleX = glow; scaleY = glow },
                        contentAlignment = Alignment.Center,
                    ) {
                        // 单色 LOGO：跟随主题主色，不再显示彩色原图
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(scheme.primary, BlendMode.SrcIn),
                            modifier = Modifier.size(82.dp),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "LSBox",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                        ),
                        color = scheme.onSurface,
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = slideIndex,
                            transitionSpec = {
                                (fadeIn(tween(500)) + slideInVertically { 16 }) togetherWith
                                    (fadeOut(tween(500)) + slideOutVertically { -16 })
                            },
                            label = "hello",
                        ) { idx ->
                            Text(
                                text = greetings[idx].text,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 28.sp,
                                ),
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        greetings.indices.forEach { i ->
                            val active = i == slideIndex
                            Box(
                                modifier = Modifier
                                    .size(if (active) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) scheme.primary
                                        else scheme.onSurfaceVariant.copy(alpha = 0.28f)
                                    ),
                            )
                        }
                    }
                }
            }

            // ===== 选择语言标题 =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.45f))
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "تىل تاللاڭ  ·  Choose Language",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "ئۆزىڭىزگە ماس كېلىدىغان تىلنى تاللاڭ",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
            }

            // ===== 底部语言列表（分组卡片：上下圆角、中间拼接、主题色底）=====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.56f)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                languageOptions.forEachIndexed { index, option ->
                    val isSelected = selected == index
                    val shape = cardShape(positionOf(index, languageOptions.size))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(
                                if (isSelected) {
                                    scheme.primary
                                } else {
                                    scheme.primaryContainer.copy(alpha = 0.55f)
                                }
                            )
                            .clickable {
                                selected = index
                                LanguageManager.set(option.language)
                                LanguageManager.markPicked()
                                (context as? Activity)?.recreate() ?: onFinished()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        scheme.onPrimary.copy(alpha = 0.20f)
                                    } else {
                                        scheme.primary.copy(alpha = 0.16f)
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) scheme.onPrimary else scheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.nativeName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) scheme.onPrimary else scheme.onPrimaryContainer,
                            )
                            Text(
                                text = option.subName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) {
                                    scheme.onPrimary.copy(alpha = 0.82f)
                                } else {
                                    scheme.onPrimaryContainer.copy(alpha = 0.72f)
                                },
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
