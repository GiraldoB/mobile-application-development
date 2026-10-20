package co.edu.unal.reto8.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EmpresaDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL CHECK (length(trim($COL_NOMBRE)) > 0),
                $COL_URL TEXT,
                $COL_TELEFONO TEXT,
                $COL_EMAIL TEXT,
                $COL_PRODUCTOS TEXT,
                $COL_CONSULTORIA INTEGER NOT NULL DEFAULT 0 CHECK ($COL_CONSULTORIA IN (0, 1)),
                $COL_DESARROLLO INTEGER NOT NULL DEFAULT 0 CHECK ($COL_DESARROLLO IN (0, 1)),
                $COL_FABRICA INTEGER NOT NULL DEFAULT 0 CHECK ($COL_FABRICA IN (0, 1)),
                CHECK ($COL_CONSULTORIA + $COL_DESARROLLO + $COL_FABRICA >= 1)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versión inicial: no hay migraciones todavía.
    }

    companion object {
        const val DB_NAME = "directorio.db"
        const val DB_VERSION = 1
        const val TABLE = "empresas"
        const val COL_ID = "id"
        const val COL_NOMBRE = "nombre"
        const val COL_URL = "url"
        const val COL_TELEFONO = "telefono"
        const val COL_EMAIL = "email"
        const val COL_PRODUCTOS = "productos_servicios"
        const val COL_CONSULTORIA = "consultoria"
        const val COL_DESARROLLO = "desarrollo_medida"
        const val COL_FABRICA = "fabrica_software"
    }
}
