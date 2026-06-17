package de.halbmann.sam.api.entity.sheets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A tag together with how many sheets carry it. Used to render the Explore view's tag cloud.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagCount {

    String tag;

    long count;
}
