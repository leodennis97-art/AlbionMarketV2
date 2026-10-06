@echo off
echo ========================================================
echo  AlbionDataPro - Vollautomatischer Build, Sync & Render Upload
echo ========================================================

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/3] Starte Gradle Build, APK Erstellung & Render Web-Aktualisierung...
call gradlew app:assembleDebug uploadApkToRender

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Gradle Build oder Render Upload fehlgeschlagen!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Starte Ngrok-Dienst & autonomen Sync (start_ngrok.js)...
node start_ngrok.js

echo [3/3] Fertig! Alle Dienste und die Render-Webseite sind aktuell.
pause
