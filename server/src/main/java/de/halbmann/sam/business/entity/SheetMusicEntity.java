package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.DifficultyLevel;
import de.halbmann.sam.api.entity.Genre;
import de.halbmann.sam.api.entity.Style;
import de.halbmann.sam.business.controller.FingerprintFactory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Represents a piece of music.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "sheets", uniqueConstraints = @UniqueConstraint(columnNames = {"fingerprint"}))
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
    @NotBlank
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
     * Primary musical genre of this sheet music.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Genre genre;

    /**
     * Stylistic classification of this sheet music.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Style style;

    /**
     * Difficulty Level.
     * Automatically mapped to grade (int) via {@link DifficultyLevelConverter}
     */
    DifficultyLevel difficultyLevel;

    /**
     * automatically mapped via {@link DurationAttributeConverter}
     */
    Duration duration;

    boolean favorite;

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

    @ElementCollection
    @CollectionTable(name = "sheet_music_tags")
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

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

    @Embedded
    FingerprintEntity fingerprint;

    @PrePersist
    void onSheetCreate() {
        this.fingerprint = FingerprintFactory.forSheet(this);
    }
}
