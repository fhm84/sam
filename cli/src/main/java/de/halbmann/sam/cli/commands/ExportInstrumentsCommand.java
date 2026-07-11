package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.instruments.Instrument;
import de.halbmann.sam.api.entity.instruments.InstrumentFilterRequest;
import de.halbmann.sam.cli.util.FilenameUtils;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/**
 * Exports all instrument definitions as JSON files readable by {@code importInstrument}
 * ({@code Instrument} and {@code CreateInstrument} share the same field set).
 */
@Unremovable
@Singleton
@CommandLine.Command(
        name = "exportInstrument",
        description = "Export all instruments to JSON file(s), one per instrument",
        mixinStandardHelpOptions = true)
public class ExportInstrumentsCommand extends AbstractExportCommand<Instrument> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected List<Instrument> fetchAll() {
        InstrumentFilterRequest request = new InstrumentFilterRequest();
        request.setSize(-1);
        return client.instruments().findInstruments(request).getData();
    }

    @Override
    protected String filenameFor(final Instrument instrument, final Set<String> usedNames) {
        // natural IDs (e.g. TROMPETE_BB) are unique by definition
        return FilenameUtils.uniqueFilename(instrument.getId(), instrument.getId(), usedNames);
    }

    @Override
    protected String describe(final Instrument instrument) {
        return instrument.getDisplayName() != null ? instrument.getDisplayName() : instrument.getId();
    }

    @Override
    protected String noun() {
        return "instrument(s)";
    }
}
