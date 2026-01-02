package com.example.tetrisprojem.ui.theme

import androidx.compose.ui.graphics.Color

// Light/Dark Theme için varsayılan renkler (Material3 standardına göre) - EKSİK OLANLAR EKLENDİ
val SoftPrimary = Color(0xFFBB86FC) // Yumuşak Mor
val SoftSecondary = Color(0xFF03DAC5) // Yumuşak Turkuaz
val SoftTertiary = Color(0xFFEFB8C8) // Yumuşak Pembe

val DarkBackground = Color(0xFF09111F) // Logonun koyu arka planı
val DarkSurface = Color(0xFF1E1E1E) // Koyu gri yüzeyler için (biraz daha açık)
val OnDarkPrimary = Color.White
val OnDarkSecondary = Color.Black // Turkuaz üzerinde siyah metin daha iyi olabilir
val OnDarkTertiary = Color.White
val OnDarkBackground = Color.White
val OnDarkSurface = Color.White

// Açık tema için soft renkler (eksik olanlar eklendi)
val LightPrimary = Color(0xFF6200EE) // Canlı Mor
val LightSecondary = Color(0xFF018786) // Canlı Turkuaz
val LightTertiary = Color(0xFF985EE0) // Canlı Pembe

val LightBackground = Color(0xFFFFFBFE)
val LightSurface = Color(0xFFFFFBFE)
val OnLightPrimary = Color.White
val OnLightSecondary = Color.White
val OnLightTertiary = Color.White
val OnLightBackground = Color(0xFF1C1B1F)
val OnLightSurface = Color(0xFF1C1B1F)

// Blok renkleri için özel tanımlamalar (daha soft tonlar)
val BlockCyan = Color(0xFF80DEEA) // Açık Cyan
val BlockYellow = Color(0xFFFFEE58) // Açık Sarı
val BlockPurple = Color(0xFFCE93D8) // Açık Mor
val BlockGreen = Color(0xFFA5D6A7) // Açık Yeşil
val BlockRed = Color(0xFFFFCDD2) // Açık Kırmızı
val BlockBlue = Color(0xFF90CAF9) // Açık Mavi
val BlockOrange = Color(0xFFFFCC80) // Açık Turuncu

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// DarkBackgroundLightStart'ın mevcut tanımlaması (0xFF1E2A3A) korunuyor.
val DarkBackgroundLightStart = Color(0xFF1E2A3A)

val RichLilaLightStart = Color(0xFFAB82FF)


// Yeni Tasarım Renkleri (Mevcut halleriyle korunuyor)
val BackgroundDeep = Color(0xFF09111F) // Ana arka plan
val AccentPrimary = Color(0xFF00BCD4) // Canlı Turkuaz Mavisi
val AccentSecondary = Color(0xFFFFC107) // Canlı Amber Sarısı
val TextSoftWhite = Color(0xFFE0E0E0) // Yumuşak Beyaz Metin
val CardSurface = Color(0xFF1A232E) // Kartlar ve diğer yüzeyler için
val ErrorRed = Color(0xFFEF5350) // Hata mesajları

// Yeni Eklenecek Renkler (Mevcut halleriyle korunuyor)
val DeepPurple = Color(0xFF6A0DAD) // Daha koyu, zengin mor (arka plan gradyanı için)
val DarkerDeepPurple = Color(0xFF4A008A) // DeepPurple'dan daha koyu (arka plan gradyanı için)
val RichLila = Color(0xFF9370DB) // Tek oyuncu butonu için daha zengin lila

// Color sınıfı için uzantı fonksiyonları
// YALNIZCA BU minus FONKSİYONU KALIYOR. DİĞER MİNUS TANIMLARI KALDIRILDI.
operator fun Color.minus(other: Color): Color {
    return Color(
        red = (this.red - other.red).coerceIn(0f, 1f),
        green = (this.green - other.green).coerceIn(0f, 1f),
        blue = (this.blue - other.blue).coerceIn(0f, 1f),
        alpha = alpha // Alfa değeri değişmez
    )
}
// operator fun Color.plus(other: Color): Color (Bu zaten vardı ve bir sorun yaratmıyor, korunuyor)
operator fun Color.plus(other: Color): Color {
    return Color(
        red = (red + other.red).coerceIn(0f, 1f),
        green = (green + other.green).coerceIn(0f, 1f),
        blue = (blue + other.blue).coerceIn(0f, 1f),
        alpha = alpha // Alfa değeri değişmez
    )
}

// Lütfen diğer 'fun Color.minus(...)' tanımlarını bu dosyadan manuel olarak kaldırın.
// Yalnızca yukarıdaki "operator fun Color.minus(other: Color): Color" tanımı kalmalı.