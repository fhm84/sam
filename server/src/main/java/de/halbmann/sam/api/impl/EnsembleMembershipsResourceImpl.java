package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.EnsembleMembershipsResource;
import de.halbmann.sam.api.entity.ensembles.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.ensembles.EnsembleMembership;
import de.halbmann.sam.business.ensembles.controller.EnsembleMembershipService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import java.util.List;

@Authenticated
@RequestScoped
public class EnsembleMembershipsResourceImpl implements EnsembleMembershipsResource {

    @PathParam("ensembleId")
    String ensembleId;

    @Inject
    EnsembleMembershipService membershipService;

    @Override
    public List<EnsembleMembership> list() {
        return membershipService.getMembers(ensembleId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public EnsembleMembership add(final CreateEnsembleMembership membership) {
        return membershipService.addMember(ensembleId, membership);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public EnsembleMembership update(final String memberId, final EnsembleMembership membership) {
        return membershipService.updateMember(memberId, membership);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void delete(final String memberId) {
        membershipService.deleteMember(memberId);
    }
}
