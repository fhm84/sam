package de.halbmann.sam.migration;

import lombok.Getter;
import lombok.Setter;

/**
 * One entry of the publisher mapping: the legacy {@code verlag} value and what it becomes.
 *
 * <p>The legacy field mixes publishers, spelling variants of the same publisher and persons
 * (composers or arrangers). {@code publisher} is the normalized publisher ({@code null} or blank
 * clears it); {@code composer}/{@code arranger} are set when the legacy value names a person
 * instead. {@code confidence} and {@code note} only document the review and are not imported.
 */
@Getter
@Setter
public class PublisherMapping {

    private String legacy;
    private String publisher;
    private String composer;
    private String arranger;
    private String confidence;
    private String note;
}
