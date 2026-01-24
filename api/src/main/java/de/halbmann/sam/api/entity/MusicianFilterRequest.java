package de.halbmann.sam.api.entity;

import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MusicianFilterRequest extends PaginationRequest {

  @QueryParam("name")
  private String name;
}
