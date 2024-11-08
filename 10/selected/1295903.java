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
import edu.univalle.lingweb.persistence.CoExercises2;
import edu.univalle.lingweb.persistence.CoExercises2DAO;
import edu.univalle.lingweb.persistence.CoMatrixExercises2;
import edu.univalle.lingweb.persistence.CoMatrixExercises2DAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_matrix_exercises2'( Matriz para el ejercicio tipo 2 )
 * 
 * @author Julio Cesar Puentes Delgado
 */
public class DataManagerMatrixExercises2 extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerMatrixExercises2.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerMatrixExercises2() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nueva matriz para el tipo de ejercicio 2 en la base de datos
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
    public RestServiceResult create(RestServiceResult result, CoMatrixExercises2 coMatrixExercises2) {
        CoMatrixExercises2DAO coMatrixExercises2DAO = new CoMatrixExercises2DAO();
        try {
            coMatrixExercises2.setMatrixId(getSequence("sq_co_matrix_exercises2"));
            EntityManagerHelper.beginTransaction();
            coMatrixExercises2DAO.save(coMatrixExercises2);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMatrixExercises2);
            Object[] args = { coMatrixExercises2.getCoExercises2().getExerciseName() };
            result.setMessage(MessageFormat.format(bundle.getString("matrixExercises2.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar la matriz tipo 2: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Realiza la busqueda de una matrix para el ejercicio tipo 2
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el
	 * mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nMatrixId
	 *            c�digo de la matriz para el ejercicio tipo 2
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nMatrixId) {
        CoMatrixExercises2 coMatrixExercises2 = new CoMatrixExercises2DAO().findById(nMatrixId);
        if (coMatrixExercises2 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixExercises2.search.notFound"));
        } else {
            List<CoMatrixExercises2> list = new ArrayList<CoMatrixExercises2>();
            EntityManagerHelper.refresh(coMatrixExercises2);
            list.add(coMatrixExercises2);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises2.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una matrix para el ejercicio tipo 2
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
        CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExerciseId);
        if (coExercises2 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixExercises2.search.notFound"));
        } else {
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_MATRIX_EXERCISES2, CoMatrixExercises2.class);
            query.setParameter(1, nExerciseId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            List<CoMatrixExercises2> list = query.getResultList();
            if (list.size() > 0) {
                Object[] arrayParam = { list.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises2.search.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            } else {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("matrixExercises2.search.notFound"));
            }
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos de una matriz para el ejercicio tipo 2
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMatrixExercises2
	 *            matriz tipo 2 a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoMatrixExercises2 coMatrixExercises2) {
        CoMatrixExercises2DAO coMatrixExercises2DAO = new CoMatrixExercises2DAO();
        String sExerciseName = coMatrixExercises2.getCoExercises2().getExerciseName();
        try {
            log.info("Actualizando la matriz tipo 2: " + sExerciseName);
            EntityManagerHelper.beginTransaction();
            coMatrixExercises2DAO.update(coMatrixExercises2);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMatrixExercises2);
            Object[] arrayParam = { sExerciseName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises2.update.success"), arrayParam));
            log.info("Se actualizo la matriz tipo 2 con �xito: " + sExerciseName);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la matriz tipo 2: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de una matriz para el ejercicio tipo 2
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coMatrixExercises2
	 *            Matriz tipo 2 a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoMatrixExercises2 coMatrixExercises2) {
        String sExerciseName = null;
        try {
            sExerciseName = coMatrixExercises2.getCoExercises2().getExerciseName();
            log.error("Eliminando la matriz tipo 2: " + sExerciseName);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_MATRIX_EXERCISES2);
            query.setParameter(1, coMatrixExercises2.getMatrixId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sExerciseName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises2.delete.success"), arrayParam));
            log.info("Eliminando matriz tipo 2: " + sExerciseName);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la matriz tipo 2: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coMatrixExercises2.getCoExercises2().getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises2.delete.error") + e.getMessage(), args));
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de matrices para el ejercicio tipo 2
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
	 * Obtiene la lista de matrices para el ejercicio tipo 2
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
        CoMatrixExercises2DAO coMatrixExercises2DAO = new CoMatrixExercises2DAO();
        List<CoMatrixExercises2> list = coMatrixExercises2DAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("matrixExercises2.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("matrixExercises2.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coMatrixExercises2DAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de matrices de para el ejercicio tipo 2 por ejercicio
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nExerciseId
	 *            Es el id o c�digo del ejecicio tipo 2.	 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listMatrixExercises2ForExercises2(RestServiceResult serviceResult, Long nExerciseId) {
        CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExerciseId);
        EntityManagerHelper.refresh(coExercises2);
        if (coExercises2 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("matrixExercises2.search.notFound"));
        } else {
            List<CoMatrixExercises2> list = new ArrayList<CoMatrixExercises2>(coExercises2.getCoMatrixExercises2s());
            Object[] arrayParam = { list.size(), coExercises2.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("matrixExercises2.listMatrixExercises2ForExercises2.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }
}
