package de.halbmann.sam.migration;

import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Merges legacy sheets that share title and publisher into one sheet.
 *
 * <p>The legacy catalogue has one entry per physical sheet, and most sheets are printed on both
 * sides. A piece the band owns on two different double-sided sheets therefore appears twice,
 * each entry with the parts of its copy. SAM catalogues the piece: the merged sheet carries
 * every part of all copies (so coverage evaluation sees what is playable) and the notes of all
 * copies, which name the respective back side. {@code cli import} would otherwise keep only the
 * first entry, since it matches sheets on title and publisher.
 *
 * <p>Copies that disagree on anything the conversion sets besides parts and notes are not
 * merged silently: {@link #merge} throws, and the export needs fixing.
 */
public final class DuplicateSheets {

    private DuplicateSheets() {}

    /**
     * @param sheets the merged list, in input order (a merged sheet takes the first copy's place)
     * @param merged one line per merged sheet, for the conversion report
     */
    public record Result(List<SheetMusic> sheets, List<String> merged) {}

    public static Result merge(List<SheetMusic> sheets) {
        Map<String, List<SheetMusic>> groups = new LinkedHashMap<>();
        for (SheetMusic sheet : sheets) {
            groups.computeIfAbsent(key(sheet), k -> new ArrayList<>()).add(sheet);
        }
        List<SheetMusic> result = new ArrayList<>();
        List<String> merged = new ArrayList<>();
        for (List<SheetMusic> group : groups.values()) {
            SheetMusic sheet = group.getFirst();
            if (group.size() > 1) {
                mergeInto(sheet, group.subList(1, group.size()));
                merged.add("%s (%s): %d copies, %d parts"
                        .formatted(
                                sheet.getTitle(),
                                sheet.getPublisher(),
                                group.size(),
                                sheet.getInstrumentations().size()));
            }
            result.add(sheet);
        }
        return new Result(result, merged);
    }

    /** Same comparison as {@code cli import}: title and publisher, ignoring case. */
    static String key(SheetMusic sheet) {
        return sheet.getTitle().strip().toLowerCase(Locale.GERMAN) + "\u0000"
                + Objects.toString(sheet.getPublisher(), "").strip().toLowerCase(Locale.GERMAN);
    }

    private static void mergeInto(SheetMusic target, List<SheetMusic> copies) {
        for (SheetMusic copy : copies) {
            requireSame(target, copy, "genre", SheetMusic::getGenre);
            requireSame(target, copy, "style", SheetMusic::getStyle);
            requireSame(target, copy, "tags", SheetMusic::getTags);
            requireSame(target, copy, "composer", s -> name(s.getComposer()));
            requireSame(target, copy, "arranger", s -> name(s.getArranger()));
        }

        Set<String> parts = new HashSet<>();
        List<Instrumentation> instrumentations = new ArrayList<>();
        Set<String> notes = new LinkedHashSet<>();
        for (SheetMusic sheet : concat(target, copies)) {
            for (Instrumentation instrumentation : sheet.getInstrumentations()) {
                if (parts.add(partKey(instrumentation))) {
                    instrumentations.add(instrumentation);
                }
            }
            if (sheet.getAdditionalNotes() != null) {
                notes.add(sheet.getAdditionalNotes());
            }
        }
        target.setInstrumentations(instrumentations);
        target.setAdditionalNotes(notes.isEmpty() ? null : String.join("\n", notes));
    }

    private static List<SheetMusic> concat(SheetMusic first, List<SheetMusic> rest) {
        List<SheetMusic> all = new ArrayList<>(rest.size() + 1);
        all.add(first);
        all.addAll(rest);
        return all;
    }

    private static String partKey(Instrumentation instrumentation) {
        return instrumentation.getInstrument().getId() + "\u0000" + instrumentation.getPartLabel();
    }

    private static void requireSame(
            SheetMusic target, SheetMusic copy, String field, Function<SheetMusic, Object> getter) {
        Object expected = getter.apply(target);
        Object actual = getter.apply(copy);
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException("Copies of '%s' (%s) differ in %s: %s vs. %s — fix the export"
                    .formatted(target.getTitle(), target.getPublisher(), field, expected, actual));
        }
    }

    private static String name(Person person) {
        return person == null ? null : person.getName();
    }
}
