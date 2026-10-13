package co.edu.unal.tictactoe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun OnlineGameScreen(
    gameId: String,
    playerId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var game by remember { mutableStateOf<OnlineGame?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var gameExisted by remember { mutableStateOf(false) }
    val connected = rememberFirebaseConnected()
    var boardSize by remember { mutableStateOf(IntSize.Zero) }

    val humanBitmap = ImageBitmap.imageResource(id = R.drawable.x_img)
    val computerBitmap = ImageBitmap.imageResource(id = R.drawable.o_img)

    // Escuchar la partida: cada cambio en la nube actualiza "game"
    DisposableEffect(gameId) {
        val stopListening = OnlineRepository.listenToGame(gameId) { updated ->
            game = updated
            loaded = true
            if (updated != null) gameExisted = true
        }
        onDispose { stopListening() }
    }


    fun leave() {
        game?.let { OnlineRepository.leaveGame(it, playerId) }
        onExit()
    }

    BackHandler { leave() }

    fun onCellClick(position: Int) {
        val g = game ?: return
        val mySymbol = OnlineRepository.symbolFor(g, playerId) ?: return

        if (g.status != OnlineRepository.PLAYING) return
        if (g.turn != mySymbol.toString()) return
        if (g.board.getOrNull(position) != TicTacToeGame.OPEN_SPOT) return

        // No dibujamos nada aquí: la jugada se manda a la nube y el
        // tablero se actualiza cuando Firebase nos avisa del cambio.
        OnlineRepository.makeMove(gameId, playerId, position)
    }

    // ---------------------------------------------------------
    // Datos para mostrar
    // ---------------------------------------------------------
    val current = game
    val mySymbol = current?.let { OnlineRepository.symbolFor(it, playerId) }
    val board = current?.board?.toList()?.takeIf { it.size == 9 } ?: List(9) { ' ' }

    val rivalName = when (mySymbol) {
        'X' -> current?.guestName
        'O' -> current?.hostName
        else -> null
    }

    val statusText = when {
        !loaded -> "Conectando..."
        current == null && gameExisted ->
            "La partida se cerró porque uno de los jugadores perdió la conexión."
        current == null -> "La partida ya no existe."
        current.status == OnlineRepository.WAITING -> "Esperando a que otro jugador se una..."
        current.status == OnlineRepository.ABANDONED -> "Tu rival abandonó la partida."
        current.status == OnlineRepository.FINISHED -> when (current.winner) {
            1 -> "¡Empate!"
            2 -> if (mySymbol == 'X') "¡Ganaste!" else "Ganó $rivalName"
            3 -> if (mySymbol == 'O') "¡Ganaste!" else "Ganó $rivalName"
            else -> ""
        }
        current.turn == mySymbol?.toString() -> "Tu turno"
        else -> "Turno de $rivalName"
    }

    val isActive = current?.status == OnlineRepository.PLAYING ||
            current?.status == OnlineRepository.WAITING

    // ---------------------------------------------------------
    // Interfaz
    // ---------------------------------------------------------
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        val boardDimension = minOf(maxWidth, maxHeight * 0.55f)

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mySymbol != null) {
                ConnectionBanner(connected)
                Spacer(Modifier.height(12.dp))
                Text("Juegas con $mySymbol", style = MaterialTheme.typography.titleMedium)
            }
            if (!rivalName.isNullOrBlank()) {
                Text(
                    "Rival: $rivalName",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            GameBoardCanvas(
                board = board,
                gameOver = current?.status != OnlineRepository.PLAYING,
                humanBitmap = humanBitmap,
                computerBitmap = computerBitmap,
                boardSize = boardSize,
                onSizeChange = { boardSize = it },
                onCellClick = { onCellClick(it) },
                size = boardDimension
            )

            Spacer(Modifier.height(20.dp))

            Text(statusText, style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(20.dp))

            OutlinedButton(onClick = { leave() }) {
                Text(if (isActive) "Salir de la partida" else "Volver a la lista")
            }
        }
    }
}