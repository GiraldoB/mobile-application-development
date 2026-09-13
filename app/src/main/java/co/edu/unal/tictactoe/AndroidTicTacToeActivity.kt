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
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            actions = {
                                Box {
                                    TextButton(onClick = { showMenu = true }) {
                                        Text("Menú")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
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
    val context = LocalContext.current

    var board by remember { mutableStateOf(List(9) { ' ' }) }
    var gameStatus by remember { mutableStateOf(context.getString(R.string.turn_human)) }
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
            gameStatus = context.getString(R.string.turn_human)
        } else {
            // Le toca a Android primero
            gameStatus = context.getString(R.string.turn_computer)
            val move = game.getComputerMove()
            game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            board = board.toMutableList().also { it[move] = TicTacToeGame.COMPUTER_PLAYER }
            gameStatus = context.getString(R.string.turn_human)
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
            0 -> context.getString(R.string.turn_human)
            1 -> context.getString(R.string.result_tie)
            2 -> context.getString(R.string.result_human_wins)
            3 -> context.getString(R.string.result_computer_wins)
            else -> gameStatus
        }

        if (winner != 0) {
            gameOver = true
            recordResult(winner)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Row {
            GameButton(value = board[0], onClick = { onCellClick(0) })
            GameButton(value = board[1], onClick = { onCellClick(1) })
            GameButton(value = board[2], onClick = { onCellClick(2) })
        }

        Row {
            GameButton(value = board[3], onClick = { onCellClick(3) })
            GameButton(value = board[4], onClick = { onCellClick(4) })
            GameButton(value = board[5], onClick = { onCellClick(5) })
        }

        Row {
            GameButton(value = board[6], onClick = { onCellClick(6) })
            GameButton(value = board[7], onClick = { onCellClick(7) })
            GameButton(value = board[8], onClick = { onCellClick(8) })
        }

        Text(
            text = gameStatus,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 20.dp)
        )

        // --- Marcador: equivalente moderno al RelativeLayout de la guía ---
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Humano: $humanWins")
            Text("Empates: $ties")
            Text("Android: $computerWins")
        }

        Button(
            onClick = { startNewGame() },
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("Nuevo juego")
        }
    }
}


@Composable
fun GameButton(
    value: Char,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = Modifier.size(100.dp)
    ) {

        Text(
            text = value.toString(),
            fontSize = 40.sp
        )
    }
}