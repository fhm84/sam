package de.halbmann.sam.business.documents.controller;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Framework-agnostic functional interface for writing content to an {@link OutputStream}.
 */
@FunctionalInterface
public interface StreamWriter {
    void write(OutputStream out) throws IOException;
}
