package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.MusiciansResource;
import de.halbmann.sam.api.entity.Musician;
import de.halbmann.sam.api.entity.MusicianFilterRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.business.boundary.MusicianRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class MusiciansResourceImpl implements MusiciansResource {

  @Inject MusicianRepository musicianRepository;

  @Override
  public PaginatedResponse<Musician> findMusicians(MusicianFilterRequest filterRequest) {
    if (filterRequest.getName() != null && !filterRequest.getName().isEmpty()) {
      return musicianRepository.findMusicians(filterRequest);
    } else {
      return musicianRepository.getAllMusicians(filterRequest);
    }
  }

  @Override
  public Musician add(Musician musician) {
    return musicianRepository.addMusician(musician);
  }

  @Override
  public Musician load(String musicianId) {
    return musicianRepository.getMusician(musicianId);
  }

  @Override
  public void update(String musicianId, Musician musician) {
    musicianRepository.updateMusician(musicianId, musician);
  }

  @Override
  public void delete(String musicianId) {
    musicianRepository.deleteMusician(musicianId);
  }
}
