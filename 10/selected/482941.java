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
import edu.univalle.lingweb.persistence.ToHandbook;
import edu.univalle.lingweb.persistence.ToHandbookDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_handbook'( Libreta )
 * 
 * @author Liliana Machuca
 */
public class DataManagerHandbook extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerHandbook.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerHandbook() {
        super();
        DOMConfigurator.configure(DataManagerHandbook.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo anuncio en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toHandbook
	 *            Diario a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToHandbook toHandbook) {
        ToHandbookDAO toHandbookDAO = new ToHandbookDAO();
        try {
            toHandbook.setHandbookId(getSequence("sq_to_handbook"));
            EntityManagerHelper.beginTransaction();
            toHandbookDAO.save(toHandbook);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toHandbook);
            log.info("El diario" + toHandbook.getTitle() + " fue creado con �xito...");
            Object[] arrayParam = { toHandbook.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un nuevo diario en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toHandbook
	 *            diario a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToHandbook toHandbook) {
        ToHandbookDAO toHandbookDAO = new ToHandbookDAO();
        try {
            log.info("Actualizando el diario: " + toHandbook.getTitle());
            EntityManagerHelper.beginTransaction();
            toHandbookDAO.update(toHandbook);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toHandbook);
            Object[] args = { toHandbook.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.update.success"), args));
            log.info("Se actualizo el diario con �xito: " + toHandbook.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un diario de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toHandbook
	 *            Diario a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToHandbook toHandbook) {
        try {
            log.info("Eliminando anotaci�n libreta: " + toHandbook.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_HANDBOOK);
            query.setParameter(1, toHandbook.getHandbookId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toHandbook);
            Object[] arrayParam = { toHandbook.getTitle() };
            log.info("Libreta eliminado con �xito: " + toHandbook.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el Libreta: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toHandbook.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.delete.error") + e.getMessage(), arrayParam));
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
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayHandbookId) {
        try {
            log.info("Eliminando DIARIO: " + sArrayHandbookId);
            String sSql = Statements.DELETE_MASIVE_HANDBOOK;
            sSql = sSql.replaceFirst("v1", sArrayHandbookId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de ANOTACI�N eliminados => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.delete.success"), arrayParam));
        } catch (Exception e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("handbook.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una anotacion de una libreta
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sHandbookName
	 *            Nombre de la anotaci�n
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sHandbookName) {
        List<ToHandbook> list = new ToHandbookDAO().findByTitle(sHandbookName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("handbook.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una anotaci�n de una libreta
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nHandbookId
	 *            C�digo del diario
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nHandbookId) {
        ToHandbook toHandbook = new ToHandbookDAO().findById(nHandbookId);
        EntityManagerHelper.refresh(toHandbook);
        if (toHandbook == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("handbook.search.notFound"));
        } else {
            List<ToHandbook> list = new ArrayList<ToHandbook>();
            EntityManagerHelper.refresh(toHandbook);
            list.add(toHandbook);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.search.success"), arrayParam));
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
	 * Obtiene la lista de anotaciones de una libreta
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        ToHandbookDAO toHandbookDAO = new ToHandbookDAO();
        List<ToHandbook> list = toHandbookDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("handbook.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toHandbookDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de anotaciones de un usuario 
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes.
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listHandbookForUser(RestServiceResult serviceResult, Long nUserId) {
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_TO_HANDBOOK_USER, ToHandbook.class);
        query.setParameter(1, nUserId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        List<ToHandbook> list = query.getResultList();
        if (list.size() == 0) {
            serviceResult.setMessage(bundle.getString("handbook.list.notFound"));
            serviceResult.setObjResult(0);
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("handbook.list.success"), array));
            serviceResult.setObjResult(list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }
}
