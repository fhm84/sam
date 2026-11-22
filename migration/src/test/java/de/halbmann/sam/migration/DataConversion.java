package de.halbmann.sam.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.instruments.Instrument;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * One-off regeneration script (a JUnit test in name only, not picked up by the normal build) —
 * see {@code migration/README.md}.
 */
public class DataConversion {

    /**
     * Folder with the legacy exports and the hand-maintained mapping files; the converted files
     * go to its {@code data/} subfolder. Real data, so it lives outside this repo: set {@code
     * -Dsam.migration.dataDir=…} (relative paths resolve against the {@code migration} module).
     */
    private static final Path DATA_DIR = Paths.get(System.getProperty("sam.migration.dataDir", "src/migration-data"))
            .toAbsolutePath()
            .normalize();

    Map<String, LegacyMapping> genreMappings;
    Map<String, PublisherMapping> publisherMappings;

    /** Legacy parts that could not be parsed, keyed by "uid title". */
    final Map<String, List<String>> unparsedParts = new TreeMap<>();

    /** Legacy publisher values without an entry in the publisher mapping. */
    final Set<String> unmappedPublishers = new java.util.TreeSet<>();

    @Test
    void shouldConvert() throws Exception {
        assertTrue(
                Files.isRegularFile(DATA_DIR.resolve("noten_mkn.json")),
                () -> "No noten_mkn.json in " + DATA_DIR + " — set -Dsam.migration.dataDir (see migration/README.md)");
        System.out.println("Reading legacy data from " + DATA_DIR);
        JsonArray jsonValues;
        try (Jsonb jsonb = JsonbBuilder.create()) {
            jsonValues = jsonb.fromJson(Files.newBufferedReader(DATA_DIR.resolve("noten_mkn.json")), JsonArray.class);

            List<LegacyMapping> mappings = List.of(jsonb.fromJson(
                    Files.newBufferedReader(Paths.get("", "src", "main", "resources", "legacy-genre-mapping.json")),
                    LegacyMapping[].class));
            genreMappings =
                    mappings.stream().collect(Collectors.toMap(LegacyMapping::getLegacyGenre, Function.identity()));

            // Real publisher data, hence next to the export (gitignored) rather than in resources/
            List<PublisherMapping> publishers = List.of(jsonb.fromJson(
                    Files.newBufferedReader(DATA_DIR.resolve("publisher-mapping.json")), PublisherMapping[].class));
            publisherMappings = publishers.stream()
                    .collect(Collectors.toMap(p -> p.getLegacy().strip(), Function.identity()));
        }

        List<SheetMusic> converted =
                jsonValues.stream().map(this::convertToSheetMusic).toList();
        assertEquals(718, converted.size());

        assertTrue(
                unparsedParts.isEmpty(),
                () -> "Unparsable parts (extend InstrumentationParser or fix noten_mkn.json):\n"
                        + unparsedParts.entrySet().stream()
                                .map(e -> "  " + e.getKey() + ": " + e.getValue())
                                .collect(Collectors.joining("\n")));
        assertTrue(
                unmappedPublishers.isEmpty(),
                () -> "Publishers missing in publisher-mapping.json: " + unmappedPublishers);

        // A piece on several double-sided sheets is one legacy entry per sheet; SAM gets one sheet
        DuplicateSheets.Result merged = DuplicateSheets.merge(converted);
        merged.merged().forEach(line -> System.out.println("Merged " + line));
        List<SheetMusic> list = merged.sheets();

        Set<Instrument> instruments = list.stream()
                .flatMap(sheet -> sheet.getInstrumentations().stream())
                .map(Instrumentation::getInstrument)
                .collect(Collectors.toSet());
        assertEquals(
                instruments.size(),
                instruments.stream().map(Instrument::getId).distinct().count(),
                "Instrument IDs must be unique");

        // prepare directories, clearing previous output so reruns are idempotent
        Path sheetsDir = DATA_DIR.resolve("data").resolve("sheets");
        Path instrumentsDir = DATA_DIR.resolve("data").resolve("instruments");
        deleteDirectoryContents(sheetsDir);
        deleteDirectoryContents(instrumentsDir);
        Files.createDirectories(sheetsDir);
        Files.createDirectories(instrumentsDir);

        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            for (Instrument i : instruments) {
                export(i, jsonb);
            }
            for (SheetMusic s : list) {
                export(s, jsonb);
            }
        }
        System.out.printf(
                "Converted %d sheets with %d parts, %d instruments%n",
                list.size(),
                list.stream().mapToInt(s -> s.getInstrumentations().size()).sum(),
                instruments.size());
    }

    SheetMusic convertToSheetMusic(final JsonValue jsonValue) {
        JsonObject jsonObject = jsonValue.asJsonObject();
        SheetMusic sheetMusic = new SheetMusic();
        sheetMusic.setTitle(jsonObject.getString("titel").strip());
        String art = jsonObject.getString("art");
        LegacyMapping legacyMapping = genreMappings.get(art);
        if (legacyMapping != null) {
            sheetMusic.setGenre(legacyMapping.getTargetGenre());
            sheetMusic.setStyle(legacyMapping.getTargetStyle());
            sheetMusic.setTags(legacyMapping.getTags());
        }
        applyPublisher(sheetMusic, jsonObject.getString("verlag").strip());
        sheetMusic.setAdditionalNotes(LegacyNotes.clean(jsonObject.getString("sonstiges")));

        InstrumentationParser.Result parts = InstrumentationParser.parse(jsonObject.getString("stimmen"));
        if (!parts.unparsed().isEmpty()) {
            unparsedParts.put(jsonObject.getInt("uid") + " " + sheetMusic.getTitle(), parts.unparsed());
        }
        if (!parts.duplicates().isEmpty()) {
            System.out.printf(
                    "Dropped repeated parts in %d %s: %s%n",
                    jsonObject.getInt("uid"), sheetMusic.getTitle(), parts.duplicates());
        }
        sheetMusic.setInstrumentations(new ArrayList<>(parts.instrumentations()));
        return sheetMusic;
    }

    private void applyPublisher(final SheetMusic sheetMusic, final String legacyPublisher) {
        if (legacyPublisher.isEmpty()) {
            return;
        }
        PublisherMapping mapping = publisherMappings.get(legacyPublisher);
        if (mapping == null) {
            unmappedPublishers.add(legacyPublisher);
            return;
        }
        sheetMusic.setPublisher(blankToNull(mapping.getPublisher()));
        sheetMusic.setComposer(person(mapping.getComposer()));
        sheetMusic.setArranger(person(mapping.getArranger()));
    }

    private static Person person(final String name) {
        if (blankToNull(name) == null) {
            return null;
        }
        Person person = new Person();
        person.setName(name.strip());
        return person;
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    void export(final SheetMusic sheetMusic, final Jsonb jsonb) throws IOException {
        Path sheets = DATA_DIR.resolve("data").resolve("sheets");
        Path file = sheets.resolve(prepareFilename(sheetMusic.getTitle()));
        if (Files.exists(file)) {
            file = sheets.resolve(prepareFilename(sheetMusic.getTitle() + "-" + sheetMusic.getPublisher()));
        }
        try (OutputStream outputStream = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
            jsonb.toJson(sheetMusic, outputStream);
        }
    }

    void export(final Instrument instrument, final Jsonb jsonb) throws IOException {
        String filename = prepareFilename(instrument.getId());
        try (OutputStream outputStream = Files.newOutputStream(
                DATA_DIR.resolve("data").resolve("instruments").resolve(filename), StandardOpenOption.CREATE_NEW)) {
            jsonb.toJson(instrument, outputStream);
        }
    }

    String prepareFilename(final String title) {
        // First of all, "cleanup" special characters like e.g. german umlauts; no leading dots
        // ("... einen bessern findst du nicht"), which would make the file hidden
        return FilenameUtils.cleanUp(title).replaceFirst("^[._]+", "") + ".json";
    }

    private static void deleteDirectoryContents(final Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                Files.delete(file);
            }
        }
    }
}
