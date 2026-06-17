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

    List<TagCount> tagCloud = new ArrayList<>();
}
