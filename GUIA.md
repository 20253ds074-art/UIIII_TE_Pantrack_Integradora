# PanTrack - Guia del proyecto

App de Android para la gestion de pedidos y reparto de una panaderia.
Todo corre en la misma computadora: **no necesita internet**.

- `app/` -> aplicacion Android (Kotlin + Jetpack Compose)
- `servidor-panaderia/` -> API REST en Spring Boot conectada a MySQL

```
Celular (app) --cable USB (adb reverse)--> Spring Boot (API REST) --JDBC--> MySQL
                                           [todo en mi computadora, localhost]
```

La app NUNCA se conecta directo a MySQL. Siempre le pide los datos a la API REST.

---

## 1. Configurar MySQL (una sola vez)

MySQL 8 ya esta instalado en la computadora (servicio `MySQL80`).
Abre `servidor-panaderia/src/main/resources/application.properties` y cambia
`TU_CONTRASENA_DE_MYSQL` por la contrasena del usuario `root` de MySQL.

No hay que crear tablas a mano: al arrancar, Spring crea la base `panaderia`,
sus tablas (`ddl-auto=update`) y el usuario administrador con unos panes de ejemplo
(`Configuracion.java`).

## 2. Correr el servidor

Abre la carpeta `servidor-panaderia` en IntelliJ y ejecuta `PanaderiaApplication`,
o desde una terminal dentro de esa carpeta:

```powershell
.\mvnw.cmd spring-boot:run
```

Para probar que funciona, abre en el navegador: http://localhost:8080/api/panes

## 3. Conectar el celular (cada vez que lo conectes)

1. Conecta el celular a la computadora con el cable USB (con depuracion USB activada).
2. Da doble clic a `conectar-celular.bat`, o ejecuta en la terminal:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
```

Ese comando hace que `localhost:8080` dentro del celular llegue a `localhost:8080`
de la computadora, por el cable. Por eso no se necesita internet, WiFi ni IP.
Si desconectas el cable o reinicias el celular, vuelve a ejecutarlo.

## 4. Correr la app

Abre la carpeta `integradora` en Android Studio y presiona **Run** con el celular conectado.
Usuario para entrar la primera vez: `admin@panaderia.com` / `admin123`.

---

## 5. Donde esta cada requisito (para la sesion de preguntas)

| Requisito | Donde esta |
|---|---|
| CRUD completo | Panes: `PanesScreen.kt`. Trabajadores y clientes: `UsuariosScreen.kt`. Pedidos: `PedidosScreen.kt` |
| Sensor (camara) | `CampoFoto` en `pantallas/Componentes.kt` (fotos de panes y trabajadores) |
| Permisos del sensor | `AndroidManifest.xml` (CAMERA y FileProvider, tomados de Memorias) |
| API REST | `servidor-panaderia/.../controlador/` (Spring Boot) |
| Base de datos | MySQL local; tablas creadas desde `modelo/` y conexion en `application.properties` |
| Seguridad | Contrasenas con BCrypt, sesion guardada y cierre por 1 hora sin uso (ver abajo) |
| Conexion app <-> API | `datos/ApiService.kt` y `datos/RetrofitClient.kt` (Retrofit) |
| La app no se cierra | Todas las llamadas al servidor estan dentro de `try/catch` y muestran un mensaje |
| Problema real | Repartidor toma pedidos a nombre de clientes sin app y el admin controla las deudas |

### Que hace cada rol

- **Administrador**: CRUD de panes (con foto), CRUD de trabajadores (con foto y NSS),
  ve todos los pedidos, asigna repartidor, registra el dinero que le entrego el trabajador,
  y ve un resumen de quien lleva menos panes y cuanto debe cada trabajador.
- **Trabajador (repartidor)**: CRUD de clientes, toma pedidos a nombre de un cliente
  (con botones + y -, elige cliente de la lista o escribe el nombre), ve sus pedidos y
  los marca como ENTREGADO.
- **Cliente**: se registra desde el login, hace pedidos desde el catalogo con + y -,
  ve su historial, cancela pedidos pendientes y edita su perfil. No hay pago en linea.

### Como funciona la deuda

Cuando un pedido esta ENTREGADO, el cliente ya le pago al repartidor.
El admin escribe cuanto dinero le entrego el repartidor (`montoEntregado`).
Deuda del pedido = `total - montoEntregado`. La suma de esas deudas es lo que debe cada trabajador.

### Preguntas que les pueden hacer

- **Que es una API REST?** Un servidor que recibe peticiones HTTP (GET leer, POST crear,
  PUT actualizar, DELETE eliminar) y responde con datos en JSON.
- **Que es Retrofit?** Libreria de Android que convierte las funciones de `ApiService`
  en peticiones HTTP y el JSON en objetos de Kotlin.
- **Para que sirve `suspend` y `scope.launch`?** Para hacer la peticion en segundo plano
  y que la pantalla no se congele mientras responde el servidor.
- **Como se guarda la foto?** La camara guarda la foto en un archivo temporal (FileProvider),
  la hacemos pequena, la convertimos a texto Base64 y se guarda en una columna de texto largo (LONGTEXT) de MySQL.
- **Para que es el FileProvider?** Le da a la app de camara permiso para escribir la foto
  en una carpeta de nuestra app de forma segura.
- **Que es JpaRepository?** Interfaz de Spring que ya trae los metodos del CRUD
  (`findAll`, `findById`, `save`, `deleteById`) sin escribir SQL.
- **Que hace `adb reverse`?** Redirige un puerto del celular a la computadora por el cable USB.
  Asi la app usa `localhost:8080` y llega al servidor de la computadora sin internet.
- **Que es BCrypt?** Un algoritmo que convierte la contrasena en un texto cifrado que no se
  puede regresar al original. Para el login se compara con `matches()`.

### Seguridad

**En el servidor**
- Las contrasenas se guardan cifradas con BCrypt (`Configuracion.java` y `UsuarioController.java`).
- La API nunca regresa la contrasena (`@JsonProperty(access = WRITE_ONLY)` en `Usuario.java`).
- Si el login falla responde 401 sin decir si el error fue el correo o la contrasena.
- No se pueden repetir correos (responde 409).
- Desde la app nadie puede crear un ADMIN ni cambiar su rol al editar.

**En la app**
- La sesion se guarda en el telefono con DataStore (`SesionRepositorio.kt`, igual que en Memorias).
  Si sales de la app o la cierras, al volver entras directo al menu, sin login.
- La sesion solo se cierra con el boton **Cerrar sesion** o despues de **1 hora sin tocar la app**.
  - `MainActivity.dispatchTouchEvent` guarda la hora de cada toque.
  - `MainActivity.onStop` guarda la sesion cuando la app deja de verse.
  - `Navegacion.kt` revisa cada 10 segundos si ya paso 1 hora; si si, cierra la sesion.
- Al cerrar sesion se borra el historial de pantallas, asi el boton "atras" no regresa
  a pantallas privadas.
- La contrasena se escribe oculta y el correo usa teclado de correo.

### Simplificaciones (por si preguntan)

- La API no usa tokens: la app recuerda al usuario, pero el servidor no revisa quien hace
  cada peticion. En una app real se usaria un token (por ejemplo JWT) en cada peticion.
