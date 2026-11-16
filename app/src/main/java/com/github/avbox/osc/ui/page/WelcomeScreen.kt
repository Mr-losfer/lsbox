package com.github.avbox.osc.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.avbox.osc.util.AppLanguage
import com.github.avbox.osc.util.LanguageManager
import kotlinx.coroutines.delay

private data class Greeting(val text: String, val lang: String)

private val greetings = listOf(
    Greeting("ياخشىمۇسىز", "ug"),
    Greeting("سلام", "ug"),
    Greeting("Hello", "en"),
    Greeting("你好", "zh"),
    Greeting("Salam", "kk"),
    Greeting("سالام", "kk"),
    Greeting("Bonjour", "fr"),
    Greeting("Hola", "es"),
    Greeting("こんにちは", "ja"),
    Greeting("안녕하세요", "ko"),
    Greeting("Привет", "ru"),
    Greeting("Hallo", "de"),
    Greeting("Ciao", "it"),
    Greeting("Olá", "pt"),
    Greeting("مرحبا", "ar"),
)

private data class LanguageOption(
    val language: AppLanguage,
    val displayName: String,
    val nativeName: String,
)

private val languageOptions = listOf(
    LanguageOption(AppLanguage.Uyghur, "Uyghur", "ئۇيغۇرچە"),
    LanguageOption(AppLanguage.Kazakh, "Kazakh", "قازاقچا"),
    LanguageOption(AppLanguage.SimplifiedChinese, "Chinese", "简体中文"),
    LanguageOption(AppLanguage.English, "English", "English"),
    LanguageOption(AppLanguage.TraditionalTW, "Chinese (TW)", "繁體(台灣)"),
    LanguageOption(AppLanguage.TraditionalHK, "Chinese (HK)", "繁體(香港)"),
    LanguageOption(AppLanguage.System, "System default", "سېستېما"),
)

@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    var slideIndex by remember { mutableIntStateOf(0) }
    var showPicker by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (showPicker == 0) {
            delay(900)
            slideIndex = (slideIndex + 1) % greetings.size
        }
    }

    val transition = rememberInfiniteTransition(label = "hello")
    val glow by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (showPicker == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "LSBox",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = glow),
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        greetings.forEachIndexed { index, g ->
                            androidx.compose.animation.AnimatedVisibility(
                                visible = index == slideIndex,
                                enter = fadeIn(tween(450)),
                                exit = fadeOut(tween(450)),
                            ) {
                                Text(
                                    text = g.text,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Light,
                                        fontSize = 38.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = "↓",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showPicker = 1 }
                            .padding(16.dp),
                    )
                }
            }
        } else {
            LanguagePickerContent(onSelected = { lang ->
                LanguageManager.set(lang)
                LanguageManager.markPicked()
                onFinished()
            })
        }
    }
}

@Composable
private fun LanguagePickerContent(onSelected: (AppLanguage) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            text = "تىل تاللاڭ / 选择语言",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Language",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(languageOptions) { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onSelected(option.language) }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.nativeName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
