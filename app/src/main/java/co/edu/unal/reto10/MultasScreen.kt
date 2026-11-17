package co.edu.unal.reto10

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private sealed interface Estado {
    data object Inicial : Estado
    data object Cargando : Estado
    data class Error(val mensaje: String) : Estado
    data class Listo(
        val resumen: Resumen,
        val comparendos: List<Comparendo>,
        val cargandoMas: Boolean = false,
        val errorExtra: String? = null,
    ) : Estado
}

@Composable
fun MultasScreen(modifier: Modifier = Modifier) {
    var placa by rememberSaveable { mutableStateOf("") }
    var estado by remember { mutableStateOf<Estado>(Estado.Inicial) }
    val alcance = rememberCoroutineScope()
    val teclado = LocalSoftwareKeyboardController.current
    val foco = LocalFocusManager.current

    fun buscar() {
        val placaConsulta = normalizarPlaca(placa)
        teclado?.hide()
        foco.clearFocus()
        if (placaConsulta.length < 3) {
            estado = Estado.Error("Escribe al menos 3 caracteres de la placa.")
            return
        }
        placa = placaConsulta
        estado = Estado.Cargando
        alcance.launch {
            estado = try {
                val resumen = consultarResumen(placaConsulta)
                val comparendos =
                    if (resumen.total == 0) emptyList()
                    else ordenarPorFecha(consultarComparendos(placaConsulta, 0))
                Estado.Listo(resumen, comparendos)
            } catch (e: Exception) {
                Estado.Error(e.message ?: "No fue posible consultar el servicio.")
            }
        }
    }

    fun cargarMas(actual: Estado.Listo) {
        if (actual.cargandoMas) return
        estado = actual.copy(cargandoMas = true, errorExtra = null)
        alcance.launch {
            estado = try {
                val siguientes = consultarComparendos(placa, actual.comparendos.size)
                actual.copy(
                    comparendos = ordenarPorFecha(actual.comparendos + siguientes),
                    cargandoMas = false,
                )
            } catch (e: Exception) {
                actual.copy(
                    cargandoMas = false,
                    errorExtra = e.message ?: "No fue posible cargar más comparendos.",
                )
            }
        }
    }

    Scaffold(modifier = modifier) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Multas SIMIT", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Historial de comparendos · datos.gov.co",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = placa,
                onValueChange = { placa = it.uppercase().take(6) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Placa") },
                placeholder = { Text("AQC74B o AQC") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { buscar() }),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { buscar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = estado !is Estado.Cargando,
            ) {
                Text("Consultar")
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            foco.clearFocus()
                            teclado?.hide()
                        }
                    },
            ) {
                Contenido(estado) { cargarMas(it) }
            }
        }
    }
}

@Composable
private fun Contenido(estado: Estado, alCargarMas: (Estado.Listo) -> Unit) {
    when (estado) {
        Estado.Inicial -> Mensaje("Escribe la placa completa o sus tres primeras letras.")
        Estado.Cargando -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is Estado.Error -> Mensaje(estado.mensaje)
        is Estado.Listo ->
            if (estado.resumen.total == 0) Mensaje("No hay comparendos para esa placa.")
            else Resultados(estado, alCargarMas)
    }
}

@Composable
private fun Mensaje(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Resultados(estado: Estado.Listo, alCargarMas: (Estado.Listo) -> Unit) {
    val resumen = estado.resumen
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${resumen.total} comparendos", style = MaterialTheme.typography.titleMedium)
                Text(formatoPesos(resumen.monto), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${resumen.pagados} pagados · ${resumen.pendientes} pendientes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(estado.comparendos) { comparendo -> Fila(comparendo) }
        item {
            when {
                estado.cargandoMas -> CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    strokeWidth = 2.dp,
                )
                estado.comparendos.size < resumen.total -> TextButton(
                    onClick = { alCargarMas(estado) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cargar más")
                }
                else -> Text(
                    "Fin del historial",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
        estado.errorExtra?.let { mensaje -> item { Mensaje(mensaje) } }
    }
}

@Composable
private fun Fila(comparendo: Comparendo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(comparendo.fecha, style = MaterialTheme.typography.titleSmall)
            val lugar = listOf(comparendo.ciudad, comparendo.departamento)
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(" · ")
            if (lugar.isNotEmpty()) {
                Text(
                    lugar,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatoPesos(comparendo.valor), style = MaterialTheme.typography.bodyMedium)
            Text(
                if (comparendo.pagado) "Pagado" else "Pendiente",
                style = MaterialTheme.typography.labelSmall,
                color = if (comparendo.pagado) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
