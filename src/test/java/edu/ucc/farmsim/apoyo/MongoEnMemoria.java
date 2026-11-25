package edu.ucc.farmsim.apoyo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucc.farmsim.infraestructura.persistencia.DocumentoGuardado;
import edu.ucc.farmsim.infraestructura.persistencia.RepositorioGuardadoMongo;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Un MongoDB que vive en un mapa: encima de él corre el {@link RepositorioGuardadoMongo} real (codec, suma de control,
 * formato), de modo que guardar, cargar y los documentos dañados se prueban de punta a punta sin base de datos. Las
 * pruebas pueden leer y cambiar los documentos guardados, o hacer que el «servidor» deje de responder.
 */
public final class MongoEnMemoria {

    private final Map<String, DocumentoGuardado> documentos = new HashMap<>();
    private final MongoTemplate plantilla = mock(MongoTemplate.class);
    private boolean abajo;
    private final RepositorioGuardadoMongo repositorio;

    /** Crea una base de datos vacía y el repositorio sobre ella. */
    public MongoEnMemoria() {
        when(plantilla.save(any(DocumentoGuardado.class))).thenAnswer(llamar -> {
            fallarSiAbajo();
            DocumentoGuardado documento = llamar.getArgument(0);
            documentos.put(documento.id(), documento);
            return documento;
        });
        when(plantilla.findById(anyString(), eq(DocumentoGuardado.class))).thenAnswer(llamar -> {
            fallarSiAbajo();
            return documentos.get(llamar.<String>getArgument(0));
        });
        when(plantilla.exists(any(Query.class), eq(DocumentoGuardado.class))).thenAnswer(llamar -> {
            fallarSiAbajo();
            Query query = llamar.getArgument(0);
            String json = query.getQueryObject().toJson();
            return documentos.keySet().stream().anyMatch(json::contains);
        });
        repositorio = new RepositorioGuardadoMongo(plantilla);
    }

    private void fallarSiAbajo() {
        if (abajo) {
            throw new DataAccessResourceFailureException("timed out after 2000 ms");
        }
    }

    /** El repositorio que usa la aplicación. */
    public RepositorioGuardadoMongo repositorio() {
        return repositorio;
    }

    /** El documento guardado en una ranura, o {@code null}. */
    public DocumentoGuardado documento(int ranura) {
        return documentos.get(DocumentoGuardado.idDe(ranura));
    }

    /** Reemplaza el documento de una ranura por una copia cambiada (para dañarlo). */
    public void manipular(int ranura, UnaryOperator<DocumentoGuardado> cambiar) {
        documentos.compute(DocumentoGuardado.idDe(ranura), (id, viejo) -> cambiar.apply(viejo));
    }

    /** Guarda un documento tal como está. */
    public void put(DocumentoGuardado documento) {
        documentos.put(documento.id(), documento);
    }

    /** Hace que el «servidor» deje de responder, como si se agotara el tiempo. */
    public void abajoIr() {
        abajo = true;
    }

    /** Hace que el «servidor» vuelva a responder. */
    public void atrasVenir() {
        abajo = false;
    }
}
