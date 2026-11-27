package utez.edu.mx.pantallas

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import utez.edu.mx.datos.*
import utez.edu.mx.ui.theme.Rojo
import utez.edu.mx.ui.theme.Verde

// Formato de dinero con 2 decimales, por ejemplo 12.5 -> "$12.50"
fun dinero(cantidad: Double): String = "$${"%.2f".format(cantidad)}"

// READ y DELETE de pedidos.
// - El ADMIN ve todos los pedidos y la carga y deuda de cada repartidor.
// - El REPARTIDOR ve sus pedidos, registra lo que le pagan los clientes y ve lo que debe al admin.
// - El CLIENTE ve sus pedidos (historial) y lo que le falta pagar.
@Composable
fun PedidosScreen(alVolver: () -> Unit, abrirEditar: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val yo = Sesion.usuario
    val rol = yo?.rol ?: ""

    var pedidos by remember { mutableStateOf(listOf<Pedido>()) }
    var trabajadores by remember { mutableStateOf(listOf<Usuario>()) }
    // Pedido que se quiere borrar o cancelar (null = no se muestra la confirmacion)
    var aEliminar by remember { mutableStateOf<Pedido?>(null) }
    // Pedido al que el cliente le pago solo una parte (abre la ventana para escribir cuanto)
    var aCobrar by remember { mutableStateOf<Pedido?>(null) }

    fun cargar() {
        scope.launch {
            try {
                pedidos = when (rol) {
                    "CLIENTE" -> RetrofitClient.api.obtenerPedidos(clienteId = yo?.id)
                    "TRABAJADOR" -> RetrofitClient.api.obtenerPedidos(trabajadorId = yo?.id)
                    else -> RetrofitClient.api.obtenerPedidos() // ADMIN: todos
                }
                if (rol == "ADMIN") {
                    trabajadores = RetrofitClient.api.obtenerUsuarios("TRABAJADOR")
                }
            } catch (e: Exception) {
                mostrarMensaje(context, mensajeDeError(e, "Error al cargar los pedidos"))
            }
        }
    }

    // El repartidor registra cuanto le pago el cliente
    fun registrarPago(pedido: Pedido, monto: Double) {
        scope.launch {
            try {
                // copy() crea una copia del pedido cambiando solo el pago del cliente
                RetrofitClient.api.actualizarPedido(pedido.id ?: 0L, pedido.copy(pagoCliente = monto))
                mostrarMensaje(context, "Pago registrado")
                cargar()
            } catch (e: Exception) {
                mostrarMensaje(context, mensajeDeError(e, "No se pudo registrar el pago"))
            }
        }
    }

    LaunchedEffect(Unit) { cargar() }

    // Conteo de panes: pendientes (en ruta) y entregados
    val panesPendientes = pedidos.filter { it.estado == "PENDIENTE" }.sumOf { it.totalPiezas }
    val panesEntregados = pedidos.filter { it.estado == "ENTREGADO" }.sumOf { it.totalPiezas }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Encabezado(if (rol == "ADMIN") "Pedidos y deudas" else "Mis pedidos", alVolver)

        // Todo va dentro de la lista para poder deslizar (tambien con el telefono acostado)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ---------- Conteo de panes ----------
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${pedidos.size} pedidos ·")
                    Text("$panesPendientes panes pendientes", color = Rojo)
                    Text("·")
                    Text("$panesEntregados entregados", color = Verde)
                }
            }

            // ---------- ADMIN: carga y deuda de cada repartidor ----------
            if (rol == "ADMIN" && trabajadores.isNotEmpty()) item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Repartidores: carga y deudas", style = MaterialTheme.typography.titleMedium)
                        Text("Ordenado de quien lleva MENOS panes a quien lleva mas", color = MaterialTheme.colorScheme.secondary)
                        // panes = piezas de sus pedidos PENDIENTES (lo que lleva en ruta)
                        // deuda = de sus pedidos ENTREGADOS, el total menos lo que ya entrego al admin
                        val resumen = trabajadores.map { t ->
                            val suyos = pedidos.filter { it.trabajadorId == t.id }
                            val panes = suyos.filter { it.estado == "PENDIENTE" }.sumOf { it.totalPiezas }
                            val deuda = suyos.sumOf { it.deudaRepartidor() }
                            Triple(t.nombreCompleto(), panes, deuda)
                        }.sortedBy { it.second }

                        resumen.forEach { (nombre, panes, deuda) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$nombre: $panes panes", Modifier.weight(1f))
                                // Rojo si debe dinero, verde si esta al corriente
                                if (deuda > 0.009) {
                                    Text("Debe ${dinero(deuda)}", color = Rojo, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Al corriente", color = Verde)
                                }
                            }
                        }
                    }
                }
            }

            // ---------- REPARTIDOR: cobros pendientes y deuda con el admin ----------
            if (rol == "TRABAJADOR") item {
                val porCobrar = pedidos.sumOf { it.faltaCliente() }
                val deboAdmin = pedidos.sumOf { it.deudaRepartidor() }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mis cuentas", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Por cobrar a clientes: ${dinero(porCobrar)}",
                            color = if (porCobrar > 0.009) Rojo else Verde
                        )
                        Text(
                            if (deboAdmin > 0.009) "Debo al administrador: ${dinero(deboAdmin)}" else "Estoy al corriente con el administrador",
                            color = if (deboAdmin > 0.009) Rojo else Verde,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ---------- CLIENTE: lo que le falta pagar ----------
            if (rol == "CLIENTE") item {
                val meFalta = pedidos.sumOf { it.faltaCliente() }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if (meFalta > 0.009) "Te falta pagar: ${dinero(meFalta)}" else "Estas al corriente con tus pagos",
                            color = if (meFalta > 0.009) Rojo else Verde,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "El pago se hace en efectivo o transferencia al repartidor.",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (pedidos.isEmpty()) item { Text("No hay pedidos") }

            // ---------- Lista de pedidos ----------
            items(pedidos) { pedido ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "#${pedido.id} ${pedido.clienteNombre}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            // Etiqueta verde (ENTREGADO) o roja (PENDIENTE)
                            EtiquetaEstado(pedido.estado)
                        }
                        Text(pedido.detalle)
                        Text("${pedido.totalPiezas} panes · Total ${dinero(pedido.total)} · ${pedido.fecha}")

                        if (rol == "ADMIN" && pedido.trabajadorId == null) {
                            Text("Sin repartidor asignado", color = Rojo)
                        }

                        // Pagos: solo cuentan cuando el pedido ya se entrego
                        if (pedido.estado == "ENTREGADO") {
                            val falta = pedido.faltaCliente()
                            Text(
                                "Pago del cliente: ${dinero(pedido.pagoCliente)} de ${dinero(pedido.total)}" +
                                        if (falta > 0.009) " · Falta ${dinero(falta)}" else " · Pagado",
                                color = if (falta > 0.009) Rojo else Verde
                            )
                            if (rol != "CLIENTE") {
                                val debe = pedido.deudaRepartidor()
                                Text(
                                    "Entregado al admin: ${dinero(pedido.montoEntregado)}" +
                                            if (debe > 0.009) " · Debe ${dinero(debe)}" else "",
                                    color = if (debe > 0.009) Rojo else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // El repartidor registra el pago del cliente al entregar
                        if (rol == "TRABAJADOR" && pedido.estado == "ENTREGADO" && pedido.faltaCliente() > 0.009) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { registrarPago(pedido, pedido.total) }) { Text("Pago completo") }
                                OutlinedButton(onClick = { aCobrar = pedido }) { Text("Le falto pagar") }
                            }
                        }

                        Row {
                            // El cliente no edita; el admin y el repartidor si
                            if (rol != "CLIENTE") {
                                TextButton(onClick = { abrirEditar(pedido.id ?: 0L) }) { Text("Editar") }
                            }
                            // El cliente solo puede cancelar si el pedido sigue pendiente
                            if (rol != "CLIENTE" || pedido.estado == "PENDIENTE") {
                                // No borra directo: primero pide confirmacion
                                TextButton(onClick = { aEliminar = pedido }) {
                                    Text(
                                        if (rol == "CLIENTE") "Cancelar pedido" else "Eliminar",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Ventana para escribir cuanto pago el cliente cuando le falto
    val cobro = aCobrar
    if (cobro != null) {
        DialogoPago(
            total = cobro.total,
            alGuardar = { monto ->
                aCobrar = null
                registrarPago(cobro, monto)
            },
            alCancelar = { aCobrar = null }
        )
    }

    // Ventana de confirmacion antes de eliminar o cancelar
    val pedido = aEliminar
    if (pedido != null) {
        DialogoConfirmar(
            mensaje = if (rol == "CLIENTE") "Cancelar el pedido #${pedido.id}?" else "Eliminar el pedido #${pedido.id}?",
            alConfirmar = {
                aEliminar = null
                scope.launch {
                    try {
                        RetrofitClient.api.eliminarPedido(pedido.id ?: 0L)
                        mostrarMensaje(context, if (rol == "CLIENTE") "Pedido cancelado" else "Pedido eliminado")
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

// CREATE de pedidos. Lo usan el CLIENTE (para si mismo) y el REPARTIDOR (a nombre de uno de sus clientes).
// Cada pan tiene botones (-) y (+) y tambien un campo para escribir la cantidad.
@Composable
fun NuevoPedidoScreen(alTerminar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val yo = Sesion.usuario
    val esTrabajador = yo?.rol == "TRABAJADOR"

    var panes by remember { mutableStateOf(listOf<Pan>()) }
    var clientes by remember { mutableStateOf(listOf<Usuario>()) }
    // Cantidad elegida de cada pan: la llave es el id del pan y el valor la cantidad
    val cantidades = remember { mutableStateMapOf<Long, Int>() }
    // Cliente elegido en el selector (o escrito a mano por el repartidor)
    var clienteElegido by remember { mutableStateOf<Usuario?>(null) }
    var nombreManual by remember { mutableStateOf("") }
    // true mientras se envia: desactiva el boton para no mandar el pedido dos veces
    var enviando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            panes = RetrofitClient.api.obtenerPanes()
            // El repartidor solo puede pedir a nombre de SUS clientes
            if (esTrabajador) clientes = RetrofitClient.api.obtenerUsuarios("CLIENTE", yo?.id)
        } catch (e: Exception) {
            mostrarMensaje(context, mensajeDeError(e, "Error al cargar el catalogo"))
        }
    }

    // Totales que se recalculan solos cada vez que cambia una cantidad
    val totalPiezas = cantidades.values.sum()
    // Math.round(x * 100) / 100.0 deja el total con 2 decimales (evita 36.2999999)
    val totalDinero = Math.round(panes.sumOf { (cantidades[it.id ?: 0L] ?: 0) * it.precio } * 100) / 100.0

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Encabezado(if (esTrabajador) "Tomar pedido" else "Hacer pedido", alTerminar)

        // Todo va dentro de la lista para poder deslizar (tambien con el telefono acostado)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (panes.isEmpty()) item { Text("No hay panes en el catalogo") }
            items(panes) { pan ->
                val id = pan.id ?: 0L
                val cantidad = cantidades[id] ?: 0
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FotoBase64(pan.foto, 56)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(pan.nombre, style = MaterialTheme.typography.titleMedium)
                                Text("${dinero(pan.precio)} c/u", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        // Fila de cantidad: [-] [campo para escribir] [+]
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Boton restar: no baja de 0
                            OutlinedButton(onClick = { if (cantidad > 0) cantidades[id] = cantidad - 1 }) { Text("-") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = if (cantidad == 0) "" else cantidad.toString(),
                                // Solo numeros, maximo 3 cifras (hasta 999 piezas)
                                onValueChange = { texto ->
                                    cantidades[id] = texto.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
                                },
                                placeholder = { Text("0", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(84.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { if (cantidad < 999) cantidades[id] = cantidad + 1 }) { Text("+") }
                        }
                    }
                }
            }

        // Parte de abajo: cliente, total y boton (un solo elemento de la lista)
        item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (esTrabajador) {
            // El repartidor elige uno de sus clientes o escribe el nombre de uno sin cuenta
            Selector(
                textoVacio = "Elegir cliente",
                opciones = clientes.map { it.sucursal ?: it.nombreCompleto() },
                seleccion = clienteElegido?.let { it.sucursal ?: it.nombreCompleto() } ?: ""
            ) { posicion ->
                clienteElegido = clientes[posicion]
                nombreManual = ""
            }
            OutlinedTextField(
                value = nombreManual,
                onValueChange = { nombreManual = it; clienteElegido = null },
                label = { Text("O escribe el nombre del cliente") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Total: $totalPiezas panes | ${dinero(totalDinero)}", style = MaterialTheme.typography.titleMedium)
        Text("Sin pago en linea: se paga al repartidor en efectivo o transferencia.")
        Text("Maximo 2 pedidos por dia por cliente.", color = MaterialTheme.colorScheme.secondary)

        Button(enabled = !enviando, modifier = Modifier.fillMaxWidth(), onClick = {
            if (totalPiezas == 0) {
                mostrarMensaje(context, "Agrega al menos un pan")
                return@Button
            }
            // Definimos el cliente segun quien hace el pedido
            val clienteId = if (esTrabajador) clienteElegido?.id else yo?.id
            val clienteNombre = when {
                !esTrabajador -> yo?.sucursal ?: yo?.nombreCompleto() ?: ""
                clienteElegido != null -> clienteElegido?.sucursal ?: clienteElegido?.nombreCompleto() ?: ""
                else -> nombreManual.trim()
            }
            if (clienteNombre.isBlank()) {
                mostrarMensaje(context, "Elige o escribe el cliente")
                return@Button
            }
            // Texto con el detalle, por ejemplo "Bolillo x10, Concha x5"
            val detalle = panes
                .filter { (cantidades[it.id ?: 0L] ?: 0) > 0 }
                .joinToString(", ") { "${it.nombre} x${cantidades[it.id ?: 0L]}" }

            // El servidor pone la fecha y asigna el repartidor del cliente automaticamente
            val pedido = Pedido(
                clienteId = clienteId,
                clienteNombre = clienteNombre,
                trabajadorId = if (esTrabajador) yo?.id else null,
                detalle = detalle,
                totalPiezas = totalPiezas,
                total = totalDinero
            )
            enviando = true
            scope.launch {
                try {
                    RetrofitClient.api.crearPedido(pedido)
                    mostrarMensaje(context, "Pedido enviado")
                    alTerminar()
                } catch (e: Exception) {
                    // Por ejemplo: "Este cliente ya tiene 2 pedidos hoy..."
                    mostrarMensaje(context, mensajeDeError(e, "No se pudo enviar el pedido"))
                }
                enviando = false
            }
        }) { Text(if (enviando) "Enviando..." else "Confirmar pedido") }
        }
        }
        }
    }
}

// UPDATE de pedidos.
// - El REPARTIDOR cambia el estado (PENDIENTE / ENTREGADO).
// - El ADMIN ademas puede cambiar el repartidor y registra cuanto dinero le entrego.
//   El repartidor ya viene puesto: es el que atiende a ese cliente.
@Composable
fun EditarPedidoScreen(id: Long, alTerminar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val esAdmin = Sesion.usuario?.rol == "ADMIN"

    var pedido by remember { mutableStateOf<Pedido?>(null) }
    var trabajadores by remember { mutableStateOf(listOf<Usuario>()) }
    var estado by remember { mutableStateOf("PENDIENTE") }
    var trabajadorId by remember { mutableStateOf<Long?>(null) }
    var montoEntregado by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        try {
            val p = RetrofitClient.api.obtenerPedido(id)
            pedido = p
            estado = p.estado
            trabajadorId = p.trabajadorId
            montoEntregado = p.montoEntregado.toString()
            if (esAdmin) trabajadores = RetrofitClient.api.obtenerUsuarios("TRABAJADOR")
        } catch (e: Exception) {
            mostrarMensaje(context, mensajeDeError(e, "Error al cargar el pedido"))
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Encabezado("Editar pedido #$id", alTerminar)

        val p = pedido
        if (p == null) {
            Text("Cargando...")
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cliente: ${p.clienteNombre}", Modifier.weight(1f))
            EtiquetaEstado(estado)
        }
        Text(p.detalle)
        Text("${p.totalPiezas} panes · Total ${dinero(p.total)}")

        Text("Estado")
        Selector("Elegir estado", listOf("PENDIENTE", "ENTREGADO"), estado) { posicion ->
            estado = listOf("PENDIENTE", "ENTREGADO")[posicion]
        }

        if (esAdmin) {
            Text("Repartidor")
            val nombreActual = trabajadores.find { it.id == trabajadorId }?.nombreCompleto() ?: ""
            if (nombreActual.isBlank()) Text("Este pedido no tiene repartidor", color = Rojo)
            Selector("Elegir repartidor", trabajadores.map { it.nombreCompleto() }, nombreActual) { posicion ->
                trabajadorId = trabajadores[posicion].id
            }
            OutlinedTextField(
                montoEntregado, { montoEntregado = it },
                label = { Text("Dinero que el repartidor ya entrego") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            val falta = p.total - (montoEntregado.replace(',', '.').toDoubleOrNull() ?: 0.0)
            Text(
                "Faltante (deuda del repartidor): ${dinero(falta)}",
                color = if (falta > 0.009) Rojo else Verde
            )
        }

        Button(enabled = !guardando, modifier = Modifier.fillMaxWidth(), onClick = {
            // No se puede entregar un pedido que nadie lleva
            if (estado == "ENTREGADO" && trabajadorId == null) {
                mostrarMensaje(context, "Asigna un repartidor antes de marcarlo como entregado")
                return@Button
            }
            val monto = montoEntregado.trim().replace(',', '.').toDoubleOrNull()
            // El dinero entregado no puede ser negativo ni mayor que el total del pedido
            if (monto == null || monto < 0 || monto > p.total) {
                mostrarMensaje(context, "Escribe una cantidad entre 0 y ${dinero(p.total)}")
                return@Button
            }
            // copy() crea una copia del pedido cambiando solo estos campos
            val actualizado = p.copy(estado = estado, trabajadorId = trabajadorId, montoEntregado = monto)
            guardando = true
            scope.launch {
                try {
                    RetrofitClient.api.actualizarPedido(id, actualizado)
                    mostrarMensaje(context, "Pedido actualizado")
                    alTerminar()
                } catch (e: Exception) {
                    mostrarMensaje(context, mensajeDeError(e, "No se pudo actualizar"))
                }
                guardando = false
            }
        }) { Text(if (guardando) "Guardando..." else "Guardar") }
    }
}
