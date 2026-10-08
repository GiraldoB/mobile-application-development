package co.edu.unal.tictactoe

import android.content.Context
import android.content.res.Configuration
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

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp


class MainActivity : ComponentActivity() {

    private lateinit var mGame: TicTacToeGame

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        mGame = TicTacToeGame()

        setContent {
            AndroidTicTacToeTutorial2Theme {

                var resetKey by rememberSaveable { mutableIntStateOf(0) }
                var resetScoresKey by rememberSaveable { mutableIntStateOf(0) }
                var selectedDifficulty by rememberSaveable(stateSaver = DifficultySaver) {
                    mutableStateOf(TicTacToeGame.DifficultyLevel.Harder)
                }

                // mGame es un objeto nuevo tras cada rotación, así que hay que
                // volver a aplicarle la dificultad restaurada
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
                            onResetScores = { resetScoresKey++ },
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
                        resetScoresKey = resetScoresKey,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Convierte el tablero (List<Char>) a algo guardable (List<String>) y viceversa
private val BoardSaver = listSaver<List<Char>, String>(
    save = { board -> board.map { it.toString() } },
    restore = { saved -> saved.map { it[0] } }
)

// Convierte el enum DifficultyLevel a su posición (Int) y viceversa
private val DifficultySaver = Saver<TicTacToeGame.DifficultyLevel, Int>(
    save = { it.ordinal },
    restore = { ordinal -> TicTacToeGame.DifficultyLevel.values()[ordinal] }
)

@Composable
private fun BottomActionsBar(
    onNewGame: () -> Unit,
    onResetScores: () -> Unit,
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

            BottomAction(
                symbol = "🏆",
                label = "Reiniciar puntaje",
                onClick = onResetScores
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
    resetScoresKey: Int,
    modifier: Modifier = Modifier
) {
    val turnHuman = stringResource(R.string.turn_human)
    val turnComputer = stringResource(R.string.turn_computer)
    val resultTie = stringResource(R.string.result_tie)
    val resultHumanWins = stringResource(R.string.result_human_wins)
    val resultComputerWins = stringResource(R.string.result_computer_wins)
    var computerTurn by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences("ttt_prefs", Context.MODE_PRIVATE)
    }

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
    // Estado del juego (sobrevive a rotación de pantalla)
    // ---------------------------------------------------------

    var gameStatus by rememberSaveable {
        mutableStateOf(turnHuman)
    }

    var board by rememberSaveable(stateSaver = BoardSaver) {
        mutableStateOf(List(9) { ' ' })
    }

    var gameOver by rememberSaveable {
        mutableStateOf(false)
    }

    // Quién empieza la próxima partida
    var humanFirst by rememberSaveable {
        mutableStateOf(true)
    }

    // Marcador (sobrevive a rotación Y a cerrar la app)
    var humanWins by rememberSaveable {
        mutableIntStateOf(prefs.getInt("humanWins", 0))
    }

    var computerWins by rememberSaveable {
        mutableIntStateOf(prefs.getInt("computerWins", 0))
    }

    var ties by rememberSaveable {
        mutableIntStateOf(prefs.getInt("ties", 0))
    }

    fun saveScores() {
        prefs.edit()
            .putInt("humanWins", humanWins)
            .putInt("computerWins", computerWins)
            .putInt("ties", ties)
            .apply()
    }

    // Re-sincroniza la lógica interna de TicTacToeGame, ya que tras
    // rotar la pantalla se crea una instancia NUEVA de TicTacToeGame
    // que no sabe nada del tablero que Compose sí recuerda.
    LaunchedEffect(Unit) {
        game.setBoardState(board.toCharArray())
    }

    // Controla que "humanFirst = false" solo se aplique la PRIMERA vez
    // que arranca la app, nunca de nuevo tras una rotación.
    var initialTurnAdjusted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!initialTurnAdjusted) {
            humanFirst = false
            initialTurnAdjusted = true
        }
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
        saveScores()
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

    var lastHandledResetKey by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(resetKey) {
        if (resetKey != lastHandledResetKey) {
            startNewGame()
            lastHandledResetKey = resetKey
        }
    }

    // ---------------------------------------------------------
    // Detectar el botón "Reiniciar puntaje"
    // ---------------------------------------------------------

    var lastHandledScoreReset by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(resetScoresKey) {
        if (resetScoresKey != lastHandledScoreReset) {
            humanWins = 0
            computerWins = 0
            ties = 0
            saveScores()
            lastHandledScoreReset = resetScoresKey
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {

        if (isLandscape) {
            // --- Landscape: tablero a la izquierda, info a la derecha ---
            val boardDimension = minOf(maxHeight * 0.9f, maxWidth * 0.5f)

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameBoardCanvas(
                    board = board,
                    gameOver = gameOver,
                    humanBitmap = humanBitmap,
                    computerBitmap = computerBitmap,
                    boardSize = boardSize,
                    onSizeChange = { boardSize = it },
                    onCellClick = ::onCellClick,
                    size = boardDimension
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ScoreBoard(
                        humanWins = humanWins,
                        ties = ties,
                        computerWins = computerWins
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = gameStatus,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

        } else {
            // --- Portrait: todo apilado verticalmente ---
            val boardDimension = minOf(maxWidth, maxHeight * 0.6f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ScoreBoard(
                    humanWins = humanWins,
                    ties = ties,
                    computerWins = computerWins
                )

                Spacer(Modifier.height(24.dp))

                GameBoardCanvas(
                    board = board,
                    gameOver = gameOver,
                    humanBitmap = humanBitmap,
                    computerBitmap = computerBitmap,
                    boardSize = boardSize,
                    onSizeChange = { boardSize = it },
                    onCellClick = ::onCellClick,
                    size = boardDimension
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = gameStatus,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(20.dp))
            }
        }
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

@Composable
private fun GameBoardCanvas(
    board: List<Char>,
    gameOver: Boolean,
    humanBitmap: ImageBitmap,
    computerBitmap: ImageBitmap,
    boardSize: IntSize,
    onSizeChange: (IntSize) -> Unit,
    onCellClick: (Int) -> Unit,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val gridWidth = 6.dp

    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .onSizeChanged { onSizeChange(it) }
            .pointerInput(gameOver, board, boardSize) {
                detectTapGestures { offset ->
                    if (boardSize.width == 0 || boardSize.height == 0) {
                        return@detectTapGestures
                    }

                    val cellWidth = boardSize.width / 3f
                    val cellHeight = boardSize.height / 3f

                    val col = (offset.x / cellWidth).toInt().coerceIn(0, 2)
                    val row = (offset.y / cellHeight).toInt().coerceIn(0, 2)

                    val position = row * 3 + col

                    onCellClick(position)
                }
            }
    ) {
        val cellWidth = size.toPx() / 3f
        val cellHeight = size.toPx() / 3f
        val gridStrokeWidth = gridWidth.toPx()

        drawLine(
            color = gridColor,
            start = Offset(cellWidth, 0f),
            end = Offset(cellWidth, this.size.height),
            strokeWidth = gridStrokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(cellWidth * 2, 0f),
            end = Offset(cellWidth * 2, this.size.height),
            strokeWidth = gridStrokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, cellHeight),
            end = Offset(this.size.width, cellHeight),
            strokeWidth = gridStrokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, cellHeight * 2),
            end = Offset(this.size.width, cellHeight * 2),
            strokeWidth = gridStrokeWidth
        )

        for (i in 0 until 9) {
            val row = i / 3
            val col = i % 3
            val occupant = board[i]

            val bitmap = when (occupant) {
                TicTacToeGame.HUMAN_PLAYER -> humanBitmap
                TicTacToeGame.COMPUTER_PLAYER -> computerBitmap
                else -> null
            }

            if (bitmap != null) {
                val margin = cellWidth * 0.12f
                val left = col * cellWidth + margin
                val top = row * cellHeight + margin
                val right = (col + 1) * cellWidth - margin
                val bottom = (row + 1) * cellHeight - margin

                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(left.toInt(), top.toInt()),
                    dstSize = IntSize(
                        (right - left).toInt(),
                        (bottom - top).toInt()
                    )
                )
            }
        }
    }
}