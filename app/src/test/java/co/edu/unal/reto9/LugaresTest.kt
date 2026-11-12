package co.edu.unal.reto9

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LugaresTest {

    @Test
    fun parseLugares_leeCoordenadasDeTextoYDescartaSinNombre() {
        val json = """
            [
              {"lat": "4.6", "lon": "-74.1", "name": "Hospital A", "type": "hospital"},
              {"lat": "4.7", "lon": "-74.2", "name": "", "type": "hospital"},
              {"lat": "4.8", "lon": "-74.3", "name": null, "type": "hospital"},
              {"lat": "4.9", "lon": "-74.4", "type": "hospital"}
            ]
        """
        assertEquals(listOf(Lugar("Hospital A", 4.6, -74.1)), parseLugares(json))
    }

    @Test
    fun urlBusqueda_cuadradoQueContieneElRadioSinNotacionCientifica() {
        // Cerca del ecuador 1 grado ≈ 111.32 km en ambos ejes.
        val url = urlBusqueda(Categoria.MUSEO, 0.0001, -74.0, 2)
        assertTrue(url, url.contains("&q=museo&"))
        assertTrue(url, !url.contains("E")) // 0.0001 no debe salir como 1.0E-4
        val caja = url.substringAfter("viewbox=").split(",").map { it.toDouble() }.toDoubleArray()
        val d = 2 / 111.32
        assertArrayEquals(doubleArrayOf(-74.0 - d, 0.0001 + d, -74.0 + d, 0.0001 - d), caja, 1e-6)
    }
}
