package com.example.tetrisprojem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tetrisprojem.data.GameRoom
import com.example.tetrisprojem.data.GameLevels
import com.example.tetrisprojem.data.Level
import com.example.tetrisprojem.repository.GameRepository
import com.example.tetrisprojem.ui.theme.BackgroundDeep
import com.example.tetrisprojem.ui.theme.AccentPrimary
import com.example.tetrisprojem.ui.theme.AccentSecondary
import com.example.tetrisprojem.ui.theme.TextSoftWhite
import com.example.tetrisprojem.ui.theme.CardSurface
import com.example.tetrisprojem.ui.theme.ErrorRed
import com.example.tetrisprojem.ui.theme.minus
import com.example.tetrisprojem.ui.theme.RichLila
import com.example.tetrisprojem.ui.theme.RichLilaLightStart
import com.example.tetrisprojem.ui.theme.DarkBackgroundLightStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.unit.offset // offset modifier'ı için import
import com.google.firebase.auth.FirebaseAuth


@Composable
fun MultiplayerGameLobby(
    userId: String,
    gameRepository: GameRepository,
    coroutineScope: CoroutineScope,
    onRoomCreated: (roomId: String) -> Unit,
    onRoomJoined: (roomId: String, isPlayer1: Boolean) -> Unit,
    onSinglePlayerClick: (Level) -> Unit
) {
    val waitingRooms by gameRepository.observeWaitingRooms().collectAsState(initial = emptyList())

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedLevel by remember { mutableStateOf(GameLevels.BEGINNER_LEVEL) } // Varsayılan olarak başlangıç seviyesi

    //DropdownMenu'nun açılıp kapanmasını kontrol eden state
    var showMenu by remember { mutableStateOf(false) }
    val auth = remember { FirebaseAuth.getInstance() } // FirebaseAuth instance'ı, remember ile sabit tutulur

    // Tüm içeriği kapsayan ana Box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBackgroundLightStart, BackgroundDeep))) // Daha açık başlangıçlı gradyan
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp), // Yatayda padding sabit kalsın
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Elemanları üstten hizala, manuel boşlukları kullanacağız
        ) {
            // İkon ve menü için boşluk bırak, aksi takdirde üstteki metinle çakışabilir
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Tetris Lobisi",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSoftWhite
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Seviye Seçimi UI'ı
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Seviye Seçimi:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextSoftWhite.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    GameLevels.ALL_LEVELS.forEach { levelOption ->
                        Button(
                            onClick = { selectedLevel = levelOption },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedLevel.id == levelOption.id) AccentPrimary else CardSurface.copy(alpha = 0.5f),
                                contentColor = if (selectedLevel.id == levelOption.id) Color.White else TextSoftWhite
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(levelOption.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Yeni Oda Oluştur Butonu
            GradientButton(
                text = "Yeni Oda Oluştur (Çok Oyunculu)",
                gradientColors = listOf(AccentPrimary, AccentPrimary.minus(Color(0.2f,0.2f,0.2f))),
                onClick = {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = gameRepository.createGameRoom(userId, selectedLevel)
                        result.onSuccess { roomId ->
                            onRoomCreated(roomId)
                        }.onFailure { e ->
                            errorMessage = "Oda oluşturulamadı: ${e.message}"
                            println("Oda oluşturma hatası: ${e.message}")
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))


            GradientButton(
                text = "Misafir Olarak Oyna (Tek Oyunculu)",
                gradientColors = listOf(RichLilaLightStart, RichLila.minus(Color(0.1f,0.1f,0.1f))), // Daha açık başlangıçlı lila gradyanı
                onClick = {
                    onSinglePlayerClick(selectedLevel)
                },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Mevcut Odalar Listesi
            Text(
                text = "Katılabileceğin Odalar",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextSoftWhite
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(color = AccentPrimary)
            } else if (errorMessage != null) {
                Text(
                    "Hata: $errorMessage",
                    color = ErrorRed,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else if (waitingRooms.isEmpty()) {
                Text(
                    "Şu an katılabilecek boş oda yok. Bir tane oluştur!",
                    color = TextSoftWhite.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface.copy(alpha = 0.5f))
                        .border(1.dp, AccentPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(waitingRooms) { room ->
                        RoomItem(room = room, userId = userId) { selectedRoom ->
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = gameRepository.joinGameRoom(selectedRoom.roomId, userId)
                                result.onSuccess {
                                    onRoomJoined(selectedRoom.roomId, false)
                                }.onFailure { e ->
                                    errorMessage = "Odaya katılamadı: ${e.message}"
                                    println("Odaya katılma hatası: ${e.message}")
                                }
                                isLoading = false
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp)) // Ekranın altında boşluk
        }


        Box(
            modifier = Modifier
                .align(Alignment.TopEnd) // Ana Box'ın sağ üst köşesine hizala
                .padding(top = 40.dp, end = 25.dp) // Ekran kenarından biraz boşluk bırak
        ) {
            IconButton(
                onClick = { showMenu = !showMenu },
                modifier = Modifier.size(48.dp) // İkon boyutunu ayarla
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profil",
                    tint = TextSoftWhite // İkon rengini arka planla uyumlu hale getirdim
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }

            ) {
                DropdownMenuItem(
                    text = { Text("Çıkış Yap", color = Color.White) },
                    onClick = {
                        auth.signOut() // Firebase'den çıkış yap
                        showMenu = false // Menüyü kapat
                        // MainActivity'deki AuthStateListener bu değişikliği algılayacak ve LoginScreen'e yönlendirecektir.
                    }
                )
            }
        }
    }
}

@Composable
fun RoomItem(room: GameRoom, userId: String, onJoinClick: (GameRoom) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = room.player1Id != userId && room.player2Id == null) {
                onJoinClick(room)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Oda ID: ${room.roomId.substring(0, 6)}...",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSoftWhite
                )
            )
            Text(
                "Oluşturan: ${room.player1Id?.substring(0, 5)}...",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSoftWhite.copy(alpha = 0.7f))
            )
            Text(
                "Seviye: ${room.level.name}",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSoftWhite.copy(alpha = 0.7f))
            )
            val statusText = when {
                room.player1Id == userId -> "Durum: Benim Odam"
                room.player2Id == null -> "Durum: Oyuncu bekleniyor..."
                room.player2Id == userId -> "Durum: Katıldın!"
                else -> "Durum: Başlamış veya Dolu"
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (room.player2Id == null) AccentSecondary else if (room.player1Id == userId || room.player2Id == userId) AccentPrimary else ErrorRed
                ),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(56.dp)
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.5f)),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Preview(showBackground = true)
@Composable
fun MultiplayerGameLobbyPreview() {
    val previewCoroutineScope = rememberCoroutineScope()

    MultiplayerGameLobby(
        userId = "testUser",
        gameRepository = GameRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance()),
        coroutineScope = previewCoroutineScope,
        onRoomCreated = {  },
        onRoomJoined = { _, _ ->  },
        onSinglePlayerClick = { }
    )
}