package de.halbmann.sam.cli.controller;

import de.halbmann.sam.cli.CliErrorReporter;
import de.halbmann.sam.cli.controller.sync.Addition;
import de.halbmann.sam.cli.controller.sync.FieldDiff;
import de.halbmann.sam.cli.controller.sync.FieldDiffs;
import de.halbmann.sam.cli.entity.ImportResult;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.WebApplicationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Shared import flow for all entity types (ADR-0019): parse the JSON file, run Jakarta Bean
 * Validation locally, then look the record up on the target. A missing record is created; an
 * existing one gets the child records it lacks, and its differing fields are reported — written
 * only with {@code update}. Nothing is deleted. A dry run does the lookup and the comparison
 * (read-only) and writes nothing.
 *
 * @param <T> the DTO type one file deserializes into
 * @param <E> the DTO type the target returns for an existing record
 */
public abstract class AbstractImporter<T, E> {

    @Inject
    Jsonb jsonb;

    @Inject
    Validator validator;

    @Inject
    CliErrorReporter errorReporter;

    /** DTO class one file deserializes into. */
    protected abstract Class<T> type();

    /** Human-readable identification of the record, e.g. its title or name. */
    protected abstract String describe(T dto);

    /** The matching record on the target, by the file's ID if it has one, else by natural key. */
    protected abstract Optional<E> findExisting(T dto);

    /** Creates the record (and its child records) on the target. */
    protected abstract void create(T dto);

    /** The fields compared with an existing record (and written with {@code --update}). */
    protected abstract List<String> comparedFields();

    /** Writes the existing record, with the file's differing values already copied onto it. */
    protected abstract void update(E existing);

    /** Child records in the file that the existing record lacks; computing them writes nothing. */
    protected List<Addition> missingChildren(final T dto, final E existing) {
        return List.of();
    }

    /** Additional non-annotation validation; return one message per problem. */
    protected List<String> extraValidation(final T dto) {
        return List.of();
    }

    public ImportResult importFile(final File file, final boolean dryRun, final boolean update) {
        if (!file.exists()) {
            System.err.println("File not found: " + file.getPath());
            return ImportResult.failed(file);
        }

        final T dto;
        try (var bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            dto = jsonb.fromJson(bis, type());
        } catch (IOException | RuntimeException e) {
            errorReporter.printError("  ✗ Error reading file " + file.getName(), e);
            return ImportResult.failed(file);
        }

        List<String> problems = new ArrayList<>();
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        violations.forEach(v -> problems.add(v.getPropertyPath() + " " + v.getMessage()));
        problems.addAll(extraValidation(dto));
        if (!problems.isEmpty()) {
            System.err.println("  ✗ Invalid file " + file.getName() + ":");
            problems.forEach(p -> System.err.println("      - " + p));
            return ImportResult.failed(file);
        }

        System.out.println("Processing " + file.getName() + " ...");
        String would = dryRun ? "[DRY RUN] Would " : "";
        try {
            Optional<E> existing = findExisting(dto);
            if (existing.isEmpty()) {
                if (dryRun) {
                    System.out.println("  [DRY RUN] Would create: " + describe(dto));
                } else {
                    create(dto);
                }
                return ImportResult.created(file);
            }
            return sync(file, dto, existing.get(), dryRun, update, would);
        } catch (Exception e) {
            errorReporter.printError("  ✗ Error importing file " + file.getName(), e);
            return ImportResult.failed(file);
        }
    }

    private ImportResult sync(
            final File file,
            final T dto,
            final E existing,
            final boolean dryRun,
            final boolean update,
            final String would) {
        List<FieldDiff> diffs = FieldDiffs.compare(dto, existing, comparedFields());
        List<Addition> additions = missingChildren(dto, existing);
        if (diffs.isEmpty() && additions.isEmpty()) {
            System.out.println("  = Unchanged: " + describe(dto));
            return new ImportResult(file, ImportResult.Status.UNCHANGED, false);
        }

        boolean writeFields = update && !diffs.isEmpty();
        if (!diffs.isEmpty()) {
            System.out.println(
                    writeFields
                            ? "  ~ " + (dryRun ? would + "update" : "Updated") + ": " + describe(dto)
                            : "  ! Differs (not updated, use --update): " + describe(dto));
            diffs.forEach(d -> System.out.println("      " + d));
            // fields first: a full-record update must not undo child records added just before
            if (writeFields && !dryRun) {
                FieldDiffs.apply(diffs, existing);
                update(existing);
            }
        }
        for (Addition addition : additions) {
            if (!dryRun) {
                addition.apply().run();
            }
            System.out.println("  + " + (dryRun ? would + "add" : "Added") + ": " + addition.description());
        }

        boolean written = writeFields || !additions.isEmpty();
        return new ImportResult(
                file,
                written ? ImportResult.Status.UPDATED : ImportResult.Status.UNCHANGED,
                !diffs.isEmpty() && !writeFields);
    }

    /** The record the call returns, or empty if the target answers 404. */
    protected static <X> Optional<X> loadIfExists(final Supplier<X> load) {
        try {
            return Optional.ofNullable(load.get());
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /** The single element, empty for none; several are ambiguous and stop the import of the file. */
    protected static <X> Optional<X> single(final List<X> matches, final String what) {
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    matches.size() + " records match " + what + " on the target — resolve the duplicates in SAM first");
        }
        return matches.stream().findFirst();
    }
}
