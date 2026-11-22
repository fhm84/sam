package de.halbmann.sam.cli.controller.sync;

/**
 * A child record (or a group of them) in the import file that the target record lacks.
 *
 * @param description what would be added, for the report
 * @param apply writes it to the target; not run in a dry run
 */
public record Addition(String description, Runnable apply) {}
