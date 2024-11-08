package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToLinks;
import edu.univalle.lingweb.persistence.ToLinksDAO;
import edu.univalle.lingweb.persistence.ToThemes;
import edu.univalle.lingweb.persistence.ToThemesDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_Links'( Enlaces )
 * 
 * @author Jose Aricapa
 */
public class DataManagerLinks extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerLinks.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerLinks() {
        super();
        DOMConfigurator.configure(DataManagerNews.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo enlace en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toLinks
	 *            Enlace a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToLinks toLinks) {
        ToLinksDAO toLinksDAO = new ToLinksDAO();
        try {
            toLinks.setLinksId(getSequence("sq_to_links"));
            EntityManagerHelper.beginTransaction();
            toLinksDAO.save(toLinks);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toLinks);
            log.info("La publicacion " + toLinks.getTitle() + " fue creada con �xito...");
            Object[] arrayParam = { toLinks.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel anuncio: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un nuevo enlace en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toLinks
	 *            anuncio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToLinks toLinks) {
        ToLinksDAO coNewsDAO = new ToLinksDAO();
        try {
            log.info("Actualizando la publicaci�n: " + toLinks.getTitle());
            EntityManagerHelper.beginTransaction();
            coNewsDAO.update(toLinks);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toLinks);
            Object[] args = { toLinks.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.update.success"), args));
            log.info("Se actualizo la publicaci�n con �xito: " + toLinks.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una publicacion de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toLinks
	 *            Publicacion a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToLinks toLinks) {
        try {
            log.info("Eliminando la publicacion: " + toLinks.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_LINKS);
            query.setParameter(1, toLinks.getLinksId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toLinks);
            Object[] arrayParam = { toLinks.getTitle() };
            log.info("Publicacion eliminada con �xito: " + toLinks.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toLinks.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.delete.error") + e.getMessage(), arrayParam));
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
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayLinksId) {
        try {
            log.info("Eliminando ANUNCIOS: " + sArrayLinksId);
            String sSql = Statements.DELETE_MASIVE_LINKS;
            sSql = sSql.replaceFirst("v1", sArrayLinksId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de publicacaciones eliminadas => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("links.delete.error") + e.getMessage());
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
        List<ToLinks> list = new ToLinksDAO().findByTitle(sTitle);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("links.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.search.success"), arrayParam));
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
	 * @param nLinksId
	 *            C�digo la publicacion
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nLinksId) {
        ToLinks coNews = new ToLinksDAO().findById(nLinksId);
        if (coNews == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("links.search.notFound"));
        } else {
            List<ToLinks> list = new ArrayList<ToLinks>();
            EntityManagerHelper.refresh(coNews);
            list.add(coNews);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.search.success"), arrayParam));
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
        ToLinksDAO toLinksDAO = new ToLinksDAO();
        List<ToLinks> list = toLinksDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("links.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("links.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toLinksDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de temas
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listLinksForCourse(RestServiceResult result, Long nThemeId) {
        return listLinksForTheme(result, 0, 0, nThemeId);
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
    public RestServiceResult listLinksForTheme(RestServiceResult serviceResult, int nRowStart, int nMaxResults, Long nThemeId) {
        ToThemes links = new ToThemesDAO().findById(nThemeId);
        EntityManagerHelper.refresh(links);
        Set<ToLinks> set = links.getToLinkses();
        List<ToLinks> list = new ArrayList<ToLinks>(set);
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
