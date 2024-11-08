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
import edu.univalle.lingweb.persistence.ToNotebook;
import edu.univalle.lingweb.persistence.ToNotebookDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_notebook'( Cuaderno )
 * 
 * @author Liliana Machuca
 */
public class DataManagerNotebook extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerNotebook.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerNotebook() {
        super();
        DOMConfigurator.configure(DataManagerNotebook.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo anuncio en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toNotebook
	 *            Diario a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToNotebook toNotebook) {
        ToNotebookDAO toNotebookDAO = new ToNotebookDAO();
        try {
            toNotebook.setNotebookId(getSequence("sq_to_notebook"));
            EntityManagerHelper.beginTransaction();
            toNotebookDAO.save(toNotebook);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toNotebook);
            log.info("El diario" + toNotebook.getTitle() + " fue creado con �xito...");
            Object[] arrayParam = { toNotebook.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un nuevo diario en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toNotebook
	 *            diario a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToNotebook toNotebook) {
        ToNotebookDAO toNotebookDAO = new ToNotebookDAO();
        try {
            log.info("Actualizando el diario: " + toNotebook.getTitle());
            EntityManagerHelper.beginTransaction();
            toNotebookDAO.update(toNotebook);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toNotebook);
            Object[] args = { toNotebook.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.update.success"), args));
            log.info("Se actualizo el diario con �xito: " + toNotebook.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un diario de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toNotebook
	 *            Diario a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToNotebook toNotebook) {
        try {
            log.info("Eliminando el diario: " + toNotebook.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_NOTEBOOK);
            query.setParameter(1, toNotebook.getNotebookId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toNotebook);
            Object[] arrayParam = { toNotebook.getTitle() };
            log.info("Diario eliminado con �xito: " + toNotebook.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el diario: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toNotebook.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Elimina un conjunto de diarios
	 * 
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult}
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUserId
	 *            c�digo de usuario
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayNotebookId) {
        try {
            log.info("Eliminando DIARIO: " + sArrayNotebookId);
            String sSql = Statements.DELETE_MASIVE_NOTEBOOK;
            sSql = sSql.replaceFirst("v1", sArrayNotebookId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de DIARIO eliminados => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.delete.success"), arrayParam));
        } catch (Exception e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("notebook.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un diario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sNotebookName
	 *            Nombre del diario
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sNotebookName) {
        List<ToNotebook> list = new ToNotebookDAO().findByTitle(sNotebookName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("notebook.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un diario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nNotebookId
	 *            C�digo del diario
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nNotebookId) {
        ToNotebook toNotebook = new ToNotebookDAO().findById(nNotebookId);
        EntityManagerHelper.refresh(toNotebook);
        if (toNotebook == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("notebook.search.notFound"));
        } else {
            List<ToNotebook> list = new ArrayList<ToNotebook>();
            EntityManagerHelper.refresh(toNotebook);
            list.add(toNotebook);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de diarios
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
	 * Obtiene la lista de diarios
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        ToNotebookDAO toNotebookDAO = new ToNotebookDAO();
        List<ToNotebook> list = toNotebookDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("notebook.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toNotebookDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de diarios de un usuario por curso
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes.
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listNotebookForUserCourse(RestServiceResult serviceResult, Long nUserId, Long nCourseId) {
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_TO_NOTEBOOK_USER_COURSE, ToNotebook.class);
        query.setParameter(1, nUserId);
        query.setParameter(2, nCourseId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        List<ToNotebook> list = query.getResultList();
        if (list.size() == 0) {
            serviceResult.setMessage(bundle.getString("notebook.list.notFound"));
            serviceResult.setObjResult(0);
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("notebook.list.success"), array));
            serviceResult.setObjResult(list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }
}
