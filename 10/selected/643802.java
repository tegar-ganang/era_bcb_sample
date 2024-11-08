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
import edu.univalle.lingweb.persistence.ToDaily;
import edu.univalle.lingweb.persistence.ToDailyDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_daily'( Diario )
 * 
 * @author Jose Aricapa
 */
public class DataManagerDaily extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerDaily.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerDaily() {
        super();
        DOMConfigurator.configure(DataManagerDaily.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo anuncio en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toDaily
	 *            Diario a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToDaily toDaily) {
        ToDailyDAO toDailyDAO = new ToDailyDAO();
        try {
            toDaily.setDailyId(getSequence("sq_to_daily"));
            EntityManagerHelper.beginTransaction();
            toDailyDAO.save(toDaily);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toDaily);
            log.info("El diario" + toDaily.getTitle() + " fue creado con �xito...");
            Object[] arrayParam = { toDaily.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un nuevo diario en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toDaily
	 *            diario a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToDaily toDaily) {
        ToDailyDAO toDailyDAO = new ToDailyDAO();
        try {
            log.info("Actualizando el diario: " + toDaily.getTitle());
            EntityManagerHelper.beginTransaction();
            toDailyDAO.update(toDaily);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toDaily);
            Object[] args = { toDaily.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.update.success"), args));
            log.info("Se actualizo el diario con �xito: " + toDaily.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un diario de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toDaily
	 *            Diario a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToDaily toDaily) {
        try {
            log.info("Eliminando el diario: " + toDaily.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_DAILY);
            query.setParameter(1, toDaily.getDailyId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toDaily);
            Object[] arrayParam = { toDaily.getTitle() };
            log.info("Diario eliminado con �xito: " + toDaily.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el diario: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toDaily.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.delete.error") + e.getMessage(), arrayParam));
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
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayDailyId) {
        try {
            log.info("Eliminando DIARIO: " + sArrayDailyId);
            String sSql = Statements.DELETE_MASIVE_NEWS;
            sSql = sSql.replaceFirst("v1", sArrayDailyId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de DIARIO eliminados => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.delete.success"), arrayParam));
        } catch (Exception e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el diario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("daily.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un diario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sDailyName
	 *            Nombre del diario
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sDailyName) {
        List<ToDaily> list = new ToDailyDAO().findByTitle(sDailyName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("daily.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.search.success"), arrayParam));
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
	 * @param nDailyId
	 *            C�digo del diario
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nDailyId) {
        ToDaily toDaily = new ToDailyDAO().findById(nDailyId);
        EntityManagerHelper.refresh(toDaily);
        if (toDaily == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("daily.search.notFound"));
        } else {
            List<ToDaily> list = new ArrayList<ToDaily>();
            EntityManagerHelper.refresh(toDaily);
            list.add(toDaily);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.search.success"), arrayParam));
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
        ToDailyDAO toDailyDAO = new ToDailyDAO();
        List<ToDaily> list = toDailyDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("daily.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toDailyDAO.findAll().size()); else serviceResult.setNumResult(list.size());
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
    public RestServiceResult listDailyForUserCourse(RestServiceResult serviceResult, Long nUserId, Long nCourseId) {
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_TO_DAILY_USER_COURSE, ToDaily.class);
        query.setParameter(1, nUserId);
        query.setParameter(2, nCourseId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        List<ToDaily> list = query.getResultList();
        if (list.size() == 0) {
            serviceResult.setMessage(bundle.getString("daily.list.notFound"));
            serviceResult.setObjResult(0);
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("daily.list.success"), array));
            serviceResult.setObjResult(list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }
}
