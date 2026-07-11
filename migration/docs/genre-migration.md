# 🎼 Mapping: Legacy-Genres → `Genre` + `Style` (+ Tags)
## Grundannahmen (wichtig!)

**Zielmodell:**

- **Genre** = musikalische Form / Gattung
- **Style** = musikalische Sprache / Groove
- **Tags** = Anlass, Stimmung, Herkunft, Marketing-Begriffe

➡️ **Medley / Potpourri ≠ Genre**, sondern **Form + Tag**

### 1️⃣ Märsche & marschverwandt

| Legacy           | Genre | Style       | Tags      |
| ---------------- | ----- | ----------- | --------- |
| Marsch           | MARCH | TRADITIONAL |           |
| Stimmungsmarsch  | MARCH | TRADITIONAL | stimmung  |
| Triumph-Marsch   | MARCH | TRADITIONAL | festlich  |
| Marcia festivo   | MARCH | TRADITIONAL | festlich  |
| Marsch-Beat      | MARCH | MODERN      | beat      |
| Marsch-Fox       | MARCH | SWING       | foxtrot   |
| Marsch-Polka     | MARCH | TRADITIONAL | polka     |
| Samba-Marsch     | MARCH | LATIN       | samba     |
| Marsch-Potpourri | MARCH | TRADITIONAL | potpourri |


### 2️⃣ Walzer / Polka / Tanzformen

| Legacy           | Genre | Style       | Tags      |
| ---------------- | ----- | ----------- | --------- |
| Walzer           | WALTZ | TRADITIONAL |           |
| Konzertwalzer    | WALTZ | CLASSICAL   | concert   |
| Beat-Waltz       | WALTZ | MODERN      | beat      |
| Stimmungs-Walzer | WALTZ | TRADITIONAL | stimmung  |
| Polka            | POLKA | TRADITIONAL |           |
| Party-Polka      | POLKA | TRADITIONAL | party     |
| Tenorhorn-Polka  | POLKA | TRADITIONAL | tenorhorn |
| Beat-Polka       | POLKA | MODERN      | beat      |
| Samba-Polka      | POLKA | LATIN       | samba     |
| Polka-Potpourri  | POLKA | TRADITIONAL | potpourri |


### 3️⃣ Medleys / Potpourris / Selections

➡️ Genre nach musikalischem Material,  
➡️ immer Tag: `medley` oder `potpourri`

| Legacy                   | Genre        | Style       | Tags             |
| ------------------------ | ------------ | ----------- | ---------------- |
| Medley                   | CONCERT_WORK | MODERN      | medley           |
| Potpourri                | CONCERT_WORK | TRADITIONAL | potpourri        |
| Stimmungsmedley          | CONCERT_WORK | TRADITIONAL | medley, stimmung |
| Weihnachtsmedley         | CHRISTMAS    | TRADITIONAL | medley           |
| Big-Band-Medley          | JAZZ         | SWING       | medley           |
| Schlager-Medley          | POP_ROCK     | POP         | medley, schlager |
| Swing-Medley             | JAZZ         | SWING       | medley           |
| Volkslieder-Potpourri    | FOLK_SONG    | TRADITIONAL | potpourri        |
| Walzer-Potpourri         | WALTZ        | TRADITIONAL | potpourri        |
| Seemannslieder-Potpourri | FOLK_SONG    | TRADITIONAL | potpourri        |


### 4️⃣ Pop / Rock / Beat / Modern

| Legacy           | Genre        | Style  | Tags      |
| ---------------- | ------------ | ------ | --------- |
| Pop-Beat         | POP_ROCK     | POP    | beat      |
| Rock             | POP_ROCK     | ROCK   |           |
| Slow-Rock        | POP_ROCK     | ROCK   | slow      |
| Rock-Shuffle     | POP_ROCK     | ROCK   | shuffle   |
| Disco-Beat       | POP_ROCK     | FUNK   | disco     |
| Techno-Beat      | POP_ROCK     | MODERN | techno    |
| Modern Selection | CONCERT_WORK | MODERN | selection |
| Soundtrack       | FILM_MUSIC   | MODERN |           |
| Musical          | SHOW_MUSIC   | MODERN |           |
| Ballade          | CONCERT_WORK | MODERN | ballad    |


