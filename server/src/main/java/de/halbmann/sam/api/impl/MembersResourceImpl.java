package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.MembersResource;
import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.members.controller.MemberService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@Authenticated
@RequestScoped
public class MembersResourceImpl implements MembersResource {

    @Inject
    MemberService memberService;

    @Override
    public PaginatedResponse<Member> findMembers(MemberFilterRequest filterRequest) {
        return memberService.findMembers(filterRequest);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public Member add(Member member) {
        return memberService.addMember(member);
    }

    @Override
    public Member load(String memberId) {
        return memberService.getMember(memberId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void update(String memberId, Member member) {
        memberService.updateMember(memberId, member);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void delete(String memberId) {
        memberService.deleteMember(memberId);
    }

    @Override
    @RolesAllowed(Roles.ADMIN)
    public void linkUser(String memberId, String userId) {
        memberService.linkUser(memberId, userId);
    }

    @Override
    @RolesAllowed(Roles.ADMIN)
    public void unlinkUser(String memberId) {
        memberService.unlinkUser(memberId);
    }
}
