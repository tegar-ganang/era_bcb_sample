package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoActivity;
import edu.univalle.lingweb.persistence.CoActivityDAO;
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseDAO;
import edu.univalle.lingweb.persistence.CoSequence;
import edu.univalle.lingweb.persistence.CoSequenceDAO;
import edu.univalle.lingweb.persistence.CoSequenceUserHistory;
import edu.univalle.lingweb.persistence.CoSequenceUserHistoryDAO;
import edu.univalle.lingweb.persistence.CoSequenceUserHistoryId;
import edu.univalle.lingweb.persistence.CoTest;
import edu.univalle.lingweb.persistence.CoTestDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'co_sequence'( Secuencias )
 * 
 * @author Jose Aricapa
 */
public class DataManagerSequence extends DataManager {

    /**
	 * Manejo de mensajes Log's
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerSequence() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva secuencia en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coSequence
	 *            Secuencia a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoSequence coSequence) {
        CoSequenceDAO coSequenceDAO = new CoSequenceDAO();
        try {
            coSequence.setSequenceId(getSequence("sq_co_sequence"));
            EntityManagerHelper.beginTransaction();
            coSequenceDAO.save(coSequence);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coSequence);
            log.info("Secuencias" + coSequence.getSequenceName() + " creada con �xito...");
            Object[] arrayParam = { coSequence.getSequenceName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la secuencia: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza una una nueva secuencia en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coSequence
	 *            Secuencia a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoSequence coSequence) {
        CoSequenceDAO coSequenceDAO = new CoSequenceDAO();
        try {
            log.info("Actualizando la secuencia: " + coSequence.getSequenceName());
            EntityManagerHelper.beginTransaction();
            coSequenceDAO.update(coSequence);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coSequence);
            Object[] args = { coSequence.getSequenceName() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.update.success"), args));
            }
            log.info("Se actualizo la secuencia con �xito: " + coSequence.getSequenceName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la secuencia: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una secuencia de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coSequence
	 *            Secuencia a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoSequence coSequence) {
        try {
            log.info("Eliminando la secuencia: " + coSequence.getSequenceName());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_SEQUENCE);
            query.setParameter(1, coSequence.getSequenceId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coSequence);
            Object[] arrayParam = { coSequence.getSequenceName() };
            log.info("Secuencia eliminada con �xito: " + coSequence.getSequenceName());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la secuencia: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coSequence.getSequenceName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una secuencia
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nSequenceId
	 *            C�digo de la actividad
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nSequenceId) {
        CoSequence coSequence = new CoSequenceDAO().findById(nSequenceId);
        if (coSequence == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("sequence.search.notFound"));
        } else {
            List<CoSequence> list = new ArrayList<CoSequence>();
            EntityManagerHelper.refresh(coSequence);
            list.add(coSequence);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de secuencias
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de sequencias
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoSequenceDAO coSequenceDAO = new CoSequenceDAO();
        List<CoSequence> list = coSequenceDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("sequence.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coSequenceDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de secuencias clonables
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult result) {
        return listClone(result, 0, 0);
    }

    /**
	 * Obtiene la lista de secuencias clonables
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoSequenceDAO coSequenceDAO = new CoSequenceDAO();
        List<CoSequence> list = coSequenceDAO.findByFlagClone("1", nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("sequence.listClone.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("sequence.listClone.success"), array));
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
	 * M�todo que permite la clonaci�n de una SECUENCIA
	 * 
	 * @param nSequenceOldId
	 *            C�digo del la secuencia a clonar
	 * @param nSequenceNewId
	 *            C�digo del la secuencia nueva
	 */
    public void cloneSequence(Long nSequenceOldId, Long nSequenceNewId, MaUser maUser) {
        try {
            CoSequence coSequence = new CoSequenceDAO().findById(nSequenceOldId);
            EntityManagerHelper.refresh(coSequence);
            EntityManagerHelper.beginTransaction();
            cloneUserHistory(nSequenceOldId, nSequenceNewId);
            EntityManagerHelper.commit();
            Set<CoActivity> setActivity = coSequence.getCoActivities();
            log.info("N�mero de actividades a clonar: " + setActivity.size());
            for (CoActivity coActivity : setActivity) {
                EntityManagerHelper.beginTransaction();
                log.info("Clonado actividad: " + coActivity.getActivityId());
                Long nActivityNewId = getSequence("sq_co_activity");
                Query query = EntityManagerHelper.createNativeQuery(Statements.CLONE_ACTIVITY_FOR_SEQUENCE.replaceAll(":CLONE", bundle.getString("course.create.clone")));
                query.setParameter(1, nActivityNewId);
                query.setParameter(2, nSequenceNewId);
                query.setParameter(3, coActivity.getActivityId());
                query.executeUpdate();
                EntityManagerHelper.commit();
                CoActivity coActivityNew = new CoActivityDAO().findById(nActivityNewId);
                EntityManagerHelper.refresh(coActivityNew);
                if (coActivityNew != null) {
                    EntityManagerHelper.beginTransaction();
                    DataManagerActivity.addUserHistory(new RestServiceResult(), maUser, coActivityNew);
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_ACTIVITY_MATERIAL);
                    query.setParameter(1, nActivityNewId);
                    query.setParameter(2, coActivity.getActivityId());
                    query.executeUpdate();
                    log.info("Ok...");
                    EntityManagerHelper.commit();
                    DataManagerActivity dataManagerActivity = new DataManagerActivity();
                    dataManagerActivity.setBundle(bundle);
                    dataManagerActivity.cloneActivity(coActivity.getActivityId(), nActivityNewId);
                }
            }
            Set<CoTest> setTestClone = coSequence.getCoTests();
            log.info("N�mero de pruebas a clonar:_" + setTestClone.size());
            for (CoTest coTest : setTestClone) {
                EntityManagerHelper.beginTransaction();
                Long nTestNewId = getSequence("sq_co_test");
                Query query = EntityManagerHelper.createNativeQuery(Statements.CLONE_TEST.replaceAll(":CLONE", bundle.getString("course.create.clone")));
                query.setParameter(1, nTestNewId);
                query.setParameter(2, nSequenceNewId);
                query.setParameter(3, coTest.getTestId());
                query.executeUpdate();
                EntityManagerHelper.commit();
                log.info("Clonando prueba: " + coTest.getTestId());
                DataManagerTest.addUserHistory(new RestServiceResult(), maUser, coTest);
                CoTest coTestNew = new CoTestDAO().findById(new Long(nTestNewId));
                EntityManagerHelper.refresh(coTestNew);
                if (coTestNew != null) {
                    EntityManagerHelper.beginTransaction();
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_TEST_MATERIAL);
                    query.setParameter(1, nTestNewId);
                    query.setParameter(2, coTest.getTestId());
                    query.executeUpdate();
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_QUESTION_WEIGHTED);
                    query.setParameter(1, nTestNewId);
                    query.setParameter(2, coTest.getTestId());
                    query.executeUpdate();
                    log.info("Ok...");
                    EntityManagerHelper.commit();
                    DataManagerTest dataManagerTest = new DataManagerTest();
                    dataManagerTest.setBundle(bundle);
                    dataManagerTest.cloneTest(coTest.getTestId(), nTestNewId);
                }
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
        }
    }

    /**
	 * Permite clonar el historial de usuario de una secuencia clonada
	 * 
	 * @param nSequenceOldId
	 *            C�digo de la unidad a clonar
	 * @param coUnitNew
	 *            Unidad Creada a partir de clonaci�n
	 */
    private void cloneUserHistory(Long nSequenceOldId, Long nSequenceNew) {
        try {
            log.info("CLONACION HISTORIAL DE SECUENCIAS X USUARIO");
            CoSequence coSequence = new CoSequenceDAO().findById(nSequenceOldId);
            EntityManagerHelper.refresh(coSequence);
            Set<CoSequenceUserHistory> setUserHistoryOld = coSequence.getCoSequenceUserHistories();
            CoSequence coSequenceNew = new CoSequenceDAO().findById(nSequenceNew);
            for (CoSequenceUserHistory coSequenceUserHistory : setUserHistoryOld) {
                MaUser maUser = coSequenceUserHistory.getMaUser();
                CoSequenceUserHistory sequenceUserHistory = new CoSequenceUserHistoryDAO().findById(new CoSequenceUserHistoryId(coSequenceNew.getSequenceId(), maUser.getUserId()));
                log.info("Resultado de la consulta => " + sequenceUserHistory);
                if (sequenceUserHistory == null) {
                    log.info("Agregando HistorialxSecuencia: Usuario '" + maUser.getUserId() + "' - secuencia '" + coSequenceNew.getSequenceId() + "'");
                    addUserHistory(maUser, coSequenceNew);
                } else {
                    log.info("El usuario '" + maUser.getUserId() + "' tiene la secuencia '" + coSequenceNew.getSequenceId() + "'");
                }
            }
            log.info("Ok...Termina clonaci�n de Historial de usuarios en SECUENCIAS");
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param maUser
	 *            usuario que se agrega al historial
	 * @param coSequence
	 *            Secuencia creada
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n
	 */
    public static RestServiceResult addUserHistory(RestServiceResult serviceResult, MaUser maUser, CoSequence coSequence) {
        log.info("HISTORIAL DE SECUENCIAS X USUARIO");
        CoSequenceUserHistory activityUserHistory = new CoSequenceUserHistoryDAO().findById(new CoSequenceUserHistoryId(coSequence.getSequenceId(), maUser.getUserId()));
        log.info("Resultado de la consulta => " + activityUserHistory);
        if (activityUserHistory == null) {
            log.info("Agregando HistorialxSecuencia: Usuario '" + maUser.getUserId() + "' - Secuencia '" + coSequence.getSequenceId() + "'");
            addUserHistory(maUser, coSequence);
        } else {
            log.info("El usuario '" + maUser.getUserId() + "' tiene la secuencia '" + coSequence.getSequenceId() + "'");
        }
        log.info("Termina HISTORIAL DE SECUENCIA...");
        return serviceResult;
    }

    /**
	 * Registra en el historial de secuencia un usuario
	 * 
	 * @param maUser
	 *            usuario a guarda
	 * @param coSequence
	 *            unidad a guardar
	 */
    private static void addUserHistory(MaUser maUser, CoSequence coSequence) {
        try {
            CoSequenceUserHistoryId userHistoryId = new CoSequenceUserHistoryId();
            userHistoryId.setSequenceId(coSequence.getSequenceId());
            userHistoryId.setUserId(maUser.getUserId());
            new CoSequenceUserHistoryDAO().save(new CoSequenceUserHistory(userHistoryId, maUser, coSequence, new Date()));
            log.info("Se guardo el usuario " + maUser.getUserName() + " con �xito al historial de la secuencia '" + coSequence.getSequenceId() + "' ");
        } catch (PersistenceException e) {
            log.info("El usuario " + maUser.getUserId() + " ya esta en el  historial de la secuencia '" + coSequence.getSequenceId() + "' " + "OMITIR EXCEPCION PRIMARY KEY");
        }
        return;
    }

    public static void main(String[] args) {
        try {
            EntityManagerHelper.beginTransaction();
            CoCourse coCourse = new CoCourseDAO().findById(new Long("183"));
            EntityManagerHelper.commit();
            EntityManagerHelper.beginTransaction();
            coCourse.setCourseId(new Long("100000"));
            new CoCourseDAO().save(coCourse);
            EntityManagerHelper.commit();
        } catch (NumberFormatException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
        }
    }
}
