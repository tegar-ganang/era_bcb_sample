package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaSingleTextCheckList;
import edu.univalle.lingweb.persistence.MaSingleTextCheckListDAO;
import edu.univalle.lingweb.persistence.MaSingleTextForm;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'MaSingleTextCheckList'( check list para el single text )
 * 
 * @author Diana Carolina Rivera Velasco
 */
public class DataManagerCheckList extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerCheckList() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nueva t�cnica en la base de datos
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, MaSingleTextCheckList maSingleTextCheckList) {
        MaSingleTextCheckListDAO maSingleTextCheckListDAO = new MaSingleTextCheckListDAO();
        log.info("estoy en el create del data manager");
        try {
            maSingleTextCheckList.setCheckListFormId(getSequence("sq_ma_single_text_check_form_1"));
            EntityManagerHelper.beginTransaction();
            maSingleTextCheckListDAO.save(maSingleTextCheckList);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSingleTextCheckList);
            log.info("Pregunta de check list creada con �xito: " + maSingleTextCheckList.getCheckListFormId());
            Object[] args = { maSingleTextCheckList.getCheckListFormId(), maSingleTextCheckList.getTitle() };
            result.setMessage(MessageFormat.format(bundle.getString("checkList.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar la lista de chequeo: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Realiza la busqueda de una t�cnica
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el
	 * mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nCourseId
	 *            c�digo del programa del curso
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nCheckListFormId) {
        MaSingleTextCheckList maSingleTextCheckList = new MaSingleTextCheckListDAO().findById(nCheckListFormId);
        if (maSingleTextCheckList == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("checkList.search.notFound"));
        } else {
            List<MaSingleTextCheckList> list = new ArrayList<MaSingleTextCheckList>();
            EntityManagerHelper.refresh(maSingleTextCheckList);
            list.add(maSingleTextCheckList);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkList.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos de una t�cnica
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coCourse
	 *            curso a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, MaSingleTextCheckList maSingleTextCheckList) {
        MaSingleTextCheckListDAO maSingleTextCheckListDAO = new MaSingleTextCheckListDAO();
        try {
            log.info("Actualizando la t�cnica: " + maSingleTextCheckList.getTitle());
            EntityManagerHelper.beginTransaction();
            maSingleTextCheckListDAO.update(maSingleTextCheckList);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSingleTextCheckList);
            Object[] arrayParam = { maSingleTextCheckList.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkList.update.success"), arrayParam));
            log.info("Se actualizo la t�cnica con �xito: " + maSingleTextCheckList.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la t�cnica: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de una t�cnica
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coCourse
	 *            Curso a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, MaSingleTextCheckList maSingleTextCheckList) {
        String sTitle = null;
        try {
            sTitle = maSingleTextCheckList.getTitle();
            log.error("Eliminando la lista de chequeo: " + maSingleTextCheckList.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.DELETE_CHECK_LIST);
            query.setParameter(1, maSingleTextCheckList.getCheckListFormId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sTitle };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkList.delete.success"), arrayParam));
            log.info("Eliminando el curso: " + maSingleTextCheckList.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el curso: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { maSingleTextCheckList.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkList.delete.error") + e.getMessage(), args));
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de tecnicas
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
	 * Obtiene la lista de tecnicas
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la
	 *            consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result, int nRowStart, int nMaxResults) {
        MaSingleTextCheckListDAO maSingleTextCheckListDAO = new MaSingleTextCheckListDAO();
        List<MaSingleTextCheckList> list = maSingleTextCheckListDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("checkList.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("checkList.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(maSingleTextCheckListDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    public RestServiceResult listByLanguagueId(RestServiceResult serviceResult, Long nLanguagueId) {
        List<MaSingleTextCheckList> listMaSingleTextCheckList = null;
        Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_MA_CHECK_LIST_FORM_LANG, MaSingleTextCheckList.class);
        query.setParameter(1, nLanguagueId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listMaSingleTextCheckList = query.getResultList();
        if (listMaSingleTextCheckList == null || listMaSingleTextCheckList.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("checkList.list.notFound"));
        } else {
            Object[] arrayParam = { listMaSingleTextCheckList.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkList.list.success"), arrayParam));
            serviceResult.setObjResult(listMaSingleTextCheckList);
            serviceResult.setNumResult(listMaSingleTextCheckList.size());
        }
        return serviceResult;
    }
}
