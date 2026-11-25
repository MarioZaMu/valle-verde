package edu.ucc.farmsim.infraestructura.persistencia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado.Motivo;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import java.time.Instant;

/**
 * Convierte una partida en el documento de una ranura y viceversa (RN-32, DT-11): el estado como texto JSON con
 * Jackson, la versión del formato y la suma de control. La lectura sigue el orden de RN-33: texto bien formado, versión
 * admitida, suma de control coincidente y, al final, una partida que se pueda construir desde el texto. La versión 2
 * agregó el avatar (RN-36); un documento de versión 1 aún se lee, y su partida no tiene avatar, así que empieza en su
 * lugar. La versión 3 amplió el mapa al valle de 48 x 24 (RN-40): los cultivos de un documento más antiguo conservan
 * sus parcelas en su esquina noroeste.
 *
 * <p>Stories: HU-29, HU-30, HU-31, HU-45, HU-50.
 */
public final class CodecGuardado {

    /** Versión del formato de {@code estado} que se escribe. */
    public static final int VERSION_ESQUEMA = 3;

    /** Primera versión con el valle de 48 x 24; los documentos anteriores guardan los cultivos del mapa de 16 x 10. */
    public static final int VERSION_VALLE = 3;

    /** Versión más antigua que todavía se puede leer: la versión 1 no tiene avatar. */
    public static final int VERSION_ANTIGUA_LEGIBLE = 1;

    private final ObjectMapper mapeador = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /** El documento de una ranura para la partida, con la versión actual y la suma de control de su texto. */
    public DocumentoGuardado codificar(int ranura, MementoJuego memento, Instant archivadoEn) {
        try {
            String estado = mapeador.writeValueAsString(memento);
            return new DocumentoGuardado(DocumentoGuardado.idDe(ranura), VERSION_ESQUEMA, archivadoEn, estado, Checksum.of(estado));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The game cannot be written as JSON", e);
        }
    }

    /**
     * La partida guardada en un documento.
     *
     * @throws ExcepcionCargarGuardado si el texto no es válido, la versión es desconocida, la suma de control difiere o el
     *                           texto no describe una partida
     */
    public MementoJuego decodificar(DocumentoGuardado documento) {
        String estado = documento.estado();
        if (estado == null) {
            throw new ExcepcionCargarGuardado(Motivo.CORRUPT, "the document has no state");
        }
        try {
            mapeador.readTree(estado);
        } catch (JsonProcessingException e) {
            throw new ExcepcionCargarGuardado(Motivo.CORRUPT, "the state is not well-formed JSON: " + e.getOriginalMessage());
        }
        if (documento.versionEsquema() < VERSION_ANTIGUA_LEGIBLE || documento.versionEsquema() > VERSION_ESQUEMA) {
            throw new ExcepcionCargarGuardado(Motivo.UNSUPPORTED_VERSION, "schemaVersion " + documento.versionEsquema());
        }
        if (!Checksum.of(estado).equals(documento.checksum())) {
            throw new ExcepcionCargarGuardado(Motivo.CORRUPT, "the checksum does not match");
        }
        try {
            MementoJuego memento = mapeador.readValue(estado, MementoJuego.class);
            if (memento == null) {
                throw new ExcepcionCargarGuardado(Motivo.CORRUPT, "the state is empty");
            }
            if (documento.versionEsquema() < VERSION_VALLE && memento.granja() != null && memento.granja().cultivos() != null
                    && memento.granja().cultivos().size() == ReglasJuego.COLS_MAPA_HEREDADOS * ReglasJuego.FILAS_MAPA_HEREDADOS) {
                return memento.conGranja(memento.granja().ensanchado(ReglasJuego.COLS_MAPA_HEREDADOS, ReglasJuego.COLS_MAPA,
                        ReglasJuego.FILAS_MAPA));
            }
            return memento;
        } catch (JsonProcessingException | RuntimeException e) {
            if (e instanceof ExcepcionCargarGuardado fallo) {
                throw fallo;
            }
            throw new ExcepcionCargarGuardado(Motivo.CORRUPT, "the state does not describe a game: " + e.getMessage());
        }
    }
}
