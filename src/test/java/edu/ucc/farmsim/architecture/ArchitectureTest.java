package edu.ucc.farmsim.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import edu.ucc.farmsim.LaunchOptions;
import edu.ucc.farmsim.Main;

/**
 * Architecture rules AR-1 to AR-5, AR-7 and AR-11 (see docs/ARQUITECTURA.md section 1).
 *
 * <p>Every rule tolerates an empty selection ({@code allowEmptyShould(true)}) because the layers are
 * filled phase by phase; as classes appear the rules start checking them automatically.
 */
@AnalyzeClasses(packages = "edu.ucc.farmsim", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "edu.ucc.farmsim";
    private static final String UI = ROOT + ".ui..";
    private static final String CLIENT = ROOT + ".client..";
    private static final String API = ROOT + ".api..";
    private static final String CONFIG = ROOT + ".config..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String INFRASTRUCTURE = ROOT + ".infrastructure..";

    /** Main, its argument parser and the Spring configuration form the composition root. */
    private static final DescribedPredicate<JavaClass> COMPOSITION =
            JavaClass.Predicates.equivalentTo(Main.class)
                    .or(JavaClass.Predicates.equivalentTo(LaunchOptions.class))
                    .or(JavaClass.Predicates.resideInAPackage(CONFIG))
                    .as("Main, LaunchOptions and the config package");

    /**
     * AR-1: server api to application to domain, infrastructure to domain; client ui and client to
     * application; only the composition root may see every layer.
     */
    @ArchTest
    static final ArchRule AR1_layersAreRespected = layeredArchitecture()
            .consideringAllDependencies()
            .withOptionalLayers(true)
            .layer("Composition").definedBy(COMPOSITION)
            .layer("UI").definedBy(UI)
            .layer("Client").definedBy(CLIENT)
            .layer("Api").definedBy(API)
            .layer("Application").definedBy(APPLICATION)
            .layer("Domain").definedBy(DOMAIN)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Composition").mayNotBeAccessedByAnyLayer()
            .whereLayer("UI").mayOnlyBeAccessedByLayers("Composition")
            .whereLayer("Client").mayOnlyBeAccessedByLayers("Composition")
            .whereLayer("Api").mayOnlyBeAccessedByLayers("Composition")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("UI", "Client", "Api", "Composition")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Composition")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Composition")
            .allowEmptyShould(true);

    /** AR-2: the domain knows nothing about Swing, AWT, Spring, Jackson, MongoDB, files or the network. */
    @ArchTest
    static final ArchRule AR2_domainHasNoFrameworkOrIoImports = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "javax.swing..", "java.awt..", "org.springframework..", "com.fasterxml.jackson..",
                    "com.mongodb..", "org.bson..", "java.io..", "java.nio.file..", "java.net..")
            .allowEmptyShould(true);

    /** AR-3: the UI draws from application DTOs; it never imports domain, infrastructure, api or client. */
    @ArchTest
    static final ArchRule AR3_uiDoesNotImportDomainInfrastructureApiOrClient = noClasses()
            .that().resideInAPackage(UI)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRASTRUCTURE, API, CLIENT)
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

    /** AR-11: the REST controllers only translate HTTP; they never import domain or infrastructure. */
    @ArchTest
    static final ArchRule AR11_apiDoesNotImportDomainOrInfrastructure = noClasses()
            .that().resideInAPackage(API)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRASTRUCTURE, UI, CLIENT)
            .allowEmptyShould(true);

    /** AR-11: the client never imports domain, infrastructure or the server's api. */
    @ArchTest
    static final ArchRule AR11_clientDoesNotImportServerSide = noClasses()
            .that().resideInAPackage(CLIENT)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRASTRUCTURE, API, UI)
            .allowEmptyShould(true);
}
