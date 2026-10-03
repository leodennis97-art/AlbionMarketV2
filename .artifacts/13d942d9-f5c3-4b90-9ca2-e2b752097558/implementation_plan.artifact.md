# Implementation Plan - Autonomer APK-Upload nach Render bei jedem Build

Ziel ist es, den Build-Prozess so zu erweitern, dass nach jedem erfolgreichen Kompilieren der App die frisch generierte APK automatisch auf den Render-Server hochgeladen wird, damit die Webseite (`https://albionmarketv2.onrender.com/download/AlbionDataPro.apk`) stets vollautomatisch die allerneueste Version bereitstellt.

## Proposed Changes

### Gradle Build Configuration

#### [MODIFY] [build.gradle (app)](file:///C:/Users/Dennis/Desktop/AlbionDataHack/AlbionMarketV2-Source/app/build.gradle)
- Hinzufügen eines Gradle-Tasks (`uploadApkToRender`), der nach dem Assemble-Vorgang ausgeführt wird oder manuell gestartet werden kann.
- Der Task liest die frisch gebaute `app-debug.apk` (bzw. Release-APK) und sendet sie per HTTP PUT/POST an den Render-Server (`https://albionmarketv2.onrender.com/api/admin/upload-apk`), sofern ein Admin-Token hinterlegt ist.

## Verification Plan

### Automated Tests
- Gradle Build ausführen (`app:assembleDebug`) und prüfen, ob der Upload-Task integriert ist.
