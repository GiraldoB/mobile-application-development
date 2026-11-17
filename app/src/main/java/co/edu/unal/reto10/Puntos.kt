package co.edu.unal.reto10

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Un punto de recolección de residuos posconsumo publicado en datos.gov.co. */
data class Punto(
    val establecimiento: String,
    val direccion: String,
    val municipio: String,
    val programa: String,
    val residuos: String,
)

private const val SERVICIO = "https://www.datos.gov.co/resource/c2fr-ezse.json"
private const val LIMITE = 50

/**
 * URL del servicio con los parámetros de la consulta.
 * El servicio (Socrata) recibe el texto libre en `$q` y el número de filas en `$limit`.
 */
fun urlConsulta(texto: String, limite: Int = LIMITE): String {
    val busqueda = texto.trim()
    val base = "$SERVICIO?\$limit=$limite"
    return if (busqueda.isEmpty()) base else "$base&\$q=${URLEncoder.encode(busqueda, "UTF-8")}"
}

/** Convierte la respuesta JSON del servicio en la lista de puntos. */
fun parsePuntos(json: String): List<Punto> {
    val raiz = JSONArray(json)
    return (0 until raiz.length()).mapNotNull { i ->
        val objeto = raiz.optJSONObject(i) ?: return@mapNotNull null
        val establecimiento = objeto.optString("raz_n_social").trim()
        if (establecimiento.isEmpty()) return@mapNotNull null
        Punto(
            establecimiento = establecimiento,
            direccion = objeto.optString("direccion").trim(),
            municipio = objeto.optString("municipio").trim(),
            programa = objeto.optString("programa_de_recolecci_n").trim(),
            residuos = objeto.optString("tipo_de_residuos").trim(),
        )
    }
}

/** Consulta el servicio y devuelve los puntos encontrados. */
suspend fun consultar(texto: String): List<Punto> = withContext(Dispatchers.IO) {
    val conexion = URL(urlConsulta(texto)).openConnection() as HttpURLConnection
    try {
        conexion.connectTimeout = 10_000
        conexion.readTimeout = 10_000
        conexion.setRequestProperty("Accept", "application/json")
        val codigo = conexion.responseCode
        if (codigo != HttpURLConnection.HTTP_OK) throw IOException("El servicio respondió $codigo")
        parsePuntos(conexion.inputStream.bufferedReader().use { it.readText() })
    } finally {
        conexion.disconnect()
    }
}
