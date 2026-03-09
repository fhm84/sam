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

**Instrument-Matching** (exakt, kein automatisches Alias/Transpositions-Fallback):

* Matching erfolgt ausschließlich auf Basis der **Instrument-ID** (exakter Treffer)
* Ersatzinstrumente werden über explizite `VoiceOption`-Einträge (ALTERNATE / FALLBACK) mit eigenem `factor` konfiguriert
* Sekundäre Korrekturfaktoren (werden auf den Match-Score multipliziert):
    * *Clef-Faktor*: 0,7 bei nicht-transponierenden Instrumenten mit gesetztem Clef, sonst 1,0
    * *Notation-Typ-Faktor*: 1,0 Standard/Lead-Sheet, 0,8 Schlagzeug, 0,7 Tabulatur/Grafik
* Score < 0,3 → kein sinnvoller Treffer (wird als 0 gewertet)
* Stimmen ohne Optionen können keiner Instrumentation zugeordnet werden

**Zuteilung (greedy, priorisiert):**

Stimmen werden in Prioritätsreihenfolge verarbeitet (Pflichtstimmen zuerst, dann nach Gewicht absteigend). Jede Instrumentation kann nur einer Stimme zugeordnet werden.

**Score pro Stimme:**

```
effectiveCount = Σ (matchScore × option.factor)   ← für alle zugeteilten Instrumentierungen
normalized     = min(effectiveCount / targetCount, 1.0)
countScore     = baseScore + (1 − baseScore) × normalized
```

`baseScore` (Standard: **0,7**, konfigurierbar via `sam.coverage.base-score`): eine einzige passende Stimme erreicht sofort mindestens 70 % des Stimmenbeitrags.

**Gesamt-Coverage:**

```
coverageScore = Σ(countScore × voice.weight) / Σ(voice.weight)
```

**Status:**

| Status | Bedingung |
|--------|-----------|
| `INCOMPLETE` | Mindestens eine Pflichtstimme fehlt |
| `PLAYABLE` | Alle Pflichtstimmen abgedeckt; `coverageScore < 0,85` |
| `COMPLETE` | Alle Pflichtstimmen abgedeckt; `coverageScore ≥ 0,85` |

Pflichtstimmen fehlen → Stück als nicht spielbar markiert, unabhängig vom Gesamtscore

---

### Snapshot-Strategie

**CoverageSnapshot**

* Zweck: Listenansichten, Sortierung, Filter
* Inhalte:

    * coverageScore
    * status (COMPLETE / PLAYABLE / INCOMPLETE)
    * missingRequired
    * details (JSONB – vollständige Stimmen-Aufschlüsselung inkl. Begründungen)
    * lastUpdate (Zeitpunkt der letzten Berechnung, aus AbstractBaseEntity)

**Regeln**

* Snapshots werden per **Upsert** aktualisiert (INSERT … ON CONFLICT DO UPDATE)
* **Keine automatische Invalidierung** – manuelle Neuberechnung via `POST /api/ensembles/{id}/coverage/compute`

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

* Live-Berechnung der Vollständigkeit eines Notensatzes gegen ein Ensemble
* Greedy-Zuteilung der Instrumentierungen (Pflichtstimmen zuerst)
* Liefert `CoverageResult` mit `coverageScore`, `status`, `missingRequired` und Detailergebnissen pro Stimme inkl. menschenlesbarer Begründungen

**MatchingService**

* Instrument-Matching (exakter ID-Vergleich)
* Korrekturfaktoren für Clef und NotationType

**CoverageSnapshotService**

* Massenberechnung aller Notensätze gegen ein Ensemble (`compute`)
* Snapshot-Abfrage für Listen-Queries (`findSummaries`)
* Gibt `EnsembleCoverageStatus` zurück (Anzahl Snapshots + Zeitpunkt der letzten Berechnung)

---

### API (Auszug)

* `GET /api/sheets/{id}/coverage?ensemble=…` — Live-Auswertung eines Notensatzes
* `GET /api/sheets?ensemble=…` — Notensatz-Suche mit Snapshot-Coverage je Ergebnis
* `POST /api/ensembles/{id}/coverage/compute` — Snapshots für alle Notensätze neu berechnen
* `GET /api/ensembles/{id}/coverage/status` — Zeitpunkt und Anzahl vorhandener Snapshots

---

## 3. Ticket-Liste (Umsetzungs-Roadmap)

### Phase 1 – Fundament

* [x] Instrument-Entity inkl. ID-Strategie
* [x] Instrumentation → Link auf Instrument
* [x] Basis-Ensemble-Modelle

### Phase 2 – Matching & Bewertung

* [x] MatchingService (exakter ID-Vergleich + Clef/NotationType-Faktoren)
* [x] VoiceOption-Modell mit Typ (PRIMARY/ALTERNATE/FALLBACK) & Faktor
* [x] CoverageEvaluationService (greedy, priorisiert, baseScore-Floor)
* [x] Pflichtstimmen-Logik

### Phase 3 – Snapshot & Performance

* [x] CoverageSnapshot-Entity (JSONB-Details, Upsert)
* [x] CoverageSnapshotService (Massenberechnung, Listenabfrage)
* [ ] Automatische Invalidierung bei Änderungen (aktuell: manuelle Neuberechnung)
* [ ] Regel-Versionierung

### Phase 4 – Admin-UI

* [ ] Instrumentenverwaltung
* [x] Ensemble-Stimmen-Konfiguration (Voices + VoiceOptions)
* [ ] Simulations-/Test-Ansicht

### Phase 5 – UX & Feinschliff

* [x] Ampel-Logik (COMPLETE / PLAYABLE / INCOMPLETE) mit Badges
* [x] Detail-Breakdown pro Stimme (Coverage-Tab im Sheet-Detail)
* [ ] Audit-Log für Regeländerungen
* [ ] Basis-Statistiken

---

## 4. Ergebnis

SAM wird damit:

* musikalisch sinnvoll
* erklärbar
* erweiterbar
* performant
* wartbar

Dieses Dokument ist als **lebende Referenz** gedacht – Regeln & Gewichtungen dürfen sich ändern, die Architektur bleibt stabil.
