package co.edu.unal.reto8.data

data class Empresa(
    val id: Long = 0,
    val nombre: String,
    val url: String = "",
    val telefono: String = "",
    val email: String = "",
    val productosServicios: String = "",
    val consultoria: Boolean = false,
    val desarrolloMedida: Boolean = false,
    val fabricaSoftware: Boolean = false
) {
    fun clasificaciones(): List<String> = buildList {
        if (consultoria) add(Clasificacion.CONSULTORIA.etiqueta)
        if (desarrolloMedida) add(Clasificacion.DESARROLLO_MEDIDA.etiqueta)
        if (fabricaSoftware) add(Clasificacion.FABRICA_SOFTWARE.etiqueta)
    }
}

enum class Clasificacion(val etiqueta: String, val columna: String?) {
    TODAS("Todas", null),
    CONSULTORIA("Consultoría", EmpresaDbHelper.COL_CONSULTORIA),
    DESARROLLO_MEDIDA("Desarrollo a la medida", EmpresaDbHelper.COL_DESARROLLO),
    FABRICA_SOFTWARE("Fábrica de software", EmpresaDbHelper.COL_FABRICA)
}
