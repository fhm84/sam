package de.halbmann.sam.api.entity.assistant;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Ranked list of AI-suggested setlist items, returned by the program builder. */
@Data
public class SetlistSuggestions {

    List<SuggestedSetlistItem> items = new ArrayList<>();
}
