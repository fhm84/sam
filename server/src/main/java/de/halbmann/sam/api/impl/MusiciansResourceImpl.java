package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.MusiciansResource;
import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.musicians.controller.MusicianService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@Authenticated
@RequestScoped
public class MusiciansResourceImpl implements MusiciansResource {

    @Inject
    MusicianService musicianService;

    @Override
    public PaginatedResponse<Musician> findMusicians(MusicianFilterRequest filterRequest) {
        return musicianService.findMusicians(filterRequest);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public Musician add(Musician musician) {
        return musicianService.addMusician(musician);
    }

    @Override
    public Musician load(String musicianId) {
        return musicianService.getMusician(musicianId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void update(String musicianId, Musician musician) {
        musicianService.updateMusician(musicianId, musician);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void delete(String musicianId) {
        musicianService.deleteMusician(musicianId);
    }

    @Override
    @RolesAllowed(Roles.ADMIN)
    public void linkUser(String musicianId, String userId) {
        musicianService.linkUser(musicianId, userId);
    }

    @Override
    @RolesAllowed(Roles.ADMIN)
    public void unlinkUser(String musicianId) {
        musicianService.unlinkUser(musicianId);
    }
}
