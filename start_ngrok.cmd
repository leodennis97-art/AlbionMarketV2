@echo off
title AlbionDataPro Ngrok & Local Server Manager
cd /d "%~dp0"

echo =========================================================
echo 🚀 STARTE LOCALHOST SERVER & NGROK TUNNEL 🚀
echo =========================================================
echo.

node start_ngrok.js
pause
