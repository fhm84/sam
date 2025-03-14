# code-with-quarkus

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/code-with-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)




# Sheet Music Archiving Data Model Documentation

## Overview
This data model is designed for an application that archives sheetMusic music, supports bands with multiple instrumentations, and allows for different notations per instrument.

## Entities

### **SheetMusic**
Represents a piece of music.
- `id` (UUID): Unique identifier
- `title` (String): Name of the piece
- `composer_id` (UUID): Reference to Composer
- `arranger_id` (UUID, optional): Reference to Arranger
- `genre` (String): Classification (e.g., Classical, Jazz)
- `difficulty_level` (String): Level (Beginner, Intermediate, Advanced)
- `time_signature` (String): Time signature (e.g., 4/4, 3/4)
- `year_of_composition` (Integer): Year of composition
- `publisher` (String): Publisher name
- `edition` (String): Edition name
- `license` (String): Licensing information
- `audio_reference_url` (String): External link to an audio reference
- `preview_image_path` (String): Thumbnail or first page preview
- `performance_notes` (String): Additional notes

### **Instrumentation**
Defines individual instrument parts for a piece of sheetMusic music.
- `id` (UUID): Unique identifier
- `sheet_music_id` (UUID): Reference to SheetMusic
- `instrument_name` (String): Instrument name (e.g., Trumpet, Violin)
- `key_signature` (String): Specific key signature for this instrument
- `clef` (String): Clef type (e.g., Treble, Bass, Alto, Tenor)
- `notation_type` (String): Type of notation (Standard, Tablature, Percussion)
- `pdf_file_path` (String): Location of the sheetMusic music file
- `midi_file_path` (String, optional): MIDI file location

### **Composer**
Stores information about composers and arrangers.
- `id` (UUID): Unique identifier
- `name` (String): Full name
- `birth_year` (Integer): Year of birth
- `death_year` (Integer, optional): Year of death

### **Band**
Represents a group of musicians using the sheetMusic music.
- `id` (UUID): Unique identifier
- `name` (String): Band name
- `description` (String): Short description

### **Band_Member**
Links users to bands with specific roles.
- `id` (UUID): Unique identifier
- `band_id` (UUID): Reference to Band
- `user_id` (UUID): Reference to User
- `role` (String): Role in the band (e.g., Trumpet Player, Conductor)

### **Collection**
Groups multiple pieces of sheetMusic music.
- `id` (UUID): Unique identifier
- `name` (String): Collection name
- `description` (String): Short description

### **SheetMusic_Collection**
Many-to-many relationship between SheetMusic and Collection.
- `sheet_music_id` (UUID): Reference to SheetMusic
- `collection_id` (UUID): Reference to Collection

### **Tag**
Represents a tag for organizing sheetMusic music.
- `id` (UUID): Unique identifier
- `name` (String): Tag name

### **SheetMusic_Tag**
Many-to-many relationship between SheetMusic and Tag.
- `sheet_music_id` (UUID): Reference to SheetMusic
- `tag_id` (UUID): Reference to Tag

## Constraints
- **Instrumentation (Unique Constraint):** Each combination of `sheet_music_id`, `instrument_name`, and `notation_type` must be unique to prevent duplicates.
- **Foreign Key Relationships:** Ensures referential integrity between entities.

This model provides a structured way to manage sheetMusic music with instrument-specific transpositions, bands, collections, and tagging for efficient organization.




TODOs (features):
security (keycloak)
multitenancy
file-upload (pdf, midi?)
AI for automatically link pdf to correct sheet music and instrumentation!?
system-info (api/info)?
search
extended revinfo
download multiple (selected) pdfs at once (as single, merged pdf or as zip-file)
generate table of content (for sheet collections) - different sorting/grouping
export(s) e.g. for GEMA
Dashboard/statistics (number of sheets by composer/arranger/genre)
manage ensemble(s) (to also double-check availabilities?)
