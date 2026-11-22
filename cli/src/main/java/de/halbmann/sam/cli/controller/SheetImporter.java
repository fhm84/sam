package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetFilterRequest;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.cli.controller.sync.Addition;
import de.halbmann.sam.cli.mapper.DataMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class SheetImporter extends AbstractImporter<SheetMusic, SheetMusic> {

    /** The sheet's own fields: those of {@code CreateSheetMusic}, without the collection link. */
    static final List<String> FIELDS = List.of(
            "title",
            "subtitle",
            "publisher",
            "publisherIpi",
            "composer",
            "arranger",
            "originalBy",
            "genre",
            "style",
            "difficultyLevel",
            "duration",
            "tempo",
            "tonality",
            "yearOfComposition",
            "edition",
            "copyright",
            "rightsStatus",
            "gemaReportable",
            "arrangementPublisher",
            "arrangementRightsUntil",
            "rating",
            "iswc",
            "gemaWorkNumber",
            "additionalNotes",
            "source");

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
    protected Optional<SheetMusic> findExisting(final SheetMusic sheet) {
        if (sheet.getId() != null) {
            Optional<SheetMusic> byId =
                    loadIfExists(() -> client.sheets().load(sheet.getId().toString()));
            if (byId.isPresent()) {
                return byId;
            }
        }
        SheetFilterRequest filter = new SheetFilterRequest();
        filter.setTitle(sheet.getTitle());
        filter.setSize(-1);
        // the server-side title filter may match fuzzily — compare exactly (but case-insensitively)
        // on title + publisher, since legacy data holds same-titled sheets from different publishers
        return single(
                        client.sheets().findSheets(filter).getData().stream()
                                .filter(existing -> sheet.getTitle().equalsIgnoreCase(existing.getTitle())
                                        && equalsIgnoreCaseNullSafe(sheet.getPublisher(), existing.getPublisher()))
                                .toList(),
                        "title + publisher of " + describe(sheet))
                .map(match -> client.sheets().load(match.getId().toString()));
    }

    @Override
    protected void create(final SheetMusic sheet) {
        final SheetMusic created = client.sheets().add(dataMapper.createFromSheet(sheet));
        System.out.println("  ✓ Created: " + created.getTitle() + " (ID: " + created.getId() + ")");
        List<Instrumentation> parts = uniqueParts(sheet.getInstrumentations(), new HashSet<>());
        if (!parts.isEmpty()) {
            addParts(created.getId().toString(), parts);
            parts.forEach(i -> System.out.println("      ✓ Part: " + describe(i)));
        }
    }

    @Override
    protected List<String> comparedFields() {
        return FIELDS;
    }

    @Override
    protected void update(final SheetMusic existing) {
        client.sheets().update(existing.getId().toString(), existing);
    }

    @Override
    protected List<Addition> missingChildren(final SheetMusic sheet, final SheetMusic existing) {
        String sheetId = existing.getId().toString();
        List<Addition> additions = new ArrayList<>();

        Set<String> present = client.sheets().instrumentations(sheetId).listAll().stream()
                .map(SheetImporter::partKey)
                .collect(Collectors.toCollection(HashSet::new));
        List<Instrumentation> missing = uniqueParts(sheet.getInstrumentations(), present);
        if (!missing.isEmpty()) {
            additions.add(new Addition(
                    missing.size() + " part(s): "
                            + missing.stream().map(SheetImporter::describe).collect(Collectors.joining(", ")),
                    () -> addParts(sheetId, missing)));
        }

        Set<String> tags = missingTags(sheet.getTags(), existing.getTags());
        if (!tags.isEmpty()) {
            additions.add(new Addition("tag(s) " + tags, () -> client.sheets().addTags(sheetId, tags)));
        }
        return additions;
    }

    /** The file's parts not yet in {@code present}, each once (adds them to {@code present}). */
    static List<Instrumentation> uniqueParts(final List<Instrumentation> parts, final Set<String> present) {
        if (parts == null) {
            return List.of();
        }
        return parts.stream().filter(i -> present.add(partKey(i))).toList();
    }

    /** The database's unique key of a sheet's part: instrument, part label, clef. */
    static String partKey(final Instrumentation part) {
        return part.getInstrument().getId() + "\u0000" + part.getPartLabel() + "\u0000" + part.getClef();
    }

    /** The server stores tags in lower case. */
    static Set<String> missingTags(final Set<String> fileTags, final Set<String> targetTags) {
        if (fileTags == null || fileTags.isEmpty()) {
            return Set.of();
        }
        Set<String> present = targetTags == null
                ? Set.of()
                : targetTags.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        return fileTags.stream()
                .map(t -> t.toLowerCase(Locale.ROOT))
                .filter(t -> !present.contains(t))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void addParts(final String sheetId, final List<Instrumentation> parts) {
        List<CreateInstrumentation> create =
                parts.stream().map(dataMapper::createFromInstrumentation).toList();
        client.sheets().instrumentations(sheetId).add(create);
    }

    private static String describe(final Instrumentation part) {
        String label = part.getPartLabel() == null ? "" : part.getPartLabel() + " ";
        return label + part.getInstrument().getId();
    }

    private static boolean equalsIgnoreCaseNullSafe(final String a, final String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }
}
