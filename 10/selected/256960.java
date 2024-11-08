package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.persistence.CoLanguage;
import edu.univalle.lingweb.persistence.CoLanguageDAO;
import edu.univalle.lingweb.persistence.CoSingleText;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaPostag;
import edu.univalle.lingweb.persistence.MaPostagDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'ma_Postag'( Postag spell Checker)
 * 
 * @author Diana Carolina Rivera
 */
public class DataManagerMaPostag extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerMaPostag.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerMaPostag() {
        super();
        DOMConfigurator.configure(DataManagerMaPostag.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo postag en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maPostag
	 *            Postag a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, MaPostag maPostag) {
        MaPostagDAO maPostagDAO = new MaPostagDAO();
        try {
            maPostag.setPostagId(getSequence("sq_ma_postag"));
            EntityManagerHelper.beginTransaction();
            maPostagDAO.save(maPostag);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maPostag);
            log.info("Postag creado con �xito: " + maPostag.getTag());
            Object[] arrayParam = { maPostag.getTag() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el postag: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un Postag en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maPostag
	 *            Postag a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, MaPostag maPostag) {
        MaPostagDAO maPostagDAO = new MaPostagDAO();
        try {
            EntityManagerHelper.beginTransaction();
            maPostagDAO.update(maPostag);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maPostag);
            Object[] args = { maPostag.getTag() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.update.success"), args));
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el postag: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maPostag
	 *            Postag a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, MaPostag maPostag) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_MA_POSTAG);
            query.setParameter(1, maPostag.getPostagId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { maPostag.getTag() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el postag: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maPostag.getTag() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un postag
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sPostag
	 *            Tag del Postag
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sPostag) {
        List<MaPostag> list = new MaPostagDAO().findByTag(sPostag);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("postag.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un Postag por Id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nPostagId
	 *            C�digo del Postag
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nPostagId) {
        MaPostag maPostag = new MaPostagDAO().findById(nPostagId);
        if (maPostag == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("postag.search.notFound"));
        } else {
            List<MaPostag> list = new ArrayList<MaPostag>();
            EntityManagerHelper.refresh(maPostag);
            list.add(maPostag);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la lista de un Postag por Idioma
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nPostagId
	 *            C�digo del Postag
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listByLanguage(RestServiceResult serviceResult, Long nLangugeId) {
        List<MaPostag> listMaPostag = null;
        log.info("nLangugeId: " + nLangugeId);
        Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_MA_POSTAG, MaPostag.class);
        query.setParameter(1, nLangugeId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listMaPostag = query.getResultList();
        if (listMaPostag == null || listMaPostag.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("postag.search.notFound"));
        } else {
            Object[] arrayParam = { listMaPostag.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.search.success"), arrayParam));
            serviceResult.setObjResult(listMaPostag);
            serviceResult.setNumResult(listMaPostag.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de Postags
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de Postags
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        MaPostagDAO maPostagDAO = new MaPostagDAO();
        List<MaPostag> list = maPostagDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("postag.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("postag.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(maPostagDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    public static void main(String[] args) {
    }
}
