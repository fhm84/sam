package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.VoiceOptionsResource;
import de.halbmann.sam.api.entity.CreateVoiceOption;
import de.halbmann.sam.api.entity.VoiceOption;
import de.halbmann.sam.business.controller.VoiceOptionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import java.util.List;

@RequestScoped
public class VoiceOptionsResourceImpl implements VoiceOptionsResource {

    @PathParam("voiceId")
    String voiceId;

    @Inject
    VoiceOptionService voiceOptionService;

    @Override
    public List<VoiceOption> listAll() {
        return voiceOptionService.getOptions(voiceId);
    }

    @Override
    public VoiceOption add(final CreateVoiceOption option) {
        return voiceOptionService.addOption(voiceId, option);
    }

    @Override
    public void update(final String optionId, final VoiceOption option) {
        voiceOptionService.updateOption(optionId, option);
    }

    @Override
    public void delete(final String optionId) {
        voiceOptionService.deleteOption(optionId);
    }
}
