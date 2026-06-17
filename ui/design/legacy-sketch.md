# Sheet Music Archive – UI Erkenntnisse & ToDo

## Ziel
Eine einheitliche Detail-Ansicht für Notensätze (Sätze für Musikvereine),  
optimiert für **Desktop & Tablet**, mit klarer Struktur, hoher Übersichtlichkeit  
und guter Erweiterbarkeit (z. B. Angular / Quarkus Frontend).

---

## 1. Grundprinzip UI

- **Eine Detail-Komponente**
- **Ein HTML / eine Component**
- Keine getrennten Desktop-/Tablet-Seiten
- Responsive Anpassung über Grid, Collapsibles und Priorisierung
- Mobile wird implizit unterstützt, aber nicht aktiv optimiert

---

## 2. Grobstruktur der Detail-Ansicht (immer gleiche Reihenfolge)

### 1️⃣ Header (immer sichtbar)
- Titel
- Untertitel
- Composer / Arranger / Publisher
- Badges:
    - Genre
    - Schwierigkeit
- Rating (Sterne)

### 2️⃣ Dateien
- Partitur
- Satzmappe / PDF-Uploads
- Download-Aktionen
- Visuell als Card

### 3️⃣ Basisdaten
- Original von
- Genre
- Edition
- Jahr
- Weitere Kerndaten
- Visuell als Card

### 4️⃣ Instrumentierungen
- Tabelle mit:
    - Instrument
    - Schlüssel
    - Tonart
    - Zugehörige Datei
- Keine Einzelstimmen-Fokussierung, sondern satzorientiert

### 5️⃣ Zusätzliche Informationen (Collapsible)
- Notizen
- Registrierung & Rechte (ISWC, GEMA, IPI)

---

## 3. Gruppierung der Felder (Datenmodell → UI)

### Base Data
- Title
- Subtitle
- Composer
- Arranger
- Publisher
- Original By

### Miscellaneous
- Genre
- Difficulty Level
- Rating
- Year of Composition
- Edition

### Registration Data
- ISWC
- GEMA Work Number
- Publisher IPI
- Copyright

### Additional Notes
- Freitext / Hinweise

### Instrumentations
- Instrument
- Key
- Transposition
- Attachment

---

## 4. Responsive Ansatz (Desktop & Tablet)

### Desktop (≥ 1200px)
- Zwei Spalten:
    - Links: Dateien
    - Rechts: Basisdaten
- Instrumentierungen als volle Breite
- Notizen offen
- Rechte standardmäßig eingeklappt

### Tablet (768–1199px)
- Maximal **eine Spalte**
- Dateien und Basisdaten untereinander
- Größere Click-Flächen
- Collapsibles häufiger geschlossen:
    - Notizen: collapsed
    - Rechte: collapsed

### Mobile
- Keine spezielle Optimierung
- Fällt automatisch untereinander
- Collapsibles zwingend notwendig

---

## 5. Collapsibles – Regeln

- Informationsdichte über Collapsibles steuern
- Inhalte **nicht ausblenden**, nur einklappen
- Unterschiedliche Default-Zustände je Breakpoint
- Clickbare Card-Header (kein Extra-Button)

---

## 6. Design & Theme

### Design-Ziele
- Ruhig, sachlich (Archiv / Bibliothek)
- Gute Scanbarkeit
- Icons & Badges zur schnellen Orientierung

### Umsetzung
- Bootstrap 5
- Bootstrap Icons
- Cards mit abgerundeten Ecken
- CSS-Variablen für Farben

### Dark Mode
- Über `data-theme="dark"`
- Keine HTML-Duplikate
- Alle Farben über CSS Custom Properties

---

## 7. Technische ToDos

- [ ] Detail-Komponente als eigenständige View bauen
- [ ] Responsive Grid (col-xl / col-lg) sauber definieren
- [ ] Collapsibles für optionale Sektionen
- [ ] CSS-Variablen für Theme extrahieren
- [ ] Dark-Mode Toggle vorbereiten
- [ ] Vorbereitung für Edit/View-Modus je Sektion

---

## 8. Zukunft (optional)

- Edit-Modus inline pro Card
- Angular BreakpointObserver
- Filter & Tags als Multiselect
- Tablet-spezifische Defaults
- Drag & Drop für Attachments

---
