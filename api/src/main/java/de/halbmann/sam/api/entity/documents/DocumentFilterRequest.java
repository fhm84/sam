package de.halbmann.sam.api.entity.documents;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import lombok.Getter;
import lombok.Setter;

/** Query parameters for the document list endpoint. Currently only carries pagination fields. */
@Getter
@Setter
public class DocumentFilterRequest extends PaginationRequest {}
