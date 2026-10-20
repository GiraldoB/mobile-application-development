package co.edu.unal.reto8.ui

import android.database.SQLException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.unal.reto8.data.EmpresaRepository

@Composable
fun DetalleScreen(
    repo: EmpresaRepository,
    id: Long,
    version: Int,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val empresa = remember(id, version) {
        try { repo.obtenerPorId(id) } catch (e: SQLException) { null }
    }
    var confirmar by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        TextButton(onClick = onVolver) { Text("← Volver al listado") }
        if (empresa == null) {
            Text("No se encontró la empresa.", modifier = Modifier.padding(top = 16.dp))
            return@Column
        }
        Text(empresa.nombre, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Etiquetas(empresa.clasificaciones())
        Spacer(Modifier.height(8.dp))
        Campo("Página web", empresa.url)
        Campo("Teléfono de contacto", empresa.telefono)
        Campo("Email de contacto", empresa.email)
        Campo("Productos y servicios", empresa.productosServicios)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onEditar) { Text("Editar") }
            OutlinedButton(
                onClick = { confirmar = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Eliminar") }
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Eliminar empresa") },
            text = { Text("¿Deseas eliminar esta empresa? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    onEliminar()
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (valor.isBlank()) {
            Text(
                "No registrado",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            Text(valor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
