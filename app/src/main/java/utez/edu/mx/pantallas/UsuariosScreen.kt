package utez.edu.mx.pantallas

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import utez.edu.mx.datos.RetrofitClient
import utez.edu.mx.datos.Sesion
import utez.edu.mx.datos.Usuario
import utez.edu.mx.datos.nombreCompleto
import utez.edu.mx.ui.theme.Rojo

// READ y DELETE de usuarios. La misma pantalla sirve para
// la lista de TRABAJADORES y la lista de CLIENTES (depende de "rol").
// - El ADMIN ve todos los clientes y puede cambiarles el repartidor, pero NO registra clientes.
// - El REPARTIDOR solo ve SUS clientes y puede registrar clientes nuevos (quedan asignados a el).
@Composable
fun UsuariosScreen(rol: String, alVolver: () -> Unit, abrirFormulario: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val yo = Sesion.usuario
    val esAdmin = yo?.rol == "ADMIN"

    var usuarios by remember { mutableStateOf(listOf<Usuario>()) }
    // El admin necesita la lista de repartidores para mostrar y cambiar el de cada cliente
    var repartidores by remember { mutableStateOf(listOf<Usuario>()) }
    // Usuario que se quiere borrar (null = no se muestra la confirmacion)
    var aEliminar by remember { mutableStateOf<Usuario?>(null) }

    fun cargar() {
        scope.launch {
            try {
                usuarios = if (rol == "CLIENTE" && yo?.rol == "TRABAJADOR") {
                    RetrofitClient.api.obtenerUsuarios("CLIENTE", yo.id) // solo mis clientes
                } else {
                    RetrofitClient.api.obtenerUsuarios(rol)
                }
                if (esAdmin && rol == "CLIENTE") {
                    repartidores = RetrofitClient.api.obtenerUsuarios("TRABAJADOR")
                }
            } catch (e: Exception) {
                mostrarMensaje(context, mensajeDeError(e, "Error al cargar la lista"))
            }
        }
    }

    // El admin cambia el repartidor de un cliente
    fun asignar(cliente: Usuario, repartidor: Usuario) {
        scope.launch {
            try {
                RetrofitClient.api.asignarRepartidor(cliente.id ?: 0L, repartidor.id ?: 0L)
                mostrarMensaje(context, "Ahora lo atiende ${repartidor.nombreCompleto()}")
                cargar()
            } catch (e: Exception) {
                mostrarMensaje(context, mensajeDeError(e, "No se pudo asignar"))
            }
        }
    }

    LaunchedEffect(Unit) { cargar() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Encabezado(
            when {
                rol == "TRABAJADOR" -> "Trabajadores"
                esAdmin -> "Clientes"
                else -> "Mis clientes"
            },
            alVolver
        )
        // El admin no registra clientes: los registra el repartidor o el cliente desde el login
        if (!(esAdmin && rol == "CLIENTE")) {
            Button(onClick = { abrirFormulario(0) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (rol == "TRABAJADOR") "+ Nuevo trabajador" else "+ Nuevo cliente")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (usuarios.isEmpty()) Text("No hay registros")

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(usuarios) { usuario ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (rol == "TRABAJADOR") {
                                FotoBase64(usuario.foto, 60)
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                if (rol == "TRABAJADOR") {
                                    Text(usuario.nombreCompleto(), style = MaterialTheme.typography.titleMedium)
                                    Text("Nomina: ${usuario.nss ?: ""}")
                                } else {
                                    Text(usuario.sucursal ?: "", style = MaterialTheme.typography.titleMedium)
                                    Text("Encargado: ${usuario.nombreCompleto()}")
                                    Text(usuario.localizacion ?: "")
                                }
                            }
                            TextButton(onClick = { abrirFormulario(usuario.id ?: 0L) }) { Text("Editar") }
                            // No borra directo: primero pide confirmacion
                            TextButton(onClick = { aEliminar = usuario }) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        // El admin ve y puede cambiar el repartidor de cada cliente
                        if (esAdmin && rol == "CLIENTE") {
                            val actual = repartidores.find { it.id == usuario.repartidorId }
                            if (actual == null) {
                                Text("Sin repartidor (solicitud pendiente)", color = Rojo)
                            }
                            Selector(
                                textoVacio = "Asignar repartidor",
                                opciones = repartidores.map { it.nombreCompleto() },
                                seleccion = actual?.let { "Repartidor: ${it.nombreCompleto()}" } ?: ""
                            ) { posicion -> asignar(usuario, repartidores[posicion]) }
                        }
                    }
                }
            }
        }
    }

    // Ventana de confirmacion antes de eliminar
    val usuario = aEliminar
    if (usuario != null) {
        DialogoConfirmar(
            mensaje = "Eliminar a \"${usuario.sucursal ?: usuario.nombreCompleto()}\"?",
            alConfirmar = {
                aEliminar = null
                scope.launch {
                    try {
                        RetrofitClient.api.eliminarUsuario(usuario.id ?: 0L)
                        mostrarMensaje(context, "Eliminado")
                        cargar()
                    } catch (e: Exception) {
                        mostrarMensaje(context, mensajeDeError(e, "No se pudo eliminar"))
                    }
                }
            },
            alCancelar = { aEliminar = null }
        )
    }
}

