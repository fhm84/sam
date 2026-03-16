package de.halbmann.sam.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.api.entity.Instrument;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.api.entity.SheetMusic;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class DataConversion {

    Map<String, LegacyMapping> genreMappings;

    @Test
    void shouldConvert() throws Exception {
        JsonArray jsonValues;
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Path dataFile = Paths.get("", "src", "test", "resources", "src/migration-data/noten_mkn.json");
            jsonValues = jsonb.fromJson(Files.newBufferedReader(dataFile), JsonArray.class);

            List<LegacyMapping> mappings = List.of(jsonb.fromJson(
                    Files.newBufferedReader(Paths.get("", "src", "main", "resources", "legacy-genre-mapping.json")),
                    LegacyMapping[].class));
            genreMappings =
                    mappings.stream().collect(Collectors.toMap(LegacyMapping::getLegacyGenre, Function.identity()));
        }

        List<SheetMusic> list =
                jsonValues.stream().map(this::convertToSheetMusic).toList();
        assertEquals(718, list.size());
        // list.forEach(this::print);

        Set<Instrument> instruments = list.stream()
                .flatMap(sheet -> sheet.getInstrumentations().stream())
                .map(Instrumentation::getInstrument)
                .collect(Collectors.toSet());

        // prepare directories
        Files.createDirectories(Paths.get("", "src", "test", "resources", "src/migration-data/data", "sheets"));
        Files.createDirectories(Paths.get("", "src", "test", "resources", "src/migration-data/data", "instruments"));

        // write data
        // write instruments json files
        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            for (Instrument i : instruments) {
                export(i, jsonb);
            }
        }

        // write sheet music json files
        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            for (SheetMusic s : list) {
                export(s, jsonb);
            }
        }
    }

    SheetMusic convertToSheetMusic(final JsonValue jsonValue) {
        JsonObject jsonObject = jsonValue.asJsonObject();
        SheetMusic sheetMusic = new SheetMusic();
        sheetMusic.setTitle(jsonObject.getString("titel"));
        String art = jsonObject.getString("art");
        LegacyMapping legacyMapping = genreMappings.get(art);
        if (legacyMapping != null) {
            sheetMusic.setGenre(legacyMapping.getTargetGenre());
            sheetMusic.setStyle(legacyMapping.getTargetStyle());
            sheetMusic.setTags(legacyMapping.getTags());
        }
        sheetMusic.setPublisher(jsonObject.getString("verlag"));
        sheetMusic.setAdditionalNotes(jsonObject.getString("sonstiges"));
        List<Instrumentation> stimmen = InstrumentationParser.parseInstrumentations(jsonObject.getString("stimmen"));
        sheetMusic.setInstrumentations(stimmen);
        return sheetMusic;
    }

    void print(final SheetMusic sheetMusic) {
        System.out.printf("%s - %s (%s)\n", sheetMusic.getTitle(), sheetMusic.getPublisher(), sheetMusic.getGenre());
        sheetMusic.getInstrumentations().forEach(System.out::println);
    }

    void export(final SheetMusic sheetMusic, final Jsonb jsonb) throws IOException {
        String filename = prepareFilename(sheetMusic.getTitle());
        OutputStream outputStream;
        try {
            outputStream = Files.newOutputStream(
                    Paths.get("", "src", "test", "resources", "src/migration-data/data", "sheets", filename),
                    StandardOpenOption.CREATE_NEW);
        } catch (final FileAlreadyExistsException e) {
            filename = prepareFilename(sheetMusic.getTitle() + "-" + sheetMusic.getPublisher());
            outputStream = Files.newOutputStream(
                    Paths.get("", "src", "test", "resources", "src/migration-data/data", "sheets", filename),
                    StandardOpenOption.CREATE_NEW);
        }
        jsonb.toJson(sheetMusic, outputStream);
    }

    void export(final Instrument instrument, final Jsonb jsonb) throws IOException {
        String filename = prepareFilename(instrument.getId());
        OutputStream outputStream;
        try {
            outputStream = Files.newOutputStream(
                    Paths.get("", "src", "test", "resources", "src/migration-data/data", "instruments", filename),
                    StandardOpenOption.CREATE_NEW);
            jsonb.toJson(instrument, outputStream);
        } catch (final FileAlreadyExistsException e) {
            System.out.println("File " + filename + " already exists");
        }
    }

    String prepareFilename(final String title) {
        // First of all, "cleanup" special characters like e.g. german umlauts
        return FilenameUtils.cleanUp(title) + ".json";
    }
}
