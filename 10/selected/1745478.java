package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToComment;
import edu.univalle.lingweb.persistence.ToCommentDAO;
import edu.univalle.lingweb.persistence.ToDictionary;
import edu.univalle.lingweb.persistence.ToDictionaryDAO;
import edu.univalle.lingweb.persistence.ToGlossary;
import edu.univalle.lingweb.persistence.ToGlossaryDAO;
import edu.univalle.lingweb.persistence.ToLinks;
import edu.univalle.lingweb.persistence.ToLinksDAO;
import edu.univalle.lingweb.persistence.ToPublication;
import edu.univalle.lingweb.persistence.ToPublicationDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_comment'( Publicaciones )
 * 
 * @author Jose Aricapa
 */
public class DataManagerComment extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerNews.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerComment() {
        super();
        DOMConfigurator.configure(DataManagerNews.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva publicacion en la base de datos
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n el
	 *            resultado de la operaci�n.
	 * @param toComment Publicacion a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToComment toComment) {
        ToCommentDAO toCommentDAO = new ToCommentDAO();
        try {
            toComment.setCommentId(getSequence("sq_to_comment"));
            EntityManagerHelper.beginTransaction();
            toCommentDAO.save(toComment);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toComment);
            log.info("El comentario " + toComment.getCommentId() + " fue creada con �xito...");
            Object[] arrayParam = { toComment.getCommentId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel anuncio: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de relacionar un material con una unidad
	 * 
	 * @param result El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMaterial men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public RestServiceResult createPublicationComment(RestServiceResult serviceResult, String sPublicationId, ToComment toComment) {
        try {
            Query query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_PUBLICATION_COMMENT);
            query.setParameter(1, toComment.getCommentId());
            query.setParameter(2, new Long(sPublicationId));
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { toComment.getCommentId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.success"), arrayParam));
        } catch (PersistenceException e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Publicacion - Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Actualiza una nueva publicacion en la base de datos
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n el
	 *            resultado de la operaci�n.
	 * @param toComment anuncio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToComment toComment) {
        ToCommentDAO coNewsDAO = new ToCommentDAO();
        try {
            log.info("Actualizando la publicaci�n: " + toComment.getCommentId());
            EntityManagerHelper.beginTransaction();
            coNewsDAO.update(toComment);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toComment);
            Object[] args = { toComment.getCommentId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.update.success"), args));
            log.info("Se actualizo la publicaci�n con �xito: " + toComment.getCommentId());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una publicacion de la base de datos
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n el
	 *            resultado de la operaci�n.
	 * @param toComment Publicacion a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToComment toComment) {
        try {
            log.info("Eliminando la publicacion: " + toComment.getCommentId());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_COMMENT);
            query.setParameter(1, toComment.getCommentId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toComment);
            Object[] arrayParam = { toComment.getCommentId() };
            log.info("Publicacion eliminada con �xito: " + toComment.getCommentId());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toComment.getCommentId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Elimina un conjunto de publicaciones
	 * 
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult}
	 * 
	 * @param result El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUserId c�digo de usuario
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayCommentId) {
        try {
            log.info("Eliminando ANUNCIOS: " + sArrayCommentId);
            String sSql = Statements.DELETE_MASIVE_NEWS;
            sSql = sSql.replaceFirst("v1", sArrayCommentId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de publicacaciones eliminadas => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("comment.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una publicacion
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n el
	 *            resultado de la operaci�n.
	 * @param sCommentId Nombre de la publicacion
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sCommentId) {
        List<ToComment> list = new ToCommentDAO().findByComment(sCommentId);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("comment.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una publicacion
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n el
	 *            resultado de la operaci�n.
	 * @param nCommentId C�digo la publicacion
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nCommentId) {
        ToComment coNews = new ToCommentDAO().findById(nCommentId);
        if (coNews == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("comment.search.notFound"));
        } else {
            List<ToComment> list = new ArrayList<ToComment>();
            EntityManagerHelper.refresh(coNews);
            list.add(coNews);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de publicaciones
	 * 
	 * @param result El {@link RestServiceResult} que contendr�n los mensajes
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
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los
	 *            mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        ToCommentDAO toCommentDAO = new ToCommentDAO();
        List<ToComment> list = toCommentDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("comment.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toCommentDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de temas
	 * 
	 * @param result El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listCommentForCourse(RestServiceResult result, Long nThemeId) {
        return listCommentForPublication(result, nThemeId);
    }

    /**
	 * Obtiene la lista de temas
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los
	 *            mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listCommentForPublication(RestServiceResult serviceResult, Long nPublicationId) {
        ToPublication toPublication = new ToPublicationDAO().findById(nPublicationId);
        EntityManagerHelper.refresh(toPublication);
        Set<ToComment> set = toPublication.getToComments();
        List<ToComment> list = new ArrayList<ToComment>(set);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("comment.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.list.success"), array));
            serviceResult.setNumResult(list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }

    /**
	 * Obtiene la lista de comenttarios por enlaces
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los
	 *            mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listCommentForLinks(RestServiceResult serviceResult, Long nLinksId) {
        ToLinks toLinks = new ToLinksDAO().findById(nLinksId);
        EntityManagerHelper.refresh(toLinks);
        Set<ToComment> set = toLinks.getToComments();
        List<ToComment> list = new ArrayList<ToComment>(set);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("comment.list.notFound"));
            log.info("Resultado es cero");
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.list.success"), array));
            serviceResult.setNumResult(list.size());
            log.info("Resultado es diferente de cero " + list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }

    /**
	 * Realiza el proceso de relacionar el comentario con el enlace
	 * 
	 * @param result El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMaterial men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public RestServiceResult createToLinksComment(RestServiceResult serviceResult, String sLinksId, ToComment toComment) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.INSERT_TO_LINKS_COMMENT);
            query.setParameter(1, toComment.getCommentId());
            query.setParameter(2, new Long(sLinksId));
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { toComment.getCommentId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.success"), arrayParam));
        } catch (PersistenceException e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Enlace - Comentario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de relacionar el comentario con el enlace
	 * 
	 * @param result El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMaterial men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public RestServiceResult createToGlossaryComment(RestServiceResult serviceResult, String sGlossaryId, ToComment toComment) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.INSERT_TO_GLOSSARY_COMMENT);
            query.setParameter(1, toComment.getCommentId());
            query.setParameter(2, new Long(sGlossaryId));
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { toComment.getCommentId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.success"), arrayParam));
        } catch (PersistenceException e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Enlace - Comentario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de comenttarios por enlaces
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los
	 *            mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listCommentForDictionary(RestServiceResult serviceResult, Long nDictionaryId) {
        ToDictionary toDictionary = new ToDictionaryDAO().findById(nDictionaryId);
        EntityManagerHelper.refresh(toDictionary);
        Set<ToComment> set = toDictionary.getToComments();
        List<ToComment> list = new ArrayList<ToComment>(set);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("comment.list.notFound"));
            log.info("Resultado es cero");
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.list.success"), array));
            serviceResult.setNumResult(list.size());
            log.info("Resultado es diferente de cero " + list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }

    /**
	 * Obtiene la lista de comenttarios por enlaces
	 * 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los
	 *            mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listCommentForGlossary(RestServiceResult serviceResult, Long nGlossaryId) {
        ToGlossary toGlossary = new ToGlossaryDAO().findById(nGlossaryId);
        EntityManagerHelper.refresh(toGlossary);
        Set<ToComment> set = toGlossary.getToComments();
        List<ToComment> list = new ArrayList<ToComment>(set);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("comment.list.notFound"));
            log.info("Resultado es cero");
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.list.success"), array));
            serviceResult.setNumResult(list.size());
            log.info("Resultado es diferente de cero " + list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }

    /**
	 * Realiza el proceso de relacionar el comentario con el enlace
	 * 
	 * @param result El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMaterial men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public RestServiceResult createToDictionaryComment(RestServiceResult serviceResult, String sDictionaryId, ToComment toComment) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.INSERT_TO_DICTIONARY_COMMENT);
            query.setParameter(1, toComment.getCommentId());
            query.setParameter(2, new Long(sDictionaryId));
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { toComment.getCommentId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.success"), arrayParam));
        } catch (PersistenceException e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Enlace - Comentario: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("comment.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }
}

/**
 * Clase que permite el ordenamiento por c�digo de publicacion
 * 
 * @author Jose
 */
class OrdeByCommentId implements Comparator<ToComment> {

    public int compare(ToComment comments1, ToComment comments2) {
        return comments1.getCommentId().intValue() - comments2.getCommentId().intValue();
    }
}
