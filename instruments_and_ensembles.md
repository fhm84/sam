# SAM – Architektur, Module & Ticket-Liste

---

## 1. Architektur-Dokument (Kurzfassung)

### Ziel

SAM bewertet Notensätze (SheetMusic) im Kontext von Ensembles hinsichtlich ihrer Vollständigkeit – **graduell, erklärbar und performant**.

### Leitprinzipien

* Fachliche Wahrheit wird **on-the-fly** berechnet
* Persistierte Daten sind **Hinweise (Snapshots)**, kein Truth Source
* **Datengetriebene Regeln**, keine Hardcodings
* **Erklärbarkeit vor Perfektion**

---

### Zentrale Domänenobjekte

**Instrument**

* Kanonische, stabile Entität
* Eindeutige `instrumentId` (z.B. `TENORHORN_BB`)
* Attribute: Familie, Rolle, Transposition, Register (optional)

**Instrumentation**

* Verbindung `SheetMusic ↔ Instrument`
* Kontextinformationen: partLabel, clef, notationType
* Referenziert Attachments (PDF, MusicXML)

**Ensemble**

* Beschreibt den Soll-Zustand
* Besteht aus EnsembleVoices

**EnsembleVoice**

* Musikalische Funktion (z.B. „Tenorhorn 1“)
* Gewicht (Bedeutung)
* Pflicht / optional

**VoiceOption**

* Mögliche Instrumente pro Stimme
* Primär / Alternativ / Notlösung
* Faktor (z.B. 1.0 / 0.85 / 0.6)

---

### Matching & Bewertung

* Matching ist **Score-basiert**
* Kriterien:

    * Instrument / Alias
    * Transposition
    * Clef
    * NotationType (optional)
* Scores werden **multipliziert**
* Untergrenze (z.B. < 0.3) = nicht sinnvoll

**Vollständigkeit:**

```
Summe(voiceWeight × optionFactor × matchScore)
---------------------------------------------
        Summe(voiceWeight)
```

Pflichtstimmen fehlen → Warnung, unabhängig vom Prozentwert

---

### Snapshot-Strategie

**CoverageSnapshot**

* Zweck: Listenansichten, Sortierung, Filter
* Inhalte:

    * coverageScore (gerundet)
    * status (COMPLETE / PLAYABLE / INCOMPLETE)
    * missingRequired
    * evaluatedAt

**Regeln**

* Snapshots werden **gelöscht oder ersetzt**, nicht gepflegt
* Invalidierung bei:

    * Änderung SheetMusic / Instrumentation
    * Änderung Ensemble
    * Regeländerung (via ruleVersion)

---

## 2. Modulübersicht

### Core Domain

* Instrument
* InstrumentAlias
* Instrumentation
* SheetMusic
* Ensemble
* EnsembleVoice
* VoiceOption

---

### Services

**CoverageEvaluationService**

* Live-Berechnung der Vollständigkeit
* Liefert Detailergebnisse inkl. Begründungen

**MatchingService**

* Instrument ↔ Instrument-Matching
* Alias-, Transpositions-, Clef-Scoring

**CoverageSnapshotService**

* Lesen / Erzeugen / Löschen von Snapshots
* Lazy-Recompute-Strategie

---

### Admin

**InstrumentAdminService**

* CRUD Instrumente
* Aktiv / Inaktiv

**AliasAdminService**

* Pflege von Ersatzbeziehungen
* Faktor-Änderungen

**SimulationService**

* Testet Matching ohne Persistenz
* Nutzt exakt gleiche Logik wie Produktivcode

---

### API (Auszug)

* `GET /sheetmusic/{id}/coverage?ensemble=…`
* `GET /sheetmusic?ensemble=…`
* `POST /admin/instruments`
* `POST /admin/aliases`
* `POST /admin/simulate`

---

## 3. Ticket-Liste (Umsetzungs-Roadmap)

### Phase 1 – Fundament

* [ ] Instrument-Entity inkl. ID-Strategie
* [ ] Instrumentation → Link auf Instrument
* [ ] Basis-Ensemble-Modelle

### Phase 2 – Matching & Bewertung

* [ ] MatchingService (Score-basiert)
* [ ] Alias-Modell inkl. Typ & Faktor
* [ ] CoverageEvaluationService
* [ ] Pflichtstimmen-Logik

### Phase 3 – Snapshot & Performance

* [ ] CoverageSnapshot-Entity
* [ ] SnapshotService (Lazy Recompute)
* [ ] Invalidierung bei Änderungen
* [ ] Regel-Versionierung

### Phase 4 – Admin-UI

* [ ] Instrumentenverwaltung
* [ ] Alias-/Ersatzpflege
* [ ] Ensemble-Stimmen-Konfiguration
* [ ] Test & Simulation View

### Phase 5 – UX & Feinschliff

* [ ] Ampel-Logik & Tooltips
* [ ] Detail-Breakdown pro Stimme
* [ ] Audit-Log für Regeländerungen
* [ ] Basis-Statistiken (häufige Ersetzungen)

---

## 4. Ergebnis

SAM wird damit:

* musikalisch sinnvoll
* erklärbar
* erweiterbar
* performant
* wartbar

Dieses Dokument ist als **lebende Referenz** gedacht – Regeln & Gewichtungen dürfen sich ändern, die Architektur bleibt stabil.
