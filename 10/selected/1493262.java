package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToAssistance;
import edu.univalle.lingweb.persistence.ToAssistanceDAO;
import edu.univalle.lingweb.persistence.ToAssistenceMetadata;
import edu.univalle.lingweb.persistence.ToAssistenceMetadataDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'to_assistance'( Ayudas )
 * @author   Jose Luis Aricapa M, Julio C�sar Puentes D
 * @uml.dependency   supplier="edu.univalle.lingweb.persistence.ToAssistenceMetadataDAO"
 */
public class DataManagerAssistance extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerAssistance.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerAssistance() {
        super();
        DOMConfigurator.configure(DataManagerAssistance.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nueva t�cnica en la base de datos
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, ToAssistance toAssistance) {
        ToAssistanceDAO maAssistanceDAO = new ToAssistanceDAO();
        try {
            toAssistance.setAssistanceId(getSequence("sq_to_assistance"));
            EntityManagerHelper.beginTransaction();
            maAssistanceDAO.save(toAssistance);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toAssistance);
            log.info("Se cre� la ayuda con �xito: " + toAssistance.getAssistanceId());
            Object[] args = { toAssistance.getAssistanceId() };
            result.setMessage(MessageFormat.format(bundle.getString("assistance.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar la Ayuda: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Reliza la busqueda de una ayuda a partir de su c�digo 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nAssistanceId C�digo de la glosa
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nAssistanceId) {
        ToAssistance maAssistance = new ToAssistanceDAO().findById(nAssistanceId);
        if (maAssistance == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("assistance.search.notFound"));
        } else {
            List<ToAssistance> list = new ArrayList<ToAssistance>();
            EntityManagerHelper.refresh(maAssistance);
            list.add(maAssistance);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("assistance.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
            log.info("Se encontro " + list.size() + " configuraci�n para el asistente " + nAssistanceId);
        }
        return serviceResult;
    }

    /**
	 * Reliza la busqueda de una ayuda a partir del nombre de la tabla y el campo de un metadata
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nAssistanceId C�digo de la glosa
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult searchAssistantByMet(RestServiceResult serviceResult, String sTableName, String sColumnName) {
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_TO_ASSISTANCE_BY_METADATA, ToAssistenceMetadata.class);
        query.setParameter(1, sTableName);
        query.setParameter(2, sColumnName);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        List<ToAssistenceMetadata> list = query.getResultList();
        ToAssistenceMetadata toAssistanceAssistenceMetadata = null;
        Long nAssistanceId = null;
        if (list != null) for (int i = 0; i < list.size(); i++) {
            toAssistanceAssistenceMetadata = (ToAssistenceMetadata) list.get(i);
        }
        if (toAssistanceAssistenceMetadata != null) {
            nAssistanceId = toAssistanceAssistenceMetadata.getToAssistance().getAssistanceId();
            serviceResult = search(serviceResult, nAssistanceId);
        } else {
            serviceResult.setMessage(bundle.getString("assistance.search.notFound"));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de una Ayuda
	 * @param serviceResult serviceResult El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param maAssistance Ayuda a Eliminar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToAssistance maAssistance) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.DELETE_TO_ASSISTANCE);
            query.setParameter(1, maAssistance.getAssistanceId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maAssistance);
            Object[] arrayParam = { maAssistance.getAssistanceId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("assistance.delete.success"), arrayParam));
            log.info("Eliminando la configuraci�n del asistente: " + maAssistance.getAssistanceId());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la configuraci�n del asistente: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maAssistance.getAssistanceId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("assistance.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos de una respuesta Abierta
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coCourse
	 *            curso a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToAssistance assistance) {
        ToAssistanceDAO assistanceDAO = new ToAssistanceDAO();
        try {
            log.info("Actualizando la Ayuda: " + assistance.getAssistanceId());
            EntityManagerHelper.beginTransaction();
            assistanceDAO.update(assistance);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(assistance);
            Object[] arrayParam = { assistance.getAssistanceId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("assistance.update.success"), arrayParam));
            log.info("Se actualizo la Ayuda con �xito: " + assistance.getAssistanceId());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la Ayuda: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de Ayudas
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de Ayudas
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la
	 *            consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result, int nRowStart, int nMaxResults) {
        ToAssistanceDAO assistanceDAO = new ToAssistanceDAO();
        List<ToAssistance> list = assistanceDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("assistance.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("assistance.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(assistanceDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Registra la metadata para un asistente
	 * @param toAssistenceMetadata
	 *            metadata asociada al asistente
	 * @param toAssistance
	 *            asistente para los formularios
	 */
    private void addMetadata(ToAssistenceMetadata toAssistenceMetadata, ToAssistance toAssistance) {
        try {
            toAssistenceMetadata.setAssistanceMetadataId(getSequence("sq_to_assistence_metadata"));
            EntityManagerHelper.beginTransaction();
            new ToAssistenceMetadataDAO().save(toAssistenceMetadata);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toAssistenceMetadata);
            log.info("Se asocio la getAssistanceMetadataId " + toAssistenceMetadata.getAssistanceMetadataId());
            log.info("Se asocio la getTableName  " + toAssistenceMetadata.getTableName());
            log.info("Se asocio la getColumnName  " + toAssistenceMetadata.getColumnName());
            log.info("Se asocio la getMetadataTypeId  " + toAssistenceMetadata.getToMetadataType().getMetadataTypeId());
            log.info("Se asocio la getAssistanceId  " + toAssistenceMetadata.getToAssistance().getAssistanceId());
            log.info("Se asocio la metadata '" + toAssistenceMetadata.getAssistanceMetadataId() + "' con �xito al asistente '" + toAssistance.getAssistanceId() + "' ");
        } catch (PersistenceException e) {
            log.info("La metadata '" + toAssistenceMetadata.getAssistanceMetadataId() + "' ya esta asociada a al asistente '" + toAssistance.getAssistanceId() + "' " + "OMITIR EXCEPCION PRIMARY KEY");
        }
        return;
    }

    /**
	 * Realiza el proceso de asociar una metadata al asistente
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.s
	 */
    public RestServiceResult addMetadata(RestServiceResult serviceResult, ToAssistenceMetadata toAssistenceMetadata, ToAssistance toAssistance) {
        log.info("METADATA ASOCIADA AL ASISTENTE");
        Long nAssistanceId = toAssistance.getAssistanceId();
        log.info("Agregando Metadata al Asistente: " + nAssistanceId);
        addMetadata(toAssistenceMetadata, toAssistance);
        log.info("TERMININA ASOCIACION DE LA METADATA");
        return serviceResult;
    }
}
