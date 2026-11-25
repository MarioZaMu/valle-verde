package edu.ucc.farmsim.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.ConfiguracionTestSinMongo;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.RepositorioGuardadoFalso;
import edu.ucc.farmsim.configuracion.AplicacionServidor;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * RNF-15 y AR-12: una prueba de integración por endpoint con el estado HTTP del éxito y de cada rechazo, contra el
 * contexto real de Spring (sin MongoDB).
 */
@SpringBootTest(classes = AplicacionServidor.class)
@AutoConfigureMockMvc
@ActiveProfiles("no-mongo")
@Import(ConfiguracionTestSinMongo.class)
class ControladorRestJuegoTest {

    private static final String PARCELA = "/api/farm/plots/" + DatosJuego.COL_TIERRA + "/" + DatosJuego.FILA_TIERRA;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private SesionJuego sesion;
    @Autowired
    private RepositorioGuardadoFalso guardados;

    @BeforeEach
    void servidorLimpio() {
        RelojJuego.getInstance().reiniciarParaTests();
        sesion.replace(null);
        guardados.reiniciar();
    }

    private ResultActions juegoNuevo() throws Exception {
        return mvc.perform(post("/api/game").contentType(MediaType.APPLICATION_JSON).content("{\"semilla\": 2026}"));
    }

