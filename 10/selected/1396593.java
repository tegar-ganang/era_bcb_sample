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
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToThemes;
import edu.univalle.lingweb.persistence.ToThemesDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update  Delete) entre otros
 * para la tabla 'to_theme'( Temas )
 * 
 * @author Jose Aricapa
 */
public class DataManagerThemes extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerThemes.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerThemes() {
        super();
        DOMConfigurator.configure(DataManagerThemes.class.getResource("/log4j.xml"));
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
    public RestServiceResult create(RestServiceResult serviceResult, ToThemes toThemes) {
        ToThemesDAO toThemesDAO = new ToThemesDAO();
        try {
            toThemes.setThemeId(getSequence("sq_to_theme"));
            EntityManagerHelper.beginTransaction();
            toThemesDAO.save(toThemes);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toThemes);
            log.info("El tema" + toThemes.getTitle() + " fue creado con �xito...");
            Object[] arrayParam = { toThemes.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardarel tema: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un nuevo tema en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toThemes
	 *            tema a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToThemes toThemes) {
        ToThemesDAO toThemesDAO = new ToThemesDAO();
        try {
            log.info("Actualizando el tema: " + toThemes.getTitle());
            EntityManagerHelper.beginTransaction();
            toThemesDAO.update(toThemes);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toThemes);
            Object[] args = { toThemes.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.update.success"), args));
            log.info("Se actualizo el tema con �xito: " + toThemes.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el tema: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un tema de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param toThemes
	 *            Anuncio a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToThemes toThemes) {
        try {
            log.info("Eliminando el tema: " + toThemes.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_FORUM);
            query.setParameter(1, toThemes.getThemeId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toThemes);
            Object[] arrayParam = { toThemes.getTitle() };
            log.info("Anuncio eliminado con �xito: " + toThemes.getTitle());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el tema: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toThemes.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Elimina un conjunto de temas
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
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayThemesId) {
        try {
            log.info("Eliminando TEMAS: " + sArrayThemesId);
            String sSql = Statements.DELETE_MASIVE_THEME;
            sSql = sSql.replaceFirst("v1", sArrayThemesId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" N�mero de Temas eliminados => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el tema: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("theme.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un tema
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sThemesName
	 *            Nombre del tema
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sThemesName) {
        List<ToThemes> list = new ToThemesDAO().findByTitle(sThemesName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("theme.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un tema
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nThemesId
	 *            C�digo del tema
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nThemesId) {
        ToThemes toThemes = new ToThemesDAO().findById(nThemesId);
        if (toThemes == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("theme.search.notFound"));
        } else {
            List<ToThemes> list = new ArrayList<ToThemes>();
            EntityManagerHelper.refresh(toThemes);
            list.add(toThemes);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.search.success"), arrayParam));
            serviceResult.setObjResult(list);
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
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
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
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        ToThemesDAO toThemesDAO = new ToThemesDAO();
        List<ToThemes> list = toThemesDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("theme.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toThemesDAO.findAll().size()); else serviceResult.setNumResult(list.size());
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
    public RestServiceResult listThemesForCourse(RestServiceResult result, Long nCourse) {
        return listThemesForCourse(result, 0, 0, nCourse);
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
    public RestServiceResult listThemesForCourse(RestServiceResult serviceResult, int nRowStart, int nMaxResults, Long nCourse) {
        CoCourse coCourse = new CoCourseDAO().findById(nCourse);
        EntityManagerHelper.refresh(coCourse);
        Set<ToThemes> set = coCourse.getToThemeses();
        List<ToThemes> list = new ArrayList<ToThemes>(set);
        if (list.size() == 0) {
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("theme.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("theme.list.success"), array));
            serviceResult.setNumResult(list.size());
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }
}

/**
 * Clase que permite el ordenamiento por c�digo de temas
 * @author Jose
 */
class OrdeByThemeId implements Comparator<ToThemes> {

    public int compare(ToThemes themes1, ToThemes themes2) {
        return themes1.getThemeId().intValue() - themes2.getThemeId().intValue();
    }
}
