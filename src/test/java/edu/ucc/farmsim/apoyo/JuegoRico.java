package edu.ucc.farmsim.apoyo;

import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.progreso.Logro;

/**
 * Juega unos cuantos días reales para que una partida tenga cultivos, un edificio con animales, movimiento del mercado
 * y logros.
 */
public final class JuegoRico {

    private JuegoRico() {
                // solo métodos de utilidad estáticos
    }

    /** Inicia una partida y la juega hasta un estado en el que cada parte del memento contiene algo. */
    public static AplicacionTest jugar() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.desbloquear(aplicacion.estado(), Logro.FIRST_HARVEST);
        aplicacion.controlador.plantar(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA, "CARROT_SEED");
        aplicacion.controlador.plantar(DatosJuego.COL_TIERRA + 1, DatosJuego.FILA_TIERRA, "POTATO_SEED");
        aplicacion.controlador.agua(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA);
        DatosJuego.plantar(aplicacion.estado(), DatosJuego.COL_TIERRA + 2, DatosJuego.FILA_TIERRA, TipoCultivo.CARROT, 4);
        aplicacion.controlador.cosecha(DatosJuego.COL_TIERRA + 2, DatosJuego.FILA_TIERRA);
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 5);
        aplicacion.controlador.vender("CARROT", 3);
        aplicacion.controlador.comprar("HAY", 5);
        aplicacion.controlador.build("COOP", DatosJuego.COL_HIERBA, DatosJuego.FILA_HIERBA);
        Edificio gallinero = aplicacion.estado().granja().edificios().get(0);
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.HEALTHY, 0, false, 0);
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.SICK, 3, false, 1);
        aplicacion.controlador.alimentar(0);
        aplicacion.controlador.diaFin();
        aplicacion.controlador.diaFin();
        return aplicacion;
    }
}
