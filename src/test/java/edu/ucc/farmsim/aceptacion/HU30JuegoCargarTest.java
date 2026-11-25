package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.aplicacion.servicio.SesionJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.JuegoRico;
import edu.ucc.farmsim.apoyo.MongoEnMemoria;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-30: cargar una partida guardada y recuperarla exactamente (RN-32, RN-33). */
class HU30JuegoCargarTest {

    @Test
    void HU30_ca1_el_cargado_estado_es_estructuralmente_identico_a_el_archivado_uno_critico_test() {
                // Dada una partida rica guardada en la ranura 1
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = JuegoRico.jugar();
        AplicacionTest almacenar = AplicacionTest.create(aplicacion.sesion, SecuenciaDia.estandar(), mongo.repositorio());
        MementoJuego archivado = aplicacion.estado().mementoCrear();
        assertTrue(almacenar.controlador.save(1).isOk());

                // Cuando hay otra partida en curso y se carga la ranura
        almacenar.controlador.juegoNuevo(99L);
        Resultado<VistaHud> resultado = almacenar.controlador.cargar(1);

                // Entonces calendario, energía, dinero, inventario, cultivos, edificios, animales, mercado, clima, logros y
        // contadores, y el generador, son idénticos
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(archivado, almacenar.estado().mementoCrear());
    }

    @Test
    void HU30_ca2_el_cargado_juego_dibuja_el_mismo_clima_como_el_original() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest original = JuegoRico.jugar();
        AplicacionTest almacenar = AplicacionTest.create(original.sesion, SecuenciaDia.estandar(), mongo.repositorio());
        almacenar.controlador.save(2);
        MementoJuego archivado = original.estado().mementoCrear();

        List<String> climaOriginal = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            climaOriginal.add(original.controlador.diaFin().value().clima());
        }
        AplicacionTest cargado = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        assertTrue(cargado.controlador.cargar(2).isOk());
        assertEquals(archivado, cargado.estado().mementoCrear());
        List<String> climaCargado = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            climaCargado.add(cargado.controlador.diaFin().value().clima());
        }

        assertEquals(climaOriginal, climaCargado);
    }

    @Test
    void HU30_ca3_cargando_reemplaza_el_juego_en_progreso_y_el_hud_muestra_el_cargado_uno() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest aplicacion = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        aplicacion.controlador.juegoNuevo(1L);
        aplicacion.controlador.diaFin();
        aplicacion.controlador.diaFin();
        aplicacion.controlador.save(1);
        aplicacion.controlador.juegoNuevo(2L);
        aplicacion.eventos.clear();

        Resultado<VistaHud> resultado = aplicacion.controlador.cargar(1);

        assertEquals(3, resultado.value().dia());
        assertEquals(3, aplicacion.controlador.hud().value().dia());
        assertTrue(aplicacion.eventos.contains(new EventoJuego.JuegoIniciado(resultado.value())));
        assertTrue(aplicacion.eventos.contains(new EventoJuego.MapaCambiado()));
        assertTrue(aplicacion.eventos.contains(new EventoJuego.InventarioCambiado()));
    }

    @Test
    void HU30_loadingWorksWithoutAGameInProgress() {
        MongoEnMemoria mongo = new MongoEnMemoria();
        AplicacionTest first = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());
        first.controlador.juegoNuevo(1L);
        first.controlador.save(3);
        AplicacionTest segundo = AplicacionTest.create(new SesionJuego(), SecuenciaDia.estandar(), mongo.repositorio());

        assertTrue(segundo.controlador.cargar(3).isOk());
        assertTrue(segundo.controlador.tieneJuegoEnProgreso());
    }
}
