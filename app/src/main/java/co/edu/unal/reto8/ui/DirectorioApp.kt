package co.edu.unal.reto8.ui

import android.database.SQLException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import co.edu.unal.reto8.data.Empresa
import co.edu.unal.reto8.data.EmpresaRepository
import kotlinx.coroutines.launch

/** Pantallas de la app; la navegación se maneja con este estado. */
sealed interface Pantalla {
    data object Lista : Pantalla
    data class Detalle(val id: Long) : Pantalla
    data class Formulario(val id: Long?) : Pantalla
}

@Composable
fun DirectorioApp(repo: EmpresaRepository) {
    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Lista) }
    // Se incrementa tras cada cambio para volver a consultar SQLite.
    var version by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val avisar: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        val mod = Modifier.padding(padding)
        when (val p = pantalla) {
            Pantalla.Lista -> ListaScreen(
                repo = repo,
                version = version,
                onAbrir = { pantalla = Pantalla.Detalle(it) },
                onAgregar = { pantalla = Pantalla.Formulario(null) },
                onError = avisar,
                modifier = mod
            )

            is Pantalla.Detalle -> {
                BackHandler { pantalla = Pantalla.Lista }
                DetalleScreen(
                    repo = repo,
                    id = p.id,
                    version = version,
                    onVolver = { pantalla = Pantalla.Lista },
                    onEditar = { pantalla = Pantalla.Formulario(p.id) },
                    onEliminar = {
                        try {
                            repo.eliminar(p.id)
                            version++
                            pantalla = Pantalla.Lista
                            avisar("Empresa eliminada")
                        } catch (e: SQLException) {
                            avisar("No se pudo eliminar la empresa")
                        }
                    },
                    modifier = mod
                )
            }

            is Pantalla.Formulario -> {
                val volver: () -> Unit = {
                    pantalla = p.id?.let { Pantalla.Detalle(it) } ?: Pantalla.Lista
                }
                BackHandler(onBack = volver)
                val inicial = remember(p.id) {
                    p.id?.let { runCatching { repo.obtenerPorId(it) }.getOrNull() }
                }
                FormularioScreen(
                    inicial = inicial,
                    onCancelar = volver,
                    onGuardar = { empresa: Empresa ->
                        try {
                            if (p.id == null) {
                                val nuevoId = repo.insertar(empresa)
                                pantalla = Pantalla.Detalle(nuevoId)
                                avisar("Empresa guardada")
                            } else {
                                repo.actualizar(empresa.copy(id = p.id))
                                pantalla = Pantalla.Detalle(p.id)
                                avisar("Cambios guardados")
                            }
                            version++
                        } catch (e: SQLException) {
                            // Se conserva el formulario con los datos ingresados.
                            avisar("No se pudo guardar. Revisa los datos e inténtalo de nuevo.")
                        }
                    },
                    modifier = mod
                )
            }
        }
    }
}
