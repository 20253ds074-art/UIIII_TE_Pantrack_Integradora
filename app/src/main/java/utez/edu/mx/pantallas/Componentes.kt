package utez.edu.mx.pantallas

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.launch
import utez.edu.mx.datos.RetrofitClient
import utez.edu.mx.ui.theme.Rojo
import utez.edu.mx.ui.theme.RojoClaro
import utez.edu.mx.ui.theme.Verde
import utez.edu.mx.ui.theme.VerdeClaro
import java.io.ByteArrayOutputStream
import java.io.File

// Muestra un mensaje corto abajo de la pantalla
fun mostrarMensaje(context: Context, texto: String) {
    Toast.makeText(context, texto, Toast.LENGTH_LONG).show()
}

// Saca el mensaje que mando el servidor cuando algo sale mal.
// El servidor responde los errores asi: {"mensaje": "Ya existe un pan con ese nombre"}
// Si no hay respuesta (servidor apagado o cable desconectado) avisa que no hay conexion.
fun mensajeDeError(e: Exception, porDefecto: String): String {
    if (e is retrofit2.HttpException) {
        return try {
            val texto = e.response()?.errorBody()?.string()
            val datos = Gson().fromJson(texto, Map::class.java)
            datos?.get("mensaje")?.toString() ?: porDefecto
        } catch (x: Exception) {
            porDefecto
        }
    }
    return "No hay conexion con el servidor. Revisa el cable o la IP en el boton Servidor"
}

// Revisa que la contrasena sea segura. Regresa el problema, o null si esta bien.
// Regla: minimo 10 caracteres, una mayuscula, una minuscula, un numero y un caracter especial
fun problemaContrasena(c: String): String? = when {
    c.length < 10 -> "La contrasena debe tener minimo 10 caracteres"
    c.none { it.isUpperCase() } -> "La contrasena necesita al menos una mayuscula"
    c.none { it.isLowerCase() } -> "La contrasena necesita al menos una minuscula"
    c.none { it.isDigit() } -> "La contrasena necesita al menos un numero"
    c.all { it.isLetterOrDigit() } -> "La contrasena necesita un caracter especial (! @ # $ ...)"
    else -> null
}

// Titulo de la pantalla con un boton para regresar
@Composable
fun Encabezado(titulo: String, alVolver: (() -> Unit)? = null) {
    // Flecha para regresar y el titulo en la misma linea; abajo una linea delgada
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (alVolver != null) {
            IconButton(onClick = alVolver) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar", // lo lee el lector de pantalla
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(titulo, style = MaterialTheme.typography.headlineSmall)
    }
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
}

