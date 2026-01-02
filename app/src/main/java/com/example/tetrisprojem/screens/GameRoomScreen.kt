package com.example.tetrisprojem.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tetrisprojem.repository.GameRepository
import com.example.tetrisprojem.data.PlayerState
import com.example.tetrisprojem.data.GameRoom
import com.example.tetrisprojem.data.Board
import com.example.tetrisprojem.data.BlockState
import com.example.tetrisprojem.logic.convert1DTo2D
import com.example.tetrisprojem.logic.convert2DTo1D
import com.example.tetrisprojem.logic.getBlockShapeById
import com.example.tetrisprojem.logic.BOARD_ROWS
import com.example.tetrisprojem.logic.BOARD_COLS
import androidx.compose.material3.MaterialTheme
import com.example.tetrisprojem.ui.theme.TetrisGameScreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch
import com.example.tetrisprojem.data.GameResult
import com.example.tetrisprojem.data.GameLevels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.times
import kotlinx.coroutines.flow.first

@Composable
fun GameRoomScreen(
    roomId: String,
    currentUserId: String,
    isPlayer1: Boolean,
    gameRepository: GameRepository,
    onGameEnd: () -> Unit
) {
    val gameRoom by gameRepository.observeGameRoom(roomId).collectAsState(initial = null)

    var localPlayerState by remember { mutableStateOf<PlayerState?>(null) }
    var opponentPlayerState by remember { mutableStateOf<PlayerState?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val currentMultiplayerLevel = remember(gameRoom) {
        gameRoom?.level ?: GameLevels.MULTIPLAYER_LEVEL
    }
    val sharedBlockSequence = gameRoom?.blockSequence ?: emptyList()

    var isGameReallyOver by remember { mutableStateOf(false) }

    LaunchedEffect(gameRoom) {
        gameRoom?.let { room ->

            if (room.status == "finished") {
                isGameReallyOver = true
                return@LaunchedEffect
            }

            if (isPlayer1) {
                localPlayerState = room.player1State
                opponentPlayerState = room.player2State
            } else {
                localPlayerState = room.player2State
                opponentPlayerState = room.player1State
            }


            if (room.player1Id != null && room.player2Id != null && room.status == "waiting") {

                if (isPlayer1) {
                    coroutineScope.launch {
                        gameRepository.updateGameRoomStatus(roomId, "in_game")
                            .onSuccess {
                                println("Oyun durumu 'in_game' olarak güncellendi.")
                            }
                            .onFailure { e ->
                                println("Oyun durumu güncelleme hatası: ${e.message}")
                            }
                    }
                }
            }
        }
    }


    if (gameRoom?.status == "in_game" && localPlayerState != null && sharedBlockSequence.isNotEmpty()) {
        TetrisGameScreen(
            level = currentMultiplayerLevel,
            initialPlayerState = localPlayerState,
            blockSequence = sharedBlockSequence,
            onPlayerStateChange = { updatedPlayerState ->
                coroutineScope.launch {
                    gameRepository.updatePlayerState(
                        roomId,
                        isPlayer1,
                        updatedPlayerState
                    ).onFailure { e ->
                        println("Firestore'a playerState güncelleme hatası: ${e.message}")
                    }
                }
            },
            onGameEnd = { gameResult ->
                coroutineScope.launch {
                    if (gameResult is GameResult.MainMenu) {
                        // Eğer oyuncu Ana Menüye dönmek istediyse
                        if (isPlayer1) {
                            // Player 1 ise odayı sil
                            gameRepository.deleteGameRoom(roomId)
                                .onSuccess {
                                    println("Player 1 odadan ayrıldığı için oda silindi: $roomId")
                                    onGameEnd() // Lobiye dön
                                }
                                .onFailure { e ->
                                    println("Oda silinirken hata: ${e.message}")
                                    onGameEnd() // Hata olsa bile lobiye dönmeye çalış
                                }
                        } else {
                            // Player 2 ise odadan ayrıl
                            gameRepository.leaveGameRoom(roomId, currentUserId)
                                .onSuccess {
                                    println("Player 2 odadan ayrıldı: $roomId")
                                    onGameEnd() // Lobiye dön
                                }
                                .onFailure { e ->
                                    println("Odadan ayrılırken hata: ${e.message}")
                                    onGameEnd() // Hata olsa bile lobiye dönmeye çalış
                                }
                        }
                        return@launch // Bu durumda diğer işlemleri yapma
                    }

                    val finalScore = when (gameResult) {
                        is GameResult.Completed -> gameResult.score
                        is GameResult.Failed -> gameResult.score
                        else -> localPlayerState?.score ?: 0 // Bu senaryoya düşmemeli, ama fallback
                    }

                    // TetrisGameScreen'den gelen GameResult.Failed durumu, oyuncunun kendi oyununun bittiği anlamına gelir.
                    val isLocalPlayerActuallyGameOver = (gameResult is GameResult.Failed)

                    val newLocalState = localPlayerState?.copy(
                        isGameOver = isLocalPlayerActuallyGameOver,
                        score = finalScore,
                        currentBlockSequenceIndex = localPlayerState?.currentBlockSequenceIndex ?: 0
                    ) ?: PlayerState(currentUserId, score = finalScore, isGameOver = isLocalPlayerActuallyGameOver, level = currentMultiplayerLevel.id)


                    gameRepository.updatePlayerState(roomId, isPlayer1, newLocalState)
                        .onSuccess {
                            println("Kendi oyun durumum Firebase'e başarıyla güncellendi. Oyun durumu: ${newLocalState.isGameOver}")

                            // Firebase'den en güncel oda durumunu çekerek rakibin durumunu da kontrol et
                            val latestGameRoom = gameRepository.observeGameRoom(roomId).first()

                            latestGameRoom?.let { room ->
                                val otherPlayerState = if (isPlayer1) room.player2State else room.player1State
                                val otherPlayerId = if (isPlayer1) room.player2Id else room.player1Id

                                // Multiplayer modda oyunun bitiş koşulları:
                                // 1. Kendi isGameOver true ise
                                // 2. Rakip isGameOver true ise
                                // Eğer multiplayer seviyedeysek ve süre görevi varsa, süre bitimi de isGameOver'ı tetikler.
                                if (newLocalState.isGameOver || (otherPlayerState?.isGameOver == true)) {
                                    val winnerId: String?

                                    if (newLocalState.isGameOver && (otherPlayerState?.isGameOver == true)) {
                                        // İki oyuncu da game over ise, skoru yüksek olan kazanır
                                        winnerId = if (newLocalState.score >= (otherPlayerState.score)) {
                                            currentUserId
                                        } else {
                                            otherPlayerId
                                        }
                                    } else if (newLocalState.isGameOver) {
                                        // Sadece kendi oyunumuz bittiyse (biz kaybettiysek), rakip kazanır (eğer rakip hala oynuyorsa)
                                        winnerId = otherPlayerId
                                    } else if (otherPlayerState?.isGameOver == true) {
                                        // Sadece rakibin oyunu bittiyse (rakip kaybettiyse), biz kazanırız
                                        winnerId = currentUserId
                                    } else {
                                        winnerId = null // Bu duruma düşmemeliyiz, ama hata yakalama
                                    }

                                    // Sadece player1 veya kendi oyunu bittiyse ve oda henüz bitmediyse güncelle
                                    // Bu kontrol, race condition'ları engeller ve sadece bir tarafın durumu 'finished' yapmasını sağlar.
                                    if (isPlayer1 || (newLocalState.isGameOver && room.status != "finished")) {
                                        gameRepository.updateGameRoomStatus(roomId, "finished", winnerId)
                                        println("Oyun sonu durumu Firebase'e gönderildi: Kazanan: $winnerId")
                                    }
                                }
                            }
                        }
                        .onFailure { e ->
                            println("Kendi oyun durumumu Firebase'e güncellerken hata: ${e.message}")
                        }
                }
            }
        )
    } else if (isGameReallyOver) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val winnerId = gameRoom?.winnerId
            val localPlayerScore = localPlayerState?.score ?: 0
            val opponentPlayerFinalScore = opponentPlayerState?.score ?: 0
            val isMultiplayerLevel = gameRoom?.level?.id == GameLevels.MULTIPLAYER_LEVEL.id

            Text("OYUN BİTTİ!", style = MaterialTheme.typography.headlineLarge, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))

            if (isMultiplayerLevel) {
                if (winnerId != null) {
                    Text(
                        text = if (winnerId == currentUserId) "Tebrikler, SEN KAZANDIN!" else "Rakip Kazandı!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (winnerId == currentUserId) Color.Green else Color.Yellow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Senin Skorun: $localPlayerScore", style = MaterialTheme.typography.bodyLarge)
                    Text("Rakip Skor: ${opponentPlayerFinalScore}", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Oyun Sona Erdi (Kazanan Belirlenemedi)", style = MaterialTheme.typography.headlineMedium)
                }
            } else {

                Text("Oyun Sona Erdi", style = MaterialTheme.typography.headlineMedium)
                Text("Skorun: $localPlayerScore", style = MaterialTheme.typography.bodyLarge)
            }


            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { onGameEnd() }) {
                Text("Lobiye Dön")
            }
        }
    }
    else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Oyun Odası ID: ${roomId.substring(0, 6)}...",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Sen: ${
                    currentUserId.substring(
                        0,
                        5
                    )
                }... (${if (isPlayer1) "Player 1" else "Player 2"})"
            )
            Spacer(modifier = Modifier.height(16.dp))

            localPlayerState?.let { localState ->
                Text("Senin Skorun: ${localState.score}")
                // Sadece multiplayer seviyesindeyse isGameOver durumunu göster
                if (gameRoom?.level?.id == GameLevels.MULTIPLAYER_LEVEL.id) {
                    Text("Senin Durumun: ${if (localState.isGameOver) "Oyun Bitti" else "Oynuyor"}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            opponentPlayerState?.let { opponentState ->
                Text("Rakip Skor: ${opponentState.score}")

                if (gameRoom?.level?.id == GameLevels.MULTIPLAYER_LEVEL.id) {
                    Text("Rakip Durumu: ${if (opponentState.isGameOver) "Oyun Bitti" else "Oynuyor"}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rakibin Tahtası:", style = MaterialTheme.typography.titleSmall)
                RakipBoardCanvas(board = convert1DTo2D(opponentState.board.grid, BOARD_ROWS, BOARD_COLS))
            } ?: Text("Rakip bekleniyor...")

            Spacer(modifier = Modifier.height(32.dp))


            gameRoom?.let { room ->
                if (room.status == "finished") {
                    Text(
                        "Oyun Bitti!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Red
                    )
                    room.winnerId?.let { winnerId ->
                        Text(
                            "Kazanan: ${winnerId.substring(0, 5)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Button(onClick = {
                coroutineScope.launch {
                    if (isPlayer1) {
                        gameRepository.deleteGameRoom(roomId)
                            .onSuccess {
                                println("Oda başarıyla silindi.")
                                onGameEnd()
                            }
                            .onFailure { e ->
                                println("Oda silme hatası: ${e.message}")
                                onGameEnd() // Hata olsa bile lobiye dön
                            }
                    } else {
                        gameRepository.leaveGameRoom(roomId, currentUserId)
                            .onSuccess {
                                println("Odadan başarıyla çıkıldı.")
                                onGameEnd()
                            }
                            .onFailure { e ->
                                println("Odadan çıkış hatası: ${e.message}")
                                onGameEnd() // Hata olsa bile lobiye dön
                            }
                    }
                }
            }) {
                Text("Oyundan Çık / Lobiye Dön")
            }
        }
    }
}


@Composable
fun RakipBoardCanvas(board: List<List<Int>>) {
    val displayCols = BOARD_COLS
    val displayRows = BOARD_ROWS
    val cellSize = 10.dp

    Canvas(
        modifier = Modifier
            .size(displayCols * cellSize, displayRows * cellSize)
            .border(2.dp, Color.White.copy(alpha = 0.5f))
            .background(Color.Black)
    ) {
        val cellWidth = size.width / displayCols.toFloat()
        val cellHeight = size.height / displayRows.toFloat()

        board.forEachIndexed { y, row ->
            row.forEachIndexed { x, cellValue ->
                if (cellValue != 0) {
                    val blockColor = Color(cellValue)
                    drawRect(
                        color = blockColor,
                        topLeft = Offset(x * cellWidth, y * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                    drawRect(
                        color = Color.Gray.copy(alpha = 0.2f),
                        topLeft = Offset(x * cellWidth, y * cellHeight),
                        size = Size(cellWidth, cellHeight),
                        style = Stroke(1f)
                    )
                }
            }
        }
    }
}