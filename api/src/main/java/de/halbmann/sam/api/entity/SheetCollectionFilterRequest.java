package de.halbmann.sam.api.entity;

import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SheetCollectionFilterRequest extends PaginationRequest {

  /** For "generic" search/query */
  @QueryParam("q")
  private String query;

  /** Search by name */
  @QueryParam("name")
  private String name;
}
