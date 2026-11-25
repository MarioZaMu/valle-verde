package edu.ucc.farmsim.api;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El controlador REST: solo traduce HTTP en llamadas al juego y la respuesta de vuelta en HTTP. Las reglas viven más
 * abajo; esta clase no contiene ninguna (AR-11). No es el controlador GRASP, que es {@code ControladorJuego}.
 *
 * <p>Hay un juego por servidor (S-01), así que toda orden corre bajo el mismo candado (DT-20). Los eventos que causa
 * una orden viajan en la respuesta (DT-19). Las rutas están documentadas en {@code docs/API.md}.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38, HU-45, HU-46, HU-47, HU-49.
 */
@RestController
@RequestMapping("/api")
public class ControladorRestJuego {

    private final ComandosJuego juego;
    private final RecolectorEvento recolector;
    private final Object candado = new Object();

    /**
     * Crea el controlador.
     *
     * @param juego      el juego, que se responde en este proceso
     * @param recolector guarda los eventos de cada orden
     */
    public ControladorRestJuego(ComandosJuego juego, RecolectorEvento recolector) {
        this.juego = juego;
        this.recolector = recolector;
    }

    /** Cuerpo de {@code POST /api/game}. */
    public record PeticionJuegoNueva(@NotNull Long semilla) { }

    /** Cuerpo de {@code POST /api/farm/plots/{col}/{row}/crop}. */
    public record PeticionSembrar(@NotBlank String idSemilla) { }

    /** Cuerpo de {@code POST /api/market/purchases} y {@code POST /api/market/sales}. */
    public record PeticionComerciar(@NotBlank String idArticulo, @NotNull Integer cantidad) { }

    /** Cuerpo de {@code POST /api/buildings}. */
    public record PeticionConstruir(@NotBlank String idEdificio, @NotNull Integer col, @NotNull Integer fila) { }

    /** Cuerpo de {@code POST /api/animals}. */
    public record PeticionCompraAnimal(@NotBlank String animalId) { }

    /** Cuerpo de {@code POST /api/avatar/moves}. */
    public record PeticionMover(@NotNull @Pattern(regexp = "NORTH|SOUTH|EAST|WEST") String direccion) { }

    /** Todos los logros con el progreso del jugador. */
    @GetMapping("/achievements")
    public ResponseEntity<?> logros() {
        return run(juego::logros, HttpStatus.OK);
    }

    /** Lo que guarda cada ranura de partida. */
    @GetMapping("/saves")
    public ResponseEntity<?> guardarRanuras() {
        return run(juego::guardarRanuras, HttpStatus.OK);
    }

    /** Guarda la partida en curso en una ranura, reemplazando lo que tenía (RN-31). */
    @PutMapping("/saves/{ranura}")
    public ResponseEntity<?> guardar(@PathVariable int ranura) {
        return run(() -> juego.guardar(ranura), HttpStatus.OK);
    }

    /** Carga una ranura y la convierte en la partida en curso (RN-33). */
    @PostMapping("/saves/{ranura}/load")
    public ResponseEntity<?> cargar(@PathVariable int ranura) {
        return run(() -> juego.cargar(ranura), HttpStatus.OK);
    }

    /** Inicia una partida nueva (RN-30). */
    @PostMapping("/game")
    public ResponseEntity<?> juegoNuevo(@Valid @RequestBody PeticionJuegoNueva peticion) {
        return run(() -> juego.juegoNuevo(peticion.semilla()), HttpStatus.CREATED);
    }

    /** La barra superior de la partida en curso. */
    @GetMapping("/game")
    public ResponseEntity<?> hud() {
        return run(juego::hud, HttpStatus.OK);
    }

    /** Termina el día (RN-03). */
    @PostMapping("/game/days")
    public ResponseEntity<?> diaFin() {
        return run(juego::diaFin, HttpStatus.OK);
    }

    /** Todo el mapa de la granja. */
    @GetMapping("/farm/map")
    public ResponseEntity<?> mapa() {
        return run(juego::mapa, HttpStatus.OK);
    }

    /** Un paso del avatar (RN-36). */
    @PostMapping("/avatar/moves")
    public ResponseEntity<?> mover(@Valid @RequestBody PeticionMover peticion) {
        return run(() -> juego.mover(peticion.direccion()), HttpStatus.OK);
    }

    /** Las personas del valle (RN-37). */
    @GetMapping("/npcs")
    public ResponseEntity<?> npcs() {
        return run(juego::npcs, HttpStatus.OK);
    }

    /** Lo que dice una persona del valle, si el avatar está a su lado (RN-37). */
    @PostMapping("/npcs/{id}/talk")
    public ResponseEntity<?> hablar(@PathVariable String id) {
        return run(() -> juego.hablar(id), HttpStatus.OK);
    }

    /** Entrega el encargo de una persona del valle (RN-38). */
    @PostMapping("/npcs/{id}/errand")
    public ResponseEntity<?> entregar(@PathVariable String id) {
        return run(() -> juego.entregar(id), HttpStatus.OK);
    }

    /** El tutorial de inicio de la partida en curso (RN-39). */
    @GetMapping("/tutorial")
    public ResponseEntity<?> tutorial() {
        return run(juego::tutorial, HttpStatus.OK);
    }

    /** Salta el tutorial de inicio (RN-39). */
    @PostMapping("/tutorial/skip")
    public ResponseEntity<?> tutorialSaltar() {
        return run(juego::tutorialSaltar, HttpStatus.OK);
    }

