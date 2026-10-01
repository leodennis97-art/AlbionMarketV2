@echo off
title Albion Server Monitor & Autostart
:: Wechselt automatisch in das Projektverzeichnis
cd /d "%~dp0"

echo =========================================================
echo 🛡️  AUTOSTART & MONITOR - UEBERWACHT PORT 4000 🛡️
echo =========================================================
echo Dieses Fenster ueberwacht den Server im Hintergrund.
echo Es prueft alle 5 Sekunden, ob Port 4000 aktiv ist.
echo Ist der Server down, wird er sofort gestartet!
echo.

:loop
:: Prüft, ob der Port 4000 lauscht (LISTENING)
netstat -ano | find "LISTENING" | find ":4000" >nul
if %errorlevel% neq 0 (
    echo [%time%] 🔴 Server nicht gefunden (Port 4000 offline)! Starte Server neu...

    :: Startet die start_server.cmd minimiert in einem neuen Fenster
    start "AlbionDataPro Server" /min cmd /c "start_server.cmd"

    :: Gibt dem Server 3 Sekunden Zeit zum Hochfahren, bevor weiter geprüft wird
    timeout /t 3 /nobreak >nul
) else (
    :: (Optional: auskommentieren, wenn du keinen Spam in der Konsole willst)
    :: echo [%time%] 🟢 Server laeuft stabil.
)

:: 5 Sekunden Pause bis zur nächsten Prüfung
timeout /t 5 /nobreak >nul
goto loop
