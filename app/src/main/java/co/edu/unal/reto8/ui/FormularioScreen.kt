package co.edu.unal.reto8.ui

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.edu.unal.reto8.data.Clasificacion
import co.edu.unal.reto8.data.Empresa

/** Formulario reutilizado para crear (inicial == null) y editar empresas. */
@Composable
fun FormularioScreen(
    inicial: Empresa?,
    onCancelar: () -> Unit,
    onGuardar: (Empresa) -> Unit,
    modifier: Modifier = Modifier
) {
    var nombre by rememberSaveable { mutableStateOf(inicial?.nombre ?: "") }
    var url by rememberSaveable { mutableStateOf(inicial?.url ?: "") }
    var telefono by rememberSaveable { mutableStateOf(inicial?.telefono ?: "") }
    var email by rememberSaveable { mutableStateOf(inicial?.email ?: "") }
    var productos by rememberSaveable { mutableStateOf(inicial?.productosServicios ?: "") }
    var consultoria by rememberSaveable { mutableStateOf(inicial?.consultoria ?: false) }
    var desarrollo by rememberSaveable { mutableStateOf(inicial?.desarrolloMedida ?: false) }
    var fabrica by rememberSaveable { mutableStateOf(inicial?.fabricaSoftware ?: false) }
    // Los errores se muestran solo después del primer intento de guardar.
    var intentado by rememberSaveable { mutableStateOf(false) }

    val errorNombre = if (nombre.isBlank()) "El nombre es obligatorio" else null
    val errorUrl = if (url.isNotBlank() && !Patterns.WEB_URL.matcher(url.trim()).matches())
        "Ingresa una URL válida, por ejemplo www.empresa.com" else null
    val errorEmail = if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())
        "Ingresa un email válido, por ejemplo contacto@empresa.com" else null
    val errorClasif = if (!consultoria && !desarrollo && !fabrica)
        "Selecciona al menos una clasificación" else null

    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (inicial == null) "Agregar empresa" else "Editar empresa",
            style = MaterialTheme.typography.headlineSmall
        )
        Campo("Nombre de la empresa *", nombre, { nombre = it }, errorNombre.takeIf { intentado })
        Campo("URL de la página web", url, { url = it }, errorUrl.takeIf { intentado }, KeyboardType.Uri)
        Campo("Teléfono de contacto", telefono, { telefono = it }, null, KeyboardType.Phone)
        Campo("Email de contacto", email, { email = it }, errorEmail.takeIf { intentado }, KeyboardType.Email)
        OutlinedTextField(
            value = productos,
            onValueChange = { productos = it },
            label = { Text("Productos y servicios") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Clasificación * (puedes elegir varias)",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Casilla(Clasificacion.CONSULTORIA.etiqueta, consultoria) { consultoria = it }
        Casilla(Clasificacion.DESARROLLO_MEDIDA.etiqueta, desarrollo) { desarrollo = it }
        Casilla(Clasificacion.FABRICA_SOFTWARE.etiqueta, fabrica) { fabrica = it }
        if (intentado && errorClasif != null) {
            Text(
                errorClasif,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancelar) { Text("Cancelar") }
            Button(onClick = {
                intentado = true
                if (listOf(errorNombre, errorUrl, errorEmail, errorClasif).all { it == null }) {
                    onGuardar(
                        Empresa(
                            id = inicial?.id ?: 0,
                            nombre = nombre.trim(),
                            url = url.trim(),
                            telefono = telefono.trim(),
                            email = email.trim(),
                            productosServicios = productos.trim(),
                            consultoria = consultoria,
                            desarrolloMedida = desarrollo,
                            fabricaSoftware = fabrica
                        )
                    )
                }
            }) { Text("Guardar") }
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    onChange: (String) -> Unit,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Casilla(texto: String, marcado: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = marcado, onCheckedChange = onChange)
        Text(texto)
    }
}