    /** Describe una parcela sin cambiar nada. */
    @GetMapping("/farm/plots/{col}/{fila}")
    public ResponseEntity<?> inspeccionar(@PathVariable int col, @PathVariable int fila) {
        return run(() -> juego.inspeccionar(col, fila), HttpStatus.OK);
    }

    /** Siembra una semilla en una parcela (RN-06). */
    @PostMapping("/farm/plots/{col}/{fila}/crop")
    public ResponseEntity<?> plantar(@PathVariable int col, @PathVariable int fila,
                                   @Valid @RequestBody PeticionSembrar peticion) {
        return run(() -> juego.plantar(col, fila, peticion.idSemilla()), HttpStatus.CREATED);
    }

    /** Riega el cultivo de una parcela (RN-07). */
    @PostMapping("/farm/plots/{col}/{fila}/watering")
    public ResponseEntity<?> agua(@PathVariable int col, @PathVariable int fila) {
        return run(() -> juego.agua(col, fila), HttpStatus.OK);
    }

    /** Cosecha un cultivo maduro o limpia uno marchito (RN-10, RN-34). */
    @PostMapping("/farm/plots/{col}/{fila}/harvest")
    public ResponseEntity<?> cosecha(@PathVariable int col, @PathVariable int fila) {
        return run(() -> juego.cosecha(col, fila), HttpStatus.OK);
    }

    /** Si al menos una ranura de guardado contiene una partida. */
    @GetMapping("/saves/any")
    public ResponseEntity<?> tieneJuegosArchivados() {
        return run(juego::tieneJuegosArchivados, HttpStatus.OK);
    }

    /** Precios, tendencias y el catálogo de compra. */
    @GetMapping("/market")
    public ResponseEntity<?> mercado() {
        return run(juego::mercado, HttpStatus.OK);
    }

    /** Compra unidades al precio fijo (RN-16). */
    @PostMapping("/market/purchases")
    public ResponseEntity<?> comprar(@Valid @RequestBody PeticionComerciar peticion) {
        return run(() -> juego.comprar(peticion.idArticulo(), peticion.cantidad()), HttpStatus.OK);
    }

    /** Vende unidades al precio dinámico (RN-17, RN-18). */
    @PostMapping("/market/sales")
    public ResponseEntity<?> vender(@Valid @RequestBody PeticionComerciar peticion) {
        return run(() -> juego.vender(peticion.idArticulo(), peticion.cantidad()), HttpStatus.OK);
    }

    /** Lo que pagaría ahora vender la cantidad, sin vender. */
    @GetMapping("/market/quotes")
    public ResponseEntity<?> cotizar(@RequestParam String idArticulo, @RequestParam int cantidad) {
        return run(() -> juego.cotizar(idArticulo, cantidad), HttpStatus.OK);
    }

    /** El inventario como una grilla. */
    @GetMapping("/inventory")
    public ResponseEntity<?> inventario() {
        return run(juego::inventario, HttpStatus.OK);
    }

    /** Los edificios construidos y el catálogo de lo que se puede construir. */
    @GetMapping("/buildings")
    public ResponseEntity<?> edificios() {
        return run(juego::edificios, HttpStatus.OK);
    }

    /** Construye un edificio sobre pasto libre (RN-20, RN-21). */
    @PostMapping("/buildings")
    public ResponseEntity<?> construir(@Valid @RequestBody PeticionConstruir peticion) {
        return run(() -> juego.construir(peticion.idEdificio(), peticion.col(), peticion.fila()), HttpStatus.CREATED);
    }

    /** Compra un animal y lo aloja en el primer edificio compatible con espacio (RN-23). */
    @PostMapping("/animals")
    public ResponseEntity<?> animalComprar(@Valid @RequestBody PeticionCompraAnimal peticion) {
        return run(() -> juego.animalComprar(peticion.animalId()), HttpStatus.CREATED);
    }

    /** Alimenta a los animales de un edificio (RN-24). */
    @PostMapping("/buildings/{id}/feeding")
    public ResponseEntity<?> alimentar(@PathVariable int id) {
        return run(() -> juego.alimentar(id), HttpStatus.OK);
    }

    /** Recoge los productos que esperan en un edificio (RN-26). */
    @PostMapping("/buildings/{id}/collection")
    public ResponseEntity<?> collect(@PathVariable int id) {
        return run(() -> juego.collect(id), HttpStatus.OK);
    }

    /** Cura a un animal enfermo de un edificio (RN-27). */
    @PostMapping("/buildings/{id}/animals/{indice}/treatment")
    public ResponseEntity<?> curar(@PathVariable int id, @PathVariable int indice) {
        return run(() -> juego.curar(id, indice), HttpStatus.OK);
    }

    private <T> ResponseEntity<?> run(Supplier<Resultado<T>> orden, HttpStatus exito) {
        synchronized (candado) {
            recolector.drenar();
            Resultado<T> resultado = orden.get();
            List<DtoEvento> eventos = recolector.drenar().stream().map(DtoEvento::of).toList();
            if (resultado.isOk()) {
                return ResponseEntity.status(exito).body(new RespuestaApi<>(resultado.value(), eventos));
            }
            return ResponseEntity.status(MapeadorEstadoError.situacionDe(resultado.codigo()))
                    .body(new ApiError(resultado.codigo().name(), resultado.mensaje()));
        }
    }
}
