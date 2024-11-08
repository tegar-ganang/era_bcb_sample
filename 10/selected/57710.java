package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToPublication;
import edu.univalle.lingweb.persistence.ToPublicationDAO;
import edu.univalle.lingweb.persistence.ToThemes;
import edu.univalle.lingweb.persistence.ToThemesDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_publication'( Publicaciones )
 * 
 * @author Jose Aricapa
 */
public class DataManagerPublication extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerNews.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerPublication() {
        super();
        DOMConfigurator.configure(DataManagerNews.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva publicacion en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toPublication
	 *            Publicacion a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToPublication toPublication) {
        ToPublicationDAO toPublicationDAO = new ToPublicationDAO();
        try {
            toPublication.setPublicationId(getSequence("sq_to_publication"));
            EntityManagerHelper.beginTransaction();
            toPublicationDAO.save(toPublication);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toPublication);
            log.info("La publicacion" + toPublication.getTitle() + " fue creada con �xito...");
            Object[] arrayParam = { toPublication.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel anuncio: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de relacionar un material con una unidad
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public RestServiceResult createPublicationMaterial(RestServiceResult serviceResult, String sArrayMaterialId, ToPublication coPublication) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_PUBLICATION_MATERIAL);
            query.setParameter(1, coPublication.getPublicationId());
            query.executeUpdate();
            StringTokenizer stringTokenizer = new StringTokenizer(sArrayMaterialId, ",");
            while (stringTokenizer.hasMoreTokens()) {
                long nMaterialId = Long.parseLong(stringTokenizer.nextToken());
                query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_PUBLICATION_MATERIAL);
                query.setParameter(1, coPublication.getPublicationId());
                query.setParameter(2, nMaterialId);
                query.executeUpdate();
            }
            EntityManagerHelper.commit();
            Object[] arrayParam = { coPublication.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.create.success"), arrayParam));
        } catch (PersistenceException e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Publicacion - Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Actualiza una nueva publicacion en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toPublication
	 *            anuncio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToPublication toPublication) {
        ToPublicationDAO coNewsDAO = new ToPublicationDAO();
        try {
            log.info("Actualizando la publicaci�n: " + toPublication.getTitle());
            EntityManagerHelper.beginTransaction();
            coNewsDAO.update(toPublication);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toPublication);
            Object[] args = { toPublication.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.update.success"), args));
            log.info("Se actualizo la publicaci�n con �xito: " + toPublication.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una publicacion de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toPublication
	 *            Publicacion a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToPublication toPublication) {
        try {
            log.info("Eliminando la publicacion: " + toPublication.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_PUBLICATION);
            query.setParameter(1, toPublication.getPublicationId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toPublication);
            Object[] arrayParam = { toPublication.getTitle() };
            log.info("Publicacion eliminada con �xito: " + toPublication.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toPublication.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.delete.error") + e.getMessage(), arrayParam));
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
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayPublicationId) {
        try {
            log.info("Eliminando ANUNCIOS: " + sArrayPublicationId);
            String sSql = Statements.DELETE_MASIVE_PUBLICATION;
            sSql = sSql.replaceFirst("v1", sArrayPublicationId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de publicacaciones eliminadas => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la publicacion: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("publication.delete.error") + e.getMessage());
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
        List<ToPublication> list = new ToPublicationDAO().findByTitle(sTitle);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("publication.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.search.success"), arrayParam));
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
	 * @param nPublicationId
	 *            C�digo la publicacion
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nPublicationId) {
        ToPublication coNews = new ToPublicationDAO().findById(nPublicationId);
        if (coNews == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("publication.search.notFound"));
        } else {
            List<ToPublication> list = new ArrayList<ToPublication>();
            EntityManagerHelper.refresh(coNews);
            list.add(coNews);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.search.success"), arrayParam));
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
        ToPublicationDAO toPublicationDAO = new ToPublicationDAO();
        List<ToPublication> list = toPublicationDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("publication.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toPublicationDAO.findAll().size()); else serviceResult.setNumResult(list.size());
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
    public RestServiceResult listPublicationForCourse(RestServiceResult result, Long nThemeId) {
        return listPublicationForTheme(result, 0, 0, nThemeId);
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
    public RestServiceResult listPublicationForTheme(RestServiceResult serviceResult, int nRowStart, int nMaxResults, Long nThemeId) {
        ToThemes publications = new ToThemesDAO().findById(nThemeId);
        EntityManagerHelper.refresh(publications);
        Set<ToPublication> set = publications.getToPublications();
        List<ToPublication> list = new ArrayList<ToPublication>(set);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("publication.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("publication.list.success"), array));
            serviceResult.setNumResult(list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }
}

/**
 * Clase que permite el ordenamiento por c�digo de publicacion
 * @author Jose
 */
class OrdeByPublicationId implements Comparator<ToPublication> {

    public int compare(ToPublication publications1, ToPublication publications2) {
        return publications1.getPublicationId().intValue() - publications2.getPublicationId().intValue();
    }
}
