package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.MyMemberResource;
import de.halbmann.sam.api.entity.members.OwnMemberData;
import de.halbmann.sam.business.members.controller.MemberService;
import de.halbmann.sam.security.CurrentUserService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@Authenticated
@RequestScoped
public class MyMemberResourceImpl implements MyMemberResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    MemberService memberService;

    @Override
    public OwnMemberData get() {
        return memberService.getOwnData(currentUserService.getUserId());
    }
}
