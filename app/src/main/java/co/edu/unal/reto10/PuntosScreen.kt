package co.edu.unal.reto10

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private sealed interface Estado {
    data object Inicial : Estado
    data object Cargando : Estado
    data class Error(val mensaje: String) : Estado
    data class Listo(val puntos: List<Punto>) : Estado
}

@Composable
fun PuntosScreen(modifier: Modifier = Modifier) {
    var texto by rememberSaveable { mutableStateOf("") }
    var estado by remember { mutableStateOf<Estado>(Estado.Inicial) }
    val alcance = rememberCoroutineScope()

    fun consultarServicio() {
        estado = Estado.Cargando
        alcance.launch {
            estado = try {
                Estado.Listo(consultar(texto))
            } catch (e: Exception) {
                Estado.Error(e.message ?: "No fue posible consultar el servicio")
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
            Text("Puntos posconsumo", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Norte de Santander · datos.gov.co",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar") },
                placeholder = { Text("municipio, programa o residuo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { consultarServicio() }),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { consultarServicio() },
                modifier = Modifier.fillMaxWidth(),
                enabled = estado !is Estado.Cargando,
            ) {
                Text("Consultar")
            }
            Spacer(Modifier.height(24.dp))
            Contenido(estado)
        }
    }
}

@Composable
private fun Contenido(estado: Estado) {
    when (estado) {
        Estado.Inicial -> Mensaje("Escribe un término y consulta el servicio.")
        Estado.Cargando -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is Estado.Error -> Mensaje(estado.mensaje)
        is Estado.Listo ->
            if (estado.puntos.isEmpty()) Mensaje("Sin resultados.")
            else Lista(estado.puntos)
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
private fun Lista(puntos: List<Punto>) {
    Text(
        "${puntos.size} punto${if (puntos.size == 1) "" else "s"}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(puntos) { punto -> Fila(punto) }
    }
}

@Composable
private fun Fila(punto: Punto) {
    Column {
        Text(punto.establecimiento, style = MaterialTheme.typography.titleSmall)
        val ubicacion = listOf(punto.direccion, punto.municipio)
            .filter { it.isNotEmpty() }
            .joinToString(" · ")
        if (ubicacion.isNotEmpty()) {
            Text(
                ubicacion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (punto.residuos.isNotEmpty()) {
            Text(
                punto.residuos,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
