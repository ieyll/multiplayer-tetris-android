package com.example.tetrisprojem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Brush // Gradyan için eklendi
import androidx.compose.ui.text.font.FontWeight // Font ağırlığı için eklendi
import androidx.compose.ui.unit.sp // Font boyutu için eklendi
import androidx.compose.foundation.shape.RoundedCornerShape // Yuvarlak köşeler için eklendi
import androidx.compose.ui.draw.clip // Yuvarlak köşeleri uygulamak için eklendi
import androidx.compose.ui.draw.shadow // Gölge efekti için eklendi
import androidx.compose.ui.text.input.PasswordVisualTransformation // Şifre gizleme için eklendi
import androidx.compose.material3.ButtonDefaults // ButtonDefaults için eklendi
import androidx.compose.material3.CircularProgressIndicator // Yükleme göstergesi için eklendi
import androidx.compose.material3.TextFieldDefaults // TextField renkleri için eklendi
import androidx.compose.ui.text.style.TextAlign // Metin hizalaması için eklendi

// ui.theme paketindeki özel renkleri import ediyoruz
import com.example.tetrisprojem.ui.theme.AccentPrimary
import com.example.tetrisprojem.ui.theme.BackgroundDeep
import com.example.tetrisprojem.ui.theme.DarkBackgroundLightStart
import com.example.tetrisprojem.ui.theme.ErrorRed
import com.example.tetrisprojem.ui.theme.RichLila
import com.example.tetrisprojem.ui.theme.RichLilaLightStart
import com.example.tetrisprojem.ui.theme.TextSoftWhite
import com.example.tetrisprojem.ui.theme.minus // Renk uzantı fonksiyonu için


@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val auth = remember { FirebaseAuth.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    val backgroundBrush = Brush.verticalGradient(
        listOf(DarkBackgroundLightStart, BackgroundDeep)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Tetris'e Hoş Geldiniz!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextSoftWhite,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // E-posta TextField
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta", color = TextSoftWhite.copy(alpha = 0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextSoftWhite,
                unfocusedTextColor = TextSoftWhite,
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                cursorColor = AccentPrimary,
                focusedIndicatorColor = AccentPrimary,
                unfocusedIndicatorColor = TextSoftWhite.copy(alpha = 0.5f),
                focusedLabelColor = AccentPrimary,
                unfocusedLabelColor = TextSoftWhite.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Şifre TextField
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre", color = TextSoftWhite.copy(alpha = 0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(), // Şifreyi gizle
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextSoftWhite,
                unfocusedTextColor = TextSoftWhite,
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                cursorColor = AccentPrimary,
                focusedIndicatorColor = AccentPrimary,
                unfocusedIndicatorColor = TextSoftWhite.copy(alpha = 0.5f),
                focusedLabelColor = AccentPrimary,
                unfocusedLabelColor = TextSoftWhite.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Giriş Yap Butonu
        GradientButton(
            text = "Giriş Yap",
            gradientColors = listOf(AccentPrimary, AccentPrimary.minus(Color(0.2f,0.2f,0.2f))),
            onClick = {
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        auth.signInWithEmailAndPassword(email, password).await()
                        onLoginSuccess(auth.currentUser!!.uid)
                    } catch (e: Exception) {
                        errorMessage = "Giriş hatası: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Kaydol Butonu
        GradientButton(
            text = "Kaydol",
            gradientColors = listOf(RichLilaLightStart, RichLila.minus(Color(0.1f,0.1f,0.1f))),
            onClick = {
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        auth.createUserWithEmailAndPassword(email, password).await()
                        onLoginSuccess(auth.currentUser!!.uid)
                    } catch (e: Exception) {
                        errorMessage = "Kaydolma hatası: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Misafir Olarak Oyna Butonu
        GradientButton(
            text = "Misafir Olarak Oyna",
            gradientColors = listOf(Color.Gray.copy(alpha = 0.7f), Color.DarkGray.copy(alpha = 0.8f)),
            onClick = {
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        auth.signInAnonymously().await()
                        onLoginSuccess(auth.currentUser!!.uid)
                    } catch (e: Exception) {
                        errorMessage = "Misafir girişi hatası: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        )

        // Yükleme göstergesi
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = AccentPrimary)
        }

        // Hata mesajı
        errorMessage?.let {
            Text(
                it,
                color = ErrorRed, // Hata rengi
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center, // Metni ortala
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginSuccess = {})
}