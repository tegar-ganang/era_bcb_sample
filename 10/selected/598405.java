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
import edu.univalle.lingweb.persistence.CoMatrixQuestion;
import edu.univalle.lingweb.persistence.CoMatrixQuestionDAO;
import edu.univalle.lingweb.persistence.CoQuestion;
import edu.univalle.lingweb.persistence.CoQuestionDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_matrix_question'( Matriz de preguntas )
 * 
 * @author Julio Cesar Puentes Delgado
 */
public class DataManagerMatrixQuestion extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerMatrixQuestion.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerMatrixQuestion() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nueva matriz de preguntas en la base de datos
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
    public RestServiceResult create(RestServiceResult result, CoMatrixQuestion coMatrixQuestion) {
        CoMatrixQuestionDAO coMatrixQuestionDAO = new CoMatrixQuestionDAO();
        try {
            coMatrixQuestion.setMatrixId(getSequence("sq_co_matrix_question"));
            EntityManagerHelper.beginTransaction();
            coMatrixQuestionDAO.save(coMatrixQuestion);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMatrixQuestion);
            Object[] args = { coMatrixQuestion.getCoQuestion().getQuestionName() };
            result.setMessage(MessageFormat.format(bundle.getString("matrixQuestion.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar la matriz de preguntas: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Realiza la busqueda de una matrix de preguntas
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el
	 * mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nMatrixId
	 *            c�digo de la matriz de preguntas
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nMatrixId) {
        CoMatrixQuestion coMatrixQuestion = new CoMatrixQuestionDAO().findById(nMatrixId);
        if (coMatrixQuestion == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixQuestion.search.notFound"));
        } else {
            List<CoMatrixQuestion> list = new ArrayList<CoMatrixQuestion>();
            EntityManagerHelper.refresh(coMatrixQuestion);
            list.add(coMatrixQuestion);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixQuestion.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una matrix para el ejercicio tipo preguntas
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el
	 * mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nExerciseId
	 *            c�digo del ejercicio
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult searchByQuestions(RestServiceResult serviceResult, Long nExerciseId) {
        CoQuestion coQuestion = new CoQuestionDAO().findById(nExerciseId);
        if (coQuestion == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixQuestion.search.notFound"));
        } else {
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_MATRIX_QUESTION, CoMatrixQuestion.class);
            query.setParameter(1, nExerciseId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            List<CoMatrixQuestion> list = query.getResultList();
            if (list.size() > 0) {
                Object[] arrayParam = { list.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixQuestion.search.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            } else {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("matrixQuestion.search.notFound"));
            }
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos de una matriz de preguntas
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMatrixQuestion
	 *            matriz de preguntas a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoMatrixQuestion coMatrixQuestion) {
        CoMatrixQuestionDAO coMatrixQuestionDAO = new CoMatrixQuestionDAO();
        String sQuestionName = coMatrixQuestion.getCoQuestion().getQuestionName();
        try {
            log.info("Actualizando la matriz de preguntas: " + sQuestionName);
            EntityManagerHelper.beginTransaction();
            coMatrixQuestionDAO.update(coMatrixQuestion);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMatrixQuestion);
            Object[] arrayParam = { sQuestionName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixQuestion.update.success"), arrayParam));
            log.info("Se actualizo la matriz de preguntas con �xito: " + sQuestionName);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la matriz de preguntas: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de una matriz de preguntas
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coCourse
	 *            Curso a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoMatrixQuestion coMatrixQuestion) {
        String sQuestionName = null;
        try {
            sQuestionName = coMatrixQuestion.getCoQuestion().getQuestionName();
            log.error("Eliminando la matriz de preguntas: " + sQuestionName);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_MATRIX_QUESTION);
            query.setParameter(1, coMatrixQuestion.getMatrixId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sQuestionName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixQuestion.delete.success"), arrayParam));
            log.info("Eliminando matriz de preguntas: " + sQuestionName);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la matriz de preguntas: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coMatrixQuestion.getEnunciate() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixQuestion.delete.error") + e.getMessage(), args));
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de matrices de preguntas
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
        CoMatrixQuestionDAO coMatrixQuestionDAO = new CoMatrixQuestionDAO();
        List<CoMatrixQuestion> list = coMatrixQuestionDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("matrixQuestion.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("matrixQuestion.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coMatrixQuestionDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de matrices de preguntas por pregunta
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nQuestionId
	 *            Es el id o c�digo de la habilidad.	 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listMatrixQuestionForQuestion(RestServiceResult serviceResult, Long nQuestionId) {
        CoQuestion coQuestion = new CoQuestionDAO().findById(nQuestionId);
        EntityManagerHelper.refresh(coQuestion);
        if (coQuestion == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixQuestion.search.notFound"));
        } else {
            List<CoMatrixQuestion> list = new ArrayList<CoMatrixQuestion>(coQuestion.getCoMatrixQuestions());
            Object[] arrayParam = { list.size(), coQuestion.getQuestionName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixQuestion.listMatrixQuestionForQuestion.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }
}
