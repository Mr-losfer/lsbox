package com.github.avbox.osc.util

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.github.avbox.osc.R
import com.github.avbox.osc.util.KV

enum class UyghurFont(val assetName: String, val displayName: String) {
    UkijTuz("ukij_tuz.ttf", "UKIJ Tuz"),
    UkijTuzK("ukij_tuz_k.ttf", "UKIJ Tuz K"),
    UkijTuzG("ukij_tuz_g.ttf", "UKIJ Tuz G"),
    UkijTuzQ("ukij_tuz_q.ttf", "UKIJ Tuz Q"),
    UkijBasma("ukij_basma.ttf", "UKIJ Basma"),
    AlkatipAsliya("alkatip_asliya.ttf", "ALKATIP Asliya"),
    AlkatipBasma("alkatip_basma.ttf", "ALKATIP Basma"),
    AlkatipKitab("alkatip_kitab.ttf", "ALKATIP Kitab"),
    AlkatipTor("alkatip_tor.ttf", "ALKATIP Tor"),
    AlpsoftBasma("alpsoft_basma.ttf", "Alpsoft Basma"),
}

val LocalUyghurFontFamily = staticCompositionLocalOf { FontFamily.Default }

object UyghurFontManager {

    private const val KEY = "uyghur_font"

    @Volatile
    private var cachedFamily: FontFamily? = null

    @Volatile
    private var cachedTypeface: Typeface? = null

    fun current(): UyghurFont {
        val stored = try {
            KV.get(KEY, UyghurFont.UkijTuz.name)
        } catch (ignored: IllegalStateException) {
            UyghurFont.UkijTuz.name
        }
        return UyghurFont.entries.firstOrNull { it.name == stored } ?: UyghurFont.UkijTuz
    }

    fun set(font: UyghurFont) {
        KV.put(KEY, font.name)
        cachedFamily = null
        cachedTypeface = null
    }

    fun available(): List<UyghurFont> = UyghurFont.entries

    fun fontFamily(): FontFamily {
        cachedFamily?.let { return it }
        val font = current()
        val family = FontFamily(
            Font(resId = fontResId(font), weight = FontWeight.Normal),
        )
        cachedFamily = family
        return family
    }

    fun typeface(context: Context): Typeface {
        cachedTypeface?.let { return it }
        val tf = try {
            Typeface.createFromAsset(context.assets, "fonts/${current().assetName}")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
        cachedTypeface = tf
        return tf
    }

    fun previewTypeface(context: Context, font: UyghurFont): Typeface {
        return try {
            Typeface.createFromAsset(context.assets, "fonts/${font.assetName}")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun fontResId(font: UyghurFont): Int {
        return when (font) {
            UyghurFont.UkijTuz -> R.font.ukij_tuz
            UyghurFont.UkijTuzK -> R.font.ukij_tuz_k
            UyghurFont.UkijTuzG -> R.font.ukij_tuz_g
            UyghurFont.UkijTuzQ -> R.font.ukij_tuz_q
            UyghurFont.UkijBasma -> R.font.ukij_basma
            UyghurFont.AlkatipAsliya -> R.font.alkatip_asliya
            UyghurFont.AlkatipBasma -> R.font.alkatip_basma
            UyghurFont.AlkatipKitab -> R.font.alkatip_kitab
            UyghurFont.AlkatipTor -> R.font.alkatip_tor
            UyghurFont.AlpsoftBasma -> R.font.alpsoft_basma
        }
    }
}
