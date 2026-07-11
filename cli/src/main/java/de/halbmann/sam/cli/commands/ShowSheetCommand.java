package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.cli.CliErrorReporter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.bind.Jsonb;
import jakarta.ws.rs.WebApplicationException;
import java.util.concurrent.Callable;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "show",
        description = "Show details of a specific music sheet",
        mixinStandardHelpOptions = true)
public class ShowSheetCommand implements Callable<Integer> {

    @Inject
    @RestClient
    SamResources client;

    @Inject
    Jsonb jsonb;

    @Inject
    CliErrorReporter errorReporter;

    @CommandLine.Parameters(index = "0", description = "ID of the music sheet to show")
    String id;

    @CommandLine.Option(
            names = {"-j", "--json"},
            description = "Output as JSON")
    boolean jsonOutput;

    @Override
    public Integer call() {
        try {
            SheetMusic sheet = client.sheets().load(id);

            if (jsonOutput) {
                String json = jsonb.toJson(sheet);
                System.out.println(json);
            } else {
                printSheet(sheet);
            }
            return 0;
        } catch (WebApplicationException e) {
            // the reactive REST client throws ClientWebApplicationException, not NotFoundException
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                System.err.println("Sheet with ID " + id + " not found.");
            } else {
                errorReporter.printError("Error retrieving sheet", e);
            }
            return 1;
        } catch (Exception e) {
            errorReporter.printError("Error retrieving sheet", e);
            return 1;
        }
    }

    private void printSheet(SheetMusic sheet) {
        System.out.println("Music Sheet Details:");
        System.out.println("===================");
        System.out.println("ID:        " + sheet.getId());
        System.out.println("Title:     " + sheet.getTitle());
        if (sheet.getSubtitle() != null) {
            System.out.println("Subtitle:  " + sheet.getSubtitle());
        }
        if (sheet.getComposer() != null) {
            System.out.println("Composer:  " + sheet.getComposer().getName());
        }
        if (sheet.getArranger() != null) {
            System.out.println("Arranger:  " + sheet.getArranger().getName());
        }
        if (sheet.getGenre() != null) {
            System.out.println("Genre:     " + sheet.getGenre());
        }
        if (sheet.getStyle() != null) {
            System.out.println("Style:     " + sheet.getStyle());
        }
        if (sheet.getPublisher() != null) {
            System.out.println("Publisher: " + sheet.getPublisher());
        }
        if (sheet.getTags() != null && !sheet.getTags().isEmpty()) {
            System.out.println("Tags:      " + String.join(", ", sheet.getTags()));
        }
        if (sheet.getInstrumentations() != null && !sheet.getInstrumentations().isEmpty()) {
            System.out.println("Parts:     " + sheet.getInstrumentations().size());
            sheet.getInstrumentations()
                    .forEach(i -> System.out.printf(
                            "  - %s%s%n",
                            i.getPartLabel() != null ? i.getPartLabel() + " " : "",
                            i.getInstrument() != null ? i.getInstrument().getName() : "?"));
        }
    }
}
