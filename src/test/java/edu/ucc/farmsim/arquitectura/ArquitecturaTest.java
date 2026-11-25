package edu.ucc.farmsim.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import edu.ucc.farmsim.Main;
import edu.ucc.farmsim.OpcionesLanzamiento;

/**
 * Reglas de arquitectura AR-1 a AR-5, AR-7 y AR-11 (véase docs/ARQUITECTURA.md sección 1).
 *
 * <p>Cada regla tolera una selección vacía ({@code allowEmptyShould(true)}) porque las capas se llenan fase por fase; a
 * medida que aparecen clases, las reglas empiezan a comprobarlas automáticamente.
 */
@AnalyzeClasses(packages = "edu.ucc.farmsim", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String RAIZ = "edu.ucc.farmsim";
    private static final String UI = RAIZ + ".ui..";
    private static final String CLIENTE = RAIZ + ".client..";
    private static final String API = RAIZ + ".api..";
    private static final String CONFIGURACION = RAIZ + ".config..";
    private static final String APLICACION = RAIZ + ".application..";
    private static final String DOMINIO = RAIZ + ".domain..";
    private static final String INFRAESTRUCTURA = RAIZ + ".infrastructure..";

    /** Main, su analizador de argumentos y la configuración de Spring forman la raíz de composición. */
    private static final DescribedPredicate<JavaClass> COMPOSICION =
            JavaClass.Predicates.equivalentTo(Main.class)
                    .or(JavaClass.Predicates.equivalentTo(OpcionesLanzamiento.class))
                    .or(JavaClass.Predicates.resideInAPackage(CONFIGURACION))
                    .as("Main, LaunchOptions and the config package");

    /**
     * AR-1: servidor api a application a domain, infrastructure a domain; cliente ui y client a application; solo la
     * raíz de composición puede ver todas las capas.
     */
    @ArchTest
    static final ArchRule AR1_capas_son_respetado = layeredArchitecture()
            .consideringAllDependencies()
            .withOptionalLayers(true)
            .layer("Composition").definedBy(COMPOSICION)
            .layer("UI").definedBy(UI)
            .layer("Client").definedBy(CLIENTE)
            .layer("Api").definedBy(API)
            .layer("Application").definedBy(APLICACION)
            .layer("Domain").definedBy(DOMINIO)
            .layer("Infrastructure").definedBy(INFRAESTRUCTURA)
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
    static final ArchRule AR2_dominio_tiene_no_marco_de_trabajo_o_es_importa = noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "javax.swing..", "java.awt..", "org.springframework..", "com.fasterxml.jackson..",
                    "com.mongodb..", "org.bson..", "java.io..", "java.nio.file..", "java.net..")
            .allowEmptyShould(true);

    /** AR-3: la UI dibuja desde los DTO de application; nunca importa domain, infrastructure, api ni client. */
    @ArchTest
    static final ArchRule AR3_uiDoesNotImportDomainInfrastructureApiOrClient = noClasses()
            .that().resideInAPackage(UI)
            .should().dependOnClassesThat().resideInAnyPackage(DOMINIO, INFRAESTRUCTURA, API, CLIENTE)
            .allowEmptyShould(true);

    /** AR-4: el controlador solo orquesta; nunca llega a los repositorios ni a la infraestructura. */
    @ArchTest
    static final ArchRule AR4_controlador_nunca_toca_repositorios = noClasses()
            .that().resideInAPackage(RAIZ + ".application.controller..")
            .should().dependOnClassesThat().resideInAnyPackage(INFRAESTRUCTURA, RAIZ + ".domain.save..")
            .allowEmptyShould(true);

    /** AR-5: sin ciclos de dependencia entre los subpaquetes del dominio. */
    @ArchTest
    static final ArchRule AR5_dominio_paquetes_tener_no_ciclos = slices()
            .matching(RAIZ + ".domain.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    /** AR-7: el azar siempre se inyecta; solo Main (y el propio StateRandom) pueden crear generadores. */
    @ArchTest
    static final ArchRule AR7_no_sin_semilla_azar_afuera_principal = noClasses()
            .that().doNotHaveFullyQualifiedName(RAIZ + ".Main")
            .and().doNotHaveFullyQualifiedName(RAIZ + ".domain.time.StateRandom")
            .should().callConstructor(java.util.Random.class)
            .orShould().callMethod(Math.class, "random")
            .allowEmptyShould(true);

    /** AR-11: los controladores REST solo traducen HTTP; nunca importan domain ni infrastructure. */
    @ArchTest
    static final ArchRule AR11_api_hace_no_importar_dominio_o_infraestructura = noClasses()
            .that().resideInAPackage(API)
            .should().dependOnClassesThat().resideInAnyPackage(DOMINIO, INFRAESTRUCTURA, UI, CLIENTE)
            .allowEmptyShould(true);

    /** AR-11: el cliente nunca importa domain, infrastructure ni la api del servidor. */
    @ArchTest
    static final ArchRule AR11_cliente_hace_no_importar_servidor_lado = noClasses()
            .that().resideInAPackage(CLIENTE)
            .should().dependOnClassesThat().resideInAnyPackage(DOMINIO, INFRAESTRUCTURA, API, UI)
            .allowEmptyShould(true);
}
