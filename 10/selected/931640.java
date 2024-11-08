package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseUser;
import edu.univalle.lingweb.persistence.CoQuestion;
import edu.univalle.lingweb.persistence.CoQuestionDAO;
import edu.univalle.lingweb.persistence.CoUserQuestionGroup;
import edu.univalle.lingweb.persistence.CoUserQuestionGroupDAO;
import edu.univalle.lingweb.persistence.CoUserQuestionGroupId;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.ToQuestionGroup;
import edu.univalle.lingweb.persistence.ToQuestionGroupDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'co_exercises1_group'( Ejercicios tipo secuencia 2)
 * 
 * @author Jose Luis Aricapa
 */
public class DataManagerQuestionGroup extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerQuestionGroup() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo ejercicio grupal tipo secuencia 2 en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coQuestionGroup
	 *            Ejercicio a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToQuestionGroup coQuestionGroup) {
        ToQuestionGroupDAO ToQuestionGroupDAO = new ToQuestionGroupDAO();
        try {
            Long nSequence = getSequence("sq_co_question_group");
            coQuestionGroup.setExerciseGroupId(nSequence);
            EntityManagerHelper.beginTransaction();
            ToQuestionGroupDAO.save(coQuestionGroup);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coQuestionGroup);
            log.info("Ejercicio grupal Pregunta creado con �xito: " + coQuestionGroup.getGroupName());
            Object[] arrayParam = { coQuestionGroup.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.create.success"), arrayParam));
            serviceResult.setId(nSequence);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio Pregunta: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un ejercicio grupal tipo pregunta en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param toQuestionGroup
	 *            Ejercicio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToQuestionGroup toQuestionGroup) {
        ToQuestionGroupDAO ToQuestionGroupDAO = new ToQuestionGroupDAO();
        try {
            EntityManagerHelper.beginTransaction();
            ToQuestionGroupDAO.update(toQuestionGroup);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toQuestionGroup);
            Object[] args = { toQuestionGroup.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.update.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio grupal Pregunta: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un ejericio grupal tipo secuencia 2
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param toQuestionGroup
	 *            Ejercicio a Eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToQuestionGroup toQuestionGroup) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_QUESTION_GROUP);
            query.setParameter(1, toQuestionGroup.getExerciseGroupId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toQuestionGroup);
            Object[] arrayParam = { toQuestionGroup.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el ejercicio grupal Pregunta: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toQuestionGroup.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio grupal tipo pregunta por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sActivityName
	 *            Nombre del ejercicio tipo pregunta
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sExerciseName) {
        List<ToQuestionGroup> list = new ToQuestionGroupDAO().findByGroupName(sExerciseName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("questionGroup.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio tipo pregunta por id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nQuestionGroupId
	 *            C�digo del ejercicio
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nQuestionGroupId) {
        ToQuestionGroup ToQuestionGroup = new ToQuestionGroupDAO().findById(nQuestionGroupId);
        if (ToQuestionGroup == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("questionGroup.search.notFound"));
        } else {
            List<ToQuestionGroup> list = new ArrayList<ToQuestionGroup>();
            EntityManagerHelper.refresh(ToQuestionGroup);
            list.add(ToQuestionGroup);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de ejercicios grupales
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de ejercicios grupales en un rango determinado
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        ToQuestionGroupDAO toQuestionGroupDAO = new ToQuestionGroupDAO();
        List<ToQuestionGroup> list = toQuestionGroupDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("questionGroup.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toQuestionGroupDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio grupal tipo pregunta por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sActivityName
	 *            Nombre del ejercicio tipo pregunta
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listGroupForExercise(RestServiceResult serviceResult, Long nQuestionId) {
        CoQuestion coQuestion = new CoQuestionDAO().findById(nQuestionId);
        EntityManagerHelper.refresh(coQuestion);
        List<ToQuestionGroup> list = new ArrayList<ToQuestionGroup>();
        Set<ToQuestionGroup> set = coQuestion.getToQuestionGroups();
        if (set.size() == 0) {
            serviceResult.setMessage(bundle.getString("questionGroup.search.notFound"));
        } else {
            list.addAll(set);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("questionGroup.search.success"), arrayParam));
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio grupal tipo pregunta por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sActivityName
	 *            Nombre del ejercicio tipo pregunta
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listUserForExerciseGroup(RestServiceResult serviceResult, Long nQuestionGroupId) {
        List<CoUserQuestionGroup> listUserGroup = null;
        List<MaUser> listUser = new ArrayList<MaUser>();
        try {
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_USER_QUESTION_GROUP, CoUserQuestionGroup.class);
            query.setParameter(1, nQuestionGroupId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            listUserGroup = query.getResultList();
            serviceResult.setObjResult(listUserGroup);
            if (listUserGroup.size() == 0) {
                serviceResult.setMessage(bundle.getString("user.list.notFound"));
            } else {
                for (Iterator<CoUserQuestionGroup> iterator = listUserGroup.iterator(); iterator.hasNext(); ) {
                    CoUserQuestionGroup coUserQuestionGroup = (CoUserQuestionGroup) iterator.next();
                    listUser.add(coUserQuestionGroup.getMaUser());
                }
                Object[] arrayParam = { listUser.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.list.success"), arrayParam));
            }
            serviceResult.setObjResult(listUser);
        } catch (Exception e) {
            serviceResult.setError(true);
            log.info("Error buscando el estado para usuarios por grupo ");
        }
        return serviceResult;
    }

    /**
	 * permite clonar el contenido de un ejercicio grupal
	 * 
	 * @param nQuestion1GroupOldId
	 *            C�digo del ejercicio viejo
	 * @param nQuestion1GroupNewId
	 *            C�digo del ejercicio nuevo
	 */
    public void cloneExercise1Group(long nQuestion1GroupOldId, long nQuestion1GroupNewId) {
        ToQuestionGroup ToQuestionGroupOld = new ToQuestionGroupDAO().findById(nQuestion1GroupOldId);
        EntityManagerHelper.refresh(ToQuestionGroupOld);
    }

    /**
	 * Sincroniza la informaci�n de usuarios de un grupo
	 * 
	 * @param nQuestionGroupId
	 *            C�digo del grupo
	 * @param sArrayUserId
	 *            Lista de usuarios
	 */
    public void synchronizeUserGroup(Long nQuestionGroupId, String sArrayUserId) {
        List<MaUser> listUserGroup = null;
        int nNumUserGroup = 0;
        int nNumArrayUser = 0;
        String sSql = null;
        try {
            sSql = Statements.UPDATE_FLAG_Y_USER_QUESTION_GROUP;
            sSql = sSql.replaceFirst("v1", sArrayUserId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            query.setParameter(1, "Y");
            query.setParameter(2, nQuestionGroupId);
            int nDeleted = query.executeUpdate();
            sSql = Statements.UPDATE_FLAG_N_USER_QUESTION_GROUP;
            sSql = sSql.replaceFirst("v1", sArrayUserId);
            query = EntityManagerHelper.createNativeQuery(sSql);
            query.setParameter(1, "N");
            query.setParameter(2, nQuestionGroupId);
            nDeleted = query.executeUpdate();
            sSql = Statements.SELECT_MA_USER_IN;
            sSql = sSql.replaceFirst("v1", sArrayUserId);
            query = EntityManagerHelper.createNativeQuery(sSql, MaUser.class);
            query.setParameter(1, nQuestionGroupId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            listUserGroup = query.getResultList();
            nNumArrayUser = listUserGroup.size();
            for (Iterator iterator = listUserGroup.iterator(); iterator.hasNext(); ) {
                MaUser maUser = (MaUser) iterator.next();
                query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_USER_QUESTION_GROUP_USER, CoUserQuestionGroup.class);
                query.setParameter(1, maUser.getUserId());
                query.setParameter(2, nQuestionGroupId);
                query.setHint(QueryHints.REFRESH, HintValues.TRUE);
                Vector vecResult = (Vector) query.getResultList();
                if (vecResult.size() == 0) {
                    CoUserQuestionGroupId CoUserQuestionGroupId = new CoUserQuestionGroupId(nQuestionGroupId, maUser.getUserId());
                    CoUserQuestionGroup coUserQuestionGroup = new CoUserQuestionGroup();
                    coUserQuestionGroup.setToQuestionGroup(new ToQuestionGroupDAO().findById(nQuestionGroupId));
                    coUserQuestionGroup.setMaUser(maUser);
                    coUserQuestionGroup.setId(CoUserQuestionGroupId);
                    coUserQuestionGroup.setFlagDeleted("N");
                    new CoUserQuestionGroupDAO().save(coUserQuestionGroup);
                } else {
                }
            }
            EntityManagerHelper.commit();
        } catch (Exception e) {
            log.info("Error buscando el estado para usuarios por grupo ");
            EntityManagerHelper.rollback();
        }
    }

    /**
	 * Obtiene los usuarios estudiantes que no pertenecen a un grupo de un ejercicio
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nExerciseId
	 *            c�digo ejercicio tipo secuencia 2
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listUserWithoutExerciseGroup(RestServiceResult serviceResult, Long nExerciseId) {
        List<MaUser> listUserWithoutGroup = new ArrayList<MaUser>();
        List<MaUser> listUserCourse = new ArrayList<MaUser>();
        Vector<Long> vecUserId = new Vector<Long>();
        try {
            CoQuestion coQuestion = new CoQuestionDAO().findById(nExerciseId);
            EntityManagerHelper.refresh(coQuestion);
            CoCourse courseOnlyActivity = null;
            if (courseOnlyActivity == null) {
                CoCourse coCourse = coQuestion.getCoTest().getCoSequence().getCoUnit().getCoCourse();
                List<CoCourseUser> list = new ArrayList<CoCourseUser>(coCourse.getCoCourseUsers());
                for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
                    CoCourseUser coCourseUser = (CoCourseUser) iterator.next();
                    listUserCourse.add(coCourseUser.getMaUser());
                }
            } else {
                List<CoCourseUser> list = new ArrayList<CoCourseUser>(courseOnlyActivity.getCoCourseUsers());
                for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
                    CoCourseUser coCourseUser = (CoCourseUser) iterator.next();
                    listUserCourse.add(coCourseUser.getMaUser());
                }
            }
            List<ToQuestionGroup> listExerciseGroup = new ArrayList<ToQuestionGroup>();
            listExerciseGroup.addAll(coQuestion.getToQuestionGroups());
            for (Iterator<ToQuestionGroup> iterator = listExerciseGroup.iterator(); iterator.hasNext(); ) {
                ToQuestionGroup toQuestionGroup = iterator.next();
                for (Iterator<CoUserQuestionGroup> iterator2 = toQuestionGroup.getCoUserQuestionGroups().iterator(); iterator2.hasNext(); ) {
                    CoUserQuestionGroup coUserQuestionGroup = iterator2.next();
                    if (coUserQuestionGroup.getFlagDeleted().equals("N")) vecUserId.add(coUserQuestionGroup.getMaUser().getUserId());
                }
            }
            for (Iterator<MaUser> iterator = listUserCourse.iterator(); iterator.hasNext(); ) {
                MaUser maUser = iterator.next();
                if (vecUserId.contains(maUser.getUserId())) {
                    continue;
                } else {
                    listUserWithoutGroup.add(maUser);
                }
            }
            if (listUserWithoutGroup.size() == 0) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("user.list.notFound"));
            } else {
                Object[] arrayParam = { listUserWithoutGroup.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.list.success"), arrayParam));
                serviceResult.setObjResult(listUserWithoutGroup);
            }
        } catch (Exception e) {
            serviceResult.setError(true);
            log.info("Error buscando usuarios sin grupo ");
            e.printStackTrace();
        }
        return serviceResult;
    }

    public static void main(String[] args) {
        new DataManagerQuestionGroup().synchronizeUserGroup(new Long("1"), "218,213,219");
    }
}
