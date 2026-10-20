package co.edu.unal.reto8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import co.edu.unal.reto8.data.EmpresaDbHelper
import co.edu.unal.reto8.data.EmpresaRepository
import co.edu.unal.reto8.ui.DirectorioApp
import co.edu.unal.reto8.ui.theme.Reto8Theme

class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: EmpresaDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dbHelper = EmpresaDbHelper(this)
        val repo = EmpresaRepository(dbHelper)
        setContent {
            Reto8Theme {
                DirectorioApp(repo)
            }
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}
