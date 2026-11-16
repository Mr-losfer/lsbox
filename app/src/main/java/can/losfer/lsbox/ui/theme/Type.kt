package can.losfer.lsbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AVBoxTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
)


/**
 * 生成一版应用了指定字体的 Typography：把每个 TextStyle 的 fontFamily 设为给定字体。
 * 用于维吾尔语/哈萨克语字体全局生效。
 */
fun typographyWithFont(base: Typography, fontFamily: FontFamily): Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
    displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
    displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
    titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
    titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
    labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
    labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
    labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
)
