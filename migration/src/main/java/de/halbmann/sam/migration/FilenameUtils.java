package de.halbmann.sam.migration;

public class FilenameUtils {

    public static String cleanUp(final String input) {
        return input.replace("\u00fc", "ue")
                .replace("\u00f6", "oe")
                .replace("\u00e4", "ae")
                .replace("\u00df", "ss")
                .replaceAll("\u00dc(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Ue")
                .replaceAll("\u00d6(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Oe")
                .replaceAll("\u00c4(?=[a-z\u00e4\u00f6\u00fc\u00df ])", "Ae")
                .replace("\u00dc", "UE")
                .replace("\u00d6", "OE")
                .replace("\u00c4", "AE")
                // others
                .replace("\u00c0", "A") // À
                .replace("\u00c1", "A") // Á
                .replace("\u00c8", "E") // È
                .replace("\u00c9", "E") // É
                .replace("\u00d2", "O") // Ò
                .replace("\u00d3", "O") // Ó
                .replace("\u00e0", "a") // à
                .replace("\u00e1", "a") // á
                .replace("\u00e8", "e") // è
                .replace("\u00e9", "e") // é
                .replace("\u00ec", "i") // ì
                .replace("\u00ed", "i") // í
                .replace("\u00f2", "o") // ò
                .replace("\u00f3", "o") // ó
                // replace all non-ascii characters by "_"
                .replace("'", "")
                .replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
