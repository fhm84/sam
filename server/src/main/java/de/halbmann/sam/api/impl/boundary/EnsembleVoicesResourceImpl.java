package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.EnsembleVoicesResource;
import de.halbmann.sam.api.boundary.VoiceOptionsResource;
import de.halbmann.sam.api.entity.CreateEnsembleVoice;
import de.halbmann.sam.api.entity.EnsembleVoice;
import de.halbmann.sam.business.controller.EnsembleVoiceService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.List;

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
    public EnsembleVoice add(final CreateEnsembleVoice voice) {
        return ensembleVoiceService.addVoice(ensembleId, voice);
    }

    @Override
    public EnsembleVoice get(final String voiceId) {
        return ensembleVoiceService.getVoice(voiceId);
    }

    @Override
    public void update(final String voiceId, final EnsembleVoice voice) {
        ensembleVoiceService.updateVoice(voiceId, voice);
    }

    @Override
    public void delete(final String voiceId) {
        ensembleVoiceService.deleteVoice(voiceId);
    }

    @Override
    public VoiceOptionsResource options(final String voiceId) {
        return resourceContext.getResource(VoiceOptionsResourceImpl.class);
    }
}
