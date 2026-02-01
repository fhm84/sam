package de.halbmann.sam.business.boundary;

import de.halbmann.sam.business.entity.GenreEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
@Transactional
public class GenreRepository implements PanacheRepositoryBase<GenreEntity, UUID> {

    public GenreEntity findOrCreateGenreByName(final String name) {
        try {
            return find("name = :name", Parameters.with("name", name)).singleResult();
        } catch (final NoResultException e) {
            final GenreEntity genreEntity = new GenreEntity();
            genreEntity.setName(name);
            return genreEntity;
        }
    }
}
