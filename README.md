# PanTrack

Aplicacion movil para la gestion de pedidos y reparto de una panaderia.

**Equipo:** 
20253DS068
20253DS074
20253DS081
Desarrollo de Aplicaciones Moviles, UTEZ.

## Que hay en este repositorio

| Carpeta | Contenido |
| --- | --- |
| `app/` | App Android (Kotlin + Jetpack Compose) |
| `servidor-panaderia/` | API REST en Spring Boot conectada a MySQL |
| `GUIA.md` | Pasos para correr el proyecto y explicacion de cada requisito |
| `conectar-celular.bat` | Conecta el celular al servidor por cable USB (`adb reverse`) |

## Requisitos que cumple

- **CRUD completo:** panes, trabajadores, clientes y pedidos.
- **Sensor:** camara para la foto de panes y trabajadores.
- **API REST:** Spring Boot + MySQL, con contrasenas cifradas (BCrypt).
- **Problema real:** el repartidor toma pedidos a nombre de clientes que no usan la app,
  y el administrador controla lo que cada repartidor lleva y debe.

## Como correrlo

1. Crear/usar MySQL local y poner la contrasena de `root` en
   `servidor-panaderia/src/main/resources/application.properties`.
2. Ejecutar `PanaderiaApplication` (servidor en `localhost:8080`).
3. Conectar el celular por USB y ejecutar `conectar-celular.bat`.
4. Correr la app desde Android Studio. Usuario inicial: `admin@panaderia.com` / `admin123`.

Detalles completos en [GUIA.md](GUIA.md).
"# UIIII_TE_Pantrack_Integradora" 
