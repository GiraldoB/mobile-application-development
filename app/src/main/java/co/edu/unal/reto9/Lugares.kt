package co.edu.unal.reto9

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.cos

/** Tipos de punto de interés y la palabra con la que se buscan en Nominatim (OpenStreetMap). */
enum class Categoria(val etiqueta: String, val busqueda: String) {
    HOSPITAL("Hospitales", "hospital"),
    MUSEO("Museos", "museo"),
    PARQUE("Parques", "parque"),
    RESTAURANTE("Restaurantes", "restaurante"),
}

data class Lugar(val nombre: String, val lat: Double, val lon: Double)

/** URL de Nominatim que busca la categoría dentro del cuadrado que contiene el círculo del radio. */
fun urlBusqueda(categoria: Categoria, lat: Double, lon: Double, radioKm: Int): String {
    val dLat = radioKm / 111.32 // km por grado de latitud
    val dLon = radioKm / (111.32 * cos(Math.toRadians(lat)))
    return String.format(
        Locale.US,
        "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=40&bounded=1&q=%s&viewbox=%.6f,%.6f,%.6f,%.6f",
        URLEncoder.encode(categoria.busqueda, "UTF-8"), lon - dLon, lat + dLat, lon + dLon, lat - dLat,
    )
}

/** Extrae los lugares con nombre de la respuesta JSON de Nominatim. */
fun parseLugares(json: String): List<Lugar> {
    val resultados = JSONArray(json)
    return (0 until resultados.length()).mapNotNull { i ->
        val r = resultados.getJSONObject(i)
        val nombre = if (r.isNull("name")) "" else r.getString("name")
        if (nombre.isBlank()) null
        else Lugar(nombre, r.getString("lat").toDouble(), r.getString("lon").toDouble())
    }
}

suspend fun buscarLugares(categoria: Categoria, lat: Double, lon: Double, radioKm: Int): List<Lugar> =
    withContext(Dispatchers.IO) {
        val conexion = URL(urlBusqueda(categoria, lat, lon, radioKm)).openConnection() as HttpURLConnection
        val json = try {
            // La política de uso de Nominatim exige identificar la app.
            conexion.setRequestProperty("User-Agent", "co.edu.unal.reto9")
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 30_000
            conexion.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
        // Las esquinas del cuadrado quedan fuera del radio: se descartan.
        val distancia = FloatArray(1)
        parseLugares(json).filter {
            Location.distanceBetween(lat, lon, it.lat, it.lon, distancia)
            distancia[0] <= radioKm * 1000
        }
    }
