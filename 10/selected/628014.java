package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoHability;
import edu.univalle.lingweb.persistence.CoHabilityDAO;
import edu.univalle.lingweb.persistence.CoTechnical;
import edu.univalle.lingweb.persistence.CoTechnicalDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_technical'( Tecnicas )
 * 
 * @author Julio Cesar Puentes Delgado
 */
public class DataManagerTechnical extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerTechnical() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
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
    public RestServiceResult create(RestServiceResult result, CoTechnical coTechnical) {
        CoTechnicalDAO coTechnicalDAO = new CoTechnicalDAO();
        try {
            coTechnical.setTechnicalId(getSequence("sq_co_technical"));
            EntityManagerHelper.beginTransaction();
            coTechnicalDAO.save(coTechnical);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coTechnical);
            Object[] args = { coTechnical.getTechnicalName(), coTechnical.getTechnicalId() };
            result.setMessage(MessageFormat.format(bundle.getString("technical.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar la t�cnica: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Realiza la busqueda de una t�cnica
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el
	 * mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nCourseId
	 *            c�digo del programa del curso
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nTechnicalId) {
        CoTechnical coTechnical = new CoTechnicalDAO().findById(nTechnicalId);
        if (coTechnical == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("technical.search.notFound"));
        } else {
            List<CoTechnical> list = new ArrayList<CoTechnical>();
            EntityManagerHelper.refresh(coTechnical);
            list.add(coTechnical);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("technical.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos de una t�cnica
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
    public RestServiceResult update(RestServiceResult serviceResult, CoTechnical coTechnical) {
        CoTechnicalDAO coTechnicalDAO = new CoTechnicalDAO();
        try {
            log.info("Actualizando la t�cnica: " + coTechnical.getTechnicalName());
            EntityManagerHelper.beginTransaction();
            coTechnicalDAO.update(coTechnical);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coTechnical);
            Object[] arrayParam = { coTechnical.getTechnicalName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("technical.update.success"), arrayParam));
            log.info("Se actualizo la t�cnica con �xito: " + coTechnical.getTechnicalName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la t�cnica: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de una t�cnica
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coCourse
	 *            Curso a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoTechnical coTechnical) {
        String sTechnicalName = null;
        CoTechnicalDAO coTechnicalDAO = new CoTechnicalDAO();
        try {
            sTechnicalName = coTechnical.getTechnicalName();
            log.error("Eliminando el Tecnica: " + coTechnical.getTechnicalName());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_TECHNICAL);
            query.setParameter(1, coTechnical.getTechnicalId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sTechnicalName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("technical.delete.success"), arrayParam));
            log.info("Eliminando el curso: " + coTechnical.getTechnicalName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el curso: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coTechnical.getTechnicalName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("technical.delete.error") + e.getMessage(), args));
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de tecnicas
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
	 * Obtiene la lista de tecnicas
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
        CoTechnicalDAO coTechnicalDAO = new CoTechnicalDAO();
        List<CoTechnical> list = coTechnicalDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("technical.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("technical.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coTechnicalDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de tecnicas por habilidad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nHabilityId
	 *            Es el id o c�digo de la habilidad.	 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listTechnicalForHability(RestServiceResult serviceResult, Long nHabilityId) {
        CoHability coHability = new CoHabilityDAO().findById(nHabilityId);
        EntityManagerHelper.refresh(coHability);
        if (coHability == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("technical.search.notFound"));
        } else {
            List<CoTechnical> list = new ArrayList<CoTechnical>(coHability.getCoTechnicals());
            Object[] arrayParam = { list.size(), coHability.getHabilityName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("technical.listTechnicalForHability.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }
}
