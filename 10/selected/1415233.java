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
import edu.univalle.lingweb.persistence.MaSyntaticError;
import edu.univalle.lingweb.persistence.MaSyntaticErrorDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'ma_syntatic_error'( Postag spell Checker)
 * 
 * @author Diana Carolina Rivera
 */
public class DataManagerMaSyntacticError extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerMaSyntactic.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerMaSyntacticError() {
        super();
        DOMConfigurator.configure(DataManagerMaSyntacticError.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo postag en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSyntaticError
	 *            Error sintactico a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, MaSyntaticError maSyntaticError) {
        MaSyntaticErrorDAO maSyntaticErrorDAO = new MaSyntaticErrorDAO();
        try {
            maSyntaticError.setErrorId(getSequence("sq_ma_syntatic_error"));
            EntityManagerHelper.beginTransaction();
            maSyntaticErrorDAO.save(maSyntaticError);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSyntaticError);
            log.info("Error sintactico creado con �xito: " + maSyntaticError.getWrongSentence());
            Object[] arrayParam = { maSyntaticError.getWrongSentence() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el error sintactico: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un Error sintactico en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSyntacticError
	 *            Error sintactico a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, MaSyntaticError maSyntaticError) {
        MaSyntaticErrorDAO maSyntaticErrorDAO = new MaSyntaticErrorDAO();
        try {
            EntityManagerHelper.beginTransaction();
            maSyntaticErrorDAO.update(maSyntaticError);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSyntaticError);
            Object[] args = { maSyntaticError.getWrongSentence() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.update.success"), args));
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el error sintactico: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un error sintactico
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSyntaticError
	 *            Error sintactico a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, MaSyntaticError maSyntaticError) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_MA_SYNTACTIC_ERROR);
            query.setParameter(1, maSyntaticError.getErrorId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { maSyntaticError.getWrongSentence() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el error sintactico: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maSyntaticError.getWrongSentence() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un error sintactico
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sWrongSentence
	 *            Oracion erronea del MaSyntaticError
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sWrongSentence) {
        List<MaSyntaticError> list = new MaSyntaticErrorDAO().findByWrongSentence(sWrongSentence);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("syntacticerror.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un error sintactico por Id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nErrorId
	 *            C�digo del error sintactico
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nErrorId) {
        MaSyntaticError maSyntaticError = new MaSyntaticErrorDAO().findById(nErrorId);
        if (maSyntaticError == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("syntacticerror.search.notFound"));
        } else {
            List<MaSyntaticError> list = new ArrayList<MaSyntaticError>();
            EntityManagerHelper.refresh(maSyntaticError);
            list.add(maSyntaticError);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de errores sintacticos
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de errores sintacticos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        MaSyntaticErrorDAO maSyntaticErrorDAO = new MaSyntaticErrorDAO();
        List<MaSyntaticError> list = maSyntaticErrorDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("syntacticerror.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntacticerror.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(maSyntaticErrorDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    public static void main(String[] args) {
    }
}
