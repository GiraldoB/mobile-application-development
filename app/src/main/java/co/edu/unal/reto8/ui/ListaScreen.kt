package co.edu.unal.reto8.ui

import android.database.SQLException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.edu.unal.reto8.data.Clasificacion
import co.edu.unal.reto8.data.Empresa
import co.edu.unal.reto8.data.EmpresaRepository

@Composable
fun ListaScreen(
    repo: EmpresaRepository,
    version: Int,
    onAbrir: (Long) -> Unit,
    onAgregar: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var texto by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf(Clasificacion.TODAS) }
    val empresas = remember(texto, filtro, version) {
        try {
            repo.buscar(texto, filtro)
        } catch (e: SQLException) {
            onError("No se pudieron cargar las empresas")
            emptyList()
        }
    }
    val hayFiltros = texto.isNotBlank() || filtro != Clasificacion.TODAS

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                "Directorio de empresas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Buscar por nombre") },
                singleLine = true,
                trailingIcon = if (texto.isNotEmpty()) {
                    { TextButton(onClick = { texto = "" }) { Text("Borrar") } }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Clasificación",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Clasificacion.entries.forEach { c ->
                    FilterChip(
                        selected = filtro == c,
                        onClick = { filtro = c },
                        label = { Text(c.etiqueta) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            when {
                empresas.isEmpty() && hayFiltros -> MensajeVacio(
                    mensaje = "No se encontraron empresas con estos criterios",
                    accion = "Limpiar filtros",
                    principal = false,
                    onClick = {
                        texto = ""
                        filtro = Clasificacion.TODAS
                    }
                )

                empresas.isEmpty() -> MensajeVacio(
                    mensaje = "Aún no hay empresas registradas",
                    accion = "Agregar la primera empresa",
                    principal = true,
                    onClick = onAgregar
                )

                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(empresas, key = { it.id }) { e ->
                        EmpresaItem(e) { onAbrir(e.id) }
                        HorizontalDivider()
                    }
                    // Espacio para que el botón flotante no tape el último elemento.
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
        if (empresas.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAgregar,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .semantics { contentDescription = "Agregar empresa" }
            ) { Text("+ Agregar empresa") }
        }
    }
}

@Composable
private fun MensajeVacio(mensaje: String, accion: String, principal: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        if (principal) Button(onClick = onClick) { Text(accion) }
        else OutlinedButton(onClick = onClick) { Text(accion) }
    }
}

@Composable
private fun EmpresaItem(empresa: Empresa, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)
    ) {
        Text(empresa.nombre, style = MaterialTheme.typography.titleMedium)
        val contacto = listOf(empresa.telefono, empresa.email).filter { it.isNotBlank() }
        if (contacto.isNotEmpty()) {
            Text(
                contacto.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Etiquetas(empresa.clasificaciones())
    }
}
