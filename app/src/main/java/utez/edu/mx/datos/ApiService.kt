package utez.edu.mx.datos

import retrofit2.http.*

// Aqui se declaran las rutas de la API REST de Spring Boot.
// Retrofit se encarga de hacer la peticion HTTP y convertir el JSON.
// "suspend" significa que la funcion se ejecuta en segundo plano sin congelar la pantalla.
interface ApiService {

    // ---------- Login ----------
    @POST("api/login")
    suspend fun login(@Body datos: LoginRequest): Usuario

    // ---------- CRUD de Panes ----------
    @GET("api/panes")
    suspend fun obtenerPanes(): List<Pan>

    @GET("api/panes/{id}")
    suspend fun obtenerPan(@Path("id") id: Long): Pan

    @POST("api/panes")
    suspend fun crearPan(@Body pan: Pan): Pan

    @PUT("api/panes/{id}")
    suspend fun actualizarPan(@Path("id") id: Long, @Body pan: Pan): Pan

    @DELETE("api/panes/{id}")
    suspend fun eliminarPan(@Path("id") id: Long)

    // ---------- CRUD de Usuarios (trabajadores y clientes) ----------
    // Si repartidorId no es null, solo trae los clientes de ese repartidor
    @GET("api/usuarios")
    suspend fun obtenerUsuarios(
        @Query("rol") rol: String,
        @Query("repartidorId") repartidorId: Long? = null
    ): List<Usuario>

    // Clientes que se registraron solos y esperan que el admin les asigne repartidor
    @GET("api/usuarios/solicitudes")
    suspend fun obtenerSolicitudes(): List<Usuario>

    // El admin asigna (o cambia) el repartidor de un cliente
    @PUT("api/usuarios/{id}/repartidor")
    suspend fun asignarRepartidor(@Path("id") clienteId: Long, @Query("repartidorId") repartidorId: Long): Usuario

    @GET("api/usuarios/{id}")
    suspend fun obtenerUsuario(@Path("id") id: Long): Usuario

    @POST("api/usuarios")
    suspend fun crearUsuario(@Body usuario: Usuario): Usuario

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(@Path("id") id: Long, @Body usuario: Usuario): Usuario

    @DELETE("api/usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: Long)

    // ---------- CRUD de Pedidos ----------
    // Si clienteId o trabajadorId son null, Retrofit no los manda en la URL
    @GET("api/pedidos")
    suspend fun obtenerPedidos(
        @Query("clienteId") clienteId: Long? = null,
        @Query("trabajadorId") trabajadorId: Long? = null
    ): List<Pedido>

    @GET("api/pedidos/{id}")
    suspend fun obtenerPedido(@Path("id") id: Long): Pedido

    @POST("api/pedidos")
    suspend fun crearPedido(@Body pedido: Pedido): Pedido

    @PUT("api/pedidos/{id}")
    suspend fun actualizarPedido(@Path("id") id: Long, @Body pedido: Pedido): Pedido

    @DELETE("api/pedidos/{id}")
    suspend fun eliminarPedido(@Path("id") id: Long)
}
