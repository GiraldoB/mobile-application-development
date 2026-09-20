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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext



import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import co.edu.unal.tictactoe.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight



class MainActivity : ComponentActivity() {

    private lateinit var mGame: TicTacToeGame

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        mGame = TicTacToeGame()

        setContent {
            AndroidTicTacToeTutorial2Theme {

                var showMenu by remember { mutableStateOf(false) }
                var resetKey by remember { mutableIntStateOf(0) }

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
                            ),
                            actions = {
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Text("⋮", fontSize = 20.sp)
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.new_game)) },
                                            onClick = {
                                                showMenu = false
                                                resetKey++
                                            }
                                        )
                                    }
                                }
                            }
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

    var gameStatus by remember { mutableStateOf(turnHuman) }

    var board by remember { mutableStateOf(List(9) { ' ' }) }
    var gameOver by remember { mutableStateOf(false) }

    // Quién le toca empezar la próxima partida
    var humanFirst by remember { mutableStateOf(true) }

    // Marcador
    var humanWins by remember { mutableIntStateOf(0) }
    var computerWins by remember { mutableIntStateOf(0) }
    var ties by remember { mutableIntStateOf(0) }

    // Corrige el desfase inicial: la partida 1 ya usó el turno del humano,
    // así que preparamos que la partida 2 sea de Android.
    LaunchedEffect(Unit) {
        humanFirst = false
    }

    // Registra el resultado en el marcador según lo que devuelva checkForWinner()
    fun recordResult(winner: Int) {
        when (winner) {
            1 -> ties++
            2 -> humanWins++
            3 -> computerWins++
        }
    }

    // Inicia una partida nueva, alternando quién empieza
    fun startNewGame() {
        game.clearBoard()
        board = List(9) { ' ' }
        gameOver = false

        if (humanFirst) {
            gameStatus = turnHuman
        } else {
            gameStatus = turnComputer
            val move = game.getComputerMove()
            game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            board = board.toMutableList().also { it[move] = TicTacToeGame.COMPUTER_PLAYER }
            gameStatus = turnHuman
        }

        // La próxima vez le toca al otro
        humanFirst = !humanFirst
    }

    LaunchedEffect(resetKey) {
        if (resetKey > 0) {
            startNewGame()
        }
    }

    fun onCellClick(location: Int) {

        if (board[location] != ' ' || gameOver) {
            return
        }

        // --- Turno del humano ---
        game.setMove(TicTacToeGame.HUMAN_PLAYER, location)
        board = board.toMutableList().also {
            it[location] = TicTacToeGame.HUMAN_PLAYER
        }

        var winner = game.checkForWinner()

        // --- Turno de Android, solo si el humano no acabó el juego ---
        if (winner == 0) {
            val move = game.getComputerMove()
            game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            board = board.toMutableList().also {
                it[move] = TicTacToeGame.COMPUTER_PLAYER
            }
            winner = game.checkForWinner()
        }

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

        Spacer(Modifier.height(32.dp))

        // --- Tablero 3x3 responsivo ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) { col ->
                        val index = row * 3 + col
                        GameCell(
                            value = board[index],
                            onClick = { onCellClick(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = gameStatus,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(20.dp))

        TextButton(
            onClick = { startNewGame() },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Nuevo juego")
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
fun GameCell(
    value: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmpty = value == ' '

    val markColor = when (value) {
        TicTacToeGame.HUMAN_PLAYER -> MaterialTheme.colorScheme.primary
        TicTacToeGame.COMPUTER_PLAYER -> MaterialTheme.colorScheme.tertiary
        else -> Color.Transparent
    }

    // Pequeño rebote al colocar la ficha: responde a la acción, no decora
    val scale by animateFloatAsState(
        targetValue = if (isEmpty) 0.6f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "markScale"
    )

    Surface(
        onClick = onClick,
        enabled = isEmpty,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = value.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = markColor,
                modifier = Modifier.scale(scale)
            )
        }
    }
}