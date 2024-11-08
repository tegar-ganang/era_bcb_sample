package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoCompleteE1;
import edu.univalle.lingweb.persistence.CoExercises1;
import edu.univalle.lingweb.persistence.CoExercises1DAO;
import edu.univalle.lingweb.persistence.CoWordsCompleteE1;
import edu.univalle.lingweb.persistence.CoWordsCompleteE1DAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_words_complete_e1'( Palabras)
 * 
 * @author Liliana Machuca
 */
public class DataManagerWordsCompleteE1 extends DataManager {

    /** Manejador de mensajes de Log'ss */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerWordsCompleteE1() {
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
    public RestServiceResult create(RestServiceResult serviceResult, CoWordsCompleteE1 coWordsCompleteE1) {
        CoWordsCompleteE1DAO coWordsCompleteE1DAO = new CoWordsCompleteE1DAO();
        try {
            coWordsCompleteE1.setWordsCompleteE1Id(getSequence("sq_co_words_complete_e1"));
            EntityManagerHelper.beginTransaction();
            coWordsCompleteE1DAO.save(coWordsCompleteE1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coWordsCompleteE1);
            log.info("Palabra creada con �xito: " + coWordsCompleteE1.getWord());
            Object[] arrayParam = { coWordsCompleteE1.getWord() };
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
    public RestServiceResult update(RestServiceResult serviceResult, List listWordsComleteE2, Long nCompleteId) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_COMPLETE1_WORD);
            query.setParameter(1, new Long(nCompleteId));
            query.executeUpdate();
            EntityManagerHelper.commit();
            if (listWordsComleteE2 != null) {
                for (int i = 0; i < listWordsComleteE2.size(); i++) {
                    CoWordsCompleteE1 coWordsCompleteE1 = (CoWordsCompleteE1) listWordsComleteE2.get(i);
                    serviceResult = this.create(serviceResult, coWordsCompleteE1);
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
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
        EntityManagerHelper.refresh(coExercises1);
        if (coExercises1 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("complete.search.notFound"));
        } else {
            List<CoCompleteE1> list = new ArrayList<CoCompleteE1>(coExercises1.getCoCompleteE1s());
            if (list.size() == 0) {
                Object[] arrayParam = { coExercises1.getExerciseName() };
                serviceResult.setError(true);
                serviceResult.setMessage(MessageFormat.format(bundle.getString("complete.listMultipleChoiceForExerciseE3.notFound"), arrayParam));
            } else {
                Object[] arrayParam = { list.size(), coExercises1.getExerciseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("complete.listMultipleChoiceForExerciseE3.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            }
        }
        return serviceResult;
    }
}
