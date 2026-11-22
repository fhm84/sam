package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.cli.CliErrorReporter;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.validation.Validation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/** Wires an importer without Quarkus, so its REST client can be a plain Mockito mock. */
final class ImporterTestSupport {

    static final Jsonb JSONB = JsonbBuilder.create();

    private ImporterTestSupport() {}

    static <I extends AbstractImporter<?, ?>> I wire(final I importer) {
        importer.jsonb = JSONB;
        importer.validator = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()
                .getValidator();
        importer.errorReporter = new CliErrorReporter();
        return importer;
    }

    /** Writes the record as an import file. */
    static File file(final Path dir, final String name, final Object dto) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, JSONB.toJson(dto));
        return file.toFile();
    }

    static <T> PaginatedResponse<T> page(final List<T> data) {
        PaginatedResponse<T> page = new PaginatedResponse<>();
        page.setData(data);
        page.setSize(data.size());
        page.setTotalCount(data.size());
        return page;
    }
}
