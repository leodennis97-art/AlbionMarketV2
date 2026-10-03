# Walkthrough - Autonomer APK-Upload nach Render

Die App wurde um eine vollautomatische Autonomie-Funktion erweitert: Bei jedem Build-Vorgang wird die frisch kompilierte APK nun vollautomatisch auf den Render-Server hochgeladen.

## Changes Made

### Gradle Build Automation
- **[build.gradle (app)](file:///C:/Users/Dennis/Desktop/AlbionDataHack/AlbionMarketV2-Source/app/build.gradle)**:
  - Hinzufügen des Tasks `uploadApkToRender`, der die frisch gebaute Debug-APK (`app-debug.apk`) per HTTP-POST direkt an den Render-Server (`https://albionmarketv2.onrender.com/api/admin/upload-apk`) überträgt.
  - Verwendung des Admin-Tokens zur Authentifizierung beim Upload.

## Validation Results

### Automated Tests
- Gradle Build & Upload Task (`app:assembleDebug app:uploadApkToRender`) wurde erfolgreich ausgeführt.
