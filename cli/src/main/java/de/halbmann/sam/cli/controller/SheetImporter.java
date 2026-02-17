package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.CreateInstrumentation;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.api.entity.SheetMusic;
import de.halbmann.sam.cli.entity.ImportResult;
import de.halbmann.sam.cli.mapper.DataMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.bind.Jsonb;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class SheetImporter {

    @Inject
    @RestClient
    SamResources client;

    @Inject
    Jsonb jsonb;

    @Inject
    DataMapper dataMapper;

    public ImportResult importFile(final File file, final boolean dryRun) {
        if (!file.exists()) {
            System.err.println("File not found: " + file.getPath());
            return new ImportResult(file, false);
        }

        try {
            SheetMusic sheet = parseJsonFile(file);
            System.out.println("Processing " + file.getName() + " ...");

            if (dryRun) {
                System.out.println("  [DRY RUN] Would import: " + sheet.getTitle());
            } else {
                final SheetMusic created = client.sheets().add(dataMapper.createFromSheet(sheet));
                System.out.println("  ✓ Imported: " + created.getTitle() + " (ID: " + created.getId() + ")");
                if (sheet.getInstrumentations() != null
                        && !sheet.getInstrumentations().isEmpty()) {
                    List<CreateInstrumentation> createInstrumentations = sheet.getInstrumentations().stream()
                            .map(dataMapper::createFromInstrumentation)
                            .toList();
                    client.sheets().instrumentations(created.getId().toString()).add(createInstrumentations);
                    for (Instrumentation i : sheet.getInstrumentations()) {
                        System.out.println("      ✓ Imported: " + i);
                    }
                }
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

    private SheetMusic parseJsonFile(File file) throws IOException {
        try (var bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            return jsonb.fromJson(bis, SheetMusic.class);
        }
    }
}