// SOLICITUDES (solo admin): clientes que se registraron solos desde el login.
// El admin elige que repartidor los atendera. Desde ahi, sus pedidos van solos a ese repartidor.
@Composable
fun SolicitudesScreen(alVolver: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var solicitudes by remember { mutableStateOf(listOf<Usuario>()) }
    var repartidores by remember { mutableStateOf(listOf<Usuario>()) }

    fun cargar() {
        scope.launch {
            try {
                solicitudes = RetrofitClient.api.obtenerSolicitudes()
                repartidores = RetrofitClient.api.obtenerUsuarios("TRABAJADOR")
            } catch (e: Exception) {
                mostrarMensaje(context, mensajeDeError(e, "Error al cargar las solicitudes"))
            }
        }
    }

    LaunchedEffect(Unit) { cargar() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Encabezado("Solicitudes de clientes", alVolver)
        Text("Elige el repartidor que atendera a cada cliente nuevo.", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(8.dp))
        if (solicitudes.isEmpty()) Text("No hay solicitudes pendientes")
        if (repartidores.isEmpty()) Text("Primero registra al menos un repartidor", color = Rojo)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(solicitudes) { cliente ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(cliente.sucursal ?: "", style = MaterialTheme.typography.titleMedium)
                        Text("Encargado: ${cliente.nombreCompleto()}")
                        Text(cliente.localizacion ?: "")
                        Text(cliente.correo ?: "", color = MaterialTheme.colorScheme.secondary)
                        Selector(
                            textoVacio = "Elegir repartidor",
                            opciones = repartidores.map { it.nombreCompleto() },
                            seleccion = ""
                        ) { posicion ->
                            val repartidor = repartidores[posicion]
                            scope.launch {
                                try {
                                    RetrofitClient.api.asignarRepartidor(cliente.id ?: 0L, repartidor.id ?: 0L)
                                    mostrarMensaje(context, "Asignado a ${repartidor.nombreCompleto()}")
                                    cargar() // la solicitud desaparece de la lista
                                } catch (e: Exception) {
                                    mostrarMensaje(context, mensajeDeError(e, "No se pudo asignar"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// CREATE y UPDATE de usuarios. Sirve para:
// - Alta y edicion de trabajadores (admin)
// - Alta de clientes por el repartidor (quedan asignados a el) y edicion de clientes
// - Registro de cliente nuevo desde el login (queda como solicitud para el admin)
// - "Mi perfil" del cliente o del trabajador
@Composable
fun UsuarioFormScreen(rol: String, id: Long, alTerminar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val yo = Sesion.usuario

    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var nss by remember { mutableStateOf("") }
    var sucursal by remember { mutableStateOf("") }
    var localizacion by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<String?>(null) }
    // true mientras se guarda: desactiva el boton para no guardar dos veces
    var guardando by remember { mutableStateOf(false) }

    // Casos donde la persona SI va a iniciar sesion, por eso el correo es obligatorio:
    // registro desde el login (nadie ha entrado), un trabajador, o "Mi perfil"
    val esMiPerfil = id != 0L && id == yo?.id
    val esRegistro = yo == null
    val necesitaCuenta = esRegistro || rol == "TRABAJADOR" || esMiPerfil

    // Si es edicion, cargamos los datos actuales
    LaunchedEffect(id) {
        if (id != 0L) {
            try {
                val u = RetrofitClient.api.obtenerUsuario(id)
                nombre = u.nombre
                apellidos = u.apellidos ?: ""
                correo = u.correo ?: ""
                // La contrasena no se carga: el servidor nunca la regresa por seguridad
                nss = u.nss ?: ""
                sucursal = u.sucursal ?: ""
                localizacion = u.localizacion ?: ""
                foto = u.foto
            } catch (e: Exception) {
                mostrarMensaje(context, mensajeDeError(e, "Error al cargar los datos"))
            }
        }
    }

    val titulo = when {
        esMiPerfil -> "Mi perfil"
        id != 0L -> "Editar"
        rol == "TRABAJADOR" -> "Nuevo trabajador"
        esRegistro -> "Registro de cliente"
        else -> "Nuevo cliente"
    }

    // verticalScroll: permite deslizar si el formulario no cabe en la pantalla
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Encabezado(titulo, alTerminar)

        if (rol == "TRABAJADOR") {
            // Sensor de camara para la foto del trabajador
            CampoFoto(foto) { nuevaFoto -> foto = nuevaFoto }
        } else {
            OutlinedTextField(sucursal, { sucursal = it }, label = { Text("Nombre de la sucursal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(
            nombre, { nombre = it },
            label = { Text(if (rol == "CLIENTE") "Nombre(s) del encargado" else "Nombre(s)") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(apellidos, { apellidos = it }, label = { Text("Apellidos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (rol == "TRABAJADOR") {
            OutlinedTextField(
                value = nss,
                // Solo acepta numeros y como maximo 6: se quitan letras, espacios y simbolos
                onValueChange = { texto -> nss = texto.filter { it.isDigit() }.take(6) },
                label = { Text("Numero de nomina (6 numeros)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(localizacion, { localizacion = it }, label = { Text("Localizacion / direccion") }, modifier = Modifier.fillMaxWidth())
        }

        Text("Datos para iniciar sesion")
        OutlinedTextField(
            correo, { correo = it },
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            contrasena, { contrasena = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            // Al editar, si se deja vacia se conserva la contrasena anterior
            label = { Text(if (id == 0L) "Contrasena" else "Nueva contrasena") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Minimo 10 caracteres, con mayuscula, minuscula, numero y un caracter especial (! @ # $ ...)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        if (esRegistro) {
            Text(
                "Al registrarte, el administrador te asignara un repartidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Button(enabled = !guardando, modifier = Modifier.fillMaxWidth(), onClick = {
            val correoLimpio = correo.trim().lowercase()
            val contrasenaLimpia = contrasena.trim()
            // ---------- Validaciones antes de mandar al servidor ----------
            if (rol == "CLIENTE" && sucursal.isBlank()) {
                mostrarMensaje(context, "El nombre de la sucursal es obligatorio")
                return@Button
            }
            if (nombre.isBlank() || apellidos.isBlank()) {
                mostrarMensaje(context, "Escribe nombre(s) y apellidos")
                return@Button
            }
            if (rol == "TRABAJADOR" && nss.length != 6) {
                mostrarMensaje(context, "El numero de nomina debe tener exactamente 6 numeros")
                return@Button
            }
            if (necesitaCuenta && correoLimpio.isBlank()) {
                mostrarMensaje(context, "El correo es obligatorio")
                return@Button
            }
            // Patterns.EMAIL_ADDRESS revisa que tenga forma de correo (algo@algo.com)
            if (correoLimpio.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(correoLimpio).matches()) {
                mostrarMensaje(context, "Escribe un correo valido")
                return@Button
            }
            // Al crear una cuenta con correo, la contrasena es obligatoria
            if (id == 0L && correoLimpio.isNotBlank() && contrasenaLimpia.isBlank()) {
                mostrarMensaje(context, "La contrasena es obligatoria")
                return@Button
            }
            // Si escribio contrasena, debe ser segura
            if (contrasenaLimpia.isNotBlank()) {
                val problema = problemaContrasena(contrasenaLimpia)
                if (problema != null) {
                    mostrarMensaje(context, problema)
                    return@Button
                }
            }
            val usuario = Usuario(
                nombre = nombre.trim(),
                apellidos = apellidos.trim(),
                correo = correoLimpio.ifBlank { null }, // minusculas y sin espacios
                contrasena = contrasenaLimpia.ifBlank { null },
                rol = rol,
                nss = nss.ifBlank { null },
                sucursal = sucursal.trim().ifBlank { null },
                localizacion = localizacion.trim().ifBlank { null },
                // Si un repartidor registra al cliente, queda asignado a el
                repartidorId = if (rol == "CLIENTE" && yo?.rol == "TRABAJADOR") yo.id else null,
                foto = foto
            )
            guardando = true
            scope.launch {
                try {
                    if (id == 0L) {
                        RetrofitClient.api.crearUsuario(usuario)
                    } else {
                        val guardado = RetrofitClient.api.actualizarUsuario(id, usuario)
                        // Si edito su propio perfil, actualizamos la sesion
                        if (esMiPerfil) Sesion.usuario = guardado
                    }
                    mostrarMensaje(
                        context,
                        if (esRegistro) "Registro enviado. El administrador te asignara un repartidor" else "Datos guardados"
                    )
                    alTerminar()
                } catch (e: Exception) {
                    // Muestra el mensaje del servidor, por ejemplo "Ese correo ya esta registrado"
                    mostrarMensaje(context, mensajeDeError(e, "No se pudo guardar"))
                }
                guardando = false
            }
        }) { Text(if (guardando) "Guardando..." else "Guardar") }
    }
}
