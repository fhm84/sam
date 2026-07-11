package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetFilterRequest;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.cli.mapper.DataMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class SheetImporter extends AbstractImporter<SheetMusic> {

    @Inject
    @RestClient
    SamResources client;

    @Inject
    DataMapper dataMapper;

    @Override
    protected Class<SheetMusic> type() {
        return SheetMusic.class;
    }

    @Override
    protected String describe(final SheetMusic sheet) {
        return sheet.getTitle() + (sheet.getPublisher() != null ? " (" + sheet.getPublisher() + ")" : "");
    }

    @Override
    protected boolean exists(final SheetMusic sheet) {
        SheetFilterRequest filter = new SheetFilterRequest();
        filter.setTitle(sheet.getTitle());
        filter.setSize(-1);
        // the server-side title filter may match fuzzily — compare exactly (but case-insensitively)
        // on title + publisher, since legacy data holds same-titled sheets from different publishers
        return client.sheets().findSheets(filter).getData().stream()
                .anyMatch(existing -> sheet.getTitle().equalsIgnoreCase(existing.getTitle())
                        && equalsIgnoreCaseNullSafe(sheet.getPublisher(), existing.getPublisher()));
    }

    @Override
    protected void create(final SheetMusic sheet) {
        final SheetMusic created = client.sheets().add(dataMapper.createFromSheet(sheet));
        System.out.println("  ✓ Imported: " + created.getTitle() + " (ID: " + created.getId() + ")");
        if (sheet.getInstrumentations() != null && !sheet.getInstrumentations().isEmpty()) {
            List<CreateInstrumentation> createInstrumentations = sheet.getInstrumentations().stream()
                    .map(dataMapper::createFromInstrumentation)
                    .toList();
            client.sheets().instrumentations(created.getId().toString()).add(createInstrumentations);
            for (Instrumentation i : sheet.getInstrumentations()) {
                System.out.println("      ✓ Imported: " + i);
            }
        }
    }

    private static boolean equalsIgnoreCaseNullSafe(final String a, final String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }
}
