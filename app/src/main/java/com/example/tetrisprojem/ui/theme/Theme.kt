package com.example.tetrisprojem.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,      // Canlı Turkuaz Mavisi (yeni tasarım rengi)
    secondary = AccentSecondary,  // Canlı Amber Sarısı (yeni tasarım rengi)
    tertiary = SoftTertiary,      // Mevcut SoftTertiary'yi koruduk
    background = BackgroundDeep,  // Ana arka planınız (yeni tasarım rengi)
    surface = CardSurface,        // Kartlar ve yüzeyler için (yeni tasarım rengi)
    onPrimary = Color.White,      // AccentPrimary üzerinde beyaz metin
    onSecondary = Color.Black,    // AccentSecondary üzerinde siyah metin (daha iyi kontrast için)
    onTertiary = OnDarkTertiary,  // Mevcut OnDarkTertiary'yi koruduk
    onBackground = TextSoftWhite, // BackgroundDeep üzerinde yumuşak beyaz metin (yeni tasarım rengi)
    onSurface = TextSoftWhite,    // CardSurface üzerinde yumuşak beyaz metin (yeni tasarım rengi)
    surfaceVariant = CardSurface, // Kartlar ve benzeri yüzeyler için CardSurface'ı kullanıyoruz
    error = ErrorRed,             // Hata mesajları için (yeni tasarım rengi)
    onError = Color.White         // ErrorRed üzerinde beyaz metin
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = OnLightPrimary,
    onSecondary = OnLightSecondary,
    onTertiary = OnLightTertiary,
    onBackground = OnLightBackground,
    onSurface = OnLightSurface
)

@Composable
fun TetrisProjemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // Dinamik renk desteği korunuyor
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Typography'nin doğru import edildiğinden emin olun (Type.kt'den)
        content = content
    )
}