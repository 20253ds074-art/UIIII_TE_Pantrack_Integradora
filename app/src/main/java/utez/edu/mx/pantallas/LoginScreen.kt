package utez.edu.mx.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import utez.edu.mx.datos.LoginRequest
import utez.edu.mx.datos.RetrofitClient
import utez.edu.mx.datos.Sesion

// Pantalla de inicio de sesion
@Composable
fun LoginScreen(alEntrar: () -> Unit, alRegistrarse: () -> Unit) {
    val context = LocalContext.current
    // scope: permite ejecutar la peticion al servidor en segundo plano
    val scope = rememberCoroutineScope()

    // Variables que guardan lo que escribe el usuario
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    // Ventana "Servidor" abierta o cerrada, y el texto que dice como estamos conectados
    var mostrarServidor by remember { mutableStateOf(false) }
    var textoServidor by remember { mutableStateOf(RetrofitClient.descripcion()) }

    // Box centra el contenido en la pantalla
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        // verticalScroll: se puede deslizar si no cabe (telefono acostado o teclado abierto)
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nombre de la app en color cafe y una linea delgada como adorno
        Text("PanTrack", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(Modifier.width(48.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Pedidos y reparto de pan", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            // Teclado de correo: no pone mayusculas automaticas ni autocorrige
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contrasena") },
            visualTransformation = PasswordVisualTransformation(), // oculta la contrasena
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (correo.isBlank() || contrasena.isBlank()) {
                    mostrarMensaje(context, "Escribe correo y contrasena")
                    return@Button
                }
                cargando = true
                scope.launch {
                    // try/catch: si falla el internet o el servidor, la app NO se cierra
                    try {
                        // trim() quita espacios al inicio y al final; lowercase() pasa el correo a minusculas
                        val usuario = RetrofitClient.api.login(LoginRequest(correo.trim().lowercase(), contrasena.trim()))
                        Sesion.usuario = usuario // guardamos quien entro
                        alEntrar()
                    } catch (e: retrofit2.HttpException) {
                        mostrarMensaje(context, "Correo o contrasena incorrectos")
                    } catch (e: Exception) {
                        mostrarMensaje(context, "No hay conexion con el servidor. Revisa el cable o la IP en el boton Servidor")
                    }
                    cargando = false
                }
            }
        ) {
            Text(if (cargando) "Entrando..." else "Iniciar sesion")
        }

        TextButton(onClick = alRegistrarse) {
            Text("Soy cliente nuevo, quiero registrarme")
        }

        // Boton para elegir como conectarse al servidor (cable USB o Wi-Fi)
        TextButton(onClick = { mostrarServidor = true }) {
            Text("Servidor: $textoServidor", color = MaterialTheme.colorScheme.secondary)
        }
    }
    }

    if (mostrarServidor) {
        DialogoServidor(alCerrar = {
            mostrarServidor = false
            textoServidor = RetrofitClient.descripcion() // actualiza el texto del boton
        })
    }
}
