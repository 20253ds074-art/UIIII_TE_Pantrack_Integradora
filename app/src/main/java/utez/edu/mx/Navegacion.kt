package utez.edu.mx

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utez.edu.mx.datos.Sesion
import utez.edu.mx.datos.SesionRepositorio
import utez.edu.mx.pantallas.*

// Aqui se definen todas las pantallas (rutas) de la app.
// Las partes entre llaves {} son datos que se mandan en la ruta, por ejemplo "pan_form/5".
@Composable
fun Navegacion(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repositorio = remember { SesionRepositorio(context) }

    // ---------- 1) Al abrir la app revisamos si hay una sesion guardada (como en Memorias) ----------
    // Si hay sesion y no ha pasado 1 hora, entramos directo al menu; si no, al login.
    var pantallaInicial by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        Sesion.usuario = repositorio.leer()
        Sesion.ultimaActividad = System.currentTimeMillis()
        pantallaInicial = if (Sesion.usuario != null) "menu" else "login"
    }
    // Mientras se lee la sesion no mostramos nada (tarda milisegundos)
    val inicio = pantallaInicial ?: return

    val nav = rememberNavController()

    // Cierra la sesion: borra los datos guardados y regresa al login
    // popUpTo(0) borra todo el historial, asi "atras" ya no regresa a pantallas privadas
    fun cerrarSesion(mensaje: String) {
        Sesion.usuario = null
        scope.launch { repositorio.cerrar() }
        mostrarMensaje(context, mensaje)
        nav.navigate("login") { popUpTo(0) { inclusive = true } }
    }

    // ---------- 2) Revisamos cada 10 segundos si paso 1 hora sin usar la app ----------
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            if (Sesion.usuario != null && Sesion.expirada()) {
                cerrarSesion("Tu sesion se cerro por 1 hora sin uso")
            }
        }
    }

    NavHost(navController = nav, startDestination = inicio, modifier = modifier) {

        composable("login") {
            LoginScreen(
                alEntrar = {
                    // Guardamos la sesion en el telefono para no pedir login otra vez
                    Sesion.ultimaActividad = System.currentTimeMillis()
                    scope.launch { repositorio.guardar(Sesion.usuario, Sesion.ultimaActividad) }
                    // Vamos al menu y quitamos el login del historial (para que "atras" no regrese)
                    nav.navigate("menu") { popUpTo("login") { inclusive = true } }
                },
                alRegistrarse = { nav.navigate("usuario_form/CLIENTE/0") }
            )
        }

        composable("menu") {
            MenuScreen(
                irA = { ruta -> nav.navigate(ruta) },
                // Solo con este boton (o por inactividad) se cierra la sesion
                alCerrarSesion = { cerrarSesion("Sesion cerrada") }
            )
        }

        // ----- Panes -----
        composable("panes") {
            PanesScreen(
                alVolver = { nav.popBackStack() },
                abrirFormulario = { id -> nav.navigate("pan_form/$id") }
            )
        }
        composable("pan_form/{id}") { entrada ->
            // Leemos el id que viene en la ruta
            val id = entrada.arguments?.getString("id")?.toLongOrNull() ?: 0L
            PanFormScreen(id = id, alTerminar = { nav.popBackStack() })
        }

        // ----- Trabajadores y clientes -----
        composable("usuarios/{rol}") { entrada ->
            val rol = entrada.arguments?.getString("rol") ?: "CLIENTE"
            UsuariosScreen(
                rol = rol,
                alVolver = { nav.popBackStack() },
                abrirFormulario = { id -> nav.navigate("usuario_form/$rol/$id") }
            )
        }
        composable("usuario_form/{rol}/{id}") { entrada ->
            val rol = entrada.arguments?.getString("rol") ?: "CLIENTE"
            val id = entrada.arguments?.getString("id")?.toLongOrNull() ?: 0L
            UsuarioFormScreen(rol = rol, id = id, alTerminar = { nav.popBackStack() })
        }

        // Solicitudes de clientes nuevos (solo admin)
        composable("solicitudes") {
            SolicitudesScreen(alVolver = { nav.popBackStack() })
        }

        // ----- Pedidos -----
        composable("pedidos") {
            PedidosScreen(
                alVolver = { nav.popBackStack() },
                abrirEditar = { id -> nav.navigate("editar_pedido/$id") }
            )
        }
        composable("nuevo_pedido") {
            NuevoPedidoScreen(alTerminar = { nav.popBackStack() })
        }
        composable("editar_pedido/{id}") { entrada ->
            val id = entrada.arguments?.getString("id")?.toLongOrNull() ?: 0L
            EditarPedidoScreen(id = id, alTerminar = { nav.popBackStack() })
        }
    }
}
