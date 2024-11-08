package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToDictionary;
import edu.univalle.lingweb.persistence.ToDictionaryDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_Dictionary'( Enlaces )
 * 
 * @author Jose Aricapa
 */
public class DataManagerDictionary extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerDictionary.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerDictionary() {
        super();
        DOMConfigurator.configure(DataManagerNews.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo enlace en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toDictionary
	 *            Enlace a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToDictionary toDictionary) {
        ToDictionaryDAO toDictionaryDAO = new ToDictionaryDAO();
        try {
            toDictionary.setDictionaryId(getSequence("sq_to_dictionary"));
            EntityManagerHelper.beginTransaction();
            toDictionaryDAO.save(toDictionary);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toDictionary);
            log.info("La publicacion " + toDictionary.getTitle() + " fue creada con �xito...");
            Object[] arrayParam = { toDictionary.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel anuncio: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un nuevo enlace en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toDictionary
	 *            anuncio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToDictionary toDictionary) {
        ToDictionaryDAO coNewsDAO = new ToDictionaryDAO();
        try {
            log.info("Actualizando la publicaci�n: " + toDictionary.getTitle());
            EntityManagerHelper.beginTransaction();
            coNewsDAO.update(toDictionary);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toDictionary);
            Object[] args = { toDictionary.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.update.success"), args));
            log.info("Se actualizo la publicaci�n con �xito: " + toDictionary.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una publicacion de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toDictionary
	 *            Publicacion a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToDictionary toDictionary) {
        try {
            log.info("Eliminando la publicacion: " + toDictionary.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_DICTIONARY);
            query.setParameter(1, toDictionary.getDictionaryId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toDictionary);
            Object[] arrayParam = { toDictionary.getTitle() };
            log.info("Publicacion eliminada con �xito: " + toDictionary.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toDictionary.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Elimina un conjunto de publicaciones
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
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayDictionaryId) {
        try {
            log.info("Eliminando ANUNCIOS: " + sArrayDictionaryId);
            String sSql = Statements.DELETE_MASIVE_DICTIONARY;
            sSql = sSql.replaceFirst("v1", sArrayDictionaryId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de publicacaciones eliminadas => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("dictionary.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una publicacion
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sTitle
	 *            Nombre de la publicacion
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sTitle) {
        List<ToDictionary> list = new ToDictionaryDAO().findByTitle(sTitle);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("dictionary.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una publicacion
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nDictionaryId
	 *            C�digo la publicacion
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nDictionaryId) {
        ToDictionary coNews = new ToDictionaryDAO().findById(nDictionaryId);
        if (coNews == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("dictionary.search.notFound"));
        } else {
            List<ToDictionary> list = new ArrayList<ToDictionary>();
            EntityManagerHelper.refresh(coNews);
            list.add(coNews);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de publicaciones
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
	 * Obtiene la lista de publicaciones
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        ToDictionaryDAO toDictionaryDAO = new ToDictionaryDAO();
        List<ToDictionary> list = toDictionaryDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("dictionary.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toDictionaryDAO.findAll().size()); else serviceResult.setNumResult(list.size());
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
    public RestServiceResult listDictionaryForUserCourse(RestServiceResult serviceResult, Long nUserId, Long nCourseId) {
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_TO_DICTIONARY_USER_COURSE, ToDictionary.class);
        query.setParameter(1, nUserId);
        query.setParameter(2, nCourseId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        List<ToDictionary> list = query.getResultList();
        if (list.size() == 0) {
            serviceResult.setMessage(bundle.getString("dictionary.list.notFound"));
            serviceResult.setObjResult(0);
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("dictionary.list.success"), array));
            serviceResult.setObjResult(list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }

    /**
	 * Obtiene la lista de temas
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listDictionaryForCourse(RestServiceResult serviceResult, int nRowStart, int nMaxResults, Long nCourseId) {
        CoCourse dictionary = new CoCourseDAO().findById(nCourseId);
        EntityManagerHelper.refresh(dictionary);
        Set<ToDictionary> set = dictionary.getToDictionaries();
        List<ToDictionary> list = new ArrayList<ToDictionary>(set);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("links.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.list.success"), array));
            serviceResult.setNumResult(list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }
}
