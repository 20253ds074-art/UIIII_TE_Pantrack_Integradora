@echo off
REM Conecta el celular (por cable USB) y/o el emulador con el servidor de esta computadora.
REM Hace que localhost:8080 del celular llegue a localhost:8080 de la computadora.
REM
REM Deja esta ventana ABIERTA durante la presentacion: cada 5 segundos vuelve a
REM activar la conexion en TODOS los dispositivos conectados (celular y emulador),
REM asi si el cable se mueve o se desconecta, se arregla sola.
REM Para terminar, cierra la ventana.

set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe

echo Conexion por cable activa. Deja esta ventana abierta.
echo (Si desconectas el cable, se vuelve a conectar sola al conectarlo otra vez)
echo.

:repetir
set CONECTADOS=0
REM "adb devices" lista los dispositivos; a cada uno que diga "device" le activamos el reverse
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" (
        "%ADB%" -s %%a reverse tcp:8080 tcp:8080 >nul 2>&1
        set CONECTADOS=1
    )
)
if "%CONECTADOS%"=="1" (
    echo [%time%] Dispositivo conectado al servidor
) else (
    echo [%time%] Esperando el celular... conecta el cable
)
timeout /t 5 /nobreak >nul
goto repetir
