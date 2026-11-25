package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import edu.ucc.farmsim.interfaz.componentes.PresentadorRanuras;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-32: listar las tres ranuras con lo que contienen (RN-31). */
class HU32RanurasListaTest {

    private final MongoEnMemoria mongo = new MongoEnMemoria();

    private AplicacionTest aplicacion() {
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        return aplicacion;
    }

    @Test
    void HU32_ca1_la_lista_muestra_ocupado_y_vacio_ranuras() {
        AplicacionTest aplicacion = aplicacion();
        DatosJuego.fechaFijar(1, Estacion.SUMMER, 5);
        DatosJuego.dineroFijar(aplicacion.estado(), 1250);
        aplicacion.controlador.save(1);
        aplicacion.controlador.save(3);

        List<String> lineas = new PresentadorRanuras(aplicacion.controlador).lineas();

        assertEquals(List.of("Ranura 1: Año 1 · Verano · día 5 · 1.250 m · 2026-11-22 10:30",
                "Ranura 2: Vacía", "Ranura 3: Año 1 · Verano · día 5 · 1.250 m · 2026-11-22 10:30"), lineas);
    }

    @Test
    void HU32_ca2_aDamagedSlotIsListedAsSuchAndTheRestStillWorks() {
        AplicacionTest aplicacion = aplicacion();
        aplicacion.controlador.save(1);
        aplicacion.controlador.save(3);
        mongo.manipular(1, d -> new DocumentoGuardado(d.id(), d.versionEsquema(), d.archivadoEn(), "{truncated", d.checksum()));

        List<VistaRanuraGuardado> ranuras = aplicacion.controlador.guardarRanuras().value();

        assertEquals("DAMAGED", ranuras.get(0).status());
        assertEquals("EMPTY", ranuras.get(1).status());
        assertEquals("OK", ranuras.get(2).status());
        assertEquals("Ranura 1: Dañada", PresentadorRanuras.linea(ranuras.get(0)));
    }

    @Test
    void HU32_ca3_guardando_en_un_vacio_ranura_muestra_su_datos_sin_reiniciando() {
        AplicacionTest aplicacion = aplicacion();
        PresentadorRanuras presentador = new PresentadorRanuras(aplicacion.controlador);
        assertEquals("Ranura 2: Vacía", presentador.lineas().get(1));

        aplicacion.controlador.save(2);
        presentador.refrescar();

        assertEquals("Ranura 2: Año 1 · Primavera · día 1 · 500 m · 2026-11-22 10:30", presentador.lineas().get(1));
    }

    @Test
    void HU32_whenTheStorageFailsTheListSaysSo() {
        AplicacionTest aplicacion = aplicacion();
        mongo.abajoIr();

        var resultado = aplicacion.controlador.guardarRanuras();

        assertEquals(CodigoError.ERROR_ES, resultado.codigo());
    }
}
