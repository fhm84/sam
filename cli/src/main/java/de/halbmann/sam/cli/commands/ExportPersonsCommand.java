package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.persons.PersonFilterRequest;
import de.halbmann.sam.cli.util.FilenameUtils;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/** Exports all persons (composers, arrangers, members' names) as JSON files readable by {@code importPerson}. */
@Unremovable
@Singleton
@CommandLine.Command(
        name = "exportPerson",
        description = "Export all persons to JSON file(s), one per person",
        mixinStandardHelpOptions = true)
public class ExportPersonsCommand extends AbstractExportCommand<Person> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected List<Person> fetchAll() {
        PersonFilterRequest request = new PersonFilterRequest();
        request.setSize(-1);
        return client.persons().findPersons(request).getData();
    }

    @Override
    protected String filenameFor(final Person person, final Set<String> usedNames) {
        return FilenameUtils.uniqueFilename(person.getName(), person.getId(), usedNames);
    }

    @Override
    protected String describe(final Person person) {
        return person.getName();
    }

    @Override
    protected String noun() {
        return "person(s)";
    }

    @Override
    protected Object toExport(final Person person) {
        return person;
    }
}
