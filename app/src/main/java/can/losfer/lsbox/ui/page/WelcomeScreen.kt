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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import can.losfer.lsbox.R
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
    Greeting("こんにちは"),
    Greeting("مرحبا"),
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

/**
 * 首启语言引导页 —— iPhone Hello 风格欢迎区 + Material 3 语言列表。
 * 顶部（状态栏）+ 底部（导航栏）安全距离均已处理。
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
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
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
            // ===== 顶部 iPhone 风格欢迎区 =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                scheme.primaryContainer.copy(alpha = 0.55f),
                                scheme.surface,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .background(scheme.surfaceBright)
                            .graphicsLayer { scaleX = glow; scaleY = glow },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "LSBox",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                        ),
                        color = scheme.primary,
                    )

                    Spacer(Modifier.height(10.dp))

                    // iPhone hello 风格多语言问候轮播
                    Box(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = slideIndex,
                            transitionSpec = {
                                (fadeIn(tween(500)) + slideInVertically { 18 }) togetherWith
                                    (fadeOut(tween(500)) + slideOutVertically { -18 })
                            },
                            label = "hello",
                        ) { idx ->
                            Text(
                                text = greetings[idx].text,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 30.sp,
                                ),
                                color = scheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 轮播指示圆点
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        greetings.indices.forEach { i ->
                            val active = i == slideIndex
                            Box(
                                modifier = Modifier
                                    .size(if (active) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) scheme.primary
                                        else scheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    ),
                            )
                        }
                    }
                }
            }

            // ===== 选择语言标题 =====
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "تىل تاللاڭ  ·  Choose Language",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ئۆزىڭىزگە ماس كېلىدىغان تىلنى تاللاڭ",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
            }

            // ===== 底部语言列表 =====
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.58f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(languageOptions) { index, option ->
                    val isSelected = selected == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) scheme.primaryContainer else scheme.surfaceContainerLow
                            )
                            .clickable {
                                selected = index
                                LanguageManager.set(option.language)
                                LanguageManager.markPicked()
                                // 重建 Activity：BaseActivity.attachBaseContext 用新 locale 包裹 → 语言即时生效
                                (context as? Activity)?.recreate() ?: onFinished()
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) scheme.primary
                                    else scheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) scheme.onPrimary else scheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.nativeName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                            )
                            Text(
                                text = option.subName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected)
                                    scheme.onPrimaryContainer.copy(alpha = 0.75f)
                                else scheme.onSurfaceVariant,
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
