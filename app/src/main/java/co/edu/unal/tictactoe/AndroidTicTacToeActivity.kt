package co.edu.unal.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.unal.tictactoe.ui.theme.AndroidTicTacToeTutorial2Theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.stringResource

import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.clickable

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.ui.draw.clip

import android.media.AudioAttributes
import android.media.SoundPool

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize



class MainActivity : ComponentActivity() {

    private lateinit var mGame: TicTacToeGame

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        mGame = TicTacToeGame()

        setContent {
            AndroidTicTacToeTutorial2Theme {

                var resetKey by remember { mutableIntStateOf(0) }
                var selectedDifficulty by remember {
                    mutableStateOf(TicTacToeGame.DifficultyLevel.Harder)
                }

                // Sincroniza la dificultad inicial de la UI con la lógica del juego
                LaunchedEffect(Unit) {
                    mGame.setDifficultyLevel(selectedDifficulty)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    },
                    bottomBar = {
                        BottomActionsBar(
                            onNewGame = { resetKey++ },
                            selectedDifficulty = selectedDifficulty,
                            onSelectDifficulty = { level ->
                                selectedDifficulty = level
                                mGame.setDifficultyLevel(level)
                            },
                            onExit = { finish() }
                        )
                    }
                ) { innerPadding ->

                    TicTacToeBoard(
                        game = mGame,
                        resetKey = resetKey,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionsBar(
    onNewGame: () -> Unit,
    selectedDifficulty: TicTacToeGame.DifficultyLevel,
    onSelectDifficulty: (TicTacToeGame.DifficultyLevel) -> Unit,
    onExit: () -> Unit
) {
    var showDifficultyMenu by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomAction(
                symbol = "↻",
                label = "Nuevo juego",
                onClick = onNewGame
            )

            Box {
                BottomAction(
                    symbol = "⚙",
                    label = "Dificultad",
                    onClick = { showDifficultyMenu = true }
                )
                DropdownMenu(
                    expanded = showDifficultyMenu,
                    onDismissRequest = { showDifficultyMenu = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DifficultyOption(
                        label = "Fácil",
                        level = TicTacToeGame.DifficultyLevel.Easy,
                        selected = selectedDifficulty,
                        onSelect = {
                            onSelectDifficulty(it)
                            showDifficultyMenu = false
                        }
                    )
                    DifficultyOption(
                        label = "Difícil",
                        level = TicTacToeGame.DifficultyLevel.Harder,
                        selected = selectedDifficulty,
                        onSelect = {
                            onSelectDifficulty(it)
                            showDifficultyMenu = false
                        }
                    )
                    DifficultyOption(
                        label = "Experto",
                        level = TicTacToeGame.DifficultyLevel.Expert,
                        selected = selectedDifficulty,
                        onSelect = {
                            onSelectDifficulty(it)
                            showDifficultyMenu = false
                        }
                    )
                }
            }

            BottomAction(
                symbol = "✕",
                label = "Salir",
                onClick = { showExitDialog = true }
            )
        }
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = {
                    showExitDialog = false
                },
                title = {
                    Text("¿Salir del juego?")
                },
                text = {
                    Text("¿Estás seguro de que quieres salir?")
                },
                confirmButton = {
                    TextButton(
                        onClick = onExit
                    ) {
                        Text("Salir")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}


@Composable
private fun BottomAction(
    symbol: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun DifficultyOption(
    label: String,
    level: TicTacToeGame.DifficultyLevel,
    selected: TicTacToeGame.DifficultyLevel,
    onSelect: (TicTacToeGame.DifficultyLevel) -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            if (level == selected) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        onClick = { onSelect(level) }
    )
}

@Composable
fun TicTacToeBoard(
    game: TicTacToeGame,
    resetKey: Int,
    modifier: Modifier = Modifier
) {
    val turnHuman = stringResource(R.string.turn_human)
    val turnComputer = stringResource(R.string.turn_computer)
    val resultTie = stringResource(R.string.result_tie)
    val resultHumanWins = stringResource(R.string.result_human_wins)
    val resultComputerWins = stringResource(R.string.result_computer_wins)
    var computerTurn by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Imágenes de X y O
    val humanBitmap = ImageBitmap.imageResource(
        id = R.drawable.x_img
    )

    val computerBitmap = ImageBitmap.imageResource(
        id = R.drawable.o_img
    )

    // ---------------------------------------------------------
    // SoundPool para efectos de sonido
    // ---------------------------------------------------------

    val soundPool = remember(context) {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    var humanSoundId by remember {
        mutableIntStateOf(0)
    }

    var computerSoundId by remember {
        mutableIntStateOf(0)
    }

    DisposableEffect(soundPool) {

        humanSoundId = soundPool.load(
            context,
            R.raw.human_move,
            1
        )

        computerSoundId = soundPool.load(
            context,
            R.raw.computer_move,
            1
        )

        onDispose {
            soundPool.release()
        }
    }

    fun playHumanSound() {
        if (humanSoundId != 0) {
            soundPool.play(
                humanSoundId,
                1f,
                1f,
                1,
                0,
                1f
            )
        }
    }

    fun playComputerSound() {
        if (computerSoundId != 0) {
            soundPool.play(
                computerSoundId,
                1f,
                1f,
                1,
                0,
                1f
            )
        }
    }

    // ---------------------------------------------------------
    // Estado del juego
    // ---------------------------------------------------------

    var gameStatus by remember {
        mutableStateOf(turnHuman)
    }

    var board by remember {
        mutableStateOf(List(9) { ' ' })
    }

    var gameOver by remember {
        mutableStateOf(false)
    }

    // Quién empieza la próxima partida
    var humanFirst by remember {
        mutableStateOf(true)
    }

    // Marcador
    var humanWins by remember {
        mutableIntStateOf(0)
    }

    var computerWins by remember {
        mutableIntStateOf(0)
    }

    var ties by remember {
        mutableIntStateOf(0)
    }

    // La primera partida ya comienza con el humano.
    // La segunda comenzará con Android.
    LaunchedEffect(Unit) {
        humanFirst = false
    }



    // ---------------------------------------------------------
    // Registrar resultado
    // ---------------------------------------------------------

    fun recordResult(winner: Int) {
        when (winner) {
            1 -> ties++
            2 -> humanWins++
            3 -> computerWins++
        }
    }

    // ---------------------------------------------------------
    // Iniciar nueva partida
    // ---------------------------------------------------------

    fun startNewGame() {

        computerTurn = false

        game.clearBoard()

        board = List(9) { ' ' }

        gameOver = false

        if (humanFirst) {

            gameStatus = turnHuman

        } else {

            gameStatus = turnComputer

            val move = game.getComputerMove()

            game.setMove(
                TicTacToeGame.COMPUTER_PLAYER,
                move
            )

            board = board.toMutableList().also {
                it[move] = TicTacToeGame.COMPUTER_PLAYER
            }

            playComputerSound()

            gameStatus = turnHuman
        }

        // Alternar quién empieza la siguiente partida
        humanFirst = !humanFirst
    }

    // ---------------------------------------------------------
    // Detectar el botón "Nuevo juego"
    // ---------------------------------------------------------

    LaunchedEffect(resetKey) {

        if (resetKey > 0) {
            startNewGame()
        }
    }

    // ---------------------------------------------------------
    // Movimiento del jugador
    // ---------------------------------------------------------

    fun onCellClick(location: Int) {

        if (board[location] != ' ' || gameOver || computerTurn) {
            return
        }

        // -----------------------------------------------------
        // Turno del humano
        // -----------------------------------------------------

        game.setMove(
            TicTacToeGame.HUMAN_PLAYER,
            location
        )

        board = board.toMutableList().also {
            it[location] = TicTacToeGame.HUMAN_PLAYER
        }

        playHumanSound()

        val winner = game.checkForWinner()

        // Si el humano ganó o hubo empate, terminar
        if (winner != 0) {

            gameStatus = when (winner) {
                1 -> resultTie
                2 -> resultHumanWins
                3 -> resultComputerWins
                else -> gameStatus
            }

            gameOver = true
            recordResult(winner)

        } else {

            // Ahora le toca a Android
            computerTurn = true
        }
    }

    LaunchedEffect(computerTurn) {

        if (computerTurn && !gameOver) {

            // Pequeña pausa para que se vea primero el movimiento humano
            kotlinx.coroutines.delay(900)

            val move = game.getComputerMove()

            if (move != -1 && !gameOver) {

                game.setMove(
                    TicTacToeGame.COMPUTER_PLAYER,
                    move
                )

                board = board.toMutableList().also {
                    it[move] = TicTacToeGame.COMPUTER_PLAYER
                }

                playComputerSound()

                val winner = game.checkForWinner()

                gameStatus = when (winner) {
                    0 -> turnHuman
                    1 -> resultTie
                    2 -> resultHumanWins
                    3 -> resultComputerWins
                    else -> gameStatus
                }

                if (winner != 0) {

                    gameOver = true
                    recordResult(winner)
                }
            }

            computerTurn = false
        }
    }

    // ---------------------------------------------------------
    // Tablero
    // ---------------------------------------------------------

    var boardSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        ScoreBoard(
            humanWins = humanWins,
            ties = ties,
            computerWins = computerWins
        )

        Spacer(
            Modifier.height(32.dp)
        )

        // ---------------------------------------------------------
        // TABLERO PERSONALIZADO
        // ---------------------------------------------------------

        val gridColor = MaterialTheme.colorScheme.outlineVariant
        val gridWidth = 6.dp

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    RoundedCornerShape(18.dp)
                )
                .onSizeChanged {
                    boardSize = it
                }
                .pointerInput(
                    gameOver,
                    board,
                    boardSize
                ) {

                    detectTapGestures { offset ->

                        if (
                            boardSize.width == 0 ||
                            boardSize.height == 0
                        ) {
                            return@detectTapGestures
                        }

                        // Tamaño de cada celda
                        val cellWidth =
                            boardSize.width / 3f

                        val cellHeight =
                            boardSize.height / 3f

                        // Columna tocada
                        val col =
                            (offset.x / cellWidth)
                                .toInt()
                                .coerceIn(0, 2)

                        // Fila tocada
                        val row =
                            (offset.y / cellHeight)
                                .toInt()
                                .coerceIn(0, 2)

                        // Convertir fila/columna
                        // a posición 0..8
                        val position =
                            row * 3 + col

                        onCellClick(position)
                    }
                }
        ) {

            // -------------------------------------------------
            // Dimensiones del tablero
            // -------------------------------------------------

            val cellWidth =
                size.width / 3f

            val cellHeight =
                size.height / 3f

            // -------------------------------------------------
            // Grosor de las líneas
            // -------------------------------------------------

            val gridStrokeWidth =
                gridWidth.toPx()

            // -------------------------------------------------
            // Dibujar líneas verticales
            // -------------------------------------------------

            drawLine(
                color = gridColor,
                start = Offset(
                    cellWidth,
                    0f
                ),
                end = Offset(
                    cellWidth,
                    size.height
                ),
                strokeWidth = gridStrokeWidth
            )

            drawLine(
                color = gridColor,
                start = Offset(
                    cellWidth * 2,
                    0f
                ),
                end = Offset(
                    cellWidth * 2,
                    size.height
                ),
                strokeWidth = gridStrokeWidth
            )

            // -------------------------------------------------
            // Dibujar líneas horizontales
            // -------------------------------------------------

            drawLine(
                color = gridColor,
                start = Offset(
                    0f,
                    cellHeight
                ),
                end = Offset(
                    size.width,
                    cellHeight
                ),
                strokeWidth = gridStrokeWidth
            )

            drawLine(
                color = gridColor,
                start = Offset(
                    0f,
                    cellHeight * 2
                ),
                end = Offset(
                    size.width,
                    cellHeight * 2
                ),
                strokeWidth = gridStrokeWidth
            )

            // -------------------------------------------------
            // Dibujar X y O
            // -------------------------------------------------

            for (i in 0 until 9) {

                val row = i / 3
                val col = i % 3

                val occupant = board[i]

                val bitmap = when (occupant) {

                    TicTacToeGame.HUMAN_PLAYER ->
                        humanBitmap

                    TicTacToeGame.COMPUTER_PLAYER ->
                        computerBitmap

                    else ->
                        null
                }

                if (bitmap != null) {

                    // Margen para que X/O no toque
                    // las líneas del tablero
                    val margin =
                        cellWidth * 0.12f

                    val left =
                        col * cellWidth + margin

                    val top =
                        row * cellHeight + margin

                    val right =
                        (col + 1) * cellWidth - margin

                    val bottom =
                        (row + 1) * cellHeight - margin

                    val imageWidth =
                        (right - left).toInt()

                    val imageHeight =
                        (bottom - top).toInt()

                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset(
                            left.toInt(),
                            top.toInt()
                        ),
                        dstSize = IntSize(
                            imageWidth,
                            imageHeight
                        )
                    )
                }
            }
        }

        Spacer(
            Modifier.height(28.dp)
        )

        Text(
            text = gameStatus,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            Modifier.height(20.dp)
        )
    }
}


@Composable
private fun ScoreBoard(
    humanWins: Int,
    ties: Int,
    computerWins: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ScoreItem("Humano", humanWins, MaterialTheme.colorScheme.primary)
        ScoreItem("Empates", ties, MaterialTheme.colorScheme.onSurfaceVariant)
        ScoreItem("Android", computerWins, MaterialTheme.colorScheme.tertiary)
    }
}


@Composable
private fun ScoreItem(label: String, value: Int, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}