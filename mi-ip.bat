@echo off
REM Muestra la IP de esta computadora para escribirla en la app (boton "Servidor").
REM Usalo cuando el celular y la computadora esten en la misma red Wi-Fi (por ejemplo el hotspot del celular).

echo.
echo ===============================================
echo   IP de esta computadora (escribela en la app):
echo ===============================================
powershell -NoProfile -Command "Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254*' } | ForEach-Object { '   ' + $_.IPAddress + '   (' + $_.InterfaceAlias + ')' }"
echo.
echo Usa la que diga Wi-Fi. En la app: boton Servidor, escribe la IP y "Probar y guardar".
echo.
pause
