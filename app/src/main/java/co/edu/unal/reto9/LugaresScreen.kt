package co.edu.unal.reto9

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow
import kotlin.math.roundToInt

private val Context.preferencias by preferencesDataStore("preferencias")
private val RADIO_KM = intPreferencesKey("radio_km")
private const val RADIO_POR_DEFECTO = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LugaresScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // null hasta que DataStore entregue el valor guardado, para no buscar dos veces al iniciar.
    val radioKm by remember { context.preferencias.data.map { it[RADIO_KM] ?: RADIO_POR_DEFECTO } }
        .collectAsState(initial = null)
    var categoria by rememberSaveable { mutableStateOf(Categoria.HOSPITAL) }
    var ubicacion by remember { mutableStateOf<GeoPoint?>(null) }
    var lugares by remember { mutableStateOf(emptyList<Lugar>()) }
    var mensaje by remember { mutableStateOf("Obteniendo ubicación…") }
    var editandoRadio by remember { mutableStateOf(false) }
    var intento by remember { mutableIntStateOf(0) }

    @SuppressLint("MissingPermission") // solo se llama tras conceder el permiso
    fun obtenerUbicacion(preciso: Boolean) {
        mensaje = "Obteniendo ubicación…"
        val lm = context.getSystemService(LocationManager::class.java)
        // El GPS exige permiso preciso; con permiso aproximado se usa la red.
        val proveedor = if (preciso && lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        LocationManagerCompat.getCurrentLocation(
            lm, proveedor, CancellationSignal(), ContextCompat.getMainExecutor(context)
        ) { actual ->
            // Si no llega una posición nueva (p. ej. sin señal), se usa la última conocida.
            val loc = actual ?: lm.getLastKnownLocation(proveedor)
            if (loc != null) {
                ubicacion = GeoPoint(loc.latitude, loc.longitude)
                intento++ // busca de nuevo aunque la posición no haya cambiado
            } else mensaje = "Activa la ubicación del dispositivo"
        }
    }

    // Si el permiso ya estaba concedido, el launcher responde al instante sin mostrar diálogo.
    val pedirPermiso = rememberLauncherForActivityResult(RequestMultiplePermissions()) { resultado ->
        when {
            resultado[Manifest.permission.ACCESS_FINE_LOCATION] == true -> obtenerUbicacion(preciso = true)
            resultado[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> obtenerUbicacion(preciso = false)
            else -> mensaje = "Se necesita permiso de ubicación"
        }
    }
    fun actualizarUbicacion() = pedirPermiso.launch(
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    LaunchedEffect(Unit) { actualizarUbicacion() }

    LaunchedEffect(ubicacion, radioKm, categoria, intento) {
        val u = ubicacion ?: return@LaunchedEffect
        val r = radioKm ?: return@LaunchedEffect
        mensaje = "Buscando ${categoria.etiqueta.lowercase()}…"
        try {
            lugares = buscarLugares(categoria, u.latitude, u.longitude, r)
            mensaje = "${lugares.size} ${categoria.etiqueta.lowercase()} a menos de $r km " +
                "(%.5f, %.5f)".format(u.latitude, u.longitude)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lugares = emptyList()
            mensaje = "Error al buscar lugares: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lugares cercanos") },
                actions = {
                    TextButton(onClick = { editandoRadio = true }, enabled = radioKm != null) {
                        Text("Radio: ${radioKm ?: "-"} km")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Categoria.entries.forEach { c ->
                    FilterChip(
                        selected = c == categoria,
                        onClick = { categoria = c },
                        label = { Text(c.etiqueta) },
                    )
                }
            }
            // clipToBounds: MapView dibuja fuera de su área y taparía los chips y el texto.
            Mapa(ubicacion, radioKm, lugares, Modifier.weight(1f).fillMaxWidth().clipToBounds())
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(mensaje, Modifier.weight(1f))
                TextButton(onClick = { actualizarUbicacion() }) { Text("Actualizar") }
            }
        }
    }

    radioKm?.let { actual ->
        if (editandoRadio) {
            RadioDialog(
                actual = actual,
                onCerrar = { editandoRadio = false },
                onGuardar = { km ->
                    scope.launch { context.preferencias.edit { it[RADIO_KM] = km } }
                    editandoRadio = false
                },
            )
        }
    }
}

@Composable
private fun RadioDialog(actual: Int, onCerrar: () -> Unit, onGuardar: (Int) -> Unit) {
    var valor by remember { mutableFloatStateOf(actual.toFloat()) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Radio de búsqueda") },
        text = {
            Column {
                Text("${valor.roundToInt()} km")
                Slider(value = valor, onValueChange = { valor = it }, valueRange = 1f..20f, steps = 18)
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(valor.roundToInt()) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun Mapa(ubicacion: GeoPoint?, radioKm: Int?, lugares: List<Lugar>, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setMultiTouchControls(true)
                controller.setZoom(5.0)
                controller.setCenter(GeoPoint(4.6, -74.08)) // Colombia mientras llega el GPS
            }
        },
        onRelease = { it.onDetach() },
        update = { map ->
            InfoWindow.closeAllInfoWindowsOn(map) // si no, la burbuja del marcador viejo queda flotando
            map.overlays.clear()
            if (ubicacion != null && radioKm != null) {
                val circulo = Polygon(map).apply {
                    points = Polygon.pointsAsCircle(ubicacion, radioKm * 1000.0)
                    fillPaint.color = Color.argb(30, 33, 150, 243)
                    outlinePaint.color = Color.rgb(33, 150, 243)
                    outlinePaint.strokeWidth = 3f
                    infoWindow = null // que tocar el círculo no abra una burbuja vacía
                }
                map.overlays += circulo
                map.overlays += Marker(map).apply {
                    position = ubicacion
                    title = "Tu ubicación"
                    icon = ContextCompat.getDrawable(map.context, org.osmdroid.library.R.drawable.person)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                // Al tocar cada marcador se muestra el nombre del lugar.
                lugares.forEach { lugar ->
                    map.overlays += Marker(map).apply {
                        position = GeoPoint(lugar.lat, lugar.lon)
                        title = lugar.nombre
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                }
                val encuadrar = { map.zoomToBoundingBox(circulo.bounds, false, 48) }
                if (map.isLayoutOccurred) encuadrar() else map.addOnFirstLayoutListener { _, _, _, _, _ -> encuadrar() }
            }
            map.invalidate()
        },
    )
}
