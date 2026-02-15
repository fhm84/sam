package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.MusiciansResource;
import de.halbmann.sam.api.entity.Musician;
import de.halbmann.sam.api.entity.MusicianFilterRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.business.controller.MusicianService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class MusiciansResourceImpl implements MusiciansResource {

    @Inject
    MusicianService musicianService;

    @Override
    public PaginatedResponse<Musician> findMusicians(MusicianFilterRequest filterRequest) {
        return musicianService.findMusicians(filterRequest);
    }

    @Override
    public Musician add(Musician musician) {
        return musicianService.addMusician(musician);
    }

    @Override
    public Musician load(String musicianId) {
        return musicianService.getMusician(musicianId);
    }

    @Override
    public void update(String musicianId, Musician musician) {
        musicianService.updateMusician(musicianId, musician);
    }

    @Override
    public void delete(String musicianId) {
        musicianService.deleteMusician(musicianId);
    }
}