// Ventana para elegir como conectarse al servidor (cable USB o Wi-Fi con la IP de la computadora).
// Antes de guardar, prueba la conexion; si falla, deja la direccion que ya funcionaba.
@Composable
fun DialogoServidor(alCerrar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actual = RetrofitClient.baseUrl
    var ip by remember {
        mutableStateOf(
            if (actual == RetrofitClient.SERVIDOR_CABLE) ""
            else actual.removePrefix("http://").removeSuffix("/").removeSuffix(":8080")
        )
    }
    var error by remember { mutableStateOf("") }
    var probando by remember { mutableStateOf(false) }

    // Prueba una direccion pidiendo la lista de panes. Si contesta, la deja guardada.
    fun probar(url: String) {
        probando = true
        error = ""
        scope.launch {
            val anterior = RetrofitClient.baseUrl
            RetrofitClient.cambiarServidor(context, url)
            try {
                RetrofitClient.api.obtenerPanes()
                mostrarMensaje(context, "Conectado: ${RetrofitClient.descripcion()}")
                alCerrar()
            } catch (e: Exception) {
                // No contesto: regresamos a la direccion anterior
                RetrofitClient.cambiarServidor(context, anterior)
                error = "No contesto el servidor. Revisa que este corriendo y que esten en la misma red."
            }
            probando = false
        }
    }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ahora: ${RetrofitClient.descripcion()}", color = MaterialTheme.colorScheme.primary)
                Text("Por Wi-Fi (hotspot): escribe la IP de la computadora. La muestra el archivo mi-ip.bat")
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim() },
                    label = { Text("IP, por ejemplo 192.168.43.20") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedButton(
                    enabled = !probando,
                    onClick = { probar(RetrofitClient.SERVIDOR_CABLE) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Usar cable USB") }
                if (probando) Text("Probando conexion...")
                if (error.isNotEmpty()) Text(error, color = Rojo)
            }
        },
        confirmButton = {
            TextButton(enabled = !probando, onClick = {
                // Una IP son 4 numeros separados por punto, por ejemplo 192.168.43.20
                if (!Regex("""\d{1,3}(\.\d{1,3}){3}""").matches(ip)) {
                    error = "Escribe una IP valida, por ejemplo 192.168.43.20"
                } else {
                    probar(RetrofitClient.direccionDesdeIp(ip))
                }
            }) { Text("Probar y guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } }
    )
}

// Ventana que pregunta antes de eliminar algo, para no borrar por error
@Composable
fun DialogoConfirmar(mensaje: String, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar, // tocar fuera de la ventana = cancelar
        title = { Text("Confirmar") },
        text = { Text(mensaje) },
        confirmButton = { TextButton(onClick = alConfirmar) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

// Menu desplegable para elegir una opcion de una lista
@Composable
fun Selector(textoVacio: String, opciones: List<String>, seleccion: String, alElegir: (Int) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (seleccion.isBlank()) textoVacio else seleccion)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEachIndexed { posicion, texto ->
                DropdownMenuItem(
                    text = { Text(texto) },
                    onClick = {
                        alElegir(posicion)
                        abierto = false
                    }
                )
            }
        }
    }
}

// Muestra una foto guardada como texto Base64
// "modifier" permite darle otro tamano (por ejemplo, todo el ancho de una tarjeta)
@Composable
fun FotoBase64(foto: String?, tamano: Int, modifier: Modifier = Modifier.size(tamano.dp)) {
    // remember(foto): solo vuelve a convertir la imagen si la foto cambia
    val imagen = remember(foto) { base64ABitmap(foto) }
    if (imagen != null) {
        Image(
            bitmap = imagen.asImageBitmap(),
            contentDescription = "Foto",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Recuadro crema cuando no hay foto
        Box(
            modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text("Sin foto", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

// Etiqueta de color para el estado del pedido: ENTREGADO en verde, PENDIENTE en rojo
@Composable
fun EtiquetaEstado(estado: String) {
    val entregado = estado == "ENTREGADO"
    Text(
        text = estado,
        color = if (entregado) Verde else Rojo,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(if (entregado) VerdeClaro else RojoClaro, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

// Ventana para registrar cuanto pago el cliente cuando le falto dinero
@Composable
fun DialogoPago(total: Double, alGuardar: (Double) -> Unit, alCancelar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Le falto pagar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Total del pedido: $${"%.2f".format(total)}")
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Cuanto pago el cliente") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (error.isNotEmpty()) Text(error, color = Rojo)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val monto = texto.trim().replace(',', '.').toDoubleOrNull()
                // Si pago todo seria "Pago completo"; aqui debe ser menos que el total
                if (monto == null || monto < 0 || monto >= total) {
                    error = "Escribe una cantidad entre 0 y $${"%.2f".format(total)} (sin llegar al total)"
                } else {
                    alGuardar(monto)
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

// ----------------- SENSOR: CAMARA (y GALERIA) -----------------
// Muestra la foto actual y dos botones: tomar una foto con la camara o elegir una de la galeria.
// La foto elegida se regresa como texto Base64 en "alTomarFoto".
@Composable
fun CampoFoto(foto: String?, alTomarFoto: (String) -> Unit) {
    val context = LocalContext.current

    // Archivo temporal donde la camara guarda la foto (carpeta cache, declarada en file_paths.xml)
    val archivo = remember { File(context.cacheDir, "foto_temporal.jpg") }
    // Direccion (Uri) segura del archivo usando el FileProvider del manifiesto (igual que en Memorias)
    val uri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }

    // ---------- CAMARA ----------
    // 1) Lanzador que abre la camara. "exito" es true si el usuario si tomo la foto.
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) {
            val texto = archivoABase64(archivo)
            if (texto != null) alTomarFoto(texto) else mostrarMensaje(context, "No se pudo leer la foto")
        }
    }

    // 2) Lanzador que pide el permiso de camara. Si lo da, abre la camara.
    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) camara.launch(uri) else mostrarMensaje(context, "Se necesita permiso de camara")
    }

    // ---------- GALERIA ----------
    // 1) Lanzador que abre la galeria para elegir una imagen ("image/*" = cualquier imagen).
    //    Regresa la direccion (Uri) de la imagen elegida, o null si el usuario cancelo.
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { elegida ->
        if (elegida != null) {
            // Copiamos la imagen a un archivo temporal y usamos la MISMA funcion que la camara
            // (la hace pequena, la gira si hace falta y la convierte a Base64)
            val temporal = File(context.cacheDir, "galeria_temporal.jpg")
            val texto = if (copiarAArchivo(context, elegida, temporal)) archivoABase64(temporal) else null
            if (texto != null) alTomarFoto(texto) else mostrarMensaje(context, "No se pudo leer la imagen")
        }
    }

    // 2) Lanzador que pide el permiso READ_MEDIA_IMAGES (declarado en el manifiesto).
    //    Si lo da, abre la galeria.
    val permisoGaleria = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) galeria.launch("image/*") else mostrarMensaje(context, "Se necesita permiso para ver la galeria")
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        FotoBase64(foto, 100)
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { permisoCamara.launch(Manifest.permission.CAMERA) }) {
                Text(if (foto == null) "Tomar foto" else "Cambiar foto")
            }
            OutlinedButton(onClick = { permisoGaleria.launch(Manifest.permission.READ_MEDIA_IMAGES) }) {
                Text("Elegir de galeria")
            }
        }
    }
}

// Copia la imagen elegida en la galeria (Uri) a un archivo de la app.
// contentResolver es el que nos deja leer archivos de otras apps (como la galeria).
fun copiarAArchivo(context: Context, origen: Uri, destino: File): Boolean {
    return try {
        context.contentResolver.openInputStream(origen)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        } ?: return false
        true
    } catch (e: Exception) {
        false
    }
}

// Convierte el archivo de la camara (o de la galeria) en texto Base64 (pequeno, para guardarlo en MySQL)
fun archivoABase64(archivo: File): String? {
    return try {
        val original = BitmapFactory.decodeFile(archivo.absolutePath) ?: return null

        // Hacemos la foto pequena (400 px de ancho) para que no pese mucho
        val alto = original.height * 400 / original.width
        var foto = Bitmap.createScaledBitmap(original, 400, alto, true)

        // Algunas camaras guardan la foto "acostada"; aqui la giramos si hace falta
        val orientacion = ExifInterface(archivo.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val grados = when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (grados != 0f) {
            val matriz = Matrix().apply { postRotate(grados) }
            foto = Bitmap.createBitmap(foto, 0, 0, foto.width, foto.height, matriz, true)
        }

        // Comprimimos a JPG y lo pasamos a texto
        val bytes = ByteArrayOutputStream()
        foto.compress(Bitmap.CompressFormat.JPEG, 70, bytes)
        Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

// Convierte el texto Base64 de vuelta en imagen para mostrarla
fun base64ABitmap(texto: String?): Bitmap? {
    if (texto.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(texto, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
