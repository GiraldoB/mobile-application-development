package co.edu.unal.reto10

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** Un comparendo reportado en el SIMIT. */
data class Comparendo(
    val fecha: String,
    val valor: Long,
    val ciudad: String,
    val departamento: String,
    val pagado: Boolean,
)

/** Totales de los comparendos encontrados para una placa o un prefijo. */
data class Resumen(
    val total: Int,
    val monto: Long,
    val pagados: Int,
    val pendientes: Int,
)

private const val SERVICIO = "https://www.datos.gov.co/resource/72nf-y4v3.json"

const val TAMANO_PAGINA = 50

/** Quita espacios y pasa a mayúsculas: el servicio distingue mayúsculas de minúsculas. */
fun normalizarPlaca(texto: String): String =
    texto.trim().uppercase().filter { it.isLetterOrDigit() }

private fun filtro(placa: String): String =
    if (placa.length == 6) "\$where=placa='$placa'"
    else "\$where=starts_with(placa,'$placa')"

/** URL del resumen: una fila por estado (pagado y pendiente) con su conteo y su monto. */
fun urlResumen(placa: String): String {
    val filtroPlaca = filtro(normalizarPlaca(placa))
    return "$SERVICIO?\$select=pagado_si_no,count(1),sum(valor_multa)&$filtroPlaca&\$group=pagado_si_no"
}

/** URL de una página de comparendos (50 filas desde [offset]). */
fun urlComparendos(placa: String, offset: Int): String {
    val filtroPlaca = filtro(normalizarPlaca(placa))
    return "$SERVICIO?\$select=fecha_multa,valor_multa,ciudad,departamento,pagado_si_no" +
        "&$filtroPlaca&\$order=fecha_multa&\$limit=$TAMANO_PAGINA&\$offset=$offset"
}

/** Convierte la respuesta del resumen en los totales. */
fun parseResumen(json: String): Resumen {
    val filas = JSONArray(json)
    var total = 0
    var monto = 0L
    var pagados = 0
    for (i in 0 until filas.length()) {
        val fila = filas.optJSONObject(i) ?: continue
        val cantidad = fila.optString("count_1").toIntOrNull() ?: 0
        total += cantidad
        monto += fila.optString("sum_valor_multa").toDoubleOrNull()?.toLong() ?: 0L
        if (fila.optString("pagado_si_no") == "SI") pagados += cantidad
    }
    return Resumen(total = total, monto = monto, pagados = pagados, pendientes = total - pagados)
}

/** Convierte la respuesta de comparendos en la lista. */
fun parseComparendos(json: String): List<Comparendo> {
    val filas = JSONArray(json)
    return (0 until filas.length()).mapNotNull { i ->
        val fila = filas.optJSONObject(i) ?: return@mapNotNull null
        val fecha = fila.optString("fecha_multa").trim()
        if (fecha.isEmpty()) return@mapNotNull null
        Comparendo(
            fecha = fecha,
            valor = fila.optString("valor_multa").toDoubleOrNull()?.toLong() ?: 0L,
            ciudad = fila.optString("ciudad").trim(),
            departamento = fila.optString("departamento").trim(),
            pagado = fila.optString("pagado_si_no") == "SI",
        )
    }
}

private fun fechaOrden(fecha: String): Long =
    try {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(fecha)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }

/** La fecha viene como texto dd/MM/yyyy, que no ordena bien como cadena. */
fun ordenarPorFecha(comparendos: List<Comparendo>): List<Comparendo> =
    comparendos.sortedByDescending { fechaOrden(it.fecha) }

/** Monto en pesos con separadores de miles: $2.450.000. */
fun formatoPesos(valor: Long): String =
    "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(valor)

private suspend fun pedir(url: String): String = withContext(Dispatchers.IO) {
    val conexion = URL(url).openConnection() as HttpURLConnection
    try {
        conexion.connectTimeout = 10_000
        conexion.readTimeout = 15_000
        conexion.setRequestProperty("Accept", "application/json")
        val codigo = conexion.responseCode
        if (codigo != HttpURLConnection.HTTP_OK) throw IOException("El servicio respondió $codigo")
        conexion.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conexion.disconnect()
    }
}

suspend fun consultarResumen(placa: String): Resumen = parseResumen(pedir(urlResumen(placa)))

suspend fun consultarComparendos(placa: String, offset: Int): List<Comparendo> =
    parseComparendos(pedir(urlComparendos(placa, offset)))
