package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoCompleteE3;
import edu.univalle.lingweb.persistence.CoQuestion;
import edu.univalle.lingweb.persistence.CoQuestionDAO;
import edu.univalle.lingweb.persistence.CoWordsCompleteE3;
import edu.univalle.lingweb.persistence.CoWordsCompleteE3DAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_words_complete_e1'( Palabras)
 * 
 * @author Liliana Machuca
 */
public class DataManagerWordsCompleteE3 extends DataManager {

    /** Manejador de mensajes de Log'ss */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerWordsCompleteE3() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva palabra del llenado de espacios en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coMultipleChoiceE1
	 *            Actividad a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoWordsCompleteE3 coWordsCompleteE3) {
        CoWordsCompleteE3DAO coWordsCompleteE3DAO = new CoWordsCompleteE3DAO();
        try {
            coWordsCompleteE3.setWordsCompleteE3Id(getSequence("sq_co_words_complete_e3"));
            EntityManagerHelper.beginTransaction();
            coWordsCompleteE3DAO.save(coWordsCompleteE3);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coWordsCompleteE3);
            log.info("Palabra creada con �xito: " + coWordsCompleteE3.getWord());
            Object[] arrayParam = { coWordsCompleteE3.getWord() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el curso: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza una una nueva palabra en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param comultipleChoice
	 *            Actividad a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, List listWordsComleteE3, Long nCompleteId) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_COMPLETE3_WORD);
            query.setParameter(1, new Long(nCompleteId));
            query.executeUpdate();
            EntityManagerHelper.commit();
            if (listWordsComleteE3 != null) {
                for (int i = 0; i < listWordsComleteE3.size(); i++) {
                    CoWordsCompleteE3 coWordsCompleteE3 = (CoWordsCompleteE3) listWordsComleteE3.get(i);
                    serviceResult = this.create(serviceResult, coWordsCompleteE3);
                }
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar las palabras: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un completar
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nCompleteId
	 *            C�digo del completar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nExerciseId) {
        CoQuestion coQuestion = new CoQuestionDAO().findById(nExerciseId);
        EntityManagerHelper.refresh(coQuestion);
        if (coQuestion == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("complete.search.notFound"));
        } else {
            List<CoCompleteE3> list = new ArrayList<CoCompleteE3>(coQuestion.getCoCompleteE3s());
            if (list.size() == 0) {
                Object[] arrayParam = { coQuestion.getQuestionName() };
                serviceResult.setError(true);
                serviceResult.setMessage(MessageFormat.format(bundle.getString("complete.listMultipleChoiceForExerciseE3.notFound"), arrayParam));
            } else {
                Object[] arrayParam = { list.size(), coQuestion.getQuestionName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("complete.listMultipleChoiceForExerciseE3.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            }
        }
        return serviceResult;
    }
}