    private ResultActions sembrar(String idSemilla) throws Exception {
        return mvc.perform(post(PARCELA + "/crop").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idSemilla\": \"" + idSemilla + "\"}"));
    }

    @Test
    void despachar_juego_crea_el_juego_y_devuelve_su_hud_y_el_juego_iniciado_evento() throws Exception {
        juegoNuevo().andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.dinero").value(500))
                .andExpect(jsonPath("$.datos.dia").value(1))
                .andExpect(jsonPath("$.datos.energiaMaxima").value(100))
                .andExpect(jsonPath("$.eventos", hasSize(1)))
                .andExpect(jsonPath("$.eventos[0].tipo").value("JuegoIniciado"))
                .andExpect(jsonPath("$.eventos[0].datos.hud.dinero").value(500));
    }

    @Test
    void POST_game_withoutSeedIsABadRequest() throws Exception {
        mvc.perform(post("/api/game").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void POST_game_withABodyThatIsNotJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/game").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void GET_game_withoutAGameIsNotFoundWithTheRn35Message() throws Exception {
        mvc.perform(get("/api/game")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SIN_PARTIDA"))
                .andExpect(jsonPath("$.mensaje").value("No hay una partida en curso."));
    }

    @Test
    void obtener_juego_devuelve_el_hud_de_el_juego_en_progreso() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/game")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.dinero").value(500))
                .andExpect(jsonPath("$.eventos", hasSize(0)));
    }

    @Test
    void despachar_dias_termina_el_dia_y_devuelve_el_dia_terminado_evento() throws Exception {
        juegoNuevo();
        mvc.perform(post("/api/game/days")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.dia").value(2))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'DiaTerminado')]").isNotEmpty());
    }

    @Test
    void POST_days_withoutAGameIsNotFound() throws Exception {
        mvc.perform(post("/api/game/days")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SIN_PARTIDA"));
    }

    @Test
    void obtener_mapa_devuelve_los_parcelas() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/farm/map")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.cols").value(48))
                .andExpect(jsonPath("$.datos.filas").value(24))
                .andExpect(jsonPath("$.datos.parcelas", hasSize(48 * 24)))
                .andExpect(jsonPath("$.datos.casas", hasSize(5)))
                .andExpect(jsonPath("$.datos.casas[1].id").value("SHOP"));
    }

    @Test
    void HU45_POST_moves_stepsTheAvatarAndABlockedStepIsAConflict() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/farm/map")).andExpect(jsonPath("$.datos.avatar.mirada").value("SOUTH"));
        mvc.perform(post("/api/avatar/moves").contentType(MediaType.APPLICATION_JSON).content("{\"direccion\": \"EAST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.col").value(8))
                .andExpect(jsonPath("$.datos.mirada").value("EAST"))
                .andExpect(jsonPath("$.eventos[0].tipo").value("AvatarMovido"));
        for (String paso : new String[] {"SOUTH", "WEST"}) {
            mvc.perform(post("/api/avatar/moves").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"direccion\": \"" + paso + "\"}")).andExpect(status().isOk());
        }
        mvc.perform(post("/api/avatar/moves").contentType(MediaType.APPLICATION_JSON).content("{\"direccion\": \"SOUTH\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MOVIMIENTO_BLOQUEADO"))
                .andExpect(jsonPath("$.mensaje").value("No puedes pasar por ahí."));
        mvc.perform(post("/api/avatar/moves").contentType(MediaType.APPLICATION_JSON).content("{\"direccion\": \"UP\"}"))
                .andExpect(status().isBadRequest());
        sesion.replace(null);
        mvc.perform(post("/api/avatar/moves").contentType(MediaType.APPLICATION_JSON).content("{\"direccion\": \"EAST\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void HU49_obtener_tutorial_sigue_los_pasos_y_despachar_saltar_termina_eso() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/tutorial")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.paso").value("WALK"))
                .andExpect(jsonPath("$.datos.numero").value(1))
                .andExpect(jsonPath("$.datos.total").value(5))
                .andExpect(jsonPath("$.datos.concluido").value(false));
        mvc.perform(post("/api/avatar/moves").contentType(MediaType.APPLICATION_JSON).content("{\"direccion\": \"EAST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos[1].tipo").value("TutorialAvanzado"))
                .andExpect(jsonPath("$.eventos[1].datos.tutorial.paso").value("SOW"));
        mvc.perform(post("/api/tutorial/skip")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.paso").value("SKIPPED"))
                .andExpect(jsonPath("$.datos.concluido").value(true))
                .andExpect(jsonPath("$.eventos", hasSize(1)));
        mvc.perform(post("/api/tutorial/skip")).andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos", hasSize(0)));
        sesion.replace(null);
        mvc.perform(get("/api/tutorial")).andExpect(status().isNotFound());
        mvc.perform(post("/api/tutorial/skip")).andExpect(status().isNotFound());
    }

    @Test
    void HU46_GET_npcs_and_POST_talk_answerByDistanceAndHU47_POST_errand_rejectsAnIncompleteErrand() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/npcs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos", hasSize(4)))
                .andExpect(jsonPath("$.datos[0].name").value("Marta"))
                .andExpect(jsonPath("$.datos[0].encargoHecho").value(false));
        mvc.perform(post("/api/npcs/MARTA/talk")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NPC_LEJOS"))
                .andExpect(jsonPath("$.mensaje").value("Acércate a Marta para hablar."));
        sesion.actual().avatar().restaurarDesde(new edu.ucc.farmsim.dominio.granja.InstantaneaAvatar(28, 11,
                edu.ucc.farmsim.dominio.granja.Direccion.EAST));
        mvc.perform(post("/api/npcs/MARTA/talk")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.name").value("Marta"))
                .andExpect(jsonPath("$.eventos", hasSize(0)));
        mvc.perform(post("/api/npcs/MARTA/errand")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ENCARGO_INCOMPLETO"));
        sesion.actual().inventario().add(TipoArticulo.CARROT, 5);
        mvc.perform(post("/api/npcs/MARTA/errand")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.encargoHecho").value(true))
                .andExpect(jsonPath("$.eventos[2].tipo").value("EncargoCompletado"));
        mvc.perform(post("/api/npcs/NOBODY/talk")).andExpect(status().isInternalServerError());
        sesion.replace(null);
        mvc.perform(get("/api/npcs")).andExpect(status().isNotFound());
    }

    @Test
    void GET_plot_describesAPlotWithoutChangingIt() throws Exception {
        juegoNuevo();
        mvc.perform(get(PARCELA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.terreno").value("SOIL"))
                .andExpect(jsonPath("$.eventos", hasSize(0)));
    }

    @Test
    void despachar_cultivo_siembra_y_devuelve_creado_con_los_eventos_de_el_orden() throws Exception {
        juegoNuevo();
        sembrar("CARROT_SEED").andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.cultivo").value("CARROT"))
                .andExpect(jsonPath("$.datos.etapa").value("SEED"))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'MapaCambiado')]").isNotEmpty())
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'EnergiaCambiada')].datos.energia").value(98));
    }

    @Test
    void POST_crop_outOfSeasonIsAConflictWithTheRn35Code() throws Exception {
        juegoNuevo();
        sembrar("PUMPKIN_SEED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_ESTACION"));
    }

    @Test
    void POST_crop_inAnOccupiedPlotIsAConflict() throws Exception {
        juegoNuevo();
        sembrar("CARROT_SEED");
        sembrar("CARROT_SEED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PARCELA_OCUPADA"));
    }

    @Test
    void despachar_cultivo_con_un_desconocido_semilla_es_un_interno_error() throws Exception {
        juegoNuevo();
        sembrar("NOT_A_SEED").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"));
    }

    @Test
    void POST_crop_withoutSeedIdIsABadRequest() throws Exception {
        juegoNuevo();
        mvc.perform(post(PARCELA + "/crop").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_watering_watersTheCropAndAGainTheSameDayIsAConflict() throws Exception {
        juegoNuevo();
        sembrar("CARROT_SEED");
        mvc.perform(post(PARCELA + "/watering")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.regado").value(true));
        mvc.perform(post(PARCELA + "/watering")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("YA_REGADO"));
    }

    @Test
    void despachar_riego_un_vacio_parcela_es_no_encontrado() throws Exception {
        juegoNuevo();
        mvc.perform(post(PARCELA + "/watering")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SIN_CULTIVO"));
    }

    @Test
    void POST_harvest_aCropThatIsNotMatureIsAConflict() throws Exception {
        juegoNuevo();
        sembrar("CARROT_SEED");
        mvc.perform(post(PARCELA + "/harvest")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NO_MADURO"));
    }

    @Test
    void despachar_cosechar_un_vacio_parcela_es_no_encontrado() throws Exception {
        juegoNuevo();
        mvc.perform(post(PARCELA + "/harvest")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SIN_CULTIVO"));
    }

    @Test
    void GET_savesAny_isFalseWhenNoSlotHoldsAGameAndTrueWhenOneDoes() throws Exception {
        mvc.perform(get("/api/saves/any")).andExpect(status().isOk()).andExpect(jsonPath("$.datos").value(false));
        guardados.conRanura(2);
        mvc.perform(get("/api/saves/any")).andExpect(status().isOk()).andExpect(jsonPath("$.datos").value(true));
    }

    @Test
    void obtener_guardados_cualquier_cuando_el_almacenamiento_es_abajo_es_servicio_no_disponible_y_jugando_todavia_funciona() throws Exception {
        guardados.inalcanzable();
        mvc.perform(get("/api/saves/any")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("ERROR_ES"))
                .andExpect(jsonPath("$.mensaje").value("No se pudo acceder a las partidas guardadas: MongoDB no respondió."));
        juegoNuevo().andExpect(status().isCreated());
    }

    @Test
    void anUnknownRouteIsNotFoundAndAWrongVerbIsRejectedWithTheSameBody() throws Exception {
        mvc.perform(get("/api/nothing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        mvc.perform(get("/api/game/days")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void aPlotCoordinateThatIsNotANumberIsABadRequest() throws Exception {
        mvc.perform(get("/api/farm/plots/x/1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    private ResultActions comerciar(String trayecto, String idArticulo, int cantidad) throws Exception {
        return mvc.perform(post("/api/market/" + trayecto).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idArticulo\": \"" + idArticulo + "\", \"cantidad\": " + cantidad + "}"));
    }

    @Test
    void obtener_mercado_lista_que_puede_ser_vendido_con_su_tendencia_y_que_puede_ser_compro() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/market")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.vende", hasSize(7)))
                .andExpect(jsonPath("$.datos.vende[?(@.idArticulo == 'CARROT')].precioUnidad").value(27))
                .andExpect(jsonPath("$.datos.vende[?(@.idArticulo == 'CARROT')].tendencia").value("SAME"))
                .andExpect(jsonPath("$.datos.compra", hasSize(7)));
    }

    @Test
    void GET_market_withoutAGameIsNotFound() throws Exception {
        mvc.perform(get("/api/market")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SIN_PARTIDA"));
    }

    @Test
    void despachar_compras_compra_y_reportes_el_dinero_y_inventario_eventos() throws Exception {
        juegoNuevo();
        comerciar("purchases", "CORN_SEED", 5).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos").value(150))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'DineroCambiado')].datos.dinero").value(350))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'InventarioCambiado')]").isNotEmpty());
    }

    @Test
    void POST_purchases_withoutEnoughMoneyIsAConflict() throws Exception {
        juegoNuevo();
        comerciar("purchases", "MEDICINE", 99).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_FONDOS"));
    }

    @Test
    void POST_purchases_withAQuantityOutOfRangeIsABadRequest() throws Exception {
        juegoNuevo();
        comerciar("purchases", "HAY", 100).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CANTIDAD_INVALIDA"));
    }

    @Test
    void despachar_ventas_vende_en_el_dinamico_precio_y_cotiza_eso_antes() throws Exception {
        juegoNuevo();
        sesion.actual().inventario().add(TipoArticulo.CARROT, 3);

        mvc.perform(get("/api/market/quotes").param("idArticulo", "CARROT").param("cantidad", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos").value(79));
        comerciar("sales", "CARROT", 3).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos").value(79))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'DineroCambiado')].datos.dinero").value(579));
    }

    @Test
    void POST_sales_moreThanOwnedIsABadRequestWithTheRange() throws Exception {
        juegoNuevo();
        sesion.actual().inventario().add(TipoArticulo.CARROT, 2);

        comerciar("sales", "CARROT", 5).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CANTIDAD_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").value("La cantidad debe estar entre 1 y 2."));
    }

    @Test
    void POST_sales_withoutBodyFieldsIsABadRequest() throws Exception {
        juegoNuevo();
        mvc.perform(post("/api/market/sales").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void obtener_inventario_devuelve_los_ranuras_de_la_grilla() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/inventory")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.capacidad").value(24))
                .andExpect(jsonPath("$.datos.ranuras", hasSize(2)))
                .andExpect(jsonPath("$.datos.ranuras[0].idArticulo").value("CARROT_SEED"))
                .andExpect(jsonPath("$.datos.ranuras[0].monto").value(10));
    }

    private ResultActions construir(String idEdificio, int col, int fila) throws Exception {
        return mvc.perform(post("/api/buildings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idEdificio\": \"" + idEdificio + "\", \"col\": " + col + ", \"fila\": " + fila + "}"));
    }

    private ResultActions animalComprar(String animalId) throws Exception {
        return mvc.perform(post("/api/animals").contentType(MediaType.APPLICATION_JSON)
                .content("{\"animalId\": \"" + animalId + "\"}"));
    }

    @Test
    void despachar_edificios_construye_y_devuelve_creado_con_los_eventos_de_el_orden() throws Exception {
        juegoNuevo();
        construir("COOP", 2, 7).andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.name").value("Gallinero"))
                .andExpect(jsonPath("$.datos.capacidad").value(4))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'DineroCambiado')].datos.dinero").value(org.hamcrest.Matchers.contains(100, 200)))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'LogroDesbloqueado')].datos.codigo").value("LG-06"))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'MapaCambiado')]").isNotEmpty());
    }

    @Test
    void POST_buildings_rejectionsKeepTheirRn35CodesAndStatus() throws Exception {
        juegoNuevo();
        construir("COOP", 3, 2).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("UBICACION_INVALIDA"));
        construir("BARN", 2, 7).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EDIFICIO_BLOQUEADO"))
                .andExpect(jsonPath("$.mensaje").value("Edificio bloqueado: requiere el logro «Manos a la tierra»."));
        construir("COOP", 2, 7);
        construir("COOP", 8, 7).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_FONDOS"));
    }

    @Test
    void POST_buildings_withoutBodyFieldsIsABadRequestAndWithoutAGameIsNotFound() throws Exception {
        mvc.perform(post("/api/buildings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        construir("COOP", 2, 7).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("SIN_PARTIDA"));
    }

    @Test
    void obtener_edificios_lista_que_es_construido_y_el_catalogo() throws Exception {
        juegoNuevo();
        sesion.actual().granja().colocar(edu.ucc.farmsim.dominio.edificio.TipoEdificio.COOP, 2, 7);
        mvc.perform(get("/api/buildings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.edificios", hasSize(1)))
                .andExpect(jsonPath("$.datos.catalogo", hasSize(4)))
                .andExpect(jsonPath("$.datos.catalogo[1].desbloqueado").value(false))
                .andExpect(jsonPath("$.datos.catalogo[1].requisito").value("Manos a la tierra"));
    }

    @Test
    void POST_animals_buysAHenAndReturnsTheBuildingThatHousesIt() throws Exception {
        juegoNuevo();
        construir("COOP", 2, 7);
        sesion.actual().ganar(2000);
        animalComprar("HEN").andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.animales", hasSize(1)))
                .andExpect(jsonPath("$.datos.animales[0].condicion").value("HEALTHY"));
        animalComprar("COW").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_ESPACIO_ANIMALES"));
    }

    @Test
    void POST_feeding_collection_and_treatment_workOnTheBuildingAndItsAnimals() throws Exception {
        juegoNuevo();
        construir("COOP", 2, 7);
        var gallinero = sesion.actual().granja().edificios().get(0);
        gallinero.animalAgregar(new edu.ucc.farmsim.dominio.animal.Animal(edu.ucc.farmsim.dominio.animal.TipoAnimal.HEN));
        gallinero.animalAgregar(edu.ucc.farmsim.dominio.animal.Animal.desdeInstantanea(new edu.ucc.farmsim.dominio.animal.InstantaneaAnimal(
                edu.ucc.farmsim.dominio.animal.TipoAnimal.HEN, edu.ucc.farmsim.dominio.animal.CondicionAnimal.SICK, 3, false, 0)));
        sesion.actual().inventario().add(TipoArticulo.HAY, 2);
        sesion.actual().inventario().add(TipoArticulo.MEDICINE, 1);

        mvc.perform(post("/api/buildings/0/feeding")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.monto").value(1))
                .andExpect(jsonPath("$.datos.notificacion").value("Hay animales enfermos: cúralos con Medicina."));
        mvc.perform(post("/api/buildings/0/animals/1/treatment")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.edificio.animales[1].condicion").value("HEALTHY"));
        mvc.perform(post("/api/buildings/0/collection")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.monto").value(0))
                .andExpect(jsonPath("$.datos.notificacion").value("No hay productos que recoger."));
    }

    @Test
    void despachar_alimentacion_rechazos_son_conflictos_y_un_desconocido_edificio_es_un_interno_error() throws Exception {
        juegoNuevo();
        construir("COOP", 2, 7);
        sesion.actual().granja().edificios().get(0).animalAgregar(
                new edu.ucc.farmsim.dominio.animal.Animal(edu.ucc.farmsim.dominio.animal.TipoAnimal.HEN));

        mvc.perform(post("/api/buildings/0/feeding")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_HENO"));
        mvc.perform(post("/api/buildings/9/feeding")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"));
        mvc.perform(post("/api/buildings/0/animals/0/treatment")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.notificacion").value("Este animal no está enfermo."));
    }

    @Test
    void despachar_dias_tambien_devuelve_el_dia_resumen_como_un_evento() throws Exception {
        juegoNuevo();
        mvc.perform(post("/api/game/days")).andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'ResumenDiaListo')].datos.resumen.luces[0]")
                        .value("Sin novedades"));
    }

    @Test
    void obtener_mercado_incluye_los_animales_para_venta() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/market")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.animales", hasSize(2)))
                .andExpect(jsonPath("$.datos.animales[1].name").value("Vaca"))
                .andExpect(jsonPath("$.datos.animales[1].precio").value(800));
    }

    @Test
    void obtener_logros_lista_el_nueve_con_su_progreso() throws Exception {
        juegoNuevo();
        mvc.perform(get("/api/achievements")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos", hasSize(9)))
                .andExpect(jsonPath("$.datos[0].codigo").value("LG-01"))
                .andExpect(jsonPath("$.datos[0].desbloqueado").value(false))
                .andExpect(jsonPath("$.datos[1].objetivo").value(25));
    }

    @Test
    void poner_guardados_guardados_y_obtener_guardados_lista_los_ranuras_y_despachar_cargar_carga_eso() throws Exception {
        juegoNuevo();
        mvc.perform(put("/api/saves/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.ranura").value(2))
                .andExpect(jsonPath("$.datos.status").value("OK"));
        mvc.perform(post("/api/game/days"));
        mvc.perform(get("/api/saves")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos", hasSize(3)))
                .andExpect(jsonPath("$.datos[0].status").value("EMPTY"))
                .andExpect(jsonPath("$.datos[1].status").value("OK"));
        mvc.perform(post("/api/saves/2/load")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.dia").value(1))
                .andExpect(jsonPath("$.eventos[?(@.tipo == 'JuegoIniciado')]").isNotEmpty());
    }

    @Test
    void POST_load_ofAnEmptyDamagedOrUnreachableSlotKeepsItsRn35CodeAndStatus() throws Exception {
        juegoNuevo();
        mvc.perform(post("/api/saves/3/load")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_INEXISTENTE"))
                .andExpect(jsonPath("$.mensaje").value("No existe una partida en la ranura 3."));
        guardados.conRanuraDanada(1);
        mvc.perform(post("/api/saves/1/load")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_CORRUPTO"));
        guardados.inalcanzable();
        mvc.perform(put("/api/saves/1")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("ERROR_ES"));
        mvc.perform(get("/api/saves")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void PUT_saves_withoutAGameIsNotFoundAndASlotOutOfRangeIsAnInternalError() throws Exception {
        mvc.perform(put("/api/saves/1")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SIN_PARTIDA"));
        juegoNuevo();
        mvc.perform(put("/api/saves/7")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"));
    }
}
