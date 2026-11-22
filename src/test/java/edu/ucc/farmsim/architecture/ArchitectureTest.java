package edu.ucc.farmsim.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import edu.ucc.farmsim.Main;

/**
 * Architecture rules AR-1 to AR-5 and AR-7 (see docs/ARQUITECTURA.md section 1).
 *
 * <p>Every rule tolerates an empty selection ({@code allowEmptyShould(true)}) because the layers are
 * filled phase by phase; as classes appear the rules start checking them automatically.
 */
@AnalyzeClasses(packages = "edu.ucc.farmsim", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "edu.ucc.farmsim";
    private static final String UI = ROOT + ".ui..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String INFRASTRUCTURE = ROOT + ".infrastructure..";

    /** AR-1: ui to application to domain; infrastructure to domain; only Main (composition root) may see every layer. */
    @ArchTest
    static final ArchRule AR1_layersAreRespected = layeredArchitecture()
            .consideringAllDependencies()
            .withOptionalLayers(true)
            .layer("Composition").definedBy(JavaClass.Predicates.equivalentTo(Main.class))
            .layer("UI").definedBy(UI)
            .layer("Application").definedBy(APPLICATION)
            .layer("Domain").definedBy(DOMAIN)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Composition").mayNotBeAccessedByAnyLayer()
            .whereLayer("UI").mayOnlyBeAccessedByLayers("Composition")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("UI", "Composition")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Composition")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Composition")
            .allowEmptyShould(true);

    /** AR-2: the domain knows nothing about Swing, AWT, Gson or file I/O. */
    @ArchTest
    static final ArchRule AR2_domainHasNoFrameworkOrIoImports = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "javax.swing..", "java.awt..", "com.google.gson..", "java.io..", "java.nio.file..")
            .allowEmptyShould(true);

    /** AR-3: the UI draws from application DTOs and never imports domain or infrastructure. */
    @ArchTest
    static final ArchRule AR3_uiDoesNotImportDomainOrInfrastructure = noClasses()
            .that().resideInAPackage(UI)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRASTRUCTURE)
            .allowEmptyShould(true);

    /** AR-4: the controller only orchestrates; it never reaches repositories or infrastructure. */
    @ArchTest
    static final ArchRule AR4_controllerNeverTouchesRepositories = noClasses()
            .that().resideInAPackage(ROOT + ".application.controller..")
            .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE, ROOT + ".domain.save..")
            .allowEmptyShould(true);

    /** AR-5: no dependency cycles between the sub-packages of the domain. */
    @ArchTest
    static final ArchRule AR5_domainPackagesHaveNoCycles = slices()
            .matching(ROOT + ".domain.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    /** AR-7: randomness is always injected; only Main (and StateRandom itself) may create generators. */
    @ArchTest
    static final ArchRule AR7_noUnseededRandomnessOutsideMain = noClasses()
            .that().doNotHaveFullyQualifiedName(ROOT + ".Main")
            .and().doNotHaveFullyQualifiedName(ROOT + ".domain.time.StateRandom")
            .should().callConstructor(java.util.Random.class)
            .orShould().callMethod(Math.class, "random")
            .allowEmptyShould(true);
}
