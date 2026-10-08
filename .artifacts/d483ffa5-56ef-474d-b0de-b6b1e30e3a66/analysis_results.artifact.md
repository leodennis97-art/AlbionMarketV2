# Verbesserungsvorschläge: Kategorien-Funktionen & Preis-Feineinstellung (DataPro - Market Companion)

Diese Analyse und das Optimierungskonzept beleuchten konkrete Verbesserungsmöglichkeiten für die **Kategorien-Funktionen** sowie die **Feineinstellung der Preise** in der App **DataPro - Market Companion (Unofficial)** für Albion Online.

---

## 1. Verbesserung der Kategorien-Funktionen (Category Functions)

### Ist-Zustand
- Die App nutzt `ResourceCategory` mit 14 Hauptkategorien (Waffen, Rüstung, Rohstoffe, Essen etc.) sowie Macro-Kategorien im Floating Bubble Service ("SAMMLER", "GEAR", "GASTRO", "LUXUS").
- Filterung erfolgt meist linear oder über Einzelauswahl.

### Optimierungsvorschläge
1. **Multi-Select & Kombinierte Filter (Mehrfachauswahl)**
   - *Problem:* Aktuell kann man oft nur eine Kategorie gleichzeitig auswählen. Händler möchten jedoch z.B. gleichzeitig *Rohstoffe* (`RESOURCES`) und *Veredelt* (`REFINED`) oder *Waffen* und *Rüstung* filtern.
   - *Lösung:* Ersetzung des Single-Choice-Chip-Filters durch eine Multi-Select Chip-Leiste (Toggle Chips), bei der mehrere Kategorien gleichzeitig aktiv sein können.
2. **Dynamische Unterkategorien (Sub-Categories / Slot-Filter)**
   - *Waffen/Rüstung:* Zusätzliche Filter nach Waffentyp (Schwerter, Bögen, Stäbe) oder Rüstungstyp (Platte, Leder, Stoff).
   - *Rohstoffe:* Schneller Wechsel zwischen Roherz, Erzen, Holz, Fellen, Fasern und Steinen direkt als Sub-Filter.
3. **Item-Counter Badges**
   - Anzeige der Anzahl gefundener Items bzw. profitabler Handelsmöglichkeiten direkt am Kategorien-Tab (z.B. `⚔️ Waffen (12)`).
4. **Favoriten & Custom-Kategorien**
   - Möglichkeit für User, eigene Item-Gruppen oder Favoriten-Listen anzulegen (z.B. "Meine Crafting-Materialien" oder "Black-Market-Flipping-Liste").

---

## 2. Feineinstellung der Preise & Handelslogik (Price Fine-Tuning)

### Ist-Zustand
- Der `AiMarketAnalyzer` berechnet Kauf- und Verkaufspreise mit festen prozentualen Offsets:
  - Kauforder: `buyPrice * 0.935` (-6.5% Dip)
  - Verkauforder: `sellPrice * 1.112` (+11.2% Peak)
- Steuern und Gebühren: Pauschal 5% Marktsteuer + 2.5% Einrichtungsgebühr (insgesamt 7.5%).
- Anomalie-Filter: Ausschluss bei Preisen < 10 Silber oder > 4x Preisverhältnis.

### Optimierungsvorschläge
1. **Premium-Status Umschalter (Market Tax Toggle)**
   - *Hintergrund:* Spieler mit Premium-Status haben in Albion Online reduzierte Marktsteuern (3% statt 6% bzw. Gesamtlast ca. 4.5% statt 7.5%–9%).
   - *Lösung:* Ein globaler Premium-Schalter in den Einstellungen / UI, der die Netto-Marge automatisch anpasst und realistische Gewinne für Premium-Spieler berechnet.
2. **Volatilitäts-basierte Preis-Offsets (Adaptive Order-Spreads)**
   - *Problem:* Feste 6.5% / 11.2% Offsets passen nicht für alle Items. Hochvolatile Items (z.B. Artefakte, T8.3 Ausrüstung) schwanken stärker als Standard-Rohstoffe (T4.0).
   - *Lösung:* Dynamische Berechnung der Kauf-/Verkauforder-Margen basierend auf der Preisspanne (Spread) zwischen den Städten:
     $$\text{RecBuy} = \text{LowestPrice} \times (1.0 - \text{VolatilityFactor})$$
3. **Transport-Risiko & Zonen-Gewichtung (Danger Zone Risk Factor)**
   - Erweiterung der Routenberechnung um ein Risikoprofil:
     - Royal Cities (Sicher, geringe Marge)
     - Caerleon & Schwarz- / Rotzonen (Höheres Risiko, höhere Marge, Gank-Gefahr)
     - Brecilien (Mists-Hub)
   - Integration eines Risiko-Multiplikators in die Gewinn-Score-Berechnung.
4. **Individueller Marge-Schwellenwert (Margin Slider)**
   - Ein intuitiver Schieberegler in der UI, mit dem der Benutzer die minimale Gewinnmarge (z.B. 5%, 10%, 15%, 20%) und das maximale Silber-Budget live anpassen kann.

---

## 3. Empfohlene Umsetzungsschritte (Roadmap)

1. **Phase 1 (UI & Kategorien):** Umstellung der Kategorie-Filter auf Multi-Select in der Hauptansicht (`AlbionResourceScreen`) und im Floating Bubble Overlay.
2. **Phase 2 (Preise & Steuern):** Einführung des Premium-Steuer-Modus (4.5% vs. 9.0%) und des Margen-Schiebereglers.
3. **Phase 3 (KI-Erweiterung):** Verfeinerung der Order-Strategie-Algorithmen im `AiMarketAnalyzer` mit adaptiven Spreads.
