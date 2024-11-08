package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
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
import edu.univalle.lingweb.persistence.CoCompleteE3;
import edu.univalle.lingweb.persistence.CoQuestion;
import edu.univalle.lingweb.persistence.CoQuestionDAO;
import edu.univalle.lingweb.persistence.CoScoreQuestion;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.OpenResponse3;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_question'( Pregutas para la secuencia de evaluaci�n )
 * 
 * @author Julio Cesar Puentes
 */
public class DataManagerQuestion extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerQuestion() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva pregunta en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coQuestion
	 *            Pregunta a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoQuestion coQuestion) {
        CoQuestionDAO coQuestionDAO = new CoQuestionDAO();
        try {
            Long nSequence = getSequence("sq_co_question");
            coQuestion.setQuestionId(nSequence);
            EntityManagerHelper.beginTransaction();
            coQuestionDAO.save(coQuestion);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coQuestion);
            log.info("Pregunta creada con �xito: " + coQuestion.getQuestionName());
            Object[] arrayParam = { coQuestion.getQuestionName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.create.success"), arrayParam));
            serviceResult.setId(nSequence);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la pregunta: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza una pregunta en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coQuestion
	 *            Pregunta a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoQuestion coQuestion) {
        CoQuestionDAO coQuestionDAO = new CoQuestionDAO();
        try {
            EntityManagerHelper.beginTransaction();
            coQuestionDAO.update(coQuestion);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coQuestion);
            Object[] args = { coQuestion.getQuestionName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.update.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la pregunta: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una pregunta
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coQuestion
	 *            Pregunta a Eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoQuestion coQuestion) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_QUESTION);
            query.setParameter(1, coQuestion.getQuestionId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coQuestion);
            Object[] arrayParam = { coQuestion.getQuestionName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la pregunta: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coQuestion.getQuestionName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una pregunta  por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sQuestionName
	 *            Nombre de la �pregunta
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sQuestionName) {
        List<CoQuestion> list = new CoQuestionDAO().findByQuestionName(sQuestionName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("question.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una pregunta por id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nExerciseId
	 *            C�digo del ejercicio
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nQuestionId) {
        CoQuestion coQuestion = new CoQuestionDAO().findById(nQuestionId);
        if (coQuestion == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("question.search.notFound"));
        } else {
            List<CoQuestion> list = new ArrayList<CoQuestion>();
            EntityManagerHelper.refresh(coQuestion);
            list.add(coQuestion);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de preguntas
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
	 * Obtiene la lista de preguntas en un rango determinado
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoQuestionDAO coQuestionDAO = new CoQuestionDAO();
        List<CoQuestion> list = coQuestionDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("question.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coQuestionDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de relacionar un material con una pregunta
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public RestServiceResult createQuestionMaterial(RestServiceResult serviceResult, String sArrayMaterialId, CoQuestion coQuestion) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_QUESTION_MATERIAL);
            query.setParameter(1, coQuestion.getQuestionId());
            query.executeUpdate();
            StringTokenizer stringTokenizer = new StringTokenizer(sArrayMaterialId, ",");
            while (stringTokenizer.hasMoreTokens()) {
                long nMaterialId = Long.parseLong(stringTokenizer.nextToken());
                query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_QUESTION_MATERIAL);
                query.setParameter(1, coQuestion.getQuestionId());
                query.setParameter(2, nMaterialId);
                query.executeUpdate();
            }
            EntityManagerHelper.commit();
            Object[] arrayParam = { coQuestion.getQuestionName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Pregunta- Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("question.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Valida si una pregunta ya calificada esta en el valor critico o en el valor execelente para visualizar pregunta complementaria
	 * @param coQuestion pregunta a validar
	 * @param nUserId usuario estudiante
	 * @return boolean true si se debe habilitar, false de lo contrario.
	 */
    public boolean validateRangeQuestionScored(CoQuestion coQuestion, Long nUserId) {
        List<CoScoreQuestion> listCoScoreQuestion = new ArrayList<CoScoreQuestion>();
        CoScoreQuestion coScoreQuestion = null;
        Float nScore = new Float(3.0);
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_SCORE_EXERCISES2, CoScoreQuestion.class);
        query.setParameter(1, coQuestion.getQuestionId());
        query.setParameter(2, nUserId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listCoScoreQuestion = query.getResultList();
        for (int i = 0; i < listCoScoreQuestion.size(); i++) {
            coScoreQuestion = (CoScoreQuestion) listCoScoreQuestion.get(i);
            nScore = new Float(coScoreQuestion.getScore());
        }
        if (coScoreQuestion == null) {
            return false;
        } else if (nScore < Common.CRITIC_VALUE_QUANTITATIVE || nScore >= Common.EXELLENT_VALUE_QUANTITATIVE) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Obtiene la lista de ejercicios clonables
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult result) {
        return listClone(result, 0, 0);
    }

    /**
	 * Obtiene la lista de ejercicio clonable
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoQuestionDAO coQuestionDAO = new CoQuestionDAO();
        List<CoQuestion> list = coQuestionDAO.findByFlagClone("1", nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(false);
            serviceResult.setMessage(bundle.getString("activity.listClone.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.listClone.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) {
                RestServiceResult serviceResult2 = listClone(new RestServiceResult());
                int nNumStudent = serviceResult2.getNumResult();
                serviceResult.setNumResult(nNumStudent);
            } else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * permite clonar el contenido de un ejercicio
	 * @param nQuestionOldId C�digo de la pregunta vieja
	 * @param nQuestionNewId C�digo de la pregunta nueva
	 */
    public void cloneQuestion(long nQuestionOldId, long nQuestionNewId) {
        EntityManagerHelper.beginTransaction();
        CoQuestion coQuestionOld = new CoQuestionDAO().findById(nQuestionOldId);
        Set<OpenResponse3> setOpenResponse = coQuestionOld.getOpenResponse3s();
        log.info("Ejercicio[" + nQuestionOldId + "] Cantidad de respuestas abiertas a clonar: " + setOpenResponse.size());
        Query query = null;
        if (setOpenResponse.size() == 1) {
            CoQuestion coQuestion = new CoQuestionDAO().findById(nQuestionNewId);
            EntityManagerHelper.refresh(coQuestion);
            if (coQuestion != null) {
                if (coQuestion.getOpenResponse3s().size() == 0) {
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_OPEN_RESPONSE3);
                    query.setParameter(1, nQuestionNewId);
                    query.setParameter(2, nQuestionOldId);
                    int nUpdate = query.executeUpdate();
                    log.info("\n\nClonaci�n OPEN-RESPONSE2-[" + nQuestionOldId + "] - NEWEXERCISES_1[" + nQuestionNewId + "]" + " - RESPONSE2  => " + nUpdate);
                } else {
                    log.error("El ejercicio2[" + nQuestionOldId + "] ya tiene clonado la respuesta abierta ");
                }
            } else {
                log.error("El ejercicio2[" + nQuestionNewId + "] no existe...");
            }
        } else {
            log.error("El ejercicio2[" + nQuestionOldId + "] tiene " + setOpenResponse.size() + " respuestas abiertas !!...");
        }
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_MULTIPLE_CHOISE_3);
        query.setParameter(1, nQuestionNewId);
        query.setParameter(2, nQuestionOldId);
        int nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n MULTIPLE_CHOICE_E3-Question[" + nQuestionOldId + "] - NEWQUESTION[" + nQuestionNewId + "]" + " - MultipleChoise " + nUpdate);
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_SINGLE_TEXT_TEACHER_3);
        query.setParameter(1, nQuestionNewId);
        query.setParameter(2, nQuestionOldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n CO_SINGLE_TEXT_TEACHER3-Question[" + nQuestionOldId + "] - NEWQUESTION[" + nQuestionNewId + "]" + " - SINGLETEXTTEACHER " + nUpdate);
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_MATRIX_QUESTION);
        query.setParameter(1, nQuestionNewId);
        query.setParameter(2, nQuestionOldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n CO_MATRIX_QUESTION-Question[" + nQuestionOldId + "] - NEWQUESTION[" + nQuestionNewId + "]" + " - MATRIX " + nUpdate);
        log.info("\n\nCOMPLETE....");
        for (Iterator<CoCompleteE3> iterator2 = coQuestionOld.getCoCompleteE3s().iterator(); iterator2.hasNext(); ) {
            CoCompleteE3 coCompleteE3Old = iterator2.next();
            Long nCompleteE3Id = getSequence("sq_co_complete_e3");
            query = EntityManagerHelper.createNativeQuery(Statements.CLONE_COMPLETE_E3);
            query.setParameter(1, nCompleteE3Id);
            query.setParameter(2, nQuestionNewId);
            query.setParameter(3, nQuestionOldId);
            nUpdate = query.executeUpdate();
            log.info("Clonaci�n co_complete_e3-Question[" + nQuestionOldId + "] - NEWQUESTION[" + nQuestionNewId + "]" + " - complete_e3 " + nUpdate);
            query = EntityManagerHelper.createNativeQuery(Statements.CLONE_WORDS_COMPLETE_E3);
            query.setParameter(1, nCompleteE3Id);
            query.setParameter(2, coCompleteE3Old.getCompleteE3Id());
            nUpdate = query.executeUpdate();
            log.info("Clonaci�n CLONE_WORDS_COMPLETE_E3-Question[" + nCompleteE3Id + "] - NEW_COMPLETE_E3[" + coCompleteE3Old.getCompleteE3Id() + "]" + " - WORDS_COMPLETE_E3 " + nUpdate);
        }
        EntityManagerHelper.commit();
    }
}
