package com.example.tetrisprojem.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.tetrisprojem.data.*
import com.example.tetrisprojem.logic.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.material3.Button
import androidx.compose.ui.unit.times
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import com.example.tetrisprojem.data.GameLevels
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect

// İkonlar için yeni importlar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon // Icon composable'ı için


@Composable
fun TetrisGameScreen(
    level: Level,
    initialPlayerState: PlayerState?,
    onPlayerStateChange: ((PlayerState) -> Unit)?,
    onGameEnd: (GameResult) -> Unit,
    blockSequence: List<String>? = null // Multiplayer için opsiyonel blok sırası
) {
    var currentBoard by remember { mutableStateOf(MutableList(BOARD_ROWS) { MutableList(BOARD_COLS) { 0 } }) }
    var currentPiece by remember { mutableStateOf<FallingPiece?>(null) }
    var score by remember { mutableStateOf(0) }
    var currentLevelSpeed by remember { mutableStateOf(level.startingSpeed) }
    var isGameOver by remember { mutableStateOf(false) } // Bu sadece o oyuncunun Game Over olup olmadığını belirtir
    var isGamePaused by remember { mutableStateOf(false) }
    var totalClearedLines by remember { mutableStateOf(0) }
    val currentMissions = remember { mutableStateListOf(*level.missions.toTypedArray()) }

    // Multiplayer için ek state'ler
    var playerBlockSequenceIndex by remember { mutableStateOf(initialPlayerState?.currentBlockSequenceIndex ?: 0) }
    var dynamicMissionTimeLeft by remember { mutableStateOf(0) } // SURVIVE_TIME görevi için sayaç
    var dynamicMissionJob by remember { mutableStateOf<Job?>(null) } // Sayaç coroutine'ini kontrol etmek için

    // Oyunu sıfırlama fonksiyonu
    val resetGame: () -> Unit = remember(level, blockSequence) {
        {
            currentBoard = initializeBoardWithObstacles(
                MutableList(BOARD_ROWS) { MutableList(BOARD_COLS) { 0 } },
                level.initialBoardObstacles
            )
            currentLevelSpeed = level.startingSpeed
            score = 0
            totalClearedLines = 0
            isGameOver = false
            isGamePaused = false // Sıfırlarken duraklatma durumunu da sıfırla
            currentMissions.clear()
            currentMissions.addAll(level.missions.map { it.copy(isCompleted = false) }) // Görevleri de sıfırla

            // Eğer multiplayer ise blockSequence indexini sıfırla ve ilk bloğu ona göre al
            if (initialPlayerState != null && blockSequence != null) {
                playerBlockSequenceIndex = 0 // Sıfırlıyoruz
                // Yeni bir parça oluştururken çarpışma kontrolü yapalım
                val newPiece = generateNextFallingPiece(blockSequence, playerBlockSequenceIndex)
                if (checkCollision(currentBoard, newPiece, 0, 0)) {
                    isGameOver = true // Oyun zaten başlamadan bitti
                    onGameEnd(GameResult.Failed(score, level.id)) // Oyun bittiğini bildir
                } else {
                    currentPiece = newPiece
                    playerBlockSequenceIndex++
                }
            } else {
                // Tek oyunculu için rastgele parça oluştur ve çarpışma kontrolü yap
                val newPiece = generateNextFallingPiece()
                if (checkCollision(currentBoard, newPiece, 0, 0)) {
                    isGameOver = true // Oyun zaten başlamadan bitti
                    onGameEnd(GameResult.Failed(score, level.id)) // Oyun bittiğini bildir
                } else {
                    currentPiece = newPiece
                }
            }

            dynamicMissionTimeLeft = 0 // Sayaç sıfırla
            dynamicMissionJob?.cancel() // Varsa önceki sayacı durdur
            dynamicMissionJob = null // İşin null olduğundan emin ol

            println("Oyun sıfırlandı: Level: ${level.name}, Multiplayer: ${initialPlayerState != null}")
        }
    }

    // İlk başlatmada ve level değiştiğinde oyunu başlat/sıfırla
    LaunchedEffect(level) {
        resetGame()
    }

    // Multiplayer modda initialPlayerState değiştiğinde yerel durumu güncelle
    // Bu LaunchedEffect, Firebase'den gelen güncel PlayerState'i dinler
    LaunchedEffect(initialPlayerState) {
        initialPlayerState?.let { playerState ->
            // Sadece multiplayer seviyesinde isGameOver durumunu Firebase'den al
            if (level.id == GameLevels.MULTIPLAYER_LEVEL.id) {
                isGameOver = playerState.isGameOver
            }

            currentBoard = convert1DTo2D(playerState.board.grid, BOARD_ROWS, BOARD_COLS)
            score = playerState.score
            playerBlockSequenceIndex = playerState.currentBlockSequenceIndex

            val firebasePiece = playerState.currentBlock?.let { block ->
                FallingPiece(
                    shapeId = block.shapeId,
                    shape = getBlockShapeById(block.shapeId),
                    color = block.color,
                    position = Point(block.col, block.row)
                )
            }

            // Parçayı senkronize et, ancak yerel hareketi koru
            if (currentPiece == null ||
                currentPiece?.shapeId != firebasePiece?.shapeId ||
                currentPiece?.position != firebasePiece?.position ||
                currentPiece?.color != firebasePiece?.color
            ) {
                currentPiece = firebasePiece
            }

            // Multiplayer görevleri için: Eğer ilk aşama görevleri tamamlandıysa ikinci aşama görevi ekle
            if (level.id == GameLevels.MULTIPLAYER_LEVEL.id &&
                currentMissions.none { it.id == "mm3_stage2_time" } &&
                currentMissions.all { it.isCompleted }
            ) {
                currentMissions.add(Mission(id = "mm3_stage2_time", type = MissionType.SURVIVE_TIME, targetValue = 40, description = "40 saniye hayatta kal", isCompleted = false))
            }
        }
    }


    // Oyun döngüsü (blok düşürme) ve Sayaç Yönetimi
    LaunchedEffect(isGameOver, isGamePaused, currentMissions.size) { // currentMissions.size eklenerek yeni görev eklendiğinde de tetiklenir
        if (!isGameOver && !isGamePaused) {
            val gameLoopInterval = currentLevelSpeed

            // Eğer multiplayer seviyedeysek ve zaman görevimiz varsa sayacı başlat/yönet
            if (level.id == GameLevels.MULTIPLAYER_LEVEL.id) {
                val surviveMission = currentMissions.find { it.type == MissionType.SURVIVE_TIME }
                if (surviveMission != null && !surviveMission.isCompleted && dynamicMissionJob == null) {
                    dynamicMissionTimeLeft = surviveMission.targetValue
                    dynamicMissionJob = launch { // LaunchedEffect'in kendi scope'unu kullan
                        while (dynamicMissionTimeLeft > 0 && isActive && !isGameOver && !surviveMission.isCompleted && !isGamePaused) {
                            delay(1000L)
                            if (!isGameOver && !isGamePaused) {
                                dynamicMissionTimeLeft--
                            }
                            if (dynamicMissionTimeLeft <= 0) {
                                surviveMission.isCompleted = true
                                isGameOver = true // Süre bittiğinde oyun bitiyor
                                onGameEnd(GameResult.Failed(score, level.id))
                                dynamicMissionJob?.cancel()
                                dynamicMissionJob = null
                                break
                            }
                        }
                        if (!isActive || isGameOver || isGamePaused || surviveMission.isCompleted) {
                            dynamicMissionJob?.cancel()
                            dynamicMissionJob = null
                        }
                    }
                } else if (surviveMission == null || surviveMission.isCompleted) {
                    dynamicMissionJob?.cancel()
                    dynamicMissionJob = null
                }
            }


            // Ana oyun düşme döngüsü
            while (isActive && !isGameOver && !isGamePaused) {
                delay(gameLoopInterval)

                if (currentPiece == null) {
                    val newPiece = if (blockSequence != null) {
                        generateNextFallingPiece(blockSequence, playerBlockSequenceIndex)
                    } else {
                        generateNextFallingPiece()
                    }

                    if (checkCollision(currentBoard, newPiece, 0, 0)) {
                        isGameOver = true
                        onGameEnd(GameResult.Failed(score, level.id))
                        break
                    }

                    currentPiece = newPiece

                    if (blockSequence != null) {
                        playerBlockSequenceIndex++
                    }
                } else {
                    val movedPiece = moveBlock(currentPiece!!, 0, 1)
                    if (checkCollision(currentBoard, movedPiece, 0, 0)) {
                        currentBoard = addToBoard(currentBoard, currentPiece!!)
                        val clearedLines = clearLines(currentBoard)
                        currentBoard = currentBoard.map { it.toMutableList() }.toMutableList()

                        if (clearedLines > 0) {
                            score += calculateScore(clearedLines)
                            totalClearedLines += clearedLines

                            updateMissionsOnLineClear(clearedLines, currentMissions, totalClearedLines)

                            currentLevelSpeed = (currentLevelSpeed * 0.95f).toLong().coerceAtLeast(100L)
                        }
                        currentPiece = null
                    } else {
                        currentPiece = movedPiece
                    }
                }

                updateMissionsOnScore(score, currentMissions)

                if (initialPlayerState != null) {
                    onPlayerStateChange?.invoke(
                        initialPlayerState.copy(
                            board = Board(convert2DTo1D(currentBoard)),
                            currentBlock = currentPiece?.let { BlockState(it.shapeId, it.color, it.position.y, it.position.x) },
                            score = score,
                            isGameOver = isGameOver,
                            level = level.id,
                            currentBlockSequenceIndex = playerBlockSequenceIndex
                        )
                    )
                }

                if (level.id != GameLevels.MULTIPLAYER_LEVEL.id && currentMissions.all { it.isCompleted }) {
                    if (!isGameOver) {
                        isGameOver = true
                        onGameEnd(GameResult.Completed(score, level.id))
                    }
                    break
                }
            }
        } else {
            dynamicMissionJob?.cancel()
            dynamicMissionJob = null
        }
    }


    // Ana dış arka plan rengi ve damalı deseni için yeni tonlar (daha koyu)
    val patternColorDark1 = Color(0xFF09111F) // Belirtilen ana renk
    val patternColorDark2 = Color(0xFF060B15) // Ondan biraz daha koyu

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(patternColorDark1) // Damalı Canvas'ın altındaki varsayılan renk
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && !isGameOver && currentPiece != null && !isGamePaused) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            moveBlock(currentPiece!!, -1, 0).let { moved ->
                                if (!checkCollision(currentBoard, moved, 0, 0)) currentPiece = moved
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            moveBlock(currentPiece!!, 1, 0).let { moved ->
                                if (!checkCollision(currentBoard, moved, 0, 0)) currentPiece = moved
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            moveBlock(currentPiece!!, 0, 1).let { moved ->
                                if (!checkCollision(currentBoard, moved, 0, 0)) {
                                    currentPiece = moved
                                }
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            rotateBlock(currentPiece!!).let { rotated ->
                                if (!checkCollision(currentBoard, rotated, 0, 0)) currentPiece = rotated
                            }
                            true
                        }
                        Key.Spacebar -> {
                            var tempPiece = currentPiece!!
                            while (!checkCollision(currentBoard, moveBlock(tempPiece, 0, 1), 0, 0)) {
                                tempPiece = moveBlock(tempPiece, 0, 1)
                            }
                            currentPiece = tempPiece
                            currentBoard = addToBoard(currentBoard, currentPiece!!)
                            currentBoard = currentBoard.map { it.toMutableList() }.toMutableList()
                            val clearedLines = clearLines(currentBoard)
                            if (clearedLines > 0) {
                                score += calculateScore(clearedLines)
                                totalClearedLines += clearedLines
                                updateMissionsOnLineClear(clearedLines, currentMissions, totalClearedLines)
                            }
                            currentPiece = null
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        if (!isGameOver && currentPiece != null && !isGamePaused) {
                            rotateBlock(currentPiece!!).let { rotated ->
                                if (!checkCollision(currentBoard, rotated, 0, 0)) currentPiece = rotated
                            }
                        }
                    },
                    onLongPress = { /* İsteğe bağlı */ }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (!isGameOver && currentPiece != null && !isGamePaused) {
                        val deltaX = if (dragAmount > 20) 1 else if (dragAmount < -20) -1 else 0
                        if (deltaX != 0) {
                            moveBlock(currentPiece!!, deltaX, 0).let { moved ->
                                if (!checkCollision(currentBoard, moved, 0, 0)) currentPiece = moved
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (!isGameOver && currentPiece != null && !isGamePaused) {
                        if (dragAmount > 20) {
                            moveBlock(currentPiece!!, 0, 1).let { moved ->
                                if (!checkCollision(currentBoard, moved, 0, 0)) {
                                    currentPiece = moved
                                }
                            }
                        } else if (dragAmount < -20) {
                            var tempPiece = currentPiece!!
                            while (!checkCollision(currentBoard, moveBlock(tempPiece, 0, 1), 0, 0)) {
                                tempPiece = moveBlock(tempPiece, 0, 1)
                            }
                            currentPiece = tempPiece
                            currentBoard = addToBoard(currentBoard, currentPiece!!)
                            currentBoard = currentBoard.map { it.toMutableList() }.toMutableList()
                            val clearedLines = clearLines(currentBoard)
                            if (clearedLines > 0) {
                                score += calculateScore(clearedLines)
                                totalClearedLines += clearedLines
                                updateMissionsOnLineClear(clearedLines, currentMissions, totalClearedLines)
                            }
                            currentPiece = null
                        }
                    }
                }
            }
            .focusable()
    ) {
        // Damalı (ızgaralı) arka plan çizimi
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 50.dp.toPx() // Izgara hücre boyutu

            val horizontalCells = (size.width / gridSize).toInt() + 1
            val verticalCells = (size.height / gridSize).toInt() + 1

            for (i in 0 until horizontalCells) {
                for (j in 0 until verticalCells) {
                    val colorToDraw = if ((i + j) % 2 == 0) patternColorDark1 else patternColorDark2
                    drawRect(
                        color = colorToDraw,
                        topLeft = Offset(i * gridSize, j * gridSize),
                        size = Size(gridSize, gridSize)
                    )
                }
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Skor: $score", fontSize = 20.sp, color = Color.White)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Görevler:", fontSize = 16.sp, color = Color.White)
                    currentMissions.forEach { mission ->
                        Text(
                            text = "${mission.description} ${if (mission.isCompleted) "(Tamamlandı!)" else ""}",
                            fontSize = 14.sp,
                            color = if (mission.isCompleted) Color.Green else Color.White
                        )
                    }
                    if (level.id == GameLevels.MULTIPLAYER_LEVEL.id) {
                        val surviveMission = currentMissions.find { it.type == MissionType.SURVIVE_TIME }
                        if (surviveMission != null) {
                            if (dynamicMissionTimeLeft > 0) {
                                Text(
                                    text = "Kalan Süre: $dynamicMissionTimeLeft saniye",
                                    fontSize = 14.sp,
                                    color = if (dynamicMissionTimeLeft <= 10) Color.Red else Color.Yellow,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (surviveMission.isCompleted) {
                                Text(
                                    text = "Süre Görevi Tamamlandı!",
                                    fontSize = 14.sp,
                                    color = Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .size(BOARD_COLS * 30.dp, BOARD_ROWS * 30.dp)
                    .background(
                        brush = Brush.verticalGradient( // Oyun tahtası gradyanı: #09111F'den Koyu Mor'a
                            colors = listOf(Color(0xFF09111F), Color(0xFF1E0C3A))
                        )
                    )
                    .border( // Parlak mavi çerçeve
                        width = 4.dp, // Çerçeve kalınlığı
                        color = Color(0xFF42A5F5), // Parlak Mavi (Deep Sky Blue gibi)
                        shape = RoundedCornerShape(4.dp) // Hafif yuvarlak köşeler
                    )
            ) {
                val cellWidth = size.width / BOARD_COLS.toFloat()
                val cellHeight = size.height / BOARD_ROWS.toFloat()
                val cornerRadius = CornerRadius(2f, 2f) // Hafif yuvarlatılmış köşeler için

                // Yardımcı fonksiyon: Tek bir 3D görünümlü blok çizer
                fun DrawScope.draw3DBlock(color: Color, x: Float, y: Float, width: Float, height: Float) {
                    // Blok renginin RGB bileşenlerini kullanarak parlaklık ve gölge renklerini hesapla
                    val baseRed = color.red
                    val baseGreen = color.green
                    val baseBlue = color.blue

                    // Parlaklık rengi: RGB değerlerini artır
                    val highlightColor = Color(
                        red = (baseRed + 0.2f).coerceIn(0f, 1f),
                        green = (baseGreen + 0.2f).coerceIn(0f, 1f),
                        blue = (baseBlue + 0.2f).coerceIn(0f, 1f),
                        alpha = color.alpha
                    )

                    // Gölge rengi: RGB değerlerini azalt
                    val shadowColor = Color(
                        red = (baseRed - 0.2f).coerceIn(0f, 1f),
                        green = (baseGreen - 0.2f).coerceIn(0f, 1f),
                        blue = (baseBlue - 0.2f).coerceIn(0f, 1f),
                        alpha = color.alpha
                    )

                    val innerColor = color // Bloğun ana rengi

                    // Ana blok gövdesi
                    drawRoundRect(
                        color = innerColor,
                        topLeft = Offset(x, y),
                        size = Size(width, height),
                        cornerRadius = cornerRadius
                    )

                    // Üst ve sol kenara parlaklık (ışık geliyormuş gibi)
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(x, y),
                        size = Size(width * 0.95f, height * 0.95f), // Hafif içe doğru
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 2f) // İnce kenarlık
                    )

                    // Alt ve sağ kenara gölge
                    drawRoundRect(
                        color = shadowColor,
                        topLeft = Offset(x + width * 0.05f, y + height * 0.05f), // Hafif dışa doğru
                        size = Size(width * 0.95f, height * 0.95f),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 2f)
                    )

                    // Ortadaki parlaklık efekti
                    clipRect(
                        left = x + width * 0.1f,
                        top = y + height * 0.1f,
                        right = x + width * 0.9f,
                        bottom = y + height * 0.9f
                    ) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.15f), // Hafif beyaz parlaklık
                            topLeft = Offset(x + width * 0.05f, y + height * 0.05f),
                            size = Size(width * 0.8f, height * 0.8f),
                            cornerRadius = cornerRadius
                        )
                    }

                    // Bloklar arası ayırıcı çizgiler
                    drawRect(
                        color = Color.Gray.copy(alpha = 0.2f),
                        topLeft = Offset(x, y),
                        size = Size(width, height),
                        style = Stroke(1f)
                    )
                }


                // Tahtayı çiz
                currentBoard.forEachIndexed { y, row ->
                    row.forEachIndexed { x, cellValue ->
                        if (cellValue != 0) {
                            val blockColor = Color(cellValue)
                            draw3DBlock(blockColor, x * cellWidth, y * cellHeight, cellWidth, cellHeight)
                        } else {
                            // Boş hücrelerin sadece ızgarasını çiz
                            drawRect(
                                color = Color.Gray.copy(alpha = 0.1f), // Daha soft ızgara
                                topLeft = Offset(x * cellWidth, y * cellHeight),
                                size = Size(cellWidth, cellHeight),
                                style = Stroke(1f)
                            )
                        }
                    }
                }

                // Aktif taşı çiz
                currentPiece?.let { piece ->
                    val blockColor = Color(piece.color)
                    piece.shape.forEach { point ->
                        val px = piece.position.x + point.x
                        val py = piece.position.y + point.y
                        if (py in 0 until BOARD_ROWS && px in 0 until BOARD_COLS) {
                            draw3DBlock(blockColor, px * cellWidth, py * cellHeight, cellWidth, cellHeight)
                        }
                    }
                }
            }

        }
        if (!isGameOver && !isGamePaused) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { isGamePaused = true },
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                ) {
                    Icon( // Text yerine Icon kullanılıyor
                        imageVector = Icons.Filled.Pause, // Pause ikonu
                        contentDescription = "Duraklat",
                        tint = Color.White // İkon rengi
                    )
                }
            }
        }

        if (isGamePaused) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(8.dp, shape = RoundedCornerShape(12.dp))
                    .background(Color.DarkGray, shape = RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "OYUN DURAKLATILDI",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { isGamePaused = false }) {
                    Text("Devam Et")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    onGameEnd(GameResult.MainMenu)
                    isGamePaused = false
                }) {
                    Text("Ana Menüye Dön")
                }
            }
        }
        if (isGameOver && initialPlayerState == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(8.dp, shape = RoundedCornerShape(12.dp))
                    .background(Color.DarkGray, shape = RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = if (currentMissions.all { it.isCompleted }) "GÖREV TAMAMLANDI!" else "OYUN BİTTİ!",
                    color = if (currentMissions.all { it.isCompleted }) Color.Green else Color.Red,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Skor: $score",
                    color = Color.Yellow,
                    fontSize = 24.sp
                )
                Text(
                    text = "Seviye: ${level.name}",
                    color = Color.Cyan,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = {
                    resetGame()
                    onGameEnd(GameResult.PlayAgain)
                }) {
                    Text("Tekrar Oyna")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (currentMissions.all { it.isCompleted }) {
                    val currentLevelIndex = GameLevels.ALL_LEVELS.indexOfFirst { it.id == level.id }
                    if (currentLevelIndex != -1 && currentLevelIndex + 1 < GameLevels.ALL_LEVELS.size) {
                        Button(onClick = {
                            onGameEnd(GameResult.NextLevel(level.id))
                        }) {
                            Text("Bir Sonraki Seviyeye Geç")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = "Tebrikler, tüm seviyeleri tamamladınız!",
                            color = Color.Green,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Button(onClick = {
                    onGameEnd(GameResult.MainMenu)
                }) {
                    Text("Ana Menüye Dön")
                }
            }
        }
    }
}