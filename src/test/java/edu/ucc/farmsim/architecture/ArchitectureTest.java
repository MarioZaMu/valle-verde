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
 * Reglas de arquitectura AR-1 a AR-5, AR-7 y AR-11 (véase docs/ARQUITECTURA.md sección 1).
 *
 * <p>Cada regla tolera una selección vacía ({@code allowEmptyShould(true)}) porque las capas se llenan fase por fase; a
 * medida que aparecen clases, las reglas empiezan a comprobarlas automáticamente.
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

    /** Main, su analizador de argumentos y la configuración de Spring forman la raíz de composición. */
    private static final DescribedPredicate<JavaClass> COMPOSITION =
            JavaClass.Predicates.equivalentTo(Main.class)
                    .or(JavaClass.Predicates.equivalentTo(LaunchOptions.class))
                    .or(JavaClass.Predicates.resideInAPackage(CONFIG))
                    .as("Main, LaunchOptions and the config package");

    /**
     * AR-1: servidor api a application a domain, infrastructure a domain; cliente ui y client a application; solo la
     * raíz de composición puede ver todas las capas.
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

    /** AR-2: el dominio no sabe nada de Swing, AWT, Spring, Jackson, MongoDB, archivos ni la red. */
    @ArchTest
    static final ArchRule AR2_domainHasNoFrameworkOrIoImports = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "javax.swing..", "java.awt..", "org.springframework..", "com.fasterxml.jackson..",
                    "com.mongodb..", "org.bson..", "java.io..", "java.nio.file..", "java.net..")
            .allowEmptyShould(true);

    /** AR-3: la UI dibuja desde los DTO de application; nunca importa domain, infrastructure, api ni client. */
    @ArchTest
    static final ArchRule AR3_uiDoesNotImportDomainInfrastructureApiOrClient = noClasses()
            .that().resideInAPackage(UI)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRASTRUCTURE, API, CLIENT)
            .allowEmptyShould(true);

    /** AR-4: el controlador solo orquesta; nunca llega a los repositorios ni a la infraestructura. */
    @ArchTest
    static final ArchRule AR4_controllerNeverTouchesRepositories = noClasses()
            .that().resideInAPackage(ROOT + ".application.controller..")
            .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE, ROOT + ".domain.save..")
            .allowEmptyShould(true);

    /** AR-5: sin ciclos de dependencia entre los subpaquetes del dominio. */
    @ArchTest
    static final ArchRule AR5_domainPackagesHaveNoCycles = slices()
            .matching(ROOT + ".domain.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    /** AR-7: el azar siempre se inyecta; solo Main (y el propio StateRandom) pueden crear generadores. */
    @ArchTest
    static final ArchRule AR7_noUnseededRandomnessOutsideMain = noClasses()
            .that().doNotHaveFullyQualifiedName(ROOT + ".Main")
            .and().doNotHaveFullyQualifiedName(ROOT + ".domain.time.StateRandom")
            .should().callConstructor(java.util.Random.class)
            .orShould().callMethod(Math.class, "random")
            .allowEmptyShould(true);

    /** AR-11: los controladores REST solo traducen HTTP; nunca importan domain ni infrastructure. */
    @ArchTest
    static final ArchRule AR11_apiDoesNotImportDomainOrInfrastructure = noClasses()
            .that().resideInAPackage(API)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRASTRUCTURE, UI, CLIENT)
            .allowEmptyShould(true);

    /** AR-11: el cliente nunca importa domain, infrastructure ni la api del servidor. */
    @ArchTest
    static final ArchRule AR11_clientDoesNotImportServerSide = noClasses()
            .that().resideInAPackage(CLIENT)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRASTRUCTURE, API, UI)
            .allowEmptyShould(true);
}
