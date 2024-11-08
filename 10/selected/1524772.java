package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.persistence.CoSingleTextCheckList;
import edu.univalle.lingweb.persistence.CoSingleTextCheckListDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'MaSingleTextCheckList'( check list para el single text )
 * 
 * @author Diana Carolina Rivera Velasco
 */
public class DataManagerCheckListStudent extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerCheckListStudent() {
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
    public RestServiceResult create(RestServiceResult result, CoSingleTextCheckList coSingleTextCheckList) {
        CoSingleTextCheckListDAO coSingleTextCheckListDAO = new CoSingleTextCheckListDAO();
        log.info("estoy en el create del data manager");
        try {
            coSingleTextCheckList.setCheckListId(getSequence("sq_co_single_text_check_1"));
            EntityManagerHelper.beginTransaction();
            coSingleTextCheckListDAO.save(coSingleTextCheckList);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coSingleTextCheckList);
            log.info("Pregunta de check list creada con �xito: " + coSingleTextCheckList.getCheckListId());
            Object[] args = { coSingleTextCheckList.getCheckListId(), coSingleTextCheckList.getTitle() };
            result.setMessage(MessageFormat.format(bundle.getString("checkListStudent.create.success"), args));
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
    public RestServiceResult search(RestServiceResult serviceResult, Long nSingleTextId) {
        List<CoSingleTextCheckList> listCoSingleTextCheckList = null;
        Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_CO_SINGLE_TEXT_CHECK_LIST, CoSingleTextCheckList.class);
        query.setParameter(1, nSingleTextId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listCoSingleTextCheckList = query.getResultList();
        if (listCoSingleTextCheckList == null || listCoSingleTextCheckList.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("checkListStudent.search.notFound"));
        } else {
            Object[] arrayParam = { listCoSingleTextCheckList.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkListStudent.search.success"), arrayParam));
            serviceResult.setObjResult(listCoSingleTextCheckList);
            serviceResult.setNumResult(listCoSingleTextCheckList.size());
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
    public RestServiceResult update(RestServiceResult serviceResult, CoSingleTextCheckList coSingleTextCheckList) {
        CoSingleTextCheckListDAO coSingleTextCheckListDAO = new CoSingleTextCheckListDAO();
        try {
            log.info("Actualizando la t�cnica: " + coSingleTextCheckList.getTitle());
            EntityManagerHelper.beginTransaction();
            coSingleTextCheckListDAO.update(coSingleTextCheckList);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coSingleTextCheckList);
            Object[] arrayParam = { coSingleTextCheckList.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkListStudent.update.success"), arrayParam));
            log.info("Se actualizo la t�cnica con �xito: " + coSingleTextCheckList.getTitle());
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
    public RestServiceResult delete(RestServiceResult serviceResult, CoSingleTextCheckList coSingleTextCheckList) {
        String sTitle = null;
        try {
            sTitle = coSingleTextCheckList.getTitle();
            log.error("Eliminando la lista de chequeo: " + coSingleTextCheckList.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.DELETE_CHECK_LIST_STUDENT);
            query.setParameter(1, coSingleTextCheckList.getCheckListId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sTitle };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkListStudent.delete.success"), arrayParam));
            log.info("Eliminando el curso: " + coSingleTextCheckList.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el curso: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coSingleTextCheckList.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkListStudent.delete.error") + e.getMessage(), args));
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
        CoSingleTextCheckListDAO coSingleTextCheckListDAO = new CoSingleTextCheckListDAO();
        List<CoSingleTextCheckList> list = coSingleTextCheckListDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("checkListStudent.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("checkListStudent.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coSingleTextCheckListDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }
}
