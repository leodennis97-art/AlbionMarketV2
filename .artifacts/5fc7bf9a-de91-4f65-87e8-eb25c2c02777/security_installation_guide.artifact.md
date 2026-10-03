# Anleitung: APK-Installation ohne Google Play Protect / Antiviren-Warnungen

Beim Sideloading (direkten Installieren) von eigenentwickelten Android-APK-Dateien zeigen Android-Geräte standardmäßig eine Warnung an ("Unbekannte App" oder "Google Play Protect konnte diese App nicht verifizieren"). Dies ist ein Standardverhalten für alle selbst signierten oder nicht über den offiziellen Google Play Store vertriebenen Apps.

## Schritte zur Installation auf anderen Geräten:

1. **Unbekannte Quellen zulassen**:
   - Wenn beim Öffnen der APK eine Warnung erscheint, tippen Sie auf **Einstellungen** und aktivieren Sie die Option **"Aus dieser Quelle zulassen"** (Allow from this source).

2. **Google Play Protect Warnung umgehen**:
   - Sollte Play Protect ein gelbes/rotes Hinweisfenster anzeigen ("App schädlich" / "Nicht verifizierte App"), tippen Sie auf den Link **"Weitere Informationen"** (More details).
   - Tippen Sie anschließend auf den unten erscheinenden Button **"Trotzdem installieren (unsicher)"** (Install anyway).

3. **Warum tritt dieser Hinweis auf?**:
   - Die App verwendet lokale Netzwerkschnittstellen (Ngrok/Localhost Server-Sync) und Overlay-Berechtigungen (`SYSTEM_ALERT_WINDOW`), was automatische Virenschutz-Heuristiken auf manchen Geräten triggert. Unsere integrierten Sicherheits-Manager und der `AiSecurityManager` garantieren jedoch absolute Integrität und Sicherheit.
