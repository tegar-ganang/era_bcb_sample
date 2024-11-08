package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
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
import edu.univalle.lingweb.persistence.CoExercises2;
import edu.univalle.lingweb.persistence.CoExercises2DAO;
import edu.univalle.lingweb.persistence.CoUserExer2Group;
import edu.univalle.lingweb.persistence.CoUserExer2GroupDAO;
import edu.univalle.lingweb.persistence.CoUserExer2GroupId;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.ToExercise2Group;
import edu.univalle.lingweb.persistence.ToExercise2GroupDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'co_exercises1_group'( Ejercicios tipo secuencia 2)
 * 
 * @author Jose Luis Aricapa
 */
public class DataManagerExercise2Group extends DataManager {

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
    public DataManagerExercise2Group() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo ejercicio grupal tipo secuencia 2 en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coExercise2Group
	 *            Ejercicio a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, ToExercise2Group coExercise2Group) {
        ToExercise2GroupDAO ToExercise2GroupDAO = new ToExercise2GroupDAO();
        try {
            Long nSequence = getSequence("sq_co_exercise2_group");
            coExercise2Group.setExerciseGroupId(nSequence);
            EntityManagerHelper.beginTransaction();
            ToExercise2GroupDAO.save(coExercise2Group);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coExercise2Group);
            log.info("Ejercicio grupal S2 creado con �xito: " + coExercise2Group.getGroupName());
            Object[] arrayParam = { coExercise2Group.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.create.success"), arrayParam));
            serviceResult.setId(nSequence);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio S2: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un ejercicio grupal tipo secuencia 1 en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param toExercise2Group
	 *            Ejercicio a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToExercise2Group toExercise2Group) {
        ToExercise2GroupDAO ToExercise2GroupDAO = new ToExercise2GroupDAO();
        try {
            EntityManagerHelper.beginTransaction();
            ToExercise2GroupDAO.update(toExercise2Group);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toExercise2Group);
            Object[] args = { toExercise2Group.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.update.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el ejercicio grupal S2: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina un ejericio grupal tipo secuencia 2
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param toExercise2Group
	 *            Ejercicio a Eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToExercise2Group toExercise2Group) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_EXERCITE2_GROUP);
            query.setParameter(1, toExercise2Group.getExerciseGroupId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toExercise2Group);
            Object[] arrayParam = { toExercise2Group.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el ejercicio grupal S2: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { toExercise2Group.getGroupName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.delete.error") + e.getMessage(), arrayParam));
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
        List<ToExercise2Group> list = new ToExercise2GroupDAO().findByGroupName(sExerciseName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercise2group.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.search.success"), arrayParam));
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
        ToExercise2Group ToExercise2Group = new ToExercise2GroupDAO().findById(nExerciseGroupId);
        if (ToExercise2Group == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercise2group.search.notFound"));
        } else {
            List<ToExercise2Group> list = new ArrayList<ToExercise2Group>();
            EntityManagerHelper.refresh(ToExercise2Group);
            list.add(ToExercise2Group);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.search.success"), arrayParam));
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
        ToExercise2GroupDAO ToExercise2GroupDAO = new ToExercise2GroupDAO();
        List<ToExercise2Group> list = ToExercise2GroupDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("exercise2group.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(ToExercise2GroupDAO.findAll().size()); else serviceResult.setNumResult(list.size());
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
        CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExerciseId);
        EntityManagerHelper.refresh(coExercises2);
        List<ToExercise2Group> list = new ArrayList<ToExercise2Group>();
        Set<ToExercise2Group> set = coExercises2.getToExercise2Groups();
        if (set.size() == 0) {
            serviceResult.setMessage(bundle.getString("exercise2group.search.notFound"));
        } else {
            list.addAll(set);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("exercise2group.search.success"), arrayParam));
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
        List<CoUserExer2Group> listUserGroup = null;
        List<MaUser> listUser = new ArrayList<MaUser>();
        try {
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_USER_EXER2_GROUP, CoUserExer2Group.class);
            query.setParameter(1, nExerciseGroupId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            listUserGroup = query.getResultList();
            serviceResult.setObjResult(listUserGroup);
            if (listUserGroup.size() == 0) {
                serviceResult.setMessage(bundle.getString("user.list.notFound"));
            } else {
                for (Iterator<CoUserExer2Group> iterator = listUserGroup.iterator(); iterator.hasNext(); ) {
                    CoUserExer2Group CoUserExer2Group = (CoUserExer2Group) iterator.next();
                    listUser.add(CoUserExer2Group.getMaUser());
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
        ToExercise2Group ToExercise2GroupOld = new ToExercise2GroupDAO().findById(nExercise1GroupOldId);
        EntityManagerHelper.refresh(ToExercise2GroupOld);
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
            sSql = Statements.UPDATE_FLAG_Y_USER_EXER2_GROUP;
            sSql = sSql.replaceFirst("v1", sArrayUserId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            query.setParameter(1, "Y");
            query.setParameter(2, nExerciseGroupId);
            int nDeleted = query.executeUpdate();
            sSql = Statements.UPDATE_FLAG_N_USER_EXER2_GROUP;
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
                query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_USER_EXER2_GROUP_USER, CoUserExer2Group.class);
                query.setParameter(1, maUser.getUserId());
                query.setParameter(2, nExerciseGroupId);
                query.setHint(QueryHints.REFRESH, HintValues.TRUE);
                Vector vecResult = (Vector) query.getResultList();
                if (vecResult.size() == 0) {
                    CoUserExer2GroupId CoUserExer2GroupId = new CoUserExer2GroupId(nExerciseGroupId, maUser.getUserId());
                    CoUserExer2Group CoUserExer2Group = new CoUserExer2Group();
                    CoUserExer2Group.setToExercise2Group(new ToExercise2GroupDAO().findById(nExerciseGroupId));
                    CoUserExer2Group.setMaUser(maUser);
                    CoUserExer2Group.setId(CoUserExer2GroupId);
                    CoUserExer2Group.setFlagDeleted("N");
                    new CoUserExer2GroupDAO().save(CoUserExer2Group);
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
            CoExercises2 coExercises2 = new CoExercises2DAO().findById(nExerciseId);
            EntityManagerHelper.refresh(coExercises2);
            CoCourse courseOnlyActivity = coExercises2.getCoActivity().getCoCourse();
            if (courseOnlyActivity == null) {
                CoCourse coCourse = coExercises2.getCoActivity().getCoSequence().getCoUnit().getCoCourse();
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
            List<ToExercise2Group> listExerciseGroup = new ArrayList<ToExercise2Group>();
            listExerciseGroup.addAll(coExercises2.getToExercise2Groups());
            for (Iterator<ToExercise2Group> iterator = listExerciseGroup.iterator(); iterator.hasNext(); ) {
                ToExercise2Group toExercise1Group = iterator.next();
                for (Iterator<CoUserExer2Group> iterator2 = toExercise1Group.getCoUserExer2Groups().iterator(); iterator2.hasNext(); ) {
                    CoUserExer2Group coUserExer2Group = iterator2.next();
                    if (coUserExer2Group.getFlagDeleted().equals("N")) vecUserId.add(coUserExer2Group.getMaUser().getUserId());
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
        Long nExerciseId = new Long(9);
        RestServiceResult serviceResult = new RestServiceResult();
        DataManagerExercise2Group dataManagerExercise2Group = new DataManagerExercise2Group();
        dataManagerExercise2Group.setBundle(ResourceBundle.getBundle("edu.univalle.lingweb.LzTrackMessages", new Locale("es")));
        dataManagerExercise2Group.listUserWithoutExerciseGroup(serviceResult, nExerciseId);
        System.out.println("result => " + serviceResult.getMessage());
    }
}
