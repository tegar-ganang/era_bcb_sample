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
import edu.univalle.lingweb.persistence.CoExercises1;
import edu.univalle.lingweb.persistence.CoExercises1DAO;
import edu.univalle.lingweb.persistence.CoMatrixExercises1;
import edu.univalle.lingweb.persistence.CoMatrixExercises1DAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_matrix_exercises1'( Matriz para el ejercicio tipo 1 )
 * 
 * @author Julio Cesar Puentes Delgado
 */
public class DataManagerMatrixExercises1 extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerMatrixExercises1.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerMatrixExercises1() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nueva matriz para el tipo de ejercicio 1 en la base de datos
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
    public RestServiceResult create(RestServiceResult result, CoMatrixExercises1 coMatrixExercises1) {
        CoMatrixExercises1DAO coMatrixExercises1DAO = new CoMatrixExercises1DAO();
        try {
            coMatrixExercises1.setMatrixId(getSequence("sq_co_matrix_exercises1"));
            EntityManagerHelper.beginTransaction();
            coMatrixExercises1DAO.save(coMatrixExercises1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMatrixExercises1);
            Object[] args = { coMatrixExercises1.getCoExercises1().getExerciseName() };
            result.setMessage(MessageFormat.format(bundle.getString("matrixExercises1.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar la matriz tipo 1: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Realiza la busqueda de una matrix para el ejercicio tipo 1
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el
	 * mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nMatrixId
	 *            c�digo de la matriz para el ejercicio tipo 1
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nMatrixId) {
        CoMatrixExercises1 coMatrixExercises1 = new CoMatrixExercises1DAO().findById(nMatrixId);
        if (coMatrixExercises1 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixExercises1.search.notFound"));
        } else {
            List<CoMatrixExercises1> list = new ArrayList<CoMatrixExercises1>();
            EntityManagerHelper.refresh(coMatrixExercises1);
            list.add(coMatrixExercises1);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises1.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una matrix para el ejercicio tipo 1
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
    public RestServiceResult searchByExercises(RestServiceResult serviceResult, Long nExerciseId) {
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
        if (coExercises1 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixExercises1.search.notFound"));
        } else {
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_MATRIX_EXERCISES1, CoMatrixExercises1.class);
            query.setParameter(1, nExerciseId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            List<CoMatrixExercises1> list = query.getResultList();
            if (list.size() > 0) {
                Object[] arrayParam = { list.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises1.search.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            } else {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("matrixExercises1.search.notFound"));
            }
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos de una matriz para el ejercicio tipo 1
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMatrixExercises1
	 *            matriz tipo 1 a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoMatrixExercises1 coMatrixExercises1) {
        CoMatrixExercises1DAO coMatrixExercises1DAO = new CoMatrixExercises1DAO();
        try {
            log.info("Actualizando la matriz tipo 1: " + coMatrixExercises1.getEnunciate());
            EntityManagerHelper.beginTransaction();
            coMatrixExercises1DAO.update(coMatrixExercises1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMatrixExercises1);
            Object[] arrayParam = { coMatrixExercises1.getCoExercises1().getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises1.update.success"), arrayParam));
            log.info("Se actualizo la matriz tipo1 con �xito: " + coMatrixExercises1.getEnunciate());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la matriz tipo 1: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de una matriz para el ejercicio tipo 1
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coMatrixExercises1
	 *            Matriz tipo 1 eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoMatrixExercises1 coMatrixExercises1) {
        String sExerciseName = null;
        try {
            sExerciseName = coMatrixExercises1.getCoExercises1().getExerciseName();
            log.error("Eliminando la matriz tipo 1: " + sExerciseName);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_MATRIX_EXERCISES1);
            query.setParameter(1, coMatrixExercises1.getMatrixId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sExerciseName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises1.delete.success"), arrayParam));
            log.info("Eliminando matriz tipo 1: " + sExerciseName);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la matriz tipo 1: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coMatrixExercises1.getCoExercises1().getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises1.delete.error") + e.getMessage(), args));
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de matrices para el ejercicio tipo 1
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
	 * Obtiene la lista de matrices para el ejercicio tipo 1
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
        CoMatrixExercises1DAO coMatrixExercises1DAO = new CoMatrixExercises1DAO();
        List<CoMatrixExercises1> list = coMatrixExercises1DAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("matrixExercises1.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("matrixExercises1.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coMatrixExercises1DAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de matrices de para el ejercicio tipo 1 por ejercicio
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nQuestionId
	 *            Es el id o c�digo de la habilidad.	 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listMatrixExercises1ForExercises1(RestServiceResult serviceResult, Long nExerciseId) {
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
        EntityManagerHelper.refresh(coExercises1);
        if (coExercises1 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixExercises1.search.notFound"));
        } else {
            List<CoMatrixExercises1> list = new ArrayList<CoMatrixExercises1>(coExercises1.getCoMatrixExercises1s());
            Object[] arrayParam = { list.size(), coExercises1.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises1.listMatrixExercises1ForExercises1.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }
}
