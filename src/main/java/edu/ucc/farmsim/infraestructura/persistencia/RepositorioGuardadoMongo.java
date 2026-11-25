package edu.ucc.farmsim.infraestructura.persistencia;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.guardado.ExcepcionAlmacenamientoGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado.Motivo;
import edu.ucc.farmsim.dominio.guardado.InfoRanura;
import edu.ucc.farmsim.dominio.guardado.RepositorioGuardado;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.MappingInstantiationException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * Guarda cada partida como un documento de la colección {@code partidas}, uno por ranura (RN-31, DT-21). Guardar
 * reemplaza el documento completo en una sola operación, así que una escritura fallida deja intacto el anterior. Todo
 * fallo técnico de MongoDB se convierte en una {@link ExcepcionAlmacenamientoGuardado}, de modo que nada por encima de esta clase
 * sabe de MongoDB.
 *
 * <p>El perfil {@code no-mongo} lo deja fuera, para que las pruebas de la API y del cliente corran sin base de datos.
 *
 * <p>Stories: HU-29, HU-30, HU-31, HU-32, HU-33.
 */
@Repository
@Profile("!no-mongo")
public class RepositorioGuardadoMongo implements RepositorioGuardado {

    private static final Logger BITACORA = Logger.getLogger(RepositorioGuardadoMongo.class.getName());
    private static final String INALCANZABLE = "MongoDB no respondió";

    private final MongoTemplate mongo;
    private final CodecGuardado codec = new CodecGuardado();

    /**
     * Crea el repositorio.
     *
     * @param mongo el acceso de Spring Data a la base configurada en {@code valleverde.mongo.uri}
     */
    public RepositorioGuardadoMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean existeRanura(int ranura) {
        ranuraExigir(ranura);
        return protegido("looking for slot " + ranura,
                () -> mongo.exists(query(where("_id").is(DocumentoGuardado.idDe(ranura))), DocumentoGuardado.class));
    }

    @Override
    public void guardar(int ranura, MementoJuego memento, Instant archivadoEn) {
        ranuraExigir(ranura);
        DocumentoGuardado documento = codec.codificar(ranura, memento, archivadoEn);
        protegido("saving slot " + ranura, () -> mongo.save(documento));
    }

    @Override
    public MementoJuego cargar(int ranura) {
        ranuraExigir(ranura);
        DocumentoGuardado documento = protegido("reading slot " + ranura, () -> mongo.findById(DocumentoGuardado.idDe(ranura), DocumentoGuardado.class));
        if (documento == null) {
            throw new ExcepcionCargarGuardado(Motivo.MISSING, "slot " + ranura + " holds no document");
        }
        return codec.decodificar(documento);
    }

    @Override
    public List<InfoRanura> ranuras() {
        List<InfoRanura> ranuras = new ArrayList<>();
        for (int ranura = 1; ranura <= ReglasJuego.RANURAS_GUARDADO; ranura++) {
            int numero = ranura;
            DocumentoGuardado documento;
            try {
                documento = protegido("reading slot " + numero,
                        () -> mongo.findById(DocumentoGuardado.idDe(numero), DocumentoGuardado.class));
            } catch (ExcepcionCargarGuardado e) {
                ranuras.add(InfoRanura.danado(ranura));
                continue;
            }
            ranuras.add(documento == null ? InfoRanura.empty(ranura) : describir(ranura, documento));
        }
        return ranuras;
    }

    private InfoRanura describir(int ranura, DocumentoGuardado documento) {
        try {
            MementoJuego memento = codec.decodificar(documento);
            return InfoRanura.ok(ranura, memento.fecha(), memento.dinero(), documento.archivadoEn());
        } catch (ExcepcionCargarGuardado e) {
            BITACORA.log(Level.WARNING, "Slot " + ranura + " is damaged: " + e.getMessage());
            return InfoRanura.danado(ranura);
        }
    }

    private static void ranuraExigir(int ranura) {
        if (ranura < 1 || ranura > ReglasJuego.RANURAS_GUARDADO) {
            throw new IllegalArgumentException("slot must be between 1 and " + ReglasJuego.RANURAS_GUARDADO + ": " + ranura);
        }
    }

    private <T> T protegido(String que, Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (MappingException | MappingInstantiationException e) {
            // el documento existe pero no tiene la forma esperada (por ejemplo, lo guardó una versión anterior): está dañado
            BITACORA.log(Level.WARNING, "A document could not be read while " + que, e);
            throw new ExcepcionCargarGuardado(Motivo.CORRUPT, "the document cannot be read: " + e.getMessage());
        } catch (RuntimeException e) {
            BITACORA.log(Level.WARNING, "MongoDB failed while " + que, e);
            throw new ExcepcionAlmacenamientoGuardado(INALCANZABLE, e);
        }
    }
}
