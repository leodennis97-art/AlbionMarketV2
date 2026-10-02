@echo off
echo [AlbionDataPro] 🟢 Installiere PM2 fuer automatischen Hintergrundbetrieb...
call npm.cmd install -g pm2
echo [AlbionDataPro] 🚀 Starte Server als Hintergrunddienst mit Auto-Watch...
call pm2 start server.js --name "albion-server" --watch
echo [AlbionDataPro] ⚙️ Konfiguriere System-Autostart...
call pm2 startup
call pm2 save
echo [AlbionDataPro] ✅ ALLES VOLLAUTOMATISCH EINGERICHTET! Der Server läuft nun 24/7 im Hintergrund und startet bei jedem PC-Start automatisch neu.
pause
