package utez.edu.mx.datos

// Estas clases tienen los mismos campos que las tablas de MySQL.
// Retrofit (con Gson) convierte el JSON del servidor en estos objetos y al reves.

data class Pan(
    val id: Long? = null,
    val nombre: String = "",
    val precio: Double = 0.0,
    val foto: String? = null      // foto en texto Base64
)

data class Usuario(
    val id: Long? = null,
    val nombre: String = "",
    val apellidos: String? = null,
    val correo: String? = null,
    val contrasena: String? = null,
    val rol: String = "",         // ADMIN, TRABAJADOR o CLIENTE
    val nss: String? = null,      // solo trabajador: numero de nomina (6 numeros)
    val sucursal: String? = null, // solo cliente
    val localizacion: String? = null, // solo cliente
    val repartidorId: Long? = null,   // solo cliente: su repartidor (null = solicitud pendiente)
    val foto: String? = null      // solo trabajador
)

// Nombre y apellidos juntos, por ejemplo "Jaime Pacheco Martinez"
fun Usuario.nombreCompleto(): String = listOfNotNull(nombre, apellidos).joinToString(" ").trim()

data class Pedido(
    val id: Long? = null,
    val clienteId: Long? = null,
    val clienteNombre: String = "",
    val trabajadorId: Long? = null,
    val detalle: String = "",     // ejemplo: "Bolillo x10, Concha x5"
    val totalPiezas: Int = 0,
    val total: Double = 0.0,
    val estado: String = "PENDIENTE",
    val montoEntregado: Double = 0.0, // dinero que el repartidor ya le entrego al admin
    val pagoCliente: Double = 0.0,    // dinero que el cliente ya le pago al repartidor
    val fecha: String = ""
)

// Lo que el cliente todavia debe de este pedido (solo cuenta si ya se entrego)
fun Pedido.faltaCliente(): Double = if (estado == "ENTREGADO") total - pagoCliente else 0.0

// Lo que el repartidor todavia le debe al admin de este pedido (solo si ya se entrego)
fun Pedido.deudaRepartidor(): Double = if (estado == "ENTREGADO") total - montoEntregado else 0.0

// Datos que mandamos al hacer login
data class LoginRequest(
    val correo: String,
    val contrasena: String
)
