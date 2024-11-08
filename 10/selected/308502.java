package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.persistence.CoParagraphCheckList;
import edu.univalle.lingweb.persistence.CoParagraphCheckListDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'MaParagraphCheckList'( check list para el single text )
 * 
 * @author Diana Carolina Rivera Velasco
 */
public class DataManagerParagraphCheckListStudent extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerParagraphCheckListStudent() {
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
    public RestServiceResult create(RestServiceResult result, CoParagraphCheckList coParagraphCheckList) {
        CoParagraphCheckListDAO coParagraphCheckListDAO = new CoParagraphCheckListDAO();
        log.info("estoy en el create del data manager");
        try {
            coParagraphCheckList.setCheckListId(getSequence("sq_co_paragraph_check_1"));
            EntityManagerHelper.beginTransaction();
            coParagraphCheckListDAO.save(coParagraphCheckList);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coParagraphCheckList);
            log.info("Pregunta de check list creada con �xito: " + coParagraphCheckList.getCheckListId());
            Object[] args = { coParagraphCheckList.getCheckListId(), coParagraphCheckList.getTitle() };
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
    public RestServiceResult search(RestServiceResult serviceResult, Long nParagraphId) {
        List<CoParagraphCheckList> listCoParagraphCheckList = null;
        Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_CO_PARAGRAPH_CHECK_LIST, CoParagraphCheckList.class);
        query.setParameter(1, nParagraphId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listCoParagraphCheckList = query.getResultList();
        if (listCoParagraphCheckList == null || listCoParagraphCheckList.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("checkListStudent.search.notFound"));
        } else {
            Object[] arrayParam = { listCoParagraphCheckList.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkListStudent.search.success"), arrayParam));
            serviceResult.setObjResult(listCoParagraphCheckList);
            serviceResult.setNumResult(listCoParagraphCheckList.size());
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
    public RestServiceResult update(RestServiceResult serviceResult, CoParagraphCheckList coParagraphCheckList) {
        CoParagraphCheckListDAO coParagraphCheckListDAO = new CoParagraphCheckListDAO();
        try {
            log.info("Actualizando la t�cnica: " + coParagraphCheckList.getTitle());
            EntityManagerHelper.beginTransaction();
            coParagraphCheckListDAO.update(coParagraphCheckList);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coParagraphCheckList);
            Object[] arrayParam = { coParagraphCheckList.getTitle() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkListStudent.update.success"), arrayParam));
            log.info("Se actualizo la t�cnica con �xito: " + coParagraphCheckList.getTitle());
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
    public RestServiceResult delete(RestServiceResult serviceResult, CoParagraphCheckList coParagraphCheckList) {
        String sTitle = null;
        try {
            sTitle = coParagraphCheckList.getTitle();
            log.error("Eliminando la lista de chequeo: " + coParagraphCheckList.getTitle());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.DELETE_CHECK_LIST_STUDENT);
            query.setParameter(1, coParagraphCheckList.getCheckListId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sTitle };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("checkListStudent.delete.success"), arrayParam));
            log.info("Eliminando el curso: " + coParagraphCheckList.getTitle());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el curso: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coParagraphCheckList.getTitle() };
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
        CoParagraphCheckListDAO coParagraphCheckListDAO = new CoParagraphCheckListDAO();
        List<CoParagraphCheckList> list = coParagraphCheckListDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("checkListStudent.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("checkListStudent.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coParagraphCheckListDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }
}
