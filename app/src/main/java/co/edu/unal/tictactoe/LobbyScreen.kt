package co.edu.unal.tictactoe

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun LobbyScreen(
    playerId: String,
    onEnterGame: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("ttt_prefs", Context.MODE_PRIVATE)
    }

    var playerName by rememberSaveable {
        mutableStateOf(prefs.getString("playerName", "") ?: "")
    }
    var games by remember { mutableStateOf<List<OnlineGame>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Escuchar la lista mientras esta pantalla esté visible
    DisposableEffect(Unit) {
        val stopListening = OnlineRepository.listenToWaitingGames(
            onChange = { list ->
                games = list.filter { it.playerX != playerId } // no mostrar mis propios juegos
                loading = false
            },
            onError = { error ->
                message = error
                loading = false
            }
        )
        onDispose { stopListening() }
    }

    val nameOk = playerName.isNotBlank()
    val connected = rememberFirebaseConnected()

    fun saveName() {
        prefs.edit().putString("playerName", playerName.trim()).apply()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Juego en línea", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))

        ConnectionBanner(connected)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = playerName,
            onValueChange = { playerName = it.take(20) },
            label = { Text("Tu nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                saveName()
                busy = true
                OnlineRepository.createGame(
                    playerId = playerId,
                    playerName = playerName.trim(),
                    onCreated = { gameId ->
                        busy = false
                        onEnterGame(gameId)
                    },
                    onError = { error ->
                        busy = false
                        message = error
                    }
                )
            },
            enabled = nameOk && !busy && connected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear juego nuevo")
        }

        Spacer(Modifier.height(24.dp))

        Text("Juegos disponibles", style = MaterialTheme.typography.titleMedium)

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                games.isEmpty() -> Text(
                    "No hay juegos disponibles. Crea uno y espera a que alguien se una.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(games, key = { it.id }) { game ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Juego de ${game.hostName}")
                                    Text(
                                        "Esperando rival",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        saveName()
                                        busy = true
                                        OnlineRepository.joinGame(
                                            gameId = game.id,
                                            playerId = playerId,
                                            playerName = playerName.trim()
                                        ) { joined ->
                                            busy = false
                                            if (joined) onEnterGame(game.id)
                                            else message = "Ese juego ya no está disponible."
                                        }
                                    },
                                    enabled = nameOk && !busy && connected,
                                ) {
                                    Text("Unirse")
                                }
                            }
                        }
                    }
                }
            }
        }

        TextButton(onClick = onBack) {
            Text("Volver al menú")
        }
    }
}

@Composable
fun rememberFirebaseConnected(): Boolean {
    var connected by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val stopListening = OnlineRepository.listenToConnection { connected = it }
        onDispose { stopListening() }
    }

    return connected
}

@Composable
fun ConnectionBanner(connected: Boolean) {
    if (!connected) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sin conexión con el servidor. Reconectando...",
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}