package de.halbmann.sam.business.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a piece of music.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "sheets")
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
     * License information.
     */
    String license;

    /**
     * Rating for the piece/music sheet.
     */
    Integer rating;

    /**
     * Additional notes.
     */
    String additionalNotes;

    /**
     * Individual instrument parts.
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sheet", orphanRemoval = true)
    List<InstrumentationEntity> instrumentations = new ArrayList<>();

}
