package edu.ucc.farmsim.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.DirectorEdificio;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.granja.MapaGranja;
import org.junit.jupiter.api.Test;

/** Los cuatro constructores y su director producen los edificios de RN-20; el mapa los conserva (RN-21). */
class DirectorEdificioTest {

    @Test
    void gallinasCuatroCasasGallineroElYSinoNada() {
        Edificio gallinero = DirectorEdificio.armar(TipoEdificio.COOP, 0, 2, 7);

        assertEquals(TipoAnimal.HEN, gallinero.especie());
        assertEquals(4, gallinero.capacidadAnimal());
        assertEquals(0, gallinero.ranurasInventarioExtra());
        assertEquals(TipoArticulo.EGG, gallinero.producto());
        assertTrue(gallinero.cubre(3, 8));
        assertEquals(false, gallinero.cubre(4, 8));
    }

    @Test
    void vacasCuatroCasasEstabloElYColumnasTresEsAmplios() {
        Edificio establo = DirectorEdificio.armar(TipoEdificio.BARN, 1, 2, 7);

        assertEquals(TipoAnimal.COW, establo.especie());
        assertEquals(4, establo.capacidadAnimal());
        assertEquals(3, establo.width());
        assertEquals(2, establo.height());
        assertEquals(TipoArticulo.MILK, establo.producto());
    }

    @Test
    void ranurasDoceAgregaAlmacenElYNadieCasas() {
        Edificio almacen = DirectorEdificio.armar(TipoEdificio.WAREHOUSE, 0, 2, 7);

        assertEquals(12, almacen.ranurasInventarioExtra());
        assertEquals(0, almacen.capacidadAnimal());
        assertNull(almacen.producto());
        assertEquals(false, almacen.animalAgregar(new Animal(TipoAnimal.HEN)));
    }

    @Test
    void parcelasNueveProtegeInvernaderoEl() {
        Edificio invernadero = DirectorEdificio.armar(TipoEdificio.GREENHOUSE, 0, 2, 7);

        assertEquals(9, invernadero.parcelasProtegidos());
        assertEquals(3, invernadero.width());
        assertEquals(3, invernadero.height());
    }

    @Test
    void aCoopRefusesCowsAndAFifthHen() {
        Edificio gallinero = DirectorEdificio.armar(TipoEdificio.COOP, 0, 2, 7);

        assertEquals(false, gallinero.animalAgregar(new Animal(TipoAnimal.COW)));
        for (int i = 0; i < 4; i++) {
            assertTrue(gallinero.animalAgregar(new Animal(TipoAnimal.HEN)));
        }
        assertEquals(false, gallinero.animalAgregar(new Animal(TipoAnimal.HEN)));
    }

    @Test
    void soloEdificiosLugaresMapaElOnHierbaLibreYEllosRestaura() {
        MapaGranja map = MapaGranja.inicial();
        Edificio gallinero = map.colocar(TipoEdificio.COOP, 2, 7);
        gallinero.animalAgregar(new Animal(TipoAnimal.HEN));
        var instantanea = map.instantanea();

        assertThrows(IllegalStateException.class, () -> map.colocar(TipoEdificio.COOP, 3, 8));
        map.restaurarDesde(new edu.ucc.farmsim.dominio.granja.InstantaneaMapaGranja(instantanea.cultivos(), java.util.List.of()));
        assertTrue(map.edificios().isEmpty());
        assertNull(map.edificioEn(2, 7));
        map.restaurarDesde(instantanea);

        assertEquals(1, map.edificios().size());
        assertEquals(1, map.edificios().get(0).animales().size());
        assertEquals(1, map.contarEdificios(TipoEdificio.COOP));
        assertNull(map.edificioEn(-1, 0));
    }

    @Test
    void alojamientoElDeEdificioElEsAnimalCadaCoincidente() {
        assertEquals(TipoEdificio.COOP, TipoEdificio.alojamiento(TipoAnimal.HEN));
        assertEquals(TipoEdificio.BARN, TipoEdificio.alojamiento(TipoAnimal.COW));
    }
}
