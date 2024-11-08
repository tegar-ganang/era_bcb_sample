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
import edu.univalle.lingweb.persistence.CoExercises1;
import edu.univalle.lingweb.persistence.CoExercises1DAO;
import edu.univalle.lingweb.persistence.CoUserExer1Group;
import edu.univalle.lingweb.persistence.CoUserExer1GroupDAO;
import edu.univalle.lingweb.persistence.CoUserExer1GroupId;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.ToExercise1Group;
import edu.univalle.lingweb.persistence.ToExercise1GroupDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'co_exercises1'( Ejercicios tipo secuencia 1)
 * 
 * @author Julio Cesar Puentes
 */
public class DataManagerExercise1Group extends DataManager {

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
    public DataManagerExercise1Group() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo ejercicio grupal tipo secuencia 1 en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coExercise1Group
	 *            Ejercicio a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToExercise1Group coExercise1Group) {
        ToExercise1GroupDAO ToExercise1GroupDAO = new ToExercise1GroupDAO();
        try {
            Long nSequence = getSequence("sq_co_exercise1_group");
            coExercise1Group.setExerciseGroupId(nSequence);
            EntityManagerHelper.beginTransaction();
            ToExercise1GroupDAO.save(coExercise1Group);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coExercise1Group);
            log.info("Ejercicio grupal S1 creado con �xito: " + coExercise1Group.getGroupName());
            Object[] arrayParam = { coExercise1Group.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.create.success"), arrayParam));
            serviceResult.setId(nSequence);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio s1: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un ejercicio grupal tipo secuencia 1 en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param ToExercise1Group
	 *            Ejercicio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToExercise1Group toExercise1Group) {
        ToExercise1GroupDAO toExercise1GroupDAO = new ToExercise1GroupDAO();
        try {
            EntityManagerHelper.beginTransaction();
            toExercise1GroupDAO.update(toExercise1Group);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toExercise1Group);
            Object[] args = { toExercise1Group.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.update.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio grupal s1: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un ejericio grupal tipo secuencia 1
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param ToExercise1Group
	 *            Ejercicio a Eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToExercise1Group toExercise1Group) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_EXERCITE1_GROUP);
            query.setParameter(1, toExercise1Group.getExerciseGroupId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toExercise1Group);
            Object[] arrayParam = { toExercise1Group.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el ejercicio grupal s1: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toExercise1Group.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio grupal tipo secuencia 1 por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sActivityName
	 *            Nombre del ejercicio tipo secuencia 1
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sExerciseName) {
        List<ToExercise1Group> list = new ToExercise1GroupDAO().findByGroupName(sExerciseName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercise1group.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio tipo secuencia 1 por id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nExerciseGroupId
	 *            C�digo del ejercicio
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nExerciseGroupId) {
        ToExercise1Group toExercise1Group = new ToExercise1GroupDAO().findById(nExerciseGroupId);
        if (toExercise1Group == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercise1group.search.notFound"));
        } else {
            List<ToExercise1Group> list = new ArrayList<ToExercise1Group>();
            EntityManagerHelper.refresh(toExercise1Group);
            list.add(toExercise1Group);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.search.success"), arrayParam));
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
        ToExercise1GroupDAO toExercise1GroupDAO = new ToExercise1GroupDAO();
        List<ToExercise1Group> list = toExercise1GroupDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercise1group.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(toExercise1GroupDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio grupal tipo secuencia 1 por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sActivityName
	 *            Nombre del ejercicio tipo secuencia 1
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listGroupForExercise(RestServiceResult serviceResult, Long nExerciseId) {
        CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
        EntityManagerHelper.refresh(coExercises1);
        List<ToExercise1Group> list = new ArrayList<ToExercise1Group>();
        Set<ToExercise1Group> set = coExercises1.getToExercise1Groups();
        if (set.size() == 0) {
            serviceResult.setMessage(bundle.getString("exercise1group.search.notFound"));
        } else {
            list.addAll(set);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise1group.search.success"), arrayParam));
        }
        serviceResult.setObjResult(list);
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un ejercicio grupal tipo secuencia 1 por nombre
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sActivityName
	 *            Nombre del ejercicio tipo secuencia 1
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listUserForExerciseGroup(RestServiceResult serviceResult, Long nExerciseGroupId) {
        List<CoUserExer1Group> listUserGroup = null;
        List<MaUser> listUser = new ArrayList<MaUser>();
        try {
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_USER_EXER1_GROUP, CoUserExer1Group.class);
            query.setParameter(1, nExerciseGroupId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            listUserGroup = query.getResultList();
            serviceResult.setObjResult(listUserGroup);
            if (listUserGroup.size() == 0) {
                serviceResult.setMessage(bundle.getString("user.list.notFound"));
            } else {
                for (Iterator<CoUserExer1Group> iterator = listUserGroup.iterator(); iterator.hasNext(); ) {
                    CoUserExer1Group coUserExer1Group = (CoUserExer1Group) iterator.next();
                    listUser.add(coUserExer1Group.getMaUser());
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
	 * @param nExercise1GroupOldId
	 *            C�digo del ejercicio viejo
	 * @param nExercise1GroupNewId
	 *            C�digo del ejercicio nuevo
	 */
    public void cloneExercise1Group(long nExercise1GroupOldId, long nExercise1GroupNewId) {
        ToExercise1Group ToExercise1GroupOld = new ToExercise1GroupDAO().findById(nExercise1GroupOldId);
        EntityManagerHelper.refresh(ToExercise1GroupOld);
    }

    /**
	 * Sincroniza la informaci�n de usuarios de un grupo
	 * 
	 * @param nExerciseGroupId
	 *            C�digo del grupo
	 * @param sArrayUserId
	 *            Lista de usuarios
	 */
    public void synchronizeUserGroup(Long nExerciseGroupId, String sArrayUserId) {
        List<MaUser> listUserGroup = null;
        int nNumUserGroup = 0;
        int nNumArrayUser = 0;
        String sSql = null;
        try {
            sSql = Statements.UPDATE_FLAG_Y_USER_EXER1_GROUP;
            sSql = sSql.replaceFirst("v1", sArrayUserId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            query.setParameter(1, "Y");
            query.setParameter(2, nExerciseGroupId);
            int nDeleted = query.executeUpdate();
            sSql = Statements.UPDATE_FLAG_N_USER_EXER1_GROUP;
            sSql = sSql.replaceFirst("v1", sArrayUserId);
            query = EntityManagerHelper.createNativeQuery(sSql);
            query.setParameter(1, "N");
            query.setParameter(2, nExerciseGroupId);
            nDeleted = query.executeUpdate();
            sSql = Statements.SELECT_MA_USER_IN;
            sSql = sSql.replaceFirst("v1", sArrayUserId);
            query = EntityManagerHelper.createNativeQuery(sSql, MaUser.class);
            query.setParameter(1, nExerciseGroupId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            listUserGroup = query.getResultList();
            nNumArrayUser = listUserGroup.size();
            for (Iterator iterator = listUserGroup.iterator(); iterator.hasNext(); ) {
                MaUser maUser = (MaUser) iterator.next();
                query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_USER_EXER1_GROUP_USER, CoUserExer1Group.class);
                query.setParameter(1, maUser.getUserId());
                query.setParameter(2, nExerciseGroupId);
                query.setHint(QueryHints.REFRESH, HintValues.TRUE);
                Vector vecResult = (Vector) query.getResultList();
                if (vecResult.size() == 0) {
                    CoUserExer1GroupId coUserExer1GroupId = new CoUserExer1GroupId(nExerciseGroupId, maUser.getUserId());
                    CoUserExer1Group coUserExer1Group = new CoUserExer1Group();
                    coUserExer1Group.setToExercise1Group(new ToExercise1GroupDAO().findById(nExerciseGroupId));
                    coUserExer1Group.setMaUser(maUser);
                    coUserExer1Group.setId(coUserExer1GroupId);
                    coUserExer1Group.setFlagDeleted("N");
                    new CoUserExer1GroupDAO().save(coUserExer1Group);
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
	 * @param sActivityName
	 *            Nombre del ejercicio tipo secuencia 1
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listUserWithoutExerciseGroup(RestServiceResult serviceResult, Long nExerciseId) {
        List<MaUser> listUserWithoutGroup = new ArrayList<MaUser>();
        List<MaUser> listUserCourse = new ArrayList<MaUser>();
        Vector<Long> vecUserId = new Vector<Long>();
        try {
            CoExercises1 coExercises1 = new CoExercises1DAO().findById(nExerciseId);
            EntityManagerHelper.refresh(coExercises1);
            CoCourse courseOnlyActivity = coExercises1.getCoActivity().getCoCourse();
            if (courseOnlyActivity == null) {
                CoCourse coCourse = coExercises1.getCoActivity().getCoSequence().getCoUnit().getCoCourse();
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
            List<ToExercise1Group> listExerciseGroup = new ArrayList<ToExercise1Group>();
            listExerciseGroup.addAll(coExercises1.getToExercise1Groups());
            for (Iterator<ToExercise1Group> iterator = listExerciseGroup.iterator(); iterator.hasNext(); ) {
                ToExercise1Group toExercise1Group = iterator.next();
                for (Iterator<CoUserExer1Group> iterator2 = toExercise1Group.getCoUserExer1Groups().iterator(); iterator2.hasNext(); ) {
                    CoUserExer1Group coUserExer1Group = iterator2.next();
                    if (coUserExer1Group.getFlagDeleted().equals("N")) vecUserId.add(coUserExer1Group.getMaUser().getUserId());
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
        new DataManagerExercise1Group().synchronizeUserGroup(new Long("1"), "218,213,219");
    }
}
