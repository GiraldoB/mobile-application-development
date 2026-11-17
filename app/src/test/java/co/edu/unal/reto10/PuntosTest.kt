package co.edu.unal.reto10

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuntosTest {

    @Test
    fun parsePuntos_mapeaCamposYDescartaRegistrosSinEstablecimiento() {
        val json = """
            [
              {"raz_n_social": "Justo y Bueno", "direccion": "Av 3N # 38-42", "municipio": "San José de Cúcuta", "programa_de_recolecci_n": "Pilas con el Ambiente", "tipo_de_residuos": "Pilas"},
              {"raz_n_social": "  ", "direccion": "Calle 1"},
              {"direccion": "Calle 2"}
            ]
        """
        assertEquals(
            listOf(
                Punto(
                    establecimiento = "Justo y Bueno",
                    direccion = "Av 3N # 38-42",
                    municipio = "San José de Cúcuta",
                    programa = "Pilas con el Ambiente",
                    residuos = "Pilas",
                )
            ),
            parsePuntos(json),
        )
    }

    @Test
    fun urlConsulta_enviaElTextoComoParametroCodificado() {
        assertEquals(
            "https://www.datos.gov.co/resource/c2fr-ezse.json?\$limit=10&\$q=Oca%C3%B1a",
            urlConsulta("Ocaña", 10),
        )
    }

    @Test
    fun urlConsulta_sinTextoNoEnviaParametroDeBusqueda() {
        val url = urlConsulta("   ")
        assertTrue(url, url.endsWith("?\$limit=50"))
    }
}
