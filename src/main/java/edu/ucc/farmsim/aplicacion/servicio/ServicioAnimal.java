package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaAccionEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Mensajes;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.juego.Energia;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Los casos de uso de animales: comprar y alojar (RN-23), alimentar un edificio (RN-24), recoger sus productos (RN-26)
 * y curar a un animal enfermo (RN-27). Cada operación comprueba primero todas sus reglas y solo después cambia la
 * partida, de modo que un rechazo nunca deja nada a medias.
 *
 * <p>Stories: HU-19, HU-20, HU-22, HU-23, HU-26.
 */
@Service
public final class ServicioAnimal {

    private final SesionJuego sesion;
    private final MapeadorVista mapeador;
    private final BusEvento eventos;
    private final ServicioLogro logros;

    /**
     * Crea el servicio.
     *
     * @param sesion la partida en curso
     * @param mapeador  construye las vistas que dibuja la UI
     * @param eventos  donde se publican los eventos
     * @param logros evalúa los logros después de comprar y curar
     */
    public ServicioAnimal(SesionJuego sesion, MapeadorVista mapeador, BusEvento eventos, ServicioLogro logros) {
        this.sesion = sesion;
        this.mapeador = mapeador;
        this.eventos = eventos;
        this.logros = logros;
    }

    /**
     * Compra un animal y lo aloja en el primer edificio de su tipo con un lugar libre (RN-23).
     *
     * @param animalId id del animal, por ejemplo {@code HEN}
     * @return el edificio donde vive ahora el animal
     */
    public Resultado<VistaEdificio> animalComprar(String animalId) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        TipoAnimal tipo = TipoAnimal.valueOf(animalId);
        EstadoJuego estado = sesion.actual();
        if (tipo.precio() > estado.dinero()) {
            return Resultado.fallarCon(CodigoError.SIN_FONDOS, tipo.precio() - estado.dinero());
        }
        Edificio hogar = estado.granja().edificios().stream().filter(b -> b.tieneEspacioPara(tipo)).findFirst().orElse(null);
        if (hogar == null) {
            return Resultado.fallarCon(CodigoError.SIN_ESPACIO_ANIMALES, Nombres.animalPlural(tipo.name()));
        }
        estado.gastar(tipo.precio());
        hogar.animalAgregar(new Animal(tipo));
        eventos.publish(new EventoJuego.DineroCambiado(estado.dinero()));
        eventos.publish(new EventoJuego.MapaCambiado());
        logros.comprobar(estado);
        return Resultado.ok(mapeador.edificio(hogar));
    }

    /**
     * Alimenta a todos los animales de un edificio que pueden comer y no han comido hoy: 1 de heno y 1 de energía cada
     * uno, todo o nada (RN-24). Los animales enfermos se omiten y se avisa al jugador.
     *
     * @param idEdificio id del edificio
     */
    public Resultado<VistaAccionEdificio> alimentar(int idEdificio) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        Edificio edificio = edificio(estado, idEdificio);
        List<Animal> hambriento = edificio.animales().stream().filter(Animal::alimentacionNecesita).toList();
        long enfermo = edificio.animales().stream().filter(Animal::esEnfermo).count();
        String notificacionEnferma = enfermo > 0 ? Mensajes.of(CodigoError.ANIMAL_ENFERMO) : null;
        if (hambriento.isEmpty()) {
            return Resultado.ok(new VistaAccionEdificio(mapeador.edificio(edificio), 0,
                    notificacionEnferma != null ? notificacionEnferma : Mensajes.NADA_A_ALIMENTAR));
        }
        int heno = hambriento.size() * ReglasJuego.HENO_POR_ANIMAL;
        if (estado.inventario().count(TipoArticulo.HAY) < heno) {
            return Resultado.fallarCon(CodigoError.SIN_HENO, heno);
        }
        int costoEnergia = hambriento.size() * ReglasJuego.ALIMENTAR_COSTO_ENERGIA_POR_ANIMAL;
        Energia energia = estado.energia();
        if (!energia.puedeGastar(costoEnergia)) {
            return Resultado.fallarCon(CodigoError.SIN_ENERGIA, costoEnergia, energia.actual());
        }
        estado.inventario().remove(TipoArticulo.HAY, heno);
        energia.gastarIntentar(costoEnergia);
        hambriento.forEach(Animal::alimentar);
        cambiarPublicar(estado, true);
        return Resultado.ok(new VistaAccionEdificio(mapeador.edificio(edificio), hambriento.size(), notificacionEnferma));
    }

    /**
     * Mueve al inventario todas las unidades que esperan en un edificio: 1 de energía, todo o nada (RN-26).
     *
     * @param idEdificio id del edificio
     */
    public Resultado<VistaAccionEdificio> collect(int idEdificio) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        Edificio edificio = edificio(estado, idEdificio);
        int pendiente = edificio.productosPendientes();
        if (pendiente == 0) {
            return Resultado.ok(new VistaAccionEdificio(mapeador.edificio(edificio), 0, Mensajes.NADA_A_RECOLECTAR));
        }
        Energia energia = estado.energia();
        if (!energia.puedeGastar(ReglasJuego.RECOLECTAR_COSTO_ENERGIA)) {
            return Resultado.fallarCon(CodigoError.SIN_ENERGIA, ReglasJuego.RECOLECTAR_COSTO_ENERGIA, energia.actual());
        }
        if (!estado.inventario().add(edificio.producto(), pendiente)) {
            return Resultado.fallarCon(CodigoError.INVENTARIO_LLENO);
        }
        edificio.todoRecolectar();
        energia.gastarIntentar(ReglasJuego.RECOLECTAR_COSTO_ENERGIA);
        cambiarPublicar(estado, true);
        return Resultado.ok(new VistaAccionEdificio(mapeador.edificio(edificio), pendiente, null));
    }

    /**
     * Cura a un animal enfermo con una medicina y 1 de energía; queda sano de inmediato (RN-27).
     *
     * @param idEdificio  id del edificio
     * @param indiceAnimal posición del animal dentro del edificio
     */
    public Resultado<VistaAccionEdificio> curar(int idEdificio, int indiceAnimal) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        Edificio edificio = edificio(estado, idEdificio);
        if (indiceAnimal < 0 || indiceAnimal >= edificio.animales().size()) {
            throw new IllegalArgumentException("No animal " + indiceAnimal + " in building " + idEdificio);
        }
        Animal animal = edificio.animales().get(indiceAnimal);
        if (!animal.esEnfermo()) {
            return Resultado.ok(new VistaAccionEdificio(mapeador.edificio(edificio), 0, Mensajes.NO_ENFERMO));
        }
        if (estado.inventario().count(TipoArticulo.MEDICINE) < ReglasJuego.MEDICINA_POR_CURAR) {
            return Resultado.fallarCon(CodigoError.SIN_MEDICINA);
        }
        Energia energia = estado.energia();
        if (!energia.puedeGastar(ReglasJuego.CURAR_COSTO_ENERGIA)) {
            return Resultado.fallarCon(CodigoError.SIN_ENERGIA, ReglasJuego.CURAR_COSTO_ENERGIA, energia.actual());
        }
        estado.inventario().remove(TipoArticulo.MEDICINE, ReglasJuego.MEDICINA_POR_CURAR);
        energia.gastarIntentar(ReglasJuego.CURAR_COSTO_ENERGIA);
        animal.curar();
        estado.progreso().curarRegistro();
        cambiarPublicar(estado, true);
        logros.comprobar(estado);
        return Resultado.ok(new VistaAccionEdificio(mapeador.edificio(edificio), 1, null));
    }

    private static Edificio edificio(EstadoJuego estado, int idEdificio) {
        List<Edificio> edificios = estado.granja().edificios();
        if (idEdificio < 0 || idEdificio >= edificios.size()) {
            throw new IllegalArgumentException("No building " + idEdificio);
        }
        return edificios.get(idEdificio);
    }

    private void cambiarPublicar(EstadoJuego estado, boolean inventarioCambiado) {
        eventos.publish(new EventoJuego.MapaCambiado());
        eventos.publish(new EventoJuego.EnergiaCambiada(estado.energia().actual(), estado.energia().max()));
        if (inventarioCambiado) {
            eventos.publish(new EventoJuego.InventarioCambiado());
        }
    }
}
