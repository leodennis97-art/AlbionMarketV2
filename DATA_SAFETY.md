# Google Play Datensicherheit (Data Safety) – Fragebogen-Leitfaden

Hier sind die korrekten Antworten für den **Datensicherheitsschnittstellen-Fragebogen** in der Google Play Console für **AlbionDataPro**:

| Kategorie | Datentyp | Erhoben? | Geteilt? | Verschlüsselt in Transit? | Zweck |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Geräte- oder andere IDs** | Geräte-ID (Hardware-ID) | **Ja** | **Nein** | **Ja** | App-Funktionalität, Lizenzprüfung & Sicherheitsmanagement |
| **App-Aktivität** | Absturzprotokolle (Crash Logs) | **Ja** | **Nein** | **Ja** | Fehlerbehebung (Debugging) & Performance |
| **Finanzdaten** | Zahlungsinformationen | **Nein** *(extern über PayPal)* | **Nein** | **Ja** | Lizenzkäufe (keine direkte Speicherung sensibler Zahlungsdaten in der App) |

### Zusätzliche Fragen im Fragebogen:
1. **Werden Daten verschlüsselt übertragen?** $\rightarrow$ **Ja** (alle Serverkommunikationen laufen über verschlüsseltes HTTPS).
2. **Können Nutzer die Löschung ihrer Daten beantragen?** $\rightarrow$ **Ja** (durch Kontaktaufnahme über die Support-E-Mail).
