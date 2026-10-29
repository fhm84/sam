package de.halbmann.sam.business.members.controller;

import de.halbmann.sam.api.entity.members.MembershipStatus;
import de.halbmann.sam.business.members.boundary.MemberRepository;
import de.halbmann.sam.business.members.entity.MemberEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Applies the retention period of member data (GDPR; plan "Person catalogue and member profiles"):
 * former members are erased once they left longer ago than {@code sam.members.retention-months},
 * and superseded audit rows of member profiles older than that are deleted, so old contact details
 * don't pile up. A period of 0 turns it off.
 */
@Slf4j
@ApplicationScoped
public class MemberRetention {

    @Inject
    MemberRepository memberRepository;

    @Inject
    MemberService memberService;

    @Inject
    MemberAuditPurge memberAuditPurge;

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "sam.members.retention-months", defaultValue = "12")
    int retentionMonths;

    /** What one run removed. */
    public record Result(int erasedMembers, int supersededAuditRows) {}

    @Scheduled(cron = "{sam.members.retention-cron}", identity = "member-retention")
    void nightly() {
        Result result = apply(LocalDate.now());
        if (result.erasedMembers() > 0 || result.supersededAuditRows() > 0) {
            log.atInfo().log(
                    "Member retention: erased {} former member(s), deleted {} superseded audit row(s)",
                    result.erasedMembers(),
                    result.supersededAuditRows());
        }
    }

    /** Runs the retention as of {@code today}; public for tests and a manual run. */
    public Result apply(final LocalDate today) {
        if (retentionMonths <= 0) {
            return new Result(0, 0);
        }
        final LocalDate cutoff = today.minusMonths(retentionMonths);
        final List<UUID> expired = QuarkusTransaction.requiringNew()
                .call(() ->
                        memberRepository
                                .list("membershipStatus = ?1 and leftAt < ?2", MembershipStatus.FORMER, cutoff)
                                .stream()
                                .map(MemberEntity::getId)
                                .toList());
        // One transaction per member: each erasure commits (and purges its history) on its own
        expired.forEach(id -> memberService.deleteMember(id.toString()));

        final long cutoffMillis =
                cutoff.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        final int superseded = QuarkusTransaction.requiringNew()
                .call(() -> entityManager
                        .createNativeQuery("delete from members_aud a using REVINFO r"
                                + " where a.REVEND = r.REV and r.REVTSTMP < :cutoff")
                        .setParameter("cutoff", cutoffMillis)
                        .executeUpdate());
        // Catches up on the history of members whose purge after deletion failed
        memberAuditPurge.purgeErasedMembers();
        return new Result(expired.size(), superseded);
    }
}
