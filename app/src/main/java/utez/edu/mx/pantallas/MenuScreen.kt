package utez.edu.mx.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import utez.edu.mx.datos.RetrofitClient
import utez.edu.mx.datos.Sesion

// Menu principal. Muestra botones diferentes segun el rol del usuario.
// "irA" recibe el nombre de la ruta a la que queremos navegar.
@Composable
fun MenuScreen(irA: (String) -> Unit, alCerrarSesion: () -> Unit) {
    val usuario = Sesion.usuario
    val rol = usuario?.rol ?: ""
    val miId = usuario?.id ?: 0L
    var mostrarServidor by remember { mutableStateOf(false) }
    var textoServidor by remember { mutableStateOf(RetrofitClient.descripcion()) }

    Column(
        // verticalScroll: se puede deslizar si no cabe (telefono acostado)
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Texto del rol mas amigable para mostrar
        val rolTexto = when (rol) {
            "ADMIN" -> "Administrador"
            "TRABAJADOR" -> "Repartidor"
            else -> "Cliente"
        }
        Spacer(Modifier.height(16.dp))
        Text(rolTexto.uppercase(), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
        Text("Hola, ${usuario?.nombre ?: ""}", style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)

        when (rol) {
            "ADMIN" -> {
                BotonMenu("Panes (catalogo)") { irA("panes") }
                BotonMenu("Trabajadores") { irA("usuarios/TRABAJADOR") }
                BotonMenu("Clientes") { irA("usuarios/CLIENTE") }
                BotonMenu("Solicitudes de clientes") { irA("solicitudes") }
                BotonMenu("Pedidos, pagos y deudas") { irA("pedidos") }
            }
            "TRABAJADOR" -> {
                BotonMenu("Tomar pedido para un cliente") { irA("nuevo_pedido") }
                BotonMenu("Mis pedidos, cobros y deudas") { irA("pedidos") }
                BotonMenu("Mis clientes") { irA("usuarios/CLIENTE") }
                BotonMenu("Mi perfil") { irA("usuario_form/TRABAJADOR/$miId") }
            }
            "CLIENTE" -> {
                BotonMenu("Catalogo / Hacer pedido") { irA("nuevo_pedido") }
                BotonMenu("Mis pedidos y pagos") { irA("pedidos") }
                BotonMenu("Mi perfil") { irA("usuario_form/CLIENTE/$miId") }
                Text("El pago se hace en efectivo o transferencia al repartidor al recibir el pan.")
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = alCerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesion")
        }
        // Para cambiar entre cable y Wi-Fi sin cerrar sesion
        TextButton(onClick = { mostrarServidor = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Servidor: $textoServidor", color = MaterialTheme.colorScheme.secondary)
        }
    }

    if (mostrarServidor) {
        DialogoServidor(alCerrar = {
            mostrarServidor = false
            textoServidor = RetrofitClient.descripcion()
        })
    }
}

// Boton grande que ocupa todo el ancho
@Composable
fun BotonMenu(texto: String, alPresionar: () -> Unit) {
    Button(onClick = alPresionar, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(texto)
    }
}
