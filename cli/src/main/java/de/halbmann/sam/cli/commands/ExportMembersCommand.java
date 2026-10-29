package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberFilterRequest;
import de.halbmann.sam.cli.util.FilenameUtils;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/**
 * Exports all members as JSON files readable by {@code importMember}. Environment-specific data
 * (OIDC user ID, invite time) is stripped — it references the source system's logins and would be
 * wrong anywhere else. Ensemble seats are not part of the member DTO and are not exported.
 */
@Unremovable
@Singleton
@CommandLine.Command(
        name = "exportMember",
        description = "Export all members to JSON file(s), one per member",
        mixinStandardHelpOptions = true)
public class ExportMembersCommand extends AbstractExportCommand<Member> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected List<Member> fetchAll() {
        MemberFilterRequest request = new MemberFilterRequest();
        request.setSize(-1);
        return client.members().findMembers(request).getData();
    }

    @Override
    protected String filenameFor(final Member member, final Set<String> usedNames) {
        return FilenameUtils.uniqueFilename(member.getName(), member.getId(), usedNames);
    }

    @Override
    protected String describe(final Member member) {
        return member.getName();
    }

    @Override
    protected String noun() {
        return "member(s)";
    }

    @Override
    protected Object toExport(final Member member) {
        member.setUserId(null);
        member.setLastInviteSentAt(null);
        member.setOnboarding(null);
        return member;
    }
}
