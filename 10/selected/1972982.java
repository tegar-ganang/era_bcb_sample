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
import edu.univalle.lingweb.persistence.CoMultipleChoiceE1;
import edu.univalle.lingweb.persistence.CoMultipleChoiceE1DAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_activity'( Actividades)
 * 
 * @author Jose Aricapa
 */
public class DataManagerMultipleChoiceE1 extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerMultipleChoiceE1() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva pregunta de selecci�n en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coMultipleChoiceE1
	 *            Actividad a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoMultipleChoiceE1 coMultipleChoiceE1) {
        CoMultipleChoiceE1DAO coMultipleChoiceE1DAO = new CoMultipleChoiceE1DAO();
        try {
            coMultipleChoiceE1.setMultipleChoiceE1Id(getSequence("sq_co_multiple_choice_e1"));
            EntityManagerHelper.beginTransaction();
            coMultipleChoiceE1DAO.save(coMultipleChoiceE1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMultipleChoiceE1);
            log.info("Pregunta de selecci�n creada con �xito: " + coMultipleChoiceE1.getMultipleChoiceName());
            Object[] arrayParam = { coMultipleChoiceE1.getMultipleChoiceName() };
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
	 * Actualiza una una nueva actividad en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param comultipleChoice
	 *            Actividad a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoMultipleChoiceE1 coMultipleChoiceE1) {
        CoMultipleChoiceE1DAO coMultipleChoiceE1DAO = new CoMultipleChoiceE1DAO();
        try {
            EntityManagerHelper.beginTransaction();
            coMultipleChoiceE1DAO.update(coMultipleChoiceE1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMultipleChoiceE1);
            Object[] args = { coMultipleChoiceE1.getMultipleChoiceName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.update.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el curso: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param comultipleChoice
	 *            Actividad a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoMultipleChoiceE1 coMultipleChoiceE1) {
        String sMultipleChoiceE1Name = null;
        try {
            sMultipleChoiceE1Name = coMultipleChoiceE1.getMultipleChoiceName();
            log.error("Eliminando la pregunta de seleccion: " + coMultipleChoiceE1.getMultipleChoiceName());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_MULTIPLE_CHOICE_E1);
            query.setParameter(1, coMultipleChoiceE1.getMultipleChoiceE1Id());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMultipleChoiceE1);
            Object[] arrayParam = { sMultipleChoiceE1Name };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.delete.success"), arrayParam));
            log.info("Eliminando la pregunta de seleccion: " + coMultipleChoiceE1.getMultipleChoiceName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la pregunta de seleccion: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coMultipleChoiceE1.getMultipleChoiceName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param smultipleChoiceName
	 *            Nombre de la actividad actividad
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sMultipleChoiceName) {
        List<CoMultipleChoiceE1> list = new CoMultipleChoiceE1DAO().findByMultipleChoiceName(sMultipleChoiceName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("multipleChoice.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nmultipleChoiceId
	 *            C�digo de la actividad
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nMultipleChoiceE1Id) {
        CoMultipleChoiceE1 coMultipleChoiceE1 = new CoMultipleChoiceE1DAO().findById(nMultipleChoiceE1Id);
        if (coMultipleChoiceE1 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("multipleChoice.search.notFound"));
        } else {
            List<CoMultipleChoiceE1> list = new ArrayList<CoMultipleChoiceE1>();
            EntityManagerHelper.refresh(coMultipleChoiceE1);
            list.add(coMultipleChoiceE1);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de actividades
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
	 * Obtiene la lista de actividades
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoMultipleChoiceE1DAO coMultipleChoiceE1DAO = new CoMultipleChoiceE1DAO();
        List<CoMultipleChoiceE1> list = coMultipleChoiceE1DAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("multipleChoice.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coMultipleChoiceE1DAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    public RestServiceResult listMChoiceForExercise1(RestServiceResult serviceResult, Long nExerciseId) {
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
        EntityManagerHelper.refresh(coExercises1);
        if (coExercises1 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("multipleChoice.search.notFound"));
        } else {
            List<CoMultipleChoiceE1> list = new ArrayList<CoMultipleChoiceE1>(coExercises1.getCoMultipleChoiceE1s());
            if (list.size() == 0) {
                Object[] arrayParam = { coExercises1.getExerciseName() };
                serviceResult.setError(true);
                serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.listMultipleChoiceForExerciseE1.notFound"), arrayParam));
            } else {
                Object[] arrayParam = { list.size(), coExercises1.getExerciseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.listMultipleChoiceForExerciseE1.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            }
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de multiple choice asociados a un ejercicio
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listPageMChoiceForExercise1(RestServiceResult result, Long nExerciseId) {
        return listPageMChoiceForExercise1(result, nExerciseId, 0, 0);
    }

    /**
	 * Obtiene la lista paginada de multiple choice asociados a un ejercicio
	 * 
	 * @param serviceResult
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
    @SuppressWarnings("unchecked")
    public RestServiceResult listPageMChoiceForExercise1(RestServiceResult serviceResult, Long nExerciseId, int nRowStart, int nMaxResults) {
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
        log.info("Entro en el datamanager y el id del ejercicio es: " + nExerciseId);
        EntityManagerHelper.refresh(coExercises1);
        if (coExercises1 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercises1.search.notFound"));
        } else {
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_MULTIPLE_CHOICE_IN_EXERCISE, CoMultipleChoiceE1.class);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            query.setParameter(1, nExerciseId);
            if (nRowStart > 0) query.setFirstResult(nRowStart);
            if (nMaxResults > 0) query.setMaxResults(nMaxResults);
            List<CoMultipleChoiceE1> list = query.getResultList();
            if (list.size() == 0) {
                Object[] arrayParam = { coExercises1.getExerciseName() };
                serviceResult.setError(true);
                serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.listMultipleChoiceForExerciseE1.notFound"), arrayParam));
            } else {
                Object[] arrayParam = { list.size(), coExercises1.getExerciseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("multipleChoice.listMultipleChoiceForExerciseE1.success"), arrayParam));
                serviceResult.setObjResult(list);
                if ((nRowStart > 0) || (nMaxResults > 0)) {
                    RestServiceResult serviceResult2 = listPageMChoiceForExercise1(new RestServiceResult(), nExerciseId);
                    int nNumMultipleChoice = serviceResult2.getNumResult();
                    serviceResult.setNumResult(nNumMultipleChoice);
                } else serviceResult.setNumResult(list.size());
            }
        }
        return serviceResult;
    }
}
