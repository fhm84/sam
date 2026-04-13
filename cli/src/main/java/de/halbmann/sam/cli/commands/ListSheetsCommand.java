package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.sheets.SheetFilterRequest;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(name = "list", description = "List all music sheets", mixinStandardHelpOptions = true)
public class ListSheetsCommand implements Runnable {

    @Inject
    @RestClient
    SamResources client;

    @CommandLine.Option(
            names = {"-v", "--verbose"},
            description = "Show detailed information")
    boolean verbose;

    @Override
    public void run() {
        try {
            SheetFilterRequest request = new SheetFilterRequest();
            var sheets = client.sheets().findSheets(request);

            if (sheets.getData().isEmpty()) {
                System.out.println("No music sheets found.");
                return;
            }

            System.out.println("Found " + sheets.getSize() + " music sheet(s):");

            for (SheetMusic sheet : sheets.getData()) {
                if (verbose) {
                    printDetailedSheet(sheet);
                } else {
                    printShortSheet(sheet);
                }
            }
        } catch (Exception e) {
            System.err.println("Error listing sheets: " + e.getMessage());
            throw new CommandLine.ExecutionException(new CommandLine(this), "Failed to list sheets", e);
        }
    }

    private void printShortSheet(SheetMusic sheet) {
        System.out.printf("  [%s] %s%n", sheet.getId(), sheet.getTitle());
    }

    private void printDetailedSheet(SheetMusic sheet) {
        System.out.println("  ID: " + sheet.getId());
        System.out.println("  Title: " + sheet.getTitle());
    }
}
