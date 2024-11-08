package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.persistence.CoActivity;
import edu.univalle.lingweb.persistence.CoActivityDAO;
import edu.univalle.lingweb.persistence.CoActivityUserHistory;
import edu.univalle.lingweb.persistence.CoActivityUserHistoryDAO;
import edu.univalle.lingweb.persistence.CoActivityUserHistoryId;
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseUser;
import edu.univalle.lingweb.persistence.CoExercises1;
import edu.univalle.lingweb.persistence.CoExercises1DAO;
import edu.univalle.lingweb.persistence.CoExercises2;
import edu.univalle.lingweb.persistence.CoExercises2DAO;
import edu.univalle.lingweb.persistence.CoSequence;
import edu.univalle.lingweb.persistence.CoUnit;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.MaUserDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'co_activity'( Actividades)
 * 
 * @author Jose Aricapa
 */
public class DataManagerActivity extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerActivity() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva actividad en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coActivity
	 *            Actividad a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoActivity coActivity) {
        CoActivityDAO coActivityDAO = new CoActivityDAO();
        try {
            coActivity.setActivityId(getSequence("sq_co_activity"));
            EntityManagerHelper.beginTransaction();
            coActivityDAO.save(coActivity);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coActivity);
            log.info("Actividad creada con �xito: " + coActivity.getActivityName());
            Object[] arrayParam = { coActivity.getActivityName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el curso: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza una una nueva actividad en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coActivity
	 *            Actividad a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoActivity coActivity) {
        CoActivityDAO coActivityDAO = new CoActivityDAO();
        try {
            EntityManagerHelper.beginTransaction();
            coActivityDAO.update(coActivity);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coActivity);
            Object[] args = { coActivity.getActivityName() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.update.success"), args));
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el curso: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coActivity
	 *            Actividad a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoActivity coActivity) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_ACTIVITY);
            query.setParameter(1, coActivity.getActivityId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { coActivity.getActivityName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el curso: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coActivity.getActivityName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sActivityName
	 *            Nombre de la actividad actividad
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sActivityName) {
        List<CoActivity> list = new CoActivityDAO().findByActivityName(sActivityName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("activity.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nActivityId
	 *            C�digo de la actividad
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nActivityId) {
        CoActivity coActivity = new CoActivityDAO().findById(nActivityId);
        if (coActivity == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("activity.search.notFound"));
        } else {
            List<CoActivity> list = new ArrayList<CoActivity>();
            EntityManagerHelper.refresh(coActivity);
            list.add(coActivity);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de actividades
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de actividades
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoActivityDAO coActivityDAO = new CoActivityDAO();
        List<CoActivity> list = coActivityDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("activity.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coActivityDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de actividades clonables
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult result) {
        return listClone(result, 0, 0);
    }

    /**
	 * Obtiene la lista de actividades clonables
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoActivityDAO coActivityDAO = new CoActivityDAO();
        List<CoActivity> list = coActivityDAO.findByFlagClone("1", nRowStart, nMaxResults);
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
	 * Realiza el proceso de relacionar un material con una actividad
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.s
	 */
    public RestServiceResult createActivityMaterial(RestServiceResult serviceResult, String sArrayMaterialId, CoActivity coActivity) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_ACTIVITY_MATERIAL);
            query.setParameter(1, coActivity.getActivityId());
            query.executeUpdate();
            StringTokenizer stringTokenizer = new StringTokenizer(sArrayMaterialId, ",");
            while (stringTokenizer.hasMoreTokens()) {
                long nMaterialId = Long.parseLong(stringTokenizer.nextToken());
                query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_ACTIVITY_MATERIAL);
                query.setParameter(1, coActivity.getActivityId());
                query.setParameter(2, nMaterialId);
                query.executeUpdate();
            }
            EntityManagerHelper.commit();
            Object[] arrayParam = { coActivity.getActivityName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Actividad - Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de agregar un usuario al historial de modificaciones
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.s
	 */
    public static RestServiceResult addUserHistory(RestServiceResult serviceResult, MaUser maUser, CoActivity coActivity) {
        log.info("HISTORIAL DE ACTIVIDADES X USUARIO");
        log.info("HISTORIAL DE ACTIVIDADES X USUARIO");
        CoActivityUserHistory activityUserHistory = new CoActivityUserHistoryDAO().findById(new CoActivityUserHistoryId(coActivity.getActivityId(), maUser.getUserId()));
        log.info("Resultado de la consulta => " + activityUserHistory);
        if (activityUserHistory == null) {
            log.info("Agregando HistorialxActividad: Usuario '" + maUser.getUserId() + "' - actividad '" + coActivity.getActivityId() + "'");
            addUserHistory(maUser, coActivity);
        } else {
            log.info("El usuario '" + maUser.getUserId() + "' tiene la actividad '" + coActivity.getActivityId() + "'");
        }
        log.info("Termina HISTORIAL DE CURSOS...");
        return serviceResult;
    }

    /**
	 * Registra en el historial de actividades un usuario
	 * 
	 * @param maUser
	 *            usuario a registrar
	 * @param coActivity
	 *            actividad a guardar
	 */
    private static void addUserHistory(MaUser maUser, CoActivity coActivity) {
        try {
            CoActivityUserHistoryId userHistoryId = new CoActivityUserHistoryId();
            userHistoryId.setActivityId(coActivity.getActivityId());
            userHistoryId.setUserId(maUser.getUserId());
            new CoActivityUserHistoryDAO().save(new CoActivityUserHistory(userHistoryId, maUser, coActivity, new Date()));
            log.info("Se guardo el usuario " + maUser.getUserName() + " con �xito al historial de la actividad '" + coActivity.getActivityId() + "' ");
        } catch (PersistenceException e) {
            log.info("El usuario " + maUser.getUserId() + " ya esta en el  historial de la acividad '" + coActivity.getActivityId() + "' " + "OMITIR EXCEPCION PRIMARY KEY");
        }
        return;
    }

    /**
	 * Clona lel historial de usuarios y los materiales
	 * 
	 * @param nActivityOldId
	 *            C�digo de la actividad a clonar
	 * @param nActivityNewId
	 *            C�digo de la nueva actividad
	 */
    public void cloneActivity(Long nActivityOldId, Long nActivityNewId) {
        try {
            CoActivity coActivityOld = new CoActivityDAO().findById(new Long(nActivityOldId));
            EntityManagerHelper.refresh(nActivityOldId);
            Set<CoExercises1> setExer1 = coActivityOld.getCoExercises1s();
            log.info(" CLONADO ACTIVIDAD: " + nActivityOldId + " ACTIVIDAD NUEVA: " + nActivityNewId);
            if (setExer1.size() != 0) {
                for (Iterator<CoExercises1> iterator = setExer1.iterator(); iterator.hasNext(); ) {
                    CoExercises1 coExercises1Old = iterator.next();
                    EntityManagerHelper.refresh(coExercises1Old);
                    Long nExercises1NewId = getSequence("sq_co_exercises1");
                    Long nExercises1OldId = coExercises1Old.getExerciseId();
                    EntityManagerHelper.beginTransaction();
                    Query query = EntityManagerHelper.createNativeQuery(Statements.CLONE_EXERCISES_1);
                    query.setParameter(1, nExercises1NewId);
                    query.setParameter(2, nActivityNewId);
                    query.setParameter(3, nExercises1OldId);
                    int nUpdate = query.executeUpdate();
                    log.info("Clonaci�n EJERCICIO_T1[" + nExercises1NewId + "] = Modificados: " + nUpdate);
                    EntityManagerHelper.commit();
                    CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExercises1NewId);
                    EntityManagerHelper.refresh(coExercises1);
                    if (coExercises1 != null) {
                        EntityManagerHelper.beginTransaction();
                        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_EXERCISES1_MATERIAL);
                        query.setParameter(1, nExercises1NewId);
                        query.setParameter(2, nExercises1OldId);
                        nUpdate = query.executeUpdate();
                        log.info("Clonaci�n[" + nExercises1OldId + "]- MATERIALES_EJERCICIO_T1[" + nExercises1NewId + "]" + " - Materiales =>  " + nUpdate);
                        EntityManagerHelper.commit();
                        DataManagerExerciseS1 dataManagerExerciseS1 = new DataManagerExerciseS1();
                        dataManagerExerciseS1.setBundle(bundle);
                        dataManagerExerciseS1.cloneExercises1(nExercises1OldId, nExercises1NewId);
                    }
                }
            } else {
                Set<CoExercises2> setExer2 = coActivityOld.getCoExercises2s();
                if (setExer2.size() != 0) {
                    for (Iterator<CoExercises2> iterator = setExer2.iterator(); iterator.hasNext(); ) {
                        EntityManagerHelper.beginTransaction();
                        CoExercises2 coExercises2Old = iterator.next();
                        Long nExercises2NewId = getSequence("sq_co_exercises2");
                        Long nExercises2OldId = coExercises2Old.getExerciseId();
                        Query query = EntityManagerHelper.createNativeQuery(Statements.CLONE_EXERCISES_2);
                        query.setParameter(1, nExercises2NewId);
                        query.setParameter(2, nActivityNewId);
                        query.setParameter(3, nExercises2OldId);
                        int nUpdate = query.executeUpdate();
                        log.info("Clonaci�n EJERCICIO_T2[" + nExercises2NewId + "] = Modificados:  => " + nUpdate);
                        EntityManagerHelper.commit();
                        CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExercises2NewId);
                        EntityManagerHelper.refresh(coExercises2);
                        if (coExercises2 != null) {
                            EntityManagerHelper.beginTransaction();
                            query = EntityManagerHelper.createNativeQuery(Statements.CLONE_EXERCISES2_MATERIAL);
                            query.setParameter(1, nExercises2NewId);
                            query.setParameter(2, nExercises2OldId);
                            nUpdate = query.executeUpdate();
                            log.info("Clonaci�n[" + nExercises2OldId + "] - MATERIALES_EJERCICIO_T2[" + nExercises2NewId + "]" + " - Materiales  => " + nUpdate);
                            EntityManagerHelper.commit();
                            DataManagerExerciseS2 dataManagerExerciseS2 = new DataManagerExerciseS2();
                            dataManagerExerciseS2.setBundle(bundle);
                            dataManagerExerciseS2.cloneExercises2(nExercises2OldId, nExercises2NewId);
                        }
                    }
                }
            }
            EntityManagerHelper.beginTransaction();
            cloneUserHistory(nActivityOldId, nActivityNewId);
            log.info("Clonaci�n Historial de actividades OK...");
            EntityManagerHelper.commit();
        } catch (Exception e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
        }
    }

    /**
	 * Permite clonar el historial de usuario de una actividad clonada
	 * 
	 * @param nActivityOldId
	 *            C�digo de la actividad a clonar
	 * @param coActivityNew
	 *            Actividad Creada a partir de clonaci�n
	 */
    public static void cloneUserHistory(Long nActivityOldId, Long nActivityNewId) {
        try {
            log.info("CLONACION HISTORIAL DE ACTIVIDADES X USUARIO");
            CoActivity coActivity = new CoActivityDAO().findById(nActivityOldId);
            CoActivity coActivityNew = new CoActivityDAO().findById(nActivityNewId);
            EntityManagerHelper.refresh(coActivity);
            EntityManagerHelper.refresh(coActivityNew);
            Set<CoActivityUserHistory> setUserHistoryOld = coActivity.getCoActivityUserHistories();
            for (CoActivityUserHistory coActivityUserHistory : setUserHistoryOld) {
                MaUser maUser = coActivityUserHistory.getMaUser();
                CoActivityUserHistory activityUserHistory = new CoActivityUserHistoryDAO().findById(new CoActivityUserHistoryId(nActivityNewId, maUser.getUserId()));
                log.info("Resultado de la consulta=> " + activityUserHistory);
                if (activityUserHistory == null) {
                    log.info("Agregando HistorialxActividad: Usuario '" + maUser.getUserId() + "' - actividad '" + coActivityNew.getActivityId() + "'");
                    addUserHistory(maUser, coActivityNew);
                } else {
                    log.info("El usuario '" + maUser.getUserId() + "' tiene la actividad '" + coActivityNew.getActivityId() + "'");
                }
            }
            log.info("Ok...Termina clonaci�n de Historial de usuarios en ACTIVIDADES");
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Obtiene la lista de actividades
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listActivityForUser(RestServiceResult serviceResult, long nUserId) {
        HashMap<String, String> tableActivity = new HashMap<String, String>();
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        if (maUser.getMaRole().getRoleId().equals(Common.ROLE_ID_TEACHER)) {
            tableActivity = getActivityTeacher(maUser);
        } else if (maUser.getMaRole().getRoleId().equals(Common.ROLE_ID_STUDENT)) {
            tableActivity = getActivityStudent(maUser);
        } else {
            log.error("EL ADMINISTRADOR NO TIENE CURSOS");
        }
        HashMap<String, String> mapResultado = Util.sortHashMapByValues(tableActivity);
        log.info("N�mero de actividades normales y solo actividades: " + mapResultado.size());
        if (mapResultado.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("activity.list.notFound"));
        } else {
            Object[] array = { tableActivity.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("activity.list.success"), array));
            serviceResult.setObjResult(mapResultado);
            serviceResult.setNumResult(mapResultado.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene las actividades de los profesores
	 * @param maUser Objeto Usuario
	 * @return HasMap<Codigo,Nombre> de las actividades
	 */
    private LinkedHashMap<String, String> getActivityTeacher(MaUser maUser) {
        LinkedHashMap<String, String> tableActivity = new LinkedHashMap<String, String>();
        for (Iterator<CoCourse> iterator = maUser.getCoCoursesForUserId().iterator(); iterator.hasNext(); ) {
            CoCourse coCourse = iterator.next();
            for (Iterator<CoUnit> iterator2 = coCourse.getCoUnits().iterator(); iterator2.hasNext(); ) {
                CoUnit coUnit = iterator2.next();
                for (Iterator<CoSequence> iterator3 = coUnit.getCoSequences().iterator(); iterator3.hasNext(); ) {
                    CoSequence coSequence = iterator3.next();
                    for (Iterator<CoActivity> iterator4 = coSequence.getCoActivities().iterator(); iterator4.hasNext(); ) {
                        CoActivity coActivity = (CoActivity) iterator4.next();
                        tableActivity.put(coActivity.getActivityId().toString(), coCourse.getCourseCod() + " - " + coActivity.getItem() + "." + coActivity.getActivityName());
                    }
                }
            }
        }
        log.info("N�mero de actividades normales: " + tableActivity.size());
        for (Iterator iterator = maUser.getCoCoursesForUserId().iterator(); iterator.hasNext(); ) {
            CoCourse coCourse = (CoCourse) iterator.next();
            for (Iterator<CoActivity> iterator2 = coCourse.getCoActivities().iterator(); iterator2.hasNext(); ) {
                CoActivity coActivity = (CoActivity) iterator2.next();
                tableActivity.put(coActivity.getActivityId().toString(), coCourse.getCourseCod() + " - " + coActivity.getItem() + "." + coActivity.getActivityName());
            }
        }
        return tableActivity;
    }

    /**
	 * Obtiene las actividades de los estudiantes
	 * @param maUser Usuario estudiante
	 * @return HasMap<Codigo,Nombre> de las actividades
	 */
    private LinkedHashMap<String, String> getActivityStudent(MaUser maUser) {
        LinkedHashMap<String, String> tableActivity = new LinkedHashMap<String, String>();
        for (Iterator<CoCourseUser> iterator = maUser.getCoCourseUsers().iterator(); iterator.hasNext(); ) {
            CoCourseUser coCourseUser = iterator.next();
            for (Iterator<CoUnit> iterator2 = coCourseUser.getCoCourse().getCoUnits().iterator(); iterator2.hasNext(); ) {
                CoUnit coUnit = iterator2.next();
                for (Iterator<CoSequence> iterator3 = coUnit.getCoSequences().iterator(); iterator3.hasNext(); ) {
                    CoSequence coSequence = iterator3.next();
                    for (Iterator<CoActivity> iterator4 = coSequence.getCoActivities().iterator(); iterator4.hasNext(); ) {
                        CoActivity coActivity = (CoActivity) iterator4.next();
                        tableActivity.put(coActivity.getActivityId().toString(), coCourseUser.getCoCourse().getCourseCod() + " - " + coActivity.getItem() + "." + coActivity.getActivityName());
                    }
                }
            }
        }
        log.info("N�mero de actividades normales: " + tableActivity.size());
        for (Iterator<CoCourseUser> iterator = maUser.getCoCourseUsers().iterator(); iterator.hasNext(); ) {
            CoCourseUser coCourseUser = (CoCourseUser) iterator.next();
            for (Iterator<CoActivity> iterator2 = coCourseUser.getCoCourse().getCoActivities().iterator(); iterator2.hasNext(); ) {
                CoActivity coActivity = (CoActivity) iterator2.next();
                tableActivity.put(coActivity.getActivityId().toString(), coCourseUser.getCoCourse().getCourseCod() + " - " + coActivity.getItem() + "." + coActivity.getActivityName());
            }
        }
        return tableActivity;
    }

    public static void main(String[] args) {
    }
}
