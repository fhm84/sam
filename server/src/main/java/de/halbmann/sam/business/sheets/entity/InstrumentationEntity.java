package de.halbmann.sam.business.sheets.entity;

import de.halbmann.sam.api.entity.instruments.Clef;
import de.halbmann.sam.api.entity.sheets.NotationType;
import de.halbmann.sam.api.entity.sheets.PhysicalCondition;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Defines individual instrument parts for a piece of sheet music.
 */
@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(
        name = "instrumentations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uc_instrumentation",
                        columnNames = {"sheet_id", "instrument_id", "partLabel"}))
public class InstrumentationEntity extends AbstractEntity {

    /**
     * The linked instrument.
     */
    @ManyToOne(optional = false)
    InstrumentEntity instrument;

    /**
     * The part number/label (for example: '2' for 2. Bass, or even '3rd' or 'Solo')
     */
    String partLabel;

    /**
     * Clef type (e.g., Treble, Bass, Alto, Tenor)
     */
    @Enumerated(EnumType.STRING)
    Clef clef;

    /**
     * Type of notation (Standard, Tablature, Percussion)
     */
    @Enumerated(EnumType.STRING)
    NotationType notationType;

    /**
     * Metadata of the sheet music/instrumentation files (including location, mime-type, fileSize,
     * ...)
     */
    @OneToMany(fetch = FetchType.LAZY)
    Set<AttachmentEntity> attachments;

    /**
     * (Optional) notes for the part
     */
    @Column(columnDefinition = "text")
    String notes;

    /**
     * Physical archive location of the printed copy (e.g. "Cabinet A / Shelf 3 / Folder 12").
     */
    @Column(columnDefinition = "text")
    String physicalLocation;

    /**
     * Physical condition of the printed copy in the archive.
     */
    @Enumerated(EnumType.STRING)
    PhysicalCondition physicalCondition;

    /**
     * Sheet music entity, the instrumentation belongs to.
     */
    @ManyToOne
    SheetMusicEntity sheet;
}
