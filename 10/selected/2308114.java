package edu.univalle.lingweb.model;

import java.sql.Date;
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
import edu.univalle.lingweb.persistence.CoCompleteE1;
import edu.univalle.lingweb.persistence.CoDeliveryDate1;
import edu.univalle.lingweb.persistence.CoDeliveryDate1DAO;
import edu.univalle.lingweb.persistence.CoExercises1;
import edu.univalle.lingweb.persistence.CoExercises1DAO;
import edu.univalle.lingweb.persistence.CoScoreExercises1;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.OpenResponse;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_exercises1'( Ejercicios tipo secuencia 1)
 * 
 * @author Julio Cesar Puentes
 */
public class DataManagerExerciseS1 extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerExerciseS1() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo ejercicio tipo secuencia 1  en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coExercises1
	 *            Ejercicio a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoExercises1 coExercises1, String sDeliveryDate) {
        CoExercises1DAO coExercises1DAO = new CoExercises1DAO();
        try {
            Long nSequence = getSequence("sq_co_exercises1");
            coExercises1.setExerciseId(nSequence);
            EntityManagerHelper.beginTransaction();
            coExercises1DAO.save(coExercises1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coExercises1);
            log.info("Ejercicio S1 creado con �xito: " + coExercises1.getExerciseName());
            Object[] arrayParam = { coExercises1.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.create.success"), arrayParam));
            serviceResult.setId(nSequence);
            this.addDeliveryDate(nSequence, sDeliveryDate);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio s1: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un ejercicio tipo secuencia 1 en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coExercises1
	 *            Ejercicio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoExercises1 coExercises1, String sDeliveryDate) {
        CoExercises1DAO coExercises1DAO = new CoExercises1DAO();
        Long nExerciseId = coExercises1.getExerciseId();
        try {
            EntityManagerHelper.beginTransaction();
            coExercises1DAO.update(coExercises1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coExercises1);
            Object[] args = { coExercises1.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.update.success"), args));
            this.addDeliveryDate(nExerciseId, sDeliveryDate);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio s1: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un ejericio tipo secuencia 1
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coExercises1
	 *            Ejercicio a Eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoExercises1 coExercises1) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_EXERCITE1);
            query.setParameter(1, coExercises1.getExerciseId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coExercises1);
            Object[] arrayParam = { coExercises1.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el ejercicio s1: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coExercises1.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio tipo secuencia 1 por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sActivityName
	 *            Nombre del ejercicio tipo secuencia 1
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sExerciseName) {
        List<CoExercises1> list = new CoExercises1DAO().findByExerciseName(sExerciseName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercises1.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio tipo secuencia 1 por id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nExerciseId
	 *            C�digo del ejercicio
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nExerciseId) {
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
        if (coExercises1 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercises1.search.notFound"));
        } else {
            List<CoExercises1> list = new ArrayList<CoExercises1>();
            EntityManagerHelper.refresh(coExercises1);
            list.add(coExercises1);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de ejercicios
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
	 * Obtiene la lista de ejercicios en un rango determinado
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoExercises1DAO coExercises1DAO = new CoExercises1DAO();
        List<CoExercises1> list = coExercises1DAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercises1.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coExercises1DAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Agrega una lista de fechas de entrega a un ejercicio tipo 1
	 * 
	 * @param nExerciseId
	 *            Long c�digo del ejercicio.
	 * @param sDates
	 * 			  String fechas de entrega separadas por ",".
	 */
    @SuppressWarnings("unchecked")
    public void addDeliveryDate(Long nExerciseId, String sDates) {
        try {
            CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
            if (coExercises1 == null) {
                log.info("Ejercicio no existe: ");
            } else {
                EntityManagerHelper.beginTransaction();
                Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_EXERCISE1_DELIVERYDATE1);
                query.setParameter(1, new Long(nExerciseId));
                query.executeUpdate();
                StringTokenizer tokenizer = new StringTokenizer(sDates, ",");
                while (tokenizer.hasMoreTokens()) {
                    CoDeliveryDate1DAO coDeliveryDate1DAO = new CoDeliveryDate1DAO();
                    try {
                        String sKeyValue = tokenizer.nextToken();
                        String[] data = sKeyValue.split("\\|");
                        String sDeliveryDateNum = data[0];
                        Date deliveryDate = Date.valueOf(data[1]);
                        CoDeliveryDate1 coDeliveryDate1 = new CoDeliveryDate1();
                        coDeliveryDate1.setDeliveryDateId(getSequence("sq_co_delivery_date1"));
                        coDeliveryDate1.setDeliveryDateNum(new Long(sDeliveryDateNum));
                        coDeliveryDate1.setDeliveryDate(deliveryDate);
                        coDeliveryDate1.setCoExercises1(new CoExercises1DAO().findById(nExerciseId));
                        coDeliveryDate1DAO.save(coDeliveryDate1);
                        log.info("Fecha Entrega " + coDeliveryDate1.getDeliveryDate() + " creada con �xito...");
                    } catch (Exception e) {
                        EntityManagerHelper.rollback();
                    }
                }
                EntityManagerHelper.commit();
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
        }
    }

    /**
	 * Realiza el proceso de relacionar un material con un ejercicio
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
    public RestServiceResult createExerciseS1Material(RestServiceResult serviceResult, String sArrayMaterialId, CoExercises1 coExercises1) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_EXERCITES1_MATERIAL);
            query.setParameter(1, coExercises1.getExerciseId());
            query.executeUpdate();
            StringTokenizer stringTokenizer = new StringTokenizer(sArrayMaterialId, ",");
            while (stringTokenizer.hasMoreTokens()) {
                long nMaterialId = Long.parseLong(stringTokenizer.nextToken());
                query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_EXERCITES1_MATERIAL);
                query.setParameter(1, coExercises1.getExerciseId());
                query.setParameter(2, nMaterialId);
                query.executeUpdate();
            }
            EntityManagerHelper.commit();
            Object[] arrayParam = { coExercises1.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Ejercicio- Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises1.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Valida si un ejercicio ya calificado esta en el valor critico o en el valor execelente para visualizar ejercicio complementario
	 * @param coExercises1 ejercicio a validar
	 * @param nUserId usuario estudiante
	 * @return boolean true si se debe habilitar, false de lo contrario.
	 */
    public boolean validateRangeExerciseS1Scored(Long nRoleUserId, CoExercises1 coExercises1, Long nUserId) {
        List<CoScoreExercises1> listCoScoreExercises1 = new ArrayList<CoScoreExercises1>();
        CoScoreExercises1 coScoreExercises1 = null;
        String sFlagExerciseScore = "";
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_SCORE_EXERCISES1, CoScoreExercises1.class);
        query.setParameter(1, coExercises1.getExerciseId());
        query.setParameter(2, nUserId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listCoScoreExercises1 = query.getResultList();
        for (int i = 0; i < listCoScoreExercises1.size(); i++) {
            coScoreExercises1 = (CoScoreExercises1) listCoScoreExercises1.get(i);
            sFlagExerciseScore = coExercises1.getFlagExerciseScore();
        }
        if (coScoreExercises1 == null) {
            return false;
        } else if (sFlagExerciseScore.equalsIgnoreCase("0")) {
            Float nScore = new Float(coScoreExercises1.getScore());
            if (nScore < Common.CRITIC_VALUE_QUANTITATIVE || nScore >= Common.EXELLENT_VALUE_QUANTITATIVE) {
                return true;
            } else {
                return false;
            }
        } else {
            Long nScore = new Long(coScoreExercises1.getScore());
            if (nScore < Common.CRITIC_VALUE_QUALITATIVE || nScore >= Common.EXELLENT_VALUE_QUALITATIVE) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
	 * Crea una nueva fecha de entrega para el ejercicio, de esta manera se podra visualizar el ejercicio complementario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coDeliveryDate1
	 *            Nueva fecha de entrega
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult createDeliveryDate1(RestServiceResult serviceResult, CoDeliveryDate1 coDeliveryDate1) {
        CoDeliveryDate1DAO coDeliveryDate1DAO = new CoDeliveryDate1DAO();
        coDeliveryDate1.setDeliveryDateId(getSequence("sq_co_delivery_date1"));
        EntityManagerHelper.beginTransaction();
        coDeliveryDate1DAO.save(coDeliveryDate1);
        EntityManagerHelper.commit();
        EntityManagerHelper.refresh(coDeliveryDate1);
        log.info("Nueva fecha para ver ejercicio complemetario " + coDeliveryDate1.getDeliveryDate() + " creada con �xito...");
        return serviceResult;
    }

    /**
	 * Actualiza la ultima fecha de entrega para el ejercicio
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coDeliveryDate1
	 *            Fecha a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult updateDeliveryDate1(RestServiceResult serviceResult, CoDeliveryDate1 coDeliveryDate1) {
        CoDeliveryDate1DAO coDeliveryDate1DAO = new CoDeliveryDate1DAO();
        EntityManagerHelper.beginTransaction();
        coDeliveryDate1DAO.update(coDeliveryDate1);
        EntityManagerHelper.commit();
        EntityManagerHelper.refresh(coDeliveryDate1);
        log.info("se actualizo la fecha " + coDeliveryDate1.getDeliveryDateId() + " con �xito...");
        return serviceResult;
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
        CoExercises1DAO coExercises1DAO = new CoExercises1DAO();
        List<CoExercises1> list = coExercises1DAO.findByFlagClone("1", nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
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

    public void methodTest() {
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(new Long("2000"));
        EntityManagerHelper.refresh(coExercises1);
        System.out.println("ejercicio: " + coExercises1);
    }

    /**
	 * permite clonar el contenido de un ejercicio
	 * @param nExercises1OldId C�digo del ejercicio viejo
	 * @param nExercises1NewId C�digo del ejercicio nuevo
	 */
    public void cloneExercises1(long nExercises1OldId, long nExercises1NewId) {
        int nUpdate = 0;
        CoExercises1 coExercises1Old = new CoExercises1DAO().findById(nExercises1OldId);
        EntityManagerHelper.refresh(coExercises1Old);
        EntityManagerHelper.beginTransaction();
        Query query = null;
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_DELIVERY_DATE_1);
        query.setParameter(1, nExercises1NewId);
        query.setParameter(2, nExercises1OldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n CO_DELIVERY_DATE1-Exercises[" + nExercises1OldId + "] - NEWEXERCISES_1[" + nExercises1NewId + "]" + " - DeliveryDate1  => " + nUpdate);
        Set<OpenResponse> setOpenResponse = coExercises1Old.getOpenResponses();
        log.info("Ejercicio[" + nExercises1OldId + "] Cantidad de respuestas abiertas a clonar: " + setOpenResponse.size());
        if (setOpenResponse.size() == 1) {
            CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExercises1NewId);
            EntityManagerHelper.refresh(coExercises1);
            if (coExercises1 != null) {
                if (coExercises1.getOpenResponses().size() == 0) {
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_OPEN_RESPONSE);
                    query.setParameter(1, nExercises1NewId);
                    query.setParameter(2, nExercises1OldId);
                    nUpdate = query.executeUpdate();
                    log.info("\n\nClonaci�n OPEN-RESPONSE-[" + nExercises1OldId + "] - NEWEXERCISES_1[" + nExercises1NewId + "]" + " - RESPONSE  => " + nUpdate);
                } else {
                    log.error("El ejercicio[" + nExercises1OldId + "] ya tiene clonado la respuesta abierta ");
                }
            } else {
                log.error("El ejercicio[" + nExercises1NewId + "] no existe...");
            }
        } else {
            log.error("El ejercicio[" + nExercises1OldId + "] tiene " + setOpenResponse.size() + " respuestas abiertas !!...");
        }
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_MULTIPLE_CHOISE_1);
        query.setParameter(1, nExercises1NewId);
        query.setParameter(2, nExercises1OldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n MULTIPLE_CHOICE_E1-Exercises[" + nExercises1OldId + "] - NEWEXERCISES_1[" + nExercises1NewId + "]" + " - MultipleChoise  => " + nUpdate);
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_SINGLE_TEXT_TEACHER_1);
        query.setParameter(1, nExercises1NewId);
        query.setParameter(2, nExercises1OldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n CO_SINGLE_TEXT_TEACHER1-Exercises[" + nExercises1OldId + "] - NEWEXERCISES_1[" + nExercises1NewId + "]" + " - SINGLETEXTTEACHER  => " + nUpdate);
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_MATRIX_EXERCISE_1);
        query.setParameter(1, nExercises1NewId);
        query.setParameter(2, nExercises1OldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n CO_MATRIX_QUESTION-Exercises[" + nExercises1OldId + "] - NEWEXERCISES_1[" + nExercises1NewId + "]" + " - MATRIX  => " + nUpdate);
        log.info("\n\nCOMPLETE EXERSICE[" + coExercises1Old.getExerciseId() + "] = " + coExercises1Old.getCoCompleteE1s().size() + " reg");
        for (Iterator<CoCompleteE1> iterator2 = coExercises1Old.getCoCompleteE1s().iterator(); iterator2.hasNext(); ) {
            CoCompleteE1 coCompleteE1Old = iterator2.next();
            Long nCompleteE1Id = getSequence("sq_co_complete_e1");
            query = EntityManagerHelper.createNativeQuery(Statements.CLONE_COMPLETE_E1);
            query.setParameter(1, nCompleteE1Id);
            query.setParameter(2, nExercises1NewId);
            query.setParameter(3, nExercises1OldId);
            nUpdate = query.executeUpdate();
            log.info("Clonaci�n co_complete_e1-Exercises[" + nExercises1OldId + "] - NEWEXERCISES_1[" + nExercises1NewId + "]" + " - complete_e1  => " + nUpdate);
            query = EntityManagerHelper.createNativeQuery(Statements.CLONE_WORDS_COMPLETE_E1);
            query.setParameter(1, nCompleteE1Id);
            query.setParameter(2, coCompleteE1Old.getCompleteE1Id());
            nUpdate = query.executeUpdate();
            log.info("Clonaci�n CLONE_WORDS_COMPLETE_E1-Exercises[" + nCompleteE1Id + "] - NEW_COMPLETE_E1[" + coCompleteE1Old.getCompleteE1Id() + "]" + " - WORDS_COMPLETE_E1 => " + nUpdate);
        }
        EntityManagerHelper.commit();
    }

    public static void main(String[] args) {
        DataManagerExerciseS1 dataManagerExerciseS1 = new DataManagerExerciseS1();
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(new Long("74"));
        EntityManagerHelper.refresh(coExercises1);
        CoExercises1 coExercises1Complete = new CoExercises1DAO().findById(new Long("75"));
        EntityManagerHelper.refresh(coExercises1Complete);
        if (Util.validateExerciseS1Dates(new Long(2), coExercises1Complete, Common.VALIDATE_EXERCISE_VISIBILITY) && (Util.validateRangeExerciseS1Scored(new Long(2), coExercises1, new Long(339)))) {
            System.out.println("actualizar padres");
            Util.updateEndDatesNodesPather(coExercises1);
        } else {
            System.out.println("no actualizar padres");
        }
    }
}
