package utez.edu.mx

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import android.view.MotionEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import utez.edu.mx.datos.RetrofitClient
import utez.edu.mx.datos.Sesion
import utez.edu.mx.datos.SesionRepositorio
import utez.edu.mx.ui.theme.IntegradoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barra de arriba (hora, bateria) con iconos oscuros, porque el fondo de la app es blanco
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        // Usa la direccion del servidor que se guardo la ultima vez (cable o Wi-Fi)
        RetrofitClient.cargarServidorGuardado(this)
        setContent {
            IntegradoraTheme {
                // Scaffold nos da el espacio para no tapar la barra de estado del telefono
                Scaffold(modifier = Modifier.fillMaxSize()) { espacio ->
                    Navegacion(modifier = Modifier.padding(espacio))
                }
            }
        }
    }

    // Se llama cada vez que el usuario toca la pantalla:
    // guardamos la hora para saber cuando fue la ultima vez que uso la app
    override fun dispatchTouchEvent(evento: MotionEvent): Boolean {
        Sesion.ultimaActividad = System.currentTimeMillis()
        return super.dispatchTouchEvent(evento)
    }

    // Se llama cuando la app deja de verse (boton de inicio, cambiar de app, apagar pantalla).
    // Guardamos la sesion en el telefono para que al regresar NO pida login otra vez.
    override fun onStop() {
        super.onStop()
        val repositorio = SesionRepositorio(applicationContext)
        lifecycleScope.launch {
            repositorio.guardar(Sesion.usuario, Sesion.ultimaActividad)
        }
    }
}
