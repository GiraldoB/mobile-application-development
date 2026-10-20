package co.edu.unal.reto8.data

import android.content.ContentValues
import android.database.Cursor
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_CONSULTORIA
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_DESARROLLO
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_EMAIL
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_FABRICA
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_ID
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_NOMBRE
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_PRODUCTOS
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_TELEFONO
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.COL_URL
import co.edu.unal.reto8.data.EmpresaDbHelper.Companion.TABLE

/** Centraliza todas las operaciones SQL sobre la tabla de empresas. */
class EmpresaRepository(private val helper: EmpresaDbHelper) {

    private val orden = "$COL_NOMBRE COLLATE NOCASE ASC"

    fun insertar(empresa: Empresa): Long =
        helper.writableDatabase.insertOrThrow(TABLE, null, valores(empresa))

    fun actualizar(empresa: Empresa): Int =
        helper.writableDatabase.update(
            TABLE, valores(empresa), "$COL_ID = ?", arrayOf(empresa.id.toString())
        )

    fun eliminar(id: Long): Int =
        helper.writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))

    fun obtenerPorId(id: Long): Empresa? =
        helper.readableDatabase.query(
            TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null
        ).use { c -> if (c.moveToFirst()) leer(c) else null }

    fun obtenerTodas(): List<Empresa> = buscar("", Clasificacion.TODAS)

    /** Filtra por coincidencia parcial de nombre y, opcionalmente, por clasificación. */
    fun buscar(texto: String, clasificacion: Clasificacion): List<Empresa> {
        val condiciones = mutableListOf<String>()
        val args = mutableListOf<String>()
        val termino = texto.trim()
        if (termino.isNotEmpty()) {
            condiciones += "$COL_NOMBRE LIKE ? ESCAPE '\\'"
            args += "%" + escaparLike(termino) + "%"
        }
        clasificacion.columna?.let { condiciones += "$it = 1" }
        val where = condiciones.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        return helper.readableDatabase.query(
            TABLE, null, where, args.toTypedArray().takeIf { it.isNotEmpty() },
            null, null, orden
        ).use { c ->
            buildList { while (c.moveToNext()) add(leer(c)) }
        }
    }

    private fun escaparLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun valores(e: Empresa) = ContentValues().apply {
        put(COL_NOMBRE, e.nombre.trim())
        put(COL_URL, e.url.trim())
        put(COL_TELEFONO, e.telefono.trim())
        put(COL_EMAIL, e.email.trim())
        put(COL_PRODUCTOS, e.productosServicios.trim())
        put(COL_CONSULTORIA, if (e.consultoria) 1 else 0)
        put(COL_DESARROLLO, if (e.desarrolloMedida) 1 else 0)
        put(COL_FABRICA, if (e.fabricaSoftware) 1 else 0)
    }

    private fun leer(c: Cursor): Empresa {
        fun str(col: String) = c.getString(c.getColumnIndexOrThrow(col)) ?: ""
        fun bool(col: String) = c.getInt(c.getColumnIndexOrThrow(col)) == 1
        return Empresa(
            id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
            nombre = str(COL_NOMBRE),
            url = str(COL_URL),
            telefono = str(COL_TELEFONO),
            email = str(COL_EMAIL),
            productosServicios = str(COL_PRODUCTOS),
            consultoria = bool(COL_CONSULTORIA),
            desarrolloMedida = bool(COL_DESARROLLO),
            fabricaSoftware = bool(COL_FABRICA)
        )
    }
}
