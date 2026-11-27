package utez.edu.mx.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


// primary = botones, background = fondo, surface... = tarjetas, outline = bordes, etc.
private val ColoresPanaderia = lightColorScheme(
    primary = Cafe,
    onPrimary = Blanco,
    primaryContainer = Crema,
    onPrimaryContainer = CafeOscuro,
    secondary = CafeClaro,
    onSecondary = Blanco,
    background = Blanco,
    onBackground = CafeOscuro,
    surface = Blanco,
    onSurface = CafeOscuro,
    onSurfaceVariant = CafeClaro,
    // Tonos que usan las tarjetas (Card) y los menus desplegables
    surfaceContainerLowest = Blanco,
    surfaceContainerLow = Crema,
    surfaceContainer = Crema,
    surfaceContainerHigh = Crema,
    surfaceContainerHighest = Crema,
    outline = Arena,
    outlineVariant = Arena,
    error = Rojo
)

// Esquinas redondeadas para tarjetas, botones y campos de texto
private val Formas = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

// Tema de la app. Siempre usamos los colores blanco y cafe
// (no cambia con el modo oscuro ni con el fondo de pantalla del telefono).
@Composable
fun IntegradoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColoresPanaderia,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}
