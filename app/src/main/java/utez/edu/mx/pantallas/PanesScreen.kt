package utez.edu.mx.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import utez.edu.mx.datos.Pan
import utez.edu.mx.datos.RetrofitClient

// Precio minimo de un pan: $2.50 (lo que cuesta un bolillo o una telera)
const val PRECIO_MINIMO = 2.50

// READ y DELETE de panes: muestra el catalogo en una cuadricula de 2 columnas.
// Los botones "Nuevo" y "Editar" abren el formulario (CREATE y UPDATE).
@Composable
fun PanesScreen(alVolver: () -> Unit, abrirFormulario: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var panes by remember { mutableStateOf(listOf<Pan>()) }
    // Pan que el usuario quiere borrar (null = no se muestra la confirmacion)
    var panAEliminar by remember { mutableStateOf<Pan?>(null) }

    // Pide la lista de panes al servidor
    fun cargar() {
        scope.launch {
            try {
                panes = RetrofitClient.api.obtenerPanes()
            } catch (e: Exception) {
                mostrarMensaje(context, mensajeDeError(e, "Error al cargar los panes"))
            }
        }
    }

    // LaunchedEffect(Unit): se ejecuta una vez cuando se abre la pantalla
    LaunchedEffect(Unit) { cargar() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Encabezado("Panes", alVolver)
        // id 0 significa "pan nuevo"
        Button(onClick = { abrirFormulario(0) }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Nuevo pan")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (panes.isEmpty()) "No hay panes registrados" else "${panes.size} panes en el catalogo",
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))

        // Cuadricula de 2 columnas: cada pan es una tarjeta con su foto arriba
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(panes) { pan ->
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        FotoBase64(pan.foto, 0, Modifier.fillMaxWidth().height(120.dp))
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                pan.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis // "..." si el nombre es muy largo
                            )
                            Text(
                                "$${"%.2f".format(pan.precio)} c/u",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                TextButton(
                                    onClick = { abrirFormulario(pan.id ?: 0L) },
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) { Text("Editar") }
                                // No borra directo: primero pide confirmacion
                                TextButton(
                                    onClick = { panAEliminar = pan },
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Ventana de confirmacion antes de eliminar
    val pan = panAEliminar
    if (pan != null) {
        DialogoConfirmar(
            mensaje = "Eliminar el pan \"${pan.nombre}\"?",
            alConfirmar = {
                panAEliminar = null
                scope.launch {
                    try {
                        RetrofitClient.api.eliminarPan(pan.id ?: 0L)
                        mostrarMensaje(context, "Pan eliminado")
                        cargar() // recargamos la lista
                    } catch (e: Exception) {
                        mostrarMensaje(context, mensajeDeError(e, "No se pudo eliminar"))
                    }
                }
            },
            alCancelar = { panAEliminar = null }
        )
    }
}

// CREATE y UPDATE de un pan. Si id es 0 se crea uno nuevo, si no se edita.
@Composable
fun PanFormScreen(id: Long, alTerminar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<String?>(null) }
    // true mientras se guarda: desactiva el boton para no guardar dos veces
    var guardando by remember { mutableStateOf(false) }

    // Si estamos editando, cargamos los datos actuales del pan
    LaunchedEffect(id) {
        if (id != 0L) {
            try {
                val pan = RetrofitClient.api.obtenerPan(id)
                nombre = pan.nombre
                precio = pan.precio.toString()
                foto = pan.foto
            } catch (e: Exception) {
                mostrarMensaje(context, mensajeDeError(e, "Error al cargar el pan"))
            }
        }
    }

    // verticalScroll: permite deslizar si no cabe (por ejemplo con el telefono acostado)
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Encabezado(if (id == 0L) "Nuevo pan" else "Editar pan", alTerminar)

        // Sensor de camara para la foto del pan
        CampoFoto(foto) { nuevaFoto -> foto = nuevaFoto }

        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre del pan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            precio, { precio = it },
            label = { Text("Precio (minimo $2.50)") },
            singleLine = true,
            // Teclado solo de numeros con punto decimal
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Button(enabled = !guardando, modifier = Modifier.fillMaxWidth(), onClick = {
            // Aceptamos "4,5" o "4.5". toDoubleOrNull regresa null si no es un numero
            val precioNumero = precio.trim().replace(',', '.').toDoubleOrNull()
            if (nombre.isBlank()) {
                mostrarMensaje(context, "Escribe el nombre del pan")
                return@Button
            }
            if (precioNumero == null || precioNumero < PRECIO_MINIMO) {
                mostrarMensaje(context, "El precio minimo de un pan es $2.50")
                return@Button
            }
            val pan = Pan(nombre = nombre.trim(), precio = precioNumero, foto = foto)
            guardando = true
            scope.launch {
                try {
                    if (id == 0L) RetrofitClient.api.crearPan(pan)
                    else RetrofitClient.api.actualizarPan(id, pan)
                    mostrarMensaje(context, "Pan guardado")
                    alTerminar()
                } catch (e: Exception) {
                    // Muestra el mensaje del servidor, por ejemplo "Ya existe un pan con ese nombre"
                    mostrarMensaje(context, mensajeDeError(e, "No se pudo guardar"))
                }
                guardando = false
            }
        }) { Text(if (guardando) "Guardando..." else "Guardar") }
    }
}
