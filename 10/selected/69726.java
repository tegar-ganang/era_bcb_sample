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
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaSpellError;
import edu.univalle.lingweb.persistence.MaSpellErrorDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'ma_spell_error'( Postag spell Checker)
 * 
 * @author Diana Carolina Rivera
 */
public class DataManagerMaSpellError extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerMaSpellError.class);

    /**
	 * Constructor de la clase
	 */
    public DataManagerMaSpellError() {
        super();
        DOMConfigurator.configure(DataManagerMaSpellError.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva error sintactico en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSpellError
	 *            Error a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, MaSpellError maSpellError) {
        MaSpellErrorDAO maSpellErrorDAO = new MaSpellErrorDAO();
        try {
            maSpellError.setErrorId(getSequence("sq_ma_spell_error"));
            EntityManagerHelper.beginTransaction();
            maSpellErrorDAO.save(maSpellError);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSpellError);
            log.info("Error sintatico creado con �xito: " + maSpellError.getWrongWord());
            Object[] arrayParam = { maSpellError.getWrongWord() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el error: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un Error (SpellError) en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSpellError
	 *            Error a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, MaSpellError maSpellError) {
        MaSpellErrorDAO maSpellErrorDAO = new MaSpellErrorDAO();
        try {
            EntityManagerHelper.beginTransaction();
            maSpellErrorDAO.update(maSpellError);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSpellError);
            Object[] args = { maSpellError.getWrongWord() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.update.success"), args));
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el error: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSpellError
	 *            Error a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, MaSpellError maSpellError) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_MA_SPELL_ERROR);
            query.setParameter(1, maSpellError.getErrorId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { maSpellError.getWrongWord() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el error: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maSpellError.getWrongWord() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un Error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sWrongWord
	 *            WrongWord del SpellError
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sWrongWord) {
        List<MaSpellError> list = new MaSpellErrorDAO().findByWrongWord(sWrongWord);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("spellerror.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un Error por Id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nErrorId
	 *            C�digo del Error
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nErrorId) {
        MaSpellError maSpellError = new MaSpellErrorDAO().findById(nErrorId);
        if (maSpellError == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("spellerror.search.notFound"));
        } else {
            List<MaSpellError> list = new ArrayList<MaSpellError>();
            EntityManagerHelper.refresh(maSpellError);
            list.add(maSpellError);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de Errores
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de Errores
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        MaSpellErrorDAO maSpellErrorDAO = new MaSpellErrorDAO();
        List<MaSpellError> list = maSpellErrorDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("spellerror.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spellerror.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(maSpellErrorDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    public static void main(String[] args) {
    }
}
