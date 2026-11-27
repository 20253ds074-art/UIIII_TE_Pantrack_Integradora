package utez.edu.mx.datos

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit



object RetrofitClient {

    const val SERVIDOR_CABLE = "http://localhost:8080/"

    // Direccion actual del servidor ("private set": solo este objeto la puede cambiar)
    var baseUrl: String = SERVIDOR_CABLE
        private set

    // Conexion con el servidor. Se vuelve a crear cuando cambia la direccion.
    var api: ApiService = crear(SERVIDOR_CABLE)
        private set

    // Crea Retrofit con la direccion indicada
    private fun crear(url: String): ApiService {
        // Si el servidor no contesta en 5 segundos, se muestra el aviso (no se queda esperando)
        val cliente = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create()) // convierte JSON <-> objetos
            .build()
            .create(ApiService::class.java)
    }

    // Cambia la direccion del servidor y la guarda en el telefono
    fun cambiarServidor(context: Context, url: String) {
        baseUrl = url
        api = crear(url)
        preferencias(context).edit().putString("servidor", url).apply()
    }

    // Al abrir la app, usa la ultima direccion guardada (o el cable si no hay ninguna)
    fun cargarServidorGuardado(context: Context) {
        val guardado = preferencias(context).getString("servidor", SERVIDOR_CABLE) ?: SERVIDOR_CABLE
        baseUrl = guardado
        api = crear(guardado)
    }

    // SharedPreferences: guarda datos pequenos en el telefono
    private fun preferencias(context: Context) =
        context.getSharedPreferences("configuracion", Context.MODE_PRIVATE)

    // Convierte lo que escribe el usuario ("192.168.43.20") en una direccion completa
    fun direccionDesdeIp(ip: String): String = "http://${ip.trim()}:8080/"

    // Texto corto para mostrar en pantalla
    fun descripcion(): String =
        if (baseUrl == SERVIDOR_CABLE) "Por cable USB" else "Wi-Fi: " + baseUrl.removePrefix("http://").removeSuffix("/")
}
