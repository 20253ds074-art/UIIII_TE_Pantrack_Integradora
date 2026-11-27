package utez.edu.mx.datos

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first


// DataStore guarda datos pequenos en el telefono, aunque la app se cierre.
// Aqui guardamos quien inicio sesion y cuando uso la app por ultima vez.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sesion_usuario")

class SesionRepositorio(private val context: Context) {

    // Nombres de los datos que guardamos
    private object Llaves {
        val USUARIO = stringPreferencesKey("usuario")                 // el usuario en formato JSON
        val ULTIMA_ACTIVIDAD = longPreferencesKey("ultima_actividad") // hora del ultimo uso
    }

    // Guarda la sesion. Si usuario es null, borra la sesion.
    suspend fun guardar(usuario: Usuario?, ultimaActividad: Long) {
        if (usuario == null) {
            cerrar()
            return
        }
        context.dataStore.edit { datos ->
            // No guardamos la foto para que no ocupe tanto espacio
            datos[Llaves.USUARIO] = Gson().toJson(usuario.copy(foto = null))
            datos[Llaves.ULTIMA_ACTIVIDAD] = ultimaActividad
        }
    }

    // Lee la sesion guardada. Regresa null si no hay sesion
    // o si paso mas de 1 hora desde el ultimo uso (en ese caso la borra).
    suspend fun leer(): Usuario? {
        val datos = context.dataStore.data.first()
        val json = datos[Llaves.USUARIO] ?: return null
        val ultima = datos[Llaves.ULTIMA_ACTIVIDAD] ?: 0L
        if (System.currentTimeMillis() - ultima > Sesion.TIEMPO_MAXIMO) {
            cerrar()
            return null
        }
        return Gson().fromJson(json, Usuario::class.java)
    }

    // Borra todo (cerrar sesion)
    suspend fun cerrar() {
        context.dataStore.edit { datos -> datos.clear() }
    }
}
