package de.halbmann.sam.business.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a piece of music.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "sheets")
@SqlResultSetMapping(
        name = "SheetWithMetrics",
        entities = @EntityResult(entityClass = SheetMusicEntity.class),
        columns = {
                @ColumnResult(name = "fts_rank", type = Double.class),
                @ColumnResult(name = "title_similarity", type = Double.class),
                @ColumnResult(name = "composer_similarity", type = Double.class),
                @ColumnResult(name = "phonetic_match", type = Boolean.class),
                @ColumnResult(name = "final_rank", type = Double.class)
        })
public class SheetMusicEntity extends AbstractEntity {

    /**
     * The title of the music sheet/piece.
     */
    String title;

    /**
     * (Optional) Subtitle of the piece.
     */
    String subtitle;

    /**
     * The publisher of the music sheet.
     */
    String publisher;

    /**
     * Interested Party Information (IPI)-Number of the publisher.
     */
    String publisherIpi;

    /**
     * The composer of the music sheet.
     */
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    MusicianEntity composer;

    /**
     * The arranger of the music sheet.
     */
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    MusicianEntity arranger;

    /**
     * Optional (e.g., original band or composer)
     */
    String originalBy;

    /**
     * Classification (e.g., Classical, Jazz)
     */
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    GenreEntity genre;

    /**
     * Level (Beginner, Intermediate, Advanced).
     */
    String difficultyLevel;

    /**
     * Year of composition.
     */
    Integer yearOfComposition;

    /**
     * Edition name.
     */
    String edition;

    /**
     * Copyright information.
     */
    String copyright;

    /**
     * Rating for the piece/music sheet.
     */
    Integer rating;

    /**
     * International Standard Musical Work Code
     */
    String iswc;

    /**
     * Identification number of GEMA (GEMA-Werk Nr.)
     */
    String gemaWorkNumber;

    /**
     * Additional notes.
     */
    @Column(columnDefinition = "text")
    String additionalNotes;

    /**
     * Individual instrument parts.
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sheet", orphanRemoval = true)
    List<InstrumentationEntity> instrumentations = new ArrayList<>();

    /**
     * Metadata of the sheet music files (including location, mime-type, fileSize, ...)
     */
    @OneToMany(fetch = FetchType.LAZY)
    Set<AttachmentEntity> attachments;
}
