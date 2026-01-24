package de.halbmann.sam.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultMetrics {

  double ftsRank;
  double titleSimilarity;
  double composerSimilarity;
  boolean phoneticMatch;
  double finalRank;
}
