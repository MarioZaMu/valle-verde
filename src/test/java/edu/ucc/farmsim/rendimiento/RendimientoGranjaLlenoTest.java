package edu.ucc.farmsim.rendimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.infraestructura.persistencia.CodecGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Rendimiento a plena carga (RNF-01, RNF-03): una granja con 40 cultivos a la intemperie, 9 en el invernadero, seis
 * edificios y 16 animales.
 */
class RendimientoGranjaLlenoTest {

    private static final int DIAS = 100;
    private static final int MS_LIMITE_P95 = 200;
    private static final int MS_LIMITE_GUARDADO = 1000;
    private static final int BYTES_LIMITE_DOCUMENTO = 200 * 1024;

    /** Una granja con todas las parcelas de tierra sembradas, seis edificios y dieciséis animales. */
    static AplicacionTest granjaLlena(MongoEnMemoria mongo) {
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        EstadoJuego estado = aplicacion.estado();
        for (int col = ReglasJuego.COL_TIERRA; col < ReglasJuego.COL_TIERRA + ReglasJuego.ANCHO_TIERRA; col++) {
            for (int fila = ReglasJuego.FILA_TIERRA; fila < ReglasJuego.FILA_TIERRA + ReglasJuego.ALTO_TIERRA; fila++) {
                DatosJuego.plantar(estado, col, fila, TipoCultivo.CORN, 3, 1, true, false);
            }
        }
        List<Edificio> hogaresAnimal = new ArrayList<>();
        hogaresAnimal.add(DatosJuego.construir(estado, TipoEdificio.COOP, 2, 7));
        hogaresAnimal.add(DatosJuego.construir(estado, TipoEdificio.COOP, 5, 7));
        hogaresAnimal.add(DatosJuego.construir(estado, TipoEdificio.BARN, 8, 7));
        hogaresAnimal.add(DatosJuego.construir(estado, TipoEdificio.BARN, 11, 7));
        DatosJuego.construir(estado, TipoEdificio.WAREHOUSE, 2, 5);
        DatosJuego.construir(estado, TipoEdificio.GREENHOUSE, 12, 2);
        for (int col = 12; col < 15; col++) {
            for (int fila = 2; fila < 5; fila++) {
                DatosJuego.plantar(estado, col, fila, TipoCultivo.PUMPKIN, 3, 1, true, false);
            }
        }
        estado.inventario().ranurasAgregar(12);
        for (Edificio hogar : hogaresAnimal) {
            for (int i = 0; i < ReglasJuego.ANIMALES_POR_EDIFICIO; i++) {
                DatosJuego.casa(hogar, hogar.especie(), i == 3 ? CondicionAnimal.SICK : CondicionAnimal.HEALTHY,
                        i == 3 ? 3 : 0, i % 2 == 0, 1);
            }
        }
        for (TipoArticulo articulo : TipoArticulo.values()) {
            estado.inventario().add(articulo, 10);
        }
        DatosJuego.dineroFijar(estado, 2500);
        return aplicacion;
    }

    @Test
    void HU10_ca3_endingTheDayOnAFullFarmTakesAtMost200msAtThe95thPercentile() {
        AplicacionTest aplicacion = granjaLlena(new MongoEnMemoria());
        for (int i = 0; i < 10; i++) { // warm-up
            aplicacion.controlador.diaFin();
        }
        List<Long> millis = new ArrayList<>();
        for (int i = 0; i < DIAS; i++) {
            long start = System.nanoTime();
            assertTrue(aplicacion.controlador.diaFin().isOk());
            millis.add((System.nanoTime() - start) / 1_000_000);
        }
        Collections.sort(millis);
        long p95 = millis.get((int) Math.ceil(DIAS * 0.95) - 1);

        assertTrue(p95 <= MS_LIMITE_P95, "p95 was " + p95 + " ms");
    }

    @Test
    void HU29_ca4_aFullFarmIsSavedInAtMostOneSecondInAtMost200Kb() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = granjaLlena(mongo);
        assertEquals(6, aplicacion.estado().granja().edificios().size());
        assertEquals(16, aplicacion.estado().granja().edificios().stream().mapToInt(b -> b.animales().size()).sum());

        long start = System.nanoTime();
        assertTrue(aplicacion.controlador.guardar(1).isOk());
        long millis = (System.nanoTime() - start) / 1_000_000;

        int bytes = mongo.documento(1).estado().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        assertTrue(millis <= MS_LIMITE_GUARDADO, "saving took " + millis + " ms");
        assertTrue(bytes <= BYTES_LIMITE_DOCUMENTO, "the document weighs " + bytes + " bytes");
    }

    @Test
    void HU30_ca4_aFullFarmIsLoadedInAtMostOneSecondAndIsIdentical() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = granjaLlena(mongo);
        MementoJuego archivado = aplicacion.estado().mementoCrear();
        aplicacion.controlador.guardar(2);
        AplicacionTest otro = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());

        long start = System.nanoTime();
        assertTrue(otro.controlador.cargar(2).isOk());
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(millis <= MS_LIMITE_GUARDADO, "loading took " + millis + " ms");
        assertEquals(archivado, otro.estado().mementoCrear());
    }

    @Test
    void theCodecRoundTripOfAFullFarmKeepsEveryPart() {
        AplicacionTest aplicacion = granjaLlena(new MongoEnMemoria());
        MementoJuego memento = aplicacion.estado().mementoCrear();
        CodecGuardado codec = new CodecGuardado();

        DocumentoGuardado documento = codec.codificar(1, memento, Instant.EPOCH);

        assertEquals(memento, codec.decodificar(documento));
    }
}
