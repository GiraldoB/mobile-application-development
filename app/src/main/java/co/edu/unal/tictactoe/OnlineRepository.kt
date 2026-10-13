package co.edu.unal.tictactoe

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.util.UUID
import android.util.Log
import com.google.firebase.database.Logger

// Así se ve una partida en la nube.
// Firebase necesita "var" y valores por defecto para poder leerla.
data class OnlineGame(
    var id: String = "",
    var hostName: String = "",
    var guestName: String = "",
    var playerX: String = "",
    var playerO: String = "",
    var board: String = "         ",   // 9 espacios = tablero vacío
    var turn: String = "X",
    var status: String = OnlineRepository.WAITING,
    var winner: Int = 0,
    var createdAt: Long = 0L
)

object OnlineRepository {

    const val WAITING = "waiting"
    const val PLAYING = "playing"
    const val FINISHED = "finished"
    const val ABANDONED = "abandoned"
    private const val TAG = "TRIQUI"

    // Un juego que lleva más de 10 minutos esperando se considera abandonado
    private const val MAX_WAIT_MS = 10 * 60 * 1000L

    private const val DATABASE_URL = "https://triqui-online-1f86e-default-rtdb.firebaseio.com"

    private val db: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(DATABASE_URL).also {
            // Activa mensajes detallados de conexión en Logcat
            // (quítalo cuando todo funcione bien)
            it.setLogLevel(Logger.Level.DEBUG)
        }
    }

    private val gamesRef by lazy {
        db.getReference("games")
    }

    // ---------------------------------------------------------
    // Identidad del jugador: un id único por teléfono, guardado
    // en SharedPreferences para que no cambie entre aperturas.
    // ---------------------------------------------------------
    fun getPlayerId(context: Context): String {
        val prefs = context.getSharedPreferences("ttt_prefs", Context.MODE_PRIVATE)
        return prefs.getString("playerId", null)
            ?: UUID.randomUUID().toString().also {
                prefs.edit().putString("playerId", it).apply()
            }
    }

    // ¿Con qué símbolo juega este teléfono en esta partida?
    fun symbolFor(game: OnlineGame, playerId: String): Char? = when (playerId) {
        game.playerX -> 'X'
        game.playerO -> 'O'
        else -> null
    }

    // ---------------------------------------------------------
    // ¿Este teléfono está conectado a Firebase ahora mismo?
    // ".info/connected" es una ruta especial que Firebase
    // actualiza sola con el estado de la conexión.
    // ---------------------------------------------------------
    fun listenToConnection(onChange: (Boolean) -> Unit): () -> Unit {
        val ref = db.getReference(".info/connected")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(snapshot.getValue(Boolean::class.java) ?: false)
            }

            override fun onCancelled(error: DatabaseError) { }
        }

        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    // ---------------------------------------------------------
    // Crear un juego nuevo (quien lo crea juega con X)
    // ---------------------------------------------------------
    fun createGame(
        playerId: String,
        playerName: String,
        onCreated: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = gamesRef.push()
        val gameId = ref.key ?: return onError("No se pudo crear el juego")

        val game = OnlineGame(
            id = gameId,
            hostName = playerName,
            playerX = playerId,
            createdAt = System.currentTimeMillis()
        )

        ref.setValue(game)
            .addOnSuccessListener {
                Log.d(TAG, "Juego creado: $gameId")
                onCreated(gameId)
            }
            .addOnFailureListener {
                Log.e(TAG, "Error creando juego", it)
                onError(it.message ?: "Error al crear el juego")
            }
    }


    // ---------------------------------------------------------
    // Escuchar la lista de juegos que están esperando rival.
    // Devuelve una función para dejar de escuchar.
    // ---------------------------------------------------------
    fun listenToWaitingGames(
        onChange: (List<OnlineGame>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val query = gamesRef.orderByChild("status").equalTo(WAITING)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val games = mutableListOf<OnlineGame>()

                for (child in snapshot.children) {
                    val game = child.getValue(OnlineGame::class.java) ?: continue

                    if (now - game.createdAt > MAX_WAIT_MS) {
                        // Juego viejo que nadie tomó: lo limpiamos
                        child.ref.removeValue()
                    } else {
                        games.add(game)
                    }
                }

                Log.d(TAG, "Juegos en espera: ${games.map { it.id }}")
                onChange(games.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error escuchando lista: ${error.message}")
                onError(error.message)
            }
        }

        query.addValueEventListener(listener)
        return { query.removeEventListener(listener) }
    }

    // ---------------------------------------------------------
    // Escuchar UNA partida (cada jugada del rival llega por aquí)
    // ---------------------------------------------------------
    fun listenToGame(gameId: String, onChange: (OnlineGame?) -> Unit): () -> Unit {
        val ref = gamesRef.child(gameId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = snapshot.getValue(OnlineGame::class.java)
                Log.d(TAG, "Partida $gameId -> status=${game?.status}, board='${game?.board}'")
                onChange(game)
            }

            override fun onCancelled(error: DatabaseError) {
                onChange(null)
            }
        }

        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    // ---------------------------------------------------------
    // Unirse a un juego (con transacción, ver explicación abajo)
    // ---------------------------------------------------------
    fun joinGame(
        gameId: String,
        playerId: String,
        playerName: String,
        onResult: (Boolean) -> Unit
    ) {
        Log.d(TAG, "Intentando unirse a $gameId")

        gamesRef.child(gameId).runTransaction(object : Transaction.Handler {

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val game = currentData.getValue(OnlineGame::class.java)

                // La primera pasada puede venir vacía mientras se
                // consulta al servidor; Firebase la repite con el dato real.
                if (game == null) {
                    Log.d(TAG, "join: dato local vacío, esperando dato del servidor")
                    return Transaction.success(currentData)
                }

                if (game.status != WAITING) {
                    Log.d(TAG, "join abortado: status=${game.status}")
                    return Transaction.abort()
                }
                if (game.playerO.isNotEmpty()) {
                    Log.d(TAG, "join abortado: ya tiene rival")
                    return Transaction.abort()
                }
                if (game.playerX == playerId) {
                    Log.d(TAG, "join abortado: es mi propio juego")
                    return Transaction.abort()
                }

                game.playerO = playerId
                game.guestName = playerName
                game.status = PLAYING
                currentData.value = game
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                val result = currentData?.getValue(OnlineGame::class.java)
                val joined = error == null && committed && result?.playerO == playerId

                Log.d(
                    TAG,
                    "join terminado: error=${error?.message}, committed=$committed, " +
                            "status=${result?.status}, joined=$joined"
                )

                onResult(joined)
            }
        }, false)
    }

    // ---------------------------------------------------------
    // Hacer una jugada (también con transacción)
    // ---------------------------------------------------------
    fun makeMove(gameId: String, playerId: String, location: Int) {
        gamesRef.child(gameId).runTransaction(object : Transaction.Handler {

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val game = currentData.getValue(OnlineGame::class.java)
                    ?: return Transaction.success(currentData)

                if (game.status != PLAYING) return Transaction.abort()

                val mySymbol = symbolFor(game, playerId) ?: return Transaction.abort()
                if (game.turn != mySymbol.toString()) return Transaction.abort()

                val cells = game.board.toCharArray()
                if (cells.size != 9 || cells[location] != TicTacToeGame.OPEN_SPOT) {
                    return Transaction.abort()
                }

                cells[location] = mySymbol

                // Reutilizamos la lógica del juego en Java para saber si alguien ganó
                val logic = TicTacToeGame()
                logic.setBoardState(cells)
                val winner = logic.checkForWinner()

                game.board = String(cells)
                game.turn = if (mySymbol == 'X') "O" else "X"

                if (winner != 0) {
                    game.status = FINISHED
                    game.winner = winner
                }

                currentData.value = game
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) { }
        })
    }

    // ---------------------------------------------------------
    // Salir de una partida
    // ---------------------------------------------------------
    fun leaveGame(game: OnlineGame, playerId: String) {
        val ref = gamesRef.child(game.id)
        when (game.status) {
            // Nadie se había unido: el juego desaparece de la lista
            WAITING -> if (game.playerX == playerId) ref.removeValue()
            // Partida en curso: se le avisa al rival que abandonaste
            PLAYING -> ref.child("status").setValue(ABANDONED)
        }
    }
}