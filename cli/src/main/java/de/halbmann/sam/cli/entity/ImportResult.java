package de.halbmann.sam.cli.entity;

import java.io.File;

public record ImportResult(File file, boolean success) {
}
