package de.halbmann.sam;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "de.halbmann.sam")
class ArchitectureTest {

    /**
     * The business layer (services, repositories, entities) must not depend on JAX-RS.
     *
     * <p>JAX-RS types ({@code jakarta.ws.rs.*}) such as {@code Response}, {@code StreamingOutput},
     * etc. belong exclusively to the REST boundary ({@code api.impl.*}). Business logic must use
     * plain Java or framework-agnostic abstractions so it stays testable and portable.
     */
    @ArchTest
    static final ArchRule businessLayerMustNotUseJaxRs = noClasses()
            .that()
            .resideInAPackage("de.halbmann.sam.business..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("jakarta.ws.rs..")
            .because("business logic must not depend on JAX-RS; "
                    + "use framework-agnostic abstractions (e.g. StreamWriter) instead");

    /**
     * Repositories must not declare {@code @Transactional} — services own the transaction boundary.
     *
     * <p>Repository-level {@code @Transactional} is redundant when a service wraps the call, and
     * dangerous when a resource ever calls a repository directly (accidental per-call transaction).
     */
    @ArchTest
    static final ArchRule repositoriesMustNotBeTransactional = noClasses()
            .that()
            .resideInAPackage("de.halbmann.sam.business..boundary..")
            .should()
            .beAnnotatedWith("jakarta.transaction.Transactional")
            .because("transaction ownership belongs to the service layer, not repositories");

    @ArchTest
    static final ArchRule repositoryMethodsMustNotBeTransactional = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("de.halbmann.sam.business..boundary..")
            .should()
            .beAnnotatedWith("jakarta.transaction.Transactional")
            .because("transaction ownership belongs to the service layer, not repositories");

    /**
     * Same constraint for the classification and enrichment sub-systems.
     */
    @ArchTest
    static final ArchRule classificationLayerMustNotUseJaxRs = noClasses()
            .that()
            .resideInAPackage("de.halbmann.sam.classification.controller..")
            .or()
            .resideInAPackage("de.halbmann.sam.enrichment.controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("jakarta.ws.rs..")
            .because("service/controller classes must not depend on JAX-RS");
}
