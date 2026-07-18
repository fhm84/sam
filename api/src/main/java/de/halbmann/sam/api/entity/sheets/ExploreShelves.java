package de.halbmann.sam.api.entity.sheets;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Curated groupings of sheets shown on the Sheets Overview's Explore view.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExploreShelves {

    List<SheetMusicSearchResult> quickFillers = new ArrayList<>();

    List<SheetMusicSearchResult> bigFinishes = new ArrayList<>();

    List<SheetMusicSearchResult> recentlyAdded = new ArrayList<>();

    /** Most setlist appearances within the last 12 months. */
    List<SheetMusicSearchResult> crowdPleasers = new ArrayList<>();

    /** Never appeared in any setlist. */
    List<SheetMusicSearchResult> hiddenGems = new ArrayList<>();

    /**
     * Coverage status {@code INCOMPLETE} for the requested ensemble. Empty when no ensemble is
     * selected or no coverage snapshot has been computed yet.
     */
    List<SheetMusicSearchResult> needsAttention = new ArrayList<>();

    List<TagCount> tagCloud = new ArrayList<>();
}
