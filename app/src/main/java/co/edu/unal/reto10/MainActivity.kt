package co.edu.unal.reto10

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(primary = Color(0xFF37474F), onPrimary = Color.White)
private val Oscuro = darkColorScheme(primary = Color(0xFFB0BEC5), onPrimary = Color(0xFF1B1B1B))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro) {
                MultasScreen()
            }
        }
    }
}
