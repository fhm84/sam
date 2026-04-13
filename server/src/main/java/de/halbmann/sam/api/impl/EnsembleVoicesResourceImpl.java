package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.EnsembleVoicesResource;
import de.halbmann.sam.api.boundary.VoiceOptionsResource;
import de.halbmann.sam.api.entity.ensembles.CreateEnsembleVoice;
import de.halbmann.sam.api.entity.ensembles.EnsembleVoice;
import de.halbmann.sam.business.ensembles.controller.EnsembleVoiceService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.List;

@Authenticated
@RequestScoped
public class EnsembleVoicesResourceImpl implements EnsembleVoicesResource {

    @PathParam("ensembleId")
    String ensembleId;

    @Context
    ResourceContext resourceContext;

    @Inject
    EnsembleVoiceService ensembleVoiceService;

    @Override
    public List<EnsembleVoice> listAll() {
        return ensembleVoiceService.getVoices(ensembleId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public EnsembleVoice add(final CreateEnsembleVoice voice) {
        return ensembleVoiceService.addVoice(ensembleId, voice);
    }

    @Override
    public EnsembleVoice get(final String voiceId) {
        return ensembleVoiceService.getVoice(voiceId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void update(final String voiceId, final EnsembleVoice voice) {
        ensembleVoiceService.updateVoice(voiceId, voice);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void delete(final String voiceId) {
        ensembleVoiceService.deleteVoice(voiceId);
    }

    @Override
    public VoiceOptionsResource options(final String voiceId) {
        return resourceContext.getResource(VoiceOptionsResourceImpl.class);
    }
}
