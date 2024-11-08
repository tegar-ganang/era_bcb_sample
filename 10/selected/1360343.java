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
import edu.univalle.lingweb.persistence.CoCompleteE2;
import edu.univalle.lingweb.persistence.CoDeliveryDate2;
import edu.univalle.lingweb.persistence.CoDeliveryDate2DAO;
import edu.univalle.lingweb.persistence.CoExercises2;
import edu.univalle.lingweb.persistence.CoExercises2DAO;
import edu.univalle.lingweb.persistence.CoScoreExercises2;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.OpenResponse2;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_exercises2'( Ejercicios tipo secuencia 2)
 * 
 * @author Julio Cesar Puentes
 */
public class DataManagerExerciseS2 extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerExerciseS2() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo ejercicio tipo secuencia 2  en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coExercises2
	 *            Ejercicio a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoExercises2 coExercises2, String sDeliveryDate) {
        CoExercises2DAO coExercises2DAO = new CoExercises2DAO();
        try {
            Long nSequence = getSequence("sq_co_exercises2");
            coExercises2.setExerciseId(nSequence);
            EntityManagerHelper.beginTransaction();
            coExercises2DAO.save(coExercises2);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coExercises2);
            log.info("Ejercicio S2 creado con �xito: " + coExercises2.getExerciseName());
            Object[] arrayParam = { coExercises2.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.create.success"), arrayParam));
            serviceResult.setId(nSequence);
            this.addDeliveryDate(nSequence, sDeliveryDate);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio s2: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un ejercicio tipo secuencia 2 en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coExercises2
	 *            Ejercicio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoExercises2 coExercises2, String sDeliveryDate) {
        CoExercises2DAO coExercises2DAO = new CoExercises2DAO();
        Long nExerciseId = coExercises2.getExerciseId();
        try {
            EntityManagerHelper.beginTransaction();
            coExercises2DAO.update(coExercises2);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coExercises2);
            Object[] args = { coExercises2.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.update.success"), args));
            this.addDeliveryDate(nExerciseId, sDeliveryDate);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio s2: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un ejercicio tipo secuencia 2
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coExercises2
	 *            Ejercicio a Eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoExercises2 coExercises2) {
        CoExercises2DAO coExercises2DAO = new CoExercises2DAO();
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_EXERCITE2);
            query.setParameter(1, coExercises2.getExerciseId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coExercises2);
            Object[] arrayParam = { coExercises2.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el ejercicio s2: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coExercises2.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio tipo secuencia 2 por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sExerciseName
	 *            Nombre del ejercicio tipo secuencia 2
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sExerciseName) {
        List<CoExercises2> list = new CoExercises2DAO().findByExerciseName(sExerciseName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercises2.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio tipo secuencia 2 por id
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
        CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExerciseId);
        if (coExercises2 == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercises2.search.notFound"));
        } else {
            List<CoExercises2> list = new ArrayList<CoExercises2>();
            EntityManagerHelper.refresh(coExercises2);
            list.add(coExercises2);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.search.success"), arrayParam));
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
        CoExercises2DAO coExercises2DAO = new CoExercises2DAO();
        List<CoExercises2> list = coExercises2DAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercises2.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coExercises2DAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Agrega una lista de fechas de entrega a un ejercicio tipo 2
	 * 
	 * @param nExerciseId
	 *            Long c�digo del ejercicio.
	 * @param sDates
	 * 			  String fechas de entrega separadas por ",".
	 */
    @SuppressWarnings("unchecked")
    public void addDeliveryDate(Long nExerciseId, String sDates) {
        try {
            CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExerciseId);
            if (coExercises2 == null) {
                log.info("Ejercicio no existe: ");
            } else {
                EntityManagerHelper.beginTransaction();
                Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_EXERCISE2_DELIVERYDATE2);
                query.setParameter(1, new Long(nExerciseId));
                query.executeUpdate();
                StringTokenizer tokenizer = new StringTokenizer(sDates, ",");
                while (tokenizer.hasMoreTokens()) {
                    CoDeliveryDate2DAO coDeliveryDate2DAO = new CoDeliveryDate2DAO();
                    try {
                        String sKeyValue = tokenizer.nextToken();
                        String[] data = sKeyValue.split("\\|");
                        String sDeliveryDateNum = data[0];
                        Date deliveryDate = Date.valueOf(data[1]);
                        CoDeliveryDate2 coDeliveryDate2 = new CoDeliveryDate2();
                        coDeliveryDate2.setDeliveryDateId(getSequence("sq_co_delivery_date2"));
                        coDeliveryDate2.setDeliveryDateNum(new Long(sDeliveryDateNum));
                        coDeliveryDate2.setDeliveryDate(deliveryDate);
                        coDeliveryDate2.setCoExercises2(new CoExercises2DAO().findById(nExerciseId));
                        coDeliveryDate2DAO.save(coDeliveryDate2);
                        log.info("Fecha Entrega " + coDeliveryDate2.getDeliveryDate() + " creada con �xito...");
                    } catch (Exception e) {
                        EntityManagerHelper.rollback();
                    }
                }
                EntityManagerHelper.commit();
            }
        } catch (PersistenceException e) {
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
    public RestServiceResult createExerciseS2Material(RestServiceResult serviceResult, String sArrayMaterialId, CoExercises2 coExercises2) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_EXERCITES2_MATERIAL);
            query.setParameter(1, coExercises2.getExerciseId());
            query.executeUpdate();
            StringTokenizer stringTokenizer = new StringTokenizer(sArrayMaterialId, ",");
            while (stringTokenizer.hasMoreTokens()) {
                long nMaterialId = Long.parseLong(stringTokenizer.nextToken());
                query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_EXERCITES2_MATERIAL);
                query.setParameter(1, coExercises2.getExerciseId());
                query.setParameter(2, nMaterialId);
                query.executeUpdate();
            }
            EntityManagerHelper.commit();
            Object[] arrayParam = { coExercises2.getExerciseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Ejercicio2- Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercises2.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Valida si un ejercicio ya calificado esta en el valor critico o en el valor execelente para visualizar ejercicio complementario
	 * @param coExercises2 ejercicio a validar
	 * @param nUserId usuario estudiante
	 * @return boolean true si se debe habilitar, false de lo contrario.
	 */
    public boolean validateRangeExerciseS2Scored(CoExercises2 coExercises2, Long nUserId) {
        List<CoScoreExercises2> listCoScoreExercises2 = new ArrayList<CoScoreExercises2>();
        CoScoreExercises2 coScoreExercises2 = null;
        String sFlagExerciseScore = "";
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_SCORE_EXERCISES2, CoScoreExercises2.class);
        query.setParameter(1, coExercises2.getExerciseId());
        query.setParameter(2, nUserId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listCoScoreExercises2 = query.getResultList();
        for (int i = 0; i < listCoScoreExercises2.size(); i++) {
            coScoreExercises2 = (CoScoreExercises2) listCoScoreExercises2.get(i);
            sFlagExerciseScore = coExercises2.getFlagExerciseScore();
        }
        if (coScoreExercises2 == null) {
            return false;
        } else if (sFlagExerciseScore.equalsIgnoreCase("0")) {
            Float nScore = new Float(coScoreExercises2.getScore());
            if (nScore < Common.CRITIC_VALUE_QUANTITATIVE || nScore >= Common.EXELLENT_VALUE_QUANTITATIVE) {
                return true;
            } else {
                return false;
            }
        } else {
            Long nScore = new Long(coScoreExercises2.getScore());
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
    public RestServiceResult createDeliveryDate2(RestServiceResult serviceResult, CoDeliveryDate2 coDeliveryDate2) {
        CoDeliveryDate2DAO coDeliveryDate2DAO = new CoDeliveryDate2DAO();
        coDeliveryDate2.setDeliveryDateId(getSequence("sq_co_delivery_date2"));
        EntityManagerHelper.beginTransaction();
        coDeliveryDate2DAO.save(coDeliveryDate2);
        EntityManagerHelper.commit();
        EntityManagerHelper.refresh(coDeliveryDate2);
        log.info("Nueva fecha para ver ejercicio complemetario " + coDeliveryDate2.getDeliveryDate() + " creada con �xito...");
        return serviceResult;
    }

    public static void main(String[] args) {
        DataManagerExerciseS2 dataManagerExerciseS2 = new DataManagerExerciseS2();
        dataManagerExerciseS2.addDeliveryDate(new Long("8"), "3|2009-5-8,2|2009-5-7,1|2009-5-6,");
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
    public RestServiceResult updateDeliveryDate2(RestServiceResult serviceResult, CoDeliveryDate2 coDeliveryDate2) {
        CoDeliveryDate2DAO coDeliveryDate2DAO = new CoDeliveryDate2DAO();
        EntityManagerHelper.beginTransaction();
        coDeliveryDate2DAO.update(coDeliveryDate2);
        EntityManagerHelper.commit();
        EntityManagerHelper.refresh(coDeliveryDate2);
        log.info("se actualiz� la fecha " + coDeliveryDate2.getDeliveryDateId() + " con �xito...");
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
        CoExercises2DAO coExercises2DAO = new CoExercises2DAO();
        List<CoExercises2> list = coExercises2DAO.findByFlagClone("1", nRowStart, nMaxResults);
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

    /**
	 * permite clonar el contenido de un ejercicio
	 * @param nExercises2OldId C�digo del ejercicio viejo
	 * @param nExercises2NewId C�digo del ejercicio nuevo
	 */
    public void cloneExercises2(long nExercises2OldId, long nExercises2NewId) {
        int nUpdate = 0;
        CoExercises2 coExercises2Old = new CoExercises2DAO().findById(nExercises2OldId);
        EntityManagerHelper.refresh(coExercises2Old);
        EntityManagerHelper.beginTransaction();
        Query query = null;
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_DELIVERY_DATE_2);
        query.setParameter(1, nExercises2NewId);
        query.setParameter(2, nExercises2OldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n CO_DELIVERY_DATE2-Exercises[" + nExercises2OldId + "] - NEWEXERCISES_2[" + nExercises2NewId + "]" + " - DeliveryDate2  => " + nUpdate);
        Set<OpenResponse2> setOpenResponse = coExercises2Old.getOpenResponse2s();
        log.info("Ejercicio[" + nExercises2OldId + "] Cantidad de respuestas abiertas a clonar: " + setOpenResponse.size());
        if (setOpenResponse.size() == 1) {
            CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExercises2NewId);
            EntityManagerHelper.refresh(coExercises2);
            if (coExercises2 != null) {
                if (coExercises2.getOpenResponse2s().size() == 0) {
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_OPEN_RESPONSE2);
                    query.setParameter(1, nExercises2NewId);
                    query.setParameter(2, nExercises2OldId);
                    nUpdate = query.executeUpdate();
                    log.info("\n\nClonaci�n OPEN-RESPONSE2-[" + nExercises2OldId + "] - NEWEXERCISES_1[" + nExercises2NewId + "]" + " - RESPONSE2  => " + nUpdate);
                } else {
                    log.error("El ejercicio2[" + nExercises2OldId + "] ya tiene clonado la respuesta abierta ");
                }
            } else {
                log.error("El ejercicio2[" + nExercises2NewId + "] no existe...");
            }
        } else {
            log.error("El ejercicio2[" + nExercises2OldId + "] tiene " + setOpenResponse.size() + " respuestas abiertas !!...");
        }
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_MULTIPLE_CHOISE_1);
        query.setParameter(1, nExercises2NewId);
        query.setParameter(2, nExercises2OldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n MULTIPLE_CHOICE_E1-Exercises[" + nExercises2OldId + "] - NEWEXERCISES_1[" + nExercises2NewId + "]" + " - MultipleChoise  => " + nUpdate);
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_SINGLE_TEXT_TEACHER_1);
        query.setParameter(1, nExercises2NewId);
        query.setParameter(2, nExercises2OldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n CO_SINGLE_TEXT_TEACHER1-Exercises[" + nExercises2OldId + "] - NEWEXERCISES_1[" + nExercises2NewId + "]" + " - SINGLETEXTTEACHER  => " + nUpdate);
        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_MATRIX_EXERCISE_1);
        query.setParameter(1, nExercises2NewId);
        query.setParameter(2, nExercises2OldId);
        nUpdate = query.executeUpdate();
        log.info("\n\nClonaci�n CO_MATRIX_QUESTION-Exercises[" + nExercises2OldId + "] - NEWEXERCISES_1[" + nExercises2NewId + "]" + " - MATRIX  => " + nUpdate);
        log.info("\n\nCOMPLETE EXERSICE[" + coExercises2Old.getExerciseId() + "] = " + coExercises2Old.getCoCompleteE2s().size() + " reg");
        for (Iterator<CoCompleteE2> iterator2 = coExercises2Old.getCoCompleteE2s().iterator(); iterator2.hasNext(); ) {
            CoCompleteE2 coCompleteE2Old = iterator2.next();
            Long nCompleteE1Id = getSequence("sq_co_complete_e1");
            query = EntityManagerHelper.createNativeQuery(Statements.CLONE_COMPLETE_E1);
            query.setParameter(1, nCompleteE1Id);
            query.setParameter(2, nExercises2NewId);
            query.setParameter(3, nExercises2OldId);
            nUpdate = query.executeUpdate();
            log.info("Clonaci�n co_complete_e1-Exercises[" + nExercises2OldId + "] - NEWEXERCISES_1[" + nExercises2NewId + "]" + " - complete_e1  => " + nUpdate);
            query = EntityManagerHelper.createNativeQuery(Statements.CLONE_WORDS_COMPLETE_E1);
            query.setParameter(1, nCompleteE1Id);
            query.setParameter(2, coCompleteE2Old.getCompleteE2Id());
            nUpdate = query.executeUpdate();
            log.info("Clonaci�n CLONE_WORDS_COMPLETE_E1-Exercises[" + nCompleteE1Id + "] - NEW_COMPLETE_E1[" + coCompleteE2Old.getCompleteE2Id() + "]" + " - WORDS_COMPLETE_E1 => " + nUpdate);
        }
        EntityManagerHelper.commit();
    }
}
