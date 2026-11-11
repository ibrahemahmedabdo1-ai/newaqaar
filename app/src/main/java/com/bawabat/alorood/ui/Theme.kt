package com.bawabat.alorood.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

// ألوان مأخوذة من ملف Figma (Real-state)
val Cream = Color(0xFFFFF8EE)
val Peach = Color(0xFFFFEDD3)
val Ink = Color(0xFF001716)
val Gold = Color(0xFFF2C200)
val Gray1 = Color(0xFF3D3C3C)
val Gray2 = Color(0xFF656565)
val Danger = Color(0xFFFF3333)
val Brown = Color(0xFF614832)
val LineGray = Color(0xFFDEDEDE)
val Sky = Color(0xFFE6F2FF)

internal val LightScheme = lightColorScheme(
    primary = Ink, onPrimary = Cream,
    secondary = Gold, onSecondary = Ink,
    background = Cream, onBackground = Gray1,
    surface = Cream, onSurface = Gray1,
    surfaceVariant = Peach, onSurfaceVariant = Gray2,
    error = Danger, outline = LineGray
)

internal val DarkScheme = darkColorScheme(
    primary = Gold, onPrimary = Ink,
    secondary = Gold, onSecondary = Ink,
    background = Ink, onBackground = Color.White,
    surface = Color(0xFF0B2623), onSurface = Color.White,
    surfaceVariant = Color(0xFF12332F), onSurfaceVariant = Color(0xFFCFCFCF),
    error = Danger, outline = Color(0xFF2B4A46)
)

val LocalLang = staticCompositionLocalOf { "ar" }

/** نص ثنائي اللغة: t("عربي", "English") */
@Composable
fun t(ar: String, en: String): String = if (LocalLang.current == "ar") ar else en

@Composable
fun BawabatTheme(lang: String, themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val dir = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLang provides lang, LocalLayoutDirection provides dir) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}
