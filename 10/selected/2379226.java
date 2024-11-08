package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToNews;
import edu.univalle.lingweb.persistence.ToNewsDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_news'( Anuncios )
 * 
 * @author Jose Aricapa
 */
public class DataManagerNews extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerNews.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerNews() {
        super();
        DOMConfigurator.configure(DataManagerNews.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo anuncio en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toNews
	 *            Anuncio a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToNews toNews) {
        ToNewsDAO toNewsDAO = new ToNewsDAO();
        try {
            toNews.setNewsId(getSequence("sq_to_news"));
            EntityManagerHelper.beginTransaction();
            toNewsDAO.save(toNews);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toNews);
            log.info("El anuncio" + toNews.getTitle() + " fue creado con �xito...");
            Object[] arrayParam = { toNews.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel anuncio: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un nuevo anuncio en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toNews
	 *            anuncio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToNews toNews) {
        ToNewsDAO coNewsDAO = new ToNewsDAO();
        try {
            log.info("Actualizando el anuncio: " + toNews.getTitle());
            EntityManagerHelper.beginTransaction();
            coNewsDAO.update(toNews);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toNews);
            Object[] args = { toNews.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.update.success"), args));
            log.info("Se actualizo el anuncio con �xito: " + toNews.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el anuncio: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un anuncio de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toNews
	 *            Anuncio a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToNews toNews) {
        try {
            log.info("Eliminando el anuncio: " + toNews.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_NEWS);
            query.setParameter(1, toNews.getNewsId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toNews);
            Object[] arrayParam = { toNews.getTitle() };
            log.info("Anuncio eliminado con �xito: " + toNews.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el anuncio: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toNews.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Elimina un conjunto de anuncios
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
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayNewsId) {
        try {
            log.info("Eliminando ANUNCIOS: " + sArrayNewsId);
            String sSql = Statements.DELETE_MASIVE_NEWS;
            sSql = sSql.replaceFirst("v1", sArrayNewsId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de ANUNCIOS eliminados => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el anuncio: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("news.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un anuncio
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sNewsName
	 *            Nombre del anuncio
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sNewsName) {
        List<ToNews> list = new ToNewsDAO().findByTitle(sNewsName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("news.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un anuncio
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nNewsId
	 *            C�digo del anuncio
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nNewsId) {
        ToNews coNews = new ToNewsDAO().findById(nNewsId);
        if (coNews == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("news.search.notFound"));
        } else {
            List<ToNews> list = new ArrayList<ToNews>();
            EntityManagerHelper.refresh(coNews);
            list.add(coNews);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de anuncios
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
	 * Obtiene la lista de anuncios
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        ToNewsDAO toNewsDAO = new ToNewsDAO();
        List<ToNews> list = toNewsDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("news.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toNewsDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de anuncios
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listNewsForCourse(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de anuncios
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listNewsForCourse(RestServiceResult serviceResult, Long nCourseId) {
        CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
        EntityManagerHelper.refresh(coCourse);
        List<ToNews> list = new ArrayList<ToNews>();
        list.addAll(coCourse.getToNewses());
        if (list.size() == 0) {
            serviceResult.setMessage(bundle.getString("news.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("news.list.success"), array));
        }
        serviceResult.setObjResult(list);
        serviceResult.setNumResult(list.size());
        return serviceResult;
    }
}