### 5️⃣ Jazz / Swing / Big Band

| Legacy          | Genre    | Style       | Tags          |
| --------------- | -------- | ----------- | ------------- |
| Jazz            | JAZZ     | SWING       |               |
| Swing           | JAZZ     | SWING       |               |
| Dixieland       | JAZZ     | TRADITIONAL | dixieland     |
| Blues           | JAZZ     | SWING       | blues         |
| Big-Band-Medley | JAZZ     | SWING       | medley        |
| Disco-Shuffle   | POP_ROCK | FUNK        | shuffle       |
| Beat-Fox        | DANCE    | MODERN      | foxtrot, beat |


### 6️⃣ Klassisch / Konzertant

| Legacy              | Genre        | Style       | Tags     |
| ------------------- | ------------ | ----------- | -------- |
| Ouvertüre           | OVERTURE     | CLASSICAL   |          |
| Fantasie            | FANTASY      | CLASSICAL   |          |
| Sonate              | CONCERT_WORK | CLASSICAL   | sonata   |
| Arie                | CONCERT_WORK | CLASSICAL   | aria     |
| Intermezzo          | CONCERT_WORK | CLASSICAL   |          |
| Festliches Vorspiel | OVERTURE     | CLASSICAL   | festlich |
| Fanfare             | CONCERT_WORK | TRADITIONAL | fanfare  |
| Hymne               | HYMN_CHORALE | TRADITIONAL |          |
| Choral              | HYMN_CHORALE | TRADITIONAL |          |


### 7️⃣ Latin / World / Folk

| Legacy             | Genre     | Style        | Tags        |
| ------------------ | --------- | ------------ | ----------- |
| Samba              | DANCE     | LATIN        | samba       |
| Calypso            | DANCE     | LATIN        | calypso     |
| Tango              | DANCE     | LATIN        | tango       |
| Beguine            | DANCE     | LATIN        | beguine     |
| Tarantella         | DANCE     | FOLKLORISTIC |             |
| Jenka              | DANCE     | FOLKLORISTIC |             |
| Galopp             | DANCE     | TRADITIONAL  |             |
| Foxtrott           | DANCE     | TRADITIONAL  | foxtrot     |
| Hawaiian Beat      | DANCE     | LATIN        | hawaii      |
| Latin-Beat         | DANCE     | LATIN        | beat        |
| Latin-Foxtrott     | DANCE     | LATIN        | foxtrot     |
| Spanische Melodien | FOLK_SONG | FOLKLORISTIC | spain       |
| La Bamba           | FOLK_SONG | LATIN        | traditional |


### 8️⃣ Volksmusik / Geistlich

| Legacy                  | Genre     | Style        | Tags   |
| ----------------------- | --------- | ------------ | ------ |
| Volkstümliche Blasmusik | FOLK_SONG | TRADITIONAL  |        |
| Volkslieder             | FOLK_SONG | TRADITIONAL  |        |
| Folklore                | FOLK_SONG | FOLKLORISTIC |        |
| Kirchenlied             | SACRED    | TRADITIONAL  |        |
| Choral                  | HYMN_CHORALE | TRADITIONAL |     |
| Lied                    | FOLK_SONG | TRADITIONAL  |        |
| Weihnachtsmedley        | CHRISTMAS | TRADITIONAL  | medley |


### 9️⃣ Sonderfälle / Cleanup

| Legacy               | Empfehlung                       |
| -------------------- | -------------------------------- |
| `new`                | ❌ löschen / ignorieren           |
| `Opening`            | Genre nach Werk, Tag: `opening`  |
| `Moderato`           | ❌ kein Genre → Tempo             |
| `Medium-Beat`        | Style: MODERN, Tag: medium       |
| `Slow` / `Slow-Beat` | Tag: slow                        |
| `Calliope`           | CONCERT_WORK + Tag: calliope     |
| `Traditional`        | ❌ kein Genre → Style.TRADITIONAL |
