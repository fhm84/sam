package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.CreateInstrument;
import de.halbmann.sam.api.entity.Instrument;
import de.halbmann.sam.cli.entity.ImportResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.bind.Jsonb;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class InstrumentImporter {

    @Inject
    @RestClient
    SamResources client;

    @Inject
    Jsonb jsonb;

    public ImportResult importFile(final File file, final boolean dryRun) {
        if (!file.exists()) {
            System.err.println("File not found: " + file.getPath());
            return new ImportResult(file, false);
        }

        try {
            CreateInstrument instrument = parseJsonFile(file);
            System.out.println("Processing " + file.getName() + " ...");

            if (dryRun) {
                System.out.println("  [DRY RUN] Would import: " + instrument.getDisplayName());
            } else {
                Instrument created = client.instruments().add(instrument);
                System.out.println("  ✓ Imported: " + created.getDisplayName() + " (ID: " + created.getId() + ")");
            }
            return new ImportResult(file, true);
        } catch (IOException e) {
            System.err.println("  ✗ Error reading file " + file.getName() + ": " + e.getMessage());
            return new ImportResult(file, false);
        } catch (Exception e) {
            System.err.println("  ✗ Error importing file " + file.getName() + ": " + e.getMessage());
            return new ImportResult(file, false);
        }
    }

    private CreateInstrument parseJsonFile(File file) throws IOException {
        try (var bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            return jsonb.fromJson(bis, CreateInstrument.class);
        }
    }
}
