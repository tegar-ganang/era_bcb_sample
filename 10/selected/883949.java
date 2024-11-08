package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToForum;
import edu.univalle.lingweb.persistence.ToForumDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update  Delete) entre otros
 * para la tabla 'to_forum'( Foro )
 * 
 * @author Jose Aricapa
 */
public class DataManagerForum extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerForum.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerForum() {
        super();
        DOMConfigurator.configure(DataManagerForum.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo anuncio en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toorum
	 *            Anuncio a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToForum toForum) {
        ToForumDAO toForumDAO = new ToForumDAO();
        try {
            toForum.setForumId(getSequence("sq_to_forum"));
            EntityManagerHelper.beginTransaction();
            toForumDAO.save(toForum);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toForum);
            log.info("El foro" + toForum.getTitle() + " fue creado con �xito...");
            Object[] arrayParam = { toForum.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel foro: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un nuevo foro en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toForum
	 *            foro a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToForum toForum) {
        ToForumDAO toForumDAO = new ToForumDAO();
        try {
            log.info("Actualizando el foro: " + toForum.getTitle());
            EntityManagerHelper.beginTransaction();
            toForumDAO.update(toForum);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toForum);
            Object[] args = { toForum.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.update.success"), args));
            log.info("Se actualizo el foro con �xito: " + toForum.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el foro: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un foro de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toForum
	 *            Anuncio a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToForum toForum) {
        try {
            log.info("Eliminando el foro: " + toForum.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_FORUM);
            query.setParameter(1, toForum.getForumId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toForum);
            Object[] arrayParam = { toForum.getTitle() };
            log.info("Anuncio eliminado con �xito: " + toForum.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el foro: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toForum.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Elimina un conjunto de foros
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
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayForumId) {
        try {
            log.info("Eliminando FOROS: " + sArrayForumId);
            String sSql = Statements.DELETE_MASIVE_FORUM;
            sSql = sSql.replaceFirst("v1", sArrayForumId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de FOROS eliminados => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el foro: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("forum.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un foro
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sForumName
	 *            Nombre del foro
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sForumName) {
        List<ToForum> list = new ToForumDAO().findByTitle(sForumName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("forum.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un foro
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nForumId
	 *            C�digo del foro
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nForumId) {
        ToForum toForum = new ToForumDAO().findById(nForumId);
        if (toForum == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("forum.search.notFound"));
        } else {
            List<ToForum> list = new ArrayList<ToForum>();
            EntityManagerHelper.refresh(toForum);
            list.add(toForum);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de foros
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
	 * Obtiene la lista de foros
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        ToForumDAO toForumDAO = new ToForumDAO();
        List<ToForum> list = toForumDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("forum.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.list.success"), array));
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toForumDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        Collections.sort(list, new OrdeByForumId());
        serviceResult.setObjResult(list);
        return serviceResult;
    }

    /**
	 * Obtiene la lista de foros
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listForumForCourse(RestServiceResult result, Long nCourseId) {
        return listForumForCourse(result, 0, 0, nCourseId);
    }

    /**
	 * Obtiene la lista de foros
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listForumForCourse(RestServiceResult serviceResult, int nRowStart, int nMaxResults, Long nCourseId) {
        CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
        EntityManagerHelper.refresh(coCourse);
        Set<ToForum> set = coCourse.getToForums();
        List<ToForum> list = new ArrayList<ToForum>(set);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("forum.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("forum.list.success"), array));
            serviceResult.setNumResult(list.size());
        }
        Collections.sort(list, new OrdeByForumId());
        serviceResult.setObjResult(list);
        return serviceResult;
    }
}

/**
 * Clase que permite el ordenamiento por c�digo de publicaci�n de foro
 * @author Jose
 */
class OrdeByForumId implements Comparator<ToForum> {

    public int compare(ToForum forumPost1, ToForum forumPost2) {
        return forumPost1.getForumId().intValue() - forumPost2.getForumId().intValue();
    }
}
