# GitHub Cloud-Transfer Workflow (Temporärer Upload & Löschung)

Dieser Leitfaden beschreibt, wie Daten (wie z. B. User-Daten, Backups oder Konfigurationen) temporär über ein privates GitHub-Repository in die Cloud transferiert und anschließend sofort wieder von GitHub gelöscht werden können.

## Workflow-Schritte:

1. **Lokales Vorbereiten der Daten**:
   - Packe die zu transferierenden Daten (z. B. `users.json`, `devices.json`, `licenses.json`) in ein temporäres Archiv oder Verzeichnis.

2. **Automatisierter Git / GitHub Transfer**:
   - Initialisiere ein temporäres Git-Repository oder nutze ein dediziertes privates Repo:
     ```bash
     git init
     git remote add origin https://github.com/DEIN-BENUTZERNAME/DEIN-PRIVATES-REPO.git
     git add .
     git commit -m "temp-cloud-sync-transfer"
     git push -u origin main --force
     ```

3. **Cloud-Synchronisation (Server zieht Daten)**:
   - Der Zielserver (Render/Cloud-Instanz) zieht die Daten aus dem Repository ab oder verarbeitet sie.

4. **Sofortiges Löschen von GitHub**:
   - Sobald der Transfer abgeschlossen ist, lösche das Repository oder leere den Branch vollständig:
     ```bash
     git checkout --orphan temp-clean-branch
     git rm -rf .
     git commit -m "purge-cloud-data"
     git push origin temp-clean-branch --force
     ```
   - Alternativ: Lösche das private Repository direkt in den GitHub-Repository-Einstellungen unter *Settings ➔ General ➔ Delete this repository*.
