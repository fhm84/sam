package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.sheets.SheetFilterRequest;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.cli.CliErrorReporter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.Callable;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(name = "list", description = "List all music sheets", mixinStandardHelpOptions = true)
public class ListSheetsCommand implements Callable<Integer> {

    @Inject
    @RestClient
    SamResources client;

    @Inject
    CliErrorReporter errorReporter;

    @CommandLine.Option(
            names = {"-v", "--verbose"},
            description = "Show detailed information")
    boolean verbose;

    @Override
    public Integer call() {
        try {
            SheetFilterRequest request = new SheetFilterRequest();
            request.setSize(-1); // disable pagination, list everything
            var sheets = client.sheets().findSheets(request);

            if (sheets.getData().isEmpty()) {
                System.out.println("No music sheets found.");
                return 0;
            }

            System.out.println("Found " + sheets.getTotalCount() + " music sheet(s):");

            for (SheetMusic sheet : sheets.getData()) {
                if (verbose) {
                    printDetailedSheet(sheet);
                } else {
                    printShortSheet(sheet);
                }
            }
            return 0;
        } catch (Exception e) {
            errorReporter.printError("Error listing sheets", e);
            return 1;
        }
    }

    private void printShortSheet(SheetMusic sheet) {
        System.out.printf("  [%s] %s%n", sheet.getId(), sheet.getTitle());
    }

    private void printDetailedSheet(SheetMusic sheet) {
        System.out.printf(
                "  [%s] %s - %s | genre: %s | %d part(s)%n",
                sheet.getId(),
                sheet.getTitle(),
                sheet.getComposer() != null ? sheet.getComposer().getName() : "unknown composer",
                sheet.getGenre() != null ? sheet.getGenre() : "-",
                sheet.getInstrumentations() != null
                        ? sheet.getInstrumentations().size()
                        : 0);
    }
}
