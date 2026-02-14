import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.api.Test;

public class DataConversion {

    @Test
    void shouldConvert() throws Exception {
        JsonArray jsonValues;
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Path dataFile = Paths.get("", "src", "test", "resources", "noten_mkn.json");
            jsonValues = jsonb.fromJson(Files.newBufferedReader(dataFile), JsonArray.class);
        }

        List<SheetMusic> list =
                jsonValues.stream().map(this::convertToSheetMusic).toList();
        assertEquals(718, list.size());
        // list.forEach(this::print);

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
        sheetMusic.setGenre(jsonObject.getString("art"));
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
                    Paths.get("", "src", "test", "resources", "data", filename), StandardOpenOption.CREATE_NEW);
        } catch (final FileAlreadyExistsException e) {
            filename = prepareFilename(sheetMusic.getTitle() + "-" + sheetMusic.getPublisher());
            outputStream = Files.newOutputStream(
                    Paths.get("", "src", "test", "resources", "data", filename), StandardOpenOption.CREATE_NEW);
        }
        jsonb.toJson(sheetMusic, outputStream);
    }

    String prepareFilename(final String title) {
        // First of all, "cleanup" special characters like e.g. german umlauts
        return cleanUp(title) + ".json";
    }

    public static String cleanUp(final String input) {
        return input.replace("\u00fc", "ue")
                .replace("\u00f6", "oe")
                .replace("\u00e4", "ae")
                .replace("\u00df", "ss")
                .replaceAll("\u00dc(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Ue")
                .replaceAll("\u00d6(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Oe")
                .replaceAll("\u00c4(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Ae")
                .replace("\u00dc", "UE")
                .replace("\u00d6", "OE")
                .replace("\u00c4", "AE")
                // others
                .replace("\u00c0", "A") // À
                .replace("\u00c1", "A") // Á
                .replace("\u00c8", "E") // È
                .replace("\u00c9", "E") // É
                .replace("\u00d2", "O") // Ò
                .replace("\u00d3", "O") // Ó
                .replace("\u00e0", "a") // à
                .replace("\u00e1", "a") // á
                .replace("\u00e8", "e") // è
                .replace("\u00e9", "e") // é
                .replace("\u00ec", "i") // ì
                .replace("\u00ed", "i") // í
                .replace("\u00f2", "o") // ò
                .replace("\u00f3", "o") // ó
                // replace all non-ascii characters by "_"
                .replace("'", "")
                .replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
