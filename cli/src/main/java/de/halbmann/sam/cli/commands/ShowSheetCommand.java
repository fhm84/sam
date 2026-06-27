package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.bind.Jsonb;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "show",
        description = "Show details of a specific music sheet",
        mixinStandardHelpOptions = true)
public class ShowSheetCommand implements Runnable {

    @Inject
    @RestClient
    SamResources client;

    @Inject
    Jsonb jsonb;

    @CommandLine.Parameters(index = "0", description = "ID of the music sheet to show")
    String id;

    @CommandLine.Option(
            names = {"-j", "--json"},
            description = "Output as JSON")
    boolean jsonOutput;

    @Override
    public void run() {
        try {
            SheetMusic sheet = client.sheets().load(id);

            if (jsonOutput) {
                String json = jsonb.toJson(sheet);
                System.out.println(json);
            } else {
                printSheet(sheet);
            }
        } catch (NotFoundException e) {
            System.err.println("Sheet with ID " + id + " not found.");
            throw new CommandLine.ExecutionException(new CommandLine(this), "Sheet not found", e);
        } catch (Exception e) {
            System.err.println("Error retrieving sheet: " + e.getMessage());
            throw new CommandLine.ExecutionException(new CommandLine(this), "Failed to retrieve sheet", e);
        }
    }

    private void printSheet(SheetMusic sheet) {
        System.out.println("Music Sheet Details:");
        System.out.println("===================");
        System.out.println("ID:       " + sheet.getId());
        System.out.println("Title:    " + sheet.getTitle());
    }
}
