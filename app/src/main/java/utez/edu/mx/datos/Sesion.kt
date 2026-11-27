package utez.edu.mx.datos

// Guarda en memoria al usuario que inicio sesion,
// asi cualquier pantalla puede saber su id y su rol.
object Sesion {
    var usuario: Usuario? = null

    // Hora (en milisegundos) en la que el usuario toco la pantalla por ultima vez
    var ultimaActividad: Long = System.currentTimeMillis()

    // Tiempo maximo sin usar la app antes de cerrar la sesion: 1 hora
    const val TIEMPO_MAXIMO = 60 * 60 * 1000L

    // Regresa true si ya paso mas de 1 hora sin usar la app
    fun expirada(): Boolean {
        return System.currentTimeMillis() - ultimaActividad > TIEMPO_MAXIMO
    }
}
