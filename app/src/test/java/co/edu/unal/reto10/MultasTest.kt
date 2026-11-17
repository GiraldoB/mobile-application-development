package co.edu.unal.reto10

import org.junit.Assert.assertEquals
import org.junit.Test

class MultasTest {

    @Test
    fun parseResumen_sumaPagadosYPendientes() {
        val json = """
            [
              {"pagado_si_no": "SI", "count_1": "2", "sum_valor_multa": "600000"},
              {"pagado_si_no": "NO", "count_1": "3", "sum_valor_multa": "1850000"}
            ]
        """
        assertEquals(Resumen(total = 5, monto = 2450000, pagados = 2, pendientes = 3), parseResumen(json))
    }

    @Test
    fun parseResumen_sinFilasEsTodoCero() {
        assertEquals(Resumen(0, 0, 0, 0), parseResumen("[]"))
    }

    @Test
    fun parseComparendos_mapeaCamposYDescartaSinFecha() {
        val json = """
            [
              {"fecha_multa": "10/06/2019", "valor_multa": "497000", "ciudad": "Soledad", "departamento": "Atlantico", "pagado_si_no": "SI"},
              {"fecha_multa": "", "valor_multa": "100", "ciudad": "Cali"},
              {"valor_multa": "200", "ciudad": "Cali"}
            ]
        """
        assertEquals(
            listOf(Comparendo("10/06/2019", 497000, "Soledad", "Atlantico", pagado = true)),
            parseComparendos(json),
        )
    }

    @Test
    fun urlComparendos_conSeisCaracteresBuscaLaPlacaExacta() {
        assertEquals(
            "https://www.datos.gov.co/resource/72nf-y4v3.json" +
                "?\$select=fecha_multa,valor_multa,ciudad,departamento,pagado_si_no" +
                "&\$where=placa='AQC74B'&\$order=fecha_multa&\$limit=50&\$offset=100",
            urlComparendos("aqc74b", 100),
        )
    }

    @Test
    fun urlComparendos_conTresLetrasBuscaPorPrefijo() {
        val url = urlComparendos("AQC", 0)
        assertEquals(true, url.contains("&\$where=starts_with(placa,'AQC')&"))
        assertEquals(true, url.endsWith("&\$limit=50&\$offset=0"))
    }

    @Test
    fun urlResumen_agrupaPorEstado() {
        val url = urlResumen(" aqc74b ")
        assertEquals(true, url.startsWith("https://www.datos.gov.co/resource/72nf-y4v3.json?\$select=pagado_si_no,count(1),sum(valor_multa)&"))
        assertEquals(true, url.endsWith("&\$where=placa='AQC74B'&\$group=pagado_si_no"))
    }

    @Test
    fun normalizarPlaca_pasaAMayusculasYQuitaEspacios() {
        assertEquals("AQC74B", normalizarPlaca(" aqc 74b "))
    }

    @Test
    fun ordenarPorFecha_ordenaPorFechaRealYNoComoTexto() {
        val comparendos = listOf(
            Comparendo("10/06/2019", 1, "", "", false),
            Comparendo("31/12/2019", 2, "", "", false),
            Comparendo("02/02/2020", 3, "", "", false),
        )
        assertEquals(listOf(3L, 2L, 1L), ordenarPorFecha(comparendos).map { it.valor })
    }

    @Test
    fun formatoPesos_usaSeparadorDeMiles() {
        assertEquals("$2.450.000", formatoPesos(2450000))
    }
}
