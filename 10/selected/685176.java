package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoCompleteE2;
import edu.univalle.lingweb.persistence.CoExercises2;
import edu.univalle.lingweb.persistence.CoExercises2DAO;
import edu.univalle.lingweb.persistence.CoWordsCompleteE2;
import edu.univalle.lingweb.persistence.CoWordsCompleteE2DAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_words_complete_e2'( Palabras)
 * 
 * @author Liliana Machuca
 */
public class DataManagerWordsCompleteE2 extends DataManager {

    /** Manejador de mensajes de Log'ss */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerWordsCompleteE2() {
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
    public RestServiceResult create(RestServiceResult serviceResult, CoWordsCompleteE2 coWordsCompleteE2) {
        CoWordsCompleteE2DAO coWordsCompleteE2DAO = new CoWordsCompleteE2DAO();
        try {
            coWordsCompleteE2.setWordsCompleteE2Id(getSequence("sq_co_words_complete_e2"));
            EntityManagerHelper.beginTransaction();
            coWordsCompleteE2DAO.save(coWordsCompleteE2);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coWordsCompleteE2);
            log.info("Palabra creada con �xito: " + coWordsCompleteE2.getWord());
            Object[] arrayParam = { coWordsCompleteE2.getWord() };
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
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_COMPLETE2_WORD);
            query.setParameter(1, new Long(nCompleteId));
            query.executeUpdate();
            EntityManagerHelper.commit();
            if (listWordsComleteE2 != null) {
                for (int i = 0; i < listWordsComleteE2.size(); i++) {
                    CoWordsCompleteE2 coWordsCompleteE2 = (CoWordsCompleteE2) listWordsComleteE2.get(i);
                    serviceResult = this.create(serviceResult, coWordsCompleteE2);
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
        CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExerciseId);
        EntityManagerHelper.refresh(coExercises2);
        if (coExercises2 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("complete.search.notFound"));
        } else {
            List<CoCompleteE2> list = new ArrayList<CoCompleteE2>(coExercises2.getCoCompleteE2s());
            if (list.size() == 0) {
                Object[] arrayParam = { coExercises2.getExerciseName() };
                serviceResult.setError(true);
                serviceResult.setMessage(MessageFormat.format(bundle.getString("complete.listMultipleChoiceForExerciseE3.notFound"), arrayParam));
            } else {
                Object[] arrayParam = { list.size(), coExercises2.getExerciseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("complete.listMultipleChoiceForExerciseE3.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            }
        }
        return serviceResult;
    }
}
